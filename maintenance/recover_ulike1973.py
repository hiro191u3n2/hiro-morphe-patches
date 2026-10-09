#!/usr/bin/env python3
"""Verify the completed .73 feed push; optionally finish only its receipt.

Default mode performs public/local reads and writes local proof files only.
--complete is reserved for the separately reviewed receipt recovery workflow.
The reviewed release source, six public assets, immutable downloads, feeds and
policy are never modified. Every proposed receipt commit has the exact leased
HEAD as its sole parent and both branch refs advance in one atomic Git push.
"""
from __future__ import annotations
import argparse
import datetime as dt
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import time
import zipfile

SOURCE = 'a7625da7fcd10e2a0dce46e0b8b7e47cbda0e4b7'
FEED_HEADS = {'main': '497e92a9fb7ecd259a516529519de3d7091dec87',
              'dev': '78db46807daca94e4b5c8fc65a144afe41a2ddbc'}
PARENTS = {'main': SOURCE, 'dev': '455e8aa0693c84d9e48aef5ff6289915a03e7e98'}
RAW = '92547bac24d554f2c894b93e4a8cffa2dc65d134'
RUN, ARTIFACT = 37923137259, 11612364539
ARTIFACT_SHA = 'dee9b7371c3c5c8d43716eb6b58d39ca7843bb900e8b2717d81da447e65dc221'
PUBLISHER_SHA = '6ad3e070f4176d9fe571deac77acfc1cdcb9f4066aab35dc16b5d6b164eaf7db'
MAINTENANCE = {'maintenance/recover_ulike1973.py', '.github/workflows/ulike1973-receipt-recovery.yml'}
CI_ASSETS = {
 'Hiro_Morphe_Patches_v1.0.206.mpp': {'bytes':17862181,'sha256':'e318a78210ad0025a92cf370aa0728bca7652477f2c405a9913d62994da0e82c'},
 'ULike_HQ_Texture_Online_v1.9.73.mpp': {'bytes':1215203,'sha256':'31f189f0ce30a2c0b40e7eaf7ad0ebc23dba48a2eac0715e113558d8e983bbba'},
 'QA_ULike_v1.9.73.json': {'bytes':728449,'sha256':'e2983927f3bd826c0450e1f7db9e9a70404ffbdc009d0019a65a40db7cd32ba2'},
 'RELEASE_NOTES.txt': {'bytes':4042,'sha256':'9ea487ecd4dfd9cf093676fab457e0b6c4ee3adbc45eaa295b28602c7ed860f1'},
 'SHA256SUMS.txt': {'bytes':472,'sha256':'d7c0f2fcf189c09f62ae52305db7bc17b17bb445bb755a805986912cee24a705'},
 'ULike_v1.9.73_sources_and_QA.zip': {'bytes':15115087,'sha256':'b754f404c25574197f0e76d0637d0a849efd46fec4adf12eccdb11b13a63feb3'},
}

def require(ok, message):
    if not ok: raise RuntimeError(message)

def sha(raw): return hashlib.sha256(raw).hexdigest()

def encoded(data):
    return (json.dumps(data, ensure_ascii=False, sort_keys=True, indent=2)+'\n').encode()

def load_publisher(repo):
    path=repo/'releases/ulike1973-gpu-latency/publication/publish1973.py'
    require(sha(path.read_bytes())==PUBLISHER_SHA,'Frozen original publisher changed')
    spec=importlib.util.spec_from_file_location('original_reviewed_publisher1973',path)
    pub=importlib.util.module_from_spec(spec);spec.loader.exec_module(pub)
    return pub

def unpack(pub, path, output):
    require(sha(path.read_bytes())==ARTIFACT_SHA,'Original failed-run artifact ZIP differs')
    destination=output/'original-ci-artifact';destination.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(path) as archive:
        names=archive.namelist();require(len(names)==len(set(names)),'Duplicate artifact paths')
        require(archive.testzip() is None,'Corrupt original CI artifact')
        for name in names:
            pub.checked_path(name.rstrip('/'))
            if not name.endswith('/'):
                target=destination/name;target.parent.mkdir(parents=True,exist_ok=True)
                target.write_bytes(archive.read(name))
    return destination

