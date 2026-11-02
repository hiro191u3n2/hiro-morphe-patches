package app.hiro.berryshare.patches;

import app.hiro.berry.patches.BerryProfiles;
import app.hiro.berry.patches.BerryProfiles.Profile;
import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.patch.PatchKt;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import app.morphe.patcher.util.smali.ExternalLabel;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.BuilderInstruction;
import com.android.tools.smali.dexlib2.builder.Label;
import com.android.tools.smali.dexlib2.builder.MethodLocation;
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import kotlin.Unit;

public final class BrowserVisibilityPatch {
    public static final String NAME = "共有先ブラウザの表示を個別に切替";
    public static final String EXTENSION = "extensions/berry_browser_visibility.mpe";
    private static final InstructionExtensions I = InstructionExtensions.INSTANCE;
    private static final String HELPER = "LHiroBerryBrowserChooser;";

    private static final BytecodePatch CHECK = PatchKt.bytecodePatch(null, null, false, builder -> {
        builder.compatibleWith(BerryProfiles.TARGET);
        builder.execute(context -> {
            BerryProfiles.require(context.classDefByOrNull(c -> ShareRuntime.isHelper(c.getType())) == null,
                "表示切替は適用済みです。未改造の APK を選んでください");
            Profile profile = BerryProfiles.select(context);
            ShareRuntime.checkTargets(context, profile);
            anchors(profile.checked(context, OriginalContracts.METHODS), profile);
            return Unit.INSTANCE;
        });
        return Unit.INSTANCE;
    });

    public static final BytecodePatch PATCH = PatchKt.bytecodePatch(NAME,
        "「他のブラウザで開く」の一覧に「表示するブラウザ」を追加します。ブラウザごとに表示をオン・オフでき、設定は再起動後も保持します。すべて非表示にしても設定ボタンから戻せます。",
        true, builder -> {
            builder.compatibleWith(BerryProfiles.TARGET);
            builder.dependsOn(CHECK);
            builder.extendWith(() -> BerryProfiles.extension(BrowserVisibilityPatch.class, EXTENSION));
            builder.execute(context -> { apply(context); return Unit.INSTANCE; });
            return Unit.INSTANCE;
        });

    private BrowserVisibilityPatch() {}

    private static void apply(BytecodePatchContext context) {
        Profile profile = BerryProfiles.select(context);
        ShareRuntime.checkTargets(context, profile);
        Map<String, MutableMethod> methods = profile.checked(context, OriginalContracts.METHODS);
        Anchors a = anchors(methods, profile);
        ShareRuntime.adapt(context, profile);
        MutableMethod view = methods.get(BerryProfiles.SHARE_VIEW);
        MutableMethod list = methods.get(BerryProfiles.SHARE_LIST);
        I.addInstructions(view, a.adapter + 1,
            "invoke-static/range {v" + a.viewDialogRegister + " .. v" + a.viewDialogRegister + "}, " + HELPER + "->attach(" + profile.type("Lb03;") + ")V");
        BuilderInstruction original = list.getImplementation().getInstructions().get(a.empty);
        List<Label> incoming = new ArrayList<>(original.getLocation().getLabels());
        I.addInstructionsWithLabels(list, a.empty,
            updateCall(a, list.getImplementation().getRegisterCount(), profile) + "goto :chooser_done\n",
            new ExternalLabel("chooser_done", list.getImplementation().getInstructions().get(a.returned)));
        MethodLocation inserted = list.getImplementation().getInstructions().get(a.empty).getLocation();
        original.getLocation().getLabels().removeAll(incoming);
        inserted.getLabels().addAll(incoming);
    }

    static final class Anchors {
        final int adapter, empty, returned, viewDialogRegister, dialogRegister, listRegister, returnRegister;
        Anchors(int a, int e, int r, int viewDialog, int dialog, int list, int returnedRegister) {
            adapter = a; empty = e; returned = r; dialogRegister = dialog;
            viewDialogRegister = viewDialog; listRegister = list; returnRegister = returnedRegister;
        }
    }

