package app.hiro.swiftkey.patches;

import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21s;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21s;

/** The same native limit guards eviction from the persisted list and the live recents map. */
final class EmojiHistoryCapacity {
    private static final String OWNER = "Lxc/d;";
    private static final String TARGET = OWNER + "->m(Lne0/g0;)V";
    private static final String ORIGINAL = "75673764e6ff10dfcf2ae79f28e08c59db3ec3c7c64e693a32bcd82fd67d400c";

    private EmojiHistoryCapacity() {}

    static void apply(BytecodePatchContext context) {
        for (MutableMethod method : context.mutableClassDefBy(OWNER).getMethods()) {
            if (!TARGET.equals(DexFormatter.INSTANCE.getMethodDescriptor(method))) continue;
            if (!ORIGINAL.equals(MethodContract.sha256(method))) {
                throw new IllegalStateException("SwiftKey emoji history writer changed");
            }
            int index = 43;
            Instruction21s limit = (Instruction21s) method.getImplementation().getInstructions().get(index);
            if (limit.getOpcode() != Opcode.CONST_16 || limit.getRegisterA() != 4 || limit.getNarrowLiteral() != 32) {
                throw new IllegalStateException("SwiftKey emoji history limit changed");
            }
            method.getImplementation().replaceInstruction(index, new BuilderInstruction21s(Opcode.CONST_16, limit.getRegisterA(), 96));
            return;
        }
        throw new IllegalStateException("Missing SwiftKey emoji history writer");
    }
}
