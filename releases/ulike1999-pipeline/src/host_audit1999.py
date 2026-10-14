#!/usr/bin/env python3
"""Run fresh .99 lifecycle, transport, real-JNI, timing and release-gate tests.

No fixture result is promoted to physical Android evidence. Every current source
file and publisher is hashed before and after execution to reject stale reports.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, sys, time

def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve()
    if work.exists():raise AssertionError('Fresh host audit work directory required')
    work.mkdir(parents=True)
    jdk=Path(jdk or os.environ.get('JAVA_HOME') or Path(shutil.which('javac')).resolve().parent.parent).resolve()
    ndk=Path(ndk or os.environ.get('ULIKE_NDK_HOME') or source.parent/'tools/android-ndk-r27c').resolve()
    os.environ['PATH']=str(jdk/'bin')+os.pathsep+os.environ.get('PATH','')
    os.environ['EGL_PLATFORM']='surfaceless';os.environ['LIBGL_ALWAYS_SOFTWARE']='1'
    if 'ULIKE_ANDROID_JAR' not in os.environ:
        choices=[source.parent/'tools/android.jar',Path.cwd()/'.ci-ulike1999/tools/android.jar',ndk.parent/'android.jar']
        found=next((p for p in choices if p.is_file()),None)
        if found:os.environ['ULIKE_ANDROID_JAR']=str(found.resolve())
    sys.path.insert(0,str(source));import build1999
    import numpy as np
    if np.__version__!='2.3.5':raise AssertionError('Pinned shader-proof NumPy 2.3.5 required')
    pins=build1999.source_pins();pubpins=build1999.publication_pins();tests={};began=time.monotonic()
    def record(name,value):
        if value.get('status')!='passed' or type(value.get('assertions')) is not int or value['assertions']<=0:raise AssertionError('Empty/failed executed group '+name)
        if value.get('physical_android_tested',False) is not False:raise AssertionError('Host result mislabels physical device '+name)
        tests[name]=value;print('PASS '+name+' assertions='+str(value['assertions']),flush=True)
    groups=[('logcopy95','host_logcopy1999.py'),('shutter94','host_shutter1994.py'),('queue95','host_permission1996.py'),('runtime_version','host_version1999.py'),('fingerprint','host1990_fingerprint.py'),('release_gate','host1999_release_gate.py'),
        ('startup','host_startup1990.py'),('strong_timing95','host_strong_timing1995.py'),
        ('timing','host_timing1999.py'),('route_failure92','host_route_failure1992.py'),('memory','host1990_memory.py'),
        ('gpu_math','host_shader1996.py'),('strong_adaptive94','host_strong_adaptive1998.py'),('strong_transport95','host_strong_transport1995.py'),
        ('model_layout94','host_model_layout1994.py'),('model_parallel96','host_model_parallel1996.py'),
        ('cpu_copy96','tests/host_copy1996.py'),('front_timing96','host_front_timing1996.py'),
        ('native91','host_native1991.py'),('finish_memory92','host_finish_memory1992.py'),('chain','host_chain1997.py')]
    for name,path in groups:
        runner=module('audit95_'+name,source/path)
        record(name,runner.test(source,work/name,jdk,ndk))
    chain=work/'chain/host_chain1978'
    # A developer's optional focused-test override must not select old classes
    # when this final audit has just compiled the current chain itself.
    os.environ['ULIKE1990_CHAIN_CLASSES']=str(chain/'classes')
    os.environ['ULIKE1994_CHAIN_CLASSES']=str(chain/'classes')
    runner=module('audit97_memory',source/'tests/host_memory1997.py')
    record('memory97',runner.test(source,work/'memory97',jdk,ndk))
    runner=module('audit95_front',source/'host_front1994.py')
    record('front94',runner.test(source,work/'front94',jdk,ndk))
    runner=module('audit95_finish_domain',source/'tests/host1992_finish_domain.py')
    record('finish_domain',runner.test(source,work/'finish_domain',chain/'classes',chain/'native',jdk))
    runner=module('audit95_pipeline_gate',source/'tests/host1992_pipeline_gate.py')
    record('pipeline_gate',runner.test(source,work/'pipeline_gate',chain/'classes',chain/'native',jdk))
    runner=module('audit95_failure',source/'host_failure1992.py')
    record('failure92',runner.test(source,work/'failure92',jdk,ndk))
    runner=module('audit95_finish_policy',source/'host_wide_restore1998.py')
    record('finish_recovery95',runner.test(source,work/'finish_policy93',jdk,ndk))
    runner=module('audit98_mask_rows',source/'host_mask_rows1998.py')
    record('mask_rows98',runner.test(source,work/'mask_rows98',jdk,ndk))
    runner=module('audit97_front_plan',source/'host_front_plan1997.py')
    record('front_plan97',runner.test(source,work/'front_plan97',jdk,ndk))
    for name,path in (('capture99','host_capture1999.py'),('front99','tests1999/front/front1999_test.py'),('chain99','tests1999/chain/host_chain1999.py')):
        runner=module('audit99_'+name,source/path)
        record(name,runner.test(source,work/name,jdk,ndk))
    if pins!=build1999.source_pins() or pubpins!=build1999.publication_pins():raise AssertionError('Current source graph changed during test execution')
    result={'schema':'ulike1999-host-audit-v1','status':'passed','assertions':sum(r['assertions'] for r in tests.values()),'tests':tests,
        'current_source_sha256':pins,'current_publication_sha256':pubpins,'source_stability_verified':True,
        'host_python_version':sys.version.split()[0],'shader_proof_numpy_version':np.__version__,'host_elapsed_seconds':round(time.monotonic()-began,3),'physical_android_tested':False,'device_speedup_verified':False,
        'first_shot_gpu_verified_on_physical_android':False,'fixture_classes_in_runtime':False,
        'scope':'Executed desktop lifecycle and real JNI/software GLES tests; no Galaxy capture latency or hardware GPU result is inferred.'}
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();r=test(a.source,a.work,a.jdk,a.ndk);print(json.dumps({'status':r['status'],'assertions':r['assertions'],'groups':list(r['tests'])}))
