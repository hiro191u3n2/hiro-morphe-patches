package hiro.berrybfcache;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.WeakHashMap;

/** Bounded native observations and explicit user-controlled copying. No page content is collected. */
public final class RenderDiagnostics {
    private static final String VERSION = "Berry 描画記録 1.4.3 / bundle335";
    private static final int MAX_TABS = 4;
    private static final int MAX_EVENTS = 64;
    private static final ArrayDeque<Timeline> TIMELINES = new ArrayDeque<Timeline>();
    private static final WeakHashMap<Activity, Badge> BADGES = new WeakHashMap<Activity, Badge>();
    private static final WeakHashMap<WebView, Runnable> SHOW_PENDING = new WeakHashMap<WebView, Runnable>();
    private static boolean hiddenUntilRestart;
    private static final String PHASES = "|RESTORE_BEGIN|PRESENTATION_START|WAIT_ATTACHED|WAIT_VISIBLE|WAIT_SIZE"
        + "|WAIT_LAYOUT|WAIT_STABLE|LAYOUT_REQUESTED|PRE_DRAW_REQUESTED|VISUAL_BUSY|VISUAL_REQUESTED"
        + "|VISUAL_READY|VISUAL_TIMEOUT|DRAW_REQUESTED|DRAW_FALLBACK_REQUESTED|DRAW_TRAVERSAL|DRAW_TIMEOUT|PRESENTATION_DEADLINE"
        + "|PRESENTATION_CANCELLED|PRESENTATION_STALE|PRESENTATION_EXCEPTION|PRESENTATION_DETACHED"
        + "|NAVIGATION_CANCEL|EVENT_REDACTED|";

    private static final class Timeline {
        final WeakReference<WebView> view;
        final long began = SystemClock.uptimeMillis();
        final ArrayDeque<String> events = new ArrayDeque<String>();
        long restores;
        long uiRevision;
        String lastSnapshot;
        Timeline(WebView view) { this.view = new WeakReference<WebView>(view); }
    }

    private static final class Badge implements View.OnClickListener, View.OnAttachStateChangeListener {
        final WeakReference<Activity> activity;
        final WeakReference<TextView> label;
        WeakReference<WebView> target = new WeakReference<WebView>(null);
        WeakReference<AlertDialog> dialog = new WeakReference<AlertDialog>(null);
        long revision;
        Badge(Activity activity, TextView label) {
            this.activity = new WeakReference<Activity>(activity);
            this.label = new WeakReference<TextView>(label);
        }
        @Override public void onClick(View ignored) { try { inspect(this); } catch (RuntimeException unavailable) {} }
        @Override public void onViewAttachedToWindow(View ignored) {}
        @Override public void onViewDetachedFromWindow(View detached) {
            try {
                clear(this);
                if (detached == label.get()) {
                    Activity a = activity.get();
                    if (a != null && BADGES.get(a) == this) BADGES.remove(a);
                }
            } catch (RuntimeException unavailable) {}
        }
    }

    private RenderDiagnostics() {}

    public static void begin(WebView view) { record(view, "RESTORE_BEGIN"); }

    /** Fixed phase codes and numeric native state only; accepts no arbitrary page strings. */
    public static void record(WebView view, String phase) {
        if (view == null || !mainThread()) return;
        try {
            String code = phase != null && phase.length() < 40 && phase.matches("[A-Z_]+") && PHASES.contains("|" + phase + "|")
                ? phase : "EVENT_REDACTED";
            Timeline timeline = timeline(view, true);
            if ("RESTORE_BEGIN".equals(code)) { timeline.restores++; timeline.uiRevision++; }
            if (timeline.events.size() == MAX_EVENTS) timeline.events.removeFirst();
            String currentSnapshot = snapshot(view);
            String observed = currentSnapshot.equals(timeline.lastSnapshot) ? "geometry=unchanged" : currentSnapshot;
            timeline.lastSnapshot = currentSnapshot;
            timeline.events.addLast("+" + Math.max(0L, SystemClock.uptimeMillis() - timeline.began)
                + "ms " + code + " " + observed);
            if ("PRESENTATION_START".equals(code) || "RESTORE_BEGIN".equals(code)
                    || "DRAW_TRAVERSAL".equals(code) || "PRESENTATION_DEADLINE".equals(code)
                    || "DRAW_TIMEOUT".equals(code) || "VISUAL_TIMEOUT".equals(code)
                    || "DRAW_FALLBACK_REQUESTED".equals(code) || "PRESENTATION_EXCEPTION".equals(code)) show(view);
        } catch (RuntimeException unavailable) { /* Observations never interrupt recovery. */ }
    }

    /** Cancel the old page's diagnostic UI without destroying its bounded observations. */
    public static void cancel(WebView view) {
        if (view == null || !mainThread()) return;
        try {
            Timeline timeline = timeline(view, false);
            if (timeline != null) { timeline.uiRevision++; record(view, "NAVIGATION_CANCEL"); }
            Runnable pending = SHOW_PENDING.remove(view);
            if (pending != null) view.removeCallbacks(pending);
            Activity a = activityFor(view);
            Badge badge = a == null ? null : BADGES.get(a);
            if (badge != null && badge.target.get() == view) clear(badge);
        } catch (RuntimeException unavailable) {}
    }

