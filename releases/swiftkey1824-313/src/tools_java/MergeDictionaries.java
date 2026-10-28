import java.io.File;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.*;

/** Replace three reviewed implementations; preserve all published method/class metadata. */
public final class MergeDictionaries {
  static final String WORD="Lhiro/swiftkey/WordCandidates;", ENGLISH="Lhiro/swiftkey/KatakanaEnglishCandidates;";
  static final String BASE_SHA="dea2051af47cd8e2106437dbf7b3bc4f9ac2e6d496a334462700d7d2584e405a";
  static final String ROMAJI="isRomaji(Ljava/lang/String;)Z";
  static final String ADD="addWords(Ljava/util/LinkedHashSet;[Ljava/lang/String;Ljava/util/Set;)V";
  static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
  static String sha(String path)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(path))));}
  static TreeMap<String,ClassDef> load(String path)throws Exception{
    TreeMap<String,ClassDef> result=new TreeMap<>();
    for(ClassDef c:DexFileFactory.loadDexFile(new File(path),Opcodes.forApi(26)).getClasses())require(result.put(c.getType(),c)==null,"Duplicate class");
    return result;
  }
  static String signature(MethodReference m){StringBuilder b=new StringBuilder(m.getName()).append('(');for(CharSequence p:m.getParameterTypes())b.append(p);return b.append(')').append(m.getReturnType()).toString();}
  static Method find(ClassDef c,String signature){for(Method m:c.getMethods())if(signature(m).equals(signature))return m;throw new IllegalStateException("Missing method "+signature);}
  static ClassDef withMethods(ClassDef c,Iterable<? extends Method> methods){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);}
  static byte[] canonical(ClassDef c)throws Exception{
    DexPool pool=new DexPool(Opcodes.forApi(26));pool.internClass(ImmutableClassDef.of(c));MemoryDataStore out=new MemoryDataStore();
    try{pool.writeTo(out);return Arrays.copyOf(out.getBuffer(),out.getSize());}finally{out.close();}
  }
  static byte[] methodBytes(ClassDef owner,Method method)throws Exception{return canonical(withMethods(owner,Arrays.asList(method)));}
  static Method implementation(Method old,Method replacement){
    require(signature(old).equals(signature(replacement)),"Method signatures differ");
    require(old.getAccessFlags()==replacement.getAccessFlags()&&old.getAccessFlags()==10,"Expected private static method");
    require(replacement.getImplementation()!=null,"No compiled implementation");
    return new ImmutableMethod(old.getDefiningClass(),old.getName(),old.getParameters(),old.getReturnType(),old.getAccessFlags(),old.getAnnotations(),old.getHiddenApiRestrictions(),replacement.getImplementation());
  }
  static ClassDef replace(ClassDef old,Map<String,Method> selected){
    List<Method> methods=new ArrayList<>();Set<String> applied=new TreeSet<>();
    for(Method m:old.getMethods()){
      String key=signature(m);Method replacement=selected.get(key);
      methods.add(replacement==null?m:implementation(m,replacement));if(replacement!=null)applied.add(key);
    }
    require(applied.equals(selected.keySet()),"Replacement selection incomplete");return withMethods(old,methods);
  }
  static void noSelfReferences(Method method){
    require(method.getImplementation().getTryBlocks().isEmpty(),"isRomaji must not introduce catch behavior");
    for(Instruction i:method.getImplementation().getInstructions())if(i instanceof ReferenceInstruction){
      Reference ref=((ReferenceInstruction)i).getReference();
      if(ref instanceof MethodReference)require(((MethodReference)ref).getDefiningClass().equals("Ljava/lang/String;"),"Unexpected isRomaji method reference");
      else throw new IllegalStateException("Unexpected isRomaji field/type reference");
    }
  }
  static String json(Object v){
    if(v==null)return "null";if(v instanceof Number||v instanceof Boolean)return v.toString();
    if(v instanceof Map){StringJoiner s=new StringJoiner(",","{","}");for(Map.Entry<?,?> e:((Map<?,?>)v).entrySet())s.add(json(e.getKey())+":"+json(e.getValue()));return s.toString();}
    if(v instanceof Iterable){StringJoiner s=new StringJoiner(",","[","]");for(Object e:(Iterable<?>)v)s.add(json(e));return s.toString();}
    return "\""+v.toString().replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";
  }
  public static void main(String[] args)throws Exception{
    require(args.length==4,"baseline.mpe fixed-word.dex output.mpe report.json");
    require(sha(args[0]).equals(BASE_SHA),"Published runtime pin differs");
    TreeMap<String,ClassDef> before=load(args[0]),overlay=load(args[1]);
    require(before.size()==91,"Published runtime class count differs");
    require(overlay.keySet().equals(new TreeSet<>(Arrays.asList(WORD,"Lhiro/swiftkey/WordCandidates$Index;","Lhiro/swiftkey/WordCandidates$1;"))),"Unexpected compiled word class inventory");
    Method romaji=find(overlay.get(WORD),ROMAJI),add=find(overlay.get(WORD),ADD);noSelfReferences(romaji);
    TreeMap<String,ClassDef> merged=new TreeMap<>(before);
    merged.put(WORD,replace(before.get(WORD),new TreeMap<>(Map.of(ROMAJI,romaji,ADD,add))));
    merged.put(ENGLISH,replace(before.get(ENGLISH),new TreeMap<>(Map.of(ROMAJI,romaji))));
    DexPool pool=new DexPool(Opcodes.forApi(26));for(ClassDef c:merged.values())pool.internClass(c);
    FileDataStore store=new FileDataStore(new File(args[2]));try{pool.writeTo(store);}finally{store.close();}
    TreeMap<String,ClassDef> after=load(args[2]);require(after.keySet().equals(before.keySet()),"Runtime class inventory changed");
    List<String> changedMethods=new ArrayList<>();
    for(ClassDef original:before.values()){
      ClassDef result=after.get(original.getType());List<Method> restored=new ArrayList<>();
      Set<String> oldKeys=new TreeSet<>(),newKeys=new TreeSet<>();for(Method m:result.getMethods())newKeys.add(signature(m));
      for(Method old:original.getMethods()){
        String sig=signature(old);oldKeys.add(sig);Method actual=find(result,sig);
        boolean changed=!Arrays.equals(methodBytes(original,old),methodBytes(original,actual));
        boolean expected=(original.getType().equals(WORD)&&(sig.equals(ROMAJI)||sig.equals(ADD)))||(original.getType().equals(ENGLISH)&&sig.equals(ROMAJI));
        require(changed==expected,"Unexpected method delta "+original.getType()+"->"+sig);
        if(changed){
          changedMethods.add(original.getType()+"->"+sig);
          Method compiled=sig.equals(ROMAJI)?romaji:add;
          require(Arrays.equals(methodBytes(original,actual),methodBytes(original,implementation(old,compiled))),"Merged implementation differs from compiled method");
        }
        restored.add(changed?old:actual);
      }
      require(oldKeys.equals(newKeys),"Method inventory changed");
      require(Arrays.equals(canonical(original),canonical(withMethods(result,restored))),"Original class/method metadata not preserved: "+original.getType());
    }
    Collections.sort(changedMethods);require(changedMethods.size()==3,"Expected exactly three changed implementations");
    Map<String,Object> report=new LinkedHashMap<>();
    report.put("status","PASSED");report.put("baseline_extension_sha256",sha(args[0]));report.put("compiled_word_sha256",sha(args[1]));report.put("runtime_sha256",sha(args[2]));
    report.put("baseline_classes",91);report.put("runtime_after_classes",91);report.put("changed_methods",changedMethods);
    report.put("all_other_method_bodies_preserved",true);report.put("all_class_and_method_metadata_preserved",true);report.put("compiled_implementations_preserved",true);report.put("romaji_no_self_references",true);
    Files.writeString(Path.of(args[3]),json(report)+"\n");System.out.println("PASS: exactly three implementations changed; 91 runtime classes and all original metadata retained");
  }
}
