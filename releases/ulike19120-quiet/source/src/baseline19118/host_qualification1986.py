#!/usr/bin/env python3
"""Keep prior queue gates and exercise optional .86 finish diagnosis on real attempts."""
from pathlib import Path
import hashlib
import importlib.util
import json
import os
import shutil


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent).resolve()
    prior = module('qualification86_retained85', source / 'host_qualification1985.py')
    retained = prior.test(source, work / 'retained85', jdk)
    fixtures = module('qualification86_fixtures81', source / 'host_qualification1981.py')
    paths = []
    for name, body in fixtures.FIXTURES.items():
        if name.endswith('/QueueHost1967.java'):
            continue
        path = work / 'fixtures' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body)
        paths.append(path)
    production = source / 'GpuQualification1961.java'
    baseline = source / 'tests1986/qualification/baseline85/GpuQualification1961.java'
    if sha(baseline) != 'f04426e4ded796d14d76c56a62e87ea9e873efaa2f041a9808f35f59c001782d':
        raise AssertionError('Negative control must be the unchanged published .85 qualification')
    legacy = source / 'tests1984/qualification/QualificationProgress1984Test.java'
    handoff = source / 'tests1985/qualification/QualificationHandoff1985Test.java'
    sink = source / 'tests1984/qualification/CameraTrace1965.java'
    repro = source / 'tests1986/qualification/QualificationPublished85ReproTest.java'
    harness = source / 'tests1986/qualification/QualificationFinish1986Test.java'
    reports = {}
    for label, java_source in [('published85', baseline), ('current86', production)]:
        classes = work / label / 'classes'
        classes.mkdir(parents=True, exist_ok=True)
        extra = [handoff, harness] if label == 'current86' else []
        prior.run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-d', classes,
                   java_source, legacy, sink, repro, *extra, *paths], classes.parent / 'compile.log')
        reports[label] = prior.detail(prior.run([jdk / 'bin/java', '-ea', '-cp', classes,
                                                'com.hiro.ulike.QualificationPublished85ReproTest', label], classes.parent / 'repro.log'))
        if label == 'current86':
            reports['finish1986'] = prior.detail(prior.run([jdk / 'bin/java', '-ea', '-cp', classes,
                                                          'com.hiro.ulike.QualificationFinish1986Test'], classes.parent / 'finish.log'))
    if reports['published85'].get('qualification_published85_finish_ambiguity_reproduced1986') is not True or reports['current86'].get('qualification_same_finish_reason_corrected1986') is not True:
        raise AssertionError('Published ambiguity and corrected same-scenario evidence are both required')
    result = dict(status='passed', assertions=retained['assertions'] + sum(v['assertions'] for v in reports.values()),
                  retained85=retained, cases=reports, physical_android_tested=False, device_speedup_verified=False,
                  fixture_classes_in_runtime=False, production_source_sha256=sha(production),
                  published85_source_sha256=sha(baseline), harness_source_sha256=sha(harness),
                  repro_source_sha256=sha(repro), retained_harness_source_sha256=sha(legacy),
                  runner_source_sha256=sha(__file__))
    for report in [retained, *reports.values()]:
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
