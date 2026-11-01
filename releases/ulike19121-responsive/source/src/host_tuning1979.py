#!/usr/bin/env python3
"""Production idle tuner and Qualification regression with controlled transport.

All .78 fixture cases remain verbatim apart from the test class name and reset
of three added selectors. New latch cases observe actual idle child persistence,
capture cancellation and later policy rejection. This is not a device benchmark
or a JNI/shader test; those are owned by the separate native regression runner.
"""
from pathlib import Path
import hashlib
import importlib.util
import json
import shutil
import subprocess


def _legacy_coverage(source):
    old = (source / 'gpu78-fixtures/Tuning1978Test.java').read_text()
    new = (source / 'gpu79-fixtures/Tuning1979Test.java').read_text()
    start = new.index('\n        // A real idle Qualification job must publish')
    end = new.index('        StringBuilder out=new StringBuilder(', start)
    normalized = (new[:start] + new[end:]).replace(
        'public final class Tuning1979Test {',
        'public final class Tuning1978Test {').replace(
        'GpuNoise1960.newMode=0;GpuNoise1960.blockProfile=GpuNoise1960.blockVariant=GpuNoise1960.policyFailureProfile=GpuNoise1960.policyFailureVariant=-1;',
        'GpuNoise1960.newMode=0;GpuNoise1960.blockProfile=-1;')
    if normalized != old:
        raise AssertionError('Original .78 tuning test cases changed')
    old = (source / 'gpu78-fixtures/TuningFixtures1978.java').read_text()
    new = (source / 'gpu79-fixtures/TuningFixtures1979.java').read_text()
    normalized = new.replace(
        'newMode,blockProfile=-1,blockVariant=-1,policyFailureProfile=-1,policyFailureVariant=-1;',
        'newMode,blockProfile=-1;').replace(
        'if(GpuNoise1960.blocked&&(GpuNoise1960.blockProfile<0||GpuNoise1960.blockProfile==b.profile)&&(GpuNoise1960.blockVariant<0||GpuNoise1960.blockVariant==b.variant))',
        'if(GpuNoise1960.blocked&&(GpuNoise1960.blockProfile<0||GpuNoise1960.blockProfile==b.profile))').replace(
        'if(GpuNoise1960.policyFailure||(GpuNoise1960.policyFailureProfile==b.profile&&GpuNoise1960.policyFailureVariant==b.variant)||b.descriptor!=null&&',
        'if(GpuNoise1960.policyFailure||b.descriptor!=null&&')
    if normalized != old:
        raise AssertionError('Original .78 tuning transport controls changed')


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    _legacy_coverage(source)
    names = ('TuningFixtures1979.java', 'Tuning1979Test.java')
    production = [source / 'GpuQualification1961.java', source / 'GpuStrongTuning1975.java']
    inputs = production + [source / 'gpu79-fixtures' / name for name in names] + [
        source / 'gpu78-fixtures/TuningFixtures1978.java',
        source / 'gpu78-fixtures/Tuning1978Test.java',
        source / 'host_tuning1978.py', source / 'host_tuning1979.py',
        source / 'host_qualification1967.py']
    pins = {str(p.relative_to(source)): hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}
    spec = importlib.util.spec_from_file_location('queue79', source / 'host_qualification1967.py')
    q = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(q)
    fixtures, classes = work / 'fixtures', work / 'classes'
    classes.mkdir(exist_ok=True)
    for name, body in q.FIXTURES.items():
        if name.startswith('android/'):
            p = fixtures / name
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(body)
    for name in names:
        p = fixtures / 'com/hiro/ulike' / name
        p.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source / 'gpu79-fixtures' / name, p)
    java = str(Path(jdk) / 'bin/java') if jdk else shutil.which('java')
    javac = str(Path(jdk) / 'bin/javac') if jdk else shutil.which('javac')
    for label, cmd in [
        ('compile', [javac, '--release', '8', '-encoding', 'UTF-8', '-d', str(classes),
                     *map(str, production), *map(str, fixtures.rglob('*.java'))]),
        ('run', [java, '-ea', '-cp', str(classes), 'com.hiro.ulike.Tuning1979Test'])
    ]:
        r = subprocess.run(cmd, text=True, capture_output=True, timeout=180)
        (work / (label + '.log')).write_text(r.stdout + r.stderr)
        if r.returncode:
            raise RuntimeError(label + '\n' + r.stdout + r.stderr)
    if pins != {str(p.relative_to(source)): hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}:
        raise AssertionError('Tuning regression inputs changed during execution')
    result = json.loads(r.stdout.strip().splitlines()[-1])
    required = ('first_legacy_exact2_progress_survives_capture_cancellation',
                'later_policy_rejection_retires_early_legacy_child')
    if result.get('status') != 'passed' or any(result.get('tests', {}).get(k, 0) <= 0 for k in required):
        raise AssertionError('Early legacy proof regression cases did not complete')
    result.update(
        physical_android_tested=False, device_speedup_verified=False, actual_jni=False,
        twentyfour_candidate_tuning1978_verified=True,
        idle_two_full_argb_confidence_policy1978_verified=True,
        idle_snapshot_cancel_and_memory1978_verified=True,
        idle_upload_reuse1978_verified=True,
        new_gpu_balanced_five_percent_gate1978_verified=True,
        legacy_exact_rejections1978_preserved=True,
        worker_bound_speed_retry1978_verified=True,
        legacy_gpu_failure_direct_recovery1978_verified=True,
        idle_first_legacy_exact2_progress1979_verified=True,
        later_policy_rejection1979_preserved=True,
        original_1978_tuning_cases_retained1979_verified=True,
        production_source_sha256={p.name: pins[p.name] for p in production},
        source_sha256=pins)
    (work / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


if __name__ == '__main__':
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument('--source', required=True)
    p.add_argument('--work', required=True)
    p.add_argument('--jdk')
    p.add_argument('--ndk')
    a = p.parse_args()
    print(json.dumps(test(a.source, a.work, a.jdk, a.ndk), indent=2))
