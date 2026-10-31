#!/usr/bin/env python3
"""Scratch-only, exact Manager237 host checks; not a physical-device test."""
from pathlib import Path
import argparse, hashlib, json, os, subprocess, zipfile

ROOT=Path(__file__).resolve().parent
WORKSPACE=ROOT.parent
JAVA=WORKSPACE/'jdk134/jdk-21.0.8+9/bin/java'
HOST=WORKSPACE/'tools134/tools/manager237-host.jar'
MANAGER=WORKSPACE/'tools134/tools/manager237.apk'
APK=WORKSPACE/'inputs134/instagram452-original.apk'
PIKO=ROOT/'piko-3.11.0-dev.3.mpp'
HELPER=WORKSPACE/'activation135/helper'
NAME='戻る1回で終了・履歴削除'

def identity(path):
    with path.open('rb') as f:return {'bytes':path.stat().st_size,'sha256':hashlib.file_digest(f,'sha256').hexdigest()}

def pin(path,size,sha):
    assert identity(path)=={'bytes':size,'sha256':sha},str(path)+' identity changed'

def json_file(path,value):path.write_text(json.dumps(value,ensure_ascii=False,indent=2,sort_keys=True)+'\n')

def main():
    p=argparse.ArgumentParser();p.add_argument('--hiro',type=Path,required=True);p.add_argument('--order',choices=['piko-first','hiro-first'],required=True);p.add_argument('--label',required=True)
    a=p.parse_args();a.hiro=a.hiro.resolve();folder=ROOT/a.label
    assert folder.parent==ROOT and not folder.exists(),'Fresh direct-child work directory required'
    pin(PIKO,8529170,'a7658d042676d750ff68b782ff02e65d077c011d6a6f09031c05b39e80fcc7a3')
    pin(APK,143237227,'22f20062d16997e26009e73b0425974efef8ac69c76dd8912ee8a091213c39e2')
    pin(HOST,19926006,'4ffa8ccabcc475a18781fcc8642c721eed35ede7d43f967c4f0bd0f51a964a29')
    pin(MANAGER,41222610,'3df9064c5648e0ea0a2d5df4394616bc2320c7bd950ab530dde3d93be6906300')
    folder.mkdir()
    h=subprocess.run([str(JAVA),'-Xmx512m','-cp',os.pathsep.join(map(str,[HELPER,HOST])),'Manager237Selections',str(a.hiro),'com.instagram.android','452.1.0.53.64'],capture_output=True,text=True,check=True)
    (folder/'hiro-selections.json').write_text(h.stdout)
    hiro=json.loads(h.stdout);piko=json.loads((ROOT/'piko-selections.json').read_text())
    prows=[r for r in piko['patches'] if r['default'] and r['version_compatible']]
    hrows=[r for r in hiro['patches'] if r['default'] and r['version_compatible']]
    assert len(piko['patches'])==68 and len(prows)==64
    assert [r['name'] for r in hrows]==[NAME]
    sources=[(PIKO,prows),(a.hiro,hrows)]
    if a.order=='hiro-first':sources.reverse()
    selected=[r['name'] for _,rows in sources for r in rows];assert len(selected)==len(set(selected))==65
    request={'packageName':'com.instagram.android','inputFile':str(APK),'outputFile':str(folder/'patched.apk'),
      'mergedApkFile':str(folder/'merged.apk'),'temporaryDir':str(folder/'temp'),'workspaceDir':str(folder/'workspace'),
      'bundles':[{'uid':i+1,'patchesFile':str(f),'patches':[r['key'] for r in rows],'options':{}} for i,(f,rows) in enumerate(sources)],
      'stripUnusedNativeLibs':False,'device':{'supportedAbis':['arm64-v8a'],'localeTags':['ja-JP'],'densityDpi':480}}
    json_file(folder/'request.json',request)
    (folder/'commands.jsonl').write_text(json.dumps({'type':'run','requestFile':str(folder/'request.json')})+'\n')
    with (folder/'process.log').open('w') as log:
        run=subprocess.run([str(JAVA),'-Xmx3g','-cp',os.pathsep.join(map(str,[HOST,MANAGER])),
           'app.morphe.manager.patcher.host.HostMainKt',str(folder/'commands.jsonl'),str(folder/'events.jsonl')],stdout=log,stderr=subprocess.STDOUT)
    events=[json.loads(l) for l in (folder/'events.jsonl').read_text().splitlines() if l.strip()]
    done=[e for e in events if e.get('type')=='finished'];failures=[e for e in events if e.get('type')=='patch_failed']
    applied=[e['name'] for e in events if e.get('type')=='patch'];counts=[e['count'] for e in events if e.get('type')=='patches_done']
    output=folder/'patched.apk'
    ok=run.returncode==0 and len(done)==1 and events[-1]==done[0] and done[0].get('failure') is None and not failures and counts==[65] and len(applied)==65 and set(applied)==set(selected) and output.is_file() and zipfile.is_zipfile(output)
    proof={'success':ok,'physical_android_tested':False,'order':a.order,'selected_count':65,'requested_patch_names':selected,
      'selected_bundles':[{'artifact':f.name,**identity(f),'selected_keys':[r['key'] for r in rows],
        'option_overrides':{},'option_defaults':{r['key']:r['option_defaults'] for r in rows}} for f,rows in sources],
      'applied_patch_names':applied,'patch_failures':failures,'finished':done,'patches_done_counts':counts,
      'input':identity(APK),'host':identity(HOST),'manager':identity(MANAGER),'request':identity(folder/'request.json'),
      'events':identity(folder/'events.jsonl'),'output':identity(output) if output.is_file() else None}
    json_file(folder/'proof.json',proof)
    print(json.dumps({'success':ok,'folder':str(folder),'selected_count':65,'patches_done_counts':counts,'output':proof['output']},ensure_ascii=False),flush=True)
    if not ok:raise SystemExit('Host application failed; inspect recorded events/proof.')

if __name__=='__main__':main()
