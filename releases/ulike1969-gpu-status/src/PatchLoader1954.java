import java.nio.file.*;
import java.security.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

/** Replace only an existing finishing row fingerprint, preserving its transaction. */
public final class PatchLoader1954 {
 static final String OWNER="app/hiro/ulike/patches/IntegrationPayload186";
 static void need(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 static ClassNode read(byte[] b){ClassNode n=new ClassNode(Opcodes.ASM8);new ClassReader(b).accept(n,0);return n;}
 static byte[] write(ClassNode n){ClassWriter w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
 static String sha(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 public static void main(String[] a)throws Exception{
  need(a.length==3,"ORIGINAL_CLASS FINISH_SO OUTPUT_CLASS");
  byte[] original=Files.readAllBytes(Path.of(a[0])),lib=Files.readAllBytes(Path.of(a[1]));
  need(lib.length>=64&&lib[0]==127&&lib[1]=='E'&&lib[2]=='L'&&lib[3]=='F'&&lib[4]==2&&lib[5]==1&&lib[18]==(byte)183&&lib[19]==0,"ELF64 AArch64 required");
  ClassNode node=read(original);need(node.name.equals(OWNER),"Pinned installer owner required");
  MethodNode init=null;for(MethodNode m:node.methods)if(m.name.equals("<clinit>"))init=m;
  need(init!=null,"Installer initializer required");
  AbstractInsnNode first=init.instructions.getFirst();while(first.getOpcode()<0)first=first.getNext();
  need(first instanceof IntInsnNode&&first.getOpcode()==Opcodes.BIPUSH&&((IntInsnNode)first).operand==8,"Exact .53 eight-row allocation required");
  var constants=new ArrayList<LdcInsnNode>();for(AbstractInsnNode n:init.instructions.toArray())if(n instanceof LdcInsnNode l)constants.add(l);
  need(constants.size()==32,"Exactly eight original four-column rows required");
  int row=-1;for(int i=0;i<constants.size();i+=4)if("lib/arm64-v8a/libulike_finish1953.so".equals(constants.get(i).cst)){need(row<0,"Unique existing finishing row required");row=i;}
  need(row==28&&"ulike1953/runtime/libulike_finish1953.so".equals(constants.get(row+3).cst),"Exact final existing finishing row required");
  LdcInsnNode digest=constants.get(row+1),size=constants.get(row+2);Object oldDigest=digest.cst,oldSize=size.cst;
  need(oldDigest instanceof String&&((String)oldDigest).matches("[a-f0-9]{64}")&&oldSize instanceof String&&((String)oldSize).matches("[1-9][0-9]*"),"Original row fingerprint required");
  digest.cst=sha(lib);size.cst=Integer.toString(lib.length);byte[] output=write(node);
  digest.cst=oldDigest;size.cst=oldSize;
  need(Arrays.equals(write(read(original)),write(node)),"Installer changed beyond existing finishing digest/size");
  Files.write(Path.of(a[2]),output);
  System.out.println("PASS existing eight-row installer update inverse; seven other rows, row count and transaction guard retained");
 }
}
