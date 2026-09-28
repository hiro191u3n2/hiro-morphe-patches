#!/usr/bin/env python3
"""Publish a delivery-only v1.0.82 after testing the public download path.

Does not modify Manager or any application patch code. The old hosted site is
not controlled by this script. Users of that endpoint must migrate explicitly.
"""
import base64
import concurrent.futures
import copy
import datetime
import hashlib
import json
import pathlib
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import zipfile

REPO = 'hiro191u3n2/hiro-morphe-patches'
VERSION = '1.0.82'
SOURCE_SHA = '690c5b7903b50c8ab2fe143704c8973e78365b4463e550e3b7a8347fbe78006d'
ROOT = pathlib.Path.cwd()
NAME = 'Hiro_Morphe_Patches_v1.0.82.mpp'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def run(*args, input=None):
    return subprocess.run(args, input=input, text=True, check=True,
                          stdout=subprocess.PIPE).stdout.strip()


def get(url, method='GET', byte_range=None, attempts=4):
    headers = {'User-Agent': 'Hiro-Patch-Delivery-QA/1.0',
               'Accept-Encoding': 'identity', 'Cache-Control': 'no-cache'}
    if byte_range is not None:
        headers['Range'] = 'bytes=%d-%d' % byte_range
    for attempt in range(attempts):
        try:
            request = urllib.request.Request(url, headers=headers, method=method)
            with urllib.request.urlopen(request, timeout=35) as response:
                data = response.read()
                result = (response.status, dict(response.headers.items()), data)
            return result
        except (OSError, urllib.error.URLError):
            if attempt + 1 == attempts:
                raise
            time.sleep(2 ** attempt)


def header(headers, name):
    return next((v for k, v in headers.items() if k.lower() == name.lower()), None)


def repack(source, destination):
    assert sha(source.read_bytes()) == SOURCE_SHA, 'Unexpected input release bytes'
    with zipfile.ZipFile(source) as old:
        assert old.testzip() is None
        names = old.namelist()
        assert len(names) == len(set(names)) == 35
        manifest = old.read('META-INF/MANIFEST.MF')
        needle = b'\r\nVersion: 1.0.81\r\n'
        assert manifest.count(needle) == 1
        replacement = manifest.replace(needle, b'\r\nVersion: 1.0.82\r\n')
        with zipfile.ZipFile(destination, 'w', compression=zipfile.ZIP_DEFLATED,
                             compresslevel=9) as new:
            for info in old.infolist():
                data = replacement if info.filename == 'META-INF/MANIFEST.MF' else old.read(info.filename)
                new.writestr(copy.copy(info), data, compress_type=info.compress_type, compresslevel=9)
        with zipfile.ZipFile(destination) as new:
            assert new.testzip() is None
            assert new.namelist() == names
            unchanged = [n for n in names if old.read(n) == new.read(n)]
            assert len(unchanged) == 34
            assert new.read('META-INF/MANIFEST.MF') == replacement
    return {'source_version': '1.0.81', 'version': VERSION,
            'source_sha256': SOURCE_SHA, 'sha256': sha(destination.read_bytes()),
            'bytes': destination.stat().st_size, 'entries': 35,
            'byte_identical_entries': 34, 'zip_crc_valid': True,
            'only_changed_entry': 'META-INF/MANIFEST.MF',
            'dex_and_patch_resources_byte_identical': True,
            'manager_code_changed': False, 'physical_device_tested': False}


