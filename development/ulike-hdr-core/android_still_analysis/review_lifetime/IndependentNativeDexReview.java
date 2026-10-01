import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Independent structural tests over the reserialized private delta DEX. */
public final class IndependentNativeDexReview {
 static int checks;
 static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
 static final String OWNER="Lcom/ss/android/medialib/RecordInvoker;",HOOK="Lcom/hiro/ulike/hdr/stillanalysis/NativeLifetimeHooks;";
 static MethodReference hook(Instruction i){if(i instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference m && m.getDefiningClass().equals(HOOK))return m;return null;}
 public static void main(String[] args)throws Exception {
  Map<String,Method> original=MergePayloads.methods(MergePayloads.classes(args[0]).values()),changed=MergePayloads.methods(MergePayloads.classes(args[1]).values());check(changed.size()==3,"three only");int totalReturns=0;
  for(var item:changed.entrySet()) {
   Method m=item.getValue(),o=original.get(item.getKey());check(o!=null && m.getDefiningClass().equals(OWNER),"original exact owner");
   check(m.getAccessFlags()==o.getAccessFlags() && m.getImplementation().getRegisterCount()==o.getImplementation().getRegisterCount(),"flags/registers retained");
   int params=1;for(CharSequence p:m.getParameterTypes())params+=p.toString().equals("J") || p.toString().equals("D")?2:1;
   int receiver=m.getImplementation().getRegisterCount()-params;
   List<Instruction> ins=new ArrayList<>();for(Instruction i:m.getImplementation().getInstructions())ins.add(i);
   int cleanup=0;for(int i=0;i<ins.size()-3;i++)cleanup+=ins.get(i).getCodeUnits();
   int entryCalls=0,returnCalls=0,failureCalls=0;
   for(int index=0;index<ins.size();index++) {
    Instruction in=ins.get(index);MethodReference h=hook(in);if(h==null)continue;
    check(in.getOpcode()==Opcode.INVOKE_STATIC_RANGE,"hook encoding");RegisterRangeInstruction range=(RegisterRangeInstruction)in;check(range.getRegisterCount()==1,"one hook argument");
    if(h.getName().equals("returned")){returnCalls++;check(ins.get(index+1).getOpcode()==Opcode.RETURN && ((OneRegisterInstruction)ins.get(index+1)).getRegisterA()==range.getStartRegister(),"status preserves original return register");}
    else if(h.getName().equals("failed")){failureCalls++;check(index==ins.size()-2 && range.getStartRegister()==receiver,"cleanup carries exact receiver");}
    else {entryCalls++;check(index==0 && range.getStartRegister()==receiver,"entry carries exact receiver");check(h.getName().equals(m.getName().equals("uninitBeautyPlay")?"beforeUninit":"beforeInit"),"correct lifecycle entry");}
   }
   int originalReturns=0;for(Instruction i:o.getImplementation().getInstructions())if(i.getOpcode()==Opcode.RETURN)originalReturns++;
   check(returnCalls==originalReturns && entryCalls==1 && failureCalls==1,"all exits and one entry/cleanup");totalReturns+=returnCalls;
   for(TryBlock<?> t:m.getImplementation().getTryBlocks())for(ExceptionHandler h:t.getExceptionHandlers())if(h.getHandlerCodeAddress()==cleanup){check(t.getStartCodeAddress()>=3 && t.getStartCodeAddress()+t.getCodeUnitCount()<=cleanup,"entry and cleanup excluded from added catch");check(h.getExceptionType()==null,"catch all unknown failures");}
   check(ins.get(ins.size()-3).getOpcode()==Opcode.MOVE_EXCEPTION && ((OneRegisterInstruction)ins.get(ins.size()-3)).getRegisterA()==0,"exception retained in v0");
   check(ins.get(ins.size()-1).getOpcode()==Opcode.THROW && ((OneRegisterInstruction)ins.get(ins.size()-1)).getRegisterA()==0,"same original throwable rethrown");
  }
  check(totalReturns==6,"actual six native return sites");System.out.println("PASS independent native DEX review "+checks+" structural checks, 3 methods/6 returns; no ART execution");
 }
}
