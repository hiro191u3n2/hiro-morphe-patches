/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import app.morphe.patcher.patch.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/** Inspect the real Morphe loader, the new target scope, and all prior entries. */
public final class Metadata130Audit {
    static final String PREFIX = "app.hiro.oneback.patches.";
    static final String NAME = "戻る1回で終了・履歴削除";
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static boolean owned(Patch<?> patch, Path source) throws Exception {
        for (String name : new String[]{"executeBlock", "finalizeBlock"}) {
            Field f = Patch.class.getDeclaredField(name); f.setAccessible(true);
            Object action = f.get(patch);
            if (action != null && action.getClass().getName().startsWith(PREFIX)) {
                check(Paths.get(action.getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toRealPath().equals(source), "New patch action not loaded from exact shipped MPP");
                return true;
            }
        }
        return false;
    }
    static String scope(Patch<?> patch) {
        if (patch.getCompatibility() == null) return "ANY";
        List<String> parts = new ArrayList<>();
        for (Compatibility c : patch.getCompatibility()) {
            List<String> versions = new ArrayList<>();
            if (c.getTargets() == null) versions.add("ANY");
            else for (AppTarget t : c.getTargets()) versions.add(t.getVersion() == null ? "ANY" : String.valueOf(t.getVersion()));
            Collections.sort(versions); parts.add(c.getPackageName() + ":" + versions);
        }
        Collections.sort(parts); return parts.toString();
    }
    static String signature(Patch<?> patch) {
        List<String> deps = new ArrayList<>();
        for (Patch<?> dep : patch.getDependencies()) deps.add(String.valueOf(dep.getName()) + "/" + scope(dep));
        Collections.sort(deps);
        return String.valueOf(patch.getDescription()) + "|" + patch.getDefault() + "|" + scope(patch) + "|" + deps;
    }
    static Map<String,String> inventory(Path source) throws Exception {
        Map<String,String> result = new TreeMap<>(); int own = 0;
        for (Patch<?> patch : PatchKt.loadPatchesFromJar(Collections.singleton(source.toFile()))) {
            if (patch.getName() == null) continue;
            check(result.put(patch.getName(), signature(patch)) == null, "Duplicate named patch " + patch.getName());
            if (owned(patch, source)) {
                own++;
                check(NAME.equals(patch.getName()), "Existing named patch identity must be retained");
                check(patch.getDefault(), "Existing enabled default must be retained");
                check(scope(patch).equals("[com.instagram.android:[ANY], com.ss.android.ugc.trill:[46.2.3], com.twitter.android:[ANY], com.zhiliaoapp.musically:[46.2.3, 47.4.3], ctrip.english:[ANY]]"),
                    "Existing application/version admission must remain exact: " + scope(patch));
            }
        }
        check(own == 1, "Expected exactly one owned named patch, found " + own);
        return result;
    }
    public static void main(String[] args) throws Exception {
        check(args.length == 3, "BASE_MPP SINGLE_MPP COMBINED_MPP");
        Map<String,String> base = inventory(Paths.get(args[0]).toRealPath());
        Map<String,String> single = inventory(Paths.get(args[1]).toRealPath());
        Map<String,String> combined = inventory(Paths.get(args[2]).toRealPath());
        check(single.size() == 1, "Standalone must have exactly one named patch");
        Map<String,String> expected = new TreeMap<>(base);
        check(expected.remove(NAME) != null, "Prior owned patch missing from baseline");
        for (Map.Entry<String,String> e : single.entrySet())
            check(expected.put(e.getKey(), e.getValue()) == null, "Owned replacement collides with unrelated baseline entry");
        check(expected.equals(combined), "Existing patch metadata changed or standalone differs");
        check(combined.size() == base.size(), "Upgrade must not create a duplicate named patch");
        System.out.println("PASS Metadata130Audit baseline=" + base.size() + " replaced_owned=1 combined=" + combined.size()
            + " scope_preserved=true nonowned_metadata_preserved=true duplicate_owned_patch=false");
    }
}
