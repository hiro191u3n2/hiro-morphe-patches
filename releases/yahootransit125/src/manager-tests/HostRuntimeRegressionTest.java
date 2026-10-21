package app.hiro.yahootransit.patches;

import app.morphe.patcher.Patcher;
import app.morphe.patcher.PatcherConfig;
import app.morphe.patcher.PatcherResult;
import app.morphe.patcher.apk.ApkUtils;
import app.morphe.patcher.dex.BytecodeMode;
import app.morphe.patcher.dex.DexVerifier;
import app.morphe.patcher.dex.NoOpDexVerifier;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.Patch;
import app.morphe.patcher.patch.PatchKt;
import app.morphe.patcher.patch.PatchResult;
import app.morphe.patcher.patch.ResourcePatch;
import app.morphe.patcher.patch.ResourcePatchContext;
import app.morphe.patcher.resource.ResourceMode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.JarURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Packaged-artifact regression for Manager's JVM host. Uses only a generated synthetic APK.
 * Run with tests + the pinned Manager dev.15 host on the classpath, never helper/source/bridge classes.
 * Every bundle is loaded directly from its exact distribution bytes by loadPatchesFromJar.
 */
public final class HostRuntimeRegressionTest {
    private static final String PACKAGE = "jp.co.yahoo.android.apps.transit";
    private static final String PATCH_NAME = "広告・不要欄を非表示";
    private static final String NS = "http://schemas.android.com/apk/res/android";
    private static final String MF = "META-INF/MANIFEST.MF";
    private static final String BASELINE_SHA = "f1737d0ad69fa8573bda7012a454ed2f1c967ff0b8b9426a5f9302abf02fde93";
    private static final String HOST_SHA = "17c30bcbbb7bb51fe0c89e878d56fcb968c8d248abaaa372d0e1137d4543d044";
    private static final String FRAMEWORKS_SHA = "5bb489e6db867c21dd953705669e80f093dde3e6adb4a5b7465068ab9fb23cfe";
    private static final String PROVIDER = "jp.hiro.patches.yahootransit.YahooTransitAdPatch";
    private static final String HELPER = "app.hiro.yahootransit.patches.NearMeLayout";
    private static final String PRESERVE = "app.hiro.yahootransit.patches.PreserveYahooCode";
    private static final Set<String> EXPECTED_ARCHIVE_CHANGES = Set.of(MF, "classes.dex",
        "app/hiro/yahootransit/patches/NearMeLayout.class");
    private static final Set<String> HIDE_ATTRIBUTES = Set.of("layout_width", "layout_height", "minWidth", "minHeight",
        "maxWidth", "maxHeight", "layout_marginHorizontal", "layout_marginVertical", "layout_margin", "layout_marginLeft",
        "layout_marginTop", "layout_marginRight", "layout_marginBottom", "layout_marginStart", "layout_marginEnd", "padding",
        "paddingLeft", "paddingTop", "paddingRight", "paddingBottom", "paddingStart", "paddingEnd", "paddingHorizontal",
        "paddingVertical", "visibility", "layout_weight", "alpha", "clipChildren", "clipToPadding", "clickable", "focusable",
        "importantForAccessibility", "enabled", "longClickable", "focusableInTouchMode");
    private static final Set<String> FIXTURE_CLASSES = Set.of(
        "Ljp/co/yahoo/android/apps/transit/TransitApplication;", "Landroidx/core/app/CoreComponentFactory;");
    private static int assertions;

