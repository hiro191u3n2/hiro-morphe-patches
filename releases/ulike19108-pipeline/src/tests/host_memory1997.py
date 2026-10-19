#!/usr/bin/env python3
"""Foreground Finish memory recovery, native ownership and actual worker cancellation.

Retains the historical background oracle suite, executes real JNI scratch and
GLES ownership, and uses explicit transport peers only for Finish route testing.
No host result asserts Android latency or GPU pixel equivalence.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, subprocess, sys

FLAGS=('foreground_same_budget_retry_verified','active_gpu_ownership_preserved',
       'busy_native_deferred_release_verified','pressure_skips_optional_finish_copy',
       'ordinary_photo_qualification_preserved','same_finish_pixels_verified',
       'cancelled_speed_workers_classified','actual_worker_join_and_owner_release')

def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

def test(source,work,jdk=None,ndk=None):
    source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
    jar=Path(os.environ.get('ULIKE_ANDROID_JAR',str(jdk.parent/'android.jar')))
    sources=[source/n for n in ['FinishMemory1989.java','GpuChain1961.java','GpuQualification1961.java','SpeedWorkers1935.java']]
    tracked=sources+list((source/'tests1997/memory').glob('*'))+[Path(__file__).resolve()]
    hashes={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    def run(label,args,env=None):
        result=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=180,env=env)
        output=result.stdout+result.stderr;(work/(label+'.log')).write_text(output)
        if result.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError(label+'\n'+output[-16000:])
        return result.stdout
    def result(label,args,env=None):return json.loads(run(label,args,env).strip().splitlines()[-1])
    # Execute the original behavioral fixture against today's unchanged
    # background API. Historical .89 source inversions are separate frozen
    # release contracts, not a reason to rewrite newer production code here.
    native=work/'native';native.mkdir(exist_ok=True);classes=native/'classes';classes.mkdir(exist_ok=True)
    builder=module('memory97_native_builder',source/'native1960/build_native1960.py')
    builder.shader_header(source/'native1960',native)
    shader=(source/'h8gpu/bilateral1950.comp').read_text()
    (native/'bilateral_source1951.h').write_text('static const char bilateral1951_source[] =\n'+''.join(json.dumps(line+'\n')+'\n' for line in shader.splitlines())+';\n')
    headers=native/'headers';sysroot=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
    for name in ('EGL','GLES3','KHR'):shutil.copytree(sysroot/name,headers/name,dirs_exist_ok=True)
    flags=['-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-DANDROID']
    includes=['-I'+str(p) for p in (source,headers,jdk/'include',jdk/'include/linux',native)]
    link=['-Wl,--no-undefined','-l:libEGL.so.1','-l:libGL.so.1','-lm','-pthread']
    run('actual-gpu-native-compile',['cc',*flags,*includes,source/'native1960/engine1960.c',*link,'-o',native/'libulike_gpu1960.so'])
    run('actual-pools-native-compile',['cc',*flags,*includes,source/'tests1989/memory/MemoryNative1989.c',*link,'-o',native/'libmemory_native1989.so'])
    run('native-java-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*[source/n for n in ['GpuNoise1960.java','SpeedWorkers1935.java','FinishMemory1989.java','tests1989/memory/MemoryOwners1989.java','tests1989/memory/Memory1989Test.java']]])
    environment=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    inherited=result('background-run',[jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xms64m','-Xmx768m','-Djava.library.path='+str(native),'-cp',classes,'com.hiro.ulike.Memory1989Test'],environment)
    run('foreground-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',classes,'-d',classes,source/'tests1997/memory/ForegroundMemory1997Test.java'])
    environment=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    foreground=result('foreground-run',[jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xms64m','-Xmx768m','-Djava.library.path='+str(native),'-cp',classes,'com.hiro.ulike.ForegroundMemory1997Test'],environment)
    fixtures=module('memory97_peers',source/'host_qualification1981.py');queue=work/'queue';queue.mkdir(exist_ok=True);qfiles=[]
    for name,body in fixtures.FIXTURES.items():
        if name.endswith('QueueHost1967.java'):continue
        if name.endswith('QueueFixtures1967.java'):
            body=body.split('final class SpeedWorkers1935')[0]
            body=body.replace('static long captureEpoch1953(){return epoch;}','static long captureEpoch1953(){return epoch;}static long captureId1989(long e){return e==epoch?1:0;}')
        p=queue/'src'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);qfiles.append(p)
    run('cancellation-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',queue,source/'GpuQualification1961.java',source/'SpeedWorkers1935.java',source/'tests1997/memory/Cancellation1997Test.java',*qfiles])
    cancellation=result('cancellation-run',[jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-cp',queue,'com.hiro.ulike.Cancellation1997Test'])
    # Keep the complete graph/equality/5%-speed/cancel suite, then use the same
    # compiled production graph with observable pressure peers for the new call.
    previous=module('memory97_finish92',source/'host_finish_memory1992.py').test(source,work/'finish92',jdk,ndk)
    graph=work/'finish92/graph/classes';production=work/'finish92/production'
    body=(source/'tests1992/finish_memory/FinishStagePeers1992.java').read_text()
    old='    static void finishWork1992(Trace trace,int kind,long nanos,long bytes){}'
    new='''    static int copies1997,skipped1997;static long[] memory1997;
    static void finishWork1992(Trace trace,int kind,long nanos,long bytes){if(kind==10)copies1997++;}
    static void finishMemory1997(Trace trace,long[] values){memory1997=values==null?null:values.clone();}
    static void finishValidationSkipped1997(Trace trace,int reason){skipped1997=reason;}'''
    assert body.count(old)==1;body=body.replace(old,new)
    old='            if(FinishMemoryControl1989.mode==2||FinishMemoryControl1989.mode==6)'
    new='            if(FinishMemoryControl1989.mode==7)return FinishMemoryControl1989.checks>2;\n'+old
    assert body.count(old)==1;body=body.replace(old,new)
    p=work/'FinishStagePeers1997.java';p.write_text(body)
    cp=os.pathsep.join(map(str,[graph,production,jar]))
    run('finish-pressure-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',graph,p,source/'tests1997/memory/FinishPressure1997Test.java'])
    finish=result('finish-pressure-run',[jdk/'bin/java','-ea','-Xmx768m','-cp',cp,'com.hiro.ulike.FinishPressure1997Test'])
    tests=[inherited,foreground,cancellation,previous,finish]
    if hashes!={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Memory/qualification source changed during test')
    evidence=dict(status='passed',assertions=sum(x['assertions'] for x in tests),tests=tests,source_sha256=hashes,physical_android_tested=False,device_speedup_verified=False)
    for flag in FLAGS:
        if not any(t.get(flag) is True for t in tests):raise AssertionError('Missing executed evidence '+flag)
        evidence[flag]=True
    (work/'result.json').write_text(json.dumps(evidence,indent=2)+'\n');return evidence

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',required=True);parser.add_argument('--work',required=True);parser.add_argument('--jdk');parser.add_argument('--ndk');args=parser.parse_args()
    print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),indent=2))
