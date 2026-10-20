#!/usr/bin/env python3
"""Execute the bounded timing observer and exact .111 -> published .110 inverse.

The production observer is unchanged. Fixtures replace only queue eligibility,
cancellation ownership and its scalar output sink. No device speed is inferred.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess

def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();local=source/'tests19111/timing'
 tracked=[source/'ResidentTiming19111.java',source/'resident_timing_inverse19111.py',Path(__file__).resolve(),*sorted(local.rglob('*.java')),*sorted((source/'tests19111/timing-inverse').rglob('*'))]
 tracked=[p for p in tracked if p.is_file()]
 inverse=module('resident_timing_inverse_test19111',source/'resident_timing_inverse19111.py')
 tracked += [source/name for name in inverse.ORIGINAL_SHA256]
 pins={p.relative_to(source).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 reports={'source_inverse':inverse.test(source)}
 def run(label,args):
  result=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=90);output=result.stdout+result.stderr;(work/(label+'.log')).write_text(output)
  if result.returncode:raise AssertionError(label+'\n'+output[-12000:])
  return output
 classes=work/'classes';classes.mkdir(exist_ok=True)
 run('compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,source/'ResidentTiming19111.java',local/'TimingPeers19111.java',local/'ResidentTiming19111Test.java'])
 output=run('observer',[jdk/'bin/java','-ea','-Xmx96m','-cp',classes,'com.hiro.ulike.ResidentTiming19111Test'])
 reports['observer']=json.loads(next(line[7:] for line in output.splitlines() if line.startswith('RESULT ')))
 if pins!={p.relative_to(source).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Relevant timing source changed during test')
 flags={}
 for name,proofs in [('source_inverse',['resident_timing_source_inverse19111']),('observer',['actual_timing_observer_executed19111','exclusive_timing_boundaries19111','timing_optional_failure_isolation19111','timing_cancel_state_preserved19111','timing_scalar_thread_ownership19111'])]:
  for proof in proofs:
   if reports[name].get(proof) is not True:raise AssertionError('Missing executed timing proof '+proof)
   flags[proof]=True
 result={'status':'passed','assertions':sum(r['assertions'] for r in reports.values()),'tests':reports,'source_sha256':pins,'physical_android_tested':False,'device_speedup_verified':False,**flags}
 (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result

if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--source',required=True);parser.add_argument('--work',required=True);parser.add_argument('--jdk');parser.add_argument('--ndk');args=parser.parse_args()
 print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),ensure_ascii=False,indent=2))
