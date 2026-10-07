import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Same reviewed native hook locations as 1.9.35; only the helper owner changes. */
public final class FrontHooks1936 {
    private static final String CAPTURE="Lcom/ss/android/vesdk/VECameraCapture;->";
    private static final String OLD="Lcom/hiro/ulike/FrontPreview1931;", NEW="Lcom/hiro/ulike/FrontPreview1936;";
    public static final Set<String> NATIVE=Set.of(
        CAPTURE+"addCapturePipelines(Lcom/ss/android/vesdk/ConcurrentList;)V",
        CAPTURE+"destroy()V",CAPTURE+"onCaptureStarted(II)V",
        CAPTURE+"start(Lcom/ss/android/vesdk/ConcurrentList;)I",CAPTURE+"startPreview()I",
        CAPTURE+"stopPreview(Z)I",
        CAPTURE+"switchCamera(Lcom/ss/android/vesdk/VECameraSettings$CAMERA_FACING_ID;Lcom/bytedance/bpea/basics/Cert;)I",
        CAPTURE+"switchCamera(Lcom/ss/android/vesdk/VECameraSettings;Lcom/bytedance/bpea/basics/Cert;)I",
        CAPTURE+"switchCameraMode(ILcom/ss/android/ttvecamera/TECameraSettings;)I",
        "Li/s/a/w/l0/b;->m(Li/s/a/w/m;)V");
    public static final Set<String> RUNTIME=Set.of(
        "Lcom/hiro/ulike/OpticalZoom;->background(Ljava/lang/Object;)V",
        "Lcom/hiro/ulike/OpticalZoom;->closing(Ljava/lang/Object;)V",
        "Lcom/hiro/ulike/OpticalZoom;->prepared(Ljava/lang/Object;I)V");
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private static Method wrap(Method method,MethodImplementation body){return new ImmutableMethod(method.getDefiningClass(),method.getName(),method.getParameters(),method.getReturnType(),method.getAccessFlags(),method.getAnnotations(),method.getHiddenApiRestrictions(),body);}
    private static Method replace(Method old,String from,String to){
        require(NATIVE.contains(MergePayloads.id(old))||RUNTIME.contains(MergePayloads.id(old)),"Unreviewed front hook "+MergePayloads.id(old));
        MutableMethodImplementation body=new MutableMethodImplementation(old.getImplementation());
        int changed=0;
        for(int i=0;i<body.getInstructions().size();i++){
            Instruction instruction=body.getInstructions().get(i);
            if(!(instruction instanceof ReferenceInstruction))continue;
            Reference ref=((ReferenceInstruction)instruction).getReference();
            if(!(ref instanceof MethodReference))continue;
            MethodReference method=(MethodReference)ref;
            if(!method.getDefiningClass().equals(from))continue;
            ImmutableMethodReference target=new ImmutableMethodReference(to,method.getName(),method.getParameterTypes(),method.getReturnType());
            if(instruction.getOpcode()==Opcode.INVOKE_STATIC){
                FiveRegisterInstruction r=(FiveRegisterInstruction)instruction;
                body.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,r.getRegisterCount(),r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG(),target));
            }else{
                require(instruction.getOpcode()==Opcode.INVOKE_STATIC_RANGE,"Unexpected front call opcode");
                RegisterRangeInstruction r=(RegisterRangeInstruction)instruction;
                body.replaceInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,r.getStartRegister(),r.getRegisterCount(),target));
            }
            changed++;
        }
        require(changed==1,"Exactly one existing front call expected: "+MergePayloads.id(old)+" got "+changed);
        return wrap(old,body);
    }
    public static Method repair(Method old){return replace(old,OLD,NEW);}
    public static void verify(Method before,Method after){
        require(MergePayloads.id(before).equals(MergePayloads.id(after)),"Front method identity");
        require(MergePayloads.hash(repair(before)).equals(MergePayloads.hash(after)),"Front hook exact reviewed transform");
        require(MergePayloads.hash(replace(after,NEW,OLD)).equals(MergePayloads.hash(before)),"Front hook preserves every original instruction/register/branch/handler");
    }
}
