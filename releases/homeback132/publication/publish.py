#!/usr/bin/env python3
"""Verify and publish the Instagram patch-time compatibility update; review modes never write remotely."""
from __future__ import annotations
import argparse, base64, datetime as dt, hashlib, importlib.util, io, json, os, re
import subprocess, tempfile, time, urllib.error, urllib.request, zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
SOURCE_ROOT = HERE.parent
spec = importlib.util.spec_from_file_location('homeback_build', SOURCE_ROOT/'src/build.py')
build = importlib.util.module_from_spec(spec); spec.loader.exec_module(build)
require, sha_file, json_bytes = build.require, build.sha, build.json_bytes
REPO = 'hiro191u3n2/hiro-morphe-patches'
VERSION, PREVIOUS, BUNDLE = build.VERSION, build.BASE_VERSION, build.BUNDLE_VERSION
SINGLE, COMBINED, QA_NAME = build.SINGLE, build.BUNDLE, build.QA
BASE_URL, BASE_SHA256 = build.BASE_URL, build.BASE_SHA
TAG, RAW_BRANCH = 'oneback-v1.1.25', 'release/homeback132-312'
RELEASE_ROOT = 'releases/homeback132'
PAGE = f'https://github.com/{REPO}/releases/tag/{TAG}'
REAL_QA_NAME = 'QA_HomeBack_v1.1.25_real_APK.json'
SOURCE_ZIP = 'HomeBack_v1.1.25_sources_and_QA.zip'
RECEIPT_NAME = 'publication_HomeBack_v1.1.25.json'
ASSETS = (SINGLE, COMBINED, QA_NAME, REAL_QA_NAME, SOURCE_ZIP, 'RELEASE_NOTES.txt', 'SHA256SUMS.txt')
POLICY_PATH = 'releases/ULike_ACTIVE.json'
INPUT = {'package':'com.instagram.android','version_name':'452.1.0.53.64','version_code':385811006,
    'bytes':143237227,'sha256':'22f20062d16997e26009e73b0425974efef8ac69c76dd8912ee8a091213c39e2'}
PATCH_NAME = '戻る1回で終了・履歴削除'
NAVIGATION = 'Customize navigation bar'
MANAGER_FAILURE = 'Instagram 452 external profile native body changed: LX/003Q;->A04'
MANAGER_CASES = ('old311-navigation','standalone-navigation',
                 'aggregate-default-baseline','aggregate-default')
MANAGER_COMMIT = '9fc6d7254c15181b3b6920a65d77dcadc73cb47e'
PATCHER_COMMIT = 'bd7bca01f438687c34ad4a5d9d8e6e6f20f0a2d1'
PIKO = {'version':'3.11.0-dev.3','bytes':8529170,
    'sha256':'a7658d042676d750ff68b782ff02e65d077c011d6a6f09031c05b39e80fcc7a3',
    'source_commit':'04987953674f98b3f302dc7a8da7492ae96b5267',
    'source_url':'https://github.com/crimera/piko/tree/04987953674f98b3f302dc7a8da7492ae96b5267',
    'artifact_url':'https://github.com/crimera/piko/releases/download/v3.11.0-dev.3/patches-3.11.0-dev.3.mpp'}
# Canonical, key-sorted rows returned by Manager237Selections for the pinned Piko MPP.
# This also binds all 68 declared keys, version compatibility and option defaults;
# only its 64 default rows are selected by the aggregate cases.
PIKO_SELECTION_SHA = '4b5a975c1cd2ff9123ed50507943ca755d56e040e6ed7eb2896f440ba64e4bb8'
MANAGER_LIMITATIONS = [
    "These explicit default selections do not reconstruct the user's unknown 68 patch selections/options.",
    'The host ran on Java 21/Linux; no physical Samsung/Android 17 execution was performed.',
    'The supplied ABI/locale/density are a host test configuration, not measured device properties.']

def sha(raw): return hashlib.sha256(raw).hexdigest()

def source_files(include_expected=False):
    result = {}
    for p in sorted(SOURCE_ROOT.rglob('*')):
        if not p.is_file() or '__pycache__' in p.parts or p.suffix in ('.pyc','.class'): continue
        name = p.relative_to(SOURCE_ROOT).as_posix()
        if name == 'publication/expected.json' and not include_expected: continue
        if name == RECEIPT_NAME or name in ASSETS: continue
        require(not p.is_symlink(), 'Source symlink not allowed: '+name)
        require(name.startswith(('src/','publication/')) or name in ('README.md','PROFILE_TAB_CONTRACT.md','LICENSE','NOTICE'), 'Unexpected source file '+name)
        build.checked_path(name)
        raw = p.read_bytes()
        if name.startswith('src/assets/upgrades/') and name.endswith('.dex'):
            require(re.fullmatch(r'src/assets/upgrades/oneback-runtime[0-9]+\.dex',name) is not None,
                    'Unexpected binary source asset '+name)
        else:
            raw.decode('utf-8'); require(b'\x00' not in raw, 'Binary source file '+name)
        result[name] = raw
    return result

def source_hashes(): return {name:sha(raw) for name,raw in source_files().items()}

