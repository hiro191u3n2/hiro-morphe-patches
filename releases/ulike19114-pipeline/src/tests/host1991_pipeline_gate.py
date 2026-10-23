#!/usr/bin/env python3
"""Execute current pipeline startup barrier and resident-copy admission.

Readiness/Looper peers are explicitly controlled to test ordering and refusal,
not pixel correctness or Android speed. All pipeline/resident code is current
production; positive/negative controls observe actual Bitmap allocation and the
real qualification queue. Retained JNI pixel runners cover the arithmetic.
"""
from pathlib import Path
import argparse, json, os, re, subprocess

PEERS={
'android/os/Looper.java':'''package android.os;public class Looper{private static final Looper MAIN=new Looper();public static boolean ui;public static Looper getMainLooper(){return MAIN;}public static Looper myLooper(){return ui?MAIN:null;}}''',
'com/hiro/ulike/GpuStartup1990.java':'''package com.hiro.ulike;
public final class GpuStartup1990 {
 static volatile boolean ready=true,hold;static volatile long requested;static volatile int calls;
 static final java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
 public static boolean awaitCoreReady(long ms){calls++;requested=ms;entered.countDown();if(hold)try{release.await();}catch(InterruptedException stop){Thread.currentThread().interrupt();return false;}return ready;}
 public static boolean domainReady(){return ready;}public static void initialize(android.content.Context c){}
 public static final class Snapshot {public int state,preparedPrograms,failedPrograms;public long startedNanos,readyNanos,elapsedNanos;public boolean domainReady,finishDomainReady;}
 public static Snapshot snapshot(){return new Snapshot();}
}''',
'com/hiro/ulike/GpuStrongDefault1990.java':'''package com.hiro.ulike;public final class GpuStrongDefault1990{static boolean available=true;static boolean ready(){return available;}static boolean owns(int[]u){return false;}static boolean accepts(int[]u){return false;}}''',
'com/hiro/ulike/GpuFinishDefault1990.java':'''package com.hiro.ulike;public final class GpuFinishDefault1990{static boolean available=true;static boolean ready(){return available;}static android.graphics.Bitmap run(android.graphics.Bitmap b,int r,int w,int h,QualityPixels1932.Plan p,boolean f,ProcessingTiming1947.Trace t,String k){return null;}}''',
}
HARNESS=r'''
package com.hiro.ulike;
import android.graphics.Bitmap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
public final class PipelineGate1991Test {
 static int assertions;
 static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
 public static void main(String[] args)throws Exception {
  check(!QualityPipeline1932.awaitStartup1990(false,null)&&GpuStartup1990.calls==0,"disabled filters do not wait");
  android.os.Looper.ui=true;
  check(!QualityPipeline1932.awaitStartup1990(true,null)&&GpuStartup1990.calls==0,"UI final-save fallback cannot block on startup");
  android.os.Looper.ui=false;
  java.lang.reflect.Field cpu=GpuResident1976.class.getDeclaredField("CPU");cpu.setAccessible(true);
  @SuppressWarnings("unchecked") ThreadLocal<Boolean> oracle=(ThreadLocal<Boolean>)cpu.get(null);oracle.set(Boolean.TRUE);
  check(!QualityPipeline1932.awaitStartup1990(true,null)&&GpuStartup1990.calls==0,"CPU oracle never waits on itself");oracle.remove();
  GpuStartup1990.hold=true;final CountDownLatch done=new CountDownLatch(1);final boolean[] result={false};
  Thread save=new Thread(new Runnable(){public void run(){result[0]=QualityPipeline1932.awaitStartup1990(true,null);done.countDown();}},"controlled-save-worker");save.start();
  check(GpuStartup1990.entered.await(2,TimeUnit.SECONDS),"worker enters startup before later image stages");
  check(!done.await(100,TimeUnit.MILLISECONDS)&&GpuStartup1990.requested==30000L,"worker waits with required 30-second budget");
  GpuStartup1990.release.countDown();check(done.await(2,TimeUnit.SECONDS)&&result[0],"worker resumes only after readiness fence");GpuStartup1990.hold=false;
  GpuStartup1990.ready=false;check(!QualityPipeline1932.awaitStartup1990(true,null),"failed preparation not called GPU ready");
  GpuStartup1990.ready=true;Thread.currentThread().interrupt();boolean cancelled=false;
  try{QualityPipeline1932.awaitStartup1990(true,null);}catch(java.util.concurrent.CancellationException expected){cancelled=true;}
  check(cancelled&&Thread.currentThread().isInterrupted(),"cancel before source/model preserves interrupt");Thread.interrupted();

  android.os.Build.FINGERPRINT="resident-copy-controlled-admission1991";
  GpuQualification1961.initialize(new android.content.Context());check(GpuNoise1960.warmEnvironment1973(),"real native context for existing resident capability gates");
  Bitmap source=Chain1962Oracle.image(65,67,2);QualityPixels1932.Plan plan=Chain1962Oracle.plan(source,65,67,0);
  ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(source);
  int allocations=Bitmap.ALL.size(),reads=source.reads;long retained=GpuQualification1961.retainedBytes();
  Bitmap value=GpuResident1976.finish1978(source,plan,4,true,null,0,65,67,plan,true,trace);
  check(value==null,"uncertified resident leaves foreground on proven standard route");
  check(Bitmap.ALL.size()==allocations+1&&source.reads==reads,"startup readiness still permits exactly one detached snapshot");
  check(GpuQualification1961.retainedBytes()>retained&&trace.residentReason1978==2,"independent proofs no longer starve connected-route qualification");
  int copied=Bitmap.ALL.size();long queued=GpuQualification1961.retainedBytes();
  value=GpuResident1976.finish1978(source,plan,4,true,null,0,65,67,plan,true,trace);
  check(value==null&&Bitmap.ALL.size()==copied,"duplicate attempt cannot take another whole-image snapshot");
  check(GpuQualification1961.retainedBytes()==queued,"duplicate request preserves bounded retained ownership");
  GpuQualification1961.captureChanged();
  check(GpuQualification1961.retainedBytes()==0,"capture cancellation releases queued resident snapshot");
  GpuFinishDefault1990.available=false;
  value=GpuResident1976.finish1978(source,plan,4,true,null,0,65,67,plan,true,trace);
  check(value==null,"missing independent finish proof never exposes an uncertified resident result");
  GpuQualification1961.captureChanged();source.recycle();
  check(GpuQualification1961.retainedBytes()==0&&!GpuNoise1960.sessionBusy(),"snapshot ownership and native session released");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"controlled_admission_peers\":true,\"actual_pipeline_resident_code\":true}");
 }
}
'''
def test(source,work,classes,native,jdk):
    source,work,classes,native,jdk=map(lambda p:Path(p).resolve(),(source,work,classes,native,jdk));work.mkdir(parents=True,exist_ok=True)
    files=[]
    peers=dict(PEERS,**{'com/hiro/ulike/PipelineGate1991Test.java':HARNESS})
    bitmap=(source/'tests/pipeline1942-fixtures/android/graphics/Bitmap.java').read_text()
    # Preserve fixture pixels/copy semantics; supply real accounting for its
    # contiguous four-byte ARGB allocation so the negative snapshot can run.
    bitmap=bitmap.replace('public final class Bitmap {','public final class Bitmap {public int getRowBytes(){return width*4;}public int getAllocationByteCount(){return width*height*4;}public int getGenerationId(){return 1;}')
    peers['android/graphics/Bitmap.java']=bitmap
    for name,body in peers.items():
        path=work/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body);files.append(path)
    compiled=work/'classes';compiled.mkdir(exist_ok=True)
    def run(command,label,env=None):
        result=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=60,env=env)
        output=result.stdout+result.stderr;(work/(label+'.log')).write_text(output)
        if result.returncode:raise AssertionError(label+'\n'+output[-12000:])
        return output
    run([jdk/'bin/javac','--release','8','-cp',classes,'-d',compiled,*files,source/'QualityPipeline1932.java',source/'GpuResident1976.java',source/'ProcessingTiming1947.java'],'compile')
    output=run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Xcheck:jni','-Djava.library.path='+str(native),'-cp',str(compiled)+os.pathsep+str(classes),'com.hiro.ulike.PipelineGate1991Test'],'execute',dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1'))
    match=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
    if not match:raise AssertionError('Missing policy execution result')
    result=json.loads(match.group(1));result.update(physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser()
    for key in ('source','work','classes','native','jdk'):p.add_argument('--'+key,required=True)
    a=p.parse_args();print(json.dumps(test(a.source,a.work,a.classes,a.native,a.jdk),indent=2))
