package hiro.swiftkey;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/** A transient choice surface. It never takes keyboard focus or writes text. */
public final class JapaneseGlideCandidates {
    public interface Listener {
        void choose(JapaneseGlideDecoder.Result result);
        void cancel();
    }

    private static Choice current;

    private JapaneseGlideCandidates() {}

    /** All calls are made on the keyboard UI thread. Replacing a popup is silent. */
    public static void show(View anchor, JapaneseGlideDecoder.Result[] values,
                            Listener listener) {
        dismiss();
        if (listener == null) return;
        if (anchor == null || values == null || values.length == 0
                || !anchor.isAttachedToWindow() || anchor.getWindowToken() == null
                || anchor.getWidth() <= 0 || anchor.getHeight() <= 0) {
            listener.cancel();
            return;
        }

        final Choice choice = new Choice(listener);
        current = choice;
        try {
            Context context = anchor.getContext();
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            boolean dark = (context.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            int foreground = dark ? Color.WHITE : Color.rgb(24, 24, 24);
            int background = dark ? Color.rgb(42, 42, 42) : Color.rgb(250, 250, 250);
            int border = dark ? Color.rgb(104, 104, 104) : Color.rgb(170, 170, 170);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(metrics, 4), dp(metrics, 4), dp(metrics, 4), dp(metrics, 4));
            row.setBackground(shape(metrics, background, border));
            row.setContentDescription("グライド入力の候補");

            HorizontalScrollView scroll = new HorizontalScrollView(context);
            scroll.setHorizontalScrollBarEnabled(true);
            scroll.setFillViewport(false);
            scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            LinearLayout entries = new LinearLayout(context);
            entries.setOrientation(LinearLayout.HORIZONTAL);
            entries.setGravity(Gravity.CENTER_VERTICAL);
            scroll.addView(entries, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            row.addView(scroll, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f));

            int shown = 0;
            for (int i = 0; i < values.length && shown < 12; i++) {
                final JapaneseGlideDecoder.Result value = values[i];
                if (value == null || value.hiragana == null || value.hiragana.length() == 0) continue;
                TextView entry = label(context, metrics,
                        value.hiragana + (value.fallback ? "（補助）" : ""), foreground);
                entry.setContentDescription((value.fallback ? "補助候補、" : "")
                        + value.hiragana + "を入力");
                entry.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) { choice.choose(value); }
                });
                entries.addView(entry, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
                shown++;
            }
            if (shown == 0) {
                choice.cancel();
                return;
            }

            TextView close = label(context, metrics, "×", foreground);
            close.setTextSize(24f);
            close.setContentDescription("グライド候補を閉じる");
            close.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) { choice.cancel(); }
            });
            row.addView(close, new LinearLayout.LayoutParams(dp(metrics, 48),
                    ViewGroup.LayoutParams.MATCH_PARENT));

            int width = Math.min(anchor.getWidth(), metrics.widthPixels);
            int height = dp(metrics, 56);
            PopupWindow popup = new PopupWindow(row, width, height, false);
            choice.popup = popup;
            popup.setFocusable(false);
            popup.setTouchable(true);
            popup.setOutsideTouchable(false);
            popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
            popup.setBackgroundDrawable(shape(metrics, background, border));
            popup.setElevation(dp(metrics, 6));
            popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override public void onDismiss() { choice.cancel(); }
            });

            // showAsDropDown follows the keyboard window during floating/one-handed
            // layout changes. The negative offset places this row above its anchor.
            // Keep normal clipping enabled so it cannot create off-screen targets.
            popup.showAsDropDown(anchor, 0, -anchor.getHeight() - height, Gravity.START);
        } catch (RuntimeException exception) {
            choice.cancel();
        } catch (LinkageError error) {
            choice.cancel();
        }
    }

    /** Lifecycle resets and replacements dismiss without calling back into the queue. */
    public static void dismiss() {
        Choice choice = current;
        if (choice != null) choice.closeSilently();
    }

    private static TextView label(Context context, DisplayMetrics metrics, String text,
                                  int foreground) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(foreground);
        view.setTextSize(16f);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        view.setMinWidth(dp(metrics, 48));
        view.setMinHeight(dp(metrics, 48));
        view.setPadding(dp(metrics, 12), 0, dp(metrics, 12), 0);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private static GradientDrawable shape(DisplayMetrics metrics, int fill, int border) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(metrics, 8));
        drawable.setStroke(dp(metrics, 1), border);
        return drawable;
    }

    private static int dp(DisplayMetrics metrics, int value) {
        return Math.max(1, Math.round(value * metrics.density));
    }

    private static final class Choice {
        final Listener listener;
        PopupWindow popup;
        boolean finished;

        Choice(Listener listener) { this.listener = listener; }

        boolean finish() {
            if (finished) return false;
            finished = true;
            if (current == this) current = null;
            if (popup != null) {
                try {
                    popup.setOnDismissListener(null);
                    if (popup.isShowing()) popup.dismiss();
                } catch (RuntimeException ignored) {
                    // A keyboard window can disappear between the token check
                    // and dismissal. The callback must still release its queue.
                }
            }
            return true;
        }

        void choose(JapaneseGlideDecoder.Result result) {
            if (finish()) listener.choose(result);
        }

        void cancel() {
            if (finish()) listener.cancel();
        }

        void closeSilently() { finish(); }
    }
}
