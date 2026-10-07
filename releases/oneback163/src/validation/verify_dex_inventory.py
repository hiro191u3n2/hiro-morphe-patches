#!/usr/bin/env python3
"""Verify APK/DEX headers, hashes, uniqueness and preservation of every original class.

Usage: verify_dex_inventory.py ORIGINAL_APK PATCHED_APK REPORT_JSON
Optional: --allow-added-class 'Lfully/qualified/Class;' (repeat for each approved legacy addition).
Successful OneBack output must add all and only its exact 19 runtime classes, plus
individually permitted additions. Any damaged ZIP/DEX, duplicate definition,
missing original class or unapproved added class produces FAIL and exit code 1.
Only class identities are inspected; no source code or disassembly is emitted.
"""
import argparse
import collections
import hashlib
import json
import os
from pathlib import Path
import re
import struct
import sys
import zipfile
import zlib


# Exact runtime ABI payload from the compiled OneBackExit extension: no prefix wildcard.
ONEBACK_CLASSES = frozenset(
    ['Lapp/hiro/oneback/runtime/OneBackExit;']
    + ['Lapp/hiro/oneback/runtime/OneBackExit$%d;' % i for i in range(1, 9)]
    + ['Lapp/hiro/oneback/runtime/OneBackExit$%s;' % suffix for suffix in (
        'Api29', 'Api33', 'Api33$Registration', 'Api33$Registration$1', 'Lifecycle',
        'Root', 'Root$1', 'Root$2', 'Scope', 'State')]
)
FORMAT_REFERENCE = 'https://source.android.com/docs/core/runtime/dex-format'


def digest_names(names):
    return hashlib.sha256(('\n'.join(sorted(names)) + '\n').encode('utf-8', 'surrogatepass')).hexdigest()