def find(root,name):
    files=list(root.rglob(name));require(len(files)==1,'Missing or ambiguous artifact file: '+name)
    return files[0]

def local_proofs(pub,args):
    root=unpack(pub,args.artifact_zip,args.output) if args.artifact_zip else args.artifact_directory
    require(root is not None,'Pinned original artifact ZIP is required')
    require(args.artifact_zip is not None or not args.complete,'--complete requires the original pinned artifact ZIP')
    generated=find(root,'generated-manifest.json');expected=pub.load_expected(generated)
    require(expected['artifacts']==CI_ASSETS,'Original CI asset pins differ from independently observed release')
    dist=find(root,pub.SINGLE).parent
    baseline=args.baseline.read_bytes() if args.baseline else pub.fetch(pub.BASE_URL)
    singlebase=args.standalone_baseline.read_bytes() if args.standalone_baseline else pub.fetch(pub.SINGLE_BASE_URL)
    payloads,notes,report=pub.validate_local(dist,expected,baseline,singlebase,args.repo)
    failed=json.loads(find(root,pub.RECEIPT_NAME).read_bytes())
    require(failed.get('source_commit')==SOURCE and failed.get('assets')==CI_ASSETS
            and failed.get('publication_manifest_sha256')==sha(generated.read_bytes())
            and failed.get('status')=='failed_requires_inspection'
            and failed.get('error')=='RuntimeError: Concurrent update: main'
            and failed.get('raw_commit')==RAW
            and failed.get('operations')==[] and not failed.get('published'),
            'Original failed checkpoint differs; automatic recovery does not apply')
    require(failed.get('local_validation')==report,'Original validation report differs')
    return expected,payloads,notes,report,failed

def remote_heads(pub,repo):
    text=pub.git(repo,'ls-remote',f'https://github.com/{pub.REPO}.git',
                 'refs/heads/main','refs/heads/dev')
    rows={line.split()[1].removeprefix('refs/heads/'):line.split()[0] for line in text.splitlines()}
    require(set(rows)=={'main','dev'},'Git transport did not return both refs')
    require(all(re.fullmatch('[a-f0-9]{40}',x) for x in rows.values()),'Malformed branch HEAD')
    return rows

def check_exact_heads(pub,repo,states):
    require(remote_heads(pub,repo)=={branch:state['head'] for branch,state in states.items()},
            'Concurrent branch update; preserve it and stop')

def await_written_heads(pub,repo,old,new):
    """Retry only a known old-to-new transition; a third SHA always stops us."""
    for attempt in range(8):
        actual=remote_heads(pub,repo)
        for branch,value in actual.items():
            require(value in (old[branch]['head'],new[branch]['head']),
                    'Unexpected concurrent HEAD after atomic push: '+branch)
        if actual=={branch:state['head'] for branch,state in new.items()}: return
        if attempt!=7: time.sleep(2)
    raise RuntimeError('Expected atomic receipt refs did not become visible within14s')

def commit_info(pub,head):
    return pub.api(f'repos/{pub.REPO}/git/commits/{head}')

def changes(pub,old,new):
    result=pub.api(f'repos/{pub.REPO}/compare/{old}...{new}')
    require(result['status']=='ahead' and result['ahead_by']==1 and result['behind_by']==0,
            'Commit is not the single expected direct descendant')
    return {row['filename'] for row in result['files']}

def state(pub,branch,head):
    return {'branch':branch,'head':head,'tree':commit_info(pub,head)['tree']['sha'],
            'manifest':json.loads(pub.content('patches-bundle.json',head)),
            'log':pub.content('CHANGELOG.md',head).decode(),
            'ulike_policy':pub.content(pub.POLICY_PATH,head)}

