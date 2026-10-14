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
    /** A cold stage does not depend on the old per-strip GPU speed certificate.
     * Only its own current-CPU-reference key is written by this detached proof. */
    static void offer(final String original,final String reference,final SingleNoise1955.Model model,final int backend,
            long retained,long peak,final Factory factory) {
        final String key=key(reference);
        final ProcessingTiming1947.Trace trace=PipelineDetail1988.current();
        try{synchronized(SNAPSHOTS) {
            if(GpuQualification1961.exactRejected(original)||CpuExact1978.enabled(key))return;
            if(retained<1||peak<0||retained>96L*1024*1024||retained>Long.MAX_VALUE-peak||
                    Thread.currentThread().isInterrupted()||CpuExact1978.background())return;
            int family=backend==SINGLE?GpuSnapshotBudget1981.SINGLE_STAGE:GpuSnapshotBudget1981.RESIDUAL_STAGE;
            GpuSnapshotBudget1981.offerStrip1981(family);
            long copied=Math.max(1L,retained-4L*model.gpuData1960().length);
            GpuSnapshotBudget1981.Copy copy=GpuSnapshotBudget1981.tryStripCopy1981(copied,family);
            if(copy==null){copy1999(trace,PipelineDetail1988.SKIPPED,retained,0,-1,null);return;}
            Operation operation=null;Proof proof=null;boolean transferred=false;
            GpuQualification1961.Reservation1984 reservation=null;
            try {
                if(!GpuNoise1960.workspaceFits(retained+peak)){
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
                    copy1999(trace,PipelineDetail1988.QUEUE_DECLINED,retained,0,-1,reservation.decision);return;
                }
                if(!reservation.begin1985()||!copy.begin()){
                    copy1999(trace,PipelineDetail1988.INTERRUPTED,retained,0,-1,reservation.decision);return;
                }
                copy1999(trace,PipelineDetail1988.RESERVED,retained,0,-1,reservation.decision);
                long started=System.nanoTime();
                operation=factory.create();if(operation==null)return;
                // This closure includes policies/models as well as pixel
                // arrays. Do not mislabel its byte estimate as pixel payload.
                copy1999(trace,PipelineDetail1988.CAPTURED,retained,-1,System.nanoTime()-started,null);
                proof=new Proof(original,key,model,backend,peak,operation);operation=null;
                if(!copy.current()||GpuQualification1961.exactRejected(original)||!key.equals(key(proof.operation.referenceKey())))return;
                // commit owns and closes even a declined probe, retaining the
                // exact same pixel and five-percent speed admission contract.
                GpuQualification1961.QueueDecision1983 decision=reservation.commit(proof);transferred=true;
                copy1999(trace,decision!=null&&decision.accepted?PipelineDetail1988.QUEUED:PipelineDetail1988.QUEUE_DECLINED,
                    retained,-1,-1,decision);
            }finally {
                try{if(!transferred){if(proof!=null)proof.close();else if(operation!=null)operation.close();}}
                finally{try{if(reservation!=null)reservation.close();}finally{copy.close();}}
            }
        }}catch(RuntimeException unavailable){}
          catch(LinkageError unavailable){}
          catch(OutOfMemoryError unavailable){GpuQualification1961.outcome1984("memory_budget");}
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
