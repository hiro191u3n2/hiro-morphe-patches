import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
public final class VerifyHooks169 {
 public static void main(String[] a)throws Exception {
  var old=MergePayloads.methods(MergePayloads.classes(a[0]).values());var current=MergePayloads.methods(MergePayloads.classes(a[1]).values());int branches=0;
  for(Method m:current.values()) {
   ArrayList<Instruction> ins=new ArrayList<>();if(m.getImplementation()==null)continue;for(var i:m.getImplementation().getInstructions())ins.add(i);
   if(ins.size()<4 || !(ins.get(0) instanceof ReferenceInstruction ref) || !(ref.getReference() instanceof MethodReference target) || !target.getDefiningClass().startsWith("Lcom/hiro/ulike/integration169/"))continue;
   if(ins.get(2).getOpcode()==Opcode.IF_EQZ){
    int branchAddress=ins.get(0).getCodeUnits()+ins.get(1).getCodeUnits();int originalAddress=branchAddress+ins.get(2).getCodeUnits()+ins.get(3).getCodeUnits();
    if(branchAddress+((OffsetInstruction)ins.get(2)).getCodeOffset()!=originalAddress)throw new IllegalStateException("gate-off branch misses original entry "+MergePayloads.id(m));
    if(ins.get(4).getOpcode()!=old.get(MergePayloads.id(m)).getImplementation().getInstructions().iterator().next().getOpcode())throw new IllegalStateException("original entry opcode changed");branches++;
   }
  }
  if(branches!=(a.length>2?Integer.parseInt(a[2]):3))throw new IllegalStateException("unexpected early-return gate count: "+branches);
  System.out.println("PASS gate-off branches target exact original instruction; no self-loop or skipped original entry.");
 }
}
