#!/usr/bin/env python3
"""Production resident admission/ownership state machine. Pixel/JNI graphs are
validated by host_chain1976 and the independent complete Strong facade runner."""
from pathlib import Path
import json, subprocess, shutil

BITMAP=r'''package android.graphics;
public class Bitmap {
 public enum Config {ARGB_8888} public int width,height;public int[] pixels;public boolean recycled,wide,hdr;public static int copies,recycles;
 public Bitmap(int w,int h){width=w;height=h;pixels=new int[]{0xff123456,0xff987654};} public int getWidth(){return width;}public int getHeight(){return height;}public Config getConfig(){return Config.ARGB_8888;}public boolean isRecycled(){return recycled;}
 public Bitmap copy(Config c,boolean mutable){copies++;Bitmap b=new Bitmap(width,height);b.pixels=pixels.clone();b.wide=wide;b.hdr=hdr;return b;}public void recycle(){if(!recycled){recycled=true;recycles++;}}
}'''
STUBS=r'''package com.hiro.ulike;
import android.graphics.Bitmap;import java.util.*;
final class SpatialNoise1934 {int width=4080,height=3060,columns=5,rows=5;}
final class FaceRegions1934 {static class Mask implements QualityPixels1932.RegionMask {long retainedBytes1976(){return 32768;}}}
final class QualityPixels1932 {
 interface RegionMask {} interface SmoothMask {}
 static class Plan {int beautyQ8=3,shadowBudgetQ8=4,sharpGainQ8=200,sharpFloorQ8=4,sharpLimit=7;boolean texturePriority=true,haloSuppression=true;float sourceSigma=2,outputScale=1.4f;RegionMask faceRegions=new FaceRegions1934.Mask();SpatialNoise1934 localNoise=new SpatialNoise1934();Object policyCache;SmoothMask smoothedRegions;
 Plan withFaceRegions(RegionMask f){Plan p=new Plan();p.faceRegions=f;p.policyCache=null;return p;}Plan withSmoothedRegions(SmoothMask s){smoothedRegions=s;return this;}}
}
final class ProcessingTiming1947 {static class Trace {}static int route;static void detailRoute1976(Trace t,int r){route=r;}}
final class GpuChain1961 {static String baseline="1:100:50";static String residentBaseline1976(Bitmap b,int r,int w,int h,QualityPixels1932.Plan p){return baseline;}static boolean eligibleResident1976(Bitmap b,int r,int w,int h,QualityPixels1932.Plan p){return !b.wide&&!b.hdr&&(p.faceRegions==null||p.faceRegions instanceof FaceRegions1934.Mask);}
 static boolean opaqueResident1976(Bitmap b){for(int p:b.pixels)if((p>>>24)!=255)return false;return true;}}
final class GpuNoise1960 {static final int FINISH1961=1,GEOMETRY=2,ANALYSIS1961=3;static boolean busy,fits=true;static long budget;static boolean sessionBusy(){return busy;}static boolean supports(int s){return true;}static String fingerprint(){return "resident-test-device";}static boolean workspaceFits(long b){budget=b;return fits;}}
final class GpuGeometry1960 {static boolean equal1961(Bitmap a,Bitmap b){return a!=null&&b!=null&&Arrays.equals(a.pixels,b.pixels);}}
final class GpuQualification1961 {
 interface Cancellation {boolean cancelled();}interface Probe {void run(Cancellation c);void close();}static class Record {long cpuNanos,gpuNanos;Record(long c,long g){cpuNanos=c;gpuNanos=g;}}
 static Map<String,Record> records=new HashMap<>();static Set<String> negatives=new HashSet<>();static Probe queued;static String key;static boolean background,cancel,decline;static int exact,slow,admissions;static long held;
 static boolean background(){return background;}static boolean exactRejected(String k){return negatives.contains(k);}static Record restore(String k){return records.get(k);}static boolean canQueue(String k,long b){return queued==null&&!negatives.contains(k)&&b>0&&b<=96L*1024*1024;}
 static boolean schedule(String k,long b,Probe p){key=k;if(decline){p.close();return false;}queued=p;held=b;return true;}static void qualified(String k,long c,long g,int v){records.put(k,new Record(c,g));admissions++;}static void rejectExact(String k){exact++;negatives.add(k);}static void rejectSpeed(String k){records.remove(k);slow++;}
 static void run(){Probe p=queued;queued=null;background=true;try{p.run(()->cancel);}finally{background=false;p.close();held=0;}}
}
final class QualityPipeline1932 {
 static int cpu,normal,resident,calls,expectSource=0xff123456,expectHalf=7,cancelAt=-1;static boolean mismatch,unavailable,crash,slow;static int[] lastHalf;static Bitmap snapshot;
 static Bitmap strongFinishOwned1976(Bitmap source,QualityPixels1932.Plan plan,int level,boolean shadows,int[] half,int rotation,int w,int h,QualityPixels1932.Plan out,boolean oracle){
  int copies=Bitmap.copies;try {Bitmap b=strongFinish1976(source,plan,level,shadows,half,rotation,w,h,out,false,oracle);if(Bitmap.copies!=copies+1)throw new AssertionError("only private output allocation in ordinary route");return b;}finally{source.recycle();}}
 static Bitmap strongFinish1976(Bitmap source,QualityPixels1932.Plan plan,int level,boolean shadows,int[] half,int rotation,int w,int h,QualityPixels1932.Plan out,boolean gpu,boolean oracle){
  calls++;if(source.pixels[0]!=expectSource||half[0]!=expectHalf||plan.policyCache!=null||out.policyCache!=null)throw new AssertionError("detached immutable source/half/plan");lastHalf=half;snapshot=source;
  if(oracle){cpu++;if(!GpuResident1976.cpuOracle())throw new AssertionError("CPU oracle mode missing");}
  else if(gpu)resident++;else {normal++;if(!GpuResident1976.benchmarking())throw new AssertionError("existing GPU baseline mode missing");}
  if(GpuQualification1961.background()&&GpuResident1976.cancellation()==null)throw new AssertionError("worker cancellation must be captured by Stage");
  if(calls==cancelAt)GpuQualification1961.cancel=true;
  if(gpu&&crash)throw new IllegalStateException("GPU lost");if(gpu&&unavailable)return null;
  try{Thread.sleep(gpu?(slow?30:1):oracle?1:15);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new java.util.concurrent.CancellationException();}
  Bitmap result=source.copy(Bitmap.Config.ARGB_8888,true);for(int i=0;i<result.pixels.length;i++)result.pixels[i]^=0x00010101;if(gpu&&mismatch)result.pixels[result.pixels.length-1]^=1;return result;
 }
}
'''
HARNESS=r'''package com.hiro.ulike;
import android.graphics.Bitmap;import java.util.*;
public class Resident1976Test {
 static int checks;static void check(boolean c,String s){checks++;if(!c)throw new AssertionError(s);}
 static Bitmap image(){return new Bitmap(4080,3060);}static QualityPixels1932.Plan plan(){return new QualityPixels1932.Plan();}
 static Bitmap invoke(Bitmap b,int[] h){return GpuResident1976.finish(b,plan(),4,true,h,90,4284,5712,plan(),new ProcessingTiming1947.Trace());}
 static void reset(){GpuChain1961.baseline="1:100:50";GpuQualification1961.records.clear();GpuQualification1961.negatives.clear();GpuQualification1961.cancel=false;GpuQualification1961.decline=false;GpuQualification1961.exact=GpuQualification1961.slow=GpuQualification1961.admissions=0;QualityPipeline1932.cpu=QualityPipeline1932.normal=QualityPipeline1932.resident=QualityPipeline1932.calls=0;QualityPipeline1932.cancelAt=-1;QualityPipeline1932.mismatch=QualityPipeline1932.unavailable=QualityPipeline1932.crash=QualityPipeline1932.slow=false;QualityPipeline1932.expectSource=0xff123456;QualityPipeline1932.expectHalf=7;}
 public static void main(String[] args){
 reset();Bitmap b=image();int[] half={7,8};check(invoke(b,half)==null,"uncertified route withheld");check(GpuQualification1961.held>49L*1024*1024-2L*1024*1024&&GpuQualification1961.held<96L*1024*1024,"24.5MP output snapshot admitted by actual source/mask bytes");check(GpuNoise1960.budget<96L*1024*1024,"snapshot reservation excludes future output phase");
 b.pixels[0]=0xff222222;half[0]=99;GpuQualification1961.run();check(QualityPipeline1932.cpu==2&&QualityPipeline1932.normal==2&&QualityPipeline1932.resident==2,"two complete CPU references, ordinary routes, resident routes");check(GpuQualification1961.admissions==1&&GpuQualification1961.records.size()==1,"exact faster complete route admitted");check(GpuQualification1961.held==0&&QualityPipeline1932.snapshot.isRecycled(),"idle snapshot released after trials");check(!GpuResident1976.cpuOracle()&&!GpuResident1976.benchmarking()&&GpuResident1976.cancellation()==null,"all inherited modes cleared");
 b.pixels[0]=0xff123456;half[0]=7;Bitmap result=invoke(b,half);check(result!=null&&ProcessingTiming1947.route==4,"qualified route consumed");check(b.pixels[0]==0xff123456&&!b.isRecycled(),"foreground original preserved");result.recycle();QualityPipeline1932.crash=true;check(invoke(b,half)==null,"lost GPU returns to original Strong fallback");check(b.pixels[0]==0xff123456,"failed resident source untouched");
 reset();b=image();QualityPipeline1932.mismatch=true;invoke(b,new int[]{7});GpuQualification1961.run();check(GpuQualification1961.exact==1&&GpuQualification1961.records.isEmpty(),"one actual last-pixel mismatch rejects complete route");check(QualityPipeline1932.cpu==1,"exact mismatch stops trials");
 reset();b=image();QualityPipeline1932.unavailable=true;invoke(b,new int[]{7});GpuQualification1961.run();check(GpuQualification1961.exact==0&&GpuQualification1961.records.isEmpty(),"unavailable candidate never becomes exact negative");
 reset();b=image();QualityPipeline1932.slow=true;invoke(b,new int[]{7});GpuQualification1961.run();check(GpuQualification1961.slow==1&&GpuQualification1961.exact==0&&GpuQualification1961.records.isEmpty(),"slower exact resident route not selected");check(QualityPipeline1932.cpu==2,"speed result includes two full trials");
 reset();b=image();QualityPipeline1932.cancelAt=2;invoke(b,new int[]{7});GpuQualification1961.run();check(GpuQualification1961.records.isEmpty()&&QualityPipeline1932.resident==0,"cancel before resident launch");check(GpuQualification1961.held==0&&GpuResident1976.cancellation()==null,"cancel snapshot/mode release");
 reset();b=image();b.wide=true;check(invoke(b,new int[]{7})==null&&GpuQualification1961.queued==null,"wide color excluded before copy");b.wide=false;b.hdr=true;check(invoke(b,new int[]{7})==null&&GpuQualification1961.queued==null,"gainmap excluded before copy");b.hdr=false;b.pixels[0]=0;invoke(b,new int[]{7});GpuQualification1961.run();check(QualityPipeline1932.calls==0&&GpuQualification1961.exact==0,"nonopaque snapshot excluded without poisoning certificate");
 reset();b=image();QualityPixels1932.Plan scoped=plan();scoped.policyCache=new Object();int beforeCopy=Bitmap.copies;
 check(GpuResident1976.finish(b,scoped,4,true,new int[]{7},90,4284,5712,plan(),new ProcessingTiming1947.Trace())==null&&Bitmap.copies==beforeCopy,"active source policy cache rejected without changing semantics");
 check(GpuResident1976.finish(b,plan(),4,true,new int[]{7},90,4284,5712,scoped,new ProcessingTiming1947.Trace())==null&&Bitmap.copies==beforeCopy,"active output policy cache rejected without changing semantics");
 scoped=plan();scoped.smoothedRegions=new QualityPixels1932.SmoothMask(){};
 check(GpuResident1976.finish(b,scoped,4,true,new int[]{7},90,4284,5712,plan(),new ProcessingTiming1947.Trace())==null&&Bitmap.copies==beforeCopy,"active source smoothing policy rejected");
 check(GpuResident1976.finish(b,plan(),4,true,new int[]{7},90,4284,5712,scoped,new ProcessingTiming1947.Trace())==null&&Bitmap.copies==beforeCopy,"active output smoothing policy rejected");
 reset();b=image();GpuChain1961.baseline=null;beforeCopy=Bitmap.copies;check(invoke(b,new int[]{7})==null&&Bitmap.copies==beforeCopy&&GpuQualification1961.queued==null,"resident queue waits for exact existing whole-finish route certificate");
 reset();b=image();invoke(b,new int[]{7});GpuChain1961.baseline="2:100:30";GpuQualification1961.run();check(QualityPipeline1932.calls==0&&GpuQualification1961.admissions==0,"changed faster finish baseline invalidates queued benchmark");
 reset();b=image();GpuQualification1961.decline=true;int recycled=Bitmap.recycles;invoke(b,new int[]{7});check(Bitmap.recycles==recycled+1&&GpuQualification1961.queued==null,"queue decline transfers and disposes snapshot once");
 System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"resident_two_whole_exact_trials_verified\":true,\"resident_original_route_timing_gate_verified\":true,\"resident_snapshot_cancel_ownership_verified\":true,\"physical_android_tested\":false}");
 }
}'''

