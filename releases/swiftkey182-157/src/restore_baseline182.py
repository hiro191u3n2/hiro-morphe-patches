#!/usr/bin/env python3
"""Restore the already public standalone MPP using exact shared bundle bytes.

This transports only the small standalone-specific ZIP segments. It does not
fetch the former Site from the CI runner, recompile old code or recompress data.
Every reused range, both complete inputs and the output are checked by SHA-256.
"""
import argparse, base64, hashlib, json
from pathlib import Path

BASE_SHA = '40c885d205d0a087192be599263a13858ff5c48fec935835791bfc93a364de88'
SINGLE_SHA = '077ad014caa5825a690fa2aad9d4d743ecc36267ae86491563e12ffade9ee8e0'

def digest(data):
    return hashlib.sha256(data).hexdigest()

def restore(bundle, transport, output):
    base = Path(bundle).read_bytes()
    spec = json.loads(Path(transport).read_text())
    assert spec['schema'] == 'swiftkey181-exact-public-baseline-v1'
    assert digest(base) == BASE_SHA == spec['base_sha256']
    assert spec['target_sha256'] == SINGLE_SHA and spec['target_bytes'] == 16083358
    data = bytearray()
    for item in spec['segments']:
        if 'literal_base64' in item:
            raw = base64.b64decode(item['literal_base64'], validate=True)
        else:
            offset, length = item['base_offset'], item['length']
            assert type(offset) is int and type(length) is int
            assert 0 <= offset <= len(base) and 0 < length <= len(base) - offset
            raw = base[offset:offset + length]
        assert digest(raw) == item['sha256']
        data.extend(raw)
    assert len(data) == spec['target_bytes'] and digest(data) == SINGLE_SHA
    Path(output).write_bytes(data)
    print('PASS restored exact published SwiftKey1.8.1 baseline: 16083358 bytes, SHA-256 verified')

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--bundle', required=True, type=Path)
    p.add_argument('--transport', required=True, type=Path)
    p.add_argument('--output', required=True, type=Path)
    args = p.parse_args()
    restore(args.bundle, args.transport, args.output)
