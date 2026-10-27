package hiro.swiftkey;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewOverlay;
import android.view.ViewParent;
import android.view.inputmethod.InputConnection;
import com.touchtype.KeyboardService;
import fc0.n2;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import lh0.a;
import wb0.x;
import xd0.d1;
import xd0.f0;

/** A pointer-transparent recent-emoji picker owned by the original keyboard touch stream. */
public final class EmojiRecentsSlide {
    private static final String SENSITIVITY = "hiro_up_flick_sensitivity";
    private static final int LIMIT = 12;
    private static final float UP_SLOPE = .41421356f;
    private static SharedPreferences preferences;
    private static Stroke active;
    private static long generation;
    private static boolean cancelling;
    // Ownership outlives the editor/overlay snapshot. Late UP must never open
    // the ordinary emoji panel after a captured gesture has been cancelled.
    private static final ArrayList<Drain> drains = new ArrayList<Drain>(8);

    private EmojiRecentsSlide() { }

    public static void init(SharedPreferences value) {
        preferences = value;
        reset();
    }

    public static void reset() {
        ++generation;
        Stroke previous = active;
        active = null;
        if (previous != null) previous.dispose();
        pruneDrains();
    }

    private static float multiplier() {
        int level = 2;
        try {
            if (preferences != null) level = preferences.getInt(SENSITIVITY, 2);
        } catch (RuntimeException | LinkageError ignored) { }
        switch (level) {
            case 0: return 2f;
            case 1: return 1.5f;
            case 3: return .8f;
            case 4: return .65f;
            default: return 1f;
        }
    }

    private static boolean terminal(int action) {
        return action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL;
    }

    private static void pruneDrains() {
        for (int i = drains.size() - 1; i >= 0; i--)
            if (drains.get(i).view.get() == null) drains.remove(i);
    }

    private static boolean owned(d1 view, long downTime) {
        pruneDrains();
        for (Drain item : drains)
            if (item.view.get() == view && item.downTime == downTime) return true;
        return false;
    }

    private static void takeOwnership(Stroke stroke) {
        if (!owned(stroke.view, stroke.downTime)) {
            // Android has one active stream per touch target. A small bounded
            // collection also tolerates stale terminal events across recreated views.
            Drain next = new Drain(stroke.view, stroke.downTime);
            if (drains.size() == 8) drains.remove(0);
            drains.add(next);
        }
        stroke.captured = true;
    }

    private static void endOwnership(d1 view, long downTime) {
        for (int i = drains.size() - 1; i >= 0; i--) {
            Drain item = drains.get(i);
            if (item.view.get() == null || (item.view.get() == view && item.downTime == downTime))
                drains.remove(i);
        }
    }

    private static boolean abandon(Stroke stroke, MotionEvent event) {
        boolean consumed = stroke.captured;
        if (active == stroke) active = null;
        stroke.dispose();
        if (consumed && terminal(event.getActionMasked()))
            endOwnership(stroke.view, stroke.downTime);
        return consumed;
    }

