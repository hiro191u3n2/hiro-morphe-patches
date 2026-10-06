#!/usr/bin/env python3
"""Trip.com 8.54.2: remove the My Plan scenic heading, keeping flight layout.

The baseline is the exact v1.10.13/v1.10.14 payload retained in bundle161.
Only indexed modules 666761 and 667189 change; the complete flight renderer,
clock/localization helpers, resources, byte offsets, and trailer stay intact.

Inputs/outputs ending in .7z use pinned py7zr==1.0.0 and standard LZMA2.
Other inputs/outputs are raw rn_business.jsbundle files. Example:
  python patch_myplan.py old.7z new.7z --bundle-output rn_business.jsbundle \
      --report js-build.json
"""
from __future__ import annotations

import argparse
import ctypes as ct
import ctypes.util
import hashlib
import json
import os
import re
import tempfile
from pathlib import Path

MEMBER = 'rn_xtaro_ibu_schedule/rn_business.jsbundle'
BASELINE_SHA256 = 'fde4c6689131987b3f0b355ee7c0f836f93757c7cffa6632debc3b3046c5fba0'
BASELINE_ARCHIVE_SHA256 = 'b4ee8ad8555d0541aba178afa953a341a915a8cddbebd67a63ee206c5c7f0e2d'
EXPECTED_SIZE = 971104
TARGET_MODULES = (666761, 667189)
PRESERVED_CLOCK_MODULES = (666817, 666790, 666755, 666756, 666726, 666788)
TITLE_START = b'_e.CmtTitle=function(e){'
TITLE_END = b'},_e.CityTitle=function(e){'
HIDDEN_TITLE = b'_e.CmtTitle=function(e){/*hiro-myplan-banner-hidden-v1.10.15*/return null'
OLD_TOP_MARGIN = b'style:{marginTop:22+(0===t?0:8)}'
NEW_TOP_MARGIN = b'style:{marginTop:0===t?0:8}'


def require(ok: bool, message: str) -> None:
    if not ok:
        raise RuntimeError(message)


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def indexes(data: bytes):
    tail = re.search(rb'\n(\d{10}),(\d{10}),(\d{10}),(\d{10})$', data)
    require(tail is not None, 'Missing indexed CRN trailer')
    module_offset, module_size, resource_offset, resource_size = map(int, tail.groups())
    require(module_offset + module_size <= resource_offset, 'Overlapping CRN indexes')
    require(resource_offset + resource_size <= tail.start(), 'Invalid resource index range')
    module_table = data[module_offset:module_offset + module_size]
    resource_table = data[resource_offset:resource_offset + resource_size]
    require(module_table.startswith(b'/*module*/'), 'Missing module index marker')
    require(resource_table.startswith(b'/*resource*/'), 'Missing resource index marker')
    rows = re.findall(rb'M(\d+)A,(\d+),(\d+);', module_table)
    ranges = {int(mid): (int(offset), int(size)) for mid, offset, size in rows}
    require(len(rows) == len(ranges) and bool(rows), 'Empty or duplicate CRN module index')
    ordered = sorted(ranges.values())
    require(all(size > 0 and offset + size <= module_offset for offset, size in ordered),
            'Invalid CRN module range')
    require(all(a + size <= b for (a, size), (b, _) in zip(ordered, ordered[1:])),
            'Overlapping CRN modules')
    resource_rows = re.findall(rb',([0-9]+),([0-9]+);', resource_table)
    require(bool(resource_rows), 'Missing embedded resource ranges')
    resource_start = min(int(offset) for offset, _ in resource_rows)
    require(max(offset + size for offset, size in ordered) <= resource_start,
            'Resources overlap JS modules')
    return ranges, (module_offset, module_size, resource_offset, resource_size), resource_start


def replace_once(data: bytes, old: bytes, new: bytes, label: str) -> bytes:
    require(data.count(old) == 1, 'Anchor not unique: ' + label)
    return data.replace(old, new, 1)


def padded(data: bytes, size: int) -> bytes:
    require(len(data) <= size, f'Module exceeds offset budget: {len(data)} > {size}')
    return data + b' ' * (size - len(data))


