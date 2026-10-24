package com.hiro.ulike;

import android.graphics.Bitmap;

/** Controlled proof/readiness transport; no pixel or native-performance claims. */
final class QualityPixels1932 {
    static final class Plan {
        int beautyQ8=128,shadowBudgetQ8=160,sharpGainQ8=100,sharpFloorQ8=7,sharpLimit=30;
        boolean texturePriority=true,haloSuppression=true;
        float sourceSigma=2.125f,outputScale=1;
        Object faceRegions,smoothedRegions,localNoise;
        private Object policyCache;
    }
}
final class GpuStartup1990 {
    static boolean coreDomainReady19102(){return true;}
    static boolean modelDomainReady19102(){return true;}
}
final class GpuStrongDefault1990 {static int profile1994(){return 4;}}
final class GpuModelDefault1990 {static int variant1994(){return 0;}}
final class GpuNoise1960 {
    static final int FINISH1961=1,GEOMETRY=2,ANALYSIS1961=3;
    static boolean sessionBusy(){return false;}
    static boolean supports(int kind){return true;}
    static String fingerprint(){return "profile112-controlled-device";}
    static boolean workspaceFits(long bytes){throw unexpected();}
    static AssertionError unexpected(){return new AssertionError("foreground proof fixture entered background path");}
}
final class GpuChain1961 {
    static int preferred,afterPreferred,planned,created,closed;
    static Preflight1981 active;
    static final class Preflight1981 {
        final int finishRows19112;long deadline1981;boolean done;
        Preflight1981(int rows){finishRows19112=rows;created++;}
        void close(){if(done)throw new AssertionError("plan closed twice");done=true;closed++;if(active==this)active=null;}
    }
    static int preferredResidentRows19112(){return preferred;}
    static Preflight1981 preflightResident1981(Bitmap b,int r,int w,int h,int half){
        if(active!=null)throw new AssertionError("stale plan");
        // Deterministic interleaving: wide readiness changes after the old
        // route check, while the producer calls geometry/preflight preparation.
        preferred=afterPreferred;active=new Preflight1981(planned);return active;
    }
    static Preflight1981 preflightResident19112(Bitmap b,int r,int w,int h,int half,boolean comparison){throw GpuNoise1960.unexpected();}
    static boolean eligibleResident1976(Bitmap b,int r,int w,int h,QualityPixels1932.Plan p){return true;}
    static boolean opaqueResident1976(Bitmap b){throw GpuNoise1960.unexpected();}
    static String residentBaseline1978(Bitmap b,int r,int w,int h,QualityPixels1932.Plan p,boolean identity){return "existing-exact-speed-proof";}
}
final class QualityPipeline1932 {
    static int calls,executedRows;
    static Bitmap strongFinish1978(Bitmap source,QualityPixels1932.Plan p,int level,boolean shadows,int[] half,
            int rotation,int width,int height,QualityPixels1932.Plan output,boolean resident,boolean cpu,boolean identity){
        if(!resident||cpu||GpuChain1961.active==null||GpuChain1961.active.done)throw new AssertionError("missing live resident plan");
        calls++;executedRows=GpuChain1961.active.finishRows19112;return new Bitmap(width,height);
    }
    static Bitmap strongFinishOwned1978(Bitmap s,QualityPixels1932.Plan p,int l,boolean sh,int[] half,int r,int w,int h,QualityPixels1932.Plan o,boolean cpu,boolean i){throw GpuNoise1960.unexpected();}
    static int compareStrongFinish1978(Bitmap s,QualityPixels1932.Plan p,int l,boolean sh,int[] half,int r,int w,int h,QualityPixels1932.Plan o,boolean gpu,boolean i,Bitmap expected,GpuQualification1961.Cancellation c){throw GpuNoise1960.unexpected();}
    static void qualifyResidentBaseline1978(Bitmap s,QualityPixels1932.Plan p,int l,boolean sh,int[] half,int r,int w,int h,QualityPixels1932.Plan o,boolean i,GpuQualification1961.Cancellation c){throw GpuNoise1960.unexpected();}
}
final class GpuQualification1961 {
    interface Cancellation {boolean cancelled();}
    interface Probe {void run(Cancellation c);void close();}
    static final class Record {final long cpuNanos=1000000000000L;}
    static int proofRows,restores,writes;static String restoredKey;
    static boolean background(){return false;}
    static boolean exactRejected(String key){return false;}
    static boolean comparableResidentPlans19116(){return true;}
    static Record restore(String key){
        restores++;restoredKey=key;
        if(!key.contains(";resident-finish="+proofRows+":"))throw new AssertionError("fixture proof belongs to other row profile");
        return new Record();
    }
    static String exactFailure1971(String key){throw GpuNoise1960.unexpected();}
    static void qualified(String key,long baseline,long candidate,int variant){writes++;throw GpuNoise1960.unexpected();}
    static void rejectExact(String key){throw GpuNoise1960.unexpected();}
    static void rejectExact1971(String key,String reason){throw GpuNoise1960.unexpected();}
    static void rejectNonSpeed1988(String key,String reason){throw GpuNoise1960.unexpected();}
    static void rejectMeasuredThreshold1992(String key,String kind,String reference,long baseline,long candidate,long limit){throw new AssertionError("fixture unexpectedly exceeded generous timing bound");}
    static void progress1984(String phase,int done,int total){}
    static void outcome1984(String reason){}
    static void timings1984(long baseline,long candidate){}
    static final class QueueDecision1983 {boolean accepted;int reason,retries,queuedJobs,runningJobs;long retryRemainingNanos,retainedBytes,requestedBytes;}
    static final class Reservation1984 {
        QueueDecision1983 decision;
        boolean current(){throw GpuNoise1960.unexpected();}boolean begin1985(){throw GpuNoise1960.unexpected();}
        QueueDecision1983 commit(Probe p){throw GpuNoise1960.unexpected();}void close(){throw GpuNoise1960.unexpected();}
    }
    static Reservation1984 reserve1985(String key,long bytes){throw GpuNoise1960.unexpected();}
}
final class ResidentProof1978 {
    static final int EXACT=1,MISMATCH=2,UNAVAILABLE=0;
    static final class Evidence19103 {boolean shapeMismatch;long compared;int width,height,x,y,expectedArgb,actualArgb;}
    static boolean identity(Bitmap b,int r,int w,int h){return r==0&&b.getWidth()==w&&b.getHeight()==h;}
    static long retainedBytes(Bitmap b,QualityPixels1932.Plan p,QualityPixels1932.Plan o){throw GpuNoise1960.unexpected();}
    static int[] half(Bitmap b,GpuQualification1961.Cancellation c){throw GpuNoise1960.unexpected();}
    static void beginEvidence19103(int w,int h){throw GpuNoise1960.unexpected();}
    static Evidence19103 endEvidence19103(){throw GpuNoise1960.unexpected();}
}
final class GpuSnapshotBudget1981 {
    static final int RESIDENT=1;
    static final class Copy {boolean begin(){throw GpuNoise1960.unexpected();}boolean current(){throw GpuNoise1960.unexpected();}void close(){throw GpuNoise1960.unexpected();}}
    static final class Decision1985 {Copy copy;int reason,preferredFamily,activeFamily;boolean activeStarted;long copyEpoch;}
    static void offerWhole(int family){throw GpuNoise1960.unexpected();}
    static Decision1985 tryCopy1985(long bytes,int family){throw GpuNoise1960.unexpected();}
}
final class ProcessingTiming1947 {
    static final class Trace {}
    static int route;
    static void detailRoute1976(Trace t,int r){route=r;}
    static void resident1978(Trace t,int sw,int sh,int w,int h,int r,boolean identity,int half,long bytes,int reason){}
    static void residentCopy1985(Trace t,int reason,int preferred,int active,boolean started,long epoch){}
    static void residentExit1983(Trace t,long bytes,int reason,int why,long retry,int retries,int queued,int running,long held,long request){}
}
final class PipelineDiagnostics1982 {
    static int exitReason;
    static void residentExit(ProcessingTiming1947.Trace t,long bytes,int reason){exitReason=reason;}
}
