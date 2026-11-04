package app.hiro.maps.patches;

import java.security.MessageDigest;
import java.util.*;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Suppress only the optional home assistive shortcuts row before its attachment. */
public final class MapsCategoryRowPatch {
    private MapsCategoryRowPatch() {}
    public static final String NAME = "検索欄の下のカテゴリ行を非表示";
    public static final String PACKAGE = "org.ungoogled.android.apps.maps.microg";
    public static final String VERSION = "26.36.04.973607363";
    public static final String CODE = "1068763346";
    public static final String OWNER = "Lafhc;";
    public static final String VIEW = "Landroid/view/View;";
    // Both tested SDKs produce these same hashes. Morphe materialization normalizes
    // debug positions, so the immutable input and the mutable copy have separate hashes.
    private static final Set<String> FINGERPRINTS = new HashSet<String>(Arrays.asList(
        "0824cb37ecb654ea2a597919b412ddd1487c2db75b848d1f10e1397770451b88",
        "6be8c7ec4eebf312bb640e5aced4d154a28710222bdbd112449ab4aed4739ac4"));

    public static final BytecodePatch PATCH = new BytecodePatch(
        NAME,
        "添付版 microG Maps 26.36.04.973607363 専用。検索欄の下に並ぶ「自宅の住所を設定・レストラン・コーヒー・コンビニ」などのカテゴリ行を表示前に取り除きます。検索欄・地図・ナビ・検索結果の絞り込みは変更しません。実機表示は未検証。",
        true,
        Collections.singleton(new Pair<String,Set<String>>(PACKAGE, Collections.singleton(VERSION))),
        Collections.emptySet(), Collections.emptySet(), null,
        new Function1<BytecodePatchContext,Unit>() { public Unit invoke(BytecodePatchContext c) {
            validatePackage(c.getPackageMetadata().getPackageName(), c.getPackageMetadata().getVersionName(), c.getPackageMetadata().getVersionCode());
            Method original = ImmutableMethod.of(validateClass(c.classDefBy(OWNER)));
            apply(c.mutableClassDefBy(OWNER), original);
            return Unit.INSTANCE;
        }}, null);

    public static void validatePackage(String p, String v, String code) {
        if (!PACKAGE.equals(p) || !VERSION.equals(v) || !CODE.equals(code))
            throw new IllegalStateException("Only the verified microG Maps 26.36.04.973607363 (1068763346) is supported");
    }
    public static boolean isTarget(Method m) {
        return OWNER.equals(m.getDefiningClass()) && "a".equals(m.getName())
            && m.getParameterTypes().isEmpty() && VIEW.equals(m.getReturnType());
    }
    public static String fingerprint(Method m) {
        try {
            ImmutableClassDef wrapper = new ImmutableClassDef(m.getDefiningClass(), 1, "Ljava/lang/Object;",
                Collections.<String>emptyList(), null, Collections.emptySet(), Collections.emptyList(),
                Collections.singletonList(ImmutableMethod.of(m)));
            MemoryDataStore store = new MemoryDataStore();
            try {
                DexPool.writeTo(store, new ImmutableDexFile(Opcodes.forApi(29), Collections.singleton(wrapper)));
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(store.getBuffer(), store.getSize()));
                StringBuilder s = new StringBuilder();
                for (byte b : hash) s.append(String.format(Locale.ROOT, "%02x", b & 255));
                return s.toString();
            } finally { store.close(); }
        } catch (Exception e) { throw new IllegalStateException("Cannot verify Maps row getter", e); }
    }
    public static Method validateClass(ClassDef c) {
        if (c == null || !OWNER.equals(c.getType())) throw new IllegalStateException("Maps row owner missing");
        Method found = null;
        for (Method m : c.getMethods()) if (isTarget(m)) {
            if (found != null || m.getAccessFlags() != 17 || m.getImplementation() == null
                || m.getImplementation().getRegisterCount() != 2)
                throw new IllegalStateException("Maps row getter declaration changed");
            found = m;
        }
        if (found == null || !FINGERPRINTS.contains(fingerprint(found)))
            throw new IllegalStateException("Unverified or already modified Maps row getter");
        return found;
    }
    private static boolean resultCall(Instruction instruction) {
        if (instruction.getOpcode() != Opcode.INVOKE_VIRTUAL || !(instruction instanceof ReferenceInstruction)
            || !(instruction instanceof FiveRegisterInstruction)) return false;
        Reference ref = ((ReferenceInstruction)instruction).getReference();
        if (!(ref instanceof MethodReference)) return false;
        MethodReference m = (MethodReference)ref;
        FiveRegisterInstruction regs = (FiveRegisterInstruction)instruction;
        return "Lbzok;".equals(m.getDefiningClass()) && "a".equals(m.getName())
            && VIEW.equals(m.getReturnType()) && m.getParameterTypes().isEmpty()
            && regs.getRegisterCount() == 1 && regs.getRegisterC() == 1;
    }
    public static int apply(MutableClass c, Method original) {
        if (c == null || !OWNER.equals(c.getType()) || !isTarget(original)
            || !FINGERPRINTS.contains(fingerprint(original)))
            throw new IllegalStateException("Verified Maps original required");
        MutableMethod target = null;
        for (MutableMethod m : c.getMethods()) if (isTarget(m)) target = m;
        if (target == null || !FINGERPRINTS.contains(fingerprint(target)))
            throw new IllegalStateException("Mutable Maps getter is not the verified original");
        MutableMethodImplementation impl = target.getImplementation();
        List<? extends Instruction> list = impl.getInstructions();
        int end = list.size() - 1;
        if (end < 2 || !resultCall(list.get(end - 2))
            || list.get(end - 1).getOpcode() != Opcode.MOVE_RESULT_OBJECT
            || ((OneRegisterInstruction)list.get(end - 1)).getRegisterA() != 1
            || list.get(end).getOpcode() != Opcode.RETURN_OBJECT
            || ((OneRegisterInstruction)list.get(end)).getRegisterA() != 1)
            throw new IllegalStateException("Maps final result anchor changed");
        // Keep the native getter, initialization, and binding intact. Its optional result is
        // null before any caller can attach the row. No view polling or global layout edit.
        impl.addInstruction(end, new BuilderInstruction11n(Opcode.CONST_4, 1, 0));
        return 1;
    }
}
