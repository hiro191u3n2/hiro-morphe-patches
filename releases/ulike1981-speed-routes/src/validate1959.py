#!/usr/bin/env python3
"""Validate H40-H45 exact host/ARM execution and pinned .58 MPP delta.

This checks patch packages and host fixtures, never claims an original-APKS
application, physical Android camera test or measured device speed/quality.
"""
from pathlib import Path, PurePosixPath
import argparse
import json
from build1959 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION,
    BASE_BUNDLE_VERSION, BASE_SINGLE, BASE_BUNDLE, BASE_SINGLE_SHA256,
    BASE_BUNDLE_SHA256, BASE_SINGLE_BYTES, BASE_BUNDLE_BYTES, SINGLE, BUNDLE,
    SELECTED, PRODUCTION, CHANGED, ADDED, NR_ENTRY, require, sha, archive,
    headers, own, dex_integrity, json_bytes)

QA_NAME = 'QA_ULike_v1.9.59.json'
SOURCE_GROUPS = {'compiled_production_source_sha256': '',
    'compiled_transformer_source_sha256': '',
    'compiled_nr_native_source_sha256': 'native1958',
    'executed_host_source_sha256': ''}
HOST_FLAGS = ('smooth_single_image_algorithm_executed', 'smooth_final_save_pipeline_executed',
    'chroma_on_off_nr_applied', 'legacy_primary_and_residual_disabled_in_new_save_path',
    'captured_noise_off_options_preserved', 'rotation_and_resize_dimensions_preserved',
    'source_bitmap_immutable', 'immutable_streaming_halos_equal_whole_image',
    'private_nr_write_failure_discarded', 'prepared_detail_duplicate_suppressed',
    'completion_capture_identity_preserved', 'copy_oom_failure_rollback_preserved',
    'concurrent_shot_options_isolated', 'f16_wide_colour_gainmap_outside_integer_nr',
    'final_sharpen_flat_noise_non_amplification_checked', 'fresh_production_c_jni_executed',
    'fresh_production_c_jni_pipeline_executed', 'native_java_pixel_equivalence',
    'forced_native_unavailable_java_fallback_executed',
    'smooth_stage_partial_write_failure_discarded', 'smooth_stage_partial_write_oom_discarded', 'smooth_model_memory_admission_checked', 'alpha_preservation_checked')


