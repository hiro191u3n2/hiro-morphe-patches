package com.hiro.ulike;
public final class FrontTiming1996Test {
 static int assertions;
 static void check(boolean b,String reason){assertions++;if(!b)throw new AssertionError(reason);}
 static String render(ProcessingTiming1947.Trace t)throws Exception{return Timing1982Test.render(t);}
 public static void main(String[] args)throws Exception {
  ProcessingTiming1947.Trace t=ProcessingTiming1947.begin(new Object());
  check(render(t).contains("前段のnative/Java・作業内訳: 未観測"),"absent data explicitly unknown");
  long[] work=new long[24],routes=new long[10];work[0]=2;work[1]=8_000_000;work[8]=2;work[9]=12_000_000;
  routes[0]=1;routes[1]=1;routes[3]=1;routes[8]=1;
  ProcessingTiming1947.front1996(t,work,routes);java.util.Arrays.fill(work,0);java.util.Arrays.fill(routes,0);
  String text=render(t);check(text.contains("native成功 1回 / native false 1回"),"outcomes separate and scalar copied");
  check(text.contains("Bitmap読取りロック待ち: 2回 / 8 ms"),"cumulative worker label");
  check(text.contains("CPU保護マスク生成: 記録なし"),"unseen operation not reported zero");
  check(text.contains("前段の実経過へ加算できません"),"no double counting promise");
  check(t.noiseFirstNanos1976==0&&!t.noiseFirstMeasured1976,"details never synthesize front wall duration");
  work[1]=1;ProcessingTiming1947.front1996(t,work,routes);check(t.frontObserved1996==1,"time without call ignored");
  work[1]=0;routes[0]=-1;ProcessingTiming1947.front1996(t,work,routes);check(t.frontObserved1996==1,"negative routes ignored atomically");
  ProcessingTiming1947.modelOverlap1996(t,7_000_000,0,2_000_000,true,true);
  ProcessingTiming1947.modelOverlap1996(t,-1,-1,-1,false,false);
  text=render(t);check(text.contains("GPU要求が未回収の間のCPU領域推定 7 ms（ほか未計測）"),"pending opportunity and unknown preserved");
  check(text.contains("GPUカーネルとの同時実行時間ではなく"),"not a hardware overlap measurement");
  ProcessingTiming1947.modelOverlap1996(t,0,0,0,false,true);check(t.modelOverlap1996[0]==2,"invalid joined ignored");
  check(t.noiseModelNanos1976==0&&!t.noiseModelMeasured1976,"overlap never changes wall timer");
  ProcessingTiming1947.pipelineCopyState1989(t,3,5,-1,0,-1,24,0,0,0,0,0);
  check(render(t).contains("GPUセッション使用中"),"new explicit permission reason24 rendered");
  ProcessingTiming1947.finish(t,true);long observed=t.frontObserved1996;long n=t.modelOverlap1996[0];
  ProcessingTiming1947.front1996(t,new long[24],new long[10]);ProcessingTiming1947.modelOverlap1996(t,9,9,9,true,true);
  check(t.frontObserved1996==observed&&t.modelOverlap1996[0]==n,"terminal owner immutable");
  ProcessingTiming1947.Trace over=ProcessingTiming1947.begin(new Object());work=new long[24];routes=new long[10];work[0]=1;work[1]=Long.MAX_VALUE;routes[0]=Long.MAX_VALUE;
  ProcessingTiming1947.front1996(over,work,routes);ProcessingTiming1947.front1996(over,work,routes);
  check(over.frontWork1996[1]==Long.MAX_VALUE&&over.frontRoutes1996[0]==Long.MAX_VALUE,"scalar sums saturate");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+"}");
 }
}
