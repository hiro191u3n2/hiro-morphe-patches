"""Shared local/build/publication evidence gates for OneBack v1.1.11.

Historical 120 records remain historical. Fresh 121 ABI evidence is generated
from the four supplied APKs once locally and pinned to the replacement DEX.
CI repeats the baseline ABI and complete distributed native projection pipeline.
None of these gates executes Instagram on Android.
"""
from pathlib import Path
import base64
import hashlib
import json
import struct

ROOT = Path(__file__).resolve().parent.parent
BASE_SHA = 'c10dda2839b733b54fab850b96f670bbf7cdd6ba687b120184a3103151bc2624'
BASE_RUNTIME_SHA = 'd614e7ba8501af6ee65b3b237cf75034d3035a46c3556ef23365363b25955fc2'
OLD_RUNTIME_SHA = '112c362a1cbc3961801f48d8b1c2fba7e1a36339a78f9e7c468b150d342e4c49'
PROJECTION_SHA = '369882d88ab3c9f4799d9d96b73437bde63bd8c7e72e9a9779483dcd18ef7854'
PROJECTION_SENTINEL = {'descriptor': 'Lapp/hiro/oneback/runtime/AlreadyLoaded;',
                       'canonical_sha256': 'b35263953e7f1c0834f89fb7597890619581439bbb75d4bbbd596902b0568f0b',
                       'released_runtime_implementation': False,
                       'source': 'legacy DexAudit.fixtures synthetic namespace sentinel'}
CURRENT_SHA = 'd5291dcb5f3f5252dff05b98b4fefba121369d3a1c74635190cb8ed8047757a7'
SINGLE = 'OneBack_Exit_NoRecents_v1.1.11.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.250.mpp'
NATIVE_REPORTS = ('evidence/native-route121-review.json', 'evidence/native-activity121-audit.json',
                  'evidence/instagram-current121-contract.json')
NATIVE_TEXTS = ('evidence/native-route121-review.md', 'evidence/native-activity121-apk-contract.txt',
               'evidence/native-activity121-host.txt', 'evidence/instagram-current121-contract.md')
INPUTS = {
    'current_instagram': (CURRENT_SHA, 'com.instagram.android', '1.1.10', 63, 2155),
    'old_instagram_a': ('b2b4254d7fc2ffb4a6e08b41488e0def0dca8918f4159573a03887f7e2927670', 'com.instagram.android', '1.1.6', 53, 2105),
    'old_instagram_b': ('7207028231d71567ec7235f36800f7297bf8376054f4fc3d6f11ada5b9f020a8', 'com.instagram.android', '1.1.6', 53, 2105),
    'old_tiktok': ('243231abe5c1f201b7d306bc3b9c086c60b183de790005f2acd22ea4f3325f7c', 'com.zhiliaoapp.musically', '1.1.6', 53, 3929),
}


def require(ok, reason):
    if not ok:
        raise RuntimeError(reason)


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def source_digests(root=ROOT):
    selected = list((root / 'src').rglob('*.java')) + list((root / 'src').rglob('*.py'))
    selected += list((root / 'publication/audit').glob('*.java'))
    selected += [root / 'publication/release_checks.py', root / 'publication/prepare_evidence.py',
                 root / 'publication/native-projection.json', root / 'evidence/ProvidedApkHookAudit.java']
    selected += [root / name for name in NATIVE_REPORTS + NATIVE_TEXTS]
    return {p.relative_to(root).as_posix(): sha(p.read_bytes()) for p in sorted(selected)}


def historical_evidence(root=ROOT):
    return {p.relative_to(root).as_posix(): {'sha256': sha(p.read_bytes()), 'report': json.loads(p.read_text())}
            for p in sorted((root / 'evidence').rglob('*.json'))
            if p.relative_to(root).as_posix() not in NATIVE_REPORTS}


