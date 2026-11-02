import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

/** Preserve every baseline DEX definition, including DEX-only providers. */
public final class MergeDex {
    private static byte[] normalized(ClassDef definition, Opcodes opcodes) throws Exception {
        Path temporary = Files.createTempFile("berry-dex-class-", ".dex");
        try {
            DexPool.writeTo(temporary.toString(), new ImmutableDexFile(opcodes, Collections.singleton(definition)));
            return Files.readAllBytes(temporary);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static void main(String[] args) throws Exception {
        Opcodes opcodes = Opcodes.forApi(26);
        DexBackedDexFile base = DexFileFactory.loadDexFile(Path.of(args[0]).toFile(), opcodes);
        DexBackedDexFile added = DexFileFactory.loadDexFile(Path.of(args[1]).toFile(), opcodes);
        Map<String, ClassDef> classes = new LinkedHashMap<>();
        for (ClassDef c : base.getClasses()) {
            if (classes.put(c.getType(), c) != null) throw new IllegalStateException("Duplicate baseline class");
        }
        int oldCount = classes.size();
        for (ClassDef c : added.getClasses()) {
            if (!c.getType().startsWith("Lapp/hiro/berrybfcache/")) throw new IllegalStateException("Unexpected added namespace: " + c.getType());
            if (classes.putIfAbsent(c.getType(), c) != null) throw new IllegalStateException("Duplicate added class: " + c.getType());
        }
        DexPool.writeTo(args[2], new ImmutableDexFile(opcodes, classes.values()));
        DexBackedDexFile written = DexFileFactory.loadDexFile(Path.of(args[2]).toFile(), opcodes);
        Map<String, ClassDef> check = new LinkedHashMap<>();
        for (ClassDef c : written.getClasses()) check.put(c.getType(), c);
        if (!check.keySet().equals(classes.keySet())) throw new IllegalStateException("Merged class membership changed");
        for (Map.Entry<String, ClassDef> entry : classes.entrySet()) {
            if (!Arrays.equals(normalized(entry.getValue(), opcodes), normalized(check.get(entry.getKey()), opcodes)))
                throw new IllegalStateException("Merged definition changed: " + entry.getKey());
        }
        String report = "{\"result\":\"PASS_DEX_DEFINITION_PRESERVATION\",\"preserved_classes\":" + oldCount
            + ",\"added_classes\":" + (classes.size() - oldCount)
            + ",\"all_class_definitions_match_after_reference_index_normalization\":true}\n";
        Files.writeString(Path.of(args[3]), report, StandardCharsets.UTF_8);
        System.out.print(report);
    }
}
