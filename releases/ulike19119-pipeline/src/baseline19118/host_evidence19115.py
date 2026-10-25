"""Strict parser contract for the current .115 proofs; no fixture is release evidence."""
from copy import deepcopy
import host_cpu_evidence19115,host_cpu_timing_evidence19115

KEYS={
    'resident_cpu_timing115':('existing_cpu_oracle_calls_only','production_strong_boundary_excludes_signatures','cpu_phase3_to4_excludes_signatures','cpu_unknown_cancel_incomplete_preserved','cpu_thread_scope_and_sink_isolation','cpu_two_trial_queue_ownership','cpu_vj9_and_legacy_preserved','cpu_diagnostics_never_admit','existing_gpu_four_trials_preserved','cpu_vj9_independent_restore','actual_native_return_executed','native_false_and_link_fallback_observed','stage_token_ownership_preserved','foreground_unobserved','wrapper_body_and_abi_preserved','controlled_jni_not_production_kernel'),
    'regional_disabled115':('distributed_helper_absent_jvm','standard_routes_preserved','controlled_gpu_peers'),
    'source_inverse115':('complete_source_inverses_verified',),
    'legacy_shader115':('legacy_tokens_identical',),
    'division115':('strict_java_oracle','independent_fraction_oracle'),
    'strong_regional115':(),
    'regional_gate115':('controlled_transport_only',),
    'regional_adaptive115':('actual_selection_owners','controlled_gpu_peers'),
    'regional_reference115':('anonymous_callback_abi_preserved','published_unaffected_keys_preserved'),
    'strong_stage115':('actual_jni','actual_native_cpu','actual_new_certificate_rechecked_before_submit_and_commit','actual_prepass_stage_selected','standard_route_no_optional_compile','required_prepass_fallback_forbidden','regional_scratch_released_after_completion'),
}
KEYS.update(host_cpu_evidence19115.KEYS)
RESEARCH_GROUPS=('division115','strong_regional115','regional_gate115','regional_adaptive115','regional_reference115','strong_stage115')
RESEARCH_SCOPE='research_only_not_distributed_adoption'
PROGRAMS=[0,9,18,*range(27,61)]
PERTURBATIONS=[0,-1,1,-7,7,-65,65,-1048576,1048576]
DISABLED_CONTROLS=['missing_state_fail_open','missing_request_prepares','missing_request_reserves','namespace_changed','unproved_regional_marker','profile7_first','same_capture_profile7','missing_timing_bridge','stage_missing_fail_open']
ADAPTIVE_CONTROLS=['selection','reservation','early_qualification','final_qualification','prepared_required','quality_only','speed_only','new_namespace','recursive_cache','heavy_reservation','pending_preparation','direct_heavy_qualification','final_terminal','both_legacy_terminal','cancelled_legacy_terminal']

