#!/usr/bin/env python3
"""Execute resident .78 ownership, memory, exact stream and ordering contracts.

Android/dispatch objects are controlled fixtures here. Actual pixel algorithms
and JNI are exercised separately by host_resident_pipeline1978 and chain1978.
"""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess

BITMAP=r'''package android.graphics;
import java.util.*;
public final class Bitmap {
 public enum Config {ARGB_8888,RGB_565}
 public static int copies,recycles,created;public static final List<Bitmap> all=new ArrayList<Bitmap>();
 public final int width,height;public final int[] pixels;public boolean wide,hdr,recycled;public int padding,slack,copySlack;private final Config config;
 public Bitmap(int w,int h){this(w,h,Config.ARGB_8888);}
 public Bitmap(int w,int h,Config c){if(w<1||h<1||(long)w*h>Integer.MAX_VALUE)throw new IllegalArgumentException();width=w;height=h;config=c;pixels=new int[w*h];Arrays.fill(pixels,0xff123456);created++;all.add(this);}
 public int getWidth(){check();return width;}public int getHeight(){check();return height;}public Config getConfig(){check();return config;}
 public boolean isRecycled(){return recycled;}private void check(){if(recycled)throw new IllegalStateException("recycled");}
 public int getRowBytes(){return width*4+padding;}public int getAllocationByteCount(){return getRowBytes()*height+slack;}
 public Bitmap copy(Config c,boolean mutable){check();copies++;Bitmap b=new Bitmap(width,height,c);System.arraycopy(pixels,0,b.pixels,0,pixels.length);b.wide=wide;b.hdr=hdr;b.slack=copySlack;return b;}
 public void recycle(){if(!recycled){recycled=true;recycles++;}}
 public void getPixels(int[] out,int offset,int stride,int x,int y,int w,int h){check();if(x<0||y<0||x+w>width||y+h>height||stride<w)throw new IllegalArgumentException();for(int r=0;r<h;r++)System.arraycopy(pixels,(y+r)*width+x,out,offset+r*stride,w);}
}
'''
STUBS=r'''package com.hiro.ulike;
import android.graphics.Bitmap;import java.util.*;
final class SpatialNoise1934 {final int width,height,columns=2,rows=3;SpatialNoise1934(int w,int h){width=w;height=h;}}
final class FaceRegions1934 {static final class Mask implements QualityPixels1932.RegionMask {private final byte[] skin,detail;Mask(){this(new byte[100],new byte[100]);}Mask(byte[] a,byte[] b){skin=a;detail=b;}Mask resample(){return new Mask(skin,detail);}long retainedBytes1976(){return 500;}public int skinQ8(int x,int y){return 0;}public int detailQ8(int x,int y){return 0;}}}
final class QualityPixels1932 {
 interface RegionMask {int skinQ8(int x,int y);int detailQ8(int x,int y);}interface SmoothMask {}
 static final class Plan {int beautyQ8=128,shadowBudgetQ8=160,sharpGainQ8=100,sharpFloorQ8=7,sharpLimit=30;boolean texturePriority=true,haloSuppression=true;float sourceSigma=2.125f,outputScale=1;
 RegionMask faceRegions;SmoothMask smoothedRegions;SpatialNoise1934 localNoise;private Object policyCache;Plan cache(){policyCache=new Object();return this;}}
}
final class ProcessingTiming1947 {static final class Trace {}static int route,reason;static boolean identity;static long retained;static int halfLength;
 static void detailRoute1976(Trace t,int r){route=r;}
 static void resident1978(Trace t,int sw,int sh,int ow,int oh,int rotation,boolean i,int h,long b,int r){if(t!=null){reason=r;identity=i;retained=b;halfLength=h;}}}
final class GpuNoise1960 {static final int FINISH1961=1,GEOMETRY=2,ANALYSIS1961=3;static boolean busy,fits=true;static long budget;static boolean sessionBusy(){return busy;}static boolean supports(int s){return true;}static String fingerprint(){return "resident78-test-device";}static boolean workspaceFits(long b){budget=b;return fits;}}
final class GpuChain1961 {static String baseline="legacy:100:50";static boolean residentIdentity;static String residentBaseline1978(Bitmap b,int r,int w,int h,QualityPixels1932.Plan p,boolean identity){residentIdentity=identity;return baseline;}
 static boolean eligibleResident1976(Bitmap b,int r,int w,int h,QualityPixels1932.Plan p){return !b.wide&&!b.hdr&&(r==0||r==90||r==180||r==270)&&w>1&&h>1&&(p.faceRegions==null||p.faceRegions instanceof FaceRegions1934.Mask);}
 static boolean opaqueResident1976(Bitmap b){for(int p:b.pixels)if((p>>>24)!=255)return false;return true;}}
final class GpuQualification1961 {
 interface Cancellation {boolean cancelled();}interface Probe {void run(Cancellation c);void close();}static final class Record {final long cpuNanos,gpuNanos;Record(long c,long g){cpuNanos=c;gpuNanos=g;}}
 static final Map<String,Record> records=new HashMap<String,Record>();static final Set<String> negatives=new HashSet<String>();static Probe queued;static String key;static boolean background,cancel,decline;static int exact,slow,admissions,checks,cancelCheck=-1;static long held;
 static boolean background(){return background;}static boolean exactRejected(String k){return negatives.contains(k);}static Record restore(String k){return records.get(k);}static boolean canQueue(String k,long b){return queued==null&&!negatives.contains(k)&&b>0&&b<=96L*1024*1024;}
 static boolean schedule(String k,long b,Probe p){key=k;if(decline){p.close();return false;}queued=p;held=b;return true;}static void qualified(String k,long c,long g,int v){records.put(k,new Record(c,g));admissions++;}static void rejectExact(String k){exact++;negatives.add(k);}static void rejectSpeed(String k){records.remove(k);slow++;}
 static void run(){Probe p=queued;queued=null;background=true;try{p.run(new Cancellation(){public boolean cancelled(){return cancel||++checks==cancelCheck;}});}finally{background=false;p.close();held=0;}}
 static void drop(){if(queued!=null){queued.close();queued=null;}held=0;}
}
final class QualityPipeline1932 {
 static int cpu,normal,resident,normalCompare,residentCompare,calls,dependency,cancelAt=-1;static boolean mismatch,unavailable,crash,slow,dependencyUnavailable,lastIdentity,changingBaseline;static int[] lastHalf;static Bitmap snapshot,oracleLive;
 static final List<String> order=new ArrayList<String>();
 static void input(Bitmap s,int[] half,boolean oracle,boolean identity){calls++;lastIdentity=identity;snapshot=s;
  if(GpuQualification1961.background()){
   if(s.pixels[0]!=0xff123456)throw new AssertionError("queued source aliases foreground");
   if(oracle){if(half!=null||!GpuResident1976.cpuOracle())throw new AssertionError("independent CPU model must reread source");}
   else {if(half==null||half[0]!=0xff123456)throw new AssertionError("exact half regenerated from immutable source");lastHalf=half;}
   if(GpuResident1976.cancellation()==null)throw new AssertionError("worker token absent");
  }
  if(calls==cancelAt)GpuQualification1961.cancel=true;
 }
 static int[] finalPixels(Bitmap source,int width,int height){int[] p=new int[width*height];for(int i=0;i<p.length;i++)p[i]=0xff000000|((source.pixels[0]+i*257)^0x00010101)&0x00ffffff;return p;}
 static void pause(int ms){try{Thread.sleep(ms);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new java.util.concurrent.CancellationException();}}
 static Bitmap strongFinish1978(Bitmap source,QualityPixels1932.Plan plan,int level,boolean shadows,int[] half,int rotation,int w,int h,QualityPixels1932.Plan out,boolean gpu,boolean oracle,boolean identity){
  input(source,half,oracle,identity);order.add(oracle?"cpu":gpu?"resident_time":"ordinary_time");
  if(oracle)cpu++;else if(gpu)resident++;else normal++;
  if(!oracle&&oracleLive!=null&&!oracleLive.isRecycled())throw new AssertionError("CPU reference retained during real destination timing");
  if(gpu&&crash)throw new IllegalStateException("GPU lost");if(gpu&&unavailable)return null;
  pause(gpu?(slow?30:1):oracle?1:18);Bitmap result=new Bitmap(w,h);System.arraycopy(finalPixels(source,w,h),0,result.pixels,0,w*h);if(oracle)oracleLive=result;return result;
 }
 static Bitmap strongFinishOwned1978(Bitmap source,QualityPixels1932.Plan plan,int level,boolean shadows,int[] half,int rotation,int w,int h,QualityPixels1932.Plan out,boolean cpu,boolean identity){try{return strongFinish1978(source,plan,level,shadows,half,rotation,w,h,out,false,cpu,identity);}finally{source.recycle();}}
 static int compareStrongFinish1978(Bitmap source,QualityPixels1932.Plan plan,int level,boolean shadows,int[] half,int rotation,int w,int h,QualityPixels1932.Plan out,boolean gpu,boolean identity,Bitmap expected,GpuQualification1961.Cancellation cancellation){
  input(source,half,false,identity);order.add(gpu?"resident_compare":"ordinary_compare");if(gpu)residentCompare++;else normalCompare++;
  if(!GpuResident1976.benchmarking())throw new AssertionError("comparison benchmark mode absent");
  if(gpu&&unavailable)return ResidentProof1978.UNAVAILABLE;
  if(GpuQualification1961.cancel)return ResidentProof1978.UNAVAILABLE;
  int before=Bitmap.created;ResidentProof1978 proof=new ResidentProof1978(expected,cancellation);int[] pixels=finalPixels(source,w,h);
  if(gpu&&mismatch)pixels[pixels.length-1]^=1;
  for(int y=0;y<h;y+=64)if(!proof.rows(y,Math.min(64,h-y),pixels,y*w))break;
  if(Bitmap.created!=before)throw new AssertionError("comparison allocated candidate Bitmap");
  if(changingBaseline)GpuChain1961.baseline="changed:100:40";
  return proof.result();
 }
 static void qualifyResidentBaseline1978(Bitmap source,QualityPixels1932.Plan plan,int level,boolean shadows,int[] half,int rotation,int w,int h,QualityPixels1932.Plan output,boolean identity,GpuQualification1961.Cancellation cancellation){
  dependency++;order.add("dependency");if(!GpuResident1976.cpuOracle())throw new AssertionError("dependency Strong CPU oracle absent");if(GpuQualification1961.queued!=null)throw new AssertionError("recursive dependency queue");if(!dependencyUnavailable)GpuChain1961.baseline="legacy:100:50";
 }
}
'''
HARNESS=r'''package com.hiro.ulike;
import android.graphics.Bitmap;import java.util.*;
public final class Resident1978Test {
 static int assertions;static long pixels;static final GpuQualification1961.Cancellation LIVE=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static Bitmap image(){return new Bitmap(65,131);}static QualityPixels1932.Plan plan(){return new QualityPixels1932.Plan();}
 static Bitmap invoke(Bitmap b,int[] h){return GpuResident1976.finish(b,plan(),4,true,h,90,71,137,plan(),new ProcessingTiming1947.Trace());}
 static void reset(){GpuQualification1961.drop();GpuQualification1961.records.clear();GpuQualification1961.negatives.clear();GpuQualification1961.background=GpuQualification1961.cancel=GpuQualification1961.decline=false;GpuQualification1961.exact=GpuQualification1961.slow=GpuQualification1961.admissions=GpuQualification1961.checks=0;GpuQualification1961.cancelCheck=-1;
 GpuChain1961.baseline="legacy:100:50";GpuNoise1960.busy=false;GpuNoise1960.fits=true;QualityPipeline1932.cpu=QualityPipeline1932.normal=QualityPipeline1932.resident=QualityPipeline1932.normalCompare=QualityPipeline1932.residentCompare=QualityPipeline1932.calls=QualityPipeline1932.dependency=0;QualityPipeline1932.cancelAt=-1;QualityPipeline1932.mismatch=QualityPipeline1932.unavailable=QualityPipeline1932.crash=QualityPipeline1932.slow=QualityPipeline1932.dependencyUnavailable=QualityPipeline1932.changingBaseline=false;QualityPipeline1932.oracleLive=null;QualityPipeline1932.order.clear();Thread.interrupted();}
 static void modes(){check(!GpuResident1976.cpuOracle()&&!GpuResident1976.benchmarking()&&GpuResident1976.cancellation()==null,"all thread modes cleared");}
 static void admission(){reset();Bitmap source=image();int[] half={7,8};int before=Bitmap.copies;
  check(invoke(source,half)==null,"unknown resident stays on existing foreground route");check(Bitmap.copies==before+1&&GpuQualification1961.queued!=null,"exactly one immutable source queued");check(ProcessingTiming1947.reason==2&&ProcessingTiming1947.halfLength==2,"foreground admission diagnostic");
  source.pixels[0]=0xff222222;half[0]=99;GpuQualification1961.run();
  check(QualityPipeline1932.cpu==2&&QualityPipeline1932.normalCompare==2&&QualityPipeline1932.residentCompare==2,"two CPU oracles and two full comparisons for each candidate");
  check(QualityPipeline1932.normal==2&&QualityPipeline1932.resident==2,"two real output-producing timings for each path");
  check(GpuQualification1961.admissions==1&&GpuQualification1961.records.size()==1,"faster exact complete route admitted");check(QualityPipeline1932.lastHalf!=half&&QualityPipeline1932.lastHalf.length==33*66,"caller half never held by queued proof");check(GpuQualification1961.held==0&&QualityPipeline1932.snapshot.isRecycled(),"snapshot released");modes();
  source.pixels[0]=0xff123456;Bitmap result=invoke(source,half);check(result!=null&&ProcessingTiming1947.route==4&&ProcessingTiming1947.reason==1,"qualified foreground uses resident");result.recycle();check(!source.isRecycled()&&source.pixels[0]==0xff123456,"foreground input preserved");QualityPipeline1932.crash=true;check(invoke(source,half)==null,"failed GPU falls back safely");source.recycle();
  reset();source=image();QualityPipeline1932.mismatch=true;invoke(source,half);GpuQualification1961.run();check(GpuQualification1961.exact==1&&GpuQualification1961.admissions==0&&QualityPipeline1932.cpu==1,"last pixel mismatch rejects before any admission");modes();source.recycle();
  reset();source=image();QualityPipeline1932.unavailable=true;invoke(source,half);GpuQualification1961.run();check(GpuQualification1961.exact==0&&GpuQualification1961.admissions==0,"unavailable is not exact evidence");source.recycle();
  reset();source=image();QualityPipeline1932.slow=true;invoke(source,half);GpuQualification1961.run();check(GpuQualification1961.slow==1&&GpuQualification1961.exact==0&&GpuQualification1961.admissions==0&&QualityPipeline1932.cpu==2,"slow exact route completes both proofs but remains disabled");source.recycle();
  reset();source=image();QualityPipeline1932.cancelAt=2;invoke(source,half);GpuQualification1961.run();check(GpuQualification1961.admissions==0&&QualityPipeline1932.residentCompare==0&&GpuQualification1961.held==0,"cancel before resident launch drains snapshot");modes();source.recycle();
  reset();source=image();GpuChain1961.baseline=null;invoke(source,half);check(GpuQualification1961.queued!=null&&GpuQualification1961.key.endsWith(":finish-dependency"),"missing dependency queues the same one source");GpuQualification1961.run();check(QualityPipeline1932.dependency==1&&"dependency".equals(QualityPipeline1932.order.get(0)),"finish prerequisite before resident comparisons");check(GpuQualification1961.admissions==1&&GpuQualification1961.records.keySet().iterator().next().contains(":legacy:100:50"),"child exact record uses newly established dependency key");source.recycle();
  reset();source=image();GpuChain1961.baseline=null;QualityPipeline1932.dependencyUnavailable=true;invoke(source,half);GpuQualification1961.run();check(QualityPipeline1932.cpu==0&&GpuQualification1961.admissions==0&&GpuQualification1961.slow==1,"missing dependency speed proof cannot admit resident");source.recycle();
  reset();source=image();invoke(source,half);GpuChain1961.baseline="changed";GpuQualification1961.run();check(QualityPipeline1932.calls==0&&GpuQualification1961.admissions==0,"changed prerequisite cancels before image work");source.recycle();
  reset();source=image();QualityPipeline1932.changingBaseline=true;invoke(source,half);GpuQualification1961.run();check(GpuQualification1961.admissions==0,"dependency changes between trials invalidate admission");source.recycle();
  reset();source=image();GpuQualification1961.decline=true;before=Bitmap.recycles;invoke(source,half);check(Bitmap.recycles==before+1&&GpuQualification1961.queued==null,"schedule decline disposes owned snapshot exactly once");source.recycle();
 }
 static void safePlans(){reset();Bitmap b=image();int[] h={1};int before=Bitmap.copies;QualityPixels1932.Plan p=plan().cache();
  check(GpuResident1976.finish(b,p,4,true,h,90,71,137,plan(),null)==null&&Bitmap.copies==before,"active source cache untouched");check(GpuResident1976.finish(b,plan(),4,true,h,90,71,137,p,null)==null&&Bitmap.copies==before,"active output cache untouched");
  p=plan();p.smoothedRegions=new QualityPixels1932.SmoothMask(){};check(GpuResident1976.finish(b,p,4,true,h,90,71,137,plan(),null)==null&&Bitmap.copies==before,"stateful source smoothing declined");check(GpuResident1976.finish(b,plan(),4,true,h,90,71,137,p,null)==null&&Bitmap.copies==before,"stateful output smoothing declined");
  final int[] calls={0};p=plan();p.faceRegions=new QualityPixels1932.RegionMask(){public int skinQ8(int x,int y){calls[0]++;return 1;}public int detailQ8(int x,int y){calls[0]++;return 1;}};
  check(GpuResident1976.finish(b,p,4,true,h,90,71,137,plan(),null)==null&&calls[0]==0&&Bitmap.copies==before,"unknown source mask is neither captured nor sampled");check(GpuResident1976.finish(b,plan(),4,true,h,90,71,137,p,null)==null&&calls[0]==0,"unknown output mask declined");
  b.wide=true;check(invoke(b,h)==null&&GpuQualification1961.queued==null,"wide colour declined");b.wide=false;b.hdr=true;check(invoke(b,h)==null&&GpuQualification1961.queued==null,"gainmap declined");b.hdr=false;b.pixels[0]=0;invoke(b,h);GpuQualification1961.run();check(QualityPipeline1932.calls==0&&GpuQualification1961.exact==0,"alpha snapshot declined without exact poison");
  reset();b.pixels[0]=0xff123456;check(GpuResident1976.finish1978(b,plan(),4,true,h,0,65,131,plan(),true,new ProcessingTiming1947.Trace())==null&&ProcessingTiming1947.identity,"identity joined route admitted explicitly");GpuQualification1961.drop();
  check(GpuResident1976.finish(b,plan(),4,true,h,0,65,131,plan(),new ProcessingTiming1947.Trace())==null&&!ProcessingTiming1947.identity,"reduce-first same dimensions retain post-moire semantics");GpuQualification1961.drop();before=Bitmap.copies;check(GpuResident1976.finish1978(b,plan(),4,true,h,90,65,131,plan(),true,null)==null&&Bitmap.copies==before,"identity must match physical geometry");
  b.recycle();check(GpuResident1976.finish1978(b,plan(),4,true,h,0,65,131,plan(),true,null)==null,"recycled input declined");check(GpuResident1976.finish1978(null,plan(),4,true,h,0,65,131,plan(),true,null)==null,"null input declined");
 }
 static void memory(){reset();Bitmap b=new Bitmap(4284,5712);QualityPixels1932.Plan p=plan(),out=plan();SpatialNoise1934 noise=new SpatialNoise1934(4284,5712);p.localNoise=out.localNoise=noise;FaceRegions1934.Mask mask=new FaceRegions1934.Mask();p.faceRegions=mask;out.faceRegions=mask.resample();
  long retained=ResidentProof1978.retainedBytes(b,p,out),old=4L*4284*5712+4L*2142*2856+65536;
  check(retained<=96L*1024*1024&&old>96L*1024*1024,"real 4284x5712 source with shared masks fits unchanged queue cap only without held half");
  check(retained==4L*4284*5712+65536+512+4*6+12L*(4284+5712)+192*2+32*2+200,"shared immutable noise and planes counted once");
  GpuResident1976.finish1978(b,p,4,true,new int[]{7},0,4284,5712,out,true,new ProcessingTiming1947.Trace());check(GpuQualification1961.queued!=null&&GpuQualification1961.held==retained&&ProcessingTiming1947.retained==retained,"maximum actual source dimensions admitted with exact retained bytes");GpuQualification1961.drop();b.recycle();
  b=new Bitmap(4096,6144);int before=Bitmap.copies;invoke(b,new int[]{1});check(GpuQualification1961.queued==null&&Bitmap.copies==before&&ProcessingTiming1947.reason==5,"snapshot over 96MiB declines before allocation");b.recycle();
  b=image();b.padding=16;b.slack=4096;check(ResidentProof1978.retainedBytes(b,plan(),plan())==4L*65*131+16L*131+4096+65536,"row alignment and actual allocation slack included");b.copySlack=96*1024*1024;before=Bitmap.recycles;invoke(b,new int[]{1});check(GpuQualification1961.queued==null&&Bitmap.recycles==before+1&&ProcessingTiming1947.reason==5,"unexpected copied allocation over cap released before queue");b.copySlack=0;
  GpuNoise1960.fits=false;before=Bitmap.copies;invoke(b,new int[]{1});check(Bitmap.copies==before&&GpuQualification1961.queued==null,"actual memory shortage also prevents source copy");b.recycle();GpuNoise1960.fits=true;
 }
 static void halfAndRows(){reset();for(int width:new int[]{1,2,3,31,65})for(int height:new int[]{1,2,3,64,131}){
  Bitmap b=new Bitmap(width,height);Random random=new Random(width*1000+height);for(int i=0;i<b.pixels.length;i++)b.pixels[i]=(i%17==0?0:0xff000000)|(random.nextInt()&0xffffff);
  int[] actual=ResidentProof1978.half(b,LIVE);int columns=(width+1)/2;check(actual.length==columns*((height+1)/2),"half shape including odd edge");
  for(int y=0;y<height;y+=2)for(int x=0;x<width;x+=2){int r=0,g=0,blue=0,n=0;boolean alpha=true;for(int yy=y;yy<Math.min(y+2,height);yy++)for(int xx=x;xx<Math.min(x+2,width);xx++){int value=b.pixels[yy*width+xx];alpha&=(value>>>24)==255;r+=(value>>>16)&255;g+=(value>>>8)&255;blue+=value&255;n++;}int expected=(alpha?0xff000000:0)|((r+n/2)/n<<16)|((g+n/2)/n<<8)|(blue+n/2)/n;check(actual[(y/2)*columns+x/2]==expected,"exact canonical 2x2 averaging");pixels++;}
  b.recycle();
 }
 Bitmap expected=image();int[] values=expected.pixels.clone();ResidentProof1978 comparison=new ResidentProof1978(expected,LIVE);check(comparison.rows(64,64,values,64*65)&&comparison.result()==ResidentProof1978.UNAVAILABLE,"partial proof cannot pass");check(comparison.rows(0,64,values,0)&&comparison.rows(128,3,values,128*65)&&comparison.result()==ResidentProof1978.EXACT,"all rows in two-bank completion order pass");check(comparison.pixelsCompared()==65L*131,"each final pixel compared exactly once");check(comparison.workspaceBytes()<4L*65*131,"bounded row workspace below complete image");
 boolean rejected=false;try{comparison.rows(0,1,values,0);}catch(IllegalStateException duplicate){rejected=true;}check(rejected,"duplicate band rejected");
 comparison=new ResidentProof1978(expected,LIVE);values[values.length-1]^=1;check(!comparison.rows(0,131,values,0)&&comparison.result()==ResidentProof1978.MISMATCH&&comparison.pixelsCompared()==values.length,"last pixel mismatch observed");
 comparison=new ResidentProof1978(expected,LIVE);rejected=false;try{comparison.rows(130,2,values,0);}catch(IllegalArgumentException bounds){rejected=true;}check(rejected,"out of bounds band rejected");
 GpuQualification1961.Cancellation stopped=new GpuQualification1961.Cancellation(){public boolean cancelled(){return true;}};comparison=new ResidentProof1978(expected,stopped);rejected=false;try{comparison.rows(0,1,values,0);}catch(java.util.concurrent.CancellationException cancel){rejected=true;}check(rejected,"stream comparison checks cancellation");rejected=false;try{ResidentProof1978.half(expected,stopped);}catch(java.util.concurrent.CancellationException cancel){rejected=true;}check(rejected,"half reconstruction checks cancellation");expected.recycle();
 }
 public static void main(String[] args){halfAndRows();admission();safePlans();memory();modes();check(GpuQualification1961.queued==null&&GpuQualification1961.held==0,"all queue ownership drained");System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"halfExactPixels\":"+pixels+",\"resident_two_whole_exact_trials_verified\":true,\"resident_original_route_timing_gate_verified\":true,\"resident_snapshot_cancel_ownership_verified\":true,\"resident1978_half_reconstruction_exact_verified\":true,\"resident1978_actual_resolution_admission_verified\":true,\"resident1978_complete_stream_comparison_verified\":true,\"resident1978_dependency_before_candidate_verified\":true,\"resident1978_identity_entry_semantics_verified\":true,\"resident1978_unknown_plan_safe_fallback_verified\":true,\"physical_android_tested\":false}");}
}
'''

