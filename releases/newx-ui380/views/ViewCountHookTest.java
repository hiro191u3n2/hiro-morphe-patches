import java.io.*;
import java.util.*;
import app.hiro.newxui.patches.ViewCountHook;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;

/** Host audit of actual Patcher 2.4 transformations and the emitted bridge bytecode. */
public class ViewCountHookTest {
    static void require(boolean value, String why) { if (!value) throw new AssertionError(why); }
    static int mapped(int index) { return index < 448 ? index : index >= 456 ? index - 6 : -1; }
    static int[] addresses(List<Instruction> code) {
        int[] pc = new int[code.size() + 1];
        for (int i = 0; i < code.size(); i++) pc[i + 1] = pc[i] + code.get(i).getCodeUnits();
        return pc;
    }
    static int destination(List<Instruction> code, int at) {
        int[] pc = addresses(code);
        int wanted = pc[at] + ((OffsetInstruction)code.get(at)).getCodeOffset();
        for (int i = 0; i < pc.length; i++) if (pc[i] == wanted) return i;
        throw new AssertionError("branch into middle of instruction");
    }
    static ClassDef load(String path) throws Exception {
        ClassDef result = null;
        MultiDexContainer<? extends DexFile> d = DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String n : d.getDexEntryNames()) for (ClassDef c : d.getEntry(n).getDexFile().getClasses()) if (ViewCountHook.TYPE.equals(c.getType())) {
            require(result == null, "unique target"); result = c;
        }
        require(result != null, "target exists"); return result;
    }
    static void preserved(Method beforeMethod, Method afterMethod) {
        List<Instruction> before = ViewCountHook.code(beforeMethod), after = ViewCountHook.code(afterMethod);
        require(after.size() == before.size() - 6, "only 8-instruction count expression replaced by 2");
        require(afterMethod.getImplementation().getRegisterCount() == 61, "native register allocation unchanged");
        for (int i = 0; i < before.size(); i++) if (i < 448 || i >= 456) {
            Instruction b = before.get(i), a = after.get(mapped(i));
            if (b instanceof OffsetInstruction) {
                require(b.getOpcode() == a.getOpcode(), "original branch opcode preserved at " + i);
                require(destination(after, mapped(i)) == mapped(destination(before, i)), "original control-flow target preserved at " + i);
                if (b instanceof OneRegisterInstruction) require(((OneRegisterInstruction)b).getRegisterA() == ((OneRegisterInstruction)a).getRegisterA(), "branch operand A");
                if (b instanceof TwoRegisterInstruction) require(((TwoRegisterInstruction)b).getRegisterB() == ((TwoRegisterInstruction)a).getRegisterB(), "branch operand B");
            } else require(ViewCountHook.fingerprint(Collections.singletonList(b)).equals(ViewCountHook.fingerprint(Collections.singletonList(a))), "all other native code preserved at " + i);
        }
        Instruction invoke = after.get(448), result = after.get(449);
        require(invoke.getOpcode() == Opcode.INVOKE_STATIC && ViewCountHook.ref(invoke).equals(ViewCountHook.TYPE + "->" + ViewCountHook.BRIDGE + "(" + ViewCountHook.MODEL + ViewCountHook.RESOURCES + ")Ljava/lang/String;"), "bridge signature");
        FiveRegisterInstruction call = (FiveRegisterInstruction)invoke;
        require(call.getRegisterCount() == 2 && call.getRegisterC() == 8 && call.getRegisterD() == 3, "raw model and resources passed before count extraction");
        require(result.getOpcode() == Opcode.MOVE_RESULT_OBJECT && ((OneRegisterInstruction)result).getRegisterA() == 7, "native label destination");
    }
    // Interpret the emitted bridge, so its actual branch/register wiring is tested.
    static final class Entry { final String action; final Long count; Entry(String a, Long c) { action = a; count = c; } }
    static String run(String action, Long count, boolean enabled) {
        List<Instruction> code = ViewCountHook.code(ViewCountHook.bridge());
        Object[] r = new Object[6]; r[4] = new Entry(action, count); r[5] = new Object(); Object result = null;
        int calls = 0, formats = 0;
        for (int pc = 0; pc < code.size(); pc++) {
            Instruction i = code.get(pc);
            switch (i.getOpcode()) {
                case IGET_OBJECT: {
                    TwoRegisterInstruction x = (TwoRegisterInstruction)i; Entry e = (Entry)r[x.getRegisterB()];
                    r[x.getRegisterA()] = ViewCountHook.ref(i).contains("->b:") ? e.count : e.action; break;
                }
                case SGET_OBJECT: r[((OneRegisterInstruction)i).getRegisterA()] = "ViewCount"; break;
                case IF_NEZ: if (r[((OneRegisterInstruction)i).getRegisterA()] != null) pc = destination(code, pc) - 1; break;
                case CONST_4: r[((OneRegisterInstruction)i).getRegisterA()] = null; break;
                case IF_NE: { TwoRegisterInstruction x = (TwoRegisterInstruction)i; if (!Objects.equals(r[x.getRegisterA()], r[x.getRegisterB()])) pc = destination(code, pc) - 1; break; }
                case INVOKE_VIRTUAL: {
                    FiveRegisterInstruction x = (FiveRegisterInstruction)i;
                    require(ViewCountHook.ref(i).equals("Ljava/lang/Number;->longValue()J"), "only long extraction");
                    result = ((Number)r[x.getRegisterC()]).longValue(); break;
                }
                case INVOKE_STATIC: {
                    FiveRegisterInstruction x = (FiveRegisterInstruction)i;
                    if (ViewCountHook.ref(i).contains("->x0(")) { calls++; require(r[x.getRegisterE()] == r[5], "native resources preserved"); result = "compact:" + r[x.getRegisterC()]; }
                    else { formats++; require(ViewCountHook.ref(i).equals(ViewCountHook.RUNTIME + "->format(Ljava/lang/Long;Ljava/lang/String;)Ljava/lang/String;"), "runtime signature"); Long raw = (Long)r[x.getRegisterC()]; result = enabled && raw >= 0 ? "full:" + raw : r[x.getRegisterD()]; }
                    break;
                }
                case MOVE_RESULT_WIDE: { int a = ((OneRegisterInstruction)i).getRegisterA(); r[a] = result; r[a + 1] = result; break; }
                case MOVE_RESULT_OBJECT: r[((OneRegisterInstruction)i).getRegisterA()] = result; break;
                case RETURN_OBJECT:
                    require(calls == (count == null ? 0 : 1), "native formatting called exactly once for nonnull");
                    require(formats == (count != null && action.equals("ViewCount") ? 1 : 0), "runtime called only for nonnull ViewCount");
                    return (String)r[((OneRegisterInstruction)i).getRegisterA()];
                default: throw new AssertionError("unexpected bridge opcode " + i.getOpcode());
            }
        }
        throw new AssertionError("bridge does not return");
    }
    public static void main(String[] args) throws Exception {
        for (String path : args) {
            ClassDef before = load(path); ViewCountHook.verify(before);
            MutableClass after = new MutableClass(before); ViewCountHook.apply(after);
            preserved(ViewCountHook.target(before), ViewCountHook.target(after));
            int oldMethods = 0, newMethods = 0;
            for (Method m : before.getMethods()) {
                oldMethods++; Method n = null;
                for (Method candidate : after.getMethods()) if (candidate.toString().equals(m.toString())) n = candidate;
                require(n != null, "every original method retained");
                if (!m.toString().equals(ViewCountHook.target(before).toString()) && m.getImplementation() != null)
                    require(ViewCountHook.fingerprint(ViewCountHook.code(m)).equals(ViewCountHook.fingerprint(ViewCountHook.code(n))), "unrelated methods unchanged");
            }
            for (Method m : after.getMethods()) { newMethods++; if (m.getName().equals(ViewCountHook.BRIDGE)) require(m.getAccessFlags() == 0x100a && m.getImplementation().getRegisterCount() == 6, "private static synthetic bridge with six registers"); }
            require(newMethods == oldMethods + 1, "exactly one method added");
            boolean rejected = false; try { ViewCountHook.apply(after); } catch (IllegalStateException expected) { rejected = true; }
            require(rejected, "duplicate patch rejected");
            MemoryDataStore bytes = new MemoryDataStore(); DexPool.writeTo(bytes, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(after)));
            DexBackedDexFile roundTrip = DexBackedDexFile.fromInputStream(Opcodes.getDefault(), new BufferedInputStream(new ByteArrayInputStream(Arrays.copyOf(bytes.getBuffer(), bytes.getSize())))); bytes.close();
            ClassDef serialized = roundTrip.getClasses().iterator().next(); preserved(ViewCountHook.target(before), ViewCountHook.target(serialized));
            System.out.println("PASS native preservation + branch relocation + bridge DEX roundtrip: " + path);
        }
        for (String action : Arrays.asList("ViewCount", "Like", "Reply", "Repost", "Bookmark", "Share", "Dislike"))
            for (Long count : Arrays.asList(null, -1L, 0L, 999L, 1000L, 3456L, 2147483648L, 9007199254740993L, Long.MAX_VALUE))
                for (boolean enabled : new boolean[]{false, true}) {
                    String expected = count == null ? null : enabled && action.equals("ViewCount") && count >= 0 ? "full:" + count : "compact:" + count;
                    require(Objects.equals(expected, run(action, count, enabled)), "emitted bridge semantics " + action + "/" + count + "/" + enabled);
                }
        System.out.println("PASS bridge execution: ViewCount-only gate, null/zero/negative/full64bit counts, OFF, all unrelated action types (126 cases)");
    }
}