def validate_qa(qa):
    for key,value in {'schema':'homeback132-build-v1','version':VERSION,'bundle_version':BUNDLE,
        'baseline_version':PREVIOUS,'baseline_bytes':build.BASE_BYTES,'baseline_sha256':BASE_SHA256,'target_package':INPUT['package'],
        'target_version':INPUT['version_name'],'physical_android_tested':False,
        'all_nonowned_loader_definitions_preserved':True,
        'all_nonowned_zip_entries_preserved_except_manifest_and_loader_dex':True}.items():
        require(qa.get(key) == value, 'Build proof mismatch: '+key)
    require(qa.get('toolchain_sha256') == build.TOOLS, 'Build tool identities differ')
    require(qa.get('sources') == build.source_ids(SOURCE_ROOT/'src'), 'Build proof is not for current source')
    require(qa.get('loader_preservation','').startswith('PASS Bundle132Audit loader:'), 'Missing loader preservation audit')
    require(qa.get('runtime_preservation') == 'PASS Bundle132Audit runtime: preserved=93; changed=[]; added=[]',
            'All published runtime definitions must be preserved')
    require(qa.get('runtime_sha256') == build.BASE_RUNTIME_SHA, 'Published runtime bytes changed')
    require(qa.get('dex_structure','').startswith('PASS selftest:'), 'Missing native wrapper DEX audit')
    host = qa.get('host',{})
    require(build.REQUIRED_HOST_TESTS <= set(host), 'Required native/host regressions absent')
    require(all(isinstance(v,str) and v for v in host.values()), 'Invalid host regression evidence')
    require(host.get('Back130InstagramProfileLinkTest','').startswith('PASS Back130InstagramProfileLinkTest'),
            'Existing profile-link regression missing')
    require(host.get('Back131InstagramProfileIdentityTest','').startswith('PASS Back131InstagramProfileIdentityTest'),
            'Registered-entry identity regression missing')
    require(host.get('Back131InstagramProfileRouteTest','').startswith('PASS Back131InstagramProfileRouteTest'),
            'Native external-profile handoff regression missing')
    require(set(qa.get('metadata',{})) == set(build.METADATA_SDKS), 'All four loader SDK reports required')
    require(all(v.startswith('PASS Metadata132Audit') for v in qa['metadata'].values()), 'Loader metadata audit failed')
    members = qa.get('mutable_members')
    require(isinstance(members,dict) and set(members) == set(build.METADATA_SDKS),
            'All four SDK snapshot-member serialization reports required')
    require(all(isinstance(value,str)
                and value.startswith('PASS Instagram132MutableContextAudit snapshot-members:')
                for value in members.values()),
            'Snapshot-member serialization regression failed or used a different audit mode')
    require(set(qa.get('artifacts',{})) == {SINGLE,COMBINED}, 'Wrong build artifacts')

def valid_digest(value):
    return isinstance(value,str) and re.fullmatch(r'[0-9a-f]{64}',value) is not None and value != '0'*64

