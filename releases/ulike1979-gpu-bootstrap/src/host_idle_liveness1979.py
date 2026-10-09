#!/usr/bin/env python3
"""Real .79 scheduler lifecycle, plus a frozen .78 missed-notification control.

Production WholeRoute, Qualification, SaveQueue and CPU workers run together.
Android objects, capture metadata and GPU/image work are controlled fixtures;
this is neither an on-device test nor evidence that the screenshot was stuck.
"""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import subprocess

BASELINE_WHOLE_SHA256 = '7041f4dbc95d238bcdfaf866b4c85abd06173fcbfe88affeb46d9bbe62139fda'


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    fixtures = source / 'idle79-fixtures'
    original = fixtures / 'reference78/WholeRoute1953.java'
    if sha(original) != BASELINE_WHOLE_SHA256:
        raise AssertionError('Frozen published .78 WholeRoute baseline changed')
    production = [source / name for name in ('WholeRoute1952.java', 'WholeRoute1953.java',
                                             'GpuQualification1961.java', 'SaveQueue1935.java', 'SpeedWorkers1935.java')]
    tests = [fixtures / 'IdleFixtures1979.java', fixtures / 'IdleLiveness1979Test.java']
    android = sorted((source / 'tests/route1953-fixtures/android').rglob('*.java'))
    tracked = production + tests + android + [original, source / 'host_idle_liveness1979.py']
    pins = {str(path.relative_to(source)): sha(path) for path in tracked}
    java = str(Path(jdk) / 'bin/java') if jdk else shutil.which('java')
    javac = str(Path(jdk) / 'bin/javac') if jdk else shutil.which('javac')
    if not java or not javac:
        raise RuntimeError('A JDK is required for the real asynchronous engine test')
    commands = []

    def run(command, label):
        command = list(map(str, command))
        commands.append({'label': label, 'argv': command})
        result = subprocess.run(command, text=True, capture_output=True, timeout=45)
        (work / (label + '.log')).write_text(result.stdout + result.stderr)
        return result

    results = {}
    for label in ('current79', 'reference78'):
        classes = work / (label + '-classes')
        if classes.exists():
            shutil.rmtree(classes)
        classes.mkdir()
        sources = [original if label == 'reference78' and path.name == 'WholeRoute1953.java' else path
                   for path in production] + tests + android
        compiled = run([javac, '--release', '8', '-encoding', 'UTF-8', '-d', classes, *sources], label + '-compile')
        if compiled.returncode:
            raise RuntimeError(label + ' compile\n' + compiled.stdout + compiled.stderr)
        executed = run([java, '-ea', '-Xmx256m', '-XX:ActiveProcessorCount=4', '-cp', classes,
                        'com.hiro.ulike.IdleLiveness1979Test'], label + '-run')
        if label == 'current79':
            if executed.returncode:
                raise RuntimeError('Current lifecycle failed\n' + executed.stdout + executed.stderr)
            results[label] = json.loads(executed.stdout.strip().splitlines()[-1])
            if results[label].get('status') != 'passed' or results[label].get('scenarios') != 6:
                raise AssertionError('All actual lifecycle scenarios did not complete')
        else:
            if executed.returncode == 0 or 'timeout: idle_transition_without_notification:' not in executed.stdout + executed.stderr:
                raise AssertionError('Frozen .78 must fail at the missing-notification transition, not a different condition\n'
                                     + executed.stdout + executed.stderr)
            results[label] = {'status': 'expected_failure', 'failure': 'idle_transition_without_notification',
                              'source_sha256': BASELINE_WHOLE_SHA256}
    if pins != {str(path.relative_to(source)): sha(path) for path in tracked}:
        raise AssertionError('Production or fixture changed during lifecycle test')
    current = results['current79']
    report = dict(status='passed', assertions=current['assertions'], scenarios=current['scenarios'],
                  sections=current['sections'], negative_controls=1, tests=results,
                  physical_android_tested=False, device_speedup_verified=False, actual_jni=False,
                  actual_whole_route_engine=True, actual_qualification_engine=True,
                  actual_save_queue=True, actual_bounded_cpu_workers=True,
                  mocked_android_gpu_images_and_capture_metadata=True,
                  screenshot_stall_cause_confirmed=False,
                  whole_route_idle_recheck1979_verified=True,
                  idle_downstream_qualification1979_verified=True,
                  idle_cancel_ownership1979_verified=True,
                  idle_save_and_exact_gates1979_verified=True,
                  source_sha256=pins)
    (work / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n')
    (work / 'result.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), indent=2))
