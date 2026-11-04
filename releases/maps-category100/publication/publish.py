#!/usr/bin/env python3
"""Publish verified Maps MPPs only after reproducible builds and preservation checks.

The supplied APK is tested locally and never included in public artifacts. CI
rebuilds the MPPs and compares their exact bytes to the reviewed local results.
"""
from __future__ import annotations

import argparse
import base64
import datetime as dt
import hashlib
import io
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import time
import urllib.request
import zipfile

REPO = 'hiro191u3n2/hiro-morphe-patches'
ROOT_NAME = 'releases/maps-category100'
HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
WORKFLOW = '.github/workflows/maps-category100-publish.yml'
VERSION = '1.0.0'
PREVIOUS = '1.0.345'
BUNDLE_VERSION = '1.0.346'
TAG = 'maps-category-v1.0.0'
RAW_BRANCH = 'release/maps-category100-346'
PAGE = f'https://github.com/{REPO}/releases/tag/{TAG}'
SINGLE = 'Maps_Hide_Category_Row_v1.0.0.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.346.mpp'
QA = 'QA_Maps_Category_v1.0.0.json'
SOURCE_ZIP = 'Maps_Hide_Category_Row_v1.0.0_sources.zip'
RECEIPT = 'publication_Maps_Category_v1.0.0.json'
BASE_URL = ('https://raw.githubusercontent.com/' + REPO +
            '/bfe8e7d4748ecbef4e9b8ac572bfdf402a904c01/downloads/Hiro_Morphe_Patches_v1.0.345.mpp')
BASE_SHA = '35993d492f845d143ae70d61dee9d839f5151649e5615de52c3ba90c087fc954'
MF = 'META-INF/MANIFEST.MF'
POLICY = 'releases/ULike_ACTIVE.json'


def require(ok, message):
    if not ok:
        raise RuntimeError(message)


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def encoded(value):
    return (json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + '\n').encode()


def run(command, data=None, env=None):
    p = subprocess.run(list(map(str, command)), input=data, stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE, env=env)
    require(p.returncode == 0, 'Command failed: ' + str(command) + '\n' + p.stderr.decode(errors='replace'))
    return p.stdout


def git(repo, *args, data=None, env=None):
    return run(['git', '-C', repo, '-c', 'credential.helper=', '-c',
                'credential.helper=!gh auth git-credential', *args], data, env).decode().strip()


