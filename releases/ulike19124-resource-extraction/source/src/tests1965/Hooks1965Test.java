import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
public final class Hooks1965Test {
 static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 interface Attempt {void run()throws Exception;}
 static void rejects(Attempt action,String why)throws Exception{boolean rejected=false;try{action.run();}catch(IllegalStateException expected){rejected=true;}check(rejected,why);}
 static ClassDef replace(ClassDef c,Method changed){var ms=new ArrayList<Method>();for(Method m:c.getMethods())ms.add(MergePayloads.id(m).equals(MergePayloads.id(changed))?changed:m);return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),ms);}
 public static void main(String[] a)throws Exception {
  var all=MergePayloads.classes(a[0]);var edited=new TreeMap<String,ClassDef>();int pins=0,prefixes=0;
  for(ClassDef before:all.values())if(CameraTraceHooks1965.owns(before.getType())){
   ClassDef after=CameraTraceHooks1965.patch(before);edited.put(after.getType(),after);
   check(!MergePayloads.classHash(before).equals(MergePayloads.classHash(after)),"every reviewed class carries its observer/metadata edit");
   var methods=MergePayloads.methods(List.of(after));
   for(Method m:before.getMethods()){
    String id=MergePayloads.id(m);if(!CameraTraceHooks1965.PINS.containsKey(id))continue;pins++;
    Method current=methods.get(id);check(current!=null&&current.getImplementation().getRegisterCount()==m.getImplementation().getRegisterCount(),"observer keeps exact original register count");
    if(CameraTraceHooks1965.prefix(m)){
     prefixes++;Instruction first=current.getImplementation().getInstructions().iterator().next();check(first.getOpcode()==Opcode.INVOKE_STATIC_RANGE,"observer is prefix only");
     var call=(RegisterRangeInstruction)first;check(call.getRegisterCount()==m.getParameterTypes().size()&&call.getStartRegister()==m.getImplementation().getRegisterCount()-m.getParameterTypes().size(),"instance preference ignores receiver, all static arguments narrow and exact");
     MethodReference ref=(MethodReference)((ReferenceInstruction)first).getReference();check(ref.getReturnType().equals("V")&&ref.getName().equals(m.getName().equals("onPreferenceClick")?"open":m.getName()),"prefix calls exactly the observer contract");
     var b=new MutableMethodImplementation(m.getImplementation());b.addInstruction(0,new BuilderInstruction10x(Opcode.NOP));ClassDef tampered=replace(before,CameraTraceHooks1965.replace(m,b));
     rejects(()->CameraTraceHooks1965.patch(tampered),"modified baseline method cannot pass published pin");
     var bad=new MutableMethodImplementation(current.getImplementation());bad.replaceInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,call.getStartRegister(),call.getRegisterCount(),new ImmutableMethodReference(ref.getDefiningClass(),"wrong",ref.getParameterTypes(),"V")));
     ClassDef wrong=replace(after,CameraTraceHooks1965.replace(current,bad));rejects(()->CameraTraceHooks1965.verify(before,wrong),"inverse cannot conceal an arbitrary prefix");
    }
   }
   rejects(()->CameraTraceHooks1965.patch(after),"duplicate already-patched input rejected");
  }
  check(edited.size()==3&&pins==12&&prefixes==11,"all exact reviewed classes/pins/prefixes tested");
  Path output=Path.of(a[1]);MergePayloads.writeDex(output,edited.values());var serialized=MergePayloads.classes(output.toString());
  for(var row:serialized.entrySet()){
   CameraTraceHooks1965.verify(all.get(row.getKey()),row.getValue());check(true,"serialized inverse restores original whole-class bytecode");
   check(CameraTraceHooks1965.canonicalHash(edited.get(row.getKey())).equals(CameraTraceHooks1965.canonicalHash(row.getValue())),"serialization keeps observer code and canonical annotation values");
  }
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"original_control_flow_restored_by_inverse\":true,\"modified_baseline_rejected\":true,\"serialized_inverse_verified\":true,\"physical_android_tested\":false}");
 }
}
