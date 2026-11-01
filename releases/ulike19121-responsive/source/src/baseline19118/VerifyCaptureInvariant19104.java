import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Preserve the deployed single-shot bypass in the emitted runtime and rebuilt APK. */
public final class VerifyCaptureInvariant19104 {
 static final String OWNER="Lcom/hiro/ulike/BurstCapture1933;";
 static final String PARAMETERS="(Ljava/lang/Object;Landroid/media/Image;Landroid/hardware/camera2/TotalCaptureResult;)Z";
 static final String TARGET=OWNER+"->beginImage"+PARAMETERS;
 static final String LEGACY=OWNER+"->legacyAdmissionDisabled19104"+PARAMETERS;
 static final String TIMING="Lcom/hiro/ulike/ShotContext1932;->q0Entry19104(Ljava/lang/Object;Landroid/media/Image;Landroid/hardware/camera2/CaptureResult;)V";
 static void req(boolean value,String message){MergePayloads.require(value,message);}
 static void verify(Map<String,ClassDef> classes){
  Map<String,Method> methods=MergePayloads.methods(classes.values());
  Method target=methods.get(TARGET),legacy=methods.get(LEGACY),timing=methods.get(TIMING);
  req(target!=null&&target.getImplementation()!=null,"Single-shot admission method missing");
  req((target.getAccessFlags()&0x2012f)==0x9,"Single-shot entry must remain public static without synchronized/native work");
  req(legacy!=null&&(legacy.getAccessFlags()&0x10f)==0xa,"Legacy admission must remain private static and non-native");
  req(timing!=null&&(timing.getAccessFlags()&0x108)==0x8,"Q0 entry timing method missing");
  MethodImplementation body=target.getImplementation();
  req(body.getTryBlocks().isEmpty(),"Single-shot bypass must not contain catch paths");
  List<Instruction> ins=new ArrayList<>();for(Instruction i:body.getInstructions())ins.add(i);
  req(ins.size()==3,"Single-shot entry must contain only timing, false, return");
  Instruction call=ins.get(0),zero=ins.get(1),ret=ins.get(2);
  req((call.getOpcode()==Opcode.INVOKE_STATIC||call.getOpcode()==Opcode.INVOKE_STATIC_RANGE)
    &&call instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference m&&m.toString().equals(TIMING),
    "Single-shot entry invokes admission or copy work");
  int first=body.getRegisterCount()-3;req(first>=0,"Invalid single-shot parameter registers");
  if(call instanceof FiveRegisterInstruction f)req(f.getRegisterCount()==3&&f.getRegisterC()==first&&f.getRegisterD()==first+1&&f.getRegisterE()==first+2,"Q0 timing must receive exact entry arguments");
  else {req(call instanceof RegisterRangeInstruction,"Unknown timing invocation format");RegisterRangeInstruction r=(RegisterRangeInstruction)call;req(r.getRegisterCount()==3&&r.getStartRegister()==first,"Q0 timing must receive exact ranged arguments");}
  req((zero.getOpcode()==Opcode.CONST_4||zero.getOpcode()==Opcode.CONST_16||zero.getOpcode()==Opcode.CONST||zero.getOpcode()==Opcode.CONST_HIGH16)
    &&zero instanceof NarrowLiteralInstruction n&&n.getNarrowLiteral()==0,"Single-shot entry must return false");
  req(ret.getOpcode()==Opcode.RETURN&&((OneRegisterInstruction)ret).getRegisterA()==((OneRegisterInstruction)zero).getRegisterA(),"Single-shot false result register differs");
  for(Method method:methods.values())if(method.getImplementation()!=null)for(Instruction i:method.getImplementation().getInstructions())
   if(i instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference m)
    req(!m.toString().equals(LEGACY),"Dormant burst admission became reachable: "+MergePayloads.id(method));
 }
 public static void main(String[] args)throws Exception{
  req(args.length==1,"DEX_OR_APK");verify(MergePayloads.classes(args[0]));
  System.out.println("PASS single capture: timing-only entry, false return, legacy admission unreachable");
 }
}
