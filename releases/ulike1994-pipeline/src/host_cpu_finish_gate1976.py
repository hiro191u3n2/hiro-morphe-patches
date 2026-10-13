#!/usr/bin/env python3
"""Run the production CPU finish qualifier with the actual bounded queue.
Controlled kernel timing exercises gate logic; actual native pixels are covered
by host_cpu_masks1976. No physical Android speed claim is made.
"""
from pathlib import Path
import importlib.util,json,subprocess,hashlib,os
STUBS=r'''package com.hiro.ulike;
import java.util.*;import java.util.concurrent.*;
final class ProcessingTiming1947 {static volatile long epoch=1;static long captureEpoch1953(){return epoch;}}
final class SaveQueue1935 {static volatile boolean idle;static boolean idle1953(){return idle;}}
final class WholeRoute1953 {static long retainedBytes(){return 0;}}
final class SpeedWorkers1935 {static int maxWorkers(){return 2;}static int availableWorkers1944(){return 2;}static void run(Runnable[] a){for(Runnable r:a)r.run();}}
final class GpuNoise1960 {static volatile boolean budget=true;static String fingerprint(){return "cpu-finish-proof-host76";}static boolean sessionBusy(){return false;}static boolean workspaceFits(long bytes){return budget&&bytes<512L*1024*1024;}}
final class NativeMoire1951 {
 static volatile boolean slow,mismatch,fail,block;static int calls,oldCalls,newCalls;static CountDownLatch entered,release;
 static boolean proof1976(CpuFinishCache1976.Snapshot p,int[] output,boolean cached) {
  calls++;if(cached)newCalls++;else oldCalls++;
  if(block){entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();return false;}}
  if(fail)return false;
  try{Thread.sleep((cached==slow)?15:1);}catch(InterruptedException e){Thread.currentThread().interrupt();return false;}
  for(int band=0;band<p.recorded;band++)for(int y=p.starts[band];y<p.ends[band];y++)for(int x=0;x<p.width;x++)output[y*p.width+x]=p.source[y*p.width+x]^p.policies[band][((y-p.starts[band])*p.width+x)*4];
  if(cached&&mismatch)output[p.last*p.width-1]^=1;
  return true;
 }
}
'''
HARNESS=r'''package com.hiro.ulike;
import android.content.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;
public final class CpuFinishGate1976Test {
 static int checks;static void check(boolean x,String s){checks++;if(!x)throw new AssertionError(s);}
 static Field field(String n)throws Exception {Field f=GpuQualification1961.class.getDeclaredField(n);f.setAccessible(true);return f;}
 static void drain()throws Exception {long stop=System.nanoTime()+10000000000L;while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>stop)throw new AssertionError("undrained");Thread.sleep(3);}check(true,"snapshot ownership drained");}
 static void clean()throws Exception {SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();if(NativeMoire1951.release!=null)NativeMoire1951.release.countDown();drain();NativeMoire1951.slow=NativeMoire1951.mismatch=NativeMoire1951.fail=NativeMoire1951.block=false;NativeMoire1951.calls=NativeMoire1951.oldCalls=NativeMoire1951.newCalls=0;GpuNoise1960.budget=true;}
 static void age()throws Exception {SaveQueue1935.idle=true;field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();}
 static String key(int tag){return CpuFinishCache1976.key(8+tag,40,3,37,true,true,true,128,512,16,true,true);}
 static CpuFinishCache1976.Snapshot snapshot(int tag) {
  int w=8+tag;int[] src=new int[w*40];Arrays.fill(src,33);CpuFinishCache1976.Snapshot p=CpuFinishCache1976.capture(key(tag),src,w,40,3,37,true,true,true,128,512,16,true,true);
  if(p==null)return null;src[0]=999;check(p.source[0]==33,"source deep copied");
  for(int start=3;start<37;){int end=Math.min(37,(start+16)&~15);int[] policy=new int[w*(end-start)*4];Arrays.fill(policy,7);p.record(start,end,policy);policy[0]=123;check(p.policies[p.recorded-1][0]==7,"all policy bands detached");start=end;}
  return p;
 }
 public static void main(String[] args)throws Exception {
  GpuQualification1961.initialize(new Context());clean();
  check(!CpuFinishCache1976.enabled(key(0)),"cold preserves old kernel");CpuFinishCache1976.Snapshot p=snapshot(0);check(p!=null,"bounded complete sequence captured");p.queue();long retained=GpuQualification1961.retainedBytes();check(retained>0&&retained<=96L*1024*1024,"only snapshot bytes retained");
  check(snapshot(0)==null,"same key deduplicated");Thread.sleep(20);check(NativeMoire1951.calls==0,"foreground does not benchmark");age();drain();check(NativeMoire1951.oldCalls==3&&NativeMoire1951.newCalls==3,"warmup plus two full trials each");check(CpuFinishCache1976.enabled(key(0)),"exact faster complete call accepted");check(p.source==null&&p.policies==null,"accepted proof drops pixels and policies");
  clean();NativeMoire1951.slow=true;p=snapshot(1);p.queue();age();drain();check(!CpuFinishCache1976.enabled(key(1)),"exact slower cache refused");
  clean();NativeMoire1951.mismatch=true;p=snapshot(2);p.queue();age();drain();check(GpuQualification1961.exactRejected(key(2))&&!CpuFinishCache1976.enabled(key(2)),"last pixel mismatch rejected");
  clean();NativeMoire1951.fail=true;p=snapshot(3);p.queue();age();drain();check(!CpuFinishCache1976.enabled(key(3)),"unavailable optional cache never certifies");
  clean();GpuNoise1960.budget=false;check(snapshot(4)==null,"no clone when budget rejected");GpuNoise1960.budget=true;p=snapshot(4);p.queue();GpuNoise1960.budget=false;age();drain();check(NativeMoire1951.calls==0&&!CpuFinishCache1976.enabled(key(4)),"fresh memory revalidation blocks work");
  clean();p=snapshot(5);NativeMoire1951.block=true;NativeMoire1951.entered=new CountDownLatch(1);NativeMoire1951.release=new CountDownLatch(1);p.queue();age();check(NativeMoire1951.entered.await(5,TimeUnit.SECONDS),"background proof started");ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();NativeMoire1951.release.countDown();drain();check(!CpuFinishCache1976.enabled(key(5))&&p.source==null,"capture invalidates pending result and releases snapshot");
  clean();int[] source=new int[320];p=CpuFinishCache1976.capture(key(6),source,8,40,3,37,true,true,true,128,512,16,true,true);check(p!=null,"malformed record fixture capture");p.record(4,16,new int[384]);check(p.source==null,"noncontiguous policy closes candidate");p.queue();check(GpuQualification1961.retainedBytes()==0,"incomplete candidate never queued");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"cpu_finish_two_exact_and_speed_gate_verified\":true,\"cpu_finish_snapshot_cancel_memory_verified\":true,\"physical_android_tested\":false}");
 }
}
'''
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True);jdk=Path(jdk or os.environ['ULIKE_JDK_HOME'])
 spec=importlib.util.spec_from_file_location('qual_fixture1976',source/'host_qualification1967.py');base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
 generated=work/'source';classes=work/'classes';classes.mkdir(exist_ok=True);paths=[]
 for name,text in base.FIXTURES.items():
  if not name.startswith('android/'):continue
  p=generated/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);paths.append(p)
 for name,text in [('FinishGateStubs.java',STUBS),('CpuFinishGate1976Test.java',HARNESS)]:
  p=generated/name;p.write_text(text);paths.append(p)
 production=[source/'CpuFinishCache1976.java',source/'GpuQualification1961.java'];pins={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in production}
 def run(args,label):
  p=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=90);(work/(label+'.log')).write_text(p.stdout+p.stderr)
  if p.returncode:raise RuntimeError(label+': '+p.stdout[-4000:]+p.stderr[-8000:])
  return p.stdout
 run([jdk/'bin/javac','--release','8','-d',classes,*paths,*production],'compile')
 result=json.loads(next(x[7:] for x in run([jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.CpuFinishGate1976Test'],'run').splitlines() if x.startswith('RESULT ')))
 if pins!={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in production}:raise AssertionError('Production changed during proof')
 result.update(production_source_sha256=pins,device_speedup_verified=False,runner_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest())
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 import argparse
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
