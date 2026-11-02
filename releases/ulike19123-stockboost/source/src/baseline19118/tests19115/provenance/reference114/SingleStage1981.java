package com.hiro.ulike;

import java.util.IdentityHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** One photograph may reuse one fixed-backend GPU session. Contending workers
 * never wait for this optional owner; the existing CPU path remains available.
 * A lease is reusable only after its complete private output has succeeded. */
final class SingleStage1981 implements AutoCloseable {
    static final int SINGLE=1,RESIDUAL=2;
    private static final IdentityHashMap<SingleNoise1955.Model,SingleStage1981> ACTIVE=new IdentityHashMap<SingleNoise1955.Model,SingleStage1981>();
    private final ReentrantLock lock=new ReentrantLock();
    private SingleNoise1955.Model model;
    private GpuNoise1960.Session session;
    private SingleResidual1961.Buffers1981 buffers;
    private int backend;
    private boolean busy,closed,disabled,modelUploaded;
    private final boolean registered;

    private SingleStage1981(SingleNoise1955.Model model,boolean registered){this.model=model;this.registered=registered;}
    static SingleStage1981 begin(SingleNoise1955.Model model) {
        if(model==null)return null;
        SingleStage1981 stage=new SingleStage1981(model,true);
        boolean added=false;
        synchronized(ACTIVE) {
            try {if(ACTIVE.containsKey(model))return null;ACTIVE.put(model,stage);added=true;return stage;}
            finally {if(!added&&ACTIVE.get(model)==stage)ACTIVE.remove(model);}
        }
    }
    static String key(String exact){return "single-stage1981-current-cpu-exact2-time5-v2:"+exact;}
    static boolean owns(SingleNoise1955.Model model) {
        synchronized(ACTIVE){return ACTIVE.containsKey(model);}
    }
    static boolean selected(SingleNoise1955.Model model,String exact) {
        return owns(model)&&CpuExact1978.enabled(key(exact));
    }
    /** Another strip may have a different, still-cold qualification key. It
     * still must not queue a legacy open behind this photograph's owner. */
    static boolean blocksLegacy(SingleNoise1955.Model model) {
        SingleStage1981 stage;
        synchronized(ACTIVE){stage=ACTIVE.get(model);}
        if(stage==null)return false;
        if(!stage.lock.tryLock())return true;
        try{return stage.busy||stage.session!=null||stage.disabled||stage.closed;}
        finally{stage.lock.unlock();}
    }
    static Lease acquire(SingleNoise1955.Model model,int backend,String exact) {
        if(!CpuExact1978.enabled(key(exact)))return null;
        SingleStage1981 stage;
        synchronized(ACTIVE){stage=ACTIVE.get(model);}
        return stage==null?null:stage.take(model,backend);
    }
    private Lease take(SingleNoise1955.Model expected,int kind) {
        if(!lock.tryLock())return null;
        try {
            if(busy||closed||disabled||model!=expected||(kind!=SINGLE&&kind!=RESIDUAL)||backend!=0&&backend!=kind)return null;
            busy=true;backend=kind;
        } finally {lock.unlock();}
        boolean okay=false;
        try {
            if(Thread.currentThread().isInterrupted())return null;
            if(session==null) {
                if(GpuNoise1960.sessionBusy())return null;
                session=GpuNoise1960.open();
            }
            if(session==null)return null;
            Lease lease=new Lease(this,session);okay=true;return lease;
        } finally {if(!okay)release(false);}
    }
    private void release(boolean success) {
        // The complete run/overlap join owns busy throughout native cleanup.
        try {
            if(!success) {
                disabled=true;
                try {if(session!=null)session.close();}
                finally {session=null;modelUploaded=false;if(buffers!=null){buffers.close();buffers=null;}}
            }
        } finally {lock.lock();try{busy=false;}finally{lock.unlock();}}
    }
    static final class Lease implements AutoCloseable {
        private SingleStage1981 owner;
        final GpuNoise1960.Session session;
        private boolean complete;
        Lease(SingleStage1981 owner,GpuNoise1960.Session session){this.owner=owner;this.session=session;}
        boolean model(SingleNoise1955.Model model) {
            SingleStage1981 stage=owner;
            if(stage==null||stage.model!=model||stage.backend!=SINGLE||stage.disabled)return false;
            if(!stage.modelUploaded) {
                if(!session.upload(2,model.gpuData1960()))return false;
                stage.modelUploaded=true;
            }
            return true;
        }
        SingleResidual1961.Buffers1981 buffers() {
            SingleStage1981 stage=owner;
            if(stage==null||stage.backend!=RESIDUAL||stage.disabled)throw new IllegalStateException("single residual stage lease");
            if(stage.buffers==null)stage.buffers=new SingleResidual1961.Buffers1981();
            return stage.buffers;
        }
        void complete(){if(owner==null)throw new IllegalStateException("single GPU lease closed");complete=true;}
        public void close(){SingleStage1981 stage=owner;if(stage==null)return;owner=null;stage.release(complete);}
    }
    public void close() {
        lock.lock();
        try {
            if(closed)return;
            if(busy)throw new IllegalStateException("single GPU stage before worker join");
            closed=true;
        } finally {lock.unlock();}
        try {
            try{if(session!=null)session.close();}
            finally {session=null;if(buffers!=null){buffers.close();buffers=null;}}
        } finally {
            if(registered)synchronized(ACTIVE){if(ACTIVE.get(model)==this)ACTIVE.remove(model);}
            model=null;
        }
    }
    /** Independently timed CPU references for the two private input images. */
    static final class CpuSample {
        final int[] pixels;final long nanos;
        CpuSample(int[] pixels,long nanos){this.pixels=pixels;this.nanos=nanos;}
    }
    interface Operation {
        String referenceKey();
        CpuSample cpu(int pass,GpuQualification1961.Cancellation cancellation);
        int[] run(Lease lease,int pass);
        long preparationNanos();
        void close();
    }
    interface Factory {Operation create();}
    private static final Object SNAPSHOTS=new Object();
    // Fixed scalar observations mirrored by ProcessingTiming1947. A stage
    // decision describes this caller only, never admission of GPU pixels.
    static final int OFFER19100=1,ORIGINAL_REJECTED19100=2,QUALIFIED19100=3,INVALID19100=4,
        INTERRUPTED19100=5,BACKGROUND19100=6,COPY_DECLINED19100=7,SNAPSHOT_MEMORY19100=8,
        QUEUE_DECLINED19100=9,BEGIN_FAILED19100=10,RESERVED19100=11,NO_SNAPSHOT19100=12,
        CAPTURED19100=13,STALE19100=14,REJECTED_AFTER_COPY19100=15,REFERENCE_CHANGED19100=16,
        QUEUED19100=17,COMMIT_DECLINED19100=18,EXCEPTION19100=19,LINKAGE19100=20,
        ALLOCATION19100=21,CUSTOM_PROTECTION19100=22;
    /** Retained compatibility entry keeps its original conservative peak. New
     * production callers explicitly separate copy and deferred-execution lives. */
    static void offer(final String original,final String reference,final SingleNoise1955.Model model,final int backend,
            long retained,long peak,final Factory factory) {
        long combined=retained<0||peak<0||retained>Long.MAX_VALUE-peak?-1:retained+peak;
        offer19100(original,reference,model,backend,retained,combined,peak,factory);
    }
    /** A cold stage does not depend on the old per-strip GPU speed certificate.
     * Only its own current-CPU-reference key is written by this detached proof.
     * snapshotPeak includes the entire new closure plus temporary construction
     * work; executionPeak is ADDITIONAL to that live closure, after saving. The
     * two CPU oracles and GPU workspace are never allocated by this method. */
    static void offer19100(final String original,final String reference,final SingleNoise1955.Model model,final int backend,
            long retained,long snapshotPeak,long executionPeak,final Factory factory) {
        final ProcessingTiming1947.Trace trace=PipelineDetail1988.current();
        state19100(trace,backend,OFFER19100,-1,retained,snapshotPeak,executionPeak);
        try{synchronized(SNAPSHOTS) {
            if(original==null||original.length()==0||reference==null||reference.length()==0||model==null||factory==null||
                    backend!=SINGLE&&backend!=RESIDUAL||retained<1||retained>96L*1024*1024||
                    snapshotPeak<retained||snapshotPeak>GpuNoise1960.MAX_BYTES||executionPeak<0||executionPeak>GpuNoise1960.MAX_BYTES){
                state19100(trace,backend,INVALID19100,-1,retained,snapshotPeak,executionPeak);return;
            }
            final String key=key(reference);
            if(GpuQualification1961.exactRejected(original)){
                state19100(trace,backend,ORIGINAL_REJECTED19100,-1,retained,snapshotPeak,executionPeak);return;
            }
            if(CpuExact1978.enabled(key)){
                state19100(trace,backend,QUALIFIED19100,-1,retained,snapshotPeak,executionPeak);return;
            }
            if(Thread.currentThread().isInterrupted()){
                state19100(trace,backend,INTERRUPTED19100,-1,retained,snapshotPeak,executionPeak);return;
            }
            if(CpuExact1978.background()){
                state19100(trace,backend,BACKGROUND19100,-1,retained,snapshotPeak,executionPeak);return;
            }
            int family=backend==SINGLE?GpuSnapshotBudget1981.SINGLE_STAGE:GpuSnapshotBudget1981.RESIDUAL_STAGE;
            GpuSnapshotBudget1981.offerStrip1981(family);
            long copied=Math.max(1L,retained-4L*model.gpuData1960().length);
            // Obtain the explanation from the very same admission; a second
            // query could report a different owner/turn after a capture change.
            GpuSnapshotBudget1981.Decision1985 decision1985=GpuSnapshotBudget1981.tryCopy1985(copied,family);
            GpuSnapshotBudget1981.Copy copy=decision1985.copy;
            if(copy==null){
                state19100(trace,backend,COPY_DECLINED19100,decision1985.reason,retained,snapshotPeak,executionPeak);
                copy1999(trace,PipelineDetail1988.SKIPPED,retained,0,-1,null);return;
            }
            Operation operation=null;Proof proof=null;boolean transferred=false;
            GpuQualification1961.Reservation1984 reservation=null;
            try {
                // Foreground owns only this snapshot. Execution allocations
                // remain guarded by Proof.run's unchanged live peak check.
                if(!GpuNoise1960.workspaceFits(snapshotPeak)){
                    state19100(trace,backend,SNAPSHOT_MEMORY19100,decision1985.reason,retained,snapshotPeak,executionPeak);
                    copy1999(trace,PipelineDetail1988.SKIPPED,retained,0,-1,
                        new GpuQualification1961.QueueDecision1983(GpuQualification1961.QueueDecision1983.SCHEDULER_MEMORY,
                            0,0,-1,-1,-1,retained));return;
                }
                // .99: a preflight boolean did not own the queue capacity. A
                // competing proof could consume it while this caller copied
                // the image. Reserve actual capacity before any snapshot; the
                // ticket also closes the capture-change/copy race.
                reservation=GpuQualification1961.reserve1985(key,retained);
                if(!reservation.accepted()){
                    state19100(trace,backend,QUEUE_DECLINED19100,decision1985.reason,retained,snapshotPeak,executionPeak);
                    copy1999(trace,PipelineDetail1988.QUEUE_DECLINED,retained,0,-1,reservation.decision);return;
                }
                if(!reservation.begin1985()||!copy.begin()){
                    state19100(trace,backend,BEGIN_FAILED19100,decision1985.reason,retained,snapshotPeak,executionPeak);
                    copy1999(trace,PipelineDetail1988.INTERRUPTED,retained,0,-1,reservation.decision);return;
                }
                state19100(trace,backend,RESERVED19100,decision1985.reason,retained,snapshotPeak,executionPeak);
                copy1999(trace,PipelineDetail1988.RESERVED,retained,0,-1,reservation.decision);
                long started=System.nanoTime();
                operation=factory.create();if(operation==null){
                    state19100(trace,backend,NO_SNAPSHOT19100,decision1985.reason,retained,snapshotPeak,executionPeak);return;
                }
                // This closure includes policies/models as well as pixel
                // arrays. Do not mislabel its byte estimate as pixel payload.
                state19100(trace,backend,CAPTURED19100,decision1985.reason,retained,snapshotPeak,executionPeak);
                copy1999(trace,PipelineDetail1988.CAPTURED,retained,-1,System.nanoTime()-started,null);
                proof=new Proof(original,key,model,backend,executionPeak,operation);operation=null;
                if(!copy.current()){
                    state19100(trace,backend,STALE19100,decision1985.reason,retained,snapshotPeak,executionPeak);return;
                }
                if(GpuQualification1961.exactRejected(original)){
                    state19100(trace,backend,REJECTED_AFTER_COPY19100,decision1985.reason,retained,snapshotPeak,executionPeak);return;
                }
                if(!key.equals(key(proof.operation.referenceKey()))){
                    state19100(trace,backend,REFERENCE_CHANGED19100,decision1985.reason,retained,snapshotPeak,executionPeak);return;
                }
                // commit owns and closes even a declined probe, retaining the
                // exact same pixel and five-percent speed admission contract.
                GpuQualification1961.QueueDecision1983 decision=reservation.commit(proof);transferred=true;
                state19100(trace,backend,decision!=null&&decision.accepted?QUEUED19100:COMMIT_DECLINED19100,
                    decision1985.reason,retained,snapshotPeak,executionPeak);
                copy1999(trace,decision!=null&&decision.accepted?PipelineDetail1988.QUEUED:PipelineDetail1988.QUEUE_DECLINED,
                    retained,-1,-1,decision);
            }finally {
                try{if(!transferred){if(proof!=null)proof.close();else if(operation!=null)operation.close();}}
                finally{try{if(reservation!=null)reservation.close();}finally{copy.close();}}
            }
        }}catch(RuntimeException unavailable){state19100(trace,backend,EXCEPTION19100,-1,retained,snapshotPeak,executionPeak);}
          catch(LinkageError unavailable){state19100(trace,backend,LINKAGE19100,-1,retained,snapshotPeak,executionPeak);}
          catch(OutOfMemoryError unavailable){state19100(trace,backend,ALLOCATION19100,-1,retained,snapshotPeak,executionPeak);GpuQualification1961.outcome1984("memory_budget");}
    }
    /** The immutable snapshot estimate already includes its arrays and 64 KiB
     * headers. One extra MiB covers temporary key/descriptor construction. No
     * native/GPU or CPU-oracle buffers exist until the deferred probe runs. */
    static long snapshotPeak19100(long retained){return retained<1||retained>Long.MAX_VALUE-1048576L?-1:retained+1048576L;}
    static void skippedProtection19100(int backend){
        state19100(PipelineDetail1988.current(),backend,CUSTOM_PROTECTION19100,-1,-1,-1,-1);
    }
    private static volatile java.lang.reflect.Method frontDetail19100;
    private static volatile boolean frontDetailLooked19100;
    private static void state19100(ProcessingTiming1947.Trace trace,int backend,int state,int copyReason,
            long retained,long snapshotPeak,long executionPeak){
        if(trace==null)return;
        try {
            if(!frontDetailLooked19100){
                try{frontDetail19100=ProcessingTiming1947.class.getMethod("frontProof19100",ProcessingTiming1947.Trace.class,
                    int.class,int.class,int.class,long.class,long.class,long.class);}
                finally{frontDetailLooked19100=true;}
            }
            java.lang.reflect.Method detail=frontDetail19100;
            if(detail!=null)detail.invoke(null,trace,Integer.valueOf(backend),Integer.valueOf(state),Integer.valueOf(copyReason),
                Long.valueOf(retained),Long.valueOf(snapshotPeak),Long.valueOf(executionPeak));
        }catch(Throwable optional){}
    }
    private static void copy1999(ProcessingTiming1947.Trace trace,int outcome,long requested,long pixels,long nanos,
            GpuQualification1961.QueueDecision1983 decision) {
        PipelineDetail1988.copy(trace,PipelineDetail1988.FRONT_STRIP,outcome,requested,nanos);
        PipelineDetail1988.copyState1989(trace,PipelineDetail1988.FRONT_STRIP,outcome,requested,pixels,nanos,
            decision==null?-1:decision.reason,decision==null?-1:decision.retryRemainingNanos,
            decision==null?-1:decision.retries,decision==null?-1:decision.queuedJobs,
            decision==null?-1:decision.runningJobs,decision==null?-1:decision.retainedBytes);
    }
    /** The original and optimized CPU executions are independently compared
     * before their faster measured result can be used as the speed reference.
     * Each GPU trial includes one open, first model upload, two full outputs,
     * the caller-output copies and final close. Both inputs must be exact on
     * both measured trials, with stable CPU output between those trials. */
    private static final class Proof implements GpuQualification1961.Probe {
        final String original,key;final int backend;final long peak;
        SingleNoise1955.Model model;Operation operation;
        Proof(String original,String key,SingleNoise1955.Model model,int backend,long peak,Operation operation){
            this.original=original;this.key=key;this.model=model;this.backend=backend;this.peak=peak;this.operation=operation;
        }
        private boolean stopped(GpuQualification1961.Cancellation c){
            return operation==null||c.cancelled()||Thread.currentThread().isInterrupted()||GpuQualification1961.exactRejected(original)||
                !key.equals(key(operation.referenceKey()));
        }
        public void run(GpuQualification1961.Cancellation cancellation) {
            try {
                long fastestCpu=Long.MAX_VALUE,slowestGpu=0;Object[] stable=null;
                for(int trial=-1;trial<2;trial++) {
                    if(stopped(cancellation))return;
                    if(!GpuNoise1960.workspaceFits(peak)){GpuQualification1961.outcome1984("memory_budget");return;}
                    Object[] cpu=new Object[2],gpu=new Object[2];long cpuNanos=0;
                    for(int pass=0;pass<2;pass++) {
                        if(stopped(cancellation))return;
                        CpuSample sample=operation.cpu(pass,cancellation);
                        if(sample==null||sample.pixels==null||sample.nanos<=0||sample.nanos>Long.MAX_VALUE-cpuNanos){GpuQualification1961.outcome1984("comparison_incomplete");return;}
                        cpu[pass]=sample.pixels;cpuNanos+=sample.nanos;
                    }
                    if(stopped(cancellation))return;
                    long start=System.nanoTime();SingleStage1981 stage=new SingleStage1981(model,false);
                    try {
                        for(int pass=0;pass<2;pass++) {
                            if(stopped(cancellation))return;
                            Lease lease=stage.take(model,backend);if(lease==null)return;
                            int[] result;
                            try{result=operation.run(lease,pass);}
                            finally{lease.close();}
                            if(result==null){GpuQualification1961.outcome1984("execution_unavailable");return;}
                            gpu[pass]=result.clone(); // The foreground's final owned-output copy.
                        }
                    }finally{stage.close();}
                    long gpuNanos=System.nanoTime()-start,preparation=operation.preparationNanos();
                    if(preparation<0||preparation>Long.MAX_VALUE-gpuNanos)return;
                    gpuNanos+=preparation;
                    if(stopped(cancellation))return;
                    if(!CpuExact1978.same(cpu,gpu)||trial==1&&!CpuExact1978.same(stable,cpu)) {
                        GpuQualification1961.rejectExact(key);return;
                    }
                    if(trial==0)stable=cpu;
                    if(trial>=0){fastestCpu=Math.min(fastestCpu,cpuNanos);slowestGpu=Math.max(slowestGpu,gpuNanos);}
                }
                if(stopped(cancellation))return;
                if(slowestGpu>0&&slowestGpu<=fastestCpu-fastestCpu/20)
                    GpuQualification1961.qualified(key,fastestCpu,slowestGpu,1);
                else GpuQualification1961.rejectMeasuredSpeed1992(key,backend==SINGLE?"前段単写GPU連結":"前段残差GPU連結",fastestCpu,slowestGpu);
            }catch(SingleCpu1981.Mismatch mismatch){if(!stopped(cancellation))GpuQualification1961.rejectExact(key);}
              catch(java.util.concurrent.CancellationException cancelled){throw cancelled;}
              catch(RuntimeException unavailable){if(!stopped(cancellation))GpuQualification1961.rejectNonSpeed1988(key,"exception");}
              catch(LinkageError unavailable){if(!stopped(cancellation))GpuQualification1961.rejectNonSpeed1988(key,"unsupported");}
              catch(OutOfMemoryError unavailable){GpuQualification1961.outcome1984("memory_budget");}
        }
        public void close(){Operation old=operation;operation=null;model=null;if(old!=null)old.close();}
    }
}
