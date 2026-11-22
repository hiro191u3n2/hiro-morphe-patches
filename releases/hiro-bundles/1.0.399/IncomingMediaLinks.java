package app.hiro.instagram.links.patches;

import app.morphe.patcher.patch.AppTarget;
import app.morphe.patcher.patch.Compatibility;
import app.morphe.patcher.patch.FormFactor;
import app.morphe.patcher.patch.PatchKt;
import app.morphe.patcher.patch.ResourcePatch;
import app.morphe.patcher.util.Document;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import kotlin.Unit;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class IncomingMediaLinks {
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static final String RECEIVER = "com.instagram.url.UrlHandlerActivity";
    private static final String LAUNCHER = "com.instagram.url.UrlHandlerLauncherActivity";
    private static final String VERSION = "456.0.0.53.78";
    private static final Compatibility TARGET = new Compatibility("com.instagram.android", "Instagram",
            "Instagram " + VERSION, null, "#C13584", null, Collections.singletonList(
            new AppTarget(VERSION, Collections.emptyMap(), false, null, null)), FormFactor.MOBILE);

    public static final ResourcePatch PATCH = PatchKt.resourcePatch("外部アプリから投稿・リールを開く",
            "Googleなどの外部アプリから開いた投稿・リールのHTTPSリンクをInstagramの投稿リンク受信画面で開きます。",
            true, builder -> {
                builder.compatibleWith(TARGET);
                builder.execute(context -> {
                    try (Document manifest = context.document("AndroidManifest.xml")) {
                        Element receiver = findActivity(manifest, RECEIVER);
                        Element launcher = findActivity(manifest, LAUNCHER);
                        if (!"true".equals(receiver.getAttributeNS(ANDROID, "exported"))
                                || receiver.hasAttributeNS(ANDROID, "permission")
                                || launcher.getElementsByTagName("intent-filter").getLength() == 0) {
                            throw new IllegalStateException("Instagramの公開リンク受信画面が想定と異なります");
                        }
                        if (hasMediaFilter(receiver)) return Unit.INSTANCE;
                        splitWebFilter(manifest, launcher);
                        Element filter = manifest.createElement("intent-filter");
                        filter.setAttributeNS(ANDROID, "android:autoVerify", "true");
                        append(manifest, filter, "action", "name", "android.intent.action.VIEW");
                        append(manifest, filter, "category", "name", "android.intent.category.DEFAULT");
                        append(manifest, filter, "category", "name", "android.intent.category.BROWSABLE");
                        append(manifest, filter, "data", "scheme", "https");
                        append(manifest, filter, "data", "host", "www.instagram.com");
                        append(manifest, filter, "data", "host", "instagram.com");
                        // Android 14 and earlier ignore relative filter groups. An https URL
                        // with an authority cannot have this rootless path.
                        append(manifest, filter, "data", "path", "unreachable-relative-path");
                        relativePath(manifest, filter, true, "pathPrefix", "/p/");
                        relativePath(manifest, filter, true, "pathPrefix", "/reel/");
                        receiver.appendChild(filter);
                    }
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });

    private static Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private static boolean hasMediaFilter(Element receiver) {
        NodeList filters = receiver.getElementsByTagName("intent-filter");
        for (int i = 0; i < filters.getLength(); i++) {
            Element filter = (Element) filters.item(i);
            if (values(filter, "pathPrefix").equals(set("/p/", "/reel/"))
                    && values(filter, "host").equals(set("www.instagram.com", "instagram.com"))
                    && values(filter, "scheme").equals(set("https"))) return true;
        }
        return false;
    }

    private static Set<String> values(Element filter, String attribute) {
        Set<String> result = new HashSet<>();
        NodeList entries = filter.getElementsByTagName("data");
        for (int i = 0; i < entries.getLength(); i++) {
            Element data = (Element) entries.item(i);
            if (data.hasAttributeNS(ANDROID, attribute)) result.add(data.getAttributeNS(ANDROID, attribute));
        }
        return result;
    }

    private static void splitWebFilter(Document manifest, Element launcher) {
        Set<String> sites = set("www.instagram.com", "instagram.com");
        Set<String> other = set("applink.instagram.com", "familycenter.instagram.com", "aistudio.instagram.com");
        Set<String> hosts = new HashSet<>(sites);
        hosts.addAll(other);
        NodeList filters = launcher.getElementsByTagName("intent-filter");
        for (int i = 0; i < filters.getLength(); i++) {
            Element original = (Element) filters.item(i);
            if (!values(original, "scheme").equals(set("http", "https"))
                    || !values(original, "host").equals(hosts)
                    || original.getElementsByTagName("data").getLength() != 7) continue;
            insertWebFilter(manifest, original, "http", hosts, false);
            insertWebFilter(manifest, original, "https", other, false);
            insertWebFilter(manifest, original, "https", sites, true);
            launcher.removeChild(original);
            return;
        }
        throw new IllegalStateException("Instagramの既存のウェブリンク設定が想定と異なります");
    }

    private static void insertWebFilter(Document manifest, Element original, String scheme, Set<String> hosts, boolean fallback) {
        Element copy = (Element) original.cloneNode(true);
        NodeList dataEntries = copy.getElementsByTagName("data");
        for (int i = dataEntries.getLength() - 1; i >= 0; i--) {
            Element data = (Element) dataEntries.item(i);
            String protocol = data.getAttributeNS(ANDROID, "scheme");
            String host = data.getAttributeNS(ANDROID, "host");
            if ((!protocol.isEmpty() && !protocol.equals(scheme)) || (!host.isEmpty() && !hosts.contains(host))) {
                copy.removeChild(data);
            }
        }
        if (fallback) {
            relativePath(manifest, copy, false, "pathPrefix", "/p/");
            relativePath(manifest, copy, false, "pathPrefix", "/reel/");
            relativePath(manifest, copy, true, "pathPattern", ".*");
        }
        original.getParentNode().insertBefore(copy, original);
    }

    private static void relativePath(Document manifest, Element filter, boolean allow, String key, String value) {
        Element group = manifest.createElement("uri-relative-filter-group");
        group.setAttributeNS(ANDROID, "android:allow", Boolean.toString(allow));
        append(manifest, group, "data", key, value);
        filter.appendChild(group);
    }

    private static Element findActivity(Document manifest, String name) {
        NodeList activities = manifest.getElementsByTagName("activity");
        for (int i = 0; i < activities.getLength(); i++) {
            Element activity = (Element) activities.item(i);
            if (name.equals(activity.getAttributeNS(ANDROID, "name"))) return activity;
        }
        throw new IllegalStateException("Instagramのリンク画面が見つかりません: " + name);
    }

    private static void append(Document manifest, Element filter, String tag, String key, String value) {
        Element element = manifest.createElement(tag);
        element.setAttributeNS(ANDROID, "android:" + key, value);
        filter.appendChild(element);
    }

    private IncomingMediaLinks() {}
}
