#!/usr/bin/env python3
"""Recreate the delivered artifacts and rerun quiet-runtime checks before publish."""
from pathlib import Path
import argparse
import hashlib
import io
import json
import shutil
import subprocess
import urllib.request
import zipfile

SINGLE = 'ULike_HQ_Texture_Online_v1.9.120.mpp'
SOURCE_ZIP = 'ULike_v1.9.120_sources_and_QA.zip'
COMBINED = 'Hiro_Morphe_Patches_v1.0.326.mpp'
SINGLE_SHA = 'f779c09136554c9b02c81971eeee8caa334fd6d0263ddda2c974beab70f67851'
SOURCE_SHA = '48989a191e1836624d83880801484b70f825a910d7bd04ebbb6ef2ed08c9820e'
COMBINED_SHA = 'bdba2dd67621b5f5bd3c1d6780b383e063866f153e6e30163160365dffe0d9dd'
BASE_URL = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/bb15cbd914f52cf62c56c36db2f9355eb4d05b38/downloads/Hiro_Morphe_Patches_v1.0.325.mpp'
BASE_SHA = '4b6068855a20f4350114748a952c2b0bb97342df31551ee5e8f49dafba1dfe09'
TOOLS = {
 'morphe.jar': ('https://github.com/MorpheApp/morphe-desktop/releases/download/v1.16.0/morphe-desktop-1.16.0-all.jar', None, None, '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c'),
 'android.jar': ('https://dl.google.com/android/repository/platform-35_r02.zip', '0988cacad01b38a18a47bac14a0695f246bc76c1b06c0eeb8eb0dc825ab0c8e0', 'android-35/android.jar', '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b'),
 'd8.jar': ('https://dl.google.com/android/repository/build-tools_r35_linux.zip', 'bd3a4966912eb8b30ed0d00b0cda6b6543b949d5ffe00bea54c04c81e1561d88', 'android-15/lib/d8.jar', '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5'),
}

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def run(*args):
    subprocess.run(list(map(str, args)), check=True)

def get(url):
    with urllib.request.urlopen(url, timeout=240) as response:
        return response.read()

def source_zip(source, target):
    manifest = json.loads((source / 'manifest.json').read_text())['files']
    # Original packaging sorted pathlib Paths (component order), not strings.
    names = sorted(set(manifest) | {'manifest.json'}, key=Path)
    for name, row in manifest.items():
        path = Path(name)
        assert not path.is_absolute() and '..' not in path.parts
        raw = (source / name).read_bytes()
        assert row == {'bytes': len(raw), 'sha256': sha(raw)}, name
    with zipfile.ZipFile(target, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name in names:
            info = zipfile.ZipInfo(name, (2026, 11, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, (source / name).read_bytes())
    assert sha(target.read_bytes()) == SOURCE_SHA, 'Delivered source archive not reproduced'

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--release', type=Path, required=True)
    p.add_argument('--work', type=Path, required=True)
    p.add_argument('--jdk', type=Path, required=True)
    a = p.parse_args()
    release, work, jdk = a.release.resolve(), a.work.resolve(), a.jdk.resolve()
    assert not work.exists(), 'Use a fresh CI work directory'
    source, publication = release / 'source', release / 'publication'
    tools, dist = work / 'tools', work / 'dist'
    tools.mkdir(parents=True); dist.mkdir()
    source_zip(source, dist / SOURCE_ZIP)
    for name, (url, outer, entry, digest) in TOOLS.items():
        raw = get(url)
        if entry:
            assert sha(raw) == outer, name + ' container changed'
            with zipfile.ZipFile(io.BytesIO(raw)) as z:
                raw = z.read(entry)
        assert sha(raw) == digest, name + ' changed'
        (tools / name).write_bytes(raw)
    baseline = work / 'Hiro_Morphe_Patches_v1.0.325.mpp'
    raw = get(BASE_URL); assert sha(raw) == BASE_SHA
    baseline.write_bytes(raw)
    run('python3', source / 'src/build19120.py', '--baseline', source / 'baseline119/ULike_HQ_Texture_Online_v1.9.119.mpp',
        '--tools', tools, '--jdk', jdk, '--families', source / 'quiet-families19120.json', '--work', work / 'build', '--output', dist)
    assert sha((dist / SINGLE).read_bytes()) == SINGLE_SHA
    run('python3', source / 'src/host_quiet_runtime19120.py', '--source', source / 'src', '--work', work / 'host-quiet', '--jdk', jdk)
    run('python3', source / 'src/host_cached_restore19120.py', '--classes', work / 'build/helper-classes', '--android', tools / 'android.jar',
        '--work', work / 'host-cached', '--java', jdk / 'bin/java', '--javac', jdk / 'bin/javac')
    run('python3', publication / 'build_bundle19120.py', '--baseline', baseline, '--standalone', dist / SINGLE,
        '--source', source / 'src', '--tools', tools, '--jdk', jdk, '--work', work / 'bundle',
        '--output', dist / COMBINED, '--report', work / 'bundle-report19120.json')
    assert sha((dist / COMBINED).read_bytes()) == COMBINED_SHA
    assert (work / 'bundle-report19120.json').read_bytes() == (publication / 'frozen/bundle-report19120.json').read_bytes()
    for name in ('QA_ULike_v1.9.120.json', 'RELEASE_NOTES.txt', 'SHA256SUMS.txt'):
        shutil.copyfile(publication / 'frozen' / name, dist / name)
    print('PASS identical standalone/source/combined artifacts; fresh 192 host assertions and 5 negative controls')

if __name__ == '__main__':
    main()
