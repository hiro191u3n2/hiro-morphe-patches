import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Replace only the existing accepted-photo display call on the .38 baseline.
 * Native camera gestures, capture acceptance and save ownership remain exact. */
public final class FeedbackHooks1941 {
    public static final String FLASH="Li/o/a/b1/a/g/f0/f;->q()V";
    static final String OLD_FLASH="Li/g/a/a;->a(Landroid/view/View;)V";
    static final String NEW_FLASH="Lcom/hiro/ulike/ShutterFeedback1941;->show(Landroid/view/View;)Z";
    public static final Set<String> NATIVE=Set.of(FLASH), RUNTIME=Set.of();
    static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    static void require(boolean b,String why){MergePayloads.require(b,why);}
    static Method wrap(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
    public static Method repair(Method old){
        String id=MergePayloads.id(old);require(NATIVE.contains(id),"Reviewed accepted-photo feedback target");
        var body=new MutableMethodImplementation(old.getImplementation());int calls=0;
        for(int i=0;i<body.getInstructions().size();i++){
            Instruction op=body.getInstructions().get(i);
            if(ref(op).equals(OLD_FLASH)){
                require(body.getRegisterCount()==3&&op.getOpcode()==Opcode.INVOKE_VIRTUAL&&op instanceof FiveRegisterInstruction,"Pinned native flash call");
                FiveRegisterInstruction call=(FiveRegisterInstruction)op;
                require(call.getRegisterCount()==2&&call.getRegisterC()==0&&call.getRegisterD()==1,"Pinned flash view v1");
                require(body.getInstructions().get(i+1).getOpcode()==Opcode.RETURN_VOID,"Feedback call has no further receiver use");
                var done=body.newLabelForIndex(i+1);
                body.addInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,1,0,0,0,0,new ImmutableMethodReference("Lcom/hiro/ulike/ShutterFeedback1941;","show",List.of("Landroid/view/View;"),"Z")));
                body.addInstruction(i+1,new BuilderInstruction11x(Opcode.MOVE_RESULT,2));
                body.addInstruction(i+2,new BuilderInstruction21t(Opcode.IF_NEZ,2,done));
                // The native animation still runs if the drawable cannot be used.
                i+=3;calls++;
            }
        }
        require(calls==1,"Exactly one reviewed photo display call changed "+id);
        return wrap(old,body);
    }
    public static void verify(Method before,Method after){
        require(MergePayloads.hash(repair(before)).equals(MergePayloads.hash(after)),"Exact photo-feedback transformation");
        var original=new MutableMethodImplementation(before.getImplementation());var reverted=new MutableMethodImplementation(after.getImplementation());int count=0;
        for(int i=0;i<original.getInstructions().size();i++){
            Instruction op=original.getInstructions().get(i);
            if(ref(op).equals(OLD_FLASH)){
                require(ref(reverted.getInstructions().get(i)).equals(NEW_FLASH),"Accepted-photo animation replaced");
                require(reverted.getInstructions().get(i+1).getOpcode()==Opcode.MOVE_RESULT&&reverted.getInstructions().get(i+2).getOpcode()==Opcode.IF_NEZ,"Conditional native fallback after result");
                reverted.removeInstruction(i+2);reverted.removeInstruction(i+1);reverted.removeInstruction(i);count++;
            }
        }
        require(count==1&&MergePayloads.hash(wrap(before,reverted)).equals(MergePayloads.hash(before)),"Inverse preserves all other native feedback instructions");
    }
}
