import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.Method;
/** Extract only new1940 native method inputs from the pinned original APK. */
public final class Seed1940 {
 public static void main(String[] args)throws Exception {
  MergePayloads.require(args.length==4,"STOCK_APK BASE_METHODS OUTPUT_DEX HASHES_TSV");
  var methods=MergePayloads.methods(MergePayloads.classes(args[0]).values());
  var baseline=MergePayloads.methods(MergePayloads.classes(args[1]).values());
  var selected=new TreeMap<String,Method>();var rows=new ArrayList<String>();
  for(String id:new TreeSet<>(Hooks1940.NATIVE)) {
   if(baseline.containsKey(id))continue;
   Method method=methods.get(id);MergePayloads.require(method!=null&&method.getImplementation()!=null,"Missing1940 original "+id);
   Hooks1940.verify(method,Hooks1940.repair(method));
   selected.put(id,method);rows.add(id+"\t"+MergePayloads.hash(method));
  }
  MergePayloads.require(!selected.isEmpty(),"No new native inputs; omit stock seed if all1940 hooks existed in baseline");
  MergePayloads.writeDex(Path.of(args[2]),MergePayloads.holders(selected));Files.write(Path.of(args[3]),rows);
  for(String row:rows)System.out.println(row);
 }
}
