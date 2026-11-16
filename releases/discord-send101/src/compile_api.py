"""Expose Kotlin synthetic patch constructors to ECJ in a compile-only API copy.

The original SDK is the runtime. This directory must never enter the MPP.
"""
from pathlib import Path
import struct
import sys
import zipfile

source, destination = Path(sys.argv[1]), Path(sys.argv[2])
with zipfile.ZipFile(source) as archive:
    for name in ('RawResourcePatch', 'ResourcePatch', 'BytecodePatch'):
        path = 'app/morphe/patcher/patch/' + name + '.class'
        data = bytearray(archive.read(path))
        u2 = lambda p: struct.unpack_from('>H', data, p)[0]
        u4 = lambda p: struct.unpack_from('>I', data, p)[0]
        count, p, i, utf = u2(8), 10, 1, {}
        while i < count:
            tag = data[p]
            p += 1
            if tag == 1:
                length = u2(p)
                utf[i] = bytes(data[p + 2:p + 2 + length]).decode('utf-8', 'replace')
                p += 2 + length
            elif tag in (3, 4): p += 4
            elif tag in (5, 6): p += 8; i += 1
            elif tag in (7, 8, 16, 19, 20): p += 2
            elif tag in (9, 10, 11, 12, 17, 18): p += 4
            elif tag == 15: p += 3
            else: raise ValueError('Unknown constant-pool tag: ' + str(tag))
            i += 1
        p += 6
        p += 2 + 2 * u2(p)
        changed = 0
        for section in range(2):
            members = u2(p)
            p += 2
            for _ in range(members):
                flags, member_name, attributes = u2(p), utf[u2(p + 2)], u2(p + 6)
                if section == 1 and member_name == '<init>' and flags & 0x1000:
                    struct.pack_into('>H', data, p, flags & ~0x1000)
                    changed += 1
                p += 8
                for __ in range(attributes): p += 6 + u4(p + 2)
        target = destination / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        print(name + ': exposed synthetic constructors ' + str(changed))
