/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
package app.hiro.tiktoklink.patches;

import java.util.*;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Disable only the dedicated shared-video-popup eligibility lazy value.
 * Its three native consumers retain their existing no-dialog routing and cleanup.
 * No player, general dialog, raw experiment, link URI or account state is modified.
 * Signed global 47.4.3 APK SHA256:
 * af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b.
 */
public final class TikTokLinkDirectPatch {
    public static final String NAME = "共有リンクの小窓を省略して動画を直接再生（TikTok 47.4.3）";
    public static final String TARGET = "LX/0ICN;";
    private static final String GATE = "LX/0ICM;->LIZIZ:LX/02o5;";
    private TikTokLinkDirectPatch() {}

    public static Set<Pair<String, Set<String>>> compatibility() {
        return Collections.singleton(new Pair<String, Set<String>>(
            "com.zhiliaoapp.musically", Collections.singleton("47.4.3")));
    }
    public static void validateTarget(BytecodePatchContext c) {
        require("com.zhiliaoapp.musically".equals(c.getPackageMetadata().getPackageName())
            && "47.4.3".equals(c.getPackageMetadata().getVersionName()),
            "requires original global TikTok 47.4.3; other versions are not supported");
    }
    public static final BytecodePatch DIRECT_SHARED_VIDEO = new BytecodePatch(
        NAME,
        "共有URLを開いた際の再生ボタン・フォロー案内付き小窓を省略し、TikTok本来の動画表示へ進みます。共有動画の小窓専用判定だけを変更し、通常・従来の共有リンク経路に対応します。",
        true, compatibility(), Collections.<Patch<?>>emptySet(), Collections.emptySet(), null,
        new Function1<BytecodePatchContext, Unit>() {
            public Unit invoke(BytecodePatchContext c) { apply(c); return Unit.INSTANCE; }
        }, null);

    static void require(boolean ok, String why) {
        if (!ok) throw new IllegalStateException("TikTok shared-link contract: " + why);
    }
    static String ref(Instruction i) {
        return i instanceof ReferenceInstruction ? ((ReferenceInstruction)i).getReference().toString() : "";
    }
    static List<Instruction> code(Method m) {
        require(m.getImplementation() != null, "method has no code");
        List<Instruction> out = new ArrayList<>();
        for (Instruction i : m.getImplementation().getInstructions()) out.add(i);
        return out;
    }
    static boolean contains(List<Instruction> body, String value) {
        for (Instruction i : body) if (ref(i).equals(value)) return true;
        return false;
    }
    static MutableMethod method(BytecodePatchContext c, String owner, String name, String result, String... params) {
        MutableClass cl = c.mutableClassDefByOrNull(owner);
        require(cl != null, "missing class " + owner);
        MutableMethod found = null;
        for (MutableMethod m : cl.getMethods()) {
            if (m.getName().equals(name) && m.getReturnType().equals(result)
                && m.getParameterTypes().equals(Arrays.asList(params))) {
                require(found == null, "ambiguous method " + owner + name); found = m;
            }
        }
        require(found != null && found.getImplementation() != null, "missing method " + owner + name);
        return found;
    }
    static void one(Instruction i, int a) {
        require(i instanceof OneRegisterInstruction && ((OneRegisterInstruction)i).getRegisterA() == a, "unexpected result register");
    }
    static void invoke(Instruction i, String reference) {
        require(i instanceof FiveRegisterInstruction && ((FiveRegisterInstruction)i).getRegisterCount() == 1
            && ((FiveRegisterInstruction)i).getRegisterC() == 0 && ref(i).equals(reference), "unexpected invoke operands");
    }
    static void consumer(BytecodePatchContext c, String owner, String name, String result, String marker, String... params) {
        List<Instruction> body = code(method(c, owner, name, result, params));
        int reads = 0;
        for (Instruction i : body) if (ref(i).equals(GATE)) reads++;
        require(reads == 1 && contains(body, marker), "shared-video eligibility consumer changed: " + owner);
    }