def native_contract_evidence(replacement_raw, root=ROOT):
    records = {name: {'sha256': sha((root / name).read_bytes()),
                      'report': json.loads((root / name).read_text())} for name in NATIVE_REPORTS}
    review = records[NATIVE_REPORTS[0]]['report']
    runtime_sources = {p.relative_to(root).as_posix(): sha(p.read_bytes())
                       for p in sorted((root / 'src/runtime').glob('*.java'))}
    manifest = ''.join(name + ' ' + digest + '\n' for name, digest in sorted(runtime_sources.items())).encode()
    require(review.get('schema') == 'instagram-back-independent-review-v1'
            and review.get('candidate') == 'homeback121' and review.get('native_input_sha256') == CURRENT_SHA
            and review.get('reviewed_source_sha256') == runtime_sources
            and review.get('reviewed_source_manifest_sha256') == sha(manifest)
            and review.get('compiled_runtime_reported_by_root') == {'sha256': sha(replacement_raw), 'bytes': len(replacement_raw)}
            and review.get('remaining_concrete_review_blockers') == []
            and review.get('physical_android_device_tested') is False
            and review.get('user_gesture_runtime_trace_available') is False,
            'Final native route review does not bind this frozen runtime or retains a blocker')
    layers = records[NATIVE_REPORTS[1]]['report']
    require(layers.get('schema') == 'instagram-native-activity-layer-audit-v121'
            and layers.get('input_apk_sha256') == CURRENT_SHA
            and layers.get('host_cases', 0) >= 34 and layers.get('host_assertions', 0) >= 60
            and layers.get('physical_android_tested') is False and layers.get('input_apk_modified') is False
            and layers.get('consuming_native_back_handlers_invoked_by_adapter') is False,
            'Native activity layer contract or its limits are missing')
    for name, digest in layers['sha256'].items():
        require(not Path(name).is_absolute() and '..' not in Path(name).parts
                and sha((root / name).read_bytes()) == digest, 'Native activity evidence binding changed: ' + name)
    current = records[NATIVE_REPORTS[2]]['report']
    require(current.get('schemaVersion') == 1 and current.get('apkSha256') == CURRENT_SHA
            and current.get('nativeMethods') and current.get('nativeFields')
            and current.get('verification', {}).get('suite') == 'Back121InstagramCurrentTest'
            and current['verification'].get('result') == 'PASS'
            and current['verification'].get('cases', 0) >= 23
            and current['verification'].get('assertions', 0) >= 78
            and current['verification'].get('deviceExecution') is False,
            'Current-container actual native contract or focused validation is missing')
    wanted = {'src/runtime/InstagramCurrent.java', 'src/test-runtime/InstagramCurrent121Fixture.java',
              'src/test-runtime/Back121InstagramCurrentTest.java'}
    bindings = {}
    for row in current['sourceBindings']:
        require('/homeback121/' in row['path'], 'Current contract source does not identify this release')
        relative = row['path'].split('/homeback121/', 1)[1]
        require(relative in wanted and relative not in bindings, 'Unexpected current contract source binding')
        raw = (root / relative).read_bytes()
        require(row['sha256'] == sha(raw) and row['bytes'] == len(raw), 'Current native contract source changed: ' + relative)
        bindings[relative] = row['sha256']
    require(set(bindings) == wanted, 'Current native contract source coverage is incomplete')
    return {'records': records, 'text_sha256': {name: sha((root / name).read_bytes()) for name in NATIVE_TEXTS}}


def baseline_abi(report, replacement_sha):
    require(report.get('schema') == 'oneback121-baseline-runtime-abi-v1'
            and report.get('baseline_version') == '1.1.10'
            and report.get('replacement_version') == '1.1.11'
            and report.get('baseline_runtime_sha256') == BASE_RUNTIME_SHA
            and report.get('replacement_runtime_sha256') == replacement_sha
            and report.get('baseline_classes') == 63
            and report.get('result') == 'PASS'
            and report.get('physical_android_tested') is False
            and report.get('removed_public_methods') == []
            and report.get('removed_public_fields') == []
            and report.get('changed_access_or_static') == []
            and report.get('preserved_public_methods', 0) > 0,
            'Fresh live-baseline249 public runtime ABI proof is missing or stale')


def projection_fixture(root=ROOT):
    fixture = json.loads((root / 'publication/native-projection.json').read_text())
    raw = base64.b64decode(fixture['dex_base64'], validate=True)
    require(fixture.get('schema') == 'oneback121-native-method-structural-projection-v1'
            and fixture.get('scope') == 'NON_EXECUTABLE_STRUCTURAL_PROJECTION_NOT_CLEAN_ORIGINAL_APK'
            and fixture.get('executable_instagram_apk') is False
            and fixture.get('clean_original_apk') is False
            and fixture.get('old_hooks_stripped') is False
            and fixture.get('source_apk_sha256') == INPUTS['old_instagram_a'][0]
            and fixture.get('class_count') == 27
            and fixture.get('synthetic_namespace_sentinel') == PROJECTION_SENTINEL
            and fixture.get('dex_bytes') == len(raw) == 27040
            and fixture.get('dex_sha256') == sha(raw) == PROJECTION_SHA,
            'Unreviewed native-method projection or incorrect executable/original claim')
    return fixture, raw


