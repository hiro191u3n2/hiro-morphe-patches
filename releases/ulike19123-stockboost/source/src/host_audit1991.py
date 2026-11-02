#!/usr/bin/env python3
"""Run fresh .91 lifecycle, transport, real-JNI, timing and release-gate tests.

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
        choices=[source.parent/'tools/android.jar',Path.cwd()/'.ci-ulike1991/tools/android.jar',ndk.parent/'android.jar']
        found=next((p for p in choices if p.is_file()),None)
        if found:os.environ['ULIKE_ANDROID_JAR']=str(found.resolve())
    sys.path.insert(0,str(source));import build1991
    pins=build1991.source_pins();pubpins=build1991.publication_pins();tests={};began=time.monotonic()
    def record(name,value):
        if value.get('status')!='passed' or type(value.get('assertions')) is not int or value['assertions']<=0:raise AssertionError('Empty/failed executed group '+name)
        if value.get('physical_android_tested',False) is not False:raise AssertionError('Host result mislabels physical device '+name)
        tests[name]=value;print('PASS '+name+' assertions='+str(value['assertions']),flush=True)
    groups=[('fingerprint','host1990_fingerprint.py'),('release_gate','host1991_release_gate.py'),
        ('startup','host_startup1990.py'),('strong_route','host_strongroute1991.py'),
        ('timing','host_timing1991.py'),('memory','host1990_memory.py'),
        ('gpu_math','host_gpu_math1990.py'),('startup_math','host_startup_math1990.py'),
        ('model_math','host_model_math1990.py'),
        ('native91','host_native1991.py'),('finish_memory91','host_finish_memory1991.py'),('chain','host1990_chain.py')]
    for name,path in groups:
        runner=module('audit91_'+name,source/path)
        record(name,runner.test(source,work/name,jdk,ndk))
    chain=work/'chain/host_chain1978'
    runner=module('audit91_finish_domain',source/'tests/host1990_finish_domain.py')
    record('finish_domain',runner.test(source,work/'finish_domain',chain/'classes',chain/'native',jdk))
    runner=module('audit91_pipeline_gate',source/'tests/host1991_pipeline_gate.py')
    record('pipeline_gate',runner.test(source,work/'pipeline_gate',chain/'classes',chain/'native',jdk))
    runner=module('audit91_frontmodel',source/'host_frontmodel1991.py')
    record('frontmodel',runner.test(source,work/'frontmodel',jdk,ndk))
    if pins!=build1991.source_pins() or pubpins!=build1991.publication_pins():raise AssertionError('Current source graph changed during test execution')
    result={'schema':'ulike1991-host-audit-v1','status':'passed','assertions':sum(r['assertions'] for r in tests.values()),'tests':tests,
        'current_source_sha256':pins,'current_publication_sha256':pubpins,'source_stability_verified':True,
        'host_elapsed_seconds':round(time.monotonic()-began,3),'physical_android_tested':False,'device_speedup_verified':False,
        'first_shot_gpu_verified_on_physical_android':False,'fixture_classes_in_runtime':False,
        'scope':'Executed desktop lifecycle and real JNI/software GLES tests; no Galaxy capture latency or hardware GPU result is inferred.'}
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();r=test(a.source,a.work,a.jdk,a.ndk);print(json.dumps({'status':r['status'],'assertions':r['assertions'],'groups':list(r['tests'])}))
