#!/usr/bin/env python3
"""Exact uploaded188 rollback over latest141; fail closed on stale inputs or concurrent publication."""
import base64, datetime, hashlib, io, json, os, re, shutil, subprocess, sys, time, urllib.error, urllib.request, zipfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent
REPO = 'hiro191u3n2/hiro-morphe-patches'
VERSION, PREVIOUS_APP, BUNDLE, PREVIOUS = '1.8.8', '1.9.17', '1.0.142', '1.0.141'
TAG = 'ulike-rollback-v1.8.8-bundle-v1.0.142'
BAD_TAG = 'ulike-v1.9.17'
RAW_BRANCH = 'release/ulike-rollback188-142'
DONOR = 'ULike_HQ_Texture_Online_v1.8.8.mpp'
BASE = 'Hiro_Morphe_Patches_v1.0.141.mpp'
OUTPUT = 'Hiro_Morphe_Patches_v1.0.142.mpp'
PINS = {
    DONOR: 'dbdaab4f578604b725f6285d1312eb33f52160cbc024a9168b95202b1dde2d79',
    BASE: '5b9ffa2d004b24aa2394be148aeb3ca578d3b133ec6616ca259b5d2afb4fd486',
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
}
PAGE = f'https://github.com/{REPO}/releases/tag/{TAG}'
NOTES = f'''【統合MPP v{BUNDLE}：ULike v{VERSION} 添付ファイルへの差し替え】

* **ULike:** ユーザー指定のULike_HQ_Texture_Online_v1.8.8.mppへ戻しました。添付ファイルのSHA-256は{PINS[DONOR]}で、再配布する単体MPPは添付と完全一致します。
* **ULike - 差し替え:** 最新統合v1.0.141を土台に、ULikeローダー全23クラス、JVMクラス、メソッドパッチ、runtime、素材、ネイティブ関連ファイルをv1.8.8へ置き換えます。v1.9.17にのみ存在するulike191の3ファイルも除去します。v1.9系の10bit経路を残す混合版ではありません。
* **ULike - 保持検証:** 他アプリのローダー220クラスをクラス単位の正規化DEXで照合し、その他の同梱ファイルは展開後の全バイトを照合します。独立した2回の統合結果も一致検証します。
* **ULike - 更新:** パッチ本体はv1.8.8ですが、統合版はv1.0.142へ更新します。ULikeを更新対象とし、他アプリを今回の変更で更新対象にしない配布情報へ更新します。
* **ULike - 不具合版:** ULike v1.9.17は採用を取り消します。新配信の確認後、当該リリースの不具合MPP配布物を削除し、リリースに撤回表示を付けます。過去のGit履歴の強制書換えはしません。

対象は未改造ULike 5.6.2（740）、arm64-v8aです。Morphe Managerでソース更新後、単体版または統合版の一方で再パッチ・再インストールしてください。ソース更新だけではインストール済みアプリは戻りません。

今回行うのは指定済みMPPへの差し替えです。Galaxy実機での撮影・保存試験、および元APKへの再適用試験は今回実行していません。v1.8.8の機能範囲と制限をそのまま引き継ぎ、10bit撮影から保存までの成功を新たに保証する変更ではありません。
'''
SUMMARY = '添付v1.8.8へ完全差し替え。不具合v1.9.17を撤回し、他アプリを維持。'

def require(ok, why):
    if not ok: raise RuntimeError(why)

def sha(raw): return hashlib.sha256(raw).hexdigest()

def api(path, data=None, method=None, absent=False):
    cmd = ['gh', 'api', path]
    if method: cmd += ['-X', method]
    if data is not None: cmd += ['--input', '-']
    p = subprocess.run(cmd, input=None if data is None else json.dumps(data), text=True, capture_output=True)
    if p.returncode:
        if absent and data is None and '(HTTP 404)' in p.stderr: return None
        raise RuntimeError('GitHub API failure: ' + p.stderr.strip())
    return json.loads(p.stdout) if p.stdout.strip() else None

