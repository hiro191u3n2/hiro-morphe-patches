import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;

/** Full original multidex inventory. Outputs identifiers, offsets and method hashes only. */
public final class Inventory169 {
 public static void main(String[] args)throws Exception {
  var classes=MergePayloads.classes(args[0]);var rows=new ArrayList<String>();var definitions=new ArrayList<String>();var restoration=new ArrayList<String>();
  Set<String> restoreOwners=Set.of("Lcom/ss/android/vesdk/TERecordFactory;","Li/f/l/n/h$e;","Li/f/l/n/q/y/c$a;","Li/f/l/n/q/y/c;","Lcom/bytedance/corecamera/camera/basic/PureCameraFragment$d;","Li/f/l/n/q/i;","Li/o/a/b1/a/g/y$d;");
  Set<String> backends=new TreeSet<>();backends.add("Lcom/ss/android/vesdk/TERecorderBase;");
  boolean more;do {more=false;for(var c:classes.values())if(backends.contains(c.getSuperclass()))more|=backends.add(c.getType());}while(more);
  for(var c:classes.values())for(var m:c.getMethods()) {
   if(m.getImplementation()==null)continue;
   String source=MergePayloads.id(m);int offset=0;
   String name=m.getName(),owner=m.getDefiningClass();
   if((owner.equals("Lcom/ss/android/vesdk/VERecorder;") || backends.contains(owner)) &&
       (name.equals("<init>") || name.equals("init") || name.contains("Preview") || name.equals("onPause") || name.equals("onResume") || name.equals("onDestroy") || name.contains("Surface") || name.startsWith("surface") || name.equals("releaseInteralRecorder")))
       definitions.add(source+"\t"+MergePayloads.hash(m));
   for(var ins:m.getImplementation().getInstructions()) {
    if(ins instanceof ReferenceInstruction ri) {
     var r=ri.getReference();String target=null,kind=null;
     if(r instanceof MethodReference mr) {
      owner=mr.getDefiningClass();name=mr.getName();
      if(owner.equals("Lcom/ss/android/medialib/RecordInvoker;") && (name.contains("initBeautyPlay")||name.equals("nativeCreate")||name.equals("<init>")||name.equals("onDestroy")||name.equals("onPause")||name.contains("Surface")||name.equals("startPlay")||name.equals("stopPlay")))kind="native_lifecycle";
      if(owner.equals("Lcom/ss/android/vesdk/VERecorder;")&&(name.equals("<init>")||name.contains("Preview")||name.startsWith("on")||name.contains("Surface")||name.equals("init")))kind="recorder_lifecycle";
      if(backends.contains(owner)&&(name.equals("<init>")||name.contains("Preview")||name.startsWith("on")||name.contains("Surface")||name.equals("init")||name.equals("releaseInteralRecorder")))kind="backend_lifecycle";
      if(name.equals("onNativeInit")||name.equals("onNativeInitCallBack")||name.contains("Composer"))kind="composer_or_init";
      if(kind!=null)target=DexFormatter.INSTANCE.getMethodDescriptor(mr);
      if(restoreOwners.contains(m.getDefiningClass()))restoration.add(source+"\t"+offset+"\t"+DexFormatter.INSTANCE.getMethodDescriptor(mr)+"\t"+MergePayloads.hash(m));
     } else if(r instanceof FieldReference fr && fr.getDefiningClass().equals("Lcom/ss/android/medialib/RecordInvoker;")&&fr.getName().equals("mHandler")&&ins.getOpcode().name().startsWith("IPUT")) {kind="handle_write";target=DexFormatter.INSTANCE.getFieldDescriptor(fr);}
     if(kind!=null)rows.add(kind+"\t"+source+"\t"+offset+"\t"+ins.getOpcode().name()+"\t"+target+"\t"+MergePayloads.hash(m));
    }offset+=ins.getCodeUnits();
   }
  }
  Collections.sort(rows);Files.write(Path.of(args[1]),rows);
  Files.write(Path.of(args[1]+".backends"),backends);
  Collections.sort(definitions);Files.write(Path.of(args[1]+".definitions"),definitions);
  Collections.sort(restoration);Files.write(Path.of(args[1]+".restoration"),restoration);
  System.out.println("Inventory classes="+classes.size()+", references="+rows.size()+", backend classes="+backends.size());
 }
}
