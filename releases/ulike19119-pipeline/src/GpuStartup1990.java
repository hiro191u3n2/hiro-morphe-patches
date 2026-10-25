package com.hiro.ulike;

import android.content.Context;
import java.util.concurrent.locks.LockSupport;

/** Start independent GPU setup from application initialization, before a camera
 * photograph exists. Context/program setup and finite-corpus evidence are
 * separate states: compilation alone is never called quality qualification.
 * No Activity, bitmap, camera object or capture epoch is owned by this worker. */
public final class GpuStartup1990 {
    private GpuStartup1990() {}
    public static final int NOT_STARTED=0,PREPARING=1,READY=2,PARTIAL=3,UNAVAILABLE=4;
    private static final Object LOCK=new Object();
    // Standard exact Strong, its conservative fallback, then the production
    // preparation/analysis/protection/geometry/finish graph. Speculative layout
    // variants are intentionally compiled only by their optional tuner.
    private static final int[] STANDARD={39,51,52,53,2,48,27,28,29,1,3,4,5,6,7,8};
    private static volatile int state,prepared,failed;
    private static volatile boolean coreReady,modelReady,finishReady,sharedReleased1991;
    private static volatile long started,elapsed,readyAt;
    private static volatile String detail="未開始";
    private static volatile String finishWideStatus1995="起動時128行状態は未取得";
    private static Thread worker;
    private static boolean initialized;

