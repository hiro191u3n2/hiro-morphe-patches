#!/usr/bin/env python3
"""Publish only the reviewed signed APK after an independent source rebuild."""
import base64, hashlib, io, json, lzma, os, pathlib, subprocess, sys, tarfile, urllib.request, urllib.error, zipfile

ROOT = pathlib.Path.cwd()
STAGE = ROOT / 'stage_easyloupe141'
TOOLS = ROOT / 'easyloupe141_tools'
PREFIX = ROOT / 'releases/easyloupe141/transport'
EXPECTED_TRANSPORT = 'baea5bb16a622b9755682c7fd2b1e48891ea9b05328ea6daee987e88510b5914'
EXPECTED_APK = '8d47819d9fdb61f534fff5125ff4958968f4e3dbaa983eae32841989cd22d003'
EXPECTED_SOURCE = '109d4b17124a68f9ca42560933b9188962801e3b2457f86524da8300b3009041'
CERT = '085db422f682f6e00b701d0969297b21dff919e24a4b14430ab27c755ba71873'
REPO = 'hiro191u3n2/hiro-morphe-patches'
TAG = 'easyloupe-v1.4.1'

def sha(data): return hashlib.sha256(data).hexdigest()
def run(*args): subprocess.run([str(a) for a in args], check=True)
def payload(path):
    with zipfile.ZipFile(path) as z:
        assert z.testzip() is None
        return {n:sha(z.read(n)) for n in z.namelist() if not n.startswith('META-INF/')}

def prepare():
    encoded = ''.join((PREFIX / ('part%d.b64' % n)).read_text().strip() for n in range(5))
    assert len(encoded) == 70384, len(encoded)
    compressed = base64.b64decode(encoded, validate=True)
    assert sha(compressed) == EXPECTED_TRANSPORT
    STAGE.mkdir(exist_ok=True)
    data = lzma.decompress(compressed)
    assert len(data) < 2_000_000
    with tarfile.open(fileobj=io.BytesIO(data), mode='r:') as tar:
        for member in tar.getmembers():
            name = pathlib.PurePosixPath(member.name)
            assert member.isfile() and not name.is_absolute() and '..' not in name.parts
            assert name.parts[0] == 'EasyLoupe141' or str(name) in ('EasyLoupe_v1.4.1.apk','reviewed_manifest.json')
            assert member.size < 500_000
            target = STAGE / str(name)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(tar.extractfile(member).read())
    manifest = json.loads((STAGE/'reviewed_manifest.json').read_text())
    assert manifest['schema'] == 'easyloupe141-public-source-and-signed-apk-v1'
    source = STAGE/'EasyLoupe141'
    for name, expected in manifest['source_files'].items():
        path=pathlib.PurePosixPath(name)
        assert not path.is_absolute() and '..' not in path.parts
        assert sha((source/name).read_bytes()) == expected, name
        assert path.suffix not in ('.p12','.jks','.keystore','.pem')
        assert 'signing' not in path.parts and path.name != 'password.txt'
    archive = STAGE/'EasyLoupe_source_v1.4.1.zip'
    # Fixed ZIP metadata and STORED entries make the published source bytes deterministic.
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_STORED) as z:
        for name in sorted(manifest['source_files']):
            info=zipfile.ZipInfo('EasyLoupe141/'+name,(2026,10,6,0,0,0))
            info.compress_type=zipfile.ZIP_STORED
            z.writestr(info,(source/name).read_bytes())
    assert sha(archive.read_bytes()) == EXPECTED_SOURCE
    assert sha((STAGE/'EasyLoupe_v1.4.1.apk').read_bytes()) == EXPECTED_APK
    print('PASS reviewed transport, 38 source files, signed APK and source archive hashes')

