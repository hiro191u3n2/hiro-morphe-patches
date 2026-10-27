package app.hiro.swiftkey.patches;

import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import java.util.List;

/** Exact-target hooks for SwiftKey Beta 9.13.16.4's expanded candidate adapter. */
public final class CandidateScrollPatch {
    private static final String[] OWNERS = {"Lrb0/h;", "Lrb0/h;", "Lrb0/k;", "Lrb0/j;", "Lrb0/k;", "Lrb0/l;"};
    private static final String[] SIGNATURES = {"d(Z)V", "H()V", "onAttachedToWindow()V", "a(Lib0/a;)V", "a()V",
        "b(Landroidx/recyclerview/widget/RecyclerView;II)V"};
    private static final String[] HASHES = {
        "2a0c1d0e75f595abba9ae7a161b70e491a78e283eda65f5f5d5080a2aa1e6ce9",
        "f26a2a574a9e6b5327dba05ddaa8ee3de8653d5b8fe0bb932cdca9686594ebbd",
        "c860c0bcad774b9091a0b0b9481c48480e91857f956873980ad21b2188becb1d",
        "0a207ca0b09de46bf4350c6df623d470c6e5653d9cf2046464804ede2d3e51eb",
        "1e4d9a1a5c8387d5da2113eab74d389310ddd921987df26061a2792cad1e806e",
        "a8610e4a8d0a8bdf1471e8dbd60187fd1ae9cc36806816c694c25e9d11e2755c"
    };
    private static final int[] REGISTERS = {2, 5, 5, 7, 3, 6};
    private static final String NOTIFY = "Lra/q1;->n()V";
    private static final String SNAPSHOT = "Lhiro/swiftkey/CandidateScroll;->snapshot(Lrb0/h;)V";
    private static final String DISPATCH = "Lfb0/u;->h(Z)V";
    private static final String SCROLL = "Landroidx/recyclerview/widget/RecyclerView;->w0(I)V";
    private static final InstructionExtensions EDIT = InstructionExtensions.INSTANCE;
    private static final DexFormatter FORMAT = DexFormatter.INSTANCE;

    private CandidateScrollPatch() {}

    public static void checkOriginal(BytecodePatchContext context) {
        inspect(context.classDefByOrNull(OWNERS[0]), context.classDefByOrNull(OWNERS[2]),
                context.classDefByOrNull(OWNERS[3]), context.classDefByOrNull(OWNERS[5]));
    }

    public static void apply(BytecodePatchContext context) {
        checkOriginal(context);
        MutableMethod[] methods = new MutableMethod[OWNERS.length];
        for (int i = 0; i < methods.length; i++) {
            String descriptor = OWNERS[i] + "->" + SIGNATURES[i];
            for (MutableMethod method : context.mutableClassDefBy(OWNERS[i]).getMethods()) {
                if (!descriptor.equals(FORMAT.getMethodDescriptor(method))) continue;
                require(methods[i] == null, "Duplicate candidate method " + descriptor);
                methods[i] = method;
            }
            require(methods[i] != null, "Missing mutable candidate method " + descriptor);
        }
        applyMethods(methods);
    }

    // Also used by the exact-native-method probe; no synthetic method doubles.
    static Method[] inspect(ClassDef adapter, ClassDef panel, ClassDef provider, ClassDef listener) {
        require(adapter != null && "Lrb0/h;".equals(adapter.getType())
                && "Lra/q1;".equals(adapter.getSuperclass()), "Candidate adapter type changed");
        require(panel != null && "Lrb0/k;".equals(panel.getType())
                && "Lrb0/d;".equals(panel.getSuperclass()), "Candidate panel type changed");
        require(provider != null && "Lrb0/j;".equals(provider.getType())
                && "Ljava/lang/Object;".equals(provider.getSuperclass()), "Candidate provider type changed");
        require(listener != null && "Lrb0/l;".equals(listener.getType())
                && "Lra/f2;".equals(listener.getSuperclass()), "Candidate scroll listener type changed");
        Method[] methods = new Method[OWNERS.length];
        for (int i = 0; i < methods.length; i++) {
            String descriptor = OWNERS[i] + "->" + SIGNATURES[i];
            ClassDef target = i == 5 ? listener : i == 3 ? provider : i == 2 || i == 4 ? panel : adapter;
            for (Method method : target.getMethods()) {
                if (!descriptor.equals(FORMAT.getMethodDescriptor(method))) continue;
                require(methods[i] == null, "Duplicate candidate method " + descriptor);
                methods[i] = method;
            }
            verify(methods[i], i);
        }
        return methods;
    }

