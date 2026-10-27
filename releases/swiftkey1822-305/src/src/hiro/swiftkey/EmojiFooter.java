package hiro.swiftkey;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.TextView;

/** Adds an Enter key to the native emoji footer without replacing its existing controls. */
public final class EmojiFooter {
    private static final int ABC_ID = 0x7f0a0543;
    private static final int DELETE_ID = 0x7f0a054a;
    private static final int TABS_ID = 0x7f0a054e;

    private EmojiFooter() { }

    /** Called once, after the native footer controller has installed its listeners. */
    public static void install(je0.c owner, wb0.x sender) {
        try {
            if (owner == null || sender == null || owner.c != wa0.p.c || owner.l == null) return;
            f50.d binding = owner.l;
            if (!(binding.b instanceof TextView) || !(binding.c instanceof View)
                    || !(binding.d instanceof ViewGroup)) return;
            TextView abc = (TextView) binding.b;
            View delete = (View) binding.c;
            View tabs = (View) binding.d;
            if (abc.getId() != ABC_ID || delete.getId() != DELETE_ID || tabs.getId() != TABS_ID) return;
            ViewParent originalParent = abc.getParent();
            if (originalParent instanceof Row) {
                Row row = (Row) originalParent;
                if (row.abc == abc && row.delete == delete && row.tabs == tabs
                        && delete.getParent() == row && tabs.getParent() == row) {
                    row.enter.sender = sender;
                }
                return;
            }
            if (!(originalParent instanceof ViewGroup)
                    || delete.getParent() != originalParent || tabs.getParent() != originalParent) return;
            ViewGroup parent = (ViewGroup) originalParent;
            if (parent.getChildCount() != 3) return;
            View[] originals = new View[3];
            ViewGroup.LayoutParams[] params = new ViewGroup.LayoutParams[3];
            for (int i = 0; i < originals.length; i++) {
                View child = parent.getChildAt(i);
                if (child != abc && child != delete && child != tabs) return;
                originals[i] = child;
                params[i] = child.getLayoutParams();
                if (params[i] == null) return;
            }
            // Build everything that can fail before moving any native widget.
            Row row = new Row(abc.getContext(), abc, delete, tabs, sender);
            try {
                for (View child : originals) parent.removeView(child);
                row.addView(abc, fill());
                row.addView(row.enter, fill());
                row.addView(delete, fill());
                row.addView(tabs, fill());
                parent.addView(row, fill());
            } catch (RuntimeException failure) {
                restore(parent, row, originals, params);
            } catch (LinkageError failure) {
                restore(parent, row, originals, params);
            }
        } catch (RuntimeException ignored) {
            // An incompatible footer is left in its original native form.
        } catch (LinkageError ignored) {
            // This extension is intentionally scoped to the verified native ABI.
        }
    }

    private static ViewGroup.LayoutParams fill() {
        return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private static void restore(ViewGroup parent, Row row, View[] originals,
            ViewGroup.LayoutParams[] params) {
        // Retain the original order, parameters, listeners, IDs, and native state on failure.
        if (row.getParent() == parent) parent.removeView(row);
        for (View child : originals) {
            ViewParent current = child.getParent();
            if (current instanceof ViewGroup) ((ViewGroup) current).removeView(child);
        }
        for (int i = 0; i < originals.length; i++) parent.addView(originals[i], i, params[i]);
    }

    /** Pure geometry: the native abc ratio, two square keys, then a scrollable viewport. */
    static int[] widths(int width, int height) {
        int available = Math.max(0, width);
        int key = Math.min(Math.max(0, height), (int) (((long) available * 4L) / 17L));
        int abc = (int) (((long) key * 5L + 2L) / 4L);
        return new int[] { abc, key, key, available - abc - key - key };
    }

    private static final class Row extends ViewGroup {
        final TextView abc;
        final View delete;
        final View tabs;
        final EnterKey enter;
        private int[] widths = new int[4];

        Row(Context context, TextView abc, View delete, View tabs, wb0.x sender) {
            super(context);
            this.abc = abc;
            this.delete = delete;
            this.tabs = tabs;
            enter = new EnterKey(context, abc, sender);
            setWillNotDraw(true);
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            int height = MeasureSpec.getMode(heightSpec) == MeasureSpec.UNSPECIFIED
                    ? Math.round(48f * getResources().getDisplayMetrics().density)
                    : MeasureSpec.getSize(heightSpec);
            setMeasuredDimension(width, height);
            widths = EmojiFooter.widths(width, height);
            int exactHeight = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).measure(MeasureSpec.makeMeasureSpec(widths[i], MeasureSpec.EXACTLY), exactHeight);
            }
        }

        @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int x = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                int next = x + widths[i];
                if (child.getVisibility() != View.GONE) child.layout(x, 0, next, bottom - top);
                x = next;
            }
        }

        @Override protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
            return fill();
        }
    }

    private static final class EnterKey extends View implements View.OnClickListener,
            ViewTreeObserver.OnPreDrawListener {
        private final TextView abc;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RippleDrawable ripple;
        private int color;
        private ViewTreeObserver observer;
        wb0.x sender;

        EnterKey(Context context, TextView abc, wb0.x sender) {
            super(context);
            this.abc = abc;
            this.sender = sender;
            color = abc.getCurrentTextColor();
            ripple = new RippleDrawable(ColorStateList.valueOf(rippleColor(color)), null,
                    new ColorDrawable(Color.WHITE));
            setBackground(ripple);
            setContentDescription("エンター");
            setFocusable(true);
            setOnClickListener(this);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        private static int rippleColor(int color) {
            return (color & 0x00ffffff) | 0x29000000;
        }

        @Override public CharSequence getAccessibilityClassName() {
            return "android.widget.Button";
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            observer = getViewTreeObserver();
            observer.addOnPreDrawListener(this);
        }

        @Override protected void onDetachedFromWindow() {
            if (observer != null && observer.isAlive()) observer.removeOnPreDrawListener(this);
            observer = null;
            super.onDetachedFromWindow();
        }

        private void syncColor() {
            int nextColor = abc.getCurrentTextColor();
            if (color == nextColor) return;
            color = nextColor;
            ripple.setColor(ColorStateList.valueOf(rippleColor(color)));
            invalidate();
        }

        @Override public boolean onPreDraw() {
            // Native theme observers invalidate abc, not this new sibling. This O(1)
            // comparison updates our render node even when Android caches its drawing.
            syncColor();
            return true;
        }

        @Override public void onClick(View ignored) {
            try {
                // The same native onEnterKey route used by the regular keyboard handles
                // composition and the editor's newline/search/send action. Do not synthesize text.
                sender.y(new lh0.a());
            } catch (RuntimeException ignoredFailure) {
                // Never send a second fallback action after a partially handled native action.
            } catch (LinkageError ignoredFailure) {
                // A native ABI mismatch must not crash the input method.
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            syncColor();
            paint.setColor(color);
            float square = Math.min(getWidth(), getHeight());
            paint.setStrokeWidth(Math.max(1f, square * .045f));
            float size = square * .44f;
            float left = (getWidth() - size) * .5f;
            float right = left + size;
            float top = (getHeight() - size) * .5f;
            float middle = top + size * .65f;
            canvas.drawLine(right, top, right, middle, paint);
            canvas.drawLine(right, middle, left, middle, paint);
            canvas.drawLine(left, middle, left + size * .35f, middle - size * .35f, paint);
            canvas.drawLine(left, middle, left + size * .35f, middle + size * .35f, paint);
        }
    }
}
