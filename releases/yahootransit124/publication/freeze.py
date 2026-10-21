#!/usr/bin/env python3
"""Bind a completed local validation to source identities and CI artifact bytes."""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json

ROOT = Path(__file__).resolve().parent.parent
NAMES = ('Hiro_Morphe_Patches_v1.0.269.mpp', 'QA_YahooTransit_v1.2.4.json')


def source_identity():
    spec = importlib.util.spec_from_file_location('yahoo_package_freeze', ROOT / 'publication/package.py')
    package = importlib.util.module_from_spec(spec); spec.loader.exec_module(package)
    sources = {name: hashlib.sha256(raw).hexdigest() for name, raw in package.source_files().items()
               if name != 'publication/expected.json'}
    identity = hashlib.sha256(json.dumps(sources, sort_keys=True, separators=(',', ':')).encode()).hexdigest()
    return sources, identity


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--dist', type=Path, required=True)
    a = p.parse_args()
    sources, identity = source_identity()
    artifacts = {n: {'bytes': (a.dist / n).stat().st_size,
        'sha256': hashlib.sha256((a.dist / n).read_bytes()).hexdigest()} for n in NAMES}
    value = {'schema': 'yahootransit124-local-identities-v1', 'source_sha256': identity,
             'sources': sources, 'artifacts': artifacts}
    (ROOT / 'publication/expected.json').write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')
    print('PASS local source and artifact identities recorded: ' + identity)


if __name__ == '__main__':
    main()
