#!/usr/bin/env python3
"""Bind final-source reproducible builds to already verified identical Patcher 2.2 artifacts.

This does not rerun an APK or claim that incomplete older-SDK jobs passed.
"""
from pathlib import Path
import argparse, importlib.util, json

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('builder',HERE.parent/'src/build.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
TOOL_SHA='855dd2d6fa5ed997a5884a44f8807c9b5bc95e69b5ebd58dfd7e8b897c86e2d7'

def main():
    p=argparse.ArgumentParser()
    p.add_argument('--dist',type=Path,required=True)
    p.add_argument('--second-dist',type=Path,required=True)
    p.add_argument('--apk',type=Path,required=True)
    a=p.parse_args()
    qa_path=a.dist/b.QA_NAME
    qa=json.loads(qa_path.read_text())
    evidence_path=HERE/'QA_Amazon_Patcher22.json'
    evidence=json.loads(evidence_path.read_text())
    b.require(qa['schema']=='amazon-review100-qa-v1' and qa['sources']==b.sources(),'Final-source build evidence differs')
    b.require(qa['baseline_version']==b.BASE_VERSION and qa['baseline_sha256']==b.BASE_SHA,'Final baseline differs')
    b.require(evidence['schema']=='amazon-review100-patcher22-v1' and evidence['tool_sha256']==TOOL_SHA,'Official 2.2 evidence differs')
    b.require(evidence['physical_android_tested'] is False and evidence['bytecode_mode']=='STRIP_FAST','Unsupported device/mode claim')
    b.require(b.sha(a.apk)==b.SUPPLIED_APK_SHA==evidence['supplied_apk_sha256'],'Supplied APK identity differs')
    artifacts={n:{'bytes':(a.dist/n).stat().st_size,'sha256':b.sha(a.dist/n)} for n in (b.SINGLE,b.BUNDLE)}
    b.require(artifacts==qa['artifacts']==evidence['artifacts'],'Built and actually tested MPPs differ')
    for n in artifacts:b.require((a.dist/n).read_bytes()==(a.second_dist/n).read_bytes(),'Independent builds differ: '+n)
    rows=evidence['applications']
    b.require(len(rows)==2 and {r['bundle'] for r in rows}=={b.SINGLE,b.BUNDLE},'Require both actual 2.2 applications')
    for r in rows:b.require(r['package']==b.PACKAGE and r['version']==b.APP_VERSION and r['sdk_version']=='2.2.0' and r['result']=='PASS' and 'PASS' in r['audit_summary'],'APK application evidence incomplete')
    b.require(evidence['single_bundle_apk_entry_contents_equal'] and evidence['supplied_apk_non_dex_resources_unchanged'],'Application preservation evidence missing')
    qa.update(
        apk_application=[dict(r,sdk='morphe-2.2.jar') for r in rows],
        apk_bytecode_mode='STRIP_FAST',
        supplied_apk_sha256=b.SUPPLIED_APK_SHA,
        supplied_apk_non_dex_resources_unchanged=True,
        single_bundle_apk_entry_contents_equal=True,
        guard_tests=qa['guard_tests']+'\n'+evidence['guard_tests'],
        verification_provenance={
            'build':'Clean final-source build with pinned toolchain; standalone/bundle bytes equal independent clean build.',
            'actual_apk_tests':'Two completed applications using official Patcher 2.2.0 / Desktop 2.0.0 with STRIP_FAST.',
            'application_evidence_sha256':b.sha(evidence_path),
            'application_tool_sha256':TOOL_SHA,
            'older_sdk_apk_claims':'No incomplete older-SDK application jobs are counted in this QA.',
            'desktop_full_mode':'Older desktop FULL rebuilding retained a stale tail DEX; that output was rejected and is not distributed.'})
    qa_path.write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
    print('PASS final-source builds and exactly two already-completed official Patcher 2.2.0 APK tests are bound to identical MPP hashes')
if __name__=='__main__':main()