def patch_bundle(data: bytes):
    require(len(data) == EXPECTED_SIZE and sha(data) == BASELINE_SHA256,
            'Unexpected My Plan baseline; expected Trip v1.10.14 payload from bundle161')
    ranges, tables, resource_start = indexes(data)
    result = bytearray(data)
    before = {}
    after = {}
    for mid in TARGET_MODULES:
        offset, size = ranges[mid]
        module = data[offset:offset + size]
        before[str(mid)] = sha(module)
        if mid == 667189:
            require(module.count(TITLE_START) == 1 and module.count(TITLE_END) == 1,
                    'CmtTitle/CityTitle boundaries are not unique')
            start = module.index(TITLE_START)
            end = module.index(TITLE_END, start)
            removed = module[start:end]
            for token in (b'height:120', b'e.imageUrl', b'e.title',
                          b'ibu_pub_app_mytrips_trip_module_load'):
                require(token in removed, 'Unexpected scenic title renderer')
            # The final brace belongs to TITLE_END. All following exports retain
            # their original bytecode. No network callback or view is constructed.
            changed = module[:start] + HIDDEN_TITLE + module[end:]
            require(changed[changed.index(TITLE_END):].rstrip() ==
                    module[end:].rstrip(), 'Non-scenic timeline export changed')
        else:
            # CmtTitle's group contributed 22px above its 120px card. Preserve
            # CityTitle's own 20px gap and the extra 8px between trip groups.
            changed = replace_once(module, OLD_TOP_MARGIN, NEW_TOP_MARGIN, 'banner group margin')
        payload = padded(changed, size)
        result[offset:offset + size] = payload
        after[str(mid)] = sha(payload)
    output = bytes(result)
    require(indexes(output) == (ranges, tables, resource_start), 'CRN indexes changed')
    require(len(output) == len(data), 'CRN byte length changed')
    changes = {mid for mid, (offset, size) in ranges.items()
               if output[offset:offset + size] != data[offset:offset + size]}
    require(changes == set(TARGET_MODULES), f'Unexpected modified modules: {changes}')
    # Verify every byte outside target intervals, including prelude/gaps/images.
    cursor = 0
    for offset, size in sorted(ranges[mid] for mid in TARGET_MODULES):
        require(output[cursor:offset] == data[cursor:offset], 'Unexpected non-target byte change')
        cursor = offset + size
    require(output[cursor:] == data[cursor:], 'Unexpected non-target tail change')
    clocks = {str(mid): sha(data[ranges[mid][0]:sum(ranges[mid])])
              for mid in PRESERVED_CLOCK_MODULES}
    return output, {
        'schema': 'trip-myplan-js-v1.10.15',
        'baseline_sha256': BASELINE_SHA256,
        'patched_sha256': sha(output),
        'bundle_bytes': len(output),
        'changed_modules': list(TARGET_MODULES),
        'unchanged_module_count': len(ranges) - len(changes),
        'module_ranges': {str(mid): list(ranges[mid]) for mid in TARGET_MODULES},
        'before_module_sha256': before,
        'after_module_sha256': after,
        'preserved_clock_module_sha256': clocks,
        'module_and_resource_indexes_unchanged': True,
        'all_non_target_bytes_unchanged': True,
        'resource_payload_start': resource_start,
        'resource_and_index_tail_sha256': sha(data[resource_start:]),
        'flight_time_layout': 'Exact v1.10.13 full-width clocks/separate duration retained',
        'scenic_title': 'CmtTitle returns null; no image, gradient, label, link, or card height',
        'timeline_gap': 'Remove scenic group extra22px; CityTitle20px and intergroup8px retained',
        'runtime_cache_selection': 'Separate native patch; changing this archive alone is insufficient',
        'android_rendering_tested': False,
    }


def archive_api():
    name = ctypes.util.find_library('archive')
    require(name is not None, 'libarchive is required for .7z input/output; raw JS also supported')
    lib = ct.CDLL(name)
    signatures = {
        'archive_read_new': (ct.c_void_p, []),
        'archive_read_support_format_7zip': (ct.c_int, [ct.c_void_p]),
        'archive_read_support_filter_all': (ct.c_int, [ct.c_void_p]),
        'archive_read_open_filename': (ct.c_int, [ct.c_void_p, ct.c_char_p, ct.c_size_t]),
        'archive_read_next_header': (ct.c_int, [ct.c_void_p, ct.POINTER(ct.c_void_p)]),
        'archive_entry_pathname': (ct.c_char_p, [ct.c_void_p]),
        'archive_entry_size': (ct.c_longlong, [ct.c_void_p]),
        'archive_read_data': (ct.c_longlong, [ct.c_void_p, ct.c_void_p, ct.c_size_t]),
        'archive_read_free': (ct.c_int, [ct.c_void_p]),
        'archive_error_string': (ct.c_char_p, [ct.c_void_p]),
    }
    for symbol, (restype, argtypes) in signatures.items():
        function = getattr(lib, symbol)
        function.restype, function.argtypes = restype, argtypes
    return lib


