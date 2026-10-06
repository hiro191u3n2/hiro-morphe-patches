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
/** Only reconnect the lost native preview-start request; do not change camera controls. */
public final class Transform1927 {
 static final String H="Lcom/hiro/ulike/PreviewStart1927;",C="Lcom/ss/android/vesdk/VECameraCapture;",L="Lcom/ss/android/vesdk/ConcurrentList;";
 static final String OPEN=C+"->onCaptureStarted(II)V", START=C+"->start("+L+")I", PIPE=C+"->addCapturePipelines("+L+")V";
 static final Set<String> TARGETS=Set.of(OPEN,START,PIPE);
 static final String DESCRIPTION="v1.9.27（v1.9.26基準）背面再起動のプレビュー開始要求の取りこぼしを修正。表示用入力の準備後に同一カメラ・世代だけ再開。保存・録画・既存セッション保護、画質維持。実機未検証。";
 static void req(boolean b,String m){MergePayloads.require(b,m);}
 static String ref(Instruction i){return Transform1926.ref(i);}
 static Method repl(Method m,MethodImplementation impl){return Transform1926.repl(m,impl);}
 static Method apply(Method m){
  var b=new MutableMethodImplementation(m.getImplementation());int hits=0;
  if(MergePayloads.id(m).equals(PIPE)){
   int self=b.getRegisterCount()-2;
   for(int i=b.getInstructions().size()-1;i>=0;i--)if(b.getInstructions().get(i).getOpcode()==Opcode.RETURN_VOID){
    b.replaceInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,self,1,new ImmutableMethodReference(H,"pipelines",List.of(C),"V")));
    b.addInstruction(i+1,new BuilderInstruction10x(Opcode.RETURN_VOID));hits++;
   }
  }else for(int i=0;i<b.getInstructions().size();i++){
   var x=b.getInstructions().get(i);if(!ref(x).equals(C+"->startPreview()I"))continue;
   var target=new ImmutableMethodReference(H,"start",List.of(C),"I");
   if(x instanceof FiveRegisterInstruction r){req(r.getRegisterCount()==1,"Native self argument");b.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,r.getRegisterC(),0,0,0,0,target));}
   else if(x instanceof RegisterRangeInstruction r){req(r.getRegisterCount()==1,"Native range self");b.replaceInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,r.getStartRegister(),1,target));}
   else throw new IllegalStateException("Unknown invoke encoding");hits++;
  }
  req(hits==1,"One native start/ready point required "+MergePayloads.id(m));return repl(m,b);
 }
 static Map<String,ClassDef> metadata(Map<String,ClassDef> cs){
  var out=new TreeMap<String,ClassDef>();int edits=0;
  for(var c:cs.values()){
   var ms=new ArrayList<Method>();boolean changed=false;
   for(var m:c.getMethods()){
    if(m.getImplementation()==null){ms.add(m);continue;}
    var b=new MutableMethodImplementation(m.getImplementation());boolean hit=false;
    for(int i=0;i<b.getInstructions().size();i++){var x=b.getInstructions().get(i);
     if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().startsWith("v1.9.26（v1.9.25基準）")){
      req(x.getOpcode()==Opcode.CONST_STRING,"Metadata encoding");b.replaceInstruction(i,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(DESCRIPTION)));edits++;hit=true;
     }
    }ms.add(hit?repl(m,b):m);changed|=hit;
   }out.put(c.getType(),changed?Transform1926.cls(c,ms):c);
  }req(edits==1,"Exactly one loader version");return out;
 }
 public static void main(String[]a)throws Exception{
  req(a.length==6,"BASE HELPER BUNDLE_DEX SEED OUT AUDIT");Path base=Path.of(a[0]),out=Path.of(a[4]);Files.createDirectories(out);
  var old=MergePayloads.methods(MergePayloads.classes(base.resolve("ulike/methods.dex").toString()).values());var payload=new TreeMap<>(old);
  var rows=MergePayloads.contracts(base.resolve("ulike/methods.tsv").toString());req(old.keySet().equals(rows.keySet()),"Target sets");
  for(var e:old.entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Baseline hash "+e.getKey());
  var seeds=MergePayloads.methods(MergePayloads.classes(a[3]).values());req(seeds.keySet().equals(TARGETS),"Seed target set");var audit=new ArrayList<String>();
  for(var e:seeds.entrySet()){
   req(!old.containsKey(e.getKey()),"New native target unexpectedly already patched "+e.getKey());var changed=apply(e.getValue());payload.put(e.getKey(),changed);
   rows.put(e.getKey(),new String[]{e.getKey(),MergePayloads.hash(e.getValue()),MergePayloads.hash(changed)});
   audit.add("STOCK\t"+String.join("\t",rows.get(e.getKey())));
  }
  for(var e:old.entrySet())req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(payload.get(e.getKey()))),"Existing native patch changed");
  var runtime=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var all=new TreeMap<>(runtime);var helper=MergePayloads.classes(a[1]);
  req(helper.keySet().equals(Set.of(H,H.substring(0,H.length()-1)+"$Pending;")),"Stub/helper namespace leaked "+helper.keySet());
  for(var e:helper.entrySet()){req(!all.containsKey(e.getKey()),"Helper collision");all.put(e.getKey(),e.getValue());}
  for(var e:runtime.entrySet())req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(all.get(e.getKey()))),"Existing runtime changed");
  var single=metadata(MergePayloads.classes(base.resolve("classes.dex").toString()));var beforeBundle=MergePayloads.classes(a[2]);var bundle=metadata(beforeBundle);int kept=0;
  for(var e:beforeBundle.entrySet())if(!e.getKey().startsWith(MergePayloads.PATCH_NS)){req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(bundle.get(e.getKey()))),"Other-app loader changed");kept++;}
  MergePayloads.writeDex(out.resolve("runtime.dex"),all.values());MergePayloads.writeDex(out.resolve("methods.dex"),MergePayloads.holders(payload));
  MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var lines=new ArrayList<String>();for(var row:rows.values())lines.add(String.join("\t",row));Files.write(out.resolve("methods.tsv"),lines);
  for(var e:MergePayloads.methods(MergePayloads.classes(out.resolve("methods.dex").toString()).values()).entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Emitted target mismatch");
  audit.add("PASS\tnew_native_hooks=3\texisting_native_retained="+old.size()+"\texisting_runtime_classes_retained="+runtime.size()+"\texisting_runtime_methods_retained="+MergePayloads.methods(runtime.values()).size()+"\thelper_classes_added=2\tother_app_loaders_retained="+kept);
  Files.write(Path.of(a[5]),audit);audit.forEach(System.out::println);
 }
}
