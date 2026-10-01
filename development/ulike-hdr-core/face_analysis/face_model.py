"""Strict local inspection of the supplied stock 106-point face model.

No model, complete graph, weight, loader material, or native binary is bundled.
This uses the model's normal loader format, never licensing/authentication code.
Inspection does not implement the detector or establish native execution parity.
"""
from collections import Counter
import json
from pathlib import Path

import model_inspector as mi
from legacy_graph_metadata import decode_ecb_blob, native_graph_checksum

MODEL_SHA = 'c4b081d04f6829f7cd5849abca0941891f1a7f2cb250efd7f0e42fece5426610'
LIBRARY_SHA = 'd40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e'
GROUPS = ('detect', 'image_detect', 'slowimage_detect', 'base_det', 'base',
          'base_partface', 'pout', 'eyecls', 'mean', 'detect_config', 'base_config')


def _material(library):
    """Read a bounded immediate-data constructor from a caller-pinned library."""
    mi.require(mi.sha(library) == LIBRARY_SHA, 'unsupported face loader library')
    registers, stack = {31: 0}, {}
    for address in range(0xe25758, 0xe258b4, 4):
        word = mi.u32(library, address)
        if word & 0xff800000 == 0x52800000:
            shift = (word >> 21) & 3
            mi.require(shift <= 1, 'invalid 32-bit immediate shift')
            registers[word & 31] = ((word >> 5) & 0xffff) << (16 * shift)
        elif word & 0xffc00000 == 0xb9000000:
            mi.require((word >> 5) & 31 == 31, 'unexpected store base')
            reg, offset = word & 31, ((word >> 10) & 0xfff) * 4
            mi.require(reg in registers and offset <= 0x148 and offset % 8 == 0,
                       'unexpected constructor argument')
            stack[offset] = registers[reg]
        else:
            # These exact, pinned instructions set object state, not varargs.
            mi.require(address in (0xe25784, 0xe257e8), 'unexpected constructor instruction')
    mi.require(all(i in registers for i in range(1, 8)) and
               all(i in stack for i in range(0, 0x150, 8)), 'missing constructor argument')
    args = [registers[i] for i in range(1, 8)] + [stack[i] for i in range(0, 0x150, 8)]
    mi.require(len(args) == 49 and args[-1] == 0 and all(0 < c < 128 for c in args[:-1]),
               'invalid constructor material')
    return bytes(args[:-1])[:32]


def load_groups(model_path, library_path):
    """Return verified graph and uninterpreted weight bytes for local use only."""
    model, library = mi.read_input(model_path), mi.read_input(library_path)
    mi.require(mi.sha(model) == MODEL_SHA, 'unsupported face model')
    material = _material(library)
    envelope = mi.inspect(model)
    mi.require(tuple(g['name'] for g in envelope['groups']) == GROUPS,
               'unexpected face model groups')
    pins = json.loads(Path(__file__).with_name('FACE_MODEL_PINS.json').read_text())
    result = {}
    for group in envelope['groups']:
        name, expected = group['name'], pins[group['name']]
        def blob(key):
            value = group[key]
            return mi.span(model, value['offset'], value['bytes'])
        key = decode_ecb_blob(blob('opaque_auxiliary'), material)
        mi.require(len(key) == 32, 'unexpected model material size')
        graph = decode_ecb_blob(blob('opaque_encoded_payload'), key)
        mi.require(native_graph_checksum(graph) == group['decoded_payload_checksum']['stored_u32'],
                   'face graph checksum mismatch')
        weights = blob('opaque_tail_payload')
        for label, data in [('graph', graph), ('tail', weights)]:
            mi.require(len(data) == expected[label + '_bytes'] and
                       mi.sha(data) == expected[label + '_sha256'], 'face group pin mismatch')
        mi.require(all(x in (0, 10, 13) or 32 <= x < 127 for x in graph), 'invalid graph text')
        result[name] = (graph, weights)
    return result


def inspect_face(model_path, library_path):
    groups = load_groups(model_path, library_path)
    base = json.loads(groups['base_config'][0])
    # These three are the network configurations, not a claim that output
    # coordinates have already been transformed to a particular image frame.
    for name in ('base_det', 'base', 'base_partface'):
        mi.require(base[name]['point_num'] == 106 and base[name]['input_w'] == 120 and
                   base[name]['input_h'] == 120, 'unexpected landmark configuration')
    reports = []
    for name, (graph, weights) in groups.items():
        row = {'name': name, 'graph_bytes': len(graph), 'graph_sha256': mi.sha(graph),
               'weight_bytes': len(weights), 'weight_sha256': mi.sha(weights),
               'native_graph_checksum_verified': True}
        if name not in ('mean', 'detect_config', 'base_config'):
            rows = [line.split() for line in graph.decode('ascii').splitlines() if line]
            row['operator_counts'] = dict(Counter(r[0] for r in rows if not r[0].isdigit()))
        reports.append(row)
    return {'status': 'PASS_PINNED_FACE_GRAPH_INSPECTION', 'model_sha256': MODEL_SHA,
            'loader_library_sha256': LIBRARY_SHA, 'groups': reports,
            'landmark_networks': {name: {k: base[name][k] for k in
                ('point_num', 'input_w', 'input_h', 'bits', 'mean_val')}
                for name in ('base_det', 'base', 'base_partface')},
            'native_inference_executed': False, 'landmarks_produced': False,
            'weight_layout_and_quantization_verified': False}


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('model', type=Path)
    parser.add_argument('library', type=Path)
    args = parser.parse_args()
    print(json.dumps(inspect_face(args.model, args.library), indent=2))
