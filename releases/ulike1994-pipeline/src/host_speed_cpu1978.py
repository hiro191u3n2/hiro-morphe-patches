#!/usr/bin/env python3
"""Execute .78 exact CPU candidates against independent pinned .77 production.

The two source closures run in separate JVMs with separately built JNI libraries.
Explicit test-local certificates exercise candidates; a separate real-queue
suite verifies actual admission and rejection. No host timing proves an Android
speedup. Historical .76 gates remain nested, with their original assertions.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,shutil,subprocess,sys

ROOT=Path(__file__).resolve().parent
REFERENCE_PIN='63d522e3f043f92e0eb2299710686a9c30216d225245daa60d3b6551d7092396'
BASE_MPP='20ca20e1dedc69a9831d2671f6d9189a8f18410dcd95ad1739e59519c1276bed'
MANDATORY_FLAGS={
    'differential':('cpu77_all_argb_model_floatbits_exact','cpu77_known_and_unknown_policy_exact','cpu78_actual_jni_foreground_executed'),
    'qualification':('cpu78_idle_exact2_speed_gate_verified','cpu78_probe_cancel_memory_ownership_verified','cpu78_original_instability_and_floatbits_rejected'),
    'ownership':('cpu78_native_lease_fault_cancel_verified','cpu78_native_foreign_release_and_generation_verified','cpu78_native_budget_and_stage_close_verified','cpu78_four_exclusive_workers_verified'),
    'policy_ownership':('cpu78_policy_parallel_unknown_and_scoped_exact','cpu78_policy_cancel_join_and_budget_verified','cpu78_policy_immutable_snapshot_accounted'),
    'arm_workspace':('cpu78_actual_arm_workspace_exact','cpu78_arm_workspace_colour_composition_exact'),
}
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def module(name,p):
    spec=importlib.util.spec_from_file_location(name,p);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
def run(command,log,timeout=300):
    result=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=timeout)
    Path(log).write_text(result.stdout+result.stderr)
    if result.returncode or 'WARNING in native method' in result.stdout+result.stderr or 'FATAL ERROR' in result.stdout+result.stderr:
        raise AssertionError(str(log)+': '+(result.stdout+result.stderr)[-14000:])
    return result.stdout
def android_jar(jdk):
    candidates=[Path(os.environ['ULIKE_ANDROID_JAR'])] if os.environ.get('ULIKE_ANDROID_JAR') else []
    candidates += [jdk.parent.parent/'tools/android.jar',Path.cwd()/'tools/android.jar']
    for p in candidates:
        if p.is_file():return p.resolve()
    raise AssertionError('Set ULIKE_ANDROID_JAR to the pinned build tools/android.jar')
def reference(source):
    root=source/'tests1978/cpu77-reference';p=root/'pins.json'
    if sha(p)!=REFERENCE_PIN:raise AssertionError('Independent .77 manifest changed')
    data=json.loads(p.read_text())
    if data['baseline_version']!='1.9.77' or data['baseline_mpp_sha256']!=BASE_MPP:raise AssertionError('Wrong CPU reference release')
    for name,pin in data['files'].items():
        if sha(root/name)!=pin['sha256'] or (root/name).stat().st_size!=pin['bytes']:raise AssertionError('Changed frozen .77 source '+name)
    return root,data
def differential(source,work,jdk):
    work.mkdir(parents=True,exist_ok=True);ref,pins=reference(source);jar=android_jar(jdk)
    builder=module('cpu78_builder',source/'build1978.py')
    production=list(builder.compile_inputs().values())+[builder.production_path(n) for n in builder.PRODUCTION]
    current_pins={p.relative_to(source).as_posix():sha(p) for p in production}
    driver=source/'tests1978/Cpu77Differential1978.java';driver_sha=sha(driver)
    results={};dumps={};libraries={};classes={}
    flags=['-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-fPIC','-shared','-Wl,--no-undefined','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux')]
    native_names={'native1955/single_noise1955.c':'libulike_nr1955.so','native1958/smooth_noise1958.c':'libulike_smooth1958.so','native1951/moire1951.c':'libulike_moire1951.so'}
    for label,root,inputs in [('published77',ref,[ref/n for n in pins['java']]),('candidate78',source,production)]:
        target=work/(label+'-classes');target.mkdir();classes[label]=target
        run([jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-bootclasspath',jar,'-d',target,*inputs],work/(label+'-compile.log'))
        run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',str(target)+os.pathsep+str(jar),'-d',target,driver],work/(label+'-driver.log'))
        native=work/(label+'-native');native.mkdir();libraries[label]={}
        for name,lib in native_names.items():
            run(['cc',*flags,root/name,'-lm','-pthread','-o',native/lib],work/(label+'-'+lib+'.log'))
            libraries[label][lib]={'sha256':sha(native/lib),'bytes':(native/lib).stat().st_size,'source_sha256':sha(root/name)}
        for backend,path in [('java',work/'unavailable'),('jni',native)]:
            key=label+'-'+backend;dump=work/(key+'.bin')
            stdout=run([jdk/'bin/java','-ea','-Xcheck:jni','-Xmx1g','-XX:ActiveProcessorCount=4','-Djava.library.path='+str(path),'-cp',str(target)+os.pathsep+str(jar),'com.hiro.ulike.Cpu77Differential1978',dump,str(label=='candidate78').lower(),str(backend=='jni').lower()],work/(key+'.log'),600)
            result=json.loads(stdout.strip().splitlines()[-1]);results[key]=result;dumps[key]=dump
            if result['status']!='passed' or result['single_cases']<60 or result['strong_cases']<20 or result['float_bits']<1000 or result['policy_integers']<100000:raise AssertionError('Insufficient complete CPU differential '+key)
    comparisons=[]
    for backend in ('java','jni'):
        a=dumps['published77-'+backend].read_bytes();b=dumps['candidate78-'+backend].read_bytes()
        if a!=b:
            limit=min(len(a),len(b));first=next((i for i in range(limit) if a[i]!=b[i]),limit)
            raise AssertionError('Published .77 '+backend+' binary mismatch byte '+str(first)+' / sizes '+str((len(a),len(b))))
        comparisons.append({'backend':backend,'bytes':len(b),'sha256':sha(dumps['candidate78-'+backend]),'differing_bytes':0})
    if current_pins!={p.relative_to(source).as_posix():sha(p) for p in production} or driver_sha!=sha(driver):raise AssertionError('Actual CPU compilation closure changed during differential')
    reference(source)
    report={'status':'passed','assertions':sum(results['candidate78-'+b]['assertions'] for b in ('java','jni')),'cpu77_all_argb_model_floatbits_exact':True,'cpu77_known_and_unknown_policy_exact':True,'cpu78_actual_jni_foreground_executed':True,'separate_jvms_and_native_libraries':True,'baseline_version':'1.9.77','baseline_mpp_sha256':BASE_MPP,'baseline_manifest_sha256':REFERENCE_PIN,'production_source_sha256':current_pins,'fixture_sha256':driver_sha,'android_jar_sha256':sha(jar),'fresh_libraries':libraries,'runs':results,'comparisons':comparisons,'physical_android_tested':False,'device_speedup_verified':False}
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
def qualification(source,work,jdk):
    work.mkdir(parents=True,exist_ok=True);generated=work/'fixtures';generated.mkdir();classes=work/'classes';classes.mkdir()
    base=module('cpu78_android_queue',source/'host_qualification1967.py');paths=[]
    for name,text in base.FIXTURES.items():
        if not name.startswith('android/'):continue
        p=generated/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);paths.append(p)
    peers=generated/'CpuGatePeers1978.java';peers.write_text('''package com.hiro.ulike;
final class ProcessingTiming1947 {static volatile long epoch=1;static long captureEpoch1953(){return epoch;}}
final class SaveQueue1935 {static volatile boolean idle;static boolean idle1953(){return idle;}}
final class WholeRoute1953 {static long retainedBytes(){return 0;}}
final class SpeedWorkers1935 {static int maxWorkers(){return 2;}static int availableWorkers1944(){return 2;}static void run(Runnable[] a){for(Runnable r:a)r.run();}}
final class GpuNoise1960 {static volatile boolean budget=true;static String fingerprint(){return "cpu78-actual-queue-fixture";}static boolean sessionBusy(){return false;}static boolean workspaceFits(long bytes){return budget&&bytes<512L*1024*1024;}}
''');paths.append(peers)
    inputs=[source/'CpuExact1978.java',source/'GpuQualification1961.java',source/'tests1978/CpuExactGate1978Test.java'];pins={p.relative_to(source).as_posix():sha(p) for p in inputs}
    run([jdk/'bin/javac','--release','8','-d',classes,*paths,*inputs],work/'compile.log')
    report=json.loads(run([jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.CpuExactGate1978Test'],work/'run.log',120).strip().splitlines()[-1])
    if report['status']!='passed' or report['assertions']<30:raise AssertionError('Incomplete CPU admission controls')
    if pins!={p.relative_to(source).as_posix():sha(p) for p in inputs}:raise AssertionError('CPU gate code changed during execution')
    report.update(source_sha256=pins,physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
def ownership(source,work,jdk,production_classes):
    work.mkdir(parents=True,exist_ok=True);classes=work/'classes';classes.mkdir();jar=android_jar(jdk)
    fixture=source/'tests1978/CpuOwnership1978Test.java';native=source/'native1955/single_noise1955.c'
    inputs=[fixture,native,source/'CpuSingle1978.java',source/'SingleNoise1955.java',source/'SpeedWorkers1935.java']
    pins={p.relative_to(source).as_posix():sha(p) for p in inputs}
    injected=work/'observed-native.c'
    injected.write_text('''#include <stdlib.h>
#include <stdint.h>
#include <jni.h>
static _Thread_local int fault_at1978,allocation_at1978;
static void *observed_malloc1978(size_t n) {
    if(fault_at1978&&++allocation_at1978==fault_at1978)return NULL;
    return malloc(n);
}
#define malloc observed_malloc1978
#include "'''+str(native)+'''"
#undef malloc
JNIEXPORT void JNICALL Java_com_hiro_ulike_CpuOwnership1978Test_fault(JNIEnv *e,jclass c,jint n) {
    (void)e;(void)c;fault_at1978=n;allocation_at1978=0;
}
JNIEXPORT jlong JNICALL Java_com_hiro_ulike_CpuOwnership1978Test_nativeOwners(JNIEnv *e,jclass c) {
    (void)e;(void)c;uint64_t slots=0,busy=0;pthread_mutex_lock(&owned_mutex1978);
    for(int i=0;i<4;i++){if(owned1978[i].id)slots++;if(owned1978[i].busy)busy++;}
    uint64_t value=(uint64_t)owned_bytes1978|(slots<<32)|(busy<<40);
    pthread_mutex_unlock(&owned_mutex1978);return (jlong)value;
}
''')
    flags=['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-pthread','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux')]
    library=work/'libulike_nr1955.so'
    run([*flags,injected,'-lm','-o',library],work/'native-build.log')
    cp=str(production_classes)+os.pathsep+str(jar)
    run([jdk/'bin/javac','--release','8','-cp',cp,'-d',classes,fixture],work/'compile.log')
    report=json.loads(run([jdk/'bin/java','-ea','-Xcheck:jni','-Xmx1g','-XX:ActiveProcessorCount=4','-Djava.library.path='+str(work),'-cp',str(classes)+os.pathsep+cp,'com.hiro.ulike.CpuOwnership1978Test'],work/'run.log',180).strip().splitlines()[-1])
    if report['status']!='passed' or report['pixels_compared']<60000:raise AssertionError('Incomplete actual native ownership exercise')
    if pins!={p.relative_to(source).as_posix():sha(p) for p in inputs}:raise AssertionError('CPU ownership source changed during execution')
    report.update(source_sha256=pins,instrumentation_sha256=sha(injected),native_library_sha256=sha(library),actual_jni_executed=True,physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
def policy_ownership(source,work,jdk,production_classes):
    work.mkdir(parents=True,exist_ok=True);classes=work/'classes';classes.mkdir();jar=android_jar(jdk)
    fixture=source/'tests1978/CpuFinishOwnership1978Test.java'
    inputs=[fixture,source/'tests1978/Cpu77Differential1978.java',source/'CpuFinishPolicy1978.java',source/'FinishPolicy1953.java',source/'NativeMoire1951.java',source/'QualityPixels1932.java',source/'SpeedWorkers1935.java']
    pins={p.relative_to(source).as_posix():sha(p) for p in inputs};cp=str(production_classes)+os.pathsep+str(jar)
    run([jdk/'bin/javac','--release','8','-cp',cp,'-d',classes,fixture],work/'compile.log')
    report=json.loads(run([jdk/'bin/java','-ea','-Xmx1g','-XX:ActiveProcessorCount=4','-cp',str(classes)+os.pathsep+cp,'com.hiro.ulike.CpuFinishOwnership1978Test'],work/'run.log',120).strip().splitlines()[-1])
    if report['status']!='passed' or report['assertions']<30000:raise AssertionError('Incomplete policy ownership exercise')
    if pins!={p.relative_to(source).as_posix():sha(p) for p in inputs}:raise AssertionError('Policy ownership source changed during execution')
    report.update(source_sha256=pins,actual_production_workers_executed=True,physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
def arm_workspace(source,work,jdk):
    work.mkdir(parents=True,exist_ok=True);ref,_=reference(source)
    compiler,qemu=shutil.which('aarch64-linux-gnu-gcc'),shutil.which('qemu-aarch64')
    if not compiler or not qemu:raise AssertionError('Actual ARM64 compiler and qemu required for CPU workspace publish gate')
    fixture=source/'tests1978/cpu_workspace_arm1978.c'
    inputs=[fixture,source/'native1955/single_noise1955.c',source/'native1960/residual_blocks1961.c']
    pins={p.relative_to(source).as_posix():sha(p) for p in inputs};units=[]
    for prefix,root in [('old',ref),('new',source)]:
        single=(root/'native1955/single_noise1955.c').read_text();residual=(root/'native1960/residual_blocks1961.c').read_text()
        if residual.count('static int export_block1961')!=1:raise AssertionError('Unexpected residual source boundary')
        code=single+'\nstatic int export_block1961'+residual.split('static int export_block1961',1)[1]
        namespace='Java_com_hiro_ulike_CpuArm1978_'+prefix+'_'
        code=code.replace('Java_com_hiro_ulike_',namespace)
        code=code.replace(namespace+'SingleNoise1955_processNative','Java_com_hiro_ulike_CpuArm1978_'+prefix+'ProcessBody')
        code=code.replace(namespace+'SingleResidual1961_prepareNative','Java_com_hiro_ulike_CpuArm1978_'+prefix+'Prepare')
        if prefix=='new':
            target='reused_workspace1978=1;'
            if code.count(target)!=1:raise AssertionError('Cannot observe exact native workspace selection')
            code='static long observed_workspace1978;\n'+code.replace(target,target+'observed_workspace1978++;')
        prelude='''\nextern int cpu_arm_colour1978;
'''
        if prefix=='new':prelude+='''static jlong arm_handle1978;static int arm_width1978,arm_rows1978;
long cpu_arm_workspace_calls1978(void){return observed_workspace1978;}
int cpu_arm_close1978(void) {
    @N@CpuSingle1978_releaseWorkspaceNative(NULL,NULL,arm_handle1978);arm_handle1978=0;
    return owned_bytes1978==0&&active_owned1978==NULL;
}
'''.replace('@N@',namespace)
        wrapper='''JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_CpuArm1978_@P@Process(
        JNIEnv *env,jclass cls,jintArray input,jintArray output,
        jint width,jint rows,jint begin,jint end,jint valid_begin,jint valid_end,
        jint origin,jint noise,jboolean shadows,jint model_width,jint model_height,
        jint columns,jint model_rows,jint patch_w,jint patch_h,jfloatArray model,jintArray policy) {
@BEGIN@
    int previous=colour_enabled1976;colour_enabled1976=cpu_arm_colour1978;
    jboolean result=Java_com_hiro_ulike_CpuArm1978_@P@ProcessBody(env,cls,input,output,
        width,rows,begin,end,valid_begin,valid_end,origin,noise,shadows,model_width,model_height,
        columns,model_rows,patch_w,patch_h,model,policy);
    colour_enabled1976=previous;
@END@
    return result;
}
'''
        begin=''
        if prefix=='new':begin='''    if(arm_handle1978&&(width!=arm_width1978||end-begin>arm_rows1978)) {
        @N@CpuSingle1978_releaseWorkspaceNative(env,cls,arm_handle1978);arm_handle1978=0;
    }
    if(!arm_handle1978) {
        arm_handle1978=@N@CpuSingle1978_createWorkspaceNative(env,cls,width,end-begin);
        arm_width1978=width;arm_rows1978=end-begin;
    }
    if(!arm_handle1978||!@N@CpuSingle1978_beginWorkspaceNative(env,cls,arm_handle1978))return JNI_FALSE;
'''.replace('@N@',namespace)
        end='    '+namespace+'CpuSingle1978_endWorkspaceNative(env,cls,arm_handle1978);' if prefix=='new' else ''
        wrapper=wrapper.replace('@P@',prefix).replace('@BEGIN@',begin).replace('@END@',end)
        unit=work/(prefix+'.c');unit.write_text(code+prelude+wrapper);units.append(unit)
    binary=work/'cpu-workspace-arm1978'
    run([compiler,'-std=c11','-O3','-static','-pthread','-Wall','-Wextra','-Werror','-fno-fast-math','-ffp-contract=off','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),*units,fixture,'-lm','-o',binary],work/'arm-compile.log')
    report=json.loads(run([qemu,binary],work/'arm-run.log',300).strip().splitlines()[-1])
    if report['status']!='passed' or report['cases']!=288 or report['actual_workspace_calls']!=report['cases']:raise AssertionError('Incomplete native ARM scope exercise')
    if pins!={p.relative_to(source).as_posix():sha(p) for p in inputs}:raise AssertionError('ARM candidate source changed during execution')
    reference(source)
    report.update(source_sha256=pins,baseline_manifest_sha256=REFERENCE_PIN,generated_units_sha256={p.name:sha(p) for p in units},binary_sha256=sha(binary))
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
def legacy_cpu_masks(source,work,jdk,ndk=None):
    """Keep every historical .76 assertion, adapting only its compile peers.

    The actual .78 native/policy sources remain direct compiler inputs. The
    schedule return type and new CPU policy dependencies are explicit fixture
    contracts; no original test condition or production branch is substituted.
    """
    legacy=module('cpu78_legacy_mask_controls',source/'host_cpu_masks1976.py')
    prior=source/'tests1976/CpuMaskStubs.java';current=source/'tests1978/CpuMaskPeers1978.java'
    historical=[source/n for n in ('host_cpu_masks1976.py','tests1976/CpuMasks1976Test.java','tests1976/NativeSharp1976.java','tests1976/NativeCache1976Test.java','tests1976/CpuMaskStubs.java')]
    pins={p.relative_to(source).as_posix():sha(p) for p in historical};oldrun=legacy.run;adaptations=[]
    def run_current_compile(command,log,timeout=180):
        if str(prior) in [str(v) for v in command]:
            command=[current if str(v)==str(prior) else v for v in command]
            added=[source/n for n in ('CpuExact1978.java','CpuFinishPolicy1978.java','FinishPolicy1953.java')]
            command=[*command,*added]
            adaptations.append({'prior_fixture':prior.relative_to(source).as_posix(),'prior_sha256':sha(prior),'current_fixture':current.relative_to(source).as_posix(),'current_sha256':sha(current),'added_actual_dependencies':{p.name:sha(p) for p in added},'assertions_changed':False})
        return oldrun(command,log,timeout)
    legacy.run=run_current_compile
    report=legacy.test(source,work,jdk,ndk)
    if len(adaptations)!=1:raise AssertionError('Expected one historical mask compilation adaptation')
    if pins!={p.relative_to(source).as_posix():sha(p) for p in historical}:raise AssertionError('Historical CPU controls changed')
    report.update(historical_assertion_fixtures_sha256=pins,compile_fixture1978_adaptations=adaptations,historical_assertions_unchanged=True)
    (Path(work)/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();sys.path.insert(0,str(source))
    builder=module('cpu78_aggregate_builder',source/'build1978.py')
    closure=list(builder.compile_inputs().values())+[builder.production_path(n) for n in builder.PRODUCTION]
    closure += [source/n for n in ('native1955/single_noise1955.c','native1958/smooth_noise1958.c','native1958/scratch1959.h','native1958/neon1959.h','native1951/moire1951.c','native1960/residual_blocks1961.c','host_speed_cpu1978.py','tests1978/Cpu77Differential1978.java','tests1978/CpuExactGate1978Test.java','tests1978/CpuOwnership1978Test.java','tests1978/CpuFinishOwnership1978Test.java','tests1978/CpuMaskPeers1978.java','tests1978/cpu_workspace_arm1978.c')]
    initial={p.relative_to(source).as_posix():sha(p) for p in closure}
    tests={'differential':differential(source,work/'differential',jdk)};classes=work/'differential/candidate78-classes'
    tests['qualification']=qualification(source,work/'qualification',jdk)
    tests['ownership']=ownership(source,work/'ownership',jdk,classes)
    tests['policy_ownership']=policy_ownership(source,work/'policy_ownership',jdk,classes)
    tests['arm_workspace']=arm_workspace(source,work/'arm_workspace',jdk)
    tests['colour_cache']=module('cpu78_old_colour',source/'host_colour_cache1976.py').test(source,work/'colour_cache',jdk,ndk)
    tests['cpu_masks']=legacy_cpu_masks(source,work/'cpu_masks',jdk,ndk)
    tests['cpu_finish_gate']=module('cpu78_old_finish_gate',source/'host_cpu_finish_gate1976.py').test(source,work/'cpu_finish_gate',jdk,ndk)
    # The old reports contain explanatory nested totals. Count each executed
    # assertion/pixel comparison once, with an explicit nonoverlapping ledger.
    leaves={name+':controls_and_comparisons':tests[name]['assertions'] for name in ('qualification','ownership','policy_ownership','arm_workspace','cpu_finish_gate')}
    for backend in ('java','jni'):leaves['differential:'+backend]=tests['differential']['runs']['candidate78-'+backend]['assertions']
    colour=tests['colour_cache'];arm=colour.get('actual_arm_neon',{}).get('assertions',0);gate=colour['runtime_admission']['assertions']
    leaves.update({'colour_cache:original_differential':colour['assertions']-arm-gate,'colour_cache:runtime_admission':gate,'colour_cache:arm_neon':arm})
    masks=tests['cpu_masks']
    leaves.update({'cpu_masks:mask_policy':masks['mask_report']['assertions'],'cpu_masks:jni_cache_controls':masks['cache_controls']['assertions'],'cpu_masks:native_pixels':masks['native_pixels_compared'],'cpu_masks:baseline_and_source_checks':masks['native_cases']*2})
    total=sum(leaves.values())
    if any(n<0 for n in leaves.values()) or total!=sum(r['assertions'] for r in tests.values()):raise AssertionError('CPU assertion leaf accounting mismatch')
    report={'schema':'ulike-speed-cpu1978-v1','status':'passed','assertions':total,'assertion_leaf_counts':leaves,'tests':tests,'physical_android_tested':False,'device_speedup_verified':False}
    for name,flags in MANDATORY_FLAGS.items():
        for flag in flags:
            if tests[name].get(flag) is not True:raise AssertionError('Missing executed CPU contract '+flag)
            report[flag]=True
    if initial!={p.relative_to(source).as_posix():sha(p) for p in closure}:raise AssertionError('CPU source closure or fixture changed during aggregate')
    report['verified_source_closure_sha256']=initial
    report['runner_sha256']=sha(__file__);(work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',default=str(ROOT));p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');p.add_argument('--phase',choices=['all','differential'],default='all');a=p.parse_args()
    sys.path.insert(0,a.source);jdk=Path(a.jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    result=differential(Path(a.source),Path(a.work),jdk) if a.phase=='differential' else test(a.source,a.work,jdk,a.ndk)
    print(json.dumps(result,indent=2))
