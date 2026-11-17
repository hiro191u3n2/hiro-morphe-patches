#!/usr/bin/env python3
"""Store bundle 382 as exact spans of immutable 381 storage and new literals.

Reuses the audited bounded-span implementation from stage-newxja378.py. Every
copied byte is flattened to an existing backing asset; no nested MPP download.
"""
from __future__ import annotations
import argparse
import base64
import importlib.util
import json
from pathlib import Path
import re

NAME = 'Hiro_Morphe_Patches_v1.0.382.mpp'
BASE = 'Hiro_Morphe_Patches_v1.0.381.mpp'
BASE_BYTES = 48325768
BASE_SHA256 = '8266fbf1274b8f9651bb67b0a55264564036b8b90499995591f267b3277341c7'
RECIPE_NAME = NAME + '.delta382.json'
PART_BYTES = 16 * 1024 * 1024

def load(site, name):
    spec = importlib.util.spec_from_file_location(name, site/'scripts/stage-newxja378.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def previous(site):
    spec = importlib.util.spec_from_file_location('_ui382_previous', site/'scripts/stage-newxui381.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def asset_source(site, source, name):
    match = re.fullmatch(r'Hiro_Morphe_Patches_v1\.0\.(355)\.mpp\.(part00[0-2]\.bin)', name)
    if match:
        return site/'scripts/mpp-release-data'/match[1]/match[2]
    match = re.fullmatch(r'Hiro_Morphe_Patches_v1\.0\.(377|378|379|380|381|382)\.mpp\.literal\d{3}\.bin', name)
    if match:
        return (source if match[1] == '382' else site/'scripts/mpp-release-data'/match[1])/name
    raise ValueError('Unexpected bounded-span asset: '+name)

def engine(site):
    current = load(site, '_ui382_span_engine')
    current.NAME, current.BASE = NAME, BASE
    current.BASE_BYTES, current.BASE_SHA256 = BASE_BYTES, BASE_SHA256
    current.RECIPE_NAME = RECIPE_NAME
    current.asset_source = asset_source
    return current

def prepare(site, source, target):
    old_recipe, baseline = previous(site).read_source(site, site/'scripts/mpp-release-data/381')
    current = engine(site)
    current.require(len(baseline) == BASE_BYTES and current.digest(baseline) == BASE_SHA256,
                    'Published 381 baseline differs')
    storage = load(site, '_ui382_delta_compiler').load_storage(site)
    storage.BASE = BASE
    wanted = target.read_bytes()
    raw = storage.make_payload_delta(baseline, wanted)
    spans, literals = [], {}
    for segment in raw['segments']:
        if 'copy' in segment:
            current.flatten_copy(spans, old_recipe['segments'], *segment['copy'])
        else:
            data = base64.b64decode(segment['literal'], validate=True)
            for offset in range(0, len(data), PART_BYTES):
                chunk = data[offset:offset+PART_BYTES]
                name = f'{NAME}.literal{len(literals):03d}.bin'
                literals[name] = chunk
                current.append_span(spans, {'name':name, 'bytes':len(chunk), 'sha256':current.digest(chunk)}, 0, len(chunk))
    recipe = {'schema':current.SCHEMA, 'name':NAME, 'bytes':len(wanted), 'sha256':current.digest(wanted),
              'base':BASE, 'baseBytes':BASE_BYTES, 'baseSha256':BASE_SHA256, 'segments':spans}
    payload = (json.dumps(recipe, separators=(',', ':'))+'\n').encode('ascii')
    current.require(len(payload) <= current.MAX_METADATA_BYTES, 'Recipe too large')
    source.mkdir(parents=True, exist_ok=True)
    for name, data in literals.items():
        path = source/name
        current.require(not path.exists() or path.read_bytes() == data, 'Immutable literal differs: '+name)
        path.write_bytes(data)
    path = source/RECIPE_NAME
    current.require(not path.exists() or path.read_bytes() == payload, 'Immutable recipe differs')
    path.write_bytes(payload)
    actual, reconstructed = current.read_source(site, source)
    current.require(actual == recipe and reconstructed == wanted, 'Exact reconstruction failed')
    return {'result':'PASS', 'name':NAME, 'bytes':len(wanted), 'sha256':current.digest(wanted),
            'spanCount':len(spans), 'literalBytes':sum(map(len,literals.values())),
            'reusedBytes':sum(s['bytes'] for s in spans if s['asset']['name'] not in literals),
            'exactReconstruction':True}

def read_source(site, source):
    return engine(site).read_source(site, source)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--site', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--source', type=Path)
    commands = parser.add_subparsers(dest='command', required=True)
    p = commands.add_parser('prepare'); p.add_argument('--target', type=Path, required=True)
    commands.add_parser('materialize')
    p = commands.add_parser('stage'); p.add_argument('directory', nargs='?', type=Path)
    args = parser.parse_args()
    site = args.site.resolve()
    source = args.source.resolve() if args.source else site/'scripts/mpp-release-data/382'
    result = prepare(site, source, args.target) if args.command == 'prepare' else \
        engine(site).materialize(site, source) if args.command == 'materialize' else \
        engine(site).stage(site, source, args.directory or site/'dist/client/_mpp')
    print(json.dumps(result, sort_keys=True))

if __name__ == '__main__': main()
