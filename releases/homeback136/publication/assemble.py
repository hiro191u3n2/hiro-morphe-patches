#!/usr/bin/env python3
"""Collect completed local diagnostic QA; never upload, commit or publish."""
import argparse
import json
import shutil
from pathlib import Path
import check

HERE, ROOT = check.HERE, check.ROOT
CASES = {'hiro-only':'hiro_only', 'piko64-hiro320':'piko_then_hiro', 'hiro320-piko64':'hiro_then_piko'}
AUDITS = {
    'initial-profile':'PASS Instagram135InitialProfileAudit:',
    'native-home':'PASS Instagram134NativeHomeAudit:',
    'profile-link':'PASS Instagram130ProfileLinkAudit output:',
    'manifest':'PASS InstagramManifestAudit ',
    'resources':'PASS InstagramResourcesAudit ',
    'whole-apk':'PASS whole fresh Instagram 452 APK semantics:',
}


def identity(path):
    return {'bytes':path.stat().st_size, 'sha256':check.sha(path)}


def read(path):
    return json.loads(path.read_text())


def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True)+'\n')


def copy(source, relative, expected=None):
    check.require(source.is_file() and not source.is_symlink(), 'Evidence missing: '+str(source))
    if expected is not None:
        check.require(identity(source) == expected, 'Evidence identity changed: '+str(source))
    check.require(source.suffix not in ('.apk', '.apkm', '.mpp', '.dex', '.class'),
                  'App, disassembly or compiled binary cannot enter evidence tree')
    target = ROOT/relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)
    return relative


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--summary', type=Path, required=True)
    parser.add_argument('--first-dist', type=Path, required=True)
    parser.add_argument('--second-dist', type=Path, required=True)
    parser.add_argument('--workspace', type=Path, required=True)
    parser.add_argument('--finalize', action='store_true',
                        help='Set ready only after final notes and all supplied evidence pass the gate')
    args = parser.parse_args()
    summary = read(args.summary)
    pin = read(HERE/'pins.json')
    artifact = pin['bundle']
    check.require(summary.get('schema') == 'homeback136-manager-final-v1'
                  and summary.get('result') == 'PASS' and summary.get('physical_android_tested') is False
                  and summary.get('intended_release_scope') == 'diagnostic_only'
                  and summary.get('mpp') == artifact, 'Final diagnostic Manager summary differs')
    freeze = summary['final_freeze_recheck']
    check.require(freeze['result'] == 'PASS' and all(freeze.get(k) is True for k in
                  ('build_proof_unchanged','mpp_unchanged','all_output_apks_unchanged','verification_tools_unchanged')),
                  'Final evidence freeze check incomplete')
    qa_name = 'QA_HomeBack_v1.1.29.json'
    first = read(args.first_dist/qa_name)
    second = read(args.second_dist/qa_name)
    check.require(first == second and first['artifacts'][check.BUNDLE] == artifact
                  and identity(args.first_dist/qa_name) == summary['build_proof'], 'Clean build reports differ')
    check.require(all(report.get('diagnostic_only') is True and report.get('all_loader_bytes_preserved') is True
                      for report in (first, second)), 'Both builds must preserve the complete loader bytes')
    for name in (check.BUNDLE, check.SINGLE, qa_name):
        check.require(identity(args.first_dist/name) == identity(args.second_dist/name),
                      'Independent clean build bytes differ: '+name)
    check.require(identity(args.first_dist/check.BUNDLE) == artifact, 'Final MPP bytes changed')
    for name, digest in first['sources'].items():
        check.require(check.sha(check.safe(ROOT/'src', name)) == digest, 'Frozen build source changed: '+name)
    check.require(summary['frozen_source_files'] == freeze['source_files'] == len(first['sources']),
                  'Source file inventory count differs')
    for key, digest in (('original_apk',check.ORIGINAL_SHA),('host',check.HOST_SHA),('piko_mpp',check.PIKO_SHA)):
        check.require(summary['pinned_inputs'][key]['sha256'] == digest, 'Manager input identity differs: '+key)
    cases = summary['cases']
    check.require(len(cases) == 3 and {row['case'] for row in cases} == set(CASES),
                  'Exactly three completed application cases required')
    if args.finalize:
        for path in (ROOT/'README.md', HERE/'RELEASE_NOTES.txt'):
            check.require('DRAFT' not in path.read_text(), 'Final wording still pending: '+str(path))

    applications = []
    for row in cases:
        case, application = row['case'], row['application']
        folder = args.summary.parent/case
        check.require(row['result'] == 'PASS' and row['physical_android_tested'] is False
                      and row['mpp'] == artifact and row['input_output_artifact_source_hashes_checked_after_each_audit'] is True
                      and row['native_libraries_byte_identical'] is True, 'Incomplete application preservation proof')
        check.require(read(folder/'preservation-proof.json') == row
                      and read(folder/'application-proof.json') == application, 'Summary differs from actual case proof')
        count = 1 if case == 'hiro-only' else 65
        check.require(application['success'] is True and application['process_exit_code'] == 0
                      and application['physical_android_tested'] is False and application['selected_count'] == count
                      and application['patch_failures'] == [] and application['output'] == row['output']
                      and application['finished'] == [{'failure':None,'type':'finished'}], 'Manager apply did not succeed')
        check.require(identity(folder/'patched.apk') == row['output'], 'Final applied APK changed')
        check.require(set(row['audits']) == set(AUDITS), 'Required audit inventory differs')
        audits = []
        for kind, prefix in AUDITS.items():
            audit = row['audits'][kind]
            check.require(audit['result'] == 'PASS', 'Final audit failed: '+kind)
            passes = [line for line in audit['summary'] if line.startswith(prefix)]
            check.require(len(passes) == 1, 'Exact audit summary missing: '+kind)
            source = folder/(kind+'-audit.txt')
            check.require(passes[0] in source.read_text().splitlines(), 'Audit pass line differs')
            relative = copy(source, 'verification/'+case+'/'+source.name, audit['log'])
            audits.append({'path':relative,'pass_line':passes[0]})
        whole = next(a['pass_line'] for a in audits if a['pass_line'].startswith(AUDITS['whole-apk']))
        check.require('nativeInstagramHooks=12,' in whole and 'emittedRuntimeClasses=101,' in whole,
                      'Exact final native/runtime audit scope missing')
        events = copy(folder/'events.jsonl', 'verification/'+case+'/events.jsonl', application['events'])
        copy(folder/'application-proof.json', 'verification/'+case+'/application-proof.json')
        copy(folder/'preservation-proof.json', 'verification/'+case+'/preservation-proof.json')
        output = {
            'name':CASES[case], 'result':'PASS', 'physical_android_tested':False,
            'bundle_sha256':artifact['sha256'], 'manager_host_sha256':check.HOST_SHA,
            'input_apk_sha256':check.ORIGINAL_SHA, 'output_apk_sha256':row['output']['sha256'],
            'output_apk_bytes':row['output']['bytes'], 'selected_patch_count':count,
            'events':events, 'audits':audits, 'runtime_matches_final_mpp':True,
            'native_libraries_byte_identical':row['native_libraries_byte_identical'],
            'unchanged_zip_entries':row['unchanged_zip_entries'],
            'requested_source_bundle_order':application['requested_source_bundle_order'],
            'actual_patch_event_order':application['applied_patch_names'],
        }
        if case == 'hiro-only':
            check.require(row['input_baseline']['sha256'] == check.ORIGINAL_SHA, 'Original comparison baseline differs')
            output['whole_original_native_bodies_preserved'] = True
        else:
            check.require(row['input_baseline'] == {k:summary['pinned_inputs']['piko_baseline'][k]
                                                  for k in ('bytes','sha256')}, 'Piko-only comparison baseline differs')
            bundles = [item for item in application['selected_bundles'] if item['sha256'] == check.PIKO_SHA]
            check.require(len(bundles) == 1 and len(bundles[0]['selected_keys']) == 64, 'Official default 64 Piko selection missing')
            output.update({'piko_sha256':check.PIKO_SHA,'piko_version':'3.11.0-dev.3',
                           'piko_default_patch_count':64,'native_hooks_verified':True})
        applications.append(output)

    builds = [copy(folder/qa_name, 'verification/build-'+label+'.json')
              for label, folder in (('first',args.first_dist),('second',args.second_dist))]
    copy(args.summary, 'verification/manager-summary.json')
    tools = summary['verification_tools']
    sources = [tools['driver'],tools['pinned_input_manifest'],*tools['resource_audit_sources']]
    for item in [*sources,*tools['compiled_resource_audit_classes']]:
        source = args.workspace/item['path']
        expected = {k:item[k] for k in ('bytes','sha256')}
        check.require(identity(source) == expected, 'Verification tool identity changed')
        if item in sources:
            copy(source, 'verification/audit-sources/'+source.name, expected)
    write(ROOT/'verification/verification-tools.json', tools)
    pin['ready'] = args.finalize
    write(HERE/'pins.json', pin)
    report = {
        'schema':'homeback136-verified-build-v1','complete':args.finalize,'diagnostic_only':True,
        'all_loader_bytes_preserved':first['all_loader_bytes_preserved'],
        'physical_android_tested':False,'bundle_version':'1.0.320','oneback_version':'1.1.29',
        'artifact':artifact,'original_apk':{'package':'com.instagram.android','version':'452.1.0.53.64',
                                          'bytes':143237227,'sha256':check.ORIGINAL_SHA},
        'builds':builds,'applications':applications,'host_suite_count':36,
        'new_host_cases':20,'new_host_assertions':182,'source_files':len(first['sources']),
        'diagnostic_review':'verification/diagnostic/independent-review.json',
        'limitations':[*summary['limitations'],
                       'Diagnostic-only release; the reported handset route and repair are unconfirmed.',
                       'No physical Android validation of the Toast or reported first-versus-second behavior.'],
        'source_and_evidence_sha256':check.inventory(),
    }
    destination = ROOT/'verification/verified.json'
    write(destination, report)
    if args.finalize:
        try:
            check.verify(args.first_dist/check.BUNDLE, destination)
        except Exception:
            pin['ready'] = False
            write(HERE/'pins.json', pin)
            report['complete'] = False
            report['source_and_evidence_sha256'] = check.inventory()
            write(destination, report)
            raise
    print('PASS collected diagnostic evidence; ready='+str(args.finalize).lower())


if __name__ == '__main__':
    main()