def validate_manager_compatibility(proof, artifacts):
    require(isinstance(proof,dict) and proof.get('schema') == 'homeback132-manager-compat-v1'
            and proof.get('result') == 'PASS' and proof.get('complete') is True,
            'Complete actual-Manager compatibility proof required')
    require(proof.get('physical_android_tested') is False and proof.get('input') == INPUT,
            'Manager proof must use the original APK without a physical-device claim')
    require(proof.get('artifacts') == artifacts, 'Manager proof uses different final MPP bytes')
    manager = proof.get('manager',{})
    require(manager == {
        'version':'2.3.0-dev.7',
        'apk':{'bytes':41222610,'sha256':build.TOOLS['manager237.apk']},
        'host':{'bytes':19926006,'sha256':build.TOOLS['manager237-host.jar']},
        'source_commit':MANAGER_COMMIT,
        'source_url':'https://github.com/MorpheApp/morphe-manager/tree/'+MANAGER_COMMIT,
        'artifact_url':'https://github.com/MorpheApp/morphe-manager/releases/tag/v2.3.0-dev.7',
        'host_unmodified':True,'frameworks_from_original_apk':True,
        'entry_point':'app.morphe.manager.patcher.host.HostMainKt'}
        and manager.get('host_unmodified') is True and manager.get('frameworks_from_original_apk') is True,
        'Use the unchanged host/frameworks from the exact official Manager APK')
    patcher = proof.get('patcher',{})
    require(patcher == {'version':'2.1.0-dev.4','embedded_in_manager_host':True,
        'source_commit':PATCHER_COMMIT,
        'source_url':'https://github.com/MorpheApp/morphe-patcher/tree/'+PATCHER_COMMIT,
        'manager_dependency_url':'https://github.com/MorpheApp/morphe-manager/blob/'
            +MANAGER_COMMIT+'/gradle/libs.versions.toml'}
        and patcher.get('embedded_in_manager_host') is True, 'Reported Patcher identity differs')
    require(proof.get('piko') == PIKO, 'Official Piko version/artifact/source identity differs')
    require(proof.get('baseline_hiro') == {'version':PREVIOUS,'bytes':build.BASE_BYTES,'sha256':BASE_SHA256},
            'Expected rejection must use the exact published Hiro 1.0.311')
    require(proof.get('selection_helper_sha256') == sha_file(SOURCE_ROOT/'src/validation/Manager237Selections.java')
            and proof.get('validation_script_sha256') == sha_file(SOURCE_ROOT/'src/validate_manager.py'),
            'Manager proof is not from the frozen selection helper and harness')
    require(proof.get('device_config') == {'supportedAbis':['arm64-v8a'],'localeTags':['ja-JP'],'densityDpi':480}
            and proof.get('limitations') == MANAGER_LIMITATIONS,
            'Manager test configuration and limitations must remain explicit')
    manifests = proof.get('selection_manifests',{})
    require(isinstance(manifests,dict) and set(manifests) == {'piko','hiro311','standalone','aggregate'},
            'All four actual-Manager selection manifests required')
    identities = {'piko':{key:PIKO[key] for key in ('bytes','sha256')},
        'hiro311':{'bytes':build.BASE_BYTES,'sha256':BASE_SHA256},
        'standalone':artifacts[SINGLE],'aggregate':artifacts[COMBINED]}
    oneback_rows = [{'default':True,'key':PATCH_NAME,'name':PATCH_NAME,'option_defaults':{},
                     'target_versions':[None],'version_compatible':True}]
    for label,manifest in manifests.items():
        require(isinstance(manifest,dict)
                and manifest.get('schema') == 'manager237-selection-manifest-v1'
                and manifest.get('package') == INPUT['package']
                and manifest.get('version') == INPUT['version_name']
                and {key:manifest.get(key) for key in ('bytes','sha256')} == identities[label],
                'Manager selection manifest identity differs: '+label)
        name = manifest.get('artifact')
        require(isinstance(name,str) and name.endswith('.mpp')
                and '/' not in name and '\\' not in name and '\x00' not in name,
                'Manager selection artifact must be a file name')
        if label in ('standalone','aggregate'):
            require(name == (SINGLE if label == 'standalone' else COMBINED),
                    'Final Manager manifest has the wrong artifact name')
        rows = manifest.get('patches')
        require(isinstance(rows,list) and all(isinstance(row,dict) and isinstance(row.get('key'),str)
                                            for row in rows), 'Malformed Manager selection rows')
        require(len({row['key'] for row in rows}) == len(rows), 'Duplicate Manager selection key')
        if label == 'piko':
            canonical = json.dumps(sorted(rows,key=lambda row:row['key']),ensure_ascii=False,
                                   sort_keys=True,separators=(',',':')).encode('utf-8')
            require(len(rows) == 68 and sha(canonical) == PIKO_SELECTION_SHA,
                    'Actual Piko selection keys, defaults or version targets differ')
        else:
            require(rows == oneback_rows and rows[0].get('default') is True
                    and rows[0].get('version_compatible') is True,
                    'Only OneBack is Instagram-compatible in the Hiro bundle')
    piko_rows = manifests['piko']['patches']
    defaults = [row for row in piko_rows if row['default'] and row['version_compatible']]
    navigation = [row for row in piko_rows if row['name'] == NAVIGATION]
    require(len(defaults) == 64 and len(navigation) == 1, 'Piko default/navigation selection differs')

    def selection(label, rows):
        manifest = manifests[label]
        return {key:manifest[key] for key in ('artifact','bytes','sha256')} | {
            'selected_patch_names':[row['name'] for row in rows],
            'selected_patch_keys':[row['key'] for row in rows], 'option_overrides':{},
            'option_defaults':{row['key']:row['option_defaults'] for row in rows}}

    wanted = ([selection('piko',navigation),selection('hiro311',oneback_rows)],
              [selection('piko',navigation),selection('standalone',oneback_rows)],
              [selection('piko',defaults)],
              [selection('piko',defaults),selection('aggregate',oneback_rows)])
    cases = proof.get('cases')
    require(isinstance(cases,list) and all(isinstance(case,dict) for case in cases)
            and tuple(case.get('id') for case in cases) == MANAGER_CASES,
            'Full ordered Manager matrix is required; partial/duplicate cases are rejected')
    for index,case in enumerate(cases):
        expected_failure = index == 0
        require(case.get('result') == 'PASS' and case.get('expected_failure') is expected_failure
                and case.get('input_sha256') == INPUT['sha256']
                and type(case.get('process_exit_code')) is int and case['process_exit_code'] == 0,
                'Manager case failed or used a different input: '+case['id'])
        require(json.dumps(case.get('selected_bundles'),sort_keys=True,ensure_ascii=False)
                == json.dumps(wanted[index],sort_keys=True,ensure_ascii=False)
                and type(case.get('selected_count')) is int
                and case['selected_count'] == (2,2,64,65)[index],
                'Manager case changed its real keys, defaults, options or bundle order: '+case['id'])
        require(valid_digest(case.get('events_sha256')) and valid_digest(case.get('request_sha256'))
                and type(case.get('events_count')) is int
                and case['events_count'] >= case['selected_count']+1,
                'Manager request/event evidence missing: '+case['id'])
        require('finished_failure' in case, 'Manager finished event evidence missing')
        if expected_failure:
            failed,finished = case.get('failed_patches'),case['finished_failure']
            require(case.get('applied_patches') == [NAVIGATION]
                    and isinstance(failed,list) and len(failed) == 1
                    and isinstance(failed[0],dict) and failed[0].get('type') == 'patch_failed'
                    and failed[0].get('name') == PATCH_NAME
                    and MANAGER_FAILURE in failed[0].get('stackTrace','')
                    and isinstance(finished,dict) and finished.get('outOfMemory') is False
                    and finished.get('stackTrace') == failed[0].get('stackTrace')
                    and case.get('expected_failure_message') == MANAGER_FAILURE
                    and case.get('patches_done_counts') == []
                    and 'output_apk_sha256' not in case and 'output_apk_bytes' not in case,
                    'Old Hiro must reproduce the exact A04 rejection after Piko navigation succeeds')
        else:
            selected = [name for bundle in wanted[index] for name in bundle['selected_patch_names']]
            applied = case.get('applied_patches')
            require(case['finished_failure'] is None and case.get('failed_patches') == []
                    and case.get('patches_done_counts') == [len(selected)]
                    and isinstance(applied,list) and len(applied) == len(selected)
                    and set(applied) == set(selected) and 'expected_failure_message' not in case,
                    'Manager process exit 0 alone is insufficient; every selected patch must finish')
            require(valid_digest(case.get('output_apk_sha256'))
                    and type(case.get('output_apk_bytes')) is int and case['output_apk_bytes'] > 0,
                    'Manager successful case has no output APK identity')
    require(len({case['events_sha256'] for case in cases}) == 4
            and len({case['request_sha256'] for case in cases}) == 4,
            'Each Manager case requires its own request and event evidence')
    return {case['id']:case for case in cases}

