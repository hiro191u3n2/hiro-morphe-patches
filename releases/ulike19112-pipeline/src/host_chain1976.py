#!/usr/bin/env python3
"""Actual finish-pipeline and resident-slot JNI pixels against frozen .60 CPU."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, shutil, subprocess, sys
ROOT=Path(__file__).resolve().parent
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def current_sources1976(root,work):
    root,work=Path(root).resolve(),Path(work).resolve();sys.path.insert(0,str(root));work.mkdir(parents=True,exist_ok=True)
    from host_gpu1962 import all_sources
    sources=all_sources(root)
    face=root/'tests/pipeline1942-fixtures/com/hiro/ulike/FaceRegions1934.java'
    replacement=work/'FaceRegions1934.java'
    face_text=face.read_text().replace('public static int calls;', 'public static int calls; public static int interruptSamples1976;')
    face_text=face_text.replace('public static final class Mask implements QualityPixels1932.RegionMask {','public static final class Mask implements QualityPixels1932.RegionMask {public long retainedBytes1976(){return 192L;}')
    face_text=face_text.replace('public int skinQ8(int x,int y){return', 'public int skinQ8(int x,int y){if(interruptSamples1976>0&&--interruptSamples1976==0)Thread.currentThread().interrupt();return')
    replacement.write_text(face_text)
    sources=[replacement if p==face else p for p in sources]
    trace=work/'CameraTrace1965.java';trace.write_text('package com.hiro.ulike; final class CameraTrace1965 {static void event(String a,long b,String c){}}\n')
    for name in ('GpuResident1976','GpuStrongLayout1976','GpuStrongTuning1975','PairedRegions1976','ColourCache1976','CpuFinishCache1976'):
        sources.append(root/(name+'.java'))
    sources.append(trace)
    return list(dict.fromkeys(sources))
def test(source,work,jdk=None,ndk=None):
    root,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True);sys.path.insert(0,str(root))
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
    from host_gpu1961 import PIN_SHA
    from host_gpu1962 import all_sources
    reference=root/'tests1961/published1960-reference'
    if sha(reference/'pins.json')!=PIN_SHA:raise AssertionError('Frozen chain CPU manifest changed')
    for name,pin in json.loads((reference/'pins.json').read_text())['files'].items():
        if sha(reference/name)!=pin['sha256']:raise AssertionError('Frozen CPU source changed '+name)
    def run(command,label,env=None,timeout=420):
        p=subprocess.run(list(map(str,command)),capture_output=True,text=True,env=env,timeout=timeout)
        (work/(label+'.log')).write_text(p.stdout+p.stderr)
        if p.returncode or 'WARNING in native method' in p.stdout+p.stderr or 'FATAL ERROR' in p.stdout+p.stderr:raise AssertionError(label+'\n'+(p.stdout+p.stderr)[-10000:])
        found=re.search(r'^RESULT (\{[^\n]+\})$',p.stdout,re.M);return json.loads(found.group(1)) if found else None
    ref=work/'reference';current=work/'classes';native=work/'native'
    for p in (ref,current,native):p.mkdir(exist_ok=True)
    oracle=root/'tests1962/Chain1962Oracle.java';candidate=root/'tests1976/Chain1976Test.java'
    reference_sources=[p for p in all_sources(reference,True) if p.name!='FastResize1933.java']+[reference/'FastResize1933.java']
    run([jdk/'bin/javac','--release','8','-d',ref,*reference_sources,oracle],'reference-compile')
    records=work/'expected.bin';expected=run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Djava.library.path='+str(work/'unavailable'),'-cp',ref,'com.hiro.ulike.Chain1962Oracle',records],'reference-run')
    sources=current_sources1976(root,work/'fixtures')+[oracle,candidate,root/'tests1960/Native1960Test.java']
    tracked=sources+[root/'native1960/engine1960.c',root/'native1960/build_native1960.py',root/'native1960/residual_blocks1961.c']+sorted((root/'native1960').glob('*.comp'))
    before={str(p):sha(p) for p in tracked}
    (work/'compiled-source-pins.json').write_text(json.dumps(before,indent=2)+'\n')
    run([jdk/'bin/javac','--release','8','-d',current,*sources],'current-compile')
    headers=native/'headers';graphics=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
    for name in ('EGL','GLES3','KHR'):shutil.copytree(graphics/name,headers/name,dirs_exist_ok=True)
    spec=importlib.util.spec_from_file_location('chain76builder',root/'native1960/build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(root/'native1960',native)
    run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-DANDROID','-I'+str(headers),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(native),root/'native1960/engine1960.c',root/'native1960/residual_blocks1961.c',root/'tests1960/fault1960.c','-Wl,--wrap=glClientWaitSync','-Wl,--wrap=glUnmapBuffer','-Wl,--wrap=glDispatchCompute','-Wl,--no-undefined','-l:libEGL.so.1','-l:libGL.so.1','-lm','-o',native/'libulike_gpu1960.so'],'native-compile')
    env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1');prefix=[jdk/'bin/java','-XX:ActiveProcessorCount=4','-Xcheck:jni','-Djava.library.path='+str(native),'-cp',current,'com.hiro.ulike.Chain1976Test',records]
    reports={mode:run(prefix+[mode],mode,env) for mode in ('pixels','failure','cancel','certified_cancel','choice','timeout','unsupported')}
    if reports['pixels']['cases']!=288 or reports['pixels']['variants']!=1152 or reports['pixels']['legacyCases']!=24 or reports['pixels']['exactPixels']<1000000:raise AssertionError('Full pipeline/resident coverage absent')
    changed=[str(p) for p in tracked if before[str(p)]!=sha(p)]
    if changed:raise AssertionError('Production chain graph changed during tests: '+str(changed))
    report=dict(status='passed',assertions=sum(r['assertions'] for r in reports.values()),tests=reports,oracle=expected,oracle_sha256=sha(records),source_sha256=before,
        chain_pipeline1976_actual_jni=True,chain_resident1976_actual_jni=True,chain_legacy_sync1976_exact_verified=True,chain_measured_old_gpu_baseline1976_verified=True,chain_legacy_fallback1976_actual_jni=True,chain_source_immutable1976_verified=True,chain_fault_cancel_drain1976_verified=True,chain_cancel_preserves_exact_certificate1976_verified=True,chain_unsupported_formats1976_verified=True,physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
