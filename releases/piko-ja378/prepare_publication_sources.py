#!/usr/bin/env python3
"""Package reviewed PikoJA378 source/QA only after final validation is ready.

This script does not publish, change feed metadata or include application/tool
binaries. It copies the exact reviewed evidence without rewriting its contents.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import shutil
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parent
README = 'README_Piko_NewX_Japanese_v1.0.0.md'
BUILD_QA = 'QA_build378.json'
APP_QA = 'QA_Piko_NewX_Japanese_v1.0.0.json'
ZIP_NAME = 'Piko_NewX_Japanese_v1.0.0_sources_and_QA.zip'
CASES = {'dev1', 'dev2', 'dev2-clone', 'standalone'}
FORBIDDEN = {'.apk', '.apkm', '.apks', '.dex', '.mpe', '.mpp', '.jar', '.class', '.pyc', '.jks', '.keystore', '.bin'}
TEXT_SUFFIXES = {'.py', '.java', '.json', '.xml', '.md', '.txt', '.tsv', '.log', '.ts', '.mjs'}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--site', type=Path, required=True, help='Existing Site checkout root')
    parser.add_argument('--source-root', type=Path, default=ROOT)
    parser.add_argument('--validation', type=Path, help='Defaults to source-root/verification/final-validation.json')
    parser.add_argument('--build-evidence', type=Path, help='Defaults to source-root/dist/build-evidence.json')
    args = parser.parse_args()
    source_root = args.source_root.resolve()
    site = args.site.resolve()
    validation_path = args.validation or source_root/'verification/final-validation.json'
    build_path = args.build_evidence or source_root/'dist/build-evidence.json'
    require((site/'public').is_dir() and (site/'scripts').is_dir(), 'Expected an existing Site checkout')
    app_raw, build_raw = validation_path.read_bytes(), build_path.read_bytes()
    app, build = json.loads(app_raw), json.loads(build_raw)
    require(app.get('result') == 'PASS' and app.get('device_tested') is False,
            'Final application audit must be PASS and explicitly report device_tested=false')
    require(set(app.get('application_tests', {})) == CASES,
            'Final application audit must include all four application cases')
    require(all(x.get('result') == 'PASS' for x in app['application_tests'].values()),
            'Application case is not PASS')
    compiled = app.get('compiled_resources', {})
    require(compiled.get('result') == 'PASS' and set(compiled.get('cases', {})) == CASES,
            'Completed APK resource audit is missing')
    require(all(x.get('result') == 'PASS' and not x.get('failures') for x in compiled['cases'].values()),
            'Completed APK resource case is not PASS')
    require(build.get('schema') == 'hiro-piko-newx-japanese-build-v1' and build.get('result') == 'PASS',
            'Build evidence is not the validated PikoJA378 report')
    require(build.get('physical_device_tested') is False and build.get('emulator_tested') is False,
            'Build evidence runtime scope differs')
    host = json.loads((source_root/'patch/QA.json').read_bytes())
    require(host.get('result') == 'PASS' and host.get('component_sha256') == build.get('component_sha256'),
            'Component QA differs from final build evidence')
    for name, expected in build['artifacts'].items():
        raw = (source_root/'dist'/name).read_bytes()
        require({'bytes': len(raw), 'sha256': sha(raw)} == expected, 'Final artifact identity differs: '+name)

    files = {}

    def include(src, dest=None):
        src = Path(src)
        require(src.is_file() and not src.is_symlink(), 'Missing/linked input: '+str(src))
        name = dest or src.relative_to(source_root).as_posix()
        require(not name.startswith('/') and '..' not in Path(name).parts and '\\' not in name,
                'Unsafe source path: '+name)
        require(Path(name).suffix.lower() not in FORBIDDEN, 'Binary/application file prohibited: '+name)
        raw = src.read_bytes()
        require(len(raw) <= 16*1024*1024, 'Unexpectedly large source/evidence: '+name)
        raw.decode('utf-8')
        require(name not in files or files[name] == raw, 'Conflicting source entry: '+name)
        files[name] = raw

    for name in ('README.md', 'toolchain.json', 'prepare_publication_sources.py', 'piko-dev2-selected.json'):
        include(source_root/name)
    for directory in ('patch', 'build', 'translations', 'verification', 'review'):
        for path in sorted((source_root/directory).iterdir()):
            if path.is_file() and path.suffix in TEXT_SUFFIXES:
                include(path)
    for name in ('resource-inventory.json', 'runtime-localization-scope.json',
                 'hardcoded-ui-candidates.json', 'shared-settings-english.txt'):
        include(source_root/'inventory'/name)
    for name in ('ConstStrings.java', 'build-core-map.py'):
        include(source_root/'inventory/dexscan'/name)
    for name in ('LICENSE', 'NOTICE'):
        include(source_root/'inventory/upstream-license'/name)
        include(source_root/'inventory/upstream-license'/name, 'upstream/'+name)
    for path in sorted((source_root/'delivery').rglob('*')):
        if path.is_file() and path.suffix in TEXT_SUFFIXES:
            include(path)
    # The standalone package builder regenerates these sources/resources, but
    # recording their reviewed versions makes source-to-MPP checks transparent.
    include(source_root/'patch/build/NewXJapaneseData.java')
    for path in sorted((source_root/'patch/build/resources').rglob('*.xml')):
        include(path)
    files['verification/final-validation.json'] = files[APP_QA] = app_raw
    files['verification/build-evidence.json'] = files[BUILD_QA] = build_raw
    files[README] = files['README.md']
    for case in sorted(CASES):
        for suffix in ('-result.json', '-resources.json', '-localized-dex.json'):
            require('verification/'+case+suffix in files, 'Missing completed APK evidence: '+case+suffix)
    preservation = 'verification/dev1-preservation.json'
    require(preservation in files and json.loads(files[preservation]).get('result') == 'PASS',
            'Completed dev.1 APK preservation audit is missing or not PASS')
    require(sum(map(len, files.values())) <= 32*1024*1024, 'Source archive unexpectedly exceeds 32 MiB')

    destination = site/'scripts/piko-ja378/source'
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = Path(tempfile.mkdtemp(prefix='piko-ja378-source-', dir=destination.parent))
    try:
        for name, raw in sorted(files.items()):
            path = temporary/name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(raw)
        # Preflight is complete before replacing only this release's source tree.
        if destination.exists():
            shutil.rmtree(destination)
        temporary.rename(destination)
    finally:
        if temporary.exists():
            shutil.rmtree(temporary)
    public = site/'public'
    for name in (README, BUILD_QA, APP_QA):
        (public/name).write_bytes(files[name])
    archive_path = public/ZIP_NAME
    with zipfile.ZipFile(archive_path, 'w') as archive:
        for name, raw in sorted(files.items()):
            info = zipfile.ZipInfo(name, (2026, 11, 17, 0, 0, 0))
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            archive.writestr(info, raw, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)
    with zipfile.ZipFile(archive_path) as archive:
        require(archive.testzip() is None and set(archive.namelist()) == set(files), 'Source ZIP validation failed')
        require(all(archive.read(n) == b for n,b in files.items()), 'Source ZIP content differs')
    print(json.dumps({'result': 'PASS', 'source_entries': len(files), 'source_bytes': sum(map(len, files.values())),
                      'archive': ZIP_NAME, 'bytes': archive_path.stat().st_size,
                      'sha256': sha(archive_path.read_bytes())}, ensure_ascii=False))


if __name__ == '__main__':
    main()
