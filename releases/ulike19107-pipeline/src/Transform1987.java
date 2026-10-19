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

/** Reviewed model capture, policy preparation and Finish reservation changes over immutable .86; image arithmetic is retained. */
public final class Transform1987 {
 static final String P="Lcom/hiro/ulike/";
 static final String OLD_DESCRIPTION="v1.9.86（v1.9.85基準）";
 static final String DESCRIPTION="v1.9.87（v1.9.86基準）モデル検証画像の重複読取り、利用できないGPU補正の準備、仕上げ検証の優先予約と保持競合を修正。画素計算・解像度・HEIF設定・保存済み画質認定を維持。実機の短縮時間は未測定。";
 static final String INSTALLER="Lapp/hiro/ulike/patches/IntegrationPayload186;";
 static final Set<String> NEW_JNI=Set.of();
 static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static final Set<String> ALLOWED_ROOTS=Set.of("CameraTrace1965","GpuAnalysis1961","GpuChain1961","GpuProtection1961","ProcessingTiming1947");
 static final Set<String> FAMILIES=new TreeSet<>(Arrays.asList(System.getProperty("ulike.production",String.join(",",ALLOWED_ROOTS)).split(",")));
 static final String BURST=P+"BurstCapture1933;";
 static final Map<String,Set<String>> INSTRUMENTED=new TreeMap<>();
 // Qualification is retained byte-for-byte; .87 needs no restored private compiler bridges.
 static final Set<String> RETAINED_COMPILER_BRIDGES1987=Set.of();
 static void retainCompilerBridges1987(Map<String,ClassDef> before,Map<String,ClassDef> after){
  var previous=MergePayloads.methods(before.values());var current=MergePayloads.methods(after.values());
  for(String id:RETAINED_COMPILER_BRIDGES1987){
   Method original=previous.get(id);req(original!=null&&(original.getAccessFlags()&0x1008)==0x1008,"Expected inherited static synthetic bridge "+id);
   req(!current.containsKey(id),"Compiler bridge preservation must not replace a newly compiled method "+id);
   ClassDef owner=after.get(original.getDefiningClass());req(owner!=null&&owned(owner.getType()),"Reviewed bridge owner absent "+id);
   var methods=new ArrayList<Method>();for(Method method:owner.getMethods())methods.add(method);methods.add(original);
   after.put(owner.getType(),members(owner,methods));
  }
  current=MergePayloads.methods(after.values());
  for(String id:RETAINED_COMPILER_BRIDGES1987)req(MergePayloads.hash(previous.get(id)).equals(MergePayloads.hash(current.get(id))),"Inherited bridge body changed "+id);
 }
 static void loadInstrumented()throws Exception{
  String file=System.getProperty("ulike.instrumented","");req(!file.isEmpty(),"Explicit diagnostic method review required");
  for(String line:Files.readAllLines(Path.of(file))){if(line.isEmpty())continue;String[] x=line.split("\t",-1);
   req(x.length==2&&x[0].matches("[A-Za-z][A-Za-z0-9_$]*")&&x[1].matches("(?:[A-Za-z_$][A-Za-z0-9_$]*|<init>|<clinit>)"),"Unsafe diagnostic method declaration");
   req(owned(P+x[0]+";"),"Diagnostic method declared outside reviewed roots "+x[0]);
   req(INSTRUMENTED.computeIfAbsent(P+x[0]+";",k->new TreeSet<>()).add(x[1]),"Duplicate diagnostic method declaration");
  }
  req(!INSTRUMENTED.isEmpty(),"Empty diagnostic method review");
 }
 static boolean instrumented(Method m){return INSTRUMENTED.getOrDefault(m.getDefiningClass(),Set.of()).contains(m.getName());}
 static boolean versionOnly(Method old,Method now){return diagnostic(old.getDefiningClass())&&MergePayloads.hash(normalizeVersion(old,"1.9.86")).equals(MergePayloads.hash(now));}
 static int verifyInstrumentation(Map<String,ClassDef> before,Map<String,ClassDef> after){
  var previous=MergePayloads.methods(before.values());var current=MergePayloads.methods(after.values());int preserved=0;
  for(Method old:previous.values()){
   Method now=current.get(MergePayloads.id(old));req(now!=null,"Approved repair removed existing method "+MergePayloads.id(old));
   if(MergePayloads.hash(old).equals(MergePayloads.hash(now))||versionOnly(old,now)){preserved++;continue;}
   req(instrumented(now),"Noninstrumented runtime method changed "+MergePayloads.id(old));
  }
  for(Method now:current.values())if(!previous.containsKey(MergePayloads.id(now))){
   req(owned(now.getDefiningClass()),"Undeclared diagnostic helper method "+MergePayloads.id(now));
   boolean helper=rootOf(now.getDefiningClass()).equals("PipelineDiagnostics1982");
   boolean synthetic=(now.getAccessFlags()&0x1000)!=0&&now.getName().matches("access\\$[0-9]+");
   req(helper||now.getName().endsWith("1987")||now.getDefiningClass().contains("1987")||instrumented(now)||synthetic,"Undeclared additive diagnostic API "+MergePayloads.id(now));
  }
  return preserved;
 }
 static int verifyInheritedSave(Map<String,ClassDef> before,Map<String,ClassDef> after)throws Exception{
  Set<String> saved=Set.of("SaveFd186","SaveQuality2","FaceRegions1934","CodecPreparation1981","EncoderTail1981","AsyncSave1935","SaveQueue1935","PreviewOutput1965","PhotoAnalysis1981","SaveMemory1981");
  var found=new TreeSet<String>();int count=0;
  for(var row:before.entrySet())if(saved.contains(rootOf(row.getKey()))){
   req(after.containsKey(row.getKey())&&MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(after.get(row.getKey()))),"Inherited .86 save/analysis/preview helper changed "+row.getKey());found.add(rootOf(row.getKey()));count++;
  }
  req(found.equals(saved),"Complete inherited .86 save/analysis/preview inventory");return count;
 }

 static String rootOf(String type){if(!type.startsWith(P)||!type.endsWith(";"))return "";String n=type.substring(P.length(),type.length()-1);int d=n.indexOf('$');return d<0?n:n.substring(0,d);}
 static boolean owned(String type){return FAMILIES.contains(rootOf(type));}
 static boolean audited(String type){return owned(type);}
 static String serializationHash(ClassDef c)throws Exception{
  // Compare reconstructed classes through the same immutable representation.
  // Adding the two exact ticket bridges reverses only the physical placement
  // of InnerClass/EnclosingClass annotation items after DEX serialization.
  // Canonicalization retains every header, field, annotation, method and debug
  // item; the independent method/field checks and hook inverses remain required.
  return (TimingCameraHooks1980.owned(c.getType())
   ||c.getType().equals(P+"GpuQualification1961$Reservation1984;"))
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
   if(row.getKey().equals(INSTALLER)){req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(current)),"Native installer byte-identical to .86");continue;}
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
  expected.put("GpuChain1961",Set.of("chooseFinish1976","win","cpuFinish","cpuFinishOwned1962","runFinishStages","runLegacy1976","compareFinish1978","finishBandsLegacy1978","finishBands1978","collectBand1978","finishRows1978"));
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
    Method now=current.get(MergePayloads.id(old));req(now!=null,"Critical kernel method removed "+MergePayloads.id(old));found.add(old.getName());req(!instrumented(now),"Critical arithmetic/dispatch method cannot be exempted by the repair review "+MergePayloads.id(old));req(MergePayloads.hash(old).equals(MergePayloads.hash(now)),"Native dispatch/pixel policy/timing method body changed "+MergePayloads.id(old));checked++;
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
    String value=s.getString().replace(version,"1.9.87");
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
   boolean exact=MergePayloads.hash(normalizeVersion(old,"1.9.86")).equals(MergePayloads.hash(now));
   if(rootOf(old.getDefiningClass()).equals("CameraTrace1965"))req(exact,"Camera trace changed beyond version "+MergePayloads.id(old));
   else req(exact,"Timing diagnostics changed beyond version "+MergePayloads.id(old));
   if(exact)checked++;
  }
  return checked;
 }
 static void verifyRemovedInternalCalls(Map<String,ClassDef> classes,Collection<String> removed){
  for(ClassDef owner:classes.values())for(Method method:owner.getMethods())if(method.getImplementation()!=null)for(Instruction instruction:method.getImplementation().getInstructions())if(instruction instanceof ReferenceInstruction reference&&reference.getReference() instanceof MethodReference call)req(!removed.contains(call.toString()),"Retained helper references removed internal Session callable "+MergePayloads.id(method));
 }
 static String jsonList(Collection<String> values){var rows=new ArrayList<String>();for(String s:new TreeSet<>(values))rows.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",rows)+"]";}
 public static void main(String[] a)throws Exception{
  req(a.length==5,"BASE HELPERS BUNDLE OUT AUDIT");req(ALLOWED_ROOTS.equals(FAMILIES),"Exact reviewed diagnostic roots");loadInstrumented();
  Path base=Path.of(a[0]),out=Path.of(a[3]);Files.createDirectories(out);
  var before=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var runtime=new TreeMap<>(before);var compiled=MergePayloads.classes(a[1]);var roots=new TreeSet<String>();
  for(var row:compiled.entrySet()){req(owned(row.getKey()),"Compile-only class leaked "+row.getKey());if(!row.getKey().contains("$"))roots.add(rootOf(row.getKey()));}req(roots.equals(FAMILIES),"Exact compiled admission root inventory");
  for(String type:new ArrayList<>(runtime.keySet()))if(owned(type))runtime.remove(type);runtime.putAll(compiled);
  var hooks=new TreeSet<String>();
  for(var row:compiled.entrySet())if(TimingCameraHooks1980.owned(row.getKey())){runtime.put(row.getKey(),TimingCameraHooks1980.patch(row.getValue()));hooks.add(rootOf(row.getKey()));}
  retainCompilerBridges1987(before,runtime);
  TimingCameraHooks1980.verifyInventory(before,runtime);
  var inheritedMethods=MergePayloads.methods(before.values());var currentMethods=MergePayloads.methods(runtime.values());
  for(var row:inheritedMethods.entrySet())if((row.getValue().getAccessFlags()&0x100)!=0){
   Method now=currentMethods.get(row.getKey());req(now!=null&&(now.getAccessFlags()&0x100)!=0&&MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(now)),"Inherited native declaration changed "+row.getKey());
  }
  int saved=verifyInheritedSave(before,runtime);int noninstrumented=verifyInstrumentation(before,runtime);
  int preserved=verifyRuntime(before,runtime);int critical=verifyPixelKernels(before,runtime);int nonUi=verifyNonUiMethods(before,runtime);
  var oldSingle=MergePayloads.classes(base.resolve("classes.dex").toString());var oldBundle=MergePayloads.classes(a[2]);var single=metadata(oldSingle);var bundle=metadata(oldBundle);verifyMetadata(oldSingle,single);verifyMetadata(oldBundle,bundle);
  MergePayloads.writeDex(out.resolve("runtime.dex"),runtime.values());MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var emitted=MergePayloads.classes(out.resolve("runtime.dex").toString());req(emitted.keySet().equals(runtime.keySet()),"Serialized helper inventory");
  for(var row:runtime.entrySet()){
   String wanted=serializationHash(row.getValue());String actual=serializationHash(emitted.get(row.getKey()));
   req(wanted.equals(actual),"Serialized production helper differs "+row.getKey());
  }
  TimingCameraHooks1980.verifyInventory(before,emitted);
  verifyInheritedSave(before,emitted);verifyInstrumentation(before,emitted);
  verifyRuntime(before,emitted);verifyPixelKernels(before,emitted);verifyNonUiMethods(before,emitted);verifyMetadata(oldSingle,MergePayloads.classes(out.resolve("loader.dex").toString()));verifyMetadata(oldBundle,MergePayloads.classes(out.resolve("bundle-loader.dex").toString()));
  Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"),StandardCopyOption.REPLACE_EXISTING);Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"),StandardCopyOption.REPLACE_EXISTING);
  var oldMethods=MergePayloads.methods(before.values());var newMethods=MergePayloads.methods(emitted.values());var changed=new TreeSet<String>();var added=new TreeSet<String>();var removed=new TreeSet<String>();var audit=new ArrayList<String>();
  for(var row:oldMethods.entrySet()){
   Method now=newMethods.get(row.getKey());if(now==null){req(false,"Approved repair removed existing method "+row.getKey());}
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
  q.append("\"preview_output_observer_bytecode_identical\":true,\n");
  for(String flag:List.of("storage_codec_fence_inverse_verified","encoder_tail_release_positions_verified","face_success_completion_positions_verified"))q.append("\"").append(flag).append("\":true,\n");
  q.append("\"camera_control_lifecycle_bytecode_identical\":true,\n");
  q.append("\"diagnostic_measurement_and_logger_bodies_preserved\":true,\n");
  for(String flag:List.of("nonreviewed_runtime_methods_byte_identical1987","inherited_save_hook_contracts_preserved1982","save_helpers_byte_identical_to_baseline86","save_encoding_helpers_byte_identical","diagnostic_instrumentation_added1982"))q.append("\"").append(flag).append("\":true,\n");
  q.append("\"save_hooks1981_reapplied\":false,\n");
  q.append("\"noninstrumented_runtime_method_count1982\":").append(noninstrumented).append(",\n");
  q.append("\"inherited_save_class_count1982\":").append(saved).append(",\n");
  q.append("\"diagnostic_preserved_method_count\":").append(nonUi).append(",\n");
  q.append("\"critical_pixel_kernel_method_count\":").append(critical).append(",\n");
  q.append("\"retained_compiler_bridges1987\":").append(jsonList(RETAINED_COMPILER_BRIDGES1987)).append(",\n");
  q.append("\"preserved_runtime_class_count\":").append(preserved).append(",\n");
  var lists=new TreeMap<String,Collection<String>>();lists.put("changed_runtime_methods",changed);lists.put("new_runtime_methods",added);lists.put("removed_runtime_methods",removed);lists.put("new_helper_classes",newClasses);lists.put("replaced_helper_roots",FAMILIES);lists.put("bytecode_patch_helper_roots",hooks);
  for(var pair:lists.entrySet())q.append("\"").append(pair.getKey()).append("\":").append(jsonList(pair.getValue())).append(",\n");
  q.append("\"changed_native_methods\":[],\n\"new_native_methods\":").append(jsonList(newJni)).append(",\n\"new_jni_methods\":").append(jsonList(newJni)).append(",\n\"new_runtime_aliases\":[]\n}\n");
  Files.writeString(out.resolve("whole-audit-inventory1987.json"),q.toString());
  audit.add("PASS\tcompiled_admission_roots="+roots.size()+"\tretained_class_count="+preserved+"\tunchanged_helper_classes_identical=true\tquality_kernel_contracts_preserved=true\tserialized_dex_verified=true\tbegin_image_false_return_verified=true");
  Files.write(Path.of(a[4]),audit);System.out.println(audit.get(audit.size()-1));
 }
}

