package com.hiro.ulike;
/** Host-only colour transform is identity, so pipeline noise/off properties isolate NR. */
public final class Chroma186 {
 public static void finishWorkspace(int[] source,int[] filtered,int[] out,int width,int rows,int lo,int hi,int radius,int[] columns,int[] covariance,boolean nativeAllowed){System.arraycopy(filtered,lo*width,out,lo*width,(hi-lo)*width);}
}
