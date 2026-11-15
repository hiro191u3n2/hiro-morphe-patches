#!/usr/bin/env python3
"""Check the unreleased resolver candidate on the authenticated Berry 3.84.12 input."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

NAMES = [
    'ダブルタップから指を離さず片手ズーム',
    'プレビュー作成を後回しにして表示中タブを優先',
    '共有先ブラウザの表示を個別に切替',
    'ロックフリーMessageQueueを有効化',
]

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('apk', 'host', 'dist', 'out'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    assert sha(args.apk) == '19602700d687462e1cd967c33460095c86e14bcfabb4843765368265de1cab19'
    assert sha(args.host) == '7e2025ce8d326af883f6de8a25865da1f7ddbb72c0035c06e6765dc76c1c85c0'
    args.out.mkdir(parents=True, exist_ok=False)
    rows = []
    for label, filename, names in (
        ('combined-four', 'Hiro_Morphe_Patches_v1.0.373.mpp', NAMES),
        ('standalone-zoom', 'Berry_Browser_v1.4.6.mpp', NAMES[:1]),
    ):
        folder = args.out / label
        folder.mkdir()
        bundle = args.dist / filename
        command = ['java', '-jar', str(args.host), 'patch', str(args.apk), '-p', str(bundle),
                   '--exclusive', '--unsigned', '--bytecode-mode', 'FULL',
                   '-o', str(folder / 'patched.apk'), '-t', str(folder / 'temp'),
                   '-r', str(folder / 'report.json')]
        for name in names:
            command += ['-e', name]
        with (folder / 'apply.log').open('w') as log:
            result = subprocess.run(command, stdout=log, stderr=subprocess.STDOUT, timeout=300)
        report = json.loads((folder / 'report.json').read_text())
        assert result.returncode == 0 and report['success'] and not report['failedPatches'], label
        assert set(report['appliedPatches']) == set(names), label
        assert report['packageVersion'] == '3.84.12', label
        assert all(row['success'] for row in report['stepResults']), label
        rows.append({'case': label, 'result': 'PASS', 'bundle_sha256': sha(bundle),
                     'selected_patches': names, 'output_sha256': sha(folder / 'patched.apk'),
                     'steps': report['stepResults']})
        print('PASS ' + label, flush=True)
    summary = {'status': 'unreleased-candidate', 'input_version': '3.84.12',
               'input_sha256': sha(args.apk), 'patcher_version': '2.4.0-dev.7',
               'host_sha256': sha(args.host), 'actual_3_84_21_verified': False,
               'android_runtime_tested': False, 'runs': rows}
    (args.out / 'result.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n')

if __name__ == '__main__':
    main()
