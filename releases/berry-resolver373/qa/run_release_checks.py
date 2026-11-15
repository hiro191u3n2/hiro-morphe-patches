#!/usr/bin/env python3
"""Run the frozen release against the user's real split APK and the older regression input."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

NAMES = ['ダブルタップから指を離さず片手ズーム',
         'プレビュー作成を後回しにして表示中タブを優先',
         '共有先ブラウザの表示を個別に切替',
         'ロックフリーMessageQueueを有効化',
         'バックフォワードキャッシュの復帰処理を補正']

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ('inputs', 'tools', 'dist', 'out'):
        parser.add_argument('--' + key, type=Path, required=True)
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=False)
    exact = args.inputs / 'berry-3.84.21/base.apk'
    older = args.inputs / 'berry-3.84.12/jp.ejimax.berrybrowser.apk'
    split = args.inputs / 'berry-3.84.21.apks'
    assert sha(exact) == '8241a1665794686b6df0f51f16b020cd994bf47e33ac4fe505e972667e2ef21a'
    assert sha(older) == '19602700d687462e1cd967c33460095c86e14bcfabb4843765368265de1cab19'
    host = args.tools / 'morphe24.jar'
    assert sha(host) == '7e2025ce8d326af883f6de8a25865da1f7ddbb72c0035c06e6765dc76c1c85c0'
    build = json.loads((args.dist / 'QA_build373.json').read_text())
    for name, row in build['artifacts'].items():
        assert sha(args.dist / name) == row['sha256']
    combined, single = 'Hiro_Morphe_Patches_v1.0.373.mpp', 'Berry_Browser_v1.4.6.mpp'
    rows = []
    for label, apk, version, bundle_name, names in [
        ('exact-combined-five', split, '3.84.21', combined, NAMES),
        ('exact-standalone-five', split, '3.84.21', single, NAMES),
        ('exact-standalone-zoom', exact, '3.84.21', single, NAMES[:1]),
        ('older-combined-five', older, '3.84.12', combined, NAMES),
    ]:
        folder = args.out / label
        folder.mkdir()
        bundle = args.dist / bundle_name
        cmd = ['java', '-jar', str(host), 'patch', str(apk), '-p', str(bundle),
               '--exclusive', '--unsigned', '--bytecode-mode', 'FULL',
               '-o', str(folder / 'patched.apk'), '-t', str(folder / 'temp'),
               '-r', str(folder / 'report.json')]
        for name in names:
            cmd += ['-e', name]
        with (folder / 'apply.log').open('w') as log:
            result = subprocess.run(cmd, stdout=log, stderr=subprocess.STDOUT, timeout=300)
        report = json.loads((folder / 'report.json').read_text())
        assert result.returncode == 0 and report['success'] and not report['failedPatches'], label
        assert report['packageVersion'] == version and set(report['appliedPatches']) == set(names), label
        assert all(row['success'] for row in report['stepResults']), label
        rows.append({'case': label, 'result': 'PASS', 'input_version': version,
                     'input_sha256': sha(apk), 'bundle_sha256': sha(bundle),
                     'selected_patches': names, 'steps': report['stepResults'],
                     'output_sha256': sha(folder / 'patched.apk')})
        print('PASS ' + label, flush=True)
    summary = {'status': 'verified-release-build', 'input_version': '3.84.21',
               'actual_3_84_21_verified': True, 'android_runtime_tested': False,
               'patcher_version': '2.4.0-dev.7', 'host_sha256': sha(host),
               'bundle_version': '1.0.373', 'berry_version': '1.4.6',
               'build': build, 'runs': rows}
    (args.out / 'result.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n')

if __name__ == '__main__':
    main()
