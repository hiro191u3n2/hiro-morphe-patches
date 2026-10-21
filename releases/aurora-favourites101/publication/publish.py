#!/usr/bin/env python3
"""Publish the verified Aurora patch and advance both Manager feeds atomically."""
from pathlib import Path, PurePosixPath
import argparse
import base64
import datetime as dt
import hashlib
import io
import json
import os
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import zipfile

REPO = 'hiro191u3n2/hiro-morphe-patches'
BASE_VERSION, VERSION, BUNDLE_VERSION = '1.0.265', '1.0.1', '1.0.266'
BASE_URL = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/6d4eb398070e348277844103c4a3eda40b5af6b2/downloads/Hiro_Morphe_Patches_v1.0.265.mpp'
BASE_SHA = '57f06381c5c4690036e3fcaa9c6102986f773475ac1619723842c0f436cf0359'
TAG, RAW_BRANCH = 'aurora-favourites-v1.0.1', 'release/aurora-favourites101'
SINGLE = 'Aurora_Store_Favourites_v1.0.1.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.266.mpp'
QA = 'QA_Aurora_Store_v1.0.1.json'
SOURCE = 'Aurora_Store_v1.0.1_sources_and_QA.zip'
ASSETS = (SINGLE, BUNDLE, QA, SOURCE, 'RELEASE_NOTES.txt', 'SHA256SUMS.txt')
RELEASE_ROOT = 'releases/aurora-favourites101'
POLICY = 'releases/ULike_ACTIVE.json'
SCOPES = ('Aurora Store',)
PAGE = f'https://github.com/{REPO}/releases/tag/{TAG}'
ROOT = Path(__file__).resolve().parent.parent


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def js(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode()


def archive(raw):
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        require(len(z.namelist()) == len(set(z.namelist())) and z.testzip() is None,
                'Corrupt or duplicate archive')
        for name in z.namelist():
            p = PurePosixPath(name)
            require(not p.is_absolute() and '..' not in p.parts and '\\' not in name,
                    'Unsafe archive path')
        return {n: z.read(n) for n in z.namelist() if not n.endswith('/')}


def local(a):
    require(sha(a.base.read_bytes()) == BASE_SHA, 'Baseline265 differs')
    payloads = {n: (a.dist / n).read_bytes() for n in ASSETS}
    expected = json.loads((ROOT / 'publication/expected.json').read_bytes())
    require(expected.get('schema') == 'aurora-favourites101-local-identities-v1', 'Local identities missing')
    import importlib.util
    freeze_spec = importlib.util.spec_from_file_location('aurora_freeze_pub', ROOT / 'publication/freeze.py')
    freeze = importlib.util.module_from_spec(freeze_spec); freeze_spec.loader.exec_module(freeze)
    source_manifest, source_identity = freeze.source_identity()
    require(expected['sources'] == source_manifest and expected['source_sha256'] == source_identity,
            'Source changed after local validation identity was recorded')
    for n in (SINGLE, BUNDLE, QA):
        require(expected['artifacts'][n] == {'bytes': len(payloads[n]), 'sha256': sha(payloads[n])},
                'CI artifact differs from locally validated artifact: ' + n)
    qa = json.loads(payloads[QA])
    require(qa.get('result') == 'PASS' and qa.get('patch_version') == VERSION
            and qa.get('bundle_version') == BUNDLE_VERSION and qa.get('baseline_version') == BASE_VERSION
            and qa.get('bundle_sha256') == sha(payloads[BUNDLE])
            and qa.get('standalone_sha256') == sha(payloads[SINGLE])
            and qa.get('physical_android_tested') is False, 'QA is not bound to this release')
    cfg = json.loads((ROOT / 'publication/input.json').read_bytes())
    require(qa.get('official_apk_sha256') == cfg['official_apk_sha256'], 'QA F-Droid APK input differs')
    require(qa.get('upstream_apk_sha256') == cfg['upstream_apk_sha256'], 'QA official-signed APK input differs')
    old, new, single = archive(a.base.read_bytes()), archive(payloads[BUNDLE]), archive(payloads[SINGLE])
    require(old.keys() == new.keys(), 'Bundle resource inventory changed')
    aurora = {n for n in old if n.startswith('app/hiro/aurora/patches/')
              or n == 'extensions/aurora_favourites.mpe'}
    require(aurora and 'extensions/aurora_favourites.mpe' in aurora, 'Baseline Aurora runtime missing')
    require(set(single) == aurora | {'classes.dex', 'META-INF/MANIFEST.MF'}, 'Standalone resource inventory differs')
    require(all(single[n] == new[n] for n in aurora), 'Standalone/combined Aurora replacement differs')
    changed = {n for n in old if old[n] != new[n]}
    require('classes.dex' in changed and 'extensions/aurora_favourites.mpe' in changed
            and changed <= aurora | {'classes.dex', 'META-INF/MANIFEST.MF'},
            'Unrelated resource changed or Aurora replacement missing')
    require(new['classes.dex'].startswith(b'dex\n'), 'Android classes.dex missing')
    import importlib.util
    spec = importlib.util.spec_from_file_location('aurora_package', ROOT / 'publication/package.py')
    package = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(package)
    source = archive(payloads[SOURCE])
    expected_source = {**package.source_files(), QA: payloads[QA], 'RELEASE_NOTES.txt': payloads['RELEASE_NOTES.txt']}
    require(source == expected_source, 'Public source package differs from reviewed checkout')
    require(payloads['RELEASE_NOTES.txt'] == (ROOT / 'publication/RELEASE_NOTES.txt').read_bytes(), 'Notes differ')
    require(payloads['SHA256SUMS.txt'].decode() == ''.join(
        sha(payloads[n]) + '  ' + n + '\n' for n in ASSETS if n != 'SHA256SUMS.txt'), 'Checksums differ')
    return payloads, qa


def api(path, body=None, method=None, absent=False):
    command = ['gh', 'api', path]
    if method:
        command += ['-X', method]
    if body is not None:
        command += ['--input', '-']
    result = subprocess.run(command, input=js(body) if body is not None else None,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode:
        if absent and ('HTTP 404' in result.stderr.decode() or '(404)' in result.stderr.decode()):
            return None
        raise RuntimeError('GitHub API failed: ' + result.stderr.decode())
    return json.loads(result.stdout) if result.stdout.strip() else None


def fetch(url):
    for attempt in range(4):
        try:
            request = urllib.request.Request(url, headers={'User-Agent': 'Hiro-AuroraFavourites101',
                'Accept-Encoding': 'identity', 'Cache-Control': 'no-cache'})
            with urllib.request.urlopen(request, timeout=45) as response:
                require(response.status == 200, 'Public download HTTP status differs')
                return response.read()
        except (OSError, urllib.error.URLError):
            if attempt == 3:
                raise
            time.sleep(2 ** attempt)


def head(branch):
    return api(f'repos/{REPO}/git/ref/heads/{branch}')['object']['sha']


def content(path, ref):
    row = api(f'repos/{REPO}/contents/{path}?ref={ref}')
    require(row.get('type') == 'file', 'Repository path is not a file')
    raw = base64.b64decode(row['content']) if row.get('encoding') == 'base64' else fetch(
        f'https://raw.githubusercontent.com/{REPO}/{ref}/{path}')
    require(len(raw) == row['size'] and hashlib.sha1(
        b'blob ' + str(len(raw)).encode() + b'\0' + raw).hexdigest() == row['sha'], 'Repository bytes differ')
    return raw


def snapshot(branch):
    commit = head(branch)
    return {'branch': branch, 'head': commit, 'feed': json.loads(content('patches-bundle.json', commit)),
            'log': content('CHANGELOG.md', commit), 'policy': content(POLICY, commit)}


def check_heads(states):
    for s in states.values():
        require(head(s['branch']) == s['head'], 'Concurrent repository update: ' + s['branch'])


def git(repo, *args, input=None, env=None):
    command = ['git', '-C', str(repo), '-c', 'credential.helper=', '-c',
               'credential.helper=!gh auth git-credential', *map(str, args)]
    r = subprocess.run(command, input=input, env=env, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    require(r.returncode == 0, 'Git operation failed: ' + ' '.join(map(str, args)) + '\n' + r.stderr.decode())
    return r.stdout.decode().strip()


def prepare_commit(repo, state, files, message):
    env = {**os.environ, 'GIT_AUTHOR_NAME': 'github-actions[bot]', 'GIT_COMMITTER_NAME': 'github-actions[bot]',
        'GIT_AUTHOR_EMAIL': '41898282+github-actions[bot]@users.noreply.github.com',
        'GIT_COMMITTER_EMAIL': '41898282+github-actions[bot]@users.noreply.github.com'}
    with tempfile.TemporaryDirectory(prefix='aurora-favourites101-index-') as temp:
        env['GIT_INDEX_FILE'] = str(Path(temp) / 'index')
        git(repo, 'read-tree', state['head'], env=env)
        for name, raw in files.items():
            p = PurePosixPath(name)
            require(not p.is_absolute() and '..' not in p.parts and '\\' not in name, 'Unsafe Git path')
            blob = git(repo, 'hash-object', '-w', '--stdin', input=raw, env=env)
            git(repo, 'update-index', '--add', '--cacheinfo', f'100644,{blob},{name}', env=env)
        tree = git(repo, 'write-tree', env=env)
        commit = git(repo, 'commit-tree', tree, '-p', state['head'], '-F', '-', input=(message + '\n').encode(), env=env)
        require(git(repo, 'show', '-s', '--format=%P', commit) == state['head'], 'Commit ancestry differs')
        changed = set(git(repo, 'diff-tree', '--no-commit-id', '--name-only', '-r', commit).splitlines())
        require(bool(changed) and changed <= set(files), 'Commit changes unrelated paths')
        return {**state, 'head': commit}


def atomic_feeds(repo, states, updates, message):
    require(set(states) == set(updates) == {'main', 'dev'}, 'Both feeds must advance together')
    check_heads(states)
    git(repo, 'fetch', '--no-tags', f'https://github.com/{REPO}.git', states['main']['head'], states['dev']['head'])
    prepared = {b: prepare_commit(repo, s, updates[b], message) for b, s in states.items()}
    check_heads(states)
    git(repo, 'push', '--atomic',
        f"--force-with-lease=refs/heads/main:{states['main']['head']}",
        f"--force-with-lease=refs/heads/dev:{states['dev']['head']}", f'https://github.com/{REPO}.git',
        f"{prepared['main']['head']}:refs/heads/main", f"{prepared['dev']['head']}:refs/heads/dev")
    check_heads(prepared)
    return prepared


def release_assets(release_id, payloads, public):
    release = api(f'repos/{REPO}/releases/{release_id}')
    require(release['tag_name'] == TAG and release['draft'] is not public and release['prerelease'] is True,
            'Release identity differs')
    assets = {row['name']: row for row in release['assets']}
    require(set(assets) == set(payloads) and len(assets) == len(release['assets']), 'Asset inventory differs')
    for name, expected in payloads.items():
        row = assets[name]
        require(row['state'] == 'uploaded' and row['size'] == len(expected), 'Asset size or state differs')
        if row.get('digest'):
            require(row['digest'] == 'sha256:' + sha(expected), 'GitHub asset digest differs')
        actual = fetch(row['browser_download_url']) if public else subprocess.check_output(
            ['gh', 'api', f"repos/{REPO}/releases/assets/{row['id']}", '-H', 'Accept: application/octet-stream'])
        require(actual == expected, 'Release asset bytes differ: ' + name)


def main():
    p = argparse.ArgumentParser()
    for name in ('dist', 'base', 'repo'):
        p.add_argument('--' + name, type=Path, required=True)
    p.add_argument('--local-only', action='store_true')
    p.add_argument('--preflight-only', action='store_true')
    a = p.parse_args()
    payloads, qa = local(a)
    if a.local_only:
        print('PASS local public package, QA binding and unrelated resources')
        return
    require(os.environ.get('GITHUB_ACTIONS') == 'true', 'Run publication in the reviewed GitHub workflow')
    require(git(a.repo, 'rev-parse', 'HEAD') == os.environ.get('GITHUB_SHA'), 'CI checkout differs')
    states = {b: snapshot(b) for b in ('main', 'dev')}
    check_heads(states)
    require(states['main']['head'] == os.environ['GITHUB_SHA'], 'main moved since CI started')
    require(states['main']['feed'] == states['dev']['feed'], 'Manager feeds diverged')
    for s in states.values():
        require(s['feed']['version'] == BASE_VERSION and s['feed']['download_url'] == BASE_URL,
                'Active bundle advanced; review the new baseline')
    require(fetch(BASE_URL) == a.base.read_bytes(), 'Live baseline MPP differs')
    require(api(f'repos/{REPO}/releases/tags/{TAG}', absent=True) is None, 'Release already exists')
    require(api(f'repos/{REPO}/git/ref/tags/{TAG}', absent=True) is None, 'Release tag already exists')
    require(api(f'repos/{REPO}/git/ref/heads/{RAW_BRANCH}', absent=True) is None, 'Distribution branch already exists')
    if a.preflight_only:
        print('PASS publication preflight; no remote writes')
        return
    notes = payloads['RELEASE_NOTES.txt'].decode()
    release = api(f'repos/{REPO}/releases', {'tag_name': TAG, 'target_commitish': states['main']['head'],
        'name': 'Aurora Store お気に入り起動・戻る修正 v1.0.1 / Hiro v1.0.266', 'body': notes,
        'draft': True, 'prerelease': True, 'make_latest': 'false'})
    for name in ASSETS:
        subprocess.run(['gh', 'release', 'upload', TAG, '--repo', REPO, str(a.dist / name)], check=True)
    release_assets(release['id'], payloads, False)
    check_heads(states)
    api(f"repos/{REPO}/releases/{release['id']}", {'draft': False, 'prerelease': True, 'make_latest': 'false'}, 'PATCH')
    release_assets(release['id'], payloads, True)
    check_heads(states)
    raw = prepare_commit(a.repo, {**states['main'], 'branch': RAW_BRANCH}, {'downloads/' + name: payloads[name] for name in (SINGLE, BUNDLE)},
                         'release: AuroraFavourites101 and bundle266 immutable download')
    git(a.repo, 'push', f'--force-with-lease=refs/heads/{RAW_BRANCH}:', f'https://github.com/{REPO}.git',
        f"{raw['head']}:refs/heads/{RAW_BRANCH}")
    require(head(RAW_BRANCH) == raw['head'], 'Distribution branch moved')
    url = f"https://raw.githubusercontent.com/{REPO}/{raw['head']}/downloads/{BUNDLE}"
    require(fetch(url) == payloads[BUNDLE], 'Immutable download bytes differ')
    single_url = f"https://raw.githubusercontent.com/{REPO}/{raw['head']}/downloads/{SINGLE}"
    require(fetch(single_url) == payloads[SINGLE], 'Immutable standalone download bytes differ')
    created = dt.datetime.now(dt.timezone.utc).replace(tzinfo=None, microsecond=0).isoformat()
    updates, metadata = {}, {}
    import importlib.util
    spec = importlib.util.spec_from_file_location('aurora_package_pub', ROOT / 'publication/package.py')
    package = importlib.util.module_from_spec(spec); spec.loader.exec_module(package)
    reviewed = {RELEASE_ROOT + '/' + n: v for n, v in package.source_files().items()}
    for b, s in states.items():
        require(created > s['feed']['created_at'], 'Feed timestamp must advance')
        feed = {**s['feed'], 'version': BUNDLE_VERSION, 'created_at': created, 'download_url': url,
            'page_url': PAGE, 'signature_download_url': '', 'description': notes.strip() + '\n\n' + s['feed']['description']}
        bullets = '\n'.join(f'* **{app}:** v1.0.1：お気に入りから戻ると通常ホームへ戻れるよう修正。実機未確認。' for app in SCOPES)
        log = (f'# {BUNDLE_VERSION} ({created[:10]})\n\n' + bullets + '\n\n' + notes.strip() + '\n\n').encode() + s['log']
        metadata[b] = (feed, log)
        updates[b] = {**reviewed, 'patches-bundle.json': js(feed), 'CHANGELOG.md': log,
            **{RELEASE_ROOT + '/' + n: payloads[n] for n in (QA, 'RELEASE_NOTES.txt', 'SHA256SUMS.txt')}}
    active = atomic_feeds(a.repo, states, updates, 'release: AuroraFavourites101 bundle266 Manager update')
    for b, s in active.items():
        feed, log = metadata[b]
        require(json.loads(content('patches-bundle.json', s['head'])) == feed, 'Committed Manager feed differs')
        require(content('CHANGELOG.md', s['head']) == log, 'Committed changelog differs')
        require(content(POLICY, s['head']) == states[b]['policy'], 'ULike active policy changed')
        require(fetch(feed['download_url']) == payloads[BUNDLE], 'Manager download differs')
        for attempt in range(12):
            nonce = str(time.time_ns())
            prefix = f'https://raw.githubusercontent.com/{REPO}/{b}'
            if (json.loads(fetch(prefix + '/patches-bundle.json?aurorafavourites101=' + nonce)) == feed
                    and fetch(prefix + '/CHANGELOG.md?aurorafavourites101=' + nonce) == log):
                break
            check_heads(active)
            if attempt < 11:
                time.sleep(3)
        else:
            raise RuntimeError('Public Manager feed propagation incomplete: ' + b)
    release_assets(release['id'], payloads, True)
    receipt = {'status': 'published_and_verified', 'patch_version': VERSION, 'bundle_version': BUNDLE_VERSION,
        'baseline_version': BASE_VERSION, 'baseline_sha256': BASE_SHA, 'source_commit': os.environ['GITHUB_SHA'],
        'release_url': PAGE, 'download_url': url, 'standalone_download_url': single_url, 'raw_commit': raw['head'], 'physical_android_tested': False,
        'main_dev_updated_atomically': True, 'ulike_policy_unchanged': True,
        'source_sha256': json.loads((ROOT / 'publication/expected.json').read_bytes())['source_sha256'],
        'assets': {n: {'bytes': len(v), 'sha256': sha(v)} for n, v in payloads.items()},
        'feed_commits': {b: s['head'] for b, s in active.items()}}
    receipt_raw = js(receipt)
    (a.dist / 'publication_Aurora_Store_v1.0.1.json').write_bytes(receipt_raw)
    saved = atomic_feeds(a.repo, active, {b: {RELEASE_ROOT + '/publication_Aurora_Store_v1.0.1.json': receipt_raw}
        for b in active}, 'docs: verified AuroraFavourites101 publication receipt')
    for b, s in saved.items():
        require(content(POLICY, s['head']) == states[b]['policy'], 'ULike policy changed saving receipt')
        require(json.loads(content('patches-bundle.json', s['head'])) == metadata[b][0], 'Feed changed saving receipt')
    print('PASS public Release bytes, immutable download, atomic main/dev feeds and unchanged ULike policy')


if __name__ == '__main__':
    main()
