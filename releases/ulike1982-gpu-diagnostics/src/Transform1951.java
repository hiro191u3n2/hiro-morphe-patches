import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.iface.value.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.immutable.value.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Incremental, pinned transformation from published 1.9.50 to H6-H11. */
public final class Transform1951 {
 static final String P="Lcom/hiro/ulike/";
 static final String OLD_DESCRIPTION="v1.9.50（v1.9.49基準）";
 static final String DESCRIPTION="v1.9.51（v1.9.50基準）H6～H11統合。画素一致を確認した色補正・主ノイズ除去・GPU二段処理・転送削減・保存処理の並列化。成立しない場合は元のCPU経路へ戻す。実機速度・画質は未確認。";
 static final String INSTALLER="Lapp/hiro/ulike/patches/IntegrationPayload186;";
 static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static final Set<String> FAMILIES=Set.of("CorePixels1950","QualityPixels1932","NativeMoire1951","QualityShadow1932","GpuInteger1949","QualityPipeline1932");
 static final String TIMER=P+"ProcessingTiming1947;";
 static final Set<String> TIMER_METHODS=Set.of(TIMER+"->init(Landroid/content/Context;)V",TIMER+"->publish(Lcom/hiro/ulike/ProcessingTiming1947$Trace;Z)V",TIMER+"->render(Lcom/hiro/ulike/ProcessingTiming1947$Trace;)Ljava/lang/String;",TIMER+"->summary()Ljava/lang/String;");
 static void req(boolean b,String s){MergePayloads.require(b,s);}
 static String prop(String name){String v=System.getProperty("ulike."+name,"");req(v.matches(name.endsWith("bytes")?"[1-9][0-9]*":"[a-f0-9]{64}"),"Missing pinned native property: "+name);return v;}
 static boolean owned(String t){if(!t.startsWith(P)||!t.endsWith(";"))return false;String n=t.substring(P.length(),t.length()-1);int dollar=n.indexOf('$');return FAMILIES.contains(dollar<0?n:n.substring(0,dollar));}
 static Method replace(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef members(ClassDef c,Collection<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 static ClassDef timingVersion(ClassDef c){
  req(c!=null&&c.getType().equals(TIMER),"Pinned timer root required");
  var fields=new ArrayList<Field>();var methods=new ArrayList<Method>();int fieldHits=0;Set<String> methodHits=new TreeSet<>();
  for(Field f:c.getFields()){
   EncodedValue value=f.getInitialValue();
   if(f.getName().equals("VERSION")){req(value instanceof StringEncodedValue&&((StringEncodedValue)value).getValue().equals("1.9.50"),"Exact old timer VERSION field");value=new ImmutableStringEncodedValue("1.9.51");fieldHits++;}
   fields.add(new ImmutableField(f.getDefiningClass(),f.getName(),f.getType(),f.getAccessFlags(),value,f.getAnnotations(),f.getHiddenApiRestrictions()));
  }
  for(Method m:c.getMethods()){
   if(m.getImplementation()==null){methods.add(m);continue;}
   var body=new MutableMethodImplementation(m.getImplementation());boolean changed=false;int hits=0;
   for(int i=0;i<body.getInstructions().size();i++){
    Instruction op=body.getInstructions().get(i);
    if(op instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().contains("1.9.50")){
     req(TIMER_METHODS.contains(MergePayloads.id(m))&&op.getOpcode()==Opcode.CONST_STRING,"Only exact timer VERSION literals may change");
     body.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)op).getRegisterA(),new ImmutableStringReference(s.getString().replace("1.9.50","1.9.51"))));hits++;changed=true;
    }
   }
   if(changed){req(hits==1,"One version literal per timing method");methodHits.add(MergePayloads.id(m));}
   methods.add(changed?replace(m,body):m);
  }
  req(fieldHits==1&&methodHits.equals(TIMER_METHODS),"Exact timer VERSION field and four literal sites");
  return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),fields,methods);
 }
 static Map<String,ClassDef> metadata(Map<String,ClassDef> old){
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
 static String[] row(String shortName,String folder){return new String[]{"lib/arm64-v8a/libulike_"+shortName+".so",prop(shortName+".sha"),prop(shortName+".bytes"),folder+"/runtime/libulike_"+shortName+".so"};}
 static void addRow(MutableMethodImplementation b,int index,String[] strings){
  int at=24;
  for(int j=0;j<4;j++)b.addInstruction(at++,new BuilderInstruction21c(Opcode.CONST_STRING,7+j,new ImmutableStringReference(strings[j])));
  b.addInstruction(at++,new BuilderInstruction35c(Opcode.FILLED_NEW_ARRAY,4,7,8,9,10,0,new ImmutableTypeReference("[Ljava/lang/String;")));
  b.addInstruction(at++,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,index));
 }
 static ClassDef installer(ClassDef c){
  var methods=new ArrayList<Method>();int initHits=0,guardHits=0;
  for(Method m:c.getMethods()){
   if(m.getName().equals("<clinit>")){
    var b=new MutableMethodImplementation(m.getImplementation());req(b.getRegisterCount()==7&&b.getInstructions().size()==28,"Pinned .50 installer initializer shape");
    String[][] oldRows={{"lib/arm64-v8a/libulike_gpu1949.so",prop("gpu.oldsha"),prop("gpu.oldbytes"),"ulike1949/runtime/libulike_gpu1949.so"},{"lib/arm64-v8a/libulike_core1950.so",prop("core.oldsha"),prop("core.oldbytes"),"ulike1950/runtime/libulike_core1950.so"}};
    for(int k=0;k<2;k++)for(int j=0;j<4;j++){Instruction x=b.getInstructions().get(12+k*6+j);req(x.getOpcode()==Opcode.CONST_STRING&&x instanceof ReferenceInstruction&&((ReferenceInstruction)x).getReference() instanceof StringReference&&((StringReference)((ReferenceInstruction)x).getReference()).getString().equals(oldRows[k][j]),"Exact .50 installer field "+k+":"+j);}
    b.replaceInstruction(13,new BuilderInstruction21c(Opcode.CONST_STRING,3,new ImmutableStringReference(prop("gpu.sha"))));
    b.replaceInstruction(14,new BuilderInstruction21c(Opcode.CONST_STRING,4,new ImmutableStringReference(prop("gpu.bytes"))));
    b.replaceInstruction(19,new BuilderInstruction21c(Opcode.CONST_STRING,4,new ImmutableStringReference(prop("core.sha"))));
    b.replaceInstruction(20,new BuilderInstruction21c(Opcode.CONST_STRING,5,new ImmutableStringReference(prop("core.bytes"))));
    Instruction outer=b.getInstructions().get(24);req(outer.getOpcode()==Opcode.FILLED_NEW_ARRAY&&outer instanceof FiveRegisterInstruction&&((FiveRegisterInstruction)outer).getRegisterCount()==4,"Pinned .50 four-row initializer");
    addRow(b,4,row("moire1951","ulike1951"));addRow(b,5,row("h8gpu1951","ulike1951"));
    b.replaceInstruction(36,new BuilderInstruction3rc(Opcode.FILLED_NEW_ARRAY_RANGE,0,6,new ImmutableTypeReference("[[Ljava/lang/String;")));
    MethodImplementation result=new ImmutableMethodImplementation(11,b.getInstructions(),b.getTryBlocks(),b.getDebugItems());
    var inverse=new MutableMethodImplementation(result);for(int j=0;j<12;j++)inverse.removeInstruction(24);
    inverse.replaceInstruction(24,new BuilderInstruction35c(Opcode.FILLED_NEW_ARRAY,4,0,1,2,3,0,new ImmutableTypeReference("[[Ljava/lang/String;")));
    inverse.replaceInstruction(13,new BuilderInstruction21c(Opcode.CONST_STRING,3,new ImmutableStringReference(oldRows[0][1])));
    inverse.replaceInstruction(14,new BuilderInstruction21c(Opcode.CONST_STRING,4,new ImmutableStringReference(oldRows[0][2])));
    inverse.replaceInstruction(19,new BuilderInstruction21c(Opcode.CONST_STRING,4,new ImmutableStringReference(oldRows[1][1])));
    inverse.replaceInstruction(20,new BuilderInstruction21c(Opcode.CONST_STRING,5,new ImmutableStringReference(oldRows[1][2])));
    req(MergePayloads.hash(m).equals(MergePayloads.hash(replace(m,new ImmutableMethodImplementation(7,inverse.getInstructions(),inverse.getTryBlocks(),inverse.getDebugItems())))),"Installer initializer inverse");
    methods.add(replace(m,result));initHits++;
   }else if(m.getName().equals("install")){
    var b=new MutableMethodImplementation(m.getImplementation());Instruction i=b.getInstructions().get(3);
    req(i.getOpcode()==Opcode.CONST_4&&i instanceof NarrowLiteralInstruction&&((NarrowLiteralInstruction)i).getNarrowLiteral()==4&&b.getInstructions().get(2).getOpcode()==Opcode.ARRAY_LENGTH,"Pinned .50 installer guard");
    int reg=((OneRegisterInstruction)i).getRegisterA();b.replaceInstruction(3,new BuilderInstruction11n(Opcode.CONST_4,reg,6));Method result=replace(m,b);
    var inverse=new MutableMethodImplementation(result.getImplementation());inverse.replaceInstruction(3,new BuilderInstruction11n(Opcode.CONST_4,reg,4));req(MergePayloads.hash(m).equals(MergePayloads.hash(replace(m,inverse))),"Installer guard inverse");methods.add(result);guardHits++;
   }else methods.add(m);
  }
  req(initHits==1&&guardHits==1,"Exactly one initializer and row guard");return members(c,methods);
 }
 static String jsonList(Collection<String> values){var rows=new ArrayList<String>();for(String s:new TreeSet<>(values))rows.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",rows)+"]";}
 public static void main(String[] args)throws Exception{
  req(args.length==5,"BASE HELPERS BUNDLE OUT AUDIT");Path base=Path.of(args[0]),out=Path.of(args[3]);Files.createDirectories(out);
  var before=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var runtime=new TreeMap<>(before);var compiled=MergePayloads.classes(args[1]);
  Set<String> roots=new TreeSet<>();
  for(var e:compiled.entrySet()){req(owned(e.getKey()),"Compile-only class leaked: "+e.getKey());if(!e.getKey().contains("$"))roots.add(e.getKey().substring(P.length(),e.getKey().length()-1));}
  req(roots.equals(FAMILIES),"Exact compiled production inventory");
  for(String t:new ArrayList<>(runtime.keySet()))if(owned(t))runtime.remove(t);
  runtime.putAll(compiled);
  H8Hooks.apply(runtime,before);
  H8Hooks.verify(before,runtime);
  runtime.put(TIMER,timingVersion(before.get(TIMER)));
  var bm=MergePayloads.methods(before.values());var nm=MergePayloads.methods(runtime.values());Set<String> changed=new TreeSet<>(),added=new TreeSet<>(),removed=new TreeSet<>();var audit=new ArrayList<String>();
  for(var e:bm.entrySet()){
   Method n=nm.get(e.getKey());
   if(n==null){req(owned(e.getValue().getDefiningClass()),"Unrelated method removed");removed.add(e.getKey());}
   else if(!MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(n))){req(owned(n.getDefiningClass())||H8Hooks.changedIds().contains(e.getKey())||TIMER_METHODS.contains(e.getKey()),"Unrelated method changed "+e.getKey());changed.add(e.getKey());audit.add("RUNTIME\t"+e.getKey()+"\t"+MergePayloads.hash(e.getValue())+"\t"+MergePayloads.hash(n));}
  }
  for(String k:nm.keySet())if(!bm.containsKey(k)){req(owned(nm.get(k).getDefiningClass())||H8Hooks.newIds().contains(k),"Unrelated method added "+k);added.add(k);}
  for(var e:before.entrySet())if(!owned(e.getKey())&&!e.getKey().equals(TIMER)&&!e.getKey().equals(H8Hooks.OWNER))req(runtime.containsKey(e.getKey())&&MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(runtime.get(e.getKey()))),"Unrelated runtime class changed "+e.getKey());
  var oldSingle=MergePayloads.classes(base.resolve("classes.dex").toString());var single=metadata(oldSingle);var oldBundle=MergePayloads.classes(args[2]);var bundle=metadata(oldBundle);
  for(var map:List.of(oldSingle,oldBundle))for(var e:map.entrySet())if(!e.getKey().equals(LOADER)&&!e.getKey().equals(INSTALLER))req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash((map==oldSingle?single:bundle).get(e.getKey()))),"Other loader changed");
  MergePayloads.writeDex(out.resolve("runtime.dex"),runtime.values());MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"),StandardCopyOption.REPLACE_EXISTING);Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"),StandardCopyOption.REPLACE_EXISTING);
  var newClasses=new TreeSet<>(runtime.keySet());newClasses.removeAll(before.keySet());
  Files.writeString(out.resolve("optimization-inventory.json"),"{\n\"changed_runtime_methods\":"+jsonList(changed)+",\n\"changed_native_methods\":[],\n\"new_native_methods\":[],\n\"new_helper_classes\":"+jsonList(newClasses)+",\n\"replaced_helper_roots\":"+jsonList(FAMILIES)+",\n\"new_runtime_methods\":"+jsonList(added)+",\n\"removed_runtime_methods\":"+jsonList(removed)+",\n\"new_runtime_aliases\":"+jsonList(H8Hooks.newIds())+"\n}\n");
  audit.add("PASS\toptimization_roots="+roots.size()+"\tchanged_methods="+changed.size()+"\ttiming_version_only=true\tunchanged_native_payload=true");Files.write(Path.of(args[4]),audit);System.out.println(audit.get(audit.size()-1));
 }
}
