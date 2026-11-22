package app.hiro.berry.links;

import android.app.Activity;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.content.pm.verify.domain.DomainVerificationUserState;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

public final class InstagramPostLinks {
    private static final String INSTAGRAM = "com.instagram.android";
    private static final Pattern POST = Pattern.compile("/(?:p|reel|tv)/[A-Za-z0-9_-]+/?");
    private static final Pattern PROFILE = Pattern.compile("/[A-Za-z0-9._]{1,30}/?");
    private static final Set<String> SITE_PATHS = new HashSet<>(Arrays.asList(
            "about", "accounts", "ads", "api", "audio", "challenge", "contact",
            "create", "developer", "developers", "direct", "directory", "download",
            "emails", "embed", "explore", "facebook", "graphql", "help", "legal",
            "linking", "nametag", "oauth", "p", "press", "privacy", "push", "qr",
            "reel", "reels", "session", "settings", "share", "static", "stories",
            "tags", "terms", "tv", "web", "_u", "robots.txt", "favicon.ico"));
    private InstagramPostLinks() {}

    private static final Pattern SHARE_TOKEN = Pattern.compile("[A-Za-z0-9_-]{1,256}");
    private static final Pattern RESULT_TOKEN = Pattern.compile("[A-Za-z0-9_-]{1,4096}={0,2}");
    private static final WeakHashMap<WebView, PendingShare> pendingShares = new WeakHashMap<>();

    private static final class PendingShare {
        final String token;
        final Uri source;
        final long started = SystemClock.elapsedRealtime();
        final boolean relay;

        PendingShare(Uri source, String token, boolean relay) {
            this.source = source;
            this.token = token;
            this.relay = relay;
        }
    }

    private static String shareToken(Uri url, boolean relay) {
        if (url == null || !"https".equalsIgnoreCase(url.getScheme())) return null;
        String host = url.getEncodedAuthority();
        String path = url.getEncodedPath();
        String token;
        if (relay) {
            if (!"www.google.com".equalsIgnoreCase(host) || !"/share.google".equals(path)) return null;
            token = url.getQueryParameter("q");
        } else {
            if (!"share.google".equalsIgnoreCase(host) || path == null || !path.startsWith("/")) return null;
            token = path.substring(1);
        }
        return token != null && SHARE_TOKEN.matcher(token).matches() ? token : null;
    }

    private static void rememberShare(WebView view, Uri url) {
        String token = shareToken(url, false);
        boolean relay = token == null;
        if (relay) token = shareToken(url, true);
        if (token != null) pendingShares.put(view, new PendingShare(url, token, relay));
    }

    private static boolean googlePath(Uri url, String path) {
        if (url == null || !"https".equalsIgnoreCase(url.getScheme()) || !path.equals(url.getEncodedPath())) return false;
        String host = url.getEncodedAuthority();
        return "www.google.com".equalsIgnoreCase(host) || "www.google.co.jp".equalsIgnoreCase(host);
    }

    // WebView does not call shouldOverrideUrlLoading for Berry's own loadUrl calls.
    public static void browserLoad(WebView view, String url) {
        pendingShares.remove(view);
        if (url != null) rememberShare(view, Uri.parse(url));
    }

    public static void pageFinished(WebView view, String url) {
        PendingShare share = pendingShares.get(view);
        if (share != null && url != null && (share.source.equals(Uri.parse(url))
                || (share.token != null && share.token.equals(shareToken(Uri.parse(url), share.relay))))) {
            pendingShares.remove(view);
        }
    }

    public static boolean allowsHandoff(WebView view, WebResourceRequest request) {
        if (Build.VERSION.SDK_INT < 31 || !request.isForMainFrame()) return false;
        PendingShare share = pendingShares.remove(view);
        if (!"GET".equalsIgnoreCase(request.getMethod())) return false;
        Uri url = request.getUrl();
        if (!request.isRedirect()) {
            if (!request.hasGesture()) return false;
            // Image-search source buttons can use an opaque /url?q=... token.
            // Let Google resolve it. Only the immediate Instagram redirect is eligible.
            String resultToken = googlePath(url, "/url") ? url.getQueryParameter("q") : null;
            if (resultToken != null && RESULT_TOKEN.matcher(resultToken).matches()) {
                pendingShares.put(view, new PendingShare(url, null, false));
            } else {
                rememberShare(view, url);
            }
            return !googlePath(url, "/imgres");
        }
        if (share == null || SystemClock.elapsedRealtime() - share.started > 60000) {
            return request.hasGesture() && !googlePath(url, "/imgres");
        }
        if (share.token != null && !share.relay && share.token.equals(shareToken(url, true))) {
            pendingShares.put(view, new PendingShare(url, share.token, true));
            return false;
        }
        if (share.token != null && googlePath(url, "/imgres")) return instagramUrl(url) != null;
        return url != null && ("www.instagram.com".equalsIgnoreCase(url.getEncodedAuthority())
                || "instagram.com".equalsIgnoreCase(url.getEncodedAuthority()));
    }

