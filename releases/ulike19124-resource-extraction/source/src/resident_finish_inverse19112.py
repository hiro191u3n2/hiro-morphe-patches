"""Reviewed112 schedule/lifetime changes restored to exact published111 bodies.

This inverse protects retained historical proofs only. The current production
bodies are compiled and executed independently by resident_finish19112.
"""
from pathlib import Path
import hashlib,json
ORIGINAL_SHA256={
 'GpuChain1961.java':'8da03b9ae3d770133f22e75defd555d8696e31ceac53955bfb86fa54063c4847',
 'GpuResident1976.java':'c7d367ed3cf20fae706807d8d1801405213b2c6d41da2d655e1a700f83fe5f9e',
}

def restore(source,name,raw=None):
 source=Path(source);current=(source/name).read_bytes() if raw is None else raw
 if name=='GpuResident1976.java':
  from source_inverse19115 import restore as restore115
  current=restore115(source,name,current)
 return _restore114(source,name,current)

def _restore114(source,name,raw):
 """Historical .112 proof only; callers validate the complete .115 inverse first."""
 source=Path(source);folder=source/'tests19112/finish'
 proof=json.loads((folder/'source-inverse.json').read_text())
 if proof.get('schema')!='ulike112-resident-finish-source-inverse-v1' or proof.get('original_release')!='1.9.111' or proof.get('reviewed') is not True:raise AssertionError('Resident112 schedule/lifetime inverse review pending or changed')
 if set(proof['files'])!=set(ORIGINAL_SHA256) or name not in ORIGINAL_SHA256:raise AssertionError('Unreviewed Resident112 source')
 record=proof['files'][name];digest=ORIGINAL_SHA256[name]
 if record['original_sha256']!=digest:raise AssertionError('Published111 source identity changed')
 lines=raw.decode().splitlines(True);previous=-1
 for change in record['changes']:
  at=change['new_line']-1
  if at<0 or at<previous:raise AssertionError('Overlapping Resident112 inverse changes')
  previous=at+len(change['new'].splitlines(True))
 for change in reversed(record['changes']):
  at=change['new_line']-1;expected=change['new'].splitlines(True)
  if lines[at:at+len(expected)]!=expected:raise AssertionError('Unreviewed Resident112 change in '+name)
  lines[at:at+len(expected)]=change['old'].splitlines(True)
 restored=''.join(lines).encode();baseline=(folder/'baseline111'/name).read_bytes()
 if hashlib.sha256(restored).hexdigest()!=digest or hashlib.sha256(baseline).hexdigest()!=digest or restored!=baseline:raise AssertionError('Published111 processing body differs: '+name)
 return restored

def test(source):
 source=Path(source);proof=json.loads((source/'tests19112/finish/source-inverse.json').read_text());checks=controls=0;restored={}
 for name in ORIGINAL_SHA256:
  raw=(source/name).read_bytes();value=restore(source,name,raw);restored[name]=hashlib.sha256(value).hexdigest();checks+=1
  from source_inverse19115 import REVIEWED,restore as restore115
  if name in REVIEWED:raw=restore115(source,name,raw)
  # Mutate only the restored .114 source so the legacy .112 guard is exercised.
  for altered in (raw+b'\n// unreviewed outside processing change\n',raw.replace(b'package com.hiro.ulike;',b'package com.hiro.changed;',1)):
   try:_restore114(source,name,altered)
   except AssertionError:controls+=1;checks+=1
   else:raise AssertionError('Resident112 inverse accepted outside change')
  for change in proof['files'][name]['changes']:
   if not change['new']:continue
   lines=raw.decode().splitlines(True);at=change['new_line']-1;lines[at]='// unreviewed schedule/lifetime replacement\n'
   try:_restore114(source,name,''.join(lines).encode())
   except AssertionError:controls+=1;checks+=1
   else:raise AssertionError('Resident112 inverse accepted changed reviewed hunk')
 return {'status':'passed','assertions':checks,'negative_controls':controls,'original_source_sha256':restored,'historical_negative_scope19115':'restored114_source','resident_finish_source_inverse19112':True,'current_source_executed_separately':True}
