import java.nio.file.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Exact-contract repair over1922, with unrelated-app bytecode conservation. */
public final class Transform1923 {
 static final String S="Lcom/bytedance/corecamera/ui/view/CameraShadeView;", F="Lcom/bytedance/corecamera/ui/view/CameraShadeView$f;", D="Lcom/bytedance/corecamera/ui/view/CameraShadeView$d;";
 static final String H="Lcom/hiro/ulike/PreviewLayout1922;", N="Lcom/hiro/ulike/ShadowDetail1923;", R="Lcom/ss/android/vesdk/VEPreviewRadio;";
 static final String W="Lcom/hiro/ulike/ChromaPipeline186$Worker;", STATE="Lcom/hiro/ulike/ChromaPipeline186$State;", BUFFER="Lcom/hiro/ulike/ChromaPipeline186$Buffer;", WORK="Lcom/hiro/ulike/DetailPixels$Work;";
 static final String DESCRIPTION="v1.9.23（v1.9.22基準）プレビューのビュー座標と描画領域を統一し、初回フレーム待ち復旧の分岐漏れを修正。保存写真の暗部を輪郭保護付きで平滑化し、粒状ノイズの再強調を抑制。実機未検証。";
 static final Set<String> TARGETS=Set.of(S+"->m("+R+"ZZZ)Z",S+"->a("+F+")V",S+"->onLayout(ZIIII)V",S+"->onDetachedFromWindow()V",D+"->g()Landroid/graphics/RectF;");
 static final List<String>AUDIT=new ArrayList<>();
 static void require(boolean b,String m){MergePayloads.require(b,m);}
 static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static Method replace(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static BuilderInstruction3rc call(int r,String name){return new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,r,1,new ImmutableMethodReference(H,name,List.of(S),"V"));}
 static Method stock(Method m){
  String id=MergePayloads.id(m);var b=new MutableMethodImplementation(m.getImplementation());
  if(id.equals(S+"->m("+R+"ZZZ)Z")){
   require(b.getRegisterCount()==27,"Ratio ABI");int calls=0,returns=0,prepare=0;
   for(int i=b.getInstructions().size()-1;i>=0;i--){var x=b.getInstructions().get(i);String r=ref(x);
    if(r.equals(H+"->ready("+S+")V")){b.removeInstruction(i);calls++;}
    else if(r.equals(H+"->cancel("+S+")V")){b.replaceInstruction(i,call(22,"prepare"));prepare++;}
   }
   require(calls==2&&prepare==1,"Expected1922 lifecycle hooks");
   for(int i=b.getInstructions().size()-1;i>=0;i--)if(b.getInstructions().get(i).getOpcode()==Opcode.RETURN){
    int reg=((OneRegisterInstruction)b.getInstructions().get(i)).getRegisterA();
    // REPLACE, not insert before: all existing branch labels must enter ready().
    b.replaceInstruction(i,call(22,"ready"));b.addInstruction(i+1,new BuilderInstruction11x(Opcode.RETURN,reg));returns++;
   }
   require(returns==2,"Expected two ratio returns");metricCalls(b,22,true);
  }else if(id.equals(S+"->a("+F+")V")){metricCalls(b,b.getRegisterCount()-2,true);
  }else if(id.equals(S+"->onLayout(ZIIII)V")){
   int count=0;for(int i=b.getInstructions().size()-1;i>=0;i--)if(b.getInstructions().get(i).getOpcode()==Opcode.RETURN_VOID){b.replaceInstruction(i,call(7,"afterLayout"));b.addInstruction(i+1,new BuilderInstruction10x(Opcode.RETURN_VOID));count++;}require(count==1,"Layout return");
  }else if(id.equals(S+"->onDetachedFromWindow()V")){
   require(ref(b.getInstructions().get(0)).equals(H+"->cancel("+S+")V"),"Detach1922 missing");b.replaceInstruction(0,call(b.getRegisterCount()-1,"detached"));
  }else if(id.equals(D+"->g()Landroid/graphics/RectF;")){
   require(b.getRegisterCount()==6,"Viewport ABI");Label original=b.newLabelForIndex(0);
   b.addInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,0,0,0,0,0,0,new ImmutableMethodReference(H,"viewport",List.of(),"Landroid/graphics/RectF;")));
   b.addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,0));
   b.addInstruction(2,new BuilderInstruction21t(Opcode.IF_EQZ,0,original));
   b.addInstruction(3,new BuilderInstruction11x(Opcode.RETURN_OBJECT,0));
  }else throw new IllegalStateException(id);
  return replace(m,b);
 }
 static void metricCalls(MutableMethodImplementation b,int self,boolean height){
  int count=0;for(int i=0;i<b.getInstructions().size();i++){
   String r=ref(b.getInstructions().get(i));String name=null;
   if(r.equals("Li/f/l/w/z;->h()I"))name="localWidth";
   else if(height&&(r.equals("Li/f/l/w/z;->e()I")||r.equals("Li/f/l/w/z;->f()I")))name="localHeight";
   if(name!=null){b.replaceInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,self,1,new ImmutableMethodReference(H,name,List.of(S),"I")));count++;}
  }require(count>0,"Metrics calls absent");AUDIT.add("LOCAL_METRICS\t"+count);
 }
 static Map<String,ClassDef> metadata(Map<String,ClassDef> before){
  var out=new TreeMap<String,ClassDef>();int count=0;
  for(var c:before.values()){
   var methods=new ArrayList<Method>();boolean changed=false;
   for(var m:c.getMethods()){
    if(m.getImplementation()==null){methods.add(m);continue;}var b=new MutableMethodImplementation(m.getImplementation());boolean hit=false;
    for(int i=0;i<b.getInstructions().size();i++){var x=b.getInstructions().get(i);
     if(x instanceof ReferenceInstruction ri&&ri.getReference() instanceof StringReference sr&&sr.getString().startsWith("v1.9.22（v1.9.21基準）")){
      require(x.getOpcode()==Opcode.CONST_STRING,"Metadata opcode");b.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(DESCRIPTION)));count++;hit=true;
     }
    }methods.add(hit?replace(m,b):m);changed|=hit;
   }
   out.put(c.getType(),changed?new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods):c);
  }require(count==1,"One loader description expected");return out;
 }
 static Map<String,ClassDef> noise(Map<String,ClassDef> original){
  var out=new TreeMap<>(original);int edits=0;
  for(String type:List.of(W,"Lcom/hiro/ulike/DetailFast174;")){
   var c=original.get(type);var methods=new ArrayList<Method>();
   for(var m:c.getMethods()){
    if(type.equals(W)&&m.getName().equals("run")&&m.getParameterTypes().isEmpty()){
     var ins=List.of(new ImmutableInstruction22c(Opcode.IGET_OBJECT,0,2,new ImmutableFieldReference(W,"s",STATE)),new ImmutableInstruction22c(Opcode.IGET_OBJECT,1,2,new ImmutableFieldReference(W,"b",BUFFER)),new ImmutableInstruction35c(Opcode.INVOKE_STATIC,2,0,1,0,0,0,new ImmutableMethodReference(N,"run",List.of(STATE,BUFFER),"V")),new ImmutableInstruction10x(Opcode.RETURN_VOID));
     methods.add(replace(m,new ImmutableMethodImplementation(3,ins,List.of(),List.of())));edits++;
    }else if(type.equals("Lcom/hiro/ulike/DetailFast174;")&&m.getName().equals("filter")&&m.getParameterTypes().size()==10){
     var b=new MutableMethodImplementation(m.getImplementation());var params=m.getParameterTypes().stream().map(Object::toString).toList();int count=0;
     for(int i=b.getInstructions().size()-1;i>=0;i--)if(b.getInstructions().get(i).getOpcode()==Opcode.RETURN_VOID){
      b.replaceInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,b.getRegisterCount()-10,10,new ImmutableMethodReference(N,"legacy",params,"V")));
      b.addInstruction(i+1,new BuilderInstruction10x(Opcode.RETURN_VOID));count++;
     }require(count==4,"Legacy filter return contract");methods.add(replace(m,b));edits++;
    }else methods.add(m);
   }
   out.put(type,new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
  }
  require(edits==2,"Noise bridge count");return out;
 }
 public static void main(String[] a)throws Exception{
  require(a.length==6,"BASE HELPER BUNDLE_DEX SEED OUT AUDIT");Path base=Path.of(a[0]),out=Path.of(a[4]);Files.createDirectories(out);
  var seeds=MergePayloads.methods(MergePayloads.classes(a[3]).values());require(seeds.keySet().equals(TARGETS),"Stock seed targets");
  var payload=MergePayloads.methods(MergePayloads.classes(base.resolve("ulike/methods.dex").toString()).values());var prior=new TreeMap<>(payload);var rows=MergePayloads.contracts(base.resolve("ulike/methods.tsv").toString());
  for(var e:payload.entrySet())require(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Input hash");
  for(String id:TARGETS){var original=seeds.get(id);var before=payload.getOrDefault(id,original);if(rows.containsKey(id))require(MergePayloads.hash(original).equals(rows.get(id)[1]),"Original contract changed "+id);
   var after=stock(before);payload.put(id,after);rows.put(id,new String[]{id,MergePayloads.hash(original),MergePayloads.hash(after)});AUDIT.add("STOCK\t"+id+"\t"+MergePayloads.hash(before)+"\t"+MergePayloads.hash(after));}
  int unchanged=0;for(var e:prior.entrySet())if(!TARGETS.contains(e.getKey())){require(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(payload.get(e.getKey()))),"Unrelated stock changed");unchanged++;}
  var rt=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var all=noise(rt);var helper=MergePayloads.classes(a[1]);
  require(helper.containsKey(N)&&helper.containsKey(H),"Both production helpers required");
  for(var e:helper.entrySet()){require(e.getKey().startsWith("Lcom/hiro/ulike/PreviewLayout1922")||e.getKey().equals(N),"Stub leaked "+e.getKey());all.put(e.getKey(),e.getValue());}
  var allowed=Set.of(W,"Lcom/hiro/ulike/DetailFast174;");int kept=0,edited=0;
  for(var e:rt.entrySet()){
   if(e.getKey().startsWith("Lcom/hiro/ulike/PreviewLayout1922")||allowed.contains(e.getKey())){edited++;continue;}
   require(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(all.get(e.getKey()))),"Unrelated runtime changed");kept++;
  }
  var single=metadata(MergePayloads.classes(base.resolve("classes.dex").toString()));var oldbundle=MergePayloads.classes(a[2]);var bundle=metadata(oldbundle);int others=0;
  for(var e:oldbundle.entrySet())if(!e.getKey().startsWith(MergePayloads.PATCH_NS)){require(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(bundle.get(e.getKey()))),"Other-app loader changed");others++;}
  MergePayloads.writeDex(out.resolve("runtime.dex"),all.values());MergePayloads.writeDex(out.resolve("methods.dex"),MergePayloads.holders(payload));MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var text=new ArrayList<String>();for(var row:rows.values())text.add(String.join("\t",row));Files.write(out.resolve("methods.tsv"),text);
  var emitted=MergePayloads.methods(MergePayloads.classes(out.resolve("methods.dex").toString()).values());for(var e:emitted.entrySet())require(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Output contract mismatch");
  AUDIT.add("PASS\tstock_changed="+TARGETS.size()+"\tstock_unchanged="+unchanged+"\truntime_unchanged="+kept+"\truntime_scoped="+edited+"\tother_app_loaders_unchanged="+others);
  Files.write(Path.of(a[5]),AUDIT);AUDIT.forEach(System.out::println);
 }
}
