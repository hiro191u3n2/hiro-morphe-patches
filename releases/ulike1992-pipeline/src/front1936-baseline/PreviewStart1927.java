package com.hiro.ulike;

import android.os.SystemClock;
import android.util.Log;
import com.ss.android.vesdk.VECameraCapture;
import com.ss.android.vesdk.frame.TECapturePipeline;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Recover a lost native preview-start request, not a camera open or lens switch.
 * Native onCaptureStarted ignores startPreview's -100/-105 result; the later
 * addCapturePipelines setter does not retry it. Keep this handshake bounded and
 * bound to the exact capture/route generation. Never construct or replace surfaces.
 */
public final class PreviewStart1927 {
    static final long WAIT_MS = 5000L;
    static final long POLL_MS = 100L;
    static final int MAX_REPLAYS = 2;
    private static final Map<VECameraCapture, Pending> pending = new WeakHashMap<>();
    private static final Map<VECameraCapture, Integer> calls = new WeakHashMap<>();
    private PreviewStart1927() {}

    /** Replaces only the original call, keeping its return and exception semantics. */
    public static int start(VECameraCapture capture) {
        enter(capture);
        try {
            int result = capture.startPreview();
            remember(capture, result);
            return result;
        } finally {
            leave(capture);
        }
    }
    private static void enter(VECameraCapture capture) {
        synchronized (pending) {
            Integer n = calls.get(capture);
            calls.put(capture, n == null ? 1 : n + 1);
        }
    }
    private static void leave(VECameraCapture capture) {
        synchronized (pending) {
            Integer n = calls.get(capture);
            if (n == null || n <= 1) calls.remove(capture); else calls.put(capture, n - 1);
        }
    }
    private static boolean owned(VECameraCapture capture) throws ReflectiveOperationException {
        Object owner = ManualLens170.get("capture");
        return capture != null && owner instanceof WeakReference
                && ((WeakReference<?>) owner).get() == capture
                && ManualLens170.yes("foreground");
    }
    static boolean sourcesReady(VECameraCapture capture) {
        if (capture == null || capture.n == null || capture.n.isEmpty()) return false;
        for (TECapturePipeline pipeline : capture.n.getImmutableList()) {
            if (pipeline != null && pipeline.isPreview() && pipeline.isValid()) return true;
        }
        return false;
    }
    /** A late pipeline assignment is an event, not a new capture request. */
    public static void pipelines(VECameraCapture capture) {
        synchronized (pending) {
            Pending p = pending.get(capture);
            if (p != null) schedule(p, 0L);
        }
    }
    private static void drop(VECameraCapture capture, Pending p) {
        synchronized (pending) {
            if (pending.get(capture) == p) pending.remove(capture);
            OpticalZoom.MAIN.removeCallbacks(p);
        }
    }
    private static void schedule(Pending p, long delay) {
        OpticalZoom.MAIN.removeCallbacks(p);
        if (!OpticalZoom.MAIN.postDelayed(p, delay)) {
            VECameraCapture capture = p.capture.get();
            if (capture != null && pending.get(capture) == p) pending.remove(capture);
        }
    }
    private static void remember(VECameraCapture capture, int result) {
        try {
            synchronized (pending) {
                Pending p = pending.get(capture);
                if (result == 0 || !owned(capture)) {
                    if (p != null) drop(capture, p);
                    return;
                }
                // Do not retry permission/HAL/format errors with already valid inputs.
                boolean notReady = !capture.p.get() || !sourcesReady(capture);
                if ((result != -100 && result != -105 && result != -1) || !notReady) {
                    if (p != null) drop(capture, p);
                    return;
                }
                long epoch = ManualLens170.number("epoch");
                OpticalZoom.Route route = (OpticalZoom.Route) ManualLens170.get("active");
                if (route == null || !route.rear || route.failed || route.epoch != epoch) {
                    if (p != null) drop(capture, p);
                    return;
                }
                if (p != null && (p.epoch != epoch || p.route.get() != route)) { drop(capture, p); p = null; }
                if (p == null) {
                    p = new Pending(capture, route, epoch, SystemClock.uptimeMillis());
                    pending.put(capture, p);
                    Log.i("ULikeStart1927", "Preview start waits for native input readiness: " + result);
                }
                schedule(p, POLL_MS);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            Log.w("ULikeStart1927", "Optional start replay could not be armed", error);
        }
    }
    static final class Pending implements Runnable {
        final WeakReference<VECameraCapture> capture;
        final long epoch, deadline;
        final WeakReference<OpticalZoom.Route> route;
        int replays;
        Pending(VECameraCapture capture, OpticalZoom.Route route, long epoch, long now) {
            this.route = new WeakReference<>(route);
            this.capture = new WeakReference<>(capture);
            this.epoch = epoch;
            this.deadline = now + WAIT_MS;
        }
        @Override public void run() {
            VECameraCapture c = capture.get();
            if (c == null) return;
            try {
                synchronized (pending) {
                    if (pending.get(c) != this) return;
                    if (!owned(c) || epoch != ManualLens170.number("epoch")
                            || SystemClock.uptimeMillis() >= deadline || replays >= MAX_REPLAYS) {
                        drop(c, this); return;
                    }
                    OpticalZoom.Route route = (OpticalZoom.Route) ManualLens170.get("active");
                    if (route == null || this.route.get() != route || route.epoch != epoch || !route.rear || route.failed
                            || route.configured || route.frames > 0) { drop(c, this); return; }
                    Object mode = route == null ? null : route.mode.get();
                    if (mode != null && OpticalZoom.field(mode, "d") != null) { drop(c, this); return; }
                    Object camera = mode == null ? null : OpticalZoom.field(mode, "g");
                    boolean connected = camera != null && Integer.valueOf(2).equals(OpticalZoom.field(camera, "I"))
                            && OpticalZoom.field(camera, "M") == mode
                            && OpticalZoom.field(camera, "K") == OpticalZoom.field(mode, "j");
                    boolean nativeInProgress = calls.containsKey(c);
                    if (nativeInProgress || route == null || mode == null || !c.p.get()
                            || OpticalZoom.field(mode, "j") == null || !connected || !sourcesReady(c)
                            || !RearRestart1926.recoveryWindowReady()
                            || ManualLens170.yes("recording") || ExitBusy1921.captureBusy(false)) {
                        schedule(this, POLL_MS); return;
                    }
                    replays++;
                    enter(c);
                }
                // Native API serializes its own provider/start operations. No helper lock
                // is held during native work or callbacks, and no GL texture is replaced.
                try {
                    int result = c.startPreview();
                    Log.i("ULikeStart1927", "Replayed prepared rear preview once: " + result);
                    remember(c, result);
                } finally { leave(c); }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                drop(c, this);
                Log.w("ULikeStart1927", "Stopped optional preview replay", error);
            }
        }
    }
}
