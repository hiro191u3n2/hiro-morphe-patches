package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Actual shared worker pool with optional injected OS-hook doubles. */
public final class SpeedWorkersHints1952Test {
    static int assertions;
    static void yes(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static final class Token {
        final Thread owner=Thread.currentThread();
        boolean completed;
    }
    static final class Hooks implements SpeedWorkers1935.Hints {
        final AtomicInteger workers=new AtomicInteger(),closed=new AtomicInteger();
        final AtomicInteger begins=new AtomicInteger(),success=new AtomicInteger(),failure=new AtomicInteger();
        final AtomicInteger active=new AtomicInteger(),maximum=new AtomicInteger(),ended=new AtomicInteger();
        volatile boolean throwBegin,throwComplete;
        public Runnable worker(final Runnable loop) {
            workers.incrementAndGet();
            return new Runnable(){public void run(){try{loop.run();}finally{closed.incrementAndGet();}}};
        }
        public Object begin(long target) {
            if(throwBegin)throw new IllegalStateException("optional OS begin failure");
            if(target<=0 || SpeedWorkers1935.cpuIdle1944() || SpeedWorkers1935.availableWorkers1944()!=1)
                throw new AssertionError("hint precedes the CPU permit/ACTIVE task");
            begins.incrementAndGet();int n=active.incrementAndGet();
            for(;;){int old=maximum.get();if(n<=old || maximum.compareAndSet(old,n))break;}
            return new Token();
        }
        public void complete(Object value,boolean ok) {
            Token token=(Token)value;
            if(token.owner!=Thread.currentThread() || token.completed)throw new AssertionError("hint token lost worker ownership");
            token.completed=true;active.decrementAndGet();ended.incrementAndGet();
            (ok?success:failure).incrementAndGet();
            if(throwComplete)throw new IllegalStateException("optional OS complete failure");
        }
    }
    static Runnable[] tasks(int count,final AtomicInteger writes){
        Runnable[] a=new Runnable[count];
        for(int i=0;i<count;i++)a[i]=new Runnable(){public void run(){writes.incrementAndGet();}};
        return a;
    }
    public static void main(String[] args)throws Exception {
        final Hooks hints=new Hooks();SpeedWorkers1935.installHints(hints);
        AtomicInteger writes=new AtomicInteger();
        SpeedWorkers1935.run(tasks(12,writes));
        yes(writes.get()==12,"all pool tasks complete");
        yes(hints.begins.get()==12 && hints.success.get()==12,"each completed actual task has exactly one hint");
        yes(hints.active.get()==0 && hints.ended.get()==12,"all hint tokens ended on their worker");
        yes(SpeedWorkers1935.cpuIdle1944(),"permits returned after successful hints");
        final AtomicInteger nested=new AtomicInteger();final int before=hints.begins.get();
        SpeedWorkers1935.run(new Runnable[]{new Runnable(){public void run(){SpeedWorkers1935.run(tasks(7,nested));}}});
        yes(nested.get()==7,"nested actual work completes inline");
        yes(hints.begins.get()==before+1,"nested tasks do not double-report an outer task");
        yes(SpeedWorkers1935.cpuIdle1944(),"nested work returns original budget");
        hints.throwBegin=true;SpeedWorkers1935.run(tasks(3,writes));hints.throwBegin=false;
        yes(writes.get()==15,"begin service failure cannot drop real work");
        yes(SpeedWorkers1935.cpuIdle1944(),"begin service failure returns all permits");
        hints.throwComplete=true;SpeedWorkers1935.run(tasks(3,writes));hints.throwComplete=false;
        yes(writes.get()==18,"complete service failure cannot fail photos");
        yes(hints.active.get()==0 && SpeedWorkers1935.cpuIdle1944(),"failed completion cannot leak hint state or CPU permits");
        final RuntimeException expected=new RuntimeException("delegate failure");
        boolean propagated=false;int failures=hints.failure.get();
        try{SpeedWorkers1935.run(new Runnable[]{new Runnable(){public void run(){throw expected;}}});}
        catch(RuntimeException actual){propagated=actual==expected;}
        yes(propagated,"original delegate exception preserved");
        yes(hints.failure.get()==failures+1,"partial failed work is reported as cancelled");
        yes(hints.active.get()==0 && SpeedWorkers1935.cpuIdle1944(),"delegate failure drains actual workers");
        final CountDownLatch started=new CountDownLatch(1),interrupted=new CountDownLatch(1);
        final AtomicReference<Throwable> cancellation=new AtomicReference<Throwable>();
        Thread producer=new Thread(new Runnable(){public void run(){try{
            SpeedWorkers1935.run(new Runnable[]{new Runnable(){public void run(){started.countDown();try{
                new CountDownLatch(1).await();
            }catch(InterruptedException stop){interrupted.countDown();Thread.currentThread().interrupt();throw new IllegalStateException(stop);}}}});
        }catch(Throwable stop){cancellation.set(stop);}}});
        producer.start();yes(started.await(5,TimeUnit.SECONDS),"cancel test reaches actual permit-owned work");
        producer.interrupt();producer.join(5000);
        yes(!producer.isAlive() && interrupted.getCount()==0,"producer cancellation drains interrupted actual worker");
        yes(cancellation.get()!=null,"producer receives original cancellation failure");
        yes(hints.active.get()==0 && SpeedWorkers1935.cpuIdle1944(),"cancelled tasks release hint and CPU ownership");
        yes(hints.maximum.get()<=SpeedWorkers1935.maxWorkers(),"hints never add simultaneous CPU workers");
        yes(hints.ended.get()==hints.begins.get(),"every successful hint begin has one terminal callback");
        Field f=SpeedWorkers1935.class.getDeclaredField("EXECUTOR");f.setAccessible(true);
        ThreadPoolExecutor executor=(ThreadPoolExecutor)f.get(null);executor.shutdown();
        yes(executor.awaitTermination(5,TimeUnit.SECONDS),"actual executor worker loops terminate");
        yes(hints.closed.get()==hints.workers.get() && hints.closed.get()>0,"wrapped worker session lifetime closes with each actual worker");
        SpeedWorkers1935.installHints(null);SpeedWorkers1935.trim();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"worker_budget_preserved\":true,\"nested_work_not_double_reported\":true,\"worker_lifecycle_closes\":true,\"optional_hint_failure_preserves_work\":true,\"cancellation_drains_before_return\":true}");
    }
}
