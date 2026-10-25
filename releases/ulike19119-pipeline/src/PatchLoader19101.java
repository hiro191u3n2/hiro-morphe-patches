import java.nio.file.*;
import java.security.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

/** Append two independent candidate rows and update only the exact cardinality guard. */
public final class PatchLoader19101 {
 static final String OWNER="app/hiro/ulike/patches/IntegrationPayload186";
 static final String[][] ROWS={{"lib/arm64-v8a/libulike_gpu1960.so","ulike1960/runtime/libulike_gpu1960.so"},{"lib/arm64-v8a/libulike_nr1955.so","ulike1955/runtime/libulike_nr1955.so"},{"lib/arm64-v8a/libulike_smooth1958.so","ulike1958/runtime/libulike_smooth1958.so"}};
 static void need(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 static ClassNode read(byte[] b){ClassNode n=new ClassNode(Opcodes.ASM8);new ClassReader(b).accept(n,0);return n;}
 static byte[] write(ClassNode n){ClassWriter w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
 static String sha(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 public static void main(String[] a)throws Exception{
  need(a.length==7,"ORIGINAL_CLASS GPU_SO NR_SO SMOOTH_SO GPU119_SO FRONT119_SO OUTPUT_CLASS");byte[] original=Files.readAllBytes(Path.of(a[0]));
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
  
  String[][] additions={{"lib/arm64-v8a/libulike_gpu19119.so","ulike19119/runtime/libulike_gpu19119.so"},{"lib/arm64-v8a/libulike_front19119.so","ulike19119/runtime/libulike_front19119.so"}};
  AbstractInsnNode insert=null;for(AbstractInsnNode x:init.instructions.toArray())if(x instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.PUTSTATIC&&f.name.equals("ITEMS")){need(insert==null,"Unique ITEMS publication");insert=x;}
  need(insert!=null,"ITEMS publication required");List<AbstractInsnNode> added=new ArrayList<>();
  ((IntInsnNode)size).operand=14;
  for(int row=0;row<2;row++){
   byte[] lib=Files.readAllBytes(Path.of(a[row+4]));need(lib.length>=64&&lib[0]==127&&lib[1]=='E'&&lib[2]=='L'&&lib[3]=='F'&&lib[4]==2&&lib[5]==1&&lib[18]==(byte)183&&lib[19]==0,"Candidate ELF64 AArch64 required");
   InsnList extra=new InsnList();extra.add(new InsnNode(Opcodes.DUP));extra.add(new IntInsnNode(Opcodes.BIPUSH,12+row));extra.add(new InsnNode(Opcodes.ICONST_4));extra.add(new TypeInsnNode(Opcodes.ANEWARRAY,"java/lang/String"));
   String[] values={additions[row][0],sha(lib),Integer.toString(lib.length),additions[row][1]};
   for(int k=0;k<4;k++){extra.add(new InsnNode(Opcodes.DUP));extra.add(new InsnNode(Opcodes.ICONST_0+k));extra.add(new LdcInsnNode(values[k]));extra.add(new InsnNode(Opcodes.AASTORE));}
   extra.add(new InsnNode(Opcodes.AASTORE));added.addAll(Arrays.asList(extra.toArray()));init.instructions.insertBefore(insert,extra);
  }
  IntInsnNode countGuard=null;
  for(MethodNode m:node.methods)if(m.name.equals("install")){
   var code=new ArrayList<AbstractInsnNode>();for(AbstractInsnNode x:m.instructions.toArray())if(x.getOpcode()>=0)code.add(x);
   for(int i=2;i+1<code.size();i++)if(code.get(i) instanceof IntInsnNode x&&x.getOpcode()==Opcodes.BIPUSH&&x.operand==12&&code.get(i-1).getOpcode()==Opcodes.ARRAYLENGTH&&code.get(i-2) instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.GETSTATIC&&f.owner.equals(OWNER)&&f.name.equals("ITEMS")&&code.get(i+1).getOpcode()==Opcodes.IF_ICMPEQ){need(countGuard==null,"Unique exact installer count guard");countGuard=x;}
  }
  need(countGuard!=null,"Exact installer count guard required");countGuard.operand=14;
  byte[] result=write(node);countGuard.operand=12;for(AbstractInsnNode x:added)init.instructions.remove(x);((IntInsnNode)size).operand=12;
  for(var entry:prior.entrySet())entry.getKey().cst=entry.getValue();
  need(Arrays.equals(write(read(original)),write(node)),"Installer changed beyond the reviewed native rows and exact count guard");Files.write(Path.of(a[6]),result);
  System.out.println("PASS 14-row installer inverse; all twelve legacy rows and transactional safeguards retained; two independently fingerprinted candidate libraries added");
 }
}
