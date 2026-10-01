#!/usr/bin/env python3
"""Read the pinned Purity model with its pinned ordinary native loader contract.

No model, library, key, full graph, or weight values are bundled or printed.
Only fixed MOVZ/STR instruction data is read; no native code is executed. This
module does not verify or bypass licensing or authentication. Returned model
bytes are for local processing and are not publication artifacts.
"""
import argparse
from collections import Counter
import json
import math
from pathlib import Path
import struct

import model_inspector as mi

MODEL_SHA = '0d60ea7e684f32628daac031cb37fd32c50bf898aa3fb62bcbc25673b5725cad'
LOADER_LIBRARY_SHA = 'd40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e'
LIBRARY_SHA = 'cedda347b55c03de82cc22b7f003777f40750d4f341494a29587d7b821ad7853'
GRAPH_SHA = '0a5c6692f3361a0c5247c95065e446ca8245db7aa632f26ef227253f06254a26'
WEIGHTS_SHA = '98ac5b9c28d57d02831e2bcb771e2732f2eb6be77285a6dde875b7b4c636cda0'
MAX_DECODE_BYTES = 1024 * 1024


def _loader_material(library):
    """Read only the pinned constructor's bounded immediate-data sequence."""
    mi.require(mi.sha(library) == LOADER_LIBRARY_SHA, 'unsupported loader library SHA-256')
    registers = {31: 0}
    stack = {}
    # IDream constructor c9f460...c9f4cc invokes the e48ee8 helper between
    # the first and second ranges. e43e04 concatenates the resulting varargs.
    ranges = [(0xc9f460, 0xc9f468), (0xe48ee8, 0xe48fb4),
              (0xc9f470, 0xc9f48c), (0xc9f490, 0xc9f4d0)]
    for start, end in ranges:
        for address in range(start, end, 4):
            word = mi.u32(library, address)
            if word & 0xff800000 == 0x52800000:  # MOVZ Wd, immediate
                shift = (word >> 21) & 3
                mi.require(shift <= 1, 'invalid 32-bit MOVZ shift')
                registers[word & 31] = ((word >> 5) & 0xffff) << (16 * shift)
            elif word & 0xffc00000 == 0xb9000000:  # STR Wt,[SP,#imm]
                mi.require((word >> 5) & 31 == 31, 'unexpected store base')
                index = word & 31
                mi.require(index in registers, 'unknown immediate source register')
                offset = ((word >> 10) & 0xfff) * 4
                mi.require(offset <= 0x148 and offset % 8 == 0, 'unexpected argument slot')
                stack[offset] = registers[index]
            else:
                raise ValueError('unsupported pinned immediate-data instruction')
    mi.require(all(i in registers for i in range(1, 8)), 'missing register argument')
    mi.require(all(i in stack for i in range(0, 0x150, 8)), 'missing stack argument')
    args = [registers[i] for i in range(1, 8)] + [stack[i] for i in range(0, 0x150, 8)]
    mi.require(len(args) == 49 and args[-1] == 0 and all(0 < c < 128 for c in args[:-1]),
               'invalid constructor material')
    # e3d824 requires 48 bytes; e3d840 passes the first 32 bytes to the parser.
    return bytes(args[:-1])[:32]


def decode_ecb_blob(encoded, material):
    """Native e40334 format: ECB blocks, then little-endian original length."""
    mi.require(len(material) in (16, 24, 32), 'unsupported AES material size')
    mi.require(20 <= len(encoded) <= MAX_DECODE_BYTES + 20 and
               (len(encoded) - 4) % 16 == 0, 'invalid encoded block length')
    n = mi.u32(encoded, len(encoded) - 4)
    padded = len(encoded) - 4
    mi.require(0 < n <= padded and padded - n < 16, 'invalid plaintext length')
    from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
    decoder = Cipher(algorithms.AES(material), modes.ECB()).decryptor()
    return (decoder.update(encoded[:-4]) + decoder.finalize())[:n]


def native_graph_checksum(data):
    """libeffect e3e1d0...e3e20c, unsigned 32-bit recurrence."""
    h = 0x4e67c6a7
    for byte in data:
        h = (h ^ ((h << 5) + (h >> 2) + byte)) & 0xffffffff
    return h


