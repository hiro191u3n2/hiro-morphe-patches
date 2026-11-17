package app.hiro.newx.patches;

import java.util.*;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.patch.Compatibility;
import app.morphe.patcher.patch.AppTarget;
import app.morphe.patcher.patch.ApkFileType;
import app.morphe.patcher.patch.FormFactor;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Retains X's own saved snoozed-topic list. No new preferences or runtime hooks. */
public final class NewXKeepCategoriesPatch {
    public static final String NAME = "NewX: タイムラインのカテゴリ設定を再起動後も保持";
    public static final String VERSION = "12.43.0-prod.01";
    public static final String OWNER = "Lcom/x/home/foryou/x;";
    public static final String REPOSITORY = "Lcom/x/repositories/home/k;";
    private static final String PREFS = "Lcom/x/http/n;";
    private static final String SAVED = "for_you_snoozed_topic_ids";
    private static final String EXPIRATION = "for_you_snoozed_topic_ids_expiration";

    public static final BytecodePatch PATCH = new BytecodePatch(
        NAME,
        "X 12.43.0: X自身が保存するカテゴリ除外設定の起動時消去と期限切れ消去を停止。適用・手動リセット・地域設定を維持。Piko NewXのトピック選択はPiko自身の保存を使用します。実機未検証。",
        true,
        Collections.singletonList(new Compatibility("com.twitter.android", "NewX", "X 12.43.0 専用", ApkFileType.APKM,
                "#000000", null, Collections.singletonList(new AppTarget(VERSION, Collections.emptyMap(), false, null, null)), FormFactor.MOBILE)),
        Collections.emptySet(), Collections.emptySet(), Collections.emptyList(),
        new Function1<BytecodePatchContext, Unit>() {
            public Unit invoke(BytecodePatchContext context) {
                require("com.twitter.android".equals(context.getPackageMetadata().getPackageName())
                        && VERSION.equals(context.getPackageMetadata().getVersionName()), "Unsupported NewX version");
                Method startup = startup(context.classDefBy(OWNER));
                Method read = read(context.classDefBy(REPOSITORY));
                int startIndex = startupReset(startup);
                int expiryIndex = expirationBranch(read);
                // Validate the manual clear and native save paths before either mutation.
                validateNativePaths(context.classDefBy(OWNER), context.classDefBy(PREFS));
                MutableMethod mutableStartup = mutable(context, startup);
                MutableMethod mutableRead = mutable(context, read);
                // Equal code-unit length keeps all existing labels, try boundaries and branches.
                nop(mutableStartup, startIndex, 3);
                nop(mutableRead, expiryIndex, 2);
                return Unit.INSTANCE;
            }
        }, null, null, null
    );

