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
    private static final Set<String> FIXTURE_CLASSES = Set.of(
        "Ljp/co/yahoo/android/apps/transit/TransitApplication;", "Landroidx/core/app/CoreComponentFactory;");
    private static int assertions;

    private static void check(boolean condition, String reason) { assertions++; if (!condition) throw new AssertionError(reason); }
    private static String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static String sha(Path path) throws Exception { return sha(Files.readAllBytes(path)); }
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
    private static Map<String, Object> run(Path bundle, Path fixture, Path work, boolean expectedExecution) throws Exception {
        Files.createDirectories(work.resolve("temporary")); Files.createDirectories(work.resolve("workspace"));
        check(ClassLoader.getSystemResource("app/hiro/yahootransit/patches/NearMeLayout.class") == null, "no implementation source/bridge classes on host classpath");
        Set<Patch<?>> loaded = PatchKt.loadPatchesFromJar(Collections.singleton(bundle.toFile()));
        Set<Patch<?>> selected = new LinkedHashSet<>();
        for (Patch<?> patch : loaded) if (compatible(patch) && PATCH_NAME.equals(patch.getName())) selected.add(patch);
        // A single relevant name uses the plain key in Manager keyedFor(...).filterKeys(requestedNames).
        check(selected.size() <= 1, "unique Yahoo key, no duplicate-key disambiguation necessary");
        check(selected.size() == (expectedExecution ? 1 : 0), "JAR loader selected Yahoo count");
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("bundleSha256", sha(bundle)); record.put("loadedJarPatchCount", loaded.size());
        record.put("requestedNameCount", 1); record.put("selectedYahooCount", selected.size());
        Map<String, String> before = payload(fixture, true);
        PatcherConfig config = config(fixture, work.resolve("temporary"), work.resolve("workspace"));
        try (Patcher patcher = new Patcher(config)) {
            patcher.plusAssign(selected);
            int allNodes = patcher.getContext().getAllPatches$morphe_patcher().size();
            check(allNodes == (expectedExecution ? 2 : 0), "resource patch plus bytecode dependency execution nodes");
            check(config.getResourceMode$morphe_patcher() == (expectedExecution ? ResourceMode.FULL : ResourceMode.NONE), "resource mode");
            check(config.getBytecodeMode$morphe_patcher() == (expectedExecution ? BytecodeMode.STRIP_FAST : BytecodeMode.NONE), "bytecode mode");
            if (expectedExecution) {
                Patch<?> selectedPatch = selected.iterator().next();
                check(selectedPatch instanceof ResourcePatch && selectedPatch.getDependencies().size() == 1
                    && selectedPatch.getDependencies().iterator().next() instanceof BytecodePatch, "real resource patch and PreserveYahooCode dependency registered");
            }
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
            check(results.size() == (expectedExecution ? 1 : 0), "one selected result; dependencies do not emit separate Flow results");
            for (PatchResult result : results) check(result.getException() == null && PATCH_NAME.equals(result.getPatch().getName()), "registered Yahoo execute succeeded");
            boolean dependencyExecuted = trace.contains("Yahoo code: application definitions verified; DEX output enabled");
            boolean adsExecuted = trace.contains("protected ad slot(s). Original Views and IDs retained.");
            check(dependencyExecuted == expectedExecution, "dependency execution witness");
            check(adsExecuted == expectedExecution, "resource execution witness");
            record.put("successfulFlowResults", results.size()); record.put("dependencyExecuted", dependencyExecuted); record.put("adResourceExecuted", adsExecuted);
            if (expectedExecution) verifyResources(patcher.getContext().getResourceContext$morphe_patcher());
            PatcherResult compiled = patcher.get();
            File resourceApk = compiled.getResources().getResourcesApk();
            check((resourceApk != null) == expectedExecution, "compiled resource output matches selection");
            record.put("compiledResourceApk", resourceApk != null);
            record.put("compiledDexOutputs", compiled.getDexFiles().size());
            Path output = work.resolve("DO_NOT_INSTALL_synthetic_output.apk");
            Files.copy(resourceApk == null ? fixture : resourceApk.toPath(), output, StandardCopyOption.REPLACE_EXISTING);
            ApkUtils.INSTANCE.applyTo(compiled, output.toFile());
            Map<String, String> after = payload(output, true);
            check(before.keySet().equals(after.keySet()), "all synthetic application archive entries retained");
            check(before.get("classes.dex").equals(after.get("classes.dex")), "original synthetic DEX bytes retained");
            int changed = 0;
            for (String name : before.keySet()) if (!before.get(name).equals(after.get(name))) changed++;
            check(expectedExecution ? changed > 0 : changed == 0, "application payload change matches resolved selection");
            record.put("applicationEntriesChanged", changed); record.put("inputApplicationEntryCount", before.size());
            record.put("outputApplicationEntryCount", after.size()); record.put("syntheticDexBytesPreserved", true);
            if (expectedExecution) {
                PatcherConfig verifyConfig = config(output, work.resolve("verify-output"), work.resolve("verify-workspace"));
                verifyConfig.setResourceMode$morphe_patcher(ResourceMode.FULL);
                try (ResourcePatchContext context = new ResourcePatchContext(verifyConfig)) {
                    context.decodeResources$morphe_patcher(ResourceMode.FULL); verifyResources(context);
                }
                record.put("encodedAdShieldsVerified", true);
            } else {
                check(compiled.getDexFiles().isEmpty(), "empty selection emits no DEX changes");
                record.put("allApplicationPayloadBytesUnchanged", true);
            }
        }
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
        if (args.length != 5) throw new IllegalArgumentException("Usage: HostRuntimeRegressionTest base268.mpp candidate269.mpp synthetic.apk work-directory report.json");
        Path base = Paths.get(args[0]).toAbsolutePath(), candidate = Paths.get(args[1]).toAbsolutePath();
        Path fixture = Paths.get(args[2]).toAbsolutePath(), work = Paths.get(args[3]).toAbsolutePath(), report = Paths.get(args[4]).toAbsolutePath();
        Map<String, String> baseEntries = payload(base, false), candidateEntries = payload(candidate, false);
        check(baseEntries.keySet().stream().noneMatch(HostRuntimeRegressionTest::yahooClass), "negative artifact contains no Yahoo JVM implementation");
        Set<String> additions = new TreeSet<>(candidateEntries.keySet()); additions.removeAll(baseEntries.keySet());
        check(additions.size() == 29 && additions.stream().allMatch(HostRuntimeRegressionTest::yahooClass), "candidate adds exactly 29 Yahoo JVM classes");
        for (String name : baseEntries.keySet()) if (!MF.equals(name)) check(baseEntries.get(name).equals(candidateEntries.get(name)), "candidate preserves shipped entry " + name);
        byte[] dex = entry(fixture, "classes.dex");
        Set<String> fixtureClasses = new TreeSet<>();
        for (var definition : new DexBackedDexFile(Opcodes.forApi(26), java.nio.ByteBuffer.wrap(dex)).getClasses()) fixtureClasses.add(definition.getType());
        check(FIXTURE_CLASSES.equals(fixtureClasses), "fixture contains only two generated class definitions, not an original application DEX");
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("schema", 1); record.put("test", "HostRuntimeRegressionTest"); record.put("status", "PASS");
        record.put("sdkSha256", sha(Paths.get(Patcher.class.getProtectionDomain().getCodeSource().getLocation().toURI())));
        record.put("managerFrameworkResourcesSha256", frameworkResources());
        record.put("syntheticApkSha256", sha(fixture)); record.put("syntheticDexSha256", sha(dex));
        record.put("syntheticDexClassCount", fixtureClasses.size()); record.put("shippedPatchDexSha256", baseEntries.get("classes.dex"));
        record.put("patchDexUnchanged", true); record.put("addedYahooJvmClasses", additions);
        record.put("candidateLoadedDirectly", true); record.put("sourceOrBridgeOnClasspath", false);
        record.put("selectionMethod", "Production loadPatchesFromJar; Manager package predicate and unique requested name key, with ambiguity rejected.");
        record.put("negative268", run(base, fixture, work.resolve("negative268"), false));
        record.put("positiveCandidate", run(candidate, fixture, work.resolve("positive-candidate"), true));
        record.put("assertions", assertions); record.put("originalYahoo856ApkTested", false); record.put("deviceTested", false);
        record.put("limitations", "Generated synthetic APK and host JVM scheduler only; no original Yahoo 8.5.6 APK, signing, installation, or Android UI runtime test.");
        Files.createDirectories(report.getParent()); Files.writeString(report, json(record) + "\n", StandardCharsets.UTF_8);
        System.out.println("PASS HostRuntimeRegressionTest: exact packaged JAR loader; old artifact selects zero, candidate executes 2 nodes and emits 1 success; compiled ad shields and original synthetic DEX retained. Synthetic APK only; no Android device test.");
    }
}
