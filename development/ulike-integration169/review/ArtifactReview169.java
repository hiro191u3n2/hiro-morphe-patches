import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;

/** Read-only checks on real DEX payloads, separate from the hook generator. */
public final class ArtifactReview169 {
    static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    public static void main(String[] args)throws Exception {
        var old=MergePayloads.classes(args[0]);var now=MergePayloads.classes(args[1]);
        String cy="Lcom/hiro/ulike/CaptureYuv;",ss="Lcom/hiro/ulike/StyleStill4;";
        Set<String> changed=Set.of(
            cy+"->init(Landroid/content/Context;)V",cy+"->install(Landroid/preference/PreferenceActivity;)V",
            ss+"->choose(Ljava/lang/Object;Ljava/lang/Object;)Z",ss+"->configureRequest(Ljava/lang/Object;Ljava/lang/Object;)Z",
            cy+"->session(Ljava/lang/Object;Landroid/hardware/camera2/CameraDevice;Landroid/hardware/camera2/params/SessionConfiguration;)V",
            cy+"->reader(Ljava/lang/Object;Landroid/media/ImageReader;)Landroid/media/ImageReader;",
            cy+"->isReady()Z",cy+"->release(Ljava/lang/Object;)V");
        int stableClasses=0,stableMethods=0,opticalClasses=0,changedCount=0;
        for(var entry:old.entrySet()) {
            ClassDef n=now.get(entry.getKey());require(n!=null,"old runtime class removed");
            if(!entry.getKey().equals(cy) && !entry.getKey().equals(ss)) {
                require(MergePayloads.classHash(entry.getValue()).equals(MergePayloads.classHash(n)),"old class altered outside eight hooks: "+entry.getKey());stableClasses++;
            }
            if(entry.getKey().startsWith("Lcom/hiro/ulike/OpticalZoom"))opticalClasses++;
        }
        var previousMethods=MergePayloads.methods(old.values());var newMethods=MergePayloads.methods(now.values());
        for(var entry:previousMethods.entrySet()) {
            Method next=newMethods.get(entry.getKey());require(next!=null,"old method removed");
            boolean same=MergePayloads.hash(entry.getValue()).equals(MergePayloads.hash(next));
            if(changed.contains(entry.getKey())){require(!same,"expected hook absent");changedCount++;}
            else{require(same,"unexpected old method change: "+entry.getKey());stableMethods++;}
        }
        require(changedCount==8 && opticalClasses>0,"exact old hook set and optical classes");
        Method gate=newMethods.get("Lcom/hiro/ulike/integration169/CandidateGate169;->enabled()Z");
        require(gate!=null,"compiled candidate gate missing");
        ArrayList<Instruction> instructions=new ArrayList<>();gate.getImplementation().getInstructions().forEach(instructions::add);
        require(instructions.size()==2 && instructions.get(0).getOpcode()==Opcode.CONST_4 && instructions.get(1).getOpcode()==Opcode.RETURN,"compiled gate not constant false");
        require(((NarrowLiteralInstruction)instructions.get(0)).getNarrowLiteral()==0 &&
            ((OneRegisterInstruction)instructions.get(0)).getRegisterA()==((OneRegisterInstruction)instructions.get(1)).getRegisterA(),"compiled gate result is not false");
        System.out.println("{\"unchanged_old_classes\":"+stableClasses+",\"unchanged_old_methods\":"+stableMethods+",\"exact_changed_runtime_methods\":"+changedCount+",\"unchanged_optical_classes\":"+opticalClasses+",\"compiled_gate_hard_false\":true}");
    }
}
