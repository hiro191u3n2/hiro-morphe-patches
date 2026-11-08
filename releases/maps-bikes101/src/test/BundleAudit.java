import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.MultiDexContainer;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Replace only the bicycle provider package in bundle369; retain every unrelated DEX class. */
public final class BundleAudit {
    private static final String PREFIX = "Lapp/hiro/mapsbikes/patches/";
    private static long checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static Map<String, ClassDef> read(String path) throws Exception {
        Map<String, ClassDef> result = new TreeMap<String, ClassDef>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String name : dex.getDexEntryNames()) {
            for (ClassDef type : dex.getEntry(name).getDexFile().getClasses()) {
                check(result.put(type.getType(), type) == null, "Duplicate class " + type.getType());
            }
        }
        return result;
    }

    private static byte[] canonical(ClassDef type) throws Exception {
        MemoryDataStore store = new MemoryDataStore();
        try {
            DexPool.writeTo(store, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(type)));
            return Arrays.copyOf(store.getBuffer(), store.getSize());
        } finally {
            store.close();
        }
    }

    public static void main(String[] args) throws Exception {
        check(args.length == 3, "Usage: BundleAudit baseline369.dex replacement.dex output.dex");
        Map<String, ClassDef> base = read(args[0]), addition = read(args[1]);
        Map<String, ClassDef> merged = new TreeMap<String, ClassDef>();
        check(!base.isEmpty(), "Empty baseline DEX");
        check(base.containsKey(PREFIX + "MapsHideBicycleMarksPatch;"), "Published baseline bicycle provider missing");
        check(addition.containsKey(PREFIX + "MapsHideBicycleMarksPatch;"), "Bicycle provider missing");
        int replaced = 0;
        for (ClassDef type : base.values()) {
            if (type.getType().startsWith(PREFIX)) replaced++;
            else merged.put(type.getType(), type);
        }
        check(replaced > 0, "No published bicycle provider classes were replaced");
        for (ClassDef type : addition.values()) {
            check(type.getType().startsWith(PREFIX), "New loader class outside bicycle provider namespace");
            check(merged.put(type.getType(), type) == null, "New loader class collides with unrelated baseline");
        }
        DexPool.writeTo(args[2], new ImmutableDexFile(Opcodes.getDefault(), merged.values()));
        Map<String, ClassDef> output = read(args[2]);
        check(output.keySet().equals(merged.keySet()), "Merged class inventory differs");
        for (ClassDef type : merged.values()) {
            check(Arrays.equals(canonical(type), canonical(output.get(type.getType()))),
                    "Merged class changed " + type.getType());
        }
        System.out.println("PASS bicycle-only loader replacement: unrelated baseline provider classes preserved="
                + (base.size() - replaced) + "; replaced old bicycle provider classes=" + replaced
                + "; new bicycle provider classes=" + addition.size() + "; assertions=" + checks);
    }
}
