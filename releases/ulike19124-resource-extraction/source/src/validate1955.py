#!/usr/bin/env python3
"""Validate fresh NR1-NR4 host execution and exact published-baseline MPP delta.

This checks patch packages and host fixtures, never claims an original-APKS
application, physical Android camera test or measured device speed/quality.
"""
from pathlib import Path, PurePosixPath
import argparse
import json
from build1955 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION,
    BASE_BUNDLE_VERSION, BASE_SINGLE, BASE_BUNDLE, BASE_SINGLE_SHA256,
    BASE_BUNDLE_SHA256, BASE_SINGLE_BYTES, BASE_BUNDLE_BYTES, SINGLE, BUNDLE,
    SELECTED, PRODUCTION, CHANGED, ADDED, NR_ENTRY, require, sha, archive,
    headers, own, dex_integrity, json_bytes)

QA_NAME = 'QA_ULike_v1.9.55.json'
SOURCE_GROUPS = {'compiled_production_source_sha256': '',
    'compiled_transformer_source_sha256': '',
    'compiled_nr_native_source_sha256': 'native1955',
    'executed_host_source_sha256': ''}
HOST_FLAGS = ('single_image_algorithm_executed', 'final_save_pipeline_executed',
    'chroma_on_off_nr_applied', 'legacy_primary_and_residual_disabled_in_new_save_path',
    'captured_noise_off_options_preserved', 'rotation_and_resize_dimensions_preserved',
    'source_bitmap_immutable', 'immutable_streaming_halos_equal_whole_image',
    'private_nr_write_failure_discarded', 'prepared_detail_duplicate_suppressed',
    'completion_capture_identity_preserved', 'copy_oom_failure_rollback_preserved',
    'concurrent_shot_options_isolated', 'f16_wide_colour_gainmap_outside_integer_nr',
    'final_sharpen_flat_noise_non_amplification_checked', 'fresh_production_c_jni_executed',
    'fresh_production_c_jni_pipeline_executed', 'native_java_pixel_equivalence',
    'forced_native_unavailable_java_fallback_executed')


def file_pins(source, pins, prefix=''):
    require(isinstance(pins, dict) and bool(pins), 'Missing compiled/executed source pins')
    for name, digest in pins.items():
        path = PurePosixPath(name)
        require(not path.is_absolute() and '..' not in path.parts and '\\' not in name,
                'Unsafe source path')
        target = source / prefix / name
        require(target.is_file() and sha(target.read_bytes()) == digest,
                'Source changed after compilation/execution: ' + prefix + '/' + name)
    return pins


