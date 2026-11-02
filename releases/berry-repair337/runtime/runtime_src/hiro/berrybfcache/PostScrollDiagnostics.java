package hiro.berrybfcache;

import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.WeakHashMap;

/** Read-only, bounded measurements after a proved BFCache restore. Never repairs or redraws. */
public final class PostScrollDiagnostics {
    private static final long AUTO_MS = 45000, SETTLE_MS = 200, JS_MS = 800;
    private static final int MAX_STATES = 4, MAX_SAMPLES = 8, MAX_SETTLED = 3;
    private static final WeakHashMap<WebView, State> STATES = new WeakHashMap<WebView, State>();
    // evaluateJavascript has no cancellation API. A timed-out request retains its
    // lease until its actual callback, including across navigation generations.
    private static final WeakHashMap<WebView, Lease> LEASES = new WeakHashMap<WebView, Lease>();
    private static long serial;
    private static final String[] FIELDS = {"schema","jsTimeMs","innerW","innerH","scrollX","scrollY","dpr",
        "clientW","clientH","scrollW","scrollH","vvPresent","vvW","vvH","vvOffsetL","vvOffsetT",
        "vvPageL","vvPageT","vvScale","imagesTotal","imagesScanned","rectIntersectImages",
        "completeIntersectImages","naturalSizeIntersectImages","zeroRectImages","imagesCapped",
        "markerPresent","markerSerial","markerPending","scrollingElementPresent"};
    private static final String SCRIPT = "/*BerryPostScrollNumericV1*/(function(){'use strict';"
        + "if(window!==window.top)return null;var m=window.__hiroBerryBFCacheRestoreV1;/*GUARD*/"
        + "var se=document.scrollingElement,d=se||document.documentElement,v=window.visualViewport,iw=window.innerWidth,ih=window.innerHeight;"
        + "if(!d)return null;var im=document.images,total=im.length,n=Math.min(total,128),vis=0,done=0,natural=0,zero=0;"
        + "var vl=v?v.offsetLeft:0,vt=v?v.offsetTop:0,vw=v?v.width:iw,vh=v?v.height:ih;"
        + "for(var i=0;i<n;i++){var x=im[i];if(!x)continue;var r=x.getBoundingClientRect();"
        + "if(r.width<=0||r.height<=0){zero++;continue;}"
        + "if(r.right>vl&&r.bottom>vt&&r.left<vl+vw&&r.top<vt+vh){vis++;"
        + "if(x.complete===true)done++;if(x.naturalWidth>0&&x.naturalHeight>0)natural++;}}"
        + "var mp=!!m&&typeof m.serial==='number'&&isFinite(m.serial)&&m.serial>=0&&m.serial<=999999999999;"
        + "return [1,performance.now(),iw,ih,window.scrollX,window.scrollY,window.devicePixelRatio,"
        + "d.clientWidth,d.clientHeight,d.scrollWidth,d.scrollHeight,v?1:0,v?v.width:0,v?v.height:0,"
        + "v?v.offsetLeft:0,v?v.offsetTop:0,v?v.pageLeft:0,v?v.pageTop:0,v?v.scale:0,"
        + "total,n,vis,done,natural,zero,total>n?1:0,mp?1:0,mp?m.serial:0,mp&&m.pending===true?1:0,se?1:0];})()";

