package com.hiro.ulike;
import android.graphics.Bitmap;import java.util.*;
final class SpatialNoise1934 {final int width,height,columns=2,rows=3;SpatialNoise1934(int w,int h){width=w;height=h;}}
final class FaceRegions1934 {static final class Mask implements QualityPixels1932.RegionMask {private final byte[] skin,detail;Mask(){this(new byte[100],new byte[100]);}Mask(byte[] a,byte[] b){skin=a;detail=b;}Mask resample(){return new Mask(skin,detail);}long retainedBytes1976(){return 500;}public int skinQ8(int x,int y){return 0;}public int detailQ8(int x,int y){return 0;}}}
final class QualityPixels1932 {
 interface RegionMask {int skinQ8(int x,int y);int detailQ8(int x,int y);}interface SmoothMask {}
 static final class Plan {int beautyQ8=128,shadowBudgetQ8=160,sharpGainQ8=100,sharpFloorQ8=7,sharpLimit=30;boolean texturePriority=true,haloSuppression=true;float sourceSigma=2.125f,outputScale=1;
 RegionMask faceRegions;SmoothMask smoothedRegions;SpatialNoise1934 localNoise;private Object policyCache;Plan cache(){policyCache=new Object();return this;}}
}
final class ProcessingTiming1947 {static long epoch=1;static int terminalCalls;static boolean sinkFailure;static long captureEpoch1953(){return epoch;}static final class Trace {}static Trace traceFor(Object o){return null;}static void backend1982(Trace t,int p,int b,int u,long n){}static void detailDefaultRoute1982(Trace t){}static void residentExit1982(Trace t,long b,int r){if(sinkFailure)throw new AssertionError("optional sink failed");if(t!=null){terminalCalls++;retained=b;reason=r;}}static int route,reason;static boolean identity;static long retained;static int halfLength;
 static void detailRoute1976(Trace t,int r){route=r;}
 static void resident1978(Trace t,int sw,int sh,int ow,int oh,int rotation,boolean i,int h,long b,int r){if(t!=null){reason=r;identity=i;retained=b;halfLength=h;}}}
final class GpuNoise1960 {static final int FINISH1961=1,GEOMETRY=2,ANALYSIS1961=3;static boolean busy,fits=true;static int unsupported,supportThrows;static final List<Integer> capabilities=new ArrayList<Integer>();static long budget;static boolean sessionBusy(){return busy;}static boolean supports(int s){capabilities.add(s);if(supportThrows!=0)throw new IllegalStateException("injected capability");return s!=unsupported;}static String fingerprint(){return "resident78-test-device";}static boolean workspaceFits(long b){budget=b;return fits;}}
final class GpuChain1961 {static boolean preflightFailure;static int closes;static final class Preflight1981 {long deadline1981;void close(){closes++;}}static Preflight1981 preflightResident1981(Bitmap b,int r,int w,int h,int half){return preflightFailure?null:new Preflight1981();}static String baseline="legacy:100:50";static boolean residentIdentity;static String residentBaseline1978(Bitmap b,int r,int w,int h,QualityPixels1932.Plan p,boolean identity){residentIdentity=identity;return baseline;}
 static boolean eligibleResident1976(Bitmap b,int r,int w,int h,QualityPixels1932.Plan p){return !b.wide&&!b.hdr&&(r==0||r==90||r==180||r==270)&&w>1&&h>1&&(p.faceRegions==null||p.faceRegions instanceof FaceRegions1934.Mask);}
 static boolean opaqueResident1976(Bitmap b){for(int p:b.pixels)if((p>>>24)!=255)return false;return true;}}
final class GpuQualification1961 {
 interface Cancellation {boolean cancelled();}interface Probe {void run(Cancellation c);void close();}static final class Record {final long cpuNanos,gpuNanos;Record(long c,long g){cpuNanos=c;gpuNanos=g;}}
 static final Map<String,Record> records=new HashMap<String,Record>();static final Set<String> negatives=new HashSet<String>();static Probe queued;static String key;static boolean background,cancel,decline;static int exact,slow,admissions,checks,cancelCheck=-1;static long held;
 static boolean background(){return background;}static boolean exactRejected(String k){return negatives.contains(k);}static Record restore(String k){return records.get(k);}static int queueCalls,declineAt,changeEpochAt;static boolean canQueue(String k,long b){queueCalls++;if(queueCalls==changeEpochAt)ProcessingTiming1947.epoch++;return queueCalls!=declineAt&&queued==null&&!negatives.contains(k)&&b>0&&b<=96L*1024*1024;}
 static boolean schedule(String k,long b,Probe p){key=k;if(decline){p.close();return false;}queued=p;held=b;return true;}static void qualified(String k,long c,long g,int v){records.put(k,new Record(c,g));admissions++;}static void rejectExact(String k){exact++;negatives.add(k);}static void rejectSpeed(String k){records.remove(k);slow++;}
 static void run(){Probe p=queued;queued=null;background=true;try{p.run(new Cancellation(){public boolean cancelled(){return cancel||++checks==cancelCheck;}});}finally{background=false;p.close();held=0;}}
 static void drop(){if(queued!=null){queued.close();queued=null;}held=0;}
}
final class QualityPipeline1932 {static int fault1982;
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
  if(gpu&&fault1982==2)throw new UnsatisfiedLinkError("injected GPU link");if(gpu&&fault1982==3)throw new OutOfMemoryError("injected GPU memory");if(gpu&&fault1982==4)throw new java.util.concurrent.CancellationException("injected GPU cancellation");if(gpu&&fault1982==5)throw new AssertionError("injected fatal error");if(gpu&&crash)throw new IllegalStateException("GPU lost");if(gpu&&unavailable)return null;
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

final class CpuExact1978 {static boolean background(){return false;}}
