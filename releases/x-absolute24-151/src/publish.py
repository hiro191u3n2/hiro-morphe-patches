#!/usr/bin/env python3
"""Publish reviewed X timestamp MPPs, then atomically advance main/dev feeds."""
import argparse,base64,concurrent.futures,datetime,hashlib,json,pathlib,shutil,subprocess,time,urllib.request,zipfile
P=pathlib.Path
REPO='hiro191u3n2/hiro-morphe-patches';VERSION='1.0.151';TAG='x-absolute24-v1.0.0';NAME='Hiro_Morphe_Patches_v1.0.151.mpp'
ROOT=P.cwd();SOURCE=P(__file__).resolve().parent

def run(*args,input=None):
    return subprocess.run(list(map(str,args)),input=input,text=True,check=True,stdout=subprocess.PIPE).stdout.strip()
def api(path,method='GET',payload=None):
    args=['gh','api','--method',method,path]
    if payload is not None:args+=['--input','-']
    return json.loads(run(*args,input=json.dumps(payload) if payload is not None else None))
def current(branch):
    item=api(f'repos/{REPO}/contents/patches-bundle.json?ref={branch}')
    return item,json.loads(base64.b64decode(item['content']))
def get(url,headers=None,method='GET'):
    for attempt in range(5):
        try:
            req=urllib.request.Request(url,headers={'User-Agent':'Hiro-X-Absolute24-Release','Accept-Encoding':'identity',**(headers or {})},method=method)
            with urllib.request.urlopen(req,timeout=60) as r:return r.status,dict(r.headers.items()),r.read()
        except OSError:
            if attempt==4:raise
            time.sleep(2**attempt)
