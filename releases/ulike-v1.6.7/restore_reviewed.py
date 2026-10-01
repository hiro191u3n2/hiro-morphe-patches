#!/usr/bin/env python3
import base64, hashlib, io, json, tarfile, zipfile
from pathlib import Path, PurePosixPath
ROOT=Path.cwd(); DEST=ROOT/'source167'
PARTS=ROOT/'releases/ulike-v1.6.7'
SOURCE_SHA='61097987afdc4ac23efc85afe41c1580b7d62d66fd7be3fc240e8f2ba05ca1cc'
BASE_SHA='1e1dd17615086b0e69dc7e8ed733526251a3a753d0ca6b8d71a33c596e4e22b3'
def safe(n):
 p=PurePosixPath(n)
 assert not p.is_absolute() and '..' not in p.parts,n
 return p
parts=sorted(PARTS.glob('source.b64.*'))
assert [p.name for p in parts]==['source.b64.000','source.b64.001']
raw=base64.b64decode(''.join(''.join(p.read_text().split()) for p in parts),validate=True)
assert hashlib.sha256(raw).hexdigest()==SOURCE_SHA,'Source delta mismatch'
DEST.mkdir(exist_ok=True)
with tarfile.open(fileobj=io.BytesIO(raw),mode='r:xz') as t:
 seen=set()
 for m in t.getmembers():
  safe(m.name);assert m.isfile() and m.name not in seen,m.name;seen.add(m.name)
  assert Path(m.name).suffix.lower() not in {'.apk','.apks','.so','.jks','.keystore','.p12'}
 t.extractall(DEST,filter='data')
base=ROOT/'input/ULike_v1.6.6_sources_and_QA.zip'
assert hashlib.sha256(base.read_bytes()).hexdigest()==BASE_SHA,'Predecessor source mismatch'
with zipfile.ZipFile(base) as z:
 assert z.testzip() is None and len(z.namelist())==len(set(z.namelist()))
 for dst,src in json.loads((DEST/'BASE_MAP.json').read_text()).items():
  safe(dst);safe(src);out=DEST/dst;assert not out.exists(),'Base overwrite '+dst
  out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(z.read(src))
for name,digest in json.loads((DEST/'SOURCE_FILES.json').read_text()).items():
 safe(name);assert hashlib.sha256((DEST/name).read_bytes()).hexdigest()==digest,'Source file mismatch '+name
print('PASS complete reviewed source restored; no application APK or private data required.')
