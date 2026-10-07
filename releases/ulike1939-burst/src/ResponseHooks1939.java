import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Two narrow interval substitutions; all actual native shutter instructions remain. */
public final class ResponseHooks1939 {
    public static final String BUTTON="Lcom/light/beauty/mc/preview/shutter/module/ShutterButton;";
    public static final String TOUCH=BUTTON+"->onTouchEvent(Landroid/view/MotionEvent;)Z";
    public static final String DOWN=BUTTON+"->e()V";
    public static final String HELPER="Lcom/hiro/ulike/ShutterResponse1939;";
    public static final Set<String> NATIVE=Set.of(TOUCH,DOWN);
    public static final Set<String> RUNTIME=Set.of();
    private static void require(boolean ok,String why){MergePayloads.require(ok,why);}
    private static Method wrap(Method m,MethodImplementation body){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),body);}
    public static Method repair(Method old){
        String id=MergePayloads.id(old);require(NATIVE.contains(id),"Unknown response hook "+id);
        boolean touch=id.equals(TOUCH);int registers=touch?6:7,receiver=touch?4:6,target=touch?2:3;
        long delay=touch?200:500;String name=touch?"touchWindow":"photoWindow";
        require(old.getImplementation()!=null&&old.getImplementation().getRegisterCount()==registers,"Pinned shutter register ABI");
        MutableMethodImplementation body=new MutableMethodImplementation(old.getImplementation());
        int count=0,holds=0;
        for(int i=0;i<body.getInstructions().size();i++){
            Instruction op=body.getInstructions().get(i);
            if(op.getOpcode()==Opcode.CONST_WIDE_16 && ((WideLiteralInstruction)op).getWideLiteral()==delay){
                require(((OneRegisterInstruction)op).getRegisterA()==target,"Pinned interval wide register");
                body.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,receiver,0,0,0,0,new ImmutableMethodReference(HELPER,name,List.of("Ljava/lang/Object;"),"J")));
                body.addInstruction(++i,new BuilderInstruction11x(Opcode.MOVE_RESULT_WIDE,target));count++;
            }
            if(!touch&&op instanceof ReferenceInstruction&&((ReferenceInstruction)op).getReference().toString().equals("Landroid/view/View;->postDelayed(Ljava/lang/Runnable;J)Z")){
                require(op.getOpcode()==Opcode.INVOKE_VIRTUAL&&op instanceof FiveRegisterInstruction,"Original long-video scheduling ABI");
                FiveRegisterInstruction call=(FiveRegisterInstruction)op;
                require(call.getRegisterCount()==4&&call.getRegisterC()==6&&call.getRegisterD()==0&&call.getRegisterE()==1&&call.getRegisterF()==2,"Original 300ms hold arguments");
                body.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,4,6,0,1,2,0,new ImmutableMethodReference(HELPER,"postHold",List.of("Landroid/view/View;","Ljava/lang/Runnable;","J"),"Z")));holds++;
            }
        }
        require(count==1,"Exactly one original manual shutter delay "+id);
        require(holds==(touch?0:1),"Exactly one existing long-video schedule ownership guard");
        return wrap(old,body);
    }
    public static void verify(Method before,Method after){
        require(MergePayloads.id(before).equals(MergePayloads.id(after)),"Response method identity");
        require(MergePayloads.hash(repair(before)).equals(MergePayloads.hash(after)),"Exact response transformation");
        boolean touch=MergePayloads.id(before).equals(TOUCH);
        MutableMethodImplementation restored=new MutableMethodImplementation(after.getImplementation());int count=0;
        for(int i=restored.getInstructions().size()-1;i>=0;i--){
            Instruction op=restored.getInstructions().get(i);
            if(op instanceof ReferenceInstruction&&((ReferenceInstruction)op).getReference().toString().startsWith(HELPER)){
                if(((ReferenceInstruction)op).getReference().toString().contains("->postHold(")){
                    restored.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,4,6,0,1,2,0,new ImmutableMethodReference("Landroid/view/View;","postDelayed",List.of("Ljava/lang/Runnable;","J"),"Z")));
                }else{
                    restored.removeInstruction(i+1);restored.replaceInstruction(i,new BuilderInstruction21s(Opcode.CONST_WIDE_16,touch?2:3,touch?200:500));
                }count++;
            }
        }
        require(count==(touch?1:2),"Exactly reviewed reversible response hooks");
        require(MergePayloads.hash(wrap(before,restored)).equals(MergePayloads.hash(before)),"All original shutter branches/registers/permissions/capture calls preserved");
    }
}
