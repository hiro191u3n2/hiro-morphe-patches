import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Independent, exact-scope DEX audits. These checks do not imply an Android device test. */
public final class AuroraAudit {
    private static final String LOADER = "Lapp/hiro/aurora/patches/";
    private static final String PATCH = LOADER + "AuroraFavouritesPatch;";
    private static final String RUNTIME_PREFIX = "Lapp/hiro/aurora/runtime/";
    private static final String RUNTIME = RUNTIME_PREFIX + "AuroraFavourites;";
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

    private static void merge(String basePath, String additionPath, String outputPath) throws Exception {
        Map<String, ClassDef> base = read(basePath), addition = read(additionPath), merged = new TreeMap<>(base);
        check(!addition.isEmpty() && addition.containsKey(PATCH), "new Aurora patch loader missing");
        for (ClassDef c : addition.values()) {
            check(c.getType().startsWith(LOADER), "new loader outside Aurora scope: " + c.getType());
            check(merged.put(c.getType(), c) == null, "new loader collides with baseline: " + c.getType());
        }
        DexPool.writeTo(outputPath, new ImmutableDexFile(Opcodes.getDefault(), merged.values()));
        Map<String, ClassDef> written = read(outputPath);
        check(written.keySet().equals(merged.keySet()), "merged loader inventory differs");
        for (ClassDef c : base.values()) {
            check(Arrays.equals(canonical(c), canonical(written.get(c.getType()))),
                    "existing loader changed: " + c.getType());
        }
        for (ClassDef c : addition.values()) {
            check(Arrays.equals(canonical(c), canonical(written.get(c.getType()))),
                    "new Aurora loader changed: " + c.getType());
        }
        System.out.println("PASS loader merge: every existing class unchanged=" + base.size()
                + "; Aurora loader classes added=" + addition.size() + "; assertions=" + assertions);
    }

    private static final String NAV = "Lcom/aurora/store/compose/navigation/c;";
    private static final String ACTIVITY = "Lcom/aurora/store/ComposeActivity;";
    private static final String BIND_METHOD = "a(Lx95;Lgn1;I)V";
    private static final String SELECT_METHOD = "j(Ld85;Lcom/aurora/store/compose/navigation/b;)V";
    private static final String FAVOURITE = "Lcom/aurora/store/compose/navigation/Screen$k;";

