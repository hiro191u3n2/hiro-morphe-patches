#!/usr/bin/env python3
"""Require both shipped MPPs to reject all eight entries on real TikTok 47.5.3.

This deliberately bypasses Manager's metadata filtering. Admission failures must
come from the production version guards, with no applied entry or output APK.
The unsupported APK remains a private local negative test input.
"""
from pathlib import Path
import argparse
import json
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
from build import SINGLE, BUNDLE, TOOLS
from bundle_tools import json_bytes, require, sha
from validate_apk import NATIVE, ONEBACK, JAPANESE

REASONS = re.compile(r'requires the original global TikTok 47\.4\.3|47\.4\.3専用|unsupported TikTok 47\.5\.3')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('apk', 'dist', 'tools', 'work'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    for name in vars(args):
        setattr(args, name, getattr(args, name).resolve())
    identity = json.loads((ROOT/'validation/unsupported-apk-inputs.json').read_text())['47.5.3']
    require(sha(args.apk) == identity['sha256'] and args.apk.stat().st_size == identity['bytes'],
            'Use the exact previously signed/pinned global 47.5.3 input for rejection')
    require(sha(args.tools/'morphe200.jar') == TOOLS['morphe200.jar'], 'Patcher 2.0 tool changed')
    require(not args.work.exists(), 'Use a fresh rejection directory')
    args.work.mkdir(parents=True)
    stage = args.work/'tools'
    stage.mkdir()
    sdk = stage/'morphe.jar'
    shutil.copyfile(args.tools/'morphe200.jar', sdk)
    evidence = ROOT/'validation/rejection/4753'
    evidence.mkdir(parents=True, exist_ok=True)
    selected = NATIVE + [ONEBACK, JAPANESE]
    hashes = {name: sha(args.dist/name) for name in (SINGLE, BUNDLE)}
    cases = []
    for label, artifact in (('standalone', SINGLE), ('aggregate', BUNDLE)):
        attempts = []
        # Morphe --continue-on-error intentionally rebuilds even after all
        # selections fail. Exercise each entry with ordinary fail-fast handling
        # so every rejected target also proves that no output APK is produced.
        for index, patch in enumerate(selected):
            attempt_id = label+'-'+str(index+1)
            folder = args.work/attempt_id
            folder.mkdir()
            output, result_file = folder/'must-not-be-created.apk', folder/'result.json'
            command = ['java', '-Xmx2g', '-jar', sdk, 'patch', '--unsigned', '--exclusive',
                       '--bytecode-mode=FULL', '-f', '-o', output, '-t', folder/'temp',
                       '-r', result_file, '-p', args.dist/artifact, '-e', patch, args.apk]
            (folder/'command.json').write_bytes(json_bytes(list(map(str, command))))
            print('RUN forced 47.5.3 rejection: '+attempt_id+' '+patch, flush=True)
            with (folder/'apply.log').open('w') as log:
                process = subprocess.run(list(map(str, command)), stdout=log, stderr=subprocess.STDOUT, timeout=1800)
            for name in ('command.json', 'apply.log', 'result.json'):
                if (folder/name).is_file():
                    shutil.copyfile(folder/name, evidence/(attempt_id+'-'+name))
            require(result_file.is_file(), 'CLI did not record its rejection result: '+attempt_id)
            result = json.loads(result_file.read_text())
            require(result.get('packageName') == identity['package'] and result.get('packageVersion') == '47.5.3',
                    'CLI did not inspect the unsupported original input')
            failures = result.get('failedPatches', [])
            require(len(failures) == 1 and failures[0].get('name') == patch,
                    'The explicitly selected entry did not reject the forced input')
            message = failures[0].get('error', '')
            require(isinstance(message, str) and REASONS.search(message),
                    'A missing feature, crash or fingerprint failure is not a version-admission rejection')
            require(process.returncode > 0 and result.get('success') is False,
                    'Fail-fast rejection must be reported as failure')
            require(result.get('appliedPatches') == [], 'A patch was applied to removed 47.5.3')
            require(not output.exists(), 'The forced unsupported entry produced an APK')
            attempts.append({'patch': patch, 'process_exit_code': process.returncode,
                             'reported_top_level_success': False, 'applied_patches': [],
                             'rejected_patches': [patch], 'output_apk_created': False,
                             'failure_message': message})
            # Logs and the failure result are the evidence. Decoded private APK
            # intermediates are reproducible and need not accumulate 16 times.
            shutil.rmtree(folder/'temp', ignore_errors=True)
        codes = {attempt['process_exit_code'] for attempt in attempts}
        require(len(codes) == 1, 'Inconsistent fail-fast process outcomes')
        names = [attempt['patch'] for attempt in attempts]
        messages = [attempt['failure_message'] for attempt in attempts]
        cases.append({
            'id': '4753_'+label+'_rejected', 'version': '47.5.3', 'artifact': artifact,
            'artifact_sha256': hashes[artifact], 'input_sha256': identity['sha256'],
            'forced_selection': True, 'selected_patches': selected,
            'rejected_patches': names, 'applied_patches': [], 'result': 'REJECTED',
            'output_apk_created': False, 'process_exit_code': next(iter(codes)),
            'reported_top_level_success': False, 'selection_mode': 'individual_fail_fast', 'attempts': attempts,
            'failure_messages': messages, 'evidence_directory': 'validation/rejection/4753',
        })
        print('PASS all eight forced entries rejected: '+label, flush=True)
    require({name: sha(args.dist/name) for name in hashes} == hashes, 'Tested MPP changed')
    report = {
        'schema': 'tiktok4743-backfix-rejection-v1', 'input': identity,
        'physical_android_tested': False, 'patcher_sdk_sha256': TOOLS['morphe200.jar'],
        'cases': cases,
    }
    (ROOT/'validation/rejection-apk-4753.json').write_bytes(json_bytes(report))
    print('PASS real 47.5.3 forced-selection rejection, exact shipped MPPs', flush=True)


if __name__ == '__main__':
    main()