def verify_delivery(url, expected):
    status, headers, whole = get(url)
    assert status == 200 and whole == expected, 'Public full download mismatch'
    full_length = header(headers, 'Content-Length')
    if full_length is not None:
        assert int(full_length) == len(expected)
    head_status, head_headers, _ = get(url, method='HEAD')
    assert head_status == 200
    if header(head_headers, 'Content-Length') is not None:
        assert int(header(head_headers, 'Content-Length')) == len(expected)
    size = len(expected)

    def part(bounds):
        start, end = bounds
        status, headers, body = get(url, byte_range=bounds)
        assert status == 206, 'Server did not return Partial Content'
        assert header(headers, 'Content-Range') == f'bytes {start}-{end}/{size}'
        assert body == expected[start:end + 1], 'Range bytes differ'
        return body

    # Match the Manager's HEAD/range probe and four concurrent download pattern.
    assert part((0, 0)) == expected[:1]
    ranges = [(i * size // 4, (i + 1) * size // 4 - 1) for i in range(4)]
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        combined = b''.join(pool.map(part, ranges))
    assert combined == expected
    # Exercise a fresh request starting beyond the reported ~10.9 MB stall point.
    resume_at = 11 * 1024 * 1024
    resumed = part((0, resume_at - 1)) + part((resume_at, size - 1))
    assert resumed == expected
    return {'url': url, 'full_get_status': 200, 'head_status': 200,
            'full_download_sha256': sha(whole), 'range_status': 206,
            'four_parallel_ranges_verified': True,
            'four_part_sha256': sha(combined), 'resume_offset_bytes': resume_at,
            'resumed_download_sha256': sha(resumed),
            'tested_from': 'GitHub Actions ubuntu-latest; not the user device/network'}


def main():
    metadata_path = ROOT / 'patches-bundle.json'
    original = json.loads(metadata_path.read_text())
    assert original['version'] == '1.0.81', 'Refuse to overwrite another/newer release'
    required_url = f'https://github.com/{REPO}/releases/download/ulike-v1.4.4/Hiro_Morphe_Patches_v1.0.81.mpp'
    assert original['download_url'] == required_url
    old_site = {'status': 'unverified'}
    try:
        status, _, data = get('https://hiro-morphe-patches.otoha10.chatgpt.site/patches-bundle.json', attempts=1)
        info = json.loads(data)
        old_site = {'http_status': status, 'version': info.get('version'),
                    'download_url': info.get('download_url'), 'modified': False}
    except Exception as error:
        old_site = {'status': 'unreachable_from_runner', 'error_type': type(error).__name__, 'modified': False}

    destination = ROOT / 'downloads' / NAME
    destination.parent.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory() as temporary:
        source = pathlib.Path(temporary) / 'original.mpp'
        source.write_bytes(get(required_url)[2])
        report = repack(source, destination)
    report['old_hosted_source'] = old_site
    report['change'] = 'Delivery only. Only the manifest version is changed; application patches remain byte-identical.'
    run('git', 'config', 'user.name', 'github-actions[bot]')
    run('git', 'config', 'user.email', '41898282+github-actions[bot]@users.noreply.github.com')
    run('git', 'add', str(destination.relative_to(ROOT)))
    run('git', 'commit', '-m', 'Publish v1.0.82 bundle for immutable direct delivery')
    run('git', 'push', 'origin', 'HEAD:main')
    binary_commit = run('git', 'rev-parse', 'HEAD')
    url = f'https://raw.githubusercontent.com/{REPO}/{binary_commit}/downloads/{NAME}'
    # Do not switch any source unless all public transfer tests pass.
    report['delivery'] = verify_delivery(url, destination.read_bytes())
    report['verified_at_utc'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
    qa_path = ROOT / 'releases' / 'delivery-v1.0.82-qa.json'
    qa_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    note = ('【統合MPP v1.0.82：パッチ更新用の配布経路を修正】\n'
            '配布ファイルをGitHubのコミット固定Raw URLへ変更。全体取得・4分割同時取得・11MiB地点からの再開相当取得を配布元で検証しました。'
            'ULike v1.4.4と既存の他アプリの改造内容は維持します。MPP内部はVersion以外の34項目がv1.0.81とバイト単位で同一です。'
            'Manager本体のコード修正ではなく、実機・利用回線での停止解消は未確認です。'
            '旧chatgpt.siteの配信元は更新していません。mainまたはdevのpatches-bundle.jsonを配信元として使用してください。\n\n')
    metadata = dict(original)
    metadata.update(version=VERSION, download_url=url,
                    created_at=datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%S'),
                    description=note + original.get('description', ''))
    metadata_path.write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n')
    changelog = ROOT / 'CHANGELOG.md'
    changelog.write_text('# 1.0.82 (2026-09-28)\n\n' + note.replace('【統合MPP v1.0.82：パッチ更新用の配布経路を修正】\n', '') + changelog.read_text())
    run('gh', 'release', 'create', 'delivery-v1.0.82', str(destination), str(qa_path),
        '--repo', REPO, '--title', 'Integrated patches v1.0.82 - direct delivery',
        '--notes', note)
    run('git', 'add', 'patches-bundle.json', 'CHANGELOG.md', str(qa_path.relative_to(ROOT)))
    run('git', 'commit', '-m', 'Switch stable patch source after full, parallel and resume transfer QA')
    run('git', 'push', 'origin', 'HEAD:main')
    # Update the existing development source with an optimistic content-SHA check.
    item = json.loads(run('gh', 'api', f'repos/{REPO}/contents/patches-bundle.json?ref=dev'))
    dev_metadata = json.loads(base64.b64decode(item['content']))
    assert dev_metadata['version'] == '1.0.81', 'Development source changed concurrently; do not overwrite'
    payload = {'message': 'Sync verified direct-delivery source v1.0.82', 'branch': 'dev',
               'sha': item['sha'], 'content': base64.b64encode(metadata_path.read_bytes()).decode()}
    run('gh', 'api', '--method', 'PUT', f'repos/{REPO}/contents/patches-bundle.json',
        '--input', '-', input=json.dumps(payload))
    print(json.dumps(report, ensure_ascii=False, indent=2))
    print('PUBLISHED_MAIN_AND_DEV', VERSION)


if __name__ == '__main__':
    main()
