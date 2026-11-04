import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import com.android.tools.r8.org.objectweb.asm.ClassReader;
import com.android.tools.r8.org.objectweb.asm.tree.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Replace the existing helper only; the two main-class bridges already exist. */
public final class UpdateSpaceLoader {
    private static final String PREFIX = "app/hiro/swiftkey/patches/";
    private static final String MAIN = PREFIX + "JapaneseSamsungEmojiPatch";
    private static final String HELPER = PREFIX + "SpaceConversionPatch";
    private static final String CONTEXT = "Lapp/morphe/patcher/patch/BytecodePatchContext;";
    private static final String VOID_DESC = "(" + CONTEXT + ")V";
    private static final String UNIT_DESC = "(" + CONTEXT + ")Lkotlin/Unit;";
    private static void require(boolean b, String message) {
        if (!b) throw new IllegalStateException(message);
    }
    private static byte[] entry(Path zip, String name) throws Exception {
        try (ZipFile z = new ZipFile(zip.toFile())) {
            ZipEntry e = z.getEntry(name); require(e != null, "Missing " + name);
            try (InputStream in = z.getInputStream(e)) { return in.readAllBytes(); }
        }
    }
    private static ClassNode node(byte[] raw) {
        ClassNode node = new ClassNode(); new ClassReader(raw).accept(node, 0); return node;
    }
    private static MethodNode method(ClassNode node, String name, String desc) {
        MethodNode found = null;
        for (MethodNode m : node.methods) if (name.equals(m.name) && desc.equals(m.desc)) {
            require(found == null, "Duplicate JVM method " + name); found = m;
        }
        require(found != null, "Missing JVM method " + name); return found;
    }
    private static void checkJvmBridge(MethodNode method, String callName) {
        int calls = 0;
        for (AbstractInsnNode instruction : method.instructions) if (instruction instanceof MethodInsnNode) {
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!HELPER.equals(call.owner)) continue;
            require(callName.equals(call.name) && VOID_DESC.equals(call.desc)
                    && call.getOpcode() == 184 && !call.itf, "Unexpected JVM helper call");
            require(call.getPrevious() instanceof VarInsnNode
                    && call.getPrevious().getOpcode() == 25
                    && ((VarInsnNode) call.getPrevious()).var == 0, "JVM context load changed");
            calls++;
        }
        require(calls == 1, "Expected exactly one existing JVM " + callName + " bridge");
    }
    private static void jvm(Path baseline, Path helpers, Path output) throws Exception {
        byte[] mainBytes = entry(baseline, MAIN + ".class");
        ClassNode main = node(mainBytes);
        require(MAIN.equals(main.name), "Unexpected JVM main class");
        checkJvmBridge(method(main, "apply", VOID_DESC), "apply");
        checkJvmBridge(method(main, "lambda$static$1", UNIT_DESC), "checkOriginal");
        byte[] previous = entry(baseline, HELPER + ".class");
        byte[] replacement = Files.readAllBytes(helpers.resolve(HELPER + ".class"));
        ClassNode helper = node(replacement);
        require(HELPER.equals(node(previous).name) && HELPER.equals(helper.name), "Wrong JVM helper class");
        require(!Arrays.equals(previous, replacement), "Helper was not updated");
        method(helper, "apply", VOID_DESC); method(helper, "checkOriginal", VOID_DESC);
        Path target = output.resolve(HELPER + ".class"); Files.createDirectories(target.getParent());
        Files.write(target, replacement);
        // Copy rather than reserialize: the main class and its bridges remain byte-for-byte identical.
        Files.write(output.resolve(MAIN + ".class"), mainBytes);
        require(Arrays.equals(mainBytes, Files.readAllBytes(output.resolve(MAIN + ".class"))), "JVM main drift");
        System.out.println("PASS JVM helper replacement: existing main and bridges byte-exact");
    }
    private static TreeMap<String, ClassDef> dex(byte[] raw) {
        TreeMap<String, ClassDef> result = new TreeMap<>();
        for (ClassDef c : new DexBackedDexFile(Opcodes.forApi(26), java.nio.ByteBuffer.wrap(raw)).getClasses())
            require(result.put(c.getType(), c) == null, "Duplicate DEX class");
        return result;
    }
    private static void checkDexBridge(ClassDef main, String methodName, String callName, int contextRegister) {
        int methods = 0;
        for (Method method : main.getMethods()) if (methodName.equals(method.getName())) {
            require(method.getParameterTypes().equals(Collections.singletonList(CONTEXT))
                    && method.getReturnType().equals(methodName.equals("apply") ? "V" : "Lkotlin/Unit;"),
                    "Android main method descriptor changed");
            require(method.getImplementation() != null, "Missing Android main implementation");
            int calls = 0;
            for (Instruction instruction : method.getImplementation().getInstructions()) {
                if (!(instruction instanceof ReferenceInstruction)
                        || !(((ReferenceInstruction) instruction).getReference() instanceof MethodReference)) continue;
                MethodReference call = (MethodReference) ((ReferenceInstruction) instruction).getReference();
                if (!("L" + HELPER + ";").equals(call.getDefiningClass())) continue;
                require(callName.equals(call.getName()) && call.getReturnType().equals("V")
                        && call.getParameterTypes().equals(Collections.singletonList(CONTEXT)), "Unexpected Android helper call");
                require(instruction.getOpcode() == Opcode.INVOKE_STATIC_RANGE, "Android bridge opcode changed");
                RegisterRangeInstruction range = (RegisterRangeInstruction) instruction;
                require(range.getStartRegister() == contextRegister && range.getRegisterCount() == 1,
                        "Android bridge context register changed");
                calls++;
            }
            require(calls == 1, "Expected exactly one existing Android " + callName + " bridge"); methods++;
        }
        require(methods == 1, "Android bridge method inventory changed");
    }
    private static byte[] canonical(ClassDef c) throws Exception {
        DexPool pool = new DexPool(Opcodes.forApi(26)); pool.internClass(c);
        MemoryDataStore out = new MemoryDataStore();
        try { pool.writeTo(out); return Arrays.copyOf(out.getBuffer(), out.getSize()); } finally { out.close(); }
    }
    private static void android(Path baseline, Path helperDex, Path output) throws Exception {
        TreeMap<String, ClassDef> previous = dex(entry(baseline, "classes.dex"));
        TreeMap<String, ClassDef> replacement = dex(Files.readAllBytes(helperDex));
        String helperType = "L" + HELPER + ";", mainType = "L" + MAIN + ";";
        require(replacement.size() == 1 && replacement.containsKey(helperType), "Unexpected compiled helper inventory");
        require(previous.containsKey(helperType), "Existing Android helper missing");
        ClassDef main = previous.get(mainType); require(main != null, "Missing Android main patch class");
        checkDexBridge(main, "apply", "apply", 25);
        checkDexBridge(main, "lambda$static$1", "checkOriginal", 2);
        require(!Arrays.equals(canonical(previous.get(helperType)), canonical(replacement.get(helperType))),
                "Android helper was not updated");
        TreeMap<String, ClassDef> updated = new TreeMap<>(previous); updated.putAll(replacement);
        DexPool pool = new DexPool(Opcodes.forApi(26)); for (ClassDef c : updated.values()) pool.internClass(c);
        Files.createDirectories(output.toAbsolutePath().getParent());
        FileDataStore store = new FileDataStore(output.toFile());
        try { pool.writeTo(store); } finally { store.close(); }
        TreeMap<String, ClassDef> serialized = dex(Files.readAllBytes(output));
        require(serialized.keySet().equals(previous.keySet()), "Android class inventory changed");
        for (Map.Entry<String, ClassDef> e : updated.entrySet())
            require(Arrays.equals(canonical(e.getValue()), canonical(serialized.get(e.getKey()))),
                    "Android serialization drift " + e.getKey());
        for (Map.Entry<String, ClassDef> e : previous.entrySet()) if (!e.getKey().equals(helperType))
            require(Arrays.equals(canonical(e.getValue()), canonical(serialized.get(e.getKey()))),
                    "Unrelated Android class changed " + e.getKey());
        System.out.println("PASS Android helper replacement: replaced=1 added=0 main-preserved=true preserved="
                + (previous.size() - 1) + " total=" + previous.size());
    }
    public static void main(String[] args) throws Exception {
        require(args.length == 4, "Usage: UpdateSpaceLoader jvm|dex baseline.mpp helper-class-dir|helper.dex output-dir|output.dex");
        if (args[0].equals("jvm")) jvm(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
        else if (args[0].equals("dex")) android(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
        else throw new IllegalArgumentException("Unknown mode");
    }
}
