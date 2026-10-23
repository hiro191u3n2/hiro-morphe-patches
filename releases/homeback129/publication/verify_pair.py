#!/usr/bin/env python3
"""Verify two fresh builds against each other and the reviewed local freeze."""
from pathlib import Path
import importlib.util, json, os, sys

first, second = map(Path, sys.argv[1:3])
root = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('publication', root / 'publish.py')
pub = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pub)
for name in (pub.SINGLE, pub.COMBINED, pub.QA_NAME):
    pub.require((first / name).read_bytes() == (second / name).read_bytes(), 'Fresh builds differ: ' + name)
cfg = json.loads((root / 'manifest-template.json').read_text())
pub.require(cfg.get('freeze_state') == 'frozen', 'Local build is not frozen')
qa_raw = (first / pub.QA_NAME).read_bytes()
qa = json.loads(qa_raw)
pub.validate_qa(qa)
pub.validate_scope_evidence(qa, cfg)
pub.require(qa['host'] == cfg['local_expected_host'], 'CI host summaries differ')
pub.require(qa['artifacts'] == cfg['local_expected_mpp'], 'CI artifacts differ from local freeze')
pub.require(pub.sha(qa_raw) == cfg['local_expected_qa_sha256'], 'CI QA differs from local freeze')
for name, row in qa['artifacts'].items():
    raw = (first / name).read_bytes()
    pub.require(row == {'bytes': len(raw), 'sha256': pub.sha(raw)}, 'Artifact identity differs: ' + name)
cfg.update(expected_mpp=qa['artifacts'], expected_qa_sha256=pub.sha(qa_raw))
verified = os.environ.get('GITHUB_ACTIONS') == 'true' and bool(os.environ.get('GITHUB_RUN_ID')) and bool(os.environ.get('GITHUB_SHA'))
cfg.update(ci_build_pair_verified=verified, ci_run_id=os.environ.get('GITHUB_RUN_ID') if verified else None,
           ci_source_commit=os.environ.get('GITHUB_SHA') if verified else None)
(root / 'manifest.json').write_bytes(pub.json_bytes(cfg))
print('PASS two fresh SDK builds and local frozen MPP/QA are byte-identical')
