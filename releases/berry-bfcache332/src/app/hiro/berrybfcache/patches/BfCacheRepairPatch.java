package app.hiro.berrybfcache.patches;

import app.hiro.berry.patches.ForegroundTabsPatch;
import app.hiro.berry.patches.BerryProfiles;
import app.hiro.berryshare.patches.BrowserVisibilityPatch;
import app.hiro.berryzoom.patches.OneHandZoomPatch;
import app.hiro.berryqueue.patches.BerryMessageQueuePatch;
import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.util.smali.ExternalLabel;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import java.util.*;
import kotlin.Unit;

/** Completes positively verified persisted restores without requiring a missing native commit. */
public final class BfCacheRepairPatch {
    public static final String NAME = "バックフォワードキャッシュの復帰処理を補正";
    public static final String EXTENSION = "extensions/berry_bfcache_restore.mpe";
    private static final String WEB = "Landroid/webkit/WebView;";
    private static final String STR = "Ljava/lang/String;";
    private static final String HELPER = "Lhiro/berrybfcache/RestoreCoordinator;";
    private static final String TARGET = "Lhiro/berrybfcache/CompletionTarget;";
    private static final String COMPLETION = "Lhiro/berrybfcache/NativeCompletion;";
    private static final String BOUNDARY = "Lorg/chromium/support_lib_boundary/WebViewProviderBoundaryInterface;";
    private static final String INSTALLED = "hiroBfcacheInstalled";
    private static final String INSTALL = "hiroInstallBfcache";
    private static final String COMPLETE = "hiroCompleteBfcache";
    private static final String START = "hiroStartedBfcache";
    private static final InstructionExtensions I = InstructionExtensions.INSTANCE;
    private static final Compatibility COMPAT = new Compatibility(
        "jp.ejimax.berrybrowser", "Berry Browser", "BFCacheを有効にしたまま復帰時の完了処理と再描画を補正します。",
        null, Integer.valueOf(6770852), null,
        Collections.singletonList(new AppTarget(null, false, null)));

    public static final BytecodePatch PATCH = PatchKt.bytecodePatch(NAME,
        "未改造のBerry Browserに元の4パッチと一緒に適用してください。BFCache復帰時の通知欠落・順序逆転・応答待ち残留を補正し、読み込み完了状態を復元します。BFCache・オートページャーの設定は維持します。実機での症状改善は確認が必要です。",
        true, builder -> {
            builder.compatibleWith(COMPAT);
            builder.dependsOn(ForegroundTabsPatch.PATCH, BrowserVisibilityPatch.PATCH,
                             OneHandZoomPatch.PATCH, BerryMessageQueuePatch.PATCH);
            builder.extendWith(() -> BfCacheRepairPatch.class.getClassLoader().getResourceAsStream(EXTENSION));
            builder.execute(context -> { applyRepair(context); return Unit.INSTANCE; });
            return Unit.INSTANCE;
        });

    private BfCacheRepairPatch() {}

