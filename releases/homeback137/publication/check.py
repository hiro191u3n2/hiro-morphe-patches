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
BUNDLE = 'Hiro_Morphe_Patches_v1.0.321.mpp'
SINGLE = 'OneBack_Exit_NoRecents_v1.1.30.mpp'
BASE_SHA = '170367733645b3ae3391a5d58f9426d7c43c01e02b6091b8c5cfc6948d1309af'
ORIGINAL_SHA = '22f20062d16997e26009e73b0425974efef8ac69c76dd8912ee8a091213c39e2'
HOST_SHA = '4ffa8ccabcc475a18781fcc8642c721eed35ede7d43f967c4f0bd0f51a964a29'
PIKO_SHA = 'a7658d042676d750ff68b782ff02e65d077c011d6a6f09031c05b39e80fcc7a3'
PIKO_BASELINE_SHA = '7b0992b62299806d33fd8fb6be9410075d50d12152c841d868062b64bda98866'
WORKFLOW = '.github/workflows/homeback137-publish.yml'
SDK_NAMES = {'morphe.jar', 'morphe22.jar', 'morphe200.jar', 'manager237-host.jar'}
BASELINE_HOST_SUITES = {'Back123InstagramTabBackTest', 'Back131InstagramProfileRouteTest', 'Back121InstagramActivityLayersTest', 'Back118InstagramGuardTest', 'Back121InstagramScopeTest', 'Back117InstagramTest', 'Back4753RejectedTest', 'Back121InstagramCurrentTest', 'Back123NativeReelsTest', 'Back124TwitterTest', 'Back128InstagramProfileTest', 'Back134InstagramLocaleProfileTest', 'Back125TwitterSystemTest', 'Back119InstagramTest', 'Back120MainDispatchTest', 'Back125TwitterKeyTest', 'Back134InstagramFeedSwitcherTest', 'Back127Instagram452Test', 'Back125TwitterTerminalTest', 'Back117TikTokTest', 'Back118InstagramTest', 'Back130InstagramProfileLinkTest', 'Back134InstagramNativeHomeTest', 'Back121InstagramDispatchTest', 'Back122InstagramDispatcherTest', 'Back4743TikTokTest', 'Back131InstagramProfileIdentityTest', 'Back119TwitterTest', 'Back120InstagramTest', 'Back4743NativeBackTest', 'Back133InstagramDispatcherProfileTest', 'Back126InstagramProfileTest', 'Back116Regression', 'HomeBackHostTest'}
REQUIRED_AUDITS = (
    'PASS Instagram137ProfileReelsAudit:',
    'PASS Instagram134NativeHomeAudit:',
    'PASS Instagram130ProfileLinkAudit output:',
    'PASS InstagramManifestAudit ',
    'PASS InstagramResourcesAudit ',
    'PASS whole fresh Instagram 452 APK semantics:',
)



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
    require(value.get('schema') == 'homeback137-publication-pins-v1' and value.get('ready') is True,
            'Publication pins are not finalized; this is a non-publishable draft')
    require(set(value['bundle']) == {'bytes', 'sha256'} and value['bundle']['bytes'] > 0
            and re.fullmatch('[0-9a-f]{64}', value['bundle']['sha256']) is not None,
            'Final artifact identity missing')
    for branch in ('main', 'dev'):
        require(re.fullmatch('[0-9a-f]{40}', value['reviewed_'+branch]) is not None,
                'Reviewed branch head missing')
    host = value.get('host_suites', {})
    require(isinstance(host.get('required'), list) and isinstance(host.get('new_required'), list)
            and host['new_required'] and len(host['new_required']) == len(set(host['new_required']))
            and all(name.startswith('Back137') for name in host['new_required'])
            and BASELINE_HOST_SUITES.isdisjoint(host['new_required'])
            and set(host['required']) == BASELINE_HOST_SUITES | set(host['new_required'])
            and len(host['required']) == host.get('count'), 'Host-suite expectations are not finalized')
    scope = value.get('preservation', {})
    require(isinstance(scope.get('runtime_classes'), int) and scope['runtime_classes'] > 0
            and isinstance(scope.get('native_hooks'), int) and scope['native_hooks'] >= 12
            and isinstance(scope.get('loader'), str) and scope['loader'].startswith('PASS Bundle137Audit loader:')
            and isinstance(scope.get('runtime'), str) and scope['runtime'].startswith('PASS Bundle137Audit runtime:'),
            'Exact loader/runtime/native scope is not finalized')
    require(isinstance(value.get('profile_reels_native_audit'), str)
            and value['profile_reels_native_audit'].startswith('PASS Instagram137ProfileReelsAudit:'),
            'Original native profile-to-Reels audit is not finalized')
    require(isinstance(value.get('piko_contract_native_audit'), str)
            and value['piko_contract_native_audit'].startswith('PASS Instagram137PikoContractAudit:'),
            'Independent Piko native compatibility audit is not finalized')
    require(isinstance(scope.get('jvm_changed'), list) and isinstance(scope.get('jvm_added'), list),
            'JVM loader scope missing')
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
    require(report.get('schema') == 'homeback137-build-v1'
            and report.get('version') == '1.1.30' and report.get('bundle_version') == '1.0.321'
            and report.get('baseline_version') == '1.0.318' and report.get('baseline_sha256') == BASE_SHA
            and report.get('physical_android_tested') is False,
            'Build provenance differs')
    require(report.get('original_apk_sha256') == ORIGINAL_SHA
            and report.get('profile_reels_native_audit') == pins()['profile_reels_native_audit'],
            'Original native profile-to-Reels contract audit differs')
    require(report.get('piko_baseline_sha256') == PIKO_BASELINE_SHA
            and report.get('piko_contract_native_audit') == pins()['piko_contract_native_audit'],
            'Independent Piko native compatibility audit differs')
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
            and all(v.startswith('PASS Metadata137Audit') for v in report['metadata'].values()),
            'Four loader metadata checks missing')
    require(set(report['mutable_members']) == SDK_NAMES
            and all(v.startswith('PASS Instagram132MutableContextAudit snapshot-members:')
                    for v in report['mutable_members'].values()), 'Mutable context checks missing')
    expected_host = pins()['host_suites']
    require(set(report['host']) == set(expected_host['required'])
            and len(report['host']) == expected_host['count'], 'Retained/new host suites missing')
    require(all(('PASS' in value or 'SUCCESS:' in value or re.fullmatch(r'REGRESSION(?: [^:]+)?: [0-9]+ cases, 0 failed', value))
                for value in report['host'].values()), 'Host suite did not pass')
    expected_scope = pins()['preservation']
    require(report.get('jvm_changed') == expected_scope['jvm_changed']
            and report.get('jvm_added') == expected_scope['jvm_added'], 'Exact JVM loader scope differs')
    require(report['loader_preservation'].strip() == expected_scope['loader']
            and report['runtime_preservation'].strip() == expected_scope['runtime']
            and report['dex_structure'].startswith('PASS selftest:'), 'Exact DEX preservation/structure scope differs')
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
    markers = [audit['pass_line'] for audit in audits]
    require(len(markers) == len(REQUIRED_AUDITS)
            and all(sum(marker.startswith(prefix) for marker in markers) == 1 for prefix in REQUIRED_AUDITS),
            'Required native / feature / whole APK / manifest / resource audit missing or duplicated')
    whole = next(marker for marker in markers if marker.startswith(REQUIRED_AUDITS[-1]))
    scope = pins()['preservation']
    require('nativeInstagramHooks=' + str(scope['native_hooks']) + ',' in whole
            and 'emittedRuntimeClasses=' + str(scope['runtime_classes']) + ',' in whole,
            'Native hook or emitted runtime scope changed')
    if row['name'] == 'hiro_only':
        require(row.get('selected_patch_count') == 1
                and row.get('whole_original_native_bodies_preserved') is True,
                'Whole original APK preservation proof missing')
    else:
        require(row.get('piko_sha256') == PIKO_SHA and row.get('piko_version') == '3.11.0-dev.3'
                and row.get('piko_default_patch_count') == 64 and row.get('selected_patch_count') == 65
                and row.get('native_hooks_verified') is True,
                'Exact Piko default integration proof missing')



