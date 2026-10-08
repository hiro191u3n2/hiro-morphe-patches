package com.hiro.ulike;

import java.util.Arrays;

/** Optional exact integer GLES kernels. Float fusion, beauty and saving remain
 * on their existing paths. A device/shape is admitted only after equality on
 * actual inputs and two faster complete calls, following one cold warmup call.
 * Admission retains timing facts only; image buffers always belong to a call.
 */
public final class GpuInteger1949 {
    private GpuInteger1949() { }
    private static final Engine ENGINE=new Engine(new NativeBackend(),new Clock());
    static boolean available(){return ENGINE.available();}
    static boolean fusionAvailable(int width,int height,int step){return ENGINE.fusionAvailable(width,height,step);}
    static boolean aggregateAvailable(int width,int rows,int begin,int end,int lo,int hi,int radius){
        return ENGINE.aggregateAvailable(width,rows,begin,end,lo,hi,radius);
    }
    static boolean fusionSums(byte[] data,int width,int height,int step,int[] sums,int[] counts){
        return ENGINE.fusion(data,width,height,step,sums,counts);
    }
    static boolean aggregate(int[] input,int[] meta,int[] out,int width,int rows,
            int begin,int end,int lo,int hi,int radius,int[] range,CpuAggregate cpu){
        return ENGINE.aggregate(input,meta,out,width,rows,begin,end,lo,hi,radius,range,cpu);
    }
    interface CpuAggregate {boolean run(int[] out);}
    interface Backend {
        boolean available();
        boolean fusion(byte[] data,int w,int h,int step,int[] sums,int[] counts);
        boolean aggregate(int[] input,int[] meta,int[] out,int w,int rows,int begin,
                int end,int lo,int hi,int radius,int[] range);
    }
    static class Clock {long now(){return System.nanoTime();}}
    private static final class NativeBackend implements Backend {
        private volatile int loaded;
        public boolean available(){
            if(loaded==0)synchronized(this){if(loaded==0){
                try {System.loadLibrary("ulike_gpu1949");loaded=nativeAbi()==1949?1:-1;}
                catch(LinkageError unavailable){loaded=-1;}
                catch(SecurityException unavailable){loaded=-1;}
            }}
            return loaded>0;
        }
        public boolean fusion(byte[] data,int w,int h,int step,int[] sums,int[] counts){
            return fusionSumsNative(data,w,h,step,sums,counts);
        }
        public boolean aggregate(int[] input,int[] meta,int[] out,int w,int rows,
                int begin,int end,int lo,int hi,int radius,int[] range){
            return aggregateNative(input,meta,out,w,rows,begin,end,lo,hi,radius,range);
        }
    }
    /** Package-private instance allows host tests to exercise the actual gate,
     * including failures and contention, without pretending to execute a GPU. */
    static final class Engine {
        private static final int LIMIT=24;
        private final Backend backend;private final Clock clock;
        private final State[] states=new State[LIMIT];private int victim;
        private volatile boolean failed;
        Engine(Backend backend,Clock clock){this.backend=backend;this.clock=clock;}
        boolean available(){
            if(failed)return false;
            try{return backend.available();}
            catch(LinkageError unavailable){failed=true;return false;}
            catch(SecurityException unavailable){failed=true;return false;}
        }
        boolean fusionAvailable(int w,int h,int step){
            if(!available())return false;
            synchronized(this){
                for(State s:states)if(s!=null&&s.key.length==4&&s.key[0]==1&&
                        s.key[1]==w&&s.key[2]==h&&s.key[3]==step)return !s.disabled&&!failed;
                return !failed;
            }
        }
        boolean aggregateAvailable(int w,int rows,int begin,int end,int lo,int hi,int radius){
            if(!available())return false;
            synchronized(this){
                for(State s:states)if(s!=null&&s.key.length==7&&s.key[0]==2&&s.key[1]==w&&
                        s.key[2]==rows&&s.key[3]==begin-lo&&s.key[4]==end-begin&&
                        s.key[5]==hi-end&&s.key[6]==radius)return !s.disabled&&!failed;
                return !failed;
            }
        }
        private State state(int... key){
            for(State s:states)if(s!=null&&Arrays.equals(s.key,key))return s;
            State s=new State(key);
            for(int i=0;i<LIMIT;i++)if(states[i]==null){states[i]=s;return s;}
            states[victim]=s;victim=(victim+1)%LIMIT;return s;
        }
        boolean fusion(byte[] data,int w,int h,int step,int[] sums,int[] counts){
            if(!available()||data==null||sums==null||counts==null||sums==counts||w<1||h<1||step<1||
                    (long)w*h>data.length||(long)step*step>Integer.MAX_VALUE/255)return false;
            long sw=((long)w+step-1)/step,sh=((long)h+step-1)/step,n=sw*sh;
            if(n>Integer.MAX_VALUE||sums.length<n||counts.length<n)return false;
            // Includes waiting for the retained GPU context, not just dispatch.
            long entered=clock.now();
            synchronized(this){
                State s=state(1,w,h,step);
                if(s.disabled||failed)return false;
                try {
                    if(s.fast()){
                        if(backend.fusion(data,w,h,step,sums,counts))return true;
                        s.disabled=true;return false;
                    }
                    int size=(int)n;
                    int[] gpuSums=new int[size],gpuCounts=new int[size];
                    boolean ok=backend.fusion(data,w,h,step,gpuSums,gpuCounts);
                    long gpuNanos=clock.now()-entered;
                    long started=clock.now();
                    cpuSums(data,w,h,step,sums,counts);
                    long cpuNanos=clock.now()-started;
                    boolean equal=ok&&prefixEquals(sums,gpuSums,size)&&prefixEquals(counts,gpuCounts,size);
                    s.probe(equal,gpuNanos,cpuNanos);
                    // Probation publishes the original CPU result even on a
                    // mismatching or slower GPU; no candidate pixels escape.
                    return true;
                } catch(OutOfMemoryError unavailable){s.disabled=true;return false;}
                  catch(LinkageError unavailable){failed=true;s.disabled=true;return false;}
                  catch(SecurityException unavailable){failed=true;s.disabled=true;return false;}
            }
        }
        boolean aggregate(int[] input,int[] meta,int[] out,int w,int rows,int begin,
                int end,int lo,int hi,int radius,int[] range,CpuAggregate cpu){
            if(!available()||cpu==null||input==null||meta==null||out==null||range==null||
                    input==meta||input==out||meta==out||input==range||meta==range||out==range||
                    w<1||rows<1||radius<1||radius>4||lo<0||hi>rows||begin<lo||end<begin||end>hi||
                    (long)w*rows>input.length||(long)w*(end-begin)>meta.length||
                    (long)w*(end-begin)*3>out.length||range.length<33*256)return false;
            if(begin==end)return true;
            long entered=clock.now();
            synchronized(this){
                State s=state(2,w,rows,begin-lo,end-begin,hi-end,radius);
                if(s.disabled||failed)return false;
                try {
                    if(s.fast()){
                        if(backend.aggregate(input,meta,out,w,rows,begin,end,lo,hi,radius,range))return true;
                        s.disabled=true;return false;
                    }
                    // Preserve inactive fine/coarse entries in both candidates.
                    int[] candidate=out.clone();
                    boolean ok=backend.aggregate(input,meta,candidate,w,rows,begin,end,lo,hi,radius,range);
                    long gpuNanos=clock.now()-entered;
                    long started=clock.now();
                    boolean reference=cpu.run(out);
                    long cpuNanos=clock.now()-started;
                    boolean equal=ok&&reference&&Arrays.equals(out,candidate);
                    s.probe(equal,gpuNanos,cpuNanos);
                    // Caller falls through to Java when old native is absent.
                    return reference;
                } catch(OutOfMemoryError unavailable){s.disabled=true;return false;}
                  catch(LinkageError unavailable){failed=true;s.disabled=true;return false;}
                  catch(SecurityException unavailable){failed=true;s.disabled=true;return false;}
            }
        }
    }
    private static final class State {
        final int[] key;int probes,wins,calls;boolean admitted,disabled;
        State(int[] key){this.key=key;}
        // Reassess actual data and transfer-inclusive latency occasionally so
        // thermal/load/content changes can return this shape to the CPU.
        boolean fast(){return admitted&&++calls%64!=0;}
        void probe(boolean equal,long gpu,long cpu){
            if(!equal){disabled=true;return;}
            // First call includes program creation and context warmup; it still
            // must match exactly, but cannot claim a steady-state speed win.
            if(probes++==0)return;
            if(gpu>=0&&cpu>0&&gpu<cpu)wins++;
            else {disabled=true;return;}
            if(wins>=2)admitted=true;
        }
    }
    static void cpuSums(byte[] data,int width,int height,int step,int[] sums,int[] counts){
        int w=(int)(((long)width+step-1)/step),h=(int)(((long)height+step-1)/step);
        for(int y=0;y<h;y++){
            if(Thread.currentThread().isInterrupted())throw new IllegalStateException("fusion interrupted");
            for(int x=0;x<w;x++){
                int right=(int)Math.min((long)width,(long)(x+1)*step);
                int bottom=(int)Math.min((long)height,(long)(y+1)*step);
                int sum=0,n=0;
                for(int yy=y*step;yy<bottom;yy++){
                    int at=yy*width+x*step;
                    for(int xx=x*step;xx<right;xx++){sum+=data[at++]&255;n++;}
                }
                sums[y*w+x]=sum;counts[y*w+x]=n;
            }
        }
    }
    private static boolean prefixEquals(int[] a,int[] b,int n){
        for(int i=0;i<n;i++)if(a[i]!=b[i])return false;return true;
    }
    private static native int nativeAbi();
    private static native boolean fusionSumsNative(byte[] data,int width,int height,int step,int[] sums,int[] counts);
    private static native boolean aggregateNative(int[] input,int[] meta,int[] out,int width,
            int rows,int begin,int end,int lo,int hi,int radius,int[] range);
}
