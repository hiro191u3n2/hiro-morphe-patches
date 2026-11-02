#!/usr/bin/env python3
"""Run the real CPU model and all exact-preparation resident GPU graphs."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, subprocess, sys

ROOT=Path(__file__).resolve().parent
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    sys.path.insert(0,str(source));import build1990
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
    android=Path(os.environ.get('ULIKE_ANDROID_JAR',str(ndk.parent/'android.jar')))
    if not android.is_file():raise RuntimeError('Set ULIKE_ANDROID_JAR')
    native=source/'baseline19118/native1960';classes=work/'classes';classes.mkdir(exist_ok=True)
    sources=list(dict.fromkeys(build1990.compile_inputs([]).values()))
    harness=source/'tests1990/strongroute/ModelCorpus1990Test.java'
    tracked=sources+[harness,source/'host_model_math1990.py',native/'build_native1960.py',native/'engine1960.c']+list(native.glob('*.comp'))+list((source/'native1958').glob('*.c'))+list((source/'native1958').glob('*.h'))
    pins={str(path.relative_to(source)):sha(path) for path in tracked}
    environment=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    commands=[]
    def run(command,name):
        command=list(map(str,command));commands.append({'name':name,'argv':command})
        result=subprocess.run(command,capture_output=True,text=True,env=environment,timeout=240)
        output=result.stdout+result.stderr;(work/(name+'.log')).write_text(output)
        if result.returncode or 'WARNING in native method' in output or 'FATAL ERROR in native method' in output:raise RuntimeError(name+' failed: '+output[-10000:])
        return output
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',android,'-d',classes,*sources,harness],'all-production-compile')
    graphics=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
    for name in ('EGL','GLES3','KHR'):shutil.copytree(graphics/name,work/'headers'/name,dirs_exist_ok=True)
    spec=importlib.util.spec_from_file_location('model_native1990',native/'build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(native,work)
    library=work/'libulike_gpu1960.so'
    run(['cc','-std=c11','-O3','-shared','-fPIC','-DANDROID','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(work/'headers'),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),native/'engine1960.c','-l:libEGL.so.1','-l:libGL.so.1','-o',library],'immutable-baseline-native-current-wrapper-compile')
    output=run([jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+str(work),'-cp',str(classes)+os.pathsep+str(android),'com.hiro.ulike.ModelCorpus1990Test'],'model-corpus-jni')
    result=json.loads(output.split('RESULT ',1)[1].strip())
    if result.get('status')!='passed' or result.get('model_cases')!=96 or result.get('assertions',0)<=0:raise AssertionError('Model graph numerical proof incomplete')
    cpu_native=work/'cpu-native';cpu_native.mkdir(exist_ok=True)
    run(['cc','-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-fPIC','-shared','-Wl,--no-undefined','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),source/'native1958/smooth_noise1958.c','-lm','-pthread','-o',cpu_native/'libulike_smooth1958.so'],'cpu-native-compile')
    native_output=run([jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+str(work)+os.pathsep+str(cpu_native),'-cp',str(classes)+os.pathsep+str(android),'com.hiro.ulike.ModelCorpus1990Test','native'],'model-corpus-native-cpu-jni')
    native_result=json.loads(native_output.split('RESULT ',1)[1].strip())
    if native_result.get('status')!='passed' or native_result.get('model_cases')!=96 or not native_result.get('actual_cpu_jni') or result.get('actual_cpu_jni'):raise AssertionError('Native/Java CPU backend evidence incomplete')
    result['java_cpu']=dict(result);result['native_cpu']=native_result
    result['assertions']+=native_result['assertions'];result['model_cases']+=native_result['model_cases'];result['compared_words']+=native_result['compared_words']
    result['actual_cpu_jni']=True
    if pins!={str(path.relative_to(source)):sha(path) for path in tracked}:raise AssertionError('Production sources changed during model proof')
    result.update(source_sha256=pins,native_sha256=sha(library),gl_environment=output.split('ENV ',1)[1].splitlines()[0],startup_cases=32,variants=[0,1,2],program_ids=list(range(51,60)),whole_model_bitwise_equal=True,resident_ownership_verified=True)
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--source',type=Path,default=ROOT);parser.add_argument('--work',type=Path,required=True);parser.add_argument('--jdk',type=Path);parser.add_argument('--ndk',type=Path);args=parser.parse_args()
    print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),indent=2))
