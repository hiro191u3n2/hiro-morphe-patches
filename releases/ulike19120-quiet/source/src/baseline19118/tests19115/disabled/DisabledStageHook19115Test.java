package com.hiro.ulike;
import java.lang.reflect.Method;

/** Exact production nested hook source, not full Stage or GPU execution. */
public final class DisabledStageHook19115Test {
 static int assertions;
 static void check(boolean ok,String label){assertions++;if(!ok)throw new AssertionError(label);}
 public static void main(String[] args)throws Exception{
  boolean absent=false;try{Class.forName("com.hiro.ulike.GpuRegionalDefault19115");}catch(ClassNotFoundException expected){absent=true;}check(absent,"Stage hook classpath excludes regional helper");
  Class<?> owner=Class.forName("com.hiro.ulike.GpuStrong1960$RegionalHooks19115");
  Method ready=owner.getDeclaredMethod("ready",Integer.TYPE,Integer.TYPE);ready.setAccessible(true);
  Method profile=owner.getDeclaredMethod("profile",Integer.TYPE,Integer.TYPE);profile.setAccessible(true);
  int adopted=0;
  for(int[] wh:new int[][]{{3060,4080},{4080,3060},{65,65},{0,0}}){
   boolean value=((Boolean)ready.invoke(null,wh[0],wh[1])).booleanValue();if(value)adopted++;
   check(!value,"missing helper disables automatic Stage adoption");
   check(((Integer)profile.invoke(null,wh[0],wh[1])).intValue()==GpuStrongDefault1990.profile1994(wh[0],wh[1]),"Stage hook retains existing shape-specific profile owner");
  }
  check(adopted==0,"Stage optional adoption count is zero");
  check(GpuNoise1960.optionalReadinessQueries==0,"missing helper returns before optional GPU readiness query");
  check(GpuNoise1960.optionalPrepareCalls==0,"missing Stage hook does not prepare optional GPU programs");
  Method report=owner.getDeclaredMethod("report",ProcessingTiming1947.Trace.class,Integer.TYPE,Long.TYPE);report.setAccessible(true);report.invoke(null,new ProcessingTiming1947.Trace(),0,0L);
  check(ProcessingTiming1947.reportCalls==1&&ProcessingTiming1947.lastStrips==0&&ProcessingTiming1947.lastPixels==0,"ordinary Stage can report zero actual adoption");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"stage_auto_adoption_count\":"+adopted+",\"optional_readiness_queries\":"+GpuNoise1960.optionalReadinessQueries+",\"optional_gpu_prepare_calls\":"+GpuNoise1960.optionalPrepareCalls+",\"exact_production_hook_extracted\":true,\"full_stage_executed\":false}");
 }
}
