package app.hiro.quicksearch.patches;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
import com.android.tools.smali.dexlib2.builder.BuilderInstruction;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.Reference;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Two exact, version-gated call-site replacements in the reviewed search flow. */
public final class QuickSearchRecentsPatch {
    private QuickSearchRecentsPatch() {}

    public static final String NAME = "検索後の終了時に履歴へ残さない";
    public static final String PACKAGE_NAME = "jp.ddo.sugihiro.quicksearch";
    public static final String VERSION = "0.4.5";
    public static final long VERSION_CODE = 36L;
    public static final String MAIN = "Ljp/ddo/sugihiro/quicksearch/activity/MainActivity;";
    public static final String RUNTIME = "Lapp/hiro/quicksearch/runtime/SearchTask;";
    private static final String COMMON = "Ljp/ddo/sugihiro/quicksearch/activity/CommonActivity;";
    private static final String ACTIVITY = "Landroid/app/Activity;";
    private static final String INTENT = "Landroid/content/Intent;";
    private static final String STRING = "Ljava/lang/String;";

    // SHA-256 of each independently serialized original method. DEX indices are
    // canonicalized; method code, registers, branches, handlers and metadata are retained.
    public static final String FINISH_FINGERPRINT = "fd876b94a1439b10ab4406bcaef35546e332790eaef79589211a3a2da74b743e";
    public static final String SEARCH_FINGERPRINT = "82eeeb778986963fe1e7bafa3af542e05cec78ada71ac8e7ab9d198809f2793e";

    // Public field and legacy constructor keep this additive patch compatible with
    // the existing Manager/Patcher bundle discovery mechanism.
    public static final BytecodePatch PATCH = new BytecodePatch(
        NAME,
        "簡単検索くん 0.4.5 (36): 「検索実行後の動作 → 簡単検索くんを終了する」がONのとき、終了した検索アプリを最近使ったアプリから削除。検索先を別タスクで開き、外部ブラウザーを維持。実機未検証。",
        true,
        Collections.singleton(new Pair<String, Set<String>>(PACKAGE_NAME, Collections.singleton(VERSION))),
        Collections.emptySet(), Collections.emptySet(),
        new Supplier<InputStream>() {
            @Override public InputStream get() {
                InputStream stream = QuickSearchRecentsPatch.class.getResourceAsStream("/extensions/quicksearch_recents.mpe");
                if (stream == null) throw new IllegalStateException("Missing quicksearch recents extension");
                return stream;
            }
        },
        new Function1<BytecodePatchContext, Unit>() {
            @Override public Unit invoke(BytecodePatchContext context) {
                validatePackage(context.getPackageMetadata().getPackageName(),
                        context.getPackageMetadata().getVersionName(),
                        context.getPackageMetadata().getVersionCode());
                validateClass(context.classDefBy(MAIN));
                apply(context.mutableClassDefBy(MAIN));
                return Unit.INSTANCE;
            }
        }, null
    );

    public static void validatePackage(String packageName, String versionName, String versionCode) {
        if (!PACKAGE_NAME.equals(packageName) || !VERSION.equals(versionName)
                || !Long.toString(VERSION_CODE).equals(versionCode)) {
            throw new IllegalStateException("Only " + PACKAGE_NAME + " " + VERSION + " ("
                    + VERSION_CODE + ") is supported; an unverified build is refused even with force enabled.");
        }
    }

    public static void validatePackage(String packageName, String versionName, long versionCode) {
        validatePackage(packageName, versionName, Long.toString(versionCode));
    }

    private static boolean signature(MethodReference method, List<String> parameters, String result) {
        if (!result.equals(method.getReturnType()) || method.getParameterTypes().size() != parameters.size()) return false;
        for (int i = 0; i < parameters.size(); i++) {
            if (!parameters.get(i).contentEquals(method.getParameterTypes().get(i))) return false;
        }
        return true;
    }

    private static boolean isFinishOwner(MethodReference method) {
        return MAIN.equals(method.getDefiningClass()) && "actionAfterSearched".equals(method.getName())
                && signature(method, Collections.<String>emptyList(), "V");
    }

    private static boolean isSearchOwner(MethodReference method) {
        return MAIN.equals(method.getDefiningClass()) && "search".equals(method.getName())
                && signature(method, Arrays.asList(STRING, STRING), "V");
    }

    /** Matches only the two reviewed virtual calls; no blanket finish/start interception. */
    public static MethodReference replacement(MethodReference owner, Instruction instruction) {
        if (!(instruction instanceof ReferenceInstruction)
                || (instruction.getOpcode() != Opcode.INVOKE_VIRTUAL
                    && instruction.getOpcode() != Opcode.INVOKE_VIRTUAL_RANGE)) return null;
        Reference reference = ((ReferenceInstruction) instruction).getReference();
        if (!(reference instanceof MethodReference)) return null;
        MethodReference call = (MethodReference) reference;
        if (!MAIN.equals(call.getDefiningClass())) return null;
        if (isFinishOwner(owner) && "finish".equals(call.getName())
                && signature(call, Collections.<String>emptyList(), "V")) {
            return new ImmutableMethodReference(RUNTIME, "finish", Collections.singletonList(ACTIVITY), "V");
        }
        if (isSearchOwner(owner) && "startActivity".equals(call.getName())
                && signature(call, Collections.singletonList(INTENT), "V")) {
            return new ImmutableMethodReference(RUNTIME, "startExternal", Arrays.asList(ACTIVITY, INTENT), "V");
        }
        return null;
    }

