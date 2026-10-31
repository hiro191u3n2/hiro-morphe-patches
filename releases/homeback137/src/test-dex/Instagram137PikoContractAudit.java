import java.util.*;
import app.hiro.oneback.patches.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;

/** Independent proof of the exact two Piko navigation changes admitted by the
 * Reels contract. All supplied APK paths are read-only. This never calls the
 * production normalization helper to generate the expected native body. */
public final class Instagram137PikoContractAudit {
    static final String EXT="Lapp/morphe/extension/instagram/patches/navigation/NavigationBarPatch;";
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static Method method(Map<String,ClassDef> classes,String owner,String name){return Instagram137ProfileReelsAudit.find(classes.get(owner),name);}
    static List<Instruction> list(Method method){return Instagram137ProfileReelsAudit.list(method);}
    static int address(List<Instruction> body,int index){int pc=0;for(int i=0;i<index;i++)pc+=body.get(i).getCodeUnits();return pc;}
    static Method withBody(Method method,MethodImplementation code){return Instagram137ProfileReelsAudit.immutable(method,code);}
    static void same(Method expected,Method actual,String why)throws Exception{check(Arrays.equals(Instagram452Audit.bytes(expected),Instagram452Audit.bytes(actual)),why);}
    static BuilderInstruction35c call(String owner,String name,List<String> parameters,String result,int... registers) {
        int[] r=Arrays.copyOf(registers,5);
        return new BuilderInstruction35c(Opcode.INVOKE_STATIC,registers.length,r[0],r[1],r[2],r[3],r[4],new ImmutableMethodReference(owner,name,parameters,result));
    }
    static String tryShape(Method method) {
        StringBuilder shape=new StringBuilder();
        for(TryBlock<? extends ExceptionHandler> block:method.getImplementation().getTryBlocks()) {
            shape.append(block.getStartCodeAddress()).append(':').append(block.getCodeUnitCount());
            for(ExceptionHandler handler:block.getExceptionHandlers())shape.append('/').append(handler.getExceptionType()).append('@').append(handler.getHandlerCodeAddress());
            shape.append(';');
        }
        return shape.toString();
    }
    /** Reproduce Piko's actual installation order, not a mutable wrapper of a
     * serialized DEX: prepend the nine instructions, then append the catch. */
    static MutableClass liveStartup(ClassDef original) {
        MutableClass type=new MutableClass(original);MutableMethod method=(MutableMethod)Instagram137ProfileReelsAudit.find(type,"A07");
        MutableMethodImplementation code=method.getImplementation();Label nativeEntry=code.newLabelForIndex(0);
        code.addInstruction(0,call(EXT,"preflightStartup",Arrays.asList("Landroid/content/Intent;","Z"),"Z",6,8));
        code.addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT,1));
        code.addInstruction(2,new BuilderInstruction21t(Opcode.IF_EQZ,1,nativeEntry));
        code.addInstruction(3,call("LX/004E;","A01",Collections.singletonList("Lcom/instagram/common/session/UserSession;"),"Ljava/util/List;",7));
        code.addInstruction(4,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,1));
        code.addInstruction(5,call(EXT,"applyStartupTab",Arrays.asList("Landroid/content/Intent;","Z","Ljava/util/List;"),"V",6,8,1));
        code.addInstruction(6,new BuilderInstruction10t(Opcode.GOTO,nativeEntry));
        code.addInstruction(7,new BuilderInstruction11x(Opcode.MOVE_EXCEPTION,1));
        code.addInstruction(8,new BuilderInstruction10t(Opcode.GOTO,nativeEntry));
        code.addCatch(code.newLabelForIndex(3),code.newLabelForIndex(5),code.newLabelForIndex(7));return type;
    }
    static void reg(Instruction instruction,Opcode opcode,int register){check(instruction.getOpcode()==opcode&&instruction instanceof OneRegisterInstruction&&((OneRegisterInstruction)instruction).getRegisterA()==register,"startup register "+opcode);}
    static void invoke(Instruction instruction,String owner,String name,List<String> parameters,String result,int... registers) {
        check(instruction.getOpcode()==Opcode.INVOKE_STATIC&&instruction instanceof FiveRegisterInstruction,"startup static call");
        MethodReference reference=Instagram137ProfileReelsAudit.reference(instruction);check(reference!=null,"startup method reference");
        check(owner.equals(reference.getDefiningClass())&&name.equals(reference.getName())&&parameters.equals(reference.getParameterTypes())&&result.equals(reference.getReturnType()),"startup exact method ABI");
        FiveRegisterInstruction value=(FiveRegisterInstruction)instruction;int[] actual={value.getRegisterC(),value.getRegisterD(),value.getRegisterE(),value.getRegisterF(),value.getRegisterG()};
        check(value.getRegisterCount()==registers.length,"startup argument count");for(int i=0;i<registers.length;i++)check(registers[i]==actual[i],"startup exact argument register");
    }
    static Method restorePager(Method original,Method piko)throws Exception {
        List<Instruction> old=list(original),body=list(piko);
        check(old.size()==body.size(),"Piko pager changes no instruction count");
        reg(old.get(349),Opcode.IF_EQZ,0);check(Instagram452Audit.target(old,349)==355,"original initial-position guard and skip target");
        check(body.get(349).getOpcode()==Opcode.NOP,"Piko only removes initial-position mobile-config guard");
        MutableMethodImplementation restored=new MutableMethodImplementation(piko.getImplementation());
        restored.replaceInstruction(349,new BuilderInstruction21t(Opcode.IF_EQZ,0,restored.newLabelForIndex(355)));
        Method result=withBody(piko,restored);same(original,result,"entire pager native body, metadata, branches and handlers restored");return result;
    }
    static Method restoreStartup(Method original,Method piko)throws Exception {
        List<Instruction> body=list(piko);check(body.size()==list(original).size()+9,"exact nine-instruction Piko startup prefix");
        invoke(body.get(0),EXT,"preflightStartup",Arrays.asList("Landroid/content/Intent;","Z"),"Z",6,8);
        reg(body.get(1),Opcode.MOVE_RESULT,1);reg(body.get(2),Opcode.IF_EQZ,1);
        invoke(body.get(3),"LX/004E;","A01",Collections.singletonList("Lcom/instagram/common/session/UserSession;"),"Ljava/util/List;",7);
        reg(body.get(4),Opcode.MOVE_RESULT_OBJECT,1);
        invoke(body.get(5),EXT,"applyStartupTab",Arrays.asList("Landroid/content/Intent;","Z","Ljava/util/List;"),"V",6,8,1);
        check(body.get(6).getOpcode()==Opcode.GOTO&&body.get(8).getOpcode()==Opcode.GOTO,"startup normal and caught goto");reg(body.get(7),Opcode.MOVE_EXCEPTION,1);
        for(int i:new int[]{2,6,8})check(Instagram452Audit.target(body,i)==9,"all added paths resume native entry");
        for(int i=9;i<body.size();i++)if(body.get(i) instanceof OffsetInstruction)check(Instagram452Audit.target(body,i)>=9,"native branch cannot enter startup prefix");
        List<TryBlock<? extends ExceptionHandler>> remaining=new ArrayList<TryBlock<? extends ExceptionHandler>>();int added=0;
        for(TryBlock<? extends ExceptionHandler> block:piko.getImplementation().getTryBlocks()) {
            if(block.getStartCodeAddress()<address(body,9)) {
                check(block.getStartCodeAddress()==address(body,3)&&block.getCodeUnitCount()==address(body,5)-address(body,3),"only candidate lookup protected by added try");
                List<? extends ExceptionHandler> handlers=block.getExceptionHandlers();check(handlers.size()==1&&handlers.get(0).getExceptionType()==null&&handlers.get(0).getHandlerCodeAddress()==address(body,7),"exact added catchall");added++;
            } else {for(ExceptionHandler handler:block.getExceptionHandlers())check(handler.getHandlerCodeAddress()>=address(body,9),"no native handler enters prefix");remaining.add(block);}
        }
        check(added==1,"one added candidate lookup catchall");
        MutableMethodImplementation code=new MutableMethodImplementation(new ImmutableMethodImplementation(piko.getImplementation().getRegisterCount(),body,remaining,Collections.emptyList()));
        for(int i=0;i<9;i++)code.removeInstruction(0);
        Method result=withBody(piko,code);same(original,result,"entire startup native body, metadata, branches and handlers restored");return result;
    }
    static void reject(Map<String,ClassDef> piko,String owner,String name,int kind) {
        Map<String,ClassDef> bad=new LinkedHashMap<String,ClassDef>(piko);MutableClass type=new MutableClass(piko.get(owner));
        MutableMethod method=(MutableMethod)Instagram137ProfileReelsAudit.find(type,name);MutableMethodImplementation code=method.getImplementation();
        if(kind==0)code.replaceInstruction(348,new BuilderInstruction11x(Opcode.MOVE_RESULT,1));
        else if(kind==1)code.replaceInstruction(350,new BuilderInstruction10x(Opcode.NOP));
        else if(kind==2) {
            Reference r=((ReferenceInstruction)code.getInstructions().get(0)).getReference();
            code.replaceInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,5,8,0,0,0,r));
        } else if(kind==3)code.addCatch(code.newLabelForIndex(3),code.newLabelForIndex(6),code.newLabelForIndex(7));
        else if(kind==5) {
            // Add a genuinely overlapping native interval; the inverse must
            // not excuse it as Piko's harmless out-of-order prefix record.
            List<Instruction> body=list(method);int nativeStart=address(body,9);
            TryBlock<? extends ExceptionHandler> nativeTry=null;
            for(TryBlock<? extends ExceptionHandler> block:method.getImplementation().getTryBlocks())if(block.getStartCodeAddress()>=nativeStart){nativeTry=block;break;}
            check(nativeTry!=null,"native try available for overlap negative");int start=-1,end=-1,handler=-1;
            for(int i=0;i<body.size();i++) {
                int pc=address(body,i);if(pc==nativeTry.getStartCodeAddress())start=i;
                if(pc==nativeTry.getStartCodeAddress()+nativeTry.getCodeUnitCount())end=i;
                if(pc==nativeTry.getExceptionHandlers().get(0).getHandlerCodeAddress())handler=i;
            }
            check(start>=9&&end>start+1&&handler>=9,"native interval/handler boundaries");
            code.addCatch("Ljava/lang/RuntimeException;",code.newLabelForIndex(start+1),code.newLabelForIndex(end),code.newLabelForIndex(handler));
        }
        else code.replaceInstruction(0,new BuilderInstruction10x(Opcode.NOP));
        bad.put(owner,type);final int[] edits={0};boolean rejected=false;
        try{InstagramExternalProfilePatch.apply(bad,t->{edits[0]++;return new MutableClass(bad.get(t));},new HashMap<String,Integer>());}
        catch(IllegalStateException expected){rejected=true;}
        check(rejected&&edits[0]==0,"corrupt compatible variant rejected before first edit: "+owner+"->"+name+" #"+kind);
    }
    public static void main(String[] args)throws Exception {
        if(args.length<2||args.length>3)throw new IllegalArgumentException("Usage: Instagram137PikoContractAudit ORIGINAL_APK PIKO_APK [EMITTED_APK] (all inputs are read-only)");
        Map<String,ClassDef> original=Instagram137ProfileReelsAudit.read(args[0]),piko=Instagram137ProfileReelsAudit.read(args[1]);
        InstagramProfileReelsContract.verifyOriginal(original);InstagramProfileReelsContract.verifyOriginal(piko);
        restorePager(method(original,"LX/00M2;","A04"),method(piko,"LX/00M2;","A04"));
        restoreStartup(method(original,"LX/003Q;","A07"),method(piko,"LX/003Q;","A07"));
        for(String[] pair:new String[][]{{"Lcom/instagram/mainactivity/InstagramMainActivity;","HNv"},{"Lcom/instagram/mainactivity/InstagramMainActivity;","HNw"},{"Lcom/instagram/mainactivity/InstagramMainActivity;","A2Z"},{"LX/00M2;","A0P"},{"LX/004K;","A07"},{"LX/004K;","A09"},{"LX/004K;","A05"},{"LX/004K;","A06"},{"LX/004E;","A03"}})
            same(method(original,pair[0],pair[1]),method(piko,pair[0],pair[1]),"native selection/reset/explicit-startup parser unchanged by Piko: "+Arrays.toString(pair));
        // Same-pass Piko+Hiro presents mutable classes. Verification must neither
        // edit their instructions nor retain a removed prefix/branch in them.
        Map<String,ClassDef> mutable=new LinkedHashMap<String,ClassDef>(piko);
        for(String owner:new String[]{"LX/00M2;","LX/003Q;",EXT})mutable.put(owner,new MutableClass(piko.get(owner)));
        InstagramProfileReelsContract.verifyOriginal(mutable);
        for(String[] pair:new String[][]{{"LX/00M2;","A04"},{"LX/003Q;","A07"},{EXT,"preflightStartup"},{EXT,"applyStartupTab"}})
            same(method(piko,pair[0],pair[1]),method(mutable,pair[0],pair[1]),"contract leaves mutable Piko executable body untouched");
        Map<String,ClassDef> live=new LinkedHashMap<String,ClassDef>(piko);live.put("LX/003Q;",liveStartup(original.get("LX/003Q;")));
        Method liveMethod=method(live,"LX/003Q;","A07");String liveTries=tryShape(liveMethod);
        boolean strictRejected=false;try{OneBackPatch.instagramNavigationDigest(liveMethod);}catch(IllegalStateException expected){strictRejected=expected.getMessage().contains("exception intervals overlap");}
        check(strictRejected,"live append-catch order reproduces actual same-pass strict-digest failure");
        same(method(piko,"LX/003Q;","A07"),liveMethod,"live Piko insertion serializes to exact pinned Piko startup body");
        InstagramProfileReelsContract.verifyOriginal(live);
        check(liveTries.equals(tryShape(liveMethod)),"verification preserves original live catch-record order");
        restoreStartup(method(original,"LX/003Q;","A07"),liveMethod);
        same(method(piko,"LX/003Q;","A07"),liveMethod,"accepted live startup executable body remains unchanged");
        reject(piko,"LX/00M2;","A04",0);reject(piko,"LX/00M2;","A04",1);
        reject(piko,"LX/003Q;","A07",2);reject(piko,"LX/003Q;","A07",3);reject(piko,EXT,"preflightStartup",4);
        reject(live,"LX/003Q;","A07",2);reject(live,"LX/003Q;","A07",3);reject(live,"LX/003Q;","A07",5);
        if(args.length==3) {
            Map<String,ClassDef> emitted=Instagram137ProfileReelsAudit.read(args[2]);
            for(String[] pair:new String[][]{{"LX/00M2;","A04"},{"LX/003Q;","A07"},{EXT,"preflightStartup"},{EXT,"applyStartupTab"}})
                same(method(piko,pair[0],pair[1]),method(emitted,pair[0],pair[1]),"emitted APK preserves exact Piko navigation operation");
            restorePager(method(original,"LX/00M2;","A04"),method(emitted,"LX/00M2;","A04"));
            restoreStartup(method(original,"LX/003Q;","A07"),method(emitted,"LX/003Q;","A07"));
        }
        System.out.println("PASS Instagram137PikoContractAudit: "+checks+" assertions; exact two Piko additions independently reversed to original bytes, native Reels selection/reset preserved, serialized and actual live append-catch contracts accepted without edits, eight corrupt variants rejected before mutation; all inputs read-only, no device execution");
    }
}
