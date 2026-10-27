package app.hiro.swiftkey.patches;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.smali.Smali;
import com.android.tools.smali.smali.SmaliOptions;

/** Exercises production patching on exact archived native methods, not fake methods. */
public final class CandidateScrollPatchProbe {
    private static int assertions;
    private static void check(boolean value, String message) {
        assertions++; if (!value) throw new AssertionError(message);
    }
    private static MutableMethod[] mutable(Method[] originals) {
        MutableMethod[] result = new MutableMethod[originals.length];
        for (int i = 0; i < result.length; i++) result[i] = new MutableMethod(originals[i]);
        return result;
    }
    private static String reference(Instruction instruction) {
        if (!(instruction instanceof ReferenceInstruction)) return "";
        Object r = ((ReferenceInstruction) instruction).getReference();
        if (!(r instanceof MethodReference)) return "";
        MethodReference m = (MethodReference) r;
        return m.getDefiningClass() + "->" + m.getName();
    }
    private static void simulate(MutableMethod method, boolean reset, boolean handled) {
        List<? extends Instruction> code = method.getImplementation().getInstructions();
        List<Integer> addresses = new ArrayList<>(); Map<Integer, Integer> indices = new HashMap<>();
        int address = 0; for (int i = 0; i < code.size(); i++) {
            addresses.add(address); indices.put(address, i); address += code.get(i).getCodeUnits();
        }
        int[] registers = {99, reset ? 1 : 0}; int result = -1, helper = 0, refresh = 0;
        Map<String, Integer> fields = new TreeMap<>(); boolean returned = false; int pc = 0;
        for (int steps = 0; steps < 100 && !returned; steps++) {
            Instruction instruction = code.get(pc); Opcode op = instruction.getOpcode(); int next = pc + 1;
            if (op == Opcode.IF_EQZ || op == Opcode.IF_NEZ) {
                int value = registers[((OneRegisterInstruction) instruction).getRegisterA()];
                if ((op == Opcode.IF_EQZ && value == 0) || (op == Opcode.IF_NEZ && value != 0)) {
                    Integer destination = indices.get(addresses.get(pc) + ((OffsetInstruction) instruction).getCodeOffset());
                    check(destination != null, "Branch points to a real instruction"); next = destination;
                }
            } else if (op == Opcode.INVOKE_STATIC) {
                FiveRegisterInstruction call = (FiveRegisterInstruction) instruction;
                check(reference(instruction).equals("Lhiro/swiftkey/CandidateScroll;->tryAppend"), "Only expected helper call");
                check(call.getRegisterCount() == 2 && registers[call.getRegisterC()] == 99
                        && registers[call.getRegisterD()] == 0, "Helper receives adapter and original false argument");
                helper++; result = handled ? 1 : 0;
            } else if (op == Opcode.MOVE_RESULT) {
                check(result != -1, "Result follows helper");
                registers[((OneRegisterInstruction) instruction).getRegisterA()] = result;
            } else if (op == Opcode.INVOKE_VIRTUAL) {
                check(reference(instruction).equals("Lrb0/h;->H"), "Original refresh call preserved");
                FiveRegisterInstruction call = (FiveRegisterInstruction) instruction;
                check(call.getRegisterCount() == 1 && registers[call.getRegisterC()] == 99, "Original receiver preserved"); refresh++;
            } else if (op == Opcode.CONST_4) {
                registers[((OneRegisterInstruction) instruction).getRegisterA()] = ((NarrowLiteralInstruction) instruction).getNarrowLiteral();
            } else if (op == Opcode.IPUT || op == Opcode.IPUT_BOOLEAN) {
                TwoRegisterInstruction put = (TwoRegisterInstruction) instruction;
                check(registers[put.getRegisterB()] == 99, "Reset uses original adapter");
                FieldReference field = (FieldReference) ((ReferenceInstruction) instruction).getReference();
                fields.put(field.getName(), registers[put.getRegisterA()]);
            } else if (op == Opcode.RETURN_VOID) returned = true;
            else check(op == Opcode.NOP, "Unexpected instruction in candidate event route " + op);
            pc = next;
        }
        check(returned, "Candidate event route terminates");
        check(helper == (reset ? 0 : 1), "Reset bypasses helper; append calls once");
        check(refresh == (reset || !handled ? 1 : 0), "Native fallback/handled dispatch is exact");
        if (reset) {
            check(fields.size() == 4 && fields.get("n") == 1 && fields.get("o") == 0
                    && fields.get("p") == 0 && fields.get("q") == 0, "All original replacement state updates preserved");
        } else check(fields.isEmpty(), "Append never executes replacement state updates");
    }
    public static void main(String[] args) throws Exception {
        Path source = Path.of(args[0]), report = Path.of(args[1]);
        Files.createDirectories(report.toAbsolutePath().getParent());
        boolean fixture = Files.isDirectory(source);
        Path input = source;
        if (fixture) {
            input = report.resolveSibling("native-candidate-fixture.dex");
            SmaliOptions options = new SmaliOptions(); options.apiLevel = 26; options.jobs = 1; options.outputDexFile = input.toString();
            check(Smali.assemble(options, source.toString()), "Assembled exact native excerpts");
        }
        MultiDexContainer<? extends DexFile> container = DexFileFactory.loadDexContainer(input.toFile(), Opcodes.forApi(26));
        ClassDef adapter = null, panel = null, provider = null;
        for (String entry : container.getDexEntryNames()) for (ClassDef c : container.getEntry(entry).getDexFile().getClasses()) {
            if (c.getType().equals("Lrb0/h;")) { check(adapter == null, "Unique native adapter"); adapter = c; }
            if (c.getType().equals("Lrb0/k;")) { check(panel == null, "Unique native panel"); panel = c; }
            if (c.getType().equals("Lrb0/j;")) { check(provider == null, "Unique native provider"); provider = c; }
        }
        Method[] originals = CandidateScrollPatch.inspect(adapter, panel, provider);
        check(originals.length == 4, "All four native entry points validated");
        MutableMethod[] patched = mutable(originals); CandidateScrollPatch.applyMethods(patched);
        for (int i = 0; i < patched.length; i++) {
            check(patched[i].getImplementation().getRegisterCount() == originals[i].getImplementation().getRegisterCount(), "Registers preserved");
            MutableMethod reverted = new MutableMethod(patched[i]);
            if (i == 0) for (int count = 0; count < 6; count++) reverted.getImplementation().removeInstruction(0);
            else if (i == 3) {
                List<? extends Instruction> code = reverted.getImplementation().getInstructions();
                check(code.get(0).getOpcode() == Opcode.INVOKE_STATIC
                        && reference(code.get(0)).equals("Lhiro/swiftkey/CandidateScroll;->stabilize"),
                        "Container stabilization runs before native candidate classification");
                MethodReference ref = (MethodReference) ((ReferenceInstruction) code.get(0)).getReference();
                check(ref.getReturnType().equals("Lib0/a;")
                        && ref.getParameterTypes().equals(Arrays.asList("Lrb0/j;", "Lib0/a;")), "Container hook signature matches runtime");
                FiveRegisterInstruction call = (FiveRegisterInstruction) code.get(0);
                check(call.getRegisterCount() == 2 && call.getRegisterC() == 5 && call.getRegisterD() == 6,
                        "Container hook receives original provider and incoming object");
                check(code.get(1).getOpcode() == Opcode.MOVE_RESULT_OBJECT
                        && ((OneRegisterInstruction) code.get(1)).getRegisterA() == 6,
                        "Container result replaces only incoming p1; p0 and all locals preserved");
                reverted.getImplementation().removeInstruction(0);
                reverted.getImplementation().removeInstruction(0);
            }
            else {
                List<? extends Instruction> code = reverted.getImplementation().getInstructions(); int found = -1;
                for (int n = 0; n < code.size(); n++) if (reference(code.get(n)).equals("Lhiro/swiftkey/CandidateScroll;->snapshot")) {
                    check(found == -1, "One snapshot per route"); found = n;
                    check(n > 0 && reference(code.get(n - 1)).equals("Lra/q1;->n"), "Snapshot follows native notify");
                    FiveRegisterInstruction call = (FiveRegisterInstruction) code.get(n);
                    check(call.getRegisterCount() == 1 && call.getRegisterC() == (i == 1 ? 4 : 1), "Snapshot receives live adapter register");
                }
                check(found >= 0, "Snapshot inserted"); reverted.getImplementation().removeInstruction(found);
            }
            check(MethodContract.sha256(reverted).equals(MethodContract.sha256(originals[i])), "All original method semantics preserved after removing only added hook");
        }
        simulate(patched[0], true, false); simulate(patched[0], true, true);
        simulate(patched[0], false, false); simulate(patched[0], false, true);
        boolean rejected = false;
        try { CandidateScrollPatch.applyMethods(patched); } catch (IllegalStateException expected) { rejected = true; }
        check(rejected, "Duplicate application rejected");
        MutableMethod[] drift = mutable(originals); drift[0].getImplementation().addInstruction(new BuilderInstruction10x(Opcode.NOP));
        rejected = false;
        try { CandidateScrollPatch.applyMethods(drift); } catch (IllegalStateException expected) { rejected = true; }
        check(rejected, "Changed native method rejected before mutation");
        check(MethodContract.sha256(drift[1]).equals(MethodContract.sha256(originals[1]))
                && MethodContract.sha256(drift[2]).equals(MethodContract.sha256(originals[2])), "Other hooks remain untouched on validation failure");
        MutableMethod[] providerDrift = mutable(originals);
        providerDrift[3].getImplementation().addInstruction(new BuilderInstruction10x(Opcode.NOP));
        rejected = false;
        try { CandidateScrollPatch.applyMethods(providerDrift); } catch (IllegalStateException expected) { rejected = true; }
        check(rejected, "Changed native container classifier rejected");
        for (int i = 0; i < 3; i++)
            check(MethodContract.sha256(providerDrift[i]).equals(MethodContract.sha256(originals[i])),
                    "No adapter hooks applied if container fingerprint differs");
        String json = "{\n  \"status\": \"PASSED\",\n  \"scope\": \"Production patch transforms exact native method bodies; modeled event dispatch, not Android execution\",\n"
                + "  \"native_fixture\": " + fixture + ",\n  \"native_methods\": 4,\n  \"dispatch_scenarios\": 4,\n  \"assertions\": " + assertions
                + ",\n  \"device_tested\": false,\n  \"original_apk_apply_tested\": false,\n  \"original_method_bodies_preserved\": true,\n  \"method_fingerprint_drift_rejected\": true\n}\n";
        Files.writeString(report, json);
        System.out.println("PASS candidate native hook transformations: methods=4 dispatch=4 assertions=" + assertions);
    }
}
