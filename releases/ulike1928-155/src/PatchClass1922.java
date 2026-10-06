import java.nio.file.*;
import jdk.internal.org.objectweb.asm.*;
/** Update the JVM patch metadata only; all loader instructions stay intact. */
public final class PatchClass1922 {
 public static void main(String[] a)throws Exception{
  ClassReader cr=new ClassReader(Files.readAllBytes(Path.of(a[0])));ClassWriter cw=new ClassWriter(0);final int[] changes={0};
  cr.accept(new ClassVisitor(Opcodes.ASM8,cw){
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions){
    return new MethodVisitor(Opcodes.ASM8,super.visitMethod(access,name,desc,sig,exceptions)){
     @Override public void visitLdcInsn(Object value){if(value instanceof String s&&s.startsWith("v1.9.21（v1.9.20基準）")){value=Transform1922.DESCRIPTION;changes[0]++;}super.visitLdcInsn(value);}
    };
   }
  },0);
  if(changes[0]!=1)throw new IllegalStateException("Expected one metadata change");Files.write(Path.of(a[1]),cw.toByteArray());System.out.println("PASS JVM loader metadata updated, implementation unchanged");
 }
}
