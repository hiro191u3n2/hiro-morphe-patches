#!/usr/bin/env python3
"""Deterministic MPP packaging with exact predecessor and unrelated-entry checks."""
import argparse,hashlib,json,zipfile
from datetime import datetime
from pathlib import Path

PINNED={
 'standalone':'cb9c34d360b5f84aa97ebfcf0a852af6136b68035848037cef6126e131f93c28',
 'integrated':'7f5d3baba1083ea1343e01ba7387216faf05d3c0e382e2435fccd0e939b2ce87',
}
CLASS='app/hiro/ulike/patches/UlikeHqMaxPatch.class'
HELPER='app/hiro/ulike/patches/NativeNv21EffectFlag.class'
CONTRACT='app/hiro/ulike/patches/MethodContract.class'
ORPHAN='app/morphe/patches/shared/misc/extension/SharedExtensionPatchKt.class'
ARCHIVED_ORPHAN='desktop-preserved/SharedExtensionPatchKt.class.bin'
ORPHAN_SHA='f538a94e25d3594cb9b2506d36a066e2a3f3297edcc7ecbf0670ab0c590b0bfe'
def sha(data):return hashlib.sha256(data).hexdigest()
def manifest_fields(data):
 rows=[]
 for raw in data.replace(b'\r\n',b'\n').split(b'\n'):
  if raw.startswith(b' '):
   if not rows:raise ValueError('Continuation without header')
   rows[-1]+=raw[1:]
  elif raw:rows.append(raw)
 out={}
 for raw in rows:
  k,v=raw.decode().split(': ',1)
  if k in out:raise ValueError('Duplicate manifest header '+k)
  out[k]=v
 return out
def encode_manifest(fields):
 lines=[]
 for k,v in fields.items():
  if any(c in v for c in '\r\n\0'):raise ValueError('Unsafe manifest value')
  b=(k+': '+v).encode();prefix=b''
  while len(prefix)+len(b)>72:
   n=72-len(prefix)
   while n and b[n]&0xc0==0x80:n-=1
   lines.append(prefix+b[:n]);b=b[n:];prefix=b' '
  lines.append(prefix+b)
 result=b'\r\n'.join(lines)+b'\r\n\r\n'
 assert manifest_fields(result)==fields and max(map(len,result.split(b'\r\n')))<=72
 return result
