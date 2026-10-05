import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
public final class Seed1922 {
 public static void main(String[] a)throws Exception {
  var all=MergePayloads.methods(MergePayloads.classes(a[0]).values());
  var chosen=new TreeMap<String,Method>();
  for(String key:Transform1922.TARGETS) {
   Method m=all.get(key);MergePayloads.require(m!=null,"Missing original method "+key);chosen.put(key,m);
  }
  MergePayloads.writeDex(Path.of(a[1]),MergePayloads.holders(chosen));
  for(var e:chosen.entrySet())System.out.println(e.getKey()+"\t"+MergePayloads.hash(e.getValue()));
 }
}
