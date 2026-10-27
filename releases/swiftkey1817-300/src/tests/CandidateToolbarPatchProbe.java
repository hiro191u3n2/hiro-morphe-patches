package app.hiro.swiftkey.patches;

import java.nio.file.*;
import java.util.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.smali.Smali;
import com.android.tools.smali.smali.SmaliOptions;

/** Verifies real method transformations, branch ownership and DEX roundtrips. */
public final class CandidateToolbarPatchProbe {
    private static int assertions;
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;
    private static void check(boolean value, String message) {
        assertions++; if (!value) throw new AssertionError(message);
    }
    private static MutableMethod[] mutable(Method[] methods) {
        MutableMethod[] result = new MutableMethod[methods.length];
        for (int i = 0; i < result.length; i++) result[i] = new MutableMethod(methods[i]);
        return result;
    }
    private static MethodReference reference(Instruction instruction) {
        if (!(instruction instanceof ReferenceInstruction)) return null;
        Object reference = ((ReferenceInstruction) instruction).getReference();
        return reference instanceof MethodReference ? (MethodReference) reference : null;
    }
    private static boolean bridge(Instruction instruction) {
        MethodReference reference = reference(instruction);
        return reference != null && (reference.getDefiningClass().equals("Lhiro/swiftkey/CandidateToolbar;")
                || reference.getDefiningClass().equals("Lhiro/swiftkey/EmojiToolbar;"));
    }
    private static int receiver(Method method) {
        int words = 1;
        for (CharSequence type : method.getParameterTypes()) words += "J".contentEquals(type) || "D".contentEquals(type) ? 2 : 1;
        return method.getImplementation().getRegisterCount() - words;
    }
    private static Map<String, ClassDef> classes(Path archive) throws Exception {
        Set<String> wanted = new HashSet<String>(Arrays.asList(CandidateToolbarPatch.OWNERS));
        Map<String, ClassDef> result = new HashMap<String, ClassDef>();
        MultiDexContainer<? extends DexFile> container = DexFileFactory.loadDexContainer(archive.toFile(), Opcodes.forApi(26));
        for (String name : container.getDexEntryNames()) for (ClassDef c : container.getEntry(name).getDexFile().getClasses())
            if (wanted.contains(c.getType())) check(result.put(c.getType(), c) == null, "Unique native target class " + c.getType());
        check(result.keySet().equals(wanted), "Complete native target class set");
        return result;
    }
    private static void branchesEnterBridge(MutableMethod method, int target) {
        if (target == 3 || target == 5) return;
        List<? extends Instruction> code = method.getImplementation().getInstructions();
        List<Integer> addresses = new ArrayList<Integer>();
        Map<Integer, Instruction> byAddress = new HashMap<Integer, Instruction>();
        Set<Integer> terminalAddresses = new HashSet<Integer>();
        int address = 0;
        for (Instruction instruction : code) {
            addresses.add(address); byAddress.put(address, instruction);
            if (instruction.getOpcode() == Opcode.RETURN_VOID || instruction.getOpcode() == Opcode.RETURN_OBJECT)
                terminalAddresses.add(address);
            address += instruction.getCodeUnits();
        }
        for (int i = 0; i < code.size(); i++) {
            Instruction instruction = code.get(i);
            if (!(instruction instanceof OffsetInstruction)) continue;
            int destination = addresses.get(i) + ((OffsetInstruction) instruction).getCodeOffset();
            check(!terminalAddresses.contains(destination), "Native branch enters the tail hook, never bypasses it");
            if (instruction.getOpcode() == Opcode.PACKED_SWITCH || instruction.getOpcode() == Opcode.SPARSE_SWITCH) {
                check(byAddress.get(destination) instanceof SwitchPayload, "Native switch payload remains valid");
                for (SwitchElement e : ((SwitchPayload) byAddress.get(destination)).getSwitchElements())
                    check(!terminalAddresses.contains(addresses.get(i) + e.getOffset()), "Native switch edge cannot bypass tail hook");
            }
        }
    }
    private static void verifyBridges(Method original, MutableMethod patched, int target) {
        check(original.getImplementation().getRegisterCount() == patched.getImplementation().getRegisterCount(), "Native registers preserved");
        List<? extends Instruction> code = patched.getImplementation().getInstructions();
        String expectedName = target == 0 ? "bindToolbar" : target == 1 ? "layoutContainer" : target < 4 ? "layoutCollapsed" : target < 6 ? "layoutExpandedClose" : "configure";
        String expectedOwner = target == 6 ? "Lhiro/swiftkey/EmojiToolbar;" : "Lhiro/swiftkey/CandidateToolbar;";
        List<String> expectedParameters = target == 6 ? Arrays.asList("Lfc0/h;", "Ljava/lang/Object;")
                : Collections.singletonList(target == 0 ? "Lob0/d1;" : target < 4 ? "Landroid/view/View;" : CandidateToolbarPatch.OWNERS[4]);
        int calls = 0;
        for (int i = 0; i < code.size(); i++) {
            Instruction instruction = code.get(i);
            if (!bridge(instruction)) continue;
            calls++;
            MethodReference reference = reference(instruction);
            check(reference.getDefiningClass().equals(expectedOwner) && reference.getName().equals(expectedName)
                    && reference.getParameterTypes().equals(expectedParameters) && reference.getReturnType().equals("V"), "Exact runtime bridge ABI");
            check(instruction.getOpcode() == Opcode.INVOKE_STATIC_RANGE, "Bridge uses register-safe range invoke");
            RegisterRangeInstruction call = (RegisterRangeInstruction) instruction;
            check(call.getStartRegister() == receiver(original) && call.getRegisterCount() == (target == 6 ? 2 : 1), "Bridge receives native receiver/argument without clobbering locals");
            if (target == 3 || target == 5) check(i == 0, "Measure layout adjustment runs before native measurement");
            else check(i + 1 < code.size() && code.get(i + 1).getOpcode() == (target == 6 ? Opcode.RETURN_OBJECT : Opcode.RETURN_VOID), "Tail bridge runs immediately before return");
        }
        check(calls == (target == 6 ? 2 : 1), "Exact helper count covers all returns");
        if (target != 3 && target != 5) for (int i = 0; i < code.size(); i++)
            if (code.get(i).getOpcode() == Opcode.RETURN_OBJECT || code.get(i).getOpcode() == Opcode.RETURN_VOID)
                check(i > 0 && bridge(code.get(i - 1)), "Every normal exit runs the bridge");
        branchesEnterBridge(patched, target);
        MutableMethod restored = new MutableMethod(patched);
        for (int i = restored.getImplementation().getInstructions().size() - 1; i >= 0; i--)
            if (bridge(restored.getImplementation().getInstructions().get(i))) restored.getImplementation().removeInstruction(i);
        check(MethodContract.sha256(restored).equals(MethodContract.sha256(original)), "Removing only inserted calls restores exact native instruction flow and metadata");
    }
    private static void serialize(Map<String, ClassDef> originals, MutableMethod[] methods, Path output) throws Exception {
        Map<String, Method> replacements = new HashMap<String, Method>();
        for (Method method : methods) replacements.put(FORMAT.getMethodDescriptor(method), method);
        DexPool pool = new DexPool(Opcodes.forApi(26));
        for (ClassDef original : new TreeMap<String, ClassDef>(originals).values()) {
            List<Method> updated = new ArrayList<Method>();
            for (Method method : original.getMethods()) {
                Method replacement = replacements.get(FORMAT.getMethodDescriptor(method));
                updated.add(replacement == null ? method : replacement);
            }
            pool.internClass(new ImmutableClassDef(original.getType(), original.getAccessFlags(), original.getSuperclass(),
                    original.getInterfaces(), original.getSourceFile(), original.getAnnotations(), original.getFields(), updated));
        }
        FileDataStore store = new FileDataStore(output.toFile());
        try { pool.writeTo(store); } finally { store.close(); }
        Map<String, ClassDef> decoded = classes(output);
        for (MutableMethod method : methods) {
            String descriptor = FORMAT.getMethodDescriptor(method); Method found = null;
            for (Method candidate : decoded.get(method.getDefiningClass()).getMethods())
                if (descriptor.equals(FORMAT.getMethodDescriptor(candidate))) found = candidate;
            check(found != null && MethodContract.sha256(found).equals(MethodContract.sha256(method)), "Serialized DEX preserves patched control flow, registers and references");
        }
    }
    public static void main(String[] args) throws Exception {
        Path source = Path.of(args[0]), report = Path.of(args[1]);
        Files.createDirectories(report.toAbsolutePath().getParent());
        boolean fixture = Files.isDirectory(source); Path archive = source;
        if (fixture) {
            archive = report.resolveSibling("native-toolbar-fixture.dex");
            SmaliOptions options = new SmaliOptions(); options.apiLevel = 26; options.jobs = 1; options.outputDexFile = archive.toString();
            check(Smali.assemble(options, source.toString()), "Assemble exact native excerpts");
        }
        Map<String, ClassDef> nativeClasses = classes(archive);
        Method[] original = CandidateToolbarPatch.inspect(nativeClasses);
        check(original.length == 7, "All seven native targets fingerprinted");
        MutableMethod[] patched = mutable(original); CandidateToolbarPatch.applyMethods(patched);
        for (int i = 0; i < patched.length; i++) verifyBridges(original[i], patched[i], i);
        serialize(nativeClasses, patched, report.resolveSibling("patched-toolbar.dex"));
        boolean rejected = false;
        try { CandidateToolbarPatch.applyMethods(patched); } catch (IllegalStateException expected) { rejected = true; }
        check(rejected, "Duplicate application rejected");
        for (int changed = 0; changed < original.length; changed++) {
            MutableMethod[] drift = mutable(original);
            drift[changed].getImplementation().addInstruction(0, new BuilderInstruction10x(Opcode.NOP));
            String[] before = new String[drift.length];
            for (int i = 0; i < drift.length; i++) before[i] = MethodContract.sha256(drift[i]);
            rejected = false;
            try { CandidateToolbarPatch.applyMethods(drift); } catch (IllegalStateException expected) { rejected = true; }
            check(rejected, "Changed native target rejected " + changed);
            for (int i = 0; i < drift.length; i++) check(before[i].equals(MethodContract.sha256(drift[i])), "All targets remain untouched on any fingerprint failure");
        }
        String json = "{\n  \"status\": \"PASSED\",\n  \"scope\": \"Exact archived native layout and action-builder transformations; not Android execution\",\n"
                + "  \"native_fixture\": " + fixture + ",\n  \"native_methods\": 7,\n  \"inserted_runtime_calls\": 8,\n  \"assertions\": " + assertions
                + ",\n  \"native_return_labels_enter_hooks\": true,\n  \"no_registers_clobbered\": true,\n  \"serialized_dex_roundtrip_verified\": true,\n"
                + "  \"all_existing_native_bodies_preserved\": true,\n  \"all_target_drift_rejected_before_mutation\": true,\n"
                + "  \"device_tested\": false,\n  \"original_apk_apply_tested\": false\n}\n";
        Files.writeString(report, json);
        System.out.println("PASS toolbar native hook transformations: methods=7 inserted=8 assertions=" + assertions);
    }
}
