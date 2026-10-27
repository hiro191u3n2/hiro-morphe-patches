package app.hiro.swiftkey.patches;

import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction;

/** Append only a footer-install call after the native controller is fully initialized. */
public final class EmojiFooterPatch {
    static final String OWNER = "Lje0/c;";
    static final String SENDER = "Lwb0/x;";
    static final String SIGNATURE = "<init>(Landroid/view/ContextThemeWrapper;Lma0/q;Lgd0/i;Landroidx/lifecycle/q0;Lgg/a;ZLoz/i6;Lf40/a;Lah0/w0;Lfz/h;Lwb0/x;Lfb0/n;Lfb0/g0;Lfb0/d;Lfb0/b4;Lwa0/q0;Z)V";
    static final String ENTER_SIGNATURE = "y(Llh0/a;)V";
    private static final String HASH = "1b508b8cadce20a0bb958251ded2e9f72fa91dc18468475822ecfe8ed5c481cd";
    private static final String ENTER_HASH = "3e81ac39e6b36dad25818f1a424a3de5230400f857722f9c994cbd6b17a81019";
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;
    private EmojiFooterPatch() {}

    public static void checkOriginal(BytecodePatchContext context) {
        inspect(context.classDefByOrNull(OWNER), context.classDefByOrNull(SENDER));
    }
    public static void apply(BytecodePatchContext context) {
        checkOriginal(context);
        MutableMethod constructor = null;
        for (MutableMethod method : context.mutableClassDefBy(OWNER).getMethods()) {
            if ((OWNER + "->" + SIGNATURE).equals(FORMAT.getMethodDescriptor(method))) {
                require(constructor == null, "Duplicate mutable emoji-footer target");
                constructor = method;
            }
        }
        require(constructor != null, "Missing mutable emoji-footer target");
        applyMethod(constructor);
    }
    static Method[] inspect(ClassDef controller, ClassDef sender) {
        require(controller != null && controller.getType().equals(OWNER)
                && controller.getSuperclass().equals("Ljava/lang/Object;"), "Emoji-footer controller changed");
        require(sender != null && sender.getType().equals(SENDER)
                && sender.getSuperclass().equals("Ljava/lang/Object;"), "Native Enter sender changed");
        Method constructor = find(controller, SIGNATURE), enter = find(sender, ENTER_SIGNATURE);
        verifyConstructor(constructor);
        require(enter != null && enter.getImplementation() != null && enter.getAccessFlags() == 0x11
                && enter.getImplementation().getRegisterCount() == 11
                && ENTER_HASH.equals(MethodContract.sha256(enter)), "Native Enter action fingerprint changed");
        return new Method[] {constructor, enter};
    }
    private static Method find(ClassDef target, String signature) {
        Method found = null;
        for (Method method : target.getMethods()) if ((target.getType() + "->" + signature).equals(FORMAT.getMethodDescriptor(method))) {
            require(found == null, "Duplicate emoji-footer native target"); found = method;
        }
        return found;
    }
    private static int verifyConstructor(Method method) {
        require(method != null && method.getImplementation() != null, "Missing emoji-footer constructor");
        require((OWNER + "->" + SIGNATURE).equals(FORMAT.getMethodDescriptor(method)), "Emoji-footer constructor signature changed");
        require(method.getAccessFlags() == 0x10001 && method.getImplementation().getRegisterCount() == 35,
                "Emoji-footer constructor ABI/registers changed");
        require(HASH.equals(MethodContract.sha256(method)), "Emoji-footer constructor fingerprint changed");
        int success = -1, index = 0;
        for (Instruction instruction : method.getImplementation().getInstructions()) {
            if (instruction.getOpcode() == Opcode.RETURN_VOID) {
                require(success < 0, "Ambiguous emoji-footer successful initialization exit"); success = index;
            }
            if (instruction.getOpcode().setsRegister() && instruction instanceof OneRegisterInstruction) {
                int register = ((OneRegisterInstruction) instruction).getRegisterA();
                require(register != 17 && register != 28, "Native owner or Enter sender argument overwritten");
                require(!instruction.getOpcode().setsWideRegister() || (register != 16 && register != 27),
                        "Native owner or Enter sender argument overwritten by wide value");
            }
            index++;
        }
        require(success >= 0, "Missing emoji-footer successful initialization exit");
        return success;
    }
    static void applyMethod(MutableMethod constructor) {
        int success = verifyConstructor(constructor);
        InstructionExtensions.INSTANCE.addInstructions(constructor, success,
            "move-object/from16 v0, p0\nmove-object/from16 v1, p11\n"
            + "invoke-static {v0, v1}, Lhiro/swiftkey/EmojiFooter;->install(Lje0/c;Lwb0/x;)V");
        require(constructor.getImplementation().getRegisterCount() == 35, "Emoji-footer hook changed native registers");
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
