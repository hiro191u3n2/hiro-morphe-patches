package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
public final class GpuIdleTrim19122Test {
 static int assertions;
 static native void configure(long bytes,boolean refuse);
 static native int trimCalls();
 static void require(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
 static Field field(String name)throws Exception{Field f=GpuNoise1960.class.getDeclaredField(name);f.setAccessible(true);return f;}
 static void setup(long bytes,boolean refuse)throws Exception{configure(bytes,refuse);field("loaded").setInt(null,1);field("retained").setLong(null,bytes);field("environment").set(null,"host-owner-transport");field("activeSession").setBoolean(null,false);PostSaveCleanup19122.idle=true;PostSaveCleanup19122.generation=7;PostSaveCleanup19122.checkedThread=null;}
 static void refused(long status,String why){long[] got=GpuNoise1960.trimIdle19122(8L<<20,7);require(got[0]==status&&trimCalls()==0,why);}
 @SuppressWarnings("unchecked") static void guards()throws Exception{
  setup(16L<<20,false);field("loaded").setInt(null,0);refused(0,"cold GPU context never initialized by cleanup");
  setup(4L<<20,false);refused(1,"small completed pool retained");
  setup(16L<<20,false);field("activeSession").setBoolean(null,true);refused(0,"active session must not be trimmed");
  setup(16L<<20,false);PostSaveCleanup19122.generation=8;refused(0,"new capture generation cancels cleanup");
  setup(16L<<20,false);PostSaveCleanup19122.idle=false;refused(0,"queue ownership cancels cleanup");
  setup(Long.MAX_VALUE,false);refused(3,"unknown retained estimate never authorizes trim");
  setup(16L<<20,false);ArrayList<GpuNoise1960.Lease1971> leases=(ArrayList<GpuNoise1960.Lease1971>)field("leases1971").get(null);
  GpuNoise1960.Lease1971 lease=new GpuNoise1960.Lease1971(77,new long[]{1},0,0,0,new long[]{0},true);leases.add(lease);
  try{refused(0,"live capacity lease prevents cleanup even if session flag cleared");}finally{leases.remove(lease);}
  setup(16L<<20,false);Thread.currentThread().interrupt();try{refused(0,"pre-interrupted cleanup submits no work");require(Thread.currentThread().isInterrupted(),"pre-interrupt preserved");}finally{Thread.interrupted();}
 }
 static void completedAndQuarantine()throws Exception{
  setup(16L<<20,false);long[] got=GpuNoise1960.trimIdle19122(8L<<20,7);
  require(Arrays.equals(got,new long[]{2,16L<<20,0})&&trimCalls()==1,"known idle large buffer reclaimed and measured");
  require("Hiro-ULike-GX1960".equals(PostSaveCleanup19122.checkedThread),"generation rechecked on GPU owner");
  require(GpuNoise1960.retainedBytes()==0,"cached accounting updated after actual free");
  setup(16L<<20,true);got=GpuNoise1960.trimIdle19122(8L<<20,7);
  require(Arrays.equals(got,new long[]{3,16L<<20,16L<<20})&&trimCalls()==1,"native quarantine refusal preserves retained bytes");
 }
 static void queuedRace()throws Exception{
  setup(16L<<20,false);ExecutorService owner=(ExecutorService)field("OWNER").get(null);
  final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  Future<?> block=owner.submit(new Runnable(){public void run(){entered.countDown();try{release.await();}catch(InterruptedException e){throw new AssertionError(e);}}});require(entered.await(2,TimeUnit.SECONDS),"fixture occupied owner");
  final long[][] got={null};Thread caller=new Thread(new Runnable(){public void run(){got[0]=GpuNoise1960.trimIdle19122(8L<<20,7);}});caller.start();
  long end=System.nanoTime()+2000000000L;while(caller.getState()!=Thread.State.WAITING&&System.nanoTime()<end)Thread.yield();
  require(caller.getState()==Thread.State.WAITING,"cleanup queued behind owner");PostSaveCleanup19122.generation=8;release.countDown();block.get();caller.join(2000);
  require(!caller.isAlive()&&got[0][0]==0&&trimCalls()==0,"generation changed after enqueue prevents actual native trim");
 }
 public static void main(String[] args)throws Exception{System.loadLibrary("idletrim19122");guards();completedAndQuarantine();queuedRace();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"production_owner_executed\":true,\"native_transport_fixture\":true,\"actual_gles\":false}");}
}
