#!/usr/bin/env python3
"""Apply final deliverables to the actual APKM with Morphe Patcher 2.4.0."""
from pathlib import Path
import argparse, json, subprocess

NAMES = [
    'NewX: タイムラインのカテゴリ設定を再起動後も保持',
    'NewX: ポストの日時を絶対時間・24時間表記にする',
    'NewX: 戻る1回で終了・履歴削除',
]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--workspace', type=Path, required=True)
    args = parser.parse_args()
    root = args.workspace.resolve()
    cases = [('hiro-only', 'NewX_Hiro_Addons_v1.0.0.mpp', False, False),
             ('hiro-piko', 'Hiro_Morphe_Patches_v1.0.377.mpp', True, False),
             ('hiro-piko-clone', 'Hiro_Morphe_Patches_v1.0.377.mpp', True, True)]
    piko = json.loads((root / 'verification/piko-selected.json').read_text())
    for label, bundle, combine, clone in cases:
        cmd = ['java', '-XX:-UsePerfData', '-Xmx3g', '-jar', str(root / 'tools/morphe.jar'), 'patch',
               str(root / 'inputs/X_12.43.0-prod.01.apkm'), '--exclusive', '--unsigned', '--bytecode-mode', 'FULL',
               '-o', str(root / 'verification' / (label + '.apk')),
               '-r', str(root / 'verification' / (label + '-result.json')),
               '-t', str(root / 'verification' / (label + '-work')),
               '-p', str(root / 'dist' / bundle)]
        for name in NAMES:
            cmd += ['-e', name]
        if combine:
            cmd += ['-p', str(root / 'inputs/piko-3.12.0-dev.1.mpp')]
            for name in piko + (['NewX: Clone'] if clone else []):
                cmd += ['-e', name]
        log = root / 'verification' / (label + '.log')
        print('START ' + label, flush=True)
        with log.open('w') as f:
            result = subprocess.run(cmd, stdout=f, stderr=subprocess.STDOUT)
        print('EXIT ' + label + ': ' + str(result.returncode), flush=True)
        if result.returncode:
            print(log.read_text()[-10000:], flush=True)
            raise SystemExit(result.returncode)
        data = json.loads((root / 'verification' / (label + '-result.json')).read_text())
        print(json.dumps(data, ensure_ascii=False)[:300], flush=True)

if __name__ == '__main__':
    main()
