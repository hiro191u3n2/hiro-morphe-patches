#!/usr/bin/env python3
"""Reconstruct local-reviewed Back113 sources. No active distribution changes."""
from pathlib import Path,PurePosixPath
import base64,hashlib,json,lzma
ROOT=Path(__file__).resolve().parent
BASE=ROOT.parent/'homeback112'
DATA=''.join((ROOT/f'source.delta.{i}.txt').read_text() for i in range(1,4))
raw=base64.b64decode(''.join(DATA.split()),validate=True)
assert hashlib.sha256(raw).hexdigest()=='961fe68e2b3b5f8ebe988442300e2281a312548cdb25ae027840ea483ebfe3e7','Source transport corrupt'
package=json.loads(lzma.decompress(raw));rows=package['rows']
def safe(name):
 p=PurePosixPath(name)
 assert str(p)==name and not p.is_absolute() and '..' not in p.parts and '\\' not in name
 return p
for name,row in rows.items():
 target=ROOT/safe(name)
 assert not target.exists(),'Source unexpectedly exists: '+name
 if 'text' in row:data=row['text'].encode('utf-8')
 else:
  src=BASE/safe(row.get('base',row.get('copy')));original=src.read_bytes()
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
print('PASS reconstructed '+str(len(rows))+' hash-pinned reviewed source files')
