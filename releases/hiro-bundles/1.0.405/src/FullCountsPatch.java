package app.hiro.newxui.patches;

import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import java.util.*;
import kotlin.Unit;

/** Exact counts can be selected alone or together with the existing Hiro settings. */
public final class FullCountsPatch {
    public static final String NAME = "件数を省略せず全桁表示";
    static final String OPTIONS = "Lapp/hiro/newxui/FullCounts;";
    private static final String LEGACY_OPTIONS = "Lapp/hiro/newxui/UiOptions;";
    private static final String LEGACY_FORMAT = "Lapp/hiro/newxui/ViewCountFormat;";

    public static final Patch PATCH = PatchKt.bytecodePatch(NAME,
        "返信・リポスト・いいね・表示回数などを元の数値から桁区切りで表示します。旧Hiro表示設定と併用する場合は、表示回数のスイッチに従います。", true, b -> {
        b.compatibleWith(new Compatibility("com.twitter.android", "NewX", null, ApkFileType.APKM,
            "#000000", null, Collections.singletonList(new AppTarget("12.43.0-prod.01", Collections.emptyMap(),
            false, null, null)), FormFactor.MOBILE));
        b.extendWith(() -> FullCountsPatch.class.getResourceAsStream("/extensions/newx_full_counts.mpe"));
        b.finalize(context -> {
            FullCountHook.apply(context.mutableClassDefBy(FullCountHook.TYPE));
            SecondaryCountHooks.apply(context);
            if (context.classDefByOrNull(LEGACY_OPTIONS) != null) useSavedPreference(context);
            return Unit.INSTANCE;
        });
        return Unit.INSTANCE;
    });

    private static MutableMethodImplementation body(BytecodePatchContext context, String type,
            String methodName, List<String> parameters, String result) {
        for (Method method : context.mutableClassDefBy(type).getMethods()) {
            if (methodName.equals(method.getName()) && parameters.equals(method.getParameterTypes())
                    && result.equals(method.getReturnType())) return ((MutableMethod) method).getImplementation();
        }
        throw new IllegalStateException("Hiroの件数設定が見つかりません: "+methodName);
    }

    private static void useSavedPreference(BytecodePatchContext context) {
        MutableMethodImplementation option = body(context, OPTIONS, "enabled", Collections.emptyList(), "Z");
        option.addInstruction(0, new BuilderInstruction35c(Opcode.INVOKE_STATIC,0,0,0,0,0,0,
                new ImmutableMethodReference(LEGACY_OPTIONS, "fullViewCounts", Collections.emptyList(),"Z")));
        option.addInstruction(1, new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
        option.addInstruction(2, new BuilderInstruction11x(Opcode.RETURN,0));
        // The older view-only renderer passes the already formatted string as its last parameter.
        MutableMethodImplementation old = body(context, LEGACY_FORMAT, "format",
                Arrays.asList("Ljava/lang/Long;","Ljava/lang/String;"), "Ljava/lang/String;");
        old.addInstruction(0,new BuilderInstruction11x(Opcode.RETURN_OBJECT,old.getRegisterCount()-1));
    }
}
