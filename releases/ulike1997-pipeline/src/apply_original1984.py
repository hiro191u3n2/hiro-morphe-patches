#!/usr/bin/env python3
"""Apply the finished MPP to the private original ULike APKS and inspect the APK.

The original and unsigned result remain in the explicitly chosen work directory.
Only application-report.json is suitable for release QA: it contains hashes,
counts and verified contracts, never source APK bytes, local paths or raw logs.
This check is a desktop application/rebuild check, not Android execution.
"""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import struct
import subprocess
import zipfile
import zlib

SOURCE = Path(__file__).resolve().parent
INPUT_SHA256 = '73c6d3a3008b9975645f63238f07dc9c1960ad982c70f141ee4b5dfe60f7a293'
MORPHE_SHA256 = '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c'
PATCH_NAME = '高画質撮影・質感美肌・素材通信を復旧'
NATIVE_CHANGE = {
    'name': 'lib/arm64-v8a/libttvesdk.so',
    'before_sha256': '67f1b97b92859630ae200cafad41f7e3db3d8d2fbcac3ffd5c7bab0683471095',
    'after_sha256': 'eac57bcaa613860c7ca03ea516c4370520f9775cdfe700688d74e223bd59c2bb',
    'offset': 4241508, 'before_hex': '26008052', 'after_hex': 'a6e34039',
    'contract': 'app.hiro.ulike.patches.NativeNv21EffectFlag',
}
EXPECTED_NATIVE_COUNT = 12
EXPECTED_STOCK_NATIVE_COUNT = 58


def sha(data):
    return hashlib.sha256(data).hexdigest()


def require(value, message):
    if not value:
        raise AssertionError(message)


def run(command, log, timeout=900):
    result = subprocess.run(list(map(str, command)), text=True, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, timeout=timeout)
    log.write_text(result.stdout)
    if result.returncode:
        raise RuntimeError('Command failed; private log: ' + str(log) + '\n' + result.stdout[-6000:])
    return result.stdout


def write_entries(archive, root, prefix):
    for name in archive.namelist():
        if not name.startswith(prefix) or not name.endswith('.class'):
            continue
        # Read only trusted in-archive class entries; never extract arbitrary paths.
        part = PurePosixPath(name)
        require(not part.is_absolute() and '..' not in part.parts, 'Unsafe class path')
        target = root.joinpath(*part.parts)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(archive.read(name))


