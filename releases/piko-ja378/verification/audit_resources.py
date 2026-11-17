#!/usr/bin/env python3
"""Independent verification of compiled APK strings, not decoded source XML.

Reads resources.arsc string pools/type chunks directly. It requires the requested
strings to be TYPE_STRING values in exactly the default or unqualified ja config.
Raw string-pool values preserve spaces, newlines, and other characters exactly.
"""
import argparse
import hashlib
import json
import re
import struct
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


class Arsc:
    def __init__(self, raw):
        self.raw = raw
        self.entries = {}
        self.packages = []
        t, h, size = self.chunk(0)
        assert t == 2 and size == len(raw), 'Not a complete compiled resource table'
        global_strings = None
        for at, typ, head, length in self.children(h, size):
            if typ == 1:
                assert global_strings is None, 'Multiple global string pools'
                global_strings = self.pool(at)
            elif typ == 0x200:
                assert global_strings is not None, 'Global string pool not found'
                self.package(at, head, length, global_strings)

    def u16(self, p):
        return struct.unpack_from('<H', self.raw, p)[0]

    def u32(self, p):
        return struct.unpack_from('<I', self.raw, p)[0]

    def chunk(self, p):
        t, h, n = struct.unpack_from('<HHI', self.raw, p)
        assert n >= h >= 8 and p + n <= len(self.raw), 'Invalid ARSC chunk bounds'
        return t, h, n

    def children(self, start, end):
        p = start
        while p < end:
            t, h, n = self.chunk(p)
            assert p + n <= end, 'Nested ARSC chunk exceeds container'
            yield p, t, h, n
            p += n
        assert p == end

    def length8(self, p):
        x = self.raw[p]
        return (((x & 127) << 8) | self.raw[p+1], p+2) if x & 128 else (x, p+1)

    def length16(self, p):
        x = self.u16(p)
        return (((x & 32767) << 16) | self.u16(p+2), p+4) if x & 32768 else (x, p+2)

    def pool(self, at):
        typ, head, length = self.chunk(at)
        assert typ == 1
        count, styles, flags, start = struct.unpack_from('<IIII', self.raw, at+8)
        values = []
        for i in range(count):
            p = at + start + self.u32(at+head+i*4)
            if flags & 0x100:
                units, p = self.length8(p)
                size, p = self.length8(p)
                assert self.raw[p+size] == 0
                # Android pools can retain Java surrogate code units as CESU-8.
                # Normalize paired surrogates to the same Python Unicode value.
                value = self.raw[p:p+size].decode('utf-8', 'surrogatepass')
                value = value.encode('utf-16le', 'surrogatepass').decode('utf-16le', 'surrogatepass')
            else:
                size, p = self.length16(p)
                assert self.u16(p+size*2) == 0
                value = self.raw[p:p+size*2].decode('utf-16le', 'surrogatepass')
            values.append(value)
        return values

    def package(self, at, head, length, global_strings):
        package_id = self.u32(at+8)
        package_name = self.raw[at+12:at+268].decode('utf-16le').split('\0', 1)[0]
        type_strings = self.pool(at+self.u32(at+268))
        key_strings = self.pool(at+self.u32(at+276))
        type_offset = self.u32(at+284) if head >= 288 else 0
        self.packages.append({'id': package_id, 'name': package_name})
        # The NewX resources belong to the main application (0x7f), while the
        # payment dynamic-feature package has unrelated duplicate names.
        if package_id != 0x7f:
            return
        for p, typ, h, n in self.children(at+head, at+length):
            if typ != 0x201:
                continue
            type_id, flags = self.raw[p+8:p+10]
            count, start = struct.unpack_from('<II', self.raw, p+12)
            if type_strings[type_id-1] != 'string':
                continue
            config_size = self.u32(p+20)
            config = self.raw[p+24:p+20+config_size]
            if not any(config):
                qualifier = ''
            else:
                # Relative to config after its size: language is bytes 4..5.
                lang = config[4:6]
                rest = config[:4] + b'\0\0' + config[6:]
                qualifier = 'ja' if lang == b'ja' and not any(rest) else 'raw:'+config.hex()
            offsets = []
            if flags & 1:
                for i in range(count):
                    index, offset = struct.unpack_from('<HH', self.raw, p+h+i*4)
                    offsets.append((index, offset*4))
            elif flags & 2:
                offsets = [(i, self.u16(p+h+i*2)*4) for i in range(count)
                           if self.u16(p+h+i*2) != 0xffff]
            else:
                offsets = [(i, self.u32(p+h+i*4)) for i in range(count)
                           if self.u32(p+h+i*4) != 0xffffffff]
            for index, offset in offsets:
                q = p+start+offset
                entry_size, entry_flags, key = struct.unpack_from('<HHI', self.raw, q)
                assert not entry_flags & 8, 'Compact entry requires explicit parser support'
                if entry_flags & 1:
                    continue
                value_size, zero, data_type, data = struct.unpack_from('<HBBI', self.raw, q+entry_size)
                assert value_size == 8 and zero == 0
                name = key_strings[key]
                value = global_strings[data] if data_type == 3 else {'type': data_type, 'data': data}
                rid = (package_id << 24) | ((type_id+type_offset) << 16) | index
                slot = self.entries.setdefault(name, {})
                assert qualifier not in slot, 'Duplicate name/config: '+name+'/'+qualifier
                slot[qualifier] = {'id': f'0x{rid:08x}', 'value': value, 'package': package_name}


