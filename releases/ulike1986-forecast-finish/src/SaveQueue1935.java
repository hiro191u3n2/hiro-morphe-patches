package com.hiro.ulike;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;

/** Three bounded owned photos, one preparation stage, one FIFO encoder and ordered verified publication tails.
 * SDK rendering is completed before admission and never parallelized here. */
public final class SaveQueue1935 {
    private SaveQueue1935() {}
    public static final int CAPACITY=3;
    public static final long RESERVE=72L*1024*1024;
    private static final ArrayDeque<Runnable> jobs=new ArrayDeque<Runnable>();
    // Submission order survives the earlier release of hardware encoder ownership.
    // A job remains here until its verified publication and receipt enqueue finish.
    private static final ArrayDeque<Runnable> publication1956=new ArrayDeque<Runnable>();
    private static final IdentityHashMap<Runnable,Long> nativeCosts=new IdentityHashMap<Runnable,Long>();
    private static final IdentityHashMap<Runnable,Long> pixels=new IdentityHashMap<Runnable,Long>();
    private static final IdentityHashMap<Runnable,SaveMemory1981.Plan> memoryPlans1981=new IdentityHashMap<Runnable,SaveMemory1981.Plan>();
    private static final IdentityHashMap<Runnable,Long> liveNative1981=new IdentityHashMap<Runnable,Long>();
    private static final IdentityHashMap<Runnable,Runnable> codecWaiters1981=new IdentityHashMap<Runnable,Runnable>();
    private static long memoryGeneration1981;
    private static int held,workers;
    private static Runnable preparing,encoding,codec;
    /** Called off the UI/camera thread before acquiring/copying the native bitmap. */
    static void reserve() {
        boolean interrupted=false;
        synchronized(jobs) {
            while(held>=CAPACITY)try{jobs.wait();}catch(InterruptedException e){interrupted=true;}
            held++;memoryGeneration1981++;
        }
        if(interrupted)Thread.currentThread().interrupt();
    }
    static void release(){synchronized(jobs){if(held<=0)throw new IllegalStateException("save ownership");held--;memoryGeneration1981++;jobs.notifyAll();}}
    public static int count(){synchronized(jobs){return held;}}
    public static long maximumPixels(){synchronized(jobs){long maximum=0;for(Long n:pixels.values())maximum=Math.max(maximum,n.longValue());return maximum;}}
    public static long nativeBytes(){synchronized(jobs){long total=0;for(Long n:nativeCosts.values()){long value=n.longValue();if(value>Long.MAX_VALUE-total)return Long.MAX_VALUE;total+=value;}return total;}}
    static void nativeCost(Runnable job,long bytes){synchronized(jobs){if(nativeCosts.containsKey(job)){memoryGeneration1981++;nativeCosts.put(job,Long.valueOf(Math.max(0,bytes)));}}}
    static void nativeCost1981(Runnable job,long bytes,long verifiedLive){synchronized(jobs){if(nativeCosts.containsKey(job)){memoryGeneration1981++;long value=Math.max(0,bytes);nativeCosts.put(job,Long.valueOf(value));liveNative1981.put(job,Long.valueOf(Math.max(0,Math.min(value,verifiedLive))));}}}
    static long memoryGeneration1981(){synchronized(jobs){return memoryGeneration1981;}}
    public static boolean memoryAllows(long available,long pixelCount){
        // Published scalar ABI remains the conservative fallback for unverified
        // system accounting or geometry. This bound does not enable multi-frame
        // capture; the actual one-frame/domain path is memoryAllows1981 below.
        long queued=nativeBytes(),proof=WholeRoute1953.retainedBytes(),gpu=GpuFinish1953.retainedBytes(),scratch=SpeedWorkers1935.nativeRetainedBytes1956();
        if(proof>Long.MAX_VALUE-queued)return false;
        queued+=proof;
        if(gpu>Long.MAX_VALUE-queued)return false;
        queued+=gpu;
        if(scratch>Long.MAX_VALUE-queued)return false;
        queued+=scratch;
        return pixelCount>0 && pixelCount<=32000000L && available>=queued && available-queued>=RESERVE+pixelCount*24L;
    }
    static boolean memoryAllows1981(SaveMemory1981.Snapshot memory,SaveMemory1981.Plan next,int phase) {
        if(next==null||!next.valid())return false;
        long nativeBytes=0,model=next.modelBytes,workspace=next.workerBytes;
        synchronized(jobs) {
            for(Runnable job:nativeCosts.keySet()) {
                SaveMemory1981.Plan plan=memoryPlans1981.get(job);
                if(plan==null||!plan.valid())return false;
                Long live=liveNative1981.get(job);
                long cost=nativeCosts.get(job).longValue(),resident=live==null?0:Math.min(cost,Math.max(0,live.longValue()));
                nativeBytes=SaveMemory1981.add(nativeBytes,cost-resident);
                model=Math.max(model,plan.modelBytes);workspace=Math.max(workspace,plan.workerBytes);
            }
        }
        SaveMemory1981.Plan peak=new SaveMemory1981.Plan(next.width,next.height,next.outputWidth,next.outputHeight,next.inputBytes,model,workspace);
        long proof=SaveMemory1981.add(WholeRoute1953.retainedBytes(),GpuFinish1953.retainedBytes());
        return SaveMemory1981.allows(memory,peak,phase,nativeBytes,proof,SpeedWorkers1935.nativeRetainedBytes1956(),PhotoAnalysis1981.retainedBytes1981());
    }
    /** Optional work must leave room for the foreground correction peak even
     * when its model/workspace has not yet been allocated. The observation is
     * rejected if queue ownership changed across the system-memory lookup. */
    static boolean analysisMemoryAllows1981(SaveMemory1981.Snapshot memory,long extra,long expectedGeneration) {
        SaveMemory1981.Plan largest=null;long nativeGrowth=0,model=0,workspace=0;
        if(extra<0)return false;
        synchronized(jobs) {
            // A caller reserves before PREP has copied its SDK image and
            // registered a plan. Never infer that untracked handoff's peak.
            if(expectedGeneration!=memoryGeneration1981||held!=nativeCosts.size())return false;
            for(Runnable job:nativeCosts.keySet()) {
                SaveMemory1981.Plan plan=memoryPlans1981.get(job);
                if(plan==null||!plan.valid())return false;
                if(largest==null||plan.pixels>largest.pixels)largest=plan;
                model=Math.max(model,plan.modelBytes);workspace=Math.max(workspace,plan.workerBytes);
                long cost=nativeCosts.get(job).longValue();Long live=liveNative1981.get(job);
                long resident=live==null?0:Math.min(cost,Math.max(0,live.longValue()));
                nativeGrowth=SaveMemory1981.add(nativeGrowth,cost-resident);
            }
        }
        if(largest==null)return false;
        SaveMemory1981.Plan peak=new SaveMemory1981.Plan(largest.width,largest.height,largest.outputWidth,largest.outputHeight,largest.inputBytes,model,workspace);
        long retained=PhotoAnalysis1981.retainedBytes1981();
        long proof=SaveMemory1981.add(WholeRoute1953.retainedBytes(),GpuFinish1953.retainedBytes());
        boolean allowed=SaveMemory1981.allows(memory,peak,SaveMemory1981.ANALYSIS,nativeGrowth,proof,
            SpeedWorkers1935.nativeRetainedBytes1956(),SaveMemory1981.add(retained,extra));
        synchronized(jobs){return allowed&&expectedGeneration==memoryGeneration1981&&held==nativeCosts.size()&&retained==PhotoAnalysis1981.retainedBytes1981();}
    }
    static void submit(Runnable job){submit(job,0,0);}
    static void submit(Runnable job,long pixelCount){submit(job,pixelCount,pixelCount*8L);}
    static void submit(Runnable job,long pixelCount,long nativeBudget){
        submit1981(job,pixelCount,nativeBudget,null,0);
    }
    static void submit1981(Runnable job,long pixelCount,long nativeBudget,SaveMemory1981.Plan memoryPlan,long verifiedLive){
        synchronized(jobs){
            if(jobs.size()>=CAPACITY)throw new IllegalStateException("save queue capacity");
            // If the second worker cannot start, the first remains a valid serial
            // fallback. If no worker can start, the caller still owns the image.
            while(workers<2){
                Thread made=new Thread(new Runnable(){public void run(){drain();}},"ULike-save1944-"+workers);
                made.setDaemon(true);
                try{made.start();workers++;}
                catch(RuntimeException failure){if(workers==0)throw failure;break;}
                catch(OutOfMemoryError failure){if(workers==0)throw failure;break;}
            }
            memoryGeneration1981++;pixels.put(job,Long.valueOf(pixelCount));
            try{nativeCosts.put(job,Long.valueOf(Math.max(0,nativeBudget)));liveNative1981.put(job,Long.valueOf(Math.max(0,Math.min(nativeBudget,verifiedLive))));if(memoryPlan!=null)memoryPlans1981.put(job,memoryPlan);publication1956.addLast(job);jobs.addLast(job);}catch(RuntimeException failure){publication1956.remove(job);jobs.remove(job);pixels.remove(job);nativeCosts.remove(job);memoryPlans1981.remove(job);liveNative1981.remove(job);throw failure;}catch(OutOfMemoryError failure){publication1956.remove(job);jobs.remove(job);pixels.remove(job);nativeCosts.remove(job);memoryPlans1981.remove(job);liveNative1981.remove(job);throw failure;}
            jobs.notifyAll();
        }
    }
    /** The current job has exact final pixels. Wait for the previous writer to
     * finish, acquire the encoder, then let the next owned photo prepare. */
    static void beginEncoding(Runnable job){
        boolean interrupted=false;
        synchronized(jobs){
            if(encoding==job)return;
            if(preparing!=job)throw new IllegalStateException("save preparation ownership");
            while(encoding!=null||(codec!=null&&codec!=job))try{jobs.wait();}catch(InterruptedException e){interrupted=true;}
            codec=job;encoding=job;preparing=null;jobs.notifyAll();
        }
        if(interrupted)Thread.currentThread().interrupt();
    }
    /** Claim hardware preparation without releasing the correction stage. A
     * later photo keeps correcting while an earlier codec is occupied, and
     * defers its own constructor until its FIFO encoding turn. */
    static boolean tryCodecPreparation(Runnable job){synchronized(jobs){
        if(preparing!=job&&encoding!=job)throw new IllegalStateException("codec preparation ownership");
        if(codec!=null&&codec!=job)return false;
        if(encoding!=null&&encoding!=job)return false;
        codec=job;return true;
    }}
    /** Register and claim under the same lock, so a writer-close notification
     * between a busy probe and registration cannot strand the next constructor. */
    static void deferCodecPreparation1981(Runnable job,Runnable available) {
        if(available==null)throw new NullPointerException("codec availability callback");
        Runnable wake;
        synchronized(jobs) {
            if(preparing!=job&&encoding!=job)throw new IllegalStateException("codec preparation ownership");
            if(codecWaiters1981.containsKey(job))throw new IllegalStateException("duplicate codec waiter");
            codecWaiters1981.put(job,available);
            if((encoding==null||encoding==job)&&(codec==null||codec==job)) {
                codec=job;wake=codecWaiters1981.remove(job);
            } else wake=null;
        }
        wakeCodec1981(wake);
    }
    static void cancelCodecPreparation1981(Runnable job){synchronized(jobs){codecWaiters1981.remove(job);}}
    /** Only the currently preparing FIFO photo may reserve a released codec.
     * No application/Executor code runs while holding the queue lock. */
    private static Runnable takeCodecWaiter1981() {
        if(encoding!=null||codec!=null||preparing==null)return null;
        Runnable wake=codecWaiters1981.remove(preparing);
        if(wake!=null)codec=preparing;
        return wake;
    }
    private static void wakeCodec1981(Runnable wake) {
        if(wake==null)return;
        // The new job owns dispatch failures. Its encoding boundary retries or
        // observes its recorded failure; never fail the preceding saved photo.
        try{wake.run();}catch(RuntimeException ignored){}catch(Error ignored){}
    }
    /** Called only after CodecDrain confirmed this encoding writer's close and
     * actual hardware release. Validation, staged-file ownership and its receipt
     * remain live; a following FIFO photo can now use the hardware encoder. */
    static boolean hardwareClosed1956(Runnable job){
        Runnable wake;
        synchronized(jobs){
            if(encoding!=job)return false;
            encoding=null;if(codec==job)codec=null;
            wake=takeCodecWaiter1981();jobs.notifyAll();
        }
        wakeCodec1981(wake);return true;
    }
    /** Terminal failures before encoding also take their submission-ordered turn.
     * The caller has joined its own abandoned constructor and close barrier first.
     * No file success is inferred from giving up this resource ownership. */
    static void terminalTurn1956(Runnable job){
        boolean interrupted=false;
        Runnable wake;
        synchronized(jobs){
            if(!publication1956.contains(job))throw new IllegalStateException("save terminal ownership");
            codecWaiters1981.remove(job);
            if(preparing==job){
                while(encoding!=null||(codec!=null&&codec!=job))try{jobs.wait();}catch(InterruptedException e){interrupted=true;}
                preparing=null;
            }
            if(encoding==job)encoding=null;
            if(codec==job)codec=null;
            wake=takeCodecWaiter1981();
            jobs.notifyAll();
        }
        wakeCodec1981(wake);
        if(interrupted)Thread.currentThread().interrupt();
        awaitPublication1956(job);
    }
    /** Must precede any existing public file/MediaStore visibility transition.
     * Interrupts do not cancel a save already admitted; retain the original
     * terminal cleanup and restore the flag after its FIFO turn is reached. */
    static void awaitPublication1956(Runnable job){
        boolean interrupted=false;
        synchronized(jobs){
            if(!publication1956.contains(job))throw new IllegalStateException("save publication ownership");
            while(publication1956.peekFirst()!=job)try{jobs.wait();}catch(InterruptedException e){interrupted=true;}
        }
        if(interrupted)Thread.currentThread().interrupt();
    }
    public static boolean idle1953(){synchronized(jobs){return held==0&&jobs.isEmpty()&&publication1956.isEmpty()&&codecWaiters1981.isEmpty()&&preparing==null&&encoding==null&&codec==null;}}
    static boolean analysisWaiting1981(Runnable job){synchronized(jobs){return preparing!=null&&preparing!=job&&jobs.contains(job);}}
    private static void finishStages(Runnable job){Runnable wake;synchronized(jobs){if(preparing==job)preparing=null;if(encoding==job)encoding=null;if(codec==job)codec=null;codecWaiters1981.remove(job);publication1956.remove(job);memoryGeneration1981++;pixels.remove(job);nativeCosts.remove(job);memoryPlans1981.remove(job);liveNative1981.remove(job);wake=takeCodecWaiter1981();jobs.notifyAll();}wakeCodec1981(wake);WholeRoute1953.wake();}
    private static void drain(){
        for(;;){
            Runnable next;
            synchronized(jobs){
                while(jobs.isEmpty()||preparing!=null)try{jobs.wait();}catch(InterruptedException ignored){}
                next=jobs.removeFirst();preparing=next;
            }
            // A failed callback/helper must not permanently reduce the fixed worker
            // count. Every admitted Job owns its cleanup in finally; all Error
            // subclasses still pass through finishStages before the next Job.
            try{next.run();}catch(RuntimeException ignored){}catch(Error ignored){}
            finally{finishStages(next);next=null;}
        }
    }
}
