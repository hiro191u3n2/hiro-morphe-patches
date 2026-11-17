#!/usr/bin/env python3
"""Require fresh382 dev2 verification and exact380 payload restoration evidence."""
from pathlib import Path
import hashlib,json
ROOT=Path(__file__).resolve().parent
def read(path):return json.loads((ROOT/path).read_text())
def main():
 build=read('dist/build-evidence.json');assert build['result']=='PASS' and build['rollback_payloads_match380']
 baseline=read('verification/baseline380-validation.json');assert baseline['result']=='PASS' and baseline['version']=='1.0.380'
 for case in ['dev1','dev2','dev2-clone','standalone']:assert baseline['application_tests'][case]['result']=='PASS'
 patch=read('verification/dev2-result.json');assert patch['success'] and not patch['failedPatches']
 dex=read('review/dev2-apk-preservation.json');resources=read('review/dev2-resources.json');assert dex['result']==resources['result']=='PASS'
 apk_sha=hashlib.sha256((ROOT/'verification/dev2.apk').read_bytes()).hexdigest();assert resources['apk_sha256']==apk_sha
 if 'apk_sha256' in dex:assert dex['apk_sha256']==apk_sha
 for name,identity in build['artifacts'].items():
  raw=(ROOT/'dist'/name).read_bytes();assert identity=={'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest()}
 result={'schema':'hiro-newx-ui-rollback-v1','result':'PASS','version':'1.0.382','restored_version':'1.0.380','target':'com.twitter.android 12.43.0-prod.01','patch':'NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）','artifacts':build['artifacts'],'application_tests':{'dev2':{'result':'PASS','validation_scope':'fresh382 patch application, APK rebuild and exact380 code/resource comparison','applied_patch_count':len(patch['appliedPatches']),'apk_sha256':apk_sha,'dex':dex,'resources':resources}},'baseline380_validation':{'result':'PASS','source':'verification/baseline380-validation.json','historical_cases':['dev1','dev2','dev2-clone','standalone'],'retested_all_four_for382':False,'reuse_basis':'All executable and resource payloads are byte-identical to published380; package manifest metadata only differs.'},'preservation':build,'settings':baseline['settings'],'limitations':{'physical_device_tested':False,'emulator_tested':False,'android_runtime_verified':False,'scroll_smoothness_improvement_measured_on_device':False,'manager_device_badge_observed':False},'regression_report':'v381 worsened scrolling according to user; its optimization is reverted without further behavior changes.'}
 (ROOT/'verification/final-validation.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'result':'PASS','fresh_case':'dev2','restored':'1.0.380'}))
if __name__=='__main__':main()