def validate_manager_preservation(proof, cases):
    require(isinstance(proof,dict) and proof.get('result') == 'PASS'
            and proof.get('baseline_case') == 'aggregate-default-baseline'
            and proof.get('output_case') == 'aggregate-default'
            and proof.get('input_sha256') == cases['aggregate-default-baseline']['output_apk_sha256']
            and proof.get('output_sha256') == cases['aggregate-default']['output_apk_sha256'],
            'Preservation proof must compare the exact Piko64 baseline and final Piko64+OneBack outputs')
    require(proof.get('profile_link_audit','').startswith('PASS Instagram130ProfileLinkAudit output')
            and proof.get('existing_native_audit','').startswith('PASS whole fresh Instagram 452 APK semantics:')
            and proof.get('manifest_audit','').startswith('PASS InstagramManifestAudit preserve-baseline:'),
            'Independent Manager combination profile/native/manifest preservation audits required')
    resources = proof.get('resources',{})
    require(resources.get('result') == 'PASS' and resources.get('native_libraries_byte_identical') is True
            and type(resources.get('byte_identical_entries')) is int and resources['byte_identical_entries'] > 0,
            'Manager combination resources/native libraries must preserve the Piko baseline')
    table = resources.get('resource_table',{})
    require(table.get('result') == 'PASS'
            and table.get('audit','').startswith('PASS InstagramResourcesAudit preserve-baseline:')
            and table.get('baseline_semantics_identical') is True
            and table.get('resource_ids_and_values_preserved') is True
            and valid_digest(table.get('original_sha256'))
            and table.get('original_sha256') == table.get('baseline_sha256')
            and valid_digest(table.get('patched_sha256')),
            'Manager resource-table proof must preserve every Piko baseline ID, name and value')

