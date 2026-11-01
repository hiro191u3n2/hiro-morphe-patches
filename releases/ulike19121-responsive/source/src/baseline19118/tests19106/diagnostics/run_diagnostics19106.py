#!/usr/bin/env python3
"""Actual 106 scalar observer, queue, VJ1-VJ5 codec and trace formatter.
The numerical trace runtime is compiled whole, with arithmetic peers used only
for compilation. Production StrongTrace class files execute; the StrongNoise type-only peer
is present to resolve its run signature and rejects every arithmetic call.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,shutil,subprocess

def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True);jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
 fixture=source/'tests19106/diagnostics';production=[source/n for n in ('ResidentStages19104.java','VerificationJournal19101.java','GpuQualification1961.java','StrongTrace19106.java')]
 tracked=production+[fixture/'TraceCompilePeers19106.java',fixture/'JournalTrace19106Test.java',Path(__file__).resolve()];pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 retained=module('retained_reasons105',source/'tests19105/diagnostics/run_diagnostics19105.py').test(source,work/'retained105',jdk,ndk)
 def run(name,args):
  r=subprocess.run(list(map(str,args)),text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=90);(work/(name+'.log')).write_text(r.stdout)
  if r.returncode:raise AssertionError(name+'\n'+r.stdout[-18000:])
  return r.stdout
 compiled=work/'trace-compile';compiled.mkdir(exist_ok=True)
 run('trace-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',compiled,source/'StrongTrace19106.java',fixture/'TraceCompilePeers19106.java'])
 classes=work/'retained105/retained104/retained103/classes';dest=classes/'com/hiro/ulike'
 for pattern in ('StrongTrace19106*.class','StrongNoise1958*.class'):
  for p in (compiled/'com/hiro/ulike').glob(pattern):shutil.copy2(p,dest/p.name)
 stage_classes=work/'retained105/retained104/classes/com/hiro/ulike'
 for p in stage_classes.glob('ResidentStages19104*.class'):shutil.copy2(p,dest/p.name)
 # Apart from the missing StrongNoise signature type (which throws on every
 # arithmetic call), retain the tested queue/Android peers and real observer.
 run('journal-trace-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',classes,'-d',classes,fixture/'JournalTrace19106Test.java'])
 output=run('journal-trace',[jdk/'bin/java','-ea','-Xmx128m','-cp',classes,'com.hiro.ulike.JournalTrace19106Test']);trace=json.loads(next(line[7:] for line in output.splitlines() if line.startswith('RESULT ')))
 if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('106 diagnostic sources changed during tests')
 result=dict(status='passed',assertions=retained['assertions']+trace['assertions'],cases=retained['cases']+trace['cases'],journal_v1_v2_v3_v4_v5_compatible=True,trace_scope_bound=True,trace_scalar_snapshot_immutable=True,strict_mismatch_preserved=True,physical_android_tested=False,device_speedup_verified=False,trace_tests=trace,retained_diagnostics=retained,source_sha256=pins)
 (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),ensure_ascii=False,indent=2))
