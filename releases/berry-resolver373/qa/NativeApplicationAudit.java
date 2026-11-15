import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.nio.file.*;
import java.util.*;

/** Audit emitted application DEX. Compile with the archived339
 * VerifyPatchedHooks.java to reuse its encoding/reference/diagnostic checks;
 * this audit does not invoke its obsolete baseline338 comparison. */
public final class NativeApplicationAudit {
    private static final String WEB = "Landroid/webkit/WebView;", STR = "Ljava/lang/String;";
    private static final DexFormatter F = DexFormatter.INSTANCE;
    private static int checks;

    public static void main(String[] args) throws Exception {
        need(args.length == 2 && Arrays.asList("platform", "interface").contains(args[1]), "patched.apk platform|interface");
        Map<String,ClassDef> all = VerifyPatchedHooks.load(Path.of(args[0]));
        boolean platform = args[1].equals("platform");
        Method complete = VerifyPatchedHooks.uniqueMethod(all, null, "hiroCompleteBfcache");
        Method finished = VerifyPatchedHooks.uniqueMethod(all, complete.getDefiningClass(), "onPageFinished");
        Method started = VerifyPatchedHooks.uniqueMethod(all, complete.getDefiningClass(), "onPageStarted");
        List<Instruction> code = ins(complete);
        need(complete.getReturnType().equals("Z") && complete.getParameterTypes().equals(Arrays.asList(WEB, STR))
            && complete.getImplementation().getRegisterCount() == 7 && (complete.getAccessFlags() & 8) == 0,
            "completion ABI must have four locals and this/WebView/URL");
        need(code.size() > 20, "completion body missing");
        expect(code.get(0), Opcode.IGET_OBJECT, 0, 4);
        expect(code.get(1), Opcode.IGET_OBJECT, 1, 0);
        expect(code.get(2), Opcode.INSTANCE_OF, 0, 1);
        need(code.get(3).getOpcode() == Opcode.IF_EQZ && reg1(code.get(3), 0), "adapter type guard");
        need(code.get(4).getOpcode() == Opcode.CHECK_CAST && reg1(code.get(4), 1), "adapter cast");
        expect(code.get(5), Opcode.IGET_OBJECT, 2, 1);
        expect(code.get(6), Opcode.IF_NE, 2, 5);
        expect(code.get(7), Opcode.IGET_OBJECT, 0, 4);
        expect(code.get(8), Opcode.IGET_OBJECT, 3, 0);
        need(ref(code.get(0)).equals(ref(code.get(7))) && ref(code.get(2)).equals(ref(code.get(4))), "stable owner lookup");
        FieldReference clientTab = field(code.get(0)), tabAdapter = field(code.get(1)), adapterWeb = field(code.get(5)), metadata = field(code.get(8));
        need(clientTab.getDefiningClass().equals(complete.getDefiningClass())
            && tabAdapter.getDefiningClass().equals(clientTab.getType())
            && adapterWeb.getDefiningClass().equals(ref(code.get(2)))
            && metadata.getDefiningClass().equals(clientTab.getType()), "typed completion ownership chain");
        need(all.containsKey(adapterWeb.getType()) && WEB.equals(all.get(adapterWeb.getType()).getSuperclass()), "owned value is WebView subclass");
        for (FieldReference f : Arrays.asList(clientTab, tabAdapter, adapterWeb, metadata)) requireField(all, f);
        int failure = VerifyPatchedHooks.target(code, 3);
        need(VerifyPatchedHooks.target(code, 6) == failure && failure == code.size() - 2
            && VerifyPatchedHooks.constant(code.get(failure), 0, 0)
            && code.get(failure + 1).getOpcode() == Opcode.RETURN && reg1(code.get(failure + 1), 0), "ownership failure returns false");
        need(VerifyPatchedHooks.constant(code.get(code.size() - 4), 0, 1)
            && code.get(code.size() - 3).getOpcode() == Opcode.RETURN && reg1(code.get(code.size() - 3), 0), "completion success returns true");

        for (int index = 0; index < 2; index++) {
            String name = index == 0 ? "getUrl" : "getTitle";
            int at = 9 + index * 3;
            Instruction call = code.get(at);
            MethodReference getter = method(call);
            need(getter.getName().equals(name) && getter.getReturnType().equals(STR) && getter.getParameterTypes().isEmpty(), "exact native getter signature");
            need(call.getOpcode() == (platform ? Opcode.INVOKE_VIRTUAL : Opcode.INVOKE_INTERFACE)
                && Arrays.equals(VerifyPatchedHooks.args(call), new int[]{platform ? 5 : 1}), "getter opcode and owned receiver");
            if (platform) need(getter.getDefiningClass().equals(WEB) && tabAdapter.getType().equals(adapterWeb.getDefiningClass()), "direct WebView getter with concrete controller");
            else need(getter.getDefiningClass().equals(tabAdapter.getType())
                && (all.get(getter.getDefiningClass()).getAccessFlags() & 0x200) != 0, "legacy getter remains on interface");
            need(code.get(at + 1).getOpcode() == Opcode.MOVE_RESULT_OBJECT && reg1(code.get(at + 1), 2), "native getter result register");
            expect(code.get(at + 2), Opcode.IPUT_OBJECT, 2, 3);
            FieldReference target = field(code.get(at + 2));
            need(target.getDefiningClass().equals(metadata.getType()) && target.getType().equals(STR), "getter writes expected metadata object");
            requireField(all, target);
            String[] original = originalMetadataPair(finished, name, platform, tabAdapter.getType());
            need(original[0].equals(F.getMethodDescriptor(getter)) && original[1].equals(F.getFieldDescriptor(target)), "generated getter/write matches original page completion");
        }
        need(!ref(code.get(11)).equals(ref(code.get(14))), "URL and title fields remain distinct");
        // No branch can enter the metadata writes after skipping the ownership prefix.
        for (int i = 0; i < code.size(); i++) if (code.get(i) instanceof OffsetInstruction)
            need(VerifyPatchedHooks.target(code, i) > 14, "branch bypasses ownership proof");

        Map<String,Integer> found = new TreeMap<>();
        List<Method> hooked = new ArrayList<>();
        for (ClassDef c : all.values()) if (!VerifyPatchedHooks.helper(c.getType())) for (Method m : c.getMethods()) {
            if (m.getImplementation() == null) continue;
            boolean touched = false;
            for (Instruction i : ins(m)) if (i instanceof ReferenceInstruction
                && ((ReferenceInstruction)i).getReference() instanceof MethodReference) {
                MethodReference mr = (MethodReference)((ReferenceInstruction)i).getReference();
                String key = mr.getDefiningClass() + "->" + mr.getName();
                if (VerifyPatchedHooks.required.containsKey(key)) { found.merge(key, 1, Integer::sum); touched = true; }
            }
            if (touched || m.getName().startsWith("hiro") || m == started) hooked.add(m);
        }
        for (Map.Entry<String,Integer> expected : VerifyPatchedHooks.required.entrySet())
            need(expected.getValue().equals(found.get(expected.getKey())), "actual application hook count: " + expected.getKey());
        int references = 0;
        for (Method m : hooked) {
            VerifyPatchedHooks.checkEncoding(m);
            references += auditReferences(all, m);
            if (m.getName().startsWith("hiro")) {
                int parameters = (m.getAccessFlags() & 8) == 0 ? 1 : 0;
                for (CharSequence t : m.getParameterTypes()) parameters += t.toString().equals("J") || t.toString().equals("D") ? 2 : 1;
                int firstParameter = m.getImplementation().getRegisterCount() - parameters;
                for (Instruction i : ins(m)) if (i.getOpcode().setsRegister() && i instanceof OneRegisterInstruction) {
                    int last = ((OneRegisterInstruction)i).getRegisterA() + (i.getOpcode().setsWideRegister() ? 1 : 0);
                    need(last < firstParameter, "generated helper overwrites parameter registers");
                }
            }
        }
        // Existing checker verifies every native return passes the tail hook,
        // branch targets do not bypass it, and the original WebView/URL registers are used.
        VerifyPatchedHooks.canonical(started, true);
        VerifyPatchedHooks.auditNoDiagnostics(all);
        int runtimeMethods = 0;
        for (ClassDef c : all.values()) if (c.getType().startsWith("Lhiro/berrybfcache/"))
            for (Method m : c.getMethods()) if (m.getImplementation() != null) {
                VerifyPatchedHooks.checkEncoding(m);
                references += auditReferences(all, m); runtimeMethods++;
            }
        System.out.println("{\"result\":\"PASS_NATIVE_APPLICATION_AUDIT\",\"getter_mode\":\"" + args[1]
            + "\",\"checks\":" + checks + ",\"hooked_application_methods\":" + hooked.size()
            + ",\"runtime_methods\":" + runtimeMethods + ",\"method_and_field_references\":" + references
            + ",\"owned_webview_identity_guard\":true,\"native_metadata_getters_and_writes\":true,"
            + "\"hook_counts_registers_branches_and_invoke_words\":true,\"available_app_invoke_opcodes\":true,"
            + "\"tail_hooks\":true,\"diagnostics_absent\":true,\"device_execution_tested\":false}");
    }

