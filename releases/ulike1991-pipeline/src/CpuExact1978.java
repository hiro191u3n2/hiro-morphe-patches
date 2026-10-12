package com.hiro.ulike;

import java.util.Arrays;

/** Admission for optional exact CPU reuse. Only detached inputs or fully
 * accounted immutable shot metadata reach the existing after-save queue.
 * Every candidate needs two full equal
 * results, a stable original result and a measured five-percent complete-call
 * improvement. An unknown, cancelled, unavailable or slower candidate stays off. */
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
    static boolean canCapture(String key,long retained,long peak) {
        if(retained<1||peak<0||retained>96L*1024*1024||retained>Long.MAX_VALUE-peak||
                Thread.currentThread().isInterrupted()||background())return false;
        try{return GpuQualification1961.canQueue(key,retained)&&GpuNoise1960.workspaceFits(retained+peak);}
        catch(RuntimeException unavailable){return false;}
        catch(LinkageError unavailable){return false;}
        catch(OutOfMemoryError unavailable){return false;}
    }
    static void offer(String key,long retained,long peak,Factory factory) {
        try{synchronized(SNAPSHOTS) {
            if(!canCapture(key,retained,peak))return;
            Work work=factory.create();if(work==null)return;
            Probe probe=new Probe(key,peak,work);
            try{if(!GpuQualification1961.schedule(key,retained,probe))probe.close();}
            catch(RuntimeException unavailable){probe.close();}
            catch(LinkageError unavailable){probe.close();}
            catch(OutOfMemoryError unavailable){probe.close();}
        }}catch(RuntimeException unavailable){}
          catch(LinkageError unavailable){}
          catch(OutOfMemoryError unavailable){}
    }
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
                }
                if(!same(output[0],output[1])||trial==1&&!same(firstReference,output[0])) {
                    if(!c.cancelled())GpuQualification1961.rejectExact(key);return;
                }
                if(trial==0)firstReference=output[0];
                if(trial>=0){fastestOld=Math.min(fastestOld,elapsed[0]);slowestNew=Math.max(slowestNew,elapsed[1]);}
            }
            if(c.cancelled())return;
            if(slowestNew<=fastestOld-fastestOld/20)GpuQualification1961.qualified(key,fastestOld,slowestNew,1);
            else GpuQualification1961.rejectSpeed(key);
        }
        public void close(){if(work!=null){work.close();work=null;}}
    }
}
