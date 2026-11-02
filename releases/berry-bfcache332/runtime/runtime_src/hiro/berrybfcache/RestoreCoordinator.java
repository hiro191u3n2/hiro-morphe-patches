package hiro.berrybfcache;

import android.os.Looper;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Completes only a positively identified persisted pageshow. Native commit and
 * visual callbacks are useful notifications, not prerequisites for completion.
 * Every JavaScript request has a watchdog; timers never synthesize completion.
 */
public final class RestoreCoordinator {
    private static final String TAG = "BerryBFCache";
    private static final String LISTENER = "hiroBerryBFCacheRestoreV1";
    private static final String MESSAGE = "hiro-bfcache-restored-v1";
    private static final int MAX_ATTEMPTS = 12;
    private static final long RETRY_DELAY_MS = 120;
    private static final long PROBE_TIMEOUT_MS = 400;
    private static final WeakHashMap<WebView, State> STATES = new WeakHashMap<WebView, State>();
    private static final Pattern RESULT = Pattern.compile("^\"B1\\|([a-z0-9]{8,64})\\|([0-9]{1,12})\\|([01])\"$");
    private static final String SCRIPT =
        "(function(){'use strict';if(window!==window.top)return;" +
        "var k='__hiroBerryBFCacheRestoreV1';if(Object.prototype.hasOwnProperty.call(window,k))return;" +
        "var s={id:Date.now().toString(36)+Math.random().toString(36).slice(2),serial:0,pending:false};" +
        "Object.defineProperty(window,k,{value:s,enumerable:false,configurable:false,writable:false});" +
        "window.addEventListener('pagehide',function(){s.pending=false;});" +
        "window.addEventListener('pageshow',function(e){s.pending=e.persisted===true;if(!s.pending)return;" +
        "s.serial++;try{var b=window.hiroBerryBFCacheRestoreV1;if(b&&typeof b.postMessage==='function')" +
        "b.postMessage('hiro-bfcache-restored-v1');}catch(ignore){}});})();";
    private static final String PROBE =
        "(function(){var s=window.__hiroBerryBFCacheRestoreV1;" +
        "if(!s||typeof s.id!=='string'||typeof s.serial!=='number')return null;" +
        "return 'B1|'+s.id+'|'+s.serial+'|'+(s.pending===true?'1':'0');})()";

    private static final class State {
        long generation;
        long commitToken;
        boolean committedSeen;
        String committedDocument;
        long retryToken;
        long visualRequest;
        int remaining;
        boolean terminal;
        boolean retryScheduled;
        boolean probeAgain;
        boolean confirmationCredit;
        boolean queryFailureLogged;
        String url;
        String candidateEvent;
        String candidateDocument;
        String lastHandledEvent;
        Runnable completion;
        Probe activeProbe;
    }

    private static final class Probe {
        final long generation;
        final long commitToken;
        final String url;
        Probe(State s) { generation = s.generation; commitToken = s.commitToken; url = s.url; }
    }

    private static final class Marker {
        final String document;
        final String event;
        final boolean pending;
        Marker(String document, String serial, boolean pending) {
            this.document = document;
            event = document + ":" + serial;
            this.pending = pending && !"0".equals(serial);
        }
    }

    private RestoreCoordinator() {}
    public static String listenerName() { return LISTENER; }
    public static String script() { return SCRIPT; }
    public static InvocationHandler listener() { return new Listener(); }

    /** Compatibility overload for older patch callers. */
    public static void started(WebView view, String url) { started(view, url, null); }

    public static void started(final WebView view, final String url, final Runnable completion) {
        if (view == null) return;
        if (!onMainThread()) {
            final WeakReference<WebView> weak = new WeakReference<WebView>(view);
            view.post(new Runnable() { public void run() { started(weak.get(), url, completion); } });
            return;
        }
        State s = state(view);
        s.generation++;
        s.committedSeen = false;
        s.committedDocument = null;
        s.retryToken++;
        s.url = url;
        s.completion = completion;
        s.remaining = MAX_ATTEMPTS;
        s.terminal = false;
        s.retryScheduled = false;
        s.probeAgain = false;
        s.confirmationCredit = false;
        s.queryFailureLogged = false;
        s.candidateDocument = null;
        s.candidateEvent = null;
        s.activeProbe = null;
        // The first check is posted so Berry's own onPageStarted body finishes
        // setting its loading state before our completion can run.
        schedule(view, s);
    }

