import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Move actual codec allocation behind the FIFO encoder permit, after unchanged
 * normalize/detail and output-size validation. Ticket leases keep original ABI. */
public final class SaveSpeedHooks1944 {
    public static final String STAGE="Lcom/hiro/ulike/SaveFd186;->saveStage(Landroid/graphics/Bitmap;Ljava/io/File;I)Z";
    public static final String SAVE="Lcom/hiro/ulike/SaveQuality2;->saveFinal(Landroid/graphics/Bitmap;Ljava/io/File;Landroid/graphics/Bitmap$CompressFormat;I)Z";
    public static final Set<String> RUNTIME=Set.of(STAGE,SAVE);
    static final String ENCODING="Lcom/hiro/ulike/AsyncSave1935;->encoding(Landroid/graphics/Bitmap;)V";
    static final String PREP="Lcom/hiro/ulike/SaveFd186;->PREP:Ljava/util/concurrent/ExecutorService;";
    static final String CTOR="Lcom/hiro/ulike/SaveFd186$4;-><init>(Lcom/hiro/ulike/PrepTicket186;Landroid/content/Context;[I)V";
    static final String EXECUTE="Ljava/util/concurrent/ExecutorService;->execute(Ljava/lang/Runnable;)V";
    static final String CLOSE="Lcom/hiro/ulike/SaveFd186$Span;->close()V";
    static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    static void need(boolean yes,String why){if(!yes)throw new IllegalStateException(why);}
    static int find(MutableMethodImplementation body,String target){int at=-1;for(int i=0;i<body.getInstructions().size();i++)if(ref(body.getInstructions().get(i)).equals(target)){need(at<0,"unique save hook "+target);at=i;}need(at>=0,"missing save hook "+target);return at;}
    static Method wrap(Method old,MethodImplementation body){return new ImmutableMethod(old.getDefiningClass(),old.getName(),old.getParameters(),old.getReturnType(),old.getAccessFlags(),old.getAnnotations(),old.getHiddenApiRestrictions(),body);}
    static BuilderInstruction35c gate(int register){return new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,register,0,0,0,0,new ImmutableMethodReference("Lcom/hiro/ulike/AsyncSave1935;","encoding",List.of("Landroid/graphics/Bitmap;"),"V"));}
    public static Method repair(Method old){
        String id=MergePayloads.id(old);need(RUNTIME.contains(id),"unreviewed save allocation hook "+id);
        MutableMethodImplementation body=new MutableMethodImplementation(old.getImplementation());
        need(body.getRegisterCount()==14,"pinned save allocation register ABI "+id);
        find(body,ENCODING); // Preserve the original final-bitmap/start handshake.
        if(id.equals(SAVE)){
            int build=find(body,"Landroidx/heifwriter/HeifWriter$Builder;->build()Landroidx/heifwriter/HeifWriter;");
            need(body.getInstructions().get(build).getOpcode()==Opcode.INVOKE_VIRTUAL,"fallback builder invocation ABI");
            body.addInstruction(build,gate(13));return wrap(old,body);
        }
        int prep=find(body,PREP),construct=find(body,CTOR),execute=find(body,EXECUTE);
        need(construct==prep+2&&execute==prep+3,"contiguous unchanged preparation task submission");
        Instruction field=body.getInstructions().get(prep),created=body.getInstructions().get(prep+1),ctor=body.getInstructions().get(construct),exec=body.getInstructions().get(execute);
        need(field.getOpcode()==Opcode.SGET_OBJECT&&((OneRegisterInstruction)field).getRegisterA()==7,"prep executor register");
        need(created.getOpcode()==Opcode.NEW_INSTANCE&&((OneRegisterInstruction)created).getRegisterA()==8&&ref(created).equals("Lcom/hiro/ulike/SaveFd186$4;"),"prep runnable register");
        need(ctor instanceof FiveRegisterInstruction&&((FiveRegisterInstruction)ctor).getRegisterC()==8&&((FiveRegisterInstruction)ctor).getRegisterD()==6&&((FiveRegisterInstruction)ctor).getRegisterE()==2&&((FiveRegisterInstruction)ctor).getRegisterF()==4,"prep task constructor argument ABI");
        need(exec instanceof FiveRegisterInstruction&&((FiveRegisterInstruction)exec).getRegisterC()==7&&((FiveRegisterInstruction)exec).getRegisterD()==8,"prep execute argument ABI");
        // Keep the original prep ExitJobs.begin in place. On normalization
        // failure, a still-QUEUED PrepTicket.abandon releases that lease once.
        // NOP the old protected submit range instead of deleting it. Keeping
        // a non-empty try span preserves ART exception table validity.
        for(int n=0;n<4;n++)body.replaceInstruction(prep+n,new BuilderInstruction10x(Opcode.NOP));
        int heightArray=-1;
        for(int i=0;i<body.getInstructions().size();i++){
            Instruction op=body.getInstructions().get(i);
            if(op.getOpcode()==Opcode.AGET&&op instanceof ThreeRegisterInstruction){ThreeRegisterInstruction r=(ThreeRegisterInstruction)op;if(r.getRegisterA()==4&&r.getRegisterB()==4&&r.getRegisterC()==5){need(heightArray<0,"unique height output dimension consumption");heightArray=i;}}
        }
        need(heightArray>=0,"height size validation ABI");
        // v10 is an unused local until later encoder timeout (wide v9/v10).
        // Preserve the same dimensions array for the deferred prep constructor.
        body.addInstruction(heightArray,new BuilderInstruction12x(Opcode.MOVE_OBJECT,10,4));
        int closed=-1;
        for(int i=0;i<body.getInstructions().size();i++)if(closed<0&&ref(body.getInstructions().get(i)).equals(CLOSE)&&((FiveRegisterInstruction)body.getInstructions().get(i)).getRegisterC()==7)closed=i;
        need(closed>heightArray,"normalization and dimension validation precede codec allocation");
        int at=closed+1;
        body.addInstruction(at++,gate(3));
        body.addInstruction(at++,new BuilderInstruction21c(Opcode.SGET_OBJECT,7,new ImmutableFieldReference("Lcom/hiro/ulike/SaveFd186;","PREP","Ljava/util/concurrent/ExecutorService;")));
        body.addInstruction(at++,new BuilderInstruction21c(Opcode.NEW_INSTANCE,8,new ImmutableTypeReference("Lcom/hiro/ulike/SaveFd186$4;")));
        body.addInstruction(at++,new BuilderInstruction35c(Opcode.INVOKE_DIRECT,4,8,6,2,10,0,new ImmutableMethodReference("Lcom/hiro/ulike/SaveFd186$4;","<init>",List.of("Lcom/hiro/ulike/PrepTicket186;","Landroid/content/Context;","[I"),"V")));
        body.addInstruction(at,new BuilderInstruction35c(Opcode.INVOKE_INTERFACE,2,7,8,0,0,0,new ImmutableMethodReference("Ljava/util/concurrent/ExecutorService;","execute",List.of("Ljava/lang/Runnable;"),"V")));
        return wrap(old,body);
    }
    public static void verify(Method before,Method after){
        need(MergePayloads.id(before).equals(MergePayloads.id(after)),"save method identity");
        need(MergePayloads.hash(repair(before)).equals(MergePayloads.hash(after)),"exact reviewed save allocation relocation");
        List<String> a=new ArrayList<String>(),b=new ArrayList<String>();
        for(Instruction i:before.getImplementation().getInstructions())if(!ref(i).isEmpty())a.add(ref(i));
        for(Instruction i:after.getImplementation().getInstructions())if(!ref(i).isEmpty())b.add(ref(i));
        Collections.sort(a);Collections.sort(b);a.add(ENCODING);Collections.sort(a);
        need(a.equals(b),"same save calls/codec options and one extra ownership gate only");
    }
}
