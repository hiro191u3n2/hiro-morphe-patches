import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
public final class VerifyGesture1925 {
 public static void main(String[] a)throws Exception{
  var stockC=MergePayloads.classes(a[0]);var stock=MergePayloads.methods(stockC.values());
  var seed=MergePayloads.methods(MergePayloads.classes(a[1]).values());var patched=MergePayloads.methods(MergePayloads.classes(a[2]).values());var rt=MergePayloads.classes(a[3]);
  var fields=new TreeMap<String,Field>();for(var c:stockC.values())for(var f:c.getFields())fields.put(DexFormatter.INSTANCE.getFieldDescriptor(f),f);
  for(String id:Transform1925.TARGETS){
   MergePayloads.require(MergePayloads.hash(stock.get(id)).equals(MergePayloads.hash(seed.get(id))),"Native gesture seed drift "+id);
   Method m=patched.get(id);MergePayloads.require(m!=null,"Gesture entry absent "+id);
   var ins=VerifyLayout1923.ins(m);
   if(id.contains("->onTouchEvent(")){
    MergePayloads.require(VerifyLayout1923.ref(ins.get(0)).equals(Transform1925.T+"->reset("+Transform1925.B+"Landroid/view/MotionEvent;)V"),"Cancel/multitouch hook absent");
    var original=VerifyLayout1923.ins(stock.get(id));MergePayloads.require(ins.size()==original.size()+1,"Native touch logic replaced");
    // Every original opcode and referenced member is kept in original order.
    for(int i=0;i<original.size();i++)MergePayloads.require(original.get(i).getOpcode()==ins.get(i+1).getOpcode()&&VerifyLayout1923.ref(original.get(i)).equals(VerifyLayout1923.ref(ins.get(i+1))),"Native touch changed");
   }else{
    String name=id.contains("onDoubleTap(")?"doubleTap":id.contains("onSingleTapConfirmed(")?"confirmed":"singleUp";
    MergePayloads.require(ins.size()==4 && VerifyLayout1923.ref(ins.get(1)).equals(Transform1925.T+"->"+name+"("+Transform1925.B+"Landroid/view/MotionEvent;)Z"),"Wrong gesture routing");
   }
  }
  int checked=0;
  for(var m:rt.get(Transform1925.T).getMethods())if(m.getImplementation()!=null)for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction ri){
   if(ri.getReference() instanceof FieldReference f && stockC.containsKey(f.getDefiningClass())){
    var actual=fields.get(DexFormatter.INSTANCE.getFieldDescriptor(f));MergePayloads.require(actual!=null&&(actual.getAccessFlags()&1)!=0,"Inaccessible native field "+f);checked++;
   }else if(ri.getReference() instanceof MethodReference ref && stockC.containsKey(ref.getDefiningClass())){
    var actual=stock.get(DexFormatter.INSTANCE.getMethodDescriptor(ref));
    if(actual!=null){MergePayloads.require((actual.getAccessFlags()&1)!=0,"Inaccessible native method "+ref);checked++;}
    else MergePayloads.require(Set.of("getWidth","getHeight","getRootView","getLocationOnScreen","isAttachedToWindow","isShown","isEnabled","hasWindowFocus").contains(ref.getName()),"Unknown inherited View method "+ref);
   }
  }
  var flip=stock.get("Li/o/a/b1/a/g/y;->switchCamera()V");boolean debounce=false,camera=false;
  for(var i:flip.getImplementation().getInstructions()){String r=VerifyLayout1923.ref(i);debounce|=r.equals("Li/o/a/b1/a/g/y;->i0()Z");camera|=r.equals("Li/f/l/t/g;->switchCamera()V");}
  MergePayloads.require(debounce&&camera,"Native switch no longer gated camera API");
  System.out.println("PASS1925 4 original gesture contracts; 3 exclusive callback routes; native touch opcodes retained; "+checked+" native ABI references; switchCamera ready/debounce path verified. Not device execution.");
 }
}
