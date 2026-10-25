#!/usr/bin/env python3
"""H17 policy representations versus the unchanged .52 Java policy arithmetic.
Reported bytes are exact leased policy array lengths, not estimated GPU speed.
"""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess
import host_moire1951
POLICY52_SHA256='71e5021132c91d9c7c5e2e4ad40034d9c60246b48a75b831ef67b7e0dd72d951'

def test(root,work,android=None):
    root=Path(root).resolve();out=Path(work).resolve()/'host-policy1953';out.mkdir(parents=True,exist_ok=True)
    policy=root/'NativeMoire1951.java'
    if hashlib.sha256(policy.read_bytes()).hexdigest()!=POLICY52_SHA256:
        raise AssertionError('H17 must reuse the exact unchanged .52 preparePolicy Java implementation')
    classes=out/'classes'
    host_moire1951.compile_java(root,classes,[root/'FinishPolicy1953.java',root/'tests/h17/FinishPolicy1953Test.java'])
    java=os.environ.get('ULIKE_JAVA') or shutil.which('java') or '/workspace/scratch/d6f5652bff71/audit_h9/jdk/bin/java'
    p=subprocess.run([java,'-cp',str(classes),'com.hiro.ulike.FinishPolicy1953Test'],capture_output=True,text=True,timeout=120)
    (out/'test.log').write_text(p.stdout+p.stderr)
    if p.returncode:raise RuntimeError(p.stdout[-2000:]+p.stderr[-10000:])
    result=json.loads(p.stdout)
    # The no-sharp shortcut executes with a deliberately throwing policy stub:
    # even a single per-pixel preparePolicy call would fail this independent run.
    unused=out/'unused-classes';unused.mkdir(exist_ok=True)
    compiler=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
    command=[compiler] if compiler else ['java','com.sun.tools.javac.Main']
    fixtures=sorted((root/'tests/h17/unused-fixtures').rglob('*.java'))
    compiled=subprocess.run(command+['-source','8','-target','8','-Xlint:-options','-d',str(unused),
        *map(str,fixtures+[root/'SpeedWorkers1935.java',root/'FinishPolicy1953.java',root/'tests/h17/FinishUnused1953Test.java'])],
        capture_output=True,text=True,timeout=120)
    (out/'unused-compile.log').write_text(compiled.stdout+compiled.stderr)
    if compiled.returncode:raise RuntimeError(compiled.stderr[-10000:])
    checked=subprocess.run([java,'-cp',str(unused),'com.hiro.ulike.FinishUnused1953Test'],capture_output=True,text=True,timeout=120)
    (out/'unused-test.log').write_text(checked.stdout+checked.stderr)
    if checked.returncode:raise RuntimeError(checked.stdout[-2000:]+checked.stderr[-10000:])
    unused_result=json.loads(checked.stdout)
    result['assertions']+=unused_result['assertions']
    result['unused_sharp_policy_test']=unused_result
    result['unused_sharp_policy_zero_prepare_calls']=unused_result['unused_sharp_policy_zero_prepare_calls']
    if result.get('status')!='passed' or result.get('cases',0)<1000 or result.get('assertions',0)<1000000:
        raise AssertionError('Insufficient exact integer policy verification')
    if result['policy_bytes']>=result['dense_policy_bytes']*3//4:
        raise AssertionError('Policy representations did not reduce actual array/transfer bytes')
    result.update(source_sha256=hashlib.sha256((root/'FinishPolicy1953.java').read_bytes()).hexdigest(),
        unchanged_java_policy_sha256=hashlib.sha256(policy.read_bytes()).hexdigest(),
        unchanged_java_float_mask_policy=True,no_precision_reduction=True,production_helper_executed=True,
        float_math_ported_to_gpu=False,physical_android_speed_measured=False,
        fixture_scope='Actual unchanged Java finishing-policy arithmetic executes for every coordinate; decoded packed/constant/raw values are exact. GPU reader/pixels are verified by the separate production GLES suite.')
    (out/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--out',required=True);a=p.parse_args()
    print(json.dumps(test(Path(__file__).resolve().parent,a.out),indent=2))
