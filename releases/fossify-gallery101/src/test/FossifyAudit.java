import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Independent, exact-scope DEX audits. These checks do not imply an Android device test. */
public final class FossifyAudit {
    private static final String LOADER = "Lapp/hiro/fossifygallery/patches/";
    private static final String PATCH = LOADER + "FossifyGalleryExitPatch;";
    private static final String RUNTIME_PREFIX = "Lapp/hiro/fossifygallery/runtime/";
    private static final String RUNTIME = RUNTIME_PREFIX + "GalleryExit;";
    private static final String CALLBACK = "onBackPressedCompat()Z";
    private static final String HELPER = "hiroGallery$originalBack";
    private static final Set<String> OWNERS = new TreeSet<>(Arrays.asList(
            "Lorg/fossify/gallery/activities/MainActivity;",
            "Lorg/fossify/gallery/activities/MediaActivity;",
            "Lorg/fossify/commons/activities/BaseSimpleActivity;"));
    private static int assertions;

    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }

    private static Map<String, ClassDef> read(String path) throws Exception {
        Map<String, ClassDef> result = new TreeMap<>();
        MultiDexContainer<? extends DexFile> container =
                DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String entry : container.getDexEntryNames()) {
            for (ClassDef c : container.getEntry(entry).getDexFile().getClasses()) {
                check(result.put(c.getType(), c) == null, "duplicate DEX class: " + c.getType());
            }
        }
        return result;
    }

    /** Serializing one class at a time makes changed DEX indices irrelevant to equality. */
    private static byte[] canonical(ClassDef c) throws Exception {
        check(c != null, "missing class");
        MemoryDataStore store = new MemoryDataStore();
        try {
            DexPool.writeTo(store, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(c)));
            return Arrays.copyOf(store.getBuffer(), store.getSize());
        } finally { store.close(); }
    }

    private static byte[] canonical(Method m) throws Exception {
        return canonical(new ImmutableClassDef(m.getDefiningClass(), 1, "Ljava/lang/Object;",
                Collections.<String>emptyList(), null, Collections.<Annotation>emptySet(),
                Collections.<Field>emptyList(), Collections.singleton(m)));
    }

    private static String signature(MethodReference m) {
        StringBuilder s = new StringBuilder(m.getName()).append('(');
        for (CharSequence p : m.getParameterTypes()) s.append(p);
        return s.append(')').append(m.getReturnType()).toString();
    }

    private static Map<String, Method> methods(ClassDef c) {
        Map<String, Method> result = new TreeMap<>();
        for (Method m : c.getMethods()) {
            check(result.put(signature(m), m) == null, "duplicate method: " + c.getType() + signature(m));
        }
        return result;
    }

    private static Method copy(Method from, String name, int access, MethodImplementation implementation) {
        return new ImmutableMethod(from.getDefiningClass(), name, from.getParameters(), from.getReturnType(),
                access, from.getAnnotations(), from.getHiddenApiRestrictions(), implementation);
    }

    /**
     * Morphe materializes every method of an edited class as MutableMethodImplementation.
     * DEX debug entries may point into the middle of an instruction; materialization attaches
     * each such entry to that instruction's start. Specify that exact conversion here rather
     * than discarding debug information or weakening code/handler/metadata comparisons.
     */
    private static Method normalizedOriginal(Method original) {
        MethodImplementation implementation = original.getImplementation();
        return copy(original, original.getName(), original.getAccessFlags(),
                implementation == null ? null : new MutableMethodImplementation(implementation));
    }

    private static ClassDef normalizedOriginal(ClassDef original) {
        List<Method> normalized = new ArrayList<>();
        for (Method method : original.getMethods()) normalized.add(normalizedOriginal(method));
        return new ImmutableClassDef(original.getType(), original.getAccessFlags(), original.getSuperclass(),
                original.getInterfaces(), original.getSourceFile(), original.getAnnotations(),
                original.getFields(), normalized);
    }

    private static MethodReference called(Instruction instruction) {
        if (!(instruction instanceof ReferenceInstruction)) return null;
        Reference reference = ((ReferenceInstruction) instruction).getReference();
        return reference instanceof MethodReference ? (MethodReference) reference : null;
    }

    private static List<Instruction> instructions(Method m) {
        check(m.getImplementation() != null, "callback implementation missing: " + m);
        List<Instruction> result = new ArrayList<>();
        for (Instruction instruction : m.getImplementation().getInstructions()) result.add(instruction);
        return result;
    }

    /** This expected wrapper is specified without calling any production patch helper. */
    private static Method expectedWrapper(Method original) {
        MutableMethodImplementation code = new MutableMethodImplementation(2);
        code.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_DIRECT, 1, 1, 0, 0, 0, 0,
                new ImmutableMethodReference(original.getDefiningClass(), HELPER, Collections.<String>emptyList(), "Z")));
        code.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
        // The retained result true reaches the same final return and never invokes the exit helper.
        code.addInstruction(new BuilderInstruction21t(Opcode.IF_NEZ, 0, code.newLabelForIndex(2)));
        code.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, 1, 0, 0, 0, 0,
                new ImmutableMethodReference(RUNTIME, "tryExit", Collections.singletonList("Landroid/app/Activity;"), "Z")));
        code.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
        code.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
        code.replaceInstruction(2, new BuilderInstruction21t(Opcode.IF_NEZ, 0, code.newLabelForIndex(5)));
        return copy(original, original.getName(), original.getAccessFlags(), code);
    }

    private static void wrapperAudit(Method original, Method wrapper, Method helper) throws Exception {
        check(original.getAccessFlags() == 1, "reviewed callback must be public instance method");
        check(original.getParameterTypes().isEmpty() && "Z".equals(original.getReturnType()), "reviewed callback ABI");
        check(helper != null, "preserved original helper missing");
        Method expectedHelper = copy(original, HELPER, 0x1002, normalizedOriginal(original).getImplementation());
        check(Arrays.equals(canonical(expectedHelper), canonical(helper)),
                "renamed original differs from exact Morphe debug-address normalization: " + original.getDefiningClass());
        check(Arrays.equals(canonical(expectedWrapper(original)), canonical(wrapper)),
                "callback differs from exact reviewed wrapper: " + original.getDefiningClass());
        List<Instruction> code = instructions(wrapper);
        check(code.size() == 6 && wrapper.getImplementation().getRegisterCount() == 2, "wrapper code/register count");
        check(code.get(2).getOpcode() == Opcode.IF_NEZ, "native handled branch must bypass exit");
        int branchAddress = code.get(0).getCodeUnits() + code.get(1).getCodeUnits();
        int returnAddress = 0;
        for (int i = 0; i < 5; i++) returnAddress += code.get(i).getCodeUnits();
        check(branchAddress + ((OffsetInstruction) code.get(2)).getCodeOffset() == returnAddress,
                "native true must return its original result");
        check(called(code.get(3)) != null && "tryExit".equals(called(code.get(3)).getName()), "false path must consult task-root exit");
    }

    private static void merge(String basePath, String additionPath, String outputPath) throws Exception {
        Map<String, ClassDef> base = read(basePath), addition = read(additionPath), merged = new TreeMap<>(base);
        check(!addition.isEmpty() && addition.containsKey(PATCH), "new Fossify patch loader missing");
        check(base.containsKey(PATCH), "baseline Fossify patch loader missing");
        int removed = 0;
        for (Iterator<String> iterator = merged.keySet().iterator(); iterator.hasNext();) {
            if (iterator.next().startsWith(LOADER)) {
                iterator.remove();
                removed++;
            }
        }
        for (ClassDef c : addition.values()) {
            check(c.getType().startsWith(LOADER), "new loader outside Fossify scope: " + c.getType());
            check(merged.put(c.getType(), c) == null, "new loader collides with baseline: " + c.getType());
        }
        DexPool.writeTo(outputPath, new ImmutableDexFile(Opcodes.getDefault(), merged.values()));
        Map<String, ClassDef> written = read(outputPath);
        check(written.keySet().equals(merged.keySet()), "merged loader inventory differs");
        int preserved = 0;
        for (ClassDef c : base.values()) {
            if (c.getType().startsWith(LOADER)) continue;
            check(Arrays.equals(canonical(c), canonical(written.get(c.getType()))),
                    "existing loader changed: " + c.getType());
            preserved++;
        }
        for (ClassDef c : addition.values()) {
            check(Arrays.equals(canonical(c), canonical(written.get(c.getType()))),
                    "new Fossify loader changed: " + c.getType());
        }
        System.out.println("PASS loader merge: every class outside Fossify loader unchanged=" + preserved
                + "; previous Fossify loader classes removed=" + removed
                + "; replacement Fossify loader classes=" + addition.size() + "; assertions=" + assertions);
    }

    /** Export frozen official callback definitions as data for an independent semantic comparison. */
    private static void exportReference(String officialPath, String outputPath) throws Exception {
        Map<String, ClassDef> official = read(officialPath), reference = new TreeMap<>();
        for (String owner : OWNERS) {
            ClassDef source = official.get(owner);
            check(source != null, "official reference owner missing: " + owner);
            Method callback = methods(source).get(CALLBACK);
            check(callback != null && callback.getImplementation() != null && callback.getAccessFlags() == 1,
                    "official reference callback missing or unsupported: " + owner);
            reference.put(owner, new ImmutableClassDef(source.getType(), source.getAccessFlags(),
                    source.getSuperclass(), source.getInterfaces(), source.getSourceFile(), source.getAnnotations(),
                    Collections.<Field>emptyList(), Collections.singleton(ImmutableMethod.of(callback))));
        }
        DexPool.writeTo(outputPath, new ImmutableDexFile(Opcodes.forApi(29), reference.values()));
        Map<String, ClassDef> written = read(outputPath);
        check(written.keySet().equals(OWNERS), "reference DEX owner inventory changed");
        for (ClassDef expected : reference.values()) {
            check(Arrays.equals(canonical(expected), canonical(written.get(expected.getType()))),
                    "reference definition changed during export: " + expected.getType());
        }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Paths.get(outputPath)));
        StringBuilder hash = new StringBuilder(64);
        for (byte value : digest) {
            hash.append(Character.forDigit((value >>> 4) & 15, 16));
            hash.append(Character.forDigit(value & 15, 16));
        }
        System.out.println("PASS export-reference: exact official callback classes=" + reference.size()
                + "; sha256=" + hash + "; assertions=" + assertions);
    }

    private static void auditApp(String originalPath, String outputPath, String runtimePath) throws Exception {
        Map<String, ClassDef> original = read(originalPath), output = read(outputPath), runtime = read(runtimePath);
        check(runtime.containsKey(RUNTIME), "compiled GalleryExit runtime missing");
        check(original.keySet().containsAll(OWNERS), "reviewed Fossify callback owners missing");
        Set<String> expectedInventory = new TreeSet<>(original.keySet());
        for (ClassDef c : runtime.values()) {
            check(c.getType().startsWith(RUNTIME_PREFIX), "runtime DEX has unrelated class: " + c.getType());
            check(expectedInventory.add(c.getType()), "runtime collides with original APK: " + c.getType());
            check(Arrays.equals(canonical(c), canonical(output.get(c.getType()))),
                    "injected runtime differs from compiled runtime DEX: " + c.getType());
        }
        check(output.keySet().equals(expectedInventory), "APK classes added/removed outside reviewed runtime");
        int preserved = 0, wrappers = 0, runtimeCalls = 0;
        for (ClassDef before : original.values()) {
            ClassDef after = output.get(before.getType());
            if (!OWNERS.contains(before.getType())) {
                check(Arrays.equals(canonical(before), canonical(after)), "non-target class changed: " + before.getType());
                preserved++;
            } else {
                Map<String, Method> oldMethods = methods(before), newMethods = methods(after);
                check(!oldMethods.containsKey(HELPER + "()Z"), "helper already present in original APK");
                Set<String> expectedMethods = new TreeSet<>(oldMethods.keySet());
                expectedMethods.add(HELPER + "()Z");
                check(newMethods.keySet().equals(expectedMethods), "target class method inventory changed: " + before.getType());
                Method source = oldMethods.get(CALLBACK);
                check(source != null, "original callback missing: " + before.getType());
                wrapperAudit(source, newMethods.get(CALLBACK), newMethods.get(HELPER + "()Z"));
                List<Method> restoredMethods = new ArrayList<>();
                for (Method m : after.getMethods()) {
                    if ((HELPER + "()Z").equals(signature(m))) continue;
                    // Undo only the exact wrapper verified above, using the preserved original code.
                    if (CALLBACK.equals(signature(m))) {
                        m = copy(m, m.getName(), m.getAccessFlags(), newMethods.get(HELPER + "()Z").getImplementation());
                    }
                    restoredMethods.add(m);
                }
                ClassDef restored = new ImmutableClassDef(after.getType(), after.getAccessFlags(), after.getSuperclass(),
                        after.getInterfaces(), after.getSourceFile(), after.getAnnotations(), after.getFields(), restoredMethods);
                check(Arrays.equals(canonical(normalizedOriginal(before)), canonical(restored)),
                        "original class differs beyond exact Morphe debug-address normalization after removing reviewed hook: " + before.getType());
                wrappers++;
            }
            for (Method m : after.getMethods()) {
                if (m.getImplementation() == null) continue;
                for (Instruction instruction : m.getImplementation().getInstructions()) {
                    MethodReference reference = called(instruction);
                    if (reference != null && reference.getDefiningClass().startsWith(RUNTIME_PREFIX)) {
                        check(OWNERS.contains(after.getType()) && CALLBACK.equals(signature(m)),
                                "runtime hook outside reviewed callback: " + after.getType() + signature(m));
                        check(RUNTIME.equals(reference.getDefiningClass()) && "tryExit(Landroid/app/Activity;)Z".equals(signature(reference)),
                                "unreviewed runtime method called");
                        runtimeCalls++;
                    }
                }
            }
        }
        check(wrappers == 3 && runtimeCalls == 3, "exactly three native-result wrappers and runtime callsites required");
        System.out.println("PASS APK audit: unchanged non-target classes=" + preserved
                + "; three callbacks preserve their exact native bodies and guard only native false results"
                + "; compiled runtime classes exact=" + runtime.size() + "; assertions=" + assertions
                + "; physical Android behavior not tested.");
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 4 && "merge".equals(args[0])) {
            merge(args[1], args[2], args[3]);
        } else if (args.length == 3 && "export-reference".equals(args[0])) {
            exportReference(args[1], args[2]);
        } else if (args.length == 3) {
            merge(args[0], args[1], args[2]);
        } else if (args.length == 4 && "audit-app".equals(args[0])) {
            auditApp(args[1], args[2], args[3]);
        } else {
            throw new IllegalArgumentException("export-reference OFFICIAL_APK OUTDEX | merge BASEDEX NEWDEX OUTDEX | BASEDEX NEWDEX OUTDEX | audit-app ORIGINAL_APK OUTPUT_APK RUNTIME_DEX");
        }
    }
}
