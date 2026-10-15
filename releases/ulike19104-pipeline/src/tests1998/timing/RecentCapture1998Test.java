package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Successful publication is the only latency boundary. Synthetic clocks keep
 * assertions deterministic; rendering never owns a camera object or a Trace. */
public final class RecentCapture1998Test {
 static int assertions;
 static final long MS=1000000L,START=1000000000L;
 static void check(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
 static Field field(String name)throws Exception{Field f=ProcessingTiming1947.class.getDeclaredField(name);f.setAccessible(true);return f;}
 static List<?> rows()throws Exception{return (List<?>)field("RECENT1998").get(null);}
 static void clear()throws Exception{Object lock=field("RECENT_LOCK1998").get(null);synchronized(lock){rows().clear();}}
 static String recent()throws Exception{Method m=ProcessingTiming1947.class.getDeclaredMethod("recentSummary1998");m.setAccessible(true);return (String)m.invoke(null);}
 static ProcessingTiming1947.Trace trace(long id,long millis){
  ProcessingTiming1947.Trace t=new ProcessingTiming1947.Trace(id,1000L,START);
  t.captureOrdinal1990=id;t.requestOrigin1991=2;t.events1991[0]=START;
  ProcessingTiming1947.settings(t,"noise4 / sharp4 / halo-on");ProcessingTiming1947.output(t,4284,5712,"画質補正");
  ProcessingTiming1947.captureMetadata1995(t,233,9991324L,2,4080,3060);
  ProcessingTiming1947.publication1990(t,true);
  t.publishedNanos1990=START+millis*MS;t.events1991[12]=t.publishedNanos1990;
  return t;
 }
 static void done(ProcessingTiming1947.Trace t){ProcessingTiming1947.finish(t,true);}
 static int count(String s,String part){int n=0,at=0;while((at=s.indexOf(part,at))>=0){n++;at+=part.length();}return n;}
 static void successOnly()throws Exception{
  clear();check(recent().equals(""),"empty process has no invented history");
  ProcessingTiming1947.Trace pending=trace(1001,3244);
  check(rows().isEmpty(),"published callback alone is not a terminal capture");
  ProcessingTiming1947.Trace failed=trace(1002,4000);ProcessingTiming1947.finish(failed,false);
  check(rows().isEmpty(),"failed final processing cannot enter successful history");
  ProcessingTiming1947.Trace missing=new ProcessingTiming1947.Trace(1003,1000,START);done(missing);
  check(rows().isEmpty(),"finish success without actual publication is excluded");
  ProcessingTiming1947.Trace publicFailure=new ProcessingTiming1947.Trace(1004,1000,START);
  ProcessingTiming1947.publication1990(publicFailure,false);done(publicFailure);
  check(rows().isEmpty(),"failed publication excluded");
  done(pending);String first=recent();check(rows().size()==1&&first.contains("全体 3244ms"),"successful exact boundary remembered");
  check(first.contains("全履歴（条件混在・参考）: 1枚 / 最小 3244ms・中央値 3244ms・最大 3244ms・幅 0ms"),"mixed reference statistics always displayed");
  check(first.contains("受信 未観測")&&first.contains("保存 未観測"),"missing stages are not zeros");
  done(pending);ProcessingTiming1947.finish(pending,false);ProcessingTiming1947.publication1990(pending,true);
  check(rows().size()==1&&first.equals(recent()),"duplicate and late callbacks do not change history");
  pending.publishedNanos1990+=2000*MS;pending.requested="mutated-test-owner";
  check(first.equals(recent()),"history owns scalar snapshot, not live Trace state");
  check(!ProcessingTiming1947.summary().contains("直近の保存時間比較"),"history never mutates persisted photo summary");
  String live=ProcessingTiming1947.liveSummary1979();
  check(count(live,"直近の保存時間比較")==1,"opened/copyable live view contains history exactly once");
  check(ProcessingTiming1947.liveSummary1979().equals(live),"reopening does not append duplicate history");
 }
 static void statisticsAndGroups()throws Exception{
  clear();ProcessingTiming1947.Trace a=trace(1101,2000),b=trace(1102,4000),c=trace(1103,3000),d=trace(1104,1000);
  ProcessingTiming1947.settings(c,"noise3 / sharp3 / halo-off");
  d.captureMetadataObserved1995=false;
  done(b);done(d);done(a);done(c);String text=recent();
  check(text.contains("4枚 / 最小 1000ms・中央値 2500ms・最大 4000ms・幅 3000ms"),"even median uses observed elapsed, includes mixed reference data");
  check(text.contains("記録条件一致 G1: 2枚 / 最小 2000ms・中央値 3000ms・最大 4000ms・幅 2000ms"),"only identical complete observed conditions grouped");
  check(text.contains("#1104（1104枚目） 00:00:01.000 条件未観測あり")&&text.contains("#1103（1103枚目） 00:00:01.000 一致相手なし"),"unknown and different settings visibly excluded");
  check(text.indexOf("#1101")<text.indexOf("#1102")&&text.indexOf("#1102")<text.indexOf("#1103"),"late completion kept in capture order");
  check(text.contains("未記録の処理条件の一致は未確認")&&text.contains("各列は加算できません"),"no scene causality or additive wall-time claim");
  clear();for(int i=0;i<3;i++)done(trace(1200+i,(i+1)*1000));
  check(recent().contains("3枚 / 最小 1000ms・中央値 2000ms・最大 3000ms"),"odd median correct");
  for(int change=0;change<12;change++){
   clear();a=trace(1300,2000);b=trace(1301,2100);
   switch(change){
    case 0:b.captureIso1995++;break;case 1:b.captureExposureNanos1995++;break;case 2:b.captureLens1995=1;break;
    case 3:b.captureWidth1995++;break;case 4:b.captureHeight1995++;break;case 5:b.width++;break;case 6:b.height++;break;
    case 7:b.format="別形式";break;case 8:b.requestOrigin1991=1;break;case 9:b.captureIso1995=0;break;
    case 10:char[] longSettings=new char[501];Arrays.fill(longSettings,'x');ProcessingTiming1947.settings(a,new String(longSettings));ProcessingTiming1947.settings(b,new String(longSettings));break;
    case 11:ProcessingTiming1947.output(a,10,10,"");ProcessingTiming1947.output(b,10,10,"");break;
   }
   done(a);done(b);check(recent().contains("比較可能な2枚以上の組なし"),"condition/unknown/truncation difference cannot create group "+change);
  }
 }
 static void actualRoutes()throws Exception{
  clear();ProcessingTiming1947.Trace t=trace(1400,3244);
  t.events1991[1]=START;t.events1991[2]=START+200*MS;t.events1991[3]=START+427*MS;
  t.events1991[5]=START+428*MS;t.events1991[6]=START+429*MS;
  t.events1991[7]=START+430*MS;t.events1991[8]=START+2998*MS;t.events1991[11]=t.publishedNanos1990-39*MS;
  ProcessingTiming1947.detailWork1976(t,0,825*MS);ProcessingTiming1947.detailWork1976(t,1,405*MS);ProcessingTiming1947.detailWork1976(t,2,868*MS);
  ProcessingTiming1947.recordInterval(t,ProcessingTiming1947.CORRECTION,START+2500*MS,START+2938*MS);
  ProcessingTiming1947.recordInterval(t,ProcessingTiming1947.ENCODE,START+2998*MS,START+3189*MS);
  ProcessingTiming1947.recordInterval(t,ProcessingTiming1947.SAVE,START+3181*MS,t.publishedNanos1990);
  ProcessingTiming1947.frontPlan1997(t,4,256,0,196*1024L*1024,196*1024L*1024,109*1024L*1024,12*1024L*1024,-1,4);
  ProcessingTiming1947.frontHalf1997(t,1,3121200);ProcessingTiming1947.detailRoute1976(t,3);ProcessingTiming1947.detailRows1994(t,128);
  ProcessingTiming1947.modelVariant1994(t,2);ProcessingTiming1947.noiseBackend(t,16,0);ProcessingTiming1947.strongProfile1994(t,4,0,16);
  t.captureProbeObserved1995=true;t.captureProbeId1995=11;t.captureProbeKind1995="写真条件の検証";t.captureProbePhase1995="検証開始";
  done(t);String s=recent();
  check(s.contains("受信 200ms / 描画 227ms / キュー 1ms / 画素 2568ms / 前段 825ms / モデル 405ms / Strong 868ms / 補正 438ms / 圧縮 191ms / 保存 63ms / 公開 39ms"),"actual nonadditive elapsed columns preserved");
  check(s.contains("前段 4並列/256行/空き枠4・half 引継ぎ成功・補正 GPU連結/128行・モデルvariant 2"),"actual worker half and correction/model routes visible");
  check(s.contains("Strong GPU/CPU 16/0・Strong profile.variant 4.0×16"),"actual Strong profile and variant retained");
  ProcessingTiming1947.Trace candidate=trace(1401,2300);ProcessingTiming1947.strongProfile1994(candidate,6,0,16);
  check(Timing1982Test.render(candidate).contains("追加認定方式・2の累乗除算 / profile 6 / variant 0 / 16区間"),"profile6 actual route is distinct from standard profile4");
  done(candidate);check(recent().contains("Strong profile.variant 6.0×16"),"history retains profile6 admission without re-labeling it as standard");
  check(s.contains("ISO 233 / 露光 9991324ns / 背面 / 入力 4080×3060 → 出力 4284×5712"),"per-row exact metadata visible");
  check(s.contains("開始時の背景検証 #11 写真条件の検証/検証開始")&&s.contains("因果は未測定"),"background observation is not delay diagnosis");
 }
 static void boundsAndLockOrder()throws Exception{
  clear();for(int i=0;i<20;i++)done(trace(2000+i,2000+i));String s=recent();
  check(rows().size()==16&&!s.contains("#2003")&&s.contains("#2004")&&s.contains("#2019"),"bounded ring preserves newest16 captured photos");
  done(trace(1999,4000));check(rows().size()==16&&!recent().contains("#1999"),"very late old completion cannot evict a newer capture");
  Class<?> entry=rows().get(0).getClass();
  for(Field f:entry.getDeclaredFields())check(f.getType().isPrimitive()||f.getType()==String.class||f.getType()==long[].class,"history retains only scalars/strings: "+f.getName());
  final Object lock=field("RECENT_LOCK1998").get(null);final ProcessingTiming1947.Trace t=trace(2100,2500);
  final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();Thread finishing=new Thread(new Runnable(){public void run(){try{done(t);}catch(Throwable e){failure.set(e);}}});
  synchronized(lock){
   finishing.start();long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
   while(finishing.getState()!=Thread.State.BLOCKED&&System.nanoTime()<until)Thread.yield();
   check(finishing.getState()==Thread.State.BLOCKED,"finish reaches held history lock");
   final CountDownLatch acquired=new CountDownLatch(1);
   Thread inspect=new Thread(new Runnable(){public void run(){synchronized(t){acquired.countDown();}}});inspect.start();
   check(acquired.await(5,TimeUnit.SECONDS),"history insertion never retains completed trace monitor");
  }
  finishing.join(5000);check(!finishing.isAlive()&&failure.get()==null,"finish resumes after independent history lock release");
  clear();final CountDownLatch start=new CountDownLatch(1);Thread[] threads=new Thread[8];
  for(int i=0;i<threads.length;i++){final int n=i;threads[i]=new Thread(new Runnable(){public void run(){try{start.await();done(trace(2200+n,2100+n));for(int j=0;j<20;j++)recent();}catch(Throwable e){failure.compareAndSet(null,e);}}});threads[i].start();}
  start.countDown();for(Thread thread:threads){thread.join(5000);check(!thread.isAlive(),"concurrent finish/live snapshot completes");}
  check(failure.get()==null&&rows().size()==8,"concurrent success creates each capture once");
 }
 public static void main(String[] args)throws Exception{
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"));successOnly();statisticsAndGroups();actualRoutes();boundsAndLockOrder();
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"recent_history_bounded16_verified\":true,\"recent_history_success_only_verified\":true,\"recent_history_exact_groups_verified\":true,\"recent_history_lock_order_verified\":true}");
 }
}
