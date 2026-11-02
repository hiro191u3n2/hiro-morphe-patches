#!/usr/bin/env python3
"""Rebuild frozen .124/.336 bytes and execute every reviewed host suite."""
from pathlib import Path
from types import SimpleNamespace
import argparse
import hashlib
import io
import json
import re
import shutil
import subprocess
import urllib.request
import zipfile
import publish19124 as p

TOOLS = {
 'morphe-modern.jar': ('https://github.com/MorpheApp/morphe-desktop/releases/download/v2.0.0-dev.4/morphe-desktop-2.0.0-dev.4-all.jar', None, None, 'ed8184451a88b373ab38e575884523a709706195a7232e971fcf8cd61fc5a22c'),
 'morphe.jar': ('https://github.com/MorpheApp/morphe-desktop/releases/download/v1.16.0/morphe-desktop-1.16.0-all.jar', None, None, '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c'),
 'android.jar': ('https://dl.google.com/android/repository/platform-35_r02.zip', '0988cacad01b38a18a47bac14a0695f246bc76c1b06c0eeb8eb0dc825ab0c8e0', 'android-35/android.jar', '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b'),
 'd8.jar': ('https://dl.google.com/android/repository/build-tools_r35_linux.zip', 'bd3a4966912eb8b30ed0d00b0cda6b6543b949d5ffe00bea54c04c81e1561d88', 'android-15/lib/d8.jar', '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5'),
}


def source_zip(source, target, pin):
    manifest = p.read_json(source / 'manifest.json')['files']
    names = sorted(set(manifest) | {'manifest.json'}, key=Path)
    for name, row in manifest.items():
        p.checked_path(name)
        p.require(row == p.file_row(source / name), 'Source manifest changed: ' + name)
    with zipfile.ZipFile(target, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name in names:
            info = zipfile.ZipInfo(name, (2026, 11, 3, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, (source / name).read_bytes())
    p.require(p.file_row(target) == pin, 'Source ZIP not reproduced')


def run(*args):
    subprocess.run(list(map(str, args)), check=True)


def get(url):
    with urllib.request.urlopen(url, timeout=240) as response:
        return response.read()


def suite_paths(plan, variables):
    result = {}
    for name, entry in sorted(plan.items()):
        p.require(re.fullmatch(r'[a-z][a-z0-9_-]*', name) is not None, 'Invalid suite name')
        p.require(set(entry) == {'command', 'report'} and isinstance(entry['command'], list), 'Invalid CI suite entry')
        command = [argument.format_map(variables) for argument in entry['command']]
        report = Path(entry['report'].format_map(variables)).resolve()
        p.require(report.is_relative_to(Path(variables['work'])), 'Host report outside CI work directory')
        result[name] = {'command': command, 'report': report}
    return result


def build(args, cfg, suites, source, publication, tools, dist):
    p.require(not args.work.exists(), 'Use a fresh CI work directory')
    tools.mkdir(parents=True); dist.mkdir()
    run('python3', publication / 'test_publication19124.py')
    source_zip(source, dist / p.SOURCE_ZIP, cfg['artifacts'][p.SOURCE_ZIP])
    for name, (url, outer, entry, digest) in TOOLS.items():
        raw = get(url)
        if entry:
            p.require(p.sha(raw) == outer, name + ' container changed')
            with zipfile.ZipFile(io.BytesIO(raw)) as z:
                raw = z.read(entry)
        p.require(p.sha(raw) == digest, name + ' changed')
        (tools / name).write_bytes(raw)
    baseline = args.work / 'Hiro_Morphe_Patches_v1.0.335.mpp'
    raw = get(p.BASE_URL); p.require(p.sha(raw) == p.BASE_SHA256, 'Active baseline bytes changed')
    baseline.write_bytes(raw)
    run('python3', source / 'src/build19124.py', '--baseline', source / 'baseline123/ULike_HQ_Texture_Online_v1.9.123.mpp',
        '--tools', tools, '--jdk', args.jdk, '--families', source / 'resource-families19124.json',
        '--work', args.work / 'build', '--output', dist)
    p.require(p.file_row(dist / p.SINGLE) == cfg['artifacts'][p.SINGLE], 'Standalone MPP not reproduced')
    for name, suite in suites.items():
        run(*suite['command'])
        report = p.read_json(suite['report'])
        p.verify_source_hashes(report, 'source_sha256', source / 'src')
        p.require(p.host_summary(report) == cfg['host_suites'][name], 'Fresh host evidence differs: ' + name)
    run('python3', publication / 'build_bundle19124.py', '--baseline', baseline, '--standalone', dist / p.SINGLE,
        '--source', source / 'src', '--tools', tools, '--jdk', args.jdk, '--work', args.work / 'bundle',
        '--output', dist / p.COMBINED, '--standalone-sha256', cfg['artifacts'][p.SINGLE]['sha256'], '--report', args.work / 'bundle-report19124.json')
    p.require(p.file_row(dist / p.COMBINED) == cfg['artifacts'][p.COMBINED], 'Combined MPP not reproduced')
    p.require((args.work / 'bundle-report19124.json').read_bytes() == (publication / 'frozen/bundle-report19124.json').read_bytes(),
              'Fresh combined report differs')
    for name in (p.QA_NAME, 'RELEASE_NOTES.txt', 'SHA256SUMS.txt'):
        shutil.copyfile(publication / 'frozen' / name, dist / name)
    print('PASS reproduced all frozen artifacts and reran all reviewed host/failed-condition suites')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--release', type=Path, required=True)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path, required=True)
    parser.add_argument('--repo', type=Path, default=Path.cwd())
    parser.add_argument('--stage', choices=('build', 'verify', 'preflight', 'publish'), default='build')
    args = parser.parse_args()
    args.release, args.work, args.jdk, args.repo = (x.resolve() for x in (args.release, args.work, args.jdk, args.repo))
    source, publication = args.release / 'source', args.release / 'publication'
    tools, dist = args.work / 'tools', args.work / 'dist'
    manifest = publication / 'manifest19124.json'
    cfg = p.read_json(manifest)
    p.require(cfg.get('schema') == 'ulike19124-publication-v1' and cfg.get('ready') is True, 'Publication not frozen')
    p.require(cfg['source_files'] == p.source_files(args.repo), 'Reviewed source/publication code changed')
    plan = p.read_json(publication / 'ci-suites19124.json')
    p.require(set(plan) == set(cfg['host_suites']), 'CI suite plan differs from frozen evidence')
    variables = {k: str(v) for k, v in {'source': source, 'work': args.work, 'jdk': args.jdk, 'tools': tools, 'dist': dist}.items()}
    suites = suite_paths(plan, variables)
    if args.stage == 'build':
        build(args, cfg, suites, source, publication, tools, dist)
        return
    request = SimpleNamespace(repo=args.repo, dist=dist, source=source,
        baseline=args.work / 'Hiro_Morphe_Patches_v1.0.335.mpp', bundle_report=args.work / 'bundle-report19124.json',
        combined_application=publication / 'frozen/combined-application19124.json', build_report=dist / 'build19124.json',
        host_reports={n: row['report'] for n, row in suites.items()}, manifest=manifest,
        local_only=args.stage == 'verify', preflight_only=args.stage == 'preflight')
    p.publication(request)


if __name__ == '__main__':
    main()
