package hiro.berrybfcache;

import android.os.Looper;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/**
 * Completes only a positively identified persisted pageshow or native Page restore.
 * Ordinary commits and visual callbacks are notifications, not restore proof.
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
    private static final int MAX_URL_CHARS = 8192;
    private static final Pattern DOCUMENT = Pattern.compile("[a-z0-9]{8,64}");
    private static final Pattern SERIAL = Pattern.compile("[0-9]{1,12}");
    private static final String SCRIPT =
        "(function(){'use strict';if(window!==window.top)return;" +
        "var k='__hiroBerryBFCacheRestoreV1';if(Object.prototype.hasOwnProperty.call(window,k))return;" +
        "var s={id:Date.now().toString(36)+Math.random().toString(36).slice(2),serial:0,pending:false,restoreUrl:''};" +
        "Object.defineProperty(window,k,{value:s,enumerable:false,configurable:false,writable:false});" +
        "window.addEventListener('pagehide',function(){s.pending=false;s.restoreUrl='';});" +
        "window.addEventListener('pageshow',function(e){s.pending=e.persisted===true;s.restoreUrl='';if(!s.pending)return;" +
        "s.serial++;s.restoreUrl=window.location.href;try{var b=window.hiroBerryBFCacheRestoreV1;if(b&&typeof b.postMessage==='function')" +
        "b.postMessage('hiro-bfcache-restored-v1');}catch(ignore){}},true);})();";
    private static final String PROBE =
        "(function(){var s=window.__hiroBerryBFCacheRestoreV1;" +
        "if(!s||typeof s.id!=='string'||typeof s.serial!=='number'||typeof s.restoreUrl!=='string')return null;" +
        "var u=window.location.href;if(s.restoreUrl.length>8192||u.length>8192)return null;" +
        "return 'B2|'+s.id+'|'+s.serial+'|'+(s.pending===true?'1':'0')+'|'+encodeURIComponent(s.restoreUrl)+'|'+encodeURIComponent(u);})()";

    private static final class State {
        long generation;
        long commitToken;
        boolean committedSeen;
        String committedDocument;
        long retryToken;
        int remaining;
        boolean terminal;
        boolean retryScheduled;
        boolean probeAgain;
        boolean confirmationCredit;
        boolean queryFailureLogged;
        boolean startedBodyFinished;
        long nativeNavigationId;
        long nativeRestoreToken;
        String url;
        String candidateEvent;
        String candidateDocument;
        String candidateUrl;
        String lastHandledEvent;
        Runnable completion;
        Probe activeProbe;
    }

    private static final class Probe {
        final long generation;
        final long commitToken;
        final String url;
        final String observedUrl;
        Probe(State s, String observedUrl) {
            generation = s.generation; commitToken = s.commitToken; url = s.url; this.observedUrl = observedUrl;
        }
    }

    private static final class Marker {
        final String document;
        final String event;
        final boolean pending;
        final String restoreUrl;
        final String currentUrl;
        Marker(String document, String serial, boolean pending, String restoreUrl, String currentUrl) {
            this.document = document;
            event = document + ":" + serial;
            this.pending = pending && !"0".equals(serial);
            this.restoreUrl = restoreUrl; this.currentUrl = currentUrl;
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
        PresentationRecovery.cancel(view);
        RenderDiagnostics.cancel(view);
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
        s.candidateUrl = null;
        s.activeProbe = null;
        s.startedBodyFinished = false;
        s.nativeNavigationId = NativeRestoreTracker.navigationId(view);
        s.nativeRestoreToken = NativeRestoreTracker.consume(view, url);
        if (s.nativeRestoreToken != s.nativeNavigationId) s.nativeRestoreToken = 0;
        // The first check is posted so Berry's own onPageStarted body finishes
        // setting its loading state before our completion can run.
        schedule(view, s);
    }

    /** Native page identity can suppress loading-only initialization, never metadata updates. */
    public static boolean shouldSkipLoading(WebView view) {
        if (view == null || !onMainThread()) return false;
        State s = STATES.get(view);
        return s != null && !s.terminal && !s.startedBodyFinished && nativeProofCurrent(view, s, currentUrl(view));
    }

    /** Called at each normal exit of Berry's original onPageStarted body. */
    public static void startedBodyFinished(final WebView view, final String url) {
        if (view == null) return;
        if (!onMainThread()) {
            final WeakReference<WebView> weak = new WeakReference<WebView>(view);
            view.post(new Runnable() { public void run() { startedBodyFinished(weak.get(), url); } });
            return;
        }
        State s = STATES.get(view);
        if (s == null || s.terminal || url == null || !url.equals(s.url) || !nativeNavigationMatches(view, s)) return;
        s.startedBodyFinished = true;
        if (!tryNativeComplete(view, s)) schedule(view, s);
    }

    /** A late native proof is accepted only for the native navigation captured by started(). */
    public static void nativeRestored(final WebView view, final String url, final long token) {
        if (view == null || token <= 0) return;
        if (!onMainThread()) {
            final WeakReference<WebView> weak = new WeakReference<WebView>(view);
            view.post(new Runnable() { public void run() { nativeRestored(weak.get(), url, token); } });
            return;
        }
        State s = STATES.get(view);
        if (s == null || s.terminal || url == null || !url.equals(s.url) || token != s.nativeNavigationId
                || !nativeNavigationMatches(view, s)) return;
        s.nativeRestoreToken = token;
        if (!tryNativeComplete(view, s) && s.startedBodyFinished) schedule(view, s);
    }

    private static boolean nativeProofCurrent(WebView view, State s, String url) {
        return s.nativeRestoreToken > 0 && s.nativeRestoreToken == s.nativeNavigationId && url != null
            && nativeNavigationMatches(view, s) && NativeRestoreTracker.isCurrent(view, s.nativeRestoreToken, url);
    }
    private static boolean nativeNavigationMatches(WebView view, State s) {
        return s.nativeNavigationId == NativeRestoreTracker.navigationId(view);
    }
    private static boolean tryNativeComplete(WebView view, State s) {
        if (s.terminal || !s.startedBodyFinished || s.completion == null) return false;
        String url = currentUrl(view);
        if (!nativeProofCurrent(view, s, url)) return false;
        Probe p = new Probe(s, url);
        try {
            if (!completeAt(s, url)) return false;
        } catch (RuntimeException failure) {
            Log.w(TAG, "BFCache native proof completion was unavailable; retry is bounded", failure);
            return false;
        }
        if (!matches(view, s, p) || !nativeProofCurrent(view, s, url)) return false;
        completeState(view, s, p, "native:" + s.nativeRestoreToken);
        return true;
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
                s.candidateUrl = null;
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
        if (!nativeNavigationMatches(view, s)) return;
        if (tryNativeComplete(view, s)) return;
        String observedUrl = currentUrl(view);
        if (observedUrl == null) { schedule(view, s); return; }
        final Probe p = new Probe(s, observedUrl);
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
                    if (current.terminal || !sameGeneration(current, p) || !nativeNavigationMatches(v, current)) return;
                    if (!sameUrl(v, p.observedUrl)) {
                        clearCandidate(current);
                        schedule(v, current);
                        return;
                    }
                    Marker marker = parse(value);
                    if (marker != null && !p.observedUrl.equals(marker.currentUrl)) marker = null;
                    if (marker != null && current.committedSeen) {
                        if (current.committedDocument == null) current.committedDocument = marker.document;
                        else if (!marker.document.equals(current.committedDocument)) {
                            // A same-URL cached document can announce pageshow
                            // before its native started callback. Never attach it
                            // to a different document's already-observed commit.
                            current.candidateDocument = null;
                            current.candidateEvent = null;
                            current.candidateUrl = null;
                            schedule(v, current);
                            return;
                        }
                    }
                    if (marker != null && marker.pending && p.url != null && p.url.equals(marker.restoreUrl)
                            && !marker.event.equals(current.lastHandledEvent)) {
                        if (marker.event.equals(current.candidateEvent)
                                && marker.document.equals(current.candidateDocument)
                                && marker.currentUrl.equals(current.candidateUrl)) {
                            repair(v, current, p, marker);
                        } else {
                            // Bind on positive evidence, including when the first
                            // commit-time query was null or read the old document.
                            current.candidateDocument = marker.document;
                            current.candidateEvent = marker.event;
                            current.candidateUrl = marker.currentUrl;
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
                        current.candidateUrl = null;
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
            if (!completeAt(s, p.observedUrl)) { schedule(view, s); return; }
        } catch (RuntimeException failure) {
            // Do not consume the event until the native call actually returns.
            Log.w(TAG, "BFCache native completion was unavailable; retry is bounded", failure);
            schedule(view, s);
            return;
        }
        if (!matches(view, s, p)) return;
        completeState(view, s, p, marker.event);
    }
    private static boolean completeAt(State s, String currentUrl) {
        if (s.completion instanceof NativeCompletion)
            return ((NativeCompletion) s.completion).completeAtUrl(currentUrl);
        if (s.completion != null && s.url != null && s.url.equals(currentUrl)) {
            // Legacy Runnables cannot be safely retargeted to another URL.
            s.completion.run(); return true;
        }
        return false;
    }
    private static void completeState(WebView view, final State s, final Probe p, final String event) {
        s.lastHandledEvent = event;
        s.terminal = true;
        s.retryToken++;
        s.retryScheduled = false;
        s.probeAgain = false;
        // Loading completion remains final even if presentation is delayed or
        // unavailable. Presentation owns a separate bounded lifecycle.
        try {
            RenderDiagnostics.begin(view);
        } catch (RuntimeException failure) {
            Log.w(TAG, "BFCache rendering diagnostics were unavailable", failure);
        }
        try {
            PresentationRecovery.start(view, new PresentationRecovery.Proof() {
                @Override public boolean isCurrent(WebView v) {
                    State current = STATES.get(v);
                    if (current != s || !sameGeneration(current, p) || !nativeNavigationMatches(v, current)
                            || !event.equals(current.lastHandledEvent)) return false;
                    // Native Page proof can follow a verified same-document URL change.
                    // Without that independent proof, an altered URL ends presentation work.
                    return sameUrl(v, p.observedUrl) || nativeProofCurrent(v, current, currentUrl(v));
                }
            });
        } catch (RuntimeException failure) {
            Log.w(TAG, "BFCache presentation recovery was unavailable", failure);
        }
    }
    private static void clearCandidate(State s) {
        s.candidateDocument = null; s.candidateEvent = null; s.candidateUrl = null;
    }
    private static Marker parse(String value) {
        if (value == null || value.length() > 148000 || !value.startsWith("\"B2|") || !value.endsWith("\"")) return null;
        String[] fields = value.substring(1, value.length() - 1).split("\\|", -1);
        if (fields.length != 6 || !"B2".equals(fields[0]) || !DOCUMENT.matcher(fields[1]).matches()
                || !SERIAL.matcher(fields[2]).matches() || !("0".equals(fields[3]) || "1".equals(fields[3]))) return null;
        String restored = decodeUrl(fields[4]);
        String current = decodeUrl(fields[5]);
        if (restored == null || current == null || current.length() == 0) return null;
        return new Marker(fields[1], fields[2], "1".equals(fields[3]), restored, current);
    }
    /** Strict encodeURIComponent inverse. Raw '+' and malformed UTF-8 are rejected. */
    private static String decodeUrl(String encoded) {
        if (encoded.length() > MAX_URL_CHARS * 9) return null;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(encoded.length());
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c == '%') {
                if (i + 2 >= encoded.length()) return null;
                int high = hex(encoded.charAt(++i));
                int low = hex(encoded.charAt(++i));
                if (high < 0 || low < 0) return null;
                bytes.write((high << 4) | low);
            } else if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || "-_.!~*'()".indexOf(c) >= 0) bytes.write(c);
            else return null;
        }
        try {
            String result = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
            return result.length() <= MAX_URL_CHARS ? result : null;
        } catch (java.nio.charset.CharacterCodingException invalid) { return null; }
    }
    private static int hex(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        return -1;
    }
    private static State state(WebView view) {
        State s = STATES.get(view);
        if (s == null) { s = new State(); STATES.put(view, s); }
        return s;
    }
    private static boolean sameGeneration(State s, Probe p) {
        return s.generation == p.generation && s.commitToken == p.commitToken && p.url != null && p.url.equals(s.url);
    }
    private static boolean matches(WebView view, State s, Probe p) {
        return !s.terminal && sameGeneration(s, p) && nativeNavigationMatches(view, s) && sameUrl(view, p.observedUrl);
    }
    private static String currentUrl(WebView view) {
        try {
            String url = view.getUrl();
            return url != null && url.length() > 0 && url.length() <= MAX_URL_CHARS ? url : null;
        } catch (RuntimeException destroyed) { return null; }
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
