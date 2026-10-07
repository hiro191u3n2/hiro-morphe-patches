package com.hiro.ulike;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import com.ss.android.vesdk.VECameraCapture;
import com.ss.android.vesdk.frame.TECapturePipeline;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Complete a front preview start through its first owned image callback.
 * A ticket never opens a camera, selects a lens, or fabricates a GL object.
 * The native client, settings, foreground generation and opened device own it;
 * consequently Camera1 is covered without an OpticalZoom Camera2 route.
 */
public final class FrontPreview1936 {
    static final long WAIT_MS = 5000L;
    static final long POLL_MS = 100L;
    static final long FRAME_GRACE_MS = 2000L;
    static final long MIN_RESTART_REMAINING_MS = 1500L;
    static final int MAX_REPLAYS = 2;
    static final int MAX_RESUME_ATTEMPTS = 2;
    private static final String TAG = "ULikeFront1936";
    private static final Object LOCK = new Object();
    private static final Map<VECameraCapture, Pending> pending = new WeakHashMap<>();
    private static final Map<VECameraCapture, Integer> calls = new WeakHashMap<>();
    private static final Map<VECameraCapture, Long> generations = new WeakHashMap<>();
    private static final Map<VECameraCapture, Long> recovered = new WeakHashMap<>();
    private static final Map<VECameraCapture, Long> expired = new WeakHashMap<>();
    private static long nextGeneration;
    private static volatile boolean hasPending;
    private static final int MISSING = 0, READY = 1, RENEW = 2;
    private FrontPreview1936() {}

    private static Object get(Object object, String name) throws ReflectiveOperationException {
        return ProviderLifecycle1929.get(object, name);
    }
    private static Object call(Object object, String name) throws ReflectiveOperationException {
        return ProviderLifecycle1929.call(object, name);
    }
    private static Object optional(Object object, String name) throws ReflectiveOperationException {
        try { return get(object, name); }
        catch (NoSuchFieldException absent) { return null; }
    }
    private static int number(Object object, String name) throws ReflectiveOperationException {
        Object value = get(object, name);
        return value instanceof Number ? ((Number) value).intValue() : Integer.MIN_VALUE;
    }
    private static Object server() throws ReflectiveOperationException {
        return Class.forName("i.s.a.w.q").getField("INSTANCE").get(null);
    }
    private static boolean front(VECameraCapture capture) throws ReflectiveOperationException {
        if (capture == null || number(get(capture, "c"), "l") != 1) return false;
        Object facing = call(get(capture, "a"), "getCameraFacing");
        return facing instanceof Enum && ((Enum<?>) facing).name().equals("FACING_FRONT");
    }
    private static boolean isFront(VECameraCapture capture) {
        try { return front(capture); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) { return false; }
    }
    private static boolean owned(VECameraCapture capture) throws ReflectiveOperationException {
        Object owner = ManualLens170.get("capture");
        return capture != null && owner instanceof WeakReference
                && ((WeakReference<?>) owner).get() == capture
                && get(capture, "d") != null
                && ManualLens170.yes("foreground") && front(capture);
    }
    private static boolean busy() throws ReflectiveOperationException {
        return ManualLens170.yes("recording") || ExitBusy1921.captureBusy(false);
    }
    private static Object hardware(Object camera) throws ReflectiveOperationException {
        Object device = optional(camera, "K");
        if (device instanceof CameraDevice) return device;
        device = optional(camera, "H");
        return device instanceof Camera ? device : null;
    }

