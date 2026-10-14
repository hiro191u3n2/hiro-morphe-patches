#!/usr/bin/env python3
"""Actual publication queue plus optional-family retry/fault ownership tests.
Retains the .94 queue and corpus-store evidence; no device speed claim.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess

def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
 tracked=[source/p for p in ['GpuQualification1961.java','GpuStrongDefault1990.java','GpuModelDefault1990.java','host_queue1995.py','tests1994/strong/AdaptivePeers1994.java','tests1994/strong/StrongAdaptiveState1994Test.java','tests1994/model/ModelLayout1994Test.java']]+list((source/'tests1995/queue').glob('*.java'))
 pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 retained=module('queue95_retained',source/'host_queue1994.py').test(source,work/'retained94',jdk,ndk)
 def run(label,args):
  done=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=90);(work/(label+'.log')).write_text(done.stdout+done.stderr)
  if done.returncode:raise AssertionError(label+'\n'+(done.stdout+done.stderr)[-14000:])
  return done.stdout
 classes=work/'retained94/retained93/classes'
 run('publication-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',classes,'-d',classes,source/'tests1995/queue/PublicationQueue1995Test.java'])
 publication=json.loads(run('publication-run',[jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.PublicationQueue1995Test']).strip().splitlines()[-1])
 # Deliberately controlled timing only for lifecycle tests. Production Java
 # corpus, exact comparison loop, request owner and speed decision unchanged.
 # .98: the artificial baseline gap is 200 ms and the control JVM uses four
 # CPUs, matching the numerical runners. This isolates lifecycle assertions
 # from oversized host-GC scheduling; it is never device speed evidence.
 body=(source/'tests1994/strong/AdaptivePeers1994.java').read_text()
 body=body.replace('static boolean slowCandidate;','static boolean slowCandidate,baselineDelay1995;')
 body=body.replace('submits++;if(fault==4)', 'submits++;if(baselineDelay1995&&profile==4)try{Thread.sleep(200);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new java.util.concurrent.CancellationException();}if(fault==4)')
 body=body.replace('static boolean acceptQueue;','static boolean acceptQueue,scheduledUnderAdaptiveLock1995;')
 body=body.replace('scheduled++;if(acceptQueue)', 'scheduledUnderAdaptiveLock1995|=Thread.holdsLock(GpuStrongDefault1990.Adaptive1994.class);scheduled++;if(acceptQueue)')
 body=body.replace('static final java.util.Map<String,Evidence> proofs=', 'static boolean interruptExact1995,failFast1995;static final java.util.Map<String,Evidence> proofs=')
 body=body.replace('if(rejected(suite))return false;proofs.put(suite,new Evidence(c,p,cpu,gpu));return true;', 'if(rejected(suite)||failFast1995&&suite.equals(GpuStrongDefault1990.Adaptive1994.FAST))return false;proofs.put(suite,new Evidence(c,p,cpu,gpu));if(interruptExact1995&&suite.equals(GpuStrongDefault1990.Adaptive1994.EXACT))Thread.currentThread().interrupt();return true;')
 peers=work/'StrongPeers1995.java';peers.write_text(body)
 strongclasses=work/'strong-classes';strongclasses.mkdir(exist_ok=True)
 run('strong-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',strongclasses,source/'GpuStrongDefault1990.java',peers,source/'tests1994/strong/StrongAdaptiveState1994Test.java',source/'tests1995/queue/StrongRecovery1995Test.java'])
 strong=json.loads(run('strong-run',[jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-Xmx512m','-cp',strongclasses,'com.hiro.ulike.StrongRecovery1995Test']).strip().splitlines()[-1])
 modelclasses=work/'model-classes';modelclasses.mkdir(exist_ok=True)
 run('model-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',modelclasses,source/'GpuModelDefault1990.java',source/'tests1994/model/ModelLayout1994Test.java',source/'tests1995/queue/OptionalModel1995Test.java'])
 model=json.loads(run('model-run',[jdk/'bin/java','-ea','-Xmx512m','-cp',modelclasses,'com.hiro.ulike.OptionalModel1995Test']).strip().splitlines()[-1])
 if pins!={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Source changed during .95 queue test')
 result=dict(status='passed',assertions=sum(x['assertions'] for x in [retained,publication,strong,model]),tests=[retained,publication,strong,model],source_sha256=pins,physical_android_tested=False,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk)))