def test(source,work,jdk=None,ndk=None):
    source=Path(source).resolve();work=Path(work).resolve()/'resident1978';work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('java')).parent.parent)
    files={'android/graphics/Bitmap.java':BITMAP,'com/hiro/ulike/Stubs.java':STUBS,'com/hiro/ulike/Resident1978Test.java':HARNESS}
    for name,body in files.items():
        path=work/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body)
    production=[source/'ResidentProof1978.java',source/'GpuResident1976.java'];pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in production}
    classes=work/'classes';classes.mkdir(exist_ok=True)
    def run(args,label):
        result=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=90)
        (work/(label+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise AssertionError(label+'\n'+result.stdout+result.stderr)
        return result.stdout
    run([jdk/'bin/javac','--release','8','-d',classes,*production,*[work/name for name in files]],'compile')
    output=run([jdk/'bin/java','-ea','-Xmx768m','-cp',classes,'com.hiro.ulike.Resident1978Test'],'run')
    report=json.loads(next(line[7:] for line in output.splitlines() if line.startswith('RESULT ')))
    if pins!={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in production}:raise AssertionError('resident sources changed during test')
    report.update(source_hashes=pins,device_speedup_verified=False,controlled_dispatch_fixture=True)
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',required=True);parser.add_argument('--work',required=True);parser.add_argument('--jdk');parser.add_argument('--ndk');args=parser.parse_args();print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),indent=2))
