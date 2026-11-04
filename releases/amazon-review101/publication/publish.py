#!/usr/bin/env python3
"""Publish verified Amazon review-count MPPs only after reproducible builds and preservation checks.

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
ROOT_NAME = 'releases/amazon-review101'
HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
WORKFLOW = '.github/workflows/amazon-review101-publish.yml'
VERSION = '1.0.1'
BASELINE = json.loads((HERE / 'baseline.json').read_text())
PREVIOUS = BASELINE['version']
BUNDLE_VERSION = '1.0.349'
TAG = 'amazon-review-count-v1.0.1'
RAW_BRANCH = 'release/amazon-review101-349'
PAGE = f'https://github.com/{REPO}/releases/tag/{TAG}'
SINGLE = 'Amazon_Review_Count_v1.0.1.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.349.mpp'
QA = 'QA_Amazon_Review_Count_v1.0.1.json'
SOURCE_ZIP = 'Amazon_Review_Count_v1.0.1_sources.zip'
RECEIPT = 'publication_Amazon_Review_Count_v1.0.1.json'
BASE_URL = BASELINE['url']
BASE_SHA = BASELINE['sha256']
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
            request = urllib.request.Request(url, headers={'User-Agent': 'Hiro-AmazonReview101', 'Cache-Control': 'no-cache'})
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
        if not p.is_file() or any(part in p.parts for part in ('__pycache__', 'node_modules')) or p.suffix == '.pyc':
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
    require(cfg['schema'] == 'amazon-review101-publication-v1' and cfg['ready'] is True, 'Publication is not frozen')
    require(cfg['baseline_url'] == BASE_URL and cfg['baseline_sha256'] == BASE_SHA, 'Wrong reviewed baseline')
    require(cfg['source_sha256'] == {n: sha(v) for n, v in source_files(args.repo).items()}, 'Reviewed source changed')
    base = args.baseline.read_bytes()
    require(re.fullmatch('[0-9a-f]{64}', BASE_SHA), 'Final baseline SHA is not pinned')
    require(BASE_URL.startswith(f'https://raw.githubusercontent.com/{REPO}/')
            and re.fullmatch('[0-9a-f]{40}', BASE_URL.split('/')[5]), 'Baseline URL must be commit-immutable')
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
    require(prefix == 'app/hiro/amazonreviews/patches/' and extension == 'extensions/amazon_review_sort.mpe', 'Replacement scope must remain Amazon-only')
    preserved = {n: v for n, v in old.items() if n not in (MF, 'classes.dex') and not own(n)}
    require(all(new.get(n) == v for n, v in preserved.items()), 'Existing bundle entry changed or removed')
    require(any(n.startswith(prefix) for n in old) and extension in old, 'Expected previous Amazon patch is missing')
    require({n for n in new if own(n)} == {n for n in single if own(n)}, 'Stale or missing Amazon replacement entries')
    require(all(n in old or own(n) for n in new), 'Unexpected new bundle entry')
    require(all(own(n) or n in (MF, 'classes.dex') for n in single), 'Unexpected standalone entry')
    require(all(new.get(n) == v for n, v in single.items() if own(n)), 'Standalone/bundle new code differs')
    qa_path = ROOT / 'verification' / QA
    qa_raw = qa_path.read_bytes()
    require(sha(qa_raw) == cfg['local_qa_sha256'], 'Local APK verification changed')
    qa = json.loads(qa_raw)
    require(qa['schema'] == 'amazon-review101-qa-v1'
            and (qa['patch_version'], qa['bundle_version'], qa['baseline_version']) == (VERSION, BUNDLE_VERSION, PREVIOUS)
            and qa['baseline_sha256'] == BASE_SHA, 'Local QA version/baseline differs')
    require(qa['artifacts'] == cfg['artifacts'], 'Local QA targets different MPPs')
    require(qa['physical_android_tested'] is False, 'Unexpected device test claim')
    require(qa['toolchain_sha256']['morphe-2.2.jar'] == '855dd2d6fa5ed997a5884a44f8807c9b5bc95e69b5ebd58dfd7e8b897c86e2d7'
            and qa['apk_bytecode_mode'] == 'STRIP_FAST', 'Actual Patcher 2.2 evidence missing')
    require(qa['production_android_sdk_compile'] is True
            and 'PASS' in qa['guard_tests'] and 'PASS' in qa['loader_preservation']
            and 'PASS' in qa['host_tests'], 'Build/regression/loader evidence missing')
    require(qa['preserved_unrelated_entries'] == len(preserved), 'Preservation count differs')
    require(qa['supplied_apk_sha256'] == cfg['supplied_apk_sha256']
            and qa['supplied_apk_non_dex_resources_unchanged'] is True
            and qa['single_bundle_apk_entry_contents_equal'] is True, 'Supplied APK preservation was not verified')
    require(len(qa['apk_application']) == 2 and
            {(r['bundle'], r['sdk'], r['package'], r['version'], r['result']) for r in qa['apk_application']} ==
            {(n, sdk, 'com.amazon.mShop.android.shopping', '32.19.0.100', 'PASS')
             for n in (SINGLE, BUNDLE) for sdk in ('morphe-2.2.jar',)},
            'Both artifacts must be applied to the supplied APK with actual Patcher 2.2')
    require(all('PASS' in r['audit_summary'] for r in qa['apk_application']), 'APK semantic verification missing')
    source = ROOT / 'src'
    require(qa['sources'] == {p.relative_to(source).as_posix(): sha(p.read_bytes())
            for p in sorted(source.rglob('*')) if p.is_file() and not any(part in p.parts for part in ('__pycache__', 'node_modules'))
            and p.suffix in ('.java', '.py', '.js', '.json')}, 'QA used different build sources')
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
    print('PASS frozen sources, local APK evidence, exact MPP bytes and Amazon-only replacement with unrelated bundle entries preserved'
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
    with tempfile.TemporaryDirectory(prefix='amazon-review101-index-') as temporary:
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
        'name': 'Amazon 評価件数順パッチ v1.0.1 / Hiro Morphe Patches v1.0.349',
        'body': notes, 'draft': True, 'prerelease': True, 'make_latest': 'false'})
    for name in payloads:
        run(['gh', 'release', 'upload', TAG, '--repo', REPO, args.dist / name])
    verify_assets(release['id'], payloads, False)
    checked_heads(states)
    git(args.repo, 'fetch', '--no-tags', f'https://github.com/{REPO}.git', *[s['head'] for s in states.values()])
    artifact_commit = commit(args.repo, source_commit, {'downloads/' + n: payloads[n] for n in (SINGLE, BUNDLE)}, 'release: immutable verified Amazon review count bundle349')
    git(args.repo, 'push', f'--force-with-lease=refs/heads/{RAW_BRANCH}:', f'https://github.com/{REPO}.git',
        f'{artifact_commit}:refs/heads/{RAW_BRANCH}')
    download_url = f'https://raw.githubusercontent.com/{REPO}/{artifact_commit}/downloads/{BUNDLE}'
    for name in (SINGLE, BUNDLE):
        url = f'https://raw.githubusercontent.com/{REPO}/{artifact_commit}/downloads/{name}'
        require(fetch(url) == payloads[name], 'Immutable download differs: ' + name)
    api(f"repos/{REPO}/releases/{release['id']}", {'draft': False, 'prerelease': True, 'make_latest': 'false'}, 'PATCH')
    verify_assets(release['id'], payloads, True)
    checked_heads(states)
    created = dt.datetime.now(dt.timezone.utc).strftime('%Y-%m-%dT%H:%M:%S')
    changelog = ('# [1.0.349](' + PAGE + ') (2026-11-05)\n\n'
        '* **Amazon ショッピング:** `商品検索を評価件数の多い順に表示` v1.0.1へ更新。検索画面の再利用・同一画面内更新時の起動処理と、評価件数の取得を修正。\n'
        '* **Amazon Shopping:** 読み込み待ち・件数未取得の表示を追加。星横の小さな件数、星と件数の共通リンク、同じASINの内側の枠、検索結果の子孫要素にある商品を修正。追加検索URLにも対応。Patcher2.2で適用確認。実機表示は未確認。\n'
        '* **Amazon:** com.amazon.mShop.android.shopping 32.19.0.100が対象。読み込み済みの通常商品を並べ替えます。今回の報告原因の断定・実機での改善確認は未実施です。\n\n').encode()
    receipt = {'schema': 'amazon-review101-publication-v1', 'release_url': PAGE,
               'source_commit': source_commit, 'artifact_commit': artifact_commit, 'download_url': download_url,
               'standalone_url': f'https://raw.githubusercontent.com/{REPO}/{artifact_commit}/downloads/{SINGLE}',
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
        updates[branch] = commit(args.repo, state['head'], files, 'release: Amazon review count and bundle349 Manager update')
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
    for name in (SINGLE, BUNDLE):
        url = f'https://raw.githubusercontent.com/{REPO}/{artifact_commit}/downloads/{name}'
        require(fetch(url) == payloads[name], 'Published MPP bytes changed: ' + name)
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
