#!/usr/bin/env python3
"""Bind reproducible MPPs to the exact local original-APKS application evidence."""
import argparse, json
from pathlib import Path
from build1919 import sha, require, write_zip, SINGLE, BUNDLE
ROOT=Path(__file__).resolve().parent
EXPECTED={'ULike_HQ_Texture_Online_v1.9.19.mpp': '1f415b2ff980f5923dd9c853b18f44faec309abb39e458f569d84485c93fadc3', 'Hiro_Morphe_Patches_v1.0.144.mpp': 'a39322cfb5914b52d567e194a079d7401782b89c00897f0fdd2828352be2910e'}
def finalize(dist,evidence):
 qpath=dist/'QA_ULike_v1.9.19.json';qa=json.loads(qpath.read_text())
 require(qa['result']=='PASS_BUILD_HOST_TESTS_STRUCTURAL_CHECKS','No passing build QA')
 report=json.loads((evidence/'APK_APPLY1919_QA.json').read_text())
 require(report['result']=='PASS_BOTH_MPP_VARIANTS_ORIGINAL_APKS_APPLICATION' and report['original_apk_apply_tested'] and not report['android_device_tested'],'No passing local application evidence')
 require({v['mpp_filename'] for v in report['variants']}==set(EXPECTED),'Wrong tested variant set')
 for v in report['variants']:
  name=v['mpp_filename'];raw=(dist/name).read_bytes();require(sha(raw)==EXPECTED[name]==v['mpp_sha256']==qa['artifacts'][name]['sha256'],'Remote build differs from locally APK-tested MPP')
  require(len(raw)==v['mpp_bytes']==qa['artifacts'][name]['bytes'],'Artifact size mismatch')
  require(v['register_flow_failures']==0 and v['register_flow_targets']==42 and v['runtime_and_stock_method_contracts_verified']==1475,'Missing application verification')
 for name,digest in report['source_sha256'].items():require(sha((ROOT/name).read_bytes())==digest,'Application verifier source changed: '+name)
 qa.update(original_apk_apply_tested=True,original_apk_apply_report='APK_APPLY1919_QA.json',original_apk_apply_execution='Local authoring container; exact MPP hashes bound to this independent build. Original APK is not uploaded to CI.',original_apk_applied_variants=2,applied_method_contracts_per_variant=1475,host_register_flow_targets_per_variant=42,host_register_flow_failures=0,source_sha256={p.name:sha(p.read_bytes()) for p in sorted(ROOT.iterdir()) if p.suffix in ('.py','.java')})
 qpath.write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
 sources={'src/'+p.name:p.read_bytes() for p in ROOT.iterdir() if p.suffix in ('.py','.java')}
 for p in evidence.iterdir():
  if p.suffix in ('.json','.txt','.tsv'):(dist/p.name).write_bytes(p.read_bytes());sources['evidence/'+p.name]=p.read_bytes()
 for name in ('QA_ULike_v1.9.19.json','cleanup-audit1919.tsv','host-tests1919.txt','policy-tests1919.txt','RELEASE_NOTES.txt'):sources[name]=(dist/name).read_bytes()
 write_zip(dist/'ULike_v1.9.19_sources_and_QA.zip',sources)
 print('PASS exact locally APK-tested MPPs bound to build and publication evidence')
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--dist',required=True,type=Path);p.add_argument('--evidence',required=True,type=Path);a=p.parse_args();finalize(a.dist,a.evidence)
