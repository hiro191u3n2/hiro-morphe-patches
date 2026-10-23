/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Audits the exact loader definitions retained when replacing old TikTok patches. */
public final class BundleAudit {
    static final String JAPANESE = "Lapp/hiro/tiktokjapanese/patches/";
    static final String ONEBACK = "Lapp/hiro/oneback/patches/";
    static boolean replaced(String type) {
        return type.startsWith(ONEBACK) || type.startsWith("Lapp/morphe/patches/tiktok/")
            || type.startsWith("Lapp/hiro/tiktok/patches/");
    }
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static Map<String,ClassDef> read(String path) throws Exception {
        Map<String,ClassDef> values = new TreeMap<>();
        MultiDexContainer<? extends DexFile> container = DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String entry : container.getDexEntryNames())
            for (ClassDef value : container.getEntry(entry).getDexFile().getClasses())
                check(values.put(value.getType(), value) == null, "Duplicate class " + value.getType());
        return values;
    }
    static byte[] canonical(Collection<? extends ClassDef> values) throws Exception {
        MemoryDataStore store = new MemoryDataStore();
        try {
            DexPool.writeTo(store, new ImmutableDexFile(Opcodes.getDefault(), values));
            return Arrays.copyOf(store.getBuffer(), store.getSize());
        } finally { store.close(); }
    }
    static void equal(Map<String,ClassDef> first, Map<String,ClassDef> second, String why) throws Exception {
        check(first.keySet().equals(second.keySet()), why + " descriptors changed");
        check(Arrays.equals(canonical(first.values()), canonical(second.values())), why + " definitions changed");
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 5 && args[0].equals("merge")) {
            Map<String,ClassDef> base = read(args[1]), fresh = read(args[2]);
            Map<String,ClassDef> kept = new TreeMap<>(base), single = new TreeMap<>(fresh);
            int removed = 0;
            for (Iterator<String> it = kept.keySet().iterator(); it.hasNext();)
                if (replaced(it.next())) { it.remove(); removed++; }
            check(removed > 0, "No historical TikTok/OneBack loader classes replaced");
            for (ClassDef c : base.values()) if (c.getType().startsWith(JAPANESE))
                check(single.put(c.getType(), c) == null, "Japanese helper loader collided");
            Map<String,ClassDef> merged = new TreeMap<>(kept);
            for (ClassDef c : fresh.values())
                check(merged.put(c.getType(), c) == null, "Unexpected new loader collision " + c.getType());
            Files.write(Paths.get(args[3]), canonical(merged.values()));
            Files.write(Paths.get(args[4]), canonical(single.values()));
            Map<String,ClassDef> reread = read(args[3]);
            reread.keySet().retainAll(kept.keySet());
            equal(kept, reread, "Unrelated patches");
            Map<String,ClassDef> jaOld = new TreeMap<>(), jaNew = new TreeMap<>();
            for (ClassDef c : base.values()) if (c.getType().startsWith(JAPANESE)) jaOld.put(c.getType(),c);
            for (ClassDef c : read(args[4]).values()) if (c.getType().startsWith(JAPANESE)) jaNew.put(c.getType(),c);
            equal(jaOld, jaNew, "Existing Japanese patch");
            System.out.println("PASS loader merge: preserved=" + kept.size() + "; replaced=" + removed
                + "; new=" + fresh.size() + "; standalone=" + single.size() + "; japanese=" + jaOld.size());
            return;
        }
        if (args.length == 3 && args[0].equals("runtime")) {
            Map<String,ClassDef> before = read(args[1]), after = read(args[2]);
            Set<String> changed = new TreeSet<>();
            for (String type : before.keySet()) {
                ClassDef old = before.get(type), now = after.get(type);
                check(now != null, "Runtime class removed: " + type);
                if (!Arrays.equals(canonical(Collections.singleton(old)), canonical(Collections.singleton(now)))) changed.add(type);
            }
            Set<String> added = new TreeSet<>(after.keySet()); added.removeAll(before.keySet());
            check(changed.equals(Collections.singleton("Lapp/hiro/oneback/runtime/TikTokHome;")),
                "Only TikTokHome runtime should change: " + changed);
            check(added.isEmpty(), "Unexpected OneBack runtime additions " + added);
            System.out.println("PASS OneBack runtime: preserved=" + (before.size()-changed.size()) + "; changed=" + changed);
            return;
        }
        throw new IllegalArgumentException("merge BASE FRESH COMBINED SINGLE | runtime OLD NEW");
    }
}
