import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Add the new Discord provider without replacing any previous loader definition. */
public final class LoaderAudit {
    private static final Opcodes OPS = Opcodes.forApi(26);
    private static final String PREFIX = "Lapp/hiro/discord/patches/";
    private static void require(boolean ok, String why) {
        if (!ok) throw new IllegalStateException(why);
    }
    private static Map<String, ClassDef> read(String path) throws Exception {
        Map<String, ClassDef> result = new TreeMap<>();
        for (ClassDef c : DexFileFactory.loadDexFile(Path.of(path).toFile(), OPS).getClasses())
            require(result.put(c.getType(), c) == null, "Duplicate type: " + c.getType());
        return result;
    }
    private static byte[] canonical(ClassDef c) throws Exception {
        MemoryDataStore out = new MemoryDataStore();
        try {
            DexPool.writeTo(out, new ImmutableDexFile(OPS, Collections.singleton(c)));
            return Arrays.copyOf(out.getBuffer(), out.getSize());
        } finally { out.close(); }
    }
    private static void same(ClassDef a, ClassDef b, String why) throws Exception {
        require(a != null && b != null && Arrays.equals(canonical(a), canonical(b)), why);
    }
    public static void main(String[] args) throws Exception {
        require(args.length == 4, "Expected baseline.dex, provider.dex, merged.dex, report.json");
        Map<String, ClassDef> base = read(args[0]), fresh = read(args[1]);
        require(!fresh.isEmpty(), "No new provider classes");
        require(Collections.disjoint(base.keySet(), fresh.keySet()), "Existing provider collision");
        for (String t : fresh.keySet()) require(t.startsWith(PREFIX), "Unowned new class: " + t);
        for (String t : base.keySet()) require(!t.startsWith(PREFIX), "Discord provider already exists");
        Map<String, ClassDef> merged = new TreeMap<>(base);
        merged.putAll(fresh);
        DexPool.writeTo(args[2], new ImmutableDexFile(OPS, merged.values()));
        Map<String, ClassDef> written = read(args[2]);
        require(written.keySet().equals(merged.keySet()), "Output inventory differs");
        for (String t : base.keySet()) same(base.get(t), written.get(t), "Existing definition changed: " + t);
        for (String t : fresh.keySet()) same(fresh.get(t), written.get(t), "New definition changed: " + t);
        String json = "{\"result\":\"PASS\",\"baseline_classes\":" + base.size()
            + ",\"new_provider_classes\":" + fresh.size() + ",\"merged_classes\":" + written.size()
            + ",\"all_existing_definitions_identical\":true,\"all_serialized_definitions_identical\":true}\n";
        Files.writeString(Path.of(args[3]), json);
        System.out.print(json);
    }
}
