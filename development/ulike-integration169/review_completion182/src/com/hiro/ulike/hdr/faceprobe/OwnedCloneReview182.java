package com.hiro.ulike.hdr.faceprobe;import java.io.*;import java.util.*;
public final class OwnedCloneReview182{
 static int checks;static void ok(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  for(Throwable cause:new Throwable[]{new IOException("receiver"),new OutOfMemoryError("receiver")}){
   int[] sdk={9,7,5,3},before=sdk.clone();int[][] alias={null};Throwable seen=null;
   try{OwnedRenderPixels.copyAndDeliver(sdk,2,2,2,2,AnalysisCapacity.legacyDiagnostic(),(owned,w,h)->{alias[0]=owned;ok(owned!=sdk,"separateclone");if(cause instanceof Exception)throw(Exception)cause;throw(Error)cause;});}catch(Exception|Error e){seen=e;}
   ok(seen==cause,"samefailure");ok(Arrays.equals(alias[0],new int[4]),"undeliveredclone wiped");ok(Arrays.equals(sdk,before),"SDKborrow untouched");
  }
  int[] sdk={1,2,3,4};int[][] received={null};OwnedRenderPixels.copyAndDeliver(sdk,2,2,2,2,AnalysisCapacity.legacyDiagnostic(),(owned,w,h)->received[0]=owned);ok(received[0]!=sdk&&Arrays.equals(received[0],sdk),"deliveredclone preserved");received[0][0]=99;ok(sdk[0]==1,"receiverisolatedfromSDK");
  int[] calls={0};try{OwnedRenderPixels.copyAndDeliver(sdk,1,4,2,2,AnalysisCapacity.legacyDiagnostic(),(p,w,h)->calls[0]++);throw new AssertionError("badgrid accepted");}catch(IllegalArgumentException e){ok(calls[0]==0,"invalidgrid neverdelivered");}
  System.out.println("PASS OwnedCloneReview182 "+checks+" bounded host checks");
 }
}
