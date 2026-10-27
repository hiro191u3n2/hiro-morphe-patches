package app.hiro.swiftkey.patches;

import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.*;

/** Exact lifecycle and initial layout selection hooks for SwiftKey Beta 9.13.16.4. */
public final class JapaneseStartPatch {
    static final String SERVICE = "Lcom/touchtype/KeyboardService;";
    static final String LAYOUT = "Lfb0/g0;";
    static final String[] OWNERS = {SERVICE, SERVICE, SERVICE, LAYOUT};
    static final String[] SIGNATURES = {
        "onStartInputView(Landroid/view/inputmethod/EditorInfo;Z)V",
        "onFinishInputView(Z)V", "onWindowHidden()V", "b(Llh0/a;Lfb0/q1;)V"
    };
    private static final String[] HASHES = {
        "673b9624b1a2d2e82e79878c82fa0532b88a5d644ea6c2f2c41c0ad315088b3c",
        "a84c52a9c1547ae38f4e5d7308cb33b939cf4b11bf60d81a88753a3db2b4150e",
        "df0f75a2f4ac6a634cc4ffc16737444a12106598dbf907872b09bf82d405754b",
        "ffa0732fd7ebc28072ee8020b91d91a63fa5cd874e5b9de15de0be4fd3dc06b0"
    };
    private static final int[] REGISTERS = {4, 3, 2, 12};
    // JapaneseGlidePatch runs earlier in the unchanged main pipeline and adds
    // exactly its reset() prefix to onFinishInputView. Accept only that state.
    private static final String AFTER_GLIDE_FINISH_HASH = "ca65b77e13855ce200961ff1aaafdaf40466057136d32d5cea84d2feba0e3a85";
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;
    private JapaneseStartPatch() {}

    public static void checkOriginal(BytecodePatchContext context) {
        inspect(context.classDefByOrNull(SERVICE), context.classDefByOrNull(LAYOUT));
    }
    public static void apply(BytecodePatchContext context) {
        MutableMethod[] methods = new MutableMethod[4];
        for (int i = 0; i < methods.length; i++) {
            for (MutableMethod method : context.mutableClassDefBy(OWNERS[i]).getMethods())
                if ((OWNERS[i] + "->" + SIGNATURES[i]).equals(FORMAT.getMethodDescriptor(method))) {
                    require(methods[i] == null, "Duplicate Japanese start method"); methods[i] = method;
                }
            require(methods[i] != null, "Missing mutable Japanese start method");
        }
        applyMethods(methods);
    }
    static Method[] inspect(ClassDef service, ClassDef layout) {
        require(service != null && service.getType().equals(SERVICE)
                && service.getSuperclass().equals("Lcom/touchtype/Hilt_KeyboardService;"), "Keyboard service type changed");
        require(layout != null && layout.getType().equals(LAYOUT)
                && layout.getSuperclass().equals("Ljava/lang/Object;"), "Initial layout controller changed");
        Method[] methods = new Method[4];
        for (int i = 0; i < methods.length; i++) {
            for (Method method : (i < 3 ? service : layout).getMethods())
                if ((OWNERS[i] + "->" + SIGNATURES[i]).equals(FORMAT.getMethodDescriptor(method))) {
                    require(methods[i] == null, "Duplicate Japanese start target"); methods[i] = method;
                }
            verify(methods[i], i, false);
        }
        return methods;
    }
    private static void verify(Method method, int index, boolean applying) {
        require(method != null && method.getImplementation() != null, "Missing Japanese start body");
        require((OWNERS[index] + "->" + SIGNATURES[index]).equals(FORMAT.getMethodDescriptor(method)), "Japanese start signature changed");
        require(method.getAccessFlags() == 0x11 && method.getImplementation().getRegisterCount() == REGISTERS[index], "Japanese start ABI changed");
        String hash = applying && index == 1 ? AFTER_GLIDE_FINISH_HASH : HASHES[index];
        require(hash.equals(MethodContract.sha256(method)), "Japanese start fingerprint changed " + SIGNATURES[index]);
    }
    static int selectionIndex(Method method) {
        List<Instruction> instructions = new ArrayList<Instruction>();
        for (Instruction instruction : method.getImplementation().getInstructions()) instructions.add(instruction);
        int found = -1;
        for (int i = 0; i + 1 < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if (!(instruction instanceof ReferenceInstruction)
                    || !(((ReferenceInstruction) instruction).getReference() instanceof MethodReference)) continue;
            MethodReference call = (MethodReference) ((ReferenceInstruction) instruction).getReference();
            if (!"Lfb0/f5;->i()Lqj0/a;".equals(FORMAT.getMethodDescriptor(call))) continue;
            require(found == -1 && instruction.getOpcode() == Opcode.INVOKE_VIRTUAL, "Ambiguous native language getter");
            FiveRegisterInstruction invoke = (FiveRegisterInstruction) instruction;
            require(invoke.getRegisterCount() == 1 && invoke.getRegisterC() == 2, "Native languages register changed");
            Instruction result = instructions.get(i + 1);
            require(result.getOpcode() == Opcode.MOVE_RESULT_OBJECT && ((OneRegisterInstruction) result).getRegisterA() == 11,
                    "Native selected layout register changed");
            found = i + 2;
        }
        require(found >= 0, "Missing native language getter"); return found;
    }
    static void applyMethods(MutableMethod[] methods) {
        require(methods != null && methods.length == 4, "Japanese start hook count changed");
        for (int i = 0; i < methods.length; i++) verify(methods[i], i, true);
        int selection = selectionIndex(methods[3]);
        InstructionExtensions.INSTANCE.addInstructions(methods[0], 0,
                "invoke-static {p0, p1, p2}, Lhiro/swiftkey/JapaneseStart;->begin(Lcom/touchtype/KeyboardService;Landroid/view/inputmethod/EditorInfo;Z)V");
        for (int i = 1; i < 3; i++) InstructionExtensions.INSTANCE.addInstructions(methods[i], 0,
                "invoke-static {p0}, Lhiro/swiftkey/JapaneseStart;->finish(Lcom/touchtype/KeyboardService;)V");
        InstructionExtensions.INSTANCE.addInstructions(methods[3], selection,
                "invoke-static {v1, v2, p2}, Lhiro/swiftkey/JapaneseStart;->select(Lfb0/i0;Lfb0/f5;Lqj0/a;)Lqj0/a;\nmove-result-object p2");
        for (int i = 0; i < methods.length; i++) require(methods[i].getImplementation().getRegisterCount() == REGISTERS[i],
                "Japanese start hook changed native register count");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
