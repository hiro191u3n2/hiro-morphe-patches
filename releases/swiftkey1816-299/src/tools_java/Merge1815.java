import java.io.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;

/** Add only the candidate/emoji toolbar helpers to the verified 1.8.14 runtime. */
public final class Merge1815 {
    static boolean allowed(String type) {
        return type.equals("Lhiro/swiftkey/CandidateToolbar;") || type.startsWith("Lhiro/swiftkey/CandidateToolbar$")
            || type.equals("Lhiro/swiftkey/EmojiToolbar;") || type.startsWith("Lhiro/swiftkey/EmojiToolbar$");
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
        if (original.size() != 62 || !original.containsKey("Lhiro/swiftkey/CandidateScroll;"))
            throw new IllegalStateException("Unexpected 1.8.14 runtime inventory");
        if (!helpers.containsKey("Lhiro/swiftkey/CandidateToolbar;") || !helpers.containsKey("Lhiro/swiftkey/EmojiToolbar;"))
            throw new IllegalStateException("Required UI runtime helpers missing");
        for (ClassDef c : helpers.values()) {
            if (!allowed(c.getType()) || original.containsKey(c.getType()))
                throw new IllegalStateException("Unexpected or replacing runtime class " + c.getType());
            original.put(c.getType(), c);
        }
        DexPool pool = new DexPool(Opcodes.forApi(26));
        for (ClassDef c : original.values()) pool.internClass(c);
        FileDataStore out = new FileDataStore(new File(args[2]));
        try { pool.writeTo(out); } finally { out.close(); }
        if (!load(args[2]).keySet().equals(original.keySet())) throw new IllegalStateException("Output class set differs");
        System.out.println("PASS runtime merge: all62 existing classes preserved; added=" + helpers.size());
    }
}
