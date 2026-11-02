package app.hiro.berry.patches;

import app.hiro.berry.patches.BerryProfiles.Profile;
import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.patch.PatchKt;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import app.morphe.patcher.util.smali.ExternalLabel;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction;
import java.util.Collections;
import java.util.Map;
import kotlin.Unit;

public final class ForegroundTabsPatch {
    public static final String NAME = "プレビュー作成を後回しにして表示中タブを優先";
    public static final String EXTENSION = "extensions/berry_foreground_tabs.mpe";
    private static final InstructionExtensions I = InstructionExtensions.INSTANCE;

    private static final BytecodePatch CHECK = PatchKt.bytecodePatch(null, null, false, builder -> {
        builder.compatibleWith(BerryProfiles.TARGET);
        builder.execute(context -> {
            BerryProfiles.require(context.classDefByOrNull(c -> c.getType().startsWith("Lhiro/berry/")) == null,
                "表示中タブ優先は適用済みです。未改造の APK を選んでください");
            checked(context, BerryProfiles.select(context));
            return Unit.INSTANCE;
        });
        return Unit.INSTANCE;
    });

    public static final BytecodePatch PATCH = PatchKt.bytecodePatch(NAME,
        "読み込み直後のタブプレビュー作成を750ms以降・操作停止後へ延期します。表示中タブの読み込み・操作中は新しい裏タブの初回HTTP(S)読込を短時間保留し、順番に開始します。選択したタブは即時開始。停止・破棄・復元を考慮し、既存の動画・音声・タイマー処理は変更しません。速度差はサイト・端末に依存します。",
        true, builder -> {
            builder.compatibleWith(BerryProfiles.TARGET);
            builder.dependsOn(CHECK);
            builder.extendWith(() -> BerryProfiles.extension(ForegroundTabsPatch.class, EXTENSION));
            builder.execute(context -> { apply(context); return Unit.INSTANCE; });
            return Unit.INSTANCE;
        });

    private ForegroundTabsPatch() {}

    private static Map<String, MutableMethod> checked(BytecodePatchContext context, Profile profile) {
        Map<String, MutableMethod> methods = profile.checked(context, OriginalContracts.METHODS);
        profile.field(context, "Lg60;->l:B");
        profile.field(context, "Lg60;->m:Ltt5;");
        profile.field(context, "Ltt5;->q:Landroid/widget/FrameLayout;");
        profile.field(context, "Lc41;->q:Lcx;");
        BerryProfiles.require(methods.get("Lg60;->run()V").getImplementation().getRegisterCount() >= 3,
            "プレビュー処理に必要な一時レジスタがありません");
        BerryProfiles.require(methods.get("Lc41;->h(Landroid/os/Bundle;)Z").getImplementation().getRegisterCount() >= 3,
            "復元処理に必要な一時レジスタがありません");
        MutableMethod dispatch = dispatch(context, profile);
        if (dispatch != null) BerryProfiles.require(dispatch.getImplementation() != null
            && (dispatch.getAccessFlags() & 8) == 0, "タッチ転送処理の形式が異なります");
        return methods;
    }

