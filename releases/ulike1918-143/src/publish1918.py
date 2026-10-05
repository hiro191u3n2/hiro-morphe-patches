#!/usr/bin/env python3
"""Publish only independently rebuilt, locally APK-tested ULike cleanup artifacts."""
import argparse, datetime, importlib.util, json, os
from pathlib import Path
from build1918 import require, sha, PINS, SINGLE, BUNDLE, NOTES
from finalize1918 import EXPECTED
ROOT=Path(__file__).resolve().parent
REPO='hiro191u3n2/hiro-morphe-patches'
TAG='ulike-v1.9.18';RAW_BRANCH='release/ulike1918-143'
PAGE=f'https://github.com/{REPO}/releases/tag/{TAG}'

def publish(dist,repo):
 helper=repo/'releases/ulike-rollback188-142/rollback142.py'
 spec=importlib.util.spec_from_file_location('verified_publication_helpers',helper);R=importlib.util.module_from_spec(spec);spec.loader.exec_module(R)
 require(R.REPO==REPO and R.BUNDLE=='1.0.142','Unexpected helper baseline')
 R.VERSION='1.9.18';R.PREVIOUS_APP='1.8.8';R.BUNDLE='1.0.143';R.PREVIOUS='1.0.142';R.NOTES=NOTES;R.PAGE=PAGE
 R.SUMMARY='承認済みv1.8.8基準。指定7設定と機能を削除し、設定から戻ると撮影画面へ。撮影画面の完全終了と他アプリは維持。'
 qa=json.loads((dist/'QA_ULike_v1.9.18.json').read_text());evidence=json.loads((dist/'APK_APPLY1918_QA.json').read_text())
 require(qa['result']=='PASS_BUILD_HOST_TESTS_STRUCTURAL_CHECKS' and qa['original_apk_apply_tested'] and not qa['android_device_tested'],'Publication QA failed')
 require(evidence['result']=='PASS_BOTH_MPP_VARIANTS_ORIGINAL_APKS_APPLICATION','Application QA failed')
 for name,pin in EXPECTED.items():require(sha((dist/name).read_bytes())==pin==qa['artifacts'][name]['sha256'],'Tested MPP mismatch')
 states={b:R.snapshot(b) for b in ('main','dev')};R.check_heads(states)
 for state in states.values():
  require(state['manifest']['version']=='1.0.142','Concurrent or stale release')
  require(sha(R.fetch(state['manifest']['download_url']))==PINS['Hiro_Morphe_Patches_v1.0.142.mpp'],'Active base differs')
  policy=json.loads(R.content('releases/ULike_ACTIVE.json',state['head']))
  require(policy['ulike_version']=='1.8.8' and policy['bundle_version']=='1.0.142' and '1.9.17' in policy['withdrawn_versions'],'Unexpected ULike baseline')
 created=datetime.datetime.now(datetime.timezone.utc).replace(tzinfo=None,microsecond=0).isoformat()
 for state in states.values():R.metadata(state,'https://example.invalid/preflight',created)
 require(R.api(f'repos/{REPO}/releases/tags/{TAG}',absent=True) is None,'Release already exists; inspect before retry')
 require(R.api(f'repos/{REPO}/git/ref/heads/{RAW_BRANCH}',absent=True) is None,'Distribution branch already exists; inspect before retry')
 names=[SINGLE,BUNDLE,'QA_ULike_v1.9.18.json','APK_APPLY1918_QA.json','ULike_v1.9.18_sources_and_QA.zip','RELEASE_NOTES.txt']
 expected={n:{'bytes':(dist/n).stat().st_size,'sha256':sha((dist/n).read_bytes())} for n in names}
 receipt={'ulike_version':'1.9.18','bundle_version':'1.0.143','base_ulike_version':'1.8.8','assets':expected,'operations':[],'android_device_tested':False,'original_apk_apply_tested':True}
 receiptpath=dist/'publication_ULike_v1.9.18.json';require(not receiptpath.exists(),'Existing publication receipt; inspect before retry')
 def checkpoint(status,**values):
  receipt.update(status=status,**values);receiptpath.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n');print(status,flush=True)
 checkpoint('preflight_passed')
 try:
  R.check_heads(states)
  release=R.api(f'repos/{REPO}/releases',{'tag_name':TAG,'target_commitish':os.environ['GITHUB_SHA'],'name':'ULike v1.9.18 設定整理・戻る修正 / Hiro Morphe v1.0.143','body':NOTES,'draft':True,'prerelease':True,'make_latest':'false'})
  checkpoint('draft_created',release_id=release['id'])
  for name in names:R.run(['gh','release','upload',TAG,'--repo',REPO,dist/name])
  R.verify_assets(release['id'],expected,False);checkpoint('draft_assets_verified');R.check_heads(states)
  R.api(f'repos/{REPO}/releases/{release["id"]}',{'draft':False,'prerelease':True,'make_latest':'false'},'PATCH')
  R.verify_assets(release['id'],expected,True);checkpoint('public_release_bytes_verified');R.check_heads(states)
  R.api(f'repos/{REPO}/git/refs',{'ref':'refs/heads/'+RAW_BRANCH,'sha':states['main']['head']})
  raw=R.commit_files({**states['main'],'branch':RAW_BRANCH},{'downloads/'+n:(dist/n).read_bytes() for n in (SINGLE,BUNDLE)},'release: verified ULike1918 and bundle143 binary downloads')
  url=f'https://raw.githubusercontent.com/{REPO}/{raw["head"]}/downloads/{BUNDLE}'
  require(R.fetch(url)==(dist/BUNDLE).read_bytes(),'Immutable Manager download mismatch')
  checkpoint('immutable_download_verified',raw_commit=raw['head'],download_url=url);R.check_heads(states)
  policy={'ulike_version':'1.9.18','bundle_version':'1.0.143','source_filename':SINGLE,'source_sha256':EXPECTED[SINGLE],'source_release':TAG,'active_release':TAG,'base_ulike_version':'1.8.8','base_source_sha256':PINS['ULike_HQ_Texture_Online_v1.8.8.mpp'],'withdrawn_versions':['1.9.17'],'reason':'Requested deletion of seven settings/features and settings-only Back navigation; approved v1.8.8 lineage.','future_ulike_base':'Use this approved188-lineage cleanup; do not restore withdrawn v1.9.17 or its 10bit route.','android_device_tested':False,'original_apk_apply_tested':True}
  sourcefiles={'releases/ulike1918-143/src/'+p.name:p.read_bytes() for p in ROOT.iterdir() if p.suffix in ('.java','.py')}
  for name in ['QA_ULike_v1.9.18.json','APK_APPLY1918_QA.json','cleanup-audit1918.tsv','host-tests1918.txt','standalone-register-analysis.tsv','integrated-register-analysis.tsv']:
   sourcefiles['releases/ulike1918-143/'+name]=(dist/name).read_bytes()
  for branch,state in states.items():
   data,log=R.metadata(state,url,created)
   files={'patches-bundle.json':(json.dumps(data,ensure_ascii=False,indent=2)+'\n').encode(),'CHANGELOG.md':log.encode(),'releases/ULike_ACTIVE.json':(json.dumps(policy,ensure_ascii=False,indent=2)+'\n').encode()}
   if branch=='main':files.update(sourcefiles)
   new=R.commit_files(state,files,'release: ULike1918 settings cleanup and bundle143 Manager update')
   require(json.loads(R.content('patches-bundle.json',new['head']))==data,'Remote feed mismatch')
   require(R.content('CHANGELOG.md',new['head']).decode()==log,'Remote changelog mismatch')
   require(R.fetch(data['download_url'])==(dist/BUNDLE).read_bytes(),'Manager file mismatch')
   receipt['operations'].append({'branch':branch,'commit':new['head'],'manifest_verified':True,'download_verified':True});checkpoint('manager_'+branch+'_verified')
  for branch in states:
   latest=R.snapshot(branch);require(latest['manifest']['version']=='1.0.143' and latest['manifest']['download_url']==url,'Active feed moved unexpectedly')
  checkpoint('published_and_verified',release_url=PAGE,manager_ulike_update_eligible=True,other_apps_false_updates=False,source_plaintext_committed=True,fixed_chatgpt_site_updated=False)
 except BaseException as error:
  checkpoint('failed_requires_inspection',error=type(error).__name__+': '+str(error));raise
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--dist',required=True,type=Path);p.add_argument('--repo',required=True,type=Path);a=p.parse_args();publish(a.dist.resolve(),a.repo.resolve())
