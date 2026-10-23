#!/usr/bin/env python3
"""Require CI to reproduce the exact MPPs tested on the signed original APK."""
from pathlib import Path
import argparse, importlib.util, json, os

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('first', type=Path)
parser.add_argument('second', type=Path)
parser.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[3])
args = parser.parse_args()
first, second, repo = args.first.resolve(), args.second.resolve(), args.repo.resolve()
root = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('publication', root/'publish.py')
pub = importlib.util.module_from_spec(spec); spec.loader.exec_module(pub)
pub.require(first != second, 'The fresh builds must have distinct output directories')
for name in (pub.SINGLE, pub.COMBINED, pub.QA_NAME):
    pub.require((first/name).read_bytes() == (second/name).read_bytes(), 'Fresh builds differ: '+name)
cfg = json.loads((root/'manifest-template.json').read_text())
pub.require(cfg.get('schema') == 'tiktok4753-publication-v1' and cfg.get('freeze_state') == 'frozen'
            and cfg.get('local_build_pair_verified') is True, 'Local builds and actual-APK evidence not frozen')
qa_raw = (first/pub.QA_NAME).read_bytes(); qa = json.loads(qa_raw)
pub.validate_qa(qa)
pub.require(qa['host'] == cfg['local_expected_host'], 'CI host result differs')
pub.require(qa['artifacts'] == cfg['local_expected_mpp'], 'CI artifacts differ from real-APK-tested local MPPs')
pub.require(pub.sha(qa_raw) == cfg['local_expected_qa_sha256'], 'CI QA differs from local freeze')
pub.require(pub.inherited_source_ids(repo) == cfg['inherited_source_sha256']
            and cfg['japanese_source_commit'] == pub.JAPANESE_SOURCE_COMMIT, 'Inherited corresponding source differs')
for name, row in qa['artifacts'].items():
    raw = (first/name).read_bytes()
    pub.require(row == {'bytes': len(raw), 'sha256': pub.sha(raw)}, 'Artifact identity differs: '+name)
real_raw = (pub.SOURCE_ROOT/'src/validation/real-apk.json').read_bytes()
pub.require(pub.sha(real_raw) == cfg['local_expected_real_apk_sha256'], 'Real-APK evidence changed after freeze')
pub.validate_real_apk(json.loads(real_raw), qa['artifacts'])
verified = (os.environ.get('GITHUB_ACTIONS') == 'true' and bool(os.environ.get('GITHUB_RUN_ID'))
            and bool(os.environ.get('GITHUB_SHA')))
cfg.update(expected_mpp=qa['artifacts'], expected_qa_sha256=pub.sha(qa_raw),
           expected_real_apk_sha256=pub.sha(real_raw), ci_build_pair_verified=verified,
           ci_run_id=os.environ.get('GITHUB_RUN_ID') if verified else None,
           ci_source_commit=os.environ.get('GITHUB_SHA') if verified else None)
(root/'manifest.json').write_bytes(pub.json_bytes(cfg))
print('PASS two fresh builds reproduce frozen local MPP/QA and both actual-APK proofs exactly')
