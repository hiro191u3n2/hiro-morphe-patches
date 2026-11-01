import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Add per-shot measurement only, keeping the inherited native payload and image kernels. */
public final class Transform1947 {
 static final String P="Lcom/hiro/ulike/";
 static final String OLD_DESCRIPTION="v1.9.45（v1.9.44基準）";
 static final String DESCRIPTION="v1.9.47（v1.9.45基準）撮影ごとの合成・ノイズ除去・補正・圧縮・保存の実測表示。古い結果の残留を修正。画質・保存設定を保持。実機未確認。";
 static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static Set<String> families(){String raw=System.getProperty("ulike.production","");req(!raw.isEmpty(),"Exact production roots required");Set<String> roots=new TreeSet<>();for(String s:raw.split(","))req(s.matches("[A-Za-z][A-Za-z0-9]*")&&roots.add(s),"Invalid production root");return roots;}
 static final Set<String> FAMILIES=families();
 static void req(boolean b,String s){MergePayloads.require(b,s);}
 static boolean owned(String t){if(!t.startsWith(P)||!t.endsWith(";"))return false;String n=t.substring(P.length(),t.length()-1);int dollar=n.indexOf('$');return FAMILIES.contains(dollar<0?n:n.substring(0,dollar));}
 static Method replace(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef members(ClassDef c,Collection<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 static Map<String,ClassDef> metadata(Map<String,ClassDef> old){
  var out=new TreeMap<String,ClassDef>();int hits=0;
  for(ClassDef c:old.values()){
   var ms=new ArrayList<Method>();boolean changed=false;
   for(Method m:c.getMethods()){
    if(m.getImplementation()==null){ms.add(m);continue;}
    var b=new MutableMethodImplementation(m.getImplementation());boolean hit=false;
    for(int i=0;i<b.getInstructions().size();i++){
     Instruction x=b.getInstructions().get(i);
     if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().startsWith(OLD_DESCRIPTION)){
      req(c.getType().equals(LOADER)&&x.getOpcode()==Opcode.CONST_STRING,"Only ULike display metadata may change");
      b.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(DESCRIPTION)));hits++;hit=true;
     }
    }
    ms.add(hit?replace(m,b):m);changed|=hit;
   }
   out.put(c.getType(),changed?members(c,ms):c);
  }
  req(hits==1,"Exactly one ULike description must change");return out;
 }
 static String jsonList(Collection<String> values){var rows=new ArrayList<String>();for(String s:new TreeSet<>(values))rows.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",rows)+"]";}
 public static void main(String[] args)throws Exception{
  req(args.length==5,"BASE HELPERS BUNDLE OUT AUDIT");Path base=Path.of(args[0]),out=Path.of(args[3]);Files.createDirectories(out);
  var before=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var runtime=new TreeMap<>(before);var compiled=MergePayloads.classes(args[1]);
  Set<String> roots=new TreeSet<>();
  for(var e:compiled.entrySet()){req(owned(e.getKey()),"Compile-only class leaked: "+e.getKey());if(!e.getKey().contains("$"))roots.add(e.getKey().substring(P.length(),e.getKey().length()-1));}
  req(roots.equals(FAMILIES),"Exact compiled production inventory");
  for(String t:new ArrayList<>(runtime.keySet()))if(owned(t))runtime.remove(t);
  runtime.putAll(compiled);
  Set<String> touched=new TreeSet<>();
  for(ClassDef c:new ArrayList<>(runtime.values())){
   var ms=new ArrayList<Method>();boolean changed=false;
   for(Method m:c.getMethods())if(TimingHooks1947.RUNTIME.contains(MergePayloads.id(m))){
    req(!owned(c.getType()),"Hook must not replace compiled family");Method n=TimingHooks1947.repair(m);TimingHooks1947.verify(m,n);ms.add(n);Method alias=TimingHooks1947.alias(m);if(alias!=null){TimingHooks1947.verifyAlias(m,alias);ms.add(alias);}touched.add(MergePayloads.id(m));changed=true;
   }else ms.add(m);
   if(changed)runtime.put(c.getType(),members(c,ms));
  }
  req(touched.equals(TimingHooks1947.RUNTIME),"Timing hook coverage differs");
  var bm=MergePayloads.methods(before.values());var nm=MergePayloads.methods(runtime.values());Set<String> changed=new TreeSet<>(),added=new TreeSet<>(),removed=new TreeSet<>();var audit=new ArrayList<String>();
  for(var e:bm.entrySet()){
   Method n=nm.get(e.getKey());
   if(n==null){req(owned(e.getValue().getDefiningClass()),"Unrelated method removed");removed.add(e.getKey());}
   else if(!MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(n))){req(owned(n.getDefiningClass())||TimingHooks1947.RUNTIME.contains(e.getKey()),"Unrelated method changed "+e.getKey());changed.add(e.getKey());audit.add("RUNTIME\t"+e.getKey()+"\t"+MergePayloads.hash(e.getValue())+"\t"+MergePayloads.hash(n));}
  }
  for(String k:nm.keySet())if(!bm.containsKey(k)){req(owned(nm.get(k).getDefiningClass())||TimingHooks1947.ALIASES.contains(k),"Unrelated method added "+k);added.add(k);}
  for(var e:before.entrySet())if(!owned(e.getKey())){
   var actual=runtime.get(e.getKey());req(actual!=null,"Unrelated class removed");
   boolean hook=false;for(Method m:e.getValue().getMethods())hook|=TimingHooks1947.RUNTIME.contains(MergePayloads.id(m));
   if(!hook)req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(actual)),"Unrelated class changed "+e.getKey());
   else{
    var originalMethods=MergePayloads.methods(List.of(e.getValue()));var nextMethods=MergePayloads.methods(List.of(actual));var expected=new TreeSet<>(originalMethods.keySet());for(Method m:originalMethods.values()){Method alias=TimingHooks1947.alias(m);if(alias!=null)expected.add(MergePayloads.id(alias));}req(expected.equals(nextMethods.keySet()),"Hook class method inventory differs");
    for(var m:originalMethods.entrySet())if(!TimingHooks1947.RUNTIME.contains(m.getKey()))req(MergePayloads.hash(m.getValue()).equals(MergePayloads.hash(nextMethods.get(m.getKey()))),"Unrelated hooked-class method changed");
    req(MergePayloads.classHash(ImmutableClassDef.of(e.getValue())).equals(MergePayloads.classHash(ImmutableClassDef.of(members(actual,originalMethods.values())))),"Hook class shell changed "+e.getKey());
   }
  }
  var single=metadata(MergePayloads.classes(base.resolve("classes.dex").toString()));var oldBundle=MergePayloads.classes(args[2]);var bundle=metadata(oldBundle);
  for(var e:oldBundle.entrySet())if(!e.getKey().equals(LOADER))req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(bundle.get(e.getKey()))),"Other loader changed");
  MergePayloads.writeDex(out.resolve("runtime.dex"),runtime.values());MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"),StandardCopyOption.REPLACE_EXISTING);Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"),StandardCopyOption.REPLACE_EXISTING);
  var newClasses=new TreeSet<>(runtime.keySet());newClasses.removeAll(before.keySet());
  Files.writeString(out.resolve("timing-inventory.json"),"{\n\"changed_runtime_methods\":"+jsonList(changed)+",\n\"changed_native_methods\":[],\n\"new_native_methods\":[],\n\"new_helper_classes\":"+jsonList(newClasses)+",\n\"replaced_helper_roots\":"+jsonList(FAMILIES)+",\n\"new_runtime_methods\":"+jsonList(added)+",\n\"removed_runtime_methods\":"+jsonList(removed)+",\n\"new_runtime_aliases\":"+jsonList(TimingHooks1947.ALIASES)+"\n}\n");
  audit.add("PASS\ttiming_roots="+roots.size()+"\thooked_methods="+touched.size()+"\tchanged_methods="+changed.size()+"\tunchanged_native_payload=true");Files.write(Path.of(args[4]),audit);System.out.println(audit.get(audit.size()-1));
 }
}
