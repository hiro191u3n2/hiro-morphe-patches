#!/usr/bin/env python3
from pathlib import Path,PurePosixPath
import base64,hashlib,json,lzma,zipfile
encoded=''.join((Path('releases/ulike-v1.6.8')/f'source.xz.b64.{i:02}').read_text().strip() for i in range(5))
assert hashlib.sha256(encoded.encode()).hexdigest()=='cc5b1c5a0109de5cfb2eabac6d49e0591f5914662d95074f453d868d22568041','Staged delta changed'
manifest=json.loads(lzma.decompress(base64.b64decode(encoded,validate=True)))
old=Path('input/ULike_v1.6.7_sources_and_QA.zip')
assert hashlib.sha256(old.read_bytes()).hexdigest()==manifest['predecessor_zip_sha256']=='4142c603b97107ce705e52380be950726d680a8ef607c634eba9cf134307fef7'
files={n:s.encode() for n,s in manifest['files'].items()}
with zipfile.ZipFile(old) as z:
 for n,origin in manifest['refs'].items():
  assert n not in files;files[n]=z.read(origin)
root=Path('source168');assert not root.exists();root.mkdir()
for n,data in files.items():
 p=PurePosixPath(n);assert not p.is_absolute() and '..' not in p.parts
 assert p.suffix not in {'.apk','.apks','.so','.jar','.dex','.mpp','.keystore','.jks'}
 target=root/n;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
hashes=json.loads((root/'SOURCE_FILES.json').read_text());assert set(files)==set(hashes)|{'SOURCE_FILES.json'}
for n,h in hashes.items():assert hashlib.sha256((root/n).read_bytes()).hexdigest()==h,'Restored source mismatch: '+n
print('PASS',len(files),'hash-bound reviewed source files restored; no original apps or toolchain included.')
