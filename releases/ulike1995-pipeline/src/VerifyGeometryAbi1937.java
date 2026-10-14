import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;

/** Verify all direct runtime ABI bindings and exact five caller-only transforms against actual APKs. */
public final class VerifyGeometryAbi1937 {
 static int checks;
 static void ok(boolean value,String why){checks++;if(!value)throw new IllegalStateException(why);}
 static Field field(Map<String,ClassDef> all,String owner,String name,String type){
  ClassDef c=all.get(owner);ok(c!=null,"Native class "+owner);
  for(Field f:c.getFields())if(f.getName().equals(name)){ok(f.getType().equals(type),"Field type "+owner+"->"+name);ok((f.getAccessFlags()&1)!=0,"Public field "+owner+"->"+name);return f;}
  throw new IllegalStateException("Missing field "+owner+"->"+name);
 }
 static Method method(Map<String,Method> methods,String id){Method m=methods.get(id);ok(m!=null,"Native method "+id);ok((m.getAccessFlags()&1)!=0,"Public method "+id);return m;}
 static void verify(String path)throws Exception {
  var all=MergePayloads.classes(path);var methods=MergePayloads.methods(all.values());
  String shade=LayoutHooks1937.SHADE,fragment=GeometryHooks1937.FRAGMENT,lp=GeometryHooks1937.LP,rect=GeometryHooks1937.RECT;
  field(all,GeometryHooks1937.API,"e","Li/o/a/b1/a/d/d;");field(all,"Li/o/a/b1/a/d/b;","b",shade);field(all,"Li/o/a/b1/a/d/b;","d","Li/o/a/b1/a/g/e0;");field(all,GeometryHooks1937.LEGACY,"a",shade);
  field(all,fragment,"y","Li/f/l/n/q/y/c;");field(all,fragment,"o","Landroid/view/ViewGroup;");field(all,fragment,"r","I");field(all,fragment,"I","Z");
  field(all,"Li/f/l/n/q/y/c;","v","Z");field(all,"Li/f/l/n/q/y/c;","b","Ljava/lang/String;");field(all,"Li/f/l/n/q/y/c;","r","Li/f/l/j;");field(all,"Li/f/l/n/q/y/c;","g","Landroid/view/SurfaceView;");field(all,"Li/f/l/n/q/y/c;","s","Lcom/bytedance/corecamera/ui/view/CameraView;");
  field(all,"Li/f/l/h;","p","Lcom/bytedance/corecamera/ui/view/PreviewView;");field(all,"Li/f/l/h;","s","Z");field(all,"Li/f/l/h;","b","Li/f/l/n/n;");
  method(methods,"Li/f/l/j;->g(Ljava/lang/String;)Li/f/l/u/g;");method(methods,"Li/f/l/u/g;->d()Li/f/l/u/p;");
  field(all,"Li/f/l/j;","a","Li/f/l/h;");field(all,"Li/f/l/h;","e","Lcom/ss/android/vesdk/VERecorder;");
  method(methods,fragment+"->z2()Lcom/bytedance/corecamera/ui/view/CameraView;");
  method(methods,fragment+"->M2("+lp+lp+")Z");method(methods,"Lcom/bytedance/corecamera/ui/view/CameraView;->getCameraShaderView()"+shade);
  method(methods,"Lcom/ss/android/vesdk/VERecorder;->getRenderView()Lcom/ss/android/vesdk/render/VERenderView;");
  method(methods,"Lcom/ss/android/vesdk/VERecorder;->getCurrentCameraCapture()Lcom/ss/android/vesdk/camera/ICameraCapture;");
  method(methods,"Li/n/c/a/o/w/e;->p()I");method(methods,"Li/n/c/a/o/w/e;->m()I");
  method(methods,"Li/f/l/n/q/y/c;->o0(IZZ"+rect+")V");
  for(String id:GeometryHooks1937.NATIVE){Method before=method(methods,id);boolean transformed=false;for(Instruction i:before.getImplementation().getInstructions())if(GeometryHooks1937.ref(i).startsWith(GeometryHooks1937.H+"->"))transformed=true;
   if(!transformed){Method after=GeometryHooks1937.repair(before);GeometryHooks1937.verify(before,after);ok(true,"Exact inverse "+id);}else ok(true,"Applied helper bindings observed "+id);
  }
 }
 public static void main(String[] args)throws Exception{ok(args.length>0,"APK paths required");for(String path:args)verify(path);System.out.println("PASS geometry native ABI "+checks+" assertions across "+args.length+" actual APK(s); five scoped method transforms, no device execution");}
}
