#!/usr/bin/env python3
"""Publish frozen Berry BFCache restore repair 1.4.2 and combined 334 after exact-byte verification."""
from pathlib import Path
import argparse
import json
import os
import subprocess
import time

from github_helpers import *

VERSION, BUNDLE, PREVIOUS = '1.4.2', '1.0.334', '1.0.333'
RELEASE_ROOT = 'releases/berry-repair334'
WORKFLOW = '.github/workflows/berry-repair334-publish.yml'
CONFIG = 'publication/manifest.json'
RAW_BRANCH = 'berry-repair334-downloads'
PAGE = f'https://github.com/{REPO}/releases/tag/{TAG}'
SINGLE = 'Berry_Browser_v1.4.2.mpp'
COMBINED = 'Hiro_Morphe_Patches_v1.0.334.mpp'
RECEIPT = 'publication_Berry_BFCache_v1.4.2.json'
BASE_URL = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/71ef6ebb1cb0be00d282cc95ab06b59829d223cf/downloads/Hiro_Morphe_Patches_v1.0.333.mpp'
BASE_SHA = '8305a6cdfd3d4bec658cc6c41e0e595e4b906a2e276631677a5a7a39ea414db2'
BUILD_QA = 'QA_build334.json'
APPLY_QA = 'QA_apply334.json'
RUNTIME_TESTS = {'jvm_test': {'scenarios': 41, 'assertions': 73},
                 'javascript_test': {'assertions': 19},
                 'native_test': {'scenarios': 5, 'assertions': 40},
                 'native_integration_test': {'scenarios': 13, 'assertions': 29}}


def source_files(repo):
    root = repo / RELEASE_ROOT
    require(root.is_dir(), 'Missing release source directory')
    result = {}
    for path in sorted(root.rglob('*')):
        if not path.is_file() or '__pycache__' in path.parts or path.suffix == '.pyc':
            continue
        relative = path.relative_to(root).as_posix()
        if relative in (CONFIG, RECEIPT):
            continue
        require(not path.is_symlink(), 'Source symlinks are not permitted')
        require(path.suffix.lower() not in ('.apk', '.apks', '.apkm', '.xapk', '.jks', '.p12', '.keystore'),
                'Private application/signing input in public source')
        result[path.relative_to(repo).as_posix()] = file_row(path)
    path = repo / WORKFLOW
    require(path.is_file(), 'Missing publication workflow')
    result[WORKFLOW] = file_row(path)
    return result


def config(repo):
    cfg = json.loads((repo / RELEASE_ROOT / CONFIG).read_text())
    require(cfg.get('schema') == 'berry-repair334-publication-v1' and cfg.get('ready') is True,
            'Publication manifest is not frozen')
    require(cfg['source_files'] == source_files(repo), 'Source changed after freeze')
    require(cfg['base_url'] == BASE_URL and cfg['base_sha256'] == BASE_SHA, 'Baseline pin differs')
    site = cfg.get('site_publication', {})
    require(site.get('status') == 'succeeded' and site.get('project_id') == 'appgprj_6a7d274188d0819185634a62fa2fc8a0'
            and site.get('source_commit') and site.get('version_id') and site.get('deployment_id'),
            'Verified fixed Site publication receipt is required before GitHub publication')
    require(site.get('single_sha256') == cfg.get('artifacts', {}).get(SINGLE, {}).get('sha256'),
            'Site single MPP must match the frozen release')
    return cfg


def verify_runtime_tests(runtime_qa):
    for name, counts in RUNTIME_TESTS.items():
        test = runtime_qa.get(name, {})
        require(test.get('result') == 'PASS' and all(test.get(k) == v for k, v in counts.items()),
                'Reviewed runtime test contract differs: ' + name)