    private static int auditReferences(Map<String,ClassDef> all, Method owner) {
        int references = 0;
        for (Instruction i : ins(owner)) {
            if (i.getOpcode().setsWideRegister() && i instanceof OneRegisterInstruction)
                need(((OneRegisterInstruction)i).getRegisterA() + 1 < owner.getImplementation().getRegisterCount(), "wide register bounds");
            if (!(i instanceof ReferenceInstruction)) continue;
            Reference r = ((ReferenceInstruction)i).getReference();
            if (r instanceof MethodReference) {
                MethodReference m = (MethodReference)r;
                need(VerifyPatchedHooks.methodExists(all, m, m.getDefiningClass(), new HashSet<>()), "unresolved hook method " + m);
                ClassDef type = all.get(m.getDefiningClass());
                if (type != null && i.getOpcode().name.startsWith("invoke-")) {
                    if (i.getOpcode() == Opcode.INVOKE_INTERFACE || i.getOpcode() == Opcode.INVOKE_INTERFACE_RANGE)
                        need((type.getAccessFlags() & 0x200) != 0, "invoke-interface on concrete owner");
                    if (i.getOpcode() == Opcode.INVOKE_VIRTUAL || i.getOpcode() == Opcode.INVOKE_VIRTUAL_RANGE)
                        need((type.getAccessFlags() & 0x200) == 0, "invoke-virtual on interface owner");
                    for (Method definition : type.getMethods()) if (F.getMethodDescriptor(definition).equals(F.getMethodDescriptor(m))) {
                        boolean stat = i.getOpcode() == Opcode.INVOKE_STATIC || i.getOpcode() == Opcode.INVOKE_STATIC_RANGE;
                        need(stat == ((definition.getAccessFlags() & 8) != 0), "invoke static/instance mismatch");
                    }
                }
                references++;
            } else if (r instanceof FieldReference) {
                FieldReference f = (FieldReference)r;
                need(VerifyPatchedHooks.fieldExists(all, f, f.getDefiningClass(), new HashSet<>()), "unresolved hook field " + f);
                ClassDef type = all.get(f.getDefiningClass());
                if (type != null) for (Field definition : type.getFields()) if (F.getFieldDescriptor(definition).equals(F.getFieldDescriptor(f))) {
                    boolean stat = i.getOpcode().name.startsWith("sget") || i.getOpcode().name.startsWith("sput");
                    need(stat == ((definition.getAccessFlags() & 8) != 0), "field static/instance mismatch");
                }
                references++;
            }
        }
        return references;
    }
    private static String[] originalMetadataPair(Method finished, String name, boolean platform, String adapterInterface) {
        String[] result = null; List<Instruction> code = ins(finished);
        for (int i = 0; i + 2 < code.size(); i++) {
            if (!(code.get(i) instanceof ReferenceInstruction) || !(((ReferenceInstruction)code.get(i)).getReference() instanceof MethodReference)) continue;
            MethodReference getter = (MethodReference)((ReferenceInstruction)code.get(i)).getReference();
            if (!getter.getName().equals(name) || !getter.getDefiningClass().equals(platform ? WEB : adapterInterface)
                || !getter.getParameterTypes().isEmpty() || !getter.getReturnType().equals(STR)) continue;
            need(code.get(i).getOpcode() == (platform ? Opcode.INVOKE_VIRTUAL : Opcode.INVOKE_INTERFACE)
                || code.get(i).getOpcode() == (platform ? Opcode.INVOKE_VIRTUAL_RANGE : Opcode.INVOKE_INTERFACE_RANGE), "original metadata getter opcode");
            need(code.get(i + 1).getOpcode() == Opcode.MOVE_RESULT_OBJECT, "original metadata move result");
            int value = ((OneRegisterInstruction)code.get(i + 1)).getRegisterA(), at = i + 2;
            if (code.get(at).getOpcode() == Opcode.IGET_OBJECT) at++;
            need(code.get(at).getOpcode() == Opcode.IPUT_OBJECT && reg1(code.get(at), value), "original metadata write");
            need(result == null, "ambiguous original metadata getter");
            result = new String[]{F.getMethodDescriptor(getter), ref(code.get(at))};
        }
        need(result != null, "original metadata getter missing"); return result;
    }
    private static void requireField(Map<String,ClassDef> all, FieldReference reference) {
        ClassDef type = all.get(reference.getDefiningClass()); need(type != null, "field owner missing");
        int count = 0;
        for (Field f : type.getFields()) if (F.getFieldDescriptor(f).equals(F.getFieldDescriptor(reference))) {
            need((f.getAccessFlags() & 9) == 1, "owned field must be public instance"); count++;
        }
        need(count == 1, "owned field must resolve once");
    }
    private static void expect(Instruction i, Opcode opcode, int a, int b) {
        need(i.getOpcode() == opcode && i instanceof TwoRegisterInstruction
            && ((TwoRegisterInstruction)i).getRegisterA() == a && ((TwoRegisterInstruction)i).getRegisterB() == b, "unexpected native register operands");
    }
    private static List<Instruction> ins(Method m) { return VerifyPatchedHooks.instructions(m); }
    private static String ref(Instruction i) { return VerifyPatchedHooks.ref(i); }
    private static boolean reg1(Instruction i, int a) { return VerifyPatchedHooks.reg1(i, a); }
    private static FieldReference field(Instruction i) { return (FieldReference)((ReferenceInstruction)i).getReference(); }
    private static MethodReference method(Instruction i) { return (MethodReference)((ReferenceInstruction)i).getReference(); }
    private static void need(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); checks++; }
}
