package app.hiro.swiftkey.patches;

import java.nio.file.*;
import java.util.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.smali.Smali;
import com.android.tools.smali.smali.SmaliOptions;

/** Real native factory transforms plus modeled helper-null/factory-result dispatch. */
public final class PunctuationPatchProbe {
    private static int assertions;
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;
    private static void check(boolean condition,String message){assertions++;if(!condition)throw new AssertionError(message);}
    private static MutableMethod[] mutable(Method[] originals){MutableMethod[] result=new MutableMethod[originals.length];for(int i=0;i<result.length;i++)result[i]=new MutableMethod(originals[i]);return result;}
    private static ClassDef target(Path archive)throws Exception{
        MultiDexContainer<? extends DexFile> dex=DexFileFactory.loadDexContainer(archive.toFile(),Opcodes.forApi(26));ClassDef result=null;
        for(String name:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(name).getDexFile().getClasses())if(c.getType().equals(PunctuationPatch.OWNER)){check(result==null,"Unique native factory");result=c;}
        check(result!=null,"Native factory present");return result;
    }
    private static MethodReference reference(Instruction ins){return (MethodReference)((ReferenceInstruction)ins).getReference();}
    private static void verifyPrefix(Method original,MutableMethod patched,int target){
        List<? extends Instruction> code=patched.getImplementation().getInstructions();
        Opcode[] ops={Opcode.INVOKE_STATIC,Opcode.MOVE_RESULT_OBJECT,Opcode.IF_EQZ,Opcode.RETURN_OBJECT,Opcode.NOP};
        for(int i=0;i<ops.length;i++)check(code.get(i).getOpcode()==ops[i],"Only the exact nullable helper dispatch prefix is added");
        MethodReference call=reference(code.get(0));
        check(call.getDefiningClass().equals("Lhiro/swiftkey/PunctuationKeys;")&&call.getName().equals(target==0?"periodPopup":"longVowelPopup")
                &&call.getParameterTypes().equals(Arrays.asList("Lfc0/s1;","Lfc0/m1;","Lfc0/v1;"))
                &&call.getReturnType().equals(target==0?"Lfb0/f2;":"Lxc0/a;"),"Exact helper ABI");
        FiveRegisterInstruction invoke=(FiveRegisterInstruction)code.get(0);
        int[] expected=target==0?new int[]{4,8,9}:new int[]{5,6,7};
        check(invoke.getRegisterCount()==3&&invoke.getRegisterC()==expected[0]&&invoke.getRegisterD()==expected[1]&&invoke.getRegisterE()==expected[2],"Original factory, geometry and key definition are forwarded");
        for(int i:new int[]{1,2,3})check(((OneRegisterInstruction)code.get(i)).getRegisterA()==0,"Only scratch local v0 is used");
        int destination=code.get(0).getCodeUnits()+code.get(1).getCodeUnits()+((OffsetInstruction)code.get(2)).getCodeOffset();
        int address=0;int index=-1;for(int i=0;i<code.size();i++){if(address==destination)index=i;address+=code.get(i).getCodeUnits();}
        check(index==4,"Null result targets prefix exit NOP, then complete original body");
        check(patched.getImplementation().getRegisterCount()==original.getImplementation().getRegisterCount(),"Native register count unchanged");
        MutableMethod restored=new MutableMethod(patched);for(int i=0;i<5;i++)restored.getImplementation().removeInstruction(0);
        check(MethodContract.sha256(restored).equals(MethodContract.sha256(original)),"Removing only prefix restores every native instruction, branch, exception and metadata");
    }
    private static void simulate(MutableMethod method,int target,boolean handled){
        List<? extends Instruction> code=method.getImplementation().getInstructions();
        int[] regs=new int[method.getImplementation().getRegisterCount()];for(int i=0;i<regs.length;i++)regs[i]=100+i;
        int[] before=regs.clone();int pc=0,calls=0;boolean returned=false,nativeReached=false;
        for(int steps=0;steps<12;steps++){
            if(pc==5){nativeReached=true;break;}
            Instruction ins=code.get(pc);switch(ins.getOpcode()){
            case INVOKE_STATIC:{calls++;FiveRegisterInstruction call=(FiveRegisterInstruction)ins;
                int[] expected=target==0?new int[]{4,8,9}:new int[]{5,6,7};
                check(regs[call.getRegisterC()]==before[expected[0]]&&regs[call.getRegisterD()]==before[expected[1]]&&regs[call.getRegisterE()]==before[expected[2]],"Helper sees original argument identities");pc++;break;}
            case MOVE_RESULT_OBJECT:regs[((OneRegisterInstruction)ins).getRegisterA()]=handled?777:0;pc++;break;
            case IF_EQZ:pc=regs[((OneRegisterInstruction)ins).getRegisterA()]==0?4:3;break;
            case RETURN_OBJECT:check(regs[((OneRegisterInstruction)ins).getRegisterA()]==777,"Handled path returns exact helper object");returned=true;break;
            case NOP:pc++;break;
            default:throw new AssertionError("Unexpected prefix instruction");
            }
            if(returned)break;
        }
        check(calls==1,"Helper called exactly once");check(returned==handled&&nativeReached!=handled,"Non-null returns; null continues native");
        for(int i=1;i<regs.length;i++)check(regs[i]==before[i],"All native parameters and other locals retained");
    }
    private static int serializeAndVerify(ClassDef nativeClass,MutableMethod[] patched,Path output)throws Exception{
        Map<String,Method> replacements=new HashMap<>();for(Method m:patched)replacements.put(FORMAT.getMethodDescriptor(m),m);
        List<Method> merged=new ArrayList<>();for(Method m:nativeClass.getMethods())merged.add(replacements.containsKey(FORMAT.getMethodDescriptor(m))?replacements.get(FORMAT.getMethodDescriptor(m)):m);
        ClassDef changed=new ImmutableClassDef(nativeClass.getType(),nativeClass.getAccessFlags(),nativeClass.getSuperclass(),nativeClass.getInterfaces(),nativeClass.getSourceFile(),nativeClass.getAnnotations(),nativeClass.getFields(),merged);
        DexPool pool=new DexPool(Opcodes.forApi(26));pool.internClass(changed);FileDataStore store=new FileDataStore(output.toFile());try{pool.writeTo(store);}finally{store.close();}
        ClassDef restored=target(output);Map<String,Method> actual=new HashMap<>();for(Method m:restored.getMethods())actual.put(FORMAT.getMethodDescriptor(m),m);
        int unchanged=0;
        for(Method m:nativeClass.getMethods()){
            String key=FORMAT.getMethodDescriptor(m);Method expected=replacements.get(key);if(expected==null){expected=m;unchanged++;}
            Method result=actual.get(key);check(result!=null,"Serialized method retained");
            if(expected.getImplementation()!=null)check(MethodContract.sha256(expected).equals(MethodContract.sha256(result)),"Serialized body identical to intended method");
        }
        check(actual.size()==merged.size(),"No native method added or removed");return unchanged;
    }
    public static void main(String[] args)throws Exception{
        Path source=Path.of(args[0]),report=Path.of(args[1]);Files.createDirectories(report.toAbsolutePath().getParent());boolean fixture=Files.isDirectory(source);Path archive=source;
        if(fixture){archive=report.resolveSibling("native-punctuation-hook-fixture.dex");SmaliOptions options=new SmaliOptions();options.apiLevel=26;options.jobs=1;options.outputDexFile=archive.toString();check(Smali.assemble(options,source.toString()),"Exact fixture assembly");}
        ClassDef nativeClass=target(archive);Method[] original=PunctuationPatch.inspect(nativeClass);MutableMethod[] patched=mutable(original);PunctuationPatch.applyMethods(patched);
        for(int i=0;i<patched.length;i++){verifyPrefix(original[i],patched[i],i);simulate(patched[i],i,false);simulate(patched[i],i,true);}
        int untouched=serializeAndVerify(nativeClass,patched,report.resolveSibling("patched-punctuation-factory.dex"));
        boolean rejected=false;try{PunctuationPatch.applyMethods(patched);}catch(IllegalStateException expected){rejected=true;}check(rejected,"Duplicate application rejected");
        for(int changed=0;changed<original.length;changed++){
            MutableMethod[] drift=mutable(original);drift[changed].getImplementation().addInstruction(0,new BuilderInstruction10x(Opcode.NOP));String[] hashes=new String[drift.length];for(int i=0;i<drift.length;i++)hashes[i]=MethodContract.sha256(drift[i]);
            rejected=false;try{PunctuationPatch.applyMethods(drift);}catch(IllegalStateException expected){rejected=true;}check(rejected,"Any drift rejected before changes");
            for(int i=0;i<drift.length;i++)check(hashes[i].equals(MethodContract.sha256(drift[i])),"No method touched on failed precondition");
        }
        String json="{\n  \"status\": \"PASSED\",\n  \"scope\": \"Actual native alternate factories transformed; modeled nullable helper dispatch, not Android execution\",\n"
            +"  \"native_fixture\": "+fixture+",\n  \"native_methods\": 2,\n  \"inserted_runtime_calls\": 2,\n  \"dispatch_scenarios\": 4,\n  \"assertions\": "+assertions+",\n"
            +"  \"null_result_falls_through_to_native\": true,\n  \"non_null_result_returned_unchanged\": true,\n  \"all_target_drift_rejected_before_mutation\": true,\n"
            +"  \"existing_native_body_preserved\": true,\n  \"unchanged_other_factory_methods\": "+untouched+",\n  \"normal_tap_factory_methods_changed\": false,\n"
            +"  \"serialized_dex_roundtrip_verified\": true,\n  \"device_tested\": false,\n  \"original_apk_apply_tested\": false\n}\n";
        Files.writeString(report,json);System.out.println("PASS punctuation native hooks:methods=2 dispatch=4 assertions="+assertions);
    }
}