    /** Complete preflight before a single CONST_4 1 -> 0 replacement. */
    public static void apply(BytecodePatchContext c) {
        validateTarget(c);
        MutableMethod target = method(c, TARGET, "invoke", "Ljava/lang/Object;");
        List<Instruction> body = code(target);
        Opcode[] expected = {Opcode.SGET_OBJECT, Opcode.INVOKE_INTERFACE, Opcode.MOVE_RESULT_OBJECT,
            Opcode.CHECK_CAST, Opcode.INVOKE_VIRTUAL, Opcode.MOVE_RESULT, Opcode.CONST_4, Opcode.IF_NE,
            Opcode.CONST_4, Opcode.INVOKE_STATIC, Opcode.MOVE_RESULT_OBJECT, Opcode.RETURN_OBJECT,
            Opcode.CONST_4, Opcode.GOTO};
        require(target.getImplementation().getRegisterCount() == 3 && body.size() == expected.length,
            "dedicated eligibility getter shape changed");
        require(target.getImplementation().getTryBlocks().isEmpty(), "unexpected getter handlers");
        for (int i = 0; i < expected.length; i++) require(body.get(i).getOpcode() == expected[i], "getter opcode changed at " + i);
        require(ref(body.get(0)).equals("LX/17u4;->LIZ:LX/02o5;"), "raw popup-style experiment changed");
        one(body.get(0),0);
        invoke(body.get(1),"LX/02o5;->getValue()Ljava/lang/Object;"); one(body.get(2),0);
        one(body.get(3),0); require(ref(body.get(3)).equals("Ljava/lang/Number;"), "experiment type changed");
        invoke(body.get(4),"Ljava/lang/Number;->intValue()I"); one(body.get(5),1); one(body.get(6),0);
        require(((NarrowLiteralInstruction)body.get(6)).getNarrowLiteral() == 4, "popup-style selector changed");
        require(((TwoRegisterInstruction)body.get(7)).getRegisterA() == 1
            && ((TwoRegisterInstruction)body.get(7)).getRegisterB() == 0
            && ((OffsetInstruction)body.get(7)).getCodeOffset() == 8, "eligibility branch changed");
        one(body.get(8),0);
        require(((NarrowLiteralInstruction)body.get(8)).getNarrowLiteral() == 1, "eligibility already altered");
        invoke(body.get(9),"Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;");
        one(body.get(10),0); one(body.get(11),0); one(body.get(12),0);
        require(((NarrowLiteralInstruction)body.get(12)).getNarrowLiteral() == 0
            && ((OffsetInstruction)body.get(13)).getCodeOffset() == -6, "native false return changed");
        List<Instruction> init = code(method(c,"LX/0ICM;","<clinit>","V"));
        int initializer = -1;
        for (int i = 0; i < init.size(); i++) if (init.get(i).getOpcode() == Opcode.NEW_INSTANCE && ref(init.get(i)).equals(TARGET)) {
            require(initializer == -1,"duplicate eligibility initializer"); initializer = i;
        }
        require(initializer >= 0 && initializer + 4 < init.size(), "dedicated eligibility initializer missing");
        require(ref(init.get(initializer+1)).equals(TARGET+"-><init>()V")
            && ref(init.get(initializer+2)).equals("LX/02o3;->LIZJ(Lkotlin/jvm/functions/Function0;)LX/02o5;")
            && init.get(initializer+4).getOpcode() == Opcode.SPUT_OBJECT && ref(init.get(initializer+4)).equals(GATE),
            "getter no longer initializes shared-video popup eligibility");
        consumer(c,"LX/0aK3;","invokeSuspend","Ljava/lang/Object;",
            "SharedVideoDeepLinkHelper@a50d.collectSharerState$1$1","Ljava/lang/Object;");
        consumer(c,"Lcom/ss/android/ugc/aweme/feed/helper/RecFeedShareComponentController;","LJ","Z",
            "not intercept shared logic. ","LX/02Hm;");
        consumer(c,"LX/07yL;","onChanged","V",
            "Lcom/ss/android/ugc/aweme/feed/ui/FeedRecommendFragment;->OX(Ljava/lang/Boolean;)V","Ljava/lang/Object;");
        target.getImplementation().replaceInstruction(8,new BuilderInstruction11n(Opcode.CONST_4,0,0));
        System.out.println("HIRO_LINK_DIRECT shared-video popup eligibility disabled; one CONST_4 at 0x000f; three native consumers retained");
    }
}