    public static void initialize(Context context) {
        if(context==null)return;
        GpuCertificates1990.initialize(context);
        boolean first;
        synchronized(LOCK){first=!initialized;initialized=true;}
        // Count the startup work toward the existing quiet window. The optional
        // tile8 job is still queued only after standard proof/session release;
        // a shutter during startup closes this window before any trial can run.
        if(first)GpuQualification1961.enablePreFirstStrongIdle19117();
        request();
    }
    public static void request() {
        synchronized(LOCK){
            if(!initialized||state!=NOT_STARTED)return;
            state=PREPARING;detail="GPU識別・プログラム準備中";started=System.nanoTime();
            try {
                worker=new Thread(new Runnable(){public void run(){prepare();}},"ULike-GPU-startup1990");
                worker.setDaemon(true);worker.start();
            }catch(RuntimeException unavailable){state=UNAVAILABLE;detail="準備スレッドを開始できません";elapsed=System.nanoTime()-started;LOCK.notifyAll();}
            catch(OutOfMemoryError unavailable){state=UNAVAILABLE;detail="準備メモリ不足";elapsed=System.nanoTime()-started;LOCK.notifyAll();}
        }
    }
    private static void prepare() {
        int outcome=UNAVAILABLE;String result="GPU環境を取得できません";boolean checked=false;
        try {
            GpuQualification1961.startupPause1990(true);
            if(!GpuNoise1960.warmEnvironment1973())return;
            // The standard route needs program39 first. Its real device suite
            // precedes speculative layouts and auxiliary compilation so a cold
            // first shutter need not wait for unused alternatives.
            boolean core=GpuNoise1960.supports(STANDARD[0]);
            synchronized(LOCK){if(core)prepared++;else failed++;}
            checked=core&&qualifyDomain1990(0);
            synchronized(LOCK){if(checked)detail="標準GPU検証済み・共有GPUの準備完了待ち";}
            for(int i=1;i<STANDARD.length;i++){
                if(Thread.currentThread().isInterrupted()){result="補助GPU準備中断";outcome=checked?READY:PARTIAL;return;}
                boolean supported=GpuNoise1960.supports(STANDARD[i]);
                synchronized(LOCK){if(supported)prepared++;else failed++;}
            }
            boolean model=qualifyDomain1990(1);
            synchronized(LOCK){modelReady=model;}
            // This separately owned finishing context must identify its driver
            // before WholeRoute reads persisted admission on the first save.
            GpuFinish1953.warmupAsync();
            // Optional finishing uses its own exact corpus/evidence. A finishing
            // failure cannot revoke an already validated Strong route.
            boolean finishing=qualifyDomain1990(2);
            synchronized(LOCK){finishReady=finishing;}
            outcome=checked?READY:PARTIAL;
            result=checked?(finishing?"標準GPU・仕上げGPUの起動時検証済み":"標準GPU検証済み・仕上げGPU検証未成立"):
                "GPU準備済み・標準経路の検証未成立";
        }catch(RuntimeException unavailable){outcome=prepared>0?PARTIAL:UNAVAILABLE;result="GPU準備または起動時検証が失敗";}
        catch(LinkageError unavailable){outcome=prepared>0?PARTIAL:UNAVAILABLE;result="GPU実装を読み込めません";}
        catch(OutOfMemoryError unavailable){outcome=prepared>0?PARTIAL:UNAVAILABLE;result="GPU準備または検証のメモリ不足";}
        finally {
            // Restored certificates perform no GPU work, but the cancelled old
            // optional probe may still own its token. Join that ownership fence
            // too; no monitor is held, and the single startup deadline applies.
            boolean released=false;
            try{released=waitForSession1990();}catch(Throwable optional){}
            // Release even after interruption, linkage, allocation or shader
            // failure. Existing optional jobs must never be stranded forever.
            try{GpuQualification1961.startupPause1990(false);}catch(Throwable optional){}
            synchronized(LOCK){
                // Every optional corpus above has now closed its shared GPU
                // session. Publishing earlier lets the foreground race into
                // openNative while startup still owns that session and would
                // disable GPU for the entire first photograph.
                sharedReleased1991=released&&!Thread.currentThread().isInterrupted();
                if(checked&&sharedReleased1991){coreReady=true;readyAt=System.nanoTime();}
                elapsed=Math.max(0,System.nanoTime()-started);state=coreReady?READY:outcome==READY?PARTIAL:outcome;
                detail=checked&&!released?"共有GPUの解放を時間内に確認できません":result;
                // Report the independent model failure; Strong success alone
                // does not mean that the model/maps run on the GPU.
                detail+=" / "+GpuModelDefault1990.summary();finishWideStatus1995=GpuFinishDefault1990.summaryWide1995();worker=null;LOCK.notifyAll();
            }
            // Optional layout work enters the cancellable idle queue only
            // after publishing the unchanged standard readiness fence.
            if(coreReady){
                // The standard proof is published already. An idle, cancellable
                // synthetic tile8 trial may finish before the first shutter;
                // foreground never waits for it or treats partial proof as fast.
                GpuStrongDefault1990.request1994();GpuModelDefault1990.requestOptional1994();GpuFinishDefault1990.requestWide1995();
            }
        }
    }
    private static String suite1990(int domain){return domain==0?GpuStrongDefault1990.SUITE:
        domain==1?GpuModelDefault1990.SUITE:GpuFinishDefault1990.SUITE;}
    private static boolean corpusReady1990(int domain){return domain==0?GpuStrongDefault1990.ready():
        domain==1?GpuModelDefault1990.ready():GpuFinishDefault1990.ready();}
    private static boolean withinDeadline1990(){return !Thread.currentThread().isInterrupted()&&
        (started==0||System.nanoTime()-started<30000000000L);}
    /** A competing old foreground/optional probe is allowed to release itself.
     * No startup/service monitor is held while waiting for GPU ownership. */
    private static boolean waitForSession1990(){
        while(GpuNoise1960.sessionBusy()){
            if(!withinDeadline1990())return false;
            LockSupport.parkNanos(2000000L);
        }
        return withinDeadline1990();
    }
    private static boolean qualifyDomain1990(int domain){
        final String suite=suite1990(domain);
        // A restored finite certificate performs no GPU commands or image work.
        // It must not be lost because another photograph currently owns a token.
        if(corpusReady1990(domain)){
            if(domain!=2||GpuFinishDefault1990.wideReady1993()||GpuCertificates1990.rejected(GpuFinishDefault1990.wideSuite1993()))return true;
            // Restored 32-row evidence must not permanently skip independent
            // 128-row recovery. Failure or the shared deadline preserves 32.
            if(!waitForSession1990())return GpuFinishDefault1990.ready();
            try{GpuFinishDefault1990.qualifyWide1995(new GpuQualification1961.Cancellation(){
                public boolean cancelled(){return !withinDeadline1990();}
            });}catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
            return GpuFinishDefault1990.ready();
        }
        for(int attempt=0;attempt<2;attempt++){
            if(GpuCertificates1990.rejected(suite)||!waitForSession1990())return false;
            boolean ok=false;
            try {
                GpuQualification1961.Cancellation cancellation=new GpuQualification1961.Cancellation(){
                    public boolean cancelled(){return !withinDeadline1990();}
                };
                ok=domain==0?GpuStrongDefault1990.qualify(cancellation):domain==1?
                    GpuModelDefault1990.qualify(cancellation):GpuFinishDefault1990.qualify(cancellation);
            }catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
            if(ok)return true;
            // Only a transient refusal can retry, at most once in this process.
            // An actual unequal output remains authoritative and never retries.
            if(GpuCertificates1990.rejected(suite)||!withinDeadline1990())return false;
            LockSupport.parkNanos(20000000L);
        }
        return false;
    }
    private static boolean mainLooper1990(){
        try {
            Class<?> looper=Class.forName("android.os.Looper");
            Object current=looper.getMethod("myLooper").invoke(null);
            return current!=null&&current==looper.getMethod("getMainLooper").invoke(null);
        }catch(ReflectiveOperationException unavailable){return false;}
        catch(LinkageError unavailable){return false;}
    }
    /** Optional bounded save-worker wait. UI/camera startup never calls this.
     * A timeout does not cancel or claim completion of the existing preflight. */
    public static boolean awaitPreparation(long milliseconds) {
        if(milliseconds<0)throw new IllegalArgumentException("startup wait");
        request();if(mainLooper1990())return coreReady;
        long budget=Math.min(milliseconds,30000L)*1000000L,start=System.nanoTime();
        synchronized(LOCK){while(state==PREPARING&&budget>0){
            long left=budget-(System.nanoTime()-start);if(left<=0)break;
            try{LOCK.wait(left/1000000L,(int)(left%1000000L));}
            catch(InterruptedException cancelled){Thread.currentThread().interrupt();return false;}
        }return state==READY;}
    }
    /** Wait until the standard route is validated AND all startup work has
     * released the shared GPU session. Production calls this only from the save
     * worker at Strong-stage entry, never UI/camera/shutter callbacks. First
     * installation preparation is included in real elapsed save time and has
     * a finite thirty-second fault ceiling. Warm evidence avoids corpus reruns. */
    public static boolean awaitCoreReady(long milliseconds) {
        if(milliseconds<0)throw new IllegalArgumentException("startup wait");
        request();if(mainLooper1990())return coreReady;
        long budget=Math.min(milliseconds,30000L)*1000000L,start=System.nanoTime();
        synchronized(LOCK){
            // Multiple pipeline stages cannot each extend a failed startup by
            // another thirty seconds. The fault window belongs to one startup.
            if(started!=0)budget=Math.min(budget,Math.max(0L,30000000000L-(start-started)));
            if(Thread.currentThread()==worker)return coreReady;
            while(!coreReady&&state==PREPARING&&budget>0){
                long left=budget-(System.nanoTime()-start);if(left<=0)break;
                try{LOCK.wait(left/1000000L,(int)(left%1000000L));}
                catch(InterruptedException cancelled){Thread.currentThread().interrupt();return false;}
            }
            return coreReady;
        }
    }
    /** The model corpus has its own admission. A Strong-corpus failure must
     * not suppress a successfully verified model, but every startup GPU owner
     * must still have released its session before foreground model dispatch. */
    static boolean awaitModelReady1991(long milliseconds) {
        if(milliseconds<0)throw new IllegalArgumentException("startup wait");
        request();
        if(mainLooper1990())return sharedReleased1991&&modelReady&&GpuModelDefault1990.ready();
        long budget=Math.min(milliseconds,30000L)*1000000L,start=System.nanoTime();
        synchronized(LOCK){
            if(started!=0)budget=Math.min(budget,Math.max(0L,30000000000L-(start-started)));
            if(Thread.currentThread()==worker)return false;
            while(state==PREPARING&&budget>0){
                long left=budget-(System.nanoTime()-start);if(left<=0)break;
                try{LOCK.wait(left/1000000L,(int)(left%1000000L));}
                catch(InterruptedException cancelled){Thread.currentThread().interrupt();return false;}
            }
            return sharedReleased1991&&modelReady&&GpuModelDefault1990.ready();
        }
    }
    public static final class Snapshot {
        public final int state,preparedPrograms,failedPrograms;
        public final long startedNanos,readyNanos,elapsedNanos;
        public final boolean domainReady,modelDomainReady,finishDomainReady;
        public final String finishWideStatus1995;
        private Snapshot(int status,int done,int failures,long began,long ready,long elapsed,boolean domain,boolean model,boolean finish){
            state=status;preparedPrograms=done;failedPrograms=failures;startedNanos=began;
            readyNanos=ready;elapsedNanos=elapsed;domainReady=domain;modelDomainReady=model;finishDomainReady=finish;finishWideStatus1995=GpuStartup1990.finishWideStatus1995;
        }
    }
    public static Snapshot snapshot(){synchronized(LOCK){return new Snapshot(state,prepared,failed,started,readyAt,
        state==PREPARING&&started!=0?Math.max(0,System.nanoTime()-started):elapsed,
        coreReady,modelReady,finishReady);}}
    public static int state(){return state;}
    public static int preparedPrograms(){return prepared;}
    public static int failedPrograms(){return failed;}
    public static long startedNanos(){return started;}
    public static long elapsedNanos(){return state==PREPARING&&started!=0?Math.max(0,System.nanoTime()-started):elapsed;}
    public static long firstReadyNanos(){return readyAt;}
    public static boolean domainReady(){return coreReady&&GpuStrongDefault1990.ready();}
    /** Read-only readiness. These cannot enqueue startup work or wait on the
     * current idle queue owner. Shared startup resources must be released. */
    static boolean coreDomainReady19102(){
        return sharedReleased1991&&coreReady&&GpuStrongDefault1990.ready();
    }
    static boolean modelDomainReady19102(){
        return sharedReleased1991&&modelReady&&GpuModelDefault1990.ready();
    }
    /** Called when the standard owner revokes its device evidence. Keep the
     * historical readiness timestamp for latency accounting, but never keep a
     * positive current-readiness label after revocation. */
    static void invalidateDomain(){synchronized(LOCK){coreReady=false;if(state==READY)state=PARTIAL;
        detail="標準GPU経路の検証が失効";LOCK.notifyAll();}}
    static void invalidateModelDomain(){synchronized(LOCK){modelReady=false;LOCK.notifyAll();}}
    static void invalidateFinishDomain(){synchronized(LOCK){finishReady=false;LOCK.notifyAll();}}
    public static String detail(){return detail;}
}
