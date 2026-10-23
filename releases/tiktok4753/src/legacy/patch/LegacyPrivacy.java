/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only
 * Preserves the reviewed privacy policy from Hiro bundle 1.0.279.
 * Reimplemented for the actual TikTok 47.5.3 original APK and Morphe's
 * on-demand raw resource extraction. This is a finite static endpoint policy,
 * not a network firewall or a claim that every future tracking host is covered.
 */
package app.hiro.tiktok4753.patches;

import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import app.morphe.patcher.util.proxy.mutableTypes.MutableField;
import app.morphe.patcher.util.proxy.mutableTypes.encodedValue.MutableStringEncodedValue;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.Field;
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Static manifest, bytecode-constant and native-library privacy policy. */
public final class LegacyPrivacy {
    private LegacyPrivacy() {}
    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static final String BLOCKED_HOST = "blocked.invalid";
    private static final long MAX_NATIVE_LIBRARY_SCAN_BYTES = 134217728L;
    private static final Set<String> TRACKING_PERMISSIONS = immutableSet("android.permission.ACCESS_ADSERVICES_AD_ID", "android.permission.ACCESS_ADSERVICES_ATTRIBUTION", "android.permission.ACCESS_ADSERVICES_TOPICS", "android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE", "android.permission.ACCESS_ADSERVICES_PROTECTED_SIGNALS", "android.permission.ACCESS_LOCAL_NETWORK", "android.permission.QUERY_ALL_PACKAGES", "android.permission.PACKAGE_USAGE_STATS", "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE", "com.google.android.gms.permission.AD_ID", "com.huawei.appmarket.service.commondata.permission.GET_COMMON_DATA", "com.orange.update.permission.READ_ATTRIBUTION", "com.samsung.android.mapsagent.permission.READ_APP_INFO", "com.tiktok.preload.permission.IDENTIFY");
    private static final Set<String> TRACKING_DECLARED_PERMISSIONS = immutableSet("com.zhiliaoapp.musically.permission.ATTRIBUTION_INFO");
    private static final Set<String> TRACKING_COMPONENTS = immutableSet("com.appsflyer.MultipleInstallBroadcastReceiver", "com.appsflyer.SingleInstallBroadcastReceiver", "com.bytedance.applog.collector.Collector", "com.ss.android.common.applog.HotsoonReceiver", "com.ss.android.ugc.aweme.crash.cp.ExportedShellProvider", "com.google.android.gms.ads.MobileAdsInitProvider", "com.google.android.gms.measurement.AppMeasurementReceiver", "com.google.android.gms.measurement.AppMeasurementService", "com.google.android.gms.measurement.AppMeasurementJobService", "com.google.firebase.crashlytics.startup.CrashlyticsInitProvider", "com.google.firebase.perf.provider.FirebasePerfProvider");
    private static final List<String> TRACKING_COMPONENT_PREFIXES = Collections.unmodifiableList(Arrays.asList("com.appsflyer.", "com.bytedance.applog.", "com.bytedance.apm.", "com.bytedance.crash.", "com.bytedance.monitor.", "com.bytedance.bdtracker.", "com.bytedance.sdk.openadsdk.", "com.google.android.gms.ads.", "com.google.android.gms.measurement.", "com.google.firebase.crashlytics.", "com.google.firebase.perf.", "com.ss.android.common.applog.", "com.ss.android.ugc.aweme.crash."));
    private static final Set<String> TRACKING_METADATA = immutableSet("android.adservices.AD_SERVICES_CONFIG", "com.google.android.gms.ads.APPLICATION_ID", "com.google.firebase.components:com.google.firebase.analytics.connector.internal.AnalyticsConnectorRegistrar", "com.google.firebase.components:com.google.firebase.crashlytics.CrashlyticsRegistrar", "com.google.firebase.components:com.google.firebase.perf.FirebasePerfRegistrar", "com.google.firebase.components:com.google.firebase.sessions.FirebaseSessionsRegistrar");
    private static final List<String> TRACKING_METADATA_FRAGMENTS = Collections.unmodifiableList(Arrays.asList("appsflyer", "firebase.analytics.connector", "firebase.crashlytics", "firebase.perf", "firebase.sessions", "google.android.gms.ads", "google.android.gms.measurement", "bytedance.applog", "bytedance.apm", "bdtracker", "openadsdk", "pangle", "pangolin", "slardar", "npth"));
    private static final Set<String> TRACKING_QUERY_ACTIONS = immutableSet("com.appsflyer.referrer.INSTALL_PROVIDER", "com.android.vending.INSTALL_REFERRER", "com.google.android.finsky.BIND_GET_INSTALL_REFERRER_SERVICE");
    private static final Set<String> THIRD_PARTY_TRACKER_SUFFIXES = immutableSet("appsflyer.com", "appsflyersdk.com", "appsflyer.tk", "adjust.com", "adjust.world", "branch.io", "app-measurement.com", "google-analytics.com", "googletagmanager.com", "googleadservices.com", "doubleclick.net", "googlesyndication.com", "crashlytics.com", "sentry.io", "bugsnag.com", "amplitude.com", "mixpanel.com", "segment.io", "scorecardresearch.com", "apmplus.volces.com", "volceapplog.com", "pangle-b.io", "pangolin-sdk-toutiao.com", "pangolin-sdk-toutiao-b.com", "pangolin-sdk-toutiao1.com", "pglstatp-toutiao.com");
    private static final Set<String> EXACT_ALLOWED_HOSTS = immutableSet("log-boot.tiktokv.com");
    private static final Set<String> EXACT_BLOCKED_HOSTS = immutableSet("sf16-telemetry-proxy-oci.tiktokcdn-us.com");
    private static final Set<String> BYTEDANCE_TRACKING_SUFFIXES = immutableSet("tiktokv.com", "tiktokv.us", "tiktok.com", "tiktokw.us", "byteoversea.com", "bytedance.net");
    private static final Set<String> BYTE_AD_CDN_SUFFIXES = immutableSet("ibyteimg.com", "ibytedtos.com", "snssdk.com");
    private static final Set<String> BYTE_TRACKER_SERVICE_SUFFIXES = immutableSet("isnssdk.com", "sgsnssdk.com", "tobsnssdk.com", "ctobsnssdk.com");
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("(?i)(?<![a-z0-9_-])(?:[a-z0-9-]+\\.)+[a-z]{2,63}(?![a-z0-9_-])");
    private static final List<String> PRIVACY_METADATA_FLAGS = Collections.unmodifiableList(Arrays.asList(
        "firebase_analytics_collection_enabled", "firebase_crashlytics_collection_enabled",
        "firebase_performance_collection_enabled", "firebase_sessions_enabled",
        "google_analytics_adid_collection_enabled", "google_analytics_automatic_screen_reporting_enabled",
        "google_analytics_default_allow_ad_storage", "google_analytics_default_allow_ad_user_data",
        "google_analytics_default_allow_analytics_storage", "google_analytics_default_allow_ad_personalization_signals",
        "com.facebook.sdk.AutoLogAppEventsEnabled", "com.facebook.sdk.AdvertiserIDCollectionEnabled"));
    private static Set<Pair<String, Set<String>>> compatibility() {
        return Collections.singleton(new Pair<String, Set<String>>("com.zhiliaoapp.musically", Collections.singleton("47.5.3")));
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("TikTok 47.5.3 privacy: " + message);
    }
    private static void validate(String packageName, String versionName) {
        require("com.zhiliaoapp.musically".equals(packageName) && "47.5.3".equals(versionName),
            "requires the original global TikTok 47.5.3");
    }
    // Finalizers run after extension insertion, so final bytecode is covered as well.
    private static final BytecodePatch BYTECODE = new BytecodePatch(null, null, true, compatibility(),
        Collections.emptySet(), Collections.emptySet(), null,
        new Function1<BytecodePatchContext, Unit>() { public Unit invoke(BytecodePatchContext c) {
            validate(c.getPackageMetadata().getPackageName(), c.getPackageMetadata().getVersionName()); return Unit.INSTANCE;
        }}, new Function1<BytecodePatchContext, Unit>() { public Unit invoke(BytecodePatchContext c) {
            blockDirectTrackingEndpoints(c); return Unit.INSTANCE;
        }});
    public static final ResourcePatch PATCH = new ResourcePatch("TikTok 怪しい通信だけ遮断（TikTok 47.5.3）",
        "47.5.3の広告・追跡・解析・クラッシュ計測用の権限とSDK部品を除去し、DEXと実際のネイティブライブラリ内の既知の通信先を遮断。通常の動画・ログイン・DM・通知・LIVE用の通信先と起動用ホストを保持します。",
        true, compatibility(), Collections.<Patch<?>>singleton(BYTECODE), Collections.emptySet(),
        new Function1<ResourcePatchContext, Unit>() { public Unit invoke(ResourcePatchContext c) {
            validate(c.getPackageMetadata().getPackageName(), c.getPackageMetadata().getVersionName()); return Unit.INSTANCE;
        }}, new Function1<ResourcePatchContext, Unit>() { public Unit invoke(ResourcePatchContext c) {
            ManifestCounts counts;
            try (app.morphe.patcher.util.Document d = c.document("AndroidManifest.xml")) {
                counts = hardenManifest(d);
            }
            require(counts.removed > 0, "manifest policy matched no original tracking entries");
            System.out.println("HIRO4753 PRIVACY_MANIFEST removed=" + counts.removed + " flags=" + counts.flags);
            hardenNativeLibraries(c);
            return Unit.INSTANCE;
        }});

