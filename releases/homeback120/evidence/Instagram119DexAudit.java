import java.util.*;import java.io.*;
import app.hiro.oneback.patches.OneBackPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.builder.*;import com.android.tools.smali.dexlib2.builder.instruction.*;import com.android.tools.smali.dexlib2.immutable.*;

/** In-memory targeted application to supplied APK, never rewrites/signs the APK. */
public final class Instagram119DexAudit {
 static int assertions;static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
 static int target(List<Instruction> ins,int i){return HomeBackAudit.addr(ins,i)+((OffsetInstruction)ins.get(i)).getCodeOffset();}
 static Method back(ClassDef c){for(Method m:c.getMethods())if(m.getName().equals("onBackPressed")&&m.getParameterTypes().isEmpty()&&m.getReturnType().equals("Z"))return m;throw new AssertionError("missing handler");}
 public static void main(String[] args)throws Exception {
  Map<String,ClassDef> classes=HomeBackAudit.read(args[0]);int matches=0;String matched="";int nativeInstructionCount=0;
  for(ClassDef c:classes.values()) {
   int scratch=OneBackPatch.instagramReelsBoundary(c,classes);if(scratch<0)continue;matches++;matched=c.getType();
   Method before=back(c);List<Instruction> old=HomeBackAudit.instructions(before);nativeInstructionCount=old.size();
   MutableClass out=new MutableClass(c);Map<String,Integer> counts=new LinkedHashMap<String,Integer>();OneBackPatch.instagramReelsHook(out,scratch,counts);
   Method after=back(out);List<Instruction> ins=HomeBackAudit.instructions(after);
   check(Integer.valueOf(1).equals(counts.get("instagram_reels_native_boundary")),"one targeted insertion");
   check(ins.size()==old.size()+4&&after.getImplementation().getRegisterCount()==before.getImplementation().getRegisterCount(),"four instructions and no register shift");
   check("tryInstagramReelsBack".equals(HomeBackAudit.ref(ins.get(17)).getName()),"actual native boundary invokes new runtime entry");
   for(int i:new int[]{2,4})check(target(ins,i)==HomeBackAudit.addr(ins,7),"first native layer targets preserved");
   for(int i:new int[]{8,12,15})check(target(ins,i)==HomeBackAudit.addr(ins,17),"each no-layer path reaches hook");
   check(target(ins,19)==HomeBackAudit.addr(ins,21),"false runtime verdict resumes exact Config invocation");
   for(int i:new int[]{6,16})check(ins.get(i).getOpcode()==Opcode.RETURN&&((OneRegisterInstruction)ins.get(i)).getRegisterA()==((OneRegisterInstruction)old.get(i)).getRegisterA(),"native popup/nested-layer true returns bypass hook");
   check(ins.get(20).getOpcode()==Opcode.RETURN&&((OneRegisterInstruction)ins.get(20)).getRegisterA()==scratch,"handled runtime result directly returns consumed true");
   MutableMethodImplementation restore=new MutableMethodImplementation(after.getImplementation());
   for(int i=0;i<4;i++)restore.removeInstruction(17);
   for(int i:new int[]{8,12,15}){OneRegisterInstruction branch=(OneRegisterInstruction)restore.getInstructions().get(i);restore.replaceInstruction(i,new BuilderInstruction21t(Opcode.IF_EQZ,branch.getRegisterA(),restore.newLabelForIndex(17)));}
   Method restored=new ImmutableMethod(before.getDefiningClass(),before.getName(),before.getParameters(),before.getReturnType(),before.getAccessFlags(),before.getAnnotations(),before.getHiddenApiRestrictions(),restore);
   check(Arrays.equals(HomeBackAudit.methodBytes(before),HomeBackAudit.methodBytes(restored)),"removing four instructions restores complete native method byte-for-byte including branches/debug/try");
   Map<String,Method> afterMethods=HomeBackAudit.methods(out);int untouched=0;
   for(Method m:c.getMethods())if(m!=before&&!HomeBackAudit.key(m).equals(HomeBackAudit.key(before))){check(Arrays.equals(HomeBackAudit.methodBytes(m),HomeBackAudit.methodBytes(afterMethods.get(HomeBackAudit.key(m)))),"unrelated method intact");untouched++;}
   for(int mutation=0;mutation<3;mutation++) {
    MutableClass changed=new MutableClass(c);app.morphe.patcher.util.proxy.mutableTypes.MutableMethod bad=(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod)back(changed);
    MutableMethodImplementation b=bad.getImplementation();
    if(mutation==0){OneRegisterInstruction branch=(OneRegisterInstruction)b.getInstructions().get(8);b.replaceInstruction(8,new BuilderInstruction21t(Opcode.IF_EQZ,branch.getRegisterA(),b.newLabelForIndex(18)));}
    if(mutation==1)b.replaceInstruction(18,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,((OneRegisterInstruction)old.get(1)).getRegisterA()));
    if(mutation==2)b.replaceInstruction(16,new BuilderInstruction11x(Opcode.RETURN,scratch));
    boolean refused=false;try{OneBackPatch.instagramReelsBoundary(changed,classes);}catch(IllegalStateException expected){refused=true;}
    check(refused,"changed native guard/branch/register fingerprint fails closed #"+mutation);
   }
   System.out.println("MATCH "+matched+" scratch=v"+scratch+" boundary_instruction=17 native_pc=31 native_instructions="+nativeInstructionCount+" unchanged_other_methods="+untouched);
  }
  check(matches==1,"exactly one semantically matched native Reels handler in actual APK");
  System.out.println("PASS Instagram119DexAudit: "+matches+" actual Reels handler, "+assertions+" assertions; APK unchanged; physical Android not tested");
 }
}
