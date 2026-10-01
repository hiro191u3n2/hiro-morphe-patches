#!/usr/bin/env python3
from pathlib import Path,PurePosixPath
import base64,hashlib,json,lzma
sha=lambda b:hashlib.sha256(b).hexdigest()
p=Path('source168');m=p/'SOURCE_FILES.json'
assert sha(m.read_bytes())=='1202fb16e410364f4d25f42b9371e970760931ced5b85f705d67bfbc2708b030'
previous=json.loads(m.read_text())
for n,h in previous.items():assert sha((p/n).read_bytes())==h,'Candidate source changed: '+n
encoded=Path('releases/ulike-v1.6.9/reviewed-delta.b64').read_text().strip()
assert sha(encoded.encode())=='f948c9860f3af941b80e0c564b18bd8f9ed11ab354ad9fc787483e9cfa1d250c'
d=json.loads(lzma.decompress(base64.b64decode(encoded,validate=True)))
assert d['predecessor_manifest_sha256']==sha(m.read_bytes())
def safe(n):
 q=PurePosixPath(n);assert not q.is_absolute() and '..' not in q.parts
 assert q.suffix not in {'.apk','.apks','.so','.jar','.dex','.mpp','.jks','.keystore'}
 return n
files={n:(p/safe(origin)).read_bytes() for n,origin in d['refs'].items()}
for n,v in d['delta'].items():
 assert n not in files
 lines=(p/safe(v['base'])).read_text().splitlines(keepends=True)
 pieces=[]
 for op in v['ops']:
  if isinstance(op,str):pieces.append(op)
  else:
   assert len(op)==2 and 0<=op[0]<=op[1]<=len(lines)
   pieces.append(''.join(lines[op[0]:op[1]]))
 raw=''.join(pieces).encode();assert sha(raw)==v['sha256'];files[n]=raw
root=Path('source169');assert not root.exists();root.mkdir()
for n,data in files.items():
 target=root/safe(n);target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
hashes=json.loads((root/'SOURCE_FILES.json').read_text());assert set(files)==set(hashes)|{'SOURCE_FILES.json'}
for n,h in hashes.items():assert sha((root/n).read_bytes())==h,'Final source mismatch: '+n
print('PASS',len(files),'QA-bound source files restored; published save-speed baseline pinned by the new build script.')
