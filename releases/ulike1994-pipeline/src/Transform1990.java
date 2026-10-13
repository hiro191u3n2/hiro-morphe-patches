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

/** Reviewed helper-family rebuild over exact .89. Emits structural facts, not device claims. */
public final class Transform1990 {
 static final String P="Lcom/hiro/ulike/",INSTALLER="Lapp/hiro/ulike/patches/IntegrationPayload186;",LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static final String OLD_DESCRIPTION="v1.9.89（v1.9.88基準）";
 static final String DESCRIPTION="v1.9.90（v1.9.89基準）撮影前GPU準備、認定経路の復元、GPU処理とメモリ経路の修正、実保存完了までの計測。画質条件・解像度・HEIFを維持。実機の速度は未測定。";
 static final Set<String> FAMILIES=new TreeSet<>(Arrays.asList(System.getProperty("ulike.production","").split(",")));
 static final String[][] NATIVE_ROWS={{"lib/arm64-v8a/libulike_gpu1960.so","ulike1960/runtime/libulike_gpu1960.so","gpu1960"}};
 static String rootOf(String type){if(!type.startsWith(P)||!type.endsWith(";"))return "";String n=type.substring(P.length(),type.length()-1);int d=n.indexOf('$');return d<0?n:n.substring(0,d);}
 static boolean owned(String type){return FAMILIES.contains(rootOf(type));}
 static void req(boolean v,String s){MergePayloads.require(v,s);}
 static String prop(String name){String v=System.getProperty("ulike."+name,"");req(v.matches(name.endsWith("bytes")?"[1-9][0-9]*":"[a-f0-9]{64}"),"Missing native fingerprint "+name);return v;}
 static Method replace(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef members(ClassDef c,Collection<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 static ClassDef updateInstaller(ClassDef c,boolean inverse,Map<String,String[]> original)throws Exception{
  var ms=new ArrayList<Method>();var hits=new TreeSet<String>();
  for(Method m:c.getMethods()){
   if(!m.getName().equals("<clinit>")){ms.add(m);continue;}
   var body=new MutableMethodImplementation(m.getImplementation());var sites=new HashMap<Integer,Integer>();var strings=new HashMap<Integer,String>();
   for(int i=0;i<body.getInstructions().size();i++){
    Instruction ins=body.getInstructions().get(i);
    if(ins.getOpcode()==Opcode.CONST_STRING&&ins instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s){int reg=((OneRegisterInstruction)ins).getRegisterA();sites.put(reg,i);strings.put(reg,s.getString());}
    if(ins.getOpcode()!=Opcode.FILLED_NEW_ARRAY||!(ins instanceof FiveRegisterInstruction row)||row.getRegisterCount()!=4)continue;
    int[] regs={row.getRegisterC(),row.getRegisterD(),row.getRegisterE(),row.getRegisterF()};String[] v=new String[4];for(int k=0;k<4;k++)v[k]=strings.get(regs[k]);
    for(String[] nativeRow:NATIVE_ROWS){
     if(!nativeRow[0].equals(v[0])||!nativeRow[1].equals(v[3]))continue;
     String prefix=nativeRow[2];req(hits.add(prefix),"Unique native row "+prefix);
     req(v[1]!=null&&v[1].matches("[a-f0-9]{64}")&&v[2]!=null&&v[2].matches("[1-9][0-9]*"),"Pinned native row fingerprint");
     if(!inverse){req(!original.containsKey(prefix),"Unique original native row");original.put(prefix,new String[]{v[1],v[2]});}
     else req(v[1].equals(prop(prefix+".sha"))&&v[2].equals(prop(prefix+".bytes")),"Serialized native row fingerprint");
     String[] replacement=inverse?original.get(prefix):new String[]{prop(prefix+".sha"),prop(prefix+".bytes")};
     for(int k=1;k<=2;k++)body.replaceInstruction(sites.get(regs[k]),new BuilderInstruction21c(Opcode.CONST_STRING,regs[k],new ImmutableStringReference(replacement[k-1])));
    }
   }
   ms.add(replace(m,body));
  }
  req(hits.size()==NATIVE_ROWS.length,"Exactly one reviewed existing native row updated");return members(c,ms);
 }
 static ClassDef installer(ClassDef c)throws Exception{
  var original=new TreeMap<String,String[]>();ClassDef after=updateInstaller(c,false,original);ClassDef restored=updateInstaller(after,true,original);
  req(MergePayloads.classHash(c).equals(MergePayloads.classHash(restored)),"Installer inverse: eleven other rows and all transaction code retained");return after;
 }
 static void verifyInstaller(ClassDef before,ClassDef after)throws Exception{
  var original=new TreeMap<String,String[]>();updateInstaller(before,false,original);ClassDef restored=updateInstaller(after,true,original);
  req(MergePayloads.classHash(before).equals(MergePayloads.classHash(restored)),"Serialized installer inverse: eleven other rows and twelve-row count preserved");
 }
 static Map<String,ClassDef> metadata(Map<String,ClassDef> old)throws Exception{
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
   out.put(c.getType(),c.getType().equals(INSTALLER)?installer(c):changed?members(c,ms):c);
  }
  req(hits==1,"Exactly one ULike description must change");return out;
 }
 static void verifyMetadata(Map<String,ClassDef> before,Map<String,ClassDef> after)throws Exception{
  req(before.keySet().equals(after.keySet()),"Metadata class inventory changed");
  for(var row:before.entrySet()){
   ClassDef current=after.get(row.getKey());
   if(row.getKey().equals(INSTALLER)){verifyInstaller(row.getValue(),current);continue;}
   if(!row.getKey().equals(LOADER)){req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(current)),"Other loader changed "+row.getKey());continue;}
   var ms=new ArrayList<Method>();int hits=0;
   Map<String,String> originalStrings=new HashMap<>();
   for(Method m:row.getValue().getMethods())if(m.getImplementation()!=null)for(Instruction x:m.getImplementation().getInstructions())if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().startsWith(OLD_DESCRIPTION))originalStrings.put(MergePayloads.id(m),s.getString());
   for(Method m:current.getMethods()){
    if(m.getImplementation()==null){ms.add(m);continue;}var b=new MutableMethodImplementation(m.getImplementation());boolean hit=false;
    for(int i=0;i<b.getInstructions().size();i++){Instruction x=b.getInstructions().get(i);if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().equals(DESCRIPTION)){
     req(originalStrings.containsKey(MergePayloads.id(m))&&x.getOpcode()==Opcode.CONST_STRING,"Metadata inverse site");b.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(originalStrings.get(MergePayloads.id(m)))));hit=true;hits++;
    }}ms.add(hit?replace(m,b):m);
   }
   req(hits==1&&MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(members(current,ms))),"ULike metadata inverse: executable loader unchanged");
  }
 }

 static String jsonList(Collection<String> values){var rows=new ArrayList<String>();for(String s:new TreeSet<>(values))rows.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",rows)+"]";}
 static String classHash(ClassDef c)throws Exception{return MergePayloads.classHash(ImmutableClassDef.of(c));}
 static void retainMissingSynthetic(Map<String,ClassDef> before,Map<String,ClassDef> runtime){
  var old=MergePayloads.methods(before.values());var now=MergePayloads.methods(runtime.values());
  for(var e:old.entrySet())if(!now.containsKey(e.getKey())){
   Method m=e.getValue();req(owned(m.getDefiningClass())&&(m.getAccessFlags()&0x1008)==0x1008&&m.getName().startsWith("access$"),"Non-synthetic method removed "+e.getKey());
   ClassDef c=runtime.get(m.getDefiningClass());req(c!=null,"Removed synthetic owner "+m.getDefiningClass());
   var all=new ArrayList<Method>();for(Method x:c.getMethods())all.add(x);all.add(m);runtime.put(c.getType(),members(c,all));
  }
 }
 static void verify(Map<String,ClassDef> before,Map<String,ClassDef> after)throws Exception{
  for(var e:before.entrySet()){
   ClassDef n=after.get(e.getKey());req(n!=null,"Existing class removed "+e.getKey());
   if(!owned(e.getKey()))req(classHash(e.getValue()).equals(classHash(n)),"Unreviewed helper changed "+e.getKey());
   req(e.getValue().getAccessFlags()==n.getAccessFlags()&&Objects.equals(e.getValue().getSuperclass(),n.getSuperclass())&&e.getValue().getInterfaces().equals(n.getInterfaces()),"Existing class header changed "+e.getKey());
  }
  var old=MergePayloads.methods(before.values());var now=MergePayloads.methods(after.values());
  for(var e:old.entrySet()){
   Method n=now.get(e.getKey());req(n!=null,"Existing runtime method removed "+e.getKey());
   req((e.getValue().getAccessFlags()&0x109)==(n.getAccessFlags()&0x109),"Public/static/native ABI changed "+e.getKey());
   if((e.getValue().getAccessFlags()&0x100)!=0)req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(n)),"Native declaration changed "+e.getKey());
  }
  for(var e:now.entrySet())if(!old.containsKey(e.getKey()))req(owned(e.getValue().getDefiningClass())&&(e.getValue().getAccessFlags()&0x100)==0,"Unexpected new class/JNI method "+e.getKey());
  for(ClassDef c:before.values())if(!owned(c.getType()))for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction x&&x.getReference() instanceof MethodReference call){
   String id=call.toString();Method prior=old.get(id),next=now.get(id);
   if(prior!=null&&(prior.getAccessFlags()&0x1000)!=0&&owned(prior.getDefiningClass()))req(next!=null&&MergePayloads.hash(prior).equals(MergePayloads.hash(next)),"Unchanged caller depends on changed synthetic bridge "+id);
  }
  for(String type:after.keySet())if(!before.containsKey(type))req(owned(type),"Compile-only helper leaked "+type);
 }
 public static void main(String[] a)throws Exception{
  req(a.length==5&&!FAMILIES.contains("")&&!FAMILIES.isEmpty(),"BASE HELPERS BUNDLE OUT AUDIT with explicit reviewed roots");
  Path base=Path.of(a[0]),out=Path.of(a[3]);Files.createDirectories(out);
  var before=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var runtime=new TreeMap<>(before);var compiled=MergePayloads.classes(a[1]);var roots=new TreeSet<String>();
  for(var e:compiled.entrySet()){req(owned(e.getKey()),"Compile-only class leaked "+e.getKey());if(!e.getKey().contains("$"))roots.add(rootOf(e.getKey()));}
  req(roots.equals(FAMILIES),"Exact production root inventory");
  for(String type:new ArrayList<>(runtime.keySet()))if(owned(type))runtime.remove(type);runtime.putAll(compiled);
  for(var e:compiled.entrySet())if(TimingCameraHooks1980.owned(e.getKey()))runtime.put(e.getKey(),TimingCameraHooks1980.patch(e.getValue()));
  retainMissingSynthetic(before,runtime);verify(before,runtime);TimingCameraHooks1980.verifyInventory(before,runtime);
  var oldSingle=MergePayloads.classes(base.resolve("classes.dex").toString());var oldBundle=MergePayloads.classes(a[2]);var single=metadata(oldSingle);var bundle=metadata(oldBundle);
  verifyMetadata(oldSingle,single);verifyMetadata(oldBundle,bundle);
  MergePayloads.writeDex(out.resolve("runtime.dex"),runtime.values());MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var emitted=MergePayloads.classes(out.resolve("runtime.dex").toString());req(emitted.keySet().equals(runtime.keySet()),"Serialized class inventory changed");
  for(var e:runtime.entrySet())req(classHash(e.getValue()).equals(classHash(emitted.get(e.getKey()))),"Serialized class changed "+e.getKey());
  verify(before,emitted);TimingCameraHooks1980.verifyInventory(before,emitted);
  verifyMetadata(oldSingle,MergePayloads.classes(out.resolve("loader.dex").toString()));verifyMetadata(oldBundle,MergePayloads.classes(out.resolve("bundle-loader.dex").toString()));
  Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"),StandardCopyOption.REPLACE_EXISTING);Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"),StandardCopyOption.REPLACE_EXISTING);
  var old=MergePayloads.methods(before.values());var now=MergePayloads.methods(emitted.values());var changed=new TreeSet<String>();var added=new TreeSet<String>();var audit=new ArrayList<String>();int preserved=0;
  for(var e:old.entrySet())if(!MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(now.get(e.getKey())))){changed.add(e.getKey());audit.add("RUNTIME\t"+e.getKey()+"\t"+MergePayloads.hash(e.getValue())+"\t"+MergePayloads.hash(now.get(e.getKey())));}else preserved++;
  for(String id:now.keySet())if(!old.containsKey(id))added.add(id);
  String q="{\n\"schema\":\"ulike1990-structural-inventory-v1\",\n\"unrelated_runtime_classes_identical\":true,\n\"native_declarations_identical\":true,\n\"timing_camera_hooks_preserved\":true,\n\"native_installer_inverse_verified\":true,\n\"new_jni_methods\":[],\n\"removed_runtime_methods\":[],\n\"preserved_runtime_methods\":"+preserved+",\n\"replaced_helper_roots\":"+jsonList(FAMILIES)+",\n\"changed_runtime_methods\":"+jsonList(changed)+",\n\"new_runtime_methods\":"+jsonList(added)+"\n}\n";
  Files.writeString(out.resolve("inventory1990.json"),q);Files.write(Path.of(a[4]),audit);System.out.println("PASS structural merge; preserved="+preserved+", changed="+changed.size()+", added="+added.size());
 }
}
