package android.os;
import java.util.ArrayDeque;
public class Handler {
    public static final ArrayDeque<Runnable> queue = new ArrayDeque<Runnable>();
    public static boolean rejectPosts;
    public Handler(Looper looper) {
        if (looper != Looper.MAIN) { throw new AssertionError("Expected main looper"); }
    }
    public boolean post(Runnable runnable) {
        if (rejectPosts) { return false; }
        queue.add(runnable);
        return true;
    }
    public static void drain() {
        int calls = 0;
        while (!queue.isEmpty()) {
            if (++calls > 100) { throw new AssertionError("Unexpected post loop"); }
            queue.remove().run();
        }
    }
}
