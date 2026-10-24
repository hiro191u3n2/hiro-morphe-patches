#!/usr/bin/env python3
"""Fail-closed publication gate for exact .117 artifacts and fresh host/APKS evidence.

This is a desktop/build gate. It cannot claim Android GPU speed, first-shot GPU
execution, or device quality from software GLES and original-APKS application.
"""
from pathlib import Path, PurePosixPath
import argparse, hashlib, json, re, stat, zipfile
from build19101 import (ROOT, VERSION, BUNDLE_VERSION, SINGLE, BUNDLE, BASE_PINS,
    GPU_ENTRY, NR_ENTRY, SMOOTH_ENTRY, CHANGED, TOOL_PINS, source_pins, publication_pins, production_path,
    headers, dex_integrity, sha, require, json_bytes, runtime_source_version)
from finalize19101 import QA_NAME, SOURCE_ZIP, SELECTED
import application_evidence19101
import native_identity19101
import native_provenance19115
import native_smooth_provenance19115
import source_inverse19117
import regional_distribution19115
import host_evidence19115

ASSETS=(SINGLE,BUNDLE,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt','SHA256SUMS.txt')
FALSE_QA=('device_tested','physical_android_tested','device_speed_measured','device_quality_verified',
    'gpu_execution_on_physical_android','first_shot_gpu_verified_on_physical_android',
    'samsung_gpu_failure_root_cause_confirmed','all_processing_on_gpu','capture_fusion_enabled','device_capture_variance_improvement_verified')
REQUIRED_HOST_GROUPS=frozenset(('logcopy95','shutter94','front94','queue95','runtime_version','fingerprint','release_gate','startup','strong_timing95',
    'timing','route_failure92','memory','gpu_math','strong_adaptive94','strong_transport95','model_layout94','native91',
    'finish_memory92','chain','finish_domain','pipeline_gate','failure92','finish_recovery95','model_parallel96','cpu_copy96','front_timing96','memory97','front_plan97','mask_rows98','front99','chain99','capture99','memory108','resident_memory108'))

CURRENT_PROOF_KEYS={
    'strong_opt114':('actual_gles','immutable_inputs_preserved','split_dispatch_tested','full_luma_ratio_shared_exactly','standard_division_algorithm_unchanged','sampling_and_accumulation_order_preserved','current_shader_executed19115'),
    'front_opt114':('actual_arm_neon_executed','existing_jni_transaction_fixture'),
    'finish_opt114':('published113_masks_equal','exact_cpu_oracle_equal','nonfinite_domain_declined','rotated_fallback_verified','actual_jni','original_rounding_preserved','operation_reduction_verified'),
    'front_opt113':('actual_arm_neon_executed','existing_jni_transaction_fixture'),
    'strong_opt113':('actual_gles','immutable_inputs_preserved','split_dispatch_tested','full_luma_ratio_shared_exactly','current_shader_executed19115'),
    'finish_memory92':('current_phase_owned_contract19112','retained_finish_phase111'),
    'resident_finish19112':('resident128_exact_pixels','resident_profile_key_bound','resident_phase_budget_verified','resident_failure_ownership_verified','actual_jni','resident_finish_source_inverse19112','foreground_profile_toctou_rejected','faulty_guard_removal_reproduces_128','stable_profiles_preserved','preflight_closed_on_all_paths'),
    'resident_plan19112':('current_resident_planner_executed19112','accepted_degraded_resident_reclaimed19112','same_budget_replan19112','cpu_lease_cancel_preserved19112','published111_plan_equivalence19112','resident_plan_source_inverse19112','published111_anonymous_abi_preserved19112'),
    'strong_cancellation19112':('fixture_corpus_bit_exact','old_unchecked_map_interrupt_reproduced','exact_comparison_unchanged','full_256_row_speed_trials_preserved','partial_certificate_writes_zero','diagnostic_failure_nonfatal','checkpoints_outside_timed_gpu_calls','native_and_gpu_drain_unchanged','published111_restored_exactly'),
    'resident_timing111':('resident_timing_source_inverse19111','actual_timing_observer_executed19111','exclusive_timing_boundaries19111','timing_optional_failure_isolation19111','timing_cancel_state_preserved19111','timing_scalar_thread_ownership19111'),
    'strong_counts111':(),
    'diagnostics111':('legacy_vj8_269_prefix_preserved19115','legacy_cpu_tail_zero_checked19115','resident_route_labels111','resident_timing_scalar_scope111','resident_timing_vj8_restart111','legacy_vj1_vj7_preserved111','resident_timing_history_not_admission111','independent_jvm_vj8_restore111'),
    'strong_admission110':('strong_admission_same_budget_retry19110','strong_admission_typed_refusal19110','strong_admission_live_ownership19110','strong_admission_cancellation19110','strong_admission_repeat_trial19110','strong_admission_actual_jni19110','strong_admission_optional_report19110','strong_admission_original_budget19110'),
    'copy_admission110':('resident_strong_typed_classification19110','resident_incomplete_gate_preserved19110','finish_pressure_order_reproduced19110','next_capture_resident_rotation_verified19110','copy_ownership_and_limits_preserved19110','actual_resident_queue_executed19110','actual_finish_pressure_and_copy_budget_executed19110','published108_strong_generic_exception_reproduced19110','resident_source_inverse19110'),
    'strong_diagnostics110':('strong_admission_scalar_scope','strong_admission_vj7_restart','legacy_vj1_vj6_preserved','diagnostic_history_cannot_grant_admission'),
    'resident_memory108':('resident_memory_classified', 'resident_memory_retry_not_exact_or_speed', 'resident_memory_scalar_scope', 'resident_memory_vj6_restart', 'resident_memory_history_no_admission', 'resident_two_exact_two_speed_gates', 'actual_resident_producer_queue_journal'),
    'memory108':('current_shader_compiled19113','memory_transport_source_unchanged19113','resident_memory_same_budget_retry19108', 'resident_memory_active_gpu_preserved19108', 'resident_memory_busy_cpu_preserved19108', 'resident_memory_cancellation_preserved19108', 'resident_memory_original_paths_preserved19108', 'resident_memory_actual_jni19108', 'resident_memory_bounded_report19108', 'resident_memory_typed_refusal19108', 'resident_memory_carried_scope19108'),
    'cache107':('legacy_ingress_sites_preserved19115','regional_candidate_ingress_guard_verified19115','cache_fingerprint_isolation_verified','all_cache_families_covered','regional_evidence_domain_verified','invalid_ingress_rejected_before_gpu','generated_regional_evidence_accepted'),
    'regional107':('actual_gles_execution','independent_rational_oracle','independent_java_oracle','exact_round_to_nearest_even','all_dyadic_weights_0_to_128','regional_coordinate_sampling_verified'),
    'diagnostics106':('journal_v1_v2_v3_v4_v5_compatible','trace_scope_bound','trace_scalar_snapshot_immutable','strict_mismatch_preserved'),
    'trace_flow106':('entry_conditions_verified','diagnostic_reproduction_failures_distinct','bounded_status_and_cancellation_verified','no_false_cause_attribution'),
    'strong_trace106':('actual_wrapper_jni','actual_jni','paired_raw_checkpoints','traced_row_matches_production','cpu_trace_matches_native_java','arithmetic_clone_guarded','negative_controls_verified','source_width_3060','source_and_policy_preserved','cancellation_and_invalid_geometry_verified'),
    'strong_inputs105':('fixed_inputs_shared_by_three_backends','actual_stage_replay_gate','foreground_inert','bounded_halo_and_memory','native_fallback_distinct','mutation_not_certified','cancellation_propagated'),
    'strong_math105':('frozen_java_expected','source_width_3060','actual_jni','confidence_on_off_same_argb','unaligned_single_row_same_argb','both_gpu_banks','source_and_policy_preserved','cancelled_row_output_preserved','native_and_explicit_java_compared'),
    'diagnostics105':('diagnostic_reasons_separated','legacy_failure_not_instability','known_empty_mask_observed','fixed_replay_api_bounded','journal_v1_v2_v3_v4_compatible','queue_journal_extended_scalars','strict_mismatch_preserved'),
    'raw_crop104':('actual_jni_gpu','source_unchanged'),
    'validation104':('stage_attribution_exact','cpu_replay_instability_not_gpu_blame','foreground_gpu_untouched','gpu_crop_exact','gpu_lease_cleanup','diagnostic_bounded','original_mismatch_preserved','journal_v1_v2_v3_compatible'),
    'face_reuse104':('baseline_double_detection_reproduced','successful_preanalysis_reused','same_mask_outputs','failure_fallback_preserved','identity_guards_preserved','thread_local_completion_verified'),
    'single_admission104':('baseline103_supplementary_admission_reproduced','single104_no_extra_request_copy_or_analysis','single104_original_q0_delivery_preserved'),
    'capture_phases104':('post_acquisition_phases_exact_owner_verified','phase_replay_and_ambiguity_safe','state_wait_not_inferred_from_result_entry'),
    'runtime_version':('distributed_regional_dex_absence_verified','single_capture_dex_invariant_negative_controls','nested_diagnostic_version_literals_verified','legacy_timing_reader_exception_scoped'),
    'validation103':('mismatch_survives_optional_replay_cancel','journal_v1_v2_v3_compatible','first_difference_scalar_evidence','reference_candidate_incomplete_distinct','negative_admission_preserved','legacy_journal_and_current_version'),
    'source_mask103':('independent_reference','parallel_ownership_verified'),
    'burst103':('legacy_admission_fixture_only','real_burst_hooks_verified','replay_does_not_retime_image','preview_results_excluded'),
    'capture103':('exact_capture_arrival_verified','sensor_clock_subtraction_absent','generation_reuse_rejected','existing_still_result_filters_retained'),
    'journal101':('actual_qualification_journal_bridge','lifecycle_photo_process_version_bound','journal_io_off_queue_lock','loaded_proof_count_scope_explicit','diagnostic_history_cannot_grant_admission'),
    'journal_adversarial101':('vj3_stage_scalar_roundtrip','vj1_vj2_zero_extension','stage_signature_exact_result_distinct','unverified_replay_not_attributed_to_gpu','flat_evidence_not_image_coordinates','stage_schema_ranges_checked','separate_jvm_restart_unknown_and_terminal','delayed_load_preserves_current_updates','in_flight_generation_does_not_ack_newer_state','failed_commit_recovery_is_honest','bounded_corruption_checked_history','storage_off_caller_and_proof_store_untouched'),
    'timing_history101':('real_journal_process_identity','saved_snapshot_process_identity','cross_process_photo_live_separation','legacy_photo_identity_unknown','empty_current_history_explicit','missing_journal_identity_unknown','diagnostic_snapshot_immutable'),
    'front100':('front19100_same_argb','front19100_same_dct_raw_bits','front19100_same_model_raw_bits','front19100_arm_neon_executed','front19100_emulated_cpu_benchmarked','front19100_benchmark_same_process'),
    'front_admission100':('foreground_snapshot_and_deferred_peak_separated','deferred_execution_memory_rechecked','snapshot_cancel_allocation_ownership_verified','copy_decision_reasons_preserved','actual_worker_owner_trace_isolated','unqualified_and_other_strip_gpu_rejected','retained_exact_and_speed_gates','retained_frontmodel_cases'),
    'model100':('whole_model_bitwise_equal','model_preparation_old_new_bitwise_equal','model_local_luma_ratio_shared_exactly','model_original_fp32_order_preserved','actual_jni'),
    'strong100':('whole_model_bitwise_equal','whole_final_image_bitwise_equal','two_independent_cpu_oracles','gpu_model_calls_zero','native_cpu_and_java_fallback_executed','cancellation_and_recovery','original_snapshot_ownership_preserved'),
    'diagnostics100':('scalar_failure_isolation','native_program_failure_cache'),
    'gpu_math':('strict19100_sharing_extension_verified','regional_coordinates_shared_exactly','local_luma_ratio_shared_exactly','local_luma_ratio_full_strong19113','local_luma_ratio_profile6_only19113','historical_shader112_source_contract','inherited_legacy_contract_restored','regional19107_exact_delta_verified'),
}
CURRENT_PROOF_KEYS.update(host_evidence19115.KEYS)
CURRENT_PROOF_KEYS['source_inverse117']=('exact_previous_sources_verified',)
CURRENT_PROOF_KEYS['resident_fair_plan116']=('actual_production_graph_compiled','incomparable_plan_speed_rejection_blocked')
CURRENT_PROOF_KEYS["timing"]=("actual_regional_output_counts_verified",)
CURRENT_PROOF_KEYS['release_gate']=('publication_metadata115_verified',)
REQUIRED_HOST_GROUPS=REQUIRED_HOST_GROUPS|frozenset(CURRENT_PROOF_KEYS)

def checked_name(name):
    path=PurePosixPath(name)
    require(isinstance(name,str) and bool(name) and not path.is_absolute() and '..' not in path.parts
        and '\\' not in name and str(path)==name and not name.endswith('/'),'Unsafe archive/source path: '+str(name))
    return path

def checked_archive(path):
    with zipfile.ZipFile(path) as archive:
        names=archive.namelist();require(len(names)==len(set(names)),'Duplicate ZIP member: '+str(path))
        for item in archive.infolist():
            checked_name(item.filename.rstrip('/'))
            require(not stat.S_ISLNK(item.external_attr>>16),'ZIP symlink: '+item.filename)
            require(not item.flag_bits&1,'Encrypted release member: '+item.filename)
        require(archive.testzip() is None,'ZIP CRC failure: '+str(path))
        return {item.filename:archive.read(item) for item in archive.infolist() if not item.is_dir()}

def pin_files(base,pins):
    require(isinstance(pins,dict) and bool(pins),'Empty source pin set')
    for name,digest in pins.items():
        checked_name(name);path=base/name
        require(re.fullmatch('[a-f0-9]{64}',digest or '') is not None,'Malformed source SHA: '+name)
        require(path.is_file() and not path.is_symlink() and sha(path.read_bytes())==digest,'Source bytes changed: '+name)

def positive_host105(tests):
    def integer(row,key,expected,label):
        require(type(row.get(key)) is int and row[key]==expected,'Strong .105 corpus count differs: '+label+'/'+key)
    inputs=tests['strong_inputs105']
    require(type(inputs.get('cases')) is int and inputs['cases']>=34 and type(inputs.get('assertions')) is int and inputs['assertions']>=157,'Frozen Strong input coverage incomplete')
    require(inputs.get('fixture_classes_in_runtime') is False,'Frozen Strong contract peers entered runtime')
    math=tests['strong_math105']
    for key in ('actual_arm_neon_executed','physical_android_tested','device_speedup_verified','device_mismatch_reproduced'):
        require(math.get(key) is False,'Strong .105 host-only or unchanged-math scope differs: '+key)
    require(math.get('shader_math_changed') is True and math.get('regional19107_exact_delta_verified') is True,'Strong corpus did not execute reviewed regional correction')
    for key,value in (('records',98),('pixels',2234848)):
        integer(math.get('corpus',{}),key,value,'corpus')
    boundaries=math.get('boundary_rows')
    require(type(boundaries) is list and all(type(n) is int for n in boundaries) and boundaries==[0,1,32,94,255,256,278,299],'Strong .105 row-boundary corpus differs')
    gpu=math.get('gpu',{})
    require(gpu.get('status')=='passed' and gpu.get('physical_android_tested') is False,'Strong .105 actual JNI/GPU replay missing')
    for key,value in (('records',98),('reads',2008),('compared_pixels',10310656),('single_rows',400),('confidence_off_checks',1004),('argb_differences',0)):
        integer(gpu,key,value,'gpu')
    require(type(gpu.get('assertions')) is int and gpu['assertions']>0,'Strong .105 GPU assertions missing')
    cpu=math.get('cpu',{})
    require(type(cpu) is dict and set(cpu)=={'native','fallback'},'Strong .105 CPU backend coverage differs')
    for name,native_rows,java_rows in (('native',400,400),('fallback',0,800)):
        row=cpu[name]
        require(row.get('status')=='passed' and row.get('physical_android_tested') is False,'Strong .105 CPU replay missing: '+name)
        for key,value in (('records',98),('rows',400),('native_rows',native_rows),('java_rows',java_rows),('compared_words',4141680)):
            integer(row,key,value,'cpu/'+name)
        require(type(row.get('assertions')) is int and row['assertions']>0,'Strong .105 CPU assertions missing: '+name)
    require(type(math.get('assertions')) is int and math['assertions']==gpu['assertions']+sum(row['assertions'] for row in cpu.values())+1,'Strong .105 assertion total differs')
    require(type(math.get('gl_environment')) is str and bool(math['gl_environment']),'Strong .105 GL identity missing')
    require(type(math.get('oracle_sha256')) is str and re.fullmatch('[a-f0-9]{64}',math['oracle_sha256']) is not None,'Strong .105 independent oracle digest missing')
    diagnostic=tests['diagnostics105']
    require(type(diagnostic.get('cases')) is int and diagnostic['cases']>=64,'Diagnostic .105 case coverage incomplete')
    for name in ('reason_tests','journal_tests','retained_validation'):
        row=diagnostic.get(name,{})
        require(row.get('status')=='passed' and type(row.get('assertions')) is int and row['assertions']>0,'Diagnostic .105 executed subtest missing: '+name)
    require(diagnostic['assertions']==sum(diagnostic[name]['assertions'] for name in ('reason_tests','journal_tests','retained_validation')),'Diagnostic .105 assertion total differs')
    require(diagnostic['reason_tests']['assertions']+diagnostic['journal_tests']['assertions']>=284 and diagnostic['retained_validation']['assertions']>=242,'Diagnostic .105 new or retained coverage incomplete')

def positive_host106_trace(tests):
    diagnostics=tests['diagnostics106']
    require(type(diagnostics.get('cases')) is int and diagnostics['cases']>=99 and type(diagnostics.get('assertions')) is int and diagnostics['assertions']>=844,'Current diagnostic journal coverage incomplete')
    for key, cases, assertions in (('trace_tests',35,222),('retained_diagnostics',64,622)):
        row=diagnostics.get(key,{})
        require(row.get('status')=='passed' and type(row.get('cases')) is int and row['cases']>=cases and type(row.get('assertions')) is int and row['assertions']>=assertions,'Current diagnostic executed coverage absent: '+key)
        require(row.get('physical_android_tested') is False,'Diagnostic peers claim physical execution')
    require(diagnostics['assertions']==diagnostics['trace_tests']['assertions']+diagnostics['retained_diagnostics']['assertions'],'Diagnostic journal assertion accounting differs')
    flow=tests['trace_flow106']
    require(type(flow.get('cases')) is int and flow['cases']>=40 and type(flow.get('assertions')) is int and flow['assertions']>=111,'Trace orchestrator coverage incomplete')
    require(flow.get('fixture_classes_in_runtime') is False,'Trace orchestrator fixture peers entered runtime')
    trace=tests['strong_trace106']
    require(trace.get('physical_android_tested') is False and trace.get('device_mismatch_reproduced') is False,'Trace host evidence overclaims physical GPU result')
    for name in ('cpu','gpu'):
        row=trace.get(name,{})
        require(row.get('status')=='passed' and type(row.get('assertions')) is int and row['assertions']>0,'Missing executed trace '+name)
        require(type(row.get('records')) is int and row['records']==79,'Trace corpus differs: '+name)
    cpu,gpu=trace['cpu'],trace['gpu']
    for key in ('nlm_on','nlm_off','sparse','near_half','checkpoints','cpu_row_pixels'):
        require(type(cpu.get(key)) is int and cpu[key]>0,'Trace branch or rounding coverage absent: '+key)
    for key,value in (('negative_controls',5),('raw_differences',0),('compared_row_pixels',261850)):
        require(type(gpu.get(key)) is int and gpu[key]==value,'Trace GPU evidence differs: '+key)
    require(type(gpu.get('compared_checkpoint_words')) is int and gpu['compared_checkpoint_words']==cpu['checkpoints'],'Trace checkpoint accounting differs')
    arithmetic=trace.get('source_arithmetic',{})
    require(type(arithmetic) is dict and set(arithmetic)=={'guide','sample','nonlocalFine','regionalSigma','main_0','main_1','main_2','main_3','main_nlm'},'CPU trace arithmetic clone proof absent')
    require(all(type(value) is str and re.fullmatch('[a-f0-9]{64}',value) is not None for value in arithmetic.values()),'CPU trace arithmetic proof malformed')
    wrapper=trace.get('wrapper',{})
    require(wrapper.get('status')=='passed' and type(wrapper.get('assertions')) is int and wrapper['assertions']>=10 and wrapper.get('actual_wrapper_jni') is True and wrapper.get('physical_android_tested') is False,'Actual Strong trace wrapper evidence missing')
    require(trace.get('assertions')==cpu['assertions']+gpu['assertions']+wrapper['assertions']+len(arithmetic)+2,'Trace assertion accounting differs')
    require(trace.get('boundary_rows')==[0,1,31,32,63,80,94,255,256,278,520,539] and all(type(n) is int for n in trace['boundary_rows']),'Trace boundary coverage differs')
    require(type(trace.get('gl_environment')) is str and bool(trace['gl_environment']),'Trace GL environment missing')
    require(type(trace.get('trace_corpus_sha256')) is str and re.fullmatch('[a-f0-9]{64}',trace['trace_corpus_sha256']) is not None,'Trace corpus digest missing')


REGIONAL107_COVERAGE={'dyadic_grid_patterns':12,'dyadic_grid_cases':199692,'weight_pairs':16641,
    'exponent_boundary_cases':12240,'seeded_random_cases':8192,'integerization_boundary_cases':5120,
    'total_cases':225244,'zero_results':19552,'subnormal_results':47851,'nonfinite_results':0}
REGIONAL107_GEOMETRIES=((1,1,0,1),(63,65,0,4095),(64,64,0,4096),(65,129,0,8385),
    (127,129,0,16383),(128,129,0,16512),(129,129,0,16641),(3060,4080,0,58140),
    (3060,4080,4,18360),(3060,4080,62,12240))

def positive_host107_regional(tests):
    regional=tests['regional107']
    for key,value in (('arithmetic_negative_controls',4),('regional_compared_words',929118),('exact_mismatches',0),('assertions',1380012)):
        require(type(regional.get(key)) is int and regional[key]==value,'Regional execution coverage differs: '+key)
    for key in ('physical_android_tested','device_speedup_verified','device_mismatch_reproduced'):
        require(regional.get(key) is False,'Regional host proof claims physical result: '+key)
    require(regional.get('physical_log_values_are_not_test_inputs') is True,'Synthetic inputs mislabeled as physical reproduction')
    require(regional.get('full_strong_regression_required')==['strong_trace106','strong_math105'],'Full Strong retained regressions missing')
    coverage=regional.get('coverage')
    require(type(coverage) is dict and set(coverage)==set(REGIONAL107_COVERAGE),'Regional corpus inventory differs')
    for key,value in REGIONAL107_COVERAGE.items():
        require(type(coverage[key]) is int and coverage[key]==value,'Regional corpus count differs: '+key)
    ties=regional.get('rounding_ties',{})
    for key in ('even_lower','even_upper','underflow'):
        require(type(ties.get(key)) is int and ties[key]>0,'Round-to-nearest-even branch not covered: '+key)
    java=regional.get('java',{})
    require(type(java.get('cases')) is int and java['cases']==225244 and java.get('strictfp') is True and java.get('canonical_nan') is True,'Independent Java oracle execution missing')
    gles=regional.get('gles',{})
    require(gles.get('status')=='passed' and gles.get('actual_gles_execution') is True,'Actual regional GLES execution missing')
    for key,value in (('cases',225244),('invalid_domain_cases',10),('weight_gate_cases',392)):
        require(type(gles.get(key)) is int and gles[key]==value,'Regional GLES coverage differs: '+key)
    controls=gles.get('negative_controls')
    expected_controls={'output_lsb':225244,'reversed_horizontal_weights':165402,'truncate_input_mantissa':81276,'scale_ties_away_instead_of_even':2684}
    require(type(controls) is list and len(controls)==4 and all(type(row) is dict for row in controls),'Regional arithmetic negative controls missing')
    require([row.get('name') for row in controls]==list(expected_controls),'Regional arithmetic mutation inventory differs')
    for row in controls:
        require(type(row.get('mismatches')) is int and row['mismatches']==expected_controls[row['name']],'Regional mutation escaped independent oracle: '+row['name'])
    groups=gles.get('regional_groups')
    require(type(groups) is list and len(groups)==10,'Regional coordinate geometry inventory differs')
    for row,(w,h,first,points) in zip(groups,REGIONAL107_GEOMETRIES):
        expected={'width':w,'height':h,'first_grid_y':first,'points':points,'planes':3,'variants':2}
        require(type(row) is dict and set(row)==set(expected) and all(type(row[k]) is int and row[k]==v for k,v in expected.items()),'Regional coordinate/plane/variant execution differs')
    for key in ('renderer','gl_version'):
        require(type(gles.get(key)) is str and bool(gles[key]),'Regional GL environment missing: '+key)
    for obj,key in ((regional,'corpus_sha256'),(gles,'helper_sha256')):
        require(type(obj.get(key)) is str and re.fullmatch('[a-f0-9]{64}',obj[key]) is not None,'Regional executable corpus identity missing: '+key)

def positive_host110(tests):
    strong=tests['strong_admission110']
    require(strong.get('physical_android_tested') is False and strong.get('pixel_equivalence_tested') is False,
        'Strong admission .110 host scope differs')
    strong_reports=strong.get('tests')
    require(type(strong_reports) is dict and set(strong_reports)=={'strong_native','original_budget','published108_regression'},
        'Strong admission .110 executed subtests missing')
    executed=strong_reports['strong_native'];inverse=strong_reports['original_budget']
    require(type(executed) is dict and executed.get('status')=='passed' and type(executed.get('assertions')) is int
        and executed['assertions']>=252 and type(executed.get('cases')) is int and executed['cases']>=19
        and type(executed.get('rounds')) is int and executed['rounds']==2,'Strong admission .110 repeated execution coverage incomplete')
    for key in ('actual_strong_java_executed','actual_memory_java_executed','actual_native_pools_executed','actual_gpu_lease_executed'):
        require(executed.get(key) is True,'Strong admission .110 actual execution missing: '+key)
    require(executed.get('physical_android_tested') is False and executed.get('pixel_equivalence_tested') is False,
        'Strong admission .110 native scope differs')
    require(type(inverse) is dict and inverse.get('status')=='passed' and type(inverse.get('assertions')) is int
        and inverse['assertions']==13 and inverse.get('reviewed_inverse') is True and inverse.get('current_source_executed_separately') is True
        and inverse.get('original_sha256')=='27fc632074e7adaf45e6f55422167ed25b383d3a3695a5f8895a3d212ab429e5',
        'Strong admission .110 original capacity/ownership proof missing')
    require(inverse.get('shader_source_inverse19113_sha256')=='4f1bb151e3e49636d71db2abb6dad1b89d1a8572317af525069c306515605e36' and type(inverse.get('shader_source_inverse19113_negative_controls')) is int and inverse['shader_source_inverse19113_negative_controls']==3 and inverse.get('memory_transport_source_unchanged19113') is True,'Strong admission original capacity requires exact .113 shader inverse and unchanged transport proof')
    for key in CURRENT_PROOF_KEYS['strong_admission110']:
        row=inverse if key=='strong_admission_original_budget19110' else executed
        require(row.get(key) is True,'Strong admission .110 proof is not backed by executed subtest: '+key)
    baseline=strong_reports['published108_regression']
    require(type(baseline) is dict and baseline.get('status')=='passed' and type(baseline.get('assertions')) is int
        and baseline['assertions']==1 and baseline.get('reproduced') is True
        and baseline.get('source_sha256')=={'GpuNoise1960.java':'27fc632074e7adaf45e6f55422167ed25b383d3a3695a5f8895a3d212ab429e5',
            'GpuStrong1960.java':'667ae2ef7774ae7e152134e91ee79702cac228ac647f14f84c79ca01b91173df'},
        'Strong admission .110 published-.108 regression reproduction missing')
    require(type(strong.get('assertions')) is int and strong['assertions']==sum(row['assertions'] for row in strong_reports.values()),
        'Strong admission .110 assertion accounting differs')
    diagnostic=tests['strong_diagnostics110']
    require(type(diagnostic.get('assertions')) is int and diagnostic['assertions']>=247
        and type(diagnostic.get('cases')) is int and diagnostic['cases']>=36,'Strong admission .110 diagnostic coverage incomplete')
    require(diagnostic.get('fixture_classes_in_runtime') is False and diagnostic.get('physical_android_tested') is False,
        'Strong admission diagnostic fixture scope differs')
    copy=tests['copy_admission110']
    reports=copy.get('tests')
    require(type(reports) is dict and set(reports)=={'baseline','current','copy_rotation','source_inverse'},'Resident/copy .110 executed subtests missing')
    for name,minimum,cases in (('baseline',18,1),('current',2195,27),('copy_rotation',13,None)):
        row=reports[name]
        require(type(row) is dict and row.get('status')=='passed' and type(row.get('assertions')) is int
            and row['assertions']>=minimum and row.get('physical_android_tested') is False,'Resident/copy .110 subtest coverage incomplete: '+name)
        if cases is not None:
            require(type(row.get('cases')) is int and row['cases']>=cases and row.get('native_transport_mocked') is True,
                'Resident/copy .110 controlled-transport coverage differs: '+name)
    restored=reports['source_inverse']
    require(type(restored) is dict and restored.get('status')=='passed' and type(restored.get('assertions')) is int
        and restored['assertions']==5 and type(restored.get('negative_controls')) is int and restored['negative_controls']==3
        and restored.get('original_sha256')=='8bd4269eb0222f86388c71af034bca0657e8fffc0b563c77a0d1f5832d781ac4'
        and restored.get('resident_source_inverse19110') is True and restored.get('current_source_executed_separately') is True,
        'Resident .110 original two-pass source and inverse controls missing')
    require(type(copy.get('assertions')) is int and copy['assertions']==sum(row['assertions'] for row in reports.values()),
        'Resident/copy .110 assertion accounting differs')

def positive_host111(tests):
    diagnostic=tests['diagnostics111']
    require(type(diagnostic.get('assertions')) is int and diagnostic['assertions']>=2700
        and type(diagnostic.get('cases')) is int and diagnostic['cases']>=57,
        'Resident timing .111 diagnostic coverage incomplete')
    require(diagnostic.get('fixture_classes_in_runtime') is False and diagnostic.get('physical_android_tested') is False,
        'Resident timing .111 diagnostic fixture scope differs')
    for key,count in [('legacy_cpu_tail_zero_words19115',612),('legacy_zero_extension_words19115',252),('legacy_memory_capacity_checks19115',18)]:
        require(type(diagnostic.get(key)) is int and diagnostic[key]==count,'Legacy VJ8 compatibility execution missing: '+key)
    timing=tests['resident_timing111'];reports=timing.get('tests')
    require(type(reports) is dict and set(reports)=={'source_inverse','observer'},
        'Resident timing .111 executed subtests missing')
    restored=reports['source_inverse'];observer=reports['observer']
    original={'GpuResident1976.java':'972971abc70b62c9f9016d1b1c8dae6ef8e7e6dd3505557caada55a0164def1b',
        'QualityPipeline1932.java':'6590b305a35d65808db0e0c163cbed2c71fb321fb262c14817c93512616425bd',
        'GpuChain1961.java':'c99ef40ed8e3b6755a88d6c293f8c39b88808d9812204546a8952cc174126ead',
        'GpuStrong1960.java':'eec980d27305a7167bb907d1d1a73b31d178a80a62c2d965946bfdd935845fb6'}
    require(type(restored) is dict and restored.get('status')=='passed' and type(restored.get('assertions')) is int
        and restored['assertions']==12 and type(restored.get('negative_controls')) is int and restored['negative_controls']==8
        and restored.get('original_source_sha256')==original and restored.get('current_source_executed_separately') is True
        and restored.get('resident_timing_source_inverse19111') is True,'Resident timing .111 exact published-.110 source inverse missing')
    require(type(observer) is dict and observer.get('status')=='passed' and type(observer.get('assertions')) is int
        and observer['assertions']>=96 and type(observer.get('cases')) is int and observer['cases']>=7
        and observer.get('physical_android_tested') is False,'Resident timing .111 actual observer coverage incomplete')
    for key in CURRENT_PROOF_KEYS['resident_timing111']:
        row=restored if key=='resident_timing_source_inverse19111' else observer
        require(row.get(key) is True,'Resident timing .111 proof lacks executed backing: '+key)
    require(type(timing.get('assertions')) is int and timing['assertions']==sum(row['assertions'] for row in reports.values())
        and timing.get('physical_android_tested') is False,'Resident timing .111 assertion accounting or scope differs')
    counts=tests['strong_counts111'];reports=counts.get('tests')
    require(type(reports) is dict and set(reports)=={'counts','absent','incompatible'},
        'Strong counts .111 executed subtests missing')
    observed=reports['counts']
    require(type(observed) is dict and observed.get('status')=='passed' and type(observed.get('assertions')) is int
        and observed['assertions']>=51 and type(observed.get('cases')) is int and observed['cases']>=11,
        'Strong counts .111 dispatcher coverage incomplete')
    for key in ('production_dispatcher_executed','actual_commit_counts','foreground_trace_isolated','cpu_oracle_excluded',
            'optional_sink_failures_preserved','ownership_preserved'):
        require(observed.get(key) is True,'Strong counts .111 actual dispatcher proof missing: '+key)
    require(observed.get('actual_jni') is False and observed.get('physical_android_tested') is False,
        'Strong counts .111 controlled-transport scope differs')
    for name in ('absent','incompatible'):
        row=reports[name]
        require(type(row) is dict and row.get('status')=='passed' and type(row.get('assertions')) is int
            and row['assertions']>=3 and row.get('missing_optional_sink_preserved') is True,
            'Strong counts .111 optional observer failure changed dispatch: '+name)
    require(type(counts.get('assertions')) is int and counts['assertions']==sum(row['assertions'] for row in reports.values())
        and type(counts.get('original_regression_assertions')) is int and counts['original_regression_assertions']>=267
        and counts.get('actual_jni') is False and counts.get('physical_android_tested') is False,
        'Strong counts .111 accounting, retained regressions or scope differs')

def positive_finish_phases112(phases):
    require(type(phases) is dict and phases.get('status')=='passed','Finish phase evidence missing')
    for flag in ('retained_finish_memory1992_gate','strict_standard1999_baseline_identity',
            'current_phase_owned_contract19112','retained_finish_phase111'):
        require(phases.get(flag) is True,'Current/historical finish phase proof missing: '+flag)
    require(type(phases.get('assertions')) is int and phases['assertions']==297
        and phases.get('physical_android_tested') is False and phases.get('device_speedup_verified') is False,
        'Current/historical finish phase coverage differs')
    for name,count,flags in (('queue',29,('actual_qualification_queue',)),
            ('finish',114,('actual_finish_graph','full_pixel_comparator','native_transport_mocked')),
            ('route',40,('actual_route_control','native_transport_mocked')),
            ('historical_finish111',114,('actual_finish_graph','full_pixel_comparator','native_transport_mocked'))):
        row=phases.get(name)
        require(type(row) is dict and row.get('status')=='passed' and type(row.get('assertions')) is int
            and row['assertions']==count and row.get('physical_android_tested') is False,
            'Executed finish phase child incomplete: '+name)
        for flag in flags:require(row.get(flag) is True,'Executed finish phase proof missing: '+name+'/'+flag)
    require(phases['assertions']==sum(phases[name]['assertions'] for name in ('queue','finish','route','historical_finish111')),
        'Current/historical finish phase assertion accounting differs')
    require(phases.get('restored_chain111_sha256')=='8da03b9ae3d770133f22e75defd555d8696e31ceac53955bfb86fa54063c4847',
        'Historical finish phase did not restore the exact published .111 graph')
    historical=phases.get('historical_source_sha256')
    require(type(historical) is dict,'Original finish phase fixture identities missing')
    for name,digest in {
            'tests1992/finish_memory/FinishRoute1992Test.java':'64c598d2f79cdeb07f602f926062c8deb5398de1b5359a23b01f1fed5909b982',
            'tests1992/finish_memory/FinishMemory1992Test.java':'471b7e4ae164abf9c702dfe1e4b460dcc7e0f881e053a4698e36dc1675d16f6b',
            'host_finish_memory1992.py':'a63e177685a3a39d603f4003466df80cf4677ddb7e3f06a61549df8f9a6cd7c5'}.items():
        require(historical.get(name)==digest,'Original finish phase fixture changed: '+name)

def positive_host112(tests):
    positive_finish_phases112(tests['finish_memory92'])
    memory=tests['memory97'];children=memory.get('tests')
    require(type(children) is list and len(children)==5,'Nested foreground memory proof inventory differs')
    for row,count in zip(children,(832,100,10,297,29)):
        require(type(row) is dict and row.get('status')=='passed' and type(row.get('assertions')) is int
            and row['assertions']==count,'Nested foreground memory execution coverage differs')
    positive_finish_phases112(children[3])
    require(type(memory.get('assertions')) is int and memory['assertions']==1268
        and memory['assertions']==sum(row['assertions'] for row in children)
        and memory.get('physical_android_tested') is False and memory.get('device_speedup_verified') is False,
        'Nested foreground memory assertion accounting or scope differs')
    finish=tests['resident_finish19112']
    for key,value in (('assertions',492),('cases',17),('compared_pixels',1384202)):
        require(type(finish.get(key)) is int and finish[key]==value,'Resident finish .112 actual JNI coverage incomplete: '+key)
    require(finish.get('physical_android_tested') is False,'Resident finish .112 host scope differs')
    inverse=finish.get('source_inverse')
    require(type(inverse) is dict and inverse.get('status')=='passed' and type(inverse.get('assertions')) is int
        and inverse['assertions']==28 and type(inverse.get('negative_controls')) is int and inverse['negative_controls']==26
        and inverse.get('original_source_sha256')=={'GpuChain1961.java':'8da03b9ae3d770133f22e75defd555d8696e31ceac53955bfb86fa54063c4847',
            'GpuResident1976.java':'c7d367ed3cf20fae706807d8d1801405213b2c6d41da2d655e1a700f83fe5f9e'}
        and inverse.get('resident_finish_source_inverse19112') is True and inverse.get('current_source_executed_separately') is True,
        'Resident finish .112 exact published source inverse or all-hunk negatives missing')
    profile=finish.get('profile')
    require(type(profile) is dict and profile.get('status')=='passed','Resident finish .112 foreground profile execution missing')
    for key,value in (('assertions',114),('cases',6),('current_cases',5),('faulty_cases',1)):
        require(type(profile.get(key)) is int and profile[key]==value,'Resident finish .112 foreground profile coverage differs: '+key)
    for key in ('foreground_profile_toctou_rejected','faulty_guard_removal_reproduces_128','stable_profiles_preserved',
            'preflight_closed_on_all_paths','actual_foreground_producer_executed','controlled_peer_fixture'):
        require(profile.get(key) is True,'Resident finish .112 foreground profile proof missing: '+key)
    for key in ('native_tested','physical_android_tested','device_speedup_verified'):
        require(profile.get(key) is False,'Resident finish .112 profile peer scope differs: '+key)
    for name,assertions,cases,faulty in (('current',45,5,False),('faulty',9,1,True),('shape_current19115',44,4,False),('shape_faulty19115',11,1,True)):
        row=profile.get(name)
        require(type(row) is dict and row.get('status')=='passed' and type(row.get('assertions')) is int and row['assertions']==assertions
            and type(row.get('cases')) is int and row['cases']==cases and row.get('faulty_guard_removed') is faulty,
            'Resident finish .112 foreground profile regression not executed: '+name)
    for key in ('historical_producer_pin_preserved19115','current_foreground_copy_sequence_preserved19115','existing_two_cpu_reference_trials_preserved19115','current_shape_profile_toctou_rejected19115','faulty_shape_guard_removal_reproduced19115'):
        require(profile.get(key) is True,'Current producer preservation proof missing: '+key)
    require(type(profile.get('producer_structure_negative_controls19115')) is int and profile['producer_structure_negative_controls19115']==5,'Current producer structural negatives missing')
    require(profile.get('historical_producer_sha25619115')=='c3aaef4eb23a532bc0af2377266a4951fe39779533fce1d0d9cfb615f2618635','Historical producer pin changed')
    require(profile['assertions']==profile['current']['assertions']+profile['faulty']['assertions']+profile['shape_current19115']['assertions']+profile['shape_faulty19115']['assertions']+profile['producer_structure_negative_controls19115']
        and finish['assertions']==350+inverse['assertions']+profile['assertions'],
        'Resident finish .112 assertion accounting differs')
    plan=tests['resident_plan19112']
    require(type(plan.get('assertions')) is int and plan['assertions']>=9608
        and type(plan.get('planner_cases')) is int and plan['planner_cases']==4000
        and plan.get('published111_sha256')=='a10406bc6f19255decf3426aafb46254229d2d73e309502a44348898a7744368',
        'Resident plan .112 actual planner/published-.111 coverage incomplete')
    inverse=plan.get('source_inverse')
    require(type(inverse) is dict and inverse.get('status')=='passed' and type(inverse.get('assertions')) is int
        and inverse['assertions']==6 and type(inverse.get('negative_controls')) is int and inverse['negative_controls']==5
        and inverse.get('original_source_sha256')=={'QualityPipeline1932.java':'a10406bc6f19255decf3426aafb46254229d2d73e309502a44348898a7744368'}
        and inverse.get('resident_plan_source_inverse19112') is True and inverse.get('current_source_executed_separately') is True,
        'Resident plan .112 exact source inverse or all-hunk negatives missing')
    require(plan.get('physical_android_tested') is False,'Resident planner host scope differs')
    cancel=tests['strong_cancellation19112']
    for key,value in (('fixture_cases',70),('cancellation_cases',18),('cancelled_qualifiers',4),('inverse_mutation_negatives',6)):
        require(type(cancel.get(key)) is int and cancel[key]==value,'Strong cancellation .112 coverage differs: '+key)
    corpus='9b4e7a866ea1651481739065b78f549ec10e32c0e435bf26f8aae3aae67521ea'
    comparison='ff6f9ea90013c79a817b585a0607d7e054cbc877c0680331c310074c970e3915'
    for name,minimum,cancelled in (('current',31,18),('baseline',11,0)):
        row=cancel.get(name)
        require(type(row) is dict and row.get('status')=='passed' and type(row.get('assertions')) is int and row['assertions']>=minimum
            and type(row.get('fixtures')) is int and row['fixtures']==70 and type(row.get('cancelled_cases')) is int and row['cancelled_cases']==cancelled
            and row.get('corpus_sha256')==corpus and row.get('comparison_sha256')==comparison,
            'Strong cancellation .112 exact published/current corpus differs: '+name)
    controlled=cancel.get('controlled')
    require(type(controlled) is dict and controlled.get('status')=='passed' and type(controlled.get('assertions')) is int and controlled['assertions']>=29,
        'Strong cancellation .112 qualifier execution missing')
    for key,value in (('cancelled_qualifiers',4),('partial_proofs_written',0),('completed_oracle_calls',70),('completed_gpu_submits',80)):
        require(type(controlled.get(key)) is int and controlled[key]==value,'Strong cancellation .112 completed/partial proof accounting differs: '+key)
    observed=cancel.get('observed_interrupt')
    require(type(observed) is dict and set(observed)=={'published111','current112'},'Strong cancellation .112 published/current interrupt control missing')
    for name,cancelled,completed in (('published111',False,True),('current112',True,False)):
        row=observed[name]
        require(type(row) is dict and row.get('status')=='passed' and type(row.get('assertions')) is int and row['assertions']>=3
            and row.get('cancelled') is cancelled and row.get('completed_after_interrupt') is completed,
            'Strong cancellation .112 interruption regression not executed: '+name)
    total=cancel['current']['assertions']+cancel['baseline']['assertions']+controlled['assertions']+sum(row['assertions'] for row in observed.values())+cancel['inverse_mutation_negatives']+1
    require(type(cancel.get('assertions')) is int and cancel['assertions']==total and total>=84
        and cancel.get('physical_android_tested') is False and cancel.get('actual_jni',False) is False,
        'Strong cancellation .112 assertion accounting or controlled-transport scope differs')

def positive_host114(tests):
    strong=tests['strong_opt114']
    require(strong.get('preprocessed_contract_scope')=='restored114_source' and strong.get('structural_negative_control_scope')=='restored114_source','Inherited .114 contract must use the strict historical layer and retain current execution')
    for key in ('whole_output_differences','confidence_differences','trace_intermediate_differences','shared_memory_bytes_added'):
        require(type(strong.get(key)) is int and strong[key]==0,'Strong .114 exact output/space differs: '+key)
    for key,minimum in (('assertions',4299),('negative_controls',4),('fixtures',18),('compared_pixels',6900000),('compared_confidence_or_trace_words',560000)):
        require(type(strong.get(key)) is int and strong[key]>=minimum,'Strong .114 coverage absent: '+key)
    require(strong.get('optimized_profiles')==[4,6] and strong.get('unchanged_tiled_profile')==7 and strong.get('unchanged_preprocessed_programs')==114,'Strong .114 active/unchanged profile scope differs')
    require(strong.get('per_opaque_active_pixel_repeated_divisions_removed_maximum')==28,'Strong .114 operation reduction differs')
    require(tests['gpu_math'].get('historical_contract_scope')=='restored114_to113_to112' and tests['gpu_math'].get('current_shader_executed19114') is True,'Inherited GPU math scope must separate historical contract/current execution')
    old=tests['strong_opt113']
    require(old.get('structural_negative_control_scope')=='restored113_source','Inherited .113 mutations must reach the historical structure layer')
    require(old.get('preprocessed_contract_scope')=='restored113_against112' and old.get('current_shader_executed19114') is True,'Historical .113 preprocessing scope must be separate from current execution')
    front=tests['front_opt114']
    require(front.get('schema')=='ulike19114-front-native-proof-v1' and front.get('baseline_version')=='1.9.113','Front .114 proof identity differs')
    architectures=front.get('architectures')
    require(type(architectures) is dict and set(architectures)=={'host','arm64'},'Front .114 both architectures required')
    for name,row in architectures.items():
        require(row.get('exact_output') is True and row.get('raw_bits_exact') is True,'Front .114 exact output missing: '+name)
        for section,key,count in [('pixels','pixel_cases',320),('safeguards','safeguard_cases',32768),('pairs','pair_cases',512),('coefficients','coefficient_cases',8192)]:
            value=row.get('cases',{}).get(section,{}).get(key)
            require(type(value) is int and value==count,'Front .114 coverage missing: '+name+'/'+key)
        require(set(row.get('benchmarks',{}))=={'uncached','cached'},'Front .114 same-process benchmark missing')
    require(front.get('target_device_gain_claimed') is False,'Front .114 host timing overclaims device gain')
    alias=front.get('alias_contract',{})
    require(alias.get('arithmetic_source_scope')=='restored114_source' and alias.get('historical_negative_scope19115')=='restored114_source' and alias.get('current_production_callsites_audited19115') is True and alias.get('current_front_source_inverse_verified19115') is True,'Front alias contract must restore the historical .114 layer')
    require(type(alias.get('production_call_sites')) is int and alias['production_call_sites']==4 and alias.get('only_disjoint_workspace_arrays') is True and alias.get('arithmetic_source_unchanged') is True and type(alias.get('negative_controls')) is int and alias['negative_controls']>=2,'Front .114 disjoint workspace contract not proven')
    for row in architectures.values():
        require(row.get('negative_controls')==['lag-numerator','lag-denominator'],'Front .114 numerical mutation controls absent')
    finish=tests['finish_opt114']
    for key,minimum in [('compared_pixels',60000000),('constant_pixels',2500000),('gpu_policy_words',10000000)]:
        require(type(finish.get(key)) is int and finish[key]>=minimum,'Finish .114 executed coverage missing: '+key)
    for key in ('extra_image_buffers','extra_mask_arrays'):
        require(type(finish.get(key)) is int and finish[key]==0,'Finish .114 extra memory differs: '+key)
    require(finish.get('published113_reference_sha256')=='5905cc2f0b3cb2327309788bb5476c40477c580703fc0944b4353c3a7de9f397','Finish .114 independent reference changed')

def positive_host(report):
    require(isinstance(report,dict) and report.get('status')=='passed','Current host suite did not pass')
    require(type(report.get('assertions')) is int and report['assertions']>0,'No executed host assertions')
    require(report.get('physical_android_tested') is False,'Host report claims physical Android')
    require(report.get('device_speedup_verified',False) is False,'Host report claims device speed')
    tests=report.get('tests');require(isinstance(tests,dict) and bool(tests),'Host groups absent')
    require(set(tests)==REQUIRED_HOST_GROUPS,'Required executed host group inventory differs')
    total=0
    for name,child in tests.items():
        require(isinstance(child,dict) and child.get('status')=='passed' and type(child.get('assertions')) is int
            and child['assertions']>0,'Incomplete host group: '+name)
        require(child.get('physical_android_tested',False) is False,'Host group claims Android execution: '+name)
        total+=child['assertions']
    require(total==report['assertions'],'Host assertion total differs from executed groups')
    for group,keys in CURRENT_PROOF_KEYS.items():
        require(tests[group].get('device_speedup_verified') is False,'Current host group claims device speed: '+group)
        for key in keys:require(tests[group].get(key) is True,'Current proof missing: '+group+'/'+key)
    fair=tests['resident_fair_plan116']
    require(fair.get('schema')=='ulike19116-resident-fair-plan-v1' and type(fair.get('assertions')) is int
        and fair['assertions']==11 and fair.get('physical_android_tested') is False,
        'Fair Strong-plan host execution differs')
    inverse=tests['source_inverse117']
    require(inverse.get('schema')=='ulike19117-source-inverse-proof-v1' and inverse.get('status')=='passed'
        and inverse.get('reviewed_files')==list(source_inverse19117.REVIEWED)
        and type(inverse.get('mutations_rejected')) is int and inverse['mutations_rejected']==len(source_inverse19117.REVIEWED)+1
        and inverse.get('manifest_sha256')==source_inverse19117.MANIFEST_SHA256
        and inverse.get('published_archive_sha256')==source_inverse19117.ARCHIVE_SHA256,
        'Reviewed .117 source inverse or published .116 archive identity differs')
    regional_distribution19115.verify(tests['runtime_version'].get('distributed_regional_dex'))
    positive_host110(tests)
    positive_host111(tests)
    positive_host112(tests)
    positive_host114(tests)
    host_evidence19115.positive(tests,require)
    require(tests['gpu_math'].get('local_luma_ratio_preparation_only') is False,'Full Strong reuse mislabeled as preparation-only')
    strong113=tests['strong_opt113']
    for key in ('whole_output_differences','confidence_differences','trace_intermediate_differences'):
        require(type(strong113.get(key)) is int and strong113[key]==0,'Strong .113 numerical differences: '+key)
    require(type(strong113.get('negative_controls')) is int and strong113['negative_controls']>=3,'Strong .113 executed mutation controls absent')
    require(strong113.get('per_pixel_repeated_divisions_removed')==25,'Strong .113 adopted work reduction differs')
    require(strong113.get('optimized_profile')==6 and strong113.get('unchanged_standard_profile')==4 and strong113.get('unchanged_tiled_profile')==7,'Strong .113 optional-only profile scope differs')
    require(strong113.get('unchanged_preprocessed_programs')==117,'Strong .113 unchanged variant proof missing')
    require(strong113.get('adaptive_gpu_speed_gate_percent')==10 and tests['strong_adaptive94'].get('profile6_preserves_10pct_gate') is True,'Strong .113 optional candidate must retain executed ten-percent gate')
    front113=tests['front_opt113']
    require(front113.get('schema')=='ulike19113-front-native-proof-v1' and front113.get('baseline_version')=='1.9.112','Front .113 proof identity differs')
    architectures=front113.get('architectures')
    require(type(architectures) is dict and set(architectures)=={'host','arm64'},'Front .113 both actual architectures required')
    for name,row in architectures.items():
        require(row.get('exact_output') is True and row.get('raw_bits_exact') is True,'Front .113 exact output absent: '+name)
        cases=row.get('cases',{})
        for section,key,count in [('pixels','pixel_cases',320),('safeguards','safeguard_cases',32768),('pairs','pair_cases',512)]:
            value=cases.get(section,{}).get(key)
            require(type(value) is int and value==count,'Front .113 exact coverage differs: '+name+'/'+key)
        require(row.get('negative_controls')==['lag-numerator','lag-denominator'],'Front .113 numerical mutation controls missing')
        require(set(row.get('benchmarks',{}))=={'uncached','cached'},'Front .113 paired same-process benchmarks missing')
    require(front113.get('target_device_gain_claimed') is False,'Native host/emulator result overclaims device gain')
    require(type(tests['memory108'].get('assertions')) is int and tests['memory108']['assertions']>=1000,'Resident memory .108 executed coverage incomplete')
    require(tests['memory108'].get('shader_source_inverse19113_sha256')=='4f1bb151e3e49636d71db2abb6dad1b89d1a8572317af525069c306515605e36' and type(tests['memory108'].get('shader_source_inverse19113_negative_controls')) is int and tests['memory108']['shader_source_inverse19113_negative_controls']>=3,'Memory historical shader inverse or mutation controls absent')
    require(type(tests['resident_memory108'].get('assertions')) is int and tests['resident_memory108']['assertions']>=400 and type(tests['resident_memory108'].get('cases')) is int and tests['resident_memory108']['cases']>=26,'Resident memory .108 diagnostic coverage incomplete')
    require(tests['gpu_math'].get('all_60_legacy_programs_unchanged') is False,'Current regional shader change mislabeled as unchanged')
    positive_host105(tests)
    positive_host106_trace(tests)
    cache=tests['cache107']
    require(cache.get('physical_android_tested') is False,'Cache host proof claims physical result')
    for key in ('native_fingerprint_old','native_fingerprint_new'):
        require(type(cache.get(key)) is str and bool(re.fullmatch('[a-f0-9]{64}',cache[key])),'Cache fingerprint identity missing: '+key)
    require(cache['native_fingerprint_old']!=cache['native_fingerprint_new'],'Changed shader retained old certificate identity')
    for name,expected in (('cache',{'assertions':143,'proof_families':8,'corpus_families':4}),('ingress',{'assertions':168,'generated_models':84,'invalid_binary32_classes':9})):
        row=cache.get(name,{})
        require(row.get('status')=='passed' and all(type(row.get(k)) is int and row[k]==v for k,v in expected.items()),'Cache/ingress executed coverage differs: '+name)
    require(type(cache.get('assertions')) is int and cache['assertions']==446,'Cache assertion accounting differs')
    sites={'GpuStrong1960.java':9,'GpuAnalysis1961.java':2,'GpuStrongTuning1975.java':2,'GpuStrongRouting1978.java':1,'GpuStrongLayout1976.java':1}
    observed=cache.get('ingress_callsites',{})
    require(type(observed) is dict and set(observed)==set(sites) and all(type(observed[k]) is int and observed[k]==v for k,v in sites.items()),'Regional ingress callsite inventory differs')
    for key,count in [('legacy_ingress_sites19115',8),('regional_candidate_ingress_sites19115',1),('legacy_ingress_negative_controls19115',8),('regional_candidate_ingress_negative_controls19115',5)]:
        require(type(cache.get(key)) is int and cache[key]==count,'Legacy/new ingress structural proof missing: '+key)
    candidate=cache.get('candidate_ingress19115',{})
    require(candidate.get('status')=='passed','Current research-wrapper ingress proof missing')
    for key,count in [('assertions',112),('invalid_binary32_classes',9),('readiness_required_combinations',4),('invalid_cases',36)]:
        require(type(candidate.get(key)) is int and candidate[key]==count,'Research-wrapper ingress coverage differs: '+key)
    require(cache.get('regional_candidate_ingress_scope19115')=='research_wrapper_cached_readiness_before_session_open_upload','Research-wrapper ingress scope differs')
    legacy_hashes=cache.get('legacy_ingress_method_sha25619115',{})
    require(type(legacy_hashes) is dict and set(legacy_hashes)=={'regionalEvidence19107','initialize','initialize1971','initializeResident19110','ephemeral','ephemeral1973','trace19106','ephemeral1971'} and all(type(x) is str and re.fullmatch('[a-f0-9]{64}',x) for x in legacy_hashes.values()),'Eight unchanged legacy ingress owner proofs absent')
    positive_host107_regional(tests)
    for key,minimum in (('cases',64),('stage_assertions',156),('retained_assertions',71)):
        require(type(tests['validation104'].get(key)) is int and tests['validation104'][key]>=minimum,'Stage-isolation diagnostic coverage incomplete: '+key)
    for key,expected in (('cases',24),('compared_argb_words',1123168),('tail_guard_words',531364),('geometry_mode',0),('max_rows',32),('output_slot',23),('scratch_slot',22),('scratch_bytes',4)):
        require(type(tests['raw_crop104'].get(key)) is int and tests['raw_crop104'][key]==expected,'Actual GLES raw crop corpus differs: '+key)
    slots=tests['raw_crop104'].get('source_slots')
    require(type(slots) is list and all(type(slot) is int for slot in slots) and slots==[0,1,2,24],'Actual GLES raw crop slot coverage differs')
    require(tests['burst103'].get('production_single_image_policy_tested') is False,'Dormant legacy fixture misreported as production single capture')
    for key,minimum in (('retained_assertions',899),('new_assertions',9)):
        require(type(tests['capture_phases104'].get(key)) is int and tests['capture_phases104'][key]>=minimum,'Capture phase coverage incomplete: '+key)
    admission=tests['single_admission104'].get('cases',{})
    for version,minimum,baseline,production in (('103',9,True,False),('104',12,False,True)):
        row=admission.get(version,{})
        require(row.get('status')=='passed' and type(row.get('assertions')) is int and row['assertions']>=minimum,'Single admission reproducer missing: '+version)
        require(row.get('baseline_reproduced') is baseline and row.get('production_single_image_policy_tested') is production,'Single admission scope differs: '+version)
    face=tests['face_reuse104'].get('tests',{})
    for name,expected_calls in (('baseline103',0),('current104',2)):
        row=face.get(name,{})
        require(type(row.get('cases')) is int and row['cases']>=9,'Face reuse case coverage incomplete: '+name)
        require(type(row.get('avoided_detector_calls')) is int and row['avoided_detector_calls']==expected_calls,'Face detector invocation proof differs: '+name)
        require(isinstance(row.get('mask_sha256'),str) and re.fullmatch('[a-f0-9]{64}',row['mask_sha256']) is not None,'Face output digest missing: '+name)
    require(face['baseline103']['mask_sha256']==face['current104']['mask_sha256'],'Face mask output changed')
    source_mask=tests['source_mask103']
    for key,minimum in (('compared_pixels',50000000),('cache_cases',47),('fallback_cases',43)):
        require(type(source_mask.get(key)) is int and source_mask[key]>=minimum,'New exact mask reuse coverage incomplete: '+key)
    reads=source_mask.get('cached_mask_byte_reads_upper_bound');baseline_reads=source_mask.get('baseline_mask_byte_reads')
    require(type(reads) is int and type(baseline_reads) is int and reads>0 and baseline_reads>0 and reads*2<baseline_reads,'New exact mask read reduction proof incomplete')
    for key,minimum in (('retained_assertions',802),('retained_scenarios',51)):
        require(type(tests['burst103'].get(key)) is int and tests['burst103'][key]>=minimum,'Retained capture lifecycle proof incomplete: '+key)
    require(tests['front_plan97'].get('retained97_planning_cases_on_optimized_kernel') is True,'Retained front planning gate was not run against current kernel')
    for key in ('retained_pipeline_gate1992', 'strict_narrow_baseline_metadata1999', 'readiness_peer_pixels_forbidden1999'):
        require(tests['pipeline_gate'].get(key) is True,'Current pipeline readiness fixture proof missing: '+key)
    for key in ('retained_finish_memory1992_gate', 'strict_standard1999_baseline_identity'):
        require(tests['finish_memory92'].get(key) is True,'Current finish route fixture proof missing: '+key)
    for group,keys in (('timing', ('capture_bitmap_provenance_verified', 'ambiguous_metadata_trace_preserved', 'exact_input_lookup_verified')), ('capture99', ('completed_bitmap_trace_isolated', 'save_input_pixels_unchanged', 'save_bitmap_dimensions_observed'))):
        require(tests[group].get('device_speedup_verified') is False,'Current capture host group claims device speed: '+group)
        for key in keys:require(tests[group].get(key) is True,'Current .99 capture proof missing: '+group+'/'+key)
    for group,keys in (('front99', ('front1999_expired_admission_rejected', 'front1999_requalified_gpu_admitted', 'front1999_stage_reserved_before_copy', 'front1999_cached_support_reason', 'retained_frontmodel1991_cases')), ('chain99', ('resident_baseline_schedule_matches', 'resident_baseline_key_binds_wide_state', 'resident_progress_cancellation_frozen'))):
        require(tests[group].get('device_speedup_verified') is False,'New host group claims device speed: '+group)
        for key in keys:require(tests[group].get(key) is True,'Current .99 regression proof missing: '+group+'/'+key)
    require(tests['finish_recovery95'].get('resident_policy_trace_verified') is True,'Resident GPU finish policy trace missing')
    for key in ('wide_restore_status_verified','no_extra_gpu_dispatch_verified'):
        require(tests['finish_recovery95'].get(key) is True,'Wide restore display proof missing: '+key)
    for key in ('recent_history_bounded16_verified','recent_history_success_only_verified','recent_history_exact_groups_verified','recent_history_lock_order_verified'):
        require(tests['timing'].get(key) is True,'Recent capture diagnostic proof missing: '+key)
    require(type(tests['release_gate'].get('publication_metadata115_assertions')) is int and tests['release_gate']['publication_metadata115_assertions']>=19,'Generated .115 metadata/three native rows/current smooth identity proof missing')
    require(tests['release_gate'].get('native_optimization115_provenance_verified') is True and type(tests['release_gate'].get('native_optimization115_negative_assertions')) is int and tests['release_gate']['native_optimization115_negative_assertions']>0,'Current exact baseline/current native rebuild and corruption controls missing')
    require(tests['release_gate'].get('large_content_exact_commit_verified') is True,'Exact commit large content retrieval proof missing')
    strong=tests['strong_adaptive94']
    for key in ('profile6_bit_exact','profile6_independent_certificates','profile6_preserves_10pct_gate','one_heavy_candidate_per_capture','existing_profile7_preserved'):
        require(strong.get(key) is True,'Current Strong profile6 proof missing: '+key)
    require(strong.get('profile6_program')==45 and strong.get('profile6_cases')==70 and type(strong.get('profile6_compared_pixels')) is int and strong['profile6_compared_pixels']>1800000,'Current Strong profile6 actual-GLES coverage incomplete')
    gpu=tests['gpu_math']
    require(gpu.get('shader96') is True and gpu.get('only_guarded_partial_predicates_changed') is False,
        'Reviewed Strong shader proof missing')
    require(gpu.get('published95_records')==1016 and gpu.get('published95_reads')==18240 and
        gpu.get('published95_argb_differences')==0 and gpu.get('published95_confidence_differences')==0 and
        gpu.get('legacy_preprocessor_programs')==60,'Frozen .95 shader replay or legacy program identity missing')
    require(gpu.get('focused_gles',{}).get('actual_gles_execution') is True and
        gpu['focused_gles'].get('cutoff_proof',{}).get('unsafe_rejections')==0 and
        gpu['focused_gles']['cutoff_proof'].get('final_contribution_differences')==0,'Actual GLES cutoff guard proof missing')
    for key in ('foreground_cpu_copy_disabled','existing_certificates_and_original_fallback_verified',
            'explicit_owned_verifiers_exact_speed_cancel_memory_verified','reused_result_alias_regression_verified',
            'actual_colour_jni_outputs_compared','gpu_queue_independent'):
        require(tests['cpu_copy96'].get(key) is True,'CPU snapshot/certificate proof missing: '+key)

    for key in ('whole_model_bitwise_equal', 'actual_source_regions_overlapped', 'resident_runtime_upload_bitwise_equal', 'model_ticket_joined_before_handoff', 'callback_once_and_fallback_verified', 'existing_region_gpu_route_preserved', 'invalid_completed_runtime_rejected'):
        require(tests['model_parallel96'].get(key) is True,'Model overlap/fallback proof missing: '+key)
    require(tests['front_timing96'].get('controlled_native_transport') is True and tests['front_timing96'].get('actual_jvm_loading') is True,'Front native/fallback diagnostic proof missing')
    for key in ('foreground_same_budget_retry_verified','active_gpu_ownership_preserved','busy_native_deferred_release_verified','pressure_skips_optional_finish_copy','ordinary_photo_qualification_preserved','same_finish_pixels_verified','cancelled_speed_workers_classified','actual_worker_join_and_owner_release'):
        require(tests['memory97'].get(key) is True,'Current foreground recovery/ownership proof missing: '+key)
    require(tests['timing'].get('cases',{}).get('MemoryTiming1997Test',{}).get('status')=='passed','Current photo memory and copy diagnostic proof missing')
    for key in ('no_worker_sacrifice','simultaneous_budget_verified','largest_fitting_rows_verified','bounded_halo_growth','immutable_96_pixels_equal','multiple_workers_verified','optional_half_budget_drop_verified'):
        require(tests['front_plan97'].get(key) is True,'Current front planner budget/pixel proof missing: '+key)
    mask=tests['mask_rows98']
    for key in ('full_policy_equal','full_output_equal','raw_double_intermediates_equal','optional_budget_fallback_verified','worker_failure_join_verified','rotation_fallback_verified','actual_jni','host_timing_is_diagnostic_only'):
        require(mask.get(key) is True,'Current mask row reuse proof missing: '+key)
    require(mask.get('device_speedup_verified') is False,'Host mask proof claims physical speed')
    rows=mask.get('rows',{})
    for key,minimum in (('compared_pixels',24000000),('cache_cases',32),('rotated_fallback_cases',42)):
        require(type(rows.get(key)) is int and rows[key]>=minimum,'Current mask row coverage incomplete: '+key)
    reads=rows.get('cached_mask_byte_reads_upper_bound');baseline_reads=rows.get('baseline_mask_byte_reads')
    require(type(reads) is int and type(baseline_reads) is int and reads>0 and baseline_reads>0 and reads*2<baseline_reads,'Mask read reduction proof incomplete')
    parallel=mask.get('parallel',{})
    require(type(parallel.get('parallel_compared_pixels')) is int and parallel['parallel_compared_pixels']>=1300000,'Real JNI parallel mask coverage incomplete')
    arm=tests['front_plan97'].get('arm97',{})
    require(arm.get('status')=='passed' and arm.get('actual_arm_neon_executed') is True and arm.get('all_16_row_boundaries_bitwise_equal') is True,'Actual AArch64 NEON boundary execution missing')
    require(arm.get('pixel_cases')==128 and type(arm.get('compared_pixels')) is int and arm['compared_pixels']>0 and type(arm.get('assertions')) is int and arm['assertions']>0,'AArch64 NEON boundary coverage incomplete')
    require(isinstance(arm.get('compiler_version'),str) and bool(arm['compiler_version']) and isinstance(arm.get('qemu_version'),str) and bool(arm['qemu_version']),'AArch64 host tool identity missing')


def validate(args):
    root=args.root.resolve();dist=root/'dist';source=root/'src'
    require(source.resolve()==ROOT.resolve(),'Validator must run from this release source graph')
    require(runtime_source_version()==VERSION,'Current timing source version differs')
    declaration=json.loads((root/'manifest.json').read_text())
    build=json.loads((dist/'build19101.json').read_text())
    qa=json.loads((dist/QA_NAME).read_text());host=json.loads(args.host_report.read_text())
    require(declaration.get('schema')=='ulike19101-build-declaration-v1','Wrong build declaration')
    require(build.get('schema')=='ulike19101-build-evidence-v1','Wrong build evidence')
    require(qa.get('schema')=='ulike19101-pipeline-release-qa-v1' and
        qa.get('status')=='HOST_AND_ORIGINAL_APKS_VERIFIED_DEVICE_UNVERIFIED','Wrong final QA identity')
    for data in (declaration,build,qa):
        require(data.get('ulike_version')==VERSION and data.get('bundle_version')==BUNDLE_VERSION,'Release version drift')
        require(data.get('baseline')==BASE_PINS,'Published baseline pins changed')
    require(declaration.get('tool_sha256')==TOOL_PINS==build.get('tool_sha256')==qa.get('tool_sha256'),'Pinned build toolchain differs')
    require(qa.get('selected_candidates')==SELECTED,'Adopted candidate inventory changed')
    require(qa.get('regional_positive_proofs_scope')==host_evidence19115.RESEARCH_SCOPE,'Research proof scope differs')
    require(qa.get('host_tests_passed') is True and qa.get('original_apk_apply_tested') is True,'Final evidence gate incomplete')
    for key in FALSE_QA:require(qa.get(key) is False,'Unverified physical capability claimed: '+key)
    require(declaration.get('runtime_version')==build.get('runtime_version')==qa.get('runtime_version')==VERSION and declaration.get('timing_version_matches_release') is True and build.get('timing_version_matches_release') is True and qa.get('timing_version_matches_release') is True,'Timing runtime/package versions differ')
    for key in ('single_capture_timing_only_false_verified', 'legacy_capture_admission_unreachable', 'fixed_strong_diagnostic_guard_verified', 'strong_trace_guard_verified', 'strict_photo_admission_preserved', 'inherited_anonymous_class_identity_preserved19117'):
        require(build.get('structural_inventory',{}).get(key) is True,'Emitted DEX single-shot invariant missing: '+key)
    require(build.get('fixed_strong_diagnostic_dex_verified') is True and type(build.get('strong_diagnostic_negative_controls')) is int and build['strong_diagnostic_negative_controls']==23,'Built Strong diagnostic DEX negative controls missing')
    require(type(build.get('cpu_timing_dex_negative_controls')) is int and build['cpu_timing_dex_negative_controls']==11 and type(qa.get('cpu_timing_dex_negative_controls')) is int and qa['cpu_timing_dex_negative_controls']==11,'CPU observation DEX controls missing')
    require(type(build.get('fair_plan_dex_negative_controls')) is int and build['fair_plan_dex_negative_controls']==6 and type(qa.get('fair_plan_dex_negative_controls')) is int and qa['fair_plan_dex_negative_controls']==6,'Fair Strong-plan DEX controls missing')
    require(build.get('strong_trace_dex_verified') is True,'Built Strong trace DEX guards missing')
    for key, expected in (('strong_trace_eligibility_cases', 958), ('strong_trace_negative_controls', 7)):
        require(type(build.get(key)) is int and build[key]==expected,'Built Strong trace DEX proof missing: '+key)
    require(build.get('structural_inventory',{}).get('runtime_version')==VERSION and build['structural_inventory'].get('timing_version_matches_release') is True,'Emitted DEX timing version missing')
    require(build.get('device_tested') is False and build.get('device_speed_measured') is False
        and build.get('device_quality_verified') is False,'Build claims physical verification')
    regional_distribution19115.verify(build.get('distributed_regional_dex'));regional_distribution19115.verify(qa.get('distributed_regional_dex'))
    require(build.get('experimental_gpu_helper_packaged') is False and qa.get('experimental_gpu_helper_packaged') is False and build.get('experimental_gpu_auto_adoption') is False and qa.get('experimental_gpu_auto_adoption') is False,'Research GPU helper must not be shipped or auto-adopted')
    pins=source_pins();pubpins=publication_pins();pin_files(source,pins);pin_files(root/'publication',pubpins)
    require(declaration.get('reviewed_source_sha256')==pins==build.get('source_sha256')==qa.get('source_sha256'),
        'Reviewed/built/current source graph differs')
    require(declaration.get('reviewed_publication_sha256')==pubpins==build.get('publication_sha256')==qa.get('publication_sha256'),
        'Reviewed/built/current publisher graph differs')
    roots=declaration.get('production_helper_roots')
    require(isinstance(roots,list) and roots and 'GpuRegionalDefault19115' not in roots and roots==sorted(set(roots)),'Production root inventory invalid')
    compiled={production_path(name).relative_to(source).as_posix():sha(production_path(name).read_bytes()) for name in roots}
    require(build.get('production_helper_roots')==roots and qa.get('production_helper_roots')==roots and
        build.get('compiled_production_sha256')==compiled==qa.get('compiled_production_sha256'),'Compiled root source hashes differ')
    for key in ('runtime_helper_references_verified','deterministic_dex_verified','other_apps_preserved'):
        require(build.get(key) is True and qa.get(key) is True,'Build contract absent: '+key)
    require(host.get('schema')=='ulike19101-host-audit-v1' and host.get('source_stability_verified') is True,
        'Current stable host source graph was not executed')
    require(host.get('current_source_sha256')==pins and host.get('current_publication_sha256')==pubpins,
        'Host tests ran against different source/publication bytes')
    positive_host(host)
    require(host['tests']['cache107']['native_fingerprint_new']==native_provenance19115.current_fingerprint(source)
        and host['tests']['cache107']['native_fingerprint_old']==native_identity19101.BASE_FINGERPRINT,
        'Cache isolation proof was run for different compute identities')
    require(qa.get('host_evidence')==host and qa.get('host_report_sha256')==sha(args.host_report.read_bytes()),'QA embeds different host evidence')
    artifacts={name:{'sha256':sha((dist/name).read_bytes()),'bytes':(dist/name).stat().st_size} for name in ASSETS}
    mpps={name:artifacts[name] for name in (SINGLE,BUNDLE)}
    require(mpps==build.get('artifacts')==qa.get('artifacts'),'Built/applied/published MPP bytes differ')
    if 'reviewed_mpp_artifacts' in declaration:require(declaration['reviewed_mpp_artifacts']==mpps,'CI rebuilt different MPP bytes')
    application=application_evidence19101.read(source,mpps,report_path=args.application_report)
    require(qa.get('original_application')==application,'QA uses different original APKS application')
    hook=build.get('stock_shutter_hook',{})
    require(hook==qa.get('stock_shutter_hook') and hook.get('added_method_count')==0 and hook.get('existing_methods_preserved') is True and hook.get('byte_identical_to_baseline') is True,'Inherited stock method byte identity missing')
    require(hook.get('target')=='Lcom/light/beauty/mc/preview/shutter/module/ShutterButton;->onTouchEvent(Landroid/view/MotionEvent;)Z' and hook.get('original_sha256')=='ba56099d88ea1dbdab32dab9302393ddc9f2aae0e5f9900574b2551f973b69c1','Shutter target/original contract differs')
    require({k:application['report']['stock_shutter_hook'].get(k) for k in ('target','original_sha256','patched_sha256')}=={k:hook.get(k) for k in ('target','original_sha256','patched_sha256')},'Applied shutter hook differs from built payload')

    packages={name:checked_archive(dist/name) for name in (SINGLE,BUNDLE)}
    for name,version in ((SINGLE,VERSION),(BUNDLE,BUNDLE_VERSION)):
        members=packages[name];require(headers(members['META-INF/MANIFEST.MF']).get('Version')==version,'Wrong MPP version: '+name)
        for dex in ('classes.dex','ulike/runtime.dex','ulike/methods.dex'):dex_integrity(members[dex],name+'/'+dex)
        require(sha(members[GPU_ENTRY])==build['gpu_native']['sha256'] and len(members[GPU_ENTRY])==build['gpu_native']['bytes'],'GPU payload differs from built JNI')
        require(sha(members[NR_ENTRY])==build['nr_native']['sha256'] and len(members[NR_ENTRY])==build['nr_native']['bytes'],'NR1955 payload differs from rebuilt JNI')
        provenance=native_provenance19115.verify(source,members[GPU_ENTRY],members[NR_ENTRY],build['gpu_native'].get('optimization_provenance'))
        require(provenance==qa['gpu_native'].get('optimization_provenance'),'Current native provenance differs from QA')
        require(sha(members[SMOOTH_ENTRY])==build['smooth_native']['sha256'] and len(members[SMOOTH_ENTRY])==build['smooth_native']['bytes'],'Strong CPU payload differs from rebuilt JNI')
        smooth_provenance=native_smooth_provenance19115.verify(source,members[SMOOTH_ENTRY],build['smooth_native'].get('optimization_provenance'))
        require(smooth_provenance==qa['smooth_native'].get('optimization_provenance'),'Strong CPU native provenance differs from QA')
        shutter_rows=[line.split('\t') for line in members['ulike/methods.tsv'].decode().splitlines() if line.split('\t')[0]==hook['target']]
        require(shutter_rows==[[hook['target'],hook['original_sha256'],hook['patched_sha256']]],'Published shutter TSV differs from built/applied proof')
        for inherited,digest in build.get('inherited_native_sha256',{}).items():
            require(inherited in members and sha(members[inherited])==digest,'Inherited native payload changed: '+inherited)
    own=lambda name:name.startswith('ulike') or name.startswith('app/hiro/ulike/')
    require({n:v for n,v in packages[SINGLE].items() if own(n)}=={n:v for n,v in packages[BUNDLE].items() if own(n)},'Standalone/bundle ULike resources differ')
    require(build.get('gpu_native',{}).get('build')=='rebuilt_from_current_sources','GPU payload provenance missing')
    require(build.get('nr_native',{}).get('build')=='rebuilt_from_current_sources' and build.get('nr_native',{}).get('byte_identical_to_baseline') is True and build.get('nr_native')==qa.get('nr_native'),'NR1955 provenance/QA differs')
    require(build.get('gpu_native')==qa.get('gpu_native'),'GPU native provenance/QA differs')
    require(build.get('smooth_native',{}).get('build')=='rebuilt_from_current_sources' and build['smooth_native'].get('byte_identical_to_baseline') is True and build.get('smooth_native')==qa.get('smooth_native'),'Strong CPU provenance/QA differs')
    require(build['gpu_native'].get('byte_identical_to_baseline') is False,'Native optimization must be distinguished from byte-identical reuse')
    require(build.get('gpu_native',{}).get('added_jni_exports')==['Java_com_hiro_ulike_GpuNoise1960_readManyIntoObservedNative19117','Java_com_hiro_ulike_GpuNoise1960_readManyObservedNative19117'],'Exact reviewed additional GPU JNI exports differ')
    require(build.get('structural_inventory',{}).get('new_jni_methods')==['Lcom/hiro/ulike/GpuNoise1960;->readManyIntoObservedNative19117(JJ[I[I[[I[I[J)Z','Lcom/hiro/ulike/GpuNoise1960;->readManyObservedNative19117(JJ[I[I[J)[[I'],'Exact reviewed additional runtime JNI declarations differ')
    require(build.get('resource_delta')=={'standalone':sorted(CHANGED),'bundle':sorted(CHANGED)},'Exact approved resource changes missing')
    require(len(build.get('inherited_native_sha256',{}))==9,'Inherited native payload inventory differs')
    require(build.get('resource_delta')==qa.get('resource_delta') and build.get('structural_inventory')==qa.get('structural_inventory'),'QA resource/type inventory drift')
    expected_sums=''.join(artifacts[name]['sha256']+'  '+name+'\n' for name in sorted(ASSETS[:-1]))
    require((dist/'SHA256SUMS.txt').read_text()==expected_sums,'SHA256SUMS incomplete or inconsistent')
    require(json.loads((dist/'release-artifacts19101.json').read_text())==artifacts,'Finalizer release inventory differs')
    archive=checked_archive(dist/SOURCE_ZIP)
    expected={'DESIGN_REVIEW.md':(root/'DESIGN_REVIEW.md').read_bytes(),'production-roots19115.json':(root/'production-roots19115.json').read_bytes(),'manifest.json':(root/'manifest.json').read_bytes(),QA_NAME:(dist/QA_NAME).read_bytes(),
        'RELEASE_NOTES.txt':(dist/'RELEASE_NOTES.txt').read_bytes(),'evidence/host-audit19101.json':args.host_report.read_bytes(),
        'evidence/original-application19101.json':args.application_report.read_bytes(),'evidence/build19101.json':(dist/'build19101.json').read_bytes()}
    expected.update({'src/'+name:(source/name).read_bytes() for name in pins})
    expected.update({'publication/'+name:(root/'publication'/name).read_bytes() for name in pubpins})
    require(set(archive)==set(expected),'Source ZIP has missing/unexpected/private members')
    for name,raw in archive.items():
        checked_name(name)
        require(PurePosixPath(name).suffix.lower() not in ('.apk','.apks','.apkm','.aab','.png','.jpg','.jpeg','.heic','.heif','.mp4'),
            'Private input or generated APK in source ZIP: '+name)
        require(raw==expected[name],'Source/evidence ZIP bytes differ: '+name)
    result={'schema':'ulike19101-release-validation-v1','status':'passed','ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,
        'release_artifacts':artifacts,'host_tests_passed':True,'host_assertions':host['assertions'],
        'host_report_sha256':sha(args.host_report.read_bytes()),'original_apk_apply_tested':True,
        'original_application_report_sha256':application['report_sha256'],'source_consistency_verified':True,
        'archive_inventory_verified':True,'no_original_apks_or_private_images_distributed':True,
        'standalone_bundle_ulike_identical':True,'device_tested':False,'physical_android_tested':False,
        'device_speed_measured':False,'device_quality_verified':False,'first_shot_gpu_verified_on_physical_android':False}
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_bytes(json_bytes(result));return result
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('root','host-report','application-report','output'):parser.add_argument('--'+name,type=Path,required=True)
    args=parser.parse_args();print(json.dumps(validate(args),ensure_ascii=False,indent=2))