def fresh_evidence(replacement_raw, root=ROOT):
    folder = root / 'publication/evidence121'
    summary_raw = (folder / 'summary.json').read_bytes()
    summary = json.loads(summary_raw)
    replacement_sha = sha(replacement_raw)
    require(summary.get('schema') == 'oneback121-fresh-provided-input-evidence-v1'
            and summary.get('result') == 'PASS'
            and summary.get('patch_version') == '1.1.11'
            and summary.get('baseline_bundle_sha256') == BASE_SHA
            and summary.get('baseline_runtime_sha256') == BASE_RUNTIME_SHA
            and summary.get('replacement_runtime_sha256') == replacement_sha
            and summary.get('physical_android_tested') is False
            and summary.get('original_apk_apply_tested') is False
            and summary.get('input_apks_modified') is False
            and summary.get('inputs_already_patched') is True,
            'Fresh 121 input evidence was not run against this replacement runtime')
    require(summary.get('sources') == source_digests(root),
            'Release sources changed after fresh supplied-APK evidence; regenerate it')
    require(summary.get('native_contract_evidence') == native_contract_evidence(replacement_raw, root),
            'Fresh generation does not include the frozen native contract/review evidence')
    require(set(summary.get('inputs', {})) == set(INPUTS), 'All four supplied APKs are required')
    required_files = {'baseline249-abi.json', 'current-code-identity.json', 'current-runtime-class-hashes.json',
                      'current-external-callsites.json', 'current-native-verification.txt',
                      'current-entry-chain.txt', 'current-manifest.txt', 'scope-model-differential.json',
                      'scope-published120.txt', 'scope-replacement121.txt', 'dispatch-model-differential.json',
                      'dispatch-published120.txt', 'dispatch-replacement121.txt'}
    required_files |= {key + '-abi.json' for key in INPUTS}
    require(required_files.issubset(summary.get('files', {})), 'Fresh evidence ledger is incomplete')
    for name, digest in summary['files'].items():
        path = Path(name)
        require(path.name == name and path.suffix in ('.json', '.txt'), 'Invalid evidence path')
        require(sha((folder / name).read_bytes()) == digest, 'Fresh evidence file changed: ' + name)
    for key, (input_sha, package, reference_version, reference_count, callsites) in INPUTS.items():
        row = json.loads((folder / (key + '-abi.json')).read_text())
        require(summary['inputs'][key] == {'sha256': input_sha, 'report': key + '-abi.json'},
                'Supplied input identity changed: ' + key)
        reference_sha = BASE_RUNTIME_SHA if reference_version == '1.1.10' else OLD_RUNTIME_SHA
        require(row.get('schema') == 'oneback121-provided-apk-abi-v1'
                and row.get('input_sha256') == input_sha and row.get('package') == package
                and row.get('reference_version') == reference_version
                and row.get('reference_runtime_sha256') == reference_sha
                and row.get('existing_runtime_classes') == row.get('reference_runtime_classes') == reference_count
                and row.get('matches_reference_runtime') is True
                and row.get('old_runtime_canonical_class_mismatches') == []
                and row.get('old_runtime_classes_missing') == []
                and row.get('external_runtime_callsites') == callsites
                and row.get('duplicate_class_definitions') == []
                and row.get('replacement_version') == '1.1.11'
                and row.get('replacement_runtime_sha256') == replacement_sha
                and row.get('replacement_abi_valid_for_all_existing_hooks') is True
                and row.get('replacement_abi_errors') == []
                and row.get('old_public_methods_preserved', 0) > 0
                and row.get('result') == 'PASS'
                and row.get('physical_android_tested') is False
                and row.get('clean_original_apk_tested') is False
                and row.get('input_apk_modified') is False,
                'Fresh supplied-input identity and replacement ABI proof failed: ' + key)
    baseline = json.loads((folder / 'baseline249-abi.json').read_text())
    baseline_abi(baseline, replacement_sha)
    differential = json.loads((folder / 'scope-model-differential.json').read_text())
    require(differential.get('schema') == 'oneback121-scope-model-differential-v1'
            and differential.get('result') == 'PASS'
            and differential.get('baseline_runtime_sha256') == BASE_RUNTIME_SHA
            and differential.get('replacement_runtime_sha256') == replacement_sha
            and differential.get('production_runtime_d8_identity_verified') is True
            and differential.get('test_classpath_shadows_production_runtime') is False
            and differential.get('test_source_sha256') == sha((root / 'src/test-runtime/Back121InstagramScopeTest.java').read_bytes())
            and len(differential.get('published120_positive_counterexamples_failed', [])) == 6
            and len(differential.get('published120_safety_cases_passed', [])) == 3
            and differential.get('replacement121_cases') == 9
            and differential.get('replacement121_assertions') == 33
            and differential.get('physical_android_tested') is False
            and differential.get('live_screen_hierarchy_observed') is False,
            'Fresh frozen-code scope-model differential is incomplete or mislabeled')
    dispatch = json.loads((folder / 'dispatch-model-differential.json').read_text())
    require(dispatch.get('schema') == 'oneback121-dispatch-model-differential-v1'
            and dispatch.get('result') == 'PASS'
            and dispatch.get('baseline_runtime_sha256') == BASE_RUNTIME_SHA
            and dispatch.get('replacement_runtime_sha256') == replacement_sha
            and dispatch.get('production_runtime_d8_identity_verified') is True
            and dispatch.get('test_classpath_shadows_production_runtime') is False
            and dispatch.get('test_source_sha256') == sha((root / 'src/test-runtime/Back121InstagramDispatchTest.java').read_bytes())
            and len(dispatch.get('modeled_entry_and_registration_counterexamples', [])) == 7
            and len(dispatch.get('intentional_protected_forward_behavior_differences', [])) == 6
            and dispatch.get('replacement121_cases', 0) >= 39
            and dispatch.get('replacement121_assertions', 0) >= 204
            and dispatch.get('physical_android_tested') is False
            and dispatch.get('live_screen_hierarchy_observed') is False
            and dispatch.get('all_baseline_failures_are_user_bugs') is False,
            'Whole-dispatch model differential is incomplete or overstates observed device causes')
    current = json.loads((folder / 'current-code-identity.json').read_text())
    require(current.get('sha256') == CURRENT_SHA
            and current.get('class_count') == 200751
            and current.get('runtime_classes') == current.get('release_runtime_classes') == 63
            and current.get('runtime_classes_exactly_equal_release') == 63
            and current.get('release_missing_classes') == current.get('release_mismatched_classes') == []
            and current.get('extra_runtime_classes') == current.get('old_only_classes_not_equal_release') == []
            and current.get('duplicate_class_definitions') == current.get('actual_runtime_abi_errors') == []
            and current.get('external_runtime_callsites') == 2155
            and len(current.get('native_hook_sites', [])) == 2
            and len(current.get('decorated_registration_sites', [])) == 10
            and current.get('undecorated_public_registration_sites') == []
            and current.get('manifest', {}).get('package') == 'com.instagram.android'
            and current.get('input_modified') is False
            and current.get('android_execution_tested') is False,
            'Actual uploaded v1.1.10 canonical identity/native/bootstrap evidence is incomplete')
    hashes = json.loads((folder / 'current-runtime-class-hashes.json').read_text())
    require(len(hashes) == 63 and all(row['equals_released'] is True
            and row['current_sha256'] == row['released_v1110_sha256'] for row in hashes),
            'Current uploaded APK full-class identity ledger changed')
    native = (folder / 'current-native-verification.txt').read_text()
    require('PASS distributedExtensionClassesByteExact=63' in native
            and native.count('exactProvidedBodyRestored=true') == 2,
            'Actual uploaded APK native guards were not verified against exact old method bodies')
    projection_fixture(root)
    return {'summary_sha256': sha(summary_raw), 'summary': summary}


