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

/** Incremental single-image NR update from exact published .54 runtime. */
public final class Transform1956 {
 static final String P="Lcom/hiro/ulike/";
 static final String OLD_DESCRIPTION="v1.9.55（v1.9.54基準）";
 static final String DESCRIPTION="v1.9.56（v1.9.55基準）H28～H33画質維持高速化。単写NR1～NR4と合成なしを維持し、整数・DCT演算、CPU処理連結、GPU再利用、保存待ちを最適化。実機速度は未測定。";
 static final String INSTALLER="Lapp/hiro/ulike/patches/IntegrationPayload186;";
 static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static final Set<String> ALLOWED_ROOTS=Set.of("CorePixels1950","QualityShadow1932","WholeRoute1953","SaveQueue1935","AsyncSave1935","CodecDrain1945","SpeedWorkers1935","QualityPipeline1932","ProcessingTiming1947");
 static final String BURST="Lcom/hiro/ulike/BurstCapture1933;";
 static final String BEGIN_IMAGE=BURST+"->beginImage(Ljava/lang/Object;Landroid/media/Image;Landroid/hardware/camera2/TotalCaptureResult;)Z";
 static final String BEGIN_IMAGE_PIN="e59cd85b6db190a1796df64442939696a8c18fd8ad220022eb1cdb9509f9ba06";
 static final Set<String> FAMILIES=new TreeSet<>(Arrays.asList(System.getProperty("ulike.production", "CorePixels1950,QualityShadow1932,WholeRoute1953,SaveQueue1935,AsyncSave1935,CodecDrain1945,SpeedWorkers1935,QualityPipeline1932,ProcessingTiming1947").split(",")));
 static final Map<String,String[]> NATIVE_ROWS=new TreeMap<>();
 static final Set<String> SAVE_HOOKS=Set.of(P+"SaveFd186;->publishStageBefore1947(Ljava/lang/String;)Ljava/lang/String;",P+"SaveQuality2;->publishFinalBefore1947(Ljava/lang/String;)Ljava/lang/String;");
 static final ImmutableMethodReference WAIT=new ImmutableMethodReference(P+"AsyncSave1935;","awaitPublication1956",List.of(),"V");
 static void req(boolean b,String s){MergePayloads.require(b,s);}
 static String prop(String name){String v=System.getProperty("ulike."+name,"");req(v.matches(name.endsWith("bytes")?"[1-9][0-9]*":"[a-f0-9]{64}"),"Missing pinned native property: "+name);return v;}
 static boolean owned(String t){if(!t.startsWith(P)||!t.endsWith(";"))return false;String n=t.substring(P.length(),t.length()-1);int dollar=n.indexOf('$');return FAMILIES.contains(dollar<0?n:n.substring(0,dollar));}
 static Method replace(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef members(ClassDef c,Collection<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 static List<Method> methods(ClassDef c){var out=new ArrayList<Method>();for(Method m:c.getMethods())out.add(m);return out;}
 static ClassDef rewriteInstaller(ClassDef c,boolean reverse)throws Exception{
  var ms=new ArrayList<Method>();int hits=0;
  for(Method m:c.getMethods()){
   if(!m.getName().equals("<clinit>")){ms.add(m);continue;}
   var b=new MutableMethodImplementation(m.getImplementation());var regs=new HashMap<Integer,Integer>();var seen=new TreeSet<String>();int rowCount=0;
   for(int at=0;at<b.getInstructions().size();at++){
    Instruction x=b.getInstructions().get(at);
    if(x instanceof ReferenceInstruction rr&&rr.getReference() instanceof StringReference){regs.put(((OneRegisterInstruction)x).getRegisterA(),at);continue;}
    if(!(x instanceof ReferenceInstruction rr)||!(rr.getReference() instanceof TypeReference tr)||!tr.getType().equals("[Ljava/lang/String;"))continue;
    int[] r;
    if(x instanceof FiveRegisterInstruction f){req(((VariableRegisterInstruction)x).getRegisterCount()==4,"Four installer row columns");r=new int[]{f.getRegisterC(),f.getRegisterD(),f.getRegisterE(),f.getRegisterF()};}
    else if(x instanceof RegisterRangeInstruction f){req(f.getRegisterCount()==4,"Four row-range columns");r=new int[]{f.getStartRegister(),f.getStartRegister()+1,f.getStartRegister()+2,f.getStartRegister()+3};}
    else throw new IllegalStateException("Installer row array opcode");
    rowCount++;req(Arrays.stream(r).allMatch(regs::containsKey),"All row registers have explicit strings");
    String resource=((StringReference)((ReferenceInstruction)b.getInstructions().get(regs.get(r[3]))).getReference()).getString();String[] row=NATIVE_ROWS.get(resource);if(row==null)continue;
    req(seen.add(resource),"Unique native resource row");
    for(int j=1;j<=2;j++){int index=regs.get(r[j]);Instruction field=b.getInstructions().get(index);String old=((StringReference)((ReferenceInstruction)field).getReference()).getString();String expected=row[(reverse?2:0)+j-1],target=row[(reverse?0:2)+j-1];req(old.equals(expected),"Pinned installer native field "+resource+" field "+j);req(field.getOpcode()==Opcode.CONST_STRING,"Native field encoding");b.replaceInstruction(index,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)field).getRegisterA(),new ImmutableStringReference(target)));hits++;}
   }
   req(rowCount==9,"Exactly nine existing native installer rows");
   req(seen.equals(NATIVE_ROWS.keySet()),"All native rows found exactly once");ms.add(replace(m,b));
  }
  req(hits==NATIVE_ROWS.size()*2,"Exact native fingerprint replacement count");return members(c,ms);
 }
 static ClassDef installer(ClassDef c)throws Exception{ClassDef changed=rewriteInstaller(c,false);verifyInstaller(c,changed);return changed;}
 static void verifyInstaller(ClassDef before,ClassDef after)throws Exception{req(MergePayloads.classHash(before).equals(MergePayloads.classHash(rewriteInstaller(after,true))),"Installer inverse: all nine rows and transactional safeguards preserved");}
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
 static ClassDef saveHooks(ClassDef before,boolean reverse)throws Exception{
  var ms=new ArrayList<Method>();
  for(Method m:before.getMethods()){
   if(!SAVE_HOOKS.contains(MergePayloads.id(m))){ms.add(m);continue;}
   var body=new MutableMethodImplementation(m.getImplementation());
   if(reverse){Instruction first=body.getInstructions().get(0);req(first.getOpcode()==Opcode.INVOKE_STATIC&&first instanceof ReferenceInstruction r&&r.getReference().equals(WAIT),"Exact H33 publication wait hook required");body.removeInstruction(0);}
   else {String expected=m.getDefiningClass().equals(P+"SaveFd186;")?"38293b342cbcd858e2eddefcd23b0be11c4cc88b6d5ca0086bc9ac5bc7bd96c8":"e45be449418de14f9fab77818f4d704bd921364de1c09c3e0a6af5047338aacd";req(MergePayloads.hash(m).equals(expected),"Pinned .55 H33 source method");body.addInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,0,0,0,0,0,0,WAIT));}
   ms.add(replace(m,body));
  }
  return members(before,ms);
 }
 static void verifyBurst(ClassDef before,ClassDef after)throws Exception{
  req(MergePayloads.classHash(before).equals(MergePayloads.classHash(after)),"Entire .55 no-fusion capture class preserved");
  Method begin=MergePayloads.methods(List.of(after)).get(BEGIN_IMAGE);req(begin!=null,"No-fusion method retained");var ops=new ArrayList<Instruction>();for(Instruction x:begin.getImplementation().getInstructions())ops.add(x);
  req(ops.size()==2&&ops.get(0).getOpcode()==Opcode.CONST_4&&((NarrowLiteralInstruction)ops.get(0)).getNarrowLiteral()==0&&ops.get(1).getOpcode()==Opcode.RETURN,"Existing .55 beginImage returns false");
 }
 static void verifyRuntime(Map<String,ClassDef> before,Map<String,ClassDef> after)throws Exception{
  for(var row:before.entrySet())if(!owned(row.getKey())){
   req(after.containsKey(row.getKey()),"Unrelated runtime class removed "+row.getKey());ClassDef current=after.get(row.getKey());
   if(row.getKey().equals(BURST))verifyBurst(row.getValue(),current);
   else if(row.getKey().equals(P+"SaveFd186;")||row.getKey().equals(P+"SaveQuality2;"))req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(saveHooks(current,true))),"H33 save class inverse: all existing code retained "+row.getKey());
   else req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(current)),"Unrelated runtime class differs "+row.getKey());
  }
  for(String type:after.keySet())if(!before.containsKey(type))req(owned(type),"Unrelated runtime class added "+type);
 }
 static String jsonList(Collection<String> values){var rows=new ArrayList<String>();for(String s:new TreeSet<>(values))rows.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",rows)+"]";}
 public static void main(String[] a)throws Exception{
  req(a.length==6,"BASE HELPERS BUNDLE OUT AUDIT NATIVE_ROWS_TSV");req(ALLOWED_ROOTS.equals(FAMILIES),"Exact reviewed .56 roots");for(String line:Files.readAllLines(Path.of(a[5]))){String[] row=line.split("\\t");req(row.length==5&&NATIVE_ROWS.put(row[0],Arrays.copyOfRange(row,1,5))==null,"Unique native fingerprint TSV row");}req(NATIVE_ROWS.size()==5,"Five native rows updated");
  Path base=Path.of(a[0]),out=Path.of(a[3]);Files.createDirectories(out);
  var before=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var runtime=new TreeMap<>(before);var compiled=MergePayloads.classes(a[1]);var roots=new TreeSet<String>();
  for(var row:compiled.entrySet()){req(owned(row.getKey()),"Compile-only class leaked "+row.getKey());if(!row.getKey().contains("$"))roots.add(row.getKey().substring(P.length(),row.getKey().length()-1));}req(roots.equals(FAMILIES),"Exact compiled production root inventory");
  for(String type:new ArrayList<>(runtime.keySet()))if(owned(type))runtime.remove(type);runtime.putAll(compiled);runtime.put(P+"SaveFd186;",saveHooks(before.get(P+"SaveFd186;"),false));runtime.put(P+"SaveQuality2;",saveHooks(before.get(P+"SaveQuality2;"),false));verifyRuntime(before,runtime);
  var oldSingle=MergePayloads.classes(base.resolve("classes.dex").toString());var oldBundle=MergePayloads.classes(a[2]);var single=metadata(oldSingle);var bundle=metadata(oldBundle);verifyMetadata(oldSingle,single);verifyMetadata(oldBundle,bundle);
  MergePayloads.writeDex(out.resolve("runtime.dex"),runtime.values());MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var serialized=MergePayloads.classes(out.resolve("runtime.dex").toString());req(serialized.keySet().equals(runtime.keySet()),"Serialized runtime class inventory equals exact compiled/preserved map");
  for(var row:runtime.entrySet())req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(serialized.get(row.getKey()))),"Serialized production/preserved class differs "+row.getKey());
  verifyRuntime(before,serialized);runtime=new TreeMap<>(serialized);verifyMetadata(oldSingle,MergePayloads.classes(out.resolve("loader.dex").toString()));verifyMetadata(oldBundle,MergePayloads.classes(out.resolve("bundle-loader.dex").toString()));
  Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"),StandardCopyOption.REPLACE_EXISTING);Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"),StandardCopyOption.REPLACE_EXISTING);
  var oldMethods=MergePayloads.methods(before.values());var newMethods=MergePayloads.methods(runtime.values());var changed=new TreeSet<String>();var added=new TreeSet<String>();var removed=new TreeSet<String>();var audit=new ArrayList<String>();
  for(var row:oldMethods.entrySet()){Method now=newMethods.get(row.getKey());if(now==null){req(owned(row.getValue().getDefiningClass()),"Unrelated method removed");removed.add(row.getKey());}else if(!MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(now))){req(owned(now.getDefiningClass())||SAVE_HOOKS.contains(row.getKey()),"Unrelated method changed");changed.add(row.getKey());audit.add("RUNTIME\t"+row.getKey()+"\t"+MergePayloads.hash(row.getValue())+"\t"+MergePayloads.hash(now));}}
  for(String id:newMethods.keySet())if(!oldMethods.containsKey(id)){req(owned(newMethods.get(id).getDefiningClass()),"Unrelated method added");added.add(id);}
  var newClasses=new TreeSet<>(runtime.keySet());newClasses.removeAll(before.keySet());var jni=new TreeSet<String>();for(String id:added)if((newMethods.get(id).getAccessFlags()&0x100)!=0)jni.add(id);req(jni.stream().allMatch(id->id.startsWith(P+"CorePixels1950;->")),"New JNI declarations limited to reviewed CorePixels1950 endpoints");
  Files.writeString(out.resolve("optimization-inventory1956.json"),"{\n\"changed_runtime_methods\":"+jsonList(changed)+",\n\"changed_native_methods\":[],\n\"new_native_methods\":[],\n\"new_jni_methods\":"+jsonList(jni)+",\n\"new_helper_classes\":"+jsonList(newClasses)+",\n\"replaced_helper_roots\":"+jsonList(FAMILIES)+",\n\"new_runtime_methods\":"+jsonList(added)+",\n\"removed_runtime_methods\":"+jsonList(removed)+",\n\"new_runtime_aliases\":[]\n}\n");
  audit.add("PASS\toptimization_roots="+roots.size()+"\tunrelated_runtime_preserved=true\tinstaller_row_count_preserved=9\tbegin_image_false_return_verified=true\tsave_publication_hooks_inverse_verified=true\tserialized_dex_verified=true");Files.write(Path.of(a[4]),audit);System.out.println(audit.get(audit.size()-1));
 }
}
