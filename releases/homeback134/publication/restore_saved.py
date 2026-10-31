#!/usr/bin/env python3
"""Restore exact verified318 bytes from immutable317 and a reviewed Git binary delta."""
import argparse
import hashlib
import json
import shutil
import subprocess
import tempfile
import urllib.request
from pathlib import Path
from check import pins, require

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
NAME = 'downloads/Hiro_Morphe_Patches_v1.0.318.mpp'
BASE_URL = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/7f297d5ff7cde380cb4cffb3dc3ea32bd3b199dd/downloads/Hiro_Morphe_Patches_v1.0.317.mpp'
BASE_SHA = '4f190bc54c91effbfa778b62bf913e2b6509f82c8e8b4f1b99b5a59ccae3a466'


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base', type=Path)
    parser.add_argument('--output', type=Path, default=REPO/NAME)
    args = parser.parse_args()
    expected = pins()['bundle']
    delta = HERE/'saved318.patch'
    require(delta.is_file() and not delta.is_symlink(), 'Final reviewed binary delta missing')
    if args.base:
        base = args.base.read_bytes()
    else:
        request = urllib.request.Request(BASE_URL, headers={'User-Agent':'Hiro-HomeBack134-Artifact-Restore'})
        with urllib.request.urlopen(request, timeout=90) as response:
            require(response.status == 200, 'Baseline download failed')
            base = response.read()
    require(len(base) == 48001679 and digest(base) == BASE_SHA, 'Immutable317 bytes differ')
    with tempfile.TemporaryDirectory(prefix='saved318-') as temporary:
        root = Path(temporary)
        target = root/NAME
        target.parent.mkdir()
        target.write_bytes(base)
        subprocess.run(['git','init','-q',str(root)],check=True)
        subprocess.run(['git','-C',str(root),'apply','--binary',str(delta)],check=True)
        raw = target.read_bytes()
        require(len(raw) == expected['bytes'] and digest(raw) == expected['sha256'], 'Restored318 identity differs')
        args.output.parent.mkdir(parents=True,exist_ok=True)
        if args.output.exists():
            require(args.output.read_bytes() == raw, 'Refusing to replace different existing output')
        else:
            shutil.copyfile(target,args.output)
    print('PASS restored exact verified318: '+expected['sha256'])


if __name__ == '__main__':
    main()