    private static void check(boolean condition, String reason) { assertions++; if (!condition) throw new AssertionError(reason); }
    private static String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static String sha(Path path) throws Exception { return sha(Files.readAllBytes(path)); }
    private static Path origin(Class<?> type) throws Exception {
        return Paths.get(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
    }
    private static void verifyRuntimeClasspath() throws Exception {
        Path host = origin(Patcher.class), tests = origin(HostRuntimeRegressionTest.class);
        check(HOST_SHA.equals(sha(host)), "pinned official Manager host");
        for (Class<?> type : List.of(PatchKt.class, ResourcePatch.class, ApkUtils.class))
            check(origin(type).equals(host), "production loader/patcher implementation from official host");
        var resource = Patcher.class.getClassLoader().getResource("frameworks/android/android-23.apk");
        check(resource != null && resource.openConnection() instanceof JarURLConnection, "frameworks loaded from resource-only JAR");
        Path frameworks = Paths.get(((JarURLConnection)resource.openConnection()).getJarFileURL().toURI()).toRealPath();
        check(FRAMEWORKS_SHA.equals(sha(frameworks)), "pinned official framework resources");
        String[] entries = System.getProperty("java.class.path").split(File.pathSeparator);
        Set<Path> paths = new LinkedHashSet<>();
        for (String value : entries) paths.add(Paths.get(value).toRealPath());
        check(entries.length == 3 && paths.equals(Set.of(tests, host, frameworks)), "classpath contains only tests, official host and resource-only frameworks");
        assertNoExternalImplementation();
    }
    private static void assertNoExternalImplementation() throws Exception {
        for (String name : List.of(PROVIDER, HELPER, PRESERVE)) {
            try {
                Class.forName(name, false, ClassLoader.getSystemClassLoader());
                throw new AssertionError("Implementation leaked onto system classpath: " + name);
            } catch (ClassNotFoundException expected) { assertions++; }
        }
    }
    private static void verifyImplementationOrigin(Patch<?> patch, Path bundle) throws Exception {
        Field execute = Patch.class.getDeclaredField("executeBlock"); execute.setAccessible(true);
        Object block = execute.get(patch); check(block != null, "resource execute block registered");
        Class<?> owner = Proxy.isProxyClass(block.getClass()) ? Proxy.getInvocationHandler(block).getClass() : block.getClass();
        check(origin(owner).equals(bundle.toRealPath()), "registered execute handler comes from exact argument MPP");
        for (String name : List.of(PROVIDER, HELPER, PRESERVE)) {
            Class<?> type = Class.forName(name, false, owner.getClassLoader());
            check(origin(type).equals(bundle.toRealPath()), "implementation comes from exact argument MPP: " + name);
        }
        check(owner.getClassLoader() != ClassLoader.getSystemClassLoader(), "bundle implementation has isolated loader");
    }
    private static byte[] entry(Path path, String name) throws Exception {
        try (ZipFile file = new ZipFile(path.toFile()); InputStream input = file.getInputStream(file.getEntry(name))) { return input.readAllBytes(); }
    }
    private static boolean yahooClass(String name) {
        return name.endsWith(".class") && (name.startsWith("app/hiro/yahootransit/") || name.startsWith("jp/hiro/patches/yahootransit/"));
    }
    private static Map<String, String> payload(Path path, boolean omitSignatures) throws Exception {
        Map<String, String> hashes = new TreeMap<>();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || omitSignatures && entry.getName().matches("META-INF/(?:[^/]+\\.(?:SF|RSA|DSA|EC)|MANIFEST\\.MF)")) continue;
                try (InputStream input = zip.getInputStream(entry)) { hashes.put(entry.getName(), sha(input.readAllBytes())); }
            }
        }
        return hashes;
    }
    private static Map<String, String> frameworkResources() throws Exception {
        // Manager's JvmInstallation.unpackHost copies these exact APK resources into its host.
        // CI supplies them as a separate resource-only JAR; no alternate implementation classes.
        Map<String, String> frameworks = new TreeMap<>();
        for (int api = 23; api <= 37; api++) {
            String name = "frameworks/android/android-" + api + ".apk";
            try (InputStream input = Patcher.class.getClassLoader().getResourceAsStream(name)) {
                check(input != null, "official Manager framework resource present: " + name);
                frameworks.put(name, sha(input.readAllBytes()));
            }
        }
        return frameworks;
    }
    private static PatcherConfig config(Path apk, Path temporary, Path workspace) throws Exception {
        try {
            Constructor<PatcherConfig> ctor = PatcherConfig.class.getConstructor(File.class, File.class,
                Set.class, BytecodeMode.class, DexVerifier.class, File.class, String.class, boolean.class);
            return ctor.newInstance(apk.toFile(), temporary.toFile(), Collections.emptySet(), BytecodeMode.STRIP_FAST,
                NoOpDexVerifier.INSTANCE, workspace.toFile(), null, false);
        } catch (NoSuchMethodException oldSdk) {
            Constructor<PatcherConfig> ctor = PatcherConfig.class.getConstructor(File.class, File.class, String.class,
                String.class, boolean.class, Set.class, BytecodeMode.class, DexVerifier.class, File.class);
            return ctor.newInstance(apk.toFile(), temporary.toFile(), null, null, true, Collections.emptySet(),
                BytecodeMode.STRIP_FAST, NoOpDexVerifier.INSTANCE, workspace.toFile());
        }
    }
    private static boolean compatible(Patch<?> patch) throws Exception {
        // Same package predicate as Manager 2.0.0-dev.15 PatchKeys.kt. Version filtering is not done there.
        List<?> compatibility = (List<?>)Patch.class.getMethod("getCompatibility").invoke(patch);
        if (compatibility == null) return true;
        for (Object target : compatibility) {
            Object packageName = target.getClass().getMethod("getPackageName").invoke(target);
            if (packageName == null || PACKAGE.equals(packageName)) return true;
        }
        return false;
    }
    private static Element document(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(file.toFile()).getDocumentElement();
    }
    private static String normalizedAttribute(String value) {
        if (value.equals("match_parent")) return "fill_parent";
        if (value.matches("-?[0-9]+(?:\\.[0-9]+)?(?:dp|dip|sp|px)?")) {
            var matcher = java.util.regex.Pattern.compile("(-?[0-9]+(?:\\.[0-9]+)?)(.*)").matcher(value);
            matcher.matches();
            return new java.math.BigDecimal(matcher.group(1)).stripTrailingZeros().toPlainString()
                + matcher.group(2).replace("dip", "dp");
        }
        return value;
    }
    private static boolean reverseButton(Element element) {
        return element.getAttributeNS(NS, "id").matches("@\\+?id/synthetic_reverse_search");
    }
    private static String fingerprint(Element element, boolean omitTargetHideAttributes) {
        Map<String, String> attributes = new TreeMap<>();
        var nodes = element.getAttributes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if ("http://www.w3.org/2000/xmlns/".equals(node.getNamespaceURI())) continue;
            if (omitTargetHideAttributes && reverseButton(element) && NS.equals(node.getNamespaceURI())
                && HIDE_ATTRIBUTES.contains(node.getLocalName())) continue;
            attributes.put(node.getNodeName(), normalizedAttribute(node.getNodeValue()));
        }
        StringBuilder signature = new StringBuilder("[").append(element.getTagName()).append(json(attributes));
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling())
            if (child instanceof Element) signature.append(fingerprint((Element)child, omitTargetHideAttributes));
        return signature.append(']').toString();
    }
    private static List<Element> childElements(Element parent) {
        List<Element> children = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling())
            if (child instanceof Element) children.add((Element)child);
        return children;
    }
    private static boolean zeroDimension(String value) { return "0dp".equals(normalizedAttribute(value)); }
    private static void verifyButtonGroup(ResourcePatchContext context, Element original, boolean expectedHidden) throws Exception {
        Element buttons = document(context.get("res/layout/view_search_btn.xml", false).toPath());
        check("merge".equals(buttons.getTagName()), "button-group merge parent retained");
        List<Element> children = childElements(buttons);
        check(children.size() == 3, "exact three original direct children, no inserted wrapper");
        String[] ids = {"result_now_condition", "synthetic_reverse_search", "result_back_condition"};
        String[] strings = {"result_research_current_time", "synthetic_reverse_search", "result_list_change_condition"};
        for (int i = 0; i < children.size(); i++) {
            Element button = children.get(i);
            check("Button".equals(button.getTagName()), "original native Button class retained");
            check(button.getAttributeNS(NS, "id").matches("@\\+?id/" + ids[i]), "button ID and sibling order retained");
            check(("@string/" + strings[i]).equals(button.getAttributeNS(NS, "text")), "original button string reference retained");
            if (i != 1 || !expectedHidden) {
                check("visible".equals(button.getAttributeNS(NS, "visibility")), "unselected button stays visible");
                check("1".equals(normalizedAttribute(button.getAttributeNS(NS, "alpha"))), "unselected button alpha retained");
                check("true".equals(button.getAttributeNS(NS, "enabled"))
                    && "true".equals(button.getAttributeNS(NS, "clickable")), "unselected button stays operable");
            }
        }
        check(fingerprint(buttons, expectedHidden).equals(fingerprint(original, expectedHidden)),
            "only middle button hide attributes may change; every ID/class/text/tag/parent/order and neighbouring attribute retained");
        Element middle = children.get(1);
        if (expectedHidden) {
            check("gone".equals(middle.getAttributeNS(NS, "visibility")), "middle button hidden");
            for (String dimension : new String[]{"layout_width", "layout_height", "minWidth", "minHeight", "maxWidth", "maxHeight",
                    "layout_margin", "layout_marginTop", "padding"})
                check(zeroDimension(middle.getAttributeNS(NS, dimension)), "middle button collapsed: " + dimension);
            check("0".equals(normalizedAttribute(middle.getAttributeNS(NS, "alpha"))), "middle button transparent");
            for (String flag : new String[]{"enabled", "clickable", "longClickable", "focusable", "focusableInTouchMode"})
                check("false".equals(middle.getAttributeNS(NS, flag)), "middle button interaction disabled: " + flag);
            check("noHideDescendants".equals(middle.getAttributeNS(NS, "importantForAccessibility")), "middle button excluded from accessibility");
        }
    }
    private static Map<String, String> layoutFingerprints(ResourcePatchContext context) throws Exception {
        Map<String, String> result = new TreeMap<>();
        for (String name : new String[]{"fragment_navi_search.xml", "fragment_search_result_list.xml", "view_search_btn.xml"})
            result.put(name, sha(fingerprint(document(context.get("res/layout/" + name, false).toPath()), true).getBytes(StandardCharsets.UTF_8)));
        return result;
    }
    private static boolean hiddenAncestor(Element element, String marker) {
        for (Node node = element.getParentNode(); node instanceof Element; node = node.getParentNode()) {
            Element parent = (Element)node;
            if (marker.equals(parent.getAttributeNS(NS, "tag")) && "gone".equals(parent.getAttributeNS(NS, "visibility"))) return true;
        }
        return false;
    }
    private static void verifyResources(ResourcePatchContext context) throws Exception {
        Element manifest = document(context.get("AndroidManifest.xml", false).toPath());
        check(PACKAGE.equals(manifest.getAttribute("package")), "fixture package preserved");
        Element home = document(context.get("res/layout/fragment_navi_search.xml", false).toPath());
        var views = home.getElementsByTagName("*");
        int adSlots = 0, searchButtons = 0;
        for (int i = 0; i < views.getLength(); i++) {
            Element view = (Element)views.item(i);
            if (view.getAttributeNS(NS, "id").matches("@\\+?id/search_button")) {
                searchButtons++;
                check(!hiddenAncestor(view, "hiro_ad_slot_hidden_v2")
                    && !hiddenAncestor(view, "hiro_nearme_pr_hidden_v1"), "normal home search button not in ad shield");
            }
            if (!view.getAttributeNS(NS, "id").matches("@\\+?id/(top_ad_view|bottom_ad_view)")) continue;
            adSlots++;
            check(hiddenAncestor(view, "hiro_ad_slot_hidden_v2"), "home ad shield retained in encoded output");
        }
        check(adSlots == 2, "both home ad views present");
        check(searchButtons == 1, "normal home search button retained");
        Element results = document(context.get("res/layout/fragment_search_result_list.xml", false).toPath());
        var nearMe = results.getElementsByTagName("jp.co.yahoo.android.apps.transit.ui.compose.components.nearme.NearMeView");
        check(nearMe.getLength() == 1, "one NearMe view retained");
        check(hiddenAncestor((Element)nearMe.item(0), "hiro_nearme_pr_hidden_v1"), "NearMe shield retained in encoded output");
        for (String normal : new String[]{"jp.co.yahoo.android.apps.transit.ui.view.navi.list.SearchResultListView", "jp.co.yahoo.android.apps.transit.ui.view.navi.SearchBtnView"}) {
            var normalViews = results.getElementsByTagName(normal);
            check(normalViews.getLength() == 1, "normal route/search-again view retained: " + normal);
            check(!hiddenAncestor((Element)normalViews.item(0), "hiro_nearme_pr_hidden_v1")
                && !hiddenAncestor((Element)normalViews.item(0), "hiro_ad_slot_hidden_v2"), "normal route/search-again view not in ad shield");
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, Object> run(Path bundle, Path fixture, Path work,
                                          Element originalButtons, boolean expectedReverseHidden) throws Exception {
        Files.createDirectories(work.resolve("temporary")); Files.createDirectories(work.resolve("workspace"));
        assertNoExternalImplementation();
        Set<Patch<?>> loaded = PatchKt.loadPatchesFromJar(Collections.singleton(bundle.toFile()));
        Set<Patch<?>> selected = new LinkedHashSet<>();
        for (Patch<?> patch : loaded) if (compatible(patch) && PATCH_NAME.equals(patch.getName())) selected.add(patch);
        check(loaded.size() == 8, "all eight packaged JVM patches discovered");
        check(selected.size() == 1, "JAR loader selects exactly one Yahoo resource patch");
        verifyImplementationOrigin(selected.iterator().next(), bundle);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("bundleSha256", sha(bundle)); record.put("loadedJarPatchCount", loaded.size());
        record.put("requestedNameCount", 1); record.put("selectedYahooCount", selected.size());
        record.put("implementationFromExactMpp", true);
        Map<String, String> before = payload(fixture, true);
        PatcherConfig config = config(fixture, work.resolve("temporary"), work.resolve("workspace"));
        try (Patcher patcher = new Patcher(config)) {
            patcher.plusAssign(selected);
            int allNodes = patcher.getContext().getAllPatches$morphe_patcher().size();
            check(allNodes == 2, "resource patch plus bytecode dependency execution nodes");
            check(config.getResourceMode$morphe_patcher() == ResourceMode.FULL, "full resource mode retained");
            check(config.getBytecodeMode$morphe_patcher() == BytecodeMode.STRIP_FAST, "bytecode preservation mode retained");
            Patch<?> selectedPatch = selected.iterator().next();
            check(selectedPatch instanceof ResourcePatch && selectedPatch.getDependencies().size() == 1
                && selectedPatch.getDependencies().iterator().next() instanceof BytecodePatch,
                "real resource patch and PreserveYahooCode dependency registered");
            record.put("executionNodesWithDependencies", allNodes);
            record.put("resourceMode", config.getResourceMode$morphe_patcher().name());
            record.put("bytecodeMode", config.getBytecodeMode$morphe_patcher().name());
            List<PatchResult> results = new ArrayList<>();
            ByteArrayOutputStream witness = new ByteArrayOutputStream();
            PrintStream console = System.out;
            try (PrintStream capture = new PrintStream(witness, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> patcher.invoke().collect(
                        (value, emitContinuation) -> { results.add(value); return kotlin.Unit.INSTANCE; },
                        (Continuation)continuation));
            } finally { System.setOut(console); }
            String trace = witness.toString(StandardCharsets.UTF_8); console.print(trace);
            check(results.size() == 1, "one selected result; dependencies do not emit separate Flow results");
            for (PatchResult result : results) check(result.getException() == null && PATCH_NAME.equals(result.getPatch().getName()),
                "registered Yahoo execute succeeded: " + result.getException());
            boolean dependencyExecuted = trace.contains("Yahoo code: application definitions verified; DEX output enabled");
            boolean adsExecuted = trace.contains("protected ad slot(s). Original Views and IDs retained.");
            check(dependencyExecuted, "dependency execution witness");
            check(adsExecuted, "resource execution witness");
            record.put("successfulFlowResults", results.size()); record.put("dependencyExecuted", dependencyExecuted);
            record.put("adResourceExecuted", adsExecuted);
            ResourcePatchContext applied = patcher.getContext().getResourceContext$morphe_patcher();
            verifyResources(applied); verifyButtonGroup(applied, originalButtons, expectedReverseHidden);
            Map<String, String> appliedFingerprints = layoutFingerprints(applied);
            PatcherResult compiled = patcher.get();
            File resourceApk = compiled.getResources().getResourcesApk();
            check(resourceApk != null, "both bundles compile patched resources");
            record.put("compiledResourceApk", true); record.put("compiledDexOutputs", compiled.getDexFiles().size());
            Path output = work.resolve("DO_NOT_INSTALL_synthetic_output.apk");
            Files.copy(resourceApk.toPath(), output, StandardCopyOption.REPLACE_EXISTING);
            ApkUtils.INSTANCE.applyTo(compiled, output.toFile());
            Map<String, String> after = payload(output, true);
            check(before.keySet().equals(after.keySet()), "all synthetic application archive entries retained");
            check(before.get("classes.dex").equals(after.get("classes.dex")), "original synthetic DEX bytes retained");
            int changed = 0;
            for (String name : before.keySet()) if (!before.get(name).equals(after.get(name))) changed++;
            check(changed > 0, "advertisement resources changed in both bundles");
            record.put("applicationEntriesChanged", changed); record.put("inputApplicationEntryCount", before.size());
            record.put("outputApplicationEntryCount", after.size()); record.put("syntheticDexBytesPreserved", true);
            PatcherConfig verifyConfig = config(output, work.resolve("verify-output"), work.resolve("verify-workspace"));
            verifyConfig.setResourceMode$morphe_patcher(ResourceMode.FULL);
            try (ResourcePatchContext context = new ResourcePatchContext(verifyConfig)) {
                context.decodeResources$morphe_patcher(ResourceMode.FULL);
                verifyResources(context); verifyButtonGroup(context, originalButtons, expectedReverseHidden);
                Map<String, String> roundtripFingerprints = layoutFingerprints(context);
                check(appliedFingerprints.equals(roundtripFingerprints), "binary resource roundtrip preserves tested layout structure");
                record.put("decodedLayoutFingerprintsIgnoringTargetHideAttributes", roundtripFingerprints);
            }
            record.put("encodedAdShieldsVerified", true); record.put("advertisementShields", 3);
            record.put("reverseSearchButtonHidden", expectedReverseHidden);
            record.put("neighbouringButtonsVisibleAndUnchanged", true);
            record.put("originalButtonIdsClassesTextsTagsParentsAndOrderRetained", true);
            record.put("binaryRoundtripVerified", true);
        }
        assertNoExternalImplementation();
        return record;
    }
    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            List<String> entries = new ArrayList<>();
            for (var e : map.entrySet()) entries.add(json(e.getKey().toString()) + ":" + json(e.getValue()));
            return "{" + String.join(",", entries) + "}";
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> entries = new ArrayList<>(); for (Object item : iterable) entries.add(json(item));
            return "[" + String.join(",", entries) + "]";
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 32) out.append(String.format("\\u%04x", (int)c)); else out.append(c);
        }
        return out.append('"').toString();
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: HostRuntimeRegressionTest base269.mpp candidate270.mpp synthetic.apk work-directory report.json");
        Path base = Paths.get(args[0]).toAbsolutePath(), candidate = Paths.get(args[1]).toAbsolutePath();
        Path fixture = Paths.get(args[2]).toAbsolutePath(), work = Paths.get(args[3]).toAbsolutePath(), report = Paths.get(args[4]).toAbsolutePath();
        verifyRuntimeClasspath();
        check(BASELINE_SHA.equals(sha(base)), "exact published 269 baseline");
        Map<String, String> baseEntries = payload(base, false), candidateEntries = payload(candidate, false);
        check(baseEntries.size() == 124 && baseEntries.keySet().equals(candidateEntries.keySet()), "same 124 distributed archive entries");
        Set<String> changedEntries = new TreeSet<>();
        for (String name : baseEntries.keySet()) {
            if (!baseEntries.get(name).equals(candidateEntries.get(name))) changedEntries.add(name);
            check(baseEntries.get(name).equals(candidateEntries.get(name)) || EXPECTED_ARCHIVE_CHANGES.contains(name),
                "all unrelated shipped entries retained: " + name);
        }
        check(changedEntries.equals(EXPECTED_ARCHIVE_CHANGES), "only manifest, shipped DEX and matching helper JVM class change");
        check(baseEntries.keySet().stream().filter(HostRuntimeRegressionTest::yahooClass).count() == 29, "all Yahoo JVM implementation classes retained");
        java.util.jar.Manifest baselineManifest = new java.util.jar.Manifest(new java.io.ByteArrayInputStream(entry(base, MF)));
        java.util.jar.Manifest candidateManifest = new java.util.jar.Manifest(new java.io.ByteArrayInputStream(entry(candidate, MF)));
        check("1.0.269".equals(baselineManifest.getMainAttributes().getValue("Version")), "baseline bundle version");
        check("1.0.270".equals(candidateManifest.getMainAttributes().getValue("Version")), "candidate bundle version");
        byte[] dex = entry(fixture, "classes.dex");
        Set<String> fixtureClasses = new TreeSet<>();
        for (var definition : new DexBackedDexFile(Opcodes.forApi(26), java.nio.ByteBuffer.wrap(dex)).getClasses()) fixtureClasses.add(definition.getType());
        check(FIXTURE_CLASSES.equals(fixtureClasses), "fixture contains only two generated class definitions, not an original application DEX");
        PatcherConfig fixtureConfig = config(fixture, work.resolve("original-fixture"), work.resolve("fixture-workspace"));
        fixtureConfig.setResourceMode$morphe_patcher(ResourceMode.FULL);
        Element originalButtons;
        try (ResourcePatchContext context = new ResourcePatchContext(fixtureConfig)) {
            context.decodeResources$morphe_patcher(ResourceMode.FULL);
            originalButtons = document(context.get("res/layout/view_search_btn.xml", false).toPath());
            verifyButtonGroup(context, originalButtons, false);
            Element strings = document(context.get("res/values/strings.xml", false).toPath());
            String targetText = null;
            for (Element item : childElements(strings)) if ("synthetic_reverse_search".equals(item.getAttribute("name")))
                targetText = item.getTextContent();
            check(targetText != null, "synthetic target label is present in resources");
            var escaped = java.util.regex.Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(targetText);
            StringBuffer literal = new StringBuffer();
            while (escaped.find()) escaped.appendReplacement(literal,
                java.util.regex.Matcher.quoteReplacement(String.valueOf((char)Integer.parseInt(escaped.group(1), 16))));
            escaped.appendTail(literal);
            check("発着を入れ替えて再検索".contentEquals(literal), "exact requested label resolved from synthetic string resource");
        }
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("schema", 2); record.put("test", "HostRuntimeRegressionTest"); record.put("status", "PASS");
        record.put("sdkSha256", sha(origin(Patcher.class)));
        record.put("managerFrameworkResourcesSha256", frameworkResources());
        record.put("syntheticApkSha256", sha(fixture)); record.put("syntheticDexSha256", sha(dex));
        record.put("syntheticDexClassCount", fixtureClasses.size()); record.put("baselinePatchDexSha256", baseEntries.get("classes.dex"));
        record.put("shippedPatchDexSha256", candidateEntries.get("classes.dex")); record.put("patchDexUnchanged", false);
        record.put("changedArchiveEntries", changedEntries); record.put("packagedYahooJvmClassCount", 29);
        record.put("candidateLoadedDirectly", true); record.put("sourceOrBridgeOnClasspath", false);
        record.put("selectionMethod", "Production loadPatchesFromJar; Manager package predicate and unique requested name key, with ambiguity rejected.");
        record.put("syntheticTargetId", "synthetic_reverse_search"); record.put("exactTargetLabel", "発着を入れ替えて再検索");
        Map<String, Object> baselineRun = run(base, fixture, work.resolve("baseline269"), originalButtons, false);
        Map<String, Object> candidateRun = run(candidate, fixture, work.resolve("positive-candidate"), originalButtons, true);
        check(baselineRun.get("decodedLayoutFingerprintsIgnoringTargetHideAttributes")
            .equals(candidateRun.get("decodedLayoutFingerprintsIgnoringTargetHideAttributes")),
            "baseline and candidate differ only in the middle button hide attributes across all tested layouts");
        record.put("baseline269", baselineRun); record.put("positiveCandidate", candidateRun);
        record.put("onlyMiddleButtonChangedBetweenBundles", true);
        record.put("assertions", assertions); record.put("originalYahoo856ApkTested", false); record.put("deviceTested", false);
        record.put("limitations", "Generated synthetic APK and host JVM scheduler only; synthetic_reverse_search is not an asserted original app resource ID; no original Yahoo 8.5.6 APK, signing, installation, or Android UI runtime test.");
        Files.createDirectories(report.getParent()); Files.writeString(report, json(record) + "\n", StandardCharsets.UTF_8);
        System.out.println("PASS HostRuntimeRegressionTest: exact packaged JAR loader; both bundles execute 2 nodes and emit 1 success; only candidate hides the middle reverse-search button; neighbouring buttons, 3 ad shields and original synthetic DEX retained. Synthetic APK only; no Android device test.");
    }
}
