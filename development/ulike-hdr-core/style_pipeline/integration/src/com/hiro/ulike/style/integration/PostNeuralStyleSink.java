package com.hiro.ulike.style.integration;

import com.hiro.ulike.style.SampledMakeupPipeline;
import com.hiro.ulike.style.StyleLutPipeline;
import hiro.ulike.beauty.BeautyImageEngine;
import java.util.List;

/** Transactional bridge from the real neural image engine's borrowed float
 * rows to the verified makeup/LUT stages and a private final staging sink.
 * Only explicit SDR is supported. This adapter never manufactures geometry,
 * runtime shader parameters, skin masks or HDR appearance information.
 */
public final class PostNeuralStyleSink implements BeautyImageEngine.TransactionalSink {
    public enum GraphPolicy {
        /** Replacement follows declared outer z-orders; caller resolves every
         * renderer at equal z and every source-texture binding. This declaration
         * does not establish the original SDK's actual runtime graph.
         */
        DECLARED_OUTER_ORDER_WITH_CALLER_RESOLVED_SOURCES_AND_SUBORDER
    }
    public static final class Budget {
        public final int maxIncomingRows,maxTilePixels;
        public final long maxImagePixels,maxModuleArrayBytesPerTile;
        public Budget(int maxIncomingRows,int maxTilePixels,long maxImagePixels,long maxArrayBytes) {
            require(maxIncomingRows>0 && maxIncomingRows<=64 && maxTilePixels>0
                    && maxTilePixels<=SampledMakeupPipeline.MAX_TILE_PIXELS
                    && maxImagePixels>0 && maxArrayBytes>0,"invalid adapter budget");
            this.maxIncomingRows=maxIncomingRows;this.maxTilePixels=maxTilePixels;
            this.maxImagePixels=maxImagePixels;this.maxModuleArrayBytesPerTile=maxArrayBytes;
        }
        public static Budget standard() {return new Budget(64,262144,25000000,32L*1024*1024);}
    }
    public static final class ResolvedTile {
        public final Object frameIdentity;
        public final List<SampledMakeupPipeline.ResolvedPass> makeup;
        public final StyleLutPipeline.ResolvedLuts luts;
        public ResolvedTile(Object frameIdentity,List<SampledMakeupPipeline.ResolvedPass> makeup,
                            StyleLutPipeline.ResolvedLuts luts) {
            this.frameIdentity=frameIdentity;this.makeup=makeup;this.luts=luts;
        }
    }
    public interface BindingProvider {
        /** Must return geometry/samples/uniforms for exactly this immutable still
         * frame and oriented tile. Arrays are borrowed until this call's output
         * has been consumed. Implementations must not use unmatched preview data.
         */
        ResolvedTile resolve(Object exactFrameIdentity,SampledMakeupPipeline.FrameTile tile)throws Exception;
    }
    private enum State { NEW,ACTIVE,COMMITTED,ABORTED }
    private State state=State.NEW;
    private int nextRow;
    private final Object expectedIdentity;
    private final String captureId;
    private final long timestamp;
    private final int width,height;
    private final SampledMakeupPipeline.Style style;
    private final SampledMakeupPipeline.Domain domain;
    private final Budget budget;
    private final BindingProvider bindings;
    private final BeautyImageEngine.TransactionalSink downstream;

