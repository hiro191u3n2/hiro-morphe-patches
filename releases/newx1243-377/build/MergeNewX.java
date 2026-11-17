import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.nio.file.*;
import java.util.*;

/** Add the three NewX providers without replacing any existing provider. */
public final class MergeNewX {
    private static final Opcodes OPS = Opcodes.forApi(26);
    private static void require(boolean b, String why) {
        if (!b) throw new IllegalStateException(why);
    }
    private static Map<String, ClassDef> read(String path) throws Exception {
        Map<String, ClassDef> result = new TreeMap<>();
        for (ClassDef c : DexFileFactory.loadDexFile(Path.of(path).toFile(), OPS).getClasses())
            require(result.put(c.getType(), c) == null, "Duplicate definition " + c.getType());
        return result;
    }
    private static byte[] canonical(ClassDef c) throws Exception {
        MemoryDataStore out = new MemoryDataStore();
        try {
            DexPool.writeTo(out, new ImmutableDexFile(OPS, Collections.singleton(c)));
            return Arrays.copyOf(out.getBuffer(), out.getSize());
        } finally { out.close(); }
    }
    private static void equal(Map<String, ClassDef> expected, Map<String, ClassDef> actual) throws Exception {
        require(expected.keySet().equals(actual.keySet()), "Definition inventory changed");
        for (String name : expected.keySet())
            require(Arrays.equals(canonical(expected.get(name)), canonical(actual.get(name))), "Definition changed " + name);
    }
    public static void main(String[] a) throws Exception {
        require(a.length == 7, "baseline category time back integrated standalone report");
        Map<String, ClassDef> base = read(a[0]), fresh = new TreeMap<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, ClassDef> component = read(a[i]);
            require(!component.isEmpty(), "Empty component");
            for (ClassDef c : component.values()) {
                require(c.getType().startsWith("Lapp/hiro/newx/"), "Unexpected new provider " + c.getType());
                require(!base.containsKey(c.getType()), "Existing provider collision " + c.getType());
                require(fresh.put(c.getType(), c) == null, "Component collision " + c.getType());
            }
        }
        Map<String, ClassDef> merged = new TreeMap<>(base);
        merged.putAll(fresh);
        DexPool.writeTo(a[4], new ImmutableDexFile(OPS, merged.values()));
        DexPool.writeTo(a[5], new ImmutableDexFile(OPS, fresh.values()));
        equal(merged, read(a[4]));
        equal(fresh, read(a[5]));
        String report = "{\"result\":\"PASS\",\"baseline_provider_classes\":" + base.size()
            + ",\"added_provider_classes\":" + fresh.size()
            + ",\"existing_provider_classes_preserved\":true,\"serialization_verified\":true}\n";
        Files.writeString(Path.of(a[6]), report);
        System.out.print(report);
    }
}
