#!/usr/bin/env python3
"""Execute H16-H21 and preserve the byte-pinned published .52 host oracles.

Production JNI and shaders execute on Mesa, while Android Bitmap, camera and
preferences surfaces are explicit host fixtures. This runner records no Galaxy
execution or device speed claim. Assertion totals count each executed test once:
the session test has distinct H16/H18/H19 counters, and the bitmap integration
assertions belong to H20 rather than also to the .52/.53 pipeline suite.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import sys


BASELINE_SHA256 = '62428a1bb7856e2b4640640a360b70ea4ff2a8602c7a0df1dfddef80849c6b77'
H51_SHA256 = '8b96457398e0a16d66965dbfb38306ba601bf43a7a02b1cdbabcc3ce8ae60bef'
SELECTED = ['H16', 'H17', 'H18', 'H19', 'H20', 'H21']


def require(value, message):
    if not value:
        raise AssertionError(message)


def load(root, name):
    path = Path(root) / (name + '.py')
    spec = importlib.util.spec_from_file_location('ulike1953_' + name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def checked(value, name):
    require(isinstance(value, dict) and value.get('status') == 'passed'
            and type(value.get('assertions')) is int and value['assertions'] > 0,
            'Actual passing assertions missing: ' + name)
    return value


def flags(value, names, label):
    for name in names:
        require(value.get(name) is True,
                'Executed ' + label + ' evidence missing: ' + name)


def test(root, work, android, baseline):
    root = Path(root).resolve()
    work = Path(work).resolve() / 'host-regression1953'
    work.mkdir(parents=True, exist_ok=True)
    android, baseline = Path(android), Path(baseline)
    require(baseline.is_file()
            and hashlib.sha256(baseline.read_bytes()).hexdigest() == BASELINE_SHA256,
            'Published .52 baseline MPP changed')
    h51 = Path(os.environ.get('ULIKE_H51_BASELINE', ''))
    require(h51.is_file()
            and hashlib.sha256(h51.read_bytes()).hexdigest() == H51_SHA256,
            'Pinned .51 MPP oracle required for inherited H12-H15 regression')
    if str(root) not in sys.path:
        sys.path.insert(0, str(root))
    common = load(root, 'host_common1953')
    # Changed-family .52 source files are pinned verbatim. The inherited runner
    # receives those files under their normal names, and its own .51/.50 pinned
    # inputs, so it does not silently run the new .53 implementation as .52.
    inherited_root = common.inherited1952_root(root, work)
    original = checked(load(inherited_root, 'host_regression1952').test(
        inherited_root, work, android, h51), 'Inherited H1-H15 regression')
    suites = dict(original['suites'])
    for name, suite in suites.items():
        checked(suite, 'Inherited ' + name)
    require(original['assertions'] == sum(s['assertions'] for s in suites.values()),
            'Inherited regression assertion total differs from executed suites')

    gpu = load(root, 'host_finishgpu1953').test(
        root, work, jdk=os.environ.get('ULIKE_JDK_HOME'),
        ndk=os.environ.get('ULIKE_NDK_HOME'))
    require(gpu.get('status') == 'passed'
            and type(gpu.get('cases')) is int and gpu['cases'] >= 600
            and type(gpu.get('sequential_chain_cases')) is int
            and gpu['sequential_chain_cases'] >= 200
            and type(gpu.get('pixels_compared')) is int
            and gpu['pixels_compared'] > 4000000,
            'Actual .53 GLES/JNI exact-oracle coverage missing')
    flags(gpu, (
        'mesa_gles_shader_executed', 'production_jni_executed',
        'gpu_chain_pixel_exact', 'java_float_and_mask_policy_exact',
        'sequential_corrected_neighborhood_pixel_exact',
        'source_immutable', 'partial_destination_rows_untouched',
        'private_gpu_owner_thread_executed', 'one_context_bind_per_photo',
        'actual_environment_fingerprint_checked',
        'foreground_environment_query_does_not_initialize_gpu',
        'lossless_raw4_packed2_constant4_gpu_variants_executed',
        'dependency_36_row_halo_pixel_exact',
        'source_uploaded_once_per_photo', 'resident_halo_copies_counted_exactly',
        'photo_token_scoped_halo_no_cross_photo_reuse',
        'adaptive_256_512_core_rows_executed',
        'native_memory_reserved_before_growth', 'two_bounded_slots_executed',
        'second_submit_before_first_collect', 'submit_completion_waits_zero',
        'submitted_java_input_copied_before_return',
        'each_collected_fence_accounted', 'close_drains_before_pool_reuse',
        'interrupted_close_preserves_interrupt_and_drains_both_slots',
        'closed_ticket_destination_unchanged',
        'concurrent_photographs_pixel_exact',
    ), 'H16/H18/H19 production native')
    require(gpu.get('physical_android_tested') is False,
            'Host GPU regression must not claim physical Android execution')
    for candidate, key in (
            ('h16_photo_gpu_session', 'h16_assertions'),
            ('h18_resident_source_halo', 'h18_assertions'),
            ('h19_cpu_gpu_overlap', 'h19_assertions')):
        require(type(gpu.get(key)) is int and gpu[key] > 0,
                'Distinct executed session assertion counter missing: ' + key)
        suites[candidate] = {
            **gpu,
            'assertions': gpu[key],
            'assertion_count_basis':
                'Actual production session test counter ' + key
                + '; other session counters are evidence only and not added here.',
            'gpu_execution_on_physical_android': False,
        }
    require(gpu.get('assertions') == sum(gpu[key] for key in (
        'h16_assertions', 'h18_assertions', 'h19_assertions')),
        'Native session assertion total differs from distinct test counters')

    policy = checked(load(root, 'host_policy1953').test(root, work, android),
                     'H17 lossless policy representations')
    flags(policy, (
        'production_helper_executed', 'unchanged_java_float_mask_policy',
        'no_precision_reduction', 'all_rounded_java_policy_values_exact',
        'signed_mask_wrap_exact', 'uint16_boundary_fallback_exact',
        'borrowed_lease_cleanup_checked', 'unused_sharp_policy_zero_prepare_calls',
    ), 'H17 policy')
    suites['h17_lossless_policy_transfer'] = policy

    library = Path(gpu.get('library_path',
                            work / 'host-finishgpu1953/libulike_finish1953.so'))
    require(library.is_file(), 'Executed .53 host GPU JNI library absent')
    require(hashlib.sha256(library.read_bytes()).hexdigest() == gpu.get('library_sha256'),
            'Native library changed after actual .53 session execution')
    pipeline = checked(load(root, 'host_pipeline1953').test(
        root, work, android, gpu_library=library),
        'Published .52/.53 actual bitmap route preservation')
    flags(pipeline, (
        'same_output_pixels', 'pixel_equivalence_to_baseline',
        'gpu_unavailable_cpu_fallback_exact',
        'captured_options_and_rotation_geometry_exact',
        'source_and_prepared_ownership_preserved',
        'copy_failure_fallback_preserved',
        'normalization_and_detail_call_contracts_preserved',
    ), '.52/.53 pipeline')
    bitmap = checked(pipeline.get('gpu_bitmap_integration'),
                     'H20 real production bitmap GPU integration')
    flags(bitmap, (
        'actual_bitmap_gpu_candidate_executed',
        'whole_cpu_bitmap_candidate_pixel_exact',
        'blank_candidate_without_initial_image_copy',
        'candidate_premultiplication_preserved',
        'independent_background_snapshots_owned',
        'pending_session_cancellation_drains_before_release',
        'gpu_copy_write_interrupt_failure_fallback_exact',
        'published_candidate_shot_ownership_preserved',
        'source_unmodified_before_cpu_fallback',
    ), 'H20 bitmap ownership')
    require(bitmap.get('device_tested') is False,
            'Bitmap fixtures must not claim physical Android execution')
    pipeline = dict(pipeline)
    pipeline['assertions'] -= bitmap['assertions']
    checked(pipeline, '.52/.53 pipeline excluding H20 assertions')
    pipeline['gpu_bitmap_assertions_counted_in'] = 'h20_blank_candidate_ownership'
    pipeline['assertion_count_basis'] = (
        'Actual baseline and updated golden/ownership tests plus worker integration; '
        'the nested GPU bitmap report is evidence only and its assertions are counted in H20.')
    pipeline['comparison_scope'] = 'Pinned published .52 versus actual updated .53 pipeline.'
    suites['published1952_pipeline_preservation'] = pipeline
    suites['h20_blank_candidate_ownership'] = {
        **bitmap,
        'production_jni_executed': gpu['production_jni_executed'],
        'mesa_gles_shader_executed': gpu['mesa_gles_shader_executed'],
        'physical_android_tested': False,
        'assertion_count_basis': 'Actual PipelineGpu1953Test bitmap assertions; '
            'excluded from published1952_pipeline_preservation.',
        'fixture_scope': 'Real finishing sessions and shader with explicit '
            'Android Bitmap/camera/primary-NR fixtures.',
    }

    route = checked(load(root, 'host_route1953').test(root, work),
                    'H21 actual background admission engine')
    idle = checked(load(root, 'host_capture_idle1953').test(root, work, android),
                   'H21 capture and save-stage idle hooks')
    flags(route, (
        'actual_java_engine_executed', 'actual_preferences_profile_logic_executed',
        'foreground_cpu_returns_before_background_gpu',
        'original_foreground_cpu_runs_once', 'successful_associated_save_required',
        'exact_capture_preparation_codec_encoder_idle_required',
        'native_snapshot_budget_reserved_before_clone',
        'queued_probe_budget_released_before_capture_readiness_returns',
        'active_gpu_cancel_release_after_real_drain',
        'background_candidate_never_published', 'full_pixel_equality_required',
        'gpu_timing_includes_candidate_policy_transfer_queue_wait_readback',
        'unknown_environment_never_restores',
        'newly_verified_matching_history_requires_current_exact_proof',
        'qualified_profile_checksum_required',
    ), 'H21 admission')
    flags(idle, (
        'invalid_metadata_delivery_keeps_timing_owner', 'actual_capture_idle_hooks',
        'invalid_delivery_finishes_exact_trace',
        'native_capture_invalidates_background', 'terminal_undelivered_capture_idle',
        'save_count_zero_not_early_idle', 'actual_codec_finally_wakes_background',
    ), 'H21 capture/save idle')
    require(route.get('physical_android_tested') is False
            and route.get('gpu_execution_claimed') is False
            and route.get('foreground_cpu_duplication_after_admission') is False
            and route.get('profile_contains_images_or_masks') is False
            and idle.get('physical_android_tested') is False,
            'Background mock-driver/capture-fixture scope differs from executed tests')
    suites['h21_background_admission'] = {
        **route,
        'assertions': route['assertions'] + idle['assertions'],
        'background_engine_assertions': route['assertions'],
        'capture_idle_hooks': idle,
        'capture_idle_assertions': idle['assertions'],
        'assertion_count_basis': 'Distinct real Java background engine and '
            'real capture/save-stage idle-hook assertions; GPU execution is '
            'covered by the separate H16/H18/H19/H20 suites.',
    }
    for name, suite in suites.items():
        checked(suite, name)
    result = {
        **original,
        'status': 'passed',
        'assertions': sum(value['assertions'] for value in suites.values()),
        'suites': suites,
        'h16_h21_selected': SELECTED,
        'selected_candidates': SELECTED,
        'baseline_version': '1.9.52',
        'baseline_mpp_sha256': BASELINE_SHA256,
        'inherited1952_oracle_mpp_sha256': H51_SHA256,
        'published1952_source_oracle_sha256': common.source_pins(root),
        'pixel_equivalence_to_baseline': True,
        'full_resolution_nv21_checked': original['full_resolution_nv21_checked'],
        'gpu_shader_execution_on_host': True,
        'gpu_execution_on_physical_android': False,
        'device_tested': False,
        'performance_measured_on_device': False,
        'full_float_beauty_chain_gpu_ported': False,
        'device_speedup_verified': False,
        'h16_h21_scope': 'Eligible integer finishing session, exact Java policy '
            'representations, resident dependency halo, bounded two-slot overlap, '
            'blank candidate ownership, background exactness and lifecycle admission.',
        'assertion_count_basis': 'Sum of distinct executed suite counters; nested '
            'evidence objects and cross-suite evidence are not counted again.',
    }
    (work / 'result.json').write_text(json.dumps(
        result, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--android', type=Path, required=True)
    parser.add_argument('--baseline', type=Path, required=True)
    arguments = parser.parse_args()
    print(json.dumps(test(arguments.root, arguments.work,
                          arguments.android, arguments.baseline),
                     ensure_ascii=False, indent=2))
