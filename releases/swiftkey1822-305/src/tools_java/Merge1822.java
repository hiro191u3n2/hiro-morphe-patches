import java.io.File;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;

/** Add the emoji footer helper while retaining every existing runtime class. */
public final class Merge1822 {
    private static final String BASELINE_SHA = "e436cdc71432ed7066335d9516d5ac2d233f4a39f4014ab7ce3787ba7d3a9593";
    private static void require(boolean ok, String why) {
        if (!ok) throw new IllegalStateException(why);
    }
    private static String sha(String path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(path))));
    }
    private static TreeMap<String, ClassDef> load(String path) throws Exception {
        TreeMap<String, ClassDef> result = new TreeMap<>();
        for (ClassDef c : DexFileFactory.loadDexFile(new File(path), Opcodes.forApi(26)).getClasses())
            require(result.put(c.getType(), c) == null, "Duplicate type: " + c.getType());
        return result;
    }
    private static byte[] canonical(ClassDef c) throws Exception {
        require(c != null, "Missing class");
        DexPool pool = new DexPool(Opcodes.forApi(26));
        pool.internClass(c);
        MemoryDataStore store = new MemoryDataStore();
        try { pool.writeTo(store); return Arrays.copyOf(store.getBuffer(), store.getSize()); }
        finally { store.close(); }
    }
    private static String json(Object v) {
        if (v == null) return "null";
        if (v instanceof Boolean || v instanceof Number) return v.toString();
        if (v instanceof Map) {
            StringJoiner out = new StringJoiner(",", "{", "}");
            for (Map.Entry<?, ?> e : ((Map<?, ?>)v).entrySet()) out.add(json(e.getKey())+":"+json(e.getValue()));
            return out.toString();
        }
        if (v instanceof Iterable) {
            StringJoiner out = new StringJoiner(",", "[", "]");
            for (Object e : (Iterable<?>)v) out.add(json(e));
            return out.toString();
        }
        return "\"" + v.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
    public static void main(String[] args) throws Exception {
        require(args.length == 4, "Usage: Merge1822 baseline.mpe helper.dex output.mpe report.json");
        require(sha(args[0]).equals(BASELINE_SHA), "Published 1.8.21 runtime pin differs");
        TreeMap<String, ClassDef> before = load(args[0]), overlay = load(args[1]);
        require(before.size() == 81, "Unexpected baseline runtime inventory");
        require(overlay.containsKey("Lhiro/swiftkey/EmojiFooter;"), "EmojiFooter entrypoint absent");
        for (String type : overlay.keySet()) {
            require(type.equals("Lhiro/swiftkey/EmojiFooter;") || type.startsWith("Lhiro/swiftkey/EmojiFooter$"), "Unexpected helper type: " + type);
            require(!before.containsKey(type), "This release may only add helper classes: " + type);
        }
        TreeMap<String, ClassDef> merged = new TreeMap<>(before);
        merged.putAll(overlay);
        DexPool pool = new DexPool(Opcodes.forApi(26));
        for (ClassDef c : merged.values()) pool.internClass(c);
        FileDataStore store = new FileDataStore(new File(args[2]));
        try { pool.writeTo(store); } finally { store.close(); }
        TreeMap<String, ClassDef> result = load(args[2]);
        require(result.keySet().equals(merged.keySet()), "Serialized class inventory differs");
        for (ClassDef c : before.values())
            require(Arrays.equals(canonical(c), canonical(result.get(c.getType()))), "Existing runtime changed: " + c.getType());
        for (ClassDef c : overlay.values())
            require(Arrays.equals(canonical(c), canonical(result.get(c.getType()))), "Compiled helper changed during merge: " + c.getType());
        Map<String,Object> report = new LinkedHashMap<>();
        report.put("status", "PASSED");
        report.put("scope", "Add EmojiFooter only; canonical preservation of all existing runtime classes");
        report.put("baseline_extension_sha256", sha(args[0]));
        report.put("overlay_sha256", sha(args[1]));
        report.put("extension_sha256", sha(args[2]));
        report.put("baseline_classes", before.size());
        report.put("runtime_after_classes", result.size());
        report.put("unchanged_existing_classes", before.size());
        report.put("unchanged_existing_class_paths", before.keySet());
        report.put("added_classes", overlay.keySet());
        report.put("changed_existing_classes", Collections.emptyList());
        report.put("existing_entrypoints_preserved", true);
        report.put("unrelated_existing_classes_byte_identical", true);
        report.put("all_existing_runtime_classes_byte_identical", true);
        report.put("serialized_overlay_matches_compiled_helper", true);
        report.put("device_tested", false);
        report.put("original_apk_apply_tested", false);
        Files.writeString(Path.of(args[3]), json(report) + "\n");
        System.out.println("PASS: all " + before.size() + " existing classes preserved; " + overlay.size() + " EmojiFooter classes added");
    }
}