    public PostNeuralStyleSink(Object expectedFrameIdentity,String captureId,long sensorTimestampNs,
                              int width,int height,SampledMakeupPipeline.Style style,
                              SampledMakeupPipeline.Domain domain,GraphPolicy graphPolicy,
                              Budget budget,BindingProvider bindings,
                              BeautyImageEngine.TransactionalSink downstream) {
        require(expectedFrameIdentity!=null && captureId!=null && !captureId.isEmpty()
                && captureId.length()<=256 && sensorTimestampNs>=0,"owned still identity required");
        require(width>0 && height>0 && width<=16384 && height<=16384,"invalid full image dimensions");
        require(style!=null && domain==SampledMakeupPipeline.Domain.ENCODED_SDR_FULL_RANGE
                && graphPolicy==GraphPolicy.DECLARED_OUTER_ORDER_WITH_CALLER_RESOLVED_SOURCES_AND_SUBORDER,
                "explicit SDR and replacement graph policy required");
        require(budget!=null && bindings!=null && downstream!=null
                && (long)width*height<=budget.maxImagePixels && width<=budget.maxTilePixels,
                "missing binding/sink or frame budget exceeded");
        this.expectedIdentity=expectedFrameIdentity;this.captureId=captureId;this.timestamp=sensorTimestampNs;
        this.width=width;this.height=height;this.style=style;this.domain=domain;
        this.budget=budget;this.bindings=bindings;this.downstream=downstream;
        require(arrayBytesForPixels(width)<=budget.maxModuleArrayBytesPerTile,
                "one full output row exceeds module array budget");
    }
    private static void require(boolean b,String message) {
        if(!b)throw new IllegalArgumentException(message);
    }
    /** Sum of primitive pixel arrays allocated by this module plus the invoked
     * verified stages per tile. This is an allocation-volume bound, not a heap
     * peak estimate. Caller source/binding/LUT/staging arrays, GC retention, JVM
     * headers, neural model/native storage and object allocations are excluded.
     */
    public long arrayBytesForPixels(int pixels) {
        require(pixels>0 && pixels<=262144,"invalid tile pixel count");
        int doubleArrays=style==SampledMakeupPipeline.Style.NATURAL_BLUSH?4:13;
        return (long)pixels*3*(doubleArrays*8+4);
    }
    @Override public synchronized void begin(int w,int h,Object identity)throws Exception {
        if(state!=State.NEW)throw new IllegalStateException("adapter is single use");
        require(w==width && h==height && identity==expectedIdentity,"neural output is not the expected still");
        state=State.ACTIVE; // begin failures may still need staging cleanup
        try {downstream.begin(w,h,identity);}catch(Exception|Error failure){rollback(failure);throw failure;}
    }
    @Override public synchronized void writeRows(int firstRow,int rows,float[] rgb)throws Exception {
        if(state!=State.ACTIVE)throw new IllegalStateException("no active staging transaction");
        try {
            require(firstRow==nextRow && rows>0 && rows<=budget.maxIncomingRows
                    && (long)firstRow+rows<=height,"noncontiguous or oversized neural rows");
            long used=(long)width*rows*3,maxBuffer=(long)width*budget.maxIncomingRows*3;
            require(rgb!=null && rgb.length>=used && rgb.length<=maxBuffer,"borrowed RGB row buffer size mismatch");
            // The neural engine may reuse its maximum-size array for a final
            // partial row group. Only the prefix described by rows is consumed.
            for(int i=0;i<(int)used;i++)require(Float.isFinite(rgb[i]) && rgb[i]>=0 && rgb[i]<=1,"invalid encoded SDR photo");
            int maxRows=Math.min(budget.maxTilePixels/width,rows);
            while(arrayBytesForPixels(width*maxRows)>budget.maxModuleArrayBytesPerTile)maxRows--;
            require(maxRows>0,"tile budget cannot fit a full row");
            for(int offset=0;offset<rows;offset+=maxRows) {
                int count=Math.min(maxRows,rows-offset),pixels=width*count;
                SampledMakeupPipeline.FrameTile tile=new SampledMakeupPipeline.FrameTile(captureId,timestamp,
                    width,height,0,firstRow+offset,width,count);
                ResolvedTile bound=bindings.resolve(expectedIdentity,tile);
                require(bound!=null && bound.frameIdentity==expectedIdentity,"resolved inputs belong to a different still");
                double[] source=new double[pixels*3];int start=offset*width*3;
                for(int i=0;i<source.length;i++)source[i]=rgb[start+i];
                double[] processed=StyleLutPipeline.runPostNeural(style,domain,tile,source,bound.makeup,bound.luts);
                require(processed.length==source.length,"style stages changed pixel dimensions");
                float[] out=new float[processed.length];
                for(int i=0;i<out.length;i++) {
                    double v=processed[i];require(Double.isFinite(v) && v>=0 && v<=1,"invalid style output");
                    out[i]=(float)v; // FP32 output, never integer/8-bit quantization
                }
                downstream.writeRows(firstRow+offset,count,out);
            }
            nextRow+=rows;
        }catch(Exception|Error failure){rollback(failure);throw failure;}
    }
    @Override public synchronized void commit()throws Exception {
        if(state!=State.ACTIVE)throw new IllegalStateException("no active transaction to commit");
        try {
            require(nextRow==height,"cannot publish incomplete photo");
            downstream.commit();state=State.COMMITTED;
        }catch(Exception|Error failure){rollback(failure);throw failure;}
    }
    private void rollback(Throwable failure) {
        if(state!=State.ACTIVE)return;
        state=State.ABORTED;
        try{downstream.abort();}catch(Exception|Error cleanup){failure.addSuppressed(cleanup);}
    }
    @Override public synchronized void abort()throws Exception {
        if(state==State.ACTIVE){state=State.ABORTED;downstream.abort();}
        else if(state==State.NEW)state=State.ABORTED;
        // Parent neural failure handling may call abort after this adapter has
        // already rolled back. Cleanup remains idempotent; commits stay final.
    }
}
