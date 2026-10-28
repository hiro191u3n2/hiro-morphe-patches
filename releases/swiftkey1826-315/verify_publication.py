#!/usr/bin/env python3
"""Verify the already active 1.8.26 release and attach its committed receipt only.

This does not rebuild, alter feed branches, overwrite assets, or rerun publication.
The inspected failed job had completed publication; its final receipt commit was
present on both branches before the subsequent branch-head check failed.
"""
from __future__ import annotations
import argparse
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import sys
sys.dont_write_bytecode = True

REPO = 'hiro191u3n2/hiro-morphe-patches'
RELEASE_ROOT = 'releases/swiftkey1826-315'
SOURCE_COMMIT = 'ab4bd25469164b7fc44549d3b5abbd2d76f2aaed'
MANIFEST_SHA = '1fde56e4b1ffe8f6d46d21bd6870ab8171f2278cd5a140faab5f1663850b126d'
RUN_ID, JOB_ID, RELEASE_ID = 40752712044, 123043362228, 427091613
RECEIPT_SHA = 'c476c0be1ac6bae1dd0007a4cd87205805a0816510e4c7bae2b4182778679db2'
RAW_COMMIT = '4ea7ad7eba95671ed54ac9cd9408d8b0aa320a0b'
RECEIPT_COMMITS = {'main': 'b3a9e0d84c7931dd0e802e01e2ab0d8f2c36249e',
                   'dev': 'd1feb60a73920dc997347ff1108eaec5133bca88'}
SUCCESS_STEPS = {
    5: 'Verify source pins and unchanged active baseline',
    6: 'Build first MPPs from pinned inputs and run regression tests',
    7: 'Independently rebuild and rerun regression tests',
    8: 'Match both builds to reviewed artifacts and preservation contract',
}
FAILURE_STEP = 'Publish verified MPPs and atomically advance both Manager feeds'

def require(ok, why):
    if not ok:
        raise RuntimeError(why)

def sha(raw):
    import hashlib
    return hashlib.sha256(raw).hexdigest()

def inspect_prior_run(helper):
    run = helper.api(f'repos/{REPO}/actions/runs/{RUN_ID}')
    require(run['id'] == RUN_ID and run['head_sha'] == SOURCE_COMMIT
            and run['head_branch'] == 'main' and run['event'] == 'push'
            and run['path'] == '.github/workflows/swiftkey1826-315-publish.yml'
            and run['run_attempt'] == 1 and run['status'] == 'completed'
            and run['conclusion'] == 'failure', 'Original run identity/state differs')
    job = helper.api(f'repos/{REPO}/actions/jobs/{JOB_ID}')
    require(job['id'] == JOB_ID and job['run_id'] == RUN_ID
            and job['head_sha'] == SOURCE_COMMIT and job['status'] == 'completed'
            and job['conclusion'] == 'failure', 'Original job identity/state differs')
    steps = {s['number']: s for s in job['steps']}
    for number, name in SUCCESS_STEPS.items():
        require(steps[number]['name'] == name and steps[number]['conclusion'] == 'success',
                'Original build/verification did not succeed: ' + name)
    require(steps[9]['name'] == FAILURE_STEP and steps[9]['conclusion'] == 'failure',
            'Original failed step differs')
    logs = subprocess.check_output(['gh', 'api', '--allow-escape-sequences', f'repos/{REPO}/actions/jobs/{JOB_ID}/logs']).decode('utf-8-sig').replace('\r\n', '\n')
    logs = re.sub(r'\x1b\[[0-?]*[ -/]*[@-~]', '', logs)
    logs = re.sub(r'(?m)^\d{4}-\d\d-\d\dT[0-9:.]+Z ', '', logs)
    published = logs.find('\npublished_and_verified\n')
    failed = logs.find('\nfailed_requires_inspection\n', published)
    trace = logs[failed:logs.find('##[error]Process completed with exit code 1.', failed)]
    require(published >= 0 and failed > published, 'Publication did not reach its verified checkpoint')
    require('saved = commit_feeds_atomically(helper, args.repo, active, receipt_updates,' in trace
            and 'helper.check_heads(prepared)' in trace
            and '\nRuntimeError: Concurrent update: main\n' in trace,
            'Failure is not the inspected post-receipt branch-head check')
    return {'run_id': RUN_ID, 'job_id': JOB_ID, 'source_commit': SOURCE_COMMIT,
            'build_and_validation_steps_successful': list(SUCCESS_STEPS.values()),
            'published_checkpoint_before_failure': True,
            'failure': 'Post-receipt commit branch-head check: Concurrent update: main',
            'original_run_conclusion': 'failure'}

