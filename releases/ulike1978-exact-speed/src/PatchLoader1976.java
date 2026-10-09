import java.nio.file.*;
import java.security.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

/** Update exactly three existing native fingerprints; preserve twelve-row installer transaction. */
public final class PatchLoader1976 {
 static final String OWNER="app/hiro/ulike/patches/IntegrationPayload186";
 static final String[][] ROWS={{"lib/arm64-v8a/libulike_gpu1960.so","ulike1960/runtime/libulike_gpu1960.so"},{"lib/arm64-v8a/libulike_moire1951.so","ulike1951/runtime/libulike_moire1951.so"},{"lib/arm64-v8a/libulike_nr1955.so","ulike1955/runtime/libulike_nr1955.so"}};
 static void need(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 static ClassNode read(byte[] b){ClassNode n=new ClassNode(Opcodes.ASM8);new ClassReader(b).accept(n,0);return n;}
 static byte[] write(ClassNode n){ClassWriter w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
 static String sha(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 public static void main(String[] a)throws Exception{
  need(a.length==5,"ORIGINAL_CLASS GPU_SO MOIRE_SO NR_SO OUTPUT_CLASS");byte[] original=Files.readAllBytes(Path.of(a[0]));
  ClassNode node=read(original);need(node.name.equals(OWNER),"Pinned installer owner");MethodNode init=null;for(MethodNode m:node.methods)if(m.name.equals("<clinit>"))init=m;
  need(init!=null,"Installer initializer");AbstractInsnNode size=init.instructions.getFirst();while(size.getOpcode()<0)size=size.getNext();need(size instanceof IntInsnNode s&&s.getOpcode()==Opcodes.BIPUSH&&s.operand==12,"Exact twelve-row allocation retained");
  List<LdcInsnNode> literals=new ArrayList<>();for(AbstractInsnNode n:init.instructions.toArray())if(n instanceof LdcInsnNode ldc&&ldc.cst instanceof String)literals.add(ldc);
  Map<LdcInsnNode,Object> prior=new LinkedHashMap<>();
  for(int row=0;row<ROWS.length;row++){
   byte[] lib=Files.readAllBytes(Path.of(a[row+1]));need(lib.length>=64&&lib[0]==127&&lib[1]=='E'&&lib[2]=='L'&&lib[3]=='F'&&lib[4]==2&&lib[5]==1&&lib[18]==(byte)183&&lib[19]==0,"ELF64 AArch64 required");
   LdcInsnNode digest=null,bytes=null;int hits=0;
   for(int i=0;i+3<literals.size();i++)if(ROWS[row][0].equals(literals.get(i).cst)){need(ROWS[row][1].equals(literals.get(i+3).cst),"Existing native path retained");digest=literals.get(i+1);bytes=literals.get(i+2);hits++;}
   need(hits==1&&((String)digest.cst).matches("[a-f0-9]{64}")&&((String)bytes.cst).matches("[1-9][0-9]*"),"Unique existing native fingerprint row");
   prior.put(digest,digest.cst);prior.put(bytes,bytes.cst);digest.cst=sha(lib);bytes.cst=Integer.toString(lib.length);
  }
  byte[] result=write(node);for(var entry:prior.entrySet())entry.getKey().cst=entry.getValue();
  need(Arrays.equals(write(read(original)),write(node)),"Installer changed beyond three existing native digest/byte counts");Files.write(Path.of(a[4]),result);
  System.out.println("PASS 12-row installer inverse; all nine other rows and transactional safeguards retained; exactly three existing native rows updated");
 }
}
