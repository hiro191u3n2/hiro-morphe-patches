#!/usr/bin/env python3
"""Negative-input tests for publication gate parsing; no release evidence mocked."""
from pathlib import Path
import argparse, importlib.util, json, sys, zipfile

def native_negative_controls(source,raw,nr,work):
    import native_delta19100 as identity
    count=0;fingerprint=identity.current_fingerprint(source)
    build=json.loads((source.parent/'dist/build19100.json').read_text())
    report=build['gpu_native']['reviewed_source_delta']
    identity.verify(raw,fingerprint,report,source,nr);count+=1
    assert report['inverse_rebuild_sha256']==identity.BASE_SHA256 and report['nr1955']['inverse_rebuild_sha256']==identity.NR_BASE_SHA256;count+=1
    def rejected(value=raw,single=nr,proof=report,expected=fingerprint):
        nonlocal count
        try:identity.verify(value,expected,proof,source,single)
        except (AssertionError,ValueError):count+=1
        else:raise AssertionError('Changed native payload or altered source/inverse proof accepted')
    for kind,binary in [('gpu',raw),('nr1955',nr)]:
        sections=identity.sections(binary)
        positions=[0,18,40]+[row[4] for name,row in sections.items() if row[1]!=8 and row[5]>0 and name not in ('',)]
        for section in ('.text','.rodata'):
            row=sections[section];positions.extend((row[4]+row[5]//2,row[4]+row[5]-1))
        for position in positions:
            changed=bytearray(binary);changed[position]^=1
            rejected(value=bytes(changed)) if kind=='gpu' else rejected(single=bytes(changed))
        for changed in (binary+b'\0',binary[:-1]):
            rejected(value=changed) if kind=='gpu' else rejected(single=changed)
    rejected(expected='0'*64);rejected(expected='bad')
    for update in ({'current_sha256':'0'*64},{'current_bytes':len(raw)-1},{'baseline_sha256':'0'*64},
          {'inverse_rebuild_sha256':'0'*64},{'same_ndk_inverse_rebuild_verified':False},
          {'forward_rebuild_sha256':'0'*64},{'same_ndk_forward_rebuild_verified':False},
          {'changed_native_sources':[]},{'reviewed_changed_source_sha256':{}},
          {'all_other_native_sources_identical_to_1999':False},
          {'compute_manifest_sha256':'0'*64},{'native_source_sha256':{}},
          {'baseline_native_source_sha256':{}},{'added_gpu_jni_exports':['Java_unreviewed']},
          {'added_runtime_jni_methods':[]},{'pixel_equivalence_claimed_by_build':True},
          {'generated_header_sha256':'0'*64},{'inverse_header_sha256':'0'*64},
          {'compile_flags':identity.FLAGS+['-ffast-math']},{'link_libraries':['EGL','m']}):
        rejected(proof={**report,**update})
    for update in ({'current_sha256':'0'*64},{'current_bytes':len(nr)-1},{'baseline_sha256':'0'*64},
          {'inverse_rebuild_sha256':'0'*64},{'same_ndk_inverse_rebuild_verified':False},
          {'forward_rebuild_sha256':'0'*64},{'same_ndk_forward_rebuild_verified':False},
          {'compile_flags':identity.NR_FLAGS+['-ffast-math']},{'link_libraries':[]},{'strip_flags':[]}):
        rejected(proof={**report,'nr1955':{**report['nr1955'],**update}})
    copied=work/'negative-sources';copied.mkdir()
    proof_sources=list(report['native_source_sha256'])+['native_delta19100_baseline.json','native_delta19100_review.json','native1960/compute_dependencies1990.json']
    proof_sources += [identity.REFERENCE+'/'+name for name in report['baseline_native_source_sha256']]
    for name in proof_sources:
        target=copied/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes((source/name).read_bytes())
    identity.verify_native_sources(copied);count+=1
    for name in list(report['native_source_sha256'])+[identity.REFERENCE+'/'+n for n in report['baseline_native_source_sha256']]+['native1960/compute_dependencies1990.json','native_delta19100_baseline.json']:
        path=copied/name;old=path.read_bytes();path.write_bytes(old+b'\n')
        try:identity.verify_native_sources(copied)
        except (AssertionError,ValueError):count+=1
        else:raise AssertionError('Native/current/frozen input mutation accepted: '+name)
        finally:path.write_bytes(old)
    path=copied/'native_delta19100_review.json';old=path.read_bytes();approved=json.loads(old)
    for change in ({'reviewed_changed_source_sha256':{}},{'reviewed_changed_source_sha256':{'../unreviewed.c':'0'*64}},
                   {'reviewed_changed_source_sha256':{name:'0'*64 for name in approved['reviewed_changed_source_sha256']}},
                   {'added_gpu_jni_exports':['bad']},{'added_runtime_jni_methods':['bad']},{'added_gpu_jni_exports':[]},{'added_runtime_jni_methods':[]}):
        path.write_text(json.dumps({**approved,**change}))
        try:identity.verify_native_sources(copied)
        except (AssertionError,ValueError):count+=1
        else:raise AssertionError('Unreviewed source scope/JNI inventory accepted')
        finally:path.write_bytes(old)
    return count

def repository_content_controls(source):
    import base64,hashlib
    from unittest.mock import patch
    path=source.parent/'publication/publish19100.py'
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
    spec=importlib.util.spec_from_file_location('release_validation1997',source/'validate19100.py');gate=importlib.util.module_from_spec(spec);spec.loader.exec_module(gate)
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
    children['release_gate'].update(native_reviewed_source_delta_inverse_verified=True,native_identity_negative_assertions=1,large_content_exact_commit_verified=True)
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
    children['front_plan97']['retained97_planning_cases_on_optimized_kernel']=True
    good={'status':'passed','assertions':2*len(children),'physical_android_tested':False,'tests':children}
    gate.positive_host(good);assertions+=1
    for update in ({'status':'pending'},{'assertions':True},{'assertions':0},{'assertions':3},{'physical_android_tested':True},
            {'device_speedup_verified':True},{'tests':{**children,'release_gate':{**children['release_gate'],'native_reviewed_source_delta_inverse_verified':False}}},{'tests':{**children,'release_gate':{**children['release_gate'],'native_identity_negative_assertions':0}}},{'tests':{**children,'finish_recovery95':{**children['finish_recovery95'],'resident_policy_trace_verified':False}}},{'tests':{}},{'tests':{**children,'chain':{'status':'failed','assertions':2}}},
            {'assertions':2*(len(children)-1),'tests':{k:v for k,v in children.items() if k!='finish_recovery95'}},
            {'assertions':2*(len(children)+1),'tests':{**children,'stale':{'status':'passed','assertions':2}}}):
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
    with zipfile.ZipFile(source.parent/'dist/ULike_HQ_Texture_Online_v1.9.100.mpp') as artifact:
        native_assertions=native_negative_controls(source,artifact.read('ulike1960/runtime/libulike_gpu1960.so'),artifact.read('ulike1955/runtime/libulike_nr1955.so'),work)
    content_assertions=repository_content_controls(source)
    assertions+=native_assertions+content_assertions
    result={'status':'passed','assertions':assertions,'native_identity_negative_assertions':native_assertions,'native_reviewed_source_delta_inverse_verified':True,'large_content_exact_commit_verified':True,'repository_content_assertions':content_assertions,'physical_android_tested':False,'device_speedup_verified':False,
        'release_gate_negative_inputs_verified':True,'actual_release_validation_in_this_unit_test':False}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work),indent=2))
