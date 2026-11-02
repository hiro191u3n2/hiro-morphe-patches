import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Only delayed UI callback entries change; all original geometry/camera instructions survive. */
public final class LayoutHooks1937 {
    public static final String SHADE="Lcom/bytedance/corecamera/ui/view/CameraShadeView;";
    public static final String CAMERA="Li/o/a/b1/a/d/b;->h(Li/o/a/b1/a/d/b;IIZ)V";
    public static final String PREVIEW="Li/o/a/m/j/f;->l(Li/o/a/m/j/f;IZ)V";
    public static final String SETTLED="Lcom/bytedance/corecamera/ui/view/CameraShadeView$h;->b("+SHADE+")V";
    public static final String POST_CAMERA="Li/o/a/b1/a/d/b;->g(IIZ)V";
    public static final String POST_PREVIEW="Li/o/a/m/j/f;->k(IZ)V";
    public static final String POST_SETTLED="Lcom/bytedance/corecamera/ui/view/CameraShadeView$h;->onAnimationEnd(Landroid/animation/Animator;)V";
    public static final String LISTENER="Lcom/bytedance/corecamera/ui/view/CameraShadeView$d;->a(Lcom/bytedance/corecamera/ui/view/CameraShadeView$c;)V";
    public static final Set<String> NATIVE=Set.of(CAMERA,PREVIEW,SETTLED,POST_CAMERA,POST_PREVIEW,POST_SETTLED,LISTENER), RUNTIME=Set.of();
    private static boolean post(String id){return id.equals(POST_CAMERA)||id.equals(POST_PREVIEW)||id.equals(POST_SETTLED);}
    private static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    private static final String H="Lcom/hiro/ulike/LayoutLifecycle1937;";
    private static void require(boolean b,String message){if(!b)throw new IllegalStateException(message);}
    private static Method wrap(Method m,MethodImplementation b){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),b);}
    public static Method repair(Method old){
        String id=MergePayloads.id(old);require(NATIVE.contains(id),"Unreviewed layout lifecycle hook "+id);
        MutableMethodImplementation body=new MutableMethodImplementation(old.getImplementation());
        require(body.getTryBlocks().isEmpty(),"Delayed layout callback has unexpected handlers");
        if(id.equals(LISTENER)) {
            require(body.getRegisterCount()==3,"Shade listener registration register ABI");
            int changes=0;
            for(int i=0;i<body.getInstructions().size();i++) {
                Instruction op=body.getInstructions().get(i);
                if(!ref(op).equals("Ljava/util/List;->add(Ljava/lang/Object;)Z"))continue;
                require(op.getOpcode()==Opcode.INVOKE_INTERFACE,"Native listener list mutation opcode");
                FiveRegisterInstruction r=(FiveRegisterInstruction)op;
                require(r.getRegisterCount()==2&&r.getRegisterD()==2,"Native listener argument ABI");
                body.addInstruction(++i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,2,1,
                    new ImmutableMethodReference("Lcom/hiro/ulike/PreviewLayout1922;","listenerAdded",List.of("Ljava/lang/Object;"),"V")));
                changes++;
            }
            require(changes==1,"One successful native listener registration");return wrap(old,body);
        }
        if(post(id)) {
            int changes=0;
            for(int i=0;i<body.getInstructions().size();i++) {
                Instruction op=body.getInstructions().get(i);
                if(!ref(op).equals("Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z"))continue;
                require(op.getOpcode()==Opcode.INVOKE_VIRTUAL,"Pinned delayed call opcode");
                FiveRegisterInstruction r=(FiveRegisterInstruction)op;
                require(r.getRegisterCount()==4,"Pinned delayed call registers");
                String target=id.equals(POST_CAMERA)?"postCamera":id.equals(POST_PREVIEW)?"postPreview":"postSettled";
                body.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,4,r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),0,
                    new ImmutableMethodReference(H,target,List.of("Landroid/os/Handler;","Ljava/lang/Runnable;","J"),"Z")));
                changes++;
            }
            require(changes==1,"Exactly one reviewed delayed native callback");
            return wrap(old,body);
        }
        int registers,first,count;String name;List<String> parameters;
        if(id.equals(CAMERA)){registers=11;first=7;count=3;name="camera";parameters=List.of("Li/o/a/b1/a/d/b;","I","I");}
        else if(id.equals(PREVIEW)){registers=12;first=9;count=2;name="preview";parameters=List.of("Li/o/a/m/j/f;","I");}
        else{registers=2;first=1;count=1;name="settled";parameters=List.of(SHADE);}
        require(body.getRegisterCount()==registers,"Delayed layout callback register ABI");
        require((old.getAccessFlags()&8)!=0 && old.getReturnType().equals("V"),"Delayed callback static void ABI");
        // Append an unreachable return as the reject destination. Native entry labels
        // remain attached to their original instruction, so internal branches never replay the guard.
        body.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        Label rejected=body.newLabelForIndex(body.getInstructions().size()-1);
        body.addInstruction(0,new BuilderInstruction21t(Opcode.IF_EQZ,0,rejected));
        body.addInstruction(0,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
        body.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,first,count,new ImmutableMethodReference(H,name,parameters,"Z")));
        return wrap(old,body);
    }
    public static void verify(Method before,Method after){
        require(MergePayloads.id(before).equals(MergePayloads.id(after)),"Lifecycle method identity");
        require(MergePayloads.hash(repair(before)).equals(MergePayloads.hash(after)),"Exact lifecycle guard transform");
        MutableMethodImplementation restored=new MutableMethodImplementation(after.getImplementation());
        if(MergePayloads.id(before).equals(LISTENER)) {
            int changes=0;
            for(int i=restored.getInstructions().size()-1;i>=0;i--)if(ref(restored.getInstructions().get(i)).equals("Lcom/hiro/ulike/PreviewLayout1922;->listenerAdded(Ljava/lang/Object;)V")){restored.removeInstruction(i);changes++;}
            require(changes==1,"One listener handoff hook only");
            require(MergePayloads.hash(wrap(before,restored)).equals(MergePayloads.hash(before)),"Inverse preserves native listener contains/add behavior");return;
        }
        if(post(MergePayloads.id(before))) {
            int changes=0;
            for(int i=0;i<restored.getInstructions().size();i++) {
                Instruction op=restored.getInstructions().get(i);
                if(!ref(op).startsWith(H+"->post"))continue;
                FiveRegisterInstruction r=(FiveRegisterInstruction)op;
                restored.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,4,r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),0,
                    new ImmutableMethodReference("Landroid/os/Handler;","postDelayed",List.of("Ljava/lang/Runnable;","J"),"Z")));
                changes++;
            }
            require(changes==1,"One exact post wrapper only");
            require(MergePayloads.hash(wrap(before,restored)).equals(MergePayloads.hash(before)),"Inverse preserves original callback scheduling/delay/registers");
            return;
        }
        require(restored.getInstructions().get(0).getOpcode()==Opcode.INVOKE_STATIC_RANGE && restored.getInstructions().get(1).getOpcode()==Opcode.MOVE_RESULT && restored.getInstructions().get(2).getOpcode()==Opcode.IF_EQZ,"Lifecycle prefix ABI");
        restored.removeInstruction(restored.getInstructions().size()-1);
        restored.removeInstruction(0);restored.removeInstruction(0);restored.removeInstruction(0);
        require(MergePayloads.hash(wrap(before,restored)).equals(MergePayloads.hash(before)),"Inverse preserves every native layout instruction/register/branch/handler");
    }
}
