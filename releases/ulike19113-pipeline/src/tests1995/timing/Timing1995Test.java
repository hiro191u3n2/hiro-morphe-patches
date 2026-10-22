package com.hiro.ulike;
public final class Timing1995Test {
 static int assertions;
 static void check(boolean b,String reason){assertions++;if(!b)throw new AssertionError(reason);}
 static String render(ProcessingTiming1947.Trace t)throws Exception{return Timing1982Test.render(t);}
 static void publication()throws Exception{
  int before=GpuQualification1961.publications1995, strong=GpuStrongDefault1990.requests,model=GpuModelDefault1990.requests,wide=GpuFinishDefault1990.requests;
  ProcessingTiming1947.Trace t=ProcessingTiming1947.begin(new Object());GpuQualification1961.expectedUnlocked1995=t;
  ProcessingTiming1947.publication1990(t,false);
  check(GpuQualification1961.publications1995==before,"failed publication cannot release admission");
  GpuStrongDefault1990.fail=true;GpuModelDefault1990.fail=true;
  ProcessingTiming1947.publication1990(t,true);
  check(t.publicationSucceeded1990&&t.publishedNanos1990>=t.started,"optional failures preserve real publication evidence");
  check(GpuQualification1961.publications1995==before+1,"successful admission called outside trace monitor");
  check(GpuStrongDefault1990.requests==strong+1&&GpuModelDefault1990.requests==model+1&&GpuFinishDefault1990.requests==wide+1,"family exceptions isolated");
  ProcessingTiming1947.publication1990(t,true);ProcessingTiming1947.publication1990(t,false);
  check(GpuQualification1961.publications1995==before+1,"duplicate success inert");
  ProcessingTiming1947.finish(t,true);ProcessingTiming1947.publication1990(t,true);
  check(GpuQualification1961.publications1995==before+1,"terminal callback inert");
  GpuStrongDefault1990.fail=GpuModelDefault1990.fail=false;
  ProcessingTiming1947.Trace next=ProcessingTiming1947.begin(new Object());GpuQualification1961.expectedUnlocked1995=next;
  ProcessingTiming1947.publication1990(next,true);
  check(GpuQualification1961.publications1995==before+2,"every accepted publication resets queue quiet interval");
  check(GpuStrongDefault1990.requests==strong+2&&GpuModelDefault1990.requests==model+2&&GpuFinishDefault1990.requests==wide+2,"next publication rearms cancellation-capable families");
  GpuQualification1961.expectedUnlocked1995=null;
 }
 static void probeAndWide()throws Exception{
  GpuQualification1961.probe1995=new GpuQualification1961.CaptureProbe1995(41,"モデル128","画質比較",false);
  GpuStartup1990.current.finishWideStatus1995="startup-128-incomplete";
  ProcessingTiming1947.Trace t=ProcessingTiming1947.begin(new Object());String a=render(t);
  check(a.contains("#41 モデル128 / 画質比較 / 撮影開始の中断要求前に観測"),"capture snapshots exact active probe scalars");
  check(a.contains("撮影遅延との因果は未測定"),"observation cannot imply causation");
  GpuQualification1961.probe1995=null;GpuStartup1990.current.finishWideStatus1995="changed";GpuFinishDefault1990.current="current-wide-recovered";
  check(render(t).contains("startup-128-incomplete")&&!render(t).contains("current-wide-recovered"),"historical startup snapshot stays independent of current verification");
  check(ProcessingTiming1947.liveSummary1979().contains("表示時点の128行補正検証: current-wide-recovered"),"live state labelled separately");
  check(render(t).contains("#41 モデル128"),"probe completion cannot rewrite captured observation");
  ProcessingTiming1947.Trace empty=ProcessingTiming1947.begin(new Object());
  check(render(empty).contains("実行中なし（観測時点）"),"no probe statement limited to observed instant");
 }
 static void directAndFinish()throws Exception{
  ProcessingTiming1947.Trace t=ProcessingTiming1947.begin(new Object());
  check(!render(t).contains("強ノイズGPU直接読戻し:"),"absent direct observation stays absent");
  ProcessingTiming1947.strongReadback1995(t,2,1000);ProcessingTiming1947.strongReadback1995(t,3,2000);
  check(render(t).contains("強ノイズGPU直接読戻し: 5区間 / 3000画素"),"successful per-stage counts aggregate");
  check(render(t).contains("強ノイズ処理: 未観測"),"direct telemetry does not fabricate adoption counter");
  ProcessingTiming1947.strongReadback1995(t,0,1);ProcessingTiming1947.strongReadback1995(t,2,1);
  check(t.strongDirectStrips1995==5&&t.strongDirectPixels1995==3000,"invalid direct counts ignored");
  ProcessingTiming1947.finishSelection1995(t,1,128);ProcessingTiming1947.finishSelection1995(t,4,32);
  String text=render(t);check(text.contains("実出力検証済み標準128行（128行） → 128行のメモリ枠不足・32行へ（32行）"),"actual fallback distinguishes memory from certification");
  ProcessingTiming1947.finishSelection1995(t,0,32);ProcessingTiming1947.finishSelection1995(t,8,32);ProcessingTiming1947.finishSelection1995(t,1,129);
  check(t.finishSelectionCount1995==2,"invalid selection ignored");
  ProcessingTiming1947.finishSelection1995(t,7,0);check(render(t).contains("GPU補正実行失敗（全3判断の最初と最後）"),"bounded first and latest sequence preserves terminal failure");
  ProcessingTiming1947.finish(t,false);text=render(t);
  ProcessingTiming1947.strongReadback1995(t,1,50);ProcessingTiming1947.finishSelection1995(t,1,128);ProcessingTiming1947.captureMetadata1995(t,400,1000,1,2,2);
  check(render(t).equals(text),"completed diagnostic immutable");
  ProcessingTiming1947.Trace overflow=ProcessingTiming1947.begin(new Object());ProcessingTiming1947.strongReadback1995(overflow,1,Long.MAX_VALUE);ProcessingTiming1947.strongReadback1995(overflow,1,1);
  check(overflow.strongDirectPixels1995==Long.MAX_VALUE,"scalar aggregation saturates without wrap");
 }
 public static void main(String[] args)throws Exception{publication();probeAndWide();directAndFinish();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+"}");}
}