    /** QA wrappers may invoke the same guarded implementation without reapplying the four prerequisites. */
    public static void applyRepair(BytecodePatchContext context) {
        if (!"jp.ejimax.berrybrowser".equals(context.getPackageMetadata().getPackageName())) fail("Berry Browser専用です");
        Profile p = profile(context);
        MutableClass client = context.mutableClassDefBy(p.client);
        MutableClass adapter = context.mutableClassDefBy(p.adapter);
        if (!"Landroid/webkit/WebViewClient;".equals(client.getSuperclass())) fail("WebViewClientの継承元が異なります");
        absentMethod(client, "onPageCommitVisible"); absentMethod(client, COMPLETE); absentMethod(client, START);
        absentMethod(adapter, INSTALL); absentField(adapter, INSTALLED);
        if (client.getInterfaces().contains(TARGET)) fail("復帰補正は適用済みです");
        field(context, p.client, p.clientTabField, p.tab);
        field(context, p.tab, p.adapterField, p.adapterInterface);
        field(context, p.adapter, p.webField, p.webSubclass);
        field(context, p.provider, p.providerBoundaryField, BOUNDARY);
        if (context.classDefByOrNull(HELPER) == null || context.classDefByOrNull(COMPLETION) == null || context.classDefByOrNull(TARGET) == null)
            fail("BFCache補正の拡張が読み込まれていません");
        method(context, HELPER + "->listenerName()" + STR);
        method(context, HELPER + "->script()" + STR);
        method(context, HELPER + "->listener()Ljava/lang/reflect/InvocationHandler;");
        method(context, HELPER + "->started(" + WEB + STR + "Ljava/lang/Runnable;)V");
        method(context, HELPER + "->finished(" + WEB + STR + ")V");
        method(context, HELPER + "->committed(" + WEB + STR + "Ljava/lang/Runnable;)V");
        method(context, COMPLETION + "-><init>(" + TARGET + WEB + STR + ")V");
        method(context, TARGET + "->" + COMPLETE + "(" + WEB + STR + ")V");
        method(context, "Lhiro/berry/TabPriority;->finished(" + WEB + STR + ")V");
        method(context, p.feature + "->" + p.featureMethod + "(" + STR + ")Z");
        method(context, p.factory + "->" + p.factoryMethod + "(" + WEB + ")" + p.provider);
        method(context, BOUNDARY + "->addWebMessageListener(" + STR + "[Ljava/lang/String;Ljava/lang/reflect/InvocationHandler;)V");
        method(context, BOUNDARY + "->addDocumentStartJavaScript(" + STR + "[Ljava/lang/String;)Ljava/lang/reflect/InvocationHandler;");

        MutableMethod setup = exact(context, p.setupOwner + "->" + p.setupName + "(" + p.tab + ")V", p.hashes[0]);
        MutableMethod started = exact(context, p.client + "->onPageStarted(" + WEB + STR + "Landroid/graphics/Bitmap;)V", p.hashes[1]);
        MutableMethod finished = method(context, p.client + "->onPageFinished(" + WEB + STR + ")V");
        exact(context, p.tab + "->" + p.completeSetter + "(I)V", p.hashes[2]);
        exact(context, p.adapter + "->" + p.listenerMethod + "(" + STR + "Ljava/util/Set;" + p.listenerCallbackType + ")Z", p.hashes[3]);
        exact(context, p.adapter + "->" + p.documentMethod + "(" + STR + STR + "Ljava/util/Set;)" + p.scriptHandler, p.hashes[4]);

        NativeCompletionStructure nativeCompletion = NativeCompletionStructure.discover(context, p, finished);

        // Mutation starts only after all profile and prerequisite checks above have passed.
        adapter.getFields().add(new MutableField(new ImmutableField(p.adapter, INSTALLED, "Z", 2,
            null, Collections.emptySet(), Collections.emptySet())));
        addMethod(adapter, INSTALL, Collections.emptyList(), 1, 6, installCode(p));
        I.addInstructionsWithLabels(setup, 0,
            "move-object/from16 v0, p1\n" +
            "iget-object v0, v0, " + p.tab + "->" + p.adapterField + ":" + p.adapterInterface + "\n" +
            "instance-of v1, v0, " + p.adapter + "\n" +
            "if-eqz v1, :original\n" +
            "check-cast v0, " + p.adapter + "\n" +
            "invoke-virtual {v0}, " + p.adapter + "->" + INSTALL + "()V",
            new ExternalLabel("original", setup.getImplementation().getInstructions().get(0)));
        // A per-navigation completion target must exist even when WebView omits commit-visible.
        // Use a separate method to avoid consuming live locals or exceeding invoke register widths.
        addMethod(client, START, Arrays.asList(WEB, STR), 17, 4,
            "new-instance v0, " + COMPLETION + "\n" +
            "invoke-direct {v0, p0, p1, p2}, " + COMPLETION + "-><init>(" + TARGET + WEB + STR + ")V\n" +
            "invoke-static {p1, p2, v0}, " + HELPER + "->started(" + WEB + STR + "Ljava/lang/Runnable;)V\n" +
            "return-void");
        I.addInstructions(started, 0, "invoke-virtual/range {p0 .. p2}, " + p.client + "->" + START + "(" + WEB + STR + ")V");
        I.addInstructions(finished, 0, "invoke-static/range {p1 .. p2}, " + HELPER + "->finished(" + WEB + STR + ")V");
        client.getInterfaces().add(TARGET);
        addMethod(client, COMPLETE, Arrays.asList(WEB, STR), 17, 7, nativeCompletion.code(p));
        addMethod(client, "onPageCommitVisible", Arrays.asList(WEB, STR), 17, 4,
            "invoke-super {p0, p1, p2}, Landroid/webkit/WebViewClient;->onPageCommitVisible(" + WEB + STR + ")V\n" +
            "new-instance v0, " + COMPLETION + "\n" +
            "invoke-direct {v0, p0, p1, p2}, " + COMPLETION + "-><init>(" + TARGET + WEB + STR + ")V\n" +
            "invoke-static {p1, p2, v0}, " + HELPER + "->committed(" + WEB + STR + "Ljava/lang/Runnable;)V\n" +
            "return-void");
        System.out.println("Berry BFCache補正：復帰通知の欠落・応答待ち残留への回復と完了状態の補正を追加しました。設定値は変更していません。");
    }

