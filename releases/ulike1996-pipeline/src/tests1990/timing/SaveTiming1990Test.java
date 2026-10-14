package com.hiro.ulike;
import java.lang.reflect.Method;
public final class SaveTiming1990Test {
 static int assertions;
 static void check(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
 static String render(ProcessingTiming1947.Trace t)throws Exception{Method m=ProcessingTiming1947.class.getDeclaredMethod("render",ProcessingTiming1947.Trace.class);m.setAccessible(true);return(String)m.invoke(null,t);}
 static void union(){
  ProcessingTiming1947.Trace t=new ProcessingTiming1947.Trace(1,1,100);
  ProcessingTiming1947.recordInterval(t,0,10,110);
  ProcessingTiming1947.recordInterval(t,1,105,140);
  ProcessingTiming1947.recordInterval(t,2,120,130);
  ProcessingTiming1947.recordInterval(t,3,180,240);
  ProcessingTiming1947.recordInterval(t,4,130,150);
  check(ProcessingTiming1947.coverageNanos1990(t,200)==70,"cross-stage overlap and both clip boundaries");
  check(ProcessingTiming1947.coverageNanos1990(t,145)==45,"finish clips interval union");
  check(ProcessingTiming1947.coverageNanos1990(t,100)==0,"empty trace window");
  check(ProcessingTiming1947.coverageNanos1990(null,200)==0,"unknown trace");
 }
 static void publication()throws Exception{
  GpuStartup1990.current=new GpuStartup1990.Snapshot();
  GpuStartup1990.current.state=2;GpuStartup1990.current.domainReady=true;
  GpuStartup1990.current.startedNanos=System.nanoTime()-2000000L;
  GpuStartup1990.current.readyNanos=System.nanoTime()-1000000L;
  ProcessingTiming1947.Trace t=ProcessingTiming1947.associate(new Object(),99001);
  check(t.captureOrdinal1990==1,"first captured photograph numbered once");
  check(t.startupReady1990,"readiness records completed real-output validation before trace");
  check(render(t).contains("完了未観測"),"processing alone cannot invent publication");
  ProcessingTiming1947.startupWait1990(t,12000000L,true);
  check(render(t).contains("撮影処理が待ったGPU準備: 12 ms / 準備完了"),"actual foreground wait independent of initial snapshot");
  ProcessingTiming1947.startupWait1990(t,-1,false);
  check(t.startupWaitNanos1990==12000000L&&t.startupWaitReady1990,"invalid measurement cannot rewrite completed wait");
  ProcessingTiming1947.publication1990(t,false);
  check(t.publicationObserved1990&&!t.publicationSucceeded1990&&t.publishedNanos1990==0,"failed publication has no success timestamp");
  check(render(t).contains("公開失敗"),"publication failure visible");
  ProcessingTiming1947.publication1990(t,true);
  check(t.publishedNanos1990>=t.started,"actual publication monotonic timestamp");
  check(render(t).contains("GPU初期化開始→一枚目の保存公開"),"first save includes initialization wait measure");
  ProcessingTiming1947.finish(t,true);long saved=t.publishedNanos1990;String snapshot=render(t);
  GpuStartup1990.current.domainReady=false;GpuStartup1990.current.state=4;
  ProcessingTiming1947.startupWait1990(t,999999999L,false);
  ProcessingTiming1947.publication1990(t,false);
  check(t.publishedNanos1990==saved&&t.publicationSucceeded1990,"late callback cannot rewrite completed photo");
  check(render(t).equals(snapshot),"later readiness cannot rewrite saved timing");
  check(snapshot.contains("工程別計測に含まれない経過"),"uncovered wall time reported explicitly");
  ProcessingTiming1947.Trace next=ProcessingTiming1947.associate(new Object(),99002);
  check(next.captureOrdinal1990==2&&!next.startupReady1990,"second photo has distinct ordinal and readiness");
  check(!render(next).contains("GPU初期化開始→一枚目の保存公開"),"first-shot initialization metric not assigned to later shots");
  ProcessingTiming1947.residentExit1982(next,0,16);
  check(render(next).contains("標準GPU経路を優先・追加検証を省略"),"standard route suppresses candidate copy with distinct reason");
  ProcessingTiming1947.residentExit1982(next,10,17);
  check(next.residentReason1978==16&&next.residentRetainedBytes1978==0,"new upper bound remains invalid");
  ProcessingTiming1947.publication1990(null,true);
 }
 public static void main(String[] args)throws Exception{union();publication();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"real_publication_and_first_shot_verified\":true}");}
}
