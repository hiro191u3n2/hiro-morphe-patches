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

/** Incremental conditional GPU update from exact published .61 runtime. */
public final class Transform1962 {
 static final String P="Lcom/hiro/ulike/";
 static final String OLD_DESCRIPTION="v1.9.61（v1.9.60基準）";
 static final String DESCRIPTION="v1.9.62（v1.9.61基準）GX24～GX29。強いノイズ後のGPU内連結、弱いノイズのCPU準備とGPU処理の重なり、配列コピー削減、段階別共有領域、不採用比較の途中打切り、一時的遅化の再資格確認を追加。同一出力・必要精度・既存設定を維持し、非対応時は既存経路へ復帰。ホストGPU検証済・Galaxy実機未測定。";
 static final String INSTALLER="Lapp/hiro/ulike/patches/IntegrationPayload186;";
 static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static final Set<String> ALLOWED_ROOTS=Set.of("QualityPipeline1932","SingleNoise1955","StrongNoise1958","GpuNoise1960","GpuPolicy1960","GpuStrong1960","GpuSingle1960","GpuGeometry1960","FastResize1933","ProcessingTiming1947","GpuAnalysis1961","SpatialNoise1934","GpuProtection1961","FinishPolicy1953","FaceRegions1934Pixels","SingleResidual1961","GpuResidual1961","GpuQualification1961","GpuChain1961","ResidualOverlap1962");
 static final Set<String> PRESERVED_ROOTS=Set.of("CorePixels1950","QualityPixels1932","QualityShadow1932","WholeRoute1953","SaveQueue1935","AsyncSave1935","CodecDrain1945","StrongNoise1957","NativeMoire1951");
 static final String BURST="Lcom/hiro/ulike/BurstCapture1933;";
 static final String BEGIN_IMAGE=BURST+"->beginImage(Ljava/lang/Object;Landroid/media/Image;Landroid/hardware/camera2/TotalCaptureResult;)Z";
 static final String BEGIN_IMAGE_PIN="e59cd85b6db190a1796df64442939696a8c18fd8ad220022eb1cdb9509f9ba06";
 static final Set<String> FAMILIES=new TreeSet<>(Arrays.asList(System.getProperty("ulike.production", "QualityPipeline1932,SingleNoise1955,StrongNoise1958,GpuNoise1960,GpuPolicy1960,GpuStrong1960,GpuSingle1960,GpuGeometry1960,FastResize1933,ProcessingTiming1947,GpuAnalysis1961,SpatialNoise1934,GpuProtection1961,FinishPolicy1953,FaceRegions1934Pixels,SingleResidual1961,GpuResidual1961,GpuQualification1961,GpuChain1961,ResidualOverlap1962").split(",")));
 static final Set<String> SOURCE_PRESERVED=new TreeSet<>(System.getProperty("ulike.sourcepreserved","").isEmpty()?List.of():Arrays.asList(System.getProperty("ulike.sourcepreserved").split(",")));
 static String rootOf(String type){if(!type.startsWith(P))return "";String name=type.substring(P.length(),type.length()-1);int dollar=name.indexOf('$');return dollar<0?name:name.substring(0,dollar);}
 static void req(boolean b,String s){MergePayloads.require(b,s);}
 static String prop(String name){String v=System.getProperty("ulike."+name,"");req(v.matches(name.endsWith("bytes")?"[1-9][0-9]*":"[a-f0-9]{64}"),"Missing pinned native property: "+name);return v;}
 static boolean owned(String t){if(!t.startsWith(P)||!t.endsWith(";"))return false;String n=t.substring(P.length(),t.length()-1);int dollar=n.indexOf('$');return FAMILIES.contains(dollar<0?n:n.substring(0,dollar));}
 static Method replace(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef members(ClassDef c,Collection<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 static List<Method> methods(ClassDef c){var out=new ArrayList<Method>();for(Method m:c.getMethods())out.add(m);return out;}
 static String[] nativeRow(){return new String[]{"lib/arm64-v8a/libulike_gpu1960.so",prop("gpu1960.sha"),prop("gpu1960.bytes"),"ulike1960/runtime/libulike_gpu1960.so"};}
 static ClassDef updateInstaller(ClassDef c,boolean inverse,String[] original)throws Exception{
  var ms=new ArrayList<Method>();int hits=0;
  for(Method m:c.getMethods()){
   if(!m.getName().equals("<clinit>")){ms.add(m);continue;}
   var body=new MutableMethodImplementation(m.getImplementation());var sites=new HashMap<Integer,Integer>();var strings=new HashMap<Integer,String>();
   for(int i=0;i<body.getInstructions().size();i++){
    Instruction ins=body.getInstructions().get(i);
    if(ins.getOpcode()==Opcode.CONST_STRING&&ins instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s){int reg=((OneRegisterInstruction)ins).getRegisterA();sites.put(reg,i);strings.put(reg,s.getString());}
    if(ins.getOpcode()!=Opcode.FILLED_NEW_ARRAY||!(ins instanceof FiveRegisterInstruction row)||row.getRegisterCount()!=4)continue;
    int[] regs={row.getRegisterC(),row.getRegisterD(),row.getRegisterE(),row.getRegisterF()};String[] v=new String[4];for(int k=0;k<4;k++)v[k]=strings.get(regs[k]);
    if(!"lib/arm64-v8a/libulike_gpu1960.so".equals(v[0])||!"ulike1960/runtime/libulike_gpu1960.so".equals(v[3]))continue;
    req(v[1]!=null&&v[1].matches("[a-f0-9]{64}")&&v[2]!=null&&v[2].matches("[1-9][0-9]*"),"Pinned GPU row fingerprint");
    if(!inverse){req(original[0]==null&&original[1]==null,"Unique GPU row");original[0]=v[1];original[1]=v[2];}
    else req(v[1].equals(prop("gpu1960.sha"))&&v[2].equals(prop("gpu1960.bytes")),"Serialized GPU row fingerprint");
    String[] replacement=inverse?original:new String[]{prop("gpu1960.sha"),prop("gpu1960.bytes")};
    for(int k=1;k<=2;k++)body.replaceInstruction(sites.get(regs[k]),new BuilderInstruction21c(Opcode.CONST_STRING,regs[k],new ImmutableStringReference(replacement[k-1])));
    hits++;
   }
   ms.add(replace(m,body));
  }
  req(hits==1,"Exactly existing GPU native row updated");return members(c,ms);
 }
 static ClassDef installer(ClassDef c)throws Exception{
  String[] original=new String[2];ClassDef after=updateInstaller(c,false,original);ClassDef restored=updateInstaller(after,true,original);
  req(MergePayloads.classHash(c).equals(MergePayloads.classHash(restored)),"Installer inverse: all eleven other rows and all transaction code retained");return after;
 }
 static void verifyInstaller(ClassDef before,ClassDef after)throws Exception{
  String[] original=new String[2];updateInstaller(before,false,original);ClassDef restored=updateInstaller(after,true,original);
  req(MergePayloads.classHash(before).equals(MergePayloads.classHash(restored)),"Serialized installer inverse: all eleven other rows and twelve-row count preserved");
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
 static void verifyBurst(ClassDef before,ClassDef after)throws Exception{
  req(MergePayloads.classHash(before).equals(MergePayloads.classHash(after)),"Entire .61 no-fusion capture class preserved");
  Method begin=MergePayloads.methods(List.of(after)).get(BEGIN_IMAGE);req(begin!=null,"No-fusion method retained");var ops=new ArrayList<Instruction>();for(Instruction x:begin.getImplementation().getInstructions())ops.add(x);
  req(ops.size()==2&&ops.get(0).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)ops.get(0)).getNarrowLiteral()==0&&ops.get(1).getOpcode()==Opcode.RETURN,"Existing .61 beginImage returns false");
 }
 static void verifyRuntime(Map<String,ClassDef> before,Map<String,ClassDef> after)throws Exception{
  req(FAMILIES.containsAll(SOURCE_PRESERVED),"Frozen source-preserved roots belong to reviewed production inventory");
  for(var row:before.entrySet())if(SOURCE_PRESERVED.contains(rootOf(row.getKey())))req(after.containsKey(row.getKey())&&MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(after.get(row.getKey()))),"Unchanged .61 source root bytecode differs "+row.getKey());
  for(String type:after.keySet())if(SOURCE_PRESERVED.contains(rootOf(type)))req(before.containsKey(type),"Unchanged .61 source root class added "+type);

  for(String root:PRESERVED_ROOTS)req(before.containsKey(P+root+";")&&after.containsKey(P+root+";")&&!owned(P+root+";"),"Reviewed inherited helper root retained "+root);
  for(var row:before.entrySet())if(!owned(row.getKey())){req(after.containsKey(row.getKey()),"Unrelated runtime class removed "+row.getKey());if(row.getKey().equals(BURST))verifyBurst(row.getValue(),after.get(row.getKey()));else req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(after.get(row.getKey()))),"Unrelated runtime class differs "+row.getKey());}
  for(String type:after.keySet())if(!before.containsKey(type))req(owned(type),"Unrelated runtime class added "+type);
 }
 static String jsonList(Collection<String> values){var rows=new ArrayList<String>();for(String s:new TreeSet<>(values))rows.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",rows)+"]";}
 public static void main(String[] a)throws Exception{
  req(a.length==5,"BASE HELPERS BUNDLE OUT AUDIT");req(ALLOWED_ROOTS.equals(FAMILIES),"Exact reviewed .62 roots");
  Path base=Path.of(a[0]),out=Path.of(a[3]);Files.createDirectories(out);
  var before=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var runtime=new TreeMap<>(before);var compiled=MergePayloads.classes(a[1]);var roots=new TreeSet<String>();
  for(var row:compiled.entrySet()){req(owned(row.getKey()),"Compile-only class leaked "+row.getKey());if(!row.getKey().contains("$"))roots.add(row.getKey().substring(P.length(),row.getKey().length()-1));}req(roots.equals(FAMILIES),"Exact compiled production root inventory");
  for(String type:new ArrayList<>(runtime.keySet()))if(owned(type))runtime.remove(type);runtime.putAll(compiled);verifyRuntime(before,runtime);
  var oldSingle=MergePayloads.classes(base.resolve("classes.dex").toString());var oldBundle=MergePayloads.classes(a[2]);var single=metadata(oldSingle);var bundle=metadata(oldBundle);verifyMetadata(oldSingle,single);verifyMetadata(oldBundle,bundle);
  MergePayloads.writeDex(out.resolve("runtime.dex"),runtime.values());MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var serialized=MergePayloads.classes(out.resolve("runtime.dex").toString());req(serialized.keySet().equals(runtime.keySet()),"Serialized runtime class inventory equals exact compiled/preserved map");
  for(var row:runtime.entrySet())req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(serialized.get(row.getKey()))),"Serialized production/preserved class differs "+row.getKey());
  verifyRuntime(before,serialized);runtime=new TreeMap<>(serialized);verifyMetadata(oldSingle,MergePayloads.classes(out.resolve("loader.dex").toString()));verifyMetadata(oldBundle,MergePayloads.classes(out.resolve("bundle-loader.dex").toString()));
  Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"),StandardCopyOption.REPLACE_EXISTING);Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"),StandardCopyOption.REPLACE_EXISTING);
  var oldMethods=MergePayloads.methods(before.values());var newMethods=MergePayloads.methods(runtime.values());var changed=new TreeSet<String>();var added=new TreeSet<String>();var removed=new TreeSet<String>();var audit=new ArrayList<String>();
  for(var row:oldMethods.entrySet()){Method now=newMethods.get(row.getKey());if(now==null){req(owned(row.getValue().getDefiningClass()),"Unrelated method removed");removed.add(row.getKey());}else if(!MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(now))){req(owned(now.getDefiningClass()),"Unrelated method changed");changed.add(row.getKey());audit.add("RUNTIME\t"+row.getKey()+"\t"+MergePayloads.hash(row.getValue())+"\t"+MergePayloads.hash(now));}}
  for(String id:newMethods.keySet())if(!oldMethods.containsKey(id)){req(owned(newMethods.get(id).getDefiningClass()),"Unrelated method added");added.add(id);}
  var newClasses=new TreeSet<>(runtime.keySet());newClasses.removeAll(before.keySet());var jni=new TreeSet<String>();for(String id:added)if((newMethods.get(id).getAccessFlags()&0x100)!=0)jni.add(id);var reviewedJni=new TreeSet<String>();String jniProperty=System.getProperty("ulike.newjni","");if(!jniProperty.isEmpty())reviewedJni.addAll(Arrays.asList(jniProperty.split(",")));req(jni.equals(reviewedJni),"Exact reviewed new JNI names and descriptors");
  Files.writeString(out.resolve("gpu-inventory1962.json"),"{\n\"changed_runtime_methods\":"+jsonList(changed)+",\n\"changed_native_methods\":[],\n\"new_native_methods\":[],\n\"new_jni_methods\":"+jsonList(jni)+",\n\"new_helper_classes\":"+jsonList(newClasses)+",\n\"replaced_helper_roots\":"+jsonList(FAMILIES)+",\n\"preserved_helper_roots\":"+jsonList(PRESERVED_ROOTS)+",\n\"new_runtime_methods\":"+jsonList(added)+",\n\"removed_runtime_methods\":"+jsonList(removed)+",\n\"new_runtime_aliases\":[]\n}\n");
  audit.add("PASS\tnr_roots="+roots.size()+"\tunrelated_runtime_preserved=true\tinstaller_old_rows_preserved=11\tbegin_image_false_return_verified=true\tserialized_dex_verified=true\tsource_preserved_helper_roots_bytecode_identical=true");Files.write(Path.of(a[4]),audit);System.out.println(audit.get(audit.size()-1));
 }
}
