package com.hiro.ulike;
final class PipelineDetail1988 {static final int CPU_FIXED=1,UNKNOWN=0;}
final class GpuSingle1960 {
 static long workspaceBytes(int width,int rows){return 0;}
 static boolean process(int[] a,int[] b,int w,int h,int lo,int hi,int vb,int ve,int origin,int noise,boolean shadow,SingleNoise1955.Model model,SingleNoise1955.Protection policy){return false;}
 static boolean process1981(int[] a,int[] b,int w,int h,int lo,int hi,int vb,int ve,int origin,int noise,boolean shadow,SingleNoise1955.Model model,SingleNoise1955.Protection policy,SingleNoise1955.Workspace workspace){return false;}
}
final class GpuPolicy1960 {
 static class Protection implements SingleNoise1955.Protection {
  public int budgetQ8(int x,int y){return 256;}public int detailQ8(int x,int y){return 0;}
  void freezePolicy1981(int width,int rows,int origin,int[] p,boolean ignored){for(int i=0;i<width*rows;i++){p[i*2]=256;p[i*2+1]=0;}}
 }
}
final class CpuExact1978 {static boolean enabled(String key){return false;}}
final class CpuSingle1978 {
 static int mode,offers;static SingleNoise1955.Model current;
 static long create(int w,int h){return 0;}static void release(long p,int w,int h){}
 static SingleNoise1955.Model probe(SingleNoise1955.Patches p,int w,int h){return SingleNoise1955.probe1978(p,w,h,false);}
 static int[] geometry(int w,int h,int lo,int hi,int vb,int ve,int origin,int noise,boolean shadow,SingleNoise1955.Model model){return new int[]{w,h,lo,hi,vb,ve,origin,noise,shadow?1:0};}
 static String key(boolean n,int[] u,boolean p){return "controlled";}
 static boolean nativeCall(int[] in,int[] out,int[] u,float[] model,int[] policy,SingleNoise1955.Workspace workspace){
  java.util.Arrays.fill(out,0x12345678);
  if(mode==1)return false;if(mode==2)throw new UnsatisfiedLinkError("controlled missing process symbol");
  SingleNoise1955.processJavaRange(in,out,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[7],u[8]!=0,current,new GpuPolicy1960.Protection());return true;
 }
 static void offerNative(String key,int[] in,int[] u,float[] model,int[] policy){offers++;}
 static void javaRange(int[] in,int[] out,int w,int h,int lo,int hi,int vb,int ve,int origin,int noise,boolean shadow,SingleNoise1955.Model model,SingleNoise1955.Protection p,SingleNoise1955.Workspace workspace){SingleNoise1955.processJavaRange1978(in,out,w,h,lo,hi,vb,ve,origin,noise,shadow,model,p,null);}
}
