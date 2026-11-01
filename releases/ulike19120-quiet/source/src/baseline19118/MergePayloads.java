import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import app.hiro.ulike.patches.MethodContract;

/** No last-writer-wins: every delta must match its exact effective input. */
public final class MergePayloads {
 static final Opcodes OPS=Opcodes.forApi(26);
 static final String PATCH_NS="Lapp/hiro/ulike/patches/";
 static void require(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 static String id(Method m){return DexFormatter.INSTANCE.getMethodDescriptor(m);}
 static String hash(Method m){return MethodContract.sha256(m);}
 static Map<String,ClassDef> classes(String file)throws Exception{
  Map<String,ClassDef> out=new TreeMap<>();var dex=DexFileFactory.loadDexContainer(Path.of(file).toFile(),OPS);
  for(String entry:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(entry).getDexFile().getClasses())require(out.put(c.getType(),c)==null,"Duplicate class "+c.getType());
  return out;
 }
 static Map<String,Method> methods(Collection<ClassDef> cs){
  Map<String,Method> out=new TreeMap<>();for(ClassDef c:cs)for(Method m:c.getMethods())require(out.put(id(m),m)==null,"Duplicate method "+id(m));return out;
 }
 static Map<String,String[]> contracts(String file)throws Exception{
  Map<String,String[]> out=new TreeMap<>();for(String line:Files.readAllLines(Path.of(file))){
   if(line.isBlank()||line.startsWith("#"))continue;String[] a=line.split("\t",-1);require(a.length==3,"Invalid contract "+line);
   require(a[1].matches("[0-9a-f]{64}")&&a[2].matches("[0-9a-f]{64}"),"Invalid contract hash "+line);
   require(out.put(a[0],a)==null,"Duplicate contract "+a[0]);
  }return out;
 }
 static byte[] dex(Collection<ClassDef> cs)throws Exception{
  DexPool pool=new DexPool(OPS);for(ClassDef c:cs)pool.internClass(c);MemoryDataStore out=new MemoryDataStore();try{pool.writeTo(out);return out.getData();}finally{out.close();}
 }
 static String classHash(ClassDef c)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(dex(List.of(c))));}
 static void writeDex(Path file,Collection<ClassDef> cs)throws Exception{Files.write(file,dex(cs));}
 static Collection<ClassDef> holders(Map<String,Method> ms){
  Map<String,List<Method>> grouped=new TreeMap<>();for(Method m:ms.values())grouped.computeIfAbsent(m.getDefiningClass(),s->new ArrayList<>()).add(m);
  List<ClassDef> out=new ArrayList<>();for(var e:grouped.entrySet())out.add(new ImmutableClassDef(e.getKey(),1,"Ljava/lang/Object;",List.of(),null,List.of(),List.of(),e.getValue()));return out;
 }
 static void merge(String[] a)throws Exception{
  require(a.length==9,"merge STOCK_APK BASE_METHODS BASE_TSV BASE_RUNTIME SPECS OUT EXPECTED_METHODS EXPECTED_RUNTIME");
  var stockClasses=classes(a[1]);var stock=methods(stockClasses.values());var payload=methods(classes(a[2]).values());var original=new TreeMap<>(payload);var tsv=contracts(a[3]);
  require(payload.size()==Integer.parseInt(a[7]),"Unexpected baseline payload count "+payload.size());require(payload.keySet().equals(tsv.keySet()),"Baseline payload/contract set mismatch");
  for(var e:tsv.entrySet()){
   Method s=stock.get(e.getKey());require(s!=null,"Missing stock method "+e.getKey());require(hash(s).equals(e.getValue()[1]),"Stock mismatch "+e.getKey());require(hash(payload.get(e.getKey())).equals(e.getValue()[2]),"Baseline mismatch "+e.getKey());
  }
  var runtime=classes(a[4]);var runtimeOriginal=new TreeMap<>(runtime);var runtimeBefore=methods(runtime.values());
  require(runtimeBefore.size()==Integer.parseInt(a[8]),"Unexpected baseline runtime count "+runtimeBefore.size());
  Set<String> touched=new TreeSet<>(),runtimeTouched=new TreeSet<>(),runtimeAllowed=new TreeSet<>();List<String> audit=new ArrayList<>();
  for(String line:Files.readAllLines(Path.of(a[5]))){
   if(line.isBlank()||line.startsWith("#"))continue;String[] spec=line.split("\t",-1);require(spec.length==3,"Invalid component spec "+line);var cs=classes(spec[1]);var ms=methods(cs.values());
   if(spec[0].equals("delta")){
    var dc=contracts(spec[2]);require(ms.keySet().equals(dc.keySet()),"Delta/contract set mismatch "+spec[1]);
    for(var e:ms.entrySet()){
     String k=e.getKey();require(touched.add(k),"Overlapping delta; explicitly combine hooks: "+k);Method st=stock.get(k);require(st!=null,"Delta target absent from stock "+k);Method before=payload.getOrDefault(k,st);String[] contract=dc.get(k);
     require(hash(before).equals(contract[1]),"Effective baseline input mismatch "+k);require(hash(e.getValue()).equals(contract[2]),"Delta output mismatch "+k);
     Method m=e.getValue();require(st.getAccessFlags()==m.getAccessFlags(),"Loader cannot change access flags "+k);
     Method emitted=new ImmutableMethod(st.getDefiningClass(),st.getName(),st.getParameters(),st.getReturnType(),st.getAccessFlags(),st.getAnnotations(),st.getHiddenApiRestrictions(),m.getImplementation());
     require(hash(emitted).equals(contract[2]),"Loader normalization changed output "+k);payload.put(k,emitted);tsv.put(k,new String[]{k,hash(st),hash(emitted)});audit.add("delta\t"+k+"\t"+hash(before)+"\t"+hash(emitted));
    }
   }else if(spec[0].equals("runtime")){
    Set<String> allowed=new TreeSet<>(),changed=new TreeSet<>();if(!spec[2].equals("-"))for(String l:Files.readAllLines(Path.of(spec[2])))if(!l.isBlank()&&!l.startsWith("#"))require(allowed.add(l),"Duplicate runtime change authorization "+l);
    for(ClassDef c:cs.values()){
     String k=c.getType();require(k.startsWith("Lhiro/")||k.startsWith("Lcom/hiro/ulike/"),"Unexpected helper namespace "+k);require(!stockClasses.containsKey(k),"Runtime collides with stock "+k);require(runtimeTouched.add(k),"Overlapping helper class "+k);ClassDef old=runtime.get(k);
     if(old!=null){
      require(old.getAccessFlags()==c.getAccessFlags()&&Objects.equals(old.getSuperclass(),c.getSuperclass())&&old.getInterfaces().equals(c.getInterfaces()),"Runtime class header changed "+k);
      Map<String,Field> fields=new TreeMap<>();for(Field f:c.getFields())fields.put(DexFormatter.INSTANCE.getFieldDescriptor(f),f);
      for(Field f:old.getFields()){Field n=fields.get(DexFormatter.INSTANCE.getFieldDescriptor(f));require(n!=null&&f.getAccessFlags()==n.getAccessFlags()&&Objects.equals(f.getInitialValue(),n.getInitialValue())&&f.getAnnotations().equals(n.getAnnotations()),"Runtime field removed/changed "+f);}
      for(Method m:old.getMethods()){Method n=ms.get(id(m));require(n!=null,"Runtime method removed "+id(m));if(!hash(m).equals(hash(n))){require(allowed.contains(id(m)),"Unauthorized runtime change "+id(m));changed.add(id(m));}}
     }
     runtime.put(k,c);
    }
    require(changed.equals(allowed),"Unused runtime authorizations "+allowed+" versus changed "+changed);runtimeAllowed.addAll(changed);for(String k:changed)audit.add("runtime\t"+k+"\t"+hash(runtimeBefore.get(k))+"\t"+hash(ms.get(k)));
   }else throw new IllegalStateException("Unknown component kind "+spec[0]);
  }
  int preserved=0;for(var e:original.entrySet())if(!touched.contains(e.getKey())){require(hash(e.getValue()).equals(hash(payload.get(e.getKey()))),"Unrelated payload changed "+e.getKey());preserved++;}
  var runtimeAfter=methods(runtime.values());int runtimePreserved=0;for(var e:runtimeBefore.entrySet()){Method m=runtimeAfter.get(e.getKey());require(m!=null,"Runtime method missing "+e.getKey());if(!runtimeAllowed.contains(e.getKey())){require(hash(e.getValue()).equals(hash(m)),"Unrelated runtime changed "+e.getKey());runtimePreserved++;}}
  for(var e:runtimeOriginal.entrySet())if(!runtimeTouched.contains(e.getKey()))require(classHash(e.getValue()).equals(classHash(runtime.get(e.getKey()))),"Unrelated runtime class changed "+e.getKey());
  Path out=Path.of(a[6]);Files.createDirectories(out);writeDex(out.resolve("methods.dex"),holders(payload));writeDex(out.resolve("runtime.dex"),runtime.values());List<String> rows=new ArrayList<>();for(String[] row:tsv.values())rows.add(String.join("\t",row));Files.write(out.resolve("methods.tsv"),rows);
  var serialized=methods(classes(out.resolve("methods.dex").toString()).values());require(serialized.keySet().equals(payload.keySet()),"Serialized payload set differs");for(var e:serialized.entrySet())require(hash(e.getValue()).equals(tsv.get(e.getKey())[2]),"Serialized payload hash differs "+e.getKey());
  var sr=classes(out.resolve("runtime.dex").toString());require(sr.keySet().equals(runtime.keySet()),"Serialized runtime set differs");for(var e:runtime.entrySet())require(classHash(e.getValue()).equals(classHash(sr.get(e.getKey()))),"Serialized runtime class differs "+e.getKey());
  audit.add("PASS baseline="+original.size()+", unrelated_payload_preserved="+preserved+", changed_or_added="+touched.size()+", final_payload="+payload.size()+", baseline_runtime="+runtimeBefore.size()+", runtime_preserved="+runtimePreserved+", runtime_changed="+runtimeAllowed.size()+", final_runtime="+runtimeAfter.size());Files.write(out.resolve("payload-audit.tsv"),audit);for(String l:audit)System.out.println(l);
 }
 static void makeContracts(String[] a)throws Exception{
  require(a.length==5,"contracts STOCK BASE_METHODS DELTA OUT_TSV");var stock=methods(classes(a[1]).values());var base=methods(classes(a[2]).values());var delta=methods(classes(a[3]).values());List<String> rows=new ArrayList<>();for(var e:delta.entrySet()){Method st=stock.get(e.getKey());require(st!=null,"Missing stock target "+e.getKey());rows.add(e.getKey()+"\t"+hash(base.getOrDefault(e.getKey(),st))+"\t"+hash(e.getValue()));}Files.write(Path.of(a[4]),rows);
 }
 static void loaders(String[] a)throws Exception{
  require(a.length==4,"loaders OLD_INTEGRATED NEW_STANDALONE OUT_DEX");var old=classes(a[1]);var next=classes(a[2]);var all=new TreeMap<>(old);Set<String> previous=new TreeSet<>();for(String k:old.keySet())if(k.startsWith(PATCH_NS))previous.add(k);Set<String> expected=new TreeSet<>(previous);String helper=PATCH_NS+"NativeNv21EffectFlag;";require(old.containsKey(helper),"Pinned predecessor lacks native helper");require(next.keySet().equals(expected),"Loader namespace set mismatch: expected "+expected+" actual "+next.keySet());all.putAll(next);writeDex(Path.of(a[3]),all.values());var emitted=classes(a[3]);require(emitted.keySet().equals(all.keySet()),"Merged loader class set differs");int count=0;for(var e:all.entrySet()){require(classHash(e.getValue()).equals(classHash(emitted.get(e.getKey()))),"Loader class changed "+e.getKey());if(!expected.contains(e.getKey()))for(Method m:e.getValue().getMethods())count++;}
  System.out.println("PASS integrated loader: "+(old.size()-previous.size())+" non-ULike classes and "+count+" methods structurally unchanged; replaced "+previous.size()+" ULike classes; existing NativeNv21EffectFlag namespace retained.");
 }
 public static void main(String[] a)throws Exception{require(a.length>0,"Need merge/contracts/loaders");switch(a[0]){case "merge"->merge(a);case "contracts"->makeContracts(a);case "loaders"->loaders(a);default->throw new IllegalArgumentException(a[0]);}}
}
