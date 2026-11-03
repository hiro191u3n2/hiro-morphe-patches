import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction;

/**
 * Verifies the final DEX's one-instruction English-priority change and evaluates
 * its list partition using the actual const/move instruction. This is a bounded
 * DEX data-flow check; it does not execute the Android prediction pipeline.
 */
public final class EnglishPriorityDexTest {
    static int checks;
    static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    static Method method(String path) throws Exception {
        for (ClassDef cls : DexFileFactory.loadDexFile(new File(path), Opcodes.forApi(26)).getClasses())
            if (cls.getType().equals("Lhiro/swiftkey/KatakanaEnglishCandidates;"))
                for (Method method : cls.getMethods()) if (method.getName().equals("enhance")) return method;
        throw new AssertionError("English enrichment method missing");
    }
    static List<Instruction> instructions(Method method) {
        List<Instruction> result = new ArrayList<>();
        for (Instruction instruction : method.getImplementation().getInstructions()) result.add(instruction);
        return result;
    }
    static int preservedPrefix(Instruction instruction, int nativeCount) {
        int[] registers = new int[8];
        registers[3] = nativeCount; // proven List.size -> move-result v3 at indices 67/68
        if (instruction.getOpcode() == Opcode.CONST_4) {
            registers[((OneRegisterInstruction) instruction).getRegisterA()] =
                    ((NarrowLiteralInstruction) instruction).getNarrowLiteral();
        } else if (instruction.getOpcode() == Opcode.MOVE) {
            TwoRegisterInstruction move = (TwoRegisterInstruction) instruction;
            registers[move.getRegisterA()] = registers[move.getRegisterB()];
        } else throw new AssertionError("Unexpected priority instruction");
        return Math.min(registers[4], registers[3]); // proven Math.min(v4,v3) at index70
    }
    static List<Object> partition(List<Object> original, List<Object> supplement, int prefix) {
        List<Object> output = new ArrayList<>();
        output.addAll(original.subList(0, prefix));
        output.addAll(supplement);
        output.addAll(original.subList(prefix, original.size()));
        return output;
    }
    static byte[] canonical(Method method) throws Exception {
        DexPool pool = new DexPool(Opcodes.forApi(26));
        pool.internClass(new ImmutableClassDef("Lhiro/swiftkey/KatakanaEnglishCandidates;", 17,
                "Ljava/lang/Object;", List.of(), null, java.util.Set.of(), List.of(), List.of(method)));
        MemoryDataStore data = new MemoryDataStore();
        try { pool.writeTo(data); return Arrays.copyOf(data.getBuffer(), data.getSize()); }
        finally { data.close(); }
    }
    public static void main(String[] args) throws Exception {
        Method before = method(args[0]), after = method(args[1]);
        String pin = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(args[0]))));
        check(pin.equals("0a5822f3cd2b78ef5550b48ba7fffa488e11cdd925d007ecf93d3216e9f06477"), "published baseline runtime pin");
        List<Instruction> oldInstructions = instructions(before), newInstructions = instructions(after);
        check(oldInstructions.size() == 102 && newInstructions.size() == 102, "instruction count retained");
        List<Instruction> restored = new ArrayList<>(newInstructions);
        restored.set(69, oldInstructions.get(69));
        ImmutableMethodImplementation implementation = new ImmutableMethodImplementation(
                after.getImplementation().getRegisterCount(), restored,
                after.getImplementation().getTryBlocks(), after.getImplementation().getDebugItems());
        Method repaired = new ImmutableMethod(after.getDefiningClass(), after.getName(), after.getParameters(),
                after.getReturnType(), after.getAccessFlags(), after.getAnnotations(), after.getHiddenApiRestrictions(), implementation);
        check(Arrays.equals(canonical(before), canonical(repaired)), "all other instructions, metadata, handlers and branches unchanged");
        check(DexFormatter.INSTANCE.getReference(((ReferenceInstruction) oldInstructions.get(67)).getReference()).equals("Ljava/util/List;->size()I"), "prefix derives from full original list size");
        check(oldInstructions.get(68).getOpcode() == Opcode.MOVE_RESULT && ((OneRegisterInstruction) oldInstructions.get(68)).getRegisterA() == 3, "size held in v3");
        check(DexFormatter.INSTANCE.getReference(((ReferenceInstruction) oldInstructions.get(70)).getReference()).equals("Ljava/lang/Math;->min(II)I"), "preserve min call unchanged");
        Instruction oldPriority = oldInstructions.get(69), newPriority = newInstructions.get(69);
        check(oldPriority.getOpcode() == Opcode.CONST_4 && newPriority.getOpcode() == Opcode.MOVE,
                "only priority constant replaced with size move");
        check(((OneRegisterInstruction) oldPriority).getRegisterA() == 4 &&
                ((NarrowLiteralInstruction) oldPriority).getNarrowLiteral() == 2, "old cutoff is two");
        check(((TwoRegisterInstruction) newPriority).getRegisterA() == 4 &&
                ((TwoRegisterInstruction) newPriority).getRegisterB() == 3, "new cutoff uses native size");
        check(oldPriority.getCodeUnits() == newPriority.getCodeUnits(), "all branch addresses remain valid");
        for (int nativeCount = 1; nativeCount <= 64; nativeCount++) {
            for (int addedCount : new int[] {0, 1, 12, 80, 128}) {
                List<Object> original = new ArrayList<>(), supplement = new ArrayList<>();
                for (int i = 0; i < nativeCount; i++) original.add(new Object());
                for (int i = 0; i < addedCount; i++) supplement.add(new Object());
                int oldPrefix = preservedPrefix(oldPriority, nativeCount);
                int newPrefix = preservedPrefix(newPriority, nativeCount);
                check(newPrefix == nativeCount, "all native candidates precede supplemental English");
                List<Object> actual = partition(original, supplement, newPrefix);
                check(actual.size() == nativeCount + addedCount, "all candidates retained");
                for (int i = 0; i < nativeCount; i++) check(actual.get(i) == original.get(i), "native order/identity retained");
                for (int i = 0; i < addedCount; i++) check(actual.get(nativeCount + i) == supplement.get(i), "English relative order retained");
                if (nativeCount > 2 && addedCount > 0)
                    check(partition(original, supplement, oldPrefix).indexOf(original.get(2)) == addedCount + 2,
                            "published English dictionary displacement reproduced");
            }
        }
        System.out.println("{\"status\":\"PASSED\",\"final_dex_one_instruction_change_verified\":true," +
                "\"bounded_partition_dataflow_passed\":true,\"old_english_displacement_reproduced\":true," +
                "\"native_android_pipeline_executed\":false,\"device_tested\":false,\"checks\":" + checks + "}");
    }
}
