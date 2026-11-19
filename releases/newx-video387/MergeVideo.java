import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.nio.file.*;
import java.util.*;

/** Merge the video provider and verify canonical definitions of every class. */
public final class MergeVideo {
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
    public static void main(String[] args) throws Exception {
        Map<String, ClassDef> combined = read(args[0]);
        Map<String, ClassDef> added = read(args[1]);
        require(added.containsKey("Lapp/hiro/newx/video/SharedVideoPatch;"), "Missing provider");
        for (ClassDef definition : added.values()) {
            require(definition.getType().startsWith("Lapp/hiro/newx/video/SharedVideoPatch"), "Foreign class " + definition.getType());
            require(combined.put(definition.getType(), definition) == null, "Duplicate class");
        }
        DexPool.writeTo(args[2], new ImmutableDexFile(OPS, combined.values()));
        equal(combined, read(args[2]));
        System.out.println("Verified " + combined.size() + " provider class definitions");
    }
}