    public static boolean isBlockedHost(String host) {
        String value = host.toLowerCase(Locale.ROOT);
        if (value.endsWith(".")) value = value.substring(0, value.length() - 1);
        if (EXACT_ALLOWED_HOSTS.contains(value)) return false;
        if (EXACT_BLOCKED_HOSTS.contains(value) || hasDomainSuffix(value, THIRD_PARTY_TRACKER_SUFFIXES)) return true;
        int dot = value.indexOf('.');
        if (dot < 0) return false;
        String first = value.substring(0, dot);
        if (hasDomainSuffix(value, BYTEDANCE_TRACKING_SUFFIXES)) return isTrackingLabel(first);
        if (hasDomainSuffix(value, BYTE_AD_CDN_SUFFIXES)) return value.equals("pangolin.snssdk.com") || isTrackingLabel(first);
        return hasDomainSuffix(value, BYTE_TRACKER_SERVICE_SUFFIXES) && isTrackingLabel(first);
    }
    private static boolean isTrackingLabel(String s) {
        for (String name : Arrays.asList("ad", "ads", "analytics", "tracking", "log", "xlog", "rtlog", "rtc-logger",
                "mon", "mcs", "tnc", "mssdk", "crash", "telemetry", "gplog", "gpm-mon", "ug-sg", "toblog"))
            if (s.equals(name) || s.startsWith(name + "-") ||
                s.length() > name.length() && s.startsWith(name) && Character.isDigit(s.charAt(name.length()))) return true;
        return s.contains("applog") || s.contains("-ad-") || s.contains("-log-") || s.endsWith("-log");
    }
    private static boolean hasDomainSuffix(String value, Set<String> suffixes) {
        for (String suffix : suffixes) if (value.equals(suffix) || value.endsWith("." + suffix)) return true;
        return false;
    }
    public static String rewriteTrackingHosts(String value) {
        if (value.indexOf('.') < 0) return value;
        Matcher matcher = DOMAIN_PATTERN.matcher(value);
        StringBuffer out = null;
        while (matcher.find()) if (isBlockedHost(matcher.group())) {
            if (out == null) out = new StringBuffer(value.length());
            matcher.appendReplacement(out, Matcher.quoteReplacement(BLOCKED_HOST));
        }
        if (out == null) return value;
        matcher.appendTail(out);
        return out.toString();
    }
    private static final class Site {
        final ClassDef owner; final Method method; final Map<Integer, String> replacements;
        Site(ClassDef owner, Method method, Map<Integer, String> replacements) {
            this.owner = owner; this.method = method; this.replacements = replacements;
        }
    }
    private static final class FieldSite {
        final ClassDef owner; final Field field;
        FieldSite(ClassDef owner, Field field) { this.owner = owner; this.field = field; }
    }
    /** Preserve field flags, annotations and type while changing only the encoded string value. */
    public static boolean rewriteStaticField(MutableField field) {
        if ((field.getAccessFlags() & 8) == 0 || !(field.getInitialValue() instanceof StringEncodedValue)) return false;
        String original = ((StringEncodedValue)field.getInitialValue()).getValue();
        String replacement = rewriteTrackingHosts(original);
        if (replacement.equals(original)) return false;
        require(field.getInitialValue() instanceof MutableStringEncodedValue, "unexpected mutable string field representation " + field);
        ((MutableStringEncodedValue)field.getInitialValue()).setValue(replacement);
        return true;
    }
    public static void blockDirectTrackingEndpoints(final BytecodePatchContext c) {
        final List<Site> sites = new ArrayList<Site>();
        final List<FieldSite> fields = new ArrayList<FieldSite>();
        c.classDefForEach(new Function1<ClassDef, Unit>() { public Unit invoke(ClassDef owner) {
            for (Field field : owner.getStaticFields()) if (field.getInitialValue() instanceof StringEncodedValue) {
                String value = ((StringEncodedValue)field.getInitialValue()).getValue();
                if (!value.equals(rewriteTrackingHosts(value))) fields.add(new FieldSite(owner, field));
            }
            for (Method method : owner.getMethods()) {
                if (method.getImplementation() == null) continue;
                Map<Integer, String> replacements = new LinkedHashMap<Integer, String>();
                int index = 0;
                for (Instruction insn : method.getImplementation().getInstructions()) {
                    if ((insn.getOpcode() == Opcode.CONST_STRING || insn.getOpcode() == Opcode.CONST_STRING_JUMBO) &&
                            insn instanceof ReferenceInstruction && ((ReferenceInstruction)insn).getReference() instanceof StringReference) {
                        String value = ((StringReference)((ReferenceInstruction)insn).getReference()).getString();
                        String rewritten = rewriteTrackingHosts(value);
                        if (!value.equals(rewritten)) replacements.put(index, rewritten);
                    }
                    index++;
                }
                if (!replacements.isEmpty()) sites.add(new Site(owner, method, replacements));
            }
            return Unit.INSTANCE;
        }});
        require(!sites.isEmpty(), "no original bytecode tracking endpoints matched");
        int changed = 0;
        List<String> evidence = new ArrayList<String>();
        for (Site site : sites) {
            MutableMethod target = null;
            for (MutableMethod method : c.mutableClassDefBy(site.owner).getMethods()) {
                if (method.getName().equals(site.method.getName()) && method.getReturnType().equals(site.method.getReturnType()) &&
                    parameterTypes(method).equals(parameterTypes(site.method))) {
                    require(target == null, "ambiguous mutable method " + site.method); target = method;
                }
            }
            require(target != null && target.getImplementation() != null, "missing mutable method " + site.method);
            for (Map.Entry<Integer, String> replacement : site.replacements.entrySet()) {
                Instruction old = target.getImplementation().getInstructions().get(replacement.getKey());
                require(old instanceof OneRegisterInstruction, "changed constant register contract");
                int reg = ((OneRegisterInstruction)old).getRegisterA();
                ImmutableStringReference string = new ImmutableStringReference(replacement.getValue());
                if (old.getOpcode() == Opcode.CONST_STRING_JUMBO)
                    target.getImplementation().replaceInstruction(replacement.getKey(), new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO, reg, string));
                else {
                    require(old.getOpcode() == Opcode.CONST_STRING, "changed constant opcode contract");
                    target.getImplementation().replaceInstruction(replacement.getKey(), new BuilderInstruction21c(Opcode.CONST_STRING, reg, string));
                }
                evidence.add(site.owner.getType() + "->" + site.method.getName() + parameterTypes(site.method) + site.method.getReturnType() + "#" + replacement.getKey());
                changed++;
            }
        }
        require(!fields.isEmpty(), "no original static-field tracking endpoints matched");
        for (FieldSite site : fields) {
            MutableField target = null;
            for (MutableField field : c.mutableClassDefBy(site.owner).getStaticFields())
                if (field.getName().equals(site.field.getName()) && field.getType().equals(site.field.getType())) {
                    require(target == null, "ambiguous mutable field " + site.field); target = field;
                }
            require(target != null && rewriteStaticField(target), "missing static-field tracking rewrite " + site.field);
            System.out.println("HIRO4753 PRIVACY_FIELD " + site.owner.getType() + "->" + site.field.getName() + ":" + site.field.getType());
        }
        Collections.sort(evidence);
        System.out.println("HIRO4753 PRIVACY_DEX methods=" + sites.size() + " constants=" + changed + " staticFields=" + fields.size() +
            " sitesSHA256=" + sha256(joinLines(evidence).getBytes(StandardCharsets.UTF_8)));
    }
    private static List<String> parameterTypes(Method method) {
        List<String> p = new ArrayList<String>(); for (CharSequence value : method.getParameterTypes()) p.add(value.toString()); return p;
    }
    private static String joinLines(List<String> lines) {
        StringBuilder s = new StringBuilder(); for (String line : lines) s.append(line).append('\n'); return s.toString();
    }

    /** Change printable host bytes without moving ELF offsets or touching any other byte. */
    public static int rewriteNativeBytes(byte[] bytes) {
        int at = 0, changed = 0;
        while (at < bytes.length) {
            while (at < bytes.length && !isPrintableAscii(bytes[at])) at++;
            int start = at; boolean hasDot = false;
            while (at < bytes.length && isPrintableAscii(bytes[at])) { if (bytes[at] == '.') hasDot = true; at++; }
            int length = at - start;
            if (length < 9 || !hasDot) continue;
            Matcher matcher = DOMAIN_PATTERN.matcher(new String(bytes, start, length, StandardCharsets.ISO_8859_1));
            while (matcher.find()) if (isBlockedHost(matcher.group())) {
                int count = matcher.end() - matcher.start();
                require(count >= 9, "native hostname too short for equal-length .invalid replacement");
                int destination = start + matcher.start();
                Arrays.fill(bytes, destination, destination + count - 8, (byte)'x');
                byte[] suffix = ".invalid".getBytes(StandardCharsets.ISO_8859_1);
                System.arraycopy(suffix, 0, bytes, destination + count - 8, suffix.length);
                changed++;
            }
        }
        return changed;
    }
    private static boolean isPrintableAscii(byte b) { int value = b & 255; return value >= 32 && value <= 126; }
    public static void hardenNativeLibraries(ResourcePatchContext c) {
        List<String> paths = new ArrayList<String>();
        for (String path : c.listApkEntries("lib/")) if (path.endsWith(".so")) paths.add(path);
        Collections.sort(paths);
        require(!paths.isEmpty(), "original APK contains no native libraries");
        int changedFiles = 0, changedHosts = 0, embeddedConstants = 0, embeddedFields = 0, embeddedDex = 0;
        Map<String,Integer> formats = new TreeMap<String,Integer>();
        long scannedBytes = 0;
        try {
            for (String path : paths) {
                // false only looks in the workspace with on-demand extraction. Enumerating
                // original APK entries and requesting true guarantees each SO is inspected.
                File file = c.get(path, true);
                require(file != null && file.isFile(), "failed to materialize original native entry " + path);
                require(file.length() > 0 && file.length() <= MAX_NATIVE_LIBRARY_SCAN_BYTES,
                    "native entry outside reviewed scan bounds " + path + " bytes=" + file.length());
                byte[] bytes = Files.readAllBytes(file.toPath());
                scannedBytes += bytes.length;
                String before = sha256(bytes);
                PrivacyContainers.Result result = PrivacyContainers.rewrite(bytes);
                formats.put(result.format, formats.containsKey(result.format) ? formats.get(result.format)+1 : 1);
                embeddedDex += result.dexEntries;
                if (result.changes() == 0) continue;
                String after = sha256(result.bytes);
                require(!before.equals(after), "native replacement made no change " + path);
                Files.write(file.toPath(), result.bytes);
                changedFiles++; changedHosts += result.hosts; embeddedConstants += result.dexConstants; embeddedFields += result.dexFields;
                System.out.println("HIRO4753 PRIVACY_NATIVE path=" + path + " format=" + result.format + " hosts=" + result.hosts +
                    " dexConstants=" + result.dexConstants + " dexFields=" + result.dexFields + " beforeBytes=" + bytes.length + " afterBytes=" + result.bytes.length +
                    " beforeSHA256=" + before + " afterSHA256=" + after);
            }
        } catch (IOException failure) { throw new IllegalStateException("Could not rewrite original native tracking endpoints", failure); }
        require(changedFiles > 0 && changedHosts > 0, "no original native tracking endpoints matched");
        System.out.println("HIRO4753 PRIVACY_NATIVE_TOTAL scanned=" + paths.size() + " scannedBytes=" + scannedBytes +
            " changedFiles=" + changedFiles + " elfHosts=" + changedHosts + " embeddedConstants=" + embeddedConstants +
            " embeddedFields=" + embeddedFields + " embeddedDex=" + embeddedDex + " formats=" + formats);
    }
    public static String sha256(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes); StringBuilder out = new StringBuilder(64);
            for (byte b : hash) out.append(String.format(Locale.ROOT, "%02x", b & 255)); return out.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    public static final class ManifestCounts {
        public int removed, flags;
    }
    public static ManifestCounts hardenManifest(Document document) {
        Element root = document.getDocumentElement();
        require(root != null && "manifest".equals(root.getTagName()) && "com.zhiliaoapp.musically".equals(root.getAttribute("package")),
            "manifest is not global TikTok");
        ManifestCounts count = new ManifestCounts();
        for (String tag : Arrays.asList("uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m"))
            for (Element entry : children(root, tag)) if (TRACKING_PERMISSIONS.contains(androidAttribute(entry, "name"))) remove(entry, count);
        for (Element entry : children(root, "permission")) if (TRACKING_DECLARED_PERMISSIONS.contains(androidAttribute(entry, "name"))) remove(entry, count);
        for (Element queries : children(root, "queries")) for (Element intent : children(queries, "intent"))
            if (containsTrackingAction(intent)) remove(intent, count);
        List<Element> applications = children(root, "application");
        require(applications.size() == 1, "expected one application");
        Element application = applications.get(0);
        for (Element entry : children(application, null)) {
            String tag = entry.getTagName(), name = androidAttribute(entry, "name");
            boolean component = "receiver".equals(tag) || "service".equals(tag) || "provider".equals(tag);
            if (component && (isTrackingComponent(name) || containsTrackingAction(entry)) ||
                ("meta-data".equals(tag) || "property".equals(tag)) && isTrackingMetadata(name)) remove(entry, count);
            else if (component || "activity".equals(tag) || "activity-alias".equals(tag))
                for (Element nested : children(entry, "meta-data")) if (isTrackingMetadata(androidAttribute(nested, "name"))) remove(nested, count);
        }
        for (String flag : PRIVACY_METADATA_FLAGS) {
            Element target = null;
            for (Element entry : children(application, "meta-data")) if (flag.equals(androidAttribute(entry, "name"))) {
                // Android metadata keys must be unique; normalize duplicates deterministically.
                if (target == null) target = entry; else remove(entry, count);
            }
            if (target == null) {
                target = document.createElement("meta-data"); setAndroidAttribute(target, "name", flag); application.appendChild(target);
            }
            target.removeAttributeNS(ANDROID_NAMESPACE, "resource"); target.removeAttribute("android:resource");
            setAndroidAttribute(target, "value", "false"); count.flags++;
        }
        require(count.flags == 12, "privacy metadata contract changed");
        return count;
    }
    private static boolean isTrackingComponent(String name) {
        if (TRACKING_COMPONENTS.contains(name)) return true;
        for (String prefix : TRACKING_COMPONENT_PREFIXES) if (name.startsWith(prefix)) return true;
        return false;
    }
    private static boolean isTrackingMetadata(String name) {
        if (TRACKING_METADATA.contains(name)) return true;
        String lower = name.toLowerCase(Locale.ROOT);
        for (String fragment : TRACKING_METADATA_FRAGMENTS) if (lower.contains(fragment)) return true;
        return false;
    }
    private static boolean containsTrackingAction(Element node) {
        for (Element filter : children(node, "intent-filter")) for (Element action : children(filter, "action"))
            if (TRACKING_QUERY_ACTIONS.contains(androidAttribute(action, "name"))) return true;
        for (Element action : children(node, "action")) if (TRACKING_QUERY_ACTIONS.contains(androidAttribute(action, "name"))) return true;
        return false;
    }
    private static List<Element> children(Element parent, String tag) {
        List<Element> result = new ArrayList<Element>(); NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) if (nodes.item(i) instanceof Element) {
            Element child = (Element)nodes.item(i); if (tag == null || tag.equals(child.getTagName())) result.add(child);
        }
        return result;
    }
    public static String androidAttribute(Element node, String name) {
        String value = node.getAttributeNS(ANDROID_NAMESPACE, name); return value.isEmpty() ? node.getAttribute("android:" + name) : value;
    }
    private static void setAndroidAttribute(Element node, String name, String value) {
        // Morphe's parser is namespace-unaware. Remove both forms to prevent an
        // existing true value from surviving beside a new namespaced false value.
        node.removeAttributeNS(ANDROID_NAMESPACE, name); node.removeAttribute("android:" + name);
        node.setAttributeNS(ANDROID_NAMESPACE, "android:" + name, value);
    }
    private static void remove(Node node, ManifestCounts count) { node.getParentNode().removeChild(node); count.removed++; }
    private static Set<String> immutableSet(String... values) {
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(values)));
    }
}
