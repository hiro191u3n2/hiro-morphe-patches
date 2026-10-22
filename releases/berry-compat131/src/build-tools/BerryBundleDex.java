import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Replace the three Berry providers while proving every other ClassDef unchanged. */
public final class BerryBundleDex {
    private static int assertions;
    private static final Set<String> PROVIDERS = new TreeSet<>(Arrays.asList(
        "Lapp/hiro/berry/patches/ForegroundTabsPatch;",
        "Lapp/hiro/berryshare/patches/BrowserVisibilityPatch;",
        "Lapp/hiro/berryzoom/patches/OneHandZoomPatch;"));
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static boolean originalThree(String type) {
        return type.startsWith("Lapp/hiro/berry/") || type.startsWith("Lapp/hiro/berryshare/")
            || type.startsWith("Lapp/hiro/berryzoom/");
    }
    private static boolean contract(String type) {
        return type.endsWith("/MethodContract;") || type.endsWith("/OriginalContracts;");
    }
    private static boolean replaceable(String type) { return originalThree(type) && !contract(type); }
    private static boolean berry(String type) {
        return originalThree(type) || type.startsWith("Lapp/hiro/berryqueue/");
    }
    private static Map<String, ClassDef> read(String file) throws Exception {
        Map<String, ClassDef> result = new TreeMap<>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(file), Opcodes.getDefault());
        for (String entry : dex.getDexEntryNames()) for (ClassDef c : dex.getEntry(entry).getDexFile().getClasses())
            check(result.put(c.getType(), c) == null, "Duplicate class " + c.getType());
        return result;
    }
    private static byte[] canonical(ClassDef clazz) throws Exception {
        MemoryDataStore out = new MemoryDataStore();
        try {
            DexPool.writeTo(out, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(clazz)));
            return Arrays.copyOf(out.getBuffer(), out.getSize());
        } finally { out.close(); }
    }
    public static void main(String[] args) throws Exception {
        check(args.length == 4, "Expected baseline DEX, replacement DEX, combined DEX, Berry DEX");
        Map<String, ClassDef> base = read(args[0]), replacement = read(args[1]);
        check(base.keySet().containsAll(PROVIDERS) && replacement.keySet().containsAll(PROVIDERS), "Missing provider");
        for (String type : replacement.keySet()) check(replaceable(type), "Replacement exceeds reviewed namespaces: " + type);
        Map<String, ClassDef> combined = new TreeMap<>();
        int removed = 0, retainedContracts = 0, retainedQueue = 0;
        for (Map.Entry<String, ClassDef> row : base.entrySet()) {
            String type = row.getKey();
            if (replaceable(type)) { removed++; continue; }
            combined.put(type, row.getValue());
            if (originalThree(type) && contract(type)) retainedContracts++;
            if (type.startsWith("Lapp/hiro/berryqueue/")) retainedQueue++;
        }
        check(removed == 22 && retainedContracts == 5 && retainedQueue == 5, "Baseline Berry inventory differs");
        check(Collections.disjoint(combined.keySet(), replacement.keySet()), "Replacement collides with retained classes");
        combined.putAll(replacement);
        DexPool.writeTo(args[2], new ImmutableDexFile(Opcodes.getDefault(), combined.values()));
        Map<String, ClassDef> written = read(args[2]);
        check(written.keySet().equals(combined.keySet()), "Combined inventory differs");
        int preserved = 0;
        for (String type : written.keySet()) {
            check(Arrays.equals(canonical(combined.get(type)), canonical(written.get(type))), "Serialization changed " + type);
            if (base.containsKey(type) && !replaceable(type)) {
                check(Arrays.equals(canonical(base.get(type)), canonical(written.get(type))), "Unrelated class changed " + type);
                preserved++;
            }
        }
        Map<String, ClassDef> subset = new TreeMap<>();
        for (String type : written.keySet()) if (berry(type)) subset.put(type, written.get(type));
        check(subset.size() == replacement.size() + retainedContracts + retainedQueue, "Berry subset inventory differs");
        DexPool.writeTo(args[3], new ImmutableDexFile(Opcodes.getDefault(), subset.values()));
        Map<String, ClassDef> single = read(args[3]);
        check(single.keySet().equals(subset.keySet()), "Standalone inventory differs");
        for (String type : single.keySet()) check(Arrays.equals(canonical(single.get(type)), canonical(written.get(type))), "Standalone class differs " + type);
        System.out.println("{\"status\":\"PASS\",\"baseline_classes\":" + base.size()
            + ",\"replaced_provider_classes\":" + removed + ",\"new_provider_classes\":" + replacement.size()
            + ",\"preserved_classes\":" + preserved + ",\"retained_contract_classes\":" + retainedContracts
            + ",\"retained_queue_classes\":" + retainedQueue + ",\"combined_classes\":" + written.size()
            + ",\"berry_subset_classes\":" + single.size() + ",\"assertions\":" + assertions + "}");
    }
}
