package com.hiro.ulike;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Production queue lifecycle: a 0/2 compare now identifies CPU, baseline,
 * and resident work; cancellation freezes partial facts without certification. */
public final class ResidentProgress1999Test {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static Field field(String name)throws Exception{Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f;}
 static void start(){SaveQueue1935.idle=true;try{field("lastCapture").setLong(null,System.nanoTime()-3000000000L);}catch(Exception e){throw new AssertionError(e);}GpuQualification1961.wake();}
 public static void main(String[] args)throws Exception {
  QueuePriority1993Test.reset();
  final CountDownLatch entered=new CountDownLatch(1),advance=new CountDownLatch(1),comparing=new CountDownLatch(1);
  final AtomicInteger closed=new AtomicInteger();
  check(GpuQualification1961.schedule("strong-resident1999:3:cancel",512,new GpuQualification1961.Probe(){
   public void run(GpuQualification1961.Cancellation c){
    GpuQualification1961.progress1984("resident_compare",0,2);
    GpuQualification1961.residentStage1999(4,1,false);entered.countDown();
    try{advance.await();}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
    GpuQualification1961.residentStage1999(4,1,true);
    GpuQualification1961.residentStage1999(5,1,false);
    GpuQualification1961.residentStage1999(6,1,true); // wrong active stage cannot invent a completed compare
    GpuQualification1961.residentStage1999(5,1,true);
    GpuQualification1961.residentStage1999(5,1,true); // duplicate is idempotent
    GpuQualification1961.residentStage1999(6,1,false);comparing.countDown();
    while(!c.cancelled())try{Thread.sleep(10);}catch(InterruptedException e){Thread.currentThread().interrupt();}
   }
   public void close(){closed.incrementAndGet();}
  }),"resident queued with actual retained owner");start();
  check(entered.await(3,TimeUnit.SECONDS),"worker starts CPU oracle stage");
  String s=GpuQualification1961.attemptSummary1985();
  check(s.contains("連結の画質比較 0/2")&&s.contains("CPU全画素基準の作成 1/2回目・実行中"),"0/2 explains live CPU reference work");
  check(s.contains("全画素一致: 既存 0/2・連結 0/2"),"reference entry is not full-pixel success");
  advance.countDown();check(comparing.await(3,TimeUnit.SECONDS),"baseline exact then resident comparison entered");
  s=GpuQualification1961.attemptSummary1985();check(s.contains("全画素一致: 既存 1/2・連結 0/2"),"only actual reported exact baseline counted once");
  check(s.contains("連結候補の全画素比較 1/2回目・実行中"),"candidate phase separately visible");
  SaveQueue1935.idle=false;GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;QueuePriority1993Test.drain();
  s=GpuQualification1961.attemptSummary1985();
  check(s.contains("撮影変更・中断で取消")&&s.contains("連結候補の全画素比較 1/2回目・未完了で終了"),"cancel retains exact interrupted stage");
  check(s.contains("写真条件の認定書込 0回")&&s.contains("速度計測完了: 既存 0/2・連結 0/2"),"no proof or unmeasured speed invented");
  Thread.sleep(15);check(s.equals(GpuQualification1961.attemptSummary1985()),"finished elapsed times frozen");
  check(closed.get()==1&&GpuQualification1961.retainedBytes()==0,"cancelled resident owner closes exactly once");
  QueuePriority1993Test.reset();
  check(GpuQualification1961.schedule("strong-resident1999:3:completed",512,new GpuQualification1961.Probe(){
   public void run(GpuQualification1961.Cancellation c){for(int trial=1;trial<=2;trial++)for(int stage=4;stage<=8;stage++){
    GpuQualification1961.residentStage1999(stage,trial,false);GpuQualification1961.residentStage1999(stage,trial,true);}
    GpuQualification1961.outcome1984("speed_condition");}
   public void close(){}
  }),"completed measured trial fixture queued");QueuePriority1993Test.drain();
  s=GpuQualification1961.attemptSummary1985();check(s.contains("全画素一致: 既存 2/2・連結 2/2")&&s.contains("速度計測完了: 既存 2/2・連結 2/2"),"both complete trials independently visible");
  check(s.contains("速度条件を満たさず")&&s.contains("写真条件の認定書込 0回"),"completed timing is not speed admission");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"actual_queue_lifecycle\":true,\"no_device_speed_claim\":true}");
 }
}
