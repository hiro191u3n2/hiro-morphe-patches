import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.MultiDexContainer;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction21s;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/**
 * Make only existing Berry selection metadata version-independent, retaining
 * every runtime version, package, class and method-contract check. Android DEX
 * is authoritative: the Berry-only output is converted to JVM classes for the
 * Manager host, rather than introducing a different patch implementation.
 *
 * Args: baseline.dex new-patch.dex combined.dex berry-only.dex
 */
public final class BerryBundleDex {
    private static final Set<String> PROVIDERS = new TreeSet<>(Arrays.asList(
        "Lapp/hiro/berry/patches/ForegroundTabsPatch;",
        "Lapp/hiro/berryshare/patches/BrowserVisibilityPatch;",
        "Lapp/hiro/berryzoom/patches/OneHandZoomPatch;"));
    private static final String NEW_PREFIX = "Lapp/hiro/berryqueue/patches/";
    private static int assertions;

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static Map<String, ClassDef> read(String path) throws Exception {
        Map<String, ClassDef> result = new TreeMap<>();
        MultiDexContainer<? extends DexFile> container =
            DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String name : container.getDexEntryNames())
            for (ClassDef clazz : container.getEntry(name).getDexFile().getClasses())
                check(result.put(clazz.getType(), clazz) == null, "Duplicate class: " + clazz.getType());
        return result;
    }

    private static byte[] canonical(ClassDef clazz) throws Exception {
        MemoryDataStore output = new MemoryDataStore();
        try {
            DexPool.writeTo(output, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(clazz)));
            return Arrays.copyOf(output.getBuffer(), output.getSize());
        } finally { output.close(); }
    }

    private static ClassDef withMethods(ClassDef clazz, Iterable<? extends Method> methods) {
        return new ImmutableClassDef(clazz.getType(), clazz.getAccessFlags(), clazz.getSuperclass(),
            clazz.getInterfaces(), clazz.getSourceFile(), clazz.getAnnotations(), clazz.getFields(), methods);
    }

    private static String methodKey(Method method) {
        return method.getName() + method.getParameterTypes() + method.getReturnType();
    }

    private static ClassDef changeProvider(ClassDef clazz) throws Exception {
        List<Method> methods = new ArrayList<>();
        int changedMethods = 0;
        for (Method method : clazz.getMethods()) {
            if (!"<clinit>".equals(method.getName())) {
                methods.add(method);
                continue;
            }
            check(method.getParameterTypes().isEmpty() && "V".equals(method.getReturnType()),
                "Unexpected initializer signature: " + clazz.getType());
            MethodImplementation implementation = method.getImplementation();
            check(implementation != null, "Initializer body missing");
            List<Instruction> instructions = new ArrayList<>();
            int changes = 0, targets = 0, compatibility = 0, packageRefs = 0;
            int oldUnits = 0, newUnits = 0;
            for (Instruction instruction : implementation.getInstructions()) {
                oldUnits += instruction.getCodeUnits();
                if (instruction instanceof ReferenceInstruction) {
                    Object ref = ((ReferenceInstruction) instruction).getReference();
                    if (ref instanceof MethodReference) {
                        MethodReference m = (MethodReference) ref;
                        if ("<init>".equals(m.getName()) && "Lapp/morphe/patcher/patch/AppTarget;".equals(m.getDefiningClass())) targets++;
                        if ("<init>".equals(m.getName()) && "Lapp/morphe/patcher/patch/Compatibility;".equals(m.getDefiningClass())) compatibility++;
                    }
                    if (ref instanceof StringReference) {
                        String value = ((StringReference) ref).getString();
                        if ("jp.ejimax.berrybrowser".equals(value)) packageRefs++;
                        if ("3.83.74".equals(value)) {
                            check(instruction.getOpcode() == Opcode.CONST_STRING || instruction.getOpcode() == Opcode.CONST_STRING_JUMBO,
                                "Version reference must be a const-string");
                            int register = ((OneRegisterInstruction) instruction).getRegisterA();
                            Instruction replacement = new ImmutableInstruction21s(Opcode.CONST_16, register, 0);
                            instructions.add(replacement);
                            newUnits += replacement.getCodeUnits();
                            // Preserve every existing branch/try/debug code address.
                            if (instruction.getCodeUnits() == 3) {
                                instructions.add(new ImmutableInstruction10x(Opcode.NOP));
                                newUnits++;
                            }
                            changes++;
                            continue;
                        }
                    }
                }
                instructions.add(instruction);
                newUnits += instruction.getCodeUnits();
            }
            check(changes == 1 && targets == 1 && compatibility == 1 && packageRefs == 1,
                "Expected exactly one Berry AppTarget version in initializer: " + clazz.getType());
            check(oldUnits == newUnits, "Initializer instruction addresses changed");
            MethodImplementation replacement = new ImmutableMethodImplementation(
                implementation.getRegisterCount(), instructions, implementation.getTryBlocks(), implementation.getDebugItems());
            methods.add(new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(),
                method.getReturnType(), method.getAccessFlags(), method.getAnnotations(), method.getHiddenApiRestrictions(), replacement));
            changedMethods++;
        }
        check(changedMethods == 1, "Expected exactly one changed initializer: " + clazz.getType());
        ClassDef changed = withMethods(clazz, methods);
        Map<String, Method> oldMethods = new TreeMap<>(), newMethods = new TreeMap<>();
        for (Method m : clazz.getMethods()) oldMethods.put(methodKey(m), m);
        for (Method m : changed.getMethods()) newMethods.put(methodKey(m), m);
        check(oldMethods.keySet().equals(newMethods.keySet()), "Existing method inventory changed");
        for (String key : oldMethods.keySet()) if (!oldMethods.get(key).getName().equals("<clinit>")) {
            check(Arrays.equals(canonical(withMethods(clazz, Collections.singleton(oldMethods.get(key)))),
                                canonical(withMethods(changed, Collections.singleton(newMethods.get(key))))),
                "Existing method changed: " + clazz.getType() + "->" + key);
        }
        return changed;
    }

    private static boolean berry(String type) {
        return type.startsWith("Lapp/hiro/berry/") || type.startsWith("Lapp/hiro/berryshare/")
            || type.startsWith("Lapp/hiro/berryzoom/") || type.startsWith(NEW_PREFIX);
    }

    public static void main(String[] args) throws Exception {
        check(args.length == 4, "Expected baseline DEX, new patch DEX, combined DEX, Berry-only DEX");
        Map<String, ClassDef> baseline = read(args[0]), added = read(args[1]), combined = new TreeMap<>(baseline);
        check(baseline.keySet().containsAll(PROVIDERS), "Missing original Berry provider");
        check(!added.isEmpty(), "New MessageQueue provider is empty");
        check(Collections.disjoint(baseline.keySet(), added.keySet()), "New patch collides with baseline class");
        for (String type : added.keySet()) check(type.startsWith(NEW_PREFIX), "New patch exceeds namespace: " + type);
        int existingBerry = 0;
        for (String type : baseline.keySet()) if (berry(type)) existingBerry++;
        check(existingBerry == 27, "Baseline Berry class inventory differs: " + existingBerry);
        for (String provider : PROVIDERS) combined.put(provider, changeProvider(baseline.get(provider)));
        combined.putAll(added);
        DexPool.writeTo(args[2], new ImmutableDexFile(Opcodes.getDefault(), combined.values()));
        Map<String, ClassDef> written = read(args[2]);
        check(written.keySet().equals(combined.keySet()), "Combined class inventory differs");
        int preserved = 0;
        for (String name : combined.keySet()) {
            check(Arrays.equals(canonical(combined.get(name)), canonical(written.get(name))),
                "Class changed during serialization: " + name);
            if (baseline.containsKey(name) && !PROVIDERS.contains(name)) {
                check(Arrays.equals(canonical(baseline.get(name)), canonical(written.get(name))),
                    "Unrelated baseline class changed: " + name);
                preserved++;
            }
        }
        Map<String, ClassDef> subset = new TreeMap<>();
        for (String name : written.keySet()) if (berry(name)) subset.put(name, written.get(name));
        check(subset.size() == 27 + added.size(), "Unexpected Berry subset inventory");
        DexPool.writeTo(args[3], new ImmutableDexFile(Opcodes.getDefault(), subset.values()));
        Map<String, ClassDef> subsetWritten = read(args[3]);
        check(subsetWritten.keySet().equals(subset.keySet()), "Berry subset class inventory changed");
        for (String name : subset.keySet()) check(Arrays.equals(canonical(subset.get(name)), canonical(subsetWritten.get(name))),
            "Berry subset differs from distributed DEX: " + name);
        System.out.println("{\"status\":\"PASS\",\"baseline_classes\":" + baseline.size()
            + ",\"changed_existing_initializers\":3,\"preserved_other_classes\":" + preserved
            + ",\"existing_berry_classes\":27,\"added_queue_classes\":" + added.size()
            + ",\"combined_classes\":" + written.size() + ",\"berry_subset_classes\":" + subsetWritten.size()
            + ",\"existing_runtime_guards_preserved\":true,\"assertions\":" + assertions + "}");
    }
}
