#!/usr/bin/env python3
"""Negative-input tests for publication gate parsing; no release evidence mocked."""
from pathlib import Path
import argparse, copy, importlib.util, json, sys, zipfile

def native_negative_controls(source,raw,nr,work):
    import native_identity19101_negative
    report=json.loads((source.parent/'dist/build19101.json').read_text())['gpu_native']['identity_delta']
    return native_identity19101_negative.test(source,raw,nr,report,work/'native-identity-controls')

def repository_content_controls(source):
    import base64,hashlib
    from unittest.mock import patch
    path=source.parent/'publication/publish19101.py'
    spec=importlib.util.spec_from_file_location('release_content1997',path);pub=importlib.util.module_from_spec(spec);spec.loader.exec_module(pub)
    ref='a'*40;name='releases/ULike_ACTIVE.json';endpoint=f'repos/{pub.REPO}/contents/{name}?ref={ref}';count=0
    def row(raw,encoding):
        return dict(type='file',path=name,size=len(raw),sha=hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest(),
            encoding=encoding,content=base64.b64encode(raw).decode() if encoding=='base64' else '')
    small=b'{"test":"small"}\n';large=b'x'*(1024*1024+7)
    with patch.object(pub,'api',return_value=row(small,'base64')) as api,patch.object(pub.subprocess,'check_output') as rawget:
        assert pub.content(name,ref)==small and api.call_args.args==(endpoint,) and rawget.call_count==0;count+=1
    with patch.object(pub,'api',return_value=row(large,'none')) as api,patch.object(pub.subprocess,'check_output',return_value=large) as rawget:
        assert pub.content(name,ref)==large and api.call_args.args==(endpoint,);count+=1
        assert rawget.call_args.args==(['gh','api',endpoint,'-H','Accept: application/vnd.github.raw+json'],);count+=1
    for meta,raw,reference in [
        ({**row(large,'none'),'sha':'0'*40},large,ref),
        ({**row(large,'none'),'size':len(large)+1},large,ref),
        (row(large,'none'),large[:-1],ref),
        ({**row(large,'none'),'path':'another.json'},large,ref),
        ({**row(large,'none'),'type':'dir'},large,ref),
        (row(small,'none'),small,ref),
        (row(large,'none'),large,'main'),
        ({**row(small,'base64'),'sha':'0'*40},small,ref),
        ({**row(small,'base64'),'size':0},small,ref),
        ({**row(large,'none'),'encoding':'unknown'},large,ref)]:
        with patch.object(pub,'api',return_value=meta),patch.object(pub.subprocess,'check_output',return_value=raw):
            try:pub.content(name,reference)
            except (AssertionError,RuntimeError,ValueError):count+=1
            else:raise AssertionError('Changed or unbound repository content accepted')
    return count

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True);sys.path.insert(0,str(source))
    spec=importlib.util.spec_from_file_location('release_validation1997',source/'validate19101.py');gate=importlib.util.module_from_spec(spec);spec.loader.exec_module(gate)
    assertions=0
    def rejected(call):
        nonlocal assertions
        try:call()
        except (AssertionError,RuntimeError,ValueError,zipfile.BadZipFile):assertions+=1
        else:raise AssertionError('Malformed release evidence accepted')
    for name in ('../secret.apk','/root/file','a/../secret','a\\b','a//b','a/',''):
        rejected(lambda name=name:gate.checked_name(name))
    gate.checked_name('src/tests1997/corpus.java');assertions+=1
    children={name:{'status':'passed','assertions':2,'physical_android_tested':False} for name in gate.REQUIRED_HOST_GROUPS}
    children['finish_recovery95'].update(resident_policy_trace_verified=True)
    children['finish_recovery95'].update(wide_restore_status_verified=True,no_extra_gpu_dispatch_verified=True)
    children['timing'].update(**{key:True for key in ('recent_history_bounded16_verified','recent_history_success_only_verified','recent_history_exact_groups_verified','recent_history_lock_order_verified')})
    children['release_gate'].update(native_regional_inverse_verified=True,native_identity_negative_assertions=1,large_content_exact_commit_verified=True)
    children['gpu_math'].update(shader96=True,only_guarded_partial_predicates_changed=False,published95_records=1016,
        published95_reads=18240,published95_argb_differences=0,published95_confidence_differences=0,legacy_preprocessor_programs=60,
        focused_gles={'actual_gles_execution':True,'cutoff_proof':{'unsafe_rejections':0,'final_contribution_differences':0}})
    children['strong_adaptive94'].update(profile6_program=45,profile6_cases=70,profile6_compared_pixels=1800001,**{key:True for key in ('profile6_bit_exact','profile6_independent_certificates','profile6_preserves_10pct_gate','one_heavy_candidate_per_capture','existing_profile7_preserved')})
    children['cpu_copy96'].update(**{k:True for k in ('foreground_cpu_copy_disabled','existing_certificates_and_original_fallback_verified',
        'explicit_owned_verifiers_exact_speed_cancel_memory_verified','reused_result_alias_regression_verified',
        'actual_colour_jni_outputs_compared','gpu_queue_independent')})
    children['model_parallel96'].update(**{k:True for k in ('whole_model_bitwise_equal', 'actual_source_regions_overlapped', 'resident_runtime_upload_bitwise_equal', 'model_ticket_joined_before_handoff', 'callback_once_and_fallback_verified', 'existing_region_gpu_route_preserved', 'invalid_completed_runtime_rejected')})
    children['front_timing96'].update(controlled_native_transport=True,actual_jvm_loading=True)
    children['memory97'].update(**{key:True for key in ('foreground_same_budget_retry_verified','active_gpu_ownership_preserved','busy_native_deferred_release_verified','pressure_skips_optional_finish_copy','ordinary_photo_qualification_preserved','same_finish_pixels_verified','cancelled_speed_workers_classified','actual_worker_join_and_owner_release')})
    children['timing'].update(cases={'MemoryTiming1997Test':{'status':'passed','assertions':1}})
    children['front_plan97'].update(**{key:True for key in ('no_worker_sacrifice','simultaneous_budget_verified','largest_fitting_rows_verified','bounded_halo_growth','immutable_96_pixels_equal','multiple_workers_verified','optional_half_budget_drop_verified')})
    children['front_plan97']['arm97']={'status':'passed','actual_arm_neon_executed':True,'all_16_row_boundaries_bitwise_equal':True,'pixel_cases':128,'compared_pixels':1,'assertions':1,'compiler_version':'parser-fixture','qemu_version':'parser-fixture'}
    children['mask_rows98'].update(**{key:True for key in ('full_policy_equal','full_output_equal','raw_double_intermediates_equal','optional_budget_fallback_verified','worker_failure_join_verified','rotation_fallback_verified','actual_jni','host_timing_is_diagnostic_only')})
    children['mask_rows98'].update(device_speedup_verified=False,rows={'compared_pixels':24000000,'cache_cases':32,'rotated_fallback_cases':42,'cached_mask_byte_reads_upper_bound':1,'baseline_mask_byte_reads':3},parallel={'parallel_compared_pixels':1300000})
    for group,keys in (('front99', ('front1999_expired_admission_rejected', 'front1999_requalified_gpu_admitted', 'front1999_stage_reserved_before_copy', 'front1999_cached_support_reason', 'retained_frontmodel1991_cases')), ('chain99', ('resident_baseline_schedule_matches', 'resident_baseline_key_binds_wide_state', 'resident_progress_cancellation_frozen'))):
        children[group].update(**{key:True for key in keys},device_speedup_verified=False)
    for group,keys in (('timing', ('capture_bitmap_provenance_verified', 'ambiguous_metadata_trace_preserved', 'exact_input_lookup_verified')), ('capture99', ('completed_bitmap_trace_isolated', 'save_input_pixels_unchanged', 'save_bitmap_dimensions_observed'))):
        children[group].update(**{key:True for key in keys},device_speedup_verified=False)
    children['finish_memory92'].update(**{key:True for key in ('retained_finish_memory1992_gate', 'strict_standard1999_baseline_identity')})
    children['pipeline_gate'].update(**{key:True for key in ('retained_pipeline_gate1992', 'strict_narrow_baseline_metadata1999', 'readiness_peer_pixels_forbidden1999')})
    for group,keys in gate.CURRENT_PROOF_KEYS.items():children[group].update(device_speedup_verified=False,**{key:True for key in keys})
    children['gpu_math']['all_60_legacy_programs_unchanged']=False
    children['regional107'].update(assertions=1380012,arithmetic_negative_controls=4,device_mismatch_reproduced=False,
        regional_compared_words=929118,exact_mismatches=0,physical_log_values_are_not_test_inputs=True,
        full_strong_regression_required=['strong_trace106','strong_math105'],coverage=dict(gate.REGIONAL107_COVERAGE),
        rounding_ties={'even_lower':1,'even_upper':1,'underflow':1},java={'cases':225244,'strictfp':True,'canonical_nan':True},
        gles={'status':'passed','actual_gles_execution':True,'cases':225244,'invalid_domain_cases':10,'weight_gate_cases':392,
            'negative_controls':[{'name':name,'mismatches':count} for name,count in (('output_lsb',225244),('reversed_horizontal_weights',165402),('truncate_input_mantissa',81276),('scale_ties_away_instead_of_even',2684))],
            'regional_groups':[{'width':w,'height':h,'first_grid_y':first,'points':points,'planes':3,'variants':2} for w,h,first,points in gate.REGIONAL107_GEOMETRIES],
            'renderer':'parser-fixture','gl_version':'parser-fixture','helper_sha256':'1'*64},corpus_sha256='1'*64)
    children['cache107'].update(assertions=321,native_fingerprint_old='1'*64,native_fingerprint_new='2'*64,
        cache={'status':'passed','assertions':143,'proof_families':8,'corpus_families':4},
        ingress={'status':'passed','assertions':168,'generated_models':84,'invalid_binary32_classes':9},
        ingress_callsites={'GpuStrong1960.java':7,'GpuAnalysis1961.java':2,'GpuStrongTuning1975.java':2,'GpuStrongRouting1978.java':1,'GpuStrongLayout1976.java':1})
    children['validation104'].update(cases=64,stage_assertions=156,retained_assertions=71)
    children['raw_crop104'].update(cases=24,compared_argb_words=1123168,tail_guard_words=531364,geometry_mode=0,max_rows=32,output_slot=23,scratch_slot=22,scratch_bytes=4,source_slots=[0,1,2,24])
    children['burst103']['production_single_image_policy_tested']=False
    children['capture_phases104'].update(retained_assertions=899,new_assertions=9)
    children['single_admission104']['cases']={version:{'status':'passed','assertions':count,'baseline_reproduced':baseline,'production_single_image_policy_tested':production} for version,count,baseline,production in (('103',9,True,False),('104',12,False,True))}
    children['face_reuse104']['tests']={name:{'cases':9,'avoided_detector_calls':calls,'mask_sha256':'1'*64} for name,calls in (('baseline103',0),('current104',2))}
    children['source_mask103'].update(compared_pixels=50000000,cache_cases=47,fallback_cases=43,cached_mask_byte_reads_upper_bound=1,baseline_mask_byte_reads=3)
    children['burst103'].update(retained_assertions=802,retained_scenarios=51)
    children['front_plan97']['retained97_planning_cases_on_optimized_kernel']=True
    children['strong_math105'].update(assertions=7,actual_arm_neon_executed=False,device_mismatch_reproduced=False,shader_math_changed=True,regional19107_exact_delta_verified=True,
        corpus={'records':98,'pixels':2234848},boundary_rows=[0,1,32,94,255,256,278,299],gl_environment='parser-fixture',oracle_sha256='1'*64,
        gpu={'status':'passed','physical_android_tested':False,'assertions':2,'records':98,'reads':2008,'compared_pixels':10310656,'single_rows':400,'confidence_off_checks':1004,'argb_differences':0},
        cpu={name:{'status':'passed','physical_android_tested':False,'assertions':2,'records':98,'rows':400,'native_rows':native_rows,'java_rows':java_rows,'compared_words':4141680} for name,native_rows,java_rows in (('native',400,400),('fallback',0,800))})
    children['diagnostics105'].update(assertions=526,cases=64,reason_tests={'status':'passed','assertions':115},journal_tests={'status':'passed','assertions':169},retained_validation={'status':'passed','assertions':242})
    children['strong_inputs105'].update(assertions=157,cases=34,fixture_classes_in_runtime=False)
    children['strong_trace106'].update(assertions=25,physical_android_tested=False,device_mismatch_reproduced=False,
        cpu={'status':'passed','assertions':2,'records':79,'cpu_row_pixels':130925,'checkpoints':68513,'nlm_on':55,'nlm_off':22,'sparse':2,'near_half':8},
        gpu={'status':'passed','assertions':2,'records':79,'compared_checkpoint_words':68513,'compared_row_pixels':261850,'negative_controls':5,'raw_differences':0},
        wrapper={'status':'passed','assertions':10,'actual_wrapper_jni':True,'physical_android_tested':False},
        source_arithmetic={key:'1'*64 for key in ('guide','sample','nonlocalFine','regionalSigma','main_0','main_1','main_2','main_3','main_nlm')},
        boundary_rows=[0,1,31,32,63,80,94,255,256,278,520,539],gl_environment='parser-fixture',trace_corpus_sha256='1'*64)
    children['trace_flow106'].update(assertions=111,cases=40,fixture_classes_in_runtime=False)
    children['diagnostics106'].update(assertions=844,cases=99,trace_tests={'status':'passed','assertions':222,'cases':35,'physical_android_tested':False},retained_diagnostics={'status':'passed','assertions':622,'cases':64,'physical_android_tested':False})
    good={'status':'passed','assertions':sum(row['assertions'] for row in children.values()),'physical_android_tested':False,'tests':children}
    gate.positive_host(good);assertions+=1
    for group,key,value in [('regional107','arithmetic_negative_controls',2),('regional107','arithmetic_negative_controls',True),('regional107','device_mismatch_reproduced',True),('regional107','physical_android_tested',True),('gpu_math','all_60_legacy_programs_unchanged',True),('strong_math105','shader_math_changed',False),('strong_math105','regional19107_exact_delta_verified',False)]:
        edited=copy.deepcopy(good);edited['tests'][group][key]=value
        rejected(lambda edited=edited:gate.positive_host(edited))
    for key,value in [('native_fingerprint_old',''),('native_fingerprint_new',''),('native_fingerprint_old','2'*64),('physical_android_tested',True)]:
        edited=copy.deepcopy(good);edited['tests']['cache107'][key]=value
        rejected(lambda edited=edited:gate.positive_host(edited))
    for part,key,bad in [(None,'cases',98),(None,'cases',True),(None,'assertions',843),('trace_tests','assertions',221),('trace_tests','cases',34),('retained_diagnostics','cases',63),('retained_diagnostics','assertions',621),('trace_tests','physical_android_tested',True),('retained_diagnostics','status','failed')]:
        edited=copy.deepcopy(good);target=edited['tests']['diagnostics106']
        if part is not None:target=target[part]
        target[key]=bad
        rejected(lambda edited=edited:gate.positive_host(edited))
    for key,bad in [('cases',39),('cases',True),('assertions',110),('assertions',True),('fixture_classes_in_runtime',True)]:
        edited=copy.deepcopy(good);edited['tests']['trace_flow106'][key]=bad
        rejected(lambda edited=edited:gate.positive_host(edited))
    # Raw trace evidence is mandatory independently of generic flags/accounting.
    for part,key,bad in [('wrapper','actual_wrapper_jni',False),('wrapper','assertions',9),('wrapper','physical_android_tested',True),('cpu','records',78),('cpu','near_half',0),('cpu','nlm_on',0),('cpu','nlm_off',0),('cpu','sparse',0),('gpu','raw_differences',1),('gpu','negative_controls',4),('gpu','compared_checkpoint_words',0),('gpu','compared_row_pixels',261849)]:
        edited=copy.deepcopy(good);edited['tests']['strong_trace106'][part][key]=bad
        rejected(lambda edited=edited:gate.positive_host(edited))
    for key,bad in [('device_mismatch_reproduced',True),('source_arithmetic',{}),('boundary_rows',[]),('gl_environment',''),('trace_corpus_sha256','')]:
        edited=copy.deepcopy(good);edited['tests']['strong_trace106'][key]=bad
        rejected(lambda edited=edited:gate.positive_host(edited))
    for update in ({'status':'pending'},{'assertions':True},{'assertions':0},{'assertions':3},{'physical_android_tested':True},
            {'device_speedup_verified':True},{'tests':{**children,'release_gate':{**children['release_gate'],'native_regional_inverse_verified':False}}},{'tests':{**children,'release_gate':{**children['release_gate'],'native_identity_negative_assertions':0}}},{'tests':{**children,'finish_recovery95':{**children['finish_recovery95'],'resident_policy_trace_verified':False}}},{'tests':{}},{'tests':{**children,'chain':{'status':'failed','assertions':2}}},
            {'assertions':good['assertions']-children['finish_recovery95']['assertions'],'tests':{k:v for k,v in children.items() if k!='finish_recovery95'}},
            {'assertions':good['assertions']+2,'tests':{**children,'stale':{'status':'passed','assertions':2}}}):
        rejected(lambda update=update:gate.positive_host({**good,**update}))
    for group,key in [('gpu_math','shader96'),('gpu_math','only_guarded_partial_predicates_changed'),
            ('gpu_math','published95_records'),('gpu_math','published95_reads'),('gpu_math','legacy_preprocessor_programs')]+[
            ('cpu_copy96',key) for key in ('foreground_cpu_copy_disabled','existing_certificates_and_original_fallback_verified',
            'explicit_owned_verifiers_exact_speed_cancel_memory_verified','reused_result_alias_regression_verified',
            'actual_colour_jni_outputs_compared','gpu_queue_independent')]:
        changed={**children,group:{**children[group],key:None}}
        rejected(lambda changed=changed:gate.positive_host({**good,'tests':changed}))
    for group,key in [('model_parallel96',k) for k in ('whole_model_bitwise_equal', 'actual_source_regions_overlapped', 'resident_runtime_upload_bitwise_equal', 'model_ticket_joined_before_handoff', 'callback_once_and_fallback_verified', 'existing_region_gpu_route_preserved', 'invalid_completed_runtime_rejected')]+[('front_timing96','controlled_native_transport'),('front_timing96','actual_jvm_loading')]:
        changed={**children,group:{**children[group],key:False}}
        rejected(lambda changed=changed:gate.positive_host({**good,'tests':changed}))
    rejected(lambda:gate.positive_host({**good,'tests':{**children,'release_gate':{**children['release_gate'],'large_content_exact_commit_verified':False}}}))
    for key in ('foreground_same_budget_retry_verified','active_gpu_ownership_preserved','busy_native_deferred_release_verified','pressure_skips_optional_finish_copy','ordinary_photo_qualification_preserved','same_finish_pixels_verified','cancelled_speed_workers_classified','actual_worker_join_and_owner_release'):
        rejected(lambda key=key:gate.positive_host({**good,'tests':{**children,'memory97':{**children['memory97'],key:False}}}))
    rejected(lambda:gate.positive_host({**good,'tests':{**children,'timing':{**children['timing'],'cases':{}}}}))
    for key in ('no_worker_sacrifice','simultaneous_budget_verified','largest_fitting_rows_verified','bounded_halo_growth','immutable_96_pixels_equal','multiple_workers_verified','optional_half_budget_drop_verified'):
        rejected(lambda key=key:gate.positive_host({**good,'tests':{**children,'front_plan97':{**children['front_plan97'],key:False}}}))
    for key in ('status','actual_arm_neon_executed','all_16_row_boundaries_bitwise_equal','pixel_cases','compared_pixels','assertions','compiler_version','qemu_version'):
        child={**children['front_plan97'],'arm97':{**children['front_plan97']['arm97'],key:None}}
        rejected(lambda child=child:gate.positive_host({**good,'tests':{**children,'front_plan97':child}}))
    for group,keys in [('finish_recovery95',('wide_restore_status_verified','no_extra_gpu_dispatch_verified')),('timing',('recent_history_bounded16_verified','recent_history_success_only_verified','recent_history_exact_groups_verified','recent_history_lock_order_verified'))]:
        for key in keys:
            rejected(lambda group=group,key=key:gate.positive_host({**good,'tests':{**children,group:{**children[group],key:False}}}))
    for key in ('profile6_bit_exact','profile6_independent_certificates','profile6_preserves_10pct_gate','one_heavy_candidate_per_capture','existing_profile7_preserved','profile6_program','profile6_cases','profile6_compared_pixels'):
        rejected(lambda key=key:gate.positive_host({**good,'tests':{**children,'strong_adaptive94':{**children['strong_adaptive94'],key:None}}}))
    for value in (True,0,1800000):
        rejected(lambda value=value:gate.positive_host({**good,'tests':{**children,'strong_adaptive94':{**children['strong_adaptive94'],'profile6_compared_pixels':value}}}))
    for key in ('full_policy_equal','full_output_equal','raw_double_intermediates_equal','optional_budget_fallback_verified','worker_failure_join_verified','rotation_fallback_verified','actual_jni','host_timing_is_diagnostic_only'):
        rejected(lambda key=key:gate.positive_host({**good,'tests':{**children,'mask_rows98':{**children['mask_rows98'],key:False}}}))
    rejected(lambda:gate.positive_host({**good,'tests':{**children,'mask_rows98':{**children['mask_rows98'],'device_speedup_verified':True}}}))
    for key,invalid in [('compared_pixels',23999999),('cache_cases',31),('rotated_fallback_cases',41),('cached_mask_byte_reads_upper_bound',2),('cached_mask_byte_reads_upper_bound',True),('baseline_mask_byte_reads',0)]:
        child={**children['mask_rows98'],'rows':{**children['mask_rows98']['rows'],key:invalid}}
        rejected(lambda child=child:gate.positive_host({**good,'tests':{**children,'mask_rows98':child}}))
    child={**children['mask_rows98'],'parallel':{'parallel_compared_pixels':1299999}}
    rejected(lambda:gate.positive_host({**good,'tests':{**children,'mask_rows98':child}}))
    for group,keys in (('front99', ('front1999_expired_admission_rejected', 'front1999_requalified_gpu_admitted', 'front1999_stage_reserved_before_copy', 'front1999_cached_support_reason', 'retained_frontmodel1991_cases')), ('chain99', ('resident_baseline_schedule_matches', 'resident_baseline_key_binds_wide_state', 'resident_progress_cancellation_frozen'))):
        for key in keys:
            for missing in (False,None,1):
                rejected(lambda group=group,key=key,missing=missing:gate.positive_host({**good,'tests':{**children,group:{**children[group],key:missing}}}))
        rejected(lambda group=group:gate.positive_host({**good,'tests':{**children,group:{**children[group],'device_speedup_verified':True}}}))
    for group,keys in (('timing', ('capture_bitmap_provenance_verified', 'ambiguous_metadata_trace_preserved', 'exact_input_lookup_verified')), ('capture99', ('completed_bitmap_trace_isolated', 'save_input_pixels_unchanged', 'save_bitmap_dimensions_observed'))):
        for key in keys:
            for missing in (False,None,1):
                rejected(lambda group=group,key=key,missing=missing:gate.positive_host({**good,'tests':{**children,group:{**children[group],key:missing}}}))
        rejected(lambda group=group:gate.positive_host({**good,'tests':{**children,group:{**children[group],'device_speedup_verified':True}}}))
    for key in ('retained_finish_memory1992_gate', 'strict_standard1999_baseline_identity'):
        for missing in (False,None,1):
            rejected(lambda key=key,missing=missing:gate.positive_host({**good,'tests':{**children,'finish_memory92':{**children['finish_memory92'],key:missing}}}))
    for key in ('retained_pipeline_gate1992', 'strict_narrow_baseline_metadata1999', 'readiness_peer_pixels_forbidden1999'):
        for missing in (False,None,1):
            rejected(lambda key=key,missing=missing:gate.positive_host({**good,'tests':{**children,'pipeline_gate':{**children['pipeline_gate'],key:missing}}}))
    for group,keys in gate.CURRENT_PROOF_KEYS.items():
        for key in keys:
            for missing in (False,None,1):
                rejected(lambda group=group,key=key,missing=missing:gate.positive_host({**good,'tests':{**children,group:{**children[group],key:missing}}}))
        rejected(lambda group=group:gate.positive_host({**good,'tests':{**children,group:{**children[group],'device_speedup_verified':True}}}))
    rejected(lambda:gate.positive_host({**good,'tests':{**children,'front_plan97':{**children['front_plan97'],'retained97_planning_cases_on_optimized_kernel':False}}}))
    for group,key,value in [('source_mask103','compared_pixels',49999999),('source_mask103','cache_cases',46),('source_mask103','fallback_cases',42),('source_mask103','cached_mask_byte_reads_upper_bound',2),('source_mask103','baseline_mask_byte_reads',0),('burst103','retained_assertions',801),('burst103','retained_scenarios',50)]:
        rejected(lambda group=group,key=key,value=value:gate.positive_host({**good,'tests':{**children,group:{**children[group],key:value}}}))
    for group,key,value in [('validation104','cases',63),('validation104','cases',True),('validation104','stage_assertions',155),('validation104','retained_assertions',70),('capture_phases104','retained_assertions',898),('capture_phases104','new_assertions',8),('burst103','production_single_image_policy_tested',True),('burst103','production_single_image_policy_tested',None)]:
        rejected(lambda group=group,key=key,value=value:gate.positive_host({**good,'tests':{**children,group:{**children[group],key:value}}}))
    for key in ('cases','compared_argb_words','tail_guard_words','geometry_mode','max_rows','output_slot','scratch_slot','scratch_bytes','source_slots'):
        for invalid in (None,False):
            rejected(lambda key=key,invalid=invalid:gate.positive_host({**good,'tests':{**children,'raw_crop104':{**children['raw_crop104'],key:invalid}}}))
    rejected(lambda:gate.positive_host({**good,'tests':{**children,'raw_crop104':{**children['raw_crop104'],'source_slots':[False,True,2,24]}}}))
    for version in ('103','104'):
        for key,value in [('status','failed'),('assertions',0),('assertions',True),('baseline_reproduced',None),('production_single_image_policy_tested',None)]:
            cases={**children['single_admission104']['cases'],version:{**children['single_admission104']['cases'][version],key:value}}
            rejected(lambda cases=cases:gate.positive_host({**good,'tests':{**children,'single_admission104':{**children['single_admission104'],'cases':cases}}}))
    for name in ('baseline103','current104'):
        for key,value in [('cases',8),('cases',True),('avoided_detector_calls',None),('avoided_detector_calls',True),('mask_sha256',''),('mask_sha256','2'*64)]:
            face={**children['face_reuse104']['tests'],name:{**children['face_reuse104']['tests'][name],key:value}}
            rejected(lambda face=face:gate.positive_host({**good,'tests':{**children,'face_reuse104':{**children['face_reuse104'],'tests':face}}}))
    # New proof fields are fail-closed independently of total accounting.
    def changed105(path,value):
        edited=copy.deepcopy(children);row=edited
        for key in path[:-1]:row=row[key]
        if value is remove105:row.pop(path[-1],None)
        else:row[path[-1]]=value
        report={**good,'tests':edited,'assertions':sum(r['assertions'] for r in edited.values())}
        gate.positive_host(report)
    remove105=object()
    paths105=[
        ('strong_inputs105','cases'),('strong_inputs105','fixture_classes_in_runtime'),
        ('strong_math105','actual_arm_neon_executed'),('strong_math105','device_mismatch_reproduced'),('strong_math105','shader_math_changed'),
        ('strong_math105','boundary_rows'),('strong_math105','gl_environment'),('strong_math105','oracle_sha256'),
        ('diagnostics105','cases')]
    paths105 += [('strong_math105','corpus',key) for key in ('records','pixels')]
    paths105 += [('strong_math105','gpu',key) for key in ('status','physical_android_tested','assertions','records','reads','compared_pixels','single_rows','confidence_off_checks','argb_differences')]
    paths105 += [('strong_math105','cpu',name,key) for name in ('native','fallback') for key in ('status','physical_android_tested','assertions','records','rows','native_rows','java_rows','compared_words')]
    paths105 += [('diagnostics105',name,key) for name in ('reason_tests','journal_tests','retained_validation') for key in ('status','assertions')]
    for path in paths105:
        for value in (None,remove105):rejected(lambda path=path,value=value:changed105(path,value))
    for path,value in [(('strong_inputs105','cases'),33),(('strong_inputs105','cases'),True),(('strong_inputs105','fixture_classes_in_runtime'),True),
            (('strong_math105','boundary_rows'),[False,True,32,94,255,256,278,299]),(('strong_math105','corpus','records'),True),
            (('strong_math105','gpu','argb_differences'),1),(('strong_math105','gpu','argb_differences'),False),
            (('strong_math105','cpu','native','native_rows'),0),(('strong_math105','cpu','fallback','java_rows'),400),
            (('diagnostics105','cases'),63),(('diagnostics105','cases'),True),(('diagnostics105','reason_tests','assertions'),114),
            (('diagnostics105','journal_tests','assertions'),168),(('diagnostics105','retained_validation','assertions'),241)]:
        rejected(lambda path=path,value=value:changed105(path,value))
    cache_paths=[('cache107','cache',key) for key in ('status','assertions','proof_families','corpus_families')]
    cache_paths += [('cache107','ingress',key) for key in ('status','assertions','generated_models','invalid_binary32_classes')]
    cache_paths += [('cache107','ingress_callsites',key) for key in children['cache107']['ingress_callsites']]
    for path in cache_paths:
        for value in (None,remove105):rejected(lambda path=path,value=value:changed105(path,value))
    rejected(lambda:changed105(('cache107','assertions'),320))
    regional_paths=[('regional107',k) for k in ('regional_compared_words','exact_mismatches','physical_log_values_are_not_test_inputs','full_strong_regression_required','corpus_sha256')]
    regional_paths += [('regional107','coverage',key) for key in gate.REGIONAL107_COVERAGE]
    regional_paths += [('regional107','rounding_ties',key) for key in ('even_lower','even_upper','underflow')]
    regional_paths += [('regional107','java',key) for key in ('cases','strictfp','canonical_nan')]
    regional_paths += [('regional107','gles',key) for key in ('status','actual_gles_execution','cases','invalid_domain_cases','weight_gate_cases','negative_controls','regional_groups','renderer','gl_version','helper_sha256')]
    for path in regional_paths:
        for value in (None,remove105):rejected(lambda path=path,value=value:changed105(path,value))
    for path,value in [(('regional107','assertions'),1380011),(('regional107','coverage','total_cases'),225243),(('regional107','exact_mismatches'),False),(('regional107','exact_mismatches'),1),
            (('regional107','rounding_ties','underflow'),0),(('regional107','gles','negative_controls',0,'mismatches'),0),
            (('regional107','gles','regional_groups',9,'variants'),1),(('regional107','gles','regional_groups',8,'first_grid_y'),0),
            (('regional107','gles','regional_groups',0,'points'),True)]:
        rejected(lambda path=path,value=value:changed105(path,value))
    bad=work/'unsafe.zip'
    with zipfile.ZipFile(bad,'w') as archive:archive.writestr('../secret.apk',b'private')
    rejected(lambda:gate.checked_archive(bad))
    symlink=work/'symlink.zip'
    with zipfile.ZipFile(symlink,'w') as archive:
        item=zipfile.ZipInfo('safe');item.create_system=3;item.external_attr=(0o120777<<16);archive.writestr(item,b'../private')
    rejected(lambda:gate.checked_archive(symlink))
    goodzip=work/'good.zip'
    with zipfile.ZipFile(goodzip,'w') as archive:archive.writestr('src/code.java',b'class A {}')
    assert gate.checked_archive(goodzip)=={'src/code.java':b'class A {}'};assertions+=1
    with zipfile.ZipFile(source.parent/'dist/ULike_HQ_Texture_Online_v1.9.107.mpp') as artifact:
        native_assertions=native_negative_controls(source,artifact.read('ulike1960/runtime/libulike_gpu1960.so'),artifact.read('ulike1955/runtime/libulike_nr1955.so'),work)
    content_assertions=repository_content_controls(source)
    assertions+=native_assertions+content_assertions
    result={'status':'passed','assertions':assertions,'native_identity_negative_assertions':native_assertions,'native_regional_inverse_verified':True,'large_content_exact_commit_verified':True,'repository_content_assertions':content_assertions,'physical_android_tested':False,'device_speedup_verified':False,
        'release_gate_negative_inputs_verified':True,'actual_release_validation_in_this_unit_test':False}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work),indent=2))
