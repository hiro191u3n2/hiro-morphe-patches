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

/** Approved .81 route changes over immutable .80. */
public final class Transform1981 {
 static final String P="Lcom/hiro/ulike/";
 static final String OLD_DESCRIPTION="v1.9.80（v1.9.79基準）";
 static final String DESCRIPTION="v1.9.81（v1.9.80基準）撮影から保存までの経路最適化：保存準備の先行、メモリ寿命の短縮、CPU/GPU完了時間での選択、NR資源再利用、待機中の同一写真解析。画質・解像度・HEIF設定を維持。実機速度未測定。";
 static final String INSTALLER="Lapp/hiro/ulike/patches/IntegrationPayload186;";
 static final Set<String> NEW_JNI=Set.of();
 static final Set<String> RETIRED_COMPARISON_ADAPTER_METHODS=Set.of(
  P+"GpuSingle1960$SingleProbe$1;->run(II)Z",P+"GpuResidual1961$ResidualProbe$1;->run(II)Z");
 static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static final Set<String> ALLOWED_ROOTS=Set.of("AsyncSave1935","CameraTrace1965","CodecPreparation1981","ColourCache1976","CpuFinishPolicy1978","CpuIdentityFinish1981","CpuSingle1978","EncoderTail1981","FaceResult1981","GpuChain1961","GpuNoise1960","GpuPolicy1960","GpuQualification1961","GpuResident1976","GpuResidual1961","GpuSingle1960","GpuSnapshotBudget1981","GpuStrong1960","GpuStrongTuning1975","PhotoAnalysis1981","PreviewOutput1965","ProcessingTiming1947","QualityPipeline1932","ReflectionCache1945","SaveMemory1981","SaveQueue1935","SingleCpu1981","SingleNoise1955","SingleResidual1961","SingleStage1981","SpeedWorkers1935","StrongNoise1958");
 static final Set<String> FAMILIES=new TreeSet<>(Arrays.asList(System.getProperty("ulike.production",String.join(",",ALLOWED_ROOTS)).split(",")));
 static final String BURST=P+"BurstCapture1933;";
 static String rootOf(String type){if(!type.startsWith(P)||!type.endsWith(";"))return "";String n=type.substring(P.length(),type.length()-1);int d=n.indexOf('$');return d<0?n:n.substring(0,d);}
 static boolean owned(String type){return FAMILIES.contains(rootOf(type));}
 static boolean audited(String type){return owned(type)||SaveRuntimeHooks1981.owned(type);}
 static String serializationHash(ClassDef c)throws Exception{
  // Builder replacements normalize member order. Compare both sides through
  // the same immutable representation; each hook also has a full inverse proof.
  return TimingCameraHooks1980.owned(c.getType())||SaveRuntimeHooks1981.owned(c.getType())
   ?MergePayloads.classHash(ImmutableClassDef.of(c)):MergePayloads.classHash(c);
 }
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
   if(row.getKey().equals(INSTALLER)){req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(current)),"Native installer byte-identical to .80");continue;}
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
  for(var row:before.entrySet())if(!audited(row.getKey())){
   req(after.containsKey(row.getKey()),"Retained runtime class removed "+row.getKey());
   {req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(after.get(row.getKey()))),"Retained camera/pixel/save/other helper changed "+row.getKey());preserved++;}
  }
  for(String type:after.keySet())if(!before.containsKey(type))req(owned(type),"Undeclared helper added "+type);
  req(MergePayloads.classHash(before.get(BURST)).equals(MergePayloads.classHash(after.get(BURST))),"No-fusion capture class identical");
  Method begin=MergePayloads.methods(List.of(after.get(BURST))).get(BURST+"->beginImage(Ljava/lang/Object;Landroid/media/Image;Landroid/hardware/camera2/TotalCaptureResult;)Z");
  var ops=new ArrayList<Instruction>();for(Instruction x:begin.getImplementation().getInstructions())ops.add(x);
  req(ops.size()==2&&ops.get(0).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)ops.get(0)).getNarrowLiteral()==0&&ops.get(1).getOpcode()==Opcode.RETURN,"No-fusion return false");
  // The published runtime already owns retained AndroidX encoder classes.
  // Only newly introduced classes must belong to the explicitly reviewed production families;
  // every inherited class outside those families was compared above.
  for(String type:after.keySet())if(!before.containsKey(type))req(owned(type),"Fixture/foreign class leaked into runtime "+type);
  return preserved;
 }
 static int verifyPixelKernels(Map<String,ClassDef> before,Map<String,ClassDef> after){
  var expected=new TreeMap<String,Set<String>>();
  expected.put("GpuStrong1960",Set.of("key","commands","readSlots","readCounts","result","collect","execute","ephemeral"));
  expected.put("GpuAnalysis1961",Set.of("ceil2","floats","equal","spatialCandidate1961","residentSpatial1962","regionsCandidate1961","residentCandidate1961"));
  expected.put("GpuPolicy1960",Set.of("uploadPolicy","pyramidCandidate","pyramidReference","evidenceEqual","evidenceCandidate","policyCount","sourcePlanData","policyMatches","geometry","pyramidUniforms","strongBasis8","lagUniforms","lagInvocations","spectralUniforms"));
  expected.put("NativeMoire1951",Set.of("preparePolicy","candidate","selfCheck"));
  expected.put("SingleResidual1961$Preparation",Set.of("cacheBytes"));
  expected.put("ProcessingTiming1947",Set.of("duration","merge"));
  var previous=MergePayloads.methods(before.values());var current=MergePayloads.methods(after.values());int checked=0;
  // The colour cache exposes only an additive read-only route-state query.
  // Preserve every existing numerical, dispatch and certification method.
  for(Method old:previous.values())if(old.getDefiningClass().equals(P+"ColourCache1976;")) {
   Method now=current.get(MergePayloads.id(old));
   req(now!=null&&MergePayloads.hash(old).equals(MergePayloads.hash(now)),"Existing colour-cache method changed "+MergePayloads.id(old));checked++;
  }
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
    String value=s.getString().replace(version,"1.9.81");
    req(x.getOpcode()==Opcode.CONST_STRING||x.getOpcode()==Opcode.CONST_STRING_JUMBO,"Version literal opcode");
    int register=((OneRegisterInstruction)x).getRegisterA();
    body.replaceInstruction(i,x.getOpcode()==Opcode.CONST_STRING?new BuilderInstruction21c(Opcode.CONST_STRING,register,new ImmutableStringReference(value)):new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,register,new ImmutableStringReference(value)));
   }
  }
  return replace(method,body);
 }
 static boolean diagnostic(String type){return Set.of("ProcessingTiming1947","CameraTrace1965").contains(rootOf(type));}
 static int verifyNonUiMethods(Map<String,ClassDef> before,Map<String,ClassDef> after){
  var previous=MergePayloads.methods(before.values());var current=MergePayloads.methods(after.values());int checked=0;
  for(Method old:previous.values())if(diagnostic(old.getDefiningClass())){
   Method now=current.get(MergePayloads.id(old));req(now!=null,"Diagnostics method removed "+MergePayloads.id(old));
   req(MergePayloads.hash(normalizeVersion(old,"1.9.80")).equals(MergePayloads.hash(now)),"Diagnostic logic changed beyond version "+MergePayloads.id(old));checked++;
  }
  for(Method now:current.values())if(diagnostic(now.getDefiningClass()))req(previous.containsKey(MergePayloads.id(now)),"Unexpected diagnostics API "+MergePayloads.id(now));
  return checked;
 }
 static void verifyRemovedInternalCalls(Map<String,ClassDef> classes,Collection<String> removed){
  for(ClassDef owner:classes.values())for(Method method:owner.getMethods())if(method.getImplementation()!=null)for(Instruction instruction:method.getImplementation().getInstructions())if(instruction instanceof ReferenceInstruction reference&&reference.getReference() instanceof MethodReference call)req(!removed.contains(call.toString()),"Retained helper references removed internal Session callable "+MergePayloads.id(method));
 }
 static String jsonList(Collection<String> values){var rows=new ArrayList<String>();for(String s:new TreeSet<>(values))rows.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",rows)+"]";}
 public static void main(String[] a)throws Exception{
  req(a.length==5,"BASE HELPERS BUNDLE OUT AUDIT");req(ALLOWED_ROOTS.equals(FAMILIES),"Exact reviewed capture-to-save roots");
  Path base=Path.of(a[0]),out=Path.of(a[3]);Files.createDirectories(out);
  var before=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var runtime=new TreeMap<>(before);var compiled=MergePayloads.classes(a[1]);var roots=new TreeSet<String>();
  for(var row:compiled.entrySet()){req(owned(row.getKey()),"Compile-only class leaked "+row.getKey());if(!row.getKey().contains("$"))roots.add(rootOf(row.getKey()));}req(roots.equals(FAMILIES),"Exact compiled admission root inventory");
  for(String type:new ArrayList<>(runtime.keySet()))if(owned(type))runtime.remove(type);runtime.putAll(compiled);
  var hooks=new TreeSet<String>();
  for(var row:compiled.entrySet())if(TimingCameraHooks1980.owned(row.getKey())){runtime.put(row.getKey(),TimingCameraHooks1980.patch(row.getValue()));hooks.add(rootOf(row.getKey()));}
  TimingCameraHooks1980.verifyInventory(before,runtime);
  var inheritedMethods=MergePayloads.methods(before.values());var currentMethods=MergePayloads.methods(runtime.values());
  for(var row:inheritedMethods.entrySet())if((row.getValue().getAccessFlags()&0x100)!=0){
   Method now=currentMethods.get(row.getKey());req(now!=null&&(now.getAccessFlags()&0x100)!=0&&MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(now)),"Inherited native declaration changed "+row.getKey());
  }
  for(String type:SaveRuntimeHooks1981.TYPES){runtime.put(type,SaveRuntimeHooks1981.patch(before.get(type)));hooks.add(rootOf(type));}
  SaveRuntimeHooks1981.verify(before,runtime);
  int preserved=verifyRuntime(before,runtime);int critical=verifyPixelKernels(before,runtime);int nonUi=verifyNonUiMethods(before,runtime);
  var oldSingle=MergePayloads.classes(base.resolve("classes.dex").toString());var oldBundle=MergePayloads.classes(a[2]);var single=metadata(oldSingle);var bundle=metadata(oldBundle);verifyMetadata(oldSingle,single);verifyMetadata(oldBundle,bundle);
  MergePayloads.writeDex(out.resolve("runtime.dex"),runtime.values());MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var emitted=MergePayloads.classes(out.resolve("runtime.dex").toString());req(emitted.keySet().equals(runtime.keySet()),"Serialized helper inventory");
  for(var row:runtime.entrySet()){
   String wanted=serializationHash(row.getValue());String actual=serializationHash(emitted.get(row.getKey()));
   req(wanted.equals(actual),"Serialized production helper differs "+row.getKey());
  }
  TimingCameraHooks1980.verifyInventory(before,emitted);
  SaveRuntimeHooks1981.verify(before,emitted);
  verifyRuntime(before,emitted);verifyPixelKernels(before,emitted);verifyNonUiMethods(before,emitted);verifyMetadata(oldSingle,MergePayloads.classes(out.resolve("loader.dex").toString()));verifyMetadata(oldBundle,MergePayloads.classes(out.resolve("bundle-loader.dex").toString()));
  Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"),StandardCopyOption.REPLACE_EXISTING);Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"),StandardCopyOption.REPLACE_EXISTING);
  var oldMethods=MergePayloads.methods(before.values());var newMethods=MergePayloads.methods(emitted.values());var changed=new TreeSet<String>();var added=new TreeSet<String>();var removed=new TreeSet<String>();var audit=new ArrayList<String>();
  for(var row:oldMethods.entrySet()){
   Method now=newMethods.get(row.getKey());if(now==null){req(owned(row.getValue().getDefiningClass())&&((row.getValue().getAccessFlags()&5)==0||row.getValue().getDefiningClass().matches("Lcom/hiro/ulike/[A-Za-z0-9]+\\$[0-9]+;")||RETIRED_COMPARISON_ADAPTER_METHODS.contains(row.getKey())),"Retained/public/protected method removed "+row.getKey());removed.add(row.getKey());}
   else if(!MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(now))){req(audited(now.getDefiningClass()),"Unreviewed runtime method change");changed.add(row.getKey());audit.add("RUNTIME\t"+row.getKey()+"\t"+MergePayloads.hash(row.getValue())+"\t"+MergePayloads.hash(now));}
  }
  for(String id:newMethods.keySet())if(!oldMethods.containsKey(id)){req(owned(newMethods.get(id).getDefiningClass()),"Unexpected new method");req((newMethods.get(id).getAccessFlags()&0x100)==0||NEW_JNI.contains(id),"Only exact reviewed diagnostic JNI may be added");added.add(id);}
  verifyRemovedInternalCalls(emitted,removed);verifyRemovedInternalCalls(MergePayloads.classes(base.resolve("ulike/methods.dex").toString()),removed);
  for(var row:oldMethods.entrySet())if((row.getValue().getAccessFlags()&0x100)!=0){
   Method now=newMethods.get(row.getKey());req(now!=null&&(now.getAccessFlags()&0x100)!=0&&MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(now)),"Existing native declaration changed "+row.getKey());
  }
  var newJni=new TreeSet<String>();for(String id:added)if((newMethods.get(id).getAccessFlags()&0x100)!=0)newJni.add(id);req(newJni.equals(NEW_JNI),"Exact bounded diagnostic JNI inventory");
  var newClasses=new TreeSet<>(emitted.keySet());newClasses.removeAll(before.keySet());
  StringBuilder q=new StringBuilder("{\n");
  for(String flag:List.of("unchanged_pixel_kernel_methods_bytecode_identical","unmodified_pixel_kernel_helpers_bytecode_identical","unmodified_gpu_runtime_helpers_bytecode_identical","quality_algorithm_and_settings_preserved","camera_session_bytecode_identical","timing_camera_trace_hooks_preserved","unrelated_runtime_classes_bytecode_identical","native_methods_byte_identical","native_methods_tsv_byte_identical","test_fixture_classes_absent_from_runtime","black_tap_disabled","capture_class_bytecode_identical","capture_begin_image_false_return_verified"))q.append("\"").append(flag).append("\":true,\n");
  q.append("\"preview_output_observer_bytecode_identical\":false,\n");
  for(String flag:List.of("storage_codec_fence_inverse_verified","encoder_tail_release_positions_verified","face_success_completion_positions_verified"))q.append("\"").append(flag).append("\":true,\n");
  q.append("\"camera_control_lifecycle_bytecode_identical\":false,\n");
  q.append("\"diagnostic_measurement_and_logger_bodies_preserved\":true,\n");
  q.append("\"diagnostic_preserved_method_count\":").append(nonUi).append(",\n");
  q.append("\"critical_pixel_kernel_method_count\":").append(critical).append(",\n");
  q.append("\"preserved_runtime_class_count\":").append(preserved).append(",\n");
  var lists=new TreeMap<String,Collection<String>>();lists.put("changed_runtime_methods",changed);lists.put("new_runtime_methods",added);lists.put("removed_runtime_methods",removed);lists.put("new_helper_classes",newClasses);lists.put("replaced_helper_roots",FAMILIES);lists.put("bytecode_patch_helper_roots",hooks);
  for(var pair:lists.entrySet())q.append("\"").append(pair.getKey()).append("\":").append(jsonList(pair.getValue())).append(",\n");
  q.append("\"changed_native_methods\":[],\n\"new_native_methods\":").append(jsonList(newJni)).append(",\n\"new_jni_methods\":").append(jsonList(newJni)).append(",\n\"new_runtime_aliases\":[]\n}\n");
  Files.writeString(out.resolve("whole-audit-inventory1981.json"),q.toString());
  audit.add("PASS\tcompiled_admission_roots="+roots.size()+"\tretained_class_count="+preserved+"\tunchanged_helper_classes_identical=true\tquality_kernel_contracts_preserved=true\tserialized_dex_verified=true\tbegin_image_false_return_verified=true");
  Files.write(Path.of(a[4]),audit);System.out.println(audit.get(audit.size()-1));
 }
}

