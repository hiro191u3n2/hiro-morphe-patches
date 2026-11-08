package app.hiro.mapsbikes.patches;

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
import com.android.tools.smali.dexlib2.builder.Label;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Selective bicycle suppression at the shared, legacy and native style boundaries. */
public final class MapsHideBicycleMarksPatch {
    private MapsHideBicycleMarksPatch() {}
    public static final String NAME = "地図上の自転車マークを非表示";
    public static final String VERSION = "26.36.04.973607363";
    public static final String CODE = "1068763346";
    public static final String OWNER = "Lbkbv;";
    public static final String TARGET = OWNER + "->b(Lcift;Lbkeo;ILjava/util/List;Ljava/util/List;Lbkdc;Lbjiv;)Lbkba;";
    public static final String RUNTIME = "Lapp/hiro/mapsbikes/BicycleMarkerFilter;";
    public static final String FILTER = RUNTIME + "->shouldHide(Ljava/lang/Object;)Z";
    public static final String LEGACY_FILTER = RUNTIME + "->shouldHideLegacy(Ljava/lang/Object;Ljava/lang/Object;)Z";
    public static final String LEGEND_RUNTIME = "Lapp/hiro/mapsbikes/BicycleLegendFilter;";
    public static final String LEGEND_FILTER = LEGEND_RUNTIME + "->filter([B)[B";
    public static final String EXTENSION = "/extensions/maps_hide_bicycle_marks.mpe";
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
            final List<ClassDef> installed = new ArrayList<ClassDef>();
            c.classDefForEach(new Function1<ClassDef, Unit>() { public Unit invoke(ClassDef type) {
                if (type.getType().startsWith(UpgradeContracts.PREFIX)) installed.add(type);
                return Unit.INSTANCE;
            }});
            UpgradeContracts.validateInstallation(new Lookup() {
                public ClassDef get(String type) { return c.classDefByOrNull(type); }
            }, installed);
            return Unit.INSTANCE;
        }}, null);

    public static final BytecodePatch PATCH = new BytecodePatch(NAME,
        "microG Maps 26.36.04.973607363用。共有・従来・ネイティブ描画の自転車アイコンを確認して、背景ごと非表示にします。旧v1.0.0から更新可能。初回はオンライン地図キャッシュを別領域に再作成します。確認済みの描画コードが一致するAPK専用です。Android画面での動作は未検証です。",
        true, compatibility(), Collections.<Patch<?>>singleton(GUARD), Collections.emptySet(),
        new Supplier<InputStream>() { public InputStream get() {
            InputStream result = MapsHideBicycleMarksPatch.class.getResourceAsStream(EXTENSION);
            require(result != null, "Missing bicycle marker extension");
            return result;
        }}, new Function1<BytecodePatchContext,Unit>() { public Unit invoke(final BytecodePatchContext c) {
            boolean upgrade = UpgradeContracts.hasSharedHook(c.classDefBy(OWNER));
            final Set<String> modified = modifiedOwners();
            applyNative(new Lookup() { public ClassDef get(String type) {
                return modified.contains(type) ? c.mutableClassDefBy(type) : c.classDefByOrNull(type);
            }}, upgrade);
            UpgradeContracts.replaceRuntime(c);
            for (String descriptor : Arrays.asList(FILTER, LEGACY_FILTER, LEGEND_FILTER)) {
                Method runtime = find(c.classDefBy(owner(descriptor)), descriptor);
                require((runtime.getAccessFlags() & 9) == 9, "Bicycle filter is not public static: " + descriptor);
            }
            return Unit.INSTANCE;
        }}, null);

    public static void require(boolean ok, String why) {
        if (!ok) throw new IllegalStateException("Maps bicycle marks: " + why);
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
            require(found == null, "Duplicate method " + d);
            found = m;
        }
        require(found != null, "Missing method " + d);
        return found;
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
        } catch (Exception e) { throw new IllegalStateException("Cannot fingerprint bicycle marker contract", e); }
    }
    public static void validateNative(Lookup lookup) {
        NativeContracts.validate(lookup);
        RendererContracts.validate(lookup);
    }

    public static Set<String> modifiedOwners() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(OWNER,
            owner(RendererContracts.LEGACY), owner(RendererContracts.NETWORK),
            owner(RendererContracts.OFFLINE), owner(RendererContracts.CACHE_CONFIG))));
    }

    /** Validate every path before the first mutation; exact old installs retain their shared hook. */
    public static void applyNative(Lookup lookup, boolean upgrade) {
        if (upgrade) UpgradeContracts.validateInstalledNative(lookup);
        else validateNative(lookup);
        Map<String, MutableClass> classes = new LinkedHashMap<String, MutableClass>();
        for (String owner : modifiedOwners()) {
            ClassDef found = lookup.get(owner);
            require(found instanceof MutableClass, "Native owner is not mutable: " + owner);
            classes.put(owner, (MutableClass) found);
        }
        if (!upgrade) apply(classes.get(OWNER));
        applyLegacy((MutableMethod)find(classes.get(owner(RendererContracts.LEGACY)), RendererContracts.LEGACY));
        filterBeforeCallback((MutableMethod)find(classes.get(owner(RendererContracts.NETWORK)), RendererContracts.NETWORK),
            RendererContracts.NETWORK_CALLBACK, 15);
        filterBeforeCallback((MutableMethod)find(classes.get(owner(RendererContracts.OFFLINE)), RendererContracts.OFFLINE),
            RendererContracts.OFFLINE_CALLBACK, 6);
        MutableMethod cache = (MutableMethod)find(classes.get(owner(RendererContracts.CACHE_CONFIG)), RendererContracts.CACHE_CONFIG);
        cache.getImplementation().replaceInstruction(RendererContracts.CACHE_INDEX,
            new BuilderInstruction21c(Opcode.CONST_STRING, 8,
                new ImmutableStringReference(RendererContracts.CACHE_DIRECTORY)));
    }

    private static void applyLegacy(MutableMethod method) {
        // Static ABI: v14 is the style and v12 the per-feature override list.
        // v0 is dead at entry and is assigned before its first native read on
        // both original branches. Both callers already discard a null icon.
        Label original = method.getImplementation().getInstructions().get(0).getLocation().addNewLabel();
        method.getImplementation().addInstruction(0, new BuilderInstruction35c(Opcode.INVOKE_STATIC,
            2, 14, 12, 0, 0, 0, new ImmutableMethodReference(RUNTIME, "shouldHideLegacy",
                Arrays.asList("Ljava/lang/Object;", "Ljava/lang/Object;"), "Z")));
        method.getImplementation().addInstruction(1, new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
        method.getImplementation().addInstruction(2, new BuilderInstruction21t(Opcode.IF_EQZ, 0, original));
        method.getImplementation().addInstruction(3, new BuilderInstruction11n(Opcode.CONST_4, 0, 0));
        method.getImplementation().addInstruction(4, new BuilderInstruction11x(Opcode.RETURN_OBJECT, 0));
    }

    private static void filterBeforeCallback(MutableMethod method, String callback, int index) {
        // Both exact callbacks deliver their raw resource byte[] in v5. Keep
        // the native handle, request ID, completion call and error path intact.
        Instruction original = method.getImplementation().getInstructions().get(index);
        require(original.getOpcode() == Opcode.INVOKE_STATIC && original instanceof ReferenceInstruction
            && callback.equals(descriptor((MethodReference)((ReferenceInstruction)original).getReference())),
            "Native resource callback moved: " + callback);
        method.getImplementation().addInstruction(index, new BuilderInstruction35c(Opcode.INVOKE_STATIC,
            1, 5, 0, 0, 0, 0, new ImmutableMethodReference(LEGEND_RUNTIME, "filter", Collections.singletonList("[B"), "[B")));
        method.getImplementation().addInstruction(index + 1, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 5));
    }

    public static void apply(MutableClass type) {
        require(type != null && OWNER.equals(type.getType()) && type.getAccessFlags() == 17
            && "Ljava/lang/Object;".equals(type.getSuperclass()) && type.getInterfaces().isEmpty(),
            "Placed-label factory class changed");
        MutableMethod method = (MutableMethod)find(type, TARGET);
        NativeContracts.validateTarget(method);

        // p0=this is v27, p1=PlacedLabel is v28. The original first instruction
        // overwrites v0. Reuse that dead local, leaving the 35-register ABI intact.
        // Label remains attached to the original entry so internal jumps retain
        // the native control flow. The added prefix is exactly 8 code units.
        Label originalEntry = method.getImplementation().getInstructions().get(0).getLocation().addNewLabel();
        method.getImplementation().addInstruction(0, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, 28, 1,
            new ImmutableMethodReference(RUNTIME, "shouldHide", Collections.singletonList("Ljava/lang/Object;"), "Z")));
        method.getImplementation().addInstruction(1, new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
        method.getImplementation().addInstruction(2, new BuilderInstruction21t(Opcode.IF_EQZ, 0, originalEntry));
        method.getImplementation().addInstruction(3, new BuilderInstruction11n(Opcode.CONST_4, 0, 0));
        method.getImplementation().addInstruction(4, new BuilderInstruction11x(Opcode.RETURN_OBJECT, 0));
    }
}
