package com.hiro.ulike;
import java.util.Arrays;
public final class Timing1994Test {
 static int assertions;static final long MS=1000000L;
 static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
 static String render(ProcessingTiming1947.Trace t)throws Exception{return Timing1982Test.render(t);}
 static void actualRows()throws Exception{
  ProcessingTiming1947.Trace t=ProcessingTiming1947.begin(new Object());
  ProcessingTiming1947.detailRoute1976(t,1);
  check(render(t).contains("GPU連結（実行行数は未観測）"),"legacy route id cannot invent actual rows");
  ProcessingTiming1947.detailRows1994(t,128);ProcessingTiming1947.detailRoute1976(t,1);
  check(render(t).contains("GPU連結（128行・実行記録）"),"actual 128 survives legacy route1 category");
  check(!render(t).contains("GPU連結（32行）"),"stale32 label eliminated");
  ProcessingTiming1947.detailRows1994(t,32);
  check(render(t).contains("GPU連結（32行・実行記録）"),"successful32 fallback can replace completed candidate rows");
  ProcessingTiming1947.detailRows1994(t,0);ProcessingTiming1947.detailRows1994(t,129);
  check(t.correctionRows1994==32,"invalid rows rejected");
  ProcessingTiming1947.geometryBand1994(t,128,10*MS);ProcessingTiming1947.geometryBand1994(t,32,2*MS);
  check(render(t).contains("回転・リサイズGPU API: 2回 / 区間 32～128行 / 呼出し累計 12 ms"),"adaptive geometry rows have independent real min/max and band count");
  ProcessingTiming1947.geometryBand1994(t,0,1);ProcessingTiming1947.geometryBand1994(t,128,-1);
  check(t.geometryBands1994==2,"invalid geometry observations rejected");
  ProcessingTiming1947.finish(t,false);ProcessingTiming1947.detailRows1994(t,64);
  ProcessingTiming1947.geometryBand1994(t,64,MS);
  check(t.correctionRows1994==32,"terminal photo row record immutable");
  check(t.geometryBands1994==2,"terminal geometry record immutable");
 }
 static void strongApis()throws Exception{
  ProcessingTiming1947.Trace t=ProcessingTiming1947.begin(new Object());
  check(!render(t).contains("強ノイズGPU枠待ちの内訳"),"missing API observations remain absent");
  ProcessingTiming1947.strongBreakdown1994(t,1200*MS,10,20*MS,16,1300*MS,16);
  String text=render(t);
  check(text.contains("待機開始→復帰 1200 ms / 10回（枠解放待ち・ロック再取得を含む）"),"wait/reacquire scope explicit");
  check(text.contains("強ノイズ転送・投入API累計: 20 ms / 16回"),"submission API distinct");
  check(text.contains("強ノイズ結果取得API累計: 1300 ms / 16回（GPU完了待ち・読戻しを含む）"),"collect includes GPU wait instead of mislabeled pure transfer");
  check(text.contains("並行ワーカーの作業累計")&&text.contains("撮影時間に加算しません"),"parallel scopes cannot be added to elapsed photo time");
  check(text.contains("強ノイズ処理: 未観測"),"telemetry cannot fabricate GPU output adoption");
  long[] before=t.strongBreakdown1994.clone();
  ProcessingTiming1947.strongBreakdown1994(null,1,1,1,1,1,1);
  ProcessingTiming1947.strongBreakdown1994(t,-1,1,1,1,1,1);
  check(Arrays.equals(before,t.strongBreakdown1994),"invalid snapshots inert");
  ProcessingTiming1947.strongBreakdown1994(t,Long.MAX_VALUE,1,0,0,0,0);
  check(t.strongBreakdown1994[0]==Long.MAX_VALUE,"overflow saturates");
  ProcessingTiming1947.finish(t,true);before=t.strongBreakdown1994.clone();
  ProcessingTiming1947.strongBreakdown1994(t,1,1,1,1,1,1);
  check(Arrays.equals(before,t.strongBreakdown1994),"completed diagnostic cannot be rewritten");
 }
 static void sharedModelReason()throws Exception{
  ProcessingTiming1947.Trace t=ProcessingTiming1947.begin(new Object());
  ProcessingTiming1947.pipelineCopyState1989(t,3,4,1024,0,-1,23,0,0,1,0,1024);
  check(render(t).contains("同じ入力のモデルGPU検証を先に実施"),"new shared-input reason rendered");
  check(t.pipelineQueueCounts1989[3*24+23]==1,"exact phase stride for reason23");
 }
 static void adoptionAndShutter()throws Exception{
  ProcessingTiming1947.Trace t=new ProcessingTiming1947.Trace(95,1,100*MS);
  ProcessingTiming1947.strongProfile1994(t,7,0,16);ProcessingTiming1947.modelVariant1994(t,2);
  String text=render(t);
  check(text.contains("強ノイズGPU実採用: 追加認定方式・タイル8 / profile 7 / variant 0 / 16区間"),"actual Strong profile counts rendered without global lookup");
  check(text.contains("モデルGPU実採用: variant 2 / 1作業グループ 128スレッド / 1回"),"model variant uses actual selected workgroup mapping");
  ProcessingTiming1947.strongProfile1994(t,8,0,1);ProcessingTiming1947.modelVariant1994(t,3);
  check(t.strongProfiles1994[21]==16&&t.modelVariants1994[2]==1,"invalid profile and variant ignored");
  ProcessingTiming1947.shutterDown1994(t,120*MS);
  check(t.shutterDownNanos1994==0,"touch after capture request cannot be attributed to this shot");
  ProcessingTiming1947.shutterDown1994(t,90*MS);ProcessingTiming1947.shutterDown1994(t,80*MS);
  check(t.shutterDownNanos1994==90*MS,"first exact UI owner wins");
  t.publicationObserved1990=t.publicationSucceeded1990=true;t.publishedNanos1990=2010*MS;t.finished=2030*MS;t.state=1;
  text=render(t);
  check(text.contains("シャッターボタン受付→保存公開: 1920 ms（ACTION_DOWN受付から・同じ同期処理の撮影）"),"real button boundary includes UI-to-native prefix");
  check(text.contains("撮影記録開始→保存公開完了: 1910 ms"),"existing request-only metric remains separate");
  check(text.contains("全体経過: 1920 ms（シャッターボタン押下受付→保存公開完了。並行工程は加算しません）"),"headline total uses observed actual shutter boundary");
  ProcessingTiming1947.shutterDown1994(t,95*MS);ProcessingTiming1947.strongProfile1994(t,4,0,1);ProcessingTiming1947.modelVariant1994(t,0);
  check(render(t).equals(text),"completed UI binding and adoption cannot change");
 }
 public static void main(String[] args)throws Exception{actualRows();strongApis();sharedModelReason();adoptionAndShutter();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_rows_and_api_scopes_verified\":true}");}
}
