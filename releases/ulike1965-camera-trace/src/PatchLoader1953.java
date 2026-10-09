import java.nio.file.*;
import java.security.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

/** Append one fingerprinted library; preserve all existing transactional installer rows. */
public final class PatchLoader1953 {
 static final String OWNER="app/hiro/ulike/patches/IntegrationPayload186";
 static void need(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 static ClassNode read(byte[] b){ClassNode n=new ClassNode(Opcodes.ASM8);new ClassReader(b).accept(n,0);return n;}
 static byte[] write(ClassNode n){ClassWriter w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
 static String sha(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 static AbstractInsnNode previous(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
 static void elf(byte[] b){need(b.length>=64&&b[0]==127&&b[1]=='E'&&b[2]=='L'&&b[3]=='F'&&b[4]==2&&b[5]==1&&b[18]==(byte)183&&b[19]==0,"ELF64 AArch64 required");}
 static void integer(InsnList list,int n){if(n<=5)list.add(new InsnNode(Opcodes.ICONST_0+n));else list.add(new IntInsnNode(Opcodes.BIPUSH,n));}
 static InsnList row(int index,String[] values){
  var out=new InsnList();out.add(new InsnNode(Opcodes.DUP));integer(out,index);integer(out,4);out.add(new TypeInsnNode(Opcodes.ANEWARRAY,"java/lang/String"));
  for(int i=0;i<4;i++){out.add(new InsnNode(Opcodes.DUP));integer(out,i);out.add(new LdcInsnNode(values[i]));out.add(new InsnNode(Opcodes.AASTORE));}
  out.add(new InsnNode(Opcodes.AASTORE));return out;
 }
 static String[] values(String name,String path,byte[] binary)throws Exception{return new String[]{"lib/arm64-v8a/libulike_"+name+".so",sha(binary),Integer.toString(binary.length),path+"/runtime/libulike_"+name+".so"};}
 public static void main(String[] a)throws Exception{
  need(a.length==3,"ORIGINAL_CLASS FINISH_SO OUTPUT_CLASS");
  byte[] original=Files.readAllBytes(Path.of(a[0])),lib=Files.readAllBytes(Path.of(a[1]));elf(lib);
  ClassNode node=read(original);need(node.name.equals(OWNER),"Pinned installer owner required");
  MethodNode init=null,install=null;for(MethodNode m:node.methods){if(m.name.equals("<clinit>"))init=m;if(m.name.equals("install"))install=m;}
  need(init!=null&&install!=null,"Installer methods required");
  AbstractInsnNode size=init.instructions.getFirst();while(size.getOpcode()<0)size=size.getNext();
  need(size instanceof IntInsnNode&&size.getOpcode()==Opcodes.BIPUSH&&((IntInsnNode)size).operand==7&&size.getNext() instanceof TypeInsnNode,"Exact .52 seven-row allocation required");
  FieldInsnNode store=null;for(AbstractInsnNode n:init.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.PUTSTATIC&&f.owner.equals(OWNER)&&f.name.equals("ITEMS")){need(store==null,"One ITEMS store required");store=f;}
  need(store!=null,"ITEMS store required");AbstractInsnNode guard=null;
  for(AbstractInsnNode n:install.instructions.toArray())if(n.getOpcode()==Opcodes.IF_ICMPEQ){AbstractInsnNode p=previous(n),q=previous(p),r=previous(q);if(p instanceof IntInsnNode&&p.getOpcode()==Opcodes.BIPUSH&&((IntInsnNode)p).operand==7&&q.getOpcode()==Opcodes.ARRAYLENGTH&&r instanceof FieldInsnNode f&&f.name.equals("ITEMS")){need(guard==null,"One exact size guard required");guard=p;}}
  need(guard!=null,"Original installer size guard required");
  var extra=row(7,values("finish1953","ulike1953",lib));AbstractInsnNode[] inserted=extra.toArray();
  AbstractInsnNode size8=new IntInsnNode(Opcodes.BIPUSH,8),guard8=new IntInsnNode(Opcodes.BIPUSH,8);
  init.instructions.set(size,size8);install.instructions.set(guard,guard8);init.instructions.insertBefore(store,extra);
  byte[] output=write(node);
  for(AbstractInsnNode n:inserted)init.instructions.remove(n);
  init.instructions.set(size8,size);install.instructions.set(guard8,guard);
  need(Arrays.equals(write(read(original)),write(node)),"Installer changed beyond one appended row and row-count guard");
  Files.write(Path.of(a[2]),output);System.out.println("PASS 8-row installer inverse; all seven existing rows and transactional safeguards retained");
 }
}
