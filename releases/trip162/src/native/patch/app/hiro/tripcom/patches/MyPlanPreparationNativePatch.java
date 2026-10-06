package app.hiro.tripcom.patches;

import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchBuilder;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.patch.PatchKt;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import kotlin.Unit;

/** An internal dependency of Trip.com's existing home/MyPlan resource patch. */
public final class MyPlanPreparationNativePatch {
    private static final String MODEL =
            "Lcom/ctrip/ibu/framework/baseview/widget/cmtv2/crn/IBUCRNCMTV2Model;";
    private static final String READABLE_MAP = "Lcom/facebook/react/bridge/ReadableMap;";
    private static final String CALLBACK = "Lcom/facebook/react/bridge/Callback;";
    private static final String WRITABLE_MAP = "Lcom/facebook/react/bridge/WritableMap;";
    private static final String GUARD = "Lapp/hiro/tripcom/extension/MyPlanPreparationGuard;";
    private static final String EXTENSION = "extensions/myplan_preparation_guard.mpe";

    public static final BytecodePatch PATCH = PatchKt.bytecodePatch(
            null,
            null,
            true,
            (BytecodePatchBuilder builder) -> {
                builder.setExtensionInputStream(MyPlanPreparationNativePatch::openExtension);
                builder.dependsOn(MyPlanBundleNativePatch.PATCH);
                builder.execute((BytecodePatchContext context) -> {
                    apply(context);
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });

    private MyPlanPreparationNativePatch() {
    }

    private static InputStream openExtension() {
        InputStream input = MyPlanPreparationNativePatch.class.getClassLoader()
                .getResourceAsStream(EXTENSION);
        if (input == null) {
            throw new IllegalStateException("MyPlan preparation guard extension is missing");
        }
        return input;
    }

    private static void apply(BytecodePatchContext context) {
        // Resolve every app/helper type and called constructor before modifying code.
        context.mutableClassDefBy(READABLE_MAP);
        context.mutableClassDefBy(CALLBACK);
        context.mutableClassDefBy(WRITABLE_MAP);
        app.morphe.patcher.util.proxy.mutableTypes.MutableClass writable = context.mutableClassDefBy("Lcom/facebook/react/bridge/WritableNativeMap;");
        boolean constructor = false;
        for (MutableMethod method : writable.getMethods()) {
            if ("<init>".equals(method.getName()) && method.getParameterTypes().isEmpty()
                    && "V".equals(method.getReturnType()) && (method.getAccessFlags() & 8) == 0) {
                constructor = true;
            }
        }
        if (!constructor) throw new IllegalStateException("WritableNativeMap constructor missing");
        requireGuard(context, "suppressRequest", READABLE_MAP);
        requireGuard(context, "suppressPage", "Ljava/lang/String;");
        MutableMethod version2 = exact(context, "getCMTDataV2",
                Arrays.asList(READABLE_MAP, CALLBACK, CALLBACK));
        addGate(version2, "p3", "suppressRequest", READABLE_MAP, "v2");

        MutableMethod legacy = exact(context, "getCMTData",
                Arrays.asList("Ljava/lang/String;", "Ljava/lang/String;", CALLBACK, CALLBACK));
        addGate(legacy, "p4", "suppressPage", "Ljava/lang/String;", "legacy");
    }

    private static void addGate(MutableMethod method, String successParameter,
            String guardMethod, String requestType, String labelSuffix) {
        if ((method.getAccessFlags() & 8) != 0 || method.getImplementation() == null
                || method.getImplementation().getRegisterCount() < method.getParameterTypes().size() + 2
                || method.getImplementation().getRegisterCount() > 16) {
            throw new IllegalStateException("MyPlan native method has no safe scratch register");
        }
        InstructionExtensions.INSTANCE.addInstructions(method, 0,
                "invoke-static {p1, " + successParameter + "}, " + GUARD + "->"
                + guardMethod + "(" + requestType + CALLBACK + ")Z\n"
                + "move-result v0\n"
                + "if-eqz v0, :hiro_keep_preparation_" + labelSuffix + "\n"
                + "new-instance v0, Lcom/facebook/react/bridge/WritableNativeMap;\n"
                + "invoke-direct {v0}, Lcom/facebook/react/bridge/WritableNativeMap;-><init>()V\n"
                + "return-object v0\n"
                + ":hiro_keep_preparation_" + labelSuffix + "\n"
                + "nop");
    }

    private static void requireGuard(BytecodePatchContext context, String name, String request) {
        int count = 0;
        for (MutableMethod method : context.mutableClassDefBy(GUARD).getMethods()) {
            if (name.equals(method.getName()) && "Z".equals(method.getReturnType())
                    && method.getParameterTypes().size() == 2
                    && request.contentEquals(method.getParameterTypes().get(0))
                    && CALLBACK.contentEquals(method.getParameterTypes().get(1))
                    && (method.getAccessFlags() & 8) != 0) count++;
        }
        if (count != 1) throw new IllegalStateException("Native guard method missing: " + name);
    }

    private static MutableMethod exact(BytecodePatchContext context, String name, List<String> parameters) {
        MutableMethod match = null;
        int count = 0;
        for (MutableMethod method : context.mutableClassDefBy(MODEL).getMethods()) {
            if (!name.equals(method.getName()) || !WRITABLE_MAP.equals(method.getReturnType())) {
                continue;
            }
            List<? extends CharSequence> actual = method.getParameterTypes();
            if (actual.size() != parameters.size()) {
                continue;
            }
            boolean same = true;
            for (int index = 0; index < actual.size(); index++) {
                if (!parameters.get(index).contentEquals(actual.get(index))) {
                    same = false;
                    break;
                }
            }
            if (same) {
                if ((method.getAccessFlags() & 8) != 0) {
                    throw new IllegalStateException("Unexpected static MyPlan native method: " + name);
                }
                match = method;
                count++;
            }
        }
        if (count != 1) {
            throw new IllegalStateException("Trip 8.54.2 MyPlan native method not unique: " + name);
        }
        return match;
    }
}