    /** READY and RENEW are usable by native startPreview. RENEW requests the
     * existing newSurfaceTexture flag; startPreview creates/notifies on that path.
     */
    private static int inputs(VECameraCapture capture) throws ReflectiveOperationException {
        if (capture.n == null || capture.n.isEmpty()) return MISSING;
        boolean valid = false, renew = false;
        for (TECapturePipeline pipeline : capture.n.getImmutableList()) {
            if (pipeline == null || !pipeline.isPreview()) continue;
            if (!pipeline.isValid()) return MISSING;
            Object format = call(pipeline, "getFormat");
            String name = format instanceof Enum ? ((Enum<?>) format).name() : "";
            Object texture = call(pipeline, "getSurfaceTexture");
            boolean released = texture instanceof SurfaceTexture
                    && ((SurfaceTexture) texture).isReleased();
            if (name.equals("PIXEL_FORMAT_OpenGL_OES")) {
                if (texture instanceof SurfaceTexture) renew |= released;
                else {
                    Object surface = call(pipeline, "getSurface");
                    if (!(surface instanceof Surface) || !((Surface) surface).isValid()) return MISSING;
                }
            } else if (name.equals("PIXEL_FORMAT_Recorder")) {
                Object surface = call(pipeline, "getRecorderSurface");
                if (released || !(surface instanceof Surface) || !((Surface) surface).isValid()) return MISSING;
            }
            valid = true;
        }
        return valid ? (renew ? RENEW : READY) : MISSING;
    }
    private static boolean nativeBlocked(Object host) throws ReflectiveOperationException {
        return nativeEnded(host) || nativeTransition(host);
    }
    private static boolean nativeEnded(Object host) throws ReflectiveOperationException {
        return Boolean.TRUE.equals(get(host, "mIsCameraPendingClose"))
                || Boolean.TRUE.equals(get(host, "mHandlerDestroyed"));
    }
    private static boolean nativeTransition(Object host) throws ReflectiveOperationException {
        return Boolean.TRUE.equals(get(host, "mIsCameraSwitchState"))
                || Boolean.TRUE.equals(get(host, "mOnBackGround"));
    }
    /** External Surface-only and recording pipelines do not necessarily deliver
     * provider image callbacks. Never infer their failure from an absent callback. */
    private static boolean texturePreview(VECameraCapture capture) throws ReflectiveOperationException {
        if (capture.n == null || capture.n.isEmpty()) return false;
        boolean found = false;
        for (TECapturePipeline pipeline : capture.n.getImmutableList()) {
            if (pipeline == null || !pipeline.isPreview()) continue;
            Object format = call(pipeline, "getFormat");
            if (!pipeline.isValid() || !(format instanceof Enum)
                    || !((Enum<?>) format).name().equals("PIXEL_FORMAT_OpenGL_OES")
                    || !(call(pipeline, "getSurfaceTexture") instanceof SurfaceTexture)) return false;
            found = true;
        }
        return found;
    }
    private static boolean nativeReady(VECameraCapture capture, Object host) throws ReflectiveOperationException {
        Object settings = get(capture, "c"), client = get(capture, "o");
        if (settings == null || client == null || get(host, "mCameraClient") != client
                || get(host, "mCameraSettings") != settings || nativeBlocked(host)) return false;
        int state = number(host, "mCurrentCameraState");
        return (state == 2 || state == 3) && hardware(get(host, "mCameraInstance")) != null;
    }
    private static boolean notReady(VECameraCapture capture) {
        try {
            return !capture.p.get() || inputs(capture) != READY || !nativeReady(capture, server());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
            // An unsupported ABI is not evidence of a retryable camera failure.
            return false;
        }
    }

