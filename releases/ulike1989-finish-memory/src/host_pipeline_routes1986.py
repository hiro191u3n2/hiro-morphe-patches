#!/usr/bin/env python3
"""Retain the complete .82 pipeline route suite and add focused .86 finish proof.

The retained route suite executes unchanged real CPU/JNI/host-Mesa numerical
tests and its historical dispatch negative controls. The nested finish suite
then runs both the pinned .85 negative control and the current .86 admission
tests with explicitly controlled complete render outputs and elapsed times.
Only assertions executed in this invocation contribute to the returned total.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json

RETAINED82_SHA256 = 'c2a0cbf5e9032451b77a027a886dd3bb1556aaf775606da8facc19d54efeb978'
REQUIRED_FLAGS = (
    'pipeline_retained_route_suite1986',
    'pipeline_finish_baseline_suite1986',
)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    historical = source / 'host_pipeline_routes1982.py'
    if sha(historical) != RETAINED82_SHA256:
        raise AssertionError('The frozen .82 route runner changed')
    retained = module('pipeline86_retained_routes82', historical)
    finish = module('pipeline86_finish_baseline', source / 'host_finish_baseline1986.py')
    production = {path.name: sha(path) for path in source.glob('*.java')}
    result = retained.test(source, work / 'retained82', jdk=jdk, ndk=ndk)
    if result.get('status') != 'passed' or result.get('pipeline_diagnostic_closure_adapter1982_verified') is not True:
        raise AssertionError('The full unchanged .82 pipeline route suite did not pass')
    retained_assertions = result['assertions']
    finish_report = finish.test(source, work / 'finish_baseline', jdk=jdk, ndk=ndk)
    for flag in finish.REQUIRED_FLAGS:
        if finish_report.get(flag) is not True:
            raise AssertionError('The nested finish baseline gate did not execute: ' + flag)
    if production != {path.name: sha(path) for path in source.glob('*.java')}:
        raise AssertionError('Production source changed during the complete .86 route suite')
    if sha(historical) != RETAINED82_SHA256:
        raise AssertionError('The historical route runner changed during execution')
    result['assertions'] = retained_assertions + finish_report['assertions']
    result['finish_baseline'] = finish_report
    result.update({flag: True for flag in REQUIRED_FLAGS})
    result.update(retained_pipeline_routes1982_sha256=RETAINED82_SHA256,
                  current_invocation_assertion_counts={
                      'retained_route_suite': retained_assertions,
                      'finish_published85': finish_report['cases']['published85']['assertions'],
                      'finish_current86': finish_report['cases']['current86']['assertions']},
                  runner_source_sha256=sha(__file__))
    (work / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
