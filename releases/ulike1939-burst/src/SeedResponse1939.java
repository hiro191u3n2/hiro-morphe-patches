import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.Method;
/** Extract just the two original manual shutter methods from the pinned stock APK. */
public final class SeedResponse1939 {
 public static void main(String[] args)throws Exception {
  MergePayloads.require(args.length==4,"STOCK_APK BASE_METHODS OUTPUT_DEX HASHES_TSV");
  var methods=MergePayloads.methods(MergePayloads.classes(args[0]).values());
  var baseline=MergePayloads.methods(MergePayloads.classes(args[1]).values());
  var selected=new TreeMap<String,Method>();var rows=new ArrayList<String>();
  for(String id:new TreeSet<>(ResponseHooks1939.NATIVE)) {
   Method method=methods.get(id);MergePayloads.require(method!=null&&method.getImplementation()!=null,"Missing shutter original "+id);
   ResponseHooks1939.verify(method,ResponseHooks1939.repair(method));
   if(baseline.containsKey(id))continue;
   selected.put(id,method);rows.add(id+"\t"+MergePayloads.hash(method));
  }
  MergePayloads.require(selected.size()==2,"Expected both new shutter methods");
  MergePayloads.writeDex(Path.of(args[2]),MergePayloads.holders(selected));Files.write(Path.of(args[3]),rows);
  for(String row:rows)System.out.println(row);
 }
}
