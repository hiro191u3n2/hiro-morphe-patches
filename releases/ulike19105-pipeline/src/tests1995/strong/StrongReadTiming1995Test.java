package com.hiro.ulike;
public final class StrongReadTiming1995Test {
 static int assertions;
 static void check(boolean b,String reason){assertions++;if(!b)throw new AssertionError(reason);}
 public static void main(String[] args)throws Exception{
  StrongDefaultRouting1990Test.reset();ProcessingTiming1947.directStrips1995=ProcessingTiming1947.directCalls1995=0;ProcessingTiming1947.directPixels1995=0;
  StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);
  check(d.process(d.route()),"committed private readback succeeds");
  check(ProcessingTiming1947.directCalls1995==0,"readback telemetry stays off per-strip hot publication path");
  GpuStrong1960.endStage(d.model);d.exact();
  check(ProcessingTiming1947.directCalls1995==1,"joined stage publishes direct counts once");
  check(ProcessingTiming1947.directStrips1995==1&&ProcessingTiming1947.directPixels1995>0,"successful direct pixels and strips recorded");
  check(ProcessingTiming1947.gpu==1&&ProcessingTiming1947.cpu==0&&d.calls==0,"direct observation matches actual GPU commit without CPU shadow");
  GpuStrong1960.endStage(d.model);
  check(ProcessingTiming1947.directCalls1995==1,"duplicate stage close cannot double count");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_jni\":false}");
 }
}
