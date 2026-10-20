import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
/** Direct cross-package members and all six connected guards checked on the actual applied APK. */
public final class VerifyLayoutLifecycleAbi1937 {
 static final String S="Lcom/bytedance/corecamera/ui/view/CameraShadeView;", H="Lcom/hiro/ulike/LayoutLifecycle1937;", P="Lcom/hiro/ulike/PreviewLayout1922;";
 static void nativeAbi(Map<String,ClassDef> all){
  VerifyLensAbi1936.field(all,"Li/o/a/b1/a/d/b;","b",S,false,true);
  VerifyLensAbi1936.field(all,"Li/o/a/b1/a/d/b;","c","I",false,true);
  VerifyLensAbi1936.field(all,"Li/o/a/m/j/f;","a",S,false,true);
  VerifyLensAbi1936.field(all,"Li/o/a/m/j/f;","d","I",false,true);
  VerifyLensAbi1936.field(all,"Li/o/a/b1/a/d/a;","c","Li/o/a/b1/a/d/b;",false,true);
  VerifyLensAbi1936.field(all,"Li/o/a/m/j/b;","c","Li/o/a/m/j/f;",false,true);
  VerifyLensAbi1936.field(all,"Li/f/l/v/b/b;","c",S,false,true);
  VerifyLensAbi1936.field(all,S,"v","Lcom/ss/android/vesdk/VEPreviewRadio;",false,true);
  VerifyLensAbi1936.field(all,S,"w","Landroid/animation/ValueAnimator;",false,true);
 }
 public static void main(String[] args)throws Exception{
  var all=VerifyLensAbi1936.load(args[0]);VerifyLensAbi1936.checks=0;nativeAbi(all);
  if(args.length>1&&args[1].equals("native-only")){System.out.println("PASS layout lifecycle original native ABI checks="+VerifyLensAbi1936.checks);return;}
  VerifyLensAbi1936.method(all,P,"current("+S+")Z",true,true);
  VerifyLensAbi1936.method(all,P,"current("+S+"J)Z",true,true);
  VerifyLensAbi1936.method(all,P,"generation("+S+")J",true,true);
  VerifyLensAbi1936.method(all,P,"ownershipGeneration("+S+")J",true,true);
  for(String signature:List.of("camera(Li/o/a/b1/a/d/b;II)Z","preview(Li/o/a/m/j/f;I)Z","settled("+S+")Z",
    "postCamera(Landroid/os/Handler;Ljava/lang/Runnable;J)Z","postPreview(Landroid/os/Handler;Ljava/lang/Runnable;J)Z","postSettled(Landroid/os/Handler;Ljava/lang/Runnable;J)Z"))VerifyLensAbi1936.method(all,H,signature,true,true);
  Map<String,String> calls=new LinkedHashMap<>();
  calls.put("Li/o/a/b1/a/d/b;->h(Li/o/a/b1/a/d/b;IIZ)V",H+"->camera(Li/o/a/b1/a/d/b;II)Z");
  calls.put("Li/o/a/m/j/f;->l(Li/o/a/m/j/f;IZ)V",H+"->preview(Li/o/a/m/j/f;I)Z");
  calls.put("Lcom/bytedance/corecamera/ui/view/CameraShadeView$h;->b("+S+")V",H+"->settled("+S+")Z");
  calls.put("Li/o/a/b1/a/d/b;->g(IIZ)V",H+"->postCamera(Landroid/os/Handler;Ljava/lang/Runnable;J)Z");
  calls.put("Li/o/a/m/j/f;->k(IZ)V",H+"->postPreview(Landroid/os/Handler;Ljava/lang/Runnable;J)Z");
  calls.put("Lcom/bytedance/corecamera/ui/view/CameraShadeView$h;->onAnimationEnd(Landroid/animation/Animator;)V",H+"->postSettled(Landroid/os/Handler;Ljava/lang/Runnable;J)Z");
  for(var entry:calls.entrySet()){
   String key=entry.getKey();int split=key.indexOf("->");String type=key.substring(0,split),signature=key.substring(split+2);boolean isStatic=!signature.startsWith("g(")&&!signature.startsWith("k(")&&!signature.startsWith("onAnimationEnd(");
   Method m=VerifyLensAbi1936.method(all,type,signature,isStatic,true);var refs=VerifyLensAbi1936.refs(m);
   VerifyLensAbi1936.need(refs.stream().filter(entry.getValue()::equals).count()==1,"Exact connected lifecycle helper: "+key);
   if(entry.getValue().contains("->post"))VerifyLensAbi1936.need(!refs.contains("Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z"),"No unbound native Handler delivery: "+key);
  }
  System.out.println("PASS layout lifecycle native/helper ABI and six callback hooks checks="+VerifyLensAbi1936.checks+". No physical device execution.");
 }
}
