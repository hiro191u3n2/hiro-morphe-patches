import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

/** Independent checks of reflection targets against the complete original APK.
 * Optional second APK must preserve those native ABI fields and lifecycle facts.
 */
public final class VerifyFrontAbi1936 {
    static final String CAP="Lcom/ss/android/vesdk/VECameraCapture;";
    static final String SETTINGS="Lcom/ss/android/ttvecamera/TECameraSettings;";
    static final String SERVER="Li/s/a/w/q;";
    static int checks;
    static void require(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static Map<String,ClassDef> load(String path)throws Exception{
        var container=DexFileFactory.loadDexContainer(new File(path),Opcodes.forApi(26));
        var out=new TreeMap<String,ClassDef>();
        for(String name:container.getDexEntryNames())for(var cls:container.getEntry(name).getDexFile().getClasses())
            require(out.put(cls.getType(),cls)==null,"Duplicate native class "+cls.getType());
        return out;
    }
    static Field field(Map<String,ClassDef> all,String owner,String name,String type){
        for(String current=owner;current!=null;){
            ClassDef cls=all.get(current);if(cls==null)break;
            for(Field f:cls.getFields())if(f.getName().equals(name)){
                require(f.getType().equals(type),"Reflection field descriptor changed: "+owner+"->"+name);
                return f;
            }
            current=cls.getSuperclass();
        }
        throw new AssertionError("Missing reflection field "+owner+"->"+name+":"+type);
    }
    static Method method(Map<String,ClassDef> all,String id){
        String owner=id.substring(0,id.indexOf("->"));ClassDef cls=all.get(owner);
        require(cls!=null,"Missing method owner "+owner);
        for(Method m:cls.getMethods())if(m.toString().equals(id)){require(true,"method");return m;}
        throw new AssertionError("Missing native endpoint "+id);
    }
    static String ref(Instruction insn){return insn instanceof ReferenceInstruction r?r.getReference().toString():"";}
    static void verify(Map<String,ClassDef> all){
        field(all,CAP,"a","Lcom/ss/android/vesdk/VECameraSettings;");
        field(all,CAP,"b",SETTINGS);field(all,CAP,"c",SETTINGS);field(all,CAP,"d","Landroid/content/Context;");
        field(all,CAP,"o","Li/s/a/w/k;");field(all,CAP,"n","Lcom/ss/android/vesdk/ConcurrentList;");
        field(all,CAP,"p","Ljava/util/concurrent/atomic/AtomicBoolean;");field(all,CAP,"u","Z");
        Field singleton=field(all,SERVER,"INSTANCE",SERVER);
        require((singleton.getAccessFlags()&9)==9,"Camera server singleton must remain public/static");
        field(all,SERVER,"mCameraClient","Li/s/a/w/k;");field(all,SERVER,"mCameraSettings",SETTINGS);
        field(all,SERVER,"mCurrentCameraState","I");field(all,SERVER,"mCameraInstance","Li/s/a/w/a;");
        field(all,SERVER,"mHandler","Landroid/os/Handler;");field(all,SERVER,"mProviderManager","Li/s/a/w/l0/c;");
        for(String n:List.of("mIsCameraPendingClose","mIsCameraSwitchState","mHandlerDestroyed","mOnBackGround","mIsInitialized"))field(all,SERVER,n,"Z");
        for(String n:List.of("l","j","M"))field(all,SETTINGS,n,"I");field(all,SETTINGS,"u0","Z");
        field(all,"Li/s/a/w/g;","K","Landroid/hardware/camera2/CameraDevice;");
        field(all,"Li/s/a/w/g;","M","Li/s/a/w/h0/b;");field(all,"Li/s/a/w/g;","I","I");
        field(all,"Li/s/a/w/d;","H","Landroid/hardware/Camera;");
        field(all,"Li/s/a/w/h0/b;","a","Landroid/hardware/camera2/CameraCharacteristics;");
        field(all,"Li/s/a/w/h0/b;","g","Li/s/a/w/g;");field(all,"Li/s/a/w/h0/b;","h",SETTINGS);
        field(all,"Li/s/a/w/h0/b;","j","Landroid/hardware/camera2/CameraDevice;");
        field(all,"Li/s/a/w/h0/b;","d","Landroid/hardware/camera2/CameraCaptureSession;");
        for(String n:List.of("a","j","k"))field(all,"Li/s/a/w/l0/b;",n,"Li/s/a/w/l0/b$c;");
        for(String n:List.of("e","h"))field(all,"Li/s/a/w/l0/b;",n,"Z");
        field(all,"Li/s/a/w/l0/b;","d","Li/s/a/w/a;");
        field(all,"Li/s/a/w/l0/b;","b","Li/s/a/w/m$d;");
        for(String suffix:List.of("startPreview()I","newSurfaceTexture()V","stopPreview(Z)I","destroy()V",
                "switchCamera(Lcom/ss/android/vesdk/VECameraSettings$CAMERA_FACING_ID;Lcom/bytedance/bpea/basics/Cert;)I",
                "switchCamera(Lcom/ss/android/vesdk/VECameraSettings;Lcom/bytedance/bpea/basics/Cert;)I",
                "switchCameraMode(ILcom/ss/android/ttvecamera/TECameraSettings;)I",
                "close(Lcom/bytedance/bpea/basics/Cert;)I","close(ZLcom/bytedance/bpea/basics/Cert;)I","onBackGround()V"))method(all,CAP+"->"+suffix);
        method(all,"Lcom/ss/android/vesdk/VECameraSettings;->getCameraFacing()Lcom/ss/android/vesdk/VECameraSettings$CAMERA_FACING_ID;");
        String pipeline="Lcom/ss/android/vesdk/frame/TECapturePipeline;";
        for(String suffix:List.of("isPreview()Z","isValid()Z","getFormat()Li/s/a/w/m$d;","getSurfaceTexture()Landroid/graphics/SurfaceTexture;","getCaptureListener()Lcom/ss/android/vesdk/frame/TECapturePipeline$CaptureListener;"))method(all,pipeline+"->"+suffix);
        method(all,"Lcom/ss/android/vesdk/frame/TETextureCapturePipeline;->getSurface()Landroid/view/Surface;");
        method(all,"Lcom/ss/android/vesdk/frame/TERecorderCapturePipeline;->getRecorderSurface()Landroid/view/Surface;");
        method(all,"Li/s/a/w/l0/c;->h()Li/s/a/w/l0/b;");

        Method renew=method(all,CAP+"->newSurfaceTexture()V");int writes=0,constructs=0;
        for(Instruction i:renew.getImplementation().getInstructions()){
            if(i.getOpcode()==Opcode.IPUT_BOOLEAN&&ref(i).equals(CAP+"->u:Z"))writes++;
            if(i.getOpcode()==Opcode.NEW_INSTANCE)constructs++;
        }
        require(writes==1&&constructs==0,"Native renewal must remain a flag, not helper-created GL objects");
        Method valid=method(all,pipeline+"->isValid()Z");boolean surfaceCheck=false;
        for(Instruction i:valid.getImplementation().getInstructions())if(ref(i).contains("SurfaceTexture")||ref(i).contains("Landroid/view/Surface;"))surfaceCheck=true;
        require(!surfaceCheck,"Audited baseline lacks native surface-lifetime checks; revisit reproduction if that changes");

        int managerWriters=0,taskConstructors=0;
        for(ClassDef cls:all.values())for(Method m:cls.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions()){
            if(i.getOpcode()==Opcode.IPUT_OBJECT&&ref(i).equals(SERVER+"->mProviderManager:Li/s/a/w/l0/c;")){
                managerWriters++;require(m.toString().equals(SERVER+"->init(Z)V"),"Unexpected provider-manager writer "+m);
            }
            if(ref(i).equals("Li/s/a/w/q$k;-><init>(Li/s/a/w/q;)V")){
                taskConstructors++;require(m.toString().equals(SERVER+"->destroy()I"),"Unexpected deferred release constructor site "+m);
            }
        }
        require(managerWriters==1&&taskConstructors==1,"Provider ownership audit requires one init writer and destroy release site");
        Method init=method(all,SERVER+"->init(Z)V"),destroy=method(all,SERVER+"->destroy()I");
        require((init.getAccessFlags()&0x20000)!=0&&(destroy.getAccessFlags()&0x20000)!=0,"init/destroy must synchronize on the same server instance");
    }
    static int index(Method method,String target){
        int n=0;for(Instruction instruction:method.getImplementation().getInstructions()){if(ref(instruction).equals(target))return n;n++;}return -1;
    }
    static void retainedPreviewAbi(Map<String,ClassDef> all){
        Method clientStop=method(all,"Li/s/a/w/k;->v0(Z)I");
        require((clientStop.getAccessFlags()&9)==1,"Reflected native v0 must remain public instance method");
        require(index(clientStop,SERVER+"->stop(Li/s/a/w/k;Z)I")>=0,"Native client stop delegates to same server/client stop");
        Method stop=method(all,SERVER+"->stop(Li/s/a/w/k;Z)I");
        require(index(stop,"Landroid/os/Looper;->myLooper()Landroid/os/Looper;")>=0
                && index(stop,"Landroid/os/Handler;->getLooper()Landroid/os/Looper;")>=0,"Native stop keeps handler-thread execution gate");
        int set=index(stop,SERVER+"->updateCameraState(I)V"),halt=index(stop,"Li/s/a/w/a;->m()V");
        require(set>=0&&halt>set,"Native stop changes state before stopping preview");
        require(index(stop,"Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z")>=0,"Other native threads retain SDK serialization");
        for(String id:List.of(SERVER+"->stop(Li/s/a/w/k;Z)I","Li/s/a/w/g;->m()V","Li/s/a/w/d;->m()V","Li/s/a/w/g;->p0()I","Li/s/a/w/h0/b;->p()V")){
            Method method=method(all,id);
            for(Instruction insn:method.getImplementation().getInstructions()){
                String target=ref(insn);
                require(!target.equals("Landroid/graphics/SurfaceTexture;->release()V")
                        &&!target.equals("Landroid/view/Surface;->release()V")
                        &&!target.equals("Landroid/hardware/camera2/CameraDevice;->close()V")
                        &&!target.equals("Landroid/hardware/Camera;->release()V"),"Native preview stop must retain texture, surface and device: "+id);
            }
        }
        Method frame=method(all,"Li/s/a/w/l0/b;->m(Li/s/a/w/m;)V");
        int listener=index(frame,"Li/s/a/w/l0/b$c;->onFrameCaptured(Li/s/a/w/m;)V");
        require(listener>=0,"Native provider must deliver frame to actual listener");
        for(String helper:List.of("FrontPreview1931","FrontPreview1936")){
            int mark=index(frame,"Lcom/hiro/ulike/"+helper+";->pixel(Ljava/lang/Object;Ljava/lang/Object;)V");
            require(mark<0||mark>listener,"Frame observation must follow listener delivery");
        }
        ClassDef pending=all.get("Lcom/hiro/ulike/FrontPreview1936$Pending;");
        if(pending!=null){
            Method work=method(all,"Lcom/hiro/ulike/FrontPreview1936$Pending;->runNative()V");
            require(index(work,CAP+"->newSurfaceTexture()V")<0,"New watchdog does not replace GL textures");
        }
    }
    public static void main(String[] args)throws Exception{
        if(args.length<1||args.length>2)throw new IllegalArgumentException("ORIGINAL_APK [APPLIED_APK]");
        for(String arg:args){checks=0;var all=load(arg);int inventory=checks;checks=0;verify(all);retainedPreviewAbi(all);
            System.out.println("PASS front native ABI/lifecycle checks="+checks+" classes="+inventory+" apk="+new File(arg).getName());}
    }
}