    private static String installCode(Profile p) {
        return "iget-boolean v0, p0, " + p.adapter + "->" + INSTALLED + ":Z\n" +
            "if-nez v0, :done\n" +
            "const-string v0, \"WEB_MESSAGE_LISTENER\"\n" +
            "invoke-static {v0}, " + p.feature + "->" + p.featureMethod + "(" + STR + ")Z\n" +
            "move-result v0\nif-eqz v0, :done\n" +
            "const-string v0, \"DOCUMENT_START_SCRIPT\"\n" +
            "invoke-static {v0}, " + p.feature + "->" + p.featureMethod + "(" + STR + ")Z\n" +
            "move-result v0\nif-eqz v0, :done\n" +
            "iget-object v0, p0, " + p.adapter + "->" + p.webField + ":" + p.webSubclass + "\n" +
            "invoke-static {v0}, " + p.factory + "->" + p.factoryMethod + "(" + WEB + ")" + p.provider + "\n" +
            "move-result-object v0\n" +
            "iget-object v0, v0, " + p.provider + "->" + p.providerBoundaryField + ":" + BOUNDARY + "\n" +
            "const/4 v1, 0x1\nnew-array v2, v1, [Ljava/lang/String;\n" +
            "const/4 v1, 0x0\nconst-string v3, \"*\"\naput-object v3, v2, v1\n" +
            "invoke-static {}, " + HELPER + "->listenerName()" + STR + "\nmove-result-object v1\n" +
            "invoke-static {}, " + HELPER + "->listener()Ljava/lang/reflect/InvocationHandler;\nmove-result-object v3\n" +
            "invoke-interface {v0, v1, v2, v3}, " + BOUNDARY + "->addWebMessageListener(" + STR + "[Ljava/lang/String;Ljava/lang/reflect/InvocationHandler;)V\n" +
            "invoke-static {}, " + HELPER + "->script()" + STR + "\nmove-result-object v1\n" +
            "invoke-interface {v0, v1, v2}, " + BOUNDARY + "->addDocumentStartJavaScript(" + STR + "[Ljava/lang/String;)Ljava/lang/reflect/InvocationHandler;\n" +
            "const/4 v1, 0x1\niput-boolean v1, p0, " + p.adapter + "->" + INSTALLED + ":Z\n" +
            ":done\nreturn-void";
    }

    private static void addMethod(MutableClass owner, String name, List<String> parameters, int access, int registers, String code) {
        List<ImmutableMethodParameter> args = new ArrayList<>();
        for (String type : parameters) args.add(new ImmutableMethodParameter(type, Collections.emptySet(), null));
        MutableMethod added = new MutableMethod(new ImmutableMethod(owner.getType(), name, args, "V", access,
            Collections.emptySet(), Collections.emptySet(), new ImmutableMethodImplementation(registers,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList())));
        I.addInstructions(added, 0, code);
        owner.getMethods().add(added);
    }
    private static MutableMethod method(BytecodePatchContext c, String descriptor) {
        String owner = descriptor.substring(0, descriptor.indexOf("->"));
        if (c.classDefByOrNull(owner) == null) fail("必要なクラスがありません：" + owner);
        MutableMethod found = null;
        for (MutableMethod m : c.mutableClassDefBy(owner).getMethods()) if (descriptor.equals(DexFormatter.INSTANCE.getMethodDescriptor(m))) {
            if (found != null) fail("処理が重複しています：" + descriptor); found = m;
        }
        if (found == null) fail("必要な処理がありません：" + descriptor);
        return found;
    }
    private static MutableMethod exact(BytecodePatchContext c, String descriptor, String hash) {
        MutableMethod m = method(c, descriptor);
        if (!hash.equals(ExactBody.sha256(m))) fail("検証済み構造と一致しません：" + descriptor);
        return m;
    }
    private static void field(BytecodePatchContext c, String owner, String name, String type) {
        for (Field f : c.mutableClassDefBy(owner).getFields()) if (name.equals(f.getName()) && type.equals(f.getType())) return;
        fail("必要なフィールドがありません：" + owner + "->" + name + ":" + type);
    }
    private static void absentMethod(MutableClass c, String name) {
        for (Method m : c.getMethods()) if (name.equals(m.getName())) fail("補正対象が適用済み、または未対応です：" + c.getType() + "->" + name);
    }
    private static void absentField(MutableClass c, String name) {
        for (Field f : c.getFields()) if (name.equals(f.getName())) fail("補正対象は適用済みです：" + name);
    }
    private static void fail(String message) { throw new IllegalStateException("BFCache復帰補正：" + message); }

