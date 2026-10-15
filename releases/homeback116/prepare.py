#!/usr/bin/env python3
"""Reconstruct reviewed Back116 fixes. Feeds change only after SDK/QA/byte gates pass."""
from pathlib import Path,PurePosixPath
import base64,hashlib,json,lzma
ROOT=Path(__file__).resolve().parent
BASE=ROOT.parent/'homeback115'
DATA=''.join((ROOT/('transport-%d.txt'%i)).read_text().strip() for i in range(3))
raw=base64.b64decode(DATA,validate=True)
assert hashlib.sha256(raw).hexdigest()=='c8a4cdab9382f776f1051a5af940f7aab1479bf9bc9f0f488db7e07cc48df308','Source transport corrupt'
package=json.loads(lzma.decompress(raw))
def safe(name):
 p=PurePosixPath(name)
 assert name and str(p)==name and not p.is_absolute() and '..' not in p.parts and '\\' not in name
 return p
for name,row in package['rows'].items():
 target=ROOT/safe(name)
 assert not target.exists(),'Source unexpectedly exists: '+name
 if 'text' in row:data=row['text'].encode('utf-8')
 else:
  original=(BASE/safe(row.get('base',row.get('copy')))).read_bytes()
  assert hashlib.sha256(original).hexdigest()==row['sha'],'Baseline source differs: '+name
  if 'copy' in row:data=original
  else:
   lines=original.decode('utf-8').splitlines(True)
   for start,end,replacement in reversed(row['edits']):
    assert 0<=start<=end<=len(lines)
    lines[start:end]=replacement
   data=''.join(lines).encode('utf-8')
 assert hashlib.sha256(data).hexdigest()==package['expected'][name],'Reconstructed source differs: '+name
 target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
print('PASS reconstructed '+str(len(package['rows']))+' hash-pinned reviewed source files')
