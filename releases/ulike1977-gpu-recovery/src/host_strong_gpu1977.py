#!/usr/bin/env python3
"""Execute .77 qualification recovery and all retained CPU/GPU/camera tests.

Controlled Java tests verify failure behavior. Production JNI and software GLES
execute actual shaders. ARM64 arithmetic proofs execute under qemu; no result
establishes physical Android performance or a Galaxy failure cause.
"""
from pathlib import Path
import importlib.util
ROOT = Path(__file__).resolve().parent

def load(name):
    spec=importlib.util.spec_from_file_location(name+'_aggregate1977',ROOT/(name+'.py'))
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve()
    runners=[('strong','tests1976/host_strong1976'),('certificate','host_certificate1973'),
             ('tuning','host_tuning1976'),('gpu76','host_gpu1976'),('timing','host_timing1977'),
             ('native_facade','host_native_facade1976'),('precision','host_precision1973'),
             ('camera','host_camera1973'),('colour_cache','host_colour_cache1976'),('layout','host_layout1976'),('cpu_masks','host_cpu_masks1976'),('cpu_finish_gate','host_cpu_finish_gate1976'),('resident','host_resident1976'),('chain','host_chain1976'),('whole_pipeline','host_resident_pipeline1976'),('recovery','host_recovery1977')]
    reports={}
    for label,name in runners:
        arguments={'jdk':jdk}
        if label in ('gpu76','native_facade','precision','camera','colour_cache','layout','cpu_masks','cpu_finish_gate','resident','chain','whole_pipeline','recovery'):arguments['ndk']=ndk
        report=load(name).test(source,work/label,**arguments)
        if not isinstance(report,dict) or report.get('status')!='passed' or type(report.get('assertions')) is not int or report['assertions']<=0:
            raise AssertionError('Fresh executed positive report absent: '+label)
        if report.get('physical_android_tested') is not False:
            raise AssertionError('Physical Android untested disclosure absent: '+label)
        reports[label]=report
    requirements={
        'strong':['strong_latency_controls_regressions_passed','strong_latency_regressions_passed','latency_budget_regressions_passed','cold_proof_bounds_passed','strong_singleflight_regressions_passed','strong_first_mismatch_identity_regressions_passed','gpu_safety_gates_preserved','strong_preflight1974_regressions_passed','strong_preflight1975_regressions_passed','strong_overlap1975_regressions_passed','strong_upload_reuse1975_regressions_passed','strong_buffer_reuse1975_regressions_passed'],
        'certificate':['qualification_certificate_regressions_passed','gpu_safety_gates_preserved'],
        'tuning':['twelve_candidate_profile_tuning_verified','tuning_full_exact_gate_verified', 'tuning_snapshot_ownership_and_cancel_verified', 'scalar_only_tuning_persistence_verified'],
        'gpu76.memory':['resident_slot_memory_admission1976_verified','native_memory_admission_regressions_passed','native_failure_diagnostics_regressions_passed','gpu_safety_gates_preserved','materialized_java_readback_counted_once','remaining_java_allocation_stays_reserved','reusable_multi_output_readback_verified','private_partial_output_never_committed','overlap_scratch_admission_serial_fallback_verified','multi_output_cancel_and_quarantine_verified'],
        'timing':['stage_details1976_regressions_passed','backend_diagnostics_regressions_passed','backend_visibility_regressions_passed','strong_preference_telemetry_regressions_passed','strong_failure_diagnostics_regressions_passed','strong_work_latency_regressions_passed','strong_full_mismatch_regressions_passed'],
        'native_facade':['native_facade_regressions_passed','gpu_shader_execution_on_host','strong_overlap_actual_jni1975','strong_overlap_failure_drain1975','strong_serial_fallback_actual_jni1975','strong_repeat_no_large_upload1975','strong_reusable_readback_actual_jni1975'],
        'precision':['exact_division_regressions_passed','legacy_program_semantics_preserved','gpu_safety_gates_preserved'],
        'gpu76':['exact_tiled1976_actual_jni','legacy_program_pixels_preserved','full_argb_confidence_exact','cropped_halo_and_two_banks','resident_copy_range1976_actual_jni','virtual_slot24_budget_verified'],
        'camera':['latest_camera_layout_regressions_passed','fresh_camera_host_execution_in_this_release'],
        'colour_cache':['baseline75_exact','actual_production_jni_executed','actual_arm_neon_executed','private_abort_rollback','concurrent_workers_exact'],
        'layout':['geometry_two_exact_trials_verified','geometry_queue_ownership_and_cancel_verified','independent_shape_certificates_required'],
    }
    requirements.update({'colour_cache.runtime_admission': ['production_java_and_jni', 'default_original_loader', 'exact2_and_five_percent_gate', 'actual_mode_branches', 'idle_queue_fixture', 'cancellation_and_memory_refusal'], 'cpu_masks': ['foreground_cpu_finish_cache_and_fallback_verified', 'published75_mask_exact', 'published75_policy_exact', 'published75_native_pixels_exact', 'unknown_callback_order_unchanged', 'source_immutable_and_alpha_edges_exact', 'jni_concurrency_cancel_executed'], 'cpu_finish_gate': ['cpu_finish_two_exact_and_speed_gate_verified', 'cpu_finish_snapshot_cancel_memory_verified'], 'resident': ['resident_two_whole_exact_trials_verified', 'resident_original_route_timing_gate_verified', 'resident_snapshot_cancel_ownership_verified'], 'chain': ['chain_legacy_sync1976_exact_verified','chain_measured_old_gpu_baseline1976_verified','chain_legacy_fallback1976_actual_jni','chain_pipeline1976_actual_jni', 'chain_resident1976_actual_jni', 'chain_source_immutable1976_verified', 'chain_fault_cancel_drain1976_verified', 'chain_unsupported_formats1976_verified', 'chain_cancel_preserves_exact_certificate1976_verified'], 'whole_pipeline': ['actual_whole_pipeline_jni_verified', 'actual_layout_halo_equivalence_verified']})
    requirements['recovery'] = ['qualification_epoch_recovery_passed', 'old_positive_certificates_invalidated', 'old_preferred_certificates_invalidated', 'legacy_blanket_not_treated_as_exact_evidence', 'signed_legacy_individual_failures_preserved', 'exact_journal_655_survives_process_restart', 'unknown_1000_lookups_do_not_persist', 'unknown_keys_require_fresh_proof', 'rejection_families_and_environments_isolated', 'speed_failures_distinct_from_exact', 'hot_failure_cache_bounded_64', 'original_1976_regression_negative_controls_passed', 'new_exact_journal_signature_integrity_passed']
    for label,flags in requirements.items():
        parts=label.split('.');evidence=reports[parts[0]]
        for part in parts[1:]:evidence=evidence.get(part,{})
        if evidence.get('status')!='passed' or evidence.get('physical_android_tested') is not False:raise AssertionError('Missing executed evidence: '+label)
        for flag in flags:
            if evidence.get(flag) is not True:raise AssertionError('Fresh current regression absent: '+label+'/'+flag)
    recovery=reports['recovery']
    proof_schema='gx1964-full-output-parallel-2wins5-v1-per-key-recovery-v1'
    preferred_schema='gx1964-full-output-parallel-2wins5-v1-per-key-recovery-v1-strong-exact2-preferred-v1'
    if recovery.get('qualification_proof_schema1977') != proof_schema or recovery.get('qualification_preferred_schema1977') != preferred_schema or recovery.get('exact_schema1977') != 'gx1977-exact-per-key-v1':
        raise AssertionError('Executed recovery proof schema differs from release declaration')
    result={'status':'passed','assertions':sum(report['assertions'] for report in reports.values()),
            'tests':reports,'physical_android_tested':False,'device_speedup_verified':False,
            'samsung_gpu_failure_root_cause_confirmed':False,'fresh_gpu_execution_in_this_release':True,
            'gpu_execution_on_physical_android':False,
            'qualification_proof_schema1977':proof_schema,'qualification_preferred_schema1977':preferred_schema,'exact_schema1977':'gx1977-exact-per-key-v1'}
    for flags in requirements.values():
        for flag in flags:result[flag]=True
    # These broad flags summarize three separately executed evidence groups,
    # rather than labelling superseded .71 control expectations as fresh tests.
    result['strong_preference_evidence_groups']=['strong','native_facade','certificate']
    result['strong_gpu_preference_regressions_passed']=True
    result['strong_generic_fallback_regressions_passed']=True
    result['superseded_strong_control_expectations']='.74 sequential CPU/GPU proof expectations replaced by .75 independent overlap, retained uploads and private reusable readback tests'
    return result
