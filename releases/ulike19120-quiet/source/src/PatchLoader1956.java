import java.nio.file.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
/** Replace only five existing library fingerprints; inverse proves original safeguards. */
public final class PatchLoader1956 {
 static final String OWNER="app/hiro/ulike/patches/IntegrationPayload186";
 static void need(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 static ClassNode read(byte[] b){ClassNode n=new ClassNode(Opcodes.ASM8);new ClassReader(b).accept(n,0);return n;}
 static byte[] write(ClassNode n){ClassWriter w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
 public static void main(String[] a)throws Exception{
  need(a.length==3,"ORIGINAL_CLASS NATIVE_ROWS_TSV OUTPUT_CLASS");
  byte[] original=Files.readAllBytes(Path.of(a[0]));ClassNode node=read(original);need(node.name.equals(OWNER),"Exact installer owner required");
  Map<String,String[]> rows=new TreeMap<>();for(String line:Files.readAllLines(Path.of(a[1]))){String[] r=line.split("\t");need(r.length==5&&rows.put(r[0],Arrays.copyOfRange(r,1,5))==null,"Unique native TSV row");}need(rows.size()==5,"Five reviewed native updates");
  MethodNode init=null;for(MethodNode m:node.methods)if(m.name.equals("<clinit>"))init=m;need(init!=null,"Installer initializer required");
  AbstractInsnNode first=init.instructions.getFirst();while(first.getOpcode()<0)first=first.getNext();need(first instanceof IntInsnNode&&first.getOpcode()==Opcodes.BIPUSH&&((IntInsnNode)first).operand==9,"Exact .55 nine-row allocation");
  var constants=new ArrayList<LdcInsnNode>();for(AbstractInsnNode n:init.instructions.toArray())if(n instanceof LdcInsnNode l)constants.add(l);need(constants.size()==36,"Nine original four-column rows");
  Map<LdcInsnNode,Object> inverse=new LinkedHashMap<>();Set<String> seen=new TreeSet<>();
  for(int k=0;k<constants.size();k+=4){Object resource=constants.get(k+3).cst;String[] r=rows.get(resource);if(r==null)continue;need(seen.add((String)resource),"Unique installer resource");for(int j=1;j<=2;j++){LdcInsnNode c=constants.get(k+j);need(c.cst.equals(r[j-1]),"Pinned original native digest/size");inverse.put(c,c.cst);c.cst=r[j+1];}}
  need(seen.equals(rows.keySet())&&inverse.size()==10,"Exactly five native rows updated");byte[] output=write(node);for(var r:inverse.entrySet())r.getKey().cst=r.getValue();need(Arrays.equals(write(read(original)),write(node)),"Installer inverse: no other changes");Files.write(Path.of(a[2]),output);System.out.println("PASS all nine existing rows; five fingerprints updated; inverse preserves row count and transaction safeguards");
 }
}
