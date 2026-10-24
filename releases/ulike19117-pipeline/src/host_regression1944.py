#!/usr/bin/env python3
"""Execute published-pixel equality, JNI parity, capture and bounded-save regressions."""
from pathlib import Path
import importlib.util
import json
import os
import re
import shutil
import subprocess
import sys


def suite(root, work, name, filename, android=None):
    spec = importlib.util.spec_from_file_location('ulike1944_' + name, root / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    result = module.test(root, work) if android is None else module.test(root, work, android)
    if filename == 'host_speed1944_noise.py':
        variants = result.get('variants', {})
        expected = {'java-cache', 'java-uncached-oom'}
        if os.environ.get('ULIKE_HOST_NATIVE'):
            expected |= {'native', 'native-workspace-oom', 'native-uncached-oom'}
        if result.get('equal') is not True or set(variants) != expected:
            raise RuntimeError('Noise equality fixture coverage absent')
        for label, value in variants.items():
            if value.get('equal') is not True or value.get('native') is not label.startswith('native') or any(type(value.get(key)) is not int or value[key] <= 0 for key in ('cases', 'comparedPixels', 'changedPixels', 'parallelRanges')):
                raise RuntimeError('Noise fixture did not execute required native/fallback pixels: ' + label)
        result['status'] = 'passed'
        result['assertions'] = sum(value['comparedPixels'] for value in variants.values())
        result['scenarios'] = sum(value['cases'] for value in variants.values())
    if not isinstance(result, dict) or result.get('status') != 'passed' or type(result.get('assertions')) is not int or result['assertions'] <= 0:
        raise RuntimeError('Missing executed passing assertions: ' + filename)
    json.dumps(result, allow_nan=False)
    return result


def test(root, work, android):
    root, work, android = Path(root), Path(work), Path(android)
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(os.environ.get('ULIKE_JAVAC') or shutil.which('javac')).resolve().parent.parent
    native = work / 'host-native1944'
    completed = subprocess.run([sys.executable, str(root / 'host_native1944.py'), '--jdk', str(jdk), '--output', str(native)],
                               capture_output=True, text=True, timeout=180)
    (work / 'host-native1944.log').write_text(completed.stdout + completed.stderr)
    if completed.returncode:
        raise RuntimeError('Actual JNI parity test failed: ' + completed.stderr[-8000:] + completed.stdout[-8000:])
    native_result = json.loads((native / 'host-native1944.json').read_text())
    counts = {name: int(value) for name, value in re.findall(r'([a-z_]+)=(\d+)', native_result.get('checks', ''))}
    if native_result.get('pass') is not True or any(counts.get(name, 0) <= 0 for name in ('cases', 'exact_int_comparisons', 'parallel', 'vertical_batches', 'exact_float_comparisons', 'full_resize_fixtures', 'exact_resize_pixels')):
        raise RuntimeError('Native JNI parity executed assertions absent')
    results = {'native_equivalence': {'status': 'passed',
                                     'assertions': sum(counts[name] for name in ('exact_int_comparisons', 'exact_float_comparisons', 'exact_resize_pixels')),
                                     'counts': counts, 'jni_report': native_result}}
    previous_native = os.environ.pop('ULIKE_HOST_NATIVE', None)
    try:
        results['noise_equivalence'] = suite(root, work / 'java-noise', 'noise_equivalence', 'host_speed1944_noise.py')
        os.environ['ULIKE_HOST_NATIVE'] = str(native)
        results['native_noise_equivalence'] = suite(root, work / 'native-noise', 'native_noise_equivalence', 'host_speed1944_noise.py')
        os.environ.pop('ULIKE_HOST_NATIVE', None)
        for name, filename, platform in [
            ('capture_timing', 'host_timing1943.py', False),
            ('noise_preservation', 'host_noise1942.py', False),
            ('save_pipeline', 'host_pipeline1942.py', True),
            ('shutter_feedback', 'host_feedback1941.py', True),
            ('scheduling', 'host_scheduling1944.py', True),
            ('save_concurrency', 'host_save1944.py', False),
            ('front_startup', 'host_front1936.py', True),
            ('renderer_startup', 'host_renderer1938.py', True),
        ]:
            results[name] = suite(root, work, name, filename, android if platform else None)
    finally:
        if previous_native is None:
            os.environ.pop('ULIKE_HOST_NATIVE', None)
        else:
            os.environ['ULIKE_HOST_NATIVE'] = previous_native
    for name, result in results.items():
        (work / ('host-regression-' + name + '-result.json')).write_text(json.dumps(result, ensure_ascii=False, indent=2, allow_nan=False) + '\n')
    return {'status': 'passed', 'assertions': sum(result['assertions'] for result in results.values()),
            'suites': results, 'device_tested': False, 'performance_measured_on_device': False}
