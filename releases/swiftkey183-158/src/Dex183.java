import java.io.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.smali.Smali;
import com.android.tools.smali.smali.SmaliOptions;
import app.hiro.swiftkey.patches.MethodContract;

/** Deterministic assembly, merge and original native method contracts. */
public final class Dex183 {
    static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    static DexFile load(String path) throws Exception { return DexFileFactory.loadDexFile(new File(path), Opcodes.forApi(26)); }
    static TreeMap<String, ClassDef> classes(DexFile dex) {
        TreeMap<String, ClassDef> result = new TreeMap<>();
        for (ClassDef c : dex.getClasses()) require(result.put(c.getType(), c) == null, "Duplicate " + c.getType());
        return result;
    }
    static void write(Map<String, ClassDef> classes, String path) throws Exception {
        DexPool pool = new DexPool(Opcodes.forApi(26));
        for (ClassDef c : classes.values()) pool.internClass(c);
        FileDataStore out = new FileDataStore(new File(path));
        try { pool.writeTo(out); } finally { out.close(); }
    }
    static String signature(Method m) {
        StringBuilder b = new StringBuilder(m.getName()).append('(');
        for (CharSequence p : m.getParameterTypes()) b.append(p);
        return b.append(')').append(m.getReturnType()).toString();
    }
    static ClassDef nativeClass(String path) throws Exception {
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(path), Opcodes.forApi(26));
        for (String entry : dex.getDexEntryNames()) {
            for (ClassDef c : dex.getEntry(entry).getDexFile().getClasses()) if (c.getType().equals("Lec0/x;")) return c;
        }
        throw new IllegalStateException("Native ec0.x not found");
    }
    public static void main(String[] args) throws Exception {
        if (args[0].equals("assemble")) {
            SmaliOptions options = new SmaliOptions();
            options.apiLevel = 26; options.jobs = 1; options.verboseErrors = true; options.outputDexFile = args[2];
            require(Smali.assemble(options, args[1]), "Smali assembly failed");
        } else if (args[0].equals("merge")) {
            TreeMap<String, ClassDef> original = classes(load(args[1]));
            TreeMap<String, ClassDef> overlay = classes(load(args[2]));
            Set<String> expectedReplaced = new TreeSet<>();
            if (!args[4].equals("-")) expectedReplaced.addAll(Arrays.asList(args[4].split(",")));
            Set<String> expectedAdded = new TreeSet<>();
            if (!args[5].equals("-")) expectedAdded.addAll(Arrays.asList(args[5].split(",")));
            Set<String> replaced = new TreeSet<>(), added = new TreeSet<>();
            for (ClassDef c : overlay.values()) {
                (original.containsKey(c.getType()) ? replaced : added).add(c.getType());
                original.put(c.getType(), c);
            }
            require(replaced.equals(expectedReplaced) && added.equals(expectedAdded),
                    "Unexpected overlay: replaced=" + replaced + " added=" + added);
            write(original, args[3]);
            require(classes(load(args[3])).keySet().equals(original.keySet()), "Output class set differs");
            System.out.println("PASS DEX overlay: replaced=" + replaced + " added=" + added + " total=" + original.size());
        } else if (args[0].equals("contracts")) {
            ClassDef nativeClass = nativeClass(args[1]);
            for (Method m : nativeClass.getMethods()) {
                if (!Arrays.asList("i", "k", "p", "w").contains(m.getName())) continue;
                System.out.println(signature(m) + " " + m.getImplementation().getRegisterCount() + " " + MethodContract.sha256(m));
            }
        } else throw new IllegalArgumentException(args[0]);
    }
}
