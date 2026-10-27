import java.io.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;

/** Replace only candidate-scroll classes in the verified 1.8.17 runtime. */
public final class Merge1818 {
    static boolean allowed(String type) {
        return type.equals("Lhiro/swiftkey/CandidateScroll;") || type.startsWith("Lhiro/swiftkey/CandidateScroll$");
    }
    static TreeMap<String, ClassDef> load(String path) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(path), Opcodes.forApi(26));
        TreeMap<String, ClassDef> result = new TreeMap<>();
        for (ClassDef c : dex.getClasses())
            if (result.put(c.getType(), c) != null) throw new IllegalStateException("Duplicate " + c.getType());
        return result;
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("baseline overlay output");
        TreeMap<String, ClassDef> original = load(args[0]), helpers = load(args[1]);
        if (original.size() != 74 || !original.containsKey("Lhiro/swiftkey/CandidateToolbar;")
                || !original.containsKey("Lhiro/swiftkey/EmojiToolbar;")
                || !original.containsKey("Lhiro/swiftkey/PunctuationKeys;"))
            throw new IllegalStateException("Unexpected 1.8.17 runtime inventory");
        Set<String> previousFamily = new TreeSet<>();
        for (String type : original.keySet()) if (allowed(type)) previousFamily.add(type);
        if (previousFamily.size() != 3 || !helpers.keySet().containsAll(previousFamily))
            throw new IllegalStateException("Existing candidate class removed");
        for (ClassDef c : helpers.values()) {
            if (!allowed(c.getType())) throw new IllegalStateException("Unexpected runtime class " + c.getType());
            original.put(c.getType(), c);
        }
        DexPool pool = new DexPool(Opcodes.forApi(26));
        for (ClassDef c : original.values()) pool.internClass(c);
        FileDataStore out = new FileDataStore(new File(args[2]));
        try { pool.writeTo(out); } finally { out.close(); }
        if (!load(args[2]).keySet().equals(original.keySet())) throw new IllegalStateException("Output class set differs");
        System.out.println("PASS runtime merge: all 74 baseline classes retained; candidate family=" + helpers.size());
    }
}