def validate_real_apk(proof, artifacts):
    require(proof.get('schema') == 'homeback132-real-apk-v1', 'Wrong original-APK proof schema')
    require(proof.get('physical_android_tested') is False, 'Physical device confirmation is not available')
    require(proof.get('input') == INPUT, 'Original APK identity differs')
    require(proof.get('artifacts') == artifacts, 'Original-APK proof uses different MPP bytes')
    require(any(line.startswith('PASS Instagram130ProfileLinkAudit original')
                for line in proof.get('original_profile_contract','').splitlines()),
            'Original native profile contract was not verified')
    require(any(line.startswith('PASS Instagram132MutableContextAudit:')
                for line in proof.get('original_profile_contract','').splitlines()),
            'Equivalent mutable-context regression was not verified')
    manager_cases = validate_manager_compatibility(proof.get('manager_compatibility'),artifacts)
    validate_manager_preservation(proof.get('manager_preservation'),manager_cases)
    baseline = proof.get('baseline_resource_reference',{})
    require(baseline.get('artifact_sha256') == BASE_SHA256
            and baseline.get('input_sha256') == INPUT['sha256']
            and baseline.get('patcher_sdk_sha256') == build.TOOLS['morphe22.jar']
            and baseline.get('result') == 'PASS' and baseline.get('applied_patches') == [PATCH_NAME],
            'Resource comparison must use the published baseline on the same original APK/SDK')
    require(all(re.fullmatch(r'[0-9a-f]{64}',baseline.get(key,'')) is not None
                for key in ('output_apk_sha256','resource_table_sha256')), 'Missing baseline resource hashes')
    cases = proof.get('cases',[])
    require(isinstance(cases,list) and len(cases) == 2, 'Standalone and aggregate original-APK cases required')
    seen = set()
    for case in cases:
        name = case.get('artifact'); require(name in artifacts and name not in seen, 'Unknown/duplicate case artifact')
        require(case.get('artifact_sha256') == artifacts[name]['sha256'], 'Case MPP hash differs')
        require(case.get('input_sha256') == INPUT['sha256'], 'Case original APK hash differs')
        require(case.get('patcher_sdk') == 'morphe22.jar'
                and case.get('patcher_sdk_sha256') == build.TOOLS['morphe22.jar'], 'Case patcher identity differs')
        require(case.get('result') == 'PASS' and case.get('steps_all_success') is True
                and case.get('failed_patches') == [], 'Original-APK application failed')
        selected,applied = case.get('selected_patches'),case.get('applied_patches')
        require(selected == applied == [PATCH_NAME], 'Expected only the requested OneBack patch')
        require(re.fullmatch(r'[0-9a-f]{64}',case.get('output_apk_sha256','')) is not None, 'Missing output APK hash')
        require(isinstance(case.get('output_apk_bytes'),int) and case['output_apk_bytes'] > 0, 'Missing output APK size')
        require(case.get('profile_link_audit','').startswith('PASS Instagram130ProfileLinkAudit output'),
                'Missing emitted profile-link routing audit')
        require(case.get('existing_native_audit','').startswith('PASS whole fresh Instagram 452 APK semantics:'),
                'Missing existing native navigation preservation audit')
        require(case.get('manifest_audit','').startswith('PASS InstagramManifestAudit'), 'Missing manifest preservation audit')
        resources = case.get('resources',{})
        require(resources.get('result') == 'PASS' and resources.get('native_libraries_byte_identical') is True
                and isinstance(resources.get('byte_identical_entries'),int) and resources['byte_identical_entries'] > 0,
                'Missing resource/native-library preservation audit')
        table = resources.get('resource_table',{})
        require(table.get('result') == 'PASS'
                and table.get('audit','').startswith('PASS InstagramResourcesAudit')
                and table.get('baseline_semantics_identical') is True
                and table.get('resource_ids_and_values_preserved') is True
                and table.get('baseline_sha256') == baseline['resource_table_sha256']
                and table.get('original_sha256') == '594a62a70ef8701425cc464f37508514abb3bd2d38bbcffbb17f72f1cd723321'
                and re.fullmatch(r'[0-9a-f]{64}',table.get('patched_sha256','')) is not None,
                'Missing exact original/baseline/current resource-table semantic proof')
        seen.add(name)
    require(seen == {SINGLE,COMBINED}, 'Both exact MPPs must be applied to original APK')

def artifacts_at(dist):
    return {name:{'bytes':(dist/name).stat().st_size,'sha256':sha_file(dist/name)} for name in (SINGLE,COMBINED)}

def validate_build(dist, base):
    require(base.stat().st_size == build.BASE_BYTES and sha_file(base) == BASE_SHA256, 'Baseline bytes changed')
    qa_raw = (dist/QA_NAME).read_bytes(); qa = json.loads(qa_raw); validate_qa(qa)
    artifacts = artifacts_at(dist); require(qa['artifacts'] == artifacts, 'Build proof MPP hashes differ')
    original = build.archive(base); combined = build.archive(dist/COMBINED); single = build.archive(dist/SINGLE)
    build.validate_baseline(original)
    require(build.headers(original[build.MF])['Version'] == PREVIOUS, 'Baseline manifest differs')
    require(build.headers(combined[build.MF])['Version'] == BUNDLE, 'Aggregate manifest differs')
    require(build.headers(single[build.MF])['Version'] == VERSION, 'Standalone manifest differs')
    retained = {n:raw for n,raw in original.items() if n not in (build.MF,'classes.dex') and not build.owned_entry(n)}
    require(all(combined.get(n) == raw for n,raw in retained.items()),
            'Unrelated JVM patch, extension, resource or old notice changed')
    fresh_names = set(single)-{build.MF,'classes.dex'}
    require(set(combined) == {n for n in original if not build.owned_entry(n)}|fresh_names,
            'Aggregate contains unexpected entries or removed unrelated entries')
    require(build.PREFIX+'OneBackPatch.class' in fresh_names, 'Owned replacement loader missing')
    for name in fresh_names:
        upgrade = re.fullmatch(r'upgrades/oneback-runtime[0-9]+\.dex',name) is not None
        require(name.startswith((build.PREFIX,build.NOTICE_PREFIX)) or name == build.EXTENSION or upgrade,
                'Unexpected replacement/new entry '+name)
        if upgrade: require(original.get(name) == single[name], 'Existing upgrade asset differs')
        require(combined[name] == single[name], 'Standalone/aggregate owned bytes differ: '+name)
    require(combined[build.EXTENSION] == single[build.EXTENSION] == original[build.EXTENSION],
            'Published runtime extension must remain byte-identical')
    require(sha(combined[build.EXTENSION]) == qa['runtime_sha256'], 'Verified runtime hash differs')
    real_raw = (SOURCE_ROOT/'src/validation/real-apk.json').read_bytes()
    validate_real_apk(json.loads(real_raw), artifacts)
    return qa_raw, real_raw, artifacts

def expected_source_archive(qa_raw, real_raw):
    result = source_files(include_expected=True)
    result[QA_NAME] = qa_raw; result[REAL_QA_NAME] = real_raw
    return result

