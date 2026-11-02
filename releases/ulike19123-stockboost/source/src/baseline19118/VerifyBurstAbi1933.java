import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;

/** Check every native reflection endpoint used by the burst bridge against the APK. */
public final class VerifyBurstAbi1933 {
    static final String OWNER="Li/s/a/w/d0/a;", FRAME="Li/s/a/w/m;", PIXEL="Li/s/a/w/m$d;";
    static final String CALLBACK="Lcom/ss/android/ttvecamera/TECameraSettings$m;";
    static void need(boolean value,String message){VerifyFrontAbi1931.require(value,message);}
    static Field field(Map<String,ClassDef> all,String owner,String name,String type){return VerifyFrontAbi1931.field(all,owner,name,type);}
    static Method method(Map<String,ClassDef> all,String id){return VerifyFrontAbi1931.method(all,id);}
    static void publicMethod(Map<String,ClassDef> all,String id){need((method(all,id).getAccessFlags()&1)!=0,"Required public reflection endpoint "+id);}
    static boolean subtype(Map<String,ClassDef> all,String actual,String expected){
        var seen=new HashSet<String>();while(actual!=null&&seen.add(actual)){
            if(actual.equals(expected))return true;ClassDef c=all.get(actual);if(c==null)return false;
            if(c.getInterfaces().contains(expected))return true;actual=c.getSuperclass();
        }return false;
    }
    static void verify(Map<String,ClassDef> all){
        field(all,OWNER,"j","Landroid/hardware/camera2/CameraDevice;");
        field(all,OWNER,"d","Landroid/hardware/camera2/CameraCaptureSession;");
        field(all,OWNER,"a","Landroid/hardware/camera2/CameraCharacteristics;");
        field(all,OWNER,"k","Landroid/os/Handler;");field(all,OWNER,"x0",CALLBACK);
        field(all,OWNER,"g","Li/s/a/w/g;");need(subtype(all,"Li/s/a/w/g;","Li/s/a/w/i;"),"Camera info must satisfy native picture callback");
        field(all,OWNER,"m0","I");field(all,OWNER,"y0","Lcom/ss/android/ttvecamera/TECameraSettings$d;");
        field(all,OWNER,"e0","Landroid/media/ImageReader;");field(all,"Li/s/a/w/d0/a$g;","a",OWNER);
        publicMethod(all,"Li/s/a/w/d0/a$g;->onImageAvailable(Landroid/media/ImageReader;)V");
        publicMethod(all,FRAME+"-><init>([B"+PIXEL+"III)V");
        need((field(all,PIXEL,"PIXEL_FORMAT_NV21",PIXEL).getAccessFlags()&9)==9,"NV21 enum is public static");
        String metadata="Li/s/a/w/m$e;";
        publicMethod(all,metadata+"-><init>()V");publicMethod(all,FRAME+"->u("+metadata+")V");
        need((field(all,metadata,"d","Landroid/hardware/camera2/TotalCaptureResult;").getAccessFlags()&1)!=0,"Native frame capture result must be public");
        need((field(all,metadata,"c","J").getAccessFlags()&1)!=0,"Native frame arrival time must be public");
        for(String cb:List.of("Lcom/ss/android/vesdk/TECameraVideoRecorder$60;","Li/s/a/w/q$g$a;")){
            need(subtype(all,cb,CALLBACK),"Only verified original callback classes are accepted");
            publicMethod(all,cb+"->onPictureTaken("+FRAME+"Li/s/a/w/i;)V");
            publicMethod(all,cb+"->onTakenFail(Ljava/lang/Exception;)V");
            publicMethod(all,cb+"->onTakenFail(Ljava/lang/Exception;I)V");
        }
        field(all,"Li/s/a/w/q$g$a;","a","Li/s/a/w/q$g;");field(all,"Li/s/a/w/q$g;","c",CALLBACK);
        Method nativeFailure=method(all,"Lcom/ss/android/vesdk/TECameraVideoRecorder$60;->onTakenFail(Ljava/lang/Exception;)V");
        boolean error=false;for(var instruction:nativeFailure.getImplementation().getInstructions())
            error|=VerifyFrontAbi1931.ref(instruction).equals("Lcom/ss/android/vesdk/VERecorder$IBitmapCaptureCallback;->onImageError(II)V");
        need(error,"The chosen one-argument failure endpoint must actually notify the image callback");
        Method wrapperFailure=method(all,"Li/s/a/w/q$g$a;->onTakenFail(Ljava/lang/Exception;)V");
        boolean condition=false,delegate=false;
        for(var instruction:wrapperFailure.getImplementation().getInstructions()){
            String reference=VerifyFrontAbi1931.ref(instruction);
            condition|=reference.equals("Landroid/os/ConditionVariable;->open()V");
            delegate|=reference.equals(CALLBACK+"->onTakenFail(Ljava/lang/Exception;)V");
        }
        need(condition&&delegate,"Wrapper failure must release camera wait and delegate exactly to the functional endpoint");
        Method unusedDefault=method(all,"Li/s/a/w/r;->b("+CALLBACK+"Ljava/lang/Exception;I)V");
        var noOp=new ArrayList<com.android.tools.smali.dexlib2.iface.instruction.Instruction>();
        unusedDefault.getImplementation().getInstructions().forEach(noOp::add);
        need(noOp.size()==1&&noOp.get(0).getOpcode()==com.android.tools.smali.dexlib2.Opcode.RETURN_VOID,
            "Review native failure dispatch if the two-argument default is no longer a no-op");
        publicMethod(all,OWNER+"->Q0(Landroid/media/Image;Landroid/hardware/camera2/TotalCaptureResult;)V");
        publicMethod(all,OWNER+"->u0("+CALLBACK+"I)V");
        if(all.containsKey("Lcom/hiro/ulike/BurstCapture1933;")){
            String capture="Lcom/hiro/ulike/CaptureYuv;",state="Lcom/hiro/ulike/CaptureYuv$State;";
            field(all,state,"yuv","Landroid/media/ImageReader;");
            Method receive=method(all,capture+"->receive(Ljava/lang/Object;"+state+"Landroid/media/ImageReader;)V");
            need((receive.getAccessFlags()&8)!=0,"Replay entry must be static");
            publicMethod(all,"Lcom/hiro/ulike/ShotContext1932;->receivedValues1933(Ljava/lang/Object;Ljava/lang/Object;JIILandroid/hardware/camera2/CaptureResult;)Z");
        }
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("ORIGINAL_OR_APPLIED_APK");
        var all=VerifyFrontAbi1931.load(args[0]);VerifyFrontAbi1931.checks=0;verify(all);
        System.out.println("PASS burst native reflection ABI: checks="+VerifyFrontAbi1931.checks+", apk="+new File(args[0]).getName()+". No device execution.");
    }
}