    private static void verify(Method method, int i) {
        require(method != null && method.getImplementation() != null,
                "Missing candidate method " + SIGNATURES[i]);
        require((OWNERS[i] + "->" + SIGNATURES[i]).equals(FORMAT.getMethodDescriptor(method)),
                "Candidate method signature changed");
        require(method.getAccessFlags() == 0x11, "Candidate method access changed");
        require(method.getImplementation().getRegisterCount() == REGISTERS[i],
                "Candidate method registers changed " + SIGNATURES[i]);
        require(HASHES[i].equals(MethodContract.sha256(method)),
                "Candidate method fingerprint changed " + SIGNATURES[i]);
    }

    static void applyMethods(MutableMethod[] methods) {
        require(methods != null && methods.length == 6, "Candidate hook target count changed");
        for (int i = 0; i < methods.length; i++) verify(methods[i], i);
        int refreshReturn = uniqueReturn(methods[1]);
        int refreshNotify = uniqueNotify(methods[1], 4);
        int attachNotify = uniqueNotify(methods[2], 1);
        int providerDispatch = uniqueVirtualCall(methods[3], DISPATCH, 0, 6);
        int providerReturn = uniqueReturn(methods[3]);
        int panelScroll = uniqueVirtualCall(methods[4], SCROLL, 1, 0);
        require(refreshNotify + 1 == refreshReturn, "Candidate refresh tail changed");
        require(providerDispatch + 1 == providerReturn, "Candidate dispatch tail changed");
        require(panelScroll + 1 == uniqueReturn(methods[4]), "Candidate panel scroll tail changed");

        // d(Z)V has no local register. Only false (append) enters the helper, so
        // its false result can reuse p1 without changing the fallback argument.
        // A true (replace/reset) event reaches the complete original method.
        EDIT.addInstructions(methods[0], 0,
                "if-nez p1, :candidate_native\n"
                + "invoke-static {p0, p1}, Lhiro/swiftkey/CandidateScroll;->tryAppend(Lrb0/h;Z)Z\n"
                + "move-result p1\n"
                + "if-eqz p1, :candidate_native\n"
                + "return-void\n"
                + ":candidate_native\n"
                + "nop");
        EDIT.addInstructions(methods[1], refreshReturn, "invoke-static {p0}, " + SNAPSHOT);
        // First attachment runs G()+notify directly, without going through H().
        EDIT.addInstructions(methods[2], attachNotify + 1, "invoke-static {v1}, " + SNAPSHOT);
        // Scope preservation around the original notifier rather than assuming
        // its WeakHashMap-backed panel/adapter listener iteration has an order.
        EDIT.replaceInstruction(methods[3], providerDispatch,
                "invoke-static {p0, v0, p1}, Lhiro/swiftkey/CandidateScroll;->dispatchUpdate(Lrb0/j;Lfb0/u;Z)V");
        // Native equal-result and disconnected-provider paths share this return.
        // Their dormant preparation must be cleared even without notification.
        // Replace the labeled return itself, then append return-void: branches
        // to the old return must execute cleanup instead of skipping an insert.
        EDIT.replaceInstruction(methods[3], providerReturn,
                "invoke-static {p0}, Lhiro/swiftkey/CandidateScroll;->finishUpdate(Lrb0/j;)V");
        EDIT.addInstruction(methods[3], providerReturn + 1, "return-void");
        // Keep all panel reset bookkeeping and refresh calls. Only the original
        // scroll-to-zero/stop-scroll operation is conditionally delegated.
        EDIT.replaceInstruction(methods[4], panelScroll,
                "invoke-static {p0, v1, v0}, Lhiro/swiftkey/CandidateScroll;->scrollPanelToStart(Lrb0/k;Landroidx/recyclerview/widget/RecyclerView;I)V");
        // A restored layout can dispatch onScrolled(0, 0). Rebase only its
        // eligible one-shot watermark; retain every native comparison/fetch.
        EDIT.addInstruction(methods[5], 0,
                "invoke-static {p0, p1, p2, p3}, Lhiro/swiftkey/CandidateScroll;->rebaseScrollWatermark(Lrb0/l;Landroidx/recyclerview/widget/RecyclerView;II)V");
        // Supplemental candidates can move the tail on each EXPANDED fetch.
        // Stabilize only that eligible incoming container before the provider's
        // unchanged native prefix comparison decides between reset and append.
        EDIT.addInstructions(methods[3], 0,
                "invoke-static {p0, p1}, Lhiro/swiftkey/CandidateScroll;->stabilize(Lrb0/j;Lib0/a;)Lib0/a;\n"
                + "move-result-object p1");
        for (int i = 0; i < methods.length; i++)
            require(methods[i].getImplementation().getRegisterCount() == REGISTERS[i],
                    "Candidate hook changed register count");
    }

