package com.hiro.ulike;

import java.lang.ref.SoftReference;

/** Reuses large chroma/detail scratch arrays. ChromaPipeline177 serializes this path with BUSY. */
public final class WorkPool179 {
    private static SoftReference<DetailPixels.Work> workRef=new SoftReference<>(null);
    private static SoftReference<int[]> intsRef=new SoftReference<>(null);
    private WorkPool179(){}
    public static synchronized DetailPixels.Work acquireWork(int length){
        if(length<=0) throw new IllegalArgumentException("length");
        DetailPixels.Work w=workRef.get();
        if(w==null || w.source.length<length || w.source.length>((long)length*2L)){
            w=new DetailPixels.Work(length);workRef=new SoftReference<>(w);
        }
        return w;
    }
    public static synchronized int[] acquireInts(int length){
        if(length<=0) throw new IllegalArgumentException("length");
        int[] a=intsRef.get();
        if(a==null || a.length<length || a.length>((long)length*2L)){
            a=new int[length];intsRef=new SoftReference<>(a);
        }
        return a;
    }
    public static synchronized void clear(){workRef.clear();intsRef.clear();}
}
