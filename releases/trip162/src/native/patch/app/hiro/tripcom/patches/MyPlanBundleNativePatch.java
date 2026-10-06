package app.hiro.tripcom.patches;

import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchBuilder;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.patch.PatchKt;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t;
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kotlin.Unit;

/** Internal dependency: MyPlan uses the reviewed APK bundle after online/cache selection. */
public final class MyPlanBundleNativePatch {
    private static final String URL = "Lctrip/android/reactnative/CRNURL;";
    private static final String INSTANCE = "Lcom/facebook/react/runtime/ReactInstance;";
    private static final String STRING = "Ljava/lang/String;";
    private static final String GUARD = "Lapp/hiro/tripcom/extension/MyPlanBundleGuard;";
    private static final String EXTENSION = "extensions/myplan_bundle_guard.mpe";

    public static final BytecodePatch PATCH = PatchKt.bytecodePatch(null, null, true,
            (BytecodePatchBuilder builder) -> {
                builder.setExtensionInputStream(MyPlanBundleNativePatch::openExtension);
                builder.execute((BytecodePatchContext context) -> {
                    apply(context);
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });

    private MyPlanBundleNativePatch() { }

    private static InputStream openExtension() {
        InputStream input = MyPlanBundleNativePatch.class.getClassLoader().getResourceAsStream(EXTENSION);
        if (input == null) throw new IllegalStateException("MyPlan bundle guard extension is missing");
        return input;
    }

    private static void apply(BytecodePatchContext context) {
        // Resolve all code and app-side reflection ABIs before writing either hook.
        MutableMethod load = exact(context, INSTANCE, "loadBusinessScript", "I", false,
                Arrays.asList(STRING, STRING, STRING, "Z"));
        MutableMethod correct = exact(context, URL, "correctRNBusinessPath", STRING, true,
                Arrays.asList("Lctrip/crn/instance/JSExecutorType;", STRING));
        exact(context, GUARD, "loadPath", STRING, true, Arrays.asList(STRING, STRING));
        exact(context, GUARD, "businessPath", STRING, true, Arrays.asList(STRING));
        exact(context, GUARD, "isPinnedPath", "Z", true, Arrays.asList(STRING, STRING));
        exact(context, GUARD, "resourceKeySucceeded", "Z", true, Arrays.asList("I", "Z"));
        exact(context, "Lctrip/android/pkg/util/Un7zUtil;", "extractAssets", "Z", true,
                Arrays.asList("Landroid/content/Context;", STRING, STRING));
        boolean contextField = false;
        for (com.android.tools.smali.dexlib2.iface.Field field : context.mutableClassDefBy(
                "Lctrip/foundation/FoundationContextHolder;").getFields()) {
            if ("context".equals(field.getName()) && "Landroid/content/Context;".equals(field.getType())
                    && (field.getAccessFlags() & 9) == 9) contextField = true;
        }
        if (!contextField) throw new IllegalStateException("Trip application context field is missing");
        if (load.getImplementation() == null || load.getImplementation().getRegisterCount() != 8
                || correct.getImplementation() == null || correct.getImplementation().getRegisterCount() != 11) {
            throw new IllegalStateException("Trip 8.54.2 CRN register layout differs");
        }
        int nativeLoads = 0;
        int nativeIndex = -1;
        int scanIndex = -1;
        for (Instruction instruction : load.getImplementation().getInstructions()) {
            scanIndex++;
            if (instruction instanceof ReferenceInstruction) {
                Object reference = ((ReferenceInstruction) instruction).getReference();
                if (reference instanceof MethodReference) {
                    MethodReference method = (MethodReference) reference;
                    if (GUARD.equals(method.getDefiningClass())) {
                        throw new IllegalStateException("MyPlan native load hook is already installed");
                    }
                    if (INSTANCE.equals(method.getDefiningClass()) && "loadBusinessScriptNative".equals(method.getName())
                            && "I".equals(method.getReturnType())
                            && same(method.getParameterTypes(), Arrays.asList(STRING, STRING, STRING, "Z"))) {
                        if (instruction.getOpcode() != Opcode.INVOKE_VIRTUAL) {
                            throw new IllegalStateException("Trip native load opcode differs");
                        }
                        nativeLoads++;
                        nativeIndex = scanIndex;
                    }
                }
            }
        }
        if (nativeLoads != 1) throw new IllegalStateException("Trip native business load is not unique");
        Instruction result = load.getImplementation().getInstructions().get(nativeIndex + 1);
        Instruction condition = load.getImplementation().getInstructions().get(nativeIndex + 2);
        if (result.getOpcode() != Opcode.MOVE_RESULT || ((OneRegisterInstruction) result).getRegisterA() != 4
                || !(condition instanceof BuilderInstruction21t) || condition.getOpcode() != Opcode.IF_NEZ
                || ((OneRegisterInstruction) condition).getRegisterA() != 4) {
            throw new IllegalStateException("Trip native business result branch differs");
        }
        BuilderInstruction21t originalCondition = (BuilderInstruction21t) condition;
        Set<String> paths = new HashSet<String>();
        for (Instruction instruction : correct.getImplementation().getInstructions()) {
            if (instruction instanceof ReferenceInstruction
                    && ((ReferenceInstruction) instruction).getReference() instanceof MethodReference
                    && GUARD.equals(((MethodReference) ((ReferenceInstruction) instruction).getReference()).getDefiningClass())) {
                throw new IllegalStateException("MyPlan business path hook is already installed");
            }
            if (instruction instanceof ReferenceInstruction
                    && ((ReferenceInstruction) instruction).getReference() instanceof StringReference) {
                paths.add(((StringReference) ((ReferenceInstruction) instruction).getReference()).getString());
            }
        }
        if (!paths.containsAll(Arrays.asList("/rn_business.jsbundle", "/rn_business.hbcbundle", "/_crn_config"))) {
            throw new IllegalStateException("Trip business-path resolver fingerprint differs");
        }
        // v2's original config-name value is dead after File.exists. Preserve p1's
        // native result (including 1 -> MIXED) while repairing the resource-key branch
        // only for this verified, isolated revision.
        load.getImplementation().replaceInstruction(nativeIndex + 2,
                new BuilderInstruction21t(Opcode.IF_EQZ, 2, originalCondition.getTarget()));
        InstructionExtensions.INSTANCE.addInstructions(load, nativeIndex + 2,
                "invoke-static {p1, v2}, " + GUARD + "->resourceKeySucceeded(IZ)Z\n"
                + "move-result v2");
        InstructionExtensions.INSTANCE.addInstructions(load, nativeIndex,
                "invoke-static {p1, p2}, " + GUARD + "->isPinnedPath(" + STRING + STRING + ")Z\n"
                + "move-result v2");
        InstructionExtensions.INSTANCE.addInstructions(load, 0,
                "invoke-static {p1, p2}, " + GUARD + "->loadPath(" + STRING + STRING + ")" + STRING + "\n"
                + "move-result-object p1");
        InstructionExtensions.INSTANCE.addInstructions(correct, 0,
                "invoke-static {p1}, " + GUARD + "->businessPath(" + STRING + ")" + STRING + "\n"
                + "move-result-object v0\n"
                + "if-eqz v0, :hiro_original_business_path\n"
                + "return-object v0\n"
                + ":hiro_original_business_path\n"
                + "nop");
    }

    private static boolean same(List<? extends CharSequence> actual, List<String> wanted) {
        if (actual.size() != wanted.size()) return false;
        for (int index = 0; index < actual.size(); index++) {
            if (!wanted.get(index).contentEquals(actual.get(index))) return false;
        }
        return true;
    }

    private static MutableMethod exact(BytecodePatchContext context, String type, String name,
            String result, boolean isStatic, List<String> parameters) {
        MutableMethod match = null;
        int count = 0;
        for (MutableMethod method : context.mutableClassDefBy(type).getMethods()) {
            if (name.equals(method.getName()) && result.equals(method.getReturnType())
                    && same(method.getParameterTypes(), parameters)
                    && ((method.getAccessFlags() & 8) != 0) == isStatic) {
                match = method;
                count++;
            }
        }
        if (count != 1) throw new IllegalStateException("Trip MyPlan ABI mismatch: " + type + "->" + name);
        return match;
    }
}
