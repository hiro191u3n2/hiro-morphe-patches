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
import android.webkit.WebResourceRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
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

    public static Intent selectedIntent(Activity activity, WebResourceRequest request) {
        if (Build.VERSION.SDK_INT < 31 || !request.isForMainFrame() || !request.hasGesture()
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
        if (("www.google.co.jp".equalsIgnoreCase(authority) || "www.google.com".equalsIgnoreCase(authority))
                && "/url".equals(url.getEncodedPath())) {
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