def host_checks(qa, source):
    require(qa.get('schema') == 'ulike1955-single-noise-v1'
            and qa.get('ulike_version') == VERSION and qa.get('bundle_version') == BUNDLE_VERSION
            and qa.get('baseline_ulike_version') == BASE_VERSION
            and qa.get('baseline_bundle_version') == BASE_BUNDLE_VERSION
            and qa.get('selected_candidates') == SELECTED,
            'Wrong build identity/NR scope')
    result = qa.get('host_quality_result', {})
    reports = result.get('reports', {})
    require(qa.get('host_quality_passed') is True and result.get('status') == 'passed'
            and result.get('selected') == SELECTED and type(result.get('assertions')) is int
            and result['assertions'] > 0
            and {'single_image_algorithm', 'final_save_pipeline', 'single_image_native',
                 'final_save_pipeline_native'} <= reports.keys(),
            'Missing freshly executed NR and final-save host tests')
    require(all((row.get('status') == 'passed' or row.get('passed') is True)
                and type(row.get('assertions')) is int and row['assertions'] > 0
                for row in reports.values())
            and sum(row['assertions'] for row in reports.values()) == result['assertions'],
            'Host report failure or assertion count mismatch')
    require(sha(json.dumps(result, sort_keys=True).encode()) == qa.get('host_quality_result_sha256'),
            'Host result digest mismatch')
    require(all(result.get(key) is True for key in HOST_FLAGS), 'Missing actual NR integration proof')
    require(qa.get('intentional_quality_algorithm_change') is True
            and qa.get('host_pixel_equivalence_to_baseline') is False
            and result.get('pixel_equivalence_to_baseline') is False,
            'Changed NR must not claim baseline pixel equivalence')
    for key in ('original_apk_apply_tested', 'original_split_merge_tested',
                'ci_android_apply_tested', 'device_tested', 'device_quality_verified', 'device_save_speed_measured'):
        require(qa.get(key) is False, 'Unexecuted Android test claimed: ' + key)
    for key in ('physical_android_tested', 'device_speedup_verified', 'device_quality_improvement_verified'):
        require(result.get(key) is False, 'Unverified device result claimed: ' + key)
    require(qa.get('replaced_helper_roots') == sorted(PRODUCTION)
            and json.loads((source/'production1955.json').read_text()) == PRODUCTION,
            'Production helper inventory differs')
    require(qa.get('compiled_production_source_sha256') == {
            name+'.java': sha((source/(name+'.java')).read_bytes()) for name in PRODUCTION}
            and qa.get('production_source_consistency_verified') is True,
            'Current production source differs from MPP compiler input')
    for key, prefix in SOURCE_GROUPS.items():
        file_pins(source, qa.get(key), prefix)
    file_pins(source, result.get('compiled_sources_sha256'))
    file_pins(source, result.get('compiled_native_sources_sha256'))
    require(result.get('host_native_build', {}).get('fresh_build') is True
            and result['host_native_build'].get('cached_native_binary_used') is False
            and result.get('native_parity_cases') == 5 and result.get('native_parity_pixel_differences') == 0
            and reports['single_image_algorithm'].get('nativeAvailable') is False
            and reports['single_image_native'].get('nativeAvailable') is True
            and reports['final_save_pipeline'].get('nativeAvailable') is False
            and reports['final_save_pipeline_native'].get('nativeAvailable') is True,
            'Fresh JNI parity and forced-unavailable Java fallback proof required')
    require(qa.get('capture_fusion_enabled') is False
            and qa.get('capture_begin_image_false_return_verified') is True
            and qa.get('capture_other_methods_and_fields_preserved') is True
            and qa.get('capture_admission_does_not_read_or_transfer_input') is True,
            'Single-image capture admission proof absent')
    return result


