package com.hiro.ulike;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/** GX1-GX8 real compute engine. Every candidate remains private until the exact
 * whole-route owner admits it. No precision substitute is accepted by this API.
 * One private EGL-owner thread; per-photo persistent GPU SSBOs; bounded memory.
 * Camera/native codec/GPU interop is separately capability checked, never assumed. */
public final class GpuNoise1960 {
    private GpuNoise1960() { }
    public static final int STRONG=0,SINGLE=1,ANALYSIS=2,GEOMETRY=3;
    public static final long MAX_BYTES=512L*1024*1024;
    private static volatile int loaded;
    private static volatile long retained;
    private static volatile boolean activeSession;
    private static volatile String environment="";
    private static final ExecutorService OWNER=Executors.newSingleThreadExecutor(new ThreadFactory(){
        public Thread newThread(Runnable task){Thread t=new Thread(task,"Hiro-ULike-GX1960");t.setDaemon(true);return t;}
    });
    static boolean available(){
        if(loaded==0)synchronized(GpuNoise1960.class){if(loaded==0){
            try{System.loadLibrary("ulike_gpu1960");loaded=nativeAbi()==19601?1:-1;}
            catch(LinkageError unavailable){loaded=-1;}
            catch(SecurityException unavailable){loaded=-1;}
        }}
        return loaded>0;
    }
    public static long retainedBytes(){return retained;}
    static boolean sessionBusy(){return activeSession;}
    /** Native GPU allocations share the capture's physical memory allowance.
     * The heap figures already include all live Java buffers; callers pass only
     * their additional peak allocation, including private qualification results. */
    static boolean workspaceFits(long extra){
        if(extra<0||extra>MAX_BYTES)return false;
        Runtime r=Runtime.getRuntime();
        long available=r.maxMemory()-(r.totalMemory()-r.freeMemory());
        long[] owners={retained,WholeRoute1953.retainedBytes(),GpuFinish1953.retainedBytes(),
            SpeedWorkers1935.nativeRetainedBytes1956(),64L*1024*1024};
        for(long bytes:owners){if(bytes<0||bytes>available)return false;available-=bytes;}
        return extra<=available;
    }
    static String fingerprint(){return environment;}
    private static void refresh(){retained=retainedNative();String e=environmentNative();environment=e==null?"":e;}
    private static <T> T owner(Callable<T> task,T fallback){
        Future<T> f;
        try{f=OWNER.submit(task);}catch(RuntimeException unavailable){return fallback;}catch(OutOfMemoryError unavailable){return fallback;}
        boolean interrupted=false;
        try{for(;;)try{return f.get();}catch(InterruptedException cancelled){interrupted=true;}catch(ExecutionException failed){
            if(failed.getCause() instanceof LinkageError)loaded=-1;return fallback;
        }}finally{if(interrupted)Thread.currentThread().interrupt();}
    }
    static void warmupAsync(){
        if(!available())return;
        try{OWNER.execute(new Runnable(){public void run(){try{supportedNative(STRONG);refresh();}catch(LinkageError unavailable){loaded=-1;}catch(RuntimeException unavailable){}}});}
        catch(RuntimeException unavailable){}catch(OutOfMemoryError unavailable){}
    }
    static boolean supports(final int shader){
        if(shader<0||shader>3||!available()||Thread.currentThread().isInterrupted())return false;
        return owner(new Callable<Boolean>(){public Boolean call(){try{return supportedNative(shader);}finally{refresh();}}},Boolean.FALSE);
    }
    static Session open(){
        if(!available()||Thread.currentThread().isInterrupted())return null;
        return owner(new Callable<Session>(){public Session call(){
            long token=openNative();refresh();if(token==0)return null;activeSession=true;
            boolean passed=false;try{Session s=new Session(token);passed=true;return s;}finally{if(!passed){if(closeNative(token))activeSession=false;refresh();}}
        }},null);
    }
    static final class Session implements AutoCloseable {
        private final long token;
        private boolean failed,closed;
        private long dispatched;
        private Session(long token){this.token=token;}
        private boolean usable(){return !closed&&!failed&&!Thread.currentThread().isInterrupted();}
        synchronized boolean allocate(final int slot,final long bytes){
            if(!usable()||slot<0||slot>=24||bytes<=0||bytes>MAX_BYTES)return false;
            boolean ok=owner(new Callable<Boolean>(){public Boolean call(){try{return allocateNative(token,slot,bytes);}finally{refresh();}}},Boolean.FALSE);
            if(!ok)failed=true;return ok;
        }
        synchronized boolean upload(final int slot,final int[] values){
            if(!usable()||slot<0||slot>=24||values==null||values.length==0||(long)values.length*4>MAX_BYTES)return false;
            boolean ok=owner(new Callable<Boolean>(){public Boolean call(){try{return uploadIntsNative(token,slot,values);}finally{refresh();}}},Boolean.FALSE);
            if(!ok)failed=true;return ok;
        }
        synchronized boolean upload(final int slot,final float[] values){
            if(!usable()||slot<0||slot>=24||values==null||values.length==0||(long)values.length*4>MAX_BYTES)return false;
            boolean ok=owner(new Callable<Boolean>(){public Boolean call(){try{return uploadFloatsNative(token,slot,values);}finally{refresh();}}},Boolean.FALSE);
            if(!ok)failed=true;return ok;
        }
        /** u[31] is reserved for the native invocation offset. Kernel workgroup width is read from its linked program; Y/Z must be1.
         * Each kernel explicitly bounds total pixels/blocks in its parameters. */
        synchronized boolean dispatch(final int shader,final int[] bindings,final int[] u,final float[] f,final int invocations){
            if(!usable()||shader<0||shader>3||bindings==null||bindings.length<1||bindings.length>8||u==null||u.length<1||u.length>32||f!=null&&f.length>32||invocations<1||(long)invocations>MAX_BYTES/4)return false;
            for(int slot:bindings)if(slot<0||slot>=24)return false;
            boolean ok=owner(new Callable<Boolean>(){public Boolean call(){return dispatchNative(token,shader,bindings,u,f,invocations);}},Boolean.FALSE);
            if(!ok)failed=true;else dispatched+=invocations;return ok;
        }
        synchronized int[] readInts(final int slot,final int count){
            if(!usable()||slot<0||slot>=24||count<1||(long)count*4>MAX_BYTES)return null;
            int[] result=owner(new Callable<int[]>(){public int[] call(){return readIntsNative(token,slot,count);}},null);
            if(result==null)failed=true;return result;
        }
        synchronized String stats(){return "gx1960 dispatched="+dispatched+" residentBytes="+retained+" failed="+failed;}
        public synchronized void close(){
            if(closed)return;
            boolean ok=owner(new Callable<Boolean>(){public Boolean call(){try{boolean done=closeNative(token);if(done)activeSession=false;return done;}finally{refresh();}}},Boolean.FALSE);
            if(!ok)failed=true;closed=true;
        }
    }
    private static native int nativeAbi();
    private static native long openNative();
    private static native boolean supportedNative(int id);
    private static native long retainedNative();
    private static native String environmentNative();
    private static native boolean allocateNative(long token,int slot,long bytes);
    private static native boolean uploadIntsNative(long token,int slot,int[] values);
    private static native boolean uploadFloatsNative(long token,int slot,float[] values);
    private static native boolean dispatchNative(long token,int shader,int[] bindings,int[] u,float[] f,int invocations);
    private static native int[] readIntsNative(long token,int slot,int count);
    private static native boolean closeNative(long token);
}
