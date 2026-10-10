import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Exact pinned diagnostic boundaries. Image and save logic remains in five
 * aliases with identical original arguments, return values and catch tables. */
public final class TimingHooks1947 {
    static final String P="Lcom/hiro/ulike/", CORE=P+"ProcessingTiming1947;", IO=P+"TimedIo1947;";
    public static final String STAGE=P+"SaveFd186;->saveStage(Landroid/graphics/Bitmap;Ljava/io/File;I)Z";
    public static final String SAVE=P+"SaveQuality2;->saveFinal(Landroid/graphics/Bitmap;Ljava/io/File;Landroid/graphics/Bitmap$CompressFormat;I)Z";
    public static final String PUBLISH_STAGE=P+"SaveFd186;->publishStage(Ljava/lang/String;)Ljava/lang/String;";
    public static final String PUBLISH=P+"SaveQuality2;->publishFinal(Ljava/lang/String;)Ljava/lang/String;";
    public static final String LEGACY=P+"PhotoDetail;->applyDetailLegacy177(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;)Landroid/graphics/Bitmap;";
    public static final String INIT=P+"PhotoDetail;->initLegacy177(Landroid/content/Context;)V";
    public static final String INSTALL=P+"PhotoDetail;->install(Landroid/preference/PreferenceActivity;)V";
    public static final String STATUS=P+"PhotoDetail;->status(Ljava/lang/String;)V";
    public static final String SPAN_CTOR=P+"SaveFd186$Span;-><init>(Ljava/lang/String;)V";
    public static final String SPAN_CLOSE=P+"SaveFd186$Span;->close()V";
    public static final String WRITER_START="Landroidx/heifwriter/WriterBase;->start()V";
    public static final String MUX_DRAIN="Landroidx/heifwriter/WriterBase$WriterCallback;->onDrainOutputBuffer(Landroidx/heifwriter/EncoderBase;Ljava/nio/ByteBuffer;)V";
    public static final String MUX_FORMAT="Landroidx/heifwriter/WriterBase$WriterCallback;->onOutputFormatChanged(Landroidx/heifwriter/EncoderBase;Landroid/media/MediaFormat;)V";
    public static final String MUX_EXIF="Landroidx/heifwriter/WriterBase;->processExifData()V";
    public static final String MUX_CLOSE="Landroidx/heifwriter/WriterBase;->closeInternal()V";
    public static final Set<String> BRIDGES=Set.of(STAGE,SAVE,PUBLISH_STAGE,PUBLISH,LEGACY);
    public static final Set<String> RUNTIME=Set.of(STAGE,SAVE,PUBLISH_STAGE,PUBLISH,LEGACY,INIT,INSTALL,STATUS,SPAN_CTOR,SPAN_CLOSE,WRITER_START,MUX_DRAIN,MUX_FORMAT,MUX_EXIF,MUX_CLOSE);
    public static final Set<String> ALIASES;
    static {var values=new TreeSet<String>();for(String key:BRIDGES){int at=key.indexOf('(');values.add(key.substring(0,at)+"Before1947"+key.substring(at));}ALIASES=Collections.unmodifiableSet(values);}
    static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    static void need(boolean yes,String why){if(!yes)throw new IllegalStateException(why);}
    static Method wrap(Method old,MethodImplementation body){return named(old,old.getName(),body);}
    static Method named(Method old,String name,MethodImplementation body){return new ImmutableMethod(old.getDefiningClass(),name,old.getParameters(),old.getReturnType(),old.getAccessFlags(),old.getAnnotations(),old.getHiddenApiRestrictions(),body);}
    static BuilderInstruction35c call(String owner,String name,List<String> args,String result,int... registers){
        need(registers.length<=5,"coarse timer invoke size");int[] r=new int[5];System.arraycopy(registers,0,r,0,registers.length);
        for(int value:registers)need(value>=0&&value<16,"coarse timer invoke register");
        return new BuilderInstruction35c(Opcode.INVOKE_STATIC,registers.length,r[0],r[1],r[2],r[3],r[4],new ImmutableMethodReference(owner,name,args,result));
    }
    static int unique(MutableMethodImplementation body,String target){int at=-1;for(int i=0;i<body.getInstructions().size();i++)if(ref(body.getInstructions().get(i)).equals(target)){need(at<0,"unique diagnostic hook "+target);at=i;}need(at>=0,"missing diagnostic hook "+target);return at;}
    static Method bridge(Method old){
        List<String> args=new ArrayList<String>();old.getParameters().forEach(x->args.add(x.getType()));
        need((old.getAccessFlags()&AccessFlags.STATIC.getValue())!=0,"static diagnostic boundary");
        int count=0;for(String s:args)count+=s.equals("J")||s.equals("D")?2:1;
        MutableMethodImplementation body=new MutableMethodImplementation(count+1);
        String target=MergePayloads.id(old).equals(LEGACY)?"legacyDetail":old.getName();
        body.addInstruction(new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,1,count,new ImmutableMethodReference(IO,target,args,old.getReturnType())));
        need(!old.getReturnType().equals("V"),"nonvoid diagnostic bridge");
        boolean object=old.getReturnType().startsWith("L")||old.getReturnType().startsWith("[");
        body.addInstruction(new BuilderInstruction11x(object?Opcode.MOVE_RESULT_OBJECT:Opcode.MOVE_RESULT,0));
        body.addInstruction(new BuilderInstruction11x(object?Opcode.RETURN_OBJECT:Opcode.RETURN,0));
        return wrap(old,body);
    }
    static Method saveCalls(Method old){
        MutableMethodImplementation body=new MutableMethodImplementation(old.getImplementation());
        need(body.getRegisterCount()==14,"pinned save ABI");int starts=0,stops=0,closes=0;
        for(int i=0;i<body.getInstructions().size();i++){
            Instruction op=body.getInstructions().get(i);String target=ref(op),name=null;List<String> args=null;
            if(target.equals("Landroidx/heifwriter/HeifWriter;->start()V")){name="start";args=List.of("Landroidx/heifwriter/HeifWriter;");starts++;}
            if(target.equals("Landroidx/heifwriter/HeifWriter;->stop(J)V")){name="stop";args=List.of("Landroidx/heifwriter/HeifWriter;","J");stops++;}
            if(target.equals(P+"CodecDrain1945;->close(Landroidx/heifwriter/HeifWriter;)V")){name="close";args=List.of("Landroidx/heifwriter/HeifWriter;");closes++;}
            if(name!=null){need(op instanceof FiveRegisterInstruction,"pinned HEIF timer invocation");FiveRegisterInstruction x=(FiveRegisterInstruction)op;body.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,x.getRegisterCount(),x.getRegisterC(),x.getRegisterD(),x.getRegisterE(),x.getRegisterF(),x.getRegisterG(),new ImmutableMethodReference(IO,name,args,"V")));}
        }
        need(starts==1&&stops==1&&closes==(MergePayloads.id(old).equals(STAGE)?1:5),"exact HEIF start/stop/close sites");return wrap(old,body);
    }
    /** Extra method to add to the original class; null for non-bridge hooks. */
    public static Method alias(Method old){
        String id=MergePayloads.id(old);if(!BRIDGES.contains(id))return null;
        Method prepared=id.equals(STAGE)||id.equals(SAVE)?saveCalls(old):old;
        return named(prepared,old.getName()+"Before1947",prepared.getImplementation());
    }
    public static Method repair(Method old){
        String id=MergePayloads.id(old);need(RUNTIME.contains(id),"unreviewed diagnostic hook "+id);
        if(BRIDGES.contains(id))return bridge(old);
        MutableMethodImplementation body=new MutableMethodImplementation(old.getImplementation());
        if(id.equals(STATUS)){
            body=new MutableMethodImplementation(1);
            body.addInstruction(call(IO,"legacyStatus",List.of("Ljava/lang/String;"),"V",0));
            body.addInstruction(call(CORE,"summary",List.of(),"Ljava/lang/String;"));
            body.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,0));
            body.addInstruction(new BuilderInstruction21c(Opcode.SPUT_OBJECT,0,new ImmutableFieldReference(P+"PhotoDetail;","last","Ljava/lang/String;")));
            body.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));return wrap(old,body);
        }
        if(id.equals(INIT)){
            need(body.getRegisterCount()==11,"pinned legacy initialization ABI");
            int get=unique(body,"Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
            need(body.getInstructions().get(get+1).getOpcode()==Opcode.MOVE_RESULT_OBJECT,"legacy summary loading result");
            body.replaceInstruction(get,call(CORE,"summary",List.of(),"Ljava/lang/String;"));
            body.addInstruction(0,call(CORE,"init",List.of("Landroid/content/Context;"),"V",10));return wrap(old,body);
        }
        if(id.equals(INSTALL)){
            need(body.getRegisterCount()==1&&body.getInstructions().size()==3,"pinned setting installer ABI");
            need(ref(body.getInstructions().get(0)).equals(P+"PhotoDetail;->installLegacy177(Landroid/preference/PreferenceActivity;)V")&&ref(body.getInstructions().get(1)).equals(P+"ChromaPipeline177;->install(Landroid/preference/PreferenceActivity;)V"),"legacy+chroma installers retained");
            body.addInstruction(2,call(CORE,"install",List.of("Landroid/preference/PreferenceActivity;"),"V",0));return wrap(old,body);
        }
        if(id.equals(SPAN_CTOR)){
            need(body.getRegisterCount()==4,"pinned save span ABI");
            int phase=unique(body,P+"SaveFd186$Span;->phase:Ljava/lang/String;");
            need(body.getInstructions().get(phase).getOpcode()==Opcode.IPUT_OBJECT,"span phase established");
            body.addInstruction(phase+1,call(IO,"spanStarted",List.of("Ljava/lang/Object;","Ljava/lang/String;"),"V",2,3));return wrap(old,body);
        }
        if(id.equals(SPAN_CLOSE)){
            need(body.getRegisterCount()==6,"pinned span-close ABI");body.addInstruction(0,call(IO,"spanClosed",List.of("Ljava/lang/Object;"),"V",5));return wrap(old,body);
        }
        if(id.equals(WRITER_START)){
            need(body.getRegisterCount()==2&&body.getInstructions().get(0).getOpcode()==Opcode.CONST_4,"pinned writer start ABI");
            body.addInstruction(0,new BuilderInstruction22c(Opcode.IGET_OBJECT,0,1,new ImmutableFieldReference("Landroidx/heifwriter/WriterBase;","mMuxer","Landroid/media/MediaMuxer;")));
            body.addInstruction(1,call(IO,"bindMuxer",List.of("Ljava/lang/Object;","Landroid/media/MediaMuxer;"),"V",1,0));return wrap(old,body);
        }
        int writes=0,starts=0,stops=0,releases=0;
        for(int i=0;i<body.getInstructions().size();i++){
            Instruction op=body.getInstructions().get(i);String target=ref(op),name=null;List<String> args=null;
            if(target.equals("Landroid/media/MediaMuxer;->writeSampleData(ILjava/nio/ByteBuffer;Landroid/media/MediaCodec$BufferInfo;)V")){name="writeSampleData";args=List.of("Landroid/media/MediaMuxer;","I","Ljava/nio/ByteBuffer;","Landroid/media/MediaCodec$BufferInfo;");writes++;}
            if(target.equals("Landroid/media/MediaMuxer;->start()V")){name="muxerStart";args=List.of("Landroid/media/MediaMuxer;");starts++;}
            if(target.equals("Landroid/media/MediaMuxer;->stop()V")){name="muxerStop";args=List.of("Landroid/media/MediaMuxer;");stops++;}
            if(target.equals("Landroid/media/MediaMuxer;->release()V")){name="muxerRelease";args=List.of("Landroid/media/MediaMuxer;");releases++;}
            if(name!=null){need(op instanceof FiveRegisterInstruction,"pinned mux timer call");FiveRegisterInstruction x=(FiveRegisterInstruction)op;body.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,x.getRegisterCount(),x.getRegisterC(),x.getRegisterD(),x.getRegisterE(),x.getRegisterF(),x.getRegisterG(),new ImmutableMethodReference(IO,name,args,"V")));}
        }
        need(writes==(id.equals(MUX_DRAIN)||id.equals(MUX_EXIF)?1:0)&&starts==(id.equals(MUX_FORMAT)?1:0)&&stops==(id.equals(MUX_CLOSE)?1:0)&&releases==(id.equals(MUX_CLOSE)?1:0),"all and only exact mux IO boundaries");return wrap(old,body);
    }
    public static void verify(Method before,Method after){
        need(MergePayloads.id(before).equals(MergePayloads.id(after)),"diagnostic method identity");
        need(MergePayloads.hash(repair(before)).equals(MergePayloads.hash(after)),"exact diagnostic hook differs "+MergePayloads.id(before));
    }
    public static void verifyAlias(Method before,Method after){
        Method expected=alias(before);need(expected!=null&&MergePayloads.id(expected).equals(MergePayloads.id(after)),"reviewed alias identity");
        need(MergePayloads.hash(expected).equals(MergePayloads.hash(after)),"alias original body/exception paths changed");
        need(after.getAccessFlags()==before.getAccessFlags(),"alias access unchanged");
    }
}
