#!/usr/bin/env python3
"""Execute UI/logging regressions against the two actual changed diagnostics sources."""
from pathlib import Path
import importlib.util

def runner(name, source):
    spec = importlib.util.spec_from_file_location(name, source / (name + '.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def test(source, work, jdk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    reports = {}
    for name in ('host_timing1969', 'host_camera_details1969'):
        report = runner(name, source).test(source, work / name, jdk=jdk)
        if not (report.get('status') == 'passed' and type(report.get('assertions')) is int and report['assertions'] > 0):
            raise RuntimeError('Fresh executed UI/logging report absent: ' + name)
        if report.get('physical_android_tested') is not False:
            raise RuntimeError('Actual Android execution must be explicitly untested: ' + name)
        reports[name] = report
    if reports['host_timing1969'].get('backend_visibility_regressions_passed') is not True:
        raise RuntimeError('Missing initial and measured GPU-row placement tests')
    if reports['host_timing1969'].get('backend_diagnostics_regressions_passed') is not True:
        raise RuntimeError('Missing retained timing diagnostics regressions')
    if reports['host_camera_details1969'].get('camera_details_regressions_passed') is not True:
        raise RuntimeError('Missing full-details dialog regressions')
    if reports['host_camera_details1969'].get('logging_regressions_passed') is not True:
        raise RuntimeError('Missing logger/export regressions against the changed helper')
    return {'status': 'passed', 'assertions': sum(r['assertions'] for r in reports.values()),
        'physical_android_tested': False, 'physical_android_ui_tested': False,
        'gpu_status_visibility_regressions_passed': True,
        'backend_diagnostics_regressions_passed': True, 'logging_regressions_passed': True,
        'tests': reports, 'fresh_gpu_execution_in_this_release': False,
        'scope': 'Actual changed diagnostics sources with controlled Android boundaries. UI/logging tests only; retained GPU/camera recovery code and native payloads are verified separately by DEX/archive preservation.'}
