package com.hiro.ulike;

import android.animation.Animator;
import android.animation.ValueAnimator;
import com.bytedance.corecamera.ui.view.CameraShadeView;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Own each shade transition until completion. No camera, bitmap or save-path changes.
 * Called on the native View/UI thread. Delayed starts capture an exact animator,
 * so a late first-frame task can never start a newer layout's animation.
 */
public final class PreviewLayout1922 {
    private static final WeakHashMap<CameraShadeView, Pending> pending = new WeakHashMap<>();
    private PreviewLayout1922() {}
    // A rendering rectangle must use the same coordinate system as its shade view.
    private static WeakReference<CameraShadeView> active = new WeakReference<>(null);
    private static final WeakHashMap<CameraShadeView, android.graphics.RectF> delivered = new WeakHashMap<>();
    public static void prepare(CameraShadeView view) {
        cancel(view);
        if (view!=null) active=new WeakReference<>(view);
    }
    public static int localWidth(CameraShadeView view) {
        int width=view.getWidth();
        if(width<=0) width=view.c;
        return width>0?width:view.getResources().getDisplayMetrics().widthPixels;
    }
    public static int localHeight(CameraShadeView view) {
        int height=view.getHeight();
        if(height<=0) height=view.j;
        return height>0?height:view.getResources().getDisplayMetrics().heightPixels;
    }
    public static android.graphics.RectF viewport() {
        CameraShadeView view=active.get();
        if(view==null || view.c<=0 || view.j<=0 || view.v==null || view.u<0 || view.t<0
                || view.u+view.t>=view.j) return null;
        int left=CameraShadeView.d0, right=CameraShadeView.e0;
        if(left<0 || right<0 || left+right>=view.c) return null;
        return new android.graphics.RectF(left,view.u,view.c-right,view.j-view.t);
    }
    public static void detached(CameraShadeView view) {
        cancel(view); delivered.remove(view);
        if(active.get()==view)active=new WeakReference<>(null);
    }
    public static void afterLayout(CameraShadeView view) {
        if(view==null || view.c<=0 || view.j<=0)return;
        active=new WeakReference<>(view);
        reconcile(view); ready(view);
    }
    private static void notifySettled(CameraShadeView view) {
        if(view.w!=null || view.v==null || view.c<=0 || view.j<=0)return;
        active=new WeakReference<>(view);
        android.graphics.RectF rect=viewport();
        if(rect==null)return;
        android.graphics.RectF previous=delivered.get(view);
        if(rect.equals(previous))return;
        delivered.put(view,rect); // Mark before native callbacks, which may re-enter.
        view.b();
    }


    private static final class Pending {
        final WeakReference<CameraShadeView> view;
        final WeakReference<ValueAnimator> animation;
        final Runnable fallback;
        final Runnable firstFrame;
        boolean framePosted;
        Pending(CameraShadeView v, ValueAnimator a) {
            view = new WeakReference<>(v);
            animation = new WeakReference<>(a);
            fallback = new Runnable() { public void run() { fire(Pending.this); } };
            firstFrame = new Runnable() { public void run() { fire(Pending.this); } };
        }
    }
    private static void removePending(CameraShadeView view) {
        Pending p;
        synchronized (pending) { p = pending.remove(view); }
        if (p != null) {
            view.removeCallbacks(p.fallback);
            view.removeCallbacks(p.firstFrame);
        }
    }
    /** Runs before native target calculation and when the shade detaches. */
    public static void cancel(CameraShadeView view) {
        if (view == null) return;
        removePending(view);
        ValueAnimator a = view.w;
        view.w = null; // invalidate ownership BEFORE any cancel callback
        if (a != null) {
            a.removeAllUpdateListeners();
            a.removeAllListeners();
            a.cancel();
        }
    }
    /** Installs a bounded fallback only for a transition waiting for a first frame. */
    public static void ready(CameraShadeView view) {
        if (view == null) return;
        if (view.w == null) { reconcile(view); notifySettled(view); return; }
        if (view.w.isStarted()) return;
        synchronized (pending) {
            Pending p = pending.get(view);
            if (p != null && p.animation.get() == view.w) return;
        }
        removePending(view);
        Pending p = new Pending(view, view.w);
        synchronized (pending) { pending.put(view, p); }
        view.postDelayed(p.fallback, 500L);
    }
    /** Replace the native anonymous, unbound 200ms first-frame callback. */
    public static void schedule(CameraShadeView view) {
        ready(view);
        Pending p;
        synchronized (pending) {
            p = pending.get(view);
            if (p == null || p.framePosted) return;
            p.framePosted = true;
        }
        view.postDelayed(p.firstFrame, 200L);
    }
    private static void fire(Pending p) {
        CameraShadeView view = p.view.get();
        ValueAnimator a = p.animation.get();
        if (view == null || a == null) return;
        synchronized (pending) { if (pending.get(view) != p) return; }
        if (view.w != a) { removePending(view); return; }
        startCurrent(view);
    }
    public static void startCurrent(CameraShadeView view) {
        if (view == null) return;
        ValueAnimator a = view.w;
        removePending(view);
        if (a != null && !a.isStarted()) a.start();
        // Do NOT clear view.w here; native code formerly lost the cancellation handle.
    }
    public static boolean owns(CameraShadeView view, ValueAnimator a) {
        return view != null && a != null && view.w == a;
    }
    /** Stale end events cannot notify camera clients or modify the current target. */
    public static boolean finish(CameraShadeView view, Animator a) {
        if (view == null || a == null || view.w != a) return false;
        removePending(view);
        view.w = null;
        reconcile(view);
        return true; // keep the original native completion/listener notifications
    }
    /** One-shot repair when settled rectangles differ from the native target.
     * Never touches an active/pending transition or chooses a guessed screen offset.
     */
    public static void reconcile(CameraShadeView view) {
        if (view == null || view.w != null || view.v == null || view.c <= 0 || view.j <= 0
                || view.n == null || view.o == null) return;
        if (active.get()==view) {
            CameraShadeView.Q=view.u; CameraShadeView.R=view.t; CameraShadeView.S=view.j;
            CameraShadeView.U=view.v;
        }
        float top = view.u;
        String ratio = view.v.name();
        if (ratio.equals("RADIO_1_1") || ratio.equals("RADIO_ROUND")) top += 1f;
        if (view.r != view.u || view.s != view.t || view.n.bottom != top
                || view.o.top != view.j - view.t) {
            view.r = view.u;
            view.s = view.t;
            view.i();
        }
    }
    /** Inset changes must invalidate the target, not merely store A. */
    public static void offsetChanged(CameraShadeView view) {
        if (view == null || view.c <= 0 || view.j <= 0 || view.v == null
                || view.n == null || view.o == null) return;
        view.m(view.v, false, view.G, false);
    }
}
