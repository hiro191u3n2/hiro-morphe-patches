import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Pin every final-detail route, including normalization fallback, to its photo's
 * selected options. No pixel math or branch in the processing code is changed. */
public final class SaveResponseHooks1939 {
    static final String DETAIL="Lcom/hiro/ulike/PhotoDetail;->applyDetail(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;)Landroid/graphics/Bitmap;";
    static final String FIELD="Lcom/hiro/ulike/PhotoDetail;->settings:Lcom/hiro/ulike/PhotoDetail$Settings;";
    static final String SETTINGS="Lcom/hiro/ulike/PhotoDetail$Settings;";
    static final String HOOK="Lcom/hiro/ulike/AsyncSave1935;->settings("+SETTINGS+")"+SETTINGS;
    public static final Set<String> RUNTIME=Set.of(DETAIL);
    static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    static void require(boolean b,String why){if(!b)throw new IllegalStateException(why);}
    static Method wrap(Method old,MethodImplementation body){return new ImmutableMethod(old.getDefiningClass(),old.getName(),old.getParameters(),old.getReturnType(),old.getAccessFlags(),old.getAnnotations(),old.getHiddenApiRestrictions(),body);}
    public static Method repair(Method before){
        require(RUNTIME.contains(MergePayloads.id(before)),"Unreviewed save response hook");
        MutableMethodImplementation body=new MutableMethodImplementation(before.getImplementation());
        require(body.getRegisterCount()==4,"PhotoDetail apply ABI");
        int reads=0,consumers=0;
        for(int i=0;i<body.getInstructions().size();i++){
            Instruction op=body.getInstructions().get(i);
            if(ref(op).equals(FIELD)){
                require(op.getOpcode()==Opcode.SGET_OBJECT,"Snapshot settings field read");
                int r=((OneRegisterInstruction)op).getRegisterA();
                require(r==0,"Snapshot settings register");
                body.addInstruction(++i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,r,0,0,0,0,new ImmutableMethodReference("Lcom/hiro/ulike/AsyncSave1935;","settings",List.of(SETTINGS),SETTINGS)));
                body.addInstruction(++i,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,r));reads++;
            }
            if(ref(op).equals("Lcom/hiro/ulike/QualityPipeline1932;->applyDetail(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;"+SETTINGS+")Landroid/graphics/Bitmap;")){
                require(op instanceof FiveRegisterInstruction&&((FiveRegisterInstruction)op).getRegisterE()==0,"Exact detail argument register");consumers++;
            }
        }
        require(reads==1&&consumers==1,"Exactly one per-photo detail snapshot and consumer");
        return wrap(before,body);
    }
    public static void verify(Method before,Method after){
        require(MergePayloads.hash(repair(before)).equals(MergePayloads.hash(after)),"Exact save response transform");
        MutableMethodImplementation body=new MutableMethodImplementation(after.getImplementation());
        int hooks=0;
        for(int i=body.getInstructions().size()-1;i>=0;i--)if(ref(body.getInstructions().get(i)).equals(HOOK)){
            require(body.getInstructions().get(i+1).getOpcode()==Opcode.MOVE_RESULT_OBJECT,"Snapshot result opcode");
            body.removeInstruction(i+1);body.removeInstruction(i);hooks++;
        }
        require(hooks==1&&MergePayloads.hash(wrap(before,body)).equals(MergePayloads.hash(before)),"Full inverse leaves all original processing instructions unchanged");
    }
}
