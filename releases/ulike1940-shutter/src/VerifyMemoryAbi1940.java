import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
/** Check new application-memory reflection endpoints on actual stock/applied APKs. */
public final class VerifyMemoryAbi1940 {
 static int checks;
 static void need(boolean b,String message){checks++;if(!b)throw new IllegalStateException(message);}
 static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
 static List<String> refs(Method m){var rows=new ArrayList<String>();if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())rows.add(ref(i));return rows;}
 static void verify(String path)throws Exception {
  var all=VerifyFrontAbi1931.load(path);checks=0;String type="Li/n/c/a/b/g;";
  ClassDef core=all.get(type);need(core!=null&&(core.getAccessFlags()&1)!=0,"Public application context holder");
  Method singleton=VerifyFrontAbi1931.method(all,type+"->j()"+type),context=VerifyFrontAbi1931.method(all,type+"->i()Landroid/content/Context;");
  need((singleton.getAccessFlags()&9)==9,"getMethod j must be public/static/no-arg with exact singleton type");
  need((context.getAccessFlags()&9)==1,"getMethod i must be public/instance/no-arg returning Android Context");
  boolean applied=all.containsKey("Lcom/hiro/ulike/CaptureMemory1940;");
  if(applied){
   ClassDef helper=all.get("Lcom/hiro/ulike/CaptureMemory1940;");var values=new TreeSet<String>();var calls=new TreeSet<String>();
   for(Method m:helper.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions()){
    if(i instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s)values.add(s.getString());
    String target=ref(i);calls.add(target);need(!target.contains("Landroid/graphics/Bitmap;->")&&!target.contains("->compress("),"Memory helper cannot change photograph pixels/encoding");
   }
   need(values.containsAll(Set.of("i.n.c.a.b.g","j","i")),"Production reflection names match inspected original endpoints");
   need(calls.contains("Landroid/app/ActivityManager;->getMemoryInfo(Landroid/app/ActivityManager$MemoryInfo;)V"),"Production includes OS memory pressure check");
   need(calls.contains("Landroid/os/Debug;->getPss()J"),"Production includes process footprint check");
   need(calls.contains("Landroid/os/Debug;->getNativeHeapAllocatedSize()J"),"Production includes native heap usage");
  }
  System.out.println("PASS memory1940 actual ABI checks="+checks+" applied="+applied+"; reflection context endpoints and memory-only helper; no device execution");
 }
 public static void main(String[] args)throws Exception {if(args.length!=1)throw new IllegalArgumentException("STOCK_OR_APPLIED_APK");verify(args[0]);}
}
