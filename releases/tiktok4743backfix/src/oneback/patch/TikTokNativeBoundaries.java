package app.hiro.oneback.patches;

import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;

/** Exact 47.4.3 native Back decisions, after the app's own guards.
 * The original global APK SHA-256 is
 * af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b.
 * Neither a UI label nor an Activity's identity is permission to remove its task.
 */
public final class TikTokNativeBoundaries {
    private TikTokNativeBoundaries() {}
    public static final String HELPER="LX/1AA5;";
    public static final String BASE="Lcom/ss/android/ugc/aweme/homepage/TiktokBaseMainHelper;";
    public static final String STRATEGY="Lcom/ss/android/ugc/aweme/main/assems/BackRefreshStrategyAssem;";
    public static final String SAF="LX/03uJ;";
    public static final String HELPER_SHA="494520fc0a5911045593632a22d6d666090da7ed3f37e7d7cae010b542583a2c";
    public static final String STRATEGY_SHA="2563f7ef1961ebeaf02d9d97b941a66ef0d9a0b246efa1ed8a04508fa9e546f3";
    private static final String ACTIVITY="Landroid/app/Activity;",RUNTIME=OneBackPatch.RUNTIME;

    private static void require(boolean valid,String message) {
        if(!valid)throw new IllegalStateException("TikTok 47.4.3 native Back boundary: "+message);
    }
    private static Method method(Map<String,ClassDef> classes,String owner,String name,String returns) {
        return TikTokNativeContract.method(classes.get(owner),name,"",returns);
    }
    public static void verifyOriginal(Map<String,ClassDef> classes) {
        Method helper=method(classes,HELPER,"onBackPressed","Z");
        Method strategy=method(classes,STRATEGY,"onBackPress","V");
        require(helper!=null&&HELPER_SHA.equals(TikTokNativeContract.digest(helper)),"native interceptor/comment/timer body differs");
        require(strategy!=null&&STRATEGY_SHA.equals(TikTokNativeContract.digest(strategy)),"native tutorial/tab strategy body differs");
        require(helper.getImplementation().getRegisterCount()==18&&strategy.getImplementation().getRegisterCount()==13,"unexpected register layout");
        ClassDef base=classes.get(BASE);
        boolean activity=false;
        if(base!=null)for(Field field:base.getFields())if("LIZ".equals(field.getName())&&ACTIVITY.equals(field.getType())&&(field.getAccessFlags()&9)==1)activity=true;
        require(activity,"native helper Activity binding missing");
        require(method(classes,SAF,"onBackPressed","V")!=null,"SAF Activity Back entry missing");
    }
    private static BuilderInstruction35c call(String name,int register) {
        return new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,register,0,0,0,0,
            new ImmutableMethodReference(RUNTIME,name,Collections.singletonList(ACTIVITY),"Z"));
    }
    public static void apply(Map<String,ClassDef> original,OneBackPatch.Editor editor,Map<String,Integer> counts) {
        verifyOriginal(original);
        MutableMethod helper=(MutableMethod)TikTokNativeContract.method(editor.mutable(HELPER),"onBackPressed","","Z");
        MutableMethod strategy=(MutableMethod)TikTokNativeContract.method(editor.mutable(STRATEGY),"onBackPress","","V");
        require(HELPER_SHA.equals(TikTokNativeContract.digest(helper))&&STRATEGY_SHA.equals(TikTokNativeContract.digest(strategy)),"another selected patch changed the native guard bodies");

        // Original index 25 / address 47 follows InterceptHomeBackPressService
        // and CommentService.LJLL. v16=0 dominates all incoming paths. True skips
        // ONLY the two-Back timer; false is returned so the caller's guards run.
        MutableMethodImplementation code=helper.getImplementation();
        code.addInstruction(25,new BuilderInstruction22c(Opcode.IGET_OBJECT,0,5,
            new ImmutableFieldReference(BASE,"LIZ",ACTIVITY)));
        code.addInstruction(26,call("skipTikTokDoubleBack",0));
        code.addInstruction(27,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
        code.addInstruction(28,new BuilderInstruction10x(Opcode.NOP));
        code.addInstruction(29,new BuilderInstruction11x(Opcode.RETURN,16));
        // Create the fallback label AFTER insertion. A pre-insertion label at
        // index 25 would stay on the gate and turn the false path into a loop.
        code.replaceInstruction(28,new BuilderInstruction21t(Opcode.IF_EQZ,0,code.newLabelForIndex(30)));
        counts.put("tiktok_native_double_back_gate",1);

        // Original index 46 / address 91 is reached only after helper/comment
        // and MainBusinessAbility.f7()/Pn() tutorial guards. v3 is the Activity.
        // Existing branches to index 46 enter the gate; true uses native cleanup.
        code=strategy.getImplementation();
        code.addInstruction(46,call("tryTikTokRootBack",3));
        code.addInstruction(47,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
        code.addInstruction(48,new BuilderInstruction21t(Opcode.IF_NEZ,0,code.newLabelForIndex(27)));
        // dexlib keeps existing incoming labels attached to the old instruction.
        // Both native paths that skip the optional guide ability must run the new
        // gate, rather than jumping past it to the old instruction at index 49.
        for(int incoming:new int[]{41,44}) {
            Instruction branch=code.getInstructions().get(incoming);
            require(branch.getOpcode()==Opcode.IF_EQZ&&branch instanceof OneRegisterInstruction
                    &&((OneRegisterInstruction)branch).getRegisterA()==0,"native tutorial incoming branch differs");
            code.replaceInstruction(incoming,new BuilderInstruction21t(Opcode.IF_EQZ,0,code.newLabelForIndex(46)));
        }
        counts.put("tiktok_native_root_back_gate",1);

        // Mark only the verified native SAF Activity entry. AndroidX's platform
        // fallback can re-enter it once, without recursive main dispatch.
        MutableMethod entry=(MutableMethod)TikTokNativeContract.method(editor.mutable(SAF),"onBackPressed","","V");
        require(entry!=null&&entry.getImplementation()!=null,"wrapped SAF Back entry missing");
        code=entry.getImplementation();int changed=0;
        for(int i=0;i<code.getInstructions().size();i++) {
            Instruction instruction=code.getInstructions().get(i);
            if(!(instruction instanceof ReferenceInstruction))continue;
            Reference reference=((ReferenceInstruction)instruction).getReference();
            if(!(reference instanceof MethodReference))continue;
            MethodReference ref=(MethodReference)reference;
            if(!RUNTIME.equals(ref.getDefiningClass())||!"enterBack".equals(ref.getName()))continue;
            require(instruction.getOpcode()==Opcode.INVOKE_STATIC&&instruction instanceof FiveRegisterInstruction,"unexpected scope entry opcode");
            FiveRegisterInstruction registers=(FiveRegisterInstruction)instruction;
            require(registers.getRegisterCount()==1&&ref.getParameterTypes().equals(Collections.singletonList(ACTIVITY))&&"V".equals(ref.getReturnType()),"unexpected scope entry signature");
            code.replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,registers.getRegisterC(),0,0,0,0,
                new ImmutableMethodReference(RUNTIME,"enterTikTokBack",Collections.singletonList(ACTIVITY),"V")));
            changed++;
        }
        require(changed==1,"expected one native SAF scope entry, found "+changed);
        counts.put("tiktok_native_saf_scope_entry",1);
    }
}