def positive(tests,need):
    def number(group,key,minimum,exact=False):
        value=tests[group].get(key)
        need(type(value) is int and (value==minimum if exact else value>=minimum),'Current .115 coverage differs: '+group+'/'+key)
    for group in RESEARCH_GROUPS:need(tests[group].get('execution_scope')==RESEARCH_SCOPE,'Research proof mislabeled as distributed adoption: '+group)
    number('source_inverse115','assertions',41);number('source_inverse115','mutations_rejected',14,True)
    number('source_inverse115','overlay116_mutations_rejected',8,True)
    need(tests['source_inverse115'].get('overlay116_sha256')=='c39eb936507c54d3fb27346a7a075bd08aaeaa9bf3ac0450396b06dc0d83b814','Reviewed .116 reverse overlay differs')
    host_cpu_evidence19115.positive(tests,need)
    number('legacy_shader115','assertions',37,True)
    need(tests['legacy_shader115'].get('program_ids')==PROGRAMS,'All 37 published114 programs must be compared directly')
    need(tests['legacy_shader115'].get('speed_equivalence_claimed') is False,'Legacy source identity is not a speed proof')
    for key,count in [('input_pairs',115420),('glsl_bit_comparisons',2077560),('rounding_mutant_differences',1)]:number('division115',key,count)
    need(tests['division115'].get('guess_perturbations')==PERTURBATIONS,'Independent quotient-bound perturbations missing')
    for key,count in [('compared_pixels',25326228),('compared_confidence_words',1584756),('prepass_mutant_differences',1)]:number('strong_regional115',key,count)
    frames=tests['strong_regional115'].get('full_frames')
    need(type(frames) is list and len(frames)==2,'Both complete source geometries required')
    for row,expected in zip(frames,[(3060,4080,16,12484800),(4080,3060,12,12484800)]):
        need(type(row) is dict and all(type(row.get(key)) is int and row[key]==value for key,value in zip(('width','height','bands','pixels'),expected)),'Regional full-frame/tail coverage differs')
    need(tests['strong_regional115'].get('programs')==['local%d/tile%d/pow2%d'%(local,tile,pow2) for local in (32,64,128) for pow2 in (0,1) for tile in (0,8)],'All 12 private regional program variants required')
    for group,minimum in [('regional_gate115',14),('regional_reference115',10),('regional_adaptive115',15),('regional_disabled115',9)]:
        number(group,'mutation_count',minimum)
        controls=tests[group].get('mutation_controls')
        need(type(controls) is list and len(controls)==tests[group]['mutation_count'] and len({row.get('name') for row in controls})==len(controls) and all(row.get('rejected') is True and isinstance(row.get('expected_assertion'),str) and row['expected_assertion'] for row in controls),'Current .115 intended-layer mutation rejection missing: '+group)
    cpu=tests['resident_cpu_timing115'];host_cpu_timing_evidence19115.positive(cpu,need)
    need(cpu.get('schema')=='ulike19115-resident-cpu-timing-proof-v1','CPU reference observation proof identity differs')
    for key in ('save_2000ms_achieved','extra_image_work_added'):need(cpu.get(key) is False,'CPU reference observation scope differs: '+key)
    number('regional_disabled115','assertions',233);number('regional_disabled115','scenarios',16)
    for key in ('optional_gpu_prepare_calls','regional_heavy_reservations','stage_auto_adoption_count'):number('regional_disabled115',key,0,True)
    disabled=tests['regional_disabled115'];hook=disabled.get('stage_hook')
    need({row['name'] for row in disabled['mutation_controls']}==set(DISABLED_CONTROLS),'Missing-helper fail-open controls differ')
    need(type(hook) is dict and hook.get('status')=='passed' and hook.get('exact_production_hook_extracted') is True and hook.get('full_stage_executed') is False,'Missing-helper hook execution scope differs')
    for key,value in [('assertions',13),('stage_auto_adoption_count',0),('optional_readiness_queries',0),('optional_gpu_prepare_calls',0)]:
        need(type(hook.get(key)) is int and hook[key]==value,'Missing-helper hook result differs: '+key)
    number('regional_gate115','assertions',168);number('regional_gate115','scenarios',36)
    number('regional_adaptive115','assertions',156);number('regional_adaptive115','scenarios',31)
    need({row['name'] for row in tests['regional_adaptive115']['mutation_controls']}==set(ADAPTIVE_CONTROLS),'Legacy adaptive route protection controls differ')
    number('strong_stage115','assertions',690)
    for label,cases,pixels,lifecycle in [('normal',70,2251161,6),('refused',2,793551,0)]:
        row=tests['strong_stage115'].get(label)
        need(type(row) is dict and row.get('status')=='passed' and row.get('actual_jni') is True and row.get('actual_native_cpu') is True and row.get('physical_android_tested') is False,'Actual JNI and native CPU execution missing: '+label)
        for key,value in [('cases',cases),('pixels',pixels),('lifecycle_cases',lifecycle),('revocation_cases',1)]:
            need(type(row.get(key)) is int and row[key]==value,'Stage .115 coverage differs: '+label+'/'+key)
    reference=tests['regional_reference115']
    need(isinstance(reference.get('published_callback_constructor'),str) and reference['published_callback_constructor']==reference.get('current_callback_constructor'),'Published callback ABI differs')
    for key,count in [('actual_queue',518),('caller_recycled_queue',526)]:
        row=reference.get(key)
        need(type(row) is dict and row.get('status')=='passed' and type(row.get('assertions')) is int and row['assertions']>=count and type(row.get('cases')) is int and row['cases']>=33 and row.get('physical_android_tested') is False,'Actual producer/owned-copy queue proof missing: '+key)