    /** Defer and coalesce UI work, including when called by an OnDraw observation. */
    public static void show(final WebView view) {
        if (view == null || !mainThread()) return;
        final Timeline timeline = timeline(view, false);
        if (timeline == null || SHOW_PENDING.containsKey(view)) return;
        final long revision = timeline.uiRevision;
        final WeakReference<WebView> weak = new WeakReference<WebView>(view);
        Runnable request = new Runnable() {
            @Override public void run() {
                WebView current = weak.get();
                if (current == null || SHOW_PENDING.get(current) != this) return;
                SHOW_PENDING.remove(current);
                Timeline active = timeline(current, false);
                if (active == timeline && active.uiRevision == revision) showNow(current);
            }
        };
        SHOW_PENDING.put(view, request);
        try { if (!view.post(request)) SHOW_PENDING.remove(view); }
        catch (RuntimeException unavailable) { SHOW_PENDING.remove(view); }
    }

    /** Add one small native overlay outside a draw callback. Never mutates WebView layout or DOM. */
    private static void showNow(WebView view) {
        try {
            if (timeline(view, false) == null || !visible(view)) return;
            Activity a = activityFor(view);
            if (!alive(a) || hiddenUntilRestart || a.getWindow() == null) return;
            View decor = a.getWindow().getDecorView();
            if (!(decor instanceof FrameLayout)) return;
            Badge badge = BADGES.get(a);
            if (badge == null || badge.label.get() == null) {
                pruneBadges();
                TextView label = new TextView(a);
                label.setText("描画記録");
                label.setContentDescription("Berryの描画記録を開いてコピー");
                label.setTextSize(11f);
                label.setTextColor(0xffffffff);
                label.setBackgroundColor(0xc0333333);
                int pad = dp(a, 6);
                label.setPadding(pad, dp(a, 4), pad, dp(a, 4));
                badge = new Badge(a, label);
                label.setOnClickListener(badge);
                label.addOnAttachStateChangeListener(badge);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
                params.topMargin = dp(a, 64);
                params.rightMargin = dp(a, 8);
                BADGES.put(a, badge);
                ((FrameLayout) decor).addView(label, params);
            }
            if (badge.target.get() != view) {
                clear(badge);
                badge.target = new WeakReference<WebView>(view);
                view.addOnAttachStateChangeListener(badge);
            }
            TextView label = badge.label.get();
            if (label != null && label.getVisibility() != View.VISIBLE) label.setVisibility(View.VISIBLE);
        } catch (RuntimeException unavailable) {}
    }

    /** A snapshot is not proof of correct pixels or a diagnosis of renderer memory pressure. */
    public static String report(WebView view) {
        if (!mainThread()) return VERSION + "\nUI thread unavailable";
        try {
            StringBuilder text = new StringBuilder(VERSION);
            text.append("\nURL・タイトル・本文・入力内容は記録しません。");
            text.append("\nDRAW_TRAVERSALは描画処理の通過記録です。表示の正常性は未判定です。");
            text.append("\n無応答だけで停止・メモリ不足とは判定できません。");
            if (view == null) return text.append("\nWebView unavailable").toString();
            text.append("\nNow: ").append(snapshot(view));
            Timeline timeline = timeline(view, false);
            text.append("\nRestores=").append(timeline == null ? 0 : timeline.restores);
            text.append("\nEvents (last ").append(MAX_EVENTS).append("):");
            if (timeline == null || timeline.events.isEmpty()) text.append("\nnone");
            else for (String row : timeline.events) text.append('\n').append(row);
            return text.toString();
        } catch (RuntimeException unavailable) { return VERSION + "\nNative snapshot unavailable"; }
    }

