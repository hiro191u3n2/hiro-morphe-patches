import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** One black-bar action removal and one replacement of the existing accepted
 * capture display call. No capture, quality, routing or readiness branch changes. */
public final class GestureFeedbackHooks1940 {
    public static final String FLASH="Li/o/a/b1/a/g/f0/f;->q()V";
    public static final String BLACK="Lcom/hiro/ulike/BlackTap1925;->doubleTap(Lcom/bytedance/corecamera/ui/view/GestureBgLayout;Landroid/view/MotionEvent;)Z";
    static final String SWITCH="Li/o/a/b1/a/g/y;->switchCamera()V";
    static final String OLD_FLASH="Li/g/a/a;->a(Landroid/view/View;)V";
    static final String NEW_FLASH="Lcom/hiro/ulike/ShutterFeedback1940;->show(Landroid/view/View;)Z";
    public static final Set<String> NATIVE=Set.of(FLASH), RUNTIME=Set.of(BLACK);
    static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    static void require(boolean b,String why){MergePayloads.require(b,why);}
    static Method wrap(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
    public static Method repair(Method old){
        String id=MergePayloads.id(old);require(NATIVE.contains(id)||RUNTIME.contains(id),"Reviewed feedback target");
        var body=new MutableMethodImplementation(old.getImplementation());int calls=0;
        for(int i=0;i<body.getInstructions().size();i++){
            Instruction op=body.getInstructions().get(i);
            if(id.equals(BLACK)&&ref(op).equals(SWITCH)){
                require(op.getOpcode()==Opcode.INVOKE_VIRTUAL&&op instanceof FiveRegisterInstruction&&((FiveRegisterInstruction)op).getRegisterCount()==1,"Pinned black switch call");
                body.replaceInstruction(i,new BuilderInstruction10x(Opcode.NOP));calls++;
            }else if(id.equals(FLASH)&&ref(op).equals(OLD_FLASH)){
                require(body.getRegisterCount()==3&&op.getOpcode()==Opcode.INVOKE_VIRTUAL&&op instanceof FiveRegisterInstruction,"Pinned native flash call");
                FiveRegisterInstruction call=(FiveRegisterInstruction)op;
                require(call.getRegisterCount()==2&&call.getRegisterC()==0&&call.getRegisterD()==1,"Pinned flash view v1");
                require(body.getInstructions().get(i+1).getOpcode()==Opcode.RETURN_VOID,"Feedback call has no further receiver use");
                var done=body.newLabelForIndex(i+1);
                body.addInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,1,0,0,0,0,new ImmutableMethodReference("Lcom/hiro/ulike/ShutterFeedback1940;","show",List.of("Landroid/view/View;"),"Z")));
                body.addInstruction(i+1,new BuilderInstruction11x(Opcode.MOVE_RESULT,2));
                body.addInstruction(i+2,new BuilderInstruction21t(Opcode.IF_NEZ,2,done));
                // Keep the original native a(view) unchanged as a failure fallback.
                i+=3;calls++;
            }
        }
        require(calls==1,"Exactly one reviewed display/action call changed "+id);
        return wrap(old,body);
    }
    public static void verify(Method before,Method after){
        require(MergePayloads.hash(repair(before)).equals(MergePayloads.hash(after)),"Exact gesture-feedback transformation");
        String id=MergePayloads.id(before);var original=new MutableMethodImplementation(before.getImplementation());var reverted=new MutableMethodImplementation(after.getImplementation());
        int count=0;
        // NOP is shorter than the removed invocation; labels are recovered by
        // MutableMethodImplementation when the original opcode is restored.
        for(int i=0;i<original.getInstructions().size();i++){
            Instruction op=original.getInstructions().get(i);
            if(id.equals(BLACK)&&ref(op).equals(SWITCH)){
                require(reverted.getInstructions().get(i).getOpcode()==Opcode.NOP,"Switch removed, gesture still consumed");
                FiveRegisterInstruction call=(FiveRegisterInstruction)op;
                reverted.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,1,call.getRegisterC(),0,0,0,0,new ImmutableMethodReference("Li/o/a/b1/a/g/y;","switchCamera",List.of(),"V")));count++;
            }else if(id.equals(FLASH)&&ref(op).equals(OLD_FLASH)){
                require(ref(reverted.getInstructions().get(i)).equals(NEW_FLASH),"Native white animation replaced");
                require(reverted.getInstructions().get(i+1).getOpcode()==Opcode.MOVE_RESULT&&reverted.getInstructions().get(i+2).getOpcode()==Opcode.IF_NEZ,"Conditional native fallback after result");
                reverted.removeInstruction(i+2);reverted.removeInstruction(i+1);reverted.removeInstruction(i);count++;
            }
        }
        require(count==1&&MergePayloads.hash(wrap(before,reverted)).equals(MergePayloads.hash(before)),"Inverse preserves every other gesture and flash instruction");
    }
}
