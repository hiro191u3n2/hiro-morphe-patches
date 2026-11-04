import java.io.*;
import java.util.*;
import java.util.zip.*;
import app.hiro.maps.patches.MapsCategoryRowPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Scope and compatibility audit; this is not a physical Android UI test. */
public final class MapsAudit {
    private static final String LOADER = "Lapp/hiro/maps/patches/";
    private static final String PATCH = LOADER + "MapsCategoryRowPatch;";
    private static final String OWNER = "Lafhc;";
    private static final String TARGET = "a()Landroid/view/View;";
    private static int assertions;
    private static void check(boolean ok, String message) {
        assertions++; if (!ok) throw new AssertionError(message);
    }
    private static Map<String,ClassDef> read(String path) throws Exception {
        Map<String,ClassDef> classes = new TreeMap<>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String name : dex.getDexEntryNames()) for (ClassDef c : dex.getEntry(name).getDexFile().getClasses())
            check(classes.put(c.getType(), c) == null, "duplicate class " + c.getType());
        return classes;
    }
    private static String signature(MethodReference m) {
        StringBuilder s = new StringBuilder(m.getName()).append('(');
        for (CharSequence p : m.getParameterTypes()) s.append(p);
        return s.append(')').append(m.getReturnType()).toString();
    }
    private static Method method(ClassDef c, String sig) {
        check(c != null, "class missing"); Method result = null;
        for (Method m : c.getMethods()) if (sig.equals(signature(m))) {
            check(result == null, "duplicate method " + sig); result = m;
        }
        check(result != null, "method missing " + c.getType() + sig); return result;
    }
    private static byte[] canonical(ClassDef c) throws Exception {
        MemoryDataStore store = new MemoryDataStore();
        try {
            DexPool.writeTo(store, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(c)));
            return Arrays.copyOf(store.getBuffer(), store.getSize());
        } finally { store.close(); }
    }
    private static byte[] canonical(Method m) throws Exception {
        return canonical(new ImmutableClassDef(m.getDefiningClass(), 1, "Ljava/lang/Object;", Collections.emptyList(),
            null, Collections.emptySet(), Collections.emptyList(), Collections.singleton(m)));
    }
    private static Method copy(Method m, MethodImplementation body) {
        return new ImmutableMethod(m.getDefiningClass(), m.getName(), m.getParameters(), m.getReturnType(),
            m.getAccessFlags(), m.getAnnotations(), m.getHiddenApiRestrictions(), body);
    }
    private static List<Instruction> instructions(Method m) {
        List<Instruction> list = new ArrayList<>();
        if (m.getImplementation() != null) for (Instruction i : m.getImplementation().getInstructions()) list.add(i);
        return list;
    }
    private static MethodReference called(Instruction i) {
        if (!(i instanceof ReferenceInstruction)) return null;
        Reference r = ((ReferenceInstruction)i).getReference();
        return r instanceof MethodReference ? (MethodReference)r : null;
    }
    private static void rejected(Runnable action, String message) {
        boolean rejected = false;
        try { action.run(); } catch (IllegalStateException expected) { rejected = true; }
        check(rejected, message);
    }
    /** Remove the one allowed new instruction, then prove the complete getter is unchanged. */
    private static void auditGetter(Method before, Method after) throws Exception {
        check(before.getAccessFlags() == after.getAccessFlags(), "getter flags changed");
        List<Instruction> old = instructions(before), changed = instructions(after);
        check(changed.size() == old.size() + 1, "expected exactly one additional instruction");
        int insertion = changed.size() - 2;
        Instruction injected = changed.get(insertion);
        check(injected.getOpcode() == Opcode.CONST_4 && injected instanceof OneRegisterInstruction
            && ((OneRegisterInstruction)injected).getRegisterA() == 1
            && ((NarrowLiteralInstruction)injected).getNarrowLiteral() == 0,
            "new instruction must replace returned view with null");
        check(changed.get(insertion + 1).getOpcode() == Opcode.RETURN_OBJECT
            && ((OneRegisterInstruction)changed.get(insertion + 1)).getRegisterA() == 1,
            "null must be returned immediately");
        check(old.get(old.size() - 2).getOpcode() == Opcode.MOVE_RESULT_OBJECT, "original final result missing");
        MethodReference result = called(old.get(old.size() - 3));
        check(result != null && "Lbzok;".equals(result.getDefiningClass()) && TARGET.equals(signature(result)),
            "native view result getter anchor differs");
        MutableMethodImplementation restored = new MutableMethodImplementation(after.getImplementation());
        restored.removeInstruction(insertion);
        Method normalizedBefore = copy(before, new MutableMethodImplementation(before.getImplementation()));
        check(Arrays.equals(canonical(normalizedBefore), canonical(copy(after, restored))),
            "native getter differs beyond the single null override");
    }
    private static void auditCallers(Map<String,ClassDef> classes) {
        Set<String> direct = new TreeSet<>(), wrappers = new TreeSet<>();
        for (ClassDef c : classes.values()) for (Method m : c.getMethods()) {
            List<Instruction> ins = instructions(m);
            for (int n = 0; n < ins.size(); n++) {
                MethodReference ref = called(ins.get(n));
                if (ref == null || !TARGET.equals(signature(ref))) continue;
                boolean getter = OWNER.equals(ref.getDefiningClass()), wrapper = "Lafhk;".equals(ref.getDefiningClass());
                if (!getter && !wrapper) continue;
                (getter ? direct : wrappers).add(c.getType());
                check(n + 2 < ins.size() && ins.get(n + 1).getOpcode() == Opcode.MOVE_RESULT_OBJECT,
                    "row getter result shape changed: " + c.getType());
                int reg = ((OneRegisterInstruction)ins.get(n + 1)).getRegisterA();
                if (getter && "Lafhk;".equals(c.getType())) {
                    check(ins.get(n + 2).getOpcode() == Opcode.RETURN_OBJECT
                        && ((OneRegisterInstruction)ins.get(n + 2)).getRegisterA() == reg, "wrapper no longer forwards optional view");
                } else {
                    // Two Kotlin callers jump over an alternate null assignment before the check.
                    int next = n + 2;
                    if (ins.get(next) instanceof OffsetInstruction && (ins.get(next).getOpcode() == Opcode.GOTO
                        || ins.get(next).getOpcode() == Opcode.GOTO_16 || ins.get(next).getOpcode() == Opcode.GOTO_32)) {
                        int dest = address(ins, next) + ((OffsetInstruction)ins.get(next)).getCodeOffset();
                        next = indexAt(ins, dest);
                    }
                    check(next >= 0 && ins.get(next).getOpcode() == Opcode.IF_EQZ
                        && ((OneRegisterInstruction)ins.get(next)).getRegisterA() == reg,
                        "caller does not accept null row: " + c.getType() + signature(m));
                }
            }
        }
        check(direct.equals(new TreeSet<>(Arrays.asList("Lafgr;", "Lafhk;"))), "unexpected direct row consumer");
        check(wrappers.equals(new TreeSet<>(Arrays.asList("Laqjx;", "Laeos;", "Lafyi;", "Lajwr;"))),
            "unexpected forwarded row consumer");
    }
    private static int address(List<Instruction> ins, int index) {
        int pc = 0; for (int i = 0; i < index; i++) pc += ins.get(i).getCodeUnits(); return pc;
    }
    private static int indexAt(List<Instruction> ins, int wanted) {
        int pc = 0; for (int i = 0; i < ins.size(); i++) { if (pc == wanted) return i; pc += ins.get(i).getCodeUnits(); } return -1;
    }
    private static boolean dexOrSignature(String name) {
        return name.matches("classes(?:[0-9]+)?\\.dex") || name.matches("(?i)META-INF/(?:MANIFEST\\.MF|[^/]+\\.(?:SF|RSA|DSA|EC))");
    }
    private static int auditResources(String original, String patched) throws Exception {
        int count = 0;
        try (ZipFile old = new ZipFile(original); ZipFile out = new ZipFile(patched)) {
            Set<String> expected = new TreeSet<>(), actual = new TreeSet<>();
            Enumeration<? extends ZipEntry> entries = old.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement(); if (e.isDirectory() || dexOrSignature(e.getName())) continue;
                expected.add(e.getName()); ZipEntry after = out.getEntry(e.getName());
                check(after != null && e.getSize() == after.getSize(), "resource missing/resized: " + e.getName());
                try (InputStream a = old.getInputStream(e); InputStream b = out.getInputStream(after)) {
                    byte[] aa = new byte[65536], bb = new byte[65536];
                    while (true) {
                        int ac = a.readNBytes(aa, 0, aa.length), bc = b.readNBytes(bb, 0, bb.length);
                        check(ac == bc && Arrays.equals(Arrays.copyOf(aa, ac), Arrays.copyOf(bb, bc)), "resource bytes changed: " + e.getName());
                        if (ac == 0) break;
                    }
                }
                count++;
            }
            entries = out.entries(); while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement(); if (!e.isDirectory() && !dexOrSignature(e.getName())) actual.add(e.getName());
            }
            check(actual.equals(expected), "unexpected added non-DEX entries");
        }
        return count;
    }
    private static void auditApp(String original, String patched) throws Exception {
        Map<String,ClassDef> old = read(original), out = read(patched);
        check(old.keySet().equals(out.keySet()), "APK class inventory changed");
        auditCallers(old); int preserved = 0;
        for (ClassDef before : old.values()) {
            ClassDef after = out.get(before.getType());
            if (!OWNER.equals(before.getType())) {
                check(Arrays.equals(canonical(before), canonical(after)), "unrelated class changed: " + before.getType()); preserved++;
            } else {
                Method originalGetter = method(before, TARGET), patchedGetter = method(after, TARGET);
                auditGetter(originalGetter, patchedGetter);
                // Morphe materializes all methods in this one mutable class, normalizing debug locations.
                List<Method> expectedMethods = new ArrayList<>();
                for (Method m : before.getMethods()) expectedMethods.add(TARGET.equals(signature(m)) ? patchedGetter
                    : copy(m, m.getImplementation() == null ? null : new MutableMethodImplementation(m.getImplementation())));
                ClassDef expected = new ImmutableClassDef(before.getType(), before.getAccessFlags(), before.getSuperclass(),
                    before.getInterfaces(), before.getSourceFile(), before.getAnnotations(), before.getFields(), expectedMethods);
                check(Arrays.equals(canonical(expected), canonical(after)), "target class changed outside getter");
            }
        }
        int resources = auditResources(original, patched);
        System.out.println("PASS APK audit: unchanged classes=" + preserved + "; modified getters=1; added instructions=1; unchanged resources="
            + resources + "; null-safe consumers=5 plus forwarding wrapper; assertions=" + assertions + "; physical Android UI not tested.");
    }
    private static void merge(String basePath, String additionPath, String outputPath) throws Exception {
        Map<String,ClassDef> base = read(basePath), addition = read(additionPath), merged = new TreeMap<>(base);
        check(addition.containsKey(PATCH), "Maps provider missing");
        for (String owner : base.keySet()) check(!owner.startsWith(LOADER), "Maps 1.0.0 must add, not replace, a provider");
        for (ClassDef c : addition.values()) {
            check(c.getType().startsWith(LOADER), "addition outside Maps provider namespace");
            check(merged.put(c.getType(), c) == null, "provider collision");
        }
        DexPool.writeTo(outputPath, new ImmutableDexFile(Opcodes.getDefault(), merged.values()));
        Map<String,ClassDef> written = read(outputPath);
        check(written.keySet().equals(merged.keySet()), "merged class inventory differs");
        for (ClassDef c : merged.values()) check(Arrays.equals(canonical(c), canonical(written.get(c.getType()))), "loader class changed " + c.getType());
        System.out.println("PASS loader merge: existing classes preserved=" + base.size() + "; new Maps provider classes=" + addition.size() + "; assertions=" + assertions);
    }
    private static void guards(String apk) throws Exception {
        MapsCategoryRowPatch.validatePackage("org.ungoogled.android.apps.maps.microg", "26.36.04.973607363", "1068763346");
        rejected(() -> MapsCategoryRowPatch.validatePackage("com.google.android.apps.maps", "26.36.04.973607363", "1068763346"), "wrong package accepted");
        rejected(() -> MapsCategoryRowPatch.validatePackage("org.ungoogled.android.apps.maps.microg", "26.36.05", "1068763346"), "wrong version accepted");
        rejected(() -> MapsCategoryRowPatch.validatePackage("org.ungoogled.android.apps.maps.microg", "26.36.04.973607363", "1068763347"), "wrong code accepted");
        rejected(() -> MapsCategoryRowPatch.validateClass(null), "missing class accepted");
        if (apk != null) {
            Map<String,ClassDef> classes = read(apk); auditCallers(classes);
            ClassDef c = classes.get(OWNER); Method original = ImmutableMethod.of(MapsCategoryRowPatch.validateClass(c));
            MutableClass changed = new MutableClass(c);
            check(MapsCategoryRowPatch.apply(changed, original) == 1, "expected one edit");
            auditGetter(original, method(changed, TARGET));
            rejected(() -> MapsCategoryRowPatch.validateClass(changed), "already patched getter accepted");
            rejected(() -> MapsCategoryRowPatch.apply(changed, original), "repeated apply accepted");
            MutableClass corrupted = new MutableClass(c);
            for (MutableMethod m : corrupted.getMethods()) if (TARGET.equals(signature(m))) m.getImplementation().addInstruction(0, new BuilderInstruction10x(Opcode.NOP));
            rejected(() -> MapsCategoryRowPatch.validateClass(corrupted), "unknown body accepted");
            rejected(() -> MapsCategoryRowPatch.apply(corrupted, original), "unknown mutable body accepted");
            rejected(() -> MapsCategoryRowPatch.apply(null, original), "missing mutable owner accepted");
        }
        System.out.println("PASS guards: package/version/code/body and repeated-application checks; assertions=" + assertions);
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 4 && "merge".equals(args[0])) merge(args[1], args[2], args[3]);
        else if (args.length == 3 && "audit-app".equals(args[0])) auditApp(args[1], args[2]);
        else if (args.length >= 1 && args.length <= 2 && "guard-tests".equals(args[0])) guards(args.length == 2 ? args[1] : null);
        else if (args.length == 2 && "fingerprint".equals(args[0])) {
            ClassDef c = read(args[1]).get(OWNER);
            System.out.println("original=" + MapsCategoryRowPatch.fingerprint(method(c, TARGET)));
            System.out.println("mutable=" + MapsCategoryRowPatch.fingerprint(method(new MutableClass(c), TARGET)));
        } else throw new IllegalArgumentException("guard-tests [APK] | merge BASE ADDITION OUT | audit-app ORIGINAL PATCHED | fingerprint APK");
    }
}
