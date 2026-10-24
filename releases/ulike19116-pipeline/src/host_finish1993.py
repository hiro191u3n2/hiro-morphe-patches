#!/usr/bin/env python3
"""Exact axis/parallel policy and independently certified32/128 memory fallback.

The fault fixture adds only a selectable128 memory exception at the production
entry. It never replaces output, numerical kernels, admission, or certificates.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess,sys

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    spec=importlib.util.spec_from_file_location('finish_policy1993',source/'host_finish_policy1992.py')
    policy=importlib.util.module_from_spec(spec);spec.loader.exec_module(policy)
    evidence=policy.test(source,work/'policy',jdk,ndk)
    fixture=work/'fault-source';fixture.mkdir(exist_ok=True)
    original=(source/'GpuChain1961.java').read_text()
    anchor='''        if(tileRows!=32&&tileRows!=64&&tileRows!=128)throw new IllegalArgumentException("finish tile rows");'''
    if original.count(anchor)!=1:raise AssertionError('Exact production fault entry missing')
    injected=anchor+'''
        if(tileRows==128){FinishSchedule1993Test.attempts128++;if(FinishSchedule1993Test.refuseWide)throw new DeferredChain(4);}
        else if(tileRows==32)FinishSchedule1993Test.attempts32++;
'''
    (fixture/'GpuChain1961.java').write_text(original.replace(anchor,injected))
    classes=work/'fault-classes';classes.mkdir(exist_ok=True)
    android=Path(os.environ.get('ULIKE_ANDROID_JAR',str(source.parent/'tools/android.jar'))).resolve()
    cp=os.pathsep.join(map(str,[work/'policy/classes',android]))
    def run(argv,name):
        r=subprocess.run(list(map(str,argv)),capture_output=True,text=True,timeout=300,env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1'))
        output=r.stdout+r.stderr;(work/(name+'.log')).write_text(output)
        if r.returncode or 'WARNING in native method' in output:raise AssertionError(name+'\n'+output[-10000:])
        return output
    run([jdk/'bin/javac','--release','8','-cp',cp,'-d',classes,fixture/'GpuChain1961.java',source/'tests1993/finish/FinishSchedule1993Test.java'],'schedule-compile')
    output=run([jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+str(work/'policy'),'-cp',str(classes)+os.pathsep+cp,'com.hiro.ulike.FinishSchedule1993Test'],'schedule-jni')
    schedule=json.loads(output.split('RESULT ',1)[1].strip())
    if schedule.get('status')!='passed' or not schedule.get('startup128_refusal_preserves32') or not schedule.get('runtime128_refusal_uses32'):raise AssertionError('Missing actual fallback evidence')
    result={'status':'passed','assertions':evidence['assertions']+schedule['assertions'],
            'resident_policy_trace_verified':evidence['resident_policy_trace_verified'],
            'policy':evidence,'schedule':schedule,'fault_scope':'Only128 entry throws existing DeferredChain(4); same production32 GPU graph computes fallback pixels.',
            'production_chain_sha256':hashlib.sha256(original.encode()).hexdigest(),'physical_android_tested':False,'device_speedup_verified':False}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    p=argparse.ArgumentParser()
    for name in ('source','work'):p.add_argument('--'+name,required=True)
    p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args()
    print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
