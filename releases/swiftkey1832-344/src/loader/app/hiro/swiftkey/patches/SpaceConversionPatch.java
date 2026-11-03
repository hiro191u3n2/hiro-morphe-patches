package app.hiro.swiftkey.patches;

import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import java.util.Collections;

/** Guard space input and override separators only at the native commit read sites. */
public final class SpaceConversionPatch {
    static final String OWNER = "Lwb0/x;";
    static final String SIGNATURE = "P(Llh0/a;Ljava/lang/String;Ljava/lang/String;Lec0/m;ZLjava/lang/Integer;)V";
    static final String HASH = "ca5a10a0231dea3a9301151ee98a1d96ab010e7b86613ecedf0d77bdcee67e16";
    static final String HOOK = "Lhiro/swiftkey/SpaceConversion;->handle(Lwb0/x;Llh0/a;Ljava/lang/String;Ljava/lang/String;)Z";
    static final String SEPARATOR_HOOK = "Lhiro/swiftkey/SpaceConversion;->separator(Lm40/b;)Ljava/lang/String;";
    static final String[] OWNERS = {OWNER, "Lac0/b;", "Lwb0/e;", "Lec0/x;", "Lec0/w;", "Lec0/w;", "Lma0/o;"};
    static final String[] SIGNATURES = {SIGNATURE, "e(Lm40/b;Lec0/n;)V", "d(Lm40/b;Lwb0/j;ILec0/n;Z)Z", "A(Lec0/x;Lm40/b;Lwb0/j;Ljava/lang/String;Lec0/y;I)V", "d(Lm40/x;)Ljava/lang/Object;", "i(Lm40/c0;)Ljava/lang/Object;", "q(Lwb0/n0;Lyb0/a;)V"};
    static final String[] HASHES = {HASH, "eac2980000af75b4f9b4a4fa795bdaa0e936c61115ac9d6afc70a1930583b243", "a70a88f9ac76f31eb9dc1d9685fbc03feea2226187fadfe16cc6198edf6ae9ab", "e34f122bb39f315e5985789af1a61c8309b72dac8934e4be774bbd66d735bc58", "1cfdaeb31e6fa0f3481768fd1437050d667a16b230df15cc1dbdc47eb2b09a22", "3f6f1c557f84804ffd3006a829772c36345ebc8c799f66436f45e08cc55dc834", "4d38a507eeab3150fbd3fe3c7dc6cd55153f3760ce126dc211c2c1f2a4b72e97"};
    static final int[] FLAGS = {17, 17, 17, 9, 17, 17, 1};
    static final int[] REGISTERS = {17, 4, 12, 6, 13, 21, 12};
    static final int[][] SITES = {{}, {2}, {8}, {5}, {13}, {15, 39}, {45}};
    static final int[][] RECEIVERS = {{}, {2}, {7}, {1}, {12}, {2, 2}, {4}};
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;
    private SpaceConversionPatch() {}

    public static void checkOriginal(BytecodePatchContext context) {
        for (int i = 0; i < OWNERS.length; i++) inspect(context.classDefByOrNull(OWNERS[i]), i);
    }
    public static void apply(BytecodePatchContext context) {
        // Validate the entire set before changing even the original space hook.
        checkOriginal(context);
        MutableMethod[] targets = new MutableMethod[OWNERS.length];
        for (int i = 0; i < OWNERS.length; i++) {
            for (MutableMethod method : context.mutableClassDefBy(OWNERS[i]).getMethods())
                if (descriptor(i).equals(FORMAT.getMethodDescriptor(method))) {
                    require(targets[i] == null, "Duplicate mutable space target"); targets[i] = method;
                }
            verify(targets[i], i);
        }
        for (int i = 0; i < targets.length; i++) applyMethod(targets[i], i);
    }
    static String descriptor(int index) { return OWNERS[index] + "->" + SIGNATURES[index]; }
    static Method inspect(ClassDef owner) { return inspect(owner, 0); }
    static Method inspect(ClassDef owner, int index) {
        String parent = "Lec0/w;".equals(OWNERS[index]) ? "Lm40/a;" : "Ljava/lang/Object;";
        require(owner != null && OWNERS[index].equals(owner.getType()) && parent.equals(owner.getSuperclass()), "Native commit owner changed: " + OWNERS[index]);
        Method found = null;
        for (Method method : owner.getMethods()) if (descriptor(index).equals(FORMAT.getMethodDescriptor(method))) {
            require(found == null, "Duplicate native space target"); found = method;
        }
        verify(found, index); return found;
    }
    static void verify(Method method, int index) {
        require(method != null && method.getImplementation() != null, "Missing native space target: " + descriptor(index));
        require(descriptor(index).equals(FORMAT.getMethodDescriptor(method)), "Space signature changed");
        require(method.getAccessFlags() == FLAGS[index] && method.getImplementation().getRegisterCount() == REGISTERS[index], "Space ABI/registers changed");
        require(HASHES[index].equals(MethodContract.sha256(method)), "Native commit fingerprint changed: " + descriptor(index));
        int n = 0, found = 0;
        for (Instruction instruction : method.getImplementation().getInstructions()) {
            for (int site = 0; site < SITES[index].length; site++) if (n == SITES[index][site]) {
                Opcode expected = index == 5 ? Opcode.INVOKE_VIRTUAL : Opcode.INVOKE_INTERFACE;
                String getterOwner = index == 5 ? "Lm40/c0;" : "Lm40/b;";
                require(instruction.getOpcode() == expected && instruction instanceof FiveRegisterInstruction && instruction instanceof ReferenceInstruction, "Native separator opcode changed");
                FiveRegisterInstruction call = (FiveRegisterInstruction) instruction;
                require(call.getRegisterCount() == 1 && call.getRegisterC() == RECEIVERS[index][site] && call.getRegisterD() == 0 && call.getRegisterE() == 0 && call.getRegisterF() == 0 && call.getRegisterG() == 0, "Native separator registers changed");
                require((getterOwner + "->getTrailingSeparator()Ljava/lang/String;").equals(FORMAT.getReference(((ReferenceInstruction) instruction).getReference())), "Native separator getter changed");
                found++;
            }
            n++;
        }
        require(found == SITES[index].length, "Missing native separator read site");
    }
    static void applyMethod(MutableMethod method) { applyMethod(method, 0); }
    static void applyMethod(MutableMethod method, int index) {
        verify(method, index);
        if (index == 0) {
            InstructionExtensions.INSTANCE.addInstructions(method, 0,
                    "invoke-static/range {p0 .. p3}, " + HOOK + "\n"
                    + "move-result v0\nif-eqz v0, :hiro_space_original\nreturn-void\n:hiro_space_original");
        } else {
            for (int site = 0; site < SITES[index].length; site++)
                method.getImplementation().replaceInstruction(SITES[index][site], new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, RECEIVERS[index][site], 0, 0, 0, 0,
                        new ImmutableMethodReference("Lhiro/swiftkey/SpaceConversion;", "separator", Collections.singletonList("Lm40/b;"), "Ljava/lang/String;")));
        }
        require(method.getImplementation().getRegisterCount() == REGISTERS[index], "Space hook changed native registers");
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
