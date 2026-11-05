package app.hiro.amazonreviews;

import android.webkit.WebView;
import android.webkit.ValueCallback;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.net.URI;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/** Trusted search bootstrap; native identity state is bounded, weak and memory-only. */
public final class ReviewCountRuntime {
    private static final Pattern HOST = Pattern.compile("(?:(?:www|m|smile)\\.)?amazon\\.(?:co\\.jp|com|co\\.uk|de|fr|it|es|ca|com\\.au|com\\.br|com\\.mx|in|nl|se|pl|com\\.be|ie|sg|ae|sa|eg|com\\.tr|co\\.za)");
    private static final Map<WebView, SearchIdentity> SEARCHES = new WeakHashMap<WebView, SearchIdentity>();
    private static long nextToken;
    private static final class SearchIdentity {
        final String digest;
        final String token;
        SearchIdentity(String digest, String token) { this.digest = digest; this.token = token; }
    }
    // Search-context events can change the result cohort without changing the
    // WebView URL. Only digests and opaque tokens survive this call boundary.
    private static final Map<WebView, NativeSearch> NATIVE_SEARCHES = new WeakHashMap<WebView, NativeSearch>();
    private static final Map<WebView, String> REVOKED_SEARCHES = new WeakHashMap<WebView, String>();
    private static final class NativeSearch {
        final String digest, token;
        final long expires;
        String document, market;
        NativeSearch(String digest, String token, String document, String market) {
            this.digest = digest; this.token = token; this.document = document; this.market = market;
            this.expires = SystemClock.uptimeMillis() + 5000;
        }
    }
    private ReviewCountRuntime() {}

