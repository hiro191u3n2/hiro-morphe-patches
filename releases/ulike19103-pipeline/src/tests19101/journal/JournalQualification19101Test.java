package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual queue and actual journal, with controlled disk latency only. No
 * fixture supplies pixel/quality evidence or modifies production admission. */
public final class JournalQualification19101Test {
 static int checks;
 interface Check {boolean ok()throws Exception;}
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static void await(Check condition,String why)throws Exception{long end=System.nanoTime()+5000000000L;while(!condition.ok()){if(System.nanoTime()>end)throw new AssertionError("timeout: "+why);Thread.sleep(2);}check(true,why);}
 static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 static Object queueLock()throws Exception{return field(GpuQualification1961.class,"LOCK").get(null);}
 static VerificationJournal19101.Engine engine()throws Exception{return (VerificationJournal19101.Engine)field(VerificationJournal19101.class,"JOURNAL").get(null);}
 static List<VerificationJournal19101.Entry> disk()throws Exception{return VerificationJournal19101.decode(Context.journalText());}
 static VerificationJournal19101.Entry entry(long photo)throws Exception{for(VerificationJournal19101.Entry e:disk())if(e.process.equals(VerificationJournal19101.processId())&&e.v[1]==photo)return e;return null;}
 static void flush()throws Exception{VerificationJournal19101.Engine e=engine();await(()->{synchronized(e.lock){return e.loaded&&e.savedGeneration==e.generation;}},"latest diagnostic snapshot committed");}
 static void start()throws Exception{SaveQueue1935.idle=true;field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();}
 static void idle()throws Exception{await(()->GpuQualification1961.retainedBytes()==0,"queue ownership drained");SaveQueue1935.idle=false;}
 static class Probe implements GpuQualification1961.Probe {final AtomicInteger closes=new AtomicInteger();public void run(GpuQualification1961.Cancellation c){}public void close(){closes.incrementAndGet();}}
 static void boundedCall(Callable<Boolean> task,String why)throws Exception{ExecutorService e=Executors.newSingleThreadExecutor();try{check(e.submit(task).get(2,TimeUnit.SECONDS),why);}finally{e.shutdownNow();}}
 public static void main(String[] args)throws Exception{
  Context.qualificationLock=queueLock();String process=VerificationJournal19101.processId();
  check(process.equals(VerificationJournal19101.processId())&&Context.journalIO==0,"process ID stable without preference access");
  long[] old=new long[VerificationJournal19101.VALUES];old[0]=77;old[1]=900;old[3]=2;old[4]=2;old[6]=1;old[9]=7;old[10]=5;old[29]=old[30]=-1;
  Context.seedJournal(VerificationJournal19101.encode(Arrays.asList(new VerificationJournal19101.Entry("old-process","1.9.101",old,new String[]{"連結","検証開始","認定記録を作成","写真条件の検証","未開始"}))));
  Context.seedProof("unrelated-physical-record","not-a-valid-certificate");
  Map<String,String> originalProof=Context.proofCopy();
  GpuQualification1961.initialize(new Context());
  await(()->VerificationJournal19101.summary().contains("old-process"),"historical scalar success restored");
  check(((Map<?,?>)field(GpuQualification1961.class,"RECORDS").get(null)).isEmpty(),"journal certificates do not populate loaded proof cache");
  check(GpuQualification1961.restore("unrelated-physical-record")==null,"journal cannot grant a current GPU certificate");
  check(Context.proofCopy().equals(originalProof),"historical restore does not mutate proof preferences");
  int scans=Context.proofScans;String status=GpuQualification1961.status1967();
  check(status.contains("現在の起動で読込み済みの写真条件認定 0件")&&status.contains("保存済み認定の総数は未集計"),"current loaded count distinct from uncounted disk total");
  for(int i=0;i<20;i++)GpuQualification1961.status1967();
  check(Context.proofScans==scans,"status opens no full preference scan");

  // A commit deliberately stalls. Producers and queue cancellation must still
  // finish, with snapshots retained in memory until the daemon can commit.
  ProcessingTiming1947.epoch=21;ProcessingTiming1947.photo=1021;
  Context.blockCommit=true;Context.commitEntered=new CountDownLatch(1);Context.releaseCommit=new CountDownLatch(1);
  Probe waiting=new Probe();check(GpuQualification1961.schedule("journal-queued-a",10,waiting),"queued owner admitted");
  check(Context.commitEntered.await(3,TimeUnit.SECONDS),"journal daemon enters controlled slow commit");
  Probe waiting2=new Probe();boundedCall(()->GpuQualification1961.schedule("journal-queued-b",20,waiting2),"second foreground enqueue remains independent of disk");
  check(VerificationJournal19101.summary().contains("保存待ち"),"blocked commit reports pending rather than saved");
  boundedCall(()->{GpuQualification1961.captureChanged();return GpuQualification1961.retainedBytes()==0;},"capture cancellation remains independent of disk");
  check(waiting.closes.get()==1&&waiting2.closes.get()==1,"cancelled queued owners close once");
  check(GpuQualification1961.attemptSummary1985().contains("撮影 #1021 / プロセス "+process+" / v"+VerificationJournal19101.version()),"current attempts bind photo process and version");
  Context.blockCommit=false;Context.releaseCommit.countDown();flush();
  int cancelled=0;for(VerificationJournal19101.Entry e:disk())if(e.process.equals(process)&&e.v[1]==1021){check(e.v[3]==2&&e.v[6]==4,"observed cancellation persists as actual terminal result");cancelled++;}
  check(cancelled==2,"both queued attempt identities retained");

  // Stage entry is committed while the oracle is still running. A later
  // capture cancels it; ownership and terminal facts remain tied to its photo.
  ProcessingTiming1947.epoch=22;ProcessingTiming1947.photo=1022;
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger close=new AtomicInteger();
  check(GpuQualification1961.schedule("strong-resident19101:cancel",32,new GpuQualification1961.Probe(){
   public void run(GpuQualification1961.Cancellation c){GpuQualification1961.progress1984("resident_compare",0,2);GpuQualification1961.residentStage1999(4,1,false);entered.countDown();
    boolean done=false;while(!done)try{release.await();done=true;}catch(InterruptedException cancelled){}
   }
   public void close(){close.incrementAndGet();}
  }),"resident progress owner queued");start();check(entered.await(3,TimeUnit.SECONDS),"actual worker enters reference stage");
  await(()->{VerificationJournal19101.Entry e=entry(1022);return e!=null&&e.v[3]==1&&e.v[18]==4;},"running reference stage durable before completion");
  VerificationJournal19101.Entry running=entry(1022);
  check(running.v[19]==1&&running.v[20]==0&&running.v[21]==0&&running.v[22]==0&&running.v[26]==0,"entering a stage invents no completion/exact count");
  check(running.v[1]==1022&&running.process.equals(process)&&running.version.equals(VerificationJournal19101.version()),"running snapshot retains exact original owner identity");
  SaveQueue1935.idle=false;GpuQualification1961.captureChanged();ProcessingTiming1947.epoch=23;ProcessingTiming1947.photo=1023;
  check(close.get()==0&&GpuQualification1961.retainedBytes()==32,"running cancellation retains pixels until owner returns");
  release.countDown();idle();flush();VerificationJournal19101.Entry ended=entry(1022);
  check(ended.v[3]==2&&ended.v[6]==4&&ended.v[18]==4&&ended.v[19]==1&&ended.v[26]==0,"cancelled oracle preserves exact incomplete stage");
  check(ended.v[20]==0&&ended.v[21]==0&&ended.v[22]==0&&ended.v[23]==0&&ended.v[24]==0&&ended.v[9]==0,"cancel does not imply reference exact speed or qualification success");
  check(ended.v[17]>0&&ended.v[25]>0&&close.get()==1,"elapsed captured once and owner closes once");
  check(entry(1023)==null,"old attempt is never rebound to new photo");

  // Actual completed observer hooks persist all two-trial counters and speed
  // rejection. They remain diagnostics and cannot make any GPU route eligible.
  ProcessingTiming1947.epoch=24;ProcessingTiming1947.photo=1024;final String completedKey="strong-resident19101:complete";
  check(GpuQualification1961.schedule(completedKey,16,new GpuQualification1961.Probe(){
   public void run(GpuQualification1961.Cancellation c){for(int trial=1;trial<=2;trial++)for(int stage=4;stage<=8;stage++){GpuQualification1961.residentStage1999(stage,trial,false);GpuQualification1961.residentStage1999(stage,trial,true);}
    GpuQualification1961.progress1984("resident_speed",2,2);GpuQualification1961.corpusCertificate1994();GpuQualification1961.timings1984(5000000,31000000);
    GpuQualification1961.rejectMeasuredSpeed1992(completedKey,"連結候補",5000000,31000000);GpuQualification1961.outcome1984("speed_condition");}
   public void close(){}
  }),"completed resident observation queued");start();idle();flush();VerificationJournal19101.Entry completed=entry(1024);
  check(completed.v[3]==2&&completed.v[6]==11&&completed.v[7]==2&&completed.v[8]==2,"terminal speed failure and phase progress persisted");
  for(int i=20;i<=24;i++)check(completed.v[i]==2,"every reference exact and speed counter retained");
  check(completed.v[9]==0&&completed.v[10]==1&&completed.v[12]==1,"separate finite-corpus write does not imply photo admission");
  check(completed.v[34]==5000000&&completed.v[35]==31000000&&completed.v[36]==4750000,"observed speed comparison survives unchanged");
  check(GpuQualification1961.restore(completedKey)==null,"persisted lifecycle never grants rejected route admission");
  check(Context.proofCopy().equals(originalProof),"all diagnostic writes are isolated from proof preference contents");
  check(Context.badJournalThread==0&&Context.journalUnderQueueLock==0&&Context.journalWrites>0,"all journal storage executes on daemon outside qualification lock");
  check(!Context.journalText().contains("host-qualified-driver")&&!Context.journalText().contains(completedKey),"stored payload excludes raw device fingerprint and candidate key");
  for(VerificationJournal19101.Entry e:disk()){for(String label:e.labels)check(!label.contains("strong-resident19101")&&!label.contains("host-qualified-driver"),"restored scalar labels exclude keys and device fingerprint");}
  check(Context.journalText().length()<=VerificationJournal19101.MAX_TEXT&&disk().size()<=16,"production bridge remains within fixed history budget");
  check(!VerificationJournal19101.summary().contains("撮影 #1024"),"journal summary does not duplicate current process attempts");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"actual_qualification_journal_bridge\":true,\"lifecycle_photo_process_version_bound\":true,\"journal_io_off_queue_lock\":true,\"loaded_proof_count_scope_explicit\":true,\"diagnostic_history_cannot_grant_admission\":true}");
 }
}
