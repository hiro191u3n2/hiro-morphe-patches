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
 unchanged_source_hashes={'bars/PinBarsPatch.java': '698dbb116cb198855d10a057c192ea01ca57284e6fddf8c489249a7fed08344d', 'chat/ChatRecommendationsHook.java': 'b9ebbdfa6058798ef4c958db3f980029af9a151bd389435c2eca42c76056561f', 'refresh/RefreshRate.java': '6241b4e4395c5a9e10c3008c173b01306c819ee26e743342b7cf0bbb4d4d24ed', 'src/runtime/UiOptions.java': 'c74f74a7d1795430ed64b80b72d34934d0c4038c220d0b43543f32840aa818b1', 'src/patch/UiOptionsPatch.java': '03f8304bb483fc1e9cd03827c84da7c43ff9429dd05e5cd897e2d656eb0f6ccb'}
 for rel,expected in unchanged_source_hashes.items():
  assert hashlib.sha256((ROOT/rel).read_bytes()).hexdigest()==expected,rel
 host={}
 for name,p in [('chat','chat/QA.json'),('bars','bars/QA.json'),('refresh_rate','refresh/QA.json'),('settings','refresh/settings-test/QA.json'),('view_count_hook','views/QA.json'),('view_count_format','views-runtime/QA.json')]:
  q=read(p)
  if name=='bars':assert q['stock_dex_test']==q['piko_dev2_hiro378_japanese_dex_test']=='PASS'
  else:assert q['result']=='PASS'
  host[name]=q
 assert host['view_count_hook']['native_formatter_calls_on_enabled_valid_view_count']==0
 assert host['view_count_hook']['native_formatter_calls_on_nonnull_fallback']==1
 assert host['view_count_format']['formatter_sha256']==hashlib.sha256((ROOT/'views-runtime/ViewCountFormat.java').read_bytes()).hexdigest()
 assert host['view_count_format']['deterministic_reuse_tested'] and host['view_count_format']['locale_invalidation_tested']
 host['formatter_benchmark']=read('views-runtime/benchmark.json')
 missing=(ROOT/'verification/missing-piko.log').read_text()
 assert 'Piko 3.12.0-dev.1/dev.2' in missing and not (ROOT/'verification/missing-piko.apk').exists()
 result={'schema':'hiro-newx-ui-options-v3','result':'PASS','version':'1.0.381','target':'com.twitter.android 12.43.0-prod.01','patch':'NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）','artifacts':build['artifacts'],'application_tests':app,'host_tests':host,'missing_piko_rejected':True,'preservation':build,'performance_change':{'native_abbreviation_skipped_for_enabled_valid_view_count':True,'formatter_reused_per_thread_and_locale':True,'no_periodic_callbacks_added':True,'bar_and_refresh_hooks_unchanged':True,'device_root_cause_established':False},'settings':{'path':'Piko設定 → Hiroの表示設定','default_enabled':[True,True,True,True],'restart_required':['hide_chat_recommendations','pin_bars','full_view_counts'],'refresh_applied_on_activity_resume':True,'count_example':'3001 -> 3,001','view_count_only':True,'raw_integer_precision':64},'limitations':{'physical_device_tested':False,'emulator_tested':False,'android_runtime_verified':False,'actual_120hz_verified':False,'manager_device_badge_observed':False,'scroll_smoothness_improvement_measured_on_device':False,'refresh_rate_subject_to_os_policy':True}}
 p=ROOT/'verification/final-validation.json';p.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'result':'PASS','cases':{k:v['applied_patch_count']for k,v in app.items()}}))
if __name__=='__main__':main()