def validate_local(dist, base, expected):
    qa_raw, real_raw, artifacts = validate_build(dist,base)
    cfg = json.loads(expected.read_text())
    require(cfg.get('schema') == 'homeback132-frozen-v1' and cfg.get('two_local_builds_identical') is True,
            'Two clean local builds must be frozen')
    require(cfg.get('artifacts') == artifacts and cfg.get('qa_sha256') == sha(qa_raw)
            and cfg.get('real_apk_sha256') == sha(real_raw), 'Reviewed output identity differs')
    require(cfg.get('source_sha256') == source_hashes(), 'Sources changed after review/freeze')
    for key in ('reviewed_main_head','reviewed_dev_head'):
        require(re.fullmatch(r'[0-9a-f]{40}',cfg.get(key,'')) is not None, 'Missing reviewed remote head '+key)
    require((dist/REAL_QA_NAME).read_bytes() == real_raw, 'Packaged actual-APK proof differs')
    require(build.archive(dist/SOURCE_ZIP) == expected_source_archive(qa_raw,real_raw), 'Corresponding source archive differs')
    notes = (HERE/'RELEASE_NOTES.txt').read_bytes()
    require((dist/'RELEASE_NOTES.txt').read_bytes() == notes, 'Release notes changed')
    sums = ''.join(sha_file(dist/name)+'  '+name+'\n' for name in ASSETS if name != 'SHA256SUMS.txt')
    require((dist/'SHA256SUMS.txt').read_text() == sums, 'Checksum inventory differs')
    payloads = {name:(dist/name).read_bytes() for name in ASSETS}
    assets = {name:{'bytes':len(raw),'sha256':sha(raw)} for name,raw in payloads.items()}
    return cfg, payloads, notes.decode(), assets

def api(path, data=None, method=None, absent=False):
    cmd = ['gh','api',path]
    if method: cmd += ['--method',method]
    if data is not None: cmd += ['--input','-']
    r = subprocess.run(cmd,input=None if data is None else json.dumps(data),text=True,capture_output=True)
    if r.returncode:
        if absent and data is None and '(HTTP 404)' in r.stderr: return None
        raise RuntimeError('GitHub API failed: '+r.stderr.strip())
    return json.loads(r.stdout) if r.stdout.strip() else None

def fetch(url):
    for attempt in range(4):
        try:
            request = urllib.request.Request(url,headers={'User-Agent':'Hiro-HomeBack132-Publication','Cache-Control':'no-cache'})
            with urllib.request.urlopen(request,timeout=90) as r:
                require(r.status == 200, 'Public download must return HTTP200'); return r.read()
        except (OSError,urllib.error.URLError):
            if attempt == 3: raise
            time.sleep(2**attempt)

def head(branch): return api(f'repos/{REPO}/git/ref/heads/{branch}')['object']['sha']

def content(path, ref):
    build.checked_path(path)
    require(re.fullmatch(r'[0-9a-f]{40}',ref) is not None, 'Immutable commit required')
    row = api(f'repos/{REPO}/contents/{path}?ref={ref}')
    require(row.get('type') == 'file', 'Expected repository file')
    raw = base64.b64decode(row['content']) if row.get('encoding') == 'base64' else fetch(f'https://raw.githubusercontent.com/{REPO}/{ref}/{path}')
    require(len(raw) == row['size'] and hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest() == row['sha'], 'Git blob identity differs')
    return raw

def snapshot(branch):
    current = head(branch)
    return {'branch':branch,'head':current,'feed':json.loads(content('patches-bundle.json',current)),
            'log':content('CHANGELOG.md',current).decode(),'policy':content(POLICY_PATH,current)}

def check_heads(states):
    for state in states.values(): require(head(state['branch']) == state['head'], 'Concurrent update: '+state['branch'])

def git(repo,*args,input=None,env=None):
    cmd = ['git','-C',str(repo),'-c','credential.helper=','-c','credential.helper=!gh auth git-credential',*map(str,args)]
    r = subprocess.run(cmd,input=input,stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=env)
    require(r.returncode == 0, 'Git failed '+str(args)+'\n'+r.stderr.decode(errors='replace'))
    return r.stdout.decode().strip()

def prepare_commit(repo,state,files,message):
    env = {**os.environ,'GIT_AUTHOR_NAME':'github-actions[bot]','GIT_COMMITTER_NAME':'github-actions[bot]',
           'GIT_AUTHOR_EMAIL':'41898282+github-actions[bot]@users.noreply.github.com',
           'GIT_COMMITTER_EMAIL':'41898282+github-actions[bot]@users.noreply.github.com'}
    with tempfile.TemporaryDirectory(prefix='homeback132-index-') as tmp:
        env['GIT_INDEX_FILE'] = str(Path(tmp)/'index')
        git(repo,'read-tree',state['head'],env=env)
        for path,raw in files.items():
            build.checked_path(path)
            blob = git(repo,'hash-object','-w','--stdin',input=raw,env=env)
            git(repo,'update-index','--add','--cacheinfo',f'100644,{blob},{path}',env=env)
        tree = git(repo,'write-tree',env=env)
        commit = git(repo,'commit-tree',tree,'-p',state['head'],'-F','-',input=(message+'\n').encode(),env=env)
        require(git(repo,'show','-s','--format=%P',commit) == state['head'], 'Commit parent differs')
        changed = set(git(repo,'diff-tree','--no-commit-id','--name-only','-r',commit).splitlines())
        require(changed and changed.issubset(files), 'Unexpected changed repository paths')
        return {**state,'head':commit}

