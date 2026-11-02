package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.concurrent.*;
public final class StrongTiming1994Test {
 static int assertions;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static void reset()throws Exception{StrongDefaultRouting1990Test.reset();ProcessingTiming1947.detail1994=null;ProcessingTiming1947.detailCalls1994=0;ProcessingTiming1947.strips1994=0;}
 static void ordinary()throws Exception{
  reset();StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);
  check(d.process(d.route()),"actual production dispatch succeeds");
  check(ProcessingTiming1947.detail1994==null,"workers do not publish new diagnostics on hot path");
  GpuStrong1960.endStage(d.model);d.exact();long[] work=ProcessingTiming1947.detail1994;
  check(ProcessingTiming1947.detailCalls1994==1,"joined stage reports one scalar snapshot");
  check(work.length==6&&work[0]==0&&work[1]==0,"available bank does not fabricate waiting");
  check(work[2]>=0&&work[3]==1&&work[4]>=0&&work[5]==1,"one submit and one collect API recorded");
  check(ProcessingTiming1947.gpu==1&&ProcessingTiming1947.cpu==0&&d.calls==0,"instrumentation preserves selected output and no CPU shadow");
  check(ProcessingTiming1947.profile1994==4&&ProcessingTiming1947.variant1994==0&&ProcessingTiming1947.strips1994==1,"committed profile and variant captured from actual route");
 }
 static void waitAndCancel()throws Exception{
  reset();final StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);
  final Object stage=StrongDefaultRouting1990Test.active();final boolean[] banks=(boolean[])StrongDefaultRouting1990Test.field(stage.getClass(),"banks").get(stage);
  synchronized(stage){banks[0]=banks[1]=true;}
  final Throwable[] failure=new Throwable[1];Thread worker=new Thread(new Runnable(){public void run(){try{d.process(d.route());}catch(Throwable t){failure[0]=t;}}});
  worker.start();long deadline=System.nanoTime()+2000000000L;
  while(worker.getState()!=Thread.State.TIMED_WAITING&&System.nanoTime()<deadline)Thread.yield();
  check(worker.getState()==Thread.State.TIMED_WAITING,"worker is inside real bank wait");
  worker.interrupt();worker.join(2000);
  check(!worker.isAlive()&&failure[0] instanceof CancellationException,"interrupted admission still cancels");
  synchronized(stage){banks[0]=banks[1]=false;}
  GpuStrong1960.endStage(d.model);long[] work=ProcessingTiming1947.detail1994;
  check(work[0]>0&&work[1]>=1,"interrupted Object.wait duration is recorded through finally");
  check(work[3]==0&&work[5]==0,"cancelled wait never invents submit or readback");
  check(ProcessingTiming1947.strips1994==0,"cancelled candidate cannot appear as adopted profile");
  check(GpuNoise1960.leases==0&&GpuNoise1960.active==0,"instrumentation preserves cancellation ownership cleanup");
 }
 public static void main(String[] args)throws Exception{ordinary();waitAndCancel();System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"wait_submit_collect_api_boundaries\":true,\"physical_android_tested\":false}");}
}
