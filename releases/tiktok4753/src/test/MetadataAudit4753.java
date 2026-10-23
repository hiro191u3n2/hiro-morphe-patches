/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import app.morphe.patcher.patch.*;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/** Load every shipped TikTok entry using the real Morphe JVM loader. */
public final class MetadataAudit4753 {
    static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    public static void main(String[] args) throws Exception {
        Path source = Paths.get(args[0]).toRealPath();
        Set<String> seen = new TreeSet<>();
        int nativeCount = 0, backCount = 0, japaneseCount = 0;
        for (Patch<?> patch : PatchKt.loadPatchesFromJar(Collections.singleton(source.toFile()))) {
            String name = patch.getName();
            if (name == null) continue;
            boolean nativePatch = name.endsWith("（TikTok 47.5.3）");
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
            if (nativePatch) {
                nativeCount++;
                check(packages.equals(Collections.singleton("com.zhiliaoapp.musically")), "Unverified package scope");
                check(globalVersions.equals(Collections.singleton("47.5.3")), "Native patches must target verified version");
            }
            if (back) { backCount++; check(globalVersions.contains("47.5.3"), "OneBack version metadata absent"); }
            if (japanese) japaneseCount++;
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
        System.out.println("PASS official loader: native=6; OneBack=1; Japanese=1; exact native package/version; all actions shipped");
    }
}
