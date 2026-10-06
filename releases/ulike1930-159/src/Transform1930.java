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

/** Only rear lens UI visibility and stale clicks change. Camera/image code stays intact. */
public final class Transform1930 {
 static final String HELPER="Lcom/hiro/ulike/RearLensUi1930;";
 static final String ZOOM="Lcom/hiro/ulike/OpticalZoom;", BAR="Lcom/hiro/ulike/OpticalZoomUi$Bar;";
 static final String SHOW=ZOOM+"->show()Z",SELECT=ZOOM+"->select(I)V",DRAW=BAR+"->onPreDraw()Z";
 static final Set<String> TARGETS=Set.of(SHOW,SELECT,DRAW);
 static final String OLD_DESCRIPTION="v1.9.29（v1.9.28基準）";
 static final String DESCRIPTION="v1.9.30（v1.9.29基準）インカメラ選択中は倍率・接写ボタンとラベルを非表示。黒帯ダブルタップ後の再表示と残留クリックを抑止。背面復帰時は通常表示。画質・起動修正・他アプリ保持。実機未確認。";
 static void req(boolean ok,String message){MergePayloads.require(ok,message);}
 static String id(Method m){return MergePayloads.id(m);}
 static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static Method repl(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef cls(ClassDef c,List<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 static ImmutableMethodReference call(String owner,String name,List<String> args,String result){return new ImmutableMethodReference(owner,name,args,result);}
 static BuilderInstruction35c noArgs(String owner,String name){return new BuilderInstruction35c(Opcode.INVOKE_STATIC,0,0,0,0,0,0,call(owner,name,List.of(),"Z"));}
 static int prefixLength(String method){return method.equals(DRAW)?6:method.equals(SHOW)?5:4;}
 static Method guard(Method m){
  String key=id(m);var b=new MutableMethodImplementation(m.getImplementation());int n=b.getRegisterCount();
  req(b.getTryBlocks().isEmpty(),"Unexpected guarded method try blocks "+key);
  req((key.equals(SHOW)&&n==1)||(key.equals(SELECT)&&n==2)||(key.equals(DRAW)&&n==7),"Pinned register shape "+key);
  if(key.equals(SELECT))req(b.getInstructions().size()==2 && ref(b.getInstructions().get(0)).equals("Lcom/hiro/ulike/ManualLens170;->select(I)V") && b.getInstructions().get(1).getOpcode()==Opcode.RETURN_VOID,"Existing selection delegation changed");
  if(key.equals(DRAW))req(ref(b.getInstructions().get(0)).equals("Landroid/os/SystemClock;->uptimeMillis()J"),"PreDraw must guard before throttle clock");
  Label original=b.newLabelForIndex(0);
  b.addInstruction(0,noArgs(key.equals(DRAW)?ZOOM:HELPER,key.equals(DRAW)?"show":"rearSelected"));
  b.addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
  b.addInstruction(2,new BuilderInstruction21t(Opcode.IF_NEZ,0,original));
  if(key.equals(DRAW)){
   b.addInstruction(3,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,1,n-1,0,0,0,0,call(BAR,"hide",List.of(),"V")));
   b.addInstruction(4,new BuilderInstruction11n(Opcode.CONST_4,0,1));
   b.addInstruction(5,new BuilderInstruction11x(Opcode.RETURN,0));
  }else if(key.equals(SHOW)){
   b.addInstruction(3,new BuilderInstruction11n(Opcode.CONST_4,0,0));
   b.addInstruction(4,new BuilderInstruction11x(Opcode.RETURN,0));
  }else b.addInstruction(3,new BuilderInstruction10x(Opcode.RETURN_VOID));
  return repl(m,b);
 }
 static Map<String,ClassDef> metadata(Map<String,ClassDef> old){
  var result=new TreeMap<String,ClassDef>();int count=0;
  for(var c:old.values()){
   var ms=new ArrayList<Method>();boolean touched=false;
   for(var m:c.getMethods()){
    if(m.getImplementation()==null){ms.add(m);continue;}
    var b=new MutableMethodImplementation(m.getImplementation());boolean changed=false;
    for(int j=0;j<b.getInstructions().size();j++){
     var i=b.getInstructions().get(j);
     if(i instanceof ReferenceInstruction ri && ri.getReference() instanceof StringReference sr && sr.getString().startsWith(OLD_DESCRIPTION)){
      req(c.getType().equals("Lapp/hiro/ulike/patches/UlikeHqMaxPatch;"),"Metadata belongs to ULike only");
      req(i.getOpcode()==Opcode.CONST_STRING,"Unexpected metadata string encoding");
      b.replaceInstruction(j,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)i).getRegisterA(),new ImmutableStringReference(DESCRIPTION)));count++;changed=true;
     }
    }
    ms.add(changed?repl(m,b):m);touched|=changed;
   }
   result.put(c.getType(),touched?cls(c,ms):c);
  }
  req(count==1,"Exactly one ULike loader description, got "+count);return result;
 }
 public static void main(String[] args)throws Exception{
  req(args.length==5,"BASE HELPER BUNDLE_DEX OUT AUDIT");Path base=Path.of(args[0]),out=Path.of(args[3]);Files.createDirectories(out);
  var old=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var result=new TreeMap<String,ClassDef>();var touched=new TreeSet<String>();var audit=new ArrayList<String>();
  for(var c:old.values()){
   var ms=new ArrayList<Method>();boolean changed=false;
   for(var m:c.getMethods()){
    if(TARGETS.contains(id(m))){var next=guard(m);ms.add(next);touched.add(id(m));changed=true;audit.add("RUNTIME\t"+id(m)+"\t"+MergePayloads.hash(m)+"\t"+MergePayloads.hash(next));}
    else ms.add(m);
   }
   result.put(c.getType(),changed?cls(c,ms):c);
  }
  req(touched.equals(TARGETS),"Exact guard target set");
  var helper=MergePayloads.classes(args[1]);req(helper.keySet().equals(Set.of(HELPER)),"Only production helper may be added");
  req(!result.containsKey(HELPER),"Helper collision");result.putAll(helper);
  var beforeMethods=MergePayloads.methods(old.values());var afterMethods=MergePayloads.methods(result.values());int kept=0;
  for(var e:beforeMethods.entrySet())if(!TARGETS.contains(e.getKey())){req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(afterMethods.get(e.getKey()))),"Unrelated runtime method changed "+e.getKey());kept++;}
  var single=metadata(MergePayloads.classes(base.resolve("classes.dex").toString()));var oldBundle=MergePayloads.classes(args[2]);var bundle=metadata(oldBundle);int keptLoaders=0;
  for(var e:oldBundle.entrySet())if(!e.getKey().startsWith(MergePayloads.PATCH_NS)){req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(bundle.get(e.getKey()))),"Non-ULike loader changed");keptLoaders++;}
  MergePayloads.writeDex(out.resolve("runtime.dex"),result.values());MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  Files.copy(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"));Files.copy(base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"));
  audit.add("PASS\texisting_runtime_methods_retained="+kept+"\texisting_runtime_methods_changed="+touched.size()+"\thelper_classes_added="+helper.size()+"\tother_app_loaders_retained="+keptLoaders+"\tnative_payload_byte_identical=true");
  Files.write(Path.of(args[4]),audit);audit.forEach(System.out::println);
 }
}
