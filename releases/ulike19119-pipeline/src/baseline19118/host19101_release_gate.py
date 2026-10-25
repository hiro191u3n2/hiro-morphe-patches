#!/usr/bin/env python3
"""Negative-input tests for publication gate parsing; no release evidence mocked."""
from pathlib import Path
import argparse, copy, importlib.util, json, sys, zipfile

def native_negative_controls(source,raw,nr,smooth,work):
    import native_provenance19115,native_smooth_provenance19115
    report=json.loads((source.parent/'dist/build19101.json').read_text())['gpu_native']['optimization_provenance']
    smooth_report=json.loads((source.parent/'dist/build19101.json').read_text())['smooth_native']['optimization_provenance']
    return native_provenance19115.negative_controls(source,raw,nr,report)+native_smooth_provenance19115.negative_controls(source,smooth,smooth_report)

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

def publication_metadata_controls(source):
    """Exercise generated local metadata; these fixtures never access the network."""
    path=source.parent/'publication/publish19101.py'
    spec=importlib.util.spec_from_file_location('release_metadata115',path);pub=importlib.util.module_from_spec(spec);spec.loader.exec_module(pub)
    notes='ULike v1.9.118: 撮影前のアイドル時間にtile8を検証し、既存認定は同一指紋のみ復元する。全画素一致、tile8の10%、連結の5%以上の速度条件を守る。実機の保存2,000msは未確認。'
    state={'manifest':{'version':pub.PREVIOUS,'description':'ULike：v1.9.117（5.6.2／740）\nTikTok：既存のまま'},'log':'# 1.0.284 (2026-10-24)\n\n* **ULike:** Previous release\n\n# 1.0.283 (2026-10-24)\n\n* **TikTok:** Historical update\n'}
    feed,log=pub.metadata(state,'https://example.invalid/metadata-fixture','2026-10-24T00:00:00Z',notes)
    first=pub.parsed_entries(log)[0]['bullets'][0][1]
    assert all(value in first for value in ('アイドル','tile8','10%','5%','未確認'));count=5
    assert not any(value in first for value in ('transform','マスク行','GPU処理全体10%'));count+=1
    assert log.endswith(state['log']) and feed['version']==pub.BUNDLE and 'ULike：v1.9.118' in feed['description'];count+=1
    previous={'ulike_version':pub.PREVIOUS_APP,'source_sha256':pub.SINGLE_BASE_SHA256,'approved_lineage_version':'1.8.8','withdrawn_versions':['1.9.17'],'native_installer_preserved_rows':9,'native_installer_updated_rows':3,'smooth_nr_native_library_sha256':'2c7634030f2e6c2177c033f931f6b7976c76da465fdada29ea5023ce088f4974','smooth_nr_native_library_bytes':33128}
    qa={name:{'sha256':str(i)*64,'bytes':100+i} for i,name in enumerate(('gpu_native','nr_native','smooth_native'),1)}
    expected={pub.SINGLE:{'sha256':'a'*64,'bytes':1000}}
    policy=pub.new_policy(json.dumps(previous),qa,expected)
    assert policy['native_installer_preserved_rows']==9 and policy['native_installer_updated_rows']==3 and policy['native_installer_total_rows']==12;count+=1
    for prefix,name in [('gpu','gpu_native'),('nr','nr_native'),('nr1955','nr_native'),('smooth_nr','smooth_native')]:
        assert policy[prefix+'_native_library_sha256']==qa[name]['sha256'] and policy[prefix+'_native_library_bytes']==qa[name]['bytes'];count+=1
    history=policy['inherited_release_evidence19101']['evidence']
    for key in ('smooth_nr_native_library_sha256','smooth_nr_native_library_bytes','native_installer_preserved_rows','native_installer_updated_rows'):
        assert history[key]==previous[key];count+=1
    assert policy['device_save_speed_measured'] is False and policy['gpu_execution_on_physical_android'] is False;count+=1
    assert policy['experimental_gpu_helper_packaged'] is False;count+=1
    assert policy['experimental_gpu_auto_adoption'] is False;count+=1
    assert policy['regional_positive_proofs_scope']=='research_only_not_distributed_adoption';count+=1
    authorized={'publication_authorized_by_user':True,'needs_explicit_release_confirmation':False,
                'standing_publication_instruction':True,'automatic_review_blocked':True}
    pub.require_explicit_release_authorization(authorized);count+=1
    for changed in ({'publication_authorized_by_user':None},
                    {'publication_authorized_by_user':False},
                    {'needs_explicit_release_confirmation':True},
                    {'standing_publication_instruction':False}):
        try:pub.require_explicit_release_authorization({**authorized,**changed})
        except RuntimeError:count+=1
        else:raise AssertionError('Unconfirmed release passed publication gate')
    return count

