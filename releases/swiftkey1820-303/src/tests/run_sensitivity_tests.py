#!/usr/bin/env python3
"""Test the production flick recognizer and settings on explicit host fakes.

Default/Standard event traces are compared to the byte-pinned published 1.8.19
production source, compiled independently with the same host fakes. Android UI,
ART, actual native timers/actors, and a clean original APK are not executed.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import re
import shutil
import subprocess
from pathlib import Path


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--source-dir',type=Path,required=True)
    parser.add_argument('--work',type=Path,required=True)
    args=parser.parse_args()
    root,work=args.source_dir.resolve(),args.work.resolve()
    work.mkdir(parents=True,exist_ok=True)
    production=root/'src/hiro/swiftkey/FlickSymbols.java'
    baseline=root/'tests/sensitivity_baseline/FlickSymbols.java'
    pin=json.loads((baseline.parent/'manifest.json').read_text())
    assert pin['version']=='1.8.19' and pin['source_commit']=='17d6c49521cb88cff518ade888a5c7532623a5ab'
    assert sha(baseline.read_bytes())==pin['sha256'] and baseline.stat().st_size==pin['bytes'],'Published baseline fixture changed'

    def run(name,command,echo=True):
        result=subprocess.run([str(x) for x in command],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        (work/(name+'.log')).write_text(result.stdout)
        if echo:print(result.stdout,end='',flush=True)
        if result.returncode:
            raise RuntimeError(name+' failed; see '+str(work/(name+'.log')))
        return result.stdout

    stubs=sorted((root/'tests/host_stubs').rglob('*.java'))
    host=sorted((root/'tests/host').rglob('*.java'))
    classes={}
    for name,source in [('current',production),('baseline',baseline)]:
        target=work/(name+'-classes')
        if target.exists():shutil.rmtree(target)
        target.mkdir()
        run('compile-'+name,['java','com.sun.tools.javac.Main','--release','8','-encoding','UTF-8','-g:none','-d',target,*stubs,source,*host])
        classes[name]=target

    def report(text):
        result=json.loads(next(line[7:] for line in text.splitlines() if line.startswith('REPORT ')))
        assert result['status']=='PASSED' and result['device_tested'] is False and result['android_instrumentation_tested'] is False
        assert result['scenarios']>0 and result['assertions']>0
        return result

    old_regressions=report(run('existing-flick',['java','-ea','-cp',classes['current'],'FlickSymbolsHostTest']))
    settings=report(run('sensitivity',['java','-ea','-cp',classes['current'],'SensitivityHostTest']))
    glide_text=run('existing-glide',['java','-ea','-cp',classes['current'],'JapaneseGlideRuntimeTest'])
    glide_match=re.search(r'PASSED (\d+) production-runtime host assertions',glide_text)
    assert glide_match,'Missing preserved glide host receipt'
    glide_assertions=int(glide_match[1])

    old_trace=run('baseline-traces',['java','-ea','-cp',classes['baseline'],'SensitivityTraceProbe','standard'],False)
    baseline_lines=old_trace.splitlines()
    assert len(baseline_lines)==672,'Baseline trace coverage drift'
    for mode in ['default','standard']:
        new_trace=run(mode+'-traces',['java','-ea','-cp',classes['current'],'SensitivityTraceProbe',mode],False)
        actual=new_trace.splitlines()
        assert len(actual)==len(baseline_lines),'Trace count changed: '+mode
        for index,(old,new) in enumerate(zip(baseline_lines,actual)):
            assert old==new,'Published behavior differs at '+mode+' case '+str(index)+': '+old+' vs '+new
    print('PASS Published 1.8.19 differential: 672 event sequences identical for default and explicit Standard',flush=True)
    result={
        'status':'PASSED',
        'scenarios':old_regressions['scenarios']+settings['scenarios']+len(baseline_lines),
        'assertions':old_regressions['assertions']+settings['assertions']+glide_assertions+len(baseline_lines)*2,
        'production_source_sha256':sha(production.read_bytes()),
        'baseline_source_sha256':pin['sha256'],
        'baseline_source_version':'1.8.19',
        'baseline_source_commit':pin['source_commit'],
        'baseline_fixture_source_matches_published_1819':True,
        'standard_sensitivity_preserved':True,
        'standard_differential_cases':len(baseline_lines),
        'standard_differential_modes':['missing preference (default)','explicit Standard'],
        'standard_differential_trace_sha256':sha(old_trace.encode()),
        'five_sensitivity_levels_verified':True,
        'preference_persistence_verified':True,
        'invalid_preference_fallback_verified':True,
        'next_gesture_uses_updated_sensitivity':True,
        'preference_changes_keep_in_flight_snapshot':True,
        'settings_dialog_cancel_and_failure_verified':True,
        'gesture_regressions_verified':True,
        'native_long_press_precedence_verified':True,
        'fullwidth_native_alternates_verified':True,
        'no_diagnostic_setting_reintroduced':True,
        'levels':[
            {'index':0,'label':'とても低い','distance_multiplier':2.0},
            {'index':1,'label':'低い','distance_multiplier':1.5},
            {'index':2,'label':'標準','distance_multiplier':1.0},
            {'index':3,'label':'高い','distance_multiplier':0.8},
            {'index':4,'label':'とても高い','distance_multiplier':0.65},
        ],
        'default_level':2,
        'existing_flick_report':old_regressions,
        'sensitivity_report':settings,
        'existing_glide_assertions':glide_assertions,
        'integrated_glide_source_sha256':sha((root/'tests/host/JapaneseGlide.java').read_bytes()),
        'device_tested':False,
        'android_instrumentation_tested':False,
        'original_apk_apply_tested':False,
        'scope':'Production FlickSymbols and its settings callbacks run on host Java fakes, plus existing JapaneseGlide Java control flow. '
                'Default/Standard are compared to pinned published 1.8.19 source with identical physical event sequences. '
                'SharedPreferences fakes test roundtrips and reinitialization; Android disk persistence, dialog rendering, ART, '
                'native engine/timers, handset feel, and clean APK patching are not executed.',
    }
    (work/'report.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
    print('PASS Sensitivity suite: '+str(result['scenarios'])+' scenarios, '+str(result['assertions'])+' assertions',flush=True)


if __name__=='__main__':
    main()
