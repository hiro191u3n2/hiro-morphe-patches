package com.hiro.ulike;

import java.util.concurrent.atomic.AtomicInteger;

public final class BackgroundNoAndroid19121Test {
    public static void main(String[] args)throws Exception {
        final AtomicInteger runs=new AtomicInteger();
        Thread worker=new Thread(SpeedWorkers1935.backgroundWorker19121(new Runnable(){public void run(){runs.incrementAndGet();}}));
        worker.start();worker.join(5000);
        if(worker.isAlive() || runs.get()!=1)throw new AssertionError("missing Android API cannot drop or duplicate work");
        if(SpeedWorkers1935.backgroundPriorityApplied19121()!=0 || SpeedWorkers1935.backgroundPriorityFailed19121()!=1)
            throw new AssertionError("missing Android API counted as unavailable");
        System.out.println("{\"status\":\"passed\",\"assertions\":2,\"android_free_worker_abi\":true}");
    }
}
