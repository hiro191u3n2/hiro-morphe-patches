#!/usr/bin/env python3
"""Verify and publish a scoped TikTok link patch upgrade; no remote writes in review modes."""
from __future__ import annotations
import argparse, base64, datetime as dt, hashlib, importlib.util, io, json, os, re
import subprocess, tempfile, time, urllib.error, urllib.request, zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
SOURCE_ROOT = HERE.parent
spec = importlib.util.spec_from_file_location('tiktok_link_build', SOURCE_ROOT/'src/build.py')
build = importlib.util.module_from_spec(spec); spec.loader.exec_module(build)
require, sha_file, json_bytes = build.require, build.sha, build.json_bytes
REPO = 'hiro191u3n2/hiro-morphe-patches'
VERSION, PREVIOUS, BUNDLE = build.VERSION, build.BASE_VERSION, build.BUNDLE_VERSION
SINGLE, COMBINED, QA_NAME = build.SINGLE, build.BUNDLE, build.QA
BASE_URL, BASE_SHA256 = build.BASE_URL, build.BASE_SHA
TAG, RAW_BRANCH = 'tiktok-link-v1.0.2', 'release/tiktok-link-direct102-310'
RELEASE_ROOT = 'releases/tiktok-link-direct102'
PAGE = f'https://github.com/{REPO}/releases/tag/{TAG}'
REAL_QA_NAME = 'QA_TikTok_LinkDirect_v1.0.2_real_APK.json'
SOURCE_ZIP = 'TikTok_LinkDirect_v1.0.2_sources_and_QA.zip'
RECEIPT_NAME = 'publication_TikTok_LinkDirect_v1.0.2.json'
ASSETS = (SINGLE, COMBINED, QA_NAME, REAL_QA_NAME, SOURCE_ZIP, 'RELEASE_NOTES.txt', 'SHA256SUMS.txt')
POLICY_PATH = 'releases/ULike_ACTIVE.json'
INPUT = {'package':'com.zhiliaoapp.musically','version_name':'47.4.3','version_code':2024704030,
    'bytes':474921201,'sha256':'af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b',
    'signature_verified':True,'signer_certificate_sha256':'9041803e91bcb814b4b4399fb5c85a91640b755e5e8ba76813814bf4cf2ab5ba'}
PATCH_NAME = '共有リンクの小窓を省略して動画を直接再生（TikTok 47.4.3）'
ONEBACK_NAME = '戻る1回で終了・履歴削除'
BASELINE_NAME = 'Hiro_Morphe_Patches_v'+PREVIOUS+'.mpp'
BASELINE_IDENTITY = {'artifact':BASELINE_NAME,'bytes':build.BASE_BYTES,'sha256':BASE_SHA256}
EXPECTED_CASES = {'standalone_link':(SINGLE,{PATCH_NAME}), 'aggregate_link':(COMBINED,{PATCH_NAME}),
                  'aggregate_link_oneback':(COMBINED,{PATCH_NAME,ONEBACK_NAME})}
SIGNATURE_PROOF = ('PASS OriginalIdentity verified=true signer_certificate_sha256='
                   +INPUT['signer_certificate_sha256']+' v1=true v2=true v3=true')

def sha(raw): return hashlib.sha256(raw).hexdigest()

def source_files(include_expected=False):
    result = {}
    for p in sorted(SOURCE_ROOT.rglob('*')):
        if not p.is_file() or '__pycache__' in p.parts or p.suffix in ('.pyc','.class'): continue
        name = p.relative_to(SOURCE_ROOT).as_posix()
        if name == 'publication/expected.json' and not include_expected: continue
        if name == RECEIPT_NAME or name in ASSETS: continue
        require(not p.is_symlink(), 'Source symlink not allowed: '+name)
        require(name.startswith(('src/','publication/')) or name in ('README.md','LICENSE','NOTICE'), 'Unexpected source file '+name)
        build.checked_path(name)
        raw = p.read_bytes(); raw.decode('utf-8'); require(b'\x00' not in raw, 'Binary source file '+name)
        result[name] = raw
    return result

def source_hashes(): return {name:sha(raw) for name,raw in source_files().items()}

