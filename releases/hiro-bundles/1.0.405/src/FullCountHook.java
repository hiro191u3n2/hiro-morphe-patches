package app.hiro.newxui.patches;

import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.Label;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import java.util.Collections;
import java.util.List;

/** Route raw counts to X's own grouped integer format before any decimal conversion. */
public final class FullCountHook {
    public static final String TYPE = "Lcom/x/android/main/landing/f;";
    private static final String SIGNATURE = TYPE + "->R(JLjava/lang/Integer;Z)Ljava/lang/String;";
    private static final String FINGERPRINT = "092759dd049decdb4539f3a2440fc93ca9d6e6b22cd6fc20c5a72a302266824e";
    private static final int AFTER_INITIAL_LOCALS = 2;
    private static final int GROUPED_INTEGER = 58;

    private FullCountHook() {}

    private static Method formatter(ClassDef type) {
        if (type != null) {
            for (Method method : type.getMethods()) {
                if (SIGNATURE.equals(method.toString())) return method;
            }
        }
        throw new IllegalStateException("Xの件数フォーマッターが見つかりません");
    }

    public static void verify(ClassDef type) {
        Method method = formatter(type);
        List<Instruction> code = ChatRecommendationsHook.code(method);
        if (method.getImplementation().getRegisterCount() != 13
                || !FINGERPRINT.equals(ChatRecommendationsHook.fingerprint(code))
                || !method.getImplementation().getTryBlocks().isEmpty()
                || !ChatRecommendationsHook.ref(code.get(GROUPED_INTEGER)).equals(TYPE + "->b:Ljava/text/NumberFormat;")
                || !ChatRecommendationsHook.ref(code.get(64)).equals("Ljava/text/NumberFormat;->format(J)Ljava/lang/String;")) {
            throw new IllegalStateException("Xの件数フォーマッターの構造が変わりました");
        }
    }

    public static void apply(MutableClass type) {
        verify(type);
        MutableMethodImplementation body = ((MutableMethod) formatter(type)).getImplementation();
        Label grouped = body.newLabelForIndex(GROUPED_INTEGER);
        // Keep v0 (units) and v1 (null) intact for both native branches. v2 is dead here.
        body.addInstruction(AFTER_INITIAL_LOCALS, new BuilderInstruction35c(Opcode.INVOKE_STATIC,
                0, 0, 0, 0, 0, 0, new ImmutableMethodReference(FullCountsPatch.OPTIONS,
                "enabled", Collections.emptyList(), "Z")));
        body.addInstruction(AFTER_INITIAL_LOCALS + 1, new BuilderInstruction11x(Opcode.MOVE_RESULT, 2));
        body.addInstruction(AFTER_INITIAL_LOCALS + 2, new BuilderInstruction21t(Opcode.IF_NEZ, 2, grouped));
    }
}
