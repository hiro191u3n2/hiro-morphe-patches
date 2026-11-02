#!/usr/bin/env python3
"""Publish frozen Berry BFCache restore repair 1.4.1 and combined 332 after exact-byte verification."""
from pathlib import Path
import argparse
import json
import os
import subprocess
import time

from github_helpers import *

VERSION, BUNDLE, PREVIOUS = '1.4.1', '1.0.332', '1.0.331'
RELEASE_ROOT = 'releases/berry-bfcache332'
WORKFLOW = '.github/workflows/berry-bfcache332-publish.yml'
CONFIG = 'publication/manifest.json'
RAW_BRANCH = 'berry-bfcache332-downloads'
PAGE = f'https://github.com/{REPO}/releases/tag/{TAG}'
SINGLE = 'Berry_Browser_v1.4.1.mpp'
COMBINED = 'Hiro_Morphe_Patches_v1.0.332.mpp'
RECEIPT = 'publication_Berry_BFCache_v1.4.1.json'
BASE_URL = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/9c5c0e20eedfa032c647a39029319ae7012897e8/downloads/Hiro_Morphe_Patches_v1.0.331.mpp'
BASE_SHA = '9017b1a789afd37d78b2011050166a7d7c1101304ea2b4c5080f663d4ac3666a'
BUILD_QA = 'QA_build332.json'
APPLY_QA = 'QA_apply332.json'


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
    require(cfg.get('schema') == 'berry-bfcache332-publication-v1' and cfg.get('ready') is True,
            'Publication manifest is not frozen')
    require(cfg['source_files'] == source_files(repo), 'Source changed after freeze')
    require(cfg['base_url'] == BASE_URL and cfg['base_sha256'] == BASE_SHA, 'Baseline pin differs')
    return cfg


def verify(repo, dist):
    cfg = config(repo)
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
    require(build.get('schema') == 'berry-bfcache332-build-qa-v1'
            and build.get('result') == 'PASS' and build.get('physical_device_tested') is False
            and build.get('baseline_sha256') == BASE_SHA,
            'Build QA identity or device claim differs')
    require(build['artifacts'] == {name: expected[name] for name in (SINGLE, COMBINED)},
            'Build QA artifact identities differ')
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
    require(runtime_qa.get('jvm_test', {}).get('result') == 'PASS'
            and runtime_qa.get('javascript_test', {}).get('result') == 'PASS',
            'Runtime callback and document-script tests must pass')
    require(build.get('compile_only_stub_excluded_from_outputs') is True,
            'Compile-only patcher ABI stub exclusion proof missing')
    all_versions = {'result': 'PASS', 'loaded_berry_patches': 5,
                    'all_versions_null': True, 'package_preserved': True}
    require(build.get('metadata') == {name: all_versions for name in (SINGLE, COMBINED)},
            'All five Berry patch package/version declarations must be verified')
    apply = json.loads((dist / APPLY_QA).read_text())
    require(apply.get('schema') == 'berry-bfcache332-apk-application-v1'
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
    inventory = 'Berry Browser：v1.4.0（構造自動検出・全バージョン表示）'
    require(old['description'].count(inventory) == 1, 'Expected Berry inventory changed')
    description = old['description'].replace(inventory,
        'Berry Browser：v1.4.1（戻る復元時の読み込み停滞対策・構造自動検出）', 1)
    feed = {**old, 'version': BUNDLE, 'created_at': created, 'download_url': url,
            'page_url': PAGE, 'signature_download_url': '', 'description': notes.strip() + '\n\n' + description}
    log = (f'# [{BUNDLE}]({PAGE}) (2026-11-02)\n\n'
           '* **Berry Browser:** 戻る復元時に読み込み中のまま残る問題への補正処理を見直し。\n'
           '* **Berry Browser:** 元APKと名称を変更した検証入力で適用を確認。3.84.12と実機動作は未検証。\n\n'
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
    receipt = {'schema': 'berry-bfcache332-publication-receipt-v1', 'patch_version': VERSION,
               'bundle_version': BUNDLE, 'source_commit': source_commit, 'assets': expected,
               'physical_device_tested': False, 'berry_3_84_12_tested': False, 'main_dev_updated_atomically': False}
    def checkpoint(status, **fields):
        receipt.update(status=status, **fields)
        receipt_path.write_bytes(json_bytes(receipt))
        print(status, flush=True)
    checkpoint('preflight_passed')
    try:
        release = api(f'repos/{REPO}/releases', {'tag_name': TAG, 'target_commitish': source_commit,
            'name': 'Berry Browser 戻る復元補正 v1.4.1 / Hiro Morphe Patches v1.0.332',
            'body': notes, 'draft': True, 'prerelease': True, 'make_latest': 'false'})
        checkpoint('draft_created', release_id=release['id'])
        for name in expected:
            subprocess.run(['gh', 'release', 'upload', TAG, '--repo', REPO, str(dist / name)], check=True)
        verify_assets(release['id'], expected, False)
        checkpoint('draft_assets_verified'); check_heads(states)
        git(repo, 'fetch', '--no-tags', f'https://github.com/{REPO}.git', source_commit)
        raw = prepare_commit(repo, {**states['main'], 'branch': RAW_BRANCH},
            {'downloads/' + name: (dist / name).read_bytes() for name in (SINGLE, COMBINED)},
            'release: immutable Berry bfcache332 and combined331 downloads')
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
        active = commit_feeds_atomically(repo, states, updates, 'release: Berry bfcache332 and combined331 Manager feeds')
        for branch, state in active.items():
            feed, log = wanted[branch]
            require(json.loads(content('patches-bundle.json', state['head'])) == feed, 'Committed feed differs')
            require(content('CHANGELOG.md', state['head']).decode() == log, 'Committed history differs')
            require(content(POLICY_PATH, state['head']) == states[branch]['ulike_policy'], 'ULike policy changed')
            require(sha(fetch(feed['download_url'])) == expected[COMBINED]['sha256'], 'Manager download differs')
            for attempt in range(8):
                nonce = str(time.time_ns())
                prefix = f'https://raw.githubusercontent.com/{REPO}/{branch}'
                if (json.loads(fetch(prefix + '/patches-bundle.json?berrybfcache332=' + nonce)) == feed
                        and fetch(prefix + '/CHANGELOG.md?berrybfcache332=' + nonce).decode() == log):
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
            'docs: verified Berry bfcache332 publication receipt')
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
