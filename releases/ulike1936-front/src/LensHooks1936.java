import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Event-driven overlay updates, preserving every existing native switch guard. */
public final class LensHooks1936 {
    public static final String SWITCH="Li/f/l/n/q/y/c;->g0(Z)V";
    public static final String INSTALL="Lcom/hiro/ulike/OpticalZoomUi$Bar;->install()V";
    public static final Set<String> NATIVE=Set.of(SWITCH),RUNTIME=Set.of(INSTALL);
    private static final String HELPER="Lcom/hiro/ulike/LensVisibility1936;";
    private static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    private static void require(boolean b,String s){if(!b)throw new IllegalStateException(s);}
    private static Method wrap(Method old,MethodImplementation body){return new ImmutableMethod(old.getDefiningClass(),old.getName(),old.getParameters(),old.getReturnType(),old.getAccessFlags(),old.getAnnotations(),old.getHiddenApiRestrictions(),body);}
    public static Method repair(Method old){
        String id=MergePayloads.id(old);require(NATIVE.contains(id)||RUNTIME.contains(id),"Unreviewed lens hook "+id);
        MutableMethodImplementation body=new MutableMethodImplementation(old.getImplementation());
        require(body.getTryBlocks().isEmpty(),"Pinned lens hooks have no existing handlers");
        if(id.equals(INSTALL)){
            require(body.getRegisterCount()==1,"Bar install register ABI");
            require(body.getInstructions().size()==3,"Bar install shape");
            require(ref(body.getInstructions().get(0)).equals("Lcom/hiro/ulike/MacroUi168;->attach(Ljava/lang/Object;)V"),"Existing macro attachment");
            require(ref(body.getInstructions().get(1)).equals("Lcom/hiro/ulike/OpticalZoomUi$Bar;->installCore168()V"),"Existing bar installer");
            body.addInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,0,0,0,0,0,new ImmutableMethodReference(HELPER,"register",List.of("Ljava/lang/Object;"),"V")));
        }else{
            require(body.getRegisterCount()==7,"Native accepted switch register ABI");
            int writes=0,starts=0;
            for(int i=0;i<body.getInstructions().size();i++){
                Instruction op=body.getInstructions().get(i);
                if(ref(op).equals("Li/f/l/u/p;->k(Li/f/l/u/p;Ljava/lang/Object;ZILjava/lang/Object;)V")){
                    require(op.getOpcode()==Opcode.INVOKE_STATIC && ((FiveRegisterInstruction)op).getRegisterCount()==5,"Native facing write ABI");
                    require(ref(body.getInstructions().get(i-3)).equals("Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;"),"Facing value boxing");
                    body.addInstruction(++i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,5,6,0,0,0,new ImmutableMethodReference(HELPER,"acceptedSwitch",List.of("Li/f/l/n/q/y/c;","Z"),"V")));
                    writes++;
                }
                if(ref(op).equals("Li/f/l/n/n;->C(Z)V"))starts++;
            }
            require(writes==1 && starts==1,"Exactly one accepted native facing write and original camera switch");
        }
        return wrap(old,body);
    }
    public static void verify(Method before,Method after){
        require(MergePayloads.id(before).equals(MergePayloads.id(after)),"Lens hook method identity");
        require(MergePayloads.hash(repair(before)).equals(MergePayloads.hash(after)),"Exact reviewed lens hook");
        MutableMethodImplementation restored=new MutableMethodImplementation(after.getImplementation());
        int removed=0;
        for(int i=restored.getInstructions().size()-1;i>=0;i--)if(ref(restored.getInstructions().get(i)).startsWith(HELPER)) {restored.removeInstruction(i);removed++;}
        require(removed==1,"One event hook only");
        require(MergePayloads.hash(wrap(before,restored)).equals(MergePayloads.hash(before)),"Full inverse preserves every native guard/branch/field/camera call");
    }
}
