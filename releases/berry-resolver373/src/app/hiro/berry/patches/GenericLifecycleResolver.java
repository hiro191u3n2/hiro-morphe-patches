package app.hiro.berry.patches;

import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.ExceptionHandler;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.TryBlock;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import java.util.*;

/** Prove that a platform lifecycle call receives this owner's WebView.
 * Supports either this.web or this.controller.web. Branches are joined with
 * provenance unions, so an unknown receiver on any path rejects the method. */
public final class GenericLifecycleResolver {
    private static final DexFormatter F = DexFormatter.INSTANCE;
    private static final int UNKNOWN = 1, SELF = 2, CONTROLLER = 4, WEB = 8;

    private GenericLifecycleResolver() {}

    public static boolean ownsWebReceiver(Method method, String anchor,
            String ownerToControllerField, String controllerToWebField) {
        if (method == null || method.getImplementation() == null || (method.getAccessFlags() & 8) != 0) return false;
        int count = method.getImplementation().getRegisterCount(), argumentSlots = 1;
        for (CharSequence parameter : method.getParameterTypes()) {
            String type = parameter.toString();
            argumentSlots += type.equals("J") || type.equals("D") ? 2 : 1;
        }
        if (count < argumentSlots) return false;
        Map<Integer,Instruction> code = new LinkedHashMap<>();
        int address = 0, target = -1;
        for (Instruction instruction : method.getImplementation().getInstructions()) {
            code.put(address, instruction);
            if (reference(instruction, anchor)) {
                if (target >= 0 || (instruction.getOpcode() != Opcode.INVOKE_VIRTUAL
                        && instruction.getOpcode() != Opcode.INVOKE_VIRTUAL_RANGE)) return false;
                target = address;
            }
            address += instruction.getCodeUnits();
        }
        if (target < 0 || !code.containsKey(0)) return false;
        int[] entry = new int[count];
        Arrays.fill(entry, UNKNOWN);
        entry[count - argumentSlots] = SELF;
        Map<Integer,int[]> states = new HashMap<>();
        Deque<Integer> pending = new ArrayDeque<>();
        states.put(0, entry); pending.add(0);
        while (!pending.isEmpty()) {
            int pc = pending.removeFirst();
            Instruction instruction = code.get(pc);
            if (instruction == null || instruction instanceof SwitchPayload
                    || instruction.getOpcode() == Opcode.ARRAY_PAYLOAD) return false;
            int[] before = states.get(pc), after = before.clone();
            Opcode op = instruction.getOpcode();
            if (op.setsRegister()) {
                if (!(instruction instanceof OneRegisterInstruction)) return false;
                int destination = ((OneRegisterInstruction) instruction).getRegisterA();
                if (!register(after, destination)) return false;
                after[destination] = UNKNOWN;
                if (op.setsWideRegister()) {
                    if (!register(after, destination + 1)) return false;
                    after[destination + 1] = UNKNOWN;
                }
                if (op == Opcode.MOVE_OBJECT || op == Opcode.MOVE_OBJECT_FROM16 || op == Opcode.MOVE_OBJECT_16) {
                    int source = ((TwoRegisterInstruction) instruction).getRegisterB();
                    if (!register(before, source)) return false;
                    after[destination] = before[source];
                } else if (op == Opcode.IGET_OBJECT) {
                    int receiver = ((TwoRegisterInstruction) instruction).getRegisterB();
                    if (!register(before, receiver)) return false;
                    if (ownerToControllerField != null && reference(instruction, ownerToControllerField)
                            && before[receiver] == SELF) after[destination] = CONTROLLER;
                    if (reference(instruction, controllerToWebField)
                            && before[receiver] == (ownerToControllerField == null ? SELF : CONTROLLER))
                        after[destination] = WEB;
                }
            }
            // Exception handlers see the pre-instruction register state. This
            // also prevents a catch path from bypassing an ownership load.
            if (op.canThrow()) for (TryBlock<? extends ExceptionHandler> block : method.getImplementation().getTryBlocks()) {
                if (pc >= block.getStartCodeAddress() && pc < block.getStartCodeAddress() + block.getCodeUnitCount())
                    for (ExceptionHandler handler : block.getExceptionHandlers())
                        if (!merge(code, states, pending, handler.getHandlerCodeAddress(), before)) return false;
            }
            String name = op.name;
            if (name.startsWith("return") || op == Opcode.THROW) continue;
            if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH) {
                int payloadAddress = pc + ((OffsetInstruction) instruction).getCodeOffset();
                Instruction payload = code.get(payloadAddress);
                if (!(payload instanceof SwitchPayload)) return false;
                for (SwitchElement element : ((SwitchPayload) payload).getSwitchElements())
                    if (!merge(code, states, pending, pc + element.getOffset(), after)) return false;
            } else if (instruction instanceof OffsetInstruction && op != Opcode.FILL_ARRAY_DATA) {
                if (!merge(code, states, pending, pc + ((OffsetInstruction) instruction).getCodeOffset(), after)) return false;
                if (name.startsWith("goto")) continue;
            }
            if (!merge(code, states, pending, pc + instruction.getCodeUnits(), after)) return false;
        }
        int[] beforeCall = states.get(target);
        if (beforeCall == null) return false;
        Instruction call = code.get(target);
        int receiver;
        if (call instanceof FiveRegisterInstruction) {
            FiveRegisterInstruction normal = (FiveRegisterInstruction) call;
            if (normal.getRegisterCount() < 1) return false;
            receiver = normal.getRegisterC();
        } else if (call instanceof RegisterRangeInstruction) {
            RegisterRangeInstruction range = (RegisterRangeInstruction) call;
            if (range.getRegisterCount() < 1) return false;
            receiver = range.getStartRegister();
        } else return false;
        return register(beforeCall, receiver) && beforeCall[receiver] == WEB;
    }

    private static boolean reference(Instruction instruction, String descriptor) {
        return instruction instanceof ReferenceInstruction
            && descriptor.equals(F.getReference(((ReferenceInstruction) instruction).getReference()));
    }
    private static boolean register(int[] values, int register) { return register >= 0 && register < values.length; }
    private static boolean merge(Map<Integer,Instruction> code, Map<Integer,int[]> states,
            Deque<Integer> pending, int target, int[] incoming) {
        if (!code.containsKey(target)) return false;
        int[] current = states.get(target);
        if (current == null) {
            states.put(target, incoming.clone()); pending.add(target); return true;
        }
        boolean changed = false;
        for (int i = 0; i < current.length; i++) {
            int combined = current[i] | incoming[i];
            if (combined != current[i]) { current[i] = combined; changed = true; }
        }
        if (changed) pending.add(target);
        return true;
    }
}
