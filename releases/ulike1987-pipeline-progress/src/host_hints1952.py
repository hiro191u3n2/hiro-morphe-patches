#!/usr/bin/env python3
"""Exercise H15 public-API reflection against deterministic service/lifecycle fixtures.
This is API integration verification, not Galaxy speed or scheduler verification.
"""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess
ROOT=Path(__file__).resolve().parent

def run(args,out,timeout=90):
    p=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=timeout)
    out.write_text(p.stdout+p.stderr)
    if p.returncode:raise RuntimeError(p.stdout[-3000:]+p.stderr[-6000:])
    return p.stdout

def test(root,work,android=None):
    root=Path(root);out=Path(work)/'host-hints1952';out.mkdir(parents=True,exist_ok=True)
    javac=Path(os.environ.get('ULIKE_JAVAC') or shutil.which('javac') or '/workspace/scratch/d6f5652bff71/audit_h9/jdk/bin/javac')
    java=Path(os.environ.get('ULIKE_JAVA') or str(javac.with_name('java')))
    fixtures=root/'tests/h15';production=root/'PerformanceHints1952.java'
    # No PerformanceHintManager symbol is needed to compile the production helper.
    results=[]
    for mode,test_name in [('supported','PerformanceHints1952Test'),('api-class-absent','PerformanceHintsUnavailable1952Test')]:
        classes=out/mode;classes.mkdir(exist_ok=True)
        sources=sorted((fixtures/'android').rglob('*.java'))
        if mode=='api-class-absent':sources=[p for p in sources if p.name!='PerformanceHintManager.java']
        sources += [production,fixtures/'com/hiro/ulike/SpeedWorkers1935.java',fixtures/'com/hiro/ulike'/(test_name+'.java')]
        run([javac,'-source','8','-target','8','-Xlint:-options','-d',classes,*sources],out/(mode+'-compile.log'))
        results.append(json.loads(run([java,'-cp',classes,'com.hiro.ulike.'+test_name],out/(mode+'-test.log'))))
    if android is not None:
        baseline=out/'baseline-android';baseline.mkdir(exist_ok=True)
        run([javac,'-source','8','-target','8','-Xlint:-options','-bootclasspath',android,'-d',baseline,production,fixtures/'com/hiro/ulike/SpeedWorkers1935.java'],out/'baseline-android-compile.log')
    result=dict(results[0]);result['assertions']+=results[1]['assertions'];result['api_absence_test']=results[1]
    result.update(production_helper_compiled=True,baseline_android_compile_checked=android is not None,
        source_sha256=hashlib.sha256(production.read_bytes()).hexdigest(),
        optional_os_hint_does_not_modify_pixels=True,physical_android_scheduler_tested=False,
        fixture_scope='Public API reflection tested with deterministic Android service/thread/clock stubs. No OS scheduler, thermal behavior or Galaxy speed is claimed.')
    (out/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);p.add_argument('--android',type=Path);a=p.parse_args()
    print(json.dumps(test(ROOT,a.out,a.android)))
