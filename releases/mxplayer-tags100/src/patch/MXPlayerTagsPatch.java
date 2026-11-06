package app.hiro.mxplayer.patches;

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
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Omit the optional shortcut-row model before the media-list adapter sees it. */
public final class MXPlayerTagsPatch {
    private MXPlayerTagsPatch() {}
    public static final String NAME = "フォルダ画面のタグ一覧を非表示";
    public static final String PACKAGE = "com.mxtech.videoplayer.pro";
    public static final String VERSION = "1.93.4";
    public static final String CODE = "2001002584";
    public static final String OWNER = "Lpf0;";
    public static final String SUPER = "Lcom/mxtech/videoplayer/list/MediaListFragment;";
    // Ignore debug positions only, so FULL and the Manager's stripping mode agree.
    // The full executable body, declaration, registers and handlers remain pinned.
    // The old and current dexlib serializers encode this same body differently.
    private static final Set<String> FINGERPRINTS = new HashSet<String>(Arrays.asList(
        "f83c9a4cb57d25e1b69e10aa064d4ab975f9f2ab7743e1de7a50d1ed35d7a0e0",
        "47ead12007cda5a39335c6e09d6c27fe678e99420956f3647a46564cae3c4310"));

    public static final BytecodePatch PATCH = new BytecodePatch(
        NAME,
        "MX Player Pro 1.93.4 専用。フォルダ画面上部の「Music／Privacy／MXShare／Video Playlist」などが並ぶタグ行を、余白ごと非表示にします。実機表示は未検証。",
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
            throw new IllegalStateException("Only the verified MX Player Pro 1.93.4 (2001002584) is supported");
    }

    public static boolean isTarget(Method m) {
        return m != null && OWNER.equals(m.getDefiningClass()) && "W1".equals(m.getName())
            && m.getParameterTypes().equals(Collections.singletonList("Ljava/util/List;"))
            && "V".equals(m.getReturnType());
    }

    public static String fingerprint(Method m) {
        try {
            MethodImplementation body = m.getImplementation();
            if (body == null) throw new IllegalStateException("Tag-row insertion body missing");
            ImmutableMethodImplementation code = new ImmutableMethodImplementation(body.getRegisterCount(),
                body.getInstructions(), body.getTryBlocks(), Collections.emptyList());
            ImmutableMethod normalized = new ImmutableMethod(m.getDefiningClass(), m.getName(), m.getParameters(),
                m.getReturnType(), m.getAccessFlags(), m.getAnnotations(), m.getHiddenApiRestrictions(), code);
            ImmutableClassDef wrapper = new ImmutableClassDef(m.getDefiningClass(), 1, "Ljava/lang/Object;",
                Collections.<String>emptyList(), null, Collections.emptySet(), Collections.emptyList(),
                Collections.singletonList(normalized));
            MemoryDataStore store = new MemoryDataStore();
            try {
                DexPool.writeTo(store, new ImmutableDexFile(Opcodes.forApi(29), Collections.singleton(wrapper)));
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(store.getBuffer(), store.getSize()));
                StringBuilder s = new StringBuilder();
                for (byte b : hash) s.append(String.format(Locale.ROOT, "%02x", b & 255));
                return s.toString();
            } finally { store.close(); }
        } catch (Exception e) { throw new IllegalStateException("Cannot verify MX Player tag-row insertion", e); }
    }

    public static Method validateClass(ClassDef c) {
        if (c == null || !OWNER.equals(c.getType()) || !SUPER.equals(c.getSuperclass()) || c.getAccessFlags() != 17)
            throw new IllegalStateException("MX Player Pro media-list owner changed");
        Method found = null;
        for (Method m : c.getMethods()) if (isTarget(m)) {
            if (found != null || m.getAccessFlags() != 17 || m.getImplementation() == null
                || m.getImplementation().getRegisterCount() != 8)
                throw new IllegalStateException("MX Player tag-row insertion declaration changed");
            found = m;
        }
        if (found == null || !FINGERPRINTS.contains(fingerprint(found)))
            throw new IllegalStateException("Unverified or already modified MX Player tag-row insertion");
        return found;
    }

    public static int apply(MutableClass c, Method original) {
        if (!isTarget(original) || !FINGERPRINTS.contains(fingerprint(original)))
            throw new IllegalStateException("Verified MX Player original required");
        validateClass(c);
        MutableMethod target = null;
        for (MutableMethod m : c.getMethods()) if (isTarget(m)) target = m;
        if (target == null) throw new IllegalStateException("Mutable MX Player target missing");
        // W1 is the optional tag-row insertion hook. Its superclass is already
        // a no-op, and this override exclusively removes/reinserts a type-20
        // TitleEntry. Returning before model creation leaves no blank list item.
        // Refresh, folder navigation and list rebuilding use this same hook.
        target.getImplementation().addInstruction(0, new BuilderInstruction10x(Opcode.RETURN_VOID));
        return 1;
    }
}
