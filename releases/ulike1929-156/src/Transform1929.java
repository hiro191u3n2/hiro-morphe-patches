import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Ownership, real preview-input readiness and ordered fallback; no image processing changes. */
public final class Transform1929 {
 static final String P="Lcom/hiro/ulike/ProviderLifecycle1929;",I="Lcom/hiro/ulike/PreviewInputs1929;",S="Lcom/hiro/ulike/SessionFallback1929;";
 static final String COPY_CTOR="Li/s/a/w/l0/c$a;-><init>(Li/s/a/w/l0/c$a;)V",COPY="Li/s/a/w/l0/c$a;->a(Li/s/a/w/l0/c$a;)V",SAME="Li/s/a/w/l0/c$a;->b(Li/s/a/w/l0/c$a;)Z";
 static final String RELEASE_CTOR="Li/s/a/w/q$k;-><init>(Li/s/a/w/q;)V",RELEASE="Li/s/a/w/q$k;->run()V",ADD="Li/s/a/w/q;->addCameraProvider(Li/s/a/w/k;Li/s/a/w/l0/c$a;)I";
 static final String START="Lcom/ss/android/vesdk/VECameraCapture;->startPreview()I",FIRST="Li/s/a/w/h0/b;->w0()I",PIXEL="Li/s/a/w/l0/b;->m(Li/s/a/w/m;)V";
 static final Set<String> TARGETS=Set.of(COPY_CTOR,COPY,SAME,RELEASE_CTOR,RELEASE,ADD,START,FIRST,PIXEL);
 static final String FOREGROUND="Lcom/hiro/ulike/OpticalZoom;->foreground(Ljava/lang/Object;)V",FAILED="Lcom/hiro/ulike/ManualLens170$State;->onConfigureFailed(Landroid/hardware/camera2/CameraCaptureSession;)V",READY="Lcom/hiro/ulike/PreviewStart1927;->sourcesReady(Lcom/ss/android/vesdk/VECameraCapture;)Z",WATCH="Lcom/hiro/ulike/LensLifecycle172$Watch;->run()V";
 static final Set<String> RUNTIME=Set.of(FOREGROUND,FAILED,READY,WATCH);
 static final String DESCRIPTION="v1.9.29（v1.9.28基準）背面再起動の出力寿命・再利用判定・初期化世代・構成復帰順を横断修正。実画像の到着を監視。画質と前後記憶・他アプリ維持。実機未検証。";
 static void req(boolean b,String m){MergePayloads.require(b,m);}
 static String ref(Instruction i){return Transform1926.ref(i);}
 static Method repl(Method m,MethodImplementation b){return Transform1926.repl(m,b);}
 static ImmutableMethodReference call(String owner,String n,List<String> p,String r){return new ImmutableMethodReference(owner,n,p,r);}
 static BuilderInstruction35c invoke(String owner,String n,List<String> p,String ret,int... regs){
  int[] r=new int[5];System.arraycopy(regs,0,r,0,regs.length);for(int v:regs)req(v<16,"invoke register limit");
  return new BuilderInstruction35c(Opcode.INVOKE_STATIC,regs.length,r[0],r[1],r[2],r[3],r[4],call(owner,n,p,ret));
 }
 static Method apply(Method m){
  String id=MergePayloads.id(m);var b=new MutableMethodImplementation(m.getImplementation());int hits=0,n=b.getRegisterCount();
  if(id.equals(COPY_CTOR)||id.equals(COPY)||id.equals(SAME)){
   b=new MutableMethodImplementation(Math.max(n,3));n=b.getRegisterCount();int self=n-2,arg=n-1;
   if(id.equals(COPY_CTOR))b.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_DIRECT,1,self,0,0,0,0,call("Ljava/lang/Object;","<init>",List.of(),"V")));
   b.addInstruction(invoke(P,id.equals(SAME)?"same":"copy",List.of("Ljava/lang/Object;","Ljava/lang/Object;"),id.equals(SAME)?"Z":"V",self,arg));
   if(id.equals(SAME)){b.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT,0));b.addInstruction(new BuilderInstruction11x(Opcode.RETURN,0));}
   else b.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));hits=1;
  }else if(id.equals(RELEASE_CTOR)){
   req(n==2&&b.getInstructions().size()==3&&b.getInstructions().get(2).getOpcode()==Opcode.RETURN_VOID,"Release constructor shape");
   b.addInstruction(2,invoke(P,"captureRelease",List.of("Ljava/lang/Object;","Ljava/lang/Object;"),"V",0,1));hits=1;
  }else if(id.equals(RELEASE)){
   b=new MutableMethodImplementation(2);
   b.addInstruction(new BuilderInstruction22c(Opcode.IGET_OBJECT,0,1,new ImmutableFieldReference("Li/s/a/w/q$k;","c","Li/s/a/w/q;")));
   b.addInstruction(invoke(P,"release",List.of("Ljava/lang/Object;","Ljava/lang/Object;"),"V",1,0));b.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));hits=1;
  }else if(id.equals(ADD)){
   for(int j=0;j<b.getInstructions().size();j++)if(ref(b.getInstructions().get(j)).equals("Li/s/a/w/a;->U()Li/s/a/w/l0/c;")){
    var old=(FiveRegisterInstruction)b.getInstructions().get(j);req(old.getRegisterCount()==1,"Reuse receiver");
    b.replaceInstruction(j,invoke(P,"liveManager",List.of("Ljava/lang/Object;"),"Li/s/a/w/l0/c;",old.getRegisterC()));hits++;
   }
  }else if(id.equals(START)){
   req(n>1,"Preview locals");
   // The old entry label stays attached to the first original instruction. An
   // explicit guarded prefix is the method entry and no existing branch skips it.
   Label original=b.newLabelForIndex(0);
   b.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,n-1,1,call(I,"before",List.of("Ljava/lang/Object;"),"Z")));
   b.addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
   b.addInstruction(2,new BuilderInstruction21t(Opcode.IF_NEZ,0,original));
   b.addInstruction(3,new BuilderInstruction21s(Opcode.CONST_16,0,-100));b.addInstruction(4,new BuilderInstruction11x(Opcode.RETURN,0));hits=1;
  }else if(id.equals(FIRST)){
   for(int j=0;j<b.getInstructions().size();j++)if(ref(b.getInstructions().get(j)).equals("Li/s/a/w/h0/b$h;->a:Z")){
    int r=((TwoRegisterInstruction)b.getInstructions().get(j)).getRegisterA();
    req(b.getInstructions().get(j+1).getOpcode()==Opcode.IF_NEZ&&((OneRegisterInstruction)b.getInstructions().get(j+1)).getRegisterA()==r,"First request success branch");
    // The false fallthrough returns an error; successful requests keep the exact
    // original branch target, rotation, preview-state and listener notification.
    b.addInstruction(j+2,new BuilderInstruction21s(Opcode.CONST_16,r,-108));
    b.addInstruction(j+3,new BuilderInstruction11x(Opcode.RETURN,r));hits++;j+=3;
   }
  }else if(id.equals(PIXEL)){
   for(int j=0;j<b.getInstructions().size();j++)if(ref(b.getInstructions().get(j)).equals("Li/s/a/w/l0/b$c;->onFrameCaptured(Li/s/a/w/m;)V")){
    b.addInstruction(j+1,invoke(I,"pixel",List.of("Ljava/lang/Object;","Ljava/lang/Object;"),"V",n-2,n-1));hits++;j++;
   }
  }else throw new IllegalStateException("Unexpected native target "+id);
  req(hits==1,"Exactly one native bridge "+id+" got="+hits);return repl(m,b);
 }
 static Method runtime(Method m){
  String id=MergePayloads.id(m);var b=new MutableMethodImplementation(m.getImplementation());int hits=0,n=b.getRegisterCount();
  if(id.equals(FOREGROUND)){
   for(int j=b.getInstructions().size()-1;j>=0;j--)if(ref(b.getInstructions().get(j)).equals("Lcom/hiro/ulike/StartupReset184;->beforeForeground(Ljava/lang/Object;)V")){b.replaceInstruction(j,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,n-1,1,call(I,"foreground",List.of("Ljava/lang/Object;"),"V")));hits++;}
  }else if(id.equals(FAILED)){
   b=new MutableMethodImplementation(2);b.addInstruction(invoke(S,"failed",List.of("Ljava/lang/Object;","Landroid/hardware/camera2/CameraCaptureSession;"),"V",0,1));b.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));hits=1;
  }else if(id.equals(READY)){
   b=new MutableMethodImplementation(2);b.addInstruction(invoke(I,"sourcesReady",List.of("Lcom/ss/android/vesdk/VECameraCapture;"),"Z",1));b.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT,0));b.addInstruction(new BuilderInstruction11x(Opcode.RETURN,0));hits=1;
  }else if(id.equals(WATCH)){
   for(int j=1;j<b.getInstructions().size();j++){
    var prev=b.getInstructions().get(j-1);
    if(ref(b.getInstructions().get(j)).equals("Lcom/hiro/ulike/LensLifecycle172;->n(Ljava/lang/String;)J")&&prev instanceof ReferenceInstruction ri&&ri.getReference() instanceof StringReference sr&&sr.getString().equals("lastFrame")){
     b.replaceInstruction(j,invoke(I,"deliveredTime",List.of(),"J"));hits++;
    }
   }
  }else throw new IllegalStateException("Unexpected runtime target "+id);
  req(hits==1,"Exactly one runtime bridge "+id);return repl(m,b);
 }
 static Map<String,ClassDef> metadata(Map<String,ClassDef> cs){
  var out=new TreeMap<String,ClassDef>();int edits=0;
  for(var c:cs.values()){var ms=new ArrayList<Method>();boolean changed=false;
   for(var m:c.getMethods()){
    if(m.getImplementation()==null){ms.add(m);continue;}
    var b=new MutableMethodImplementation(m.getImplementation());boolean hit=false;
    for(int j=0;j<b.getInstructions().size();j++){var x=b.getInstructions().get(j);
     if(x instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().startsWith("v1.9.28（v1.9.27基準）")){
      req(x.getOpcode()==Opcode.CONST_STRING,"Metadata encoding");b.replaceInstruction(j,new BuilderInstruction21c(Opcode.CONST_STRING,((OneRegisterInstruction)x).getRegisterA(),new ImmutableStringReference(DESCRIPTION)));edits++;hit=true;
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
   req(!old.containsKey(e.getKey()),"New target already patched "+e.getKey());var changed=apply(e.getValue());payload.put(e.getKey(),changed);
   rows.put(e.getKey(),new String[]{e.getKey(),MergePayloads.hash(e.getValue()),MergePayloads.hash(changed)});audit.add("STOCK\t"+String.join("\t",rows.get(e.getKey())));
  }
  for(var e:old.entrySet())req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(payload.get(e.getKey()))),"Existing native patch changed");
  var oldruntime=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var all=new TreeMap<String,ClassDef>();var edits=new TreeSet<String>();int keptMethods=0;
  for(var c:oldruntime.values()){var ms=new ArrayList<Method>();boolean changed=false;for(var m:c.getMethods()){
   if(RUNTIME.contains(MergePayloads.id(m))){var replacement=runtime(m);ms.add(replacement);edits.add(MergePayloads.id(m));changed=true;audit.add("RUNTIME\t"+MergePayloads.id(m)+"\t"+MergePayloads.hash(m)+"\t"+MergePayloads.hash(replacement));}else{ms.add(m);keptMethods++;}
  }all.put(c.getType(),changed?Transform1926.cls(c,ms):c);}
  req(edits.equals(RUNTIME),"Runtime repair coverage");var helper=MergePayloads.classes(a[1]);
  req(helper.containsKey(P)&&helper.containsKey(I)&&helper.containsKey(S),"Missing helper");
  for(var e:helper.entrySet()){req(e.getKey().matches("Lcom/hiro/ulike/(ProviderLifecycle1929|PreviewInputs1929|SessionFallback1929)(\\$[^;]+)?;"),"Stub leaked "+e.getKey());req(!all.containsKey(e.getKey()),"Helper collision");all.put(e.getKey(),e.getValue());}
  var after=MergePayloads.methods(all.values());for(var m:MergePayloads.methods(oldruntime.values()).values())if(!RUNTIME.contains(MergePayloads.id(m)))req(MergePayloads.hash(m).equals(MergePayloads.hash(after.get(MergePayloads.id(m)))),"Unrelated runtime changed");
  var single=metadata(MergePayloads.classes(base.resolve("classes.dex").toString()));var beforeBundle=MergePayloads.classes(a[2]);var bundle=metadata(beforeBundle);int kept=0;
  for(var e:beforeBundle.entrySet())if(!e.getKey().startsWith(MergePayloads.PATCH_NS)){req(MergePayloads.classHash(e.getValue()).equals(MergePayloads.classHash(bundle.get(e.getKey()))),"Other-app loader changed");kept++;}
  MergePayloads.writeDex(out.resolve("runtime.dex"),all.values());MergePayloads.writeDex(out.resolve("methods.dex"),MergePayloads.holders(payload));MergePayloads.writeDex(out.resolve("loader.dex"),single.values());MergePayloads.writeDex(out.resolve("bundle-loader.dex"),bundle.values());
  var lines=new ArrayList<String>();for(var row:rows.values())lines.add(String.join("\t",row));Files.write(out.resolve("methods.tsv"),lines);
  for(var e:MergePayloads.methods(MergePayloads.classes(out.resolve("methods.dex").toString()).values()).entrySet())req(MergePayloads.hash(e.getValue()).equals(rows.get(e.getKey())[2]),"Emitted target mismatch");
  audit.add("PASS\tnew_native_hooks="+TARGETS.size()+"\texisting_native_retained="+old.size()+"\texisting_runtime_methods_retained="+keptMethods+"\texisting_runtime_methods_changed="+edits.size()+"\thelper_classes_added="+helper.size()+"\tother_app_loaders_retained="+kept);Files.write(Path.of(a[5]),audit);audit.forEach(System.out::println);
 }
}
