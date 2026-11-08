package app.hiro.mapsstrict.patches;

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
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Search filtering at the request-owned response boundary, before map/list model creation. */
public final class MapsStrictSearchPatch {
    private MapsStrictSearchPatch() {}
    public static final String NAME = "店名検索を厳密にする";
    public static final String VERSION = "26.36.04.973607363";
    public static final String CODE = "1068763346";
    public static final String OWNER = "Lavds;";
    public static final String MODEL = "Lavdv;";
    public static final String MARKER = "hiroStrictSearchResponse";
    public static final int MARKER_FLAGS = 4226; // private transient synthetic; excluded from serialization.
    public static final String TARGET = OWNER + "->f(Lcqgm;Landroid/app/Application;Lawcl;)V";
    public static final String RUNTIME = "Lapp/hiro/mapsstrict/SearchFilter;";
    public static final String FILTER = RUNTIME + "->filterResponse(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;";
    public static final String RECONCILE = RUNTIME + "->reconcileModel(Ljava/lang/Object;Ljava/lang/Object;)V";
    public static final String EXTENSION = "/extensions/maps_strict_search.mpe";
    public static final Set<String> PACKAGES = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(
        "org.ungoogled.android.apps.maps.microg", "com.google.android.apps.maps")));

    public interface Lookup { ClassDef get(String type); }
    public static Set<Pair<String,Set<String>>> compatibility() {
        Set<Pair<String,Set<String>>> result = new LinkedHashSet<Pair<String,Set<String>>>();
        for (String pkg : PACKAGES) result.add(new Pair<String,Set<String>>(pkg, Collections.singleton(VERSION)));
        return Collections.unmodifiableSet(result);
    }
    private static final BytecodePatch GUARD = new BytecodePatch(null, null, false, compatibility(),
        Collections.<Patch<?>>emptySet(), Collections.emptySet(), null,
        new Function1<BytecodePatchContext,Unit>() { public Unit invoke(final BytecodePatchContext c) {
            validatePackage(c.getPackageMetadata().getPackageName(), c.getPackageMetadata().getVersionName(),
                c.getPackageMetadata().getVersionCode());
            final Lookup lookup=new Lookup() { public ClassDef get(String type) { return c.classDefByOrNull(type); }};
            final List<ClassDef> installed=new ArrayList<ClassDef>();
            c.classDefForEach(new Function1<ClassDef,Unit>() { public Unit invoke(ClassDef type) {
                if(type.getType().startsWith(UpgradeContracts.PREFIX))installed.add(type);
                return Unit.INSTANCE;
            }});
            if(installed.isEmpty())validateNative(lookup);
            else {
                UpgradeContracts.validateLegacyRuntime(lookup,installed);
                UpgradeContracts.validateInstalledNative(lookup);
            }
            return Unit.INSTANCE;
        }}, null);
    public static final BytecodePatch PATCH = new BytecodePatch(NAME,
        "microG Maps 26.36.04.973607363用。店名検索v1.0.1: ランダ／RANDAなどの店名に合う一覧と地図上のピンを残します。ピン専用データを店舗IDで絞り、v1.0.0でピンが消える問題に対応。表記違い・支店名・カテゴリ検索の扱いは維持。確認済みv1.0.0適用済みAPKから更新可能。起動時現在地・カテゴリ行非表示・戻る終了も保持します。対象コードが一致するAPK専用。Android画面での動作は未検証です。",
        true, compatibility(), Collections.<Patch<?>>singleton(GUARD), Collections.emptySet(),
        new Supplier<InputStream>() { public InputStream get() {
            InputStream result = MapsStrictSearchPatch.class.getResourceAsStream(EXTENSION);
            require(result != null, "Missing strict search extension"); return result;
        }}, new Function1<BytecodePatchContext,Unit>() { public Unit invoke(final BytecodePatchContext c) {
            boolean upgrade=false;
            for(Field field:c.classDefBy(MODEL).getFields())if(field.getName().equals(MARKER))upgrade=true;
            if(upgrade)UpgradeContracts.validateInstalledNative(new Lookup(){public ClassDef get(String type){return c.classDefByOrNull(type);}});
            else apply(c.mutableClassDefBy(OWNER), c.mutableClassDefBy(MODEL));
            UpgradeContracts.replaceRuntime(c);
            Method runtime = find(c.classDefBy(RUNTIME), FILTER);
            require((runtime.getAccessFlags() & 9) == 9, "Runtime filter is not public static");
            require((find(c.classDefBy(RUNTIME), RECONCILE).getAccessFlags() & 9) == 9,
                "Runtime model reconciliation is not public static");
            return Unit.INSTANCE;
        }}, null);

    public static void require(boolean ok, String why) {
        if (!ok) throw new IllegalStateException("Maps strict search: " + why);
    }
    public static void validatePackage(String p, String v, String code) {
        require(PACKAGES.contains(p) && VERSION.equals(v) && CODE.equals(code),
            "Requires verified Maps 26.36.04.973607363 (1068763346)");
    }
    public static String descriptor(MethodReference m) {
        StringBuilder b = new StringBuilder(m.getDefiningClass()).append("->").append(m.getName()).append('(');
        for (CharSequence p : m.getParameterTypes()) b.append(p);
        return b.append(')').append(m.getReturnType()).toString();
    }
    public static String owner(String descriptor) { return descriptor.substring(0, descriptor.indexOf("->")); }
    public static Method find(ClassDef c, String d) {
        require(c != null, "Missing class for " + d);
        Method found = null;
        for (Method m : c.getMethods()) if (d.equals(descriptor(m))) {
            require(found == null, "Duplicate method " + d); found = m;
        }
        require(found != null, "Missing method " + d); return found;
    }
    public static String fingerprint(Method m) {
        try {
            MethodImplementation b = m.getImplementation();
            require(b != null, "Missing native body " + descriptor(m));
            ImmutableMethodImplementation normalized = new ImmutableMethodImplementation(b.getRegisterCount(),
                b.getInstructions(), b.getTryBlocks(), Collections.emptyList());
            ImmutableMethod method = new ImmutableMethod(m.getDefiningClass(), m.getName(), m.getParameters(), m.getReturnType(),
                m.getAccessFlags(), m.getAnnotations(), m.getHiddenApiRestrictions(), normalized);
            ImmutableClassDef wrapper = new ImmutableClassDef(m.getDefiningClass(), 1, "Ljava/lang/Object;",
                Collections.emptyList(), null, Collections.emptySet(), Collections.emptyList(), Collections.singleton(method));
            MemoryDataStore store = new MemoryDataStore();
            try {
                DexPool.writeTo(store, new ImmutableDexFile(Opcodes.forApi(29), Collections.singleton(wrapper)));
                byte[] digest = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(store.getBuffer(), store.getSize()));
                StringBuilder result = new StringBuilder();
                for (byte value : digest) result.append(String.format(Locale.ROOT, "%02x", value & 255));
                return result.toString();
            } finally { store.close(); }
        } catch (Exception e) { throw new IllegalStateException("Cannot fingerprint native search contract", e); }
    }
    public static void validateNative(Lookup lookup) { NativeContracts.validate(lookup); }
    public static int insertionIndex(Method method) {
        require(method.getImplementation() != null && method.getImplementation().getRegisterCount() == 13,
            "Search response register layout changed");
        int index = 0, found = -1;
        for (Instruction ins : method.getImplementation().getInstructions()) {
            if (ins.getOpcode() == Opcode.CHECK_CAST && ((OneRegisterInstruction)ins).getRegisterA() == 0
                && ((ReferenceInstruction)ins).getReference().toString().equals("Lcqgi;")) {
                require(found == -1, "Ambiguous search parameters"); found = index + 1;
            }
            index++;
        }
        require(found >= 0, "Missing search parameter decode"); return found;
    }
    public static void apply(MutableClass type, MutableClass model) {
        MutableMethod method = (MutableMethod)find(type, TARGET);
        NativeContracts.validateTarget(method);
        NativeContracts.validateModel(model);
        applyRequest(type);
        applyModel(model);
    }
    public static void applyRequest(MutableClass type) {
        MutableMethod method = (MutableMethod)find(type, TARGET);
        NativeContracts.validateTarget(method);
        int at = insertionIndex(method);
        // v0 retains the native request parameters; v1/v2 are dead until their
        // next original assignments; v10 is exactly this request's response.
        method.getImplementation().addInstruction(at++, new BuilderInstruction22c(Opcode.IGET_OBJECT, 1, 0,
            new ImmutableFieldReference("Lcqgi;", "e", "Ljava/lang/String;")));
        method.getImplementation().addInstruction(at++, new BuilderInstruction22c(Opcode.IGET_OBJECT, 2, 9,
            new ImmutableFieldReference(OWNER, "g", MODEL)));
        method.getImplementation().addInstruction(at++, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 3, 10, 1, 2, 0, 0,
            new ImmutableMethodReference(RUNTIME, "filterResponse", Arrays.asList("Ljava/lang/Object;", "Ljava/lang/String;", "Ljava/lang/Object;"), "Ljava/lang/Object;")));
        method.getImplementation().addInstruction(at++, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 10));
        method.getImplementation().addInstruction(at, new BuilderInstruction21c(Opcode.CHECK_CAST, 10,
            new ImmutableTypeReference("Lcqgm;")));
    }
    public static int mergeInsertionIndex(Method method) {
        int index = 0, found = -1;
        for (Instruction ins : method.getImplementation().getInstructions()) {
            if (ins.getOpcode() == Opcode.IGET_OBJECT && ((TwoRegisterInstruction)ins).getRegisterA() == 10
                && ((TwoRegisterInstruction)ins).getRegisterB() == 10
                && ((ReferenceInstruction)ins).getReference().toString().equals(MODEL + "->E:Lawfr;")) {
                require(found == -1, "Ambiguous merge completion boundary"); found = index;
            }
            index++;
        }
        require(found >= 0, "Missing accepted merge completion boundary"); return found;
    }
    public static void applyModel(MutableClass model) {
        NativeContracts.validateModel(model);
        MutableMethod merge = (MutableMethod)find(model, NativeContracts.MERGE);
        int at = mergeInsertionIndex(merge);
        model.getFields().add(new MutableField(new ImmutableField(MODEL, MARKER, "Ljava/lang/Object;",
            MARKER_FLAGS, null, Collections.emptySet(), Collections.emptySet())));
        // Only accepted merges reach this point. Both native locks are held,
        // and v10 still refers to the incoming result rather than its E field.
        merge.getImplementation().addInstruction(at, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2, 9, 10, 0, 0, 0,
            new ImmutableMethodReference(RUNTIME, "reconcileModel", Arrays.asList("Ljava/lang/Object;", "Ljava/lang/Object;"), "V")));
    }
}
