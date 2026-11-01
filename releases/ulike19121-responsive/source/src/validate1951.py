#!/usr/bin/env python3
"""Verify the exact .50/.183 MPP delta and record honest, fresh host evidence.

The original ULike APKS is not an input to this workflow.  Neither a successful
MPP rebuild nor a host pixel oracle is recorded as an Android APK application.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re
import struct
import zipfile
import zlib

from build1951 import (
    ROOT, VERSION, BUNDLE_VERSION, BASE_SINGLE, BASE_SINGLE_SHA256,
    BASE_BUNDLE_VERSION, BASE_BUNDLE_SHA256, SINGLE, SELECTED, PRODUCTION,
    GPU_ENTRY, CORE_ENTRY, MOIRE_ENTRY, H8_ENTRY, archive, bundle_name,
    headers, own, require, sha,
)

BASE_BUNDLE = 'Hiro_Morphe_Patches_v' + BASE_BUNDLE_VERSION + '.mpp'
QA_NAME = 'QA_ULike_v1.9.51.json'
INSTALLER = 'app/hiro/ulike/patches/IntegrationPayload186.class'
LOADER = 'app/hiro/ulike/patches/UlikeHqMaxPatch.class'
RUNTIME = 'ulike/runtime.dex'
MANIFEST = 'META-INF/MANIFEST.MF'
LEGACY_NATIVE = 'ulike1935/runtime/libulike_speed1935.so'
CHANGED = {MANIFEST, RUNTIME, 'classes.dex', INSTALLER, LOADER,
           GPU_ENTRY, CORE_ENTRY}
ADDED = {MOIRE_ENTRY, H8_ENTRY}
HOST_SUITES = {
    'baseline_camera_quality_save_timing', 'exact_temporal_fusion',
    'capture_preprocessing_lifecycle', 'primary_denoise_native_exact',
    'fused_gpu_finishing_exact', 'gpu_runtime_admission1950',
    'exact_correction_copy_elision', 'h6_moire_exact',
    'h7_primary_denoise_exact', 'h8_two_pass_gpu_exact', 'h9_h11_gpu_exact',
}


def file_pins(root, expected, prefix=''):
    require(isinstance(expected, dict) and bool(expected), 'Missing source pins: ' + prefix)
    actual = {}
    for name, digest in expected.items():
        p = Path(name)
        require(not p.is_absolute() and '..' not in p.parts and '\\' not in name,
                'Unsafe source pin: ' + name)
        path = root / prefix / p
        require(path.is_file(), 'Missing compiled/tested source: ' + str(path))
        actual[name] = sha(path.read_bytes())
        require(actual[name] == digest, 'Compiled/tested source changed: ' + str(path))
    return actual


def dex_integrity(data, label):
    require(len(data) >= 112 and data[:4] == b'dex\n' and data[7] == 0,
            'Malformed DEX: ' + label)
    require(struct.unpack_from('<I', data, 32)[0] == len(data)
            and struct.unpack_from('<I', data, 36)[0] == 112
            and struct.unpack_from('<I', data, 40)[0] == 0x12345678,
            'DEX header mismatch: ' + label)
    require(data[12:32] == hashlib.sha1(data[32:]).digest()
            and struct.unpack_from('<I', data, 8)[0] == zlib.adler32(data[12:]) & 0xffffffff,
            'DEX checksum mismatch: ' + label)


def elf(data, label):
    require(len(data) >= 64 and data[:6] == b'\x7fELF\x02\x01'
            and data[18:20] == b'\xb7\0',
            'Expected ARM64 ELF64 little-endian native payload: ' + label)


def validate(args):
    qa = json.loads((args.dist / QA_NAME).read_text())
    require(qa.get('schema') == 'ulike1951-optimization-v1'
            and qa.get('ulike_version') == VERSION
            and qa.get('bundle_version') == BUNDLE_VERSION
            and qa.get('selected_candidates') == SELECTED,
            'Wrong diagnostics build identity or candidate scope')
    result = qa.get('host_quality_result', {})
    suites = result.get('suites', {})
    require(qa.get('host_quality_passed') is True and result.get('status') == 'passed'
            and type(result.get('assertions')) is int and result['assertions'] > 0
            and HOST_SUITES <= suites.keys()
            and result.get('pixel_equivalence_to_baseline') is True
            and result.get('full_resolution_nv21_checked') == '4080x3060',
            'Executed pixel-exact host regression is required')
    require(all(s.get('status') == 'passed'
                and type(s.get('assertions')) is int and s['assertions'] > 0
                for s in suites.values()), 'Host suite failed or lacks executed assertions')
    require(sum(s['assertions'] for s in suites.values()) == result['assertions']
            and sha(json.dumps(result, sort_keys=True).encode()) == qa.get('host_quality_result_sha256'),
            'Host result assertion total or fingerprint differs')
    require(suites['primary_denoise_native_exact'].get('production_c_jni_executed') is True
            and suites['fused_gpu_finishing_exact'].get('software_egl_shaders_executed') is True
            and suites['h8_two_pass_gpu_exact'].get('software_egl_two_pass_pixel_exact') is True
            and suites['h9_h11_gpu_exact'].get('software_egl_saved_output_pixel_exact') is True,
            'Native JNI and integer software GPU execution evidence required')
    require(all(suites['h8_two_pass_gpu_exact'].get(key) is True for key in (
                'stage_parameter_order_verified', 'sharpening_halo_pixel_exact',
                'production_filter_dex_oracle_executed')),
            'Production H8 stage order and sharpening halo pixel equivalence required')
    require(all(suites['h6_moire_exact'].get(key) is True for key in (
                'native_sharpen_c_executed', 'native_sharpen_pixel_exact',
                'combined_moire_sharpen_pixel_exact')),
            'H6 native final sharpening and combined pixel equivalence required')
    for key in ('original_apk_apply_tested', 'device_tested', 'device_quality_verified',
                'ci_android_apply_tested', 'device_save_speed_measured'):
        require(qa.get(key) is False, 'Unperformed Android testing claimed: ' + key)
    require(json.loads((ROOT / 'production1951.json').read_text()) == PRODUCTION,
            'Reviewed production roots changed')
    expected_production = {name + '.java': sha((ROOT / (name + '.java')).read_bytes())
                           for name in PRODUCTION}
    require(qa.get('compiled_production_source_sha256') == expected_production
            and qa.get('production_source_consistency_verified') is True,
            'Current production code differs from compiler input')
    source_pins = {}
    for key, path in (
        ('compiled_production_source_sha256', ''),
        ('timing_metadata_source_sha256', ''),
        ('compiled_transformer_source_sha256', ''),
        ('compiled_gpu_native_source_sha256', 'native1949'),
        ('compiled_core_native_source_sha256', 'native1950'),
        ('executed_host_source_sha256', ''),
    ):
        source_pins[key] = file_pins(ROOT, qa.get(key), path)
    for key, path in (('moire_native_build', 'native1951'),
                      ('h8gpu_native_build', 'h8gpu')):
        source_pins[key + '_sources'] = file_pins(ROOT, qa.get(key, {}).get('sources'), path)

    inputs = args.input or args.build.parent / 'input'
    require(sha((inputs / BASE_SINGLE).read_bytes()) == BASE_SINGLE_SHA256
            and sha((inputs / BASE_BUNDLE).read_bytes()) == BASE_BUNDLE_SHA256,
            'Exact .50 standalone and .183 integrated baselines required')
    base = archive(inputs / BASE_SINGLE)
    base_bundle = archive(inputs / BASE_BUNDLE)
    single_name, combined_name = SINGLE, bundle_name(qa)
    current = archive(args.dist / single_name)
    combined = archive(args.dist / combined_name)
    require(headers(base[MANIFEST]).get('Version') == '1.9.50'
            and headers(base_bundle[MANIFEST]).get('Version') == BASE_BUNDLE_VERSION
            and headers(current[MANIFEST]).get('Version') == VERSION
            and headers(combined[MANIFEST]).get('Version') == BUNDLE_VERSION,
            'Manifest version lineage differs')
    require({n: b for n, b in base.items() if own(n)}
            == {n: b for n, b in base_bundle.items() if own(n)},
            'Published baseline standalone and integrated ULike copies differ')
    added = set(current) - set(base)
    changed = {n for n in base if n in current and base[n] != current[n]}
    bundle_added = set(combined) - set(base_bundle)
    bundle_changed = {n for n in base_bundle if n in combined
                      and base_bundle[n] != combined[n]}
    require(set(base) <= set(current) and added == ADDED
            and set(base_bundle) <= set(combined) and bundle_added == ADDED
            and changed == CHANGED and bundle_changed == CHANGED,
            'Unexpected standalone or integrated resource delta')
    for prefix, actual in (('standalone', (changed, added)),
                           ('bundle', (bundle_changed, bundle_added))):
        require(qa.get('changed_' + prefix + '_entries') == sorted(actual[0])
                and qa.get('added_' + prefix + '_entries') == sorted(actual[1]),
                'Build QA resource delta differs: ' + prefix)
    require({n: b for n, b in current.items() if own(n)}
            == {n: b for n, b in combined.items() if own(n)},
            'Current standalone and integrated ULike resources differ')
    require(all(combined[n] == value for n, value in base_bundle.items()
                if not own(n) and n not in ('classes.dex', MANIFEST)),
            'Another application resource changed')
    for name in (LEGACY_NATIVE, 'ulike/methods.dex', 'ulike/methods.tsv'):
        require(current[name] == base[name], 'Protected legacy binary changed: ' + name)
    require(qa.get('non_ulike_resources_byte_identical') is True
            and qa.get('native_methods_byte_identical') is True
            and qa.get('native_methods_tsv_byte_identical') is True,
            'QA preservation claims differ from archive bytes')

    emitted = args.build / 'emitted'
    for entry, path in ((RUNTIME, 'runtime.dex'), ('classes.dex', 'loader.dex'),
                        (INSTALLER, 'IntegrationPayload186.class'),
                        (LOADER, 'UlikeHqMaxPatch.class')):
        require(current[entry] == (emitted / path).read_bytes(),
                'Packaged payload differs from executed build: ' + entry)
    require(combined['classes.dex'] == (emitted / 'bundle-loader.dex').read_bytes(),
            'Integrated Dex differs from emitted reviewed installer')
    for label, raw in ((RUNTIME, current[RUNTIME]),
                       ('single classes.dex', current['classes.dex']),
                       ('integrated classes.dex', combined['classes.dex'])):
        dex_integrity(raw, label)
    require('PASS 6-row installer inverse' in (args.build / 'native-installer-metadata.log').read_text(),
            'Native installer inverse / transaction guard not verified')
    require((args.build / 'helper-references.txt').read_text().startswith('PASS helper references in ')
            and 'PASS\toptimization_roots=' in (args.build / 'optimization-dex-verification.txt').read_text(),
            'Helper references or incremental Dex verification missing')
    for label, entry in (('gpu', GPU_ENTRY), ('core', CORE_ENTRY),
                         ('moire', MOIRE_ENTRY), ('h8gpu', H8_ENTRY)):
        raw = current[entry]
        elf(raw, entry)
        report = qa.get(label + '_native_build', {})
        require(qa.get(label + '_native_library_sha256') == sha(raw) == report.get('sha256')
                and qa.get(label + '_native_library_bytes') == len(raw) == report.get('bytes')
                and report.get('ndk_revision') == '27.2.12479018'
                and report.get('physical_android_tested') is False,
                'Native build provenance or bytes differ: ' + entry)
    require(sha(current[LEGACY_NATIVE]) == qa.get('native_library_sha256')
            and len(current[LEGACY_NATIVE]) == qa.get('native_library_bytes'),
            'Legacy CPU native binary differs from pinned build')
    artifacts = {n: {'sha256': sha((args.dist / n).read_bytes()),
                     'bytes': (args.dist / n).stat().st_size}
                 for n in (single_name, combined_name)}
    require(qa.get('artifacts') == artifacts,
            'Build QA does not fingerprint the same exact releasable MPP bytes')
    evidence = {
        'schema': 'ulike1951-desktop-validation-v1',
        'ulike_version': VERSION, 'bundle_version': BUNDLE_VERSION,
        'artifacts': artifacts,
        'original_apk_apply_tested': False,
        'original_split_merge_tested': False,
        'original_apk_application': {'status': 'not_tested',
                                     'reason': 'Original ULike 5.6.2 (740) APKS unavailable to this build'},
        'device_tested': False, 'device_quality_verified': False,
        'source_consistency_verified': True,
        'desktop_validation': {
            'single': {'status': 'mpp_bytes_verified', 'artifact': single_name,
                       'original_apk_apply_tested': False},
            'bundle': {'status': 'mpp_bytes_verified', 'artifact': combined_name,
                       'original_apk_apply_tested': False},
        },
        'host_quality_result_sha256': qa['host_quality_result_sha256'],
        'host_assertions': result['assertions'],
        'native_payloads': {label: {'sha256': sha(current[entry]), 'bytes': len(current[entry])}
                            for label, entry in (('gpu', GPU_ENTRY), ('core', CORE_ENTRY),
                                                 ('moire', MOIRE_ENTRY), ('h8gpu', H8_ENTRY))},
        'resource_delta': {'standalone_changed': sorted(changed),
                           'standalone_added': sorted(added),
                           'bundle_changed': sorted(bundle_changed),
                           'bundle_added': sorted(bundle_added)},
        'runtime_preservation_verification': {
            'status': 'passed', 'legacy_native_and_methods_unchanged': True,
            'other_app_resources_byte_identical': True,
            'standalone_and_bundle_ulike_resources_identical': True,
            'installer_inverse_and_six_rows_verified': True,
            'emitted_mpp_dex_checksums_valid': True,
            'original_apk_apply_tested': False,
        },
        **source_pins,
        'validator_source_sha256': sha(Path(__file__).read_bytes()),
        'published': False,
        'limitations': 'Original APKS patch application, Android runtime and Galaxy camera quality/speed are not tested by this workflow.',
    }
    require(not args.output.exists(), 'Fresh desktop validation evidence path required')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(evidence, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
    print('PASS exact .50/.183 MPP delta, ARM64 payloads, desktop Dex/host evidence; original APKS and device untested')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('build', 'dist', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--input', type=Path, help='Directory containing the byte-pinned baseline MPPs')
    args = parser.parse_args()
    for name, value in vars(args).items():
        if value is not None:
            setattr(args, name, value.resolve())
    validate(args)


if __name__ == '__main__':
    main()
