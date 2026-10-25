package com.hiro.ulike;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Shared bounded CPU workers and exclusively leased scratch arrays. No Android state. */
public final class SpeedWorkers1935 {
    private SpeedWorkers1935() { }
    private static final int LIMIT = Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors()));
    private static final long RETAIN_LIMIT = 24L * 1024 * 1024;
    private static final long ARRAY_LIMIT = 8L * 1024 * 1024;
    private static final long RESERVE = 32L * 1024 * 1024;
    private static final int ARRAY_COUNT_LIMIT = 64;
    private static final Semaphore CPU = new Semaphore(LIMIT, true);
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<Boolean>();
    private static final ArrayDeque<Object> ARRAYS = new ArrayDeque<Object>();
    private static long retained;
    private static final AtomicInteger THREAD_IDS = new AtomicInteger();
    private static final AtomicInteger GROUPS = new AtomicInteger();
    private static final AtomicLong COMPETITION = new AtomicLong();
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(LIMIT, LIMIT,
        30L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(32), new ThreadFactory() {
            public Thread newThread(Runnable work) {
                Thread thread = new Thread(work, "ULikeSpeed1935-" + THREAD_IDS.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        }, new ThreadPoolExecutor.AbortPolicy());
    static { EXECUTOR.allowCoreThreadTimeOut(true); }

    public static int maxWorkers() { return LIMIT; }
    public static boolean cpuIdle1944() {return GROUPS.get()==0 && CPU.availablePermits()==LIMIT;}
    public static long competitionEpoch1944() {return COMPETITION.get();}
    public static int availableWorkers1944() {return Boolean.TRUE.equals(ACTIVE.get())?1:Math.max(1,CPU.availablePermits());}

    /** Existing native schedulers keep their ABI but share this CPU budget. */
    public static boolean enterLegacy() {
        if(Boolean.TRUE.equals(ACTIVE.get()))return false;
        if(GROUPS.get()>0)COMPETITION.incrementAndGet();
        try { CPU.acquire(); }
        catch(InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("speed interrupted",failure);
        }
        try { ACTIVE.set(Boolean.TRUE); }
        catch(Throwable failure) {CPU.release();throw failure;}
        return true;
    }
    public static void leaveLegacy(boolean acquired) {
        if(acquired) {ACTIVE.remove();CPU.release();}
    }

    /**
     * Finish every submitted task before returning or throwing, so callers may safely
     * release their buffers. Nested calls stay on their existing worker; a worker never
     * blocks waiting for this pool. Concurrent external calls share the same CPU cap.
     */
    public static void run(Runnable[] work) {
        if (work == null) throw new NullPointerException("speed tasks");
        for (Runnable task : work) if (task == null) throw new NullPointerException("speed task");
        if (work.length == 0) return;
        if (Boolean.TRUE.equals(ACTIVE.get())) {
            for (Runnable task : work) {
                if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("speed interrupted");
                task.run();
            }
            return;
        }
        Group group = new Group();
        Task[] tasks = new Task[work.length];
        for (int i=0; i<tasks.length; i++) tasks[i] = new Task(work[i],group);
        if(GROUPS.getAndIncrement()>0 || CPU.availablePermits()!=LIMIT)COMPETITION.incrementAndGet();
        boolean interrupted = Thread.interrupted();
        Throwable failure = null;
        int submitted = 0;
        try {
            if (!interrupted) {
                for (; submitted<tasks.length; submitted++) {
                    Task task = tasks[submitted];
                    try { EXECUTOR.execute(task); }
                    catch (RejectedExecutionException saturated) {
                        // Bounded queue with blocking producer, never CallerRuns (which
                        // would add CPU workers when two saves overlap).
                        EXECUTOR.getQueue().put(task);
                    }
                }
            }
        } catch (InterruptedException error) { interrupted = true; }
          catch (Throwable error) { failure = error; }
        finally {
            if (interrupted || failure != null) cancelAll(tasks);
            for (int i=0;i<tasks.length;i++) {
                for (;;) {
                    try { tasks[i].finished.await(); break; }
                    catch (InterruptedException error) {
                        interrupted = true;
                        cancelAll(tasks);
                    }
                }
                if (failure == null && tasks[i].failure != null) failure = tasks[i].failure;
            }
            if (interrupted) Thread.currentThread().interrupt();
            GROUPS.decrementAndGet();
        }
        if (failure instanceof OutOfMemoryError) { trim(); throw (OutOfMemoryError)failure; }
        if (failure instanceof Error) throw (Error)failure;
        if (failure instanceof RuntimeException) throw (RuntimeException)failure;
        if (failure != null) throw new IllegalStateException("speed worker failed", failure);
        if (interrupted) throw new IllegalStateException("speed interrupted");
    }

    private static void cancelAll(Task[] tasks) {
        // One publication blocks every queued task before running tasks are interrupted.
        if(tasks.length>0)tasks[0].group.cancelled=true;
        for (Task task : tasks) {
            task.cancel();
            // Drop queued delegates immediately: they can own a whole shot bitmap.
            EXECUTOR.remove(task);
        }
    }

    private static final class Group { volatile boolean cancelled; }

    private static final class Task implements Runnable {
        final Runnable delegate;
        final Group group;
        final CountDownLatch finished = new CountDownLatch(1);
        volatile Throwable failure;
        private Thread running;
        private boolean cancelled;
        Task(Runnable delegate,Group group) { this.delegate=delegate;this.group=group; }
        synchronized void cancel() {
            cancelled=true;
            if(running!=null)running.interrupt(); else finished.countDown();
        }
        public void run() {
            Boolean previous = ACTIVE.get();
            boolean oldInterrupt = Thread.currentThread().isInterrupted();
            boolean acquired = false;
            try {
                synchronized(this) {
                    if(cancelled||group.cancelled)return;
                    running=Thread.currentThread();
                }
                CPU.acquire();
                acquired = true;
                if(group.cancelled)return;
                ACTIVE.set(Boolean.TRUE);
                delegate.run();
            } catch(Throwable error) { failure=error; }
            finally {
                synchronized(this) { running=null; }
                if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);
                if(acquired)CPU.release();
                if(!oldInterrupt)Thread.interrupted();
                finished.countDown();
            }
        }
    }

    /** Exact lengths preserve existing caller contracts; an array has one lease. */
    public static int[] borrowInts(int length) {
        if(length<0)throw new IllegalArgumentException("speed array length");
        synchronized(ARRAYS) {
            Iterator<Object> it=ARRAYS.iterator();
            while(it.hasNext()) {
                Object value=it.next();
                if(value instanceof int[] && ((int[])value).length==length) {
                    it.remove();retained-=(long)length*4;return (int[])value;
                }
            }
        }
        trimForAllocation((long)length*4);
        try { return new int[length]; } catch(OutOfMemoryError failure) {trim();throw failure;}
    }
    public static float[] borrowFloats(int length) {
        if(length<0)throw new IllegalArgumentException("speed array length");
        synchronized(ARRAYS) {
            Iterator<Object> it=ARRAYS.iterator();
            while(it.hasNext()) {
                Object value=it.next();
                if(value instanceof float[] && ((float[])value).length==length) {
                    it.remove();retained-=(long)length*4;return (float[])value;
                }
            }
        }
        trimForAllocation((long)length*4);
        try { return new float[length]; } catch(OutOfMemoryError failure) {trim();throw failure;}
    }
    public static void release(int[] array) { if(array!=null)releaseArray(array,(long)array.length*4); }
    public static void release(float[] array) { if(array!=null)releaseArray(array,(long)array.length*4); }
    private static void releaseArray(Object array,long bytes) {
        if(bytes==0 || bytes>ARRAY_LIMIT)return;
        synchronized(ARRAYS) {
            if(ARRAYS.contains(array))return; // defensive duplicate-return guard
            if(availableMemory()<RESERVE) { ARRAYS.clear();retained=0;return; }
            while(!ARRAYS.isEmpty() && (retained+bytes>RETAIN_LIMIT || ARRAYS.size()>=ARRAY_COUNT_LIMIT)) {
                Object old=ARRAYS.removeFirst();
                retained-=old instanceof int[]?(long)((int[])old).length*4:(long)((float[])old).length*4;
            }
            ARRAYS.addLast(array);retained+=bytes;
        }
    }
    private static long availableMemory() {
        Runtime runtime=Runtime.getRuntime();
        return runtime.maxMemory()-(runtime.totalMemory()-runtime.freeMemory());
    }
    private static void trimForAllocation(long bytes) { if(availableMemory()-bytes<RESERVE)trim(); }
    public static long retainedBytes() { synchronized(ARRAYS) {return retained;} }
    public static void trim() { synchronized(ARRAYS) {ARRAYS.clear();retained=0;} }
}
