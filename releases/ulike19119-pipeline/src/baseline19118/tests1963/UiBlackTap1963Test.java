import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
public final class UiBlackTap1963Test {
 static int checks;
 static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static String ref(Instruction op){return op instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static List<Instruction> instructions(Method m){var ops=new ArrayList<Instruction>();for(Instruction op:m.getImplementation().getInstructions())ops.add(op);return ops;}
 static Method body(Method m,List<Instruction> ops){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),new ImmutableMethodImplementation(m.getImplementation().getRegisterCount(),ops,List.of(),List.of()));}
 public static void main(String[] args)throws Exception {
  ClassDef before=MergePayloads.classes(args[0]).get(TransformBlackTap1963.TYPE);
  check(before!=null,"Published BlackTap class exists");
  check(MergePayloads.classHash(before).equals(TransformBlackTap1963.CLASS_PIN),"Exact published .62 class pin");
  var old=MergePayloads.methods(List.of(before));Method original=old.get(TransformBlackTap1963.METHOD);
  check(MergePayloads.hash(original).equals(TransformBlackTap1963.METHOD_PIN),"Exact .62 doubleTap method pin");
  var ops=instructions(original);int boundary=-1,switches=0,delegates=0,address=0;
  for(int i=0;i<ops.size();i++){
   Instruction op=ops.get(i);String reference=ref(op);
   if(reference.equals(TransformBlackTap1963.BLACK_ENTRY)){boundary=i;check(address==0x36,"Real black branch is original code address 0x36");}
   if(reference.equals("Li/o/a/b1/a/g/y;->switchCamera()V"))switches++;
   if(reference.equals(TransformBlackTap1963.DELEGATE))delegates++;
   address+=op.getCodeUnits();
  }
  check(boundary>0&&switches==1&&delegates==1,"Previous black-region camera switch reproduced in actual published dex");
  check(original.getImplementation().getTryBlocks().isEmpty(),"Truncated branch crosses no handler boundary");
  ClassDef patched=TransformBlackTap1963.patch(before);TransformBlackTap1963.verify(before,patched);
  Path output=Path.of(args[1]);MergePayloads.writeDex(output,List.of(patched));
  ClassDef after=MergePayloads.classes(output.toString()).get(TransformBlackTap1963.TYPE);
  TransformBlackTap1963.verify(before,after);check(true,"Serialized emitted dex verifies exact class repair");
  var next=MergePayloads.methods(List.of(after));var changed=new TreeSet<String>();
  check(old.keySet().equals(next.keySet()),"Complete method inventory retained");
  for(var row:old.entrySet())if(!MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(next.get(row.getKey()))))changed.add(row.getKey());
  check(changed.equals(Set.of(TransformBlackTap1963.METHOD)),"Changed runtime method inventory is exactly one");
  var actual=instructions(next.get(TransformBlackTap1963.METHOD));
  check(actual.size()==boundary+1,"Only black camera-work tail removed");
  Instruction last=actual.get(actual.size()-1);
  check(last.getOpcode()==Opcode.RETURN&&((OneRegisterInstruction)last).getRegisterA()==2,"Black-region branch immediately returns native handled true");
  var wanted=new ArrayList<Instruction>(ops.subList(0,boundary));wanted.add(new ImmutableInstruction11x(Opcode.RETURN,2));
  check(MergePayloads.hash(body(original,wanted)).equals(MergePayloads.hash(body(next.get(TransformBlackTap1963.METHOD),actual))),"Normalized full nonblack/pending prefix and all branch offsets preserved");
  switches=0;delegates=0;for(Instruction op:actual){String reference=ref(op);
   if(reference.contains("switchCamera")||reference.contains("startPreview")||reference.contains("openCamera"))switches++;
   if(reference.equals(TransformBlackTap1963.DELEGATE))delegates++;
  }
  check(switches==0,"No camera switch/open/restart call remains reachable or present");
  check(delegates==1,"Outside-preview native double-tap delegated once");
  check(MergePayloads.classHash(patched).equals(MergePayloads.classHash(after)),"All unrelated fields/methods/class metadata preserved after serialization");
  boolean rejected=false;try{TransformBlackTap1963.patch(after);}catch(RuntimeException expected){rejected=true;}
  check(rejected,"Already-patched or different-version input is rejected");
  System.out.println("UI_BLACKTAP1963_ASSERTIONS="+checks);
 }
}