NUMERICAL_FLAGS = ('nr5_actual_image_pyramid_executed', 'nr5_correlated_coarse_noise_reduced', 'nr6_flat_dark_noise_reduced', 'nr6_edge_texture_protection_checked', 'nr7_wide_chroma_noise_reduced', 'nr7_colour_boundary_protection_checked', 'nr8_same_image_nlm_executed', 'nr8_bounded_search_checked')


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
    require(qa.get('schema') == 'ulike1959-h40-h45-v1'
            and qa.get('ulike_version') == VERSION and qa.get('bundle_version') == BUNDLE_VERSION
            and qa.get('baseline_ulike_version') == BASE_VERSION
            and qa.get('baseline_bundle_version') == BASE_BUNDLE_VERSION
            and qa.get('selected_candidates') == SELECTED, 'Wrong .59 identity/H40-H45 scope')
    result = qa.get('host_quality_result', {})
    reports = result.get('reports', {})
    require(qa.get('host_quality_passed') is True and result.get('status') == 'passed'
            and result.get('selected') == SELECTED and type(result.get('assertions')) is int
            and result['assertions'] > 0 and isinstance(reports,dict) and bool(reports),
            'Missing freshly executed H40-H45 host and ARM tests')
    require(all((row.get('status') == 'passed' or row.get('passed') is True)
                and type(row.get('assertions')) is int and row['assertions'] > 0
                for row in reports.values())
            and sum(row['assertions'] for row in reports.values()) == result['assertions'],
            'Host report failure or assertion count mismatch')
    require(sha(json.dumps(result, sort_keys=True).encode()) == qa.get('host_quality_result_sha256'),
            'Host result digest mismatch')
    require(all(result.get(flag) is True for flag in ('host_pixel_equivalence_to_baseline',
            'arm_neon_pixel_equivalence','arm_neon_actual_execution','native_java_pixel_equivalence')),
            'Frozen .58 exact pixel, Java/JNI and actual ARM NEON proof required')
    inherited = result.get('baseline_nr1958_result', {})
    require(inherited.get('status') == 'passed' and inherited.get('selected') == ['NR9','NR10','NR11','NR12','NR13']
            and type(inherited.get('assertions')) is int and inherited['assertions'] > 0
            and all(inherited.get(key) is True for key in HOST_FLAGS), 'Inherited NR9-NR13 gates not executed')
    old_reports = inherited.get('reports',{})
    require({'existing_single_image_algorithm','retained_strong_single_image_algorithm',
            'smooth_acceptance_java','smooth_acceptance_native','smooth_single_image_algorithm',
            'smooth_final_save_pipeline','smooth_single_image_native','smooth_final_save_pipeline_native'} == old_reports.keys()
            and all((row.get('status') == 'passed' or row.get('passed') is True)
                    and type(row.get('assertions')) is int and row['assertions'] > 0 for row in old_reports.values())
            and sum(row['assertions'] for row in old_reports.values()) == inherited['assertions'],
            'Inherited NR report inventory/count differs')
    require(inherited.get('native_parity_cases',0) >= 5 and inherited.get('native_parity_pixel_differences') == 0
            and inherited.get('host_native_build',{}).get('fresh_build') is True
            and inherited['host_native_build'].get('cached_native_binary_used') is False,
            'Inherited JNI numerical/fallback gate missing')
    require(all(old_reports[name].get(key) is True for name in ('smooth_single_image_algorithm', 'smooth_single_image_native') for key in NUMERICAL_FLAGS), 'Missing executed NR9-NR13 numerical proof')
    require(all(old_reports[name].get(key) is True for name in ('smooth_final_save_pipeline', 'smooth_final_save_pipeline_native') for key in ('nr13_geometry_mapping_checked', 'nr13_shared_finish_policy_checked', 'nr13_actual_smoothing_admission_checked', 'nr13_residual_grain_sharpen_suppressed')), 'NR13 shared Java/native/GPU finishing policy evidence absent')
    require(old_reports['smooth_final_save_pipeline'].get('nativeFinishAvailable') is False
            and old_reports['smooth_final_save_pipeline'].get('nr13_native_finish_parity_checked') is False
            and old_reports['smooth_final_save_pipeline_native'].get('nativeFinishAvailable') is True
            and old_reports['smooth_final_save_pipeline_native'].get('nr13_native_finish_parity_checked') is True,
            'NR13 fresh inherited JNI finishing exact-pixel parity evidence absent')
    for name in ('smooth_acceptance_java', 'smooth_acceptance_native'):
        require(all(old_reports[name].get(flag) is True for flag in ('knownTruthFixtures', 'relative1957Comparison', 'halfAndFullNlmAblation')) and old_reports[name].get('physicalAndroidTested') is False,
                'Independent NR9-NR12 acceptance evidence absent: '+name)
    require(old_reports['smooth_acceptance_java'].get('nativeAvailable') is False and old_reports['smooth_acceptance_java'].get('nativeParityCases') == 0
            and old_reports['smooth_acceptance_native'].get('nativeAvailable') is True and old_reports['smooth_acceptance_native'].get('nativeParityCases', 0) >= 5
            and old_reports['smooth_acceptance_native'].get('nativeParityPixelDifferences') == 0, 'Independent fresh-JNI acceptance parity absent')
    require(qa.get('intentional_quality_algorithm_change') is False
            and qa.get('host_pixel_equivalence_to_baseline') is True,
            'Speed-only .59 must preserve frozen .58 pixels')
    for key in ('original_apk_apply_tested','original_split_merge_tested','ci_android_apply_tested',
                'device_tested','device_quality_verified','device_save_speed_measured'):
        require(qa.get(key) is False,'Unperformed Android test claimed: '+key)
    for key in ('physical_android_tested','device_speedup_verified','device_quality_improvement_verified'):
        require(result.get(key) is False,'Unverified device result claimed: '+key)
    require(qa.get('replaced_helper_roots') == sorted(PRODUCTION)
            and json.loads((source/'production1959.json').read_text()) == PRODUCTION,
            'Reviewed production helper inventory differs')
    require(qa.get('compiled_production_source_sha256') == {
            name+'.java': sha((source/(name+'.java')).read_bytes()) for name in PRODUCTION}
            and qa.get('production_source_consistency_verified') is True,
            'Current production source differs from MPP compiler input')
    for key,prefix in SOURCE_GROUPS.items(): file_pins(source,qa.get(key),prefix)
    for block in (result,inherited):
        file_pins(source,block.get('compiled_sources_sha256'))
        file_pins(source,block.get('compiled_native_sources_sha256'))
    require(qa.get('capture_fusion_enabled') is False
            and qa.get('capture_begin_image_false_return_verified') is True
            and qa.get('capture_class_bytecode_identical') is True,'Single-image capture proof absent')
    require(qa.get('nr1_nr4_helper_bytecode_identical') is True
            and qa.get('nr5_nr8_helper_bytecode_identical') is True
            and qa.get('nr13_shared_finish_policy_bytecode_identical') is True
            and qa.get('save_publication_hooks_preserved') is True,'Inherited NR/save algorithm changed')
    return result


