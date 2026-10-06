import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;

/** Verifies hooks in an APK produced by applying the final distribution MPP. */
public final class VerifyAppliedMyPlanBundle {
    static final String URL = "Lctrip/android/reactnative/CRNURL;";
    static final String INSTANCE = "Lcom/facebook/react/runtime/ReactInstance;";
    static final String MODEL = "Lcom/ctrip/ibu/framework/baseview/widget/cmtv2/crn/IBUCRNCMTV2Model;";
    static final String GUARD = "Lapp/hiro/tripcom/extension/MyPlanBundleGuard;";
    static final String OLD_GUARD = "Lapp/hiro/tripcom/extension/MyPlanPreparationGuard;";
    static final String STRING = "Ljava/lang/String;";
    static void req(boolean b, String text) { if (!b) throw new IllegalStateException(text); }
    static Map<String, ClassDef> load(String path) throws Exception {
        Map<String, ClassDef> out = new TreeMap<>();
        var zip = DexFileFactory.loadDexContainer(Path.of(path).toFile(), Opcodes.forApi(26));
        for (String entry : zip.getDexEntryNames()) for (ClassDef c : zip.getEntry(entry).getDexFile().getClasses()) {
            req(out.put(c.getType(), c) == null, "Duplicate class " + c.getType());
        }
        return out;
    }
    static String hash(ClassDef c) throws Exception {
        DexPool pool = new DexPool(Opcodes.forApi(26)); pool.internClass(ImmutableClassDef.of(c));
        MemoryDataStore data = new MemoryDataStore();
        try {
            pool.writeTo(data);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data.getData()));
        } finally { data.close(); }
    }
    static Method method(Method m, MethodImplementation b) {
        return new ImmutableMethod(m.getDefiningClass(), m.getName(), m.getParameters(), m.getReturnType(),
                m.getAccessFlags(), m.getAnnotations(), m.getHiddenApiRestrictions(), b);
    }
    static ClassDef rebuilt(ClassDef c, List<Method> methods) {
        return new ImmutableClassDef(c.getType(), c.getAccessFlags(), c.getSuperclass(), c.getInterfaces(),
                c.getSourceFile(), c.getAnnotations(), c.getFields(), methods);
    }
    static ClassDef normalized(ClassDef c) {
        List<Method> methods = new ArrayList<>();
        for (Method m : c.getMethods()) {
            if (m.getImplementation() == null) { methods.add(m); continue; }
            MutableMethodImplementation b = new MutableMethodImplementation(m.getImplementation());
            for (int index = 0; index < b.getInstructions().size(); index++) {
                Instruction v = b.getInstructions().get(index);
                if (v.getOpcode() == Opcode.CONST_STRING && v instanceof ReferenceInstruction r) {
                    b.replaceInstruction(index, new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,
                            ((OneRegisterInstruction) v).getRegisterA(), r.getReference()));
                } else if ((v.getOpcode() == Opcode.GOTO || v.getOpcode() == Opcode.GOTO_16)
                        && v instanceof BuilderOffsetInstruction r) {
                    b.replaceInstruction(index, new BuilderInstruction30t(Opcode.GOTO_32, r.getTarget()));
                }
            }
            methods.add(method(m, b));
        }
        return rebuilt(c, methods);
    }
    static String ref(Instruction i) {
        return i instanceof ReferenceInstruction r ? DexFormatter.INSTANCE.getReference(r.getReference()) : "";
    }
    static void invoke(Instruction i, String reference, int... registers) {
        req(i.getOpcode() == Opcode.INVOKE_STATIC && ref(i).equals(reference), "Hook reference differs: " + reference);
        req(i instanceof FiveRegisterInstruction, "Expected fixed-register invoke");
        FiveRegisterInstruction f = (FiveRegisterInstruction) i;
        req(f.getRegisterCount() == registers.length, "Hook parameter count differs");
        int[] actual = {f.getRegisterC(), f.getRegisterD(), f.getRegisterE(), f.getRegisterF(), f.getRegisterG()};
        for (int index = 0; index < registers.length; index++) req(actual[index] == registers[index], "Hook register differs");
    }
    static void one(Instruction i, Opcode op, int register) {
        req(i.getOpcode() == op && ((OneRegisterInstruction) i).getRegisterA() == register, "Instruction/register differs: " + op);
    }
    static void branchToIndex(List<? extends Instruction> instructions, int branchIndex, int targetIndex) {
        int branch = 0, target = 0;
        for (int index = 0; index < branchIndex; index++) branch += instructions.get(index).getCodeUnits();
        for (int index = 0; index < targetIndex; index++) target += instructions.get(index).getCodeUnits();
        req(branch + ((OffsetInstruction) instructions.get(branchIndex)).getCodeOffset() == target, "Original continuation target differs");
    }
    static Method restorePath(Method m) {
        MutableMethodImplementation b = new MutableMethodImplementation(m.getImplementation());
        var i = b.getInstructions(); req(b.getRegisterCount() == 11 && i.size() > 5, "Path register count differs");
        invoke(i.get(0), GUARD + "->businessPath(" + STRING + ")" + STRING, 10);
        one(i.get(1), Opcode.MOVE_RESULT_OBJECT, 0); one(i.get(2), Opcode.IF_EQZ, 0);
        one(i.get(3), Opcode.RETURN_OBJECT, 0); req(i.get(4).getOpcode() == Opcode.NOP, "Path continuation NOP missing");
        branchToIndex(i, 2, 4);
        for (int n = 0; n < 5; n++) b.removeInstruction(0);
        return method(m, b);
    }
    static Method restoreLoad(Method m) {
        MutableMethodImplementation b = new MutableMethodImplementation(m.getImplementation());
        var i = b.getInstructions(); req(b.getRegisterCount() == 8, "Native load register count differs");
        invoke(i.get(0), GUARD + "->loadPath(" + STRING + STRING + ")" + STRING, 4, 5);
        one(i.get(1), Opcode.MOVE_RESULT_OBJECT, 4);
        b.removeInstruction(0); b.removeInstruction(0);
        int nativeIndex = -1;
        for (int n = 0; n < i.size(); n++) {
            if (ref(i.get(n)).equals(INSTANCE + "->loadBusinessScriptNative(" + STRING + STRING + STRING + "Z)I")) {
                req(nativeIndex == -1, "Native call duplicated"); nativeIndex = n;
                req(i.get(n).getOpcode() == Opcode.INVOKE_VIRTUAL, "Native opcode changed");
                FiveRegisterInstruction f = (FiveRegisterInstruction) i.get(n);
                req(f.getRegisterCount() == 5 && f.getRegisterC() == 3 && f.getRegisterD() == 4
                        && f.getRegisterE() == 5 && f.getRegisterF() == 6 && f.getRegisterG() == 7,
                        "Native product/package/Hermes parameters changed");
            }
        }
        req(nativeIndex >= 2, "Native call missing");
        invoke(i.get(nativeIndex - 2), GUARD + "->isPinnedPath(" + STRING + STRING + ")Z", 4, 5);
        one(i.get(nativeIndex - 1), Opcode.MOVE_RESULT, 2);
        one(i.get(nativeIndex + 1), Opcode.MOVE_RESULT, 4);
        invoke(i.get(nativeIndex + 2), GUARD + "->resourceKeySucceeded(IZ)Z", 4, 2);
        one(i.get(nativeIndex + 3), Opcode.MOVE_RESULT, 2);
        one(i.get(nativeIndex + 4), Opcode.IF_EQZ, 2);
        BuilderInstruction21t replacement = (BuilderInstruction21t) i.get(nativeIndex + 4);
        b.replaceInstruction(nativeIndex + 4, new BuilderInstruction21t(Opcode.IF_NEZ, 4, replacement.getTarget()));
        b.removeInstruction(nativeIndex + 2); b.removeInstruction(nativeIndex + 2);
        b.removeInstruction(nativeIndex - 2); b.removeInstruction(nativeIndex - 2);
        return method(m, b);
    }
    static Method restoreOldGate(Method m) {
        MutableMethodImplementation b = new MutableMethodImplementation(m.getImplementation());
        var i = b.getInstructions(); req(i.size() > 7, "Existing native gate missing");
        boolean v2 = m.getName().equals("getCMTDataV2");
        int start = b.getRegisterCount() - m.getParameterTypes().size() - 1;
        String call = v2 ? "suppressRequest(Lcom/facebook/react/bridge/ReadableMap;Lcom/facebook/react/bridge/Callback;)Z"
                : "suppressPage(Ljava/lang/String;Lcom/facebook/react/bridge/Callback;)Z";
        invoke(i.get(0), OLD_GUARD + "->" + call, start + 1, start + (v2 ? 3 : 4));
        one(i.get(1), Opcode.MOVE_RESULT, 0); one(i.get(2), Opcode.IF_EQZ, 0);
        one(i.get(3), Opcode.NEW_INSTANCE, 0);
        req(ref(i.get(3)).equals("Lcom/facebook/react/bridge/WritableNativeMap;"), "Existing empty-map type differs");
        req(i.get(4).getOpcode() == Opcode.INVOKE_DIRECT
                && ref(i.get(4)).equals("Lcom/facebook/react/bridge/WritableNativeMap;-><init>()V"), "Existing empty-map constructor differs");
        one(i.get(5), Opcode.RETURN_OBJECT, 0); req(i.get(6).getOpcode() == Opcode.NOP, "Existing gate continuation differs");
        branchToIndex(i, 2, 6);
        for (int n = 0; n < 7; n++) b.removeInstruction(0);
        return method(m, b);
    }
    static ClassDef restored(ClassDef c) {
        List<Method> methods = new ArrayList<>(); int changes = 0;
        for (Method m : c.getMethods()) {
            if (c.getType().equals(URL) && m.getName().equals("correctRNBusinessPath")) {
                methods.add(restorePath(m)); changes++;
            } else if (c.getType().equals(INSTANCE) && m.getName().equals("loadBusinessScript")) {
                methods.add(restoreLoad(m)); changes++;
            } else if (c.getType().equals(MODEL) && (m.getName().equals("getCMTData") || m.getName().equals("getCMTDataV2"))) {
                methods.add(restoreOldGate(m)); changes++;
            } else methods.add(m);
        }
        req(changes == (c.getType().equals(MODEL) ? 2 : 1), "Unexpected hooked-method count");
        return rebuilt(c, methods);
    }
    public static void main(String[] args) throws Exception {
        req(args.length == 4, "ORIGINAL_APK PATCHED_APK MYPLAN_BUNDLE_GUARD_MPE OUTPUT_JSON");
        var old = load(args[0]); var next = load(args[1]); var extension = load(args[2]);
        req(extension.size() == 1 && extension.containsKey(GUARD), "New extension must contain only its reviewed guard class");
        req(!old.containsKey(GUARD) && !old.containsKey(OLD_GUARD), "Input APK is already Hiro-patched");
        var expected = new TreeSet<>(old.keySet()); expected.add(GUARD); expected.add(OLD_GUARD);
        req(next.keySet().equals(expected), "Patched APK class inventory differs beyond the two guard extensions");
        req(hash(normalized(extension.get(GUARD))).equals(hash(normalized(next.get(GUARD)))), "Runtime extension differs in final APK");
        StringBuilder restoredHashes = new StringBuilder();
        for (String type : List.of(URL, INSTANCE, MODEL)) {
            String originalHash = hash(normalized(old.get(type)));
            req(originalHash.equals(hash(normalized(restored(next.get(type))))), "Removing hooks did not restore exact original class: " + type);
            if (restoredHashes.length() > 0) restoredHashes.append(",\n");
            restoredHashes.append("    \"").append(type).append("\":\"").append(originalHash).append("\"");
        }
        String receipt = "{\n  \"result\":\"PASS_ACTUAL_MPP_APPLY\",\n"
                + "  \"new_guard_matches_reviewed_mpe\":true,\n"
                + "  \"class_inventory_is_original_plus_two_guards\":true,\n"
                + "  \"original_app_class_count\":" + old.size() + ",\n"
                + "  \"path_prefix_and_original_fallback_branch_verified\":true,\n"
                + "  \"native_load_preserves_product_package_id_and_Hermes_flag\":true,\n"
                + "  \"pinned_identity_captured_before_path_register_reused\":true,\n"
                + "  \"resource_key_initialized_for_pinned_result_one_only\":true,\n"
                + "  \"native_result_one_return_preserved_for_MIXED_selection\":true,\n"
                + "  \"both_existing_preparation_API_gates_retained\":true,\n"
                + "  \"all_original_code_in_three_hooked_classes_restored_exactly\":true,\n"
                + "  \"restored_normalized_class_sha256\":{\n" + restoredHashes + "\n  },\n"
                + "  \"android_device_tested\":false,\n  \"blocking_findings\":[]\n}\n";
        Files.writeString(Path.of(args[3]), receipt); System.out.print(receipt);
    }
}
