package app.hiro.berry.links.patches;

import app.hiro.berry.patches.MethodContract;
import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import app.morphe.patcher.util.smali.ExternalLabel;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.builder.BuilderInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import kotlin.Unit;

public final class InstagramPostLinkPatch {
    private static final String EXTENSION = "extensions/berry_instagram_post_links.mpe";
    private static final String RUNTIME = "Lapp/hiro/berry/links/InstagramPostLinks;->";
    private static final String DISPATCH = "a(Landroid/content/Intent;Ljava/lang/String;Z)Z";
    private static final String CALLBACK = "shouldOverrideUrlLoading(Landroid/webkit/WebView;Landroid/webkit/WebResourceRequest;)Z";
    private enum Layout {
        V12("Lk80;", "Lr61;", "Lfy;", "Lzx3;", "Lh43;",
                "3ed7f87b3a9bf5f539ebedb1cc0e7e688bc3437c2383298743a807f0c79fcd2f",
                "8d6bec4a1fa6bc94d962acf1f721774d16559c481c47b30d06c8d41cd9a17b14"),
        V21("La90;", "Ld71;", "Lty;", "Laz3;", "Lz43;",
                "859b1583cbfaf48a07591ca279aa291802bab599b54a25ce500b9df48eb4d22d",
                "c50ee642d728d0e11511e1936502983ecfa0dbe067b17f41ee78b68757ae07e6"),
        V22("Lb90;", "Lf71;", "Luy;", "Lrz3;", "Lj53;",
                "b674eed1f6bb039a8f02a27f340decfef9b039eb9e9c9ecdea746bcf8cb1870d",
                "1215406a115042cd678d16ea7ed45444303226e1f68717217b3bdcb8bb4c427f");

        final String client, controller, webView, ruleOwner, dispatcher, callbackHash, dispatchHash;

        Layout(String client, String controller, String webView, String ruleOwner,
               String dispatcher, String callbackHash, String dispatchHash) {
            this.client = client;
            this.controller = controller;
            this.webView = webView;
            this.ruleOwner = ruleOwner;
            this.dispatcher = dispatcher;
            this.callbackHash = callbackHash;
            this.dispatchHash = dispatchHash;
        }
    }
    private static final Compatibility TARGET = new Compatibility("jp.ejimax.berrybrowser", "Berry Browser",
            "Berry Browser", null, "#6750A4", null, Arrays.asList(
            new AppTarget("3.84.22", Collections.emptyMap(), false, null, null),
            new AppTarget("3.84.21", Collections.emptyMap(), false, null, null),
            new AppTarget("3.84.12", Collections.emptyMap(), false, null, null)), FormFactor.MOBILE);