def load_pinned_model(model_path, loader_library_path, library_path):
    """Return (graph_bytes, FP32_weight_bytes, bounded_summary_metadata)."""
    model = mi.read_input(model_path)
    loader = mi.read_input(loader_library_path)
    library = mi.read_input(library_path)
    mi.require(mi.sha(model) == MODEL_SHA, 'unsupported model SHA-256')
    mi.require(mi.sha(loader) == LOADER_LIBRARY_SHA, 'unsupported loader library SHA-256')
    mi.require(mi.sha(library) == LIBRARY_SHA, 'unsupported inference library SHA-256')
    envelope = mi.inspect(model)
    mi.require(envelope.get('version') == 'v3' and envelope.get('name_candidate') == 'tt_goodlike_v1.0',
               'unsupported legacy envelope')
    mi.require([g['name'] for g in envelope['groups']] == ['v0', 'face_point'], 'unexpected model groups')
    group = envelope['groups'][0]
    def blob(name):
        item = group[name]
        return mi.span(model, item['offset'], item['bytes'])
    material = decode_ecb_blob(blob('opaque_auxiliary'), _loader_material(loader))
    mi.require(len(material) == 32, 'unexpected model material size')
    graph = decode_ecb_blob(blob('opaque_encoded_payload'), material)
    mi.require(len(graph) == 12118 and mi.sha(graph) == GRAPH_SHA, 'decoded graph SHA-256 mismatch')
    mi.require(all(x in (10, 13) or 32 <= x < 127 for x in graph), 'invalid graph text')
    checksum = native_graph_checksum(graph)
    mi.require(checksum == group['decoded_payload_checksum']['stored_u32'], 'native graph checksum mismatch')
    rows = [x.split() for x in graph.decode('ascii').splitlines() if x]
    mi.require(rows[0] == ['1', '96', '1614766077'] and len(rows) == 98,
               'unexpected graph header or row count')
    mi.require(rows[1] == ['DataV2', 'data', '1', '256', '256', '3', '4', '0', '0'],
               'unexpected graph input')
    tail = blob('opaque_tail_payload')
    mi.require(len(tail) == 1000020 and mi.u32(tail, len(tail) - 4) == int(rows[0][2]),
               'graph/weights marker mismatch')
    weights = tail[:-4]
    mi.require(mi.sha(weights) == WEIGHTS_SHA, 'weight SHA-256 mismatch')
    mi.require(all(math.isfinite(x[0]) for x in struct.iter_unpack('<f', weights)), 'nonfinite weight')
    metadata = {'model_sha256': MODEL_SHA, 'loader_library_sha256': LOADER_LIBRARY_SHA, 'effect_library_sha256': LOADER_LIBRARY_SHA,
                'library_sha256': LIBRARY_SHA, 'graph_sha256': GRAPH_SHA,
                'weight_values_sha256': WEIGHTS_SHA, 'graph_bytes': len(graph),
                'weight_values_bytes': len(weights), 'weight_values_count': len(weights) // 4,
                'model_weight_offset': group['opaque_tail_payload']['offset'],
                'graph_header': [int(x) for x in rows[0]], 'graph_native_checksum_verified': True,
                'graph_native_checksum': checksum, 'graph_marker_matches_weights': True,
                'weight_tail_tag_u32_uninterpreted': group['tail_tag_u32_uninterpreted'],
                'native_model_execution_verified': False}
    return graph, weights, metadata


# Stable adapter for model_replay; identical strict pinning and return contract.
load_goodlike = load_pinned_model


def summary(model_path, loader_library_path, library_path):
    graph, weights, metadata = load_pinned_model(model_path, loader_library_path, library_path)
    rows = [x.split() for x in graph.decode('ascii').splitlines() if x]
    return {'status': 'PASS_PINNED_LEGACY_GRAPH_METADATA', **metadata,
            'operators': dict(Counter(row[0] for row in rows[1:])),
            'input_shape_nchw': [1, 3, 256, 256], 'output_channels': int(rows[-2][2]),
            'last_operator': rows[-1][0],
            'limits': ['No native execution parity or complete beauty pipeline is established by this decoder.',
                       'The separate face_point group and the tail tag remain uninterpreted here.']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('model', type=Path)
    parser.add_argument('loader_library', type=Path)
    parser.add_argument('inference_library', type=Path)
    args = parser.parse_args()
    print(json.dumps(summary(args.model, args.loader_library, args.inference_library), indent=2))


if __name__ == '__main__':
    main()
