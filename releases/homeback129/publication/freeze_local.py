#!/usr/bin/env python3
"""Freeze completed SDK artifacts and local synthetic collision evidence; no remote writes."""
from pathlib import Path
import argparse, hashlib, importlib.util, json

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--dist', type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('publication', root / 'publish.py')
pub = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pub)
qa_raw = (args.dist / pub.QA_NAME).read_bytes()
qa = json.loads(qa_raw)
pub.validate_qa(qa)
pub.validate_scope_evidence(qa)
for name, row in qa['artifacts'].items():
    raw = (args.dist / name).read_bytes()
    pub.require(row == {'bytes': len(raw), 'sha256': pub.sha(raw)}, 'Artifact changed: ' + name)
cfg = json.loads((root / 'manifest-template.json').read_text())
cfg.update(freeze_state='frozen', local_expected_mpp=qa['artifacts'],
           local_expected_qa_sha256=pub.sha(qa_raw), local_expected_host=qa['host'],
           local_scope_evidence_sha256=pub.sha((root / pub.LOCAL_SCOPE_EVIDENCE).read_bytes()))
(root / 'manifest-template.json').write_bytes(pub.json_bytes(cfg))
cfg.update(expected_mpp=qa['artifacts'], expected_qa_sha256=pub.sha(qa_raw), ci_build_pair_verified=False)
(root / 'manifest.json').write_bytes(pub.json_bytes(cfg))
print('PASS frozen SDK artifact hashes, host results, QA and synthetic collision scope evidence')
