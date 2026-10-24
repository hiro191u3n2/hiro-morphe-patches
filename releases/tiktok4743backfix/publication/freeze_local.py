#!/usr/bin/env python3
"""Freeze two fresh local builds only after their exact MPP bytes pass real-APK checks."""
from pathlib import Path
import argparse, importlib.util, json, re

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--first', type=Path, required=True)
parser.add_argument('--second', type=Path, required=True)
parser.add_argument('--repo', type=Path, required=True)
parser.add_argument('--reviewed-main')
parser.add_argument('--reviewed-dev')
args = parser.parse_args()
root = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('publication', root/'publish.py')
pub = importlib.util.module_from_spec(spec); spec.loader.exec_module(pub)
first, second, repo = args.first.resolve(), args.second.resolve(), args.repo.resolve()
pub.require(first != second, 'The local builds must have separate output directories')
for name in (pub.SINGLE, pub.COMBINED, pub.QA_NAME):
    pub.require((first/name).read_bytes() == (second/name).read_bytes(), 'Fresh local builds differ: '+name)
qa_raw = (first/pub.QA_NAME).read_bytes(); qa = json.loads(qa_raw)
pub.validate_qa(qa)
for name, row in qa['artifacts'].items():
    raw = (first/name).read_bytes()
    pub.require(row == {'bytes': len(raw), 'sha256': pub.sha(raw)}, 'Artifact changed: '+name)
real_raw = (pub.SOURCE_ROOT/'src/validation/real-apk.json').read_bytes()
pub.validate_real_apk(json.loads(real_raw), qa['artifacts'])
cfg = json.loads((root/'manifest-template.json').read_text())
pub.require(cfg['schema'] == 'tiktok4743-backfix-publication-v1', 'Wrong publication template')
for field, value in (('reviewed_main_head', args.reviewed_main), ('reviewed_dev_head', args.reviewed_dev)):
    if value is not None: cfg[field] = value
    pub.require(re.fullmatch('[0-9a-f]{40}', cfg[field]) is not None, 'Invalid reviewed branch head: '+field)
cfg.update(freeze_state='frozen', local_build_pair_verified=True,
           local_expected_mpp=qa['artifacts'], local_expected_qa_sha256=pub.sha(qa_raw),
           local_expected_real_apk_sha256=pub.sha(real_raw), local_expected_host=qa['host'],
           inherited_source_sha256=pub.inherited_source_ids(repo),
           japanese_source_commit=pub.JAPANESE_SOURCE_COMMIT,
           ci_build_pair_verified=False, original_apk_apply_tested=True, original_apk_rejection_tested=True,
           integration_139_verified=True,
           physical_android_tested=False)
(root/'manifest-template.json').write_bytes(pub.json_bytes(cfg))
cfg.update(expected_mpp=qa['artifacts'], expected_qa_sha256=pub.sha(qa_raw),
           expected_real_apk_sha256=pub.sha(real_raw), ci_run_id=None, ci_source_commit=None)
(root/'manifest.json').write_bytes(pub.json_bytes(cfg))
print('PASS frozen two fresh MPP/QA builds, two 47.4.3 apply proofs, 139-patch integration and two forced 47.5.3 rejection proofs; no Android device claim')
