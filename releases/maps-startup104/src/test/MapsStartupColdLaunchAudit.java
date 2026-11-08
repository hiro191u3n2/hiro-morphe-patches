import java.io.File;
import java.util.*;
import app.hiro.mapsstartup.patches.MapsStartupLocationPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/**
 * Binds the host callback replay to the supplied APK's actual cold-create path.
 * Lnce.onCreate(null), effective Bundle v4 == null: 0xA4C -> getIntent() ->
 * 0xA52 invoke-virtual Lpw.onNewIntent. Dynamic dispatch reaches Lnce's override
 * and therefore the startup hook before the first onStart/onResume.
 * This is structural/dataflow inspection, not execution of Android/onCreate.
 */
public final class MapsStartupColdLaunchAudit {
    private static final String OWNER = "Lnce;";
    private static final String MAIN = "Lcom/google/android/maps/MapsActivity;";
    private static final String INTENT = "Landroid/content/Intent;";
    private static int checks;
    private static int inspectedPrefixes;

    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why);
    }
    private static String desc(MethodReference method) {
        return MapsStartupLocationPatch.descriptor(method);
    }
    private static Method method(ClassDef owner, String signature) {
        check(owner != null, "Missing class for " + signature);
        return MapsStartupLocationPatch.find(owner, signature);
    }
    private static List<Instruction> instructions(Method method) {
        List<Instruction> out = new ArrayList<Instruction>();
        for (Instruction instruction : method.getImplementation().getInstructions()) out.add(instruction);
        return out;
    }
    private static NavigableMap<Integer, Instruction> code(Method method) {
        NavigableMap<Integer, Instruction> out = new TreeMap<Integer, Instruction>();
        int address = 0;
        for (Instruction instruction : instructions(method)) {
            out.put(address, instruction);
            address += instruction.getCodeUnits();
        }
        return out;
    }
    private static Instruction at(Map<Integer, Instruction> code, int address, Opcode opcode) {
        Instruction found = code.get(address);
        check(found != null && found.getOpcode() == opcode,
            String.format(Locale.ROOT, "Native cold-create opcode differs at 0x%X: expected %s", address, opcode));
        return found;
    }
    private static int[] registers(Instruction instruction) {
        if (instruction instanceof RegisterRangeInstruction) {
            RegisterRangeInstruction range = (RegisterRangeInstruction)instruction;
            int[] out = new int[range.getRegisterCount()];
            for (int index = 0; index < out.length; index++) out[index] = range.getStartRegister() + index;
            return out;
        }
        FiveRegisterInstruction five = (FiveRegisterInstruction)instruction;
        return Arrays.copyOf(new int[] {five.getRegisterC(), five.getRegisterD(), five.getRegisterE(),
            five.getRegisterF(), five.getRegisterG()}, five.getRegisterCount());
    }
    private static void invoke(Map<Integer, Instruction> code, int address, Opcode opcode,
            String signature, int... arguments) {
        Instruction instruction = at(code, address, opcode);
        Reference reference = ((ReferenceInstruction)instruction).getReference();
        check(reference instanceof MethodReference && signature.equals(desc((MethodReference)reference)),
            "Unexpected call at " + address);
        check(Arrays.equals(arguments, registers(instruction)), "Wrong call argument registers at " + address);
    }
    private static void pair(Map<Integer, Instruction> code, int address, Opcode opcode, int a, int b) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)at(code, address, opcode);
        check(instruction.getRegisterA() == a && instruction.getRegisterB() == b,
            "Wrong native value transfer at " + address);
    }
    private static void one(Map<Integer, Instruction> code, int address, Opcode opcode, int register) {
        check(((OneRegisterInstruction)at(code, address, opcode)).getRegisterA() == register,
            "Wrong native value register at " + address);
    }
    private static void zero(Map<Integer, Instruction> code, int address, int register) {
        one(code, address, Opcode.CONST_4, register);
        check(((NarrowLiteralInstruction)code.get(address)).getNarrowLiteral() == 0,
            "Expected null constant at " + address);
    }
    private static void target(Map<Integer, Instruction> code, int address, int expected) {
        check(address + ((OffsetInstruction)code.get(address)).getCodeOffset() == expected,
            "Native cold-create branch target differs at " + address);
    }
    private static boolean writes(Instruction instruction, int register) {
        if (!instruction.getOpcode().setsRegister() || !(instruction instanceof OneRegisterInstruction)) return false;
        int written = ((OneRegisterInstruction)instruction).getRegisterA();
        return written == register || (instruction.getOpcode().setsWideRegister() && written + 1 == register);
    }
    private static void prefix(Method method, int hook) {
        List<Instruction> body = instructions(method);
        Instruction first = body.get(0);
        check(first.getOpcode() == Opcode.INVOKE_STATIC_RANGE && body.get(1).getOpcode() == Opcode.NOP,
            "Expected an exact invoke-range/NOP entry hook");
        Reference reference = ((ReferenceInstruction)first).getReference();
        String expected = MapsStartupLocationPatch.RUNTIME + "->" + MapsStartupLocationPatch.HELPER_NAMES[hook]
            + "(Landroid/app/Activity;" + MapsStartupLocationPatch.HOOK_ARGUMENTS[hook] + ")V";
        check(reference instanceof MethodReference && expected.equals(desc((MethodReference)reference)),
            "Unexpected startup helper in the entry hook");
        int parameters = MapsStartupLocationPatch.HOOK_ARGUMENTS[hook].isEmpty() ? 1 : 2;
        int[] expectedRegisters = new int[parameters];
        for (int index = 0; index < parameters; index++)
            expectedRegisters[index] = method.getImplementation().getRegisterCount() - parameters + index;
        check(Arrays.equals(expectedRegisters, registers(first)), "Entry hook does not forward original parameters");
    }
    private static MutableMethod nativeMethod(ClassDef owner, int hook) {
        MutableMethod result = new MutableMethod(method(owner, MapsStartupLocationPatch.CONTRACTS[hook]));
        Instruction first = instructions(result).get(0);
        if (first instanceof ReferenceInstruction) {
            Reference reference = ((ReferenceInstruction)first).getReference();
            if (reference instanceof MethodReference
                    && MapsStartupLocationPatch.RUNTIME.equals(((MethodReference)reference).getDefiningClass())) {
                // The separately SHA-pinned reported v2.8.7 APK contains the known
                // old startup pair. Inspect/remove that pair only in this copy;
                // the complete resulting native fingerprint must still match.
                prefix(result, hook);
                result.getImplementation().removeInstruction(1);
                result.getImplementation().removeInstruction(0);
                inspectedPrefixes++;
            }
        }
        MapsStartupLocationPatch.validateContract(result, hook);
        return result;
    }
    private static Method resolveVirtual(Map<String, ClassDef> classes, String receiver, String name) {
        Set<String> visited = new HashSet<String>();
        for (String type = receiver; type != null && classes.containsKey(type); type = classes.get(type).getSuperclass()) {
            check(visited.add(type), "Activity hierarchy is cyclic");
            for (Method candidate : classes.get(type).getMethods())
                if (name.equals(candidate.getName()) && "V".equals(candidate.getReturnType())
                        && candidate.getParameterTypes().equals(Collections.singletonList(INTENT))) return candidate;
        }
        throw new AssertionError("Virtual intent handler is missing");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: MapsStartupColdLaunchAudit <apk>");
        Map<String, ClassDef> classes = new TreeMap<String, ClassDef>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(args[0]), Opcodes.getDefault());
        for (String entry : dex.getDexEntryNames())
            for (ClassDef type : dex.getEntry(entry).getDexFile().getClasses())
                if (classes.put(type.getType(), type) != null) throw new AssertionError("Duplicate class " + type.getType());
        ClassDef main = classes.get(MAIN), owner = classes.get(OWNER);
        check(main != null && OWNER.equals(main.getSuperclass()), "Maps launcher hierarchy differs");
        Set<String> ancestry = new LinkedHashSet<String>();
        for (ClassDef type = main; type != null; type = classes.get(type.getSuperclass()))
            check(ancestry.add(type.getType()), "Maps activity superclass cycle");
        check(ancestry.contains("Lpw;"), "Referenced virtual owner is no longer a Maps ancestor");
        for (Method declared : main.getMethods())
            check(!"onCreate".equals(declared.getName()) && !"onNewIntent".equals(declared.getName()),
                "Launcher subclass now intercepts native lifecycle parameters");
        MutableMethod create = nativeMethod(owner, 0), intent = nativeMethod(owner, 1);
        check(create.getAccessFlags() == 17 && intent.getAccessFlags() == 20,
            "Expected final native onCreate/onNewIntent overrides");
        check(create.getImplementation().getRegisterCount() == 23, "Native onCreate parameter layout differs");
        NavigableMap<Integer, Instruction> body = code(create);

        // A null original Bundle v22 remains null in either native selection.
        pair(body, 0x000, Opcode.MOVE_OBJECT_FROM16, 1, 21);
        zero(body, 0x0EC, 10);
        for (Instruction instruction : body.subMap(0x0ED, true, 0x2C3, false).values())
            check(!writes(instruction, 10), "Null temporary overwritten before Bundle selection");
        invoke(body, 0x2B9, Opcode.INVOKE_DIRECT_RANGE, "Lnce;->aq(Landroid/os/Bundle;)Z", 21, 22);
        one(body, 0x2BC, Opcode.MOVE_RESULT, 4);
        pair(body, 0x2BD, Opcode.IF_NE, 9, 4); target(body, 0x2BD, 0x2C1);
        pair(body, 0x2BF, Opcode.MOVE_OBJECT, 4, 10);
        at(body, 0x2C0, Opcode.GOTO); target(body, 0x2C0, 0x2C3);
        pair(body, 0x2C1, Opcode.MOVE_OBJECT_FROM16, 4, 22);
        invoke(body, 0x644, Opcode.INVOKE_SUPER, "Lncs;->onCreate(Landroid/os/Bundle;)V", 1, 4);

        // The only later v4 writes before the replay guard are a temporary in
        // the null-state block (reset before its continuation), plus a thrown
        // exception temporary. Neither turns null into a normal restored state.
        Set<Integer> bundleWrites = new TreeSet<Integer>();
        for (Map.Entry<Integer, Instruction> entry : body.subMap(0x2C3, true, 0xA4C, false).entrySet())
            if (writes(entry.getValue(), 4)) bundleWrites.add(entry.getKey());
        check(bundleWrites.equals(new TreeSet<Integer>(Arrays.asList(0x664, 0x679, 0x76A))),
            "Native effective-Bundle dataflow changed before initial intent replay: " + bundleWrites);
        one(body, 0x64E, Opcode.IF_NEZ, 4); target(body, 0x64E, 0x67A);
        pair(body, 0x664, Opcode.IGET_OBJECT, 4, 0);
        for (Map.Entry<Integer, Instruction> entry : body.subMap(0x650, true, 0x679, false).entrySet())
            if (entry.getValue() instanceof OffsetInstruction) target(body, entry.getKey(), 0x679);
        zero(body, 0x679, 4);
        one(body, 0x769, Opcode.MOVE_EXCEPTION, 0);
        pair(body, 0x76A, Opcode.MOVE_OBJECT, 4, 0);
        for (Map.Entry<Integer, Instruction> entry : body.subMap(0x76A, true, 0x773, false).entrySet())
            if (entry.getValue() instanceof OffsetInstruction) target(body, entry.getKey(), 0x773);
        one(body, 0x773, Opcode.THROW, 4);

        one(body, 0xA4C, Opcode.IF_NEZ, 4); target(body, 0xA4C, 0xA76);
        invoke(body, 0xA4E, Opcode.INVOKE_VIRTUAL, "Lnce;->getIntent()Landroid/content/Intent;", 1);
        one(body, 0xA51, Opcode.MOVE_RESULT_OBJECT, 0);
        invoke(body, 0xA52, Opcode.INVOKE_VIRTUAL, "Lpw;->onNewIntent(Landroid/content/Intent;)V", 1, 0);
        Method ancestor = method(classes.get("Lpw;"), "Lpw;->onNewIntent(Landroid/content/Intent;)V");
        check((ancestor.getAccessFlags() & (AccessFlags.STATIC.getValue() | AccessFlags.PRIVATE.getValue())) == 0,
            "Initial intent call is no longer an overridable ancestor method");
        Method dispatched = resolveVirtual(classes, MAIN, "onNewIntent");
        check("Lnce;->onNewIntent(Landroid/content/Intent;)V".equals(desc(dispatched)),
            "Initial virtual intent call no longer reaches the hooked override");

        // Verify the actual current patch puts both hooks at entry with original
        // arguments. Host tests replay these callbacks, including the nested one.
        MutableMethod hookedCreate = new MutableMethod(create), hookedIntent = new MutableMethod(intent);
        MapsStartupLocationPatch.applyHook(hookedCreate, 0); prefix(hookedCreate, 0);
        MapsStartupLocationPatch.applyHook(hookedIntent, 1); prefix(hookedIntent, 1);
        System.out.println("PASS native cold-launch structure: exact native onCreate/onNewIntent fingerprints; "
            + "null Bundle stays null at 0xA4C; 0xA52 invoke-virtual Lpw.onNewIntent resolves to Lnce; "
            + "both current entry hooks forward original Activity/argument; inspected existing entry pairs="
            + inspectedPrefixes + "; assertions=" + checks
            + "; structural inspection plus separate host replay only; Android/onCreate execution not performed.");
    }
}
