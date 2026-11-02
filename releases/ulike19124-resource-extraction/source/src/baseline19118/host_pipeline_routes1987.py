#!/usr/bin/env python3
"""Keep all .86 routes/native tests and the current complete Finish proof suite.

The sole nested-runner substitution supplies the exact .87 queue-only source
inverse for Finish preservation. Actual runtime production and all prior
CPU/JNI/GLSL, quality, ownership and failure assertions execute unchanged.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json

RETAINED86_SHA256 = '7c694eea61e0dfad01debc3c9159120bbbd8ee0458ae5c7fbaa4fd2a3c0ef946'
REQUIRED_FLAGS = ('pipeline_current_route_suite1987_verified', 'pipeline_current_finish_baseline_suite1987_verified')


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec);spec.loader.exec_module(result)
    return result


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve();work.mkdir(parents=True, exist_ok=True)
    path = source / 'host_pipeline_routes1986.py'
    if sha(path) != RETAINED86_SHA256:
        raise AssertionError('Frozen .86 pipeline route runner changed')
    retained = module('pipeline87_retained_routes86', path);original = retained.module;observed = []

    def load(name, requested):
        if Path(requested).resolve() == source / 'host_finish_baseline1986.py':
            observed.append('finish_baseline1987')
            return module(name + '_current87', source / 'host_finish_baseline1987.py')
        return original(name, requested)

    retained.module = load
    result = retained.test(source, work / 'retained86', jdk=jdk, ndk=ndk)
    if observed != ['finish_baseline1987'] or any(result.get(flag) is not True for flag in retained.REQUIRED_FLAGS):
        raise AssertionError('Complete retained numerical and Finish suites must execute once')
    if result['finish_baseline'].get('finish_current_complete_baseline_suite1987_verified') is not True:
        raise AssertionError('Nested current-source Finish preservation/proof evidence absent')
    if sha(path) != RETAINED86_SHA256:
        raise AssertionError('Frozen pipeline runner changed during execution')
    result.update({flag: True for flag in REQUIRED_FLAGS})
    result.update(nested_runner_substitution1987=observed, retained_routes86_sha256=RETAINED86_SHA256,
                  runner_source_sha256=sha(__file__))
    (work / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True);parser.add_argument('--work', required=True)
    parser.add_argument('--jdk');parser.add_argument('--ndk');args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