def validate_qa(qa):
    for key, value in {'schema':'tiktok-link-direct-build-v3','version':VERSION,'bundle_version':BUNDLE,
        'baseline_version':PREVIOUS,'baseline_sha256':BASE_SHA256,'target_package':INPUT['package'],
        'target_version':'47.4.3','physical_android_tested':False,'all_nonowned_loader_definitions_preserved':True,
        'all_nonowned_zip_entries_preserved_except_manifest_and_loader_dex':True}.items():
        require(qa.get(key) == value, 'Build proof mismatch: '+key)
    require(qa.get('toolchain_sha256') == build.TOOLS, 'Build tool identities differ')
    require(qa.get('replaced_baseline_jvm_sha256') == build.OWNED_BASE_JVM, 'Wrong replacement scope or baseline own-loader pins')
    require(qa.get('sources') == build.source_ids(SOURCE_ROOT/'src'), 'Build proof is not for current source')
    require(qa.get('loader_preservation','').startswith('PASS BundleMergeAudit'), 'Missing actual loader preservation audit')
    require(set(qa.get('metadata',{})) == {'morphe.jar','morphe200.jar'}, 'Both loader SDK reports required')
    require(all(v.startswith('PASS MetadataAudit') for v in qa['metadata'].values()), 'Metadata loader audit failed')
    require(set(qa.get('artifacts',{})) == {SINGLE,COMBINED}, 'Wrong build artifact identities')
    require(qa.get('runtime_added') is True, 'Dedicated runtime extension required')
    require(qa.get('baseline_oneback_extension') == build.BASE_ONEBACK_EXTENSION, 'Baseline OneBack extension pin differs')
    classes = qa.get('runtime_classes')
    require(isinstance(classes,list) and classes and classes == sorted(set(classes))
            and all(n.startswith(build.RUNTIME_PREFIX) and n.endswith('.class') for n in classes), 'Runtime class inventory differs')
    require(qa.get('runtime_extension_audit','').startswith('PASS RuntimeExtensionAudit '), 'Missing actual serialized runtime audit')
    require(qa.get('runtime_host_test','').startswith('PASS SharedLinkExitHostTest '), 'Missing actual compiled runtime host tests')

def validate_case(case, name, artifact_sha, selected):
    require(case.get('artifact') == name and case.get('artifact_sha256') == artifact_sha, 'Case MPP identity differs')
    require(case.get('result') == 'PASS' and case.get('steps_all_success') is True
            and case.get('failed_patches') == [], 'Original-APK application failed')
    for key in ('selected_patches','applied_patches'):
        value = case.get(key)
        require(isinstance(value,list) and len(value) == len(selected) and set(value) == selected,
                'Exact selected/applied patch inventory differs: '+key)
    require(re.fullmatch(r'[0-9a-f]{64}',case.get('output_apk_sha256','')) is not None, 'Missing output APK hash')
    require(type(case.get('output_apk_bytes')) is int and case['output_apk_bytes'] > 0, 'Missing output APK byte count')

