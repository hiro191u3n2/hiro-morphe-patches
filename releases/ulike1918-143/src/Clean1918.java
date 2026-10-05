import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;

/** Fail-closed bytecode cleanup of the exact uploaded 188 baseline. No image-math substitutions. */
public final class Clean1918 {
 static final String P="Lcom/hiro/ulike/",CY=P+"CaptureYuv;",SS=P+"StyleStill4;",CAP=P+"StyleStill4$Capture;";
 static final String DESCRIPTION="v1.9.18（v1.8.8基準）指定7設定・高解像度特徴点の強制・AI試験・素材書き出し・診断記録を削除。設定の戻るは撮影画面、撮影画面の戻るは従来の終了処理。実機未検証。";
 static final List<String> PREFIX=List.of(P+"trial181/",P+"trial182/",P+"readback/",P+"StyleExport",P+"StyleSnapshot",P+"StyleMaterial",P+"ModelExport",P+"ModelFiles",P+"ModelLookup",P+"TrialModel",P+"FacePrecision","Lai/onnxruntime/","Lhiro/ulike/model/","Lapp/hiro/ulike/patches/IntegrationPayload181","Lapp/hiro/ulike/patches/IntegrationPayload182");
 static final Set<String> EXTRA=Set.of(P+"StyleStill4$2;",P+"StyleStill4$2$1;",P+"StyleStill4$ComposerHistory;",P+"StyleStill4$ComposerToken;",P+"CaptureYuv$$ExternalSyntheticLambda2;",P+"CaptureYuv$$ExternalSyntheticLambda3;");
 static final Set<String> SSM=Set.of("init","install","lastStatus","note","noteCapture","nodes","composerBegin","composerReplaceBegin","composerResult","composerStatus","composerSummary","access$000","access$100","observe","delivered","failed");
 static final Set<String> CYM=Set.of("note","access$100","reportInput","lambda$install$5","lambda$install$6","resultDetails");
 static final List<String> audit=new ArrayList<>();
 static boolean gone(String t){return EXTRA.contains(t)||PREFIX.stream().anyMatch(t::startsWith);}
 static boolean goneMethod(MethodReference m){return gone(m.getDefiningClass())||(m.getDefiningClass().equals(SS)&&SSM.contains(m.getName()))||(m.getDefiningClass().equals(CY)&&CYM.contains(m.getName()));}
 static boolean goneField(FieldReference f){return f.getDefiningClass().equals(CY)&&f.getName().equals("status") || f.getDefiningClass().equals(SS)&&Set.of("status","prefs","composers","callbacks","nextCapture","latestCapture").contains(f.getName()) || f.getDefiningClass().equals(CAP)&&!f.getName().equals("recorder");}
 static void require(boolean b,String s){MergePayloads.require(b,s);}
 static List<Instruction> instructions(MethodImplementation m){List<Instruction> r=new ArrayList<>();m.getInstructions().forEach(r::add);return r;}
 static String id(MethodReference m){return DexFormatter.INSTANCE.getMethodDescriptor(m);}
 static String fid(FieldReference f){return DexFormatter.INSTANCE.getFieldDescriptor(f);}
 static void nop(Map<Integer,List<Instruction>> edits,List<Instruction> ins,int n){List<Instruction> v=new ArrayList<>();for(int k=0;k<ins.get(n).getCodeUnits();k++)v.add(new ImmutableInstruction10x(Opcode.NOP));edits.put(n,v);}
 static void range(Map<Integer,List<Instruction>> edits,List<Instruction> ins,int from,int to){for(int n=from;n<=to;n++)nop(edits,ins,n);}
 static void sameSize(Map<Integer,List<Instruction>> edits,List<Instruction> ins,int n,Instruction replacement){List<Instruction> v=new ArrayList<>();v.add(replacement);int remaining=ins.get(n).getCodeUnits()-replacement.getCodeUnits();require(remaining>=0,"Replacement expands instruction");while(remaining-->0)v.add(new ImmutableInstruction10x(Opcode.NOP));edits.put(n,v);}
 static Method body(Method m,MethodImplementation impl){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),impl);}
 static Method transform(Method m){
  if(m.getImplementation()==null)return m;
  String owner=m.getDefiningClass(),name=m.getName(),key=id(m);var imp=m.getImplementation();var ins=instructions(imp);Map<Integer,List<Instruction>> edits=new TreeMap<>();
  // Remove instrumentation calls. Replace diagnostic token results, never original method arguments.
  for(int n=0;n<ins.size();n++){
   Instruction i=ins.get(n);
   if(owner.equals(CY)&&name.equals("install")&&n>=25)continue;
   if(owner.equals(CAP)&&name.equals("<init>"))continue;
   if(i instanceof ReferenceInstruction ri&&ri.getReference() instanceof MethodReference ref&&goneMethod(ref)){
    require(i.getOpcode().name.startsWith("invoke-static"),"Unexpected nonstatic deletion in "+key+" -> "+id(ref));
    nop(edits,ins,n);
    if(!ref.getReturnType().equals("V")){
     require(n+1<ins.size()&&ins.get(n+1).getOpcode()==Opcode.MOVE_RESULT_OBJECT,"Unexpected result form "+key);
     int result=((OneRegisterInstruction)ins.get(n+1)).getRegisterA();nop(edits,ins,n+1);
     if(!ref.getDefiningClass().equals(P+"FacePrecision3;")){
      Instruction value=ref.getReturnType().equals("Ljava/lang/String;")?new ImmutableInstruction21c(Opcode.CONST_STRING,result,new ImmutableStringReference("")):new ImmutableInstruction21s(Opcode.CONST_16,result,0);
      sameSize(edits,ins,n,value);
     } else require(result==8,"Face precision argument register changed");
    }
   }
   if(i instanceof ReferenceInstruction ri&&ri.getReference() instanceof StringReference sr&&sr.getString().startsWith("v1.8.8 倍率・接写UI")){
    sameSize(edits,ins,n,new ImmutableInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)i).getRegisterA(),new ImmutableStringReference(DESCRIPTION)));
   }
  }
  boolean noTries=false,trimSettingsTry=false;
  if(owner.equals(CY)){
   if(name.equals("install")){require(ins.size()==39,"CaptureYuv install baseline drift");range(edits,ins,25,34);range(edits,ins,36,37);}
   if(name.equals("init")){require(ins.size()==28,"CaptureYuv init drift");range(edits,ins,17,22);}
   if(name.equals("<clinit>")){range(edits,ins,0,1);}
  }
  if(owner.equals(SS)){
   if(name.equals("<clinit>")){range(edits,ins,6,13);}
   if(name.equals("choose")){
    require(ins.size()==67,"choose baseline drift");range(edits,ins,8,18);range(edits,ins,39,49);range(edits,ins,55,65);
    // Retain the real capture route token (recorder), not diagnostic IDs or labels.
    range(edits,ins,25,31);sameSize(edits,ins,33,new ImmutableInstruction21s(Opcode.CONST_WIDE_16,2,0));
    sameSize(edits,ins,34,new ImmutableInstruction11n(Opcode.CONST_4,4,0));nop(edits,ins,36);range(edits,ins,52,54);noTries=true;
   }
   if(name.equals("configureRequest")){
    require(ins.size()==125,"configureRequest baseline drift");range(edits,ins,15,25);range(edits,ins,67,103);range(edits,ins,109,122);
    // Jump past removed callback diagnostics, including their exception handler blocks.
    int offset=0;for(int n=67;n<104;n++)offset+=ins.get(n).getCodeUnits();sameSize(edits,ins,67,new ImmutableInstruction20t(Opcode.GOTO_16,offset));trimSettingsTry=true;
   }
  }
  if(owner.equals(CAP)&&name.equals("<init>")){
   // Only weak recorder reference is needed for strict capture routing.
   List<Instruction> core=new ArrayList<>(ins.subList(0,4));core.add(new ImmutableInstruction10x(Opcode.RETURN_VOID));
   Method out=body(m,new ImmutableMethodImplementation(imp.getRegisterCount(),core,List.of(),List.of()));audit.add("CHANGED\t"+key+"\tcapture_token_no_diagnostic_fields");return out;
  }
  if(owner.equals(P+"BackExit185;")&&name.equals("request")){
   require(ins.size()==26&&imp.getRegisterCount()==4,"Back baseline drift");
   MutableMethodImplementation b=new MutableMethodImplementation(imp);Label original=b.newLabelForIndex(0);
   b.addInstruction(0,new BuilderInstruction10x(Opcode.RETURN_VOID));
   b.addInstruction(0,new BuilderInstruction21t(Opcode.IF_EQZ,0,original));
   b.addInstruction(0,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
   b.addInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,3,0,0,0,0,new ImmutableMethodReference(P+"SettingsReturn1918;","returnToCameraIfSettings",List.of("Landroid/app/Activity;"),"Z")));
   Method out=body(m,b);audit.add("CHANGED\t"+key+"\tsettings_gate_then_unchanged_camera_exit");return out;
  }
  if(edits.isEmpty())return m;
  List<Instruction> out=new ArrayList<>();for(int n=0;n<ins.size();n++)out.addAll(edits.getOrDefault(n,List.of(ins.get(n))));
  List<TryBlock<? extends ExceptionHandler>> tries=new ArrayList<>();
  if(!noTries)for(var t:imp.getTryBlocks()){
   if(trimSettingsTry&&t.getStartCodeAddress()>=0x0087&&t.getStartCodeAddress()<0x00cf)continue;
   tries.add(t);
  }
  // All edits retain exact code-unit addresses. Try/catch of non-diagnostic operations is retained.
  int before=ins.stream().mapToInt(Instruction::getCodeUnits).sum(),after=out.stream().mapToInt(Instruction::getCodeUnits).sum();require(before==after,"Address mismatch "+key);
  for(var t:tries)for(var h:t.getExceptionHandlers()){int address=0;for(int n=0;n<ins.size();n++){if(address==h.getHandlerCodeAddress()&&ins.get(n).getOpcode()==Opcode.MOVE_EXCEPTION)require(!edits.containsKey(n),"Changed retained exception entry "+key);address+=ins.get(n).getCodeUnits();}}
  Method result=body(m,new ImmutableMethodImplementation(imp.getRegisterCount(),out,tries,imp.getDebugItems()));
  audit.add("CHANGED\t"+key+"\t"+edits.size()+"_instruction_sites");return result;
 }
 static Map<String,ClassDef> clean(Map<String,ClassDef> before){
  Map<String,ClassDef> out=new TreeMap<>();
  for(ClassDef c:before.values()){
   if(gone(c.getType())){audit.add("REMOVED_CLASS\t"+c.getType());continue;}
   List<Method> ms=new ArrayList<>();for(Method m:c.getMethods())if(goneMethod(m))audit.add("REMOVED_METHOD\t"+id(m));else ms.add(transform(m));
   List<Field> fs=new ArrayList<>();for(Field f:c.getFields())if(goneField(f))audit.add("REMOVED_FIELD\t"+fid(f));else fs.add(f);
   // MemberClasses annotations can list the physically removed UI/diagnostic nested classes.
   List<Annotation> annotations=new ArrayList<>();for(Annotation a:c.getAnnotations())if(!a.getType().equals("Ldalvik/annotation/MemberClasses;"))annotations.add(a);
   boolean untouched=true;var olds=MergePayloads.methods(List.of(c));for(Method m:ms)if(!MergePayloads.hash(m).equals(MergePayloads.hash(olds.get(id(m)))))untouched=false;
   if(ms.size()!=olds.size()||fs.size()!=((Collection<?>)new ArrayList<Field>(){{c.getFields().forEach(this::add);}}).size())untouched=false;
   if(untouched){out.put(c.getType(),c);continue;}
   out.put(c.getType(),new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),annotations,fs,ms));
  }
  return out;
 }
 static void verify(Collection<ClassDef> classes,Set<String> droppedMethods){
  for(ClassDef c:classes){
   require(!gone(c.getType()),"Deleted class survives");
   for(Field f:c.getFields()){require(!goneField(f)&&!gone(f.getType()),"Deleted field/type survives "+fid(f));}
   for(Method m:c.getMethods()){
    require(!goneMethod(m),"Deleted method survives "+id(m));
    for(CharSequence p:m.getParameterTypes())require(!gone(p.toString()),"Removed parameter type "+id(m));
    if(m.getImplementation()==null)continue;var ins=instructions(m.getImplementation());Set<Integer> addresses=new HashSet<>();int addr=0;
    for(int n=0;n<ins.size();n++){
     var i=ins.get(n);addresses.add(addr);addr+=i.getCodeUnits();
     if(i instanceof ReferenceInstruction ri){
      var ref=ri.getReference();
      if(ref instanceof MethodReference mr)require(!goneMethod(mr)&&!droppedMethods.contains(id(mr)),"Dangling removed method "+id(m)+" => "+id(mr));
      if(ref instanceof FieldReference fr)require(!goneField(fr)&&!gone(fr.getDefiningClass())&&!gone(fr.getType()),"Dangling removed field "+id(m)+" => "+fid(fr));
      if(ref instanceof TypeReference tr)require(!gone(tr.getType()),"Dangling removed type "+id(m)+" => "+tr.getType());
      if(ref instanceof StringReference sr){String s=sr.getString();for(String label:List.of("顔特徴点の高解像度検出","保存専用入力の状態","美顔 AI 実機試験","美顔の形状・透明度を取得","対象スタイルのAIモデルを保存","スタイルの素材・設定を端末に保存","保存専用の美顔撮影診断"))require(!s.contains(label),"Removed label survives "+s);}
     }
     if(i.getOpcode()==Opcode.MOVE_RESULT||i.getOpcode()==Opcode.MOVE_RESULT_OBJECT||i.getOpcode()==Opcode.MOVE_RESULT_WIDE){require(n>0&&(ins.get(n-1).getOpcode().name.startsWith("invoke-")||ins.get(n-1).getOpcode().name.startsWith("filled-new-array")),"Orphan move-result "+id(m));}
    }
    addresses.add(addr);int offset=0;for(var i:ins){if(i instanceof OffsetInstruction jump)require(addresses.contains(offset+jump.getCodeOffset()),"Bad branch target "+id(m));offset+=i.getCodeUnits();}
    for(var t:m.getImplementation().getTryBlocks())for(var h:t.getExceptionHandlers()){require(addresses.contains(h.getHandlerCodeAddress()),"Bad handler "+id(m));}
   }
  }
 }
 public static void main(String[] a)throws Exception{
  require(a.length==5,"BASE_DIR HELPER_DEX BUNDLE_DEX OUT AUDIT");Path base=Path.of(a[0]),out=Path.of(a[3]);Files.createDirectories(out);
  var rt0=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var mt0=MergePayloads.classes(base.resolve("ulike/methods.dex").toString());var ld0=MergePayloads.classes(base.resolve("classes.dex").toString());
  require(rt0.size()==405&&MergePayloads.methods(mt0.values()).size()==170&&ld0.size()==23,"Unexpected 188 baseline counts");
  var tsv=MergePayloads.contracts(base.resolve("ulike/methods.tsv").toString());for(Method m:MergePayloads.methods(mt0.values()).values())require(MergePayloads.hash(m).equals(tsv.get(id(m))[2]),"Bad baseline contract "+id(m));
  var rt=clean(rt0);var mt=clean(mt0);var ld=clean(ld0);var helper=MergePayloads.classes(a[1]);require(helper.size()==2,"Helper/stub leakage");for(ClassDef c:helper.values())require(rt.put(c.getType(),c)==null,"Helper collision");
  Set<String> removed=new HashSet<>();for(String line:audit)if(line.startsWith("REMOVED_METHOD\t"))removed.add(line.split("\t")[1]);verify(rt.values(),removed);verify(mt.values(),removed);verify(ld.values(),removed);
  MergePayloads.writeDex(out.resolve("runtime.dex"),rt.values());MergePayloads.writeDex(out.resolve("methods.dex"),mt.values());MergePayloads.writeDex(out.resolve("loader.dex"),ld.values());
  List<String> rows=new ArrayList<>();var newMethods=MergePayloads.methods(mt.values());require(newMethods.keySet().equals(tsv.keySet()),"Stock contract set changed");for(var e:tsv.entrySet())rows.add(e.getKey()+"\t"+e.getValue()[1]+"\t"+MergePayloads.hash(newMethods.get(e.getKey())));Files.write(out.resolve("methods.tsv"),rows);
  var bundle0=MergePayloads.classes(a[2]);var bundle=new TreeMap<String,ClassDef>();int other=0;for(ClassDef c:bundle0.values())if(!c.getType().startsWith(MergePayloads.PATCH_NS)){bundle.put(c.getType(),c);other++;}require(other==220,"Other app baseline class count changed");bundle.putAll(ld);MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var reread=MergePayloads.classes(out.resolve("bundle-loader.dex").toString());for(ClassDef c:bundle0.values())if(!c.getType().startsWith(MergePayloads.PATCH_NS))require(MergePayloads.classHash(c).equals(MergePayloads.classHash(reread.get(c.getType()))),"Other app changed "+c.getType());
  // Exact camera exit tail retains its instructions and relative branch offsets.
  Method oldBack=MergePayloads.methods(rt0.values()).get(P+"BackExit185;->request(Landroid/app/Activity;)V"),newBack=MergePayloads.methods(rt.values()).get(id(oldBack));
  List<Instruction> oldIns=instructions(oldBack.getImplementation()),newIns=instructions(newBack.getImplementation());
  var tail=new ImmutableMethodImplementation(4,newIns.subList(4,newIns.size()),oldBack.getImplementation().getTryBlocks(),oldBack.getImplementation().getDebugItems());require(MergePayloads.hash(oldBack).equals(MergePayloads.hash(body(oldBack,tail))),"Camera exit tail changed");
  for(String filename:List.of("runtime.dex","methods.dex","loader.dex","bundle-loader.dex")){var cs=MergePayloads.classes(out.resolve(filename).toString());verify(cs.values(),removed);byte[] roundtrip=MergePayloads.dex(cs.values());require(Arrays.equals(roundtrip,Files.readAllBytes(out.resolve(filename))),"DEX serialization unstable "+filename);}
  audit.add("PASS\tremoved_classes="+audit.stream().filter(s->s.startsWith("REMOVED_CLASS")).count()+"\truntime_classes="+rt.size()+"\tstock_methods="+newMethods.size()+"\tulike_loader_classes="+ld.size()+"\tother_app_classes_unchanged="+other);
  Files.write(Path.of(a[4]),audit);System.out.println(audit.get(audit.size()-1));
 }
}