def main(input, mpp, work, jdk, tools, stage='final', patch_only=False, resume=False):
    input, mpp, work, jdk, tools = [Path(p).resolve() for p in (input, mpp, work, jdk, tools)]
    require(not (patch_only and resume), 'Patch-only and resume are exclusive')
    require(work.is_dir() if resume else not work.exists(), 'Use a fresh work directory or a pinned patch-only checkpoint')
    require(input.is_file() and mpp.is_file(), 'Original APKS and candidate MPP are required')
    require(sha(input.read_bytes()) == INPUT_SHA256, 'Pinned original ULike v5.6.2(740) APKS changed')
    java, javac, morphe = jdk / 'bin/java', jdk / 'bin/javac', tools / 'morphe.jar'
    require(java.is_file() and javac.is_file() and morphe.is_file(), 'Pinned JDK and Morphe toolchain required')
    require(sha(morphe.read_bytes()) == MORPHE_SHA256, 'Pinned Morphe binary changed')
    if not resume:
        work.mkdir(parents=True)
    settings = run([java, '-XshowSettings:properties', '-version'], work / 'java-identity.log', 30)
    properties = dict(re.findall(r'^\s*(java\.(?:runtime\.version|vendor|version)) = (.+)$', settings, re.MULTILINE))
    require(properties.get('java.runtime.version') == '21.0.8+9-LTS'
            and properties.get('java.vendor') == 'Eclipse Adoptium'
            and properties.get('java.version') == '21.0.8', 'Pinned Java runtime changed')
    compiler = run([javac, '-version'], work / 'javac-identity.log', 30)
    require(compiler.strip() == 'javac 21.0.8', 'Pinned Java compiler changed')
    expected = zipfile.ZipFile(mpp)
    require(expected.testzip() is None, 'Candidate MPP CRC failure')
    with zipfile.ZipFile(input) as original:
        require(original.testzip() is None, 'Original APKS CRC failure')
        split = zipfile.ZipFile(io.BytesIO(original.read('split_config.arm64_v8a.apk')))
        original_native = {name: split.read(name) for name in split.namelist()
                           if name.startswith('lib/arm64-v8a/') and name.endswith('.so')}
        require(len(original_native) == EXPECTED_STOCK_NATIVE_COUNT, 'Original split native count changed')

    output, patch_result = work / 'patched-private.apk', work / 'patch-result.json'
    if resume:
        checkpoint = json.loads((work / 'patch-checkpoint.json').read_text())
        require(checkpoint['mpp_sha256'] == sha(mpp.read_bytes()) and checkpoint['input_sha256'] == INPUT_SHA256,
                'Resumed MPP/original differs from applied checkpoint')
        for name, digest in checkpoint['files'].items():
            require(name in ('patched-private.apk', 'patch-result.json', 'real-patch.log', 'patch-list.log'), 'Unknown checkpoint artifact')
            require(sha((work / name).read_bytes()) == digest, 'Private patch checkpoint changed: ' + name)
        actual_log = (work / 'real-patch.log').read_text()
    else:
        listing = run([java, '-jar', morphe, 'list-patches', '--patches', mpp], work / 'patch-list.log', 60)
        indices = re.findall(r'Index: (\d+)\s+Name: ' + re.escape(PATCH_NAME) + r'(?:\r?\n)', listing)
        require(len(indices) == 1, 'Exactly one ULike patch must resolve')
        command = [java, '-Xmx3g', '-jar', morphe, 'patch', input, '--exclusive', '-p', mpp,
                   '--ei', indices[0], '--unsigned', '--bytecode-mode', 'FULL', '--striplibs',
                   'arm64-v8a', '-o', output, '-t', work / 'tmp', '-r', patch_result]
        actual_log = run(command, work / 'real-patch.log')
    markers = ('Merging split APK bundle', 'Merging: base', 'Merging: split_config.arm64_v8a',
               'Compiling patched dex files (mode: FULL)', 'Compiling modified resources', 'Aligning APK')
    require(all(marker in actual_log for marker in markers), 'Actual split merge/FULL rebuild evidence incomplete')
    patch = json.loads(patch_result.read_text())
    require(patch.get('packageName') == 'com.gorgeous.liteinternational' and patch.get('packageVersion') == '5.6.2',
            'Rebuilt package/version changed')
    require(not patch.get('failedPatches') and all(step['success'] for step in patch['patchingSteps']),
            'Actual Morphe patch/rebuild step failed')
    require([p['name'] for p in patch['appliedPatches']] == [PATCH_NAME], 'Wrong patch set applied')
    if not resume:
        checkpoint = {'mpp_sha256': sha(mpp.read_bytes()), 'input_sha256': INPUT_SHA256,
                      'files': {name: sha((work / name).read_bytes()) for name in
                                ('patched-private.apk', 'patch-result.json', 'real-patch.log', 'patch-list.log')}}
        (work / 'patch-checkpoint.json').write_text(json.dumps(checkpoint, indent=2) + '\n')
    if patch_only:
        print(json.dumps({'status': 'patch_completed_verification_pending', 'mpp_sha256': checkpoint['mpp_sha256'],
                          'work': str(work), 'final_report_created': False}, indent=2))
        return None
    archive = zipfile.ZipFile(output)
    require(archive.testzip() is None, 'Final APK CRC failure')
    dex = {n: archive.read(n) for n in archive.namelist() if re.fullmatch(r'classes(?:[2-9][0-9]*|1[0-9]+)?\.dex', n)}
    require(dex, 'Rebuilt DEX absent')
    for name, data in dex.items():
        require(len(data) > 112 and data[:4] == b'dex\n' and struct.unpack_from('<I', data, 32)[0] == len(data), 'DEX header failure: ' + name)
        require(data[12:32] == hashlib.sha1(data[32:]).digest(), 'DEX SHA-1 failure: ' + name)
        require(struct.unpack_from('<I', data, 8)[0] == zlib.adler32(data[12:]) & 0xffffffff, 'DEX Adler failure: ' + name)
    installed_native = {name: archive.read(name) for name in archive.namelist() if name.startswith('lib/') and name.endswith('.so')}
    require(sorted({n.split('/')[1] for n in installed_native}) == ['arm64-v8a'], 'Unexpected native ABI')
    for name, before in original_native.items():
        require(name in installed_native, 'Original native library lost: ' + name)
        after = installed_native[name]
        if name == NATIVE_CHANGE['name']:
            p = NATIVE_CHANGE
            offset = p['offset']
            require(sha(before) == p['before_sha256'] and sha(after) == p['after_sha256'], 'Inherited NV21 native contract changed')
            require(before[offset:offset+4].hex() == p['before_hex'] and after[offset:offset+4].hex() == p['after_hex']
                    and before[:offset] == after[:offset] and before[offset+4:] == after[offset+4:],
                    'Unexpected original native change outside the inherited instruction')
        else:
            require(after == before, 'Original native bytes changed: ' + name)

    native = {}
    def payload(installed, data, descriptor=None):
        require(installed in installed_native and installed_native[installed] == data,
                'MPP native payload not installed exactly: ' + installed)
        record = {'bytes': len(data), 'sha256': sha(data)}
        require(installed not in native or native[installed] == record, 'Contradictory MPP native descriptors')
        native[installed] = record
    for name in expected.namelist():
        if name.endswith('.so'):
            payload('lib/arm64-v8a/' + PurePosixPath(name).name, expected.read(name))
        elif name.endswith('resourceitems.tsv'):
            for line in expected.read(name).decode().splitlines():
                if not line or line.startswith('#'):
                    continue
                installed, digest, size, asset = line.split('\t')
                if installed.startswith('lib/') and installed.endswith('.so'):
                    data = expected.read(asset)
                    require(sha(data) == digest and len(data) == int(size), 'MPP native resource descriptor mismatch')
                    payload(installed, data)
    require(len(native) == EXPECTED_NATIVE_COUNT, 'Expected twelve ULike native assets')
    require(set(installed_native) == set(original_native) | set(native), 'Unexpected additional installed native libraries')
    require('lib/arm64-v8a/libulike_gpu1960.so' in native, 'Current GPU backend absent')
    require(not any(marker in name for name in archive.namelist()
                    for marker in ('quality-host', 'quality-stubs', '/tests1981/', '/tests1982/', 'pipeline80-fixtures')), 'Test fixtures leaked into APK')

    runtime = work / 'expected'
    runtime.mkdir()
    for source_name, target_name in (('ulike/runtime.dex', 'runtime.dex'), ('ulike/methods.tsv', 'methods.tsv'), ('ulike/methods.dex', 'methods.dex')):
        (runtime / target_name).write_bytes(expected.read(source_name))
    patch_classes, validators = work / 'patch-classes', work / 'validator-classes'
    patch_classes.mkdir(); validators.mkdir()
    write_entries(expected, patch_classes, 'app/hiro/ulike/patches/')
    cp = os.pathsep.join(map(str, (morphe, patch_classes, validators)))
    validator_sources = [SOURCE / name for name in ('MergePayloads.java', 'VerifyApplied1920.java',
        'VerifyAllHelperReferences1980.java', 'tests1981/VerifyAppliedTypes1981.java')]
    run([javac, '-encoding', 'UTF-8', '-cp', cp, '-d', validators, *validator_sources], work / 'validators-compile.log', 60)
    contracts = run([java, '-Xmx2g', '-cp', cp, 'VerifyApplied1920', runtime / 'runtime.dex', runtime / 'methods.tsv', output],
                    work / 'exact-method-contracts.log', 120).strip()
    require(contracts.startswith('PASS final APK:'), 'Exact applied DEX contracts absent')
    references = run([java, '-Xmx2g', '-cp', cp, 'VerifyAllHelperReferences1980', runtime / 'methods.dex', runtime / 'runtime.dex',
                      runtime / 'methods.dex', output], work / 'all-apk-helper-references.log', 120).strip()
    require(references.startswith('PASS helper references'), 'All APK helper member reference proof absent')
    types = json.loads(run([java, '-Xmx2g', '-cp', cp, 'VerifyAppliedTypes1981', output], work / 'all-apk-helper-types.log', 120))
    require(types['status'] == 'passed' and types['missing_helper_types'] == 0, 'All APK helper type proof failed')
    logs = ('real-patch.log', 'patch-result.json', 'exact-method-contracts.log', 'all-apk-helper-references.log', 'all-apk-helper-types.log')
    contract_count = re.search(r'\. (\d+) runtime/payload contracts', contracts)
    reference_count = re.search(r': (\d+) included members resolved; (\d+) inherited external members', references)
    require(contract_count is not None and reference_count is not None, 'Machine-readable full APK contract counts absent')
    report = {
        'schema': 'ulike1984-original-apks-application-v1', 'status': 'passed', 'artifact_stage': stage,
        'original_input': {'bytes': input.stat().st_size, 'sha256': INPUT_SHA256},
        'mpp': {'filename': mpp.name, 'bytes': mpp.stat().st_size, 'sha256': sha(mpp.read_bytes())},
        'output_apk': {'bytes': output.stat().st_size, 'sha256': sha(output.read_bytes())},
        'package_name': patch['packageName'], 'package_version': patch['packageVersion'],
        'original_apk_apply_tested': True, 'original_split_merge_tested': True, 'device_tested': False,
        'signing_or_installation_tested': False, 'ci_android_apply_tested': False,
        'output_unsigned': True, 'output_publicly_distributed': False,
        'expected_runtime_contracts': int(contract_count.group(1)),
        'all_apk_linkage_count': int(reference_count.group(1)),
        'inherited_external_linkage_count': int(reference_count.group(2)),
        'actual_split_merge_tested': True, 'full_dex_compilation_tested': True, 'resource_compilation_tested': True,
        'apk_alignment_tested': True, 'arm64_only_verified': True, 'physical_android_tested': False,
        'applied_patch_names': [p['name'] for p in patch['appliedPatches']], 'all_patching_steps_succeeded': True,
        'original_native_library_count': len(original_native), 'original_native_libraries_byte_identical': len(original_native) - 1,
        'inherited_native_changes': [NATIVE_CHANGE], 'installed_patch_native_assets': native,
        'dex_entries': {n: {'bytes': len(data), 'sha256': sha(data)} for n, data in dex.items()},
        'exact_method_contracts': contracts, 'all_apk_helper_member_references': references, 'all_apk_helper_types': types,
        'morphe_jar': {'bytes': morphe.stat().st_size, 'sha256': sha(morphe.read_bytes())},
        'java_toolchain': {'runtime_version': properties['java.runtime.version'], 'vendor': properties['java.vendor'], 'compiler_version': '21.0.8'},
        'validator_sources': {p.relative_to(SOURCE).as_posix(): sha(p.read_bytes()) for p in validator_sources},
        'application_script_sha256': sha(Path(__file__).read_bytes()),
        'private_log_sha256': {name: sha((work / name).read_bytes()) for name in logs},
        'limitations': 'Desktop Morphe split merge, FULL rebuild and exact DEX/native inspection. Android ART, Galaxy camera transitions, saved image appearance and device speed were not executed.',
    }
    path = work / 'application-report.json'
    path.write_text(json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
    print(json.dumps({'status': 'passed', 'report': str(path), 'mpp_sha256': report['mpp']['sha256'],
        'exact_method_contracts': contracts, 'native_assets': len(native), 'original_native_libraries': len(original_native),
        'physical_android_tested': False}, ensure_ascii=False, indent=2))
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('input', 'mpp', 'work', 'jdk', 'tools'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--stage', choices=('provisional', 'final'), default='final')
    parser.add_argument('--patch-only', action='store_true', help='Create a pinned private rebuild checkpoint without a success report')
    parser.add_argument('--resume', action='store_true', help='Verify an unchanged private checkpoint with the finalized current validators')
    args = parser.parse_args()
    main(args.input, args.mpp, args.work, args.jdk, args.tools, args.stage, args.patch_only, args.resume)