def validate_real_apk(proof, artifacts):
    require(proof.get('schema') == 'tiktok-link-direct-real-apk-v3', 'Wrong original-APK proof schema')
    require(proof.get('physical_android_tested') is False, 'Physical device confirmation is not available')
    require(proof.get('input') == INPUT, 'Original APK/signature identity differs')
    require(isinstance(proof.get('original_signature'),str) and proof['original_signature'].startswith(SIGNATURE_PROOF),
            'Actual original APK signature verification output missing')
    require(proof.get('artifacts') == artifacts, 'Original-APK proof uses different MPP bytes')
    require(proof.get('baseline') == BASELINE_IDENTITY, 'Combined proof uses a different published baseline')
    require(proof.get('baseline_oneback_extension') == build.BASE_ONEBACK_EXTENSION, 'Combined proof uses a different OneBack runtime')
    require(proof.get('patcher_sdk_sha256') == build.TOOLS['morphe200.jar'], 'Actual application SDK identity differs')
    require(proof.get('runtime_host_test','').startswith('PASS SharedLinkExitHostTest '), 'Actual runtime host proof missing')
    runtime = proof.get('runtime_extension')
    require(isinstance(runtime,dict) and runtime.get('path') == build.EXTENSION
            and type(runtime.get('bytes')) is int and runtime['bytes'] > 0
            and re.fullmatch(r'[0-9a-f]{64}',runtime.get('sha256','')) is not None, 'Actual runtime extension identity missing')
    for key,prefix in (('original_contract','PASS LinkBranchAudit original:'),
                       ('original_fast_route_contract','PASS LinkFastRouteAudit original:'),
                       ('original_back_contract','PASS LinkBackAudit original:')):
        require(proof.get(key,'').startswith(prefix), 'Original contract preflight missing: '+key)
    admission = proof.get('forced_target_admission',{})
    require(set(admission) == {'morphe.jar','morphe200.jar'}
            and all(v.startswith('PASS TargetAdmissionTest') for v in admission.values()), 'Forced target rejection proof missing')
    baseline = proof.get('baseline_oneback')
    require(isinstance(baseline,dict) and baseline.get('case') == 'baseline_oneback', 'Baseline OneBack-only reference run missing')
    validate_case(baseline,BASELINE_NAME,BASE_SHA256,{ONEBACK_NAME})
    cases = proof.get('cases',[])
    require(isinstance(cases,list) and len(cases) == len(EXPECTED_CASES), 'All three exact new actual-APK cases required')
    seen = set()
    for case in cases:
        label = case.get('case'); require(label in EXPECTED_CASES and label not in seen, 'Unknown/duplicate actual-APK case')
        name, selected = EXPECTED_CASES[label]
        validate_case(case,name,artifacts[name]['sha256'],selected)
        require(case.get('direct_link_audit','').startswith('PASS LinkBranchAudit output:'), 'Missing emitted direct-link call-site audit')
        require(case.get('fast_route_audit','').startswith('PASS LinkFastRouteAudit output:'), 'Missing emitted target-first routing audit')
        require(case.get('back_exit_audit','').startswith('PASS LinkBackAudit output:'), 'Missing emitted shared-link Back audit')
        if label == 'aggregate_link_oneback':
            require(case.get('baseline_reference_apk_sha256') == baseline['output_apk_sha256'], 'Combined audit did not use exact OneBack-only output')
        else:
            require(case.get('baseline_reference_apk_sha256') is None, 'Link-only case must compare to the original APK')
        seen.add(label)
    require(seen == set(EXPECTED_CASES), 'A required new output case is absent')

def artifacts_at(dist):
    return {name:{'bytes':(dist/name).stat().st_size,'sha256':sha_file(dist/name)} for name in (SINGLE,COMBINED)}

def validate_build(dist, base):
    require(sha_file(base) == BASE_SHA256 and base.stat().st_size == build.BASE_BYTES, 'Baseline bytes changed')
    qa_raw = (dist/QA_NAME).read_bytes(); qa = json.loads(qa_raw); validate_qa(qa)
    artifacts = artifacts_at(dist); require(qa['artifacts'] == artifacts, 'Build proof MPP hashes differ')
    original = build.archive(base); combined = build.archive(dist/COMBINED); single = build.archive(dist/SINGLE)
    build.validate_baseline(original)
    require(build.headers(original[build.MF])['Version'] == PREVIOUS, 'Baseline manifest differs')
    require(build.headers(combined[build.MF])['Version'] == BUNDLE, 'Aggregate manifest differs')
    require(build.headers(single[build.MF])['Version'] == VERSION, 'Standalone manifest differs')
    retained = {n:raw for n,raw in original.items() if n not in (build.MF,'classes.dex') and n not in build.OWNED_BASE_JVM}
    require(all(combined.get(n) == raw for n,raw in retained.items()),
            'Unrelated JVM patch, extension, resource or old notice changed')
    fresh_names = set(single)-{build.MF,'classes.dex'}
    require(set(combined) == (set(original)-set(build.OWNED_BASE_JVM))|fresh_names,
            'Aggregate contains unexpected entries or removed unrelated entries')
    require(build.PREFIX+'TikTokLinkDirectPatch.class' in fresh_names, 'Owned replacement loader missing')
    for name in fresh_names:
        require(name.startswith((build.PREFIX,build.NOTICE_PREFIX)) or name == build.EXTENSION, 'Unexpected replacement/new entry '+name)
        require(combined[name] == single[name], 'Standalone/aggregate owned bytes differ: '+name)
    runtime = single.get(build.EXTENSION)
    require(runtime is not None, 'Dedicated runtime extension missing from actual MPP')
    runtime_identity = {'path':build.EXTENSION,'bytes':len(runtime),'sha256':sha(runtime)}
    require(qa.get('runtime_extension') == runtime_identity, 'Runtime audit proof uses different extension bytes')
    require(combined[build.BASE_ONEBACK_EXTENSION['path']] == original[build.BASE_ONEBACK_EXTENSION['path']], 'Existing OneBack runtime changed')
    real_raw = (SOURCE_ROOT/'src/validation/real-apk.json').read_bytes()
    real = json.loads(real_raw); validate_real_apk(real, artifacts)
    require(real.get('runtime_extension') == runtime_identity and real.get('runtime_host_test') == qa['runtime_host_test'],
            'Original-APK proof must identify the same audited runtime and host-test result')
    return qa_raw, real_raw, artifacts

