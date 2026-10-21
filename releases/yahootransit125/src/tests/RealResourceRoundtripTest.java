package app.hiro.yahootransit.patches;

import app.morphe.engine.patches.PatchBundleLoader;
import app.morphe.patcher.Patcher;
import app.morphe.patcher.PatcherConfig;
import app.morphe.patcher.PatcherResult;
import app.morphe.patcher.dex.BytecodeMode;
import app.morphe.patcher.dex.DexVerifier;
import app.morphe.patcher.dex.NoOpDexVerifier;
import app.morphe.patcher.patch.Patch;
import app.morphe.patcher.patch.ResourcePatch;
import app.morphe.patcher.patch.ResourcePatchContext;
import app.morphe.patcher.resource.ResourceMode;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/** Local opt-in test. Requires a privately supplied archived APK; never bundles app resources.
 * The available 8.4.1/8.5.0 archives are previously modified and lack original DEX. This verifies
 * real resource transformations and serialization, not installation or Android runtime behavior.
 */
public final class RealResourceRoundtripTest {
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static final String AD_MARKER = "hiro_ad_slot_hidden_v2";
    private static final String NEARME_MARKER = "hiro_nearme_pr_hidden_v1";
    private static final Pattern DIMENSION = Pattern.compile("(-?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+))(dp|dip|sp|px|pt|in|mm)");
    private static final Set<String> FLOAT_ATTRIBUTES = Set.of("alpha", "layout_weight", "weightSum", "scaleX", "scaleY", "rotation", "rotationX", "rotationY");
    private static int assertions;

