package com.hiro.ulike;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/** H16/H18/H19: one private GPU-owner thread, bounded two-slot photo sessions,
 * source-halo retention by photo token, and asynchronous GPU fences. Java still
 * prepares exact noise/mask policy, while the previous submitted band executes.
 * WholeRoute1953 owns photo-level quality and speed admission. */
public final class GpuFinish1953 {
    private GpuFinish1953() {}
    private static volatile int loaded;
    private static volatile String environment="";
    private static volatile long retained;
    private static volatile boolean warming;
    private static final ExecutorService OWNER=Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable work) {
            Thread thread=new Thread(work,"Hiro-ULike-GPU-1953");
            thread.setDaemon(true);return thread;
        }
    });
    static boolean available() {
        if(loaded==0)synchronized(GpuFinish1953.class){if(loaded==0){
            try {System.loadLibrary("ulike_finish1953");loaded=nativeAbi()==19531?1:-1;}
            catch(LinkageError unavailable){loaded=-1;}
            catch(SecurityException unavailable){loaded=-1;}
        }}
        return loaded>0;
    }
    /** Cached actual vendor/renderer/GL/GLSL/capabilities/ABI/reviewed-source facts.
     * A foreground history lookup never initializes EGL or compiles a program. */
    static String environment1953() {return environment;}
    static String fingerprint() {return environment;}
    public static long retainedBytes(){return retained;}
    private static void refreshRetained(){retained=retainedNative();}
    /** Optional idle bootstrap. Failure leaves history unqualified and CPU safe. */
    static void warmupAsync() {
        if(!available())return;
        synchronized(GpuFinish1953.class){if(warming || !environment.isEmpty())return;warming=true;}
        try {OWNER.execute(new Runnable(){public void run(){
            try {if(warmupNative())cacheEnvironment();refreshRetained();}
            catch(LinkageError unavailable){loaded=-1;}
            catch(RuntimeException unavailable){} finally {warming=false;}
        }});}catch(RuntimeException unavailable){warming=false;}
    }
    private static void cacheEnvironment() {
        String actual=environmentNative();
        environment=actual==null?"":actual;
    }
    /** Waiting interruption cannot release arrays before the owner copied them.
     * Finish the short owner command, then restore the caller's interrupt flag. */
    private static <T> T owner(Callable<T> command,T unavailable) {
        Future<T> future;
        try {future=OWNER.submit(command);}
        catch(RuntimeException stopped){loaded=-1;environment="";return unavailable;}
        catch(OutOfMemoryError noQueueMemory){
            // Native leases remain active and are never reused without a drain.
            // Do not move its private EGL context onto a caller to reclaim them.
            loaded=-1;environment="";return unavailable;
        }
        boolean interrupted=false;
        try {
            for(;;)try {return future.get();}
                catch(InterruptedException cancelled){interrupted=true;}
                catch(ExecutionException failed){
                    Throwable reason=failed.getCause();
                    if(reason instanceof LinkageError)loaded=-1;
                    return unavailable;
                }
        } finally {if(interrupted)Thread.currentThread().interrupt();}
    }
    static Session open(final int width,final int height,final QualityPixels1932.Plan plan,
            final boolean moire,boolean requestedSharp,final boolean sequential,final int preferredCore) {
        if(width<1 || height<1 || plan==null || (preferredCore!=256 && preferredCore!=512) ||
                Thread.currentThread().isInterrupted() || !available())return null;
        final boolean sharp=requestedSharp && plan.sharpGainQ8>0;
        return owner(new Callable<Session>(){public Session call(){
            long token=openNative(width,height,moire,sharp,sharp?plan.sharpGainQ8:0,
                sharp?plan.sharpFloorQ8:0,sharp?plan.sharpLimit:0,
                sharp && plan.texturePriority,sharp && plan.haloSuppression,sequential,preferredCore);
            refreshRetained();
            if(token==0){cacheEnvironment();return null;}
            boolean handedOff=false;
            try {
                cacheEnvironment();
                int core=coreNative(token),halo=haloNative(token);
                if(core<1 || halo<0)return null;
                Session session=new Session(token,width,height,core,halo);
                handedOff=true;return session;
            } finally {if(!handedOff){closeNative(token);refreshRetained();}}
        }},null);
    }
    static final class Ticket {
        public final int first,last;
        private final Session session;
        private final int slot;
        private boolean collected;
        Ticket(Session session,int slot,int first,int last){
            this.session=session;this.slot=slot;this.first=first;this.last=last;
        }
    }
    static final class Session implements AutoCloseable {
        private final long token;
        private final int width,height,core,halo;
        private final Ticket[] pending=new Ticket[2];
        private boolean closed,failed;
        private String finalStats="";
        Session(long token,int width,int height,int core,int halo){
            this.token=token;this.width=width;this.height=height;this.core=core;this.halo=halo;
        }
        int coreRows(){return core;}
        int halo(){return halo;}
        /** Returns after Java input/policy are copied and GPU work is fenced, with
         * no GPU completion wait. The caller may now close Band/reuse its array. */
        synchronized Ticket submit(final int[] input,final int inputOrigin,final int inputRows,
                final int first,final int last,final FinishPolicy1953.Band policy) {
            if(closed || failed || Thread.currentThread().isInterrupted() || input==null || policy==null ||
                    first<0 || last<=first || last>height || last-first>core ||
                    inputOrigin!=Math.max(0,first-halo) ||
                    inputRows!=Math.min(height,last+halo)-inputOrigin ||
                    (long)width*inputRows>input.length || policy.pixels!=(long)width*(last-first))return null;
            int selected=pending[0]==null?0:pending[1]==null?1:-1;
            if(selected<0)return null;
            final int slot=selected;
            // Allocate Java ownership before any native fence can be submitted.
            Ticket ticket=new Ticket(this,slot,first,last);
            boolean ok=owner(new Callable<Boolean>(){public Boolean call(){
                try {return submitNative(token,slot,input,inputOrigin,inputRows,first,last,
                    policy.words,policy.mode);}finally {refreshRetained();}
            }},Boolean.FALSE);
            if(!ok){failed=true;environment="";return null;}
            pending[slot]=ticket;return ticket;
        }
        /** Only the completed ticket's linear core range is copied on success. */
        synchronized boolean collect(final Ticket ticket,final int[] output,final int outputOffset) {
            if(closed || failed || ticket==null || ticket.session!=this || ticket.collected ||
                    pending[ticket.slot]!=ticket || output==null || outputOffset<0 ||
                    (long)outputOffset+(long)width*(ticket.last-ticket.first)>output.length)return false;
            boolean ok=owner(new Callable<Boolean>(){public Boolean call(){
                try {return collectNative(token,ticket.slot,output,outputOffset);}
                finally {refreshRetained();}
            }},Boolean.FALSE);
            if(ok){pending[ticket.slot]=null;ticket.collected=true;}
            else {failed=true;environment="";}
            return ok;
        }
        synchronized String stats() {
            if(closed)return finalStats;
            return owner(new Callable<String>(){public String call(){return statsNative(token);}},"");
        }
        /** Every queued fence drains before pool reuse or cancellation, including
         * when the caller is interrupted. Unknown completion quarantines buffers. */
        public synchronized void close() {
            if(closed)return;
            finalStats=owner(new Callable<String>(){public String call(){
                try {return closeNative(token);}finally {refreshRetained();}
            }},"");
            closed=true;for(int i=0;i<2;i++)pending[i]=null;
        }
    }
    private static native int nativeAbi();
    private static native long retainedNative();
    private static native boolean warmupNative();
    private static native String environmentNative();
    private static native long openNative(int width,int height,boolean moire,boolean sharp,
        int gain,int floor,int limit,boolean texture,boolean halo,boolean sequential,int preferredCore);
    private static native int coreNative(long token);
    private static native int haloNative(long token);
    private static native boolean submitNative(long token,int slot,int[] input,int origin,int inputRows,
        int first,int last,int[] policy,int mode);
    private static native boolean collectNative(long token,int slot,int[] output,int offset);
    private static native String statsNative(long token);
    private static native String closeNative(long token);
}
