package com.hiro.ulike;

/** Exact integer residual/texture aggregation, once per bounded strip.
 * The retained v1935 library also exports all older YUV/resize entry points.
 * A missing or old native library leaves the same-pixel Java path available.
 */
public final class NativeSpeed1944 {
    private static final boolean AVAILABLE=load();
    private static final int[] OFFSETS={-4,-2,-1,0,1,2,4};
    private NativeSpeed1944() { }
    private static boolean load() {
        try {System.loadLibrary("ulike_speed1935");return nativeAbi()==1944;}
        catch(LinkageError unavailable){return false;}
        catch(SecurityException unavailable){return false;}
    }
    public static boolean available(){return AVAILABLE;}

    /** meta ACTIVE bit31, range sigma bits0..5, texture threshold bits6..11.
     * summaries: fine RGB24, coarse RGB24, ACTIVE|periodicQ8<<16|edge<<8|lumaRange.
     * All inactive summaries have zero stats. No destination pixels are changed.
     */
    static boolean aggregate(int[] input,int[] meta,int[] summaries,int width,int rows,
            int begin,int end,int validBegin,int validEnd,int radius,int[] flatRange) {
        if(!AVAILABLE || input==null || meta==null || summaries==null || flatRange==null ||
                input==meta || input==summaries || meta==summaries || input==flatRange ||
                meta==flatRange || summaries==flatRange || width<1 || rows<1 ||
                radius<1 || radius>4 || validBegin<0 || validEnd>rows ||
                begin<validBegin || end<begin || end>validEnd ||
                (long)width*rows>input.length || (long)width*(end-begin)>meta.length ||
                (long)width*(end-begin)*3>summaries.length || flatRange.length<33*256 ||
                (long)width*(radius*2+1)>Integer.MAX_VALUE)return false;
        if(begin==end)return true;
        int[] ring=null,x=null;
        try {
            int taps=radius==4?7:radius*2+1;
            ring=SpeedWorkers1935.borrowInts(width*(radius*2+1));
            x=SpeedWorkers1935.borrowInts(width*taps);
            for(int c=0;c<width;c++)for(int i=0;i<taps;i++){
                int dx=radius==4?OFFSETS[i]:i-radius;
                x[c*taps+i]=Math.max(0,Math.min(width-1,c+dx));
            }
            return aggregateNative(input,meta,summaries,width,rows,begin,end,validBegin,
                    validEnd,radius,flatRange,ring,x);
        } catch(OutOfMemoryError unavailable){return false;}
          catch(LinkageError unavailable){return false;}
          finally {SpeedWorkers1935.release(ring);SpeedWorkers1935.release(x);}
    }
    private static native int nativeAbi();
    /** Group existing vertical taps in their original order into one JNI call.
     * The caller pins only cached rows that fit simultaneously in its LRU cache.
     */
    static boolean verticalBatch(float[][] rows,float[] weights,int first,int taps,
            float[] accum,float[] minimum,float[] maximum,int count) {
        if(!AVAILABLE || rows==null || weights==null || accum==null || minimum==null ||
                maximum==null || taps<1 || taps>128 || rows.length<taps || first<0 ||
                (long)first+taps>weights.length || count<1 || accum.length<count ||
                minimum.length<count || maximum.length<count || accum==minimum ||
                accum==maximum || minimum==maximum)return false;
        for(int t=0;t<taps;t++){
            float[] row=rows[t];float weight=weights[first+t];
            if(row==null || row.length<count || row==accum || row==minimum || row==maximum ||
                    Float.isNaN(weight) || Float.isInfinite(weight))return false;
        }
        try{return verticalBatchNative(rows,weights,first,taps,accum,minimum,maximum,count);}
        catch(OutOfMemoryError unavailable){return false;}
        catch(LinkageError unavailable){return false;}
    }
    private static native boolean aggregateNative(int[] input,int[] meta,int[] summaries,
            int width,int rows,int begin,int end,int validBegin,int validEnd,int radius,
            int[] flatRange,int[] ring,int[] x);
    private static native boolean verticalBatchNative(float[][] rows,float[] weights,
            int first,int taps,float[] accum,float[] minimum,float[] maximum,int count);
}