def verify_native_metadata(root):
    audit = json.loads((root / 'qa/native_audit/metadata_qa.json').read_text())
    require(audit.get('result') == 'PASS' and audit.get('clean_apk_tested') == '3.83.74'
            and audit.get('synthetic_renamed_passed') is True
            and audit.get('actual_3_84_12_tested') is False
            and audit.get('physical_device_tested') is False
            and audit.get('new_negative_guard_cases') == 9
            and audit.get('existing_negative_guard_cases') == 4
            and audit.get('abi') == 'hiroCompleteBfcache(WebView,String)Z',
            'Native completion metadata and negative guard evidence differs')
    require(audit.get('source_sha256') == file_row(root / 'src/app/hiro/berrybfcache/patches/NativeCompletionStructure.java')['sha256'],
            'Native metadata evidence is not bound to the final provider source')
    tail = json.loads((root / 'qa/native_audit/started_arguments_qa.json').read_text())
    require(tail.get('schema') == 'berry-repair334-started-arguments-guard-v1'
            and tail.get('result') == 'PASS' and tail.get('negative_guard_cases') == 6
            and tail.get('local_writes_and_argument_reads_allowed') is True
            and tail.get('wide_register_overlap_rejected') is True
            and tail.get('physical_device_tested') is False,
            'Original started-argument preservation guard evidence differs')
    for key, path in [('provider_source_sha256', 'src/app/hiro/berrybfcache/patches/BfCacheRepairPatch.java'),
                      ('test_source_sha256', 'qa/native_audit/StartedArgumentsGuardTest.java'),
                      ('test_log_sha256', 'qa/native_audit/started_arguments_guards.log')]:
        require(tail.get(key) == file_row(root / path)['sha256'],
                'Started-argument guard evidence is not bound to the final source: ' + key)


def verify_application_runs(apply, root, expected):
    require(apply.get('hook_audit_verifier_sha256') == file_row(root / 'qa/VerifyPatchedHooks.java')['sha256']
            and apply.get('transformer_sha256') == file_row(root / 'qa/ReobfuscateFixture.java')['sha256'],
            'Application evidence verifier or synthetic transformer source differs')
    runs = apply.get('runs', [])
    pairs = [(run.get('artifact'), run.get('input_kind')) for run in runs]
    require(len(runs) == 4 and set(pairs) == {(name, kind) for name in (SINGLE, COMBINED)
                                           for kind in ('original', 'renamed')},
            'All four original/synthetic and standalone/combined applications are required')
    flags = ('register_bounds_invoke_words_and_branches_valid', 'queue_provider_and_manifest_present',
             'native_start_body_preserved_from_332', 'guarded_loading_and_tail_hooks_valid',
             'completion_boolean_and_native_metadata_valid', 'native_installer_registers_valid',
             'tail_webview_url_parameters_preserved', 'diagnostic_ui_absent')
    for run in runs:
        require(run.get('artifact_sha256') == expected[run['artifact']]['sha256']
                and run.get('success') is True and run.get('exit_code') == 0
                and run.get('failed_patches') == [] and run.get('all_build_steps_success') is True
                and run.get('physical_device_tested') is False
                and len(run.get('selected_patches', [])) == 5
                and set(run.get('applied_patches', [])) == set(run['selected_patches']),
                'Incomplete or mismatched patch application evidence')
        baseline = run.get('native_body_baseline_332_apk_sha256', '')
        require(len(baseline) == 64 and all(c in '0123456789abcdef' for c in baseline),
                'Exact 332 native-body comparison input hash is missing')
        audit = run.get('hook_audit', {})
        require(audit.get('result') == 'PASS_ACTUAL_HOOKS'
                and audit.get('expected_hook_kinds') == 27
                and all(audit.get(flag) is True for flag in flags),
                'Native loading/body/metadata/tail hooks or existing modifications were not verified')