def validate(args):
    qa = json.loads((args.dist/QA_NAME).read_text())
    result = host_checks(qa, ROOT)
    inputs = args.input or args.build.parent/'input'
    for name, digest, size in ((BASE_SINGLE, BASE_SINGLE_SHA256, BASE_SINGLE_BYTES),
                              (BASE_BUNDLE, BASE_BUNDLE_SHA256, BASE_BUNDLE_BYTES)):
        raw = (inputs/name).read_bytes()
        require(sha(raw) == digest and len(raw) == size, 'Wrong pinned .54/.187 baseline')
    base, old_bundle = archive(inputs/BASE_SINGLE), archive(inputs/BASE_BUNDLE)
    current, combined = archive(args.dist/SINGLE), archive(args.dist/BUNDLE)
    for items, version in ((base, BASE_VERSION), (old_bundle, BASE_BUNDLE_VERSION),
                           (current, VERSION), (combined, BUNDLE_VERSION)):
        require(headers(items['META-INF/MANIFEST.MF']).get('Version') == version, 'MPP version differs')
        for name in ('ulike/runtime.dex', 'classes.dex'):
            dex_integrity(items[name], name)
    delta = {}
    for kind, old, new in (('standalone', base, current), ('bundle', old_bundle, combined)):
        changed = {name for name in old.keys() & new.keys() if old[name] != new[name]}
        added = set(new)-set(old)
        require(set(old) <= set(new) and changed == CHANGED and added == ADDED,
                'Unexpected package resource delta: '+kind)
        require(qa.get('changed_'+kind+'_entries') == sorted(changed)
                and qa.get('added_'+kind+'_entries') == sorted(added), 'QA resource inventory differs')
        delta[kind+'_changed'], delta[kind+'_added'] = sorted(changed), sorted(added)
    require({name: raw for name, raw in base.items() if own(name)}
            == {name: raw for name, raw in old_bundle.items() if own(name)}
            and {name: raw for name, raw in current.items() if own(name)}
            == {name: raw for name, raw in combined.items() if own(name)}, 'Standalone/bundle ULike copies differ')
    require(all(combined[name] == raw for name, raw in old_bundle.items()
                if not own(name) and name not in ('classes.dex', 'META-INF/MANIFEST.MF')),
            'Other application resource changed')
    inherited = {name: {'sha256': sha(raw), 'bytes': len(raw)} for name, raw in base.items()
                 if name.endswith('.so') or name == 'ulike186/runtime/0000.bin'}
    require(len(inherited) == 8 and inherited == qa.get('inherited_native_payloads')
            and all(current[name] == base[name] for name in inherited), 'Inherited native library changed')
    require(all(current[name] == base[name] for name in ('ulike/methods.dex', 'ulike/methods.tsv')),
            'Legacy native methods changed')
    nr = current[NR_ENTRY]
    require(nr[:6] == b'\x7fELF\x02\x01' and nr[18:20] == b'\xb7\x00'
            and sha(nr) == qa.get('nr_native_library_sha256') and len(nr) == qa.get('nr_native_library_bytes'),
            'NR payload invalid or changed')
    native = qa.get('nr_native_build', {})
    require(native.get('ndk_revision') == '27.2.12479018' and native.get('physical_android_tested') is False
            and native.get('sha256') == sha(nr) and native.get('bytes') == len(nr), 'NR build provenance differs')
    file_pins(ROOT, native.get('sources'), 'native1955')
    require(qa.get('nr_native_jni_exports') == [
                'Java_com_hiro_ulike_SingleNoise1955_nativeAbi', 'Java_com_hiro_ulike_SingleNoise1955_processNative']
            and qa.get('nr_native_load_segment_alignments')
            and all(value >= 16384 for value in qa['nr_native_load_segment_alignments'])
            and set(qa.get('nr_native_needed_libraries', [])) <= {'libc.so', 'libm.so', 'libdl.so'},
            'Native JNI exports, alignment or runtime dependencies differ')
    artifacts = {name: {'sha256': sha((args.dist/name).read_bytes()), 'bytes': (args.dist/name).stat().st_size}
                 for name in (SINGLE, BUNDLE)}
    require(qa.get('artifacts') == artifacts, 'QA MPP fingerprints differ')
    inventory = json.loads((args.build/'emitted/nr-inventory1955.json').read_text())
    require(all(qa.get(key) == value for key, value in inventory.items()), 'Serialized DEX inventory differs')
    log = (args.build/'emitted.log').read_text()
    require(all(value in log for value in ('begin_image_false_return_verified=true',
                'serialized_dex_verified=true', 'unrelated_runtime_preserved=true', 'installer_old_rows_preserved=8')),
            'Fresh serialized/inverse bytecode audit missing')
    for path, entry in (('runtime.dex', 'ulike/runtime.dex'), ('loader.dex', 'classes.dex'),
                        ('UlikeHqMaxPatch.class', 'app/hiro/ulike/patches/UlikeHqMaxPatch.class'),
                        ('IntegrationPayload186.class', 'app/hiro/ulike/patches/IntegrationPayload186.class')):
        require((args.build/'emitted'/path).read_bytes() == current[entry], 'Audited bytes differ from final package')
    require((args.build/'emitted/bundle-loader.dex').read_bytes() == combined['classes.dex'],
            'Audited bundle loader differs from final package')
    evidence = {'schema': 'ulike1955-desktop-validation-v1', 'status': 'passed',
        'ulike_version': VERSION, 'bundle_version': BUNDLE_VERSION, 'selected_candidates': SELECTED,
        'artifacts': artifacts, 'resource_delta': delta, 'source_consistency_verified': True,
        'host_quality_result_sha256': qa['host_quality_result_sha256'], 'host_assertions': result['assertions'],
        'host_reports': result['reports'], 'compiled_production_source_sha256': qa['compiled_production_source_sha256'],
        'source_groups': {key: qa[key] for key in SOURCE_GROUPS}, 'inherited_native_payloads': inherited,
        'nr_native_payload': {'sha256': sha(nr), 'bytes': len(nr)}, 'serialized_dex_preservation_verified': True,
        'single_image_capture_admission_verified': True, 'intentional_quality_algorithm_change': True,
        'original_apk_apply_tested': False, 'original_split_merge_tested': False,
        'device_tested': False, 'device_quality_verified': False, 'device_save_speed_measured': False,
        'original_apk_application': {'status': 'not_tested', 'reason': 'Original APKS unavailable; package and host validation only.'}}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(json_bytes(evidence))
    print('PASS fresh NR1-NR4 host/source/package evidence; original APKS and physical Android untested')
    return evidence


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('build', 'dist', 'output'):
        parser.add_argument('--'+name, type=Path, required=True)
    parser.add_argument('--input', type=Path)
    args = parser.parse_args()
    for name, value in vars(args).items():
        if value is not None:
            setattr(args, name, value.resolve())
    validate(args)
