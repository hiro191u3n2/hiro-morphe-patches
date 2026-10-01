#!/usr/bin/env python3
"""Reconstruct only reviewed UTF-8 sources, rejecting all unpinned inputs."""
import argparse,base64,hashlib,json,shutil,zipfile,zlib
from pathlib import Path,PurePosixPath

def sha(b):return hashlib.sha256(b).hexdigest()
def blob(b):return hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,required=True);p.add_argument('--payload',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
 assert sha(a.source.read_bytes())=='3f7d2100f8a6e86e5a717baf6df50f6ad15bb36466d2c98f4a39c7a2b5381344','Unexpected predecessor'
 assert not a.out.exists(),'Output must not exist'
 chunks=[]
 pins=['621d9b66a12916f5bfe35a0361e3efa0a287c108','1fc418ab6828e2bec33971ab1a40f3ff10664bef','5eccaadf6dbe2d8284fa3408f9f06ebf939a80ba','60fad8daeafcf2b52583ef842f6b1b54f9bfc69e']
 for i,h in enumerate(pins):
  b=(a.payload/f'payload.b64.{i:02}').read_bytes()
  # Normalize a known transcription error only under its exact transport hash.
  if i==2 and blob(b)=='930532351f0ce7a6285ae107477784c95a1c95d0':
   for old,new in [('VdVMG1BV','VdVM1BV'),('bh+kUhrgk','bh+khrgk'),('AnNpaIHYnhw','AnNpaIHnhw'),('BpfTmyw3i6','BpfTmyw0i6'),('+Ip0d1So5','+Ip0dSo5'),('B4UArx5SyLZZ','B4UArx5LZZ'),('FxfOzs8uOVU','FxfOzs8OVU')]:
    assert b.count(old.encode())==1;b=b.replace(old.encode(),new.encode())
  assert blob(b)==h,f'Chunk {i} does not match reviewed bytes'
  chunks.append(b.strip())
 encoded=b''.join(chunks)
 assert sha(encoded)=='d818523aa1205c32a79dc2663ae86e70ee81f32b6303bf77476f1fe328e77593'
 delta=json.loads(zlib.decompress(base64.b64decode(encoded,validate=True)))
 with zipfile.ZipFile(a.source) as z:
  assert z.testzip() is None
  for entry in z.infolist():
   n=PurePosixPath(entry.filename);assert not n.is_absolute() and '..' not in n.parts and not entry.is_dir()
   assert (entry.external_attr>>16)&0o170000 != 0o120000
   f=a.out/entry.filename;f.parent.mkdir(parents=True,exist_ok=True);f.write_bytes(z.read(entry))
 for name,d in delta.items():
  n=PurePosixPath(name);assert not n.is_absolute() and '..' not in n.parts
  f=a.out/name;old=f.read_bytes() if f.exists() else b'';assert sha(old)==d['before'],name
  lines=old.decode('utf-8').splitlines(keepends=True)
  for start,end,text in reversed(d['ops']):
   assert 0<=start<=end<=len(lines);lines[start:end]=text.splitlines(keepends=True)
  result=''.join(lines).encode('utf-8');assert sha(result)==d['after'],name
  f.parent.mkdir(parents=True,exist_ok=True);f.write_bytes(result)
 for n in ['repair171.py','finalize171.py','Transform170.java','SOURCE_FILES.json']:(a.out/n).unlink(missing_ok=True)
 shutil.rmtree(a.out/'qa170',ignore_errors=True);(a.out/'qa170').mkdir()
 print('PASS: exact reviewed source delta restored; no application bytes included.')
if __name__=='__main__':main()
