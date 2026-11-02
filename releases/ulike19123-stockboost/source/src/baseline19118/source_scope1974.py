#!/usr/bin/env python3
from pathlib import Path
import hashlib,json

def sha(raw):return hashlib.sha256(raw).hexdigest()
def test(source,work):
 source,work=Path(source),Path(work);work.mkdir(parents=True,exist_ok=True)
 reference=json.loads((source/'tests1974/source-scope73.json').read_text())
 allowed={name+'.java' for name in json.loads((source/'production1974.json').read_text())}
 native=[];java=[]
 for name,digest in reference.items():
  if name in allowed:continue
  raw=(source/name).read_bytes()
  if sha(raw)!=digest:raise AssertionError('Unreviewed .73 native/pixel/source changed: '+name)
  (native if name.startswith('native1960/') else java).append(name)
 original=(source/'tests1974/CameraTrace1965.java').read_text()
 current=(source/'CameraTrace1965.java').read_text()
 if current.replace('1.9.74','1.9.73')!=original:raise AssertionError('CameraTrace changed beyond version')
 if not native or not java:raise AssertionError('Empty source preservation inventory')
 result={'status':'passed','native_sources_byte_identical':True,'unmodified_java_sources_byte_identical':True,
 'unchanged_cpu_and_other_shader_sources':True,'camera_trace_version_only_preserved':True,
 'reference_sha256':sha((source/'tests1974/source-scope73.json').read_bytes()),'native_sources':native,'java_sources':java}
 (work/'source-scope.json').write_text(json.dumps(result,sort_keys=True,indent=2)+'\n')
 return result