def validate(args):
    qa = json.loads((args.dist/QA_NAME).read_text())
    result = host_checks(qa, ROOT)
    inputs = args.input or args.build.parent/'input'
    for name, digest, size in ((BASE_SINGLE, BASE_SINGLE_SHA256, BASE_SINGLE_BYTES),
                              (BASE_BUNDLE, BASE_BUNDLE_SHA256, BASE_BUNDLE_BYTES)):
        raw = (inputs/name).read_bytes()
        require(sha(raw) == digest and len(raw) == size, 'Wrong pinned .58/.191 baseline')
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
                 if (name.endswith('.so') or name == 'ulike186/runtime/0000.bin') and name != NR_ENTRY}
    require(len(inherited) == 10 and inherited == qa.get('inherited_native_payloads')
            and all(current[name] == base[name] for name in inherited), 'Inherited native library changed')
    require(all(current[name] == base[name] for name in ('ulike/methods.dex', 'ulike/methods.tsv')),
            'Legacy native methods changed')
    nr = current[NR_ENTRY]
    require(nr[:6] == b'\x7fELF\x02\x01' and nr[18:20] == b'\xb7\x00'
            and sha(nr) == qa.get('nr_native_library_sha256') and len(nr) == qa.get('nr_native_library_bytes'),
            'NR payload invalid or changed')
    native = qa.get('nr_native_build', {})
    require(native.get('ndk_revision') == '27.2.12479018' and native.get('native_abi') == 1958 and native.get('abi') == 'arm64-v8a' and native.get('min_sdk') == 26 and native.get('library') == 'libulike_smooth1958.so' and native.get('physical_android_tested') is False
            and native.get('sha256') == sha(nr) and native.get('bytes') == len(nr), 'NR build provenance differs')
    file_pins(ROOT, native.get('sources'), 'native1958')
    require(qa.get('nr_native_jni_exports') == [
                'Java_com_hiro_ulike_StrongNoise1958_nativeAbi', 'Java_com_hiro_ulike_StrongNoise1958_processNative', 'Java_com_hiro_ulike_StrongNoise1958_processNativeShared', 'Java_com_hiro_ulike_StrongNoise1958_nativeScratchBytes', 'Java_com_hiro_ulike_StrongNoise1958_nativeReleaseScratch']
            and qa.get('nr_native_load_segment_alignments')
            and all(value >= 16384 for value in qa['nr_native_load_segment_alignments'])
            and set(qa.get('nr_native_needed_libraries', [])) <= {'libc.so', 'libm.so', 'libdl.so'},
            'Native JNI exports, alignment or runtime dependencies differ')
    artifacts = {name: {'sha256': sha((args.dist/name).read_bytes()), 'bytes': (args.dist/name).stat().st_size}
                 for name in (SINGLE, BUNDLE)}
    require(qa.get('artifacts') == artifacts, 'QA MPP fingerprints differ')
    inventory = json.loads((args.build/'emitted/speed-inventory1959.json').read_text())
    require(all(qa.get(key) == value for key, value in inventory.items()), 'Serialized DEX inventory differs')
    log = (args.build/'emitted.log').read_text()
    require(all(value in log for value in ('begin_image_false_return_verified=true',
                'serialized_dex_verified=true', 'unrelated_runtime_preserved=true', 'installer_old_rows_preserved=10')),
            'Fresh serialized/inverse bytecode audit missing')
    for path, entry in (('runtime.dex', 'ulike/runtime.dex'), ('loader.dex', 'classes.dex'),
                        ('UlikeHqMaxPatch.class', 'app/hiro/ulike/patches/UlikeHqMaxPatch.class'),
                        ('IntegrationPayload186.class', 'app/hiro/ulike/patches/IntegrationPayload186.class')):
        require((args.build/'emitted'/path).read_bytes() == current[entry], 'Audited bytes differ from final package')
    require((args.build/'emitted/bundle-loader.dex').read_bytes() == combined['classes.dex'],
            'Audited bundle loader differs from final package')
    evidence = {'schema': 'ulike1959-desktop-validation-v1', 'status': 'passed',
        'ulike_version': VERSION, 'bundle_version': BUNDLE_VERSION, 'selected_candidates': SELECTED,
        'artifacts': artifacts, 'resource_delta': delta, 'source_consistency_verified': True,
        'host_quality_result_sha256': qa['host_quality_result_sha256'], 'host_assertions': result['assertions'],
        'host_reports': result['reports'], 'compiled_production_source_sha256': qa['compiled_production_source_sha256'],
        'source_groups': {key: qa[key] for key in SOURCE_GROUPS}, 'inherited_native_payloads': inherited,
        'nr_native_payload': {'sha256': sha(nr), 'bytes': len(nr)}, 'serialized_dex_preservation_verified': True,
        'single_image_capture_admission_verified': True, 'intentional_quality_algorithm_change': False,
        'original_apk_apply_tested': False, 'original_split_merge_tested': False,
        'device_tested': False, 'device_quality_verified': False, 'device_save_speed_measured': False,
        'original_apk_application': {'status': 'not_tested', 'reason': 'Original APKS unavailable; package and host validation only.'}}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(json_bytes(evidence))
    print('PASS fresh H40-H45 host/ARM/source/package evidence; original APKS and physical Android untested')
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
