import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
/** Preserve every non-ULike class and replace only the ULike patch namespace. */
public final class MergePatchDex {
  public static void main(String[] args) throws Exception {
    if(args.length!=3)throw new IllegalArgumentException("old.dex new-ulike.dex output.dex");
    var original=DexFileFactory.loadDexFile(args[0],Opcodes.forApi(26));
    var replacement=DexFileFactory.loadDexFile(args[1],Opcodes.forApi(26));
    String prefix="Lapp/hiro/ulike/patches/";
    TreeMap<String,ClassDef> all=new TreeMap<>();Set<String> oldTypes=new TreeSet<>(),newTypes=new TreeSet<>();
    for(ClassDef c:original.getClasses()) {if(c.getType().startsWith(prefix))oldTypes.add(c.getType());else all.put(c.getType(),c);}
    for(ClassDef c:replacement.getClasses()) {
      if(!c.getType().startsWith(prefix))throw new IllegalStateException("Unexpected class: "+c.getType());
      if(all.put(c.getType(),c)!=null)throw new IllegalStateException("Duplicate class"); newTypes.add(c.getType());
    }
    if(!oldTypes.equals(newTypes))throw new IllegalStateException("Class-set mismatch: "+oldTypes+" vs "+newTypes);
    DexPool out=new DexPool(original.getOpcodes());for(ClassDef c:all.values())out.internClass(c);
    FileDataStore store=new FileDataStore(new java.io.File(args[2]));
    try {out.writeTo(store);} finally {store.close();}
    System.out.println("Merged "+all.size()+" classes; replaced "+newTypes.size()+" ULike patch classes; other classes retained.");
  }
}
