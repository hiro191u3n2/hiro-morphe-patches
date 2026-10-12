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

/** Camera restart repair from exact .71; retained GPU, pixel, native and save classes remain identical. */
public final class Transform1972 {
 static final String P="Lcom/hiro/ulike/";
 static final String OLD_DESCRIPTION="v1.9.71（v1.9.70基準）";
 static final String DESCRIPTION="v1.9.72（v1.9.71基準）インカメラ再起動時の入力未到着の復旧と診断記録を修正。起動・所有者・準備状態を照合し、復旧結果と中断理由を記録。GPU方針・画素計算・保存・全nativeを維持。実機未検証。";
 static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static final Set<String> ALLOWED_ROOTS=Set.of("FrontPreview1936","CameraSession1965","PreviewLayout1922","CameraTrace1965","ProcessingTiming1947");
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
   {req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(after.get(row.getKey()))),"Retained camera/pixel/save/other helper changed "+row.getKey());preserved++;}
  }
  for(String type:after.keySet())if(!before.containsKey(type))req(owned(type),"Undeclared helper added "+type);
  req(MergePayloads.classHash(before.get(BURST)).equals(MergePayloads.classHash(after.get(BURST))),"No-fusion capture class identical");
  Method begin=MergePayloads.methods(List.of(after.get(BURST))).get(BURST+"->beginImage(Ljava/lang/Object;Landroid/media/Image;Landroid/hardware/camera2/TotalCaptureResult;)Z");
  var ops=new ArrayList<Instruction>();for(Instruction x:begin.getImplementation().getInstructions())ops.add(x);
  req(ops.size()==2&&ops.get(0).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)ops.get(0)).getNarrowLiteral()==0&&ops.get(1).getOpcode()==Opcode.RETURN,"No-fusion return false");
  // The published runtime already owns retained AndroidX encoder classes.
  // Only newly introduced classes must belong to the five reviewed production families;
  // every inherited class outside those families was compared above.
  for(String type:after.keySet())if(!before.containsKey(type))req(owned(type),"Fixture/foreign class leaked into runtime "+type);
  return preserved;
 }
 static int verifyPixelKernels(Map<String,ClassDef> before,Map<String,ClassDef> after){
  var expected=new TreeMap<String,Set<String>>();
  expected.put("GpuStrong1960",Set.of("key","commands","readSlots","readCounts","result","collect","execute","ephemeral"));
  expected.put("GpuSingle1960",Set.of("run"));
  expected.put("GpuResidual1961",Set.of("run"));
  expected.put("GpuAnalysis1961",Set.of("ceil2","floats","equal","spatialCandidate1961","residentSpatial1962","regionsCandidate1961","residentCandidate1961"));
  expected.put("GpuPolicy1960",Set.of("uploadPolicy","pyramidCandidate","pyramidReference","evidenceEqual","evidenceCandidate","sourcePlan","policyCount","sourcePlanData","policyMatches","geometry","pyramidUniforms","strongBasis8","lagUniforms","lagInvocations","spectralUniforms"));
  expected.put("ProcessingTiming1947",Set.of("duration","merge"));
  var previous=MergePayloads.methods(before.values());var current=MergePayloads.methods(after.values());int checked=0;
  for(var family:expected.entrySet()){
   var found=new TreeSet<String>();String type=P+family.getKey()+";";
   for(Method old:previous.values())if(old.getDefiningClass().equals(type)&&family.getValue().contains(old.getName())){
    Method now=current.get(MergePayloads.id(old));req(now!=null&&MergePayloads.hash(old).equals(MergePayloads.hash(now)),"Critical native dispatch/pixel policy/timing method body changed "+MergePayloads.id(old));found.add(old.getName());checked++;
   }
   req(found.equals(family.getValue()),"Complete critical kernel/timing method inventory "+family.getKey());
  }
  return checked;
 }
 static Method normalizeVersion(Method method,String version){
  if(method.getImplementation()==null)return method;
  var body=new MutableMethodImplementation(method.getImplementation());
  for(int i=0;i<body.getInstructions().size();i++){
   Instruction x=body.getInstructions().get(i);
   if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().contains(version)){
    String value=s.getString().replace(version,"1.9.72");
    req(x.getOpcode()==Opcode.CONST_STRING||x.getOpcode()==Opcode.CONST_STRING_JUMBO,"Version literal opcode");
    int register=((OneRegisterInstruction)x).getRegisterA();
    body.replaceInstruction(i,x.getOpcode()==Opcode.CONST_STRING?new BuilderInstruction21c(Opcode.CONST_STRING,register,new ImmutableStringReference(value)):new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,register,new ImmutableStringReference(value)));
   }
  }
  return replace(method,body);
 }
 static boolean menuListener(Method method){
  if(!method.getDefiningClass().startsWith(P+"CameraTrace1965$")||!method.getName().equals("onClick")||method.getImplementation()==null)return false;
  for(Instruction x:method.getImplementation().getInstructions())if(x instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference m&&m.getDefiningClass().equals(P+"CameraTrace1965;")&&m.getName().startsWith("access$"))return true;
  return false;
 }
 static boolean diagnostic(String type){return Set.of("CameraTrace1965","ProcessingTiming1947").contains(rootOf(type));}
 static int verifyNonUiMethods(Map<String,ClassDef> before,Map<String,ClassDef> after){
  var oldTypes=new TreeSet<String>();var newTypes=new TreeSet<String>();for(String type:before.keySet())if(diagnostic(type))oldTypes.add(type);for(String type:after.keySet())if(diagnostic(type))newTypes.add(type);req(oldTypes.equals(newTypes),"Exact inherited logger class inventory");
  for(String type:oldTypes){
   ClassDef old=before.get(type),now=after.get(type);var fields=new TreeMap<String,Field>();for(Field f:now.getFields())fields.put(f.getName(),f);
   for(Field field:old.getFields()){
    Field f=fields.remove(field.getName());req(f!=null&&field.getType().equals(f.getType())&&field.getAccessFlags()==f.getAccessFlags()&&field.getAnnotations().equals(f.getAnnotations()),"Logger field structure changed "+type+"->"+field.getName());
    if(field.getName().equals("VERSION")&&Set.of(P+"CameraTrace1965;",P+"ProcessingTiming1947;").contains(type))req(f.getInitialValue() instanceof com.android.tools.smali.dexlib2.iface.value.StringEncodedValue&&((com.android.tools.smali.dexlib2.iface.value.StringEncodedValue)f.getInitialValue()).getValue().equals("1.9.72"),"Exact new logger version");
    else req(Objects.equals(field.getInitialValue(),f.getInitialValue()),"Logger field value changed "+type+"->"+field.getName());
   }req(fields.isEmpty(),"No new logger fields");
  }
  var previous=MergePayloads.methods(before.values());var current=MergePayloads.methods(after.values());int checked=0;
  for(Method old:previous.values())if(diagnostic(old.getDefiningClass())){Method now=current.get(MergePayloads.id(old));req(now!=null,"Logger method removed");req(MergePayloads.hash(normalizeVersion(old,"1.9.71")).equals(MergePayloads.hash(now)),"Logger body changed beyond version literal "+MergePayloads.id(old));checked++;}
  for(Method now:current.values())if(diagnostic(now.getDefiningClass()))req(previous.containsKey(MergePayloads.id(now)),"Logger method added");
  return checked;
 }
 static final String LAYOUT=P+"PreviewLayout1922;";
 static boolean layoutBridge(Method m){return m.getDefiningClass().equals(LAYOUT)&&m.getName().matches("access\\$[0-9]+")&&(m.getAccessFlags()&0x1000)!=0;}
 static String bridgeBody(Method m){
  req(layoutBridge(m)&&m.getAccessFlags()==0x1008&&m.getImplementation()!=null,"Exact javac static synthetic bridge form "+MergePayloads.id(m));
  return MergePayloads.hash(new ImmutableMethod(m.getDefiningClass(),"access$canonical",m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),m.getImplementation()));
 }
 static boolean layoutDiagnostic(Method m){
  String type=m.getDefiningClass();return type.equals(P+"PreviewLayout1922$LayoutProbe;")||type.equals(P+"PreviewLayout1922$2;")||type.equals(LAYOUT)&&Set.of("layoutSnapshot","probeSettledLayout").contains(m.getName());
 }
 static Map<String,MethodReference> layoutBridgeAliases(Map<String,Method> previous,Map<String,Method> current){
  var nowBodies=new TreeMap<String,Method>();var aliases=new TreeMap<String,MethodReference>();var matched=new TreeSet<String>();
  for(Method m:current.values())if(layoutBridge(m))req(nowBodies.put(bridgeBody(m),m)==null,"Ambiguous current layout bridge body");
  for(Method old:previous.values())if(layoutBridge(old)){
   Method now=nowBodies.get(bridgeBody(old));req(now!=null,"Inherited synthetic layout bridge body removed "+MergePayloads.id(old));req(matched.add(MergePayloads.id(now)),"Non-injective layout bridge match");
   aliases.put(MergePayloads.id(now),new ImmutableMethodReference(old.getDefiningClass(),old.getName(),old.getParameterTypes(),old.getReturnType()));
  }
  var allowed=Set.of(LAYOUT+"->layoutEpoch()J",LAYOUT+"->trace(Ljava/lang/String;Lcom/bytedance/corecamera/ui/view/CameraShadeView;Ljava/lang/String;)V");
  var newTargets=new TreeSet<String>();
  for(Method bridge:current.values())if(layoutBridge(bridge)&&!matched.contains(MergePayloads.id(bridge))){
   bridgeBody(bridge);var ops=new ArrayList<Instruction>();for(Instruction x:bridge.getImplementation().getInstructions())ops.add(x);
   req(ops.get(0).getOpcode()==Opcode.INVOKE_STATIC&&ops.get(0) instanceof ReferenceInstruction,"New diagnostic bridge must begin with static forwarding call");
   Reference r=((ReferenceInstruction)ops.get(0)).getReference();req(r instanceof MethodReference,"Diagnostic bridge target must be a method");MethodReference target=(MethodReference)r;
   req(allowed.contains(target.toString())&&newTargets.add(target.toString())&&bridge.getParameterTypes().equals(target.getParameterTypes())&&bridge.getReturnType().equals(target.getReturnType()),"Only unique signature-exact epoch/trace diagnostic forwarding bridges may be added");
   req(target.getReturnType().equals("V")?ops.size()==2&&ops.get(1).getOpcode()==Opcode.RETURN_VOID:ops.size()==3&&ops.get(1).getOpcode()==Opcode.MOVE_RESULT_WIDE&&ops.get(2).getOpcode()==Opcode.RETURN_WIDE,"New diagnostic bridge contains only invocation and matching return");
   int uses=0;for(Method caller:current.values())if(caller.getImplementation()!=null)for(Instruction x:caller.getImplementation().getInstructions())if(x instanceof ReferenceInstruction ref&&ref.getReference() instanceof MethodReference m&&m.toString().equals(MergePayloads.id(bridge))){req(layoutDiagnostic(caller),"New diagnostic bridge used by non-diagnostic layout policy "+MergePayloads.id(caller));uses++;}
   req(uses>0,"Unused new diagnostic bridge");
  }
  req(newTargets.equals(allowed),"Exact two new diagnostic-only bridge target inventory");return aliases;
 }
 static Method normalizeLayoutAliases(Method method,Map<String,MethodReference> aliases){
  if(method.getImplementation()==null)return method;var body=new MutableMethodImplementation(method.getImplementation());
  for(int i=0;i<body.getInstructions().size();i++){
   Instruction x=body.getInstructions().get(i);if(!(x instanceof ReferenceInstruction ref)||!(ref.getReference() instanceof MethodReference target))continue;
   MethodReference old=aliases.get(target.toString());if(old==null||old.toString().equals(target.toString()))continue;
   if(x.getOpcode()==Opcode.INVOKE_STATIC&&x instanceof FiveRegisterInstruction call)body.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,call.getRegisterCount(),call.getRegisterC(),call.getRegisterD(),call.getRegisterE(),call.getRegisterF(),call.getRegisterG(),old));
   else if(x.getOpcode()==Opcode.INVOKE_STATIC_RANGE&&x instanceof RegisterRangeInstruction call)body.replaceInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,call.getStartRegister(),call.getRegisterCount(),old));
   else req(false,"Only static invocation may reference a matched javac bridge");
  }
  String name=method.getName();if(layoutBridge(method)){MethodReference old=aliases.get(MergePayloads.id(method));req(old!=null,"Unmatched diagnostic bridge requested as policy");name=old.getName();}
  return new ImmutableMethod(method.getDefiningClass(),name,method.getParameters(),method.getReturnType(),method.getAccessFlags(),method.getAnnotations(),method.getHiddenApiRestrictions(),body);
 }
 static void verifyLayoutPolicy(Map<String,ClassDef> before,Map<String,ClassDef> after){
  var previous=MergePayloads.methods(before.values());var current=MergePayloads.methods(after.values());var aliases=layoutBridgeAliases(previous,current);var canonical=new TreeMap<String,Method>();
  for(Method now:current.values())if(rootOf(now.getDefiningClass()).equals("PreviewLayout1922")){
   if(layoutBridge(now)&&!aliases.containsKey(MergePayloads.id(now)))continue;
   Method normalized=normalizeLayoutAliases(now,aliases);req(canonical.put(MergePayloads.id(normalized),normalized)==null,"Alias normalization collides with a retained method");
  }
  for(Method old:previous.values())if(rootOf(old.getDefiningClass()).equals("PreviewLayout1922")&&!layoutDiagnostic(old)){
   Method now=canonical.get(MergePayloads.id(old));req(now!=null&&MergePayloads.hash(old).equals(MergePayloads.hash(now)),"Non-diagnostic preview layout policy changed after exact synthetic bridge normalization "+MergePayloads.id(old));
  }
 }
 static String jsonList(Collection<String> values){var rows=new ArrayList<String>();for(String s:new TreeSet<>(values))rows.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",rows)+"]";}
 public static void main(String[] a)throws Exception{
  req(a.length==5,"BASE HELPERS BUNDLE OUT AUDIT");req(ALLOWED_ROOTS.equals(FAMILIES),"Exact reviewed preview repair roots");
  Path base=Path.of(a[0]),out=Path.of(a[3]);Files.createDirectories(out);
  var before=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var runtime=new TreeMap<>(before);var compiled=MergePayloads.classes(a[1]);var roots=new TreeSet<String>();
  for(var row:compiled.entrySet()){req(owned(row.getKey()),"Compile-only class leaked "+row.getKey());if(!row.getKey().contains("$"))roots.add(rootOf(row.getKey()));}req(roots.equals(FAMILIES),"Exact compiled admission root inventory");
  for(String type:new ArrayList<>(runtime.keySet()))if(owned(type))runtime.remove(type);runtime.putAll(compiled);
  var hooks=new TreeSet<String>();
  for(var row:compiled.entrySet())if(TimingCameraHooks1972.owned(row.getKey()))runtime.put(row.getKey(),TimingCameraHooks1972.patch(row.getValue()));
  TimingCameraHooks1972.verifyInventory(before,runtime);
  var inheritedMethods=MergePayloads.methods(before.values());var currentMethods=MergePayloads.methods(runtime.values());
  for(var row:inheritedMethods.entrySet())if((row.getValue().getAccessFlags()&0x100)!=0){
   Method now=currentMethods.get(row.getKey());req(now!=null&&(now.getAccessFlags()&0x100)!=0&&MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(now)),"Inherited native declaration changed "+row.getKey());
  }
  int preserved=verifyRuntime(before,runtime);int critical=verifyPixelKernels(before,runtime);int nonUi=verifyNonUiMethods(before,runtime);verifyLayoutPolicy(before,runtime);
  var oldSingle=MergePayloads.classes(base.resolve("classes.dex").toString());var oldBundle=MergePayloads.classes(a[2]);var single=metadata(oldSingle);var bundle=metadata(oldBundle);verifyMetadata(oldSingle,single);verifyMetadata(oldBundle,bundle);
  MergePayloads.writeDex(out.resolve("runtime.dex"),runtime.values());MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var emitted=MergePayloads.classes(out.resolve("runtime.dex").toString());req(emitted.keySet().equals(runtime.keySet()),"Serialized helper inventory");
  for(var row:runtime.entrySet()){
   String wanted=TimingCameraHooks1972.owned(row.getKey())?TimingCameraHooks1972.canonical(row.getValue()):MergePayloads.classHash(row.getValue());String actual=TimingCameraHooks1972.owned(row.getKey())?TimingCameraHooks1972.canonical(emitted.get(row.getKey())):MergePayloads.classHash(emitted.get(row.getKey()));
   req(wanted.equals(actual),"Serialized production helper differs "+row.getKey());
  }
  TimingCameraHooks1972.verifyInventory(before,emitted);
  verifyRuntime(before,emitted);verifyPixelKernels(before,emitted);verifyNonUiMethods(before,emitted);verifyLayoutPolicy(before,emitted);verifyMetadata(oldSingle,MergePayloads.classes(out.resolve("loader.dex").toString()));verifyMetadata(oldBundle,MergePayloads.classes(out.resolve("bundle-loader.dex").toString()));
  Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"),StandardCopyOption.REPLACE_EXISTING);Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"),StandardCopyOption.REPLACE_EXISTING);
  var oldMethods=MergePayloads.methods(before.values());var newMethods=MergePayloads.methods(emitted.values());var changed=new TreeSet<String>();var added=new TreeSet<String>();var removed=new TreeSet<String>();var audit=new ArrayList<String>();
  for(var row:oldMethods.entrySet()){
   Method now=newMethods.get(row.getKey());if(now==null){req(owned(row.getValue().getDefiningClass())&&(row.getValue().getAccessFlags()&5)==0,"Retained/public/protected method removed");removed.add(row.getKey());}
   else if(!MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(now))){req(owned(now.getDefiningClass()),"Unreviewed runtime method change");changed.add(row.getKey());audit.add("RUNTIME\t"+row.getKey()+"\t"+MergePayloads.hash(row.getValue())+"\t"+MergePayloads.hash(now));}
  }
  for(String id:newMethods.keySet())if(!oldMethods.containsKey(id)){req(owned(newMethods.get(id).getDefiningClass()),"Unexpected new method");req((newMethods.get(id).getAccessFlags()&0x100)==0,"New JNI forbidden");added.add(id);}
  var newClasses=new TreeSet<>(emitted.keySet());newClasses.removeAll(before.keySet());
  StringBuilder q=new StringBuilder("{\n");
  for(String flag:List.of("critical_pixel_kernel_methods_bytecode_identical","pixel_kernel_helpers_bytecode_identical","gpu_other_runtime_helpers_bytecode_identical","quality_algorithm_and_settings_preserved","preview_output_observer_bytecode_identical","timing_camera_trace_hooks_preserved","save_encoding_helpers_byte_identical","unrelated_runtime_classes_bytecode_identical","native_methods_byte_identical","native_methods_tsv_byte_identical","test_fixture_classes_absent_from_runtime","black_tap_disabled","capture_class_bytecode_identical","capture_begin_image_false_return_verified"))q.append("\"").append(flag).append("\":true,\n");
  q.append("\"camera_control_lifecycle_bytecode_identical\":false,\n\"camera_session_bytecode_identical\":false,\n\"camera_repair_scope_verified\":true,\n\"preview_layout_policy_bytecode_preserved\":true,\n\"strong_gpu_policy_preserved\":true,\n");
  q.append("\"diagnostic_measurement_and_logger_bodies_preserved\":true,\n");
  q.append("\"diagnostic_preserved_method_count\":").append(nonUi).append(",\n");
  q.append("\"critical_pixel_kernel_method_count\":").append(critical).append(",\n");
  q.append("\"preserved_runtime_class_count\":").append(preserved).append(",\n");
  var lists=new TreeMap<String,Collection<String>>();lists.put("changed_runtime_methods",changed);lists.put("new_runtime_methods",added);lists.put("removed_runtime_methods",removed);lists.put("new_helper_classes",newClasses);lists.put("replaced_helper_roots",FAMILIES);lists.put("bytecode_patch_helper_roots",hooks);
  for(var pair:lists.entrySet())q.append("\"").append(pair.getKey()).append("\":").append(jsonList(pair.getValue())).append(",\n");
  q.append("\"changed_native_methods\":[],\n\"new_native_methods\":[],\n\"new_jni_methods\":[],\n\"new_runtime_aliases\":[]\n}\n");
  Files.writeString(out.resolve("camera-restart-inventory1972.json"),q.toString());
  audit.add("PASS\tcompiled_admission_roots="+roots.size()+"\tretained_class_count="+preserved+"\timage_save_helpers_preserved=true\tno_retained_gpu_pixel_save_helpers_changed=true\tserialized_dex_verified=true\tbegin_image_false_return_verified=true");
  Files.write(Path.of(a[4]),audit);System.out.println(audit.get(audit.size()-1));
 }
}

