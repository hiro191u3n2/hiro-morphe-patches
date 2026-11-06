import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import app.morphe.patcher.patch.Patch;
import app.morphe.patcher.patch.PatchKt;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.ResourcePatch;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.builder.BuilderInstruction;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.MethodParameter;
import com.android.tools.smali.dexlib2.iface.MultiDexContainer;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.FieldReference;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.Reference;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/**
 * Independent packaging and APK scope audit; this deliberately does not import
 * the provider under test. Compile once against morphe.jar and run the same
 * class with either morphe.jar or morphe-2.2.jar, without provider classes on
 * the host class path. These are host checks, not Android execution results.
 */
public final class PrivacyAudit {
    static final String NAMESPACE = "Lapp/hiro/swiftkeyprivacy/patches/";
    static final String CLASS_PATH = "app/hiro/swiftkeyprivacy/patches/";
    static final String PROVIDER = NAMESPACE + "SwiftKeyPrivacyPatch;";
    static final String NAME = "テレメトリ・解析・エラー報告を停止";
    static final String PACKAGE = "com.touchtype.swiftkey.beta";
    static final String VERSION = "9.13.16.4";

    // Pinned independently from the original 9.13.16.4 APK investigation.
    // Never populate this list from provider constants or from patched output.
    static final Set<String> ALLOWED_METHODS = Collections.unmodifiableSet(
        new TreeSet<String>(Arrays.asList(
            "Lwh0/j;->b(Lorg/apache/avro/generic/GenericRecord;)V",
            "Lqd0/a;->b(Ltl0/c;)Ljava/lang/Object;",
            "Lqd0/a;->a(Lqn0/f;Ltl0/c;)Ljava/lang/Object;",
            "Ln70/g;->a(Z)V",
            "Ln70/h;->run()V",
            "Lcs/w;->c()Z"
        )));

    static long checks;
    static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    static Map<String, ClassDef> read(String path) throws Exception {
        Map<String, ClassDef> result = new TreeMap<String, ClassDef>();
        MultiDexContainer<? extends DexFile> container = DexFileFactory.loadDexContainer(
            new File(path), Opcodes.getDefault());
        for (String entry : container.getDexEntryNames()) {
            for (ClassDef type : container.getEntry(entry).getDexFile().getClasses()) {
                check(result.put(type.getType(), type) == null,
                    "Duplicate DEX class: " + type.getType());
            }
        }
        check(!result.isEmpty(), "No DEX classes in " + path);
        return result;
    }

    static String signature(MethodReference method) {
        StringBuilder out = new StringBuilder(method.getName()).append('(');
        for (CharSequence parameter : method.getParameterTypes()) out.append(parameter);
        return out.append(')').append(method.getReturnType()).toString();
    }

    static String descriptor(MethodReference method) {
        return method.getDefiningClass() + "->" + signature(method);
    }

    static String owner(String descriptor) {
        return descriptor.substring(0, descriptor.indexOf("->"));
    }

    static Map<String, Method> methods(ClassDef type) {
        Map<String, Method> result = new TreeMap<String, Method>();
        for (Method method : type.getMethods()) {
            check(result.put(descriptor(method), method) == null,
                "Duplicate method: " + descriptor(method));
        }
        return result;
    }

    static byte[] canonical(ClassDef type) throws Exception {
        MemoryDataStore store = new MemoryDataStore();
        try {
            DexPool.writeTo(store, new ImmutableDexFile(
                Opcodes.getDefault(), Collections.singleton(type)));
            return Arrays.copyOf(store.getBuffer(), store.getSize());
        } finally {
            store.close();
        }
    }

