package app.hiro.oneback.patches;

import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t;
import com.android.tools.smali.dexlib2.immutable.*;

/** Read-only inverses of two verified Piko 3.11.0-dev.3 navigation additions.
 * The canonical native body must still match its complete original digest.
 * Neither this inverse nor the original branch is written into the app. */
final class InstagramProfileReelsPikoContract {
    private InstagramProfileReelsPikoContract() {}
    private static final String EXT="Lapp/morphe/extension/instagram/patches/navigation/NavigationBarPatch;";
    private static void require(boolean value,String why) {
        if(!value)throw new IllegalStateException("Instagram452 Piko navigation contract changed: "+why);
    }
    private static List<Instruction> instructions(Method method) {
        List<Instruction> list=new ArrayList<Instruction>();for(Instruction i:method.getImplementation().getInstructions())list.add(i);return list;
    }
    private static Method withBody(Method method,MethodImplementation body) {
        return new ImmutableMethod(method.getDefiningClass(),method.getName(),method.getParameters(),method.getReturnType(),
            method.getAccessFlags(),method.getAnnotations(),method.getHiddenApiRestrictions(),body);
    }
    static boolean hasStartupPrefix(Method method) {
        if(!"LX/003Q;".equals(method.getDefiningClass())||!"A07".equals(method.getName())
                ||method.getImplementation()==null)return false;
        Iterator<? extends Instruction> instructions=method.getImplementation().getInstructions().iterator();
        if(!instructions.hasNext())return false;Instruction first=instructions.next();
        if(first.getOpcode()!=Opcode.INVOKE_STATIC||!(first instanceof ReferenceInstruction))return false;
        Reference reference=((ReferenceInstruction)first).getReference();
        if(!(reference instanceof MethodReference))return false;MethodReference call=(MethodReference)reference;
        return EXT.equals(call.getDefiningClass())&&"preflightStartup".equals(call.getName());
    }
    static Method original(Map<String,ClassDef> classes,Method method) {
        if("LX/00M2;".equals(method.getDefiningClass())&&"A04".equals(method.getName())) {
            List<Instruction> list=instructions(method);
            require(list.size()>355&&list.get(349).getOpcode()==Opcode.NOP,"initial pager positioning guard");
            MutableMethodImplementation code=new MutableMethodImplementation(method.getImplementation());
            // Piko always initializes the requested reordered pager index.
            // The original target skips only this initial positioning block;
            // reset, selected-tab arguments and subsequent selection stay native.
            code.replaceInstruction(349,new BuilderInstruction21t(Opcode.IF_EQZ,0,code.newLabelForIndex(355)));
            return withBody(method,code);
        }
        if("LX/003Q;".equals(method.getDefiningClass())&&"A07".equals(method.getName()))return startup(classes,method);
        throw new IllegalStateException("Instagram452 unknown native navigation body: "+method.getDefiningClass()+"->"+method.getName());
    }
    private static int address(List<Instruction> list,int index) {
        int pc=0;for(int i=0;i<index;i++)pc+=list.get(i).getCodeUnits();return pc;
    }
    private static int destination(List<Instruction> list,int index) {
        return address(list,index)+((OffsetInstruction)list.get(index)).getCodeOffset();
    }
    private static void register(Instruction value,Opcode opcode,int register) {
        require(value.getOpcode()==opcode&&value instanceof OneRegisterInstruction
            &&((OneRegisterInstruction)value).getRegisterA()==register,"startup register "+opcode);
    }
    private static void invoke(Instruction value,String owner,String name,List<String> args,String returns,int... expected) {
        require(value.getOpcode()==Opcode.INVOKE_STATIC&&value instanceof FiveRegisterInstruction&&value instanceof ReferenceInstruction,"startup static call");
        Reference reference=((ReferenceInstruction)value).getReference();require(reference instanceof MethodReference,"startup method reference");
        MethodReference method=(MethodReference)reference;FiveRegisterInstruction registers=(FiveRegisterInstruction)value;
        require(owner.equals(method.getDefiningClass())&&name.equals(method.getName())&&args.equals(method.getParameterTypes())
            &&returns.equals(method.getReturnType())&&registers.getRegisterCount()==expected.length,"startup exact call ABI");
        int[] actual={registers.getRegisterC(),registers.getRegisterD(),registers.getRegisterE(),registers.getRegisterF(),registers.getRegisterG()};
        for(int i=0;i<expected.length;i++)require(actual[i]==expected[i],"startup exact call operand");
    }
    private static void extension(Map<String,ClassDef> classes,String name,List<String> args,String returns,int registers,String digest) {
        ClassDef type=classes.get(EXT);require(type!=null&&type.getAccessFlags()==17,"Piko navigation extension");
        for(Method method:type.getMethods())if(OneBackPatch.signature(method,name,args,returns)) {
            require(method.getAccessFlags()==9&&method.getImplementation()!=null
                &&method.getImplementation().getRegisterCount()==registers
                &&digest.equals(OneBackPatch.instagramNavigationDigest(method)),"Piko "+name+" body");return;
        }
        throw new IllegalStateException("Instagram452 Piko startup extension method missing: "+name);
    }
    private static Method startup(Map<String,ClassDef> classes,Method method) {
        List<Instruction> list=instructions(method);require(list.size()>9,"startup prefix");
        invoke(list.get(0),EXT,"preflightStartup",Arrays.asList("Landroid/content/Intent;","Z"),"Z",6,8);
        register(list.get(1),Opcode.MOVE_RESULT,1);register(list.get(2),Opcode.IF_EQZ,1);
        invoke(list.get(3),"LX/004E;","A01",Collections.singletonList("Lcom/instagram/common/session/UserSession;"),"Ljava/util/List;",7);
        register(list.get(4),Opcode.MOVE_RESULT_OBJECT,1);
        invoke(list.get(5),EXT,"applyStartupTab",Arrays.asList("Landroid/content/Intent;","Z","Ljava/util/List;"),"V",6,8,1);
        require(list.get(6).getOpcode()==Opcode.GOTO&&list.get(8).getOpcode()==Opcode.GOTO,"startup normal and caught continuation");
        register(list.get(7),Opcode.MOVE_EXCEPTION,1);
        int nativeStart=address(list,9);
        for(int i:new int[]{2,6,8})require(destination(list,i)==nativeStart,"startup continuation reaches unchanged native entry");
        // The untouched native body may not jump into the added prefix.
        for(int i=9;i<list.size();i++)if(list.get(i) instanceof OffsetInstruction)
            require(destination(list,i)>=nativeStart,"native branch enters Piko startup prefix");
        List<TryBlock<? extends ExceptionHandler>> nativeTries=new ArrayList<TryBlock<? extends ExceptionHandler>>();int injected=0;
        for(TryBlock<? extends ExceptionHandler> block:method.getImplementation().getTryBlocks()) {
            if(block.getStartCodeAddress()<nativeStart) {
                require(block.getStartCodeAddress()==address(list,3)
                    &&block.getCodeUnitCount()==address(list,5)-address(list,3),"startup exact candidate lookup try interval");
                List<? extends ExceptionHandler> handlers=block.getExceptionHandlers();
                require(handlers.size()==1&&handlers.get(0).getExceptionType()==null
                    &&handlers.get(0).getHandlerCodeAddress()==address(list,7),"startup exact catchall");injected++;
            } else {
                for(ExceptionHandler handler:block.getExceptionHandlers())require(handler.getHandlerCodeAddress()>=nativeStart,"native handler enters startup prefix");
                nativeTries.add(block);
            }
        }
        require(injected==1,"one startup lookup catchall");
        // These known extension bodies leave an explicit STARTUP_TAB unchanged.
        // Our fresh handoff supplies CLIPS there before this native entry.
        extension(classes,"preflightStartup",Arrays.asList("Landroid/content/Intent;","Z"),"Z",3,
            "3b422c28fe06006c83c985e8db5bb7b1734d15b9dd70c3e0629c32a60c3ef1ee");
        extension(classes,"applyStartupTab",Arrays.asList("Landroid/content/Intent;","Z","Ljava/util/List;"),"V",8,
            "f5568896edf9bcc90b7c2a0716e4aacdfe7426c46c686f524e123d21db0cace0");
        MethodImplementation filtered=new ImmutableMethodImplementation(method.getImplementation().getRegisterCount(),list,nativeTries,Collections.emptyList());
        MutableMethodImplementation code=new MutableMethodImplementation(filtered);
        for(int i=0;i<9;i++)code.removeInstruction(0);
        return withBody(method,code);
    }
}
