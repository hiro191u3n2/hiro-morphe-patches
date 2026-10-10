package com.hiro.ulike;
import java.lang.reflect.*;import java.util.*;
public final class NativeCache1976Test {
 static int assertions;static void check(boolean x,String s){assertions++;if(!x)throw new AssertionError(s);}
 public static void main(String[] ignored)throws Exception {
  Method borrow=NativeMoire1951.class.getDeclaredMethod("borrowCache1976",int.class,int.class,boolean.class,boolean.class);borrow.setAccessible(true);
  GpuNoise1960.allowBudget=false;check(borrow.invoke(null,128,100,true,true)==null,"GPU reservation refusal disables optional cache");GpuNoise1960.allowBudget=true;
  check(borrow.invoke(null,7000,100,true,true)==null,"two MiB per-worker cap");
  int[] lease=(int[])borrow.invoke(null,128,100,true,true);check(lease!=null&&lease.length==128*80+2&&lease[0]==-1&&lease[1]==0,"bounded cache starts uninitialized");SpeedWorkers1935.release(lease);
  Method entry=NativeMoire1951.class.getDeclaredMethod("finishStripCached1976",int[].class,int[].class,int[].class,boolean.class,int.class,int.class,int.class,int.class,boolean.class,boolean.class,int.class,int.class,int.class,boolean.class,boolean.class,int[].class);entry.setAccessible(true);
  int w=87,h=91;int[] src=new int[w*h],out=new int[src.length],old=new int[src.length],scratch=new int[w*80+2];scratch[0]=-1;Arrays.fill(out,0x12345678);Arrays.fill(old,0x12345678);
  for(int i=0;i<src.length;i++){int z=(i*173+i/87*37)&255;src[i]=0xff000000|z<<16|z<<8|z;}
  Object[] args={src,out,null,false,w,h,0,16,true,false,0,0,0,false,false,scratch};
  check((Boolean)entry.invoke(null,args),"actual new native entry executes");check(scratch[0]==0&&scratch[1]==48,"native records first input band");
  args[6]=16;args[7]=32;check((Boolean)entry.invoke(null,args),"next native band executes");check(scratch[0]==0&&scratch[1]==64,"rolling extends exact shared rows");
  args[6]=32;args[7]=48;check((Boolean)entry.invoke(null,args),"full support band executes");check(scratch[1]==80,"bounded max support");
  args[6]=48;args[7]=64;check((Boolean)entry.invoke(null,args),"band shifts and reuses halo");check(scratch[0]==16&&scratch[1]==75,"bottom clamp tracks valid rows");
  QualityPixels1932.finishStripAtBefore1951(src,old,w,h,0,64,null,true,false,0);check(Arrays.equals(out,old),"actual cached JNI full exact moire");
  int[] snap=out.clone();args[15]=new int[4];check(!(Boolean)entry.invoke(null,args)&&Arrays.equals(out,snap),"insufficient cache rejected before writes");
  args[15]=src;check(!(Boolean)entry.invoke(null,args)&&Arrays.equals(out,snap),"cache source alias rejected");args[15]=out;check(!(Boolean)entry.invoke(null,args)&&Arrays.equals(out,snap),"cache destination alias rejected");
  args[15]=scratch;args[7]=70;check(!(Boolean)entry.invoke(null,args)&&Arrays.equals(out,snap),"batch longer than 16 rejected");
  GpuQualification1961.Record admitted=new GpuQualification1961.Record();admitted.cpuNanos=100;admitted.gpuNanos=70;admitted.variant=1;GpuQualification1961.admitted=admitted;
  Arrays.fill(out,0x12345678);check(NativeMoire1951.testNative(src,out,w,h,0,64)&&Arrays.equals(out,old),"certified foreground native-cache integration remains exact");
  GpuNoise1960.allowBudget=false;Arrays.fill(out,0x12345678);check(NativeMoire1951.testNative(src,out,w,h,0,64)&&Arrays.equals(out,old),"certified foreground memory refusal uses original native kernel");GpuNoise1960.allowBudget=true;GpuQualification1961.admitted=null;
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+"}");
 }
}
