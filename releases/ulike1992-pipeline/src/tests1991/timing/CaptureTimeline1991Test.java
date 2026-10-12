package com.hiro.ulike;
import java.lang.reflect.Method;
public final class CaptureTimeline1991Test {
 static int assertions;
 static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
 static String render(ProcessingTiming1947.Trace t)throws Exception{
  Method m=ProcessingTiming1947.class.getDeclaredMethod("render",ProcessingTiming1947.Trace.class);
  m.setAccessible(true);return(String)m.invoke(null,t);
 }
 static void events()throws Exception{
  final long ms=1000000L;
  ProcessingTiming1947.Trace t=new ProcessingTiming1947.Trace(1,1,ms);
  t.requestOrigin1991=1;t.events1991[0]=ms;
  for(int e=1;e<12;e++)ProcessingTiming1947.markAt1991(t,e,(e+1)*10*ms);
  String text=render(t);
  check(text.contains("シャッターボタン受付→保存公開: 未計測"),"SDK request must not masquerade as UI input");
  check(text.contains("撮影要求→ネイティブ要求: 19 ms"),"exact monotonic request span");
  check(text.contains("キュー投入開始→保存ワーカー実行: 10 ms"),"explicit queue wait");
  check(text.contains("公開開始→公開成功: 未計測"),"no success invented by publication start");
  ProcessingTiming1947.markAt1991(t,12,130*ms);
  check(t.events1991[12]==0,"only actual publication may supply completion");
  ProcessingTiming1947.markAt1991(t,1,400*ms);
  check(t.events1991[1]==20*ms,"duplicate callbacks cannot rewrite original event");
  ProcessingTiming1947.Trace missing=new ProcessingTiming1947.Trace(2,1,100*ms);
  ProcessingTiming1947.markAt1991(missing,1,99*ms);
  check(missing.events1991[1]==0,"pre-owner event rejected");
  ProcessingTiming1947.markAt1991(missing,9,150*ms);
  ProcessingTiming1947.markAt1991(missing,8,200*ms);
  text=render(missing);
  check(text.contains("保存画素処理終了→圧縮開始: 並行・順序外"),"overlap is explicit, never negative or zeroed duration");
  check(text.contains("撮影要求受付→保存公開: 未計測（撮影要求の記録なし）"),"late standalone save cannot invent capture latency");
  check(text.contains("対応画像受信→描画済み画像受取: 未計測"),"absent input observation does not use unrelated latest event");
 }
 static void ownership()throws Exception{
  final long now=System.nanoTime();
  final ProcessingTiming1947.Trace one=ProcessingTiming1947.beginCapture1991(new Object(),now-1000000L,1);
  final ProcessingTiming1947.Trace two=ProcessingTiming1947.beginCapture1991(new Object(),now,2);
  check(one.started==now-1000000L&&one.events1991[0]==one.started,"observer overhead included from actual request entry");
  check(two.requestOrigin1991==2,"native fallback explicitly identified");
  final ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(two);
  Thread thread=new Thread(new Runnable(){public void run(){ProcessingTiming1947.mark1991(one,3);}});
  thread.start();thread.join();ProcessingTiming1947.restore(scope);
  check(one.events1991[3]>0&&two.events1991[3]==0,"cross-thread callback respects explicit owner");
  ProcessingTiming1947.publication1990(one,false);
  check(one.events1991[12]==0&&!one.publicationSucceeded1990,"failed publish cannot count as saved");
  ProcessingTiming1947.publication1990(one,true);long success=one.events1991[12];
  ProcessingTiming1947.publication1990(one,false);
  check(success==one.events1991[12]&&one.publicationSucceeded1990,"late failure cannot erase observed success");
  ProcessingTiming1947.finish(one,true);String before=render(one);
  ProcessingTiming1947.mark1991(one,4);
  ProcessingTiming1947.publication1990(one,true);
  check(before.equals(render(one)),"terminal report immutable");
  check(render(two).contains("前段未観測")==false,"incomplete native fallback is not complete");
  ProcessingTiming1947.publication1990(two,true);
  check(render(two).contains("ネイティブ撮影要求から・前段未観測"),"successful native fallback retains metric origin");
 }
 public static void main(String[] args)throws Exception{
  events();ownership();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"request_origin_and_owner_verified\":true}");
 }
}
