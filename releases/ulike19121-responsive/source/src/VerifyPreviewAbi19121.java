import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import java.io.*;import java.nio.file.*;import java.security.*;import java.util.*;

/** Independent APK-level checks for the exact reflection chain and display-only
 * endpoint. Run against the fully applied APK, not only overlay helper dex. */
public final class VerifyPreviewAbi19121 {
    static Map<String,ClassDef> all;static int assertions;
    static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    static Field field(String owner,String name,String type){
        for(String at=owner;at!=null;){ClassDef c=all.get(at);if(c==null)break;for(Field f:c.getFields())if(f.getName().equals(name)){check(f.getType().equals(type),"Field type "+owner+"->"+name);return f;}at=c.getSuperclass();}
        throw new AssertionError("Missing field "+owner+"->"+name);
    }
    static Method method(String owner,String name,String params,String result){
        for(String at=owner;at!=null;){ClassDef c=all.get(at);if(c==null)break;for(Method m:c.getMethods())if(m.getName().equals(name)&&String.join("",m.getParameterTypes()).equals(params)&&m.getReturnType().equals(result)){check((m.getAccessFlags()&1)!=0,"Method must be public "+m);return m;}at=c.getSuperclass();}
        throw new AssertionError("Missing public method "+owner+"->"+name+"("+params+")"+result);
    }
    static List<String> invokes(Method m){List<String> refs=new ArrayList<>();for(Instruction i:m.getImplementation().getInstructions())if(i.getOpcode().name().startsWith("INVOKE_"))refs.add(ref(i));return refs;}
    static boolean references(Method m,String s){for(Instruction i:m.getImplementation().getInstructions())if(ref(i).equals(s))return true;return false;}
    static String hash(Path p)throws Exception{byte[] sum=MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p));StringBuilder out=new StringBuilder();for(byte b:sum)out.append(String.format("%02x",b));return out.toString();}
    static void verify() {
        field("Li/o/a/b1/a/b/f/a;","b","Li/o/a/b1/a/b/f/a$c;");
        field("Li/o/a/b1/a/b/c$a;","a","Li/o/a/b1/a/b/c;");
        field("Li/o/a/b1/a/b/c;","c","Li/o/a/b1/a/g/e0;");
        field("Li/o/a/b1/a/g/y;","a","Li/f/l/t/g;");
        method("Li/o/a/b1/a/b/c;","e","","Li/o/a/b1/a/g/e0;");
        method("Li/o/a/b1/a/g/y;","O0","","Li/f/l/t/g;");
        String fragment="Lcom/bytedance/corecamera/camera/basic/PureCameraFragment;";
        method(fragment,"getActivity","","Landroidx/fragment/app/FragmentActivity;");
        for(String n:List.of("isAdded","isResumed","isVisible"))method(fragment,n,"","Z");
        method(fragment,"getCameraState","","Li/f/l/u/g;");method(fragment,"c1","","Li/f/l/j;");
        method("Li/f/l/u/g;","l","","Li/f/l/u/j;");
        for(String n:List.of("a","q"))method("Li/f/l/u/j;",n,"","Li/f/l/u/p;");
        method("Li/f/l/u/p;","a","","Ljava/lang/Object;");
        method("Li/f/l/j;","l","","Lcom/ss/android/vesdk/VERecorder;");method("Li/f/l/j;","h","","Li/f/l/n/n;");
        method("Lcom/ss/android/vesdk/VERecorder;","getCurrentCameraCapture","","Lcom/ss/android/vesdk/camera/ICameraCapture;");
        field("Li/f/l/n/h;","b","Lcom/ss/android/vesdk/VERecorder;");field("Li/f/l/n/h;","h","Li/f/l/n/g;");field("Li/f/l/n/g;","a","Lcom/ss/android/vesdk/VERecorder;");
        Method m=method("Li/f/l/n/h;","M","Z","V");check(invokes(m).equals(List.of("Li/f/l/n/g;->M(Z)V")),"M only forwards texture-render hold");
        m=method("Li/f/l/n/g;","M","Z","V");check(invokes(m).equals(List.of("Lcom/ss/android/vesdk/VERecorder;->preventTextureRender(Z)V")),"M is exactly preventTextureRender");
        method("Li/f/l/n/s/a;","b","","Li/f/l/n/s/a;");field("Li/f/l/n/s/a;","b","Landroid/graphics/Bitmap;");
        String zoom="Lcom/hiro/ulike/OpticalZoom;";field(zoom,"LOCK","Ljava/lang/Object;");field(zoom,"capture","Ljava/lang/ref/WeakReference;");field(zoom,"active","Lcom/hiro/ulike/OpticalZoom$Route;");field(zoom,"epoch","J");for(String n:List.of("foreground","recording","switching"))field(zoom,n,"Z");
        String yuv="Lcom/hiro/ulike/CaptureYuv;",state="Lcom/hiro/ulike/CaptureYuv$State;";
        field(yuv,"active","Ljava/lang/ref/WeakReference;");field(yuv,"states","Ljava/util/Map;");for(String n:List.of("ready","closed"))field(state,n,"Z");for(String n:List.of("yuv","original"))field(state,n,"Landroid/media/ImageReader;");field("Li/s/a/w/d0/a;","e0","Landroid/media/ImageReader;");
        for(String suffix:List.of("13","15")){
            m=method("Lcom/ss/android/medialib/presenter/MediaRecordPresenter$"+suffix+";","onImage","[III","V");
            check(references(m,"Landroid/graphics/Bitmap;->createBitmap([IIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"),"per-shot image is created from completed pixels");
        }
        m=method("Lcom/ss/android/medialib/presenter/MediaRecordPresenter;","renderPictureToBitmap","ZLcom/ss/android/medialib/camera/ImageFrame;IIIZLcom/ss/android/medialib/presenter/MediaRecordPresenter$OnPictureCallbackV2;Lcom/ss/android/medialib/presenter/MediaRecordPresenter$OnVEFrameCallback;","V");
        check(references(m,"Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"),"direct bitmap is allocated for the shot");
        for(ClassDef c:all.values())if(c.getType().startsWith("Lcom/hiro/ulike/CapturePreview19121"))for(Method mm:c.getMethods())if(mm.getImplementation()!=null)for(String r:invokes(mm)){
            check(!r.contains("->startPreview(")&&!r.contains("->onResume(")&&!r.contains("->recycle(")&&!r.contains("->setPixels(")&&!r.contains("->copyPixelsFromBuffer("),"display helper must not restart session or mutate image: "+r);
        }
    }
    public static void main(String[] args)throws Exception {
        if(args.length==0)throw new IllegalArgumentException("APPLIED_APK...");
        for(String arg:args){all=new HashMap<>();var dex=DexFileFactory.loadDexContainer(new File(arg),Opcodes.forApi(26));for(String entry:dex.getDexEntryNames())for(ClassDef cls:dex.getEntry(entry).getDexFile().getClasses())all.put(cls.getType(),cls);assertions=0;verify();
            System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"apk_sha256\":\""+hash(Path.of(arg))+"\",\"exact_reflection_chain_verified\":true,\"display_only_endpoint_verified\":true,\"physical_android_tested\":false}");}
    }
}