def api(path, data=None, method=None, absent=False):
    command = ['gh', 'api', path]
    if method:
        command += ['--method', method]
    if data is not None:
        command += ['--input', '-']
    p = subprocess.run(command, input=None if data is None else encoded(data),
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if p.returncode:
        if absent and data is None and b'(HTTP 404)' in p.stderr:
            return None
        raise RuntimeError(p.stderr.decode(errors='replace'))
    return json.loads(p.stdout) if p.stdout.strip() else None


def fetch(url):
    for attempt in range(4):
        try:
            request = urllib.request.Request(url, headers={'User-Agent': 'Hiro-MapsCategory100', 'Cache-Control': 'no-cache'})
            with urllib.request.urlopen(request, timeout=120) as response:
                require(response.status == 200, 'Download did not return HTTP 200')
                return response.read()
        except OSError:
            if attempt == 3:
                raise
            time.sleep(2 ** attempt)


def head(branch):
    return api(f'repos/{REPO}/git/ref/heads/{branch}')['object']['sha']


def content(path, commit):
    require(re.fullmatch('[0-9a-f]{40}', commit), 'Immutable commit required')
    row = api(f'repos/{REPO}/contents/{path}?ref={commit}')
    raw = base64.b64decode(row['content']) if row.get('encoding') == 'base64' else fetch(
        f'https://raw.githubusercontent.com/{REPO}/{commit}/{path}')
    require(len(raw) == row['size'] and hashlib.sha1(b'blob ' + str(len(raw)).encode() + b'\0' + raw).hexdigest() == row['sha'],
            'Remote content hash differs: ' + path)
    return raw


def source_files(repo):
    files = {}
    root = repo / ROOT_NAME
    for p in sorted(root.rglob('*')):
        if not p.is_file() or '__pycache__' in p.parts or p.suffix == '.pyc':
            continue
        relative = p.relative_to(root).as_posix()
        if relative in ('publication/manifest.json', RECEIPT):
            continue
        require(not p.is_symlink(), 'Source symlink not allowed')
        require(p.suffix not in ('.apk', '.apks', '.apkm', '.mpp', '.class', '.zip'),
                'Application/build binary must not enter public sources: ' + relative)
        files[p.relative_to(repo).as_posix()] = p.read_bytes()
    files[WORKFLOW] = (repo / WORKFLOW).read_bytes()
    return files


def archive(raw):
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        require(z.testzip() is None and len(z.namelist()) == len(set(z.namelist())), 'Invalid ZIP')
        return {n: z.read(n) for n in z.namelist() if not n.endswith('/')}


def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    return dict(row.decode().split(': ', 1) for row in rows)


def write_zip(path, files):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name, raw in sorted(files.items()):
            info = zipfile.ZipInfo(name, (2026, 11, 5, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, raw)


def verify(args):
    cfg = json.loads((HERE / 'manifest.json').read_text())
    require(cfg['schema'] == 'maps-category100-publication-v1' and cfg['ready'] is True, 'Publication is not frozen')
    require(cfg['baseline_url'] == BASE_URL and cfg['baseline_sha256'] == BASE_SHA, 'Wrong reviewed baseline')
    require(cfg['source_sha256'] == {n: sha(v) for n, v in source_files(args.repo).items()}, 'Reviewed source changed')
    base = args.baseline.read_bytes()
    require(sha(base) == BASE_SHA, 'Baseline MPP differs')
    payloads = {}
    for name in (SINGLE, BUNDLE):
        raw = (args.dist / name).read_bytes()
        require({'bytes': len(raw), 'sha256': sha(raw)} == cfg['artifacts'][name], 'MPP differs from reviewed local output: ' + name)
        if args.second_dist:
            require(raw == (args.second_dist / name).read_bytes(), 'Fresh CI builds differ: ' + name)
        payloads[name] = raw
    old, new, single = map(archive, (base, payloads[BUNDLE], payloads[SINGLE]))
    for items, version in ((old, PREVIOUS), (new, BUNDLE_VERSION), (single, VERSION)):
        require(headers(items[MF])['Version'] == version and items['classes.dex'].startswith(b'dex\n'), 'Invalid MPP version/loader')
    prefix = cfg['patch_prefix']
    extension = cfg.get('runtime_extension')
    own = lambda n: n.startswith(prefix) or (extension is not None and n == extension)
    require(prefix.startswith('app/hiro/') and prefix.endswith('/'), 'Invalid patch prefix')
    preserved = {n: v for n, v in old.items() if n not in (MF, 'classes.dex')}
    require(all(new.get(n) == v for n, v in preserved.items()), 'Existing bundle entry changed or removed')
    require(not any(own(n) for n in old), 'New patch already exists in baseline')
    require(all(n in old or own(n) for n in new), 'Unexpected new bundle entry')
    require(all(own(n) or n in (MF, 'classes.dex') for n in single), 'Unexpected standalone entry')
    require(all(new.get(n) == v for n, v in single.items() if own(n)), 'Standalone/bundle new code differs')
    qa_path = ROOT / 'verification' / QA
    qa_raw = qa_path.read_bytes()
    require(sha(qa_raw) == cfg['local_qa_sha256'], 'Local APK verification changed')
    qa = json.loads(qa_raw)
    require(qa['schema'] == 'maps-category100-qa-v1'
            and (qa['patch_version'], qa['bundle_version'], qa['baseline_version']) == (VERSION, BUNDLE_VERSION, PREVIOUS)
            and qa['baseline_sha256'] == BASE_SHA, 'Local QA version/baseline differs')
    require(qa['artifacts'] == cfg['artifacts'], 'Local QA targets different MPPs')
    require(qa['physical_android_tested'] is False, 'Unexpected device test claim')
    require(qa['production_android_sdk_compile'] is True
            and 'PASS' in qa['guard_tests'] and 'PASS' in qa['loader_preservation'], 'Build/regression/loader evidence missing')
    require(qa['preserved_unrelated_entries'] == len(preserved), 'Preservation count differs')
    require(qa['supplied_apk_sha256'] == cfg['supplied_apk_sha256']
            and qa['supplied_apk_non_dex_resources_unchanged'] is True
            and qa['single_bundle_apk_entry_contents_equal'] is True, 'Supplied APK preservation was not verified')
    require(len(qa['apk_application']) == 2 and
            {(r['bundle'], r['package'], r['version'], r['result']) for r in qa['apk_application']} ==
            {(n, 'org.ungoogled.android.apps.maps.microg', '26.36.04.973607363', 'PASS') for n in (SINGLE, BUNDLE)}, 'Both real APK applications are required')
    require(all('PASS' in r['audit_summary'] for r in qa['apk_application']), 'APK semantic verification missing')
    source = ROOT / 'src'
    require(qa['sources'] == {p.relative_to(source).as_posix(): sha(p.read_bytes())
            for p in sorted(source.rglob('*')) if p.is_file() and p.suffix in ('.java', '.py')}, 'QA used different build sources')
    notes = (HERE / 'RELEASE_NOTES.txt').read_bytes()
    require('未確認' in notes.decode(), 'Physical-device limitation must be explicit')
    payloads[QA] = qa_raw
    payloads['RELEASE_NOTES.txt'] = notes
    args.dist.mkdir(parents=True, exist_ok=True)
    for n, v in payloads.items():
        (args.dist / n).write_bytes(v)
    files = source_files(args.repo)
    files[ROOT_NAME + '/publication/manifest.json'] = (HERE / 'manifest.json').read_bytes()
    write_zip(args.dist / SOURCE_ZIP, files)
    payloads[SOURCE_ZIP] = (args.dist / SOURCE_ZIP).read_bytes()
    sums = ''.join(sha(v) + '  ' + n + '\n' for n, v in sorted(payloads.items())).encode()
    (args.dist / 'SHA256SUMS.txt').write_bytes(sums)
    payloads['SHA256SUMS.txt'] = sums
    print('PASS frozen sources, local APK evidence, exact MPP bytes and additive bundle entries'
          + ('; both fresh builds are byte-identical' if args.second_dist else ''), flush=True)
    return cfg, payloads


def snapshot(branch):
    commit = head(branch)
    return {'branch': branch, 'head': commit,
            'feed': json.loads(content('patches-bundle.json', commit)),
            'log': content('CHANGELOG.md', commit), 'policy': content(POLICY, commit)}


def checked_heads(states):
    require(all(head(b) == s['head'] for b, s in states.items()), 'A remote branch advanced; refusing stale publication')


def commit(repo, base, files, message):
    env = {**os.environ, 'GIT_AUTHOR_NAME': 'github-actions[bot]', 'GIT_COMMITTER_NAME': 'github-actions[bot]',
           'GIT_AUTHOR_EMAIL': '41898282+github-actions[bot]@users.noreply.github.com',
           'GIT_COMMITTER_EMAIL': '41898282+github-actions[bot]@users.noreply.github.com'}
    with tempfile.TemporaryDirectory(prefix='maps-category100-index-') as temporary:
        env['GIT_INDEX_FILE'] = str(Path(temporary) / 'index')
        git(repo, 'read-tree', base, env=env)
        for path, raw in files.items():
            require(not path.startswith('/') and '..' not in Path(path).parts, 'Unsafe Git path')
            blob = git(repo, 'hash-object', '-w', '--stdin', data=raw, env=env)
            git(repo, 'update-index', '--add', '--cacheinfo', f'100644,{blob},{path}', env=env)
        tree = git(repo, 'write-tree', env=env)
        result = git(repo, 'commit-tree', tree, '-p', base, '-F', '-', data=(message + '\n').encode(), env=env)
        changed = set(git(repo, 'diff-tree', '--no-commit-id', '--name-only', '-r', result).splitlines())
        require(changed and changed.issubset(files), 'Unexpected changed paths')
        return result


def verify_assets(release_id, payloads, public):
    release = api(f'repos/{REPO}/releases/{release_id}')
    require(release['tag_name'] == TAG and release['draft'] is (not public), 'Wrong release identity/state')
    assets = {a['name']: a for a in release['assets']}
    require(set(assets) == set(payloads) and len(assets) == len(release['assets']), 'Release asset inventory differs')
    for name, raw in payloads.items():
        asset = assets[name]
        require(asset['size'] == len(raw) and asset['digest'] == 'sha256:' + sha(raw), 'Uploaded asset differs: ' + name)


def publish(args, cfg, payloads):
    states = {b: snapshot(b) for b in ('main', 'dev')}
    source_commit = git(args.repo, 'rev-parse', 'HEAD')
    require(states['main']['head'] == source_commit and states['dev']['head'] == cfg['reviewed_dev'], 'Reviewed heads changed')
    require(git(args.repo, 'show', '-s', '--format=%P', source_commit) == cfg['reviewed_main'], 'Source commit has wrong parent')
    for state in states.values():
        require(state['feed']['version'] == PREVIOUS and state['feed']['download_url'] == BASE_URL, 'Active baseline advanced')
    require(states['main']['feed'] == states['dev']['feed'], 'Manager feeds diverged')
    require(states['main']['policy'] == states['dev']['policy'], 'Existing ULike policy diverged')
    for path in (f'repos/{REPO}/releases/tags/{TAG}', f'repos/{REPO}/git/ref/tags/{TAG}',
                 f'repos/{REPO}/git/ref/heads/{RAW_BRANCH}'):
        require(api(path, absent=True) is None, 'Publication name already exists: ' + path)
    require(not any(a['name'] == BUNDLE for r in api(f'repos/{REPO}/releases?per_page=100') for a in r.get('assets', [])), 'Bundle version already exists')
    checked_heads(states)
    if args.preflight_only:
        print('PASS reviewed heads and unpublished version')
        return
    notes = payloads['RELEASE_NOTES.txt'].decode()
    release = api(f'repos/{REPO}/releases', {'tag_name': TAG, 'target_commitish': source_commit,
        'name': 'Maps Hide Category Row v1.0.0 / Hiro Morphe Patches v1.0.346',
        'body': notes, 'draft': True, 'prerelease': True, 'make_latest': 'false'})
    for name in payloads:
        run(['gh', 'release', 'upload', TAG, '--repo', REPO, args.dist / name])
    verify_assets(release['id'], payloads, False)
    checked_heads(states)
    git(args.repo, 'fetch', '--no-tags', f'https://github.com/{REPO}.git', *[s['head'] for s in states.values()])
    artifact_commit = commit(args.repo, source_commit, {'downloads/' + BUNDLE: payloads[BUNDLE]}, 'release: immutable verified Maps category bundle346')
    git(args.repo, 'push', f'--force-with-lease=refs/heads/{RAW_BRANCH}:', f'https://github.com/{REPO}.git',
        f'{artifact_commit}:refs/heads/{RAW_BRANCH}')
    download_url = f'https://raw.githubusercontent.com/{REPO}/{artifact_commit}/downloads/{BUNDLE}'
    require(fetch(download_url) == payloads[BUNDLE], 'Immutable Manager download differs')
    api(f"repos/{REPO}/releases/{release['id']}", {'draft': False, 'prerelease': True, 'make_latest': 'false'}, 'PATCH')
    verify_assets(release['id'], payloads, True)
    checked_heads(states)
    created = dt.datetime.now(dt.timezone.utc).strftime('%Y-%m-%dT%H:%M:%S')
    changelog = ('# [1.0.346](' + PAGE + ') (2026-11-05)\n\n'
        '* **Google Maps:** 検索欄下のカテゴリ行（自宅・レストラン・コーヒー等）を非表示にするパッチを追加。\n'
        '* **Google Maps:** 添付microG版APKへの適用・再構築と既存パッチの保持を検証。実機表示は未確認。\n\n').encode()
    receipt = {'schema': 'maps-category100-publication-v1', 'release_url': PAGE,
               'source_commit': source_commit, 'artifact_commit': artifact_commit, 'download_url': download_url,
               'artifacts': {n: {'bytes': len(v), 'sha256': sha(v)} for n, v in payloads.items()},
               'physical_android_tested': False, 'status': 'assets_and_download_verified'}
    updates, expected = {}, {}
    for branch, state in states.items():
        feed = {**state['feed'], 'version': BUNDLE_VERSION, 'created_at': created,
                'download_url': download_url, 'page_url': PAGE, 'signature_download_url': '',
                'description': notes.strip() + '\n\n' + state['feed']['description']}
        require(created > state['feed']['created_at'], 'Feed timestamp must increase')
        files = {'patches-bundle.json': encoded(feed), 'CHANGELOG.md': changelog + state['log'],
                 ROOT_NAME + '/' + RECEIPT: encoded(receipt)}
        updates[branch] = commit(args.repo, state['head'], files, 'release: Maps category row and bundle346 Manager update')
        expected[branch] = files
    checked_heads(states)
    git(args.repo, 'push', '--atomic', f"--force-with-lease=refs/heads/main:{states['main']['head']}",
        f"--force-with-lease=refs/heads/dev:{states['dev']['head']}", f'https://github.com/{REPO}.git',
        *[f'{updates[b]}:refs/heads/{b}' for b in ('main', 'dev')])
    for branch, updated in updates.items():
        require(head(branch) == updated, 'Feed ref differs after atomic push')
        for path, raw in expected[branch].items():
            require(content(path, updated) == raw, 'Published metadata differs')
        require(content(POLICY, updated) == states[branch]['policy'], 'Unrelated policy changed')
        require(fetch(f'https://raw.githubusercontent.com/{REPO}/{updated}/patches-bundle.json') == expected[branch]['patches-bundle.json'],
                'Public feed differs')
    require(fetch(download_url) == payloads[BUNDLE], 'Published MPP bytes changed')
    (args.dist / RECEIPT).write_bytes(encoded({**receipt, 'status': 'published_and_verified', 'feed_heads': updates}))
    print('PASS published and verified ' + PAGE, flush=True)


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--repo', type=Path, required=True)
    p.add_argument('--dist', type=Path, required=True)
    p.add_argument('--second-dist', type=Path)
    p.add_argument('--baseline', type=Path, required=True)
    p.add_argument('--local-only', action='store_true')
    p.add_argument('--preflight-only', action='store_true')
    args = p.parse_args()
    args.repo = args.repo.resolve()
    cfg, payloads = verify(args)
    if not args.local_only:
        publish(args, cfg, payloads)


if __name__ == '__main__':
    main()
