import java.io.*;
import java.nio.file.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.util.TraceClassVisitor;

/** Inserts exactly two bootstrap calls without recompiling the existing patch. */
public final class Bootstrap183 {
    static final String PREFIX = "app/hiro/swiftkey/patches/";
    static final String DESC = "(Lapp/morphe/patcher/patch/BytecodePatchContext;)V";
    static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
    static ClassNode read(byte[] data) {
        ClassNode node = new ClassNode(Opcodes.ASM8);
        new ClassReader(data).accept(node, 0);
        return node;
    }
    static String trace(ClassNode node) {
        StringWriter result = new StringWriter();
        node.accept(new TraceClassVisitor(new PrintWriter(result)));
        return result.toString();
    }
    static void inject(ClassNode node, String methodName, String action) {
        int count = 0;
        for (MethodNode m : node.methods) {
            if (!m.name.equals(methodName)) continue;
            for (AbstractInsnNode ins : m.instructions.toArray()) {
                if (!(ins instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) ins;
                if (call.owner.equals(PREFIX + "JapaneseGlidePatch")
                        && call.name.equals(action) && call.desc.equals(DESC)
                        && call.getOpcode() == Opcodes.INVOKESTATIC && !call.itf) {
                    InsnList add = new InsnList();
                    add.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            PREFIX + "DeleteProtectionPatch", action, DESC, false));
                    m.instructions.insert(ins, add);
                    require(m.maxStack >= 1, "Insufficient existing stack");
                    count++;
                }
            }
        }
        require(count == 1, "Expected one existing anchor for " + methodName + ": " + count);
    }
    static void strip(ClassNode node) {
        int count = 0;
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode ins : m.instructions.toArray()) {
                if (!(ins instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) ins;
                if (!call.owner.equals(PREFIX + "DeleteProtectionPatch")) continue;
                require(call.desc.equals(DESC) && call.getOpcode() == Opcodes.INVOKESTATIC && !call.itf,
                        "Unexpected injected descriptor");
                require((m.name.equals("apply") && call.name.equals("apply"))
                                || (m.name.equals("lambda$static$1") && call.name.equals("checkOriginal")),
                        "Unexpected injected method");
                AbstractInsnNode previous = ins.getPrevious();
                require(previous instanceof VarInsnNode && previous.getOpcode() == Opcodes.ALOAD
                        && ((VarInsnNode) previous).var == 0, "Context parameter is not local 0");
                m.instructions.remove(previous);
                m.instructions.remove(ins);
                count++;
            }
        }
        require(count == 2, "Expected exactly two bootstrap calls");
    }
    public static void main(String[] args) throws Exception {
        byte[] original = Files.readAllBytes(Paths.get(args[0]));
        ClassNode changed = read(original);
        require(changed.name.equals(PREFIX + "JapaneseSamsungEmojiPatch"), "Wrong bootstrap class");
        inject(changed, "apply", "apply");
        inject(changed, "lambda$static$1", "checkOriginal");
        ClassWriter writer = new ClassWriter(new ClassReader(original), 0);
        changed.accept(writer);
        byte[] output = writer.toByteArray();
        ClassNode normalized = read(output);
        strip(normalized);
        require(trace(read(original)).equals(trace(normalized)),
                "Existing JVM instructions, frames, metadata or other methods changed");
        Files.write(Paths.get(args[1]), output);
        System.out.println("PASS JVM bootstrap: exactly two static calls; stripping them restores all original code, frames and metadata");
    }
}
