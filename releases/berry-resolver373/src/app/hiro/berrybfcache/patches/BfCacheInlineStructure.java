package app.hiro.berrybfcache.patches;

import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import java.util.*;

/** Prove ownership at setup calls that newer Berry builds inline. Every
 * reaching path must carry the tab parameter through the expected field chain;
 * a matching field name or call elsewhere in the method is insufficient. */
final class BfCacheInlineStructure {
    private static final DexFormatter F = DexFormatter.INSTANCE;
    private static final int UNKNOWN = 1, TAB = 2, CONTROLLER = 4, WEB = 8,
        PROVIDER = 16, BOUNDARY = 32;

    private BfCacheInlineStructure() {}

    static boolean ownsBoundaryReceiver(Method setup, String tabAdapter, String adapterWeb,
            String factory, String providerBoundary, String boundaryCall) {
        return prove(setup, tabAdapter, adapterWeb, factory, providerBoundary,
            boundaryCall, BOUNDARY, true);
    }

    static boolean ownsControllerReceiver(Method setup, String tabAdapter, String adapterSetter) {
        return prove(setup, tabAdapter, null, null, null, adapterSetter, CONTROLLER, false);
    }

    private static boolean prove(Method method, String tabAdapter, String adapterWeb,
            String factory, String providerBoundary, String anchor, int expected, boolean boundary) {
        if (method == null || method.getImplementation() == null || (method.getAccessFlags() & 9) != 1
                || !"V".equals(method.getReturnType()) || method.getParameterTypes().size() != 1
                || !tabAdapter.substring(0, tabAdapter.indexOf("->")).contentEquals(method.getParameterTypes().get(0)))
            return false;
        int count = method.getImplementation().getRegisterCount();
        if (count < 2) return false;
        // The extra slot models the short-lived invoke result. It is cleared
        // by every instruction other than the following move-result-object.
        int result = count;
        Map<Integer,Instruction> code = new LinkedHashMap<>();
        int address = 0, target = -1;
        for (Instruction instruction : method.getImplementation().getInstructions()) {
            code.put(address, instruction);
            if (reference(instruction, anchor)) {
                boolean opcode = boundary
                    ? instruction.getOpcode() == Opcode.INVOKE_INTERFACE || instruction.getOpcode() == Opcode.INVOKE_INTERFACE_RANGE
                    : instruction.getOpcode() == Opcode.INVOKE_VIRTUAL || instruction.getOpcode() == Opcode.INVOKE_VIRTUAL_RANGE;
                if (target >= 0 || !opcode) return false;
                target = address;
            }
            address += instruction.getCodeUnits();
        }
        if (target < 0 || !code.containsKey(0)) return false;
        int[] entry = new int[count + 1];
        Arrays.fill(entry, UNKNOWN);
        entry[count - 1] = TAB; // p1 for an instance method taking exactly one reference.
        Map<Integer,int[]> states = new HashMap<>();
        Deque<Integer> pending = new ArrayDeque<>();
        states.put(0, entry); pending.add(0);
        while (!pending.isEmpty()) {
            int pc = pending.removeFirst();
            Instruction instruction = code.get(pc);
            if (instruction == null || instruction instanceof SwitchPayload || instruction.getOpcode() == Opcode.ARRAY_PAYLOAD)
                return false;
            int[] before = states.get(pc), after = before.clone();
            after[result] = UNKNOWN;
            Opcode op = instruction.getOpcode();
            if (op.setsRegister()) {
                if (!(instruction instanceof OneRegisterInstruction)) return false;
                int destination = ((OneRegisterInstruction) instruction).getRegisterA();
                if (!register(count, destination)) return false;
                after[destination] = UNKNOWN;
                if (op.setsWideRegister()) {
                    if (!register(count, destination + 1)) return false;
                    after[destination + 1] = UNKNOWN;
                }
                if (op == Opcode.MOVE_OBJECT || op == Opcode.MOVE_OBJECT_FROM16 || op == Opcode.MOVE_OBJECT_16) {
                    int source = ((TwoRegisterInstruction) instruction).getRegisterB();
                    if (!register(count, source)) return false;
                    after[destination] = before[source];
                } else if (op == Opcode.CHECK_CAST) {
                    after[destination] = before[destination];
                } else if (op == Opcode.MOVE_RESULT_OBJECT) {
                    after[destination] = before[result];
                } else if (op == Opcode.IGET_OBJECT) {
                    int receiver = ((TwoRegisterInstruction) instruction).getRegisterB();
                    if (!register(count, receiver)) return false;
                    if (reference(instruction, tabAdapter) && before[receiver] == TAB)
                        after[destination] = CONTROLLER;
                    else if (reference(instruction, adapterWeb) && before[receiver] == CONTROLLER)
                        after[destination] = WEB;
                    else if (reference(instruction, providerBoundary) && before[receiver] == PROVIDER)
                        after[destination] = BOUNDARY;
                }
            }
            if (reference(instruction, factory)) {
                if (op != Opcode.INVOKE_STATIC && op != Opcode.INVOKE_STATIC_RANGE) return false;
                int[] args = args(instruction);
                if (args.length != 1 || !register(count, args[0])) return false;
                if (before[args[0]] == WEB) after[result] = PROVIDER;
            }
            if (op.canThrow()) for (TryBlock<? extends ExceptionHandler> block : method.getImplementation().getTryBlocks()) {
                if (pc < block.getStartCodeAddress() || pc >= block.getStartCodeAddress() + block.getCodeUnitCount()) continue;
                int[] exceptional = before.clone(); exceptional[result] = UNKNOWN;
                for (ExceptionHandler handler : block.getExceptionHandlers())
                    if (!merge(code, states, pending, handler.getHandlerCodeAddress(), exceptional)) return false;
            }
            if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH) {
                Instruction payload = code.get(pc + ((OffsetInstruction) instruction).getCodeOffset());
                if (!(payload instanceof SwitchPayload)) return false;
                for (SwitchElement element : ((SwitchPayload) payload).getSwitchElements())
                    if (!merge(code, states, pending, pc + element.getOffset(), after)) return false;
            } else if (instruction instanceof OffsetInstruction && op != Opcode.FILL_ARRAY_DATA) {
                if (!merge(code, states, pending, pc + ((OffsetInstruction) instruction).getCodeOffset(), after)) return false;
            }
            if (op.canContinue() && !merge(code, states, pending, pc + instruction.getCodeUnits(), after)) return false;
        }
        int[] beforeCall = states.get(target), arguments = args(code.get(target));
        return beforeCall != null && arguments.length >= 1 && register(count, arguments[0])
            && beforeCall[arguments[0]] == expected;
    }

    private static boolean reference(Instruction instruction, String descriptor) {
        return descriptor != null && instruction instanceof ReferenceInstruction
            && descriptor.equals(F.getReference(((ReferenceInstruction) instruction).getReference()));
    }
    private static boolean register(int count, int register) { return register >= 0 && register < count; }
    private static int[] args(Instruction instruction) {
        if (instruction instanceof FiveRegisterInstruction) {
            FiveRegisterInstruction call = (FiveRegisterInstruction) instruction;
            return Arrays.copyOf(new int[]{call.getRegisterC(),call.getRegisterD(),call.getRegisterE(),call.getRegisterF(),call.getRegisterG()}, call.getRegisterCount());
        }
        if (instruction instanceof RegisterRangeInstruction) {
            RegisterRangeInstruction call = (RegisterRangeInstruction) instruction;
            int[] args = new int[call.getRegisterCount()];
            for (int i=0;i<args.length;i++) args[i]=call.getStartRegister()+i;
            return args;
        }
        return new int[0];
    }
    private static boolean merge(Map<Integer,Instruction> code, Map<Integer,int[]> states,
            Deque<Integer> pending, int target, int[] incoming) {
        if (!code.containsKey(target)) return false;
        int[] current = states.get(target);
        if (current == null) { states.put(target,incoming.clone()); pending.add(target); return true; }
        boolean changed = false;
        for (int i=0;i<current.length;i++) {
            int union = current[i] | incoming[i];
            if (union != current[i]) { current[i]=union; changed=true; }
        }
        if (changed) pending.add(target);
        return true;
    }
}
