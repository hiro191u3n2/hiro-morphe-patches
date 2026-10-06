#!/usr/bin/env python3
"""Redirect exactly two Home document calls; preserve every existing JVM attribute."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import zipfile

ENTRY = 'app/hiro/tripcom/patches/HideHomeRecommendationsPatchKt$hideHomeRecommendationsPatch$1$1.class'
BASE_SHA = '3cd3b88ce140656c19f57e6ffc576c715c5057e841de9b51deab3d83d84528ab'
CONTEXT = 'app/morphe/patcher/patch/ResourcePatchContext'
HELPER = 'app/hiro/tripcom/patches/HomeResourceDocuments'
DOCUMENT = 'Lapp/morphe/patcher/util/Document;'


def require(ok, message):
    if not ok:
        raise ValueError(message)


def u2(data, pos):
    return int.from_bytes(data[pos:pos + 2], 'big')


def u4(data, pos):
    return int.from_bytes(data[pos:pos + 4], 'big')


def pool(data):
    require(data[:4] == bytes.fromhex('cafebabe'), 'Invalid JVM class')
    count, pos, index = u2(data, 8), 10, 1
    entries, utf = {}, {}
    while index < count:
        start, tag = pos, data[pos]
        pos += 1
        if tag == 1:
            size = u2(data, pos)
            utf[index] = data[pos + 2:pos + 2 + size]
            pos += 2 + size
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            pos += 4
        elif tag in (7, 8, 16, 19, 20):
            pos += 2
        elif tag == 15:
            pos += 3
        elif tag in (5, 6):
            pos += 8
        else:
            raise ValueError('Unsupported constant-pool tag')
        entries[index] = data[start:pos]
        index += 2 if tag in (5, 6) else 1
    return count, pos, entries, utf


def transform(old):
    require(hashlib.sha256(old).hexdigest() == BASE_SHA, 'Exact bundle161 Home JVM baseline differs')
    count, end, entries, utf = pool(old)

    def utf_index(value):
        matches = [n for n, b in utf.items() if b == value.encode()]
        require(len(matches) == 1, 'Expected unique UTF-8 constant: ' + value)
        return matches[0]

    def unique(tag, payload):
        matches = [n for n, b in entries.items() if b == bytes([tag]) + payload]
        require(len(matches) == 1, 'Expected unique JVM constant')
        return matches[0]

    owner = unique(7, struct.pack('>H', utf_index(CONTEXT)))
    signature = unique(12, struct.pack('>HH', utf_index('document'),
                                      utf_index('(Ljava/lang/String;)' + DOCUMENT)))
    old_ref = unique(10, struct.pack('>HH', owner, signature))
    additions = []

    def add(tag, payload):
        value = bytes([tag]) + payload
        for n, entry in entries.items():
            if entry == value:
                return n
        for n, entry in additions:
            if entry == value:
                return n
        n = count + len(additions)
        additions.append((n, value))
        return n

    def string(value):
        value = value.encode()
        return add(1, struct.pack('>H', len(value)) + value)

    helper = add(7, struct.pack('>H', string(HELPER)))
    descriptor = '(L' + CONTEXT + ';Ljava/lang/String;)' + DOCUMENT
    helper_nat = add(12, struct.pack('>HH', string('document'), string(descriptor)))
    new_ref = add(10, struct.pack('>HH', helper, helper_nat))
    tail = bytearray(old[end:])
    pos = 6
    pos += 2 + 2 * u2(tail, pos)
    members = u2(tail, pos)
    pos += 2
    for _ in range(members):
        attrs = u2(tail, pos + 6)
        pos += 8
        for _ in range(attrs):
            pos += 6 + u4(tail, pos + 2)
    methods = u2(tail, pos)
    pos += 2
    changed = []
    for _ in range(methods):
        name, desc, attrs = u2(tail, pos + 2), u2(tail, pos + 4), u2(tail, pos + 6)
        pos += 8
        for _ in range(attrs):
            attr, length = u2(tail, pos), u4(tail, pos + 2)
            body = pos + 6
            pos += 6 + length
            if utf.get(name) != b'invoke' or utf.get(desc) != b'(Lapp/morphe/patcher/patch/ResourcePatchContext;)V' or utf.get(attr) != b'Code':
                continue
            start = body + 8
            code_length = u4(tail, body + 4)
            require(code_length == 659, 'Home invoke code length differs')
            code = bytes(tail[start:start + code_length])
            source = b'\xb6' + struct.pack('>H', old_ref)
            require(code.count(source) == 2, 'Home document call count differs')
            # These are actual instruction starts in the hash-pinned original
            # class; their stack input and result types are unchanged.
            for offset in (75, 444):
                require(code[offset:offset + 3] == source, 'Home document instruction offset differs')
                at = start + offset
                tail[at:at + 3] = b'\xb8' + struct.pack('>H', new_ref)
                changed.append((at, source))
    require(len(changed) == 2, 'Expected exactly two changed invokes')
    restored = bytearray(tail)
    for at, source in changed:
        restored[at:at + 3] = source
    require(restored == old[end:], 'Unrelated JVM code/metadata changed')
    result = old[:8] + struct.pack('>H', count + len(additions)) + old[10:end] + b''.join(e for _, e in additions) + tail
    return result, {'entry': ENTRY, 'before_sha256': BASE_SHA,
                    'after_sha256': hashlib.sha256(result).hexdigest(),
                    'document_calls_redirected': 2,
                    'original_code_metadata_and_frames_preserved': True,
                    'new_helper': HELPER}


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--baseline', required=True, type=Path)
    p.add_argument('--classes', required=True, type=Path)
    p.add_argument('--report', required=True, type=Path)
    args = p.parse_args()
    with zipfile.ZipFile(args.baseline) as archive:
        raw = archive.read(ENTRY)
    patched, report = transform(raw)
    target = args.classes / ENTRY
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(patched)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(report, ensure_ascii=False))


if __name__ == '__main__':
    main()
