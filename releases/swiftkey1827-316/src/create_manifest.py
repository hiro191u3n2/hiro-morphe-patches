#!/usr/bin/env python3
"""Pin reviewed v1.8.27 build sources and exact independently rebuilt assets."""
import argparse
import importlib.util
import json
from pathlib import Path
import sys
sys.dont_write_bytecode = True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True, type=Path)
    parser.add_argument('--dist', required=True, type=Path)
    parser.add_argument('--second-dist', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    source = args.source.resolve()
    spec = importlib.util.spec_from_file_location('publish1827', source / 'publish1827.py')
    p = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(p)
    p.require(args.dist.resolve() != args.second_dist.resolve(), 'Independent output directories required')
    pins = {}
    for path in sorted(source.rglob('*')):
        if not path.is_file() or '__pycache__' in path.parts:
            continue
        p.require(not path.is_symlink(), 'Symlink source not allowed')
        raw = path.read_bytes()
        pins[path.relative_to(source).as_posix()] = {'bytes': len(raw), 'sha256': p.sha(raw)}
    assets = {}
    for name in p.ASSETS:
        raw = (args.dist / name).read_bytes()
        p.require(raw == (args.second_dist / name).read_bytes(), 'Builds differ: ' + name)
        assets[name] = {'bytes': len(raw), 'sha256': p.sha(raw)}
    manifest = {
        'schema': 'swiftkey1827-publication-v1',
        'swiftkey_version': p.VERSION, 'bundle_version': p.BUNDLE,
        'baseline_bundle_version': p.PREVIOUS, 'baseline_bundle_sha256': p.BASE_SHA256,
        'baseline_swiftkey_version': p.SINGLE_PREVIOUS, 'baseline_swiftkey_sha256': p.SINGLE_BASE_SHA256,
        'change_plan_reviewed': True, 'qa_contract_reviewed': True,
        'android_device_tested': False, 'original_apk_apply_tested': False,
        'allowed_changed_bundle_entries': sorted(p.REQUIRED_CHANGED),
        'allowed_changed_standalone_entries': sorted(p.REQUIRED_CHANGED),
        'allowed_added_bundle_entries': [], 'allowed_added_standalone_entries': [],
        'qa_required_values': p.REQUIRED_QA,
        'source_files': pins, 'artifacts': assets,
    }
    args.output.write_text(json.dumps(manifest, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
    p.load_expected(args.output)
    p.verify_source_files(source, manifest)
    print('PASS reviewed sources and two identical build outputs pinned')


if __name__ == '__main__':
    main()
