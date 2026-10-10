package com.hiro.ulike;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** GX25: bounded two-bank ordered residual finish. Only one CPU preparation
 * can run ahead. A cancelled/failed invocation joins that preparation before
 * returning its borrowed source/model lease to the caller. */
final class ResidualOverlap1962 {
    private ResidualOverlap1962() {}
    interface Guard { void check(); }
    interface Driver {
        float[] prepare(int begin,int end);
        Object submit(float[] records,int bank,int begin,int end);
        boolean collect(Object ticket,int bank,int count,int[] target,int offset);
    }
    private static final ExecutorService PREPARER=new ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<Runnable>(1),new ThreadFactory(){
        public Thread newThread(Runnable task){Thread t=new Thread(task,"Hiro-ULike-GX25-prepare");t.setDaemon(true);return t;}
    },new ThreadPoolExecutor.AbortPolicy());
    private static void check(Guard guard){
        if(Thread.currentThread().isInterrupted())throw new CancellationException("GPU residual overlap cancelled");
        guard.check();
    }
    private static final class Preparation implements Runnable {
        final CountDownLatch done=new CountDownLatch(1);
        final int begin,end;
        Driver driver;Guard guard;float[] records;Throwable failure;
        private Thread worker;private boolean stopped;
        Preparation(Driver d,Guard g,int b,int e){driver=d;guard=g;begin=b;end=e;}
        void start(){
            synchronized(this){
                try{PREPARER.execute(this);}
                catch(RuntimeException rejected){stopped=true;driver=null;guard=null;done.countDown();throw rejected;}
                catch(Error failed){stopped=true;driver=null;guard=null;done.countDown();throw failed;}
            }
        }
        public void run(){
            try {
                synchronized(this){if(stopped)return;worker=Thread.currentThread();}
                check(guard);float[] prepared=driver.prepare(begin,end);check(guard);
                synchronized(this){if(!stopped)records=prepared;}
            } catch(Throwable error){failure=error;}
            finally {
                synchronized(this){worker=null;driver=null;guard=null;}
                // This private worker belongs to the pool; caller cancellation
                // must not leak an interrupt into the next photograph's task.
                Thread.interrupted();done.countDown();
            }
        }
        float[] take(){
            try{done.await();}catch(InterruptedException interrupted){
                Thread.currentThread().interrupt();throw new CancellationException("GPU residual preparation wait cancelled");
            }
            if(failure instanceof RuntimeException)throw (RuntimeException)failure;
            if(failure instanceof Error)throw (Error)failure;
            if(failure!=null)throw new IllegalStateException("GPU residual preparation failed",failure);
            float[] result=records;records=null;return result;
        }
        void stopAndJoin(){
            synchronized(this){stopped=true;if(worker!=null)worker.interrupt();}
            boolean interrupted=false;
            for(;;)try{done.await();break;}catch(InterruptedException cancelled){interrupted=true;}
            records=null;failure=null;
            if(interrupted)Thread.currentThread().interrupt();
        }
    }
    private static final class Band {
        final int bank,begin,end;final Object ticket;
        Band(int b,int lo,int hi,Object t){bank=b;begin=lo;end=hi;ticket=t;}
    }
    private static Band submit(Driver driver,Guard guard,float[] records,int bank,int begin,int end){
        check(guard);if(records==null)return null;
        Object ticket=driver.submit(records,bank,begin,end);check(guard);
        return ticket==null?null:new Band(bank,begin,end,ticket);
    }
    static int[] run(int width,int begin,int end,int batchRows,Driver driver,Guard guard){
        long count=(long)width*(end-begin);
        if(width<1||begin<0||end<=begin||batchRows<1||count>Integer.MAX_VALUE||driver==null||guard==null)
            throw new IllegalArgumentException("GPU residual overlap dimensions");
        check(guard);int[] candidate=new int[(int)count];Preparation ahead=null;
        try {
            int finish=(int)Math.min((long)end,(long)begin+batchRows);
            Band current=submit(driver,guard,driver.prepare(begin,finish),0,begin,finish);
            if(current==null)return null;
            for(;;){
                check(guard);Band following=null;
                if(current.end<end){
                    int nextEnd=(int)Math.min((long)end,(long)current.end+batchRows);
                    ahead=new Preparation(driver,guard,current.end,nextEnd);
                    ahead.start();
                    float[] records=ahead.take();check(guard);ahead=null;
                    following=submit(driver,guard,records,current.bank^1,current.end,nextEnd);
                    records=null;
                    if(following==null)return null;
                }
                int batchCount=width*(current.end-current.begin);
                boolean complete=driver.collect(current.ticket,current.bank,batchCount,candidate,(current.begin-begin)*width);check(guard);
                // A late mapped-read/unmap error may already have touched this
                // private array. It can never escape as a partial photograph.
                if(!complete)return null;
                if(following==null){check(guard);return candidate;}
                current=following;
            }
        } finally {
            // Future.cancel() alone cannot prove that JNI stopped reading the
            // source. The actual task completion is always awaited instead.
            if(ahead!=null)ahead.stopAndJoin();
        }
    }
}
