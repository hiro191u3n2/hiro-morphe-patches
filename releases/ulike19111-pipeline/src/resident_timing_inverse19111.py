"""Exact inverse of reviewed .111 benchmark-only timing instrumentation.

The complete public .110 processing source is the immutable comparison target.
Only explicitly reviewed line replacements may be undone. A proof draft never
passes. Current .111 code is still compiled and executed by the host tests.
"""
from pathlib import Path
import hashlib,json

ORIGINAL_SHA256={
 'GpuResident1976.java':'972971abc70b62c9f9016d1b1c8dae6ef8e7e6dd3505557caada55a0164def1b',
 'QualityPipeline1932.java':'6590b305a35d65808db0e0c163cbed2c71fb321fb262c14817c93512616425bd',
 'GpuChain1961.java':'c99ef40ed8e3b6755a88d6c293f8c39b88808d9812204546a8952cc174126ead',
 'GpuStrong1960.java':'eec980d27305a7167bb907d1d1a73b31d178a80a62c2d965946bfdd935845fb6',
}

def restore(source,name,raw=None):
 source=Path(source);folder=source/'tests19111/timing-inverse'
 proof=json.loads((folder/'instrumentation-inverse.json').read_text())
 if proof.get('schema')!='ulike111-resident-timing-source-inverse-v1' or proof.get('original_release')!='1.9.110' or proof.get('reviewed') is not True:raise AssertionError('Resident timing inverse review is pending or differs')
 if set(proof['files'])!=set(ORIGINAL_SHA256) or name not in ORIGINAL_SHA256:raise AssertionError('Unreviewed Resident timing source')
 record=proof['files'][name];expected_hash=ORIGINAL_SHA256[name]
 if record['original_sha256']!=expected_hash:raise AssertionError('Published .110 processing identity changed')
 lines=((source/name).read_bytes() if raw is None else raw).decode().splitlines(True)
 previous=-1
 for change in record['changes']:
  at=change['new_line']-1
  if at<0 or at<previous:raise AssertionError('Overlapping timing inverse replacements')
  previous=at+len(change['new'].splitlines(True))
 for change in reversed(record['changes']):
  at=change['new_line']-1;expected=change['new'].splitlines(True)
  if lines[at:at+len(expected)]!=expected:raise AssertionError('Unreviewed timing instrumentation in '+name)
  lines[at:at+len(expected)]=change['old'].splitlines(True)
 restored=''.join(lines).encode()
 baseline=(folder/'baseline110'/name).read_bytes()
 if hashlib.sha256(restored).hexdigest()!=expected_hash or hashlib.sha256(baseline).hexdigest()!=expected_hash or restored!=baseline:raise AssertionError('Published .110 pixel/quality/speed/ownership body changed: '+name)
 return restored

def test(source):
 source=Path(source);folder=source/'tests19111/timing-inverse';proof=json.loads((folder/'instrumentation-inverse.json').read_text())
 controls=0;checks=0;restored={}
 for name in ORIGINAL_SHA256:
  raw=(source/name).read_bytes();value=restore(source,name,raw);restored[name]=hashlib.sha256(value).hexdigest();checks+=1
  # Unlisted changes outside the instrumentation never disappear in inverse.
  try:restore(source,name,raw+b'\n// unreviewed processing change\n')
  except AssertionError:controls+=1;checks+=1
  else:raise AssertionError('Timing inverse accepted an outside-source change')
  changes=proof['files'][name]['changes']
  if not changes:raise AssertionError('Timing proof lacks a reviewed instrumentation change: '+name)
  changed=raw.decode().splitlines(True);at=changes[0]['new_line']-1
  changed[at]='// unreviewed instrumentation replacement\n'
  try:restore(source,name,''.join(changed).encode())
  except AssertionError:controls+=1;checks+=1
  else:raise AssertionError('Timing inverse accepted altered reviewed instrumentation')
 return {'status':'passed','assertions':checks,'negative_controls':controls,'original_source_sha256':restored,'resident_timing_source_inverse19111':True,'current_source_executed_separately':True}
