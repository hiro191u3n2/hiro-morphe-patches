import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.nio.file.*;
import java.util.*;

/** Replace only the reviewed UI component, preserving every other provider definition. */
public final class MergeProviders {
  static final Opcodes OPS=Opcodes.forApi(26);
  static Map<String,ClassDef> read(String p)throws Exception {
    Map<String,ClassDef> m=new TreeMap<>();
    for(ClassDef c:DexFileFactory.loadDexFile(Path.of(p).toFile(),OPS).getClasses())
      if(m.put(c.getType(),c)!=null)throw new IllegalStateException("Duplicate "+c.getType());
    return m;
  }
  static byte[] canonical(ClassDef c)throws Exception {
    MemoryDataStore out=new MemoryDataStore();
    try {
      DexPool.writeTo(out,new ImmutableDexFile(OPS,Collections.singleton(c)));
      return Arrays.copyOf(out.getBuffer(),out.getSize());
    } finally {out.close();}
  }
  public static void main(String[] a)throws Exception {
    if(a.length!=5)throw new IllegalArgumentException("base addon old-ui output report");
    Map<String,ClassDef> base=read(a[0]),add=read(a[1]),old=read(a[2]),all=new TreeMap<>(base);
    for(String n:old.keySet()) {
      if(!n.startsWith("Lapp/hiro/newxui/patches/") || !base.containsKey(n)
          || !Arrays.equals(canonical(old.get(n)),canonical(base.get(n))))
        throw new IllegalStateException("Baseline UI component differs "+n);
      all.remove(n);
    }
    for(ClassDef c:add.values()) {
      if(!c.getType().startsWith("Lapp/hiro/newxui/patches/")||all.put(c.getType(),c)!=null)
        throw new IllegalStateException("Unsafe provider collision "+c.getType());
    }
    DexPool.writeTo(a[3],new ImmutableDexFile(OPS,all.values()));
    Map<String,ClassDef> loaded=read(a[3]);
    if(!loaded.keySet().equals(all.keySet()))throw new IllegalStateException("Class set differs");
    for(String n:all.keySet())if(!Arrays.equals(canonical(all.get(n)),canonical(loaded.get(n))))
      throw new IllegalStateException("Class content differs "+n);
    String report="{\"result\":\"PASS\",\"baseline_provider_classes\":"+base.size()+
      ",\"old_ui_provider_classes\":"+old.size()+",\"new_ui_provider_classes\":"+add.size()+
      ",\"preserved_non_ui_provider_classes\":"+(base.size()-old.size())+
      ",\"existing_non_ui_provider_classes_preserved\":true,\"serialization_verified\":true}\n";
    Files.writeString(Path.of(a[4]),report);System.out.print(report);
  }
}
