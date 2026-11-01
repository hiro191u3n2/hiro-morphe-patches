#!/usr/bin/env python3
"""Validate fresh camera fixtures and exact image/native/save preservation."""
from pathlib import Path, PurePosixPath
import argparse, json
from build1965 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    BASE_SINGLE, BASE_BUNDLE, BASE_SINGLE_SHA256, BASE_BUNDLE_SHA256, BASE_SINGLE_BYTES,
    BASE_BUNDLE_BYTES, SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, INSTALLER, LOADER,
    require, sha, archive, headers, own, dex_integrity, json_bytes, production_path,
    source_pins, publication_pins, inventory_checks)

QA_NAME = 'QA_ULike_v1.9.65.json'
SOURCE_GROUPS = {'compiled_production_source_sha256': '', 'compiled_compile_only_source_sha256': '',
    'compiled_transformer_source_sha256': '', 'executed_host_source_sha256': ''}

def file_pins(source, pins, prefix=''):
    require(isinstance(pins, dict) and bool(pins), 'Missing compiler/execution source pins')
    for name, digest in pins.items():
        path = PurePosixPath(name)
        require(not path.is_absolute() and '..' not in path.parts and '\\' not in name, 'Unsafe source pin path')
        target = source / prefix / name
        require(target.is_file() and sha(target.read_bytes()) == digest, 'Source changed after compilation/test: ' + name)
    return pins

def host_checks(qa, source):
    require(qa.get('schema') == 'ulike1965-camera-trace-v1'
        and qa.get('status') == 'MPP_CAMERA_TRACE_HOST_VERIFIED_ORIGINAL_APKS_AND_DEVICE_UNVERIFIED'
        and qa.get('ulike_version') == VERSION and qa.get('bundle_version') == BUNDLE_VERSION
        and qa.get('baseline_ulike_version') == BASE_VERSION
        and qa.get('baseline_bundle_version') == BASE_BUNDLE_VERSION
        and qa.get('selected_candidates') == SELECTED, 'Wrong camera update identity/scope')
    inventory_checks(qa)
    result = qa.get('camera_host_evidence', {})
    require(qa.get('host_quality_passed') is True and result.get('status') == 'passed'
        and type(result.get('assertions')) is int and result['assertions'] > 0, 'Missing fresh executed camera host tests')
    require(sha(json.dumps(result, sort_keys=True).encode()) == qa.get('camera_host_evidence_sha256'), 'Camera evidence digest differs')
    require(result.get('physical_android_tested', result.get('physicalAndroidTested')) is False, 'Camera host evidence must disclose physical Android untested')
    for key in ('camera_session_lifecycle_regressions_passed', 'persistent_trace_storage_tests_passed',
                'trace_export_tests_passed', 'output_probe_session_ownership_tests_passed',
                'sampled_input_is_not_visible_success'):
        require(result.get(key) is True, 'Fresh camera semantic test evidence absent: ' + key)
    reports = result.get('tests', {})
    require(isinstance(reports, dict) and bool(reports), 'Missing fresh executed camera test inventory')
    for name, report in reports.items():
        require(report.get('status') == 'passed' and type(report.get('assertions')) is int
            and report['assertions'] > 0, 'Camera test report absent/empty: ' + name)
    require(sum(report['assertions'] for report in reports.values()) == result['assertions'], 'Executed camera assertion sum differs')
    require(qa.get('test_fixture_classes_absent_from_runtime') is True, 'Compiler/test fixture runtime leak proof absent')
    require(qa.get('production_source_consistency_verified') is True
        and qa.get('compiled_production_source_sha256') == {production_path(name).relative_to(ROOT).as_posix():
            sha(production_path(name).read_bytes()) for name in PRODUCTION}, 'Camera production differs from compiler input')
    for key, prefix in SOURCE_GROUPS.items():
        file_pins(source, qa.get(key), prefix)
    require(qa['executed_host_source_sha256'] == source_pins(), 'Complete source graph differs from executed graph')
    for key in ('sources', 'compiled_sources_sha256'):
        if key in result:
            file_pins(source, result[key])
    for key in ('persistent_rolling_across_restarts', 'anomaly_snapshots_protected',
                'user_incident_snapshot', 'logging_async_bounded', 'camera_trace_log_data_excluded',
                'inherited_native_libraries_byte_identical', 'native_library_byte_identical', 'native_installer_byte_identical',
                'native_installer_existing_rows_preserved', 'native_installer_existing_row_count_preserved',
                'non_ulike_loader_classes_unchanged', 'non_ulike_resources_byte_identical',
                'standalone_and_bundle_ulike_resources_identical', 'resolution_and_save_format_preserved',
                'save_publication_hooks_preserved', 'save_publication_contract_preserved',
                'save_format_and_codec_configuration_preserved', 'host_pixel_equivalence_to_baseline',
                'stage_timing_algorithm_preserved'):
        require(qa.get(key) is True, 'Required camera/preservation contract absent: ' + key)
    require(qa.get('native_installer_preserved_rows') == 12 and qa.get('native_installer_total_rows') == 12
        and qa.get('native_installer_updated_rows') == 0 and qa.get('native_installer_appended_rows') == 0,
        'Native installer preservation inventory differs')
    require(qa.get('diagnostic_stage_names') == ['fusion', 'noise', 'correction', 'compression', 'save'], 'Retained diagnostic stage inventory differs')
    for key in ('changed_native_methods', 'new_native_methods', 'new_jni_methods'):
        require(qa.get(key) == [], 'Camera-only update changed native linkage: ' + key)
    for key in ('intentional_quality_algorithm_change', 'capture_fusion_enabled',
                'original_apk_apply_tested', 'original_split_merge_tested', 'ci_android_apply_tested',
                'device_tested', 'device_quality_verified', 'device_save_speed_measured',
                'camera_visible_preview_verified_on_device', 'gpu_execution_on_physical_android',
                'all_processing_on_gpu', 'gx9_beauty_interop_supported', 'gx10_encoder_interop_supported',
                'fresh_whole_app_audit_in_this_release'):
        require(qa.get(key) is False, 'Unsupported/unperformed capability claimed: ' + key)
    require(qa.get('inherited_host_validation_source_version') == BASE_VERSION
        and qa.get('inherited_gpu_validation', {}).get('fresh_gpu_execution_in_this_release') is False,
        'Inherited GPU evidence must not be presented as fresh execution')
    declaration = json.loads((source.parent / 'manifest.json').read_text())
    require(declaration.get('reviewed_source_sha256') == source_pins()
        and declaration.get('reviewed_publication_sha256') == publication_pins(), 'Reviewed graph changed after build')
    return result

