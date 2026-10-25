#!/usr/bin/env python3
"""Execute model starvation and duplicate-copy regression plus the .93 queue gates."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess

def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
 tracked=[source/'GpuQualification1961.java',source/'CpuFinishCache1976.java',source/'host_queue1994.py',source/'tests1994/queue/QueueModel1994Test.java',source/'GpuCertificates1990.java',source/'tests1994/queue/CorpusLedger1994Test.java']
 pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 retained=module('queue94_retained',source/'host_queue1993.py').test(source,work/'retained93',jdk,ndk)
 classes=work/'retained93/classes'
 def run(name,args):
  done=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=60);(work/(name+'.log')).write_text(done.stdout+done.stderr)
  if done.returncode:raise AssertionError(name+'\n'+(done.stdout+done.stderr)[-12000:])
  return done.stdout
 run('model-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',classes,'-d',classes,source/'tests1994/queue/QueueModel1994Test.java',source/'GpuCertificates1990.java',source/'tests1994/queue/CorpusLedger1994Test.java'])
 current=json.loads(run('model-run',[jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.QueueModel1994Test']).strip().splitlines()[-1])
 corpus=json.loads(run('corpus-run',[jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.CorpusLedger1994Test']).strip().splitlines()[-1])
 if pins!={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Source changed during model queue test')
 result=dict(status='passed',assertions=retained['assertions']+current['assertions']+corpus['assertions'],tests=[retained,current,corpus],source_sha256=pins,physical_android_tested=False,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--source',required=True);parser.add_argument('--work',required=True);parser.add_argument('--jdk');a=parser.parse_args();print(json.dumps(test(a.source,a.work,a.jdk)))
