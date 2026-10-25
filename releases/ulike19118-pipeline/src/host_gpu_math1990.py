#!/usr/bin/env python3
"""Strong exact-program differential executed through real JNI and software GLES.

CPU images/model/maps/confidence come from byte-frozen published Java.  Tests
include quantization boundaries, transparency, masks, all 12 exact full kernels,
preparation variants, physical-width strips, both banks and cropped source halos.
This is not a physical Galaxy test and does not claim its mismatch root cause.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,shutil,subprocess,sys
ROOT=Path(__file__).resolve().parent

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    sys.path.insert(0,str(source))
    from host_speed1959 import _sources
    fixtures=source/'tests1990/gpu_math';native=source/'native1960';reference=source/'tests1960/published1959-reference'
    jdk=Path(jdk or source.parent/'tools/jdk-21.0.8+9').resolve()
    ndk=Path(ndk or source.parent/'tools/android-ndk-r27c').resolve()
    java,javac=jdk/'bin/java',jdk/'bin/javac'
    if sha(reference/'pins.json')!='0510bf56f58a873563cdac95168d8f1d5b223f79a98a36ac01315e9193963575':raise AssertionError('Frozen published CPU manifest changed')
    for name,pin in json.loads((reference/'pins.json').read_text())['files'].items():
        if sha(reference/name)!=pin['sha256'] or (reference/name).stat().st_size!=pin['bytes']:raise AssertionError('Frozen CPU source changed: '+name)
    environment=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    commands=[]
    def run(command,name,negative=False):
        command=list(map(str,command));commands.append({'name':name,'argv':command})
        result=subprocess.run(command,capture_output=True,text=True,env=environment,timeout=600)
        output=result.stdout+result.stderr;(work/(name+'.log')).write_text(output)
        if 'WARNING in native method' in output or 'FATAL ERROR in native method' in output:raise AssertionError('JNI checker warning: '+name)
        if negative:
            if result.returncode==0 or 'deliberate corrupted oracle rejected' not in output:raise AssertionError('Corrupt oracle negative control did not fail')
        elif result.returncode:raise RuntimeError(name+' failed: '+output[-8000:])
        return output
    for directory in ('reference','classes','headers'):(work/directory).mkdir(exist_ok=True)
    graphics=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
    for name in ('EGL','GLES3','KHR'):shutil.copytree(graphics/name,work/'headers'/name,dirs_exist_ok=True)
    sources=_sources(reference)+[reference/'NativeSpeed1935.java',reference/'FastPixels1933.java',fixtures/'FuzzOracle1990.java']
    run([javac,'--release','8','-d',work/'reference',*sources],'oracle-compile')
    oracle=json.loads(run([java,'-XX:ActiveProcessorCount=4','-cp',work/'reference','com.hiro.ulike.FuzzOracle1990',work/'oracle.bin'],'oracle').strip().splitlines()[-1])
    if oracle!={'records':1016,'pixels':5896956}:raise AssertionError('Numerical corpus changed unexpectedly')
    spec=importlib.util.spec_from_file_location('native_math1990',native/'build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(native,work)
    tracked=[source/'GpuNoise1960.java',native/'engine1960.c',native/'build_native1960.py',native/'compute_dependencies1990.json',fixtures/'FuzzOracle1990.java',fixtures/'FuzzReplay1990.java',source/'host_gpu_math1990.py',fixtures/'ConfidenceTail1990.java',fixtures/'reference89/strong1960.comp']+sorted(native.glob('*.comp'))
    pins={str(path.relative_to(source)):sha(path) for path in tracked}
    library=work/'libulike_gpu1960.so'
    run(['cc','-std=c11','-O3','-shared','-fPIC','-DANDROID','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(work/'headers'),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),native/'engine1960.c','-l:libEGL.so.1','-l:libGL.so.1','-o',library],'native-compile')
    # The facade's newer memory diagnostics are orthogonal to this numerical
    # fixture. Real source/transport/JNI run unchanged; the holder returns no
    # retained CPU-native pool, exactly as the old scalar stub did.
    stubs=(source/'native75testfixtures/FacadeStubs.java').read_text().replace('final class SpeedWorkers1935{','final class SpeedWorkers1935{static long nativeRetainedBytes1989(long[] values,int offset){return 0;}')
    (work/'FacadeStubs.java').write_text(stubs)
    run([javac,'--release','8','-d',work/'classes',source/'GpuNoise1960.java',work/'FacadeStubs.java',fixtures/'FuzzReplay1990.java',fixtures/'ConfidenceTail1990.java'],'replay-compile')
    prefix=[java,'-Xcheck:jni','-Djava.library.path='+str(work),'-cp',work/'classes','com.hiro.ulike.FuzzReplay1990',work/'oracle.bin']
    output=run(prefix,'replay')
    match=re.search(r'RESULT records=(\d+) reads=(\d+) pixelsDiff=(\d+) confidenceDiff=(\d+)',output)
    if not match or int(match[1])!=1016 or int(match[2])!=18240 or int(match[3]) or int(match[4]) or 'UNSUPPORTED ' in output:raise AssertionError('Incomplete or inexact shader matrix: '+output[-8000:])
    run(prefix+['corrupt-oracle'],'negative-control',True)
    tail=[java,'-Xcheck:jni','-Djava.library.path='+str(work),'-cp',work/'classes','com.hiro.ulike.ConfidenceTail1990',work/'oracle.bin']
    tail_output=run(tail,'confidence-tail-current')
    if 'confidenceMismatches=0 columns=17' not in tail_output:raise AssertionError('Current partial confidence cells not initialized')
    historical=fixtures/'reference89/strong1960.comp'
    if sha(historical)!='bfaeabf930c42769ae86912928d0f1b9fc934c4b3fa58faa73e1c41cbfaf7cbe':raise AssertionError('Frozen published89 Strong shader changed')
    baseline_work=work/'baseline-build';baseline_work.mkdir(exist_ok=True)
    # Only the frozen Strong shader is substituted for this negative control.
    # Preserve current native transport; never ask the production dependency
    # checker to certify an incomplete synthetic source tree.
    header=(work/'shader_sources1960.h').read_text()
    def literal(text):return 'static const char strong1960_source[]=\n'+''.join(json.dumps(line+'\n')+'\n' for line in text.splitlines())+';\n'
    current_literal=literal((native/'strong1960.comp').read_text())
    if header.count(current_literal)!=1:raise AssertionError('Generated Strong source header boundary changed')
    header=header.replace(current_literal,literal(historical.read_text()))
    digest=hashlib.sha256(header.encode()).hexdigest()
    header=re.sub(r'#define GPU1960_SOURCE_SHA256 "[0-9a-f]+"','#define GPU1960_SOURCE_SHA256 "'+digest+'"',header)
    (baseline_work/'shader_sources1960.h').write_text(header)
    run(['cc','-std=c11','-O3','-shared','-fPIC','-DANDROID','-fno-fast-math','-ffp-contract=off','-I'+str(work/'headers'),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(baseline_work),native/'engine1960.c','-l:libEGL.so.1','-l:libGL.so.1','-o',baseline_work/'libulike_gpu1960.so'],'baseline-native-compile')
    historical_tail=run([java,'-Xcheck:jni','-Djava.library.path='+str(baseline_work),'-cp',work/'classes','com.hiro.ulike.ConfidenceTail1990',work/'oracle.bin','baseline'],'confidence-tail-published89-negative')
    if 'confidenceMismatches=17 columns=17 first=10203040' not in historical_tail:raise AssertionError('Published89 missing-write negative control absent')
    if pins!={str(path.relative_to(source)):sha(path) for path in tracked}:raise AssertionError('Production source changed during numerical proof')
    report=dict(status='passed',assertions=int(match[2])+1,records=1016,unique_oracle_pixels=5896956,actual_jni_reads=int(match[2]),argb_differences=0,confidence_differences=0,
        exact_program_ids=list(range(39,51)),preparation_program_ids=list(range(27,30))+list(range(31,34))+list(range(35,38))+list(range(51,60)),
        all_nine_exact_preparation_programs_verified=True,
        cropped_source_storage_origin_tested=True,rebased_source_origin_tested=True,source_widths_3060_4080_4284_tested=True,banks=[0,1],
        corrupted_oracle_negative_control=True,physical_android_tested=False,device_speedup_verified=False,galaxy_mismatch_root_cause_confirmed=False,
        legacy_float_math_changed=False,strict_preparation_programs_appended=True,sparse_confidence_tail_fixed=True,published89_undefined_confidence_reproduced=True,production_native_changed=True,frozen_java_cpu_oracle_sha256=sha(work/'oracle.bin'),source_sha256=pins,native_sha256=sha(library),gl_environment=output.split('ENV ',1)[1].splitlines()[0])
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',type=Path,default=ROOT);parser.add_argument('--work',type=Path,required=True);parser.add_argument('--jdk',type=Path);parser.add_argument('--ndk',type=Path);args=parser.parse_args()
    print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),indent=2))
