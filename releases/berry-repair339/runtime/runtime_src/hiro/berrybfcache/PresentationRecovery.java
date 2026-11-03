package hiro.berrybfcache;

import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.WeakHashMap;

/** Bounded presentation work after independently proved restore completion. */
public final class PresentationRecovery {
    public interface Proof { boolean isCurrent(WebView view); }
    private static final long DEADLINE_MS = 3000;
    private static final long CALLBACK_TIMEOUT_MS = 700;
    private static final long CHECK_MS = 100;
    private static final int MAX_VISUAL_REQUESTS = 2;
    private static final int MAX_LAYOUT_REQUESTS = 2;
    private static final WeakHashMap<WebView, State> STATES = new WeakHashMap<WebView, State>();
    private static final WeakHashMap<WebView, Lease> LEASES = new WeakHashMap<WebView, Lease>();
    private static long nextRequest;

    // Android has no cancellation method for VisualStateCallback. Keep at most
    // one actual native request outstanding, including across newer restores.
    private static final class Lease { long request; }

    private static final class State implements Runnable, View.OnAttachStateChangeListener,
            ViewTreeObserver.OnPreDrawListener, ViewTreeObserver.OnDrawListener {
        final WeakReference<WebView> view;
        final Proof proof;
        final long deadline = SystemClock.uptimeMillis() + DEADLINE_MS;
        WeakReference<ViewTreeObserver> observer = new WeakReference<ViewTreeObserver>(null);
        boolean stopped, scheduled, preDrawRequested, preDrawReady, drawPending, drawPosted, fallbackDrawIssued;
        int stableFrames, visualAttempts, layoutAttempts;
        long visualRequest, phaseSince;
        int[] sampledGeometry, visualGeometry;
        Runnable drawAction;
        State(WebView view, Proof proof) { this.view = new WeakReference<WebView>(view); this.proof = proof; }

        @Override public void run() { scheduled = false; step(this); }
        @Override public boolean onPreDraw() {
            WebView v = view.get();
            if (v == null || !current(v, this)) return true;
            try {
                if (eligible(v) && !v.isLayoutRequested() && !inconsistent(v)) {
                    int[] geometry = geometry(v);
                    stableFrames = Arrays.equals(sampledGeometry, geometry) ? stableFrames + 1 : 1;
                    sampledGeometry = geometry;
                    preDrawReady = stableFrames >= 2;
                    preDrawRequested = false;
                }
                step(this);
            } catch (RuntimeException error) { finish(this, false); }
            return true;
        }
        @Override public void onDraw() {
            WebView v = view.get();
            if (v == null || stopped || !drawPending || drawPosted) return;
            // Android forbids modifying Views or OnDrawListener registrations
            // inside onDraw. Observe only; proof checks, UI and all cleanup run
            // after this traversal. The posted action never retains the WebView.
            final State s = this;
            try {
                final int[] observed = geometry(v);
                final boolean observedEligible = eligible(v) && !v.isLayoutRequested();
                drawAction = new Runnable() { public void run() {
                    if (s.drawAction != this) return;
                    s.drawAction = null;
                    s.drawPosted = false;
                    WebView currentView = s.view.get();
                    if (currentView == null || !current(currentView, s) || !s.drawPending) return;
                    try {
                        if (SystemClock.uptimeMillis() >= s.deadline) finish(s, false);
                        else if (observedEligible && eligible(currentView) && !currentView.isLayoutRequested()
                                && Arrays.equals(observed, s.visualGeometry) && Arrays.equals(observed, geometry(currentView)))
                            finish(s, false);
                        else { resetReadiness(s); step(s); }
                    } catch (RuntimeException error) { finish(s, false); }
                } };
                drawPosted = v.post(drawAction);
                if (!drawPosted) drawAction = null;
            } catch (RuntimeException error) { /* The bounded watchdog performs cleanup outside onDraw. */ }
        }
        @Override public void onViewAttachedToWindow(View ignored) { step(this); }
        @Override public void onViewDetachedFromWindow(View ignored) {
            resetReadiness(this);
            removeObserver(this, false);
            schedule(this);
        }
    }

    private PresentationRecovery() {}

    public static void start(final WebView view, final Proof proof) {
        if (view == null || proof == null) return;
        if (!mainThread()) {
            final WeakReference<WebView> weak = new WeakReference<WebView>(view);
            view.post(new Runnable() { public void run() { start(weak.get(), proof); } });
            return;
        }
        cancel(view);
        State s = new State(view, proof);
        STATES.put(view, s);
        try {
            view.addOnAttachStateChangeListener(s);
            step(s);
        } catch (RuntimeException error) { finish(s, false); }
    }

