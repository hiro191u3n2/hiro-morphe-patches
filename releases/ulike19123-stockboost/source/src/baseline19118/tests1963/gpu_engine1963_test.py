"""Focused production JNI/GLES regression for the whole-pipeline GPU audit."""
from pathlib import Path
import importlib.util,json,os,re,shutil,subprocess

def test(root,work,jdk,ndk):
    root=Path(root).resolve();work=Path(work).resolve()/'gpu-engine1963';work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk);ndk=Path(ndk)
    def run(args,name,env=None):
        result=subprocess.run(list(map(str,args)),capture_output=True,text=True,env=env,timeout=90)
        (work/(name+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise RuntimeError(name+'\n'+result.stdout[-7000:]+result.stderr[-7000:])
        return result.stdout
    headers=work/'headers';headers.mkdir(exist_ok=True)
    for name in ('EGL','GLES3','KHR'):
        shutil.copytree(ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'/name,headers/name,dirs_exist_ok=True)
    spec=importlib.util.spec_from_file_location('gpu_engine1963_builder',root/'native1960/build_native1960.py')
    builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(root/'native1960',work)
    wraps=['glClientWaitSync','glUnmapBuffer','glDispatchCompute','glMapBufferRange','glGetError','glGetIntegerv','eglDestroyContext','eglDestroySurface','eglTerminate']
    run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-DANDROID',
         '-I'+str(headers),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),root/'native1960/engine1960.c',root/'native1960/residual_blocks1961.c',
         root/'tests1960/fault1960.c',root/'tests1963/gpu_engine_fault1963.c',*['-Wl,--wrap='+name for name in wraps],
         '-Wl,--no-undefined','-l:libEGL.so.1','-l:libGL.so.1','-lm','-o',work/'libulike_gpu1960.so'],'native-compile')
    from host_gpu1962 import all_sources
    classes=work/'classes';classes.mkdir(exist_ok=True)
    run([jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-d',classes,*all_sources(root),root/'tests1960/Native1960Test.java',root/'tests1963/GpuEngine1963Test.java'],'java-compile')
    environment=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1');reports={}
    for mode in ('normal','map-array','map-direct','timeout','init','busy-proof'):
        output=run([jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.GpuEngine1963Test',mode],mode,environment)
        if 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError('JNI checker failed '+mode)
        match=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
        if not match:raise AssertionError('Executed GPU report missing '+mode)
        reports[mode]=json.loads(match.group(1))
    report={'status':'passed','assertions':sum(item['assertions'] for item in reports.values()),'actual_host_gpu_execution':True,
            'device_speedup_verified':False,'reports':reports}
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
