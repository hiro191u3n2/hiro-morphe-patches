package com.hiro.ulike;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/** Exact CPU colour reuse is optional: original arithmetic stays selected until
 * this device proves two complete outputs and a five-percent full-call win.
 * Detached comparisons use the existing bounded, after-save idle queue. */
public final class ColourCache1976 {
    private ColourCache1976() {}
    private static final Object SNAPSHOTS=new Object();
    private static final int CPU=0,PREPARE=1,DIRECT=2;
    private static int[] geometry(int width,int rows,int begin,int end,int vb,int ve,
            int origin,int noise,boolean shadows,int mw,int mh,int columns,int mr,int pw,int ph) {
        return new int[]{width,rows,begin,end,vb,ve,origin,noise,shadows?1:0,mw,mh,columns,mr,pw,ph};
    }
    private static String key(int kind,int[] u,boolean policy) {
        return "cpu-colour1976-exact2-time5-v1:"+kind+":"+(policy?1:0)+":"+Arrays.toString(u);
    }
    private static boolean enabled(String key) {
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        return proof!=null&&proof.variant==1&&proof.cpuNanos>0&&proof.gpuNanos>0&&
            proof.gpuNanos<=proof.cpuNanos-proof.cpuNanos/20;
    }
    /** Read the existing CPU colour route without changing its certificate. */
    static boolean cpuEnabled1981(int[] geometry,boolean policy) {
        return enabled(key(CPU,geometry,policy));
    }
    static boolean cpu(int[] input,int[] output,int width,int rows,int begin,int end,
            int vb,int ve,int origin,int noise,boolean shadows,int mw,int mh,int columns,
            int mr,int pw,int ph,float[] model,int[] policy) {
        int[] u=geometry(width,rows,begin,end,vb,ve,origin,noise,shadows,mw,mh,columns,mr,pw,ph);
        String key=key(CPU,u,policy!=null);boolean okay=cpuNative(input,output,width,rows,begin,end,
            vb,ve,origin,noise,shadows,mw,mh,columns,mr,pw,ph,model,policy,enabled(key));
        if(okay)schedule(key,CPU,input,u,model,policy,null);return okay;
    }
    static float[] prepare(int[] input,int width,int rows,int begin,int end,int vb,int ve,
            int origin,int noise,boolean shadows,int mh,int columns,int mr,int pw,int ph,float[] model) {
        int[] u=geometry(width,rows,begin,end,vb,ve,origin,noise,shadows,width,mh,columns,mr,pw,ph);
        String key=key(PREPARE,u,false);float[] result=prepareNative(input,width,rows,begin,end,
            vb,ve,origin,noise,shadows,mh,columns,mr,pw,ph,model,enabled(key));
        if(result!=null)schedule(key,PREPARE,input,u,model,null,null);return result;
    }
    static boolean direct(ByteBuffer cache,ByteBuffer output,int[] input,int width,int rows,
            int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,int mh,
            int columns,int mr,int pw,int ph,float[] model) {
        int[] u=geometry(width,rows,begin,end,vb,ve,origin,noise,shadows,width,mh,columns,mr,pw,ph);
        String key=key(DIRECT,u,false);boolean cached=enabled(key);
        if(!cached&&!GpuQualification1961.background()&&!Thread.currentThread().isInterrupted()) {
            synchronized(SNAPSHOTS) {
                // Snapshot only after admission, and before JNI updates the
                // transactional tail. Keep admission/copy/submission joined so
                // another local snapshot cannot consume this reserved window.
                Detached pending=null;
                try{pending=detach(key,DIRECT,input,u,model,null,cache);}
                catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
                if(pending!=null) {
                    try {
                        boolean okay=directNative(cache,output,input,width,rows,begin,end,vb,ve,
                            origin,noise,shadows,mh,columns,mr,pw,ph,model,false);
                        if(okay&&!Thread.currentThread().isInterrupted()){pending.submit();pending=null;}
                        return okay;
                    } finally {if(pending!=null)pending.probe.close();}
                }
            }
        }
        return directNative(cache,output,input,width,rows,begin,end,vb,ve,origin,noise,shadows,
            mh,columns,mr,pw,ph,model,cached);
    }
    private static final class Detached {
        final String key;final long bytes;final Proof probe;
        Detached(String key,long bytes,Proof probe){this.key=key;this.bytes=bytes;this.probe=probe;}
        void submit() {
            try{GpuQualification1961.schedule(key,bytes,probe);}
            catch(RuntimeException unavailable){probe.close();}
            catch(LinkageError unavailable){probe.close();}
            catch(OutOfMemoryError unavailable){probe.close();}
        }
    }
    /** Called under SNAPSHOTS; no caller-owned storage is retained. */
    private static Detached detach(String key,int kind,int[] input,int[] u,float[] model,
            int[] policy,ByteBuffer cache) {
        long count=(long)u[0]*u[1],core=(long)u[0]*(u[3]-u[2]);
        if(count<1||count>Integer.MAX_VALUE||core<1||core>Integer.MAX_VALUE/2||
                u[3]-u[2]>256||input.length<count||policy!=null&&policy.length<core*2)return null;
        long cacheBytes=cache==null?0:cache.capacity();
        long bytes=4L*(count+model.length+(policy==null?0:core*2))+cacheBytes+4096L;
        if(bytes>96L*1024*1024||!GpuQualification1961.canQueue(key,bytes)||
                !GpuNoise1960.workspaceFits(bytes+peak(kind,u,model.length,cacheBytes)))return null;
        ByteBuffer detached=cache==null?null:copy(cache);
        Proof probe=new Proof(key,kind,Arrays.copyOf(input,(int)count),u.clone(),model.clone(),
            policy==null?null:Arrays.copyOf(policy,(int)core*2),detached);
        return new Detached(key,bytes,probe);
    }
    private static void schedule(String key,int kind,int[] input,int[] u,float[] model,
            int[] policy,ByteBuffer cache) {
        if(GpuQualification1961.background()||Thread.currentThread().isInterrupted())return;
        try {synchronized(SNAPSHOTS) {
            Detached pending=detach(key,kind,input,u,model,policy,cache);
            if(pending!=null)pending.submit();
        }}catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
    }
    private static long recordBytes(int[] u) {
        long start=(long)u[6]+u[2],finish=(long)u[6]+u[3];
        long first=Math.floorDiv(start-7,4)*4;
        return ((u[0]+3L)/4+1)*((finish-first+3)/4)*248L*4L;
    }
    private static long peak(int kind,int[] u,int modelFloats,long cacheBytes) {
        long count=(long)u[0]*u[1],core=(long)u[0]*(u[3]-u[2]);
        // Array preparation can briefly own the Java result and its JNI copy
        // while the other full trial remains live. CPU policy JNI copies are
        // also reserved, independently of the detached policy snapshot.
        long outputs=kind==CPU?8L*count:(kind==PREPARE?3L:2L)*recordBytes(u);
        return outputs+2L*cacheBytes+4L*count+4L*modelFloats+12L*core+1024L*u[0]+1048576L;
    }
    private static ByteBuffer copy(ByteBuffer original) {
        ByteBuffer target=ByteBuffer.allocateDirect(original.capacity()).order(ByteOrder.nativeOrder());
        restore(target,original);return target;
    }
    private static void restore(ByteBuffer target,ByteBuffer original) {
        ByteBuffer in=original.duplicate();in.clear();target.clear();target.put(in);target.clear();
    }
    private static final class Proof implements GpuQualification1961.Probe {
        final String key;final int kind;int[] input,u,policy;float[] model;ByteBuffer cache;
        Proof(String key,int kind,int[] input,int[] u,float[] model,int[] policy,ByteBuffer cache){
            this.key=key;this.kind=kind;this.input=input;this.u=u;this.model=model;this.policy=policy;this.cache=cache;
        }
        private Object invoke(boolean cached,int[] pixels,ByteBuffer records,ByteBuffer state) {
            if(kind==CPU)return cpuNative(input,pixels,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[7],
                u[8]!=0,u[9],u[10],u[11],u[12],u[13],u[14],model,policy,cached)?pixels:null;
            if(kind==PREPARE)return prepareNative(input,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[7],
                u[8]!=0,u[10],u[11],u[12],u[13],u[14],model,cached);
            return directNative(state,records,input,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[7],u[8]!=0,
                u[10],u[11],u[12],u[13],u[14],model,cached)?records:null;
        }
        private boolean equal(Object a,Object b) {
            if(a==null||b==null)return false;
            if(kind==CPU)return Arrays.equals((int[])a,(int[])b);
            if(kind==PREPARE){float[] x=(float[])a,y=(float[])b;if(x.length!=y.length)return false;
                for(int i=0;i<x.length;i++)if(Float.floatToRawIntBits(x[i])!=Float.floatToRawIntBits(y[i]))return false;return true;}
            ByteBuffer x=(ByteBuffer)a,y=(ByteBuffer)b;if(x.capacity()!=y.capacity())return false;
            for(int i=0;i<x.capacity();i+=4)if(x.getInt(i)!=y.getInt(i))return false;return true;
        }
        public void run(GpuQualification1961.Cancellation cancellation) {
            if(cancellation.cancelled()||input==null||!GpuNoise1960.workspaceFits(peak(kind,u,model.length,cache==null?0:cache.capacity())))return;
            long bytes=recordBytes(u);if(bytes<1||bytes>Integer.MAX_VALUE||bytes>GpuNoise1960.MAX_BYTES)return;
            int[][] pixels=kind==CPU?new int[][]{new int[input.length],new int[input.length]}:null;
            ByteBuffer[] records=kind==DIRECT?new ByteBuffer[]{ByteBuffer.allocateDirect((int)bytes).order(ByteOrder.nativeOrder()),ByteBuffer.allocateDirect((int)bytes).order(ByteOrder.nativeOrder())}:null;
            ByteBuffer[] states=kind==DIRECT?new ByteBuffer[]{copy(cache),copy(cache)}:null;
            long fastestOld=Long.MAX_VALUE,slowestNew=0;
            // One warmup pair, then two full comparisons with reversed order.
            for(int trial=-1;trial<2;trial++) {
                Object[] output=new Object[2];long[] time=new long[2];
                for(int turn=0;turn<2;turn++) {
                    if(cancellation.cancelled())return;int choice=(trial&1)==0?turn:1-turn;
                    if(states!=null)restore(states[choice],cache);
                    long start=System.nanoTime();output[choice]=invoke(choice==1,pixels==null?null:pixels[choice],records==null?null:records[choice],states==null?null:states[choice]);
                    time[choice]=Math.max(1L,System.nanoTime()-start);if(cancellation.cancelled())return;
                    if(output[choice]==null)return;
                }
                if(!equal(output[0],output[1])){GpuQualification1961.rejectExact(key);return;}
                if(trial>=0){fastestOld=Math.min(fastestOld,time[0]);slowestNew=Math.max(slowestNew,time[1]);}
            }
            if(cancellation.cancelled())return;
            if(slowestNew<=fastestOld-fastestOld/20)GpuQualification1961.qualified(key,fastestOld,slowestNew,1);
            else GpuQualification1961.rejectSpeed(key);
        }
        public void close(){input=null;u=null;policy=null;model=null;cache=null;}
    }
    private static native boolean cpuNative(int[] input,int[] output,int width,int rows,int begin,int end,
        int vb,int ve,int origin,int noise,boolean shadows,int mw,int mh,int columns,int mr,int pw,int ph,float[] model,int[] policy,boolean cached);
    private static native float[] prepareNative(int[] input,int width,int rows,int begin,int end,int vb,int ve,
        int origin,int noise,boolean shadows,int mh,int columns,int mr,int pw,int ph,float[] model,boolean cached);
    private static native boolean directNative(ByteBuffer cache,ByteBuffer records,int[] input,int width,int rows,
        int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,int mh,int columns,int mr,int pw,int ph,float[] model,boolean cached);
}