    private static void check(boolean condition, String reason) {
        assertions++;
        if (!condition) throw new AssertionError(reason);
    }
    private static String sha(byte[] bytes) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private static String sha(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] bytes = new byte[65536];
            for (int n; (n = input.read(bytes)) >= 0;) digest.update(bytes, 0, n);
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }
    private static String sha(String text) throws Exception { return sha(text.getBytes(StandardCharsets.UTF_8)); }
    private static String attr(Element element, String name) { return element.getAttributeNS(ANDROID, name); }
    private static boolean shield(Element element) {
        String tag = attr(element, "tag");
        return AD_MARKER.equals(tag) || NEARME_MARKER.equals(tag);
    }
    private static boolean behindShield(Element element, String required) {
        for (Node node = element.getParentNode(); node instanceof Element; node = node.getParentNode()) {
            Element ancestor = (Element)node;
            if (required == null ? shield(ancestor) : required.equals(attr(ancestor, "tag"))) return true;
        }
        return false;
    }
    private static org.w3c.dom.Document read(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try (InputStream input = Files.newInputStream(file)) { return factory.newDocumentBuilder().parse(input); }
    }
    private static String expanded(Node node) {
        return node.getNamespaceURI() == null ? node.getNodeName() : "{" + node.getNamespaceURI() + "}" + node.getLocalName();
    }
    private static String value(Node attribute) {
        String value = attribute.getNodeValue();
        if (!ANDROID.equals(attribute.getNamespaceURI())) return value;
        String name = attribute.getLocalName();
        if ((name.equals("layout_width") || name.equals("layout_height")) && value.equals("fill_parent")) return "match_parent";
        Matcher match = DIMENSION.matcher(value);
        if (match.matches()) return new BigDecimal(match.group(1)).stripTrailingZeros().toPlainString() + (match.group(2).equals("dip") ? "dp" : match.group(2));
        if (FLOAT_ATTRIBUTES.contains(name) && value.matches("-?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)"))
            return new BigDecimal(value).stripTrailingZeros().toPlainString();
        return value;
    }
    private static void token(StringBuilder out, String value) { out.append(value.length()).append(':').append(value); }
    private static void canonical(Node node, StringBuilder out, boolean identityOnly) {
        if (node instanceof Element element) {
            if (identityOnly && shield(element)) {
                for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) canonical(child, out, true);
                return;
            }
            out.append('(');
            token(out, expanded(element));
            if (identityOnly) {
                token(out, attr(element, "id"));
                token(out, attr(element, "tag"));
            } else {
                Map<String, String> attributes = new TreeMap<>();
                NamedNodeMap nodes = element.getAttributes();
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node attribute = nodes.item(i);
                    if ("http://www.w3.org/2000/xmlns/".equals(attribute.getNamespaceURI())) continue;
                    attributes.put(expanded(attribute), value(attribute));
                }
                for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                    token(out, attribute.getKey());
                    token(out, attribute.getValue());
                }
            }
            out.append('|');
            for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) canonical(child, out, identityOnly);
            out.append(')');
        } else if (!identityOnly && (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE)
                && !node.getNodeValue().trim().isEmpty()) {
            out.append('T'); token(out, node.getNodeValue());
        }
    }
    private static String canonical(Element element, boolean identityOnly) {
        StringBuilder out = new StringBuilder(); canonical(element, out, identityOnly); return out.toString();
    }
    private static String normalCategory(Element element) {
        String tag = element.getTagName();
        if (tag.endsWith(".InputStationView")) return "searchInput";
        if (tag.endsWith(".SearchDateStateView")) return "searchDateState";
        if (tag.endsWith(".SearchCondStateView")) return "searchConditionState";
        if (tag.endsWith(".SearchResultListView")) return "routeResults";
        if (tag.endsWith(".SearchBtnView")) return "reSearchButton";
        if (attr(element, "id").matches("@\\+?id/button_search")) return "searchButton";
        return null;
    }
    private static final class Snapshot {
        final Map<String, String> layouts = new TreeMap<>();
        final Map<String, String> identities = new TreeMap<>();
        final Map<String, String> normalViews = new TreeMap<>();
        final Map<String, Integer> normalCategories = new TreeMap<>();
        int originalElements, originalIds, originalTags, wrappers, homeSlots, nearMeSlots, sdkAdGroups, farePromotionSlots;
    }
    private static Snapshot snapshot(Path res, boolean requireProtection) throws Exception {
        Snapshot snapshot = new Snapshot();
        List<Path> paths;
        try (Stream<Path> files = Files.walk(res)) {
            paths = files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".xml")
                && path.getParent().getFileName().toString().matches("layout(?:-.*)?")).sorted().toList();
        }
        for (Path file : paths) {
            String relative = res.relativize(file).toString().replace(File.separatorChar, '/');
            Element root = read(file).getDocumentElement();
            snapshot.layouts.put(relative, canonical(root, false));
            snapshot.identities.put(relative, canonical(root, true));
            List<Element> elements = new ArrayList<>();
            elements.add(root);
            var descendants = root.getElementsByTagName("*");
            for (int i = 0; i < descendants.getLength(); i++) elements.add((Element)descendants.item(i));
            int homeSlots = 0, normalIndex = 0;
            for (Element element : elements) {
                if (shield(element)) {
                    snapshot.wrappers++;
                    if (requireProtection) {
                        check(attr(element, "id").isEmpty(), relative + ": shield has no ID");
                        check("gone".equals(attr(element, "visibility")), relative + ": shield hidden");
                        check("0dp".equals(value(element.getAttributeNodeNS(ANDROID, "layout_width"))), relative + ": shield width zero");
                        check("0dp".equals(value(element.getAttributeNodeNS(ANDROID, "layout_height"))), relative + ": shield height zero");
                        check("0".equals(value(element.getAttributeNodeNS(ANDROID, "alpha"))), relative + ": shield transparent");
                        int children = 0;
                        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) if (child instanceof Element) children++;
                        check(children == 1, relative + ": shield contains exactly one original view");
                    }
                } else {
                    snapshot.originalElements++;
                    if (!attr(element, "id").isEmpty()) snapshot.originalIds++;
                    if (!attr(element, "tag").isEmpty()) snapshot.originalTags++;
                }
                if (file.getFileName().toString().equals("fragment_navi_search.xml")
                        && attr(element, "id").matches("@\\+?id/(top_ad_view|bottom_ad_view)")) {
                    homeSlots++;
                    if (requireProtection) check(behindShield(element, AD_MARKER), relative + ": home ad slot lacks hidden ancestor: " + attr(element, "id"));
                }
                if (element.getTagName().equals("jp.co.yahoo.android.apps.transit.ui.compose.components.nearme.NearMeView")) {
                    snapshot.nearMeSlots++;
                    String expectedMarker = file.getFileName().toString().equals("fragment_search_result_list.xml") ? NEARME_MARKER : AD_MARKER;
                    if (requireProtection) check(behindShield(element, expectedMarker), relative + ": NearMe lacks hidden ancestor");
                }
                if (element.getTagName().equals("jp.co.yahoo.android.ads.util.AdViewGroup")) {
                    snapshot.sdkAdGroups++;
                    if (requireProtection) check(behindShield(element, null) || "gone".equals(attr(element, "visibility")), relative + ": SDK ad group remains visible");
                }
                if (file.getFileName().toString().equals("view_fare_module_fare.xml") && attr(element, "id").equals("@id/promotion_banner_container")) {
                    snapshot.farePromotionSlots++;
                    if (requireProtection) check("gone".equals(attr(element, "visibility")), relative + ": confirmed PR fare block remains visible");
                }
                String category = normalCategory(element);
                if (category != null) {
                    String key = relative + ":" + category + ":" + attr(element, "id") + ":" + (++normalIndex);
                    String parent = element.getParentNode() instanceof Element p ? expanded(p) + ":" + attr(p, "id") + ":" + attr(p, "tag") : "root";
                    snapshot.normalViews.put(key, sha(parent + canonical(element, false)));
                    snapshot.normalCategories.merge(category, 1, Integer::sum);
                    if (requireProtection) check(!behindShield(element, null), key + ": normal search view hidden by an ad wrapper");
                }
            }
            if (file.getFileName().toString().equals("fragment_navi_search.xml")) check(homeSlots == 2, relative + ": both home ad views present");
            snapshot.homeSlots += homeSlots;
        }
        check(snapshot.layouts.size() > 0, "real layout resources decoded");
        check(snapshot.homeSlots >= 2, "actual home layout found");
        check(snapshot.nearMeSlots >= 1, "actual NearMe view found");
        check(snapshot.sdkAdGroups >= 1, "actual SDK ad groups found");
        check(snapshot.farePromotionSlots == 1, "actual fare PR block found");
        for (String category : new String[]{"searchInput", "searchDateState", "searchConditionState", "searchButton", "routeResults", "reSearchButton"})
            check(snapshot.normalCategories.getOrDefault(category, 0) > 0, "normal view coverage: " + category);
        return snapshot;
    }
    private static void compare(Map<String, String> expected, Map<String, String> actual, String label) {
        check(expected.keySet().equals(actual.keySet()), label + ": same file/view keys");
        for (String key : expected.keySet()) check(expected.get(key).equals(actual.get(key)), label + ": " + key);
    }
    private static PatcherConfig config(File apk, File work) throws Exception {
        try {
            Constructor<PatcherConfig> ctor = PatcherConfig.class.getConstructor(File.class, File.class,
                Set.class, BytecodeMode.class, DexVerifier.class, File.class, String.class, boolean.class);
            return ctor.newInstance(apk, work, Collections.emptySet(), BytecodeMode.NONE, NoOpDexVerifier.INSTANCE, null, null, false);
        } catch (NoSuchMethodException legacy) {
            Constructor<PatcherConfig> ctor = PatcherConfig.class.getConstructor(File.class, File.class, String.class,
                String.class, boolean.class, Set.class, BytecodeMode.class, DexVerifier.class, File.class);
            return ctor.newInstance(apk, work, null, null, true, Collections.emptySet(), BytecodeMode.NONE, NoOpDexVerifier.INSTANCE, null);
        }
    }
    private static int verifyDistributedArtifact(Path shipped) throws Exception {
        try (ZipFile original = new ZipFile(shipped.toFile())) {
            check(original.getEntry("classes.dex") != null, "distributed Android DEX present");
            check(original.getEntry("app/hiro/yahootransit/patches/NearMeLayout.class") != null,
                "distributed JVM helper present without a test bridge");
            long count = original.stream().filter(e -> e.getName().endsWith(".class")
                && (e.getName().startsWith("app/hiro/yahootransit/")
                || e.getName().startsWith("jp/hiro/patches/yahootransit/"))).count();
            check(count == 29, "exact distributed Yahoo JVM class inventory");
            return (int)count;
        }
    }
    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            List<String> entries = new ArrayList<>();
            for (var entry : map.entrySet()) entries.add(json(entry.getKey().toString()) + ":" + json(entry.getValue()));
            return "{" + String.join(",", entries) + "}";
        }
        if (value instanceof Iterable<?> values) {
            List<String> items = new ArrayList<>(); for (Object item : values) items.add(json(item));
            return "[" + String.join(",", items) + "]";
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 32) out.append(String.format("\\u%04x", (int)c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: RealResourceRoundtripTest distributed.mpp archived-resources.apk work-directory report.json");
        Path shipped = Paths.get(args[0]).toAbsolutePath();
        Path apk = Paths.get(args[1]).toAbsolutePath(), work = Paths.get(args[2]).toAbsolutePath(), report = Paths.get(args[3]).toAbsolutePath();
        check(Files.isRegularFile(apk), "private archived APK argument is required");
        check(ClassLoader.getSystemResource("app/hiro/yahootransit/patches/NearMeLayout.class") == null, "helper must load from distributed MPP, not host source classpath");
        int yahooClasses = verifyDistributedArtifact(shipped);
        int apkDexEntries;
        String shippedDexSha;
        try (ZipFile archive = new ZipFile(apk.toFile()); ZipFile bundle = new ZipFile(shipped.toFile())) {
            apkDexEntries = (int)archive.stream().filter(e -> e.getName().matches("classes(?:[0-9]+)?\\.dex")).count();
            try (InputStream dex = bundle.getInputStream(bundle.getEntry("classes.dex"))) { shippedDexSha = sha(dex.readAllBytes()); }
        }
        ResourcePatch selected = null;
        for (Patch<?> patch : PatchBundleLoader.INSTANCE.loadFlat(Collections.singleton(shipped.toFile()))) {
            if ("広告・不要欄を非表示".equals(patch.getName())) {
                check(selected == null && patch instanceof ResourcePatch, "unique selected Yahoo resource patch");
                selected = (ResourcePatch)patch;
            }
        }
        check(selected != null, "production loader discovers Yahoo resource patch");
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("schema", 1);
        output.put("status", "PASS");
        output.put("test", "RealResourceRoundtripTest");
        output.put("archivedInputProvenance", "Previously modified private Yahoo Transit archive; not a pristine official APK.");
        output.put("inputApkSha256", sha(apk));
        output.put("inputApkDexEntries", apkDexEntries);
        output.put("pristineOriginalApkAvailable", false);
        output.put("archivedAppCodeMissing", apkDexEntries == 0);
        output.put("shippedMppSha256", sha(shipped));
        output.put("shippedDexSha256", shippedDexSha);
        output.put("evidenceIdentity", "shippedMppSha256");
        output.put("candidateLoadedDirectly", true);
        output.put("testOnlyBridge", false);
        output.put("distributedYahooJvmClasses", yahooClasses);
        Path sdk = Paths.get(Patcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        output.put("sdkSha256", sha(sdk));
        output.put("resourceOnly", true);
        output.put("originalCodeDependencyExercised", false);
        output.put("deviceTested", false);
        output.put("limitations", "Registered resource execution and real resource encode/redecode only. Original application code, dependency scheduling on original code, installation, and Android UI runtime are not tested.");
        PatcherConfig config = config(apk.toFile(), work.resolve("apply").toFile());
        try (Patcher patcher = new Patcher(config)) {
            patcher.plusAssign(Collections.singleton(selected));
            try { output.put("resolvedPatcherBehaviors", PatcherConfig.class.getMethod("getBehaviors$morphe_patcher").invoke(config).toString()); }
            catch (NoSuchMethodException legacy) { output.put("resolvedPatcherBehaviors", "legacy SDK: eager resource decoding"); }
            ResourcePatchContext context = patcher.getContext().getResourceContext$morphe_patcher();
            context.decodeResources$morphe_patcher(ResourceMode.FULL);
            Element manifest = read(context.get("AndroidManifest.xml", false).toPath()).getDocumentElement();
            check("jp.co.yahoo.android.apps.transit".equals(manifest.getAttribute("package")), "Yahoo package identity");
            String version = attr(manifest, "versionName");
            check(Set.of("8.4.1", "8.5.0").contains(version), "archived resource version supported by this evidence test");
            output.put("inputVersionName", version);
            Snapshot before = snapshot(context.get("res", false).toPath(), false);
            selected.execute(context);
            Snapshot after = snapshot(context.get("res", false).toPath(), true);
            compare(before.identities, after.identities, "all original view types, IDs, tags and relative topology preserved");
            compare(before.normalViews, after.normalViews, "normal search view contents and parents preserved");
            int existingActionLayouts = 0;
            for (String path : before.layouts.keySet()) {
                if (!path.endsWith("/view_search_btn.xml")) continue;
                check(before.layouts.get(path).equals(after.layouts.get(path)),
                    "archived result actions, including adjacent buttons and parent properties, unchanged: " + path);
                existingActionLayouts++;
            }
            check(existingActionLayouts > 0, "archived result action layout coverage");
            output.put("existingResultActionLayoutCount", existingActionLayouts);
            output.put("existingResultActionLayoutsUnchanged", true);
            selected.execute(context);
            Snapshot repeated = snapshot(context.get("res", false).toPath(), true);
            compare(after.layouts, repeated.layouts, "repeated execution idempotence");
            check(after.wrappers == repeated.wrappers, "no wrapper multiplication");
            try { ResourcePatchContext.class.getMethod("finishDecoding$morphe_patcher").invoke(context); }
            catch (NoSuchMethodException legacy) { /* eager legacy decoder */ }
            PatcherResult.PatchedResources compiled = context.get();
            File encoded = compiled.getResourcesApk();
            check(encoded != null && encoded.isFile(), "resource APK encoded");
            PatcherConfig roundtripConfig = config(encoded, work.resolve("roundtrip").toFile());
            roundtripConfig.setResourceMode$morphe_patcher(ResourceMode.FULL);
            try (ResourcePatchContext roundtrip = new ResourcePatchContext(roundtripConfig)) {
                roundtrip.decodeResources$morphe_patcher(ResourceMode.FULL);
                Snapshot decoded = snapshot(roundtrip.get("res", false).toPath(), true);
                compare(after.layouts, decoded.layouts, "binary resource roundtrip structure");
                compare(before.identities, decoded.identities, "roundtrip original IDs and binding tags");
                compare(before.normalViews, decoded.normalViews, "roundtrip normal search view content");
                Map<String, String> modified = new TreeMap<>();
                for (String path : after.layouts.keySet()) if (!before.layouts.get(path).equals(after.layouts.get(path))) modified.put(path, sha(after.layouts.get(path)));
                check(!modified.isEmpty(), "actual resource changes were made");
                output.put("layoutCount", before.layouts.size());
                output.put("originalElementCount", before.originalElements);
                output.put("originalIdCount", before.originalIds);
                output.put("originalBindingTagCount", before.originalTags);
                output.put("originalViewIdentityTopologyPreserved", true);
                output.put("originalIdentityTopologySha256", sha(json(before.identities)));
                output.put("normalViewCount", before.normalViews.size());
                output.put("normalViewCategories", before.normalCategories);
                output.put("normalViewFingerprints", before.normalViews);
                output.put("normalSearchContentAndParentsPreserved", true);
                output.put("homeProtectedSlotCount", decoded.homeSlots);
                output.put("nearMeProtectedSlotCount", decoded.nearMeSlots);
                output.put("sdkAdGroupProtectedCount", decoded.sdkAdGroups);
                output.put("farePromotionHiddenCount", decoded.farePromotionSlots);
                output.put("hiddenWrapperCount", decoded.wrappers);
                output.put("modifiedLayoutCount", modified.size());
                output.put("modifiedLayoutStructuralSha256", modified);
                output.put("idempotent", true);
                output.put("binaryRoundtripStructureEqual", true);
                output.put("structuralCanonicalization", "Namespace-expanded names, sorted attributes, original child order, nonblank text, equivalent Android numeric dimensions/floats and fill_parent normalized; not a binary byte equality claim.");
            }
        }
        output.put("assertions", assertions);
        Files.createDirectories(report.getParent());
        Files.writeString(report, json(output) + "\n", StandardCharsets.UTF_8);
        System.out.println("PASS RealResourceRoundtripTest: " + assertions + " assertions; " + output.get("layoutCount") + " real layouts; " + output.get("hiddenWrapperCount") + " hidden wrappers; original IDs/tags and normal search views preserved; idempotent and binary resource roundtrip equal; archived modified input, resource-only, device not tested.");
    }
}
