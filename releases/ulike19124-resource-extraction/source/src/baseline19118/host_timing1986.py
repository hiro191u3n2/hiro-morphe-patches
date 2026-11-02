#!/usr/bin/env python3
"""Run retained photo timing gates and .86 same-lookup forecast/live-finish controls."""
from pathlib import Path
import hashlib
import importlib.util
import json
import os
import re
import shutil


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def adapt_timing_peers1986(body):
    anchor = 'final class GpuQualification1961 {'
    if anchor not in body:
        return body
    if body.count(anchor) != 1:
        raise AssertionError('Ambiguous .86 qualification timing peer')
    addition = '''
 static String attempts1986;static int attemptCalls1986;static boolean throwAttempts1986;
 public static String attemptSummary1986(){attemptCalls1986++;if(throwAttempts1986)throw new LinkageError("optional .86 provider fixture");return attempts1986==null?attemptSummary1985():attempts1986;}
'''
    return body.replace(anchor, anchor + addition)


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    if source.is_file():
        source = source.parent
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent).resolve()
    retained = module('timing86_retained82', source / 'host_timing1982.py')
    peers84 = module('timing86_peers84', source / 'host_timing1984.py')
    prior = module('timing86_retained85', source / 'host_timing1985.py')
    paths = []
    for name, body in retained.STUBS.items():
        path = work / 'fixtures' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(adapt_timing_peers1986(prior.adapt_timing_peers1985(peers84.adapt_timing_peers1984(body))))
        paths.append(path)
    original = source / 'Timing1982Test.java'
    body = original.read_text()
    if not body.count('1.9.82'):
        raise AssertionError('Retained timing version expectation changed')
    copied = work / 'fixtures/com/hiro/ulike/Timing1982Test.java'
    copied.write_text(body.replace('1.9.82', '1.9.86'))
    queue_original = source / 'tests1983/qualification/QueueTiming1983Test.java'
    queue_copied = work / 'fixtures/com/hiro/ulike/QueueTiming1983Test.java'
    queue_copied.write_text(prior.adapt_queue_upper_bound1985(queue_original.read_text()))
    harness84 = source / 'tests1984/timing/QueueTiming1984Test.java'
    harness85 = source / 'tests1985/timing/QueueTiming1985Test.java'
    harness = source / 'tests1986/timing/ForecastTiming1986Test.java'
    paths.extend([copied, queue_copied, harness84, harness85, harness])
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    production = source / 'ProcessingTiming1947.java'
    prior.run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-d', classes, production, *paths], work / 'compile.log')
    reports = {}
    for key, name in [('retained82', 'Timing1982Test'), ('retained83', 'QueueTiming1983Test'),
                      ('attempts1984', 'QueueTiming1984Test'), ('handoff1985', 'QueueTiming1985Test'),
                      ('forecast_finish1986', 'ForecastTiming1986Test')]:
        reports[key] = prior.parsed(prior.run([jdk / 'bin/java', '-ea', '-XX:ActiveProcessorCount=4', '-cp', classes,
                                             'com.hiro.ulike.' + name], work / (key + '.log')))
    bytecode = prior.run([jdk / 'bin/javap', '-classpath', classes, '-c', '-p', 'com.hiro.ulike.ProcessingTiming1947'], work / 'bytecode.log')
    match = re.search(r'  public static void strongForecast1986\([^\n]+\);\n(.*?)(?=\n  (?:public|private|protected|static))', bytecode, re.S)
    if not match or 'monitorenter' not in match.group(1):
        raise AssertionError('Missing executed observer bytecode')
    if re.search(r'^\s*\d+:\s+(?:new|anewarray|newarray|multianewarray|invoke\w+)\b', match.group(1), re.M):
        raise AssertionError('Hot forecast observer must not allocate or call other helpers')
    result = dict(status='passed', assertions=sum(v['assertions'] for v in reports.values()) + 2, cases=reports,
                  resident_queue_labels1983=True, resident_saved_decision_immutable1983=True,
                  resident_diagnostics_read_only1983=True, retained_queue_reason_upper_bound_adapter1985_verified=True,
                  timing_forecast_hot_observer_no_allocations_or_callbacks1986=True,
                  physical_android_tested=False, device_speedup_verified=False, fixture_classes_in_runtime=False,
                  production_source_sha256=sha(production), retained_harness_sha256=sha(original),
                  version_adapted_harness_sha256=sha(copied), retained_queue_harness_sha256=sha(queue_original),
                  upper_bound_adapted_harness_sha256=sha(queue_copied), retained84_harness_sha256=sha(harness84),
                  retained85_harness_sha256=sha(harness85), harness_source_sha256=sha(harness),
                  bytecode_sha256=sha(work / 'bytecode.log'), runner_source_sha256=sha(__file__))
    for report in reports.values():
        result.update({key: value for key, value in report.items() if value is True})
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), ensure_ascii=False, indent=2))
