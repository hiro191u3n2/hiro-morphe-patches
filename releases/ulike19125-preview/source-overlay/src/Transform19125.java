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

/** Guarded preview release/runtime observation overlay; stock methods and every native payload are inherited verbatim. */
public final class Transform19125 {
 static final String P="Lcom/hiro/ulike/",LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static final String OLD_DESCRIPTION="v1.9.124（v1.9.123基準）";
 static final String DESCRIPTION="v1.9.125（v1.9.124基準）保存入力の保護後に既存ガード付きHDプレビュー早期解除を通常化。解除要求の時刻とカメラ入力再開を区別して記録。画質計算・解像度・保存形式・撮影保護・CPU/GPU負荷制御・必要時素材展開を維持。実機の操作改善は未確認。";
 static final Set<String> FAMILIES=new TreeSet<>(Arrays.asList(System.getProperty("ulike.production","").split(",")));
 static void req(boolean value,String message){MergePayloads.require(value,message);}
 static String root(String type){if(!type.startsWith(P)||!type.endsWith(";"))return "";String value=type.substring(P.length(),type.length()-1);int at=value.indexOf('$');return at<0?value:value.substring(0,at);}
 static boolean owned(String type){return FAMILIES.contains(root(type));}
 static String canonical(ClassDef c)throws Exception{return MergePayloads.classHash(ImmutableClassDef.of(c));}
 static Method body(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef members(ClassDef c,Collection<Method> methods){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);}
 static Map<String,ClassDef> metadata(Map<String,ClassDef> before,boolean inverse,String original)throws Exception{
  var after=new TreeMap<>(before);ClassDef c=before.get(LOADER);req(c!=null,"ULike loader missing");var ms=new ArrayList<Method>();int hits=0;
  for(Method m:c.getMethods()){
   if(m.getImplementation()==null){ms.add(m);continue;}
   var b=new MutableMethodImplementation(m.getImplementation());boolean edited=false;
   for(int i=0;i<b.getInstructions().size();i++){
    Instruction x=b.getInstructions().get(i);
    if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&
       (inverse?s.getString().equals(DESCRIPTION):s.getString().startsWith(OLD_DESCRIPTION))){
     req(x.getOpcode()==Opcode.CONST_STRING,"Pinned metadata opcode");
     b.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(inverse?original:DESCRIPTION)));
     edited=true;hits++;
    }
   }
   ms.add(edited?body(m,b):m);
  }
  req(hits==1,"Exactly one display-description change required");after.put(LOADER,members(c,ms));return after;
 }
 static String oldDescription(Map<String,ClassDef> old){
  String result=null;int count=0;
  for(Method m:old.get(LOADER).getMethods())if(m.getImplementation()!=null)for(Instruction x:m.getImplementation().getInstructions())
   if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().startsWith(OLD_DESCRIPTION)){result=s.getString();count++;}
  req(count==1,"Pinned original loader metadata");return result;
 }
 static void verifyMetadata(Map<String,ClassDef> old,Map<String,ClassDef> next,String original)throws Exception{
  req(old.keySet().equals(next.keySet()),"Loader inventory changed");var restored=metadata(next,true,original);
  for(var row:old.entrySet())req(canonical(row.getValue()).equals(canonical(restored.get(row.getKey()))),"Executable loader or installer changed: "+row.getKey());
 }
 static void retainMissingSynthetic(Map<String,ClassDef> before,Map<String,ClassDef> after){
  var current=MergePayloads.methods(after.values());
  for(Method m:MergePayloads.methods(before.values()).values())if(!current.containsKey(MergePayloads.id(m))){
   req(owned(m.getDefiningClass())&&(m.getAccessFlags()&0x1008)==0x1008&&m.getName().startsWith("access$"),"Non-synthetic method removed "+MergePayloads.id(m));
   ClassDef c=after.get(m.getDefiningClass());req(c!=null,"Synthetic owner removed "+m.getDefiningClass());
   var ms=new ArrayList<Method>();for(Method x:c.getMethods())ms.add(x);ms.add(m);after.put(c.getType(),members(c,ms));
  }
 }
 static Set<String> anonymousShape(ClassDef c){
  var out=new TreeSet<String>();for(Field f:c.getFields())out.add(f.getName()+":"+f.getType()+":"+f.getAccessFlags());
  for(Method m:c.getMethods())if(m.getName().equals("<init>"))out.add(MergePayloads.id(m)+":"+m.getAccessFlags());return out;
 }
 static void verify(Map<String,ClassDef> before,Map<String,ClassDef> after)throws Exception{
  var old=MergePayloads.methods(before.values());var now=MergePayloads.methods(after.values());
  for(var row:before.entrySet()){
   ClassDef next=after.get(row.getKey());req(next!=null,"Existing class removed "+row.getKey());ClassDef prior=row.getValue();
   req(prior.getAccessFlags()==next.getAccessFlags()&&Objects.equals(prior.getSuperclass(),next.getSuperclass())&&prior.getInterfaces().equals(next.getInterfaces()),"Existing class header changed "+row.getKey());
   if(!owned(row.getKey()))req(canonical(prior).equals(canonical(next)),"Unreviewed class changed "+row.getKey());
   if(row.getKey().matches(".*\\$[0-9]+;"))req(anonymousShape(prior).equals(anonymousShape(next)),"Anonymous identity shifted "+row.getKey());
   var fields=new TreeMap<String,Field>();for(Field f:next.getFields())fields.put(f.getName()+":"+f.getType(),f);
   for(Field f:prior.getFields()){
    Field n=fields.get(f.getName()+":"+f.getType());req(n!=null&&f.getAccessFlags()==n.getAccessFlags(),"Field ABI changed "+f);
   }
  }
  for(var row:old.entrySet()){
   Method next=now.get(row.getKey());req(next!=null,"Runtime method removed "+row.getKey());
   req(row.getValue().getAccessFlags()==next.getAccessFlags(),"Existing method access/ABI changed "+row.getKey());
   if((next.getAccessFlags()&0x100)!=0)req(MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(next)),"Native declaration changed "+row.getKey());
  }
  for(var row:now.entrySet())if(!old.containsKey(row.getKey()))req(owned(row.getValue().getDefiningClass())&&(row.getValue().getAccessFlags()&0x100)==0,"Unexpected helper or JNI addition "+row.getKey());
  for(ClassDef c:before.values())if(!owned(c.getType()))for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())
   if(i instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference call){Method prior=old.get(call.toString()),next=now.get(call.toString());
    if(prior!=null&&(prior.getAccessFlags()&0x1000)!=0&&owned(prior.getDefiningClass()))req(next!=null&&MergePayloads.hash(prior).equals(MergePayloads.hash(next)),"Unchanged caller depends on modified synthetic bridge "+call);
   }
  for(String type:after.keySet())if(!before.containsKey(type))req(owned(type),"Compile-only class leaked "+type);
 }
 static String jsonList(Collection<String> values){var out=new ArrayList<String>();for(String s:new TreeSet<>(values))out.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",out)+"]";}
 public static void main(String[] a)throws Exception{
  req(a.length==3&&!FAMILIES.isEmpty()&&!FAMILIES.contains(""),"BASE_DIRECTORY COMPILED_HELPERS_DEX OUTPUT_DIRECTORY, explicit families required");
  Path base=Path.of(a[0]),out=Path.of(a[2]);Files.createDirectories(out);
  var before=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var after=new TreeMap<>(before);var compiled=MergePayloads.classes(a[1]);var roots=new TreeSet<String>();
  for(var row:compiled.entrySet()){req(owned(row.getKey()),"Compile-only class in input "+row.getKey());if(!row.getKey().contains("$"))roots.add(root(row.getKey()));}
  req(roots.equals(FAMILIES),"Exact changed family list required");
  for(String type:new ArrayList<>(after.keySet()))if(owned(type))after.remove(type);after.putAll(compiled);
  for(var row:compiled.entrySet())if(TimingCameraHooks1980.owned(row.getKey()))after.put(row.getKey(),TimingCameraHooks1980.patch(row.getValue()));
  retainMissingSynthetic(before,after);verify(before,after);TimingCameraHooks1980.verifyInventory(before,after);VerifyRuntimeVersion19101.verify(after,"1.9.125");
  MergePayloads.writeDex(out.resolve("runtime.dex"),after.values());var emitted=MergePayloads.classes(out.resolve("runtime.dex").toString());
  req(emitted.keySet().equals(after.keySet()),"Serialized runtime inventory changed");
  for(var row:after.entrySet())req(canonical(row.getValue()).equals(canonical(emitted.get(row.getKey()))),"Serialized helper changed "+row.getKey());
  verify(before,emitted);TimingCameraHooks1980.verifyInventory(before,emitted);VerifyRuntimeVersion19101.verify(emitted,"1.9.125");
  Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"),StandardCopyOption.REPLACE_EXISTING);
  Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"),StandardCopyOption.REPLACE_EXISTING);
  req(Arrays.equals(Files.readAllBytes(base.resolve("ulike/methods.dex")),Files.readAllBytes(out.resolve("methods.dex")))&&Arrays.equals(Files.readAllBytes(base.resolve("ulike/methods.tsv")),Files.readAllBytes(out.resolve("methods.tsv"))),"Stock methods/contracts must remain byte-identical");
  var loader=MergePayloads.classes(base.resolve("classes.dex").toString());String original=oldDescription(loader);var nextLoader=metadata(loader,false,original);
  verifyMetadata(loader,nextLoader,original);MergePayloads.writeDex(out.resolve("loader.dex"),nextLoader.values());verifyMetadata(loader,MergePayloads.classes(out.resolve("loader.dex").toString()),original);
  var oldMethods=MergePayloads.methods(before.values());var newMethods=MergePayloads.methods(emitted.values());var changed=new TreeSet<String>();var added=new TreeSet<String>();var audit=new ArrayList<String>();int preserved=0;
  for(var row:oldMethods.entrySet())if(!MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(newMethods.get(row.getKey())))){changed.add(row.getKey());audit.add(row.getKey()+"\t"+MergePayloads.hash(row.getValue())+"\t"+MergePayloads.hash(newMethods.get(row.getKey())));}else preserved++;
  for(String name:newMethods.keySet())if(!oldMethods.containsKey(name))added.add(name);
  Files.write(out.resolve("changed-methods.tsv"),audit);
  Files.writeString(out.resolve("inventory19125.json"),"{\n\"schema\":\"ulike19125-overlay-inventory-v1\",\n\"runtime_version\":\"1.9.125\",\n\"changed_families\":"+jsonList(FAMILIES)+",\n\"changed_methods\":"+jsonList(changed)+",\n\"added_methods\":"+jsonList(added)+",\n\"preserved_method_count\":"+preserved+",\n\"unrelated_runtime_classes_identical\":true,\n\"existing_method_abi_preserved\":true,\n\"existing_anonymous_identity_preserved\":true,\n\"native_declarations_unchanged\":true,\n\"executable_loader_and_installer_preserved\":true,\n\"timing_camera_hooks_preserved\":true,\n\"stock_methods_and_contracts_byte_identical\":true,\n\"resource_extraction_loader_preserved\":true\n}\n");
  System.out.println("PASS guarded-preview/runtime overlay: preserved="+preserved+" changed="+changed.size()+" added="+added.size());
 }
}
