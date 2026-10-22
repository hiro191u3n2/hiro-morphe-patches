import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MultiDexContainer;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.FieldReference;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/**
 * Audits actual patched APK definitions, not just build-source intent.
 * queue: every pre-existing ClassDef is canonically unchanged; only the new
 *        app.hiro.berry.runtime namespace may be added.
 * all:   old Berry changes are confined to seven explicitly audited classes;
 *        all other original definitions remain canonically unchanged.
 *
 * Resource bytes are compared and reported but not required to match, since
 * the official patcher may normalize the compiled resource table/XML.
 * The MessageQueue reflection scan is deliberately advisory and incomplete:
 * it inspects DEX instruction references/string constants, not native code,
 * downloaded code, encrypted strings, or arbitrary reflective data flow.
 * Args: queue|all input.apk output.apk report.json
 */
public final class AppDexAudit {
    private static final String QUEUE = "Landroid/os/MessageQueue;";
    private static final String RUNTIME = "Lapp/hiro/berry/runtime/";
    private static final Set<String> OLD_MUTATIONS = new TreeSet<>(Arrays.asList(
        "Lg60;", "Lc41;", "Lc70;", "Lcx;", "Lb03;", "Lj8;", "Lz31;"));
    private static final Set<String> CURRENT_MUTATIONS = new TreeSet<>(Arrays.asList(
        "Lz60;", "Lu51;", "Lt70;", "Lpx;", "Lz13;", "Lj8;", "Lr51;"));
    private static final Set<String> PUBLIC_QUEUE_METHODS = new TreeSet<>(Arrays.asList(
        "addIdleHandler", "removeIdleHandler", "isIdle", "addOnFileDescriptorEventListener",
        "removeOnFileDescriptorEventListener"));
    private static final Set<String> QUEUE_STRINGS = new TreeSet<>(Arrays.asList(
        "mMessages", "mQueue", "android.os.MessageQueue", "Landroid/os/MessageQueue;", "MessageQueue"));
    private static int assertions;

    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }

    private static Map<String, ClassDef> read(String path) throws Exception {
        Map<String, ClassDef> classes = new TreeMap<>();
        MultiDexContainer<? extends DexFile> dex = DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String entry : dex.getDexEntryNames()) for (ClassDef clazz : dex.getEntry(entry).getDexFile().getClasses())
            check(classes.put(clazz.getType(), clazz) == null, "Duplicate class: " + clazz.getType());
        check(!classes.isEmpty(), "Input/output APK has no DEX classes");
        return classes;
    }

    private static byte[] canonical(ClassDef clazz) throws Exception {
        MemoryDataStore output = new MemoryDataStore();
        try {
            DexPool.writeTo(output, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(clazz)));
            return Arrays.copyOf(output.getBuffer(), output.getSize());
        } finally { output.close(); }
    }

    private static String sha(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder value = new StringBuilder();
        for (byte b : digest) value.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return value.toString();
    }

    private static Map<String, String> resources(String path) throws Exception {
        Map<String, String> entries = new TreeMap<>();
        try (ZipFile zip = new ZipFile(path)) {
            java.util.Enumeration<? extends ZipEntry> enumeration = zip.entries();
            Set<String> allNames = new TreeSet<>();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                String name = entry.getName();
                check(allNames.add(name), "Duplicate APK ZIP entry: " + name);
                if (entry.isDirectory() || name.matches("classes(?:[0-9]+)?\\.dex")
                        || name.startsWith("META-INF/")) continue;
                try (java.io.InputStream stream = zip.getInputStream(entry);
                     java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
                    byte[] buffer = new byte[65536];
                    int size;
                    while ((size = stream.read(buffer)) != -1) output.write(buffer, 0, size);
                    entries.put(name, sha(output.toByteArray()));
                }
            }
        }
        return entries;
    }

    private static Map<String, Object> row(String owner, String method, int index, Object reference) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("class", owner); result.put("method", method); result.put("instruction_index", index);
        result.put("reference", String.valueOf(reference));
        return result;
    }

    private static Map<String, Object> scan(Map<String, ClassDef> classes) {
        List<Object> fields = new ArrayList<>(), methods = new ArrayList<>(), nonPublic = new ArrayList<>();
        List<Object> strings = new ArrayList<>(), nearbyReflection = new ArrayList<>();
        int scannedMethods = 0, scannedInstructions = 0;
        for (ClassDef clazz : classes.values()) for (Method method : clazz.getMethods()) {
            if (method.getImplementation() == null) continue;
            scannedMethods++;
            List<Instruction> instructions = new ArrayList<>();
            for (Instruction instruction : method.getImplementation().getInstructions()) instructions.add(instruction);
            String descriptor = DexFormatter.INSTANCE.getMethodDescriptor(method);
            for (int index = 0; index < instructions.size(); index++) {
                scannedInstructions++;
                Instruction instruction = instructions.get(index);
                if (!(instruction instanceof ReferenceInstruction)) continue;
                Object reference = ((ReferenceInstruction) instruction).getReference();
                if (reference instanceof FieldReference && QUEUE.equals(((FieldReference) reference).getDefiningClass()))
                    fields.add(row(clazz.getType(), descriptor, index, reference));
                if (reference instanceof MethodReference && QUEUE.equals(((MethodReference) reference).getDefiningClass())) {
                    Map<String, Object> finding = row(clazz.getType(), descriptor, index, reference);
                    methods.add(finding);
                    if (!PUBLIC_QUEUE_METHODS.contains(((MethodReference) reference).getName())) nonPublic.add(finding);
                }
                if (reference instanceof StringReference && QUEUE_STRINGS.contains(((StringReference) reference).getString())) {
                    String value = ((StringReference) reference).getString();
                    Map<String, Object> finding = row(clazz.getType(), descriptor, index, value);
                    strings.add(finding);
                    // A bounded proximity clue, not an assertion that this value
                    // flows into the reflective call's name parameter.
                    for (int other = Math.max(0, index - 12); other <= Math.min(instructions.size() - 1, index + 12); other++) {
                        Instruction adjacent = instructions.get(other);
                        if (!(adjacent instanceof ReferenceInstruction)) continue;
                        Object adjacentRef = ((ReferenceInstruction) adjacent).getReference();
                        if (!(adjacentRef instanceof MethodReference)) continue;
                        MethodReference called = (MethodReference) adjacentRef;
                        if ("Ljava/lang/Class;".equals(called.getDefiningClass()) &&
                                ("getDeclaredField".equals(called.getName()) || "getDeclaredMethod".equals(called.getName()))) {
                            Map<String, Object> clue = new LinkedHashMap<>(finding);
                            clue.put("reflection_instruction_index", other);
                            clue.put("reflection_call", called.toString());
                            clue.put("data_flow_verified", false);
                            nearbyReflection.add(clue);
                        }
                    }
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", "Original APK DEX instruction references and selected string constants only");
        result.put("exhaustive_reflection_analysis", false);
        result.put("native_or_downloaded_code_scanned", false);
        result.put("scanned_methods", scannedMethods); result.put("scanned_instructions", scannedInstructions);
        result.put("direct_messagequeue_fields", fields); result.put("direct_messagequeue_methods", methods);
        result.put("nonpublic_messagequeue_method_candidates", nonPublic);
        result.put("messagequeue_related_strings", strings); result.put("nearby_reflection_candidates", nearbyReflection);
        result.put("limitations", "No findings does not prove runtime compatibility: dynamically assembled/encrypted field names, interprocedural reflection, native code and externally loaded code are not resolved.");
        return result;
    }

    private static boolean allowedAdded(String type, boolean all) {
        return type.startsWith(RUNTIME) || all && (type.startsWith("Lhiro/berry/")
            || type.startsWith("Lhiro/berryzoom/") || type.startsWith("Lhiro/berryshare/")
            || type.equals("LHiroBerryBrowserChooser;") || type.startsWith("LHiroBerryBrowserChooser$"));
    }

    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?, ?>) {
            List<String> entries = new ArrayList<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) entries.add(json(entry.getKey().toString()) + ":" + json(entry.getValue()));
            return "{" + String.join(",", entries) + "}";
        }
        if (value instanceof Collection<?>) {
            List<String> entries = new ArrayList<>();
            for (Object item : (Collection<?>) value) entries.add(json(item));
            return "[" + String.join(",", entries) + "]";
        }
        StringBuilder text = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            if (c == '"' || c == '\\') text.append('\\').append(c);
            else if (c < 32) text.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int)c));
            else text.append(c);
        }
        return text.append('"').toString();
    }

    public static void main(String[] args) throws Exception {
        check(args.length == 4, "Expected queue|all, input APK, output APK, report JSON");
        boolean all = "all".equals(args[0]);
        check(all || "queue".equals(args[0]), "Unknown audit mode");
        Map<String, ClassDef> original = read(args[1]), patched = read(args[2]);
        String inputDigest = sha(Files.readAllBytes(Paths.get(args[1])));
        Set<String> expectedChanges = Collections.emptySet();
        if (all) {
            if (inputDigest.equals("4a4da9029d1d71edf376b2d1edf3636e3fba946b49d1fafa5787d20252c8ad5b")) expectedChanges = OLD_MUTATIONS;
            else if ((inputDigest.equals("63d6f38c50c1cdca7a6c0bfed7abffa0183817aa33741b5bcb3bd287ef3342ac")
                || inputDigest.equals("fe0586a46ab1fa8b7386078b093f42585bc1383ef6c385fedfa73d07aa78a2b6")
                || inputDigest.equals("0e604165f45511130265d0480f78cd42616400fa4ee990c9fc58df8bbbdbb5b0"))) expectedChanges = CURRENT_MUTATIONS;
            else throw new AssertionError("Unreviewed input APK hash for combined audit");
        }
        check(patched.keySet().containsAll(original.keySet()), "Original classes were removed");
        Set<String> changed = new TreeSet<>(), added = new TreeSet<>(patched.keySet());
        added.removeAll(original.keySet());
        for (String name : added) check(allowedAdded(name, all), "Unexpected injected class: " + name);
        for (String name : original.keySet()) {
            if (!Arrays.equals(canonical(original.get(name)), canonical(patched.get(name)))) {
                changed.add(name);
                check(all && expectedChanges.contains(name), "Unexpected original class mutation: " + name);
            }
        }
        check(changed.equals(expectedChanges), "Expected all selected original class modifications: " + changed);
        Map<String, String> originalResources = resources(args[1]), patchedResources = resources(args[2]);
        List<Object> resourceChanges = new ArrayList<>();
        Set<String> names = new TreeSet<>(originalResources.keySet()); names.addAll(patchedResources.keySet());
        int unchangedResources = 0;
        for (String name : names) {
            String before = originalResources.get(name), after = patchedResources.get(name);
            if (before != null && before.equals(after)) { unchangedResources++; continue; }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("entry", name); item.put("input_sha256", before); item.put("output_sha256", after);
            resourceChanges.add(item);
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("status", "PASS"); report.put("mode", args[0]);
        report.put("input_sha256", sha(Files.readAllBytes(Paths.get(args[1]))));
        report.put("output_sha256", sha(Files.readAllBytes(Paths.get(args[2]))));
        report.put("input_classes", original.size()); report.put("output_classes", patched.size());
        report.put("original_classes_removed", 0); report.put("original_classes_changed", changed);
        report.put("original_classes_canonically_preserved", original.size() - changed.size());
        report.put("added_classes", added); report.put("input_messagequeue_scan", scan(original));
        report.put("resource_entries_compared_excluding_dex_and_signatures", names.size());
        report.put("resource_entries_byte_identical", unchangedResources); report.put("resource_changes", resourceChanges);
        report.put("resource_changes_are_reported_not_semantically_verified", true);
        report.put("android_device_execution", false); report.put("assertions", assertions);
        Path destination = Paths.get(args[3]).toAbsolutePath(); Files.createDirectories(destination.getParent());
        Files.write(destination, (json(report) + "\n").getBytes(StandardCharsets.UTF_8));
        System.out.println("PASS AppDexAudit mode=" + args[0] + " original=" + original.size()
            + " preserved=" + (original.size() - changed.size()) + " changed=" + changed
            + " added=" + added.size() + " resource_changes=" + resourceChanges.size());
    }
}
