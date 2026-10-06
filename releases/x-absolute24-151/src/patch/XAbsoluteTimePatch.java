package app.hiro.twitter.patches;

import java.io.InputStream;
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
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.builder.BuilderInstruction;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc;

/** Additive, version-gated patch. No replacement of X's shared datetime utilities. */
public final class XAbsoluteTimePatch {
    private XAbsoluteTimePatch() {}
    public static final String NAME = "ポストの日時を絶対時間・24時間表記にする";
    public static final String VERSION = "12.19.1-release.0";
    public static final String RUNTIME = "Lapp/hiro/twitter/runtime/AbsoluteTime;";
    private static final String UTIL = "Lcom/twitter/util/datetime/d;";
    private static final String RES = "Landroid/content/res/Resources;";
    private static final String STR = "Ljava/lang/String;";
    private static final String CORE = "Lcom/twitter/tweetview/core/";
    private static final String DETAIL = "Lcom/twitter/tweetview/focal/ui/combinedbyline/e;";
    public static final List<String> TARGETS = Collections.unmodifiableList(Arrays.asList(
            CORE + "b;", CORE + "g$a;", CORE + "QuoteView;",
            CORE + "ui/accessibility/u;", DETAIL));

    // Legacy public constructor retained for Manager/Patcher 1.8 compatibility.
    public static final BytecodePatch PATCH = new BytecodePatch(
        NAME,
        "X 12.19.1: タイムライン・引用・詳細の投稿日時を yyyy/MM/dd HH:mm に変更。端末のタイムゾーンを使用し、12時間設定でも24時間表記。DMと動画の時間は維持。実機未検証。",
        true,
        Collections.singleton(new Pair<String, Set<String>>("com.twitter.android", Collections.singleton(VERSION))),
        Collections.emptySet(), Collections.emptySet(),
        new Supplier<InputStream>() {
            @Override public InputStream get() {
                InputStream in = XAbsoluteTimePatch.class.getResourceAsStream("/extensions/x_absolute_time.mpe");
                if (in == null) throw new IllegalStateException("Missing X absolute timestamp extension");
                return in;
            }
        },
        new Function1<BytecodePatchContext, Unit>() {
            @Override public Unit invoke(BytecodePatchContext context) {
                validatePackage(context.getPackageMetadata().getPackageName(), context.getPackageMetadata().getVersionName());
                validateOriginalUtilities(context.classDefBy(UTIL));
                // Validate every call site before changing any app instructions.
                for (String type : TARGETS) validateClass(context.classDefBy(type));
                int count = 0;
                for (String type : TARGETS) count += apply(context.mutableClassDefBy(type));
                if (count != 6) throw new IllegalStateException("Expected six post timestamp hooks; found " + count);
                return Unit.INSTANCE;
            }
        }, null
    );

