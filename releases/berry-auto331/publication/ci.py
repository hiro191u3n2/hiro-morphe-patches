#!/usr/bin/env python3
"""Rebuild the reviewed providers twice; bind original/synthetic application evidence."""
from pathlib import Path
import argparse
import shutil
import subprocess
import sys

import publish as p


def build(repo, work, java):
    cfg = p.config(repo)
    root = repo / p.RELEASE_ROOT
    tools, dist = work / 'tools', work / 'dist'
    tools.mkdir(parents=True, exist_ok=True)
    dist.mkdir(parents=True, exist_ok=True)
    base = work / 'baseline330.mpp'
    raw = p.fetch(p.BASE_URL)
    p.require(p.sha(raw) == p.BASE_SHA, 'Public baseline changed')
    base.write_bytes(raw)
    for name in ('classpath', 'ecj', 'd8'):
        row = cfg['tools'][name]
        raw = p.fetch(row['url'])
        p.require(p.sha(raw) == row['sha256'], 'Build tool bytes differ: ' + name)
        (tools / (name + '.jar')).write_bytes(raw)
    for variant in ('build-a', 'build-b'):
        subprocess.run([sys.executable, str(root / 'build/build.py'),
                        '--base', str(base), '--src', str(root / 'src'),
                        '--out', str(work / variant), '--java', str(java),
                        '--classpath', str(tools / 'classpath.jar'),
                        '--ecj', str(tools / 'ecj.jar'), '--d8', str(tools / 'd8.jar')], check=True)
    first, second = work / 'build-a', work / 'build-b'
    for name in (p.COMBINED, p.SINGLE, p.BUILD_QA):
        p.require((first / name).read_bytes() == (second / name).read_bytes(),
                  'Two builds differ: ' + name)
        p.require(p.file_row(first / name) == cfg['artifacts'][name],
                  'CI build differs from reviewed artifact: ' + name)
        shutil.copyfile(first / name, dist / name)
    # The original APK is a private test input. CI binds the already completed,
    # candidate-specific local application evidence without publishing that APK.
    for name in (p.APPLY_QA, 'RELEASE_NOTES.txt'):
        shutil.copyfile(root / 'payload' / name, dist / name)
    sums = ''.join(cfg['artifacts'][name]['sha256'] + '  ' + name + '\n'
                   for name in sorted(cfg['artifacts']) if name != 'SHA256SUMS.txt')
    (dist / 'SHA256SUMS.txt').write_text(sums)
    p.verify(repo, dist)
    print('PASS two clean provider builds and hash-bound application evidence', flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, required=True)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--java', type=Path, required=True)
    parser.add_argument('--stage', choices=('build', 'verify', 'publish'), required=True)
    args = parser.parse_args()
    repo, work, java = args.repo.resolve(), args.work.resolve(), args.java.resolve()
    if args.stage == 'build':
        build(repo, work, java)
    elif args.stage == 'verify':
        p.verify(repo, work / 'dist')
        print('PASS publication artifact verification')
    else:
        p.publish(repo, work / 'dist')
