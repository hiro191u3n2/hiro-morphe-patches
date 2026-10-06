package app.hiro.swiftkey.patches;

import java.util.*;
import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.AccessFlags;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation;

/** Three proven Backspace paths, wrapped only after complete native operations. */
public final class DeleteProtectionPatch {
    private static final String OWNER = "Lec0/x;";
    private static final String ORIGINAL_PREFIX = "hiro$deleteOriginal$";
    private static final String HELPER = "Lhiro/swiftkey/DeleteLatinBoundary;->";
    private static final String[] SIGNATURES = {
        "i(Lec0/n;I)Z", "k(Lec0/n;I)Z", "w(Ljava/lang/String;Lec0/n;Ljava/lang/Long;)Z"
    };
    private static final int[] REGISTERS = {6, 5, 8};
    private static final String[] HASHES = {
        "51b836fc90aaf016f87ebef9fb316d7c2b63aa2e4022e434f3233c687e717214",
        "a47a2a74b52c561d0f1165e68108295947b82e7c7c6547d806e8caac188ac19c",
        "e1c3d49a56e518713f3da45c252a5624fc99bf5d16c24701122d2faf59b04814"
    };
    private DeleteProtectionPatch() {}
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("SwiftKey deletion protection: " + message);
    }
    private static String signature(Method method) {
        StringBuilder s = new StringBuilder(method.getName()).append('(');
        for (CharSequence p : method.getParameterTypes()) s.append(p);
        return s.append(')').append(method.getReturnType()).toString();
    }
    private static MutableMethod find(MutableClass owner, String signature) {
        MutableMethod found = null;
        for (MutableMethod method : owner.getMethods()) if (signature(method).equals(signature)) {
            require(found == null, "Duplicate native method " + signature);
            found = method;
        }
        require(found != null, "Missing native method " + signature);
        return found;
    }
    private static MutableMethod[] checked(MutableClass owner) {
        require(owner.getType().equals(OWNER), "Unexpected owner");
        for (MutableMethod method : owner.getMethods())
            require(!method.getName().startsWith(ORIGINAL_PREFIX), "Already patched APK; select the original APK");
        MutableMethod[] result = new MutableMethod[SIGNATURES.length];
        for (int i = 0; i < result.length; i++) {
            MutableMethod method = find(owner, SIGNATURES[i]);
            require(method.getImplementation() != null && method.getImplementation().getRegisterCount() == REGISTERS[i],
                    "Native register layout differs: " + SIGNATURES[i]);
            require(HASHES[i].equals(MethodContract.sha256(method)), "Native method is not original: " + SIGNATURES[i]);
            require(!AccessFlags.STATIC.isSet(method.getAccessFlags()) && AccessFlags.PUBLIC.isSet(method.getAccessFlags())
                    && AccessFlags.FINAL.isSet(method.getAccessFlags()), "Native access flags differ");
            result[i] = method;
        }
        return result;
    }
    public static void checkOriginal(BytecodePatchContext context) {
        require(context.classDefByOrNull(OWNER) != null, "Native class is missing");
        checked(context.mutableClassDefBy(OWNER));
    }
    private static MutableMethod renamed(Method original) {
        return new MutableMethod(new ImmutableMethod(original.getDefiningClass(), ORIGINAL_PREFIX + original.getName(),
                original.getParameters(), original.getReturnType(), original.getAccessFlags(), original.getAnnotations(),
                original.getHiddenApiRestrictions(), original.getImplementation()));
    }
    private static MutableMethod wrapper(Method original) {
        int words = 1;
        for (CharSequence p : original.getParameterTypes()) words += p.toString().equals("J") || p.toString().equals("D") ? 2 : 1;
        require(words <= 4, "Unexpected argument width");
        MutableMethod result = new MutableMethod(new ImmutableMethod(original.getDefiningClass(), original.getName(),
                original.getParameters(), original.getReturnType(), original.getAccessFlags(), original.getAnnotations(),
                original.getHiddenApiRestrictions(), new ImmutableMethodImplementation(words + 2,
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList())));
        String nativeSignature = ORIGINAL_PREFIX + signature(original);
        String code = "invoke-static {p0}, " + HELPER + "beforeDelete(Lec0/x;)[Ljava/lang/Object;\n"
                + "move-result-object v0\n"
                + "invoke-virtual/range {p0 .. p" + (words - 1) + "}, " + OWNER + "->" + nativeSignature + "\n"
                + "move-result v1\n"
                + "invoke-static {p0, v0, v1}, " + HELPER + "afterDelete(Lec0/x;[Ljava/lang/Object;Z)V\n"
                + "return v1";
        InstructionExtensions.INSTANCE.addInstructions(result, code);
        return result;
    }
    public static void apply(BytecodePatchContext context) {
        applyToClass(context.mutableClassDefBy(OWNER));
    }
    // The same checked operation is exercised against the target's native DEX in local QA.
    static void applyToClass(MutableClass owner) {
        MutableMethod[] originals = checked(owner);
        MutableMethod[] renamed = new MutableMethod[originals.length];
        MutableMethod[] wrappers = new MutableMethod[originals.length];
        // Validate and assemble every wrapper before mutating the class.
        for (int i = 0; i < originals.length; i++) {
            renamed[i] = renamed(originals[i]);
            wrappers[i] = wrapper(originals[i]);
            require(MethodContract.body(originals[i]).equals(MethodContract.body(renamed[i])), "Native body changed");
        }
        Set<MutableMethod> virtual = owner.getVirtualMethods();
        Set<MutableMethod> methods = owner.getMethods();
        for (MutableMethod original : originals) require(virtual.contains(original) && methods.contains(original), "Native method set differs");
        for (int i = 0; i < originals.length; i++) {
            virtual.remove(originals[i]); methods.remove(originals[i]);
            virtual.add(renamed[i]); methods.add(renamed[i]);
            virtual.add(wrappers[i]); methods.add(wrappers[i]);
        }
        for (int i = 0; i < originals.length; i++) {
            require(find(owner, SIGNATURES[i]) == wrappers[i], "Wrapper installation failed");
            require(find(owner, ORIGINAL_PREFIX + SIGNATURES[i]) == renamed[i], "Native method preservation failed");
        }
    }
}
