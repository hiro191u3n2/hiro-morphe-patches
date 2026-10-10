#!/usr/bin/env python3
"""Run inherited .76 full-image JNI cases, then .78 identity/stream contracts.

All GPU commands use production JNI and real Mesa GLES. Host timing metadata is
controlled solely to reach established-route test branches, never to claim an
Android performance improvement. No pixel result is substituted or sampled.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, shutil, subprocess, sys

def test(source,work,jdk=None,ndk=None):
    source=Path(source).resolve();work=Path(work).resolve()/'resident-pipeline1978';work.mkdir(parents=True,exist_ok=True);sys.path.insert(0,str(source))
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve();native=source/'native1960'
    def module(name,path):
        spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value
    builder=module('resident1978_native_builder',native/'build_native1960.py')
    base=module('resident1978_old_chain_sources',source/'host_chain1976.py')
    additions=module('resident1978_current_dependencies',source/'host_chain1978.py').additions
    headers=work/'headers';graphics=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
    for name in ('EGL','GLES3','KHR'):shutil.copytree(graphics/name,headers/name,dirs_exist_ok=True)
    commands=[]
    def run(args,label,timeout=420):
        args=list(map(str,args));commands.append(dict(label=label,argv=args));result=subprocess.run(args,text=True,capture_output=True,timeout=timeout,env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1'))
        (work/(label+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise RuntimeError(label+'\n'+(result.stdout+result.stderr)[-18000:])
        if 'WARNING in native method' in result.stdout+result.stderr or 'FATAL ERROR' in result.stdout+result.stderr:raise AssertionError('JNI checker failure '+label)
        return result.stdout
    builder.shader_header(native,work)
    run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-I'+str(headers),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),native/'engine1960.c',native/'residual_blocks1961.c',source/'tests1960/fault1960.c','-Wl,--wrap=glClientWaitSync','-Wl,--wrap=glUnmapBuffer','-Wl,--wrap=glDispatchCompute','-l:libEGL.so.1','-l:libGLESv2.so.2','-lm','-o',work/'libulike_gpu1960.so'],'native-compile')
    fixtures=work/'fixtures';rows=base.current_sources1976(source,fixtures)+additions(source)
    # The old whole-pipeline fixture counts external camera diagnostics. Keep its
    # original public event counter when sharing the chain's Android fixtures.
    camera=fixtures/'CameraTrace1965.java';camera.write_text('package com.hiro.ulike; public final class CameraTrace1965 {public static int events;public static synchronized void event(String n,long epoch,String detail){events++;}}')
    rows += [source/'tests1960/Native1960Test.java',source/'tests1962/Chain1962Oracle.java',source/'resident76testfixtures/ResidentPipeline1976Test.java',source/'resident78testfixtures/ResidentPipeline1978Test.java']
    rows=list(dict.fromkeys(rows));tracked=rows+[native/'engine1960.c',native/'build_native1960.py',native/'residual_blocks1961.c',source/'tests1960/fault1960.c']+sorted(native.glob('*.comp'));pins={str(path):hashlib.sha256(path.read_bytes()).hexdigest() for path in tracked}
    classes=work/'classes';classes.mkdir(exist_ok=True)
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*rows],'compile')
    reports={}
    for label,main in (('inherited76','ResidentPipeline1976Test'),('identity78','ResidentPipeline1978Test')):
        output=run([jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx1536m','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.'+main],label)
        found=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
        if not found:raise AssertionError('missing executed result '+label)
        report=json.loads(found.group(1));reports[label]=report
        if report.get('status')!='passed' or report.get('assertions',0)<=0:raise AssertionError('passing assertions missing '+label)
    old=reports['inherited76'];current=reports['identity78']
    if old.get('pipelineCases')!=8 or old.get('layoutCases')!=4:raise AssertionError('unchanged .76 inherited coverage incomplete')
    required=('resident1978_identity_before_moire_actual_jni','resident1978_reducefirst_after_moire_preserved','resident1978_stream_all_pixels_actual_jni','resident1978_no_comparison_output_bitmap','resident1978_cancellation_and_ownership_actual_jni','resident1978_noise_position_negative_control')
    if any(current.get(flag) is not True for flag in required):raise AssertionError('new resident executed case missing')
    if pins!={str(path):hashlib.sha256(path.read_bytes()).hexdigest() for path in tracked}:raise AssertionError('production graph changed during native replay')
    result=dict(status='passed',assertions=sum(r['assertions'] for r in reports.values()),tests=reports,physical_android_tested=False,device_speedup_verified=False,host_timing_admission_fixture=True,actual_whole_pipeline_jni_verified=True,actual_layout_halo_equivalence_verified=True,original_resident_pipeline1976_cases_executed_unchanged=True,source_hashes=pins)
    for flag in required:result[flag]=True
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');(work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');return result
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',required=True);parser.add_argument('--work',required=True);parser.add_argument('--jdk');parser.add_argument('--ndk');args=parser.parse_args();print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),indent=2))
