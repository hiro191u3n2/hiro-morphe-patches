#!/usr/bin/env python3
"""Execute guarded Strong preference, fresh certificates and per-photo telemetry tests."""
from pathlib import Path
import importlib.util

def runner(name, source):
    spec=importlib.util.spec_from_file_location(name,source/(name+'.py'))
    module=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def test(source,work,jdk=None):
    source,work=Path(source).resolve(),Path(work).resolve()
    work.mkdir(parents=True,exist_ok=True)
    reports={}
    for name in ('host_strong_preferred1970','host_certificate1970','host_timing1970'):
        report=runner(name,source).test(source,work/name,jdk=jdk)
        if not (report.get('status')=='passed' and type(report.get('assertions')) is int and report['assertions']>0):
            raise RuntimeError('Fresh executed Strong preference report absent: '+name)
        if report.get('physical_android_tested') is not False:
            raise RuntimeError('Physical Android execution must be explicitly untested: '+name)
        reports[name]=report
    for name,flag in (('host_strong_preferred1970','strong_gpu_preference_regressions_passed'),
        ('host_certificate1970','qualification_certificate_regressions_passed'),
        ('host_timing1970','strong_preference_telemetry_regressions_passed')):
        if reports[name].get(flag) is not True:raise RuntimeError('Missing '+flag)
    if not all(reports[name].get('gpu_safety_gates_preserved') is True for name in ('host_strong_preferred1970','host_certificate1970')):
        raise RuntimeError('Exact quality, bounded resource and cancellation tests are required')
    if reports['host_timing1970'].get('backend_diagnostics_regressions_passed') is not True:
        raise RuntimeError('Retained per-photo timing/backend diagnostics regressions absent')
    return {'status':'passed','assertions':sum(r['assertions'] for r in reports.values()),
        'physical_android_tested':False,'fresh_gpu_execution_in_this_release':False,
        'strong_gpu_preference_regressions_passed':True,'qualification_certificate_regressions_passed':True,
        'strong_preference_telemetry_regressions_passed':True,'backend_diagnostics_regressions_passed':True,
        'gpu_safety_gates_preserved':True,'tests':reports,
        'scope':'Actual changed Strong/qualification/timing Java with controlled Android/GPU boundaries; host behavior tests, not shader execution or a device benchmark. Native/pixel/camera/other-helper preservation is verified separately by serialized DEX and archive proofs.'}
