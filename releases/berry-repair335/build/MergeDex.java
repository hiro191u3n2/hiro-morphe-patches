import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Replace only selected Berry providers; retain every other DEX definition. */
public final class MergeDex {
    private static final Opcodes OPCODES = Opcodes.forApi(26);
    private static final List<String> EDIT_PREFIXES = Arrays.asList(
        "Lapp/hiro/berry/", "Lapp/hiro/berryshare/", "Lapp/hiro/berryzoom/", "Lapp/hiro/berrybfcache/");
    private static final Set<String> PROVIDERS = new TreeSet<>(Arrays.asList(
        "Lapp/hiro/berry/patches/ForegroundTabsPatch;",
        "Lapp/hiro/berryshare/patches/BrowserVisibilityPatch;",
        "Lapp/hiro/berryzoom/patches/OneHandZoomPatch;",
        "Lapp/hiro/berrybfcache/patches/BfCacheRepairPatch;"));
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
    private static boolean contract(String type) {
        return type.endsWith("/MethodContract;") || type.endsWith("/OriginalContracts;");
    }
    private static boolean editedNamespace(String type) {
        for (String prefix : EDIT_PREFIXES) if (type.startsWith(prefix)) return true;
        return false;
    }
    private static boolean replaceable(String type) { return editedNamespace(type) && !contract(type); }
    private static boolean berry(String type) { return editedNamespace(type) || type.startsWith("Lapp/hiro/berryqueue/"); }
    private static Map<String, ClassDef> read(String file) throws Exception {
        Map<String, ClassDef> result = new TreeMap<>();
        for (ClassDef c : DexFileFactory.loadDexFile(Path.of(file).toFile(), OPCODES).getClasses())
            require(result.put(c.getType(), c) == null, "Duplicate class: " + c.getType());
        return result;
    }
    private static byte[] canonical(ClassDef definition) throws Exception {
        MemoryDataStore out = new MemoryDataStore();
        try {
            DexPool.writeTo(out, new ImmutableDexFile(OPCODES, Collections.singleton(definition)));
            return Arrays.copyOf(out.getBuffer(), out.getSize());
        } finally { out.close(); }
    }
    private static void same(ClassDef a, ClassDef b, String message) throws Exception {
        require(a != null && b != null && Arrays.equals(canonical(a), canonical(b)), message);
    }
    public static void main(String[] args) throws Exception {
        require(args.length == 5, "Expected baseline, new DEX, combined DEX, standalone DEX and report");
        Map<String, ClassDef> base = read(args[0]), fresh = read(args[1]);
        require(base.keySet().containsAll(PROVIDERS) && fresh.keySet().containsAll(PROVIDERS), "Missing required provider");
        for (String type : fresh.keySet()) require(replaceable(type), "New definition exceeds allowed namespaces: " + type);
        Map<String, ClassDef> combined = new TreeMap<>();
        int replaced = 0, preserved = 0, retainedContracts = 0, retainedQueue = 0;
        for (Map.Entry<String, ClassDef> row : base.entrySet()) {
            if (replaceable(row.getKey())) { replaced++; continue; }
            combined.put(row.getKey(), row.getValue());
            if (editedNamespace(row.getKey()) && contract(row.getKey())) retainedContracts++;
            if (row.getKey().startsWith("Lapp/hiro/berryqueue/")) retainedQueue++;
        }
        require(retainedContracts == 5 && retainedQueue == 5, "Baseline retained Berry inventory differs");
        require(Collections.disjoint(combined.keySet(), fresh.keySet()), "New classes collide with retained definitions");
        combined.putAll(fresh);
        DexPool.writeTo(args[2], new ImmutableDexFile(OPCODES, combined.values()));
        Map<String, ClassDef> written = read(args[2]);
        require(written.keySet().equals(combined.keySet()), "Combined class membership differs");
        for (String type : written.keySet()) {
            same(combined.get(type), written.get(type), "Serialization changed definition: " + type);
            if (base.containsKey(type) && !replaceable(type)) {
                same(base.get(type), written.get(type), "Unrelated definition changed: " + type);
                preserved++;
            }
        }
        Map<String, ClassDef> subset = new TreeMap<>();
        for (String type : written.keySet()) if (berry(type)) subset.put(type, written.get(type));
        require(subset.size() == fresh.size() + retainedContracts + retainedQueue, "Standalone membership differs");
        DexPool.writeTo(args[3], new ImmutableDexFile(OPCODES, subset.values()));
        Map<String, ClassDef> single = read(args[3]);
        require(single.keySet().equals(subset.keySet()), "Standalone class membership changed");
        for (String type : single.keySet()) same(single.get(type), written.get(type), "Standalone definition differs: " + type);
        String report = "{\"result\":\"PASS_DEX_DEFINITION_PRESERVATION\",\"baseline_classes\":" + base.size()
            + ",\"replaced_classes\":" + replaced + ",\"new_classes\":" + fresh.size()
            + ",\"preserved_classes\":" + preserved + ",\"retained_contract_classes\":" + retainedContracts
            + ",\"retained_queue_classes\":" + retainedQueue + ",\"combined_classes\":" + written.size()
            + ",\"standalone_classes\":" + single.size()
            + ",\"all_retained_definitions_identical\":true,\"all_output_definitions_equal_inputs\":true}\n";
        Files.writeString(Path.of(args[4]), report);
        System.out.print(report);
    }
}
