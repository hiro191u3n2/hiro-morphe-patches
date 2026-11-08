package app.hiro.mapsstartup.patches;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Startup-only coordination of the verified, idempotent native recenter operation. */
public final class MapsStartupLocationPatch {
    private MapsStartupLocationPatch() {}
    public static final String NAME = "起動時に現在地を表示";
    public static final String VERSION = "26.36.04.973607363";
    public static final String CODE = "1068763346";
    public static final String OWNER = "Lnce;";
    public static final String MAIN = "Lcom/google/android/maps/MapsActivity;";
    public static final String LAYOUT = "Lcom/google/android/apps/gmm/base/layout/MainLayout;";
    public static final String RUNTIME = "Lapp/hiro/mapsstartup/StartupLocation;";
    public static final String HOST = "Lapp/hiro/mapsstartup/LocationHost;";
    public static final String BRIDGE = "hiroTryCurrentLocation";
    public static final String EXTENSION = "/extensions/maps_startup_location.mpe";
    public static final Set<String> PACKAGES = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(
        "org.ungoogled.android.apps.maps.microg", "com.google.android.apps.maps")));

    // One non-clobbering invoke plus one alignment NOP at each entry.
    // Four code units preserve every existing switch/array payload's alignment.
    public static final String[] HOOK_NAMES = {
        "onCreate", "onNewIntent", "onResume", "onPause", "onStop", "onStart", "onDestroy", "dispatchTouchEvent"
    };
    public static final String[] HELPER_NAMES = {
        "onCreate", "onNewIntent", "onResume", "onPause", "onStop", "onStart", "onDestroy", "onTouch"
    };
    public static final String[] HOOK_ARGUMENTS = {
        "Landroid/os/Bundle;", "Landroid/content/Intent;", "", "", "", "", "", "Landroid/view/MotionEvent;"
    };
    public static final int[] HOOK_FLAGS = {17, 20, 20, 20, 20, 20, 20, 17};

    /** Complete contracts include fix copying, direct camera control and camera acknowledgement. */
    public static final String[] CONTRACTS = {
        "Lnce;->onCreate(Landroid/os/Bundle;)V",
        "Lnce;->onNewIntent(Landroid/content/Intent;)V",
        "Lnce;->onResume()V",
        "Lnce;->onPause()V",
        "Lnce;->onStop()V",
        "Lnce;->onStart()V",
        "Lnce;->onDestroy()V",
        "Lnce;->dispatchTouchEvent(Landroid/view/MotionEvent;)Z",
        "Lamcr;->f()V",
        "Lamcr;->g()V",
        "Lamcr;->h()Z",
        "Laiau;->K()Z",
        "Lamdp;->b()Lbkmd;",
        "Lamdy;->I(Lbjbd;)Z",
        "Lamgm;->e()Z",
        "Lamdy;->h()Lamgk;",
        "Lamgk;-><clinit>()V",
        "Lbkmd;-><clinit>()V",
        "Lamcp;->a(Lamdk;)V",
        "Lcom/google/android/maps/MapsActivity;-><init>()V",
        "Loye;-><init>(Landroid/app/Activity;Lgfy;Laxtw;Laybr;Laije;Lbehy;Lbgsj;Lgfy;Ljava/util/concurrent/Executor;Lbwon;Lcqpp;Lbbnp;Lnxh;Lcqpp;)V",
        "Lamdp;->l(Lbjoz;Z)V",
        "Lamdp;->j(Lbkmd;Lbjoz;Z)V",
        "Lamdp;->a()Lbjox;",
        "Lamdy;->b()F",
        "Lbjbd;-><init>()V",
        "Lbjbd;->v()Lbjaw;",
        "Lbjaw;->v(Lbjaw;Lbjaw;D)Z",
        "Lbjau;->c(Lbjaw;Lbjaw;)D",
        "Lbjoz;-><init>(Lbikz;FFFLbjox;Lbjoy;)V",
        "Lbjoy;-><clinit>()V"
    };
    // Generated from the supplied APK, with debug positions excluded. Never learned at patch time.
    public static final String[][] HASHES = {
        {"0744aa2eab85b5d8b1126b14944d590fac1232dbd4acff9825eb21c3105b0ff6", "bded78482e5eb78eb4de76e56b18c6faab8faa4f6640a13261c7fa36f395d460"}, {"3a300bcf727002a79ba9fcfc593d39cc80c55332feff33e7f49048bd1b37c347", "98ec077e5af5ca2becc295b5a56511ad55359f6e7226dbd112cab8e8ba299a6b"}, {"a83c245c08dc92e4084f49d60611a15aa5106b61ede01722e9a63c71a5d88f43", "8ec35bef9bd1f2af25558af214e49280df83d75a12f5dab54f55eff02c6b4616"}, {"628c07c540f231f157ec5449e8de36e94b842a922edd587fc84a6c654984cc0e", "41f156e55a0fe2142c96d09da0982472765f6406b3504ecc857eba00745f9940"}, {"edac5bf6dc40b45c7ced7d16727eda76baf341d58cab78b69adb8ec3f0f43187", "1234837d8245365a52a12608424297e315f5c33cc27a21e3570f657104f2972c"}, {"99a4c441c2b3e4ccda0b57e7c80de2a687b5a74197aadec7f9f392aefaafaa54", "3366ba40a0ef8e33ad8120f3b17e9befcab7a63608c784893ba2c4dc6291dba6"}, {"9fff3ad7213d58a70cba2064706323227db3a729e5631e59914421a6c96c81e9", "aef9c9818b65f932ca83b5c73c77764e0e8cd3b175b4c205d4a25ea1a7d50c63"},
        {"a888cb02d23cb4d834dce46ba1bcb874d7b28e4c74d193b9e27d0ef03acb0f61"}, {"206860408e9e63269573356a9f74e59a66161328ed233460fd9e1594ffc76c6b"}, {"081e2b1afec992c66f634157314050ea09c630dcdf591c00584651614afd3ffc", "aed1ac716fc8752d5a662eccbe4ed12dc2b931d8deb6f385572b5c66e6951a45"}, {"b0f3490107a21c68ef93c76b9be1159ade99f8955d39c44f2afcefff02948bb1"}, {"ae2f0f31ca786d46e4dcc9f85ae31596bc2c42aca75f6c52d46f24fe8297ad54"}, {"a9223d70d64831cd45404bebc052d1f8f5ef73c71a90add01b457e0101bdbee0"}, {"50e4603c8f6590876bd4e4f6933905e282d2e0fcbbca1a528e714c73bf8cedd9"},
        {"30b020d0020b88df2e8c4d3ba44efefbd16528fff506e9e01e6f9f1118008f25"}, {"36321d66590e7a6f35351146a4dea0767b7e662e60bc5db50ff32c1bcd0ece34"}, {"263731b1265315206d25b025231b7f153a58c12df447f2c5c679107f5cedca00"}, {"21b4a707a679d9370da4685ca96baea5eeab13d3947b5f9e0f0f17e7dcd65929"}, {"f32b9aba4bd0e0c613a955569786a2f13cdc6e10b1343d216da66a7542076f87", "8ac64659f555822aabdcdb797c5deef001da2274c4e911610d7a5927b5ca6e09"}, {"a5dad5a51e41f702d6edec56876e32b67d780bcc14f65fff57119eab539dd64b"}, {"a5e51398a6887c86d0a33392ab0559e08d12a9b8a9a614c82938f0a90b8065ff", "7698918239751987f591095df0a839d882c51cd15a5c627892a6d7fef0a9c86c"},
        {"dbf761c96398efe577db4fd1200caabbd8f97e9133cee94954a5649fca19e115"},
        {"552b7c1e9981720114eca1351deb736fb45c7a00bd01848475b64a677e4949ca", "9d446bada85a9b0c936b866cea40b773c503aad993a12adce536a5f870c40d25"},
        {"db68544175992ccb7a312c3ea339805d0f01536f95cb9226d69b5fb1b101a41e"},
        {"adf4e8bf67e4b66ae3082af3b951151d374c45e75650179aa6b0708b105babe6"},
        {"08bf5b1e10f9cb63a224ad474656c3129c136e570801eb6a9f13671a5392025d"},
        {"720c6d94bf8b079b03a7d4537c32c71d24b13bdc71cf30f6662c443f5ab42985"},
        {"57a3ca3ef54eb00459d9806224bd6d605389edcba2f3465be8bd82f836957b52", "1ba3b5bb2b52fa2ed171bfac1d6d4e7c74f3563622409f2d539db41f21bd7730"},
        {"e4ec9fadec9356936620b9ed98aa48e252deb692b8e2633ad6fa719317f42df5", "3f6b2570780ab6ee0e58f7278c19bf21b7ea328c97ed5f20aac97f3ad4733680"},
        {"9dd80f9b960e27eb7a23adc1256b7bb6d9ecf83f84bce932d80becba08f60fdc"},
        {"e2914aef5ca63b7a28e2e31aad7df967f1ca2c82b74b47f3108453cbab98f7d8"}
    };

    public static Set<Pair<String,Set<String>>> compatibility() {
        Set<Pair<String,Set<String>>> result = new LinkedHashSet<Pair<String,Set<String>>>();
        for (String p : PACKAGES) result.add(new Pair<String,Set<String>>(p, Collections.singleton(VERSION)));
        return Collections.unmodifiableSet(result);
    }

    private static final BytecodePatch GUARD = new BytecodePatch(null, null, false, compatibility(),
        Collections.<Patch<?>>emptySet(), Collections.emptySet(), null,
        new Function1<BytecodePatchContext,Unit>() { public Unit invoke(final BytecodePatchContext c) {
            validatePackage(c.getPackageMetadata().getPackageName(), c.getPackageMetadata().getVersionName(), c.getPackageMetadata().getVersionCode());
            require(c.classDefByOrNull(RUNTIME) == null && c.classDefByOrNull(HOST) == null, "Startup location runtime already installed");
            validateNative(new Lookup() { public ClassDef get(String owner) { return c.classDefByOrNull(owner); }});
            return Unit.INSTANCE;
        }}, null);

    public static final BytecodePatch PATCH = new BytecodePatch(NAME,
        "添付版 microG Maps 26.36.04.973607363（対象処理が一致する版）用。Google マップの選択画面にも表示しますが、対象処理が一致しないAPKには適用しません。新規起動時だけ現在地へ移動し、起動直後にタップしても準備待ちを続けます。ホームや別アプリへ移った後のタスク復帰、履歴からの復元では再移動せず、起動時の未完了処理も停止します。到達後の操作、住所リンクや経路案内を優先します。位置情報権限が必要です。旧パッチ適用済みAPKへの重ね掛けはできません。元のAPKに適用してください。Android端末での画面動作は未検証です。",
        true, compatibility(), Collections.<Patch<?>>singleton(GUARD), Collections.emptySet(),
        new Supplier<InputStream>() { public InputStream get() {
            InputStream result = MapsStartupLocationPatch.class.getResourceAsStream(EXTENSION);
            require(result != null, "Missing startup location extension"); return result;
        }}, new Function1<BytecodePatchContext,Unit>() { public Unit invoke(final BytecodePatchContext c) {
            require(c.classDefByOrNull(RUNTIME) != null && c.classDefByOrNull(HOST) != null, "Startup location extension did not load");
            require(find(c.classDefBy(HOST), HOST + "->" + BRIDGE + "(Z)I").getAccessFlags() == 1025,
                    "Startup location interface changed");
            for (int n = 0; n < HOOK_NAMES.length; n++) {
                String args = "Landroid/app/Activity;" + HOOK_ARGUMENTS[n];
                Method entry = find(c.classDefBy(RUNTIME), RUNTIME + "->" + HELPER_NAMES[n] + "(" + args + ")V");
                require((entry.getAccessFlags() & 9) == 9, "Startup runtime entry is not public static");
            }
            apply(c.mutableClassDefBy(OWNER));
            return Unit.INSTANCE;
        }}, null);

    public interface Lookup { ClassDef get(String owner); }
    public static void require(boolean value, String reason) {
        if (!value) throw new IllegalStateException("Maps startup location: " + reason);
    }
    public static void validatePackage(String p, String v, String code) {
        require(PACKAGES.contains(p) && VERSION.equals(v) && CODE.equals(code),
                "Requires supplied microG Maps 26.36.04.973607363 (1068763346) verified native code");
    }
    public static String descriptor(MethodReference m) {
        StringBuilder result = new StringBuilder(m.getDefiningClass()).append("->").append(m.getName()).append('(');
        for (CharSequence p : m.getParameterTypes()) result.append(p);
        return result.append(')').append(m.getReturnType()).toString();
    }
    public static String owner(String descriptor) { return descriptor.substring(0, descriptor.indexOf("->")); }
    public static Method find(ClassDef c, String descriptor) {
        require(c != null, "Missing class for " + descriptor);
        Method result = null;
        for (Method m : c.getMethods()) if (descriptor.equals(descriptor(m))) {
            require(result == null, "Duplicate method " + descriptor); result = m;
        }
        require(result != null, "Missing method " + descriptor); return result;
    }
    public static String fingerprint(Method m) {
        try {
            MethodImplementation b = m.getImplementation();
            require(b != null, "Missing body " + descriptor(m));
            ImmutableMethodImplementation normalized = new ImmutableMethodImplementation(b.getRegisterCount(),
                b.getInstructions(), b.getTryBlocks(), Collections.emptyList());
            ImmutableMethod method = new ImmutableMethod(m.getDefiningClass(), m.getName(), m.getParameters(), m.getReturnType(),
                m.getAccessFlags(), m.getAnnotations(), m.getHiddenApiRestrictions(), normalized);
            ImmutableClassDef wrapper = new ImmutableClassDef(m.getDefiningClass(), 1, "Ljava/lang/Object;", Collections.emptyList(),
                null, Collections.emptySet(), Collections.emptyList(), Collections.singleton(method));
            MemoryDataStore store = new MemoryDataStore();
            try {
                DexPool.writeTo(store, new ImmutableDexFile(Opcodes.forApi(29), Collections.singleton(wrapper)));
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(store.getBuffer(), store.getSize()));
                StringBuilder result = new StringBuilder();
                for (byte item : hash) result.append(String.format(Locale.ROOT, "%02x", item & 255));
                return result.toString();
            } finally { store.close(); }
        } catch (Exception error) { throw new IllegalStateException("Cannot fingerprint Maps native method", error); }
    }
    public static void validateContract(Method m, int contract) {
        require(CONTRACTS[contract].equals(descriptor(m)) && m.getImplementation() != null, "Native declaration changed " + contract);
        if (contract < HOOK_NAMES.length) require(m.getAccessFlags() == HOOK_FLAGS[contract], "Lifecycle flags changed " + contract);
        require(Arrays.asList(HASHES[contract]).contains(fingerprint(m)), "Unverified, modified or already patched native body " + CONTRACTS[contract]);
    }
    private static void field(Lookup l, String owner, String name, String type, int access) {
        ClassDef c = l.get(owner); require(c != null, "Missing field owner " + owner);
        int matches = 0;
        for (Field f : c.getFields()) if (name.equals(f.getName()) && type.equals(f.getType())) {
            require(f.getAccessFlags() == access, "Field access changed " + owner + "->" + name); matches++;
        }
        require(matches == 1, "Field missing or ambiguous " + owner + "->" + name);
    }
    public static void validateNative(Lookup l) {
        ClassDef main = l.get(MAIN), activity = l.get(OWNER);
        require(main != null && OWNER.equals(main.getSuperclass()), "Launcher hierarchy changed");
        require(activity != null && "Lncs;".equals(activity.getSuperclass()), "Activity hierarchy changed");
        require(l.get("Lamcr;") != null && "Laiau;".equals(l.get("Lamcr;").getSuperclass()), "Location controller hierarchy changed");
        require(!activity.getInterfaces().contains(HOST), "Location host already installed");
        for (Method m : activity.getMethods()) require(!BRIDGE.equals(m.getName()), "Native bridge already installed");
        for (int n = 0; n < CONTRACTS.length; n++) validateContract(find(l.get(owner(CONTRACTS[n])), CONTRACTS[n]), n);
        field(l, OWNER, "bx", LAYOUT, 4);
        field(l, LAYOUT, "i", "Lcqpp;", 1);
        field(l, "Lamcr;", "p", "Lamdp;", 17);
        field(l, "Lamdp;", "d", "Lamej;", 17);
        field(l, "Lamdp;", "f", "Z", 1);
        field(l, "Lamdp;", "g", "Lbizr;", 1);
        field(l, "Lamdp;", "k", "Lamds;", 1);
        field(l, "Lamdp;", "l", "Lamdt;", 1);
        field(l, "Lamdp;", "p", "Lbjja;", 1);
        field(l, "Lamgk;", "a", "Lamgk;", 16409);
        field(l, "Lamgk;", "l", "Lamgk;", 16409);
        field(l, "Lamgk;", "m", "Lamgk;", 16409);
        field(l, "Lbkmd;", "b", "Lbkmd;", 16409);
        field(l, "Lbkmd;", "c", "Lbkmd;", 16409);
        field(l, "Lbjoy;", "a", "Lbjoy;", 16409);
        field(l, "Lbjoy;", "b", "Lbjoy;", 16409);
        field(l, "Lbjjc;", "a", "Lbjaw;", 17);
        field(l, "Lbjjc;", "h", "F", 17);
        field(l, "Lbjjc;", "e", "F", 17);
        field(l, "Lbjjc;", "d", "F", 17);
        Method provider = find(l.get("Lcqpp;"), "Lcqpp;->a()Ljava/lang/Object;");
        Method mode = find(l.get("Lamej;"), "Lamej;->h()Lamgk;");
        require(provider.getAccessFlags() == 1025 && mode.getAccessFlags() == 1025,
                "Provider or map-mode interface changed");
        for (String signature : Arrays.asList("Lamej;->I(Lbjbd;)Z", "Lamej;->b()F", "Lbjja;->c()Lbjjc;"))
            require(find(l.get(owner(signature)), signature).getAccessFlags() == 1025,
                    "Native camera interface changed " + signature);
    }
    public static int hookIndex(Method m) {
        String d = descriptor(m);
        for (int n = 0; n < HOOK_NAMES.length; n++) if (CONTRACTS[n].equals(d)) return n;
        return -1;
    }
    public static void applyHook(MutableMethod m, int n) {
        validateContract(m, n);
        MutableMethodImplementation body = m.getImplementation();
        List<String> parameters = new ArrayList<String>();
        parameters.add("Landroid/app/Activity;");
        if (!HOOK_ARGUMENTS[n].isEmpty()) parameters.add(HOOK_ARGUMENTS[n]);
        int count = parameters.size(), start = body.getRegisterCount() - count;
        require(start >= 0, "Invalid lifecycle registers");
        ImmutableMethodReference target = new ImmutableMethodReference(RUNTIME, HELPER_NAMES[n], parameters, "V");
        body.addInstruction(0, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, start, count, target));
        body.addInstruction(1, new BuilderInstruction10x(Opcode.NOP));
    }
    public static String bridgeCode() {
        // 0 = wait; 1 = camera acknowledged; 2 = issued; -1 = route/nav cancel.
        // p1=false is strictly a read-only acknowledgement poll. The native f/g
        // permission callback path is deliberately absent: it can later toggle
        // tracking/compass and cannot be withdrawn after user interaction.
        // A nearby cached fix is acknowledged only while native follow is active,
        // so a later fresh fix still updates the map after startup completes.
        return "iget-object v0, p0, Lnce;->bx:" + LAYOUT + "\n" +
            "if-eqz v0, :wait\n" +
            "iget-object v0, v0, " + LAYOUT + "->i:Lcqpp;\n" +
            "if-eqz v0, :wait\n" +
            "invoke-interface {v0}, Lcqpp;->a()Ljava/lang/Object;\n" +
            "move-result-object v0\n" +
            "instance-of v1, v0, Lamcr;\n" +
            "if-eqz v1, :wait\n" +
            "check-cast v0, Lamcr;\n" +
            "invoke-virtual {v0}, Laiau;->K()Z\n" +
            "move-result v1\n" +
            "if-eqz v1, :wait\n" +
            "iget-object v0, v0, Lamcr;->p:Lamdp;\n" +
            "if-eqz v0, :wait\n" +
            "iget-object v1, v0, Lamdp;->d:Lamej;\n" +
            "if-eqz v1, :wait\n" +
            "invoke-interface {v1}, Lamej;->h()Lamgk;\n" +
            "move-result-object v2\n" +
            "if-eqz v2, :wait\n" +
            "sget-object v3, Lamgk;->l:Lamgk;\n" +
            "if-eq v2, v3, :wait\n" +
            "sget-object v3, Lamgk;->a:Lamgk;\n" +
            "if-eq v2, v3, :map\n" +
            "sget-object v3, Lamgk;->m:Lamgk;\n" +
            "if-ne v2, v3, :cancel\n" +
            ":map\n" +
            "iget-object v2, v0, Lamdp;->g:Lbizr;\n" +
            "if-eqz v2, :wait\n" +
            "iget-boolean v2, v0, Lamdp;->f:Z\n" +
            "if-eqz v2, :old_camera\n" +
            "iget-object v2, v0, Lamdp;->l:Lamdt;\n" +
            "goto :camera_ready\n" +
            ":old_camera\n" +
            "iget-object v2, v0, Lamdp;->k:Lamds;\n" +
            ":camera_ready\n" +
            "if-eqz v2, :wait\n" +
            "iget-object v2, v0, Lamdp;->p:Lbjja;\n" +
            "if-eqz v2, :wait\n" +
            "new-instance v3, Lbjbd;\n" +
            "invoke-direct {v3}, Lbjbd;-><init>()V\n" +
            "invoke-interface {v1, v3}, Lamej;->I(Lbjbd;)Z\n" +
            "move-result v4\n" +
            "if-eqz v4, :wait\n" +
            "invoke-interface {v2}, Lbjja;->c()Lbjjc;\n" +
            "move-result-object v2\n" +
            "if-eqz v2, :wait\n" +
            "invoke-virtual {v0}, Lamdp;->b()Lbkmd;\n" +
            "move-result-object v10\n" +
            "if-eqz v10, :wait\n" +
            "invoke-virtual {v3}, Lbjbd;->v()Lbjaw;\n" +
            "move-result-object v3\n" +
            "iget-object v4, v2, Lbjjc;->a:Lbjaw;\n" +
            "const-wide v5, 0x4049000000000000L\n" +
            "invoke-static {v3, v4, v5, v6}, Lbjaw;->v(Lbjaw;Lbjaw;D)Z\n" +
            "move-result v3\n" +
            "iget v7, v2, Lbjjc;->h:F\n" +
            "const/high16 v4, 0x41500000\n" +
            "cmpl-float v4, v7, v4\n" +
            "if-ltz v4, :need_move\n" +
            "if-eqz v3, :need_move\n" +
            "sget-object v3, Lbkmd;->b:Lbkmd;\n" +
            "if-eq v10, v3, :confirmed\n" +
            "sget-object v3, Lbkmd;->c:Lbkmd;\n" +
            "if-ne v10, v3, :need_move\n" +
            ":confirmed\n" +
            "const/4 v3, 0x1\n" +
            "return v3\n" +
            ":need_move\n" +
            "if-eqz p1, :wait\n" +
            "if-gez v4, :zoom_ready\n" +
            "const/high16 v7, 0x41700000\n" +
            ":zoom_ready\n" +
            "iget v8, v2, Lbjjc;->e:F\n" +
            "iget v9, v2, Lbjjc;->d:F\n" +
            "sget-object v11, Lbjoy;->a:Lbjoy;\n" +
            "sget-object v4, Lbkmd;->c:Lbkmd;\n" +
            "if-ne v10, v4, :bearing_ready\n" +
            "sget-object v11, Lbjoy;->b:Lbjoy;\n" +
            "invoke-interface {v1}, Lamej;->b()F\n" +
            "move-result v4\n" +
            "sub-float/2addr v9, v4\n" +
            ":bearing_ready\n" +
            "invoke-virtual {v0}, Lamdp;->a()Lbjox;\n" +
            "move-result-object v10\n" +
            "const/4 v6, 0x0\n" +
            "new-instance v5, Lbjoz;\n" +
            "invoke-direct/range {v5 .. v11}, Lbjoz;-><init>(Lbikz;FFFLbjox;Lbjoy;)V\n" +
            "const/4 v3, 0x0\n" +
            "invoke-virtual {v0, v5, v3}, Lamdp;->l(Lbjoz;Z)V\n" +
            "const/4 v3, 0x2\n" +
            "return v3\n" +
            ":cancel\n" +
            "const/4 v3, -0x1\n" +
            "return v3\n" +
            ":wait\n" +
            "const/4 v3, 0x0\n" +
            "return v3";
    }
    public static MutableMethod bridge() {
        MutableMethod m = new MutableMethod(new ImmutableMethod(OWNER, BRIDGE,
            Collections.singletonList(new ImmutableMethodParameter("Z", Collections.emptySet(), "issue")), "I", 17,
            Collections.emptySet(), Collections.emptySet(), new ImmutableMethodImplementation(14,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList())));
        InstructionExtensions.INSTANCE.addInstructions(m, 0, bridgeCode());
        return m;
    }
    public static void apply(MutableClass activity) {
        require(activity != null && OWNER.equals(activity.getType()) && !activity.getInterfaces().contains(HOST), "Location host already applied or wrong target");
        for (Method m : activity.getMethods()) require(!BRIDGE.equals(m.getName()), "Location bridge already applied");
        List<MutableMethod> hooks = new ArrayList<MutableMethod>();
        // Complete all validation before mutating the first method.
        for (int n = 0; n < HOOK_NAMES.length; n++) {
            Method found = find(activity, CONTRACTS[n]); validateContract(found, n); hooks.add((MutableMethod)found);
        }
        for (int n = 0; n < hooks.size(); n++) applyHook(hooks.get(n), n);
        activity.getInterfaces().add(HOST);
        require(activity.getMethods().add(bridge()), "Failed adding native bridge");
    }
}
