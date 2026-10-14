#!/usr/bin/env python3
"""Execute actual .71 route, certificate, allocation, diagnostic and JNI/Mesa regressions.

Controlled fixtures validate fallback and memory behavior. The native facade
executes production Java/C against host software GLES; no test establishes a
Samsung driver failure cause or Android device performance.
"""
from pathlib import Path
import importlib.util
ROOT = Path(__file__).resolve().parent

def load(name):
    spec = importlib.util.spec_from_file_location(name + '_aggregate1971', ROOT / (name + '.py'))
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    return module

def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    reports = {}
    runners = [('strong', 'host_strong_fallback1971'), ('certificate', 'host_certificate1971'),
               ('memory', 'host_memory1971'), ('timing', 'host_timing1971'),
               ('native_facade', 'host_native_facade1971')]
    for label, name in runners:
        parameters = {'jdk': jdk}
        if label in ('memory', 'native_facade'): parameters['ndk'] = ndk
        report = load(name).test(source, work / label, **parameters)
        if not isinstance(report, dict) or report.get('status') != 'passed' or type(report.get('assertions')) is not int or report['assertions'] <= 0:
            raise AssertionError('Executed positive regression report absent: ' + label)
        if report.get('physical_android_tested') is not False:
            raise AssertionError('Physical Android untested disclosure absent: ' + label)
        reports[label] = report
    requirements = {
        'strong': ['strong_gpu_preference_regressions_passed', 'strong_generic_fallback_regressions_passed', 'gpu_safety_gates_preserved'],
        'certificate': ['qualification_certificate_regressions_passed', 'gpu_safety_gates_preserved'],
        'memory': ['native_memory_admission_regressions_passed', 'native_failure_diagnostics_regressions_passed', 'gpu_safety_gates_preserved'],
        'timing': ['backend_diagnostics_regressions_passed', 'backend_visibility_regressions_passed', 'strong_preference_telemetry_regressions_passed', 'strong_failure_diagnostics_regressions_passed', 'camera_version_only_regression_passed'],
        'native_facade': ['native_facade_regressions_passed', 'gpu_shader_execution_on_host'],
    }
    for label, flags in requirements.items():
        for flag in flags:
            if reports[label].get(flag) is not True: raise AssertionError('Fresh regression absent: ' + label + '/' + flag)
    result = {'status': 'passed', 'assertions': sum(r['assertions'] for r in reports.values()),
              'tests': reports, 'physical_android_tested': False, 'device_speedup_verified': False,
              'samsung_gpu_failure_root_cause_confirmed': False, 'fresh_gpu_execution_in_this_release': True,
              'gpu_execution_on_physical_android': False}
    for flags in requirements.values():
        for flag in flags: result[flag] = True
    return result
