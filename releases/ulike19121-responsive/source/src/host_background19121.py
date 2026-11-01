#!/usr/bin/env python3
"""Actual worker lifecycle and disabled photo-ADPF policy on deterministic API fixtures.
This checks calls/ownership/cancellation, not Android FPS or scheduling effects.
"""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess

ROOT=Path(__file__).resolve().parent

def test(source,work,jdk=None):
    source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    fixture=source/'tests19121/background'
    classes=work/'classes';classes.mkdir(exist_ok=True)
    api=work/'api';api.mkdir(exist_ok=True)
    api_sources=sorted(p for p in (source/'tests/h15/android').rglob('*.java') if p.name!='Process.java')
    tracked=[source/'SpeedWorkers1935.java',source/'PerformanceHints1952.java',Path(__file__).resolve(),
        source/'tests/SpeedWorkersHints1952Test.java',*api_sources,*sorted(fixture.rglob('*.java'))]
    pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    def run(label,args):
        result=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=60)
        (work/(label+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise AssertionError(label+'\n'+(result.stdout+result.stderr)[-12000:])
        return result.stdout
    compiler=[jdk/'bin/javac','--release','8','-encoding','UTF-8']
    run('android-fixtures-compile',compiler+['-d',api,fixture/'android/os/Process.java',*api_sources])
    run('production-compile',compiler+['-cp',api,'-d',classes,source/'SpeedWorkers1935.java',
        source/'PerformanceHints1952.java',*sorted((fixture/'com/hiro/ulike').glob('*.java')),
        source/'tests/SpeedWorkersHints1952Test.java'])
    results=[]
    for cp,count,name in [(str(classes),4,'BackgroundNoAndroid19121Test'),
            (os.pathsep.join(map(str,[classes,api])),4,'BackgroundWorker19121Test'),
            (os.pathsep.join(map(str,[classes,api])),1,'BackgroundWorker19121Test'),
            (os.pathsep.join(map(str,[classes,api])),8,'BackgroundWorker19121Test'),
            (os.pathsep.join(map(str,[classes,api])),4,'PhotoHintsDisabled19121Test'),
            (os.pathsep.join(map(str,[classes,api])),4,'SpeedWorkersHints1952Test')]:
        text=run(name+'-'+str(count),[jdk/'bin/java','-ea','-XX:ActiveProcessorCount='+str(count),'-cp',cp,'com.hiro.ulike.'+name])
        result=json.loads(text.strip().splitlines()[-1]);result['test']=name;result['host_processor_count']=count;results.append(result)
    if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:
        raise AssertionError('Tested sources changed during host checks')
    result=dict(status='passed',assertions=sum(r['assertions'] for r in results),tests=results,source_sha256=pins,
        negative_controls=dict(android_api_absent=True,priority_api_denied=True,delegate_exception=True,
            producer_interruption=True,optional_hint_failure=True,cached_adpf_api_disabled=True),
        physical_android_tested=False,device_speedup_verified=False,
        fixture_scope='Public Android API calls use deterministic host fixtures; real production pool owns permits, cancellation, delegate failure and shutdown. No frame rate or OS scheduling effect measured.')
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',default=ROOT);parser.add_argument('--work',required=True);parser.add_argument('--jdk')
    args=parser.parse_args();print(json.dumps(test(args.source,args.work,args.jdk)))
