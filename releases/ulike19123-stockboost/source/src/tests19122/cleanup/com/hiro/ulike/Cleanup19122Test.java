package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class Cleanup19122Test {
    static int assertions;
    static void check(boolean condition,String message){assertions++;if(!condition)throw new AssertionError(message);}
    static final class Scratch implements SpeedWorkers1935.ScratchMemory {
        long bytes;int trims;boolean fail;
        Scratch(long bytes){this.bytes=bytes;}
        public long retainedBytes(){return bytes;}
        public void trim(){trims++;if(fail)throw new IllegalStateException("owner failure");bytes=0;}
    }
    static Object field(Class<?> type,String name)throws Exception {
        Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(null);
    }
    static void flush()throws Exception {
        ScheduledThreadPoolExecutor executor=(ScheduledThreadPoolExecutor)field(PostSaveCleanup19122.class,"EXECUTOR");
        executor.schedule(new Runnable(){public void run(){}},350,TimeUnit.MILLISECONDS).get(5,TimeUnit.SECONDS);
    }
    static void workerBudget(int configured)throws Exception {
        CapturePolicy19122.workers=configured;
        final int expected=Math.min(configured,Math.max(1,Runtime.getRuntime().availableProcessors()));
        check(SpeedWorkers1935.maxWorkers()==expected,"effective planning budget "+configured);
        final AtomicInteger active=new AtomicInteger(),maximum=new AtomicInteger(),writes=new AtomicInteger();
        final CountDownLatch entered=new CountDownLatch(expected);
        Runnable[] work=new Runnable[12];
        for(int i=0;i<work.length;i++)work[i]=new Runnable(){public void run(){
            int count=active.incrementAndGet();
            for(;;){int old=maximum.get();if(count<=old||maximum.compareAndSet(old,count))break;}
            entered.countDown();
            try{if(!entered.await(5,TimeUnit.SECONDS))throw new AssertionError("underfilled CPU budget");
                Thread.sleep(4);writes.incrementAndGet();
            }catch(InterruptedException e){throw new IllegalStateException(e);}
            finally{active.decrementAndGet();}
        }};
        SpeedWorkers1935.run(work);
        check(writes.get()==12,"every output task completed once");
        check(maximum.get()==expected,"actual CPU concurrency equals policy "+configured);
        check(SpeedWorkers1935.cpuIdle1944(),"weighted permits returned");
    }
    static void arrayRelief()throws Exception {
        SpeedWorkers1935.trim();
        int[] small=SpeedWorkers1935.borrowInts(1024),large=SpeedWorkers1935.borrowInts(1024*1024);
        int[] owned=SpeedWorkers1935.borrowInts(1024*1024);owned[0]=717;
        SpeedWorkers1935.release(small);SpeedWorkers1935.release(large);
        Scratch nativeLarge=new Scratch(8L*1024*1024),nativeSmall=new Scratch(1024*1024);
        SpeedWorkers1935.installScratchMemory1956(nativeLarge);SpeedWorkers1935.installScratchMemory1959(nativeSmall);
        long[] cleaned=SpeedWorkers1935.trimLargeIdle19122();
        check(cleaned[0]==1,"idle cleanup succeeds");
        check(cleaned[1]-cleaned[2]==4L*1024*1024,"only large returned array reference dropped");
        check(SpeedWorkers1935.borrowInts(1024)==small,"small hot cache reused");
        check(owned[0]==717,"exclusively borrowed input untouched");
        check(nativeLarge.trims==1&&nativeLarge.bytes==0,"large installed idle native owner trimmed");
        check(nativeSmall.trims==0&&nativeSmall.bytes==1024*1024,"small native capacity retained");
        check(cleaned[3]-cleaned[4]==8L*1024*1024,"native measured capacity delta recorded");
        boolean lease=SpeedWorkers1935.enterLegacy();
        check(lease&&!SpeedWorkers1935.enterLegacy(),"legacy nested work shares lease");
        check(SpeedWorkers1935.trimLargeIdle19122()[0]==0,"active legacy lease blocks cleanup");
        CapturePolicy19122.workers=1;
        SpeedWorkers1935.leaveLegacy(lease);
        check(SpeedWorkers1935.cpuIdle1944(),"original permit weight restored despite changed policy");
        boolean analysis=SpeedWorkers1935.tryEnterAnalysis1981();
        check(analysis,"idle analysis obtains weighted permit");
        check(!SpeedWorkers1935.tryEnterAnalysis1981(),"nested analysis cannot claim second permit");
        SpeedWorkers1935.leaveAnalysis1981(analysis);
        check(SpeedWorkers1935.cpuIdle1944(),"analysis releases exact weight");
        Scratch activeSingle=new Scratch(1);SpeedWorkers1935.installScratchMemory1978(activeSingle);
        check(SpeedWorkers1935.trimLargeIdle19122()[0]==0&&activeSingle.trims==0,"Single active workspace never revoked");
        SpeedWorkers1935.installScratchMemory1978(null);
        Scratch unknown=new Scratch(-1);SpeedWorkers1935.installScratchMemory1956(unknown);
        long[] unavailable=SpeedWorkers1935.trimLargeIdle19122();
        check(unavailable[0]==2&&unavailable[3]==-1&&unknown.trims==0,"unknown retention is not treated as actual bytes");
        Scratch failing=new Scratch(8L*1024*1024);failing.fail=true;SpeedWorkers1935.installScratchMemory1956(failing);
        check(SpeedWorkers1935.trimLargeIdle19122()[0]==2,"optional trim failure observed");
        check(SpeedWorkers1935.cpuIdle1944(),"optional failure returns every permit");
        SpeedWorkers1935.installScratchMemory1956(null);SpeedWorkers1935.installScratchMemory1959(null);
        SpeedWorkers1935.release(small);SpeedWorkers1935.release(owned);SpeedWorkers1935.trim();
        CapturePolicy19122.workers=2;
    }
    static void coordinator()throws Exception {
        CapturePolicy19122.cleanup=true;SaveQueue1935.idle=false;
        PostSaveCleanup19122.captureStarted19122(1);PostSaveCleanup19122.attachTrace19122(1,101);
        PostSaveCleanup19122.captureStarted19122(1);
        check(PostSaveCleanup19122.snapshot19122()[3]==1,"duplicate admission does not strand terminal ownership");
        PostSaveCleanup19122.queueIdle19122();flush();
        check(GpuNoise1960.calls==0,"no cleanup while queue and terminal notification live");
        PostSaveCleanup19122.terminal19122(1);flush();
        check(GpuNoise1960.calls==0,"UI completion alone cannot bypass queue owner");
        SaveQueue1935.idle=true;PostSaveCleanup19122.queueIdle19122();flush();
        check(GpuNoise1960.calls==1&&PostSaveCleanup19122.snapshot19122()[1]==1,"queue release after UI triggers one cleanup");
        check(Responsiveness19122.lastTrace==101,"cleanup observation keeps exact trace identity");
        PostSaveCleanup19122.queueIdle19122();PostSaveCleanup19122.terminal19122(1);flush();
        check(GpuNoise1960.calls==1,"repeated idle and terminal notifications do not retrigger work");
        PostSaveCleanup19122.captureStarted19122(2);PostSaveCleanup19122.attachTrace19122(2,102);
        PostSaveCleanup19122.queueIdle19122();flush();
        check(GpuNoise1960.calls==1,"queue idle does not bypass outstanding terminal UI");
        PostSaveCleanup19122.terminal19122(2);
        PostSaveCleanup19122.captureStarted19122(3);flush();
        check(GpuNoise1960.calls==1,"new capture invalidates scheduled old cleanup");
        PostSaveCleanup19122.attachTrace19122(3,103);PostSaveCleanup19122.terminal19122(3);flush();
        check(GpuNoise1960.calls==2&&Responsiveness19122.lastTrace==103,"replacement capture cleans after its own terminal");
        CapturePolicy19122.cleanup=false;PostSaveCleanup19122.captureStarted19122(4);PostSaveCleanup19122.attachTrace19122(4,104);
        PostSaveCleanup19122.terminal19122(4);CapturePolicy19122.cleanup=true;flush();
        check(GpuNoise1960.calls==2&&PostSaveCleanup19122.snapshot19122()[4]==4,"skip-cleanup comparison frozen past policy reset");
        check(Responsiveness19122.lastTrace==104,"skipped comparison retains exact trace");
        ShotContext1932.idle=false;PostSaveCleanup19122.captureStarted19122(5);PostSaveCleanup19122.terminal19122(5);flush();
        check(GpuNoise1960.calls==2,"in-flight camera shot blocks cleanup");
        ShotContext1932.idle=true;PostSaveCleanup19122.queueIdle19122();flush();
        check(GpuNoise1960.calls==3,"explicit idle event may admit eligible cleanup later");
        GpuNoise1960.busy=true;GpuNoise1960.retained=16L*1024*1024;
        PostSaveCleanup19122.captureStarted19122(6);PostSaveCleanup19122.terminal19122(6);flush();
        check(PostSaveCleanup19122.snapshot19122()[4]==2,"busy GPU capacity is not reported as fully cleaned");
        GpuNoise1960.busy=false;GpuNoise1960.race=true;PostSaveCleanup19122.captureStarted19122(7);PostSaveCleanup19122.terminal19122(7);flush();
        check(PostSaveCleanup19122.snapshot19122()[3]==1,"GPU owner generation check observes raced next capture");
        check(!PostSaveCleanup19122.isCurrentIdle19122(6),"stale generation cannot claim current idle");
        check(PostSaveCleanup19122.summary19122().contains("実メモリ回収・操作復帰は別計測"),"diagnostic never promises physical reclamation or FPS");
    }
    public static void main(String[] args)throws Exception {
        workerBudget(2);workerBudget(4);arrayRelief();coordinator();
        ((ScheduledThreadPoolExecutor)field(PostSaveCleanup19122.class,"EXECUTOR")).shutdownNow();
        ThreadPoolExecutor workers=(ThreadPoolExecutor)field(SpeedWorkers1935.class,"EXECUTOR");workers.shutdown();
        check(workers.awaitTermination(5,TimeUnit.SECONDS),"all actual workers terminate");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+"}");
    }
}
