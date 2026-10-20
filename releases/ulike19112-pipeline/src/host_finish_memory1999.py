#!/usr/bin/env python3
"""Retain the .92 gate and execute the .112 phase-owned lifetime contract.

The old runner and fixtures are unchanged on disk. The current graph retains
every assertion except the retired future-phase-debt assertion, which becomes
an exact zero-debt check with the same live-source bind requirement. Separately,
the unchanged original graph fixture executes against the strict inverse to
published .111. Current production is compiled and executed first; historical
classes never enter its classpath or the later memory97 pressure fixture.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, subprocess

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 original=source/'tests1992/finish_memory/FinishRoute1992Test.java'
 original_memory=source/'tests1992/finish_memory/FinishMemory1992Test.java'
 runner=source/'host_finish_memory1992.py'
 tracked=(original,original_memory,runner,source/'GpuChain1961.java',source/'GpuResident1976.java',
  source/'resident_finish_inverse19112.py',source/'tests19112/finish/source-inverse.json',
  source/'tests19112/finish/baseline111/GpuChain1961.java',source/'tests19112/finish/baseline111/GpuResident1976.java')
 before={str(p.relative_to(source)):sha(p) for p in tracked}
 text=original.read_text()
 old='baseline!=null&&baseline.startsWith("standard:")'
 # This fixture installed CPU_SUITE1992 only; a wide/default-policy certificate
 # would be an error. Require the entire expected key, not a looser new prefix.
 new='baseline!=null&&baseline.equals("standard1999:"+GpuFinishDefault1990.CPU_SUITE1992+":32")'
 if text.count(old)!=1:raise AssertionError('Exactly one historical standard-key assertion required')
 adapted=work/'fixture1999/FinishRoute1992Test.java';adapted.parent.mkdir(parents=True,exist_ok=True)
 adapted.write_text(text.replace(old,new))
 old_memory='preflight.javaPeak>0&&preflight.bind(carried),"future phase debt admitted before Strong processing"'
 new_memory='preflight.javaPeak==0&&preflight.bind(carried),"only live source is reserved before Strong; future finish phase has zero debt"'
 memory_text=original_memory.read_text()
 if memory_text.count(old_memory)!=1:raise AssertionError('Exactly one retired future-phase-debt assertion required')
 adapted_memory=work/'fixture1999/FinishMemory1992Test.java'
 adapted_memory.write_text(memory_text.replace(old_memory,new_memory))
 spec=importlib.util.spec_from_file_location('finish_memory92_adapter99',runner)
 inherited=importlib.util.module_from_spec(spec);spec.loader.exec_module(inherited)
 replacements={str(original):[],str(original_memory):[]}
 adaptations={str(original):adapted,str(original_memory):adapted_memory}
 class CompilerAdapter:
  @staticmethod
  def run(args,*a,**kw):
   args=list(args)
   if args and Path(str(args[0])).name=='javac':
    for original_path,adapted_path in adaptations.items():
     indices=[i for i,p in enumerate(args) if str(p)==original_path]
     for i in indices:args[i]=str(adapted_path);replacements[original_path].append(i)
   return subprocess.run(args,*a,**kw)
 inherited.subprocess=CompilerAdapter
 result=inherited.test(source,work/'retained92',jdk,ndk)
 if any(len(indices)!=1 for indices in replacements.values()):raise AssertionError('Exactly one compile substitution for each reviewed fixture required')
 if result.get('status')!='passed' or not result.get('route',{}).get('actual_route_control'):raise AssertionError('Retained route gate did not execute')
 # The old positive-debt contract remains an executed historical gate, not a
 # deleted assertion. Only the reviewed .112 changes can invert to this exact
 # published .111 body, and this separate classpath cannot mask current code.
 inverse_spec=importlib.util.spec_from_file_location('finish_memory112_inverse',source/'resident_finish_inverse19112.py')
 inverse=importlib.util.module_from_spec(inverse_spec);inverse_spec.loader.exec_module(inverse)
 restored=inverse.restore(source,'GpuChain1961.java')
 history=work/'historical111';history.mkdir(exist_ok=True)
 restored_chain=history/'GpuChain1961.java';restored_chain.write_bytes(restored)
 history_classes=history/'classes';history_classes.mkdir(exist_ok=True)
 jdk_path=Path(jdk or os.environ['ULIKE_JDK_HOME'])
 jar=Path(os.environ.get('ULIKE_ANDROID_JAR',str(jdk_path.parent/'android.jar')))
 current_cp=os.pathsep.join(map(str,[work/'retained92/graph/classes',work/'retained92/production',jar]))
 def historical_run(label,args):
  completed=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=180)
  output=completed.stdout+completed.stderr;(history/(label+'.log')).write_text(output)
  if completed.returncode:raise AssertionError(label+'\n'+output[-14000:])
  return completed.stdout
 historical_run('compile-chain',[jdk_path/'bin/javac','--release','8','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[work/'retained92/production',jar])),'-d',history_classes,restored_chain])
 historical_run('compile-fixture',[jdk_path/'bin/javac','--release','8','-encoding','UTF-8','-cp',str(history_classes)+os.pathsep+current_cp,'-d',history_classes,original_memory])
 historical=json.loads(historical_run('run',[jdk_path/'bin/java','-ea','-Xmx768m','-cp',str(history_classes)+os.pathsep+current_cp,'com.hiro.ulike.FinishMemory1992Test']).strip().splitlines()[-1])
 if historical.get('status')!='passed' or historical.get('actual_finish_graph') is not True or historical.get('full_pixel_comparator') is not True:raise AssertionError('Historical phase lifetime gate did not execute')
 if before!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Historical gate or current production was modified')
 result['historical_finish111']=historical;result['assertions']+=historical['assertions']
 result.update(retained_finish_memory1992_gate=True,strict_standard1999_baseline_identity=True,
  current_phase_owned_contract19112=True,retained_finish_phase111=True,
  historical_source_sha256=before,adapted_fixture_sha256=sha(adapted),adapted_memory_fixture_sha256=sha(adapted_memory),
  restored_chain111_sha256=sha(restored_chain),adapter_sha256=sha(Path(__file__)))
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