def test(source,work,jdk=None,ndk=None):
 source=Path(source);work=Path(work)/'resident1976';work.mkdir(parents=True,exist_ok=True)
 java=Path(jdk)/'bin/java' if jdk else Path(shutil.which('java'));javac=java.with_name('javac')
 files={'android/graphics/Bitmap.java':BITMAP,'com/hiro/ulike/Stubs.java':STUBS,'com/hiro/ulike/Resident1976Test.java':HARNESS}
 for name,data in files.items():
  p=work/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(data)
 classes=work/'classes';classes.mkdir(exist_ok=True)
 compiled=subprocess.run([str(javac),'-source','8','-target','8','-Xlint:-options','-d',str(classes),str(source/'GpuResident1976.java'),*[str(work/n) for n in files]],capture_output=True,text=True)
 (work/'compile.log').write_text(compiled.stdout+compiled.stderr)
 if compiled.returncode:raise AssertionError(compiled.stderr)
 run=subprocess.run([str(java),'-cp',str(classes),'com.hiro.ulike.Resident1976Test'],capture_output=True,text=True,timeout=30)
 (work/'run.log').write_text(run.stdout+run.stderr)
 if run.returncode:raise AssertionError(run.stdout+run.stderr)
 result=json.loads(next(x[7:] for x in run.stdout.splitlines() if x.startswith('RESULT ')))
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 import argparse
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