def dex_inventory(data, name):
    errors = []
    def require(ok, message):
        if not ok and message not in errors:
            errors.append(message)
    def u4(offset):
        if offset < 0 or offset + 4 > len(data):
            raise ValueError('u32 outside DEX: %s at %d' % (name, offset))
        return struct.unpack_from('<I', data, offset)[0]
    if len(data) < 112:
        raise ValueError('Truncated DEX header: ' + name)
    require(data[:4] == b'dex\n' and data[7] == 0, 'DEX magic mismatch')
    require(u4(0x20) == len(data), 'header file_size differs from ZIP entry bytes')
    require(u4(0x24) == 112, 'unexpected DEX header_size')
    require(u4(0x28) == 0x12345678, 'unexpected endian_tag')
    signature = hashlib.sha1(data[32:]).hexdigest()
    checksum = zlib.adler32(data[12:]) & 0xffffffff
    require(signature == data[12:32].hex(), 'DEX SHA-1 signature mismatch')
    require(checksum == u4(8), 'DEX Adler-32 checksum mismatch')
    sizes = {}
    for label, at, unit in [('string_ids', 0x38, 4), ('type_ids', 0x40, 4),
                           ('proto_ids', 0x48, 12), ('field_ids', 0x50, 8),
                           ('method_ids', 0x58, 8), ('class_defs', 0x60, 32)]:
        count, offset = u4(at), u4(at + 4)
        require((count == 0 and offset == 0) or (offset >= 112 and offset % 4 == 0
            and offset + count * unit <= len(data)), label + ' section bounds/alignment invalid')
        sizes[label] = (count, offset)
    string_count, string_offset = sizes['string_ids']
    type_count, type_offset = sizes['type_ids']
    class_count, class_offset = sizes['class_defs']
    require(u4(0x68) + u4(0x6c) == len(data), 'data section bounds inconsistent')

    map_offset = u4(0x34)
    map_class_defs = []
    if 0 < map_offset <= len(data) - 4:
        map_count = u4(map_offset)
        require(map_offset + 4 + map_count * 12 <= len(data), 'map_list truncated')
        for i in range(map_count):
            at = map_offset + 4 + 12 * i
            kind, unused, count, offset = struct.unpack_from('<HHII', data, at)
            if kind == 0x0006:
                map_class_defs.append({'count': count, 'offset': offset})
        require(map_class_defs == [{'count': class_count, 'offset': class_offset}]
            if class_count else not map_class_defs, 'map class_def count/offset differs from header')
    else:
        errors.append('map_list offset invalid')

    cache = {}
    def string(index):
        if index in cache:
            return cache[index]
        if not 0 <= index < string_count:
            raise ValueError('string index outside table: %s %d' % (name, index))
        offset = u4(string_offset + 4 * index)
        declared = 0
        shift = 0
        for _ in range(5):
            value = data[offset]
            offset += 1
            declared |= (value & 127) << shift
            if not value & 128:
                break
            shift += 7
        else:
            raise ValueError('Invalid string length ULEB128: ' + name)
        end = data.index(0, offset)
        value = data[offset:end].replace(b'\xc0\x80', b'\x00').decode('utf-8', 'surrogatepass')
        require(len(value.encode('utf-16le', 'surrogatepass')) // 2 == declared, 'descriptor UTF-16 string length mismatch')
        cache[index] = value
        return value

    classes = []
    for i in range(class_count):
        at = class_offset + 32 * i
        index = u4(at)
        # DEX class_defs are dependency-ordered, not sorted by class_idx.
        # AOSP dex-format File layout/class_defs requires unique definitions.
        if index >= type_count:
            raise ValueError('class_idx outside type_ids: %s %d' % (name, index))
        descriptor = string(u4(type_offset + 4 * index))
        require(descriptor.startswith('L') and descriptor.endswith(';') and '\x00' not in descriptor, 'invalid class descriptor')
        classes.append(descriptor)
        superclass, source = u4(at + 8), u4(at + 16)
        require(superclass == 0xffffffff or superclass < type_count, 'superclass index outside type_ids')
        require(source == 0xffffffff or source < string_count, 'source_file index outside string_ids')
        for field in (12, 20, 24, 28):
            offset = u4(at + field)
            require(offset == 0 or 112 <= offset < len(data), 'class_def data offset outside DEX')
    duplicates = [key for key, count in collections.Counter(classes).items() if count > 1]
    require(not duplicates, 'duplicate class descriptors within DEX')
    return {
        'name': name, 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest(),
        'header_hex': data[:112].hex(), 'header_all_zero': not any(data[:112]),
        'magic': data[:8].decode('ascii'), 'header_size': u4(0x24), 'header_file_size': u4(0x20),
        'header_class_count': class_count, 'parsed_class_count': len(classes),
        'unique_class_count': len(set(classes)), 'map_class_defs': map_class_defs,
        'string_count': string_count, 'type_count': type_count,
        'class_descriptor_set_sha256': digest_names(classes),
        'dex_sha1_signature': data[12:32].hex(), 'calculated_sha1_signature': signature,
        'dex_adler32': '%08x' % u4(8), 'calculated_adler32': '%08x' % checksum,
        'class_duplicates': duplicates, 'errors': errors,
    }, classes


def apk_inventory(path):
    path = Path(path).resolve()
    classes = collections.defaultdict(list)
    entries = []
    with path.open('rb') as handle:
        before = os.fstat(handle.fileno())
        sha256 = hashlib.file_digest(handle, 'sha256').hexdigest()
        handle.seek(0)
        with zipfile.ZipFile(handle) as archive:
            infos = archive.infolist()
            counts = collections.Counter(info.filename for info in infos)
            duplicates = {name: count for name, count in counts.items() if count > 1}
            dex_infos = [info for info in infos if re.fullmatch(r'classes(?:[0-9]+)?\.dex', info.filename)]
            dex_infos.sort(key=lambda info: (1 if info.filename == 'classes.dex'
                else int(info.filename[7:-4]), info.header_offset))
            for info in dex_infos:
                data = archive.read(info)  # ZipFile checks this entry's CRC-32 while reading it.
                report, definitions = dex_inventory(data, info.filename)
                report.update({'zip_crc32': '%08x' % info.CRC, 'calculated_crc32': '%08x' % (zlib.crc32(data) & 0xffffffff),
                    'zip_uncompressed_size': info.file_size, 'zip_compressed_size': info.compress_size,
                    'zip_local_header_offset': info.header_offset})
                entries.append(report)
                for descriptor in definitions:
                    classes[descriptor].append(info.filename)
        after = os.fstat(handle.fileno())
    current = path.stat()
    unchanged = (before.st_size, before.st_mtime_ns, before.st_ino) == (after.st_size, after.st_mtime_ns, after.st_ino)
    unchanged &= (after.st_size, after.st_mtime_ns, after.st_ino) == (current.st_size, current.st_mtime_ns, current.st_ino)
    duplicate_classes = {name: locations for name, locations in classes.items() if len(locations) > 1}
    errors = [entry['name'] + ': ' + error for entry in entries for error in entry['errors']]
    if duplicates: errors.append('duplicate ZIP entry names')
    if duplicate_classes: errors.append('duplicate class definitions across DEX entries')
    if not unchanged: errors.append('APK changed while inventory was read')
    if not entries: errors.append('No root multidex entries')
    result = {
        'path': str(path), 'bytes': before.st_size, 'sha256': sha256, 'unchanged_during_read': unchanged,
        'zip_entry_count': len(infos), 'duplicate_zip_entries': duplicates,
        'dex_entry_count': len(entries), 'dex_entries': entries,
        'header_class_count_sum': sum(entry['header_class_count'] for entry in entries),
        'parsed_class_count_sum': sum(entry['parsed_class_count'] for entry in entries),
        'unique_class_count': len(classes), 'duplicate_class_definitions': duplicate_classes,
        'class_descriptor_set_sha256': digest_names(classes), 'errors': errors,
        'result': 'PASS' if not errors else 'FAIL',
    }
    return result, classes


def checked_apk_inventory(path):
    try:
        return apk_inventory(path)
    except Exception as error:
        resolved = Path(path).resolve()
        sha256 = None
        size = None
        try:
            size = resolved.stat().st_size
            with resolved.open('rb') as handle:
                sha256 = hashlib.file_digest(handle, 'sha256').hexdigest()
        except Exception:
            pass
        return {'path': str(resolved), 'bytes': size, 'sha256': sha256,
            'result': 'FAIL', 'inventory_complete': False, 'unique_class_count': 0,
            'dex_entries': [], 'errors': [type(error).__name__ + ': ' + str(error)]}, {}


def comparison(original, emitted, additionally_allowed):
    before_report, before = original
    after_report, after = emitted
    old, new = set(before), set(after)
    missing, added = old - new, new - old
    allowed = ONEBACK_CLASSES | set(additionally_allowed)
    unexpected = added - allowed
    missing_runtime_additions = ONEBACK_CLASSES - added
    missing_from = collections.Counter(location for name in missing for location in before[name])
    by_dex = []
    for entry in before_report['dex_entries']:
        names = {name for name, locations in before.items() if entry['name'] in locations}
        gone = names - new
        by_dex.append({'original_dex': entry['name'], 'original_class_count': len(names),
            'retained_class_count': len(names & new), 'missing_class_count': len(gone),
            'original_dex_sha256': entry['sha256'], 'missing_descriptor_set_sha256': digest_names(gone)})
    findings = ['original APK: ' + value for value in before_report['errors']]
    findings += ['patched APK: ' + value for value in after_report['errors']]
    if missing:
        findings.append('Original class definitions missing from emitted APK: %d' % len(missing))
    if unexpected:
        findings.append('Unexpected added class definitions: %d' % len(unexpected))
    if missing_runtime_additions:
        findings.append('Required OneBack runtime additions missing: %d' % len(missing_runtime_additions))
    return {
        'result': 'PASS' if not findings else 'FAIL', 'blocking_findings': findings,
        'original_count': len(old), 'emitted_count': len(new), 'retained_count': len(old & new),
        'missing_count': len(missing), 'added_count': len(added),
        'missing_by_original_dex': dict(missing_from),
        'original_dex_class_retention': by_dex,
        'missing_descriptor_set_sha256': digest_names(missing),
        'added_descriptor_set_sha256': digest_names(added),
        'unexpected_added_count': len(unexpected),
        'unexpected_added_examples': sorted(unexpected)[:20],
        'missing_oneback_additions': sorted(missing_runtime_additions),
        'required_oneback_runtime_classes': sorted(ONEBACK_CLASSES),
        'explicit_additional_allowed_classes': sorted(additionally_allowed),
        'explicit_additional_classes_added': sorted(added & set(additionally_allowed)),
        'no_original_classes_missing': not missing,
        'only_explicitly_allowed_classes_added': not unexpected,
        'all_19_oneback_classes_added': not missing_runtime_additions,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('original_apk')
    parser.add_argument('patched_apk')
    parser.add_argument('report_json')
    parser.add_argument('--allow-added-class', action='append', default=[], metavar='EXACT_DESCRIPTOR',
        help='Explicit additional class allowed for a combined legacy patch; repeat once per class. No wildcards.')
    args = parser.parse_args()
    for descriptor in args.allow_added_class:
        if not re.fullmatch(r'L[^;\s*?]+;', descriptor):
            parser.error('--allow-added-class requires an exact Lpackage/Class; descriptor')
    destination = Path(args.report_json).resolve()
    original_path, patched_path = Path(args.original_apk).resolve(), Path(args.patched_apk).resolve()
    if destination in (original_path, patched_path):
        parser.error('report path must not overwrite an APK')
    if destination.exists():
        for path in (original_path, patched_path):
            if path.exists() and os.path.samefile(destination, path):
                parser.error('report path must not overwrite an APK through an alias')
    before, after = checked_apk_inventory(original_path), checked_apk_inventory(patched_path)
    result = comparison(before, after, set(args.allow_added_class))
    report = {
        'schema_version': 1,
        'validation': 'Independent stdlib ZIP and raw DEX class_def preservation audit',
        'format_reference': FORMAT_REFERENCE,
        'verifier_source_sha256': hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
        'result': result['result'], 'blocking_findings': result['blocking_findings'],
        'original': before[0], 'patched': after[0], 'comparison': result,
        'scope': 'APK/DEX integrity and class-definition inventory; method behavior is checked by the separate DEX audit.',
        'physical_device_tested': False,
    }
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(report, ensure_ascii=True, indent=2) + '\n')
    print(json.dumps({'result': result['result'], 'original_count': result['original_count'],
        'emitted_count': result['emitted_count'], 'missing_count': result['missing_count'],
        'added_count': result['added_count'], 'blocking_findings': result['blocking_findings'],
        'report': str(destination)}), flush=True)
    return 0 if result['result'] == 'PASS' else 1


if __name__ == '__main__':
    sys.exit(main())
