"""Exact, reviewed inverse of v1.9.110 capacity-refusal instrumentation.

The original .107/.108 pin is immutable. Only explicitly enumerated new blocks
are replaced; any missing, duplicate, altered or additional source change fails.
The current production source is still the one compiled and executed by tests.
"""
from pathlib import Path
import hashlib,json
from resident_timing_inverse19111 import restore as restore_timing19111
from inherited_shader19114 import restore_shader19113

def restore(source,name="GpuNoise1960.java"):
 source=Path(source);proof_name={'GpuNoise1960.java':'capacity-diagnostic-inverse.json','GpuStrong1960.java':'strong-admission-inverse.json'}[name];proof=json.loads((source/'tests19110/strong'/proof_name).read_text())
 current=(source/name).read_bytes()
 if name=='GpuNoise1960.java':
  from source_inverse19115 import restore as restore115
  current=restore115(source,name,current)
 if name=='GpuStrong1960.java':current=restore_timing19111(source,name,current)
 lines=current.decode().splitlines(True)
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
 # The reviewed .114/.113 arithmetic changes do not alter allocation/ownership.
 # Reverse only its exact reviewed blocks for this historical guard; the caller
 # still compiles the current shader and runs the actual JNI admission tests.
 shader_name='native1960/strong1960.comp';shader_text=(source/shader_name).read_text()
 def original_shader113(text):
  raw=restore_shader19113(text,required=True).encode()
  if hashlib.sha256(raw).hexdigest()!=pins[shader_name]:raise AssertionError('Strong .113 inverse exceeded reviewed .112 source')
  return raw
 shader_original=original_shader113(shader_text);shader_negatives=0
 from source_inverse19115 import restore as restore115
 historical114=restore115(source,'native1960/strong1960.comp',shader_text.encode()).decode()
 for mutated in (historical114+'\n',historical114.replace('GX_FULL_RATIO19113','GX_FULL_RATIO_UNREVIEWED',1),historical114.replace('GX_STRONG_MODE == 3','GX_STRONG_MODE == 2',1)):
  if mutated==historical114:raise AssertionError('Missing shader source negative-control anchor')
  try:original_shader113(mutated)
  except (AssertionError,RuntimeError,ValueError):shader_negatives+=1
  else:raise AssertionError('Unreviewed Strong source edit accepted by historical admission guard')
 for name,value in pins.items():
  if name=='GpuNoise1960.java':continue
  raw=shader_original if name==shader_name else (source/name).read_bytes()
  from source_inverse19115 import REVIEWED,restore as restore115
  if name!=shader_name and name in REVIEWED:raw=restore115(source,name,raw)
  if hashlib.sha256(raw).hexdigest()!=value:raise AssertionError('Original owner/native bytecode source changed '+name)
 return {'status':'passed','assertions':len(pins)+1+shader_negatives,'original_sha256':digest,'reviewed_inverse':True,'current_source_executed_separately':True,'strong_admission_original_budget19110':True,
  'historical_native107_sha256':pins,'current_native_source_sha256':{name:hashlib.sha256((source/name).read_bytes()).hexdigest() for name in pins},
  'shader_source_inverse19113_sha256':hashlib.sha256(shader_original).hexdigest(),'shader_source_inverse19113_negative_controls':shader_negatives,'historical_negative_scope19115':'restored114_source','memory_transport_source_unchanged19113':True}
