#!/usr/bin/env python3
"""Reproduce desktop patch/application checks; this does not execute Android or publish.

Run build1922.py first. Use the original ULike 5.6.2 (740) APKS, not a patched APK.
The desktop merge/resource rebuild can take several minutes for each bundle.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import subprocess
import zipfile

ORIGINAL_SHA256 = '73c6d3a3008b9975645f63238f07dc9c1960ad982c70f141ee4b5dfe60f7a293'
ROOT = Path(__file__).resolve().parent


def execute(command, log):
    """Retain output and fail on a subprocess failure or a 15-minute timeout."""
    with log.open('wb') as stream:
        subprocess.run([str(value) for value in command], stdout=stream,
                       stderr=subprocess.STDOUT, timeout=900, check=True)
    return log.read_text()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('original', 'build', 'dist', 'tools', 'work'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    for key, value in vars(args).items():
        setattr(args, key, value.resolve())
    if hashlib.sha256(args.original.read_bytes()).hexdigest() != ORIGINAL_SHA256:
        raise ValueError('Original APKS SHA256 mismatch; do not use a patched application.')
    if args.work.exists():
        raise FileExistsError('Use a fresh --work directory.')
    args.work.mkdir(parents=True)
    classes = args.work / 'classes'
    classes.mkdir()
    classpath = os.pathsep.join(map(str, (
        args.tools / 'morphe.jar', args.build / 'baseline', classes)))
    execute(['javac', '-encoding', 'UTF-8', '-cp', classpath, '-d', classes,
             *[ROOT / (name + '.java') for name in (
                 'MergePayloads', 'VerifyApplied1920', 'AnalyzeAll', 'AnalyzeStock', 'Transform1922', 'VerifyLayout1922')]],
            args.work / 'compile-verifiers.log')
    with zipfile.ZipFile(args.original) as z:
        stock = args.work / 'stock.apk'
        stock.write_bytes(z.read('base.apk'))
    layout_checks = execute(['java', '-Xmx2g', '-cp', classpath, 'VerifyLayout1922',
                             stock, ROOT / 'layout-stock-seed.dex',
                             args.build / 'emitted/methods.dex',
                             args.build / 'emitted/runtime.dex'], args.work / 'layout-contracts.txt')
    outcomes = {}
    for kind, filename, index in (
            ('single', 'ULike_HQ_Texture_Online_v1.9.22.mpp', 0),
            ('bundle', 'Hiro_Morphe_Patches_v1.0.147.mpp', 1)):
        folder = args.work / kind
        folder.mkdir()
        apk = folder / 'patched.apk'
        execute(['java', '-Xmx3g', '-jar', args.tools / 'morphe.jar', 'patch',
                 args.original, '--exclusive', '-p', args.dist / filename, '--ei', index,
                 '--unsigned', '--bytecode-mode', 'FULL', '--striplibs', 'arm64-v8a',
                 '-o', apk, '-t', folder / 'tmp', '-r', folder / 'result.json'],
                folder / 'apply.log')
        result = json.loads((folder / 'result.json').read_text())
        if (result['packageName'] != 'com.gorgeous.liteinternational'
                or result['packageVersion'] != '5.6.2'
                or result['failedPatches']
                or not all(step['success'] for step in result['patchingSteps'])):
            raise RuntimeError(f'{kind}: patch result failed validation')
        with zipfile.ZipFile(apk) as archive:
            if archive.testzip() is not None:
                raise RuntimeError(f'{kind}: APK CRC error')
            if any(name.startswith('lib/') and name.split('/')[1] != 'arm64-v8a'
                   for name in archive.namelist() if not name.endswith('/')):
                raise RuntimeError(f'{kind}: unexpected native architecture')
        emitted = args.build / 'emitted'
        contracts = execute(['java', '-Xmx2g', '-cp', classpath, 'VerifyApplied1920',
                             emitted / 'runtime.dex', emitted / 'methods.tsv', apk],
                            folder / 'method-contracts.txt')
        counts = {}
        for verifier, label in (('AnalyzeAll', 'all-registers'),
                                ('AnalyzeStock', 'stock-registers')):
            command = ['java', '-Xmx2g', '-cp', classpath, verifier, apk,
                       args.tools / 'android.jar', folder / (label + '.tsv')]
            if verifier == 'AnalyzeStock':
                command.append(emitted / 'methods.tsv')
            text = execute(command, folder / (label + '.txt'))
            match = re.search(r'tested=(\d+) failed=(\d+)', text)
            if match is None or int(match[2]) != 0:
                raise RuntimeError(f'{kind}: {label} reports errors or no result')
            counts[label] = {'tested': int(match[1]), 'failed': int(match[2])}
        outcomes[kind] = {'patch_result': result, 'method_contracts': contracts.strip(),
                          'register_analysis': counts}
    report = {'desktop_validation': outcomes, 'android_device_test': False, 'layout_contracts': layout_checks.strip(),
              'black_preview_cause_confirmed': False, 'published': False,
              'limitations': 'Dexlib2 and host checks are not Android/ART execution.'}
    (args.work / 'validation.json').write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
