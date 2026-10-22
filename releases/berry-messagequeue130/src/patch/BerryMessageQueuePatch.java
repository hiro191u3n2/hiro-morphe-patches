package app.hiro.berryqueue.patches;

import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.patch.Patch;
import app.morphe.patcher.patch.ResourcePatch;
import app.morphe.patcher.patch.ResourcePatchContext;
import app.morphe.patcher.util.Document;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Select Android 17's OS queue at process creation; never replace a live queue. */
public final class BerryMessageQueuePatch {
    public static final String NAME = "ロックフリーMessageQueueを有効化";
    public static final String PACKAGE = "jp.ejimax.berrybrowser";
    public static final String PROVIDER = "app.hiro.berry.runtime.BerryQueueInit";
    public static final String AUTHORITY = PACKAGE + ".hiro.messagequeue";
    public static final String PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK";
    public static final String NS = "http://schemas.android.com/apk/res/android";

    private BerryMessageQueuePatch() {}
    private static Set<Pair<String, Set<String>>> compatibility() {
        return Collections.singleton(new Pair<String, Set<String>>(PACKAGE, null));
    }
    private static final BytecodePatch RUNTIME = new BytecodePatch(
        null, "Berry queue diagnostics and local network permission", false,
        compatibility(), Collections.emptySet(), Collections.emptySet(),
        new Supplier<InputStream>() {
            public InputStream get() {
                InputStream in = BerryMessageQueuePatch.class.getResourceAsStream("/extensions/berry_messagequeue.mpe");
                if (in == null) throw new IllegalStateException("Berry queue extension is missing");
                return in;
            }
        },
        new Function1<BytecodePatchContext, Unit>() {
            public Unit invoke(final BytecodePatchContext context) {
                if (!PACKAGE.equals(context.getPackageMetadata().getPackageName()))
                    throw new IllegalStateException("Berry Browser package required");
                // Pin original definitions into lazy bytecode writers without editing them.
                context.classDefForEach(new Function1<ClassDef, Unit>() {
                    public Unit invoke(ClassDef definition) {
                        context.mutableClassDefBy(definition);
                        return Unit.INSTANCE;
                    }
                });
                return Unit.INSTANCE;
            }
        }, null);

    public static final ResourcePatch PATCH = new ResourcePatch(
        NAME,
        "Android 17以降でOS標準のロックフリーMessageQueueを使用する対象SDK37以上へ設定。既に37以上なら維持。起動ログで実装を確認し、LAN権限を初回のみ要求。Android16以前への移植ではありません。実機での採用・高速化は未確認。",
        true, compatibility(), Collections.<Patch<?>>singleton(RUNTIME), Collections.emptySet(),
        new Function1<ResourcePatchContext, Unit>() {
            public Unit invoke(ResourcePatchContext context) {
                if (!PACKAGE.equals(context.getPackageMetadata().getPackageName()))
                    throw new IllegalStateException("Berry Browser package required");
                try (Document document = context.document("AndroidManifest.xml")) {
                    applyManifest(document);
                }
                return Unit.INSTANCE;
            }
        }, null);

    private static IllegalStateException invalid(String reason) {
        return new IllegalStateException("Unsupported Berry manifest: " + reason);
    }
    private static int number(String value, String label, int fallback) {
        if (value.isEmpty()) return fallback;
        if (!value.matches("[0-9]+")) throw invalid(label + " must be a release API number");
        try { return Integer.parseInt(value); }
        catch (NumberFormatException error) { throw invalid(label + " is out of range"); }
    }
    private static Element child(Element parent, String name) {
        Element found = null;
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (!(n instanceof Element) || !name.equals(n.getNodeName())) continue;
            if (found != null) throw invalid("duplicate " + name);
            found = (Element)n;
        }
        return found;
    }
    private static String attribute(Element e, String name) {
        String value = e.getAttributeNS(NS, name);
        return value.isEmpty() ? e.getAttribute("android:" + name) : value;
    }
    private static void set(Element e, String name, String value) {
        // Morphe Document may parse without namespace awareness. Remove both
        // representations so an old unnamespaced android:targetSdkVersion cannot
        // survive next to the new namespaced value during serialization.
        e.removeAttributeNS(NS, name);
        e.removeAttribute("android:" + name);
        e.setAttributeNS(NS, "android:" + name, value);
    }

    /** Validate the full mutation boundary first, then make narrowly scoped changes. */
    public static void applyManifest(org.w3c.dom.Document document) {
        Element manifest = document.getDocumentElement();
        if (!"manifest".equals(manifest.getNodeName()) || !PACKAGE.equals(manifest.getAttribute("package")))
            throw invalid("wrong package");
        if (!manifest.getAttribute("split").isEmpty()) throw invalid("base APK is required");
        Element application = child(manifest, "application"), sdk = child(manifest, "uses-sdk");
        if (application == null) throw invalid("missing application");
        int target = sdk == null ? 1 : number(attribute(sdk, "targetSdkVersion"), "targetSdkVersion",
                number(attribute(sdk, "minSdkVersion"), "minSdkVersion", 1));
        int maximum = sdk == null ? Integer.MAX_VALUE : number(attribute(sdk, "maxSdkVersion"), "maxSdkVersion", Integer.MAX_VALUE);
        if (maximum < 37) throw invalid("maxSdkVersion excludes Android 17");
        Element provider = null, permission = null;
        for (Node n = application.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (!(n instanceof Element) || !"provider".equals(n.getNodeName())) continue;
            Element e = (Element)n;
            if (PROVIDER.equals(attribute(e, "name"))) {
                if (provider != null || !AUTHORITY.equals(attribute(e, "authorities"))
                        || !"false".equals(attribute(e, "exported"))
                        || !attribute(e, "process").isEmpty()
                        || !(attribute(e, "enabled").isEmpty() || "true".equals(attribute(e, "enabled")))
                        || !"false".equals(attribute(e, "grantUriPermissions")))
                    throw invalid("conflicting queue provider");
                provider = e;
            } else {
                for (String authority : attribute(e, "authorities").split(";"))
                    if (AUTHORITY.equals(authority)) throw invalid("queue provider authority collision");
            }
        }
        for (Node n = manifest.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (!(n instanceof Element) || !("uses-permission".equals(n.getNodeName())
                    || "uses-permission-sdk-23".equals(n.getNodeName()))) continue;
            Element e = (Element)n;
            if (PERMISSION.equals(attribute(e, "name"))) {
                if (permission != null) throw invalid("duplicate local network permission");
                permission = e;
            }
        }
        if (sdk == null) {
            sdk = document.createElement("uses-sdk");
            manifest.insertBefore(sdk, manifest.getFirstChild());
        }
        if (target < 37 || attribute(sdk, "targetSdkVersion").isEmpty())
            set(sdk, "targetSdkVersion", Integer.toString(Math.max(37, target)));
        if (permission == null) {
            permission = document.createElement("uses-permission");
            set(permission, "name", PERMISSION);
            manifest.insertBefore(permission, application);
        }
        // This permission did not exist before API37; an older max would defeat the declaration.
        permission.removeAttributeNS(NS, "maxSdkVersion");
        permission.removeAttribute("android:maxSdkVersion");
        if (provider == null) {
            provider = document.createElement("provider");
            set(provider, "name", PROVIDER);
            set(provider, "authorities", AUTHORITY);
            set(provider, "exported", "false");
            set(provider, "grantUriPermissions", "false");
            application.appendChild(provider);
        }
    }
}
