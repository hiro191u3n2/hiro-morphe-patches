#!/usr/bin/env python3
"""Freeze two distinct fresh local builds; no remote reads or writes."""
from pathlib import Path
import argparse, importlib.util, json
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--first',type=Path,required=True)
parser.add_argument('--second',type=Path,required=True)
args=parser.parse_args()
root=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('publication',root/'publish.py')
pub=importlib.util.module_from_spec(spec);spec.loader.exec_module(pub)
first,second=args.first.resolve(),args.second.resolve()
pub.require(first!=second,'The local builds must have separate output directories')
for name in (pub.SINGLE,pub.COMBINED,pub.QA_NAME):
    pub.require((first/name).read_bytes()==(second/name).read_bytes(),'Fresh local builds differ: '+name)
qa_raw=(first/pub.QA_NAME).read_bytes();qa=json.loads(qa_raw);pub.validate_qa(qa)
for name,row in qa['artifacts'].items():
    raw=(first/name).read_bytes()
    pub.require(row=={'bytes':len(raw),'sha256':pub.sha(raw)},'Artifact changed: '+name)
cfg=json.loads((root/'manifest-template.json').read_text())
pub.require(cfg['schema']=='tiktok-japanese100-publication-v1','Wrong publication template')
cfg.update(freeze_state='frozen',local_build_pair_verified=True,local_expected_mpp=qa['artifacts'],
           local_expected_qa_sha256=pub.sha(qa_raw),local_expected_host=qa['host'],ci_build_pair_verified=False)
(root/'manifest-template.json').write_bytes(pub.json_bytes(cfg))
cfg.update(expected_mpp=qa['artifacts'],expected_qa_sha256=pub.sha(qa_raw),ci_run_id=None,ci_source_commit=None)
(root/'manifest.json').write_bytes(pub.json_bytes(cfg))
print('PASS frozen two fresh local MPP/QA builds, complete Japanese catalog and executed checks')
