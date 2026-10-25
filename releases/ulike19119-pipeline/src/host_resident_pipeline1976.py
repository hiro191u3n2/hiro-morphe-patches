#!/usr/bin/env python3
"""Actual production whole-image Strong/NR13/resident/geometry/finish JNI gate.
Uses functional Android object fixtures and real Mesa shader execution; never a
physical Android benchmark. The CPU reference uses the preserved CPU route.
"""
from pathlib import Path
import hashlib,importlib.util,json,os,re,shutil,subprocess,sys

def test(source,work,jdk=None,ndk=None):
    source=Path(source).resolve();work=Path(work).resolve()/'resident-pipeline1976';work.mkdir(parents=True,exist_ok=True);sys.path.insert(0,str(source))
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();native=source/'native1960'
    def module(name,path):
        spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
    builder=module('resident1976_builder',native/'build_native1960.py');base=module('resident1976_base',source/'host_gpu1962.py')
    headers=work/'headers'
    if ndk:
        graphics=Path(ndk).resolve()/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
        for n in ('EGL','GLES3','KHR'):shutil.copytree(graphics/n,headers/n,dirs_exist_ok=True)
    else:
        fallback=source.parent/'gpu76-memory/headers'
        if fallback.exists():shutil.copytree(fallback,headers,dirs_exist_ok=True)
    commands=[]
    def run(args,label,timeout=240):
        args=list(map(str,args));commands.append(dict(label=label,argv=args));r=subprocess.run(args,text=True,capture_output=True,timeout=timeout,env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1'))
        (work/(label+'.log')).write_text(r.stdout+r.stderr)
        if r.returncode:raise RuntimeError(label+'\n'+(r.stdout+r.stderr)[-18000:])
        if 'WARNING in native method' in r.stdout+r.stderr or 'FATAL ERROR' in r.stdout+r.stderr:raise AssertionError('JNI checker failure '+label)
        return r.stdout
    builder.shader_header(native,work)
    run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-I'+str(headers),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),native/'engine1960.c',native/'residual_blocks1961.c',source/'tests1960/fault1960.c','-Wl,--wrap=glClientWaitSync','-Wl,--wrap=glUnmapBuffer','-Wl,--wrap=glDispatchCompute','-l:libEGL.so.1','-l:libGLESv2.so.2','-lm','-o',work/'libulike_gpu1960.so'],'native-compile')
    classes=work/'classes';classes.mkdir(exist_ok=True)
    fixture=work/'fixtures';fixture.mkdir(exist_ok=True)
    face=fixture/'FaceRegions1934.java';face.write_text((source/'tests/pipeline1942-fixtures/com/hiro/ulike/FaceRegions1934.java').read_text().replace('public Mask resample(', 'public long retainedBytes1976(){return 128;}public Mask resample('))
    camera=fixture/'CameraTrace1965.java';camera.write_text('package com.hiro.ulike; public final class CameraTrace1965 {public static int events;public static synchronized void event(String n,long epoch,String detail){events++;}}')
    rows=base.all_sources(source);rows=[p for p in rows if p.name not in ('FaceRegions1934.java','CameraTrace1965.java')]
    for name in ('GpuStrongTuning1975','GpuStrongLayout1976','GpuResident1976','ColourCache1976','PairedRegions1976','CpuFinishCache1976'):
        rows.append(source/(name+'.java'))
    rows.extend([face,camera,source/'tests1960/Native1960Test.java',source/'tests1962/Chain1962Oracle.java',source/'resident76testfixtures/ResidentPipeline1976Test.java'])
    rows=list(dict.fromkeys(rows));tracked=rows+[native/'engine1960.c',native/'build_native1960.py',native/'residual_blocks1961.c',source/'tests1960/fault1960.c']+sorted(native.glob('*.comp'));pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*rows],'pipeline-compile')
    output=run([jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx1536m','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.ResidentPipeline1976Test'],'pipeline-run')
    match=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
    if not match:raise AssertionError('missing executed whole pipeline result')
    report=json.loads(match.group(1));report.update(physical_android_tested=False,device_speedup_verified=False,host_timing_admission_fixture=True,actual_whole_pipeline_jni_verified=True,actual_layout_halo_equivalence_verified=True,source_hashes=pins)
    if report.get('status')!='passed' or report.get('assertions',0)<=0:raise AssertionError('no passing assertions')
    if pins!={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('production graph changed during replay')
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');(work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');return report
if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