def verify(repo, dist):
    cfg = config(repo)
    root = repo / RELEASE_ROOT
    expected = cfg['artifacts']
    require({SINGLE, COMBINED, BUILD_QA, APPLY_QA, 'RELEASE_NOTES.txt', 'SHA256SUMS.txt'} == set(expected),
            'Required release assets differ')
    for name, pin in expected.items():
        require(checked_path(name).name == name, 'Asset names must be filenames')
        require(file_row(dist / name) == pin, 'Reviewed artifact bytes differ: ' + name)
    wanted_sums = ''.join(expected[name]['sha256'] + '  ' + name + '\n'
                         for name in sorted(expected) if name != 'SHA256SUMS.txt')
    require((dist / 'SHA256SUMS.txt').read_text() == wanted_sums, 'Checksum inventory differs')
    build = json.loads((dist / BUILD_QA).read_text())
    require(build.get('schema') == 'berry-repair334-build-qa-v1'
            and build.get('result') == 'PASS' and build.get('physical_device_tested') is False
            and build.get('baseline_sha256') == BASE_SHA,
            'Build QA identity or device claim differs')
    require(build['artifacts'] == {name: expected[name] for name in (SINGLE, COMBINED)},
            'Build QA artifact identities differ')
    require(build.get('bundle_version') == BUNDLE and build.get('berry_version') == VERSION
            and build.get('source_files') == {p.relative_to(root / 'src').as_posix(): file_row(p)
                                            for p in sorted((root / 'src').rglob('*.java'))}
            and build.get('tool_sha256') == {name: row['sha256'] for name, row in cfg['tools'].items()},
            'Build QA version, provider source, or tool identity differs')
    require(build.get('all_unrelated_jvm_entries_preserved') is True
            and build.get('four_unrelated_extensions_identical') is True
            and build.get('dex_preservation', {}).get('result') == 'PASS_DEX_DEFINITION_PRESERVATION',
            'Unrelated provider and extension preservation proof differs')
    runtime_audit = build.get('runtime_dex_audit', {})
    require(runtime_audit.get('result') == 'PASS_RUNTIME_DEX'
            and runtime_audit.get('compile_only_stubs_absent') is True
            and runtime_audit.get('native_hook_abi_present') is True,
            'Runtime DEX and native bridge ABI proof differs')
    runtime_entry = 'extensions/berry_bfcache_restore.mpe'
    require(build.get('runtime_extensions', {}).get(runtime_entry) == cfg['runtime_extension']['sha256']
            and build.get('previous_bfcache_runtime_sha256') != cfg['runtime_extension']['sha256'],
            'Replacement runtime identity differs')
    runtime_report = repo / RELEASE_ROOT / 'payload/RUNTIME_BUILD_QA.json'
    require(file_row(runtime_report) == cfg['runtime_qa'], 'Reviewed runtime evidence differs')
    runtime_qa = json.loads(runtime_report.read_text())
    require(runtime_qa.get('result') == 'PASS'
            and runtime_qa.get('android_device_tested') is False
            and runtime_qa.get('renderer_behavior_tested') is False
            and runtime_qa.get('extension_sha256') == cfg['runtime_extension']['sha256'],
            'Runtime test identity or device claim differs')
    require(runtime_qa.get('compile_only_android_stubs_packaged') is False
            and runtime_qa.get('source_sha256') == {p.relative_to(root / 'runtime').as_posix(): file_row(p)['sha256']
                   for p in sorted((root / 'runtime/runtime_src').rglob('*.java'))},
            'Runtime source or compile-only stub exclusion evidence differs')
    verify_runtime_tests(runtime_qa)
    verify_native_metadata(root)
    require(build.get('compile_only_stub_excluded_from_outputs') is True,
            'Compile-only patcher ABI stub exclusion proof missing')
    all_versions = {'result': 'PASS', 'loaded_berry_patches': 5,
                    'all_versions_null': True, 'package_preserved': True}
    require(build.get('metadata') == {name: all_versions for name in (SINGLE, COMBINED)},
            'All five Berry patch package/version declarations must be verified')
    apply = json.loads((dist / APPLY_QA).read_text())
    require(apply.get('schema') == 'berry-repair334-apk-application-v1'
            and apply.get('result') == 'PASS'
            and apply.get('physical_device_tested') is False
            and apply.get('berry_3_84_12_tested') is False,
            'Application QA status or tested-version scope differs')
    require(apply.get('combined_sha256') == expected[COMBINED]['sha256']
            and apply.get('standalone_sha256') == expected[SINGLE]['sha256'],
            'Application QA is not bound to the reviewed artifacts')
    require(apply.get('actual_original_apk_application_passed') is True
            and apply.get('synthetic_renamed_apk_application_passed') is True,
            'Actual original and synthetic renamed application evidence required')
    verify_application_runs(apply, root, expected)
    for name, version in [(SINGLE, VERSION), (COMBINED, BUNDLE)]:
        entries = archive((dist / name).read_bytes())
        require(headers(entries['META-INF/MANIFEST.MF'])['Version'] == version, 'MPP version differs')
        require(runtime_entry in entries and sha(entries[runtime_entry]) == cfg['runtime_extension']['sha256'],
                'BFCache runtime missing or different from tested build')
    return cfg, expected, (dist / 'RELEASE_NOTES.txt').read_text()


