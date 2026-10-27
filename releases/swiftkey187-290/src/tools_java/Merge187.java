import java.io.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;

/** Add one flick helper family and replace only two exactly reviewed hook classes. */
public final class Merge187 {
    static final Set<String> HOOKS = new TreeSet<>(Arrays.asList(
        "Lhiro/swiftkey/KeySwap;", "Lhiro/swiftkey/JapaneseGlide;"));
    static boolean helper(String type) {
        return type.equals("Lhiro/swiftkey/FlickSymbols;") || type.startsWith("Lhiro/swiftkey/FlickSymbols$");
    }
    static TreeMap<String, ClassDef> load(String path) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(path), Opcodes.forApi(26));
        TreeMap<String, ClassDef> result = new TreeMap<>();
        for (ClassDef c : dex.getClasses())
            if (result.put(c.getType(), c) != null) throw new IllegalStateException("Duplicate " + c.getType());
        return result;
    }
    public static void main(String[] args) throws Exception {
        TreeMap<String, ClassDef> original = load(args[0]), helpers = load(args[1]), hooks = load(args[2]);
        if (original.size() != 56 || !original.keySet().containsAll(HOOKS))
            throw new IllegalStateException("Unexpected baseline runtime inventory");
        if (!hooks.keySet().equals(HOOKS)) throw new IllegalStateException("Unexpected hook overlay " + hooks.keySet());
        if (!helpers.containsKey("Lhiro/swiftkey/FlickSymbols;")) throw new IllegalStateException("Missing flick helper");
        for (ClassDef c : helpers.values()) {
            if (!helper(c.getType()) || original.containsKey(c.getType()))
                throw new IllegalStateException("Unexpected new runtime class " + c.getType());
            original.put(c.getType(), c);
        }
        original.putAll(hooks);
        DexPool pool = new DexPool(Opcodes.forApi(26));
        for (ClassDef c : original.values()) pool.internClass(c);
        FileDataStore out = new FileDataStore(new File(args[3]));
        try { pool.writeTo(out); } finally { out.close(); }
        if (!load(args[3]).keySet().equals(original.keySet())) throw new IllegalStateException("Output class set differs");
        System.out.println("PASS runtime merge: preserved=54 exact-hooks=2 added=" + helpers.size() + " total=" + original.size());
    }
}
