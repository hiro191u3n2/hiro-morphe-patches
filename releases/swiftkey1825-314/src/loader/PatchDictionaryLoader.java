import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.*;

/** Rewrite only dictionary hash constants and the existing bounded resource limit. */
public final class PatchDictionaryLoader {
 static final String TYPE="Lapp/hiro/swiftkey/patches/JapaneseSamsungEmojiPatch;";
 static final String WORD="1f15766663f5cbf1de8bce2df6a2b8f5e80a488166950979b92ecac8f9e27ca0";
 static final String LICENSE="cab6e0d7ddc41d32e6ea7a7b6d4629367a7d1dfd7744aa14bd8b87bf53b89fca";
 static final int OLD_LIMIT=33554432,NEW_LIMIT=201326592;
 static void require(boolean ok,String m){if(!ok)throw new IllegalStateException(m);}
 static String sha(byte[] x)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(x));}
 static String sig(MethodReference m){StringBuilder b=new StringBuilder(m.getName()).append('(');for(CharSequence p:m.getParameterTypes())b.append(p);return b.append(')').append(m.getReturnType()).toString();}
 static Map<String,ClassDef> load(String path)throws Exception{Map<String,ClassDef> out=new TreeMap<>();for(ClassDef c:DexFileFactory.loadDexFile(new File(path),Opcodes.forApi(26)).getClasses())require(out.put(c.getType(),c)==null,"duplicate class");return out;}
 static ClassDef cls(ClassDef c,Iterable<? extends Method> methods){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);}
 static byte[] canonical(ClassDef c)throws Exception{DexPool p=new DexPool(Opcodes.forApi(26));p.internClass(c);MemoryDataStore out=new MemoryDataStore();try{p.writeTo(out);return Arrays.copyOf(out.getBuffer(),out.getSize());}finally{out.close();}}
 static byte[] methodBytes(ClassDef c,Method m)throws Exception{return canonical(cls(c,List.of(m)));}
 static String json(Object v){if(v==null)return "null";if(v instanceof Number||v instanceof Boolean)return v.toString();if(v instanceof Map){StringJoiner s=new StringJoiner(",","{","}");for(var e:((Map<?,?>)v).entrySet())s.add(json(e.getKey())+":"+json(e.getValue()));return s.toString();}if(v instanceof Iterable){StringJoiner s=new StringJoiner(",","[","]");for(Object x:(Iterable<?>)v)s.add(json(x));return s.toString();}return "\""+v.toString().replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";}
 static Method patch(Method m,Map<String,String> replacements,int[] counts){
  MethodImplementation impl=m.getImplementation();if(impl==null)return m;
  List<Instruction> code=new ArrayList<>();boolean modified=false;
  for(Instruction ins:impl.getInstructions()){
   Instruction result=ins;
   if(ins instanceof ReferenceInstruction && ((ReferenceInstruction)ins).getReference() instanceof StringReference){
    String old=((StringReference)((ReferenceInstruction)ins).getReference()).getString();
    if(replacements.containsKey(old)){
     require(m.getName().equals("<clinit>"),"hash outside static initializer");
     int reg=((OneRegisterInstruction)ins).getRegisterA();
     require(ins.getOpcode()==Opcode.CONST_STRING || ins.getOpcode()==Opcode.CONST_STRING_JUMBO,"unexpected hash opcode");
     result=ins.getOpcode()==Opcode.CONST_STRING?new ImmutableInstruction21c(Opcode.CONST_STRING,reg,new ImmutableStringReference(replacements.get(old))):new ImmutableInstruction31c(Opcode.CONST_STRING_JUMBO,reg,new ImmutableStringReference(replacements.get(old)));
     counts[old.equals(WORD)?0:1]++;modified=true;
    }
   }
   if(ins instanceof NarrowLiteralInstruction && ((NarrowLiteralInstruction)ins).getNarrowLiteral()==OLD_LIMIT){
    require(sig(m).equals("readAsset(Ljava/io/InputStream;)[B"),"limit outside readAsset");
    int reg=((OneRegisterInstruction)ins).getRegisterA();
    require(ins.getOpcode()==Opcode.CONST_HIGH16 || ins.getOpcode()==Opcode.CONST,"unexpected size limit opcode");
    result=ins.getOpcode()==Opcode.CONST_HIGH16?new ImmutableInstruction21ih(Opcode.CONST_HIGH16,reg,NEW_LIMIT):new ImmutableInstruction31i(Opcode.CONST,reg,NEW_LIMIT);
    counts[2]++;modified=true;
   }
   require(ins.getCodeUnits()==result.getCodeUnits(),"instruction width changed");code.add(result);
  }
  if(!modified)return m;
  return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),new ImmutableMethodImplementation(impl.getRegisterCount(),code,impl.getTryBlocks(),impl.getDebugItems()));
 }
 public static void main(String[] a)throws Exception{
  require(a.length==5,"input.dex output.dex words-sha license-sha report.json");
  require(a[2].matches("[0-9a-f]{64}") && a[3].matches("[0-9a-f]{64}") && !a[2].equals(WORD) && !a[3].equals(LICENSE),"new hashes required");
  String base=sha(Files.readAllBytes(Path.of(a[0])));require(Set.of("a16e8eb8bebfc86d25aaec7411f71e519315cb4423e21a0cb514fefc7652a42d","90ac85e6978165882ebe076fa58487674e0c744b5661463be23a5352dc1979b3").contains(base),"baseline loader pin differs");
  Map<String,ClassDef> before=load(a[0]);require(before.containsKey(TYPE),"target class missing");int[] counts=new int[3];Map<String,ClassDef> updated=new TreeMap<>();
  for(ClassDef c:before.values()){
   if(!c.getType().equals(TYPE)){updated.put(c.getType(),c);continue;}
   List<Method> methods=new ArrayList<>();for(Method m:c.getMethods())methods.add(patch(m,Map.of(WORD,a[2],LICENSE,a[3]),counts));updated.put(c.getType(),cls(c,methods));
  }
  require(Arrays.equals(counts,new int[]{1,1,1}),"unexpected pin/limit replacement counts "+Arrays.toString(counts));
  DexPool pool=new DexPool(Opcodes.forApi(26));for(ClassDef c:updated.values())pool.internClass(c);FileDataStore dest=new FileDataStore(new File(a[1]));try{pool.writeTo(dest);}finally{dest.close();}
  Map<String,ClassDef> after=load(a[1]);require(before.keySet().equals(after.keySet()),"class inventory changed");Set<String> changedMethods=new TreeSet<>();
  for(ClassDef old:before.values()){
   ClassDef now=after.get(old.getType());
   if(!old.getType().equals(TYPE)){require(Arrays.equals(canonical(old),canonical(now)),"unrelated class changed "+old.getType());continue;}
   Map<String,Method> oldMethods=new TreeMap<>(),newMethods=new TreeMap<>();for(Method m:old.getMethods())oldMethods.put(sig(m),m);for(Method m:now.getMethods())newMethods.put(sig(m),m);require(oldMethods.keySet().equals(newMethods.keySet()),"member inventory changed");List<Method> restored=new ArrayList<>();
   for(String key:oldMethods.keySet()){
    Method o=oldMethods.get(key),n=newMethods.get(key);boolean different=!Arrays.equals(methodBytes(old,o),methodBytes(now,n));
    if(different){changedMethods.add(TYPE+"->"+key);require(Arrays.equals(methodBytes(now,n),methodBytes(old,patch(o,Map.of(WORD,a[2],LICENSE,a[3]),new int[3]))),"unreviewed method difference "+key);restored.add(o);}else restored.add(n);
   }
   require(Arrays.equals(canonical(old),canonical(cls(now,restored))),"target class metadata changed");
  }
  require(changedMethods.equals(Set.of(TYPE+"-><clinit>()V",TYPE+"->readAsset(Ljava/io/InputStream;)[B")),"unexpected changed methods");
  Map<String,Object> report=new LinkedHashMap<>();report.put("status","PASSED");report.put("baseline_sha256",base);report.put("android_sha256",sha(Files.readAllBytes(Path.of(a[1]))));report.put("classes",before.size());report.put("changed_class",TYPE);report.put("changed_methods",changedMethods);report.put("unrelated_android_classes_preserved",true);report.put("all_class_and_member_metadata_preserved",true);report.put("instruction_widths_preserved",true);report.put("only_two_hash_strings_and_one_limit_changed",true);report.put("words_sha256",a[2]);report.put("license_sha256",a[3]);report.put("old_limit",OLD_LIMIT);report.put("new_limit",NEW_LIMIT);Files.writeString(Path.of(a[4]),json(report)+"\n");System.out.println(json(report));
 }
}
