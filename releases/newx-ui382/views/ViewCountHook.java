package app.hiro.newxui.patches;

import java.util.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Guarded X 12.43.0-prod.01 inline ViewCount label hook. */
public final class ViewCountHook {
    public static final String TYPE = "Lcom/x/inlineactionbar/w;";
    public static final String BRIDGE = "hiroFullViewCount";
    public static final String MODEL = "Lcom/x/models/n4;";
    public static final String ACTION = "Lcom/x/models/x5;";
    public static final String RESOURCES = "Landroid/content/res/Resources;";
    public static final String RUNTIME = "Lapp/hiro/newxui/ViewCountFormat;";
    public static final String SHA256 = "e93ba7d10766eb9a2149f83bcab2c7ba9757473b9aa1c91a4a574ee36762bf8c";
    public static final int START = 448, END = 456;
    private static final List<String> PARAMETERS = Arrays.asList(
        "Lcom/x/inlineactionbar/r1;", "Landroidx/compose/ui/Modifier;", "J", "F",
        "Lcom/x/inlineactionbar/t1;", "Landroidx/compose/foundation/layout/c3;",
        "Ldev/chrisbanes/haze/a1;", "Landroidx/compose/ui/graphics/z;", "J", "Z", "Z", "J",
        "Lkotlin/jvm/functions/Function1;", "Lcom/x/inlineactionbar/moneylike/q0;",
        "Landroidx/compose/runtime/Composer;", "I", "I", "I");
    private ViewCountHook() {}

    public static Method target(ClassDef c) {
        if (c == null || !TYPE.equals(c.getType())) throw new IllegalStateException("Full view counts: target missing");
        Method found = null;
        for (Method m : c.getMethods()) if (m.getName().equals("c") && m.getReturnType().equals("V") && m.getParameterTypes().equals(PARAMETERS)) {
            if (found != null) throw new IllegalStateException("Full view counts: ambiguous renderer");
            found = m;
        }
        if (found == null || found.getImplementation() == null) throw new IllegalStateException("Full view counts: renderer missing");
        return found;
    }
    public static List<Instruction> code(Method m) { return ChatRecommendationsHook.code(m); }
    public static String fingerprint(List<? extends Instruction> a) { return ChatRecommendationsHook.fingerprint(a); }
    public static String ref(Instruction i) { return ChatRecommendationsHook.ref(i); }

    public static void verify(ClassDef c) {
        Method m = target(c);
        List<Instruction> a = code(m);
        for (Method n : c.getMethods()) if (n.getName().equals(BRIDGE)) throw new IllegalStateException("Full view counts: bridge already present");
        if (m.getImplementation().getRegisterCount() != 61 || a.size() != 768 || !SHA256.equals(fingerprint(a)))
            throw new IllegalStateException("Full view counts: native renderer changed or already patched");
        if (!m.getImplementation().getTryBlocks().isEmpty()) throw new IllegalStateException("Full view counts: exception boundaries changed");
        if (a.get(447).getOpcode() != Opcode.CHECK_CAST || !ref(a.get(447)).equals(MODEL)
            || !ref(a.get(448)).equals(MODEL + "->b:Ljava/lang/Long;")
            || !ref(a.get(450)).equals("Ljava/lang/Number;->longValue()J")
            || !ref(a.get(452)).equals("Lcom/x/android/main/landing/f;->x0(J" + RESOURCES + ")Ljava/lang/String;")
            || !ref(a.get(456)).equals("Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z"))
            throw new IllegalStateException("Full view counts: native raw-count boundary changed");
        // No external control-flow entry is allowed into the replaced native block.
        int[] pc = new int[a.size() + 1];
        for (int i = 0; i < a.size(); i++) pc[i + 1] = pc[i] + a.get(i).getCodeUnits();
        for (int i = 0; i < a.size(); i++) if ((i < START || i >= END) && a.get(i) instanceof OffsetInstruction) {
            int to = pc[i] + ((OffsetInstruction)a.get(i)).getCodeOffset();
            if (to > pc[START] && to < pc[END]) throw new IllegalStateException("Full view counts: external entry into replaced block");
        }
    }

    /** Native null and abbreviated formatting path, with a ViewCount-only final override. */
    public static ImmutableMethod bridge() {
        MethodImplementationBuilder b = new MethodImplementationBuilder(6);
        // v4=model, v5=Resources, v0=raw boxed Long, v1/v2=wide raw count.
        b.addInstruction(new BuilderInstruction22c(Opcode.IGET_OBJECT, 0, 4, new ImmutableFieldReference(MODEL, "b", "Ljava/lang/Long;")));
        b.addInstruction(new BuilderInstruction21t(Opcode.IF_NEZ, 0, b.getLabel("nonNull")));
        b.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 0, 0));
        b.addInstruction(new BuilderInstruction11x(Opcode.RETURN_OBJECT, 0));
        b.addLabel("nonNull");
        b.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0,
            new ImmutableMethodReference("Ljava/lang/Number;", "longValue", Collections.<String>emptyList(), "J")));
        b.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT_WIDE, 1));
        b.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 3, 1, 2, 5, 0, 0,
            new ImmutableMethodReference("Lcom/x/android/main/landing/f;", "x0", Arrays.asList("J", RESOURCES), "Ljava/lang/String;")));
        b.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1));
        // Both halves of the wide temporary are overwritten before reference use.
        b.addInstruction(new BuilderInstruction22c(Opcode.IGET_OBJECT, 2, 4, new ImmutableFieldReference(MODEL, "a", ACTION)));
        b.addInstruction(new BuilderInstruction21c(Opcode.SGET_OBJECT, 3, new ImmutableFieldReference(ACTION, "ViewCount", ACTION)));
        b.addInstruction(new BuilderInstruction22t(Opcode.IF_NE, 2, 3, b.getLabel("result")));
        b.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2, 0, 1, 0, 0, 0,
            new ImmutableMethodReference(RUNTIME, "format", Arrays.asList("Ljava/lang/Long;", "Ljava/lang/String;"), "Ljava/lang/String;")));
        b.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1));
        b.addLabel("result");
        b.addInstruction(new BuilderInstruction11x(Opcode.RETURN_OBJECT, 1));
        return new ImmutableMethod(TYPE, BRIDGE,
            Arrays.asList(new ImmutableMethodParameter(MODEL, Collections.emptySet(), null), new ImmutableMethodParameter(RESOURCES, Collections.emptySet(), null)),
            "Ljava/lang/String;", 0x100a, Collections.emptySet(), Collections.emptySet(), b.getMethodImplementation());
    }

    public static void apply(MutableClass c) {
        verify(c);
        MutableMethodImplementation impl = ((MutableMethod)target(c)).getImplementation();
        // v8 still contains n4 before the original count-field read. v3 is Resources.
        // The bridge returns the same nullable String into the native result register v7.
        impl.replaceInstruction(START, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2, 8, 3, 0, 0, 0,
            new ImmutableMethodReference(TYPE, BRIDGE, Arrays.asList(MODEL, RESOURCES), "Ljava/lang/String;")));
        impl.replaceInstruction(START + 1, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 7));
        for (int i = START + 2; i < END; i++) impl.removeInstruction(START + 2);
        c.getMethods().add(new MutableMethod(bridge()));
    }
}
