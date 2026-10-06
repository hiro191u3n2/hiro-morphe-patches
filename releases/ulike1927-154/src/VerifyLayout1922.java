import java.nio.file.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
/** Verify exact original contracts, emitted guard branches, and helper -> stock ABI. */
public final class VerifyLayout1922 {
 static void check(boolean b,String m){MergePayloads.require(b,m);}
 static List<Instruction> ins(Method m){var a=new ArrayList<Instruction>();m.getImplementation().getInstructions().forEach(a::add);return a;}
 public static void main(String[] a)throws Exception {
  var stockC=MergePayloads.classes(a[0]);var stock=MergePayloads.methods(stockC.values());var seed=MergePayloads.methods(MergePayloads.classes(a[1]).values());
  var patched=MergePayloads.methods(MergePayloads.classes(a[2]).values());var rt=MergePayloads.classes(a[3]);var fields=new HashMap<String,Field>();
  for(ClassDef c:stockC.values())for(Field f:c.getFields())fields.put(DexFormatter.INSTANCE.getFieldDescriptor(f),f);
  for(String key:Transform1922.TARGETS){check(MergePayloads.hash(stock.get(key)).equals(MergePayloads.hash(seed.get(key))),"Original method drift: "+key);}
  int refs=0;
  for(ClassDef c:rt.values())if(c.getType().startsWith("Lcom/hiro/ulike/PreviewLayout1922"))for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:ins(m))if(i instanceof ReferenceInstruction r){
   if(r.getReference() instanceof FieldReference f && f.getDefiningClass().equals(Transform1922.S)){
    Field actual=fields.get(DexFormatter.INSTANCE.getFieldDescriptor(f));check(actual!=null&&(actual.getAccessFlags()&1)!=0,"Stock field absent/inaccessible "+f);refs++;
   }
   if(r.getReference() instanceof MethodReference f && f.getDefiningClass().equals(Transform1922.S)){
    String key=DexFormatter.INSTANCE.getMethodDescriptor(f);Method actual=stock.get(key);
    if(actual!=null){check((actual.getAccessFlags()&1)!=0,"Inaccessible stock method "+f);refs++;}
    else check(Set.of("postDelayed(Ljava/lang/Runnable;J)Z","removeCallbacks(Ljava/lang/Runnable;)Z").contains(key.substring(key.indexOf("->")+2)),"Unexpected inherited stock method "+f);
   }
  }
  for(String k:List.of(Transform1922.U+"->onAnimationUpdate("+Transform1922.A+")V",Transform1922.E+"->onAnimationEnd("+Transform1922.AN+")V")){
   var code=ins(patched.get(k));check(code.get(0).getOpcode()==Opcode.IGET_OBJECT&&code.get(1).getOpcode()==Opcode.INVOKE_STATIC&&code.get(2).getOpcode()==Opcode.MOVE_RESULT,"Missing ownership probe");
   check(code.get(3).getOpcode()==Opcode.IF_NEZ&&((OffsetInstruction)code.get(3)).getCodeOffset()==3&&code.get(4).getOpcode()==Opcode.RETURN_VOID,"Callback guard must jump beyond early return");
  }
  var layout=ins(patched.get(Transform1922.S+"->onLayout(ZIIII)V"));check(layout.get(0).getOpcode()==Opcode.INVOKE_SUPER_RANGE,"Superclass coordinates modified");
  for(int k=1;k<=2;k++){var i=(ThreeRegisterInstruction)layout.get(k);check(layout.get(k).getOpcode()==Opcode.SUB_INT&&i.getRegisterA()==10+k&&i.getRegisterB()==10+k&&i.getRegisterC()==8+k,"Local dimension math drift");}
  var set=ins(patched.get(Transform1922.S+"->setTopOffset(I)V"));check(((OffsetInstruction)set.get(1)).getCodeOffset()==7,"Setter early-return offset");
  for(var m:patched.values())if(Transform1922.TARGETS.contains(MergePayloads.id(m))){var bounds=new HashSet<Integer>();int at=0;for(Instruction i:ins(m)){bounds.add(at);at+=i.getCodeUnits();}at=0;for(Instruction i:ins(m)){if(i instanceof OffsetInstruction off)check(bounds.contains(at+off.getCodeOffset()),"Branch into noninstruction "+MergePayloads.id(m));at+=i.getCodeUnits();}}
  System.out.println("PASS 9 original layout contracts; 2 callback guards; local width/height math; setter no-op branch; instruction boundaries; "+refs+" direct helper-to-stock references accessible. Not an ART/device test.");
 }
}
