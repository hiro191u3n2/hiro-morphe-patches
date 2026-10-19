#!/usr/bin/env python3
"""Retain the .92 memory/queue/route gate with .99's versioned baseline key.

The old runner and fixture are unchanged on disk. Only javac's one route-fixture
input is replaced with a generated copy whose old prefix assertion becomes a
strict current-suite and 32-row identity check. All other assertions and every
production source remain unchanged. Full 32/128 transitions are also covered by
host_chain1999.py; no proof/speed/cancellation condition is relaxed here.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, subprocess

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 original=source/'tests1992/finish_memory/FinishRoute1992Test.java'
 runner=source/'host_finish_memory1992.py'
 before={str(p.relative_to(source)):sha(p) for p in (original,runner)}
 text=original.read_text()
 old='baseline!=null&&baseline.startsWith("standard:")'
 # This fixture installed CPU_SUITE1992 only; a wide/default-policy certificate
 # would be an error. Require the entire expected key, not a looser new prefix.
 new='baseline!=null&&baseline.equals("standard1999:"+GpuFinishDefault1990.CPU_SUITE1992+":32")'
 if text.count(old)!=1:raise AssertionError('Exactly one historical standard-key assertion required')
 adapted=work/'fixture1999/FinishRoute1992Test.java';adapted.parent.mkdir(parents=True,exist_ok=True)
 adapted.write_text(text.replace(old,new))
 spec=importlib.util.spec_from_file_location('finish_memory92_adapter99',runner)
 inherited=importlib.util.module_from_spec(spec);spec.loader.exec_module(inherited)
 replacements=[]
 class CompilerAdapter:
  @staticmethod
  def run(args,*a,**kw):
   args=list(args)
   if args and Path(str(args[0])).name=='javac':
    indices=[i for i,p in enumerate(args) if str(p)==str(original)]
    for i in indices:args[i]=str(adapted);replacements.append(i)
   return subprocess.run(args,*a,**kw)
 inherited.subprocess=CompilerAdapter
 result=inherited.test(source,work/'retained92',jdk,ndk)
 if len(replacements)!=1:raise AssertionError('Exactly one fixture compile substitution required')
 if result.get('status')!='passed' or not result.get('route',{}).get('actual_route_control'):raise AssertionError('Retained route gate did not execute')
 if before!={str(p.relative_to(source)):sha(p) for p in (original,runner)}:raise AssertionError('Historical gate was modified')
 result.update(retained_finish_memory1992_gate=True,strict_standard1999_baseline_identity=True,
  historical_source_sha256=before,adapted_fixture_sha256=sha(adapted),adapter_sha256=sha(Path(__file__)))
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
