import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.Method;
/** Extract exactly the reviewed native targets absent from the current payload. */
public final class SeedLayout1937 {
 public static void main(String[] args)throws Exception{
  MergePayloads.require(args.length==4,"STOCK_APK BASE_METHODS OUTPUT_DEX HASHES_TSV");
  var stock=MergePayloads.methods(MergePayloads.classes(args[0]).values());var baseline=MergePayloads.methods(MergePayloads.classes(args[1]).values());
  var selected=new TreeMap<String,Method>();var rows=new ArrayList<String>();
  for(String target:new TreeSet<>(Hooks1937.NATIVE)){
   Method m=stock.get(target);MergePayloads.require(m!=null&&m.getImplementation()!=null,"Missing original layout native target "+target);
   if(baseline.containsKey(target))continue;
   selected.put(target,m);rows.add(target+"\t"+MergePayloads.hash(m));
  }
  MergePayloads.require(!selected.isEmpty(),"No new native target seed is needed");
  MergePayloads.writeDex(Path.of(args[2]),MergePayloads.holders(selected));Files.write(Path.of(args[3]),rows);for(String row:rows)System.out.println(row);
 }
}
