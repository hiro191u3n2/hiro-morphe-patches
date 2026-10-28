#!/usr/bin/env python3
"""Resume the inspected partial publication, preserving all six published assets.

Only the exact failed run and the exact still-inactive baseline are accepted.
Large MPPs travel as ordinary Git objects, never JSON/base64 GitHub API blobs.
"""
from __future__ import annotations
import argparse
import importlib.util
import json
import os
from pathlib import Path
import sys
import zipfile
sys.dont_write_bytecode = True

ROOT = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('reviewed_publish1825', ROOT / 'src/publish1825.py')
p = importlib.util.module_from_spec(spec)
spec.loader.exec_module(p)
SOURCE_COMMIT = 'e708aa8f72320c2753b8b3a0bfc4ea364ae5cd76'
DEV_COMMIT = 'ad3d3d7397b8b6a9d948429dba7f0746f9375fe3'
MANIFEST_SHA256 = '2e8f8acc917d1de3f1e283b995122f7a333eae5a97527a010f8f3a7ac89dd06e'
RUN_ID, JOB_ID, ARTIFACT_ID, RELEASE_ID = 40723427107, 122940194872, 12831338695, 426903702
ARTIFACT_NAME = 'swiftkey1825-bundle314-verified-publication'
ARTIFACT_SHA256 = '9677569eeca7bdacd479de484549faeb7097d2014422bdd165880063bdf99cb1'
ARTIFACT_BYTES = 185359788
RECOVERY_PATHS = {
    'releases/swiftkey1825-314/resume_publication.py',
    'releases/swiftkey1825-314/resume_tests.py',
    '.github/workflows/swiftkey1825-314-resume-publish.yml',
}
SUCCESS_STEPS = {
    5: 'Verify source pins and unchanged active baseline',
    6: 'Build first MPPs from pinned inputs and run regression tests',
    7: 'Independently rebuild and rerun regression tests',
    8: 'Match both builds to reviewed artifacts and preservation contract',
}


def validate_prior_run(run, job, artifact):
    p.require(run['id'] == RUN_ID and run['head_sha'] == SOURCE_COMMIT
              and run['head_branch'] == 'main' and run['event'] == 'push'
              and run['status'] == 'completed' and run['conclusion'] == 'failure',
              'Original run is not the inspected partial publication')
    p.require(job['id'] == JOB_ID and job['run_id'] == RUN_ID and job['head_sha'] == SOURCE_COMMIT
              and job['conclusion'] == 'failure', 'Original job identity changed')
    steps = {row['number']: row for row in job['steps']}
    for number, name in SUCCESS_STEPS.items():
        p.require(steps[number]['name'] == name and steps[number]['conclusion'] == 'success',
                  'Original build/source validation did not succeed: ' + name)
    p.require(steps[9]['name'] == 'Publish verified MPPs and atomically advance both Manager feeds'
              and steps[9]['conclusion'] == 'failure', 'Original failure step differs')
    p.require(artifact['id'] == ARTIFACT_ID and artifact['name'] == ARTIFACT_NAME
              and artifact['expired'] is False and artifact['size_in_bytes'] == ARTIFACT_BYTES
              and artifact['digest'] == 'sha256:' + ARTIFACT_SHA256
              and artifact['workflow_run']['id'] == RUN_ID
              and artifact['workflow_run']['head_sha'] == SOURCE_COMMIT,
              'Original two-build artifact identity changed')


def validate_release(release):
    p.require(release['id'] == RELEASE_ID and release['tag_name'] == p.TAG
              and release['target_commitish'] == SOURCE_COMMIT
              and release['draft'] is False and release['prerelease'] is True,
              'Existing release identity or publication state changed')


def validate_states(states, raw_state, checkout):
    p.require(set(states) == {'main', 'dev'}, 'Both feed branches required')
    p.require(states['main']['head'] == checkout and states['dev']['head'] == DEV_COMMIT,
              'Main or dev moved since the inspected baseline')
    p.require(states['main']['manifest'] == states['dev']['manifest'], 'Main/dev feeds differ')
    p.require(all(s['manifest']['version'] == p.PREVIOUS
                  and s['manifest']['download_url'] == p.BASE_URL for s in states.values()),
              'Active feed advanced or baseline URL changed')
    p.require(raw_state['branch'] == p.RAW_BRANCH and raw_state['head'] == SOURCE_COMMIT,
              'Distribution branch advanced; do not overwrite it')


def commit_raw(helper, repo, raw_state, payloads):
    p.require(raw_state['head'] == SOURCE_COMMIT, 'Unexpected distribution branch parent')
    helper.check_heads({'raw': raw_state})
    p.git(repo, 'fetch', '--no-tags', f'https://github.com/{p.REPO}.git', raw_state['head'])
    prepared = p.prepare_commit(repo, raw_state,
        {'downloads/' + name: payloads[name] for name in (p.SINGLE, p.COMBINED)},
        'release: exact SwiftKey1825 and bundle314 immutable MPP downloads')
    helper.check_heads({'raw': raw_state})
    p.git(repo, 'push', f'--force-with-lease=refs/heads/{p.RAW_BRANCH}:{raw_state["head"]}',
          f'https://github.com/{p.REPO}.git', f'{prepared["head"]}:refs/heads/{p.RAW_BRANCH}')
    helper.check_heads({'raw': prepared})
    return prepared


