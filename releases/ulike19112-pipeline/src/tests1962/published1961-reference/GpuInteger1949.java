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
    static boolean finish(int[] input,int[] meta,int[] policy,int[] out,int width,int rows,
            int begin,int end,int lo,int hi,int radius,int[] range,int noise,boolean shadows,
            int global,int beauty,int smoothLimit,CpuFinish cpu){
        return ENGINE.finish(input,meta,policy,out,width,rows,begin,end,lo,hi,radius,range,
                noise,shadows,global,beauty,smoothLimit,cpu);
    }
    static boolean finishInto(int[] input,int[] meta,int[] policy,int[] out,int outputOffset,
            int width,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
            boolean shadows,int global,int beauty,int smoothLimit,CpuFinishInto cpu){
        return ENGINE.finishInto(input,meta,policy,out,outputOffset,width,rows,begin,end,lo,hi,
                radius,range,noise,shadows,global,beauty,smoothLimit,cpu);
    }
    /** The save pipeline's output is an exact copy of input before residual NR.
     * A 27-bit lossless policy word holds three bounded Q8 values. */
    static boolean finishSavedInto(int[] input,int[] meta,int[] packedPolicy,int[] out,int outputOffset,
            int width,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
            boolean shadows,int global,int beauty,int smoothLimit,CpuFinishInto cpu){
        return ENGINE.finishSavedInto(input,meta,packedPolicy,out,outputOffset,width,rows,begin,end,
                lo,hi,radius,range,noise,shadows,global,beauty,smoothLimit,cpu);
    }
    interface CpuAggregate {boolean run(int[] out);}
    interface CpuFinish {boolean run(int[] out);}
    interface CpuFinishInto {boolean run(int[] out,int outputOffset);}
    interface Backend {
        boolean available();
        boolean fusion(byte[] data,int w,int h,int step,int[] sums,int[] counts);
        boolean aggregate(int[] input,int[] meta,int[] out,int w,int rows,int begin,
                int end,int lo,int hi,int radius,int[] range);
        default boolean finish(int[] input,int[] meta,int[] policy,int[] out,int w,int rows,
                int begin,int end,int lo,int hi,int radius,int[] range,int noise,
                boolean shadows,int global,int beauty,int smoothLimit){return false;}
        default boolean finishInto(int[] input,int[] meta,int[] policy,int[] out,int outputOffset,
                int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
                boolean shadows,int global,int beauty,int smoothLimit){return false;}
        default boolean finishSavedInto(int[] input,int[] meta,int[] packedPolicy,int[] out,int outputOffset,
                int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
                boolean shadows,int global,int beauty,int smoothLimit){return false;}
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
        public boolean finish(int[] input,int[] meta,int[] policy,int[] out,int w,int rows,
                int begin,int end,int lo,int hi,int radius,int[] range,int noise,
                boolean shadows,int global,int beauty,int smoothLimit){
            return finishNative(input,meta,policy,out,w,rows,begin,end,lo,hi,radius,range,
                    noise,shadows,global,beauty,smoothLimit);
        }
        public boolean finishInto(int[] input,int[] meta,int[] policy,int[] out,int outputOffset,
                int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
                boolean shadows,int global,int beauty,int smoothLimit){
            return finishIntoNative(input,meta,policy,out,outputOffset,w,rows,begin,end,lo,hi,
                    radius,range,noise,shadows,global,beauty,smoothLimit);
        }
        public boolean finishSavedInto(int[] input,int[] meta,int[] packedPolicy,int[] out,int outputOffset,
                int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
                boolean shadows,int global,int beauty,int smoothLimit){
            return finishSavedIntoNative(input,meta,packedPolicy,out,outputOffset,w,rows,begin,end,
                    lo,hi,radius,range,noise,shadows,global,beauty,smoothLimit);
        }
    }
    /** Package-private instance allows host tests to exercise the actual gate,
     * including failures and contention, without pretending to execute a GPU. */
    static final class Engine {
        private static final int LIMIT=24;
        private final Backend backend;private final Clock clock;
        // GLES owns one retained context. Serialize dispatch only, never the
        // independent CPU reference or unrelated admission-state lookups.
        private final Object dispatch=new Object();
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
            State s=find(1,w,h,step);return (s==null||!s.disabled)&&!failed;
        }
        boolean aggregateAvailable(int w,int rows,int begin,int end,int lo,int hi,int radius){
            if(!available())return false;
            State s=find(shape(2,w,begin,end,lo,hi,radius));
            return (s==null||!s.disabled)&&!failed;
        }
        private synchronized State find(int... key){
            for(State s:states)if(s!=null&&Arrays.equals(s.key,key))return s;
            return null;
        }
        private synchronized State state(int... key){
            for(State s:states)if(s!=null&&Arrays.equals(s.key,key))return s;
            State s=new State(key);
            for(int i=0;i<LIMIT;i++)if(states[i]==null){states[i]=s;return s;}
            states[victim]=s;victim=(victim+1)%LIMIT;return s;
        }
        private static int[] shape(int kind,int w,int begin,int end,int lo,int hi,int radius){
            // The native backend uploads a compact halo, not the global image.
            // Absolute tile position must not restart probation or evict the
            // interior shape once a capture contains more than 24 tiles.
            int top=Math.min(radius,begin-lo),bottom=Math.min(radius,hi-end),tile=end-begin;
            return new int[]{kind,w,top+tile+bottom,top,tile,bottom,radius};
        }
        private boolean dispatchFusion(byte[] data,int w,int h,int step,int[] sums,int[] counts){
            synchronized(dispatch){return backend.fusion(data,w,h,step,sums,counts);}
        }
        private boolean dispatchAggregate(int[] input,int[] meta,int[] out,int w,int rows,
                int begin,int end,int lo,int hi,int radius,int[] range){
            synchronized(dispatch){return backend.aggregate(input,meta,out,w,rows,begin,end,lo,hi,radius,range);}
        }
        private boolean dispatchFinish(int[] input,int[] meta,int[] policy,int[] out,int w,int rows,
                int begin,int end,int lo,int hi,int radius,int[] range,int noise,
                boolean shadows,int global,int beauty,int smoothLimit){
            synchronized(dispatch){return backend.finish(input,meta,policy,out,w,rows,begin,end,lo,hi,
                    radius,range,noise,shadows,global,beauty,smoothLimit);}
        }
        private boolean dispatchFinishInto(int[] input,int[] meta,int[] policy,int[] out,int outputOffset,
                int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
                boolean shadows,int global,int beauty,int smoothLimit){
            synchronized(dispatch){return backend.finishInto(input,meta,policy,out,outputOffset,w,rows,
                    begin,end,lo,hi,radius,range,noise,shadows,global,beauty,smoothLimit);}
        }
        private boolean dispatchFinishSavedInto(int[] input,int[] meta,int[] policy,int[] out,int outputOffset,
                int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
                boolean shadows,int global,int beauty,int smoothLimit){
            synchronized(dispatch){return backend.finishSavedInto(input,meta,policy,out,outputOffset,w,rows,
                    begin,end,lo,hi,radius,range,noise,shadows,global,beauty,smoothLimit);}
        }
        boolean fusion(byte[] data,int w,int h,int step,int[] sums,int[] counts){
            if(!available()||data==null||sums==null||counts==null||sums==counts||w<1||h<1||step<1||
                    (long)w*h>data.length||(long)step*step>Integer.MAX_VALUE/255)return false;
            long sw=((long)w+step-1)/step,sh=((long)h+step-1)/step,n=sw*sh;
            if(n>Integer.MAX_VALUE||sums.length<n||counts.length<n)return false;
            // Includes waiting for the retained GPU context, not just dispatch.
            State s=state(1,w,h,step);
            if(failed)return false;
            int mode=s.claim();if(mode==0)return false;
            try {
                if(mode==2){
                    if(dispatchFusion(data,w,h,step,sums,counts))return true;
                    s.disabled=true;return false;
                }
                int size=(int)n;
                long entered=clock.now();
                int[] gpuSums=new int[size],gpuCounts=new int[size];
                boolean ok=dispatchFusion(data,w,h,step,gpuSums,gpuCounts);
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
              finally{s.release();}
        }
        boolean aggregate(int[] input,int[] meta,int[] out,int w,int rows,int begin,
                int end,int lo,int hi,int radius,int[] range,CpuAggregate cpu){
            if(!available()||cpu==null||input==null||meta==null||out==null||range==null||
                    input==meta||input==out||meta==out||input==range||meta==range||out==range||
                    w<1||rows<1||radius<1||radius>4||lo<0||hi>rows||begin<lo||end<begin||end>hi||
                    (long)w*rows>input.length||(long)w*(end-begin)>meta.length||
                    (long)w*(end-begin)*3>out.length||range.length<33*256)return false;
            if(begin==end)return true;
            State s=state(shape(2,w,begin,end,lo,hi,radius));
            if(failed)return false;
            int mode=s.claim();if(mode==0)return false;
            try {
                if(mode==2){
                    if(dispatchAggregate(input,meta,out,w,rows,begin,end,lo,hi,radius,range))return true;
                    s.disabled=true;return false;
                }
                // Preserve inactive fine/coarse entries in both candidates.
                long entered=clock.now();
                int[] candidate=out.clone();
                boolean ok=dispatchAggregate(input,meta,candidate,w,rows,begin,end,lo,hi,radius,range);
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
              finally{s.release();}
        }
        boolean finish(int[] input,int[] meta,int[] policy,int[] out,int w,int rows,int begin,
                int end,int lo,int hi,int radius,int[] range,int noise,boolean shadows,
                int global,int beauty,int smoothLimit,CpuFinish cpu){
            if(!available()||cpu==null||input==null||meta==null||policy==null||out==null||range==null||
                    input==meta||input==policy||input==out||input==range||meta==policy||meta==out||
                    meta==range||policy==out||policy==range||out==range||w<1||rows<1||radius<1||
                    radius>4||lo<0||hi>rows||begin<lo||end<begin||end>hi||
                    (long)w*rows>input.length||(long)w*(end-begin)>meta.length||
                    (long)w*(end-begin)*3>policy.length||(long)w*(end-begin)>out.length||
                    range.length<33*256)return false;
            if(begin==end)return true;
            int[] geometry=shape(3,w,begin,end,lo,hi,radius),key=Arrays.copyOf(geometry,12);
            key[7]=noise;key[8]=shadows?1:0;key[9]=global;key[10]=beauty;key[11]=smoothLimit;
            State s=state(key);
            if(failed)return false;
            int mode=s.claim();if(mode==0)return false;
            try {
                if(mode==2){
                    if(dispatchFinish(input,meta,policy,out,w,rows,begin,end,lo,hi,radius,range,
                            noise,shadows,global,beauty,smoothLimit))return true;
                    s.disabled=true;return false;
                }
                long entered=clock.now();int[] candidate=out.clone();
                boolean ok=dispatchFinish(input,meta,policy,candidate,w,rows,begin,end,lo,hi,radius,
                        range,noise,shadows,global,beauty,smoothLimit);
                long gpuNanos=clock.now()-entered;
                long started=clock.now();boolean reference=cpu.run(out);
                long cpuNanos=clock.now()-started;
                s.probe(ok&&reference&&Arrays.equals(out,candidate),gpuNanos,cpuNanos);
                return reference;
            } catch(OutOfMemoryError unavailable){s.disabled=true;return false;}
              catch(LinkageError unavailable){failed=true;s.disabled=true;return false;}
              catch(SecurityException unavailable){failed=true;s.disabled=true;return false;}
              finally{s.release();}
        }
        boolean finishInto(int[] input,int[] meta,int[] policy,int[] out,int outputOffset,int w,
                int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
                boolean shadows,int global,int beauty,int smoothLimit,CpuFinishInto cpu){
            return finishIntoMode(input,meta,policy,out,outputOffset,w,rows,begin,end,lo,hi,radius,
                    range,noise,shadows,global,beauty,smoothLimit,cpu,false);
        }
        boolean finishSavedInto(int[] input,int[] meta,int[] policy,int[] out,int outputOffset,int w,
                int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
                boolean shadows,int global,int beauty,int smoothLimit,CpuFinishInto cpu){
            return finishIntoMode(input,meta,policy,out,outputOffset,w,rows,begin,end,lo,hi,radius,
                    range,noise,shadows,global,beauty,smoothLimit,cpu,true);
        }
        private boolean finishIntoMode(int[] input,int[] meta,int[] policy,int[] out,int outputOffset,int w,
                int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
                boolean shadows,int global,int beauty,int smoothLimit,CpuFinishInto cpu,boolean saved){
            long length=(long)w*((long)end-begin);
            if(!available()||cpu==null||input==null||meta==null||policy==null||out==null||range==null||
                    input==meta||input==policy||input==out||input==range||meta==policy||meta==out||
                    meta==range||policy==out||policy==range||out==range||w<1||rows<1||radius<1||
                    radius>4||lo<0||hi>rows||begin<lo||end<begin||end>hi||outputOffset<0||
                    (long)w*rows>input.length||length>meta.length||length*(saved?1:3)>policy.length||
                    (long)outputOffset+length>out.length||range.length<33*256)return false;
            if(begin==end)return true;
            // Separate route from the historical tile-output endpoint: its
            // admission must include GPU-only output seeding and publication.
            int[] geometry=shape(saved?5:4,w,begin,end,lo,hi,radius),key=Arrays.copyOf(geometry,12);
            key[7]=noise;key[8]=shadows?1:0;key[9]=global;key[10]=beauty;key[11]=smoothLimit;
            State s=state(key);
            if(failed)return false;
            int mode=s.claim();if(mode==0)return false;
            try {
                if(mode==2){
                    // Production JNI writes this tile directly into the final
                    // workspace. There are no Java seed/copy-back operations
                    // outside the measured GPU route.
                    boolean ok=saved?dispatchFinishSavedInto(input,meta,policy,out,outputOffset,w,rows,
                            begin,end,lo,hi,radius,range,noise,shadows,global,beauty,smoothLimit)
                            :dispatchFinishInto(input,meta,policy,out,outputOffset,w,rows,begin,end,lo,hi,
                            radius,range,noise,shadows,global,beauty,smoothLimit);
                    if(ok)return true;
                    s.disabled=true;return false;
                }
                // Use the exact production endpoint and destination size,
                // including any whole-array pin/copy cost at the JNI boundary.
                // Cloning the full output is extra probation-only work and
                // makes the speed comparison conservative.
                long entered=clock.now();int[] candidate=out.clone();
                boolean ok=saved?dispatchFinishSavedInto(input,meta,policy,candidate,outputOffset,w,rows,
                        begin,end,lo,hi,radius,range,noise,shadows,global,beauty,smoothLimit)
                        :dispatchFinishInto(input,meta,policy,candidate,outputOffset,w,rows,begin,end,
                        lo,hi,radius,range,noise,shadows,global,beauty,smoothLimit);
                long gpuNanos=clock.now()-entered;
                long started=clock.now();boolean reference=cpu.run(out,outputOffset);
                long cpuNanos=clock.now()-started;
                s.probe(ok&&reference&&Arrays.equals(out,candidate),gpuNanos,cpuNanos);
                return reference;
            } catch(OutOfMemoryError unavailable){s.disabled=true;return false;}
              catch(LinkageError unavailable){failed=true;s.disabled=true;return false;}
              catch(SecurityException unavailable){failed=true;s.disabled=true;return false;}
              finally{s.release();}
        }
    }
    private static final class State {
        final int[] key;int probes,wins,calls;boolean admitted,inFlight;volatile boolean disabled;
        State(int[] key){this.key=key;}
        // Reassess actual data and transfer-inclusive latency occasionally so
        // thermal/load/content changes can return this shape to the CPU.
        synchronized int claim(){
            // Several workers commonly share the compact interior geometry.
            // A busy candidate immediately leaves the caller on its original
            // CPU path; never queue its work behind another CPU comparison.
            if(disabled||inFlight)return 0;
            inFlight=true;return admitted&&++calls%64!=0?2:1;
        }
        synchronized void release(){inFlight=false;}
        synchronized void probe(boolean equal,long gpu,long cpu){
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
    private static native boolean finishNative(int[] input,int[] meta,int[] policy,int[] out,int width,
            int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
            boolean shadows,int global,int beauty,int smoothLimit);
    private static native boolean finishIntoNative(int[] input,int[] meta,int[] policy,int[] out,
            int outputOffset,int width,int rows,int begin,int end,int lo,int hi,int radius,int[] range,
            int noise,boolean shadows,int global,int beauty,int smoothLimit);
    private static native boolean finishSavedIntoNative(int[] input,int[] meta,int[] packedPolicy,int[] out,
            int outputOffset,int width,int rows,int begin,int end,int lo,int hi,int radius,int[] range,
            int noise,boolean shadows,int global,int beauty,int smoothLimit);
}
