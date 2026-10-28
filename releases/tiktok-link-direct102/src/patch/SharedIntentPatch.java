/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
package app.hiro.tiktoklink.patches;

import java.util.*;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;

/** Bind scope issued to a selector's temporary Intent to the actual native result.
 * Only runtime-owned pending identities can transfer. Native target construction,
 * final id values, route guards and all unprepared paths remain unchanged.
 */
public final class SharedIntentPatch {
    public static final String RUNTIME = "Lapp/hiro/tiktoklink/runtime/SharedLinkExit;";
    public static final String C_SHA256 = "d213cf81a6946e6a821dc3cb8a950c81eb1e51edec8b00c30e66bf2d01e49128";
    public static final String E_SHA256 = "d2ccbba5497229742ac41a34f04f135b13dc429bf9c417dbdf66076c3f1b198a";
    public static final String G_SHA256 = "5f972936701ceda81210030b763b3e2ff087fcf106d3c6d06f712c9b3c3d5657";
    private static final String INTENT = "Landroid/content/Intent;";
    private static final String STRING = "Ljava/lang/String;";
    private static final String SELECTOR = "LX/1SyD;->LIZLLL(Landroid/app/Activity;Landroid/content/Intent;Landroid/net/Uri;Ljava/lang/String;Ljava/lang/String;Z)Landroid/content/Intent;";
    private static final String PUT_ID = "Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;";
    private SharedIntentPatch() {}
    private static void require(boolean ok, String why) { TikTokLinkDirectPatch.require(ok, why); }

    static int at(List<Instruction> body, int pc) {
        int address = 0;
        for (int i = 0; i < body.size(); i++) {
            if (address == pc) return i;
            address += body.get(i).getCodeUnits();
        }
        throw new IllegalStateException("TikTok shared-link contract: native factory instruction missing at " + pc);
    }
    private static MutableMethod pinned(BytecodePatchContext c, String owner, String hash) {
        MutableMethod method = TikTokLinkDirectPatch.method(c, owner, "LIZ", INTENT,
            "Landroid/app/Activity;", "Landroid/net/Uri;", STRING, STRING, STRING, "Z");
        require(hash.equals(FastRoutePatch.fingerprint(method)), "native detail factory changed: " + owner);
        return method;
    }
    private static void alias(List<Instruction> body, int pc, int saved, int original) {
        Instruction i = body.get(at(body, pc));
        require(i.getOpcode() == Opcode.MOVE_OBJECT_FROM16
            && ((TwoRegisterInstruction)i).getRegisterA() == saved
            && ((TwoRegisterInstruction)i).getRegisterB() == original,
            "native selector temporary-Intent alias changed");
    }
    private static void selector(List<Instruction> body, int pc, int start) {
        Instruction i = body.get(at(body, pc));
        require(i.getOpcode() == Opcode.INVOKE_STATIC_RANGE && SELECTOR.equals(TikTokLinkDirectPatch.ref(i))
            && ((RegisterRangeInstruction)i).getStartRegister() == start
            && ((RegisterRangeInstruction)i).getRegisterCount() == 6,
            "native factory selector call changed");
    }
    private static void idKey(List<Instruction> body, int pc, int register) {
        Instruction i = body.get(at(body, pc));
        require(i.getOpcode() == Opcode.CONST_STRING
            && ((OneRegisterInstruction)i).getRegisterA() == register
            && "id".equals(TikTokLinkDirectPatch.ref(i)), "native final target key changed");
    }
    private static void idWrite(List<Instruction> body, int pc, int target, int key, int value) {
        Instruction i = body.get(at(body, pc));
        require(i.getOpcode() == Opcode.INVOKE_VIRTUAL && PUT_ID.equals(TikTokLinkDirectPatch.ref(i)),
            "native final target write changed");
        FiveRegisterInstruction args = (FiveRegisterInstruction)i;
        require(args.getRegisterCount() == 3 && args.getRegisterC() == target
            && args.getRegisterD() == key && args.getRegisterE() == value,
            "native final target-write operands changed");
    }
    private static void noIncomingBranch(List<Instruction> body, int pc) {
        int address = 0;
        for (Instruction i : body) {
            require(!(i instanceof OffsetInstruction)
                || address + ((OffsetInstruction)i).getCodeOffset() != pc,
                "native branch could bypass the Intent transfer");
            address += i.getCodeUnits();
        }
    }

