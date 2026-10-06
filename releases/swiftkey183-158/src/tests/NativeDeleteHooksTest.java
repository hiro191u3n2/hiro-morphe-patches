package app.hiro.swiftkey.patches;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;

/** Runs production patching against the target version's ec0.x from a locally supplied APK. */
public final class NativeDeleteHooksTest {
    static final String PREFIX = "hiro$deleteOriginal$";
    static final String[] NAMES = {"i", "k", "w"};
    static int checks;
    static void require(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    static String signature(Method m) {
        StringBuilder s = new StringBuilder(m.getName()).append('(');
        for (CharSequence p : m.getParameterTypes()) s.append(p);
        return s.append(')').append(m.getReturnType()).toString();
    }
    static TreeMap<String, Method> methods(ClassDef c) {
        TreeMap<String, Method> result = new TreeMap<>();
        for (Method m : c.getMethods()) require(result.put(signature(m),m) == null, "Duplicate method");
        return result;
    }
    static ClassDef find(String path) throws Exception {
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(path), Opcodes.forApi(26));
        for (String e : dex.getDexEntryNames()) for (ClassDef c : dex.getEntry(e).getDexFile().getClasses())
            if (c.getType().equals("Lec0/x;")) return c;
        throw new IllegalArgumentException("ec0.x missing");
    }
    static void rejected(MutableClass target, String label) {
        Map<String, String> before = new TreeMap<>();
        for (Method m : target.getMethods()) before.put(signature(m), MethodContract.sha256(m));
        boolean rejected = false;
        try { DeleteProtectionPatch.applyToClass(target); } catch (IllegalStateException e) { rejected = true; }
        require(rejected, label + " was accepted");
        Map<String, String> after = new TreeMap<>();
        for (Method m : target.getMethods()) after.put(signature(m), MethodContract.sha256(m));
        require(before.equals(after), label + " partially mutated the class");
    }
    static void checkWrapper(Method old, Method wrapper, Method nativeMethod) {
        Method restored = new ImmutableMethod(nativeMethod.getDefiningClass(), old.getName(), nativeMethod.getParameters(),
                nativeMethod.getReturnType(), nativeMethod.getAccessFlags(), nativeMethod.getAnnotations(),
                nativeMethod.getHiddenApiRestrictions(), nativeMethod.getImplementation());
        require(MethodContract.sha256(old).equals(MethodContract.sha256(restored)), "Native method body or registers changed");
        require(MethodContract.body(old).equals(MethodContract.body(nativeMethod)), "Native branches/try blocks differ");
        require(wrapper.getAccessFlags() == old.getAccessFlags(), "Wrapper access changed");
        require(wrapper.getAnnotations().equals(old.getAnnotations()), "Wrapper annotations changed");
        require(wrapper.getImplementation().getTryBlocks().isEmpty(), "Wrapper swallows native exceptions");
        List<Instruction> instructions = new ArrayList<>();
        for (Instruction i : wrapper.getImplementation().getInstructions()) instructions.add(i);
        require(instructions.size() == 6, "Unexpected wrapper instruction count");
        Opcode[] expected = {Opcode.INVOKE_STATIC, Opcode.MOVE_RESULT_OBJECT, Opcode.INVOKE_VIRTUAL_RANGE,
                Opcode.MOVE_RESULT, Opcode.INVOKE_STATIC, Opcode.RETURN};
        for (int i=0;i<expected.length;i++) require(instructions.get(i).getOpcode() == expected[i], "Wrapper order changed");
        int words = old.getParameterTypes().size() + 1;
        require(wrapper.getImplementation().getRegisterCount() == words + 2, "Wrapper registers differ");
        FiveRegisterInstruction before = (FiveRegisterInstruction)instructions.get(0);
        require(before.getRegisterCount()==1 && before.getRegisterC()==2, "Wrong before snapshot context");
        MethodReference beforeRef=(MethodReference)((ReferenceInstruction)before).getReference();
        require(beforeRef.getDefiningClass().equals("Lhiro/swiftkey/DeleteLatinBoundary;") && beforeRef.getName().equals("beforeDelete"), "Wrong before method");
        require(((OneRegisterInstruction)instructions.get(1)).getRegisterA()==0, "Snapshot register changed");
        RegisterRangeInstruction originalCall=(RegisterRangeInstruction)instructions.get(2);
        require(originalCall.getStartRegister()==2 && originalCall.getRegisterCount()==words, "Native arguments reordered or omitted");
        MethodReference originalRef=(MethodReference)((ReferenceInstruction)originalCall).getReference();
        require(originalRef.getDefiningClass().equals("Lec0/x;") && originalRef.getName().equals(PREFIX+old.getName()), "Wrong native call");
        require(originalRef.getParameterTypes().equals(old.getParameterTypes()) && originalRef.getReturnType().equals("Z"), "Native signature differs");
        require(((OneRegisterInstruction)instructions.get(3)).getRegisterA()==1, "Native result not retained");
        FiveRegisterInstruction after=(FiveRegisterInstruction)instructions.get(4);
        require(after.getRegisterCount()==3 && after.getRegisterC()==2 && after.getRegisterD()==0 && after.getRegisterE()==1, "After arguments differ");
        MethodReference afterRef=(MethodReference)((ReferenceInstruction)after).getReference();
        require(afterRef.getDefiningClass().equals("Lhiro/swiftkey/DeleteLatinBoundary;") && afterRef.getName().equals("afterDelete"), "Wrong after method");
        require(((OneRegisterInstruction)instructions.get(5)).getRegisterA()==1, "Native result changed");
    }
    public static void main(String[] args) throws Exception {
        ClassDef original=find(args[0]);
        TreeMap<String, Method> before=methods(original);
        MutableClass target = new MutableClass(original);
        // Populate all lazily cached sets before production mutation.
        target.getMethods(); target.getVirtualMethods(); target.getDirectMethods();
        int virtualBefore=target.getVirtualMethods().size(), directBefore=target.getDirectMethods().size();
        DeleteProtectionPatch.applyToClass(target);
        TreeMap<String, Method> after=methods(target);
        require(after.size()==before.size()+3, "Unexpected method additions");
        require(target.getVirtualMethods().size()==virtualBefore+3 && target.getDirectMethods().size()==directBefore, "Cached method sets differ");
        int wrapped=0;
        for (Map.Entry<String,Method> e:before.entrySet()) {
            Method old=e.getValue();
            if (Arrays.asList(NAMES).contains(old.getName())) {
                checkWrapper(old, after.get(e.getKey()), after.get(PREFIX+e.getKey())); wrapped++;
            } else require(MethodContract.sha256(old).equals(MethodContract.sha256(after.get(e.getKey()))), "Other native method changed: "+e.getKey());
        }
        require(wrapped==3,"Wrong number of wrapped methods");
        rejected(target,"Already patched native class");
        MutableClass changed = new MutableClass(original);
        for (MutableMethod method:changed.getMethods()) if (method.getName().equals("w"))
            method.getImplementation().addInstruction(0,new BuilderInstruction10x(Opcode.NOP));
        rejected(changed,"Modified final target (all-original preflight)");
        MutableClass missing = new MutableClass(original);
        missing.getMethods().removeIf(m->m.getName().equals("k"));
        rejected(missing,"Missing native target");
        DexPool pool=new DexPool(Opcodes.forApi(26)); pool.internClass(target);
        FileDataStore out=new FileDataStore(new File(args[1]));
        try {pool.writeTo(out);} finally {out.close();}
        ClassDef roundtrip=find(args[1]);
        TreeMap<String, Method> emitted=methods(roundtrip);
        require(emitted.keySet().equals(after.keySet()),"Emitted DEX omitted methods");
        for (String s:after.keySet()) require(MethodContract.sha256(after.get(s)).equals(MethodContract.sha256(emitted.get(s))), "DEX roundtrip changed method: "+s);
        System.out.println("PASS native ec0.x hook application: " + checks + " checks; three original methods preserved, three wrappers emitted, native calls/results/exceptions retained, other methods unchanged");
        System.out.println("Input limitation: ec0.x is taken from the same-version archived v2 APK; this is not a full clean-APK or Android/ART execution test.");
    }
}
