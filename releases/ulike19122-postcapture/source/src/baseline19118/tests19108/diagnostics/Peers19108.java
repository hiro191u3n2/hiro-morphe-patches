package com.hiro.ulike;
import android.graphics.Bitmap;
/** Controlled dispatch/Android peers. Real resident producer, comparator,
 * queue and journal execute. No GPU timing or pixel algorithm claim is made. */
final class ProcessingTiming1947 {
 static final String VERSION="@VERSION@";static long epoch=1,photo=1;
 static long captureEpoch1953(){return epoch;}static long captureId1989(long e){return e==epoch?photo:0;}
 static final class Trace {}
 static void detailRoute1976(Trace t,int route){}
 static void resident1978(Trace t,int sw,int sh,int ow,int oh,int r,boolean identity,int half,long retained,int reason){}
 static void residentCopy1985(Trace t,int reason,int preferred,int active,long started,long epoch){}
 static void residentExit1983(Trace t,long retained,int terminal,int reason,long retry,int retries,int queued,int running,long held,long requested){}
}
final class SaveQueue1935 {static volatile boolean idle;static boolean idle1953(){return idle;}}
/** Controlled trial metadata for this memory/refusal fixture. Production
 * timing capture and its validity rules are exercised in the timing suite. */
final class ResidentTiming19111 {
 private static final ThreadLocal<Boolean> completed=new ThreadLocal<Boolean>();
 public static void dispatch(String operation,long[] args){
  if("begin".equals(operation)){completed.set(Boolean.FALSE);return;}
  if("stop".equals(operation)){completed.set(Boolean.valueOf(args!=null&&args.length>1&&args[1]==1));return;}
  if("publish".equals(operation)){
   if(Boolean.TRUE.equals(completed.get())){
    long[] sample=new long[29];sample[0]=1;sample[1]=1;sample[2]=1;sample[4]=1;
    sample[21]=4;sample[22]=256;sample[24]=16;sample[26]=-1;
    GpuQualification1961.residentTiming19111(sample);
   }
   completed.remove();
  }
 }
}
final class WholeRoute1953 {static long retainedBytes(){return 0;}}
final class SpeedWorkers1935 {static void run(Runnable[] tasks){throw new AssertionError("unused pixel workers");}static int maxWorkers(){return 2;}static int availableWorkers1944(){return 2;}}
final class GpuNoise1960 {
 static final int FINISH1961=1,GEOMETRY=2,ANALYSIS1961=3;
 static boolean sessionBusy(){return false;}static String fingerprint(){return "controlled-validation19103";}
 static boolean workspaceFits(long b){return true;}static boolean supports(int s){return true;}
}
final class SpatialNoise1934 {int width,height,columns,rows;}
final class FaceRegions1934 {static final class Mask implements QualityPixels1932.RegionMask {
 private byte[] skin,detail;long retainedBytes1976(){return 0;}
}}
final class QualityPixels1932 {
 interface RegionMask {}static final class Plan {
 int beautyQ8,shadowBudgetQ8,sharpGainQ8=100,sharpFloorQ8,sharpLimit;boolean texturePriority,haloSuppression;
 float sourceSigma,outputScale;Object smoothedRegions,policyCache;RegionMask faceRegions;SpatialNoise1934 localNoise;
 }
}
final class GpuStartup1990 {static boolean coreDomainReady19102(){return true;}static boolean modelDomainReady19102(){return true;}}
final class GpuStrongDefault1990 {static int profile1994(){return 4;}}
final class GpuModelDefault1990 {static int variant1994(){return 0;}}
final class GpuChain1961 {static int preferredResidentRows19112(){return 64;}static Preflight1981 preflightResident19112(Bitmap b,int r,int w,int h,int half,boolean comparison){return new Preflight1981();}
 // This peer represents the typed sentinel boundary; the real classifier is
 // executed by the independent chain budget suite.
 private static final class DeferredChain extends RuntimeException {final int reason;DeferredChain(int r){reason=r;}}
 static RuntimeException failure(int reason){return new DeferredChain(reason);}
 static boolean memoryRefusal19108(RuntimeException e){if(QualityPipeline1932.open)throw new AssertionError("GPU owner not closed before classification");return e instanceof DeferredChain&&((DeferredChain)e).reason==4;}
 static final class Preflight1981 {int finishRows19112=64;long deadline1981;void close(){}}
 static Preflight1981 preflightResident1981(Bitmap b,int r,int w,int h,int length){throw new AssertionError("unused preflight");}
 static boolean eligibleResident1976(Bitmap b,int r,int w,int h,QualityPixels1932.Plan p){return true;}
 static boolean opaqueResident1976(Bitmap b){return true;}
 static String residentBaseline1978(Bitmap b,int r,int w,int h,QualityPixels1932.Plan p,boolean identity){return "controlled-baseline";}
}
final class QualityPipeline1932 {
 static int mode,failAt=1,candidateRuns,comparisons;static boolean open;static Bitmap oracle;
 static long[] memory(){long[] v=new long[33];java.util.Arrays.fill(v,9,33,-1);v[0]=1;v[1]=234L*1048576;v[3]=1;v[5]=2;v[6]=1;v[7]=13000000;v[8]=3;v[12]=48L*1048576;v[15]=60L*1048576;v[17]=60L*1048576;v[19]=3L*1048576;v[20]=233L*1048576;v[24]=v[12];v[27]=0;v[29]=0;v[31]=v[19];v[32]=240L*1048576;return v;}
 static void fail(){
  long[] report=memory();GpuQualification1961.residentMemory19108(report);java.util.Arrays.fill(report,0);
  if(mode==1||mode==4)throw GpuChain1961.failure(4);
  if(mode==2)throw new IllegalStateException("DeferredChain memory_budget");
  if(mode==3)throw GpuChain1961.failure(3);
  if(mode==5){Thread.currentThread().interrupt();throw GpuChain1961.failure(4);}
 }
 static Bitmap strongFinish1978(Bitmap s,QualityPixels1932.Plan p,int l,boolean sh,int[] half,int r,int w,int h,QualityPixels1932.Plan out,boolean gpu,boolean cpu,boolean identity){
  if(cpu){oracle=new Bitmap(w,h);return oracle;}
  if(oracle!=null&&!oracle.isRecycled())throw new AssertionError("oracle retained during speed");
  if(gpu){candidateRuns++;open=true;try{if(mode==6){long[] report=memory();report[4]=1;GpuQualification1961.residentMemory19108(report);long wait=System.nanoTime();while(System.nanoTime()-wait<40000000L){}}else if(mode!=0&&mode!=4&&candidateRuns==failAt)fail();return new Bitmap(w,h);}finally{open=false;}}
  long start=System.nanoTime();while(System.nanoTime()-start<15000000L){}return new Bitmap(w,h);
 }
 static Bitmap strongFinishOwned1978(Bitmap s,QualityPixels1932.Plan p,int l,boolean sh,int[] half,int r,int w,int h,QualityPixels1932.Plan out,boolean gpu,boolean identity){try{return strongFinish1978(s,p,l,sh,half,r,w,h,out,false,false,identity);}finally{s.recycle();}}
 static void qualifyResidentBaseline1978(Bitmap s,QualityPixels1932.Plan p,int l,boolean sh,int[] half,int r,int w,int h,QualityPixels1932.Plan out,boolean identity,GpuQualification1961.Cancellation c){throw new AssertionError("unused prerequisite");}
 static int compareStrongFinish1978(Bitmap s,QualityPixels1932.Plan p,int l,boolean sh,int[] half,int r,int w,int h,QualityPixels1932.Plan out,boolean resident,boolean identity,Bitmap expected,GpuQualification1961.Cancellation c){
  comparisons++;open=true;try{if(resident&&mode==4)fail();ResidentProof1978 proof=new ResidentProof1978(expected,c);proof.rows(0,h,expected.pixels,0);return proof.result();}finally{open=false;}
 }
}
final class GpuSnapshotBudget1981 {
 static final int RESIDENT=2;static void offerWhole(int family){}
 static final class Copy {boolean begin(){return true;}boolean current(){return true;}void close(){}}
 static final class Decision1985 {Copy copy;int reason,preferredFamily,activeFamily;long activeStarted,copyEpoch;}
 static Decision1985 tryCopy1985(long b,int family){Decision1985 d=new Decision1985();d.copy=new Copy();return d;}
}
final class PipelineDiagnostics1982 {static void residentExit(ProcessingTiming1947.Trace t,long b,int reason){}}
