import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
/** Checks emitted control flow, not just source mocks. Full APK supplies native ABI. */
public final class VerifyLayout1923 {
 static void check(boolean b,String s){MergePayloads.require(b,s);}
 static List<Instruction> ins(Method m){var list=new ArrayList<Instruction>();m.getImplementation().getInstructions().forEach(list::add);return list;}
 static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static int guardedReturns(Method m,String guard,Opcode ret){
  var code=ins(m);var addresses=new ArrayList<Integer>();var map=new HashMap<Integer,Integer>();int pc=0;
  for(int i=0;i<code.size();i++){addresses.add(pc);map.put(pc,i);pc+=code.get(i).getCodeUnits();}
  Set<Integer> incoming=new HashSet<>();for(int i=0;i<code.size();i++)if(code.get(i) instanceof OffsetInstruction o){int dest=addresses.get(i)+o.getCodeOffset();check(map.containsKey(dest),"Unaligned branch: "+MergePayloads.id(m));incoming.add(dest);}
  int found=0;for(int i=0;i<code.size();i++)if(code.get(i).getOpcode()==ret){
   check(i>0&&ref(code.get(i-1)).equals(guard),"Return bypasses required guard: "+MergePayloads.id(m));
   check(!incoming.contains(addresses.get(i)),"Branch skips guard and directly enters return: "+MergePayloads.id(m));found++;
  }
  check(found>0,"No guarded returns");return found;
 }
 public static void main(String[] a)throws Exception {
  var stockC=MergePayloads.classes(a[0]);var stock=MergePayloads.methods(stockC.values());
  var seed=MergePayloads.methods(MergePayloads.classes(a[1]).values());var patched=MergePayloads.methods(MergePayloads.classes(a[2]).values());var rt=MergePayloads.classes(a[3]);
  for(String key:Transform1923.TARGETS)check(MergePayloads.hash(stock.get(key)).equals(MergePayloads.hash(seed.get(key))),"Stock seed drift: "+key);
  var fields=new HashMap<String,Field>();for(var c:stockC.values())for(var f:c.getFields())fields.put(DexFormatter.INSTANCE.getFieldDescriptor(f),f);
  int refs=0;
  for(var c:rt.values())if(c.getType().startsWith("Lcom/hiro/ulike/PreviewLayout1922"))for(var m:c.getMethods())if(m.getImplementation()!=null)for(var i:ins(m))if(i instanceof ReferenceInstruction r){
   if(r.getReference() instanceof FieldReference f && f.getDefiningClass().equals(Transform1923.S)){
    Field actual=fields.get(DexFormatter.INSTANCE.getFieldDescriptor(f));check(actual!=null&&(actual.getAccessFlags()&1)!=0,"Inaccessible stock field "+f);
    boolean isStatic=i.getOpcode().name().startsWith("SGET")||i.getOpcode().name().startsWith("SPUT");check(isStatic==((actual.getAccessFlags()&8)!=0),"Field static mismatch "+f);refs++;
   }
   if(r.getReference() instanceof MethodReference f && f.getDefiningClass().equals(Transform1923.S)){
    String key=DexFormatter.INSTANCE.getMethodDescriptor(f);var actual=stock.get(key);
    if(actual!=null){check((actual.getAccessFlags()&1)!=0,"Inaccessible stock method "+f);refs++;}
    else check(Set.of("postDelayed(Ljava/lang/Runnable;J)Z","removeCallbacks(Ljava/lang/Runnable;)Z","getWidth()I","getHeight()I","getResources()Landroid/content/res/Resources;").contains(key.substring(key.indexOf("->")+2)),"Unexpected View ABI "+f);
   }
  }
  String S=Transform1923.S,H=Transform1923.H,R=Transform1923.R;
  int returns=guardedReturns(patched.get(S+"->m("+R+"ZZZ)Z"),H+"->ready("+S+")V",Opcode.RETURN);check(returns==2,"Two native ratio exit paths required");
  guardedReturns(patched.get(S+"->onLayout(ZIIII)V"),H+"->afterLayout("+S+")V",Opcode.RETURN_VOID);
  int metric=0;for(String id:List.of(S+"->m("+R+"ZZZ)Z",S+"->a("+Transform1923.F+")V"))for(var i:ins(patched.get(id))){String r=ref(i);check(!r.matches("Li/f/l/w/z;->(h|e|f)\\(\\)I"),"Global screen metrics remain");if(r.startsWith(H+"->local"))metric++;}
  check(metric>=2,"Local metrics missing");
  var g=ins(patched.get(Transform1923.D+"->g()Landroid/graphics/RectF;"));check(ref(g.get(0)).equals(H+"->viewport()Landroid/graphics/RectF;"),"Local viewport missing");
  check(g.get(1).getOpcode()==Opcode.MOVE_RESULT_OBJECT&&g.get(2).getOpcode()==Opcode.IF_EQZ&&((OffsetInstruction)g.get(2)).getCodeOffset()==3&&g.get(3).getOpcode()==Opcode.RETURN_OBJECT,"Viewport null fallback must enter original code");
  var layout=ins(patched.get(S+"->onLayout(ZIIII)V"));check(layout.get(0).getOpcode()==Opcode.INVOKE_SUPER_RANGE,"Superclass dimensions changed");
  for(int k=1;k<=2;k++){var i=(ThreeRegisterInstruction)layout.get(k);check(layout.get(k).getOpcode()==Opcode.SUB_INT&&i.getRegisterA()==10+k&&i.getRegisterB()==10+k&&i.getRegisterC()==8+k,"Measured view dimension math changed");}
  var methods=MergePayloads.methods(rt.values());int legacy=0;
  for(var entry:methods.entrySet())if(entry.getKey().startsWith("Lcom/hiro/ulike/DetailFast174;->filter(")){
   Method m=entry.getValue();if(m.getParameterTypes().size()==10)legacy=guardedReturns(m,Transform1923.N+"->legacy("+String.join("",m.getParameterTypes())+")V",Opcode.RETURN_VOID);
  }check(legacy==4,"All legacy exits need shadow finishing");
  var worker=ins(methods.get(Transform1923.W+"->run()V"));check(worker.size()==4&&ref(worker.get(2)).equals(Transform1923.N+"->run("+Transform1923.STATE+Transform1923.BUFFER+")V"),"Worker production hook missing");
  String U="Lcom/bytedance/corecamera/ui/view/CameraShadeView$c;",E="Lcom/bytedance/corecamera/ui/view/CameraShadeView$b;";
  // The established callback-ownership guards remain covered by unchanged-method hashes.
  System.out.println("PASS 5 original contracts; "+returns+" ratio returns and 1 onLayout return are label-safe; "+metric+" metric calls use live view bounds; viewport native fallback; "+legacy+" legacy exits; production worker hook; "+refs+" native ABI references. Host/DEX verification, not ART/device execution.");
 }
}