def package(old,out,build,release,kind):
 assert sha(old.read_bytes())==PINNED[kind],'Unexpected predecessor '+str(old)
 meta=release[kind];t=datetime.fromisoformat(release['timestamp']);stamp=(t.year,t.month,t.day,t.hour,t.minute,t.second)
 replacements={
  'classes.dex':build/('integrated-new.dex' if kind=='integrated' else 'newdex/classes.dex'),
  'ulike/methods.dex':build/'methods.dex','ulike/methods.tsv':build/'methods.tsv','ulike/runtime.dex':build/'runtime.dex',
  'ulike/assets.tsv':build/'assets.tsv',
  **{f'ulike/assets/{n:04d}.bin':build/f'assets/{n:04d}.bin' for n in range(3)},
 }
 # Both pinned 1.6.4 / 1.0.97 archives contain the validated dual loader layout.
 additions={};renames={}
 for path in sorted((build/'newpatch/app/hiro/ulike/patches').glob('IntegrationPayload169*.class')):
  additions[str(path.relative_to(build/'newpatch'))]=path
 assert set(additions)=={'app/hiro/ulike/patches/IntegrationPayload169'+s+'.class' for s in ('','$Resolver','$Payload')}
 manifest=(build/'ulike169/resourceitems.tsv').read_text().splitlines()
 assert len(manifest)==6
 for row in manifest:
  target,digest,length,resource=row.split('\t')
  path=build/resource
  assert sha(path.read_bytes())==digest and path.stat().st_size==int(length)
  additions[resource]=path
 additions['ulike169/resourceitems.tsv']=build/'ulike169/resourceitems.tsv' 
 replacements[CLASS]=build/'newpatch'/CLASS
 for n in [CONTRACT,HELPER]:replacements[n]=build/'newpatch'/n
 changed={};retained={}
 with zipfile.ZipFile(old) as z:
  names=z.namelist();assert len(names)==len(set(names)),'Duplicate ZIP entries';assert set(replacements)<=set(names),'Missing prior entry'
  if kind=='integrated':
   assert set(n for n in names if n.endswith('.class'))=={CLASS,CONTRACT,HELPER}, 'Unexpected desktop class inventory'
   assert ORPHAN not in names and ARCHIVED_ORPHAN in names and sha(z.read(ARCHIVED_ORPHAN))==ORPHAN_SHA, 'Archived extension changed'
  prior_assets=[line.split('\t') for line in z.read('ulike/assets.tsv').decode().splitlines() if line]
  next_assets=[line.split('\t') for line in (build/'assets.tsv').read_text().splitlines() if line]
  assert len(prior_assets)==len(next_assets)==3,'Unexpected asset inventory'
  for before,after in zip(prior_assets,next_assets):
   assert len(before)==len(after)==4 and before[:2]==after[:2] and before[3]==after[3],'Asset target/original contract changed'
   assert after[3] in replacements and sha(replacements[after[3]].read_bytes())==after[2],'Asset payload/hash mismatch'
  assert not set(additions)&set(names),'New helper already present in predecessor'
  output_names=[renames.get(n,n) for n in names]+list(additions)
  fields=manifest_fields(z.read('META-INF/MANIFEST.MF'));fields.update(Name=meta['name'],Description=release['manifest_description'],Version=meta['version'],Timestamp=release['timestamp'])
  manifest=encode_manifest(fields);out.parent.mkdir(parents=True,exist_ok=True)
  with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as w:
   for entry in names:
    destination=renames.get(entry,entry)
    prev=data=z.read(entry)
    if entry=='META-INF/MANIFEST.MF':data=manifest
    elif entry in replacements:data=replacements[entry].read_bytes()
    if prev!=data:changed[entry]={'before':sha(prev),'after':sha(data)}
    else:retained[destination]=sha(data)
    zi=zipfile.ZipInfo(destination,stamp);zi.compress_type=zipfile.ZIP_DEFLATED;w.writestr(zi,data)
   for entry,path in additions.items():
    data=path.read_bytes();changed[entry]={'before':None,'after':sha(data)}
    zi=zipfile.ZipInfo(entry,stamp);zi.compress_type=zipfile.ZIP_DEFLATED;w.writestr(zi,data)
 required={'META-INF/MANIFEST.MF','classes.dex'}
 if kind=='standalone':required.add(CLASS)
 assert required<=changed.keys(),'Missing release metadata changes'
 assert set(changed)<={'META-INF/MANIFEST.MF',*replacements,*additions},'Unexpected entry changed'
 with zipfile.ZipFile(out) as z:
  assert z.testzip() is None and z.namelist()==output_names
  assert manifest_fields(z.read('META-INF/MANIFEST.MF'))['Version']==meta['version']
  for entry,h in retained.items():assert sha(z.read(entry))==h
  for entry,path in replacements.items():assert z.read(entry)==path.read_bytes()
  for entry,path in additions.items():assert z.read(entry)==path.read_bytes()
 return dict(file=out.name,bytes=out.stat().st_size,sha256=sha(out.read_bytes()),changed_entries=changed,retained_entries=retained,renamed_entries=renames)
def run(old_standalone,old_integrated,build,release,output):
 output.mkdir(parents=True,exist_ok=True);results=[]
 for kind,old in [('standalone',old_standalone),('integrated',old_integrated)]:
  name=release[kind]['filename'];assert Path(name).name==name and name.endswith('.mpp')
  results.append(package(old,output/name,build,release,kind))
 with zipfile.ZipFile(output/results[0]['file']) as s,zipfile.ZipFile(output/results[1]['file']) as i:
  for n in s.namelist():
   if n.startswith(('ulike/','ulike169/')):assert s.read(n)==i.read(n),'Standalone/integrated payload mismatch '+n
 (output/'build-artifacts.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
 return results
def main():
 p=argparse.ArgumentParser()
 for key in ['old-standalone','old-integrated','build','release','output']:p.add_argument('--'+key,type=Path,required=True)
 a=p.parse_args();results=run(a.old_standalone,a.old_integrated,a.build,json.loads(a.release.read_text()),a.output);print(json.dumps(results,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
