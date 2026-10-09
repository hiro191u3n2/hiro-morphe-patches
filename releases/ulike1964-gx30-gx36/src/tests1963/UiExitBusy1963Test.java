import java.nio.file.*;
import java.util.*;
import java.lang.reflect.Field;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
public final class UiExitBusy1963Test {
 static int checks;
 static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static String ref(Instruction op){return op instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static Map<String,Integer> references(Method method){var refs=new TreeMap<String,Integer>();for(Instruction op:method.getImplementation().getInstructions()){String r=ref(op);if(!r.isEmpty())refs.merge(r,1,Integer::sum);}return refs;}
 public static class Parent {private boolean delivering;}
 public static final class State extends Parent {private boolean selected;private Object pending;private Object captureCallback;}
 public static void main(String[] args)throws Exception {
  var classes=MergePayloads.classes(args[0]);ClassDef before=classes.get(TransformExitBusy1963.TYPE);
  check(before!=null&&MergePayloads.classHash(before).equals(TransformExitBusy1963.CLASS_PIN),"Exact actual published .62 ExitBusy class");
  var methods=MergePayloads.methods(List.of(classes.get(TransformExitBusy1963.CACHE)));
  check(methods.containsKey(TransformExitBusy1963.TYPE_CACHE)&&methods.containsKey(TransformExitBusy1963.FIELD_CACHE),"Redirect signatures already exist in retained .62 cache");
  check((methods.get(TransformExitBusy1963.TYPE_CACHE).getAccessFlags()&9)==9&&(methods.get(TransformExitBusy1963.FIELD_CACHE).getAccessFlags()&9)==9,"Both existing cache targets are public static");
  check(references(methods.get(TransformExitBusy1963.TYPE_CACHE)).getOrDefault(TransformExitBusy1963.CLASS_LOOKUP,0)==1,"Existing type cache retains one-string same-loader Class.forName semantics");
  ClassDef patched=TransformExitBusy1963.patch(before);TransformExitBusy1963.verify(before,patched);
  Path output=Path.of(args[1]);MergePayloads.writeDex(output,List.of(patched));
  ClassDef after=MergePayloads.classes(output.toString()).get(TransformExitBusy1963.TYPE);
  TransformExitBusy1963.verify(before,after);check(true,"Serialized emitted busy-guard dex verifies exact inverse");
  var old=MergePayloads.methods(List.of(before));var next=MergePayloads.methods(List.of(after));
  var changed=new TreeSet<String>();for(var row:old.entrySet())if(!MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(next.get(row.getKey()))))changed.add(row.getKey());
  check(changed.equals(TransformExitBusy1963.METHOD_PINS.keySet()),"Changed runtime inventory exactly captureBusy and value");
  int originalDiscoveries=0,cachedDiscoveries=0;
  for(String id:changed){
   Method prior=old.get(id),now=next.get(id);var a=references(prior);var b=references(now);
   int discoveries=a.getOrDefault(TransformExitBusy1963.CLASS_LOOKUP,0)+a.getOrDefault(TransformExitBusy1963.FIELD_LOOKUP,0);
   originalDiscoveries+=discoveries;cachedDiscoveries+=b.getOrDefault(TransformExitBusy1963.TYPE_CACHE,0)+b.getOrDefault(TransformExitBusy1963.FIELD_CACHE,0);
   check(!b.containsKey(TransformExitBusy1963.CLASS_LOOKUP)&&!b.containsKey(TransformExitBusy1963.FIELD_LOOKUP),"Repeated discovery removed from "+id);
   a.remove(TransformExitBusy1963.CLASS_LOOKUP);a.remove(TransformExitBusy1963.FIELD_LOOKUP);
   b.remove(TransformExitBusy1963.TYPE_CACHE);b.remove(TransformExitBusy1963.FIELD_CACHE);
   check(a.equals(b),"All live field reads, timing/capture state and exit-routing references retained "+id);
   check(prior.getImplementation().getRegisterCount()==now.getImplementation().getRegisterCount(),"Register ABI unchanged "+id);
   check(prior.getImplementation().getTryBlocks().size()==now.getImplementation().getTryBlocks().size(),"Exception handler inventory unchanged "+id);
  }
  check(originalDiscoveries==4&&cachedDiscoveries==4,"Exactly four discovery instructions redirected");
  check(MergePayloads.classHash(before).equals(MergePayloads.classHash(TransformExitBusy1963.transform(after,true))),"Whole class inverse keeps every branch/monitor/handler/debug item");
  State state=new State();Field selected=com.hiro.ulike.ReflectionCache1945.declaredField(State.class,"selected");
  check(selected==com.hiro.ulike.ReflectionCache1945.declaredField(State.class,"selected"),"Repeated discovery reuses metadata field");
  check(!selected.getBoolean(state),"Initial live selected value read");selected.setBoolean(state,true);
  check(selected.getBoolean(state),"Cached metadata observes changed live capture flag");
  Field pending=com.hiro.ulike.ReflectionCache1945.declaredField(State.class,"pending");Object one=new Object(),two=new Object();
  pending.set(state,one);check(pending.get(state)==one,"Live callback slot read");pending.set(state,two);
  check(com.hiro.ulike.ReflectionCache1945.declaredField(State.class,"pending").get(state)==two,"Cached discovery never retains callback value");
  boolean rejected=false;try{com.hiro.ulike.ReflectionCache1945.declaredField(State.class,"delivering");}catch(NoSuchFieldException expected){rejected=true;}
  check(rejected,"Exact-owner declaredField does not accidentally search inherited state");
  check(com.hiro.ulike.ReflectionCache1945.type(State.class.getName())==State.class,"Existing type lookup retains native class identity");
  rejected=false;try{TransformExitBusy1963.patch(after);}catch(RuntimeException expected){rejected=true;}
  check(rejected,"Already-patched or different-version busy guard rejected");
  System.out.println("UI_EXITBUSY1963_ASSERTIONS="+checks);
 }
}
