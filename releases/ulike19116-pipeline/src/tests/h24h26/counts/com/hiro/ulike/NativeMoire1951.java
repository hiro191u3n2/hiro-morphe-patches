package com.hiro.ulike;
/** Only policy-call/storage control is tested here; actual policy floats are
 * verified independently against the pinned published .53 implementation. */
public final class NativeMoire1951 {
 public static int calls,width,overflowAt,uniformPrefix,interruptAt,throwAt;
 public static boolean constant;
 public static final RuntimeException FAILURE=new RuntimeException("mask-failure");
 public static void preparePolicy(QualityPixels1932.Plan p,int x,int y,int[] out,int at) {
  int index=calls++;
  if(calls==interruptAt)Thread.currentThread().interrupt();
  if(calls==throwAt)throw FAILURE;
  if(p.texturePriority && p.faceRegions!=null)p.faceRegions.skinQ8(x,y);
  values(index,out,at);
 }
 public static void values(int index,int[] out,int at) {
  int value=constant || index<uniformPrefix?7:index&65535;
  out[at]=value;out[at+1]=65535-value;out[at+2]=value^12345;
  out[at+3]=index>=overflowAt?Integer.MIN_VALUE+index:256;
 }
}
