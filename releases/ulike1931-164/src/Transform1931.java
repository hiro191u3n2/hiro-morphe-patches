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

/** Add a front-camera startup handshake without replacing the rear-camera helper. */
public final class Transform1931 {
 static final String H="Lcom/hiro/ulike/FrontPreview1931;";
 static final String C="Lcom/ss/android/vesdk/VECameraCapture;";
 static final String Z="Lcom/hiro/ulike/OpticalZoom;";
 static final String OLD_DESCRIPTION="v1.9.30（v1.9.29基準）";
 static final String DESCRIPTION="v1.9.31（v1.9.30基準）インカメラ再起動の開始要求取りこぼしと解放済み表示入力を修正。Camera1/2の所有者と世代を確認し準備後に再開。終了・切替で取消。画質・前後記憶・倍率非表示・他アプリ保持。実機未確認。";
 static final Map<String,String> CALLS=new TreeMap<>();
 static final Map<String,String> RUNTIME_CALLS=new TreeMap<>();
 static final Set<String> CANCEL=new TreeSet<>(Set.of(
  C+"->destroy()V",
  C+"->stopPreview(Z)I",
  C+"->switchCamera(Lcom/ss/android/vesdk/VECameraSettings$CAMERA_FACING_ID;Lcom/bytedance/bpea/basics/Cert;)I",
  C+"->switchCamera(Lcom/ss/android/vesdk/VECameraSettings;Lcom/bytedance/bpea/basics/Cert;)I",
  C+"->switchCameraMode(ILcom/ss/android/ttvecamera/TECameraSettings;)I"));
 static final Set<String> RUNTIME_CANCEL=new TreeSet<>(Set.of(Z+"->closing(Ljava/lang/Object;)V",Z+"->background(Ljava/lang/Object;)V"));
 static final Set<String> NATIVE=new TreeSet<>(), RUNTIME=new TreeSet<>();
 static {
  CALLS.put(C+"->onCaptureStarted(II)V","Lcom/hiro/ulike/PreviewStart1927;->start("+C+")I");
  CALLS.put(C+"->start(Lcom/ss/android/vesdk/ConcurrentList;)I","Lcom/hiro/ulike/PreviewStart1927;->start("+C+")I");
  CALLS.put(C+"->addCapturePipelines(Lcom/ss/android/vesdk/ConcurrentList;)V","Lcom/hiro/ulike/PreviewStart1927;->pipelines("+C+")V");
  CALLS.put(C+"->startPreview()I","Lcom/hiro/ulike/PreviewInputs1929;->before(Ljava/lang/Object;)Z");
  CALLS.put("Li/s/a/w/l0/b;->m(Li/s/a/w/m;)V","Lcom/hiro/ulike/PreviewInputs1929;->pixel(Ljava/lang/Object;Ljava/lang/Object;)V");
  RUNTIME_CALLS.put(Z+"->prepared(Ljava/lang/Object;I)V","Lcom/hiro/ulike/RearRestart1926;->prepared(Ljava/lang/Object;I)V");
  NATIVE.addAll(CALLS.keySet());NATIVE.addAll(CANCEL);
  RUNTIME.addAll(RUNTIME_CALLS.keySet());RUNTIME.addAll(RUNTIME_CANCEL);
 }
 static void req(boolean b,String m){MergePayloads.require(b,m);}
 static String id(Method m){return MergePayloads.id(m);}
 static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static Method repl(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
 static ClassDef cls(ClassDef c,List<Method> ms){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 static int words(Method m){int n=(m.getAccessFlags()&AccessFlags.STATIC.getValue())!=0?0:1;for(CharSequence p:m.getParameterTypes())n+=p.toString().equals("J")||p.toString().equals("D")?2:1;return n;}
 static Method repair(Method m,boolean runtime){
  var b=new MutableMethodImplementation(m.getImplementation());String key=id(m);
  String old=(runtime?RUNTIME_CALLS:CALLS).get(key);int hits=0;
  if(old!=null){
   for(int j=0;j<b.getInstructions().size();j++){
    var x=b.getInstructions().get(j);if(!ref(x).equals(old))continue;
    req(x instanceof ReferenceInstruction,"Expected hook reference");
    MethodReference mr=(MethodReference)((ReferenceInstruction)x).getReference();
    var next=new ImmutableMethodReference(H,mr.getName(),mr.getParameterTypes(),mr.getReturnType());
    if(x instanceof RegisterRangeInstruction r){req(x.getOpcode()==Opcode.INVOKE_STATIC_RANGE,"Hook invocation kind");b.replaceInstruction(j,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,r.getStartRegister(),r.getRegisterCount(),next));}
    else if(x instanceof FiveRegisterInstruction r){req(x.getOpcode()==Opcode.INVOKE_STATIC,"Hook invocation kind");b.replaceInstruction(j,new BuilderInstruction35c(Opcode.INVOKE_STATIC,r.getRegisterCount(),r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG(),next));}
    else throw new IllegalStateException("Unexpected hook encoding "+key);hits++;
   }
   req(hits==1,"Exactly one startup hook "+key+" got "+hits);
  }else{
   req((runtime?RUNTIME_CANCEL:CANCEL).contains(key),"Unknown cancellation target "+key);
   int receiver=b.getRegisterCount()-words(m);
   b.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,receiver,1,new ImmutableMethodReference(H,"cancel",List.of("Ljava/lang/Object;"),"V")));
  }
  return repl(m,b);
 }
 static Map<String,ClassDef> metadata(Map<String,ClassDef> old){
  var out=new TreeMap<String,ClassDef>();int count=0;
  for(var c:old.values()){
   var ms=new ArrayList<Method>();boolean changed=false;
   for(var m:c.getMethods()){
    if(m.getImplementation()==null){ms.add(m);continue;}
    var b=new MutableMethodImplementation(m.getImplementation());boolean hit=false;
    for(int j=0;j<b.getInstructions().size();j++){
     var x=b.getInstructions().get(j);
     if(x instanceof ReferenceInstruction ri&&ri.getReference() instanceof StringReference sr&&sr.getString().startsWith(OLD_DESCRIPTION)){
      req(c.getType().equals("Lapp/hiro/ulike/patches/UlikeHqMaxPatch;"),"Only ULike metadata can change");
      req(x.getOpcode()==Opcode.CONST_STRING,"Metadata encoding");
      b.replaceInstruction(j,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(DESCRIPTION)));count++;hit=true;
     }
    }
    ms.add(hit?repl(m,b):m);changed|=hit;
   }
   out.put(c.getType(),changed?cls(c,ms):c);
  }
  req(count==1,"Expected one ULike loader metadata string");return out;
 }
 public static void main(String[]a)throws Exception{
  req(a.length==6,"BASE HELPER BUNDLE_DEX SEED OUT AUDIT");Path base=Path.of(a[0]),out=Path.of(a[4]);Files.createDirectories(out);
  var old=MergePayloads.methods(MergePayloads.classes(base.resolve("ulike/methods.dex").toString()).values());var payload=new TreeMap<>(old);
  var rows=MergePayloads.contracts(base.resolve("ulike/methods.tsv").toString());req(old.keySet().equals(rows.keySet()),"Existing native inventory");
  for(var e:old.entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Baseline contract "+e.getKey());
  var seeds=MergePayloads.methods(MergePayloads.classes(a[3]).values());req(seeds.keySet().equals(CANCEL),"Pinned original cancellation seed set");
  var audit=new ArrayList<String>();
  for(String key:NATIVE){
   Method before=old.get(key);boolean added=before==null;
   if(added){before=seeds.get(key);req(before!=null,"Native target missing "+key);}
   else req(!seeds.containsKey(key),"Unexpected existing cancellation hook");
   Method after=repair(before,false);payload.put(key,after);
   if(added)rows.put(key,new String[]{key,MergePayloads.hash(before),MergePayloads.hash(after)});else rows.get(key)[2]=MergePayloads.hash(after);
   audit.add((added?"NATIVE_ADDED":"NATIVE_CHANGED")+"\t"+key+"\t"+MergePayloads.hash(before)+"\t"+MergePayloads.hash(after));
  }
  int keptNative=0;
  for(var e:old.entrySet())if(!NATIVE.contains(e.getKey())){req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(payload.get(e.getKey()))),"Unrelated native method changed "+e.getKey());keptNative++;}
  var oldRuntime=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var all=new TreeMap<String,ClassDef>();var touched=new TreeSet<String>();
  for(var c:oldRuntime.values()){
   var ms=new ArrayList<Method>();boolean changed=false;
   for(var m:c.getMethods()){
    if(RUNTIME.contains(id(m))){var next=repair(m,true);ms.add(next);touched.add(id(m));changed=true;audit.add("RUNTIME\t"+id(m)+"\t"+MergePayloads.hash(m)+"\t"+MergePayloads.hash(next));}
    else ms.add(m);
   }
   all.put(c.getType(),changed?cls(c,ms):c);
  }
  req(touched.equals(RUNTIME),"Runtime hook coverage");
  var helper=MergePayloads.classes(a[1]);req(helper.containsKey(H),"Missing front preview helper");
  for(var e:helper.entrySet()){req(e.getKey().matches("Lcom/hiro/ulike/FrontPreview1931(\\$[^;]+)?;"),"Stub leaked "+e.getKey());req(!all.containsKey(e.getKey()),"Helper collision");all.put(e.getKey(),e.getValue());}
  var afterMethods=MergePayloads.methods(all.values());int keptRuntime=0;
  for(var e:MergePayloads.methods(oldRuntime.values()).entrySet())if(!RUNTIME.contains(e.getKey())){req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(afterMethods.get(e.getKey()))),"Unrelated runtime changed "+e.getKey());keptRuntime++;}
  var single=metadata(MergePayloads.classes(base.resolve("classes.dex").toString()));var oldBundle=MergePayloads.classes(a[2]);var bundle=metadata(oldBundle);int keptLoaders=0;
  for(var e:oldBundle.entrySet())if(!e.getKey().startsWith(MergePayloads.PATCH_NS)){req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(bundle.get(e.getKey()))),"Other app loader changed");keptLoaders++;}
  MergePayloads.writeDex(out.resolve("runtime.dex"),all.values());MergePayloads.writeDex(out.resolve("methods.dex"),MergePayloads.holders(payload));MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var lines=new ArrayList<String>();for(var row:rows.values())lines.add(String.join("\t",row));Files.write(out.resolve("methods.tsv"),lines);
  for(var e:MergePayloads.methods(MergePayloads.classes(out.resolve("methods.dex").toString()).values()).entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Final native payload contract");
  audit.add("PASS\texisting_native_methods_retained="+keptNative+"\texisting_native_methods_changed="+CALLS.size()+"\tnew_native_hooks="+CANCEL.size()+"\texisting_runtime_methods_retained="+keptRuntime+"\texisting_runtime_methods_changed="+RUNTIME.size()+"\thelper_classes_added="+helper.size()+"\tother_app_loaders_retained="+keptLoaders);
  Files.write(Path.of(a[5]),audit);audit.forEach(System.out::println);
 }
}
