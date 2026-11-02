#!/usr/bin/env python3
"""Production stage reasons, fixed-row bridge and versioned journal integration.
Android/GPU peers enforce scope only; no physical-device claim is made.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess

def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True);jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
 production=[source/name for name in ('ResidentStages19104.java','VerificationJournal19101.java','GpuQualification1961.java')]
 fixtures=source/'tests19105/diagnostics';tracked=production+[fixtures/'ReasonDiagnostic19105Test.java',fixtures/'JournalReason19105Test.java',Path(__file__).resolve()]
 pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 retained=module('retained_diagnostics104',source/'tests19104/validation/run_validation19104.py').test(source,work/'retained104',jdk,ndk)
 def run(name,args):
  result=subprocess.run(list(map(str,args)),text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=60);(work/(name+'.log')).write_text(result.stdout)
  if result.returncode:raise AssertionError(name+'\n'+result.stdout)
  return result.stdout
 def execute(name,classes,harness):
  run(name+'-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',classes,'-d',classes,fixtures/(harness+'.java')])
  output=run(name,[jdk/'bin/java','-ea','-Xmx128m','-cp',classes,'com.hiro.ulike.'+harness])
  return json.loads(next(line[7:] for line in output.splitlines() if line.startswith('RESULT ')))
 reasons=execute('reasons',work/'retained104/classes','ReasonDiagnostic19105Test')
 journal=execute('journal',work/'retained104/retained103/classes','JournalReason19105Test')
 if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Diagnostic production source changed during test')
 result=dict(status='passed',assertions=reasons['assertions']+journal['assertions']+retained['assertions'],cases=reasons['cases']+journal['cases'],diagnostic_reasons_separated=True,legacy_failure_not_instability=True,known_empty_mask_observed=True,fixed_replay_api_bounded=True,journal_v1_v2_v3_v4_compatible=True,queue_journal_extended_scalars=True,strict_mismatch_preserved=True,physical_android_tested=False,device_speedup_verified=False,reason_tests=reasons,journal_tests=journal,retained_validation=retained,source_sha256=pins)
 (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),ensure_ascii=False,indent=2))
