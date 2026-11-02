#!/usr/bin/env python3
"""Execute the actual production first-shot corpus, CPU oracle and GLES program39.

All production Java is freshly compiled. No CPU arithmetic, model factory,
fixture generator, GPU transport or shader is replaced. Android declarations
are compiler-only; these numerical paths do not call Android framework code.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,shutil,subprocess,sys
ROOT=Path(__file__).resolve().parent

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    sys.path.insert(0,str(source))
    import build1990
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
    android=Path(os.environ.get('ULIKE_ANDROID_JAR',str(ndk.parent/'android.jar')))
    if not android.is_file():raise RuntimeError('Set ULIKE_ANDROID_JAR to the Android compile API jar')
    fixtures=source/'tests1990/gpu_math';native=source/'native1960';classes=work/'classes';classes.mkdir(exist_ok=True)
    sources=list(dict.fromkeys(build1990.compile_inputs([]).values()))
    tracked=sources+[fixtures/'StartupOracle1990.java',fixtures/'StartupReplay1990.java',source/'host_startup_math1990.py',native/'build_native1960.py',native/'compute_dependencies1990.json',native/'engine1960.c']+list(native.glob('*.comp'))+list((source/'native1958').glob('*.c'))+list((source/'native1958').glob('*.h'))
    pins={str(path.relative_to(source)):sha(path) for path in tracked}
    environment=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    commands=[]
    def run(command,name):
        command=list(map(str,command));commands.append({'name':name,'argv':command})
        result=subprocess.run(command,capture_output=True,text=True,env=environment,timeout=240)
        output=result.stdout+result.stderr;(work/(name+'.log')).write_text(output)
        if result.returncode or 'WARNING in native method' in output or 'FATAL ERROR in native method' in output:raise RuntimeError(name+' failed: '+output[-10000:])
        return output
    java,javac=jdk/'bin/java',jdk/'bin/javac'
    run([javac,'--release','8','-encoding','UTF-8','-cp',android,'-d',classes,*sources,fixtures/'StartupOracle1990.java',fixtures/'StartupReplay1990.java'],'all-current-production-compile')
    classpath=str(classes)+os.pathsep+str(android)
    oracle=run([java,'-XX:ActiveProcessorCount=4','-Xmx512m','-cp',classpath,'com.hiro.ulike.StartupOracle1990',work/'oracle.bin'],'actual-startup-cpu-oracle')
    cpu_native=work/'cpu-native';cpu_native.mkdir(exist_ok=True)
    run(['cc','-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-fPIC','-shared','-Wl,--no-undefined','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),source/'native1958/smooth_noise1958.c','-lm','-pthread','-o',cpu_native/'libulike_smooth1958.so'],'cpu-native-compile')
    native_oracle=run([java,'-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+str(cpu_native),'-cp',classpath,'com.hiro.ulike.StartupOracle1990',work/'native-oracle.bin','native'],'actual-startup-native-cpu-oracle')
    if 'native=false' not in oracle or 'native=true' not in native_oracle or (work/'oracle.bin').read_bytes()!=(work/'native-oracle.bin').read_bytes():raise AssertionError('Java and actual native CPU startup outputs differ')
    count=re.search(r'STARTUP cases=(\d+) pixels=(\d+)',oracle)
    if not count or int(count[1])<68:raise AssertionError('Complete production startup corpus missing')
    graphics=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
    for name in ('EGL','GLES3','KHR'):shutil.copytree(graphics/name,work/'headers'/name,dirs_exist_ok=True)
    spec=importlib.util.spec_from_file_location('startup_native1990',native/'build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(native,work)
    library=work/'libulike_gpu1960.so'
    run(['cc','-std=c11','-O3','-shared','-fPIC','-DANDROID','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(work/'headers'),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),native/'engine1960.c','-l:libEGL.so.1','-l:libGL.so.1','-o',library],'current-native-compile')
    output=run([java,'-Xcheck:jni','-Xmx512m','-Djava.library.path='+str(work),'-cp',classpath,'com.hiro.ulike.StartupReplay1990',work/'oracle.bin'],'production-corpus-jni-replay')
    match=re.search(r'RESULT records=(\d+) reads=(\d+) pixelsDiff=(\d+) confidenceDiff=(\d+)',output)
    if not match or int(match[1])!=int(count[1]) or int(match[2])!=int(count[1])*2 or int(match[3]) or int(match[4]) or 'UNSUPPORTED ' in output:raise AssertionError('Startup CPU/GPU matrix failed: '+output[-10000:])
    if pins!={str(path.relative_to(source)):sha(path) for path in tracked}:raise AssertionError('Production source changed during startup numerical proof')
    report=dict(status='passed',assertions=int(match[2]),startup_cases=int(count[1]),unique_oracle_pixels=int(count[2]),actual_jni_reads=int(match[2]),argb_differences=0,confidence_differences=0,
        actual_production_startup_fixture=True,actual_production_cpu_model=True,actual_production_cpu_oracle=True,actual_production_jni_transport=True,gpu_program=39,banks=[0,1],reused_confidence_buffer_poisoned=True,
        production_confidence_geometry_guard_verified=True,java_and_native_cpu_bit_exact=True,native_cpu_direct_completion_verified=True,physical_android_tested=False,device_speedup_verified=False,galaxy_mismatch_root_cause_confirmed=False,
        source_sha256=pins,oracle_sha256=sha(work/'oracle.bin'),native_sha256=sha(library),gl_environment=output.split('ENV ',1)[1].splitlines()[0])
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',type=Path,default=ROOT);parser.add_argument('--work',type=Path,required=True);parser.add_argument('--jdk',type=Path);parser.add_argument('--ndk',type=Path);args=parser.parse_args()
    print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),indent=2))
