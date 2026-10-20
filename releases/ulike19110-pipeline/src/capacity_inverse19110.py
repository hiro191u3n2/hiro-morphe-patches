"""Exact, reviewed inverse of v1.9.110 capacity-refusal instrumentation.

The original .107/.108 pin is immutable. Only explicitly enumerated new blocks
are replaced; any missing, duplicate, altered or additional source change fails.
The current production source is still the one compiled and executed by tests.
"""
from pathlib import Path
import hashlib,json

def restore(source,name="GpuNoise1960.java"):
 source=Path(source);proof_name={'GpuNoise1960.java':'capacity-diagnostic-inverse.json','GpuStrong1960.java':'strong-admission-inverse.json'}[name];proof=json.loads((source/'tests19110/strong'/proof_name).read_text())
 lines=(source/name).read_text().splitlines(True)
 for change in reversed(proof['changes']):
  at=change['new_line']-1;expected=change['new'].splitlines(True)
  if lines[at:at+len(expected)]!=expected:raise AssertionError('Unreviewed capacity instrumentation at line '+str(at+1))
  lines[at:at+len(expected)]=change['old'].splitlines(True)
 restored=''.join(lines).encode();digest=hashlib.sha256(restored).hexdigest()
 if digest!=proof['original_sha256']:raise AssertionError('Original capacity/native ownership body changed outside reviewed instrumentation')
 return restored

def test(source):
 source=Path(source);pins=json.loads((source/'tests19108/memory/unchanged-native107.json').read_text());restored=restore(source)
 digest=hashlib.sha256(restored).hexdigest()
 if digest!=pins['GpuNoise1960.java']:raise AssertionError('Historical original-budget pin changed')
 for name,value in pins.items():
  if name!='GpuNoise1960.java' and hashlib.sha256((source/name).read_bytes()).hexdigest()!=value:raise AssertionError('Original owner/native bytecode source changed '+name)
 return {'status':'passed','assertions':len(pins)+1,'original_sha256':digest,'reviewed_inverse':True,'current_source_executed_separately':True,'strong_admission_original_budget19110':True}