def expected_source_archive(qa_raw, real_raw):
    result = source_files(include_expected=True)
    result[QA_NAME] = qa_raw; result[REAL_QA_NAME] = real_raw
    return result

def validate_local(dist, base, expected):
    qa_raw, real_raw, artifacts = validate_build(dist,base)
    cfg = json.loads(expected.read_text())
    require(cfg.get('schema') == 'tiktok-link-direct-frozen-v3' and cfg.get('two_local_builds_identical') is True,
            'Two clean local builds must be frozen')
    require(cfg.get('artifacts') == artifacts and cfg.get('qa_sha256') == sha(qa_raw)
            and cfg.get('real_apk_sha256') == sha(real_raw), 'Reviewed output identity differs')
    require(cfg.get('source_sha256') == source_hashes(), 'Sources changed after review/freeze')
    for key in ('reviewed_main_head','reviewed_dev_head'):
        require(re.fullmatch(r'[0-9a-f]{40}',cfg.get(key,'')) is not None, 'Missing reviewed remote head '+key)
    require((dist/REAL_QA_NAME).read_bytes() == real_raw, 'Packaged actual-APK proof differs')
    require(build.archive(dist/SOURCE_ZIP) == expected_source_archive(qa_raw,real_raw), 'Corresponding source archive differs')
    notes = (HERE/'RELEASE_NOTES.txt').read_bytes()
    require((dist/'RELEASE_NOTES.txt').read_bytes() == notes, 'Release notes changed')
    sums = ''.join(sha_file(dist/name)+'  '+name+'\n' for name in ASSETS if name != 'SHA256SUMS.txt')
    require((dist/'SHA256SUMS.txt').read_text() == sums, 'Checksum inventory differs')
    payloads = {name:(dist/name).read_bytes() for name in ASSETS}
    assets = {name:{'bytes':len(raw),'sha256':sha(raw)} for name,raw in payloads.items()}
    return cfg, payloads, notes.decode(), assets

def api(path, data=None, method=None, absent=False):
    cmd = ['gh','api',path]
    if method: cmd += ['--method',method]
    if data is not None: cmd += ['--input','-']
    r = subprocess.run(cmd,input=None if data is None else json.dumps(data),text=True,capture_output=True)
    if r.returncode:
        if absent and data is None and '(HTTP 404)' in r.stderr: return None
        raise RuntimeError('GitHub API failed: '+r.stderr.strip())
    return json.loads(r.stdout) if r.stdout.strip() else None

def fetch(url):
    for attempt in range(4):
        try:
            request = urllib.request.Request(url,headers={'User-Agent':'Hiro-TikTok-Link-Publication','Cache-Control':'no-cache'})
            with urllib.request.urlopen(request,timeout=90) as r:
                require(r.status == 200, 'Public download must return HTTP200'); return r.read()
        except (OSError,urllib.error.URLError):
            if attempt == 3: raise
            time.sleep(2**attempt)

def head(branch): return api(f'repos/{REPO}/git/ref/heads/{branch}')['object']['sha']

def content(path, ref):
    build.checked_path(path)
    require(re.fullmatch(r'[0-9a-f]{40}',ref) is not None, 'Immutable commit required')
    row = api(f'repos/{REPO}/contents/{path}?ref={ref}')
    require(row.get('type') == 'file', 'Expected repository file')
    raw = base64.b64decode(row['content']) if row.get('encoding') == 'base64' else fetch(f'https://raw.githubusercontent.com/{REPO}/{ref}/{path}')
    require(len(raw) == row['size'] and hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest() == row['sha'], 'Git blob identity differs')
    return raw

