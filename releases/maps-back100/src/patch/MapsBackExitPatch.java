package app.hiro.mapsback.patches;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/**
 * Adds the first (lowest-priority) enabled handler to Maps' own back dispatcher.
 * Later native handlers stay ahead of it because Lamee.h uses deque.addFirst.
 * Unlike only changing Activity.onBackPressed, this keeps native API 33+ back
 * dispatch enabled at the map root, where Android otherwise backgrounds the task.
 * No listener, lifecycle method, manifest option or process state is replaced.
 */
public final class MapsBackExitPatch {
    private MapsBackExitPatch() {}
    public static final String NAME = "戻るで終了して履歴から削除";
    public static final String VERSION = "26.36.04.973607363";
    public static final String CODE = "1068763346";
    public static final String OWNER = "Lps;";
    public static final String TARGET = OWNER;
    public static final String TARGET_METHOD = "Lps;->a()Ljava/lang/Object;";
    public static final String MAIN = "Lcom/google/android/maps/MapsActivity;";
    public static final String CALLBACK = "Lapp/hiro/mapsback/NativeBackFallback;";
    public static final String ACTIVITY = "Landroid/app/Activity;";
    public static final String DISPATCHER = "Lamet;";
    public static final String NATIVE_CALLBACK = "Lqi;";
    public static final String INSTALL = CALLBACK + "->install(Ljava/lang/Object;Lamet;)V";
    public static final String ANCHOR = "Lamet;-><init>(Ljava/lang/Runnable;)V";
    public static final Set<String> PACKAGES = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(
            "org.ungoogled.android.apps.maps.microg", "com.google.android.apps.maps")));

    public static final String[] CONTRACTS = {
        TARGET_METHOD,
        "Lnce;->onBackPressed()V",
        "Lpw;->nQ()Lamet;",
        ANCHOR,
        "Lamet;->as()Lql;",
        "Lamet;->at(Lqi;)V",
        "Lamet;->av()V",
        "Lamet;->aw(Landroid/window/OnBackInvokedDispatcher;)V",
        "Lql;-><init>(Lamet;)V",
        "Lqi;-><init>(Z)V",
        "Lqi;->g(Lgdz;)Lqh;",
        "Lqh;-><init>(Lqi;Lgdz;)V",
        "Lqh;->b()V",
        "Lamee;->h(Lijz;)V",
        "Likd;->a(I)Lijz;",
        "Likc;->b()V",
        "Likh;-><init>(Landroid/window/OnBackInvokedDispatcher;I)V",
        "Likh;->c(Z)V",
        "Likh;->e(Z)V",
        "Lqm;->a(Lgre;Lgqu;)V",
        "Lpw;->B(Lamet;)V",
        "Lijz;-><init>(Lgdz;Z)V",
        "Lijz;->h()Z"
    };
    // Generated from both supplied APKs using the legacy and 2.2 SDK serializers, with debug positions excluded.
    // Native methods are never accepted or fingerprinted into this allowlist at patch time.
    public static final String[][] HASHES = {
        {"fe90c0dff4b13c2c7c465f95443816459fd61aad6e16204cde0fe13354a56172", "6b92de26322e2458578c5703e6673c24ce95a94298534c84f59f5e4eeeb67132"}, // 0 Lps;->a()Ljava/lang/Object;
        {"72c0032a4a15fd7b0602ab33f459b432b15c5458eca8681c90b5f1775a2a56fe"}, // 1 Lnce;->onBackPressed()V
        {"94ddf0fdacfd5ccc02c917c88a427eaaf1e7a521edf8d53ff35957a4f22104ed"}, // 2 Lpw;->nQ()Lamet;
        {"1e6ff225b7c802386cb170aeefd3bed6266218d296a2c11d2a2c80e1f8a000a3", "e560fdd672f88c0109a94d3ca4d28e039a907368e6129f9816af337c91b09085"}, // 3 Lamet;-><init>(Ljava/lang/Runnable;)V
        {"447ffa5a76eaf15e8ad240eb3dd45b34cdede872a55c95bcf38a17927ac1e540"}, // 4 Lamet;->as()Lql;
        {"dfa8514f51f7cf1b35b10e3d4251c7595c076aa2a569ff0c8de30ded59249fba", "5870f79dd243d1a7db714c272352623e7d9e61370f3acd6cb4481c2d2db51b22"}, // 5 Lamet;->at(Lqi;)V
        {"e6bde55827baa1348669a8c3fb9439aef49906ac7968377c72bfb959a7c77cfc"}, // 6 Lamet;->av()V
        {"f4d476c4cbc1021784af88609f23e14bcb2653fa263187583623a652fd7782cc"}, // 7 Lamet;->aw(Landroid/window/OnBackInvokedDispatcher;)V
        {"4e53837a4e112e6ec1f42d64b3dc6364fdbd1798a72eb3d244b67f416be2dbd9", "615fbe15a19da075c81c52f21e19b0583308e278b189d201ed1e1efa1548b95c"}, // 8 Lql;-><init>(Lamet;)V
        {"eda8c5c4d0d3453fadf49678d02bd4b460ac9b0b8aa456b67141cb8a72ebaff3"}, // 9 Lqi;-><init>(Z)V
        {"2fca4ce02c6d750d105a9aa17e5de2d0d9f734e232582b7ba6051e6e88b0dcc1", "9d665966747e1e4a01bc3b5655bd8f2009ac49be6fe5d6be14cfcea4cad6db36"}, // 10 Lqi;->g(Lgdz;)Lqh;
        {"4226f86dacd0fc3c8a5d8a9ed65fa6bd65e9d716e8cfb1a0d435b8d4671e301d", "9149bd620d0e0bf5fd1764ebc179195c29914bc0a737c7d38c1639733a5a8f68"}, // 11 Lqh;-><init>(Lqi;Lgdz;)V
        {"a07885b34c0b668bcab671d4e2b83d48ce206a60bf4a8863f40b6ee630450811"}, // 12 Lqh;->b()V
        {"d45bbe97810c9e28a7350bc73dd5837944c09a1c1bde28d1c63b945c34b01d12"}, // 13 Lamee;->h(Lijz;)V
        {"410abd35bc596a95c90cd82e0b908fd0b2831b10f89905f19879119f995e93dd"}, // 14 Likd;->a(I)Lijz;
        {"a29ede7340411c3a38629fabc8703a608032078dfdd219ecc3d796a182bfa257", "2fdbf39f3d3a31797d09e23391c299f53ba7ff09271d1b39bcc5e32e7cb6ad04"}, // 15 Likc;->b()V
        {"aa3262d7deac0411214f1cd80f37906c2abd47d3fbc1268564efc043f856130e", "380546b9c81a23709cab8b9a4611f50b7141f946c2570eeecc3a8b52aea417d0"}, // 16 Likh;-><init>(Landroid/window/OnBackInvokedDispatcher;I)V
        {"5e6a210f44ac9f42931107f2567574d407cc5f640260855eada0828d80387567"}, // 17 Likh;->c(Z)V
        {"40c1173bb0ab0e31bbeb7dc82e42a3372482c8c3ee656c64167814133a54aa63", "cff305e38f4eeb50a27910f2c72504890ee3c2c407590ec2bbe7dd3f3a333695"}, // 18 Likh;->e(Z)V
        {"5af427a06d43cf2613e61a0a2aa69f79e13814e5cff277e9226e4082d5c92faf", "1ddcf074b98396ecba918af8ccc4d54dd5a454eaa5b2ced42ab20f56e7cd80e1"}, // 19 Lqm;->a(Lgre;Lgqu;)V
        {"69a30042b01ca4c1ce5b511d1d50d3c041b2c82ebe34da6ba2abf48a6d77b657"}, // 20 Lpw;->B(Lamet;)V
        {"a60de4f6f496f03c39d6d01972a876a6cb30ececaaeffe20807d2ae00b01d27c"}, // 21 Lijz;-><init>(Lgdz;Z)V
        {"e4d67f3710eeea30608449042d8ec9e29740929a96a0d7e59ef10ec13955afb3"}, // 22 Lijz;->h()Z
    };

    public interface Lookup { ClassDef get(String type); }

    public static Set<Pair<String,Set<String>>> compatibility() {
        Set<Pair<String,Set<String>>> result = new LinkedHashSet<Pair<String,Set<String>>>();
        for (String p : PACKAGES) result.add(new Pair<String,Set<String>>(p, Collections.singleton(VERSION)));
        return Collections.unmodifiableSet(result);
    }

    private static final BytecodePatch GUARD = new BytecodePatch(null, null, false, compatibility(),
        Collections.<Patch<?>>emptySet(), Collections.emptySet(), null,
        new Function1<BytecodePatchContext,Unit>() { public Unit invoke(final BytecodePatchContext c) {
            validatePackage(c.getPackageMetadata().getPackageName(), c.getPackageMetadata().getVersionName(),
                    c.getPackageMetadata().getVersionCode());
            validateNative(new Lookup() { public ClassDef get(String type) { return c.classDefByOrNull(type); }});
            return Unit.INSTANCE;
        }}, null);

    public static final BytecodePatch PATCH = new BytecodePatch(NAME,
        "microG Maps 26.36.04.973607363用。検索結果・場所の詳細・キーボードなどは通常どおり戻り、地図画面から戻るで終了するときに最近使ったアプリの履歴から削除します。Androidの戻るボタンとジェスチャーに対応する既存の処理を使用します。起動時現在地・カテゴリ行非表示と併用できます。端末上の動作は未検証です。同じパッチの重ね掛けには対応しません。",
        true, compatibility(), Collections.<Patch<?>>singleton(GUARD), Collections.emptySet(),
        new Supplier<InputStream>() { public InputStream get() {
            byte[] dex = extensionDex();
            require(dex.length > 0, "Empty native fallback extension");
            return new ByteArrayInputStream(dex);
        }}, new Function1<BytecodePatchContext,Unit>() { public Unit invoke(final BytecodePatchContext c) {
            require(c.classDefByOrNull(CALLBACK) != null, "Native fallback extension did not load");
            apply(c.mutableClassDefBy(OWNER));
            return Unit.INSTANCE;
        }}, null);

    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Maps back exit: " + message);
    }
    public static void validatePackage(String p, String v, String code) {
        require(PACKAGES.contains(p) && VERSION.equals(v) && CODE.equals(code),
                "Requires verified Maps 26.36.04.973607363 (1068763346)");
    }
    public static String descriptor(MethodReference m) {
        StringBuilder s = new StringBuilder(m.getDefiningClass()).append("->").append(m.getName()).append('(');
        for (CharSequence p : m.getParameterTypes()) s.append(p);
        return s.append(')').append(m.getReturnType()).toString();
    }
    public static String owner(String d) { return d.substring(0, d.indexOf("->")); }
    public static Method find(ClassDef c, String d) {
        require(c != null, "Missing class for " + d);
        Method result = null;
        for (Method m : c.getMethods()) if (d.equals(descriptor(m))) {
            require(result == null, "Ambiguous method " + d); result = m;
        }
        require(result != null, "Missing method " + d); return result;
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
            MemoryDataStore out = new MemoryDataStore();
            try {
                DexPool.writeTo(out, new ImmutableDexFile(Opcodes.forApi(29), Collections.singleton(wrapper)));
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(out.getBuffer(), out.getSize()));
                StringBuilder result = new StringBuilder();
                for (byte v : hash) result.append(String.format(Locale.ROOT, "%02x", v & 255));
                return result.toString();
            } finally { out.close(); }
        } catch (Exception e) { throw new IllegalStateException("Cannot fingerprint Maps native method", e); }
    }
    public static void validateContract(Method m, int i) {
        require(i >= 0 && i < CONTRACTS.length && HASHES.length == CONTRACTS.length, "Invalid contract table");
        require(CONTRACTS[i].equals(descriptor(m)) && m.getImplementation() != null, "Native declaration changed " + i);
        require(Arrays.asList(HASHES[i]).contains(fingerprint(m)), "Unverified, modified or already patched native body " + CONTRACTS[i]);
    }
    private static void hierarchy(Lookup l, String type, String parent, boolean publicClass) {
        ClassDef c = l.get(type);
        require(c != null && parent.equals(c.getSuperclass()), "Class hierarchy changed " + type);
        if (publicClass) require((c.getAccessFlags() & 1) == 1, "Native class no longer public " + type);
    }
    public static void validateNative(Lookup l) {
        require(l.get(CALLBACK) == null, "Back-exit patch already installed; use an APK without this patch");
        String[] chain = {MAIN, "Lnce;", "Lncs;", "Lnxb;", "Layeq;", "Laxsp;", "Lef;", "Lbk;", "Lpw;", "Lcw;", ACTIVITY};
        for (int i = 0; i + 1 < chain.length; i++) hierarchy(l, chain[i], chain[i+1], true);
        hierarchy(l, OWNER, "Ljava/lang/Object;", true);
        hierarchy(l, DISPATCHER, "Ljava/lang/Object;", true);
        hierarchy(l, NATIVE_CALLBACK, "Ljava/lang/Object;", true);
        hierarchy(l, "Lql;", "Likc;", true);
        hierarchy(l, "Lqh;", "Lijz;", true);
        hierarchy(l, "Likh;", "Likc;", true);
        require((l.get(NATIVE_CALLBACK).getAccessFlags() & (AccessFlags.FINAL.getValue() | AccessFlags.INTERFACE.getValue())) == 0,
                "Native back callback cannot be subclassed");
        for (int i = 0; i < CONTRACTS.length; i++) validateContract(find(l.get(owner(CONTRACTS[i])), CONTRACTS[i]), i);
        require(find(l.get(NATIVE_CALLBACK), "Lqi;->b()V").getAccessFlags() == 1025, "Native back callback virtual contract changed");
        injectionIndex(find(l.get(OWNER), TARGET_METHOD));
    }

    /** The factory has 12 locals and p0=v12 contains the Activity at this constructor. */
    public static int injectionIndex(Method m) {
        require(TARGET_METHOD.equals(descriptor(m)) && m.getAccessFlags() == 17 && m.getImplementation() != null
                && m.getImplementation().getRegisterCount() == 13, "Dispatcher factory declaration changed");
        int index = 0, result = -1;
        for (Instruction in : m.getImplementation().getInstructions()) {
            if (in instanceof ReferenceInstruction) {
                Reference r = ((ReferenceInstruction)in).getReference();
                if (r instanceof MethodReference && ANCHOR.equals(descriptor((MethodReference)r))) {
                    require(result < 0 && in.getOpcode() == Opcode.INVOKE_DIRECT && in instanceof FiveRegisterInstruction,
                            "Dispatcher constructor is absent or ambiguous");
                    FiveRegisterInstruction v = (FiveRegisterInstruction)in;
                    require(v.getRegisterCount() == 2 && v.getRegisterC() == 0 && v.getRegisterD() == 3,
                            "Dispatcher constructor registers changed");
                    result = index + 1;
                }
            }
            index++;
        }
        require(result > 0, "Dispatcher factory constructor missing"); return result;
    }
    public static void apply(MutableClass c) {
        MutableMethod m = (MutableMethod)find(c, TARGET_METHOD);
        validateContract(m, 0);
        int at = injectionIndex(m);
        MutableMethodImplementation body = m.getImplementation();
        body.addInstruction(at, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2, 12, 0, 0, 0, 0,
                new ImmutableMethodReference(CALLBACK, "install", Arrays.asList("Ljava/lang/Object;", DISPATCHER), "V")));
        body.addInstruction(at + 1, new BuilderInstruction10x(Opcode.NOP));
    }

    private static ImmutableMethod method(String name, List<String> args, String result, int flags, MethodImplementation code) {
        List<ImmutableMethodParameter> parameters = new ArrayList<ImmutableMethodParameter>();
        for (String a : args) parameters.add(new ImmutableMethodParameter(a, Collections.emptySet(), null));
        return new ImmutableMethod(CALLBACK, name, parameters, result, flags, Collections.emptySet(), Collections.emptySet(), code);
    }
    private static BuilderInstruction35c invoke(Opcode opcode, String owner, String name, List<String> args, String result, int... registers) {
        int[] r = new int[5]; require(registers.length <= 5, "Too many invocation registers");
        for (int i=0; i<registers.length; i++) { require(registers[i] >= 0 && registers[i] <= 15, "Invocation register overflow"); r[i] = registers[i]; }
        return new BuilderInstruction35c(opcode, registers.length, r[0], r[1], r[2], r[3], r[4],
                new ImmutableMethodReference(owner, name, args, result));
    }

    /** Exactly one generated native subclass; no dependency on a separately compiled runtime. */
    public static ImmutableClassDef callback() {
        ImmutableFieldReference activity = new ImmutableFieldReference(CALLBACK, "activity", ACTIVITY);
        MethodImplementationBuilder ctor = new MethodImplementationBuilder(3);
        ctor.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 0, 1));
        ctor.addInstruction(invoke(Opcode.INVOKE_DIRECT, NATIVE_CALLBACK, "<init>", Collections.singletonList("Z"), "V", 1, 0));
        ctor.addInstruction(new BuilderInstruction22c(Opcode.IPUT_OBJECT, 2, 1, activity));
        ctor.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        MethodImplementationBuilder back = new MethodImplementationBuilder(2);
        back.addInstruction(new BuilderInstruction22c(Opcode.IGET_OBJECT, 0, 1, activity));
        back.addInstruction(invoke(Opcode.INVOKE_VIRTUAL, ACTIVITY, "finishAndRemoveTask", Collections.emptyList(), "V", 0));
        back.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        MethodImplementationBuilder install = new MethodImplementationBuilder(3);
        install.addInstruction(new BuilderInstruction22c(Opcode.INSTANCE_OF, 0, 1, new ImmutableTypeReference(MAIN)));
        install.addInstruction(new BuilderInstruction21t(Opcode.IF_EQZ, 0, install.getLabel("done")));
        install.addInstruction(new BuilderInstruction21c(Opcode.CHECK_CAST, 1, new ImmutableTypeReference(ACTIVITY)));
        install.addInstruction(new BuilderInstruction21c(Opcode.NEW_INSTANCE, 0, new ImmutableTypeReference(CALLBACK)));
        install.addInstruction(invoke(Opcode.INVOKE_DIRECT, CALLBACK, "<init>", Collections.singletonList(ACTIVITY), "V", 0, 1));
        install.addInstruction(invoke(Opcode.INVOKE_VIRTUAL, DISPATCHER, "at", Collections.singletonList(NATIVE_CALLBACK), "V", 2, 0));
        install.addLabel("done");
        install.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        List<ImmutableMethod> methods = Arrays.asList(
                method("<init>", Collections.singletonList(ACTIVITY), "V", 65537, ctor.getMethodImplementation()),
                method("b", Collections.emptyList(), "V", 17, back.getMethodImplementation()),
                method("install", Arrays.asList("Ljava/lang/Object;", DISPATCHER), "V", 9, install.getMethodImplementation()));
        ImmutableField field = new ImmutableField(CALLBACK, "activity", ACTIVITY, 18, null, Collections.emptySet(), Collections.emptySet());
        return new ImmutableClassDef(CALLBACK, 17, NATIVE_CALLBACK, Collections.emptyList(), null, Collections.emptySet(),
                Collections.singleton(field), methods);
    }
    public static byte[] extensionDex() {
        try {
            MemoryDataStore out = new MemoryDataStore();
            try {
                DexPool.writeTo(out, new ImmutableDexFile(Opcodes.forApi(28), Collections.singleton(callback())));
                return Arrays.copyOf(out.getBuffer(), out.getSize());
            } finally { out.close(); }
        } catch (Exception e) { throw new IllegalStateException("Cannot generate Maps native back extension", e); }
    }
}
