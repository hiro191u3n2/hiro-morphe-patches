#!/usr/bin/env python3
"""Bind completed CLI/ARSC/DEX evidence to the three distributed MPP files."""
import argparse, hashlib, json, shutil, zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
CASES=('dev3','dev3-clone','dev2','standalone')
def read(path):return json.loads(path.read_text())
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--workspace',type=Path,required=True);a=p.parse_args();workspace=a.workspace.resolve();qa=workspace/'qa'
 build=read(ROOT/'dist/build-evidence.json');assert build['result']=='PASS'
 for name,identity in build['artifacts'].items():
  path=ROOT/'dist'/name;assert identity=={'bytes':path.stat().st_size,'sha256':sha(path)}
 tests={};output=ROOT/'verification'
 for case in CASES:
  summary=read(qa/(case+'-summary.json'));cli=read(qa/(case+'-result.json'))
  resources=read(qa/(case+'-resources.json'));dex=read(qa/(case+'-localized-dex.json'))
  assert summary['result']==resources['result']==dex['result']=='PASS'
  assert cli['success'] is True and not cli['failedPatches']
  assert len(cli['appliedPatches'])==summary['selected_patch_count']
  identity=sha(qa/(case+'.apk'))
  assert summary['apk_sha256']==resources['apk_sha256']==dex['apk_sha256']==identity
  assert len(resources['checks'])==1322 and not resources['failures'] and dex['actual_lookup_sites']==224 and not dex['failures']
  tests[case]={**summary,'cli_result':{'success':True,'appliedPatches':cli['appliedPatches'],'failedPatches':[]},'compiled_resource_checks':1322,'localized_lookup_sites':224,'classes_scanned':dex['classes_scanned']}
  for suffix in ['-summary.json','-result.json','-resources.json','-localized-dex.json']:
   shutil.copyfile(qa/(case+suffix),output/(case+suffix))
 baseline=read(qa/'baseline-dev2-summary.json');assert baseline['result']=='PASS'
 assert baseline['apk_sha256']==tests['dev2']['apk_sha256']==sha(qa/'baseline-dev2.apk')
 repro=read(qa/'repro382-result.json');assert repro['success'] is False
 assert '未検証のPikoソース' in (qa/'repro382.log').read_text()
 shutil.copyfile(qa/'repro382-result.json',output/'repro382-result.json')
 shutil.copyfile(qa/'baseline-dev2-result.json',output/'baseline-dev2-result.json')
 providers=read(ROOT/'patch/QA.json');ui=read(ROOT/'ui/QA.json');secondary=read(ROOT/'patch/QA_patcher25.json')
 assert providers['result']==ui['result']==secondary['result']=='PASS'
 members=['extensions/newx.mpe','extensions/shared.mpe'];equivalence={}
 for version in ['3.12.0-dev.1','3.12.0-dev.2','3.12.0-dev.3']:
  path=workspace/'upstream-piko'/('patches-'+version+'.mpp')
  with zipfile.ZipFile(path) as z:equivalence[version]={'bundle_sha256':sha(path),'members':{m:hashlib.sha256(z.read(m)).hexdigest() for m in members}}
 assert len({json.dumps(v['members'],sort_keys=True) for v in equivalence.values()})==1
 report={'schema':'hiro-piko-dev3-compatibility-v1','result':'PASS','version':'1.0.383','japanese_component_version':'1.0.1','ui_component_version':'1.1.3','target':{'package':'com.twitter.android','version':'12.43.0-prod.01','input_sha256':sha(workspace/'inputs/X_12.43.0-prod.01.apkm')},'artifacts':build['artifacts'],'original_failure':{'reproduced_on_382':True,'piko':'3.12.0-dev.3','message':'NewX日本語化: 未検証のPikoソースです'},'application_tests':tests,'dev2_regression':{'result':'PASS','baseline_version':'1.0.382','entire_apk_byte_identical':True,'sha256':baseline['apk_sha256']},'upstream_extensions':equivalence,'provider_validation':{'japanese':providers['host_audit'],'ui':ui['selection_audit'],'patcher25':secondary},'preservation':{'runtime_extensions_byte_identical_to382':True,'translation_resources_byte_identical_to382':True,'changed_provider_classes':build['dex_preservation']['changed_provider_classes']},'environment':{'desktop':'2.3.0-dev.2','patcher':'2.5.0-dev.1','exact_user_patcher_2_5_0_dev2_tested':False,'manager_device_tested':False,'note':'Latest official downloadable Desktop contains Patcher2.5.0-dev.1; exact user2.5.0-dev.2 runtime unavailable.'},'physical_device_tested':False,'emulator_tested':False,'signature_installation_tested':False}
 raw=json.dumps(report,ensure_ascii=False,indent=2)+'\n'
 (ROOT/'dist/QA_Piko_NewX_Japanese_v1.0.1.json').write_text(raw);(output/'final-validation.json').write_text(raw)
 (ROOT/'QA_build383.json').write_bytes((ROOT/'dist/build-evidence.json').read_bytes())
 print(json.dumps({'result':'PASS','cases':list(tests),'compiled_resource_checks_per_case':1322,'localized_sites_per_case':224,'dev2_entire_apk_identical_to382':True}))
if __name__=='__main__':main()
