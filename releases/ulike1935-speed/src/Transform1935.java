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

/** Replace only reviewed image-processing families and explicitly declared hooks. */
public final class Transform1935 {
 static final String P="Lcom/hiro/ulike/";
 static final String OLD_DESCRIPTION="v1.9.34（v1.9.33基準）";
 static final String DESCRIPTION="v1.9.35（v1.9.34基準）S2・S3・S4・S5・S6・S7・S8・S16の画素維持型高速化とT1保存中の次撮影受付。画質設定・起動復帰修正・他アプリを保持。実機未確認。";
 static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static final Set<String> FAMILIES=Set.of("FusionPixels1933","BurstCapture1933","FastPixels1933","FastResize1933",
   "QualityPixels1932","QualityPipeline1932","QualityShadow1932",
   "FaceRegions1934","FaceRegions1934Pixels","SpatialNoise1934","LongMoire1934","YuvPlanes1934","Geometry1934","SpeedWorkers1935","NativeSpeed1935","AsyncSave1935","SaveQueue1935");
 static final String[] PROTECTED={"FrontPreview1931","RearRestart1926","FacingMemory1928","RearLensUi1930","PreviewStart1927",
   "ProviderLifecycle1929","PreviewInputs1929","OpticalZoom","ManualLens170","ExitBusy1921","CapturePolicy1933","ShotContext1932"};
 static boolean nativeLoader(String type){return type.equals("Lapp/hiro/ulike/patches/IntegrationPayload186;")||type.startsWith("Lapp/hiro/ulike/patches/IntegrationPayload186$");}
 static Map<String,ClassDef> nativeLoader(Map<String,ClassDef> base,Map<String,ClassDef> compiled){var result=new TreeMap<>(base);for(String k:new ArrayList<>(result.keySet()))if(nativeLoader(k))result.remove(k);for(var e:compiled.entrySet()){req(nativeLoader(e.getKey()),"Unreviewed native installer helper "+e.getKey());result.put(e.getKey(),e.getValue());}return result;}
 static void req(boolean b,String s){MergePayloads.require(b,s);}
 static boolean owned(String type){if(!type.startsWith(P)||!type.endsWith(";"))return false;String name=type.substring(P.length(),type.length()-1);int at=name.indexOf('$');return FAMILIES.contains(at<0?name:name.substring(0,at));}
 static String id(Method m){return MergePayloads.id(m);}
 static Method replace(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef members(ClassDef c,Collection<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static Map<String,ClassDef> metadata(Map<String,ClassDef> old){
  var out=new TreeMap<String,ClassDef>();int count=0;
  for(ClassDef c:old.values()){
   var ms=new ArrayList<Method>();boolean changed=false;
   for(Method m:c.getMethods()){
    if(m.getImplementation()==null){ms.add(m);continue;}
    var b=new MutableMethodImplementation(m.getImplementation());boolean hit=false;
    for(int i=0;i<b.getInstructions().size();i++){
     Instruction x=b.getInstructions().get(i);
     if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().startsWith(OLD_DESCRIPTION)){
      req(c.getType().equals(LOADER)&&x.getOpcode()==Opcode.CONST_STRING,"Only ULike description may change");
      b.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(DESCRIPTION)));count++;hit=true;
     }
    }ms.add(hit?replace(m,b):m);changed|=hit;
   }out.put(c.getType(),changed?members(c,ms):c);
  }req(count==1,"Exactly one ULike description");return out;
 }
 static String jsonList(Collection<String> values){var a=new ArrayList<String>();for(String s:new TreeSet<>(values))a.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",a)+"]";}
 public static void main(String[] args)throws Exception{
  req(args.length==7,"BASE HELPERS BUNDLE SEED OUT AUDIT NATIVE_INSTALLER_DEX");Path base=Path.of(args[0]),out=Path.of(args[4]);Files.createDirectories(out);
  var oldNativeClasses=MergePayloads.classes(base.resolve("ulike/methods.dex").toString());
  var oldNative=MergePayloads.methods(oldNativeClasses.values());var payload=new TreeMap<>(oldNative);
  var rows=MergePayloads.contracts(base.resolve("ulike/methods.tsv").toString());
  req(rows.keySet().equals(oldNative.keySet()),"Baseline native contract inventory");
  for(var e:oldNative.entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Baseline native contract "+e.getKey());
  var seed=args[3].equals("-")?new TreeMap<String,Method>():MergePayloads.methods(MergePayloads.classes(args[3]).values());
  var audit=new ArrayList<String>();var nativeChanged=new TreeSet<String>();var nativeAdded=new TreeSet<String>();
  for(String key:Hooks1935.NATIVE){
   Method before=oldNative.getOrDefault(key,seed.get(key));req(before!=null,"Native target absent "+key);
   Method after=Hooks1935.repair(before);req(!MergePayloads.hash(before).equals(MergePayloads.hash(after)),"Native hook did not change "+key);
   payload.put(key,after);nativeChanged.add(key);
   if(oldNative.containsKey(key))rows.get(key)[2]=MergePayloads.hash(after);
   else{rows.put(key,new String[]{key,MergePayloads.hash(before),MergePayloads.hash(after)});nativeAdded.add(key);}
   audit.add("NATIVE\t"+key+"\t"+MergePayloads.hash(before)+"\t"+MergePayloads.hash(after));
  }
  req(seed.keySet().equals(nativeAdded),"Stock seed must contain exactly newly patched methods");
  var oldRuntime=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());
  var runtime=new TreeMap<>(oldRuntime);var compiled=MergePayloads.classes(args[1]);
  req(!compiled.isEmpty(),"No compiled production classes");
  for(var e:compiled.entrySet())req(owned(e.getKey()),"Compile stub or unreviewed class leaked "+e.getKey());
  var replacementRoots=new TreeSet<String>();
  for(String k:compiled.keySet())if(!k.contains("$"))replacementRoots.add(k.substring(P.length(),k.length()-1));
  for(String k:new ArrayList<>(runtime.keySet()))if(owned(k)){
   String name=k.substring(P.length(),k.length()-1);int at=name.indexOf('$');String root=at<0?name:name.substring(0,at);
   if(replacementRoots.contains(root))runtime.remove(k);
  }
  runtime.putAll(compiled);
  var touchedHooks=new TreeSet<String>();
  for(ClassDef c:new ArrayList<>(runtime.values())){
   var ms=new ArrayList<Method>();boolean changed=false;
   for(Method m:c.getMethods())if(Hooks1935.RUNTIME.contains(id(m))){
    req(!owned(c.getType()),"Do not hook compiled classes");ms.add(Hooks1935.repair(m));changed=true;touchedHooks.add(id(m));
   }else ms.add(m);
   if(changed)runtime.put(c.getType(),members(c,ms));
  }
  req(touchedHooks.equals(Hooks1935.RUNTIME),"Runtime hook coverage");
  var oldMethods=MergePayloads.methods(oldRuntime.values());var nextMethods=MergePayloads.methods(runtime.values());
  var changedRuntime=new TreeSet<String>();var addedRuntime=new TreeSet<String>();var removedRuntime=new TreeSet<String>();
  for(var e:oldMethods.entrySet()){
   Method after=nextMethods.get(e.getKey());
   if(after==null){req(owned(e.getValue().getDefiningClass()),"Unrelated method removed");removedRuntime.add(e.getKey());}
   else if(!MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(after))){
    req(owned(after.getDefiningClass())||Hooks1935.RUNTIME.contains(e.getKey()),"Unrelated runtime method changed");changedRuntime.add(e.getKey());
    audit.add("RUNTIME\t"+e.getKey()+"\t"+MergePayloads.hash(e.getValue())+"\t"+MergePayloads.hash(after));
   }
  }
  for(String k:nextMethods.keySet())if(!oldMethods.containsKey(k))addedRuntime.add(k);
  for(String name:PROTECTED){String type=P+name+";";req(oldRuntime.containsKey(type)&&runtime.containsKey(type)&&MergePayloads.classHash(oldRuntime.get(type)).equals(MergePayloads.classHash(runtime.get(type))),"Protected class changed "+name);}
  for(var e:oldRuntime.entrySet())if(!owned(e.getKey())&&e.getValue().getMethods().iterator().hasNext()&&
     java.util.stream.StreamSupport.stream(e.getValue().getMethods().spliterator(),false).noneMatch(m->Hooks1935.RUNTIME.contains(id(m))))
   req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(runtime.get(e.getKey()))),"Unrelated runtime class changed "+e.getKey());
  var nativeClasses=new TreeMap<String,ClassDef>();
  for(ClassDef holder:MergePayloads.holders(payload)){
   ClassDef previous=oldNativeClasses.get(holder.getType());var ms=new ArrayList<Method>();holder.getMethods().forEach(ms::add);
   nativeClasses.put(holder.getType(),previous==null?holder:ms.stream().anyMatch(m->Hooks1935.NATIVE.contains(id(m)))?members(previous,ms):previous);
  }
  var installer=MergePayloads.classes(args[6]);req(!installer.isEmpty(),"Missing native installer compiler output");var single=nativeLoader(metadata(MergePayloads.classes(base.resolve("classes.dex").toString())),installer);var oldBundle=MergePayloads.classes(args[2]);var bundle=nativeLoader(metadata(oldBundle),installer);
  for(var e:oldBundle.entrySet())if(!e.getKey().equals(LOADER)&&!nativeLoader(e.getKey()))req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(bundle.get(e.getKey()))),"Other loader changed");
  MergePayloads.writeDex(out.resolve("runtime.dex"),runtime.values());
  if(nativeChanged.isEmpty()){Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"),StandardCopyOption.REPLACE_EXISTING);Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"),StandardCopyOption.REPLACE_EXISTING);}
  else{MergePayloads.writeDex(out.resolve("methods.dex"),nativeClasses.values());var lines=new ArrayList<String>();for(var row:rows.values())lines.add(String.join("\t",row));Files.write(out.resolve("methods.tsv"),lines);}
  MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var newClasses=new TreeSet<>(runtime.keySet());newClasses.removeAll(oldRuntime.keySet());
  Files.writeString(out.resolve("speed-inventory.json"),"{\n\"changed_runtime_methods\":"+jsonList(changedRuntime)+",\n\"changed_native_methods\":"+jsonList(nativeChanged)+",\n\"new_native_methods\":"+jsonList(nativeAdded)+",\n\"new_helper_classes\":"+jsonList(newClasses)+",\n\"replaced_helper_roots\":"+jsonList(replacementRoots)+",\n\"new_runtime_methods\":"+jsonList(addedRuntime)+",\n\"removed_runtime_methods\":"+jsonList(removedRuntime)+",\n\"new_runtime_aliases\":[],\n\"camera_startup_recovery_byte_identical\":true\n}\n");
  audit.add("PASS\treplaced_helper_roots="+replacementRoots.size()+"\truntime_changes="+changedRuntime.size()+"\tnative_changes="+nativeChanged.size());
  Files.write(Path.of(args[5]),audit);System.out.println(audit.get(audit.size()-1));
 }
}
