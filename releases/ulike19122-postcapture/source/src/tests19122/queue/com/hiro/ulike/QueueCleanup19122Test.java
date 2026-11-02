package com.hiro.ulike;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
public final class QueueCleanup19122Test {
    static int assertions;
    static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    static void flush()throws Exception {
        Field f=PostSaveCleanup19122.class.getDeclaredField("EXECUTOR");f.setAccessible(true);
        ((ScheduledThreadPoolExecutor)f.get(null)).schedule(new Runnable(){public void run(){}},350,TimeUnit.MILLISECONDS).get(5,TimeUnit.SECONDS);
    }
    public static void main(String[] args)throws Exception {
        SaveQueue1935.reserve();PostSaveCleanup19122.captureStarted19122(1);PostSaveCleanup19122.attachTrace19122(1,101);
        PostSaveCleanup19122.terminal19122(1);flush();
        check(GpuNoise1960.calls==0&&!SaveQueue1935.idle1953(),"failed preparation callback cannot trim reserved image");
        SaveQueue1935.release();flush();
        check(SaveQueue1935.idle1953()&&GpuNoise1960.calls==1,"release event cleans failed preparation after earlier UI callback");
        SaveQueue1935.reserve();PostSaveCleanup19122.captureStarted19122(2);
        final CountDownLatch run=new CountDownLatch(1);
        SaveQueue1935.submit(new Runnable(){public void run(){PostSaveCleanup19122.terminal19122(2);SaveQueue1935.release();run.countDown();}});
        check(run.await(5,TimeUnit.SECONDS),"actual save worker ran");flush();
        check(SaveQueue1935.idle1953()&&GpuNoise1960.calls==2,"finishStages event cleans after both receipt and all queue owners");
        SaveQueue1935.reserve();PostSaveCleanup19122.captureStarted19122(3);
        final CountDownLatch done=new CountDownLatch(1);
        SaveQueue1935.submit(new Runnable(){public void run(){SaveQueue1935.release();done.countDown();}});
        check(done.await(5,TimeUnit.SECONDS),"reverse-order save ran");flush();
        check(SaveQueue1935.idle1953()&&GpuNoise1960.calls==2,"released queue cannot skip outstanding terminal callback");
        PostSaveCleanup19122.terminal19122(3);flush();
        check(GpuNoise1960.calls==3,"later UI terminal completes reverse-order cleanup gate");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+"}");
    }
}
