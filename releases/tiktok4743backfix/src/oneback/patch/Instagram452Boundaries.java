package app.hiro.oneback.patches;

import java.util.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;

/** Authored gates for the supplied 452.1.0.53.64 APK. The new third native
 * Reels guard must run before the exit gate. Unknown bodies remain rejected.
 * Whole-method hashes normalize only string and branch encoding widths. */
public final class Instagram452Boundaries {
    private Instagram452Boundaries() {}
    public static final String REELS="LX/02Hr;";
    public static boolean supports(Map<String,ClassDef> classes) {
        return classes.containsKey("Lcom/instagram/mainactivity/InstagramMainActivity;")
                &&classes.containsKey(REELS)&&classes.containsKey("LX/001p;")&&classes.containsKey("LX/004K;");
    }
    public static void verifyMain(Method method) {
        if(!"A1u".equals(method.getName())||method.getAccessFlags()!=17
                ||!method.getParameterTypes().equals(Collections.singletonList("LX/05Jc;"))
                ||!"aecd319627ceeb636a9f64b716a6b98f48bd79e15d66509f6424a54b20ec789f".equals(OneBackPatch.instagramNavigationDigest(method)))
            throw new IllegalStateException("Instagram 452 main Back contract changed");
    }
    public static int reels(ClassDef clazz,Map<String,ClassDef> classes) {
        if(!supports(classes))throw new IllegalStateException("Instagram 452 Reels dependencies absent");
        Method method=find(clazz);
        if(method.getAccessFlags()!=17||method.getImplementation().getRegisterCount()!=11
                ||!"6cf72ca941e43a06a75c3610b1bc807dbbfbe180b9629da4358515f17e374228".equals(OneBackPatch.instagramNavigationDigest(method)))
            throw new IllegalStateException("Instagram 452 Reels Back contract changed");
        // Only the two non-consuming edges of the third native guard may
        // enter this boundary. Its other paths return true before our gate.
        List<Instruction> ins=new ArrayList<Instruction>();
        for(Instruction i:method.getImplementation().getInstructions())ins.add(i);
        int[] address=new int[ins.size()+1];
        for(int i=0;i<ins.size();i++)address[i+1]=address[i]+ins.get(i).getCodeUnits();
        List<Integer> incoming=new ArrayList<Integer>();
        for(int i=0;i<ins.size();i++)if(ins.get(i) instanceof OffsetInstruction
                &&address[i]+((OffsetInstruction)ins.get(i)).getCodeOffset()==address[44])incoming.add(i);
        if(!incoming.equals(Arrays.asList(27,34)))throw new IllegalStateException("Instagram 452 Reels incoming edges changed");
        return 0;
    }
    private static Method find(ClassDef clazz) {
        for(Method m:clazz.getMethods())if("onBackPressed".equals(m.getName())&&m.getParameterTypes().isEmpty()&&"Z".equals(m.getReturnType()))return m;
        throw new IllegalStateException("Instagram 452 Reels Back method missing");
    }
    public static void hookReels(MutableClass clazz,int scratch,Map<String,Integer> counts) {
        if(scratch!=0)throw new IllegalStateException("Instagram 452 Reels scratch changed");
        MutableMethod method=(MutableMethod)find(clazz);MutableMethodImplementation code=method.getImplementation();
        code.addInstruction(44,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,10,0,0,0,0,
                new ImmutableMethodReference(OneBackPatch.RUNTIME,"tryInstagramReelsBack",Collections.singletonList("Ljava/lang/Object;"),"Z")));
        code.addInstruction(45,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
        code.addInstruction(46,new BuilderInstruction10x(Opcode.NOP));
        code.addInstruction(47,new BuilderInstruction11x(Opcode.RETURN,0));
        code.replaceInstruction(46,new BuilderInstruction21t(Opcode.IF_EQZ,0,code.newLabelForIndex(48)));
        for(int index:new int[]{27,34}) {
            Instruction old=code.getInstructions().get(index);
            code.replaceInstruction(index,new BuilderInstruction21t(old.getOpcode(),((OneRegisterInstruction)old).getRegisterA(),code.newLabelForIndex(44)));
        }
        counts.put("instagram_reels_native_boundary",counts.getOrDefault("instagram_reels_native_boundary",0)+1);
        counts.put("instagram_452_third_native_guard_preserved",1);
    }
}
