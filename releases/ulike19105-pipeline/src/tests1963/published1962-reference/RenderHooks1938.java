import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.builder.Label;
/** Prefix-only native renderer observation; preserves native body, registers and handlers. */
public final class RenderHooks1938 {
 static final String R="Lcom/ss/android/vesdk/TECameraVideoRecorder;",H="Lcom/hiro/ulike/RenderStartup1938;";
 public static final Set<String> NATIVE=Set.of(
 R+"->startPreview(Landroid/view/Surface;)V",
 R+"->startPreviewAsync(Landroid/view/Surface;Lcom/ss/android/vesdk/VEListener$VECallListener;)V",
 R+"->surfaceCreated(Landroid/view/Surface;)V",
 R+"->surfaceDestroyed(Landroid/view/Surface;)V",
 R+"->stopPreview()V",
 R+"->stopPreviewAsync(Lcom/ss/android/vesdk/VEListener$VECallListener;Z)V",
 R+"->onPause()V", R+"->onDestroy()V", R+"->notifyRecState(IILjava/lang/String;)V",
 "Lcom/ss/android/vesdk/TECameraVideoRecorder$11;->run()V");
 public static final Set<String> RUNTIME=Set.of();
 private static void req(boolean value,String message){MergePayloads.require(value,message);}
 private static Method wrap(Method m,MethodImplementation body){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),body);}
 public static Method repair(Method old) {
  String id=MergePayloads.id(old);req(NATIVE.contains(id),"Unknown renderer hook "+id);
  if(old.getName().equals("run")) {
   req(old.getImplementation().getRegisterCount()==3 && old.getImplementation().getTryBlocks().isEmpty(),"Pinned renderer task ABI");
   MutableMethodImplementation body=new MutableMethodImplementation(old.getImplementation());
   body.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));Label rejected=body.newLabelForIndex(body.getInstructions().size()-1);
   body.addInstruction(0,new BuilderInstruction21t(Opcode.IF_EQZ,0,rejected));
   body.addInstruction(0,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
   body.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,0,1,new ImmutableMethodReference(H,"admitted",List.of("Lcom/ss/android/vesdk/VEListener$VECallListener;"),"Z")));
   body.addInstruction(0,new BuilderInstruction22c(Opcode.IGET_OBJECT,0,2,new ImmutableFieldReference("Lcom/ss/android/vesdk/TECameraVideoRecorder$11;","j","Lcom/ss/android/vesdk/VEListener$VECallListener;")));
   return wrap(old,body);
  }
  int registers,first,count=1;String method="cancelled";List<String> args=List.of("Ljava/lang/Object;");
  switch(old.getName()) {
   case "startPreview":case "surfaceCreated":registers=3;first=1;count=2;method="requested";args=List.of("Ljava/lang/Object;","Landroid/view/Surface;");break;
   case "startPreviewAsync":registers=4;first=1;count=3;method="requestedAsync";args=List.of("Ljava/lang/Object;","Landroid/view/Surface;","Lcom/ss/android/vesdk/VEListener$VECallListener;");break;
   case "surfaceDestroyed":registers=2;first=0;break;
   case "stopPreview":case "onPause":registers=2;first=1;break;
   case "stopPreviewAsync":registers=9;first=6;break;
   case "onDestroy":registers=4;first=3;break;
   case "notifyRecState":registers=7;first=3;count=2;method="event";args=List.of("Ljava/lang/Object;","I");break;
   default:throw new IllegalStateException(id);
  }
  req(old.getImplementation()!=null && old.getImplementation().getRegisterCount()==registers,"Pinned renderer register ABI "+id);
  req((old.getAccessFlags()&8)==0 && old.getReturnType().equals("V"),"Pinned renderer instance ABI");
  MutableMethodImplementation body=new MutableMethodImplementation(old.getImplementation());
  body.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,first,count,new ImmutableMethodReference(H,method,args,"V")));
  return wrap(old,body);
 }
 public static void verify(Method before,Method after) {
  req(MergePayloads.id(before).equals(MergePayloads.id(after)),"Renderer identity");
  req(MergePayloads.hash(repair(before)).equals(MergePayloads.hash(after)),"Exact renderer observation transform");
  MutableMethodImplementation restored=new MutableMethodImplementation(after.getImplementation());
  if(before.getName().equals("run")){restored.removeInstruction(restored.getInstructions().size()-1);restored.removeInstruction(0);restored.removeInstruction(0);restored.removeInstruction(0);}
  restored.removeInstruction(0);
  req(MergePayloads.hash(wrap(before,restored)).equals(MergePayloads.hash(before)),"Renderer inverse preserves every native instruction/register/branch/handler");
 }
}
