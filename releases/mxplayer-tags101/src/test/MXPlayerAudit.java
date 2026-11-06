import java.io.*;
import java.util.*;
import java.util.zip.*;
import app.hiro.mxplayer.patches.MXPlayerTagsPatch;
import app.hiro.mxplayer.patches.MXPlayerNativeRepair;
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

/** APK scope and compatibility checks; not an Android device/UI execution test. */
public final class MXPlayerAudit {
    private static final String LOADER = "Lapp/hiro/mxplayer/patches/";
    private static final String PATCH = LOADER + "MXPlayerTagsPatch;";
    private static final String OWNER = MXPlayerTagsPatch.OWNER;
    private static final String SUPER = MXPlayerTagsPatch.SUPER;
    private static final String ENTRY = "Lhx0;";
    private static final String TARGET = "W1(Ljava/util/List;)V";
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

    private static Reference reference(Instruction i) {
        return i instanceof ReferenceInstruction ? ((ReferenceInstruction)i).getReference() : null;
    }

    private static MethodReference called(Instruction i) {
        Reference r = reference(i); return r instanceof MethodReference ? (MethodReference)r : null;
    }

    private static void rejected(Runnable action, String message) {
        boolean rejected = false;
        try { action.run(); } catch (IllegalStateException expected) { rejected = true; }
        check(rejected, message);
    }

    /** Restore the one added return and compare the entire original method. */
    private static int auditRowMethod(Method before, Method after) throws Exception {
        check(before.getAccessFlags() == after.getAccessFlags(), "insertion-hook flags changed");
        check(before.getImplementation().getRegisterCount() == after.getImplementation().getRegisterCount(),
            "insertion-hook register count changed");
        if (MXPlayerTagsPatch.alreadyTagged(before)) {
            check(Arrays.equals(canonical(before), canonical(after)), "already-hidden tag hook changed");
            return 0;
        }
        List<Instruction> old = instructions(before), changed = instructions(after);
        check(changed.size() == old.size() + 1, "expected exactly one additional instruction");
        check(changed.get(0).getOpcode() == Opcode.RETURN_VOID, "hook must return before any original work");
        MutableMethodImplementation restored = new MutableMethodImplementation(after.getImplementation());
        restored.removeInstruction(0);
        Method normalizedBefore = copy(before, new MutableMethodImplementation(before.getImplementation()));
        check(Arrays.equals(canonical(normalizedBefore), canonical(copy(after, restored))),
            "insertion hook differs beyond the single early return");
        return 1;
    }

