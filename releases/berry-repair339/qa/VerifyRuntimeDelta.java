import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.nio.file.*;
import java.util.*;

/** Bound DEX delta: removed diagnostics, exact unchanged classes, and approved instrumented classes.
 * The runner additionally proves source token equality after explicit diagnostic-only deletions.
 */
public final class VerifyRuntimeDelta {
    private static final Opcodes OPCODES = Opcodes.forApi(26);
    private static final String COORD = "Lhiro/berrybfcache/RestoreCoordinator;";
    private static final String WEB = "Landroid/webkit/WebView;", STR = "Ljava/lang/String;";
    private static final String PROOF = "Lhiro/berrybfcache/PresentationRecovery$Proof;";
    private static final String PRESENT = "Lhiro/berrybfcache/PresentationRecovery;->start(" + WEB + PROOF + ")V";
    private static final DexFormatter F = DexFormatter.INSTANCE;
    private static void need(boolean ok, String message) { if (!ok) throw new IllegalStateException(message); }
    private static boolean family(String type, String name) {
        String prefix="Lhiro/berrybfcache/"+name;
        return type.equals(prefix+";") || type.equals(prefix+"-IA;") || type.startsWith(prefix+"$");
    }
    private static boolean diagnostic(String type) {
        return family(type,"RenderDiagnostics") || family(type,"PostScrollDiagnostics") || family(type,"RestoreDiagnostics");
    }
    private static boolean instrumented(String type) {
        return family(type,"RestoreCoordinator") || family(type,"NativeRestoreTracker") || family(type,"PresentationRecovery");
    }
    private static Map<String,ClassDef> read(String path) throws Exception {
        Map<String,ClassDef> result=new TreeMap<>();
        for(ClassDef c:DexFileFactory.loadDexFile(Path.of(path).toFile(),OPCODES).getClasses())
            need(result.put(c.getType(),c)==null,"Duplicate class: "+c.getType());
        return result;
    }
    private static byte[] canonical(ClassDef c) throws Exception {
        List<Method> methods=new ArrayList<>();
        for(Method m:c.getMethods()) {
            MethodImplementation impl=m.getImplementation();
            if(impl!=null)impl=new ImmutableMethodImplementation(impl.getRegisterCount(),impl.getInstructions(),impl.getTryBlocks(),Collections.emptyList());
            methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),impl));
        }
        ClassDef normalized=new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);
        MemoryDataStore output=new MemoryDataStore();
        try { DexPool.writeTo(output,new ImmutableDexFile(OPCODES,Collections.singleton(normalized)));
            return Arrays.copyOf(output.getBuffer(),output.getSize()); } finally { output.close(); }
    }
    private static Method complete(ClassDef c) {
        Method found=null;
        for(Method m:c.getMethods())if(m.getName().equals("completeState")){need(found==null,"Duplicate completion method");found=m;}
        need(found!=null,"Missing completion method");
        need((found.getAccessFlags()&10)==10 && found.getReturnType().equals("V") && found.getParameterTypes().equals(Arrays.asList(WEB,"Lhiro/berrybfcache/RestoreCoordinator$State;","Lhiro/berrybfcache/RestoreCoordinator$Probe;",STR)),"Changed completion ABI");
        return found;
    }
    private static int[] args(Instruction i) {
        if(i instanceof FiveRegisterInstruction){FiveRegisterInstruction r=(FiveRegisterInstruction)i;return Arrays.copyOf(new int[]{r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()},r.getRegisterCount());}
        RegisterRangeInstruction r=(RegisterRangeInstruction)i;int[] a=new int[r.getRegisterCount()];for(int n=0;n<a.length;n++)a[n]=r.getStartRegister()+n;return a;
    }
    private static String register(int r,int count,Map<Integer,Integer> locals){return r>=count-4?"p"+(r-count+4):"v"+locals.computeIfAbsent(r,k->locals.size());}
    private static String prefix(Method m) {
        StringBuilder b=new StringBuilder();int count=m.getImplementation().getRegisterCount();Map<Integer,Integer> locals=new LinkedHashMap<>();
        for(Instruction i:m.getImplementation().getInstructions()){
            if(i.getOpcode().name().startsWith("INVOKE_")||i.getOpcode().name().equals("NEW_INSTANCE"))break;
            need(!(i instanceof OffsetInstruction),"Completion prefix has a branch");
            // D8 hoists the old log TAG constant before the first state write.
            if(i instanceof ReferenceInstruction && ((ReferenceInstruction)i).getReference() instanceof StringReference && ((StringReference)((ReferenceInstruction)i).getReference()).getString().equals("BerryBFCache")) {
                need(i.getOpcode().name().startsWith("CONST_STRING"),"Unexpected TAG reference");continue;
            }
            b.append(i.getOpcode().name());
            if(i instanceof OneRegisterInstruction)b.append('/').append(register(((OneRegisterInstruction)i).getRegisterA(),count,locals));
            if(i instanceof TwoRegisterInstruction)b.append('/').append(register(((TwoRegisterInstruction)i).getRegisterB(),count,locals));
            if(i instanceof ThreeRegisterInstruction)b.append('/').append(register(((ThreeRegisterInstruction)i).getRegisterC(),count,locals));
            if(i instanceof WideLiteralInstruction)b.append('/').append(((WideLiteralInstruction)i).getWideLiteral());
            if(i instanceof ReferenceInstruction)b.append('/').append(F.getReference(((ReferenceInstruction)i).getReference()));b.append('\n');
        }return b.toString();
    }
    private static Map<String,Integer> calls(Method m) {
        Map<String,Integer> calls=new TreeMap<>();int[] presentation=null,constructor=null;int writes=0;boolean invoked=false;
        for(Instruction i:m.getImplementation().getInstructions()){
            if(i.getOpcode().name().startsWith("IPUT")){need(!invoked,"State writes added after proof construction");writes++;}
            if(!(i instanceof ReferenceInstruction)||!(((ReferenceInstruction)i).getReference() instanceof MethodReference))continue;
            invoked=true;MethodReference r=(MethodReference)((ReferenceInstruction)i).getReference();String descriptor=F.getMethodDescriptor(r);
            if(diagnostic(r.getDefiningClass())||r.getDefiningClass().equals("Landroid/util/Log;"))continue;
            if(descriptor.equals(PRESENT)){need(presentation==null,"Duplicate presentation start");presentation=args(i);}
            if(r.getName().equals("<init>")&&family(r.getDefiningClass(),"RestoreCoordinator")){need(constructor==null,"Duplicate proof construction");constructor=args(i);}
            calls.merge(descriptor,1,Integer::sum);
        }
        need(writes==5&&presentation!=null&&constructor!=null,"Completion state/proof/presentation missing");
        int p=m.getImplementation().getRegisterCount()-4;
        need(presentation.length==2&&presentation[0]==p,"Presentation is not the current view");
        need(Arrays.equals(constructor,new int[]{presentation[1],p+1,p+2,p+3}),"Presentation proof capture changed");return calls;
    }
    public static void main(String[] args) throws Exception {
        need(args.length==2,"Expected baseline and candidate runtime extensions");
        Map<String,ClassDef> base=read(args[0]),next=read(args[1]);
        Method before=complete(base.get(COORD)),after=complete(next.get(COORD));
        need(prefix(before).equals(prefix(after)),"Loading completion state prefix changed\nBASE\n"+prefix(before)+"NEXT\n"+prefix(after));
        need(calls(before).equals(calls(after)),"Completion calls changed beyond diagnostics");
        int preserved=0,changed=0,removed=0;
        for(String type:base.keySet()){
            if(!next.containsKey(type)){need(diagnostic(type),"Non-diagnostic class removed: "+type);removed++;continue;}
            need(!diagnostic(type),"Diagnostic class retained: "+type);
            if(Arrays.equals(canonical(base.get(type)),canonical(next.get(type))))preserved++;
            else {need(instrumented(type),"Non-instrumented class changed: "+type);changed++;}
        }
        for(String type:next.keySet())need(base.containsKey(type),"Unexpected class added: "+type);
        String raw=new String(Files.readAllBytes(Path.of(args[1])),java.nio.charset.StandardCharsets.UTF_8);
        for(String forbidden:Arrays.asList("RenderDiagnostics","PostScrollDiagnostics","RestoreDiagnostics","android/util/Log","android/widget/","描画記録","OnScrollChangedListener","imagesScanned","SCROLL_SETTLED"))
            need(!raw.contains(forbidden),"Diagnostic reference/string remains: "+forbidden);
        need(preserved>0&&removed>0,"Expected preserved core and removed diagnostics");
        System.out.println("{\"result\":\"PASS_DIAGNOSTIC_REMOVAL_DEX_DELTA\",\"runtime_diagnostics_absent\":true,\"completion_terminal_prefix_and_shared_proof_valid\":true,\"debug_positions_excluded\":true,\"preserved_classes\":"+preserved+",\"changed_instrumented_classes\":"+changed+",\"removed_diagnostic_classes\":"+removed+",\"added_classes\":0}");
    }
}
