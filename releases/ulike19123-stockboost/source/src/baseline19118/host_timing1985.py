#!/usr/bin/env python3
"""Retain prior timing tests and verify .85 end-ledger/copy-decision isolation."""
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


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


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


def adapt_timing_peers1985(body):
    anchor = 'final class GpuQualification1961 {'
    if anchor not in body:
        return body
    if body.count(anchor) != 1:
        raise AssertionError('Ambiguous new qualification timing peer')
    # The default delegates to the old provider so all .84 assertions still
    # execute without editing their tests. New cases exercise .85 independently.
    addition = '''
 static String attempts1985;static int attemptCalls1985;static boolean throwAttempts1985;
 public static String attemptSummary1985(){attemptCalls1985++;if(throwAttempts1985)throw new LinkageError("optional .85 provider fixture");return attempts1985==null?attemptSummary1984():attempts1985;}
'''
    return body.replace(anchor, anchor + addition)


def adapt_queue_upper_bound1985(body):
    # .85 allocates stable code 21 to COPY_NOT_STARTED1985. Keep the old test's
    # first-out-of-range negative control by moving only that literal to 22.
    anchor = '{21,0,0,4,1,80*M,47*M}'
    if body.count(anchor) != 1:
        raise AssertionError('Retained queue upper-bound fixture changed')
    return body.replace(anchor, '{22,0,0,4,1,80*M,47*M}')


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    if source.is_file():
        source = source.parent
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    retained = module('timing85_retained82', source / 'host_timing1982.py')
    prior = module('timing85_peers84', source / 'host_timing1984.py')
    paths = []
    for name, body in retained.STUBS.items():
        path = work / 'fixtures' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(adapt_timing_peers1985(prior.adapt_timing_peers1984(body)))
        paths.append(path)
    original = source / 'Timing1982Test.java'
    body = original.read_text()
    if not body.count('1.9.82'):
        raise AssertionError('Retained timing version expectations changed')
    copied = work / 'fixtures/com/hiro/ulike/Timing1982Test.java'
    copied.write_text(body.replace('1.9.82', '1.9.85'))
    queue_original = source / 'tests1983/qualification/QueueTiming1983Test.java'
    queue_copied = work / 'fixtures/com/hiro/ulike/QueueTiming1983Test.java'
    queue_copied.write_text(adapt_queue_upper_bound1985(queue_original.read_text()))
    old_harness = source / 'tests1984/timing/QueueTiming1984Test.java'
    harness = source / 'tests1985/timing/QueueTiming1985Test.java'
    paths.extend([copied, queue_copied, old_harness, harness])
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    production = source / 'ProcessingTiming1947.java'
    run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-d', classes, production, *paths], work / 'compile.log')
    reports = {}
    for key, name in [('retained82', 'Timing1982Test'), ('retained83', 'QueueTiming1983Test'),
                      ('attempts1984', 'QueueTiming1984Test'), ('handoff1985', 'QueueTiming1985Test')]:
        reports[key] = parsed(run([jdk / 'bin/java', '-ea', '-XX:ActiveProcessorCount=4', '-cp', classes,
                                  'com.hiro.ulike.' + name], work / (key + '.log')))
    result = dict(status='passed', assertions=sum(v['assertions'] for v in reports.values()), cases=reports,
                  resident_queue_labels1983=True, resident_saved_decision_immutable1983=True,
                  resident_diagnostics_read_only1983=True,
                  live_attempt_results_isolated1984=True, live_attempt_failure_noninterference1984=True,
                  reserved_slot_refusal_explicit1984=True, retained_queue_reason_upper_bound_adapter1985_verified=True,
                  physical_android_tested=False, device_speedup_verified=False,
                  production_source_sha256=sha(production), retained_harness_sha256=sha(original),
                  version_adapted_harness_sha256=sha(copied), retained_queue_harness_sha256=sha(queue_original),
                  upper_bound_adapted_harness_sha256=sha(queue_copied), retained84_harness_sha256=sha(old_harness),
                  harness_source_sha256=sha(harness), runner_source_sha256=sha(__file__))
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
