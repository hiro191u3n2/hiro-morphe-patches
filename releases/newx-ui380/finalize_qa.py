#!/usr/bin/env python3
"""Aggregate observed test results; fails if any required evidence is absent."""
from pathlib import Path
import hashlib,json
ROOT=Path(__file__).resolve().parent
def read(p):return json.loads((ROOT/p).read_text())
def main():
 build=read('dist/build-evidence.json');assert build['result']=='PASS'
 app={}
 for case in ['dev1','dev2','dev2-clone','standalone']:
  patch=read('verification/'+case+'-result.json');assert patch['success']and not patch['failedPatches']
  dex=read('review/'+case+'-apk-preservation.json');resources=read('review/'+case+'-resources.json')
  assert dex['result']==resources['result']=='PASS'
  apk=ROOT/'verification'/(case+'.apk')
  sha=hashlib.sha256(apk.read_bytes()).hexdigest()
  assert resources['apk_sha256']==sha
  if 'apk_sha256' in dex:assert dex['apk_sha256']==sha
  app[case]={'result':'PASS','applied_patch_count':len(patch['appliedPatches']),'apk_sha256':sha,'dex':dex,'resources':resources}
 host={}
 for name,p in [('chat','chat/QA.json'),('bars','bars/QA.json'),('refresh_rate','refresh/QA.json'),('settings','refresh/settings-test/QA.json'),('view_count_hook','views/QA.json'),('view_count_format','views-runtime/QA.json')]:
  q=read(p)
  if name=='bars':assert q['stock_dex_test']==q['piko_dev2_hiro378_japanese_dex_test']=='PASS'
  else:assert q['result']=='PASS'
  host[name]=q
 missing=(ROOT/'verification/missing-piko.log').read_text()
 assert 'Piko 3.12.0-dev.1/dev.2' in missing and not (ROOT/'verification/missing-piko.apk').exists()
 result={'schema':'hiro-newx-ui-options-v2','result':'PASS','version':'1.0.380','target':'com.twitter.android 12.43.0-prod.01','patch':'NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）','artifacts':build['artifacts'],'application_tests':app,'host_tests':host,'missing_piko_rejected':True,'preservation':build,'settings':{'path':'Piko設定 → Hiroの表示設定','default_enabled':[True,True,True,True],'restart_required':['hide_chat_recommendations','pin_bars','full_view_counts'],'refresh_applied_on_activity_resume':True,'count_example':'3001 -> 3,001','view_count_only':True,'raw_integer_precision':64},'limitations':{'physical_device_tested':False,'emulator_tested':False,'android_runtime_verified':False,'actual_120hz_verified':False,'manager_device_badge_observed':False,'refresh_rate_subject_to_os_policy':True}}
 p=ROOT/'verification/final-validation.json';p.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'result':'PASS','cases':{k:v['applied_patch_count']for k,v in app.items()}}))
if __name__=='__main__':main()
