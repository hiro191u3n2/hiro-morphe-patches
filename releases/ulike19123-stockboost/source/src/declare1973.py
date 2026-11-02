#!/usr/bin/env python3
"""Pin the complete .73 GPU latency/arithmetic and .72 camera regression source graph."""
from pathlib import Path
import argparse, importlib.util
from build1973 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    BASE_SINGLE_SHA256, BASE_BUNDLE_SHA256, BASE_SINGLE_BYTES, BASE_BUNDLE_BYTES,
    SELECTED, PRODUCTION, CHANGED, ADDED, TOOL_PINS, PUBLISHER_FILES,
    json_bytes, sha, source_pins, NEW_JNI)

def declaration(source):
    publication = source.parent / 'publication'
    spec = importlib.util.spec_from_file_location('strong_gpu1973_publisher', publication / 'publish1973.py')
    publisher = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(publisher)
    return {'schema': 'ulike1973-build-declaration-v1', 'change_plan_reviewed': True,
        'qa_contract_reviewed': True, 'ulike_version': VERSION, 'bundle_version': BUNDLE_VERSION,
        'baseline_ulike_version': BASE_VERSION, 'baseline_bundle_version': BASE_BUNDLE_VERSION,
        'baseline_ulike_sha256': BASE_SINGLE_SHA256, 'baseline_bundle_sha256': BASE_BUNDLE_SHA256,
        'baseline_ulike_bytes': BASE_SINGLE_BYTES, 'baseline_bundle_bytes': BASE_BUNDLE_BYTES,
        'baseline_ulike_url': publisher.SINGLE_BASE_URL, 'baseline_bundle_url': publisher.BASE_URL,
        'selected_candidates': SELECTED, 'production_helper_roots': PRODUCTION,
        'toolchain_sha256': TOOL_PINS, 'ndk_revision': '27.2.12479018',
        'jdk_runtime_version': '21.0.8+9-LTS', 'jdk_vendor': 'Eclipse Adoptium',
        'android_device_tested': False, 'original_apk_apply_tested': False,
        'intentional_quality_algorithm_change': False, 'gpu_arithmetic_implementation_changed': True, 'new_native_methods': sorted(NEW_JNI), 'new_jni_methods': sorted(NEW_JNI),
        'allowed_changed_bundle_entries': sorted(CHANGED), 'allowed_added_bundle_entries': sorted(ADDED),
        'allowed_changed_standalone_entries': sorted(CHANGED), 'allowed_added_standalone_entries': sorted(ADDED),
        'reviewed_source_sha256': source_pins(), 'reviewed_publication_sha256':
        {name: sha((publication / name).read_bytes()) for name in PUBLISHER_FILES}}

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=ROOT.parent / 'manifest.json')
    parser.add_argument('--reviewed-mpp', type=Path, help='Directory of locally built and validated MPP packages whose exact bytes CI must reproduce')
    args = parser.parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    data = declaration(ROOT)
    if args.reviewed_mpp:
        from build1973 import SINGLE, BUNDLE
        data['reviewed_mpp_artifacts'] = {name: {'sha256': sha((args.reviewed_mpp / name).read_bytes()),
            'bytes': (args.reviewed_mpp / name).stat().st_size} for name in (SINGLE, BUNDLE)}
    args.output.write_bytes(json_bytes(data))
    print('PASS GPU latency declaration pins complete source/publication graph')