def test(source,work,jdk=None,ndk=None,parser_only=False):
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
    import source_inverse19117
    children['source_inverse117'].update(schema='ulike19117-source-inverse-proof-v1',reviewed_files=list(source_inverse19117.REVIEWED),
        mutations_rejected=len(source_inverse19117.REVIEWED)+1,manifest_sha256=source_inverse19117.MANIFEST_SHA256,
        published_archive_sha256=source_inverse19117.ARCHIVE_SHA256)
    import source_inverse19118
    children['source_inverse118'].update(schema='ulike19118-source-inverse-proof-v1',reviewed_files=list(source_inverse19118.REVIEWED),
        mutations_rejected=2*len(source_inverse19118.REVIEWED)+1,manifest_sha256=source_inverse19118.MANIFEST_SHA256,
        published_archive_sha256=source_inverse19118.PUBLISHED117_ARCHIVE_SHA256)
    cache_cases=[]
    for scenario,profile,status in (('full',7,'restored'),('driver-changed',4,'identity-rejected'),
            ('source=3579d27380ef0ce17650c470d77af2747d6cf4d2b80812cd26d46e7deafd9c13',4,'identity-rejected'),
            ('app-5.6.3',4,'identity-rejected'),('missing-fast',4,'restored'),('reject',4,'restored'),('cancel',4,'restored')):
        row={'scenario':scenario,'restore':{'status':status,'profile':profile,'assertions':2}}
        if status=='restored':row['write']={'status':'written','assertions':2}
        cache_cases.append(row)
    children['cache_preservation118'].update(compute_fingerprint='320003f2dea928712ae437b355969564c4934df9c719f5a2b88569b499129b1b',
        compute_inputs=109,expected_gpu_native_sha256='b0d512633e3eb08a1ac36c59f77dd6bd7c6cc3e35486f4f4dfc16ca59ccff170',
        built_native_checked=False,device_save_speed_verified=False,assertions=35,cases=cache_cases)
    children['resident_fair_plan116'].update(schema='ulike19116-resident-fair-plan-v1',assertions=11)
    children['finish_recovery95'].update(resident_policy_trace_verified=True)
    children['finish_recovery95'].update(wide_restore_status_verified=True,no_extra_gpu_dispatch_verified=True)
    children['timing'].update(**{key:True for key in ('recent_history_bounded16_verified','recent_history_success_only_verified','recent_history_exact_groups_verified','recent_history_lock_order_verified')})
    children['release_gate'].update(publication_metadata115_assertions=19,native_optimization115_provenance_verified=True,native_optimization115_negative_assertions=1,large_content_exact_commit_verified=True)
    children['gpu_math'].update(local_luma_ratio_preparation_only=False,shader96=True,only_guarded_partial_predicates_changed=False,published95_records=1016,
        published95_reads=18240,published95_argb_differences=0,published95_confidence_differences=0,legacy_preprocessor_programs=60,
        focused_gles={'actual_gles_execution':True,'cutoff_proof':{'unsafe_rejections':0,'final_contribution_differences':0}})
    children['gpu_math'].update(historical_contract_scope='restored114_to113_to112',current_shader_executed19114=True)
    children['strong_opt113'].update(whole_output_differences=0,confidence_differences=0,trace_intermediate_differences=0,negative_controls=3,per_pixel_repeated_divisions_removed=25,optimized_profile=6,unchanged_standard_profile=4,unchanged_tiled_profile=7,unchanged_preprocessed_programs=117,adaptive_gpu_speed_gate_percent=10)
    children['strong_opt113'].update(structural_negative_control_scope='restored113_source',preprocessed_contract_scope='restored113_against112',current_shader_executed19114=True)
    children['front_opt113'].update(schema='ulike19113-front-native-proof-v1',baseline_version='1.9.112',target_device_gain_claimed=False,
        architectures={name:dict(exact_output=True,raw_bits_exact=True,cases={'pixels':{'pixel_cases':320},'safeguards':{'safeguard_cases':32768},'pairs':{'pair_cases':512}},negative_controls=['lag-numerator','lag-denominator'],benchmarks={'uncached':{},'cached':{}}) for name in ('host','arm64')})
    children['strong_opt114'].update(preprocessed_contract_scope='restored114_source',structural_negative_control_scope='restored114_source',assertions=4299,whole_output_differences=0,confidence_differences=0,trace_intermediate_differences=0,shared_memory_bytes_added=0,negative_controls=4,fixtures=18,compared_pixels=6997651,compared_confidence_or_trace_words=566981,optimized_profiles=[4,6],unchanged_tiled_profile=7,unchanged_preprocessed_programs=114,per_opaque_active_pixel_repeated_divisions_removed_maximum=28)
    children['front_opt114'].update(schema='ulike19114-front-native-proof-v1',baseline_version='1.9.113',target_device_gain_claimed=False,architectures=copy.deepcopy(children['front_opt113']['architectures']))
    children['front_opt114'].update(alias_contract=dict(arithmetic_source_scope='restored114_source',historical_negative_scope19115='restored114_source',current_production_callsites_audited19115=True,current_front_source_inverse_verified19115=True,production_call_sites=4,only_disjoint_workspace_arrays=True,arithmetic_source_unchanged=True,negative_controls=2))
    for row in children['front_opt114']['architectures'].values():row['cases']['coefficients']={'coefficient_cases':8192}
    children['finish_opt114'].update(compared_pixels=71535760,constant_pixels=6456354,gpu_policy_words=26320896,extra_image_buffers=0,extra_mask_arrays=0,published113_reference_sha256='5905cc2f0b3cb2327309788bb5476c40477c580703fc0944b4353c3a7de9f397')
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
    children['finish_memory92'].update(assertions=297,
        queue=dict(status='passed',assertions=29,actual_qualification_queue=True,physical_android_tested=False),
        finish=dict(status='passed',assertions=114,actual_finish_graph=True,full_pixel_comparator=True,native_transport_mocked=True,physical_android_tested=False),
        route=dict(status='passed',assertions=40,actual_route_control=True,native_transport_mocked=True,physical_android_tested=False),
        historical_finish111=dict(status='passed',assertions=114,actual_finish_graph=True,full_pixel_comparator=True,native_transport_mocked=True,physical_android_tested=False),
        restored_chain111_sha256='8da03b9ae3d770133f22e75defd555d8696e31ceac53955bfb86fa54063c4847',
        historical_source_sha256={
            'tests1992/finish_memory/FinishRoute1992Test.java':'64c598d2f79cdeb07f602f926062c8deb5398de1b5359a23b01f1fed5909b982',
            'tests1992/finish_memory/FinishMemory1992Test.java':'471b7e4ae164abf9c702dfe1e4b460dcc7e0f881e053a4698e36dc1675d16f6b',
            'host_finish_memory1992.py':'a63e177685a3a39d603f4003466df80cf4677ddb7e3f06a61549df8f9a6cd7c5'})
    children['memory97'].update(assertions=1268,device_speedup_verified=False,tests=[
        dict(status='passed',assertions=832),dict(status='passed',assertions=100),dict(status='passed',assertions=10),
        copy.deepcopy(children['finish_memory92']),dict(status='passed',assertions=29)])
    children['resident_finish19112'].update(assertions=492,cases=17,compared_pixels=1384202,
        source_inverse=dict(status='passed',assertions=28,negative_controls=26,
            original_source_sha256={'GpuChain1961.java':'8da03b9ae3d770133f22e75defd555d8696e31ceac53955bfb86fa54063c4847',
                'GpuResident1976.java':'c7d367ed3cf20fae706807d8d1801405213b2c6d41da2d655e1a700f83fe5f9e'},
            resident_finish_source_inverse19112=True,current_source_executed_separately=True),
        profile=dict(status='passed',assertions=114,cases=6,current_cases=5,faulty_cases=1,
            foreground_profile_toctou_rejected=True,faulty_guard_removal_reproduces_128=True,stable_profiles_preserved=True,
            preflight_closed_on_all_paths=True,actual_foreground_producer_executed=True,controlled_peer_fixture=True,
            native_tested=False,physical_android_tested=False,device_speedup_verified=False,
            current=dict(status='passed',assertions=45,cases=5,faulty_guard_removed=False),
            faulty=dict(status='passed',assertions=9,cases=1,faulty_guard_removed=True)))
    children['resident_finish19112']['profile'].update(
        **{key:True for key in ('historical_producer_pin_preserved19115','current_foreground_copy_sequence_preserved19115','existing_two_cpu_reference_trials_preserved19115','current_shape_profile_toctou_rejected19115','faulty_shape_guard_removal_reproduced19115')},
        producer_structure_negative_controls19115=5,historical_producer_sha25619115='c3aaef4eb23a532bc0af2377266a4951fe39779533fce1d0d9cfb615f2618635',
        shape_current19115=dict(status='passed',assertions=44,cases=4,faulty_guard_removed=False),shape_faulty19115=dict(status='passed',assertions=11,cases=1,faulty_guard_removed=True))
    children['resident_plan19112'].update(assertions=9608,planner_cases=4000,
        published111_sha256='a10406bc6f19255decf3426aafb46254229d2d73e309502a44348898a7744368',
        source_inverse=dict(status='passed',assertions=6,negative_controls=5,resident_plan_source_inverse19112=True,
            current_source_executed_separately=True,original_source_sha256={'QualityPipeline1932.java':'a10406bc6f19255decf3426aafb46254229d2d73e309502a44348898a7744368'}))
    children['strong_cancellation19112'].update(assertions=84,fixture_cases=70,cancellation_cases=18,cancelled_qualifiers=4,inverse_mutation_negatives=6,
        current=dict(status='passed',assertions=31,fixtures=70,cancelled_cases=18,corpus_sha256='9b4e7a866ea1651481739065b78f549ec10e32c0e435bf26f8aae3aae67521ea',comparison_sha256='ff6f9ea90013c79a817b585a0607d7e054cbc877c0680331c310074c970e3915'),
        baseline=dict(status='passed',assertions=11,fixtures=70,cancelled_cases=0,corpus_sha256='9b4e7a866ea1651481739065b78f549ec10e32c0e435bf26f8aae3aae67521ea',comparison_sha256='ff6f9ea90013c79a817b585a0607d7e054cbc877c0680331c310074c970e3915'),
        controlled=dict(status='passed',assertions=29,cancelled_qualifiers=4,partial_proofs_written=0,completed_oracle_calls=70,completed_gpu_submits=80),
        observed_interrupt={'published111':dict(status='passed',assertions=3,cancelled=False,completed_after_interrupt=True),
            'current112':dict(status='passed',assertions=3,cancelled=True,completed_after_interrupt=False)})
    children['diagnostics111'].update(assertions=2700,cases=57,fixture_classes_in_runtime=False,legacy_cpu_tail_zero_words19115=612,legacy_zero_extension_words19115=252,legacy_memory_capacity_checks19115=18)
    children['resident_timing111'].update(assertions=108,tests={
        'source_inverse':dict(status='passed',assertions=12,negative_controls=8,
            original_source_sha256={'GpuResident1976.java':'972971abc70b62c9f9016d1b1c8dae6ef8e7e6dd3505557caada55a0164def1b',
                'QualityPipeline1932.java':'6590b305a35d65808db0e0c163cbed2c71fb321fb262c14817c93512616425bd',
                'GpuChain1961.java':'c99ef40ed8e3b6755a88d6c293f8c39b88808d9812204546a8952cc174126ead',
                'GpuStrong1960.java':'eec980d27305a7167bb907d1d1a73b31d178a80a62c2d965946bfdd935845fb6'},
            resident_timing_source_inverse19111=True,current_source_executed_separately=True),
        'observer':dict(status='passed',assertions=96,cases=7,physical_android_tested=False,
            **{k:True for k in gate.CURRENT_PROOF_KEYS['resident_timing111'] if k!='resident_timing_source_inverse19111'})})
    children['strong_counts111'].update(assertions=57,original_regression_assertions=267,actual_jni=False,tests={
        'counts':dict(status='passed',assertions=51,cases=11,actual_jni=False,physical_android_tested=False,
            **{k:True for k in ('production_dispatcher_executed','actual_commit_counts','foreground_trace_isolated','cpu_oracle_excluded','optional_sink_failures_preserved','ownership_preserved')}),
        'absent':dict(status='passed',assertions=3,missing_optional_sink_preserved=True),
        'incompatible':dict(status='passed',assertions=3,missing_optional_sink_preserved=True)})
    children['strong_admission110'].update(assertions=266,pixel_equivalence_tested=False,tests={
        'strong_native':dict(status='passed',assertions=252,cases=19,rounds=2,physical_android_tested=False,pixel_equivalence_tested=False,
            actual_strong_java_executed=True,actual_memory_java_executed=True,actual_native_pools_executed=True,actual_gpu_lease_executed=True,
            **{key:True for key in gate.CURRENT_PROOF_KEYS['strong_admission110'] if key!='strong_admission_original_budget19110'}),
        'original_budget':dict(status='passed',assertions=13,shader_source_inverse19113_sha256='4f1bb151e3e49636d71db2abb6dad1b89d1a8572317af525069c306515605e36',shader_source_inverse19113_negative_controls=3,memory_transport_source_unchanged19113=True,original_sha256='27fc632074e7adaf45e6f55422167ed25b383d3a3695a5f8895a3d212ab429e5',
            reviewed_inverse=True,current_source_executed_separately=True,strong_admission_original_budget19110=True),
        'published108_regression':dict(status='passed',assertions=1,reproduced=True,source_sha256={
            'GpuNoise1960.java':'27fc632074e7adaf45e6f55422167ed25b383d3a3695a5f8895a3d212ab429e5',
            'GpuStrong1960.java':'667ae2ef7774ae7e152134e91ee79702cac228ac647f14f84c79ca01b91173df'})})
    children['strong_diagnostics110'].update(assertions=247,cases=36,fixture_classes_in_runtime=False)
    children['copy_admission110'].update(assertions=2231,tests={
        name:dict(status='passed',assertions=count,physical_android_tested=False,**extra)
        for name,count,extra in (('baseline',18,dict(cases=1,native_transport_mocked=True)),
            ('current',2195,dict(cases=27,native_transport_mocked=True)),('copy_rotation',13,{}),
            ('source_inverse',5,dict(negative_controls=3,original_sha256='8bd4269eb0222f86388c71af034bca0657e8fffc0b563c77a0d1f5832d781ac4',
                resident_source_inverse19110=True,current_source_executed_separately=True)))})
    children['memory108'].update(assertions=1000,shader_source_inverse19113_sha256='4f1bb151e3e49636d71db2abb6dad1b89d1a8572317af525069c306515605e36',shader_source_inverse19113_negative_controls=3)
    children['resident_memory108'].update(assertions=400,cases=26)
    children['gpu_math']['all_60_legacy_programs_unchanged']=False
    children['regional107'].update(assertions=1380012,arithmetic_negative_controls=4,device_mismatch_reproduced=False,
        regional_compared_words=929118,exact_mismatches=0,physical_log_values_are_not_test_inputs=True,
        full_strong_regression_required=['strong_trace106','strong_math105'],coverage=dict(gate.REGIONAL107_COVERAGE),
        rounding_ties={'even_lower':1,'even_upper':1,'underflow':1},java={'cases':225244,'strictfp':True,'canonical_nan':True},
        gles={'status':'passed','actual_gles_execution':True,'cases':225244,'invalid_domain_cases':10,'weight_gate_cases':392,
            'negative_controls':[{'name':name,'mismatches':count} for name,count in (('output_lsb',225244),('reversed_horizontal_weights',165402),('truncate_input_mantissa',81276),('scale_ties_away_instead_of_even',2684))],
            'regional_groups':[{'width':w,'height':h,'first_grid_y':first,'points':points,'planes':3,'variants':2} for w,h,first,points in gate.REGIONAL107_GEOMETRIES],
            'renderer':'parser-fixture','gl_version':'parser-fixture','helper_sha256':'1'*64},corpus_sha256='1'*64)
    children['cache107'].update(assertions=446,native_fingerprint_old='1'*64,native_fingerprint_new='2'*64,
        cache={'status':'passed','assertions':143,'proof_families':8,'corpus_families':4},
        ingress={'status':'passed','assertions':168,'generated_models':84,'invalid_binary32_classes':9},
        ingress_callsites={'GpuStrong1960.java':9,'GpuAnalysis1961.java':2,'GpuStrongTuning1975.java':2,'GpuStrongRouting1978.java':1,'GpuStrongLayout1976.java':1})
    children['cache107'].update(legacy_ingress_sites19115=8,regional_candidate_ingress_sites19115=1,legacy_ingress_negative_controls19115=8,regional_candidate_ingress_negative_controls19115=5,
        candidate_ingress19115=dict(status='passed',assertions=112,invalid_binary32_classes=9,readiness_required_combinations=4,invalid_cases=36),
        regional_candidate_ingress_scope19115='research_wrapper_cached_readiness_before_session_open_upload',
        legacy_ingress_method_sha25619115={name:'a'*64 for name in ('regionalEvidence19107','initialize','initialize1971','initializeResident19110','ephemeral','ephemeral1973','trace19106','ephemeral1971')})
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
    import host_evidence19115
    children.update(host_evidence19115.parser_fixtures())
    import regional_distribution19115
    children['runtime_version']['distributed_regional_dex']=dict(regional_distribution19115.EXPECTED)
    good={'status':'passed','assertions':sum(row['assertions'] for row in children.values()),'physical_android_tested':False,'tests':children}
    gate.positive_host(good);assertions+=1
    for key,value in regional_distribution19115.EXPECTED.items():
        for bad in (None,True,False,'',0):
            if type(bad) is type(value) and bad==value:continue
            child={**children['runtime_version'],'distributed_regional_dex':{**regional_distribution19115.EXPECTED,key:bad}}
            rejected(lambda child=child:gate.positive_host({**good,'tests':{**children,'runtime_version':child}}))
    assertions+=host_evidence19115.malformed_controls(good,gate.positive_host)
    # Every new timing/display proof stays mandatory independently of totals.
    for key in ('legacy_cpu_tail_zero_words19115','legacy_zero_extension_words19115','legacy_memory_capacity_checks19115'):
        for bad in (None,True,0):
            child={**children['diagnostics111'],key:bad}
            rejected(lambda child=child:gate.positive_host({**good,'tests':{**children,'diagnostics111':child}}))
    for key in gate.CURRENT_PROOF_KEYS['diagnostics111']:
        for value in (False,1,None):
            edited=copy.deepcopy(good);edited['tests']['diagnostics111'][key]=value
            rejected(lambda edited=edited:gate.positive_host(edited))
    for key,value in [('assertions',1817),('assertions',True),('cases',56),('cases',True),
            ('fixture_classes_in_runtime',True),('physical_android_tested',True),('device_speedup_verified',True)]:
        edited=copy.deepcopy(good);edited['tests']['diagnostics111'][key]=value
        if key=='assertions' and type(value) is int:
            edited['assertions']=sum(row['assertions'] for row in edited['tests'].values())
        rejected(lambda edited=edited:gate.positive_host(edited))
    for key in ('assertions','cases','fixture_classes_in_runtime','physical_android_tested',*gate.CURRENT_PROOF_KEYS['diagnostics111']):
        edited=copy.deepcopy(good);del edited['tests']['diagnostics111'][key]
        rejected(lambda edited=edited:gate.positive_host(edited))
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
    for group,key,value in [('strong_opt114','preprocessed_contract_scope','current115'),('strong_opt114','structural_negative_control_scope','current115'),('strong_opt114','whole_output_differences',1),('strong_opt114','confidence_differences',True),('strong_opt114','shared_memory_bytes_added',1),('strong_opt114','negative_controls',3),('strong_opt114','optimized_profiles',[6]),('strong_opt114','compared_pixels',0),('strong_opt113','structural_negative_control_scope','current115'),('strong_opt113','preprocessed_contract_scope','current114'),('strong_opt113','current_shader_executed19114',False),('front_opt114','baseline_version','1.9.112'),('front_opt114','architectures',{}),('front_opt114','alias_contract',{}),('front_opt114','target_device_gain_claimed',True),('finish_opt114','compared_pixels',True),('finish_opt114','gpu_policy_words',0),('finish_opt114','extra_mask_arrays',1),('finish_opt114','published113_reference_sha256','0'*64)]:
        edited=copy.deepcopy(good);edited['tests'][group][key]=value
        rejected(lambda edited=edited:gate.positive_host(edited))
    # Raw trace evidence is mandatory independently of generic flags/accounting.
    for part,key,bad in [('wrapper','actual_wrapper_jni',False),('wrapper','assertions',9),('wrapper','physical_android_tested',True),('cpu','records',78),('cpu','near_half',0),('cpu','nlm_on',0),('cpu','nlm_off',0),('cpu','sparse',0),('gpu','raw_differences',1),('gpu','negative_controls',4),('gpu','compared_checkpoint_words',0),('gpu','compared_row_pixels',261849)]:
        edited=copy.deepcopy(good);edited['tests']['strong_trace106'][part][key]=bad
        rejected(lambda edited=edited:gate.positive_host(edited))
    for key,bad in [('device_mismatch_reproduced',True),('source_arithmetic',{}),('boundary_rows',[]),('gl_environment',''),('trace_corpus_sha256','')]:
        edited=copy.deepcopy(good);edited['tests']['strong_trace106'][key]=bad
        rejected(lambda edited=edited:gate.positive_host(edited))
    for key,bad in [('schema','old-schema'),('assertions',10),('assertions',True)]:
        edited=copy.deepcopy(good);edited['tests']['resident_fair_plan116'][key]=bad
        rejected(lambda edited=edited:gate.positive_host(edited))
    for update in ({'status':'pending'},{'assertions':True},{'assertions':0},{'assertions':3},{'physical_android_tested':True},
            {'device_speedup_verified':True},{'tests':{**children,'release_gate':{**children['release_gate'],'native_optimization115_provenance_verified':False}}},{'tests':{**children,'release_gate':{**children['release_gate'],'native_optimization115_negative_assertions':0}}},{'tests':{**children,'finish_recovery95':{**children['finish_recovery95'],'resident_policy_trace_verified':False}}},{'tests':{}},{'tests':{**children,'chain':{'status':'failed','assertions':2}}},
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
        if value is remove105:
            if type(row) is list:row.pop(path[-1])
            else:row.pop(path[-1],None)
        else:row[path[-1]]=value
        totals=[r.get('assertions') if type(r) is dict else None for r in edited.values()]
        # Let malformed counters reach the real parser; do not fail in the
        # negative-control setup before the validation under test executes.
        total=sum(totals) if all(type(value) is int for value in totals) else good['assertions']
        report={**good,'tests':edited,'assertions':total}
        gate.positive_host(report)
    remove105=object()
    # The .111 observer, dispatcher and persistence evidence must be present
    # down to each executed subtest. Numeric booleans are never evidence.
    def proof_paths111(row,prefix):
        for key,value in (enumerate(row) if type(row) is list else row.items()):
            path=prefix+(key,)
            yield path,value
            if type(value) in (dict,list):yield from proof_paths111(value,path)
    for group in ('resident_timing111','strong_counts111','diagnostics111','resident_plan19112','strong_cancellation19112','resident_finish19112','finish_memory92','memory97'):
        rejected(lambda group=group:changed105((group,),remove105))
        for path,value in proof_paths111(children[group],(group,)):
            rejected(lambda path=path:changed105(path,remove105))
            if type(value) is bool:bad=1 if value else 0
            elif type(value) is int:bad=True
            elif type(value) is str:bad=value+'-unverified'
            else:continue
            rejected(lambda path=path,bad=bad:changed105(path,bad))
    for path,value in [
            (('resident_timing111','tests','source_inverse','assertions'),11),
            (('resident_timing111','tests','source_inverse','negative_controls'),7),
            (('resident_timing111','tests','observer','assertions'),95),
            (('resident_timing111','tests','observer','cases'),6),
            (('strong_counts111','tests','counts','assertions'),50),
            (('strong_counts111','tests','counts','cases'),10),
            (('strong_counts111','tests','absent','assertions'),2),
            (('strong_counts111','tests','incompatible','assertions'),2),
            (('strong_counts111','original_regression_assertions'),266)]:
        rejected(lambda path=path,value=value:changed105(path,value))
    for path,value in [
            (('finish_memory92','assertions'),296),(('finish_memory92','queue','assertions'),28),
            (('finish_memory92','finish','assertions'),113),(('finish_memory92','route','assertions'),39),
            (('finish_memory92','historical_finish111','assertions'),113),
            (('memory97','assertions'),1267),(('memory97','tests',3,'historical_finish111','assertions'),113),
            (('resident_plan19112','assertions'),9607),(('resident_plan19112','planner_cases'),3999),
            (('resident_plan19112','source_inverse','assertions'),5),(('resident_plan19112','source_inverse','negative_controls'),4),
            (('resident_finish19112','assertions'),431),(('resident_finish19112','cases'),16),
            (('resident_finish19112','compared_pixels'),1384201),(('resident_finish19112','source_inverse','negative_controls'),25),
            (('resident_finish19112','profile','assertions'),53),(('resident_finish19112','profile','current','cases'),4),
            (('resident_finish19112','profile','faulty','faulty_guard_removed'),False),
            (('strong_cancellation19112','controlled','partial_proofs_written'),1),
            (('strong_cancellation19112','assertions'),83),
            (('strong_cancellation19112','current','assertions'),30),
            (('strong_cancellation19112','baseline','assertions'),10),
            (('strong_cancellation19112','current','cancelled_cases'),17),
            (('strong_cancellation19112','baseline','corpus_sha256'),'0'*64),
            (('strong_cancellation19112','observed_interrupt','current112','cancelled'),False)]:
        rejected(lambda path=path,value=value:changed105(path,value))
    # Each .110 group, boolean proof, and executed subtest is mandatory even
    # when the caller recomputes the overall assertion total consistently.
    for group in ('strong_admission110','copy_admission110','strong_diagnostics110'):
        rejected(lambda group=group:changed105((group,),remove105))
        for key in gate.CURRENT_PROOF_KEYS[group]:
            rejected(lambda group=group,key=key:changed105((group,key),remove105))
    paths110=[('strong_admission110',key) for key in ('tests','physical_android_tested','pixel_equivalence_tested')]
    paths110 += [('strong_admission110','tests','strong_native',key) for key in (
        'status','assertions','cases','rounds','physical_android_tested','pixel_equivalence_tested',
        'actual_strong_java_executed','actual_memory_java_executed','actual_native_pools_executed','actual_gpu_lease_executed')]
    paths110 += [('strong_admission110','tests','original_budget',key) for key in (
        'status','assertions','original_sha256','reviewed_inverse','current_source_executed_separately','strong_admission_original_budget19110')]
    paths110 += [('strong_admission110','tests','published108_regression',key) for key in ('status','assertions','reproduced','source_sha256')]
    paths110 += [('strong_admission110','tests','strong_native',key) for key in gate.CURRENT_PROOF_KEYS['strong_admission110'] if key!='strong_admission_original_budget19110']
    paths110 += [('strong_diagnostics110',key) for key in ('assertions','cases','fixture_classes_in_runtime','physical_android_tested')]
    paths110 += [('copy_admission110','tests')]
    paths110 += [('copy_admission110','tests',name,key)
        for name in ('baseline','current','copy_rotation') for key in ('status','assertions','physical_android_tested')]
    paths110 += [('copy_admission110','tests',name,key)
        for name in ('baseline','current') for key in ('cases','native_transport_mocked')]
    paths110 += [('copy_admission110','tests','source_inverse',key) for key in (
        'status','assertions','negative_controls','original_sha256','resident_source_inverse19110','current_source_executed_separately')]
    for path in paths110:
        for value in (None,remove105):rejected(lambda path=path,value=value:changed105(path,value))
    for path,value in [(('strong_admission110','tests','strong_native','rounds'),1),(('strong_admission110','tests','strong_native','rounds'),True),
            (('strong_admission110','tests','strong_native','assertions'),251),
            (('strong_admission110','tests','strong_native','cases'),18),(('strong_admission110','tests','strong_native','cases'),True),
            (('strong_admission110','tests','strong_native','pixel_equivalence_tested'),True),
            (('strong_admission110','tests','original_budget','original_sha256'),'0'*64),(('strong_admission110','tests','original_budget','assertions'),9),
            (('strong_admission110','tests','original_budget','reviewed_inverse'),1),
            (('strong_admission110','tests','published108_regression','reproduced'),1),
            (('strong_admission110','tests','published108_regression','source_sha256','GpuStrong1960.java'),'0'*64),
            (('strong_diagnostics110','assertions'),246),(('strong_diagnostics110','assertions'),True),
            (('strong_diagnostics110','cases'),35),(('strong_diagnostics110','cases'),True),
            (('strong_diagnostics110','fixture_classes_in_runtime'),True),
            (('copy_admission110','tests','baseline','cases'),0),(('copy_admission110','tests','current','cases'),26),
            (('copy_admission110','tests','current','cases'),True),(('copy_admission110','tests','current','native_transport_mocked'),False),
            (('copy_admission110','tests','current','assertions'),2194),(('copy_admission110','tests','copy_rotation','assertions'),12),
            (('copy_admission110','tests','source_inverse','assertions'),4),(('copy_admission110','tests','source_inverse','negative_controls'),2),
            (('copy_admission110','tests','source_inverse','negative_controls'),True),
            (('copy_admission110','tests','source_inverse','original_sha256'),'0'*64),
            (('cache107','ingress_callsites','GpuStrong1960.java'),7)]:
        rejected(lambda path=path,value=value:changed105(path,value))
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
    profile115_paths=[('resident_finish19112','profile',key) for key in ('historical_producer_pin_preserved19115','current_foreground_copy_sequence_preserved19115','existing_two_cpu_reference_trials_preserved19115','current_shape_profile_toctou_rejected19115','faulty_shape_guard_removal_reproduced19115','producer_structure_negative_controls19115','historical_producer_sha25619115')]
    profile115_paths += [('resident_finish19112','profile',row,key) for row in ('shape_current19115','shape_faulty19115') for key in ('status','assertions','cases','faulty_guard_removed')]
    for path in profile115_paths:
        for value in (None,remove105):rejected(lambda path=path,value=value:changed105(path,value))
    cache_paths=[('cache107','cache',key) for key in ('status','assertions','proof_families','corpus_families')]
    cache_paths += [('cache107','ingress',key) for key in ('status','assertions','generated_models','invalid_binary32_classes')]
    cache_paths += [('cache107','ingress_callsites',key) for key in children['cache107']['ingress_callsites']]
    cache_paths += [('cache107',key) for key in ('legacy_ingress_sites19115','regional_candidate_ingress_sites19115','legacy_ingress_negative_controls19115','regional_candidate_ingress_negative_controls19115','regional_candidate_ingress_scope19115','legacy_ingress_method_sha25619115')]
    cache_paths += [('cache107','candidate_ingress19115',key) for key in children['cache107']['candidate_ingress19115']]

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
    for group,key,bad108 in [('release_gate','publication_metadata115_assertions',0),('release_gate','publication_metadata115_assertions',True),('memory108','shader_source_inverse19113_negative_controls',2),('memory108','shader_source_inverse19113_sha256','0'*64),('memory108','assertions',999),('memory108','assertions',True),('resident_memory108','assertions',399),('resident_memory108','cases',25),('resident_memory108','cases',True),('release_gate','native_optimization115_provenance_verified',False),('release_gate','native_optimization115_negative_assertions',0)]:
        rejected(lambda group=group,key=key,bad108=bad108:changed105((group,key),bad108))
    for path,value in [(('strong_admission110','tests','original_budget','shader_source_inverse19113_negative_controls'),2),(('strong_admission110','tests','original_budget','shader_source_inverse19113_sha256'),'0'*64),(('strong_admission110','tests','original_budget','memory_transport_source_unchanged19113'),False),(('strong_opt113','whole_output_differences'),1),(('strong_opt113','confidence_differences'),False),(('strong_opt113','trace_intermediate_differences'),1),(('strong_opt113','negative_controls'),2),(('strong_opt113','optimized_profile'),4),(('strong_opt113','unchanged_preprocessed_programs'),116),(('strong_opt113','adaptive_gpu_speed_gate_percent'),5),
            (('front_opt113','architectures','host','exact_output'),False),(('front_opt113','architectures','arm64','cases','pixels','pixel_cases'),319),
            (('front_opt113','architectures','arm64','cases','safeguards','safeguard_cases'),32767),(('front_opt113','architectures','arm64','cases','pairs','pair_cases'),511),
            (('front_opt113','architectures','host','negative_controls'),['lag-numerator']),(('front_opt113','target_device_gain_claimed'),True)]:
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
    if parser_only:
        return {'status':'passed','assertions':assertions,'physical_android_tested':False,'device_speedup_verified':False,'parser_only':True,'native_optimization115_provenance_verified':False,'actual_release_validation_in_this_unit_test':False}
    with zipfile.ZipFile(source.parent/'dist/ULike_HQ_Texture_Online_v1.9.118.mpp') as artifact:
        native_assertions=native_negative_controls(source,artifact.read('ulike1960/runtime/libulike_gpu1960.so'),artifact.read('ulike1955/runtime/libulike_nr1955.so'),artifact.read('ulike1958/runtime/libulike_smooth1958.so'),work)
    content_assertions=repository_content_controls(source)
    metadata_assertions=publication_metadata_controls(source)
    assertions+=native_assertions+content_assertions+metadata_assertions
    result={'status':'passed','assertions':assertions,'native_optimization115_negative_assertions':native_assertions,'native_optimization115_provenance_verified':True,'large_content_exact_commit_verified':True,'repository_content_assertions':content_assertions,'physical_android_tested':False,'device_speedup_verified':False,
        'publication_metadata115_verified':True,'publication_metadata115_assertions':metadata_assertions,'release_gate_negative_inputs_verified':True,'actual_release_validation_in_this_unit_test':False}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work),indent=2))