    private static final class Lease { long request; }
    private static final class State implements ViewTreeObserver.OnScrollChangedListener, View.OnAttachStateChangeListener {
        final WeakReference<WebView> view;
        final PresentationRecovery.Proof proof;
        final String script;
        final long began = SystemClock.uptimeMillis();
        final ArrayDeque<String> samples = new ArrayDeque<String>();
        WeakReference<ViewTreeObserver> observer = new WeakReference<ViewTreeObserver>(null);
        boolean cancelled, autoEnded, firstScroll;
        int settled, lastScrollX, lastScrollY;
        long request, scrollEvents;
        Runnable timeout, completion;
        final Runnable deadline = new Runnable() { public void run() { stopAuto(State.this, "AUTO_DEADLINE"); } };
        final Runnable settle = new Runnable() { public void run() {
            try {
                WebView v = view.get();
                if (!valid(v, State.this) || autoEnded) return;
                settled++;
                sample(v, State.this, "SCROLL_SETTLED", null);
                if (settled >= MAX_SETTLED) stopAuto(State.this, "SCROLL_LIMIT");
            } catch (RuntimeException unavailable) { stopAuto(State.this, "OBSERVATION_UNAVAILABLE"); }
        } };
        State(WebView v, PresentationRecovery.Proof p, String event) { view = new WeakReference<WebView>(v); proof = p; script = scriptFor(event); }
        @Override public void onScrollChanged() {
            try {
                WebView v = view.get();
                if (!valid(v, this) || autoEnded) return;
                int x = v.getScrollX(), y = v.getScrollY();
                // This observer belongs to a whole tree; other Views must not consume our budget.
                if (x == lastScrollX && y == lastScrollY) return;
                lastScrollX = x; lastScrollY = y; scrollEvents++;
                if (!firstScroll) { firstScroll = true; sample(v, this, "SCROLL_FIRST", null); }
                remove(v, settle);
                if (!v.postDelayed(settle, SETTLE_MS)) stopAuto(this, "SCHEDULE_UNAVAILABLE");
            } catch (RuntimeException unavailable) { stopAuto(this, "OBSERVATION_UNAVAILABLE"); }
        }
        @Override public void onViewAttachedToWindow(View ignored) {}
        @Override public void onViewDetachedFromWindow(View ignored) { cancel(view.get()); }
    }