def snapshot(branch):
    current = head(branch)
    return {'branch':branch,'head':current,'feed':json.loads(content('patches-bundle.json',current)),
            'log':content('CHANGELOG.md',current).decode(),'policy':content(POLICY_PATH,current)}

def check_heads(states):
    for state in states.values(): require(head(state['branch']) == state['head'], 'Concurrent update: '+state['branch'])

def git(repo,*args,input=None,env=None):
    cmd = ['git','-C',str(repo),'-c','credential.helper=','-c','credential.helper=!gh auth git-credential',*map(str,args)]
    r = subprocess.run(cmd,input=input,stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=env)
    require(r.returncode == 0, 'Git failed '+str(args)+'\n'+r.stderr.decode(errors='replace'))
    return r.stdout.decode().strip()

def prepare_commit(repo,state,files,message):
    env = {**os.environ,'GIT_AUTHOR_NAME':'github-actions[bot]','GIT_COMMITTER_NAME':'github-actions[bot]',
           'GIT_AUTHOR_EMAIL':'41898282+github-actions[bot]@users.noreply.github.com',
           'GIT_COMMITTER_EMAIL':'41898282+github-actions[bot]@users.noreply.github.com'}
    with tempfile.TemporaryDirectory(prefix='tiktok-link-index-') as tmp:
        env['GIT_INDEX_FILE'] = str(Path(tmp)/'index')
        git(repo,'read-tree',state['head'],env=env)
        for path,raw in files.items():
            build.checked_path(path)
            blob = git(repo,'hash-object','-w','--stdin',input=raw,env=env)
            git(repo,'update-index','--add','--cacheinfo',f'100644,{blob},{path}',env=env)
        tree = git(repo,'write-tree',env=env)
        commit = git(repo,'commit-tree',tree,'-p',state['head'],'-F','-',input=(message+'\n').encode(),env=env)
        require(git(repo,'show','-s','--format=%P',commit) == state['head'], 'Commit parent differs')
        changed = set(git(repo,'diff-tree','--no-commit-id','--name-only','-r',commit).splitlines())
        require(changed and changed.issubset(files), 'Unexpected changed repository paths')
        return {**state,'head':commit}

def commit_feeds(repo,states,updates,message):
    require(set(states) == set(updates) == {'main','dev'}, 'Both feed branches required')
    check_heads(states)
    git(repo,'fetch','--no-tags',f'https://github.com/{REPO}.git',states['main']['head'],states['dev']['head'])
    prepared = {branch:prepare_commit(repo,state,updates[branch],message) for branch,state in states.items()}
    check_heads(states)
    git(repo,'push','--atomic',f"--force-with-lease=refs/heads/main:{states['main']['head']}",
        f"--force-with-lease=refs/heads/dev:{states['dev']['head']}",f'https://github.com/{REPO}.git',
        f"{prepared['main']['head']}:refs/heads/main",f"{prepared['dev']['head']}:refs/heads/dev")
    check_heads(prepared); return prepared

def metadata(state,url,created,notes):
    old = state['feed']
    require(old['version'] == PREVIOUS and old['download_url'] == BASE_URL, 'Active feed advanced from reviewed baseline309')
    require(created > old['created_at'], 'Feed timestamp must increase')
    feed = {**old,'version':BUNDLE,'created_at':created,'download_url':url,'page_url':PAGE,
            'signature_download_url':'','description':notes.strip()+'\n\n'+old['description']}
    prefix = f'# {BUNDLE} ({created[:10]})\n\n* **TikTok:** 共有動画リンクの遷移処理を更新 v{VERSION}。\n\n'+notes.strip()+'\n\n'
    require(re.findall(r'^\* \*\*(.+?):\*\*',prefix,re.M) == ['TikTok'], 'Unexpected Manager app update scope')
    return feed,prefix+state['log']

