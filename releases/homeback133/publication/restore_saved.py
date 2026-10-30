#!/usr/bin/env python3
"""Restore exact saved317 bytes from a verified316 binary and a Git binary delta.

This applies an artifact delta. It does not compile, rebuild, or recompress MPP
contents. Both input and final bytes must match their reviewed SHA-256 values.
"""
import argparse
import hashlib
from pathlib import Path
import shutil
import subprocess
import tempfile
import urllib.request

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
NAME = 'downloads/Hiro_Morphe_Patches_v1.0.317.mpp'
BASE_URL = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/b4000c37b3d162f3f26daaefd3d0ee1966e42f51/downloads/Hiro_Morphe_Patches_v1.0.316.mpp'
BASE_SHA = 'd45359b07529e3c1bcbebfe795befeeae8ebe53e5794725c2c06750e996e06ea'
SAVED_SHA = '4f190bc54c91effbfa778b62bf913e2b6509f82c8e8b4f1b99b5a59ccae3a466'

def require(ok, message):
    if not ok:
        raise RuntimeError(message)

def digest(raw):
    return hashlib.sha256(raw).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base', type=Path)
    parser.add_argument('--output', type=Path, default=REPO/NAME)
    args = parser.parse_args()
    if args.base:
        base = args.base.read_bytes()
    else:
        request = urllib.request.Request(BASE_URL, headers={'User-Agent': 'Hiro-HomeBack133-Artifact-Recovery'})
        with urllib.request.urlopen(request, timeout=90) as response:
            require(response.status == 200, 'Baseline download failed')
            base = response.read()
    require(len(base) == 47985964 and digest(base) == BASE_SHA, 'Immutable316 bytes differ')
    with tempfile.TemporaryDirectory(prefix='saved317-') as temporary:
        root = Path(temporary)
        target = root/NAME
        target.parent.mkdir()
        target.write_bytes(base)
        subprocess.run(['git', 'init', '-q', str(root)], check=True)
        subprocess.run(['git', '-C', str(root), 'apply', '--binary', str(HERE/'saved317.patch')], check=True)
        raw = target.read_bytes()
        require(len(raw) == 48001679 and digest(raw) == SAVED_SHA, 'Restored saved317 identity differs')
        args.output.parent.mkdir(parents=True, exist_ok=True)
        if args.output.exists():
            require(args.output.read_bytes() == raw, 'Refusing to replace different existing output')
        else:
            shutil.copyfile(target, args.output)
    print('PASS restored exact previously verified317: '+SAVED_SHA)

if __name__ == '__main__':
    main()
