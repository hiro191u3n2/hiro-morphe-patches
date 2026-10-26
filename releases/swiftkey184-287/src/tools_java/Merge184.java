import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;

/** Replaces a closed set of runtime families; never recompiles unrelated code. */
public final class Merge184 {
    static final String[] FAMILIES = {
        "Lhiro/swiftkey/JapaneseGlide", "Lhiro/swiftkey/JapaneseGlideDecoder",
        "Lhiro/swiftkey/JapaneseGlideCandidates"
    };
    static boolean owned(String type) {
        if (type.equals("Lhiro/swiftkey/KeySwap;")) return true;
        for (String family : FAMILIES)
            if (type.equals(family + ";") || type.startsWith(family + "$")) return true;
        return false;
    }
    static TreeMap<String, ClassDef> load(String path) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(path), Opcodes.forApi(26));
        TreeMap<String, ClassDef> map = new TreeMap<>();
        for (ClassDef c : dex.getClasses())
            if (map.put(c.getType(), c) != null) throw new IllegalStateException("Duplicate " + c.getType());
        return map;
    }
    public static void main(String[] args) throws Exception {
        TreeMap<String, ClassDef> original = load(args[0]), overlay = load(args[1]);
        TreeMap<String, ClassDef> preference = load(args[2]);
        if (!preference.keySet().equals(Collections.singleton("Lhiro/swiftkey/KeySwap;")))
            throw new IllegalStateException("Unexpected preference overlay " + preference.keySet());
        overlay.putAll(preference);
        int removed = 0;
        for (Iterator<String> it = original.keySet().iterator(); it.hasNext();)
            if (owned(it.next())) { it.remove(); removed++; }
        for (ClassDef c : overlay.values()) {
            if (!owned(c.getType())) throw new IllegalStateException("Unexpected replacement class " + c.getType());
            if (original.put(c.getType(), c) != null) throw new IllegalStateException("Unowned overlap " + c.getType());
        }
        for (String name : new String[]{"Lhiro/swiftkey/JapaneseGlide;", "Lhiro/swiftkey/JapaneseGlideDecoder;"})
            if (!overlay.containsKey(name)) throw new IllegalStateException("Missing " + name);
        DexPool pool = new DexPool(Opcodes.forApi(26));
        for (ClassDef c : original.values()) pool.internClass(c);
        FileDataStore out = new FileDataStore(new File(args[3]));
        try { pool.writeTo(out); } finally { out.close(); }
        if (!load(args[3]).keySet().equals(original.keySet())) throw new IllegalStateException("Output class set differs");
        System.out.println("PASS runtime merge: removed=" + removed + " replacement=" + overlay.size() + " total=" + original.size());
    }
}
