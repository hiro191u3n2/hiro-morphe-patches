/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Audits scoped OneBack replacement and preservation of other applications. */
public final class Bundle135Audit {
    
    static final String ONEBACK = "Lapp/hiro/oneback/patches/";
    static boolean replaced(String type) {
        return type.startsWith(ONEBACK);
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
    /** Source edits move line/local debug records in otherwise unchanged nested
     * classes. Ignore ONLY MethodImplementation.debugItems for this comparison:
     * instruction encodings, registers, tries/handlers, method parameters and
     * annotations, fields/initial values, class metadata and sourceFile remain.
     * This normalized definition is never shipped; verified matches retain the
     * complete baseline ClassDef, including its original debug information. */
    static List<Method> withoutDebug(Iterable<? extends Method> methods) {
        List<Method> values = new ArrayList<>();
        for (Method method : methods) {
            MethodImplementation body = method.getImplementation();
            if (body != null) body = new ImmutableMethodImplementation(body.getRegisterCount(),
                body.getInstructions(), body.getTryBlocks(), Collections.emptyList());
            values.add(new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(),
                method.getReturnType(), method.getAccessFlags(), method.getAnnotations(),
                method.getHiddenApiRestrictions(), body));
        }
        return values;
    }
    static ClassDef withoutDebug(ClassDef value) {
        return new ImmutableClassDef(value.getType(), value.getAccessFlags(), value.getSuperclass(),
            value.getInterfaces(), value.getSourceFile(), value.getAnnotations(),
            value.getStaticFields(), value.getInstanceFields(),
            withoutDebug(value.getDirectMethods()), withoutDebug(value.getVirtualMethods()));
    }
    static boolean sameDefinition(ClassDef first, ClassDef second) throws Exception {
        return Arrays.equals(canonical(Collections.singleton(first)), canonical(Collections.singleton(second)));
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 5 && args[0].equals("merge")) {
            Map<String,ClassDef> base = read(args[1]), fresh = read(args[2]);
            Map<String,ClassDef> kept = new TreeMap<>(base), single = new TreeMap<>(fresh);
            int removed = 0;
            for (Iterator<String> it = kept.keySet().iterator(); it.hasNext();)
                if (replaced(it.next())) { it.remove(); removed++; }
            check(removed == 20, "Published baseline318 OneBack loader inventory differs");
            check(kept.size() == 282, "Published baseline318 unrelated loader inventory differs");
            for (String type : fresh.keySet()) check(type.startsWith(ONEBACK), "Unexpected new loader namespace " + type);
            Set<String> expectedLoader = new TreeSet<>(base.keySet());
            expectedLoader.removeAll(kept.keySet());
            check(expectedLoader.add(ONEBACK + "InstagramInitialProfileContract;"),
                "Initial profile contract unexpectedly exists in baseline318");
            check(fresh.size() == 21 && fresh.keySet().equals(expectedLoader),
                "Release135 must retain every existing OneBack loader and add only the initial profile contract");
            Map<String,ClassDef> merged = new TreeMap<>(kept);
            for (ClassDef c : fresh.values())
                check(merged.put(c.getType(), c) == null, "Unexpected new loader collision " + c.getType());
            Files.write(Paths.get(args[3]), canonical(merged.values()));
            Files.write(Paths.get(args[4]), canonical(single.values()));
            Map<String,ClassDef> reread = read(args[3]);
            reread.keySet().retainAll(kept.keySet());
            equal(kept, reread, "Unrelated patches");
            System.out.println("PASS Bundle135Audit loader: preserved=" + kept.size() + "; replaced=" + removed
                + "; new=" + fresh.size() + "; standalone=" + single.size());
            return;
        }
        if (args.length == 4 && args[0].equals("runtime")) {
            Map<String,ClassDef> before = read(args[1]), after = read(args[2]);
            check(before.size() == 98, "Published baseline318 OneBack runtime inventory differs");
            Map<String,ClassDef> verified = new TreeMap<>(after), preserved = new TreeMap<>();
            Set<String> changed = new TreeSet<>();
            for (String type : before.keySet()) {
                ClassDef old = before.get(type), now = after.get(type);
                check(now != null, "Runtime class removed: " + type);
                if (sameDefinition(old, now) || sameDefinition(withoutDebug(old), withoutDebug(now))) {
                    verified.put(type, old);
                    preserved.put(type, old);
                } else changed.add(type);
            }
            Set<String> added = new TreeSet<>(after.keySet()); added.removeAll(before.keySet());
            // This release changes only the registered initial external-profile
            // route handling. Preserve every other baseline318 runtime definition,
            // including its original debug records, and admit no new classes.
            Set<String> expectedChanged = Collections.singleton(
                "Lapp/hiro/oneback/runtime/InstagramExternalProfile;");
            Set<String> expectedAdded = Collections.emptySet();
            check(changed.equals(expectedChanged), "Unexpected runtime implementation changes: " + changed);
            check(added.equals(expectedAdded), "Unexpected runtime additions: " + added);
            check(after.size() == 98 && preserved.size() == 97,
                "Initial profile correction must retain 97 of 98 runtime definitions");
            byte[] output = canonical(verified.values());
            Map<String,ClassDef> reread = new TreeMap<>();
            for (ClassDef value : DexBackedDexFile.fromInputStream(Opcodes.getDefault(), new ByteArrayInputStream(output)).getClasses())
                check(reread.put(value.getType(), value) == null, "Duplicate verified runtime class " + value.getType());
            equal(verified, reread, "Verified OneBack runtime");
            reread.keySet().retainAll(preserved.keySet());
            equal(preserved, reread, "Preserved baseline OneBack runtime");
            // Only publish bytes after every comparison and round-trip passed.
            Files.write(Paths.get(args[3]), output);
            System.out.println("PASS Bundle135Audit runtime: preserved=" + (before.size()-changed.size()) + "; changed=" + changed + "; added=" + added);
            return;
        }
        throw new IllegalArgumentException("merge BASE FRESH COMBINED SINGLE | runtime OLD FRESH OUTPUT");
    }
}