    /** Establish the full-row model, sole producer and both native refresh paths. */
    private static void auditRowScope(Map<String,ClassDef> classes) {
        ClassDef owner = classes.get(OWNER), entry = classes.get(ENTRY);
        check(owner != null && SUPER.equals(owner.getSuperclass()), "hook inheritance changed");
        check(entry != null && "TitleEntry.kt".equals(entry.getSourceFile()) && "LLw;".equals(entry.getSuperclass()),
            "shortcut model identity changed");
        Method fallback = method(classes.get(SUPER), TARGET);
        check(instructions(fallback).size() == 1 && instructions(fallback).get(0).getOpcode() == Opcode.RETURN_VOID,
            "superclass hook is no longer optional");
        Set<String> labels = new TreeSet<>();
        for (Instruction i : instructions(method(entry, "z(Landroid/view/View;)V"))) {
            Reference ref = reference(i);
            if (ref instanceof StringReference) labels.add(((StringReference)ref).getString());
        }
        check(labels.containsAll(Arrays.asList("Music", "Privacy", "MXShare", "Video Playlist")),
            "shortcut row labels do not match the requested UI");
        Set<String> callers = new TreeSet<>(), producers = new TreeSet<>();
        int callCount = 0, creationCount = 0;
        for (ClassDef c : classes.values()) for (Method m : c.getMethods()) {
            for (Instruction i : instructions(m)) {
                Reference ref = reference(i);
                if (i.getOpcode() == Opcode.NEW_INSTANCE && ref instanceof TypeReference
                    && ENTRY.equals(((TypeReference)ref).getType())) {
                    producers.add(c.getType() + "->" + signature(m)); creationCount++;
                }
                MethodReference call = called(i);
                if (call != null && TARGET.equals(signature(call))
                    && (SUPER.equals(call.getDefiningClass()) || OWNER.equals(call.getDefiningClass()))) {
                    check(i.getOpcode() == Opcode.INVOKE_VIRTUAL, "hook is not called virtually");
                    callers.add(c.getType() + "->" + signature(m)); callCount++;
                }
            }
        }
        check(creationCount == 1 && producers.equals(Collections.singleton(OWNER + "->" + TARGET)),
            "shortcut row has an alternate producer");
        check(callCount == 2 && callers.equals(new TreeSet<>(Arrays.asList(
            "Lhn;->doInBackground([Ljava/lang/Object;)Ljava/lang/Object;",
            SUPER + "->l2(Z)V"))), "media-list rebuild/refresh call sites changed");
        Method original = method(owner, TARGET);
        Set<String> calls = new TreeSet<>();
        boolean rowType = false;
        for (Instruction i : instructions(original)) {
            MethodReference call = called(i);
            if (call != null) calls.add(call.getDefiningClass() + "->" + signature(call));
            if (i instanceof NarrowLiteralInstruction && ((NarrowLiteralInstruction)i).getNarrowLiteral() == 20)
                rowType = true;
        }
        check(rowType, "type-20 shortcut entry marker missing");
        check(calls.equals(new TreeSet<>(Arrays.asList(
            "Ljava/util/Collection;->size()I",
            "Ljava/util/List;->get(I)Ljava/lang/Object;",
            "Ljava/util/List;->remove(I)Ljava/lang/Object;",
            "Ljava/util/List;->add(ILjava/lang/Object;)V",
            "LLw;-><init>(Landroid/net/Uri;Lcom/mxtech/videoplayer/list/MediaListFragment;I)V",
            "LON;-><init>(ILjava/lang/Object;)V"))), "hook performs other work besides title-row insertion");
    }

    private static boolean dexOrSignature(String name) {
        return name.matches("classes(?:[0-9]+)?\\.dex") || name.matches("(?i)META-INF/(?:MANIFEST\\.MF|[^/]+\\.(?:SF|RSA|DSA|EC))");
    }

