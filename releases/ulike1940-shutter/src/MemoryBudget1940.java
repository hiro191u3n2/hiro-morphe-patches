package com.hiro.ulike;

/** Separate real Java YUV buffers from native Bitmap/renderer allocations.
 * The pixel algorithms, sensor frames, dimensions and encoder quality are unchanged. */
public final class MemoryBudget1940 {
    private MemoryBudget1940() {}
    public static final long MIB=1024L*1024L;
    public static final long JAVA_RESERVE=72L*MIB;
    public static final long SYSTEM_RESERVE=256L*MIB;
    public static final long NATIVE_RESERVE=96L*MIB;
    public static final long MAX_PIXELS=32000000L;
    public static final long MAX_PROCESS=1536L*MIB;
    public static final long MIN_PROCESS=768L*MIB;
    public static boolean dimensions(long pixels){return pixels>0 && pixels<=MAX_PIXELS;}
    /** Four NV21 references, a fusion output and a conversion scratch = 9 B/pixel.
     * JAVA_RESERVE also covers strip workers and the unchanged single save worker. */
    public static boolean javaAllows(long available,long pixels){return javaAllows(available,pixels,0);}
    public static boolean javaAllows(long available,long pixels,long qualityFloor){
        return dimensions(pixels)&&qualityFloor>=0&&qualityFloor<=Long.MAX_VALUE-pixels*9L
            &&available>=Math.max(JAVA_RESERVE,qualityFloor)+pixels*9L;
    }
    /** Exact guard threshold for ChromaPipeline186.plan's selected tuple. The
     * radius is Chroma179.radius and the search order/tiling/native Work budget
     * are copied from the pinned inherited plan, not a guessed NR setting. */
    public static long chromaPlanBytes(int width,int height,int radius,int core,int workers,int nativeWork){
        long rows=Math.min(height,core+2L*(radius+7));
        long values=(long)width*rows;
        long perWorker=values*16L+(long)width*36L+4L*MIB;
        if(nativeWork==1)perWorker+=values*12L+(long)width*36L;
        return 40L*MIB+(long)width*height*4L+workers*perWorker;
    }
    public static int chromaRadius(int width,int height){return Math.max(12,Math.min(128,(Math.min(width,height)+16)/32));}
    public static long selectedChromaFloor(int width,int height,long javaCeiling,int processors){
        if(width<1||height<1||width>8192||height>8192||javaCeiling<0||processors<1)return Long.MAX_VALUE;
        int radius=chromaRadius(width,height);
        int most=height>=1024?Math.min(4,processors):1;
        for(int workers=most;workers>=1;workers--)for(int nativeWork=1;nativeWork>=0;nativeWork--)
            for(int core=512;core>=128;core/=2){
                if(workers>1&&core<256)continue;
                if(workers>((long)height+core-1)/core)continue;
                long need=chromaPlanBytes(width,height,radius,core,workers,nativeWork);
                if(need<=javaCeiling)return need;
            }
        // If the original plan already has no capacity, never add another
        // capture during its quality processing. Encoding still frees this floor.
        return Long.MAX_VALUE;
    }
    /** Retain all unchanged Java guards even when they reserve native Bitmaps:
     * mutable/normalize 4P+32MiB; FastResize output+axes+full bounded caches and
     * quarter-turn buffers; four source/output finish strips; and the best
     * inherited Chroma plan which this process could choose without overlap.
     * Using the VM ceiling covers queued jobs after an older job frees its arrays. */
    public static long qualityFloor(int width,int height,int outWidth,int outHeight,long javaCeiling,int processors){
        if(width<1||height<1||outWidth<1||outHeight<1||width>8192||height>8192||outWidth>8192||outHeight>8192)return Long.MAX_VALUE;
        long source=(long)width*height,output=(long)outWidth*outHeight;
        if(!dimensions(source)||!dimensions(output))return Long.MAX_VALUE;
        long chroma=Math.max(selectedChromaFloor(width,height,javaCeiling,processors),selectedChromaFloor(outWidth,outHeight,javaCeiling,processors));
        long normalize=32L*MIB+4L*Math.max(source,output)+(long)Math.max(Math.max(width,height),outWidth)*256L;
        long axes=((long)width+height+outWidth+outHeight)*96L;
        int workers=Math.min(4,Math.max(1,processors));
        // FastPixels bounds each complete row cache at 4MiB. Per-worker terms
        // include RGB accum/min/max, batch16, raw/output, row ids and a 32-column
        // rotation cache. Axes are allocated before worker admission.
        long resizeWorker=4L*MIB+(long)outWidth*(36L+64L+4L)+(long)Math.max(width,height)*4L
            +(long)Math.min(32,width)*height*4L+128L*64L+2048L;
        long resize=32L*MIB+output*4L+axes+workers*resizeWorker;
        long stripWidth=Math.max(width,outWidth),stripHeight=Math.max(height,outHeight);
        long strips=32L*MIB+workers*stripWidth*Math.min(stripHeight,128L+2L*32L)*8L+stripWidth*32L*4L;
        return Math.max(chroma,Math.max(normalize,Math.max(resize,strips)));
    }
    public static long nativeHeadroom(long systemAvailable,long systemThreshold,long total,long processPss,long nativeAllocated,boolean lowMemory){
        if(lowMemory||systemAvailable<=0||total<=0||processPss<=0||nativeAllocated<0)return 0;
        long reserve=Math.max(SYSTEM_RESERVE,systemThreshold);
        long cap=Math.min(MAX_PROCESS,Math.max(MIN_PROCESS,total/8L));
        // PSS includes resident Bitmap pages, Java and renderer allocations.
        // The native heap guard also counts allocations which may not be resident yet.
        return Math.max(0,Math.min(systemAvailable-reserve,Math.min(cap-processPss,cap-nativeAllocated)));
    }
    /** Next full-resolution renderer + exact owned snapshot + working native
     * renderer/encoder reserve, plus this photo's pending full output. Already
     * held photos are present in PSS/native allocated and therefore not double counted. */
    public static boolean nativeAllows(long available,long pixels,long outputPixels,int bytesPerPixel){
        if(!dimensions(pixels)||outputPixels<0||outputPixels>MAX_PIXELS||bytesPerPixel<2||bytesPerPixel>8)return false;
        long need=NATIVE_RESERVE+pixels*(bytesPerPixel+12L)+outputPixels*8L;
        return available>=need;
    }
    public static boolean permits(long javaAvailable,long nativeAvailable,long pixels,long outputPixels,int bytesPerPixel){
        return permits(javaAvailable,nativeAvailable,pixels,outputPixels,bytesPerPixel,0);
    }
    public static boolean permits(long javaAvailable,long nativeAvailable,long pixels,long outputPixels,int bytesPerPixel,long qualityFloor){
        return qualityFloor!=Long.MAX_VALUE&&javaAllows(javaAvailable,pixels,qualityFloor)&&nativeAllows(nativeAvailable,pixels,outputPixels,bytesPerPixel);
    }
}
