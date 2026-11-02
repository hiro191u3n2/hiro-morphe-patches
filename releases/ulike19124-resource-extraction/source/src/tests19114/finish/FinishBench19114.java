package com.hiro.ulike;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
/** Diagnostic host timing; never used as a device certificate or speed gate. */
public final class FinishBench19114 {
 static long checksum;
 static QualityPixels1932.Plan plan(boolean varying)throws Exception {
  int sw=3060,sh=4080,rw=765,rh=1020,width=4284,height=5712;
  QualityPixels1932.Plan p=QualityPixels1932.plan(new QualityPixels1932.NoiseStats(4f,4f,128f,0f,64),800,16666666L,0,.35f,4,4,true,true,1.4f);
  Class<?> type=Class.forName("com.hiro.ulike.FaceRegions1934$Mask");
  Constructor<?> c=type.getDeclaredConstructor(int.class,int.class,int.class,int.class,byte[].class,byte[].class,double.class,double.class,double.class,double.class,double.class,double.class,boolean.class);c.setAccessible(true);
  byte[] skin=new byte[rw*rh],detail=new byte[rw*rh];Random random=new Random(19114);
  for(int i=0;i<skin.length;i++)skin[i]=(byte)(varying?random.nextInt(256):64);
  QualityPixels1932.RegionMask mask=(QualityPixels1932.RegionMask)c.newInstance(sw,sh,rw,rh,skin,detail,(rw-1.0)/(sw-1),0.0,0.0,0.0,(rh-1.0)/(sh-1),0.0,true);
  Method resample=type.getMethod("resample",int.class,int.class,int.class);p=p.withFaceRegions((QualityPixels1932.RegionMask)resample.invoke(mask,0,width,height));
  type=Class.forName("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958");c=type.getDeclaredConstructor(int.class,int.class);c.setAccessible(true);Object smooth=c.newInstance(sw,sh);
  Field f=type.getDeclaredField("confidence");f.setAccessible(true);byte[] confidence=(byte[])f.get(smooth);for(int i=0;i<confidence.length;i++)confidence[i]=(byte)(varying?random.nextInt(256):128);
  Method output=type.getDeclaredMethod("outputMask",int.class,int.class,int.class);output.setAccessible(true);
  return p.withSmoothedRegions((QualityPixels1932.SmoothMask)output.invoke(smooth,0,width,height));
 }
 static long run(QualityPixels1932.Plan plan,boolean candidate)throws Exception {
  GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new AssertionError("session");long elapsed=0;
  try{
   if(candidate){GpuFinishPolicy1992.Context c=GpuFinishPolicy1992.open(session,plan,4284,5712,128);if(c==null)throw new AssertionError("candidate context");try{
    for(int first=0;first<5712;first+=128){GpuFinishPolicy1992.Band band=c.prepare(first,Math.min(5712,first+128),(first/128)&1);try{elapsed+=band.cpuNanos;checksum+=band.pixels;}finally{band.close();}}
   }finally{c.close();}}
   else{BaselineFinishPolicy19113.Context c=BaselineFinishPolicy19113.open(session,plan,4284,5712,128);if(c==null)throw new AssertionError("reference context");try{
    for(int first=0;first<5712;first+=128){BaselineFinishPolicy19113.Band band=c.prepare(first,Math.min(5712,first+128),(first/128)&1);try{elapsed+=band.cpuNanos;checksum+=band.pixels;}finally{band.close();}}
   }finally{c.close();}}
  }finally{session.close();}
  return elapsed;
 }
 public static void main(String[] args)throws Exception {
  System.out.print("RESULT {\"status\":\"passed\",\"physical_android_tested\":false,\"device_speedup_verified\":false,\"host_timing_is_diagnostic_only\":true");
  for(boolean varying:new boolean[]{false,true}){
   QualityPixels1932.Plan plan=plan(varying);for(int i=0;i<2;i++){run(plan,false);run(plan,true);}
   long[] old=new long[7],now=new long[7];for(int i=0;i<7;i++)if((i&1)==0){old[i]=run(plan,false);now[i]=run(plan,true);}else{now[i]=run(plan,true);old[i]=run(plan,false);}
   long[] sortedOld=old.clone(),sortedNow=now.clone();Arrays.sort(sortedOld);Arrays.sort(sortedNow);
   System.out.print(",\""+(varying?"varying":"constant")+"\":{\"published113_ns\":"+Arrays.toString(old)+",\"candidate_ns\":"+Arrays.toString(now)+",\"published113_median_ns\":"+sortedOld[3]+",\"candidate_median_ns\":"+sortedNow[3]+",\"pixels\":24470208,\"source_mask_columns\":765,\"source_mask_rows\":1020}");
  }
  System.out.println(",\"checksum\":"+checksum+"}");
 }
}
