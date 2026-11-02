#!/usr/bin/env python3
"""Apply both reviewed save-speed MPPs to original APKS and verify emitted APK bytes and types."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import struct
import zipfile
import zlib
from build1944 import run, require, sha, SINGLE, SELECTED, bundle_name, verify_jdk

ROOT = Path(__file__).resolve().parent
ORIGINAL_SHA256 = '73c6d3a3008b9975645f63238f07dc9c1960ad982c70f141ee4b5dfe60f7a293'
ORIGINAL_BASE_SHA256 = 'f51c64cc2ffedf2110f18816831e5c7cff5652980b42d6bb22a4a3f37316730d'
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
        if value is not None:
            setattr(args, key, value.resolve())
    verify_jdk()
    qa = json.loads((args.dist / 'QA_ULike_v1.9.44.json').read_text())
    require(qa.get('schema') == 'ulike1944-speed-v1' and qa.get('ulike_version') == '1.9.44', 'Wrong save-speed QA')
    require(qa.get('host_quality_passed') is True and qa.get('host_quality_result', {}).get('status') == 'passed',
            'Host speed/quality-preservation regressions must pass before APK validation')
    require(qa.get('selected_candidates') == SELECTED, 'Selected save-speed candidates differ')
    combined = bundle_name(qa)
    original_digest = sha(args.original.read_bytes())
    require(original_digest in (ORIGINAL_SHA256, ORIGINAL_BASE_SHA256), 'Original APK/APKS pin mismatch')
    split_merge_tested = original_digest == ORIGINAL_SHA256
    input_format = 'APKS' if split_merge_tested else 'APK_BASE'
    if not split_merge_tested:
        print('Validating the exact original base APK; original split/native-library merge is not tested.', flush=True)
    for name in (SINGLE, combined):
        path = args.dist / name
        require(qa['artifacts'][name] == {'sha256': sha(path.read_bytes()), 'bytes': path.stat().st_size},
                'Build QA differs from MPP: ' + name)
    require(not args.work.exists(), 'Use fresh validation directory')
    args.work.mkdir(parents=True)
    classes = args.work / 'classes'
    classes.mkdir()
    cp = os.pathsep.join(map(str, (args.tools / 'morphe.jar', args.build / 'baseline', classes)))
    names = ['MergePayloads', 'Hooks1944', 'Transform1944', 'VerifyApplied1920', 'AnalyzeAll', 'Verify1944',
             'VerifyBurstAbi1933', 'VerifySaveAbi1935', 'VerifyLensAbi1936', 'VerifyFrontAbi1931', 'VerifyLayoutAbi1937', 'VerifyGeometryAbi1937', 'VerifyLayoutLifecycleAbi1937', 'GeometryHooks1937', 'LayoutHooks1937', 'VerifyRenderAbi1938', 'RenderHooks1938', 'VerifyFeedbackAbi1941', 'FeedbackHooks1941']
    names += [p.stem for p in sorted(ROOT.glob('*Hooks1944.java')) if p.stem not in names]
    require((ROOT / 'VerifyBurstAbi1933.java').is_file(), 'Actual native burst ABI verifier is required')
    retained_abi = ROOT / 'VerifyFrontAbi1938.java'
    require(retained_abi.is_file(), 'Front startup native and detached-renewal guard verification is required')
    if retained_abi.is_file():
        names.append('VerifyFrontAbi1938')
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes,
         *[ROOT / (name + '.java') for name in names]], args.work / 'compile-verifiers.log')
    production = ','.join(json.loads((ROOT / 'production1944.json').read_text()))
    native_properties = ['-Dulike.native.sha=' + qa['native_library_sha256'], '-Dulike.native.bytes=' + str(qa['native_library_bytes'])]
    stock = args.work / 'stock.apk'
    if split_merge_tested:
        with zipfile.ZipFile(args.original) as z:
            stock.write_bytes(z.read('base.apk'))
    else:
        stock.write_bytes(args.original.read_bytes())
    require(sha(stock.read_bytes()) == ORIGINAL_BASE_SHA256, 'Original base APK provenance mismatch')
    dex_checks = run(['java', '-Xmx2g', *native_properties, '-Dulike.production=' + production, '-cp', cp, 'Verify1944', args.build / 'baseline',
                     args.build / 'emitted', stock], args.work / 'speed-dex-verification.txt')
    abi_checks = None
    if retained_abi.is_file():
        abi_checks = run(['java', '-Xmx2g', '-cp', cp, 'VerifyFrontAbi1938', stock], args.work / 'stock-front-lifecycle-abi.txt')
    render_abi_checks = run(['java', '-Xmx2g', '-cp', cp, 'VerifyRenderAbi1938', stock], args.work / 'stock-render-lifecycle-abi1938.txt')
    require(render_abi_checks.startswith('PASS render1938 native ABI/lifecycle checks='), 'Stock renderer ABI verification required')
    burst_abi_checks = run(['java', '-Xmx2g', '-cp', cp, 'VerifyBurstAbi1933', stock],
                          args.work / 'stock-burst-capture-abi.txt')
    feedback_abi_checks = run(['java', '-Xmx2g', '-cp', cp, 'VerifyFeedbackAbi1941', stock, args.work / 'stock-feedback-abi1941.txt', 'false'], args.work / 'stock-feedback-abi1941.log')
    require(feedback_abi_checks.startswith('PASS feedback1941 native ABI '), 'Stock feedback ABI verification required')
    outcomes = {}
    for kind, filename in (('single', SINGLE), ('bundle', combined)):
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
            for name in dex_entries:
                data = z.read(name)
                require(len(data) >= 112 and data[:4] == b'dex\n' and data[7] == 0, 'Malformed DEX: ' + name)
                require(struct.unpack_from('<I', data, 32)[0] == len(data), 'DEX file-size header: ' + name)
                require(struct.unpack_from('<I', data, 36)[0] == 112 and struct.unpack_from('<I', data, 40)[0] == 0x12345678,
                        'DEX header/endian: ' + name)
                require(hashlib.sha1(data[32:]).digest() == data[12:32], 'DEX SHA-1: ' + name)
                require(zlib.adler32(data[12:]) & 0xffffffff == struct.unpack_from('<I', data, 8)[0], 'DEX Adler32: ' + name)
            abis = sorted({n.split('/')[1] for n in z.namelist() if n.startswith('lib/') and not n.endswith('/')})
            require(abis == ['arm64-v8a'], 'Native architecture mismatch')
            native_kernel = z.read('lib/arm64-v8a/libulike_speed1935.so')
            require(sha(native_kernel) == qa['native_library_sha256'] and len(native_kernel) == qa['native_library_bytes'],
                    'Actual APK native kernel differs from reviewed build')
            require(not any(any(marker in n for marker in
                                ('quality-stubs', 'quality-host', 'capture-stubs', 'capture-host',
                                 'fast-stubs', 'fast-host', 'fusion-host', '/tests/'))
                            for n in z.namelist()),
                    'Test source fixtures leaked into APK')
        contracts = run(['java', '-Xmx2g', '-cp', cp, 'VerifyApplied1920', args.build / 'emitted/runtime.dex',
                         args.build / 'emitted/methods.tsv', apk], folder / 'method-contracts.txt')
        applied_abi = None
        if retained_abi.is_file():
            applied_abi = run(['java', '-Xmx2g', '-cp', cp, 'VerifyFrontAbi1938', apk], folder / 'front-lifecycle-abi.txt')
        render_abi = run(['java', '-Xmx2g', '-cp', cp, 'VerifyRenderAbi1938', apk], folder / 'render-lifecycle-abi1938.txt')
        require(render_abi.startswith('PASS render1938 native ABI/lifecycle checks='), 'Applied renderer ABI verification required')
        burst_abi = run(['java', '-Xmx2g', '-cp', cp, 'VerifyBurstAbi1933', apk],
                        folder / 'burst-capture-abi.txt')
        feedback_abi = run(['java', '-Xmx2g', '-cp', cp, 'VerifyFeedbackAbi1941', apk, folder / 'feedback-abi1941.txt', 'true'], folder / 'feedback-abi1941.log')
        require(feedback_abi.startswith('PASS feedback1941 native ABI '), 'Applied feedback ABI required')
        save_abi = run(['java', '-Xmx2g', '-cp', cp, 'VerifySaveAbi1935', apk],
                       folder / 'save-native-abi1935.txt')
        require('PASS save native reflection ABI:' in save_abi, 'Actual save reflection ABI verification required')
        lens_abi = run(['java', '-Xmx2g', '-cp', cp, 'VerifyLensAbi1936', apk], folder / 'lens-abi1936.txt')
        require('PASS lens direct native/helper ABI:' in lens_abi, 'Actual lens switch field and method ABI verification required')
        layout_abi = run(['java', '-Xmx2g', '-cp', cp, 'VerifyLayoutAbi1937', apk], folder / 'layout-native-abi1937.txt')
        require('PASS layout native/direct ABI checks=' in layout_abi, 'Actual layout field, method, hook ABI verification required')
        geometry_abi = run(['java', '-Xmx2g', '-cp', cp, 'VerifyGeometryAbi1937', apk], folder / 'geometry-native-abi1937.txt')
        require(geometry_abi.startswith('PASS geometry native ABI '), 'Actual measured-geometry ABI verification required')
        lifecycle_abi = run(['java', '-Xmx2g', '-cp', cp, 'VerifyLayoutLifecycleAbi1937', apk], folder / 'lifecycle-layout-abi1937.txt')
        require('PASS layout lifecycle native/helper ABI and six callback hooks checks=' in lifecycle_abi, 'Actual delayed-layout native ABI verification required')
        registers = run(['java', '-Xmx2g', '-cp', cp, 'AnalyzeAll', apk, args.tools / 'android.jar',
                         folder / 'runtime-registers.tsv', args.build / 'emitted/methods.tsv'], folder / 'runtime-registers.txt')
        match = re.search(r'tested=(\d+) failed=(\d+)', registers)
        require(match is not None and int(match[1]) > 0 and int(match[2]) == 0, 'Runtime register/type analysis failed')
        outcomes[kind] = {'patch_index': int(indices[0]), 'patch_result': result,
                          'method_contracts': contracts.strip(), 'native_abis': abis,
                          'burst_capture_abi': burst_abi.strip(), 'render_lifecycle_abi': render_abi.strip(),
                          'feedback_native_abi': feedback_abi.strip(), 'save_native_abi': save_abi.strip(), 'lens_native_abi': lens_abi.strip(), 'layout_native_abi': layout_abi.strip(), 'geometry_native_abi': geometry_abi.strip(), 'layout_lifecycle_abi': lifecycle_abi.strip(),
                          'dex_integrity_entries': len(dex_entries),
                          'speed_native_library_sha256': sha(native_kernel), 'speed_native_library_bytes': len(native_kernel),
                          'register_analysis': {'tested': int(match[1]), 'failed': int(match[2])}}
        if applied_abi is not None:
            outcomes[kind]['front_lifecycle_abi'] = applied_abi.strip()
        print(kind + ': patch, resource rebuild, exact method contracts and register checks passed', flush=True)
    artifacts = {name: {'sha256': sha((args.dist / name).read_bytes()), 'bytes': (args.dist / name).stat().st_size}
                 for name in (SINGLE, combined)}
    evidence = {
        'schema': 'ulike1944-desktop-validation-v1', 'original_input_sha256': original_digest,
        'original_input_format': input_format, 'original_base_apk_sha256': ORIGINAL_BASE_SHA256,
        'original_split_merge_tested': split_merge_tested,
        'ulike_version': '1.9.44', 'bundle_version': qa['bundle_version'],
        'original_apk_apply_tested': True, 'device_tested': False, 'device_quality_verified': False,
        'original_apk_application': {'status': 'passed', 'input_sha256': original_digest,
                                     'single_and_bundle': True, 'split_merge_tested': split_merge_tested,
                                     'device_tested': False},
        'desktop_validation': outcomes, 'speed_dex_verification': dex_checks.strip(),
        'stock_burst_capture_abi_verification': burst_abi_checks.strip(),
        'stock_render_lifecycle_abi_verification': render_abi_checks.strip(),
        'stock_feedback_abi_verification': feedback_abi_checks.strip(),
        'artifacts': artifacts, 'published': False,
        'limitations': 'Morphe rebuild, host execution and DEX type analysis do not execute Android/ART or camera hardware.' + ('' if split_merge_tested else ' Only the original base APK was used; original split/native-library merging is not tested.'),
    }
    if split_merge_tested:
        evidence['original_apks_sha256'] = ORIGINAL_SHA256
    if abi_checks is not None:
        evidence['stock_lifecycle_abi_verification'] = abi_checks.strip()
    (args.work / 'validation.json').write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + '\n')
    print('PASS standalone and integrated MPPs validated against exact original ULike5.6.2(740) ' + input_format + '. Split merge tested=' + str(split_merge_tested) + '; device untested.')


if __name__ == '__main__':
    main()
