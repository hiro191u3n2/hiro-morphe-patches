package com.hiro.ulike;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public final class SpeedWorkers1935Test {
 static int assertions;
 static void check(boolean b,String m){assertions++;if(!b)throw new AssertionError(m);}
 static void join(Thread t)throws Exception {t.join(15000);check(!t.isAlive(),"no deadlock");}
 static void concurrency()throws Exception {
  final AtomicInteger active=new AtomicInteger(),peak=new AtomicInteger(),done=new AtomicInteger();
  final CountDownLatch release=new CountDownLatch(1),started=new CountDownLatch(SpeedWorkers1935.maxWorkers());
  final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
  Runnable caller=new Runnable(){public void run(){try {
   Runnable[] tasks=new Runnable[4];for(int i=0;i<4;i++)tasks[i]=new Runnable(){public void run(){
    int n=active.incrementAndGet();for(;;){int p=peak.get();if(n<=p||peak.compareAndSet(p,n))break;}
    started.countDown();try{release.await();}catch(InterruptedException e){throw new RuntimeException(e);}finally{active.decrementAndGet();done.incrementAndGet();}
   }};SpeedWorkers1935.run(tasks);
  }catch(Throwable e){failure.compareAndSet(null,e);}}};
  Thread a=new Thread(caller),b=new Thread(caller);a.start();b.start();check(started.await(10,TimeUnit.SECONDS),"shared workers started");
  check(peak.get()==SpeedWorkers1935.maxWorkers(),"shared cap reached");release.countDown();join(a);join(b);
  check(failure.get()==null&&done.get()==8&&active.get()==0,"all concurrent groups completed");
  check(peak.get()<=4,"never caller-runs oversubscription");
 }
 static void legacyOverlap()throws Exception {
  final int limit=SpeedWorkers1935.maxWorkers();final AtomicInteger active=new AtomicInteger(),peak=new AtomicInteger(),done=new AtomicInteger();
  final CountDownLatch started=new CountDownLatch(limit),release=new CountDownLatch(1);final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
  final Runnable body=new Runnable(){public void run(){int n=active.incrementAndGet();for(;;){int p=peak.get();if(n<=p||peak.compareAndSet(p,n))break;}try{for(int i=0;i<1000;i++)Thread.yield();done.incrementAndGet();}finally{active.decrementAndGet();}}};
  Thread[] old=new Thread[limit];for(int i=0;i<limit;i++){old[i]=new Thread(new Runnable(){public void run(){boolean acquired=false;try{
   acquired=SpeedWorkers1935.enterLegacy();int n=active.incrementAndGet();for(;;){int p=peak.get();if(n<=p||peak.compareAndSet(p,n))break;}started.countDown();release.await();active.decrementAndGet();
   SpeedWorkers1935.run(new Runnable[]{body});
  }catch(Throwable error){failure.compareAndSet(null,error);}finally{SpeedWorkers1935.leaveLegacy(acquired);}}});old[i].start();}
  check(started.await(10,TimeUnit.SECONDS),"legacy workers own shared budget");
  Thread modern=new Thread(new Runnable(){public void run(){try{SpeedWorkers1935.run(new Runnable[]{body,body,body,body});}catch(Throwable e){failure.compareAndSet(null,e);}}});modern.start();release.countDown();
  for(Thread t:old)join(t);join(modern);check(failure.get()==null&&done.get()==limit+4&&active.get()==0,"legacy and pooled groups drain");
  check(peak.get()<=limit,"legacy4 plus next-capture4 share max4 CPU bodies");
 }
 static void nested()throws Exception {
  final AtomicInteger done=new AtomicInteger();final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
  Thread caller=new Thread(new Runnable(){public void run(){try {
   Runnable[] outer=new Runnable[4];for(int i=0;i<4;i++)outer[i]=new Runnable(){public void run(){
    Runnable[] inner=new Runnable[4];for(int k=0;k<4;k++)inner[k]=new Runnable(){public void run(){done.incrementAndGet();}};SpeedWorkers1935.run(inner);
   }};SpeedWorkers1935.run(outer);
  }catch(Throwable e){failure.set(e);}}});caller.start();join(caller);check(failure.get()==null&&done.get()==16,"nested scheduling completes");
 }
 static void interruption()throws Exception {
  final CountDownLatch started=new CountDownLatch(1);final AtomicInteger active=new AtomicInteger(),finished=new AtomicInteger();
  final AtomicBoolean restored=new AtomicBoolean(),threw=new AtomicBoolean();
  Thread caller=new Thread(new Runnable(){public void run(){
   Runnable[] work=new Runnable[12];for(int i=0;i<work.length;i++)work[i]=new Runnable(){public void run(){
    active.incrementAndGet();started.countDown();try{while(!Thread.currentThread().isInterrupted())Thread.yield();}
    finally{active.decrementAndGet();finished.incrementAndGet();}
   }};
   try{SpeedWorkers1935.run(work);}catch(IllegalStateException expected){threw.set(true);}
   restored.set(Thread.currentThread().isInterrupted());
  }});caller.start();check(started.await(10,TimeUnit.SECONDS),"cancel task started");caller.interrupt();join(caller);
  check(restored.get()&&threw.get()&&active.get()==0,"interruption drained tasks and restored caller flag");
  check(finished.get()>0&&finished.get()<=SpeedWorkers1935.maxWorkers(),"queued tasks skip after cancellation");
  final AtomicBoolean clean=new AtomicBoolean();SpeedWorkers1935.run(new Runnable[]{new Runnable(){public void run(){clean.set(!Thread.currentThread().isInterrupted());}}});
  check(clean.get(),"worker interruption does not leak into next group");
 }
 static void errors() {
  final AtomicBoolean slowDone=new AtomicBoolean();boolean caught=false;
  try{SpeedWorkers1935.run(new Runnable[]{new Runnable(){public void run(){throw new IllegalArgumentException("expected");}},new Runnable(){public void run(){for(int i=0;i<1000;i++)Thread.yield();slowDone.set(true);}}});}
  catch(IllegalArgumentException expected){caught=true;}
  check(caught&&slowDone.get(),"failure waits all leased users before returning");
  SpeedWorkers1935.release(new int[1024]);caught=false;
  try{SpeedWorkers1935.run(new Runnable[]{new Runnable(){public void run(){throw new OutOfMemoryError("injected");}}});}
  catch(OutOfMemoryError expected){caught=true;}
  check(caught&&SpeedWorkers1935.retainedBytes()==0,"OOM clears retained scratch");
 }
 static void arrays()throws Exception {
  SpeedWorkers1935.trim();int[] a=SpeedWorkers1935.borrowInts(41),b=SpeedWorkers1935.borrowInts(41);check(a!=b&&a.length==41&&b.length==41,"exclusive exact size leases");
  SpeedWorkers1935.release(a);check(SpeedWorkers1935.borrowInts(41)==a,"same-size array reuse");SpeedWorkers1935.release(a);SpeedWorkers1935.release(a);
  check(SpeedWorkers1935.retainedBytes()==164,"duplicate return cannot duplicate leases");
  float[] f=SpeedWorkers1935.borrowFloats(41);check(f.length==41,"float type independent");SpeedWorkers1935.release(f);SpeedWorkers1935.release(b);
  final Set<Object> owned=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
  Thread[] ts=new Thread[8];for(int k=0;k<ts.length;k++){final int id=k+1;ts[k]=new Thread(new Runnable(){public void run(){try{for(int i=0;i<100;i++){
   int[] x=SpeedWorkers1935.borrowInts(47);synchronized(owned){if(!owned.add(x))throw new AssertionError("shared lease");}Arrays.fill(x,id);Thread.yield();for(int v:x)if(v!=id)throw new AssertionError("cross-photo bytes");
   synchronized(owned){owned.remove(x);}SpeedWorkers1935.release(x);
  }}catch(Throwable e){failure.compareAndSet(null,e);}}});ts[k].start();}for(Thread t:ts)join(t);
  check(failure.get()==null&&owned.isEmpty(),"parallel leases never alias");
  for(int i=0;i<5;i++)SpeedWorkers1935.release(new int[2*1024*1024]);check(SpeedWorkers1935.retainedBytes()<=24L*1024*1024,"retained byte cap");
  long n=SpeedWorkers1935.retainedBytes();SpeedWorkers1935.release(new int[2*1024*1024+1]);check(SpeedWorkers1935.retainedBytes()==n,"oversize not retained");SpeedWorkers1935.trim();check(SpeedWorkers1935.retainedBytes()==0,"explicit trim");
 }
 public static void main(String[] args)throws Exception{concurrency();legacyOverlap();nested();interruption();errors();arrays();System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":6,\"physical_device_verified\":false}");}
}
