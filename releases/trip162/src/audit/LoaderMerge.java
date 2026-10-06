import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Merge only explicitly compiled Trip patch families; round-trip all retained classes. */
public final class LoaderMerge {
    private static final Opcodes OPS = Opcodes.forApi(26);
    private static final String PREFIX = "Lapp/hiro/tripcom/patches/";

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static Map<String, ClassDef> load(Path path) throws Exception {
        Map<String, ClassDef> out = new TreeMap<>();
        MultiDexContainer<? extends DexFile> input = DexFileFactory.loadDexContainer(path.toFile(), OPS);
        for (String entry : input.getDexEntryNames()) {
            for (ClassDef item : input.getEntry(entry).getDexFile().getClasses()) {
                require(out.put(item.getType(), ImmutableClassDef.of(item)) == null, "Duplicate class " + item.getType());
            }
        }
        return out;
    }

    private static byte[] canonical(ClassDef definition) throws Exception {
        require(definition != null, "Missing class definition");
        MemoryDataStore data = new MemoryDataStore();
        try {
            DexPool.writeTo(data, new ImmutableDexFile(OPS, Collections.singleton(definition)));
            return Arrays.copyOf(data.getBuffer(), data.getSize());
        } finally { data.close(); }
    }

    private static void same(ClassDef a, ClassDef b, String message) throws Exception {
        require(a != null && b != null && Arrays.equals(canonical(a), canonical(b)), message);
    }

    private static boolean owned(String type, Set<String> families) {
        for (String name : families) if (type.equals(name + ";") || type.startsWith(name + "$")) return true;
        return false;
    }

    private static Map<String, ClassDef> merge(Map<String, ClassDef> baseline,
            Map<String, ClassDef> fresh, Set<String> families) {
        Map<String, ClassDef> merged = new TreeMap<>();
        for (ClassDef c : baseline.values()) if (!owned(c.getType(), families)) merged.put(c.getType(), c);
        for (ClassDef c : fresh.values()) {
            require(c.getType().startsWith(PREFIX) && owned(c.getType(), families), "New class outside explicit Trip scope " + c.getType());
            require(merged.put(c.getType(), c) == null, "Unexpected class collision " + c.getType());
        }
        for (String family : families) require(merged.containsKey(family + ";"), "Compiled root class missing " + family);
        return merged;
    }

    private static Map<String, ClassDef> writeAndCheck(Path path, Map<String, ClassDef> expected) throws Exception {
        DexPool.writeTo(path.toString(), new ImmutableDexFile(OPS, expected.values()));
        Map<String, ClassDef> actual = load(path);
        require(expected.keySet().equals(actual.keySet()), "Serialized class inventory changed");
        for (String name : expected.keySet()) same(expected.get(name), actual.get(name), "Serialized class content differs " + name);
        return actual;
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 7, "BASEDEX OLD_SINGLEDEX PATCHDEX FAMILIES BUNDLEDEX SINGLEDEX REPORT");
        Map<String, ClassDef> base = load(Path.of(args[0]));
        Map<String, ClassDef> single = load(Path.of(args[1]));
        Map<String, ClassDef> fresh = load(Path.of(args[2]));
        Set<String> families = new TreeSet<>();
        for (String line : Files.readAllLines(Path.of(args[3]))) {
            if (line.isBlank()) continue;
            require(line.startsWith(PREFIX) && !line.endsWith(";"), "Unexpected class family " + line);
            require(families.add(line), "Duplicate family");
        }
        require(!families.isEmpty() && !fresh.isEmpty(), "No new patch definitions");
        for (String name : single.keySet()) {
            require(name.startsWith("Lapp/hiro/tripcom/"), "Old single contains another app class");
            same(single.get(name), base.get(name), "Old Trip loader differs from active bundle " + name);
        }
        Map<String, ClassDef> next = writeAndCheck(Path.of(args[4]), merge(base, fresh, families));
        Map<String, ClassDef> nextSingle = writeAndCheck(Path.of(args[5]), merge(single, fresh, families));
        int preserved = 0, nonTrip = 0, replaced = 0, added = 0;
        for (String name : base.keySet()) {
            if (owned(name, families)) { replaced++; continue; }
            same(base.get(name), next.get(name), "Retained loader changed " + name);
            preserved++;
            if (!name.startsWith("Lapp/hiro/tripcom/")) nonTrip++;
        }
        for (String name : fresh.keySet()) if (!base.containsKey(name)) added++;
        for (String name : nextSingle.keySet()) same(nextSingle.get(name), next.get(name), "Single and bundle loader differ " + name);
        String report = "{\n  \"status\": \"PASS\",\n  \"baseline_loader_classes\": " + base.size()
            + ",\n  \"final_loader_classes\": " + next.size()
            + ",\n  \"preserved_loader_classes\": " + preserved
            + ",\n  \"preserved_non_trip_loader_classes\": " + nonTrip
            + ",\n  \"replaced_trip_loader_classes\": " + replaced
            + ",\n  \"added_trip_loader_classes\": " + added
            + ",\n  \"standalone_loader_classes\": " + nextSingle.size()
            + ",\n  \"non_trip_loader_classes_unchanged\": true,\n  \"standalone_and_bundle_trip_loaders_identical\": true\n}\n";
        Files.writeString(Path.of(args[6]), report);
        System.out.print(report);
    }
}
