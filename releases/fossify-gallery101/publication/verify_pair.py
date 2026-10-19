#!/usr/bin/env python3
"""Require two fresh CI builds to match reviewed local MPP and QA bytes."""
import importlib.util,json,os,sys
from pathlib import Path
first,second=map(Path,sys.argv[1:3]);root=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('publication',root/'publish.py');pub=importlib.util.module_from_spec(spec);spec.loader.exec_module(pub)
cfg=json.loads((root/'manifest-template.json').read_text())
pub.require(cfg.get('freeze_state')=='frozen','Local build is not frozen')
pub.require(cfg['source_sha256']=={name:pub.sha(raw) for name,raw in pub.reviewed_files().items()},'Source changed after review')
for name in (pub.SINGLE,pub.COMBINED,pub.QA_NAME):pub.require((first/name).read_bytes()==(second/name).read_bytes(),'Independent builds differ: '+name)
qa_raw=(first/pub.QA_NAME).read_bytes();qa=json.loads(qa_raw);pub.validate_qa(qa,cfg)
pub.require(qa['artifacts']==cfg['local_expected_mpp'] and pub.sha(qa_raw)==cfg['local_expected_qa_sha256'],'CI build differs from local build')
for name,row in qa['artifacts'].items():
    raw=(first/name).read_bytes();pub.require(row=={'bytes':len(raw),'sha256':pub.sha(raw)},'Current artifact bytes differ: '+name)
verified=os.environ.get('GITHUB_ACTIONS')=='true' and bool(os.environ.get('GITHUB_RUN_ID')) and bool(os.environ.get('GITHUB_SHA'))
cfg.update(expected_mpp=qa['artifacts'],expected_qa_sha256=pub.sha(qa_raw),ci_build_pair_verified=bool(verified),ci_run_id=os.environ.get('GITHUB_RUN_ID') if verified else None,ci_source_commit=os.environ.get('GITHUB_SHA') if verified else None)
(root/'manifest.json').write_bytes(pub.json_bytes(cfg))
print('PASS both fresh builds and locally frozen MPP/QA are byte-identical')
