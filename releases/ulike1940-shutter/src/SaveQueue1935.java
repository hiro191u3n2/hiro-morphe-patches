package com.hiro.ulike;

import java.util.ArrayDeque;

/** Exactly one quality/encoder worker and up to four owned actual photos.
 * No shutter-tap queue and no delayed or automatic camera requests. */
public final class SaveQueue1935 {
    private SaveQueue1935() {}
    public static final int CAPACITY=4;
    public static final long RESERVE=72L*1024*1024;
    private static final ArrayDeque<Runnable> jobs=new ArrayDeque<Runnable>();
    private static int held;
    private static Thread worker;
    /** Never wait on the shutter/UI thread. Admission holds actual photos only. */
    static boolean tryReserve() {
        synchronized(jobs) {
            if(held>=CAPACITY)return false;
            held++;
            return true;
        }
    }
    /** Kept for binary compatibility; capacity pressure is fail-fast. */
    static void reserve() {
        if(!tryReserve())throw new IllegalStateException("save capacity");
    }
    static void release(){synchronized(jobs){if(held<=0)throw new IllegalStateException("save ownership");held--;jobs.notifyAll();}}
    public static int count(){synchronized(jobs){return held;}}
    public static boolean memoryAllows(long available,long pixels){
        // Legacy/pre-Android 8 unified allocation bound. Modern Android admission
        // separates the Java YUV and native Bitmap budgets in CaptureMemory1940.
        return pixels>0 && pixels<=32000000L && available>=RESERVE+pixels*20L;
    }
    /** The early handoff can overlap next-shot acquisition with this shot's
     * normalization. Reserve its full output allocation in addition to the
     * existing capture/fusion workspace, without reducing any image dimensions. */
    public static boolean processingMemoryAllows(long available,long pixels,long outputPixels){
        return outputPixels>0 && outputPixels<=32000000L
            && memoryAllows(available-outputPixels*4L,pixels);
    }
    static void submit(Runnable job){
        synchronized(jobs){
            if(jobs.size()>=CAPACITY)throw new IllegalStateException("save queue capacity");
            jobs.addLast(job);
            if(worker==null){
                Thread made=new Thread(new Runnable(){public void run(){drain();}},"ULike-save1935");
                made.setDaemon(true);
                try{made.start();worker=made;}catch(RuntimeException e){jobs.removeLastOccurrence(job);throw e;}catch(OutOfMemoryError e){jobs.removeLastOccurrence(job);throw e;}
            }
            jobs.notifyAll();
        }
    }
    private static void drain(){
        for(;;){
            Runnable next;
            synchronized(jobs){
                while(jobs.isEmpty())try{jobs.wait();}catch(InterruptedException ignored){}
                next=jobs.removeFirst();
            }
            try{next.run();}catch(RuntimeException ignored){}catch(LinkageError ignored){}catch(OutOfMemoryError ignored){}
            finally{next=null;} // Do not retain a completed shot/Activity while idle.
        }
    }
}