    private static int auditResources(String original, String patched) throws Exception {
        int count = 0;
        try (ZipFile old = new ZipFile(original); ZipFile out = new ZipFile(patched)) {
            check(old.getEntry(MXPlayerNativeRepair.PATH) != null, "verified arm64 native split missing");
            Set<String> expected = new TreeSet<>(), actual = new TreeSet<>();
            Enumeration<? extends ZipEntry> entries = old.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement(); if (e.isDirectory() || dexOrSignature(e.getName())) continue;
                expected.add(e.getName()); ZipEntry after = out.getEntry(e.getName());
                check(after != null && e.getSize() == after.getSize(), "resource missing/resized: " + e.getName());
                if (MXPlayerNativeRepair.PATH.equals(e.getName())) {
                    try (InputStream a = old.getInputStream(e); InputStream b = out.getInputStream(after)) {
                        auditNative(a.readAllBytes(), b.readAllBytes());
                    }
                    continue;
                }
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

    private static void auditNative(byte[] before, byte[] after) throws Exception {
        String pre = MXPlayerNativeRepair.sha256(before);
        check(MXPlayerNativeRepair.ORIGINAL_SHA.equals(pre) || MXPlayerNativeRepair.REPAIRED_SHA.equals(pre),
            "unverified input native library");
        check(before.length == after.length && before.length == MXPlayerNativeRepair.SIZE, "native library resized");
        check(MXPlayerNativeRepair.REPAIRED_SHA.equals(MXPlayerNativeRepair.sha256(after)), "wrong repaired native SHA-256");
        if (MXPlayerNativeRepair.REPAIRED_SHA.equals(pre)) {
            check(Arrays.equals(before, after), "already-repaired native library changed");
            return;
        }
        int differences = 0;
        for (int i = 0; i < before.length; i++) if (before[i] != after[i]) {
            boolean known = false;
            for (int site = 0; site < MXPlayerNativeRepair.OFFSETS.length; site++)
                known |= i >= MXPlayerNativeRepair.OFFSETS[site]
                    && i < MXPlayerNativeRepair.OFFSETS[site] + MXPlayerNativeRepair.AFTER[site].length;
            check(known, "native byte changed outside verified signature/integrity decision words");
            differences++;
        }
        check(differences > 0, "native repair did not change input");
        for (int site = 0; site < MXPlayerNativeRepair.OFFSETS.length; site++) {
            int off = MXPlayerNativeRepair.OFFSETS[site];
            check(Arrays.equals(Arrays.copyOfRange(before, off, off + MXPlayerNativeRepair.BEFORE[site].length),
                MXPlayerNativeRepair.BEFORE[site]), "original native instruction differs");
            check(Arrays.equals(Arrays.copyOfRange(after, off, off + MXPlayerNativeRepair.AFTER[site].length),
                MXPlayerNativeRepair.AFTER[site]), "repaired native instruction differs");
        }
    }

    private static void auditApp(String original, String patched) throws Exception {
        Map<String,ClassDef> old = read(original), out = read(patched);
        check(old.keySet().equals(out.keySet()), "APK class inventory changed");
        auditRowScope(old); int preserved = 0, addedInstructions = 0;
        for (ClassDef before : old.values()) {
            ClassDef after = out.get(before.getType());
            if (!OWNER.equals(before.getType())) {
                check(Arrays.equals(canonical(before), canonical(after)), "unrelated class changed: " + before.getType()); preserved++;
            } else {
                Method originalHook = method(before, TARGET), patchedHook = method(after, TARGET);
                addedInstructions = auditRowMethod(originalHook, patchedHook);
                // Morphe materializes methods within this one mutable class,
                // normalizing debug locations. Executable code stays identical.
                List<Method> expectedMethods = new ArrayList<>();
                for (Method m : before.getMethods()) expectedMethods.add(TARGET.equals(signature(m)) ? patchedHook
                    : copy(m, m.getImplementation() == null ? null : new MutableMethodImplementation(m.getImplementation())));
                ClassDef expected = new ImmutableClassDef(before.getType(), before.getAccessFlags(), before.getSuperclass(),
                    before.getInterfaces(), before.getSourceFile(), before.getAnnotations(), before.getFields(), expectedMethods);
                check(Arrays.equals(canonical(expected), canonical(after)), "target class changed outside insertion hook");
            }
        }
        int resources = auditResources(original, patched);
        System.out.println("PASS APK audit: unchanged unrelated classes=" + preserved + "; tag instructions added=" + addedInstructions + "; unchanged non-target resources="
            + resources + "; three native signature/integrity fatal decisions repaired; Java startup/account/license paths unchanged; shortcut producers=1; refresh paths=2; assertions="
            + assertions + "; Android launch/playback not tested.");
    }

    private static void merge(String basePath, String additionPath, String outputPath) throws Exception {
        Map<String,ClassDef> base = read(basePath), addition = read(additionPath), merged = new TreeMap<>();
        check(addition.containsKey(PATCH), "MX Player provider missing");
        check(base.containsKey(PATCH), "MX Player 1.0.0 baseline provider missing");
        int preserved = 0, replaced = 0;
        for (ClassDef c : base.values()) {
            if (c.getType().startsWith(LOADER)) replaced++;
            else { merged.put(c.getType(), c); preserved++; }
        }
        for (ClassDef c : addition.values()) {
            check(c.getType().startsWith(LOADER), "addition outside MX Player provider namespace");
            check(merged.put(c.getType(), c) == null, "provider collision");
        }
        DexPool.writeTo(outputPath, new ImmutableDexFile(Opcodes.getDefault(), merged.values()));
        Map<String,ClassDef> written = read(outputPath);
        check(written.keySet().equals(merged.keySet()), "merged class inventory differs");
        for (ClassDef c : merged.values()) check(Arrays.equals(canonical(c), canonical(written.get(c.getType()))), "loader class changed " + c.getType());
        System.out.println("PASS loader merge: unrelated classes preserved=" + preserved + "; old MX Player provider classes replaced=" + replaced
            + "; new MX Player provider classes=" + addition.size() + "; assertions=" + assertions);
    }

    private static void guards(String apk) throws Exception {
        MXPlayerTagsPatch.validatePackage("com.mxtech.videoplayer.pro", "1.93.4", "2001002584");
        rejected(() -> MXPlayerTagsPatch.validatePackage("com.mxtech.videoplayer.ad", "1.93.4", "2001002584"), "wrong package accepted");
        rejected(() -> MXPlayerTagsPatch.validatePackage("com.mxtech.videoplayer.pro", "1.93.5", "2001002584"), "wrong version accepted");
        rejected(() -> MXPlayerTagsPatch.validatePackage("com.mxtech.videoplayer.pro", "1.93.4", "2001002585"), "wrong code accepted");
        rejected(() -> MXPlayerTagsPatch.validateClass(null), "missing class accepted");
        if (apk != null) {
            Map<String,ClassDef> classes = read(apk); auditRowScope(classes);
            ClassDef c = classes.get(OWNER); Method original = ImmutableMethod.of(MXPlayerTagsPatch.validateClass(c));
            MutableClass changed = new MutableClass(c);
            boolean tagged = MXPlayerTagsPatch.alreadyTagged(original);
            check(MXPlayerTagsPatch.apply(changed, original) == (tagged ? 0 : 1), "wrong tag edit count");
            auditRowMethod(original, method(changed, TARGET));
            Method taggedSnapshot = ImmutableMethod.of(MXPlayerTagsPatch.validateClass(changed));
            check(MXPlayerTagsPatch.alreadyTagged(taggedSnapshot), "tag-state recognition failed");
            check(MXPlayerTagsPatch.apply(changed, taggedSnapshot) == 0, "repeated tag edit was not idempotent");
            if (!tagged) rejected(() -> MXPlayerTagsPatch.apply(changed, original), "stale method snapshot accepted");
            MutableClass corrupted = new MutableClass(c);
            for (MutableMethod m : corrupted.getMethods()) if (TARGET.equals(signature(m)))
                m.getImplementation().addInstruction(0, new BuilderInstruction10x(Opcode.NOP));
            rejected(() -> MXPlayerTagsPatch.validateClass(corrupted), "unknown body accepted");
            rejected(() -> MXPlayerTagsPatch.apply(corrupted, original), "unknown mutable body accepted");
            rejected(() -> MXPlayerTagsPatch.apply(null, original), "missing mutable owner accepted");
            rejected(() -> MXPlayerTagsPatch.apply(new MutableClass(c), null), "missing original accepted");
            try (ZipFile zip = new ZipFile(apk)) {
                ZipEntry entry = zip.getEntry(MXPlayerNativeRepair.PATH);
                check(entry != null, "arm64 native split missing in guard fixture");
                byte[] nativeInput;
                try (InputStream in = zip.getInputStream(entry)) { nativeInput = in.readAllBytes(); }
                byte[] snapshot = nativeInput.clone();
                byte[] repaired = MXPlayerNativeRepair.repair(nativeInput);
                auditNative(nativeInput, repaired);
                check(Arrays.equals(nativeInput, snapshot), "native transformer mutated caller input");
                check(Arrays.equals(repaired, MXPlayerNativeRepair.repair(repaired)), "native repair not idempotent");
                byte[] damaged = nativeInput.clone(); damaged[100] ^= 1;
                rejected(() -> MXPlayerNativeRepair.repair(damaged), "unknown native library accepted");
                rejected(() -> MXPlayerNativeRepair.repair(new byte[32]), "truncated native library accepted");
                rejected(() -> MXPlayerNativeRepair.repair(null), "missing native library accepted");
            }
        }
        System.out.println("PASS guards: package/version/code/body, original/already-tagged inputs, native SHA/instruction/idempotence/corruption gates, shortcut producer and refresh paths; assertions=" + assertions);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 4 && "merge".equals(args[0])) merge(args[1], args[2], args[3]);
        else if (args.length == 3 && "audit-app".equals(args[0])) auditApp(args[1], args[2]);
        else if (args.length >= 1 && args.length <= 2 && "guard-tests".equals(args[0])) guards(args.length == 2 ? args[1] : null);
        else if (args.length == 2 && "fingerprint".equals(args[0])) {
            ClassDef c = read(args[1]).get(OWNER);
            System.out.println("original=" + MXPlayerTagsPatch.fingerprint(method(c, TARGET)));
            System.out.println("mutable=" + MXPlayerTagsPatch.fingerprint(method(new MutableClass(c), TARGET)));
        } else throw new IllegalArgumentException("guard-tests [APK] | merge BASE ADDITION OUT | audit-app ORIGINAL PATCHED | fingerprint APK");
    }
}
