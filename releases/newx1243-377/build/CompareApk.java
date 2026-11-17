import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Canonical per-class diff; APK ZIP/DEX ordering cannot disguise unrelated edits. */
public final class CompareApk {
    private static final Opcodes OPS = Opcodes.forApi(26);
    private static Map<String, String> inventory(String path) throws Exception {
        Map<String, String> result = new TreeMap<>();
        MultiDexContainer<? extends DexFile> container = DexFileFactory.loadDexContainer(new File(path), OPS);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (String entry : container.getDexEntryNames()) {
            for (ClassDef c : container.getEntry(entry).getDexFile().getClasses()) {
                MemoryDataStore data = new MemoryDataStore();
                try {
                    DexPool.writeTo(data, new ImmutableDexFile(OPS, Collections.singleton(c)));
                    digest.update(data.getBuffer(), 0, data.getSize());
                    String hash = HexFormat.of().formatHex(digest.digest());
                    if (result.put(c.getType(), hash) != null) throw new IllegalStateException("Duplicate class " + c.getType());
                } finally { data.close(); }
            }
        }
        return result;
    }
    private static String json(Collection<String> names) {
        List<String> quoted = new ArrayList<>();
        for (String n : names) quoted.add("\"" + n.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
        return "[" + String.join(",", quoted) + "]";
    }
    public static void main(String[] a) throws Exception {
        if (a.length != 3) throw new IllegalArgumentException("before.apk after.apk report.json");
        Map<String, String> before = inventory(a[0]), after = inventory(a[1]);
        Set<String> removed = new TreeSet<>(before.keySet()); removed.removeAll(after.keySet());
        Set<String> added = new TreeSet<>(after.keySet()); added.removeAll(before.keySet());
        Set<String> changed = new TreeSet<>();
        int same = 0;
        for (String n : before.keySet()) {
            if (!after.containsKey(n)) continue;
            if (before.get(n).equals(after.get(n))) same++; else changed.add(n);
        }
        String report = "{\"before_classes\":" + before.size() + ",\"after_classes\":" + after.size()
            + ",\"identical_classes\":" + same + ",\"changed_classes\":" + json(changed)
            + ",\"added_classes\":" + json(added) + ",\"removed_classes\":" + json(removed) + "}\n";
        Files.writeString(Path.of(a[2]), report);
        System.out.println("identical=" + same + " changed=" + changed.size() + " added=" + added.size() + " removed=" + removed.size());
    }
}
