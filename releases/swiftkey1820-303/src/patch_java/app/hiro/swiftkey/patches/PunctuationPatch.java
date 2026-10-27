package app.hiro.swiftkey.patches;

import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;

/** Only tagged punctuation alternate factories are eligible; normal taps stay native. */
public final class PunctuationPatch {
    static final String OWNER = "Lfc0/s1;";
    static final String[] SIGNATURES = {
        "q(Ljava/util/List;Ljava/lang/String;Ljava/lang/String;Lfc0/m1;Lfc0/v1;)Lfb0/f2;",
        "c(Lfc0/m1;Lfc0/v1;)Lxc0/a;"
    };
    private static final String[] HASHES = {
        "e0580b86a2c4c8f8761fa98838c58e292be7d26f03d80371d3d4af83212ffe29",
        "7d34c827f57a81aa2311ea9b49ab09dfc6d825818b51ba7d98d16b9961fdc289"
    };
    private static final int[] REGISTERS = {10, 8};
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;
    private PunctuationPatch() {}
    public static void checkOriginal(BytecodePatchContext context) { inspect(context.classDefByOrNull(OWNER)); }
    public static void apply(BytecodePatchContext context) {
        checkOriginal(context);
        MutableMethod[] methods = new MutableMethod[SIGNATURES.length];
        for (int i = 0; i < methods.length; i++) {
            for (MutableMethod method : context.mutableClassDefBy(OWNER).getMethods())
                if ((OWNER+"->"+SIGNATURES[i]).equals(FORMAT.getMethodDescriptor(method))) {
                    require(methods[i] == null, "Duplicate punctuation target"); methods[i] = method;
                }
            require(methods[i] != null, "Missing mutable punctuation target");
        }
        applyMethods(methods);
    }
    static Method[] inspect(ClassDef target) {
        require(target != null && target.getType().equals(OWNER) && target.getSuperclass().equals("Ljava/lang/Object;"), "Punctuation factory type changed");
        Method[] result = new Method[SIGNATURES.length];
        for (int i = 0; i < result.length; i++) {
            for (Method method : target.getMethods()) if ((OWNER+"->"+SIGNATURES[i]).equals(FORMAT.getMethodDescriptor(method))) {
                require(result[i] == null, "Duplicate punctuation factory"); result[i] = method;
            }
            verify(result[i],i);
        }
        return result;
    }
    private static void verify(Method method, int index) {
        require(method != null && method.getImplementation() != null, "Missing punctuation factory body");
        require((OWNER+"->"+SIGNATURES[index]).equals(FORMAT.getMethodDescriptor(method)), "Punctuation signature changed");
        require(method.getAccessFlags()==0x11 && method.getImplementation().getRegisterCount()==REGISTERS[index], "Punctuation ABI/registers changed");
        require(HASHES[index].equals(MethodContract.sha256(method)), "Punctuation method fingerprint changed " + SIGNATURES[index]);
    }
    static void applyMethods(MutableMethod[] methods) {
        require(methods != null && methods.length == 2, "Punctuation hook count changed");
        for (int i = 0; i < methods.length; i++) verify(methods[i],i);
        for (int i = 0; i < methods.length; i++) {
            String call = i == 0
                ? "invoke-static {p0, p4, p5}, Lhiro/swiftkey/PunctuationKeys;->periodPopup(Lfc0/s1;Lfc0/m1;Lfc0/v1;)Lfb0/f2;"
                : "invoke-static {p0, p1, p2}, Lhiro/swiftkey/PunctuationKeys;->longVowelPopup(Lfc0/s1;Lfc0/m1;Lfc0/v1;)Lxc0/a;";
            InstructionExtensions.INSTANCE.addInstructions(methods[i],0,call+"\nmove-result-object v0\nif-eqz v0, :native_punctuation\nreturn-object v0\n:native_punctuation\nnop");
            require(methods[i].getImplementation().getRegisterCount()==REGISTERS[i], "Punctuation hook changed registers");
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