    /** False leaves the complete original dispatcher responsible for this event. */
    public static boolean handle(d1 view, a token, MotionEvent event) {
        if (cancelling || event == null) return false;
        Stroke stroke = null;
        boolean consumed = false;
        try {
            int action = event.getActionMasked();
            long downTime = event.getDownTime();
            consumed = owned(view, downTime);
            if (action == MotionEvent.ACTION_DOWN) {
                Stroke previous = active;
                active = null;
                if (previous != null) previous.dispose();
                // Duplicate DOWN on an already captured stream is not a new tap.
                if (consumed) return true;
                if (!(view instanceof f0) || token == null || event.getPointerCount() != 1
                        || !supported(event.getToolType(0))) return false;
                active = begin((f0) view, token, event);
                return false;
            }
            stroke = active;
            if (consumed && (stroke == null || stroke.view != view
                    || stroke.downTime != downTime || !stroke.captured)) {
                if (terminal(action)) endOwnership(view, downTime);
                return true;
            }
            if (stroke == null || stroke.view != view) return false;
            consumed = stroke.captured;
            if (action == MotionEvent.ACTION_CANCEL || event.getPointerCount() != 1
                    || event.getPointerId(0) != stroke.pointer || downTime != stroke.downTime
                    || event.getEventTime() < stroke.downTime || !finite(event.getX()) || !finite(event.getY())
                    || !stroke.valid())
                return abandon(stroke, event);
            if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_UP)
                return abandon(stroke, event);

            boolean openedNow = false;
            if (!stroke.captured) {
                // Historical MOVE coordinates must not override an already due
                // native long press, even if delivery of this event was delayed.
                if (event.getEventTime() - stroke.downTime >= stroke.holdMillis
                        || SystemClock.uptimeMillis() - stroke.bornUptime >= stroke.holdMillis)
                    return abandon(stroke, event);
                for (int i = 0; i < event.getHistorySize() && !stroke.qualified; i++) {
                    if (!stroke.add(event.getHistoricalX(0, i), event.getHistoricalY(0, i)))
                        return abandon(stroke, event);
                }
                if (!stroke.add(event.getX(), event.getY())) return abandon(stroke, event);
                if (stroke.qualified) {
                    if (!capture(stroke, token, event)) return abandon(stroke, event);
                    openedNow = true;
                }
            }
            if (!stroke.captured) {
                if (action == MotionEvent.ACTION_UP) return abandon(stroke, event);
                return false;
            }
            consumed = true;
            if (!stroke.valid() || stroke.picker == null) return abandon(stroke, event);
            // A DOWN/UP-only flick cannot choose an item from a window that was
            // never visible while the finger was down. It still owns that UP.
            if (action == MotionEvent.ACTION_UP && openedNow) return abandon(stroke, event);
            stroke.picker.select(event.getX(), event.getY());
            if (action != MotionEvent.ACTION_UP) return true;

            int selected = stroke.picker.selected;
            boolean insert = token != null && selected >= 0 && stroke.editorMatches() && stroke.valid();
            EmojiRecentsBridge.Snapshot snapshot = stroke.recents;
            // Clear the UI and stream ownership before native insertion, which
            // may synchronously change editors or recreate the keyboard.
            active = null;
            stroke.dispose();
            endOwnership(view, downTime);
            // Removing the overlay or releasing parent interception can call
            // back into the keyboard. Revalidate after cleanup as well, so an
            // editor switch during that callback never receives this emoji.
            if (insert && active == null && stroke.contextValid() && stroke.editorMatches()
                    && active == null && stroke.contextValid()) snapshot.commit(token, selected);
            return true;
        } catch (Exception | LinkageError | OutOfMemoryError ignored) {
            consumed = consumed || (stroke != null && stroke.captured) || owned(view, event.getDownTime());
            if (stroke != null) {
                if (active == stroke) active = null;
                stroke.dispose();
            }
            if (consumed && terminal(event.getActionMasked())) endOwnership(view, event.getDownTime());
            return consumed;
        }
    }

    private static boolean capture(Stroke stroke, a token, MotionEvent event) {
        if (token == null || !stroke.valid() || !stroke.editorMatches()) return false;
        EmojiRecentsBridge.Snapshot recents = EmojiRecentsBridge.snapshot(stroke.model, LIMIT);
        if (recents == null) return false;
        RectF bounds = popupBounds(stroke.width, stroke.height, stroke.keyPixels,
                stroke.density, recents.size());
        if (bounds == null) return false;
        Picker picker = new Picker(stroke.view, recents, bounds, stroke.density);
        MotionEvent cancel = MotionEvent.obtain(event);
        try {
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            takeOwnership(stroke);
            cancelling = true;
            try { stroke.view.f(token, cancel); }
            finally { cancelling = false; }
        } finally { cancel.recycle(); }
        if (!stroke.valid() || active != stroke) return false;
        stroke.recents = recents;
        stroke.picker = picker;
        stroke.overlay = stroke.view.getOverlay();
        stroke.overlay.add(picker);
        stroke.interceptParent = stroke.view.getParent();
        if (stroke.interceptParent != null) stroke.interceptParent.requestDisallowInterceptTouchEvent(true);
        stroke.view.addOnAttachStateChangeListener(stroke);
        stroke.listening = true;
        if (!stroke.valid()) return false;
        picker.invalidateSelf();
        return true;
    }

    private static Stroke begin(f0 view, a token, MotionEvent event) {
        if (event.getEventTime() < event.getDownTime() || !ready(view)
                || !(view.a instanceof fb0.f0) || !(view.z instanceof x)) return null;
        KeyboardService service = service(view.getContext());
        if (service == null || service.i == null || connection(service) == null) return null;
        int type = service.i.inputType, variation = type & 0xff0;
        if ((type & 15) != 1 || variation == 128 || variation == 144 || variation == 224) return null;
        int width = view.getWidth(), height = view.getHeight();
        float px = event.getX(), py = event.getY();
        if (width <= 0 || height <= 0 || view.getWindowToken() == null
                || !finite(px) || !finite(py) || px < 0 || py < 0 || px >= width || py >= height) return null;
        n2 chosen = null;
        for (Object object : ((fb0.f0) view.a).d) {
            if (!(object instanceof n2)) continue;
            n2 key = (n2) object;
            if (key.e == null || !validRect(key.e.a)
                    || !key.e.a.contains(px / width, py / height)) continue;
            if (chosen != null) return null;
            chosen = key;
        }
        if (chosen == null) return null;
        int holdMillis = EmojiToolbar.emojiHoldMillis(chosen);
        if (holdMillis <= 0) return null;
        Stroke result = new Stroke(view, service, (x) view.z, chosen, holdMillis, event);
        return result.snapshotValid() && result.valid() ? result : null;
    }

    private static boolean supported(int tool) {
        return tool == MotionEvent.TOOL_TYPE_FINGER || tool == MotionEvent.TOOL_TYPE_STYLUS;
    }

    private static boolean finite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    private static boolean validRect(RectF rect) {
        final float epsilon = .0001f;
        return rect != null && finite(rect.left) && finite(rect.top) && finite(rect.right) && finite(rect.bottom)
                && rect.left >= -epsilon && rect.top >= -epsilon
                && rect.right <= 1 + epsilon && rect.bottom <= 1 + epsilon
                && rect.width() > 0 && rect.height() > 0;
    }

    private static boolean ready(f0 view) {
        return view != null && view.isShown() && view.isAttachedToWindow() && view.j
                && view.A != null && !view.A.b() && view.I != null && view.I.size() == 0;
    }

    private static KeyboardService service(Context context) {
        for (int i = 0; i < 8 && context != null; i++) {
            if (context instanceof KeyboardService) return (KeyboardService) context;
            if (!(context instanceof ContextWrapper)) return null;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) return null;
            context = next;
        }
        return null;
    }

    private static InputConnection connection(KeyboardService service) {
        return service.h == null ? service.getCurrentInputConnection() : service.h;
    }

    private static String text(CharSequence value) { return value == null ? null : value.toString(); }
    private static boolean equal(String a, String b) { return a == null ? b == null : a.equals(b); }

    /** Place a compact 4-column picker entirely inside the original keyboard surface. */
    static RectF popupBounds(int width, int height, RectF key, float density, int count) {
        if (width <= 0 || height <= 0 || key == null || !finite(density) || density <= 0
                || count < 0 || count > LIMIT) return null;
        float margin = 4f * density, padding = 8f * density;
        float header = 28f * density;
        int rows = Math.max(1, (count + 3) / 4);
        float popupWidth = Math.min(width - margin * 2, 4f * 48f * density + padding * 2);
        float cellHeight = Math.min(48f * density, (height - margin * 2 - header - padding * 2) / rows);
        if (popupWidth < padding * 2 + 4f * 24f * density || cellHeight < 24f * density) return null;
        float popupHeight = header + padding * 2 + cellHeight * rows;
        float left = Math.max(margin, Math.min(width - margin - popupWidth,
                key.centerX() - popupWidth * .25f));
        float top = Math.max(margin, Math.min(height - margin - popupHeight, key.top - popupHeight - margin));
        return new RectF(left, top, left + popupWidth, top + popupHeight);
    }

    private static final class Drain {
        final WeakReference<d1> view;
        final long downTime;
        Drain(d1 view, long downTime) { this.view = new WeakReference<d1>(view); this.downTime = downTime; }
    }

    private static final class Stroke implements View.OnAttachStateChangeListener {
        final f0 view;
        final KeyboardService service;
        final x model;
        final fb0.t0 layout;
        final n2 key;
        final Object window, editor;
        final InputConnection connection;
        final int width, height, pointer, holdMillis, inputType, imeOptions, start, end;
        final long downTime, bornUptime, epoch;
        final float startX, startY, minimum, minimumUp, directionSlop, density;
        final RectF keyRect, keyPixels;
        final String composition, raw, before, after;
        boolean qualified, captured, closed, listening;
        EmojiRecentsBridge.Snapshot recents;
        Picker picker;
        ViewOverlay overlay;
        ViewParent interceptParent;

        Stroke(f0 view, KeyboardService service, x model, n2 key, int holdMillis, MotionEvent event) {
            this.view = view; this.service = service; this.model = model; this.key = key; this.holdMillis = holdMillis;
            layout = view.a; window = view.getWindowToken(); editor = service.i; connection = connection(service);
            inputType = service.i.inputType; imeOptions = service.i.imeOptions;
            width = view.getWidth(); height = view.getHeight(); pointer = event.getPointerId(0);
            downTime = event.getDownTime(); bornUptime = SystemClock.uptimeMillis(); epoch = generation;
            startX = event.getX(); startY = event.getY();
            keyRect = new RectF(key.e.a);
            keyPixels = new RectF(keyRect.left * width, keyRect.top * height, keyRect.right * width, keyRect.bottom * height);
            float configuredDensity = view.getResources().getDisplayMetrics().density;
            density = finite(configuredDensity) && configuredDensity > 0 ? configuredDensity : 1f;
            float side = Math.min(keyPixels.width(), keyPixels.height()), jitter = 0f;
            try { jitter = Math.max(0f, Math.min(ViewConfiguration.get(view.getContext()).getScaledTouchSlop(), side * .5f)); }
            catch (RuntimeException | LinkageError ignored) { }
            float base = Math.max(6f, side * .24f), scale = multiplier();
            minimum = base * scale;
            minimumUp = Math.max(base * .5f, jitter * .5f) * scale;
            directionSlop = Math.max(base * 2f, jitter * 2f) * scale;
            dc0.a state = model.c.C();
            start = state.k(); end = state.S(); composition = state.s(); raw = state.G();
            before = text(connection.getTextBeforeCursor(32, 0));
            after = text(connection.getTextAfterCursor(16, 0));
        }

        boolean snapshotValid() { return composition != null && raw != null && minimum > 0; }

        boolean valid() { return !closed && contextValid(); }

        boolean contextValid() {
            if (epoch != generation || !ready(view) || view.a != layout || view.z != model
                    || view.getWidth() != width || view.getHeight() != height || view.getWindowToken() != window
                    || service.i != editor || service.i.inputType != inputType || service.i.imeOptions != imeOptions
                    || connection(service) != connection || !((fb0.f0) layout).d.contains(key)
                    || key.e == null || key.e.a == null || !keyRect.equals(key.e.a)
                    || EmojiToolbar.emojiHoldMillis(key) != holdMillis || model.c == null) return false;
            dc0.a state = model.c.C();
            return state != null && start == state.k() && end == state.S()
                    && equal(composition, state.s()) && equal(raw, state.G());
        }

        boolean editorMatches() {
            return equal(before, text(connection.getTextBeforeCursor(32, 0)))
                    && equal(after, text(connection.getTextAfterCursor(16, 0)));
        }

        boolean add(float px, float py) {
            if (!finite(px) || !finite(py)) return false;
            if (qualified) return true;
            double dx = Math.abs((double) px - startX), up = (double) startY - py;
            double distance = dx * dx + up * up;
            if (distance >= (double) minimum * minimum && up >= minimumUp && up >= dx * UP_SLOPE) {
                qualified = true;
                return true;
            }
            return distance < (double) directionSlop * directionSlop;
        }

        void dispose() {
            closed = true;
            Picker previous = picker;
            picker = null;
            if (previous != null) {
                previous.hidden = true;
                try { previous.invalidateSelf(); } catch (RuntimeException | LinkageError | OutOfMemoryError ignored) { }
                try { if (overlay != null) overlay.remove(previous); } catch (RuntimeException | LinkageError | OutOfMemoryError ignored) { }
            }
            overlay = null;
            ViewParent previousParent = interceptParent;
            interceptParent = null;
            if (previousParent != null) {
                try { previousParent.requestDisallowInterceptTouchEvent(false); }
                catch (RuntimeException | LinkageError | OutOfMemoryError ignored) { }
            }
            if (listening) {
                listening = false;
                try { view.removeOnAttachStateChangeListener(this); } catch (RuntimeException | LinkageError | OutOfMemoryError ignored) { }
            }
            recents = null;
        }

        public void onViewAttachedToWindow(View ignored) { }
        public void onViewDetachedFromWindow(View ignored) {
            if (active == this) active = null;
            dispose();
        }
    }

    private static final class Picker extends Drawable {
        final View view;
        final EmojiRecentsBridge.Snapshot recents;
        final RectF panel, cells;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final float density, cellWidth, cellHeight;
        final int count, rows;
        int selected = -1;
        boolean hidden;

        Picker(View view, EmojiRecentsBridge.Snapshot recents, RectF panel, float density) {
            this.view = view; this.recents = recents; this.panel = panel; this.density = density;
            count = recents.size(); rows = Math.max(1, (count + 3) / 4);
            float padding = 8f * density;
            cells = new RectF(panel.left + padding, panel.top + padding + 28f * density,
                    panel.right - padding, panel.bottom - padding);
            cellWidth = cells.width() / 4f; cellHeight = cells.height() / rows;
            setBounds(0, 0, view.getWidth(), view.getHeight());
        }

        void select(float x, float y) {
            int next = -1;
            if (finite(x) && finite(y) && cells.contains(x, y)) {
                int col = Math.min(3, (int) ((x - cells.left) / cellWidth));
                int row = Math.min(rows - 1, (int) ((y - cells.top) / cellHeight));
                int index = row * 4 + col;
                if (index >= 0 && index < count) next = index;
            }
            if (selected != next) { selected = next; invalidateSelf(); }
        }

        private void fitText(String value, float desiredSize, float availableWidth) {
            paint.setTextSize(desiredSize);
            float measured = paint.measureText(value);
            if (finite(measured) && measured > availableWidth && availableWidth > 0)
                paint.setTextSize(desiredSize * availableWidth / measured);
        }

        @Override public void draw(Canvas canvas) {
            if (hidden) return;
            float radius = 12f * density;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xf522262b);
            canvas.drawRoundRect(panel, radius, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(density);
            paint.setColor(0xff58616b);
            canvas.drawRoundRect(panel, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.LEFT);
            fitText("最近使った絵文字", 13f * density, cells.width());
            paint.setColor(0xffeeeeee);
            canvas.drawText("最近使った絵文字", cells.left, panel.top + 23f * density, paint);
            if (count == 0) {
                paint.setTextAlign(Paint.Align.CENTER);
                fitText("履歴がありません", 13f * density, cells.width());
                paint.setColor(0xffb9c0c8);
                canvas.drawText("履歴がありません", panel.centerX(), cells.centerY() + 4f * density, paint);
                return;
            }
            float gap = 3f * density;
            for (int i = 0; i < count; i++) {
                float left = cells.left + (i % 4) * cellWidth;
                float top = cells.top + (i / 4) * cellHeight;
                RectF cell = new RectF(left + gap, top + gap, left + cellWidth - gap, top + cellHeight - gap);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(i == selected ? 0xff205360 : 0xff30363d);
                canvas.drawRoundRect(cell, 7f * density, 7f * density, paint);
                if (i == selected) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2f * density);
                    paint.setColor(0xff52d9f0);
                    canvas.drawRoundRect(cell, 7f * density, 7f * density, paint);
                }
                paint.setStyle(Paint.Style.FILL);
                paint.setTextAlign(Paint.Align.CENTER);
                String emoji = recents.get(i);
                fitText(emoji, Math.min(29f * density, Math.min(cellWidth, cellHeight) * .62f),
                        cell.width() - 4f * density);
                paint.setColor(0xffffffff);
                Paint.FontMetrics metrics = paint.getFontMetrics();
                float baseline = cell.centerY() - (metrics.ascent + metrics.descent) * .5f;
                // A complete emoji sequence is shaped as one String. Never
                // split surrogate pairs, skin tones, flags or ZWJ sequences.
                canvas.drawText(emoji, cell.centerX(), baseline, paint);
            }
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
