package com.hiro.ulike;

import android.content.Context;
import android.os.Build;
import android.os.PerformanceHintManager;
import android.os.PerformanceHintManager.Session;
import android.os.Process;
import android.os.SystemClock;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

public final class PerformanceHints1952Test {
    private static int assertions;
    private static void need(boolean value,String message) { assertions++;if(!value)throw new AssertionError(message); }
    private static void reset() throws Exception {
        Field api=PerformanceHints1952.class.getDeclaredField("api");api.setAccessible(true);api.set(null,null);
        Field worker=PerformanceHints1952.class.getDeclaredField("WORKER");worker.setAccessible(true);((ThreadLocal<?>)worker.get(null)).remove();
        SpeedWorkers1935.hints=null;
        Build.VERSION.SDK_INT=36;Process.setTid(923);SystemClock.setNanos(1000000000L);
    }
    private static PerformanceHintManager configured() throws Exception {
        reset();PerformanceHintManager manager=new PerformanceHintManager();PerformanceHints1952.init(new Context(manager));
        need(SpeedWorkers1935.hints!=null,"pool adapter installed only with valid public service");return manager;
    }
    private static void unavailable() throws Exception {
        reset();PerformanceHintManager m=new PerformanceHintManager();Context c=new Context(m);
        Build.VERSION.SDK_INT=30;PerformanceHints1952.init(c);
        need(c.requests==0 && PerformanceHints1952.begin(1000000L)==null && SpeedWorkers1935.hints==null,"API <31 harmless");
        reset();PerformanceHints1952.init(null);need(PerformanceHints1952.begin(1000)==null,"null context");
        reset();PerformanceHints1952.init(new Context(null));need(PerformanceHints1952.begin(1000)==null,"missing service");
        reset();c=new Context(new Object());PerformanceHints1952.init(c);need(PerformanceHints1952.begin(1000)==null,"wrong service type");
        reset();c=new Context(m);c.failure=new SecurityException("denied");PerformanceHints1952.init(c);need(PerformanceHints1952.begin(1000)==null,"service denied harmless");
        reset();c=new Context(m);c.failure=new OutOfMemoryError("allocation");PerformanceHints1952.init(c);need(PerformanceHints1952.begin(1000)==null,"optional init OOM harmless");
        m=configured();m.unsupported=true;need(PerformanceHints1952.begin(1000)==null && m.sessions.size()==0,"unsupported null session");
        m=configured();m.createFails=true;need(PerformanceHints1952.begin(1000)==null,"session denied");
        m=configured();m.createOom=true;need(PerformanceHints1952.begin(1000)==null,"session OOM harmless");
        m=configured();need(PerformanceHints1952.begin(0)==null && PerformanceHints1952.begin(-1)==null && m.attempts==0,"positive targets only");
        Process.setTid(0);need(PerformanceHints1952.begin(1000)==null && m.attempts==0,"invalid Linux TID skipped");
        PerformanceHints1952.complete(null);PerformanceHints1952.cancel(null);
    }
    private static void oneShot() throws Exception {
        PerformanceHintManager m=configured();PerformanceHints1952.Token token=PerformanceHints1952.begin(5000000L);
        need(token!=null && m.sessions.size()==1,"one-shot created");Session s=m.sessions.get(0);
        need(s.tids.length==1 && s.tids[0]==923 && s.tids[0]!=(int)Thread.currentThread().getId(),"uses Process.myTid, not Java id");
        need(s.target==5000000L,"target forwarded");need(PerformanceHints1952.begin(8000000L)==null && m.sessions.size()==1,"nested one-shot skipped");
        SystemClock.advance(3000123L);PerformanceHints1952.complete(token);
        need(s.reports==1 && s.actual==3000123L && s.closes==1,"nanosecond actual then close");
        PerformanceHints1952.complete(token);PerformanceHints1952.cancel(token);need(s.reports==1 && s.closes==1,"completion idempotent");
        token=PerformanceHints1952.begin(6000000L);SystemClock.advance(2000000L);PerformanceHints1952.cancel(token);
        need(m.sessions.size()==2 && m.sessions.get(1).reports==0 && m.sessions.get(1).closes==1,"abort doesn't train partial work");
        token=PerformanceHints1952.begin(6000000L);PerformanceHints1952.complete(token);
        need(m.sessions.get(2).reports==0 && m.sessions.get(2).closes==1,"zero sample omitted and session closed");
        m=configured();Build.VERSION.SDK_INT=34; // Reinitialize to test pre35 clock selection.
        reset();Build.VERSION.SDK_INT=34;m=new PerformanceHintManager();PerformanceHints1952.init(new Context(m));
        token=PerformanceHints1952.begin(5000000L);SystemClock.advance(5000999L);PerformanceHints1952.complete(token);
        need(m.sessions.get(0).actual==5000000L,"API31-34 uptimeMillis nanoseconds conversion");
    }
    private static void reusable() throws Exception {
        final PerformanceHintManager m=configured();final PerformanceHints1952.Token[] abandoned=new PerformanceHints1952.Token[1];
        PerformanceHints1952.worker(new Runnable(){public void run(){
            PerformanceHints1952.Token a=PerformanceHints1952.begin(10000000L);SystemClock.advance(4000000L);PerformanceHints1952.complete(a);
            need(m.sessions.size()==1 && m.sessions.get(0).closes==0,"worker session kept for learning");
            a=PerformanceHints1952.begin(10000000L);need(PerformanceHints1952.begin(30000000L)==null,"nested worker scope skipped");
            SystemClock.advance(5000000L);PerformanceHints1952.complete(a);
            a=PerformanceHints1952.begin(12000000L);SystemClock.advance(6000000L);PerformanceHints1952.complete(a);
            Session s=m.sessions.get(0);need(s.reports==3 && s.updates==1 && s.target==12000000L && s.actual==6000000L,"repeated tasks reused with actual feedback and target update");
            PerformanceHints1952.worker(new Runnable(){public void run(){
                PerformanceHints1952.Token b=PerformanceHints1952.begin(12000000L);SystemClock.advance(6000000L);PerformanceHints1952.complete(b);
            }}).run();need(s.closes==0 && s.reports==4,"nested wrapper retains outer lifetime");
            a=PerformanceHints1952.begin(12000000L);PerformanceHints1952.cancel(a);
            need(s.closes==1 && s.reports==4,"cancel closes reused session without reporting");
            abandoned[0]=PerformanceHints1952.begin(12000000L);need(m.sessions.size()==2,"later task creates a fresh session");
        }}).run();
        need(m.sessions.get(1).closes==1 && m.sessions.get(1).reports==0,"worker exit closes active scope");
        PerformanceHints1952.complete(abandoned[0]);need(m.sessions.get(1).reports==0 && m.sessions.get(1).closes==1,"late completion after exit ignored");
        PerformanceHints1952.Token normal=PerformanceHints1952.begin(12000000L);SystemClock.advance(3000000L);PerformanceHints1952.complete(normal);
        need(m.sessions.get(2).closes==1,"worker binding removed after exit");
    }
    private static void failures() throws Exception {
        final PerformanceHintManager report=configured();report.reportFails=true;report.closeFails=true;
        PerformanceHints1952.Token token=PerformanceHints1952.begin(2000000L);SystemClock.advance(3000000L);PerformanceHints1952.complete(token);
        need(report.sessions.get(0).closes==1,"report and close failures harmless with close attempted");
        final PerformanceHintManager update=configured();
        PerformanceHints1952.worker(new Runnable(){public void run(){
            PerformanceHints1952.Token a=PerformanceHints1952.begin(10000000L);SystemClock.advance(3000000L);PerformanceHints1952.complete(a);
            update.updateFails=true;need(PerformanceHints1952.begin(12000000L)==null,"update failure skips hints");
            need(update.sessions.get(0).closes==1 && PerformanceHints1952.begin(10000000L)==null && update.attempts==1,"failed worker session closed/disabled without retry loop");
        }}).run();
        final PerformanceHintManager exit=configured();final RuntimeException photoFailure=new RuntimeException("real photo failure");
        try { PerformanceHints1952.worker(new Runnable(){public void run(){PerformanceHints1952.begin(10000000L);throw photoFailure;}}).run();throw new AssertionError("work failure swallowed"); }
        catch(RuntimeException real){need(real==photoFailure,"original delegate failure preserved");}
        need(exit.sessions.get(0).closes==1 && exit.sessions.get(0).reports==0,"failure closes without optimistic feedback");
    }
    private static void adapter() throws Exception {
        final PerformanceHintManager m=configured();final SpeedWorkers1935.Hints hints=SpeedWorkers1935.hints;
        hints.worker(new Runnable(){public void run(){
            Object token=hints.begin(200000000L);SystemClock.advance(10000000L);hints.complete(token,true);
            token=hints.begin(200000000L);SystemClock.advance(10000000L);hints.complete(token,false);
            hints.complete(null,true);hints.complete(new Object(),false);
        }}).run();
        need(m.sessions.size()==1 && m.sessions.get(0).reports==1 && m.sessions.get(0).closes==1,"adapter maps successful vs cancelled CPU work and closes lifetime");
    }
    private static void concurrency() throws Exception {
        final PerformanceHintManager m=configured();final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        Thread[] threads=new Thread[2];
        for(int i=0;i<2;i++){
            final int tid=1200+i;
            threads[i]=new Thread(PerformanceHints1952.worker(new Runnable(){public void run(){
                try {
                    Process.setTid(tid);SystemClock.setNanos(1000000000L);
                    for(int j=0;j<3;j++){PerformanceHints1952.Token a=PerformanceHints1952.begin(7000000L);SystemClock.advance(3000000L);PerformanceHints1952.complete(a);}
                } catch(Throwable error){failure.compareAndSet(null,error);}
            }}));threads[i].start();
        }
        for(Thread t:threads)t.join();if(failure.get()!=null)throw new AssertionError(failure.get());
        need(m.sessions.size()==2,"independent per-worker sessions");
        need(m.sessions.get(0).tids[0]!=m.sessions.get(1).tids[0],"different Linux TIDs never shared");
        for(Session s:m.sessions)need(s.reports==3 && s.closes==1,"each worker reports three cycles then closes");
    }
    public static void main(String[] args)throws Exception {
        unavailable();oneShot();reusable();failures();adapter();concurrency();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"real_worker_tid_checked\":true,\"repeated_work_feedback_checked\":true,\"worker_lifetime_close_checked\":true,\"unsupported_optional_api_checked\":true,\"device_performance_measured\":false}");
    }
}
