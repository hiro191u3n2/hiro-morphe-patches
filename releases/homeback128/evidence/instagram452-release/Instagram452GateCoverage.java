import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;

/** Additional independent proof before deleting injected instructions for the
 * full executable comparison. Deletion alone can hide exception-scope changes
 * that affect only the injected call, or incoming jumps into its interior. */
public final class Instagram452GateCoverage {
    static int assertions, checkedInstructions, rejectedMutations;
    static void check(boolean yes, String why) {
        assertions++;
        if (!yes) throw new AssertionError(why);
    }
    static final class Gate {
        final String owner, name, result;
        final int start, end;
        final Set<Integer> incoming = new HashSet<>();
        Gate(String owner, String name, String result, int start, int end, int... incoming) {
            this.owner = owner; this.name = name; this.result = result;
            this.start = start; this.end = end;
            for (int edge : incoming) this.incoming.add(edge);
        }
    }
    static final Gate[] GATES = {
        new Gate("Lcom/instagram/mainactivity/InstagramMainActivity;", "A1u", "V", 62, 66, 37, 48),
        new Gate("LX/02Hr;", "onBackPressed", "Z", 44, 48, 27, 34),
        new Gate("Landroidx/activity/ComponentActivity;", "CxU", "LX/001p;", 4, 5),
        new Gate("LX/001p;", "A04", "V", 0, 4),
        new Gate("LX/001p;", "A00", "V", 0, 4),
        new Gate("LX/00nv;", "invoke", "Ljava/lang/Object;", 42, 45, 40),
        new Gate("LX/00nv;", "invoke", "Ljava/lang/Object;", 56, 59),
        new Gate("LX/00M2;", "A0T", "Z", 163, 167)
    };
    static Method find(Map<String,ClassDef> classes, Gate gate) {
        ClassDef owner = classes.get(gate.owner);
        check(owner != null, "Native gate class exists");
        Method found = null;
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(gate.name) && method.getReturnType().equals(gate.result)) {
                check(found == null, "Unambiguous gate method");
                found = method;
            }
        }
        check(found != null, "Native gate method exists");
        return found;
    }
    static List<String> routes(MethodImplementation code, int pc) {
        List<String> routes = new ArrayList<>();
        for (TryBlock<? extends ExceptionHandler> region : code.getTryBlocks()) {
            if (pc >= region.getStartCodeAddress() &&
                    pc < region.getStartCodeAddress() + region.getCodeUnitCount()) {
                for (ExceptionHandler handler : region.getExceptionHandlers()) {
                    routes.add(handler.getExceptionType() + "@" + handler.getHandlerCodeAddress());
                }
            }
        }
        return routes;
    }
    static void incoming(Gate gate, int source, int target, int[] pc, boolean handler) {
        if (target < pc[gate.start] || target >= pc[gate.end]) return;
        check(!handler && target == pc[gate.start] && gate.incoming.contains(source),
              "No unapproved incoming branch/handler into gate " + gate.owner + "->" + gate.name);
    }
    static void audit(Method method, Gate gate) {
        MethodImplementation code = method.getImplementation();
        List<Instruction> body = new ArrayList<>();
        for (Instruction instruction : code.getInstructions()) body.add(instruction);
        int[] pc = new int[body.size() + 1];
        Map<Integer,Instruction> byPc = new HashMap<>();
        for (int i = 0; i < body.size(); i++) {
            byPc.put(pc[i], body.get(i));
            pc[i + 1] = pc[i] + body.get(i).getCodeUnits();
        }
        check(gate.end < body.size(), "Gate has native continuation");
        List<String> expected = routes(code, pc[gate.end]);
        for (int i = gate.start; i < gate.end; i++) {
            check(routes(code, pc[i]).equals(expected), "Injected exception routes match native continuation " +
                  gate.owner + "->" + gate.name + " at " + i);
            checkedInstructions++;
        }
        for (int i = 0; i < body.size(); i++) {
            Instruction instruction = body.get(i);
            if (!(instruction instanceof OffsetInstruction)) continue;
            int target = pc[i] + ((OffsetInstruction) instruction).getCodeOffset();
            if (instruction.getOpcode() == Opcode.PACKED_SWITCH ||
                    instruction.getOpcode() == Opcode.SPARSE_SWITCH) {
                Instruction payload = byPc.get(target);
                check(payload instanceof SwitchPayload, "Switch payload resolves");
                for (SwitchElement edge : ((SwitchPayload) payload).getSwitchElements())
                    incoming(gate, i, pc[i] + edge.getOffset(), pc, false);
            } else if (instruction.getOpcode() != Opcode.FILL_ARRAY_DATA) {
                incoming(gate, i, target, pc, false);
            }
        }
        for (TryBlock<? extends ExceptionHandler> region : code.getTryBlocks())
            for (ExceptionHandler handler : region.getExceptionHandlers())
                incoming(gate, -1, handler.getHandlerCodeAddress(), pc, true);
    }
    static Method replace(Method method, MethodImplementation code) {
        return new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(),
            method.getReturnType(), method.getAccessFlags(), method.getAnnotations(),
            method.getHiddenApiRestrictions(), code);
    }
    static void rejects(Method method, Gate gate, String message) {
        try { audit(method, gate); }
        catch (AssertionError expected) { rejectedMutations++; return; }
        throw new AssertionError("Audit accepted mutation: " + message);
    }
    static void negativeTests(Map<String,ClassDef> classes) {
        Gate tab = GATES[7];
        Method method = find(classes, tab);
        MethodImplementation code = method.getImplementation();
        List<Instruction> body = ActualInstagram452Audit.ins(method);
        int begin = ActualInstagram452Audit.addr(body, tab.start);
        int end = ActualInstagram452Audit.addr(body, tab.end);
        List<ImmutableTryBlock> changed = new ArrayList<>();
        boolean removed = false;
        for (TryBlock<? extends ExceptionHandler> region : code.getTryBlocks()) {
            int from = region.getStartCodeAddress(), to = from + region.getCodeUnitCount();
            if (from < begin && to > end) {
                changed.add(new ImmutableTryBlock(from, begin - from, region.getExceptionHandlers()));
                changed.add(new ImmutableTryBlock(end, to - end, region.getExceptionHandlers()));
                removed = true;
            } else changed.add(new ImmutableTryBlock(from, to - from, region.getExceptionHandlers()));
        }
        check(removed, "Tab gate originally covered by native exception handler");
        rejects(replace(method, new ImmutableMethodImplementation(code.getRegisterCount(),
            code.getInstructions(), changed, code.getDebugItems())), tab, "gate-only exception hole");
        Gate main = GATES[0];
        method = find(classes, main);
        MutableMethodImplementation mutable = new MutableMethodImplementation(method.getImplementation());
        mutable.replaceInstruction(37, new BuilderInstruction21t(Opcode.IF_EQZ, 0,
            mutable.newLabelForIndex(main.start + 1)));
        rejects(replace(method, mutable), main, "incoming branch into gate interior");
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("PATCHED_APK");
        Map<String,ClassDef> classes = RealUpgradeAudit.read(args[0]);
        for (Gate gate : GATES) audit(find(classes, gate), gate);
        int checked = checkedInstructions;
        negativeTests(classes);
        System.out.println("PASS independent gate exception/entry audit: 8 gates in 7 native methods; " +
            checked + " injected instructions preserve native exception routes; no unapproved branch/handler " +
            "entry; rejectedMutations=" + rejectedMutations + "; assertions=" + assertions);
    }
}
