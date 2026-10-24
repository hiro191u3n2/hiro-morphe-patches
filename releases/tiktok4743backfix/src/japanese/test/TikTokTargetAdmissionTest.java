/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import app.hiro.tiktokjapanese.patches.TikTokJapanesePatch;
import app.hiro.tiktok4753.patches.TikTok4753LegacyPatches;
import app.hiro.tiktok4753.patches.LegacyPrivacy;
import app.morphe.patcher.PackageMetadata;
import app.morphe.patcher.PatcherConfig;
import app.morphe.patcher.dex.BytecodeMode;
import app.morphe.patcher.dex.DexVerifier;
import app.morphe.patcher.patch.*;

/** Execute the real admission blocks without compatibility filtering or APK mutation. */
public final class TikTokTargetAdmissionTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    private static final DexVerifier NO_DEX = new DexVerifier() {
        public void verifyDexFile(File file) { throw new AssertionError("Admission touched DEX"); }
        public void verifyDexDirectory(File file) { throw new AssertionError("Admission touched DEX"); }
        public void verifyApkFile(File file) { throw new AssertionError("Admission touched APK"); }
    };
    private static PatcherConfig config(Path path) throws Exception {
        // The release validates its loader on Patcher 1.x and 2.x. Their config
        // constructors differ; no private or uninitialized objects are used.
        for (Constructor<?> c : PatcherConfig.class.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 9 && p[2] == String.class)
                return (PatcherConfig)c.newInstance(path.resolve("unused.apk").toFile(), path.toFile(),
                    null, null, true, Collections.emptySet(), BytecodeMode.FULL, NO_DEX, null);
            if (p.length == 8 && p[2] == Set.class)
                return (PatcherConfig)c.newInstance(path.resolve("unused.apk").toFile(), path.toFile(),
                    Collections.emptySet(), BytecodeMode.FULL, NO_DEX, null, "2.0.0", true);
        }
        throw new AssertionError("Unsupported official PatcherConfig constructor");
    }
    private static BytecodePatchContext context(PatcherConfig config, String packageName, String version) {
        return new BytecodePatchContext(config, new PackageMetadata(packageName, version, "1", null));
    }
    private static void metadata(Patch<?> patch) {
        List<Compatibility> entries = patch.getCompatibility();
        check(entries.size() == 1, "Unexpected compatibility package count: " + patch);
        Compatibility c = entries.get(0);
        check("com.zhiliaoapp.musically".equals(c.getPackageName()), "Unexpected package: " + patch);
        check(c.getTargets() != null && c.getTargets().size() == 1
            && "47.4.3".equals(c.getTargets().get(0).getVersion()), "Unrestricted or wrong version: " + patch);
    }
    private static BytecodePatch onlyDependency(Patch<?> patch) {
        check(patch.getDependencies().size() == 1, "Missing unique dependency: " + patch);
        Patch<?> dependency = patch.getDependencies().iterator().next();
        check(dependency instanceof BytecodePatch, "Admission dependency is not bytecode: " + patch);
        metadata(dependency);
        return (BytecodePatch)dependency;
    }
    private static boolean explainsVersion(Throwable failure) {
        for (Throwable e = failure; e != null; e = e.getCause())
            if (e.getMessage() != null && e.getMessage().contains("47.4.3")) return true;
        return false;
    }
    private static void rejects(BytecodePatch guard, PatcherConfig config, String packageName, String version) {
        boolean rejected = false;
        try (BytecodePatchContext context = context(config, packageName, version)) {
            // Directly execute the production block as a forced selection would.
            // Metadata filtering cannot make this assertion pass.
            guard.execute(context);
        } catch (RuntimeException expected) {
            rejected = explainsVersion(expected);
        }
        check(rejected, "Forced target not rejected by production admission: " + packageName + " " + version);
    }
    private static void exercise(BytecodePatch guard, PatcherConfig config) {
        metadata(guard);
        check(guard.getExtensionInputStream() == null, "Admission merges an extension before rejection");
        check(guard.getDependencies().isEmpty(), "Unexpected earlier admission dependency");
        try (BytecodePatchContext context = context(config, "com.zhiliaoapp.musically", "47.4.3")) {
            guard.execute(context);
            check(context.classDefByOrNull("Lunrelated/Absent;") == null, "Admission changed classes");
        }
        for (String version : Arrays.asList("47.5.3", "47.5.2", "47.4.2", "46.2.3", "", "47.4.3 "))
            rejects(guard, config, "com.zhiliaoapp.musically", version);
        rejects(guard, config, "com.ss.android.ugc.trill", "47.4.3");
    }
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("tiktok-target-admission-");
        try {
            PatcherConfig config = config(directory);
            metadata(TikTokJapanesePatch.PATCH);
            exercise(onlyDependency(TikTokJapanesePatch.PATCH), config);
            // A direct invocation of its execute/finalize blocks must reject the
            // dropped version before looking for the upstream translation class.
            rejects(TikTokJapanesePatch.PATCH, config, "com.zhiliaoapp.musically", "47.5.3");
            boolean finalizerRejected = false;
            try (BytecodePatchContext context = context(config, "com.zhiliaoapp.musically", "47.5.3")) {
                TikTokJapanesePatch.PATCH.finalize(context);
            } catch (RuntimeException expected) { finalizerRejected = explainsVersion(expected); }
            check(finalizerRejected, "Japanese finalizer changed unverified classes");
            Set<BytecodePatch> admissions = Collections.newSetFromMap(new IdentityHashMap<BytecodePatch, Boolean>());
            for (BytecodePatch patch : Arrays.asList(TikTok4753LegacyPatches.HIDE_TEXT,
                    TikTok4753LegacyPatches.HIDE_PROMOTIONS, TikTok4753LegacyPatches.SHOW_SEEKBAR,
                    TikTok4753LegacyPatches.SHOW_THUMBNAIL, TikTok4753LegacyPatches.HIDE_STORIES)) {
                metadata(patch);
                BytecodePatch runtime = onlyDependency(patch);
                check(runtime.getExtensionInputStream() != null, "Native runtime extension absent");
                admissions.add(onlyDependency(runtime));
                rejects(patch, config, "com.zhiliaoapp.musically", "47.5.3");
            }
            check(admissions.size() == 1, "Native admission is not shared by all five patches");
            exercise(admissions.iterator().next(), config);
            metadata(LegacyPrivacy.PATCH);
            exercise(onlyDependency(LegacyPrivacy.PATCH), config);
            check(!Files.exists(directory.resolve("unused.apk")), "Admission created an APK");
            try (java.util.stream.Stream<Path> children = Files.list(directory)) {
                check(!children.findAny().isPresent(), "Admission wrote resources before accepting the target");
            }
            System.out.println("PASS TikTokTargetAdmissionTest checks=" + checks
                + "; 47.4.3 only; all seven native/Japanese entries and dependencies; forced 47.5.3 rejected before mutation");
        } finally {
            Files.deleteIfExists(directory);
        }
    }
}
