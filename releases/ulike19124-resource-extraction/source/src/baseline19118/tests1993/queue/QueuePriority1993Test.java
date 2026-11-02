package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Execute the real bounded queue; only Android/save-idle peers are fixtures. */
public final class QueuePriority1993Test {
 static int checks;static final long M=1024L*1024;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static Field field(String name)throws Exception{Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f;}
 static void reset()throws Exception{SaveQueue1935.idle=false;GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
  long deadline=System.nanoTime()+3000000000L;while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>deadline)throw new AssertionError("owner drain");Thread.sleep(2);}
  synchronized(field("LOCK").get(null)){((Map<?,?>)field("DEFERRED1991").get(null)).clear();field("consecutivePrimary1991").setInt(null,0);}
  GpuQualification1961.initialize(new Context());
 }
 static class Probe implements GpuQualification1961.Probe {
  final AtomicInteger closes=new AtomicInteger(),runs=new AtomicInteger();final List<String> order;final String name;
  Probe(String name,List<String> order){this.name=name;this.order=order;}
  public void run(GpuQualification1961.Cancellation c){runs.incrementAndGet();if(order!=null)order.add(name);}
  public void close(){closes.incrementAndGet();}
 }
 static void drain()throws Exception{SaveQueue1935.idle=true;field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();
  long deadline=System.nanoTime()+3000000000L;while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>deadline)throw new AssertionError("worker drain");Thread.sleep(2);}}
 public static void main(String[] args)throws Exception {
  reset();String prefix="cpu-finish1976-exact2-time5-v1:";Probe cache=new Probe("cache",null);
  check(GpuQualification1961.schedule(prefix+"first",60*M,cache),"one cache proof allowed");
  GpuQualification1961.QueueDecision1983 declined=GpuQualification1961.canQueue1983(prefix+"second",1);
  check(!declined.accepted&&declined.reason==22,"next band declined before copying");
  check(GpuQualification1961.canQueue("single-stage1981-current-cpu-exact2-time5-v2:front",48*M),"foreground proof reclaims cache bytes");
  check(cache.closes.get()==1&&cache.runs.get()==0,"retired cache closes once without running");
  check(GpuQualification1961.retainedBytes()==0,"preflight retirement releases budget");
  check(((Map<?,?>)field("DEFERRED1991").get(null)).isEmpty(),"tiny cache does not gain starvation priority");
  reset();GpuQualification1961.Reservation1984 ticket=GpuQualification1961.reserve1985(prefix+"reserved",1);
  check(ticket.accepted(),"one reserved cache allowed");
  check(GpuQualification1961.reserve1985(prefix+"reserved-second",1).decision.reason==22,"reservation counts toward cap");ticket.close();
  check(GpuQualification1961.reserve1985(prefix+"reserved-third",1).accepted(),"closing cache permits next proof");reset();
  List<String> order=Collections.synchronizedList(new ArrayList<String>());
  check(GpuQualification1961.schedule(prefix+"normal",1,new Probe("cache",order)),"cache queued");
  check(GpuQualification1961.schedule("other-auxiliary",1,new Probe("other",order)),"ordinary auxiliary queued");
  check(GpuQualification1961.schedule("single-stage1981-current-cpu-exact2-time5-v2:fast",1,new Probe("front",order)),"front queued");
  drain();check(order.get(0).equals("front"),"front runs before optional band cache");
  check(new HashSet<String>(order).size()==3,"no auxiliary dropped without need");
  reset();GpuQualification1961.rejectExact("single-stage1981-current-cpu-exact2-time5-v2:bad");
  check(!GpuQualification1961.canQueue("single-stage1981-current-cpu-exact2-time5-v2:bad",1),"priority does not bypass pixel rejection");
  check(!GpuQualification1961.canQueue("single-stage1981-current-cpu-exact2-time5-v2:oversized",96*M+1),"96MiB cap unchanged");
  check(GpuQualification1961.schedule("exception93",1,new Probe("fault",null){public void run(GpuQualification1961.Cancellation c){throw new IllegalArgumentException("PRIVATE-IMAGE-TEXT-MUST-NOT-BE-LOGGED");}}),"fault probe queued");drain();
  String summary=GpuQualification1961.attemptSummary1985();
  check(summary.contains("IllegalArgumentException")&&summary.contains("QueuePriority1993Test"),"exception type and code location recorded");
  check(summary.contains("発生工程")&&!summary.contains("PRIVATE-IMAGE-TEXT"),"phase shown and throwable message omitted");
  check(GpuQualification1961.retainedBytes()==0,"fault releases input ownership");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"actual_qualification_queue\":true,\"physical_android_tested\":false}");
 }
}
