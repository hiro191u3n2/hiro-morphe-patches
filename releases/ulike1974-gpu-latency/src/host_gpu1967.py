#!/usr/bin/env python3
"""Run fresh host regressions against actual new GPU admission Java sources."""
from pathlib import Path
import hashlib, importlib.util

def runner(name, source):
    path=source/(name+'.py')
    spec=importlib.util.spec_from_file_location(name,path)
    module=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def test(source, work, jdk=None):
    source, work=Path(source), Path(work)
    work.mkdir(parents=True,exist_ok=True)
    results={}
    for name in ('host_strong1967','host_qualification1967','host_timing1967','host_logging1967'):
        results[name]=runner(name,source).test(source,work/name,jdk=jdk)
    for name,result in results.items():
        if not (result.get('status')=='passed' and type(result.get('assertions')) is int and result['assertions']>0):
            raise RuntimeError('Missing executed host report '+name)
        if result.get('physical_android_tested') is not False:
            raise RuntimeError('Physical Android must be explicitly untested '+name)
    if results['host_strong1967'].get('strong_admission_regressions_passed') is not True:
        raise RuntimeError('Missing actual strong-bank admission regressions')
    if results['host_qualification1967'].get('admission_queue_regressions_passed') is not True:
        raise RuntimeError('Missing qualification/queue regression evidence')
    if results['host_timing1967'].get('backend_diagnostics_regressions_passed') is not True:
        raise RuntimeError('Missing per-shot backend diagnostics regressions')
    if results['host_logging1967'].get('logging_regressions_passed') is not True:
        raise RuntimeError('Missing display logger recovery/export regression evidence')
    if not all(results[n].get('gpu_safety_gates_preserved') is True for n in ('host_strong1967','host_qualification1967')):
        raise RuntimeError('GPU safety gate tests are required')
    return {'status':'passed','assertions':sum(r['assertions'] for r in results.values()),
        'physical_android_tested':False,'gpu_admission_route_regressions_passed':True,
        'gpu_safety_gates_preserved':True,'backend_diagnostics_regressions_passed':True,'logging_regressions_passed':True,'tests':results,
        'fresh_gpu_execution_in_this_release':False,
        'scope':'Actual changed admission/qualification Java source with modeled Android/GPU delivery; retained native shaders and camera/pixel helpers checked separately by DEX/archive preservation.'}
