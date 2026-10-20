package com.hiro.ulike;

import android.content.Context;
import android.os.Build;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Production scheduler and rejection store; the clock and capture/save states
 * are controlled. This verifies retry policy, not physical GPU performance. */
public final class Gx29Qualification1962Test {
    private static long assertions;
    private static void check(boolean yes,String label){assertions++;if(!yes)throw new AssertionError(label);}
    private static Object value(String name)throws Exception{Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
    private static Object failure(String key)throws Exception{
        Method env=GpuQualification1961.class.getDeclaredMethod("environment");env.setAccessible(true);
        Method f=GpuQualification1961.class.getDeclaredMethod("failure",String.class,String.class);f.setAccessible(true);
        synchronized(value("LOCK")){return f.invoke(null,key,(String)env.invoke(null));}
    }
    private static void due(String key)throws Exception{Object f=failure(key);check(f!=null,"speed failure stored");Field stamp=f.getClass().getDeclaredField("retryAfter");stamp.setAccessible(true);synchronized(value("LOCK")){stamp.setLong(f,System.nanoTime()-1);}}
    private static int retries(String key)throws Exception{Object f=failure(key);Field count=f.getClass().getDeclaredField("retries");count.setAccessible(true);synchronized(value("LOCK")){return count.getInt(f);}}
    private static void quiet()throws Exception{Field f=GpuQualification1961.class.getDeclaredField("lastCapture");f.setAccessible(true);synchronized(value("LOCK")){f.setLong(null,System.nanoTime()-3000000000L);}GpuQualification1961.wake();}
    private static void await(CountDownLatch latch,String label)throws Exception{check(latch.await(3,TimeUnit.SECONDS),label);}
    private static void drained()throws Exception{Object lock=value("LOCK");long deadline=System.nanoTime()+3000000000L;synchronized(lock){while(GpuQualification1961.retainedBytes()!=0&&System.nanoTime()<deadline)lock.wait(20);}check(GpuQualification1961.retainedBytes()==0,"probe snapshot closed and released");}
    private static Context context(String build){Build.FINGERPRINT=build;Context c=new Context();c.prefs=new Qualification1961Test.MemoryPreferences();GpuQualification1961.initialize(c);return c;}
    private static void speedRecovery()throws Exception{
        context("gx1962-speed-fixture");final String key="speed-only";
        GpuQualification1961.qualified(key,100000,90000,0);check(GpuQualification1961.restore(key)!=null,"qualified baseline restored");
        GpuQualification1961.rejectSpeed(key);check(!GpuQualification1961.exactRejected(key),"timing rejection is not a pixel failure");
        check(GpuQualification1961.restore(key)==null&&!GpuQualification1961.maySchedule(key),"timing failure invalidates proof and imposes cooldown");
        check(GpuQualification1961.retainedBytes()==0&&retries(key)==0,"cooldown retains no photograph and consumes no retry");
        AtomicInteger close=new AtomicInteger();check(!GpuQualification1961.schedule(key,1,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){throw new AssertionError("cooldown cannot run");}public void close(){close.incrementAndGet();}})&&close.get()==1,"cooldown declines and returns probe ownership once");
        due(key);check(GpuQualification1961.maySchedule(key),"cooldown expiration permits a new detached proof");
        final CountDownLatch closed=new CountDownLatch(1);SaveQueue1935.idle=false;
        check(GpuQualification1961.schedule(key,4096,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){
            check(GpuQualification1961.background()&&SaveQueue1935.idle&&!c.cancelled(),"retry uses idle worker only");
            // Two full-output trials are owned by the caller; success still must
            // pass the unchanged inclusive timing margin enforced by service.
            GpuQualification1961.qualified(key,100000,96000,0);check(GpuQualification1961.restore(key)==null,"retry cannot relax five-percent admission");
            GpuQualification1961.qualified(key,100000,95000,2);
        }public void close(){closed.countDown();}}),"bounded retry scheduled");
        quiet();check(!closed.await(100,TimeUnit.MILLISECONDS),"saving blocks speed retry");SaveQueue1935.idle=true;quiet();await(closed,"idle retry completes");drained();
        GpuQualification1961.Record restored=GpuQualification1961.restore(key);check(restored!=null&&restored.variant==2&&GpuQualification1961.maySchedule(key),"successful complete requalification restores route");
    }
    private static void boundedRetries()throws Exception{
        final String key="persistently-slow";GpuQualification1961.rejectSpeed(key);
        for(int i=0;i<3;i++){
            due(key);final CountDownLatch closed=new CountDownLatch(1);
            check(GpuQualification1961.schedule(key,1024,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){GpuQualification1961.rejectSpeed(key);GpuQualification1961.rejectSpeed(key);}public void close(){closed.countDown();}}),"retry within fixed bound");
            quiet();await(closed,"failed timing retry completes");drained();check(retries(key)==i+1,"one failed retry counted once despite repeated reports");
        }
        due(key);check(!GpuQualification1961.maySchedule(key)&&GpuQualification1961.restore(key)==null&&!GpuQualification1961.exactRejected(key),"three failed idle retries stop without declaring pixel mismatch");
    }
    private static void cancelledRetry()throws Exception{
        final String key="cancel-speed";GpuQualification1961.rejectSpeed(key);due(key);
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),closed=new CountDownLatch(1);
        check(GpuQualification1961.schedule(key,2048,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){entered.countDown();try{release.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}check(c.cancelled(),"capture cancels retry");GpuQualification1961.rejectSpeed(key);GpuQualification1961.rejectExact(key);GpuQualification1961.qualified(key,100000,80000,0);}public void close(){closed.countDown();}}),"cancelled retry queued");
        quiet();await(entered,"retry started");ProcessingTiming1947.capture();release.countDown();await(closed,"cancelled retry closes");drained();
        check(retries(key)==0&&GpuQualification1961.maySchedule(key)&&!GpuQualification1961.exactRejected(key)&&GpuQualification1961.restore(key)==null,"cancelled retry cannot spend budget, persist pixel rejection, or certify");
    }
    private static void cancelledNegativePersistence()throws Exception{
        final Context c=context("gx1962-negative-race-fixture");final String key="negative-race";
        final CountDownLatch reading=new CountDownLatch(1),release=new CountDownLatch(1),closed=new CountDownLatch(1);
        c.prefs.beforeGetAll=new Runnable(){public void run(){reading.countDown();try{release.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}};
        check(GpuQualification1961.schedule(key,1024,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){GpuQualification1961.rejectExact(key);}public void close(){closed.countDown();}}),"negative-storage-race probe accepted");
        quiet();await(reading,"negative persistence preparation starts");ProcessingTiming1947.capture();release.countDown();await(closed,"cancelled negative writer closes");drained();c.prefs.beforeGetAll=null;
        check(c.prefs.getAll().isEmpty()&&!GpuQualification1961.exactRejected(key)&&GpuQualification1961.maySchedule(key),"capture during negative storage preparation suppresses heap and disk rejection");
        final String changed="environment-changed-during-proof";final String old=GpuNoise1960.identity;final CountDownLatch done=new CountDownLatch(1);
        check(GpuQualification1961.schedule(changed,1024,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){GpuNoise1960.identity=old+"-changed";GpuQualification1961.qualified(changed,100000,80000,0);GpuQualification1961.rejectExact(changed);}public void close(){done.countDown();}}),"environment-change probe accepted");
        quiet();await(done,"environment-change probe closes");drained();
        check(GpuQualification1961.restore(changed)==null&&!GpuQualification1961.exactRejected(changed),"old-driver work cannot certify or reject changed driver");GpuNoise1960.identity=old;
        check(GpuQualification1961.restore(changed)==null&&!GpuQualification1961.exactRejected(changed)&&c.prefs.getAll().isEmpty(),"changed-environment proof leaves no state in old environment");
    }
    private static void exactPersistenceAndCapacity()throws Exception{
        Context c=context("gx1962-exact-fixture");String key="wrong-pixel";
        GpuQualification1961.qualified(key,100000,90000,0);GpuQualification1961.rejectExact(key);
        check(GpuQualification1961.exactRejected(key)&&!GpuQualification1961.maySchedule(key)&&GpuQualification1961.restore(key)==null,"pixel failure permanent for environment");
        synchronized(value("LOCK")){((Map<?,?>)value("FAILURES")).clear();}
        check(GpuQualification1961.exactRejected(key),"pixel rejection restores from private preferences");
        GpuQualification1961.qualified(key,100000,80000,0);check(GpuQualification1961.restore(key)==null,"direct certificate cannot overwrite known pixel failure");
        String old=GpuNoise1960.identity;GpuNoise1960.identity+="-new-driver";check(GpuQualification1961.maySchedule(key)&&!GpuQualification1961.exactRejected(key),"changed driver permits fresh full proof");GpuNoise1960.identity=old;
        for(int i=0;i<80;i++)GpuQualification1961.rejectExact("pixel-fault"+i);
        int negatives=0;for(String stored:c.prefs.getAll().keySet())if(stored.startsWith("reject-")&&!stored.startsWith("reject-all-"))negatives++;
        check(negatives==64&&((Map<?,?>)value("FAILURES")).size()<=64,"persistent and heap pixel-failure records bounded");
        check(!GpuQualification1961.maySchedule("brand-new-config")&&GpuQualification1961.exactRejected("wrong-pixel"),"overflow fails closed instead of evicting a proven pixel failure");
    }
    public static void main(String[] args)throws Exception{speedRecovery();boundedRetries();cancelledRetry();cancelledNegativePersistence();exactPersistenceAndCapacity();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"speedRetryCooldownSeconds\":30,\"maxFailedIdleRetries\":3,\"pixelMismatchPersistent\":true,\"cancelledRetryConsumesBudget\":false,\"physicalAndroidTested\":false}");}
}
