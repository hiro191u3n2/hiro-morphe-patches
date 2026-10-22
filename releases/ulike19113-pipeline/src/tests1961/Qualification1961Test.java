package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** The actual qualification service with conforming staged preference writes.
 * Fault/capture/queue scheduling boundaries are controlled; GPU pixel parity is
 * tested separately by the actual production shader suites. */
public final class Qualification1961Test {
    private static long assertions;
    private static void check(boolean yes,String label){assertions++;if(!yes)throw new AssertionError(label);}
    private static void await(CountDownLatch latch,String label)throws Exception{check(latch.await(3,TimeUnit.SECONDS),label);}
    private static Object value(String name)throws Exception {Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
    private static void clearCache()throws Exception {Object lock=value("LOCK");synchronized(lock){((Map<?,?>)value("RECORDS")).clear();}}
    private static void quiet()throws Exception {Field f=GpuQualification1961.class.getDeclaredField("lastCapture");f.setAccessible(true);Object lock=value("LOCK");synchronized(lock){f.setLong(null,System.nanoTime()-3000000000L);}GpuQualification1961.wake();}
    private static void drained()throws Exception {Object lock=value("LOCK");long deadline=System.nanoTime()+3000000000L;synchronized(lock){while(GpuQualification1961.retainedBytes()!=0&&System.nanoTime()<deadline)lock.wait(50);}check(GpuQualification1961.retainedBytes()==0,"all probe memory released");}
    public static final class MemoryPreferences implements SharedPreferences {
        final LinkedHashMap<String,Object> values=new LinkedHashMap<String,Object>();
        volatile Runnable beforeGetAll;volatile boolean failRead,failApply;
        public Map<String,?> getAll(){Map<String,Object> snapshot;synchronized(values){snapshot=new LinkedHashMap<String,Object>(values);}Runnable hook=beforeGetAll;if(hook!=null)hook.run();return snapshot;}
        public String getString(String key,String fallback){if(failRead)throw new IllegalStateException("read");synchronized(values){Object v=values.get(key);if(v==null)return fallback;if(!(v instanceof String))throw new ClassCastException("corrupt preference type");return (String)v;}}
        public Editor edit(){return new Editor(){final LinkedHashMap<String,Object> staged=new LinkedHashMap<String,Object>();
            public Editor remove(String key){staged.put(key,null);return this;}public Editor putString(String key,String v){staged.put(key,v);return this;}
            public void apply(){if(failApply)throw new IllegalStateException("write");synchronized(values){for(Map.Entry<String,Object> e:staged.entrySet()){if(e.getValue()==null)values.remove(e.getKey());else values.put(e.getKey(),e.getValue());}}}
        };}
        void clear(){synchronized(values){values.clear();}}
        int size(){synchronized(values){return values.size();}}
    }
    private static Context context(){Context c=new Context();c.prefs=new MemoryPreferences();return c;}
    private static void certificates()throws Exception {
        Context c=context();GpuQualification1961.initialize(c);
        GpuQualification1961.qualified("good",100000,90000,2);
        GpuQualification1961.Record proof=GpuQualification1961.restore("good");check(proof!=null&&proof.variant==2&&proof.cpuNanos==100000&&proof.gpuNanos==90000,"complete certificate");
        clearCache();check(GpuQualification1961.restore("good")!=null,"persistent restoration");
        for(int variant:new int[]{-1,3})GpuQualification1961.qualified("badvariant"+variant,100000,90000,variant);
        GpuQualification1961.qualified("slow",100000,96000,0);GpuQualification1961.qualified("zero",0,1,0);
        check(GpuQualification1961.restore("slow")==null&&GpuQualification1961.restore("zero")==null&&GpuQualification1961.restore("badvariant-1")==null,"unqualified timings and variants rejected");
        GpuQualification1961.qualified("boundary",100000,95000,0);check(GpuQualification1961.restore("boundary")!=null,"inclusive 5 percent margin");
        String original=GpuNoise1960.identity;GpuNoise1960.identity+="-driver-change";
        check(GpuQualification1961.restore("good")==null,"driver shader identity invalidates");GpuNoise1960.identity=original;
        String build=Build.FINGERPRINT;Build.FINGERPRINT+="-update";GpuQualification1961.initialize(c);check(GpuQualification1961.restore("good")==null,"OS build invalidates");Build.FINGERPRINT=build;GpuQualification1961.initialize(c);
        c.versionName="fixture-2";GpuQualification1961.initialize(c);check(GpuQualification1961.restore("good")==null,"APK version invalidates");c.versionName="fixture-1";GpuQualification1961.initialize(c);
        String key; synchronized(c.prefs.values){key=c.prefs.values.keySet().iterator().next();Object raw=c.prefs.values.get(key);c.prefs.values.put(key,Long.valueOf(1));clearCache();check(GpuQualification1961.restore("good")==null,"corrupt type fails closed");c.prefs.values.put(key,raw);}
        c.prefs.failRead=true;clearCache();check(GpuQualification1961.restore("good")==null,"preference failure is optional");c.prefs.failRead=false;
        GpuQualification1961.reject("good");check(GpuQualification1961.restore("good")==null,"rejected proof removed");
        clearCache();c.prefs.clear();for(int i=0;i<80;i++)GpuQualification1961.qualified("bounded"+i,100000,90000,i%3);
        check(c.prefs.size()==64&&((Map<?,?>)value("RECORDS")).size()==64,"disk and heap certificate bound");
        // Disk entry corruption with intact digest must never become an admission.
        synchronized(c.prefs.values){key=c.prefs.values.keySet().iterator().next();String raw=(String)c.prefs.values.get(key);c.prefs.values.put(key,raw.replace("100000:90000","100000:99000"));}
        clearCache();check(GpuQualification1961.restore("bounded16")==null,"modified evidence fails closed");
    }
    private static void concurrentCertificateBound()throws Exception {
        final Context c=context();GpuQualification1961.initialize(c);clearCache();
        for(int i=0;i<63;i++)GpuQualification1961.qualified("prior"+i,100000,90000,i%3);
        final CountDownLatch readers=new CountDownLatch(2),start=new CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicReference<Throwable> failed=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        c.prefs.beforeGetAll=new Runnable(){public void run(){readers.countDown();try{readers.await(200,TimeUnit.MILLISECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}};
        Thread[] threads=new Thread[2];
        for(int i=0;i<2;i++){final int index=i;threads[i]=new Thread(new Runnable(){public void run(){try{start.await();GpuQualification1961.qualified("concurrent"+index,100000,90000,index);}catch(Throwable failure){failed.set(failure);}}});threads[i].start();}
        start.countDown();for(Thread thread:threads){thread.join(3000);check(!thread.isAlive(),"concurrent certificate writer drained");}
        c.prefs.beforeGetAll=null;check(failed.get()==null,"concurrent storage succeeds");
        check(c.prefs.size()==64&&((Map<?,?>)value("RECORDS")).size()==64,"simultaneous certificate writes preserve disk and heap bounds");
        clearCache();check(GpuQualification1961.restore("concurrent0")!=null&&GpuQualification1961.restore("concurrent1")!=null,"both serialized proofs are independently restorable");
    }
    private static void cancellation()throws Exception {
        final Context c=context();GpuQualification1961.initialize(c);clearCache();
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),closed=new CountDownLatch(1);
        final AtomicInteger run=new AtomicInteger(),closes=new AtomicInteger();
        GpuQualification1961.Probe probe=new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation token){
            run.incrementAndGet();check(GpuQualification1961.background(),"background thread identity");entered.countDown();
            try{release.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}
            check(token.cancelled(),"new capture cancels running proof");GpuQualification1961.qualified("cancelled",100000,80000,0);
        }public void close(){closes.incrementAndGet();closed.countDown();}};
        SaveQueue1935.idle=false;check(GpuQualification1961.schedule("cancelled",4096,probe),"bounded job admitted");check(GpuQualification1961.retainedBytes()==4096,"pending retention accounted");
        quiet();check(!entered.await(100,TimeUnit.MILLISECONDS),"active save blocks idle probe");SaveQueue1935.idle=true;GpuNoise1960.busy=true;GpuQualification1961.wake();check(!entered.await(100,TimeUnit.MILLISECONDS),"active GPU blocks idle probe");
        GpuNoise1960.busy=false;quiet();await(entered,"idle proof starts");ProcessingTiming1947.capture();release.countDown();await(closed,"cancelled probe closes");drained();
        check(run.get()==1&&closes.get()==1&&GpuQualification1961.restore("cancelled")==null&&c.prefs.size()==0,"cancelled evidence never certified");
        final AtomicInteger rejectedClose=new AtomicInteger();GpuQualification1961.Probe rejected=new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation x){throw new AssertionError("must not run");}public void close(){rejectedClose.incrementAndGet();}};
        check(!GpuQualification1961.schedule("huge",96L*1024*1024+1,rejected)&&rejectedClose.get()==1,"overbudget ownership returned once");
        Thread.currentThread().interrupt();try{check(!GpuQualification1961.schedule("interrupt",1,rejected)&&Thread.currentThread().isInterrupted(),"interrupted scheduling declines");}finally{Thread.interrupted();}
    }
    private static void cancelledBeforePersistence()throws Exception {
        final Context c=context();GpuQualification1961.initialize(c);clearCache();
        final CountDownLatch reading=new CountDownLatch(1),release=new CountDownLatch(1),closed=new CountDownLatch(1);
        c.prefs.beforeGetAll=new Runnable(){public void run(){reading.countDown();try{release.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}};
        check(GpuQualification1961.schedule("race",1024,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation token){GpuQualification1961.qualified("race",100000,80000,0);}public void close(){closed.countDown();}}),"race job admitted");
        quiet();await(reading,"certificate storage read in progress");ProcessingTiming1947.capture();release.countDown();await(closed,"race probe closed");drained();c.prefs.beforeGetAll=null;
        check(c.prefs.size()==0&&GpuQualification1961.restore("race")==null,"capture during storage preparation suppresses commit");
    }
    private static void cpuInterruptionAndReuse()throws Exception {
        final CountDownLatch started=new CountDownLatch(1),closed=new CountDownLatch(1),nextClosed=new CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicBoolean observed=new java.util.concurrent.atomic.AtomicBoolean();
        check(GpuQualification1961.schedule("cpu-interrupt",1024,new GpuQualification1961.Probe(){
            public void run(GpuQualification1961.Cancellation token){
                started.countDown();long deadline=System.nanoTime()+3000000000L;
                while(System.nanoTime()<deadline) {
                    if(Thread.currentThread().isInterrupted()){observed.set(true);break;}
                    Thread.yield();
                }
                check(token.cancelled(),"CPU probe sees capture cancellation");
            }
            public void close(){closed.countDown();}
        }),"CPU work probe admitted");
        quiet();await(started,"CPU work starts");ProcessingTiming1947.capture();await(closed,"CPU work interrupted and closed");drained();
        check(observed.get(),"new capture interrupts running CPU oracle");
        check(GpuQualification1961.schedule("after-cpu-interrupt",1024,new GpuQualification1961.Probe(){
            public void run(GpuQualification1961.Cancellation token){check(!Thread.currentThread().isInterrupted()&&!token.cancelled(),"next probe has clean interruption state");}
            public void close(){nextClosed.countDown();}
        }),"next job accepted after CPU interruption");quiet();await(nextClosed,"next job runs after interrupted oracle");drained();
    }
    private static void failureRecovery()throws Exception {
        final CountDownLatch failedClosed=new CountDownLatch(1);
        check(GpuQualification1961.schedule("assertion",1024,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation x){throw new AssertionError("test probe failure");}public void close(){failedClosed.countDown();throw new AssertionError("close failure");}}),"failure probe admitted");quiet();await(failedClosed,"failure closes");drained();
        final CountDownLatch recovered=new CountDownLatch(1);check(GpuQualification1961.schedule("recovery",1024,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation x){check(!x.cancelled(),"recovery live");}public void close(){recovered.countDown();}}),"next probe accepted after failure");quiet();await(recovered,"worker survives run and close assertion");drained();
    }
    public static void main(String[] args)throws Exception {certificates();concurrentCertificateBound();cancellation();cancelledBeforePersistence();cpuInterruptionAndReuse();failureRecovery();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"scope\":\"production qualification, controlled capture/queue/persistence faults\"}");}
}
