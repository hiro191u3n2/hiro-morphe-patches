#!/usr/bin/env python3
"""Reproducible release archive; verify exact predecessor and retained resources."""
import hashlib,json,zipfile,argparse
from pathlib import Path
STAMP=(2026,9,30,0,0,0)
def manifest(integrated):
 name='ひろ Morphe 統合パッチ' if integrated else 'ひろ ULike 高画質・ノイズ低減強化'
 version='1.0.92' if integrated else '1.5.8'
 text=f'''Manifest-Version: 1.0
Name: {name}
Description: ノイズ低減と2段階くっきり補正を強化。最強・質感保護・白フチ抑制・暗部優先を追加。既存望遠切替等は保持。実機未検証。
Version: {version}
Timestamp: 2026-09-30T00:00:00
Source: remote
Author: ひろ
Contact: na
Website: na
License: GPLv3
Patcher-Version: 1.8.0

'''
 lines=[]
 for line in text.splitlines():
  b=line.encode(); prefix=b''
  while len(prefix)+len(b)>72:
   n=72-len(prefix)
   while n and b[n]&0xc0==0x80:n-=1
   lines.append(prefix+b[:n]);b=b[n:];prefix=b' '
  lines.append(prefix+b)
 return b'\r\n'.join(lines)+b'\r\n'
def package(old,out,build,integrated):
 expected='5615e67dfca1da9ccad4b6625f433588d005755e9aedca63bf6d3d205c26c46f' if integrated else '537f4aa69058d8f65481d9556e2cb36be76f42235e44acf45c39859f9b0e8a7a'
 assert hashlib.sha256(old.read_bytes()).hexdigest()==expected,'Unexpected predecessor'
 changes={}; retained=[]
 with zipfile.ZipFile(old) as z,zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as w:
  assert len(z.namelist())==len(set(z.namelist()))
  for entry in z.namelist():
   prev=data=z.read(entry)
   if entry=='META-INF/MANIFEST.MF':data=manifest(integrated)
   elif entry=='classes.dex':data=(build/('integrated-new.dex' if integrated else 'newdex/classes.dex')).read_bytes()
   elif entry=='app/hiro/ulike/patches/UlikeHqMaxPatch.class':data=(build/'newpatch'/entry).read_bytes()
   elif entry=='ulike/runtime.dex':data=(build/'runtime.dex').read_bytes()
   if prev!=data:changes[entry]=hashlib.sha256(data).hexdigest()
   else:retained.append(entry)
   zi=zipfile.ZipInfo(entry,STAMP);zi.compress_type=zipfile.ZIP_DEFLATED;w.writestr(zi,data)
 expected_changes={'META-INF/MANIFEST.MF','classes.dex','ulike/runtime.dex'}
 if not integrated:expected_changes.add('app/hiro/ulike/patches/UlikeHqMaxPatch.class')
 assert set(changes)==expected_changes,changes
 with zipfile.ZipFile(out) as z:assert z.testzip() is None
 return dict(file=out.name,bytes=out.stat().st_size,sha256=hashlib.sha256(out.read_bytes()).hexdigest(),changed_entries=changes,retained_entries=retained)
def main():
 p=argparse.ArgumentParser();p.add_argument('--old-standalone',type=Path,required=True);p.add_argument('--old-integrated',type=Path,required=True);p.add_argument('--build',type=Path,default=Path('build'));p.add_argument('--output',type=Path,default=Path('dist'));a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
 result=[package(a.old_standalone,a.output/'ULike_HQ_Texture_Online_v1.5.8.mpp',a.build,False),package(a.old_integrated,a.output/'Hiro_Morphe_Patches_v1.0.92.mpp',a.build,True)]
 (a.output/'build-artifacts.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps(result,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
