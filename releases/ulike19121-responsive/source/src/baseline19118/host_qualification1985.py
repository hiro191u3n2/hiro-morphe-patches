#!/usr/bin/env python3
"""Retain .84 gates and execute .85 terminal-state and copier lifetime regressions."""
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


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def run(command, log):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=90)
    Path(log).write_text(result.stdout + result.stderr)
    if result.returncode:
        raise AssertionError(str(log) + '\n' + result.stdout + result.stderr)
    return result.stdout


def detail(text):
    result = json.loads(text.strip().splitlines()[-1])
    if result.get('status') != 'passed' or result.get('assertions', 0) <= 0:
        raise AssertionError('Missing executed qualification evidence')
    return result


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent).resolve()
    retained = module('qualification85_retained84', source / 'host_qualification1984.py').test(source, work / 'retained84', jdk)
    fixtures = module('qualification85_fixtures81', source / 'host_qualification1981.py')
    paths = []
    for name, body in fixtures.FIXTURES.items():
        if name.endswith('/QueueHost1967.java'):
            continue
        path = work / 'fixtures' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body)
        paths.append(path)
    retained_harness = source / 'tests1984/qualification/QualificationProgress1984Test.java'
    sink = source / 'tests1984/qualification/CameraTrace1965.java'
    harness = source / 'tests1985/qualification/QualificationHandoff1985Test.java'
    repro = source / 'tests1985/qualification/QualificationPublished84ReproTest.java'
    production = source / 'GpuQualification1961.java'
    baseline = source / 'tests1985/qualification/baseline84/GpuQualification1961.java'
    scope = source / 'tests1985/source-scope84.json'
    if sha(scope) != '56fba85078c3362b65be983345088082a4030d2f0526e75ba903698197e26ebe':
        raise AssertionError('Published .84 reference graph changed')
    if sha(baseline) != json.loads(scope.read_text())['GpuQualification1961.java']:
        raise AssertionError('Negative control must use unmodified published .84 qualification')
    cases = {}
    for label, java_source in [('published84', baseline), ('current85', production)]:
        classes = work / label / 'classes'
        classes.mkdir(parents=True, exist_ok=True)
        extra = [harness] if label == 'current85' else []
        run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-d', classes,
             java_source, retained_harness, sink, repro, *extra, *paths], classes.parent / 'compile.log')
        cases[label] = detail(run([jdk / 'bin/java', '-ea', '-cp', classes,
                                  'com.hiro.ulike.QualificationPublished84ReproTest', label], classes.parent / 'repro.log'))
        if label == 'current85':
            cases['handoff1985'] = detail(run([jdk / 'bin/java', '-ea', '-cp', classes,
                                              'com.hiro.ulike.QualificationHandoff1985Test'], classes.parent / 'handoff.log'))
    if cases['published84'].get('qualification_published84_ambiguity_reproduced1985') is not True or cases['current85'].get('qualification_same_scenario_corrected1985') is not True:
        raise AssertionError('Both published failure and corrected same-scenario evidence required')
    result = dict(status='passed', assertions=retained['assertions'] + sum(v['assertions'] for v in cases.values()),
                  retained84=retained, cases=cases, physical_android_tested=False, device_speedup_verified=False,
                  fixture_classes_in_runtime=False, production_source_sha256=sha(production),
                  published84_source_sha256=sha(baseline), harness_source_sha256=sha(harness),
                  repro_source_sha256=sha(repro), retained_harness_source_sha256=sha(retained_harness),
                  runner_source_sha256=sha(__file__))
    for report in [retained, *cases.values()]:
        result.update({key: value for key, value in report.items() if value is True})
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