    /** Hook at native startPreview entry. Rear startup keeps its existing helper.
     * A front texture released by the previous lifetime is renewed by the native
     * pipeline before a provider/session is built from it.
     */
    public static boolean before(Object object) {
        if (!(object instanceof VECameraCapture)) return PreviewInputs1929.before(object);
        VECameraCapture capture = (VECameraCapture) object;
        if (!isFront(capture)) return PreviewInputs1929.before(object);
        try {
            if (!owned(capture)) return true;
            if (!capture.p.get() || !nativeReady(capture, server())) return false;
            int input = inputs(capture);
            if (input == MISSING) return false;
            if (input == RENEW) {
                if (busy()) return false;
                capture.newSurfaceTexture();
                Log.i(TAG, "Front preview texture will be renewed by the native renderer");
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
            Log.w(TAG, "Front input guard unavailable; native startup retained", unsupported);
            return true;
        }
    }

    private static void enter(VECameraCapture capture) {
        synchronized (LOCK) {
            Integer count = calls.get(capture);
            calls.put(capture, count == null ? 1 : count + 1);
        }
    }
    private static void leave(VECameraCapture capture) {
        synchronized (LOCK) {
            Integer count = calls.get(capture);
            if (count == null || count <= 1) calls.remove(capture);
            else calls.put(capture, count - 1);
        }
    }
    private static long generation(VECameraCapture capture) {
        synchronized (LOCK) {
            Long value = generations.get(capture);
            return value == null ? 0L : value.longValue();
        }
    }
    private static void cancelGeneration(VECameraCapture capture, long token) {
        synchronized (LOCK) { if (generation(capture) == token) cancel(capture); }
    }
    /** Preserve the caller's native result and exceptions. Observe asynchronous
     * acceptance through an image, while retaining the bounded missing-input replay. */
    public static int start(VECameraCapture capture) {
        if (!isFront(capture)) {
            cancel(capture);
            return PreviewStart1927.start(capture);
        }
        long token = generation(capture);
        boolean missing = notReady(capture);
        enter(capture);
        try {
            // Native work may execute synchronously on its handler, including
            // the first image callback. Observe before calling it, not afterward.
            Pending watched = observe(capture, token);
            synchronized (LOCK) { if (watched != null && watched.resumeRequired) watched.frameSeen = false; }
            int result = capture.startPreview();
            if (generation(capture) == token) {
                synchronized (LOCK) {
                    Pending ticket = pending.get(capture);
                    if (result == 0 && ticket != null && ticket.generation == token
                            && generation(capture) == token) ticket.acceptedAt = SystemClock.uptimeMillis();
                }
                completed(capture, result, missing || notReady(capture), token);
            }
            return result;
        } catch (RuntimeException | Error error) {
            cancelGeneration(capture, token);
            throw error;
        } finally { leave(capture); }
    }
    private static void completed(VECameraCapture capture, int result, boolean missing, long token) {
        synchronized (LOCK) {
            Pending ticket = pending.get(capture);
            if (generation(capture) != token || ticket != null && ticket.generation != token) return;
            if (result == 0 && ticket != null && ticket.resumeRequired) {
                ticket.resumeRequired = false;
                if (result == 0 && ticket.frameSeen) { cancel(capture); return; }
            }
        }
        remember(capture, result, missing, token);
    }
    private static void remember(VECameraCapture capture, int result, boolean missing, long token) {
        if (generation(capture) != token) return;
        if (result != 0 && (!missing || (result != -100 && result != -105 && result != -1))) {
            cancelGeneration(capture, token); return;
        }
        try {
            if (!owned(capture)) { cancelGeneration(capture, token); return; }
            if (result == 0 && !texturePreview(capture)) { cancelGeneration(capture, token); return; }
            Pending ticket = observe(capture, token);
            if (ticket == null) return;
            synchronized (LOCK) {
                if (ticket.cancelled || pending.get(capture) != ticket) return;
                if (result == 0) {
                    // q.start() returns zero after posting asynchronous work.
                    // It is acceptance, not evidence that a frame was delivered.
                    if (!ticket.awaitingFrame) ticket.acceptedAt = SystemClock.uptimeMillis();
                    ticket.awaitingFrame = true;
                } else ticket.awaitingFrame = false;
                schedule(ticket, POLL_MS);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
            cancelGeneration(capture, token);
            Log.w(TAG, "Optional front startup tracking unavailable", unsupported);
        }
    }
    private static Pending observe(VECameraCapture capture, long token) {
        try {
            if (generation(capture) != token || !owned(capture)) return null;
            Object client = get(capture, "o"), settings = get(capture, "c");
            if (client == null || settings == null) return null;
            Object host = server();
            if (nativeEnded(host)) { cancelGeneration(capture, token); return null; }
            long epoch = ManualLens170.number("epoch");
            synchronized (LOCK) {
                if (generation(capture) != token) return null;
                Long oldExpiry = expired.get(capture);
                if (oldExpiry != null && oldExpiry.longValue() == token) return null;
                Pending ticket = pending.get(capture);
                if (ticket != null && (ticket.generation != token || ticket.epoch != epoch || ticket.client != client || ticket.settings != settings)) {
                    drop(capture, ticket); ticket = null;
                }
                if (ticket == null) {
                    ticket = new Pending(capture, client, settings, epoch, token, SystemClock.uptimeMillis());
                    ticket.snapshot(host);
                    pending.put(capture, ticket);
                    hasPending = true;
                    if (!OpticalZoom.MAIN.postDelayed(ticket.timeout, WAIT_MS)) {
                        drop(capture, ticket); return null;
                    }
                    Log.i(TAG, "Front preview waits for its native inputs and first image");
                }
                schedule(ticket, POLL_MS);
                return ticket;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
            cancelGeneration(capture, token);
            Log.w(TAG, "Optional front startup retry unavailable", unsupported);
            return null;
        }
    }
    public static void pipelines(VECameraCapture capture) {
        PreviewStart1927.pipelines(capture);
        synchronized (LOCK) {
            Pending ticket = pending.get(capture);
            if (ticket != null) schedule(ticket, 0L);
        }
    }
    public static void cancel(Object object) {
        if (!(object instanceof VECameraCapture)) return;
        VECameraCapture capture = (VECameraCapture) object;
        synchronized (LOCK) {
            generations.put(capture, Long.valueOf(++nextGeneration));
            Pending ticket = pending.get(capture);
            if (ticket != null) drop(capture, ticket);
        }
    }
    private static void drop(VECameraCapture capture, Pending ticket) {
        synchronized (LOCK) {
            ticket.cancelled = true;
            if (pending.get(capture) == ticket) {
                if (generation(capture) == ticket.generation && SystemClock.uptimeMillis() >= ticket.deadline)
                    expired.put(capture, Long.valueOf(ticket.generation));
                pending.remove(capture);
            }
            hasPending = !pending.isEmpty();
            OpticalZoom.MAIN.removeCallbacks(ticket);
            OpticalZoom.MAIN.removeCallbacks(ticket.timeout);
            if (ticket.handler != null) ticket.handler.removeCallbacks(ticket.work);
        }
    }
    private static void schedule(Pending ticket, long delay) {
        synchronized (LOCK) {
            VECameraCapture capture = ticket.capture.get();
            if (capture == null || ticket.cancelled || pending.get(capture) != ticket || ticket.queued) return;
            OpticalZoom.MAIN.removeCallbacks(ticket);
            if (!OpticalZoom.MAIN.postDelayed(ticket, delay)) drop(capture, ticket);
        }
    }

    /** Runs before CameraManager.openCamera, while settings are still private
     * to this opening. A front ordinary Camera2 session waits for actual outputs
     * even if a remote VEConfig enabled the optional deferred-surface path.
     */
    public static void prepared(Object mode, int result) {
        RearRestart1926.prepared(mode, result);
        if (mode == null || result != 0) return;
        try {
            Object reference = ManualLens170.get("capture");
            Object object = reference instanceof WeakReference ? ((WeakReference<?>) reference).get() : null;
            if (!(object instanceof VECameraCapture)) return;
            VECameraCapture capture = (VECameraCapture) object;
            if (!owned(capture)) return;
            Object settings = get(mode, "h"), host = server();
            if (settings == null || settings != get(capture, "c") || settings != get(host, "mCameraSettings")
                    || get(host, "mCameraClient") != get(capture, "o")
                    || get(mode, "g") != get(host, "mCameraInstance")
                    || number(settings, "l") != 1 || number(settings, "j") != 2) return;
            int cameraMode = number(settings, "M");
            Object characteristics = get(mode, "a");
            if (!(characteristics instanceof CameraCharacteristics)
                    || !Integer.valueOf(0).equals(((CameraCharacteristics) characteristics).get(CameraCharacteristics.LENS_FACING))) return;
            if ((cameraMode == 0 || cameraMode == 1) && Boolean.TRUE.equals(get(settings, "u0"))) {
                ProviderLifecycle1929.put(settings, "u0", Boolean.FALSE);
                Log.i(TAG, "Front Camera2 session will wait for its concrete preview outputs");
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
            Log.w(TAG, "Front session startup policy unchanged", unsupported);
        }
    }

    /** A real frame to the current preview listener ends pending startup work.
     * An old provider or a dummy listener cannot declare the new preview ready.
     */
    public static void pixel(Object provider, Object frame) {
        PreviewInputs1929.pixel(provider, frame);
        if (!hasPending || provider == null || frame == null) return;
        try {
            Object reference = ManualLens170.get("capture");
            Object object = reference instanceof WeakReference ? ((WeakReference<?>) reference).get() : null;
            if (!(object instanceof VECameraCapture)) return;
            VECameraCapture capture = (VECameraCapture) object;
            Pending ticket;
            synchronized (LOCK) { ticket = pending.get(capture); }
            if (ticket == null || !ticket.current(capture)) return;
            Object host = server(), camera = get(host, "mCameraInstance");
            if (!nativeReady(capture, host) || get(provider, "d") != camera
                    || call(get(host, "mProviderManager"), "h") != provider
                    || Boolean.TRUE.equals(get(provider, "h")) || !Boolean.TRUE.equals(get(provider, "e"))) return;
            if (ticket.camera != null && (ticket.camera != camera || ticket.device != hardware(camera))) return;
            Object listener = get(provider, "a");
            if (listener == null || listener == get(provider, "j") || listener == get(provider, "k") || capture.n == null) return;
            for (TECapturePipeline pipeline : capture.n.getImmutableList()) {
                if (pipeline != null && pipeline.isPreview() && pipeline.isValid()
                        && call(pipeline, "getCaptureListener") == listener
                        && (call(pipeline, "getSurfaceTexture") == null
                            || call(pipeline, "getSurfaceTexture") == call(provider, "f"))) {
                    synchronized (LOCK) {
                        if (ticket.cancelled || pending.get(capture) != ticket) return;
                        if (ticket.resumeRequired) {
                            // Native stop can synchronously deliver a final old
                            // frame. It must not cancel the required matching start.
                            ticket.frameSeen = true;
                        } else {
                            // Prevent a synchronous start from re-arming completion.
                            cancel(capture);
                        }
                    }
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Frame delivery must never fail because optional retry tracking failed.
        }
    }

    private static final class Pending implements Runnable {
        final WeakReference<VECameraCapture> capture;
        final Object client, settings;
        final long epoch, generation, deadline;
        final Runnable work = new Runnable() { @Override public void run() { runNative(); } };
        final Runnable timeout = new Runnable() {
            @Override public void run() {
                VECameraCapture value = capture.get();
                synchronized (LOCK) {
                    // A native stop admitted inside the window can finish later.
                    // Its matching safe start is completion of the same operation.
                    if (nativeRecovery) return;
                    if (value != null && pending.get(value) == Pending.this) {
                        expired.put(value, Long.valueOf(generation));
                        drop(value, Pending.this);
                    }
                }
            }
        };
        volatile boolean cancelled, queued;
        volatile Handler handler;
        volatile Object camera, device;
        boolean ownerBound;
        int replays, resumeAttempts;
        boolean awaitingFrame;
        boolean resumeRequired, frameSeen;
        boolean nativeRecovery;
        long acceptedAt;
        Pending(VECameraCapture capture, Object client, Object settings, long epoch, long generation, long now) {
            this.capture = new WeakReference<>(capture);
            this.client = client; this.settings = settings; this.epoch = epoch; this.generation = generation; this.deadline = now + WAIT_MS;
        }
        /** If the failed start already had an opened device, that exact device
         * owns the ticket even before the first delayed/native callback runs.
         */
        void snapshot(Object host) throws ReflectiveOperationException {
            if (get(host, "mCameraClient") != client || get(host, "mCameraSettings") != settings) return;
            ownerBound = true;
            Object thread = get(host, "mHandler");
            if (thread instanceof Handler) handler = (Handler) thread;
            Object liveCamera = get(host, "mCameraInstance"), liveDevice = hardware(liveCamera);
            if (liveCamera != null && liveDevice != null) { camera = liveCamera; device = liveDevice; }
        }
        boolean current(VECameraCapture capture) throws ReflectiveOperationException {
            synchronized (LOCK) {
                if (cancelled || pending.get(capture) != this || FrontPreview1936.generation(capture) != generation) return false;
            }
            return owned(capture) && get(capture, "o") == client && get(capture, "c") == settings
                    && ManualLens170.number("epoch") == epoch;
        }
        boolean exhausted() { return resumeRequired ? resumeAttempts >= MAX_RESUME_ATTEMPTS : replays >= MAX_REPLAYS; }
        @Override public void run() {
            VECameraCapture capture = this.capture.get();
            if (capture == null) return;
            try {
                if (!current(capture) || SystemClock.uptimeMillis() >= deadline || exhausted()) {
                    drop(capture, this); return;
                }
                if (busy() || !RearRestart1926.recoveryWindowReady()) { schedule(this, POLL_MS); return; }
                Object host = server();
                if (nativeEnded(host)) { drop(capture, this); return; }
                if (nativeTransition(host)) { schedule(this, POLL_MS); return; }
                if (get(host, "mCameraClient") != client || get(host, "mCameraSettings") != settings) {
                    if (ownerBound) drop(capture, this); else schedule(this, POLL_MS);
                    return;
                }
                ownerBound = true;
                Object thread = get(host, "mHandler");
                if (!(thread instanceof Handler)) { schedule(this, POLL_MS); return; }
                Handler nativeHandler = (Handler) thread;
                synchronized (LOCK) {
                    if (cancelled || pending.get(capture) != this || queued) return;
                    if (handler != null && handler != nativeHandler) { drop(capture, this); return; }
                    handler = nativeHandler; queued = true;
                }
                if (!nativeHandler.post(work)) drop(capture, this);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
                drop(capture, this);
                Log.w(TAG, "Front preview wait stopped", unsupported);
            }
        }
        void runNative() {
            VECameraCapture capture = this.capture.get();
            if (capture == null) return;
            synchronized (LOCK) { queued = false; }
            try {
                if (!current(capture) || SystemClock.uptimeMillis() >= deadline || exhausted()) {
                    drop(capture, this); return;
                }
                Object host = server();
                if (nativeEnded(host) || get(host, "mCameraClient") != client || get(host, "mCameraSettings") != settings
                        || get(host, "mHandler") != handler) { drop(capture, this); return; }
                if (nativeTransition(host)) { schedule(this, POLL_MS); return; }
                Object liveCamera = get(host, "mCameraInstance"), liveDevice = hardware(liveCamera);
                if (camera != null && (camera != liveCamera || device != liveDevice)) { drop(capture, this); return; }
                if (camera == null && liveCamera != null && liveDevice != null) { camera = liveCamera; device = liveDevice; }
                if (!capture.p.get() || !nativeReady(capture, host) || busy()
                        || !RearRestart1926.recoveryWindowReady() || inputs(capture) == MISSING) {
                    schedule(this, POLL_MS); return;
                }
                boolean restart = awaitingFrame;
                if (restart) {
                    if (!texturePreview(capture)) { drop(capture, this); return; }
                    if (SystemClock.uptimeMillis() - acceptedAt < FRAME_GRACE_MS) {
                        schedule(this, POLL_MS); return;
                    }
                    // Camera2's native preview stop can wait up to one second.
                    // Do not begin it near the end of this observation window.
                    if (deadline - SystemClock.uptimeMillis() < MIN_RESTART_REMAINING_MS) {
                        drop(capture, this); return;
                    }
                    synchronized (LOCK) {
                        Long done = recovered.get(capture);
                        if (done != null && done.longValue() == generation) { schedule(this, POLL_MS); return; }
                    }
                }
                synchronized (LOCK) {
                    if (cancelled || pending.get(capture) != this) return;
                    if (calls.containsKey(capture)) { schedule(this, POLL_MS); return; }
                    enter(capture);
                }
                boolean missing = notReady(capture);
                try {
                    if (restart) {
                        if (Looper.myLooper() != handler.getLooper()) { drop(capture, this); return; }
                        synchronized (LOCK) {
                            if (cancelled || pending.get(capture) != this
                                    || FrontPreview1936.generation(capture) != generation) return;
                            recovered.put(capture, Long.valueOf(generation));
                            resumeRequired = true;
                            frameSeen = false;
                            nativeRecovery = true;
                        }
                        // Native stop only stops preview / closes its session. It
                        // retains the CameraDevice, provider and GL texture. Calling
                        // the client directly avoids the user-stop cancellation hook.
                        if (number(host, "mCurrentCameraState") == 3) {
                            Object result = client.getClass().getMethod("v0", Boolean.TYPE).invoke(client, Boolean.FALSE);
                            if (!(result instanceof Number) || ((Number) result).intValue() != 0) {
                                drop(capture, this); return;
                            }
                        }
                        // Stop callbacks can re-enter lifecycle code. Never restart
                        // a replaced owner/device or a newly backgrounded capture.
                        if (!current(capture) || get(host, "mCameraInstance") != camera
                                || hardware(camera) != device || get(host, "mCameraClient") != client
                                || get(host, "mCameraSettings") != settings || get(host, "mHandler") != handler
                                || nativeBlocked(host) || number(host, "mCurrentCameraState") != 2) {
                            drop(capture, this); return;
                        }
                        if (busy() || !RearRestart1926.recoveryWindowReady() || inputs(capture) != READY) {
                            if (SystemClock.uptimeMillis() >= deadline) { drop(capture, this); return; }
                            awaitingFrame = false; schedule(this, POLL_MS); return;
                        }
                        Log.i(TAG, "Front start accepted without an image; restarting its retained preview session once");
                    }
                    synchronized (LOCK) { if (resumeRequired) { frameSeen = false; resumeAttempts++; } }
                    replays++;
                    int result = capture.startPreview();
                    Log.i(TAG, "Re-delivered front preview start on its native camera thread: " + result);
                    if (current(capture)) completed(capture, result, missing || notReady(capture), generation);
                } finally {
                    leave(capture);
                    synchronized (LOCK) {
                        nativeRecovery = false;
                        if (SystemClock.uptimeMillis() >= deadline) {
                            expired.put(capture, Long.valueOf(generation));
                            drop(capture, this);
                        }
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
                drop(capture, this);
                Log.w(TAG, "Optional front preview replay stopped", unsupported);
            }
        }
    }
}
