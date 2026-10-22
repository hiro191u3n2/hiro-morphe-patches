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
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
 * Calls the actual official Manager dev.16 patch selection path on distributed MPPs.
 * Runtime classpath must contain only these tests and the pinned official host JAR.
 * No Berry implementation, converted source bridge or candidate MPP may be placed
 * on the parent classpath. This test does not execute an APK or Android ART.
 *
 * Args: baseline271.mpp candidate272.mpp manager-dev16-host.jar report.json
 */
public final class BerryManagerTest {
    private static final String HOST_SHA =
            "cbc973be92d2962d21194377155bce3792c6c8d6cccd68bd6e93ccfc344a6d1c";
    private static final String BASELINE_SHA =
            "83ffc3416c99cef5d0464a147633f3d064a8b1856282715031176d2bb3cdbf7a";
    private static final String PACKAGE = "jp.ejimax.berrybrowser";
    private static final String QUEUE = "ロックフリーMessageQueueを有効化";
    private static final Set<String> KEYS = Collections.unmodifiableSet(new LinkedHashSet<>(List.of(
            "プレビュー作成を後回しにして表示中タブを優先",
            "共有先ブラウザの表示を個別に切替",
            "ダブルタップから指を離さず片手ズーム",
            QUEUE)));
    // The first three types are the actual public PATCH field descriptors recovered
    // from baseline271 classes.dex: ForegroundTabsPatch/BrowserVisibilityPatch are
    // BytecodePatch, while OneHandZoomPatch is ResourcePatch with bytecode dependencies.
    private static final Map<String, Class<?>> EXPECTED_TYPES = Map.of(
            "プレビュー作成を後回しにして表示中タブを優先", BytecodePatch.class,
            "共有先ブラウザの表示を個別に切替", BytecodePatch.class,
            "ダブルタップから指を離さず片手ズーム", ResourcePatch.class,
            QUEUE, ResourcePatch.class);
    private static final List<String> PREFIXES = List.of(
            "app/hiro/berry/", "app/hiro/berryshare/", "app/hiro/berryzoom/", "app/hiro/berryqueue/");
    private static int assertions;