    public static void cancel(final WebView view) {
        if (view == null) return;
        if (!mainThread()) {
            final WeakReference<WebView> weak = new WeakReference<WebView>(view);
            view.post(new Runnable() { public void run() { cancel(weak.get()); } });
            return;
        }
        State s = STATES.get(view);
        if (s != null) finish(s, false);
    }

    private static void step(State s) {
        WebView v = s.view.get();
        if (v == null || s.stopped) return;
        try {
            if (!current(v, s)) return;
            long now = SystemClock.uptimeMillis();
            if (now >= s.deadline) { finish(s, false); return; }
            if (!v.isAttachedToWindow()) {
                resetReadiness(s); schedule(s); return;
            }
            if (!v.isShown()) {
                resetReadiness(s); schedule(s); return;
            }
            observe(v, s);
            if (v.isLayoutRequested()) {
                resetReadiness(s); schedule(s); return;
            }
            if (inconsistent(v)) {
                resetReadiness(s);
                if (s.layoutAttempts < MAX_LAYOUT_REQUESTS) {
                    s.layoutAttempts++;
                    // The completed layout disagrees with this View's own
                    // positive measured size. requestLayout follows the original
                    // parent/LayoutParams constraints; no size is assigned here.
                    v.requestLayout();
                }
                schedule(s); return;
            }
            if (v.getWidth() <= 0 || v.getHeight() <= 0 || v.getMeasuredWidth() <= 0 || v.getMeasuredHeight() <= 0) {
                resetReadiness(s); schedule(s); return;
            }
            if (s.drawPending) {
                if (!Arrays.equals(s.visualGeometry, geometry(v))) resetReadiness(s);
                else if (now - s.phaseSince >= CALLBACK_TIMEOUT_MS) {
                    if (s.visualAttempts >= MAX_VISUAL_REQUESTS) { finish(s, false); return; }
                    resetReadiness(s);
                } else { schedule(s); return; }
            }
            if (s.visualRequest != 0) {
                if (now - s.phaseSince >= CALLBACK_TIMEOUT_MS) {
                    // Keep the native lease until its real callback returns.
                    // Issuing another request here could accumulate callbacks.
                    fallbackDraw(v, s);
                    finish(s, false); return;
                }
                schedule(s); return;
            }
            if (!s.preDrawReady) {
                if (!s.preDrawRequested) {
                    s.preDrawRequested = true;
                    v.postInvalidateOnAnimation();
                }
                schedule(s); return;
            }
            Lease lease = LEASES.get(v);
            if (lease == null) { lease = new Lease(); LEASES.put(v, lease); }
            if (lease.request != 0) { fallbackDraw(v, s); schedule(s); return; }
            if (s.visualAttempts >= MAX_VISUAL_REQUESTS) { finish(s, false); return; }
            requestVisual(v, s, lease);
            schedule(s);
        } catch (RuntimeException error) { finish(s, false); }
    }

    private static void requestVisual(WebView view, final State s, final Lease lease) {
        long serial = ++nextRequest;
        if (serial <= 0) { nextRequest = 1; serial = 1; }
        final long request = serial;
        lease.request = request;
        s.visualRequest = request;
        s.phaseSince = SystemClock.uptimeMillis();
        s.visualGeometry = geometry(view);
        s.visualAttempts++;
        final WeakReference<WebView> weakView = new WeakReference<WebView>(view);
        final WeakReference<State> weakState = new WeakReference<State>(s);
        try {
            view.postVisualStateCallback(request, new WebView.VisualStateCallback() {
                @Override public void onComplete(long requestId) {
                    if (!mainThread() || requestId != request || lease.request != request) return;
                    lease.request = 0;
                    WebView v = weakView.get();
                    if (v == null) return;
                    State pending = weakState.get();
                    if (pending != null && current(v, pending) && pending.visualRequest == request) {
                        pending.visualRequest = 0;
                        try {
                            if (SystemClock.uptimeMillis() >= pending.deadline) finish(pending, false);
                            else if (eligible(v) && !v.isLayoutRequested() && Arrays.equals(pending.visualGeometry, geometry(v))) {
                                pending.drawPending = true;
                                pending.phaseSince = SystemClock.uptimeMillis();
                                v.postInvalidateOnAnimation();
                                schedule(pending);
                            } else { resetReadiness(pending); step(pending); }
                        } catch (RuntimeException error) { finish(pending, false); }
                    }
                    // An old callback can release its lease, but cannot act on
                    // the new navigation; that navigation must get its own proof.
                    State newest = STATES.get(v);
                    if (newest != null && newest != pending) step(newest);
                }
            });
        } catch (RuntimeException error) {
            if (lease.request == request) lease.request = 0;
            s.visualRequest = 0;
            finish(s, false);
        }
    }

