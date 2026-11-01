#!/usr/bin/env python3
"""Current Java queue execution: priority, copy-budget cap and safe fault detail."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, subprocess

def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
 inputs=[source/'GpuQualification1961.java',source/'tests1993/queue/QueuePriority1993Test.java',source/'tests1992/failure/Qualification1992Test.java',source/'tests1992/failure/TypedFailure1992Test.java']
 tracked=inputs+[source/'CpuFinishCache1976.java',source/'host_queue1993.py',source/'host_cpu_finish_gate1976.py']
 pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 spec=importlib.util.spec_from_file_location('queue93_peers',source/'host_qualification1981.py');peers=importlib.util.module_from_spec(spec);spec.loader.exec_module(peers)
 files=[]
 for name,body in peers.FIXTURES.items():
  if name.endswith('QueueHost1967.java'):continue
  if name.endswith('QueueFixtures1967.java'):body=body.replace('static long captureEpoch1953(){return epoch;}','static long captureEpoch1953(){return epoch;}static long captureId1989(long e){return e==epoch?1:0;}')
  path=work/'peers'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body);files.append(path)
 classes=work/'classes';classes.mkdir(exist_ok=True)
 def run(label,args):
  done=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=60);(work/(label+'.log')).write_text(done.stdout+done.stderr)
  if done.returncode:raise AssertionError(label+'\n'+(done.stdout+done.stderr)[-12000:])
  return done.stdout
 run('compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*inputs,*files])
 results=[]
 for name in ['QueuePriority1993Test','Qualification1992Test','TypedFailure1992Test']:
  results.append(json.loads(run(name,[jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.'+name]).strip().splitlines()[-1]))
 # Retain the actual finish-cache lifecycle/gating test, with the current
 # scalar timing peer and a check before queue() transfers the reservation.
 spec=importlib.util.spec_from_file_location('queue93_finish',source/'host_cpu_finish_gate1976.py');finish=importlib.util.module_from_spec(spec);spec.loader.exec_module(finish)
 finish.STUBS=finish.STUBS.replace('static long captureEpoch1953(){return epoch;}','static long captureEpoch1953(){return epoch;}static long captureId1989(long e){return e==epoch?1:0;}')
 finish.HARNESS=finish.HARNESS.replace('check(p!=null,"bounded complete sequence captured");p.queue();','check(p!=null,"bounded complete sequence captured");check(snapshot(7)==null,"another band blocked before first snapshot queues");p.queue();')
 results.append(finish.test(source,work/'finish-cache',jdk,ndk))
 if pins!={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Source changed during queue test')
 result=dict(status='passed',assertions=sum(x['assertions'] for x in results),tests=results,source_sha256=pins,physical_android_tested=False,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--source',required=True);parser.add_argument('--work',required=True);parser.add_argument('--jdk');a=parser.parse_args();print(json.dumps(test(a.source,a.work,a.jdk)))