    /** Cancel finite fallback work after an ordinary native completion. */
    public static void finished(final WebView view, final String url) {
        if (view == null) return;
        if (!onMainThread()) {
            final WeakReference<WebView> weak = new WeakReference<WebView>(view);
            view.post(new Runnable() { public void run() { finished(weak.get(), url); } });
            return;
        }
        State s = STATES.get(view);
        if (s == null || !sameUrl(view, url) || !url.equals(s.url)) return;
        s.terminal = true;
        s.activeProbe = null;
        s.retryToken++;
        s.retryScheduled = false;
        s.probeAgain = false;
    }

    public static void committed(final WebView view, final String url, final Runnable completion) {
        if (view == null || completion == null) return;
        if (!onMainThread()) {
            final WeakReference<WebView> weak = new WeakReference<WebView>(view);
            view.post(new Runnable() { public void run() { committed(weak.get(), url, completion); } });
            return;
        }
        if (!sameUrl(view, url)) return;
        State s = state(view);
        if (s.url == null || !url.equals(s.url)) {
            started(view, url, completion);
            s = state(view);
        } else {
            s.completion = completion;
        }
        if (!s.terminal) {
            if (!s.committedSeen) {
                s.committedSeen = true;
                // A delayed first native commit is independent new evidence
                // after the initial finite probe window. Duplicate commits do
                // not replenish the budget or create an endless retry loop.
                if (s.remaining == 0) {
                    s.remaining = MAX_ATTEMPTS;
                    s.confirmationCredit = false;
                }
                s.commitToken++;
                s.activeProbe = null;
                s.candidateDocument = null;
                s.candidateEvent = null;
            }
            probe(view, s);
        }
    }

    private static void message(final WebView view) {
        if (view == null) return;
        if (!onMainThread()) {
            final WeakReference<WebView> weak = new WeakReference<WebView>(view);
            view.post(new Runnable() { public void run() { message(weak.get()); } });
            return;
        }
        State s = STATES.get(view);
        if (s == null || s.terminal || s.completion == null) return;
        // A late genuine pageshow can arrive after the initial finite window.
        // A wake-up grants one new finite window, never completion by itself.
        if (s.remaining == 0) { s.remaining = MAX_ATTEMPTS; s.confirmationCredit = false; }
        probe(view, s);
    }

    private static void schedule(WebView view, final State s) {
        if (s.terminal || s.completion == null || s.retryScheduled || s.remaining <= 0) return;
        s.retryScheduled = true;
        final long generation = s.generation;
        final long token = ++s.retryToken;
        final WeakReference<WebView> weak = new WeakReference<WebView>(view);
        view.postDelayed(new Runnable() { public void run() {
            WebView v = weak.get();
            if (v == null) return;
            State current = STATES.get(v);
            if (current != s || current.generation != generation || current.retryToken != token) return;
            current.retryScheduled = false;
            probe(v, current);
        }}, RETRY_DELAY_MS);
    }

    private static void probe(WebView view, final State s) {
        if (s.terminal || s.completion == null) return;
        if (s.activeProbe != null) { s.probeAgain = true; return; }
        if (s.remaining <= 0) return;
        s.remaining--;
        s.retryScheduled = false;
        s.retryToken++;
        if (!sameUrl(view, s.url)) { schedule(view, s); return; }
        final Probe p = new Probe(s);
        s.activeProbe = p;
        final WeakReference<WebView> weak = new WeakReference<WebView>(view);
        // A dropped evaluateJavascript callback must not block all later wakes.
        view.postDelayed(new Runnable() { public void run() {
            WebView v = weak.get();
            if (v == null) return;
            State current = STATES.get(v);
            if (current == null || current.activeProbe != p || !sameGeneration(current, p)) return;
            current.activeProbe = null;
            current.probeAgain = false;
            schedule(v, current);
        }}, PROBE_TIMEOUT_MS);
        try {
            view.evaluateJavascript(PROBE, new ValueCallback<String>() {
                @Override public void onReceiveValue(String value) {
                    WebView v = weak.get();
                    if (v == null) return;
                    State current = STATES.get(v);
                    if (current == null || current.activeProbe != p) return;
                    current.activeProbe = null;
                    if (!matches(v, current, p)) return;
                    Marker marker = parse(value);
                    if (marker != null && current.committedSeen) {
                        if (current.committedDocument == null) current.committedDocument = marker.document;
                        else if (!marker.document.equals(current.committedDocument)) {
                            // A same-URL cached document can announce pageshow
                            // before its native started callback. Never attach it
                            // to a different document's already-observed commit.
                            current.candidateDocument = null;
                            current.candidateEvent = null;
                            schedule(v, current);
                            return;
                        }
                    }
                    if (marker != null && marker.pending && !marker.event.equals(current.lastHandledEvent)) {
                        if (marker.event.equals(current.candidateEvent)
                                && marker.document.equals(current.candidateDocument)) {
                            repair(v, current, p, marker);
                        } else {
                            // Bind on positive evidence, including when the first
                            // commit-time query was null or read the old document.
                            current.candidateDocument = marker.document;
                            current.candidateEvent = marker.event;
                            current.probeAgain = false;
                            if (current.remaining == 0 && !current.confirmationCredit) {
                                current.confirmationCredit = true;
                                current.remaining = 1;
                            }
                            probe(v, current); // independently confirm current document/event
                            return;
                        }
                    } else {
                        current.candidateDocument = null;
                        current.candidateEvent = null;
                    }
                    if (!matches(v, current, p)) return;
                    if (current.probeAgain) {
                        current.probeAgain = false;
                        probe(v, current);
                    } else schedule(v, current);
                }
            });
        } catch (RuntimeException failure) {
            if (s.activeProbe == p) s.activeProbe = null;
            if (!s.queryFailureLogged) {
                s.queryFailureLogged = true;
                Log.w(TAG, "BFCache marker query was unavailable; retry is bounded", failure);
            }
            schedule(view, s);
        }
    }

