package com.hiro.ulike;

import android.os.Process;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class BackgroundWorker19121Test {
    private static int assertions;
    private static void check(boolean value,String message) { assertions++;if(!value)throw new AssertionError(message); }
    private static Thread start(Runnable work) throws Exception {
        Thread thread=new Thread(work,"background-fixture");thread.start();thread.join(5000);
        check(!thread.isAlive(),"dedicated worker completed");return thread;
    }
    public static void main(String[] args)throws Exception {
        final Thread producer=Thread.currentThread();
        final AtomicInteger writes=new AtomicInteger();
        final AtomicReference<Thread> owner=new AtomicReference<Thread>();
        Runnable wrapped=SpeedWorkers1935.backgroundWorker19121(new Runnable(){public void run(){
            owner.set(Thread.currentThread());writes.incrementAndGet();
        }});
        check(Process.calls.get()==0,"construction never reprioritizes the producer");
        Thread worker=start(wrapped);
        check(owner.get()==worker && Process.lastCaller==worker,"priority and delegate execute on dedicated worker");
        check(!Process.priorities.containsKey(producer),"producer priority stays untouched");
        check(Process.priorities.get(worker).intValue()==10,"public API receives background priority 10");
        check(writes.get()==1 && Process.calls.get()==1,"one delegate invocation and one API attempt");
        check(SpeedWorkers1935.backgroundPriorityApplied19121()==1 && SpeedWorkers1935.backgroundPriorityFailed19121()==0,"successful call observation counted");

        Process.deny=true;
        start(SpeedWorkers1935.backgroundWorker19121(new Runnable(){public void run(){writes.incrementAndGet();}}));
        check(writes.get()==2,"denied scheduling still runs delegate exactly once");
        check(SpeedWorkers1935.backgroundPriorityApplied19121()==1 && SpeedWorkers1935.backgroundPriorityFailed19121()==1,"denial observed independently of successful calls");
        Process.deny=false;

        final RuntimeException expected=new RuntimeException("delegate failure");
        final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        Thread failing=new Thread(SpeedWorkers1935.backgroundWorker19121(new Runnable(){public void run(){writes.incrementAndGet();throw expected;}}));
        failing.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){public void uncaughtException(Thread t,Throwable value){failure.set(value);}});
        failing.start();failing.join(5000);
        check(!failing.isAlive() && failure.get()==expected,"original delegate exception reaches uncaught handler");
        check(writes.get()==3 && SpeedWorkers1935.backgroundPriorityApplied19121()==2,"failure does not rerun work or relabel API success");
        boolean nullRejected=false;
        try{SpeedWorkers1935.backgroundWorker19121(null);}catch(NullPointerException expectedNull){nullRejected=true;}
        check(nullRejected && Process.calls.get()==3,"null work rejected before any scheduling call");

        final int limit=SpeedWorkers1935.maxWorkers();
        check(limit==Math.min(2,Math.max(1,Runtime.getRuntime().availableProcessors())),"normal CPU worker cap preserves UI headroom");
        final CountDownLatch entered=new CountDownLatch(limit);
        final AtomicInteger active=new AtomicInteger(),maximum=new AtomicInteger();
        Runnable[] tasks=new Runnable[limit];
        for(int i=0;i<tasks.length;i++)tasks[i]=new Runnable(){public void run(){
            Integer priority=Process.priorities.get(Thread.currentThread());
            if(priority==null || priority.intValue()!=10)throw new AssertionError("actual CPU factory omitted priority wrapper");
            int n=active.incrementAndGet();
            for(;;){int old=maximum.get();if(n<=old || maximum.compareAndSet(old,n))break;}
            entered.countDown();
            try{if(!entered.await(5,TimeUnit.SECONDS))throw new AssertionError("parallelism reduced");}
            catch(InterruptedException stop){Thread.currentThread().interrupt();throw new IllegalStateException(stop);}
            finally{active.decrementAndGet();}
        }};
        SpeedWorkers1935.run(tasks);
        check(maximum.get()==limit,"all existing worker slots remain usable");
        check(active.get()==0 && SpeedWorkers1935.cpuIdle1944(),"parallel pass returns every CPU permit");
        check(!Process.priorities.containsKey(producer),"pool submission never changes producer priority");
        Field field=SpeedWorkers1935.class.getDeclaredField("EXECUTOR");field.setAccessible(true);
        ThreadPoolExecutor pool=(ThreadPoolExecutor)field.get(null);pool.shutdown();
        check(pool.awaitTermination(5,TimeUnit.SECONDS),"production executor terminates cleanly");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"worker_limit\":"+limit+",\"delegate_exception_preserved\":true,\"producer_priority_untouched\":true}");
    }
}
