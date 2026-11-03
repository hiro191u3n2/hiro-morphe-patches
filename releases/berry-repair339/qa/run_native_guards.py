#!/usr/bin/env python3
"""Bind native structure rejection tests to the exact built provider sources."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
FIXTURE = ROOT.parent / 'private_inputs'
PREFIX = 'app/hiro/berrybfcache/patches/'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--dist', type=Path, required=True)
    args = parser.parse_args()
    dist = args.dist.resolve()
    config = json.loads((ROOT / 'tool-paths.json').read_text())
    build = json.loads((dist / 'QA_build339.json').read_text())
    for name in ('BfCacheRepairPatch.java', 'BfCacheStructure.java', 'NativeCompletionStructure.java'):
        path = ROOT / 'src' / PREFIX / name
        assert build['source_files'][PREFIX + name]['sha256'] == sha(path), name
    classes = ROOT / 'qa/guard-classes'
    classes.mkdir(exist_ok=True)
    audit = ROOT / 'qa/native_audit'
    sources = [audit / (name + '.java') for name in (
        'NativeCompletionTest', 'NativeCompletionGuardTest',
        'NativeMetadataGuardTest', 'StartedArgumentsGuardTest')]
    cp = os.pathsep.join(map(str, (dist / 'work/classes', dist / 'work/baseline-jvm.jar',
                                  config['tools']['classpath.jar']['path'])))
    subprocess.run([config['java'], '-jar', config['tools']['ecj.jar']['path'],
                    '-source', '11', '-target', '11', '-encoding', 'UTF-8',
                    '-cp', cp, '-d', str(classes), *map(str, sources)], check=True)
    cp = str(classes) + os.pathsep + cp
    old = FIXTURE / 'Berry_3.83.74_original_base.apk'
    renamed = ROOT.parent / 'private_qa/Berry_synthetic_renamed_unsigned.apk'
    mappings = json.loads((ROOT.parent / 'private_qa/synthetic-fixture.json').read_text())['types']
    original_types = ['Lc70;', 'Lc41;', 'Ltt5;', 'Lcx;']
    cases = [
        ('NativeCompletionTest', [old, *original_types], 'old_native.log'),
        ('NativeCompletionTest', [renamed, *[mappings[t] for t in original_types]], 'renamed_native.log'),
        ('NativeCompletionGuardTest', [old], 'existing_guards.log'),
        ('NativeMetadataGuardTest', [old], 'metadata_guards.log'),
        ('StartedArgumentsGuardTest', [], 'started_arguments_guards.log'),
    ]
    for test, parameters, log in cases:
        result = subprocess.run([config['java'], '-cp', cp, 'app.hiro.berrybfcache.patches.' + test,
                                 *map(str, parameters)], capture_output=True, text=True)
        (audit / log).write_text(result.stdout + result.stderr)
        if result.returncode:
            raise RuntimeError(test + ' failed:\n' + result.stdout + result.stderr)
    assert (audit / 'existing_guards.log').read_text().count('PASS rejected ') == 4
    assert (audit / 'metadata_guards.log').read_text().count('PASS rejected ') == 9
    assert (audit / 'started_arguments_guards.log').read_text().count('PASS rejected ') == 6
    metadata = {
        'result': 'PASS', 'source_sha256': sha(ROOT / 'src' / PREFIX / 'NativeCompletionStructure.java'),
        'clean_apk_tested': '3.83.74', 'synthetic_renamed_passed': True,
        'actual_3_84_12_tested': False, 'physical_device_tested': False,
        'new_negative_guard_cases': 9, 'existing_negative_guard_cases': 4,
        'abi': 'hiroCompleteBfcache(WebView,String)Z', 'metadata': 'Ltt5;->l:Lns5;',
        'url': 'Lns5;->n:Ljava/lang/String;', 'title': 'Lns5;->o:Ljava/lang/String;',
    }
    (audit / 'metadata_qa.json').write_text(json.dumps(metadata, indent=2) + '\n')
    tail = {
        'schema': 'berry-repair339-started-arguments-guard-v1', 'result': 'PASS',
        'provider_source_sha256': sha(ROOT / 'src' / PREFIX / 'BfCacheRepairPatch.java'),
        'test_source_sha256': sha(audit / 'StartedArgumentsGuardTest.java'),
        'test_log_sha256': sha(audit / 'started_arguments_guards.log'),
        'negative_guard_cases': 6, 'local_writes_and_argument_reads_allowed': True,
        'wide_register_overlap_rejected': True, 'physical_device_tested': False,
    }
    (audit / 'started_arguments_qa.json').write_text(json.dumps(tail, indent=2) + '\n')
    public_audit = ROOT / 'audit_native'
    public_audit.mkdir(exist_ok=True)
    for name in ('NativeMetadataGuardTest.java', 'StartedArgumentsGuardTest.java',
                 'metadata_qa.json', 'started_arguments_qa.json',
                 *[case[2] for case in cases]):
        shutil.copyfile(audit / name, public_audit / name)
    print('PASS native metadata 9 + existing 4 + tail-argument 6 rejection guards; original and synthetic positive cases')


if __name__ == '__main__':
    main()
