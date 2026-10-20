#!/usr/bin/env python3
from pathlib import Path
import hashlib,json

def sha(raw):return hashlib.sha256(raw).hexdigest()
def test(source,work):
 source,work=Path(source),Path(work);work.mkdir(parents=True,exist_ok=True)
 reference=json.loads((source/'tests1975/source-scope74.json').read_text())
 allowed={name+'.java' for name in json.loads((source/'production1975.json').read_text())}
 transport={'native1960/engine1960.c','native1960/build_native1960.py'}
 native=[];java=[];changed=[]
 for name,digest in reference.items():
  if name in allowed:continue
  raw=(source/name).read_bytes()
  if name in transport:
   if sha(raw)!=digest:changed.append(name)
   continue
  if sha(raw)!=digest:raise AssertionError('Unreviewed .74 pixel/source changed: '+name)
  (native if name.startswith('native1960/') else java).append(name)
 original=(source/'tests1975/CameraTrace1965.java').read_text()
 current=(source/'CameraTrace1965.java').read_text()
 if current.replace('1.9.75','1.9.74')!=original:raise AssertionError('CameraTrace changed beyond version')
 if set(changed)!=transport:raise AssertionError('Expected exactly engine and native builder transport changes')
 if not native or not java:raise AssertionError('Empty source preservation inventory')
 result={'status':'passed','native_shader_sources_byte_identical':True,'native_transport_only_changed':True,
 'unmodified_java_sources_byte_identical':True,'unchanged_cpu_and_other_shader_sources':True,'camera_trace_version_only_preserved':True,
 'reference_sha256':sha((source/'tests1975/source-scope74.json').read_bytes()),'native_sources':native,'java_sources':java,'changed_native_transport_sources':changed}
 (work/'source-scope.json').write_text(json.dumps(result,sort_keys=True,indent=2)+'\n')
 return result