def validate(args):
    qa = json.loads((args.dist / QA_NAME).read_text())
    result = host_checks(qa, ROOT)
    inputs = args.input or args.build.parent / 'input'
    for name, digest, size in ((BASE_SINGLE, BASE_SINGLE_SHA256, BASE_SINGLE_BYTES),
                              (BASE_BUNDLE, BASE_BUNDLE_SHA256, BASE_BUNDLE_BYTES)):
        raw = (inputs / name).read_bytes()
        require(sha(raw) == digest and len(raw) == size, 'Wrong pinned .64/.197 baseline')
    base, old_bundle = archive(inputs / BASE_SINGLE), archive(inputs / BASE_BUNDLE)
    current, combined = archive(args.dist / SINGLE), archive(args.dist / BUNDLE)
    for items, version in ((base, BASE_VERSION), (old_bundle, BASE_BUNDLE_VERSION),
                           (current, VERSION), (combined, BUNDLE_VERSION)):
        require(headers(items['META-INF/MANIFEST.MF']).get('Version') == version, 'MPP version differs')
        for name in ('ulike/runtime.dex', 'classes.dex'):
            dex_integrity(items[name], name)
    delta = {}
    for kind, old, new in (('standalone', base, current), ('bundle', old_bundle, combined)):
        changed = {name for name in old.keys() & new.keys() if old[name] != new[name]}
        added = set(new) - set(old)
        require(set(old) <= set(new) and changed == CHANGED and added == ADDED, 'Unexpected ' + kind + ' resource delta')
        require(qa.get('changed_' + kind + '_entries') == sorted(changed)
            and qa.get('added_' + kind + '_entries') == sorted(added), 'QA resource inventory differs')
        delta[kind + '_changed'], delta[kind + '_added'] = sorted(changed), sorted(added)
    require({n: b for n, b in base.items() if own(n)} == {n: b for n, b in old_bundle.items() if own(n)}
        and {n: b for n, b in current.items() if own(n)} == {n: b for n, b in combined.items() if own(n)}, 'Standalone/bundle ULike copies differ')
    require(all(combined[n] == b for n, b in old_bundle.items() if not own(n)
        and n not in ('classes.dex', 'META-INF/MANIFEST.MF')), 'Other app resource changed')
    inherited = {n: {'sha256': sha(b), 'bytes': len(b)} for n, b in base.items()
        if n.endswith('.so') or n == 'ulike186/runtime/0000.bin'}
    require(len(inherited) == 12 and inherited == qa.get('inherited_native_payloads')
        and all(current[n] == base[n] for n in inherited), 'One of twelve native payloads changed')
    require(all(current[n] == base[n] for n in (INSTALLER, 'ulike/methods.dex', 'ulike/methods.tsv')), 'Installer/native method payload changed')
    gpu = current['ulike1960/runtime/libulike_gpu1960.so']
    require(sha(gpu) == qa.get('gpu_native_library_sha256') and len(gpu) == qa.get('gpu_native_library_bytes'), 'Retained GPU identity differs')
    artifacts = {name: {'sha256': sha((args.dist / name).read_bytes()), 'bytes': (args.dist / name).stat().st_size} for name in (SINGLE, BUNDLE)}
    require(qa.get('artifacts') == artifacts, 'QA artifact fingerprints differ')
    inventory = json.loads((args.build / 'emitted/camera-inventory1965.json').read_text())
    inventory_checks(inventory)
    require(all(qa.get(key) == value for key, value in inventory.items()), 'Fresh serialized camera inventory differs from QA')
    for name, entry in (('runtime.dex', 'ulike/runtime.dex'), ('loader.dex', 'classes.dex'), ('UlikeHqMaxPatch.class', LOADER)):
        require((args.build / 'emitted' / name).read_bytes() == current[entry], 'Package differs from transformed bytes: ' + entry)
    require((args.build / 'emitted/bundle-loader.dex').read_bytes() == combined['classes.dex'], 'Bundle loader differs from audited bytes')
    require((args.build / 'helper-references.txt').read_bytes().startswith(b'PASS helper references in '), 'Fresh helper linkage proof absent')
    require('exact executable loader implementation preserved' in (args.build / 'metadata.log').read_text(), 'Fresh loader metadata inverse proof absent')
    evidence = {'schema': 'ulike1965-desktop-validation-v1', 'status': 'passed',
        'ulike_version': VERSION, 'bundle_version': BUNDLE_VERSION, 'selected_candidates': SELECTED,
        'artifacts': artifacts, 'resource_delta': delta, 'source_consistency_verified': True,
        'camera_host_evidence_sha256': qa['camera_host_evidence_sha256'], 'host_assertions': result['assertions'],
        'host_reports': result['tests'], 'source_groups': {key: qa[key] for key in SOURCE_GROUPS},
        'inherited_native_payloads': inherited, 'native_installer_byte_identical': True,
        'native_methods_byte_identical': True, 'native_methods_tsv_byte_identical': True,
        'serialized_dex_preservation_verified': True, 'single_image_capture_admission_verified': True,
        'image_pipeline_byte_identical': True, 'intentional_quality_algorithm_change': False,
        'original_apk_apply_tested': False, 'original_split_merge_tested': False,
        'device_tested': False, 'device_quality_verified': False, 'device_save_speed_measured': False,
        'camera_visible_preview_verified_on_device': False,
        'original_apk_application': {'status': 'not_tested', 'reason': 'Original APKS unavailable; camera host and package preservation checks only.'}}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(json_bytes(evidence))
    print('PASS fresh camera/source/package evidence; original APKS and physical Android untested')
    return evidence

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('build', 'dist', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--input', type=Path)
    args = parser.parse_args()
    for name, value in vars(args).items():
        if isinstance(value, Path):
            setattr(args, name, value.resolve())
    validate(args)