def commit_feeds(repo,states,updates,message):
    require(set(states) == set(updates) == {'main','dev'}, 'Both feed branches required')
    check_heads(states)
    git(repo,'fetch','--no-tags',f'https://github.com/{REPO}.git',states['main']['head'],states['dev']['head'])
    prepared = {branch:prepare_commit(repo,state,updates[branch],message) for branch,state in states.items()}
    check_heads(states)
    git(repo,'push','--atomic',f"--force-with-lease=refs/heads/main:{states['main']['head']}",
        f"--force-with-lease=refs/heads/dev:{states['dev']['head']}",f'https://github.com/{REPO}.git',
        f"{prepared['main']['head']}:refs/heads/main",f"{prepared['dev']['head']}:refs/heads/dev")
    check_heads(prepared); return prepared

def metadata(state,url,created,notes):
    old = state['feed']
    require(old['version'] == PREVIOUS and old['download_url'] == BASE_URL, 'Active feed advanced from reviewed baseline311')
    require(created > old['created_at'], 'Feed timestamp must increase')
    feed = {**old,'version':BUNDLE,'created_at':created,'download_url':url,'page_url':PAGE,
            'signature_download_url':'','description':notes.strip()+'\n\n'+old['description']}
    prefix = f'# {BUNDLE} ({created[:10]})\n\n* **Instagram:** Piko併用時に例外処理の表現差を変更と誤判定する適用時検査を修正（OneBack v{VERSION} / 総合 v{BUNDLE}）。\n\n'+notes.strip()+'\n\n'
    require(re.findall(r'^\* \*\*(.+?):\*\*',prefix,re.M) == ['Instagram'], 'Unexpected Manager app update scope')
    return feed,prefix+state['log']

def verify_assets(release_id,expected,public):
    release = api(f'repos/{REPO}/releases/{release_id}')
    require(release['tag_name'] == TAG and release['draft'] is (not public) and release['prerelease'] is True, 'Release identity/state differs')
    assets = {a['name']:a for a in release['assets']}
    require(set(assets) == set(expected) and len(assets) == len(release['assets']), 'Release asset inventory differs')
    for name,row in expected.items():
        raw = subprocess.check_output(['gh','api',f"repos/{REPO}/releases/assets/{assets[name]['id']}",'-H','Accept: application/octet-stream'])
        require(len(raw) == row['bytes'] and sha(raw) == row['sha256'], 'Release asset bytes differ: '+name)

