from pathlib import Path
import json, os, subprocess, re, sys, importlib.util, hashlib, shutil

def run(command,log,env=None):
    result=subprocess.run(list(map(str,command)),capture_output=True,text=True,env=env,timeout=420)
    Path(log).write_text(result.stdout+result.stderr)
    if result.returncode:raise AssertionError(str(log)+'\n'+result.stdout[-5000:]+result.stderr[-5000:])
    if 'WARNING in native method' in result.stdout+result.stderr:raise AssertionError('JNI checker '+str(log))
    match=re.search(r'^RESULT (\{[^\n]+\})$',result.stdout,re.M)
    return json.loads(match.group(1)) if match else None

def test(root,work,jdk,ndk):
    root,work,jdk,ndk=map(lambda p:Path(p).resolve(),(root,work,jdk,ndk));work=work/'chain1962';work.mkdir(parents=True,exist_ok=True)
    sys.path.insert(0,str(root))
    from host_gpu1961 import PIN_SHA
    from host_gpu1962 import all_sources
    reference=root/'tests1961/published1960-reference';pins=json.loads((reference/'pins.json').read_text())
    if hashlib.sha256((reference/'pins.json').read_bytes()).hexdigest()!=PIN_SHA:raise AssertionError('frozen oracle manifest')
    for name,facts in pins['files'].items():
        if hashlib.sha256((reference/name).read_bytes()).hexdigest()!=facts['sha256']:raise AssertionError('frozen source '+name)
    ref=work/'reference';current=work/'current';ref.mkdir(exist_ok=True);current.mkdir(exist_ok=True)
    oracle=root/'tests1962/Chain1962Oracle.java';candidate=root/'tests1962/Chain1962Test.java'
    reference_sources=[p for p in all_sources(reference,True) if p.name!='FastResize1933.java']+[reference/'FastResize1933.java']
    run([jdk/'bin/javac','-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-d',ref,*reference_sources,oracle],work/'reference-compile.log')
    records=work/'expected.bin'
    oracle_report=run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Djava.library.path='+str(work/'unavailable'),'-cp',ref,'com.hiro.ulike.Chain1962Oracle',records],work/'reference-run.log')
    sources=all_sources(root)+[oracle,candidate,root/'tests1960/Native1960Test.java']
    run([jdk/'bin/javac','-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-d',current,*dict.fromkeys(sources)],work/'current-compile.log')
    # Use the shared .62 host library if the parent already built it; otherwise
    # build the exact current production shaders/engine with host-only faults.
    native=work.parent/'host-gpu1961'
    if not (native/'libulike_gpu1960.so').is_file():
        native=work/'native';native.mkdir(exist_ok=True)
        includes=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
        headers=native/'headers';headers.mkdir(exist_ok=True)
        for d in ('EGL','GLES3','KHR'):shutil.copytree(includes/d,headers/d,dirs_exist_ok=True)
        spec=importlib.util.spec_from_file_location('gx24_builder',root/'native1960/build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(root/'native1960',native)
        run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-DANDROID','-I'+str(headers),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(native),root/'native1960/engine1960.c',root/'native1960/residual_blocks1961.c',root/'tests1960/fault1960.c',root/'tests1961/engine_observe1961.c','-Wl,--wrap=glClientWaitSync','-Wl,--wrap=glUnmapBuffer','-Wl,--wrap=glDispatchCompute','-Wl,--no-undefined','-l:libEGL.so.1','-l:libGL.so.1','-lm','-o',native/'libulike_gpu1960.so'],work/'native-compile.log')
    env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    base=[jdk/'bin/java','-XX:ActiveProcessorCount=4','-Xcheck:jni','-Djava.library.path='+str(native),'-cp',current,'com.hiro.ulike.Chain1962Test',records]
    exact=run(base,work/'pixels.log',env);fault=run(base+['failure'],work/'failure.log',env)
    if exact is None or exact.get('cases',0)<288 or exact.get('exactPixels',0)<1000000:raise AssertionError('full compatible slice coverage absent')
    if fault is None or fault.get('assertions',0)<7:raise AssertionError('failure coverage absent')
    result={'status':'passed','assertions':exact['assertions']+fault['assertions'],'oracle':oracle_report,'pixels':exact,'failed_candidate':fault,'physicalAndroidTested':False}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--root',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk',required=True);p.add_argument('--ndk',required=True);a=p.parse_args();print(json.dumps(test(a.root,a.work,a.jdk,a.ndk),indent=2))