def inspect_active(p, helper, expected, receipt_bytes):
    states = {branch: helper.snapshot(branch) for branch in ('main', 'dev')}
    helper.check_heads(states)
    url = f'https://raw.githubusercontent.com/{REPO}/{RAW_COMMIT}/downloads/{p.COMBINED}'
    require(states['main']['manifest'] == states['dev']['manifest'], 'Active feeds differ')
    for branch, state in states.items():
        feed = state['manifest']
        require(feed['version'] == p.BUNDLE and feed['download_url'] == url
                and feed['page_url'] == p.PAGE, 'Active publication changed: ' + branch)
        require(helper.content(RELEASE_ROOT + '/' + p.RECEIPT_NAME, state['head']) == receipt_bytes,
                'Current committed receipt differs: ' + branch)
        if branch == 'main':
            require(sha(helper.content(RELEASE_ROOT + '/manifest.json', state['head'])) == MANIFEST_SHA,
                    'Reviewed manifest changed on main')
        for app in p.APP_NAMES:
            require(p.has_changes_for(state['log'], p.PREVIOUS, [app]), 'Missing Manager app update scope')
            require(not p.has_changes_for(state['log'], p.BUNDLE, [app]), 'Fresh installation marked outdated')
        require(not any(p.has_changes_for(state['log'], p.PREVIOUS, [app]) for app in p.OTHER_APPS),
                'Unexpected other-app update scope')
        public_feed = json.loads(helper.fetch(f'https://raw.githubusercontent.com/{REPO}/{branch}/patches-bundle.json?verify1826={state["head"]}'))
        require(public_feed == feed, 'Public active feed differs: ' + branch)
    helper.check_heads(states)
    return states

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--repo', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args(); args.repo = args.repo.resolve(); args.output = args.output.resolve()
    require(os.environ.get('GITHUB_REPOSITORY') == REPO, 'Receipt upload requires the authorized Actions repository')
    source = args.repo / RELEASE_ROOT / 'src'; manifest = source.parent / 'manifest.json'
    require(sha(manifest.read_bytes()) == MANIFEST_SHA, 'Reviewed manifest pin differs')
    expected_json = json.loads(manifest.read_text())
    publisher_bytes = (source / 'publish1826.py').read_bytes()
    require(sha(publisher_bytes) == expected_json['source_files']['publish1826.py']['sha256'], 'Publisher source pin differs')
    spec = importlib.util.spec_from_file_location('swiftkey1826_publisher', source / 'publish1826.py')
    p = importlib.util.module_from_spec(spec); spec.loader.exec_module(p)
    expected = p.load_expected(manifest); source_count = p.verify_source_files(source, expected)
    helper = p.load_helper(args.repo)
    prior = inspect_prior_run(helper)
    receipt_path = RELEASE_ROOT + '/' + p.RECEIPT_NAME
    committed = {branch: helper.content(receipt_path, commit) for branch, commit in RECEIPT_COMMITS.items()}
    require(committed['main'] == committed['dev'], 'Original committed receipts differ')
    receipt_bytes = committed['main']
    require(sha(receipt_bytes) == RECEIPT_SHA, 'Exact committed receipt pin differs')
    receipt = json.loads(receipt_bytes)
    require(receipt['status'] == 'published_and_verified' and receipt['published'] is True
            and receipt['swiftkey_version'] == p.VERSION and receipt['bundle_version'] == p.BUNDLE
            and receipt['release_id'] == RELEASE_ID and receipt['raw_commit'] == RAW_COMMIT
            and receipt['release_url'] == p.PAGE and receipt['assets'] == expected['artifacts']
            and receipt['manager_main_dev_updated_atomically'] is True
            and receipt['local_validation']['two_independent_builds_byte_identical'] is True,
            'Committed receipt does not describe the reviewed publication')
    urls = {name: f'https://raw.githubusercontent.com/{REPO}/{RAW_COMMIT}/downloads/{name}'
            for name in (p.SINGLE, p.COMBINED)}
    require(receipt['download_urls'] == urls, 'Receipt immutable URLs differ')
    states = inspect_active(p, helper, expected, receipt_bytes)
    release = helper.api(f'repos/{REPO}/releases/{RELEASE_ID}')
    require(release['id'] == RELEASE_ID and release['tag_name'] == p.TAG
            and release['target_commitish'] == SOURCE_COMMIT
            and release['draft'] is False and release['prerelease'] is True,
            'Public release identity/state differs')
    tagged = helper.api(f'repos/{REPO}/releases/tags/{p.TAG}')
    require(tagged['id'] == RELEASE_ID, 'Tag identifies a different release')
    names = [a['name'] for a in release['assets']]
    require(len(names) == len(set(names)), 'Duplicate release assets')
    receipt_pin = {'bytes': len(receipt_bytes), 'sha256': sha(receipt_bytes)}
    all_expected = {**expected['artifacts'], p.RECEIPT_NAME: receipt_pin}
    already = p.RECEIPT_NAME in names
    require(set(names) == set(all_expected if already else expected['artifacts']), 'Unexpected release asset inventory')
    helper.verify_assets(RELEASE_ID, all_expected if already else expected['artifacts'], True)
    for name, url in urls.items():
        raw = helper.fetch(url); pin = expected['artifacts'][name]
        require(len(raw) == pin['bytes'] and sha(raw) == pin['sha256'], 'Immutable MPP bytes differ: ' + name)
    helper.check_heads(states)
    args.output.mkdir(parents=True, exist_ok=True)
    local_receipt = args.output / p.RECEIPT_NAME; local_receipt.write_bytes(receipt_bytes)
    if not already:
        # No --clobber: a concurrently appearing asset is never overwritten.
        helper.run(['gh', 'release', 'upload', p.TAG, '--repo', REPO, local_receipt])
    helper.verify_assets(RELEASE_ID, all_expected, True)
    final_states = inspect_active(p, helper, expected, receipt_bytes)
    require({b:s['head'] for b,s in final_states.items()} == {b:s['head'] for b,s in states.items()},
            'Feed branch changed during receipt verification')
    result = {'status': 'PUBLICATION_AND_RECEIPT_VERIFIED', 'swiftkey_version': p.VERSION,
        'bundle_version': p.BUNDLE, 'release_id': RELEASE_ID, 'release_url': p.PAGE,
        'source_manifest_sha256': MANIFEST_SHA, 'source_files_verified': source_count,
        'original_ci': prior, 'committed_receipt_commits': RECEIPT_COMMITS,
        'receipt_asset': {'name': p.RECEIPT_NAME, **receipt_pin, 'already_present': already},
        'public_assets_verified': all_expected, 'immutable_downloads_verified': urls,
        'active_heads': {b:s['head'] for b,s in final_states.items()},
        'main_dev_feed_bytes_unchanged': True, 'rebuild_performed': False,
        'existing_assets_overwritten': False, 'device_tested': False, 'original_apk_apply_tested': False}
    (args.output / 'verification_SwiftKey_v1.8.26.json').write_bytes(p.json_bytes(result))
    print(json.dumps(result, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    main()