    private PostScrollDiagnostics() {}
    public static void start(WebView view, PresentationRecovery.Proof proof) { start(view, proof, null); }
    public static void start(WebView view, PresentationRecovery.Proof proof, String event) {
        if (view == null || proof == null || !main()) return;
        cancel(view);
        while (STATES.size() >= MAX_STATES) {
            Iterator<WebView> it = STATES.keySet().iterator();
            if (!it.hasNext()) break;
            WebView old = it.next();
            if (old == null) { it.remove(); continue; }
            cancel(old);
        }
        State s = new State(view, proof, event);
        STATES.put(view, s);
        if (!valid(view, s)) return;
        try {
            s.lastScrollX = view.getScrollX(); s.lastScrollY = view.getScrollY();
            ViewTreeObserver o = view.getViewTreeObserver();
            if (o != null && o.isAlive()) { o.addOnScrollChangedListener(s); s.observer = new WeakReference<ViewTreeObserver>(o); }
            view.addOnAttachStateChangeListener(s);
            if (!view.postDelayed(s.deadline, AUTO_MS)) stopAuto(s, "SCHEDULE_UNAVAILABLE");
            sample(view, s, "RESTORE_BASELINE", null);
        } catch (RuntimeException unavailable) { cancel(view); }
    }
    public static void cancel(WebView view) {
        if (view == null || !main()) return;
        State s = STATES.remove(view);
        if (s == null) return;
        s.cancelled = true;
        stopAuto(s, null);
        if (s.timeout != null) remove(view, s.timeout);
        s.timeout = null; s.completion = null; s.request = 0;
        try { view.removeOnAttachStateChangeListener(s); } catch (RuntimeException unavailable) {}
    }
    /** One manual measurement, even after the automatic budget expires; never restores listeners. */
    public static void inspect(WebView view, Runnable completion) {
        if (!main()) return;
        State s = view == null ? null : STATES.get(view);
        if (!valid(view, s)) { run(completion); return; }
        sample(view, s, "MANUAL_INSPECT", completion);
    }
    public static String describe(WebView view) {
        if (!main()) return "PostScroll: unavailable";
        State s = view == null ? null : STATES.get(view);
        if (s == null) return "PostScroll: no current proved restore";
        Lease lease = LEASES.get(view);
        StringBuilder b = new StringBuilder("PostScroll: automatic=").append(!s.autoEnded)
            .append(" scrollEvents=").append(s.scrollEvents).append(" settled=").append(s.settled)
            .append(" waitingForResult=").append(s.request != 0)
            .append(" evaluationOutstanding=").append(lease != null && lease.request != 0);
        for (String row : s.samples) b.append('\n').append(row);
        return b.toString();
    }
    private static boolean valid(WebView v, State s) {
        if (v == null || s == null || s.cancelled || STATES.get(v) != s) return false;
        boolean ok;
        try { ok = v.isAttachedToWindow() && v.isShown() && s.proof.isCurrent(v); }
        catch (RuntimeException unavailable) { ok = false; }
        if (!ok) cancel(v);
        return ok;
    }
    private static void stopAuto(State s, String reason) {
        if (s.autoEnded) return;
        s.autoEnded = true;
        WebView v = s.view.get();
        if (v != null) {
            remove(v, s.settle); remove(v, s.deadline);
            try { v.removeOnAttachStateChangeListener(s); } catch (RuntimeException unavailable) {}
        }
        ViewTreeObserver o = s.observer.get(); s.observer.clear();
        try { if (o != null && o.isAlive()) o.removeOnScrollChangedListener(s); } catch (RuntimeException unavailable) {}
        if (reason != null) add(s, reason);
    }
    private static void sample(final WebView view, final State s, final String phase, Runnable completion) {
        try { sampleNow(view, s, phase, completion); }
        catch (RuntimeException unavailable) { add(s, phase + " OBSERVATION_UNAVAILABLE"); run(completion); }
    }
    private static void sampleNow(final WebView view, final State s, final String phase, Runnable completion) {
        if (!valid(view, s)) return;
        Lease existing = LEASES.get(view);
        if (existing != null && existing.request != 0) {
            add(s, phase + " JS_BUSY");
            run(completion);
            return;
        }
        final Lease lease = existing == null ? new Lease() : existing;
        LEASES.put(view, lease);
        long next = ++serial; if (next <= 0) { serial = 1; next = 1; }
        final long id = next, requested = SystemClock.uptimeMillis(), events = s.scrollEvents;
        final int sx = view.getScrollX(), sy = view.getScrollY(), w = view.getWidth(), h = view.getHeight();
        final String nativeBefore = nativeValues(view);
        lease.request = id; s.request = id; s.completion = completion;
        final WeakReference<WebView> weakView = new WeakReference<WebView>(view);
        final WeakReference<State> weakState = new WeakReference<State>(s);
        s.timeout = new Runnable() { public void run() {
            WebView v = weakView.get(); State current = weakState.get();
            if (current == null || !valid(v, current) || current.request != id) return;
            current.timeout = null; current.request = 0;
            add(current, phase + " JS_TIMEOUT delayMs=" + (SystemClock.uptimeMillis() - requested) + " before=" + nativeBefore);
            complete(current);
        } };
        try {
            if (!view.postDelayed(s.timeout, JS_MS)) throw new IllegalStateException();
            view.evaluateJavascript(s.script, new ValueCallback<String>() { public void onReceiveValue(String result) {
                if (!main() || lease.request != id) return;
                lease.request = 0;
                WebView v = weakView.get(); State current = weakState.get();
                if (current == null || !valid(v, current) || current.request != id) return;
                if (current.timeout != null) remove(v, current.timeout);
                current.timeout = null; current.request = 0;
                try {
                    String numeric = parse(result);
                    boolean moved = sx != v.getScrollX() || sy != v.getScrollY() || w != v.getWidth() || h != v.getHeight()
                        || events != current.scrollEvents;
                    add(current, phase + (numeric == null ? " JS_REJECTED" : " SAMPLE_OK")
                        + " requestMs=" + Math.max(0, requested - current.began)
                        + " delayMs=" + Math.max(0, SystemClock.uptimeMillis() - requested) + " moved=" + moved
                        + " before=" + nativeBefore + " after=" + nativeValues(v)
                        + (numeric == null ? "" : " dom={" + numeric + "}"));
                } catch (RuntimeException unavailable) { add(current, phase + " OBSERVATION_UNAVAILABLE"); }
                complete(current);
            } });
        } catch (RuntimeException unavailable) {
            // A synchronous exception means no evaluation callback was registered.
            if (lease.request == id) lease.request = 0;
            if (s.timeout != null) remove(view, s.timeout);
            s.timeout = null; s.request = 0;
            add(s, phase + " JS_EXCEPTION"); complete(s);
        }
    }
    private static void complete(State s) { Runnable r = s.completion; s.completion = null; run(r); }
    private static void run(Runnable r) { try { if (r != null) r.run(); } catch (RuntimeException unavailable) {} }
    private static void remove(WebView v, Runnable r) { try { v.removeCallbacks(r); } catch (RuntimeException unavailable) {} }
    private static void add(State s, String row) {
        if (s.samples.size() >= MAX_SAMPLES) s.samples.removeFirst();
        s.samples.addLast("+" + Math.max(0, SystemClock.uptimeMillis() - s.began) + "ms " + row);
    }
    private static String nativeValues(WebView v) {
        float scale = v.getScale(), density = v.getContext().getResources().getDisplayMetrics().density;
        return "{scroll=" + v.getScrollX() + "," + v.getScrollY() + " size=" + v.getWidth() + "x" + v.getHeight()
            + " scale=" + finite(scale) + " density=" + finite(density) + " contentHeight=" + v.getContentHeight() + "}";
    }
    private static String finite(float n) { return Float.isNaN(n) || Float.isInfinite(n) ? "unknown" : Float.toString(n); }
    private static String scriptFor(String event) {
        String guard = "";
        if (event != null && event.matches("[a-z0-9]{8,64}:[0-9]{1,12}")) {
            int colon = event.indexOf(':');
            guard = "if(!m||m.id!=='" + event.substring(0, colon) + "'||m.serial!==" + Long.parseLong(event.substring(colon + 1))
                + "||m.pending!==true)return null;";
        }
        return SCRIPT.replace("/*GUARD*/", guard);
    }
    /** Exact numeric schema: no page strings, unknown fields, nonfinite values or oversized responses. */
    private static String parse(String result) {
        if (result == null || result.length() > 4096 || !result.startsWith("[") || !result.endsWith("]")) return null;
        String[] values = result.substring(1, result.length() - 1).split(",", -1);
        if (values.length != FIELDS.length) return null;
        double[] n = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            String value = values[i].trim();
            if (value.length() > 40 || !value.matches("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")) return null;
            try { n[i] = Double.parseDouble(value); } catch (NumberFormatException invalid) { return null; }
            if (Double.isNaN(n[i]) || Double.isInfinite(n[i]) || Math.abs(n[i]) > 1e12) return null;
            if (i != 4 && i != 5 && i != 14 && i != 15 && i != 16 && i != 17 && n[i] < 0) return null;
        }
        if (n[0] != 1 || n[6] <= 0 || n[11] != 0 && n[11] != 1 || n[20] > 128
                || n[20] > n[19] || n[21] > n[20] || n[22] > n[21] || n[23] > n[21] || n[24] > n[20]
                || n[25] != (n[19] > n[20] ? 1 : 0) || n[26] != 0 && n[26] != 1 || n[28] != 0 && n[28] != 1
                || n[29] != 0 && n[29] != 1) return null;
        for (int i = 19; i < 30; i++) if (n[i] != Math.floor(n[i])) return null;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < n.length; i++) { if (i != 0) out.append(' '); out.append(FIELDS[i]).append('=').append(Double.toString(n[i])); }
        return out.toString();
    }
    private static boolean main() { return Looper.myLooper() == Looper.getMainLooper(); }
}