def metadata(state, url, created, notes):
    old = state['manifest']
    require(old['version'] == PREVIOUS and old['download_url'] == BASE_URL, 'Active bundle advanced')
    # Update only the exact old Berry inventory row; preserve the entire release history.
    inventory = 'Berry Browser：v1.4.1（戻る復元時の読み込み停滞対策・構造自動検出）'
    require(old['description'].count(inventory) == 1, 'Expected Berry inventory changed')
    description = old['description'].replace(inventory,
        'Berry Browser：v1.4.2（同文書URL変更とネイティブ復帰判定を補正）', 1)
    feed = {**old, 'version': BUNDLE, 'created_at': created, 'download_url': url,
            'page_url': PAGE, 'signature_download_url': '', 'description': notes.strip() + '\n\n' + description}
    log = (f'# [{BUNDLE}]({PAGE}) (2026-11-03)\n\n'
           '* **Berry Browser:** 同じ文書のURL変更で復帰補正が止まる問題を修正し、対応WebViewのネイティブ復帰判定を追加。\n'
           '* **Berry Browser:** BFCache・オートページャー・既存4改造を維持。表示崩れへの効果と3.84.12実機動作は未検証。\n\n'
           + notes.strip() + '\n\n' + state['log'])
    require(has_changes_for(log, PREVIOUS, ['Berry Browser'])
            and not has_changes_for(log, BUNDLE, ['Berry Browser']), 'Berry update detection differs')
    scopes = {scope for entry in parsed_entries(state['log']) for scope, _ in entry['bullets']}
    require(not any(has_changes_for(log, PREVIOUS, [scope]) for scope in scopes
                    if scope.casefold() != 'berry browser' and not scope.casefold().startswith('berry browser - ')),
            'Unrelated application falsely marked updated')
    require(log.endswith(state['log']), 'Previous release history was lost')
    return feed, log


