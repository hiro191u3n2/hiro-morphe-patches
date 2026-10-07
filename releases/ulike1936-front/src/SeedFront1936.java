import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.Method;
/** Extract only the reviewed accepted camera switch method. */
public final class SeedFront1936 {
 static final Set<String> TARGETS=Set.of("Li/f/l/n/q/y/c;->g0(Z)V");
 public static void main(String[] args)throws Exception{
  MergePayloads.require(args.length==3,"STOCK_APK OUTPUT_DEX HASHES_TSV");var methods=MergePayloads.methods(MergePayloads.classes(args[0]).values());var selected=new TreeMap<String,Method>();var rows=new ArrayList<String>();
  for(String target:new TreeSet<>(TARGETS)){Method m=methods.get(target);MergePayloads.require(m!=null&&m.getImplementation()!=null,"Missing original accepted switch target "+target);selected.put(target,m);rows.add(target+"\t"+MergePayloads.hash(m));}
  MergePayloads.writeDex(Path.of(args[1]),MergePayloads.holders(selected));Files.write(Path.of(args[2]),rows);for(String row:rows)System.out.println(row);
 }
}