    /** Specify the three reviewed call sites independently of production patch helpers. */
    private static ClassDef expectedNavigation(ClassDef original) {
        List<Method> result = new ArrayList<>();
        int bind = 0, select = 0;
        for (Method source : original.getMethods()) {
            Method method = normalizedOriginal(source);
            if (BIND_METHOD.equals(signature(source))) {
                MutableMethodImplementation code = new MutableMethodImplementation(source.getImplementation());
                check(code.getRegisterCount() == 24 && code.getInstructions().size() == 644,
                        "reviewed NavDisplay original ABI changed");
                Instruction contextCast = code.getInstructions().get(68);
                check(contextCast.getOpcode() == Opcode.CHECK_CAST
                        && ((OneRegisterInstruction) contextCast).getRegisterA() == 4
                        && "Landroid/content/Context;".equals(((TypeReference) ((ReferenceInstruction) contextCast).getReference()).getType()),
                        "bind must follow resolved activity Context");
                Instruction nextWrite = code.getInstructions().get(69);
                check(nextWrite.getOpcode() == Opcode.SGET_OBJECT
                        && ((OneRegisterInstruction) nextWrite).getRegisterA() == 7,
                        "scratch v7 must be overwritten by the next original instruction before any use");
                code.addInstruction(69, new BuilderInstruction21c(Opcode.SGET_OBJECT, 7,
                        new ImmutableFieldReference(FAVOURITE, "INSTANCE", FAVOURITE)));
                code.addInstruction(70, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 3, 4, 2, 7, 0, 0,
                        new ImmutableMethodReference(RUNTIME, "bind", Arrays.asList("Landroid/content/Context;", "Ljava/util/List;", "Ljava/lang/Object;"), "V")));
                method = copy(source, source.getName(), source.getAccessFlags(), code);
                bind++;
            } else if (SELECT_METHOD.equals(signature(source))) {
                MutableMethodImplementation code = new MutableMethodImplementation(source.getImplementation());
                check(code.getRegisterCount() == 4 && code.getInstructions().get(17).getOpcode() == Opcode.INVOKE_DIRECT,
                        "reviewed success-navigation constructor changed");
                MethodReference constructor = called(code.getInstructions().get(17));
                check(constructor != null && "Lcom/aurora/store/compose/navigation/Screen$p;".equals(constructor.getDefiningClass())
                        && "<init>".equals(constructor.getName()), "select must follow Main construction after successful authentication");
                code.addInstruction(18, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2, 2, 0, 0, 0, 0,
                        new ImmutableMethodReference(RUNTIME, "selectMain", Arrays.asList("Ljava/util/List;", "Ljava/lang/Object;"), "Ljava/lang/Object;")));
                code.addInstruction(19, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
                code.addInstruction(20, new BuilderInstruction21c(Opcode.CHECK_CAST, 0, new ImmutableTypeReference("Lx95;")));
                method = copy(source, source.getName(), source.getAccessFlags(), code);
                select++;
            }
            result.add(method);
        }
        check(bind == 1 && select == 1, "exactly two original navigation methods must receive hooks");
        return new ImmutableClassDef(original.getType(), original.getAccessFlags(), original.getSuperclass(),
                original.getInterfaces(), original.getSourceFile(), original.getAnnotations(), original.getFields(), result);
    }

    private static final String START_ACTIVITY = "startActivityForResult(Landroid/content/Intent;ILandroid/os/Bundle;)V";
    private static final String START_SENDER = "startIntentSenderForResult(Landroid/content/IntentSender;ILandroid/content/Intent;IIILandroid/os/Bundle;)V";
    private static final Set<String> ADDED_ACTIVITY = new TreeSet<>(Arrays.asList(
            "onRestart()V", "onResume()V", START_ACTIVITY, START_SENDER));

    private static Method lifecycle(String name, String helper) {
        MutableMethodImplementation code = new MutableMethodImplementation(1);
        code.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_SUPER, 1, 0, 0, 0, 0, 0,
                new ImmutableMethodReference("Lyk3;", name, Collections.<String>emptyList(), "V")));
        code.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, 0, 0, 0, 0, 0,
                new ImmutableMethodReference(RUNTIME, helper, Collections.singletonList("Landroid/app/Activity;"), "V")));
        code.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        return new ImmutableMethod(ACTIVITY, name, Collections.emptyList(), "V", "onResume".equals(name) ? 1 : 4,
                Collections.emptySet(), Collections.emptySet(), code);
    }

    private static Method outbound(String name, List<String> parameters, boolean range) {
        int registers = parameters.size() + 1;
        MutableMethodImplementation code = new MutableMethodImplementation(registers);
        ImmutableMethodReference parent = new ImmutableMethodReference("Lyk3;", name, parameters, "V");
        if (range) code.addInstruction(new BuilderInstruction3rc(Opcode.INVOKE_SUPER_RANGE, 0, registers, parent));
        else code.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_SUPER, 4, 0, 1, 2, 3, 0, parent));
        code.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, 0, 0, 0, 0, 0,
                new ImmutableMethodReference(RUNTIME, "external", Collections.singletonList("Landroid/app/Activity;"), "V")));
        code.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        List<ImmutableMethodParameter> params = new ArrayList<>();
        for (String type : parameters) params.add(new ImmutableMethodParameter(type, Collections.emptySet(), null));
        return new ImmutableMethod(ACTIVITY, name, params, "V", 1,
                Collections.emptySet(), Collections.emptySet(), code);
    }

    private static ClassDef expectedActivity(ClassDef original) {
        List<Method> result = new ArrayList<>();
        for (Method method : original.getMethods()) {
            check(!ADDED_ACTIVITY.contains(signature(method)), "original unexpectedly defines new callback: " + signature(method));
            result.add(normalizedOriginal(method));
        }
        check("Lyk3;".equals(original.getSuperclass()), "reviewed ComposeActivity superclass changed");
        result.add(lifecycle("onRestart", "restart"));
        result.add(lifecycle("onResume", "resumed"));
        result.add(outbound("startActivityForResult", Arrays.asList("Landroid/content/Intent;", "I", "Landroid/os/Bundle;"), false));
        result.add(outbound("startIntentSenderForResult", Arrays.asList("Landroid/content/IntentSender;", "I", "Landroid/content/Intent;", "I", "I", "I", "Landroid/os/Bundle;"), true));
        return new ImmutableClassDef(original.getType(), original.getAccessFlags(), original.getSuperclass(),
                original.getInterfaces(), original.getSourceFile(), original.getAnnotations(), original.getFields(), result);
    }

    private static final Map<String, Set<String>> ACCESS_ONLY;
    static {
        Map<String, Set<String>> changed = new TreeMap<>();
        changed.put("Lbb3;", Collections.singleton("onResume()V"));
        changed.put("Ly61;", new TreeSet<>(Arrays.asList(START_ACTIVITY, START_SENDER)));
        ACCESS_ONLY = Collections.unmodifiableMap(changed);
    }

    /** Keep each original superclass body, handler, annotation and field; remove only FINAL. */
    private static ClassDef expectedAncestor(ClassDef original) {
        Set<String> expected = ACCESS_ONLY.get(original.getType()), seen = new TreeSet<>();
        check(expected != null, "unreviewed ancestor access change");
        List<Method> result = new ArrayList<>();
        for (Method source : original.getMethods()) {
            Method normalized = normalizedOriginal(source);
            int access = source.getAccessFlags();
            if (expected.contains(signature(source))) {
                check(access == 17, "reviewed original superclass method must be public final: " + source);
                access &= ~16;
                seen.add(signature(source));
            }
            result.add(copy(normalized, source.getName(), access, normalized.getImplementation()));
        }
        check(seen.equals(expected), "missing reviewed superclass declaration");
        return new ImmutableClassDef(original.getType(), original.getAccessFlags(), original.getSuperclass(),
                original.getInterfaces(), original.getSourceFile(), original.getAnnotations(), original.getFields(), result);
    }

    private static int visibility(int access) {
        if ((access & 1) != 0) return 3;
        if ((access & 4) != 0) return 2;
        if ((access & 2) != 0) return 0;
        return 1;
    }

    /**
     * DEX writing alone does not check ART's final-method and visibility rules.
     * Walk the actual complete application superclass chain for all four new overrides.
     * Platform Activity declares these four extension points; the chain must reach it.
     */
    private static void hierarchy(Map<String, ClassDef> classes, boolean requireOverrides) {
        ClassDef activity = classes.get(ACTIVITY);
        check(activity != null, "ComposeActivity absent");
        Map<String, Method> declared = methods(activity);
        int inherited = 0;
        for (String signature : ADDED_ACTIVITY) {
            Method added = declared.get(signature);
            if (requireOverrides) check(added != null, "added override absent: " + signature);
            int overrideAccess = "onRestart()V".equals(signature) ? 4 : 1;
            if (added != null) {
                check(added.getAccessFlags() == overrideAccess, "new override visibility/access differs: " + signature);
                check((added.getAccessFlags() & (2 | 8 | 16 | 256 | 1024)) == 0,
                        "new callback must be overridable concrete instance method: " + signature);
            }
            Set<String> visited = new TreeSet<>();
            String owner = activity.getSuperclass();
            while (!"Landroid/app/Activity;".equals(owner)) {
                check(owner != null && visited.add(owner), "incomplete/cyclic Activity hierarchy");
                ClassDef ancestor = classes.get(owner);
                check(ancestor != null, "non-platform ancestor missing from APK: " + owner);
                Method parent = methods(ancestor).get(signature);
                if (parent != null) {
                    int flags = parent.getAccessFlags();
                    check((flags & 16) == 0, "illegal override of final superclass method: " + owner + "->" + signature);
                    check((flags & (2 | 8)) == 0, "super target must be accessible instance method: " + owner + "->" + signature);
                    check(visibility(overrideAccess) >= visibility(flags),
                            "override reduces inherited visibility: " + owner + "->" + signature);
                    inherited++;
                }
                owner = ancestor.getSuperclass();
            }
        }
        check(inherited >= 3, "expected inherited callback declarations not found");
    }

    private static void auditApp(String originalPath, String outputPath, String runtimePath) throws Exception {
        Map<String, ClassDef> original = read(originalPath), output = read(outputPath), runtime = read(runtimePath);
        check(runtime.containsKey(RUNTIME), "compiled AuroraFavourites runtime missing");
        check(original.containsKey(NAV) && original.containsKey(ACTIVITY), "reviewed Aurora owners missing");
        Set<String> inventory = new TreeSet<>(original.keySet());
        for (ClassDef clazz : runtime.values()) {
            check(clazz.getType().startsWith(RUNTIME_PREFIX), "unrelated runtime class: " + clazz.getType());
            check(inventory.add(clazz.getType()), "runtime collides with original APK");
            check(Arrays.equals(canonical(clazz), canonical(output.get(clazz.getType()))), "compiled runtime changed during application");
        }
        check(output.keySet().equals(inventory), "APK classes added or removed outside runtime");
        int preserved = 0;
        for (ClassDef before : original.values()) {
            ClassDef expected = before;
            if (NAV.equals(before.getType())) expected = expectedNavigation(before);
            else if (ACTIVITY.equals(before.getType())) expected = expectedActivity(before);
            else if (ACCESS_ONLY.containsKey(before.getType())) expected = expectedAncestor(before);
            else preserved++;
            check(Arrays.equals(canonical(expected), canonical(output.get(before.getType()))),
                    "APK class differs from independent exact reviewed transform: " + before.getType());
        }
        hierarchy(output, true);
        int calls = 0;
        for (ClassDef clazz : output.values()) if (!clazz.getType().startsWith(RUNTIME_PREFIX)) {
            for (Method method : clazz.getMethods()) if (method.getImplementation() != null) {
                for (Instruction instruction : method.getImplementation().getInstructions()) {
                    MethodReference ref = called(instruction);
                    if (ref == null || !ref.getDefiningClass().startsWith(RUNTIME_PREFIX)) continue;
                    check(RUNTIME.equals(ref.getDefiningClass()), "unknown runtime method owner");
                    String where = clazz.getType() + "->" + signature(method);
                    check((NAV + "->" + BIND_METHOD).equals(where) && "bind".equals(ref.getName())
                            || (NAV + "->" + SELECT_METHOD).equals(where) && "selectMain".equals(ref.getName())
                            || (ACTIVITY + "->onRestart()V").equals(where) && "restart".equals(ref.getName())
                            || (ACTIVITY + "->onResume()V").equals(where) && "resumed".equals(ref.getName())
                            || ((ACTIVITY + "->" + START_ACTIVITY).equals(where) || (ACTIVITY + "->" + START_SENDER).equals(where)) && "external".equals(ref.getName()),
                            "runtime called outside reviewed sites: " + where);
                    calls++;
                }
            }
        }
        check(calls == 6, "exactly six runtime call sites required");
        System.out.println("PASS APK audit: unchanged non-target classes=" + preserved
                + "; exact navigation/lifecycle modifications=6; superclass access-only changes=3; ART final/visibility hierarchy=PASS; compiled runtime classes=" + runtime.size()
                + "; assertions=" + assertions + "; physical Android behavior not tested.");
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "hierarchy".equals(args[0])) {
            hierarchy(read(args[1]), false);
            System.out.println("PASS proposed Aurora overrides have no inherited final/static/private clash or reduced visibility; assertions=" + assertions);
        } else if (args.length == 4 && "merge".equals(args[0])) {
            merge(args[1], args[2], args[3]);
        } else if (args.length == 3) {
            merge(args[0], args[1], args[2]);
        } else if (args.length == 4 && "audit-app".equals(args[0])) {
            auditApp(args[1], args[2], args[3]);
        } else {
            throw new IllegalArgumentException("merge BASEDEX NEWDEX OUTDEX | BASEDEX NEWDEX OUTDEX | audit-app ORIGINAL_APK OUTPUT_APK RUNTIME_DEX");
        }
    }
}
