import java.io.File;
import java.util.*;
import app.hiro.quicksearch.patches.QuickSearchRecentsPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.BuilderInstruction;
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Exact-scope static audit and lossless bundle-DEX merge. No Android device simulation. */
public final class DexAudit {
    private static final String MAIN = "Ljp/ddo/sugihiro/quicksearch/activity/MainActivity;";
    private static final String RUNTIME = "Lapp/hiro/quicksearch/runtime/SearchTask;";
    private static final String AFTER = "actionAfterSearched()V";
    private static final String SEARCH = "search(Ljava/lang/String;Ljava/lang/String;)V";
    private static int assertions;

    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
        assertions++;
    }

    private interface Action { void run() throws Exception; }
    private static void rejected(Action action, String reason) throws Exception {
        boolean failed = false;
        try { action.run(); } catch (IllegalStateException | IllegalArgumentException expected) { failed = true; }
        check(failed, reason);
    }

    private static Map<String, ClassDef> classes(String file) throws Exception {
        Map<String, ClassDef> result = new TreeMap<>();
        MultiDexContainer<? extends DexFile> container = DexFileFactory.loadDexContainer(new File(file), Opcodes.getDefault());
        for (String entry : container.getDexEntryNames()) {
            for (ClassDef c : container.getEntry(entry).getDexFile().getClasses()) {
                check(result.put(c.getType(), c) == null, "duplicate class in input: " + c.getType());
            }
        }
        return result;
    }

    /** Each class is reserialized alone so DEX table-index changes do not hide or invent differences. */
    private static byte[] canonical(ClassDef c) throws Exception {
        check(c != null, "missing class to canonicalize");
        MemoryDataStore store = new MemoryDataStore();
        try {
            DexPool.writeTo(store, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(c)));
            return Arrays.copyOf(store.getBuffer(), store.getSize());
        } finally { store.close(); }
    }

    private static byte[] canonical(Method method) throws Exception {
        return canonical(new ImmutableClassDef(method.getDefiningClass(), 1, "Ljava/lang/Object;",
                Collections.<String>emptyList(), null, Collections.<Annotation>emptySet(),
                Collections.<Field>emptyList(), Collections.singleton(method)));
    }

    private static String signature(MethodReference method) {
        StringBuilder text = new StringBuilder(method.getName()).append('(');
        for (CharSequence p : method.getParameterTypes()) text.append(p);
        return text.append(')').append(method.getReturnType()).toString();
    }

    private static Map<String, Method> methods(ClassDef c) {
        Map<String, Method> result = new TreeMap<>();
        for (Method m : c.getMethods()) check(result.put(signature(m), m) == null, "duplicate method");
        return result;
    }

    private static MethodReference reference(String owner, String name, String... params) {
        return new ImmutableMethodReference(owner, name, Arrays.asList(params), "V");
    }

    private static MethodReference called(Instruction instruction) {
        if (!(instruction instanceof ReferenceInstruction)) return null;
        Reference r = ((ReferenceInstruction) instruction).getReference();
        return r instanceof MethodReference ? (MethodReference) r : null;
    }

    private static BuilderInstruction35c staticCall(Instruction instruction, MethodReference destination) {
        check(instruction instanceof FiveRegisterInstruction, "reviewed hook must remain invoke-35c");
        FiveRegisterInstruction old = (FiveRegisterInstruction) instruction;
        return new BuilderInstruction35c(Opcode.INVOKE_STATIC, old.getRegisterCount(), old.getRegisterC(),
                old.getRegisterD(), old.getRegisterE(), old.getRegisterF(), old.getRegisterG(), destination);
    }

    /** Expected transformation is specified here independently of the patch's matcher/redirect helpers. */
    private static MutableClass expectedClass(ClassDef source) {
        MutableClass expected = new MutableClass(source);
        int finish = 0, launch = 0;
        for (MutableMethod method : expected.getMethods()) {
            if (method.getImplementation() == null) continue;
            List<BuilderInstruction> instructions = method.getImplementation().getInstructions();
            for (int i = 0; i < instructions.size(); i++) {
                Instruction old = instructions.get(i);
                MethodReference call = called(old);
                MethodReference destination = null;
                if (AFTER.equals(signature(method)) && reference(MAIN, "finish").equals(call)) {
                    check(old.getOpcode() == Opcode.INVOKE_VIRTUAL, "original finish opcode");
                    destination = reference(RUNTIME, "finish", "Landroid/app/Activity;");
                    finish++;
                } else if (SEARCH.equals(signature(method))
                        && reference(MAIN, "startActivity", "Landroid/content/Intent;").equals(call)) {
                    check(old.getOpcode() == Opcode.INVOKE_VIRTUAL, "original launch opcode");
                    destination = reference(RUNTIME, "startExternal", "Landroid/app/Activity;", "Landroid/content/Intent;");
                    launch++;
                }
                if (destination != null) method.getImplementation().replaceInstruction(i, staticCall(old, destination));
            }
        }
        check(finish == 1 && launch == 1, "exactly one reviewed finish and external launch");
        return expected;
    }

    private static void packageGates() throws Exception {
        QuickSearchRecentsPatch.validatePackage("jp.ddo.sugihiro.quicksearch", "0.4.5", 36L);
        for (final long code : new long[]{35, 37}) {
            rejected(() -> QuickSearchRecentsPatch.validatePackage("jp.ddo.sugihiro.quicksearch", "0.4.5", code), "wrong versionCode accepted");
        }
        for (final String version : Arrays.asList("0.4.4", "0.4.6", "")) {
            rejected(() -> QuickSearchRecentsPatch.validatePackage("jp.ddo.sugihiro.quicksearch", version, 36L), "wrong versionName accepted");
        }
        rejected(() -> QuickSearchRecentsPatch.validatePackage("jp.ddo.sugihiro.other", "0.4.5", 36L), "wrong package accepted");
    }

    private static void matchingScope() {
        MethodReference after = reference(MAIN, "actionAfterSearched");
        MethodReference search = reference(MAIN, "search", "Ljava/lang/String;", "Ljava/lang/String;");
        Instruction finish = new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 4, 0, 0, 0, 0, reference(MAIN, "finish"));
        Instruction launch = new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 5, 1, 0, 0, 0,
                reference(MAIN, "startActivity", "Landroid/content/Intent;"));
        check(reference(RUNTIME, "finish", "Landroid/app/Activity;").equals(QuickSearchRecentsPatch.replacement(after, finish)), "finish destination");
        check(reference(RUNTIME, "startExternal", "Landroid/app/Activity;", "Landroid/content/Intent;")
                .equals(QuickSearchRecentsPatch.replacement(search, launch)), "external destination");
        for (MethodReference owner : Arrays.asList(reference(MAIN, "onBackPressed"),
                reference(MAIN, "search", "Ljava/lang/String;"), reference("Lother/Activity;", "actionAfterSearched"))) {
            check(QuickSearchRecentsPatch.replacement(owner, finish) == null, "unrelated finish matched");
            check(QuickSearchRecentsPatch.replacement(owner, launch) == null, "unrelated launch matched");
        }
        Instruction internal = new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL, 3, 5, 2, 1, 0, 0,
                reference(MAIN, "startActivityForResult", "Landroid/content/Intent;", "I"));
        check(QuickSearchRecentsPatch.replacement(search, internal) == null, "internal WebActivity launch matched");
    }

    private static void assertRejectedWithoutMutation(final MutableClass changed, String reason) throws Exception {
        final byte[] before = canonical(changed);
        rejected(() -> QuickSearchRecentsPatch.apply(changed), reason);
        check(Arrays.equals(before, canonical(changed)), "partial mutation after rejection: " + reason);
    }

    private static void rejectionCases(ClassDef original) throws Exception {
        MutableClass reversedExit = new MutableClass(original);
        boolean altered = false;
        for (MutableMethod method : reversedExit.getMethods()) if (AFTER.equals(signature(method))) {
            List<BuilderInstruction> list = method.getImplementation().getInstructions();
            // The final conditional controls the exit preference; reversing it must invalidate the full-method gate.
            for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).getOpcode() == Opcode.IF_EQZ) {
                BuilderInstruction old = list.get(i);
                method.getImplementation().replaceInstruction(i, new BuilderInstruction21t(Opcode.IF_NEZ,
                        ((OneRegisterInstruction) old).getRegisterA(), ((BuilderOffsetInstruction) old).getTarget()));
                altered = true;
                break;
            }
        }
        check(altered, "exit-branch fixture found");
        assertRejectedWithoutMutation(reversedExit, "reversed exit preference guard accepted");

        MutableClass changedSearch = new MutableClass(original);
        altered = false;
        for (MutableMethod method : changedSearch.getMethods()) if (SEARCH.equals(signature(method))) {
            List<BuilderInstruction> list = method.getImplementation().getInstructions();
            for (int i = 0; i < list.size(); i++) if (list.get(i).getOpcode() == Opcode.CONST_4) {
                Instruction old = list.get(i);
                method.getImplementation().replaceInstruction(i, new BuilderInstruction11n(Opcode.CONST_4,
                        ((OneRegisterInstruction) old).getRegisterA(), ((NarrowLiteralInstruction) old).getNarrowLiteral() ^ 1));
                altered = true;
                break;
            }
        }
        check(altered, "search-literal fixture found");
        assertRejectedWithoutMutation(changedSearch, "unknown search-method variant accepted");
        MutableClass alreadyPatched = new MutableClass(original);
        check(QuickSearchRecentsPatch.apply(alreadyPatched) == 2, "first application hook count");
        assertRejectedWithoutMutation(alreadyPatched, "already-patched input accepted");
    }

    private static void methodAudit(ClassDef original, ClassDef patched) throws Exception {
        Map<String, Method> before = methods(original), after = methods(patched);
        check(before.keySet().equals(after.keySet()), "MainActivity method set changed");
        int changed = 0;
        for (String key : before.keySet()) {
            boolean equal = Arrays.equals(canonical(before.get(key)), canonical(after.get(key)));
            boolean target = AFTER.equals(key) || SEARCH.equals(key);
            check(equal != target, "unexpected MainActivity method delta: " + key);
            if (target) {
                changed++;
                MethodImplementation a = before.get(key).getImplementation(), b = after.get(key).getImplementation();
                check(a.getRegisterCount() == b.getRegisterCount(), "register count changed: " + key);
                List<Instruction> oldInstructions = new ArrayList<>(), newInstructions = new ArrayList<>();
                for (Instruction i : a.getInstructions()) oldInstructions.add(i);
                for (Instruction i : b.getInstructions()) newInstructions.add(i);
                check(oldInstructions.size() == newInstructions.size(), "instruction count changed: " + key);
                int redirects = 0;
                for (int i = 0; i < oldInstructions.size(); i++) {
                    Instruction x = oldInstructions.get(i), y = newInstructions.get(i);
                    check(x.getCodeUnits() == y.getCodeUnits(), "code offsets changed: " + key);
                    MethodReference call = called(y);
                    if (call != null && RUNTIME.equals(call.getDefiningClass())) {
                        redirects++;
                        check(x.getOpcode() == Opcode.INVOKE_VIRTUAL && y.getOpcode() == Opcode.INVOKE_STATIC, "hook opcodes");
                        FiveRegisterInstruction p = (FiveRegisterInstruction) x, q = (FiveRegisterInstruction) y;
                        check(p.getRegisterCount() == q.getRegisterCount() && p.getRegisterC() == q.getRegisterC()
                                && p.getRegisterD() == q.getRegisterD() && p.getRegisterE() == q.getRegisterE()
                                && p.getRegisterF() == q.getRegisterF() && p.getRegisterG() == q.getRegisterG(), "hook register operands changed");
                    }
                }
                check(redirects == 1, "each reviewed method must contain exactly one redirect");
            }
        }
        check(changed == 2, "changed method count");
    }

    private static void wholeApkAudit(Map<String, ClassDef> original, Map<String, ClassDef> patched, ClassDef expected) throws Exception {
        check(patched.size() == original.size() + 1, "exactly one runtime class must be added");
        int preserved = 0;
        for (String type : original.keySet()) {
            check(patched.containsKey(type), "original class missing: " + type);
            if (MAIN.equals(type)) continue;
            check(Arrays.equals(canonical(original.get(type)), canonical(patched.get(type))), "non-target class changed: " + type);
            preserved++;
        }
        check(Arrays.equals(canonical(expected), canonical(patched.get(MAIN))), "final MainActivity differs from the independently specified two-invoke change");
        methodAudit(original.get(MAIN), patched.get(MAIN));
        int hooks = 0;
        for (ClassDef c : patched.values()) {
            if (!original.containsKey(c.getType())) check(RUNTIME.equals(c.getType()), "unexpected added class: " + c.getType());
            for (Method m : c.getMethods()) if (m.getImplementation() != null) {
                for (Instruction i : m.getImplementation().getInstructions()) {
                    MethodReference call = called(i);
                    if (call != null && RUNTIME.equals(call.getDefiningClass()) && !RUNTIME.equals(c.getType())) {
                        check(MAIN.equals(c.getType()) && (AFTER.equals(signature(m)) || SEARCH.equals(signature(m))), "runtime reference escaped reviewed scope");
                        hooks++;
                    }
                }
            }
        }
        check(hooks == 2, "whole-APK runtime reference count");
        System.out.println("PASS final APK: all " + preserved + " non-target classes unchanged; MainActivity exactly two invoke replacements; one runtime class added.");
    }

    private static void merge(String baseFile, String additionFile, String outputFile) throws Exception {
        Map<String, ClassDef> base = classes(baseFile), addition = classes(additionFile);
        Map<String, ClassDef> merged = new TreeMap<>(base);
        for (ClassDef c : addition.values()) check(merged.put(c.getType(), c) == null, "new bundle class collides with baseline: " + c.getType());
        DexPool.writeTo(outputFile, new ImmutableDexFile(Opcodes.getDefault(), merged.values()));
        Map<String, ClassDef> read = classes(outputFile);
        check(read.keySet().equals(merged.keySet()), "merged class inventory mismatch");
        for (ClassDef c : base.values()) check(Arrays.equals(canonical(c), canonical(read.get(c.getType()))), "baseline loader class changed: " + c.getType());
        for (ClassDef c : addition.values()) check(Arrays.equals(canonical(c), canonical(read.get(c.getType()))), "new loader class changed: " + c.getType());
        System.out.println("PASS bundle merge: every baseline loader class preserved=" + base.size() + "; added=" + addition.size());
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 4 && "merge".equals(args[0])) { merge(args[1], args[2], args[3]); return; }
        if ((args.length != 2 || !"original".equals(args[0]))
                && (args.length != 3 || !"patched".equals(args[0]))) {
            throw new IllegalArgumentException("Usage: DexAudit merge BASEDEX NEWDEX OUTDEX | original BASEAPK | patched BASEAPK PATCHEDAPK");
        }
        packageGates();
        matchingScope();
        Map<String, ClassDef> original = classes(args[1]);
        ClassDef main = original.get(MAIN);
        check(main != null, "original MainActivity missing");
        QuickSearchRecentsPatch.validateClass(main);
        MutableClass expected = expectedClass(main), actual = new MutableClass(main);
        check(QuickSearchRecentsPatch.apply(actual) == 2, "patch must replace exactly two invokes");
        check(Arrays.equals(canonical(expected), canonical(actual)), "patch result differs from independent expected class");
        methodAudit(main, actual);
        rejectionCases(main);
        if (args.length == 3) wholeApkAudit(original, classes(args[2]), expected);
        System.out.println("PASS static QA: original classes=" + original.size() + "; assertions=" + assertions
                + "; version/package gate, wrong-variant rejection, atomic double-patch rejection. Android device behavior NOT tested.");
    }
}
