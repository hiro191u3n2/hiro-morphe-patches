package com.hiro.ulike;
import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;
/** Executes the production scalar observer. No copied observer implementation. */
public final class ResidentTiming19111Test {
 static int assertions,cases;static final long LIMIT=86400000000000L;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static Field field(Class<?> c,String name)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}
 static final class Cancel implements GpuQualification1961.Cancellation {volatile boolean value;public boolean cancelled(){return value;}}
 @SuppressWarnings("unchecked") static ThreadLocal<Object> current()throws Exception{return (ThreadLocal<Object>)field(ResidentTiming19111.class,"CURRENT").get(null);}
 static Object sample()throws Exception{return current().get();}
 static long[] values(Object s)throws Exception{return (long[])field(s.getClass(),"values").get(s);}
 static void set(Object s,String n,Object value)throws Exception{field(s.getClass(),n).set(s,value);}
 static void invoke(Object s,String name,Class<?>[] types,Object...args)throws Exception{Method m=s.getClass().getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(s,args);}
 static Cancel eligible()throws Exception{current().remove();GpuQualification1961.records.clear();GpuQualification1961.throwSink=false;Cancel c=new Cancel();GpuQualification1961.owner.set(c);GpuQualification1961.bg.set(true);GpuResident1976.cpu.set(false);return c;}
 static void send(String op,long...args){ResidentTiming19111.dispatch(op,args);}
 static Object begin()throws Exception{send("begin",0,0);Object s=sample();check(s!=null,"eligible saved-image speed scope starts");return s;}
 static void deterministic(Object s)throws Exception{send("start",1000);set(s,"active",0);}
 static long[] publish()throws Exception{send("publish");check(sample()==null,"publish detaches its thread owner");check(GpuQualification1961.records.size()==1,"exactly one bounded scalar record");return GpuQualification1961.records.get(0);}
 static void eligibility()throws Exception{
  cases++;
  for(int mode=0;mode<5;mode++){Cancel c=eligible();if(mode==0)GpuQualification1961.owner.remove();if(mode==1)c.value=true;if(mode==2)GpuQualification1961.bg.set(false);if(mode==3)GpuResident1976.cpu.set(true);
   send("begin",mode==4?2:0,0);check(sample()==null,"foreground/oracle/cancelled/invalid trial is not observed");send("start",1000);send("phase",0);send("stop",1010,1);send("publish");check(GpuQualification1961.records.isEmpty(),"ineligible scope emits no report");}
  eligible();ResidentTiming19111.dispatch("begin",null);check(sample()==null,"missing begin input rejected");send("begin",0);check(sample()==null,"short begin input rejected");send("begin",0,2);check(sample()==null,"invalid candidate rejected");
 }
 static void exclusive()throws Exception{
  cases++;eligible();Object s=begin();send("start",1000);long now=1000,expected=0;
  for(int p=0;p<14;p++){set(s,"active",p);long elapsed=p+1;now+=elapsed;expected+=elapsed;invoke(s,"interval",new Class<?>[]{long.class,boolean.class},now,true);check(values(s)[7+p]==elapsed,"each phase owns its disjoint interval");}
  set(s,"active",-1);send("stop",now+9,1);long[] record=publish();
  check(record.length==29&&record[0]==1&&record[1]==1,"bounded versioned completed record");check(record[2]==expected+9&&record[3]==expected&&record[4]==9,"phase subtotal plus unassigned time equals measured wall");
  check(record[5]==(1L<<14)-1&&record[6]==0,"all completed phases represented once");
  for(int i=21;i<29;i++)check(record[i]==-1,"unobserved plan/count fields remain unknown");
  values(s)[7]=9999;check(record[7]==1,"sink receives detached scalar clone");send("publish");check(GpuQualification1961.records.size()==1,"duplicate publish emits nothing");
 }
 static void actualDispatch()throws Exception{
  cases++;eligible();begin();long start=System.nanoTime();send("start",start);send("phase",0);send("phase",0);send("phase",13);long end=System.nanoTime();send("stop",end,1);long[] r=publish();
  check(r[2]==end-start&&r[3]>=0&&r[3]<=r[2]&&r[4]==r[2]-r[3],"actual timestamp dispatch is bounded and exclusive");check(r[5]==((1L<<0)|(1L<<13))&&r[6]==0,"duplicate active phase does not add a second phase");
 }
 static void partialAndCancel()throws Exception{
  cases++;eligible();Object s=begin();deterministic(s);invoke(s,"interval",new Class<?>[]{long.class,boolean.class},1030L,true);set(s,"active",1);send("stop",1080,2);long[] r=publish();
  check(r[1]==2&&r[2]==80&&r[3]==80&&r[7]==30&&r[8]==50,"partial work retains observed durations");check(r[5]==0&&r[6]==3,"failed scope marks every visited phase as incomplete");
  Cancel c=eligible();s=begin();deterministic(s);send("stop",1010,1);c.value=true;r=publish();check(r[1]==3&&r[5]==0&&r[6]==1,"late cancellation removes completed-phase claims");
  eligible();s=begin();deterministic(s);send("stop",1010,1);Thread.currentThread().interrupt();try{r=publish();check(r[1]==3&&Thread.currentThread().isInterrupted(),"observer reports interruption without clearing its state");}finally{Thread.interrupted();}
  eligible();begin();send("publish");check(GpuQualification1961.records.isEmpty(),"scope without timing start emits nothing");
 }
 static void planBounds()throws Exception{
  cases++;eligible();Object s=begin();deterministic(s);send("strongPlan",4,256,4);send("strongCounts",16,0);send("finishRows",128);send("geometryBand",0);send("finishBand",0);send("finishRows",32);send("geometryBand",0);send("finishBand",0);
  send("strongPlan",0,256,4);send("strongPlan",4,65537,4);send("strongCounts",-1,3);send("strongCounts",1000001,3);send("finishRows",0);send("stop",1010,1);long[] r=publish();
  check(Arrays.equals(Arrays.copyOfRange(r,21,29),new long[]{4,256,4,16,0,32,2,2}),"retry keeps API counts while the latest row plan and invalid-input bounds stay accurate");
  eligible();s=begin();deterministic(s);send("geometryBand",0);send("finishBand",0);send("stop",1010,1);r=publish();check(r[27]==-1&&r[28]==-1,"band count stays unknown until an actual finish plan is recorded");
 }
 static void invalidAndFailure()throws Exception{
  cases++;
  for(int mode=0;mode<5;mode++){eligible();Object s=begin();deterministic(s);if(mode==0)send("phase",14);if(mode==1)send("phase",Long.MAX_VALUE);if(mode==2)send("start",1000);if(mode==3)send("stop",999,1);if(mode==4){set(s,"last",1000L);send("stop",1000+LIMIT+1,1);}send("publish");check(sample()==null&&GpuQualification1961.records.isEmpty(),"invalid timestamps/phase/restart drop diagnostics without retaining owner");}
  eligible();Object s=begin();deterministic(s);send("stop",1010,1);GpuQualification1961.throwSink=true;send("publish");check(sample()==null&&GpuQualification1961.records.isEmpty(),"sink failure cannot escape or retain timing scope");GpuQualification1961.throwSink=false;
 }
 static void threadIsolation()throws Exception{
  cases++;eligible();final Throwable[] failures=new Throwable[2];CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);Thread[] threads=new Thread[2];
  for(int id=0;id<2;id++){final int index=id;threads[id]=new Thread(()->{try{GpuQualification1961.owner.set(new Cancel());GpuQualification1961.bg.set(true);GpuResident1976.cpu.set(false);send("begin",0,index);Object s=sample();send("start",1000);set(s,"active",index);ready.countDown();if(!go.await(5,TimeUnit.SECONDS))throw new AssertionError("thread test release timeout");send("stop",1010+index,1);send("publish");if(sample()!=null)throw new AssertionError("thread timing owner retained");}catch(Throwable t){failures[index]=t;ready.countDown();}});threads[id].start();}
  check(ready.await(5,TimeUnit.SECONDS),"parallel observers reached independent scopes");go.countDown();for(Thread t:threads)t.join(5000);for(int i=0;i<2;i++)check(!threads[i].isAlive()&&failures[i]==null,"each observer completed without shared-state failure");
  check(GpuQualification1961.records.size()==2,"each thread emits one independent record");Set<Long> bits=new HashSet<Long>();for(long[] r:GpuQualification1961.records)bits.add(r[5]);check(bits.equals(new HashSet<Long>(Arrays.asList(1L,2L))),"thread phases never merge");check(sample()==null,"worker diagnostics do not leak to caller thread");
 }
 public static void main(String[] args)throws Exception{
  eligibility();exclusive();actualDispatch();partialAndCancel();planBounds();invalidAndFailure();threadIsolation();
  for(Class<?> c:ResidentTiming19111.class.getDeclaredClasses())for(Field f:c.getDeclaredFields())check(f.getType().isPrimitive()||f.getType()==long[].class,"observer retains only scalar fields");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"actual_timing_observer_executed19111\":true,\"exclusive_timing_boundaries19111\":true,\"timing_optional_failure_isolation19111\":true,\"timing_cancel_state_preserved19111\":true,\"timing_scalar_thread_ownership19111\":true,\"physical_android_tested\":false}");
 }
}
