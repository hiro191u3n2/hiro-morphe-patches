import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import app.hiro.swiftkey.patches.MethodContract;
import com.android.tools.r8.org.objectweb.asm.ClassReader;
import com.android.tools.r8.org.objectweb.asm.ClassWriter;
import com.android.tools.r8.org.objectweb.asm.tree.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Add two narrowly located bridge calls, preserving the existing patch body. */
public final class UpdateShiftLoader {
    private static final String PREFIX = "app/hiro/swiftkey/patches/";
    private static final String MAIN = PREFIX + "JapaneseSamsungEmojiPatch";
    private static final String ANCHOR = PREFIX + "EmojiFooterPatch";
    private static final String HELPER = PREFIX + "ShiftLockPatch";
    private static final String CONTEXT = "Lapp/morphe/patcher/patch/BytecodePatchContext;";
    private static final String VOID_DESC = "(" + CONTEXT + ")V";
    private static final String UNIT_DESC = "(" + CONTEXT + ")Lkotlin/Unit;";
    private static void require(boolean b, String message) { if (!b) throw new IllegalStateException(message); }
    private static String sha(byte[] raw) throws Exception {
        StringBuilder b = new StringBuilder();
        for (byte x : MessageDigest.getInstance("SHA-256").digest(raw)) b.append(String.format("%02x", x & 255));
        return b.toString();
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
    private static byte[] write(ClassReader pool, ClassNode node) {
        ClassWriter out = new ClassWriter(pool, 0); node.accept(out); return out.toByteArray();
    }
    private static MethodNode method(ClassNode node, String name, String desc) {
        MethodNode found = null;
        for (MethodNode method : node.methods) if (name.equals(method.name) && desc.equals(method.desc)) {
            require(found == null, "Duplicate JVM method"); found = method;
        }
        require(found != null, "Missing JVM method " + name); return found;
    }
    private static void injectJvm(MethodNode method, String callName) {
        MethodInsnNode anchor = null;
        for (AbstractInsnNode instruction : method.instructions) if (instruction instanceof MethodInsnNode) {
            MethodInsnNode call = (MethodInsnNode) instruction;
            require(!HELPER.equals(call.owner), "Shift bridge already present");
            if (ANCHOR.equals(call.owner) && callName.equals(call.name) && VOID_DESC.equals(call.desc)) {
                require(anchor == null && call.getOpcode() == 184 && !call.itf, "Ambiguous JVM bridge anchor");
                require(call.getPrevious() instanceof VarInsnNode
                        && call.getPrevious().getOpcode() == 25
                        && ((VarInsnNode) call.getPrevious()).var == 0, "JVM context register changed");
                anchor = call;
            }
        }
        require(anchor != null && method.maxStack >= 1, "Missing JVM bridge anchor");
        InsnList extra = new InsnList();
        extra.add(new VarInsnNode(25, 0));
        extra.add(new MethodInsnNode(184, HELPER, callName, VOID_DESC, false));
        method.instructions.insert(anchor, extra);
    }
    private static void removeJvmBridge(MethodNode method, String callName) {
        MethodInsnNode target = null;
        for (AbstractInsnNode instruction : method.instructions) if (instruction instanceof MethodInsnNode) {
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (HELPER.equals(call.owner)) {
                require(target == null && callName.equals(call.name) && VOID_DESC.equals(call.desc), "Unexpected JVM helper call");
                target = call;
            }
        }
        require(target != null && target.getPrevious() instanceof VarInsnNode, "Missing inserted JVM bridge");
        method.instructions.remove(target.getPrevious()); method.instructions.remove(target);
    }
    private static void jvm(Path baseline, Path helpers, Path output) throws Exception {
        byte[] original = entry(baseline, MAIN + ".class");
        require(original.length > 0, "Empty baseline JVM patch");
        ClassReader pool = new ClassReader(original);
        ClassNode main = node(original);
        require(MAIN.equals(main.name), "Unexpected JVM main class");
        byte[] normalized = write(pool, main);
        injectJvm(method(main, "apply", VOID_DESC), "apply");
        injectJvm(method(main, "lambda$static$1", UNIT_DESC), "checkOriginal");
        byte[] updated = write(pool, main);
        ClassNode reverted = node(updated);
        removeJvmBridge(method(reverted, "apply", VOID_DESC), "apply");
        removeJvmBridge(method(reverted, "lambda$static$1", UNIT_DESC), "checkOriginal");
        require(Arrays.equals(normalized, write(pool, reverted)), "Unrelated JVM bytecode changed");
        Path target = output.resolve(MAIN + ".class"); Files.createDirectories(target.getParent()); Files.write(target, updated);
        byte[] helper = Files.readAllBytes(helpers.resolve(HELPER + ".class"));
        require(HELPER.equals(node(helper).name), "Wrong helper class");
        Files.write(output.resolve(HELPER + ".class"), helper);
        System.out.println("PASS JVM bridge: two inserted calls; all prior normalized class content preserved");
    }
    private static TreeMap<String, ClassDef> dex(byte[] raw) {
        TreeMap<String, ClassDef> result = new TreeMap<>();
        for (ClassDef c : new DexBackedDexFile(Opcodes.forApi(26), java.nio.ByteBuffer.wrap(raw)).getClasses())
            require(result.put(c.getType(), c) == null, "Duplicate DEX class");
        return result;
    }
    private static Method injectDex(Method original, String callName, String hash, int registers, int contextRegister) {
        require(original.getImplementation() != null, "Missing original loader implementation");
        hash = MethodContract.sha256(original);
        MutableMethodImplementation body = new MutableMethodImplementation(original.getImplementation());
        require(body.getRegisterCount() == registers, "Android loader registers changed");
        int anchor = -1;
        for (int i = 0; i < body.getInstructions().size(); i++) {
            Instruction instruction = body.getInstructions().get(i);
            if (!(instruction instanceof ReferenceInstruction)
                    || !(((ReferenceInstruction) instruction).getReference() instanceof MethodReference)) continue;
            MethodReference call = (MethodReference) ((ReferenceInstruction) instruction).getReference();
            require(!("L" + HELPER + ";").equals(call.getDefiningClass()), "Shift Android bridge already present");
            if (!("L" + ANCHOR + ";").equals(call.getDefiningClass()) || !callName.equals(call.getName())) continue;
            require(anchor == -1 && call.getReturnType().equals("V")
                    && call.getParameterTypes().equals(Collections.singletonList(CONTEXT)), "Ambiguous Android bridge anchor");
            require(instruction.getOpcode() == Opcode.INVOKE_STATIC_RANGE, "Android anchor opcode changed");
            RegisterRangeInstruction range = (RegisterRangeInstruction) instruction;
            require(range.getRegisterCount() == 1 && range.getStartRegister() == contextRegister, "Android context register changed");
            anchor = i;
        }
        require(anchor >= 0, "Missing Android bridge anchor");
        body.addInstruction(anchor + 1, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, contextRegister, 1,
                new ImmutableMethodReference("L" + HELPER + ";", callName, Collections.singletonList(CONTEXT), "V")));
        Method updated = copy(original, body);
        MutableMethodImplementation reverted = new MutableMethodImplementation(updated.getImplementation());
        reverted.removeInstruction(anchor + 1);
        require(MethodContract.sha256(copy(original, reverted)).equals(hash), "Unrelated Android method body changed");
        return updated;
    }
    private static Method copy(Method original, MethodImplementation implementation) {
        return new ImmutableMethod(original.getDefiningClass(), original.getName(), original.getParameters(), original.getReturnType(),
                original.getAccessFlags(), original.getAnnotations(), original.getHiddenApiRestrictions(), implementation);
    }
    private static byte[] canonical(ClassDef c) throws Exception {
        DexPool p = new DexPool(Opcodes.forApi(26)); p.internClass(c);
        MemoryDataStore out = new MemoryDataStore();
        try { p.writeTo(out); return Arrays.copyOf(out.getBuffer(),out.getSize()); } finally { out.close(); }
    }
    private static void android(Path baseline, Path helperDex, Path output) throws Exception {
        TreeMap<String, ClassDef> old = dex(entry(baseline, "classes.dex")), added = dex(Files.readAllBytes(helperDex));
        require(added.size() == 1 && added.containsKey("L" + HELPER + ";"), "Unexpected compiled helper inventory");
        require(!old.containsKey("L" + HELPER + ";"), "Shift helper already present");
        ClassDef original = old.get("L" + MAIN + ";"); require(original != null, "Missing Android main patch class");
        List<Method> methods = new ArrayList<>(); int changed = 0;
        for (Method method : original.getMethods()) {
            if (method.getName().equals("apply")) {
                methods.add(injectDex(method, "apply", null, 26, 25)); changed++;
            } else if (method.getName().equals("lambda$static$1")) {
                methods.add(injectDex(method, "checkOriginal", null, 3, 2)); changed++;
            } else methods.add(method);
        }
        require(changed == 2, "Android bridge method count changed");
        old.put(original.getType(), new ImmutableClassDef(original.getType(), original.getAccessFlags(), original.getSuperclass(),
                original.getInterfaces(), original.getSourceFile(), original.getAnnotations(), original.getFields(), methods));
        old.putAll(added);
        DexPool pool = new DexPool(Opcodes.forApi(26)); for (ClassDef c : old.values()) pool.internClass(c);
        Files.createDirectories(output.toAbsolutePath().getParent());
        FileDataStore store = new FileDataStore(output.toFile()); try { pool.writeTo(store); } finally { store.close(); }
        TreeMap<String, ClassDef> serialized = dex(Files.readAllBytes(output));
        require(serialized.keySet().equals(old.keySet()), "Merged Android inventory changed");
        for (Map.Entry<String,ClassDef> entry : old.entrySet())
            require(Arrays.equals(canonical(entry.getValue()), canonical(serialized.get(entry.getKey()))), "Serialized Android class drift " + entry.getKey());
        TreeMap<String,ClassDef> untouched = dex(entry(baseline,"classes.dex"));
        for (Map.Entry<String,ClassDef> entry : untouched.entrySet()) if (!entry.getKey().equals("L"+MAIN+";"))
            require(Arrays.equals(canonical(entry.getValue()), canonical(serialized.get(entry.getKey()))), "Unrelated Android class changed " + entry.getKey());
        System.out.println("PASS Android bridge: changed=1 added=1 preserved=" + (old.size() - 2) + " total=" + old.size());
    }
    public static void main(String[] args) throws Exception {
        require(args.length == 4, "Usage: UpdateShiftLoader jvm|dex baseline.mpp helper-class-dir|helper.dex output-dir|output.dex");
        if (args[0].equals("jvm")) jvm(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
        else if (args[0].equals("dex")) android(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
        else throw new IllegalArgumentException("Unknown mode");
    }
}
