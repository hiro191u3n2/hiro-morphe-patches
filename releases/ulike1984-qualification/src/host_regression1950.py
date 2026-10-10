#!/usr/bin/env python3
"""Execute exact CPU/GPU H1-H5 kernels and preserved camera/save contracts.

Performance figures, where present, describe host execution only. They never
establish a Galaxy speedup or replace Android camera validation.
"""
from pathlib import Path
import importlib.util,json,os,shutil

def load(root,name):
    spec=importlib.util.spec_from_file_location('ulike1950_'+name,Path(root)/(name+'.py'))
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module

def checked(value,name):
    if value.get('status')!='passed' or value.get('assertions',0)<=0:
        raise RuntimeError('Executed passing assertions missing: '+name)
    return value

def test(root,work,android):
    root,work,android=Path(root),Path(work)/'host-regression1950',Path(android)
    work.mkdir(parents=True,exist_ok=True)
    results={}
    for name,module in [
        ('baseline_camera_quality_save_timing','host_regression1947'),
        ('exact_temporal_fusion','host_fusion1949'),
        ('capture_preprocessing_lifecycle','host_capture1948'),
        ('gpu_runtime_admission1950','host_gate1950'),
        ('primary_denoise_native_exact','host_core1950'),
        ('exact_correction_copy_elision','host_correction1950'),
        ('fused_gpu_finishing_exact','host_gpu_finish1950')]:
        task=load(root,module)
        value=task.test(root,work) if module in ('host_fusion1949','host_capture1948') else task.test(root,work,android)
        # Isolated host copy timings remain in the raw local report; they are
        # not deterministic correctness evidence or Android speed measurements.
        if module == 'host_correction1950':
            value = {key: item for key, item in value.items()
                     if key not in ('host_copy_elapsed_nanos', 'copy_benchmark_mean_ms')}
        results[name]=checked(value,name)
    for name in ('primary_denoise_native_exact','fused_gpu_finishing_exact','exact_correction_copy_elision'):
        if results[name].get('pixel_equivalence_to_baseline') is not True:
            raise RuntimeError('Independent exact baseline pixel comparison missing: '+name)
    gate=results['gpu_runtime_admission1950']
    for key in ('pixel_equivalence_guard_preserved','canonical_boundary_shape_key','cpu_reference_not_global_lock','transfer_inclusive_timing_gate'):
        if gate.get(key) is not True:raise RuntimeError('GPU gate evidence missing: '+key)
    result={'status':'passed','assertions':sum(v['assertions'] for v in results.values()),
        'suites':results,'device_tested':False,'performance_measured_on_device':False,
        'pixel_equivalence_to_baseline':True,'full_resolution_nv21_checked':'4080x3060',
        'h1_h5_selected':['H1','H2','H3','H4','H5'],
        'native_payload_scope':'Exact primary noise native kernel, fused integer GPU finishing and preserved source/settings/save semantics.',
        'gpu_shader_execution_on_host':True,'gpu_execution_on_physical_android':False}
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
    return result
