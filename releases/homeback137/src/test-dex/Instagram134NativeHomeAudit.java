import java.io.*;
import java.nio.file.*;
import java.util.*;
import app.hiro.oneback.patches.OneBackPatch;
import app.hiro.oneback.patches.InstagramNativeHomePatch;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.pool.*;
import com.android.tools.smali.dexlib2.writer.io.*;

/** Independent executable-body inverse for the two new native Home hooks.
 * It does not call a production inverse or generate expected native bodies.
 * Expected instructions/branches/handlers always come from the original APK.
 */
public final class Instagram134NativeHomeAudit {
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
    static String key(Method method){return method.getName()+method.getParameterTypes()+method.getReturnType();}
    static int target(List<Instruction> body,int index) {
        int address=0;for(int i=0;i<index;i++)address+=body.get(i).getCodeUnits();
        address+=((OffsetInstruction)body.get(index)).getCodeOffset();int pc=0;
        for(int i=0;i<body.size();i++){if(pc==address)return i;pc+=body.get(i).getCodeUnits();}
        throw new AssertionError("branch not at instruction boundary");
    }
    static void call(Instruction instruction,String name,String result,int first,int second,int count) {
        check(instruction.getOpcode()==Opcode.INVOKE_STATIC&&instruction instanceof FiveRegisterInstruction&&instruction instanceof ReferenceInstruction,"static runtime call shape");
        FiveRegisterInstruction registers=(FiveRegisterInstruction)instruction;Object reference=((ReferenceInstruction)instruction).getReference();
        check(reference instanceof MethodReference,"method reference");MethodReference method=(MethodReference)reference;
        check(OneBackPatch.RUNTIME.equals(method.getDefiningClass())&&name.equals(method.getName())&&result.equals(method.getReturnType()),"exact runtime owner/name/result");
        check(method.getParameterTypes().equals(count==2?Arrays.asList("Ljava/lang/Object;","Z"):Collections.singletonList("Ljava/lang/Object;")),"exact runtime parameter types");
        check(registers.getRegisterCount()==count&&registers.getRegisterC()==first&&(count!=2||registers.getRegisterD()==second),"exact receiver and native boolean registers");
    }
    static void register(Instruction instruction,Opcode opcode,int register) {
        check(instruction.getOpcode()==opcode&&((OneRegisterInstruction)instruction).getRegisterA()==register,"native result register preserved");
    }
    /** The independent inverse is also used by the whole rebuilt-APK audit. */
    public static Method restored(Method method) {
        List<Instruction> instructions=list(method);MutableMethodImplementation code=new MutableMethodImplementation(method.getImplementation());
        if("onBackPressed".equals(method.getName())) {
            call(instructions.get(0),"beginInstagramNativeHomeBack","V",10,0,1);
            // Original return indices 6/65 shift by one entry hook and by the
            // first result replacement's two extra instructions.
            for(int index:new int[]{7,68}) {
                call(instructions.get(index),"finishInstagramNativeHomeBack","Z",10,0,2);
                register(instructions.get(index+1),Opcode.MOVE_RESULT,0);register(instructions.get(index+2),Opcode.RETURN,0);
            }
            for(int index:new int[]{68,7}) {
                code.replaceInstruction(index,new BuilderInstruction11x(Opcode.RETURN,0));
                code.removeInstruction(index+1);code.removeInstruction(index+1);
            }
            code.removeInstruction(0);
        } else if("F6u".equals(method.getName())) {
            call(instructions.get(42),"tryInstagramNativeHomePrompt","Z",6,0,1);
            register(instructions.get(43),Opcode.MOVE_RESULT,0);register(instructions.get(44),Opcode.IF_EQZ,0);register(instructions.get(45),Opcode.RETURN,0);
            check(target(instructions,44)==46,"false hook reaches original prompt creation");
            check(instructions.get(46).getOpcode()==Opcode.NEW_INSTANCE&&((OneRegisterInstruction)instructions.get(46)).getRegisterA()==4
                    &&"LX/0BJz;".equals(((ReferenceInstruction)instructions.get(46)).getReference().toString()),"exact original prompt object remains");
            // Both original prompt-entry paths visit the gate: disabled
            // gesture check at 37 and the checked path's fallthrough at 41.
            for(int i=0;i<instructions.size();i++)if(i!=44&&instructions.get(i) instanceof OffsetInstruction) {
                int destination=target(instructions,i);
                if(i==37){register(instructions.get(i),Opcode.IF_EQZ,0);check(destination==42,"original gesture-check-disabled branch visits gate");}
                else check(destination<42||destination>46,"no other original branch bypasses or enters prompt gate");
            }
            for(int i=0;i<4;i++)code.removeInstruction(42);
        } else throw new IllegalArgumentException("Not a native Home hook method");
        return new ImmutableMethod(method.getDefiningClass(),method.getName(),method.getParameters(),method.getReturnType(),
                method.getAccessFlags(),method.getAnnotations(),method.getHiddenApiRestrictions(),code);
    }
    static void compare(ClassDef original,ClassDef rebuilt)throws Exception {
        Map<String,Method> prior=new LinkedHashMap<String,Method>();for(Method method:original.getMethods())prior.put(key(method),method);
        int touched=0;
        for(Method method:rebuilt.getMethods()) {
            Method old=prior.remove(key(method));check(old!=null,"no unexpected Home method");
            boolean hook=method.getName().equals("onBackPressed")||method.getName().equals("F6u");
            Method value=hook?restored(method):method;if(hook)touched++;
            check(Arrays.equals(Instagram452Audit.bytes(old),Instagram452Audit.bytes(value)),"original executable Home body preserved: "+method.getName());
        }
        check(prior.isEmpty()&&touched==2,"exactly two original Home methods hooked");
    }
    static void rejectBeforeMutation(Map<String,ClassDef> original,String owner,String name,int index,Instruction replacement) {
        Map<String,ClassDef> bad=new LinkedHashMap<String,ClassDef>(original);MutableClass type=new MutableClass(original.get(owner));
        ((MutableMethod)find(type,name)).getImplementation().replaceInstruction(index,(BuilderInstruction)replacement);bad.put(owner,type);
        final int[] edits={0};boolean rejected=false;
        try{InstagramNativeHomePatch.apply(bad,t->{edits[0]++;return new MutableClass(bad.get(t));},new HashMap<String,Integer>());}
        catch(IllegalStateException expected){rejected=true;}
        check(rejected&&edits[0]==0,"mutated native body rejected before first edit: "+owner+"->"+name);
    }
    public static void main(String[]args)throws Exception {
        Map<String,ClassDef> original=read(args[0]);InstagramNativeHomePatch.verifyOriginal(original);
        Map<String,MutableClass> changes=new LinkedHashMap<String,MutableClass>();Map<String,Integer> counts=new LinkedHashMap<String,Integer>();
        InstagramNativeHomePatch.apply(original,t->changes.computeIfAbsent(t,k->new MutableClass(original.get(k))),counts);
        check(changes.size()==1&&changes.containsKey(InstagramNativeHomePatch.HOME),"one native class changed");
        check(counts.get("instagram_452_native_home_begin")==1&&counts.get("instagram_452_native_home_result")==2
                &&counts.get("instagram_452_native_home_prompt")==1&&counts.size()==3,"exact hook counts");
        MemoryDataStore store=new MemoryDataStore();DexPool.writeTo(store,new ImmutableDexFile(Opcodes.getDefault(),changes.values()));
        Files.write(Paths.get(args[1]),Arrays.copyOf(store.getBuffer(),store.getSize()));store.close();
        Map<String,ClassDef> emitted=read(args[1]);compare(original.get(InstagramNativeHomePatch.HOME),emitted.get(InstagramNativeHomePatch.HOME));
        rejectBeforeMutation(original,InstagramNativeHomePatch.HOME,"onBackPressed",6,new BuilderInstruction11x(Opcode.RETURN,1));
        rejectBeforeMutation(original,InstagramNativeHomePatch.HOME,"F6u",41,new BuilderInstruction21t(Opcode.IF_EQZ,0,
                new MutableMethodImplementation(find(original.get(InstagramNativeHomePatch.HOME),"F6u").getImplementation()).newLabelForIndex(91)));
        rejectBeforeMutation(original,"LX/00ZX;","A03",14,new BuilderInstruction11x(Opcode.RETURN,2));
        // A corrupt emitted hook operand must not pass the independent inverse.
        MutableClass corrupt=new MutableClass(emitted.get(InstagramNativeHomePatch.HOME));MutableMethod prompt=(MutableMethod)find(corrupt,"F6u");
        Reference ref=((ReferenceInstruction)prompt.getImplementation().getInstructions().get(42)).getReference();
        prompt.getImplementation().replaceInstruction(42,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,5,0,0,0,0,ref));
        boolean refused=false;try{restored(prompt);}catch(AssertionError expected){refused=true;}check(refused,"wrong Home receiver rejected by independent inverse");
        if(args.length>2)compare(original.get(InstagramNativeHomePatch.HOME),read(args[2]).get(InstagramNativeHomePatch.HOME));
        System.out.println("PASS Instagram134NativeHomeAudit: "+checks+" assertions; both native executable bodies restored byte-exact, unrelated Home methods unchanged, three changed-body negatives rejected before mutation; static DEX validation, no device execution");
    }
}
