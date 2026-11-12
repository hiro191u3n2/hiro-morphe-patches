package app.hiro.yahootransit.patches;

import app.morphe.engine.patches.PatchBundleLoader;
import app.morphe.patcher.Patcher;
import app.morphe.patcher.PatcherConfig;
import app.morphe.patcher.dex.BytecodeMode;
import app.morphe.patcher.dex.NoOpDexVerifier;
import app.morphe.patcher.patch.Patch;
import app.morphe.patcher.patch.PatchResult;
import app.morphe.patcher.patch.ResourcePatch;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import kotlin.Unit;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.flow.FlowKt;

/** Full production scheduler, preservation dependency, and APK writer on Patcher 2.3. */
public final class FullApk23Test {
    private static int assertions;
    private static void check(boolean value, String why) { assertions++; if (!value) throw new AssertionError(why); }
    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private static Map<String, String> dex(Path apk) throws Exception {
        Map<String, String> result = new TreeMap<>();
        try (ZipFile zip = new ZipFile(apk.toFile())) {
            for (ZipEntry entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes(?:[2-9]|[1-9][0-9]+)?\\.dex")) continue;
                try (InputStream input = zip.getInputStream(entry)) { result.put(entry.getName(), sha(input.readAllBytes())); }
            }
        }
        return result;
    }
    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) throw new IllegalArgumentException("MPP APK fresh-work [STRIP_FAST|FULL]");
        Path mpp = Paths.get(args[0]), apk = Paths.get(args[1]), work = Paths.get(args[2]);
        BytecodeMode mode = args.length == 4 ? BytecodeMode.valueOf(args[3]) : BytecodeMode.STRIP_FAST;
        check(!Files.exists(work), "fresh full-patch work directory");
        ResourcePatch selected = null;
        for (Patch patch : PatchBundleLoader.INSTANCE.loadFlat(Collections.singleton(mpp.toFile()))) {
            if ("広告・不要欄を非表示".equals(patch.getName())) {
                check(selected == null && patch instanceof ResourcePatch, "unique resource patch"); selected = (ResourcePatch) patch;
            }
        }
        check(selected != null, "Yahoo patch loaded");
        check(selected.getDependencies().size() == 1, "original preserve-code dependency present");
        Map<String, String> before = dex(apk);
        check(!before.isEmpty(), "original APK contains application bytecode");
        PatcherConfig config = new PatcherConfig(apk.toFile(), work.resolve("patcher").toFile(),
            Collections.emptySet(), mode, NoOpDexVerifier.INSTANCE, null, "2.3.0", false, null);
        Path output = work.resolve("YahooTransit_8.5.9_patched_unsigned.apk");
        try (Patcher patcher = new Patcher(config)) {
            patcher.plusAssign(Collections.singleton(selected));
            List<Patch> executed = new ArrayList<>();
            List<? extends PatchResult> results = BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
                (scope, continuation) -> FlowKt.toList(patcher.invoke(patch -> { executed.add(patch); return Unit.INSTANCE; }), new ArrayList<PatchResult>(), continuation));
            check(results.size() == 1, "scheduler returned selected patch result");
            check(executed.contains(selected) && executed.containsAll(selected.getDependencies()), "scheduler executed selected patch and preservation dependency");
            for (PatchResult result : results) {
                if (result.getException() != null) throw new AssertionError("Production scheduler failed: " + result.getPatch().getName(), result.getException());
            }
            patcher.writeApk(output.toFile());
        }
        check(Files.isRegularFile(output), "complete patched APK written");
        check(before.equals(dex(output)), "every original DEX filename and byte hash retained exactly");
        try (ZipFile input = new ZipFile(apk.toFile()); ZipFile patched = new ZipFile(output.toFile())) {
            check(patched.getEntry("AndroidManifest.xml") != null && patched.getEntry("resources.arsc") != null, "manifest and resources retained");
            int unchanged = 0;
            for (ZipEntry entry : Collections.list(input.entries())) {
                String name = entry.getName();
                if (entry.isDirectory() || !(name.startsWith("assets/") || name.startsWith("lib/"))) continue;
                ZipEntry outputEntry = patched.getEntry(name);
                check(outputEntry != null, "original asset/native library remains: " + name);
                try (InputStream a = input.getInputStream(entry); InputStream b = patched.getInputStream(outputEntry)) {
                    check(sha(a.readAllBytes()).equals(sha(b.readAllBytes())), "original asset/native library bytes preserved: " + name);
                }
                unchanged++;
            }
            System.out.println("PASS FullApk23Test: " + assertions + " assertions; production dependency scheduler and full APK rebuild (" + mode + "); " + before.size() + " original DEX files byte-identical; " + unchanged + " original asset/native entries byte-identical; output=" + output + "; MPP sha256=" + sha(Files.readAllBytes(mpp)) + ". Unsigned output; Android installation/UI not tested.");
        }
    }
}
