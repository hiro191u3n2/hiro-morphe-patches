#!/usr/bin/env python3
"""Retain .83 queue regressions and exercise .84 pre-copy ticket ownership."""
from pathlib import Path
import hashlib
import importlib.util
import json
import os
import shutil
import subprocess


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def run(command, log):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=90)
    Path(log).write_text(result.stdout + result.stderr)
    if result.returncode:
        raise AssertionError(str(log) + '\n' + result.stdout + result.stderr)
    return result.stdout


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent).resolve()
    retained = module('qualification84_retained83', source / 'host_qualification1983.py').test(source, work / 'retained83', jdk)
    fixture = module('qualification84_fixtures81', source / 'host_qualification1981.py')
    paths = []
    for name, body in fixture.FIXTURES.items():
        if name.endswith('/QueueHost1967.java'):
            continue
        path = work / 'fixtures' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body)
        paths.append(path)
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    production = source / 'GpuQualification1961.java'
    harness = source / 'tests1984/qualification/QualificationProgress1984Test.java'
    sink = source / 'tests1984/qualification/CameraTrace1965.java'
    run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-d', classes, production, harness, sink, *paths], work / 'compile.log')
    detail = json.loads(run([jdk / 'bin/java', '-ea', '-cp', classes, 'com.hiro.ulike.QualificationProgress1984Test'], work / 'run.log').strip().splitlines()[-1])
    if detail.get('status') != 'passed' or detail.get('assertions', 0) <= 0:
        raise AssertionError('Missing executed qualification ownership assertions')
    result = dict(status='passed', assertions=retained['assertions'] + detail['assertions'], retained83=retained,
                  progress1984=detail, physical_android_tested=False, device_speedup_verified=False,
                  production_source_sha256=hashlib.sha256(production.read_bytes()).hexdigest(),
                  harness_source_sha256=hashlib.sha256(harness.read_bytes()).hexdigest(),
                  runner_source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest())
    result.update({key: value for key, value in retained.items() if value is True})
    result.update({key: value for key, value in detail.items() if value is True})
    (work / 'qualification-host-result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), ensure_ascii=False, indent=2))
