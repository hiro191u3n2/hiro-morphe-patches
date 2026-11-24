package app.hiro.swiftkey.patches;

import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21s;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21s;

/** Extend both the native store and the clock-category count used by the keyboard grid. */
final class EmojiHistoryCapacity {
    private static final String OWNER = "Lxc/d;";
    private static final String TARGET = OWNER + "->m(Lne0/g0;)V";
    private static final String ORIGINAL = "75673764e6ff10dfcf2ae79f28e08c59db3ec3c7c64e693a32bcd82fd67d400c";

    private EmojiHistoryCapacity() {}

    private static final int CAPACITY = 96;
    private static final String CLOCK = "Lne0/s0;";
    private static final String CLOCK_COUNT = CLOCK + "->getCount()I";
    private static final String ORIGINAL_CLOCK = "28b53606c064159544defa3509890b2df86728900ad0ce08e82b195306578722";

    static void apply(BytecodePatchContext context) {
        MutableMethod writer = locate(context, OWNER, TARGET, ORIGINAL);
        MutableMethod count = locate(context, CLOCK, CLOCK_COUNT, ORIGINAL_CLOCK);
        MutableMethod gestures = locate(context, "Lbo0/l;", "Lbo0/l;->m(Lne0/g0;ZLandroid/view/View;Lwb0/x;Ljava/util/function/Supplier;Ljava/lang/String;Lxc/d;ILfc0/n1;Lf40/a;Loz/j2;Lfz/h;Landroid/content/Context;Lah0/w0;Lda0/d;Lp40/w;Loz/n9;F)Lqe0/c;", "827548175ea46770d0ab7898a334fe498948d84f74f054e132b6bb6d35ef00ef");
        // v6 is free after the original variant action has been stored in v8.
        InstructionExtensions.INSTANCE.replaceInstruction(gestures, 157,
            "invoke-static {v4, v1, v2, v8, v6}, Lhiro/swiftkey/ClockHistoryHold;->bind(Lhc0/b;ILcm0/d;[Lic0/c0;Ljava/util/function/Supplier;)V");
        InstructionExtensions.INSTANCE.addInstructions(gestures, 157, "move-object/from16 v6, p4");
        // Native visual and accessibility hints must both use the tab's gesture.
        InstructionExtensions.INSTANCE.addInstructions(gestures, 40,
            "move-object/from16 v3, p4\ninvoke-static {v0, v3}, Lhiro/swiftkey/ClockHistoryHold;->showVariantIndicator(ZLjava/util/function/Supplier;)Z\nmove-result v0");
        replaceLimit(writer, 43, 4, 32);
        replaceLimit(count, 6, 1, 28);
    }

    private static MutableMethod locate(BytecodePatchContext context, String owner, String signature, String fingerprint) {
        for (MutableMethod method : context.mutableClassDefBy(owner).getMethods()) {
            if (signature.equals(DexFormatter.INSTANCE.getMethodDescriptor(method))) {
                if (fingerprint.equals(MethodContract.sha256(method))) return method;
                throw new IllegalStateException("SwiftKey emoji history method changed: " + signature);
            }
        }
        throw new IllegalStateException("Missing SwiftKey emoji history method: " + signature);
    }

    private static void replaceLimit(MutableMethod method, int index, int register, int expected) {
        Instruction21s limit = (Instruction21s) method.getImplementation().getInstructions().get(index);
        if (limit.getOpcode() != Opcode.CONST_16 || limit.getRegisterA() != register || limit.getNarrowLiteral() != expected) {
            throw new IllegalStateException("SwiftKey emoji history limit changed: " + method.getName());
        }
        method.getImplementation().replaceInstruction(index, new BuilderInstruction21s(Opcode.CONST_16, register, CAPACITY));
    }
}