    private static void apply(BytecodePatchContext context) {
        Profile profile = BerryProfiles.select(context);
        Map<String, MutableMethod> methods = checked(context, profile);
        MutableMethod method = methods.get("Lg60;->run()V");
        I.addInstructionsWithLabels(method, 0,
            "move-object/from16 v0, p0\niget-byte v0, v0, " + profile.ref("Lg60;->l:B") + "\n" +
            "if-ltz v0, :original\nconst/4 v1, 0x1\nif-gt v0, v1, :original\n" +
            "move-object/from16 v0, p0\niget-object v0, v0, " + profile.ref("Lg60;->m:Ltt5;") + "\n" +
            "iget-object v0, v0, " + profile.ref("Ltt5;->q:Landroid/widget/FrameLayout;") + "\n" +
            "move-object/from16 v1, p0\ninvoke-static {v0, v1}, Lhiro/berry/PreviewScheduler;->defer(Landroid/view/View;Ljava/lang/Runnable;)Z\n" +
            "move-result v0\nif-eqz v0, :original\nreturn-void\n",
            new ExternalLabel("original", method.getImplementation().getInstructions().get(0)));
        method = methods.get("Lc41;->S(Ljava/lang/String;)V");
        int load = BerryProfiles.unique(method, "Landroid/webkit/WebView;->loadUrl(Ljava/lang/String;)V");
        I.replaceInstruction(method, load, call(method, load,
            "Lhiro/berry/TabPriority;->load(Landroid/webkit/WebView;Ljava/lang/String;)V", 2));
        hook(methods.get("Lc41;->V()V"), "Landroid/webkit/WebView;->onResume()V", "resumed", true);
        hook(methods.get("Lc41;->t0()V"), "Landroid/webkit/WebView;->onPause()V", "paused", true);
        hook(methods.get("Lc41;->m()V"), "Landroid/webkit/WebView;->stopLoading()V", "stopped", false);
        hook(methods.get("Lc41;->destroy()V"), "Landroid/webkit/WebView;->destroy()V", "destroyed", false);
        method = methods.get("Lc41;->r(Landroid/os/Bundle;)V");
        int save = BerryProfiles.unique(method, "Landroid/webkit/WebView;->saveState(Landroid/os/Bundle;)Landroid/webkit/WebBackForwardList;");
        I.addInstructions(method, save, call(method, save,
            "Lhiro/berry/TabPriority;->savePending(Landroid/webkit/WebView;Landroid/os/Bundle;)V", 2));
        method = methods.get("Lc41;->h(Landroid/os/Bundle;)Z");
        String restore = method.getImplementation().getRegisterCount() <= 16
            ? "iget-object v0, p0, " + profile.ref("Lc41;->q:Lcx;") + "\n" +
              "invoke-static {v0, p1}, Lhiro/berry/TabPriority;->restorePending(Landroid/webkit/WebView;Landroid/os/Bundle;)Z\n"
            : "move-object/from16 v0, p0\niget-object v0, v0, " + profile.ref("Lc41;->q:Lcx;") + "\n" +
              "move-object/from16 v1, p1\ninvoke-static/range {v0 .. v1}, Lhiro/berry/TabPriority;->restorePending(Landroid/webkit/WebView;Landroid/os/Bundle;)Z\n";
        I.addInstructionsWithLabels(method, 0, restore +
            "move-result v0\nif-eqz v0, :original\nconst/4 v0, 0x1\nreturn v0\n",
            new ExternalLabel("original", method.getImplementation().getInstructions().get(0)));
        I.addInstructions(methods.get("Lc70;->onPageStarted(Landroid/webkit/WebView;Ljava/lang/String;Landroid/graphics/Bitmap;)V"), 0,
            "invoke-static/range {p1 .. p1}, Lhiro/berry/TabPriority;->started(Landroid/webkit/WebView;)V");
        I.addInstructions(methods.get("Lc70;->onPageFinished(Landroid/webkit/WebView;Ljava/lang/String;)V"), 0,
            "invoke-static/range {p1 .. p2}, Lhiro/berry/TabPriority;->finished(Landroid/webkit/WebView;Ljava/lang/String;)V");
        I.addInstructions(methods.get("Lcx;->onScrollChanged(IIII)V"), 0,
            "invoke-static {}, Lhiro/berry/PreviewScheduler;->scroll()V\n" +
            "invoke-static/range {p0 .. p0}, Lhiro/berry/TabPriority;->scroll(Landroid/webkit/WebView;)V");
        MutableMethod dispatch = dispatch(context, profile);
        if (dispatch != null) {
            I.addInstructions(dispatch, 0,
                "invoke-static/range {p1 .. p1}, Lhiro/berry/PreviewScheduler;->touch(Landroid/view/MotionEvent;)V\n" +
                "invoke-static/range {p0 .. p1}, Lhiro/berry/TabPriority;->touch(Landroid/webkit/WebView;Landroid/view/MotionEvent;)V");
            return;
        }
        dispatch = new MutableMethod(new ImmutableMethod(profile.type("Lcx;"), "dispatchTouchEvent",
            Collections.singletonList(new ImmutableMethodParameter("Landroid/view/MotionEvent;", Collections.emptySet(), null)),
            "Z", 1, Collections.emptySet(), Collections.emptySet(),
            new ImmutableMethodImplementation(3, Collections.emptyList(), Collections.emptyList(), Collections.emptyList())));
        I.addInstructions(dispatch, 0,
            "invoke-static {p1}, Lhiro/berry/PreviewScheduler;->touch(Landroid/view/MotionEvent;)V\n" +
            "invoke-static {p0, p1}, Lhiro/berry/TabPriority;->touch(Landroid/webkit/WebView;Landroid/view/MotionEvent;)V\n" +
            "invoke-super {p0, p1}, Landroid/webkit/WebView;->dispatchTouchEvent(Landroid/view/MotionEvent;)Z\nmove-result v0\nreturn v0");
        context.mutableClassDefBy(profile.type("Lcx;")).getMethods().add(dispatch);
    }

    private static void hook(MutableMethod method, String anchor, String helper, boolean after) {
        int position = BerryProfiles.unique(method, anchor);
        String code = call(method, position, "Lhiro/berry/TabPriority;->" + helper + "(Landroid/webkit/WebView;)V", 1);
        I.addInstructions(method, position + (after ? 1 : 0), code);
    }

    private static MutableMethod dispatch(BytecodePatchContext context, Profile profile) {
        MutableMethod found = null;
        for (MutableMethod method : context.mutableClassDefBy(profile.type("Lcx;")).getMethods()) {
            if (!"dispatchTouchEvent".equals(method.getName()) || !"Z".equals(method.getReturnType())
                || method.getParameterTypes().size() != 1
                || !"Landroid/view/MotionEvent;".contentEquals(method.getParameterTypes().get(0))) continue;
            BerryProfiles.require(found == null, "タッチ転送処理が重複しています");
            found = method;
        }
        return found;
    }

    private static String call(MutableMethod method, int position, String target, int count) {
        Instruction instruction = method.getImplementation().getInstructions().get(position);
        int[] registers;
        if (instruction instanceof RegisterRangeInstruction) {
            RegisterRangeInstruction range = (RegisterRangeInstruction) instruction;
            BerryProfiles.require(range.getRegisterCount() == count, "呼び出し引数数が異なります");
            return "invoke-static/range {v" + range.getStartRegister() + " .. v" +
                (range.getStartRegister() + count - 1) + "}, " + target;
        }
        BerryProfiles.require(instruction instanceof FiveRegisterInstruction, "呼び出し形式が異なります");
        FiveRegisterInstruction invoke = (FiveRegisterInstruction) instruction;
        BerryProfiles.require(invoke.getRegisterCount() == count, "呼び出し引数数が異なります");
        registers = new int[] {invoke.getRegisterC(), invoke.getRegisterD(), invoke.getRegisterE(),
            invoke.getRegisterF(), invoke.getRegisterG()};
        StringBuilder code = new StringBuilder("invoke-static {");
        for (int i = 0; i < count; i++) {
            if (i != 0) code.append(", ");
            code.append('v').append(registers[i]);
        }
        return code.append("}, ").append(target).toString();
    }
}
