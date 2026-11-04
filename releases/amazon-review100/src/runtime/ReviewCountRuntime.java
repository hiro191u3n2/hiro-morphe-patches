package app.hiro.amazonreviews;

import android.webkit.WebView;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.regex.Pattern;

/** Search-only runtime. No requests, credentials, storage or background worker. */
public final class ReviewCountRuntime {
    private static final Pattern HOST = Pattern.compile("(?:(?:www|m|smile)\\.)?amazon\\.(?:co\\.jp|com|co\\.uk|de|fr|it|es|ca|com\\.au|com\\.br|com\\.mx|in|nl|se|pl|com\\.be|ie|sg|ae|sa|eg|com\\.tr|co\\.za)");
    private ReviewCountRuntime() {}

    private static boolean isSearchUrl(String url) {
        if (url == null) return false;
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getRawUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) return false;
            String host = uri.getHost();
            if (host == null || !HOST.matcher(host.toLowerCase(Locale.ROOT)).matches()) return false;
            String path = uri.getRawPath();
            if ("/s/browse".equals(path) || (path != null && path.startsWith("/s/browse/"))) return false;
            return "/s".equals(path) || (path != null && path.startsWith("/s/")) || "/gp/aw/search/s".equals(path) || "/gp/aw/s".equals(path) || (path != null && path.startsWith("/gp/aw/s/"));
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

    /** Queue on the view thread and re-check the current URL against stale callbacks. */
    public static void inject(final WebView view, String callbackUrl) {
        if (view == null || !isSearchUrl(callbackUrl)) return;
        try {
            view.post(new Runnable() {
                @Override public void run() {
                    try {
                        if (isSearchUrl(view.getUrl())) view.evaluateJavascript(ScriptPayload.SCRIPT, null);
                    } catch (RuntimeException ignored) {
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
