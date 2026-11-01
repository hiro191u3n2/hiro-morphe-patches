#!/usr/bin/env python3
"""Actual two-library JNI/GLES routing, session admission and memory regression."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, shutil, subprocess

def module(name,path):
    s=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
    source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk).resolve() if jdk else Path(shutil.which('javac')).resolve().parent.parent
    old=source/'baseline19118';fixture=source/'tests19119/gpu';classes=work/'classes';classes.mkdir(exist_ok=True)
    tracked=[source/'GpuNoise1960.java',source/'GpuNoiseOptimized19119.java',source/'native19119/engine19119.c',source/'native19119/strong19119.comp',old/'native1960/engine1960.c',*fixture.glob('*')]
    pins={str(x.relative_to(source)):sha(x) for x in tracked if x.is_file()}
    def run(args,label,good=True):
        p=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=180,env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1'))
        result=p.stdout+p.stderr;(work/(label+'.log')).write_text(result)
        if good and (p.returncode or 'WARNING in native method' in result or 'FATAL ERROR' in result):raise AssertionError(label+': '+result[-6000:])
        return p.returncode,result
    module('immutable119_old_builder',old/'native1960/build_native1960.py').shader_header(old/'native1960',work)
    new=module('independent119_builder',source/'native19119/build_gpu19119.py');identity,_=new.fingerprint(source)
    header='/* Independent production candidate, host transport build. */\n'
    for name in new.NAMES:
        path=source/'native19119/strong19119.comp' if name=='strong1960' else source/'native1960'/(name+'.comp')
        header+='static const char '+name+'_source[]=\n'+''.join(json.dumps(line+'\n')+'\n' for line in path.read_text().splitlines())+';\n'
    header+='#define GPU1960_SOURCE_SHA256 "'+identity+'"\n';(work/'shader_sources19119.h').write_text(header)
    flags=['-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work)]
    for name,c in [('1960',old/'native1960/engine1960.c'),('19119',source/'native19119/engine19119.c')]:
        run(['cc',*flags,c,'-l:libEGL.so.1','-l:libGL.so.1','-lm','-o',work/('libulike_gpu'+name+'.so')],'compile-native-'+name)
    sources=[source/'GpuNoise1960.java',source/'GpuNoiseOptimized19119.java',fixture/'DualOwner19119.java',fixture/'DualOwnerStubs19119.java']
    run([jdk/'bin/javac','--release','8','-d',classes,*sources],'compile-java')
    cmd=[jdk/'bin/java','-Xcheck:jni','-Xmx1g','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.DualOwner19119']
    _,output=run(cmd,'dual-owner-execute');match=re.search(r'RESULT assertions=(\d+) dualOwner=true physicalAndroid=false',output);assert match,output
    # Deliberately send a marked candidate upload to the old context. The raw
    # token value alone collides across engines; this test must reject routing.
    text=(source/'GpuNoise1960.java').read_text()
    faults={
        'wrong-owner':('GpuNoiseOptimized19119.uploadIntsNative(rawToken19119(token),slot,values)','uploadIntsNative(rawToken19119(token),slot,values)','AssertionError: dummy upload'),
        'missing-candidate-memory':('b=GpuNoiseOptimized19119.retainedBytes()','b=0L','AssertionError: combined retained sums both contexts'),
        'missing-global-session-guard':('if(activeSession)return null;\n            trimNative();','trimNative();','AssertionError: candidate second open refused')}
    for label,(seam,replacement,expected) in faults.items():
        assert text.count(seam)==1,'Mutation seam changed '+label
        mutant=work/label;mutant.mkdir(exist_ok=True);(mutant/'GpuNoise1960.java').write_text(text.replace(seam,replacement))
        run([jdk/'bin/javac','--release','8','-d',mutant,mutant/'GpuNoise1960.java',*sources[1:]],'compile-'+label)
        bad=cmd[:];bad[bad.index('-cp')+1]=mutant;code,output=run(bad,label+'-rejected',False)
        assert code!=0 and expected in output,label+' fault escaped: '+output[-3000:]
    assert pins=={str(x.relative_to(source)):sha(x) for x in tracked if x.is_file()},'Sources changed during review test'
    result=dict(status='passed',assertions=int(match.group(1))+len(faults),actual_jni_gles=True,independent_native_contexts=True,
        interleaving='baseline,candidate,baseline,candidate,trim,baseline,candidate,trim',global_second_open_refused=True,
        per_engine_capacity_and_failure_routing=True,combined_retained_counted=True,trim_both_engines=True,
        positive_assertions=int(match.group(1)),negative_controls=len(faults),negative_control_names=list(faults),wrong_owner_negative_control_detected=True,
        physical_android_tested=False,device_speedup_verified=False,source_sha256=pins)
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
