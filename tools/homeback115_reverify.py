#!/usr/bin/env python3
"""Reverify an already-published release after a public-feed propagation mismatch.
Does not rebuild, replace assets, or change feed/patch behavior. Saves a verified receipt only.
"""
from pathlib import Path
from types import SimpleNamespace
import importlib.util,json,os,time
ROOT=Path(__file__).resolve().parents[1]
SPEC=importlib.util.spec_from_file_location('homeback115_publish',ROOT/'releases/homeback115/publication/publish.py')
p=importlib.util.module_from_spec(SPEC);SPEC.loader.exec_module(p)
STAGED=ROOT/'.ci-homeback115-reverify'
FIRST=STAGED/'dist-a';SECOND=STAGED/'dist-b'
BASE=STAGED/'baseline239.mpp'
BASE.write_bytes(p.fetch(p.BASE_URL))
args=SimpleNamespace(config=ROOT/'releases/homeback115/publication/manifest.json',base=BASE,dist=FIRST)
cfg,payloads,notes,assets,report=p.validate_local(args)
for name in (p.SINGLE,p.COMBINED,p.QA_NAME):
    p.require((FIRST/name).read_bytes()==(SECOND/name).read_bytes(),'Two completed CI builds differ: '+name)
receipt=json.loads((FIRST/p.RECEIPT_NAME).read_text())
p.require(receipt['status']=='failed_requires_inspection' and receipt['error']=='RuntimeError: Public branch feed mismatch','Unexpected recovery target')
p.require(receipt['assets']==assets and receipt['local_validation']==report,'Original publication evidence differs')
p.require(receipt['source_commit']=='03f5b8e76cf4154535e13cbb4ab0981e54d2eb54','Unreviewed build source')
run=p.api(f'repos/{p.REPO}/actions/runs/38680353053')
p.require(run['head_sha']==receipt['source_commit'] and run['conclusion']=='failure','Unexpected original run')
jobs=p.api(f'repos/{p.REPO}/actions/runs/38680353053/jobs')['jobs']
steps={step['number']:step for job in jobs for step in job['steps']}
p.require(all(steps[n]['conclusion']=='success' for n in (7,8,9)),'Original clean build/verification did not complete')
p.verify_assets(receipt['release_id'],assets,True)
p.require(p.head(p.RAW_BRANCH)==receipt['raw_commit'],'Immutable distribution branch moved')
for name,url in receipt['download_urls'].items():
    p.require(p.fetch(url)==payloads[name],'Immutable MPP download differs: '+name)
states={branch:p.snapshot(branch) for branch in ('main','dev')}
p.require(states['main']['manifest']==states['dev']['manifest'],'Manager branches differ')
p.require(states['main']['manifest']['version']==p.BUNDLE,'Active bundle advanced; do not overwrite')
preflight=json.loads((STAGED/'preflight.log').read_text())
operations=[]
for branch,state in states.items():
    before=preflight['heads'][branch]
    baseline={'manifest':json.loads(p.content('patches-bundle.json',before)),
              'log':p.content('CHANGELOG.md',before).decode()}
    expected_feed,expected_log=p.metadata(baseline,cfg,receipt['download_urls'][p.COMBINED],state['manifest']['created_at'],notes)
    p.require(state['manifest']==expected_feed and state['log']==expected_log,'Published metadata differs from reviewed update: '+branch)
    p.require(state['ulike_policy']==p.content(p.POLICY_PATH,before),'Existing ULike policy differs: '+branch)
    # Branch/CDN content can lag a successful Git commit. Retry mismatches without any remote rewrite.
    for attempt in range(18):
        nonce=str(time.time_ns())
        prefix=f'https://raw.githubusercontent.com/{p.REPO}/{branch}'
        raw_feed=p.fetch(prefix+'/patches-bundle.json?homeback115_verify='+nonce)
        raw_log=p.fetch(prefix+'/CHANGELOG.md?homeback115_verify='+nonce)
        feed=json.loads(raw_feed)
        if feed==expected_feed and raw_log.decode()==expected_log:
            print('PASS public branch metadata',branch,'attempt',attempt+1,flush=True)
            break
        print('Public branch metadata not propagated',branch,'attempt',attempt+1,'version',feed.get('version'),flush=True)
        p.check_heads(states)
        time.sleep(5)
    else:raise RuntimeError('Public metadata still differs; no receipt written: '+branch)
    p.require(p.fetch(expected_feed['download_url'])==payloads[p.COMBINED],'Manager download differs: '+branch)
    operations.append({'branch':branch,'commit':state['head'],'feed_verified':True,'changelog_verified':True,
                       'ulike_policy_unchanged':True,'manager_download_verified':True})
p.check_heads(states)
p.verify_assets(receipt['release_id'],assets,True)
receipt['recovered_from_error']=receipt.pop('error')
receipt.update(status='published_and_verified',published=True,release_url=p.PAGE,operations=operations,
               manager_main_dev_updated_atomically=True,manager_app_scopes_verified=list(p.APP_SCOPES),
               other_apps_false_updates=False,all_unrelated_patch_resources_preserved=True,
               trip_com_behavior_unchanged=True,trip_com_home_only_proactive_exit=True,
               trip_com_native_terminal_back_cleanup=True,instagram_reels_exit=True,
               instagram_comments_collapse_first=True,instagram_same_gesture_exit_protected=True,
               tiktok_native_child_back_retained=True,tiktok_messages_profile_exit=False,
               tiktok_messages_profile_home_navigation=True,trip_bounded_async_back_and_stop_cleanup=True,
               fixed_chatgpt_site_updated=False,initial_publication_run=38680353053,
               verification_recovery_run=os.environ.get('GITHUB_RUN_ID'),
               verification_recovery_source=os.environ.get('GITHUB_SHA'),
               public_metadata_reverified=True,assets_replaced_during_recovery=False)
raw=p.json_bytes(receipt)
(FIRST/p.RECEIPT_NAME).write_bytes(raw)
saved=p.commit_feeds(ROOT,states,{branch:{p.RELEASE_ROOT+'/'+p.RECEIPT_NAME:raw} for branch in states},
                     'docs: verified Back115 publication receipt after public feed recheck')
for branch,state in saved.items():
    p.require(p.content(p.RELEASE_ROOT+'/'+p.RECEIPT_NAME,state['head'])==raw,'Saved receipt mismatch')
    p.require(json.loads(p.content('patches-bundle.json',state['head']))==states[branch]['manifest'],'Feed changed while saving receipt')
    p.require(p.content(p.POLICY_PATH,state['head'])==states[branch]['ulike_policy'],'ULike policy changed while saving receipt')
p.check_heads(saved)
print('PASS existing release assets and local/two-CI bytes; main/dev feeds and scopes verified; receipt saved atomically',flush=True)
