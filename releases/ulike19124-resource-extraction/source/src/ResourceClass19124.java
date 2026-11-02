import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

/** JVM counterpart of the exact three DEX resource-read changes. */
public final class ResourceClass19124 {
 static final String LOADER="app/hiro/ulike/patches/UlikeHqMaxPatch";
 static final String INSTALLER="app/hiro/ulike/patches/IntegrationPayload186";
 static final String CONTEXT="app/morphe/patcher/patch/ResourcePatchContext";
 static final String OLD_DESCRIPTION="v1.9.123（v1.9.122基準）";
 static final String DESCRIPTION="v1.9.124（v1.9.123基準）Morpheの必要時展開に対応し、元の素材・ネイティブファイルを検査前に取得。素材ハッシュ・衝突検査と撮影時CPUブースト抑止、画質・解像度・保存経路を維持。実機の操作改善は未確認。";
 static final Map<String,String> BASE_HASH=Map.of(LOADER,"f1f31bc3bd3bb8266bb59b1156cd2fa42229ae5cfe2e7dcda6bccf0b51195efa",INSTALLER,"1d5196c551bf086c9d62376e661641816a2a2fb6e2a1cd60b6e5f5bea7ab4ab1");
 static void req(boolean v,String m){if(!v)throw new IllegalStateException(m);}
 static ClassNode read(byte[] raw){var n=new ClassNode(Opcodes.ASM8);new ClassReader(raw).accept(n,0);return n;}
 static byte[] write(ClassNode n){var w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
 static String sha(byte[] raw)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));}
 static AbstractInsnNode previous(AbstractInsnNode ins){for(var p=ins.getPrevious();p!=null;p=p.getPrevious())if(p.getOpcode()>=0)return p;throw new IllegalStateException("Boolean operand absent");}
 static String originalDescription(ClassNode n){String result=null;int hits=0;
  for(MethodNode m:n.methods)for(AbstractInsnNode ins:m.instructions.toArray())if(ins instanceof LdcInsnNode l&&l.cst instanceof String s&&s.startsWith(OLD_DESCRIPTION)){result=s;hits++;}
  req(n.name.equals(LOADER)?hits==1:hits==0,"Pinned class description inventory differs");return result;
 }
 static int transform(ClassNode n,boolean inverse,String original){
  req(n.name.equals(LOADER)||n.name.equals(INSTALLER),"Unexpected JVM patch class");int edits=0,gets=0,metadata=0;
  for(MethodNode m:n.methods)for(AbstractInsnNode ins:m.instructions.toArray()){
   if(ins instanceof MethodInsnNode call&&call.owner.equals(CONTEXT)&&call.name.equals("get")&&call.desc.equals("(Ljava/lang/String;Z)Ljava/io/File;")){
    gets++;boolean read=(n.name.equals(LOADER)&&m.name.equals("applyAssets")&&m.desc.equals("(L"+CONTEXT+";)V"))||
       (n.name.equals(INSTALLER)&&m.name.equals("lambda$install$0")&&m.desc.equals("(L"+CONTEXT+";Ljava/lang/String;)Ljava/io/File;"));
    var literal=previous(call);req(call.getOpcode()==Opcodes.INVOKEVIRTUAL,"Resource get opcode differs");
    if(read){req(literal.getOpcode()==(inverse?Opcodes.ICONST_1:Opcodes.ICONST_0),"Pinned original-file copy operand differs");m.instructions.set(literal,new InsnNode(inverse?Opcodes.ICONST_0:Opcodes.ICONST_1));edits++;}
    else req(n.name.equals(INSTALLER)&&m.name.equals("install")&&literal.getOpcode()==Opcodes.ICONST_0,"Manifest parent lookup must remain unchanged");
   }
   if(ins instanceof LdcInsnNode l&&l.cst instanceof String s&&(inverse?s.equals(DESCRIPTION):original!=null&&s.equals(original))){l.cst=inverse?original:DESCRIPTION;metadata++;}
  }
  req(gets==2&&edits==(n.name.equals(LOADER)?2:1),"Exactly reviewed resource calls required");
  req(metadata==(n.name.equals(LOADER)?1:0),"Exactly reviewed metadata change required");return edits;
 }
 static byte[] patch(byte[] before)throws Exception{
  var n=read(before);req(BASE_HASH.get(n.name).equals(sha(before)),"Published .123 JVM class differs");String original=originalDescription(n);
  transform(n,false,original);byte[] after=write(n);verify(before,after);return after;
 }
 static String verify(byte[] before,byte[] after)throws Exception{
  var old=read(before);req(BASE_HASH.get(old.name).equals(sha(before)),"Pinned base class differs");var n=read(after);req(old.name.equals(n.name),"JVM class identity differs");
  int count=transform(n,true,originalDescription(old));req(Arrays.equals(write(old),write(n)),"JVM inverse changes code, guard, control flow or class metadata");
  return "{\"schema\":\"ulike19124-resource-read-jvm-v1\",\"status\":\"passed\",\"class\":\""+old.name+"\",\"read_requests_enabled\":"+count+",\"manifest_parent_lookup_preserved\":true,\"exact_inverse_verified\":true,\"jvm_inverse_verified\":true,\"guard_instructions_preserved\":true}";
 }
 static String verify(Path before,Path after)throws Exception{return verify(Files.readAllBytes(before),Files.readAllBytes(after));}
 public static void main(String[] a)throws Exception{
  req(a.length==3||a.length==4,"patch BASE_CLASS OUTPUT_CLASS or verify BASE_CLASS NEXT_CLASS REPORT_JSON");
  if(a[0].equals("patch")&&a.length==3){var before=Files.readAllBytes(Path.of(a[1]));var after=patch(before);Files.write(Path.of(a[2]),after);System.out.println(verify(before,after));}
  else{req(a[0].equals("verify")&&a.length==4,"Unknown operation");String r=verify(Path.of(a[1]),Path.of(a[2]));Files.writeString(Path.of(a[3]),r+"\n");System.out.println(r);}
 }
}
