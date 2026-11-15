import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Replace only the reviewed structural providers; preserve all other definitions. */
public final class MergeResolver {
    private static final Opcodes OPS = Opcodes.forApi(26);
    private static final String TYPE = "Lapp/hiro/berry/patches/GenericBerryResolver";
    private static final String FOREGROUND = "Lapp/hiro/berry/patches/ForegroundTabsPatch";
    private static final String LIFECYCLE = "Lapp/hiro/berry/patches/GenericLifecycleResolver";
    private static final String[] EXISTING = {TYPE, FOREGROUND,
        "Lapp/hiro/berrybfcache/patches/BfCacheStructure",
        "Lapp/hiro/berrybfcache/patches/BfCacheRepairPatch",
        "Lapp/hiro/berrybfcache/patches/NativeCompletionStructure"};
    private static final String[] ADDED = {LIFECYCLE, "Lapp/hiro/berrybfcache/patches/BfCacheInlineStructure"};
    private static List<String> families() {
        List<String> result = new ArrayList<>(Arrays.asList(EXISTING));
        result.addAll(Arrays.asList(ADDED)); return result;
    }
    private static boolean resolver(String t) {
        for (String family : families())
            if (t.equals(family + ";") || t.startsWith(family + "$")) return true;
        return false;
    }
    private static boolean berry(String t) {
        for (String p : new String[]{"Lapp/hiro/berry/", "Lapp/hiro/berryzoom/", "Lapp/hiro/berryshare/", "Lapp/hiro/berryqueue/", "Lapp/hiro/berrybfcache/"})
            if (t.startsWith(p)) return true;
        return false;
    }
    private static void require(boolean b, String why) { if (!b) throw new IllegalStateException(why); }
    private static Map<String, ClassDef> read(String path) throws Exception {
        Map<String, ClassDef> classes = new TreeMap<>();
        for (ClassDef c : DexFileFactory.loadDexFile(Path.of(path).toFile(), OPS).getClasses())
            require(classes.put(c.getType(), c) == null, "Duplicate class " + c.getType());
        return classes;
    }
    private static byte[] canonical(ClassDef c) throws Exception {
        MemoryDataStore out = new MemoryDataStore();
        try { DexPool.writeTo(out, new ImmutableDexFile(OPS, Collections.singleton(c))); return Arrays.copyOf(out.getBuffer(), out.getSize()); }
        finally { out.close(); }
    }
    private static void same(ClassDef a, ClassDef b, String why) throws Exception {
        require(a != null && b != null && Arrays.equals(canonical(a), canonical(b)), why);
    }
    public static void main(String[] args) throws Exception {
        require(args.length == 5, "Expected baseline, providers, merged, standalone, report");
        Map<String, ClassDef> base = read(args[0]), fresh = read(args[1]), merged = new TreeMap<>();
        for (String family : EXISTING)
            require(base.containsKey(family + ";") && fresh.containsKey(family + ";"), "Provider missing: " + family);
        for (String family : ADDED)
            require(!base.containsKey(family + ";") && fresh.containsKey(family + ";"), "New helper inventory differs: " + family);
        for (String t : fresh.keySet()) require(resolver(t), "Unexpected new type " + t);
        int removed = 0;
        for (Map.Entry<String, ClassDef> e : base.entrySet()) {
            if (resolver(e.getKey())) removed++; else merged.put(e.getKey(), e.getValue());
        }
        require(Collections.disjoint(merged.keySet(), fresh.keySet()), "New type collides with retained type");
        merged.putAll(fresh);
        DexPool.writeTo(args[2], new ImmutableDexFile(OPS, merged.values()));
        Map<String, ClassDef> written = read(args[2]);
        require(written.keySet().equals(merged.keySet()), "Merged inventory changed");
        int preserved = 0;
        for (String t : written.keySet()) {
            same(merged.get(t), written.get(t), "Serialization changed " + t);
            if (!resolver(t)) { same(base.get(t), written.get(t), "Unrelated class changed " + t); preserved++; }
        }
        Map<String, ClassDef> subset = new TreeMap<>();
        for (String t : written.keySet()) if (berry(t)) subset.put(t, written.get(t));
        DexPool.writeTo(args[3], new ImmutableDexFile(OPS, subset.values()));
        Map<String, ClassDef> single = read(args[3]);
        require(single.keySet().equals(subset.keySet()), "Standalone inventory changed");
        for (String t : single.keySet()) same(written.get(t), single.get(t), "Standalone definition changed " + t);
        String report = "{\"result\":\"PASS\",\"baseline_classes\":" + base.size()
            + ",\"old_changed_provider_classes\":" + removed + ",\"new_changed_provider_classes\":" + fresh.size()
            + ",\"preserved_classes\":" + preserved + ",\"standalone_classes\":" + single.size()
            + ",\"all_unrelated_definitions_identical\":true,\"all_serialized_definitions_identical\":true}\n";
        Files.writeString(Path.of(args[4]), report);
        System.out.print(report);
    }
}
