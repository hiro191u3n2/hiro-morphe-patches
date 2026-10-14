#!/usr/bin/env python3
"""Execute baseline camera/save contracts and selected M optimization regressions."""
from pathlib import Path
import importlib.util,json,os,shutil

def load(root,name):
    spec=importlib.util.spec_from_file_location('ulike1949_'+name,Path(root)/(name+'.py'))
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module

def test(root,work,android):
    root,work,android=Path(root),Path(work)/'host-regression1949',Path(android)
    work.mkdir(parents=True,exist_ok=True)
    if not shutil.which('javac'):
        shim=work/'bin/javac';shim.parent.mkdir(parents=True,exist_ok=True)
        shim.write_text('#!/bin/sh\nexec java com.sun.tools.javac.Main "$@"\n');shim.chmod(0o755)
        java=shim.parent/'java'
        if not java.exists():java.symlink_to(Path(shutil.which('java')).resolve())
        os.environ['PATH']=str(shim.parent.resolve())+os.pathsep+os.environ.get('PATH','')
    results={}
    results['baseline_camera_quality_save_timing']=load(root,'host_regression1947').test(root,work,android)
    results['exact_temporal_fusion']=load(root,'host_fusion1949').test(root,work)
    results['capture_preprocessing_lifecycle']=load(root,'host_capture1948').test(root,work)
    results['gpu_runtime_admission']=load(root,'host_gpu1949').test(root,work)
    results['gpu_native_jni_execution']=load(root,'host_gpu_jni1949').test(root,work)
    results['gpu_shader_exact_execution']=load(root/'native1949','host_gpu1949').test(root,work)
    for name,value in results.items():
        if value.get('status')!='passed' or value.get('assertions',0)<=0:raise RuntimeError('Passing executed assertions missing: '+name)
    result={'status':'passed','assertions':sum(v['assertions'] for v in results.values()),'suites':results,
        'device_tested':False,'performance_measured_on_device':False,
        'pixel_equivalence_to_baseline':True,'full_resolution_nv21_checked':'4080x3060',
        'native_payload_scope':'Existing native denoise, beauty and encoder library retained. Separate optional integer GPU library added with exact CPU fallback.',
        'gpu_shader_execution_on_host':True,'gpu_execution_on_physical_android':False}
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n');return result
