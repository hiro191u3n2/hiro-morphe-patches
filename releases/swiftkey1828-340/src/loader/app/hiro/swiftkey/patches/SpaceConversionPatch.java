package app.hiro.swiftkey.patches;

import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;

/** Guard the software key path with a runtime Japanese-space conversion check. */
public final class SpaceConversionPatch {
    static final String OWNER = "Lwb0/x;";
    static final String SIGNATURE = "P(Llh0/a;Ljava/lang/String;Ljava/lang/String;Lec0/m;ZLjava/lang/Integer;)V";
    static final String HASH = "ca5a10a0231dea3a9301151ee98a1d96ab010e7b86613ecedf0d77bdcee67e16";
    static final String HOOK = "Lhiro/swiftkey/SpaceConversion;->handle(Lwb0/x;Llh0/a;Ljava/lang/String;Ljava/lang/String;)Z";
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;
    private SpaceConversionPatch() {}

    public static void checkOriginal(BytecodePatchContext context) {
        inspect(context.classDefByOrNull(OWNER));
    }
    public static void apply(BytecodePatchContext context) {
        checkOriginal(context);
        MutableMethod target = null;
        for (MutableMethod method : context.mutableClassDefBy(OWNER).getMethods())
            if ((OWNER + "->" + SIGNATURE).equals(FORMAT.getMethodDescriptor(method))) {
                require(target == null, "Duplicate mutable space target"); target = method;
            }
        require(target != null, "Missing mutable space target");
        applyMethod(target);
    }
    static Method inspect(ClassDef owner) {
        require(owner != null && OWNER.equals(owner.getType()) && "Ljava/lang/Object;".equals(owner.getSuperclass()),
                "Keyboard controller changed");
        Method found = null;
        for (Method method : owner.getMethods())
            if ((OWNER + "->" + SIGNATURE).equals(FORMAT.getMethodDescriptor(method))) {
                require(found == null, "Duplicate native space target"); found = method;
            }
        verify(found); return found;
    }
    private static void verify(Method method) {
        require(method != null && method.getImplementation() != null, "Missing native space target");
        require((OWNER + "->" + SIGNATURE).equals(FORMAT.getMethodDescriptor(method)), "Space signature changed");
        require(method.getAccessFlags() == 0x11 && method.getImplementation().getRegisterCount() == 17,
                "Space ABI/registers changed");
        require(HASH.equals(MethodContract.sha256(method)), "Software key fingerprint changed");
    }
    static void applyMethod(MutableMethod method) {
        verify(method);
        InstructionExtensions.INSTANCE.addInstructions(method, 0,
                "invoke-static/range {p0 .. p3}, " + HOOK + "\n"
                + "move-result v0\n"
                + "if-eqz v0, :hiro_space_original\n"
                + "return-void\n"
                + ":hiro_space_original");
        require(method.getImplementation().getRegisterCount() == 17, "Space hook changed native registers");
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