    static Anchors anchors(Map<String, MutableMethod> methods, Profile profile) {
        MutableMethod view = methods.get(BerryProfiles.SHARE_VIEW);
        BerryProfiles.require((view.getAccessFlags() & 8) == 0, "共有画面のインスタンス処理が異なります");
        int adapter = BerryProfiles.unique(view, profile.ref("Landroidx/recyclerview/widget/RecyclerView;->setAdapter(Lal4;)V"));
        int[] adapterArgs = invoke(view, adapter, Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE, 2);
        int self = view.getImplementation().getRegisterCount() - 3;
        BerryProfiles.require(self >= 0, "共有画面の引数構造が異なります");
        int adapterField = latestWrite(view, adapter, adapterArgs[1]);
        BerryProfiles.require(adapterField >= 0, "共有画面のアダプター元参照がありません");
        Instruction af = view.getImplementation().getInstructions().get(adapterField);
        BerryProfiles.require(af.getOpcode() == Opcode.IGET_OBJECT && af instanceof TwoRegisterInstruction,
            "共有画面のアダプター参照を確認できません");
        int viewDialogRegister = ((TwoRegisterInstruction) af).getRegisterB();
        BerryProfiles.require(originatesFromSelf(view, adapterField, viewDialogRegister, self)
            && reference(af).equals(profile.ref("Lb03;->M0:Lz32;")), "共有画面のアダプターが異なります");
        noWrite(view, adapterField, adapter + 1, viewDialogRegister, "共有画面の自身参照が上書きされています");
        MutableMethod list = methods.get(BerryProfiles.SHARE_LIST);
        int cast = BerryProfiles.unique(list, profile.type("Lb03;"));
        int query = BerryProfiles.unique(list, profile.ref("Ltp0;->c(Landroid/content/Intent;Lyx0;)Ljava/lang/Object;"));
        int empty = BerryProfiles.unique(list, "Ljava/util/List;->isEmpty()Z");
        int field = BerryProfiles.unique(list, profile.ref("Lb03;->M0:Lz32;"));
        int update = BerryProfiles.unique(list, profile.ref("Lfy;->O(Ljava/util/List;)V"));
        BerryProfiles.require(cast < query && query < empty && empty < field && field < update,
            "共有一覧の処理順序が異なります");
        int dialogRegister = register(list, cast, Opcode.CHECK_CAST);
        invoke(list, query, Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE, 3);
        int listRegister = invoke(list, empty, Opcode.INVOKE_INTERFACE, Opcode.INVOKE_INTERFACE_RANGE, 1)[0];
        Instruction fieldInstruction = list.getImplementation().getInstructions().get(field);
        BerryProfiles.require(fieldInstruction.getOpcode() == Opcode.IGET_OBJECT && fieldInstruction instanceof TwoRegisterInstruction,
            "共有一覧のアダプター取得形式が異なります");
        TwoRegisterInstruction load = (TwoRegisterInstruction) fieldInstruction;
        BerryProfiles.require(load.getRegisterB() == dialogRegister, "共有一覧の画面参照が異なります");
        int[] updateArgs = invoke(list, update, Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE, 2);
        BerryProfiles.require(updateArgs[0] == load.getRegisterA() && updateArgs[1] == listRegister,
            "共有一覧のアダプターまたは一覧引数が異なります");
        int returnRegister = register(list, update + 1, Opcode.RETURN_OBJECT);
        BerryProfiles.require(returnRegister != dialogRegister && returnRegister != listRegister,
            "共有一覧の復帰値と入力参照が重複しています");
        noWrite(list, cast + 1, empty, dialogRegister, "共有一覧の画面参照が上書きされています");
        noWrite(list, empty, update + 1, returnRegister, "共有一覧の復帰値が分岐内で上書きされています");
        noWrite(list, field + 1, update, load.getRegisterA(), "共有一覧のアダプター参照が上書きされています");
        return new Anchors(adapter, empty, update + 1, viewDialogRegister, dialogRegister, listRegister, returnRegister);
    }

    private static String reference(Instruction i) {
        BerryProfiles.require(i instanceof com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction,
            "共有一覧の参照命令が異なります");
        return com.android.tools.smali.dexlib2.formatter.DexFormatter.INSTANCE.getReference(
            ((com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction) i).getReference());
    }