    public static void main(String[] args) throws Exception {
        check(args.length == 4, "Expected baseline, candidate, official host and report paths");
        Path baseline = Path.of(args[0]).toRealPath();
        Path candidate = Path.of(args[1]).toRealPath();
        Path host = Path.of(args[2]).toRealPath();
        Path report = Path.of(args[3]).toAbsolutePath();
        check(sha(host).equals(HOST_SHA), "Official Manager host SHA mismatch");
        check(sha(baseline).equals(BASELINE_SHA), "Published baseline271 SHA mismatch");
        check(!baseline.equals(candidate), "Baseline and candidate must be distinct");
        for (Class<?> type : List.of(PatchPipeline.class, PatchKeysKt.class, PatchKt.class)) {
            check(origin(type).equals(host), "Manager/loader code is not from the official host: " + type);
        }
        Path tests = origin(BerryManagerTest.class);
        String[] classpath = System.getProperty("java.class.path").split(File.pathSeparator);
        check(classpath.length == 2, "Runtime classpath must be only test classes and official host");
        Set<Path> classpathPaths = new LinkedHashSet<>();
        for (String entry : classpath) classpathPaths.add(Path.of(entry).toRealPath());
        check(classpathPaths.equals(Set.of(tests, host)), "Unexpected runtime classpath entry");
        check(!tests.equals(candidate) && !tests.equals(baseline), "MPP is on the system classpath");

        Set<String> berryClasses = candidateBerryClasses(candidate);
        check(!berryClasses.isEmpty(), "Candidate contains no Berry JVM implementation classes");
        for (String name : berryClasses) assertAbsentFromSystemLoader(name);
        check("1.0.271".equals(manifestVersion(baseline)), "Baseline manifest version differs");
        check("1.0.272".equals(manifestVersion(candidate)), "Combined candidate manifest version differs");

        Selection old = select(baseline, 4);
        Selection current = select(candidate, 4);
        check(current.loadedCount == old.loadedCount,
                "Public patch count must be preserved");
        check(current.otherNames.equals(old.otherNames), "Unrelated JVM patch names changed");
        for (String name : berryClasses) assertAbsentFromSystemLoader(name);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "PASS");
        result.put("method", "Official Manager PatchPipeline.selectedPatches + PatchKt.loadPatchesFromJar");
        result.put("manager_version", "2.0.0-dev.16");
        result.put("patcher_version", "2.0.0-dev.22");
        result.put("host_sha256", HOST_SHA);
        result.put("package_name", PACKAGE);
        result.put("requested_selection", new ArrayList<>(KEYS));
        result.put("isolated_runtime_classpath", true);
        result.put("external_berry_bridge", false);
        result.put("packaged_berry_jvm_classes", berryClasses.size());
        result.put("published_271", old.report);
        result.put("candidate_272", current.report);
        result.put("unrelated_jvm_patch_names_preserved", true);
        result.put("assertions", assertions);
        result.put("apk_execution_in_this_test", false);
        result.put("android_art_execution_in_this_test", false);
        Files.createDirectories(report.getParent());
        Files.writeString(report, json(result) + "\n", StandardCharsets.UTF_8);
        System.out.println("PASS BerryManagerTest: " + assertions
                + " assertions; published271 selected4; candidate272 selected4; all-version metadata; isolated official host");
    }

    private static Selection select(Path bundlePath, int expectedSelected) throws Exception {
        ProbeLogger logger = new ProbeLogger();
        ProbeListener listener = new ProbeListener();
        PatchRequest.Bundle bundle = new PatchRequest.Bundle(1, bundlePath.toString(), KEYS, Map.of());
        String unused = bundlePath.getParent().resolve("selection-only-unused").toString();
        PatchRequest request = new PatchRequest(PACKAGE, unused, unused, unused, unused, unused,
                List.of(bundle), false, new DeviceConfig(List.of("arm64-v8a"), List.of("ja-JP"), 420));
        List<Collection<? extends Patch<?>>> loaded = new ArrayList<>();
        PatchPipeline pipeline = new PatchPipeline(request, logger, listener, (b, continuation) -> {
            Collection<? extends Patch<?>> patches = PatchKt.loadPatchesFromJar(Set.of(new File(b.getPatchesFile())));
            loaded.add(patches);
            return patches;
        });
        Method method = PatchPipeline.class.getDeclaredMethod("selectedPatches", Continuation.class);
        method.setAccessible(true);
        Object raw = method.invoke(pipeline, new Continuation<Object>() {
            public CoroutineContext getContext() { return EmptyCoroutineContext.INSTANCE; }
            public void resumeWith(Object value) { throw new AssertionError("Unexpected asynchronous selection"); }
        });
        check(raw != IntrinsicsKt.getCOROUTINE_SUSPENDED(), "Selection unexpectedly suspended");
        check(raw instanceof List<?>, "Selection did not return a list");
        List<?> selected = (List<?>) raw;
        check(loaded.size() == 1, "Exact distribution bundle must load once");
        Map<String, ? extends Patch<?>> keyed = PatchKeysKt.keyedFor(loaded.get(0), PACKAGE);
        check(keyed.size() == expectedSelected, "Wrong count of Berry-compatible Manager keys");
        check(selected.size() == expectedSelected, "Wrong actual Manager selected count");
        check(listener.steps == 1, "Manager loading step must complete once");
        check(logger.logs.isEmpty(), "Unexpected selection logs: " + logger.logs);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mpp_sha256", sha(bundlePath));
        result.put("jvm_discovered_patches", loaded.get(0).size());
        result.put("package_compatible_keys", new ArrayList<>(new TreeSet<>(keyed.keySet())));
        result.put("manager_requested_count", request.getBundles().get(0).getPatches().size());
        result.put("manager_selected_count", selected.size());
        result.put("manager_loading_steps", listener.steps);
        result.put("selection_logs", logger.logs);
        List<Object> implementations = new ArrayList<>();
        if (expectedSelected != 0) {
            check(keyed.keySet().equals(KEYS), "Manager keys differ from the four requested names");
            Set<Patch<?>> selectedObjects = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Object item : selected) {
                check(item instanceof Patch<?>, "Selected object is not a patch");
                Patch<?> patch = (Patch<?>) item;
                check(selectedObjects.add(patch), "Duplicate selected patch object");
                check(patch == keyed.get(patch.getName()), "Manager selected a different patch object");
                check(patch.getCompatibility() != null && patch.getCompatibility().size() == 1,
                        "Unexpected compatibility metadata for " + patch.getName());
                var compatibility = patch.getCompatibility().get(0);
                check(PACKAGE.equals(compatibility.getPackageName()), "Wrong package compatibility");
                check(compatibility.getTargets() == null || compatibility.getTargets().isEmpty()
                                || compatibility.getTargets().stream().anyMatch(target -> target.getVersion() == null
                                        && target.getVersionCodes() == null && target.getMinSdk() == null),
                        "Version-restricted Manager metadata remains: " + patch.getName());
                check(patch.getClass() == EXPECTED_TYPES.get(patch.getName()),
                        "Public Berry patch type differs from its verified definition: " + patch.getName());
                if (QUEUE.equals(patch.getName())) {
                    check(patch.getDependencies().size() == 1, "MessageQueue runtime dependency missing");
                    Patch<?> runtime = patch.getDependencies().iterator().next();
                    check(runtime instanceof BytecodePatch, "MessageQueue dependency must preserve and inject bytecode");
                }
                Set<Patch<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
                checkNodeOrigins(patch, bundlePath, visited);
                Class<?> owner = executeOwner(patch);
                Map<String, Object> implementation = new LinkedHashMap<>();
                implementation.put("name", patch.getName());
                implementation.put("type", patch.getClass().getName());
                implementation.put("execute_handler", owner.getName());
                implementation.put("execute_handler_from_distributed_mpp", true);
                implementation.put("unrestricted_app_versions", true);
                implementation.put("reachable_execution_nodes", visited.size());
                implementations.add(implementation);
            }
        }
        // Stable ordering keeps QA reproducible despite the loader's Set iteration.
        implementations.sort((a, b) -> ((Map<?, ?>) a).get("name").toString()
                .compareTo(((Map<?, ?>) b).get("name").toString()));
        result.put("implementations", implementations);
        Set<String> otherNames = new TreeSet<>();
        for (Patch<?> patch : loaded.get(0)) if (!keyed.containsValue(patch))
            otherNames.add(patch.getName());
        return new Selection(loaded.get(0).size(), otherNames, result);
    }

    private static void checkNodeOrigins(Patch<?> node, Path bundle, Set<Patch<?>> visited) throws Exception {
        if (!visited.add(node)) return;
        Class<?> owner = executeOwner(node);
        check(origin(owner).equals(bundle), "Execute handler is outside the distributed MPP: " + owner.getName());
        check(owner.getClassLoader() != ClassLoader.getSystemClassLoader(), "Bundle implementation leaked to parent loader");
        for (Patch<?> dependency : node.getDependencies()) checkNodeOrigins(dependency, bundle, visited);
    }

    private static Class<?> executeOwner(Patch<?> patch) throws Exception {
        Field execute = Patch.class.getDeclaredField("executeBlock");
        execute.setAccessible(true);
        Object block = execute.get(patch);
        check(block != null, "Patch execute block missing");
        return Proxy.isProxyClass(block.getClass())
                ? Proxy.getInvocationHandler(block).getClass() : block.getClass();
    }

    private static Set<String> candidateBerryClasses(Path bundle) throws Exception {
        Set<String> result = new TreeSet<>();
        try (ZipFile zip = new ZipFile(bundle.toFile())) {
            for (var entry : Collections.list(zip.entries())) {
                String name = entry.getName();
                if (name.endsWith(".class") && PREFIXES.stream().anyMatch(name::startsWith))
                    check(result.add(name.substring(0, name.length() - 6).replace('/', '.')), "Duplicate Berry JVM class");
            }
        }
        return result;
    }

    private static String manifestVersion(Path bundle) throws Exception {
        try (ZipFile zip = new ZipFile(bundle.toFile())) {
            return new Manifest(zip.getInputStream(zip.getEntry("META-INF/MANIFEST.MF")))
                    .getMainAttributes().getValue("Version");
        }
    }

    private static void assertAbsentFromSystemLoader(String name) throws Exception {
        try {
            Class.forName(name, false, ClassLoader.getSystemClassLoader());
            throw new AssertionError("External Berry class masks the candidate MPP: " + name);
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
            List<String> parts = new ArrayList<>();
            for (var entry : map.entrySet()) parts.add(json(entry.getKey().toString()) + ":" + json(entry.getValue()));
            return "{" + String.join(",", parts) + "}";
        }
        if (value instanceof Collection<?> list) {
            List<String> parts = new ArrayList<>();
            for (Object item : list) parts.add(json(item));
            return "[" + String.join(",", parts) + "]";
        }
        StringBuilder result = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            if (c == '\\' || c == '"') result.append('\\').append(c);
            else if (c < 32) result.append(String.format("\\u%04x", (int) c));
            else result.append(c);
        }
        return result.append('"').toString();
    }

    private record Selection(int loadedCount, Set<String> otherNames, Map<String, Object> report) {}

    private static final class ProbeLogger extends Logger {
        final List<String> logs = new ArrayList<>();
        public void log(LogLevel level, String message) { logs.add(level + ": " + message); }
    }

    private static final class ProbeListener implements PatchingListener {
        int steps;
        public Object stepCompleted(Continuation<? super Unit> c) { steps++; return Unit.INSTANCE; }
        public Object splitPreparation(SplitPreparationEvent e, Continuation<? super Unit> c) { throw unexpected(); }
        public Object patchesStarted(Continuation<? super Unit> c) { throw unexpected(); }
        public Object patchSucceeded(String name, Continuation<? super Unit> c) { throw unexpected(); }
        public Object patchFailed(String name, String error, Continuation<? super Unit> c) { throw unexpected(); }
        public Object patchesFinished(int count, Continuation<? super Unit> c) { throw unexpected(); }
        private AssertionError unexpected() { return new AssertionError("Selection unexpectedly entered APK execution"); }
    }
}
