#!/usr/bin/env python3
"""Bounded callback correlation checks; does not establish native restoration."""
import argparse, hashlib, json, pathlib, subprocess, tempfile
ROOT=pathlib.Path(__file__).resolve().parent
WORK=ROOT.parents[2]
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def run(args):
    result=subprocess.run([str(x) for x in args],capture_output=True,text=True)
    if result.returncode: raise RuntimeError(result.stdout+'\n'+result.stderr)
    return result.stdout.strip()
def main():
    if not __debug__: raise SystemExit('Python -O is not supported')
    p=argparse.ArgumentParser();p.add_argument('--report',type=pathlib.Path,default=ROOT/'QA_INIT_CALLBACK.json');args=p.parse_args()
    tools=WORK/'models168/tools';jdk=tools/'jdk21/jdk-21.0.12.1+1/bin';sdk=tools/'android.jar';r8=tools/'r8-8.3.37.jar'
    sources=sorted((ROOT/'src').rglob('*.java'));tests=sorted((ROOT/'test').rglob('*.java'))
    lifecycle=WORK/'complete_integration169/lifecycle'
    evidence=lifecycle/'NATIVE_INIT_CALLBACK_EVIDENCE.json'
    transform=lifecycle/'PrepareNativeInitCallback182.java'
    with tempfile.TemporaryDirectory(prefix='composer-init182-') as temporary:
        build=pathlib.Path(temporary);host=build/'host';android=build/'android';dex=build/'dex'
        for d in (host,android,dex):d.mkdir()
        run([jdk/'javac','--release','8','-d',host,*sources,*tests])
        results={name:run([jdk/'java','-cp',host,'com.hiro.ulike.composer.'+name]) for name in ('ComposerReplayTest','NativeComposerBoundaryTest','NativeInitCallbackTest')}
        run([jdk/'javac','--release','8','-cp',sdk,'-d',android,*sources])
        run([jdk/'java','-cp',r8,'com.android.tools.r8.D8','--min-api','26','--lib',sdk,'--output',dex,*sorted(android.rglob('*.class'))])
        report={'schema':'ulike-composer-init-callback-182-v1','status':'PASS_BOUNDED_HOST_SDK36_AND_PINNED_CALLBACK_TRANSFORM',
            'host_tests':results,'sdk36_compile':True,'d8_min_api':26,'dex_bytes':(dex/'classes.dex').stat().st_size,
            'source_sha256':{str(path.relative_to(ROOT)):sha(path) for path in sources+tests+[pathlib.Path(__file__).resolve()]},
            'callback_transform_sha256':sha(transform),'callback_evidence_sha256':sha(evidence),'callback_evidence':json.loads(evidence.read_text()),
            'tools_sha256':{'android_sdk36':sha(sdk),'r8':sha(r8)},'production_callback_hook_enabled':False,
            'native_queue_barrier_implemented':False,'complete_settings_restoration':False,'device_execution':False,
            'limitations':['Callback receipt proves an observed entry status and identity correlation only; listener and queued style work may remain pending.','Zero terminal return without a callback remains insufficient for initializationReceipt.','Receiver reuse is rejected because the pinned callback supplies no native generation ID; a verified prior native join could enable a future different policy.','Compiler and host protocol tests do not run JNI callbacks or Android camera/preview restoration.']}
        args.report.write_text(json.dumps(report,indent=2)+'\n')
        for result in results.values():print(result)
        print('PASS SDK36/D8 and pinned original-method callback evidence')
if __name__=='__main__':main()
