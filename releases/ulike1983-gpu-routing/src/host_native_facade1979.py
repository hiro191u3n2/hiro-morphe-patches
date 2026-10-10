#!/usr/bin/env python3
"""Actual released Strong facade/JNI/Mesa regression versus frozen published Java.
Model/Android/preference holders are fixtures; engine/shaders/transport/commands
execute for real. This report is not a physical Android or speedup measurement.
Requires a C compiler and system Mesa EGL/GLES libraries/development headers.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,shutil,subprocess,sys
ROOT=Path(__file__).resolve().parent

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
    source=Path(source).resolve();sys.path.insert(0,str(source));work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    fixtures=source/'gpu79-fixtures'
    java=Path(jdk).resolve()/'bin/java' if jdk else Path(shutil.which('java') or '/usr/lib/jvm/java-17-openjdk-amd64/bin/java').resolve()
    javac=java.parent/'javac';jdk_root=java.parent.parent
    native=source/'native1960';reference=source/'tests1960/published1959-reference'
    if sha(reference/'pins.json')!='0510bf56f58a873563cdac95168d8f1d5b223f79a98a36ac01315e9193963575':raise AssertionError('Frozen published .59 pin manifest changed')
    reference_pins=json.loads((reference/'pins.json').read_text())
    for name,expected in reference_pins['files'].items():
        if sha(reference/name)!=expected['sha256'] or (reference/name).stat().st_size!=expected['bytes']:raise AssertionError('Frozen published .59 CPU reference changed: '+name)
    native_headers=[]
    if ndk:
        graphics=Path(ndk).resolve()/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
        for name in ('EGL','GLES3','KHR'):shutil.copytree(graphics/name,work/'headers'/name,dirs_exist_ok=True)
        native_headers=['-I'+str(work/'headers')]
    def module(name,path):
        spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
    speed=module('native_facade_speed_reference1971',source/'host_speed1959.py')
    builder=module('native_facade_builder1971',native/'build_native1960.py')
    oracle_sources=speed._sources(reference)+[reference/'NativeSpeed1935.java',reference/'FastPixels1933.java',source/'tests1960/Oracle1960.java']
    production=[source/'GpuStrong1960.java',source/'GpuNoise1960.java',native/'engine1960.c',native/'build_native1960.py',source/'host_speed1959.py',source/'tests1960/Oracle1960.java']+sorted(native.glob('*.comp'))+sorted(source.glob('*.java'))+sorted((source/'quality-dependencies').rglob('*.java'))+[native/'residual_blocks1961.c',source/'native1955/single_noise1955.c',reference/'pins.json']+oracle_sources+[fixtures/name for name in ('NativeFixtures1979.java','NativeFacadeReplay1979.java','NativeSpeed1979Test.java','native_fault1979.c')]
    source_pins={str(p.relative_to(source)) if p.is_relative_to(source) else 'native76testfixtures/'+p.name:sha(p) for p in production}
    commands=[]
    def run(cmd,label,timeout=180):
        cmd=[str(x) for x in cmd];commands.append(dict(label=label,argv=cmd))
        p=subprocess.run(cmd,capture_output=True,text=True,timeout=timeout,env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1'))
        (work/(label+'.log')).write_text(p.stdout+p.stderr)
        if p.returncode:raise RuntimeError(label+' failed: '+(p.stdout+p.stderr)[-6000:])
        if '-Xcheck:jni' in cmd and ('WARNING in native method' in p.stdout+p.stderr or 'FATAL ERROR' in p.stdout+p.stderr):raise AssertionError('JNI checker reported a failure: '+label)
        return p.stdout
    refclasses=work/'reference';classes=work/'classes'
    for directory in (refclasses,classes):
        if directory.exists():shutil.rmtree(directory)
        directory.mkdir()
    compiler=[str(javac)] if javac.is_file() else [str(java),'-m','jdk.compiler/com.sun.tools.javac.Main']
    run(compiler+['-source','8','-target','8','-Xlint:-options','-d',refclasses,*oracle_sources],'oracle-compile')
    oracle_output=run([java,'-XX:ActiveProcessorCount=4','-Djava.library.path='+str(work/'unavailable'),'-cp',refclasses,'com.hiro.ulike.Oracle1960',work/'oracle.bin'],'oracle-generate')
    oracle_report=json.loads(oracle_output.strip().splitlines()[-1])
    if oracle_report.get('referenceNativeEnabled') is not False or oracle_report.get('records')!=152:raise AssertionError('Independent frozen Java reference absent')
    builder.shader_header(native,work)
    run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off',*native_headers,'-I'+str(jdk_root/'include'),'-I'+str(jdk_root/'include/linux'),'-I'+str(work),native/'engine1960.c',fixtures/'native_fault1979.c','-Wl,--wrap=glDispatchCompute','-Wl,--wrap=glMapBufferRange','-l:libEGL.so.1','-l:libGLESv2.so.2','-o',work/'libulike_gpu1960.so'],'native-compile')
    run(compiler+['-source','8','-target','8','-Xlint:-options','-d',classes,source/'GpuNoise1960.java',source/'GpuStrong1960.java',source/'GpuStrongRouting1978.java',source/'SpeedWorkers1935.java',fixtures/'NativeFixtures1979.java',fixtures/'NativeFacadeReplay1979.java',fixtures/'NativeSpeed1979Test.java'],'facade-compile')
    prefix=[java,'-XX:ActiveProcessorCount=4','-Xcheck:jni','-Djava.library.path='+str(work),'-cp',classes]
    out=run(prefix+['com.hiro.ulike.NativeFacadeReplay1979',work/'oracle.bin'],'facade-replay')
    m=re.search(r'RESULT records=(\d+) captures=(\d+) oracleCalls=(\d+) assertions=(\d+)',out)
    if not m:raise AssertionError(out)
    records,captures,oracles,assertions=map(int,m.groups())
    if (records,captures,oracles)!=(16,1536,720):raise AssertionError('Production facade coverage incomplete')
    controls={}
    for mode in ('deferred','reuse','false','throw','interrupt','readback','memory','accept','reject','cpu-accept','cpu-reject','route-mixed','route-workers','route-resident','cohort-memory','cohort-cancel','cohort-argb','cohort-confidence'):
        checked=run(prefix+['com.hiro.ulike.NativeSpeed1979Test',work/'oracle.bin',mode],'control-'+mode,timeout=180)
        controls[mode]=json.loads(checked.strip().splitlines()[-1])
    final_pins={str(p.relative_to(source)) if p.is_relative_to(source) else 'native76testfixtures/'+p.name:sha(p) for p in production}
    if source_pins!=final_pins:raise AssertionError('Source graph changed during native replay')
    report=dict(status='passed',assertions=assertions+sum(v['assertions'] for v in controls.values()),tests=controls,
        physical_android_tested=False,device_speedup_verified=False,controlled_contention=True,
        new_profiles_idle_only1979_verified=True,legacy_foreground_exact2_native1979_verified=True,certified_readback_reuse1978_verified=True,
        foreground_failure_cancel_memory1978_verified=True,private_partial_output1978_not_committed=True,
        parallel_cohort1978_gate_verified=True,actual_production_workers1978_executed=True,
        cohort_cancel_memory_and_mismatch1978_verified=True,mixed_route_admission1978_verified=True,
        actual_parallel_cpu_fallback1978_verified=True,
        direct_policy_command_graph1978_actual_jni=True,
        source_hashes=source_pins,frozen_java_reference=oracle_report,reference_payload_sha256=sha(work/'oracle.bin'),
        records=records,captures=captures,oracle_calls=oracles,foreground_new_profile_gpu_proofs=0,foreground_legacy_bootstrap_groups=144,
        complete_argb_confidence_exact=True,pooled_confidence_tails_untouched=True,
        gl_environment=out.split('ENV ',1)[1].splitlines()[0],jni_check_enabled=True)
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
