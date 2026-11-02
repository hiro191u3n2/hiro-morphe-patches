package com.hiro.ulike;
public final class Timing1993Test {
 static int assertions; static final long MS=1000000L;
 static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
 static String render(ProcessingTiming1947.Trace trace)throws Exception{return Timing1982Test.render(trace);}
 static void publicationEndpoint()throws Exception{
  ProcessingTiming1947.Trace t=new ProcessingTiming1947.Trace(93,1,MS);
  t.publicationObserved1990=true;t.publicationSucceeded1990=true;t.publishedNanos1990=3983*MS;
  t.finished=4099*MS;t.state=1;
  String text=render(t);
  check(text.contains("撮影記録開始→保存公開完了: 3982 ms"),"publication duration remains exact");
  check(text.contains("全体経過: 3982 ms（撮影記録開始→保存公開完了。並行工程は加算しません）"),"total uses publication rather than later diagnostics completion");
  check(!text.contains("4098 ms"),"post-publication overhead cannot inflate save duration");
  t.publicationSucceeded1990=false;t.publishedNanos1990=0;
  text=render(t);
  check(text.contains("全体経過: 4098 ms（保存公開未完了。並行工程は加算しません）"),"failed capture retains real completion time with clear publication failure");
  check(text.contains("撮影記録開始→保存公開完了: 公開失敗"),"completion never invents successful publication");
 }
 static void newQueueReason()throws Exception{
  ProcessingTiming1947.Trace t=new ProcessingTiming1947.Trace(94,1,MS);
  ProcessingTiming1947.pipelineCopyState1989(t,9,4,1024,0,-1,22,0,0,1,0,1024);
  check(render(t).contains("CPU仕上げキャッシュ検証は同時1件まで"),"new queue reason rendered on owning phase");
  check(t.pipelineQueueCounts1989.length==11*23&&t.pipelineQueueCounts1989[9*23+22]==1,"expanded finite queue counters have exact phase stride");
  long[] snapshot=t.pipelineQueueCounts1989.clone();
  ProcessingTiming1947.pipelineCopyState1989(t,9,4,1024,0,-1,23,0,0,1,0,1024);
  check(java.util.Arrays.equals(snapshot,t.pipelineQueueCounts1989),"unknown reason still rejected");
 }
 public static void main(String[] args)throws Exception{publicationEndpoint();newQueueReason();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"save_publication_endpoint_consistent\":true}");}
}