def android_string(text):
    """Interpret Android source escapes/quotes without changing escaped newlines.

    These input XMLs contain simple string nodes, so no spans/references/plurals
    are handled. Fail closed on unsupported escape sequences.
    """
    out, quoted, pending, i = [], False, False, 0
    while i < len(text):
        ch = text[i]
        if ch == '\\':
            i += 1
            assert i < len(text), 'Incomplete Android string escape'
            ch = text[i]
            escapes = {'n': '\n', 't': '\t', 'r': '\r', '\\': '\\', '"': '"', "'": "'", '@': '@', '?': '?'}
            if ch == 'u':
                assert re.fullmatch('[0-9a-fA-F]{4}', text[i+1:i+5])
                ch = chr(int(text[i+1:i+5], 16)); i += 4
            else:
                assert ch in escapes, 'Unsupported Android escape: '+ch
                ch = escapes[ch]
            if pending and out:
                out.append(' ')
            pending = False
            out.append(ch)
        elif ch == '"':
            quoted = not quoted
        elif ch.isspace() and not quoted:
            pending = True
        else:
            if pending and out:
                out.append(' ')
            pending = False
            out.append(ch)
        i += 1
    assert not quoted, 'Unclosed Android source quote'
    return ''.join(out)


def read_xml(path):
    result = {}
    for node in ET.parse(path).getroot():
        assert node.tag == 'string' and len(node) == 0, 'Only simple string nodes expected'
        name = node.attrib['name']
        assert name not in result
        result[name] = android_string(node.text or '')
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('apk', type=Path)
    parser.add_argument('--report', type=Path, required=True)
    parser.add_argument('--baseline', type=Path)
    args = parser.parse_args()
    with zipfile.ZipFile(args.apk) as z:
        raw = z.read('resources.arsc')
    compiled = Arsc(raw)
    expected = read_xml(ROOT/'translations/newx-ja.xml')
    source = read_xml(ROOT/'translations/upstream-newx-strings.xml')
    targets = json.loads((ROOT/'translations/hardcoded-ja.json').read_text())
    helpers = {}
    for row in targets:
        pair = {'': row['source'], 'ja': row['ja']}
        assert row['resource'] not in helpers or helpers[row['resource']] == pair
        helpers[row['resource']] = pair
    assert len(expected) == 485 and len(helpers) == 176
    # Cross-check implementation XML against independent, exact JSON literal data.
    default_xml = read_xml(ROOT/'patch/build/resources/res/values/hiro_piko_ui.xml')
    helper_ja_xml = read_xml(ROOT/'patch/build/resources/res/values-ja/hiro_piko_ui.xml')
    failures, checks = [], []
    def check(name, config, value, group):
        actual = compiled.entries.get(name, {}).get(config, {}).get('value')
        ok = actual == value
        checks.append({'name': name, 'configuration': config or 'default', 'group': group,
                       'matches': ok, 'expected': value, 'actual': actual})
        if not ok:
            failures.append(f'{group}/{name}/{config or "default"}: {actual!r} != {value!r}')
    for name, value in expected.items():
        check(name, 'ja', value, 'newx_ja')
        check(name, '', source[name], 'newx_default')
    for name, pairs in helpers.items():
        assert default_xml.get(name) == pairs[''], 'Default source encoding differs: '+name
        assert helper_ja_xml.get(name) == pairs['ja'], 'Japanese source encoding differs: '+name
        for config, value in pairs.items():
            check(name, config, value, 'helper_ja' if config else 'helper_default')
    exact_boundaries = [name for name, pair in helpers.items()
                        if any(v != v.strip() or '\n' in v or '\t' in v for v in pair.values())]
    nontranslatable = 'piko_newx_patch_version'
    if 'ja' in compiled.entries.get(nontranslatable, {}):
        failures.append('Nontranslatable Piko patch-version has an unexpected Japanese value')
    baseline_result = None
    if args.baseline:
        with zipfile.ZipFile(args.baseline) as z:
            baseline = Arsc(z.read('resources.arsc'))
        altered = []
        for name in expected:
            old = baseline.entries.get(name, {}).get('', {}).get('value')
            new = compiled.entries.get(name, {}).get('', {}).get('value')
            if old != new:
                altered.append({'name': name, 'before': old, 'after': new})
        existing_configurations, changed_existing, newly_localized = 0, [], []
        for name, configs in baseline.entries.items():
            for config, old_entry in configs.items():
                existing_configurations += 1
                old = old_entry['value']
                new = compiled.entries.get(name, {}).get(config, {}).get('value')
                if old != new:
                    changed_existing.append({'name': name, 'configuration': config or 'default',
                                             'before': old, 'after': new})
        for name in expected:
            if 'ja' not in baseline.entries.get(name, {}) and 'ja' in compiled.entries.get(name, {}):
                newly_localized.append(name)
        baseline_result = {'path': str(args.baseline), 'newx_default_changed': altered,
                           'existing_string_configurations_checked': existing_configurations,
                           'existing_string_configurations_changed': changed_existing,
                           'newly_localized_newx_keys': len(newly_localized),
                           'note': 'Only NewX default changes fail this cross-version comparison; other differences are listed for review because Piko versions and clone labels may differ.'}
        if altered:
            failures.append(f'{len(altered)} default NewX values changed versus baseline')
    report = {'result': 'PASS' if not failures else 'FAIL', 'apk': str(args.apk.resolve()),
              'apk_sha256': hashlib.sha256(args.apk.read_bytes()).hexdigest(),
              'arsc_sha256': hashlib.sha256(raw).hexdigest(), 'packages': compiled.packages,
              'newx_japanese_keys': len(expected), 'helper_japanese_keys': len(helpers),
              'helper_default_exact_keys': len(helpers), 'newx_default_keys': len(expected),
              'boundary_space_or_newline_helpers': exact_boundaries,
              'nontranslatable_patch_version': compiled.entries.get(nontranslatable),
              'baseline_comparison': baseline_result, 'failures': failures, 'checks': checks,
              'limitations': ['Static ARSC values/configurations checked; no Android device UI or locale execution.',
                              'Japanese configuration is required to be unqualified ja; Android runtime locale choice is not emulated.']}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({k: report[k] for k in ('result', 'apk', 'newx_japanese_keys',
          'helper_japanese_keys', 'helper_default_exact_keys', 'failures')}, ensure_ascii=False))
    raise SystemExit(bool(failures))


if __name__ == '__main__':
    main()
