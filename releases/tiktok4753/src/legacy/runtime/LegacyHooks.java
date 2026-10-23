/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
package app.hiro.tiktok4753.runtime;

import android.view.View;
import android.view.ViewTreeObserver;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Tiny, independent runtime. No Metra classes, activity hooks, timers or view-tree scans. */
public final class LegacyHooks {
    private LegacyHooks() {}
    private static final WeakHashMap<View, WeakReference<HiddenView>> hidden = new WeakHashMap<>();

    public static int seekbarType(int type) { return type == 3 || type == 4 ? 0 : type; }

    /** Keep only a native information/stories root hidden, including recycler rebinds. */
    public static void hide(Object candidate) {
        if (!(candidate instanceof View)) return;
        View view = (View) candidate;
        view.setVisibility(View.GONE);
        synchronized (hidden) {
            WeakReference<HiddenView> reference = hidden.get(view);
            if (reference != null && reference.get() != null) return;
            HiddenView listener = new HiddenView(view);
            hidden.put(view, new WeakReference<>(listener));
            view.addOnAttachStateChangeListener(listener);
            if (view.isAttachedToWindow()) listener.onViewAttachedToWindow(view);
        }
    }

    /** Listener lifetime follows the target view; neither map keys nor values retain it. */
    private static final class HiddenView implements View.OnAttachStateChangeListener, ViewTreeObserver.OnPreDrawListener {
        private final WeakReference<View> target;
        private ViewTreeObserver observer;
        HiddenView(View view) { target = new WeakReference<>(view); }
        public void onViewAttachedToWindow(View view) {
            view.setVisibility(View.GONE);
            detach();
            observer = view.getViewTreeObserver();
            if (observer.isAlive()) observer.addOnPreDrawListener(this);
        }
        public void onViewDetachedFromWindow(View view) { detach(); }
        private void detach() {
            if (observer != null && observer.isAlive()) observer.removeOnPreDrawListener(this);
            observer = null;
        }
        public boolean onPreDraw() {
            View view = target.get();
            if (view == null) detach();
            else if (view.getVisibility() != View.GONE) view.setVisibility(View.GONE);
            return true;
        }
    }
}