    private static int uniqueReturn(MutableMethod method) {
        int found = -1;
        List<? extends Instruction> code = method.getImplementation().getInstructions();
        for (int i = 0; i < code.size(); i++) if (code.get(i).getOpcode() == Opcode.RETURN_VOID) {
            require(found == -1, "Multiple candidate refresh returns");
            found = i;
        }
        require(found >= 0, "Missing candidate refresh return");
        return found;
    }

    private static int uniqueNotify(MutableMethod method, int register) {
        int found = -1;
        List<? extends Instruction> code = method.getImplementation().getInstructions();
        for (int i = 0; i < code.size(); i++) {
            Instruction instruction = code.get(i);
            if (instruction.getOpcode() != Opcode.INVOKE_VIRTUAL
                    || !(instruction instanceof ReferenceInstruction)
                    || !NOTIFY.equals(FORMAT.getReference(((ReferenceInstruction) instruction).getReference()))) continue;
            require(found == -1, "Multiple candidate full-refresh calls");
            FiveRegisterInstruction call = (FiveRegisterInstruction) instruction;
            require(call.getRegisterCount() == 1 && call.getRegisterC() == register,
                    "Candidate full-refresh receiver changed");
            found = i;
        }
        require(found >= 0, "Missing candidate full-refresh call");
        return found;
    }

    private static int uniqueVirtualCall(MutableMethod method, String reference, int first, int second) {
        int found = -1;
        List<? extends Instruction> code = method.getImplementation().getInstructions();
        for (int i = 0; i < code.size(); i++) {
            Instruction instruction = code.get(i);
            if (!(instruction instanceof ReferenceInstruction)
                    || !reference.equals(FORMAT.getReference(((ReferenceInstruction) instruction).getReference()))) continue;
            require(found == -1 && instruction.getOpcode() == Opcode.INVOKE_VIRTUAL,
                    "Ambiguous candidate native call " + reference);
            FiveRegisterInstruction call = (FiveRegisterInstruction) instruction;
            require(call.getRegisterCount() == 2 && call.getRegisterC() == first && call.getRegisterD() == second,
                    "Candidate native call registers changed " + reference);
            found = i;
        }
        require(found >= 0, "Missing candidate native call " + reference);
        return found;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
