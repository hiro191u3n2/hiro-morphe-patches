package com.hiro.ulike;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.WeakHashMap;

/** Deliver accepted native facing changes to the added lens overlay immediately.
 * SurfaceView frames do not guarantee an Android View-tree traversal, so the
 * existing onPreDraw guard alone cannot remove an already visible overlay.
 * This helper never changes the native facing selection or camera lifecycle.
 */
public final class LensVisibility1936 {
    private static final WeakHashMap<View, Boolean> ROOTS = new WeakHashMap<View, Boolean>();
    private LensVisibility1936() {}

    public static void register(Object candidate) {
        if (!(candidate instanceof OpticalZoomUi.Bar)) return;
        final View root = ((OpticalZoomUi.Bar) candidate).root;
        if (root == null) return;
        synchronized (ROOTS) { ROOTS.put(root, Boolean.TRUE); }
        // Newly installed/recreated bars must also start hidden for front.
        if (!RearLensUi1930.rearSelected()) root.setVisibility(View.GONE);
    }

    public static void acceptedSwitch(i.f.l.n.q.y.c provider, boolean front) {
        if (provider == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            refresh(provider);
        } else {
            // Native callers normally use the main thread. Re-read the live
            // selection when a queued callback executes; never replay a stale
            // front/rear Boolean over a newer accepted switch.
            final WeakReference<i.f.l.n.q.y.c> source = new WeakReference<i.f.l.n.q.y.c>(provider);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override public void run() {
                    i.f.l.n.q.y.c current = source.get();
                    if (current != null) refresh(current);
                }
            });
        }
    }

    private static void refresh(i.f.l.n.q.y.c provider) {
        try {
            // Ignore a switch from a retired scene/provider. Read the same
            // session + scene that native g0 just wrote, not cached route data.
            if (provider.r != i.f.l.n.q.y.m.b || provider.b == null
                    || !provider.b.equals(i.f.l.n.q.y.m.c)) return;
            i.f.l.u.g camera = provider.r.g(provider.b);
            if (camera == null) return;
            i.f.l.u.j state = camera.k();
            if (state == null) return;
            i.f.l.u.p<?> facing = state.x();
            if (facing == null) return;
            Object selected = facing.a();
            boolean rear = Boolean.FALSE.equals(selected);
            ArrayList<View> roots;
            synchronized (ROOTS) { roots = new ArrayList<View>(ROOTS.keySet()); }
            for (View root : roots) {
                if (root == null) continue;
                if (!rear) root.setVisibility(View.GONE);
                // A rear switch schedules the existing guarded updater. Do
                // not show the bar before the rear route itself is ready.
                root.requestLayout();
                root.invalidate();
            }
        } catch (RuntimeException unavailableState) {
            // A transient retired provider does not alter another screen.
        } catch (LinkageError unavailableBridge) {
            // Existing onPreDraw/read-only eligibility checks remain active.
        }
    }
}