    public static Intent selectedIntent(Activity activity, WebResourceRequest request, boolean allowed) {
        if (Build.VERSION.SDK_INT < 31 || !allowed
                || !"GET".equalsIgnoreCase(request.getMethod()) || activity.isFinishing()
                || activity.isDestroyed()) return null;
        try {
            Uri url = instagramUrl(request.getUrl());
            if (url == null) return null;
            DomainVerificationManager domains = activity.getSystemService(DomainVerificationManager.class);
            if (domains == null) return null;
            DomainVerificationUserState state = domains.getDomainVerificationUserState(INSTAGRAM);
            if (state == null || !state.isLinkHandlingAllowed()) return null;
            Integer selected = state.getHostToStateMap().get(url.getHost().toLowerCase(java.util.Locale.ROOT));
            if (selected == null || (selected != DomainVerificationUserState.DOMAIN_STATE_SELECTED
                    && selected != DomainVerificationUserState.DOMAIN_STATE_VERIFIED)) return null;
            Intent intent = new Intent(Intent.ACTION_VIEW, url).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(INSTAGRAM);
            ResolveInfo target = activity.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (target == null || target.activityInfo == null || !target.activityInfo.exported
                    || !INSTAGRAM.equals(target.activityInfo.packageName)) return null;
            return intent;
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            return null;
        }
    }

    public static void routeSelectedLink(Activity activity, Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return;
        ComponentName selected = intent.getComponent();
        String launcher = "com.instagram.url.UrlHandlerLauncherActivity";
        if (selected != null) {
            if (!INSTAGRAM.equals(selected.getPackageName()) || !launcher.equals(selected.getClassName())) return;
        } else if (!INSTAGRAM.equals(intent.getPackage())) {
            return;
        }
        Uri link = instagramUrl(intent.getData());
        if (link == null) return;
        PackageManager pm = activity.getPackageManager();
        try {
            if (!"456.0.0.53.78".equals(pm.getPackageInfo(INSTAGRAM, 0).versionName)) return;
            ComponentName receiver = new ComponentName(INSTAGRAM, "com.instagram.url.UrlHandlerActivity");
            ActivityInfo handler = pm.getActivityInfo(receiver, 0);
            if (!handler.exported || handler.permission != null || !handler.applicationInfo.enabled) return;
            Intent direct = new Intent(intent).setData(link).setComponent(receiver);
            if (pm.resolveActivity(direct, 0) == null) return;
            intent.setData(link).setComponent(receiver);
        } catch (PackageManager.NameNotFoundException | RuntimeException unavailable) {
            // Keep Berry's selected handler if this Instagram build cannot accept a direct handoff.
        }
    }

    static Uri instagramUrl(Uri url) {
        if (url == null || !"https".equalsIgnoreCase(url.getScheme())) return null;
        String authority = url.getEncodedAuthority();
        if (googlePath(url, "/imgres")) {
            // imgurl is the thumbnail or CDN image. imgrefurl is the publishing page.
            String source = url.getQueryParameter("imgrefurl");
            if (source == null) return null;
            url = Uri.parse(source);
            authority = url.getEncodedAuthority();
        } else if (googlePath(url, "/url")) {
            String dest = url.getQueryParameter("url");
            if (dest == null) dest = url.getQueryParameter("q");
            if (dest == null) return null;
            url = Uri.parse(dest);
            authority = url.getEncodedAuthority();
        }
        if (!"https".equalsIgnoreCase(url.getScheme())
                || !("www.instagram.com".equalsIgnoreCase(authority) || "instagram.com".equalsIgnoreCase(authority))) return null;
        String path = url.getEncodedPath();
        if (path == null) return null;
        if (POST.matcher(path).matches()) return url;
        if (!PROFILE.matcher(path).matches()) return null;
        String username = url.getPathSegments().get(0);
        if (username.startsWith(".") || username.endsWith(".") || username.contains("..")
                || SITE_PATHS.contains(username.toLowerCase(Locale.ROOT))) return null;
        return url;
    }
}
