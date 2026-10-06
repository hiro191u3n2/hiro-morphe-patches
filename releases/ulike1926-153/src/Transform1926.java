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
/** Scoped 1925 -> 1926 transformation. All unrelated app and runtime code pinned. */
public final class Transform1926 {
 static final String H="Lcom/hiro/ulike/RearRestart1926;", Z="Lcom/hiro/ulike/OpticalZoom;";
 static final String INIT="Lcom/ss/android/vesdk/VECameraCapture;->init(Landroid/content/Context;Lcom/ss/android/vesdk/VECameraSettings;)I";
 static final String DESCRIPTION="v1.9.26（v1.9.25基準）背面カメラ再起動対策。初期化完了後に監視を開始、固定実レンズは表示面準備後にセッションを構成。復旧判定を現在の撮影ウィンドウへ分離。画質・前後選択維持。実機未検証。";
 static final Set<String> RUNTIME=Set.of(Z+"->prepared(Ljava/lang/Object;I)V", "Lcom/hiro/ulike/LensLifecycle172;->blocked()Z", "Lcom/hiro/ulike/LensLifecycle172;->failure(JLjava/lang/String;)V", "Lcom/hiro/ulike/CameraSession1920$Ticket;->pending(Ljava/lang/Object;)Z");
 static final List<String>AUDIT=new ArrayList<>();
 static void req(boolean x,String m){MergePayloads.require(x,m);}
 static String ref(Instruction x){return x instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static Method repl(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef cls(ClassDef c,List<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 static ImmutableMethodReference method(String name,List<String> args,String ret){return new ImmutableMethodReference(H,name,args,ret);}
 static Method init(Method m){
  var b=new MutableMethodImplementation(m.getImplementation());int removed=0,returns=0;
  for(int i=b.getInstructions().size()-1;i>=0;i--)if(ref(b.getInstructions().get(i)).equals(Z+"->track(Ljava/lang/Object;Landroid/content/Context;)V")){req(i==0,"Unexpected tracking hook position");b.removeInstruction(i);removed++;}
  req(removed==1,"Init tracking contract");int self=b.getRegisterCount()-3;
  for(int i=b.getInstructions().size()-1;i>=0;i--)if(b.getInstructions().get(i).getOpcode()==Opcode.RETURN){
   var ret=b.getInstructions().get(i);int r=((OneRegisterInstruction)ret).getRegisterA();
   b.replaceInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,self,1,method("initialized",List.of("Ljava/lang/Object;"),"V")));
   b.addInstruction(i+1,new BuilderInstruction11x(Opcode.RETURN,r));returns++;
  }req(returns==1,"Native init should have exactly one successful return");return repl(m,b);
 }
 static Method runtime(Method m){
  var b=new MutableMethodImplementation(m.getImplementation());String id=MergePayloads.id(m);
  if(id.equals(Z+"->prepared(Ljava/lang/Object;I)V")){
   b.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,b.getRegisterCount()-2,2,method("prepared",List.of("Ljava/lang/Object;","I"),"V")));
  }else{
   int n=0;for(int i=0;i<b.getInstructions().size();i++)if(ref(b.getInstructions().get(i)).equals("Lcom/hiro/ulike/ManualLens170;->ready()Z")){
    b.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,0,0,0,0,0,0,method("recoveryWindowReady",List.of(),"Z")));n++;
   }req(n==1,"Recovery readiness expected once "+id);
  }return repl(m,b);
 }
 static Map<String,ClassDef> metadata(Map<String,ClassDef> cs){
  Map<String,ClassDef> out=new TreeMap<>();int count=0;
  for(var c:cs.values()){
   var ms=new ArrayList<Method>();boolean changed=false;
   for(var m:c.getMethods()){
    if(m.getImplementation()==null){ms.add(m);continue;}
    var b=new MutableMethodImplementation(m.getImplementation());boolean hit=false;
    for(int i=0;i<b.getInstructions().size();i++){var x=b.getInstructions().get(i);
     if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().startsWith("v1.9.25（v1.9.24基準）")){
      req(x.getOpcode()==Opcode.CONST_STRING,"Metadata opcode"); b.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(DESCRIPTION)));count++;hit=true;
     }
    }ms.add(hit?repl(m,b):m);changed|=hit;
   }out.put(c.getType(),changed?cls(c,ms):c);
  }req(count==1,"One loader version expected");return out;
 }
 public static void main(String[] a)throws Exception{
  req(a.length==5,"BASE HELPER BUNDLE_DEX OUT AUDIT");Path base=Path.of(a[0]),out=Path.of(a[3]);Files.createDirectories(out);
  var payload=MergePayloads.methods(MergePayloads.classes(base.resolve("ulike/methods.dex").toString()).values());var previous=new TreeMap<>(payload);
  var rows=MergePayloads.contracts(base.resolve("ulike/methods.tsv").toString());
  req(payload.keySet().equals(rows.keySet()),"Contract target sets differ");for(var e:payload.entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Baseline method hash");
  Method before=payload.get(INIT);req(before!=null,"Init target absent");Method after=init(before);payload.put(INIT,after);rows.get(INIT)[2]=MergePayloads.hash(after);
  AUDIT.add("STOCK\t"+INIT+"\t"+MergePayloads.hash(before)+"\t"+MergePayloads.hash(after));
  for(var e:previous.entrySet())if(!e.getKey().equals(INIT))req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(payload.get(e.getKey()))),"Unrelated stock changed");
  var old=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var all=new TreeMap<String,ClassDef>();Set<String> edits=new TreeSet<>();
  for(var c:old.values()){
   var ms=new ArrayList<Method>();boolean changed=false;
   for(var m:c.getMethods()){
    String id=MergePayloads.id(m);if(RUNTIME.contains(id)){var n=runtime(m);ms.add(n);edits.add(id);changed=true;AUDIT.add("RUNTIME\t"+id+"\t"+MergePayloads.hash(m)+"\t"+MergePayloads.hash(n));}else ms.add(m);
   }all.put(c.getType(),changed?cls(c,ms):c);
  }req(edits.equals(RUNTIME),"Runtime patch coverage");
  var helper=MergePayloads.classes(a[1]);req(helper.keySet().equals(Set.of(H)),"Unexpected generated helper/stub leak");all.putAll(helper);
  var oldms=MergePayloads.methods(old.values());var newms=MergePayloads.methods(all.values());int retained=0;
  for(var e:oldms.entrySet())if(!RUNTIME.contains(e.getKey())){req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(newms.get(e.getKey()))),"Unrelated runtime changed "+e.getKey());retained++;}
  var single=metadata(MergePayloads.classes(base.resolve("classes.dex").toString()));var oldbundle=MergePayloads.classes(a[2]);var bundle=metadata(oldbundle);int kept=0;
  for(var e:oldbundle.entrySet())if(!e.getKey().startsWith(MergePayloads.PATCH_NS)){req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(bundle.get(e.getKey()))),"Other-app loader changed");kept++;}
  MergePayloads.writeDex(out.resolve("runtime.dex"),all.values());MergePayloads.writeDex(out.resolve("methods.dex"),MergePayloads.holders(payload));MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var lines=new ArrayList<String>();for(var row:rows.values())lines.add(String.join("\t",row));Files.write(out.resolve("methods.tsv"),lines);
  for(var e:MergePayloads.methods(MergePayloads.classes(out.resolve("methods.dex").toString()).values()).entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Emitted contract mismatch");
  AUDIT.add("PASS\tstock_changed=1\tstock_retained="+(previous.size()-1)+"\truntime_changed="+edits.size()+"\truntime_methods_retained="+retained+"\thelper_classes_added=1\tother_app_loaders_retained="+kept);Files.write(Path.of(a[4]),AUDIT);AUDIT.forEach(System.out::println);
 }
}
