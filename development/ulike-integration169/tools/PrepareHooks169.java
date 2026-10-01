import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Candidate-only entry and exact platform-call hooks; every effective predecessor is hash-bound. */
public final class PrepareHooks169 {
 static final String PREFIX="Lcom/hiro/ulike/integration169/";
 static Method replace(Method m,MethodImplementation body){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),body);}
 static ImmutableMethodReference helper(String cls,String name,List<String> args,String ret){return new ImmutableMethodReference(PREFIX+cls+";",name,args,ret);}
 static int parameterWords(Method m){int n=(m.getAccessFlags()&8)==0?1:0;for(var p:m.getParameterTypes())n+=p.equals("J")||p.equals("D")?2:1;return n;}
 static Method entry(Method m,String cls,String name,List<String> params,String ret,String mode){
  var b=new MutableMethodImplementation(m.getImplementation());int words=parameterWords(m),start=b.getRegisterCount()-words;

  BuilderInstruction call=words==0?new BuilderInstruction35c(Opcode.INVOKE_STATIC,0,0,0,0,0,0,helper(cls,name,params,ret)):new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,start,words,helper(cls,name,params,ret));
  if(mode.equals("observe")){b.addInstruction(0,call);return replace(m,b);}
  MergePayloads.require(start>0,"branch hook needs dead local v0");
  Label original=b.newLabelForIndex(0);
  // The original first instruction remains the target after insertions.
  b.addInstruction(0,call);b.addInstruction(1,new BuilderInstruction11x(ret.equals("Z")?Opcode.MOVE_RESULT:Opcode.MOVE_RESULT_OBJECT,0));
  b.addInstruction(2,new BuilderInstruction21t(Opcode.IF_EQZ,0,original));
  b.addInstruction(3,mode.equals("voidTrue")?new BuilderInstruction10x(Opcode.RETURN_VOID):new BuilderInstruction11x(ret.equals("Z")?Opcode.RETURN:Opcode.RETURN_OBJECT,0));
  return replace(m,b);
 }
 static void addRuntime(Map<String,Method> all,Map<String,Method> changes,String id,String cls,String helper,List<String>params,String ret,String mode){
  Method m=all.get(id);MergePayloads.require(m!=null,"missing runtime "+id);changes.put(id,entry(m,cls,helper,params,ret,mode));
 }
 static Method calls(Method m){
  var b=new MutableMethodImplementation(m.getImplementation());int changed=0;
  for(int i=0;i<b.getInstructions().size();i++){
   var in=b.getInstructions().get(i);if(!(in instanceof ReferenceInstruction ri)||!(ri.getReference() instanceof MethodReference mr))continue;
   ImmutableMethodReference replacement=null;
   if(mr.getDefiningClass().equals("Lcom/hiro/ulike/OpticalZoom;")&&mr.getName().equals("capture"))replacement=helper("CameraBridge169","capture",List.of("Landroid/hardware/camera2/CameraCaptureSession;","Landroid/hardware/camera2/CaptureRequest;","Landroid/hardware/camera2/CameraCaptureSession$CaptureCallback;","Landroid/os/Handler;"),"I");
   if(mr.getDefiningClass().equals("Lcom/hiro/ulike/OpticalZoom;")&&mr.getName().equals("burst"))replacement=helper("CameraBridge169","burst",List.of("Landroid/hardware/camera2/CameraCaptureSession;","Ljava/util/List;","Landroid/hardware/camera2/CameraCaptureSession$CaptureCallback;","Landroid/os/Handler;"),"I");
   if(mr.getDefiningClass().equals("Lcom/hiro/ulike/OpticalZoom;")&&mr.getName().equals("build"))replacement=helper("CameraBridge169","build",List.of("Landroid/hardware/camera2/CaptureRequest$Builder;"),"Landroid/hardware/camera2/CaptureRequest;");
   if(replacement!=null){
    if(in instanceof FiveRegisterInstruction regs)b.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,regs.getRegisterCount(),regs.getRegisterC(),regs.getRegisterD(),regs.getRegisterE(),regs.getRegisterF(),regs.getRegisterG(),replacement));
    else if(in instanceof RegisterRangeInstruction regs)b.replaceInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,regs.getStartRegister(),regs.getRegisterCount(),replacement));else throw new IllegalStateException("unexpected invoke format");changed++;
   }
  }
  MergePayloads.require(changed==(m.getName().equals("j")?2:1),"unexpected platform call inventory "+MergePayloads.id(m)+" "+changed);return replace(m,b);
 }
 public static void main(String[] args)throws Exception{
  if(args.length!=4)throw new IllegalArgumentException("STOCK BASE_METHODS BASE_RUNTIME OUT");
  MergePayloads.require(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(args[1])))).equals("8cd6db67f2832bbcc45a1fe7eb78eddd69ead2eacb943940a7f981ea08dfdc8d"),"released v165 application payload changed");
  MergePayloads.require(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(args[2])))).equals("0c559c317c26d1306da989003c9a343c59ba53615e96ee533ebfb7c30f6dea0e"),"released v165 runtime changed");
  Path out=Path.of(args[3]);Files.createDirectories(out);
  var stock=MergePayloads.methods(MergePayloads.classes(args[0]).values());var base=MergePayloads.methods(MergePayloads.classes(args[1]).values());var runtime=MergePayloads.classes(args[2]);var rm=MergePayloads.methods(runtime.values());
  var rc=new TreeMap<String,Method>();String cy="Lcom/hiro/ulike/CaptureYuv;",ss="Lcom/hiro/ulike/StyleStill4;";
  addRuntime(rm,rc,cy+"->init(Landroid/content/Context;)V","AppHook169","init",List.of("Landroid/content/Context;"),"V","observe");
  addRuntime(rm,rc,cy+"->install(Landroid/preference/PreferenceActivity;)V","AppHook169","install",List.of("Landroid/preference/PreferenceActivity;"),"V","observe");
  addRuntime(rm,rc,ss+"->choose(Ljava/lang/Object;Ljava/lang/Object;)Z","AppHook169","choice",List.of("Ljava/lang/Object;","Ljava/lang/Object;"),"V","observe");
  addRuntime(rm,rc,ss+"->configureRequest(Ljava/lang/Object;Ljava/lang/Object;)Z","AppHook169","request",List.of("Ljava/lang/Object;","Ljava/lang/Object;"),"V","observe");
  addRuntime(rm,rc,cy+"->session(Ljava/lang/Object;Landroid/hardware/camera2/CameraDevice;Landroid/hardware/camera2/params/SessionConfiguration;)V","CameraBridge169","session",List.of("Ljava/lang/Object;","Landroid/hardware/camera2/CameraDevice;","Landroid/hardware/camera2/params/SessionConfiguration;"),"Z","voidTrue");
  addRuntime(rm,rc,cy+"->reader(Ljava/lang/Object;Landroid/media/ImageReader;)Landroid/media/ImageReader;","CameraBridge169","reader",List.of("Ljava/lang/Object;","Landroid/media/ImageReader;"),"Landroid/media/ImageReader;","returnTrue");
  addRuntime(rm,rc,cy+"->isReady()Z","CameraBridge169","ready",List.of(),"Z","returnTrue");
  addRuntime(rm,rc,cy+"->release(Ljava/lang/Object;)V","CameraBridge169","release",List.of("Ljava/lang/Object;"),"V","observe");
  ArrayList<ClassDef> emitted=new ArrayList<>();for(ClassDef c:runtime.values()){boolean touched=false;ArrayList<Method> methods=new ArrayList<>();for(Method m:c.getMethods()){Method n=rc.get(MergePayloads.id(m));methods.add(n==null?m:n);if(n!=null)touched=true;}if(touched)emitted.add(new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));}
  MergePayloads.writeDex(out.resolve("runtime-overrides.dex"),emitted);Files.write(out.resolve("runtime-allowlist.txt"),rc.keySet());
  ArrayList<String> runtimeContracts=new ArrayList<>();for(var e:rc.entrySet())runtimeContracts.add(e.getKey()+"\t"+MergePayloads.hash(rm.get(e.getKey()))+"\t"+MergePayloads.hash(e.getValue()));Files.write(out.resolve("runtime-contracts.tsv"),runtimeContracts);
  var dc=new TreeMap<String,Method>();
  String root="Li/s/a/w/h0/b;->";
  for(String signature:List.of(root+"j(Landroid/hardware/camera2/CaptureRequest$Builder;Landroid/hardware/camera2/CameraCaptureSession$CaptureCallback;Landroid/os/Handler;)Li/s/a/w/h0/b$h;",root+"k(Landroid/hardware/camera2/CaptureRequest;Landroid/hardware/camera2/CameraCaptureSession$CaptureCallback;Landroid/os/Handler;)Li/s/a/w/h0/b$h;",root+"l(Ljava/util/List;Landroid/hardware/camera2/CameraCaptureSession$CaptureCallback;Landroid/os/Handler;)Li/s/a/w/h0/b$h;")){
   Method m=base.getOrDefault(signature,stock.get(signature));MergePayloads.require(m!=null,"capture call not found "+signature);dc.put(signature,calls(m));
  }
  for(String shot:List.of("H0","J0")) {
   String key="Li/s/a/w/d0/a;->"+shot+"()V";Method original=base.getOrDefault(key,stock.get(key));MergePayloads.require(original!=null,"missing still callback "+key);
   var body=new MutableMethodImplementation(original.getImplementation());int count=0;
   for(int i=body.getInstructions().size()-1;i>=0;i--) {
    var instruction=body.getInstructions().get(i);
    if(instruction instanceof ReferenceInstruction reference && reference.getReference() instanceof MethodReference target && target.getDefiningClass().equals("Li/s/a/w/d0/a;") && target.getName().equals("W0") && target.getParameterTypes().equals(List.of("Landroid/hardware/camera2/CaptureRequest$Builder;"))) {
     var hook=helper("CameraBridge169","configureRequest",List.of("Ljava/lang/Object;","Landroid/hardware/camera2/CaptureRequest$Builder;"),"V");
     if(instruction instanceof FiveRegisterInstruction regs)body.addInstruction(i+1,new BuilderInstruction35c(Opcode.INVOKE_STATIC,regs.getRegisterCount(),regs.getRegisterC(),regs.getRegisterD(),regs.getRegisterE(),regs.getRegisterF(),regs.getRegisterG(),hook));
     else if(instruction instanceof RegisterRangeInstruction regs)body.addInstruction(i+1,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,regs.getStartRegister(),regs.getRegisterCount(),hook));
     else throw new IllegalStateException("unexpected still config invoke format");
     count++;
    }
   }
   MergePayloads.require(count==1,"still configuration invoke inventory "+key+" "+count);dc.put(key,replace(original,body));
  }
  String dimensions="Li/f/l/r/s;->c(II)V";Method dm=base.getOrDefault(dimensions,stock.get(dimensions));MergePayloads.require(dm!=null,"real picture dimensions callback missing");dc.put(dimensions,entry(dm,"SavedUriHandoff169","dimensions",List.of("Ljava/lang/Object;","I","I"),"Z","voidTrue"));
  MergePayloads.writeDex(out.resolve("methods-delta.dex"),MergePayloads.holders(dc));
  ArrayList<String> contracts=new ArrayList<>();for(var e:dc.entrySet())contracts.add(e.getKey()+"\t"+MergePayloads.hash(base.getOrDefault(e.getKey(),stock.get(e.getKey())))+"\t"+MergePayloads.hash(e.getValue()));Files.write(out.resolve("method-contracts.tsv"),contracts);
  System.out.println("PASS generated 8 runtime hooks +6 stock submit/builder/handoff hooks; candidate gate remains disabled.");
 }
}