def pipeline_result(qa, replacement_raw, artifacts):
    proof = qa.get('distributed_native_projection', {})
    require(proof.get('schema') == 'oneback121-full-distributed-native-projection-v1'
            and proof.get('scope') == 'NON_EXECUTABLE_STRUCTURAL_PROJECTION_NOT_CLEAN_ORIGINAL_APK'
            and proof.get('input_dex_sha256') == PROJECTION_SHA
            and proof.get('synthetic_namespace_sentinel') == PROJECTION_SENTINEL
            and proof.get('physical_android_tested') is False
            and proof.get('original_apk_apply_tested') is False
            and proof.get('other_patches_coapplied') is False
            and proof.get('single_bundle_output_dex_identical') is True,
            'Full distributed native projection proof or its limits are missing')
    rows = proof.get('outputs', {})
    require(set(rows) == {SINGLE, BUNDLE}, 'Both distributed MPPs must pass the native projection')
    runtime_classes = struct.unpack_from('<I', replacement_raw, 96)[0]
    for name, row in rows.items():
        require(row.get('result') == 'PASS' and row.get('mpp_sha256') == artifacts[name]['sha256']
                and row.get('native_reels_hooks') == row.get('native_main_hooks') == 1
                and row.get('canonical_runtime_classes') == runtime_classes
                and row.get('provided_native_bodies_restored_exactly') == 2
                and row.get('synthetic_namespace_sentinel_preserved') is True
                and row.get('actual_distributed_loader') is True,
                'Native hook inclusion/canonical runtime proof failed for ' + name)
    require(rows[SINGLE]['emitted_dex_sha256'] == rows[BUNDLE]['emitted_dex_sha256'],
            'Single and bundle native projection results differ')
    baseline_abi(qa.get('fresh_baseline_runtime_abi', {}), sha(replacement_raw))
