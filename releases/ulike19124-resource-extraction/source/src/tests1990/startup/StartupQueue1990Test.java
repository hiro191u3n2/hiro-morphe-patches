package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
/** Real qualification scheduler, controlled probes, no GPU performance claim. */
public final class StartupQueue1990Test {
    static int assertions;
    static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    static void age()throws Exception{Field f=GpuQualification1961.class.getDeclaredField("lastCapture");f.setAccessible(true);f.setLong(null,System.nanoTime()-3000000000L);}
    public static void main(String[] args)throws Exception{
        GpuQualification1961.initialize(new Context());SaveQueue1935.idle=false;
        check(!GpuQualification1961.optionalStrongBenchmarksAllowed19117()&&!GpuQualification1961.optionalBenchmarksAllowed1995(),"cold installation admits no optional proof before core readiness");
        GpuQualification1961.enablePreFirstStrongIdle19117();
        check(GpuQualification1961.optionalStrongBenchmarksAllowed19117()&&!GpuQualification1961.optionalBenchmarksAllowed1995(),"idle Strong proof alone enabled before publication");
        AtomicInteger preFirstClosed=new AtomicInteger(),preFirstRuns=new AtomicInteger();
        check(GpuQualification1961.schedule("strong-standard-layout1994-v1",512,new GpuQualification1961.Probe(){
            public void run(GpuQualification1961.Cancellation c){preFirstRuns.incrementAndGet();GpuQualification1961.qualified("pre-first-tile8",100,50,0);}
            public void close(){preFirstClosed.incrementAndGet();}
        }),"synthetic tile8 can be queued during the idle window");
        GpuQualification1961.captureChanged();
        check(preFirstClosed.get()==1&&preFirstRuns.get()==0&&GpuQualification1961.restore("pre-first-tile8")==null,
                "shutter detaches pending tile8 exactly once with no proof");
        check(!GpuQualification1961.optionalStrongBenchmarksAllowed19117(),"first shutter closes optional Strong proof window");
        GpuQualification1961.enablePreFirstStrongIdle19117();
        check(!GpuQualification1961.optionalStrongBenchmarksAllowed19117(),"stale startup callback cannot reopen first-shot window");
        CountDownLatch started=new CountDownLatch(1),cancelled=new CountDownLatch(1),queuedClosed=new CountDownLatch(1);
        AtomicInteger activeClosed=new AtomicInteger(),queuedRuns=new AtomicInteger();
        GpuQualification1961.Probe active=new GpuQualification1961.Probe(){
            public void run(GpuQualification1961.Cancellation c){started.countDown();
                while(!c.cancelled())try{Thread.sleep(2);}catch(InterruptedException expected){}
                GpuQualification1961.qualified("startup-active",100,50,0);
            }
            public void close(){activeClosed.incrementAndGet();cancelled.countDown();}
        };
        GpuQualification1961.Probe queued=new GpuQualification1961.Probe(){
            public void run(GpuQualification1961.Cancellation c){queuedRuns.incrementAndGet();GpuQualification1961.qualified("startup-queued",100,50,0);}
            public void close(){queuedClosed.countDown();}
        };
        check(GpuQualification1961.schedule("startup-active",10,active),"old optional probe queued");
        check(GpuQualification1961.schedule("startup-queued",20,queued),"second optional probe queued");
        SaveQueue1935.idle=true;age();GpuQualification1961.wake();
        check(started.await(2,TimeUnit.SECONDS),"first optional probe really running");
        GpuQualification1961.startupPause1990(true);
        check(cancelled.await(2,TimeUnit.SECONDS),"running optional owner drains cooperatively");
        check(activeClosed.get()==1,"active snapshot closed exactly once");
        check(!queuedClosed.await(30,TimeUnit.MILLISECONDS)&&queuedRuns.get()==0,"queued snapshot neither runs nor closes during startup");
        check(GpuQualification1961.restore("startup-active")==null,"cancelled running probe cannot commit proof");
        check(GpuQualification1961.maySchedule("startup-active"),"startup cancellation consumes no failure retry");
        GpuQualification1961.startupPause1990(false);
        check(queuedClosed.await(2,TimeUnit.SECONDS),"queued work resumes after startup finally");
        check(queuedRuns.get()==1&&GpuQualification1961.restore("startup-queued")!=null,"resumed owner runs once and commits its own proof");
        GpuQualification1961.rejectExact("sticky-mismatch");
        GpuQualification1961.startupPause1990(true);GpuQualification1961.startupPause1990(false);
        check(GpuQualification1961.exactRejected("sticky-mismatch")&&!GpuQualification1961.maySchedule("sticky-mismatch"),"startup pause never removes real pixel rejection");
        System.out.println("{\"scenario\":\"qualification_pause\",\"status\":\"passed\",\"assertions\":"+assertions+"}");
    }
}