    /** Existing exact layouts are a fast path; versionName is never consulted. */
    private static Profile profile(BytecodePatchContext context) {
        BerryProfiles.Profile core = BerryProfiles.select(context);
        String client = core.type("Lc70;"), adapter = core.type("Lc41;");
        String tab = core.type("Ltt5;"), web = core.type("Lcx;");
        for (String layout : Arrays.asList("3.83.74", "3.83.93.1")) {
            Profile candidate = knownProfile(layout);
            if (!candidate.client.equals(client) || !candidate.adapter.equals(adapter)
                    || !candidate.tab.equals(tab) || !candidate.webSubclass.equals(web)) continue;
            try {
                exact(context, candidate.setupOwner + "->" + candidate.setupName + "(" + tab + ")V", candidate.hashes[0]);
                exact(context, client + "->onPageStarted(" + WEB + STR + "Landroid/graphics/Bitmap;)V", candidate.hashes[1]);
                exact(context, tab + "->" + candidate.completeSetter + "(I)V", candidate.hashes[2]);
                exact(context, adapter + "->" + candidate.listenerMethod + "(" + STR + "Ljava/util/Set;" + candidate.listenerCallbackType + ")Z", candidate.hashes[3]);
                exact(context, adapter + "->" + candidate.documentMethod + "(" + STR + STR + "Ljava/util/Set;)" + candidate.scriptHandler, candidate.hashes[4]);
                return candidate;
            } catch (IllegalStateException mismatch) {
                // A stable name is not enough: rediscover the actual typed graph.
            }
        }
        Map<String, ClassDef> definitions = new LinkedHashMap<>();
        context.classDefForEach(c -> { definitions.put(c.getType(), c); return Unit.INSTANCE; });
        Profile result = BfCacheStructure.discover(definitions, client, adapter, tab, web);
        System.out.println("Berry BFCache補正：版番号を使わず復帰処理の構造を検出しました。");
        return result;
    }

    private static Profile knownProfile(String layout) {
        if ("3.83.93.1".equals(layout)) return new Profile(
            "Lt70;", "Lu51;", "Lxw5;", "Le87;", "y", "o", "Lp51;", "r", "Lpx;", "S0", "Lls6;", "Lvr6;", "Lbt6;", "v0", "w", "Lbo4;",
            "9f3955fd214911f5fa1ac6fb78fee87927c182fd90d619809b132114dbcf820c",
            "c640e5a2410efc0c3c27c7367071d368bc018997ec3b5e4c2d771101d3e786a6",
            "5b6f299f8bf4272fcddfefcc950573cf90137b34c970430d073a560601f96bbe",
            "dc61961e6680adfd7c02f30f8d8f0b6a4f3758b6a92483186e0626cdeee527c1",
            "46a7d4616efcfafce2834a22c03a85a3ce397f00b6939987494e6889f484c818");
        if ("3.83.74".equals(layout)) return new Profile(
            "Lc70;", "Lc41;", "Ltt5;", "Lsm2;", "v", "m", "Lx31;", "q", "Lcx;", "T0", "Lip6;", "Lso6;", "Lyp6;", "w0", "v", "Ljn4;",
            "032631c616157e7c03ef5718c9c2b72d9bd8a29a02fcb913cd4bf24bb80f2c25",
            "28ae824793c9c7b01c748ecc88993562d067a670c841b7ff1c6e068e8a5106e5",
            "cfa15cb7a4ef5e76fe31e7d7a82c59f94afdc308c78052cf6ad3bddc15f19a82",
            "007d0531110a61fff046f0998729d94056abd809ae67613a11324951cb0cf272",
            "e7ab556929de829526d28582d1ee873557ebe01e59c2b024bc409dbeb27382fc");
        fail("既知の内部構造名がありません"); return null;
    }
    static final class Profile {
        final String client, adapter, tab, setupOwner, setupName, adapterField, adapterInterface,
                     webField, webSubclass, completeSetter, feature, factory, provider, listenerMethod, documentMethod, scriptHandler;
        final String[] hashes;
        String clientTabField = "a", providerBoundaryField = "a", featureMethod = "b", factoryMethod = "d",
               listenerCallbackType = "Le6;";
        Profile(String client, String adapter, String tab, String setupOwner, String setupName, String adapterField,
                String adapterInterface, String webField, String webSubclass, String completeSetter, String feature,
                String factory, String provider, String listenerMethod, String documentMethod, String scriptHandler, String... hashes) {
            this.client=client; this.adapter=adapter; this.tab=tab; this.setupOwner=setupOwner; this.setupName=setupName;
            this.adapterField=adapterField; this.adapterInterface=adapterInterface; this.webField=webField; this.webSubclass=webSubclass;
            this.completeSetter=completeSetter; this.feature=feature; this.factory=factory; this.provider=provider;
            this.listenerMethod=listenerMethod; this.documentMethod=documentMethod; this.scriptHandler=scriptHandler; this.hashes=hashes;
        }
    }
}