def fetch(url):
    for attempt in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent':'Hiro-ULike-rollback142','Accept-Encoding':'identity'}), timeout=90) as r:
                return r.read()
        except urllib.error.HTTPError as error:
            if attempt == 3 or error.code not in (404,429,500,502,503,504): raise
            time.sleep(2 ** attempt)

def head(branch): return api(f'repos/{REPO}/git/ref/heads/{branch}')['object']['sha']
def content(path, ref): return base64.b64decode(api(f'repos/{REPO}/contents/{path}?ref={ref}')['content'])
def snapshot(branch):
    h = head(branch); c = api(f'repos/{REPO}/git/commits/{h}')
    return {'branch':branch, 'head':h, 'tree':c['tree']['sha'],
            'manifest':json.loads(content('patches-bundle.json',h)), 'log':content('CHANGELOG.md',h).decode()}

def check_heads(states):
    for s in states.values(): require(head(s['branch']) == s['head'], 'Concurrent update: '+s['branch'])

def commit_files(state, files, message):
    check_heads({'target':state}); entries = []
    for path, raw in files.items():
        b = api(f'repos/{REPO}/git/blobs', {'encoding':'base64', 'content':base64.b64encode(raw).decode()})
        entries.append({'path':path,'mode':'100644','type':'blob','sha':b['sha']})
    tree = api(f'repos/{REPO}/git/trees', {'base_tree':state['tree'],'tree':entries})
    commit = api(f'repos/{REPO}/git/commits', {'tree':tree['sha'],'parents':[state['head']],'message':message})
    check_heads({'target':state})
    api(f'repos/{REPO}/git/refs/heads/{state["branch"]}', {'sha':commit['sha'],'force':False}, 'PATCH')
    require(head(state['branch']) == commit['sha'], 'Commit moved after publication')
    return {**state,'head':commit['sha'],'tree':tree['sha']}

def headers(raw):
    rows=[]
    for line in raw.replace(b'\r\n',b'\n').split(b'\n'):
        if line.startswith(b' '): rows[-1] += line[1:]
        elif line: rows.append(line)
    pairs=[r.decode().split(': ',1) for r in rows]
    require(len(pairs)==len(dict(pairs)),'Duplicate manifest key')
    return dict(pairs)

def manifest(fields):
    lines=[]
    for key,value in fields.items():
        require(not any(x in value for x in '\r\n\0'),'Invalid manifest value')
        raw=(key+': '+value).encode(); prefix=b''
        while len(prefix)+len(raw)>72:
            cut=72-len(prefix)
            while raw[cut]&192==128: cut-=1
            lines.append(prefix+raw[:cut]);raw=raw[cut:];prefix=b' '
        lines.append(prefix+raw)
    raw=b'\r\n'.join(lines)+b'\r\n\r\n'
    require(headers(raw)==fields,'Manifest round-trip failed');return raw

def archive(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None,'Corrupt ZIP: '+str(path))
        require(len(z.namelist())==len(set(z.namelist())),'Duplicate ZIP paths')
        for n in z.namelist(): require(not Path(n).is_absolute() and '..' not in Path(n).parts,'Unsafe ZIP path')
        return {n:z.read(n) for n in z.namelist() if not n.endswith('/')}

def target(n): return n.startswith('app/hiro/ulike/patches/') or re.match(r'^ulike(?:\d+)?/',n) is not None

