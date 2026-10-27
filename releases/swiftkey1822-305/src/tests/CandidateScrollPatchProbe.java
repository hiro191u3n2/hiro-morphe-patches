package app.hiro.swiftkey.patches;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
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
    private static int unique(List<? extends Instruction> code, String target) {
        int found = -1;
        for (int i = 0; i < code.size(); i++) if (reference(code.get(i)).equals(target)) {
            check(found == -1, "Unique hook " + target); found = i;
        }
        check(found >= 0, "Present hook " + target); return found;
    }
    private static void signature(Instruction instruction, List<String> parameters, String result, int... registers) {
        check(instruction.getOpcode() == Opcode.INVOKE_STATIC, "Helper uses static invocation");
        MethodReference ref = (MethodReference) ((ReferenceInstruction) instruction).getReference();
        check(ref.getParameterTypes().equals(parameters) && ref.getReturnType().equals(result), "Helper ABI matches runtime");
        FiveRegisterInstruction call = (FiveRegisterInstruction) instruction;
        int[] actual = {call.getRegisterC(), call.getRegisterD(), call.getRegisterE(), call.getRegisterF(), call.getRegisterG()};
        check(call.getRegisterCount() == registers.length, "Helper register count preserved");
        for (int i = 0; i < registers.length; i++) check(actual[i] == registers[i], "Helper original argument register " + i);
    }
    private static void nativeCall(MutableMethod method, int index, String owner, String name,
                                   List<String> parameters, int first, int second) {
        method.getImplementation().replaceInstruction(index, new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,
                2, first, second, 0, 0, 0, new ImmutableMethodReference(owner, name, parameters, "V")));
    }
    private static void cleanupBranches(List<? extends Instruction> code, int finish) {
        int[] addresses = new int[code.size()]; int address = 0, returns = 0, cleanupTargets = 0;
        for (int i = 0; i < code.size(); i++) { addresses[i] = address; address += code.get(i).getCodeUnits(); }
        check(finish + 1 == code.size() - 1 && code.get(finish + 1).getOpcode() == Opcode.RETURN_VOID,
                "Cleanup precedes the one terminal return");
        int returnAddress = addresses[finish + 1];
        for (int i = 0; i < code.size(); i++) {
            if (code.get(i).getOpcode() == Opcode.RETURN_VOID) returns++;
            if (code.get(i) instanceof OffsetInstruction) {
                int target = addresses[i] + ((OffsetInstruction) code.get(i)).getCodeOffset();
                check(target != returnAddress, "Native early return branch cannot bypass cleanup");
                if (target == addresses[finish]) cleanupTargets++;
            }
        }
        check(returns == 1 && cleanupTargets >= 2, "Equal-result and disconnected-provider branches execute cleanup");
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
        ClassDef adapter = null, panel = null, provider = null, listener = null;
        for (String entry : container.getDexEntryNames()) for (ClassDef c : container.getEntry(entry).getDexFile().getClasses()) {
            if (c.getType().equals("Lrb0/h;")) { check(adapter == null, "Unique native adapter"); adapter = c; }
            if (c.getType().equals("Lrb0/k;")) { check(panel == null, "Unique native panel"); panel = c; }
            if (c.getType().equals("Lrb0/j;")) { check(provider == null, "Unique native provider"); provider = c; }
            if (c.getType().equals("Lrb0/l;")) { check(listener == null, "Unique native scroll listener"); listener = c; }
        }
        Method[] originals = CandidateScrollPatch.inspect(adapter, panel, provider, listener);
        check(originals.length == 6, "All six native entry points validated");
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
                int dispatch = unique(code, "Lhiro/swiftkey/CandidateScroll;->dispatchUpdate");
                signature(code.get(dispatch), Arrays.asList("Lrb0/j;", "Lfb0/u;", "Z"), "V", 5, 0, 6);
                int finish = unique(code, "Lhiro/swiftkey/CandidateScroll;->finishUpdate");
                signature(code.get(finish), Collections.singletonList("Lrb0/j;"), "V", 5);
                cleanupBranches(code, finish);
                int skip = unique(code, "Lhiro/swiftkey/CandidateScroll;->canSkipSameResult");
                signature(code.get(skip), Arrays.asList("Lrb0/j;", "Lib0/a;"), "Z", 5, 6);
                check(code.get(skip - 1).getOpcode() == Opcode.IF_EQZ
                        && ((OneRegisterInstruction) code.get(skip - 1)).getRegisterA() == 1
                        && code.get(skip + 1).getOpcode() == Opcode.MOVE_RESULT
                        && ((OneRegisterInstruction) code.get(skip + 1)).getRegisterA() == 1
                        && code.get(skip + 2).getOpcode() == Opcode.IF_EQZ
                        && ((OneRegisterInstruction) code.get(skip + 2)).getRegisterA() == 1,
                        "Same-result helper follows only successful native text equality and reuses v1");
                int[] addresses = new int[code.size()]; int address = 0;
                for (int n = 0; n < code.size(); n++) { addresses[n] = address; address += code.get(n).getCodeUnits(); }
                check(addresses[skip - 1] + ((OffsetInstruction) code.get(skip - 1)).getCodeOffset()
                        == addresses[skip + 2] + ((OffsetInstruction) code.get(skip + 2)).getCodeOffset(),
                        "Only eligible same-result updates reach the exact original refresh branch");
                for (Instruction instruction : code)
                    check(!reference(instruction).equals("Lfb0/u;->h"), "Exactly one notifier call is delegated, never duplicated");
                nativeCall(reverted, dispatch, "Lfb0/u;", "h", Collections.singletonList("Z"), 0, 6);
                reverted.getImplementation().removeInstruction(finish);
                for (int n = 0; n < 3; n++) reverted.getImplementation().removeInstruction(skip);
                reverted.getImplementation().removeInstruction(0);
                reverted.getImplementation().removeInstruction(0);
            }
            else if (i == 4) {
                List<? extends Instruction> code = reverted.getImplementation().getInstructions();
                int scroll = unique(code, "Lhiro/swiftkey/CandidateScroll;->scrollPanelToStart");
                signature(code.get(scroll), Arrays.asList("Lrb0/k;", "Landroidx/recyclerview/widget/RecyclerView;", "I"),
                        "V", 2, 1, 0);
                check(scroll + 1 == code.size() - 1 && code.get(scroll + 1).getOpcode() == Opcode.RETURN_VOID,
                        "Only native scroll tail is delegated after panel bookkeeping");
                for (Instruction instruction : code)
                    check(!reference(instruction).equals("Landroidx/recyclerview/widget/RecyclerView;->w0"),
                            "Panel cannot also call the original stop-scroll route");
                nativeCall(reverted, scroll, "Landroidx/recyclerview/widget/RecyclerView;", "w0",
                        Collections.singletonList("I"), 1, 0);
            }
            else if (i == 5) {
                List<? extends Instruction> code = reverted.getImplementation().getInstructions();
                int rebase = unique(code, "Lhiro/swiftkey/CandidateScroll;->rebaseScrollWatermark");
                check(rebase == 0, "Watermark hook precedes every native scroll-listener instruction");
                signature(code.get(rebase), Arrays.asList("Lrb0/l;", "Landroidx/recyclerview/widget/RecyclerView;", "I", "I"),
                        "V", 2, 3, 4, 5);
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
        for (int i = 0; i < originals.length; i++) if (i != 3)
            check(MethodContract.sha256(providerDrift[i]).equals(MethodContract.sha256(originals[i])),
                    "No adapter hooks applied if container fingerprint differs");
        MutableMethod[] panelDrift = mutable(originals);
        panelDrift[4].getImplementation().addInstruction(new BuilderInstruction10x(Opcode.NOP));
        rejected = false;
        try { CandidateScrollPatch.applyMethods(panelDrift); } catch (IllegalStateException expected) { rejected = true; }
        check(rejected, "Changed panel reset fingerprint rejected before any mutation");
        for (int i = 0; i < 4; i++)
            check(MethodContract.sha256(panelDrift[i]).equals(MethodContract.sha256(originals[i])),
                    "Existing hooks untouched when panel reset fingerprint differs");
        MutableMethod[] listenerDrift = mutable(originals);
        listenerDrift[5].getImplementation().addInstruction(new BuilderInstruction10x(Opcode.NOP));
        rejected = false;
        try { CandidateScrollPatch.applyMethods(listenerDrift); } catch (IllegalStateException expected) { rejected = true; }
        check(rejected, "Changed native fetch listener fingerprint rejected before any mutation");
        for (int i = 0; i < 5; i++)
            check(MethodContract.sha256(listenerDrift[i]).equals(MethodContract.sha256(originals[i])),
                    "Other hooks untouched when native fetch listener differs");
        String json = "{\n  \"status\": \"PASSED\",\n  \"scope\": \"Production patch transforms exact native method bodies; modeled event dispatch, not Android execution\",\n"
                + "  \"native_fixture\": " + fixture + ",\n  \"native_methods\": 6,\n  \"native_hook_sites\": 9,\n  \"dispatch_scenarios\": 4,\n  \"assertions\": " + assertions
                + ",\n  \"device_tested\": false,\n  \"original_apk_apply_tested\": false,\n  \"original_method_bodies_preserved\": true,\n  \"method_fingerprint_drift_rejected\": true\n}\n";
        Files.writeString(report, json);
        System.out.println("PASS candidate native hook transformations: methods=6 hook-sites=9 dispatch=4 assertions=" + assertions);
    }
}