    private static boolean current(WebView view, State s) {
        if (s.stopped || STATES.get(view) != s) return false;
        boolean valid;
        try { valid = s.proof.isCurrent(view); } catch (RuntimeException error) { valid = false; }
        if (!valid) finish(s, false);
        return valid;
    }
    private static boolean eligible(WebView view) {
        return view.isAttachedToWindow() && view.isShown() && view.getWidth() > 0 && view.getHeight() > 0
            && view.getMeasuredWidth() > 0 && view.getMeasuredHeight() > 0;
    }
    private static boolean inconsistent(WebView view) {
        return (view.getMeasuredWidth() > 0 && view.getWidth() != view.getMeasuredWidth())
            || (view.getMeasuredHeight() > 0 && view.getHeight() != view.getMeasuredHeight());
    }
    private static int[] geometry(WebView view) {
        android.view.ViewParent parent = view.getParent();
        View p = parent instanceof View ? (View) parent : null;
        return new int[] {view.getWidth(), view.getHeight(), view.getMeasuredWidth(), view.getMeasuredHeight(),
            view.getLeft(), view.getTop(), view.getRight(), view.getBottom(),
            p == null ? -1 : p.getWidth(), p == null ? -1 : p.getHeight()};
    }
    private static void fallbackDraw(WebView view, State s) {
        if (s.fallbackDrawIssued || !s.preDrawReady || !eligible(view) || view.isLayoutRequested()
                || !Arrays.equals(s.sampledGeometry, geometry(view))) return;
        s.fallbackDrawIssued = true;
        // A safe redraw opportunity, not synthetic visual readiness or success.
        view.postInvalidateOnAnimation();
    }
    private static void resetReadiness(State s) {
        s.stableFrames = 0;
        s.sampledGeometry = null;
        s.preDrawReady = false;
        s.preDrawRequested = false;
        s.drawPending = false;
        // The outstanding native lease remains held until the real callback.
        s.visualRequest = 0;
    }
    private static void observe(WebView view, State s) {
        ViewTreeObserver observer = view.getViewTreeObserver();
        if (s.observer.get() == observer) return;
        removeObserver(s, false);
        if (observer == null || !observer.isAlive()) return;
        observer.addOnPreDrawListener(s);
        observer.addOnDrawListener(s);
        s.observer = new WeakReference<ViewTreeObserver>(observer);
        s.preDrawReady = false;
        s.stableFrames = 0;
    }
    private static void schedule(State s) {
        WebView view = s.view.get();
        if (s.stopped || s.scheduled || view == null) return;
        s.scheduled = view.postDelayed(s, CHECK_MS);
        if (!s.scheduled) finish(s, false);
    }
    private static void finish(final State s, boolean deferObserverRemoval) {
        if (s.stopped) return;
        s.stopped = true;
        WebView view = s.view.get();
        if (view != null) {
            if (STATES.get(view) == s) STATES.remove(view);
            view.removeCallbacks(s);
            if (s.drawAction != null) view.removeCallbacks(s.drawAction);
            view.removeOnAttachStateChangeListener(s);
        }
        s.scheduled = false;
        s.visualRequest = 0;
        s.drawPending = false;
        s.drawAction = null;
        s.drawPosted = false;
        removeObserver(s, deferObserverRemoval);
    }
    private static void removeObserver(final State s, boolean defer) {
        final ViewTreeObserver observer = s.observer.get();
        s.observer = new WeakReference<ViewTreeObserver>(null);
        if (observer == null || !observer.isAlive()) return;
        if (defer) {
            WebView view = s.view.get();
            if (view != null) {
                final WeakReference<ViewTreeObserver> weak = new WeakReference<ViewTreeObserver>(observer);
                view.post(new Runnable() { public void run() { remove(weak.get(), s); } });
                return;
            }
        }
        remove(observer, s);
    }
    private static void remove(ViewTreeObserver observer, State s) {
        if (observer == null || !observer.isAlive()) return;
        try { observer.removeOnPreDrawListener(s); observer.removeOnDrawListener(s); }
        catch (RuntimeException unavailable) { /* Detached/dead observers need no further work. */ }
    }
    private static boolean mainThread() { return Looper.myLooper() == Looper.getMainLooper(); }
}
