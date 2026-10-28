package app.hiro.oneback.patches;

import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;

/** Verified 452.1.0.53.64 AndroidX contracts. The display version range is not
 * used to admit unknown bytecode: every participating native body is checked
 * before mutation. The original v447 implementation remains separate. */
public final class Instagram452Patch {
    private Instagram452Patch() {}
    private static final String MAIN="Lcom/instagram/mainactivity/InstagramMainActivity;";
    private static final String COMPONENT="Landroidx/activity/ComponentActivity;";
    private static final String DISPATCHER="LX/001p;", GESTURE="LX/00nv;";
    private static final String OBJECT="Ljava/lang/Object;", ACTIVITY="Landroid/app/Activity;";
    private static final List<String> NONE=Collections.emptyList();

    /** A distinct native contract selects the 452 path; exact bodies below
     * decide whether it can actually be patched. */
    public static boolean supports(Map<String,ClassDef> classes) {
        return classes.containsKey(MAIN) && find(classes.get(COMPONENT),"CxU",NONE,DISPATCHER)!=null;
    }
    private static Method find(ClassDef c,String name,List<String> args,String result) {
        if(c==null)return null;
        outer:for(Method m:c.getMethods()) {
            if(!name.equals(m.getName())||!result.equals(m.getReturnType())||m.getParameterTypes().size()!=args.size())continue;
            for(int i=0;i<args.size();i++)if(!args.get(i).contentEquals(m.getParameterTypes().get(i)))continue outer;
            return m;
        }
        return null;
    }
    private static void fingerprint(Map<String,ClassDef> classes,String owner,String name,List<String> args,String result,String hash) {
        Method m=find(classes.get(owner),name,args,result);
        if(m==null||!hash.equals(OneBackPatch.instagramNavigationDigest(m)))
            throw new IllegalStateException("Instagram v452 dispatcher fingerprint changed: "+owner+"->"+name);
    }
    public static void verifyDispatcher(Map<String,ClassDef> classes) {
        fingerprint(classes,COMPONENT,"CxU",NONE,DISPATCHER,"8a3b6ba50d01f3d33aeea1a1003b6617dfd66b5c21af5e9ab1f37aa019f37f81");
        fingerprint(classes,DISPATCHER,"A04",NONE,"V","e58ddb979e2fbe173f28d3b2ba54308fbc6018c91eecf1b3c65f57cbf972e27d");
        fingerprint(classes,DISPATCHER,"A00",Collections.singletonList(DISPATCHER),"V","edb655cf819bbf55b7b552dc6bb810ed4b948181ae77d0eaa7e3f5ecafdf0149");
        fingerprint(classes,GESTURE,"invoke",Collections.singletonList(OBJECT),OBJECT,"46f7c0acc42e8ff320290b951619bb163d3221c673b1fe1bcd26d4d3f8ad1337");
    }
    private static BuilderInstruction35c runtime(String name,List<String> args,String result,int... registers) {
        return new BuilderInstruction35c(Opcode.INVOKE_STATIC,registers.length,
            registers.length>0?registers[0]:0,registers.length>1?registers[1]:0,
            registers.length>2?registers[2]:0,0,0,
            new ImmutableMethodReference(OneBackPatch.RUNTIME,name,args,result));
    }
    private static void bump(Map<String,Integer> counts,String key) {Integer old=counts.get(key);counts.put(key,old==null?1:old+1);}
    private static void voidGate(MutableMethod m,String name) {
        MutableMethodImplementation code=m.getImplementation();
        // v4 is the instance / sole static parameter; v0 is overwritten by
        // the native body before every read on both accepted methods.
        code.addInstruction(0,runtime(name,Collections.singletonList(OBJECT),"Z",4));
        code.addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
        code.addInstruction(2,new BuilderInstruction10x(Opcode.NOP));
        code.addInstruction(3,new BuilderInstruction10x(Opcode.RETURN_VOID));
        code.replaceInstruction(2,new BuilderInstruction21t(Opcode.IF_EQZ,0,code.newLabelForIndex(4)));
    }
    public static void applyDispatcher(Map<String,ClassDef> original,OneBackPatch.Editor editor,Map<String,Integer> counts) {
        if(!supports(original))return;
        verifyDispatcher(original);
        MutableMethod owner=(MutableMethod)find(editor.mutable(COMPONENT),"CxU",NONE,DISPATCHER);
        owner.getImplementation().addInstruction(4,runtime("bindInstagramDispatcher",Arrays.asList(ACTIVITY,OBJECT),"V",1,0));
        bump(counts,"instagram_androidx_owner_binding");
        voidGate((MutableMethod)find(editor.mutable(DISPATCHER),"A04",NONE,"V"),"tryInstagramDispatcherBack");
        bump(counts,"instagram_androidx_commit");
        voidGate((MutableMethod)find(editor.mutable(DISPATCHER),"A00",Collections.singletonList(DISPATCHER),"V"),"cancelInstagramDispatcherGesture");
        bump(counts,"instagram_androidx_cancel");
        MutableMethod gesture=(MutableMethod)find(editor.mutable(GESTURE),"invoke",Collections.singletonList(OBJECT),OBJECT);
        MutableMethodImplementation code=gesture.getImplementation();
        Label unitReturn=code.newLabelForIndex(72);
        // Progress is entered only after the dispatcher cast, and before
        // reading/choosing a callback. v1 is dead at this boundary.
        code.addInstruction(53,runtime("progressInstagramDispatcherGesture",Collections.singletonList(OBJECT),"Z",0));
        code.addInstruction(54,new BuilderInstruction11x(Opcode.MOVE_RESULT,1));
        code.addInstruction(55,new BuilderInstruction21t(Opcode.IF_NEZ,1,unitReturn));
        // Start remains after cancellation of the previous native gesture,
        // and before writing/invoking the selected callback. Preserve v1.
        code.addInstruction(42,runtime("startInstagramDispatcherGesture",Collections.singletonList(OBJECT),"Z",3));
        code.addInstruction(43,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
        code.addInstruction(44,new BuilderInstruction21t(Opcode.IF_NEZ,0,unitReturn));
        // The no-previous-callback edge must enter this gate as well.
        code.replaceInstruction(40,new BuilderInstruction21t(Opcode.IF_EQZ,0,code.newLabelForIndex(42)));
        bump(counts,"instagram_androidx_start");bump(counts,"instagram_androidx_progress");
    }
    public static void applyTab(Map<String,ClassDef> original,OneBackPatch.Editor editor,Map<String,Integer> counts) {
        if(supports(original))Instagram452Tab.apply(original,editor,counts);
    }
}