    /** Canonical per-method DEX serialization is independent of the APK's ID ordering. */
    public static String fingerprint(Method method) {
        try {
            ImmutableClassDef wrapper = new ImmutableClassDef(method.getDefiningClass(), 1,
                    "Ljava/lang/Object;", Collections.<String>emptyList(), null,
                    Collections.emptySet(), Collections.emptyList(),
                    Collections.singletonList(ImmutableMethod.of(method)));
            MemoryDataStore data = new MemoryDataStore();
            try {
                DexPool.writeTo(data, new ImmutableDexFile(Opcodes.forApi(29), Collections.singleton(wrapper)));
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(data.getBuffer(), 0, data.getSize());
                StringBuilder result = new StringBuilder(64);
                for (byte value : digest.digest()) {
                    result.append(Character.forDigit((value >>> 4) & 15, 16));
                    result.append(Character.forDigit(value & 15, 16));
                }
                return result.toString();
            } finally {
                data.close();
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Could not validate original quicksearch method " + method.getName(), failure);
        }
    }

    /** Validate the complete plan before allowing any mutation, including direct apply callers. */
    public static void validateClass(ClassDef clazz) {
        if (clazz == null || !MAIN.equals(clazz.getType()) || !COMMON.equals(clazz.getSuperclass())) {
            throw new IllegalStateException("Missing original quicksearch MainActivity");
        }
        int finishMethods = 0;
        int searchMethods = 0;
        int finishCalls = 0;
        int searchCalls = 0;
        for (Method method : clazz.getMethods()) {
            boolean finishOwner = isFinishOwner(method);
            boolean searchOwner = isSearchOwner(method);
            if (method.getImplementation() != null) {
                for (Instruction instruction : method.getImplementation().getInstructions()) {
                    if (instruction instanceof ReferenceInstruction) {
                        Reference reference = ((ReferenceInstruction) instruction).getReference();
                        if (reference instanceof MethodReference
                                && RUNTIME.equals(((MethodReference) reference).getDefiningClass())) {
                            throw new IllegalStateException("Quicksearch recents is already patched");
                        }
                    }
                    MethodReference target = replacement(method, instruction);
                    if (target == null) continue;
                    int words = instruction instanceof FiveRegisterInstruction
                            ? ((FiveRegisterInstruction) instruction).getRegisterCount()
                            : instruction instanceof RegisterRangeInstruction
                                ? ((RegisterRangeInstruction) instruction).getRegisterCount() : -1;
                    if (words != (finishOwner ? 1 : 2) || instruction.getCodeUnits() != 3) {
                        throw new IllegalStateException("Unsafe quicksearch invoke encoding");
                    }
                    if (finishOwner) finishCalls++;
                    else searchCalls++;
                }
            }
            if (!finishOwner && !searchOwner) continue;
            if (method.getAccessFlags() != 2 || method.getImplementation() == null) {
                throw new IllegalStateException("Unexpected original quicksearch method declaration");
            }
            String expected = finishOwner ? FINISH_FINGERPRINT : SEARCH_FINGERPRINT;
            if (!expected.equals(fingerprint(method))) {
                throw new IllegalStateException("Unverified or modified quicksearch code: " + method.getName());
            }
            if (finishOwner) finishMethods++;
            else searchMethods++;
        }
        if (finishMethods != 1 || searchMethods != 1 || finishCalls != 1 || searchCalls != 1) {
            throw new IllegalStateException("Expected exactly one search-exit hook and one external-search hook");
        }
    }

    /** Replace only the invoke opcode and reference, retaining register words and code size. */
    public static BuilderInstruction redirect(Instruction old, MethodReference target) {
        if (old instanceof FiveRegisterInstruction) {
            FiveRegisterInstruction registers = (FiveRegisterInstruction) old;
            return new BuilderInstruction35c(Opcode.INVOKE_STATIC, registers.getRegisterCount(),
                    registers.getRegisterC(), registers.getRegisterD(), registers.getRegisterE(),
                    registers.getRegisterF(), registers.getRegisterG(), target);
        }
        if (old instanceof RegisterRangeInstruction) {
            RegisterRangeInstruction registers = (RegisterRangeInstruction) old;
            return new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,
                    registers.getStartRegister(), registers.getRegisterCount(), target);
        }
        throw new IllegalStateException("Unsupported quicksearch invoke format");
    }

    public static int apply(MutableClass clazz) {
        validateClass(clazz);
        int count = 0;
        for (MutableMethod method : clazz.getMethods()) {
            if (method.getImplementation() == null) continue;
            List<BuilderInstruction> instructions = method.getImplementation().getInstructions();
            for (int i = 0; i < instructions.size(); i++) {
                Instruction old = instructions.get(i);
                MethodReference target = replacement(method, old);
                if (target == null) continue;
                method.getImplementation().replaceInstruction(i, redirect(old, target));
                count++;
            }
        }
        if (count != 2) throw new IllegalStateException("Quicksearch hook count changed after validation");
        return count;
    }
}
