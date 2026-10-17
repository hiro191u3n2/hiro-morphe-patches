#!/usr/bin/env python3
"""Generate fresh 121 static evidence from the frozen runtime and supplied APKs.

All original APKs are read in place. No input is repatched, signed, installed or
rewritten. The current uploaded APK is compared with baseline v1.1.10; the three
earlier APKs are compared with the independently pinned v1.1.6 runtime. The new
v1.1.11 runtime is checked against every input's existing external hook ABI.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import zipfile
import re

import release_checks as checks


def main():
    parser = argparse.ArgumentParser()
    for name in ('runtime', 'base', 'tools', 'legacy', 'work', 'java-home',
                 'current-instagram', 'old-instagram-a', 'old-instagram-b',
                 'old-tiktok', 'reference116'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--baseline-runtime-source', type=Path,
                        default=Path(__file__).resolve().parents[2] / 'homeback120/src/runtime')
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    checks.require(not args.work.exists(), 'Use a fresh evidence work directory')
    checks.require(checks.sha(args.base.read_bytes()) == checks.BASE_SHA, 'Expected exact live bundle249')
    checks.require(checks.sha(args.reference116.read_bytes()) == checks.OLD_RUNTIME_SHA, 'Expected exact v1.1.6 reference')
    supplied = {key: getattr(args, key) for key in checks.INPUTS}
    for key, path in supplied.items():
        checks.require(checks.sha(path.read_bytes()) == checks.INPUTS[key][0], 'Supplied input hash differs: ' + key)
    before_sources = checks.source_digests(root)
    runtime = args.runtime.read_bytes()
    checks.require(runtime.startswith(b'dex\n'), 'Replacement runtime must be D8 output')
    native_review = checks.native_contract_evidence(runtime, root)
    args.work.mkdir(parents=True)
    audit = args.work / 'classes'
    audit.mkdir()
    stage = args.work / 'evidence'
    stage.mkdir()
    (args.work / 'replacement121.dex').write_bytes(runtime)
    with zipfile.ZipFile(args.base) as bundle:
        baseline = bundle.read('extensions/oneback_exit.mpe')
    checks.require(checks.sha(baseline) == checks.BASE_RUNTIME_SHA, 'Live bundle249 runtime identity differs')
    (args.work / 'baseline120.dex').write_bytes(baseline)
    java, javac = args.java_home / 'bin/java', args.java_home / 'bin/javac'
    jdk = subprocess.check_output([str(java), '--version'], text=True)
    checks.require('Temurin-21.0.8+9' in jdk and '21.0.8' in jdk,
                   'Use pinned Temurin JDK 21.0.8+9.0.LTS')
    checks.require(subprocess.check_output([str(javac), '-version'], text=True).strip() == 'javac 21.0.8',
                   'Use pinned javac 21.0.8')
    cp = os.pathsep.join(map(str, [audit, args.base, args.tools / 'morphe.jar']))
    commands = []

    def run(argv, log, expected_exit=0):
        argv = list(map(str, argv))
        path = stage / log
        print('RUN ' + log, flush=True)
        with path.open('w') as output:
            result = subprocess.run(argv, stdout=output, stderr=subprocess.STDOUT,
                                    timeout=300, check=False)
        commands.append({'argv': argv, 'stdout_stderr': log, 'exit_code': result.returncode})
        checks.require(result.returncode == expected_exit,
                       'Evidence command failed: ' + log + '\n' + path.read_text()[-8000:])
        return path.read_text()

    sources = [args.legacy / 'test-dex/DexAudit.java', args.legacy / 'test-dex/Fixtures.java',
               root / 'src/test-dex/HomeBackAudit.java', root / 'evidence/ProvidedApkHookAudit.java',
               *sorted((root / 'publication/audit').glob('*.java'))]
    run([javac, '-encoding', 'UTF-8', '-cp', cp, '-d', audit, *sources], 'audit-javac.txt')

    # Recompile both production runtimes, and require their D8 bytes to equal the
    # supplied frozen new extension and the actual published baseline extension.
    # Thus the differential uses the same production code as those DEX files;
    # the model's Android surfaces still remain explicit host test stubs.
    models = args.work / 'models'
    models.mkdir()
    for version, source, expected in [('120', args.baseline_runtime_source, baseline),
                                      ('121', root / 'src/runtime', runtime)]:
        classes = models / ('runtime' + version)
        classes.mkdir()
        dex = models / ('dex' + version)
        dex.mkdir()
        run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', args.tools / 'android.jar',
             '-d', classes, *sorted(source.glob('*.java'))], 'model-runtime' + version + '-javac.txt')
        jar = models / ('runtime' + version + '.jar')
        with zipfile.ZipFile(jar, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
            for path in sorted(classes.rglob('*.class')):
                archive.writestr(path.relative_to(classes).as_posix(), path.read_bytes())
        run([java, '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '29',
             '--lib', args.tools / 'android.jar', '--classpath', args.tools / 'android.jar', '--output', dex, jar],
            'model-runtime' + version + '-d8.txt')
        checks.require((dex / 'classes.dex').read_bytes() == expected,
                       'Host differential runtime' + version + ' differs from the exact production extension')
    tests = models / 'tests'
    tests.mkdir()
    new_host_cp = os.pathsep.join(map(str, [tests, models / 'runtime121', args.tools / 'android.jar']))
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', new_host_cp, '-d', tests,
         *sorted((root / 'src/test-stubs').rglob('*.java')),
         *sorted((root / 'src/test-runtime').glob('*.java'))], 'model-host-javac.txt')
    for version in ('120', '121'):
        for path in (models / ('runtime' + version)).rglob('*.class'):
            checks.require(not (tests / path.relative_to(models / ('runtime' + version))).exists(),
                           'Test classpath shadows a production runtime definition')
    test_class = 'app.hiro.oneback.runtime.Back121InstagramScopeTest'
    old_host_cp = os.pathsep.join(map(str, [tests, models / 'runtime120', args.tools / 'android.jar']))
    old_model = run([java, '-ea', '-cp', old_host_cp, test_class], 'scope-published120.txt', expected_exit=1)
    new_model = run([java, '-ea', '-cp', new_host_cp, test_class], 'scope-replacement121.txt')
    expected_failures = ['footer_local_content_601_fragment', 'footer_local_content_601_main',
                         'footer_local_content_1801_fragment', 'footer_local_content_1801_main',
                         'caption_local_content_601', 'caption_local_content_1801']
    expected_safe = ['actual_comments_outside_local_reels_keep_precedence',
                     'real_footer_editor_is_not_the_static_prompt',
                     'unknown_oversized_current_content_stays_native']
    checks.require(re.findall(r'^FAIL ([^:]+):', old_model, re.M) == expected_failures
                   and re.findall(r'^PASS (.+)$', old_model, re.M) == expected_safe
                   and 'PASS Back121InstagramScopeTest: 9 cases, 33 assertions' in new_model,
                   'Published-vs-replacement scope counterexample or safety results changed')
    differential = {'schema': 'oneback121-scope-model-differential-v1', 'result': 'PASS',
                    'baseline_runtime_sha256': checks.BASE_RUNTIME_SHA,
                    'replacement_runtime_sha256': checks.sha(runtime),
                    'production_runtime_d8_identity_verified': True,
                    'test_classpath_shadows_production_runtime': False,
                    'test_source_sha256': checks.sha((root / 'src/test-runtime/Back121InstagramScopeTest.java').read_bytes()),
                    'test_class_sha256': checks.sha((tests / 'app/hiro/oneback/runtime/Back121InstagramScopeTest.class').read_bytes()),
                    'published120_positive_counterexamples_failed': expected_failures,
                    'published120_safety_cases_passed': expected_safe,
                    'replacement121_cases': 9, 'replacement121_assertions': 33,
                    'physical_android_tested': False, 'live_screen_hierarchy_observed': False,
                    'scope': 'Same compiled host model against production code whose D8 serialization equals released v1.1.10 and frozen v1.1.11. Six positive counterexamples improve and three safety cases remain protected; this is not a live device trace.'}
    (stage / 'scope-model-differential.json').write_text(json.dumps(differential, indent=2) + '\n')
    dispatch_class = 'app.hiro.oneback.runtime.Back121InstagramDispatchTest'
    old_dispatch = run([java, '-ea', '-cp', old_host_cp, dispatch_class],
                       'dispatch-published120.txt', expected_exit=1)
    new_dispatch = run([java, '-ea', '-cp', new_host_cp, dispatch_class], 'dispatch-replacement121.txt')
    old_failures = re.findall(r'^FAIL ([^:]+):', old_dispatch, re.M)
    old_passes = re.findall(r'^PASS (.+)$', old_dispatch, re.M)
    improved = ['direct_androidx_reels_retained_601', 'direct_androidx_reels_retained_1801',
                'direct_androidx_reels_retained_4001', 'direct_androidx_home_retained_1801',
                'direct_androidx_home_retained_4001', 'native_home_without_selected_accessibility_view',
                'owned_root_callback_stays_registered_for_native_reels']
    intentional = ['small_identified_layer_' + value for value in
                   ('comment_edit_text', 'comment_sheet', 'comments_sheet', 'share_sheet',
                    'bottom_sheet', 'design_bottom_sheet')]
    match = re.search(r'^PASS Back121InstagramDispatchTest: (\d+) cases, (\d+) assertions$', new_dispatch, re.M)
    checks.require(set(improved).issubset(old_failures) and set(intentional).issubset(old_failures)
                   and match is not None and int(match[1]) >= 39 and int(match[2]) >= 204,
                   'Whole-dispatch model counterexamples/new coverage differ from the reviewed cases')
    dispatch = {'schema': 'oneback121-dispatch-model-differential-v1', 'result': 'PASS',
                'baseline_runtime_sha256': checks.BASE_RUNTIME_SHA,
                'replacement_runtime_sha256': checks.sha(runtime),
                'production_runtime_d8_identity_verified': True,
                'test_classpath_shadows_production_runtime': False,
                'test_source_sha256': checks.sha((root / 'src/test-runtime/Back121InstagramDispatchTest.java').read_bytes()),
                'test_class_sha256': checks.sha((tests / 'app/hiro/oneback/runtime/Back121InstagramDispatchTest.class').read_bytes()),
                'baseline_cases_passed': old_passes, 'baseline_cases_failed': old_failures,
                'modeled_entry_and_registration_counterexamples': improved,
                'intentional_protected_forward_behavior_differences': intentional,
                'remaining_baseline_failures': [name for name in old_failures if name not in improved + intentional],
                'replacement121_cases': int(match[1]), 'replacement121_assertions': int(match[2]),
                'physical_android_tested': False, 'live_screen_hierarchy_observed': False,
                'all_baseline_failures_are_user_bugs': False,
                'scope': 'Same compiled whole-dispatch host fixture against D8-identified production runtimes. Only the named entry/registration cases are reported as model counterexamples. Small-layer forwarding differences are intentional; remaining differences, including a null old callback caused by unrecognized Home, are not assigned as device bugs.'}
    (stage / 'dispatch-model-differential.json').write_text(json.dumps(dispatch, indent=2) + '\n')
    run([java, '-Xmx1800m', '-ea', '-cp', cp, 'BaselineRuntimeAbiAudit121',
         args.work / 'baseline120.dex', args.work / 'replacement121.dex', stage / 'baseline249-abi.json'],
        'baseline249-abi.txt')
    for key, path in supplied.items():
        version = checks.INPUTS[key][2]
        reference = args.work / 'baseline120.dex' if version == '1.1.10' else args.reference116
        run([java, '-Xmx2200m', '-ea', '-cp', cp, 'ProvidedApkAbiAudit121', path,
             reference, version, args.work / 'replacement121.dex', stage / (key + '-abi.json')],
            key + '-abi.txt')
    current_dir = args.work / 'current-identity'
    current_dir.mkdir()
    run([java, '-Xmx1800m', '-ea', '-cp', cp, 'UploadIdentityAudit', args.current_instagram,
         args.base, args.old_instagram_a, current_dir], 'current-code-identity.txt')
    for original, final in [('identity-and-hooks.json', 'current-code-identity.json'),
                            ('runtime-class-hashes.json', 'current-runtime-class-hashes.json'),
                            ('all-external-runtime-callsites.json', 'current-external-callsites.json')]:
        shutil.copyfile(current_dir / original, stage / final)
    # The reference in this classpath is the actually distributed baseline249
    # MPP. This command inspects the existing guards; it does not insert again.
    run([java, '-Xmx2200m', '-ea', '-cp', cp, 'PipelineProbe', 'verify',
         args.old_instagram_a, args.current_instagram], 'current-native-verification.txt')
    run([java, '-Xmx1800m', '-ea', '-cp', cp, 'MainHierarchyAudit', args.current_instagram],
        'current-entry-chain.txt')
    run([java, '-ea', '-cp', cp, 'PipelineProbe', 'manifest', args.current_instagram],
        'current-manifest.txt')
    checks.require(args.runtime.read_bytes() == runtime, 'Replacement runtime changed during audit')
    checks.require(before_sources == checks.source_digests(root), 'Sources changed during audit')
    for key, path in supplied.items():
        checks.require(checks.sha(path.read_bytes()) == checks.INPUTS[key][0], 'Input APK changed during audit: ' + key)
    summary = {
        'schema': 'oneback121-fresh-provided-input-evidence-v1', 'patch_version': '1.1.11', 'result': 'PASS',
        'baseline_bundle_sha256': checks.BASE_SHA, 'baseline_runtime_sha256': checks.BASE_RUNTIME_SHA,
        'replacement_runtime_sha256': checks.sha(runtime), 'physical_android_tested': False,
        'original_apk_apply_tested': False, 'input_apks_modified': False, 'inputs_already_patched': True,
        'historical_120_records_reused_as_fresh_runs': False,
        'scope': 'Read-only full DEX canonical identity, every existing external runtime hook ABI, actual current native guards, manifest and inherited input/bootstrap routes. This is neither a clean-original apply nor device execution. Runtime ABI compatibility does not assert in-place runtime replacement.',
        'jdk': jdk.strip(), 'sources': before_sources,
        'native_contract_evidence': native_review,
        'inputs': {key: {'sha256': checks.INPUTS[key][0], 'report': key + '-abi.json'} for key in checks.INPUTS},
        'files': {p.name: checks.sha(p.read_bytes()) for p in sorted(stage.iterdir()) if p.is_file()},
        'commands': commands,
    }
    destination = root / 'publication/evidence121'
    destination.mkdir(exist_ok=True)
    for path in stage.iterdir():
        shutil.copyfile(path, destination / path.name)
    (destination / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n')
    checks.fresh_evidence(runtime, root)
    print('PASS fresh 121 evidence: current + three older supplied APKs, baseline249 ABI, actual current native guards; no APK mutation or Android execution', flush=True)


if __name__ == '__main__':
    main()
