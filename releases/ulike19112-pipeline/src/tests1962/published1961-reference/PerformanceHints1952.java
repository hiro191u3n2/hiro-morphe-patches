package com.hiro.ulike;

import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import java.lang.reflect.Method;

/** Optional H15 OS scheduling hints. This helper never changes pixels or clocks.
 * Only public API31+ methods are reflected, so the baseline Android jar remains usable.
 * An unsupported/denied hint is a no-op and cannot fail a photo. */
public final class PerformanceHints1952 {
    private PerformanceHints1952() {}
    private static volatile Api api;
    private static final ThreadLocal<State> WORKER = new ThreadLocal<State>();

    /** Initialize from an existing application Context startup hook, never by probing
     * hidden ActivityThread/AppGlobals APIs. No Activity or Context is retained. */
    public static void init(Context context) {
        if (context == null || Build.VERSION.SDK_INT < 31 || api != null) return;
        try {
            Class<?> managerClass = Class.forName("android.os.PerformanceHintManager");
            Class<?> sessionClass = Class.forName("android.os.PerformanceHintManager$Session");
            Object manager = context.getSystemService("performance_hint");
            if (!managerClass.isInstance(manager)) return;
            Method clock = null;
            if (Build.VERSION.SDK_INT >= 35) {
                try { clock = SystemClock.class.getMethod("uptimeNanos"); }
                catch (Throwable absent) { /* uptimeMillis is the same non-suspend clock. */ }
            }
            Api ready = new Api(manager,
                managerClass.getMethod("createHintSession", int[].class, long.class),
                sessionClass.getMethod("updateTargetWorkDuration", long.class),
                sessionClass.getMethod("reportActualWorkDuration", long.class),
                sessionClass.getMethod("close"), clock);
            synchronized (PerformanceHints1952.class) {
                if (api == null) {
                    api = ready;
                    // CPU pool owns permits, cancellation and worker lifetimes. Its
                    // injected interface leaves the pixel-only pool Android-free.
                    SpeedWorkers1935.installHints(new SpeedWorkers1935.Hints() {
                        public Runnable worker(Runnable loop) { return PerformanceHints1952.worker(loop); }
                        public Object begin(long target) { return PerformanceHints1952.begin(target); }
                        public void complete(Object token, boolean success) {
                            if (token instanceof Token) {
                                if (success) PerformanceHints1952.complete((Token) token);
                                else PerformanceHints1952.cancel((Token) token);
                            }
                        }
                    });
                }
            }
        } catch (Throwable unsupported) { /* Optional service/SDK failures are harmless. */ }
    }

    /** Wrap ThreadFactory's executor worker loop. Sessions live only as long as their
     * real Linux worker TID and learn from successive tasks. Always closes on timeout,
     * shutdown, interruption or delegate failure. A nested wrapper preserves its owner. */
    public static Runnable worker(final Runnable work) {
        if (work == null) throw new NullPointerException("hint worker");
        try {
            return new Runnable() {
                public void run() {
                    State state = null;
                    State previous = null;
                    try {
                        previous = WORKER.get();
                        if (previous == null) {
                            state = new State(true);
                            WORKER.set(state);
                        }
                    } catch (Throwable optional) { state = null; }
                    try { work.run(); }
                    finally {
                        if (state != null) {
                            synchronized (state) {
                                if (state.active != null) state.active.finished = true;
                                state.active = null;
                                closeSession(state);
                            }
                            try { if (previous == null) WORKER.remove(); else WORKER.set(previous); }
                            catch (Throwable optional) { }
                        }
                    }
                }
            };
        } catch (Throwable optional) { return work; }
    }

    /** Begin after a CPU permit is obtained, on the actual thread doing the work.
     * The caller supplies a positive workload deadline in nanoseconds, not an FPS.
     * Outside a wrapped pool worker, the returned scope owns a one-shot session.
     * Nested scopes are skipped so the same workload is never reported twice. */
    public static Token begin(long targetDurationNanos) {
        Api current = api;
        if (current == null || targetDurationNanos <= 0 || Build.VERSION.SDK_INT < 31) return null;
        State state = null;
        try {
            int tid = Process.myTid(); // Java Thread.getId() is not an Android Linux TID.
            if (tid <= 0) return null;
            state = WORKER.get();
            if (state == null) {
                state = new State(false);
                WORKER.set(state);
            }
            synchronized (state) {
                if (state.disabled || state.active != null) return null;
                if (state.session != null && (state.tid != tid || state.api != current)) closeSession(state);
                if (state.session == null) {
                    state.api = current;
                    state.tid = tid;
                    state.session = current.create.invoke(current.manager, new int[] {tid}, targetDurationNanos);
                    if (state.session == null) {
                        state.disabled = true;
                        clearOneShot(state);
                        return null;
                    }
                    state.target = targetDurationNanos;
                } else if (state.target != targetDurationNanos) {
                    current.update.invoke(state.session, targetDurationNanos);
                    state.target = targetDurationNanos;
                }
                Token token = new Token(state, now(current));
                state.active = token;
                return token;
            }
        } catch (Throwable optional) {
            if (state != null) synchronized (state) {
                state.disabled = true;
                closeSession(state);
                clearOneShot(state);
            }
            return null;
        }
    }

    /** Call only after completed work. Duration excludes queue/permit wait. A report
     * failure disables just this worker's optional hint; it never changes photo status. */
    public static void complete(Token token) { end(token, true); }
    /** Failed/cancelled partial work is not reported as a faster successful workload. */
    public static void cancel(Token token) { end(token, false); }

    private static void end(Token token, boolean completed) {
        if (token == null) return;
        State state = token.state;
        synchronized (state) {
            if (token.finished) return;
            token.finished = true;
            if (state.active != token) return;
            state.active = null;
            boolean reported = false;
            try {
                if (completed && state.session != null) {
                    long elapsed = now(state.api) - token.started;
                    // uptimeMillis quantization on API31-34 can produce a zero sample.
                    if (elapsed > 0) state.api.report.invoke(state.session, elapsed);
                    reported = true;
                }
            } catch (Throwable optional) { state.disabled = true; }
            finally {
                if (!state.reusable || !completed || !reported || state.disabled) closeSession(state);
                clearOneShot(state);
            }
        }
    }

    private static long now(Api current) {
        if (current.clock != null) {
            try { return ((Long) current.clock.invoke(null)).longValue(); }
            catch (Throwable optional) { }
        }
        return SystemClock.uptimeMillis() * 1000000L;
    }
    /** Caller holds the state lock, serializing report/update/close as Android requires. */
    private static void closeSession(State state) {
        Object session = state.session;
        state.session = null;
        if (session != null) {
            try { state.api.close.invoke(session); }
            catch (Throwable optional) { /* Already disposed/OS failure is non-fatal. */ }
        }
    }
    private static void clearOneShot(State state) {
        if (!state.reusable) {
            try { if (WORKER.get() == state) WORKER.remove(); }
            catch (Throwable optional) { }
        }
    }

    public static final class Token {
        private final State state;
        private final long started;
        private boolean finished;
        private Token(State state, long started) { this.state = state; this.started = started; }
    }
    private static final class State {
        final boolean reusable;
        Api api;
        Object session;
        int tid;
        long target;
        boolean disabled;
        Token active;
        State(boolean reusable) { this.reusable = reusable; }
    }
    private static final class Api {
        final Object manager;
        final Method create, update, report, close, clock;
        Api(Object manager, Method create, Method update, Method report, Method close, Method clock) {
            this.manager = manager; this.create = create; this.update = update;
            this.report = report; this.close = close; this.clock = clock;
        }
    }
}
