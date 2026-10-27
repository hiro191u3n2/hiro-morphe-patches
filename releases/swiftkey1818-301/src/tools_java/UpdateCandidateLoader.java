import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import com.android.tools.r8.org.objectweb.asm.ClassReader;
import com.android.tools.r8.org.objectweb.asm.tree.ClassNode;
import com.android.tools.r8.org.objectweb.asm.tree.MethodNode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Replace only the installed candidate patch helper in the verified 1.8.17 loaders. */
public final class UpdateCandidateLoader {
    private static final String PREFIX = "app/hiro/swiftkey/patches/";
    private static final String MAIN = PREFIX + "JapaneseSamsungEmojiPatch";
    private static final String HELPER = PREFIX + "CandidateScrollPatch";
    private static final String PUNCTUATION = PREFIX + "PunctuationPatch";
    private static final String VOID_DESC = "(Lapp/morphe/patcher/patch/BytecodePatchContext;)V";
    private static final Set<String> BASELINES = new HashSet<String>(Arrays.asList(
            "d67006d5b338765a256518e6e2a2363d74de30afa6e593bdd0eb9a740f4b5f14",
            "f3fd6bdec26620497a13fb840419c02713357f80f624ac902f3063acb70ac69a"));
    private static final String MAIN_SHA = "869a3b153738f8694dad9e0fba6b134a9cf094ad3fef888896bf71bae947c5fe";
    private static final String HELPER_SHA = "f7a91d2438dff681b8489d02e0bb1f3dd0545ad725ca37ff2aabd9f88ef4a7a0";
    private static final String PUNCTUATION_SHA = "45cb65cc8e18090d0f8ec8373ec566c3183d383c9548aea52021184a762bd38f";

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
    private static String sha(byte[] raw) throws Exception {
        StringBuilder out = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(raw))
            out.append(String.format("%02x", value & 255));
        return out.toString();
    }
    private static byte[] entry(Path zip, String name) throws Exception {
        try (ZipFile input = new ZipFile(zip.toFile())) {
            ZipEntry found = input.getEntry(name);
            require(found != null, "Missing baseline entry " + name);
            try (InputStream stream = input.getInputStream(found)) { return stream.readAllBytes(); }
        }
    }
    private static void baseline(Path input) throws Exception {
        require(BASELINES.contains(sha(Files.readAllBytes(input))), "Unverified 1.8.17 / 1.0.300 baseline");
        require(MAIN_SHA.equals(sha(entry(input, MAIN + ".class"))), "Baseline main patch changed");
        require(HELPER_SHA.equals(sha(entry(input, HELPER + ".class"))), "Installed candidate helper changed");
        require(PUNCTUATION_SHA.equals(sha(entry(input, PUNCTUATION + ".class"))), "Punctuation patch missing or changed");
        int count = 0;
        try (ZipFile zip = new ZipFile(input.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(PREFIX) && name.endsWith(".class")) count++;
            }
        }
        require(count == 11, "Baseline SwiftKey patch class inventory changed");
    }
    private static ClassNode node(byte[] raw) {
        ClassNode result = new ClassNode(); new ClassReader(raw).accept(result, 0); return result;
    }
    private static void jvm(Path input, Path helpers, Path output) throws Exception {
        baseline(input);
        byte[] replacement = Files.readAllBytes(helpers.resolve(HELPER + ".class"));
        ClassNode parsed = node(replacement);
        require(HELPER.equals(parsed.name), "Wrong replacement helper class");
        Set<String> entryPoints = new HashSet<String>();
        for (MethodNode method : parsed.methods) {
            if (method.name.equals("apply") || method.name.equals("checkOriginal")) {
                require(method.desc.equals(VOID_DESC) && (method.access & 9) == 9,
                        "Candidate bridge entry point changed " + method.name);
                require(entryPoints.add(method.name), "Duplicate candidate bridge entry point");
            }
        }
        require(entryPoints.size() == 2, "Missing installed bridge entry point");
        require(!HELPER_SHA.equals(sha(replacement)), "Candidate helper replacement is unchanged");
        Path target = output.resolve(HELPER + ".class");
        Files.createDirectories(target.getParent()); Files.write(target, replacement);
        System.out.println("PASS JVM helper replacement: changed=CandidateScrollPatch; existing main and 9 other patch classes untouched");
    }
    private static TreeMap<String, ClassDef> dex(byte[] raw) {
        TreeMap<String, ClassDef> result = new TreeMap<String, ClassDef>();
        for (ClassDef type : new DexBackedDexFile(Opcodes.forApi(26), java.nio.ByteBuffer.wrap(raw)).getClasses())
            require(result.put(type.getType(), type) == null, "Duplicate Android loader class");
        return result;
    }
    private static void android(Path input, Path helperDex, Path output) throws Exception {
        baseline(input);
        TreeMap<String, ClassDef> original = dex(entry(input, "classes.dex"));
        TreeMap<String, ClassDef> replacement = dex(Files.readAllBytes(helperDex));
        String type = "L" + HELPER + ";";
        require(replacement.size() == 1 && replacement.containsKey(type), "Unexpected replacement DEX inventory");
        require(original.containsKey(type) && original.containsKey("L" + MAIN + ";")
                && original.containsKey("L" + PUNCTUATION + ";"), "Installed Android patch inventory incomplete");
        Set<String> inventory = new TreeSet<String>(original.keySet());
        original.put(type, replacement.get(type));
        DexPool pool = new DexPool(Opcodes.forApi(26));
        for (ClassDef item : original.values()) pool.internClass(item);
        Files.createDirectories(output.toAbsolutePath().getParent());
        FileDataStore file = new FileDataStore(output.toFile());
        try { pool.writeTo(file); } finally { file.close(); }
        require(dex(Files.readAllBytes(output)).keySet().equals(inventory), "Android loader inventory changed");
        System.out.println("PASS Android helper replacement: changed=1 added=0 preserved=" + (inventory.size() - 1)
                + " total=" + inventory.size());
    }
    public static void main(String[] args) throws Exception {
        require(args.length == 4,
                "Usage: UpdateCandidateLoader jvm|dex baseline.mpp helper-class-dir|helper.dex output-dir|output.dex");
        if (args[0].equals("jvm")) jvm(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
        else if (args[0].equals("dex")) android(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
        else throw new IllegalArgumentException("Unknown mode");
    }
}
