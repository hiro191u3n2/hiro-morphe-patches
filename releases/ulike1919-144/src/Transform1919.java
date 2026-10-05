import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;

/** Restore exact188 face detector overrides over exact1918. Remove reporting only, not capture/save machinery. */
public final class Transform1919 {
 static final String P="Lcom/hiro/ulike/", C=P+"CaptureAdvanced3;", Q=P+"SaveQuality2;";
 static final String DESCRIPTION="v1.9.19（v1.8.8基準）顔特徴点の高解像度検出を設定・実処理とも復元。直近の保存結果と撮影の対応状況・診断の表示・記録・収集を削除。保存失敗通知と設定から撮影へ戻る操作は維持。実機未検証。";
 static final Set<String> CM=Set.of("note","prefix","access$200","access$300","capabilities","show","lambda$show$5","lambda$install$4","sizeList","received","result");
 static final Set<String> QM=Set.of("lastSaveStatus","status186","access$400","access$402");
 static final Set<String> RESTORE=Set.of(P+"FacePrecision3;",P+"FacePrecision3$1;",P+"FacePrecisionPolicy;");
 static final List<String> audit=new ArrayList<>();
 static Map<String,Method> methods188;
 static int restoredHooks=0;
 static boolean gone(String t){return t.startsWith(P+"Native245InputProbe164")||t.startsWith(P+"HdrCapabilities")||Set.of(P+"CaptureAdvanced3$$ExternalSyntheticLambda0;",P+"CaptureAdvanced3$$ExternalSyntheticLambda5;").contains(t);}
 static boolean goneMethod(MethodReference m){return gone(m.getDefiningClass()) || m.getDefiningClass().equals(C)&&CM.contains(m.getName()) || m.getDefiningClass().equals(Q)&&QM.contains(m.getName());}
 static boolean goneField(FieldReference f){return f.getDefiningClass().equals(C)&&f.getName().equals("status") || f.getDefiningClass().equals(Q)&&f.getName().equals("lastStatus") || f.getDefiningClass().equals(P+"CaptureAdvanced3$State;")&&f.getName().equals("actualPixelMode");}
 static void require(boolean b,String s){MergePayloads.require(b,s);}
 static String id(MethodReference m){return DexFormatter.INSTANCE.getMethodDescriptor(m);}
 static String fid(FieldReference m){return DexFormatter.INSTANCE.getFieldDescriptor(m);}
 static List<Instruction> instructions(MethodImplementation m){List<Instruction> r=new ArrayList<>();m.getInstructions().forEach(r::add);return r;}
 static Method body(Method m,MethodImplementation impl){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),impl);}
 record Edit(int end,List<Instruction> value){}
 static class Editing {
  final Method m;final List<Instruction> ins;final int[] addr;final TreeMap<Integer,Edit> edits=new TreeMap<>();
  Editing(Method m){this.m=m;ins=instructions(m.getImplementation());addr=new int[ins.size()+1];for(int i=0;i<ins.size();i++)addr[i+1]=addr[i]+ins.get(i).getCodeUnits();}
  void span(int from,int to,List<Instruction> replacement){
   require(Arrays.binarySearch(addr,from)>=0&&Arrays.binarySearch(addr,to)>=0,"Misaligned edit "+id(m));
   for(var e:edits.entrySet())require(to<=e.getKey()||from>=e.getValue().end,"Overlapping edit "+id(m)+" at "+from);
   var value=new ArrayList<Instruction>(replacement);int size=value.stream().mapToInt(Instruction::getCodeUnits).sum();require(size<=to-from,"Expanding edit "+id(m));
   while(size++<to-from)value.add(new ImmutableInstruction10x(Opcode.NOP));edits.put(from,new Edit(to,value));
  }
  void range(int first,int last){require(first>=0&&last<ins.size(),"Index drift "+id(m));span(addr[first],addr[last+1],List.of());}
  boolean covered(int index){var e=edits.floorEntry(addr[index]);return e!=null&&addr[index]<e.getValue().end;}
  void genericFailure(int index){require(ins.get(index+1).getOpcode()==Opcode.MOVE_RESULT_OBJECT,"Status result drift");int reg=((OneRegisterInstruction)ins.get(index+1)).getRegisterA();span(addr[index],addr[index+2],List.of(new ImmutableInstruction21c(Opcode.CONST_STRING,reg,new ImmutableStringReference("保存に失敗しました。"))));}
  Method finish(){
   if(edits.isEmpty())return m;
   List<Instruction> result=new ArrayList<>();for(int i=0;i<ins.size();){Edit e=edits.get(addr[i]);if(e==null){result.add(ins.get(i++));continue;}result.addAll(e.value);i=Arrays.binarySearch(addr,e.end);require(i>=0,"Edit end drift");}
   require(result.stream().mapToInt(Instruction::getCodeUnits).sum()==addr[ins.size()],"Method addresses changed");
   for(var t:m.getImplementation().getTryBlocks())for(var h:t.getExceptionHandlers()){int i=Arrays.binarySearch(addr,h.getHandlerCodeAddress());require(i>=0&&!covered(i),"Retained exception handler removed "+id(m));}
   Method out=body(m,new ImmutableMethodImplementation(m.getImplementation().getRegisterCount(),result,m.getImplementation().getTryBlocks(),m.getImplementation().getDebugItems()));
   audit.add("CHANGED\t"+id(m)+"\t"+MergePayloads.hash(m)+"\t"+MergePayloads.hash(out)+"\tspans="+edits.size());return out;
  }
 }
 static void ranges(Editing e,int... values){require(values.length%2==0,"Range pairs");for(int i=0;i<values.length;i+=2)e.range(values[i],values[i+1]);}
 static Method transform(Method m){
  if(m.getImplementation()==null)return m;
  Editing e=new Editing(m);String owner=m.getDefiningClass(),name=m.getName();
  if(owner.equals(P+"SaveOptions172;")&&name.equals("install")){require(e.ins.size()==47,"Save options drift");e.range(36,44);}
  if(owner.equals(C)){
   switch(name){
    case "<clinit>":e.range(4,5);break;
    case "init":e.range(18,23);break;
    case "install":require(e.ins.size()==58,"Capture options drift");e.range(46,55);break;
    case "lambda$requestNativeRatio$0":ranges(e,32,43,47,48);break;
    case "restore":ranges(e,81,110,159,172);break;
    case "select":ranges(e,86,97,141,160,181,226,256,278,280,299,307,320);break;
    case "session":e.range(178,179);break;
    case "still":e.range(79,96);break;
   }
  }
  if(owner.equals(P+"CaptureAdvanced3$State;")&&name.equals("<init>")&&m.getParameterTypes().isEmpty())e.range(1,2);
  if(owner.equals(P+"CaptureAdvanced3$1;")&&name.equals("onConfigured"))e.range(14,40);
  if(owner.equals(Q)){
   switch(name){
    case "<clinit>":e.range(5,6);break;
    case "confirmPublish":ranges(e,2,3,96,97,105,106);break;
    case "newStagingPath":e.range(17,18);break;
    case "publishExisting":ranges(e,41,42,49,50,57,58);break;
    case "publishFinal":ranges(e,57,76,101,102);break;
    case "saveFinal":ranges(e,20,21,29,30,104,122,181,182,195,196,210,225,248,249);break;
   }
  }
  if(owner.equals(P+"SaveFd186;")){
   if(name.equals("publishStage"))ranges(e,85,86,134,147);
   if(name.equals("saveStage"))ranges(e,98,99,171,184);
  }
  if(owner.equals(P+"SaveQuality2$4;")&&name.equals("run"))ranges(e,20,21,24,25);
  // Replace old shared last-result error text with a direct failure message. The error is not hidden.
  for(int i=0;i<e.ins.size();i++){
   if(e.covered(i))continue;Instruction ins=e.ins.get(i);
   if(ins instanceof ReferenceInstruction ri&&ri.getReference() instanceof MethodReference ref&&goneMethod(ref)){
    if(ref.getDefiningClass().equals(Q)&&Set.of("lastSaveStatus","access$400").contains(ref.getName())){e.genericFailure(i);audit.add("PRESERVED_FAILURE_NOTIFICATION\t"+id(m));}
    else if(ref.getDefiningClass().equals(C)&&Set.of("received","result").contains(ref.getName())){require(ref.getReturnType().equals("V"),"Diagnostic hook changed");e.range(i,i);}
    else throw new IllegalStateException("Unreviewed removed-method call "+id(m)+" => "+id(ref));
   }
   if(ins instanceof ReferenceInstruction ri&&ri.getReference() instanceof StringReference sr&&sr.getString().startsWith("v1.9.18（v1.8.8基準）"))e.span(e.addr[i],e.addr[i+1],List.of(new ImmutableInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)ins).getRegisterA(),new ImmutableStringReference(DESCRIPTION))));
  }
  // Recover only the four exact face-feature hook spans; never whole old methods (which include discarded trials).
  Method original=methods188.get(id(m));
  if(original!=null&&original.getImplementation()!=null){var old=instructions(original.getImplementation());int offset=0;
   for(int i=0;i<old.size();i++){
    Instruction ins=old.get(i);
    if(ins instanceof ReferenceInstruction ri&&ri.getReference() instanceof MethodReference ref&&ref.getDefiningClass().equals(P+"FacePrecision3;")){
     require(ins.getOpcode().name.startsWith("invoke-static"),"Face hook drift");int end=offset+ins.getCodeUnits();List<Instruction> restored=new ArrayList<>();restored.add(ins);
     if(!ref.getReturnType().equals("V")){require(old.get(i+1).getOpcode()==Opcode.MOVE_RESULT_OBJECT,"Face override result drift");restored.add(old.get(i+1));end+=old.get(i+1).getCodeUnits();}
     int lo=Arrays.binarySearch(e.addr,offset),hi=Arrays.binarySearch(e.addr,end);require(lo>=0&&hi>=0,"Face insertion misalignment");for(int j=lo;j<hi;j++)require(e.ins.get(j).getOpcode()==Opcode.NOP,"Face deleted slot not empty "+id(m));
     e.span(offset,end,restored);restoredHooks++;audit.add("RESTORED_188_HOOK\t"+id(m)+"\t"+offset+"\t"+id(ref));
    }
    offset+=ins.getCodeUnits();
   }
  }
  return e.finish();
 }
 static Map<String,ClassDef> clean(Map<String,ClassDef> before){
  Map<String,ClassDef> result=new TreeMap<>();
  for(ClassDef c:before.values()){
   if(gone(c.getType())){audit.add("REMOVED_CLASS\t"+c.getType());continue;}
   List<Method> methods=new ArrayList<>();boolean changed=false;
   for(Method m:c.getMethods()){if(goneMethod(m)){audit.add("REMOVED_METHOD\t"+id(m));changed=true;}else{Method out=transform(m);methods.add(out);changed|=out!=m;}}
   List<Field> fields=new ArrayList<>();for(Field f:c.getFields())if(goneField(f)){audit.add("REMOVED_FIELD\t"+fid(f));changed=true;}else fields.add(f);
   List<Annotation> annotations=new ArrayList<>();for(Annotation a:c.getAnnotations())if(!a.getType().equals("Ldalvik/annotation/MemberClasses;"))annotations.add(a);
   result.put(c.getType(),changed?new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),annotations,fields,methods):c);
  }
  return result;
 }
 static void verify(Collection<ClassDef> classes){
  for(ClassDef c:classes){require(!gone(c.getType()),"Removed class survived");for(Field f:c.getFields())require(!goneField(f)&&!gone(f.getType()),"Removed field survived");
   for(Method m:c.getMethods()){
    require(!goneMethod(m),"Removed method survived");if(m.getImplementation()==null)continue;var ins=instructions(m.getImplementation());Set<Integer> addresses=new HashSet<>();int addr=0;
    for(int i=0;i<ins.size();i++){
     var op=ins.get(i);addresses.add(addr);addr+=op.getCodeUnits();
     if(op instanceof ReferenceInstruction ri){var ref=ri.getReference();
      if(ref instanceof MethodReference mr)require(!goneMethod(mr),"Dangling method "+id(m)+" => "+id(mr));
      if(ref instanceof FieldReference fr)require(!goneField(fr)&&!gone(fr.getDefiningClass())&&!gone(fr.getType()),"Dangling field "+id(m)+" => "+fid(fr));
      if(ref instanceof TypeReference tr)require(!gone(tr.getType()),"Dangling type "+tr.getType());
      if(ref instanceof StringReference sr&&c.getType().startsWith(P)){String s=sr.getString();for(String label:List.of("直近の保存結果","撮影の対応状況・診断を表示","撮影後に保存結果を表示します。","last_status","保存専用入力の状態","美顔 AI 実機試験","美顔の形状・透明度を取得","対象スタイルのAIモデルを保存","スタイルの素材・設定を端末に保存","保存専用の美顔撮影診断"))require(!s.contains(label),"Removed label/key remains "+id(m)+" "+s);}
     }
     if(op.getOpcode()==Opcode.MOVE_RESULT||op.getOpcode()==Opcode.MOVE_RESULT_OBJECT||op.getOpcode()==Opcode.MOVE_RESULT_WIDE)require(i>0&&(ins.get(i-1).getOpcode().name.startsWith("invoke-")||ins.get(i-1).getOpcode().name.startsWith("filled-new-array")),"Orphan move-result "+id(m));
    }
    addresses.add(addr);int offset=0;for(var op:ins){if(op instanceof OffsetInstruction j)require(addresses.contains(offset+j.getCodeOffset()),"Invalid branch "+id(m));offset+=op.getCodeUnits();}
    for(var t:m.getImplementation().getTryBlocks())for(var h:t.getExceptionHandlers())require(addresses.contains(h.getHandlerCodeAddress()),"Invalid handler "+id(m));
   }
  }
 }
 public static void main(String[] a)throws Exception{
  require(a.length==5,"BASE1918 BASE188 BUNDLE143DEX OUT AUDIT");Path base=Path.of(a[0]),old=Path.of(a[1]),out=Path.of(a[3]);Files.createDirectories(out);
  var rt0=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var mt0=MergePayloads.classes(base.resolve("ulike/methods.dex").toString());var ld0=MergePayloads.classes(base.resolve("classes.dex").toString());
  var rt188=MergePayloads.classes(old.resolve("ulike/runtime.dex").toString());methods188=MergePayloads.methods(MergePayloads.classes(old.resolve("ulike/methods.dex").toString()).values());
  require(rt0.size()==226&&MergePayloads.methods(mt0.values()).size()==170&&ld0.size()==13,"Baseline1918 class count drift");
  var tsv=MergePayloads.contracts(base.resolve("ulike/methods.tsv").toString());for(Method m:MergePayloads.methods(mt0.values()).values())require(MergePayloads.hash(m).equals(tsv.get(id(m))[2]),"Bad baseline method contract");
  var rt=clean(rt0);var mt=clean(mt0);var ld=clean(ld0);require(restoredHooks==4,"Expected four restored hooks, got "+restoredHooks);
  for(String type:RESTORE){require(!rt.containsKey(type)&&rt188.containsKey(type),"Face class drift "+type);rt.put(type,rt188.get(type));audit.add("RESTORED_188_CLASS\t"+type+"\t"+MergePayloads.classHash(rt188.get(type)));}
  verify(rt.values());verify(mt.values());verify(ld.values());
  for(String type:List.of(P+"SettingsReturn1918;",P+"SettingsReturn1918$1;",P+"BackExit185;",P+"ExitJobs185;",P+"FacePrecision3;",P+"FacePrecision3$1;",P+"FacePrecisionPolicy;")){
   ClassDef expected=RESTORE.contains(type)?rt188.get(type):rt0.get(type);require(MergePayloads.classHash(expected).equals(MergePayloads.classHash(rt.get(type))),"Preserved class changed "+type);audit.add("EXACT_PRESERVED_CLASS\t"+type);
  }
  MergePayloads.writeDex(out.resolve("runtime.dex"),rt.values());MergePayloads.writeDex(out.resolve("methods.dex"),mt.values());MergePayloads.writeDex(out.resolve("loader.dex"),ld.values());
  var newMethods=MergePayloads.methods(mt.values());require(newMethods.keySet().equals(tsv.keySet()),"Stock method set drift");List<String> rows=new ArrayList<>();for(var e:tsv.entrySet())rows.add(e.getKey()+"\t"+e.getValue()[1]+"\t"+MergePayloads.hash(newMethods.get(e.getKey())));Files.write(out.resolve("methods.tsv"),rows);
  var bundle0=MergePayloads.classes(a[2]);var bundle=new TreeMap<String,ClassDef>();int others=0;for(ClassDef c:bundle0.values())if(!c.getType().startsWith(MergePayloads.PATCH_NS)){bundle.put(c.getType(),c);others++;}require(others==220,"Other app count drift");bundle.putAll(ld);MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var reread=MergePayloads.classes(out.resolve("bundle-loader.dex").toString());for(ClassDef c:bundle0.values())if(!c.getType().startsWith(MergePayloads.PATCH_NS))require(MergePayloads.classHash(c).equals(MergePayloads.classHash(reread.get(c.getType()))),"Other application class changed");
  // Canonicalize once after pool reconstruction (shared debug-item ordering); method contracts must remain exact.
  for(String f:List.of("runtime.dex","methods.dex","loader.dex","bundle-loader.dex")){
   var first=MergePayloads.classes(out.resolve(f).toString());var hashes=MergePayloads.methods(first.values());MergePayloads.writeDex(out.resolve(f),first.values());var second=MergePayloads.methods(MergePayloads.classes(out.resolve(f).toString()).values());for(var e:hashes.entrySet())require(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(second.get(e.getKey()))),"Canonicalization changed code "+e.getKey());
  }
  for(String f:List.of("runtime.dex","methods.dex","loader.dex","bundle-loader.dex")){var cs=MergePayloads.classes(out.resolve(f).toString());verify(cs.values());require(Arrays.equals(MergePayloads.dex(cs.values()),Files.readAllBytes(out.resolve(f))),"Unstable DEX roundtrip "+f);}
  audit.add("PASS\tremoved_classes="+audit.stream().filter(s->s.startsWith("REMOVED_CLASS")).count()+"\truntime_classes="+rt.size()+"\truntime_methods="+MergePayloads.methods(rt.values()).size()+"\tstock_methods="+newMethods.size()+"\tulike_loader_classes="+ld.size()+"\tother_app_classes_unchanged="+others+"\tface_hooks_restored="+restoredHooks);
  Files.write(Path.of(a[4]),audit);System.out.println(audit.get(audit.size()-1));
 }
}