    /** Check all three factories before changing any method body. */
    public static Plan prepare(BytecodePatchContext c) {
        MutableMethod normal = pinned(c, "LX/1SyC;", C_SHA256);
        MutableMethod detail = pinned(c, "LX/1SyE;", E_SHA256);
        MutableMethod item = pinned(c, "LX/1SyG;", G_SHA256);
        List<Instruction> nc = TikTokLinkDirectPatch.code(normal);
        List<Instruction> dc = TikTokLinkDirectPatch.code(detail);
        List<Instruction> ic = TikTokLinkDirectPatch.code(item);
        require(normal.getImplementation().getRegisterCount() == 57
            && detail.getImplementation().getRegisterCount() == 38
            && item.getImplementation().getRegisterCount() == 30,
            "native factory register layout changed");
        alias(nc, 0x035b, 39, 11); selector(nc, 0x0363, 38);
        alias(dc, 0x00d8, 26, 8); selector(dc, 0x00e0, 25);
        alias(ic, 0x006c, 18, 4); selector(ic, 0x006e, 17);
        idKey(nc, 0x01a6, 5); idWrite(nc, 0x052c, 11, 5, 10);
        idKey(dc, 0x0043, 1); idWrite(dc, 0x0124, 2, 1, 0);
        idKey(ic, 0x001c, 6); idWrite(ic, 0x007c, 4, 6, 5);
        int ni = at(nc, 0x052f), di = at(dc, 0x0127), ii = at(ic, 0x007f);
        require(ni == 665 && di == 153 && ii == 64, "native factory transfer boundary moved");
        require(nc.get(ni).getOpcode() == Opcode.CONST_STRING
            && ((OneRegisterInstruction)nc.get(ni)).getRegisterA() == 10
            && "from_uid".equals(TikTokLinkDirectPatch.ref(nc.get(ni))), "normal factory scratch register changed");
        require(dc.get(di).getOpcode() == Opcode.INVOKE_STATIC
            && "LX/1SyD;->LJIIIIZZ(Landroid/net/Uri;)Z".equals(TikTokLinkDirectPatch.ref(dc.get(di)))
            && ((FiveRegisterInstruction)dc.get(di)).getRegisterCount() == 1
            && ((FiveRegisterInstruction)dc.get(di)).getRegisterC() == 3
            && dc.get(di + 1).getOpcode() == Opcode.MOVE_RESULT
            && ((OneRegisterInstruction)dc.get(di + 1)).getRegisterA() == 0,
            "legacy detail factory scratch register changed");
        require(ic.get(ii).getOpcode() == Opcode.CONST_STRING
            && ((OneRegisterInstruction)ic.get(ii)).getRegisterA() == 0
            && "gd_label".equals(TikTokLinkDirectPatch.ref(ic.get(ii))), "legacy item factory scratch register changed");
        noIncomingBranch(nc, 0x052f); noIncomingBranch(dc, 0x0127); noIncomingBranch(ic, 0x007f);
        return new Plan(normal, detail, item, ni, di, ii);
    }

    public static final class Plan {
        private final MutableMethod normal, detail, item;
        private final int normalAt, detailAt, itemAt;
        private Plan(MutableMethod normal, MutableMethod detail, MutableMethod item, int normalAt, int detailAt, int itemAt) {
            this.normal = normal; this.detail = detail; this.item = item;
            this.normalAt = normalAt; this.detailAt = detailAt; this.itemAt = itemAt;
        }
        public void apply() {
            // C's ordinary owned route has push=false, so v39 still refers to the
            // temporary selector Intent when the final v11 has received its id.
            // E/G retain the preconstructed DetailActivity Intent as their result;
            // their saved aliases remain typed Intent on unprepared Home branches too.
            transfer(normal, normalAt, 39, 10, 11);
            transfer(detail, detailAt, 26, 0, 2);
            transfer(item, itemAt, 18, 0, 4);
            System.out.println("HIRO_LINK_DIRECT issued shared-link scope transferred to native C/E/G final Intents after exact target-id writes");
        }
    }

    private static void transfer(MutableMethod method, int at, int temporary, int scratch, int target) {
        method.getImplementation().addInstruction(at, new BuilderInstruction22x(Opcode.MOVE_OBJECT_FROM16, scratch, temporary));
        method.getImplementation().addInstruction(at + 1, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2,
            scratch, target, 0, 0, 0, new ImmutableMethodReference(RUNTIME, "transfer", Arrays.asList(INTENT, INTENT), "V")));
    }
}
