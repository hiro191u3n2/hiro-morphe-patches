#!/usr/bin/env python3
"""Publish frozen Berry all-versions metadata 1.0.4 and combined 330 after exact-byte verification."""
from pathlib import Path
import argparse
import json
import os
import subprocess
import time

from github_helpers import *

VERSION, BUNDLE, PREVIOUS = '1.0.4', '1.0.330', '1.0.329'
RELEASE_ROOT = 'releases/berry-allversions330'
WORKFLOW = '.github/workflows/berry-allversions330-publish.yml'
CONFIG = 'publication/manifest.json'
RAW_BRANCH = 'berry-allversions330-downloads'
PAGE = f'https://github.com/{REPO}/releases/tag/{TAG}'
SINGLE = 'Berry_BFCache_Repair_v1.0.4.mpp'
COMBINED = 'Hiro_Morphe_Patches_v1.0.330.mpp'
RECEIPT = 'publication_Berry_allversions_v1.0.4.json'
BASE_URL = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/059ab59b9787f0002a85ce95d4c57f4d533d1329/downloads/Hiro_Morphe_Patches_v1.0.329.mpp'
BASE_SHA = '37401913eddad841bd9981e0374804b858ff36ae39fe9ff197aa6815e22e053b'
BASE_SINGLE_URL = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/059ab59b9787f0002a85ce95d4c57f4d533d1329/downloads/Berry_BFCache_Repair_v1.0.3.mpp'
BASE_SINGLE_SHA = 'ec31d178533b026302ff348c4e119b4242a3527c1f20c35f15ae8fbae82eae2c'
QA = 'QA_allversions330.json'


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
        require(path.suffix.lower() not in ('.apk', '.apks', '.apkm', '.jks', '.p12', '.keystore'),
                'Private application/signing input in public source')
        result[path.relative_to(repo).as_posix()] = file_row(path)
    path = repo / WORKFLOW
    require(path.is_file(), 'Missing publication workflow')
    result[WORKFLOW] = file_row(path)
    return result


def config(repo):
    cfg = json.loads((repo / RELEASE_ROOT / CONFIG).read_text())
    require(cfg.get('schema') == 'berry-allversions330-publication-v1' and cfg.get('ready') is True,
            'Publication manifest is not frozen')
    require(cfg['source_files'] == source_files(repo), 'Source changed after freeze')
    require(cfg['base_url'] == BASE_URL and cfg['base_sha256'] == BASE_SHA, 'Baseline pin differs')
    require(cfg['base_single_url'] == BASE_SINGLE_URL and cfg['base_single_sha256'] == BASE_SINGLE_SHA, 'Standalone baseline pin differs')
    return cfg


def verify(repo, dist):
    cfg = config(repo)
    expected = cfg['artifacts']
    require({SINGLE, COMBINED, QA, 'RELEASE_NOTES.txt', 'SHA256SUMS.txt'} == set(expected),
            'Required release assets differ')
    for name, pin in expected.items():
        require(checked_path(name).name == name, 'Asset names must be filenames')
        require(file_row(dist / name) == pin, 'Reviewed artifact bytes differ: ' + name)
    wanted_sums = ''.join(expected[name]['sha256'] + '  ' + name + '\n'
                         for name in sorted(expected) if name != 'SHA256SUMS.txt')
    require((dist / 'SHA256SUMS.txt').read_text() == wanted_sums, 'Checksum inventory differs')
    qa = json.loads((dist / QA).read_text())
    require(qa['result'] == 'PASS' and qa['physical_device_tested'] is False,
            'Candidate QA status or device claim differs')
    require(qa['combined_sha256'] == expected[COMBINED]['sha256']
            and qa['standalone_sha256'] == expected[SINGLE]['sha256']
            and qa['baseline_combined_sha256'] == BASE_SHA
            and qa['baseline_standalone_sha256'] == BASE_SINGLE_SHA,
            'Candidate QA artifact identity differs')
    require(qa['all_five_berry_versions_null'] is True
            and qa['runtime_guards_preserved'] is True
            and qa['extensions_unchanged'] is True
            and qa['unrelated_providers_preserved'] is True,
            'Metadata-only preservation verification failed')
    for name, version in [(SINGLE, VERSION), (COMBINED, BUNDLE)]:
        entries = archive((dist / name).read_bytes())
        require(headers(entries['META-INF/MANIFEST.MF'])['Version'] == version, 'MPP version differs')
        require('extensions/berry_bfcache_restore.mpe' in entries, 'BFCache runtime missing')
    return cfg, expected, (dist / 'RELEASE_NOTES.txt').read_text()


