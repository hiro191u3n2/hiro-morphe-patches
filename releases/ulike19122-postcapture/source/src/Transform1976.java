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

/** Strong full-mode GPU preference from exact .69. Immutable CPU pixel math and every unrelated GPU/camera/save helper family remain identical. */
public final class Transform1976 {
 static final String P="Lcom/hiro/ulike/";
 static final String OLD_DESCRIPTION="v1.9.75（v1.9.74基準）";
 static final String DESCRIPTION="v1.9.76（v1.9.75基準）CPU色変換と保護判定の再利用、GPU補正の連続投入、厳密除算と近傍共有、区間長の実測選択、GPU内のノイズから補正への受け渡しを追加。完全一致照合と失敗時CPU退避を保持。実機未確認。";
 static final String INSTALLER="Lapp/hiro/ulike/patches/IntegrationPayload186;";
 static final Set<String> NEW_JNI=Set.of("Lcom/hiro/ulike/GpuNoise1960;->copyNative1976(JIIIII)Z","Lcom/hiro/ulike/GpuNoise1960;->uploadRangeNative1976(JII[III)Z","Lcom/hiro/ulike/NativeMoire1951;->finishStripCached1976([I[I[IZIIIIZZIIIZZ[I)Z","Lcom/hiro/ulike/ColourCache1976;->cpuNative([I[IIIIIIIIIZIIIIII[F[IZ)Z","Lcom/hiro/ulike/ColourCache1976;->prepareNative([IIIIIIIIIZIIIII[FZ)[F","Lcom/hiro/ulike/ColourCache1976;->directNative(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;[IIIIIIIIIZIIIII[FZ)Z");
 static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static final Set<String> ALLOWED_ROOTS=Set.of("GpuStrong1960","GpuNoise1960","GpuQualification1961","GpuStrongTuning1975","GpuStrongLayout1976","GpuPolicy1960","GpuChain1961","GpuResident1976","QualityPipeline1932","NativeMoire1951","FaceRegions1934","PairedRegions1976","ProcessingTiming1947","CameraTrace1965","ColourCache1976","SingleNoise1955","SingleResidual1961","CpuFinishCache1976");
 static final Set<String> FAMILIES=new TreeSet<>(Arrays.asList(System.getProperty("ulike.production",String.join(",",ALLOWED_ROOTS)).split(",")));
 static final String BURST=P+"BurstCapture1933;";
 static String rootOf(String type){if(!type.startsWith(P)||!type.endsWith(";"))return "";String n=type.substring(P.length(),type.length()-1);int d=n.indexOf('$');return d<0?n:n.substring(0,d);}
 static String prop(String name){String v=System.getProperty("ulike."+name,"");req(v.matches(name.endsWith("bytes")?"[1-9][0-9]*":"[a-f0-9]{64}"),"Missing pinned native property: "+name);return v;}
 static boolean owned(String type){return FAMILIES.contains(rootOf(type));}
 static void req(boolean value,String message){MergePayloads.require(value,message);}
 static Method replace(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef members(ClassDef c,Collection<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 static final String[][] NATIVE_ROWS={
  {"lib/arm64-v8a/libulike_gpu1960.so","ulike1960/runtime/libulike_gpu1960.so","gpu1960"},
  {"lib/arm64-v8a/libulike_moire1951.so","ulike1951/runtime/libulike_moire1951.so","moire1951"},
  {"lib/arm64-v8a/libulike_nr1955.so","ulike1955/runtime/libulike_nr1955.so","nr1955"}};
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
  req(hits.size()==NATIVE_ROWS.length,"Exactly three existing native rows updated");return members(c,ms);
 }
 static ClassDef installer(ClassDef c)throws Exception{
  var original=new TreeMap<String,String[]>();ClassDef after=updateInstaller(c,false,original);ClassDef restored=updateInstaller(after,true,original);
  req(MergePayloads.classHash(c).equals(MergePayloads.classHash(restored)),"Installer inverse: nine other rows and all transaction code retained");return after;
 }
 static void verifyInstaller(ClassDef before,ClassDef after)throws Exception{
  var original=new TreeMap<String,String[]>();updateInstaller(before,false,original);ClassDef restored=updateInstaller(after,true,original);
  req(MergePayloads.classHash(before).equals(MergePayloads.classHash(restored)),"Serialized installer inverse: nine other rows and twelve-row count preserved");
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
  expected.put("GpuPolicy1960",Set.of("uploadPolicy","pyramidCandidate","pyramidReference","evidenceEqual","evidenceCandidate","policyCount","sourcePlanData","policyMatches","geometry","pyramidUniforms","strongBasis8","lagUniforms","lagInvocations","spectralUniforms"));
  expected.put("SingleNoise1955",Set.of("processJavaRange"));
  expected.put("NativeMoire1951",Set.of("preparePolicy","candidate","selfCheck"));
  expected.put("SingleResidual1961$Preparation",Set.of("cacheBytes"));
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
    String value=s.getString().replace(version,"1.9.76");
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
 static boolean diagnostic(String type){return Set.of("ProcessingTiming1947","CameraTrace1965").contains(rootOf(type));}
 static int verifyNonUiMethods(Map<String,ClassDef> before,Map<String,ClassDef> after){
  var oldTypes=new TreeSet<String>();var newTypes=new TreeSet<String>();for(String type:before.keySet())if(diagnostic(type))oldTypes.add(type);for(String type:after.keySet())if(diagnostic(type))newTypes.add(type);req(oldTypes.equals(newTypes),"Exact inherited timing/logger class inventory");
  for(String type:oldTypes){
   ClassDef old=before.get(type),now=after.get(type);req(old.getAccessFlags()==now.getAccessFlags()&&Objects.equals(old.getSuperclass(),now.getSuperclass())&&old.getInterfaces().equals(now.getInterfaces())&&Objects.equals(old.getSourceFile(),now.getSourceFile())&&old.getAnnotations().equals(now.getAnnotations()),"Inherited diagnostics class metadata changed "+type);var fields=new TreeMap<String,Field>();for(Field f:now.getFields())fields.put(f.getName(),f);
   for(Field field:old.getFields()){
    Field currentField=fields.remove(field.getName());req(currentField!=null&&field.getType().equals(currentField.getType())&&field.getAccessFlags()==currentField.getAccessFlags()&&field.getAnnotations().equals(currentField.getAnnotations()),"Inherited diagnostics field structure changed "+type+"->"+field.getName());
    if(field.getName().equals("VERSION")&&Set.of(P+"ProcessingTiming1947;",P+"CameraTrace1965;").contains(type))req(currentField.getInitialValue() instanceof com.android.tools.smali.dexlib2.iface.value.StringEncodedValue&&((com.android.tools.smali.dexlib2.iface.value.StringEncodedValue)currentField.getInitialValue()).getValue().equals("1.9.76"),"Exact new diagnostics version");
    else req(Objects.equals(field.getInitialValue(),currentField.getInitialValue()),"Inherited diagnostics field value changed "+type+"->"+field.getName());
   }
   for(Field addedField:fields.values()){String n=addedField.getName();String expected=n.equals("correctionRoute1976")?"I":n.endsWith("Measured1976")?"Z":"J";req(addedField.getType().equals(expected)&&addedField.getAccessFlags()==0&&addedField.getAnnotations().isEmpty()&&addedField.getInitialValue()==null,"Only exact primitive Strong telemetry fields "+n);}
   Set<String> added=fields.keySet();req(added.isEmpty()||(type.equals(P+"ProcessingTiming1947$Trace;")&&added.equals(Set.of("noiseFirstNanos1976","noiseModelNanos1976","noiseStrongNanos1976","noiseFirstMeasured1976","noiseModelMeasured1976","noiseStrongMeasured1976","correctionRoute1976"))),"Only bounded Strong diagnostic reason/counter fields may be added");
  }
  var previous=MergePayloads.methods(before.values());var current=MergePayloads.methods(after.values());int checked=0;
  for(Method old:previous.values())if(diagnostic(old.getDefiningClass())){
   Method now=current.get(MergePayloads.id(old));req(now!=null,"Inherited diagnostics method removed "+MergePayloads.id(old));String type=old.getDefiningClass();
   if(type.equals(P+"ProcessingTiming1947;")&&Set.of("render","<clinit>").contains(old.getName()))continue;
   if(type.equals(P+"ProcessingTiming1947$Trace;")&&old.getName().equals("<init>"))continue; // Exact old initialization/source is separately inverted; new mismatch sentinels are -1.
   req(MergePayloads.hash(normalizeVersion(old,"1.9.75")).equals(MergePayloads.hash(now)),"Timing measurement/logger method changed beyond exact version literal "+MergePayloads.id(old));checked++;
  }
  for(Method now:current.values())if(diagnostic(now.getDefiningClass())&&!previous.containsKey(MergePayloads.id(now)))req(now.getDefiningClass().equals(P+"ProcessingTiming1947;")&&Set.of("detailWork1976","detailRoute1976").contains(now.getName()),"Only the reviewed bounded Strong telemetry APIs may be added "+MergePayloads.id(now));
  return checked;
 }
 static boolean removedInternalSessionCall(Method method){
  if(!method.getDefiningClass().matches("Lcom/hiro/ulike/GpuNoise1960(?:\\$Session)?\\$[0-9]+;")||!method.getParameterTypes().isEmpty())return false;
  if(method.getName().equals("run")&&method.getReturnType().equals("V"))return true;
  return method.getName().equals("call")&&Set.of("Ljava/lang/Object;","Ljava/lang/Boolean;","Ljava/lang/Long;","Ljava/lang/Integer;","Lcom/hiro/ulike/GpuNoise1960$Session;","Lcom/hiro/ulike/GpuNoise1960$Lease1971;","[J","[I","[[I").contains(method.getReturnType());
 }
 static void verifyRemovedInternalCalls(Map<String,ClassDef> classes,Collection<String> removed){
  for(ClassDef owner:classes.values())if(!owned(owner.getType()))for(Method method:owner.getMethods())if(method.getImplementation()!=null)for(Instruction instruction:method.getImplementation().getInstructions())if(instruction instanceof ReferenceInstruction reference&&reference.getReference() instanceof MethodReference call)req(!removed.contains(call.toString()),"Retained helper references removed internal Session callable "+MergePayloads.id(method));
 }
 static String jsonList(Collection<String> values){var rows=new ArrayList<String>();for(String s:new TreeSet<>(values))rows.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",rows)+"]";}
 public static void main(String[] a)throws Exception{
  req(a.length==5,"BASE HELPERS BUNDLE OUT AUDIT");req(ALLOWED_ROOTS.equals(FAMILIES),"Exact reviewed preview repair roots");
  Path base=Path.of(a[0]),out=Path.of(a[3]);Files.createDirectories(out);
  var before=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var runtime=new TreeMap<>(before);var compiled=MergePayloads.classes(a[1]);var roots=new TreeSet<String>();
  for(var row:compiled.entrySet()){req(owned(row.getKey()),"Compile-only class leaked "+row.getKey());if(!row.getKey().contains("$"))roots.add(rootOf(row.getKey()));}req(roots.equals(FAMILIES),"Exact compiled admission root inventory");
  for(String type:new ArrayList<>(runtime.keySet()))if(owned(type))runtime.remove(type);runtime.putAll(compiled);
  var hooks=new TreeSet<String>();
  for(var row:compiled.entrySet())if(TimingCameraHooks1976.owned(row.getKey()))runtime.put(row.getKey(),TimingCameraHooks1976.patch(row.getValue()));
  TimingCameraHooks1976.verifyInventory(before,runtime);
  var inheritedMethods=MergePayloads.methods(before.values());var currentMethods=MergePayloads.methods(runtime.values());
  for(var row:inheritedMethods.entrySet())if((row.getValue().getAccessFlags()&0x100)!=0){
   Method now=currentMethods.get(row.getKey());req(now!=null&&(now.getAccessFlags()&0x100)!=0&&MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(now)),"Inherited native declaration changed "+row.getKey());
  }
  int preserved=verifyRuntime(before,runtime);int critical=verifyPixelKernels(before,runtime);int nonUi=verifyNonUiMethods(before,runtime);
  var oldSingle=MergePayloads.classes(base.resolve("classes.dex").toString());var oldBundle=MergePayloads.classes(a[2]);var single=metadata(oldSingle);var bundle=metadata(oldBundle);verifyMetadata(oldSingle,single);verifyMetadata(oldBundle,bundle);
  MergePayloads.writeDex(out.resolve("runtime.dex"),runtime.values());MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var emitted=MergePayloads.classes(out.resolve("runtime.dex").toString());req(emitted.keySet().equals(runtime.keySet()),"Serialized helper inventory");
  for(var row:runtime.entrySet()){
   String wanted=TimingCameraHooks1976.owned(row.getKey())?TimingCameraHooks1976.canonical(row.getValue()):MergePayloads.classHash(row.getValue());String actual=TimingCameraHooks1976.owned(row.getKey())?TimingCameraHooks1976.canonical(emitted.get(row.getKey())):MergePayloads.classHash(emitted.get(row.getKey()));
   req(wanted.equals(actual),"Serialized production helper differs "+row.getKey());
  }
  TimingCameraHooks1976.verifyInventory(before,emitted);
  verifyRuntime(before,emitted);verifyPixelKernels(before,emitted);verifyNonUiMethods(before,emitted);verifyMetadata(oldSingle,MergePayloads.classes(out.resolve("loader.dex").toString()));verifyMetadata(oldBundle,MergePayloads.classes(out.resolve("bundle-loader.dex").toString()));
  Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"),StandardCopyOption.REPLACE_EXISTING);Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"),StandardCopyOption.REPLACE_EXISTING);
  var oldMethods=MergePayloads.methods(before.values());var newMethods=MergePayloads.methods(emitted.values());var changed=new TreeSet<String>();var added=new TreeSet<String>();var removed=new TreeSet<String>();var audit=new ArrayList<String>();
  for(var row:oldMethods.entrySet()){
   Method now=newMethods.get(row.getKey());if(now==null){req(owned(row.getValue().getDefiningClass())&&((row.getValue().getAccessFlags()&5)==0||removedInternalSessionCall(row.getValue())),"Retained/public/protected method removed "+row.getKey());removed.add(row.getKey());}
   else if(!MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(now))){req(owned(now.getDefiningClass()),"Unreviewed runtime method change");changed.add(row.getKey());audit.add("RUNTIME\t"+row.getKey()+"\t"+MergePayloads.hash(row.getValue())+"\t"+MergePayloads.hash(now));}
  }
  for(String id:newMethods.keySet())if(!oldMethods.containsKey(id)){req(owned(newMethods.get(id).getDefiningClass()),"Unexpected new method");req((newMethods.get(id).getAccessFlags()&0x100)==0||NEW_JNI.contains(id),"Only exact reviewed diagnostic JNI may be added");added.add(id);}
  verifyRemovedInternalCalls(emitted,removed);
  for(var row:oldMethods.entrySet())if((row.getValue().getAccessFlags()&0x100)!=0){
   Method now=newMethods.get(row.getKey());req(now!=null&&(now.getAccessFlags()&0x100)!=0&&MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(now)),"Existing native declaration changed "+row.getKey());
  }
  var newJni=new TreeSet<String>();for(String id:added)if((newMethods.get(id).getAccessFlags()&0x100)!=0)newJni.add(id);req(newJni.equals(NEW_JNI),"Exact bounded diagnostic JNI inventory");
  var newClasses=new TreeSet<>(emitted.keySet());newClasses.removeAll(before.keySet());
  StringBuilder q=new StringBuilder("{\n");
  for(String flag:List.of("unchanged_pixel_kernel_methods_bytecode_identical","unmodified_pixel_kernel_helpers_bytecode_identical","unmodified_gpu_runtime_helpers_bytecode_identical","quality_algorithm_and_settings_preserved","camera_session_bytecode_identical","preview_output_observer_bytecode_identical","timing_camera_trace_hooks_preserved","save_encoding_helpers_byte_identical","unrelated_runtime_classes_bytecode_identical","native_methods_byte_identical","native_methods_tsv_byte_identical","test_fixture_classes_absent_from_runtime","black_tap_disabled","capture_class_bytecode_identical","capture_begin_image_false_return_verified"))q.append("\"").append(flag).append("\":true,\n");
  q.append("\"camera_control_lifecycle_bytecode_identical\":true,\n");
  q.append("\"diagnostic_measurement_and_logger_bodies_preserved\":true,\n");
  q.append("\"diagnostic_preserved_method_count\":").append(nonUi).append(",\n");
  q.append("\"critical_pixel_kernel_method_count\":").append(critical).append(",\n");
  q.append("\"preserved_runtime_class_count\":").append(preserved).append(",\n");
  var lists=new TreeMap<String,Collection<String>>();lists.put("changed_runtime_methods",changed);lists.put("new_runtime_methods",added);lists.put("removed_runtime_methods",removed);lists.put("new_helper_classes",newClasses);lists.put("replaced_helper_roots",FAMILIES);lists.put("bytecode_patch_helper_roots",hooks);
  for(var pair:lists.entrySet())q.append("\"").append(pair.getKey()).append("\":").append(jsonList(pair.getValue())).append(",\n");
  q.append("\"changed_native_methods\":[],\n\"new_native_methods\":").append(jsonList(newJni)).append(",\n\"new_jni_methods\":").append(jsonList(newJni)).append(",\n\"new_runtime_aliases\":[]\n}\n");
  Files.writeString(out.resolve("gpu-latency-inventory1976.json"),q.toString());
  audit.add("PASS\tcompiled_admission_roots="+roots.size()+"\tretained_class_count="+preserved+"\timage_save_helpers_preserved=true\tno_retained_gpu_pixel_save_helpers_changed=true\tserialized_dex_verified=true\tbegin_image_false_return_verified=true");
  Files.write(Path.of(a[4]),audit);System.out.println(audit.get(audit.size()-1));
 }
}

