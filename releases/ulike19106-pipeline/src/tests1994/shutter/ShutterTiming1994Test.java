package com.hiro.ulike;

import android.view.MotionEvent;
import java.util.concurrent.atomic.AtomicLong;

public final class ShutterTiming1994Test {
    static int assertions;
    static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        check(ShutterTiming1994.consumeDown1994() == 0, "no implicit latest gesture");
        ShutterTiming1994.exit();
        long before = System.nanoTime();
        ShutterTiming1994.enter(new MotionEvent(MotionEvent.ACTION_DOWN));
        long down = ShutterTiming1994.consumeDown1994();
        check(down >= before && down <= System.nanoTime(), "entry records monotonic handler receipt");
        check(ShutterTiming1994.consumeDown1994() == 0, "only one capture consumes one press");
        ShutterTiming1994.exit();
        check(ShutterTiming1994.consumeDown1994() == 0, "ordinary return closes scope");
        for (int action : new int[]{1, 2, 3, 5, 6, 0x100}) {
            ShutterTiming1994.enter(new MotionEvent(action));
            check(ShutterTiming1994.consumeDown1994() == 0, "non-down is never a finger press: " + action);
            ShutterTiming1994.exit();
        }
        ShutterTiming1994.enter(new MotionEvent(0));
        ShutterTiming1994.enter(new MotionEvent(1));
        check(ShutterTiming1994.consumeDown1994() == 0, "nested non-down masks parent");
        ShutterTiming1994.exit();
        check(ShutterTiming1994.consumeDown1994() > 0, "balanced nested event restores own parent");
        ShutterTiming1994.exit();
        ShutterTiming1994.enter(new MotionEvent(0));
        ShutterTiming1994.enter(new MotionEvent(0));
        long inner = ShutterTiming1994.consumeDown1994();
        check(inner > 0, "nested down has its own scope");
        ShutterTiming1994.exit();
        long outer = ShutterTiming1994.consumeDown1994();
        check(outer > 0 && outer <= inner, "outer and inner consumes are independent");
        ShutterTiming1994.exit();
        ShutterTiming1994.enter(new MotionEvent(0));
        AtomicLong foreign = new AtomicLong(-1);
        Thread other = new Thread(() -> foreign.set(ShutterTiming1994.consumeDown1994()));
        other.start(); other.join();
        check(foreign.get() == 0, "worker cannot inherit UI event");
        check(ShutterTiming1994.consumeDown1994() > 0, "foreign attempt cannot consume UI owner");
        ShutterTiming1994.exit();
        ShutterTiming1994.enter(new MotionEvent(0));
        ShutterTiming1994.exit();
        check(ShutterTiming1994.consumeDown1994() == 0, "deferred same-thread capture cannot borrow old touch");
        RuntimeException expected = new RuntimeException("original dispatch failure");
        try {
            ShutterTiming1994.enter(new MotionEvent(0));
            try { throw expected; } finally { ShutterTiming1994.exit(); }
        } catch (RuntimeException actual) {
            check(actual == expected, "dispatch failure identity retained");
        }
        check(ShutterTiming1994.consumeDown1994() == 0, "exception closes dispatch scope");
        ShutterTiming1994.enter(null);
        check(ShutterTiming1994.consumeDown1994() == 0, "null event remains unobserved");
        ShutterTiming1994.exit();
        ShutterTiming1994.enter(new MotionEvent(0));
        MotionEvent broken = new MotionEvent(0); broken.fail = true;
        ShutterTiming1994.enter(broken);
        check(ShutterTiming1994.consumeDown1994() == 0, "failed instrumentation discards all scopes");
        ShutterTiming1994.exit(); ShutterTiming1994.exit();
        check(ShutterTiming1994.consumeDown1994() == 0, "failed nested entry cannot expose stale parent");
        for (int i = 0; i < 50; i++) {
            ShutterTiming1994.enter(new MotionEvent(0));
            check(ShutterTiming1994.consumeDown1994() > 0, "successive new press admitted " + i);
            ShutterTiming1994.exit();
        }
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":" + assertions + "}");
    }
}
