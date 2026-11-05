package app.hiro.amazonreviews;

import android.webkit.WebView;
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
            return path.isEmpty() || "/".equals(path) || path.matches("/ref=[^/]*/?") || path.matches("/gp/aw(?:/|/ref=[^/]*/?)?");
        } catch (Exception ignored) { return false; }
    }

    private static boolean identityField(String key) {
        return "k".equals(key) || "keywords".equals(key) || "field-keywords".equals(key) ||
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

    /** Bootstrap only current trusted home/search documents. Current search identity wins
     * over stale callbacks; matching-site callbacks support home-URL SPA search transitions. */
    public static void inject(final WebView view, final String callbackUrl) {
        if (view == null) return;
        try {
            view.post(new Runnable() {
                @Override public void run() {
                    try {
                        String current = view.getUrl();
                        if (!isBootstrapUrl(current)) return;
                        String search = isSearchUrl(current) ? current :
                            isSearchUrl(callbackUrl) && site(current).equals(site(callbackUrl)) ? callbackUrl : null;
                        String token = searchToken(view, search);
                        String hint = search == null ? "false" : "true";
                        String argument = token == null ? "null" : "\"" + token + "\"";
                        view.evaluateJavascript(ScriptPayload.SCRIPT + ";if(window.__hiroReviewCountSort103){window.__hiroReviewCountSort103.setSearchHint(" + hint + "," + argument + ");}", null);
                    } catch (Exception ignored) {
                        // A detached/destroyed WebView must not affect shopping navigation.
                    } catch (LinkageError ignored) {
                        // Fail open if an unexpected WebView implementation lacks the API.
                    }
                }
            });
        } catch (RuntimeException ignored) {
        }
    }
}
