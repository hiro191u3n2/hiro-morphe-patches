#!/usr/bin/env python3
"""Bind completed public synthetic scope tests to the exact release; no remote writes."""
from pathlib import Path
import argparse, importlib.util, json

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--dist', type=Path, required=True)
a = p.parse_args()
root = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('publication', root / 'publish.py')
pub = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pub)
qa_raw = (a.dist / pub.QA_NAME).read_bytes()
qa = json.loads(qa_raw)
pub.validate_qa(qa)
for name, row in qa['artifacts'].items():
    raw = (a.dist / name).read_bytes()
    pub.require(row == {'bytes': len(raw), 'sha256': pub.sha(raw)}, 'Artifact changed: ' + name)
evidence = {
    'schema': 'homeback129-local-scope-validation-v1', 'result': 'PASS',
    'reported_package': 'com.zhiliaoapp.musically', 'reported_version': '47.4.3',
    'original_apk_apply_tested': False, 'physical_android_tested': False,
    'baseline_version': qa['baseline_version'], 'baseline_sha256': qa['baseline_sha256'],
    'artifacts': qa['artifacts'], 'qa_sha256': pub.sha(qa_raw),
    'runtime_sha256': qa['runtime_sha256'],
    'baseline_runtime_sha256': qa['baseline_runtime_sha256'],
    'runtime_preserved': qa['runtime_preserved'],
    'app_scope': qa['app_scope'], 'synthetic': qa['synthetic'],
    'limitations': [
        'TikTok 47.4.3 original APK was unavailable; no actual TikTok APK application or installation was performed.',
        'The reported exception was reproduced with a purpose-built DEX collision fixture and the prior published patch.',
        'The user selection of 123 patches across three patch sources was not recreated.',
        'No Android 17 device navigation, process exit or recent-task-card behavior was tested.'
    ]
}
(root / pub.LOCAL_SCOPE_EVIDENCE).write_bytes(pub.json_bytes(evidence))
pub.validate_scope_evidence(qa)
print('PASS current collision scope evidence bound to final QA, 2 MPPs and 30 synthetic pipelines; actual APK/device untested')
