import java.io.File;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction12x;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.*;

/** Preserve the published runtime; replace only ranking and add the reviewed space helper. */
public final class Merge1828 {
  static final String BASE_SHA="0a5822f3cd2b78ef5550b48ba7fffa488e11cdd925d007ecf93d3216e9f06477";
  static final String WORD="Lhiro/swiftkey/WordCandidates;";
  static final String ENGLISH="Lhiro/swiftkey/KatakanaEnglishCandidates;";
  static final String ENHANCE="enhance(Ljava/util/List;Ll40/c;)Ljava/util/List;";
  static void require(boolean v,String m){if(!v)throw new IllegalStateException(m);}
  static boolean space(String t){return t.equals("Lhiro/swiftkey/SpaceConversion;")||t.startsWith("Lhiro/swiftkey/SpaceConversion$");}
  static boolean word(String t){return t.equals(WORD)||t.startsWith("Lhiro/swiftkey/WordCandidates$");}
  static String sha(String p)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(p))));}
  static Map<String,ClassDef> load(String p)throws Exception{Map<String,ClassDef> r=new TreeMap<>();for(ClassDef c:DexFileFactory.loadDexFile(new File(p),Opcodes.forApi(26)).getClasses())require(r.put(c.getType(),c)==null,"duplicate type");return r;}
  static String sig(MethodReference m){StringBuilder b=new StringBuilder(m.getName()).append('(');for(CharSequence p:m.getParameterTypes())b.append(p);return b.append(')').append(m.getReturnType()).toString();}
  static Method method(ClassDef c,String s){require(c!=null,"Missing method owner");for(Method m:c.getMethods())if(sig(m).equals(s))return m;throw new IllegalStateException("Missing "+s);}
  static ClassDef cls(ClassDef c,Iterable<? extends Method> m){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),m);}
  static byte[] canonical(ClassDef c)throws Exception{DexPool p=new DexPool(Opcodes.forApi(26));p.internClass(ImmutableClassDef.of(c));MemoryDataStore o=new MemoryDataStore();try{p.writeTo(o);return Arrays.copyOf(o.getBuffer(),o.getSize());}finally{o.close();}}
  static byte[] mb(ClassDef owner,Method m)throws Exception{return canonical(cls(owner,List.of(m)));}
  static Method transplant(Method o,Method n){require(sig(o).equals(sig(n))&&o.getAccessFlags()==n.getAccessFlags(),"Method contract changed");return new ImmutableMethod(o.getDefiningClass(),o.getName(),o.getParameters(),o.getReturnType(),o.getAccessFlags(),o.getAnnotations(),o.getHiddenApiRestrictions(),n.getImplementation());}
  static Method englishAfterNative(Method old){
    require(sig(old).equals(ENHANCE),"English enhance signature changed");
    MethodImplementation body=old.getImplementation();require(body!=null,"English enhance has no body");
    List<Instruction> code=new ArrayList<>();for(Instruction i:body.getInstructions())code.add(i);
    require(code.size()==102,"English enhance instruction inventory changed");
    int found=-1;
    for(int i=0;i<code.size();i++)if(code.get(i) instanceof ReferenceInstruction){
      Reference r=((ReferenceInstruction)code.get(i)).getReference();
      if(r instanceof MethodReference){MethodReference call=(MethodReference)r;
        if(call.getDefiningClass().equals("Ljava/lang/Math;")&&sig(call).equals("min(II)I")){
          require(found==-1&&i>=2&&code.get(i).getOpcode()==Opcode.INVOKE_STATIC,"Ambiguous English preserve bound");
          FiveRegisterInstruction invoke=(FiveRegisterInstruction)code.get(i);
          require(invoke.getRegisterCount()==2&&invoke.getRegisterC()==4&&invoke.getRegisterD()==3,"English min registers changed");
          Instruction bound=code.get(i-1),size=code.get(i-2);
          require(bound.getOpcode()==Opcode.CONST_4&&((OneRegisterInstruction)bound).getRegisterA()==4
                  &&((NarrowLiteralInstruction)bound).getNarrowLiteral()==2,"English old preserve constant changed");
          require(size.getOpcode()==Opcode.MOVE_RESULT&&((OneRegisterInstruction)size).getRegisterA()==3,"English native size register changed");
          found=i-1;
        }
      }
    }
    require(found==69,"Missing/shifted English native-size preserve block");
    Instruction replacement=new ImmutableInstruction12x(Opcode.MOVE,4,3);
    require(code.get(found).getCodeUnits()==replacement.getCodeUnits(),"English instruction width changed");
    code.set(found,replacement);
    MethodImplementation impl=new ImmutableMethodImplementation(body.getRegisterCount(),code,body.getTryBlocks(),body.getDebugItems());
    return new ImmutableMethod(old.getDefiningClass(),old.getName(),old.getParameters(),old.getReturnType(),old.getAccessFlags(),old.getAnnotations(),old.getHiddenApiRestrictions(),impl);
  }
  static String json(Object v){if(v==null)return "null";if(v instanceof Number||v instanceof Boolean)return v.toString();if(v instanceof Map){StringJoiner s=new StringJoiner(",","{","}");for(var e:((Map<?,?>)v).entrySet())s.add(json(e.getKey())+":"+json(e.getValue()));return s.toString();}if(v instanceof Iterable){StringJoiner s=new StringJoiner(",","[","]");for(Object x:(Iterable<?>)v)s.add(json(x));return s.toString();}return "\""+v.toString().replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";}
  public static void main(String[] a)throws Exception{
    require(a.length==4,"baseline.mpe compiled.dex output.mpe report.json");require(sha(a[0]).equals(BASE_SHA),"Published runtime pin differs");
    Map<String,ClassDef> before=load(a[0]),overlay=load(a[1]),merged=new TreeMap<>(before);
    require(before.size()==91,"Baseline type count");
    require(overlay.containsKey(WORD)&&overlay.containsKey("Lhiro/swiftkey/SpaceConversion;"),"Required ranking/space implementation missing");
    for(String t:overlay.keySet())require(word(t)||space(t),"Unexpected overlay type "+t);
    Set<String> added=new TreeSet<>();
    for(ClassDef c:overlay.values())if(space(c.getType())){
      require(!before.containsKey(c.getType()),"Space helper collides with baseline");
      merged.put(c.getType(),c);added.add(c.getType());
    }
    ClassDef wc=before.get(WORD);List<Method> replacements=new ArrayList<>();
    for(Method m:wc.getMethods())replacements.add(sig(m).equals(ENHANCE)?transplant(m,method(overlay.get(WORD),ENHANCE)):m);
    merged.put(WORD,cls(wc,replacements));
    ClassDef ec=before.get(ENGLISH);List<Method> english=new ArrayList<>();
    for(Method m:ec.getMethods())english.add(sig(m).equals(ENHANCE)?englishAfterNative(m):m);
    merged.put(ENGLISH,cls(ec,english));
    DexPool p=new DexPool(Opcodes.forApi(26));for(ClassDef c:merged.values())p.internClass(c);
    FileDataStore out=new FileDataStore(new File(a[2]));try{p.writeTo(out);}finally{out.close();}
    Map<String,ClassDef> after=load(a[2]);require(after.keySet().equals(merged.keySet()),"Type inventory changed");
    Set<String> changed=new TreeSet<>();
    for(ClassDef old:before.values()){
      ClassDef now=after.get(old.getType());
      if(!old.getType().equals(WORD)&&!old.getType().equals(ENGLISH))require(Arrays.equals(canonical(old),canonical(now)),"Unrelated class changed "+old.getType());
      else {
        List<Method> restored=new ArrayList<>();Set<String> oldM=new TreeSet<>(),newM=new TreeSet<>();
        for(Method n:now.getMethods())newM.add(sig(n));
        for(Method m:old.getMethods()){
          oldM.add(sig(m));Method n=method(now,sig(m));boolean differs=!Arrays.equals(mb(old,m),mb(old,n));
          require(differs==sig(m).equals(ENHANCE),"Unexpected WordCandidates method delta "+sig(m));
          if(differs){changed.add(old.getType()+"->"+sig(m));Method expected=old.getType().equals(WORD)?transplant(m,method(overlay.get(WORD),ENHANCE)):englishAfterNative(m);require(Arrays.equals(mb(old,n),mb(old,expected)),"Compiled implementation differs");}
          restored.add(differs?m:n);
        }
        require(oldM.equals(newM),"Word method inventory changed");
        require(Arrays.equals(canonical(old),canonical(cls(now,restored))),"Word class metadata/fields changed");
      }
    }
    for(String t:added)require(Arrays.equals(canonical(overlay.get(t)),canonical(after.get(t))),"Added implementation differs "+t);
    require(changed.equals(Set.of(WORD+"->"+ENHANCE,ENGLISH+"->"+ENHANCE)),"Missing ranking delta");
    Map<String,Object> r=new LinkedHashMap<>();r.put("status","PASSED");r.put("baseline_extension_sha256",sha(a[0]));r.put("runtime_sha256",sha(a[2]));r.put("compiled_overlay_sha256",sha(a[1]));r.put("runtime_before_classes",before.size());r.put("runtime_after_classes",after.size());r.put("changed_methods",changed);r.put("changed_classes",List.of(ENGLISH,WORD));r.put("added_classes",added);r.put("all_other_method_bodies_preserved",true);r.put("all_other_classes_preserved",true);r.put("class_and_member_metadata_preserved",true);r.put("compiled_implementations_preserved",true);Files.writeString(Path.of(a[3]),json(r)+"\n");System.out.println(json(r));
  }
}
