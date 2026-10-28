import java.io.File;
import java.util.*;
import app.hiro.oneback.patches.OneBackPatch;
import app.hiro.oneback.patches.TikTokNativeBoundaries;
import app.hiro.oneback.patches.TikTokNativeContract;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Applies the actual new patch to real v4.1.0 output methods, then audits the
 * serialized DEX. This is a native-code regression, not an Android device test. */
public final class TikTok4743NativeBoundaryAudit {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static void mustReject(Method method,boolean helper,String why){
        boolean failed=false;
        try{if(helper)TikTok4753ContractAudit.helperGate(method);else TikTok4753ContractAudit.strategyGate(method);}
        catch(AssertionError expected){failed=true;}
        check(failed,why);
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("REAL_V4_1_OUTPUT_APK OUTPUT_DEX");
        final Map<String,ClassDef> original=TikTok4753ContractAudit.read(args[0],false);
        TikTokNativeBoundaries.verifyOriginal(original);
        Method old=TikTokNativeContract.method(original.get(TikTokNativeBoundaries.HELPER),"onBackPressed","","Z");
        mustReject(old,true,"old real output must fail the new native boundary audit");
        final Map<String,MutableClass> changed=new TreeMap<String,MutableClass>();
        OneBackPatch.Editor editor=new OneBackPatch.Editor(){
            public MutableClass mutable(String type){
                MutableClass c=changed.get(type);if(c==null){check(original.containsKey(type),"missing real class "+type);c=new MutableClass(original.get(type));changed.put(type,c);}return c;
            }
        };
        Map<String,Integer> counts=new TreeMap<String,Integer>();
        TikTokNativeBoundaries.apply(original,editor,counts);
        check(counts.size()==3&&counts.get("tiktok_native_double_back_gate")==1&&counts.get("tiktok_native_root_back_gate")==1&&counts.get("tiktok_native_saf_scope_entry")==1,"all three real native boundaries applied");
        DexPool.writeTo(args[1],new ImmutableDexFile(Opcodes.getDefault(),changed.values()));
        Map<String,ClassDef> after=TikTok4753ContractAudit.read(args[1],true);
        Method helper=TikTokNativeContract.method(after.get(TikTokNativeBoundaries.HELPER),"onBackPressed","","Z");
        Method strategy=TikTokNativeContract.method(after.get(TikTokNativeBoundaries.STRATEGY),"onBackPress","","V");
        TikTok4753ContractAudit.helperGate(helper);
        TikTok4753ContractAudit.strategyGate(strategy);
        TikTok4753ContractAudit.wrapper(after,TikTokNativeBoundaries.SAF,false);
        TikTok4753ContractAudit.wrapper(after,TikTokNativeBoundaries.SAF,true);
        // Two dangerous label regressions are independent of source instruction
        // hashes: the helper can loop back into itself, or a pre-existing branch
        // can bypass the new root gate while the original code still hashes equal.
        MutableMethodImplementation broken=new MutableMethodImplementation(helper.getImplementation());
        broken.replaceInstruction(28,new BuilderInstruction21t(Opcode.IF_EQZ,0,broken.newLabelForIndex(25)));
        mustReject(TikTok4753ContractAudit.withBody(helper,broken),true,"false gate self-loop must fail audit");
        broken=new MutableMethodImplementation(strategy.getImplementation());
        broken.replaceInstruction(41,new BuilderInstruction21t(Opcode.IF_EQZ,0,broken.newLabelForIndex(49)));
        mustReject(TikTok4753ContractAudit.withBody(strategy,broken),false,"native guard branch skipping gate must fail audit");
        System.out.println("PASS TikTok4743NativeBoundaryAudit: old real output rejected; three actual native hooks applied; serialized DEX verified; original guards/control flow restored; two branch regressions rejected; assertions="+TikTok4753ContractAudit.assertions+"; device behavior not asserted");
    }
}
