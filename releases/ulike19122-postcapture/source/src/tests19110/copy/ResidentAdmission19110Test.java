package com.hiro.ulike;
import android.content.Context;import android.graphics.Bitmap;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.Callable;
/** Actual resident producer/queue/comparator with controlled Strong sentinel and
 * pixel dispatch. The independent Strong suite verifies the actual sentinel. */
public final class ResidentAdmission19110Test {
 static int checks,cases;
 static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 static void waitFor(Callable<Boolean> p)throws Exception{long end=System.nanoTime()+5000000000L;while(!p.call()){if(System.nanoTime()>end)throw new AssertionError("owner failed to drain");Thread.sleep(2);}}
 static int value(Object x,String n)throws Exception{return field(x.getClass(),n).getInt(x);}
 static Object failure(String key)throws Exception{Method env=GpuQualification1961.class.getDeclaredMethod("environment");env.setAccessible(true);Method f=GpuQualification1961.class.getDeclaredMethod("failure",String.class,String.class);f.setAccessible(true);return f.invoke(null,key,env.invoke(null));}
 static void runCase(int mode,int trial,int expected)throws Exception{
  cases++;SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;ProcessingTiming1947.photo++;
  QualityPipeline1932.mode=mode;QualityPipeline1932.failAt=trial;QualityPipeline1932.candidateRuns=QualityPipeline1932.comparisons=0;QualityPipeline1932.oracle=null;
  Bitmap source=new Bitmap(65+cases,24);QualityPixels1932.Plan p=new QualityPixels1932.Plan();
  check(GpuResident1976.finish(source,p,4,true,null,0,72,28,p,null)==null,"unproved route stays normal foreground path");
  Object job=((Deque<?>)field(GpuQualification1961.class,"QUEUED").get(null)).peekFirst();check(job!=null,"one resident owner queued");
  String key=(String)field(job.getClass(),"key").get(job);Object attempt=field(job.getClass(),"attempt1984").get(job);
  field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);SaveQueue1935.idle=true;GpuQualification1961.wake();
  waitFor(()->GpuQualification1961.retainedBytes()==0);SaveQueue1935.idle=false;
  check(value(attempt,"outcome")==expected,"mode "+mode+" trial "+trial+" expected "+expected+" got "+value(attempt,"outcome"));
  check(!QualityPipeline1932.open,"GPU owner closed before failure handling");
  check(value(attempt,"certificates")==0&&GpuQualification1961.restore(key)==null,"partial comparison never certifies");
  check(value(attempt,"exactFailures")==0&&!GpuQualification1961.exactRejected(key),"admission failure is not pixel mismatch");
  check(value(attempt,"speedFailures")==0,"uncompleted call is not a measured speed failure");
  check(value(attempt,"residentBaselineExact1999")==trial,"real comparator completed existing-route trials");
  check(value(attempt,"residentCandidateExact1999")==trial-1,"candidate count excludes failed comparison");
  check(value(attempt,"residentBaselineSpeed1999")==trial-1&&value(attempt,"residentCandidateSpeed1999")==trial-1,"speed counts exclude uncompleted round");
  Object retry=failure(key);
  if(expected==4)check(retry==null,"cancellation spends no retry");
  else check(retry!=null&&value(retry,"outcome1991")==expected&&!GpuQualification1961.maySchedule(key),"bounded operational cooldown matches outcome");
  for(Bitmap b:Bitmap.all)if(b!=source)check(b.isRecycled(),"each prior snapshot, oracle and destination is released");
  check(!source.isRecycled(),"caller source ownership retained");source.recycle();
 }
 public static void main(String[] args)throws Exception{
  GpuQualification1961.initialize(new Context());
  boolean baseline=args.length>0&&args[0].equals("baseline");
  if(baseline)runCase(10,2,15);
  else{
   int[] outcomes={6,8,8,8,7,8,7,8,8,8,8};
   for(int i=0;i<outcomes.length;i++)for(int t=1;t<=2;t++)runCase(10+i,t,outcomes[i]);
   runCase(100,2,15);runCase(101,2,15);runCase(102,2,4);runCase(103,2,4);runCase(104,2,15);
  }
  String proof=baseline?"published108_strong_generic_exception_reproduced19110":"resident_strong_typed_classification19110";
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"cases\":"+cases+",\""+proof+"\":true,\"resident_incomplete_gate_preserved19110\":true,\"actual_resident_queue_executed19110\":true,\"native_transport_mocked\":true,\"physical_android_tested\":false}");
 }
}
