#!/usr/bin/env python3
"""Finite actual GLES mode0 crop proof. Not a timing/device certification.

Compiles current production Java, generates unchanged shader sources and builds
the actual JNI engine for host Mesa. Only raw-crop commands are executed.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,shutil,subprocess,sys

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None,android=None):
    jdk=jdk or os.environ.get("JAVA_HOME") or os.environ["ULIKE_JDK_HOME"]
    ndk=ndk or os.environ["ULIKE_NDK_HOME"]
    android=android or os.environ["ULIKE_ANDROID_JAR"]
    source,work,jdk,ndk,android=map(lambda p:Path(p).resolve(),(source,work,jdk,ndk,android))
    work.mkdir(parents=True,exist_ok=True);sys.path.insert(0,str(source));import build1990
    java=list(dict.fromkeys(build1990.compile_inputs([]).values()))
    fixture=Path(__file__).resolve().with_name('RawCrop104.java');native=source/'native1960'
    spec=importlib.util.spec_from_file_location('rawcrop_native19104',native/'build_native1960.py')
    builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder)
    fingerprint,compute_pins=builder.compute_fingerprint1990(native)
    pins={str(p.relative_to(source)):sha(p) for p in java}
    fixture_pin,runner_pin=sha(fixture),sha(__file__)
    env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1');commands=[]
    def run(argv,name):
        argv=list(map(str,argv));commands.append({'name':name,'argv':argv})
        p=subprocess.run(argv,env=env,text=True,capture_output=True,timeout=180)
        output=p.stdout+p.stderr;(work/(name+'.log')).write_text(output)
        if p.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise RuntimeError(name+' failed\n'+output[-10000:])
        return output
    classes=work/'classes';classes.mkdir(exist_ok=True)
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',android,'-d',classes,*java,fixture],'compile-production')
    headers=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
    for name in ('EGL','GLES3','KHR'):shutil.copytree(headers/name,work/'headers'/name,dirs_exist_ok=True)
    builder.shader_header(native,work);library=work/'libulike_gpu1960.so'
    run(['cc','-std=c11','-O3','-shared','-fPIC','-DANDROID','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(work/'headers'),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),native/'engine1960.c','-l:libEGL.so.1','-l:libGL.so.1','-o',library],'compile-native')
    output=run([jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+str(work),'-cp',str(classes)+os.pathsep+str(android),'com.hiro.ulike.RawCrop104'],'raw-crop-actual-gl')
    result=json.loads(next(line[7:] for line in output.splitlines() if line.startswith('RESULT ')))
    environment=next(line[4:] for line in output.splitlines() if line.startswith('ENV '))
    if result['cases']!=24 or result['compared_argb_words']!=1123168 or result['tail_guard_words']!=531364:raise AssertionError('finite corpus incomplete')
    if 'source='+fingerprint not in environment:raise AssertionError('runtime shader fingerprint differs')
    if pins!={str(p.relative_to(source)):sha(p) for p in java} or (fingerprint,compute_pins)!=builder.compute_fingerprint1990(native) or fixture_pin!=sha(fixture) or runner_pin!=sha(__file__):raise AssertionError('source changed during run')
    result.update(scope='actual production JNI/GLES; finite raw-copy correctness; no device/timing certification',source_sha256=pins,compute_source_sha256=compute_pins,compute_fingerprint=fingerprint,fixture_sha256=fixture_pin,runner_sha256=runner_pin,native_sha256=sha(library),gl_environment=environment,source_unchanged=True)
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for n in ('source','work','jdk','ndk','android'):p.add_argument('--'+n,required=True,type=Path)
    a=p.parse_args();r=test(a.source,a.work,a.jdk,a.ndk,a.android)
    print(json.dumps({k:v for k,v in r.items() if not k.endswith('sha256')},indent=2))
