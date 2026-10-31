#!/usr/bin/env python3
"""Fail closed unless the exact release is bound to fresh source/build/APK evidence."""
import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
REPO = ROOT.parents[1]
BUNDLE = 'Hiro_Morphe_Patches_v1.0.319.mpp'
SINGLE = 'OneBack_Exit_NoRecents_v1.1.28.mpp'
BASE_SHA = '170367733645b3ae3391a5d58f9426d7c43c01e02b6091b8c5cfc6948d1309af'
ORIGINAL_SHA = '22f20062d16997e26009e73b0425974efef8ac69c76dd8912ee8a091213c39e2'
HOST_SHA = '4ffa8ccabcc475a18781fcc8642c721eed35ede7d43f967c4f0bd0f51a964a29'
PIKO_SHA = 'a7658d042676d750ff68b782ff02e65d077c011d6a6f09031c05b39e80fcc7a3'
WORKFLOW = '.github/workflows/homeback135-publish.yml'
SDK_NAMES = {'morphe.jar', 'morphe22.jar', 'morphe200.jar', 'manager237-host.jar'}


def require(ok, message):
    if not ok:
        raise RuntimeError(message)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def safe(root, name):
    require(isinstance(name, str) and name and '\\' not in name and '\x00' not in name
            and not Path(name).is_absolute() and all(x not in ('', '.', '..') for x in name.split('/')),
            'Unsafe evidence path')
    path = root / name
    require(path.is_file() and not path.is_symlink(), 'Evidence file missing or symlink: ' + name)
    require(path.resolve().is_relative_to(root.resolve()), 'Evidence escapes its root')
    return path


def pins():
    value = json.loads((HERE/'pins.json').read_text())
    require(value.get('schema') == 'homeback135-publication-pins-v1' and value.get('ready') is True,
            'Publication pins are not finalized; this is a non-publishable draft')
    require(set(value['bundle']) == {'bytes', 'sha256'} and value['bundle']['bytes'] > 0
            and re.fullmatch('[0-9a-f]{64}', value['bundle']['sha256']) is not None,
            'Final artifact identity missing')
    for branch in ('main', 'dev'):
        require(re.fullmatch('[0-9a-f]{40}', value['reviewed_'+branch]) is not None,
                'Reviewed branch head missing')
    host = value.get('host_suites', {})
    require(isinstance(host.get('count'), int) and host['count'] > 34
            and isinstance(host.get('new_required'), list) and host['new_required']
            and all(isinstance(name, str) and re.fullmatch(r'Back135[A-Za-z0-9]+Test', name)
                    for name in host['new_required']),
            'New first-profile Back host expectations are not finalized')
    return value


def inventory():
    result = {}
    for path in sorted(ROOT.rglob('*')):
        if not path.is_file() or '__pycache__' in path.parts or path.suffix in ('.pyc', '.class'):
            continue
        if path == ROOT/'verification/verified.json':
            continue
        if path.parent == ROOT and (path.name.startswith('publication_')
                                   or path.suffix in ('.json', '.txt', '.zip')):
            continue
        require(not path.is_symlink(), 'Source/evidence symlink')
        require(path.suffix not in ('.apk', '.apkm', '.mpp'), 'Private APK/artifact in source tree')
        result[path.relative_to(REPO).as_posix()] = sha(path)
    result[WORKFLOW] = sha(REPO/WORKFLOW)
    return result


def build_report(name, artifact):
    report = json.loads(safe(ROOT, name).read_text())
    require(report.get('schema') == 'homeback135-build-v1'
            and report.get('version') == '1.1.28' and report.get('bundle_version') == '1.0.319'
            and report.get('baseline_version') == '1.0.318' and report.get('baseline_sha256') == BASE_SHA
            and report.get('physical_android_tested') is False,
            'Build provenance differs')
    require(report.get('target_package') == 'com.instagram.android'
            and report.get('target_version') == '452.1.0.53.64', 'Build target differs')
    require(set(report['artifacts']) == {BUNDLE, SINGLE} and report['artifacts'][BUNDLE] == artifact,
            'Build artifact identity differs')
    for name, digest in report['sources'].items():
        require(sha(safe(ROOT/'src', name)) == digest, 'Build source changed: ' + name)
    require(report.get('all_nonowned_loader_definitions_preserved') is True
            and report.get('all_nonowned_zip_entries_preserved_except_manifest_and_loader_dex') is True,
            'Unrelated patch preservation was not verified')
    require(report['toolchain_sha256']['manager237-host.jar'] == HOST_SHA,
            'Reported Manager patcher host differs')
    require(set(report['metadata']) == SDK_NAMES
            and all(v.startswith('PASS Metadata135Audit') for v in report['metadata'].values()),
            'Four loader metadata checks missing')
    require(set(report['mutable_members']) == SDK_NAMES
            and all(v.startswith('PASS Instagram132MutableContextAudit snapshot-members:')
                    for v in report['mutable_members'].values()), 'Mutable context checks missing')
    required = {'Back130InstagramProfileLinkTest', 'Back131InstagramProfileIdentityTest',
                'Back131InstagramProfileRouteTest', 'Back133InstagramDispatcherProfileTest',
                'Back134InstagramNativeHomeTest', 'Back134InstagramFeedSwitcherTest',
                'Back134InstagramLocaleProfileTest'}
    expected_host = pins()['host_suites']
    required.update(expected_host['new_required'])
    require(required <= set(report['host']) and len(report['host']) == expected_host['count'],
            'Retained/new host suites missing')
    require(all(('PASS' in value or 'SUCCESS:' in value or re.fullmatch(r'REGRESSION(?: [^:]+)?: [0-9]+ cases, 0 failed', value))
                for value in report['host'].values()), 'Host suite did not pass')
    require('PASS' in report['loader_preservation'] and 'PASS' in report['runtime_preservation']
            and report['dex_structure'].startswith('PASS selftest:'), 'DEX preservation/structure check missing')
    return report


