import java.io.File;
import java.util.*;
import app.hiro.oneback.patches.InstagramInitialProfileContract;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** Tests the initial-profile contract against the actual original APK.
 * No native hook is added by this contract. Optional rebuilt APK comparison
 * serializes its bodies independently and compares them with the original. */
public final class Instagram135InitialProfileAudit {
    private static final String[][] TARGETS={
        {"LX/01PZ;","A00"},
        {"Lcom/instagram/mainactivity/InstagramMainActivity;","BwK"},
        {"Landroidx/fragment/app/FragmentActivity;","A1Z"},
        {"LX/00fk;","A0O"}
    };
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static Map<String,ClassDef> read(String path)throws Exception {
        Map<String,ClassDef> result=new LinkedHashMap<String,ClassDef>();
        MultiDexContainer<? extends DexFile> dex=DexFileFactory.loadDexContainer(new File(path),Opcodes.getDefault());
        for(String entry:dex.getDexEntryNames())for(ClassDef type:dex.getEntry(entry).getDexFile().getClasses())result.put(type.getType(),type);
        return result;
    }
    static Method find(ClassDef type,String name){for(Method method:type.getMethods())if(name.equals(method.getName()))return method;throw new AssertionError(name);}
    static List<Instruction> list(Method method){List<Instruction> result=new ArrayList<Instruction>();for(Instruction i:method.getImplementation().getInstructions())result.add(i);return result;}
    static String reference(Instruction instruction){return instruction instanceof ReferenceInstruction?((ReferenceInstruction)instruction).getReference().toString():"";}
    static int target(List<Instruction> body,int index) {
        int address=0;for(int i=0;i<index;i++)address+=body.get(i).getCodeUnits();
        address+=((OffsetInstruction)body.get(index)).getCodeOffset();int pc=0;
        for(int i=0;i<body.size();i++){if(pc==address)return i;pc+=body.get(i).getCodeUnits();}
        throw new AssertionError("non-boundary native branch");
    }
    static void routeProof(Map<String,ClassDef> original) {
        List<Instruction> body=list(find(original.get(TARGETS[0][0]),"A00"));int selected=-1,root=-1;
        for(int i=0;i<body.size();i++) {
            if(reference(body.get(i)).equals("LX/0IaN;->BwK()LX/00fk;"))selected=i;
            if(reference(body.get(i)).equals("Landroidx/fragment/app/FragmentActivity;->A1Z()LX/00fk;"))root=i;
        }
        check(selected>=0&&root==selected+3,"BwK result immediately decides root fallback");
        Instruction branch=body.get(selected+2);
        check(branch.getOpcode()==Opcode.IF_NEZ&&((OneRegisterInstruction)branch).getRegisterA()==0,"nonnull selected manager skips root lookup");
        int returned=target(body,selected+2);
        check(body.get(returned).getOpcode()==Opcode.RETURN_OBJECT&&((OneRegisterInstruction)body.get(returned)).getRegisterA()==0,"selected and fallback managers share native return");
        check(((FiveRegisterInstruction)body.get(root)).getRegisterC()==1,"fallback uses the same Activity");
        List<Instruction> selectedBody=list(find(original.get(TARGETS[1][0]),"BwK"));
        boolean nullValue=false,nullableReturn=false;
        for(Instruction i:selectedBody) {
            if(i.getOpcode()==Opcode.CONST_4&&((OneRegisterInstruction)i).getRegisterA()==2&&((NarrowLiteralInstruction)i).getNarrowLiteral()==0)nullValue=true;
            if(i.getOpcode()==Opcode.RETURN_OBJECT&&((OneRegisterInstruction)i).getRegisterA()==2)nullableReturn=true;
        }
        check(nullValue&&nullableReturn,"Main manager selection has an explicit null result");
    }
    static void readOnlyGetters(Map<String,ClassDef> original) {
        Set<String> allowed=new HashSet<String>(Arrays.asList(
            "Ljava/util/AbstractCollection;->size()I","Ljava/util/AbstractList;->get(I)Ljava/lang/Object;",
            "Ljava/util/AbstractMap;->values()Ljava/util/Collection;","Ljava/util/Collection;->iterator()Ljava/util/Iterator;",
            "Ljava/util/Iterator;->hasNext()Z","Ljava/util/Iterator;->next()Ljava/lang/Object;"));
        for(int x=2;x<TARGETS.length;x++)for(Instruction i:list(find(original.get(TARGETS[x][0]),TARGETS[x][1]))) {
            String opcode=i.getOpcode().name();
            check(!opcode.startsWith("IPUT")&&!opcode.startsWith("SPUT")&&!opcode.startsWith("APUT")&&!opcode.startsWith("NEW_"),"root getter never writes or creates app state");
            if(opcode.startsWith("INVOKE"))check(allowed.contains(reference(i)),"root getter calls only collection reads");
        }
    }
    static void rejectMutation(Map<String,ClassDef> original,int which)throws Exception {
        String owner=TARGETS[which][0],name=TARGETS[which][1];
        Map<String,ClassDef> changed=new LinkedHashMap<String,ClassDef>(original);
        MutableClass type=new MutableClass(original.get(owner));MutableMethod method=(MutableMethod)find(type,name);
        MutableMethodImplementation code=method.getImplementation();boolean edited=false;
        for(int i=0;i<code.getInstructions().size();i++) {
            BuilderInstruction old=code.getInstructions().get(i);Opcode opcode=old.getOpcode();
            if(which==0&&opcode==Opcode.IF_NEZ||which==1&&opcode==Opcode.IF_EQZ) {
                BuilderInstruction21t branch=(BuilderInstruction21t)old;
                code.replaceInstruction(i,new BuilderInstruction21t(opcode==Opcode.IF_NEZ?Opcode.IF_EQZ:Opcode.IF_NEZ,branch.getRegisterA(),branch.getTarget()));edited=true;break;
            }
            if(which==2&&opcode==Opcode.RETURN_OBJECT) {
                code.replaceInstruction(i,new BuilderInstruction11x(Opcode.RETURN_OBJECT,1));edited=true;break;
            }
            if(which==3&&opcode==Opcode.IF_NE) {
                BuilderInstruction22t branch=(BuilderInstruction22t)old;
                code.replaceInstruction(i,new BuilderInstruction22t(Opcode.IF_EQ,branch.getRegisterA(),branch.getRegisterB(),branch.getTarget()));edited=true;break;
            }
        }
        check(edited,"negative mutation reached the intended native decision");changed.put(owner,type);
        boolean rejected=false;try{InstagramInitialProfileContract.verifyOriginal(changed);}catch(IllegalStateException expected){rejected=true;}
        check(rejected,"changed native decision rejected: "+owner+"->"+name);
    }
    public static void main(String[] args)throws Exception {
        if(args.length<1||args.length>2)throw new IllegalArgumentException("ORIGINAL_APK [REBUILT_APK]");
        Map<String,ClassDef> original=read(args[0]);InstagramInitialProfileContract.verifyOriginal(original);
        check(true,"actual original APK satisfies the contract");routeProof(original);readOnlyGetters(original);
        for(int i=0;i<TARGETS.length;i++)rejectMutation(original,i);
        if(args.length==2) {
            Map<String,ClassDef> rebuilt=read(args[1]);InstagramInitialProfileContract.verifyOriginal(rebuilt);
            for(String[] target:TARGETS)check(Arrays.equals(Instagram452Audit.bytes(find(original.get(target[0]),target[1])),
                Instagram452Audit.bytes(find(rebuilt.get(target[0]),target[1]))),"native initial-profile body byte-exact: "+target[0]+"->"+target[1]);
        }
        System.out.println("PASS Instagram135InitialProfileAudit: "+checks+" assertions; native root fallback and read-only selection verified; four mutated-body negatives rejected; static DEX validation, no device execution");
    }
}