    private static int latestWrite(MutableMethod method, int before, int register) {
        for (int i = before - 1; i >= 0; i--) if (writes(method.getImplementation().getInstructions().get(i), register)) return i;
        return -1;
    }
    private static boolean originatesFromSelf(MutableMethod method, int before, int register, int self) {
        int written = latestWrite(method, before, register);
        if (written < 0) return register == self;
        Instruction i = method.getImplementation().getInstructions().get(written);
        if (i.getOpcode() == Opcode.CHECK_CAST) return originatesFromSelf(method, written, register, self);
        if ((i.getOpcode() == Opcode.MOVE_OBJECT || i.getOpcode() == Opcode.MOVE_OBJECT_FROM16
                || i.getOpcode() == Opcode.MOVE_OBJECT_16) && i instanceof TwoRegisterInstruction)
            return originatesFromSelf(method, written, ((TwoRegisterInstruction) i).getRegisterB(), self);
        return false;
    }
    private static boolean writes(Instruction i, int register) {
        if (!i.getOpcode().setsRegister() || !(i instanceof OneRegisterInstruction)) return false;
        int a = ((OneRegisterInstruction) i).getRegisterA();
        return a == register || i.getOpcode().setsWideRegister() && a + 1 == register;
    }
    private static void noWrite(MutableMethod method, int from, int to, int register, String message) {
        for (int i = from; i < to; i++) BerryProfiles.require(!writes(method.getImplementation().getInstructions().get(i), register), message);
    }
    private static int register(MutableMethod method, int at, Opcode opcode) {
        BerryProfiles.require(at >= 0 && at < method.getImplementation().getInstructions().size(), "共有一覧の復帰先がありません");
        Instruction i = method.getImplementation().getInstructions().get(at);
        BerryProfiles.require(i.getOpcode() == opcode && i instanceof OneRegisterInstruction, "共有一覧の画面参照または復帰先が異なります");
        return ((OneRegisterInstruction) i).getRegisterA();
    }
    private static int[] invoke(MutableMethod method, int position, Opcode normal, Opcode range, int count) {
        Instruction instruction = method.getImplementation().getInstructions().get(position);
        if (instruction.getOpcode() == normal && instruction instanceof FiveRegisterInstruction) {
            FiveRegisterInstruction i = (FiveRegisterInstruction) instruction;
            BerryProfiles.require(i.getRegisterCount() == count, "共有一覧の呼び出し引数数が異なります");
            int[] values = {i.getRegisterC(), i.getRegisterD(), i.getRegisterE(), i.getRegisterF(), i.getRegisterG()};
            return java.util.Arrays.copyOf(values, count);
        }
        BerryProfiles.require(instruction.getOpcode() == range && instruction instanceof RegisterRangeInstruction,
            "共有一覧の呼び出し形式が異なります");
        RegisterRangeInstruction i = (RegisterRangeInstruction) instruction;
        BerryProfiles.require(i.getRegisterCount() == count, "共有一覧の呼び出し引数数が異なります");
        int[] result = new int[count];
        for (int n = 0; n < count; n++) result[n] = i.getStartRegister() + n;
        return result;
    }
    private static String updateCall(Anchors a, int total, Profile profile) {
        String target = HELPER + "->update(" + profile.type("Lb03;") + "Ljava/util/List;)V\n";
        if (a.dialogRegister < 16 && a.listRegister < 16)
            return "invoke-static {v" + a.dialogRegister + ", v" + a.listRegister + "}, " + target;
        // The inserted branch goes straight to return-object. Preserve that live
        // value and both inputs while borrowing two dead low registers for 35c.
        int first = -1, second = -1;
        for (int r = 0; r < Math.min(total, 16); r++) {
            if (r == a.returnRegister || r == a.dialogRegister || r == a.listRegister) continue;
            if (first < 0) first = r; else { second = r; break; }
        }
        BerryProfiles.require(second >= 0, "共有一覧の引数転送レジスタを確保できません");
        return "move-object/from16 v" + first + ", v" + a.dialogRegister + "\nmove-object/from16 v" + second + ", v" + a.listRegister +
            "\ninvoke-static {v" + first + ", v" + second + "}, " + target;
    }
}
