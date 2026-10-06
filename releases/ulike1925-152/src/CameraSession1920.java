package com.hiro.ulike;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

/** Bounded recovery for an advanced Camera2 session whose configuration callback never arrives.
 * Normal and successfully configured sessions are not changed. This does not diagnose all
 * possible causes of a black preview (permissions, native renderer, or blocked driver threads).
 */
public final class CameraSession1920 {
    static final long WAIT_MS = 10000L;
    private CameraSession1920() {}

    public static void session(Object owner, CameraDevice device, SessionConfiguration config) {
        // Keep all original selection, surfaces, callbacks and exception semantics.
        CaptureAdvanced3.sessionCore1920(owner, device, config);
        try {
            CaptureAdvanced3.State state = CaptureAdvanced3.state1920(owner);
            if (state == null || !state.maximum || state.restoring || state.configured
                    || state.device != device) return;
            Ticket ticket = new Ticket(owner, state, config.getExecutor());
            new Handler(Looper.getMainLooper()).postDelayed(ticket, WAIT_MS);
        } catch (RuntimeException error) {
            Log.w("ULikeSession1920", "Could not arm optional startup timeout", error);
        } catch (LinkageError error) {
            Log.w("ULikeSession1920", "Startup timeout unavailable", error);
        }
    }

    /** Atomically arbitrate a late successful callback against our timeout. */
    public static boolean acceptConfigured1920(Object owner, CaptureAdvanced3.State state, long token) {
        synchronized (state) {
            if (state.token != token || state.timedOutToken1920 == token || state.restoring
                    || !CaptureAdvanced3.current1920(owner, state)) return false;
            state.configured = true;
            return true;
        }
    }

    static final class Ticket implements Runnable {
        final WeakReference<Object> owner;
        final CaptureAdvanced3.State state;
        final long token;
        final long deadline;
        final Executor executor;
        boolean dispatched;
        Ticket(Object owner, CaptureAdvanced3.State state, Executor executor) {
            this.owner = new WeakReference<Object>(owner);
            this.state = state;
            this.token = state.token;
            this.deadline = SystemClock.uptimeMillis() + WAIT_MS;
            this.executor = executor;
        }
        boolean pending(Object host) {
            try {
                return host != null && state.token == token && state.timedOutToken1920 != token && state.maximum
                        && !state.restoring && !state.configured
                        && ManualLens170.yes("foreground") && ManualLens170.ready()
                        && CaptureAdvanced3.current1920(host, state);
            } catch (ReflectiveOperationException error) {
                Log.w("ULikeSession1920", "Camera ownership could not be confirmed", error);
                return false;
            } catch (RuntimeException error) {
                return false;
            } catch (LinkageError error) {
                return false;
            }
        }
        @Override public void run() {
            if (dispatched || SystemClock.uptimeMillis() < deadline || !pending(owner.get())) return;
            dispatched = true;
            try {
                // Recovery must use the same executor as the original session callback,
                // not the main/UI thread used solely for the deadline.
                executor.execute(new Runnable() {
                    @Override public void run() {
                        Object host = owner.get();
                        synchronized (state) {
                            if (!pending(host)) return;
                            state.timedOutToken1920 = token;
                        }
                        Log.w("ULikeSession1920", "Advanced session timed out; using existing normal-mode recovery");
                        CaptureAdvanced3.retry1920(host, state,
                                new TimeoutException("Camera2 advanced configuration callback absent for 10 seconds"));
                    }
                });
            } catch (RuntimeException error) {
                Log.w("ULikeSession1920", "Camera callback executor rejected timeout recovery", error);
            }
        }
    }
}
