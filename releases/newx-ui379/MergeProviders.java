import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.nio.file.*;
import java.util.*;

/** Preserve all existing provider definitions while adding the Japanese addon. */
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
    if(a.length!=4)throw new IllegalArgumentException("base addon output report");
    Map<String,ClassDef> base=read(a[0]),add=read(a[1]),all=new TreeMap<>(base);
    for(ClassDef c:add.values()) {
      if(!c.getType().startsWith("Lapp/hiro/")||all.put(c.getType(),c)!=null)
        throw new IllegalStateException("Unsafe provider collision "+c.getType());
    }
    DexPool.writeTo(a[2],new ImmutableDexFile(OPS,all.values()));
    Map<String,ClassDef> loaded=read(a[2]);
    if(!loaded.keySet().equals(all.keySet()))throw new IllegalStateException("Class set differs");
    for(String n:all.keySet())if(!Arrays.equals(canonical(all.get(n)),canonical(loaded.get(n))))
      throw new IllegalStateException("Class content differs "+n);
    String report="{\"result\":\"PASS\",\"baseline_provider_classes\":"+base.size()+
      ",\"added_provider_classes\":"+add.size()+",\"existing_provider_classes_preserved\":true,\"serialization_verified\":true}\n";
    Files.writeString(Path.of(a[3]),report);System.out.print(report);
  }
}
