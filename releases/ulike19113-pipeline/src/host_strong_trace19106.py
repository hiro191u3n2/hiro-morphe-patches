#!/usr/bin/env python3
"""Actual Java/native/GLSL checkpoint instrumentation proof on Mesa, never Galaxy certification."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,shutil,subprocess,sys
ROOT=Path(__file__).resolve().parent
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def method(text,name,marker=None):
    start=text.index(marker or 'private static void '+name+'(');start=text.index('{',start);depth=1;end=start+1
    while depth:
        depth+=(text[end]=='{')-(text[end]=='}');end+=1
    return text[start+1:end-1]
def normalize(text):
    text=re.sub(r'/\*.*?\*/|//[^\n]*','',text,flags=re.S)
    text=re.sub(r'trace(?:Floats|Int)19106\([^;]*?\);','',text)
    return re.sub(r'\s+','',text)
def source_equivalence(source):
    text=(source/'StrongNoise1958.java').read_text();results={}
    # Compare complete helper arithmetic bodies after removing write-only instrumentation.
    pairs=[('guide',method(text,'guide','private static void guide(int[] src,int width,int height,int x,int y,int validBegin,int validEnd,float sy,float sc,float[] out,ColourRows colours)'),method(text,'guideTrace19106')),
           ('sample',method(text,'sample'),method(text,'sampleTrace19106'))]
    for label,ordinary,traced in pairs:
        if normalize(ordinary)!=normalize(traced):raise AssertionError('Diagnostic helper arithmetic differs: '+label)
        results[label]=hashlib.sha256(normalize(ordinary).encode()).hexdigest()
    ordinary=method(text,'nonlocalFine','private static void nonlocalFine(int[] src,int width,int height,int x,int y,int validBegin,int validEnd,float sy,float sc,float[] result,ColourRows colours)')
    traced=method(text,'nonlocalTrace19106').replace('int candidate=-1;','').replace('int slot=544+96*(++candidate);','')
    if normalize(ordinary)!=normalize(traced):raise AssertionError('Diagnostic NLM helper arithmetic differs')
    results['nonlocalFine']=hashlib.sha256(normalize(ordinary).encode()).hexdigest()
    # Region helper's only structural difference names the return expression for recording.
    ordinary=method(text,'regionalSigma','private static float regionalSigma(')
    traced=method(text,'regionalTrace19106','private static float regionalTrace19106(')
    traced=re.sub(r'float value=([^;]+);',r'return \1;',traced).replace('return value;','')
    if normalize(ordinary)!=normalize(traced):raise AssertionError('Diagnostic regional interpolation differs')
    results['regionalSigma']=hashlib.sha256(normalize(ordinary).encode()).hexdigest()
    # Main arithmetic has identical statement subsequences between its existing block boundaries.
    ordinary=method(text,'processJavaRangeInternal','private static void processJavaRangeInternal(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection,boolean useNlm,Workspace workspace,int[] policy,int[] confidenceMask)')
    traced=method(text,'diagnosticTrace19106','static int[] diagnosticTrace19106(')
    for n,(begin,end) in enumerate([('float dark=','float strength='),('float strength=','if('),('float dy=(targetY','for(int k=0;k<3;k++)'),('float gate=','traceFloats19106(trace,48')]):
        a=ordinary[ordinary.index(begin):];b=traced[traced.index(begin):]
        if n==3:a=a[:a.index('}',a.index('dR+='))];b=b[:b.index(end)]
        else:a=a[:a.index(end)];b=b[:b.index(end)]
        b=b.replace('int slot=128+16*((dy+2)*5+dx+2);','')
        if normalize(a)!=normalize(b):raise AssertionError('Diagnostic main arithmetic differs at block '+str(n))
        results['main_'+str(n)]=hashlib.sha256(normalize(a).encode()).hexdigest()
    a=ordinary[ordinary.index('if(useNlm&&'):ordinary.index('float dy=(targetY')]
    b=traced[traced.index('if(confidence>'):traced.index('float dy=(targetY')]
    a=a.replace('useNlm&&','');b=b.replace('nonlocalTrace19106','nonlocalFine').replace('nl,colours,trace)','nl,colours)')
    if normalize(a)!=normalize(b):raise AssertionError('Diagnostic NLM activation and blend differ')
    results['main_nlm']=hashlib.sha256(normalize(a).encode()).hexdigest()
    # Negative control proves the normalization retains and rejects arithmetic changes.
    mutated=traced.replace('sy*4','sy*5',1)
    a=normalize(traced[traced.index('float dark='):traced.index('float strength=')]);b=normalize(mutated[mutated.index('float dark='):mutated.index('float strength=')])
    if a==b:raise AssertionError('Arithmetic mutation escaped source gate')
    return results

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    sys.path.insert(0,str(source));import build1992
    jdk=Path(jdk or os.environ.get('JAVA_HOME') or os.environ['ULIKE_JDK_HOME']);ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']);android=Path(os.environ['ULIKE_ANDROID_JAR'])
    fixtures=source/'tests19106/strong_trace';native=source/'native1960';equivalence=source_equivalence(source)
    tracked=[source/'StrongNoise1958.java',source/'GpuNoise1960.java',source/'GpuStrong1960.java',source/'GpuResident1976.java',source/'ResidentStages19104.java',source/'host_strong_trace19106.py',native/'engine1960.c',native/'build_native1960.py',source/'native1958/smooth_noise1958.c',source/'native1958/neon1959.h',source/'native1958/scratch1959.h']+list(fixtures.glob('*.java'))+list(native.glob('*.comp'))
    pins={str(p.relative_to(source)):sha(p) for p in tracked};commands=[]
    env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    def run(argv,name):
        argv=list(map(str,argv));commands.append(dict(name=name,argv=argv));p=subprocess.run(argv,text=True,capture_output=True,env=env,timeout=600);out=p.stdout+p.stderr;(work/(name+'.log')).write_text(out)
        if p.returncode or 'WARNING in native method' in out or 'FATAL ERROR in native method' in out:raise RuntimeError(name+'\n'+out[-14000:])
        return out
    for d in ['gpu-classes','cpu-classes','headers','native-cpu']:(work/d).mkdir(exist_ok=True)
    sources=list(dict.fromkeys(build1992.compile_inputs([]).values()))
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',android,'-d',work/'cpu-classes',*sources,fixtures/'StrongTraceCorpus19106.java',fixtures/'StrongTraceWrapper19106.java'],'cpu-compile')
    cc=['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux')]
    run(cc+[source/'native1958/smooth_noise1958.c','-lm','-pthread','-o',work/'native-cpu/libulike_smooth1958.so'],'cpu-native-compile')
    cpu=json.loads(run([jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Djava.library.path='+str(work/'native-cpu'),'-cp',str(work/'cpu-classes')+os.pathsep+str(android),'com.hiro.ulike.StrongTraceCorpus19106',work/'trace.bin'],'cpu-corpus').split('RESULT ',1)[1])
    for name in ('EGL','GLES3','KHR'):shutil.copytree(ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'/name,work/'headers'/name,dirs_exist_ok=True)
    spec=importlib.util.spec_from_file_location('native_trace19106',native/'build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(native,work)
    run(cc+['-DANDROID','-Wno-misleading-indentation','-I'+str(work/'headers'),'-I'+str(work),native/'engine1960.c','-l:libEGL.so.1','-l:libGL.so.1','-o',work/'libulike_gpu1960.so'],'gpu-native-compile')
    stubs=(source/'native75testfixtures/FacadeStubs.java').read_text().replace('final class SpeedWorkers1935{','final class SpeedWorkers1935{static long nativeRetainedBytes1989(long[] values,int offset){return 0;}');(work/'FacadeStubs.java').write_text(stubs)
    run([jdk/'bin/javac','--release','8','-d',work/'gpu-classes',source/'GpuNoise1960.java',work/'FacadeStubs.java',fixtures/'StrongTraceReplay19106.java'],'gpu-compile')
    gpu_out=run([jdk/'bin/java','-Xcheck:jni','-Djava.library.path='+str(work),'-cp',work/'gpu-classes','com.hiro.ulike.StrongTraceReplay19106',work/'trace.bin'],'gpu-replay');gpu=json.loads(gpu_out.split('RESULT ',1)[1])
    wrapper=json.loads(run([jdk/'bin/java','-ea','-Xcheck:jni','-Djava.library.path='+str(work)+os.pathsep+str(work/'native-cpu'),'-cp',str(work/'cpu-classes')+os.pathsep+str(android),'com.hiro.ulike.StrongTraceWrapper19106',work/'trace.bin'],'wrapper-replay').split('RESULT ',1)[1])
    if cpu['records']!=79 or gpu['records']!=79 or gpu['negative_controls']!=5 or gpu['raw_differences']!=0:raise AssertionError('Trace corpus incomplete')
    if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Trace proof sources changed during run')
    result=dict(status='passed',assertions=cpu['assertions']+gpu['assertions']+wrapper['assertions']+len(equivalence)+2,cpu=cpu,gpu=gpu,wrapper=wrapper,source_arithmetic=equivalence,
        actual_wrapper_jni=True,actual_jni=True,paired_raw_checkpoints=True,traced_row_matches_production=True,cpu_trace_matches_native_java=True,
        arithmetic_clone_guarded=True,negative_controls_verified=True,source_width_3060=True,boundary_rows=[0,1,31,32,63,80,94,255,256,278,520,539],
        source_and_policy_preserved=True,cancellation_and_invalid_geometry_verified=True,physical_android_tested=False,device_speedup_verified=False,device_mismatch_reproduced=False,
        gl_environment=gpu_out.split('ENV ',1)[1].splitlines()[0],source_sha256=pins,trace_corpus_sha256=sha(work/'trace.bin'))
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',required=True,type=Path);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