def publish(repo, dist, preflight_only=False):
    cfg, expected, notes = verify(repo, dist)
    states = {branch: snapshot(branch) for branch in ('main', 'dev')}
    source_commit = git(repo, 'rev-parse', 'HEAD')
    require(os.environ.get('GITHUB_ACTIONS') == 'true' and os.environ.get('GITHUB_SHA') == source_commit,
            'Publication must use the reviewed workflow checkout')
    require(states['main']['head'] == source_commit, 'Main advanced since the workflow started')
    require(git(repo, 'show', '-s', '--format=%P', source_commit) == cfg['reviewed_main'],
            'Source commit is not the reviewed direct child')
    require(states['dev']['head'] == cfg['reviewed_dev'], 'Dev advanced since review')
    changed = set(git(repo, 'diff-tree', '--no-commit-id', '--name-only', '-r', source_commit).splitlines())
    require(changed and all(n == WORKFLOW or n.startswith(RELEASE_ROOT + '/') for n in changed),
            'Source commit changes unrelated paths')
    require(states['main']['manifest'] == states['dev']['manifest'], 'Main/dev feeds diverged')
    require(all(s['manifest_bytes_sha256'] == cfg['reviewed_feed_sha256'] for s in states.values()),
            'Manager feed changed since review')
    require(states['main']['ulike_policy'] == states['dev']['ulike_policy']
            and sha(states['main']['ulike_policy']) == cfg['reviewed_policy_sha256'], 'ULike policy differs')
    created = utc_created(states)
    for state in states.values():
        metadata(state, 'https://example.invalid/preflight-only', created, notes)
    require(sha(fetch(BASE_URL)) == BASE_SHA, 'Public baseline bytes differ')
    for path in (f'releases/tags/{TAG}', f'git/ref/tags/{TAG}', f'git/ref/heads/{RAW_BRANCH}'):
        require(api(f'repos/{REPO}/' + path, absent=True) is None, 'Existing release/tag/branch; inspect first')
    require(not any(asset['name'] == COMBINED for release in api(f'repos/{REPO}/releases?per_page=100')
                    for asset in release.get('assets', [])), 'Combined version already used')
    check_heads(states)
    if preflight_only:
        print('PASS publication preflight, no writes'); return
    receipt_path = dist / RECEIPT
    require(not receipt_path.exists(), 'Prior receipt requires inspection')
    receipt = {'schema': 'berry-repair334-publication-receipt-v1', 'patch_version': VERSION,
               'bundle_version': BUNDLE, 'source_commit': source_commit, 'assets': expected,
               'physical_device_tested': False, 'berry_3_84_12_tested': False, 'main_dev_updated_atomically': False}
    def checkpoint(status, **fields):
        receipt.update(status=status, **fields)
        receipt_path.write_bytes(json_bytes(receipt))
        print(status, flush=True)
    checkpoint('preflight_passed')
    try:
        release = api(f'repos/{REPO}/releases', {'tag_name': TAG, 'target_commitish': source_commit,
            'name': 'Berry Browser 戻る復元補正 v1.4.2 / Hiro Morphe Patches v1.0.334',
            'body': notes, 'draft': True, 'prerelease': True, 'make_latest': 'false'})
        checkpoint('draft_created', release_id=release['id'])
        for name in expected:
            subprocess.run(['gh', 'release', 'upload', TAG, '--repo', REPO, str(dist / name)], check=True)
        verify_assets(release['id'], expected, False)
        checkpoint('draft_assets_verified'); check_heads(states)
        git(repo, 'fetch', '--no-tags', f'https://github.com/{REPO}.git', source_commit)
        raw = prepare_commit(repo, {**states['main'], 'branch': RAW_BRANCH},
            {'downloads/' + name: (dist / name).read_bytes() for name in (SINGLE, COMBINED)},
            'release: immutable Berry repair334 and combined334 downloads')
        git(repo, 'push', f'--force-with-lease=refs/heads/{RAW_BRANCH}:', f'https://github.com/{REPO}.git',
            f"{raw['head']}:refs/heads/{RAW_BRANCH}")
        require(head(RAW_BRANCH) == raw['head'], 'Raw distribution branch differs')
        urls = {name: f"https://raw.githubusercontent.com/{REPO}/{raw['head']}/downloads/{name}"
                for name in (SINGLE, COMBINED)}
        for name, url in urls.items():
            require(sha(fetch(url)) == expected[name]['sha256'], 'Immutable download bytes differ: ' + name)
        checkpoint('immutable_downloads_verified', raw_commit=raw['head'], download_urls=urls)
        api(f"repos/{REPO}/releases/{release['id']}", {'draft': False, 'prerelease': True, 'make_latest': 'false'}, 'PATCH')
        verify_assets(release['id'], expected, True)
        checkpoint('public_release_verified'); check_heads(states)
        updates, wanted = {}, {}
        for branch, state in states.items():
            feed, log = metadata(state, urls[COMBINED], created, notes)
            updates[branch] = {'patches-bundle.json': json_bytes(feed), 'CHANGELOG.md': log.encode()}
            wanted[branch] = feed, log
        active = commit_feeds_atomically(repo, states, updates, 'release: Berry repair334 and combined334 Manager feeds')
        for branch, state in active.items():
            feed, log = wanted[branch]
            require(json.loads(content('patches-bundle.json', state['head'])) == feed, 'Committed feed differs')
            require(content('CHANGELOG.md', state['head']).decode() == log, 'Committed history differs')
            require(content(POLICY_PATH, state['head']) == states[branch]['ulike_policy'], 'ULike policy changed')
            require(sha(fetch(feed['download_url'])) == expected[COMBINED]['sha256'], 'Manager download differs')
            for attempt in range(8):
                nonce = str(time.time_ns())
                prefix = f'https://raw.githubusercontent.com/{REPO}/{branch}'
                if (json.loads(fetch(prefix + '/patches-bundle.json?berryrepair334=' + nonce)) == feed
                        and fetch(prefix + '/CHANGELOG.md?berryrepair334=' + nonce).decode() == log):
                    break
                check_heads(active)
                require(attempt < 7, 'Public Manager feed propagation incomplete: ' + branch)
                time.sleep(2)
        check_heads(active)
        checkpoint('published_and_verified', published=True, release_url=PAGE,
                   main_dev_updated_atomically=True, ulike_policy_unchanged=True,
                   feed_commits={b: s['head'] for b, s in active.items()})
        raw_receipt = receipt_path.read_bytes()
        saved = commit_feeds_atomically(repo, active,
            {b: {RELEASE_ROOT + '/' + RECEIPT: raw_receipt} for b in active},
            'docs: verified Berry repair334 publication receipt')
        for branch, state in saved.items():
            require(content(RELEASE_ROOT + '/' + RECEIPT, state['head']) == raw_receipt, 'Receipt differs')
            require(content(POLICY_PATH, state['head']) == states[branch]['ulike_policy'], 'Policy changed saving receipt')
        subprocess.run(['gh', 'release', 'upload', TAG, '--repo', REPO, str(receipt_path)], check=True)
        verify_assets(release['id'], {**expected, RECEIPT: file_row(receipt_path)}, True)
        check_heads(saved)
    except BaseException as error:
        checkpoint('failed_requires_inspection', error=type(error).__name__ + ': ' + str(error))
        raise


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--repo', type=Path, required=True)
    p.add_argument('--dist', type=Path, required=True)
    p.add_argument('--local-only', action='store_true')
    p.add_argument('--preflight-only', action='store_true')
    a = p.parse_args()
    if a.local_only:
        verify(a.repo.resolve(), a.dist.resolve()); print('PASS frozen publication assets')
    else:
        publish(a.repo.resolve(), a.dist.resolve(), a.preflight_only)
