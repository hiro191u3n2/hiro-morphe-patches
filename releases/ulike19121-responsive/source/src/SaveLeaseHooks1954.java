import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c;
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc;
import com.android.tools.smali.dexlib2.iface.reference.Reference;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc;

/** Four pinned final-save disposal calls transfer frozen proof disposal to the
 * last owner. Registers, branches, try ranges and all codec/publish calls retain
 * their exact .53 instruction shape; the inverse proves that scope. */
public final class SaveLeaseHooks1954 {
    static final String BITMAP="Landroid/graphics/Bitmap;";
    static final String RECYCLE=BITMAP+"->recycle()V";
    static final String GATE="Lcom/hiro/ulike/QualityPipeline1932;->recycle1954("+BITMAP+")V";
    public static final Map<String,String> PINS=Map.of(
        "Lcom/hiro/ulike/SaveFd186;->recycle("+BITMAP+BITMAP+")V",
        "8f7b3d03416c1ba35a749377d414805b53e84ab45c0d304ea9862b0d2f282a83",
        "Lcom/hiro/ulike/SaveQuality2;->saveFinalBefore1947("+BITMAP+"Ljava/io/File;Landroid/graphics/Bitmap$CompressFormat;I)Z",
        "3e9028bdb526d223e68c3d3d6c66e1139b381e4329e88dd5c598ba5a1caebd84");
    public static final Map<String,String> TARGETS=PINS;
    static final ImmutableMethodReference CALL=new ImmutableMethodReference("Lcom/hiro/ulike/QualityPipeline1932;","recycle1954",List.of(BITMAP),"V");
    static final ImmutableMethodReference OLD=new ImmutableMethodReference(BITMAP,"recycle",List.of(),"V");
    static void need(boolean value,String label){if(!value)throw new IllegalStateException(label);}
    static String ref(Instruction op){return op instanceof ReferenceInstruction?((ReferenceInstruction)op).getReference().toString():"";}
    static Method wrap(Method method,MethodImplementation body){return new ImmutableMethod(method.getDefiningClass(),method.getName(),method.getParameters(),method.getReturnType(),method.getAccessFlags(),method.getAnnotations(),method.getHiddenApiRestrictions(),body);}
    public static boolean isTarget(Method method){return TARGETS.containsKey(MergePayloads.id(method));}
    static Instruction replacement(Instruction op,boolean inverse){
        Reference target=inverse?OLD:CALL;
        if(op instanceof Instruction35c){
            Instruction35c r=(Instruction35c)op;need(r.getRegisterCount()==1,"single Bitmap disposal register");
            return new BuilderInstruction35c(inverse?Opcode.INVOKE_VIRTUAL:Opcode.INVOKE_STATIC,1,r.getRegisterC(),0,0,0,0,target);
        }
        need(op instanceof Instruction3rc,"reviewed Bitmap disposal invoke format");
        Instruction3rc r=(Instruction3rc)op;need(r.getRegisterCount()==1,"single range Bitmap disposal register");
        return new BuilderInstruction3rc(inverse?Opcode.INVOKE_VIRTUAL_RANGE:Opcode.INVOKE_STATIC_RANGE,r.getStartRegister(),1,target);
    }
    public static Method repair(Method method){
        String id=MergePayloads.id(method);need(isTarget(method),"reviewed final-save disposal method");
        need(PINS.get(id).equals(MergePayloads.hash(method)),"published .53 final-save disposal hash "+id);
        MutableMethodImplementation body=new MutableMethodImplementation(method.getImplementation());int count=0;
        for(int at=0;at<body.getInstructions().size();at++){
            Instruction op=body.getInstructions().get(at);if(!ref(op).equals(RECYCLE))continue;
            need(op.getOpcode()==Opcode.INVOKE_VIRTUAL||op.getOpcode()==Opcode.INVOKE_VIRTUAL_RANGE,"original Bitmap.recycle invocation");
            body.replaceInstruction(at,(com.android.tools.smali.dexlib2.builder.BuilderInstruction)replacement(op,false));count++;
        }
        need(count==(id.contains("SaveFd186;")?1:3),"exact four final-save disposal sites");return wrap(method,body);
    }
    public static void verify(Method before,Method after){
        need(isTarget(before)&&MergePayloads.id(before).equals(MergePayloads.id(after)),"final-save disposal identity");
        need(MergePayloads.hash(repair(before)).equals(MergePayloads.hash(after)),"only reviewed disposal calls replaced");
        MutableMethodImplementation inverse=new MutableMethodImplementation(after.getImplementation());int count=0;
        for(int at=0;at<inverse.getInstructions().size();at++){
            Instruction op=inverse.getInstructions().get(at);if(!ref(op).equals(GATE))continue;
            need(op.getOpcode()==Opcode.INVOKE_STATIC||op.getOpcode()==Opcode.INVOKE_STATIC_RANGE,"lease disposal invocation");
            inverse.replaceInstruction(at,(com.android.tools.smali.dexlib2.builder.BuilderInstruction)replacement(op,true));count++;
        }
        need(count==(MergePayloads.id(before).contains("SaveFd186;")?1:3),"inverse four exact final-save disposal calls");
        need(MergePayloads.hash(before).equals(MergePayloads.hash(wrap(after,inverse))),"lease hook inverse preserves every original instruction and try range");
    }
}