def verify_published(pub,args,expected,payloads,notes,failed):
    original_run=pub.api(f'repos/{pub.REPO}/actions/runs/{RUN}')
    require(original_run.get('head_sha')==SOURCE and original_run.get('status')=='completed'
            and original_run.get('conclusion')=='failure' and original_run.get('event')=='push'
            and original_run.get('path')=='.github/workflows/ulike1973-gpu-latency-publish.yml',
            'Original verification workflow provenance differs')
    artifact=pub.api(f'repos/{pub.REPO}/actions/artifacts/{ARTIFACT}')
    require(artifact.get('id')==ARTIFACT and artifact.get('name')=='ulike1973-gpu-latency-verified-publication'
            and artifact.get('expired') is False and artifact.get('digest')=='sha256:'+ARTIFACT_SHA
            and artifact.get('workflow_run',{}).get('id')==RUN
            and artifact.get('workflow_run',{}).get('head_sha')==SOURCE,
            'Original verification artifact provenance differs')
    current=remote_heads(pub,args.repo)
    recovery_source=os.environ.get('GITHUB_SHA') or pub.git(args.repo,'rev-parse','HEAD')
    require(current=={'main':recovery_source,'dev':FEED_HEADS['dev']},'Recovery source/main or dev HEAD differs')
    require(pub.git(args.repo,'rev-parse','HEAD')==recovery_source,'Recovery checkout differs')
    require(recovery_source!=FEED_HEADS['main'],'Recovery must run from its reviewed maintenance commit')
    require([row['sha'] for row in commit_info(pub,recovery_source)['parents']]==[FEED_HEADS['main']],
            'Recovery source does not directly follow the verified feed commit')
    require(changes(pub,FEED_HEADS['main'],recovery_source)==MAINTENANCE,'Recovery source changed unrelated paths')
    # The archived release code and declaration remain the exact reviewed source.
    declaration=args.repo/pub.RELEASE_ROOT/'manifest.json'
    require(declaration.read_bytes()==pub.content(pub.RELEASE_ROOT+'/manifest.json',SOURCE),
            'Original source declaration changed')
    require(pub.content(pub.RELEASE_ROOT+'/publication/publish1973.py',SOURCE)==
            (args.repo/pub.RELEASE_ROOT/'publication/publish1973.py').read_bytes(),'Publisher source lineage differs')
    parents={branch:state(pub,branch,head) for branch,head in PARENTS.items()}
    published={branch:state(pub,branch,head) for branch,head in FEED_HEADS.items()}
    active={branch:state(pub,branch,head) for branch,head in current.items()}
    require([row['sha'] for row in commit_info(pub,RAW)['parents']]==[SOURCE],'Raw distribution lineage differs')
    require(changes(pub,SOURCE,RAW)=={'downloads/'+pub.SINGLE,'downloads/'+pub.COMBINED},'Raw distribution changed unrelated files')
    urls={name:f'https://raw.githubusercontent.com/{pub.REPO}/{RAW}/downloads/{name}'
          for name in (pub.SINGLE,pub.COMBINED)}
    require(failed.get('download_urls')==urls,'Original immutable download URLs differ')
    for name,url in urls.items(): require(pub.fetch(url)==payloads[name],'Immutable Manager bytes differ: '+name)
    require(pub.head(pub.RAW_BRANCH)==RAW,'Immutable distribution branch moved')
    release=pub.api(f'repos/{pub.REPO}/releases/tags/{pub.TAG}')
    require(release['target_commitish']==SOURCE,'Public release targets another source commit')
    require(release['body']==notes,'Public release notes differ')
    pub.verify_assets(release['id'],expected['artifacts'],True)
    for branch,old in parents.items():
        commit=commit_info(pub,FEED_HEADS[branch])
        require([row['sha'] for row in commit['parents']]==[PARENTS[branch]],'Original feed commit lineage differs: '+branch)
        allowed={'patches-bundle.json','CHANGELOG.md',pub.POLICY_PATH}
        if branch=='main': allowed|={pub.RELEASE_ROOT+'/'+name for name in
                                     (pub.QA_NAME,'RELEASE_NOTES.txt','SHA256SUMS.txt')}
        modified=changes(pub,PARENTS[branch],FEED_HEADS[branch])
        require({'patches-bundle.json','CHANGELOG.md',pub.POLICY_PATH}<=modified<=allowed,
                'Original feed commit changed unrelated paths: '+branch)
        feed=published[branch]['manifest'];created=feed['created_at']
        wanted_feed,wanted_log=pub.metadata(old,urls[pub.COMBINED],created,notes)
        wanted_policy=pub.new_policy(pub.validate_policy(old['ulike_policy']),expected)
        require(feed==wanted_feed and published[branch]['log']==wanted_log
                and json.loads(published[branch]['ulike_policy'])==wanted_policy,'Published feed/policy/changelog differs: '+branch)
        for path,wanted in [('patches-bundle.json',pub.json_bytes(wanted_feed)),('CHANGELOG.md',wanted_log.encode()),
                            (pub.POLICY_PATH,pub.json_bytes(wanted_policy))]:
            require(pub.content(path,current[branch])==pub.content(path,FEED_HEADS[branch]),'Recovery source changed active metadata: '+branch)
            require(pub.fetch(f'https://raw.githubusercontent.com/{pub.REPO}/{branch}/{path}?receipt-recovery={current[branch]}')==wanted,
                    'Public branch metadata differs: '+branch+'/'+path)
    require(active['main']['manifest']==active['dev']['manifest'],'Manager feed branches differ')
    for name in (pub.QA_NAME,'RELEASE_NOTES.txt','SHA256SUMS.txt'):
        require(pub.content(pub.RELEASE_ROOT+'/'+name,current['main'])==payloads[name],'Published diagnostic differs: '+name)
    receipt_path=pub.RELEASE_ROOT+'/'+pub.RECEIPT_NAME
    for branch,head in current.items():
        require(pub.api(f'repos/{pub.REPO}/contents/{receipt_path}?ref={head}',absent=True) is None,
                'A receipt already exists; inspect instead of rewriting it')
    check_exact_heads(pub,args.repo,active)
    return active,release,recovery_source

