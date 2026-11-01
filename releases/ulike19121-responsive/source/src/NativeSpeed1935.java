package com.hiro.ulike;

import java.nio.ByteBuffer;

/** Optional ARM64 kernels. A missing/unloadable library always keeps exact Java fallbacks. */
public final class NativeSpeed1935 {
    private static final boolean AVAILABLE=load();
    private NativeSpeed1935() { }
    private static boolean load() {
        try { System.loadLibrary("ulike_speed1935"); return nativeAbi()==1935; }
        catch(LinkageError unavailable) { return false; }
        catch(SecurityException unavailable) { return false; }
    }
    public static boolean available() { return AVAILABLE; }

    /** Fully validated native operation; false means no destination bytes changed. */
    static boolean pack(ByteBuffer y,int ys,int yl,int yr,int yp,
                        ByteBuffer u,int us,int ul,int ur,int up,
                        ByteBuffer v,int vs,int vl,int vr,int vp,
                        int width,int height,byte[] out) {
        if(!AVAILABLE || y==null || u==null || v==null || out==null ||
           !y.isDirect() || !u.isDirect() || !v.isDirect())return false;
        try{return packNative(y,ys,yl,yr,yp,u,us,ul,ur,up,v,vs,vl,vr,vp,width,height,out);}
        catch(OutOfMemoryError unavailable){return false;}
    }
    public static boolean horizontal(int[] raw,int[] offsets,int[] indices,float[] weights,float[] out,int width) {
        if(!AVAILABLE || raw==null || offsets==null || indices==null || weights==null || out==null ||
           weights==out || width<=0 || width>Integer.MAX_VALUE/3 || out.length<width*3 || offsets.length<=width)return false;
        try{return horizontalNative(raw,offsets,indices,weights,out,width);}
        catch(OutOfMemoryError unavailable){return false;}
    }
    public static boolean verticalAdd(float[] accum,float[] min,float[] max,float[] row,float weight,int count) {
        if(!AVAILABLE || accum==null || min==null || max==null || row==null || count<0 ||
           accum.length<count || min.length<count || max.length<count || row.length<count ||
           accum==min || accum==max || accum==row || min==max || min==row || max==row ||
           Float.isNaN(weight) || Float.isInfinite(weight))return false;
        try{return verticalAddNative(accum,min,max,row,weight,count);}
        catch(OutOfMemoryError unavailable){return false;}
    }
    private static native int nativeAbi();
    private static native boolean packNative(ByteBuffer y,int ys,int yl,int yr,int yp,
                        ByteBuffer u,int us,int ul,int ur,int up,
                        ByteBuffer v,int vs,int vl,int vr,int vp,int width,int height,byte[] out);
    private static native boolean horizontalNative(int[] raw,int[] offsets,int[] indices,float[] weights,float[] out,int width);
    private static native boolean verticalAddNative(float[] accum,float[] min,float[] max,float[] row,float weight,int count);
}
