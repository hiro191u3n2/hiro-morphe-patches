#!/usr/bin/env python3
"""Download checksum-pinned public toolchains and the unchanged bundle baseline."""
import argparse
import concurrent.futures
import hashlib
import io
import json
import lzma
from pathlib import Path
import tarfile
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parent
BASE_URL = 'https://hiro-morphe-patches.otoha10.chatgpt.site/Hiro_Morphe_Patches_v1.0.371.mpp'
BASE_SHA = '996206eb4ea224a254dd49cbda83ed6b3566a0f0b322de60f05bb4aa15b1744d'


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--destination', type=Path, required=True)
    a = p.parse_args()
    a.destination.mkdir(parents=True, exist_ok=True)
    rows = json.loads((ROOT / 'tool-sources.json').read_text())
    urls = {v['url'] for v in rows.values()} | {BASE_URL}
    def download(url):
        request = urllib.request.Request(url, headers={'User-Agent': 'Hiro-YahooTransit126-Build'})
        with urllib.request.urlopen(request, timeout=180) as response:
            assert response.status == 200
            raw = response.read()
        print('Downloaded ' + url.rsplit('/', 1)[-1] + ' (' + str(len(raw)) + ' bytes)', flush=True)
        return url, raw
    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
        payloads = dict(pool.map(download, sorted(urls)))
    for name, row in rows.items():
        raw = payloads[row['url']]
        if 'archive_sha256' in row:
            assert sha(raw) == row['archive_sha256'], 'Wrong tool archive: ' + name
        if 'entry' in row:
            with zipfile.ZipFile(io.BytesIO(raw)) as z:
                raw = z.read(row['entry'])
        assert sha(raw) == row['sha256'], 'Wrong tool bytes: ' + name
        dest = a.destination / name
        dest.write_bytes(raw)
        if row.get('executable'):
            dest.chmod(0o755)
    assert sha(payloads[BASE_URL]) == BASE_SHA
    (a.destination / 'Hiro_Morphe_Patches_v1.0.371.mpp').write_bytes(payloads[BASE_URL])
    with tarfile.open(a.destination / 'jdk21.tar.gz') as archive:
        archive.extractall(a.destination, filter='data')
    with zipfile.ZipFile(a.destination / 'manager250.apk') as archive:
        name = 'assets/jvm/patcher-host.jar.xz'
        (a.destination / 'manager250-host.jar').write_bytes(lzma.decompress(archive.read(name)))
        frameworks = [n for n in archive.namelist() if n.startswith('frameworks/') and n.endswith('.apk')]
        with zipfile.ZipFile(a.destination / 'manager250-frameworks.jar', 'w') as result:
            for entry in sorted(frameworks):
                result.writestr(entry, archive.read(entry))
    print('PASS pinned baseline and tools staged at ' + str(a.destination.resolve()))


if __name__ == '__main__':
    main()
