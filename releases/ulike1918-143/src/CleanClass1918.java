import java.nio.file.*;
import jdk.internal.org.objectweb.asm.*;

/** Keep JVM loader equivalent to Android DEX; strip only the two obsolete payload installers. */
public final class CleanClass1918 {
 public static final String DESCRIPTION="v1.9.18（v1.8.8基準）指定7設定・高解像度特徴点の強制・AI試験・素材書き出し・診断記録を削除。設定の戻るは撮影画面、撮影画面の戻るは従来の終了処理。実機未検証。";
 public static void main(String[] args)throws Exception {
  byte[] input=Files.readAllBytes(Path.of(args[0]));ClassReader cr=new ClassReader(input);
  // A fresh constant pool prevents removed installer/description names from lingering.
  ClassWriter cw=new ClassWriter(0); final int[] calls={0},descriptions={0};
  cr.accept(new ClassVisitor(Opcodes.ASM8,cw){
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions){
    MethodVisitor mv=super.visitMethod(access,name,desc,sig,exceptions);
    return new MethodVisitor(Opcodes.ASM8,mv){
     @Override public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean itf){
      if(owner.matches("app/hiro/ulike/patches/IntegrationPayload18[12]")&&method.equals("install")){
       if(opcode!=Opcodes.INVOKESTATIC||!descriptor.equals("(Lapp/morphe/patcher/patch/ResourcePatchContext;)V"))throw new IllegalStateException("Unexpected installer signature");
       super.visitInsn(Opcodes.POP);calls[0]++;return;
      }
      super.visitMethodInsn(opcode,owner,method,descriptor,itf);
     }
     @Override public void visitLdcInsn(Object value){
      if(value instanceof String s&&s.startsWith("v1.8.8 倍率・接写UI")){value=DESCRIPTION;descriptions[0]++;}
      super.visitLdcInsn(value);
     }
    };
   }
  },0);
  if(calls[0]!=2||descriptions[0]!=1)throw new IllegalStateException("Unexpected JVM loader edits "+calls[0]+"/"+descriptions[0]);
  Path out=Path.of(args[1]);Files.createDirectories(out.getParent());Files.write(out,cw.toByteArray());
  System.out.println("PASS JVM loader: 2 obsolete installers removed; description updated");
 }
}
