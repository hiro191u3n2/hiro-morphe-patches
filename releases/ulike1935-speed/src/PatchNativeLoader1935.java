import java.nio.file.*;
import java.security.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
/** Append one exact arm64 kernel to the retained verified resource installer. */
public final class PatchNativeLoader1935 {
 static final String OWNER="app/hiro/ulike/patches/IntegrationPayload186";
 static final String TARGET="lib/arm64-v8a/libulike_speed1935.so";
 static final String RESOURCE="ulike1935/runtime/libulike_speed1935.so";
 static void req(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
 static ClassNode read(byte[] bytes){ClassNode node=new ClassNode(Opcodes.ASM8);new ClassReader(bytes).accept(node,0);return node;}
 static byte[] write(ClassNode node){ClassWriter out=new ClassWriter(0);node.accept(out);return out.toByteArray();}
 static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
 static boolean expectedField(AbstractInsnNode n){return n instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.PUTSTATIC&&f.owner.equals(OWNER)&&f.name.equals("ITEMS")&&f.desc.equals("[[Ljava/lang/String;");}
 static AbstractInsnNode firstReal(MethodNode m){AbstractInsnNode n=m.instructions.getFirst();while(n!=null&&n.getOpcode()<0)n=n.getNext();return n;}
 static AbstractInsnNode nextReal(AbstractInsnNode n){n=n.getNext();while(n!=null&&n.getOpcode()<0)n=n.getNext();return n;}
 static void item(InsnList out,int index,String value){out.add(new InsnNode(Opcodes.DUP));out.add(new InsnNode(Opcodes.ICONST_0+index));out.add(new LdcInsnNode(value));out.add(new InsnNode(Opcodes.AASTORE));}
 public static void main(String[] args)throws Exception{
  req(args.length==3,"ORIGINAL_CLASS NATIVE_SO OUTPUT_CLASS");byte[] bytes=Files.readAllBytes(Path.of(args[0])),lib=Files.readAllBytes(Path.of(args[1]));
  req(lib.length>=64&&lib[0]==0x7f&&lib[1]=='E'&&lib[2]=='L'&&lib[3]=='F'&&lib[4]==2&&lib[5]==1&&lib[18]==(byte)183&&lib[19]==0,"Expected ELF64 little-endian AArch64 library");
  ClassNode node=read(bytes);req(node.name.equals(OWNER),"Wrong installer class");int count=0;MethodNode init=null,install=null;AbstractInsnNode initCount=null,installCount=null;List<AbstractInsnNode> added=new ArrayList<>();
  for(MethodNode m:node.methods){
   if(m.name.equals("<clinit>")){init=m;initCount=firstReal(m);req(initCount.getOpcode()==Opcodes.ICONST_1&&nextReal(initCount) instanceof TypeInsnNode t&&t.getOpcode()==Opcodes.ANEWARRAY&&t.desc.equals("[Ljava/lang/String;"),"Exact original one-row initializer");m.instructions.set(initCount,new InsnNode(Opcodes.ICONST_2));initCount=firstReal(m);int hits=0;
    for(AbstractInsnNode n=m.instructions.getFirst();n!=null;n=n.getNext())if(expectedField(n)){
     InsnList row=new InsnList();row.add(new InsnNode(Opcodes.DUP));row.add(new InsnNode(Opcodes.ICONST_1));row.add(new InsnNode(Opcodes.ICONST_4));row.add(new TypeInsnNode(Opcodes.ANEWARRAY,"java/lang/String"));
     item(row,0,TARGET);item(row,1,sha(lib));item(row,2,Integer.toString(lib.length));item(row,3,RESOURCE);row.add(new InsnNode(Opcodes.AASTORE));
     for(AbstractInsnNode a:row.toArray())added.add(a);m.instructions.insertBefore(n,row);hits++;
    }req(hits==1,"One ITEMS assignment");count++;
   }
   if(m.name.equals("install")&&m.desc.equals("(Lapp/morphe/patcher/patch/ResourcePatchContext;)V")){install=m;AbstractInsnNode f=firstReal(m);req(f instanceof FieldInsnNode k&&k.getOpcode()==Opcodes.GETSTATIC&&k.owner.equals(OWNER)&&k.name.equals("ITEMS"),"Exact installer gate");AbstractInsnNode len=nextReal(f),one=nextReal(len);req(len.getOpcode()==Opcodes.ARRAYLENGTH&&one.getOpcode()==Opcodes.ICONST_1&&nextReal(one).getOpcode()==Opcodes.IF_ICMPEQ,"Exact one-row gate");installCount=new InsnNode(Opcodes.ICONST_2);m.instructions.set(one,installCount);count++;}
  }
  req(count==2,"Both and only exact initializer/gate patched");byte[] output=write(node);
  // Invert those two edits and require every other instruction, field and attribute.
  init.instructions.set(initCount,new InsnNode(Opcodes.ICONST_1));for(AbstractInsnNode a:added)init.instructions.remove(a);install.instructions.set(installCount,new InsnNode(Opcodes.ICONST_1));
  req(Arrays.equals(write(read(bytes)),write(node)),"Installer changed beyond appended pinned row and exact gate");
  Files.write(Path.of(args[2]),output);System.out.println("PASS native loader exact inverse; existing chroma row/methods retained; "+TARGET+" sha256="+sha(lib)+" bytes="+lib.length);
 }
}
