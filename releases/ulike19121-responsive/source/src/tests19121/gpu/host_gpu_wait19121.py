#!/usr/bin/env python3
"""Real production Java ownership/backoff with deterministic host JNI transport.

The fixture does not emulate GLES performance or test Android responsiveness.
"""
from pathlib import Path
import argparse, hashlib, json, os, re, shutil, subprocess

def sha(path): return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def test(source,work,jdk=None):
    source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk).resolve() if jdk else Path(shutil.which('javac')).resolve().parent.parent
    fixture=source/'tests19121/gpu'
    tracked=[source/'GpuNoise1960.java',source/'GpuNoiseOptimized19119.java',source/'GpuFinish1953.java',*sorted(fixture.glob('*'))]
    pins={str(p.relative_to(source)):sha(p) for p in tracked if p.is_file()}
    def run(command,label,good=True):
        p=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=45)
        output=p.stdout+p.stderr;(work/(label+'.log')).write_text(output)
        if good and (p.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output):raise AssertionError(label+': '+output[-6000:])
        return p.returncode,output
    run(['cc','-std=c11','-O2','-shared','-fPIC','-Wall','-Wextra','-Werror','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),fixture/'wait_jni19121.c','-o',work/'libwait19121.so'],'jni-fixture-compile')
    sources=[source/'GpuNoise1960.java',source/'GpuNoiseOptimized19119.java',fixture/'WaitStubs19121.java',fixture/'GpuWait19121Test.java']
    classes=work/'classes';classes.mkdir(exist_ok=True)
    run([jdk/'bin/javac','--release','8','-d',classes,*sources],'production-java-compile')
    command=[jdk/'bin/java','-Xcheck:jni','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.GpuWait19121Test']
    _,output=run(command,'wait-cases');match=re.search(r'RESULT assertions=(\d+) physicalAndroid=false GLES=false',output);assert match,output
    text=(source/'GpuNoise1960.java').read_text()
    faults={
        'unbounded-poll-delay':('if(nextNanos<2000000L)nextNanos*=2;','nextNanos*=2;','bounded backoff schedule'),
        'wrong-bank-release':('pending[ticket.bank]=null;ticket.collected=true;','pending[0]=null;pending[1]=null;ticket.collected=true;','only collected bank released')}
    for name,(before,after,expected) in faults.items():
        assert before in text,name+' mutation seam missing'
        folder=work/name;folder.mkdir(exist_ok=True);mutant=folder/'GpuNoise1960.java';mutant.write_text(text.replace(before,after))
        run([jdk/'bin/javac','--release','8','-d',folder,mutant,*sources[1:]],name+'-compile')
        cmd=command[:];cmd[cmd.index('-cp')+1]=folder;code,output=run(cmd,name,False)
        assert code!=0 and expected in output,name+' escaped: '+output[-2000:]
    assert pins=={str(p.relative_to(source)):sha(p) for p in tracked if p.is_file()},'Sources changed while tests ran'
    result=dict(status='passed',assertions=int(match.group(1)),negative_controls=len(faults),negative_control_names=list(faults),
        source_sha256=pins,production_java_executed=True,host_jni_fixture=True,actual_jni_gles=False,
        all_five_collect_paths=True,two_bank_ownership=True,readiness_error_and_readback_failure=True,
        interrupt_flag_preserved=True,five_second_timeout_executed=True,six_existing_metrics_unchanged=True,
        bounded_backoff_resets_per_call=True,physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');args=p.parse_args()
    print(json.dumps(test(args.source,args.work,args.jdk),indent=2))
