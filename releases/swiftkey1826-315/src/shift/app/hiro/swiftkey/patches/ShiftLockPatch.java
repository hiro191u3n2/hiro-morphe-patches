package app.hiro.swiftkey.patches;

import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference;

/** Make the explicit software Shift-down transition use native CAPSLOCKED.
 * Automatic sentence capitalization and hardware-key processing are untouched.
 */
public final class ShiftLockPatch {
    static final String OWNER = "Lwb0/p1;";
    static final String SIGNATURE = "n(Llh0/a;)V";
    static final String HASH = "098d23bd8c99e605fd1357ca13126c07532374afdb1bec6a5ca76be897e869b6";
    static final String STATE = "Lwb0/r1;";
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;
    private ShiftLockPatch() {}

    public static void checkOriginal(BytecodePatchContext context) {
        inspect(context.classDefByOrNull(OWNER));
    }
    public static void apply(BytecodePatchContext context) {
        checkOriginal(context);
        MutableMethod target = null;
        for (MutableMethod method : context.mutableClassDefBy(OWNER).getMethods()) {
            if ((OWNER + "->" + SIGNATURE).equals(FORMAT.getMethodDescriptor(method))) {
                require(target == null, "Duplicate mutable Shift target"); target = method;
            }
        }
        require(target != null, "Missing mutable Shift target");
        applyMethod(target);
    }
    static Method inspect(ClassDef owner) {
        require(owner != null && OWNER.equals(owner.getType()) && "Ljava/lang/Object;".equals(owner.getSuperclass()), "Shift controller changed");
        Method found = null;
        for (Method method : owner.getMethods()) if ((OWNER + "->" + SIGNATURE).equals(FORMAT.getMethodDescriptor(method))) {
            require(found == null, "Duplicate native Shift target"); found = method;
        }
        verify(found); return found;
    }
    private static int verify(Method method) {
        require(method != null && method.getImplementation() != null, "Missing native Shift target");
        require((OWNER + "->" + SIGNATURE).equals(FORMAT.getMethodDescriptor(method)), "Shift signature changed");
        require(method.getAccessFlags() == 0x11 && method.getImplementation().getRegisterCount() == 7, "Shift ABI/registers changed");
        require(HASH.equals(MethodContract.sha256(method)), "Shift-down fingerprint changed");
        int found = -1, index = 0;
        for (Instruction instruction : method.getImplementation().getInstructions()) {
            if (instruction.getOpcode() == Opcode.SGET_OBJECT && instruction instanceof ReferenceInstruction
                    && (STATE + "->b:" + STATE).equals(FORMAT.getReference(((ReferenceInstruction) instruction).getReference()))) {
                require(found < 0 && ((OneRegisterInstruction) instruction).getRegisterA() == 0, "Shifted-state site changed"); found = index;
            }
            index++;
        }
        require(found == index - 3, "Ordinary Shift transition is not final state load/call/return");
        return found;
    }
    static void applyMethod(MutableMethod method) {
        int index = verify(method);
        method.getImplementation().replaceInstruction(index, new BuilderInstruction21c(Opcode.SGET_OBJECT, 0,
                new ImmutableFieldReference(STATE, "c", STATE)));
        require(method.getImplementation().getRegisterCount() == 7, "Shift hook changed native registers");
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