    public static final BytecodePatch PATCH = PatchKt.bytecodePatch("Instagramの投稿リンクをアプリで開く",
            "AndroidでInstagramに許可したプロフィール・投稿・リールを、Googleの共有リンクや画像検索の共有リンクからもアプリで開きます。", true, builder -> {
                builder.compatibleWith(TARGET);
                builder.extendWith(() -> {
                    InputStream stream = InstagramPostLinkPatch.class.getClassLoader().getResourceAsStream(EXTENSION);
                    if (stream == null) throw new IllegalStateException("Instagramリンクの実行部品がありません");
                    return stream;
                });
                builder.finalize(context -> {
                    patchLinks(context);
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });

    private static void patchLinks(BytecodePatchContext context) {
        // Identical application code can carry distinct Android version names.
        // Choose the insertion layout from the actual WebView callback body.
        Layout layout = null;
        for (Layout candidate : Layout.values()) {
            if (!matchesCallback(context, candidate.client, candidate.callbackHash)) continue;
            if (layout != null) throw new IllegalStateException("Berryのリンク処理が複数あります");
            layout = candidate;
        }
        if (layout == null) throw new IllegalStateException("Berryのリンク処理を一意に特定できません: "
                + context.getPackageMetadata().getVersionName());

        boolean older = layout == Layout.V12;
        String client = layout.client;
        String dispatcher = layout.dispatcher;
        MutableMethod load = method(context, layout.controller, older ? "R(Ljava/lang/String;)V" : "k(Ljava/lang/String;)V");
        MutableMethod finish = method(context, client, "onPageFinished(Landroid/webkit/WebView;Ljava/lang/String;)V");
        MutableMethod callback = method(context, client, CALLBACK);
        MutableMethod dispatch = method(context, dispatcher, DISPATCH);

        // The callback reuses its parameter registers. Verify the complete register
        // allocation, staged policy decisions, branch destinations and dispatch path.
        requireBody(dispatch, layout.dispatchHash);
        verifyLoad(load, layout.controller + "->s:" + layout.webView);
        String ruleOwner = layout.ruleOwner;
        String dispatchField = ruleOwner + "->d:" + dispatcher;
        boolean found = false;
        for (com.android.tools.smali.dexlib2.iface.Field field : context.classDefBy(ruleOwner).getFields())
            if (dispatchField.equals(field.toString())) found = true;
        if (!found) throw new IllegalStateException("Berryの外部アプリ起動先が見つかりません: " + dispatchField);

        InstructionExtensions edit = InstructionExtensions.INSTANCE;
        edit.addInstructions(load, 1, "invoke-static {v0, v1}, " + RUNTIME + "browserLoad(Landroid/webkit/WebView;Ljava/lang/String;)V");
        edit.addInstructions(finish, 0, "invoke-static/range {p1 .. p2}, " + RUNTIME + "pageFinished(Landroid/webkit/WebView;Ljava/lang/String;)V");
        List<BuilderInstruction> ops = callback.getImplementation().getInstructions();
        String selected = RUNTIME + "selectedIntent(Landroid/app/Activity;Landroid/webkit/WebResourceRequest;Z)Landroid/content/Intent;";

        if (older) {
            // 3.84.12's IF_EQZ jumps directly to the web fallback. Inserting at
            // that target would miss that incoming edge. Intercept the result
            // before IF_EQZ and preserve v10 for the original decision.
            edit.addInstructionsWithLabels(callback, 119,
                    "if-nez v10, :native\n"
                    + "invoke-static {v8, v7, v5}, " + selected + "\n"
                    + "move-result-object v7\nif-eqz v7, :native\n"
                    + "iget-object v1, v1, " + dispatchField + "\nconst-string v5, \"instagram_post_link\"\n"
                    + "invoke-virtual {v1, v7, v5, v0}, " + dispatcher + "->" + DISPATCH + "\n"
                    + "move-result v5\nif-eqz v5, :native\nreturn v6",
                    new ExternalLabel("native", ops.get(119)));
        } else {
            // 3.84.21 and 3.84.22 fall through to the hook only after Berry declines the URL.
            edit.addInstructionsWithLabels(callback, 113,
                    "invoke-static {v0, v7, v5}, " + selected + "\n"
                    + "move-result-object v7\nif-eqz v7, :web\n"
                    + "iget-object v1, v1, " + dispatchField + "\nconst-string v8, \"instagram_post_link\"\n"
                    + "invoke-virtual {v1, v7, v8, v10}, " + dispatcher + "->" + DISPATCH + "\n"
                    + "move-result v8\nif-eqz v8, :web\nreturn v6",
                    new ExternalLabel("web", ops.get(113)));
        }
        edit.addInstructions(callback, 0, "move-object v7, p2\n"
                + "invoke-static {p1, p2}, " + RUNTIME + "allowsHandoff(Landroid/webkit/WebView;Landroid/webkit/WebResourceRequest;)Z\n"
                + "move-result v5");
        edit.addInstructions(dispatch, 38,
                "invoke-static {v1, v11}, " + RUNTIME + "routeSelectedLink(Landroid/app/Activity;Landroid/content/Intent;)V");
    }

    private static boolean matchesCallback(BytecodePatchContext context, String owner, String expected) {
        ClassDef client = context.classDefByOrNull(owner);
        if (client == null || !"Landroid/webkit/WebViewClient;".equals(client.getSuperclass())) return false;
        for (Method candidate : client.getVirtualMethods()) {
            if ((owner + "->" + CALLBACK).equals(candidate.toString()) && candidate.getImplementation() != null) {
                return expected.equals(MethodContract.sha256(method(context, owner, CALLBACK)));
            }
        }
        return false;
    }

    private static MutableMethod method(BytecodePatchContext context, String owner, String signature) {
        String descriptor = owner + "->" + signature;
        for (MutableMethod method : context.mutableClassDefBy(owner).getVirtualMethods())
            if (descriptor.equals(method.toString()) && method.getImplementation() != null) return method;
        throw new IllegalStateException("Berryのリンク処理が見つかりません: " + descriptor);
    }

    private static void requireBody(MutableMethod method, String expected) {
        if (!expected.equals(MethodContract.sha256(method)))
            throw new IllegalStateException("Berryのリンク処理の命令構成が一致しません: " + method);
    }

    private static void verifyLoad(MutableMethod load, String webField) {
        List<BuilderInstruction> ops = load.getImplementation().getInstructions();
        if (load.getImplementation().getRegisterCount() == 2 && ops.size() == 3
                && ops.get(0).getOpcode() == Opcode.IGET_OBJECT && ref(ops.get(0), webField)
                && ((TwoRegisterInstruction)ops.get(0)).getRegisterA() == 0 && ((TwoRegisterInstruction)ops.get(0)).getRegisterB() == 0
                && ops.get(1) instanceof FiveRegisterInstruction && ops.get(2).getOpcode() == Opcode.RETURN_VOID) {
            FiveRegisterInstruction invoke = (FiveRegisterInstruction)ops.get(1);
            if (invoke.getRegisterCount() == 2 && invoke.getRegisterC() == 0 && invoke.getRegisterD() == 1
                    && ((invoke.getOpcode() == Opcode.INVOKE_VIRTUAL && ref(ops.get(1), "Landroid/webkit/WebView;->loadUrl(Ljava/lang/String;)V"))
                    || (invoke.getOpcode() == Opcode.INVOKE_STATIC && ref(ops.get(1), "Lhiro/berry/TabPriority;->load(Landroid/webkit/WebView;Ljava/lang/String;)V")))) return;
        }
        throw new IllegalStateException("BerryのURL読み込み処理と一致しません: " + load);
    }

    private static boolean ref(BuilderInstruction instruction, String expected) {
        return instruction instanceof ReferenceInstruction
                && ((ReferenceInstruction)instruction).getReference().toString().equals(expected);
    }
}