def read_archive_libarchive(path: Path) -> bytes:
    """Independent optional reader for build QA; packaging itself uses py7zr."""
    lib = archive_api()
    handle = lib.archive_read_new()
    require(bool(handle), 'Unable to allocate archive reader')
    try:
        require(lib.archive_read_support_format_7zip(handle) == 0, '7z reader unavailable')
        require(lib.archive_read_support_filter_all(handle) == 0, 'Archive filters unavailable')
        require(lib.archive_read_open_filename(handle, str(path).encode(), 65536) == 0,
                'Cannot open 7z input')
        entry = ct.c_void_p()
        require(lib.archive_read_next_header(handle, ct.byref(entry)) == 0, 'Missing 7z member')
        require(lib.archive_entry_pathname(entry) == MEMBER.encode(), 'Unexpected 7z member name')
        size = lib.archive_entry_size(entry)
        require(size == EXPECTED_SIZE, 'Unexpected 7z member size')
        data = bytearray()
        buffer = ct.create_string_buffer(65536)
        while True:
            count = lib.archive_read_data(handle, buffer, len(buffer))
            require(count >= 0, '7z member read failed')
            if count == 0:
                break
            data.extend(buffer.raw[:count])
            require(len(data) <= EXPECTED_SIZE, 'Oversized 7z member')
        require(len(data) == size, 'Truncated 7z member')
        require(lib.archive_read_next_header(handle, ct.byref(entry)) == 1,
                'Unexpected additional 7z member')
        return bytes(data)
    finally:
        lib.archive_read_free(handle)


def py7zr_api():
    import py7zr
    require(py7zr.__version__ == '1.0.0', 'Reproducible archive build requires py7zr==1.0.0')
    return py7zr


def read_archive(path: Path) -> bytes:
    py7zr = py7zr_api()
    with tempfile.TemporaryDirectory(prefix='trip-myplan-read-') as directory:
        with py7zr.SevenZipFile(path, 'r') as archive:
            require(archive.getnames() == [MEMBER], 'Unexpected 7z members')
            info = archive.list()[0]
            require(info.uncompressed == EXPECTED_SIZE and not info.is_directory,
                    'Unexpected archive member size or type')
            archive.extractall(directory)
        return (Path(directory) / MEMBER).read_bytes()


def write_archive(path: Path, data: bytes) -> None:
    py7zr = py7zr_api()
    with tempfile.TemporaryDirectory(prefix='trip-myplan-write-') as directory:
        member = Path(directory) / 'rn_business.jsbundle'
        member.write_bytes(data)
        member.chmod(0o644)
        os.utime(member, (946684800, 946684800))
        with py7zr.SevenZipFile(path, 'w', filters=[{'id': py7zr.FILTER_LZMA2, 'preset': 7}]) as archive:
            archive.write(member, arcname=MEMBER)
    require(read_archive(path) == data, '7z roundtrip differs')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('input', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('--bundle-output', type=Path)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    require(args.input.resolve() != args.output.resolve(), 'Use a separate output path')
    archive_input = args.input.suffix.lower() == '.7z'
    raw = args.input.read_bytes()
    if archive_input:
        require(sha(raw) == BASELINE_ARCHIVE_SHA256, 'Unexpected baseline7z checksum')
    original = read_archive(args.input) if archive_input else raw
    patched, report = patch_bundle(original)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.suffix.lower() == '.7z':
        write_archive(args.output, patched)
        report['archive_bytes'] = args.output.stat().st_size
        report['archive_sha256'] = sha(args.output.read_bytes())
        report['archive_writer'] = 'py7zr1.0.0; LZMA2 preset7; mtime2000-01-01; mode0644'
    else:
        args.output.write_bytes(patched)
    if args.bundle_output:
        args.bundle_output.parent.mkdir(parents=True, exist_ok=True)
        args.bundle_output.write_bytes(patched)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
