import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
/** Extract only the unchanged canonical stop/switch/destroy methods from stock. */
public final class Seed1931 {
 public static void main(String[]a)throws Exception{
  var source=MergePayloads.methods(MergePayloads.classes(a[0]).values());var out=new TreeMap<String,Method>();
  for(String key:Transform1931.CANCEL){Method m=source.get(key);if(m==null)throw new IllegalStateException("Missing stock method "+key);out.put(key,m);}
  MergePayloads.writeDex(Path.of(a[1]),MergePayloads.holders(out));
  for(var e:out.entrySet())System.out.println(e.getKey()+"\t"+MergePayloads.hash(e.getValue()));
 }
}
