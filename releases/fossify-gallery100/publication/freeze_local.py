#!/usr/bin/env python3
"""Freeze reviewed source, official APK identity and locally validated output bytes."""
import argparse,importlib.util,json,shutil
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--dist',type=Path,required=True);parser.add_argument('--official-apk-sha256',required=True)
parser.add_argument('--main-head',required=True);parser.add_argument('--dev-head',required=True)
args=parser.parse_args();root=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('publication',root/'publish.py');pub=importlib.util.module_from_spec(spec);spec.loader.exec_module(pub)
repo=root.parents[2]
shutil.copyfile(repo/'.github/workflows/fossify-gallery100-publish.yml',root/'workflow.yml')
cfg=json.loads((root/'manifest-template.json').read_text())
qa_raw=(args.dist/pub.QA_NAME).read_bytes();qa=json.loads(qa_raw)
cfg.update(freeze_state='frozen',reviewed_main_head=args.main_head,reviewed_dev_head=args.dev_head,official_apk_sha256=args.official_apk_sha256)
pub.validate_qa(qa,cfg)
expected={name:{'bytes':len((args.dist/name).read_bytes()),'sha256':pub.sha((args.dist/name).read_bytes())} for name in (pub.SINGLE,pub.COMBINED)}
pub.require(qa['artifacts']==expected,'QA does not identify current outputs')
cfg.update(source_sha256={name:pub.sha(raw) for name,raw in pub.reviewed_files().items()},local_expected_mpp=expected,expected_mpp=expected,
           local_expected_qa_sha256=pub.sha(qa_raw),expected_qa_sha256=pub.sha(qa_raw),ci_build_pair_verified=False,ci_run_id=None,ci_source_commit=None)
for name in ('manifest-template.json','manifest.json'):(root/name).write_bytes(pub.json_bytes(cfg))
print('PASS frozen exact reviewed sources, official APK, locally built MPPs and QA')
