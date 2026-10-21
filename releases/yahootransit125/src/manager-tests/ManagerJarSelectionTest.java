import app.morphe.manager.patcher.DeviceConfig;
import app.morphe.manager.patcher.PatchPipeline;
import app.morphe.manager.patcher.PatchRequest;
import app.morphe.manager.patcher.PatchingListener;
import app.morphe.manager.patcher.logger.LogLevel;
import app.morphe.manager.patcher.logger.Logger;
import app.morphe.manager.patcher.patch.PatchKeysKt;
import app.morphe.manager.patcher.split.SplitPreparationEvent;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.Patch;
import app.morphe.patcher.patch.PatchKt;
import app.morphe.patcher.patch.ResourcePatch;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;

/**
 * Executes the selection method from the official Manager 2.0.0-dev.15 JVM host.
 * The runtime classpath MUST contain only these tests and that host JAR. In
 * particular, neither the MPP nor a converted/source Yahoo bridge belongs there.
 * Both bundles are opened by the same loadPatchesFromJar call used by HostMain.
 * This test covers discovery and selection, not APK execution or Android ART.
 *
 * Args: baseline269.mpp candidate270.mpp manager-dev15-host.jar report.json
 */
public final class ManagerJarSelectionTest {
    private static final String HOST_SHA =
            "17c30bcbbb7bb51fe0c89e878d56fcb968c8d248abaaa372d0e1137d4543d044";
    private static final String BASELINE_SHA =
            "f1737d0ad69fa8573bda7012a454ed2f1c967ff0b8b9426a5f9302abf02fde93";
    private static final String PACKAGE = "jp.co.yahoo.android.apps.transit";
    private static final String KEY = "広告・不要欄を非表示";
    private static final String PROVIDER = "jp.hiro.patches.yahootransit.YahooTransitAdPatch";
    private static final String HELPER = "app.hiro.yahootransit.patches.NearMeLayout";
    private static final String PRESERVE = "app.hiro.yahootransit.patches.PreserveYahooCode";
    private static int assertions;

