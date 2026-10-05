#!/usr/bin/env python3
"""Publish exact desktop-validated exit hotfix; retain explicit device uncertainty."""
import argparse, datetime, importlib.util, json, os
from pathlib import Path
from build1921 import sha, require, PINS
from finalize1921 import EXPECTED, NOTES
ROOT=Path(__file__).resolve().parent
REPO='hiro191u3n2/hiro-morphe-patches'
TAG='ulike-v1.9.21';RAW_BRANCH='release/ulike1921-146'
SINGLE='ULike_HQ_Texture_Online_v1.9.21.mpp';BUNDLE='Hiro_Morphe_Patches_v1.0.146.mpp'
PAGE=f'https://github.com/{REPO}/releases/tag/{TAG}'
def publish(dist,repo):
 helper=repo/'releases/ulike-rollback188-142/rollback142.py'
 raw=helper.read_bytes()
 import hashlib
 require(hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest()=='e7771a4868d7a798217304af6d6099bcded5569e','Publication helper drift')
 spec=importlib.util.spec_from_file_location('publication_helpers',helper)
 R=importlib.util.module_from_spec(spec);spec.loader.exec_module(R)
 require(R.REPO==REPO and R.BUNDLE=='1.0.142','Wrong helper baseline')
 R.VERSION='1.9.21';R.PREVIOUS_APP='1.9.20';R.BUNDLE='1.0.146';R.PREVIOUS='1.0.145';R.NOTES=NOTES;R.PAGE=PAGE
 R.SUMMARY='削除済み診断フィールドの参照による保存中の誤判定を修正。終了待ちは戻る・タッチで取消でき、画面を閉じる前の待機は15秒で取消。実際の保存・録画は保護。実機未検証。'
 qa=json.loads((dist/'QA_ULike_v1.9.21.json').read_text())
 require(qa['status']=='REBUILD_MATCHES_DESKTOP_TESTED_MPP_DEVICE_UNVERIFIED','Missing finalized QA')
 require(qa['rebuild_sha256_matches_desktop_evidence'] and qa['host_tests']['total']==150 and not qa['device_tested'],'Invalid verification scope')
 for name,pin in EXPECTED.items():
  require(sha((dist/name).read_bytes())==pin==qa['artifacts'][name]['sha256'],'Attachment MPP mismatch')
 states={b:R.snapshot(b) for b in ('main','dev')};R.check_heads(states)
 policies={}
 for branch,state in states.items():
  require(state['manifest']['version']=='1.0.145','Concurrent or stale bundle')
  require(sha(R.fetch(state['manifest']['download_url']))==PINS['Hiro_Morphe_Patches_v1.0.145.mpp'],'Active baseline bytes differ')
  policy=json.loads(R.content('releases/ULike_ACTIVE.json',state['head']));policies[branch]=policy
  require(policy['ulike_version']=='1.9.20' and policy['bundle_version']=='1.0.145' and policy['approved_lineage_version']=='1.8.8' and '1.9.17' in policy['withdrawn_versions'],'Unexpected ULike baseline')
 created=datetime.datetime.now(datetime.timezone.utc).replace(tzinfo=None,microsecond=0).isoformat()
 for state in states.values():R.metadata(state,'https://example.invalid/preflight-only',created)
 require(R.api(f'repos/{REPO}/releases/tags/{TAG}',absent=True) is None,'Release exists; inspect before retry')
 require(R.api(f'repos/{REPO}/git/ref/heads/{RAW_BRANCH}',absent=True) is None,'Distribution branch exists; inspect before retry')
 names=[SINGLE,BUNDLE,'QA_ULike_v1.9.21.json','ULike_v1.9.21_sources_and_QA.zip','RELEASE_NOTES.txt','SHA256SUMS.txt']
 expected={n:{'bytes':(dist/n).stat().st_size,'sha256':sha((dist/n).read_bytes())} for n in names}
 receipt={'ulike_version':'1.9.21','bundle_version':'1.0.146','base_ulike_version':'1.9.20','approved_lineage_version':'1.8.8',
          'assets':expected,'operations':[],'android_device_tested':False,'black_preview_cause_confirmed':False,
          'black_preview_fixed_on_device':False,'ci_rebuild_matches_desktop_tested_mpp':True,'exit_root_cause_confirmed_in_code':True,
          'host_tests':{'total':150,'back':26,'session':45,'exit':79,'device_execution':False}}
 receiptpath=dist/'publication_ULike_v1.9.21.json';require(not receiptpath.exists(),'Existing receipt; inspect before retry')
 def checkpoint(status,**values):
  receipt.update(status=status,**values);receiptpath.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n');print(status,flush=True)
 checkpoint('preflight_passed')
 try:
  R.check_heads(states)
  release=R.api(f'repos/{REPO}/releases',{'tag_name':TAG,'target_commitish':os.environ['GITHUB_SHA'],
    'name':'ULike v1.9.21 終了待ち固まり修正 / Hiro Morphe v1.0.146',
    'body':NOTES,'draft':True,'prerelease':True,'make_latest':'false'})
  checkpoint('draft_created',release_id=release['id'])
  for name in names:R.run(['gh','release','upload',TAG,'--repo',REPO,dist/name])
  R.verify_assets(release['id'],expected,False);checkpoint('draft_assets_verified');R.check_heads(states)
  R.api(f'repos/{REPO}/releases/{release["id"]}',{'draft':False,'prerelease':True,'make_latest':'false'},'PATCH')
  R.verify_assets(release['id'],expected,True);checkpoint('public_release_bytes_verified');R.check_heads(states)
  R.api(f'repos/{REPO}/git/refs',{'ref':'refs/heads/'+RAW_BRANCH,'sha':states['main']['head']})
  rawstate=R.commit_files({**states['main'],'branch':RAW_BRANCH},{'downloads/'+n:(dist/n).read_bytes() for n in (SINGLE,BUNDLE)},'release: exact ULike1921 hotfix and bundle146 binary downloads')
  url=f'https://raw.githubusercontent.com/{REPO}/{rawstate["head"]}/downloads/{BUNDLE}'
  require(R.fetch(url)==(dist/BUNDLE).read_bytes(),'Immutable Manager download mismatch')
  checkpoint('immutable_download_verified',raw_commit=rawstate['head'],download_url=url);R.check_heads(states)
  sourcefiles={'releases/ulike1921-146/src/'+p.relative_to(ROOT).as_posix():p.read_bytes() for p in sorted(ROOT.rglob('*')) if p.is_file() and p.suffix in ('.java','.py')}
  for name in ('QA_ULike_v1.9.21.json','RELEASE_NOTES.txt','SHA256SUMS.txt','host-tests1920.txt','host-tests1921.txt','transform1921-audit.tsv','busy-schema.tsv','helper-references.txt'):
   sourcefiles['releases/ulike1921-146/'+name]=(dist/name).read_bytes()
  active={}
  for branch,state in states.items():
   data,log=R.metadata(state,url,created)
   policy={**policies[branch],'ulike_version':'1.9.21','bundle_version':'1.0.146','source_filename':SINGLE,
     'source_sha256':EXPECTED[SINGLE],'source_release':TAG,'active_release':TAG,'base_ulike_version':'1.9.20',
     'reason':'User requested exit-wait freeze repair: remove nonexistent diagnostics field from busy guard; cancellable pending exit and15s preclose limit without terminating live saves. Device unverified.',
     'future_ulike_base':'Use this approved188-lineage hotfix; preserve native panel Back and face feature. Removed diagnostic state is not an active capture queue. Do not restore withdrawn1917 or its10bit route.',
     'release_status':'hotfix_device_unverified','android_device_tested':False,'original_apk_apply_tested':True,
     'ci_rebuild_matches_local_tested_artifacts':True,'exit_root_cause_confirmed_in_code':True,'pending_exit_user_cancellable':True,'pending_exit_wait_limit_ms':15000,'black_preview_cause_confirmed':False,'black_preview_fixed_on_device':False}
   updates={'patches-bundle.json':(json.dumps(data,ensure_ascii=False,indent=2)+'\n').encode(),
            'CHANGELOG.md':log.encode(),'releases/ULike_ACTIVE.json':(json.dumps(policy,ensure_ascii=False,indent=2)+'\n').encode()}
   if branch=='main':updates.update(sourcefiles)
   new=R.commit_files(state,updates,'release: ULike1921 hotfix and bundle146 Manager update')
   require(json.loads(R.content('patches-bundle.json',new['head']))==data,'Remote feed mismatch')
   require(R.content('CHANGELOG.md',new['head']).decode()==log,'Remote changelog mismatch')
   require(R.fetch(data['download_url'])==(dist/BUNDLE).read_bytes(),'Manager download mismatch')
   active[branch]=new
   receipt['operations'].append({'branch':branch,'commit':new['head'],'manifest_verified':True,'download_verified':True})
   checkpoint('manager_'+branch+'_verified')
  for branch in states:
   latest=R.snapshot(branch)
   require(latest['manifest']['version']=='1.0.146' and latest['manifest']['download_url']==url,'Active feed moved')
  R.verify_assets(release['id'],expected,True)
  checkpoint('published_and_verified',release_url=PAGE,published=True,manager_feed_updated=True,
             manager_ulike_update_eligible=True,other_apps_false_updates=False,source_plaintext_committed=True,
             fixed_chatgpt_site_updated=False)
  # Publish the receipt after the actual public files and both feeds were verified.
  for branch,state in active.items():
   saved=R.commit_files(state,{'releases/ulike1921-146/'+receiptpath.name:receiptpath.read_bytes()},
                        'docs: verified ULike1921 publication receipt')
   require(R.content('releases/ulike1921-146/'+receiptpath.name,saved['head'])==receiptpath.read_bytes(),'Receipt commit mismatch')
  R.run(['gh','release','upload',TAG,'--repo',REPO,receiptpath])
  expected[receiptpath.name]={'bytes':receiptpath.stat().st_size,'sha256':sha(receiptpath.read_bytes())}
  R.verify_assets(release['id'],expected,True)
  print('PASS public release, immutable MPP bytes, main/dev feeds, update eligibility and receipt verified',flush=True)
 except BaseException as error:
  checkpoint('failed_requires_inspection',error=type(error).__name__+': '+str(error));raise
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--dist',required=True,type=Path);p.add_argument('--repo',required=True,type=Path)
 a=p.parse_args();publish(a.dist.resolve(),a.repo.resolve())