def piko_output_compatibility(report, artifact):
    path = safe(ROOT, report['piko_output_compatibility'])
    proof = json.loads(path.read_text())
    require(proof.get('schema') == 'homeback137-piko-output-compatibility-v1'
            and proof.get('result') == 'PASS' and proof.get('physical_android_tested') is False
            and proof.get('mpp') == artifact
            and proof.get('compiled_classes_frozen_to_manager_summary') is True
            and proof.get('all_inputs_and_outputs_unchanged') is True
            and proof.get('source_build_proof_and_mpp_unchanged') is True
            and proof.get('frozen_source_files') == report['source_files'],
            'Independent final Piko output proof incomplete')
    summary_path = safe(ROOT, 'verification/manager-summary.json')
    require(proof['manager_summary'] == {'bytes':summary_path.stat().st_size, 'sha256':sha(summary_path)},
            'Piko output proof refers to another Manager summary')
    summary = json.loads(summary_path.read_text())
    require(summary['schema'] == 'homeback137-manager-final-v1' and summary['complete'] is True
            and summary['result'] == 'PASS' and summary['mpp'] == artifact,
            'Complete matching Manager summary required')
    require(proof['original_apk'] == {'bytes':143237227, 'sha256':ORIGINAL_SHA}
            and proof['piko_baseline'] == {'bytes':156624198, 'sha256':PIKO_BASELINE_SHA},
            'Independent Piko output input identities differ')
    require(proof['compiled_audit_and_patch_class_count'] > 0
            and proof['compiled_audit_and_patch_class_count'] ==
                len(summary['verification_tools']['compiled_build_audit_and_patch_classes']),
            'Frozen Piko audit tool inventory differs')
    driver = proof['driver']
    driver_path = safe(ROOT, 'verification/audit-sources/' + Path(driver['path']).name)
    require(driver['bytes'] == driver_path.stat().st_size and driver['sha256'] == sha(driver_path),
            'Independent Piko output driver changed')
    cases = proof['cases']
    require(set(cases) == {'piko64-hiro321', 'hiro321-piko64'}, 'Both final Piko outputs required')
    manager_cases = {row['case']:row for row in summary['cases']}
    release_names = {'piko64-hiro321':'piko_then_hiro', 'hiro321-piko64':'hiro_then_piko'}
    release_rows = {row['name']:row for row in report['applications']}
    for name, row in cases.items():
        release = release_rows[release_names[name]]
        require(row['result'] == 'PASS' and row['all_inputs_and_output_unchanged'] is True
                and row['output'] == manager_cases[name]['output']
                and row['output'] == {'bytes':release['output_apk_bytes'], 'sha256':release['output_apk_sha256']},
                'Independent Piko output differs: ' + name)
        log = safe(ROOT, 'verification/' + name + '/piko-compatibility-output-audit.txt')
        require(row['log'] == {'bytes':log.stat().st_size, 'sha256':sha(log)}
                and len(row['summary']) == 1
                and row['summary'][0].startswith('PASS Instagram137PikoContractAudit:')
                and row['summary'][0] in log.read_text().splitlines(),
                'Independent Piko output log differs: ' + name)