def save_receipts(pub,args,states,raw):
    path=pub.RELEASE_ROOT+'/'+pub.RECEIPT_NAME
    check_exact_heads(pub,args.repo,states)
    pub.git(args.repo,'fetch','--no-tags',f'https://github.com/{pub.REPO}.git',*[s['head'] for s in states.values()])
    prepared={branch:pub.prepare_commit(args.repo,old,{path:raw},'docs: verified ULike1973 publication receipt recovery')
              for branch,old in states.items()}
    for branch,new in prepared.items():
        require(pub.git(args.repo,'diff-tree','--no-commit-id','--name-only','-r',new['head'])==path,
                'Prepared receipt commit changed another file')
    check_exact_heads(pub,args.repo,states)
    pub.git(args.repo,'push','--atomic',
            *[f"--force-with-lease=refs/heads/{branch}:{old['head']}" for branch,old in states.items()],
            f'https://github.com/{pub.REPO}.git',*[f"{new['head']}:refs/heads/{branch}" for branch,new in prepared.items()])
    await_written_heads(pub,args.repo,states,prepared)
    for branch,new in prepared.items():
        require(pub.content(path,new['head'])==raw,'Saved receipt differs: '+branch)
        for kept in ('patches-bundle.json','CHANGELOG.md',pub.POLICY_PATH):
            require(pub.content(kept,new['head'])==pub.content(kept,states[branch]['head']),
                    'Active metadata changed while saving receipt')
    return prepared

