import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** 1.9.44 baseline only: prepare one idle codec during unchanged correction,
 * reuse one callback looper, and await asynchronous release before reuse.
 * Existing MediaStore FD destination and all pixel/codec options are retained. */
public final class SaveSpeedHooks1945 {
    public static final String STAGE="Lcom/hiro/ulike/SaveFd186;->saveStage(Landroid/graphics/Bitmap;Ljava/io/File;I)Z";
    public static final String SAVE="Lcom/hiro/ulike/SaveQuality2;->saveFinal(Landroid/graphics/Bitmap;Ljava/io/File;Landroid/graphics/Bitmap$CompressFormat;I)Z";
    public static final String PREPARE="Lcom/hiro/ulike/SaveFd186$Pending;->prepare()V";
    public static final String ABORT="Lcom/hiro/ulike/SaveFd186$Pending;->abort()V";
    public static final String ENCODER_CTOR="Landroidx/heifwriter/EncoderBase;-><init>(Ljava/lang/String;IIZIILandroid/os/Handler;Landroidx/heifwriter/EncoderBase$Callback;Z)V";
    public static final String STOP_INTERNAL="Landroidx/heifwriter/EncoderBase;->stopInternal()V";
    public static final Set<String> RUNTIME=Set.of(STAGE,SAVE,PREPARE,ABORT,ENCODER_CTOR,STOP_INTERNAL);
    static final String ENCODING="Lcom/hiro/ulike/AsyncSave1935;->encoding(Landroid/graphics/Bitmap;)V";
    static final String PREP="Lcom/hiro/ulike/SaveFd186;->PREP:Ljava/util/concurrent/ExecutorService;";
    static final String CTOR="Lcom/hiro/ulike/SaveFd186$4;-><init>(Lcom/hiro/ulike/PrepTicket186;Landroid/content/Context;[I)V";
    static final String EXECUTE="Ljava/util/concurrent/ExecutorService;->execute(Ljava/lang/Runnable;)V";
    static final String PREPARE_OWNED="Lcom/hiro/ulike/AsyncSave1935;->prepareCodec(Ljava/util/concurrent/ExecutorService;Ljava/lang/Runnable;)V";
    static final String CLOSE="Landroidx/heifwriter/HeifWriter;->close()V";
    static final String BUILD="Landroidx/heifwriter/HeifWriter$Builder;->build()Landroidx/heifwriter/HeifWriter;";
    static final String HANDLER="Lcom/hiro/ulike/CodecDrain1945;->handler()Landroid/os/Handler;";
    static final String BARRIER="Lcom/hiro/ulike/CodecDrain1945;->awaitClosed()V";
    static final String SET_HANDLER="Landroidx/heifwriter/HeifWriter$Builder;->setHandler(Landroid/os/Handler;)Landroidx/heifwriter/HeifWriter$Builder;";
    static final String OWNED_BUILD="Lcom/hiro/ulike/CodecDrain1945;->build(Landroidx/heifwriter/HeifWriter$Builder;)Landroidx/heifwriter/HeifWriter;";
    static final String OWNED_CLOSE="Lcom/hiro/ulike/CodecDrain1945;->close(Landroidx/heifwriter/HeifWriter;)V";
    static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    static void need(boolean yes,String why){if(!yes)throw new IllegalStateException(why);}
    static int find(MutableMethodImplementation body,String target){int at=-1;for(int i=0;i<body.getInstructions().size();i++)if(ref(body.getInstructions().get(i)).equals(target)){need(at<0,"unique save hook "+target);at=i;}need(at>=0,"missing save hook "+target);return at;}
    static Method wrap(Method old,MethodImplementation body){return new ImmutableMethod(old.getDefiningClass(),old.getName(),old.getParameters(),old.getReturnType(),old.getAccessFlags(),old.getAnnotations(),old.getHiddenApiRestrictions(),body);}
    static BuilderInstruction35c noArgs(String owner,String name,String result){return new BuilderInstruction35c(Opcode.INVOKE_STATIC,0,0,0,0,0,0,new ImmutableMethodReference(owner,name,List.of(),result));}
    public static Method repair(Method old){
        String id=MergePayloads.id(old);need(RUNTIME.contains(id),"unreviewed save lifecycle hook "+id);
        MutableMethodImplementation body=new MutableMethodImplementation(old.getImplementation());
        if(id.equals(ENCODER_CTOR))return repairConstructor(old,body);
        if(id.equals(STOP_INTERNAL))return repairStop(old,body);
        need(body.getRegisterCount()==(id.equals(PREPARE)?8:id.equals(ABORT)?4:14),"pinned save lifecycle register ABI "+id);
        if(id.equals(STAGE)){
            int prep=find(body,PREP),construct=find(body,CTOR),execute=find(body,EXECUTE);
            need(construct==prep+2&&execute==prep+3,"1.9.44 deferred preparation task");
            Instruction create=body.getInstructions().get(prep+1);
            need(create.getOpcode()==Opcode.NEW_INSTANCE&&((OneRegisterInstruction)create).getRegisterA()==8,"prep task object ABI");
            need(body.getInstructions().get(prep-1) instanceof FiveRegisterInstruction&&ref(body.getInstructions().get(prep-1)).equals(ENCODING)&&((FiveRegisterInstruction)body.getInstructions().get(prep-1)).getRegisterC()==3,"44 after-correction encoder gate retained");
            FiveRegisterInstruction constructor=(FiveRegisterInstruction)body.getInstructions().get(construct);
            need(constructor.getRegisterC()==8&&constructor.getRegisterD()==6&&constructor.getRegisterE()==2&&constructor.getRegisterF()==10,"44 dimensions-array constructor ABI");
            // Replace deferred task with NOPs so all existing catch boundaries
            // remain anchored. The original gate dispatches a deferred task.
            for(int n=0;n<4;n++)body.replaceInstruction(prep+n,new BuilderInstruction10x(Opcode.NOP));
            int lease=-1,count=0;
            for(int i=0;i<body.getInstructions().size();i++)if(ref(body.getInstructions().get(i)).equals("Lcom/hiro/ulike/ExitJobs185;->begin()V")){count++;if(count==2)lease=i;}
            need(count==2&&lease>=0&&lease<prep,"exact preparation ticket lease");
            for(int n=1;n<=4;n++)need(body.getInstructions().get(lease+n).getOpcode()==Opcode.NOP,"44 protected early submit placeholder");
            body.replaceInstruction(lease+1,new BuilderInstruction21c(Opcode.SGET_OBJECT,7,new ImmutableFieldReference("Lcom/hiro/ulike/SaveFd186;","PREP","Ljava/util/concurrent/ExecutorService;")));
            body.replaceInstruction(lease+2,new BuilderInstruction21c(Opcode.NEW_INSTANCE,8,new ImmutableTypeReference("Lcom/hiro/ulike/SaveFd186$4;")));
            body.replaceInstruction(lease+3,new BuilderInstruction35c(Opcode.INVOKE_DIRECT,4,8,6,2,4,0,new ImmutableMethodReference("Lcom/hiro/ulike/SaveFd186$4;","<init>",List.of("Lcom/hiro/ulike/PrepTicket186;","Landroid/content/Context;","[I"),"V")));
            body.replaceInstruction(lease+4,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,7,8,0,0,0,new ImmutableMethodReference("Lcom/hiro/ulike/AsyncSave1935;","prepareCodec",List.of("Ljava/util/concurrent/ExecutorService;","Ljava/lang/Runnable;"),"V")));
        }
        if(id.equals(PREPARE)||id.equals(SAVE)){
            int build=find(body,BUILD),receiver=id.equals(PREPARE)?0:5;
            need(body.getInstructions().get(build) instanceof FiveRegisterInstruction&&((FiveRegisterInstruction)body.getInstructions().get(build)).getRegisterC()==receiver,"builder receiver ABI");
            body.replaceInstruction(build,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,receiver,0,0,0,0,new ImmutableMethodReference("Lcom/hiro/ulike/CodecDrain1945;","build",List.of("Landroidx/heifwriter/HeifWriter$Builder;"),"Landroidx/heifwriter/HeifWriter;")));
        }
        int expected=id.equals(STAGE)?1:id.equals(SAVE)?5:id.equals(ABORT)?1:0,seen=0;
        for(int i=body.getInstructions().size()-1;i>=0;i--)if(ref(body.getInstructions().get(i)).equals(CLOSE)){
            FiveRegisterInstruction close=(FiveRegisterInstruction)body.getInstructions().get(i);
            body.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,close.getRegisterC(),0,0,0,0,new ImmutableMethodReference("Lcom/hiro/ulike/CodecDrain1945;","close",List.of("Landroidx/heifwriter/HeifWriter;"),"V")));seen++;
        }
        need(seen==expected,"all and only reviewed HEIF close barriers "+id);
        return wrap(old,body);
    }
    static Method repairStop(Method old,MutableMethodImplementation body){
        need(body.getRegisterCount()==4,"pinned AndroidX stop register ABI");
        int stop=find(body,"Landroid/media/MediaCodec;->stop()V"),release=find(body,"Landroid/media/MediaCodec;->release()V");
        need(release==stop+2&&body.getInstructions().get(stop+1).getOpcode()==Opcode.IGET_OBJECT,"pinned stop then release sequence");
        need(((FiveRegisterInstruction)body.getInstructions().get(stop)).getRegisterC()==2&&((FiveRegisterInstruction)body.getInstructions().get(release)).getRegisterC()==2,"pinned actual codec register");
        body.replaceInstruction(stop,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,2,0,0,0,0,new ImmutableMethodReference("Lcom/hiro/ulike/CodecDrain1945;","stopAndRelease",List.of("Landroid/media/MediaCodec;"),"V")));
        body.replaceInstruction(release,new BuilderInstruction10x(Opcode.NOP));return wrap(old,body);
    }
    static Method repairConstructor(Method old,MutableMethodImplementation body){
        need(body.getRegisterCount()==34,"pinned AndroidX constructor register ABI");
        int byName=0,byType=0,releases=0;
        for(int i=0;i<body.getInstructions().size();i++){
            Instruction op=body.getInstructions().get(i);String call=ref(op);
            String name=null,argument=null,result=null;
            if(call.equals("Landroid/media/MediaCodec;->createByCodecName(Ljava/lang/String;)Landroid/media/MediaCodec;")){name="createByCodecName";argument="Ljava/lang/String;";result="Landroid/media/MediaCodec;";byName++;}
            if(call.equals("Landroid/media/MediaCodec;->createEncoderByType(Ljava/lang/String;)Landroid/media/MediaCodec;")){name="createEncoderByType";argument="Ljava/lang/String;";result="Landroid/media/MediaCodec;";byType++;}
            if(call.equals("Landroid/media/MediaCodec;->release()V")){name="releaseCodec";argument="Landroid/media/MediaCodec;";result="V";releases++;}
            if(name!=null){need(op instanceof FiveRegisterInstruction,"pinned codec factory invocation");int register=((FiveRegisterInstruction)op).getRegisterC();body.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,register,0,0,0,0,new ImmutableMethodReference("Lcom/hiro/ulike/CodecDrain1945;",name,List.of(argument),result)));}
        }
        need(byName==2&&byType==1&&releases==1,"only exact reviewed constructor factories/release");return wrap(old,body);
    }
    public static void verify(Method before,Method after){
        String id=MergePayloads.id(before);need(id.equals(MergePayloads.id(after)),"save method identity");
        need(MergePayloads.hash(repair(before)).equals(MergePayloads.hash(after)),"exact reviewed save lifecycle change");
        List<String>a=new ArrayList<String>(),b=new ArrayList<String>();
        for(Instruction i:before.getImplementation().getInstructions())if(!ref(i).isEmpty())a.add(ref(i));
        for(Instruction i:after.getImplementation().getInstructions())if(!ref(i).isEmpty())b.add(ref(i));
        if(id.equals(STOP_INTERNAL)){
            need(a.remove("Landroid/media/MediaCodec;->stop()V")&&a.remove("Landroid/media/MediaCodec;->release()V"),"original stop and release");a.add("Lcom/hiro/ulike/CodecDrain1945;->stopAndRelease(Landroid/media/MediaCodec;)V");
            Collections.sort(a);Collections.sort(b);need(a.equals(b),"same encoder teardown with unconditional release");return;
        }
        if(id.equals(ENCODER_CTOR)){
            for(int n=0;n<2;n++){need(a.remove("Landroid/media/MediaCodec;->createByCodecName(Ljava/lang/String;)Landroid/media/MediaCodec;"),"original named codec factory");a.add("Lcom/hiro/ulike/CodecDrain1945;->createByCodecName(Ljava/lang/String;)Landroid/media/MediaCodec;");}
            need(a.remove("Landroid/media/MediaCodec;->createEncoderByType(Ljava/lang/String;)Landroid/media/MediaCodec;"),"original typed codec factory");a.add("Lcom/hiro/ulike/CodecDrain1945;->createEncoderByType(Ljava/lang/String;)Landroid/media/MediaCodec;");
            need(a.remove("Landroid/media/MediaCodec;->release()V"),"original unsupported-size release");a.add("Lcom/hiro/ulike/CodecDrain1945;->releaseCodec(Landroid/media/MediaCodec;)V");
            Collections.sort(a);Collections.sort(b);need(a.equals(b),"same codec configuration/fallback with tracked constructor resources");return;
        }
        if(id.equals(STAGE)){need(a.remove(EXECUTE),"existing executor submission");a.add(PREPARE_OWNED);}
        if(id.equals(PREPARE)||id.equals(SAVE)){need(a.remove(BUILD),"existing builder allocation");a.add(OWNED_BUILD);}
        int closes=id.equals(STAGE)?1:id.equals(SAVE)?5:id.equals(ABORT)?1:0;
        for(int n=0;n<closes;n++){need(a.remove(CLOSE),"existing writer close");a.add(OWNED_CLOSE);}
        Collections.sort(a);Collections.sort(b);need(a.equals(b),"same save/verify/publish/quality calls with lifecycle hooks only "+id);
    }
}
