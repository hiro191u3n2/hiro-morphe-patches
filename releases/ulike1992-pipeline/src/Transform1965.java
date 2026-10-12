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

/** Camera-only update from exact published .64. Every image/native/save helper survives unchanged. */
public final class Transform1965 {
 static final String P="Lcom/hiro/ulike/";
 static final String OLD_DESCRIPTION="v1.9.64（v1.9.63基準）";
 static final String DESCRIPTION="v1.9.65（v1.9.64基準）カメラ状態管理と継続診断。切替時の監視所有者・未測定画面の更新を修正。再起動をまたぐ上限付き記録・異常前後の保護・診断ZIP共有。画像計算・保存・GPUを維持。実機未検証。";
 static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static final Set<String> ALLOWED_ROOTS=Set.of("RenderStartup1938","PreviewLayout1922","LayoutLifecycle1937","FrontPreview1936","CameraTrace1965","PreviewOutput1965","CameraSession1965");
 static final Set<String> FAMILIES=new TreeSet<>(Arrays.asList(System.getProperty("ulike.production",String.join(",",ALLOWED_ROOTS)).split(",")));
 static final String BURST=P+"BurstCapture1933;";
 static String rootOf(String type){if(!type.startsWith(P)||!type.endsWith(";"))return "";String n=type.substring(P.length(),type.length()-1);int d=n.indexOf('$');return d<0?n:n.substring(0,d);}
 static boolean owned(String type){return FAMILIES.contains(rootOf(type));}
 static void req(boolean value,String message){MergePayloads.require(value,message);}
 static Method replace(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef members(ClassDef c,Collection<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
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
   out.put(c.getType(),changed?members(c,ms):c);
  }
  req(hits==1,"Exactly one ULike description must change");return out;
 }
 static void verifyMetadata(Map<String,ClassDef> before,Map<String,ClassDef> after)throws Exception{
  req(before.keySet().equals(after.keySet()),"Metadata class inventory changed");
  for(var row:before.entrySet()){
   ClassDef current=after.get(row.getKey());
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

 static int verifyRuntime(Map<String,ClassDef> before,Map<String,ClassDef> after)throws Exception{
  int preserved=0;
  for(var row:before.entrySet())if(!owned(row.getKey())){
   req(after.containsKey(row.getKey()),"Retained runtime class removed "+row.getKey());
   if(CameraTraceHooks1965.owns(row.getKey()))CameraTraceHooks1965.verify(row.getValue(),after.get(row.getKey()));
   else{req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(after.get(row.getKey()))),"Image/save/other helper changed "+row.getKey());preserved++;}
  }
  for(String type:after.keySet())if(!before.containsKey(type))req(owned(type),"Undeclared helper added "+type);
  req(MergePayloads.classHash(before.get(BURST)).equals(MergePayloads.classHash(after.get(BURST))),"No-fusion capture class identical");
  Method begin=MergePayloads.methods(List.of(after.get(BURST))).get(BURST+"->beginImage(Ljava/lang/Object;Landroid/media/Image;Landroid/hardware/camera2/TotalCaptureResult;)Z");
  var ops=new ArrayList<Instruction>();for(Instruction x:begin.getImplementation().getInstructions())ops.add(x);
  req(ops.size()==2&&ops.get(0).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)ops.get(0)).getNarrowLiteral()==0&&ops.get(1).getOpcode()==Opcode.RETURN,"No-fusion return false");
  // The published runtime already owns retained AndroidX encoder classes.
  // Only newly introduced classes must belong to the seven reviewed families;
  // every inherited class outside those families was compared above.
  for(String type:after.keySet())if(!before.containsKey(type))req(owned(type),"Fixture/foreign class leaked into runtime "+type);
  return preserved;
 }
 static String jsonList(Collection<String> values){var rows=new ArrayList<String>();for(String s:new TreeSet<>(values))rows.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",rows)+"]";}
 public static void main(String[] a)throws Exception{
  req(a.length==5,"BASE HELPERS BUNDLE OUT AUDIT");req(ALLOWED_ROOTS.equals(FAMILIES),"Exact reviewed camera roots");
  Path base=Path.of(a[0]),out=Path.of(a[3]);Files.createDirectories(out);
  var before=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var runtime=new TreeMap<>(before);var compiled=MergePayloads.classes(a[1]);var roots=new TreeSet<String>();
  for(var row:compiled.entrySet()){req(owned(row.getKey()),"Compile-only class leaked "+row.getKey());if(!row.getKey().contains("$"))roots.add(rootOf(row.getKey()));}req(roots.equals(FAMILIES),"Exact compiled camera root inventory");
  for(String type:new ArrayList<>(runtime.keySet()))if(owned(type))runtime.remove(type);runtime.putAll(compiled);
  var hooks=new TreeSet<String>();for(var row:before.entrySet())if(CameraTraceHooks1965.owns(row.getKey())){runtime.put(row.getKey(),CameraTraceHooks1965.patch(row.getValue()));hooks.add(rootOf(row.getKey()));}
  req(hooks.equals(Set.of("OpticalZoom","ProcessingTiming1947")),"Exact retained lifecycle/timing hook roots");
  int preserved=verifyRuntime(before,runtime);
  var oldSingle=MergePayloads.classes(base.resolve("classes.dex").toString());var oldBundle=MergePayloads.classes(a[2]);var single=metadata(oldSingle);var bundle=metadata(oldBundle);verifyMetadata(oldSingle,single);verifyMetadata(oldBundle,bundle);
  MergePayloads.writeDex(out.resolve("runtime.dex"),runtime.values());MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var emitted=MergePayloads.classes(out.resolve("runtime.dex").toString());req(emitted.keySet().equals(runtime.keySet()),"Serialized helper inventory");
  for(var row:runtime.entrySet()){
   boolean hook=CameraTraceHooks1965.owns(row.getKey());String wanted=hook?CameraTraceHooks1965.canonicalHash(row.getValue()):MergePayloads.classHash(row.getValue());String actual=hook?CameraTraceHooks1965.canonicalHash(emitted.get(row.getKey())):MergePayloads.classHash(emitted.get(row.getKey()));
   req(wanted.equals(actual),"Serialized production helper differs "+row.getKey());
  }
  verifyRuntime(before,emitted);verifyMetadata(oldSingle,MergePayloads.classes(out.resolve("loader.dex").toString()));verifyMetadata(oldBundle,MergePayloads.classes(out.resolve("bundle-loader.dex").toString()));
  Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"),StandardCopyOption.REPLACE_EXISTING);Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"),StandardCopyOption.REPLACE_EXISTING);
  var oldMethods=MergePayloads.methods(before.values());var newMethods=MergePayloads.methods(emitted.values());var changed=new TreeSet<String>();var added=new TreeSet<String>();var removed=new TreeSet<String>();var audit=new ArrayList<String>();
  for(var row:oldMethods.entrySet()){
   Method now=newMethods.get(row.getKey());if(now==null){req(owned(row.getValue().getDefiningClass())&&(row.getValue().getAccessFlags()&3)==0,"Retained/public/protected method removed");removed.add(row.getKey());}
   else if(!MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(now))){req(owned(now.getDefiningClass())||CameraTraceHooks1965.owns(now.getDefiningClass()),"Unreviewed runtime method change");changed.add(row.getKey());audit.add("RUNTIME\t"+row.getKey()+"\t"+MergePayloads.hash(row.getValue())+"\t"+MergePayloads.hash(now));}
  }
  for(String id:newMethods.keySet())if(!oldMethods.containsKey(id)){req(owned(newMethods.get(id).getDefiningClass()),"Unexpected new method");req((newMethods.get(id).getAccessFlags()&0x100)==0,"New JNI forbidden");added.add(id);}
  var newClasses=new TreeSet<>(emitted.keySet());newClasses.removeAll(before.keySet());
  StringBuilder q=new StringBuilder("{\n");
  for(String flag:List.of("image_pipeline_byte_identical","quality_pixel_math_byte_identical","save_encoding_helpers_byte_identical","unrelated_runtime_classes_bytecode_identical","native_methods_byte_identical","native_methods_tsv_byte_identical","test_fixture_classes_absent_from_runtime","black_tap_disabled","capture_class_bytecode_identical","capture_begin_image_false_return_verified"))q.append("\"").append(flag).append("\":true,\n");
  q.append("\"preserved_runtime_class_count\":").append(preserved).append(",\n");
  var lists=new TreeMap<String,Collection<String>>();lists.put("changed_runtime_methods",changed);lists.put("new_runtime_methods",added);lists.put("removed_runtime_methods",removed);lists.put("new_helper_classes",newClasses);lists.put("replaced_helper_roots",FAMILIES);lists.put("bytecode_patch_helper_roots",hooks);
  for(var pair:lists.entrySet())q.append("\"").append(pair.getKey()).append("\":").append(jsonList(pair.getValue())).append(",\n");
  q.append("\"changed_native_methods\":[],\n\"new_native_methods\":[],\n\"new_jni_methods\":[],\n\"new_runtime_aliases\":[]\n}\n");
  Files.writeString(out.resolve("camera-inventory1965.json"),q.toString());
  audit.add("PASS\tcompiled_camera_roots="+roots.size()+"\tretained_class_count="+preserved+"\timage_save_helpers_preserved=true\tretained_hooks_inverse_verified=true\tserialized_dex_verified=true\tbegin_image_false_return_verified=true");
  Files.write(Path.of(a[4]),audit);System.out.println(audit.get(audit.size()-1));
 }
}
