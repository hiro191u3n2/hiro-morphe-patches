#!/usr/bin/env python3
"""Bounded dark/noisy Strong differential; never a device quality certificate.

The byte-pinned 1.9.59 Java implementation produces the independent expected
pixels. Current program39 uses real JNI and Mesa. Current CPU native JNI and
the explicit Java diagnostic use the same source/policy/model/halo. The .107 regional arithmetic is compiled unchanged into this test. This
host result cannot establish that the physical Galaxy mismatch is resolved.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,shutil,subprocess,sys
ROOT=Path(__file__).resolve().parent
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    sys.path.insert(0,str(source));from host_speed1959 import _sources
    import build1992
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']);android=Path(os.environ['ULIKE_ANDROID_JAR'])
    fixtures=source/'tests19105/strong_math';reference=source/'tests1960/published1959-reference';native=source/'native1960'
    if sha(reference/'pins.json')!='0510bf56f58a873563cdac95168d8f1d5b223f79a98a36ac01315e9193963575':raise AssertionError('Frozen published CPU manifest changed')
    for name,pin in json.loads((reference/'pins.json').read_text())['files'].items():
        if sha(reference/name)!=pin['sha256'] or (reference/name).stat().st_size!=pin['bytes']:raise AssertionError('Frozen CPU source changed '+name)
    tracked=[source/'StrongNoise1958.java',source/'GpuNoise1960.java',source/'host_strong_math19105.py',native/'engine1960.c',native/'build_native1960.py',native/'compute_dependencies1990.json',source/'native1958/smooth_noise1958.c',source/'native1958/neon1959.h',source/'native1958/scratch1959.h']+list(fixtures.glob('*.java'))+list(native.glob('*.comp'))
    tracked.append(source/'legacy_shader19106.py')
    from legacy_shader19106 import restore_regional19107
    restore_regional19107((native/'strong1960.comp').read_text(),required=True)
    pins={str(p.relative_to(source)):sha(p) for p in tracked};commands=[]
    env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    def run(argv,name,negative=False):
        argv=list(map(str,argv));commands.append(dict(name=name,argv=argv));p=subprocess.run(argv,text=True,capture_output=True,env=env,timeout=600);output=p.stdout+p.stderr;(work/(name+'.log')).write_text(output)
        if 'WARNING in native method' in output or 'FATAL ERROR in native method' in output:raise AssertionError(name+' JNI checker failed')
        if negative:
            if p.returncode==0 or 'AssertionError' not in output or 'expected=' not in output:raise AssertionError('corrupted oracle was not rejected')
        elif p.returncode:raise RuntimeError(name+'\n'+output[-12000:])
        return output
    for d in ['reference','gpu-classes','cpu-classes','headers','native-cpu','no-native']:(work/d).mkdir(exist_ok=True)
    run([jdk/'bin/javac','--release','8','-d',work/'reference',*_sources(reference),reference/'NativeSpeed1935.java',reference/'FastPixels1933.java',fixtures/'StrongMathOracle19105.java'],'oracle-compile')
    corpus=json.loads(run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-cp',work/'reference','com.hiro.ulike.StrongMathOracle19105',work/'oracle.bin'],'oracle').strip().splitlines()[-1])
    if corpus!={'records':98,'pixels':2234848}:raise AssertionError('independent corpus incomplete')
    for name in ('EGL','GLES3','KHR'):shutil.copytree(ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'/name,work/'headers'/name,dirs_exist_ok=True)
    spec=importlib.util.spec_from_file_location('native_math19105',native/'build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(native,work)
    cc=['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux')]
    run(cc+['-DANDROID','-Wno-misleading-indentation','-I'+str(work/'headers'),'-I'+str(work),native/'engine1960.c','-l:libEGL.so.1','-l:libGL.so.1','-o',work/'libulike_gpu1960.so'],'gpu-native-compile')
    stubs=(source/'native75testfixtures/FacadeStubs.java').read_text().replace('final class SpeedWorkers1935{','final class SpeedWorkers1935{static long nativeRetainedBytes1989(long[] values,int offset){return 0;}');(work/'FacadeStubs.java').write_text(stubs)
    run([jdk/'bin/javac','--release','8','-d',work/'gpu-classes',source/'GpuNoise1960.java',work/'FacadeStubs.java',fixtures/'StrongMathReplay19105.java'],'gpu-replay-compile')
    gpu_prefix=[jdk/'bin/java','-Xcheck:jni','-Djava.library.path='+str(work),'-cp',work/'gpu-classes','com.hiro.ulike.StrongMathReplay19105',work/'oracle.bin']
    gpu_out=run(gpu_prefix,'gpu-replay');gpu=json.loads(gpu_out.split('RESULT ',1)[1]);run(gpu_prefix+['corrupt'],'gpu-negative',True)
    if gpu['records']!=98 or gpu['single_rows']!=400 or gpu['reads']!=2008 or gpu['argb_differences']!=0:raise AssertionError('GPU matrix incomplete')
    sources=list(dict.fromkeys(build1992.compile_inputs([]).values()))
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',android,'-d',work/'cpu-classes',*sources,fixtures/'StrongMathCpu19105.java'],'cpu-replay-compile')
    run(cc+[source/'native1958/smooth_noise1958.c','-lm','-pthread','-o',work/'native-cpu/libulike_smooth1958.so'],'cpu-native-compile')
    cpu={}
    for name,path in [('native',work/'native-cpu'),('fallback',work/'no-native')]:
        out=run([jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Djava.library.path='+str(path),'-cp',str(work/'cpu-classes')+os.pathsep+str(android),'com.hiro.ulike.StrongMathCpu19105',work/'oracle.bin',name],'cpu-'+name)
        cpu[name]=json.loads(out.split('RESULT ',1)[1]);r=cpu[name]
        if r['rows']!=400 or r['native_rows']!=(400 if name=='native' else 0) or r['java_rows']!=(400 if name=='native' else 800):raise AssertionError('CPU backend matrix incomplete')
    if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('numerical sources changed during proof')
    result=dict(status='passed',assertions=gpu['assertions']+sum(r['assertions'] for r in cpu.values())+1,corpus=corpus,gpu=gpu,cpu=cpu,
        frozen_java_expected=True,source_width_3060=True,boundary_rows=[0,1,32,94,255,256,278,299],actual_jni=True,
        confidence_on_off_same_argb=True,unaligned_single_row_same_argb=True,both_gpu_banks=True,source_and_policy_preserved=True,
        cancelled_row_output_preserved=True,native_and_explicit_java_compared=True,actual_arm_neon_executed=False,
        physical_android_tested=False,device_speedup_verified=False,device_mismatch_reproduced=False,shader_math_changed=True,regional19107_exact_delta_verified=True,
        gl_environment=gpu_out.split('ENV ',1)[1].splitlines()[0],source_sha256=pins,oracle_sha256=sha(work/'oracle.bin'))
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',required=True,type=Path);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
