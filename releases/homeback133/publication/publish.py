#!/usr/bin/env python3
"""Publish the unchanged recovered bundle317 with explicitly scoped recovery evidence.

This is an artifact-recovery publication, not a replacement for homeback132's
build/APK gates. The sibling check.py validates fresh recovery evidence. The
original bundle317 QA reports were lost and are not synthesized here.
"""
from __future__ import annotations

import argparse
import base64
import datetime as dt
import hashlib
import io
import json
import os
import re
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
SOURCE = HERE.parent
RELEASE_ROOT = 'releases/homeback133'
REPO = 'hiro191u3n2/hiro-morphe-patches'
REVIEWED_MAIN = '32919714f676ffa7709271fd89fbda9ba825d46f'
REVIEWED_DEV = '842f1942e601269ffad3c35602a2093bd8690ffd'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.317.mpp'
BUNDLE_BYTES = 48001679
BUNDLE_SHA = '4f190bc54c91effbfa778b62bf913e2b6509f82c8e8b4f1b99b5a59ccae3a466'
BASE_URL = ('https://raw.githubusercontent.com/' + REPO +
            '/b4000c37b3d162f3f26daaefd3d0ee1966e42f51/downloads/Hiro_Morphe_Patches_v1.0.316.mpp')
BASE_SHA = 'd45359b07529e3c1bcbebfe795befeeae8ebe53e5794725c2c06750e996e06ea'
TAG = 'oneback-v1.1.26'
RAW_BRANCH = 'release/homeback133-317'
PAGE = 'https://github.com/' + REPO + '/releases/tag/' + TAG
SOURCE_ZIP = 'HomeBack_v1.1.26_recovered_sources.zip'
QA = 'HomeBack_v1.1.26_recovery-verification.json'
NOTES = 'RELEASE_NOTES.txt'
SUMS = 'SHA256SUMS.txt'
RECEIPT = 'publication_HomeBack_v1.1.26_recovery.json'
ASSETS = (BUNDLE, SOURCE_ZIP, QA, NOTES, SUMS)
POLICY_PATH = 'releases/ULike_ACTIVE.json'
WORKFLOW = '.github/workflows/homeback133-publish.yml'


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + '\n').encode('utf-8')


def checked_path(path):
    require(isinstance(path, str) and path and '\\' not in path and '\x00' not in path,
            'Invalid repository path')
    require(not path.startswith('/') and all(part not in ('', '.', '..') for part in path.split('/')),
            'Unsafe repository path: ' + path)


def run(command, *, data=None, env=None):
    result = subprocess.run(command, input=data, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, env=env)
    require(result.returncode == 0,
            'Command failed: ' + str(command) + '\n' + result.stderr.decode(errors='replace'))
    return result.stdout


def git(repo, *args, data=None, env=None):
    return run(['git', '-C', str(repo), '-c', 'credential.helper=', '-c',
                'credential.helper=!gh auth git-credential', *map(str, args)],
               data=data, env=env).decode().strip()


