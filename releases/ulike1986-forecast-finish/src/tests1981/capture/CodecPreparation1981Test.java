package com.hiro.ulike;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Execute public queue transitions and the real constructor/storage state machine. */
public final class CodecPreparation1981Test {
    static int assertions;
    static synchronized void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    static void await(CountDownLatch latch,String reason)throws Exception{check(latch.await(4,TimeUnit.SECONDS),reason);}
    static ExecutorService executor(){return Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"codec-host1981");t.setDaemon(true);return t;});}
    static void waitIdle()throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);while(!SaveQueue1935.idle1953()&&System.nanoTime()<until)Thread.yield();check(SaveQueue1935.idle1953(),"queue idle after terminal cleanup");}
    static void onceAndRejection()throws Exception {
        ExecutorService worker=executor();AtomicInteger count=new AtomicInteger();
        CodecPreparation1981 prep=new CodecPreparation1981(worker,()->count.incrementAndGet());
        Thread[] calls=new Thread[24];CountDownLatch start=new CountDownLatch(1);
        for(int i=0;i<calls.length;i++){calls[i]=new Thread(()->{try{start.await();prep.start();}catch(InterruptedException e){throw new AssertionError(e);}});calls[i].start();}
        start.countDown();for(Thread thread:calls)thread.join(2000);
        prep.closeAndAwait();check(count.get()==1,"hardware event/encoding concurrent starts submit once");check(prep.idle(),"completed constructor is idle");
        prep.start();check(count.get()==1,"closed task cannot restart");
        CodecPreparation1981 cancelled=new CodecPreparation1981(worker,()->count.incrementAndGet());cancelled.closeAndAwait();cancelled.start();
        check(count.get()==1,"undispatched cancelled constructor never runs");
        worker.shutdown();RejectedExecutionException failure=null;
        CodecPreparation1981 rejected=new CodecPreparation1981(worker,()->count.incrementAndGet());rejected.start();
        try{rejected.throwIfFailed();throw new AssertionError("missing dispatch rejection");}catch(RejectedExecutionException expected){failure=expected;}
        check(failure!=null,"dispatch failure belongs to target photo");rejected.closeAndAwait();check(rejected.idle(),"rejection drains completion latch");
        try{rejected.throwIfFailed();throw new AssertionError("missing retained rejection");}catch(RejectedExecutionException same){check(same==failure,"same dispatch exception retained");}
    }
    static void storageGate(boolean abandon)throws Exception {
        ExecutorService worker=executor();CountDownLatch row=new CountDownLatch(1),finished=new CountDownLatch(1);
        AtomicInteger rows=new AtomicInteger(),writers=new AtomicInteger(),closed=new AtomicInteger();
        CodecPreparation1981 prep=new CodecPreparation1981(worker,()->{
            rows.incrementAndGet();row.countDown();
            try{CodecPreparation1981.awaitCodec1981();writers.incrementAndGet();}
            catch(CancellationException expected){closed.incrementAndGet();}
            finally{finished.countDown();}
        },true);
        prep.startStorage1981();await(row,"row/FD phase starts before codec permission");
        check(writers.get()==0,"hardware writer cannot precede codec fence");
        if(abandon)prep.closeAndAwait();else{prep.start();await(finished,"codec event unblocks already prepared storage");prep.closeAndAwait();}
        await(finished,"storage producer cleanup joined");
        check(rows.get()==1,"single existing storage producer");check(writers.get()==(abandon?0:1),"writer count matches owned permit");check(closed.get()==(abandon?1:0),"abandoned FD cleanup executes once");
        prep.start();prep.startStorage1981();check(rows.get()==1,"closed storage cannot restart");
        CountDownLatch cleanThread=new CountDownLatch(1);worker.execute(()->{CodecPreparation1981.awaitCodec1981();cleanThread.countDown();});await(cleanThread,"storage ThreadLocal removed on reusable executor");worker.shutdown();
    }
    static void runningCloseAndInterrupt()throws Exception {
        ExecutorService worker=executor();CountDownLatch running=new CountDownLatch(1),release=new CountDownLatch(1),closing=new CountDownLatch(1),joined=new CountDownLatch(1);
        CodecPreparation1981 prep=new CodecPreparation1981(worker,()->{running.countDown();try{release.await();}catch(InterruptedException e){throw new AssertionError(e);}});
        prep.start();await(running,"running producer entered");AtomicBoolean restored=new AtomicBoolean();
        Thread closer=new Thread(()->{Thread.currentThread().interrupt();closing.countDown();prep.closeAndAwait();restored.set(Thread.currentThread().isInterrupted());joined.countDown();});closer.start();await(closing,"terminal closer entered");
        check(!joined.await(100,TimeUnit.MILLISECONDS),"terminal cleanup waits for actual producer completion");release.countDown();await(joined,"terminal closer released after producer");check(restored.get(),"terminal join preserves interruption flag");worker.shutdown();
    }
    static void queueEvent(boolean closeBeforeRegistration,boolean failCorrection)throws Exception {
        ExecutorService constructors=executor();CountDownLatch firstEncoding=new CountDownLatch(1),allowClose=new CountDownLatch(1),closed=new CountDownLatch(1),allowReceipt=new CountDownLatch(1);
        CountDownLatch secondCorrecting=new CountDownLatch(1),register=new CountDownLatch(1),storageRow=new CountDownLatch(1),writer=new CountDownLatch(1),finishCorrection=new CountDownLatch(1),secondDone=new CountDownLatch(1);
        AtomicReference<Throwable> failure=new AtomicReference<Throwable>();AtomicInteger builds=new AtomicInteger(),aborts=new AtomicInteger();
        Runnable first=new Runnable(){public void run(){try{SaveQueue1935.beginEncoding(this);firstEncoding.countDown();allowClose.await();check(SaveQueue1935.hardwareClosed1956(this),"only encoder owner closes hardware");closed.countDown();allowReceipt.await();SaveQueue1935.terminalTurn1956(this);}catch(Throwable error){failure.compareAndSet(null,error);}finally{SaveQueue1935.release();}}};
        Runnable second=new Runnable(){public void run(){CodecPreparation1981 prep=null;try{
            secondCorrecting.countDown();register.await();
            prep=new CodecPreparation1981(constructors,()->{storageRow.countDown();try{CodecPreparation1981.awaitCodec1981();builds.incrementAndGet();writer.countDown();}catch(CancellationException expected){aborts.incrementAndGet();}},true);
            final CodecPreparation1981 owned=prep;SaveQueue1935.deferCodecPreparation1981(this,()->owned.start());prep.startStorage1981();
            finishCorrection.await();
            if(!failCorrection){SaveQueue1935.beginEncoding(this);prep.start();prep.throwIfFailed();}
        }catch(Throwable error){failure.compareAndSet(null,error);}finally{
            try{SaveQueue1935.cancelCodecPreparation1981(this);if(prep!=null)prep.closeAndAwait();SaveQueue1935.terminalTurn1956(this);}
            catch(Throwable error){failure.compareAndSet(null,error);}finally{SaveQueue1935.release();secondDone.countDown();}
        }}};
        SaveQueue1935.reserve();SaveQueue1935.submit(first);await(firstEncoding,"first photo owns encoder");SaveQueue1935.reserve();SaveQueue1935.submit(second);await(secondCorrecting,"following photo corrects while prior encodes");
        if(closeBeforeRegistration){allowClose.countDown();await(closed,"hardware closes before next registration");}
        register.countDown();await(storageRow,"next pending row prepared during correction");
        if(!closeBeforeRegistration)check(builds.get()==0,"occupied encoder defers only hardware phase");
        if(failCorrection){finishCorrection.countDown();long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(aborts.get()==0&&System.nanoTime()<until)Thread.yield();check(aborts.get()==1,"correction failure cancels blocked storage and cleans row");check(builds.get()==0,"failed correction never builds writer");}
        if(!closeBeforeRegistration){allowClose.countDown();await(closed,"prior hardware release fires event");}
        if(!failCorrection){await(writer,"next constructor starts on close before correction finishes");check(finishCorrection.getCount()==1,"event resumes prep without waiting for normalize completion");check(builds.get()==1,"event grants one writer");finishCorrection.countDown();}
        check(!secondDone.await(100,TimeUnit.MILLISECONDS),"following terminal receipt retains FIFO order");allowReceipt.countDown();await(secondDone,"following receipt completes after predecessor");waitIdle();check(failure.get()==null,"queue event/cancel has no cross-photo exception: "+failure.get());constructors.shutdown();
    }
    public static void main(String[]args)throws Exception {
        onceAndRejection();storageGate(false);storageGate(true);runningCloseAndInterrupt();queueEvent(false,false);queueEvent(true,false);queueEvent(false,true);
        System.out.println("PASS CodecPreparation1981 assertions="+assertions);
    }
}
