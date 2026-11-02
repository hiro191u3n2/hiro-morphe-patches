import java.nio.file.*;
import java.security.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

/** Update the existing GPU row and add one core row, preserving all installer safeguards. */
public final class PatchGpuLoader1950 {
 static final String OWNER="app/hiro/ulike/patches/IntegrationPayload186";
 static void need(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 static ClassNode read(byte[] b){ClassNode n=new ClassNode(Opcodes.ASM8);new ClassReader(b).accept(n,0);return n;}
 static byte[] write(ClassNode n){ClassWriter w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
 static String sha(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 static AbstractInsnNode previous(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
 static void elf(byte[] b){need(b.length>=64&&b[0]==127&&b[1]=='E'&&b[2]=='L'&&b[3]=='F'&&b[4]==2&&b[5]==1&&b[18]==(byte)183&&b[19]==0,"ELF64 AArch64 required");}
 public static void main(String[] a)throws Exception{
  need(a.length==4,"ORIGINAL_CLASS GPU_SO CORE_SO OUTPUT_CLASS");byte[] original=Files.readAllBytes(Path.of(a[0])),gpu=Files.readAllBytes(Path.of(a[1])),core=Files.readAllBytes(Path.of(a[2]));elf(gpu);elf(core);
  ClassNode node=read(original);need(node.name.equals(OWNER),"Exact installer owner required");
  MethodNode init=null,install=null;for(MethodNode m:node.methods){if(m.name.equals("<clinit>"))init=m;if(m.name.equals("install"))install=m;}
  need(init!=null&&install!=null,"Installer methods required");
  AbstractInsnNode size=init.instructions.getFirst();while(size.getOpcode()<0)size=size.getNext();need(size.getOpcode()==Opcodes.ICONST_3&&size.getNext() instanceof TypeInsnNode,"Exact .49 three-row allocation required");
  FieldInsnNode store=null;for(AbstractInsnNode n:init.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.PUTSTATIC&&f.owner.equals(OWNER)&&f.name.equals("ITEMS")){need(store==null,"One ITEMS store required");store=f;}
  need(store!=null,"ITEMS store required");AbstractInsnNode guard=null;
  for(AbstractInsnNode n:install.instructions.toArray())if(n.getOpcode()==Opcodes.IF_ICMPEQ){AbstractInsnNode p=previous(n),q=previous(p),r=previous(q);if(p.getOpcode()==Opcodes.ICONST_3&&q.getOpcode()==Opcodes.ARRAYLENGTH&&r instanceof FieldInsnNode f&&f.name.equals("ITEMS")){need(guard==null,"One exact size guard required");guard=p;}}
  need(guard!=null,"Original installer size guard required");
  String oldSha=System.getProperty("ulike.gpu.oldsha"),oldBytes=System.getProperty("ulike.gpu.oldbytes");need(oldSha!=null&&oldBytes!=null,"Exact GPU baseline pins required");
  LdcInsnNode digest=null,bytes=null;int targets=0;
  for(AbstractInsnNode n:init.instructions.toArray())if(n instanceof LdcInsnNode x){if("lib/arm64-v8a/libulike_gpu1949.so".equals(x.cst))targets++;if(oldSha.equals(x.cst)){need(digest==null,"Unique old GPU checksum");digest=x;}if(oldBytes.equals(x.cst)){need(bytes==null,"Unique old GPU size");bytes=x;}}
  need(targets==1&&digest!=null&&bytes!=null,"Exactly one existing integrity-pinned GPU row");digest.cst=sha(gpu);bytes.cst=Integer.toString(gpu.length);
  var row=new InsnList();row.add(new InsnNode(Opcodes.DUP));row.add(new InsnNode(Opcodes.ICONST_3));row.add(new InsnNode(Opcodes.ICONST_4));row.add(new TypeInsnNode(Opcodes.ANEWARRAY,"java/lang/String"));
  String[] values={"lib/arm64-v8a/libulike_core1950.so",sha(core),Integer.toString(core.length),"ulike1950/runtime/libulike_core1950.so"};for(int i=0;i<4;i++){row.add(new InsnNode(Opcodes.DUP));row.add(new InsnNode(Opcodes.ICONST_0+i));row.add(new LdcInsnNode(values[i]));row.add(new InsnNode(Opcodes.AASTORE));}row.add(new InsnNode(Opcodes.AASTORE));
  AbstractInsnNode[] added=row.toArray();AbstractInsnNode size4=new InsnNode(Opcodes.ICONST_4),guard4=new InsnNode(Opcodes.ICONST_4);
  init.instructions.set(size,size4);install.instructions.set(guard,guard4);init.instructions.insertBefore(store,row);byte[] output=write(node);
  for(AbstractInsnNode n:added)init.instructions.remove(n);init.instructions.set(size4,size);install.instructions.set(guard4,guard);digest.cst=oldSha;bytes.cst=oldBytes;
  need(Arrays.equals(write(read(original)),write(node)),"Installer changed beyond GPU hash/length, fourth pinned row and exact row-count guard");Files.write(Path.of(a[3]),output);
  System.out.println("PASS GPU/core installer inverse; existing CPU rows and transactional safeguards retained; existing GPU row updated without duplicate; core sha256="+sha(core)+" bytes="+core.length);
 }
}
