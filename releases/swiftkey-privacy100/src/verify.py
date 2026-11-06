#!/usr/bin/env python3
"""Apply each released MPP to the supplied reference and collect real APK audits.

The APK is private input. This script never uploads or packages an application.
"""
from pathlib import Path
import argparse
import hashlib
import io
import json
import os
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
APKS_SHA = '8f3dbdcd975bad0e63355702dc371761953fd845add6f11277f5c0a2412bdd24'
NAME = 'テレメトリ・解析・エラー報告を停止'
ARTIFACTS = ('SwiftKeyBeta_Privacy_v1.0.0.mpp', 'Hiro_Morphe_Patches_v1.0.362.mpp')


def require(ok, message):
    if not ok:
        raise RuntimeError(message)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def entries(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None and len(z.namelist()) == len(set(z.namelist())), 'Invalid ZIP')
        return {n: z.read(n) for n in z.namelist() if not n.endswith('/')}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    for key in ('apks', 'merged', 'tools', 'java-home', 'apktool', 'build', 'dist', 'work'):
        p.add_argument('--' + key, type=Path, required=True)
    a = p.parse_args()
    for key in vars(a):
        setattr(a, key, getattr(a, key).resolve())
    require(not a.work.exists(), 'Use a fresh verification directory')
    for path in (a.work, a.work / 'tmp'):
        path.mkdir(parents=True, exist_ok=True)
    env = dict(os.environ, TMPDIR=str(a.work / 'tmp'), MORPHE_DATA_DIR=str(a.work / 'morphe-data'))
    java = a.java_home / 'bin/java'
    flags = ['-XX:-UsePerfData', '-Djava.io.tmpdir=' + str(a.work / 'tmp'), '-Xmx2g']

    def run(command, name, timeout=240):
        print('RUN ' + name, flush=True)
        with (a.work / name).open('w') as out:
            r = subprocess.run(list(map(str, command)), stdout=out, stderr=subprocess.STDOUT,
                               env=env, timeout=timeout)
        raw = (a.work / name).read_text()
        require(r.returncode == 0, name + ' failed:\n' + raw[-10000:])
        return raw.strip()

    build = json.loads((a.build / 'build.json').read_text())
    require(build['bundle_version'] == '1.0.362', 'Unexpected build version')
    for name in ARTIFACTS:
        require(build['artifacts'][name] == {'bytes': (a.dist / name).stat().st_size,
                                           'sha256': sha(a.dist / name)}, 'MPP changed after build')
    current_sources = {path.relative_to(ROOT).as_posix(): sha(path) for path in ROOT.rglob('*')
                       if path.is_file() and path.suffix in ('.java', '.py')}
    require(build['source_sha256'] == current_sources, 'Source changed after build')
    require(sha(a.apks) == APKS_SHA, 'Wrong private reference APKS')
    split = entries(a.apks)
    base = entries(io.BytesIO(split['base.apk']))
    merged = entries(a.merged)
    dex = lambda data: {n: v for n, v in data.items() if n.endswith('.dex')}
    require(dex(base) == dex(merged), 'Split merging changed base DEX')
    split_payloads = {}
    for name, payload in split.items():
        if not name.endswith('.apk'):
            continue
        for path, raw in entries(io.BytesIO(payload)).items():
            if path.startswith(('assets/', 'lib/')):
                require(path not in split_payloads or split_payloads[path] == raw, 'Conflicting split payload')
                split_payloads[path] = raw
    require(all(merged.get(n) == raw for n, raw in split_payloads.items()), 'Split assets/native bytes changed')
    original_sha = sha(a.merged)
    framework = a.work / 'framework'

    def decode(apk, dest, label):
        run([java, *flags, '-jar', a.apktool, 'd', '-s', '-p', framework, '-o', dest, apk], label)

    original_decoded = a.work / 'original-decoded'
    decode(a.merged, original_decoded, 'decode-original.txt')
    guards = []
    for loader in ('morphe.jar', 'morphe-2.2.jar'):
        cp = os.pathsep.join(map(str, (a.build / 'test', a.build / 'patch', a.tools / loader)))
        result = run([java, *flags, '-ea', '-cp', cp, 'PrivacyGuardTest', 'guards', a.merged], 'guards-' + loader + '.txt')
        require(result.startswith('PASS'), 'Guard audit missing')
        guards.append({'loader': loader, 'result': result})
    applications, outputs = [], []
    for index, artifact in enumerate(ARTIFACTS):
        label = 'single' if index == 0 else 'integrated'
        output, decoded = a.work / (label + '.apk'), a.work / (label + '-decoded')
        cli_report = a.work / (label + '-application.json')
        run([java, *flags, '-jar', a.tools / 'morphe-2.2.jar', 'patch', a.merged,
             '-p', a.dist / artifact, '--exclusive', '-e', NAME, '--unsigned',
             '--bytecode-mode', 'STRIP_FAST', '-o', output, '-t', a.work / 'tmp', '-r', cli_report], label + '-apply.txt')
        cli = json.loads(cli_report.read_text())
        require(cli.get('success') is True and cli.get('failedPatches') == []
                and cli.get('appliedPatches') == [NAME], 'Unexpected patching result')
        require(cli.get('packageName') == 'com.touchtype.swiftkey.beta'
                and cli.get('packageVersion') == '9.13.16.4', 'Wrong APK identity')
        cp = os.pathsep.join(map(str, (a.build / 'test', a.tools / 'morphe-2.2.jar')))
        audit = run([java, *flags, '-ea', '-cp', cp, 'PrivacyAudit', 'diff', a.merged, output], label + '-dex-audit.txt')
        require(audit.startswith('PASS'), 'DEX audit missing')
        decode(output, decoded, label + '-decode.txt')
        resource_report = a.work / (label + '-resource-audit.json')
        run(['python3', ROOT / 'audit_resources.py', '--original-apk', a.merged,
             '--original-apk-sha256', original_sha, '--patched-apk', output,
             '--original-decoded', original_decoded, '--patched-decoded', decoded,
             '--output', resource_report], label + '-resource-audit.txt')
        resources = json.loads(resource_report.read_text())
        require(resources['status'] == 'PASS' and resources['errors'] == [], 'Resource audit failed')
        resources['result'] = resources['status']
        applications.append({'artifact': artifact, 'result': 'PASS', 'sdk_version': '2.2.0',
                             'bytecode_mode': 'STRIP_FAST', 'applied_patches': cli['appliedPatches'],
                             'patched_apk_sha256': sha(output), 'dex_audit': audit, 'resource_audit': resources})
        outputs.append(output)
    require(entries(outputs[0]) == entries(outputs[1]), 'Single and integrated APK payloads differ')
    qa = {**build, 'schema': 'swiftkey-privacy100-qa-v1', 'release_timezone': 'Asia/Tokyo',
          'original_stock_apk_tested': False, 'android_emulator_tested': False,
          'runtime_android_tested': False, 'stock_input_patch_coapplication_tested': False,
          'reference_input': {'kind': 'previously_modified_v2_apks', 'sha256': APKS_SHA,
                              'splits': sorted(n for n in split if n.endswith('.apk')),
                              'merged_apk_sha256': original_sha,
                              'base_merged_dex_equal': True, 'dex_entries': len(dex(base)),
                              'preserved_split_assets_and_native_entries': len(split_payloads)},
          'guard_tests': guards, 'apk_applications': applications,
          'single_bundle_apk_entry_contents_equal': True,
          'limitations': ['No device/emulator or traffic-capture execution.',
                         'The input is previously modified v2, not an unmodified stock APK.',
                         'Crashlytics local crash records may remain; automatic collection/reporting is disabled.',
                         'This option does not prohibit all app networking or erase stored logs.']}
    destination = a.dist / 'QA_SwiftKey_Privacy_v1.0.0.json'
    destination.write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    print('PASS both real APK applications, independent six-method/resource audits and payload parity')


if __name__ == '__main__':
    main()
