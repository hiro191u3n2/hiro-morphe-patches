import java.nio.file.*;import org.objectweb.asm.*;
/** Change only the version description; preserve the existing exact native hook. */
public final class UpdatePatchStrings {
 public static void main(String[] a)throws Exception{
  if(a.length!=4)throw new IllegalArgumentException("OLD_CLASS NEW_CLASS OLD_VERSION_PREFIX DESCRIPTION_FILE");String description=Files.readString(Path.of(a[3])).strip();if(description.isEmpty()||description.contains("\n"))throw new IllegalArgumentException("One-line description required");ClassReader r=new ClassReader(Files.readAllBytes(Path.of(a[0])));ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);int[] n={0,0,0,0};
  String context="app/morphe/patcher/patch/ResourcePatchContext",helper="app/hiro/ulike/patches/NativeNv21EffectFlag";
  r.accept(new ClassVisitor(Opcodes.ASM9,w){@Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex){
   boolean hook=name.equals("applyAssets")&&desc.equals("(L"+context+";)V");
   if(hook){if((access&Opcodes.ACC_STATIC)==0)throw new IllegalStateException("applyAssets must be static");n[1]++;}
   return new MethodVisitor(Opcodes.ASM9,super.visitMethod(access,name,desc,sig,ex)){
    @Override public void visitCode(){super.visitCode();if(hook){super.visitVarInsn(Opcodes.ALOAD,0);super.visitMethodInsn(Opcodes.INVOKESTATIC,"app/hiro/ulike/patches/IntegrationPayload169","install","(L"+context+";)V",false);n[3]++;}}
    @Override public void visitLdcInsn(Object value){if(value instanceof String s&&s.startsWith(a[2])){value=description;n[0]++;}super.visitLdcInsn(value);}
    @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){if(owner.equals(helper)){if(!hook||op!=Opcodes.INVOKESTATIC||!name.equals("apply")||!desc.equals("(Ljava/nio/file/Path;)V"))throw new IllegalStateException("Unexpected native hook");n[2]++;}super.visitMethodInsn(op,owner,name,desc,itf);}
   };
  }},0);
  if(n[0]!=1||n[1]!=1||n[2]!=1||n[3]!=1)throw new IllegalStateException("Expected description, applyAssets and native hook exactly once: "+java.util.Arrays.toString(n));Path out=Path.of(a[1]);Files.createDirectories(out.getParent());Files.write(out,w.toByteArray());
 }
}
