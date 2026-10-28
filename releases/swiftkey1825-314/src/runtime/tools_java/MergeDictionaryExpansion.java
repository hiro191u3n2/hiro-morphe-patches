import java.io.File;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.*;

/** Keep the published runtime; transplant only reviewed bounds and full-row parsing. */
public final class MergeDictionaryExpansion {
  static final String BASE_SHA="494565fcceffcdbcb6df0e5885cf537bd5e7b8ffa6d53f4b78f603b3ef38ca55";
  static final String WORD="Lhiro/swiftkey/WordCandidates;", INDEX="Lhiro/swiftkey/WordCandidates$Index;", LOAD="Lhiro/swiftkey/WordCandidates$1;";
  static final Set<String> METHODS=Set.of(LOAD+"->run()V",INDEX+"->fromBytes([B)Lhiro/swiftkey/WordCandidates$Index;",INDEX+"->words(I)[Ljava/lang/String;");
  static final Set<String> FIELDS=Set.of(WORD+"->MAX_ASSET_BYTES:I",WORD+"->MAX_ROWS:I");
  static void require(boolean v,String m){if(!v)throw new IllegalStateException(m);}
  static String sha(String p)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(p))));}
  static Map<String,ClassDef> load(String p)throws Exception{Map<String,ClassDef> r=new TreeMap<>();for(ClassDef c:DexFileFactory.loadDexFile(new File(p),Opcodes.forApi(26)).getClasses())require(r.put(c.getType(),c)==null,"duplicate type");return r;}
  static String sig(MethodReference m){StringBuilder b=new StringBuilder(m.getName()).append('(');for(CharSequence p:m.getParameterTypes())b.append(p);return b.append(')').append(m.getReturnType()).toString();}
  static String fsig(FieldReference f){return f.getName()+":"+f.getType();}
  static Method method(ClassDef c,String s){for(Method m:c.getMethods())if(sig(m).equals(s))return m;throw new IllegalStateException("Missing "+s);}
  static Field field(ClassDef c,String s){for(Field f:c.getFields())if(fsig(f).equals(s))return f;throw new IllegalStateException("Missing "+s);}
  static ClassDef cls(ClassDef c,Iterable<? extends Field> f,Iterable<? extends Method> m){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),f,m);}
  static byte[] canonical(ClassDef c)throws Exception{DexPool p=new DexPool(Opcodes.forApi(26));p.internClass(ImmutableClassDef.of(c));MemoryDataStore o=new MemoryDataStore();try{p.writeTo(o);return Arrays.copyOf(o.getBuffer(),o.getSize());}finally{o.close();}}
  static byte[] mb(ClassDef owner,Method m)throws Exception{return canonical(cls(owner,owner.getFields(),List.of(m)));}
  static byte[] fb(ClassDef owner,Field f)throws Exception{return canonical(cls(owner,List.of(f),List.of()));}
  static Method transplant(Method o,Method n){require(sig(o).equals(sig(n))&&o.getAccessFlags()==n.getAccessFlags(),"Method contract changed");return new ImmutableMethod(o.getDefiningClass(),o.getName(),o.getParameters(),o.getReturnType(),o.getAccessFlags(),o.getAnnotations(),o.getHiddenApiRestrictions(),n.getImplementation());}
  static Field transplant(Field o,Field n){require(fsig(o).equals(fsig(n))&&o.getAccessFlags()==n.getAccessFlags(),"Field contract changed");return new ImmutableField(o.getDefiningClass(),o.getName(),o.getType(),o.getAccessFlags(),n.getInitialValue(),o.getAnnotations(),o.getHiddenApiRestrictions());}
  static String json(Object v){if(v==null)return "null";if(v instanceof Number||v instanceof Boolean)return v.toString();if(v instanceof Map){StringJoiner s=new StringJoiner(",","{","}");for(var e:((Map<?,?>)v).entrySet())s.add(json(e.getKey())+":"+json(e.getValue()));return s.toString();}if(v instanceof Iterable){StringJoiner s=new StringJoiner(",","[","]");for(Object x:(Iterable<?>)v)s.add(json(x));return s.toString();}return "\""+v.toString().replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";}
  public static void main(String[] a)throws Exception{
    require(a.length==4,"baseline.mpe compiled.dex output.mpe report.json");require(sha(a[0]).equals(BASE_SHA),"Published runtime pin differs");
    Map<String,ClassDef> before=load(a[0]),overlay=load(a[1]),merged=new TreeMap<>();require(before.size()==91,"Baseline type count");require(overlay.keySet().equals(Set.of(WORD,INDEX,LOAD)),"Overlay type inventory");
    for(ClassDef c:before.values()){
      List<Method> methods=new ArrayList<>();List<Field> fields=new ArrayList<>();
      for(Method m:c.getMethods())methods.add(METHODS.contains(c.getType()+"->"+sig(m))?transplant(m,method(overlay.get(c.getType()),sig(m))):m);
      for(Field f:c.getFields())fields.add(FIELDS.contains(c.getType()+"->"+fsig(f))?transplant(f,field(overlay.get(c.getType()),fsig(f))):f);
      merged.put(c.getType(),cls(c,fields,methods));
    }
    DexPool p=new DexPool(Opcodes.forApi(26));for(ClassDef c:merged.values())p.internClass(c);FileDataStore out=new FileDataStore(new File(a[2]));try{p.writeTo(out);}finally{out.close();}
    Map<String,ClassDef> after=load(a[2]);require(after.keySet().equals(before.keySet()),"Type inventory changed");Set<String> changedM=new TreeSet<>(),changedF=new TreeSet<>();
    for(ClassDef old:before.values()){
      ClassDef now=after.get(old.getType());List<Method> restoredM=new ArrayList<>();List<Field> restoredF=new ArrayList<>();
      Set<String> oldM=new TreeSet<>(),newM=new TreeSet<>(),oldF=new TreeSet<>(),newF=new TreeSet<>();
      for(Method m:now.getMethods())newM.add(sig(m));for(Field f:now.getFields())newF.add(fsig(f));
      for(Method m:old.getMethods()){
        oldM.add(sig(m));Method n=method(now,sig(m));String k=old.getType()+"->"+sig(m);boolean changed=!Arrays.equals(mb(old,m),mb(old,n));require(changed==METHODS.contains(k),"Unexpected method delta "+k);
        if(changed){changedM.add(k);require(Arrays.equals(mb(old,n),mb(old,transplant(m,method(overlay.get(old.getType()),sig(m))))),"Compiled implementation differs "+k);}restoredM.add(changed?m:n);
      }
      for(Field f:old.getFields()){
        oldF.add(fsig(f));Field n=field(now,fsig(f));String k=old.getType()+"->"+fsig(f);boolean changed=!Arrays.equals(fb(old,f),fb(old,n));require(changed==FIELDS.contains(k),"Unexpected field delta "+k);
        if(changed){changedF.add(k);require(Arrays.equals(fb(old,n),fb(old,transplant(f,field(overlay.get(old.getType()),fsig(f))))),"Compiled field differs "+k);}restoredF.add(changed?f:n);
      }
      require(oldM.equals(newM)&&oldF.equals(newF),"Member inventory changed");require(Arrays.equals(canonical(old),canonical(cls(now,restoredF,restoredM))),"Original metadata changed "+old.getType());
    }
    require(changedM.equals(METHODS)&&changedF.equals(FIELDS),"Missing expected deltas");Map<String,Object> r=new LinkedHashMap<>();r.put("status","PASSED");r.put("baseline_extension_sha256",sha(a[0]));r.put("runtime_sha256",sha(a[2]));r.put("compiled_word_sha256",sha(a[1]));r.put("runtime_classes",91);r.put("changed_methods",changedM);r.put("changed_fields",changedF);r.put("all_other_method_bodies_preserved",true);r.put("all_other_fields_preserved",true);r.put("class_and_member_metadata_preserved",true);r.put("compiled_implementations_preserved",true);Files.writeString(Path.of(a[3]),json(r)+"\n");System.out.println(json(r));
  }
}
