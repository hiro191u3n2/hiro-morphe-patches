/* SPDX-License-Identifier: GPL-3.0-only */
package app.hiro.tiktoklink.runtime;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlainVideoLink {
    private static final Pattern VIDEO = Pattern.compile(
        "(?i:https://(?:www\\.|us\\.)?tiktok\\.com)/@[A-Za-z0-9._]+/video/([1-9][0-9]{0,19})/?");
    private static final String SHARE = "?utm_campaign=client_share";

    private PlainVideoLink() {}

    static String plainVideoId(String url) {
        if (url == null || url.length() > 512) return null;
        Matcher video = VIDEO.matcher(url);
        return video.matches() ? video.group(1) : null;
    }

    public static void onCreate(Activity activity) {
        onNewIntent(activity.getIntent());
    }

    public static void onNewIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return;
        String url = intent.getDataString();
        if (plainVideoId(url) != null) {
            intent.setDataAndType(Uri.parse(url + SHARE), intent.getType());
        }
    }

    // Some resolver paths carry the original web URL inside params_url.
    // Confirm that it names the same post before accepting a markerless route.
    public static boolean matchesOriginal(Uri resolved) {
        String id = plainVideoId(resolved.getQueryParameter("params_url"));
        if (id == null || resolved.getQueryParameter("utm_campaign") != null) return false;
        if ("aweme".equals(resolved.getHost())) {
            return id.equals(resolved.getLastPathSegment());
        }
        return id.equals(resolved.getQueryParameter("id"));
    }
}
