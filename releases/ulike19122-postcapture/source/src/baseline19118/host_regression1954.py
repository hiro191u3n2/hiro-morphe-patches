#!/usr/bin/env python3
"""Execute H22-H27 and independent published .53/.52/.51 host preservation.

Historical suites execute a strict .53 source view. New adoption suites execute
the current .54 production code and use distinct measured assertion counters;
they never recycle historical counts or claim a physical Android speedup.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import sys

BASELINE_SHA256 = '0169006ac7d86349ce6dd274b263332a7bd202845fe8bb28f8c947ae662ee982'
H52_SHA256 = '62428a1bb7856e2b4640640a360b70ea4ff2a8602c7a0df1dfddef80849c6b77'
SELECTED = ['H22', 'H23', 'H24', 'H25', 'H26', 'H27']
NEW_SUITES = {
    'h22_dispatch_batch_exact', 'h23_direct_candidate_readback',
    'h24_exact_coordinate_policy_reuse', 'h25_source_halo_band_reuse',
    'h26_single_pass_policy_fallback', 'h27_frozen_probe_ownership',
    'published1953_pipeline_preservation',
}


def require(value, message):
    if not value:
        raise AssertionError(message)


def load(root, name):
    spec = importlib.util.spec_from_file_location(
        'ulike1954_' + name, Path(root) / (name + '.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def checked(value, label):
    require(isinstance(value, dict) and value.get('status') == 'passed'
            and type(value.get('assertions')) is int and value['assertions'] > 0,
            'Actual passing assertions missing: ' + label)
    return value


def flags(value, names, label):
    for name in names:
        require(value.get(name) is True,
                'Executed ' + label + ' evidence missing: ' + name)


def split(value, key, label):
    require(type(value.get(key)) is int and value[key] > 0,
            'Distinct executed assertion counter missing: ' + key)
    return {
        **value, 'assertions': value[key],
        'assertion_count_basis': 'Actual current .54 ' + label + ' counter '
            + key + '; other counters in this evidence object are counted only '
            'in their corresponding suites.',
    }


def test(root, work, android, baseline):
    root = Path(root).resolve()
    work = Path(work).resolve() / 'host-regression1954'
    work.mkdir(parents=True, exist_ok=True)
    android, baseline = Path(android), Path(baseline)
    require(baseline.is_file()
            and hashlib.sha256(baseline.read_bytes()).hexdigest() == BASELINE_SHA256,
            'Published .53 baseline MPP changed')
    h52 = Path(os.environ.get('ULIKE_H52_BASELINE',
                    str(baseline.parent / 'ULike_HQ_Texture_Online_v1.9.52.mpp')))
    require(h52.is_file()
            and hashlib.sha256(h52.read_bytes()).hexdigest() == H52_SHA256,
            'Pinned .52 MPP required for inherited .53 regression')
    if str(root) not in sys.path:
        sys.path.insert(0, str(root))
    common = load(root, 'host_common1954')
    inherited = common.inherited1953_root(root, work)
    original = checked(load(inherited, 'host_regression1953').test(
        inherited, work, android, h52), 'Independent published .53 regression')
    suites = dict(original['suites'])
    for name, suite in suites.items():
        checked(suite, 'Historical ' + name)
    require(original['assertions'] == sum(s['assertions'] for s in suites.values()),
            'Historical assertion total differs from executed suites')

    gpu = checked(load(root, 'host_finishgpu1953').test(
        root, work, jdk=os.environ.get('ULIKE_JDK_HOME'),
        ndk=os.environ.get('ULIKE_NDK_HOME')), 'Current .54 production GPU')
    flags(gpu, (
        'production_jni_executed', 'mesa_gles_shader_executed',
        'h22_dispatch_batch_pixel_exact', 'h23_direct_candidate_readback_pixel_exact',
        'dispatch_options_pixel_exact_to_pinned_java',
        'sequential_36_row_dependency_halo_preserved',
        'larger_dispatch_batches_reduce_command_count',
        'bounded_two_slot_fences_preserved',
        'direct_candidate_no_native_staging_memcpy',
        'direct_candidate_pixels_equal_staged_readback', 'old_staged_collect_preserved',
        'late_unmap_failure_old_staged_destination_unchanged',
        'late_unmap_failure_private_candidate_written_but_rejected',
        'late_unmap_failure_original_source_unchanged',
        'late_failure_other_pending_slot_drained_and_failed_slot_quarantined',
        'cancelled_private_ticket_destination_unchanged', 'source_immutable',
    ), 'H22/H23 native')
    require(gpu.get('dispatch_batch_sizes_executed') == [32, 64, 0]
            and gpu.get('physical_android_tested') is False,
            'Exact 32/64/full native dispatch execution or host scope missing')
    require(gpu.get('h22_h23_assertions')
            == gpu.get('h22_assertions', -1) + gpu.get('h23_assertions', -1)
            and gpu['assertions']
            == gpu['h22_h23_assertions'] + gpu.get('legacy_session_assertions', -1),
            'Current native distinct assertion totals differ')
    suites['h22_dispatch_batch_exact'] = split(gpu, 'h22_assertions', 'H22 native')
    suites['h23_direct_candidate_readback'] = split(gpu, 'h23_assertions', 'H23 native')
    suites['updated1954_photo_gpu_legacy_preservation'] = split(
        gpu, 'legacy_session_assertions', 'updated H16/H18/H19 preservation')

    engine = checked(load(root, 'host_route1953').test(root, work),
                     'Current .54 dispatch admission engine')
    require(engine.get('actual_java_engine_executed') is True
            and engine.get('physical_android_tested') is False
            and engine.get('gpu_execution_claimed') is False,
            'Current dispatch admission must execute real Java with explicit driver fixtures')
    flags(engine, (
        'h22_actual_dispatch_selection_engine_executed',
        'h22_all_mode_full_pixel_proofs', 'h22_transfer_inclusive_mode_selection',
        'h22_unknown_dispatch_history_requires_fresh_proof',
        'h22_private_candidate_released_between_trials',
        'h22_mode_selection_cancellation_checked',
    ), 'H22 current dispatch selection')
    require(engine.get('h22_dispatch_choices') == [32, 64, 0]
            and type(engine.get('h22_assertions')) is int
            and 0 < engine['h22_assertions'] < engine['assertions'],
            'Distinct current H22 engine assertion counter missing')
    h22 = suites['h22_dispatch_batch_exact']
    h22['native_assertions'] = h22['assertions']
    h22['dispatch_engine_integration'] = engine
    h22['dispatch_engine_assertions'] = engine['h22_assertions']
    h22['assertions'] += engine['h22_assertions']
    h22['assertion_count_basis'] = (
        'Actual current native h22_assertions plus the separately executed '
        'current Java dispatch-admission engine; no historical counts reused.')
    suites['updated1954_background_engine_preservation'] = {
        **engine, 'assertions': engine['assertions'] - engine['h22_assertions'],
        'assertion_count_basis': 'Actual current .54 engine legacy admission '
            'assertions; excludes the distinct H22 adoption counter counted in H22.',
    }

    policy = checked(load(root, 'host_speedpolicy1954').test(root, work, android),
                     'Current H24/H26 coordinate and policy arithmetic')
    flags(policy, (
        'pixel_equivalence_to_published1953',
        'exact_float_coordinate_coefficients', 'constant_policy_shortcut',
        'immutable_mask_single_pass_prefix_expansion_exact',
        'unknown_custom_mask_original_call_order_preserved',
        'policy_integer_equivalence_to_published1953',
    ), 'H24/H26 policy')
    require(policy.get('physical_android_tested') is False
            and policy['assertions']
            == policy.get('h24_assertions', -1) + policy.get('h26_assertions', -1),
            'H24/H26 policy assertion totals differ')
    suites['h24_exact_coordinate_policy_reuse'] = split(policy, 'h24_assertions', 'H24')
    suites['h26_single_pass_policy_fallback'] = split(policy, 'h26_assertions', 'H26')

    library = Path(gpu['library_path'])
    require(library.is_file()
            and hashlib.sha256(library.read_bytes()).hexdigest() == gpu['library_sha256'],
            'Current JNI library changed after H22/H23 native execution')
    lease = checked(load(root, 'host_pipelinelease1954').test(
        root, work, android, gpu_library=library), 'Current H25/H27 GPU ownership')
    flags(lease, (
        'actual_bitmap_gpu_candidate_executed', 'source_halo_suffix_reads_measured',
        'native_coverage_fallback_exact', 'frozen_source_reference_leases_checked',
        'unknown_mutable_owner_never_leased',
        'recycle_resize_save_cancel_failure_cleanup_checked',
        'actual_normalize_cpu_destination_prepared_detail_save_cancel_checked',
    ), 'H25/H27 ownership')
    require(lease.get('reference_snapshot_copies') == 0
            and lease.get('cpu_destination_copies_per_probe') == 1
            and lease.get('physical_android_tested') is False
            and lease['assertions']
            == lease.get('h25_assertions', -1) + lease.get('h27_assertions', -1),
            'H25/H27 executed lease copy or distinct assertion accounting differs')
    suites['h25_source_halo_band_reuse'] = split(lease, 'h25_assertions', 'H25')
    suites['h27_frozen_probe_ownership'] = split(lease, 'h27_assertions', 'H27')

    pipeline = checked(load(root, 'host_pipeline1954').test(root, work, android),
                       'Independent .53 versus current .54 saved pipeline')
    flags(pipeline, (
        'same_output_pixels', 'pixel_equivalence_to_baseline',
        'independent_published1953_source_tree_executed',
        'independent_published1953_bitmap_fixtures_executed',
        'gpu_unavailable_cpu_fallback_exact',
        'captured_options_and_rotation_geometry_exact',
        'source_and_prepared_ownership_preserved', 'copy_failure_fallback_preserved',
        'normalization_and_detail_call_contracts_preserved',
    ), '.53/.54 pipeline')
    require(pipeline.get('baseline_version') == '1.9.53',
            'Forward pixel preservation requires actual published .53 baseline')
    suites['published1953_pipeline_preservation'] = pipeline
    for name, suite in suites.items():
        checked(suite, name)
    require(NEW_SUITES <= suites.keys(), 'Current H22-H27 executed suites incomplete')
    result = {
        **original, 'status': 'passed',
        'assertions': sum(s['assertions'] for s in suites.values()), 'suites': suites,
        'h22_h27_selected': SELECTED, 'selected_candidates': SELECTED,
        'baseline_version': '1.9.53', 'baseline_mpp_sha256': BASELINE_SHA256,
        'inherited1953_oracle_mpp_sha256': H52_SHA256,
        'published1953_source_oracle_sha256': common.source_pins(root)['historical_files'],
        'pixel_equivalence_to_baseline': True,
        'full_resolution_nv21_checked': original['full_resolution_nv21_checked'],
        'gpu_shader_execution_on_host': True, 'gpu_execution_on_physical_android': False,
        'device_tested': False, 'performance_measured_on_device': False,
        'full_float_beauty_chain_gpu_ported': False, 'device_speedup_verified': False,
        'h22_h27_scope': 'Exact 32/64/full dispatch admission, private candidate '
            'readback, exact coordinate/constant policy reuse, covered fresh-suffix '
            'source bands, one-pass eligible policy overflow, frozen probe leases.',
        'assertion_count_basis': 'Sum of distinct executed historical and current '
            'suite counters. Nested evidence and cross-suite evidence are not counted again.',
    }
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False,
                                               sort_keys=True, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--android', type=Path, required=True)
    parser.add_argument('--baseline', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(test(args.root, args.work, args.android, args.baseline),
                     ensure_ascii=False, indent=2))
