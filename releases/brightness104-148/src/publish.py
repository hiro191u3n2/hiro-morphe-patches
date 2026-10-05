#!/usr/bin/env python3
"""Publish tested MPP bytes and main/dev Manager feeds with concurrent-update guards."""
import argparse, datetime, json, os
from pathlib import Path
from build import BASE_SHA,BUNDLE,SINGLE,NOTES,ROOT,sha,require,load_helpers
REPO='hiro191u3n2/hiro-morphe-patches';TAG='brightnessclick-v1.0.4';RAW_BRANCH='release/brightness104-148'
PAGE=f'https://github.com/{REPO}/releases/tag/{TAG}'
def publish(repo,dist):
    R=load_helpers(repo);qa=json.loads((dist/'QA_BrightnessClick_v1.0.4.json').read_text())
    require(qa['manifest_host_assertions']==219 and qa['independent_transform_identical'],'Required host validation absent')
    require(not qa['android_device_tested'] and not qa['original_apk_apply_tested'],'Verification scope drift')
    for name,spec in qa['artifacts'].items():require(sha((dist/name).read_bytes())==spec['sha256'],'Validated bytes changed')
    states={b:R.snapshot(b) for b in ('main','dev')};R.check_heads(states)
    untouched={}
    for branch,state in states.items():
        require(state['manifest']['version']=='1.0.147','Concurrent/stale bundle; review new base')
        require(sha(R.fetch(state['manifest']['download_url']))==BASE_SHA,'Active bundle bytes differ')
        untouched[branch]={p:R.content(p,state['head']) for p in ('releases/ULike_ACTIVE.json',)}
    require(R.api(f'repos/{REPO}/releases/tags/{TAG}',absent=True) is None,'Release already exists; inspect before retry')
    require(R.api(f'repos/{REPO}/git/ref/heads/{RAW_BRANCH}',absent=True) is None,'Distribution branch already exists')
    expected={p.name:{'bytes':p.stat().st_size,'sha256':sha(p.read_bytes())} for p in dist.iterdir() if p.is_file()}
    receipt={'bundle_version':'1.0.148','brightness_version':'1.0.4','android_device_tested':False,'original_apk_apply_tested':False,'assets':expected,'operations':[]}
    path=dist/'publication_BrightnessClick_v1.0.4.json'
    def checkpoint(status,**more):
        receipt.update(status=status,**more);path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n');print(status,flush=True)
    checkpoint('preflight_passed')
    release=R.api(f'repos/{REPO}/releases',{'tag_name':TAG,'target_commitish':os.environ['GITHUB_SHA'],'name':'明るさタッチ v1.0.4 履歴非表示 / Hiro Morphe v1.0.148','body':NOTES,'draft':True,'prerelease':True,'make_latest':'false'})
    checkpoint('draft_created',release_id=release['id'])
    for name in expected:R.run(['gh','release','upload',TAG,'--repo',REPO,dist/name])
    R.verify_assets(release['id'],expected,False);R.check_heads(states)
    R.api(f'repos/{REPO}/releases/{release["id"]}',{'draft':False,'prerelease':True,'make_latest':'false'},'PATCH')
    R.verify_assets(release['id'],expected,True);checkpoint('public_release_bytes_verified');R.check_heads(states)
    R.api(f'repos/{REPO}/git/refs',{'ref':'refs/heads/'+RAW_BRANCH,'sha':states['main']['head']})
    raw=R.commit_files({**states['main'],'branch':RAW_BRANCH},{'downloads/'+n:(dist/n).read_bytes() for n in (BUNDLE,SINGLE)},'release: exact BrightnessClick104 and bundle148 MPP downloads')
    url=f'https://raw.githubusercontent.com/{REPO}/{raw["head"]}/downloads/{BUNDLE}'
    require(R.fetch(url)==(dist/BUNDLE).read_bytes(),'Immutable download mismatch')
    checkpoint('immutable_download_verified',download_url=url,raw_commit=raw['head']);R.check_heads(states)
    created=datetime.datetime.now(datetime.timezone.utc).replace(tzinfo=None,microsecond=0).isoformat()
    active={}
    for branch,state in states.items():
        old=state['manifest'];require(datetime.datetime.fromisoformat(created)>datetime.datetime.fromisoformat(old['created_at']),'Feed timestamp must advance')
        feed={**old,'version':'1.0.148','created_at':created,'download_url':url,'page_url':PAGE,'signature_download_url':'','description':NOTES.strip()+'\n\n'+old['description']}
        log=f'# 1.0.148 ({created[:10]})\n\n* **BrightnessClick:** v1.0.4：起動直後から履歴非表示。明るさ切替・保存設定・権限案内の実行コードは維持。\n\n'+NOTES.strip()+'\n\n'+state['log']
        require(R.eligible(log,'1.0.147','BrightnessClick'),'Missing BrightnessClick update scope')
        require(not R.eligible(log,'1.0.148','BrightnessClick'),'Already updated app falsely outdated')
        for app in ('ULike','Berry Browser','SwiftKey Beta','Instagram','X','Trip.com'):
            require(not R.eligible(log,'1.0.147',app),'False other-app update: '+app)
        files={'patches-bundle.json':(json.dumps(feed,ensure_ascii=False,indent=2)+'\n').encode(),'CHANGELOG.md':log.encode()}
        if branch=='main':
            files.update({'releases/brightness104-148/src/'+p.name:p.read_bytes() for p in ROOT.iterdir() if p.suffix in ('.java','.py')})
            files.update({'releases/brightness104-148/'+p.name:p.read_bytes() for p in dist.iterdir() if p.suffix in ('.json','.txt') and p!=path})
        result=R.commit_files(state,files,'release: BrightnessClick104 Recents exclusion and bundle148 Manager update')
        require(json.loads(R.content('patches-bundle.json',result['head']))==feed,'Remote feed mismatch')
        require(R.fetch(feed['download_url'])==(dist/BUNDLE).read_bytes(),'Manager MPP download mismatch')
        for p,body in untouched[branch].items():require(R.content(p,result['head'])==body,'Unrelated ULike feed/policy changed')
        active[branch]=result;receipt['operations'].append({'branch':branch,'commit':result['head'],'feed_verified':True,'download_verified':True,'ulike_metadata_unchanged':True})
        checkpoint('manager_'+branch+'_verified')
    for branch in active:
        state=R.snapshot(branch);require(state['manifest']['version']=='1.0.148' and state['manifest']['download_url']==url,'Published feed moved')
    checkpoint('published_and_verified',published=True,release_url=PAGE,manager_feed_updated=True,manager_brightness_update_eligible=True,other_apps_false_updates=False,fixed_chatgpt_site_updated=False)
    for branch,state in active.items():
        R.commit_files(state,{'releases/brightness104-148/'+path.name:path.read_bytes()},'docs: verified BrightnessClick104 publication receipt')
    R.run(['gh','release','upload',TAG,'--repo',REPO,path]);expected[path.name]={'bytes':path.stat().st_size,'sha256':sha(path.read_bytes())}
    R.verify_assets(release['id'],expected,True)
    print('PASS public release, immutable downloads, main/dev feeds and unchanged ULike metadata verified',flush=True)
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--repo',type=Path,required=True);p.add_argument('--dist',type=Path,required=True);a=p.parse_args();publish(a.repo.resolve(),a.dist.resolve())
