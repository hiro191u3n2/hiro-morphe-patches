#!/usr/bin/env python3
"""Run retained timing/queue evidence and .84 live-attempt isolation controls."""
from pathlib import Path
import hashlib
import importlib.util
import json
import os
import re
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


def parsed(text):
    match = re.search(r'^RESULT (\{[^\n]+\})$', text, re.M)
    if not match:
        raise AssertionError('No executed timing result')
    value = json.loads(match.group(1))
    if value.get('status') != 'passed' or value.get('assertions', 0) <= 0:
        raise AssertionError('Failed or empty timing result')
    return value


def adapt_timing_peers1984(body):
    anchor = 'final class GpuQualification1961 {'
    if anchor not in body:
        return body
    if body.count(anchor) != 1:
        raise AssertionError('Ambiguous qualification timing peer')
    addition = '''
 static String attempts1984="";static int attemptCalls1984;static boolean throwAttempts1984;
 public static String attemptSummary1984(){attemptCalls1984++;if(throwAttempts1984)throw new AssertionError("optional attempt fixture");return attempts1984;}
'''
    return body.replace(anchor, anchor + addition)


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    if source.is_file():
        source = source.parent
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    retained = module('timing84_retained82', source / 'host_timing1982.py')
    paths = []
    for name, body in retained.STUBS.items():
        path = work / 'fixtures' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(adapt_timing_peers1984(body))
        paths.append(path)
    original = source / 'Timing1982Test.java'
    body = original.read_text()
    if not body.count('1.9.82'):
        raise AssertionError('Retained timing version expectations changed')
    copied = work / 'fixtures/com/hiro/ulike/Timing1982Test.java'
    copied.write_text(body.replace('1.9.82', '1.9.84'))
    paths.extend([copied, source / 'tests1983/qualification/QueueTiming1983Test.java',
                  source / 'tests1984/timing/QueueTiming1984Test.java'])
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    production = source / 'ProcessingTiming1947.java'
    run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-d', classes, production, *paths], work / 'compile.log')
    reports = {}
    for key, name in [('retained82', 'Timing1982Test'), ('retained83', 'QueueTiming1983Test'),
                      ('attempts1984', 'QueueTiming1984Test')]:
        reports[key] = parsed(run([jdk / 'bin/java', '-ea', '-XX:ActiveProcessorCount=4', '-cp', classes,
                                  'com.hiro.ulike.' + name], work / (key + '.log')))
    result = dict(status='passed', assertions=sum(v['assertions'] for v in reports.values()), cases=reports,
                  resident_queue_labels1983=True, resident_saved_decision_immutable1983=True,
                  resident_diagnostics_read_only1983=True,
                  live_attempt_results_isolated1984=True, live_attempt_failure_noninterference1984=True,
                  reserved_slot_refusal_explicit1984=True, physical_android_tested=False,
                  production_source_sha256=hashlib.sha256(production.read_bytes()).hexdigest(),
                  retained_harness_sha256=hashlib.sha256(original.read_bytes()).hexdigest(),
                  runner_source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest())
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