def api(path, data=None, method=None, absent=False):
    command = ['gh', 'api', path]
    if method:
        command += ['--method', method]
    if data is not None:
        command += ['--input', '-']
    result = subprocess.run(command, input=None if data is None else json_bytes(data),
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode:
        if absent and data is None and b'(HTTP 404)' in result.stderr:
            return None
        raise RuntimeError('GitHub API failed: ' + result.stderr.decode(errors='replace').strip())
    return json.loads(result.stdout) if result.stdout.strip() else None


def fetch(url):
    for attempt in range(4):
        try:
            request = urllib.request.Request(url, headers={
                'User-Agent': 'Hiro-HomeBack133-Recovery-Publication', 'Cache-Control': 'no-cache'})
            with urllib.request.urlopen(request, timeout=90) as response:
                require(response.status == 200, 'Download must return HTTP 200')
                return response.read()
        except (OSError, urllib.error.URLError):
            if attempt == 3:
                raise
            time.sleep(2 ** attempt)


def head(branch):
    return api(f'repos/{REPO}/git/ref/heads/{branch}')['object']['sha']


def content(path, ref):
    checked_path(path)
    require(re.fullmatch(r'[0-9a-f]{40}', ref) is not None, 'Immutable commit required')
    row = api(f'repos/{REPO}/contents/{path}?ref={ref}')
    require(row.get('type') == 'file', 'Expected repository file: ' + path)
    raw = (base64.b64decode(row['content']) if row.get('encoding') == 'base64'
           else fetch(f'https://raw.githubusercontent.com/{REPO}/{ref}/{path}'))
    require(len(raw) == row['size']
            and hashlib.sha1(b'blob ' + str(len(raw)).encode() + b'\0' + raw).hexdigest() == row['sha'],
            'Git blob identity differs: ' + path)
    return raw


def snapshot(branch):
    current = head(branch)
    return {'branch': branch, 'head': current,
            'feed': json.loads(content('patches-bundle.json', current)),
            'log': content('CHANGELOG.md', current),
            'policy': content(POLICY_PATH, current)}


def check_heads(states):
    for state in states.values():
        require(head(state['branch']) == state['head'], 'Concurrent update: ' + state['branch'])


def prepare_commit(repo, state, files, message):
    env = {**os.environ, 'GIT_AUTHOR_NAME': 'github-actions[bot]',
           'GIT_COMMITTER_NAME': 'github-actions[bot]',
           'GIT_AUTHOR_EMAIL': '41898282+github-actions[bot]@users.noreply.github.com',
           'GIT_COMMITTER_EMAIL': '41898282+github-actions[bot]@users.noreply.github.com'}
    with tempfile.TemporaryDirectory(prefix='homeback133-index-') as directory:
        env['GIT_INDEX_FILE'] = str(Path(directory) / 'index')
        git(repo, 'read-tree', state['head'], env=env)
        for path, raw in files.items():
            checked_path(path)
            blob = git(repo, 'hash-object', '-w', '--stdin', data=raw, env=env)
            git(repo, 'update-index', '--add', '--cacheinfo', f'100644,{blob},{path}', env=env)
        tree = git(repo, 'write-tree', env=env)
        commit = git(repo, 'commit-tree', tree, '-p', state['head'], '-F', '-',
                     data=(message + '\n').encode(), env=env)
        require(git(repo, 'show', '-s', '--format=%P', commit) == state['head'], 'Commit parent differs')
        changed = set(git(repo, 'diff-tree', '--no-commit-id', '--name-only', '-r', commit).splitlines())
        require(changed and changed.issubset(files), 'Unexpected changed repository paths')
        return {**state, 'head': commit}


def commit_feeds(repo, states, updates, message):
    require(set(states) == set(updates) == {'main', 'dev'}, 'Both feed branches required')
    check_heads(states)
    remote = f'https://github.com/{REPO}.git'
    git(repo, 'fetch', '--no-tags', remote, states['main']['head'], states['dev']['head'])
    prepared = {branch: prepare_commit(repo, state, updates[branch], message)
                for branch, state in states.items()}
    check_heads(states)
    git(repo, 'push', '--atomic',
        f"--force-with-lease=refs/heads/main:{states['main']['head']}",
        f"--force-with-lease=refs/heads/dev:{states['dev']['head']}", remote,
        f"{prepared['main']['head']}:refs/heads/main", f"{prepared['dev']['head']}:refs/heads/dev")
    check_heads(prepared)
    return prepared


def source_files(repo):
    result = {}
    for release in ('homeback132', 'homeback133'):
        root = repo / 'releases' / release
        require(root.is_dir(), 'Source directory missing: ' + str(root))
        for path in sorted(root.rglob('*')):
            if not path.is_file() or '__pycache__' in path.parts or path.suffix in ('.pyc', '.class'):
                continue
            relative = path.relative_to(root).as_posix()
            if relative in (RECEIPT, SOURCE_ZIP):
                continue
            require(not path.is_symlink(), 'Source symlink not allowed')
            require(path.suffix not in ('.apk', '.apkm', '.mpp'), 'App/artifact binary in source tree')
            name = path.relative_to(repo).as_posix()
            checked_path(name)
            result[name] = path.read_bytes()
    result[WORKFLOW] = (repo / WORKFLOW).read_bytes()
    return result


def package(args):
    bundle = args.repo / 'downloads' / BUNDLE
    report = SOURCE / 'verification/verified.json'
    run([sys.executable, str(HERE / 'check.py'), '--bundle', str(bundle), '--report', str(report)])
    raw = bundle.read_bytes()
    require(len(raw) == BUNDLE_BYTES and sha(raw) == BUNDLE_SHA, 'Recovered bundle bytes differ')
    require((args.repo / WORKFLOW).read_bytes() == (HERE / 'workflow.yml').read_bytes(),
            'Workflow mirror differs')
    notes = (HERE / NOTES).read_text(encoding='utf-8')
    require(notes.strip(), 'Release notes missing')
    sources = source_files(args.repo)
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, data in sorted(sources.items()):
            info = zipfile.ZipInfo(name, (2026, 10, 31, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            archive.writestr(info, data, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)
    payloads = {BUNDLE: raw, SOURCE_ZIP: buffer.getvalue(), QA: report.read_bytes(),
                NOTES: notes.encode('utf-8')}
    payloads[SUMS] = ''.join(f'{sha(payloads[name])}  {name}\n' for name in sorted(payloads)).encode()
    require(set(payloads) == set(ASSETS), 'Asset inventory differs')
    args.dist.mkdir(parents=True, exist_ok=True)
    for name, data in payloads.items():
        (args.dist / name).write_bytes(data)
    identities = {name: {'bytes': len(data), 'sha256': sha(data)} for name, data in payloads.items()}
    return payloads, identities, notes, sources


def metadata(state, url, created, notes):
    old = state['feed']
    require(old['version'] == '1.0.316' and old['download_url'] == BASE_URL,
            'Active feed advanced from reviewed baseline316')
    require(created > old['created_at'], 'Feed timestamp must increase')
    feed = {**old, 'version': '1.0.317', 'created_at': created, 'download_url': url,
            'page_url': PAGE, 'signature_download_url': '',
            'description': notes.strip() + '\n\n' + old['description']}
    prefix = (f'# 1.0.317 ({created[:10]})\n\n'
              '* **Instagram:** ブラウザリンクから開いたプロフィールの戻る終了経路を修正'
              '（OneBack v1.1.26）。保存済み検証版を同一バイトで公開。実機未確認。\n\n'
              + notes.strip() + '\n\n').encode('utf-8')
    return feed, prefix + state['log']


def verify_assets(release_id, expected, public):
    release = api(f'repos/{REPO}/releases/{release_id}')
    require(release['tag_name'] == TAG and release['draft'] is (not public)
            and release['prerelease'] is True, 'Release identity/state differs')
    assets = {item['name']: item for item in release['assets']}
    require(set(assets) == set(expected) and len(assets) == len(release['assets']),
            'Release asset inventory differs')
    for name, identity in expected.items():
        raw = run(['gh', 'api', f"repos/{REPO}/releases/assets/{assets[name]['id']}",
                   '-H', 'Accept: application/octet-stream'])
        require(len(raw) == identity['bytes'] and sha(raw) == identity['sha256'],
                'Release asset bytes differ: ' + name)


def publish(args):
    payloads, identities, notes, sources = package(args)
    if args.local_only:
        print(json.dumps({'status': 'recovery_artifacts_verified_no_remote_writes',
                          'assets': identities}, indent=2))
        return
    require(os.environ.get('GITHUB_ACTIONS') == 'true', 'Publication requires GitHub Actions')
    source_head = os.environ['GITHUB_SHA']
    require(git(args.repo, 'rev-parse', 'HEAD') == source_head, 'Checkout differs from workflow source')
    require(git(args.repo, 'show', '-s', '--format=%P', source_head) == REVIEWED_MAIN,
            'Recovery commit must descend directly from reviewed main')
    states = {branch: snapshot(branch) for branch in ('main', 'dev')}
    require(states['main']['head'] == source_head and states['dev']['head'] == REVIEWED_DEV,
            'Reviewed branches moved')
    require(states['main']['feed'] == states['dev']['feed'], 'Manager feeds diverged')
    require(sha(fetch(BASE_URL)) == BASE_SHA, 'Public baseline bytes changed')
    created = dt.datetime.now(dt.timezone.utc).replace(tzinfo=None, microsecond=0).isoformat()
    for state in states.values():
        metadata(state, 'https://example.invalid/preflight', created, notes)
    for path in (f'repos/{REPO}/releases/tags/{TAG}', f'repos/{REPO}/git/ref/tags/{TAG}',
                 f'repos/{REPO}/git/ref/heads/{RAW_BRANCH}'):
        require(api(path, absent=True) is None, 'Publication object exists; inspect before retry: ' + path)
    require(not any(item.get('name') == BUNDLE
                    for release in api(f'repos/{REPO}/releases?per_page=100')
                    for item in release.get('assets', [])), 'Bundle version already published')
    if args.preflight_only:
        print(json.dumps({'status': 'recovery_preflight_passed_no_remote_writes',
                          'heads': {key: state['head'] for key, state in states.items()}}, indent=2))
        return
    receipt_path = args.dist / RECEIPT
    require(not receipt_path.exists(), 'Prior receipt exists; inspect before retry')
    receipt = {'schema': 'homeback133-recovery-publication-v1', 'bundle_version': '1.0.317',
               'oneback_version': '1.1.26', 'source_commit': source_head,
               'workflow_run_id': os.environ['GITHUB_RUN_ID'], 'assets': identities,
               'preserved_artifact_sha256': BUNDLE_SHA, 'physical_android_tested': False,
               'original_apk_apply_repeated_in_this_publication': False,
               'original_detailed_qa_reports_recovered': False}

    def checkpoint(status, **changes):
        receipt.update(status=status, **changes)
        receipt_path.write_bytes(json_bytes(receipt))
        print(status, flush=True)

    checkpoint('preflight_passed')
    try:
        check_heads(states)
        release = api(f'repos/{REPO}/releases', {
            'tag_name': TAG, 'target_commitish': source_head,
            'name': 'OneBack v1.1.26 / Hiro Morphe Patches v1.0.317',
            'body': notes, 'draft': True, 'prerelease': True, 'make_latest': 'false'})
        checkpoint('draft_created', release_id=release['id'])
        for name in ASSETS:
            run(['gh', 'release', 'upload', TAG, '--repo', REPO, str(args.dist / name)])
        verify_assets(release['id'], identities, False)
        checkpoint('draft_assets_verified')
        check_heads(states)
        # CI restores the saved artifact byte-for-byte from a pinned binary delta.
        # Publish those verified bytes in a dedicated immutable distribution commit.
        raw_state = prepare_commit(args.repo, {**states['main'], 'branch': RAW_BRANCH},
                                   {'downloads/' + BUNDLE: payloads[BUNDLE]},
                                   'release: immutable preserved Instagram Back bundle317 bytes')
        raw_commit = raw_state['head']
        git(args.repo, 'push', f'--force-with-lease=refs/heads/{RAW_BRANCH}:',
            f'https://github.com/{REPO}.git', f'{raw_commit}:refs/heads/{RAW_BRANCH}')
        url = f'https://raw.githubusercontent.com/{REPO}/{raw_commit}/downloads/{BUNDLE}'
        require(head(RAW_BRANCH) == raw_commit
                and content('downloads/' + BUNDLE, raw_commit) == payloads[BUNDLE]
                and fetch(url) == payloads[BUNDLE],
                'Immutable distribution bytes differ')
        checkpoint('immutable_download_verified', raw_commit=raw_commit, download_url=url)
        check_heads(states)
        api(f"repos/{REPO}/releases/{release['id']}",
            {'draft': False, 'prerelease': True, 'make_latest': 'false'}, 'PATCH')
        verify_assets(release['id'], identities, True)
        checkpoint('release_assets_verified')
        updates, wanted = {}, {}
        for branch, state in states.items():
            feed, log = metadata(state, url, created, notes)
            wanted[branch] = (feed, log)
            files = {'patches-bundle.json': json_bytes(feed), 'CHANGELOG.md': log}
            files.update({name: data for name, data in sources.items()
                          if name.startswith(RELEASE_ROOT + '/')})
            files.update({RELEASE_ROOT + '/' + name: payloads[name] for name in (QA, NOTES, SUMS)})
            updates[branch] = files
        active = commit_feeds(args.repo, states, updates,
                              'release: publish preserved Instagram Back fix and bundle317 Manager update')
        for branch, state in active.items():
            feed, log = wanted[branch]
            require(json.loads(content('patches-bundle.json', state['head'])) == feed
                    and content('CHANGELOG.md', state['head']) == log, 'Remote feed/log differ')
            require(content(POLICY_PATH, state['head']) == states[branch]['policy'],
                    'Unrelated ULike policy changed')
            require(fetch(feed['download_url']) == payloads[BUNDLE], 'Manager download bytes differ')
            for attempt in range(10):
                public = json.loads(fetch(f'https://raw.githubusercontent.com/{REPO}/{branch}/patches-bundle.json?verify={time.time_ns()}'))
                if public == feed:
                    break
                check_heads(active)
                time.sleep(3)
            else:
                raise RuntimeError('Public feed propagation not confirmed: ' + branch)
        check_heads(active)
        verify_assets(release['id'], identities, True)
        checkpoint('published_and_verified', release_url=PAGE,
                   manager_main_dev_updated_atomically=True, manager_app_scope='Instagram',
                   main_commit=active['main']['head'], dev_commit=active['dev']['head'])
        saved = commit_feeds(args.repo, active,
                             {branch: {RELEASE_ROOT + '/' + RECEIPT: receipt_path.read_bytes()}
                              for branch in active},
                             'docs: verified preserved bundle317 publication receipt')
        for state in saved.values():
            require(content(RELEASE_ROOT + '/' + RECEIPT, state['head']) == receipt_path.read_bytes(),
                    'Receipt save differs')
        print('PASS published preserved bundle317, scoped recovery evidence, and both Manager feeds', flush=True)
    except BaseException as error:
        checkpoint('failed_requires_inspection', error=type(error).__name__ + ': ' + str(error))
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, required=True)
    parser.add_argument('--dist', type=Path, required=True)
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument('--local-only', action='store_true')
    modes.add_argument('--preflight-only', action='store_true')
    args = parser.parse_args()
    args.repo = args.repo.resolve()
    args.dist = args.dist.resolve()
    require(SOURCE == args.repo / RELEASE_ROOT, 'Unexpected checkout/source directory')
    publish(args)


if __name__ == '__main__':
    main()
