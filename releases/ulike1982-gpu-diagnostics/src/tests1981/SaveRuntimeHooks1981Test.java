import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.immutable.*;

public final class SaveRuntimeHooks1981Test {
 public static void main(String[] a)throws Exception{
  Map<String,ClassDef> before=MergePayloads.classes(a[0]),after=new TreeMap<>(before);int assertions=0;
  for(String type:SaveRuntimeHooks1981.TYPES){
   after.put(type,SaveRuntimeHooks1981.patch(before.get(type)));assertions++;
   try{SaveRuntimeHooks1981.patch(after.get(type));throw new AssertionError("double application accepted");}catch(IllegalStateException expected){assertions++;}
  }
  SaveRuntimeHooks1981.verify(before,after);assertions+=8;
  MergePayloads.writeDex(Path.of(a[1]),after.values());var emitted=MergePayloads.classes(a[1]);
  SaveRuntimeHooks1981.verify(before,emitted);assertions+=8;
  for(String type:before.keySet())if(!SaveRuntimeHooks1981.owned(type)){
   if(!MergePayloads.classHash(before.get(type)).equals(MergePayloads.classHash(emitted.get(type))))throw new AssertionError("unrelated helper changed");assertions++;
  }
  // The original class in the patched map must fail the exact gate inventory.
  for(String type:SaveRuntimeHooks1981.TYPES){
   var bad=new TreeMap<>(emitted);bad.put(type,before.get(type));
   try{SaveRuntimeHooks1981.verify(before,bad);throw new AssertionError("missing fence accepted");}catch(IllegalStateException expected){assertions++;}
  }
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"four_runtime_bodies_inverse_identical\":true,\"serialized_hook_positions_verified\":true,\"physical_android_tested\":false}");
 }
}
