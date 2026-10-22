#!/usr/bin/env python3
"""Bind completed reproducible local validation to CI source and artifact bytes."""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json

ROOT = Path(__file__).resolve().parent.parent
NAMES = ('Berry_Browser_v1.3.0.mpp', 'Hiro_Morphe_Patches_v1.0.271.mpp',
         'QA_Berry_Browser_v1.3.0.json')


def package_module():
    spec = importlib.util.spec_from_file_location('berry_package_freeze', ROOT / 'publication/package.py')
    package = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(package)
    return package


def source_identity():
    sources = {name: hashlib.sha256(raw).hexdigest()
               for name, raw in package_module().source_files().items()
               if name != 'publication/expected.json'}
    identity = hashlib.sha256(json.dumps(sources, sort_keys=True, separators=(',', ':')).encode()).hexdigest()
    return sources, identity


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dist', type=Path, required=True)
    args = parser.parse_args()
    package_module().validate_qa(args.dist)
    sources, identity = source_identity()
    artifacts = {name: {'bytes': (args.dist / name).stat().st_size,
                       'sha256': hashlib.sha256((args.dist / name).read_bytes()).hexdigest()}
                 for name in NAMES}
    result = {'schema': 'berry-messagequeue130-local-identities-v1', 'source_sha256': identity,
              'sources': sources, 'artifacts': artifacts}
    (ROOT / 'publication/expected.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print('PASS Berry local source and artifact identities recorded: ' + identity)


if __name__ == '__main__':
    main()
