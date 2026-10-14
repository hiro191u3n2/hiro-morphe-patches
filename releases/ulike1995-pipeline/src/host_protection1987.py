#!/usr/bin/env python3
"""Run production protection routing and all-coordinate Java finishing policy.

The published .86 helper must fail the duplicate-work regression; the current
helper must pass. GPU transport and queue state are explicit controlled peers.
This suite proves control flow, exact policy integers and ownership, not Android
GPU performance. Pixel/native kernels and the existing proof gates are unchanged.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import subprocess
import sys

BASELINE86_SHA256 = 'c356658167d872177b5dc2c80e346a1875532b32b0d7eccc5232d8508dd13686'
REQUIRED_FLAGS = (
    'protection_published86_duplicate_work_reproduced1987_verified',
    'protection_blocked_probe_skips_duplicate_work1987_verified',
    'protection_complete_policy_exact1987_verified',
    'protection_qualified_and_memory_handoff1987_verified',
    'protection_queue_progress1987_verified',
    'protection_rejection_and_allocation_fallback1987_verified',
    'protection_cancellation_and_ownership1987_verified',
    'protection_reviewed_source_inverse1987_verified',
)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def inverse(current, baseline):
    """Only the early predicate and deferred allocation may differ from .86."""
    text = current.read_text()
    start = text.index('    /** A missing proof cannot use GPU in this call.')
    end = text.index('    static FinishPolicy1953.Band finishBand(', start)
    text = text[:start] + text[end:]
    before = '        int[] u=new int[32];float[] f=new float[32];'
    after = '        int[] masks=new int[pixels*2],u=new int[32];float[] f=new float[32];'
    if text.count(before) != 1:
        raise AssertionError('Reviewed descriptor declaration changed')
    text = text.replace(before, after)
    before = ('        try {\n            if(!finishOpportunity1987(key))return null;\n'
              '            int[] masks=new int[pixels*2];\n')
    after = '        if(!mayTry(key))return null;\n        try {\n'
    if text.count(before) != 1:
        raise AssertionError('Reviewed early decline changed')
    text = text.replace(before, after)
    if text.encode() != baseline.read_bytes():
        raise AssertionError('A nonreviewed protection method or pixel path changed')


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    jar = Path(os.environ.get('ULIKE_ANDROID_JAR', str(jdk.parent / 'android.jar'))).resolve()
    local = source / 'tests1987/protection'
    baseline = local / 'baseline86/GpuProtection1961.java'
    if sha(baseline) != BASELINE86_SHA256:
        raise AssertionError('The independently published .86 reference changed')
    inverse(source / 'GpuProtection1961.java', baseline)
    sys.path.insert(0, str(source))
    builder = module('protection87_compile_closure', source / 'build1985.py')
    inputs = list(dict.fromkeys(list(builder.compile_inputs().values()) +
                              [builder.production_path(name) for name in builder.PRODUCTION]))
    tests = [local / 'ProtectionPeers1987.java', local / 'ProtectionOpportunity1987Test.java']
    tracked = inputs + tests + [baseline, Path(__file__).resolve()]
    before = {str(path): sha(path) for path in tracked}
    commands = []

    def run(label, command, expected_failure=False):
        command = list(map(str, command))
        commands.append(dict(label=label, argv=command))
        result = subprocess.run(command, text=True, capture_output=True, timeout=240)
        output = result.stdout + result.stderr
        (work / (label + '.log')).write_text(output)
        if expected_failure:
            if result.returncode == 0 or ('Unqualified blocked proof must skip mask export and RAW4 oracle roundtrip' not in output):
                raise AssertionError('Published .86 did not reproduce the exact reviewed deficiency\n' + output[-15000:])
        elif result.returncode:
            raise AssertionError(label + '\n' + output[-15000:])
        return result.stdout

    production, peers, old = work / 'production', work / 'peers', work / 'published86'
    for directory in (production, peers, old):
        directory.mkdir(exist_ok=True)
    run('production-compile', [jdk / 'bin/javac', '-source', '8', '-target', '8', '-Xlint:-options',
        '-encoding', 'UTF-8', '-bootclasspath', jar, '-d', production, *inputs])
    cp = os.pathsep.join(map(str, (production, jar)))
    run('peers-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8',
        '-cp', cp, '-d', peers, *tests])
    run('published86-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8',
        '-cp', os.pathsep.join(map(str, (peers, production, jar))), '-d', old, baseline])
    prefix = [jdk / 'bin/java', '-ea', '-Xmx512m']
    run('published86-negative-repro', prefix + ['-cp', os.pathsep.join(map(str, (old, peers, production, jar))),
        'com.hiro.ulike.ProtectionOpportunity1987Test', 'published86-repro'], expected_failure=True)
    output = run('current87-protection', prefix + ['-cp', os.pathsep.join(map(str, (peers, production, jar))),
        'com.hiro.ulike.ProtectionOpportunity1987Test'])
    result = json.loads(output.strip().splitlines()[-1])
    if result.get('status') != 'passed' or result.get('assertions', 0) <= 0:
        raise AssertionError('No executed protection regression assertions')
    if before != {str(path): sha(path) for path in tracked}:
        raise AssertionError('Reviewed protection sources changed during execution')
    result.update(
        protection_published86_duplicate_work_reproduced1987_verified=True,
        protection_reviewed_source_inverse1987_verified=True,
        physical_android_tested=False, device_speedup_verified=False,
        actual_gpu_backend_executed=False,
        complete_production_policy_arithmetic_executed=True,
        controlled_peers=['GPU transport', 'qualification state', 'array allocation failures'],
        source_sha256=before, commands=commands,
        published86_sha256=BASELINE86_SHA256)
    if any(result.get(flag) is not True for flag in REQUIRED_FLAGS):
        raise AssertionError('Missing protection contract evidence')
    (work / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    args = parser.parse_args()
    value = test(args.source, args.work, args.jdk)
    print(json.dumps({key: value[key] for key in ('status', 'assertions', *REQUIRED_FLAGS)}))
