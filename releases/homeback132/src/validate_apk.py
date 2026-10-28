#!/usr/bin/env python3
"""Apply and independently inspect final MPPs alone and with the reported Piko.

The original and rebuilt APKs stay in the supplied private work directory.
Only content hashes, successful audit summaries and patcher results form the
publication proof. This runner does not claim physical Android operation.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
from build import SINGLE, BUNDLE, TOOLS, BASE_BYTES, BASE_SHA

PATCH = '戻る1回で終了・履歴削除'
INPUT = {
    'package': 'com.instagram.android', 'version_name': '452.1.0.53.64',
    'version_code': 385811006, 'bytes': 143237227,
    'sha256': '22f20062d16997e26009e73b0425974efef8ac69c76dd8912ee8a091213c39e2',
}


def require(value, why):
    if not value:
        raise RuntimeError(why)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + '\n').encode()


def resource_table_sha(path):
    with zipfile.ZipFile(path) as archive:
        return hashlib.sha256(archive.read('resources.arsc')).hexdigest()


def resources(original, output, baseline, table_audit):
    def mutable(name):
        return (name in ('AndroidManifest.xml', 'resources.arsc')
                or re.fullmatch(r'classes(?:[0-9]+)?\.dex', name)
                or re.fullmatch(r'META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))', name, re.I))
    def inventory(path):
        result = {}
        with zipfile.ZipFile(path) as archive:
            seen = set()
            for entry in archive.infolist():
                require(entry.filename not in seen, 'Duplicate APK entry')
                seen.add(entry.filename)
                if entry.is_dir() or mutable(entry.filename):
                    continue
                result[entry.filename] = hashlib.sha256(archive.read(entry)).hexdigest()
        return result
    before, after = inventory(original), inventory(output)
    changed = sorted(name for name in before.keys() | after.keys() if before.get(name) != after.get(name))
    require(not changed, 'Unexpected APK resource/native-library delta: ' + repr(changed[:12]))
    require(table_audit.startswith('PASS InstagramResourcesAudit'), 'Resource table semantic proof absent')
    return {'result': 'PASS', 'byte_identical_entries': len(before),
            'native_libraries_byte_identical': True,
            'resource_table': {'result': 'PASS', 'audit': table_audit,
                               'original_sha256': resource_table_sha(original),
                               'patched_sha256': resource_table_sha(output),
                               'baseline_sha256': resource_table_sha(baseline),
                               'baseline_semantics_identical': True,
                               'resource_ids_and_values_preserved': True},
            'excluded': ['DEX code (independent semantic audit)',
                         'AndroidManifest.xml (independent exact semantic audit)',
                         'resources.arsc (independent original/baseline/current semantic audit)',
                         'APK signature metadata']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('apk', 'piko', 'dist', 'base', 'tools', 'build-work', 'work'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--proof', type=Path, default=ROOT/'validation/real-apk.json')
    args = parser.parse_args()
    for name in vars(args):
        setattr(args, name, getattr(args, name).resolve())
    require(args.apk.stat().st_size == INPUT['bytes'] and sha(args.apk) == INPUT['sha256'],
            'Use the exact original Instagram452 APK')
    for name in ('morphe.jar', 'morphe22.jar', 'manager237.apk', 'manager237-host.jar'):
        require(sha(args.tools/name) == TOOLS[name], 'Pinned tool changed: ' + name)
    baseline_mpp = args.base
    require(baseline_mpp.stat().st_size == BASE_BYTES and sha(baseline_mpp) == BASE_SHA,
            'Use exact published baseline311 for resource comparison')
    require(not args.work.exists(), 'Use a fresh actual-APK work directory')
    args.work.mkdir(parents=True)
    artifacts = {name: {'bytes': (args.dist/name).stat().st_size, 'sha256': sha(args.dist/name)}
                 for name in (SINGLE, BUNDLE)}
    classes = args.work/'audit'
    classes.mkdir()
    cp = os.pathsep.join(map(str, [classes, args.build_work/'audit', args.dist/SINGLE, args.tools/'morphe.jar']))

    def run(command, name, timeout=1800):
        print('RUN ' + name, flush=True)
        with (args.work/(name+'.log')).open('w') as log:
            result = subprocess.run(list(map(str, command)), stdout=log, stderr=subprocess.STDOUT, timeout=timeout)
        output = (args.work/(name+'.log')).read_text()
        require(result.returncode == 0, 'Failed ' + name + '\n' + output[-12000:])
        return output

    def java(class_name, *values):
        return ['java', '-Xmx5g', '-ea', '-cp', cp, class_name, *values]

    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes,
         ROOT/'validation/InstagramManifestAudit.java', ROOT/'validation/InstagramResourcesAudit.java'],
        'resource-audits-compile', 180)
    original_audit = run(java('Instagram130ProfileLinkAudit', 'original', args.apk), 'original-profile-contract')
    require('PASS Instagram130ProfileLinkAudit original' in original_audit, 'Original boundary proof absent')
    require('PASS Instagram132MutableContextAudit:' in original_audit,
            'Actual mutable-context regression proof absent')

    # Use the unmodified host embedded in the user's exact Manager release.
    # Its STRIP_FAST pipeline and real cross-source selections complement the
    # independent legacy FULL rebuilds below. A successful process exit alone
    # does not mean that the Manager patching pipeline completed successfully.
    manager_work = args.work/'manager'
    manager_proof_path = args.work/'manager-compatibility.json'
    run([sys.executable, ROOT/'validate_manager.py', '--apk', args.apk, '--piko', args.piko,
         '--base', baseline_mpp, '--dist', args.dist,
         '--host', args.tools/'manager237-host.jar', '--manager-apk', args.tools/'manager237.apk',
         '--work', manager_work, '--proof', manager_proof_path],
        'manager-compatibility', 7200)
    manager_proof = json.loads(manager_proof_path.read_text())
    require(manager_proof.get('result') == 'PASS' and manager_proof.get('complete') is True
            and manager_proof.get('artifacts') == artifacts,
            'Complete actual Manager proof for the final artifacts is absent')

    # Compare two real Manager outputs so that every existing Piko change is
    # part of the baseline. The independent audits permit only OneBack's
    # reviewed additions, not arbitrary deltas from the original APK.
    piko_baseline = manager_work/'aggregate-default-baseline/patched.apk'
    piko_combined = manager_work/'aggregate-default/patched.apk'
    manager_profile = run(java('Instagram130ProfileLinkAudit', 'output', piko_baseline, piko_combined),
                          'manager-piko-profile-audit')
    manager_profile_pass = next((line for line in manager_profile.splitlines()
                                if line.startswith('PASS Instagram130ProfileLinkAudit output')), None)
    require(manager_profile_pass, 'Piko combination profile hook audit did not pass')
    manager_native = run(java('ActualInstagram452Audit', piko_baseline, piko_combined, args.dist/BUNDLE),
                         'manager-piko-whole-apk-semantics')
    manager_native_pass = next((line for line in manager_native.splitlines()
                               if line.startswith('PASS whole fresh Instagram 452 APK semantics:')), None)
    require(manager_native_pass, 'Piko combination whole-APK semantics audit did not pass')
    manager_manifest = run(java('InstagramManifestAudit', '--preserve-baseline', piko_baseline, piko_combined),
                           'manager-piko-manifest-audit')
    require(manager_manifest.strip().startswith('PASS InstagramManifestAudit'),
            'Piko combination manifest preservation failed')
    manager_table = run(java('InstagramResourcesAudit', '--preserve-baseline', piko_baseline, piko_combined),
                        'manager-piko-resource-table-audit')
    manager_resources = resources(piko_baseline, piko_combined, piko_baseline, manager_table.strip())
    manager_preservation = {
        'result': 'PASS', 'baseline_case': 'aggregate-default-baseline', 'output_case': 'aggregate-default',
        'input_sha256': sha(piko_baseline), 'output_sha256': sha(piko_combined),
        'profile_link_audit': manager_profile_pass, 'existing_native_audit': manager_native_pass,
        'manifest_audit': manager_manifest.strip(), 'resources': manager_resources,
    }

    def apply(case_id, artifact):
        folder = args.work/case_id
        folder.mkdir()
        output, report = folder/'patched.apk', folder/'morphe-result.json'
        command = ['java', '-Xmx5g', '-jar', args.tools/'morphe22.jar', 'patch', args.apk,
                   '-p', artifact, '--exclusive', '-e', PATCH, '--unsigned',
                   '--bytecode-mode', 'FULL', '-o', output, '-t', folder/'temp', '-r', report]
        run(command, case_id+'-apply')
        result = json.loads(report.read_text())
        require(result.get('success') is True and result.get('failedPatches') == [], 'Patch failure')
        applied = [value if isinstance(value, str) else value.get('name')
                   for value in result.get('appliedPatches', [])]
        require(applied == [PATCH], 'Unexpected patch selection')
        steps = result.get('stepResults', result.get('patchingSteps', []))
        require(steps and all(row.get('success') is True for row in steps), 'Incomplete patch pipeline')
        require(result.get('packageName') == INPUT['package']
                and result.get('packageVersion') == INPUT['version_name'], 'Wrong package/version result')
        shutil.rmtree(folder/'temp', ignore_errors=True)
        return output, applied

    baseline, baseline_applied = apply('baseline-resource-reference', baseline_mpp)
    baseline_reference = {'artifact_sha256': BASE_SHA, 'input_sha256': INPUT['sha256'],
                          'patcher_sdk_sha256': TOOLS['morphe22.jar'],
                          'result': 'PASS', 'applied_patches': baseline_applied,
                          'output_apk_sha256': sha(baseline),
                          'resource_table_sha256': resource_table_sha(baseline)}
    cases = []
    for case_id, name in (('standalone', SINGLE), ('aggregate', BUNDLE)):
        output, applied = apply(case_id, args.dist/name)
        profile = run(java('Instagram130ProfileLinkAudit', 'output', args.apk, output), case_id+'-profile-audit')
        profile_pass = next((line for line in profile.splitlines()
                             if line.startswith('PASS Instagram130ProfileLinkAudit output')), None)
        require(profile_pass, 'Profile hook audit did not pass')
        native = run(java('ActualInstagram452Audit', args.apk, output, args.dist/name), case_id+'-whole-apk-semantics')
        native_pass = next((line for line in native.splitlines()
                            if line.startswith('PASS whole fresh Instagram 452 APK semantics:')), None)
        require(native_pass, 'Whole-APK semantics audit did not pass')
        manifest = run(java('InstagramManifestAudit', args.apk, output), case_id+'-manifest-audit')
        require(manifest.strip().startswith('PASS InstagramManifestAudit'), 'Manifest preservation failed')
        table_audit = run(java('InstagramResourcesAudit', args.apk, baseline, output), case_id+'-resource-table-audit')
        resource_proof = resources(args.apk, output, baseline, table_audit.strip())
        cases.append({
            'id': case_id, 'artifact': name, 'artifact_sha256': artifacts[name]['sha256'],
            'input_sha256': INPUT['sha256'], 'result': 'PASS',
            'selected_patches': [PATCH], 'applied_patches': applied, 'failed_patches': [],
            'steps_all_success': True, 'patcher_sdk': 'morphe22.jar',
            'patcher_sdk_sha256': TOOLS['morphe22.jar'],
            'output_apk_sha256': sha(output), 'output_apk_bytes': output.stat().st_size,
            'profile_link_audit': profile_pass, 'existing_native_audit': native_pass,
            'manifest_audit': manifest.strip(), 'resources': resource_proof,
        })
        print('PASS actual APK ' + case_id, flush=True)
    require(all(sha(args.dist/name) == row['sha256'] for name, row in artifacts.items()),
            'MPP changed during verification')
    proof = {'schema': 'homeback132-real-apk-v1', 'physical_android_tested': False,
             'input': INPUT, 'artifacts': artifacts, 'cases': cases,
             'baseline_resource_reference': baseline_reference,
             'original_profile_contract': original_audit.strip(),
             'manager_compatibility': manager_proof,
             'manager_preservation': manager_preservation,
             'limitations': ['No physical Samsung/Android17 execution in this environment.',
                             'The exact reported 68 selections/options are unknown; the actual Manager '
                             'matrix records the navigation pair and Piko64 plus OneBack default selections.']}
    args.proof.parent.mkdir(parents=True, exist_ok=True)
    args.proof.write_bytes(json_bytes(proof))
    print('PASS final MPPs applied to original Instagram452 alone and with Piko; independently audited', flush=True)


if __name__ == '__main__':
    main()
