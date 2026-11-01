package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Real queue, failure store and renderer; GPU/save readiness are declared peers. */
public final class TypedFailure1992Test {
 static int assertions;static final long M=1000000L;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 static Field qfield(String name)throws Exception{return field(GpuQualification1961.class,name);}
 static Object failure(String key)throws Exception{Method m=GpuQualification1961.class.getDeclaredMethod("failure",String.class,String.class);m.setAccessible(true);return m.invoke(null,key,GpuQualification1961.environment1990());}
 static int outcome(String key)throws Exception{Object f=failure(key);return f==null?0:field(f.getClass(),"outcome1991").getInt(f);}
 static void clean()throws Exception{
  SaveQueue1935.idle=false;GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
  long end=System.nanoTime()+3000000000L;while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>end)throw new AssertionError("owner drain");Thread.sleep(2);}
  synchronized(qfield("LOCK").get(null)){((Map<?,?>)qfield("FAILURES").get(null)).clear();((Map<?,?>)qfield("RECORDS").get(null)).clear();}
  GpuQualification1961.initialize(new Context());
 }
 static void age()throws Exception{SaveQueue1935.idle=true;qfield("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();}
 static void waitDone()throws Exception{long end=System.nanoTime()+3000000000L;while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>end)throw new AssertionError("queue drain");Thread.sleep(2);}}
 static void classifications()throws Exception{
  clean();String[] names={"memory_budget","unsupported","execution_unavailable","exception","comparison_incomplete","baseline_unavailable","reference_unstable"};int[] ids={6,7,8,15,14,10,9};
  for(int i=0;i<names.length;i++){
   String key="typed-"+i;GpuQualification1961.rejectNonSpeed1988(key,names[i]);
   check(outcome(key)==ids[i],"real typed failure retained: "+names[i]);check(!GpuQualification1961.exactRejected(key),"nonquality failure not promoted to exact");
   check(!GpuQualification1961.maySchedule(key),"unchanged cooldown for "+names[i]);
  }
  GpuQualification1961.rejectMeasuredSpeed1992("speed","model maps",100*M,96*M);
  Object f=failure("speed");check(outcome("speed")==11,"actual slow comparison is speed");
  check(field(f.getClass(),"measuredCpu1992").getLong(f)==100*M&&field(f.getClass(),"measuredGpu1992").getLong(f)==96*M&&field(f.getClass(),"measuredLimit1992").getLong(f)==95*M,"stored durations/threshold are exact actual inputs");
  check("model maps".equals(field(f.getClass(),"measuredCandidate1992").get(f)),"candidate name retained");
  GpuQualification1961.rejectMeasuredSpeed1992("won","fast",100*M,95*M);check(failure("won")==null,"5-percent boundary accepted without failure");
  GpuQualification1961.rejectMeasuredSpeed1992("zero","missing",0,96*M);check(outcome("zero")==14,"unmeasured duration never classed as speed");
  GpuQualification1961.rejectSpeed("legacy");check(outcome("legacy")==11,"legacy rejection policy retained");
  check(field(failure("legacy").getClass(),"measuredCpu1992").getLong(failure("legacy"))==0,"legacy call cannot manufacture measured timing");
  String status=GpuQualification1961.status1967();check(status.contains("実測速度未達1件")&&status.contains("時間・詳細未記録1件"),"legacy and real measured failures displayed separately");
  check(status.contains("GPU未対応1件")&&status.contains("例外1件")&&status.contains("実行・転送失敗1件"),"unavailable errors have independent counts");
  check(status.contains("起動時の標準GPU検証とは別"),"zero photo proof cache never claims no startup certification");
  GpuQualification1961.rejectMeasuredThreshold1992("oldgpu","finish","既存GPU",100*M,99*M,95*M);check(outcome("oldgpu")==11,"actual old-GPU baseline supported");
  check("既存GPU".equals(field(failure("oldgpu").getClass(),"measuredReference1992").get(failure("oldgpu"))),"reference identity is explicit");
  GpuQualification1961.rejectExact("exact");GpuQualification1961.rejectNonSpeed1988("exact","memory_budget");check(GpuQualification1961.exactRejected("exact"),"typed failure cannot erase quality rejection");
  GpuQualification1961.qualified("exact",100*M,1,0);check(GpuQualification1961.restore("exact")==null,"later candidate cannot bypass exact rejection");
  Thread.currentThread().interrupt();try{GpuQualification1961.rejectMeasuredSpeed1992("cancelled","cancelled",100*M,100*M);GpuQualification1961.rejectNonSpeed1988("cancelled","exception");}finally{Thread.interrupted();}
  check(failure("cancelled")==null,"interrupted candidates create no failure and spend no retry");
 }
 static void state()throws Exception{
  clean();final CountDownLatch running=new CountDownLatch(1),release=new CountDownLatch(1);final AtomicInteger closed=new AtomicInteger();
  GpuQualification1961.Probe first=new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){running.countDown();try{release.await();}catch(InterruptedException x){Thread.currentThread().interrupt();}GpuQualification1961.rejectMeasuredSpeed1992("state-active","actual-map-candidate",100*M,101*M);}public void close(){closed.incrementAndGet();}};
  check(GpuQualification1961.schedule("state-active",1,first),"active job queued");
  check(GpuQualification1961.schedule("state-wait",1,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){throw new IllegalStateException("controlled");}public void close(){closed.incrementAndGet();}}),"waiting job queued");
  String before=GpuQualification1961.attemptSummary1985();check(before.contains("実行待ち")&&!before.contains("検証中"),"queued attempts cannot be called running");
  age();check(running.await(2,TimeUnit.SECONDS),"first actual owner entered");
  String during=GpuQualification1961.attemptSummary1985();check(during.contains("実行中")&&during.contains("実行待ち"),"running owner distinguished from queued job");
  check(!during.contains("終了時点の記録・最大16件"),"mixed live history has no finished-only heading");
  release.countDown();waitDone();check(closed.get()==2,"each queued owner closed exactly once");
  String after=GpuQualification1961.attemptSummary1985();check(after.contains("完了 / 検証中の例外で終了"),"real Runtime exception remains exception outcome");
  check(after.contains("actual-map-candidate")&&after.contains("100ms")&&after.contains("101ms")&&after.contains("95ms"),"history includes actual timings and threshold");
  check(!after.contains("実行中")&&!after.contains("実行待ち"),"finished jobs no longer labeled active");
  clean();final AtomicInteger cancels=new AtomicInteger();
  check(GpuQualification1961.schedule("cancel-run",1,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){throw new CancellationException("controlled");}public void close(){cancels.incrementAndGet();}}),"cancellation queued");age();waitDone();
  check(cancels.get()==1&&failure("cancel-run")==null,"cancel never charged as retry failure");
  check(GpuQualification1961.attemptSummary1985().contains("撮影変更・中断で取消"),"cancellation explicit terminal state");
 }
 public static void main(String[] args)throws Exception{classifications();state();System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_qualification_queue\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");}
}
