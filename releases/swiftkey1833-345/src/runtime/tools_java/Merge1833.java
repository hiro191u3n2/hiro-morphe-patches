import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;

/** Replace SpaceConversion and only FrequencyLearning.rank in pinned 1.8.32. */
public final class Merge1833 {
    private static final String FAMILY = "Lhiro/swiftkey/SpaceConversion";
    private static final String RANK_OWNER = "Lhiro/swiftkey/FrequencyLearning;";
    private static final String RANK = RANK_OWNER + "->rank(Ljava/util/List;Ll40/c;Z)Ljava/util/List;";
    private static final String BASELINE_SHA = "377de86d964dea1f66515dae59b098dbdeda0cb0e5b469cd215e7c2e6aad0834";
    private static final int PUBLIC = 1, PRIVATE = 2, PROTECTED = 4, STATIC = 8, FINAL = 16, INTERFACE = 0x200;
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;

    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }
    private static boolean family(String type) {
        return type.equals(FAMILY + ";") || type.startsWith(FAMILY + "$");
    }
    private static boolean modified(String type) { return family(type) || type.equals(RANK_OWNER); }
    private static Method rank(ClassDef owner) {
        require(owner != null, "Ranking class absent");
        Method found = null;
        for (Method m : owner.getMethods()) if (FORMAT.getMethodDescriptor(m).equals(RANK)) {
            require(found == null, "Duplicate rank method"); found = m;
        }
        require(found != null && found.getImplementation() != null, "Rank implementation absent");
        return found;
    }
    private static ClassDef withRank(ClassDef original, Method donor) {
        Method old = rank(original);
        require(FORMAT.getMethodDescriptor(donor).equals(RANK), "Wrong rank donor descriptor");
        require(donor.getAccessFlags() == old.getAccessFlags(), "Rank donor changes access/opcode contract");
        Method replacement = new ImmutableMethod(old.getDefiningClass(), old.getName(), old.getParameters(),
                old.getReturnType(), old.getAccessFlags(), old.getAnnotations(), old.getHiddenApiRestrictions(),
                donor.getImplementation());
        List<Method> methods = new ArrayList<>();
        for (Method m : original.getMethods()) methods.add(FORMAT.getMethodDescriptor(m).equals(RANK) ? replacement : m);
        return new ImmutableClassDef(original.getType(), original.getAccessFlags(), original.getSuperclass(),
                original.getInterfaces(), original.getSourceFile(), original.getAnnotations(), original.getFields(), methods);
    }
    private static byte[] canonicalRank(ClassDef owner) throws Exception {
        return canonical(new ImmutableClassDef(RANK_OWNER, PUBLIC, "Ljava/lang/Object;", Collections.emptyList(),
                null, Collections.emptySet(), Collections.emptyList(), Collections.singletonList(rank(owner))));
    }
    private static String digest(byte[] raw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
    }
    private static TreeMap<String, ClassDef> load(String path) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(path), Opcodes.forApi(26));
        TreeMap<String, ClassDef> result = new TreeMap<>();
        for (ClassDef c : dex.getClasses())
            require(result.put(c.getType(), c) == null, "Duplicate class " + c.getType());
        return result;
    }
    private static byte[] canonical(ClassDef c) throws Exception {
        require(c != null, "Missing class for canonical comparison");
        DexPool pool = new DexPool(Opcodes.forApi(26));
        pool.internClass(c);
        MemoryDataStore out = new MemoryDataStore();
        try { pool.writeTo(out); return Arrays.copyOf(out.getBuffer(), out.getSize()); }
        finally { out.close(); }
    }
    private static String sha(String path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(path))));
    }
    private static Method method(ClassDef owner, MethodReference ref) {
        if (owner == null) return null;
        String descriptor = FORMAT.getMethodDescriptor(ref);
        for (Method m : owner.getMethods())
            if (FORMAT.getMethodDescriptor(m).equals(descriptor)) return m;
        return null;
    }
    private static Field field(ClassDef owner, FieldReference ref) {
        if (owner == null) return null;
        for (Field f : owner.getFields())
            if (f.getName().equals(ref.getName()) && f.getType().equals(ref.getType())) return f;
        return null;
    }
    private static String pkg(String type) {
        int slash = type.lastIndexOf('/');
        return slash < 0 ? "" : type.substring(0, slash);
    }
    private static boolean accessible(String caller, ClassDef owner, int memberAccess) {
        if (owner == null) return false;
        boolean samePackage = pkg(caller).equals(pkg(owner.getType()));
        if ((owner.getAccessFlags() & PUBLIC) == 0 && !samePackage) return false;
        if ((memberAccess & PUBLIC) != 0) return true;
        if ((memberAccess & PRIVATE) != 0) return caller.equals(owner.getType());
        // Existing consumers are in the same package; do not guess protected subclass access.
        return samePackage;
    }
    private static List<String> checkPublicApi(Map<String, ClassDef> baseline, Map<String, ClassDef> result) {
        List<String> checked = new ArrayList<>();
        for (ClassDef oldClass : baseline.values()) {
            if (!modified(oldClass.getType())) continue;
            ClassDef replacement = result.get(oldClass.getType());
            require(replacement != null, "Existing SpaceConversion class removed: " + oldClass.getType());
            require(oldClass.getAccessFlags() == replacement.getAccessFlags(), "Existing class access changed: " + oldClass.getType());
            require(Objects.equals(oldClass.getSuperclass(), replacement.getSuperclass())
                    && oldClass.getInterfaces().equals(replacement.getInterfaces()), "Existing class hierarchy changed: " + oldClass.getType());
            for (Method old : oldClass.getMethods()) {
                if ((old.getAccessFlags() & (PUBLIC | PROTECTED)) == 0) continue;
                Method next = method(replacement, old);
                String descriptor = FORMAT.getMethodDescriptor(old);
                require(next != null, "Existing public/protected method removed: " + descriptor);
                int mask = PUBLIC | PRIVATE | PROTECTED | STATIC;
                require((old.getAccessFlags() & mask) == (next.getAccessFlags() & mask), "Existing method ABI changed: " + descriptor);
                require(old.getImplementation() == null || next.getImplementation() != null, "Existing implemented API became abstract: " + descriptor);
                checked.add(descriptor);
            }
            for (Field old : oldClass.getFields()) {
                if ((old.getAccessFlags() & (PUBLIC | PROTECTED)) == 0) continue;
                Field next = field(replacement, old);
                String descriptor = FORMAT.getFieldDescriptor(old);
                require(next != null, "Existing public/protected field removed: " + descriptor);
                int mask = PUBLIC | PRIVATE | PROTECTED | STATIC | FINAL;
                require((old.getAccessFlags() & mask) == (next.getAccessFlags() & mask), "Existing field ABI changed: " + descriptor);
                require(Objects.equals(old.getInitialValue(), next.getInitialValue()), "Existing API constant changed: " + descriptor);
                checked.add(descriptor);
            }
        }
        Collections.sort(checked);
        return checked;
    }
    private static void checkReference(String caller, Instruction ins, Reference ref,
            Map<String, ClassDef> result, Set<String> checked) {
        String owner;
        if (ref instanceof MethodReference) owner = ((MethodReference) ref).getDefiningClass();
        else if (ref instanceof FieldReference) owner = ((FieldReference) ref).getDefiningClass();
        else if (ref instanceof TypeReference) {
            owner = ((TypeReference) ref).getType();
            while (owner.startsWith("[")) owner = owner.substring(1);
        } else return;
        if (!modified(owner)) return;
        ClassDef target = result.get(owner);
        require(target != null, "Existing runtime references removed SpaceConversion type: " + owner);
        String opcode = ins.getOpcode().name().toLowerCase(Locale.ROOT).replace('_', '-');
        if (ref instanceof MethodReference) {
            MethodReference mr = (MethodReference) ref;
            Method m = method(target, mr);
            require(m != null, "Existing runtime method reference unresolved: " + FORMAT.getReference(ref));
            require(accessible(caller, target, m.getAccessFlags()), "Existing runtime method inaccessible: " + FORMAT.getReference(ref));
            require(((m.getAccessFlags() & STATIC) != 0) == opcode.startsWith("invoke-static"), "Existing runtime method static/instance mismatch: " + FORMAT.getReference(ref));
            if (opcode.startsWith("invoke-interface")) require((target.getAccessFlags() & INTERFACE) != 0, "Interface invocation owner changed");
            if (opcode.startsWith("invoke-virtual")) require((target.getAccessFlags() & INTERFACE) == 0, "Virtual invocation owner changed");
        } else if (ref instanceof FieldReference) {
            Field f = field(target, (FieldReference) ref);
            require(f != null, "Existing runtime field reference unresolved: " + FORMAT.getReference(ref));
            require(accessible(caller, target, f.getAccessFlags()), "Existing runtime field inaccessible: " + FORMAT.getReference(ref));
            require(((f.getAccessFlags() & STATIC) != 0) == (opcode.startsWith("sget") || opcode.startsWith("sput")), "Existing runtime field static/instance mismatch: " + FORMAT.getReference(ref));
        } else {
            require((target.getAccessFlags() & PUBLIC) != 0 || pkg(caller).equals(pkg(owner)), "Existing runtime type inaccessible: " + owner);
        }
        checked.add(caller + " | " + opcode + " | " + FORMAT.getReference(ref));
    }
    private static Set<String> checkExistingReferences(Map<String, ClassDef> baseline, Map<String, ClassDef> result) {
        Set<String> checked = new TreeSet<>();
        for (ClassDef c : baseline.values()) {
            if (modified(c.getType())) continue;
            for (Method m : c.getMethods()) {
                if (m.getImplementation() == null) continue;
                for (Instruction ins : m.getImplementation().getInstructions()) {
                    if (ins instanceof ReferenceInstruction)
                        checkReference(c.getType(), ins, ((ReferenceInstruction) ins).getReference(), result, checked);
                    if (ins instanceof DualReferenceInstruction)
                        checkReference(c.getType(), ins, ((DualReferenceInstruction) ins).getReference2(), result, checked);
                }
            }
        }
        // Entrypoint is called from the unchanged patch-injected native method, not another extension class.
        return checked;
    }
    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map) {
            StringJoiner out = new StringJoiner(",", "{", "}");
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) out.add(json(e.getKey().toString()) + ":" + json(e.getValue()));
            return out.toString();
        }
        if (value instanceof Iterable) {
            StringJoiner out = new StringJoiner(",", "[", "]");
            for (Object e : (Iterable<?>) value) out.add(json(e));
            return out.toString();
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            if (c == '\\' || c == '\"') out.append('\\').append(c);
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else if (c < 32) out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
    public static void main(String[] args) throws Exception {
        require(args.length == 5, "Usage: Merge1833 baseline.mpe space.dex rank.dex output.mpe report.json");
        require(sha(args[0]).equals(BASELINE_SHA), "Pinned 1.8.32 baseline extension SHA differs");
        TreeMap<String, ClassDef> baseline = load(args[0]), overlay = load(args[1]), ranking = load(args[2]);
        require(baseline.size() == 92, "Unexpected 1.8.32 runtime class count");
        require(overlay.keySet().equals(Collections.singleton(FAMILY + ";")), "Space donor must contain its single class only");
        require(ranking.keySet().equals(Collections.singleton(RANK_OWNER)), "Ranking donor must contain its single class only");
        require(baseline.containsKey(FAMILY + ";") && baseline.containsKey(RANK_OWNER), "Baseline changed-class inventory differs");
        ClassDef oldRankOwner = baseline.get(RANK_OWNER);
        Method donorRank = rank(ranking.get(RANK_OWNER));
        TreeMap<String, ClassDef> merged = new TreeMap<>(baseline);
        merged.putAll(overlay);
        merged.put(RANK_OWNER, withRank(oldRankOwner, donorRank));
        require(!Arrays.equals(canonicalRank(oldRankOwner), canonicalRank(merged.get(RANK_OWNER))), "Rank replacement has no change");
        List<String> api = checkPublicApi(baseline, merged);
        Set<String> consumers = checkExistingReferences(baseline, merged);
        DexPool pool = new DexPool(Opcodes.forApi(26));
        for (ClassDef c : merged.values()) pool.internClass(c);
        FileDataStore out = new FileDataStore(new File(args[3]));
        try { pool.writeTo(out); } finally { out.close(); }
        TreeMap<String, ClassDef> result = load(args[3]);
        require(result.keySet().equals(baseline.keySet()), "Serialized output class set differs");
        List<String> unchanged = new ArrayList<>(), changed = new ArrayList<>();
        for (ClassDef c : baseline.values()) {
            boolean equal = Arrays.equals(canonical(c), canonical(result.get(c.getType())));
            if (!modified(c.getType())) require(equal, "Unrelated existing runtime changed: " + c.getType());
            (equal ? unchanged : changed).add(c.getType());
        }
        require(changed.equals(Arrays.asList(RANK_OWNER, FAMILY + ";")), "Expected exactly rank and SpaceConversion changes");
        require(Arrays.equals(canonical(overlay.get(FAMILY + ";")), canonical(result.get(FAMILY + ";"))), "Compiled SpaceConversion changed during merge");
        require(Arrays.equals(canonical(merged.get(RANK_OWNER)), canonical(result.get(RANK_OWNER))), "Compiled rank body changed during serialization");
        require(Arrays.equals(canonical(oldRankOwner), canonical(withRank(result.get(RANK_OWNER), rank(oldRankOwner)))),
                "Restoring original rank does not restore exact original FrequencyLearning class");
        require(checkPublicApi(baseline, result).equals(api), "Serialized API verification changed");
        require(checkExistingReferences(baseline, result).equals(consumers), "Serialized consumer reference verification changed");
        require(result.size() == 92 && unchanged.size() == 90, "Final preservation inventory differs");
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("status", "PASSED");
        report.put("scope", "SpaceConversion class replacement plus FrequencyLearning.rank method-body graft; all other members/classes canonical-identical");
        report.put("baseline_extension_sha256", sha(args[0]));
        report.put("space_overlay_sha256", sha(args[1]));
        report.put("rank_overlay_sha256", sha(args[2]));
        report.put("extension_sha256", sha(args[3]));
        report.put("runtime_before_classes", baseline.size());
        report.put("runtime_after_classes", result.size());
        report.put("baseline_classes", baseline.size());
        report.put("unrelated_existing_classes", 90);
        report.put("unrelated_existing_classes_byte_identical", true);
        report.put("unchanged_existing_classes", unchanged.size());
        report.put("unchanged_existing_class_paths", unchanged);
        report.put("changed_existing_classes", changed);
        report.put("added_classes", Collections.emptyList());
        report.put("existing_class_paths_preserved", true);
        report.put("old_public_api_preserved", true);
        report.put("existing_entrypoints_preserved", true);
        report.put("verified_public_api", api);
        report.put("existing_runtime_references_resolved", true);
        report.put("existing_external_references_resolve", true);
        report.put("verified_existing_runtime_references", consumers);
        report.put("compiled_helper_classes_preserved", true);
        report.put("rank_method_only_replacement", true);
        report.put("rank_method_descriptor", RANK);
        report.put("rank_method_before_sha256", digest(canonicalRank(oldRankOwner)));
        report.put("rank_method_after_sha256", digest(canonicalRank(result.get(RANK_OWNER))));
        report.put("frequency_other_members_preserved", true);
        report.put("inverse_rank_restores_original_class", true);
        report.put("compiled_rank_body_preserved", true);
        report.put("frequency_nested_classes_preserved", true);
        report.put("device_tested", false);
        report.put("original_apk_apply_tested", false);
        Path reportPath = Path.of(args[4]);
        Files.createDirectories(reportPath.toAbsolutePath().getParent());
        Files.writeString(reportPath, json(report) + "\n");
        System.out.println("PASS context-selection merge: 90 unrelated classes identical; FrequencyLearning only rank body changed; SpaceConversion API retained");
    }
}
