import java.util.*;import java.nio.file.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.builder.*;import com.android.tools.smali.dexlib2.builder.instruction.*;import com.android.tools.smali.dexlib2.immutable.reference.*;import com.android.tools.smali.dexlib2.formatter.DexFormatter;
public final class VerifyAudit1929 {
 static void ck(boolean b,String m){MergePayloads.require(b,m);}
 static String ref(Instruction i){return Transform1929.ref(i);}
 static int at(List<? extends Instruction> xs,int offset){int off=0;for(int n=0;n<xs.size();n++){if(off==offset)return n;off+=xs.get(n).getCodeUnits();}return -1;}
 static int off(List<? extends Instruction> xs,int index){int n=0;for(int i=0;i<index;i++)n+=xs.get(i).getCodeUnits();return n;}
 static void inverse(Method actual,Method seed,MutableMethodImplementation b){ck(MergePayloads.hash(Transform1929.repl(actual,b)).equals(MergePayloads.hash(seed)),"Native control flow inverse mismatch "+MergePayloads.id(actual));}
 public static void main(String[]a)throws Exception{
  ck(a.length==4||a.length==5,"BASE SEED METHODS RUNTIME [STOCK]");Path base=Path.of(a[0]);
  var old=MergePayloads.methods(MergePayloads.classes(base.resolve("ulike/methods.dex").toString()).values());
  var previousClasses=MergePayloads.classes(base.resolve("ulike/runtime.dex").toString());var previous=MergePayloads.methods(previousClasses.values());
  var seed=MergePayloads.methods(MergePayloads.classes(a[1]).values());var ms=MergePayloads.methods(MergePayloads.classes(a[2]).values());var runtime=MergePayloads.classes(a[3]);var rs=MergePayloads.methods(runtime.values());
  ck(seed.keySet().equals(Transform1929.TARGETS),"seed scope");ck(ms.size()==old.size()+9,"native target count");
  for(var e:old.entrySet())ck(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(ms.get(e.getKey()))),"Previous native/image/back/facing patch changed "+e.getKey());
  int retained=0;for(var e:previous.entrySet())if(!Transform1929.RUNTIME.contains(e.getKey())){ck(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(rs.get(e.getKey()))),"Non-target runtime changed "+e.getKey());retained++;}
  Method m=ms.get(Transform1929.START);var b=new MutableMethodImplementation(m.getImplementation());var ins=b.getInstructions();
  ck(ref(ins.get(0)).equals(Transform1929.I+"->before(Ljava/lang/Object;)Z")&&((RegisterRangeInstruction)ins.get(0)).getStartRegister()==b.getRegisterCount()-1,"guard uses self");
  ck(ins.get(2).getOpcode()==Opcode.IF_NEZ&&at(ins,off(ins,2)+((OffsetInstruction)ins.get(2)).getCodeOffset())==5,"success jumps to original entry, not retry guard");
  ck(((NarrowLiteralInstruction)ins.get(3)).getNarrowLiteral()==-100&&ins.get(4).getOpcode()==Opcode.RETURN,"not-ready returns native readiness error");
  for(int n=4;n>=0;n--)b.removeInstruction(n);inverse(m,seed.get(Transform1929.START),b);
  m=ms.get(Transform1929.FIRST);b=new MutableMethodImplementation(m.getImplementation());int hits=0;
  for(int n=0;n<b.getInstructions().size();n++)if(ref(b.getInstructions().get(n)).equals("Li/s/a/w/h0/b$h;->a:Z")){
   ins=b.getInstructions();ck(ins.get(n+1).getOpcode()==Opcode.IF_NEZ&&((NarrowLiteralInstruction)ins.get(n+2)).getNarrowLiteral()==-108&&ins.get(n+3).getOpcode()==Opcode.RETURN,"failed first request cannot report preview success");
   int dest=at(ins,off(ins,n+1)+((OffsetInstruction)ins.get(n+1)).getCodeOffset());ck(dest>n+3&&ref(ins.get(dest)).equals("Li/s/a/w/h0/b;->h:Lcom/ss/android/ttvecamera/TECameraSettings;"),"successful request retains native destination");
   b.removeInstruction(n+3);b.removeInstruction(n+2);hits++;
  }ck(hits==1,"first-request failure guard");inverse(m,seed.get(Transform1929.FIRST),b);
  m=ms.get(Transform1929.PIXEL);b=new MutableMethodImplementation(m.getImplementation());hits=0;
  for(int n=0;n<b.getInstructions().size();n++)if(ref(b.getInstructions().get(n)).equals(Transform1929.I+"->pixel(Ljava/lang/Object;Ljava/lang/Object;)V")){
   ck(ref(b.getInstructions().get(n-1)).equals("Li/s/a/w/l0/b$c;->onFrameCaptured(Li/s/a/w/m;)V"),"pixel marker after real listener call only");var r=(FiveRegisterInstruction)b.getInstructions().get(n);ck(r.getRegisterC()==b.getRegisterCount()-2&&r.getRegisterD()==b.getRegisterCount()-1,"pixel receiver/frame arguments");b.removeInstruction(n);hits++;
  }ck(hits==1,"one delivered-frame marker");inverse(m,seed.get(Transform1929.PIXEL),b);
  m=ms.get(Transform1929.ADD);b=new MutableMethodImplementation(m.getImplementation());hits=0;
  for(int n=0;n<b.getInstructions().size();n++)if(ref(b.getInstructions().get(n)).equals(Transform1929.P+"->liveManager(Ljava/lang/Object;)Li/s/a/w/l0/c;")){
   int r=((FiveRegisterInstruction)b.getInstructions().get(n)).getRegisterC();b.replaceInstruction(n,new BuilderInstruction35c(Opcode.INVOKE_INTERFACE,1,r,0,0,0,0,new ImmutableMethodReference("Li/s/a/w/a;","U",List.of(),"Li/s/a/w/l0/c;")));hits++;
  }ck(hits==1,"one reuse substitution");inverse(m,seed.get(Transform1929.ADD),b);
  m=ms.get(Transform1929.RELEASE_CTOR);b=new MutableMethodImplementation(m.getImplementation());ck(ref(b.getInstructions().get(2)).equals(Transform1929.P+"->captureRelease(Ljava/lang/Object;Ljava/lang/Object;)V"),"capture ownership at construction");b.removeInstruction(2);inverse(m,seed.get(Transform1929.RELEASE_CTOR),b);
  m=rs.get(Transform1929.WATCH);b=new MutableMethodImplementation(m.getImplementation());hits=0;
  for(int n=0;n<b.getInstructions().size();n++)if(ref(b.getInstructions().get(n)).equals(Transform1929.I+"->deliveredTime()J")){
   int r=((OneRegisterInstruction)b.getInstructions().get(n-1)).getRegisterA();b.replaceInstruction(n,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,r,0,0,0,0,new ImmutableMethodReference("Lcom/hiro/ulike/LensLifecycle172;","n",List.of("Ljava/lang/String;"),"J")));hits++;
  }ck(hits==1,"one watchdog heartbeat substitution");inverse(m,previous.get(Transform1929.WATCH),b);
  m=rs.get(Transform1929.FOREGROUND);b=new MutableMethodImplementation(m.getImplementation());ck(b.getInstructions().size()==4&&ref(b.getInstructions().get(0)).equals(Transform1929.I+"->foreground(Ljava/lang/Object;)V"),"no extra foreground epoch reset");
  b.replaceInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,0,1,new ImmutableMethodReference("Lcom/hiro/ulike/StartupReset184;","beforeForeground",List.of("Ljava/lang/Object;"),"V")));inverse(m,previous.get(Transform1929.FOREGROUND),b);
  int direct=0,reflect=0;
  var all=new TreeMap<>(runtime);if(a.length==5)all.putAll(MergePayloads.classes(a[4]));else all.putAll(MergePayloads.classes(a[1]));
  var fields=new TreeMap<String,Field>();for(var c:all.values())for(var f:c.getFields())fields.put(DexFormatter.INSTANCE.getFieldDescriptor(f),f);var allMethods=MergePayloads.methods(all.values());
  for(String id:List.of("Lcom/hiro/ulike/ManualLens170;->PINNED:Ljava/util/Map;","Lcom/hiro/ulike/ManualLens170;->SESSION_TOKENS:Ljava/util/Map;","Lcom/hiro/ulike/ManualLens170;->CURRENT_SESSIONS:Ljava/util/Map;","Lcom/hiro/ulike/ManualLens170$State;->r:Lcom/hiro/ulike/OpticalZoom$Route;","Lcom/hiro/ulike/ManualLens170$State;->next:Landroid/hardware/camera2/CameraCaptureSession$StateCallback;","Lcom/hiro/ulike/ManualLens170$State;->token:J")){ck(fields.containsKey(id),"Reflected runtime field "+id);reflect++;}
  if(a.length==5){
   for(var e:seed.entrySet())ck(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(allMethods.get(e.getKey()))),"Original APK seed checksum "+e.getKey());
   for(var c:runtime.values())if(c.getType().matches("Lcom/hiro/ulike/(ProviderLifecycle1929|PreviewInputs1929|SessionFallback1929)(\\$[^;]+)?;"))for(var method:c.getMethods())if(method.getImplementation()!=null)for(var i:method.getImplementation().getInstructions())if(i instanceof ReferenceInstruction ri){
    if(ri.getReference() instanceof FieldReference fr&&(fr.getDefiningClass().startsWith("Li/s/")||fr.getDefiningClass().startsWith("Lcom/ss/"))){var f=fields.get(DexFormatter.INSTANCE.getFieldDescriptor(fr));ck(f!=null&&(f.getAccessFlags()&1)!=0,"Native field ABI "+fr);direct++;}
    if(ri.getReference() instanceof MethodReference mr&&(mr.getDefiningClass().startsWith("Li/s/")||mr.getDefiningClass().startsWith("Lcom/ss/"))){var f=allMethods.get(DexFormatter.INSTANCE.getMethodDescriptor(mr));ck(f!=null&&(f.getAccessFlags()&1)!=0,"Native method ABI "+mr);direct++;}
   }
   for(String id:List.of("Li/s/a/w/q;->mProviderManager:Li/s/a/w/l0/c;","Li/s/a/w/q;->mSurfaceTextureLock:Ljava/lang/Object;","Li/s/a/w/q;->mbNeedReleaseSurfaceTexture:Z","Li/s/a/w/l0/c;->a:Li/s/a/w/l0/b;","Li/s/a/w/l0/b;->d:Li/s/a/w/a;","Li/s/a/w/l0/b;->a:Li/s/a/w/l0/b$c;","Li/s/a/w/l0/b;->j:Li/s/a/w/l0/b$c;","Li/s/a/w/l0/b;->k:Li/s/a/w/l0/b$c;","Li/s/a/w/l0/b;->h:Z","Li/s/a/w/l0/b;->e:Z","Li/s/a/w/l0/b;->b:Li/s/a/w/m$d;","Li/s/a/w/h0/b;->g:Li/s/a/w/g;","Li/s/a/w/h0/b;->k:Landroid/os/Handler;")){ck(fields.containsKey(id),"Reflected native field "+id);reflect++;}
   for(String id:List.of("Li/s/a/w/a;->U()Li/s/a/w/l0/c;","Li/s/a/w/l0/c;->h()Li/s/a/w/l0/b;","Li/s/a/w/l0/c;->n()V","Li/s/a/w/l0/b;->f()Landroid/graphics/SurfaceTexture;","Li/s/a/w/l0/b;->d()Landroid/view/Surface;","Li/s/a/w/l0/b;->p()V","Li/s/a/w/l0/b;->o()V","Lcom/ss/android/vesdk/frame/TECapturePipeline;->getFormat()Li/s/a/w/m$d;","Lcom/ss/android/vesdk/frame/TECapturePipeline;->getSurfaceTexture()Landroid/graphics/SurfaceTexture;","Lcom/ss/android/vesdk/frame/TETextureCapturePipeline;->getSurface()Landroid/view/Surface;","Lcom/ss/android/vesdk/frame/TERecorderCapturePipeline;->getRecorderSurface()Landroid/view/Surface;")){ck(allMethods.containsKey(id),"Reflected native method "+id);reflect++;}
  }
  System.out.println("PASS1929 native hooks=9; runtime changes=4; native methods retained="+old.size()+"; runtime methods retained="+retained+"; guard/first-request/listener/reuse/teardown/watchdog/foreground inverse CFG verified; direct native references="+direct+"; reflected members="+reflect+"; device untested");
 }
}
