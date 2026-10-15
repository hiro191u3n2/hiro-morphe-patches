import java.nio.file.*;
import jdk.internal.org.objectweb.asm.*;
/** Change display metadata only; retain the JVM loader implementation. */
public final class PatchClass1941 {
 public static void main(String[] args)throws Exception{
  ClassReader cr=new ClassReader(Files.readAllBytes(Path.of(args[0])));ClassWriter cw=new ClassWriter(0);int[] count={0};
  cr.accept(new ClassVisitor(Opcodes.ASM8,cw){
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){
    return new MethodVisitor(Opcodes.ASM8,super.visitMethod(access,name,desc,signature,exceptions)){
     @Override public void visitLdcInsn(Object value){if(value instanceof String s&&s.startsWith(Transform1941.OLD_DESCRIPTION)){value=Transform1941.DESCRIPTION;count[0]++;}super.visitLdcInsn(value);}
    };
   }
  },0);
  if(count[0]!=1)throw new IllegalStateException("Expected one ULike metadata string");
  Files.write(Path.of(args[1]),cw.toByteArray());System.out.println("PASS JVM metadata updated; loader implementation preserved");
 }
}