    public static void validatePackage(String packageName, String versionName) {
        if (!"com.twitter.android".equals(packageName) || !VERSION.equals(versionName))
            throw new IllegalStateException("This build supports com.twitter.android " + VERSION
                    + "; refusing an unverified version even when force is enabled.");
    }
    public static void validateOriginalUtilities(ClassDef utility) {
        for (String name : Arrays.asList("i", "j")) {
            int found = 0;
            for (Method m : utility.getMethods()) {
                if (name.equals(m.getName()) && signature(m, Arrays.asList("J", RES), STR)
                        && (m.getAccessFlags() & 9) == 9 && m.getImplementation() != null) found++;
            }
            if (found != 1) throw new IllegalStateException("Missing original timestamp fallback " + name);
        }
    }
    private static boolean signature(MethodReference m, List<String> params, String result) {
        if (!result.equals(m.getReturnType()) || m.getParameterTypes().size() != params.size()) return false;
        for (int i = 0; i < params.size(); i++)
            if (!params.get(i).contentEquals(m.getParameterTypes().get(i))) return false;
        return true;
    }
    private static boolean targetMethod(MethodReference m) {
        String c = m.getDefiningClass();
        if ((CORE + "b;").equals(c) || (CORE + "g$a;").equals(c))
            return m.getName().equals("a") && signature(m,
                    Arrays.asList("Lcom/twitter/model/timeline/n2;", RES, "J"), STR);
        if ((CORE + "QuoteView;").equals(c))
            return m.getName().equals("getTimestampFromQuotedTweet") && signature(m, Collections.<String>emptyList(), STR);
        if ((CORE + "ui/accessibility/u;").equals(c))
            return m.getName().equals("a") && STR.equals(m.getReturnType())
                    && m.getParameterTypes().size() == 28
                    && m.getParameterTypes().get(0).toString().equals("Landroid/content/Context;")
                    && m.getParameterTypes().get(13).toString().equals("J");
        return DETAIL.equals(c) && m.getName().equals("accept")
                && signature(m, Collections.singletonList("Ljava/lang/Object;"), "V");
    }
    /** Exact call-reference and owner allowlist; layout, DM, durations and other apps are untouched. */
    public static MethodReference replacement(MethodReference owner, Instruction instruction) {
        if (!targetMethod(owner) || !(instruction instanceof ReferenceInstruction)) return null;
        Reference ref = ((ReferenceInstruction) instruction).getReference();
        if (!(ref instanceof MethodReference)) return null;
        MethodReference call = (MethodReference) ref;
        boolean stat = instruction.getOpcode() == Opcode.INVOKE_STATIC || instruction.getOpcode() == Opcode.INVOKE_STATIC_RANGE;
        boolean virt = instruction.getOpcode() == Opcode.INVOKE_VIRTUAL || instruction.getOpcode() == Opcode.INVOKE_VIRTUAL_RANGE;
        if (stat && UTIL.equals(call.getDefiningClass()) && signature(call, Arrays.asList("J", RES), STR)) {
            boolean accessible = (CORE + "ui/accessibility/u;").equals(owner.getDefiningClass());
            if (call.getName().equals(accessible ? "i" : "j"))
                return new ImmutableMethodReference(RUNTIME, accessible ? "accessiblePost" : "post", Arrays.asList("J", RES), STR);
        }
        if (virt && DETAIL.equals(owner.getDefiningClass())) {
            if ("Ljava/text/Format;".equals(call.getDefiningClass()) && "format".equals(call.getName())
                    && signature(call, Collections.singletonList("Ljava/lang/Object;"), STR))
                return new ImmutableMethodReference(RUNTIME, "detail", Arrays.asList("Ljava/text/Format;", "Ljava/lang/Object;"), STR);
            if ("Ljava/text/DateFormat;".equals(call.getDefiningClass()) && "format".equals(call.getName())
                    && signature(call, Collections.singletonList("Ljava/util/Date;"), STR))
                return new ImmutableMethodReference(RUNTIME, "accessibleDetail", Arrays.asList("Ljava/text/DateFormat;", "Ljava/util/Date;"), STR);
        }
        return null;
    }
    public static void validateClass(ClassDef clazz) {
        if (!TARGETS.contains(clazz.getType())) throw new IllegalStateException("Class outside post timestamp allowlist");
        Map<String,Integer> found = new HashMap<>();
        for (Method m : clazz.getMethods()) {
            if (m.getImplementation() == null) continue;
            for (Instruction i : m.getImplementation().getInstructions()) {
                MethodReference ref = replacement(m, i);
                if (ref != null) {
                    // Assert register word counts without shifting any existing register or branch.
                    int words = 0;
                    for (CharSequence p : ref.getParameterTypes()) words += p.toString().equals("J") || p.toString().equals("D") ? 2 : 1;
                    int actual = i instanceof FiveRegisterInstruction ? ((FiveRegisterInstruction)i).getRegisterCount()
                            : i instanceof RegisterRangeInstruction ? ((RegisterRangeInstruction)i).getRegisterCount() : -1;
                    if (actual != words || i.getCodeUnits() != 3) throw new IllegalStateException("Unsafe invoke encoding");
                    found.put(ref.getName(), found.containsKey(ref.getName()) ? found.get(ref.getName()) + 1 : 1);
                }
            }
        }
        Map<String,Integer> expected = new HashMap<>();
        if (clazz.getType().equals(DETAIL)) { expected.put("detail",1); expected.put("accessibleDetail",1); }
        else expected.put(clazz.getType().equals(CORE + "ui/accessibility/u;") ? "accessiblePost" : "post",1);
        if (!expected.equals(found)) throw new IllegalStateException("Unexpected or already-patched timestamp code in " + clazz.getType() + ": " + found);
    }
    public static BuilderInstruction redirect(Instruction old, MethodReference target) {
        if (old instanceof FiveRegisterInstruction) {
            FiveRegisterInstruction r = (FiveRegisterInstruction) old;
            return new BuilderInstruction35c(Opcode.INVOKE_STATIC, r.getRegisterCount(),
                    r.getRegisterC(), r.getRegisterD(), r.getRegisterE(), r.getRegisterF(), r.getRegisterG(), target);
        }
        if (old instanceof RegisterRangeInstruction) {
            RegisterRangeInstruction r = (RegisterRangeInstruction) old;
            return new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, r.getStartRegister(), r.getRegisterCount(), target);
        }
        throw new IllegalStateException("Unsupported timestamp invoke format");
    }
    public static int apply(MutableClass clazz) {
        int count = 0;
        for (MutableMethod method : clazz.getMethods()) {
            if (method.getImplementation() == null) continue;
            List<BuilderInstruction> code = method.getImplementation().getInstructions();
            for (int i = 0; i < code.size(); i++) {
                Instruction old = code.get(i);
                MethodReference target = replacement(method, old);
                if (target != null) {
                    method.getImplementation().replaceInstruction(i, redirect(old, target));
                    count++;
                }
            }
        }
        return count;
    }
}
