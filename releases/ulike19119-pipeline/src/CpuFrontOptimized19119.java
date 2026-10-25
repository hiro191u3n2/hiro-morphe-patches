package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** Optional protected-block arithmetic. Old CPU proofs never admit this engine.
 * One owned strip is learned after save, with full output comparisons and the
 * same conservative best-old/worst-new 5% threshold as the existing CPU trials.
 * The original native library remains the independent oracle and fallback. */
final class CpuFrontOptimized19119 {
    private CpuFrontOptimized19119() {}
    private static final Object SNAPSHOTS=new Object();
    private static volatile int loaded;
    private static volatile String identity;
    private static volatile long offeredEpoch=Long.MIN_VALUE;
    private static boolean available() {
        if(loaded==0)synchronized(SNAPSHOTS){if(loaded==0)try {
            System.loadLibrary("ulike_front19119");String fingerprint=nativeFingerprint();
            if(nativeAbi()!=19119||fingerprint==null||!fingerprint.matches("[a-f0-9]{64}")){loaded=-1;return false;}
            identity=fingerprint;loaded=1;
        }catch(LinkageError unavailable){loaded=-1;}catch(SecurityException unavailable){loaded=-1;}}
        return loaded==1;
    }
    private static boolean foreground(){return !Thread.currentThread().isInterrupted()&&!GpuQualification1961.background();}
    /** All geometry remains in the key. Protection coverage is an additional
     * performance class, not a substitute for pixel comparison. Sparse masks
     * and absent protection do not collect an expensive optional snapshot. */
    static int coverage(int[] u,int[] policy) {
        if(u==null||u.length!=15||policy==null||u[0]<1||u[0]>8192||u[1]<1||u[2]<0||u[3]<=u[2]||u[3]>u[1]||u[3]-u[2]>256)return -1;
        long count=(long)u[0]*(u[3]-u[2]);if(count>Integer.MAX_VALUE/2||policy.length<count*2)return -1;
        int zeros=0;
        for(int i=0;i<(int)count;i++) {
            if((i&4095)==0&&Thread.currentThread().isInterrupted())return -1;
            if(policy[i*2]<=0||policy[i*2+1]>=256)zeros++;
        }
        return zeros<count/4||zeros==0?-1:(int)(4L*zeros/count);
    }
    private static String key(int[] u,int coverage,boolean reused) {
        return "front19119-zero-blend-exact2-time5-v1:"+identity+":"+reused+":"+coverage+":"+Arrays.toString(u);
    }
    static boolean enabled(String key) {
        GpuQualification1961.Record p=GpuQualification1961.restore(key);
        return p!=null&&p.variant==1&&p.cpuNanos>0&&p.gpuNanos>0&&p.gpuNanos<=p.cpuNanos-p.cpuNanos/20;
    }
    static boolean tryProcess(int[] source,int[] output,int[] u,float[] model,int[] policy,SingleNoise1955.Workspace workspace) {
        try {
            if(!foreground())return false;int coverage=coverage(u,policy);
            if(coverage<0||!available())return false;String key=key(u,coverage,workspace!=null);
            if(!enabled(key))return false;
            // Candidate scratch is private to the call. Retire the old owned
            // scratch before entering it; never hold both workspaces at once.
            if(workspace!=null)workspace.dropNative();
            if(!invoke(source,output,u,model,policy)||!foreground()||!enabled(key))return false;
            try {ProcessingTiming1947.class.getMethod("noiseOptimization19119",ProcessingTiming1947.Trace.class,Integer.TYPE,Long.TYPE,Long.TYPE)
                .invoke(null,ProcessingTiming1947.current(),Integer.valueOf(1),Long.valueOf(1),Long.valueOf((long)u[0]*(u[3]-u[2])));}
            catch(Throwable diagnosticOnly){}
            return true;
        }catch(RuntimeException unavailable){return false;}catch(LinkageError unavailable){loaded=-1;return false;}
          catch(OutOfMemoryError unavailable){return false;}
    }
    static void offer(int[] source,int[] u,float[] model,int[] policy,boolean reused) {
        if(source==null||model==null)return;
        try {
            if(!foreground()||offeredEpoch==ProcessingTiming1947.captureEpoch1953())return;
            int coverage=coverage(u,policy);if(coverage<0||!available())return;
            long count=(long)u[0]*u[1],core=(long)u[0]*(u[3]-u[2]);
            if(count<1||count>source.length||count>Integer.MAX_VALUE)return;
            String key=key(u,coverage,reused);if(enabled(key))return;
            long retained=4L*(count+model.length+core*2)+4096,peak=peak(u,model.length);
            if(retained>32L*1024*1024||!GpuNoise1960.workspaceFits(retained+peak))return;
            synchronized(SNAPSHOTS) {
                long epoch=ProcessingTiming1947.captureEpoch1953();if(offeredEpoch==epoch)return;
                GpuQualification1961.Reservation1984 reservation=GpuQualification1961.reserve1985(key,retained);
                try {
                    if(!reservation.accepted()||!reservation.begin1985())return;
                    offeredEpoch=epoch;
                    int[] copy=Arrays.copyOf(source,(int)count),geometry=u.clone(),mask=Arrays.copyOf(policy,(int)core*2);
                    float[] evidence=model.clone();
                    if(!reservation.current())return;
                    reservation.commit(new Probe(key,copy,geometry,evidence,mask,reused,peak));
                }finally{reservation.close();}
            }
        }catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
    }
    private static long peak(int[] u,int modelFloats) {
        long count=(long)u[0]*u[1],core=(long)u[0]*(u[3]-u[2]);
        // Three full comparison outputs, JNI source/policy copies, transactional
        // native output and accumulator, plus an optional old owned workspace.
        return 20L*count+32L*core+4L*modelFloats+CpuSingle1978.bytes(u[0],u[3]-u[2])+1048576L;
    }
    private static boolean invoke(int[] source,int[] output,int[] u,float[] model,int[] policy) {
        return processNative(source,output,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[7],u[8]!=0,
            u[9],u[10],u[11],u[12],u[13],u[14],model,policy);
    }
    private static void check(GpuQualification1961.Cancellation c) {
        if(Thread.currentThread().isInterrupted()||c.cancelled())throw new CancellationException("front119 proof cancelled");
    }
    static final class Probe implements GpuQualification1961.Probe {
        final String key;final boolean reused;final long peak;
        int[] source,u,policy;float[] model;
        Probe(String key,int[] source,int[] u,float[] model,int[] policy,boolean reused,long peak){
            this.key=key;this.source=source;this.u=u;this.model=model;this.policy=policy;this.reused=reused;this.peak=peak;
        }
        public void run(GpuQualification1961.Cancellation cancellation) {
            check(cancellation);if(source==null||!available()||!GpuNoise1960.workspaceFits(peak))return;
            SingleNoise1955.Workspace workspace=reused?new SingleNoise1955.Workspace():null;
            if(workspace!=null)workspace.acquire();
            try {
                int[] first=null;long fastest=Long.MAX_VALUE,slowest=0;
                for(int trial=-1;trial<2;trial++) {
                    int[][] output=new int[2][];long[] elapsed=new long[2];
                    for(int turn=0;turn<2;turn++) {
                        check(cancellation);int choice=(trial&1)==0?turn:1-turn;output[choice]=new int[source.length];
                        long start=System.nanoTime();boolean ok;
                        if(choice==0)ok=CpuSingle1978.originalNativeCall19119(source,output[0],u,model,policy,workspace,false);
                        // Keep the reference's already-warm workspace across
                        // trials. Destroying it before every reference call
                        // would incorrectly charge repeated allocation to old.
                        else ok=invoke(source,output[1],u,model,policy);
                        elapsed[choice]=Math.max(1L,System.nanoTime()-start);check(cancellation);if(!ok)return;
                    }
                    if(!Arrays.equals(output[0],output[1])||trial==1&&!Arrays.equals(first,output[0])) {
                        check(cancellation);GpuQualification1961.rejectExact(key);return;
                    }
                    if(trial==0)first=output[0];
                    if(trial>=0){fastest=Math.min(fastest,elapsed[0]);slowest=Math.max(slowest,elapsed[1]);}
                }
                check(cancellation);
                if(slowest<=fastest-fastest/20)GpuQualification1961.qualified(key,fastest,slowest,1);
                else GpuQualification1961.rejectMeasuredSpeed1992(key,"前段・保護領域の不要計算省略",fastest,slowest);
            }finally{if(workspace!=null){workspace.relinquish();workspace.close();}}
        }
        public void close(){source=u=policy=null;model=null;}
    }
    private static native int nativeAbi();
    private static native String nativeFingerprint();
    private static native boolean processNative(int[] input,int[] output,int width,int rows,int begin,int end,
        int validBegin,int validEnd,int originY,int noise,boolean shadows,int modelWidth,int modelHeight,
        int columns,int modelRows,int patchWidth,int patchHeight,float[] modelData,int[] protection);
}
