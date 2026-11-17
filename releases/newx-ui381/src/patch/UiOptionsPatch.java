package app.hiro.newxui.patches;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import kotlin.Unit;
import app.morphe.patcher.PatcherContext;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Exact NewX hooks with Piko's existing in-app settings and persisted preferences. */
public final class UiOptionsPatch {
    public static final String NAME = "NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）";
    public static final String VERSION = "12.43.0-prod.01";
    public static final String MAIN = "Lcom/x/android/main/MainActivity;";
    public static final String BUILTIN = "Lapp/morphe/extension/newx/settings/BuiltInSettings;";
    public static final String OPTIONS = "Lapp/hiro/newxui/UiOptions;";
    public static final String REFRESH = "Lapp/hiro/newxui/RefreshRate;";
    private static final String[] REQUIRED = {"SettingsPatch.newXSettingsPatch", "SettingsResourcePatch.newXSettingsResourcePatch", "ExtensionPatch.newXExtensionPatch", "AddResourcesPatch.addResourcesPatch"};
    private static final Patch RUNTIME = PatchKt.bytecodePatch(null, null, false, b -> {
        b.extendWith(() -> {
            InputStream input = UiOptionsPatch.class.getResourceAsStream("/extensions/newx_ui_options.mpe");
            require(input != null, "追加処理がありません"); return input;
        });
        return Unit.INSTANCE;
    });
    public static final Patch PATCH = PatchKt.patch(NAME,
        "X 12.43.0-prod.01用。チャットのおすすめ非表示・上下バー固定・120Hz優先・タイムラインの表示回数を省略せず表示する機能をPiko設定内で個別に切替（初期オン）。Piko NewXと一緒に選択。120HzはOS制限内で要求します。",
        false, b -> {
            b.compatibleWith(new Compatibility("com.twitter.android", "NewX", "Piko NewXと同時適用", ApkFileType.APKM, "#000000", null,
                Collections.singletonList(new AppTarget(VERSION, Collections.emptyMap(), false, null, null)), FormFactor.MOBILE));
            b.dependsOn(RUNTIME);
            b.execute(() -> {
                require("com.twitter.android".equals(PatchFunctionsKt.getPackageMetadata().getPackageName()) && VERSION.equals(PatchFunctionsKt.getPackageMetadata().getVersionName()), "未改造のX 12.43.0-prod.01 APKMを使用してください");
                validateSelection(); return Unit.INSTANCE;
            });
            b.finalize(() -> {
                validateSelection();
                BytecodePatchContext c = PatchFunctionsKt.getCurrentBytecode();
                ChatRecommendationsHook.verify(c.classDefBy(ChatRecommendationsHook.TYPE));
                PinBarsPatch.verify(c.classDefBy(PinBarsPatch.TOP), c.classDefBy(PinBarsPatch.BOTTOM));
                ViewCountHook.verify(c.classDefBy(ViewCountHook.TYPE));
                verifySettings(c.classDefBy(BUILTIN));
                verifyActivity(c.classDefBy(MAIN));
                addStrings("res/values/strings.xml", "/newx-ui/values.xml");
                addStrings("res/values-ja/strings.xml", "/newx-ui/values-ja.xml");
                ChatRecommendationsHook.apply(c.mutableClassDefBy(ChatRecommendationsHook.TYPE));
                PinBarsPatch.apply(c.mutableClassDefBy(PinBarsPatch.TOP), c.mutableClassDefBy(PinBarsPatch.BOTTOM));
                ViewCountHook.apply(c.mutableClassDefBy(ViewCountHook.TYPE));
                applySettings(c.mutableClassDefBy(BUILTIN));
                applyActivity(c.mutableClassDefBy(MAIN));
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });
    private UiOptionsPatch() {}
    static void require(boolean ok, String why) { if (!ok) throw new IllegalStateException("NewX表示設定: " + why); }
    private static void visit(Patch p, Set<Patch> seen, List<Patch> out) {
        if (!seen.add(p)) return; out.add(p); for (Patch d : p.getDependencies()) visit(d, seen, out);
    }
    private static void validateSelection() {
        List<Patch> all = new ArrayList<Patch>();
        Set<Patch> seen = Collections.newSetFromMap(new IdentityHashMap<Patch, Boolean>());
        for (Patch p : PatcherContext.Companion.getCurrent$morphe_patcher().getExecutablePatches$morphe_patcher()) visit(p, seen, all);
        PatchBundleInfo selected = null;
        for (String name : REQUIRED) {
            List<Patch> matches = new ArrayList<Patch>();
            for (Patch p : all) if (name.equals(p.getDeclaredAs$morphe_patcher())) matches.add(p);
            require(matches.size() == 1, "Piko 3.12.0-dev.1/dev.2のNewX項目と一緒に選択してください（" + name + "）");
            PatchBundleInfo bundle = matches.get(0).getBundle$morphe_patcher();
            require(bundle != null && "Piko".equals(bundle.getName()) && ("3.12.0-dev.1".equals(bundle.getVersion()) || "3.12.0-dev.2".equals(bundle.getVersion())), "未検証のPikoソースです");
            if (selected == null) selected = bundle; else require(selected.equals(bundle), "複数のPikoソースが選択されています");
        }
    }
    public static Method register(ClassDef c) {
        require(c != null && BUILTIN.equals(c.getType()), "Piko設定登録クラスがありません");
        Method found = null;
        for (Method m : c.getMethods()) if (m.getName().equals("register") && m.getParameterTypes().isEmpty() && m.getReturnType().equals("V")) {
            require(found == null, "設定登録メソッドが重複しています"); found = m;
        }
        require(found != null && found.getImplementation() != null && (found.getAccessFlags() & 8) != 0, "設定登録メソッドがありません"); return found;
    }
    public static void verifySettings(ClassDef c) {
        Method m = register(c); boolean category = false;
        for (Instruction i : m.getImplementation().getInstructions()) if (i instanceof ReferenceInstruction) {
            String r = ((ReferenceInstruction)i).getReference().toString();
            require(!r.startsWith(OPTIONS), "表示設定がすでに適用されています");
            if (r.equals("newx.advanced")) category = true;
        }
        require(category, "Piko設定登録の構造が異なります");
    }
    public static void applySettings(MutableClass c) {
        ((MutableMethod)register(c)).getImplementation().addInstruction(0, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0,
            new ImmutableMethodReference(OPTIONS, "register", Collections.<String>emptyList(), "V")));
    }
    public static void verifyActivity(ClassDef c) {
        require(c != null && MAIN.equals(c.getType()) && "Landroidx/appcompat/app/p;".equals(c.getSuperclass()), "MainActivityの継承が異なります");
        for (Method m : c.getMethods()) require(!(m.getName().equals("onResume") && m.getParameterTypes().isEmpty()), "MainActivity.onResumeが変更済みです");
    }
    public static void applyActivity(MutableClass c) {
        List<Instruction> instructions = Arrays.<Instruction>asList(
            new BuilderInstruction35c(Opcode.INVOKE_SUPER, 1, 0, 0, 0, 0, 0, new ImmutableMethodReference(c.getSuperclass(), "onResume", Collections.<String>emptyList(), "V")),
            new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, 0, 0, 0, 0, 0, new ImmutableMethodReference(REFRESH, "bind", Collections.singletonList("Landroid/app/Activity;"), "V")),
            new BuilderInstruction10x(Opcode.RETURN_VOID));
        ImmutableMethod method = new ImmutableMethod(MAIN, "onResume", Collections.<ImmutableMethodParameter>emptyList(), "V", 4,
            Collections.emptySet(), Collections.emptySet(), new ImmutableMethodImplementation(1, instructions, Collections.emptyList(), Collections.emptyList()));
        c.getMethods().add(new MutableMethod(method));
    }
    private static void addStrings(String path, String asset) {
        File file = PatchFunctionsKt.resourceFile(path, true);
        try (InputStream in = UiOptionsPatch.class.getResourceAsStream(asset)) {
            require(in != null, "表示文字列がありません");
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int n;
            while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
            String additions = new String(out.toByteArray(), StandardCharsets.UTF_8);
            String original = file.exists() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) : "<resources>\n</resources>\n";
            require(!original.contains("name=\"hiro_newx_ui_") && !original.contains("<!DOCTYPE"), "文字列リソースが変更済みです");
            int end = original.lastIndexOf("</resources>"); require(end >= 0, "リソースXMLが不正です");
            String result = original.substring(0, end) + additions + original.substring(end);
            Files.createDirectories(file.toPath().getParent()); Files.write(file.toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) { throw new IllegalStateException("NewX表示設定リソースを保存できません", e); }
    }
}
