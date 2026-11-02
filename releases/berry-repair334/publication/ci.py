#!/usr/bin/env python3
"""Rebuild and test the runtime and providers twice; bind application evidence."""
from pathlib import Path
import argparse
import shutil
import subprocess
import sys

import publish as p


def build(repo, work, java):
    cfg = p.config(repo)
    root = repo / p.RELEASE_ROOT
    p.require(not work.exists() or not any(work.iterdir()), 'CI work directory must be empty')
    tools, dist = work / 'tools', work / 'dist'
    tools.mkdir(parents=True, exist_ok=True)
    dist.mkdir(parents=True, exist_ok=True)
    base = work / 'baseline333.mpp'
    raw = p.fetch(p.BASE_URL)
    p.require(p.sha(raw) == p.BASE_SHA, 'Public baseline changed')
    base.write_bytes(raw)
    for name in ('classpath', 'ecj', 'd8'):
        row = cfg['tools'][name]
        raw = p.fetch(row['url'])
        p.require(p.sha(raw) == row['sha256'], 'Build tool bytes differ: ' + name)
        (tools / (name + '.jar')).write_bytes(raw)
    runtime_outputs = []
    for variant in ('runtime-a', 'runtime-b'):
        output = work / variant
        subprocess.run([sys.executable, str(root / 'runtime/build_runtime.py'),
                        '--java', str(java), '--ecj', str(tools / 'ecj.jar'),
                        '--d8', str(tools / 'd8.jar'), '--output-dir', str(output)], check=True)
        extension = output / 'extensions/berry_bfcache_restore.mpe'
        report = output / 'RUNTIME_BUILD_QA.json'
        p.require(p.file_row(extension) == cfg['runtime_extension'],
                  'Runtime build differs from reviewed extension')
        p.require(p.file_row(report) == cfg['runtime_qa'],
                  'Runtime tests differ from reviewed evidence')
        runtime_outputs.append(extension)
    p.require(runtime_outputs[0].read_bytes() == runtime_outputs[1].read_bytes(),
              'Two runtime builds differ')
    for variant, runtime in zip(('build-a', 'build-b'), runtime_outputs):
        subprocess.run([sys.executable, str(root / 'build/build.py'),
                        '--base', str(base), '--src', str(root / 'src'),
                        '--runtime', str(runtime),
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
    print('PASS two clean runtime/provider builds, all runtime suites, and hash-bound application/native guard evidence', flush=True)


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
