package com.hiro.ulike;
import java.lang.reflect.Method;
import java.util.Arrays;
public final class NoiseTiming19119Test {
 static int assertions;static void need(boolean b,String s){assertions++;if(!b)throw new AssertionError(s);}
 static String render(ProcessingTiming1947.Trace t)throws Exception{Method m=ProcessingTiming1947.class.getDeclaredMethod("render",ProcessingTiming1947.Trace.class);m.setAccessible(true);return (String)m.invoke(null,t);}
 public static void main(String[] args)throws Exception{
  ProcessingTiming1947.Trace t=new ProcessingTiming1947.Trace(1,1,1);
  need(render(t).contains("強ノイズ除算最適化の実採用: 未観測"),"not observed initially");
  need(render(t).contains("前段の保護領域スキップ経路の実採用: 未観測"),"front not observed initially");
  ProcessingTiming1947.noiseOptimization19119(null,0,1,1);
  ProcessingTiming1947.noiseOptimization19119(t,-1,1,1);ProcessingTiming1947.noiseOptimization19119(t,2,1,1);
  ProcessingTiming1947.noiseOptimization19119(t,0,-1,1);ProcessingTiming1947.noiseOptimization19119(t,0,1,-1);ProcessingTiming1947.noiseOptimization19119(t,0,0,1);
  need(Arrays.equals(t.noiseOptimization19119,new long[4]),"invalid observations ignored");need(!t.noiseOptimizationObserved19119[0]&&!t.noiseOptimizationObserved19119[1],"invalid observations do not mark");
  ProcessingTiming1947.noiseOptimization19119(t,0,1,32);ProcessingTiming1947.noiseOptimization19119(t,0,2,64);ProcessingTiming1947.noiseOptimization19119(t,1,4,128);
  need(Arrays.equals(t.noiseOptimization19119,new long[]{3,96,4,128}),"independent accumulators");
  need(render(t).contains("強ノイズ除算最適化の実採用: 3区間 / 96画素"),"strong exact rendered count");
  need(render(t).contains("前段の保護領域スキップ経路の実採用: 4区間 / 128画素"),"front exact rendered count");
  need(render(t).contains("短縮時間の測定ではありません"),"not a speed claim");
  ProcessingTiming1947.noiseOptimization19119(t,0,Long.MAX_VALUE,Long.MAX_VALUE);
  need(t.noiseOptimization19119[0]==Long.MAX_VALUE&&t.noiseOptimization19119[1]==Long.MAX_VALUE,"saturating counters");
  long[] frozen=t.noiseOptimization19119.clone();t.state=1;ProcessingTiming1947.noiseOptimization19119(t,1,10,10);need(Arrays.equals(frozen,t.noiseOptimization19119),"complete photograph immutable");
  t.state=2;ProcessingTiming1947.noiseOptimization19119(t,1,10,10);need(Arrays.equals(frozen,t.noiseOptimization19119),"failed photograph immutable");
  ProcessingTiming1947.Trace other=new ProcessingTiming1947.Trace(2,1,1);need(Arrays.equals(other.noiseOptimization19119,new long[4]),"separate owner");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"physical_android_tested\":false}");
 }
}
