/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
package app.hiro.tiktoklink.patches;

import java.util.*;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;

/** Suppress the native detail-window slide only for an already prepared Intent.
 * This changes presentation, not URL resolution, network requests or player startup.
 */
public final class SharedTransitionPatch {
    public static final String OWNER = "LX/1SxW;";
    public static final String RUNTIME = "Lapp/hiro/tiktoklink/runtime/SharedLinkExit;";
    public static final String SHA256 = "df6c8aa5503ae888194c3340da5b9e660f526554c854b1b00fa3bddb6523c2be";
    private static final String INTENT = "Landroid/content/Intent;";
    private static final String ANIMATION = "LX/00Dv;->LIZIZ(IILandroid/content/Context;)LX/01Yv;";
    private SharedTransitionPatch() {}
    private static void require(boolean ok, String why) { TikTokLinkDirectPatch.require(ok, why); }

    /** Read-only admission; the owning patch also admits the runtime before mutation. */
    public static Plan prepare(BytecodePatchContext c) {
        MutableMethod method = TikTokLinkDirectPatch.method(c, OWNER, "LIZJ", "Lkotlin/Pair;",
            "LX/1Sxc;", "Ljava/util/HashMap;", "LX/15dt;", "LX/1Sxm;");
        require(SHA256.equals(FastRoutePatch.fingerprint(method)), "native detail-transition method changed");
        List<Instruction> body = TikTokLinkDirectPatch.code(method);
        int at = SharedIntentPatch.at(body, 0x0127);
        require(at == 149 && method.getImplementation().getRegisterCount() == 26,
            "native detail-transition register or instruction layout changed");
        Instruction resource = body.get(at - 1), call = body.get(at);
        require(resource.getOpcode() == Opcode.CONST
            && ((OneRegisterInstruction)resource).getRegisterA() == 0
            && ((NarrowLiteralInstruction)resource).getNarrowLiteral() == 0x7f0200af,
            "native detail animation resource changed");
        require(call.getOpcode() == Opcode.INVOKE_STATIC && ANIMATION.equals(TikTokLinkDirectPatch.ref(call)),
            "native detail animation call changed");
        FiveRegisterInstruction args = (FiveRegisterInstruction)call;
        require(args.getRegisterCount() == 3 && args.getRegisterC() == 0
            && args.getRegisterD() == 0 && args.getRegisterE() == 7,
            "native detail animation arguments changed");
        Instruction result = body.get(at + 1);
        require(result.getOpcode() == Opcode.MOVE_RESULT_OBJECT
            && ((OneRegisterInstruction)result).getRegisterA() == 18,
            "native detail animation result moved");
        Instruction nextScratchWrite = body.get(SharedIntentPatch.at(body, 0x012d));
        require(nextScratchWrite.getOpcode() == Opcode.CONST_STRING
            && ((OneRegisterInstruction)nextScratchWrite).getRegisterA() == 6
            && "is_ddl".equals(TikTokLinkDirectPatch.ref(nextScratchWrite)),
            "detail-transition scratch register is no longer dead");
        return new Plan(method, at);
    }

    public static final class Plan {
        private final MutableMethod method;
        private final int at;
        private Plan(MutableMethod method, int at) { this.method = method; this.at = at; }
        public void apply() {
            MutableMethodImplementation body = method.getImplementation();
            Label original = body.newLabelForIndex(at);
            body.addInstruction(at, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1,
                12, 0, 0, 0, 0, new ImmutableMethodReference(RUNTIME, "isPrepared",
                    Collections.singletonList(INTENT), "Z")));
            body.addInstruction(at + 1, new BuilderInstruction11x(Opcode.MOVE_RESULT, 6));
            body.addInstruction(at + 2, new BuilderInstruction21t(Opcode.IF_EQZ, 6, original));
            body.addInstruction(at + 3, new BuilderInstruction11n(Opcode.CONST_4, 0, 0));
            System.out.println("HIRO_LINK_DIRECT prepared shared-detail Intent uses native custom animation 0/0; other transitions retained");
        }
    }
}
