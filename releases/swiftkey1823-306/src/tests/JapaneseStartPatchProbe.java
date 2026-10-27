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

/** Transform real native method bodies, verify exact calls and all untouched code. */
public final class JapaneseStartPatchProbe {
    private static int assertions;
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;
    private static void check(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
    private static Map<String,ClassDef> targets(Path archive)throws Exception{
        Map<String,ClassDef> result=new TreeMap<>();
        MultiDexContainer<? extends DexFile> dex=DexFileFactory.loadDexContainer(archive.toFile(),Opcodes.forApi(26));
        for(String name:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(name).getDexFile().getClasses())
            if(c.getType().equals(JapaneseStartPatch.SERVICE)||c.getType().equals(JapaneseStartPatch.LAYOUT)||c.getType().equals("Lxd0/d1;"))
                check(result.put(c.getType(),c)==null,"Unique target owner");
        check(result.size()==3,"Native target and prior Glide owners present");return result;
    }
    private static Method[] afterGlide(Map<String,ClassDef> targets,Method[] original)throws Exception{
        String[] keys={"Lxd0/d1;->onTouchEvent(Landroid/view/MotionEvent;)Z",
            JapaneseStartPatch.SERVICE+"->onStartInput(Landroid/view/inputmethod/EditorInfo;Z)V",
            JapaneseStartPatch.SERVICE+"->onFinishInput()V",JapaneseStartPatch.SERVICE+"->onFinishInputView(Z)V"};
        MutableMethod[] glide=new MutableMethod[4];
        for(ClassDef c:targets.values())for(Method m:c.getMethods())for(int i=0;i<keys.length;i++)
            if(keys[i].equals(FORMAT.getMethodDescriptor(m)))glide[i]=new MutableMethod(m);
        for(MutableMethod m:glide)check(m!=null,"Prior published Glide target present");
        java.lang.reflect.Method apply=JapaneseGlidePatch.class.getDeclaredMethod("applyToMethods",MutableMethod[].class);
        apply.setAccessible(true);apply.invoke(null,(Object)glide);
        Method[] result=original.clone();result[1]=glide[3];
        MutableMethod restored=new MutableMethod(result[1]);
        Instruction first=restored.getImplementation().getInstructions().get(0);
        check(first.getOpcode()==Opcode.INVOKE_STATIC&&((FiveRegisterInstruction)first).getRegisterCount()==0
                &&FORMAT.getReference(((ReferenceInstruction)first).getReference()).equals("Lhiro/swiftkey/JapaneseGlide;->reset()V"),
                "Actual published Glide helper added its expected reset prefix");
        restored.getImplementation().removeInstruction(0);
        check(MethodContract.sha256(restored).equals(MethodContract.sha256(original[1])),"Prior Glide change is exactly the shared lifecycle prefix");
        return result;
    }
    private static MutableMethod[] mutable(Method[] originals){MutableMethod[] result=new MutableMethod[originals.length];for(int i=0;i<result.length;i++)result[i]=new MutableMethod(originals[i]);return result;}
    private static void verify(Method original,MutableMethod patched,int target){
        int index=target==3?JapaneseStartPatch.selectionIndex(original):0;
        List<? extends Instruction> code=patched.getImplementation().getInstructions();
        Instruction ins=code.get(index);check(ins.getOpcode()==Opcode.INVOKE_STATIC,"Exact static call opcode");
        String[] signatures={
            "begin(Lcom/touchtype/KeyboardService;Landroid/view/inputmethod/EditorInfo;Z)V",
            "finish(Lcom/touchtype/KeyboardService;)V","finish(Lcom/touchtype/KeyboardService;)V",
            "select(Lfb0/i0;Lfb0/f5;Lqj0/a;)Lqj0/a;"};
        MethodReference call=(MethodReference)((ReferenceInstruction)ins).getReference();
        check(FORMAT.getMethodDescriptor(call).equals("Lhiro/swiftkey/JapaneseStart;->"+signatures[target]),"Exact helper ABI");
        FiveRegisterInstruction invoke=(FiveRegisterInstruction)ins;
        int[][] registers={{1,2,3},{1},{1},{1,2,11}};int[] want=registers[target];
        int[] actual={invoke.getRegisterC(),invoke.getRegisterD(),invoke.getRegisterE()};
        check(invoke.getRegisterCount()==want.length,"Exact argument word count");
        for(int i=0;i<want.length;i++)check(actual[i]==want[i],"Original native argument register is forwarded");
        if(target==3){
            check(code.get(index+1).getOpcode()==Opcode.MOVE_RESULT_OBJECT&&((OneRegisterInstruction)code.get(index+1)).getRegisterA()==11,"Selected layout replaces only native result p2");
            check(code.get(index-1).getOpcode()==Opcode.MOVE_RESULT_OBJECT,"Hook follows native selection getter");
        }
        MutableMethod restored=new MutableMethod(patched);
        for(int i=0;i<(target==3?2:1);i++)restored.getImplementation().removeInstruction(index);
        check(MethodContract.sha256(restored).equals(MethodContract.sha256(original)),"Removing exact inserted instructions restores all native code, branches and metadata");
        check(patched.getImplementation().getRegisterCount()==original.getImplementation().getRegisterCount(),"Native registers unchanged");
        int runtimeCalls=0;
        for(Instruction instruction:code)if(instruction instanceof ReferenceInstruction){Reference ref=((ReferenceInstruction)instruction).getReference();if(ref instanceof MethodReference&&((MethodReference)ref).getDefiningClass().equals("Lhiro/swiftkey/JapaneseStart;"))runtimeCalls++;}
        check(runtimeCalls==1,"Exactly one Japanese helper call per native method");
    }
    private static int serialize(Map<String,ClassDef> originals,MutableMethod[] patched,Path path)throws Exception{
        Map<String,Method> replacements=new TreeMap<>();for(Method m:patched)replacements.put(FORMAT.getMethodDescriptor(m),m);
        DexPool pool=new DexPool(Opcodes.forApi(26));
        for(ClassDef c:originals.values()){
            List<Method> merged=new ArrayList<>();for(Method m:c.getMethods())merged.add(replacements.getOrDefault(FORMAT.getMethodDescriptor(m),m));
            pool.internClass(new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),merged));
        }
        FileDataStore store=new FileDataStore(path.toFile());try{pool.writeTo(store);}finally{store.close();}
        Map<String,ClassDef> loaded=targets(path);int unchanged=0;
        for(ClassDef c:originals.values()){
            Map<String,Method> actual=new TreeMap<>();for(Method m:loaded.get(c.getType()).getMethods())actual.put(FORMAT.getMethodDescriptor(m),m);
            int count=0;
            for(Method original:c.getMethods()){
                String key=FORMAT.getMethodDescriptor(original);Method expected=replacements.getOrDefault(key,original);count++;
                if(expected==original)unchanged++;
                check(actual.containsKey(key),"Serialized method retained");
                if(expected.getImplementation()!=null)check(MethodContract.sha256(expected).equals(MethodContract.sha256(actual.get(key))),"Serialized method body exactly matches intended method");
            }
            check(count==actual.size(),"No native methods added or removed");
        }
        return unchanged;
    }
    public static void main(String[] args)throws Exception{
        Path source=Path.of(args[0]),report=Path.of(args[1]);Files.createDirectories(report.toAbsolutePath().getParent());
        boolean fixture=Files.isDirectory(source);Path archive=source;
        if(fixture){archive=report.resolveSibling("native-japanese-start-fixture.dex");SmaliOptions options=new SmaliOptions();options.apiLevel=26;options.jobs=1;options.outputDexFile=archive.toString();check(Smali.assemble(options,source.toString()),"Exact fixture assembly");}
        Map<String,ClassDef> targets=targets(archive);
        Method[] original=JapaneseStartPatch.inspect(targets.get(JapaneseStartPatch.SERVICE),targets.get(JapaneseStartPatch.LAYOUT));
        Method[] prior=afterGlide(targets,original);
        MutableMethod[] patched=mutable(prior);JapaneseStartPatch.applyMethods(patched);
        for(int i=0;i<patched.length;i++)verify(prior[i],patched[i],i);
        check(FORMAT.getReference(((ReferenceInstruction)patched[1].getImplementation().getInstructions().get(1)).getReference())
                .equals("Lhiro/swiftkey/JapaneseGlide;->reset()V"),"Japanese finish leaves existing Glide reset immediately following it");
        int unchanged=serialize(targets,patched,report.resolveSibling("patched-japanese-start.dex"));
        boolean rejected=false;try{JapaneseStartPatch.applyMethods(patched);}catch(IllegalStateException expected){rejected=true;}check(rejected,"Duplicate application fails closed");
        for(int changed=0;changed<original.length;changed++){
            MutableMethod[] drift=mutable(prior);drift[changed].getImplementation().addInstruction(0,new BuilderInstruction10x(Opcode.NOP));
            String[] hashes=new String[drift.length];for(int i=0;i<drift.length;i++)hashes[i]=MethodContract.sha256(drift[i]);
            rejected=false;try{JapaneseStartPatch.applyMethods(drift);}catch(IllegalStateException expected){rejected=true;}
            check(rejected,"Every native drift rejected before mutation");
            for(int i=0;i<drift.length;i++)check(hashes[i].equals(MethodContract.sha256(drift[i])),"Failed preconditions do not partially patch any method");
        }
        String json="{\n  \"status\": \"PASSED\",\n  \"scope\": \"Exact native lifecycle and layout-selection transforms; no Android execution\",\n"
                +"  \"native_fixture\": "+fixture+",\n  \"native_methods\": 4,\n  \"native_hook_sites\": 4,\n  \"runtime_entrypoints\": 3,\n  \"assertions\": "+assertions+",\n"
                +"  \"all_target_drift_rejected_before_mutation\": true,\n  \"existing_native_body_preserved\": true,\n  \"unchanged_other_native_methods\": "+unchanged+",\n"
                +"  \"published_prior_glide_transform_executed\": true,\n  \"shared_lifecycle_glide_hook_preserved\": true,\n"
                +"  \"selection_after_pending_layout_restore\": true,\n  \"native_input_type_policy_retained\": true,\n  \"serialized_dex_roundtrip_verified\": true,\n  \"device_tested\": false,\n  \"original_apk_apply_tested\": false\n}\n";
        Files.writeString(report,json);System.out.println("PASS Japanese startup native hooks: methods=4 sites=4 assertions="+assertions);
    }
}