    private NewXKeepCategoriesPatch() {}
    public static Method startup(ClassDef type) {
        Method found = null;
        for (Method method : type.getMethods()) {
            if ("<init>".equals(method.getName()) && strings(method).contains("co_timeline_reset_period_minutes")) {
                require(found == null, "Ambiguous NewX timeline constructor"); found = method;
            }
        }
        require(found != null && OWNER.equals(found.getDefiningClass()), "Missing NewX timeline constructor");
        return found;
    }
    public static Method read(ClassDef type) {
        Method found = null;
        for (Method method : type.getMethods()) {
            if ("a".equals(method.getName()) && method.getParameterTypes().isEmpty()
                    && "Ljava/util/List;".equals(method.getReturnType())) {
                require(found == null, "Ambiguous NewX saved category reader"); found = method;
            }
        }
        require(found != null && REPOSITORY.equals(found.getDefiningClass()), "Missing NewX saved category reader");
        return found;
    }
    public static int startupReset(Method method) {
        List<Instruction> code = code(method);
        require(strings(method).contains("co_timeline_snooze_mode_enabled"), "Timeline mode guard changed");
        int found = -1;
        for (int i = 3; i + 1 < code.size(); i++) {
            if (!call(code.get(i), PREFS, "a", "V", 0)) continue;
            require(found < 0, "Ambiguous startup category reset");
            Instruction invoke = code.get(i), cast = code.get(i - 1), load = code.get(i - 2), branch = code.get(i - 3);
            require(invoke.getOpcode() == Opcode.INVOKE_VIRTUAL && invoke instanceof FiveRegisterInstruction,
                    "Startup reset invoke changed");
            int receiver = ((FiveRegisterInstruction) invoke).getRegisterC();
            require(((FiveRegisterInstruction) invoke).getRegisterCount() == 1
                    && cast.getOpcode() == Opcode.CHECK_CAST && ((OneRegisterInstruction) cast).getRegisterA() == receiver
                    && reference(cast).equals(PREFS)
                    && load.getOpcode() == Opcode.IGET_OBJECT && load instanceof TwoRegisterInstruction
                    && ((TwoRegisterInstruction) load).getRegisterA() == receiver
                    && reference(load).equals(REPOSITORY + "->a:Lcom/x/common/api/n;")
                    && branch.getOpcode() == Opcode.IF_EQZ
                    && code.get(i + 1).getOpcode() == Opcode.IF_EQZ,
                    "Startup category reset boundaries changed");
            found = i;
        }
        require(found >= 0, "Startup category reset missing or already patched");
        return found;
    }
    public static int expirationBranch(Method method) {
        List<Instruction> code = code(method);
        Set<String> strings = strings(method);
        require(strings.contains(SAVED) && strings.contains(EXPIRATION), "Saved topic keys changed");
        int found = -1, removals = 0;
        for (int i = 0; i < code.size(); i++) {
            if (call(code.get(i), "Landroid/content/SharedPreferences$Editor;", "remove",
                    "Landroid/content/SharedPreferences$Editor;", 1)) removals++;
            if (i < 3 || i + 3 >= code.size() || code.get(i).getOpcode() != Opcode.IF_GEZ) continue;
            if (!call(code.get(i - 3), "Lkotlin/time/Instant;", "a", "I", 1)) continue;
            require(found < 0, "Ambiguous category expiration branch");
            require(code.get(i - 2).getOpcode() == Opcode.MOVE_RESULT
                    && ((OneRegisterInstruction) code.get(i)).getRegisterA() == ((OneRegisterInstruction) code.get(i - 2)).getRegisterA()
                    && SAVED.equals(string(code.get(i - 1)))
                    && call(code.get(i + 1), "Lcom/x/android/preferences/b;", "a", "Ljava/lang/String;", 1)
                    && code.get(i + 2).getOpcode() == Opcode.MOVE_RESULT_OBJECT
                    && code.get(i + 3).getOpcode() == Opcode.GOTO,
                    "Category expiration/read boundaries changed");
            int address = 0;
            for (int n = 0; n < i; n++) address += code.get(n).getCodeUnits();
            int target = address + ((OffsetInstruction) code.get(i)).getCodeOffset();
            int expected = address;
            for (int n = i; n < i + 4; n++) expected += code.get(n).getCodeUnits();
            require(target == expected
                    && call(code.get(i + 4), "Lcom/x/android/preferences/b;", "d", "Lcom/google/firebase/remoteconfig/internal/d;", 0),
                    "Expiration branch no longer points to native clear path");
            found = i;
        }
        require(found >= 0 && removals == 2, "Category expiration path missing or already patched");
        return found;
    }
    public static void validateNativePaths(ClassDef timeline, ClassDef preferences) {
        Method apply = null, clear = null;
        for (Method method : timeline.getMethods()) if ("C".equals(method.getName())
                && method.getParameterTypes().size() == 1 && "V".equals(method.getReturnType())) apply = method;
        for (Method method : preferences.getMethods()) if ("a".equals(method.getName())
                && method.getParameterTypes().isEmpty() && "V".equals(method.getReturnType())) clear = method;
        require(apply != null && clear != null, "Native apply/reset path missing");
        require(strings(apply).contains(SAVED) && strings(apply).contains(EXPIRATION)
                && strings(clear).contains(SAVED) && strings(clear).contains(EXPIRATION), "Native save/reset keys changed");
        int manualClears = 0, saves = 0;
        for (Instruction instruction : code(apply)) {
            if (call(instruction, PREFS, "a", "V", 0)) manualClears++;
            if (call(instruction, "Landroid/content/SharedPreferences$Editor;", "putString", "Landroid/content/SharedPreferences$Editor;", 2)) saves++;
        }
        require(manualClears == 2 && saves == 1, "Native apply/manual reset changed");
    }
    private static MutableMethod mutable(BytecodePatchContext context, Method wanted) {
        for (MutableMethod method : context.mutableClassDefBy(wanted.getDefiningClass()).getMethods())
            if (method.toString().equals(wanted.toString())) return method;
        throw new IllegalStateException("NewX target mutation lookup failed");
    }
    public static void nop(MutableMethod method, int index, int width) {
        require(method.getImplementation().getInstructions().get(index).getCodeUnits() == width, "Unexpected target width");
        method.getImplementation().replaceInstruction(index, new BuilderInstruction10x(Opcode.NOP));
        for (int i = 1; i < width; i++) method.getImplementation().addInstruction(index + i, new BuilderInstruction10x(Opcode.NOP));
    }
    public static List<Instruction> code(Method method) {
        require(method != null && method.getImplementation() != null, "Missing method body");
        List<Instruction> result = new ArrayList<>();
        for (Instruction instruction : method.getImplementation().getInstructions()) result.add(instruction);
        return result;
    }
    private static Set<String> strings(Method method) {
        Set<String> result = new HashSet<>();
        for (Instruction instruction : code(method)) { String value = string(instruction); if (value != null) result.add(value); }
        return result;
    }
    private static String string(Instruction instruction) {
        if (!(instruction instanceof ReferenceInstruction)) return null;
        Reference ref = ((ReferenceInstruction) instruction).getReference();
        return ref instanceof StringReference ? ((StringReference) ref).getString() : null;
    }
    private static String reference(Instruction instruction) {
        return instruction instanceof ReferenceInstruction ? ((ReferenceInstruction) instruction).getReference().toString() : "";
    }
    private static boolean call(Instruction instruction, String owner, String name, String result, int parameterCount) {
        if (!(instruction instanceof ReferenceInstruction)) return false;
        Reference reference = ((ReferenceInstruction) instruction).getReference();
        if (!(reference instanceof MethodReference)) return false;
        MethodReference method = (MethodReference) reference;
        return owner.equals(method.getDefiningClass()) && name.equals(method.getName()) && result.equals(method.getReturnType())
                && method.getParameterTypes().size() == parameterCount;
    }
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("NewX category retention: " + message);
    }
}
