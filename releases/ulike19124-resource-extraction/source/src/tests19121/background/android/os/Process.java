package android.os;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Deterministic public-API fixture; this does not emulate Android scheduling. */
public final class Process {
    public static final AtomicInteger calls=new AtomicInteger();
    public static final ConcurrentHashMap<Thread,Integer> priorities=new ConcurrentHashMap<Thread,Integer>();
    public static volatile boolean deny;
    public static volatile Thread lastCaller;
    public static int myTid() { return 923; }
    public static void setThreadPriority(int priority) {
        calls.incrementAndGet();lastCaller=Thread.currentThread();
        if(deny)throw new SecurityException("fixture denial");
        priorities.put(Thread.currentThread(),Integer.valueOf(priority));
    }
}
