package com.hiro.ulike;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import com.bytedance.corecamera.ui.view.CameraShadeView;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Accepted-photo feedback replaces the native white flash. A drawable in the
 * existing shade overlay never takes focus, dispatches touch, or enters the
 * image pipeline. The live visible preview bounds exclude the black bars. */
public final class ShutterFeedback1940 {
    private ShutterFeedback1940() {}
    static final long HOLD_MS = 90L, DURATION_MS = 240L;
    private static final WeakHashMap<CameraShadeView, Pulse> active = new WeakHashMap<>();

    public static boolean show(View nativeFlash) {
        if (Looper.myLooper() != Looper.getMainLooper()) return false;
        try { return showOnUi(nativeFlash); }
        catch (Throwable unavailable) {
            // The native caller keeps its original animation as a fallback.
            return false;
        }
    }
    private static boolean showOnUi(View nativeFlash) {
        CameraShadeView shade = PreviewLayout1922.gestureView1925();
        if (shade == null || !shade.isAttachedToWindow() || !shade.isShown()
                || !shade.hasWindowFocus() || shade.getWidth() <= 0 || shade.getHeight() <= 0
                || nativeFlash == null || shade.getRootView() != nativeFlash.getRootView()) return false;
        RectF rect = PreviewLayout1922.viewport();
        if (rect != null && shade.w != null && shade.n != null && shade.o != null)
            rect = new RectF(rect.left, shade.n.bottom, rect.right, shade.o.top);
        if (!valid(rect, shade.getWidth(), shade.getHeight())) return false;
        Pulse pulse = active.get(shade);
        boolean fresh = pulse == null;
        if (fresh) pulse = new Pulse(shade);
        boolean committed = false;
        try {
            if (fresh) {
                active.put(shade, pulse);
                shade.getOverlay().add(pulse);
            }
            shade.removeCallbacks(pulse);
            pulse.rect = new RectF(rect.left, rect.top, rect.right, rect.bottom);
            pulse.started = SystemClock.uptimeMillis();
            pulse.setBounds(0, 0, shade.getWidth(), shade.getHeight());
            // A restarted pulse replaces the previous deadline, so a fast accepted
            // shot gets its own visible cue without stacking or early old cleanup.
            if (!shade.postDelayed(pulse, DURATION_MS)) return false;
            // This is the dedicated photo-capture mask, not the front-screen light.
            // Hide it only after an input-free replacement has been scheduled.
            nativeFlash.clearAnimation();
            nativeFlash.setVisibility(View.GONE);
            shade.postInvalidateOnAnimation();
            committed = true;
            return true;
        } finally {
            if (!committed) {
                active.remove(shade);
                shade.removeCallbacks(pulse);
                shade.getOverlay().remove(pulse);
            }
        }
    }

    static boolean valid(RectF r, int w, int h) {
        return r != null && r.left >= 0 && r.top >= 0 && r.right <= w && r.bottom <= h
                && r.right > r.left && r.bottom > r.top;
    }
    static float strength(long elapsed) {
        if (elapsed < 0 || elapsed >= DURATION_MS) return 0f;
        if (elapsed <= HOLD_MS) return 1f;
        return (float)(DURATION_MS - elapsed) / (float)(DURATION_MS - HOLD_MS);
    }

    private static final class Pulse extends Drawable implements Runnable {
        private final WeakReference<CameraShadeView> owner;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float width;
        private RectF rect;
        private long started;
        Pulse(CameraShadeView shade) {
            owner = new WeakReference<>(shade);
            width = Math.max(1f, shade.getResources().getDisplayMetrics().density) * 5f;
        }
        @Override public void draw(Canvas canvas) {
            CameraShadeView shade = owner.get();
            if (shade == null || !shade.isAttachedToWindow() || !shade.isShown() || rect == null) return;
            float strength = strength(SystemClock.uptimeMillis() - started);
            if (strength <= 0f) return;
            int saved = canvas.save();
            canvas.clipRect(rect);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            paint.setAlpha(Math.round(31f * strength));
            canvas.drawRect(rect, paint);
            float inset = width;
            // Black outer stroke makes the white frame visible against both
            // bright backgrounds and dark backgrounds.
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(width * 1.6f);
            paint.setAlpha(Math.round(230f * strength));
            canvas.drawRect(rect.left+inset, rect.top+inset, rect.right-inset, rect.bottom-inset, paint);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(width);
            paint.setAlpha(Math.round(255f * strength));
            canvas.drawRect(rect.left+inset, rect.top+inset, rect.right-inset, rect.bottom-inset, paint);
            canvas.restoreToCount(saved);
            shade.postInvalidateOnAnimation();
        }
        @Override public void run() {
            CameraShadeView shade = owner.get();
            if (shade == null) return;
            // Identity ownership ensures a late runnable cannot remove a newer
            // replacement pulse after lifecycle teardown or reattachment.
            if (active.get(shade) != this) return;
            long remaining = DURATION_MS - (SystemClock.uptimeMillis() - started);
            if (remaining > 0 && shade.isAttachedToWindow() && shade.isShown()) {
                shade.removeCallbacks(this);
                if (shade.postDelayed(this, remaining)) return;
            }
            active.remove(shade);
            shade.removeCallbacks(this);
            shade.getOverlay().remove(this);
            shade.invalidate();
        }
        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter filter) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
