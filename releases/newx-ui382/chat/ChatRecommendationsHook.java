package app.hiro.newxui.patches;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** X 12.43.0-prod.01 chat recommendation list-items boundary. */
public final class ChatRecommendationsHook {
    public static final String TARGET = "Lcom/x/dm/convlist/j0;";
    public static final String TYPE = TARGET;
    public static final String RUNTIME = "Lapp/hiro/newxui/UiOptions;";
    public static final String SHA256 = "1df1b2d39a21823e0b9be1161d8af5ab60e15f79997101b41649103a844e2435";
    public static final int HOOK_INDEX = 136;
    public static final int SKIP_INDEX = 181;

    private ChatRecommendationsHook() {}

    public static Method target(ClassDef c) {
        if (c == null || !TARGET.equals(c.getType())) throw new IllegalStateException("Chat recommendations: target missing");
        Method result = null;
        for (Method m : c.getMethods()) {
            if (m.getName().equals("invoke") && m.getReturnType().equals("Ljava/lang/Object;") && m.getParameterTypes().equals(Collections.singletonList("Ljava/lang/Object;"))) {
                if (result != null) throw new IllegalStateException("Chat recommendations: ambiguous target");
                result = m;
            }
        }
        if (result == null || result.getImplementation() == null) throw new IllegalStateException("Chat recommendations: method missing");
        return result;
    }

    public static List<Instruction> code(Method m) {
        List<Instruction> result = new ArrayList<Instruction>();
        for (Instruction i : m.getImplementation().getInstructions()) result.add(i);
        return result;
    }

    public static String ref(Instruction i) {
        return i instanceof ReferenceInstruction ? ((ReferenceInstruction)i).getReference().toString() : "";
    }

    public static String fingerprint(List<? extends Instruction> instructions) {
        try {
            StringBuilder b = new StringBuilder();
            for (Instruction i : instructions) {
                b.append(i.getOpcode().name()).append(':');
                if (i instanceof FiveRegisterInstruction) {
                    FiveRegisterInstruction r = (FiveRegisterInstruction)i;
                    b.append(r.getRegisterCount()).append(',').append(r.getRegisterC()).append(',').append(r.getRegisterD()).append(',').append(r.getRegisterE()).append(',').append(r.getRegisterF()).append(',').append(r.getRegisterG());
                } else if (i instanceof RegisterRangeInstruction) {
                    RegisterRangeInstruction r = (RegisterRangeInstruction)i;
                    b.append(r.getStartRegister()).append(',').append(r.getRegisterCount());
                } else if (i instanceof ThreeRegisterInstruction) {
                    ThreeRegisterInstruction r = (ThreeRegisterInstruction)i;
                    b.append(r.getRegisterA()).append(',').append(r.getRegisterB()).append(',').append(r.getRegisterC());
                } else if (i instanceof TwoRegisterInstruction) {
                    TwoRegisterInstruction r = (TwoRegisterInstruction)i;
                    b.append(r.getRegisterA()).append(',').append(r.getRegisterB());
                } else if (i instanceof OneRegisterInstruction) b.append(((OneRegisterInstruction)i).getRegisterA());
                b.append(':').append(ref(i));
                if (i instanceof WideLiteralInstruction) b.append(':').append(((WideLiteralInstruction)i).getWideLiteral());
                if (i instanceof OffsetInstruction) b.append(':').append(((OffsetInstruction)i).getCodeOffset());
                b.append('\n');
            }
            StringBuilder out = new StringBuilder();
            for (byte n : MessageDigest.getInstance("SHA-256").digest(b.toString().getBytes(StandardCharsets.UTF_8))) out.append(String.format("%02x", n & 255));
            return out.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public static void verify(ClassDef c) {
        Method m = target(c);
        List<Instruction> a = code(m);
        if (m.getImplementation().getRegisterCount() != 27 || a.size() != 183 || !SHA256.equals(fingerprint(a))) throw new IllegalStateException("Chat recommendations: verified native boundary changed or already patched");
        if (a.get(HOOK_INDEX).getOpcode() != Opcode.IF_EQZ || ((OneRegisterInstruction)a.get(HOOK_INDEX)).getRegisterA() != 22 || ((OffsetInstruction)a.get(HOOK_INDEX)).getCodeOffset() != 103) throw new IllegalStateException("Chat recommendations: native section guard changed");
        if (!ref(a.get(HOOK_INDEX + 1)).equals("cold-inbox-suggestions-title") || !ref(a.get(149)).equals("cold-inbox-find-friends") || !ref(a.get(158)).equals("cold-inbox-invite-friends") || !ref(a.get(162)).equals("cold-inbox-suggestions-loading")) throw new IllegalStateException("Chat recommendations: item keys changed");
        if (a.get(SKIP_INDEX).getOpcode() != Opcode.SGET_OBJECT || !ref(a.get(SKIP_INDEX)).equals("Lkotlin/Unit;->a:Lkotlin/Unit;") || a.get(SKIP_INDEX + 1).getOpcode() != Opcode.RETURN_OBJECT) throw new IllegalStateException("Chat recommendations: end boundary changed");
    }

    public static void apply(MutableClass c) {
        verify(c);
        MutableMethod m = (MutableMethod)target(c);
        MutableMethodImplementation impl = m.getImplementation();
        Label skip = impl.newLabelForIndex(SKIP_INDEX);
        // v6 is dead here: the native enabled path immediately overwrites it with
        // the section key, and the native disabled path immediately returns Unit.
        impl.addInstruction(HOOK_INDEX, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0,
            new ImmutableMethodReference(RUNTIME, "hideChatRecommendations", Collections.<String>emptyList(), "Z")));
        impl.addInstruction(HOOK_INDEX + 1, new BuilderInstruction11x(Opcode.MOVE_RESULT, 6));
        impl.addInstruction(HOOK_INDEX + 2, new BuilderInstruction21t(Opcode.IF_NEZ, 6, skip));
    }

    public static void apply(BytecodePatchContext c) {
        verify(c.classDefBy(TARGET));
        apply(c.mutableClassDefBy(TARGET));
    }
}
