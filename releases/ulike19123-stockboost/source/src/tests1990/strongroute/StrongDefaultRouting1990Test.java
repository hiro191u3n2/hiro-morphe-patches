package com.hiro.ulike;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

public final class StrongDefaultRouting1990Test {
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static Object active()throws Exception{return field(GpuStrong1960.class,"active").get(null);}
    static void reset()throws Exception{
        StrongDiagnostics1982Test.reset();GpuStrongDefault1990.owned=true;GpuStrongDefault1990.ready=true;GpuStartup1990.waits=0;GpuStrongDefault1990.optional1994=false;
    }
    static void firstShot()throws Exception{
        reset();StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);
        GpuStrong1960.Route1981 r=d.route();
        check(GpuStartup1990.waits==1,"core readiness considered once before stage");
        check(r!=null&&r.startup1990&&r.profile==4&&r.variant==0&&!r.rawPolicy(),"first shot uses fixed direct policy route");
        check(r.cpu==0&&r.gpu==0&&GpuQualification1961.restore(r.key)==null,"finite corpus never fabricates a shape or speed certificate");
        check(d.process(r),"first-shot GPU dispatch completes");GpuStrong1960.endStage(d.model);d.exact();
        check(d.calls==0&&ProcessingTiming1947.gpu==1&&ProcessingTiming1947.cpu==0,"only actual GPU output counted, no CPU shadow");
        check(GpuStrongTuning1975.schedules==0,"successful default does not queue photograph tuning");
        check(GpuNoise1960.leases==0&&GpuNoise1960.active==0,"all ownership drained");
    }
    static void rejectAndRevoke()throws Exception{
        reset();StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);
        GpuQualification1961.rejectExact1971(d.key(4),"argb_mismatch");GpuStrong1960.Route1981 r=d.route();
        check(r.profile<0,"existing unchanged-program mismatch blocks default");check(d.process(r),"rejected default falls back to CPU");
        GpuStrong1960.endStage(d.model);d.exact();check(d.calls==1&&ProcessingTiming1947.gpu==0,"failed evidence never counted as GPU");
        check(GpuQualification1961.exactRejected(d.key(4)),"mismatch record remains sticky");
        reset();d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);r=d.route();GpuStrongDefault1990.ready=false;
        check(d.process(r),"revocation after route decision selects CPU");GpuStrong1960.endStage(d.model);d.exact();
        check(d.calls==1&&GpuNoise1960.submits==0,"stale route cannot submit after evidence revocation");
        reset();d=new StrongDiagnostics1982Test.Data();GpuStrongDefault1990.ready=false;GpuStrong1960.beginStage(d.model);r=d.route();
        check(r.profile<0,"unfinished startup remains CPU");check(d.process(r),"unfinished startup has safe CPU path");GpuStrong1960.endStage(d.model);
    }
    static void busyBanksWait()throws Exception{
        reset();StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);
        final Object stage=active();final boolean[] banks=(boolean[])field(stage.getClass(),"banks").get(stage);
        synchronized(stage){banks[0]=banks[1]=true;}
        Thread release=new Thread(new Runnable(){public void run(){try{Thread.sleep(90);}catch(InterruptedException e){throw new AssertionError(e);}synchronized(stage){banks[0]=false;stage.notifyAll();}}});release.start();
        long start=System.nanoTime();check(d.process(d.route()),"busy-bank default eventually dispatches");long elapsed=System.nanoTime()-start;release.join();
        synchronized(stage){banks[1]=false;}
        GpuStrong1960.endStage(d.model);d.exact();
        check(elapsed>=60000000L,"test actually waited beyond legacy 50 ms routing budget");
        check(d.calls==0&&ProcessingTiming1947.gpu==1,"mere bank occupancy never forces a default strip to CPU");
        check(GpuNoise1960.leases==0&&GpuNoise1960.active==0,"waiter releases ownership");
    }
    static void cancellation()throws Exception{
        reset();final StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);
        final Object stage=active();final boolean[] banks=(boolean[])field(stage.getClass(),"banks").get(stage);
        synchronized(stage){banks[0]=banks[1]=true;}
        final Throwable[] failure=new Throwable[1];final boolean[] interrupted=new boolean[1];
        Thread worker=new Thread(new Runnable(){public void run(){try{d.process(d.route());}catch(Throwable t){failure[0]=t;interrupted[0]=Thread.currentThread().isInterrupted();}}});worker.start();
        long deadline=System.nanoTime()+1000000000L;
        while(System.nanoTime()<deadline){synchronized(stage){if(!((Deque<?>)field(stage.getClass(),"preferredWaiters").get(stage)).isEmpty())break;}Thread.yield();}
        worker.interrupt();worker.join(2000);
        check(!worker.isAlive()&&failure[0] instanceof CancellationException&&interrupted[0],"cancelled bank waiter preserves interrupt and exits");
        synchronized(stage){check(((Deque<?>)field(stage.getClass(),"preferredWaiters").get(stage)).isEmpty(),"cancelled waiter removed from FIFO");banks[0]=banks[1]=false;}
        GpuStrong1960.endStage(d.model);check(GpuNoise1960.submits==0&&d.calls==0,"cancelled photo commits neither GPU nor CPU pixels");
    }
    public static void main(String[] args)throws Exception{
        firstShot();rejectAndRevoke();busyBanksWait();cancellation();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"physical_android_tested\":false,\"actual_jni\":false}");
    }
}