    private static String snapshot(WebView view) {
        try {
            StringBuilder s = new StringBuilder();
            s.append("attached=").append(view.isAttachedToWindow()).append(" shown=").append(view.isShown());
            geometry(s, " view", view);
            s.append(" layoutPending=").append(view.isLayoutRequested());
            s.append(" scroll=").append(view.getScrollX()).append(',').append(view.getScrollY());
            float scale = view.getScale();
            s.append(" scale=").append(Float.isNaN(scale) || Float.isInfinite(scale) ? "unknown" : Float.toString(scale));
            s.append(" contentHeight=").append(view.getContentHeight()).append(" progress=").append(view.getProgress());
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params != null) s.append(" lp=").append(params.width).append('x').append(params.height);
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) params;
                s.append(" margin=").append(m.leftMargin).append(',').append(m.topMargin).append(',')
                    .append(m.rightMargin).append(',').append(m.bottomMargin);
            }
            ViewParent parent = view.getParent();
            if (parent instanceof View) geometry(s, " parent", (View) parent);
            return s.toString();
        } catch (RuntimeException unavailable) { return "native-state-unavailable"; }
    }

    private static void geometry(StringBuilder s, String label, View v) {
        s.append(label).append('=').append(v.getWidth()).append('x').append(v.getHeight());
        s.append(" measured=").append(v.getMeasuredWidth()).append('x').append(v.getMeasuredHeight());
        s.append(" bounds=").append(v.getLeft()).append(',').append(v.getTop()).append(',')
            .append(v.getRight()).append(',').append(v.getBottom());
        s.append(" padding=").append(v.getPaddingLeft()).append(',').append(v.getPaddingTop()).append(',')
            .append(v.getPaddingRight()).append(',').append(v.getPaddingBottom());
    }

    private static void inspect(final Badge badge) {
        WebView view = badge.target.get();
        Activity a = badge.activity.get();
        if (!alive(a) || BADGES.get(a) != badge) return;
        if (!visible(view)) { clear(badge); return; }
        AlertDialog old = badge.dialog.get();
        if (old != null) old.dismiss();
        final String text = report(view);
        final long revision = badge.revision;
        AlertDialog dialog = new AlertDialog.Builder(a).setTitle("Berry 描画記録").setMessage(text)
            .setPositiveButton("コピー", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface ignored, int which) { copy(badge, text, revision); }
            }).setNeutralButton("アプリ再起動まで隠す", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface ignored, int which) {
                    if (badge.revision != revision) return;
                    hiddenUntilRestart = true;
                    for (Badge current : BADGES.values()) clear(current);
                }
            }).setNegativeButton("閉じる", null).create();
        badge.dialog = new WeakReference<AlertDialog>(dialog);
        dialog.show();
    }

    /** The only clipboard write path: an explicit dialog button chosen by the user. */
    private static void copy(Badge badge, String text, long revision) {
        Activity a = badge.activity.get();
        if (!alive(a) || badge.revision != revision || !visible(badge.target.get())) return;
        try {
            Object service = a.getSystemService(Context.CLIPBOARD_SERVICE);
            if (service instanceof ClipboardManager)
                ((ClipboardManager) service).setPrimaryClip(ClipData.newPlainText("Berry 描画記録", text));
        } catch (RuntimeException unavailable) {}
    }

    private static void clear(Badge badge) {
        badge.revision++;
        AlertDialog dialog = badge.dialog.get();
        badge.dialog.clear();
        if (dialog != null) try { dialog.dismiss(); } catch (RuntimeException unavailable) {}
        WebView target = badge.target.get();
        badge.target.clear();
        if (target != null) try { target.removeOnAttachStateChangeListener(badge); } catch (RuntimeException unavailable) {}
        TextView label = badge.label.get();
        // INVISIBLE preserves the overlay's layout slot and avoids a root remeasure on every Back.
        if (label != null) try {
            if (label.getVisibility() != View.INVISIBLE) label.setVisibility(View.INVISIBLE);
        } catch (RuntimeException unavailable) {}
    }

    private static void pruneBadges() {
        // Weak ownership does not permit an unbounded number of still-live Activities.
        while (BADGES.size() >= MAX_TABS) {
            Iterator<Badge> it = BADGES.values().iterator();
            if (!it.hasNext()) return;
            Badge old = it.next(); it.remove(); clear(old);
            TextView label = old.label.get();
            if (label != null && label.getParent() instanceof ViewGroup)
                ((ViewGroup) label.getParent()).removeView(label);
        }
    }

    private static Timeline timeline(WebView view, boolean create) {
        Timeline found = null;
        for (Iterator<Timeline> it = TIMELINES.iterator(); it.hasNext();) {
            Timeline t = it.next();
            WebView target = t.view.get();
            if (target == null) it.remove();
            else if (target == view) { found = t; if (create) it.remove(); }
        }
        if (found != null) { if (create) TIMELINES.addLast(found); return found; }
        if (!create) return null;
        while (TIMELINES.size() >= MAX_TABS) {
            Timeline old = TIMELINES.removeFirst();
            WebView oldView = old.view.get();
            if (oldView != null) {
                Runnable pending = SHOW_PENDING.remove(oldView);
                if (pending != null) oldView.removeCallbacks(pending);
            }
        }
        found = new Timeline(view); TIMELINES.addLast(found); return found;
    }
    private static Activity activityFor(View view) {
        View current = view;
        for (int i = 0; i < 16 && current != null; i++) {
            Activity found = activity(current.getContext());
            if (found != null) return found;
            ViewParent parent = current.getParent();
            if (!(parent instanceof View) || parent == current) return null;
            current = (View) parent;
        }
        return null;
    }
    private static Activity activity(Context context) {
        for (int i = 0; i < 8 && context != null; i++) {
            if (context instanceof Activity) return (Activity) context;
            if (!(context instanceof ContextWrapper)) return null;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) return null;
            context = next;
        }
        return null;
    }
    private static boolean alive(Activity a) { return a != null && !a.isFinishing() && !a.isDestroyed(); }
    private static boolean visible(WebView v) { return v != null && v.isAttachedToWindow() && v.isShown(); }
    private static boolean mainThread() { return Looper.myLooper() == Looper.getMainLooper(); }
    private static int dp(Activity a, int value) {
        return Math.max(1, (int) (a.getResources().getDisplayMetrics().density * value + 0.5f));
    }
}