def application(row, artifact):
    require(row.get('result') == 'PASS' and row.get('physical_android_tested') is False
            and row.get('bundle_sha256') == artifact['sha256']
            and row.get('manager_host_sha256') == HOST_SHA
            and row.get('input_apk_sha256') == ORIGINAL_SHA,
            'Application evidence does not match final release/original/host')
    require(re.fullmatch('[0-9a-f]{64}', row['output_apk_sha256']) is not None
            and row['output_apk_bytes'] > 143237227, 'Patched output identity missing')
    events = [json.loads(line) for line in safe(ROOT, row['events']).read_text().splitlines() if line.strip()]
    finished = [event for event in events if event.get('type') == 'finished']
    require(len(finished) == 1 and finished[0].get('failure', 'absent') is None,
            'Exact Manager host apply did not finish successfully')
    require(row.get('runtime_matches_final_mpp') is True, 'Final emitted runtime was not checked')
    audits = row.get('audits', [])
    require(audits, 'APK audits absent')
    for audit in audits:
        text = safe(ROOT, audit['path']).read_text()
        require(audit['pass_line'].startswith('PASS ') and audit['pass_line'] in text.splitlines(),
                'Required APK audit pass marker absent')
    if row['name'] == 'hiro_only':
        require(row.get('selected_patch_count') == 1
                and row.get('whole_original_native_bodies_preserved') is True,
                'Whole original APK preservation proof missing')
    else:
        require(row.get('piko_sha256') == PIKO_SHA and row.get('piko_version') == '3.11.0-dev.3'
                and row.get('piko_default_patch_count') == 64 and row.get('selected_patch_count') == 65
                and row.get('native_hooks_verified') is True,
                'Exact Piko default integration proof missing')


def verify(bundle, report_path):
    pin = pins()
    artifact = pin['bundle']
    require(bundle.stat().st_size == artifact['bytes'] and sha(bundle) == artifact['sha256'],
            'Final release bytes differ')
    with zipfile.ZipFile(bundle) as z:
        require(len(z.namelist()) == len(set(z.namelist())) and z.testzip() is None, 'MPP ZIP corruption')
    report = json.loads(report_path.read_text())
    require(report.get('schema') == 'homeback135-verified-build-v1' and report.get('complete') is True
            and report.get('physical_android_tested') is False
            and report.get('bundle_version') == '1.0.319' and report.get('oneback_version') == '1.1.28'
            and report.get('artifact') == artifact, 'Frozen release evidence incomplete')
    require(report['original_apk'] == {'package':'com.instagram.android', 'version':'452.1.0.53.64',
            'bytes':143237227, 'sha256':ORIGINAL_SHA}, 'Original APK identity differs')
    require(len(report['builds']) == 2 and len(set(report['builds'])) == 2,
            'Two independent build reports required')
    first, second = [build_report(path, artifact) for path in report['builds']]
    require(first == second, 'The two clean build reports/artifacts differ')
    rows = report['applications']
    require(len(rows) == 3 and {row['name'] for row in rows} == {'hiro_only', 'piko_then_hiro', 'hiro_then_piko'},
            'Original APK / both Piko order checks required')
    for row in rows:
        application(row, artifact)
    require(report['source_and_evidence_sha256'] == inventory(), 'Frozen source/evidence inventory changed')
    require((REPO/WORKFLOW).read_bytes() == (HERE/'workflow.yml').read_bytes(), 'Workflow mirror differs')
    notes = (HERE/'RELEASE_NOTES.txt').read_text()
    require('DRAFT' not in notes and notes.strip() and '実機未確認' in notes,
            'Final release notes must state handset validation limit')
    print('PASS release135 gate: exact final bytes, two clean builds, original APK and both Piko orders, scoped preservation')
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bundle', type=Path, required=True)
    parser.add_argument('--report', type=Path, required=True)
    args = parser.parse_args()
    verify(args.bundle, args.report)


if __name__ == '__main__':
    main()