def verify_seventh_asset(pub,release_id,raw):
    expected={**CI_ASSETS,pub.RECEIPT_NAME:{'bytes':len(raw),'sha256':sha(raw)}}
    for attempt in range(8):
        release=pub.api(f'repos/{pub.REPO}/releases/{release_id}')
        require(release.get('tag_name')==pub.TAG and release.get('draft') is False
                and release.get('prerelease') is True,'Release identity changed after receipt upload')
        assets={row['name']:row for row in release['assets']}
        require(len(assets)==len(release['assets']) and set(assets) in (set(CI_ASSETS),set(expected)),
                'Unexpected release asset inventory after receipt upload')
        for name,asset in assets.items():
            pin=expected[name]
            require(asset['size']==pin['bytes'] and asset['state']=='uploaded'
                    and (asset.get('digest') in (None,'sha256:'+pin['sha256'])),
                    'Release asset changed after receipt upload: '+name)
        if set(assets)==set(expected):
            for name,asset in assets.items():
                data=pub.fetch(asset['browser_download_url'])
                require(len(data)==expected[name]['bytes'] and sha(data)==expected[name]['sha256'],
                        'Public seventh-asset verification failed: '+name)
            return
        if attempt!=7: time.sleep(2)
    raise RuntimeError('The uploaded seventh asset did not become visible within14s')

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    for key in ('repo','output'): parser.add_argument('--'+key,type=Path,required=True)
    parser.add_argument('--artifact-zip',type=Path)
    parser.add_argument('--artifact-directory',type=Path)
    parser.add_argument('--baseline',type=Path);parser.add_argument('--standalone-baseline',type=Path)
    parser.add_argument('--local-only',action='store_true')
    parser.add_argument('--complete',action='store_true')
    args=parser.parse_args();require(not(args.local_only and args.complete),'Conflicting recovery modes')
    args.repo=args.repo.resolve();args.output=args.output.resolve();args.output.mkdir(parents=True,exist_ok=True)
    pub=load_publisher(args.repo)
    expected,payloads,notes,report,failed=local_proofs(pub,args)
    if args.local_only:
        result={'status':'local_original_ci_artifacts_verified_no_remote_writes','assets':CI_ASSETS,
                'source_commit':SOURCE,'validation':report}
    else:
        states,release,recovery_source=verify_published(pub,args,expected,payloads,notes,failed)
        receipt={key:value for key,value in failed.items() if key not in ('error','status')}
        receipt.update(status='published_and_verified',published=True,release_url=pub.PAGE,
                       manager_main_dev_updated_atomically=True,manager_ulike_update_eligible=True,
                       previous_ulike1969_repatch_eligible=True,previous_ulike1972_repatch_eligible=True,
                       other_apps_false_updates=False,
                       baseline_other_apps_preserved=True,fixed_chatgpt_site_updated=False,
                       recovery_source_commit=recovery_source,recovery_workflow_run=os.environ.get('GITHUB_RUN_ID'),
                       recovery_reason='Original atomic feed push was followed by a failed immediate REST HEAD verification. The exact direct-descendant feed commits, current Git refs, assets and immutable downloads were independently verified before receipt recovery.',
                       original_workflow_run=RUN,original_artifact_id=ARTIFACT,original_artifact_sha256=ARTIFACT_SHA,
                       recovered_at=dt.datetime.now(dt.timezone.utc).isoformat(),
                       operations=[{'branch':branch,'commit':head,'feed_verified':True,'changelog_verified':True,
                                    'ulike_policy_verified':True,'manager_download_verified':True}
                                   for branch,head in FEED_HEADS.items()])
        raw=pub.json_bytes(receipt);path=args.output/pub.RECEIPT_NAME;path.write_bytes(raw)
        result={'status':'recovery_preflight_passed_no_remote_writes','source_commit':SOURCE,
                'recovery_source_commit':recovery_source,'heads':{b:s['head'] for b,s in states.items()},
                'receipt_sha256':sha(raw),'receipt_bytes':len(raw),'assets':CI_ASSETS}
        if args.complete:
            saved=save_receipts(pub,args,states,raw)
            pub.run(['gh','release','upload',pub.TAG,'--repo',pub.REPO,path])
            verify_seventh_asset(pub,release['id'],raw)
            check_exact_heads(pub,args.repo,saved)
            result.update(status='published_receipt_recovered_and_verified',
                          receipt_commits={b:s['head'] for b,s in saved.items()},release_url=pub.PAGE)
    (args.output/'recovery-verification.json').write_bytes(encoded(result))
    print(json.dumps(result,ensure_ascii=False,indent=2))

if __name__=='__main__': main()
