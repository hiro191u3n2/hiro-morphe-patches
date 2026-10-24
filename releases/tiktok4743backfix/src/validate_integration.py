#!/usr/bin/env python3
"""Verify the user's three Hiro features with every pinned Metra entry on 47.4.3.

The exact user's 120 upstream choices are unknown. This explicit 136 + 3
configuration is an additional collision and emitted Back/Settings hook audit,
not a claim about every upstream feature's behavior on an Android device.
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
from build import BUNDLE, TOOLS
from bundle_tools import json_bytes, require, sha
from validate_apk import NATIVE, ONEBACK, JAPANESE, UPSTREAM_SHA


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('apk', 'dist', 'upstream', 'tools', 'work'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    for name in vars(args):
        setattr(args, name, getattr(args, name).resolve())
    identity = json.loads((ROOT/'validation/apk-inputs.json').read_text())['47.4.3']
    upstream = json.loads((ROOT/'validation/integration/upstream-selected.json').read_text())
    hiro = [NATIVE[0], ONEBACK, JAPANESE]
    selected = upstream + hiro
    require(len(upstream) == 136 and len(set(selected)) == 139 and 'Settings' in upstream
            and 'Feed tab navigation' in upstream, 'Explicit upstream integration catalog changed')
    require(sha(args.apk) == identity['sha256'] and args.apk.stat().st_size == identity['bytes'],
            'Use the exact signed original global TikTok 47.4.3 APK')
    require(sha(args.upstream) == UPSTREAM_SHA and sha(args.tools/'morphe200.jar') == TOOLS['morphe200.jar'],
            'Integration tool or upstream bytes changed')
    require(not args.work.exists(), 'Use a fresh integration directory')
    args.work.mkdir(parents=True)
    evidence = ROOT/'validation/integration/139'
    evidence.mkdir(parents=True, exist_ok=True)
    stage = args.work/'tools'
    stage.mkdir()
    sdk = stage/'morphe.jar'
    shutil.copyfile(args.tools/'morphe200.jar', sdk)
    artifact = args.dist/BUNDLE
    artifact_hash = sha(artifact)
    output, result_file = args.work/'patched.apk', args.work/'result.json'
    command = ['java', '-Xmx3g', '-jar', sdk, 'patch', '--unsigned', '--exclusive',
               '--continue-on-error', '--bytecode-mode=FULL',
               '-o', output, '-t', args.work/'temp', '-r', result_file]
    for source, names in ((args.upstream, upstream), (artifact, hiro)):
        command += ['-p', source]
        for name in names:
            command += ['-e', name]
    command += [args.apk]
    (evidence/'command.json').write_bytes(json_bytes(list(map(str, command))))
    (evidence/'selected-patches.json').write_bytes(json_bytes(selected))

    def run(label, values, timeout=3600):
        print('RUN integration139 '+label, flush=True)
        log_path = args.work/(label+'.log')
        with log_path.open('w') as stream:
            process = subprocess.run(list(map(str, values)), stdout=stream, stderr=subprocess.STDOUT, timeout=timeout)
        shutil.copyfile(log_path, evidence/log_path.name)
        text = log_path.read_text()
        require(process.returncode == 0, 'Integration command failed: '+label+'\n'+text[-12000:])
        return text.strip()

    run('apply', command)
    require(result_file.is_file(), 'Missing integration patcher result')
    result = json.loads(result_file.read_text())
    shutil.copyfile(result_file, evidence/'patcher-result.json')
    applied = result.get('appliedPatches', [])
    require(result.get('success') is True and result.get('failedPatches') == []
            and len(applied) == 139 and set(applied) == set(selected), 'Incomplete 139-patch integration')
    steps = result.get('stepResults', [])
    require(steps and all(row.get('success') is True for row in steps)
            and {'PATCHING', 'REBUILDING'} <= {row.get('step') for row in steps}, 'Partial integration pipeline')
    require(result.get('packageName') == identity['package'] and result.get('packageVersion') == '47.4.3'
            and output.is_file(), 'Wrong integration output identity')
    classes = args.work/'audit-classes'
    classes.mkdir()
    cp = os.pathsep.join(map(str, [classes, artifact, sdk, args.upstream]))
    sources = [ROOT/'oneback/test-dex/TikTok4753ContractAudit.java', ROOT/'validation/metra/SettingsHookAudit.java']
    run('audit-compile', ['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes, *sources], 300)
    oneback = run('oneback', ['java', '-Xmx2g', '-ea', '-cp', cp, 'TikTok4753ContractAudit', 'output', output, '47.4.3'])
    require(oneback.splitlines()[-1].startswith('PASS TikTok4753ContractAudit output:'), 'Missing exact native Back hook proof')
    japanese = run('settings-japanese', ['java', '-Xmx2g', '-ea', '-cp', cp, 'SettingsHookAudit', args.apk, output])
    require(japanese.splitlines()[-1] == 'NATIVE_SETTINGS_HOOK_AUDIT_PASSED', 'Missing native Settings/Japanese connection')
    require(sha(artifact) == artifact_hash, 'Integration artifact changed')
    report = {
        'schema': 'tiktok4743-backfix-integration-v1', 'input_sha256': identity['sha256'],
        'upstream_sha256': UPSTREAM_SHA, 'artifact': BUNDLE, 'artifact_sha256': artifact_hash,
        'selected_patches': selected, 'applied_patches': applied, 'failed_patches': [],
        'steps_all_success': True, 'result': 'PASS',
        'output_apk_sha256': sha(output), 'output_apk_bytes': output.stat().st_size,
        'audits': {'oneback': oneback.splitlines()[-1], 'settings_japanese': 'NATIVE_SETTINGS_HOOK_AUDIT_PASSED'},
        'max_heap': '3g', 'force_compatibility': False, 'default_options_used': True,
        'physical_android_tested': False, 'user_exact_selection_reproduced': False,
        'evidence_directory': 'validation/integration/139',
        'scope': 'All 139 patches applied and rebuilt; exact native Back gates, retained guards and Japanese Settings hooks audited. No physical device or all-feature behavior claim.',
    }
    (ROOT/'validation/integration-139.json').write_bytes(json_bytes(report))
    shutil.rmtree(args.work/'temp', ignore_errors=True)
    print('PASS 139-patch real 47.4.3 integration and native Back/Settings hook audits', flush=True)


if __name__ == '__main__':
    main()
