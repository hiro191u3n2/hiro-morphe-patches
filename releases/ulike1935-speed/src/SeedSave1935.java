import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.Method;
/** Extract only the reviewed autosave dispatch and shutter admission methods. */
public final class SeedSave1935 {
 static final Set<String> TARGETS=Set.of("Li/o/a/b1/a/b/f/a;->c(II)V","Li/o/a/b1/a/g/y;->X(IZ)Z");
 public static void main(String[] args)throws Exception{
  MergePayloads.require(args.length==3,"STOCK_APK OUTPUT_DEX HASHES_TSV");var methods=MergePayloads.methods(MergePayloads.classes(args[0]).values());var selected=new TreeMap<String,Method>();var rows=new ArrayList<String>();
  for(String target:new TreeSet<>(TARGETS)){Method m=methods.get(target);MergePayloads.require(m!=null&&m.getImplementation()!=null,"Missing original save/shutter target "+target);selected.put(target,m);rows.add(target+"\t"+MergePayloads.hash(m));}
  MergePayloads.writeDex(Path.of(args[1]),MergePayloads.holders(selected));Files.write(Path.of(args[2]),rows);for(String row:rows)System.out.println(row);
 }
}
