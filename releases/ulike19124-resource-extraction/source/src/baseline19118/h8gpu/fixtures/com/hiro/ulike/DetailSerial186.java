package com.hiro.ulike;
public final class DetailSerial186 {
 public static final int[] stages=new int[4];
 public static final class Context { final Object dex;Context(Object d){dex=d;} }
 public static void filterBeforeH8(DetailPixels.Work w,int width,int rows,int first,int count,int noise,int sharp,boolean texture,boolean halos,boolean shadows){H8DexOracle1951.filter(w,width,rows,first,count,noise,sharp,texture,halos,shadows,true);}
 public static void stageBeforeH8(Context c,int phase,int first,int last){stages[phase]++;H8DexOracle1951.stage(c.dex,phase,first,last);}
 public static void bilateralBefore1950(int[] g,int[] v,int[] o,int width,int rows,int noise,boolean h,boolean t,boolean s,int begin,int end){H8DexOracle1951.call(H8DexOracle1951.OWNER+"->bilateralBefore1950([I[I[IIIIZZZII)V",g,v,o,width,rows,noise,h,t,s,begin,end);}
}
