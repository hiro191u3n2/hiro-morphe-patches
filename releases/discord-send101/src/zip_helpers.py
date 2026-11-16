#!/usr/bin/env python3
"""Manifest and ZIP helpers; preserve unchanged compressed baseline records."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import subprocess
import struct
import zipfile

MF = 'META-INF/MANIFEST.MF'

def require(value, why):
    if not value:
        raise RuntimeError(why)

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def archive(path):
    with zipfile.ZipFile(path) as z:
        require(len(z.namelist()) == len(set(z.namelist())) and z.testzip() is None, 'Invalid archive')
        require(all(not PurePosixPath(n).is_absolute() and '..' not in PurePosixPath(n).parts and '\\' not in n for n in z.namelist()), 'Unsafe entry')
        return {n: z.read(n) for n in z.namelist() if not n.endswith('/')}

def write_zip(path, entries):
    with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for name, raw in sorted(entries.items(), key=lambda row: (row[0] != MF, row[0] != 'classes.dex', row[0])):
            info = zipfile.ZipInfo(name, (2026, 11, 15, 0, 0, 0))
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            z.writestr(info, raw, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)

def zip_records(path):
    """Read complete local records and their unmodified central-directory metadata."""
    raw = path.read_bytes()
    with zipfile.ZipFile(path) as z:
        infos = sorted(z.infolist(), key=lambda item: item.header_offset)
        local = {info.filename: raw[info.header_offset:(infos[i + 1].header_offset if i + 1 < len(infos) else z.start_dir)]
                 for i, info in enumerate(infos)}
        central, position = {}, z.start_dir
        for info in z.infolist():
            require(raw[position:position + 4] == b'PK\x01\x02', 'Invalid central directory')
            name_bytes, extra_bytes, comment_bytes = struct.unpack_from('<HHH', raw, position + 28)
            end = position + 46 + name_bytes + extra_bytes + comment_bytes
            record = raw[position:end]
            flags = struct.unpack_from('<H', record, 8)[0]
            name = record[46:46 + name_bytes].decode('utf-8' if flags & 0x800 else 'cp437')
            require(name == info.filename and name not in central, 'Central directory name differs')
            require(struct.unpack_from('<L', record, 42)[0] == info.header_offset, 'ZIP64/invalid offset is unsupported')
            central[name] = record
            position = end
        require(raw[position:position + 4] == b'PK\x05\x06', 'Unexpected ZIP tail')
    return local, central

def write_combined_zip(path, entries, baseline, base_entries, temporary):
    """Keep unchanged compressed records byte-identical for immutable delivery deltas."""
    original_local, original_central = zip_records(baseline)
    changed = {n: b for n, b in entries.items() if n not in base_entries or b != base_entries[n]}
    write_zip(temporary, changed)
    fresh_local, fresh_central = zip_records(temporary)
    offset, central, retained = 0, bytearray(), 0
    require(len(entries) < 65535, 'ZIP64 entry count unsupported')
    with path.open('wb') as out:
        for name in sorted(entries, key=lambda n: (n != MF, n != 'classes.dex', n)):
            is_fresh = name in changed
            local = (fresh_local if is_fresh else original_local)[name]
            record = bytearray((fresh_central if is_fresh else original_central)[name])
            require(offset < 0xffffffff, 'ZIP64 offset unsupported')
            struct.pack_into('<L', record, 42, offset)
            out.write(local)
            central.extend(record)
            offset += len(local)
            retained += not is_fresh
        out.write(central)
        out.write(struct.pack('<4s4H2LH', b'PK\x05\x06', 0, 0, len(entries), len(entries), len(central), offset, 0))
    written_local, _ = zip_records(path)
    require(all(written_local[n] == original_local[n] for n in entries if n not in changed), 'Unchanged compressed record differs')
    require(archive(path) == entries, 'Raw-record ZIP roundtrip differs')
    return retained

def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            require(bool(rows), 'Invalid manifest continuation')
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    pairs = [line.decode().split(': ', 1) for line in rows]
    require(all(len(pair) == 2 for pair in pairs) and len(pairs) == len(dict(pairs)), 'Invalid manifest headers')
    return dict(pairs)

def manifest(fields):
    out = bytearray()
    for key, value in fields.items():
        require(not any(c in value for c in '\r\n\0'), 'Invalid manifest value')
        row = (key + ': ' + value).encode()
        while len(row) > 70:
            end = 70
            while row[end] & 0xc0 == 0x80:
                end -= 1
            out += row[:end] + b'\r\n'
            row = b' ' + row[end:]
        out += row + b'\r\n'
    raw = bytes(out) + b'\r\n'
    require(headers(raw) == fields, 'Manifest roundtrip failed')
    return raw
