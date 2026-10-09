package com.hiro.ulike;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CancellationException;

/** Exact native1955 binary64 preparation for the FP32 GPU residual finish.
 * Records contain only the original float casts; all transforms, thresholds,
 * residual safeguards and source samples remain the current CPU arithmetic. */
final class SingleResidual1961 {
    private SingleResidual1961() {}
    static float[] prepare(int[] input,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,
            SingleNoise1955.Model model) {
        return prepareNative(input,width,rows,begin,end,validBegin,validEnd,originY,
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
        private int nextBank;
        Preparation(int[] source,int width,int rows,int validBegin,int validEnd,
                int originY,int noise,boolean shadows,SingleNoise1955.Model model) {
            if(source==null||model==null||width<1||rows<1)
                throw new IllegalArgumentException("GPU residual preparation lease");
            this.input=source;this.width=width;this.rows=rows;this.validBegin=validBegin;
            this.validEnd=validEnd;this.originY=originY;this.noise=noise;
            this.shadows=shadows;this.model=model;
            long bytes=cacheBytes(width,model.gpuData1960().length);
            if(bytes<1||bytes>Integer.MAX_VALUE||bytes>GpuNoise1960.MAX_BYTES)
                throw new IllegalArgumentException("GPU residual preparation cache bounds");
            cache=ByteBuffer.allocateDirect((int)bytes).order(ByteOrder.nativeOrder());
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
                banks[bank]=storage=ByteBuffer.allocateDirect((int)bytes).order(ByteOrder.nativeOrder());
            ByteBuffer output=storage.duplicate().order(ByteOrder.nativeOrder());
            output.position(0);output.limit((int)bytes);
            output=output.slice().order(ByteOrder.nativeOrder());
            boolean okay=prepareDirectNative(cache,output,input,width,rows,begin,end,validBegin,validEnd,
                originY,noise,shadows,model.height,model.columns,model.rows,
                model.gpuPatchWidth1960(),model.gpuPatchHeight1960(),model.gpuData1960());
            if(Thread.currentThread().isInterrupted())throw new CancellationException("GPU residual preparation cancelled after JNI");
            return okay?output:null;
        }
        public synchronized void close(){input=null;model=null;cache=null;banks[0]=null;banks[1]=null;}
    }
    private static native boolean prepareDirectNative(ByteBuffer cache,ByteBuffer records,
        int[] input,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,
        int noise,boolean shadows,int modelHeight,int columns,int modelRows,int patchWidth,
        int patchHeight,float[] modelData);

}
