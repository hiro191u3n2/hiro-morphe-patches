#!/usr/bin/env python3
"""Apply the exact release MPPs to the signed, pinned original TikTok APK.

The APK is a private local test input, never a release asset. Every selected patch,
pipeline step and emitted native hook must pass. This is not a physical-device UI
test. Run after build.py, then rebuild twice with the completed evidence frozen.
"""
from pathlib import Path
import argparse
import json
import os
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
from bundle_tools import json_bytes, require, sha
from build import SINGLE, BUNDLE, TOOLS

ORIGINAL_SHA = '6670ca3f93bfea379658cad3c27f05d3a380b23a0d82569cce684b7453e8ff3e'
SIGNER_SHA = '9041803e91bcb814b4b4399fb5c85a91640b755e5e8ba76813814bf4cf2ab5ba'
UPSTREAM_SHA = '45dc0c2244f8cf6973de18542d2d9a0cc25512f5b1de52438c685b82edb7c4cd'
NATIVE = [
    'TikTok 怪しい通信だけ遮断（TikTok 47.5.3）',
    'フィード左下の文字・再投稿欄を非表示（TikTok 47.5.3）',
    'フィード下部のCapCut・エフェクト誘導枠を完全遮断（TikTok 47.5.3）',
    '動画のシークバーを常時表示（TikTok 47.5.3）',
    'シーク中の動画プレビューサムネイルを表示（TikTok 47.5.3）',
    'フォロー中のストーリーズ件数を非表示（TikTok 47.5.3）',
]
ONEBACK = '戻る1回で終了・履歴削除'
JAPANESE = 'TikTok設定を日本語化'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('apk', 'dist', 'upstream', 'tools', 'work'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--proof', type=Path, default=ROOT/'validation/real-apk.json')
    args = parser.parse_args()
    for name in vars(args):
        setattr(args, name, getattr(args, name).resolve())
    require(sha(args.apk) == ORIGINAL_SHA and args.apk.stat().st_size == 476733983,
            'Use the exact signed original global 47.5.3 APK')
    require(sha(args.upstream) == UPSTREAM_SHA, 'Metra Settings input changed')
    require(sha(args.tools/'morphe200.jar') == TOOLS['morphe200.jar'], 'Patcher 2.0 SDK changed')
    require(not args.work.exists(), 'Use a fresh actual-APK work directory')
    args.work.mkdir(parents=True)
    evidence = args.proof.parent/'real-apk'
    evidence.mkdir(parents=True, exist_ok=True)
    artifact_hashes = {name: sha(args.dist/name) for name in (SINGLE, BUNDLE)}
    stage = args.work/'tools'
    stage.mkdir()
    shutil.copyfile(args.tools/'morphe200.jar', stage/'morphe.jar')
    sdk = stage/'morphe.jar'

    def run(command, label, timeout=3600):
        print('RUN ' + label, flush=True)
        path = args.work/(label+'.log')
        with path.open('w') as log:
            result = subprocess.run(list(map(str, command)), stdout=log, stderr=subprocess.STDOUT, timeout=timeout)
        text = path.read_text()
        require(result.returncode == 0, 'Failed '+label+'\n'+text[-16000:])
        (evidence/path.name).write_text(text)
        return text.strip()

    def java(cp, main_class, *values):
        return ['java', '-Xmx2g', '-ea', '-cp', cp, main_class, *values]

    classes = args.work/'audit-classes'
    classes.mkdir()
    cp = os.pathsep.join(map(str, [classes, args.dist/SINGLE, sdk, args.upstream]))
    audit_sources = [
        ROOT/'test/ApkIntegrityAudit.java',
        ROOT/'oneback/test-dex/TikTok4753ContractAudit.java',
        ROOT/'legacy/test/LegacyNativeAudit.java',
        ROOT/'legacy/test/PrivacyApkAudit.java',
        ROOT/'validation/metra/SettingsHookAudit.java',
        ROOT/'validation/metra/VerifyOriginal.java',
    ]
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes, *audit_sources], 'audit-compile', 300)
    signature = run(java(cp, 'VerifyOriginal', args.apk), 'original-signature')
    require('verified=true' in signature and 'v1=true,v2=true,v3=true' in signature
            and 'certificate_sha256='+SIGNER_SHA in signature and 'ERROR=' not in signature,
            'Original APK signature or signer differs')
    original_native = run(java(cp, 'LegacyNativeAudit', 'original', args.apk), 'original-native')
    require(original_native.splitlines()[-1].startswith('PASS LegacyNativeAudit original'), 'Original native contracts failed')
    original_oneback = run(java(cp, 'TikTok4753ContractAudit', 'original', args.apk), 'original-oneback')
    require(original_oneback.splitlines()[-1].startswith('PASS TikTok4753ContractAudit original:'), 'Original OneBack contracts failed')
    cases = []
    for case_id, artifact, settings in (
        ('standalone_native', SINGLE, False), ('aggregate_settings_ja', BUNDLE, True)
    ):
        folder = args.work/case_id
        folder.mkdir()
        output = folder/'patched.apk'
        result_file = folder/'result.json'
        selected = NATIVE + [ONEBACK] + ([JAPANESE, 'Settings'] if settings else [])
        command = ['java', '-Xmx2g', '-jar', sdk, 'patch', '--unsigned', '--exclusive',
                   '--bytecode-mode=FULL', '-o', output, '-t', folder/'temp', '-r', result_file]
        if settings:
            # Only the external Settings metadata targets 47.4.3. Our own native
            # contracts still verify 47.5.3 independently of this CLI override.
            command += ['-f', '-p', args.upstream, '-e', 'Settings']
        command += ['-p', args.dist/artifact]
        for patch in NATIVE + [ONEBACK] + ([JAPANESE] if settings else []):
            command += ['-e', patch]
        command += [args.apk]
        run(command, case_id+'-apply')
        result = json.loads(result_file.read_text())
        require(result.get('success') is True and result.get('failedPatches') == [], 'Patch failures in '+case_id)
        applied = result.get('appliedPatches', [])
        require(len(applied) == len(selected) and set(applied) == set(selected), 'Selected/applied patch mismatch in '+case_id)
        steps = result.get('stepResults', [])
        require(steps and all(x.get('success') is True for x in steps)
                and {'PATCHING', 'REBUILDING'} <= {x.get('step') for x in steps}, 'Partial pipeline in '+case_id)
        require(result.get('packageName') == 'com.zhiliaoapp.musically'
                and result.get('packageVersion') == '47.5.3' and output.is_file(), 'Wrong output identity')
        (evidence/(case_id+'-patcher-result.json')).write_bytes(json_bytes(result))
        audits = {}
        checks = [
            ('native', 'LegacyNativeAudit', ['output', args.apk, output], 'PASS LegacyNativeAudit output'),
            ('oneback', 'TikTok4753ContractAudit', ['output', output], 'PASS TikTok4753ContractAudit output:'),
            ('integrity', 'ApkIntegrityAudit', [args.apk, output], 'PASS APK inventory:'),
            ('privacy', 'PrivacyApkAudit', [args.apk, output, folder/'privacy-report.json'], 'PRIVACY_APK_AUDIT_PASSED'),
        ]
        if settings:
            checks.append(('settings_japanese', 'SettingsHookAudit', [args.apk, output], 'NATIVE_SETTINGS_HOOK_AUDIT_PASSED'))
        for key, test, parameters, marker in checks:
            text = run(java(cp, test, *parameters), case_id+'-'+key)
            last = text.splitlines()[-1]
            require(last.startswith(marker), 'Missing successful output audit '+key)
            audits[key] = marker if key in ('privacy', 'settings_japanese') else last
        shutil.copyfile(folder/'privacy-report.json', evidence/(case_id+'-privacy-report.json'))
        row = {
            'id': case_id, 'artifact': artifact, 'artifact_sha256': artifact_hashes[artifact],
            'selected_patches': selected, 'applied_patches': applied, 'failed_patches': [],
            'steps_all_success': True, 'result': 'PASS',
            'output_apk_sha256': sha(output), 'output_apk_bytes': output.stat().st_size,
            'audits': audits,
            'evidence_directory': 'validation/real-apk',
        }
        if settings:
            row.update(upstream_version='0.9.0-dev.1', upstream_sha256=UPSTREAM_SHA)
        cases.append(row)
        print('PASS actual APK case '+case_id, flush=True)
    require({name: sha(args.dist/name) for name in artifact_hashes} == artifact_hashes, 'MPP changed during verification')
    proof = {
        'schema': 'tiktok4753-real-apk-v1',
        'input': {'package': 'com.zhiliaoapp.musically', 'version_name': '47.5.3',
                  'version_code': 2024705030, 'bytes': args.apk.stat().st_size, 'sha256': ORIGINAL_SHA,
                  'signature_verified': True, 'signer_certificate_sha256': SIGNER_SHA},
        'physical_android_tested': False, 'patcher_sdk_sha256': TOOLS['morphe200.jar'],
        'original_preflight': {'native': original_native.splitlines()[-1], 'oneback': original_oneback.splitlines()[-1]},
        'cases': cases,
        'limitations': [
            'No physical Android device interaction, live service traffic, or rendered UI was tested. Native output hooks, branch behavior, host lifecycle regressions and full APK reconstruction were checked.',
            'The external Metra 0.9.0-dev.1 full 136-patch exploratory probe had 12 failures on 47.5.3. This release fixes the eight authored Hiro entries and verifies Settings plus Japanese; it does not claim to repair the external 12 patches.',
            'Preview thumbnails require thumbnail data supplied by TikTok for the selected video. Unsupported LIVE/photo/protected cases keep native seek restrictions.',
        ],
    }
    args.proof.write_bytes(json_bytes(proof))
    print('PASS both actual-APK cases; exact tested MPP hashes recorded in '+str(args.proof), flush=True)


if __name__ == '__main__':
    main()
