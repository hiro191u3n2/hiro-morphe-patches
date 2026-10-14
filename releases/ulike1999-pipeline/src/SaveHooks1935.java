import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Pinned auto-save admission, immutable encoder handoff and per-shot options. */
public final class SaveHooks1935 {
    public static final String AUTO="Li/o/a/b1/a/b/f/a;->c(II)V";
    public static final String SHUTTER="Li/o/a/b1/a/g/y;->X(IZ)Z";
    public static final String STAGE="Lcom/hiro/ulike/SaveFd186;->saveStage(Landroid/graphics/Bitmap;Ljava/io/File;I)Z";
    public static final String SAVE="Lcom/hiro/ulike/SaveQuality2;->saveFinal(Landroid/graphics/Bitmap;Ljava/io/File;Landroid/graphics/Bitmap$CompressFormat;I)Z";
    public static final String FIXED="Lcom/hiro/ulike/SaveQuality2;->isFixed245Enabled()Z";
    public static final String CHROMA="Lcom/hiro/ulike/ChromaPipeline177;->apply(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;Lcom/hiro/ulike/PhotoDetail$Settings;)Landroid/graphics/Bitmap;";
    public static final Set<String> NATIVE=Set.of(AUTO,SHUTTER);
    public static final Set<String> RUNTIME=Set.of(STAGE,SAVE,FIXED,CHROMA);
    private static final String HELPER="Lcom/hiro/ulike/AsyncSave1935;";
    private static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    private static void require(boolean b,String s){if(!b)throw new IllegalStateException(s);}
    private static BuilderInstruction35c call(String name,String ret,List<String> args,int...r){return new BuilderInstruction35c(Opcode.INVOKE_STATIC,r.length,r.length>0?r[0]:0,r.length>1?r[1]:0,r.length>2?r[2]:0,0,0,new ImmutableMethodReference(HELPER,name,args,ret));}
    private static Method wrap(Method old,MethodImplementation body){return new ImmutableMethod(old.getDefiningClass(),old.getName(),old.getParameters(),old.getReturnType(),old.getAccessFlags(),old.getAnnotations(),old.getHiddenApiRestrictions(),body);}
    public static Method repair(Method old){
        String id=MergePayloads.id(old);require(NATIVE.contains(id)||RUNTIME.contains(id),"Unreviewed save hook "+id);
        MutableMethodImplementation body=new MutableMethodImplementation(old.getImplementation());
        if(id.equals(AUTO)){
            require(body.getRegisterCount()==4,"Auto-save ABI");
            int constructors=0;for(Instruction i:body.getInstructions())if(ref(i).equals("Li/o/a/b1/a/b/f/a$a;-><init>(Li/o/a/b1/a/b/f/a;II)V"))constructors++;
            require(constructors==1,"Auto-save field carrier constructor");
            return wrap(old,new ImmutableMethodImplementation(4,List.of(call("submitAuto","V",List.of("Ljava/lang/Object;","I","I"),1,2,3),new BuilderInstruction10x(Opcode.RETURN_VOID)),List.of(),List.of()));
        }
        if(id.equals(SHUTTER)){
            require(body.getRegisterCount()==9,"Shutter ABI");
            int ready=0;
            for(int i=0;i<body.getInstructions().size();i++){
                Instruction op=body.getInstructions().get(i);
                if(ref(op).equals("Li/o/a/b1/a/g/y;->D1()I")){
                    require(op.getOpcode()==Opcode.INVOKE_VIRTUAL && ((FiveRegisterInstruction)op).getRegisterC()==6,"Original shutter readiness ABI");
                    body.replaceInstruction(i,call("readiness","I",List.of("Ljava/lang/Object;"),6));ready++;
                }
            }
            require(ready==1,"Exactly one original readiness hook");
            return wrap(old,body);
        }
        int encoders=0,options=0;
        for(int i=0;i<body.getInstructions().size();i++){
            Instruction op=body.getInstructions().get(i);String target=ref(op);
            if((id.equals(STAGE)||id.equals(SAVE))&&target.equals("Landroidx/heifwriter/HeifWriter;->start()V")){
                require(op.getOpcode()==Opcode.INVOKE_VIRTUAL,"Encoder invocation ABI");
                int bitmap=id.equals(STAGE)?3:13;
                body.addInstruction(i++,call("encoding","V",List.of("Landroid/graphics/Bitmap;"),bitmap));encoders++;
            }
            if(target.equals("Lcom/hiro/ulike/SaveQuality2;->fixed245:Z")||target.equals("Lcom/hiro/ulike/ChromaPipeline177;->enabled:Z")){
                require(op.getOpcode()==Opcode.SGET_BOOLEAN,"Read-only option snapshot ABI");
                int register=((OneRegisterInstruction)op).getRegisterA();
                body.addInstruction(++i,call(id.equals(CHROMA)?"chroma":"fixed","Z",List.of("Z"),register));
                body.addInstruction(++i,new BuilderInstruction11x(Opcode.MOVE_RESULT,register));options++;
            }
        }
        require(encoders==((id.equals(STAGE)||id.equals(SAVE))?1:0),"Exactly one immutable encoder handoff "+id);
        require(options==(id.equals(STAGE)?0:1),"Exactly one per-shot option read "+id);
        return wrap(old,body);
    }
    public static void verify(Method before,Method after){
        require(MergePayloads.id(before).equals(MergePayloads.id(after)),"Save hook method identity");
        require(MergePayloads.hash(repair(before)).equals(MergePayloads.hash(after)),"Save hook exact reviewed transformation, registers/branches/handlers preserved");
        String id=MergePayloads.id(before);
        if(!id.equals(AUTO)){
            MutableMethodImplementation restored=new MutableMethodImplementation(after.getImplementation());
            if(id.equals(SHUTTER)){for(int i=0;i<restored.getInstructions().size();i++)if(ref(restored.getInstructions().get(i)).equals(HELPER+"->readiness(Ljava/lang/Object;)I"))restored.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,1,6,0,0,0,0,new ImmutableMethodReference("Li/o/a/b1/a/g/y;","D1",List.of(),"I")));}
            else for(int i=restored.getInstructions().size()-1;i>=0;i--){Instruction op=restored.getInstructions().get(i);if(ref(op).startsWith(HELPER)){
                if(ref(op).contains("->fixed(")||ref(op).contains("->chroma("))restored.removeInstruction(i+1);
                restored.removeInstruction(i);
            }}
            require(MergePayloads.hash(wrap(before,restored)).equals(MergePayloads.hash(before)),"Save hook full inverse changed unrelated code "+id);
        }
    }
}
