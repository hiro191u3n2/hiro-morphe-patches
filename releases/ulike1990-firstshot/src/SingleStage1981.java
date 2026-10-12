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
        try{synchronized(SNAPSHOTS) {
            if(GpuQualification1961.exactRejected(original)||CpuExact1978.enabled(key))return;
            int family=backend==SINGLE?GpuSnapshotBudget1981.SINGLE_STAGE:GpuSnapshotBudget1981.RESIDUAL_STAGE;
            GpuSnapshotBudget1981.offerStrip1981(family);
            long copied=Math.max(1L,retained-4L*model.gpuData1960().length);
            GpuSnapshotBudget1981.Copy copy=GpuSnapshotBudget1981.tryStripCopy1981(copied,family);
            if(copy==null)return;
            Operation operation=null;Proof proof=null;boolean transferred=false;
            try {
                if(!CpuExact1978.canCapture(key,retained,peak)||!copy.begin())return;
                operation=factory.create();if(operation==null)return;
                proof=new Proof(original,key,model,backend,peak,operation);operation=null;
                if(!copy.current()||GpuQualification1961.exactRejected(original)||!key.equals(key(proof.operation.referenceKey())))return;
                // schedule owns and closes even a declined probe.
                GpuQualification1961.schedule(key,retained,proof);transferred=true;
            }finally {
                try{if(!transferred){if(proof!=null)proof.close();else if(operation!=null)operation.close();}}
                finally{copy.close();}
            }
        }}catch(RuntimeException unavailable){}
          catch(LinkageError unavailable){}
          catch(OutOfMemoryError unavailable){}
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
                    if(stopped(cancellation)||!GpuNoise1960.workspaceFits(peak))return;
                    Object[] cpu=new Object[2],gpu=new Object[2];long cpuNanos=0;
                    for(int pass=0;pass<2;pass++) {
                        if(stopped(cancellation))return;
                        CpuSample sample=operation.cpu(pass,cancellation);
                        if(sample==null||sample.pixels==null||sample.nanos<=0||sample.nanos>Long.MAX_VALUE-cpuNanos)return;
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
                            if(result==null)return;
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
                else GpuQualification1961.rejectSpeed(key);
            }catch(SingleCpu1981.Mismatch mismatch){if(!stopped(cancellation))GpuQualification1961.rejectExact(key);}
              catch(java.util.concurrent.CancellationException cancelled){throw cancelled;}
              catch(RuntimeException unavailable){if(!stopped(cancellation))GpuQualification1961.rejectSpeed(key);}
              catch(LinkageError unavailable){if(!stopped(cancellation))GpuQualification1961.rejectSpeed(key);}
              catch(OutOfMemoryError unavailable){}
        }
        public void close(){Operation old=operation;operation=null;model=null;if(old!=null)old.close();}
    }
}
