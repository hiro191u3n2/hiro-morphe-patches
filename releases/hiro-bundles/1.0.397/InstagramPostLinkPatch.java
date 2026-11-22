package app.hiro.berry.links.patches;

import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import app.morphe.patcher.util.smali.ExternalLabel;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.BuilderInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import kotlin.Unit;

public final class InstagramPostLinkPatch {
    private static final String OWNER = "La90;";
    private static final String CALLBACK = OWNER + "->shouldOverrideUrlLoading(Landroid/webkit/WebView;Landroid/webkit/WebResourceRequest;)Z";
    private static final String EXTENSION = "extensions/berry_instagram_post_links.mpe";
    private static final Compatibility TARGET = new Compatibility("jp.ejimax.berrybrowser", "Berry Browser",
            "Berry Browser 3.84.21", null, "#6750A4", null,
            Collections.singletonList(new AppTarget("3.84.21", Collections.emptyMap(), false, null, null)), FormFactor.MOBILE);

    public static final BytecodePatch PATCH = PatchKt.bytecodePatch("Instagramの投稿リンクをアプリで開く",
            "AndroidでInstagramに許可したプロフィール・投稿・リールを、Googleの共有リンクからもアプリで開きます。", true, builder -> {
                builder.compatibleWith(TARGET);
                builder.extendWith(() -> {
                    InputStream stream = InstagramPostLinkPatch.class.getClassLoader().getResourceAsStream(EXTENSION);
                    if (stream == null) throw new IllegalStateException("Instagramリンクの実行部品がありません");
                    return stream;
                });
                builder.finalize(context -> {
                    MutableMethod load = findMethod(context.mutableClassDefBy("Ld71;").getVirtualMethods(),
                            "Ld71;->k(Ljava/lang/String;)V");
                    List<BuilderInstruction> ops = load.getImplementation().getInstructions();
                    if (load.getImplementation().getRegisterCount() != 2 || ops.size() != 3
                            || !ref(ops.get(0), "Ld71;->s:Lty;")
                            || !(ref(ops.get(1), "Landroid/webkit/WebView;->loadUrl(Ljava/lang/String;)V")
                              || ref(ops.get(1), "Lhiro/berry/TabPriority;->load(Landroid/webkit/WebView;Ljava/lang/String;)V"))) {
                        throw new IllegalStateException("BerryのURL読み込み処理と一致しません");
                    }
                    InstructionExtensions.INSTANCE.addInstructions(load, 1,
                            "invoke-static {v0, v1}, Lapp/hiro/berry/links/InstagramPostLinks;->browserLoad(Landroid/webkit/WebView;Ljava/lang/String;)V");
                    MutableMethod finish = findMethod(context.mutableClassDefBy(OWNER).getVirtualMethods(),
                            OWNER + "->onPageFinished(Landroid/webkit/WebView;Ljava/lang/String;)V");
                    InstructionExtensions.INSTANCE.addInstructions(finish, 0,
                            "invoke-static/range {p1 .. p2}, Lapp/hiro/berry/links/InstagramPostLinks;->pageFinished(Landroid/webkit/WebView;Ljava/lang/String;)V");
                    for (MutableMethod method : context.mutableClassDefBy(OWNER).getVirtualMethods()) {
                        if (method.toString().equals(CALLBACK)) {
                            apply(method);
                            for (MutableMethod dispatch : context.mutableClassDefBy("Lz43;").getVirtualMethods()) {
                                if (dispatch.toString().equals("Lz43;->a(Landroid/content/Intent;Ljava/lang/String;Z)Z")) {
                                    route(dispatch);
                                    return Unit.INSTANCE;
                                }
                            }
                            throw new IllegalStateException("Berryの外部アプリ起動処理が見つかりません");
                        }
                    }
                    throw new IllegalStateException("Berryのリンク処理が見つかりません");
                });
                return Unit.INSTANCE;
            });

    private static MutableMethod findMethod(Iterable<MutableMethod> methods, String name) {
        for (MutableMethod method : methods) if (name.equals(method.toString())) return method;
        throw new IllegalStateException("Berryのリンク処理が見つかりません: " + name);
    }

    private static void apply(MutableMethod method) {
        List<BuilderInstruction> ins = method.getImplementation().getInstructions();
        if (ins.size() != 240 || method.getImplementation().getRegisterCount() != 11
                || !ref(ins.get(0), "Landroid/webkit/WebResourceRequest;->getUrl()Landroid/net/Uri;")
                || !ref(ins.get(110), "Laz3;->a(Lh26;Ljava/lang/String;Lqj3;)Z")
                || ins.get(112).getOpcode() != Opcode.IF_NEZ
                || !ref(ins.get(113), "Ljp/ejimax/berrybrowser/browser_impl/BrowserActivity;->o0()Lcn6;"))
            throw new IllegalStateException("Berry 3.84.21のリンク処理と一致しません");
        InstructionExtensions edit = InstructionExtensions.INSTANCE;
        edit.addInstructionsWithLabels(method, 113,
                "invoke-static {v0, v7, v5}, Lapp/hiro/berry/links/InstagramPostLinks;->selectedIntent(Landroid/app/Activity;Landroid/webkit/WebResourceRequest;Z)Landroid/content/Intent;\n"
                + "move-result-object v7\nif-eqz v7, :web\n"
                + "iget-object v1, v1, Laz3;->d:Lz43;\nconst-string v8, \"instagram_post_link\"\n"
                + "invoke-virtual {v1, v7, v8, v10}, Lz43;->a(Landroid/content/Intent;Ljava/lang/String;Z)Z\n"
                + "move-result v8\nif-eqz v8, :web\nreturn v6",
                new ExternalLabel("web", ins.get(113)));
        // v5 and v7 are unused up to the hook. Berry reuses both parameter registers.
        edit.addInstructions(method, 0, "move-object v7, p2\n"
                + "invoke-static {p1, p2}, Lapp/hiro/berry/links/InstagramPostLinks;->allowsHandoff(Landroid/webkit/WebView;Landroid/webkit/WebResourceRequest;)Z\n"
                + "move-result v5");
    }

    private static void route(MutableMethod dispatch) {
        List<BuilderInstruction> instructions = dispatch.getImplementation().getInstructions();
        if (dispatch.getImplementation().getRegisterCount() != 14 || instructions.size() < 39
                || !ref(instructions.get(0), "Lz43;->a:Lhh;")
                || !ref(instructions.get(38), "Landroid/content/Context;->startActivity(Landroid/content/Intent;)V")) {
            throw new IllegalStateException("Berry 3.84.21の外部アプリ起動処理と一致しません");
        }
        InstructionExtensions.INSTANCE.addInstructions(dispatch, 38,
                "invoke-static {v1, v11}, Lapp/hiro/berry/links/InstagramPostLinks;->routeSelectedLink(Landroid/app/Activity;Landroid/content/Intent;)V");
    }

    private static boolean ref(BuilderInstruction instruction, String expected) {
        return instruction instanceof ReferenceInstruction
                && ((ReferenceInstruction)instruction).getReference().toString().equals(expected);
    }
}
