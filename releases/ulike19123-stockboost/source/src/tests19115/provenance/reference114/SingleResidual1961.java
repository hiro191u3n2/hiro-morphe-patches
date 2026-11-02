package com.hiro.ulike;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CancellationException;

/** Exact native1955 binary64 preparation for the FP32 GPU residual finish.
 * Records contain only the original float casts; all transforms, thresholds,
 * residual safeguards and source samples remain the current CPU arithmetic. */
final class SingleResidual1961 {
    private SingleResidual1961() {}
    private static final Object MEMORY1981=new Object();
    private static long retained1981;
    static long retainedBytes1981(){synchronized(MEMORY1981){return retained1981;}}
    /** Capacity only, owned by one joined photograph stage. Native cache state
     * is invalidated for every new source strip before any JNI call. */
    static final class Buffers1981 implements AutoCloseable {
        private ByteBuffer cache;
        private final ByteBuffer[] banks=new ByteBuffer[2];
        private boolean leased,closed;
        private long bytes;
        private ByteBuffer grow(ByteBuffer old,long requested) {
            if(requested<1||requested>Integer.MAX_VALUE||requested>GpuNoise1960.MAX_BYTES)
                throw new IllegalArgumentException("single residual retained capacity");
            if(old!=null&&old.capacity()>=requested)return old;
            // Reserve the complete replacement while the previous capacity is
            // still live, including concurrent stages and CPU native scratch.
            if(!GpuNoise1960.workspaceFits(requested))throw new OutOfMemoryError("single residual retained budget");
            synchronized(MEMORY1981) {
                if(requested>96L*1024*1024-retained1981)throw new OutOfMemoryError("single residual shared retained limit");
                retained1981+=requested;
            }
            ByteBuffer made=null;
            try {made=ByteBuffer.allocateDirect((int)requested).order(ByteOrder.nativeOrder());return made;}
            finally {synchronized(MEMORY1981) {
                if(made==null)retained1981-=requested;
                else {long previous=old==null?0:old.capacity();retained1981-=previous;bytes+=requested-previous;}
            }}
        }
        synchronized ByteBuffer acquire(long requested) {
            if(closed||leased)throw new IllegalStateException("single residual buffers leased");
            cache=grow(cache,requested);
            // ResidualCacheHeader1964 is exactly 80 bytes. Its invalid magic and
            // valid=0 force the native transaction to rewrite model/tail state.
            for(int i=0;i<80;i+=4)cache.putInt(i,0);
            leased=true;return cache;
        }
        synchronized ByteBuffer bank(int bank,long requested) {
            if(closed||!leased||bank<0||bank>1)throw new IllegalStateException("single residual bank lease");
            return banks[bank]=grow(banks[bank],requested);
        }
        synchronized void release(){if(!leased)throw new IllegalStateException("single residual buffers not leased");leased=false;}
        public synchronized void close() {
            if(closed)return;if(leased)throw new IllegalStateException("single residual close before overlap join");
            closed=true;cache=null;banks[0]=banks[1]=null;
            synchronized(MEMORY1981){retained1981-=bytes;bytes=0;if(retained1981<0)throw new IllegalStateException("single residual retained accounting");}
        }
    }
    static float[] prepare(int[] input,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,
            SingleNoise1955.Model model) {
        return ColourCache1976.prepare(input,width,rows,begin,end,validBegin,validEnd,originY,
            noise,shadows,model.height,model.columns,model.rows,
            model.gpuPatchWidth1960(),model.gpuPatchHeight1960(),model.gpuData1960());
    }
    private static native float[] prepareNative(int[] input,int width,int rows,int begin,int end,
        int validBegin,int validEnd,int originY,int noise,boolean shadows,int modelHeight,
        int columns,int modelRows,int patchWidth,int patchHeight,float[] modelData);
    /** GX34/GX35: one photograph's bounded coefficient lease. Its two record
     * banks are native-order Java-owned buffers. Submit snapshots each bank
     * before it can be reused; close is reached only after the overlap worker
     * has joined. No image or native pointer survives this lease. */
    static final class Preparation implements AutoCloseable {
        private int[] input;
        private SingleNoise1955.Model model;
        private ByteBuffer cache;
        private final ByteBuffer[] banks=new ByteBuffer[2];
        private final int width,rows,validBegin,validEnd,originY,noise;
        private final boolean shadows;
        private Buffers1981 reusable;
        private int nextBank;
        Preparation(int[] source,int width,int rows,int validBegin,int validEnd,
                int originY,int noise,boolean shadows,SingleNoise1955.Model model) {
            this(source,width,rows,validBegin,validEnd,originY,noise,shadows,model,null);
        }
        Preparation(int[] source,int width,int rows,int validBegin,int validEnd,
                int originY,int noise,boolean shadows,SingleNoise1955.Model model,Buffers1981 reusable) {
            if(source==null||model==null||width<1||rows<1)
                throw new IllegalArgumentException("GPU residual preparation lease");
            this.input=source;this.width=width;this.rows=rows;this.validBegin=validBegin;
            this.validEnd=validEnd;this.originY=originY;this.noise=noise;
            this.shadows=shadows;this.model=model;
            long bytes=cacheBytes(width,model.gpuData1960().length);
            if(bytes<1||bytes>Integer.MAX_VALUE||bytes>GpuNoise1960.MAX_BYTES)
                throw new IllegalArgumentException("GPU residual preparation cache bounds");
            cache=reusable==null?ByteBuffer.allocateDirect((int)bytes).order(ByteOrder.nativeOrder()):reusable.acquire(bytes);
            this.reusable=reusable;
        }
        static long cacheBytes(int width,int modelFloats) {
            // Native header, exact model snapshot, two previous/new tail rows.
            return 80L+4L*modelFloats+4L*((width+3L)/4+1)*1260L;
        }
        synchronized ByteBuffer prepare(int begin,int end) {
            if(input==null)throw new IllegalStateException("GPU residual preparation lease closed");
            if(Thread.currentThread().isInterrupted())throw new CancellationException("GPU residual preparation cancelled");
            long start=(long)originY+begin,finish=(long)originY+end;
            if(start<Integer.MIN_VALUE+7L||start>Integer.MAX_VALUE||finish>Integer.MAX_VALUE||end<=begin)return null;
            long first=Math.floorDiv(start-7L,4L)*4L;
            long records=((width+3L)/4+1)*((finish-first+3L)/4L);
            long bytes=records*248L*4L;
            if(bytes<1||bytes>Integer.MAX_VALUE||bytes>GpuNoise1960.MAX_BYTES)return null;
            int bank=nextBank;nextBank^=1;
            ByteBuffer storage=banks[bank];
            if(storage==null||storage.capacity()<bytes)
                banks[bank]=storage=reusable==null?ByteBuffer.allocateDirect((int)bytes).order(ByteOrder.nativeOrder()):reusable.bank(bank,bytes);
            ByteBuffer output=storage.duplicate().order(ByteOrder.nativeOrder());
            output.position(0);output.limit((int)bytes);
            output=output.slice().order(ByteOrder.nativeOrder());
            boolean okay=ColourCache1976.direct(cache,output,input,width,rows,begin,end,validBegin,validEnd,
                originY,noise,shadows,model.height,model.columns,model.rows,
                model.gpuPatchWidth1960(),model.gpuPatchHeight1960(),model.gpuData1960());
            if(Thread.currentThread().isInterrupted())throw new CancellationException("GPU residual preparation cancelled after JNI");
            return okay?output:null;
        }
        public synchronized void close(){input=null;model=null;cache=null;banks[0]=null;banks[1]=null;if(reusable!=null){Buffers1981 release=reusable;reusable=null;release.release();}}
    }
    private static native boolean prepareDirectNative(ByteBuffer cache,ByteBuffer records,
        int[] input,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,
        int noise,boolean shadows,int modelHeight,int columns,int modelRows,int patchWidth,
        int patchHeight,float[] modelData);

}
