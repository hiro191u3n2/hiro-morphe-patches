import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.nio.file.*;
import java.util.*;

/** Narrow provider-only update; all unrelated DEX classes must remain identical. */
public final class MergeJapanese {
  static final Opcodes OPS=Opcodes.forApi(26);
  static boolean allowed(String name) {
    return name.startsWith("Lapp/hiro/newx/patches/NewXJapanese");
  }
  static Map<String,ClassDef> read(String path)throws Exception {
    Map<String,ClassDef> result=new TreeMap<>();
    for(ClassDef c:DexFileFactory.loadDexFile(Path.of(path).toFile(),OPS).getClasses())
      if(result.put(c.getType(),c)!=null)throw new IllegalStateException("Duplicate "+c.getType());
    return result;
  }
  static byte[] canonical(ClassDef c)throws Exception {
    MemoryDataStore out=new MemoryDataStore();
    try {
      DexPool.writeTo(out,new ImmutableDexFile(OPS,Collections.singleton(c)));
      return Arrays.copyOf(out.getBuffer(),out.getSize());
    } finally {out.close();}
  }
  static String strings(List<String> values) {
    StringJoiner result=new StringJoiner(",","[","]");
    for(String value:values)result.add("\""+value+"\"");
    return result.toString();
  }
  public static void main(String[] args)throws Exception {
    if(args.length!=4)throw new IllegalArgumentException("base japanese output report");
    Map<String,ClassDef> base=read(args[0]),addon=read(args[1]),all=new TreeMap<>(base);
    Set<String> oldNames=new TreeSet<>(),newNames=new TreeSet<>();
    for(String name:base.keySet())if(allowed(name))oldNames.add(name);
    for(String name:addon.keySet())if(allowed(name))newNames.add(name);
    if(oldNames.isEmpty()||!oldNames.equals(newNames))throw new IllegalStateException("Target class set differs");
    List<String> changed=new ArrayList<>(),same=new ArrayList<>();
    for(Map.Entry<String,ClassDef> entry:addon.entrySet()) {
      String name=entry.getKey();
      if(!base.containsKey(name))throw new IllegalStateException("Unexpected addition "+name);
      if(Arrays.equals(canonical(base.get(name)),canonical(entry.getValue())))same.add(name);
      else {
        if(!allowed(name))throw new IllegalStateException("Unexpected replacement "+name);
        all.put(name,entry.getValue());changed.add(name);
      }
    }
    if(changed.isEmpty())throw new IllegalStateException("No provider change");
    DexPool.writeTo(args[2],new ImmutableDexFile(OPS,all.values()));
    Map<String,ClassDef> loaded=read(args[2]);
    if(!loaded.keySet().equals(base.keySet()))throw new IllegalStateException("Class set differs");
    int unrelated=0;
    for(String name:all.keySet()) {
      if(!Arrays.equals(canonical(all.get(name)),canonical(loaded.get(name))))
        throw new IllegalStateException("Serialization changed "+name);
      if(!changed.contains(name)) {
        if(!Arrays.equals(canonical(base.get(name)),canonical(loaded.get(name))))
          throw new IllegalStateException("Unchanged provider differs "+name);
        unrelated++;
      }
    }
    String report="{\"result\":\"PASS\",\"baseline_provider_classes\":"+base.size()+
      ",\"changed_provider_classes\":"+strings(changed)+",\"unchanged_component_classes\":"+strings(same)+
      ",\"preserved_provider_classes\":"+unrelated+",\"existing_unrelated_provider_classes_preserved\":true"+
      ",\"no_added_or_removed_classes\":true,\"serialization_verified\":true}\n";
    Files.writeString(Path.of(args[3]),report);System.out.print(report);
  }
}