def extract_original(path, output):
    raw = path.read_bytes()
    p.require(len(raw) == ARTIFACT_BYTES and p.sha(raw) == ARTIFACT_SHA256,
              'Downloaded original CI ZIP differs from its reviewed digest')
    p.require(not output.exists(), 'Artifact extraction directory already exists')
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        p.require(len(names) == len(set(names)), 'Duplicate original artifact paths')
        for entry in z.infolist():
            p.checked_path(entry.filename)
            p.require((entry.external_attr >> 16) & 0o170000 != 0o120000, 'Artifact contains symlink')
        p.require(z.testzip() is None, 'Original CI ZIP is corrupt')
        z.extractall(output)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', required=True, type=Path)
    parser.add_argument('--artifact-zip', required=True, type=Path)
    parser.add_argument('--work', required=True, type=Path)
    parser.add_argument('--preflight-only', action='store_true')
    args = parser.parse_args()
    args.repo, args.work = args.repo.resolve(), args.work.resolve()
    p.require(os.environ.get('GITHUB_REPOSITORY') == p.REPO, 'Use the authorized repository Actions runner')
    checkout = os.environ.get('GITHUB_SHA')
    p.require(checkout and p.git(args.repo, 'rev-parse', 'HEAD') == checkout, 'Unexpected workflow checkout')
    helper = p.load_helper(args.repo)
    p.git(args.repo, 'fetch', '--no-tags', f'https://github.com/{p.REPO}.git', SOURCE_COMMIT)
    p.require(p.git(args.repo, 'show', '-s', '--format=%P', checkout) == SOURCE_COMMIT,
              'Recovery commit must directly follow the reviewed source commit')
    p.require(set(p.git(args.repo, 'diff', '--name-only', SOURCE_COMMIT, checkout).splitlines()) == RECOVERY_PATHS,
              'Recovery commit contains unexpected edits')
    manifest_path = ROOT / 'manifest.json'
    p.require(p.sha(manifest_path.read_bytes()) == MANIFEST_SHA256, 'Reviewed artifact manifest changed')
    expected = p.load_expected(manifest_path)
    source_count = p.verify_source_files(ROOT / 'src', expected)
    p.require((args.repo / '.github/workflows/swiftkey1825-314-publish.yml').read_bytes()
              == (ROOT / 'src/workflow.yml').read_bytes(), 'Original build workflow changed')
    run = helper.api(f'repos/{p.REPO}/actions/runs/{RUN_ID}')
    job = helper.api(f'repos/{p.REPO}/actions/jobs/{JOB_ID}')
    artifact = helper.api(f'repos/{p.REPO}/actions/artifacts/{ARTIFACT_ID}')
    validate_prior_run(run, job, artifact)
    args.work.mkdir(parents=True, exist_ok=True)
    extract_original(args.artifact_zip, args.work / 'original')
    dist, second = args.work / 'original/dist-one', args.work / 'original/dist-two'
    old_receipt_raw = (dist / p.RECEIPT_NAME).read_bytes()
    old_receipt = json.loads(old_receipt_raw)
    p.require(old_receipt['status'] == 'failed_requires_inspection' and old_receipt['release_id'] == RELEASE_ID
              and old_receipt['assets'] == expected['artifacts'] and old_receipt['operations'] == []
              and 'too large to process' in old_receipt['error'], 'Prior failure receipt differs')
    payloads, notes, local_report = p.validate_local(dist, second, expected,
        helper.fetch(p.BASE_URL), helper.fetch(p.SINGLE_BASE_URL))
    states = {branch: helper.snapshot(branch) for branch in ('main', 'dev')}
    raw_state = helper.snapshot(p.RAW_BRANCH)
    validate_states(states, raw_state, checkout)
    helper.check_heads({**states, 'raw': raw_state})
    release = helper.api(f'repos/{p.REPO}/releases/tags/{p.TAG}')
    validate_release(release)
    helper.verify_assets(RELEASE_ID, expected['artifacts'], True)
    policies = {branch: helper.content('releases/ULike_ACTIVE.json', state['head']) for branch, state in states.items()}
    created = p.utc_created(states)
    for state in states.values():
        p.metadata(state, 'https://example.invalid/preflight', created, notes)
    if args.preflight_only:
        print('PASS original two-build evidence, exact six public assets, source pins, and inspected partial state; no writes')
        return
    receipt_path = args.work / p.RECEIPT_NAME
    p.require(not receipt_path.exists(), 'Recovery receipt already exists; inspect before retry')
    receipt = {key: value for key, value in old_receipt.items() if key not in {'status', 'error'}}
    receipt.update(local_validation=local_report, operations=[], recovery={
        'reason': 'GitHub JSON blob API rejected large MPP; resume with leased Git pack push',
        'source_commit': SOURCE_COMMIT, 'recovery_commit': checkout,
        'original_run_id': RUN_ID, 'original_job_id': JOB_ID, 'original_artifact_id': ARTIFACT_ID,
        'original_artifact_sha256': ARTIFACT_SHA256, 'original_failed_receipt_sha256': p.sha(old_receipt_raw),
        'original_successful_validation_steps': SUCCESS_STEPS, 'source_files_verified': source_count,
        'existing_six_assets_replaced': False, 'raw_transfer': 'git-private-index-and-leased-push'})

    def checkpoint(status, **values):
        receipt.update(status=status, **values)
        receipt_path.write_bytes(p.json_bytes(receipt))
        print(status, flush=True)

    checkpoint('recovery_preflight_passed')
    try:
        helper.check_heads(states)
        raw_state = commit_raw(helper, args.repo, raw_state, payloads)
        urls = {name: f'https://raw.githubusercontent.com/{p.REPO}/{raw_state["head"]}/downloads/{name}'
                for name in (p.SINGLE, p.COMBINED)}
        for name, url in urls.items():
            p.require(helper.fetch(url) == payloads[name], 'Immutable MPP mismatch: ' + name)
        checkpoint('immutable_downloads_verified', raw_commit=raw_state['head'], download_urls=urls)
        helper.check_heads(states)
        new_metadata, updates = {}, {}
        for branch, state in states.items():
            feed, changelog = p.metadata(state, urls[p.COMBINED], created, notes)
            new_metadata[branch] = (feed, changelog)
            updates[branch] = {'patches-bundle.json': p.json_bytes(feed), 'CHANGELOG.md': changelog.encode()}
            if branch == 'main':
                for name in (p.QA_NAME, 'RELEASE_NOTES.txt', 'SHA256SUMS.txt'):
                    updates[branch][p.RELEASE_ROOT + '/' + name] = payloads[name]
        active = p.commit_feeds_atomically(helper, args.repo, states, updates,
            'release: SwiftKey1825 dictionary updates and deduplicated expansion; bundle314 Manager update')
        for branch, state in active.items():
            feed, changelog = new_metadata[branch]
            p.require(json.loads(helper.content('patches-bundle.json', state['head'])) == feed, 'Remote feed mismatch')
            p.require(helper.content('CHANGELOG.md', state['head']).decode() == changelog, 'Remote changelog mismatch')
            p.require(helper.content('releases/ULike_ACTIVE.json', state['head']) == policies[branch], 'ULike policy changed')
            p.require(helper.fetch(feed['download_url']) == payloads[p.COMBINED], 'Manager download mismatch')
            attempts = p.verify_public_branch(helper, active, branch, feed, changelog)
            receipt['operations'].append({'branch': branch, 'commit': state['head'], 'feed_verified': True,
                'changelog_verified': True, 'ulike_policy_byte_identical': True, 'manager_download_verified': True,
                'public_verification_attempts': attempts})
        helper.check_heads(active)
        helper.verify_assets(RELEASE_ID, expected['artifacts'], True)
        checkpoint('published_and_verified', published=True, release_url=p.PAGE,
            manager_main_dev_updated_atomically=True, manager_update_eligible_for_both_app_names=True,
            manager_device_update_badge_observed=False, other_apps_false_updates=False, fixed_chatgpt_site_updated=False)
        receipt_bytes = receipt_path.read_bytes()
        saved = p.commit_feeds_atomically(helper, args.repo, active,
            {branch: {p.RELEASE_ROOT + '/' + p.RECEIPT_NAME: receipt_bytes} for branch in active},
            'docs: verified SwiftKey1825 publication receipt')
        for branch, state in saved.items():
            p.require(helper.content(p.RELEASE_ROOT + '/' + p.RECEIPT_NAME, state['head']) == receipt_bytes,
                      'Saved receipt differs')
            p.require(helper.content('releases/ULike_ACTIVE.json', state['head']) == policies[branch],
                      'ULike policy changed while saving receipt')
        helper.run(['gh', 'release', 'upload', p.TAG, '--repo', p.REPO, receipt_path])
        helper.verify_assets(RELEASE_ID, {**expected['artifacts'], p.RECEIPT_NAME:
            {'bytes': len(receipt_bytes), 'sha256': p.sha(receipt_bytes)}}, True)
        helper.check_heads(saved)
        print('PASS seven public assets, immutable MPPs, both atomic Manager feeds, preservation, and receipts', flush=True)
    except BaseException as error:
        checkpoint('failed_requires_inspection', error=type(error).__name__ + ': ' + str(error))
        raise


if __name__ == '__main__':
    main()
