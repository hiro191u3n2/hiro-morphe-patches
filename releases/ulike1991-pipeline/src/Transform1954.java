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

/** Incremental, pinned transformation from published 1.9.53 to H22-H27. */
public final class Transform1954 {
 static final String P="Lcom/hiro/ulike/";
 static final String OLD_DESCRIPTION="v1.9.53（v1.9.52基準）";
 static final String DESCRIPTION="v1.9.54（v1.9.53基準）H22～H27統合。GPU処理行数の実測選択、専用受信、正確な補助値・座標の再利用、周辺画素の差分転送、可逆な形式展開と凍結画像の所有権で複製を削減。画素一致を守り既存経路へ復帰。実機速度・画質は未確認。";
 static final String INSTALLER="Lapp/hiro/ulike/patches/IntegrationPayload186;";
 static final String LOADER="Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
 static final Set<String> FAMILIES=Set.of("QualityPipeline1932","ProcessingTiming1947","SaveQueue1935","ShotContext1932","AsyncSave1935","GpuFinish1953","FinishPolicy1953","WholeRoute1953","SpatialNoise1934");
 static void req(boolean b,String s){MergePayloads.require(b,s);}
 static String prop(String name){String v=System.getProperty("ulike."+name,"");req(v.matches(name.endsWith("bytes")?"[1-9][0-9]*":"[a-f0-9]{64}"),"Missing pinned native property: "+name);return v;}
 static boolean owned(String t){if(!t.startsWith(P)||!t.endsWith(";"))return false;String n=t.substring(P.length(),t.length()-1);int dollar=n.indexOf('$');return FAMILIES.contains(dollar<0?n:n.substring(0,dollar));}
 static Method replace(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef members(ClassDef c,Collection<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
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
 static ClassDef installer(ClassDef c){
  var methods=new ArrayList<Method>();int initHits=0;
  for(Method m:c.getMethods()){
   if(m.getName().equals("<clinit>")){
    var b=new MutableMethodImplementation(m.getImplementation());
    req(b.getRegisterCount()==11&&b.getInstructions().size()==52,"Pinned .53 eight-row initializer shape");
    Instruction outer=b.getInstructions().get(48);
    req(outer.getOpcode()==Opcode.FILLED_NEW_ARRAY_RANGE&&outer instanceof RegisterRangeInstruction&&((RegisterRangeInstruction)outer).getStartRegister()==0&&((RegisterRangeInstruction)outer).getRegisterCount()==8,"Pinned .53 eight-row initializer");
    String[] values=row("finish1953","ulike1953");
    Instruction oldSha=b.getInstructions().get(43),oldSize=b.getInstructions().get(44);
    for(int j=0;j<4;j++){Instruction x=b.getInstructions().get(42+j);req(x.getOpcode()==Opcode.CONST_STRING&&x instanceof ReferenceInstruction&&((ReferenceInstruction)x).getReference() instanceof StringReference&&((OneRegisterInstruction)x).getRegisterA()==7+j,"Pinned .53 finishing row constants");}
    req(((StringReference)((ReferenceInstruction)b.getInstructions().get(42)).getReference()).getString().equals(values[0])&&((StringReference)((ReferenceInstruction)b.getInstructions().get(45)).getReference()).getString().equals(values[3]),"Existing finishing filename/resource path required");
    b.replaceInstruction(43,new BuilderInstruction21c(Opcode.CONST_STRING,8,new ImmutableStringReference(values[1])));
    b.replaceInstruction(44,new BuilderInstruction21c(Opcode.CONST_STRING,9,new ImmutableStringReference(values[2])));
    Method result=replace(m,b);var inverse=new MutableMethodImplementation(result.getImplementation());
    inverse.replaceInstruction(43,new BuilderInstruction21c(Opcode.CONST_STRING,8,((ReferenceInstruction)oldSha).getReference()));
    inverse.replaceInstruction(44,new BuilderInstruction21c(Opcode.CONST_STRING,9,((ReferenceInstruction)oldSize).getReference()));
    req(MergePayloads.hash(m).equals(MergePayloads.hash(replace(m,inverse))),"Installer inverse: only existing finish hash/size change; all eight rows and guard retained");
    methods.add(result);initHits++;
   }else methods.add(m);
  }
  req(initHits==1,"Exactly one eight-row initializer");return members(c,methods);
 }
 static void verifyUnownedClass(ClassDef before,ClassDef after)throws Exception{
  boolean targetClass=false;for(Method m:before.getMethods())targetClass|=SaveLeaseHooks1954.isTarget(m);
  if(!targetClass)req(MergePayloads.classHash(before).equals(MergePayloads.classHash(after)),"Unowned runtime class changed "+before.getType());
  req(MergePayloads.classHash(members(before,toMethods(before))).equals(MergePayloads.classHash(members(after,toMethods(before)))),"Unowned runtime class fields/header changed "+before.getType());
  var old=MergePayloads.methods(List.of(before));var now=MergePayloads.methods(List.of(after));req(old.keySet().equals(now.keySet()),"Unowned method inventory changed "+before.getType());
  for(var e:old.entrySet())if(SaveLeaseHooks1954.isTarget(e.getValue()))SaveLeaseHooks1954.verify(e.getValue(),now.get(e.getKey()));else req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(now.get(e.getKey()))),"Unowned method changed "+e.getKey());
 }
 static List<Method> toMethods(ClassDef c){var out=new ArrayList<Method>();for(Method m:c.getMethods())out.add(m);return out;}
 static String jsonList(Collection<String> values){var rows=new ArrayList<String>();for(String s:new TreeSet<>(values))rows.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",rows)+"]";}
 public static void main(String[] args)throws Exception{
  req(args.length==5,"BASE HELPERS BUNDLE OUT AUDIT");Path base=Path.of(args[0]),out=Path.of(args[3]);Files.createDirectories(out);
  var before=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var runtime=new TreeMap<>(before);var compiled=MergePayloads.classes(args[1]);
  Set<String> roots=new TreeSet<>();
  for(var e:compiled.entrySet()){req(owned(e.getKey()),"Compile-only class leaked: "+e.getKey());if(!e.getKey().contains("$"))roots.add(e.getKey().substring(P.length(),e.getKey().length()-1));}
  req(roots.equals(FAMILIES),"Exact compiled production inventory");
  for(String t:new ArrayList<>(runtime.keySet()))if(owned(t))runtime.remove(t);
  runtime.putAll(compiled);
  Set<String> leaseHooks=new TreeSet<>();
  for(ClassDef c:new ArrayList<>(runtime.values())){
   var ms=new ArrayList<Method>();boolean changedHook=false;
   for(Method m:c.getMethods())if(SaveLeaseHooks1954.isTarget(m)){
    Method repaired=SaveLeaseHooks1954.repair(m);SaveLeaseHooks1954.verify(m,repaired);ms.add(repaired);changedHook=true;leaseHooks.add(MergePayloads.id(m));
   }else ms.add(m);
   if(changedHook)runtime.put(c.getType(),members(c,ms));
  }
  req(leaseHooks.equals(SaveLeaseHooks1954.TARGETS.keySet()),"Exactly reviewed final-save recycling hooks required");
  var bm=MergePayloads.methods(before.values());var nm=MergePayloads.methods(runtime.values());Set<String> changed=new TreeSet<>(),added=new TreeSet<>(),removed=new TreeSet<>();var audit=new ArrayList<String>();
  for(var e:bm.entrySet()){
   Method n=nm.get(e.getKey());
   if(n==null){req(owned(e.getValue().getDefiningClass()),"Unrelated method removed");removed.add(e.getKey());}
   else if(!MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(n))){req(owned(n.getDefiningClass())||SaveLeaseHooks1954.isTarget(e.getValue()),"Unrelated method changed "+e.getKey());changed.add(e.getKey());audit.add("RUNTIME\t"+e.getKey()+"\t"+MergePayloads.hash(e.getValue())+"\t"+MergePayloads.hash(n));}
  }
  for(String k:nm.keySet())if(!bm.containsKey(k)){req(owned(nm.get(k).getDefiningClass()),"Unrelated method added "+k);added.add(k);}
  for(var e:before.entrySet())if(!owned(e.getKey())){req(runtime.containsKey(e.getKey()),"Unrelated runtime class removed "+e.getKey());verifyUnownedClass(e.getValue(),runtime.get(e.getKey()));}
  var oldSingle=MergePayloads.classes(base.resolve("classes.dex").toString());var single=metadata(oldSingle);var oldBundle=MergePayloads.classes(args[2]);var bundle=metadata(oldBundle);
  for(var map:List.of(oldSingle,oldBundle))for(var e:map.entrySet())if(!e.getKey().equals(LOADER)&&!e.getKey().equals(INSTALLER))req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash((map==oldSingle?single:bundle).get(e.getKey()))),"Other loader changed");
  MergePayloads.writeDex(out.resolve("runtime.dex"),runtime.values());MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"),StandardCopyOption.REPLACE_EXISTING);Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"),StandardCopyOption.REPLACE_EXISTING);
  var newClasses=new TreeSet<>(runtime.keySet());newClasses.removeAll(before.keySet());
  Files.writeString(out.resolve("optimization-inventory.json"),"{\n\"changed_runtime_methods\":"+jsonList(changed)+",\n\"changed_native_methods\":[],\n\"new_native_methods\":[],\n\"new_helper_classes\":"+jsonList(newClasses)+",\n\"replaced_helper_roots\":"+jsonList(FAMILIES)+",\n\"new_runtime_methods\":"+jsonList(added)+",\n\"removed_runtime_methods\":"+jsonList(removed)+",\n\"new_runtime_aliases\":"+jsonList(Set.of())+"\n}\n");
  audit.add("PASS\toptimization_roots="+roots.size()+"\tchanged_methods="+changed.size()+"\ttiming_algorithm_preserved=true\tunchanged_native_payload=true");Files.write(Path.of(args[4]),audit);System.out.println(audit.get(audit.size()-1));
 }
}
