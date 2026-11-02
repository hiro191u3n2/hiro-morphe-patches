import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.nio.file.Path;
import java.util.*;

/** Compare full canonical DEX definitions; only the diagnostic implementation may change. */
public final class VerifyRuntimeDelta {
    private static final Opcodes OPCODES = Opcodes.forApi(26);
    private static final String COORD = "Lhiro/berrybfcache/RestoreCoordinator;";
    private static final String WEB = "Landroid/webkit/WebView;", STR = "Ljava/lang/String;";
    private static final String PROOF = "Lhiro/berrybfcache/PresentationRecovery$Proof;";
    private static final String OLD_BEGIN = "Lhiro/berrybfcache/RenderDiagnostics;->begin(" + WEB + ")V";
    private static final String NEW_BEGIN = "Lhiro/berrybfcache/RenderDiagnostics;->begin(" + WEB + PROOF + STR + ")V";
    private static final String PRESENT = "Lhiro/berrybfcache/PresentationRecovery;->start(" + WEB + PROOF + ")V";
    private static final DexFormatter F = DexFormatter.INSTANCE;
    private static void need(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }
    private static boolean diagnostic(String type) {
        for (String name : Arrays.asList("RenderDiagnostics", "PostScrollDiagnostics")) {
            String prefix = "Lhiro/berrybfcache/" + name;
            if (type.equals(prefix + ";") || type.equals(prefix + "-IA;") || type.startsWith(prefix + "$")) return true;
        }
        return false;
    }
    private static Map<String, ClassDef> read(String path) throws Exception {
        Map<String, ClassDef> result = new TreeMap<>();
        for (ClassDef c : DexFileFactory.loadDexFile(Path.of(path).toFile(), OPCODES).getClasses())
            need(result.put(c.getType(), c) == null, "Duplicate class: " + c.getType());
        return result;
    }
    private static ClassDef normalized(ClassDef c) {
        List<Method> methods = new ArrayList<>();
        for (Method m : c.getMethods()) {
            if (c.getType().equals(COORD) && m.getName().equals("completeState")) continue;
            MethodImplementation implementation = m.getImplementation();
            if (implementation != null) implementation = new ImmutableMethodImplementation(
                implementation.getRegisterCount(), implementation.getInstructions(), implementation.getTryBlocks(), Collections.emptyList());
            methods.add(new ImmutableMethod(m.getDefiningClass(), m.getName(), m.getParameters(), m.getReturnType(),
                m.getAccessFlags(), m.getAnnotations(), m.getHiddenApiRestrictions(), implementation));
        }
        return new ImmutableClassDef(c.getType(), c.getAccessFlags(), c.getSuperclass(), c.getInterfaces(),
            c.getSourceFile(), c.getAnnotations(), c.getFields(), methods);
    }
    private static byte[] canonical(ClassDef c) throws Exception {
        MemoryDataStore output = new MemoryDataStore();
        try {
            DexPool.writeTo(output, new ImmutableDexFile(OPCODES, Collections.singleton(normalized(c))));
            return Arrays.copyOf(output.getBuffer(), output.getSize());
        } finally { output.close(); }
    }
    private static Method complete(ClassDef c) {
        Method found = null;
        for (Method m : c.getMethods()) if (m.getName().equals("completeState")) {
            need(found == null, "Duplicate completion method"); found = m;
        }
        need(found != null, "Missing completion method");
        need((found.getAccessFlags() & 10) == 10 && found.getReturnType().equals("V")
            && found.getParameterTypes().equals(Arrays.asList(WEB, "Lhiro/berrybfcache/RestoreCoordinator$State;",
                "Lhiro/berrybfcache/RestoreCoordinator$Probe;", STR)), "Changed completion signature");
        return found;
    }
    private static int[] args(Instruction i) {
        if (i instanceof FiveRegisterInstruction) {
            FiveRegisterInstruction r = (FiveRegisterInstruction)i;
            return Arrays.copyOf(new int[]{r.getRegisterC(), r.getRegisterD(), r.getRegisterE(), r.getRegisterF(), r.getRegisterG()}, r.getRegisterCount());
        }
        RegisterRangeInstruction r = (RegisterRangeInstruction)i;
        int[] a = new int[r.getRegisterCount()]; for (int n=0; n<a.length; n++) a[n]=r.getStartRegister()+n; return a;
    }
    private static String register(int register, int count) { return register >= count - 4 ? "p"+(register-count+4) : "v"+register; }
    private static String prefix(Method m) {
        StringBuilder b = new StringBuilder(); int count=m.getImplementation().getRegisterCount();
        for (Instruction i : m.getImplementation().getInstructions()) {
            if (i.getOpcode().name().startsWith("INVOKE_") || i.getOpcode().name().equals("NEW_INSTANCE")) break;
            need(!(i instanceof OffsetInstruction), "Completion prefix has a new branch");
            b.append(i.getOpcode().name());
            if(i instanceof OneRegisterInstruction)b.append('/').append(register(((OneRegisterInstruction)i).getRegisterA(),count));
            if(i instanceof TwoRegisterInstruction)b.append('/').append(register(((TwoRegisterInstruction)i).getRegisterB(),count));
            if(i instanceof ThreeRegisterInstruction)b.append('/').append(register(((ThreeRegisterInstruction)i).getRegisterC(),count));
            if(i instanceof WideLiteralInstruction)b.append('/').append(((WideLiteralInstruction)i).getWideLiteral());
            if(i instanceof ReferenceInstruction)b.append('/').append(F.getReference(((ReferenceInstruction)i).getReference()));
            b.append('\n');
        }
        return b.toString();
    }
    private static Map<String,Integer> calls(Method m, boolean updated) {
        Map<String,Integer> calls=new TreeMap<>();int[] begin=null,presentation=null,constructor=null;int writes=0;
        boolean invoked=false;
        for(Instruction i:m.getImplementation().getInstructions()) {
            if(i.getOpcode().name().startsWith("IPUT")) { need(!invoked,"State writes added after diagnostics");writes++; }
            if(!(i instanceof ReferenceInstruction)||!(((ReferenceInstruction)i).getReference() instanceof MethodReference))continue;
            invoked=true;MethodReference reference=(MethodReference)((ReferenceInstruction)i).getReference();String descriptor=F.getMethodDescriptor(reference);
            if(descriptor.equals(updated?NEW_BEGIN:OLD_BEGIN)){need(begin==null,"Duplicate diagnostics start");begin=args(i);}
            if(descriptor.equals(PRESENT)){need(presentation==null,"Duplicate presentation start");presentation=args(i);}
            if(reference.getName().equals("<init>")&&reference.getDefiningClass().startsWith(COORD.substring(0,COORD.length()-1)+"$")){need(constructor==null,"Duplicate proof construction");constructor=args(i);}
            calls.merge(descriptor.equals(NEW_BEGIN)?OLD_BEGIN:descriptor,1,Integer::sum);
        }
        need(writes==5,"Completion state write count changed");
        need(begin!=null&&presentation!=null&&constructor!=null,"Completion initialization missing");
        if(updated){int p=m.getImplementation().getRegisterCount()-4;
            need(begin.length==3&&begin[0]==p&&begin[2]==p+3,"Diagnostics arguments not current view/event");
            need(Arrays.equals(presentation,new int[]{p,begin[1]}),"Presentation proof differs from diagnostic proof");
            need(Arrays.equals(constructor,new int[]{begin[1],p+1,p+2,p+3}),"Shared proof capture differs");
        }
        return calls;
    }
    public static void main(String[] args) throws Exception {
        need(args.length == 2, "Expected baseline and candidate runtime extensions");
        Map<String, ClassDef> base = read(args[0]), next = read(args[1]);
        Method before=complete(base.get(COORD)),after=complete(next.get(COORD));
        need(prefix(before).equals(prefix(after)),"Loading completion state prefix changed");
        need(calls(before,false).equals(calls(after,true)),"Completion calls changed beyond diagnostic begin ABI");
        int preserved = 0, changed = 0, added = 0, removed = 0;
        for (String type : base.keySet()) {
            if (!next.containsKey(type)) {
                need(diagnostic(type), "Non-diagnostic class removed: " + type); removed++; continue;
            }
            if (Arrays.equals(canonical(base.get(type)), canonical(next.get(type)))) preserved++;
            else { need(diagnostic(type), "Non-diagnostic class changed: " + type); changed++; }
        }
        for (String type : next.keySet()) if (!base.containsKey(type)) {
            need(diagnostic(type), "Unexpected runtime class added: " + type); added++;
        }
        need(preserved > 0 && changed > 0, "Expected retained core and changed diagnostics");
        System.out.println("{\"result\":\"PASS_DIAGNOSTIC_ONLY_DEX_DELTA\","
            + "\"core_loading_and_presentation_unchanged\":true,\"only_complete_state_instrumentation_allowed\":true,"
            + "\"completion_terminal_prefix_and_shared_proof_valid\":true,\"debug_positions_excluded\":true,\"preserved_classes\":" + preserved
            + ",\"changed_diagnostic_classes\":" + changed + ",\"added_diagnostic_classes\":" + added
            + ",\"removed_diagnostic_classes\":" + removed + "}");
    }
}
