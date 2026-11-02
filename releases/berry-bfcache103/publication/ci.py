#!/usr/bin/env python3
"""Rebuild the merged provider DEX twice and publish only frozen matching bytes."""
from pathlib import Path
import argparse
import json
import shutil
import subprocess
import sys

import publish as p


def build(repo, work, java):
    cfg = p.config(repo)
    root = repo / p.RELEASE_ROOT
    tools, dist = work / 'tools', work / 'dist'
    tools.mkdir(parents=True, exist_ok=True); dist.mkdir(parents=True, exist_ok=True)
    baseline = work / 'baseline328.mpp'
    raw = p.fetch(p.BASE_URL); p.require(p.sha(raw) == p.BASE_SHA, 'Public baseline changed')
    baseline.write_bytes(raw)
    for name in ('r8', 'classpath', 'ecj'):
        row = cfg['tools'][name]
        raw = p.fetch(row['url'])
        p.require(p.sha(raw) == row['sha256'], 'Build tool bytes differ: ' + name)
        (tools / (name + '.jar')).write_bytes(raw)
    for name in cfg['artifacts']:
        if name not in (p.COMBINED, 'SHA256SUMS.txt'):
            shutil.copyfile(root / 'payload' / name, dist / name)
    for variant in ('build-a', 'build-b'):
        subprocess.run([sys.executable, str(root / 'build/build_merge.py'), '--base', str(baseline),
            '--addition', str(dist / p.SINGLE), '--java', str(java), '--d8', str(tools / 'r8.jar'),
            '--ecj', str(tools / 'ecj.jar'),
            '--classpath', str(tools / 'classpath.jar'), '--out', str(work / variant)], check=True)
    first, second = work / 'build-a', work / 'build-b'
    for name in (p.COMBINED, 'QA_bundle_merge.json'):
        p.require((first / name).read_bytes() == (second / name).read_bytes(), 'Two builds differ: ' + name)
        shutil.copyfile(first / name, dist / name)
    p.require(p.file_row(dist / p.COMBINED) == cfg['artifacts'][p.COMBINED], 'CI build differs from reviewed combined bytes')
    sums = ''.join(cfg['artifacts'][name]['sha256'] + '  ' + name + '\n'
                   for name in sorted(cfg['artifacts']) if name != 'SHA256SUMS.txt')
    (dist / 'SHA256SUMS.txt').write_text(sums)
    p.verify(repo, dist)
    print('PASS two clean merges and frozen public payloads', flush=True)


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
        p.verify(repo, work / 'dist'); print('PASS publication artifact verification')
    else:
        p.publish(repo, work / 'dist')
