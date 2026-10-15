import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import java.util.*;

/** Verify the original APK contract used by owned retained-session recovery. */
public final class VerifyCameraAbi1971 {
    static int checks;
    static void ck(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void verify(Map<String,ClassDef> all){
        String cam="Li/s/a/w/g;",mode="Li/s/a/w/h0/b;",settings="Lcom/ss/android/ttvecamera/TECameraSettings;";
        VerifyFrontAbi1938.field(all,cam,"I","I");
        VerifyFrontAbi1938.field(all,cam,"K","Landroid/hardware/camera2/CameraDevice;");
        VerifyFrontAbi1938.field(all,cam,"M",mode);
        VerifyFrontAbi1938.field(all,cam,"c","Z");
        VerifyFrontAbi1938.field(all,mode,"d","Landroid/hardware/camera2/CameraCaptureSession;");
        VerifyFrontAbi1938.field(all,mode,"c","Landroid/hardware/camera2/CaptureRequest$Builder;");
        VerifyFrontAbi1938.field(all,mode,"g",cam);
        VerifyFrontAbi1938.field(all,mode,"h",settings);
        VerifyFrontAbi1938.field(all,mode,"j","Landroid/hardware/camera2/CameraDevice;");
        VerifyFrontAbi1938.field(all,settings,"s","Z");
        VerifyFrontAbi1938.field(all,settings,"u0","Z");
        Method stop=VerifyFrontAbi1938.method(all,cam+"->p0()I");
        ck((stop.getAccessFlags()&9)==1,"Native session cleanup remains public instance int");
        ck(VerifyFrontAbi1938.index(stop,mode+"->p()V")>=0,"Wrapper cleanup delegates to native preview-mode teardown");
        Method close=VerifyFrontAbi1938.method(all,mode+"->p()V");
        ck(VerifyFrontAbi1938.index(close,"Landroid/hardware/camera2/CameraCaptureSession;->close()V")>=0,"Mode teardown releases native session according to its settings");
        boolean clears=false;
        for(Instruction i:close.getImplementation().getInstructions())if(VerifyFrontAbi1938.ref(i).equals(mode+"->d:Landroid/hardware/camera2/CameraCaptureSession;")&&i.getOpcode().name().startsWith("IPUT"))clears=true;
        ck(clears,"Mode teardown drops retained session reference");
        for(Method m:List.of(stop,close))for(Instruction i:m.getImplementation().getInstructions()){
            String ref=VerifyFrontAbi1938.ref(i);
            ck(!ref.equals("Landroid/hardware/camera2/CameraDevice;->close()V")&&!ref.equals("Landroid/graphics/SurfaceTexture;->release()V")&&!ref.equals("Landroid/view/Surface;->release()V"),"Cleanup retains device and input surfaces");
            if(i.getOpcode().name().startsWith("IPUT"))ck(!ref.equals(cam+"->K:Landroid/hardware/camera2/CameraDevice;")&&!ref.equals(cam+"->M:"+mode)&&!ref.equals(mode+"->h:"+settings)&&!ref.equals(mode+"->j:Landroid/hardware/camera2/CameraDevice;"),"Cleanup keeps lifetime owner fields");
        }
    }
    public static void main(String[] args)throws Exception {
        if(args.length==0)throw new IllegalArgumentException("ORIGINAL_APK [PATCHED_APK]");
        for(String path:args)verify(VerifyFrontAbi1938.load(path));
        System.out.println("PASS retained camera ABI1971 assertions="+checks);
    }
}
