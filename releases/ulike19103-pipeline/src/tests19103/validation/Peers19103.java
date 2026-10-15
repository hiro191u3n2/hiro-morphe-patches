package com.hiro.ulike;
import android.graphics.Bitmap;
/** Compile-only Android/dispatch peers. Unused pixel APIs fail loudly; this
 * suite executes production proof comparison, rejection and journal code. */
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
final class WholeRoute1953 {static long retainedBytes(){return 0;}}
final class SpeedWorkers1935 {static void run(Runnable[] tasks){throw new AssertionError("unused pixel workers");}static int maxWorkers(){return 2;}static int availableWorkers1944(){return 2;}}
final class GpuNoise1960 {
 static final int FINISH1961=1,GEOMETRY=2,ANALYSIS1961=3;
 static boolean sessionBusy(){return false;}static String fingerprint(){return "controlled-validation19103";}
 static boolean workspaceFits(long b){throw new AssertionError("unused GPU memory API");}static boolean supports(int s){throw new AssertionError("unused GPU capability API");}
}
final class SpatialNoise1934 {int width,height,columns,rows;}
final class FaceRegions1934 {static final class Mask implements QualityPixels1932.RegionMask {
 private byte[] skin,detail;long retainedBytes1976(){return 0;}
}}
final class QualityPixels1932 {
 interface RegionMask {}static final class Plan {
 int beautyQ8,shadowBudgetQ8,sharpGainQ8,sharpFloorQ8,sharpLimit;boolean texturePriority,haloSuppression;
 float sourceSigma,outputScale;Object smoothedRegions,policyCache;RegionMask faceRegions;SpatialNoise1934 localNoise;
 }
}
final class GpuStartup1990 {static boolean coreDomainReady19102(){return true;}static boolean modelDomainReady19102(){return true;}}
final class GpuStrongDefault1990 {static int profile1994(){return 4;}}
final class GpuModelDefault1990 {static int variant1994(){return 0;}}
final class GpuChain1961 {
 static final class Preflight1981 {long deadline1981;void close(){}}
 static Preflight1981 preflightResident1981(Bitmap b,int r,int w,int h,int length){throw new AssertionError("unused preflight");}
 static boolean eligibleResident1976(Bitmap b,int r,int w,int h,QualityPixels1932.Plan p){throw new AssertionError("unused route");}
 static boolean opaqueResident1976(Bitmap b){throw new AssertionError("unused opaque");}
 static String residentBaseline1978(Bitmap b,int r,int w,int h,QualityPixels1932.Plan p,boolean identity){throw new AssertionError("unused baseline");}
}
final class QualityPipeline1932 {
 static Bitmap strongFinish1978(Bitmap s,QualityPixels1932.Plan p,int l,boolean sh,int[] half,int r,int w,int h,QualityPixels1932.Plan out,boolean gpu,boolean cpu,boolean identity){throw new AssertionError("unused kernel");}
 static Bitmap strongFinishOwned1978(Bitmap s,QualityPixels1932.Plan p,int l,boolean sh,int[] half,int r,int w,int h,QualityPixels1932.Plan out,boolean gpu,boolean identity){throw new AssertionError("unused kernel");}
 static void qualifyResidentBaseline1978(Bitmap s,QualityPixels1932.Plan p,int l,boolean sh,int[] half,int r,int w,int h,QualityPixels1932.Plan out,boolean identity,GpuQualification1961.Cancellation c){throw new AssertionError("unused qualification");}
 static int compareStrongFinish1978(Bitmap s,QualityPixels1932.Plan p,int l,boolean sh,int[] half,int r,int w,int h,QualityPixels1932.Plan out,boolean resident,boolean identity,Bitmap expected,GpuQualification1961.Cancellation c){throw new AssertionError("unused pipeline");}
}
final class GpuSnapshotBudget1981 {
 static final int RESIDENT=2;static void offerWhole(int family){throw new AssertionError("unused snapshot");}
 static final class Copy {boolean begin(){return false;}boolean current(){return false;}void close(){}}
 static final class Decision1985 {Copy copy;int reason,preferredFamily,activeFamily;long activeStarted,copyEpoch;}
 static Decision1985 tryCopy1985(long b,int family){throw new AssertionError("unused snapshot");}
}
final class PipelineDiagnostics1982 {static void residentExit(ProcessingTiming1947.Trace t,long b,int reason){}}
