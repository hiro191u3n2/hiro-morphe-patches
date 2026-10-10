import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Check actual stock/applied DEX members and renderer lifecycle evidence.
 * Reflection fixtures are deliberately not input to this verifier. */
public final class VerifyRenderAbi1938 {
 static final String R="Lcom/ss/android/vesdk/TECameraVideoRecorder;";
 static final String BASE="Lcom/ss/android/vesdk/TERecorderBase;";
 static final String VIEW="Lcom/ss/android/vesdk/render/VERenderView;";
 static final String CALLBACK="Lcom/ss/android/vesdk/VEListener$VECallListener;";
 static final String TASK="Lcom/ss/android/vesdk/TECameraVideoRecorder$11;";
 static final String H="Lcom/hiro/ulike/RenderStartup1938;";
 static int checks;
 static void need(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static Map<String,ClassDef> load(String path)throws Exception {
  var all=new TreeMap<String,ClassDef>();var dex=DexFileFactory.loadDexContainer(new File(path),Opcodes.forApi(26));
  for(String entry:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(entry).getDexFile().getClasses())
   need(all.put(c.getType(),c)==null,"Duplicate class "+c.getType());
  return all;
 }
 static Method method(Map<String,ClassDef> all,String id) {
  String owner=id.substring(0,id.indexOf("->"));ClassDef c=all.get(owner);need(c!=null,"Missing owner "+owner);
  for(Method m:c.getMethods())if(m.toString().equals(id))return m;
  throw new AssertionError("Missing endpoint "+id);
 }
 static Field field(Map<String,ClassDef> all,String owner,String name,String type) {
  for(String current=owner;current!=null;) {
   ClassDef c=all.get(current);if(c==null)break;
   for(Field f:c.getFields())if(f.getName().equals(name)){need(f.getType().equals(type),"Wrong field ABI "+owner+"->"+name);return f;}
   current=c.getSuperclass();
  }
  throw new AssertionError("Missing field "+owner+"->"+name);
 }
 static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static int index(Method m,String target){int n=0;for(Instruction i:m.getImplementation().getInstructions()){if(ref(i).equals(target))return n;n++;}return -1;}
 static boolean literal(Method m,long value){for(Instruction i:m.getImplementation().getInstructions())if(i instanceof WideLiteralInstruction w&&w.getWideLiteral()==value)return true;return false;}
 static void publicMethod(Map<String,ClassDef> all,String id){need((method(all,id).getAccessFlags()&9)==1,"Expected public instance endpoint "+id);}
 static void verify(Map<String,ClassDef> all) {
  for(String name:List.of("mRenderEnvActive","t1","a1"))field(all,R,name,"Z");
  field(all,R,"mCurRecordStatus","I");
  field(all,TASK,"j",CALLBACK);field(all,TASK,"c","Landroid/view/Surface;");field(all,TASK,"k",R);
  field(all,"Lcom/ss/android/vesdk/VECameraCapture;","a","Lcom/ss/android/vesdk/VECameraSettings;");
  publicMethod(all,R+"->getCurrentCameraCapture()Lcom/ss/android/vesdk/camera/ICameraCapture;");
  publicMethod(all,BASE+"->getRenderView()"+VIEW);
  publicMethod(all,VIEW+"->getSurface()Landroid/view/Surface;");
  String surfaceView="Lcom/ss/android/vesdk/render/VERenderSurfaceView;";
  need(all.get(surfaceView)!=null&&VIEW.equals(all.get(surfaceView).getSuperclass()),"Surface renderer subtype owns the Android SurfaceView");
  publicMethod(all,surfaceView+"->getSurfaceView()Landroid/view/SurfaceView;");
  publicMethod(all,"Lcom/ss/android/vesdk/VECameraSettings;->getCameraFacing()Lcom/ss/android/vesdk/VECameraSettings$CAMERA_FACING_ID;");
  publicMethod(all,R+"->startPreviewAsync(Landroid/view/Surface;"+CALLBACK+")V");
  publicMethod(all,CALLBACK+"->onDone(I)V");
  need((all.get(CALLBACK).getAccessFlags()&0x201)==0x201,"Callback remains public interface");
  Method async=method(all,R+"->startPreviewAsync(Landroid/view/Surface;"+CALLBACK+")V");
  need(index(async,TASK+"-><init>("+R+"Landroid/view/Surface;"+CALLBACK+")V")>=0,"Async request retains marker-bearing task");
  need(index(async,R+"->executeSafeSubmit(Ljava/lang/Runnable;)V")>=0,"Async request retains native dispatch");
  Method task=method(all,TASK+"->run()V");
  need(index(task,R+"->startRecordPreview(Landroid/view/Surface;)I")>=0,"Worker starts actual native renderer");
  need(index(task,CALLBACK+"->onDone(I)V")>=0,"Worker preserves native completion callback");
  Method start=method(all,R+"->startRecordPreview(Landroid/view/Surface;)I");
  need((start.getAccessFlags()&0x20000)!=0,"Native renderer start remains synchronized");
  need(literal(start,0)&&literal(start,1)&&literal(start,-105),"Native start retains stopped-state acceptance / live-state rejection");
  String gl="Lcom/ss/android/vesdk/TECameraVideoRecorder$37;";
  for(String name:List.of("onOpenGLCreate","onOpenGLDestroy")) {
   Method m=method(all,gl+"->"+name+"()V");boolean create=name.equals("onOpenGLCreate");
   need(index(m,R+"->mRenderEnvActive:Z")>=0,"Native GL lifecycle writes active state");
   need(literal(m,create?1000:1001),"Native renderer event constant");
   need(index(m,R+"->notifyRecState(IILjava/lang/String;)V")>=0,"Native GL lifecycle emits recorder event");
  }
  for(String id:RenderHooks1938.NATIVE)method(all,id);
  boolean applied=all.containsKey(H);
  if(applied) {
   ClassDef marker=all.get("Lcom/hiro/ulike/RenderStartup1938$Replay;");
   need(marker!=null&&marker.getInterfaces().contains(CALLBACK),"Replay implements actual native callback interface");
   String admitted=H+"->admitted("+CALLBACK+")Z";
   need(index(task,admitted)>=0&&index(task,admitted)<index(task,R+"->startRecordPreview(Landroid/view/Surface;)I"),"Ticket gate precedes SDK renderer execution");
   need(index(async,H+"->requestedAsync(Ljava/lang/Object;Landroid/view/Surface;"+CALLBACK+")V")>=0,"Async entry distinguishes helper replay from normal request");
   Method admission=method(all,admitted);
   need(index(admission,"Lcom/hiro/ulike/ExitBusy1921;->captureBusy(Z)Z")>=0,"Worker checks actual still-capture state");
   need(index(admission,"Landroid/os/SystemClock;->uptimeMillis()J")>=0,"Worker checks bounded deadline");
   for(Instruction i:admission.getImplementation().getInstructions())need(!ref(i).startsWith("Landroid/view/View;")&&!ref(i).startsWith("Landroid/view/SurfaceView;"),"No UI View access on SDK command worker");
   for(String name:List.of("stopPreview()V","stopPreviewAsync("+CALLBACK+"Z)V","onPause()V","onDestroy()V","surfaceDestroyed(Landroid/view/Surface;)V"))
    need(index(method(all,R+"->"+name),H+"->cancelled(Ljava/lang/Object;)V")==0,"Cancellation hook precedes native lifecycle body "+name);
   need(index(method(all,R+"->notifyRecState(IILjava/lang/String;)V"),H+"->event(Ljava/lang/Object;I)V")==0,"Native GL events observed independently from camera frames");
   for(ClassDef c:all.values())if(c.getType().startsWith("Lcom/hiro/ulike/RenderStartup1938"))
    for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions()) {
     String target=ref(i);
     need(!target.equals("Landroid/graphics/SurfaceTexture;->attachToGLContext(I)V")&&!target.equals("Landroid/graphics/SurfaceTexture;->detachFromGLContext()V"),"Helper does not manipulate GL attachment");
     need(!(i.getOpcode()==Opcode.NEW_INSTANCE&&(target.equals("Landroid/graphics/SurfaceTexture;")||target.equals("Landroid/view/Surface;"))),"Native SDK retains ownership of GL objects");
    }
  }
  System.out.println("PASS render1938 native ABI/lifecycle checks="+checks+" applied="+applied+"; no device execution");
 }
 public static void main(String[] args)throws Exception {
  if(args.length<1||args.length>2)throw new IllegalArgumentException("ORIGINAL_OR_APPLIED_APK [APPLIED_APK]");
  Map<String,ClassDef> previous=null;
  for(String arg:args){checks=0;var all=load(arg);checks=0;verify(all);
   if(previous!=null)for(String id:RenderHooks1938.NATIVE)RenderHooks1938.verify(method(previous,id),method(all,id));
   previous=all;
  }
 }
}
