package android.os;
import java.util.ArrayDeque;
public class Handler {
    private static final ArrayDeque<Runnable> QUEUE = new ArrayDeque<Runnable>();
    public Handler(Looper looper) { }
    public boolean post(Runnable action) {
        synchronized (QUEUE) { QUEUE.addLast(action); }
        return true;
    }
    public static void drain() {
        int remaining = 1000;
        for (;;) {
            Runnable action;
            synchronized (QUEUE) { action = QUEUE.pollFirst(); }
            if (action == null) return;
            if (--remaining == 0) throw new AssertionError("unbounded posted work");
            action.run();
        }
    }
    public static int queuedCount() { synchronized (QUEUE) { return QUEUE.size(); } }
}
