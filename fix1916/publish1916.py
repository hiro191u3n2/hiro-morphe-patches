#!/usr/bin/env python3
"""Publish only independently rebuilt and original-APK-applied trial artifacts."""
import datetime,json,os,subprocess,sys,zipfile
from pathlib import Path
R=Path(__file__).resolve().parents[1];sys.path.insert(0,str(R/'fix197/publication'))
import publish197 as p
p.VERSION='1.9.16';p.PREVIOUS_APP='1.9.15';p.BUNDLE='1.0.140';p.PREVIOUS='1.0.139';p.TAG='ulike-v1.9.16'
BRANCH='release/ulike-graph1916'
def main():
 dist=Path(sys.argv[1]).resolve();q=json.loads((dist/'QA_ULike_v1.9.16.json').read_text())
 assert q['schema']=='ulike1916-source-build-release-1' and q['version']==p.VERSION and q['bundle_version']==p.BUNDLE
 assert q['result']=='PASS_EXACT_SOURCE_HOST_GPU_INDEPENDENT_BUILD_AND_ORIGINAL_APK_APPLICATION'
 for key in ('trial','android_arm64_native_built','mpp_built','native_independent_rebuild_byte_identical','java_independent_rebuild_byte_identical','mpp_independent_rebuild_byte_identical','native_exported_abi_preserved','full_original_apk_apply_tested','no_8bit_photo_fallback_added'):assert q[key] is True,key
 for key in ('android_device_tested','physical_original_sdk_execution_verified','capture_success_proven'):assert q[key] is False,key
 for row in q['artifacts']:
  b=(dist/row['file']).read_bytes();assert len(b)==row['bytes'] and p.sha(b)==row['sha256']
  assert q['original_apk_application']['artifacts'][row['file']]=={'sha256':row['sha256'],'bytes':row['bytes']}
 source=dist/'ULike_v1.9.16_sources_and_QA.zip'
 with zipfile.ZipFile(source) as z:
  assert z.testzip() is None;catalog=json.loads(z.read('SOURCE_ARCHIVE_MANIFEST.json'));assert set(z.namelist())==set(catalog['files'])|{'SOURCE_ARCHIVE_MANIFEST.json'}
  for n,row in catalog['files'].items():assert len(z.read(n))==row['bytes'] and p.sha(z.read(n))==row['sha256']
  assert z.read('fix1916/evidence/QA_ULike_v1.9.16.json')==(dist/'QA_ULike_v1.9.16.json').read_bytes()
 notes=p.checked_text(R/'fix1916/RELEASE_NOTES.txt');summary=p.checked_text(R/'fix1916/SUMMARY.txt')
 states={b:p.snapshot(b) for b in ('main','dev')};assert all(s['manifest']['version']==p.PREVIOUS for s in states.values())
 upload=p.snapshot(BRANCH);target=os.environ['GITHUB_SHA'];assert upload['head']==target
 for s in states.values():
  old,_=p.fetch(s['manifest']['download_url']);assert p.sha(old)=='2cf16810b717e7807622332dc70f4f9b48f26d790f6a40eaf7bed72dd89b3c02'
 assert p.api(f'repos/{p.REPO}/releases/tags/{p.TAG}',allow_absent=True) is None
 assert p.api(f'repos/{p.REPO}/git/ref/tags/{p.TAG}',allow_absent=True) is None
 paths=[dist/'Hiro_Morphe_Patches_v1.0.140.mpp',dist/'ULike_HQ_Texture_Online_v1.9.16.mpp',source,dist/'QA_ULike_v1.9.16.json']
 expected={x.name:{'bytes':x.stat().st_size,'sha256':p.sha(x.read_bytes())} for x in paths}
 raw=paths[0].read_bytes();rawpath='downloads/'+paths[0].name
 created=datetime.datetime.now(datetime.timezone.utc).replace(tzinfo=None,microsecond=0).isoformat();date=created[:10]
 for s in states.values():p.make_metadata(s,f'https://raw.githubusercontent.com/{p.REPO}/'+('0'*40)+'/'+rawpath,notes,summary,date,raw,created)
 statefile=dist/'publication-graph1916.json';assert not statefile.exists()
 state={'version':p.VERSION,'bundle_version':p.BUNDLE,'trial':True,'capture_success_proven':False,'android_device_tested':False,'target_commit':target,'assets':expected,'operations':[]}
 def checkpoint(status,**fields):
  state.update(status=status,**fields);statefile.write_text(json.dumps(state,ensure_ascii=False,indent=2)+'\n');print(status,flush=True)
 checkpoint('preflight_passed')
 try:
  p.assert_heads(states)
  release=p.api(f'repos/{p.REPO}/releases',{'tag_name':p.TAG,'target_commitish':target,'name':'ULike v1.9.16 試用版 / Hiro Morphe v1.0.140','body':notes,'draft':True,'prerelease':True,'make_latest':'false'})
  checkpoint('draft_created',release_id=release['id'])
  for path in paths:subprocess.run(['gh','release','upload',p.TAG,'--repo',p.REPO,str(path)],check=True)
  checks=p.verify_assets(release['id'],expected,True);checkpoint('draft_bytes_verified',draft_asset_checks=checks)
  p.assert_heads(states)
  p.api(f'repos/{p.REPO}/releases/{release["id"]}',{'draft':False,'prerelease':True,'make_latest':'false'},method='PATCH')
  checks=p.verify_assets(release['id'],expected,False);checkpoint('public_release_bytes_verified',public_asset_checks=checks)
  p.assert_heads(states)
  uploaded=p.commit_files(upload,{rawpath:raw},'release: ULike 1.9.16 verified source build / bundle 1.0.140')
  url=f'https://raw.githubusercontent.com/{p.REPO}/{uploaded["head"]}/{rawpath}';actual,_=p.fetch(url);assert actual==raw
  checkpoint('immutable_download_verified',download_url=url,raw_commit=uploaded['head'])
  prepared={b:p.make_metadata(s,url,notes,summary,date,raw,created) for b,s in states.items()};p.assert_heads(states);checks=[]
  for b,s in states.items():
   data,log,check=prepared[b]
   committed=p.commit_files(s,{'patches-bundle.json':(json.dumps(data,ensure_ascii=False,indent=2)+'\n').encode(),'CHANGELOG.md':log.encode()},'release: ULike 1.9.16 / bundle 1.0.140 Manager metadata')
   assert json.loads(p.contents_at('patches-bundle.json',committed['head']))==data and p.contents_at('CHANGELOG.md',committed['head']).decode()==log
   state['operations'].append({'branch':b,'metadata_commit':committed['head']});checks.append({'branch':b,**check});checkpoint('metadata_'+b+'_verified')
  state['release_url']=f'https://github.com/{p.REPO}/releases/tag/{p.TAG}'
  checkpoint('published_and_manager_download_verified',manager_metadata_checks=checks)
 except BaseException as error:
  checkpoint('failed_requires_inspection',error=type(error).__name__+': '+str(error));raise
if __name__=='__main__':main()