    private static boolean isAmazonUrl(String url) {
        if (url == null) return false;
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getRawUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) return false;
            String host = uri.getHost();
            return host != null && HOST.matcher(host.toLowerCase(Locale.ROOT)).matches();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isSearchUrl(String url) {
        if (!isAmazonUrl(url)) return false;
        try {
            String path = new URI(url).getRawPath();
            if (path == null) return false;
            path = path.replaceFirst("^/-/[A-Za-z]{2}(?:_[A-Za-z]{2})?(?=/)", "");
            if ("/s/browse".equals(path) || path.startsWith("/s/browse/")) return false;
            return path.matches("/(?:[^/]+/)?s(?:/.*)?") || "/gp/search".equals(path) ||
                "/gp/aw/search/s".equals(path) || "/gp/aw/s".equals(path) || path.startsWith("/gp/aw/s/") ||
                "/mn/search".equals(path) || "/search-by-list".equals(path) || path.startsWith("/search-by-list/");
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Preserve the original encoding/order of every unrelated query parameter. */
    public static String normalizeUrl(String url) {
        if (!isSearchUrl(url)) return url;
        try {
            int hash = url.indexOf('#');
            String fragment = hash < 0 ? "" : url.substring(hash);
            String base = hash < 0 ? url : url.substring(0, hash);
            int question = base.indexOf('?');
            String path = question < 0 ? base : base.substring(0, question);
            String query = question < 0 ? "" : base.substring(question + 1);
            StringBuilder kept = new StringBuilder();
            if (!query.isEmpty()) {
                for (String pair : query.split("&", -1)) {
                    int equals = pair.indexOf('=');
                    String rawKey = equals < 0 ? pair : pair.substring(0, equals);
                    String key = URLDecoder.decode(rawKey, "UTF-8");
                    if ("s".equals(key) || "sort".equals(key)) continue;
                    if (kept.length() > 0) kept.append('&');
                    kept.append(pair);
                }
            }
            if (kept.length() > 0) kept.append('&');
            kept.append("s=review-count-rank");
            return path + "?" + kept + fragment;
        } catch (Exception ignored) {
            return url;
        }
    }

    private static String normalizedPath(URI uri) {
        String path = uri.getRawPath();
        return path == null ? "" : path.replaceFirst("^/-/[A-Za-z]{2}(?:_[A-Za-z]{2})?(?=/)", "");
    }

    private static String site(String url) throws Exception {
        return new URI(url).getHost().toLowerCase(Locale.ROOT).replaceFirst("^(?:www|m|smile)\\.", "");
    }

    private static boolean isBootstrapUrl(String url) {
        if (!isAmazonUrl(url)) return false;
        if (isSearchUrl(url)) return true;
        try {
            String path = normalizedPath(new URI(url));
            // Exact home routes recognized by the supplied APK's MShopRuleConfig.
            return path.isEmpty() || "/".equals(path) || path.matches("/ref=[^/]*/?") || path.matches("/gp/aw(?:/|/ref=[^/]*/?)?") ||
                "/gp/homepage.html".equals(path) || "/gp/aw/h.html".equals(path) ||
                "/gp/aw/homepage.html".equals(path) || "/gp/gw/ajax/mshop.html".equals(path);
        } catch (Exception ignored) { return false; }
    }

    private static boolean identityField(String key) {
        return "k".equals(key) || "keyword".equals(key) || "keywords".equals(key) || "field-keywords".equals(key) ||
            "i".equals(key) || "rh".equals(key) || "bbn".equals(key) || "node".equals(key) ||
            "me".equals(key) || "emi".equals(key) || key.matches("p_[A-Za-z0-9_:-]{1,80}");
    }

    /** Store a bounded digest, never query text, and send only an opaque token to JavaScript. */
    private static String searchDigest(String url) {
        if (url == null || url.length() > 16384 || !isSearchUrl(url)) return null;
        try {
            URI uri = new URI(url);
            List<String> fields = new ArrayList<String>();
            String query = uri.getRawQuery();
            if (query != null) {
                String[] pairs = query.split("&", -1);
                if (pairs.length > 128) return null;
                for (String pair : pairs) {
                    int equals = pair.indexOf('=');
                    String key = URLDecoder.decode(equals < 0 ? pair : pair.substring(0, equals), "UTF-8");
                    if (!identityField(key)) continue;
                    String value = URLDecoder.decode(equals < 0 ? "" : pair.substring(equals + 1), "UTF-8");
                    if (value.length() > 4096) return null;
                    fields.add(key.length() + ":" + key + value.length() + ":" + value);
                }
            }
            Collections.sort(fields);
            StringBuilder canonical = new StringBuilder(site(url));
            for (String field : fields) canonical.append('|').append(field);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes("UTF-8"));
            StringBuilder encoded = new StringBuilder(64);
            for (byte part : digest) encoded.append(Character.forDigit((part >>> 4) & 15, 16)).append(Character.forDigit(part & 15, 16));
            return encoded.toString();
        } catch (Exception ignored) { return null; }
    }

    private static String searchToken(WebView view, String searchUrl) {
        String digest = searchDigest(searchUrl);
        if (digest == null) return null;
        synchronized (SEARCHES) {
            SearchIdentity identity = SEARCHES.get(view);
            if (identity == null || !identity.digest.equals(digest)) {
                if (identity == null && SEARCHES.size() >= 64) SEARCHES.remove(SEARCHES.keySet().iterator().next());
                identity = new SearchIdentity(digest, Long.toHexString(++nextToken));
                SEARCHES.put(view, identity);
            }
            return identity.token;
        }
    }

    private static final String VERSION = "1.0.8";
    private static final String STATE = "__hiroReviewCountSort108";
    private static final Map<WebView, Request> REQUESTS = new WeakHashMap<WebView, Request>();
    private static final Set<String> FAILURE_NOTICES = new HashSet<String>();
    private static boolean loadedNotice;
    private static long generation;
    private static final class Ui { static final Handler HANDLER = new Handler(Looper.getMainLooper()); }

    private static String documentKey(String value) {
        if (value == null) return null;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));
            StringBuilder out = new StringBuilder(64);
            for (byte b : digest) out.append(Character.forDigit((b >>> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
            return out.toString();
        } catch (Exception ignored) { return null; }
    }
    /** Bind to navigation meaning, not changing tracking fields or the fragment. */
    private static String navigationKey(String url) {
        if (url == null || url.length() > 16384 || !isAmazonUrl(url)) return null;
        try {
            URI uri = new URI(url);
            String path = normalizedPath(uri).replaceFirst("/ref=[^/]*/*$", "").replaceFirst("/$", "");
            if (path.isEmpty()) path = "/";
            List<String> fields = new ArrayList<String>();
            String query = uri.getRawQuery();
            if (query != null) {
                String[] pairs = query.split("&", -1);
                if (pairs.length > 128) return null;
                for (String pair : pairs) {
                    int split = pair.indexOf('=');
                    String key = URLDecoder.decode(split < 0 ? pair : pair.substring(0, split), "UTF-8");
                    if (!identityField(key)) continue;
                    String value = URLDecoder.decode(split < 0 ? "" : pair.substring(split + 1), "UTF-8");
                    if (value.length() > 4096) return null;
                    fields.add(key.length() + ":" + key + value.length() + ":" + value);
                }
            }
            Collections.sort(fields);
            StringBuilder canonical = new StringBuilder(site(url)).append('|').append(path.length()).append(':').append(path);
            for (String field : fields) canonical.append('|').append(field);
            return documentKey(canonical.toString());
        } catch (Exception ignored) { return null; }
    }
    private static boolean pendingDocument(String url) { return url == null || url.isEmpty() || "about:blank".equals(url); }
    private static String route(String url) { return isSearchUrl(url) ? "search" : isBootstrapUrl(url) ? "home" : "other"; }
    // Cancel a pending-document retry if navigation reached a known excluded destination.
    // This is deliberately not a home/search allowlist: unfamiliar trusted routes may host search.
    private static boolean excludedDestination(String url) {
        try { return normalizedPath(new URI(url)).matches("^/(?:dp(?:/.*)?|gp/(?:product|aw/d|aw/c|cart|buy|checkout|checkoutportal|your-account|your-orders|css)(?:/.*)?|hz/(?:cart|checkout)(?:/.*)?|cart(?:/.*)?|checkout(?:/.*)?|ap(?:/.*)?|your-orders(?:/.*)?)$"); }
        catch (Exception ignored) { return true; }
    }
    private static String nextOpaqueToken() {
        // The n prefix distinguishes an actual native search event from the
        // legacy URL-derived token without exposing any query information.
        synchronized (SEARCHES) { return "n" + Long.toHexString(++nextToken); }
    }
    private static boolean pendingNativeSearch(WebView view) {
        synchronized (NATIVE_SEARCHES) {
            NativeSearch search = NATIVE_SEARCHES.get(view);
            if (search == null || search.document != null) return false;
            if (SystemClock.uptimeMillis() > search.expires) { NATIVE_SEARCHES.remove(view); return false; }
            return true;
        }
    }
    private static void clearNativeSearch(WebView view) {
        synchronized (NATIVE_SEARCHES) { NATIVE_SEARCHES.remove(view); }
    }
    private static boolean revokedDocument(WebView view, String url) {
        synchronized (REVOKED_SEARCHES) {
            String document = REVOKED_SEARCHES.get(view);
            if (document == null) return false;
            if (document.equals(navigationKey(url))) return true;
            REVOKED_SEARCHES.remove(view); return false;
        }
    }
    /** An actual search fragment exit must not leave mobile ranking active on home. */
    public static void revokeSearch(final WebView view) {
        if (view == null) return;
        final WeakReference<WebView> weak = new WeakReference<WebView>(view);
        Runnable exit = new Runnable() { @Override public void run() {
            WebView active = weak.get(); if (active == null) return;
            clearNativeSearch(active);
            synchronized (SEARCHES) { SEARCHES.remove(active); }
            synchronized (REQUESTS) { Request old = REQUESTS.get(active); if (old != null) old.finish("superseded"); }
            try {
                String url = active.getUrl(), document = navigationKey(url);
                if (document == null) return;
                synchronized (REVOKED_SEARCHES) {
                    if (!REVOKED_SEARCHES.containsKey(active) && REVOKED_SEARCHES.size() >= 64) REVOKED_SEARCHES.remove(REVOKED_SEARCHES.keySet().iterator().next());
                    REVOKED_SEARCHES.put(active, document);
                }
                inject(active, null);
            } catch (RuntimeException ignored) {}
        }};
        if (Looper.myLooper() == Looper.getMainLooper()) exit.run();
        else Ui.HANDLER.post(exit);
    }
    private static String keywordDigest(String url) {
        if (!isSearchUrl(url) || url.length() > 16384) return null;
        try {
            String query = new URI(url).getRawQuery(), value = null;
            if (query == null) return null;
            String[] pairs = query.split("&", -1);
            if (pairs.length > 128) return null;
            for (String pair : pairs) {
                int split = pair.indexOf('=');
                String key = URLDecoder.decode(split < 0 ? pair : pair.substring(0, split), "UTF-8");
                if (!"k".equals(key) && !"keyword".equals(key) && !"keywords".equals(key) && !"field-keywords".equals(key)) continue;
                String decoded = URLDecoder.decode(split < 0 ? "" : pair.substring(split + 1), "UTF-8");
                if (decoded.isEmpty() || decoded.length() > 4096 || value != null && !value.equals(decoded)) return null;
                value = decoded;
            }
            return value == null ? null : documentKey(value);
        } catch (Exception ignored) { return null; }
    }
    private static String nativeToken(WebView view, String url) {
        synchronized (NATIVE_SEARCHES) {
            NativeSearch search = NATIVE_SEARCHES.get(view);
            if (search == null) return null;
            if (!isAmazonUrl(url) || excludedDestination(url)) { NATIVE_SEARCHES.remove(view); return null; }
            String document = navigationKey(url);
            if (document == null) { NATIVE_SEARCHES.remove(view); return null; }
            String market;
            try { market = site(url); } catch (Exception ignored) { NATIVE_SEARCHES.remove(view); return null; }
            if (search.document == null && SystemClock.uptimeMillis() <= search.expires) { search.document = document; search.market = market; }
            if (!document.equals(search.document)) { NATIVE_SEARCHES.remove(view); return null; }
            return search.token;
        }
    }

    /** Called by the APK's actual search-context update, including same-URL SPA searches. */
    public static void authoritativeSearch(final WebView view, String keywords) {
        // This APK method also updates searchbar metadata before a navigation
        // decision. Its original null/empty behavior is a no-op, not an exit.
        if (view == null || keywords == null || keywords.isEmpty() || keywords.length() > 4096) return;
        final String digest = documentKey(keywords);
        if (digest == null) return;
        announceNativeSearch(view, digest);
    }
    /** The actual search fragment has become visible, even without mKeywords. */
    public static void resumeSearch(final WebView view) {
        if (view != null) announceNativeSearch(view, null);
    }
    private static void announceNativeSearch(final WebView view, final String announcedDigest) {
        final WeakReference<WebView> weak = new WeakReference<WebView>(view);
        Runnable event = new Runnable() { @Override public void run() {
            WebView active = weak.get(); if (active == null) return;
            try {
                String url = active.getUrl();
                if (!pendingDocument(url) && (!isAmazonUrl(url) || excludedDestination(url))) return;
                String document = pendingDocument(url) ? null : navigationKey(url);
                String market = pendingDocument(url) ? null : site(url);
                String digest = announcedDigest != null ? announcedDigest : keywordDigest(url);
                if (document != null) nativeToken(active, url);
                synchronized (REVOKED_SEARCHES) { REVOKED_SEARCHES.remove(active); }
                synchronized (NATIVE_SEARCHES) {
                    NativeSearch previous = NATIVE_SEARCHES.get(active);
                    if (previous == null || digest != null && !digest.equals(previous.digest) ||
                        !(document == null ? previous.document == null : document.equals(previous.document)) ||
                        document == null && SystemClock.uptimeMillis() > previous.expires) {
                        if (previous == null && NATIVE_SEARCHES.size() >= 64) NATIVE_SEARCHES.remove(NATIVE_SEARCHES.keySet().iterator().next());
                        NATIVE_SEARCHES.put(active, new NativeSearch(digest, nextOpaqueToken(), document, market));
                    }
                }
                inject(active, null);
            } catch (Exception ignored) {}
        }};
        if (Looper.myLooper() == Looper.getMainLooper()) event.run();
        else Ui.HANDLER.post(event);
    }
    private static boolean namedError(String name) {
        return Arrays.asList("TypeError", "ReferenceError", "SyntaxError", "RangeError", "Error").contains(name);
    }
    private static void toast(WebView view, String text) {
        if (Looper.myLooper() != Looper.getMainLooper()) return;
        try { Toast.makeText(view.getContext(), text, Toast.LENGTH_SHORT).show(); } catch (RuntimeException ignored) {}
    }
    private static void failureNotice(WebView view, String code, Receipt receipt, boolean candidate) {
        if (!candidate && !code.startsWith("E_JS_")) return;
        synchronized (FAILURE_NOTICES) {
            if (FAILURE_NOTICES.size() >= 3 || !FAILURE_NOTICES.add(code)) return;
        }
        String counts = receipt == null || receipt.phase == null ? "" : " (R" + receipt.roots + " C" + receipt.cards + " K" + receipt.known + ")";
        String label = code.startsWith("E_JS_") ? "JSエラー" : code.equals("E_INACTIVE") ? "対象認識なし" :
            code.equals("E_UNREADABLE") ? "件数順を確認できません" : code.equals("E_LAYOUT") ? "検索一覧を認識できません" :
            code.equals("E_POST") ? "実行予約失敗" : code.equals("E_NO_DOC") ? "画面待機終了" : "JS応答未確認";
        toast(view, "評価件数順 v" + VERSION + "\n" + label + " " + code + counts);
    }
    static String lastStage(WebView view) {
        synchronized (REQUESTS) { Request r = REQUESTS.get(view); return r == null ? "none" : r.stage; }
    }

    private static String wrapper(String route, boolean hint, String token, boolean fresh) {
        String prefix = "{v:'" + VERSION + "',route:'" + route + "',";
        String quotedToken = token == null ? "null" : "'" + token + "'";
        return "(function(){try{" +
            "if(typeof document==='undefined'||!document.documentElement)return JSON.stringify(" + prefix + "stage:'no-document',report:null,error:null});" +
            "var s=window." + STATE + ";if(!s){" + ScriptPayload.SCRIPT + ";s=window." + STATE + ";}" +
            (fresh ? "else if(typeof s.schedule==='function'){s.schedule();}" : "") +
            "if(!s||typeof s.report!=='function'||typeof s.setSearchHint!=='function'||typeof s.setNativeCheckpoint!=='function')return JSON.stringify(" + prefix + "stage:'no-state',report:null,error:null});" +
            "s.setNativeCheckpoint('" + VERSION + "','" + route + "');s.setSearchHint(" + hint + "," + quotedToken + ");" +
            "return JSON.stringify(" + prefix + "stage:'ack',report:s.report(),error:null});" +
            "}catch(e){var n='Error';try{n=e&&e.name;}catch(ignored){}if(['TypeError','ReferenceError','SyntaxError','RangeError','Error'].indexOf(n)<0)n='Error';" +
            "return JSON.stringify(" + prefix + "stage:'js-error',report:null,error:n});}})()";
    }

    private static Object json(String text) throws Exception {
        // Only an outer JSON string or the two-level fixed envelope/report is accepted.
        // Reject excessive nesting before the platform parser can recurse.
        int depth = 0; boolean quoted = false, escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) { if (escaped) escaped = false; else if (c == '\\') escaped = true; else if (c == '"') quoted = false; }
            else if (c == '"') quoted = true;
            else if (c == '[' || c == ']') throw new IllegalArgumentException("Receipt array");
            else if (c == '{' && ++depth > 2) throw new IllegalArgumentException("Receipt nesting");
            else if (c == '}') depth--;
        }
        JSONTokener tokener = new JSONTokener(text);
        Object value = tokener.nextValue();
        if (tokener.nextClean() != 0) throw new IllegalArgumentException("Trailing JSON");
        return value;
    }
    private static void keys(JSONObject object, String... expected) {
        if (object.length() != expected.length) throw new IllegalArgumentException("Receipt shape");
        for (String name : expected) if (!object.has(name)) throw new IllegalArgumentException("Receipt key");
    }
    private static String string(JSONObject object, String key) {
        Object value = object.opt(key);
        if (!(value instanceof String)) throw new IllegalArgumentException("Receipt string");
        return (String) value;
    }
    private static boolean nil(Object value) { return value == null || value == JSONObject.NULL; }
    private static int count(JSONObject object, String key) {
        Object value = object.opt(key);
        if (!(value instanceof Number)) throw new IllegalArgumentException("Receipt number");
        double number = ((Number) value).doubleValue();
        if (number < 0 || number > 100 || number != Math.floor(number)) throw new IllegalArgumentException("Receipt range");
        return (int) number;
    }
    private static final class Receipt {
        String stage, phase, error; int roots, cards, known;
    }
    private static Receipt receipt(String raw, String expectedRoute) {
        if (raw == null || raw.length() > 4096) return null;
        try {
            Object parsed = json(raw);
            // evaluateJavascript JSON-serializes the JSON.stringify return value once more.
            if (!(parsed instanceof String)) return null;
            parsed = json((String) parsed);
            if (!(parsed instanceof JSONObject)) return null;
            JSONObject envelope = (JSONObject) parsed;
            keys(envelope, "v", "stage", "route", "report", "error");
            if (!VERSION.equals(string(envelope, "v")) || !expectedRoute.equals(string(envelope, "route"))) return null;
            Receipt result = new Receipt(); result.stage = string(envelope, "stage");
            Object error = envelope.opt("error"), report = envelope.opt("report");
            if (result.stage.equals("js-error")) {
                if (!(error instanceof String) || !namedError((String) error) || !nil(report)) return null;
                result.error = (String) error; return result;
            }
            if (!nil(error)) return null;
            if (result.stage.equals("no-document") || result.stage.equals("no-state")) return nil(report) ? result : null;
            if (!result.stage.equals("ack") || !(report instanceof JSONObject)) return null;
            JSONObject body = (JSONObject) report;
            keys(body, "version", "phase", "roots", "cards", "known", "status", "error", "limited");
            if (!VERSION.equals(string(body, "version"))) return null;
            result.phase = string(body, "phase");
            if (!Arrays.asList("inactive","waiting","collecting","ready","frozen","awaiting","unreadable","disposed","error").contains(result.phase)) return null;
            result.roots = count(body, "roots"); result.cards = count(body, "cards"); result.known = count(body, "known");
            if (result.known > result.cards || !(body.opt("status") instanceof Boolean) || !(body.opt("limited") instanceof Boolean)) return null;
            error = body.opt("error");
            if (!nil(error) && (!(error instanceof String) || !namedError((String) error))) return null;
            result.error = nil(error) ? null : (String) error;
            return result;
        } catch (Exception ignored) { return null; }
    }

    private static final class Request implements Runnable {
        final WeakReference<WebView> view; final long id, deadline;
        String callbackUrl, document, stage = "pending", route;
        int retries, attempt; boolean done, candidate, suppressed;
        Runnable timeout;
        Request(WebView view, String callbackUrl) {
            this.view = new WeakReference<WebView>(view); this.callbackUrl = callbackUrl;
            this.id = ++generation; this.deadline = SystemClock.uptimeMillis() + 5000;
        }
        boolean current(WebView value) {
            synchronized (REQUESTS) { Request r = REQUESTS.get(value); return !done && r == this && r.id == id; }
        }
        void finish(String value) {
            done = true; stage = value; callbackUrl = null;
            Ui.HANDLER.removeCallbacks(this);
            if (timeout != null) Ui.HANDLER.removeCallbacks(timeout);
        }
        void fail(WebView value, String stage, String code, Receipt result) {
            finish(stage); failureNotice(value, code, result, candidate);
        }
        boolean sameDocument(WebView value) {
            try { String now = value.getUrl(); return document != null && document.equals(navigationKey(now)); }
            catch (RuntimeException ignored) { return false; }
        }
        void retry(WebView value, String finalStage, String code) {
            if (timeout != null) Ui.HANDLER.removeCallbacks(timeout);
            long delay = retries == 0 ? 250 : 750;
            if (retries >= 2 || SystemClock.uptimeMillis() + delay >= deadline) { fail(value, finalStage, code, null); return; }
            retries++; stage = "retry";
            if (!Ui.HANDLER.postDelayed(this, delay)) fail(value, "post-failed", "E_POST", null);
        }
        @Override public void run() {
            final WebView value = view.get();
            if (value == null || !current(value)) return;
            try {
                String now = value.getUrl();
                if (document != null && !sameDocument(value)) { finish("superseded"); return; }
                if (!isAmazonUrl(now)) {
                    boolean nativePending = pendingNativeSearch(value);
                    candidate = nativePending || isSearchUrl(callbackUrl);
                    if (document == null && pendingDocument(now) && (nativePending || isAmazonUrl(callbackUrl))) retry(value, "no-document", "E_NO_DOC");
                    else { clearNativeSearch(value); revokedDocument(value, now); finish("blocked"); }
                    return;
                }
                if (document == null && retries > 0 && (pendingNativeSearch(value) || isSearchUrl(callbackUrl)) && excludedDestination(now)) { clearNativeSearch(value); finish("superseded"); return; }
                if (document == null) document = navigationKey(now);
                if (document == null) { finish("blocked"); return; }
                if (!loadedNotice) { loadedNotice = true; toast(value, "評価件数順 v" + VERSION + " 読込"); }
                route = route(now);
                String search = isSearchUrl(now) ? now : isSearchUrl(callbackUrl) && site(now).equals(site(callbackUrl)) ? callbackUrl : null;
                suppressed = revokedDocument(value, now);
                String explicitToken = nativeToken(value, now);
                candidate = !suppressed && (explicitToken != null || search != null);
                String token = suppressed ? "n0" : explicitToken != null ? explicitToken : searchToken(value, search);
                final int round = ++attempt;
                timeout = new Runnable() { @Override public void run() {
                    WebView active = view.get();
                    if (active == null || !current(active) || round != attempt) return;
                    if (!sameDocument(active)) { finish("superseded"); return; }
                    // A callback that never arrives is not evidence of a JS exception.
                    // Do not reinject while a heavy evaluation may still be running.
                    fail(active, "no-ack", "E_ACK", null);
                }};
                if (!Ui.HANDLER.postDelayed(timeout, Math.max(1, deadline - SystemClock.uptimeMillis()))) { fail(value, "post-failed", "E_POST", null); return; }
                value.evaluateJavascript(wrapper(route, candidate, token, retries == 0), new ValueCallback<String>() {
                    @Override public void onReceiveValue(String result) {
                        WebView active = view.get();
                        if (active == null || !current(active) || round != attempt) return;
                        if (!sameDocument(active)) { finish("superseded"); return; }
                        Ui.HANDLER.removeCallbacks(timeout);
                        Receipt reply = receipt(result, route);
                        if (reply == null) { retry(active, "no-ack", "E_ACK"); return; }
                        if (reply.stage.equals("no-document")) { retry(active, "no-document", "E_NO_DOC"); return; }
                        if (reply.stage.equals("no-state")) { fail(active, "no-state", "E_STATE", null); return; }
                        if (reply.error != null || "error".equals(reply.phase)) { fail(active, "js-error", "E_JS_" + (reply.error == null ? "Error" : reply.error), reply); return; }
                        if (!suppressed && "search".equals(route) && ("inactive".equals(reply.phase) || "disposed".equals(reply.phase))) { fail(active, "inactive", "E_INACTIVE", reply); return; }
                        finish("ack");
                        if (!suppressed && "unreadable".equals(reply.phase)) failureNotice(active, "E_UNREADABLE", reply, candidate);
                        else if (!suppressed && "awaiting".equals(reply.phase)) failureNotice(active, "E_LAYOUT", reply, candidate);
                    }
                });
            } catch (Exception ignored) { fail(value, "native-error", "E_NATIVE", null); }
            catch (LinkageError ignored) { fail(value, "native-error", "E_LINK", null); }
        }
    }

    /** Start a bounded, observable handshake on the main looper for trusted Amazon documents. */
    public static void inject(final WebView view, final String callbackUrl) {
        if (view == null) return;
        final WeakReference<WebView> weak = new WeakReference<WebView>(view);
        Runnable begin = new Runnable() { @Override public void run() {
            WebView active = weak.get(); if (active == null) return;
            Request next = new Request(active, callbackUrl);
            synchronized (REQUESTS) {
                Request old = REQUESTS.get(active); if (old != null) old.finish("superseded");
                if (old == null && REQUESTS.size() >= 64) {
                    WebView first = REQUESTS.keySet().iterator().next(); Request removed = REQUESTS.remove(first);
                    if (removed != null) removed.finish("superseded");
                }
                REQUESTS.put(active, next);
            }
            next.run();
        }};
        if (Looper.myLooper() == Looper.getMainLooper()) begin.run();
        else if (!Ui.HANDLER.post(begin)) {
            // A stopped main looper cannot safely run WebView or Toast work inline.
            synchronized (REQUESTS) {
                Request old = REQUESTS.get(view); if (old != null) old.finish("superseded");
                if (old == null && REQUESTS.size() >= 64) { Request removed = REQUESTS.remove(REQUESTS.keySet().iterator().next()); if (removed != null) removed.finish("superseded"); }
                Request failed = new Request(view, null); failed.finish("post-failed"); REQUESTS.put(view, failed);
            }
        }
    }
}
