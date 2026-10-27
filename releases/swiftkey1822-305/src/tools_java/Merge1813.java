import java.io.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;

/** Replace only the existing flick helper family, preserving all other classes. */
public final class Merge1813 {
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
        TreeMap<String, ClassDef> original = load(args[0]), helpers = load(args[1]);
        if (original.size() != 59 || !original.keySet().containsAll(HOOKS))
            throw new IllegalStateException("Unexpected baseline runtime inventory");
        Set<String> expected = new TreeSet<>();
        for (String type : original.keySet()) if (helper(type)) expected.add(type);
        if (expected.size() != 3 || !helpers.keySet().equals(expected))
            throw new IllegalStateException("Flick helper family inventory changed");
        for (ClassDef c : helpers.values()) {
            if (!helper(c.getType()) || !original.containsKey(c.getType()))
                throw new IllegalStateException("Unexpected replacement runtime class " + c.getType());
            original.put(c.getType(), c);
        }
        DexPool pool = new DexPool(Opcodes.forApi(26));
        for (ClassDef c : original.values()) pool.internClass(c);
        FileDataStore out = new FileDataStore(new File(args[2]));
        try { pool.writeTo(out); } finally { out.close(); }
        if (!load(args[2]).keySet().equals(original.keySet())) throw new IllegalStateException("Output class set differs");
        System.out.println("PASS runtime merge: preserved=56 existing-hooks=unchanged replaced=" + helpers.size() + " total=" + original.size());
    }
}
