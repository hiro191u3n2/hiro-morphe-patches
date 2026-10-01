import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.builder.*;

/** Removes only declared hooks and proves exact original instruction/register,
 * branch, monitor-handler and method metadata reconstruction after DEX writing.
 */
public final class VerifyNativeLifetime169 {
 static final String HOOK="Lcom/hiro/ulike/hdr/stillanalysis/NativeLifetimeHooks;";
 static String hook(Instruction in){if(in instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference m && m.getDefiningClass().equals(HOOK))return m.getName();return "";}
 public static void main(String[] args)throws Exception {
  var stock=MergePayloads.methods(MergePayloads.classes(args[0]).values());var patched=MergePayloads.methods(MergePayloads.classes(args[1]).values());int returns=0,exceptionRanges=0;
  MergePayloads.require(patched.keySet().equals(PrepareNativeLifetime169.PINS.keySet()),"Partial native method set changed");
  for(var e:patched.entrySet()) {
   Method m=e.getValue(),original=stock.get(e.getKey());var b=new MutableMethodImplementation(m.getImplementation());var ins=b.getInstructions();int n=ins.size(),handler=0;
   for(int i=0;i<n-3;i++)handler+=ins.get(i).getCodeUnits();
   MergePayloads.require(ins.get(n-3).getOpcode()==Opcode.MOVE_EXCEPTION && ((OneRegisterInstruction)ins.get(n-3)).getRegisterA()==0,"missing exception scratch");
   MergePayloads.require(hook(ins.get(n-2)).equals("failed") && ins.get(n-1).getOpcode()==Opcode.THROW && ((OneRegisterInstruction)ins.get(n-1)).getRegisterA()==0,"original exception not rethrown");
   MergePayloads.require(hook(ins.get(0)).equals(m.getName().equals("uninitBeautyPlay")?"beforeUninit":"beforeInit"),"wrong entry hook");
   var originalRanges=new ArrayList<BuilderTryBlock>();
   for(var t:b.getTryBlocks()) {
    boolean ours=false;for(var h:t.getExceptionHandlers())if(h.getHandlerCodeAddress()==handler){MergePayloads.require(h.getExceptionType()==null,"cleanup is not catch-all");ours=true;}
    if(ours){MergePayloads.require(t.getStartCodeAddress()>=3 && t.getStartCodeAddress()+t.getCodeUnitCount()<=handler,"entry or appended handler inside outer try");exceptionRanges++;}
    else originalRanges.add(t);
   }
   for(int i=1;i<n-3;i++)if(ins.get(i).getOpcode()==Opcode.RETURN) {
    MergePayloads.require(hook(ins.get(i-1)).equals("returned"),"normal exit bypasses completion");
    MergePayloads.require(((RegisterRangeInstruction)ins.get(i-1)).getStartRegister()==((OneRegisterInstruction)ins.get(i)).getRegisterA(),"wrong return status");returns++;
   }
   // Every original explicit rethrow escapes its old monitor catch and is
   // covered by the new cleanup. Otherwise a native failure could be lost.
   int address=0;for(int i=0;i<n-3;i++) {
    if(ins.get(i).getOpcode()==Opcode.THROW) {boolean found=false;for(var t:b.getTryBlocks())if(address>=t.getStartCodeAddress()&&address<t.getStartCodeAddress()+t.getCodeUnitCount())for(var h:t.getExceptionHandlers())found|=h.getHandlerCodeAddress()==handler;MergePayloads.require(found,"original throw not quarantined");}
    address+=ins.get(i).getCodeUnits();
   }
   for(int i=n-1;i>=n-3;i--)b.removeInstruction(i);
   for(int i=b.getInstructions().size()-1;i>=0;i--)if(!hook(b.getInstructions().get(i)).isEmpty())b.removeInstruction(i);
   var stripped=new ImmutableMethodImplementation(b.getRegisterCount(),b.getInstructions(),originalRanges,b.getDebugItems());
   Method reconstructed=new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),stripped);
   MergePayloads.require(MergePayloads.hash(reconstructed).equals(MergePayloads.hash(original)),"Original method did not reconstruct exactly: "+e.getKey());
  }
  System.out.println("PASS exact reconstruction of 3 stock methods; normal exits="+returns+", exception cleanup regions="+exceptionRanges+"; not ART/device tested");
 }
}
