#!/usr/bin/env python3
"""Fail-closed publication gate for exact .105 artifacts and fresh host/APKS evidence.

This is a desktop/build gate. It cannot claim Android GPU speed, first-shot GPU
execution, or device quality from software GLES and original-APKS application.
"""
from pathlib import Path, PurePosixPath
import argparse, hashlib, json, re, stat, zipfile
from build19101 import (ROOT, VERSION, BUNDLE_VERSION, SINGLE, BUNDLE, BASE_PINS,
    GPU_ENTRY, NR_ENTRY, CHANGED, TOOL_PINS, source_pins, publication_pins, production_path,
    headers, dex_integrity, sha, require, json_bytes, runtime_source_version)
from finalize19101 import QA_NAME, SOURCE_ZIP, SELECTED
import application_evidence19101
import native_identity19101

ASSETS=(SINGLE,BUNDLE,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt','SHA256SUMS.txt')
FALSE_QA=('device_tested','physical_android_tested','device_speed_measured','device_quality_verified',
    'gpu_execution_on_physical_android','first_shot_gpu_verified_on_physical_android',
    'samsung_gpu_failure_root_cause_confirmed','all_processing_on_gpu','capture_fusion_enabled','device_capture_variance_improvement_verified')
REQUIRED_HOST_GROUPS=frozenset(('logcopy95','shutter94','front94','queue95','runtime_version','fingerprint','release_gate','startup','strong_timing95',
    'timing','route_failure92','memory','gpu_math','strong_adaptive94','strong_transport95','model_layout94','native91',
    'finish_memory92','chain','finish_domain','pipeline_gate','failure92','finish_recovery95','model_parallel96','cpu_copy96','front_timing96','memory97','front_plan97','mask_rows98','front99','chain99','capture99'))

CURRENT_PROOF_KEYS={
    'strong_inputs105':('fixed_inputs_shared_by_three_backends','actual_stage_replay_gate','foreground_inert','bounded_halo_and_memory','native_fallback_distinct','mutation_not_certified','cancellation_propagated'),
    'strong_math105':('frozen_java_expected','source_width_3060','actual_jni','confidence_on_off_same_argb','unaligned_single_row_same_argb','both_gpu_banks','source_and_policy_preserved','cancelled_row_output_preserved','native_and_explicit_java_compared'),
    'diagnostics105':('diagnostic_reasons_separated','legacy_failure_not_instability','known_empty_mask_observed','fixed_replay_api_bounded','journal_v1_v2_v3_v4_compatible','queue_journal_extended_scalars','strict_mismatch_preserved'),
    'raw_crop104':('actual_jni_gpu','source_unchanged'),
    'validation104':('stage_attribution_exact','cpu_replay_instability_not_gpu_blame','foreground_gpu_untouched','gpu_crop_exact','gpu_lease_cleanup','diagnostic_bounded','original_mismatch_preserved','journal_v1_v2_v3_compatible'),
    'face_reuse104':('baseline_double_detection_reproduced','successful_preanalysis_reused','same_mask_outputs','failure_fallback_preserved','identity_guards_preserved','thread_local_completion_verified'),
    'single_admission104':('baseline103_supplementary_admission_reproduced','single104_no_extra_request_copy_or_analysis','single104_original_q0_delivery_preserved'),
    'capture_phases104':('post_acquisition_phases_exact_owner_verified','phase_replay_and_ambiguity_safe','state_wait_not_inferred_from_result_entry'),
    'runtime_version':('single_capture_dex_invariant_negative_controls','nested_diagnostic_version_literals_verified','legacy_timing_reader_exception_scoped'),
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
    'gpu_math':('strict19100_sharing_extension_verified','regional_coordinates_shared_exactly','local_luma_ratio_shared_exactly','local_luma_ratio_preparation_only','all_60_legacy_programs_unchanged'),
}
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
    for key in ('actual_arm_neon_executed','physical_android_tested','device_speedup_verified','device_mismatch_reproduced','shader_math_changed'):
        require(math.get(key) is False,'Strong .105 host-only or unchanged-math scope differs: '+key)
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
    positive_host105(tests)
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
    require(tests['release_gate'].get('native_identity_only_inverse_verified') is True and type(tests['release_gate'].get('native_identity_negative_assertions')) is int and tests['release_gate']['native_identity_negative_assertions']>0,'Native identity corruption controls missing')
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
    require(qa.get('host_tests_passed') is True and qa.get('original_apk_apply_tested') is True,'Final evidence gate incomplete')
    for key in FALSE_QA:require(qa.get(key) is False,'Unverified physical capability claimed: '+key)
    require(declaration.get('runtime_version')==build.get('runtime_version')==qa.get('runtime_version')==VERSION and declaration.get('timing_version_matches_release') is True and build.get('timing_version_matches_release') is True and qa.get('timing_version_matches_release') is True,'Timing runtime/package versions differ')
    for key in ('single_capture_timing_only_false_verified', 'legacy_capture_admission_unreachable', 'fixed_strong_diagnostic_guard_verified', 'strict_photo_admission_preserved'):
        require(build.get('structural_inventory',{}).get(key) is True,'Emitted DEX single-shot invariant missing: '+key)
    require(build.get('fixed_strong_diagnostic_dex_verified') is True and type(build.get('strong_diagnostic_negative_controls')) is int and build['strong_diagnostic_negative_controls']==4,'Built Strong diagnostic DEX negative controls missing')
    require(build.get('structural_inventory',{}).get('runtime_version')==VERSION and build['structural_inventory'].get('timing_version_matches_release') is True,'Emitted DEX timing version missing')
    require(build.get('device_tested') is False and build.get('device_speed_measured') is False
        and build.get('device_quality_verified') is False,'Build claims physical verification')
    pins=source_pins();pubpins=publication_pins();pin_files(source,pins);pin_files(root/'publication',pubpins)
    require(declaration.get('reviewed_source_sha256')==pins==build.get('source_sha256')==qa.get('source_sha256'),
        'Reviewed/built/current source graph differs')
    require(declaration.get('reviewed_publication_sha256')==pubpins==build.get('publication_sha256')==qa.get('publication_sha256'),
        'Reviewed/built/current publisher graph differs')
    roots=declaration.get('production_helper_roots')
    require(isinstance(roots,list) and roots and roots==sorted(set(roots)),'Production root inventory invalid')
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
        require(sha(members[NR_ENTRY])==build['nr_native']['sha256'] and len(members[NR_ENTRY])==build['nr_native']['bytes'],'NR1955 payload differs from inherited JNI')
        identity=native_identity19101.verify(members[GPU_ENTRY],native_identity19101.current_fingerprint(source),build['gpu_native']['identity_delta'],source,members[NR_ENTRY])
        require(build['gpu_native'].get('identity_delta')==identity==qa['gpu_native'].get('identity_delta'),'Exact identity-only native inverse proof differs')
        shutter_rows=[line.split('\t') for line in members['ulike/methods.tsv'].decode().splitlines() if line.split('\t')[0]==hook['target']]
        require(shutter_rows==[[hook['target'],hook['original_sha256'],hook['patched_sha256']]],'Published shutter TSV differs from built/applied proof')
        for inherited,digest in build.get('inherited_native_sha256',{}).items():
            require(inherited in members and sha(members[inherited])==digest,'Inherited native payload changed: '+inherited)
    own=lambda name:name.startswith('ulike') or name.startswith('app/hiro/ulike/')
    require({n:v for n,v in packages[SINGLE].items() if own(n)}=={n:v for n,v in packages[BUNDLE].items() if own(n)},'Standalone/bundle ULike resources differ')
    require(build.get('gpu_native',{}).get('build')=='rebuilt_from_current_sources','GPU payload provenance missing')
    require(build.get('nr_native',{}).get('build')=='inherited_byte_identical_from_published_1.9.104' and build.get('nr_native',{}).get('byte_identical_to_baseline') is True and build.get('nr_native')==qa.get('nr_native'),'NR1955 provenance/QA differs')
    require(build.get('gpu_native')==qa.get('gpu_native'),'GPU native provenance/QA differs')
    require(build.get('gpu_native',{}).get('added_jni_exports')==[],'Exact reviewed additional GPU JNI exports differ')
    require(build.get('structural_inventory',{}).get('new_jni_methods')==[],'Exact reviewed additional runtime JNI declarations differ')
    require(build.get('resource_delta')=={'standalone':sorted(CHANGED),'bundle':sorted(CHANGED)},'Exact approved resource changes missing')
    require(len(build.get('inherited_native_sha256',{}))==11,'Inherited native payload inventory differs')
    require(build.get('resource_delta')==qa.get('resource_delta') and build.get('structural_inventory')==qa.get('structural_inventory'),'QA resource/type inventory drift')
    expected_sums=''.join(artifacts[name]['sha256']+'  '+name+'\n' for name in sorted(ASSETS[:-1]))
    require((dist/'SHA256SUMS.txt').read_text()==expected_sums,'SHA256SUMS incomplete or inconsistent')
    require(json.loads((dist/'release-artifacts19101.json').read_text())==artifacts,'Finalizer release inventory differs')
    archive=checked_archive(dist/SOURCE_ZIP)
    expected={'manifest.json':(root/'manifest.json').read_bytes(),QA_NAME:(dist/QA_NAME).read_bytes(),
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
