import java.nio.file.*;
import java.security.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

/** Add one integrity-pinned GPU library row; preserve both original rows and installation safeguards. */
public final class PatchGpuLoader1949 {
 static final String OWNER="app/hiro/ulike/patches/IntegrationPayload186";
 static final String TARGET="lib/arm64-v8a/libulike_gpu1949.so";
 static final String RESOURCE="ulike1949/runtime/libulike_gpu1949.so";
 static void need(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 static ClassNode read(byte[] b){ClassNode n=new ClassNode(Opcodes.ASM8);new ClassReader(b).accept(n,0);return n;}
 static byte[] write(ClassNode n){ClassWriter w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
 static String sha(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 static AbstractInsnNode previous(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
 public static void main(String[] a)throws Exception{
  need(a.length==3,"ORIGINAL_CLASS GPU_SO OUTPUT_CLASS");byte[] original=Files.readAllBytes(Path.of(a[0])),gpu=Files.readAllBytes(Path.of(a[1]));
  need(gpu.length>=64&&gpu[0]==127&&gpu[1]=='E'&&gpu[2]=='L'&&gpu[3]=='F'&&gpu[4]==2&&gpu[5]==1&&gpu[18]==(byte)183&&gpu[19]==0,"ELF64 AArch64 required");
  ClassNode node=read(original);need(node.name.equals(OWNER),"Exact installer owner required");
  MethodNode init=null,install=null;for(MethodNode m:node.methods){if(m.name.equals("<clinit>"))init=m;if(m.name.equals("install"))install=m;}
  need(init!=null&&install!=null,"Installer methods required");
  AbstractInsnNode size=init.instructions.getFirst();while(size.getOpcode()<0)size=size.getNext();need(size.getOpcode()==Opcodes.ICONST_2&&size.getNext() instanceof TypeInsnNode,"Exact inherited two-row allocation required");
  FieldInsnNode store=null;for(AbstractInsnNode n:init.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.PUTSTATIC&&f.owner.equals(OWNER)&&f.name.equals("ITEMS")){need(store==null,"One ITEMS store required");store=f;}
  need(store!=null,"ITEMS store required");AbstractInsnNode guard=null;
  for(AbstractInsnNode n:install.instructions.toArray())if(n.getOpcode()==Opcodes.IF_ICMPEQ){AbstractInsnNode p=previous(n),q=previous(p),r=previous(q);if(p.getOpcode()==Opcodes.ICONST_2&&q.getOpcode()==Opcodes.ARRAYLENGTH&&r instanceof FieldInsnNode f&&f.name.equals("ITEMS")){need(guard==null,"One exact size guard required");guard=p;}}
  need(guard!=null,"Original installer size guard required");
  var row=new InsnList();row.add(new InsnNode(Opcodes.DUP));row.add(new InsnNode(Opcodes.ICONST_2));row.add(new InsnNode(Opcodes.ICONST_4));row.add(new TypeInsnNode(Opcodes.ANEWARRAY,"java/lang/String"));
  String[] values={TARGET,sha(gpu),Integer.toString(gpu.length),RESOURCE};for(int i=0;i<4;i++){row.add(new InsnNode(Opcodes.DUP));row.add(new InsnNode(Opcodes.ICONST_0+i));row.add(new LdcInsnNode(values[i]));row.add(new InsnNode(Opcodes.AASTORE));}row.add(new InsnNode(Opcodes.AASTORE));
  AbstractInsnNode[] added=row.toArray();AbstractInsnNode size3=new InsnNode(Opcodes.ICONST_3),guard3=new InsnNode(Opcodes.ICONST_3);
  init.instructions.set(size,size3);install.instructions.set(guard,guard3);init.instructions.insertBefore(store,row);byte[] output=write(node);
  // Independent inverse proves no staging, digest checks, rollback, manifest or inherited row changed.
  for(AbstractInsnNode n:added)init.instructions.remove(n);init.instructions.set(size3,size);install.instructions.set(guard3,guard);
  need(Arrays.equals(write(read(original)),write(node)),"Installer changed beyond third pinned row and exact row-count guard");Files.write(Path.of(a[2]),output);
  System.out.println("PASS GPU installer inverse; original two rows and transactional safeguards retained; new row sha256="+sha(gpu)+" bytes="+gpu.length);
 }
}
