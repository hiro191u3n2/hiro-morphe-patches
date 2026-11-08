package android.os;

import java.util.Iterator;
import java.util.PriorityQueue;

/** Deterministic main-thread scheduler: no sleeps, threads, or wall-clock dependence. */
public final class Handler {
    private static final class Entry implements Comparable<Entry> {
        final Handler handler;
        final Runnable runnable;
        final long due;
        final long sequence;
        Entry(Handler handler, Runnable runnable, long due, long sequence) {
            this.handler = handler;
            this.runnable = runnable;
            this.due = due;
            this.sequence = sequence;
        }
        @Override public int compareTo(Entry other) {
            int time = Long.compare(due, other.due);
            return time != 0 ? time : Long.compare(sequence, other.sequence);
        }
    }
    private static final PriorityQueue<Entry> QUEUE = new PriorityQueue<Entry>();
    private static long sequence;
    public static RuntimeException nextPostFailure;

    public Handler(Looper looper) {
        if (looper != Looper.getMainLooper()) throw new IllegalArgumentException("Main looper required");
    }
    public boolean postDelayed(Runnable runnable, long delayMillis) {
        if (nextPostFailure != null) {
            RuntimeException failure = nextPostFailure;
            nextPostFailure = null;
            throw failure;
        }
        if (runnable == null) throw new NullPointerException("runnable");
        QUEUE.add(new Entry(this, runnable, SystemClock.elapsedRealtime() + Math.max(0L, delayMillis), sequence++));
        return true;
    }
    public void removeCallbacks(Runnable runnable) {
        for (Iterator<Entry> iterator = QUEUE.iterator(); iterator.hasNext();) {
            Entry entry = iterator.next();
            if (entry.handler == this && entry.runnable == runnable) iterator.remove();
        }
    }
    public static int queuedCallbacks() { return QUEUE.size(); }
    public static void advanceBy(long durationMillis) {
        if (durationMillis < 0) throw new IllegalArgumentException("Cannot reverse virtual time");
        runUntil(SystemClock.elapsedRealtime() + durationMillis);
    }
    public static void runUntil(long targetMillis) {
        if (targetMillis < SystemClock.elapsedRealtime()) throw new IllegalArgumentException("Cannot reverse virtual time");
        int executed = 0;
        while (!QUEUE.isEmpty() && QUEUE.peek().due <= targetMillis) {
            if (++executed > 100000) throw new AssertionError("Unbounded callback loop");
            Entry entry = QUEUE.remove();
            SystemClock.setElapsedRealtime(entry.due);
            entry.runnable.run();
        }
        SystemClock.setElapsedRealtime(targetMillis);
    }
    public static void reset() {
        QUEUE.clear();
        sequence = 0L;
        nextPostFailure = null;
        SystemClock.setElapsedRealtime(0L);
    }
}
