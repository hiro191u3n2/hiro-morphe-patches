import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;

/** Add punctuation helper after the untouched prior KeySwap transformation. */
public final class Merge1817 {
    private static final String KEY = "Lhiro/swiftkey/KeySwap;";
    private static final String WRAPPED = "Lhiro/swiftkey/KeySwap$Wrapped;";
    private static final String HELPER = "Lhiro/swiftkey/PunctuationKeys;";
    private static final String PRIOR = "wrapBeforePunctuation1817";
    private static final List<String> ARGS = Arrays.asList("Lsc0/c;", "Landroid/content/res/Resources;");
    private static final List<String> NEXT_ARGS = Arrays.asList("Lsc0/c;", "Landroid/content/res/Resources;", WRAPPED);
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;
    private static void require(boolean ok, String message) { if (!ok) throw new IllegalStateException(message); }
    static boolean helper(String type) { return type.equals(HELPER) || type.startsWith("Lhiro/swiftkey/PunctuationKeys$"); }
    static TreeMap<String, ClassDef> load(String path) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(path), Opcodes.forApi(26));
        TreeMap<String, ClassDef> classes = new TreeMap<>();
        for (ClassDef c : dex.getClasses()) require(classes.put(c.getType(), c) == null, "Duplicate class " + c.getType());
        return classes;
    }
    private static byte[] canonical(ClassDef c) throws Exception {
        DexPool pool = new DexPool(Opcodes.forApi(26)); pool.internClass(c);
        MemoryDataStore out = new MemoryDataStore();
        try { pool.writeTo(out); return Arrays.copyOf(out.getBuffer(), out.getSize()); }
        finally { out.close(); }
    }
    private static Method find(ClassDef c, String name, List<String> parameters, String result) {
        require(c != null, "Missing class for " + name);
        Method found = null;
        for (Method m : c.getMethods()) if (m.getName().equals(name) && m.getParameterTypes().equals(parameters) && m.getReturnType().equals(result)) {
            require(found == null, "Duplicate method " + name); found = m;
        }
        require(found != null, "Missing method " + name); return found;
    }
    private static Method rename(Method original, String name) {
        return new ImmutableMethod(original.getDefiningClass(), name, original.getParameters(), original.getReturnType(),
                original.getAccessFlags(), original.getAnnotations(), original.getHiddenApiRestrictions(), original.getImplementation());
    }
    private static ClassDef withMethods(ClassDef original, List<Method> methods) {
        return new ImmutableClassDef(original.getType(), original.getAccessFlags(), original.getSuperclass(), original.getInterfaces(),
                original.getSourceFile(), original.getAnnotations(), original.getFields(), methods);
    }
    private static Method wrapper(Method original) {
        MutableMethodImplementation body = new MutableMethodImplementation(3);
        body.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2, 1, 2, 0, 0, 0,
                new ImmutableMethodReference(KEY, PRIOR, ARGS, WRAPPED)));
        body.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
        body.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 3, 1, 2, 0, 0, 0,
                new ImmutableMethodReference(HELPER, "wrap", NEXT_ARGS, WRAPPED)));
        body.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
        body.addInstruction(new BuilderInstruction11x(Opcode.RETURN_OBJECT, 0));
        return new ImmutableMethod(KEY, "wrap", original.getParameters(), WRAPPED, original.getAccessFlags(),
                original.getAnnotations(), original.getHiddenApiRestrictions(), body);
    }
    private static ClassDef patch(ClassDef original) {
        Method prior = find(original, "wrap", ARGS, WRAPPED);
        require(prior.getAccessFlags() == 9 && prior.getImplementation() != null
                && prior.getImplementation().getRegisterCount() == 13, "Unexpected baseline KeySwap.wrap contract");
        for (Method m : original.getMethods()) require(!m.getName().equals(PRIOR), "Punctuation wrapper already installed");
        for (Instruction ins : prior.getImplementation().getInstructions()) if (ins instanceof ReferenceInstruction) {
            Object ref = ((ReferenceInstruction) ins).getReference();
            if (ref instanceof MethodReference) {
                MethodReference call = (MethodReference) ref;
                require(!(call.getDefiningClass().equals(KEY) && call.getName().equals("wrap")), "Unexpected recursive prior wrapper");
            }
        }
        List<Method> methods = new ArrayList<>();
        for (Method m : original.getMethods()) methods.add(FORMAT.getMethodDescriptor(m).equals(FORMAT.getMethodDescriptor(prior)) ? rename(m, PRIOR) : m);
        methods.add(wrapper(prior)); return withMethods(original, methods);
    }
    private static ClassDef restore(ClassDef patched) {
        Method current = find(patched, "wrap", ARGS, WRAPPED);
        Method prior = find(patched, PRIOR, ARGS, WRAPPED);
        List<Method> methods = new ArrayList<>();
        for (Method m : patched.getMethods()) {
            String descriptor = FORMAT.getMethodDescriptor(m);
            if (descriptor.equals(FORMAT.getMethodDescriptor(current))) continue;
            methods.add(descriptor.equals(FORMAT.getMethodDescriptor(prior)) ? rename(m, "wrap") : m);
        }
        return withMethods(patched, methods);
    }
    private static void verifyWrapper(ClassDef c) {
        Method method = find(c, "wrap", ARGS, WRAPPED);
        require(method.getAccessFlags() == 9 && method.getImplementation().getRegisterCount() == 3, "Wrapper ABI/registers changed");
        List<Instruction> code = new ArrayList<>(); for (Instruction ins : method.getImplementation().getInstructions()) code.add(ins);
        require(code.size() == 5 && method.getImplementation().getTryBlocks().isEmpty(), "Wrapper exceeds two-call straight-line chain");
        Opcode[] expected = {Opcode.INVOKE_STATIC, Opcode.MOVE_RESULT_OBJECT, Opcode.INVOKE_STATIC, Opcode.MOVE_RESULT_OBJECT, Opcode.RETURN_OBJECT};
        for (int i = 0; i < expected.length; i++) require(code.get(i).getOpcode() == expected[i], "Wrapper dispatch changed");
        for (int i : new int[] {1,3,4}) require(((OneRegisterInstruction) code.get(i)).getRegisterA() == 0, "Wrapper result register changed");
        FiveRegisterInstruction first = (FiveRegisterInstruction) code.get(0), next = (FiveRegisterInstruction) code.get(2);
        require(first.getRegisterCount() == 2 && first.getRegisterC() == 1 && first.getRegisterD() == 2, "Prior arguments changed");
        require(next.getRegisterCount() == 3 && next.getRegisterC() == 1 && next.getRegisterD() == 2 && next.getRegisterE() == 0, "Postprocessor arguments changed");
        require(FORMAT.getReference(((ReferenceInstruction)code.get(0)).getReference()).equals(KEY+"->"+PRIOR+"(Lsc0/c;Landroid/content/res/Resources;)"+WRAPPED), "Prior call differs");
        require(FORMAT.getReference(((ReferenceInstruction)code.get(2)).getReference()).equals(HELPER+"->wrap(Lsc0/c;Landroid/content/res/Resources;"+WRAPPED+")"+WRAPPED), "Helper call differs");
    }
    private static String sha(String path) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(path)))); }
    public static void main(String[] args) throws Exception {
        require(args.length == 3 || args.length == 4, "Usage: Merge1817 baseline.mpe helper.dex output.mpe [merge-review.json]");
        TreeMap<String, ClassDef> baseline = load(args[0]), additions = load(args[1]);
        require(baseline.size() == 71 && baseline.containsKey(KEY) && baseline.containsKey("Lhiro/swiftkey/CandidateScroll;")
                && baseline.containsKey("Lhiro/swiftkey/CandidateToolbar;") && baseline.containsKey("Lhiro/swiftkey/EmojiToolbar;"), "Unexpected1.8.16 runtime inventory");
        Method entry = find(additions.get(HELPER), "wrap", NEXT_ARGS, WRAPPED);
        require(entry.getAccessFlags() == 9 && entry.getImplementation() != null, "Punctuation helper entry ABI differs");
        for (ClassDef c : additions.values()) {
            require(helper(c.getType()) && !baseline.containsKey(c.getType()), "Unexpected/replacing overlay " + c.getType());
            for (Method m : c.getMethods()) if (m.getImplementation()!=null) for (Instruction ins : m.getImplementation().getInstructions())
                if (ins instanceof ReferenceInstruction && ((ReferenceInstruction)ins).getReference() instanceof MethodReference) {
                    MethodReference call = (MethodReference)((ReferenceInstruction)ins).getReference();
                    require(!(call.getDefiningClass().equals(KEY) && (call.getName().equals("wrap") || call.getName().equals(PRIOR))), "Postprocessor would repeat prior conversion or recurse");
                }
        }
        TreeMap<String, ClassDef> merged = new TreeMap<>(baseline); merged.put(KEY, patch(baseline.get(KEY))); merged.putAll(additions);
        require(Arrays.equals(canonical(baseline.get(KEY)), canonical(restore(merged.get(KEY)))), "Prior KeySwap body/metadata changed before serialization");
        verifyWrapper(merged.get(KEY));
        DexPool pool = new DexPool(Opcodes.forApi(26)); for (ClassDef c : merged.values()) pool.internClass(c);
        FileDataStore out = new FileDataStore(new File(args[2])); try { pool.writeTo(out); } finally { out.close(); }
        TreeMap<String, ClassDef> result = load(args[2]); require(result.keySet().equals(merged.keySet()), "Output class set differs");
        for (ClassDef c : baseline.values()) if (!c.getType().equals(KEY))
            require(Arrays.equals(canonical(c), canonical(result.get(c.getType()))), "Unrelated existing runtime changed " + c.getType());
        for (ClassDef c : additions.values()) require(Arrays.equals(canonical(c), canonical(result.get(c.getType()))), "Compiled helper changed in merge");
        require(Arrays.equals(canonical(baseline.get(KEY)), canonical(restore(result.get(KEY)))), "Serialized prior KeySwap body/metadata changed");
        verifyWrapper(result.get(KEY));
        if (args.length == 4) {
            List<String> added = new ArrayList<>();
            for (String type : additions.keySet()) added.add("\"" + type + "\"");
            String report = "{\n  \"status\": \"PASSED\",\n"
                + "  \"scope\": \"Lossless existing runtime preservation and exact punctuation wrapper DEX integration\",\n"
                + "  \"baseline_extension_sha256\": \"" + sha(args[0]) + "\",\n"
                + "  \"extension_sha256\": \"" + sha(args[2]) + "\",\n"
                + "  \"overlay_sha256\": \"" + sha(args[1]) + "\",\n"
                + "  \"runtime_before_classes\": " + baseline.size() + ",\n"
                + "  \"runtime_after_classes\": " + result.size() + ",\n"
                + "  \"unchanged_existing_classes\": 70,\n"
                + "  \"changed_existing_classes\": [\"Lhiro/swiftkey/KeySwap;\"],\n"
                + "  \"added_classes\": [" + String.join(",", added) + "],\n"
                + "  \"prior_wrap_body_restored_byte_identical\": true,\n"
                + "  \"wrapper_calls_prior_once\": true,\n"
                + "  \"wrapper_calls_postprocessor_once\": true,\n"
                + "  \"wrapper_instruction_count\": 5,\n"
                + "  \"no_recursive_helper_calls\": true,\n"
                + "  \"compiled_helper_classes_preserved\": true,\n"
                + "  \"device_tested\": false,\n  \"original_apk_apply_tested\": false\n}\n";
            Path reportPath = Path.of(args[3]); Files.createDirectories(reportPath.toAbsolutePath().getParent()); Files.writeString(reportPath, report);
        }
        System.out.println("PASS punctuation merge:70 existing classes identical; KeySwap.wrap renamed losslessly +5instruction wrapper; helpers="+additions.size()+" total="+result.size());
    }
}
