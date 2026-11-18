#!/usr/bin/env python3
"""Recompile the UI selection gate; retain every runtime/resource byte from 1.1.2."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
BASE_SHA = '8aa87497670bebbc4d31f589a935a1335361c6f6be8ce1a5e0732f574bd5ada8'
MF = 'META-INF/MANIFEST.MF'


def digest(data):
    return hashlib.sha256(data).hexdigest()


def run(args):
    result = subprocess.run(list(map(str, args)), text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    print(result.stdout, end='')
    result.check_returncode()
    return result.stdout


def archive(path, entries):
    with zipfile.ZipFile(path, 'w') as output:
        for name, data in sorted(entries.items()):
            info = zipfile.ZipInfo(name, (2026, 11, 18, 0, 0, 0))
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            output.writestr(info, data, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--base', type=Path, required=True)
    parser.add_argument('--tools', type=Path, required=True)
    parser.add_argument('--patcher', type=Path, required=True)
    parser.add_argument('--piko', type=Path, action='append', required=True)
    args = parser.parse_args()
    assert digest(args.base.read_bytes()) == BASE_SHA, 'UI 1.1.2 baseline SHA differs'
    with zipfile.ZipFile(args.base) as source:
        base = {name: source.read(name) for name in source.namelist()}
    assert b'Version: 1.1.2\r\n' in base[MF]
    work = ROOT / 'build'
    for name in ('classes', 'tests', 'dex'):
        path = work / name
        if path.exists():
            shutil.rmtree(path)
        path.mkdir(parents=True)
    originals = {name: data for name, data in base.items() if name.endswith('.class')}
    assert originals and all(name.startswith('app/hiro/newxui/patches/') for name in originals)
    archive(work / 'baseline-provider.jar', originals)
    compiler = ['java', '-XX:-UsePerfData', '-jar', args.tools / 'ecj.jar', '-8', '-nowarn', '-encoding', 'UTF-8']
    cp = os.pathsep.join(map(str, (args.patcher, work / 'baseline-provider.jar')))
    run(compiler + ['-cp', cp, '-d', work / 'classes', ROOT / 'UiOptionsPatch.java'])
    changed = {path.relative_to(work / 'classes').as_posix(): path.read_bytes() for path in (work / 'classes').rglob('*.class')}
    assert set(changed) == {'app/hiro/newxui/patches/UiOptionsPatch.class'}, changed.keys()
    providers = dict(originals, **changed)
    archive(work / 'provider.jar', providers)
    cp = os.pathsep.join(map(str, (work / 'provider.jar', args.patcher)))
    run(compiler + ['-cp', cp, '-d', work / 'tests', ROOT / 'UiSelectionAudit.java'])
    audit = run(['java', '-XX:-UsePerfData', '-cp', os.pathsep.join((str(work / 'tests'), cp)),
                 'app.hiro.newxui.patches.UiSelectionAudit', *args.piko])
    run(['java', '-XX:-UsePerfData', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8',
         '--release', '--min-api', '26', '--classpath', args.patcher, '--output', work / 'dex', work / 'provider.jar'])
    updated = dict(base, **changed)
    updated['classes.dex'] = (work / 'dex/classes.dex').read_bytes()
    updated[MF] = base[MF].replace(b'Version: 1.1.2\r\n', b'Version: 1.1.3\r\n').replace(
        b'Timestamp: 2026-11-17T00:00:00\r\n', b'Timestamp: 2026-11-18T00:00:00\r\n')
    output = ROOT / 'component.mpp'
    archive(output, updated)
    replaced = sorted(name for name in base if base[name] != updated[name])
    assert set(replaced) == {MF, 'classes.dex', 'app/hiro/newxui/patches/UiOptionsPatch.class'}
    with zipfile.ZipFile(output) as built:
        assert built.testzip() is None
        assert {name: built.read(name) for name in built.namelist()} == updated
    qa = {
        'result': 'PASS', 'component_version': '1.1.3',
        'component_sha256': digest(output.read_bytes()), 'baseline_sha256': BASE_SHA,
        'changed_zip_entries': replaced,
        'runtime_extension_sha256': digest(updated['extensions/newx_ui_options.mpe']),
        'runtime_rebuilt': False, 'runtime_and_assets_byte_identical_to_1_1_2': True,
        'selection_audit': audit.strip(),
        'input_sha256': {str(path): digest(path.read_bytes()) for path in [args.base, args.patcher, *args.piko]},
        'source_sha256': {name: digest((ROOT/name).read_bytes()) for name in ('build.py', 'UiOptionsPatch.java', 'UiSelectionAudit.java')},
        'device_tested': False,
    }
    (ROOT / 'QA.json').write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({k: qa[k] for k in ('result', 'component_sha256', 'changed_zip_entries', 'runtime_extension_sha256')}, ensure_ascii=False))


if __name__ == '__main__':
    main()