    public static void main(String[] args) throws Exception {
        check(args.length == 4, "Expected baseline, candidate, official host and report paths");
        Path baseline = Path.of(args[0]).toRealPath();
        Path candidate = Path.of(args[1]).toRealPath();
        Path host = Path.of(args[2]).toRealPath();
        Path report = Path.of(args[3]).toAbsolutePath();
        check(sha(host).equals(HOST_SHA), "Official Manager host SHA mismatch");
        check(sha(baseline).equals(BASELINE_SHA), "Published 269 SHA mismatch");
        for (Class<?> type : List.of(PatchPipeline.class, PatchKeysKt.class, PatchKt.class)) {
            check(origin(type).equals(host), "Manager/loader class is not from pinned official host: " + type);
        }
        Path testClasses = origin(ManagerJarSelectionTest.class);
        String[] classpath = System.getProperty("java.class.path").split(File.pathSeparator);
        check(classpath.length == 2, "Classpath must be test classes plus official host only");
        Set<Path> classpathPaths = new java.util.HashSet<>();
        for (String entry : classpath) classpathPaths.add(Path.of(entry).toRealPath());
        check(classpathPaths.equals(Set.of(testClasses, host)), "Unexpected runtime classpath");
        check(!testClasses.equals(candidate) && !testClasses.equals(baseline), "MPP is on system classpath");
        for (String type : List.of(PROVIDER, HELPER, PRESERVE)) assertAbsentFromSystemLoader(type);

        Map<String, Object> archive = verifyArchive(baseline, candidate);
        Map<String, Object> baselineSelection = select(baseline, 8, 1);
        Map<String, Object> positive = select(candidate, 8, 1);
        // Loading a bundle must never inject its implementation into the host parent.
        for (String type : List.of(PROVIDER, HELPER, PRESERVE)) assertAbsentFromSystemLoader(type);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "PASS");
        result.put("method", "Official Manager PatchPipeline.selectedPatches + HostMain loadPatchesFromJar");
        result.put("manager_version", "2.0.0-dev.15");
        result.put("patcher_version", "2.0.0-dev.22");
        result.put("host_sha256", HOST_SHA);
        result.put("package_name", PACKAGE);
        result.put("requested_selection", List.of(KEY));
        result.put("isolated_runtime_classpath", true);
        result.put("test_only_yahoo_bridge", false);
        result.put("archive", archive);
        result.put("published_269", baselineSelection);
        result.put("candidate_270", positive);
        result.put("assertions", assertions);
        result.put("apk_execution_in_this_test", false);
        result.put("android_art_execution_in_this_test", false);
        Files.createDirectories(report.getParent());
        Files.writeString(report, json(result) + "\n", StandardCharsets.UTF_8);
        System.out.println("PASS ManagerJarSelectionTest: " + assertions
                + " assertions; published 269 selected 1; candidate 270 selected 1; no external Yahoo classes");
    }

    private static Map<String, Object> select(Path bundlePath, int expectedLoaded,
                                               int expectedSelected) throws Exception {
        ProbeLogger logger = new ProbeLogger();
        ProbeListener listener = new ProbeListener();
        PatchRequest.Bundle bundle = new PatchRequest.Bundle(1, bundlePath.toString(), Set.of(KEY), Map.of());
        String unused = bundlePath.getParent().resolve("selection-only-unused").toString();
        PatchRequest request = new PatchRequest(PACKAGE, unused, unused, unused, unused, unused,
                List.of(bundle), false, new DeviceConfig(List.of("arm64-v8a"), List.of("ja-JP"), 420));
        List<Collection<? extends Patch<?>>> loaded = new ArrayList<>();
        PatchPipeline pipeline = new PatchPipeline(request, logger, listener, (b, continuation) -> {
            // Identical API and argument shape to official HostMain. The only bundle
            // supplied is the exact argument MPP; no supplementary bridge is loaded.
            Collection<? extends Patch<?>> patches = PatchKt.loadPatchesFromJar(Set.of(new File(b.getPatchesFile())));
            loaded.add(patches);
            return patches;
        });
        Method selectedPatches = PatchPipeline.class.getDeclaredMethod("selectedPatches", Continuation.class);
        selectedPatches.setAccessible(true);
        Object raw = selectedPatches.invoke(pipeline, new Continuation<Object>() {
            public CoroutineContext getContext() { return EmptyCoroutineContext.INSTANCE; }
            public void resumeWith(Object result) { throw new AssertionError("Unexpected asynchronous selection"); }
        });
        check(raw != IntrinsicsKt.getCOROUTINE_SUSPENDED(), "Selection unexpectedly suspended");
        check(raw instanceof List<?>, "Selection did not return a list");
        List<?> selected = (List<?>) raw;
        check(loaded.size() == 1, "Exact bundle must be loaded once");
        check(loaded.get(0).size() == expectedLoaded, "Wrong total JVM-discovered patch count");
        Map<String, ? extends Patch<?>> keyed = PatchKeysKt.keyedFor(loaded.get(0), PACKAGE);
        check(keyed.size() == expectedSelected, "Wrong Yahoo-compatible key count");
        check(selected.size() == expectedSelected, "Wrong actual Manager selected count");
        check(listener.steps == 1, "Manager loading step must complete exactly once");
        check(logger.logs.isEmpty(), "Unexpected selection logs: " + logger.logs);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mpp_sha256", sha(bundlePath));
        result.put("jvm_discovered_patches", loaded.get(0).size());
        result.put("package_compatible_keys", new ArrayList<>(keyed.keySet()));
        result.put("manager_requested_count", request.getBundles().get(0).getPatches().size());
        result.put("manager_selected_count", selected.size());
        result.put("manager_loading_steps", listener.steps);
        result.put("selection_logs", logger.logs);
        if (expectedSelected == 1) {
            check(keyed.keySet().equals(Set.of(KEY)), "Manager key differs from selected UI key");
            check(selected.get(0) == keyed.get(KEY), "Manager selected a different patch object");
            Patch<?> patch = (Patch<?>) selected.get(0);
            check(patch instanceof ResourcePatch, "Actual selection must be the resource patch");
            check(KEY.equals(patch.getName()), "Unexpected selected patch name");
            check(patch.getCompatibility() != null && patch.getCompatibility().size() == 1,
                    "Unexpected compatibility metadata");
            var compatibility = patch.getCompatibility().get(0);
            check(PACKAGE.equals(compatibility.getPackageName()), "Wrong package compatibility");
            check(compatibility.getTargets() == null || compatibility.getTargets().isEmpty()
                            || compatibility.getTargets().stream().anyMatch(target -> target.getVersion() == null
                                    && target.getVersionCodes() == null && target.getMinSdk() == null),
                    "Unexpected version-restricted compatibility: " + compatibility.getTargets());
            check(patch.getDependencies().size() == 1, "Preservation dependency must remain attached");
            Patch<?> dependency = patch.getDependencies().iterator().next();
            check(dependency instanceof BytecodePatch, "Expected original-code preservation bytecode dependency");
            Class<?> owner = executeOwner(patch);
            check(owner.getName().equals(PROVIDER + "$ExecuteBlockHandler"), "Unexpected execute handler");
            check(origin(owner).equals(bundlePath), "Execute handler was not loaded from the candidate MPP");
            Class<?> provider = Class.forName(PROVIDER, false, owner.getClassLoader());
            Class<?> helper = Class.forName(HELPER, false, owner.getClassLoader());
            Class<?> preserve = Class.forName(PRESERVE, false, owner.getClassLoader());
            for (Class<?> type : List.of(provider, helper, preserve)) {
                check(origin(type).equals(bundlePath), "Yahoo implementation came from outside candidate: " + type);
            }
            check(owner.getClassLoader() != ClassLoader.getSystemClassLoader(), "Bundle implementation leaked into parent");
            result.put("selected_patch_type", patch.getClass().getName());
            result.put("dependency_type", dependency.getClass().getName());
            result.put("execution_handler", owner.getName());
            result.put("execution_handler_from_candidate_mpp", true);
            result.put("unrestricted_app_versions", true);
        }
        return result;
    }

    private static Class<?> executeOwner(Patch<?> patch) throws Exception {
        Field execute = Patch.class.getDeclaredField("executeBlock");
        execute.setAccessible(true);
        Object block = execute.get(patch);
        check(block != null, "Resource execute block is missing");
        return Proxy.isProxyClass(block.getClass())
                ? Proxy.getInvocationHandler(block).getClass() : block.getClass();
    }

    private static Map<String, Object> verifyArchive(Path baseline, Path candidate) throws Exception {
        try (ZipFile before = new ZipFile(baseline.toFile()); ZipFile after = new ZipFile(candidate.toFile())) {
            Set<String> oldEntries = new TreeSet<>();
            before.stream().forEach(entry -> oldEntries.add(entry.getName()));
            Set<String> newEntries = new TreeSet<>();
            after.stream().forEach(entry -> newEntries.add(entry.getName()));
            check(oldEntries.size() == before.size(), "Duplicate baseline archive entries");
            check(newEntries.size() == after.size(), "Duplicate candidate archive entries");
            check(oldEntries.size() == 124 && oldEntries.equals(newEntries), "Candidate must retain the same 124 archive entries");
            Set<String> expectedChanges = Set.of("META-INF/MANIFEST.MF", "classes.dex",
                    "app/hiro/yahootransit/patches/NearMeLayout.class");
            Set<String> changed = new TreeSet<>();
            for (String entry : oldEntries) {
                boolean same = Arrays.equals(before.getInputStream(before.getEntry(entry)).readAllBytes(),
                        after.getInputStream(after.getEntry(entry)).readAllBytes());
                if (!same) changed.add(entry);
                check(same || expectedChanges.contains(entry), "Candidate changed an unrelated baseline entry: " + entry);
            }
            check(changed.equals(expectedChanges), "Only manifest, Android DEX and matching Yahoo helper JVM class must change");
            long yahooClasses = newEntries.stream().filter(e -> e.endsWith(".class")
                    && (e.startsWith("app/hiro/yahootransit/patches/") || e.startsWith("jp/hiro/patches/yahootransit/"))).count();
            check(yahooClasses == 29, "All 29 Yahoo JVM classes must remain packaged");
            Manifest baselineManifest = new Manifest(before.getInputStream(before.getEntry("META-INF/MANIFEST.MF")));
            check("1.0.269".equals(baselineManifest.getMainAttributes().getValue("Version")), "Wrong baseline version");
            Manifest manifest = new Manifest(after.getInputStream(after.getEntry("META-INF/MANIFEST.MF")));
            check("1.0.270".equals(manifest.getMainAttributes().getValue("Version")), "Wrong candidate version");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("archive_entry_count", oldEntries.size());
            result.put("preserved_baseline_entries", oldEntries.size() - changed.size());
            result.put("changed_archive_entries", changed);
            result.put("added_archive_entries", 0);
            result.put("removed_archive_entries", 0);
            result.put("packaged_yahoo_jvm_classes", yahooClasses);
            result.put("other_jvm_classes_preserved_byte_for_byte", true);
            return result;
        }
    }

    private static void assertAbsentFromSystemLoader(String name) throws Exception {
        try {
            Class.forName(name, false, ClassLoader.getSystemClassLoader());
            throw new AssertionError("External Yahoo class masks the bundle: " + name);
        } catch (ClassNotFoundException expected) { assertions++; }
    }
    private static Path origin(Class<?> type) throws Exception {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
    }
    private static String sha(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            List<String> pairs = new ArrayList<>();
            for (var entry : map.entrySet()) pairs.add(json(entry.getKey().toString()) + ":" + json(entry.getValue()));
            return "{" + String.join(",", pairs) + "}";
        }
        if (value instanceof Collection<?> list) {
            List<String> parts = new ArrayList<>();
            for (Object item : list) parts.add(json(item));
            return "[" + String.join(",", parts) + "]";
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            if (c == '\\' || c == '"') out.append('\\').append(c);
            else if (c < 32) out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
    private static final class ProbeLogger extends Logger {
        final List<String> logs = new ArrayList<>();
        public void log(LogLevel level, String message) { logs.add(level + ": " + message); }
    }
    private static final class ProbeListener implements PatchingListener {
        int steps;
        public Object stepCompleted(Continuation<? super Unit> c) { steps++; return Unit.INSTANCE; }
        public Object splitPreparation(SplitPreparationEvent e, Continuation<? super Unit> c) { throw unexpected(); }
        public Object patchesStarted(Continuation<? super Unit> c) { throw unexpected(); }
        public Object patchSucceeded(String n, Continuation<? super Unit> c) { throw unexpected(); }
        public Object patchFailed(String n, String e, Continuation<? super Unit> c) { throw unexpected(); }
        public Object patchesFinished(int n, Continuation<? super Unit> c) { throw unexpected(); }
        private AssertionError unexpected() { return new AssertionError("Selection unexpectedly entered APK execution"); }
    }
}
