"""Exact reviewed .112 Strong-plan/finish-row handoff inverse to public .111.

This preservation transform is only for inherited source-identity gates. The
new behavior is executed from unmodified current sources by its own tests.
"""
from pathlib import Path
import hashlib,json

ORIGINAL_SHA256={'QualityPipeline1932.java':'a10406bc6f19255decf3426aafb46254229d2d73e309502a44348898a7744368'}

def restore(source,name,raw=None):
 source=Path(source);folder=source/'tests19112/plan'
 proof=json.loads((folder/'plan-inverse.json').read_text())
 if proof.get('schema')!='ulike112-resident-plan-source-inverse-v1' or proof.get('original_release')!='1.9.111' or proof.get('reviewed') is not True:raise AssertionError('Resident plan inverse review pending or differs')
 if set(proof['files'])!=set(ORIGINAL_SHA256) or name not in ORIGINAL_SHA256:raise AssertionError('Unreviewed resident plan source')
 record=proof['files'][name];expected_hash=ORIGINAL_SHA256[name]
 if record['original_sha256']!=expected_hash:raise AssertionError('Published .111 planner identity changed')
 lines=((source/name).read_bytes() if raw is None else raw).decode().splitlines(True)
 previous=-1
 for change in record['changes']:
  at=change['new_line']-1
  if at<0 or at<previous:raise AssertionError('Overlapping resident plan inverse replacements')
  previous=at+len(change['new'].splitlines(True))
 for change in reversed(record['changes']):
  at=change['new_line']-1;expected=change['new'].splitlines(True)
  if lines[at:at+len(expected)]!=expected:raise AssertionError('Unreviewed resident plan block in '+name)
  lines[at:at+len(expected)]=change['old'].splitlines(True)
 restored=''.join(lines).encode();baseline=(folder/'baseline111'/name).read_bytes()
 if hashlib.sha256(restored).hexdigest()!=expected_hash or hashlib.sha256(baseline).hexdigest()!=expected_hash or restored!=baseline:raise AssertionError('Unreviewed .111 processing change outside resident plan: '+name)
 return restored

def test(source):
 source=Path(source);folder=source/'tests19112/plan';proof=json.loads((folder/'plan-inverse.json').read_text());checks=0;controls=0
 for name in ORIGINAL_SHA256:
  raw=(source/name).read_bytes();restore(source,name,raw);checks+=1
  altered=[raw+b'\n// unreviewed processing change\n']
  changes=proof['files'][name]['changes']
  if not changes:raise AssertionError('Resident plan inverse lacks reviewed changes')
  for change in changes:
   if not change['new']:continue
   lines=raw.decode().splitlines(True);lines[change['new_line']-1]='// unreviewed plan implementation\n';altered.append(''.join(lines).encode())
  for value in altered:
   try:restore(source,name,value)
   except AssertionError:controls+=1;checks+=1
   else:raise AssertionError('Resident plan inverse accepted mutation')
 return {'status':'passed','assertions':checks,'negative_controls':controls,'resident_plan_source_inverse19112':True,'current_source_executed_separately':True,'original_source_sha256':ORIGINAL_SHA256}
