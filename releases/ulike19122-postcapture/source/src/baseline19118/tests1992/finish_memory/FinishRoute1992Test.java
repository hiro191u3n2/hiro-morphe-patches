package com.hiro.ulike;
import android.graphics.Bitmap;
import android.content.Context;
import java.lang.reflect.*;
import java.util.*;
/** Actual current route/control/queue, explicit pixel-transport peers. No device speed claim. */
public final class FinishRoute1992Test {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static String key(Bitmap source,QualityPixels1932.Plan plan)throws Exception{
  Method method=GpuChain1961.class.getDeclaredMethod("finishKey1976",Bitmap.class,int.class,int.class,int.class,QualityPixels1932.Plan.class,boolean.class);method.setAccessible(true);
  return (String)method.invoke(null,source,0,37,67,plan,false);
 }
 static Object field(Class<?> type,Object owner,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
 public static void main(String[] args)throws Exception{
  long[] fastest={1100,1000,1000,1000};int[] twice={2,2,2,2},valid={1,1,1,1};
  check(GpuChain1961.cpuChoice1992(950,fastest,twice,twice,valid),"CPU boundary exactly five percent wins all candidates");
  check(!GpuChain1961.cpuChoice1992(951,fastest,twice,twice,valid),"CPU less than five percent never preferred");
  for(int i=1;i<4;i++){
   long[] f=fastest.clone();f[i]=900;check(!GpuChain1961.cpuChoice1992(950,f,twice,twice,valid),"one faster GPU prevents CPU choice");
   int[] e=twice.clone();e[i]=1;check(!GpuChain1961.cpuChoice1992(900,fastest,e,twice,valid),"one exact comparison insufficient");
   int[] t=twice.clone();t[i]=1;check(!GpuChain1961.cpuChoice1992(900,fastest,twice,t,valid),"one complete timed output insufficient");
   int[] r=valid.clone();r[i]=4;check(!GpuChain1961.cpuChoice1992(900,fastest,twice,twice,r),"memory refusal never creates CPU speed proof");
   f=fastest.clone();f[i]=Long.MAX_VALUE;check(!GpuChain1961.cpuChoice1992(950,f,twice,twice,valid),"unknown GPU time excluded");
  }
  fastest[0]=900;check(!GpuChain1961.cpuChoice1992(950,fastest,twice,twice,valid),"measured faster legacy also prevents CPU choice");
  int[] missingLegacy=valid.clone();missingLegacy[0]=4;check(GpuChain1961.cpuChoice1992(950,fastest,twice,twice,missingLegacy),"unavailable legacy does not invalidate three full new-route proofs");
  Context context=new Context();GpuQualification1961.initialize(context);GpuCertificates1990.initialize(context);SaveQueue1935.idle=false;
  Method reject=GpuChain1961.class.getDeclaredMethod("finishReject1992",String.class,int[].class,int[].class,int[].class,long.class,long[].class,long.class);reject.setAccessible(true);
  reject.invoke(null,"controlled-legacy-speed",new int[]{5,5,5,5},twice,twice,1000L,new long[]{900,920,950},800L);
  Collection<?> failures=((Map<?,?>)field(GpuQualification1961.class,null,"FAILURES")).values();Object measured=null;
  for(Object failure:failures)if("legacy_finish".equals(field(failure.getClass(),failure,"measuredReference1992")))measured=failure;
  check(measured!=null,"complete comparison losing to legacy is an actual measured legacy speed failure");
  check(((Long)field(measured.getClass(),measured,"measuredCpu1992"))==800L&&((Long)field(measured.getClass(),measured,"measuredGpu1992"))==900L&&((Long)field(measured.getClass(),measured,"measuredLimit1992"))==760L,"legacy gate logs actual reference/candidate/threshold without fabricated CPU time");
  check(!GpuFinishPolicy1992.ready(),"new policy not enabled by unrelated default record");
  check(GpuCertificates1990.passed(GpuFinishDefault1990.CPU_SUITE1992,1,1,100,100),"controlled CPU-policy corpus evidence setup");
  check(GpuFinishDefault1990.ready(),"fallback standard remains admissible with unverified new policy");
  check(GpuFinishDefault1990.activeSuite1992().equals(GpuFinishDefault1990.CPU_SUITE1992),"fallback suite has independent identity");
  Bitmap source=FinishMemory1992Test.image(80,80);QualityPixels1932.Plan plan=FinishMemory1992Test.plan();Bitmap result=null;
  try{
   String baseline=GpuChain1961.residentBaseline1978(source,0,37,67,plan,true);
   check(baseline!=null&&baseline.startsWith("standard:"),"whole route can benchmark deployed standard without standalone speed win");
   Field mask=GpuChain1961.class.getDeclaredField("RESIDENT_MASK_KEY1976");mask.setAccessible(true);
   String residentKey=key(source,plan.withSmoothedRegions((QualityPixels1932.SmoothMask)mask.get(null)));
   GpuQualification1961.qualified(GpuChain1961.cpuChoiceKey1992(residentKey),1000,900,0);
   check(GpuChain1961.residentBaseline1978(source,0,37,67,plan,true).startsWith("cpu1992:"),"whole baseline follows actually preferred CPU");
   GpuQualification1961.qualified(residentKey,1000,800,1);
   check(GpuChain1961.residentBaseline1978(source,0,37,67,plan,true).startsWith("cpu1992:"),"CPU priority shared with foreground");
   GpuQualification1961.rejectNonSpeed1988(GpuChain1961.cpuChoiceKey1992(residentKey),"reference_unstable");
   check(GpuChain1961.residentBaseline1978(source,0,37,67,plan,true).startsWith("pipeline:1:"),"remaining deployed photo GPU becomes baseline");
   GpuQualification1961.rejectNonSpeed1988(residentKey,"reference_unstable");
   String legacyKey=residentKey.replace("|finish-chain1976:v2|","|finish-chain1962:v1|");
   GpuQualification1961.qualified(legacyKey,1000,900,0);
   check(GpuChain1961.residentBaseline1978(source,0,37,67,plan,true).startsWith("legacy:"),"measured legacy winner is not hidden by quality-only standard");
   GpuQualification1961.rejectExact(legacyKey);
   GpuQualification1961.rejectExact(residentKey);
   check(GpuChain1961.residentBaseline1978(source,0,37,67,plan,true)==null,"exact rejection blocks standard admission");
   Bitmap failedSource=FinishMemory1992Test.image(82,80);
   try {
    GpuQualification1961.qualified(key(failedSource,plan),1000,800,0);
    FinishStageControl1988.reset(20,0);FinishMemoryControl1989.reset(0,0);
    check(GpuChain1961.benchmarkFinish1978(failedSource,0,37,67,plan,false)==null,"failed deployed benchmark stays incomplete");
    check(FinishStageControl1988.opens==1,"timed benchmark never falls through to different standard route");
   } finally {failedSource.recycle();}
   FinishStageControl1988.reset(0,0);FinishMemoryControl1989.reset(0,0);
   result=GpuChain1961.finish(source,0,37,67,plan,false,null);
   check(result!=null,"standard foreground success preserved");
   check(FinishStageControl1988.opens==1,"no foreground candidate or duplicate CPU rendering");
   check(GpuQualification1961.retainedBytes()==4L*80*80,"standard success owns bounded post-save source copy");
   ArrayDeque<?> jobs=(ArrayDeque<?>)field(GpuQualification1961.class,null,"QUEUED");
   check(jobs.size()==1,"one full photo proof queued after standard success");
   Object job=jobs.peekFirst(),probe=field(job.getClass(),job,"probe");Bitmap owned=(Bitmap)field(probe.getClass(),probe,"val$owned");
   check(owned!=source&&!owned.isRecycled(),"queued probe owns independent immutable snapshot");
   source.recycle();check(!owned.isRecycled(),"caller recycling cannot invalidate post-save copy");
   GpuQualification1961.captureChanged();check(owned.isRecycled()&&GpuQualification1961.retainedBytes()==0,"cancellation closes copy and reservation exactly once");
  }finally{GpuQualification1961.captureChanged();if(result!=null)result.recycle();if(!source.isRecycled())source.recycle();}
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"actual_route_control\":true,\"native_transport_mocked\":true,\"physical_android_tested\":false}");
 }
}
