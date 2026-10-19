package app.hiro.fossifygallery.patches;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.Label;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Extend the app's verified AndroidX back callback only after native navigation declines it. */
public final class FossifyGalleryExitPatch {
    private FossifyGalleryExitPatch() {}
    public static final String NAME = "戻る終了時に履歴を削除";
    public static final String PACKAGE_NAME = "org.fossify.gallery";
    public static final String VERSION = "1.14.0";
    public static final String VERSION_CODE = "29";
    public static final String RUNTIME = "Lapp/hiro/fossifygallery/runtime/GalleryExit;";
    public static final String ORIGINAL = "hiroGallery$originalBack";
    public static final String CALLBACK = "onBackPressedCompat";
    public static final String BASE = "Lorg/fossify/commons/activities/BaseSimpleActivity;";
    public static final String MAIN = "Lorg/fossify/gallery/activities/MainActivity;";
    public static final String MEDIA = "Lorg/fossify/gallery/activities/MediaActivity;";
    public static final String ACTIVITY = "Landroid/app/Activity;";
    public static final Set<String> OWNERS = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(MAIN, MEDIA, BASE)));
    private static volatile Map<String, Method> references;

    public static final BytecodePatch PATCH = new BytecodePatch(
        NAME,
        "Fossify Gallery 1.14.0 (29): 戻る操作でアプリを終了するときだけ、最近使ったアプリから削除。写真・フォルダー内の戻る操作、検索の閉じる操作、画像選択元のアプリを維持。実機未検証。",
        true,
        Collections.singleton(new Pair<String, Set<String>>(PACKAGE_NAME, Collections.singleton(VERSION))),
        Collections.emptySet(), Collections.emptySet(),
        new Supplier<InputStream>() {
            @Override public InputStream get() {
                InputStream result = FossifyGalleryExitPatch.class.getResourceAsStream("/extensions/fossify_gallery_exit.mpe");
                if (result == null) throw new IllegalStateException("Missing Fossify Gallery exit extension");
                return result;
            }
        },
        new Function1<BytecodePatchContext, Unit>() {
            @Override public Unit invoke(BytecodePatchContext context) {
                validatePackage(context.getPackageMetadata().getPackageName(),
                        context.getPackageMetadata().getVersionName(), context.getPackageMetadata().getVersionCode());
                // Check every callback before any mutation. Unsupported or changed apps fail closed.
                Map<String, Method> originals = new LinkedHashMap<String, Method>();
                for (String owner : OWNERS)
                    originals.put(owner, ImmutableMethod.of(validateClass(context.classDefBy(owner))));
                for (String owner : OWNERS)
                    apply(context.mutableClassDefBy(owner), originals.get(owner));
                return Unit.INSTANCE;
            }
        }, null
    );

    public static void validatePackage(String packageName, String versionName, String versionCode) {
        if (!PACKAGE_NAME.equals(packageName) || !VERSION.equals(versionName) || !VERSION_CODE.equals(versionCode))
            throw new IllegalStateException("Only Fossify Gallery " + VERSION + " (" + VERSION_CODE
                    + ") is verified; an unverified build is refused even if force is enabled.");
    }

    /**
     * Compare executable code using the SAME active writer for both sides. A
     * stored hash of DexPool output is not portable: newer smali writers reorder
     * type-list pools while preserving every decoded instruction and reference.
     * Debug lines can also move to instruction starts in mutable adapters.
     */
    public static String fingerprint(Method method) {
        try {
            MethodImplementation implementation = method.getImplementation();
            if (implementation == null) throw new IllegalStateException("Missing callback implementation");
            Method executable = new ImmutableMethod(method.getDefiningClass(), method.getName(),
                    method.getParameters(), method.getReturnType(), method.getAccessFlags(),
                    method.getAnnotations(), method.getHiddenApiRestrictions(),
                    new ImmutableMethodImplementation(implementation.getRegisterCount(),
                            implementation.getInstructions(), implementation.getTryBlocks(),
                            Collections.emptyList()));
            ImmutableClassDef wrapper = new ImmutableClassDef(method.getDefiningClass(), 1,
                    "Ljava/lang/Object;", Collections.<String>emptyList(), null, Collections.emptySet(),
                    Collections.emptyList(), Collections.singletonList(executable));
            MemoryDataStore data = new MemoryDataStore();
            try {
                DexPool.writeTo(data, new ImmutableDexFile(Opcodes.forApi(29), Collections.singleton(wrapper)));
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                hash.update(data.getBuffer(), 0, data.getSize());
                StringBuilder result = new StringBuilder(64);
                for (byte value : hash.digest()) {
                    result.append(Character.forDigit((value >>> 4) & 15, 16));
                    result.append(Character.forDigit(value & 15, 16));
                }
                return result.toString();
            } finally { data.close(); }
        } catch (Exception failure) {
            throw new IllegalStateException("Could not verify Fossify Gallery original back callback", failure);
        }
    }

    public static boolean isCallback(Method method) {
        return CALLBACK.equals(method.getName()) && method.getParameterTypes().isEmpty()
                && "Z".equals(method.getReturnType());
    }

    /** Reference DEX contains only the three reviewed callbacks from the pinned official APK. */
    public static Map<String, Method> readReference(InputStream stream) {
        if (stream == null) throw new IllegalStateException("Missing bundled Fossify Gallery callback reference");
        try (InputStream source = stream; ByteArrayOutputStream data = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = source.read(buffer)) != -1) {
                data.write(buffer, 0, count);
                if (data.size() > 262144) throw new IllegalStateException("Oversized Fossify Gallery callback reference");
            }
            DexBackedDexFile dex = DexBackedDexFile.fromInputStream(
                    Opcodes.forApi(29), new ByteArrayInputStream(data.toByteArray()));
            Map<String, Method> result = new LinkedHashMap<String, Method>();
            for (ClassDef clazz : dex.getClasses()) {
                if (!OWNERS.contains(clazz.getType()) || result.containsKey(clazz.getType())
                        || clazz.getFields().iterator().hasNext())
                    throw new IllegalStateException("Unexpected Fossify Gallery callback reference inventory");
                Method callback = null;
                for (Method method : clazz.getMethods()) {
                    if (callback != null || !isCallback(method) || method.getAccessFlags() != 1
                            || method.getImplementation() == null)
                        throw new IllegalStateException("Unexpected Fossify Gallery reference method");
                    callback = ImmutableMethod.of(method);
                }
                if (callback == null) throw new IllegalStateException("Empty Fossify Gallery reference class");
                result.put(clazz.getType(), callback);
            }
            if (!result.keySet().equals(OWNERS))
                throw new IllegalStateException("Incomplete Fossify Gallery callback reference");
            return Collections.unmodifiableMap(result);
        } catch (Exception failure) {
            throw new IllegalStateException("Could not load verified Fossify Gallery callback reference", failure);
        }
    }

    private static Map<String, Method> referenceMethods() {
        Map<String, Method> result = references;
        if (result != null) return result;
        synchronized (FossifyGalleryExitPatch.class) {
            if (references == null)
                references = readReference(FossifyGalleryExitPatch.class.getResourceAsStream(
                        "/fossify-gallery/original-back.dex"));
            return references;
        }
    }

    public static boolean matchesReference(Method actual, Method expected) {
        return actual != null && expected != null && isCallback(actual)
                && OWNERS.contains(actual.getDefiningClass())
                && actual.getDefiningClass().equals(expected.getDefiningClass())
                && actual.getAccessFlags() == 1 && actual.getImplementation() != null
                && fingerprint(actual).equals(fingerprint(expected));
    }

    public static Method validateClass(ClassDef clazz) {
        if (clazz == null || !OWNERS.contains(clazz.getType()))
            throw new IllegalStateException("Missing verified Fossify Gallery back owner");
        Method callback = null;
        for (Method method : clazz.getMethods()) {
            if (ORIGINAL.equals(method.getName())) throw new IllegalStateException("Fossify Gallery exit is already patched");
            if (!isCallback(method)) continue;
            if (callback != null || method.getImplementation() == null || method.getAccessFlags() != 1)
                throw new IllegalStateException("Unsupported Fossify Gallery callback declaration");
            callback = method;
        }
        if (!matchesReference(callback, referenceMethods().get(clazz.getType())))
            throw new IllegalStateException("Unverified or modified Fossify Gallery back callback: " + clazz.getType());
        return callback;
    }

    public static int apply(MutableClass clazz, Method original) {
        // Both reference and input use the active writer. Only debug positions
        // are omitted from comparison; the original body itself remains intact.
        if (clazz == null || original == null || !clazz.getType().equals(original.getDefiningClass())
                || !OWNERS.contains(clazz.getType())
                || !matchesReference(original, referenceMethods().get(clazz.getType())))
            throw new IllegalStateException("Verified immutable Fossify callback snapshot required");
        MutableMethod oldMutable = null;
        for (MutableMethod method : clazz.getMethods()) {
            if (ORIGINAL.equals(method.getName())) throw new IllegalStateException("Fossify Gallery exit is already patched");
            if (!isCallback(method)) continue;
            if (oldMutable != null || method.getAccessFlags() != 1 || method.getImplementation() == null)
                throw new IllegalStateException("Unexpected mutable Fossify callback declaration");
            oldMutable = method;
        }
        if (oldMutable == null) throw new IllegalStateException("Mutable Fossify callback missing");
        if (!matchesReference(oldMutable, referenceMethods().get(clazz.getType())))
            throw new IllegalStateException("Fossify callback changed after validation");
        // The optimizer reuses the original receiver register for AppLockManager
        // before false returns. Keep its complete body in a separate stack frame.
        MutableMethod preserved = new MutableMethod(new ImmutableMethod(clazz.getType(), ORIGINAL,
                original.getParameters(), "Z", 0x1002, original.getAnnotations(),
                original.getHiddenApiRestrictions(), original.getImplementation()));
        MutableMethodImplementation wrapper = new MutableMethodImplementation(2);
        wrapper.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_DIRECT, 1, 1, 0, 0, 0, 0,
                new ImmutableMethodReference(clazz.getType(), ORIGINAL, Collections.<String>emptyList(), "Z")));
        wrapper.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
        wrapper.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        wrapper.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, 1, 0, 0, 0, 0,
                new ImmutableMethodReference(RUNTIME, "tryExit", Collections.singletonList(ACTIVITY), "Z")));
        wrapper.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
        wrapper.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
        Label consumed = wrapper.newLabelForIndex(5);
        wrapper.replaceInstruction(2, new BuilderInstruction21t(Opcode.IF_NEZ, 0, consumed));
        MutableMethod replacement = new MutableMethod(new ImmutableMethod(clazz.getType(), CALLBACK,
                original.getParameters(), "Z", original.getAccessFlags(), original.getAnnotations(),
                original.getHiddenApiRestrictions(), wrapper));
        if (!clazz.getMethods().remove(oldMutable)) throw new IllegalStateException("Original callback disappeared");
        clazz.getMethods().add(preserved);
        clazz.getMethods().add(replacement);
        return 1;
    }
}
