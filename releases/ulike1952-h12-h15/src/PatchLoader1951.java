import java.nio.file.*;
import java.security.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

/** Update two pinned rows and append two libraries to the existing transactional installer. */
public final class PatchLoader1951 {
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
  need(a.length==6,"ORIGINAL_CLASS GPU_SO CORE_SO MOIRE_SO H8_SO OUTPUT_CLASS");
  byte[] original=Files.readAllBytes(Path.of(a[0]));byte[][] libs=new byte[4][];for(int i=0;i<4;i++){libs[i]=Files.readAllBytes(Path.of(a[i+1]));elf(libs[i]);}
  ClassNode node=read(original);need(node.name.equals(OWNER),"Pinned installer owner required");
  MethodNode init=null,install=null;for(MethodNode m:node.methods){if(m.name.equals("<clinit>"))init=m;if(m.name.equals("install"))install=m;}
  need(init!=null&&install!=null,"Installer methods required");
  AbstractInsnNode size=init.instructions.getFirst();while(size.getOpcode()<0)size=size.getNext();need(size.getOpcode()==Opcodes.ICONST_4&&size.getNext() instanceof TypeInsnNode,"Exact .50 four-row allocation required");
  FieldInsnNode store=null;for(AbstractInsnNode n:init.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.PUTSTATIC&&f.owner.equals(OWNER)&&f.name.equals("ITEMS")){need(store==null,"One ITEMS store required");store=f;}
  need(store!=null,"ITEMS store required");AbstractInsnNode guard=null;
  for(AbstractInsnNode n:install.instructions.toArray())if(n.getOpcode()==Opcodes.IF_ICMPEQ){AbstractInsnNode p=previous(n),q=previous(p),r=previous(q);if(p.getOpcode()==Opcodes.ICONST_4&&q.getOpcode()==Opcodes.ARRAYLENGTH&&r instanceof FieldInsnNode f&&f.name.equals("ITEMS")){need(guard==null,"One exact size guard required");guard=p;}}
  need(guard!=null,"Original installer size guard required");
  String[] names={"gpu1949","core1950"};LdcInsnNode[] digests=new LdcInsnNode[2],lengths=new LdcInsnNode[2];
  for(int i=0;i<2;i++){
   String oldSha=System.getProperty("ulike."+(i==0?"gpu":"core")+".oldsha"),oldBytes=System.getProperty("ulike."+(i==0?"gpu":"core")+".oldbytes");
   need(oldSha!=null&&oldBytes!=null,"Pinned old checksum and length required");int targets=0;
   for(AbstractInsnNode n:init.instructions.toArray())if(n instanceof LdcInsnNode x){
    if(("lib/arm64-v8a/libulike_"+names[i]+".so").equals(x.cst))targets++;
    if(oldSha.equals(x.cst)){need(digests[i]==null,"Unique old checksum");digests[i]=x;}
    if(oldBytes.equals(x.cst)){need(lengths[i]==null,"Unique old length");lengths[i]=x;}
   }
   need(targets==1&&digests[i]!=null&&lengths[i]!=null,"Exactly one existing integrity-pinned row for "+names[i]);
   digests[i].cst=sha(libs[i]);lengths[i].cst=Integer.toString(libs[i].length);
  }
  var moire=row(4,values("moire1951","ulike1951",libs[2]));var h8=row(5,values("h8gpu1951","ulike1951",libs[3]));
  AbstractInsnNode[] insertedMoire=moire.toArray(),insertedH8=h8.toArray();
  AbstractInsnNode size6=new IntInsnNode(Opcodes.BIPUSH,6),guard6=new IntInsnNode(Opcodes.BIPUSH,6);
  init.instructions.set(size,size6);install.instructions.set(guard,guard6);init.instructions.insertBefore(store,moire);init.instructions.insertBefore(store,h8);
  byte[] output=write(node);
  for(AbstractInsnNode n:insertedMoire)init.instructions.remove(n);for(AbstractInsnNode n:insertedH8)init.instructions.remove(n);
  init.instructions.set(size6,size);install.instructions.set(guard6,guard);
  for(int i=0;i<2;i++){digests[i].cst=System.getProperty("ulike."+(i==0?"gpu":"core")+".oldsha");lengths[i].cst=System.getProperty("ulike."+(i==0?"gpu":"core")+".oldbytes");}
  need(Arrays.equals(write(read(original)),write(node)),"Installer changed beyond hashes, lengths, two pinned rows and row-count guard");
  Files.write(Path.of(a[5]),output);System.out.println("PASS 6-row installer inverse; original two CPU rows and transactional safeguards retained");
 }
}
