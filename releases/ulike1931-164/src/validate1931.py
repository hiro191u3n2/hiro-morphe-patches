#!/usr/bin/env python3
"""Apply both MPPs to the original APKS and verify the emitted APK bytes and types."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import zipfile
from build1931 import run, require, sha, SINGLE, BUNDLE

ROOT = Path(__file__).resolve().parent
ORIGINAL_SHA256 = '73c6d3a3008b9975645f63238f07dc9c1960ad982c70f141ee4b5dfe60f7a293'
PATCH_NAME = '高画質撮影・質感美肌・素材通信を復旧'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('original', 'build', 'dist', 'tools', 'work'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    args = parser.parse_args()
    if args.jdk:
        os.environ['PATH'] = str(args.jdk.resolve() / 'bin') + os.pathsep + os.environ.get('PATH', '')
    for key, value in vars(args).items():
        if value is not None: setattr(args, key, value.resolve())
    require(sha(args.original.read_bytes()) == ORIGINAL_SHA256, 'Original APKS pin mismatch')
    require(not args.work.exists(), 'Use fresh validation directory')
    args.work.mkdir(parents=True)
    classes = args.work / 'classes'
    classes.mkdir()
    cp = os.pathsep.join(map(str, (args.tools / 'morphe.jar', args.build / 'baseline', classes)))
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes,
         *[ROOT / (n + '.java') for n in ('MergePayloads', 'Transform1931', 'VerifyApplied1920', 'AnalyzeAll', 'VerifyFront1931', 'VerifyNativeAbi1931', 'VerifyFrontAbi1931')]],
        args.work / 'compile-verifiers.log')
    stock = args.work / 'stock.apk'
    with zipfile.ZipFile(args.original) as z:
        stock.write_bytes(z.read('base.apk'))
    dex_checks = run(['java', '-Xmx2g', '-cp', cp, 'VerifyFront1931', args.build / 'baseline',
                     args.build / 'emitted', stock], args.work / 'front-dex-verification.txt')
    abi_checks = run(['java', '-Xmx2g', '-cp', cp, 'VerifyNativeAbi1931', stock, args.build / 'emitted'], args.work / 'native-front1931-abi.txt')
    stock_lifecycle_abi = run(['java', '-Xmx2g', '-cp', cp, 'VerifyFrontAbi1931', stock], args.work / 'stock-lifecycle-abi.txt')
    outcomes = {}
    for kind, filename in (('single', SINGLE), ('bundle', BUNDLE)):
        folder = args.work / kind
        folder.mkdir()
        listing = run(['java', '-jar', args.tools / 'morphe.jar', 'list-patches', '--patches', args.dist / filename],
                      folder / 'patch-list.txt')
        indices = re.findall(r'Index: (\d+)\s+Name: ' + re.escape(PATCH_NAME) + r'(?:\r?\n)', listing)
        require(len(indices) == 1, 'ULike patch index must resolve exactly once')
        apk = folder / 'patched.apk'
        run(['java', '-Xmx3g', '-jar', args.tools / 'morphe.jar', 'patch', args.original,
             '--exclusive', '-p', args.dist / filename, '--ei', indices[0], '--unsigned',
             '--bytecode-mode', 'FULL', '--striplibs', 'arm64-v8a', '-o', apk,
             '-t', folder / 'tmp', '-r', folder / 'result.json'], folder / 'apply.log')
        result = json.loads((folder / 'result.json').read_text())
        require(result['packageName'] == 'com.gorgeous.liteinternational' and result['packageVersion'] == '5.6.2'
                and not result['failedPatches'] and all(x['success'] for x in result['patchingSteps']),
                'Application patch/rebuild failed')
        require([x['name'] for x in result['appliedPatches']] == [PATCH_NAME], 'Unexpected applied patch')
        with zipfile.ZipFile(apk) as z:
            require(z.testzip() is None, 'Output APK CRC failure')
            dex_entries = [n for n in z.namelist() if re.fullmatch(r'classes(?:[2-9][0-9]*|1[0-9]+)?\.dex', n)]
            require(bool(dex_entries), 'No emitted DEX')
            import struct, zlib
            for n in dex_entries:
                data = z.read(n)
                require(len(data) >= 112 and data[:4] == b'dex\n' and data[7] == 0, 'Malformed DEX: ' + n)
                require(struct.unpack_from('<I', data, 32)[0] == len(data), 'DEX file-size header: ' + n)
                require(struct.unpack_from('<I', data, 36)[0] == 112 and struct.unpack_from('<I', data, 40)[0] == 0x12345678, 'DEX header/endian: ' + n)
                require(hashlib.sha1(data[32:]).digest() == data[12:32], 'DEX SHA-1: ' + n)
                require(zlib.adler32(data[12:]) & 0xffffffff == struct.unpack_from('<I', data, 8)[0], 'DEX Adler32: ' + n)
            abis = sorted({n.split('/')[1] for n in z.namelist() if n.startswith('lib/') and not n.endswith('/')})
            require(abis == ['arm64-v8a'], 'Native architecture mismatch')
            require(not any('front-stubs' in n or 'front-host' in n for n in z.namelist()), 'Test fixtures leaked')
        contracts = run(['java', '-Xmx2g', '-cp', cp, 'VerifyApplied1920', args.build / 'emitted/runtime.dex',
                         args.build / 'emitted/methods.tsv', apk], folder / 'method-contracts.txt')
        applied_abi = run(['java', '-Xmx2g', '-cp', cp, 'VerifyFrontAbi1931', apk], folder / 'front-lifecycle-abi.txt')
        registers = run(['java', '-Xmx2g', '-cp', cp, 'AnalyzeAll', apk, args.tools / 'android.jar',
                         folder / 'runtime-registers.tsv'], folder / 'runtime-registers.txt')
        match = re.search(r'tested=(\d+) failed=(\d+)', registers)
        require(match is not None and int(match[2]) == 0, 'Runtime register/type analysis failed')
        outcomes[kind] = {'patch_index': int(indices[0]), 'patch_result': result,
                          'method_contracts': contracts.strip(), 'native_abis': abis,
                          'dex_integrity_entries': len(dex_entries),
                          'front_lifecycle_abi': applied_abi.strip(),
                          'register_analysis': {'tested': int(match[1]), 'failed': int(match[2])}}
        print(kind + ': patch, resource rebuild, exact method contracts and register checks passed', flush=True)
    artifacts = {name: {'sha256': sha((args.dist / name).read_bytes()), 'bytes': (args.dist / name).stat().st_size}
                 for name in (SINGLE, BUNDLE)}
    evidence = {
        'schema': 'ulike1931-desktop-validation-v1', 'original_apks_sha256': ORIGINAL_SHA256,
        'original_apk_apply_tested': True, 'device_tested': False, 'device_fix_confirmed': False,
        'desktop_validation': outcomes, 'front_dex_verification': dex_checks.strip(), 'front_abi_verification': abi_checks.strip(),
        'artifacts': artifacts, 'published': False,
        'stock_lifecycle_abi_verification': stock_lifecycle_abi.strip(),
        'limitations': 'Morphe rebuild, DEX host execution and type analysis do not execute Android/ART or camera hardware.',
    }
    (args.work / 'validation.json').write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + '\n')
    print('PASS both standalone and integrated MPPs validated against original ULike5.6.2(740). Device untested.')


if __name__ == '__main__':
    main()
