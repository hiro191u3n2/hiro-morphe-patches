#!/usr/bin/env python3
"""Execute appended pow2 and retained Strong programs in actual JNI/Mesa.
Frozen published Java supplies ARGB/confidence oracles; no Android speed claim.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, shutil, subprocess
ROOT = Path(__file__).resolve().parent
def sha(p): return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    java=Path(jdk).resolve()/'bin/java' if jdk else Path(shutil.which('java')).resolve();javac=java.parent/'javac';jdk_root=java.parent.parent
    reference=source/'tests1960/published1959-reference';native=source/'native1960'
    if sha(reference/'pins.json')!='0510bf56f58a873563cdac95168d8f1d5b223f79a98a36ac01315e9193963575':raise AssertionError('Frozen published CPU manifest changed')
    for name,pin in json.loads((reference/'pins.json').read_text())['files'].items():
        if sha(reference/name)!=pin['sha256'] or (reference/name).stat().st_size!=pin['bytes']:raise AssertionError('Frozen CPU source changed: '+name)
    fixtures=source/'gpu78-fixtures';oracle_sources=module('speed76',source/'host_speed1959.py')._sources(reference)+[reference/'NativeSpeed1935.java',reference/'FastPixels1933.java',source/'tests1960/Oracle1960.java']
    tracked=[source/'GpuNoise1960.java',native/'engine1960.c',native/'build_native1960.py',fixtures/'NativeBankReplay1978.java']+sorted(native.glob('*.comp'))
    pins={str(p.relative_to(source)):sha(p) for p in tracked}
    def run(command,label,timeout=300):
        result=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=timeout,env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1'))
        (work/(label+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode or 'WARNING in native method' in result.stdout+result.stderr or 'FATAL ERROR' in result.stdout+result.stderr:raise RuntimeError(label+': '+(result.stdout+result.stderr)[-9000:])
        return result.stdout
    classes=work/'classes';classes.mkdir(exist_ok=True);refclasses=work/'reference';refclasses.mkdir(exist_ok=True)
    run([javac,'--release','8','-d',refclasses,*oracle_sources],'oracle-compile')
    oracle=json.loads(run([java,'-XX:ActiveProcessorCount=4','-Djava.library.path='+str(work/'unavailable'),'-cp',refclasses,'com.hiro.ulike.Oracle1960',work/'oracle.bin'],'oracle-generate').strip().splitlines()[-1])
    if oracle['referenceNativeEnabled'] is not False or oracle['records']!=152:raise AssertionError('Frozen CPU oracle unavailable')
    module('builder76',native/'build_native1960.py').shader_header(native,work)
    run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(jdk_root/'include'),'-I'+str(jdk_root/'include/linux'),'-I'+str(work),native/'engine1960.c','-l:libEGL.so.1','-l:libGLESv2.so.2','-o',work/'libulike_gpu1960.so'],'native-compile')
    run([javac,'--release','8','-d',classes,source/'GpuNoise1960.java',source/'native75testfixtures/FacadeStubs.java',fixtures/'NativeBankReplay1978.java'],'bank-compile')
    output=run([java,'-Xcheck:jni','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.NativeBankReplay1978',work/'oracle.bin'],'bank-replay')
    match=re.search(r'RESULT records=(\d+) reads=(\d+) pixelsDiff=(\d+) confidenceDiff=(\d+)',output)
    if not match or tuple(map(int,match.groups()))!=(16,864,0,0) or 'UNSUPPORTED ' in output:raise AssertionError('Exact tiled old/new replay mismatch: '+output[-9000:])
    if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('GPU production graph changed during replay')
    report=dict(status='passed',assertions=864,actual_program_ids=[0,9,18,30,34,38,39,40,41,42,43,44,45,46,47,48,49,50],actual_jni_bank_reads=864,
        pow2_exact1978_actual_jni_verified=True,retained_strong_programs1978_exact_verified=True,direct_policy1978_full_exact_verified=True,full_argb_confidence_exact=True,cropped_halo_and_two_banks=True,
        physical_android_tested=False,device_speedup_verified=False,frozen_cpu_oracle=oracle,oracle_payload_sha256=sha(work/'oracle.bin'),source_sha256=pins)
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