def verify(bundle, report_path):
    pin = pins()
    artifact = pin['bundle']
    require(bundle.stat().st_size == artifact['bytes'] and sha(bundle) == artifact['sha256'],
            'Final release bytes differ')
    with zipfile.ZipFile(bundle) as z:
        require(len(z.namelist()) == len(set(z.namelist())) and z.testzip() is None, 'MPP ZIP corruption')
    report = json.loads(report_path.read_text())
    require(report.get('schema') == 'homeback137-verified-build-v1' and report.get('complete') is True
            and report.get('physical_android_tested') is False
            and report.get('bundle_version') == '1.0.321' and report.get('oneback_version') == '1.1.30'
            and report.get('artifact') == artifact
            and report.get('intended_release_scope') == 'external_profile_back_to_reels_then_exit', 'Frozen release evidence incomplete')
    require(report['original_apk'] == {'package':'com.instagram.android', 'version':'452.1.0.53.64',
            'bytes':143237227, 'sha256':ORIGINAL_SHA}, 'Original APK identity differs')
    require(len(report['builds']) == 2 and len(set(report['builds'])) == 2,
            'Two independent build reports required')
    first, second = [build_report(path, artifact) for path in report['builds']]
    require(first == second, 'The two clean build reports/artifacts differ')
    require(report['source_files'] == len(first['sources'])
            and report['host_suite_count'] == len(first['host']), 'Frozen source/host counts differ')
    for name, marker in (
        ('profile-reels-audit.txt', pin['profile_reels_native_audit']),
        ('piko-contract-audit.txt', pin['piko_contract_native_audit']),
        ('loader-preservation.txt', pin['preservation']['loader']),
        ('runtime-preservation.txt', pin['preservation']['runtime']),
    ):
        require(marker in safe(ROOT, 'verification/build-audits/' + name).read_text().splitlines(),
                'Fresh build audit log differs: ' + name)
    for name, marker in first['host'].items():
        require(marker in safe(ROOT, 'verification/host/' + name + '.txt').read_text().splitlines(),
                'Fresh host audit log differs: ' + name)
    rows = report['applications']
    require(len(rows) == 3 and {row['name'] for row in rows} == {'hiro_only', 'piko_then_hiro', 'hiro_then_piko'},
            'Original APK / both Piko order checks required')
    for row in rows:
        application(row, artifact)
    piko_output_compatibility(report, artifact)
    require(report['source_and_evidence_sha256'] == inventory(), 'Frozen source/evidence inventory changed')
    require((REPO/WORKFLOW).read_bytes() == (HERE/'workflow.yml').read_bytes(), 'Workflow mirror differs')
    notes = (HERE/'RELEASE_NOTES.txt').read_text()
    require('DRAFT' not in notes and notes.strip() and '実機未確認' in notes,
            'Final release notes must state handset validation limit')
    print('PASS release137 gate: exact final bytes, two clean builds, original APK and both Piko orders, scoped preservation')
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bundle', type=Path, required=True)
    parser.add_argument('--report', type=Path, required=True)
    args = parser.parse_args()
    verify(args.bundle, args.report)


if __name__ == '__main__':
    main()