def verify():
    source=STAGE/'EasyLoupe141'
    apk=STAGE/'EasyLoupe_v1.4.1.apk'
    run(sys.executable,source/'tests/run.py')
    run(sys.executable,source/'qa_lifecycle/run_tests.py')
    rebuild=STAGE/'rebuilt-unsigned.apk'
    run(sys.executable,source/'build.py','--baseline',source/'baseline/EasyLoupe_v1.4.0.apk','--tools',TOOLS,'--work',STAGE/'rebuild','--output',rebuild)
    assert payload(apk) == payload(rebuild), 'Independent rebuild differs from signed release'
    signatures=[]
    for path in (source/'baseline/EasyLoupe_v1.4.0.apk', apk):
        result=subprocess.check_output(['java','-jar',str(TOOLS/'apksigner.jar'),'verify','--verbose','--print-certs',str(path)],text=True)
        assert 'Signer #1 certificate SHA-256 digest: '+CERT in result
        signatures.append(result)
    run(TOOLS/'zipalign','-c','-P','16','4',apk)
    badging=subprocess.check_output([str(TOOLS/'aapt2'),'dump','badging',str(apk)],text=True)
    assert "name='jp.hiro.easyloupe'" in badging and "versionCode='6'" in badging and "versionName='1.4.1'" in badging
    classes=STAGE/'verifier';classes.mkdir(exist_ok=True)
    run('javac','-cp',TOOLS/'morphe.jar','-d',classes,source/'VerifyDex.java')
    dex=subprocess.check_output(['java','-cp',str(classes)+os.pathsep+str(TOOLS/'morphe.jar'),'VerifyDex',str(apk),str(TOOLS/'android.jar')],text=True)
    assert '0 failures' in dex
    print(dex)
    report={'version':'1.4.1','versionCode':6,'package':'jp.hiro.easyloupe','apk_sha256':EXPECTED_APK,'source_sha256':EXPECTED_SOURCE,'same_signer_as_v140':True,'certificate_sha256':CERT,'independent_source_rebuild_payloads_equal':True,'apk_alignment_verified':True,'dex_analysis':dex.strip(),'startup_host_tests':json.loads((source/'tests/report.json').read_text()),'on_demand_lifecycle_host_tests':json.loads((source/'qa_lifecycle/report.json').read_text()),'galaxy_device_tested':False,'tabelog_device_tested':False,'workflow_run':os.environ.get('GITHUB_RUN_ID'),'note':'Host tests and DEX verification do not establish Galaxy or app-specific runtime latency.'}
    (STAGE/'EasyLoupe_v1.4.1_QA.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    names=['EasyLoupe_v1.4.1.apk','EasyLoupe_source_v1.4.1.zip','EasyLoupe_v1.4.1_QA.json']
    (STAGE/'EasyLoupe_v1.4.1_SHA256SUMS.txt').write_text(''.join(sha((STAGE/n).read_bytes())+'  '+n+'\n' for n in names))
    print('PASS independent source rebuild, signature continuity, alignment, manifest, DEX and host tests')

def api(method,path,data=None,binary=False):
    url=path if path.startswith('https://') else 'https://api.github.com/repos/'+REPO+path
    headers={'Authorization':'Bearer '+os.environ['GH_TOKEN'],'Accept':'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28','User-Agent':'EasyLoupe141-verified-publisher'}
    if data is not None:
        if binary: headers['Content-Type']='application/octet-stream'
        else: data=json.dumps(data).encode();headers['Content-Type']='application/json'
    with urllib.request.urlopen(urllib.request.Request(url,data=data,headers=headers,method=method),timeout=90) as response:
        return json.loads(response.read())

def put(path,data):
    change={'message':'release: publish verified EasyLoupe 1.4.1 standalone APK','content':base64.b64encode(data).decode(),'branch':'main'}
    try: change['sha']=api('GET','/contents/'+path+'?ref=main')['sha']
    except urllib.error.HTTPError as e:
        if e.code != 404: raise
    return api('PUT','/contents/'+path,change)

def publish():
    # No publication is permitted until this exact run has completed verification.
    report=json.loads((STAGE/'EasyLoupe_v1.4.1_QA.json').read_text())
    assert report['apk_sha256']==EXPECTED_APK and report['workflow_run']==os.environ['GITHUB_RUN_ID']
    assert report['independent_source_rebuild_payloads_equal'] and report['same_signer_as_v140']
    assert report['startup_host_tests']['passed'] and report['on_demand_lifecycle_host_tests']['passed']==13
    assert sha((STAGE/'EasyLoupe_v1.4.1.apk').read_bytes())==EXPECTED_APK
    body='''かんたん拡大 1.4.1（versionCode 6）

起動直後に一時的な画面取得失敗を「この画面は拡大できません」と表示していた処理を修正しました。

- 対象判定から他アプリのUIツリー取得を除去し、ウィンドウの位置・種類・重なり順だけで選択。
- 起動準備中、無効なウィンドウID、一時的な取得失敗は短い間隔で再試行。OSの撮影間隔制限は維持。
- 本当に保護された画面と権限不足は個別に案内。保護の回避は行いません。
- 古い取得結果の破棄を強化。倍率、移動、×ボタン、非常駐終了の既存コードは維持。

旧1.4.0と署名証明書の一致を確認。独立したソース再ビルドと署名APKの内容一致、DEX解析、署名・整列、回復処理と既存非常駐処理のホストテストを確認しました。

Galaxy実機および食べログアプリでの再現テスト・表示時間の実測は未実施です。

今回は独立アプリのAPK更新です。Hiro Morphe Patchesの総合パッチ、既存更新フィード、リポジトリのLatest指定は変更していません。公開ソースに署名秘密鍵は含みません。
'''
    try: release=api('GET','/releases/tags/'+TAG)
    except urllib.error.HTTPError as e:
        if e.code!=404:raise
        release=api('POST','/releases',{'tag_name':TAG,'target_commitish':os.environ['GITHUB_SHA'],'name':'かんたん拡大 1.4.1 — 起動時の画面取得を修正','body':body,'draft':False,'prerelease':False,'make_latest':'false'})
    existing={a['name']:a for a in api('GET','/releases/'+str(release['id'])+'/assets')}
    names=['EasyLoupe_v1.4.1.apk','EasyLoupe_source_v1.4.1.zip','EasyLoupe_v1.4.1_QA.json','EasyLoupe_v1.4.1_SHA256SUMS.txt']
    uploaded=[]
    for name in names:
        data=(STAGE/name).read_bytes()
        if name in existing:
            asset=existing[name]
            assert asset.get('digest')=='sha256:'+sha(data), 'Existing release asset differs; refuse overwrite'
        else:
            asset=api('POST',release['upload_url'].split('{')[0]+'?name='+name,data,True)
        assert asset['size']==len(data)
        if asset.get('digest'):assert asset['digest']=='sha256:'+sha(data)
        uploaded.append(asset)
    put('downloads/EasyLoupe_latest.apk',(STAGE/'EasyLoupe_v1.4.1.apk').read_bytes())
    latest={'name':'かんたん拡大','package':'jp.hiro.easyloupe','version':'1.4.1','versionCode':6,'sha256':EXPECTED_APK,'apk':'https://raw.githubusercontent.com/'+REPO+'/main/downloads/EasyLoupe_latest.apk','release':release['html_url'],'signatureCertificateSha256':CERT,'deviceTested':False}
    put('downloads/easyloupe-latest.json',(json.dumps(latest,ensure_ascii=False,indent=2)+'\n').encode())
    check=api('GET','/contents/downloads/EasyLoupe_latest.apk?ref=main')
    assert sha(base64.b64decode(check['content']))==EXPECTED_APK
    (STAGE/'publication.json').write_text(json.dumps({'release':release['html_url'],'tag':TAG,'assets':[{'name':a['name'],'url':a['browser_download_url'],'size':a['size'],'digest':a.get('digest')} for a in uploaded],'fixed_apk_sha256_verified':True,'existing_morphe_feeds_untouched':True},ensure_ascii=False,indent=2)+'\n')
    print('PUBLISHED',release['html_url'])

if __name__=='__main__':
    os.environ['LD_LIBRARY_PATH']=str(TOOLS)+os.pathsep+os.environ.get('LD_LIBRARY_PATH','')
    {'prepare':prepare,'verify':verify,'publish':publish}[sys.argv[1]]()