def verify_delivery(url,data):
    status,_,downloaded=get(url);assert status==200 and downloaded==data,'Full public download mismatch'
    status,headers,_=get(url,method='HEAD');assert status==200,'HEAD failed'
    if 'Content-Length' in headers:assert int(headers['Content-Length'])==len(data)
    size=len(data)
    def part(bounds):
        a,b=bounds;s,h,d=get(url,{'Range':f'bytes={a}-{b}'})
        cr=next((v for k,v in h.items() if k.lower()=='content-range'),None)
        assert s==206 and cr==f'bytes {a}-{b}/{size}' and d==data[a:b+1],'Range mismatch'
        return d
    assert part((0,0))==data[:1]
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        assert b''.join(pool.map(part,[(i*size//4,(i+1)*size//4-1) for i in range(4)]))==data
    return {'url':url,'full_download_sha256':hashlib.sha256(downloaded).hexdigest(),'head_status':200,'four_parallel_ranges_verified':True,'device_network_tested':False}
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--dist',required=True);a=ap.parse_args();dist=P(a.dist).resolve()
    expected=json.loads((SOURCE/'expected-mpp.json').read_text())
    for name,row in expected.items():
        data=(dist/name).read_bytes();assert len(data)==row['bytes'] and hashlib.sha256(data).hexdigest()==row['sha256'],'Unreviewed CI output'
    _,main_old=current('main');_,dev_old=current('dev')
    assert main_old['version']==dev_old['version']=='1.0.150','Feed advanced concurrently; refuse to overwrite'
    # Refresh the checkout without rewriting any remote history.
    run('gh','auth','setup-git');run('git','fetch','origin','main','dev')
    assert run('git','rev-parse','HEAD')==run('git','rev-parse','origin/main'),'Main changed during build; retry after reconciliation'
    run('git','config','user.name','github-actions[bot]');run('git','config','user.email','41898282+github-actions[bot]@users.noreply.github.com')
    target=ROOT/'releases/x-absolute24-151/src';target.mkdir(parents=True,exist_ok=True)
    for f in SOURCE.rglob('*'):
        if f.is_file() and '__pycache__' not in f.parts:
            dest=target/f.relative_to(SOURCE);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(f,dest)
    (ROOT/'downloads').mkdir(exist_ok=True)
    for name in expected:shutil.copyfile(dist/name,ROOT/'downloads'/name)
    reportfolder=ROOT/'releases/x-absolute24-151'
    for name in ['build-evidence.json','local-qa.json','SHA256SUMS.txt']:shutil.copyfile(dist/name,reportfolder/name)
    shutil.copyfile(SOURCE/'RELEASE.md',reportfolder/'RELEASE.md')
    run('git','add','downloads/'+NAME,'downloads/X_Absolute_Time_24h_v1.0.0.mpp','releases/x-absolute24-151')
    run('git','commit','-m','release: publish verified X absolute 24h timestamps and bundle1.0.151 assets')
    run('git','push','origin','HEAD:main');binary_commit=run('git','rev-parse','HEAD')
    raw=f'https://raw.githubusercontent.com/{REPO}/{binary_commit}/downloads/{NAME}'
    delivery=verify_delivery(raw,(dist/NAME).read_bytes())
    # Keep a readable source archive alongside the binary and test evidence.
    with zipfile.ZipFile(dist/'X_Absolute_Time_24h_v1.0.0_source.zip','w',zipfile.ZIP_DEFLATED) as z:
        for f in sorted(target.rglob('*')):
            if f.is_file():z.write(f,f.relative_to(target))
    files=[str(dist/n) for n in expected]+[str(dist/n) for n in ['build-evidence.json','local-qa.json','SHA256SUMS.txt','X_Absolute_Time_24h_v1.0.0_source.zip']]
    run('gh','release','create',TAG,*files,'--repo',REPO,'--target',binary_commit,'--title','Hiro Morphe Patches v1.0.151 / X Absolute Time 24h v1.0.0','--notes-file',SOURCE/'RELEASE.md','--latest')
    _,main_check=current('main');_,dev_check=current('dev')
    assert main_check==main_old and dev_check==dev_old,'Source changed before feed update; refusing overwrite'
    now=datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%S')
    metadata=dict(main_old);metadata.update(version=VERSION,download_url=raw,created_at=now,page_url=f'https://github.com/{REPO}/releases/tag/{TAG}',description=(SOURCE/'RELEASE.md').read_text()+'\n\n'+main_old.get('description',''))
    encoded=json.dumps(metadata,ensure_ascii=False,indent=2)+'\n'
    (ROOT/'patches-bundle.json').write_text(encoded)
    changelog=ROOT/'CHANGELOG.md';changelog.write_text('# 1.0.151 (2026-10-06)\n\n'+(SOURCE/'RELEASE.md').read_text()+'\n\n'+changelog.read_text())
    run('git','add','patches-bundle.json','CHANGELOG.md');run('git','commit','-m','release: announce X24h bundle1.0.151 after public download verification')
    devwork=ROOT.parent/'x24-feed-dev'
    run('git','worktree','add','--detach',devwork,'origin/dev')
    (devwork/'patches-bundle.json').write_text(encoded)
    run('git','-C',devwork,'add','patches-bundle.json');run('git','-C',devwork,'commit','-m','release: sync verified X24h bundle1.0.151 Manager source')
    devsha=run('git','-C',devwork,'rev-parse','HEAD')
    run('git','push','--atomic','origin','HEAD:main',devsha+':dev')
    verified={}
    for branch in ['main','dev']:
        item,feed=current(branch);assert feed['version']==VERSION and feed['download_url']==raw
        status,_,body=get(f'https://raw.githubusercontent.com/{REPO}/{branch}/patches-bundle.json?verified={binary_commit}')
        public=json.loads(body);assert status==200 and public['version']==VERSION and public['download_url']==raw
        verified[branch]={'version':feed['version'],'feed_blob_sha':item['sha'],'public_feed_verified':True}
    report={'version':VERSION,'release_tag':TAG,'binary_commit':binary_commit,'delivery':delivery,'manager_feeds':verified,'device_tested':False,'old_chatgpt_site_modified':False}
    (dist/'publication-evidence.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    run('gh','release','upload',TAG,str(dist/'publication-evidence.json'),'--repo',REPO)
    print(json.dumps(report,ensure_ascii=False,indent=2),flush=True);print('PUBLISHED_MAIN_AND_DEV',VERSION,flush=True)
if __name__=='__main__':main()
