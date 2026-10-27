package app.hiro.swiftkey.patches;

import java.nio.file.*;
import java.util.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.smali.Smali;
import com.android.tools.smali.smali.SmaliOptions;

/** Execute actual pinned native constructor transformation and roundtrip its DEX. */
public final class EmojiFooterPatchProbe {
    private static int assertions;
    private static final DexFormatter FORMAT= DexFormatter.INSTANCE;
    private static void check(boolean v,String m){assertions++;if(!v)throw new AssertionError(m);}
    private static TreeMap<String,ClassDef> targets(Path archive)throws Exception{
        MultiDexContainer<? extends DexFile> all=DexFileFactory.loadDexContainer(archive.toFile(),Opcodes.forApi(26));
        TreeMap<String,ClassDef> found=new TreeMap<>();
        for(String name:all.getDexEntryNames())for(ClassDef c:all.getEntry(name).getDexFile().getClasses())
            if(c.getType().equals(EmojiFooterPatch.OWNER)||c.getType().equals(EmojiFooterPatch.SENDER))
                check(found.put(c.getType(),c)==null,"Unique native target class");
        check(found.size()==2,"Both native footer and Enter sender present");return found;
    }
    private static ClassDef replace(ClassDef owner,Method replacement){
        List<Method> methods=new ArrayList<>();int count=0;
        for(Method m:owner.getMethods())if(FORMAT.getMethodDescriptor(m).equals(FORMAT.getMethodDescriptor(replacement))){methods.add(replacement);count++;}else methods.add(m);
        check(count==1,"Single exact method replacement");
        return new ImmutableClassDef(owner.getType(),owner.getAccessFlags(),owner.getSuperclass(),owner.getInterfaces(),owner.getSourceFile(),owner.getAnnotations(),owner.getFields(),methods);
    }
    private static int findReturn(Method m){int answer=-1,index=0;for(Instruction i:m.getImplementation().getInstructions()){if(i.getOpcode()==Opcode.RETURN_VOID){check(answer<0,"One successful constructor exit");answer=index;}index++;}check(answer>=0,"Constructor returns after successful initialization");return answer;}
    private static void verify(Method original,MutableMethod patched){
        int start=findReturn(original);List<? extends Instruction> code=patched.getImplementation().getInstructions();
        check(code.get(start).getOpcode()==Opcode.MOVE_OBJECT_FROM16&&code.get(start+1).getOpcode()==Opcode.MOVE_OBJECT_FROM16,"Owner/sender copied into dead locals only at success exit");
        TwoRegisterInstruction owner=(TwoRegisterInstruction)code.get(start),sender=(TwoRegisterInstruction)code.get(start+1);
        check(owner.getRegisterA()==0&&owner.getRegisterB()==17,"Original p0 owner forwarded");
        check(sender.getRegisterA()==1&&sender.getRegisterB()==28,"Original p11 Enter sender forwarded");
        Instruction instruction=code.get(start+2);check(instruction.getOpcode()==Opcode.INVOKE_STATIC,"Install is static call");
        FiveRegisterInstruction call=(FiveRegisterInstruction)instruction;
        check(call.getRegisterCount()==2&&call.getRegisterC()==0&&call.getRegisterD()==1,"Install receives exact owner/sender");
        MethodReference ref=(MethodReference)((ReferenceInstruction)instruction).getReference();
        check(ref.getDefiningClass().equals("Lhiro/swiftkey/EmojiFooter;")&&ref.getName().equals("install")
                &&ref.getParameterTypes().equals(Arrays.asList("Lje0/c;","Lwb0/x;"))&&ref.getReturnType().equals("V"),"Install ABI matches runtime");
        check(code.get(start+3).getOpcode()==Opcode.RETURN_VOID,"Returns immediately after runtime hook");
        check(patched.getImplementation().getRegisterCount()==35,"Original register count preserved");
        MutableMethod restored=new MutableMethod(patched);for(int i=0;i<3;i++)restored.getImplementation().removeInstruction(start);
        check(MethodContract.sha256(restored).equals(MethodContract.sha256(original)),"Removing exactly three inserted instructions restores full native body and metadata");
        int installs=0,nativeTouchIndex=-1,index=0;
        for(Instruction i:code){if(i instanceof ReferenceInstruction&&((ReferenceInstruction)i).getReference() instanceof MethodReference){MethodReference r=(MethodReference)((ReferenceInstruction)i).getReference();if(r.getDefiningClass().equals("Lhiro/swiftkey/EmojiFooter;"))installs++;if(r.getName().equals("setOnTouchListener"))nativeTouchIndex=index;}index++;}
        check(installs==1,"Exactly one footer installation call");check(nativeTouchIndex>=0&&nativeTouchIndex<start,"Native delete handler initialized before layout change");
        Object ownerObject=new Object(),senderObject=new Object();Object[] regs=new Object[35];regs[17]=ownerObject;regs[28]=senderObject;
        regs[owner.getRegisterA()]=regs[owner.getRegisterB()];regs[sender.getRegisterA()]=regs[sender.getRegisterB()];
        check(regs[call.getRegisterC()]==ownerObject&&regs[call.getRegisterD()]==senderObject,"Modeled register transfer preserves identity");
    }
    public static void main(String[] args)throws Exception{
        Path source=Path.of(args[0]),report=Path.of(args[1]);Files.createDirectories(report.toAbsolutePath().getParent());
        boolean fixture=Files.isDirectory(source);Path archive=source;
        if(fixture){archive=report.resolveSibling("native-emoji-footer-fixture.dex");SmaliOptions options=new SmaliOptions();options.apiLevel=26;options.jobs=1;options.outputDexFile=archive.toString();check(Smali.assemble(options,source.toString()),"Pinned native fixture assembles");}
        TreeMap<String,ClassDef> originals=targets(archive);ClassDef controller=originals.get(EmojiFooterPatch.OWNER),sender=originals.get(EmojiFooterPatch.SENDER);
        Method[] baseline=EmojiFooterPatch.inspect(controller,sender);
        MutableMethod patched=new MutableMethod(baseline[0]);EmojiFooterPatch.applyMethod(patched);verify(baseline[0],patched);
        TreeMap<String,ClassDef> intended=new TreeMap<>(originals);intended.put(EmojiFooterPatch.OWNER,replace(controller,patched));
        DexPool pool=new DexPool(Opcodes.forApi(26));for(ClassDef c:intended.values())pool.internClass(c);
        Path output=report.resolveSibling("patched-emoji-footer.dex");FileDataStore store=new FileDataStore(output.toFile());try{pool.writeTo(store);}finally{store.close();}
        TreeMap<String,ClassDef> loaded=targets(output);int unchanged=0;
        for(ClassDef c:intended.values()){
            Map<String,Method> actual=new TreeMap<>();for(Method m:loaded.get(c.getType()).getMethods())actual.put(FORMAT.getMethodDescriptor(m),m);
            int count=0;for(Method m:c.getMethods()){Method roundtrip=actual.get(FORMAT.getMethodDescriptor(m));check(roundtrip!=null,"Every native method retained");if(m.getImplementation()!=null)check(MethodContract.sha256(m).equals(MethodContract.sha256(roundtrip)),"Serialized intended/native method body preserved");count++;if(!FORMAT.getMethodDescriptor(m).equals(FORMAT.getMethodDescriptor(patched)))unchanged++;}
            check(count==actual.size(),"No native method added or removed");
        }
        boolean rejected=false;String alreadyPatched=MethodContract.sha256(patched);try{EmojiFooterPatch.applyMethod(patched);}catch(IllegalStateException expected){rejected=true;}
        check(rejected&&alreadyPatched.equals(MethodContract.sha256(patched)),"Duplicate apply rejected without mutation");
        for(int changed=0;changed<2;changed++){
            MutableMethod drift=new MutableMethod(baseline[changed]);drift.getImplementation().addInstruction(0,new BuilderInstruction10x(Opcode.NOP));String before=MethodContract.sha256(drift);
            ClassDef first=changed==0?replace(controller,drift):controller,second=changed==1?replace(sender,drift):sender;
            rejected=false;try{EmojiFooterPatch.inspect(first,second);}catch(IllegalStateException expected){rejected=true;}
            check(rejected,"Constructor or native Enter semantic drift rejected before mutation");check(before.equals(MethodContract.sha256(drift)),"Failed preflight preserves target");
        }
        rejected=false;try{EmojiFooterPatch.inspect(null,sender);}catch(IllegalStateException expected){rejected=true;}check(rejected,"Missing native controller rejected");
        rejected=false;try{EmojiFooterPatch.inspect(controller,null);}catch(IllegalStateException expected){rejected=true;}check(rejected,"Missing native Enter sender rejected");
        String json="{\n  \"status\": \"PASSED\",\n  \"scope\": \"Exact native footer constructor transform and native Enter method preservation; no Android execution\",\n"
            +"  \"native_fixture\": "+fixture+",\n  \"native_methods\": 2,\n  \"native_methods_changed\": 1,\n  \"native_hook_sites\": 1,\n  \"inserted_runtime_calls\": 1,\n"
            +"  \"inserted_native_instructions\": 3,\n  \"assertions\": "+assertions+",\n  \"all_target_drift_rejected_before_mutation\": true,\n"
            +"  \"owner_and_native_enter_sender_forwarded\": true,\n  \"installation_after_native_delete_handler\": true,\n  \"native_enter_method_byte_identical\": true,\n"
            +"  \"existing_native_body_preserved\": true,\n  \"unchanged_other_native_methods\": "+unchanged+",\n  \"serialized_dex_roundtrip_verified\": true,\n"
            +"  \"constructor_source_method_sha256\": \""+MethodContract.sha256(baseline[0])+"\",\n  \"native_enter_method_sha256\": \""+MethodContract.sha256(baseline[1])+"\",\n"
            +"  \"device_tested\": false,\n  \"original_apk_apply_tested\": false\n}\n";
        Files.writeString(report,json);System.out.println("PASS emoji footer native hook: methods=2 changed=1 sites=1 assertions="+assertions);
    }
}
