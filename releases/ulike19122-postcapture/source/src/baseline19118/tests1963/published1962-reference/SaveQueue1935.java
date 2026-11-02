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
    private static int held,workers;
    private static Runnable preparing,encoding,codec;
    /** Called off the UI/camera thread before acquiring/copying the native bitmap. */
    static void reserve() {
        boolean interrupted=false;
        synchronized(jobs) {
            while(held>=CAPACITY)try{jobs.wait();}catch(InterruptedException e){interrupted=true;}
            held++;
        }
        if(interrupted)Thread.currentThread().interrupt();
    }
    static void release(){synchronized(jobs){if(held<=0)throw new IllegalStateException("save ownership");held--;jobs.notifyAll();}}
    public static int count(){synchronized(jobs){return held;}}
    public static long maximumPixels(){synchronized(jobs){long maximum=0;for(Long n:pixels.values())maximum=Math.max(maximum,n.longValue());return maximum;}}
    public static long nativeBytes(){synchronized(jobs){long total=0;for(Long n:nativeCosts.values()){long value=n.longValue();if(value>Long.MAX_VALUE-total)return Long.MAX_VALUE;total+=value;}return total;}}
    static void nativeCost(Runnable job,long bytes){synchronized(jobs){if(nativeCosts.containsKey(job))nativeCosts.put(job,Long.valueOf(Math.max(0,bytes)));}}
    public static boolean memoryAllows(long available,long pixelCount){
        // Existing four NV21/fusion/renderer/output reserve plus the concurrently
        // active encoder bitmap/workspace. Android 8+ Bitmap pixels use native
        // heap, so Runtime.available does NOT account for queued source/final
        // images: explicitly subtract each job's tracked native bitmap budget.
        long queued=nativeBytes(),proof=WholeRoute1953.retainedBytes(),gpu=GpuFinish1953.retainedBytes(),scratch=SpeedWorkers1935.nativeRetainedBytes1956();
        if(proof>Long.MAX_VALUE-queued)return false;
        queued+=proof;
        if(gpu>Long.MAX_VALUE-queued)return false;
        queued+=gpu;
        if(scratch>Long.MAX_VALUE-queued)return false;
        queued+=scratch;
        return pixelCount>0 && pixelCount<=32000000L && available>=queued && available-queued>=RESERVE+pixelCount*24L;
    }
    static void submit(Runnable job){submit(job,0,0);}
    static void submit(Runnable job,long pixelCount){submit(job,pixelCount,pixelCount*8L);}
    static void submit(Runnable job,long pixelCount,long nativeBudget){
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
            pixels.put(job,Long.valueOf(pixelCount));
            try{nativeCosts.put(job,Long.valueOf(Math.max(0,nativeBudget)));publication1956.addLast(job);jobs.addLast(job);}catch(RuntimeException failure){publication1956.remove(job);jobs.remove(job);pixels.remove(job);nativeCosts.remove(job);throw failure;}catch(OutOfMemoryError failure){publication1956.remove(job);jobs.remove(job);pixels.remove(job);nativeCosts.remove(job);throw failure;}
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
    /** Called only after CodecDrain confirmed this encoding writer's close and
     * actual hardware release. Validation, staged-file ownership and its receipt
     * remain live; a following FIFO photo can now use the hardware encoder. */
    static boolean hardwareClosed1956(Runnable job){synchronized(jobs){
        if(encoding!=job)return false;
        encoding=null;if(codec==job)codec=null;jobs.notifyAll();return true;
    }}
    /** Terminal failures before encoding also take their submission-ordered turn.
     * The caller has joined its own abandoned constructor and close barrier first.
     * No file success is inferred from giving up this resource ownership. */
    static void terminalTurn1956(Runnable job){
        boolean interrupted=false;
        synchronized(jobs){
            if(!publication1956.contains(job))throw new IllegalStateException("save terminal ownership");
            if(preparing==job){
                while(encoding!=null||(codec!=null&&codec!=job))try{jobs.wait();}catch(InterruptedException e){interrupted=true;}
                preparing=null;
            }
            if(encoding==job)encoding=null;
            if(codec==job)codec=null;
            jobs.notifyAll();
        }
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
    public static boolean idle1953(){synchronized(jobs){return held==0&&jobs.isEmpty()&&publication1956.isEmpty()&&preparing==null&&encoding==null&&codec==null;}}
    private static void finishStages(Runnable job){synchronized(jobs){if(preparing==job)preparing=null;if(encoding==job)encoding=null;if(codec==job)codec=null;publication1956.remove(job);pixels.remove(job);nativeCosts.remove(job);jobs.notifyAll();}WholeRoute1953.wake();}
    private static void drain(){
        for(;;){
            Runnable next;
            synchronized(jobs){
                while(jobs.isEmpty()||preparing!=null)try{jobs.wait();}catch(InterruptedException ignored){}
                next=jobs.removeFirst();preparing=next;
            }
            try{next.run();}catch(RuntimeException ignored){}catch(LinkageError ignored){}catch(OutOfMemoryError ignored){}
            finally{finishStages(next);next=null;}
        }
    }
}
