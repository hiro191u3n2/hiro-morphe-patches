package com.hiro.ulike;

import java.util.Arrays;

/** Restore already-proven exact CPU reuse. Normal captures no longer collect
 * CPU workspace/model/policy trials: their pixel copies cost foreground time
 * even when the later idle benchmark is cancelled or finds no speed advantage.
 * Unknown, cancelled, unavailable or slower candidates keep the original path.
 * The owned-input verifier below retains the exactness and speed contract. */
final class CpuExact1978 {
    private CpuExact1978() {}
    private static final Object SNAPSHOTS=new Object();
    interface Work {Object run(boolean reused);void close();}
    interface Factory {Work create();}
    static boolean enabled(String key) {
        try {
            GpuQualification1961.Record p=GpuQualification1961.restore(key);
            return p!=null&&p.variant==1&&p.cpuNanos>0&&p.gpuNanos>0&&
                p.gpuNanos<=p.cpuNanos-p.cpuNanos/20;
        } catch(RuntimeException unavailable){return false;}
          catch(LinkageError unavailable){return false;}
    }
    static boolean background() {
        try{return GpuQualification1961.background();}
        catch(RuntimeException unavailable){return true;}
        catch(LinkageError unavailable){return true;}
    }
    /** CPU-only learning is closed before callers allocate patch captures.
     * GPU stage qualification also uses this shared admission helper; keep its
     * existing queue and peak-memory gates rather than disabling GPU learning. */
    static boolean canCapture(String key,long retained,long peak) {
        if(key==null||key.startsWith("cpu-"))return false;
        if(retained<1||peak<0||retained>96L*1024*1024||retained>Long.MAX_VALUE-peak||
                Thread.currentThread().isInterrupted()||background())return false;
        try{return GpuQualification1961.canQueue(key,retained)&&GpuNoise1960.workspaceFits(retained+peak);}
        catch(RuntimeException unavailable){return false;}
        catch(LinkageError unavailable){return false;}
        catch(OutOfMemoryError unavailable){return false;}
    }
    /** Keep source compatibility with original CPU callers, without invoking a
     * factory, retaining shot metadata or consuming the GPU verification queue. */
    static void offer(String key,long retained,long peak,Factory factory) {}
    static boolean same(Object a,Object b) {
        if(a==null||b==null)return false;
        if(a instanceof int[]&&b instanceof int[])return Arrays.equals((int[])a,(int[])b);
        if(a instanceof float[]&&b instanceof float[]) {
            float[] x=(float[])a,y=(float[])b;if(x.length!=y.length)return false;
            for(int i=0;i<x.length;i++)if(Float.floatToRawIntBits(x[i])!=Float.floatToRawIntBits(y[i]))return false;
            return true;
        }
        if(a instanceof Object[]&&b instanceof Object[]) {
            Object[] x=(Object[])a,y=(Object[])b;if(x.length!=y.length)return false;
            for(int i=0;i<x.length;i++)if(!same(x[i],y[i]))return false;
            return true;
        }
        return false;
    }
    /** A Work may return a reusable scratch array. Preserve the completed
     * value outside the measured call before another trial can overwrite it. */
    private static Object freeze1996(Object value) {
        if(value instanceof int[])return ((int[])value).clone();
        if(value instanceof float[])return ((float[])value).clone();
        if(value instanceof Object[]) {
            Object[] original=(Object[])value,result=new Object[original.length];
            for(int i=0;i<original.length;i++) {
                result[i]=freeze1996(original[i]);if(result[i]==null)return null;
            }
            return result;
        }
        return null;
    }
    private static final class Probe implements GpuQualification1961.Probe {
        final String key;final long peak;Work work;
        Probe(String key,long peak,Work work){this.key=key;this.peak=peak;this.work=work;}
        public void run(GpuQualification1961.Cancellation c) {
            if(work==null||c.cancelled()||!GpuNoise1960.workspaceFits(peak))return;
            Object firstReference=null;long fastestOld=Long.MAX_VALUE,slowestNew=0;
            for(int trial=-1;trial<2;trial++) {
                Object[] output=new Object[2];long[] elapsed=new long[2];
                for(int turn=0;turn<2;turn++) {
                    if(c.cancelled())return;int choice=(trial&1)==0?turn:1-turn;
                    long start=System.nanoTime();output[choice]=work.run(choice==1);
                    elapsed[choice]=Math.max(1L,System.nanoTime()-start);
                    if(output[choice]==null||c.cancelled())return;
                    output[choice]=freeze1996(output[choice]);
                    if(output[choice]==null||c.cancelled())return;
                }
                if(!same(output[0],output[1])||trial==1&&!same(firstReference,output[0])) {
                    if(!c.cancelled())GpuQualification1961.rejectExact(key);return;
                }
                if(trial==0)firstReference=output[0];
                if(trial>=0){fastestOld=Math.min(fastestOld,elapsed[0]);slowestNew=Math.max(slowestNew,elapsed[1]);}
            }
            if(c.cancelled())return;
            if(slowestNew<=fastestOld-fastestOld/20)GpuQualification1961.qualified(key,fastestOld,slowestNew,1);
            else GpuQualification1961.rejectMeasuredSpeed1992(key,"CPU作業領域再利用",fastestOld,slowestNew);
        }
        public void close(){if(work!=null){work.close();work=null;}}
    }
}
