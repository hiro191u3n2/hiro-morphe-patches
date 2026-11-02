#!/usr/bin/env python3
"""Bound .76 source changes against the frozen, published .75 source inventory.

Optimized files are covered by executed old/new exact-output regression runners;
all other production Java/native/shader sources must retain their exact bytes.
"""
from pathlib import Path
import hashlib,json

def sha(raw):return hashlib.sha256(raw).hexdigest()

# Changes permitted by accepted candidates 6-12 and new bounded diagnostics.
JAVA_EDITS={
 'GpuStrong1960.java','GpuNoise1960.java','GpuQualification1961.java','GpuStrongTuning1975.java',
 'GpuPolicy1960.java','GpuChain1961.java','QualityPipeline1932.java','NativeMoire1951.java',
 'ProcessingTiming1947.java','CameraTrace1965.java','SingleNoise1955.java','SingleResidual1961.java',
 'quality-dependencies/com/hiro/ulike/FaceRegions1934.java',
}
NATIVE_EDITS={
 'native1960/engine1960.c','native1960/build_native1960.py','native1960/residual_blocks1961.c',
 'native1951/moire1951.c','native1951/build_native1951.py','native1955/single_noise1955.c','native1955/build_native1955.py',
}

def production(name):
 path=Path(name)
 return (len(path.parts)==1 and path.suffix=='.java') or name.startswith('quality-dependencies/') or (path.parts[0].startswith('native') and path.parts[0][6:].isdigit() and path.suffix in ('.c','.h','.comp','.glsl'))

def test(source,work):
 source,work=Path(source),Path(work);work.mkdir(parents=True,exist_ok=True)
 reference_path=source/'tests1976/source-scope75.json'
 reference=json.loads(reference_path.read_text())
 allowed=JAVA_EDITS|NATIVE_EDITS
 unchanged_java=[];unchanged_native=[];changed=[]
 for name,digest in reference.items():
  if not production(name):continue
  raw=(source/name).read_bytes()
  if sha(raw)!=digest:
   if name not in allowed:raise AssertionError('Unreviewed published75 source changed: '+name)
   changed.append(name)
  else:
   (unchanged_native if name.startswith('native') else unchanged_java).append(name)
 original=(source/'tests1976/CameraTrace1965.java').read_text()
 if (source/'CameraTrace1965.java').read_text().replace('1.9.76','1.9.75')!=original:
  raise AssertionError('CameraTrace changed beyond version')
 if not changed or not unchanged_java or not unchanged_native:raise AssertionError('Empty reviewed source-scope inventory')
 result={'status':'passed','unmodified_native_shader_sources_byte_identical':True,
  'reviewed_native_scope_verified':True,'unmodified_java_sources_byte_identical':True,
  'unmodified_cpu_and_other_shader_sources':True,'camera_trace_version_only_preserved':True,
  'reference_sha256':sha(reference_path.read_bytes()),'native_sources':unchanged_native,
  'java_sources':unchanged_java,'reviewed_changed_sources':sorted(changed),
  'allowed_changed_sources':sorted(allowed),'optimized_file_equivalence_requires_fresh_host_tests':True}
 (work/'source-scope.json').write_text(json.dumps(result,sort_keys=True,indent=2)+'\n')
 return result
