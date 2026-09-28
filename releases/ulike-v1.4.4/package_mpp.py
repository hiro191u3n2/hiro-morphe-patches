#!/usr/bin/env python3
"""Deterministically package the ULike network fix, preserving all other payloads."""
import argparse, hashlib, json, zipfile
from pathlib import Path
STAMP=(2026,9,28,5,8,0)
DESCRIPTION='フィルター等の素材通信を妨げていたINTERNET権限の削除を廃止。既存の解析・プッシュ停止、24.5MP・HEIF専用保存・3:4表示修正は維持。通信先の許可リストではありません。'
def manifest(name,version):
    s=f'''Manifest-Version: 1.0
Name: {name}
Description: {DESCRIPTION}
Version: {version}
Timestamp: 2026-09-28T14:08:00
Source: remote
Author: ひろ
Contact: na
Website: na
License: GPLv3
Patcher-Version: 1.8.0

'''
    lines=[]
    for line in s.splitlines():
        b=line.encode('utf-8'); prefix=b''
        while len(prefix)+len(b)>72:
            n=72-len(prefix)
            while n and b[n]&0xc0==0x80:n-=1
            lines.append(prefix+b[:n]);b=b[n:];prefix=b' '
        lines.append(prefix+b)
    return b'\r\n'.join(lines)+b'\r\n'
def package(old,out,build,integrated):
    expected='daf77cd1b88e30f03a315016da0c441d493fdf3436ee70df310be380f74bc3f7' if integrated else '1eea22b0f8f75b3c1d7e1f98e32e96643c69133276100b2f821e5b8945d44c2e'
    if hashlib.sha256(old.read_bytes()).hexdigest()!=expected:raise ValueError('Input must match the published v1.4.3/v1.0.80 release')
    version='1.0.81' if integrated else '1.4.4'
    name='ひろ Morphe 統合パッチ' if integrated else 'ひろ ULike 高画質・質感美肌・素材通信復旧'
    changed=[]
    with zipfile.ZipFile(old) as z,zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as w:
        if len(z.namelist())!=len(set(z.namelist())):raise ValueError('Duplicate archive entry')
        for entry in z.namelist():
            value=z.read(entry); previous=value
            if entry=='META-INF/MANIFEST.MF':value=manifest(name,version)
            elif entry=='classes.dex':value=(build/('integrated-new.dex' if integrated else 'dex/classes.dex')).read_bytes()
            elif entry=='app/hiro/ulike/patches/UlikeHqMaxPatch.class':value=(build/'patch'/entry).read_bytes()
            if value!=previous:changed.append(entry)
            zi=zipfile.ZipInfo(entry,STAMP);zi.compress_type=zipfile.ZIP_DEFLATED
            w.writestr(zi,value)
    expected_changes={'META-INF/MANIFEST.MF','classes.dex'}
    if not integrated:expected_changes.add('app/hiro/ulike/patches/UlikeHqMaxPatch.class')
    if set(changed)!=expected_changes:raise ValueError(changed)
    return {'file':out.name,'bytes':out.stat().st_size,'sha256':hashlib.sha256(out.read_bytes()).hexdigest(),'changed_entries':changed}
def main():
    p=argparse.ArgumentParser();p.add_argument('--old-standalone',type=Path,required=True);p.add_argument('--old-integrated',type=Path,required=True);p.add_argument('--build',type=Path,default=Path('build'));p.add_argument('--output',type=Path,default=Path('dist'));a=p.parse_args()
    a.output.mkdir(parents=True,exist_ok=True)
    results=[package(a.old_standalone,a.output/'ULike_HQ_Texture_Online_v1.4.4.mpp',a.build,False),package(a.old_integrated,a.output/'Hiro_Morphe_Patches_v1.0.81.mpp',a.build,True)]
    (a.output/'build-artifacts.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(results,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