    static Method copy(Method method, MethodImplementation body) {
        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
            method.getParameters(), method.getReturnType(), method.getAccessFlags(),
            method.getAnnotations(), method.getHiddenApiRestrictions(), body);
    }

    /** Remove only source/debug metadata stripped by STRIP_FAST. */
    static Method normalizedMethod(Method method) {
        List<MethodParameter> parameters = new ArrayList<MethodParameter>();
        for (MethodParameter parameter : method.getParameters()) {
            parameters.add(new ImmutableMethodParameter(
                parameter.getType(), parameter.getAnnotations(), null));
        }
        MethodImplementation body = method.getImplementation();
        if (body != null) {
            // Morphe materializes mutable methods. Apply that normalization on
            // both sides, retaining registers, instructions and try handlers.
            MutableMethodImplementation materialized = new MutableMethodImplementation(body);
            body = new ImmutableMethodImplementation(materialized.getRegisterCount(),
                materialized.getInstructions(), materialized.getTryBlocks(),
                Collections.emptyList());
        }
        return new ImmutableMethod(method.getDefiningClass(), method.getName(), parameters,
            method.getReturnType(), method.getAccessFlags(), method.getAnnotations(),
            method.getHiddenApiRestrictions(), body);
    }

    static ClassDef normalizedClass(ClassDef type, Map<String, Method> restoreBodies) {
        List<Method> result = new ArrayList<Method>();
        for (Method method : type.getMethods()) {
            Method before = restoreBodies == null ? null : restoreBodies.get(descriptor(method));
            if (before != null) method = copy(method, before.getImplementation());
            result.add(normalizedMethod(method));
        }
        return new ImmutableClassDef(type.getType(), type.getAccessFlags(), type.getSuperclass(),
            type.getInterfaces(), null, type.getAnnotations(), type.getFields(), result);
    }

    static byte[] canonicalMethod(Method method) throws Exception {
        return canonical(new ImmutableClassDef(method.getDefiningClass(), 1,
            "Ljava/lang/Object;", Collections.<String>emptyList(), null,
            Collections.emptySet(), Collections.emptyList(),
            Collections.singleton(normalizedMethod(method))));
    }

    static void registerZero(Instruction instruction, Opcode opcode, String target) {
        check(instruction.getOpcode() == opcode && instruction instanceof OneRegisterInstruction
                && ((OneRegisterInstruction) instruction).getRegisterA() == 0,
            "Incorrect opcode or return register at " + target + ": expected " + opcode + " v0");
    }

    static void nop(Instruction instruction, String target) {
        check(instruction.getOpcode() == Opcode.NOP && instruction.getCodeUnits() == 1,
            "Expected a single NOP at " + target);
    }

    static int originalCallIndex(List<? extends Instruction> instructions, String call, String target) {
        int found = -1;
        for (int index = 0; index < instructions.size(); index++) {
            Instruction instruction = instructions.get(index);
            if (!(instruction instanceof ReferenceInstruction)) continue;
            Reference reference = ((ReferenceInstruction) instruction).getReference();
            if (!(reference instanceof MethodReference)
                    || !call.equals(descriptor((MethodReference) reference))) continue;
            check(found == -1, "Ambiguous original call in " + target + ": " + call);
            check(instruction.getOpcode() == Opcode.INVOKE_STATIC
                    || instruction.getOpcode() == Opcode.INVOKE_STATIC_RANGE,
                "Original call is not static at " + target);
            found = index;
        }
        check(found >= 0, "Original call missing at " + target + ": " + call);
        return found;
    }

    /**
     * Inspect the emitted APK, then undo only the independently specified edit.
     * Equality after undo also covers every later instruction, branch target,
     * try-handler range, register, declaration and annotation in the method.
     */
    static void auditBody(Method original, Method patched) throws Exception {
        String target = descriptor(original);
        check(target.equals(descriptor(patched)) && ALLOWED_METHODS.contains(target),
            "Unexpected body-contract target: " + target);
        check(original.getImplementation() != null && patched.getImplementation() != null,
            "Missing body for " + target);
        check(original.getImplementation().getRegisterCount() == patched.getImplementation().getRegisterCount(),
            "Register count changed at " + target);
        MutableMethodImplementation restored = new MutableMethodImplementation(patched.getImplementation());
        List<BuilderInstruction> output = restored.getInstructions();
        int prefix = 0;
        if (target.equals("Lwh0/j;->b(Lorg/apache/avro/generic/GenericRecord;)V")) {
            check(output.size() >= 2 && output.get(0).getOpcode() == Opcode.RETURN_VOID
                    && output.get(0).getCodeUnits() == 1,
                "First-party recording boundary must return void immediately");
            nop(output.get(1), target);
            prefix = 2;
        } else if (target.equals("Lqd0/a;->b(Ltl0/c;)Ljava/lang/Object;")
                || target.equals("Lqd0/a;->a(Lqn0/f;Ltl0/c;)Ljava/lang/Object;")) {
            check(output.size() >= 3, "Missing uploader completion prefix: " + target);
            registerZero(output.get(0), Opcode.SGET_OBJECT, target);
            check(output.get(0) instanceof ReferenceInstruction, "Missing uploader singleton reference");
            Reference reference = ((ReferenceInstruction) output.get(0)).getReference();
            check(reference instanceof FieldReference, "Uploader completion is not a field read");
            FieldReference field = (FieldReference) reference;
            String expected = target.equals("Lqd0/a;->b(Ltl0/c;)Ljava/lang/Object;")
                ? "Lpn0/n;" : "Lnl0/f0;";
            check(expected.equals(field.getDefiningClass()) && "a".equals(field.getName())
                    && expected.equals(field.getType()),
                "Incorrect uploader success/Unit type at " + target);
            registerZero(output.get(1), Opcode.RETURN_OBJECT, target);
            nop(output.get(2), target);
            prefix = 3;
        } else if (target.equals("Lcs/w;->c()Z")) {
            check(output.size() >= 2, "Missing Crashlytics opt-out prefix");
            registerZero(output.get(0), Opcode.CONST_4, target);
            check(output.get(0) instanceof NarrowLiteralInstruction
                    && ((NarrowLiteralInstruction) output.get(0)).getNarrowLiteral() == 0,
                "Crashlytics collection predicate must return false");
            registerZero(output.get(1), Opcode.RETURN, target);
            prefix = 2;
        } else {
            String call;
            if (target.equals("Ln70/g;->a(Z)V")) {
                call = "Lcom/adjust/sdk/Adjust;->initSdk(Lcom/adjust/sdk/AdjustConfig;)V";
            } else {
                check(target.equals("Ln70/h;->run()V"), "Unknown permitted invocation edit: " + target);
                call = "Ltm0/d0;->E(Lcm0/f;)Ljava/lang/Object;";
            }
            MutableMethodImplementation input = new MutableMethodImplementation(original.getImplementation());
            List<BuilderInstruction> originalInstructions = input.getInstructions();
            int index = originalCallIndex(originalInstructions, call, target);
            check(output.size() == originalInstructions.size() && index < output.size(),
                "Invocation-only edit changed the instruction inventory at " + target);
            nop(output.get(index), target);
            if (target.equals("Ln70/h;->run()V")) {
                check(index + 1 < originalInstructions.size(), "Adjust wait has no following code");
                Opcode next = originalInstructions.get(index + 1).getOpcode();
                check(next != Opcode.MOVE_RESULT && next != Opcode.MOVE_RESULT_OBJECT
                        && next != Opcode.MOVE_RESULT_WIDE,
                    "Adjust wait return value is consumed by the original method");
            }
            restored.replaceInstruction(index, originalInstructions.get(index));
        }
        for (int index = 0; index < prefix; index++) restored.removeInstruction(0);
        check(Arrays.equals(canonicalMethod(original), canonicalMethod(copy(patched, restored))),
            "Body differs beyond its exact approved prefix/invocation edit: " + target);
    }

    /** Add the provider; every previous loader class must round-trip unchanged. */
    static void merge(String basePath, String additionPath, String outputPath) throws Exception {
        Map<String, ClassDef> base = read(basePath);
        Map<String, ClassDef> addition = read(additionPath);
        Map<String, ClassDef> combined = new TreeMap<String, ClassDef>(base);
        check(addition.containsKey(PROVIDER), "Privacy provider is missing");
        for (String type : base.keySet()) {
            check(!type.startsWith(NAMESPACE), "Baseline already has a privacy provider: " + type);
        }
        for (ClassDef type : addition.values()) {
            check(type.getType().startsWith(NAMESPACE),
                "Additional class is outside the privacy namespace: " + type.getType());
            check(combined.put(type.getType(), type) == null,
                "Provider class collision: " + type.getType());
        }
        DexPool.writeTo(outputPath, new ImmutableDexFile(Opcodes.getDefault(), combined.values()));
        Map<String, ClassDef> written = read(outputPath);
        check(written.keySet().equals(combined.keySet()), "Merged class inventory differs");
        for (ClassDef type : combined.values()) {
            check(Arrays.equals(canonical(type), canonical(written.get(type.getType()))),
                "Loader class changed during additive merge: " + type.getType());
        }
        System.out.println("PASS loader merge: preserved=" + base.size()
            + "; added=" + addition.size() + "; assertions=" + checks);
    }

    static Set<Patch<?>> load(String path) {
        return PatchKt.loadPatchesFromJar(Collections.singleton(new File(path)));
    }

    static Map<String, Set<String>> packages(Patch<?> patch) {
        Map<String, Set<String>> result = new TreeMap<String, Set<String>>();
        if (patch.getCompatiblePackages() == null) return result;
        for (Object item : patch.getCompatiblePackages()) {
            kotlin.Pair<?, ?> pair = (kotlin.Pair<?, ?>) item;
            Set<String> versions = new TreeSet<String>();
            if (pair.getSecond() != null) {
                for (Object version : (Set<?>) pair.getSecond()) versions.add((String) version);
            }
            check(result.put((String) pair.getFirst(), versions) == null,
                "Duplicate compatible package for " + patch.getName());
        }
        return result;
    }

    static Set<Patch<?>> closure(Patch<?> patch) {
        Set<Patch<?>> result = Collections.newSetFromMap(new IdentityHashMap<Patch<?>, Boolean>());
        Deque<Patch<?>> pending = new ArrayDeque<Patch<?>>();
        pending.add(patch);
        while (!pending.isEmpty()) {
            Patch<?> next = pending.removeFirst();
            if (result.add(next)) pending.addAll(next.getDependencies());
        }
        return result;
    }

    static Map<String, Integer> visibleMetadata(Collection<Patch<?>> patches, boolean omitPrivacy) {
        Map<String, Integer> result = new TreeMap<String, Integer>();
        for (Patch<?> patch : patches) {
            if (patch.getName() == null || (omitPrivacy && NAME.equals(patch.getName()))) continue;
            List<String> dependencyNames = new ArrayList<String>();
            for (Patch<?> dependency : closure(patch)) {
                if (dependency != patch && dependency.getName() != null)
                    dependencyNames.add(dependency.getName());
            }
            Collections.sort(dependencyNames);
            String key = patch.getName() + "|" + patch.getDefault() + "|"
                + packages(patch) + "|" + dependencyNames;
            Integer count = result.get(key);
            result.put(key, count == null ? 1 : count + 1);
        }
        return result;
    }

    /** Actual archive discovery and dependency selection, not provider constants. */
    static void selection(String path, String kind, String baseline) throws Exception {
        check(kind.equals("standalone") || kind.equals("integrated"),
            "Selection kind must be standalone or integrated");
        Set<Patch<?>> loaded = load(path);
        Patch<?> privacy = null;
        int visible = 0;
        int existingSwiftKey = 0;
        for (Patch<?> patch : loaded) {
            if (patch.getName() == null) continue;
            visible++;
            if (NAME.equals(patch.getName())) {
                check(privacy == null, "Privacy option is duplicated");
                privacy = patch;
            } else if (packages(patch).containsKey(PACKAGE)) {
                existingSwiftKey++;
            }
        }
        check(privacy != null, "Privacy option was not discovered by the real archive loader");
        check(!privacy.getDefault(), "Privacy option is selected by default");
        check(packages(privacy).equals(Collections.singletonMap(PACKAGE,
            Collections.singleton(VERSION))), "Privacy package/version scope differs");
        Set<Patch<?>> selected = closure(privacy);
        int resources = 0;
        int guards = 0;
        for (Patch<?> dependency : selected) {
            if (dependency == privacy) continue;
            check(dependency.getName() == null,
                "Privacy option depends on a visible patch: " + dependency.getName());
            check(dependency instanceof ResourcePatch || dependency instanceof BytecodePatch,
                "Privacy option contains an unexpected dependency kind: " + dependency.getClass().getName());
            if (dependency instanceof ResourcePatch) resources++;
            if (dependency instanceof BytecodePatch) guards++;
        }
        check(resources == 1 && guards == 1 && selected.size() == 3,
            "Privacy selection must include exactly its anonymous resource and preflight guard dependencies");
        check(privacy instanceof BytecodePatch && privacy.getDependencies().size() == 1,
            "Privacy must be a bytecode option with a single direct resource dependency");
        Patch<?> resource = privacy.getDependencies().iterator().next();
        check(resource instanceof ResourcePatch && resource.getDependencies().size() == 1,
            "Privacy resource dependency must run exactly one preflight guard");
        Patch<?> guard = resource.getDependencies().iterator().next();
        check(guard instanceof BytecodePatch && guard.getDependencies().isEmpty(),
            "Privacy preflight guard contains unexpected dependencies");
        // Selecting an existing input option must not transitively enable privacy.
        for (Patch<?> patch : loaded) {
            if (patch == privacy || patch.getName() == null) continue;
            check(!closure(patch).contains(privacy),
                "Existing option now depends on privacy: " + patch.getName());
        }
        if (kind.equals("standalone")) {
            check(visible == 1 && existingSwiftKey == 0,
                "Standalone archive contains unrelated visible options");
            try (ZipFile zip = new ZipFile(path)) {
                for (ZipEntry entry : Collections.list(zip.entries())) {
                    if (entry.getName().endsWith(".class")) {
                        check(entry.getName().startsWith(CLASS_PATH),
                            "Standalone contains an unrelated provider class: " + entry.getName());
                    }
                }
            }
        } else {
            check(existingSwiftKey > 0, "Integrated archive lost existing SwiftKey options");
            if (baseline != null) {
                Set<Patch<?>> old = load(baseline);
                for (Patch<?> patch : old)
                    check(!NAME.equals(patch.getName()), "Baseline unexpectedly contains privacy");
                check(visibleMetadata(old, false).equals(visibleMetadata(loaded, true)),
                    "Existing option names/defaults/compatibility/dependencies changed");
            }
        }
        System.out.println("PASS selection: kind=" + kind + "; name=" + NAME
            + "; default=false; dependencies=" + (selected.size() - 1)
            + "; existingSwiftKey=" + existingSwiftKey + "; visible=" + visible
            + "; baselineCompared=" + (baseline != null) + "; assertions=" + checks);
    }

    /**
     * Exact class/method/field preservation except the six independently named
     * method bodies. Each body is independently checked for the exact prescribed
     * return value or invocation removal, then restored and compared in full.
     */
    static void diff(String originalPath, String patchedPath) throws Exception {
        Map<String, ClassDef> before = read(originalPath);
        Map<String, ClassDef> after = read(patchedPath);
        check(before.keySet().equals(after.keySet()), "APK DEX class inventory changed");
        Set<String> targetOwners = new HashSet<String>();
        for (String target : ALLOWED_METHODS) targetOwners.add(owner(target));
        Set<String> changedTargets = new TreeSet<String>();
        long preservedMethods = 0;
        int preservedClasses = 0;
        for (ClassDef original : before.values()) {
            ClassDef patched = after.get(original.getType());
            Map<String, Method> restore = new TreeMap<String, Method>();
            if (targetOwners.contains(original.getType())) {
                Map<String, Method> oldMethods = methods(original);
                Map<String, Method> newMethods = methods(patched);
                check(oldMethods.keySet().equals(newMethods.keySet()),
                    "Target class method inventory changed: " + original.getType());
                for (String target : ALLOWED_METHODS) {
                    if (!owner(target).equals(original.getType())) continue;
                    Method oldMethod = oldMethods.get(target);
                    Method newMethod = newMethods.get(target);
                    check(oldMethod != null && newMethod != null, "Target method missing: " + target);
                    check(oldMethod.getImplementation() != null && newMethod.getImplementation() != null,
                        "Target method body missing: " + target);
                    check(!Arrays.equals(canonicalMethod(oldMethod), canonicalMethod(newMethod)),
                        "Required target method was not changed: " + target);
                    auditBody(oldMethod, newMethod);
                    restore.put(target, oldMethod);
                    changedTargets.add(target);
                }
            } else {
                preservedClasses++;
            }
            check(Arrays.equals(canonical(normalizedClass(original, null)),
                                canonical(normalizedClass(patched, restore))),
                "APK class differs outside the permitted method bodies: " + original.getType());
            for (Method method : original.getMethods()) {
                if (!ALLOWED_METHODS.contains(descriptor(method))) preservedMethods++;
            }
        }
        check(changedTargets.equals(ALLOWED_METHODS), "Not all approved method changes were found");
        System.out.println("PASS APK DEX preservation: classes=" + before.size()
            + "; whollyPreservedClasses=" + preservedClasses
            + "; preservedMethods=" + preservedMethods
            + "; permittedChangedBodies=" + changedTargets.size()
            + "; exactBodyContracts=" + changedTargets.size()
            + "; debugMetadataIgnored=true; assertions=" + checks);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 4 && args[0].equals("merge")) {
            merge(args[1], args[2], args[3]);
        } else if ((args.length == 3 || args.length == 4) && args[0].equals("selection")) {
            selection(args[1], args[2], args.length == 4 ? args[3] : null);
        } else if (args.length == 3 && args[0].equals("diff")) {
            diff(args[1], args[2]);
        } else {
            throw new IllegalArgumentException(
                "merge BASE_DEX ADDITION_DEX OUTPUT_DEX | selection MPP standalone|integrated [BASE_MPP]"
                + " | diff ORIGINAL_APK PATCHED_APK");
        }
    }
}
