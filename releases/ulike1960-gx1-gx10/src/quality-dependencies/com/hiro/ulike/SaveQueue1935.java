package com.hiro.ulike;

import java.util.ArrayDeque;

/** Exactly one encoder plus one captured waiting photo. No shutter-tap queue. */
public final class SaveQueue1935 {
    private SaveQueue1935() {}
    public static final int CAPACITY=2;
    public static final long RESERVE=72L*1024*1024;
    private static final ArrayDeque<Runnable> jobs=new ArrayDeque<Runnable>();
    private static int held;
    private static Thread worker;
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
    public static boolean memoryAllows(long available,long pixels){
        // Four NV21 inputs + fusion + renderer/output workspaces. Queued bitmap
        // is already allocated and therefore already excluded from available.
        return pixels>0 && pixels<=32000000L && available>=RESERVE+pixels*20L;
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