def parser_fixtures():
    """Artificial parser inputs, used exclusively to reject malformed evidence."""
    rows={name:dict(status='passed',assertions=2,physical_android_tested=False,device_speedup_verified=False,**{key:True for key in keys}) for name,keys in KEYS.items()}
    rows['resident_cpu_timing115'].update(schema='ulike19115-resident-cpu-timing-proof-v1',assertions=2035,save_2000ms_achieved=False,extra_image_work_added=False,tests=host_cpu_timing_evidence19115.fixture())
    rows['regional_disabled115'].update(assertions=233,scenarios=16,optional_gpu_prepare_calls=0,regional_heavy_reservations=0,stage_auto_adoption_count=0,stage_hook=dict(status='passed',assertions=13,stage_auto_adoption_count=0,optional_readiness_queries=0,optional_gpu_prepare_calls=0,exact_production_hook_extracted=True,full_stage_executed=False))
    rows['source_inverse115'].update(assertions=41,mutations_rejected=14,overlay116_mutations_rejected=8,overlay116_sha256='c39eb936507c54d3fb27346a7a075bd08aaeaa9bf3ac0450396b06dc0d83b814')
    rows['legacy_shader115'].update(assertions=37,program_ids=PROGRAMS,speed_equivalence_claimed=False)
    rows['division115'].update(input_pairs=115420,glsl_bit_comparisons=2077560,rounding_mutant_differences=1,guess_perturbations=PERTURBATIONS)
    rows['strong_regional115'].update(compared_pixels=25326228,compared_confidence_words=1584756,prepass_mutant_differences=1,programs=['local%d/tile%d/pow2%d'%(local,tile,pow2) for local in (32,64,128) for pow2 in (0,1) for tile in (0,8)],full_frames=[dict(width=w,height=h,bands=b,pixels=12484800) for w,h,b in [(3060,4080,16),(4080,3060,12)]])
    for group,count in [('regional_gate115',14),('regional_reference115',10),('regional_adaptive115',15),('regional_disabled115',9)]:rows[group].update(mutation_count=count,mutation_controls=[dict(name='parser-fixture-'+str(i),rejected=True,expected_assertion='parser fixture only') for i in range(count)])
    rows['regional_disabled115']['mutation_controls']=[dict(name=name,rejected=True,expected_assertion='parser fixture only') for name in DISABLED_CONTROLS]
    rows['regional_gate115'].update(assertions=168,scenarios=36)
    rows['regional_adaptive115'].update(assertions=156,scenarios=31,mutation_controls=[dict(name=name,rejected=True,expected_assertion='parser fixture only') for name in ADAPTIVE_CONTROLS])
    rows['regional_reference115'].update(published_callback_constructor='parser-fixture',current_callback_constructor='parser-fixture',**{key:dict(status='passed',assertions=count,cases=33,physical_android_tested=False) for key,count in [('actual_queue',518),('caller_recycled_queue',526)]})
    rows['strong_stage115'].update(assertions=690,**{label:dict(status='passed',assertions=1,cases=cases,pixels=pixels,lifecycle_cases=lifecycle,revocation_cases=1,actual_jni=True,actual_native_cpu=True,physical_android_tested=False) for label,cases,pixels,lifecycle in [('normal',70,2251161,6),('refused',2,793551,0)]})
    for group in RESEARCH_GROUPS:rows[group]['execution_scope']=RESEARCH_SCOPE
    rows.update(host_cpu_evidence19115.parser_fixtures())
    return rows

def malformed_controls(good,positive_host):
    count=0
    for group,row in parser_fixtures().items():
        for key,value in row.items():
            if key in ('status','physical_android_tested','device_speedup_verified','assertions') or key in KEYS[group]:continue
            for bad in (None,True,False,''):
                if type(bad) is type(value) and bad==value:continue
                changed=deepcopy(good);changed['tests'][group][key]=bad
                try:positive_host(changed)
                except (AssertionError,RuntimeError,ValueError,TypeError):count+=1
                else:raise AssertionError('Malformed .115 proof accepted: '+group+'/'+key)
    def leaves(value,path=()):
        if isinstance(value,dict):
            for key,child in value.items():yield from leaves(child,path+(key,))
        elif isinstance(value,list):
            for key,child in enumerate(value):yield from leaves(child,path+(key,))
        else:yield path,value
    for path,value in leaves(host_cpu_timing_evidence19115.fixture()):
        for bad in (None,True,False,''):
            if type(bad) is type(value) and bad==value:continue
            changed=deepcopy(good);row=changed['tests']['resident_cpu_timing115']['tests']
            for key in path[:-1]:row=row[key]
            row[path[-1]]=bad
            try:positive_host(changed)
            except (AssertionError,RuntimeError,ValueError,TypeError):count+=1
            else:raise AssertionError('Malformed nested CPU observation proof accepted: '+repr(path))
    return count
