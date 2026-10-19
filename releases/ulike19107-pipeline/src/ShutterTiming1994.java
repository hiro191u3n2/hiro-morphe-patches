package com.hiro.ulike;

import android.view.MotionEvent;

/** One lexical dispatch of the real shutter view. Never remembers a latest tap. */
public final class ShutterTiming1994 {
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<Scope>();
    private ShutterTiming1994() { }

    private static final class Scope {
        final Scope previous;
        final long downNanos;
        boolean consumed;
        Scope(Scope previous, long downNanos) {
            this.previous = previous;
            this.downNanos = downNanos;
        }
    }

    /** Inserted at the actual ShutterButton.onTouchEvent entry. */
    public static void enter(MotionEvent event) {
        final long entered = System.nanoTime();
        try {
            // A nested non-down event must mask an outer down scope too.
            long down = event != null && event.getAction() == MotionEvent.ACTION_DOWN
                    ? entered : 0;
            ACTIVE.set(new Scope(ACTIVE.get(), down));
        } catch (Throwable unavailable) {
            // A missing marker cannot safely be matched to a later exit.
            // Discard all measurement scopes rather than exposing an older tap.
            clear();
        }
    }

    /** Inserted before every return and in the original dispatch catch-all. */
    public static void exit() {
        try {
            Scope scope = ACTIVE.get();
            if (scope == null || scope.previous == null) ACTIVE.remove();
            else ACTIVE.set(scope.previous);
        } catch (Throwable unavailable) {
            clear();
        }
    }

    /** Only an exact synchronous capture callback may consume the current down. */
    public static long consumeDown1994() {
        try {
            Scope scope = ACTIVE.get();
            if (scope == null || scope.consumed || scope.downNanos <= 0) return 0;
            scope.consumed = true;
            return scope.downNanos;
        } catch (Throwable unavailable) {
            clear();
            return 0;
        }
    }

    private static void clear() {
        try { ACTIVE.remove(); } catch (Throwable unavailable) { }
    }
}
