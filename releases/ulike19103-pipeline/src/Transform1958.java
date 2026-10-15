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

/** Incremental single-image NR update from exact published .57 runtime. */
public final class Transform1958 {
 static final String P="Lcom/hiro/ulike/";
 static final String OLD_DESCRIPTION="v1.9.57（v1.9.56基準）";
 static final String DESCRIPTION="v1.9.58（v1.9.57基準）NR9～NR13単写ノイズ低減。粒と輪郭の判別、残留ノイズに応じた平坦部の強処理、原寸の類似領域参照、独立した色ノイズ処理、仕上げシャープ抑制を追加。H28～H33を継承し合成なし。ホスト検証済・実機画質と速度は未確認。";
 static final String INSTALLER="Lapp/hiro/ulike/patches/IntegrationPayload186;";
 static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static final Set<String> ALLOWED_ROOTS=Set.of("QualityPipeline1932","QualityPixels1932","StrongNoise1958","NativeMoire1951","FinishPolicy1953","ProcessingTiming1947");
 static final Set<String> PRESERVED_ROOTS=Set.of("CorePixels1950","QualityShadow1932","WholeRoute1953","SaveQueue1935","AsyncSave1935","CodecDrain1945","SpeedWorkers1935","SingleNoise1955","StrongNoise1957");
 static final String BURST="Lcom/hiro/ulike/BurstCapture1933;";
 static final String BEGIN_IMAGE=BURST+"->beginImage(Ljava/lang/Object;Landroid/media/Image;Landroid/hardware/camera2/TotalCaptureResult;)Z";
 static final String BEGIN_IMAGE_PIN="e59cd85b6db190a1796df64442939696a8c18fd8ad220022eb1cdb9509f9ba06";
 static final Set<String> FAMILIES=new TreeSet<>(Arrays.asList(System.getProperty("ulike.production", "QualityPipeline1932,QualityPixels1932,StrongNoise1958,NativeMoire1951,FinishPolicy1953,ProcessingTiming1947").split(",")));
 static void req(boolean b,String s){MergePayloads.require(b,s);}
 static String prop(String name){String v=System.getProperty("ulike."+name,"");req(v.matches(name.endsWith("bytes")?"[1-9][0-9]*":"[a-f0-9]{64}"),"Missing pinned native property: "+name);return v;}
 static boolean owned(String t){if(!t.startsWith(P)||!t.endsWith(";"))return false;String n=t.substring(P.length(),t.length()-1);int dollar=n.indexOf('$');return FAMILIES.contains(dollar<0?n:n.substring(0,dollar));}
 static Method replace(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef members(ClassDef c,Collection<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 static List<Method> methods(ClassDef c){var out=new ArrayList<Method>();for(Method m:c.getMethods())out.add(m);return out;}
 static String[] nativeRow(){return new String[]{"lib/arm64-v8a/libulike_smooth1958.so",prop("smooth1958.sha"),prop("smooth1958.bytes"),"ulike1958/runtime/libulike_smooth1958.so"};}
 static ClassDef installer(ClassDef c)throws Exception{
  var ms=new ArrayList<Method>();int initHits=0,guardHits=0;
  for(Method m:c.getMethods()){
   if(m.getName().equals("<clinit>")){
    var b=new MutableMethodImplementation(m.getImplementation());
    req(b.getRegisterCount()==13&&b.getInstructions().size()==64,"Pinned .57 ten-row initializer shape");
    Instruction outer=b.getInstructions().get(60);
    req(outer.getOpcode()==Opcode.FILLED_NEW_ARRAY_RANGE&&outer instanceof RegisterRangeInstruction&&((RegisterRangeInstruction)outer).getStartRegister()==0&&((RegisterRangeInstruction)outer).getRegisterCount()==10,"Pinned .57 ten-row initializer");
    String[] values=nativeRow();int at=60;
    for(int j=0;j<4;j++)b.addInstruction(at++,new BuilderInstruction21c(Opcode.CONST_STRING,10+j,new ImmutableStringReference(values[j])));
    b.addInstruction(at++,new BuilderInstruction35c(Opcode.FILLED_NEW_ARRAY,4,10,11,12,13,0,new ImmutableTypeReference("[Ljava/lang/String;")));
    b.addInstruction(at++,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,10));
    b.replaceInstruction(66,new BuilderInstruction3rc(Opcode.FILLED_NEW_ARRAY_RANGE,0,11,new ImmutableTypeReference("[[Ljava/lang/String;")));
    ms.add(replace(m,new ImmutableMethodImplementation(14,b.getInstructions(),b.getTryBlocks(),b.getDebugItems())));initHits++;
   }else if(m.getName().equals("install")){
    var b=new MutableMethodImplementation(m.getImplementation());Instruction old=b.getInstructions().get(3);
    req(old.getOpcode()==Opcode.CONST_16&&old instanceof NarrowLiteralInstruction&&((NarrowLiteralInstruction)old).getNarrowLiteral()==10&&b.getInstructions().get(2).getOpcode()==Opcode.ARRAY_LENGTH,"Pinned .57 installer count guard");
    b.replaceInstruction(3,new BuilderInstruction21s(Opcode.CONST_16,((OneRegisterInstruction)old).getRegisterA(),11));ms.add(replace(m,b));guardHits++;
   }else ms.add(m);
  }
  req(initHits==1&&guardHits==1,"Exactly one appended native row and count guard");
  ClassDef after=members(c,ms);verifyInstaller(c,after);return after;
 }
 static void verifyInstaller(ClassDef before,ClassDef after)throws Exception{
  var restored=new ArrayList<Method>();int initHits=0,guardHits=0;
  for(Method m:after.getMethods()){
   if(m.getName().equals("<clinit>")){
    var b=new MutableMethodImplementation(m.getImplementation());req(b.getRegisterCount()==14&&b.getInstructions().size()==70,"Serialized .58 eleven-row initializer shape");String[] values=nativeRow();
    for(int j=0;j<4;j++){Instruction x=b.getInstructions().get(60+j);req(x.getOpcode()==Opcode.CONST_STRING&&x instanceof ReferenceInstruction&&((ReferenceInstruction)x).getReference() instanceof StringReference&&((StringReference)((ReferenceInstruction)x).getReference()).getString().equals(values[j])&&((OneRegisterInstruction)x).getRegisterA()==10+j,"Serialized added native row field "+j);}
    Instruction outer=b.getInstructions().get(66);req(outer.getOpcode()==Opcode.FILLED_NEW_ARRAY_RANGE&&((RegisterRangeInstruction)outer).getStartRegister()==0&&((RegisterRangeInstruction)outer).getRegisterCount()==11,"Serialized eleven-row outer array");
    for(int j=0;j<6;j++)b.removeInstruction(60);
    b.replaceInstruction(60,new BuilderInstruction3rc(Opcode.FILLED_NEW_ARRAY_RANGE,0,10,new ImmutableTypeReference("[[Ljava/lang/String;")));
    restored.add(replace(m,new ImmutableMethodImplementation(13,b.getInstructions(),b.getTryBlocks(),b.getDebugItems())));initHits++;
   }else if(m.getName().equals("install")){
    var b=new MutableMethodImplementation(m.getImplementation());Instruction x=b.getInstructions().get(3);req(x.getOpcode()==Opcode.CONST_16&&((NarrowLiteralInstruction)x).getNarrowLiteral()==11&&b.getInstructions().get(2).getOpcode()==Opcode.ARRAY_LENGTH,"Serialized eleven-row count guard");
    b.replaceInstruction(3,new BuilderInstruction21s(Opcode.CONST_16,((OneRegisterInstruction)x).getRegisterA(),10));restored.add(replace(m,b));guardHits++;
   }else restored.add(m);
  }
  req(initHits==1&&guardHits==1&&MergePayloads.classHash(before).equals(MergePayloads.classHash(members(after,restored))),"Installer inverse: all ten original rows and transactional safeguards preserved");
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
  req(MergePayloads.classHash(before).equals(MergePayloads.classHash(after)),"Entire .57 no-fusion capture class preserved");
  Method begin=MergePayloads.methods(List.of(after)).get(BEGIN_IMAGE);req(begin!=null,"No-fusion method retained");var ops=new ArrayList<Instruction>();for(Instruction x:begin.getImplementation().getInstructions())ops.add(x);
  req(ops.size()==2&&ops.get(0).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)ops.get(0)).getNarrowLiteral()==0&&ops.get(1).getOpcode()==Opcode.RETURN,"Existing .57 beginImage returns false");
 }
 static void verifyRuntime(Map<String,ClassDef> before,Map<String,ClassDef> after)throws Exception{
  for(String root:PRESERVED_ROOTS)req(before.containsKey(P+root+";")&&after.containsKey(P+root+";")&&!owned(P+root+";"),"Reviewed inherited helper root retained "+root);
  for(var row:before.entrySet())if(!owned(row.getKey())){req(after.containsKey(row.getKey()),"Unrelated runtime class removed "+row.getKey());if(row.getKey().equals(BURST))verifyBurst(row.getValue(),after.get(row.getKey()));else req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(after.get(row.getKey()))),"Unrelated runtime class differs "+row.getKey());}
  for(String type:after.keySet())if(!before.containsKey(type))req(owned(type),"Unrelated runtime class added "+type);
 }
 static String jsonList(Collection<String> values){var rows=new ArrayList<String>();for(String s:new TreeSet<>(values))rows.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",rows)+"]";}
 public static void main(String[] a)throws Exception{
  req(a.length==5,"BASE HELPERS BUNDLE OUT AUDIT");req(ALLOWED_ROOTS.equals(FAMILIES),"Exact reviewed .57 roots");
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
  var newClasses=new TreeSet<>(runtime.keySet());newClasses.removeAll(before.keySet());var jni=new TreeSet<String>();for(String id:added)if((newMethods.get(id).getAccessFlags()&0x100)!=0)jni.add(id);req(jni.equals(Set.of(P+"StrongNoise1958;->nativeAbi()I",P+"StrongNoise1958;->processNative([I[IIIIIIIIIZ[F[I[I[III[II)Z")),"Exactly two reviewed new JNI names and descriptors");
  Files.writeString(out.resolve("nr-inventory1958.json"),"{\n\"changed_runtime_methods\":"+jsonList(changed)+",\n\"changed_native_methods\":[],\n\"new_native_methods\":[],\n\"new_jni_methods\":"+jsonList(jni)+",\n\"new_helper_classes\":"+jsonList(newClasses)+",\n\"replaced_helper_roots\":"+jsonList(FAMILIES)+",\n\"preserved_helper_roots\":"+jsonList(PRESERVED_ROOTS)+",\n\"new_runtime_methods\":"+jsonList(added)+",\n\"removed_runtime_methods\":"+jsonList(removed)+",\n\"new_runtime_aliases\":[]\n}\n");
  audit.add("PASS\tnr_roots="+roots.size()+"\tunrelated_runtime_preserved=true\tinstaller_old_rows_preserved=10\tbegin_image_false_return_verified=true\tserialized_dex_verified=true");Files.write(Path.of(a[4]),audit);System.out.println(audit.get(audit.size()-1));
 }
}
