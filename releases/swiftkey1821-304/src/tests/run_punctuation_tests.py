#!/usr/bin/env python3
"""Run production punctuation code and preserved FlickSymbols on host fakes.

All native resource fixtures are pinned raw resources from the archived APK.
Baseline key swaps are generated with a reviewed port of the unchanged DEX
algorithm and its real static tables. This suite is not Android/ART execution.
"""
import argparse
import base64
import json
import shutil
import struct
import subprocess
import sys
import zlib
from pathlib import Path

from punctuation_fixtures import KeySwapBaseline, parse_layout, sha256, utf


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-dir', type=Path, required=True)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--tools', type=Path)
    args = parser.parse_args()
    root, work = args.source_dir.resolve(), args.work.resolve()
    work.mkdir(parents=True, exist_ok=True)
    fixtures = root / 'tests/punctuation_fixtures'
    manifest = json.loads((fixtures / 'manifest.json').read_text())
    production = root / 'src/hiro/swiftkey/PunctuationKeys.java'
    flick = root / 'src/hiro/swiftkey/FlickSymbols.java'
    assertions = 0
    scenarios = 0

    def check(condition, message):
        nonlocal assertions
        assertions += 1
        if not condition:
            raise AssertionError(message)

    original = {}
    for name, metadata in manifest['fixtures'].items():
        data = (fixtures / name).read_bytes()
        check(sha256(data) == metadata['sha256'], 'fixture drift: ' + name)
        if metadata.get('encoding') == 'base64':
            data = base64.b64decode(data, validate=False)
            check(sha256(data) == metadata['decoded_sha256'], 'decoded fixture drift: ' + name)
            original[Path(name).stem] = data
    check(sha256(flick.read_bytes()) == manifest['baseline_flick_source_sha256'],
          'FlickSymbols source must remain byte identical to 1.8.16')
    standard = 'keyboard_layout_standard_romaji'
    compact = standard + '_compact'
    pc = standard + '_pc'
    latin = 'keyboard_layout_standard_qwerty'
    (work / 'original-standard.zlib').write_bytes(original[standard])
    (work / 'original-pc.zlib').write_bytes(original[pc])
    key_swap = KeySwapBaseline(fixtures / 'baseline/KeySwapBinary.smali')
    inputs = work / 'input'
    outputs = work / 'output'
    inputs.mkdir(exist_ok=True)
    outputs.mkdir(exist_ok=True)
    cases = []
    bitmask_cases = []
    unique_per_layout = {}
    for short, name in [('standard', standard), ('compact', compact), ('pc', pc)]:
        hashes = set()
        for mask in range(256):
            # KeySwap.wrap only reads the four Japanese-QWERTY options for these
            # resources. Other settings are present but irrelevant to this page.
            effective = mask & (1 | 2 | 16 | 32)
            source = original[name] if name == pc else key_swap.transform(original[name], effective)
            hashes.add(sha256(source))
            case_id = '%s-%03d' % (short, mask)
            path = inputs / (case_id + '.zlib')
            path.write_bytes(source)
            cases.append((case_id, name, path, 'changed'))
            bitmask_cases.append((case_id, name, mask))
        unique_per_layout[short] = len(hashes)
    check(unique_per_layout == {'standard': 16, 'compact': 16, 'pc': 1},
          'expected all effective Japanese key-swap variants, PC has no baseline transform')

    marker = 'hiroPunctuationKeys1817'
    native = parse_layout(original[standard])
    period = next(k for k in native.keys if k.primary == '。')
    vowel = next(k for k in native.keys if k.primary == 'ー')

    def add_failure(name, data, resource=standard):
        path = inputs / (name + '.zlib')
        path.write_bytes(data)
        cases.append((name, resource, path, 'same'))

    add_failure('unknown-name', original[standard], 'keyboard_layout_future_romaji')
    add_failure('null-name', original[standard], '<null>')
    add_failure('latin-unchanged', original[latin], latin)
    add_failure('latin-misnamed', original[latin])
    add_failure('wrong-layout-name', original[pc])
    add_failure('empty-input', b'')
    add_failure('random-input', b'not a compressed layout')
    add_failure('truncated-zlib', original[standard][:-3])
    add_failure('trailing-zlib', original[standard] + b'unknown trailer')
    add_failure('oversize-compressed', bytes(8193))
    add_failure('oversize-expanded', zlib.compress(bytes(16385)))
    add_failure('trailing-expanded', zlib.compress(native.raw + b'x'))
    raw = bytearray(native.raw)
    struct.pack_into('>i', raw, 0, 2)
    add_failure('unknown-version', zlib.compress(raw))
    raw = bytearray(native.raw)
    struct.pack_into('>h', raw, period.start, 5)
    add_failure('foreign-period-key-type', zlib.compress(raw))
    raw = bytearray(native.raw)
    raw[vowel.start + 28] = 1
    add_failure('foreign-vowel-flags', zlib.compress(raw))
    raw = bytearray(native.raw)
    struct.pack_into('>d', raw, vowel.start + 12, float('nan'))
    add_failure('nan-key-geometry', zlib.compress(raw))
    raw = bytearray(native.raw)
    raw[8] = 2
    add_failure('invalid-native-boolean', zlib.compress(raw))
    raw = native.raw[:period.start] + period.replaced(v='？ ! 、') + native.raw[period.end:]
    add_failure('unrecognized-alternates', zlib.compress(raw))
    raw = native.raw[:period.start] + period.replaced(top='...') + native.raw[period.end:]
    add_failure('unrecognized-hint', zlib.compress(raw))
    raw = native.raw[:period.start] + period.replaced(r=marker, v='？', top='？') + native.raw[period.end:]
    add_failure('partially-normalized-layout', zlib.compress(raw))
    raw = native.raw[:period.start] + period.replaced(u='、') + native.raw[period.end:]
    add_failure('missing-period-variant', zlib.compress(raw))

    classes = work / 'classes'
    if classes.exists():
        shutil.rmtree(classes)
    classes.mkdir()
    stubs = {str(p.relative_to(root / 'tests/host_stubs')): p
             for p in (root / 'tests/host_stubs').rglob('*.java')}
    # New per-suite fakes override only their own class; shared stubs are not edited.
    stubs.update({str(p.relative_to(root / 'tests/punctuation_stubs')): p
                  for p in (root / 'tests/punctuation_stubs').rglob('*.java')})
    sources = sorted(stubs.values()) + sorted((root / 'tests/punctuation_host').glob('*.java'))
    sources += [production, flick]

    def run(name, command):
        result = subprocess.run(command, text=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT)
        (work / (name + '.log')).write_text(result.stdout)
        print(result.stdout, end='', flush=True)
        if result.returncode:
            raise RuntimeError(name + ' failed; see ' + str(work / (name + '.log')))
        return result.stdout

    run('compile', ['java', 'com.sun.tools.javac.Main', '--release', '8', '-encoding', 'UTF-8',
                    '-g:none', '-d', str(classes), *map(str, sources)])
    case_path = work / 'cases.tsv'
    case_path.write_text(''.join('\t'.join(map(str, row)) + '\n' for row in cases))
    text = run('transform', ['java', '-ea', '-cp', str(classes), 'PunctuationTransformProbe',
                             str(case_path), str(outputs)])
    transform_report = json.loads(next(line[7:] for line in text.splitlines() if line.startswith('REPORT ')))
    scenarios += transform_report['scenarios']
    assertions += transform_report['assertions']
    untouched_keys = 0
    targets_checked = 0
    row_variants = set()
    for case_id, name, mask in bitmask_cases:
        before = parse_layout((inputs / (case_id + '.zlib')).read_bytes())
        after = parse_layout((outputs / (case_id + '.zlib')).read_bytes())
        check(before.header == after.header and before.tail == after.tail,
              case_id + ': common layout fields unchanged')
        counts = {'。': 0, 'ー': 0}
        for before_row, after_row in zip(before.rows, after.rows):
            check(before_row.header == after_row.header, case_id + ': row header unchanged')
            check(len(before_row.keys) == len(after_row.keys), case_id + ': key count unchanged')
            for old, new in zip(before_row.keys, after_row.keys):
                if old.primary in counts:
                    alternate = '？' if old.primary == '。' else '！'
                    check(new.raw == old.replaced(r=marker, v=alternate, top=alternate),
                          case_id + ': only private tag, candidate and hint change')
                    check(new.primary == old.primary and new.shorts == old.shorts,
                          case_id + ': native key type and ordinary tap output retained')
                    check(new.popup.split(' ') == [alternate] and new.top == alternate,
                          case_id + ': sole fullwidth candidate equals hint')
                    counts[old.primary] += 1
                    targets_checked += 1
                    if old.primary == '。':
                        row_variants.add((name, before_row.kind))
                else:
                    check(new.raw == old.raw, case_id + ': unrelated key byte-for-byte unchanged')
                    untouched_keys += 1
        check(counts == ({'。': 1, 'ー': 1} if name == pc else {'。': 12, 'ー': 1}),
              case_id + ': all and only expected punctuation records normalized')
        vowel_after = next(k for k in after.keys if k.primary == 'ー')
        expected_column = 0 if name == pc or not mask & 2 else 8
        check(vowel_after.column == expected_column, case_id + ': long-vowel/backspace swap remains in chosen position')

    # Reapply every distinct normalized layout to prove the cache/marker gate is
    # stable without rerunning hundreds of equivalent settings permutations.
    seen, repeat_cases = set(), []
    for case_id, name, _ in bitmask_cases:
        data = (outputs / (case_id + '.zlib')).read_bytes()
        fingerprint = (name, sha256(data))
        if fingerprint in seen:
            continue
        seen.add(fingerprint)
        repeat_cases.append(('again-' + case_id, name, outputs / (case_id + '.zlib'), 'same'))
    repeat_path = work / 'repeat.tsv'
    repeat_path.write_text(''.join('\t'.join(map(str, row)) + '\n' for row in repeat_cases))
    repeat_text = run('idempotence', ['java', '-ea', '-cp', str(classes), 'PunctuationTransformProbe',
                                     str(repeat_path), str(work / 'repeat')])
    repeat_report = json.loads(next(line[7:] for line in repeat_text.splitlines() if line.startswith('REPORT ')))
    scenarios += repeat_report['scenarios']
    assertions += repeat_report['assertions']

    behavior = []
    for case_id in ['standard-000', 'standard-002', 'compact-000', 'compact-002', 'pc-000']:
        layout = parse_layout((outputs / (case_id + '.zlib')).read_bytes())
        for row in layout.rows:
            for key in row.keys:
                if key.primary not in ('。', 'ー'):
                    continue
                # The host geometry is explicit, only its left/right placement
                # follows the native key record. Pixel rendering is not claimed.
                left = min(.85, key.column / len(row.keys) * .9)
                behavior.append((case_id + '-r' + str(key.row), key.primary, key.popup,
                                 key.top, str(left), key.strings[1]))
    (work / 'behavior.tsv').write_text(''.join('\t'.join(row) + '\n' for row in behavior))
    host_text = run('behavior', ['java', '-ea', '-cp', str(classes), 'PunctuationKeysHostTest', str(work)])
    host_report = json.loads(next(line[7:] for line in host_text.splitlines() if line.startswith('REPORT ')))
    scenarios += host_report['scenarios']
    assertions += host_report['assertions']

    review = json.loads((root / 'tests/native_punctuation_review.json').read_text())
    check(review['status'] == 'STATIC_NATIVE_REVIEW_PASSED', 'native static review complete')
    check(review['production_source_sha256'] == sha256(production.read_bytes()),
          'production source must match final independent native review')
    report = {
        'status': 'PASSED', 'scenarios': scenarios, 'assertions': assertions,
        'production_source_sha256': sha256(production.read_bytes()),
        'flick_source_sha256': sha256(flick.read_bytes()),
        'fixture_manifest_sha256': sha256((fixtures / 'manifest.json').read_bytes()),
        'native_review_sha256': sha256((root / 'tests/native_punctuation_review.json').read_bytes()),
        'device_tested': False, 'android_instrumentation_tested': False,
        'scope': 'Production PunctuationKeys and byte-unchanged FlickSymbols on host Java fakes; '
                 'actual pinned native raw-layout fixtures; reviewed baseline KeySwap DEX algorithm port '
                 'and exact static tables; native construction verified statically, not executed on ART.',
        'all_settings_combinations': 256,
        'layout_setting_cases': len(bitmask_cases),
        'effective_unique_variants': unique_per_layout,
        'normalized_native_key_records_checked': targets_checked,
        'unrelated_native_key_records_byte_preserved': untouched_keys,
        'period_row_variants_checked': len(row_variants),
        'idempotent_distinct_layouts': len(repeat_cases),
        'native_field_behavior_fixtures': len(behavior),
        'raw_type_and_tap_output_unchanged': True,
        'sole_candidate_and_small_label_match': True,
        'native_period_primary_prepend_bypassed': True,
        'native_language_variant_append_bypassed': True,
        'popup_scope_and_failure_fallback_tested': True,
        'wrapper_fallback_cursor_and_cache_namespace_tested': True,
        'production_flick_geometry_and_capture_constants_unchanged': True,
        'baseline_keyswap_executed_as_dex': False,
        'baseline_keyswap_validation': 'Exact pinned DEX tables loaded mechanically; reviewed host port '
                                      'executes its row-byte swaps, width adjustment and arrow permutation.',
        'limitations': [
            'Host fakes model inspected native constructor and actor-field shapes; they do not execute Android resources, rendering or the fluency engine.',
            'Ordinary-tap forwarding and longpress candidate selection are modeled; native timer scheduling and popup touch routing require device confirmation.',
            'The full upstream gesture suite is intentionally not rerun; this suite tests only the two normalized native routes and existing recognition boundaries.'
        ]
    }
    (work / 'report.json').write_text(json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
    print('PASSED punctuation: %d scenarios / %d assertions; 768 layout/settings combinations' % (scenarios, assertions))


if __name__ == '__main__':
    main()
