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
            builder.setExtensionInputStream(() -> BerryProfiles.extension(BrowserVisibilityPatch.class, EXTENSION));
            builder.execute(context -> { apply(context); return Unit.INSTANCE; });
            return Unit.INSTANCE;
        });

    private BrowserVisibilityPatch() {}

    private static void apply(BytecodePatchContext context) {
        Profile profile = BerryProfiles.select(context);
        ShareRuntime.checkTargets(context, profile);
        Map<String, MutableMethod> methods = profile.checked(context, OriginalContracts.METHODS);
        int[] positions = anchors(methods, profile);
        // Only injected helper definitions are rewritten. Original application
        // methods remain untouched until every target and helper reference passes.
        ShareRuntime.adapt(context, profile);
        MutableMethod view = methods.get(BerryProfiles.SHARE_VIEW);
        MutableMethod list = methods.get(BerryProfiles.SHARE_LIST);
        I.addInstructions(view, positions[0] + 1,
            "invoke-static {p0}, " + HELPER + "->attach(" + profile.type("Lb03;") + ")V");
        BuilderInstruction original = list.getImplementation().getInstructions().get(positions[1]);
        List<Label> incoming = new ArrayList<>(original.getLocation().getLabels());
        I.addInstructionsWithLabels(list, positions[1],
            "invoke-static {v" + profile.shareDialogRegister + ", v0}, " + HELPER + "->update(" +
            profile.type("Lb03;") + "Ljava/util/List;)V\ngoto :chooser_done\n",
            new ExternalLabel("chooser_done", list.getImplementation().getInstructions().get(positions[2])));
        MethodLocation inserted = list.getImplementation().getInstructions().get(positions[1]).getLocation();
        original.getLocation().getLabels().removeAll(incoming);
        inserted.getLabels().addAll(incoming);
    }

    private static int[] anchors(Map<String, MutableMethod> methods, Profile profile) {
        MutableMethod view = methods.get(BerryProfiles.SHARE_VIEW);
        int adapter = BerryProfiles.unique(view, profile.ref("Landroidx/recyclerview/widget/RecyclerView;->setAdapter(Lal4;)V"));
        assertInvoke(view, adapter, Opcode.INVOKE_VIRTUAL, 14, 15);
        BerryProfiles.require(view.getImplementation().getRegisterCount() == 16, "共有画面のレジスタ構造が異なります");
        MutableMethod list = methods.get(BerryProfiles.SHARE_LIST);
        int cast = BerryProfiles.unique(list, profile.type("Lb03;"));
        int query = BerryProfiles.unique(list, profile.ref("Ltp0;->c(Landroid/content/Intent;Lyx0;)Ljava/lang/Object;"));
        int empty = BerryProfiles.unique(list, "Ljava/util/List;->isEmpty()Z");
        int field = BerryProfiles.unique(list, profile.ref("Lb03;->M0:Lz32;"));
        int update = BerryProfiles.unique(list, profile.ref("Lfy;->O(Ljava/util/List;)V"));
        BerryProfiles.require(cast < query && query < empty && empty < field && field < update,
            "共有一覧の処理順序が異なります");
        assertRegister(list, cast, Opcode.CHECK_CAST, profile.shareDialogRegister);
        assertInvoke(list, query, Opcode.INVOKE_VIRTUAL, 1, 2, 0);
        assertInvoke(list, empty, Opcode.INVOKE_INTERFACE, 0);
        assertInvoke(list, update, Opcode.INVOKE_VIRTUAL, 1, 0);
        assertRegister(list, update + 1, Opcode.RETURN_OBJECT, profile.shareReturnRegister);
        BerryProfiles.require(list.getImplementation().getRegisterCount() == profile.shareRegisters,
            "共有一覧のレジスタ構造が異なります");
        return new int[] {adapter, empty, update + 1};
    }

    private static void assertRegister(MutableMethod method, int position, Opcode opcode, int register) {
        Instruction instruction = method.getImplementation().getInstructions().get(position);
        BerryProfiles.require(instruction.getOpcode() == opcode && instruction instanceof OneRegisterInstruction
            && ((OneRegisterInstruction) instruction).getRegisterA() == register,
            "共有一覧の画面参照または復帰先が異なります");
    }

    private static void assertInvoke(MutableMethod method, int position, Opcode opcode, int... registers) {
        Instruction instruction = method.getImplementation().getInstructions().get(position);
        BerryProfiles.require(instruction.getOpcode() == opcode && instruction instanceof FiveRegisterInstruction,
            "共有一覧の呼び出し形式が異なります");
        FiveRegisterInstruction invoke = (FiveRegisterInstruction) instruction;
        BerryProfiles.require(invoke.getRegisterCount() == registers.length, "共有一覧の呼び出し引数数が異なります");
        int[] actual = {invoke.getRegisterC(), invoke.getRegisterD(), invoke.getRegisterE(), invoke.getRegisterF(), invoke.getRegisterG()};
        for (int i = 0; i < registers.length; i++)
            BerryProfiles.require(actual[i] == registers[i], "共有一覧の呼び出しレジスタが異なります");
    }
}
