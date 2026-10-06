import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
public final class VerifyFacing1928 {
 static void req(boolean b,String m){MergePayloads.require(b,m);}
 static String ref(Instruction i){return Transform1928.ref(i);}
 static Method repl(Method m,MethodImplementation b){return Transform1928.repl(m,b);}
 static List<Instruction> ins(Method m){var a=new ArrayList<Instruction>();m.getImplementation().getInstructions().forEach(a::add);return a;}
 static String snapshot=Transform1928.H+"->rememberCurrent()V";
 public static void main(String[] a)throws Exception{
  req(a.length==4||a.length==5,"SEED BASE_RUNTIME NEW_METHODS NEW_RUNTIME [STOCK]");
  var seed=MergePayloads.methods(MergePayloads.classes(a[0]).values());var old=MergePayloads.methods(MergePayloads.classes(a[1]).values());
  var ms=MergePayloads.methods(MergePayloads.classes(a[2]).values());var runtime=MergePayloads.classes(a[3]);var rs=MergePayloads.methods(runtime.values());
  req(seed.keySet().equals(Transform1928.TARGETS),"Native target set");
  Method t=ms.get(Transform1928.INITIAL);var b=new MutableMethodImplementation(t.getImplementation());int hits=0;
  for(int i=b.getInstructions().size()-1;i>=0;i--)if(ref(b.getInstructions().get(i)).equals(Transform1928.H+"->initial(Z)Z")){
   int r=((FiveRegisterInstruction)b.getInstructions().get(i)).getRegisterC();req(b.getInstructions().get(i+1).getOpcode()==Opcode.MOVE_RESULT&&b.getInstructions().get(i+2).getOpcode()==Opcode.RETURN,"Startup result/return");
   b.replaceInstruction(i,new BuilderInstruction11x(Opcode.RETURN,r));b.removeInstruction(i+2);b.removeInstruction(i+1);hits++;
  }
  req(hits==1 && MergePayloads.hash(repl(t,b)).equals(MergePayloads.hash(seed.get(Transform1928.INITIAL))),"Original startup fallback not preserved exactly");
  var st=ms.get(Transform1928.STORE);var si=ins(st);req(si.size()==2&&ref(si.get(0)).equals(Transform1928.H+"->remember(Z)V")&&si.get(1).getOpcode()==Opcode.RETURN_VOID,"Native setter must only call durable helper");
  req(((RegisterRangeInstruction)si.get(0)).getStartRegister()==st.getImplementation().getRegisterCount()-1,"Native setter argument mismatch");
  Method life=ms.get(Transform1928.LIFECYCLE),nativeLife=seed.get(Transform1928.LIFECYCLE);
  var lb=new MutableMethodImplementation(life.getImplementation());var nb=new MutableMethodImplementation(nativeLife.getImplementation());
  req(ref(lb.getInstructions().get(3)).equals(snapshot),"Lifecycle snapshot hook");
  int n=-1;for(int i=0;i<nb.getInstructions().size();i++)if(ref(nb.getInstructions().get(i)).equals(Transform1928.STORE))n=i;
  req(n>=0,"Native lifecycle store anchor");for(int i=n;i>=0;i--)nb.removeInstruction(i);for(int i=3;i>=0;i--)lb.removeInstruction(i);
  req(MergePayloads.hash(repl(life,lb)).equals(MergePayloads.hash(repl(nativeLife,nb))),"Independent lifecycle settings changed");
  Method ex=rs.get(Transform1928.EXIT);b=new MutableMethodImplementation(ex.getImplementation());req(ref(b.getInstructions().get(0)).equals(snapshot),"Snapshot not before screen closing");b.removeInstruction(0);
  req(MergePayloads.hash(repl(ex,b)).equals(MergePayloads.hash(old.get(Transform1928.EXIT))),"Exit logic changed after snapshot");
  for(var e:old.entrySet())if(!e.getKey().equals(Transform1928.EXIT))req(MergePayloads.hash(e.getValue()).equals(MergePayloads.hash(rs.get(e.getKey()))),"Unrelated runtime changed "+e.getKey());
  int commit=0,apply=0;for(var m:runtime.get(Transform1928.H).getMethods())if(m.getImplementation()!=null)for(var i:m.getImplementation().getInstructions()){
   String r=ref(i);if(r.equals("Landroid/content/SharedPreferences$Editor;->commit()Z"))commit++;if(r.equals("Landroid/content/SharedPreferences$Editor;->apply()V"))apply++;
   req(!r.contains("->switchCamera(")&&!r.contains("->startPreview(")&&!r.contains("->openCamera("),"Persistence helper must not start/switch cameras");
  }req(commit==1&&apply==0,"Small preference durability policy");
  int abi=0;
  if(a.length==5){
   var stock=MergePayloads.classes(a[4]);var nativeMethods=MergePayloads.methods(stock.values());var fields=new TreeMap<String,Field>();
   for(var c:stock.values())for(var f:c.getFields())fields.put(DexFormatter.INSTANCE.getFieldDescriptor(f),f);
   for(String id:Transform1928.TARGETS)req(MergePayloads.hash(seed.get(id)).equals(MergePayloads.hash(nativeMethods.get(id))),"Seed must match original APK "+id);
   for(var m:runtime.get(Transform1928.H).getMethods())if(m.getImplementation()!=null)for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction ri){
    if(ri.getReference() instanceof MethodReference r&&r.getDefiningClass().startsWith("Li/f/")){
     var actual=nativeMethods.get(DexFormatter.INSTANCE.getMethodDescriptor(r));req(actual!=null&&(actual.getAccessFlags()&1)!=0,"Unknown/nonpublic native method "+r);abi++;
    }else if(ri.getReference() instanceof FieldReference r&&r.getDefiningClass().startsWith("Li/f/")){
     var actual=fields.get(DexFormatter.INSTANCE.getFieldDescriptor(r));req(actual!=null&&(actual.getAccessFlags()&9)==9,"Unknown/nonpublic/nonstatic native field "+r);abi++;
    }
   }
   // The native startup still feeds the remembered getter into F(front), and
   // the special copyright-camera branch remains outside the getter unchanged.
   var start=nativeMethods.get("Li/f/l/n/q/y/c;->x()V");boolean getter=false,field=false,special=false;
   for(var i:start.getImplementation().getInstructions()){
    String r=ref(i);getter|=r.equals(Transform1928.INITIAL);field|=r.equals("Li/f/l/k;->F(Z)V");
    if(i instanceof ReferenceInstruction ri&&ri.getReference() instanceof StringReference sr)special|=sr.getString().equals("publish_camera_copyright");
   }req(getter&&field&&special,"Native startup/copyright contract");req(!ms.containsKey("Li/f/l/n/q/y/c;->x()V"),"Startup coordinator unexpectedly changed");
  }
  System.out.println("PASS1928 native bridges=3; startup fallback and independent lifecycle suffix verified; pre-close snapshot=1; exit inverse matches baseline; unrelated runtime unchanged; synchronous preference writer=1; native ABI references="+abi+"; camera startup coordinator unchanged");
 }
}