def verify_assets(release_id,expected,public):
    release = api(f'repos/{REPO}/releases/{release_id}')
    require(release['tag_name'] == TAG and release['draft'] is (not public) and release['prerelease'] is True, 'Release identity/state differs')
    assets = {a['name']:a for a in release['assets']}
    require(set(assets) == set(expected) and len(assets) == len(release['assets']), 'Release asset inventory differs')
    for name,row in expected.items():
        raw = subprocess.check_output(['gh','api',f"repos/{REPO}/releases/assets/{assets[name]['id']}",'-H','Accept: application/octet-stream'])
        require(len(raw) == row['bytes'] and sha(raw) == row['sha256'], 'Release asset bytes differ: '+name)

def publish(args):
    cfg,payloads,notes,assets = validate_local(args.dist,args.base,args.expected)
    if args.local_only:
        print(json.dumps({'status':'local_verified_no_remote_writes','assets':assets},indent=2)); return
    require(os.environ.get('GITHUB_ACTIONS') == 'true', 'Publication requires verified GitHub Actions run')
    ci = json.loads((args.dist/'ci-pair.json').read_text())
    require(ci.get('run_id') == os.environ.get('GITHUB_RUN_ID') and ci.get('source_commit') == os.environ.get('GITHUB_SHA')
            and ci.get('artifacts') == cfg['artifacts'] and ci.get('qa_sha256') == cfg['qa_sha256'], 'Current CI pair proof missing')
    source_head = os.environ['GITHUB_SHA']
    require(git(args.repo,'rev-parse','HEAD') == source_head, 'Checkout differs from workflow source')
    require(git(args.repo,'show','-s','--format=%P',source_head) == cfg['reviewed_main_head'], 'Source commit must descend directly from reviewed main')
    require((args.repo/'.github/workflows/tiktok-link-direct102-publish.yml').read_bytes() == (HERE/'workflow.yml').read_bytes(), 'Workflow mirror differs')
    states = {branch:snapshot(branch) for branch in ('main','dev')}
    require(states['main']['head'] == source_head and states['dev']['head'] == cfg['reviewed_dev_head'], 'Reviewed main/dev moved')
    require(states['main']['feed'] == states['dev']['feed'], 'Manager feeds diverged')
    require(sha(fetch(BASE_URL)) == BASE_SHA256, 'Public baseline bytes changed')
    created = dt.datetime.now(dt.timezone.utc).replace(tzinfo=None,microsecond=0).isoformat()
    for state in states.values(): metadata(state,'https://example.invalid/preflight',created,notes)
    for path in (f'repos/{REPO}/releases/tags/{TAG}',f'repos/{REPO}/git/ref/tags/{TAG}',f'repos/{REPO}/git/ref/heads/{RAW_BRANCH}'):
        require(api(path,absent=True) is None, 'Release/tag/distribution branch exists; inspect before retry')
    for state in states.values():
        require(api(f"repos/{REPO}/contents/downloads/{COMBINED}?ref={state['head']}",absent=True) is None, 'Bundle filename already present')
    require(not any(a.get('name') == COMBINED for r in api(f'repos/{REPO}/releases?per_page=100') for a in r.get('assets',[])), 'Bundle version already published')
    if args.preflight_only:
        print(json.dumps({'status':'preflight_passed_no_remote_writes','heads':{k:v['head'] for k,v in states.items()}},indent=2)); return
    receipt_path = args.dist/RECEIPT_NAME
    require(not receipt_path.exists(), 'Prior receipt exists; inspect before retry')
    receipt = {'schema':'tiktok-link-direct-publication-v3','version':VERSION,'bundle_version':BUNDLE,
               'source_commit':source_head,'assets':assets,'physical_android_tested':False,'original_apk_apply_tested':True}
    def checkpoint(status,**changes):
        receipt.update(status=status,**changes); receipt_path.write_bytes(json_bytes(receipt)); print(status,flush=True)
    checkpoint('preflight_passed')
    try:
        check_heads(states)
        release = api(f'repos/{REPO}/releases',{'tag_name':TAG,'target_commitish':source_head,
            'name':'Hiro TikTok Link Direct v1.0.2 / bundle 1.0.310','body':notes,'draft':True,'prerelease':True,'make_latest':'false'})
        checkpoint('draft_created',release_id=release['id'])
        for name in ASSETS: subprocess.run(['gh','release','upload',TAG,'--repo',REPO,str(args.dist/name)],check=True)
        verify_assets(release['id'],assets,False); checkpoint('draft_assets_verified')
        check_heads(states)
        raw_state = prepare_commit(args.repo,{**states['main'],'branch':RAW_BRANCH},
            {'downloads/'+name:payloads[name] for name in (SINGLE,COMBINED)},'release: immutable TikTok link patch and bundle310 bytes')
        git(args.repo,'push',f'--force-with-lease=refs/heads/{RAW_BRANCH}:',f'https://github.com/{REPO}.git',f"{raw_state['head']}:refs/heads/{RAW_BRANCH}")
        urls = {name:f"https://raw.githubusercontent.com/{REPO}/{raw_state['head']}/downloads/{name}" for name in (SINGLE,COMBINED)}
        require(head(RAW_BRANCH) == raw_state['head'], 'Immutable distribution branch moved')
        for name,url in urls.items(): require(fetch(url) == payloads[name], 'Immutable distribution bytes differ')
        checkpoint('immutable_downloads_verified',raw_commit=raw_state['head'],download_urls=urls)
        check_heads(states)
        api(f"repos/{REPO}/releases/{release['id']}",{'draft':False,'prerelease':True,'make_latest':'false'},'PATCH')
        verify_assets(release['id'],assets,True); checkpoint('release_assets_verified')
        updates,expected_metadata = {},{}
        for branch,state in states.items():
            feed,log = metadata(state,urls[COMBINED],created,notes); expected_metadata[branch] = (feed,log)
            files = {'patches-bundle.json':json_bytes(feed),'CHANGELOG.md':log.encode()}
            for name,raw in source_files(include_expected=True).items(): files[RELEASE_ROOT+'/'+name] = raw
            for name in (QA_NAME,REAL_QA_NAME,'RELEASE_NOTES.txt','SHA256SUMS.txt'): files[RELEASE_ROOT+'/'+name] = payloads[name]
            updates[branch] = files
        active = commit_feeds(args.repo,states,updates,'release: TikTok shared-video link direct patch and bundle310 Manager update')
        for branch,state in active.items():
            feed,log = expected_metadata[branch]
            require(json.loads(content('patches-bundle.json',state['head'])) == feed and content('CHANGELOG.md',state['head']).decode() == log, 'Remote feed/log differ')
            require(content(POLICY_PATH,state['head']) == states[branch]['policy'], 'Unrelated ULike policy changed')
            require(fetch(feed['download_url']) == payloads[COMBINED], 'Manager download bytes differ')
            for attempt in range(10):
                public = json.loads(fetch(f'https://raw.githubusercontent.com/{REPO}/{branch}/patches-bundle.json?verify={time.time_ns()}'))
                if public == feed: break
                check_heads(active); time.sleep(3)
            else: raise RuntimeError('Public feed propagation not confirmed: '+branch)
        check_heads(active); verify_assets(release['id'],assets,True)
        checkpoint('published_and_verified',release_url=PAGE,manager_main_dev_updated_atomically=True,
                   manager_app_scope='TikTok',main_commit=active['main']['head'],dev_commit=active['dev']['head'])
        saved = commit_feeds(args.repo,active,{b:{RELEASE_ROOT+'/'+RECEIPT_NAME:receipt_path.read_bytes()} for b in active},
            'docs: verified TikTok direct-link publication receipt')
        for state in saved.values(): require(content(RELEASE_ROOT+'/'+RECEIPT_NAME,state['head']) == receipt_path.read_bytes(), 'Receipt save differs')
        print('PASS published exact assets, immutable MPP downloads and both Manager feeds',flush=True)
    except BaseException as error:
        checkpoint('failed_requires_inspection',error=type(error).__name__+': '+str(error)); raise

def main():
    p = argparse.ArgumentParser(description=__doc__)
    for name in ('dist','base','repo'): p.add_argument('--'+name,type=Path,required=True)
    p.add_argument('--expected',type=Path,default=HERE/'expected.json')
    modes = p.add_mutually_exclusive_group(); modes.add_argument('--local-only',action='store_true'); modes.add_argument('--preflight-only',action='store_true')
    args = p.parse_args()
    for name in ('dist','base','repo','expected'): setattr(args,name,getattr(args,name).resolve())
    publish(args)

if __name__ == '__main__': main()
