#!/usr/bin/env python3
"""Exercise native certificate identity invalidation on a private source graph."""
from pathlib import Path
import argparse,importlib.util,json,shutil

def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 manifest=json.loads((source/'native1960/compute_dependencies1990.json').read_text())
 private=work/'source';private.mkdir()
 names=set(manifest['native_dependencies']+manifest['numerical_java']+list(manifest['excluded_runtime_java']))|{'native1960/compute_dependencies1990.json'}
 for name in names:
  p=private/name;p.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source/name,p)
 spec=importlib.util.spec_from_file_location('fingerprint1990_test',private/'native1960/build_native1960.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
 native=private/'native1960';compute=module.compute_fingerprint1990
 assertions=0
 def check(v,label):
  nonlocal assertions
  assert v,label;assertions+=1
 baseline,pins=compute(native);check(len(pins)==len(manifest['native_dependencies'])+len(manifest['numerical_java']),'all declared numerical/native files hashed')
 check(compute(native)[0]==baseline,'deterministic identity')
 (private/'PatchClass9999.java').write_text('public class PatchClass9999 {}\n')
 check(compute(native)[0]==baseline,'packaging helper leaves compute identity unchanged')
 p=private/'ProcessingTiming1947.java';p.write_text(p.read_text()+'\n// diagnostic-only build revision\n')
 check(compute(native)[0]==baseline,'diagnostics-only edit leaves compute identity unchanged')
 for name in ('QualityPixels1932.java','GpuStrongDefault1990.java','native1960/strong1960.comp','native1960/engine1960.c','native1955/single_noise1955.c'):
  p=private/name;raw=p.read_bytes();p.write_bytes(raw+b'\n// numerical revision\n')
  check(compute(native)[0]!=baseline,'numerical/corpus/transport/reference change invalidates '+name);p.write_bytes(raw)
 def refuses(label):
  nonlocal assertions
  try:compute(native)
  except (RuntimeError,FileNotFoundError):assertions+=1;return
  raise AssertionError(label)
 p=private/'GpuUnclassified1991.java';p.write_text('package com.hiro.ulike;\npublic class GpuUnclassified1991 {}\n');refuses('new runtime requires review');p.unlink()
 p=native/'new1991.comp';p.write_text('#version 310 es\n');refuses('new native compute source requires review');p.unlink()
 p=private/'QualityPixels1932.java';raw=p.read_bytes();p.unlink();refuses('missing dependency fails closed');p.write_bytes(raw)
 p=native/'compute_dependencies1990.json';raw=p.read_bytes();bad=dict(manifest);bad['schema']='unknown';p.write_text(json.dumps(bad));refuses('unknown schema fails closed');p.write_bytes(raw)
 check(compute(native)[0]==baseline,'restored graph returns exact identity')
 report={'status':'passed','assertions':assertions,'physical_android_tested':False,'schema':'ulike1990-compute-fingerprint-tests-v1','compute_dependency_count':len(pins),'diagnostic_and_packaging_changes_preserve_identity':True,'numerical_and_corpus_changes_invalidate_identity':True,'unclassified_or_missing_source_fails_closed':True}
 (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=Path(__file__).parent);p.add_argument('--work',type=Path,required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work),indent=2))
