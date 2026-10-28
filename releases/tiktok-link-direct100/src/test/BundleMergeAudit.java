/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import java.io.File;
import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Add new loader definitions; never replace or normalize any baseline class. */
public final class BundleMergeAudit {
    static final String PREFIX = "Lapp/hiro/tiktoklink/patches/";
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static Map<String,ClassDef> read(String path) throws Exception {
        Map<String,ClassDef> result = new TreeMap<>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String entry : dex.getDexEntryNames())
            for (ClassDef c : dex.getEntry(entry).getDexFile().getClasses())
                check(result.put(c.getType(), c) == null, "Duplicate loader: " + c.getType());
        return result;
    }
    static byte[] canonical(Collection<? extends ClassDef> classes) throws Exception {
        MemoryDataStore store = new MemoryDataStore();
        try {
            DexPool.writeTo(store, new ImmutableDexFile(Opcodes.getDefault(), classes));
            return Arrays.copyOf(store.getBuffer(), store.getSize());
        } finally { store.close(); }
    }
    static void same(Map<String,ClassDef> expected, Map<String,ClassDef> actual, String what) throws Exception {
        check(expected.keySet().equals(actual.keySet()), what + " class inventory changed");
        check(Arrays.equals(canonical(expected.values()), canonical(actual.values())), what + " definitions changed");
    }
    public static void main(String[] args) throws Exception {
        check(args.length == 4, "BASE_DEX NEW_DEX COMBINED_DEX SINGLE_DEX");
        Map<String,ClassDef> base = read(args[0]), fresh = read(args[1]);
        check(!base.isEmpty() && !fresh.isEmpty(), "Missing baseline or new loader");
        Map<String,ClassDef> merged = new TreeMap<>(base);
        for (ClassDef c : fresh.values()) {
            check(c.getType().startsWith(PREFIX), "Unexpected new loader namespace: " + c.getType());
            check(merged.put(c.getType(), c) == null, "Existing loader collision: " + c.getType());
        }
        Files.write(Paths.get(args[2]), canonical(merged.values()));
        Files.write(Paths.get(args[3]), canonical(fresh.values()));
        Map<String,ClassDef> roundtrip = read(args[2]);
        same(merged, roundtrip, "Combined loader");
        Map<String,ClassDef> oldOutput = new TreeMap<>(roundtrip);
        oldOutput.keySet().retainAll(base.keySet());
        same(base, oldOutput, "Every baseline loader including debug/annotations");
        Map<String,ClassDef> newOutput = new TreeMap<>(roundtrip);
        newOutput.keySet().retainAll(fresh.keySet());
        same(fresh, newOutput, "Added aggregate loader");
        same(fresh, read(args[3]), "Standalone loader");
        System.out.println("PASS BundleMergeAudit baseline=" + base.size() + " added=" + fresh.size()
            + " combined=" + merged.size() + " every_existing_definition_preserved=true");
    }
}