def metadata(state, url, created, notes):
    old = state['manifest']
    require(old['version'] == PREVIOUS and old['download_url'] == BASE_URL, 'Active bundle advanced')
    # Update only the exact old Berry inventory row; preserve the entire release history.
    inventory = 'Berry Browser：v1.3.1＋BFCache復帰補正v1.0.3（3.83.74／3.83.93.1）'
    require(old['description'].count(inventory) == 1, 'Expected Berry inventory changed')
    description = old['description'].replace(inventory,
        'Berry Browser：v1.3.1＋BFCache復帰補正v1.0.4（全バージョン表示）', 1)
    feed = {**old, 'version': BUNDLE, 'created_at': created, 'download_url': url,
            'page_url': PAGE, 'signature_download_url': '', 'description': notes.strip() + '\n\n' + description}
    log = (f'# [{BUNDLE}]({PAGE}) (2026-11-02)\n\n'
           '* **Berry Browser:** 5パッチの対応バージョン表示を「全バージョン」に統一。\n'
           '* **Berry Browser:** 表示用メタデータのみ変更。BFCache補正と既存機能・実行時の対象構造確認を維持。\n\n'
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
    require(sha(fetch(BASE_SINGLE_URL)) == BASE_SINGLE_SHA, 'Public standalone baseline bytes differ')
    for path in (f'releases/tags/{TAG}', f'git/ref/tags/{TAG}', f'git/ref/heads/{RAW_BRANCH}'):
        require(api(f'repos/{REPO}/' + path, absent=True) is None, 'Existing release/tag/branch; inspect first')
    require(not any(asset['name'] == COMBINED for release in api(f'repos/{REPO}/releases?per_page=100')
                    for asset in release.get('assets', [])), 'Combined version already used')
    check_heads(states)
    if preflight_only:
        print('PASS publication preflight, no writes'); return
    receipt_path = dist / RECEIPT
    require(not receipt_path.exists(), 'Prior receipt requires inspection')
    receipt = {'schema': 'berry-allversions330-publication-receipt-v1', 'patch_version': VERSION,
               'bundle_version': BUNDLE, 'source_commit': source_commit, 'assets': expected,
               'physical_device_tested': False, 'main_dev_updated_atomically': False}
    def checkpoint(status, **fields):
        receipt.update(status=status, **fields)
        receipt_path.write_bytes(json_bytes(receipt))
        print(status, flush=True)
    checkpoint('preflight_passed')
    try:
        release = api(f'repos/{REPO}/releases', {'tag_name': TAG, 'target_commitish': source_commit,
            'name': 'Berry Browser 全バージョン表示 v1.0.4 / Hiro Morphe Patches v1.0.330',
            'body': notes, 'draft': True, 'prerelease': True, 'make_latest': 'false'})
        checkpoint('draft_created', release_id=release['id'])
        for name in expected:
            subprocess.run(['gh', 'release', 'upload', TAG, '--repo', REPO, str(dist / name)], check=True)
        verify_assets(release['id'], expected, False)
        checkpoint('draft_assets_verified'); check_heads(states)
        git(repo, 'fetch', '--no-tags', f'https://github.com/{REPO}.git', source_commit)
        raw = prepare_commit(repo, {**states['main'], 'branch': RAW_BRANCH},
            {'downloads/' + name: (dist / name).read_bytes() for name in (SINGLE, COMBINED)},
            'release: immutable Berry all-versions330 and combined330 downloads')
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
        active = commit_feeds_atomically(repo, states, updates, 'release: Berry all-versions330 and combined330 Manager feeds')
        for branch, state in active.items():
            feed, log = wanted[branch]
            require(json.loads(content('patches-bundle.json', state['head'])) == feed, 'Committed feed differs')
            require(content('CHANGELOG.md', state['head']).decode() == log, 'Committed history differs')
            require(content(POLICY_PATH, state['head']) == states[branch]['ulike_policy'], 'ULike policy changed')
            require(sha(fetch(feed['download_url'])) == expected[COMBINED]['sha256'], 'Manager download differs')
            for attempt in range(8):
                nonce = str(time.time_ns())
                prefix = f'https://raw.githubusercontent.com/{REPO}/{branch}'
                if (json.loads(fetch(prefix + '/patches-bundle.json?berryallversions330=' + nonce)) == feed
                        and fetch(prefix + '/CHANGELOG.md?berryallversions330=' + nonce).decode() == log):
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
            'docs: verified Berry all-versions330 publication receipt')
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
