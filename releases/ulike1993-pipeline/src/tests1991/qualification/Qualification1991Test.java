package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class Qualification1991Test {
 static int checks;static final long M=1024L*1024;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static Field field(String name)throws Exception{Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f;}
 static void reset()throws Exception{SaveQueue1935.idle=false;GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
  long end=System.nanoTime()+3000000000L;while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>end)throw new AssertionError("owner drain");Thread.sleep(2);}
  synchronized(field("LOCK").get(null)){((Map<?,?>)field("DEFERRED1991").get(null)).clear();field("consecutivePrimary1991").setInt(null,0);field("lastPrimary1984").setInt(null,0);field("lastLegacy1981").setBoolean(null,false);}
  GpuQualification1961.initialize(new Context());
 }
 static final class Probe implements GpuQualification1961.Probe {
  final AtomicInteger closes=new AtomicInteger(),runs=new AtomicInteger();final List<String> order;final String name;
  Probe(String name,List<String> order){this.name=name;this.order=order;}
  public void run(GpuQualification1961.Cancellation c){runs.incrementAndGet();if(order!=null)order.add(name);}
  public void close(){closes.incrementAndGet();}
 }
 static void queue(String key,Probe p)throws Exception{GpuQualification1961.Reservation1984 r=GpuQualification1961.reserve1985(key,1);
  try{check(r.accepted()&&r.begin1985(),"primary begins only after budget");check(r.commit(p).accepted,"primary transferred once");}finally{r.close();}}
 static void drain()throws Exception{SaveQueue1935.idle=true;field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();
  long end=System.nanoTime()+3000000000L;while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>end)throw new AssertionError("worker drain");Thread.sleep(2);}}
 public static void main(String[] args)throws Exception{
  reset();String key="preparation-starved";
  for(int i=0;i<2;i++){Probe p=new Probe(key,null);check(GpuQualification1961.schedule(key,60*M,p),"fresh auxiliary admitted");
   GpuQualification1961.Reservation1984 r=GpuQualification1961.reserve1985("test|finish-chain1976:v2|"+i,48*M);
   check(r.accepted(),"first two priority admissions allowed");check(p.closes.get()==1&&p.runs.get()==0,"retired snapshot closed exactly once before budget release");r.close();
   GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;check(GpuQualification1961.retainedBytes()==0,"cancelled old snapshots not retained by fairness");}
  List<String> order=Collections.synchronizedList(new ArrayList<String>());Probe aged=new Probe("aged",order);
  check(GpuQualification1961.schedule(key,60*M,aged),"twice retired key gets fresh snapshot");
  GpuQualification1961.Reservation1984 denied=GpuQualification1961.reserve1985("test|finish-chain1976:v2|late",48*M);
  check(!denied.accepted()&&aged.closes.get()==0&&GpuQualification1961.retainedBytes()==60*M,"protected snapshot cannot be evicted or overbook cap");denied.close();
  queue("test|finish-chain1976:v2|small",new Probe("primary",order));drain();
  check(order.equals(Arrays.asList("aged","primary")),"protected auxiliary executes at next quiet opportunity");check(aged.closes.get()==1&&aged.runs.get()==1,"protected ownership released once");
  reset();order=Collections.synchronizedList(new ArrayList<String>());
  check(GpuQualification1961.schedule("waiting-aux",1,new Probe("aux",order)),"aux queued");
  queue("strong-test:3:primary1",new Probe("strong1",order));queue("test|finish-chain1976:v2|p1",new Probe("finish1",order));
  queue("strong-test:3:primary2",new Probe("strong2",order));queue("test|finish-chain1976:v2|p2",new Probe("finish2",order));
  drain();check(order.indexOf("aux")==2,"waiting auxiliary executes after two primary starts");
  reset();GpuQualification1961.rejectNonSpeed1988("memory-only","memory_budget");GpuQualification1961.rejectSpeed("speed-only");
  String status=GpuQualification1961.status1967();check(status.contains("メモリ不足1件 / 実測速度未達1件"),"unmeasured allocation failure distinguished from measured speed failure");
  check(!GpuQualification1961.exactRejected("memory-only"),"memory failure not pixel rejection");check(!GpuQualification1961.maySchedule("memory-only"),"existing cooldown preserved");
  GpuQualification1961.rejectExact("bad-pixel");check(GpuQualification1961.exactRejected("bad-pixel"),"exact rejection survives fairness changes");
  check(!GpuQualification1961.canQueue("too-large",96*M+1),"96MiB admission ceiling remains");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"actual_qualification_queue\":true,\"physical_android_tested\":false}");
 }
}
