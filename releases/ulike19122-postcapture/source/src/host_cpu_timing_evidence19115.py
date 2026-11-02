"""Evidence contract for observing existing CPU references, not timing them anew."""
RUNTIME_CONTROLS=('native_completion','native_false','native_link','java_completion','incomplete','workspace_release','duplicate_token','missing_finish','missing_strip_coverage','wrong_model','foreground_token')
BODY_CONTROLS=('extra_native_attempt','extra_copy')
COUNTS={'source_scope':(3,None),'ResidentTiming19111Test':(102,7),'CpuObserver19115Test':(100,21),'journal':(1603,74),'restart':(2,1),'backend':(225,None)}
ABI_OWNERS=('StrongNoise1958','StrongNoise1958$1','StrongNoise1958$2','StrongNoise1958$3','StrongNoise1958$4')

def positive(cpu,need):
    need(type(cpu.get('assertions')) is int and cpu['assertions']==2035,'CPU reference executed assertion count differs')
    rows=cpu.get('tests');need(type(rows) is dict and set(rows)==set(COUNTS),'CPU reference executed subtests differ')
    for name,(assertions,cases) in COUNTS.items():
        row=rows[name];need(type(row) is dict and row.get('status')=='passed','CPU reference subtest failed: '+name)
        need(type(row.get('assertions')) is int and row['assertions']==assertions,'CPU reference coverage differs: '+name)
        if cases is not None:need(type(row.get('cases')) is int and row['cases']==cases,'CPU reference cases differ: '+name)
    b=rows['backend'];need(type(b.get('scenarios')) is int and b['scenarios']==44,'CPU backend scenarios differ')
    need(type(b.get('original_regression_assertions')) is int and b['original_regression_assertions']==267,'Existing routing regression missing')
    need(b.get('controlled_jni_not_production_kernel') is True,'Controlled native completion must not claim production kernel execution')
    for name,assertions,scenarios in [('backend',102,24),('observer_absent',2,1),('stage',103,19)]:
        row=b.get(name);need(type(row) is dict and row.get('status')=='passed','CPU backend executed subtest absent')
        for key,count in [('assertions',assertions),('scenarios',scenarios)]:need(type(row.get(key)) is int and row[key]==count,'CPU backend coverage differs: '+name+'/'+key)
    for key in ('actual_native_return_executed','controlled_jni_not_production_kernel','native_false_and_link_fallback_observed','actual_java_fallback_executed','observer_failures_preserved'):
        need(b['backend'].get(key) is True,'Actual native completion/fallback proof absent: '+key)
    for key in ('actual_gpu_strong_stage','stage_token_ownership_preserved','foreground_unobserved','partial_reports_unobserved','controlled_backend_reports'):
        need(b['stage'].get(key) is True,'Actual Stage ownership proof absent: '+key)
    need(b['stage'].get('actual_jni') is False,'Separate controlled Stage must not claim combined JNI execution')
    for field,count_key,names in [('mutation_controls','mutation_count',RUNTIME_CONTROLS),('body_mutation_controls','body_mutation_count',BODY_CONTROLS)]:
        controls=b.get(field)
        need(type(b.get(count_key)) is int and b[count_key]==len(names) and type(controls) is list and len(controls)==len(names),'CPU controls missing')
        need({x.get('name') for x in controls}==set(names) and all(x.get('rejected') is True for x in controls),'CPU controls did not reject intended mutations')
        if field=='mutation_controls':need(all(type(x.get('expected_assertion')) is str and x['expected_assertion'] for x in controls),'CPU mutations lack intended-layer failures')
    abi=b.get('anonymous_and_wrapper_abi');need(type(abi) is dict and set(abi)==set(ABI_OWNERS),'Existing wrapper/callback ABI proof absent')
    need(all(type(row) is dict and row and all(type(v) is str and v.startswith('descriptor: ') for v in row.values()) for row in abi.values()),'Malformed wrapper/callback ABI proof')

def fixture():
    rows={name:dict(status='passed',assertions=assertions,**({} if cases is None else dict(cases=cases))) for name,(assertions,cases) in COUNTS.items()}
    b=rows['backend'];b.update(scenarios=44,original_regression_assertions=267,controlled_jni_not_production_kernel=True,
        mutation_count=11,body_mutation_count=2,
        mutation_controls=[dict(name=name,rejected=True,expected_assertion='parser fixture only') for name in RUNTIME_CONTROLS],
        body_mutation_controls=[dict(name=name,rejected=True) for name in BODY_CONTROLS],
        anonymous_and_wrapper_abi={name:{'parser_fixture':'descriptor: ()V'} for name in ABI_OWNERS})
    for name,assertions,scenarios in [('backend',102,24),('observer_absent',2,1),('stage',103,19)]:b[name]=dict(status='passed',assertions=assertions,scenarios=scenarios)
    b['backend'].update(**{key:True for key in ('actual_native_return_executed','controlled_jni_not_production_kernel','native_false_and_link_fallback_observed','actual_java_fallback_executed','observer_failures_preserved')})
    b['stage'].update(actual_jni=False,**{key:True for key in ('actual_gpu_strong_stage','stage_token_ownership_preserved','foreground_unobserved','partial_reports_unobserved','controlled_backend_reports')})
    return rows