def write_zip(path, items):
    with zipfile.ZipFile(path,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for n in sorted(items, key=lambda x:(x!='META-INF/MANIFEST.MF',x)):
            i=zipfile.ZipInfo(n,(2026,10,5,0,0,0));i.compress_type=zipfile.ZIP_DEFLATED;i.external_attr=0o100644<<16
            z.writestr(i,items[n],compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)

def run(cmd): subprocess.run(list(map(str,cmd)),check=True)

def build():
    for folder in ('input','toolchain','build','dist'): Path(folder).mkdir(exist_ok=True)
    urls = {
        DONOR:f'https://github.com/{REPO}/releases/download/ulike-v1.8.8/{DONOR}',
        BASE:f'https://raw.githubusercontent.com/{REPO}/737314bba14bf74ab016f9fc5a4283883904a239/downloads/{BASE}',
        'morphe.jar':'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.16.0/morphe-desktop-1.16.0-all.jar',
    }
    for name,url in urls.items():
        path=Path('toolchain' if name=='morphe.jar' else 'input')/name
        raw=path.read_bytes() if path.exists() else fetch(url)
        require(sha(raw)==PINS[name],'Input SHA-256 mismatch: '+name);path.write_bytes(raw)
    base,donor=archive(Path('input')/BASE),archive(Path('input')/DONOR)
    require(headers(base['META-INF/MANIFEST.MF'])['Version']==PREVIOUS,'Wrong base version')
    require(headers(donor['META-INF/MANIFEST.MF'])['Version']==VERSION,'Wrong donor version')
    require(all(target(n) or n in ('classes.dex','META-INF/MANIFEST.MF') for n in donor),'Donor contains unrelated files')
    Path('build/base.dex').write_bytes(base['classes.dex']);Path('build/donor.dex').write_bytes(donor['classes.dex'])
    run(['javac','-cp','toolchain/morphe.jar','-d','build',ROOT/'Rollback188.java'])
    mf=headers(base['META-INF/MANIFEST.MF']);mf.update(Version=BUNDLE,Timestamp='2026-10-05T00:00:00',Description='ULike rollback to exact uploaded v1.8.8; all other applications retained from bundle v1.0.141. Device test not performed.')
    retained={n:raw for n,raw in base.items() if not target(n) and n not in ('classes.dex','META-INF/MANIFEST.MF')}
    replacement={n:raw for n,raw in donor.items() if target(n)}
    removed=sorted(n for n in base if target(n) and n not in replacement)
    require(removed==['ulike191/runtime/0000.bin','ulike191/runtime/0001.bin','ulike191/runtime/0002.bin'],'Unexpected discarded resource set')
    for index in (1,2):
        run(['java','-cp','toolchain/morphe.jar'+os.pathsep+'build','Rollback188','build/base.dex','build/donor.dex',f'build/merged{index}.dex',f'build/classes{index}.tsv'])
        write_zip(Path('dist')/(OUTPUT if index==1 else 'independent.mpp'),{**retained,**replacement,'classes.dex':Path(f'build/merged{index}.dex').read_bytes(),'META-INF/MANIFEST.MF':manifest(mf)})
    output=(Path('dist')/OUTPUT).read_bytes()
    require(output==Path('dist/independent.mpp').read_bytes(),'Independent merge mismatch');Path('dist/independent.mpp').unlink()
    actual=archive(Path('dist')/OUTPUT)
    require({n:raw for n,raw in actual.items() if target(n)}==replacement,'Donor resources not exact')
    require({n:raw for n,raw in actual.items() if not target(n) and n not in ('classes.dex','META-INF/MANIFEST.MF')}==retained,'Other app bytes changed')
    require(not any(n.startswith('ulike191/') for n in actual),'Withdrawn runtime residue')
    shutil.copyfile(Path('input')/DONOR,Path('dist')/DONOR)
    qa={'result':'PASS_EXACT_UPLOADED188_REPLACEMENT_OTHER_APPS_PRESERVED','bundle_version':BUNDLE,'ulike_version':VERSION,
        'previous_bundle_version':PREVIOUS,'withdrawn_ulike_version':PREVIOUS_APP,'attachment_sha256':PINS[DONOR],
        'standalone_byte_identical_to_attachment':True,'replaced_loader_classes':23,'unchanged_other_loader_classes':220,
        'replaced_resource_entries':len(replacement),'unchanged_other_entries':len(retained),'removed_entries':removed,
        'all_ulike_payload_entries_byte_identical_to_attachment':True,'all_other_entries_byte_identical_to_latest_bundle':True,
        'independent_rebuild_byte_identical':True,'android_device_tested':False,'original_apk_apply_tested_this_release':False,
        'toolchain_sha256':PINS['morphe.jar'],'base_bundle_sha256':PINS[BASE],
        'artifacts':{n:{'bytes':(Path('dist')/n).stat().st_size,'sha256':sha((Path('dist')/n).read_bytes())} for n in (OUTPUT,DONOR)}}
    Path('dist/QA_ULike_rollback_v1.0.142.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
    Path('dist/RELEASE_NOTES.txt').write_text(NOTES)
    write_zip(Path('dist/ULike_rollback_v1.0.142_sources_and_QA.zip'),{
        'rollback142.py':Path(__file__).read_bytes(),'Rollback188.java':(ROOT/'Rollback188.java').read_bytes(),
        'QA.json':Path('dist/QA_ULike_rollback_v1.0.142.json').read_bytes(),'loader-class-verification.tsv':Path('build/classes1.tsv').read_bytes(),
        'RELEASE_NOTES.txt':NOTES.encode()})
    print(json.dumps(qa,ensure_ascii=False,indent=2),flush=True)

HEADING=re.compile(r'^#{1,3}\s+(?:\S+\s+)?(?:\[([^]]+)]\([^)]*\)|([^\s\[(]+))\s+\((\d{4}-\d{2}-\d{2})\)',re.I)
SCOPE=re.compile(r'^\* \*\*(.+?):\*\*')
def versions(v): return tuple(map(int,v.removeprefix('v').split('.')))
def eligible(log,baseline,app):
    current=None
    for line in log.splitlines():
        h=HEADING.match(line)
        if h: current=h[1] or h[2]
        s=SCOPE.match(line.strip())
        if s and current and re.fullmatch(r'\d+\.\d+\.\d+',current) and versions(current)>versions(baseline):
            scope=s[1].casefold()
            if scope==app.casefold() or scope.startswith(app.casefold()+' - '): return True
    return False

def metadata(state,url,created):
    old=state['manifest']; require(old['version']==PREVIOUS,'Another release is already active')
    require(datetime.datetime.fromisoformat(created)>datetime.datetime.fromisoformat(old['created_at']),'Timestamp must advance')
    pattern=r'(?m)^ULike：v'+re.escape(PREVIOUS_APP)+r'（5\.6\.2／740）(?=\r?$)'
    require(len(re.findall(pattern,old['description']))==1,'Ambiguous current ULike inventory')
    description=NOTES.strip()+'\n\n'+re.sub(pattern,f'ULike：v{VERSION}（5.6.2／740）',old['description'],count=1)
    data={**old,'created_at':created,'version':BUNDLE,'description':description,'download_url':url,'page_url':PAGE,'signature_download_url':''}
    log=f'# {BUNDLE} ({created[:10]})\n\n* **ULike:** v{VERSION}：{SUMMARY}\n\n'+NOTES.strip()+'\n\n'+state['log']
    require(versions(BUNDLE)>versions(PREVIOUS),'Bundle version did not advance')
    require(eligible(log,PREVIOUS,'ULike'),'Missing ULike update scope')
    require(not eligible(log,BUNDLE,'ULike'),'Already repatched ULike remains outdated')
    for app in ('Berry Browser','SwiftKey Beta','Instagram','X','Trip.com'):
        require(not eligible(log,PREVIOUS,app),'False other-app update: '+app)
    require(re.findall(r'(?m)^ULike：v([^\r\n]+)（5\.6\.2／740）\r?$',description)==[VERSION],'Wrong active inventory')
    return data,log

def verify_assets(release_id,expected,public):
    release=api(f'repos/{REPO}/releases/{release_id}')
    require(release['draft'] is not public,'Wrong draft status')
    assets={a['name']:a for a in release['assets']};require(set(assets)==set(expected),'Release assets differ')
    for name,row in expected.items():
        asset=assets[name];require(asset['size']==row['bytes'],'Wrong asset size')
        if asset.get('digest'): require(asset['digest']=='sha256:'+row['sha256'],'Asset digest mismatch')
        if public: raw=fetch(asset['browser_download_url'])
        else: raw=subprocess.check_output(['gh','api',f'repos/{REPO}/releases/assets/{asset["id"]}','-H','Accept: application/octet-stream'])
        require(len(raw)==row['bytes'] and sha(raw)==row['sha256'],'Published bytes mismatch: '+name)

def publish():
    dist=Path('dist');q=json.loads((dist/'QA_ULike_rollback_v1.0.142.json').read_text())
    require(q['result']=='PASS_EXACT_UPLOADED188_REPLACEMENT_OTHER_APPS_PRESERVED','No passing merge QA')
    for name,row in q['artifacts'].items():
        raw=(dist/name).read_bytes();require(len(raw)==row['bytes'] and sha(raw)==row['sha256'],'QA/file mismatch')
    states={b:snapshot(b) for b in ('main','dev')};check_heads(states)
    require(all(s['manifest']['version']==PREVIOUS for s in states.values()),'Stale baseline')
    for s in states.values(): require(sha(fetch(s['manifest']['download_url']))==PINS[BASE],'Active baseline bytes differ')
    created=datetime.datetime.now(datetime.timezone.utc).replace(tzinfo=None,microsecond=0).isoformat()
    for s in states.values(): metadata(s,'https://example.invalid/preflight-only',created)
    require(api(f'repos/{REPO}/releases/tags/{TAG}',absent=True) is None,'Rollback release already exists; inspect before retry')
    require(api(f'repos/{REPO}/git/ref/heads/{RAW_BRANCH}',absent=True) is None,'Rollback branch already exists; inspect before retry')
    bad=api(f'repos/{REPO}/releases/tags/{BAD_TAG}')
    bad_assets={a['name']:a for a in bad['assets']}
    rejected={BASE:PINS[BASE],'ULike_HQ_Texture_Online_v1.9.17.mpp':'6d64053094a4035f55c1bea3d66e369f7bbe92771138ace4b37c25a96c403f16'}
    for name,digest in rejected.items(): require(bad_assets[name]['digest']=='sha256:'+digest,'Rejected asset identity differs')
    files=[dist/OUTPUT,dist/DONOR,dist/'QA_ULike_rollback_v1.0.142.json',dist/'ULike_rollback_v1.0.142_sources_and_QA.zip']
    expected={p.name:{'bytes':p.stat().st_size,'sha256':sha(p.read_bytes())} for p in files}
    receipt={'bundle_version':BUNDLE,'ulike_version':VERSION,'attachment_sha256':PINS[DONOR],'operations':[],'assets':expected}
    receiptfile=dist/'publication_ULike_rollback_v1.0.142.json'
    require(not receiptfile.exists(),'Prior publication receipt exists; inspect before retry')
    def checkpoint(status,**changes):
        receipt.update(status=status,**changes);receiptfile.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n');print(status,flush=True)
    checkpoint('preflight_passed')
    try:
        check_heads(states)
        release=api(f'repos/{REPO}/releases',{'tag_name':TAG,'target_commitish':os.environ['GITHUB_SHA'],'name':f'Hiro Morphe v{BUNDLE} / ULike v{VERSION} 差し替え版','body':NOTES,'draft':True,'prerelease':True,'make_latest':'false'})
        checkpoint('draft_created',release_id=release['id'])
        for f in files: run(['gh','release','upload',TAG,'--repo',REPO,f])
        verify_assets(release['id'],expected,False);checkpoint('draft_bytes_verified');check_heads(states)
        api(f'repos/{REPO}/releases/{release["id"]}',{'draft':False,'prerelease':True,'make_latest':'false'},'PATCH')
        verify_assets(release['id'],expected,True);checkpoint('public_release_bytes_verified');check_heads(states)
        api(f'repos/{REPO}/git/refs',{'ref':'refs/heads/'+RAW_BRANCH,'sha':states['main']['head']})
        upload={**states['main'],'branch':RAW_BRANCH}
        upload=commit_files(upload,{'downloads/'+OUTPUT:(dist/OUTPUT).read_bytes()},'release: exact ULike188 rollback bundle142')
        url=f'https://raw.githubusercontent.com/{REPO}/{upload["head"]}/downloads/{OUTPUT}'
        require(fetch(url)==(dist/OUTPUT).read_bytes(),'Raw published bundle mismatch')
        checkpoint('immutable_download_verified',raw_commit=upload['head'],download_url=url);check_heads(states)
        policy={'ulike_version':VERSION,'bundle_version':BUNDLE,'source_filename':DONOR,'source_sha256':PINS[DONOR],
            'source_release':'ulike-v1.8.8','active_release':TAG,'withdrawn_versions':[PREVIOUS_APP],
            'reason':'User requested exact uploaded v1.8.8 and rejection of latest buggy v1.9.17.',
            'future_ulike_base':'Use the exact v1.8.8 donor; do not automatically restore rejected v1.9.17.'}
        for branch,state in states.items():
            data,log=metadata(state,url,created)
            new=commit_files(state,{'patches-bundle.json':(json.dumps(data,ensure_ascii=False,indent=2)+'\n').encode(),
                 'CHANGELOG.md':log.encode(),'releases/ULike_ACTIVE.json':(json.dumps(policy,ensure_ascii=False,indent=2)+'\n').encode()},
                 'release: bundle142 exact ULike188 rollback and Manager update')
            require(json.loads(content('patches-bundle.json',new['head']))==data,'Remote manifest differs')
            require(content('CHANGELOG.md',new['head']).decode()==log,'Remote changelog differs')
            require(fetch(data['download_url'])==(dist/OUTPUT).read_bytes(),'Manager download differs')
            receipt['operations'].append({'branch':branch,'commit':new['head'],'manifest_verified':True,'download_verified':True})
            checkpoint('manager_'+branch+'_verified')
        checkpoint('new_distribution_verified',release_url=PAGE,manager_ulike_update_eligible=True,other_apps_false_updates=False)
        current=api(f'repos/{REPO}/releases/{bad["id"]}')
        require(current['tag_name']==BAD_TAG,'Wrong release selected for withdrawal')
        warning=f'【撤回済み・配信停止】ユーザーから不具合報告があり、ULike v1.9.17の採用を取り消しました。現在の指定版はv1.8.8、統合版はv1.0.142です。{PAGE}\n\n過去Git履歴は保持します。このリリースのMPP配布物は削除対象です。\n\n'
        api(f'repos/{REPO}/releases/{bad["id"]}',{'name':'撤回済み：ULike v1.9.17 / Hiro Morphe v1.0.141','body':warning+bad['body'],'prerelease':True,'make_latest':'false'},'PATCH')
        for name in rejected:
            api(f'repos/{REPO}/releases/assets/{bad_assets[name]["id"]}',method='DELETE')
            receipt['operations'].append({'deleted_bad_asset':name,'asset_id':bad_assets[name]['id']});checkpoint('rejected_asset_removed')
        after=api(f'repos/{REPO}/releases/{bad["id"]}')
        require(not (set(rejected)&{a['name'] for a in after['assets']}),'Rejected MPP still published as release asset')
        for branch in states:
            active=snapshot(branch);require(active['manifest']['version']==BUNDLE,'Active version changed unexpectedly')
            require(active['manifest']['download_url']==url,'Active download changed unexpectedly')
        checkpoint('published_verified_and_bad_mpp_withdrawn',withdrawn_release=BAD_TAG,
                   deleted_bad_mpp_assets=list(rejected),git_history_force_rewritten=False,
                   fixed_chatgpt_site_updated=False,android_device_tested=False)
    except BaseException as error:
        checkpoint('failed_requires_inspection',error=type(error).__name__+': '+str(error));raise

if __name__=='__main__':
    require(len(sys.argv)==2 and sys.argv[1] in ('build','publish'),'Use build or publish')
    globals()[sys.argv[1]]()