    private static void repair(WebView view, final State s, final Probe p, final Marker marker) {
        Runnable completion = s.completion;
        if (completion == null || !matches(view, s, p)) return;
        try {
            completion.run();
        } catch (RuntimeException failure) {
            // Do not consume the event until the native call actually returns.
            Log.w(TAG, "BFCache native completion was unavailable; retry is bounded", failure);
            schedule(view, s);
            return;
        }
        if (!matches(view, s, p)) return;
        s.lastHandledEvent = marker.event;
        s.terminal = true;
        s.retryToken++;
        s.retryScheduled = false;
        s.probeAgain = false;
        // Request a draw now as well as after visual readiness. A missing visual
        // callback must not leave the native tab in a loading state.
        redraw(view);
        final WeakReference<WebView> weak = new WeakReference<WebView>(view);
        try {
            view.postVisualStateCallback(++s.visualRequest, new WebView.VisualStateCallback() {
                @Override public void onComplete(long requestId) {
                    WebView v = weak.get();
                    if (v == null) return;
                    State current = STATES.get(v);
                    if (current != null && sameGeneration(current, p) && sameUrl(v, p.url)
                            && marker.event.equals(current.lastHandledEvent)) redraw(v);
                }
            });
        } catch (RuntimeException failure) {
            Log.w(TAG, "BFCache visual callback was unavailable", failure);
        }
    }

    private static void redraw(WebView view) {
        if (view.isAttachedToWindow() && view.isShown()) view.postInvalidateOnAnimation();
    }
    private static Marker parse(String value) {
        if (value == null) return null;
        Matcher m = RESULT.matcher(value);
        return m.matches() ? new Marker(m.group(1), m.group(2), "1".equals(m.group(3))) : null;
    }
    private static State state(WebView view) {
        State s = STATES.get(view);
        if (s == null) { s = new State(); STATES.put(view, s); }
        return s;
    }
    private static boolean sameGeneration(State s, Probe p) {
        return s.generation == p.generation && s.commitToken == p.commitToken && p.url.equals(s.url);
    }
    private static boolean matches(WebView view, State s, Probe p) {
        return !s.terminal && sameGeneration(s, p) && sameUrl(view, p.url);
    }
    private static boolean sameUrl(WebView view, String url) {
        if (url == null || url.length() == 0) return false;
        try { return url.equals(view.getUrl()); }
        catch (RuntimeException destroyed) { return false; }
    }
    private static boolean onMainThread() { return Looper.myLooper() == Looper.getMainLooper(); }

    private static final class Listener implements InvocationHandler {
        @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("getSupportedFeatures".equals(name)) return new String[] { "WEB_MESSAGE_LISTENER" };
            if ("toString".equals(name)) return "BerryBFCacheRestoreListener";
            if ("hashCode".equals(name)) return Integer.valueOf(System.identityHashCode(this));
            if ("equals".equals(name)) return Boolean.valueOf(args != null && args.length == 1 && proxy == args[0]);
            if (!"onPostMessage".equals(name) || args == null || args.length != 5
                    || !(args[0] instanceof WebView) || !(args[1] instanceof InvocationHandler)
                    || !Boolean.TRUE.equals(args[3])) return null;
            Object data;
            try {
                Class<?> boundary = Class.forName("org.chromium.support_lib_boundary.WebMessageBoundaryInterface", false,
                    RestoreCoordinator.class.getClassLoader());
                Method getData = boundary.getMethod("getData");
                data = ((InvocationHandler) args[1]).invoke(null, getData, new Object[0]);
            } catch (Throwable malformed) {
                if (malformed instanceof Error) throw malformed;
                return null;
            }
            if (MESSAGE.equals(data)) message((WebView) args[0]);
            return null;
        }
    }
}
