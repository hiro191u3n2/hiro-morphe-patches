#!/usr/bin/env python3
"""Fresh call-site inverse, deliberate DEX mutations and live Java counter checks.

Structural inventory checks are reported separately from functional assertions.
Neither proves that a vendor hint was previously applied or that the device is faster.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import subprocess
import zipfile

BASE_SHA = '674bddd1058f5b1ad72bda7f3798aae8b6f54c862a5dd19d3017bd0c9ce43c2a'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(command, log, expect=True):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=180)
    log.parent.mkdir(parents=True, exist_ok=True)
    log.write_text(result.stdout + result.stderr)
    if expect and result.returncode:
        raise AssertionError(result.stdout + result.stderr)
    if not expect and result.returncode == 0:
        raise AssertionError('Deliberately broken counter was accepted')
    return result.stdout


def counter(source, work, jdk, helper=None, expect=True):
    classes = work / 'classes'
    classes.mkdir(parents=True)
    run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-d', classes,
         helper or source / 'CaptureBoost19123.java', source / 'tests19123/IndependentCounterReview.java'], work / 'compile.log')
    return run([jdk / 'bin/java', '-ea', '-cp', classes, 'IndependentCounterReview'], work / 'execution.log', expect)


def test(source, work, jdk, tools, baseline, candidate):
    source, work, jdk, tools, baseline, candidate = map(lambda value: Path(value).resolve(),
        (source, work, jdk, tools, baseline, candidate))
    if work.exists():
        raise AssertionError('Use a fresh stock-boost host work directory')
    work.mkdir(parents=True)
    if sha(baseline) != BASE_SHA or baseline.stat().st_size != 1635446:
        raise AssertionError('Exact released .122 baseline required')
    tracked = ['CaptureBoost19123.java', 'StockBoostHooks19123.java', 'MergePayloads.java',
               'host_stock_boost19123.py', 'tests19123/IndependentCallsiteReview.java',
               'tests19123/IndependentCounterReview.java']
    pins = {name: sha(source / name) for name in tracked}
    for label, archive in (('baseline', baseline), ('candidate', candidate)):
        with zipfile.ZipFile(archive) as zipped:
            for name in ('ulike/methods.dex', 'ulike/methods.tsv', 'ulike/runtime.dex'):
                path = work / label / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(zipped.read(name))
            if label == 'candidate':
                for name in zipped.namelist():
                    if name.startswith('app/hiro/ulike/patches/') and name.endswith('.class'):
                        path = work / 'patch-classes' / name
                        path.parent.mkdir(parents=True, exist_ok=True)
                        path.write_bytes(zipped.read(name))
    classes = work / 'validators'
    classes.mkdir()
    cp = os.pathsep.join(map(str, (tools / 'morphe.jar', work / 'patch-classes', classes)))
    run([jdk / 'bin/javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes,
         source / 'MergePayloads.java', source / 'StockBoostHooks19123.java',
         source / 'tests19123/IndependentCallsiteReview.java'], work / 'validator-compile.log')
    base, next_ = work / 'baseline/ulike', work / 'candidate/ulike'
    run([jdk / 'bin/java', '-cp', cp, 'StockBoostHooks19123', base / 'methods.dex', next_ / 'methods.dex',
         base / 'methods.tsv', next_ / 'methods.tsv', work / 'stock-boost-proof.json'], work / 'stock-boost-proof.log')
    structural = run([jdk / 'bin/java', '-cp', cp, 'IndependentCallsiteReview', base / 'methods.dex',
                      next_ / 'methods.dex', next_ / 'runtime.dex'], work / 'independent-callsite.log')
    match = re.search(r'assertions=(\d+) negatives=(\d+)', structural)
    if match is None or int(match.group(2)) != 4:
        raise AssertionError('Independent DEX mutation controls missing')
    behavior = counter(source, work / 'production', jdk)
    checks = re.search(r'behavior checks=(\d+)', behavior)
    if checks is None or int(checks.group(1)) != 5:
        raise AssertionError('Counter behavior checks missing')
    original = (source / 'CaptureBoost19123.java').read_text()
    mutants = {
        'counter-does-not-observe-request': ('if (suppressed < Long.MAX_VALUE) suppressed++;', ''),
        'counter-wraps-instead-of-saturating': ('if (suppressed < Long.MAX_VALUE) suppressed++;', 'suppressed++;'),
        'receiver-retained-in-static-field': ('private static long suppressed;', 'private static long suppressed; private static Object retained;'),
    }
    negative = [
        {'name': name, 'status': 'rejected', 'kind': 'dex_mutation'}
        for name in ('suppression-removed', 'next-capture-guard-lost', 'rejection-return-changed', 'wrong-receiver')]
    for name, (old, new) in mutants.items():
        if original.count(old) != 1:
            raise AssertionError('Missing mutation point: ' + name)
        edited = original.replace(old, new)
        if name == 'receiver-retained-in-static-field':
            edited = edited.replace('if (suppressed < Long.MAX_VALUE) suppressed++;',
                                    'retained=ignoredReceiver; if (suppressed < Long.MAX_VALUE) suppressed++;')
        helper = work / 'mutants' / name / 'CaptureBoost19123.java'
        helper.parent.mkdir(parents=True)
        helper.write_text(edited)
        counter(source, work / name, jdk, helper, False)
        negative.append({'name': name, 'status': 'rejected', 'kind': 'java_mutation'})
    if pins != {name: sha(source / name) for name in tracked}:
        raise AssertionError('Stock boost test inputs changed during execution')
    proof = json.loads((work / 'stock-boost-proof.json').read_text())
    report = {
        'schema': 'ulike19123-stock-boost-host-v1', 'status': 'passed',
        'assertions': int(checks.group(1)), 'structural_checks': int(match.group(1)),
        'functional_scope': 'actual Java counter, ignored receiver, concurrent calls, saturation and primitive-only state',
        'structural_scope': 'actual emitted DEX single-callsite full inverse and all other payload methods/contracts',
        'negative_controls': negative, 'source_sha256': pins,
        'artifact': {'filename': candidate.name, 'bytes': candidate.stat().st_size, 'sha256': sha(candidate)},
        'baseline': {'bytes': baseline.stat().st_size, 'sha256': sha(baseline)},
        'stock_boost_patch': proof,
        'physical_android_tested': False, 'previous_vendor_activation_measured': False,
        'device_cause_confirmed': False, 'device_speedup_verified': False,
    }
    (work / 'result.json').write_text(json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('source', 'work', 'jdk', 'tools', 'baseline', 'candidate'):
        parser.add_argument('--' + name, required=True)
    args = parser.parse_args()
    print(json.dumps(test(**vars(args)), ensure_ascii=False, indent=2))
