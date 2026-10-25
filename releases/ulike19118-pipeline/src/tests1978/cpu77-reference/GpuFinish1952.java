package com.hiro.ulike;

/** H12/H13 exact eligible integer final correction chain. The photograph-level
 * WholeRoute1952 gate owns pixel equality and end-to-end speed admission. These
 * raw endpoints never choose a GPU merely because one kernel is fast.
 * Float Chroma186/beauty and Java-derived noise/mask policies remain unchanged. */
public final class GpuFinish1952 {
    private static volatile int loaded;
    private GpuFinish1952() {}
    static boolean available() {
        if(loaded==0)synchronized(GpuFinish1952.class){if(loaded==0){
            try {System.loadLibrary("ulike_finish1952");loaded=nativeAbi()==19521?1:-1;}
            catch(LinkageError unavailable){loaded=-1;}
            catch(SecurityException unavailable){loaded=-1;}
        }}
        return loaded>0;
    }
    /** Mirrors the existing combined finishStrip API: sharpening reads original
     * immutable neighbors, with the moire-corrected center. Halo = 32 rows. */
    static boolean rawFinishStrip(int[] source,int[] output,int width,int rows,int first,int last,
            QualityPixels1932.Plan plan,boolean moire,boolean sharp,int originY) {
        return run(source,output,width,rows,first,last,plan,moire,sharp,originY,false);
    }
    /** Mirrors TWO separate in-place bitmap stages, without intermediate upload,
     * readback or CPU wait: moire first, then sharp reads corrected neighbors.
     * A caller must preserve 36 source halo rows (moire 32 + texture support 4).
     * Rotation/crop/resize must be a proven no-op before selecting this endpoint. */
    static boolean rawSequentialStrip(int[] source,int[] output,int width,int rows,int first,int last,
            QualityPixels1932.Plan sharpPlan,int originY) {
        return run(source,output,width,rows,first,last,sharpPlan,true,true,originY,true);
    }
    private static boolean run(int[] source,int[] output,int width,int rows,int first,int last,
            QualityPixels1932.Plan plan,boolean moire,boolean requestedSharp,int originY,boolean sequential) {
        if(source==null || output==null || source==output || width<1 || rows<1 || first<0 ||
                last<first || last>rows || (long)width*rows>source.length ||
                (long)width*rows>output.length)return false;
        if(first==last)return true;
        if(!available())return false;
        final boolean sharp=requestedSharp && plan!=null && plan.sharpGainQ8>0;
        long words=sharp?(long)width*(last-first)*4:4;
        if(words>Integer.MAX_VALUE || words>16L*1024*1024)return false;
        int[] policy=null;
        try {
            if(Thread.currentThread().isInterrupted())return false;
            policy=SpeedWorkers1935.borrowInts((int)words);
            if(sharp)for(int y=first;y<last;y++) {
                if((y&15)==0 && Thread.currentThread().isInterrupted())return false;
                for(int x=0;x<width;x++)NativeMoire1951.preparePolicy(plan,x,y+originY,
                    policy,((y-first)*width+x)*4);
            }
            return finishNative(source,output,policy,width,rows,first,last,moire,sharp,
                sharp?plan.sharpGainQ8:0,sharp?plan.sharpFloorQ8:0,
                sharp?plan.sharpLimit:0,sharp && plan.texturePriority,
                sharp && plan.haloSuppression,sequential);
        } catch(LinkageError unavailable){loaded=-1;return false;}
          catch(OutOfMemoryError optionalWorkspace){SpeedWorkers1935.trim();return false;}
        finally {SpeedWorkers1935.release(policy);}
    }
    private static native int nativeAbi();
    private static native boolean finishNative(int[] source,int[] output,int[] policy,
        int width,int rows,int first,int last,boolean moire,boolean sharp,int gain,int floor,
        int limit,boolean texture,boolean halo,boolean sequential);
}
