package app.hiro.swiftkey.patches;
import java.nio.file.*;
import java.util.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.smali.*;

/** Validate actual injected DEX control flow, original-body restoration and drift rejection. */
public final class NativeSpaceProbe {
    static int assertions;
    static final DexFormatter F = DexFormatter.INSTANCE;
    static void check(boolean value, String message) { assertions++; if (!value) throw new AssertionError(message); }
    static ClassDef read(Path path) throws Exception {
        MultiDexContainer<? extends DexFile> container = DexFileFactory.loadDexContainer(path.toFile(), Opcodes.forApi(26));
        ClassDef found = null;
        for (String name : container.getDexEntryNames()) for (ClassDef c : container.getEntry(name).getDexFile().getClasses())
            if (c.getType().equals(SpaceConversionPatch.OWNER)) { check(found == null, "Unique native owner"); found = c; }
        check(found != null, "Native owner exists"); return found;
    }
    static Method find(ClassDef c) {
        for (Method m : c.getMethods()) if ((SpaceConversionPatch.OWNER + "->" + SpaceConversionPatch.SIGNATURE).equals(F.getMethodDescriptor(m))) return m;
        throw new AssertionError("Missing native target");
    }
    static List<Instruction> instructions(Method m) { List<Instruction> a = new ArrayList<>(); for (Instruction i : m.getImplementation().getInstructions()) a.add(i); return a; }
    static boolean dispatch(List<Instruction> code, boolean converted) {
        // Only execute the inserted guard. Returning false reaches the exact original body.
        int pc = 0; boolean result = false; int steps = 0;
        while (pc < 4) {
            check(++steps <= 4, "Guard terminates");
            Instruction i = code.get(pc);
            switch (i.getOpcode()) {
                case INVOKE_STATIC_RANGE: result = converted; pc++; break;
                case MOVE_RESULT: pc++; break;
                case IF_EQZ: if (!result) return false; pc++; break;
                case RETURN_VOID: return true;
                default: throw new AssertionError("Unexpected guard opcode " + i.getOpcode());
            }
        }
        throw new AssertionError("Guard falls through unexpectedly");
    }
    public static void main(String[] args) throws Exception {
        Path fixtures = Path.of(args[0]), output = Path.of(args[1]); Files.createDirectories(output);
        Path originalDex = output.resolve("space-native-fixture.dex");
        SmaliOptions options = new SmaliOptions(); options.apiLevel = 26; options.jobs = 1; options.outputDexFile = originalDex.toString();
        check(Smali.assemble(options, fixtures.toString()), "Native fixture assembles");
        ClassDef owner = read(originalDex); Method original = SpaceConversionPatch.inspect(owner); MutableMethod target = new MutableMethod(original);
        SpaceConversionPatch.applyMethod(target);
        List<Instruction> before = instructions(original), after = instructions(target);
        check(after.size() == before.size() + 4, "Exactly four guard instructions added");
        check(target.getImplementation().getRegisterCount() == 17, "Native register count preserved");
        check(after.get(0).getOpcode() == Opcode.INVOKE_STATIC_RANGE, "Guard invokes runtime");
        RegisterRangeInstruction call = (RegisterRangeInstruction) after.get(0);
        check(call.getStartRegister() == 10 && call.getRegisterCount() == 4, "Pass model, breadcrumb, input and punctuation");
        check(F.getReference(((ReferenceInstruction) call).getReference()).equals(SpaceConversionPatch.HOOK), "Correct runtime ABI");
        check(after.get(1).getOpcode() == Opcode.MOVE_RESULT && ((OneRegisterInstruction) after.get(1)).getRegisterA() == 0, "Boolean result in spare local");
        check(after.get(2).getOpcode() == Opcode.IF_EQZ && ((OneRegisterInstruction) after.get(2)).getRegisterA() == 0 && ((OffsetInstruction) after.get(2)).getCodeOffset() == 3, "False branches to original first instruction");
        check(after.get(3).getOpcode() == Opcode.RETURN_VOID, "True consumes key");
        check(dispatch(after, true), "Successful conversion consumes software key");
        check(!dispatch(after, false), "No conversion delegates normal key");
        MutableMethod undo = new MutableMethod(target);
        for (int n = 0; n < 4; n++) undo.getImplementation().removeInstruction(0);
        check(MethodContract.sha256(undo).equals(MethodContract.sha256(original)), "All original instructions, branches, catches and debug data restored");
        List<Method> methods = new ArrayList<>();
        for (Method m : owner.getMethods()) methods.add(F.getMethodDescriptor(m).equals(F.getMethodDescriptor(original)) ? target : m);
        ClassDef updated = new ImmutableClassDef(owner.getType(), owner.getAccessFlags(), owner.getSuperclass(), owner.getInterfaces(), owner.getSourceFile(), owner.getAnnotations(), owner.getFields(), methods);
        DexPool pool = new DexPool(Opcodes.forApi(26)); pool.internClass(updated); Path patchedDex = output.resolve("space-native-patched.dex");
        FileDataStore store = new FileDataStore(patchedDex.toFile()); try { pool.writeTo(store); } finally { store.close(); }
        Method restored = find(read(patchedDex)); check(MethodContract.sha256(restored).equals(MethodContract.sha256(target)), "Patched native method survives DEX serialization");
        check(dispatch(instructions(restored), true) && !dispatch(instructions(restored), false), "Serialized native guard routes both outcomes");
        boolean rejected = false; String hash = MethodContract.sha256(target);
        try { SpaceConversionPatch.applyMethod(target); } catch (IllegalStateException e) { rejected = true; }
        check(rejected && hash.equals(MethodContract.sha256(target)), "Duplicate application rejected before mutation");
        MutableMethod drift = new MutableMethod(original); drift.getImplementation().addInstruction(0, new BuilderInstruction10x(Opcode.NOP));
        hash = MethodContract.sha256(drift); rejected = false;
        try { SpaceConversionPatch.applyMethod(drift); } catch (IllegalStateException e) { rejected = true; }
        check(rejected && hash.equals(MethodContract.sha256(drift)), "Changed native method rejected before mutation");
        rejected = false; try { SpaceConversionPatch.inspect(null); } catch (IllegalStateException e) { rejected = true; }
        check(rejected, "Missing native owner rejected");
        String report = "{\"status\":\"PASSED\",\"assertions\":" + assertions + ",\"native_method\":\"" + SpaceConversionPatch.OWNER + "->" + SpaceConversionPatch.SIGNATURE + "\",\"baseline_method_sha256\":\"" + MethodContract.sha256(original) + "\",\"patched_method_sha256\":\"" + MethodContract.sha256(target) + "\",\"native_methods_changed\":1,\"native_instructions_added\":4,\"inverse_native_method_restoration\":true,\"native_guard_control_flow_tested\":true,\"all_target_drift_rejected_before_mutation\":true,\"device_tested\":false,\"original_apk_apply_tested\":false}\n";
        Files.writeString(output.resolve("native-space-report.json"), report); System.out.print(report);
    }
}
