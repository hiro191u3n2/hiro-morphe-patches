import java.nio.file.*;
import java.security.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

/** Update the fingerprint of the existing GPU row, retaining twelve rows. */
public final class PatchLoader1962 {
 static final String OWNER="app/hiro/ulike/patches/IntegrationPayload186";
 static void need(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 static ClassNode read(byte[] b){ClassNode n=new ClassNode(Opcodes.ASM8);new ClassReader(b).accept(n,0);return n;}
 static byte[] write(ClassNode n){ClassWriter w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
 static String sha(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 public static void main(String[] a)throws Exception{
  need(a.length==3,"ORIGINAL_CLASS NR_SO OUTPUT_CLASS");byte[] original=Files.readAllBytes(Path.of(a[0])),lib=Files.readAllBytes(Path.of(a[1]));
  need(lib.length>=64&&lib[0]==127&&lib[1]=='E'&&lib[2]=='L'&&lib[3]=='F'&&lib[4]==2&&lib[5]==1&&lib[18]==(byte)183&&lib[19]==0,"ELF64 AArch64 required");
  ClassNode node=read(original);need(node.name.equals(OWNER),"Pinned installer owner");MethodNode init=null;for(MethodNode m:node.methods)if(m.name.equals("<clinit>"))init=m;
  need(init!=null,"Installer initializer");AbstractInsnNode size=init.instructions.getFirst();while(size.getOpcode()<0)size=size.getNext();need(size instanceof IntInsnNode s&&s.getOpcode()==Opcodes.BIPUSH&&s.operand==12,"Exact twelve-row allocation retained");
  List<LdcInsnNode> literals=new ArrayList<>();for(AbstractInsnNode n:init.instructions.toArray())if(n instanceof LdcInsnNode ldc&&ldc.cst instanceof String)literals.add(ldc);
  LdcInsnNode digest=null,bytes=null;int hits=0;
  for(int i=0;i+3<literals.size();i++)if("lib/arm64-v8a/libulike_gpu1960.so".equals(literals.get(i).cst)){
   need("ulike1960/runtime/libulike_gpu1960.so".equals(literals.get(i+3).cst),"Existing GPU path retained");digest=literals.get(i+1);bytes=literals.get(i+2);hits++;
  }
  need(hits==1&&((String)digest.cst).matches("[a-f0-9]{64}")&&((String)bytes.cst).matches("[1-9][0-9]*"),"Unique existing GPU fingerprint row");
  Object oldDigest=digest.cst,oldBytes=bytes.cst;digest.cst=sha(lib);bytes.cst=Integer.toString(lib.length);byte[] result=write(node);digest.cst=oldDigest;bytes.cst=oldBytes;
  need(Arrays.equals(write(read(original)),write(node)),"Installer changed beyond existing GPU digest/byte count");Files.write(Path.of(a[2]),result);
  System.out.println("PASS 12-row installer inverse; all eleven other existing rows and transactional safeguards retained; existing GPU row updated");
 }
}
