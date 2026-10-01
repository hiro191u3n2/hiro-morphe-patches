#!/usr/bin/env python3
"""Load the pinned Purity alignment template locally; publish no coordinates."""
import argparse
import json
import math
from pathlib import Path
import re
import struct

import legacy_graph_metadata as legacy
import model_inspector as mi

TEXT_SHA = '171cc2187d3025400075d55f42c89dd34c4349c62ee9abcf9c9aa6c031288474'
POINTS_SHA = '3c68979e89af5a444cc3902a8f232b29e9285a024ad696a1dc4ee0268ef54f12'


def validate_template_pair(text, payload):
    """Validate exactly 106 CSV pairs against their binary float32 template."""
    mi.require(0 < len(text) <= 8192 and len(payload) == 106 * 2 * 4,
               'invalid alignment template size')
    rows = text.splitlines()
    mi.require(len(rows) == 106, 'unexpected landmark count')
    parsed = []
    for row in rows:
        mi.require(re.fullmatch(rb'-?\d+\.\d+,-?\d+\.\d+,?', row) is not None,
                   'invalid alignment CSV row')
        x, y = row.rstrip(b',').split(b',')
        parsed.extend((float(x), float(y)))
    values = struct.unpack('<212f', payload)
    mi.require(all(math.isfinite(v) and -4 <= v <= 4 for v in values),
               'invalid normalized alignment point')
    mi.require(all(struct.pack('<f', text_v) == struct.pack('<f', binary_v)
                   for text_v, binary_v in zip(parsed, values)), 'text/binary template mismatch')
    return [(values[i], values[i + 1]) for i in range(0, 212, 2)]


def load_goodlike_face_template(model_path, loader_library_path):
    model = mi.read_input(model_path)
    loader = mi.read_input(loader_library_path)
    mi.require(mi.sha(model) == legacy.MODEL_SHA, 'unsupported model SHA-256')
    mi.require(mi.sha(loader) == legacy.LOADER_LIBRARY_SHA, 'unsupported loader SHA-256')
    envelope = mi.inspect(model)
    mi.require(envelope.get('version') == 'v3' and envelope.get('name_candidate') == 'tt_goodlike_v1.0',
               'unsupported legacy envelope')
    group = next(g for g in envelope['groups'] if g['name'] == 'face_point')
    def blob(name):
        item = group[name]
        return mi.span(model, item['offset'], item['bytes'])
    material = legacy.decode_ecb_blob(blob('opaque_auxiliary'), legacy._loader_material(loader))
    text = legacy.decode_ecb_blob(blob('opaque_encoded_payload'), material)
    payload = blob('opaque_tail_payload')
    mi.require(mi.sha(text) == TEXT_SHA and mi.sha(payload) == POINTS_SHA, 'template SHA-256 mismatch')
    checksum = legacy.native_graph_checksum(text)
    mi.require(checksum == group['decoded_payload_checksum']['stored_u32'], 'template checksum mismatch')
    points = validate_template_pair(text, payload)
    parameters = {p['key']: p['payload']['value'] for p in envelope['parameters']}
    metadata = {'status': 'PASS_PINNED_ALIGNMENT_TEMPLATE', 'model_sha256': legacy.MODEL_SHA,
                'loader_library_sha256': legacy.LOADER_LIBRARY_SHA,
                'text_sha256': TEXT_SHA, 'point_bytes_sha256': POINTS_SHA,
                'point_count': len(points), 'coordinates_per_point': 2, 'dtype': 'little-endian FP32',
                'csv_float32_bitwise_matches_binary': True, 'native_checksum_verified': True,
                'native_checksum': checksum, 'parameters': parameters,
                'limits': ['This verifies the stored alignment template, not native crop/warp runtime parity.',
                           'Coordinates are returned locally in memory and deliberately omitted from CLI output.']}
    return points, metadata


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('model', type=Path)
    parser.add_argument('loader_library', type=Path)
    args = parser.parse_args()
    _, metadata = load_goodlike_face_template(args.model, args.loader_library)
    print(json.dumps(metadata, indent=2))

if __name__ == '__main__':
    main()
