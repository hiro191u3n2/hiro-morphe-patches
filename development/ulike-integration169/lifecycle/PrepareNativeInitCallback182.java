import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** One disabled private callback observer; no init success is promoted to restoration. */
public final class PrepareNativeInitCallback182 {
    static final String OWNER="Lcom/ss/android/medialib/RecordInvoker;";
    static final String CALLBACK=OWNER+"->onNativeCallback_Init(I)V";
    static final String CALLBACK_SHA="9293eb56a7330b0d64008948b5710f0c088334878ed7a874d06667724883eff5";
    static final String INIT=OWNER+"->initBeautyPlay(IILjava/lang/String;IILjava/lang/String;IZZZ)I";
    static final String INIT_SHA="8dc53066a33d64e75cd562ebbaae682d5831f482d020a5f011a8f898a897c95e";
    static final String HOOK="Lcom/hiro/ulike/composer/NativeComposerHooks;";
    static Method with(Method m,MethodImplementation body){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),body);}
    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("STOCK_APK PRIVATE_OUTPUT");
        var methods=MergePayloads.methods(MergePayloads.classes(args[0]).values());
        Method callback=methods.get(CALLBACK),init=methods.get(INIT);
        MergePayloads.require(callback!=null && MergePayloads.hash(callback).equals(CALLBACK_SHA),"Pinned init callback changed");
        MergePayloads.require(init!=null && MergePayloads.hash(init).equals(INIT_SHA),"Pinned terminal init changed");
        // Exact Java wrapper success/failure split, not a claim about native queue completion.
        var original=new ArrayList<Instruction>();callback.getImplementation().getInstructions().forEach(original::add);
        int registers=callback.getImplementation().getRegisterCount(),self=registers-2;
        MergePayloads.require(registers==6 && original.get(2).getOpcode()==Opcode.IF_GEZ
                && ((OneRegisterInstruction)original.get(2)).getRegisterA()==self+1,"Callback status branch changed");
        int handleWrite=-1,nativeInit=-1,index=0;
        for(Instruction instruction:init.getImplementation().getInstructions()) {
            if(instruction instanceof ReferenceInstruction r) {
                if(r.getReference() instanceof FieldReference field && field.getDefiningClass().equals(OWNER)
                        && field.getName().equals("mHandler") && instruction.getOpcode()==Opcode.IPUT_WIDE)handleWrite=index;
                if(r.getReference() instanceof MethodReference target && target.getDefiningClass().equals(OWNER)
                        && target.getName().equals("nativeInitBeautyPlay"))nativeInit=index;
            }
            index++;
        }
        MergePayloads.require(handleWrite==3 && nativeInit==26 && handleWrite<nativeInit,"Handle assignment/native init order changed");
        int directCallers=0;
        for(Method m:methods.values())if(m.getImplementation()!=null)for(Instruction instruction:m.getImplementation().getInstructions())
            if(instruction instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference target
                    && target.toString().equals(CALLBACK))directCallers++;
        MergePayloads.require(directCallers==0,"Unexpected DEX caller of native callback");
        var body=new MutableMethodImplementation(callback.getImplementation());
        body.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,self,2,
                new ImmutableMethodReference(HOOK,"initCallback",List.of("Ljava/lang/Object;","I"),"V")));
        Method transformed=with(callback,body);Path out=Path.of(args[1]);Files.createDirectories(out);
        Path dex=out.resolve("native-init-callback-partial.dex");MergePayloads.writeDex(dex,MergePayloads.holders(Map.of(CALLBACK,transformed)));
        Method reread=MergePayloads.methods(MergePayloads.classes(dex.toString()).values()).get(CALLBACK);
        var stripped=new MutableMethodImplementation(reread.getImplementation());Instruction injected=stripped.getInstructions().get(0);
        MergePayloads.require(injected.getOpcode()==Opcode.INVOKE_STATIC_RANGE && injected instanceof ReferenceInstruction ri
                && ri.getReference().toString().equals(HOOK+"->initCallback(Ljava/lang/Object;I)V"),"Callback hook missing");
        stripped.removeInstruction(0);
        MergePayloads.require(MergePayloads.hash(with(reread,stripped)).equals(CALLBACK_SHA),"Stripped callback differs from stock");
        Files.writeString(out.resolve("native-init-callback-contract.tsv"),CALLBACK+"\t"+CALLBACK_SHA+"\t"+MergePayloads.hash(reread)+"\n");
        String report="{\n  \"schema\":\"ulike-native-init-callback-182-v1\",\n  \"callback_method_sha256\":\""+CALLBACK_SHA+"\",\n"
                +"  \"terminal_init_method_sha256\":\""+INIT_SHA+"\",\n  \"callback_access_flags\":"+callback.getAccessFlags()+",\n"
                +"  \"original_callback_instructions\":"+original.size()+",\n  \"direct_dex_callback_callers\":"+directCallers+",\n"
                +"  \"callback_java_success_branch\":\"status >= 0\",\n  \"handle_assigned_before_native_init\":true,\n"
                +"  \"stripped_original_method_reconstruction\":true,\n  \"installed\":false,\n  \"device_execution\":false,\n"
                +"  \"native_queue_barrier_proven\":false,\n  \"restoration_proven\":false\n}\n";
        Files.writeString(out.resolve("NATIVE_INIT_CALLBACK_EVIDENCE.json"),report);
        System.out.println("PASS callback entry observer; original instructions reconstructed; native queue and restoration remain unproved");
    }
}