def publish(args):
    cfg,payloads,notes,assets = validate_local(args.dist,args.base,args.expected)
    if args.local_only:
        print(json.dumps({'status':'local_verified_no_remote_writes','assets':assets},indent=2)); return
    require(os.environ.get('GITHUB_ACTIONS') == 'true', 'Publication requires verified GitHub Actions run')
    ci = json.loads((args.dist/'ci-pair.json').read_text())
    require(ci.get('run_id') == os.environ.get('GITHUB_RUN_ID') and ci.get('source_commit') == os.environ.get('GITHUB_SHA')
            and ci.get('artifacts') == cfg['artifacts'] and ci.get('qa_sha256') == cfg['qa_sha256'], 'Current CI pair proof missing')
    source_head = os.environ['GITHUB_SHA']
    require(git(args.repo,'rev-parse','HEAD') == source_head, 'Checkout differs from workflow source')
    require(git(args.repo,'show','-s','--format=%P',source_head) == cfg['reviewed_main_head'], 'Source commit must descend directly from reviewed main')
    require((args.repo/'.github/workflows/homeback132-publish.yml').read_bytes() == (HERE/'workflow.yml').read_bytes(), 'Workflow mirror differs')
    states = {branch:snapshot(branch) for branch in ('main','dev')}
    require(states['main']['head'] == source_head and states['dev']['head'] == cfg['reviewed_dev_head'], 'Reviewed main/dev moved')
    require(states['main']['feed'] == states['dev']['feed'], 'Manager feeds diverged')
    require(sha(fetch(BASE_URL)) == BASE_SHA256, 'Public baseline bytes changed')
    created = dt.datetime.now(dt.timezone.utc).replace(tzinfo=None,microsecond=0).isoformat()
    for state in states.values(): metadata(state,'https://example.invalid/preflight',created,notes)
    for path in (f'repos/{REPO}/releases/tags/{TAG}',f'repos/{REPO}/git/ref/tags/{TAG}',f'repos/{REPO}/git/ref/heads/{RAW_BRANCH}'):
        require(api(path,absent=True) is None, 'Release/tag/distribution branch exists; inspect before retry')
    for state in states.values():
        require(api(f"repos/{REPO}/contents/downloads/{COMBINED}?ref={state['head']}",absent=True) is None, 'Bundle filename already present')
    require(not any(a.get('name') == COMBINED for r in api(f'repos/{REPO}/releases?per_page=100') for a in r.get('assets',[])), 'Bundle version already published')
    if args.preflight_only:
        print(json.dumps({'status':'preflight_passed_no_remote_writes','heads':{k:v['head'] for k,v in states.items()}},indent=2)); return
    receipt_path = args.dist/RECEIPT_NAME
    require(not receipt_path.exists(), 'Prior receipt exists; inspect before retry')
    receipt = {'schema':'homeback132-publication-v1','version':VERSION,'bundle_version':BUNDLE,
               'source_commit':source_head,'assets':assets,'physical_android_tested':False,'original_apk_apply_tested':True}
    def checkpoint(status,**changes):
        receipt.update(status=status,**changes); receipt_path.write_bytes(json_bytes(receipt)); print(status,flush=True)
    checkpoint('preflight_passed')
    try:
        check_heads(states)
        release = api(f'repos/{REPO}/releases',{'tag_name':TAG,'target_commitish':source_head,
            'name':'OneBack Exit No Recents v1.1.25 / bundle 1.0.312','body':notes,'draft':True,'prerelease':True,'make_latest':'false'})
        checkpoint('draft_created',release_id=release['id'])
        for name in ASSETS: subprocess.run(['gh','release','upload',TAG,'--repo',REPO,str(args.dist/name)],check=True)
        verify_assets(release['id'],assets,False); checkpoint('draft_assets_verified')
        check_heads(states)
        raw_state = prepare_commit(args.repo,{**states['main'],'branch':RAW_BRANCH},
            {'downloads/'+name:payloads[name] for name in (SINGLE,COMBINED)},'release: immutable Instagram Back patch and bundle312 bytes')
        git(args.repo,'push',f'--force-with-lease=refs/heads/{RAW_BRANCH}:',f'https://github.com/{REPO}.git',f"{raw_state['head']}:refs/heads/{RAW_BRANCH}")
        urls = {name:f"https://raw.githubusercontent.com/{REPO}/{raw_state['head']}/downloads/{name}" for name in (SINGLE,COMBINED)}
        require(head(RAW_BRANCH) == raw_state['head'], 'Immutable distribution branch moved')
        for name,url in urls.items(): require(fetch(url) == payloads[name], 'Immutable distribution bytes differ')
        checkpoint('immutable_downloads_verified',raw_commit=raw_state['head'],download_urls=urls)
        check_heads(states)
        api(f"repos/{REPO}/releases/{release['id']}",{'draft':False,'prerelease':True,'make_latest':'false'},'PATCH')
        verify_assets(release['id'],assets,True); checkpoint('release_assets_verified')
        updates,expected_metadata = {},{}
        for branch,state in states.items():
            feed,log = metadata(state,urls[COMBINED],created,notes); expected_metadata[branch] = (feed,log)
            files = {'patches-bundle.json':json_bytes(feed),'CHANGELOG.md':log.encode()}
            for name,raw in source_files(include_expected=True).items(): files[RELEASE_ROOT+'/'+name] = raw
            for name in (QA_NAME,REAL_QA_NAME,'RELEASE_NOTES.txt','SHA256SUMS.txt'): files[RELEASE_ROOT+'/'+name] = payloads[name]
            updates[branch] = files
        active = commit_feeds(args.repo,states,updates,'release: Instagram profile Back handling and bundle312 Manager update')
        for branch,state in active.items():
            feed,log = expected_metadata[branch]
            require(json.loads(content('patches-bundle.json',state['head'])) == feed and content('CHANGELOG.md',state['head']).decode() == log, 'Remote feed/log differ')
            require(content(POLICY_PATH,state['head']) == states[branch]['policy'], 'Unrelated ULike policy changed')
            require(fetch(feed['download_url']) == payloads[COMBINED], 'Manager download bytes differ')
            for attempt in range(10):
                public = json.loads(fetch(f'https://raw.githubusercontent.com/{REPO}/{branch}/patches-bundle.json?verify={time.time_ns()}'))
                if public == feed: break
                check_heads(active); time.sleep(3)
            else: raise RuntimeError('Public feed propagation not confirmed: '+branch)
        check_heads(active); verify_assets(release['id'],assets,True)
        checkpoint('published_and_verified',release_url=PAGE,manager_main_dev_updated_atomically=True,
                   manager_app_scope='Instagram',main_commit=active['main']['head'],dev_commit=active['dev']['head'])
        saved = commit_feeds(args.repo,active,{b:{RELEASE_ROOT+'/'+RECEIPT_NAME:receipt_path.read_bytes()} for b in active},
            'docs: verified Instagram profile Back publication receipt')
        for state in saved.values(): require(content(RELEASE_ROOT+'/'+RECEIPT_NAME,state['head']) == receipt_path.read_bytes(), 'Receipt save differs')
        print('PASS published exact assets, immutable MPP downloads and both Manager feeds',flush=True)
    except BaseException as error:
        checkpoint('failed_requires_inspection',error=type(error).__name__+': '+str(error)); raise

def main():
    p = argparse.ArgumentParser(description=__doc__)
    for name in ('dist','base','repo'): p.add_argument('--'+name,type=Path,required=True)
    p.add_argument('--expected',type=Path,default=HERE/'expected.json')
    modes = p.add_mutually_exclusive_group(); modes.add_argument('--local-only',action='store_true'); modes.add_argument('--preflight-only',action='store_true')
    args = p.parse_args()
    for name in ('dist','base','repo','expected'): setattr(args,name,getattr(args,name).resolve())
    publish(args)

if __name__ == '__main__': main()
