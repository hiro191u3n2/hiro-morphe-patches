"""Fixed inverse of only the reviewed .110 Resident typed-refusal changes.

Reconstructs the complete published .108 source. In particular the two-round
comparison, timing, qualification and ownership flow must remain byte exact.
This proof complements execution of the actual current source; it is not a
substitute source for current compilation or execution.
"""
from pathlib import Path
import hashlib,json
from resident_timing_inverse19111 import _restore114 as restore_timing19111

ORIGINAL_SHA256='8bd4269eb0222f86388c71af034bca0657e8fffc0b563c77a0d1f5832d781ac4'

def restore(source,raw=None):
 source=Path(source);current=(source/'GpuResident1976.java').read_bytes() if raw is None else raw
 from source_inverse19115 import restore as restore115
 return _restore114(source,restore115(source,'GpuResident1976.java',current))

def _restore114(source,raw):
 """Historical .110 proof only; callers validate the complete .115 inverse first."""
 source=Path(source);proof=json.loads((source/'tests19110/copy/resident-admission-inverse.json').read_text())
 if proof['original_release']!='1.9.108' or proof['original_sha256']!=ORIGINAL_SHA256:raise AssertionError('Published Resident identity changed')
 current=raw
 # Keep the original .110 -> .108 proof unchanged; first undo only reviewed
 # .111 observer calls, retaining an exact published .110 intermediate.
 lines=restore_timing19111(source,'GpuResident1976.java',current).decode().splitlines(True)
 for change in reversed(proof['changes']):
  at=change['new_line']-1;expected=change['new'].splitlines(True)
  if lines[at:at+len(expected)]!=expected:raise AssertionError('Unreviewed Resident typed-refusal change')
  lines[at:at+len(expected)]=change['old'].splitlines(True)
 restored=''.join(lines).encode()
 if hashlib.sha256(restored).hexdigest()!=ORIGINAL_SHA256:raise AssertionError('Published Resident comparison/timing/ownership changed')
 return restored

def test(source):
 source=Path(source);raw=(source/'GpuResident1976.java').read_bytes();restored=restore(source)
 from source_inverse19115 import restore as restore115
 raw=restore115(source,'GpuResident1976.java',raw)
 baseline=(source/'tests19110/copy/baseline108/GpuResident1976.java').read_bytes()
 assert hashlib.sha256(baseline).hexdigest()==ORIGINAL_SHA256 and restored==baseline
 controls=0
 # Both reviewed new text and otherwise unchanged proof flow are immutable.
 for before,after in [(b'String refusal19110=admissionRefusal19110(failure);',b'String refusal19110="memory_budget";'),
                      (b'if(refusal19110==null)throw failure;',b'if(refusal19110==null)return;'),
                      (b'if(comparison==ResidentProof1978.EXACT)return true;',b'if(comparison==ResidentProof1978.EXACT)return false;')]:
  assert raw.count(before)==1
  try:_restore114(source,raw.replace(before,after))
  except AssertionError:controls+=1
  else:raise AssertionError('Resident source inverse accepted a negative control')
 return {'status':'passed','assertions':5,'negative_controls':controls,'original_sha256':ORIGINAL_SHA256,'historical_negative_scope19115':'restored114_source','resident_source_inverse19110':True,'current_source_executed_separately':True}
