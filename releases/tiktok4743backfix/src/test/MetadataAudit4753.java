/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import app.morphe.patcher.patch.*;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/** Load every shipped TikTok entry using the real Morphe JVM loader. */
public final class MetadataAudit4753 {
    static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    static boolean accepts(Patch<?> patch, String pkg, String version) {
        if (patch.getCompatibility() == null) return true;
        for (Compatibility c : patch.getCompatibility()) if (pkg.equals(c.getPackageName())) {
            if (c.getTargets() == null) return true;
            for (AppTarget t : c.getTargets()) if (t.getVersion() == null || version.equals(t.getVersion())) return true;
        }
        return false;
    }
    static void checkDependencies(Patch<?> patch, Set<Patch<?>> seen) {
        if (!seen.add(patch)) return;
        check(accepts(patch, "com.zhiliaoapp.musically", "47.4.3"), "Supported input blocked by dependency " + patch);
        check(!accepts(patch, "com.zhiliaoapp.musically", "47.5.3"), "Removed version still accepted by dependency " + patch);
        check(!accepts(patch, "com.ss.android.ugc.trill", "47.5.3"), "Removed version still accepted for trill " + patch);
        for (Patch<?> dependency : patch.getDependencies()) checkDependencies(dependency, seen);
    }
    public static void main(String[] args) throws Exception {
        Path source = Paths.get(args[0]).toRealPath();
        Set<String> seen = new TreeSet<>();
        int nativeCount = 0, backCount = 0, japaneseCount = 0;
        for (Patch<?> patch : PatchKt.loadPatchesFromJar(Collections.singleton(source.toFile()))) {
            String name = patch.getName();
            if (name == null) continue;
            boolean nativePatch = name.endsWith("（TikTok 47.4.3）");
            boolean back = name.equals("戻る1回で終了・履歴削除");
            boolean japanese = name.equals("TikTok設定を日本語化");
            if (!nativePatch && !back && !japanese) continue;
            check(seen.add(name), "Duplicate entry " + name);
            check(patch.getDefault() == !japanese, "Unexpected default for " + name);
            Set<String> packages = new TreeSet<>();
            Set<String> globalVersions = new TreeSet<>();
            for (Compatibility compatibility : patch.getCompatibility()) {
                packages.add(compatibility.getPackageName());
                if ("com.zhiliaoapp.musically".equals(compatibility.getPackageName()) && compatibility.getTargets() != null)
                    for (AppTarget target : compatibility.getTargets())
                        if (target.getVersion() != null) globalVersions.add(target.getVersion());
            }
            check(packages.contains("com.zhiliaoapp.musically"), "Missing TikTok scope");
            checkDependencies(patch, Collections.newSetFromMap(new IdentityHashMap<Patch<?>, Boolean>()));
            if (nativePatch) {
                nativeCount++;
                check(packages.equals(Collections.singleton("com.zhiliaoapp.musically")), "Unverified package scope");
                check(globalVersions.equals(Collections.singleton("47.4.3")), "Native patches must target only 47.4.3");
            }
            if (back) { backCount++; check(globalVersions.equals(new TreeSet<String>(Arrays.asList("46.2.3", "47.4.3"))), "OneBack must retain only legacy 46.2.3 and 47.4.3 for TikTok"); }
            if (japanese) {
                japaneseCount++;
                check(packages.equals(Collections.singleton("com.zhiliaoapp.musically")) && globalVersions.equals(Collections.singleton("47.4.3")), "Japanese settings must target only global 47.4.3");
            }
            boolean actionFound = false;
            for (String fieldName : new String[]{"executeBlock", "finalizeBlock"}) {
                Field field = Patch.class.getDeclaredField(fieldName); field.setAccessible(true);
                Object block = field.get(patch);
                if (block == null) continue;
                check(Paths.get(block.getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                      .toRealPath().equals(source), "Patch action was not loaded from shipped MPP: " + name);
                actionFound = true;
            }
            check(actionFound, "No executable action " + name);
        }
        check(nativeCount == 6 && backCount == 1 && japaneseCount == 1,
            "Expected all eight authored TikTok entries: " + seen);
        System.out.println("PASS official loader: native=6; OneBack=1; Japanese=1; 47.4.3 accepted and 47.5.3 rejected by all entries and dependencies; all actions shipped");
    }
}
