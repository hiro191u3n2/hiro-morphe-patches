#!/usr/bin/env python3
"""Apply the exact release MPPs to either signed, pinned original TikTok APK.

Run once per version in separate work directories. Then merge_apk_proofs.py
requires all four successful cases and matches the same MPP bytes. APKs remain
private local test inputs. No physical Android interaction is claimed.
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

INPUTS = json.loads((ROOT/'validation/apk-inputs.json').read_text())
UPSTREAM_SHA = '45dc0c2244f8cf6973de18542d2d9a0cc25512f5b1de52438c685b82edb7c4cd'
NATIVE = [
    'TikTok 怪しい通信だけ遮断（TikTok 47.4.3・47.5.3）',
    'フィード左下の文字・再投稿欄を非表示（TikTok 47.4.3・47.5.3）',
    'フィード下部のCapCut・エフェクト誘導枠を完全遮断（TikTok 47.4.3・47.5.3）',
    '動画のシークバーを常時表示（TikTok 47.4.3・47.5.3）',
    'シーク中の動画プレビューサムネイルを表示（TikTok 47.4.3・47.5.3）',
    'フォロー中のストーリーズ件数を非表示（TikTok 47.4.3・47.5.3）',
]
ONEBACK = '戻る1回で終了・履歴削除'
JAPANESE = 'TikTok設定を日本語化'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('apk', 'dist', 'upstream', 'tools', 'work'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--version', choices=sorted(INPUTS), required=True)
    parser.add_argument('--proof', type=Path)
    args = parser.parse_args()
    for name in ('apk', 'dist', 'upstream', 'tools', 'work'):
        setattr(args, name, getattr(args, name).resolve())
    version = args.version
    identity = INPUTS[version]
    abi = version.replace('.', '')
    args.proof = (args.proof or ROOT/'validation'/('real-apk-'+abi+'.json')).resolve()
    require(sha(args.apk) == identity['sha256'] and args.apk.stat().st_size == identity['bytes'],
            'Use the exact signed original global '+version+' APK')
    require(sha(args.upstream) == UPSTREAM_SHA, 'Metra Settings input changed')
    require(sha(args.tools/'morphe200.jar') == TOOLS['morphe200.jar'], 'Patcher 2.0 SDK changed')
    require(not args.work.exists(), 'Use a fresh actual-APK work directory')
    args.work.mkdir(parents=True)
    evidence = args.proof.parent/'real-apk'/abi
    evidence.mkdir(parents=True, exist_ok=True)
    artifact_hashes = {name: sha(args.dist/name) for name in (SINGLE, BUNDLE)}
    stage = args.work/'tools'
    stage.mkdir()
    shutil.copyfile(args.tools/'morphe200.jar', stage/'morphe.jar')
    sdk = stage/'morphe.jar'

    def run(command, label, timeout=3600):
        print('RUN '+abi+' '+label, flush=True)
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
            and 'certificate_sha256='+identity['signer_certificate_sha256'] in signature and 'ERROR=' not in signature,
            'Original APK signature or signer differs')
    original_native = run(java(cp, 'LegacyNativeAudit', 'original', args.apk), 'original-native')
    require(original_native.splitlines()[-1].startswith('PASS LegacyNativeAudit original'), 'Original native contracts failed')
    original_oneback = run(java(cp, 'TikTok4753ContractAudit', 'original', args.apk, version), 'original-oneback')
    require(original_oneback.splitlines()[-1].startswith('PASS TikTok4753ContractAudit original:'), 'Original OneBack contracts failed')
    cases = []
    for case_name, artifact, settings in (
        ('standalone_native', SINGLE, False), ('aggregate_settings_ja', BUNDLE, True)
    ):
        case_id = abi+'_'+case_name
        folder = args.work/case_name
        folder.mkdir()
        output = folder/'patched.apk'
        result_file = folder/'result.json'
        selected = NATIVE + [ONEBACK] + ([JAPANESE, 'Settings'] if settings else [])
        command = ['java', '-Xmx2g', '-jar', sdk, 'patch', '--unsigned', '--exclusive',
                   '--bytecode-mode=FULL', '-o', output, '-t', folder/'temp', '-r', result_file]
        if settings:
            # Metra 0.9.0-dev.1 declares 47.4.3. Only the 47.5.3 retained
            # compatibility test needs a CLI override; own contracts still run.
            if version == '47.5.3':
                command.append('-f')
            command += ['-p', args.upstream, '-e', 'Settings']
        command += ['-p', args.dist/artifact]
        for patch in NATIVE + [ONEBACK] + ([JAPANESE] if settings else []):
            command += ['-e', patch]
        command += [args.apk]
        run(command, case_name+'-apply')
        result = json.loads(result_file.read_text())
        require(result.get('success') is True and result.get('failedPatches') == [], 'Patch failures in '+case_id)
        applied = result.get('appliedPatches', [])
        require(len(applied) == len(selected) and set(applied) == set(selected), 'Selected/applied patch mismatch in '+case_id)
        steps = result.get('stepResults', [])
        require(steps and all(x.get('success') is True for x in steps)
                and {'PATCHING', 'REBUILDING'} <= {x.get('step') for x in steps}, 'Partial pipeline in '+case_id)
        require(result.get('packageName') == identity['package']
                and result.get('packageVersion') == version and output.is_file(), 'Wrong output identity')
        (evidence/(case_name+'-patcher-result.json')).write_bytes(json_bytes(result))
        audits = {}
        checks = [
            ('native', 'LegacyNativeAudit', ['output', args.apk, output], 'PASS LegacyNativeAudit output'),
            ('oneback', 'TikTok4753ContractAudit', ['output', output, version], 'PASS TikTok4753ContractAudit output:'),
            ('integrity', 'ApkIntegrityAudit', [args.apk, output], 'PASS APK inventory:'),
            ('privacy', 'PrivacyApkAudit', [args.apk, output, folder/'privacy-report.json'], 'PRIVACY_APK_AUDIT_PASSED'),
        ]
        if settings:
            checks.append(('settings_japanese', 'SettingsHookAudit', [args.apk, output], 'NATIVE_SETTINGS_HOOK_AUDIT_PASSED'))
        for key, test, parameters, marker in checks:
            text = run(java(cp, test, *parameters), case_name+'-'+key)
            last = text.splitlines()[-1]
            require(last.startswith(marker), 'Missing successful output audit '+key)
            audits[key] = marker if key in ('privacy', 'settings_japanese') else last
        output_sha = sha(output)
        privacy = json.loads((folder/'privacy-report.json').read_text())
        require(privacy.get('success') is True and privacy.get('original_sha256') == identity['sha256']
                and privacy.get('patched_sha256') == output_sha, 'Privacy evidence does not describe this input/output')
        shutil.copyfile(folder/'privacy-report.json', evidence/(case_name+'-privacy-report.json'))
        row = {
            'id': case_id, 'version': version, 'artifact': artifact, 'artifact_sha256': artifact_hashes[artifact],
            'input_sha256': identity['sha256'],
            'selected_patches': selected, 'applied_patches': applied, 'failed_patches': [],
            'steps_all_success': True, 'result': 'PASS',
            'output_apk_sha256': output_sha, 'output_apk_bytes': output.stat().st_size,
            'audits': audits, 'evidence_directory': 'validation/real-apk/'+abi,
        }
        if settings:
            row.update(upstream_version='0.9.0-dev.1', upstream_sha256=UPSTREAM_SHA)
        cases.append(row)
        print('PASS actual APK case '+case_id, flush=True)
    require({name: sha(args.dist/name) for name in artifact_hashes} == artifact_hashes, 'MPP changed during verification')
    proof = {
        'schema': 'tiktok4743-real-apk-part-v1', 'version': version, 'input': identity,
        'physical_android_tested': False, 'patcher_sdk_sha256': TOOLS['morphe200.jar'],
        'original_preflight': {'native': original_native.splitlines()[-1], 'oneback': original_oneback.splitlines()[-1]},
        'cases': cases,
    }
    args.proof.write_bytes(json_bytes(proof))
    print('PASS both actual-APK cases for '+version+'; exact tested MPP hashes recorded in '+str(args.proof), flush=True)


if __name__ == '__main__':
    main()
