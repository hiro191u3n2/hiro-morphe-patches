import java.nio.file.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;

/** Check actual stock/applied members, accepted-photo call site, and separation
 * of the capture mask from the front-screen fill light. No device execution. */
public final class VerifyFeedbackAbi1941 {
 static int checks;
 static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
 static void req(boolean b,String s){checks++;MergePayloads.require(b,s);}
 static boolean call(Method m,String target){for(Instruction i:m.getImplementation().getInstructions())if(ref(i).equals(target))return true;return false;}
 static boolean androidViewCall(Method m,String signature){return call(m,"Landroid/view/View;->"+signature)||call(m,"Lcom/bytedance/corecamera/ui/view/CameraShadeView;->"+signature);}
 static Method method(Map<String,Method> ms,String id){Method m=ms.get(id);req(m!=null&&m.getImplementation()!=null,"Present native method "+id);return m;}
 static void field(Map<String,ClassDef> cs,String id){String type=id.substring(0,id.indexOf("->"));ClassDef c=cs.get(type);req(c!=null,"Present field owner "+type);boolean found=false;for(Field f:c.getFields())if(f.toString().equals(id))found=true;req(found,"Present native field "+id);}
 public static void main(String[] a)throws Exception {
  req(a.length==3,"APK OUT applied-boolean");boolean applied=Boolean.parseBoolean(a[2]);var classes=MergePayloads.classes(a[0]);var methods=MergePayloads.methods(classes.values());
  String y="Li/o/a/b1/a/g/y;",f="Li/o/a/b1/a/g/f0/f;",e="Li/o/a/b1/a/g/f0/e;";
  Method accepted=method(methods,y+"->K1(Z)V");req(call(accepted,FeedbackHooks1941.FLASH),"Actual capture-start calls q feedback event");
  Method flash=method(methods,FeedbackHooks1941.FLASH);
  req(call(flash,e+"->e()Landroid/view/View;"),"q gets dedicated photo-capture mask");
  req(call(flash,applied?FeedbackHooks1941.NEW_FLASH:FeedbackHooks1941.OLD_FLASH),"Expected native feedback target applied="+applied);
  req(call(flash,FeedbackHooks1941.OLD_FLASH),"Native white animation retained as conditional failure fallback");
  if(applied){boolean conditional=false;for(Instruction i:flash.getImplementation().getInstructions())if(i.getOpcode()==Opcode.IF_NEZ&&i instanceof OneRegisterInstruction&&((OneRegisterInstruction)i).getRegisterA()==2)conditional=true;req(conditional,"Successful drawable overlay skips original white blink");}
  Method mask=method(methods,e+"->e()Landroid/view/View;");req(call(mask,e+"->d:Landroid/view/View;"),"Dedicated mask field d");
  Method light=method(methods,e+"->f()Landroid/view/View;");req(call(light,e+"->b:Landroid/view/View;"),"Front-screen fill light field b differs from d");
  Method front=method(methods,f+"->p(Landroid/app/Activity;)V");req(call(front,e+"->f()Landroid/view/View;"),"Front fill uses separate b getter");req(call(front,"Li/f/l/v/b/e;->b(Landroid/view/View;Landroid/app/Activity;Landroid/os/Handler;)V"),"Native front exposure/brightness function preserved");
  field(classes,e+"->d:Landroid/view/View;");field(classes,e+"->b:Landroid/view/View;");
  for(String id:List.of("Lcom/bytedance/corecamera/ui/view/CameraShadeView;->w:Landroid/animation/ValueAnimator;","Lcom/bytedance/corecamera/ui/view/CameraShadeView;->n:Landroid/graphics/RectF;","Lcom/bytedance/corecamera/ui/view/CameraShadeView;->o:Landroid/graphics/RectF;"))field(classes,id);
  if(applied){
   for(String id:List.of("Lcom/hiro/ulike/PreviewLayout1922;->gestureView1925()Lcom/bytedance/corecamera/ui/view/CameraShadeView;","Lcom/hiro/ulike/PreviewLayout1922;->viewport()Landroid/graphics/RectF;","Lcom/hiro/ulike/ShutterFeedback1941;->show(Landroid/view/View;)Z"))method(methods,id);
   req(call(method(methods,"Lcom/hiro/ulike/ShutterFeedback1941;->showOnUi(Landroid/view/View;)Z"),"Landroid/view/ViewOverlay;->add(Landroid/graphics/drawable/Drawable;)V"),"Feedback is a drawable overlay, no touch-intercepting View");
   req(androidViewCall(method(methods,"Lcom/hiro/ulike/ShutterFeedback1941;->showOnUi(Landroid/view/View;)Z"),"addOnAttachStateChangeListener(Landroid/view/View$OnAttachStateChangeListener;)V"),"Internal detach cleanup listener registered");
   req(androidViewCall(method(methods,"Lcom/hiro/ulike/ShutterFeedback1941$Pulse;->clear(Lcom/bytedance/corecamera/ui/view/CameraShadeView;)V"),"removeOnAttachStateChangeListener(Landroid/view/View$OnAttachStateChangeListener;)V"),"Cleanup releases detach listener");
  }
  String summary="PASS feedback1941 native ABI checks="+checks+" applied="+applied+"; no device execution";Files.writeString(Path.of(a[1]),summary+"\n");System.out.println(summary);
 }
}
