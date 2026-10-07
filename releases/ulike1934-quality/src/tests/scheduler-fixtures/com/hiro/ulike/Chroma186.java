package com.hiro.ulike;
/** Known chroma transform exposes any erroneous source-restoring mix AFTER colour. */
public final class Chroma186 {
 static int corrected(int p){int r=Math.min(255,((p>>>16)&255)+7),g=(p>>>8)&255,b=Math.max(0,(p&255)-3);return 0xff000000|(r<<16)|(g<<8)|b;}
 public static void finishWorkspace(int[] raw,int[] processed,int[] out,int width,int rows,int begin,int end,int radius,int[] columns,int[] covariance,boolean nativeAllowed){
  for(int i=begin*width;i<end*width;i++)out[i]=corrected(processed[i]);
 }
}
