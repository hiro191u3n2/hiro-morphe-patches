import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction35c;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Keeps the exact original DEX closure except two calls to the logical resolver. */
public final class HomeClosureDex {
    static final Opcodes OPS = Opcodes.forApi(26);
    static final String HOME = "Lapp/hiro/tripcom/patches/HideHomeRecommendationsPatchKt$hideHomeRecommendationsPatch$1$1;";
    static final String CONTEXT = "Lapp/morphe/patcher/patch/ResourcePatchContext;";
    static final String HELPER = "Lapp/hiro/tripcom/patches/HomeResourceDocuments;";
    static final String STRING = "Ljava/lang/String;";
    static final String DOCUMENT = "Lapp/morphe/patcher/util/Document;";
    static void req(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    static Map<String, ClassDef> load(String path) throws Exception {
        var result = new TreeMap<String, ClassDef>();
        var input = DexFileFactory.loadDexContainer(Path.of(path).toFile(), OPS);
        for (String entry : input.getDexEntryNames()) for (ClassDef c : input.getEntry(entry).getDexFile().getClasses())
            req(result.put(c.getType(), ImmutableClassDef.of(c)) == null, "Duplicate class " + c.getType());
        return result;
    }
    static byte[] canonical(ClassDef c) throws Exception {
        MemoryDataStore data = new MemoryDataStore();
        try {
            DexPool.writeTo(data, new ImmutableDexFile(OPS, Collections.singleton(c)));
            return Arrays.copyOf(data.getBuffer(), data.getSize());
        } finally { data.close(); }
    }
    static ClassDef transform(ClassDef c, boolean forward) {
        req(c != null && HOME.equals(c.getType()), "Original Home closure missing");
        int replaced = 0;
        List<Method> methods = new ArrayList<>();
        for (Method method : c.getMethods()) {
            MethodImplementation body = method.getImplementation();
            if (body == null) { methods.add(method); continue; }
            List<Instruction> instructions = new ArrayList<>();
            for (Instruction instruction : body.getInstructions()) {
                Instruction result = instruction;
                if (instruction instanceof ReferenceInstruction reference && reference.getReference() instanceof MethodReference ref
                        && (forward ? CONTEXT : HELPER).equals(ref.getDefiningClass()) && "document".equals(ref.getName())) {
                    req(method.getName().equals("invoke") && method.getReturnType().equals("V")
                            && method.getParameterTypes().equals(List.of(CONTEXT)), "Document call outside original typed invoke");
                    req(ref.getReturnType().equals(DOCUMENT) && ref.getParameterTypes().equals(forward ? List.of(STRING) : List.of(CONTEXT, STRING)),
                            "Document ABI differs");
                    req(instruction.getOpcode() == (forward ? Opcode.INVOKE_VIRTUAL : Opcode.INVOKE_STATIC)
                            && instruction instanceof FiveRegisterInstruction, "Document opcode differs");
                    FiveRegisterInstruction call = (FiveRegisterInstruction) instruction;
                    req(call.getRegisterCount() == 2, "Document register count differs");
                    var replacement = new ImmutableMethodReference(forward ? HELPER : CONTEXT, "document",
                            forward ? List.of(CONTEXT, STRING) : List.of(STRING), DOCUMENT);
                    result = new ImmutableInstruction35c(forward ? Opcode.INVOKE_STATIC : Opcode.INVOKE_VIRTUAL,
                            call.getRegisterCount(), call.getRegisterC(), call.getRegisterD(), call.getRegisterE(),
                            call.getRegisterF(), call.getRegisterG(), replacement);
                    replaced++;
                }
                instructions.add(result);
            }
            methods.add(new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(),
                    method.getReturnType(), method.getAccessFlags(), method.getAnnotations(), method.getHiddenApiRestrictions(),
                    new ImmutableMethodImplementation(body.getRegisterCount(), instructions, body.getTryBlocks(), body.getDebugItems())));
        }
        req(replaced == 2, "Expected exactly two Home document calls; found " + replaced);
        return new ImmutableClassDef(c.getType(), c.getAccessFlags(), c.getSuperclass(), c.getInterfaces(),
                c.getSourceFile(), c.getAnnotations(), c.getFields(), methods);
    }
    public static void main(String[] args) throws Exception {
        req(args.length == 4, "BASELINE_MPP_OR_DEX FRESH_PATCH_DEX OUTPUT_PATCH_DEX REPORT_JSON");
        var original = load(args[0]);
        var fresh = load(args[1]);
        req(fresh.containsKey(HELPER), "New logical resource helper missing");
        ClassDef next = transform(original.get(HOME), true);
        req(Arrays.equals(canonical(original.get(HOME)), canonical(transform(next, false))),
                "Removing document redirects did not restore the exact original Home closure");
        // D8 may already have translated the amended JVM closure. Use the
        // original DEX with only the two verified invokes changed instead.
        fresh.put(HOME, next);
        DexPool.writeTo(args[2], new ImmutableDexFile(OPS, fresh.values()));
        var written = load(args[2]);
        req(written.keySet().equals(fresh.keySet()), "DEX inventory changed during serialization");
        for (String type : fresh.keySet()) req(Arrays.equals(canonical(fresh.get(type)), canonical(written.get(type))),
                "DEX class changed during serialization: " + type);
        String report = "{\n  \"result\":\"PASS_HOME_LOGICAL_RESOURCE_REDIRECT\",\n"
                + "  \"original_dex_document_calls_redirected\":2,\n"
                + "  \"registers_branch_offsets_checks_and_attributes_unchanged\":true,\n"
                + "  \"restoring_calls_recovers_exact_original_class\":true,\n"
                + "  \"other_fresh_patch_classes_unchanged\":true\n}\n";
        Files.writeString(Path.of(args[3]), report);
        System.out.print(report);
    }
}
