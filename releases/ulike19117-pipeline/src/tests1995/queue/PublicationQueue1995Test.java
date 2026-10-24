package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Execute the real queue, publication clock and cancellation ownership. */
public final class PublicationQueue1995Test {
 static int checks;
 static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static Field field(String n)throws Exception{Field f=GpuQualification1961.class.getDeclaredField(n);f.setAccessible(true);return f;}
 static void await(CountDownLatch l,String why)throws Exception{check(l.await(3,TimeUnit.SECONDS),why);}
 public static void main(String[] args)throws Exception {
  QueueModel1994Test.reset();check(!GpuQualification1961.optionalBenchmarksAllowed1995(),"no optional benchmark before actual publication");
  GpuQualification1961.captureChanged();check(!GpuQualification1961.optionalBenchmarksAllowed1995(),"capture alone is not publication");
  long before=System.nanoTime();check(GpuQualification1961.publicationSucceeded1995(),"first accepted success opens gate");
  long first=field("lastCapture").getLong(null);check(first>=before&&first<=System.nanoTime(),"quiet clock anchored at publication");
  check(GpuQualification1961.optionalBenchmarksAllowed1995(),"publication gate remains visible");
  SaveQueue1935.idle=true;final AtomicInteger runs=new AtomicInteger();
  check(GpuQualification1961.schedule("publication95-quiet",512,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){runs.incrementAndGet();}public void close(){}}),"quiet probe queued");
  Thread.sleep(120);check(runs.get()==0,"two-second publication quiet period enforced");
  check(!GpuQualification1961.publicationSucceeded1995(),"later success reports not-first");
  check(field("lastCapture").getLong(null)>first,"later success resets quiet origin too");
  QueuePriority1993Test.drain();check(runs.get()==1,"probe starts when quiet elapsed");
  QueueModel1994Test.reset();check(GpuQualification1961.optionalBenchmarksAllowed1995(),"capture cancellation does not reset first-success gate");
  final CountDownLatch started=new CountDownLatch(1),closing=new CountDownLatch(1),finishClose=new CountDownLatch(1);final AtomicInteger closes=new AtomicInteger();
  check(GpuQualification1961.schedule("strong-standard-layout1994-v1",512,new GpuQualification1961.Probe(){
   public void run(GpuQualification1961.Cancellation c){started.countDown();while(!c.cancelled())try{Thread.sleep(20);}catch(InterruptedException expected){Thread.currentThread().interrupt();}}
   public void close(){closes.incrementAndGet();closing.countDown();boolean interrupted=false;for(;;)try{finishClose.await();break;}catch(InterruptedException expected){interrupted=true;}if(interrupted)Thread.currentThread().interrupt();}
  }),"active optional probe queued");
  SaveQueue1935.idle=true;field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();await(started,"actual worker starts");
  GpuQualification1961.CaptureProbe1995 snap=GpuQualification1961.captureProbe1995();
  check(snap!=null&&snap.id>0&&snap.kind.contains("強ノイズ")&&!snap.alreadyCancelled,"capture gets identified live optional probe");
  check(snap.phase!=null,"capture snapshot includes observed phase");
  SaveQueue1935.idle=false;check(!GpuQualification1961.captureProbe1995().alreadyCancelled,"save-busy predicate alone is not an explicit cancellation request");
  GpuQualification1961.captureChanged();await(closing,"cancellation reaches close");
  GpuQualification1961.CaptureProbe1995 cancelled=GpuQualification1961.captureProbe1995();
  check(cancelled!=null&&cancelled.id==snap.id&&cancelled.alreadyCancelled,"cancelled owner remains visible until resources closed");
  check(GpuQualification1961.attemptSummary1985().contains("取消要求→解放: 終了待ち"),"no false drained timestamp before close finishes");
  finishClose.countDown();QueuePriority1993Test.drain();check(closes.get()==1&&GpuQualification1961.captureProbe1995()==null,"owner closes exactly once and disappears");
  String summary=GpuQualification1961.attemptSummary1985();check(summary.contains("種別: 強ノイズ追加方式")&&summary.contains("取消要求→解放:")&&!summary.contains("取消要求→解放: 終了待ち"),"completed cancellation drain recorded");
  check(!snap.alreadyCancelled,"earlier scalar snapshot immutable");
  for(Field f:GpuQualification1961.CaptureProbe1995.class.getDeclaredFields())check(f.getType().isPrimitive()||f.getType()==String.class,"snapshot retains no job/image reference");
  for(String key:new String[]{"model-layout1994-optional-v1:1","model-layout1994-optional-v1:2","finish-wide1995-v1","strong-standard-stripe1998-v1"}){
   QueueModel1994Test.reset();check(GpuQualification1961.schedule(key,512,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){}public void close(){}}),"metadata family queued");QueuePriority1993Test.drain();
  }
  summary=GpuQualification1961.attemptSummary1985();check(summary.contains("モデル32スレッド")&&summary.contains("モデル128スレッド")&&summary.contains("補正128行の再検証"),"different synthetic proof kinds named");
  check(summary.contains("種別: 強ノイズ追加方式・2の累乗除算"),"stripe profile 6 proof named separately from profile 7");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"actual_qualification_queue\":true,\"physical_android_tested\":false}");
 }
}
