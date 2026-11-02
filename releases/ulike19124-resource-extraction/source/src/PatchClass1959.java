import java.nio.file.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
/** Display metadata update with a normalized class inverse proof. */
public final class PatchClass1959 {
 static ClassNode read(byte[] raw){ClassNode n=new ClassNode(Opcodes.ASM8);new ClassReader(raw).accept(n,0);return n;}
 static byte[] write(ClassNode n){ClassWriter w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
 public static void main(String[] args)throws Exception{
  byte[] original=Files.readAllBytes(Path.of(args[0]));ClassNode n=read(original);LdcInsnNode hit=null;Object old=null;int count=0;
  for(MethodNode m:n.methods)for(AbstractInsnNode ins:m.instructions.toArray())if(ins instanceof LdcInsnNode ldc&&ldc.cst instanceof String s&&s.startsWith(Transform1959.OLD_DESCRIPTION)){hit=ldc;old=ldc.cst;ldc.cst=Transform1959.DESCRIPTION;count++;}
  if(count!=1)throw new IllegalStateException("Expected one ULike display metadata string");
  byte[] result=write(n);hit.cst=old;
  if(!Arrays.equals(write(read(original)),write(n)))throw new IllegalStateException("Loader changed beyond ULike description metadata");
  Files.write(Path.of(args[1]),result);System.out.println("PASS JVM metadata inverse; exact executable loader implementation preserved");
 }
}
