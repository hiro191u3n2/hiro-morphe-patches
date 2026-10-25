package com.hiro.ulike;
import java.util.Arrays;
public final class Timing1992Test {
 static int assertions;
 static final long MS=1000000L;
 static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
 static String render(ProcessingTiming1947.Trace trace)throws Exception{return Timing1982Test.render(trace);}
 static void event(ProcessingTiming1947.Trace t,int kind,long ms){ProcessingTiming1947.markAt1991(t,kind,ms*MS);}
 static void imageWait()throws Exception{
  ProcessingTiming1947.Trace t=new ProcessingTiming1947.Trace(1,1,MS);
  event(t,ProcessingTiming1947.IMAGE_RECEIVED1991,208);
  event(t,ProcessingTiming1947.BITMAP_DELIVERED1991,549);
  event(t,ProcessingTiming1947.PREP_START1991,234);
  event(t,ProcessingTiming1947.IMAGE_GET_START1992,239);
  event(t,ProcessingTiming1947.IMAGE_GET_END1992,551);
  event(t,ProcessingTiming1947.QUEUE_SUBMIT1991,578);
  String s=render(t);
  check(s.contains("保存準備ワーカー起動→画像取得呼出し: 5 ms"),"getter setup isolated");
  check(s.contains("画像取得呼出し→取得完了（画像完成待ちを含む）: 312 ms"),"real getter wait belongs to getter, not CPU preparation");
  check(s.contains("画像取得後の保存準備→キュー投入開始: 27 ms"),"post-acquisition preparation excludes blocking image wait");
  check(s.contains("描画済み画像受取→保存用画像取得完了: 2 ms"),"callback to acquired source handoff");
  check(s.contains("画像受信→描画完了の重複: 310 ms（同じ実時間のため再加算不可）"),"overlap is exact intersection, never extra work");
  check(s.contains("315 ms 前に起動（画像完成前の起動は正常です）"),"expected producer-consumer order explained");
  check(!s.contains("描画済み画像受取→保存準備開始"),"reversed misleading sequential phase removed");
  ProcessingTiming1947.Trace unrelated=new ProcessingTiming1947.Trace(2,1,MS);
  String absent=render(unrelated);
  check(absent.contains("画像取得後の保存準備→キュー投入開始: 未計測")&&!absent.contains("画像受信→描画完了の重複:"),"unobserved peer capture stays unknown");
  event(t,ProcessingTiming1947.IMAGE_GET_END1992,999);
  check(t.events1991[ProcessingTiming1947.IMAGE_GET_END1992]==551*MS,"late duplicate preserves exact first acquisition");
  ProcessingTiming1947.Trace late=new ProcessingTiming1947.Trace(3,1,600*MS);
  event(late,ProcessingTiming1947.IMAGE_GET_START1992,239);event(late,ProcessingTiming1947.IMAGE_GET_END1992,551);
  check(late.events1991[13]==0&&late.events1991[14]==0,"fallback trace cannot inherit unowned earlier event");
 }
 static void correctionWork()throws Exception{
  final ProcessingTiming1947.Trace t=new ProcessingTiming1947.Trace(4,1,MS);
  int calls=GpuQualification1961.statusCalls;
  Thread[] workers=new Thread[4];
  for(int i=0;i<workers.length;i++){
   workers[i]=new Thread(new Runnable(){public void run(){for(int j=0;j<50;j++)ProcessingTiming1947.finishWork1992(t,ProcessingTiming1947.FINISH_CPU_POLICY1992,MS,64);}});
   workers[i].start();
  }
  for(Thread worker:workers)worker.join();
  check(t.finishWork1992[0]==200&&t.finishWork1992[1]==200*MS&&t.finishWork1992[2]==12800,"concurrent work accumulates on exact owner");
  check(GpuQualification1961.statusCalls==calls,"recording never queries GPU qualification");
  ProcessingTiming1947.finishWork1992(t,ProcessingTiming1947.FINISH_CPU_MASK_PACK1992,0,0);
  ProcessingTiming1947.finishWork1992(t,ProcessingTiming1947.FINISH_READBACK1992,-1,-1);
  ProcessingTiming1947.finishWork1992(t,ProcessingTiming1947.FINISH_TRANSFER_SUBMIT1992,12*MS,1024*1024L);
  ProcessingTiming1947.finishWork1992(t,ProcessingTiming1947.FINISH_READBACK1992,3*MS,4096);
  String s=render(t);
  check(s.contains("CPU補正ポリシー生成: 200回 / 200 ms"),"full CPU policy distinguished even on GPU route");
  check(s.contains("CPU顔・肌マスク記述子作成: 1回 / 0 ms / 論理量 0.0 MiB"),"packed descriptor distinct from full policy; measured zero preserved");
  check(s.contains("GPU結果取得API（待機・読戻し）: 2回 / 3 ms（一部未測定）")&&s.contains("（一部未計測）"),"unknown work never becomes exact zero or complete quantity");
  check(s.contains("補助データ転送・GPU投入一体API: 1回 / 12 ms / 論理量 1.0 MiB"),"fused API has one measured boundary");
  check(s.contains("GPUカーネル単独の時間ではありません")&&s.contains("並行・内包する区間は加算できません")&&s.contains("実転送量・現在のメモリ使用量ではありません"),"API elapsed and logical data limitations explicit");
  long[] before=t.finishWork1992.clone();
  ProcessingTiming1947.finishWork1992(null,0,1,1);ProcessingTiming1947.finishWork1992(t,10,1,1);
  ProcessingTiming1947.finishWork1992(t,0,-2,1);ProcessingTiming1947.finishWork1992(t,0,1,-2);
  check(Arrays.equals(before,t.finishWork1992),"invalid observations cannot corrupt aggregate");
  ProcessingTiming1947.finishWork1992(t,0,Long.MAX_VALUE,Long.MAX_VALUE);
  check(t.finishWork1992[1]==Long.MAX_VALUE&&t.finishWork1992[2]==Long.MAX_VALUE,"overflow saturates rather than displaying negative work");
  ProcessingTiming1947.finish(t,false);before=t.finishWork1992.clone();
  ProcessingTiming1947.finishWork1992(t,0,1,1);
  check(Arrays.equals(before,t.finishWork1992),"terminal failed photo immutable");
 }
 static void versionStorage()throws Exception{
  check(ProcessingTiming1947.VERSION.equals("1.9.92"),"release runtime version exact target, never dynamic acceptance");
  Timing1982Test.MemoryPreferences p=new Timing1982Test.MemoryPreferences();
  p.values.put("id",Long.valueOf(100));p.values.put("version","1.9.92");p.values.put("summary","ULike v1.9.90 / old mislabeled result");
  Timing1982Test.reset();ProcessingTiming1947.init(new Timing1982Test.MemoryContext(p));
  check(ProcessingTiming1947.summary().startsWith("ULike v1.9.92 / 新しい撮影の計測待ち"),"old header rejected even if preference version matches");
  Timing1982Test.reset();p.values.put("version","1.9.90");p.values.put("summary","ULike v1.9.92 / wrong preference version");
  ProcessingTiming1947.init(new Timing1982Test.MemoryContext(p));
  check(ProcessingTiming1947.summary().startsWith("ULike v1.9.92 / 新しい撮影の計測待ち"),"old preference version rejected even if header matches");
  Timing1982Test.reset();p.values.put("version","1.9.92");String expected="ULike v1.9.92 / saved current result";p.values.put("summary",expected);
  ProcessingTiming1947.init(new Timing1982Test.MemoryContext(p));
  check(ProcessingTiming1947.summary().equals(expected),"matching persisted summary still restores unchanged");
 }
 public static void main(String[] args)throws Exception{
  imageWait();correctionWork();versionStorage();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"image_wait_and_correction_categories_verified\":true,\"release_version_restore_verified\":true}");
 }
}
