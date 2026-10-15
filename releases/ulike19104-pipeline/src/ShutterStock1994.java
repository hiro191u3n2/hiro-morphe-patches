import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;

/** One pinned original touch method, with balanced timing scope on every exit. */
public final class ShutterStock1994 {
    public static final String OWNER = "Lcom/light/beauty/mc/preview/shutter/module/ShutterButton;";
    public static final String TARGET = OWNER + "->onTouchEvent(Landroid/view/MotionEvent;)Z";
    public static final String ORIGINAL_SHA256 = "ba56099d88ea1dbdab32dab9302393ddc9f2aae0e5f9900574b2551f973b69c1";
    private static final String HELPER = "Lcom/hiro/ulike/ShutterTiming1994;";
    private static final String ENTER = HELPER + "->enter(Landroid/view/MotionEvent;)V";
    private static final String EXIT = HELPER + "->exit()V";

    static void require(boolean condition, String why) { MergePayloads.require(condition, why); }
    static String ref(Instruction instruction) {
        return instruction instanceof ReferenceInstruction r ? r.getReference().toString() : "";
    }
    static Method replace(Method method, MethodImplementation body) {
        return new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(),
                method.getReturnType(), method.getAccessFlags(), method.getAnnotations(),
                method.getHiddenApiRestrictions(), body);
    }
    static BuilderInstruction35c exitCall() {
        return new BuilderInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0,
                new ImmutableMethodReference(HELPER, "exit", List.of(), "V"));
    }
    static Method seed(Path file) throws Exception {
        var classes = MergePayloads.classes(file.toString());
        var methods = MergePayloads.methods(classes.values());
        require(classes.keySet().equals(Set.of(OWNER)) && methods.keySet().equals(Set.of(TARGET)),
                "Seed must contain only the pinned original shutter method");
        require(!classes.get(OWNER).getFields().iterator().hasNext(), "No unrelated stock fields in seed");
        Method method = methods.get(TARGET);
        original(method);
        return method;
    }
    static void original(Method method) {
        require(method != null && TARGET.equals(MergePayloads.id(method))
                && method.getAccessFlags() == 1 && method.getImplementation() != null
                && method.getImplementation().getRegisterCount() == 6
                && method.getImplementation().getTryBlocks().isEmpty()
                && ORIGINAL_SHA256.equals(MergePayloads.hash(method)), "Pinned original shutter body/header");
        var addresses = new ArrayList<Integer>();
        var registers = new ArrayList<Integer>();
        int address = 0;
        for (Instruction instruction : method.getImplementation().getInstructions()) {
            if (instruction.getOpcode() == Opcode.RETURN) {
                addresses.add(address); registers.add(((OneRegisterInstruction)instruction).getRegisterA());
            }
            address += instruction.getCodeUnits();
        }
        require(address == 80 && addresses.equals(List.of(7,36,74,79))
                && registers.equals(List.of(1,1,1,5)), "Pinned original return paths");
    }
    public static Method patch(Method original) {
        original(original);
        var body = new MutableMethodImplementation(original.getImplementation());
        // Replace the return's location with cleanup, so all original branches
        // targeting that return necessarily run cleanup before the same result.
        int returns = 0;
        for (int i = body.getInstructions().size() - 1; i >= 0; i--) {
            Instruction instruction = body.getInstructions().get(i);
            if (instruction.getOpcode() != Opcode.RETURN) continue;
            int register = ((OneRegisterInstruction)instruction).getRegisterA();
            body.replaceInstruction(i, exitCall());
            body.addInstruction(i + 1, new BuilderInstruction11x(Opcode.RETURN, register));
            returns++;
        }
        require(returns == 4, "Exactly four cleanup return sites");
        body.addInstruction(0, new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,5,0,0,0,0,
                new ImmutableMethodReference(HELPER,"enter",List.of("Landroid/view/MotionEvent;"),"V")));
        int handler = body.getInstructions().size();
        body.addInstruction(new BuilderInstruction11x(Opcode.MOVE_EXCEPTION,0));
        body.addInstruction(exitCall());
        body.addInstruction(new BuilderInstruction11x(Opcode.THROW,0));
        body.addCatch(body.newLabelForIndex(1),body.newLabelForIndex(handler),body.newLabelForIndex(handler));
        Method result = replace(original,body);
        require(MergePayloads.hash(unpatch(result)).equals(ORIGINAL_SHA256), "Shutter inverse restores original");
        return result;
    }
    static Method unpatch(Method patched) {
        require(TARGET.equals(MergePayloads.id(patched)) && patched.getAccessFlags() == 1
                && patched.getImplementation().getRegisterCount() == 6, "Shutter header/registers retained");
        var implementation = patched.getImplementation();
        var instructions = new ArrayList<Instruction>();
        implementation.getInstructions().forEach(instructions::add);
        int count = instructions.size();
        require(count >= 4 && ref(instructions.get(0)).equals(ENTER), "Shutter entry timing hook");
        FiveRegisterInstruction entry = (FiveRegisterInstruction)instructions.get(0);
        require(entry.getOpcode() == Opcode.INVOKE_STATIC && entry.getRegisterCount() == 1
                && entry.getRegisterC() == 5, "Original event parameter is sole entry argument");
        Instruction move = instructions.get(count-3), cleanup = instructions.get(count-2), rethrow = instructions.get(count-1);
        require(move.getOpcode() == Opcode.MOVE_EXCEPTION && ((OneRegisterInstruction)move).getRegisterA() == 0
                && ref(cleanup).equals(EXIT) && cleanup.getOpcode() == Opcode.INVOKE_STATIC
                && ((FiveRegisterInstruction)cleanup).getRegisterCount() == 0
                && rethrow.getOpcode() == Opcode.THROW && ((OneRegisterInstruction)rethrow).getRegisterA() == 0,
                "Shutter exceptional cleanup preserves throwable identity");
        int handlerAddress=0,total=0;
        for(int i=0;i<count;i++){if(i==count-3)handlerAddress=total;total+=instructions.get(i).getCodeUnits();}
        var blocks=implementation.getTryBlocks();
        require(blocks.size()==1 && blocks.get(0).getStartCodeAddress()==instructions.get(0).getCodeUnits()
                && blocks.get(0).getStartCodeAddress()+blocks.get(0).getCodeUnitCount()==handlerAddress
                && blocks.get(0).getExceptionHandlers().size()==1
                && blocks.get(0).getExceptionHandlers().get(0).getExceptionType()==null
                && blocks.get(0).getExceptionHandlers().get(0).getHandlerCodeAddress()==handlerAddress,
                "Catch-all covers every original body instruction, excludes itself");
        var noTry = new ImmutableMethodImplementation(6,instructions,List.of(),implementation.getDebugItems());
        var body = new MutableMethodImplementation(noTry);
        for(int i=0;i<3;i++)body.removeInstruction(body.getInstructions().size()-1);
        int returns=0;
        for(int i=body.getInstructions().size()-1;i>0;i--){
            Instruction instruction=body.getInstructions().get(i);
            if(instruction.getOpcode()!=Opcode.RETURN)continue;
            Instruction previous=body.getInstructions().get(i-1);
            require(ref(previous).equals(EXIT) && previous.getOpcode()==Opcode.INVOKE_STATIC
                    && ((FiveRegisterInstruction)previous).getRegisterCount()==0,"Every normal return has cleanup");
            body.replaceInstruction(i-1,new BuilderInstruction11x(Opcode.RETURN,((OneRegisterInstruction)instruction).getRegisterA()));
            body.removeInstruction(i);returns++;i--;
        }
        require(returns==4,"Exactly original four normal exits");
        body.removeInstruction(0);
        Method restored=replace(patched,body);original(restored);return restored;
    }
    static String report(Method patched) {
        return "{\"schema\":\"ulike1994-stock-shutter-v1\",\"target\":\""+TARGET
                +"\",\"original_sha256\":\""+ORIGINAL_SHA256+"\",\"patched_sha256\":\""+MergePayloads.hash(patched)
                +"\",\"added_method_count\":1,\"existing_methods_preserved\":true,\"register_count\":6,\"normal_exit_count\":4,\"catch_all_cleanup\":true,\"inverse_verified\":true}";
    }
    public static void write(Path baseline, Path seedPath, Path out) throws Exception {
        Method patched=patch(seed(seedPath));
        var classes=MergePayloads.classes(baseline.resolve("ulike/methods.dex").toString());
        var methods=MergePayloads.methods(classes.values());
        var contracts=MergePayloads.contracts(baseline.resolve("ulike/methods.tsv").toString());
        require(!methods.containsKey(TARGET) && !contracts.containsKey(TARGET),"Shutter observer must be a new exact stock method");
        require(methods.keySet().equals(contracts.keySet()),"Baseline app method contract set");
        for(var row:methods.entrySet())require(MergePayloads.hash(row.getValue()).equals(contracts.get(row.getKey())[2]),"Baseline method contract "+row.getKey());
        ClassDef owner=classes.get(OWNER);
        var members=new ArrayList<Method>();if(owner!=null)owner.getMethods().forEach(members::add);members.add(patched);
        classes.put(OWNER,new ImmutableClassDef(OWNER,owner==null?1:owner.getAccessFlags(),owner==null?"Ljava/lang/Object;":owner.getSuperclass(),
                owner==null?List.of():owner.getInterfaces(),owner==null?null:owner.getSourceFile(),
                owner==null?List.of():owner.getAnnotations(),owner==null?List.of():owner.getFields(),members));
        contracts.put(TARGET,new String[]{TARGET,ORIGINAL_SHA256,MergePayloads.hash(patched)});
        Files.createDirectories(out);MergePayloads.writeDex(out.resolve("methods.dex"),classes.values());
        var lines=new ArrayList<String>();for(String[] row:contracts.values())lines.add(String.join("\t",row));Files.write(out.resolve("methods.tsv"),lines);
        verify(baseline,seedPath,out);
    }
    public static void verify(Path baseline, Path seedPath, Path out) throws Exception {
        Method patched=patch(seed(seedPath));
        var before=MergePayloads.methods(MergePayloads.classes(baseline.resolve("ulike/methods.dex").toString()).values());
        var after=MergePayloads.methods(MergePayloads.classes(out.resolve("methods.dex").toString()).values());
        var oldRows=MergePayloads.contracts(baseline.resolve("ulike/methods.tsv").toString());
        var newRows=MergePayloads.contracts(out.resolve("methods.tsv").toString());
        var expected=new TreeSet<String>(before.keySet());require(expected.add(TARGET),"Target absent in baseline");
        require(after.keySet().equals(expected) && newRows.keySet().equals(expected),"Exactly one new stock method");
        for(var row:before.entrySet())require(MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(after.get(row.getKey())))
                && Arrays.equals(oldRows.get(row.getKey()),newRows.get(row.getKey())),"Existing app method/contract retained "+row.getKey());
        require(MergePayloads.hash(patched).equals(MergePayloads.hash(after.get(TARGET)))
                && Arrays.equals(newRows.get(TARGET),new String[]{TARGET,ORIGINAL_SHA256,MergePayloads.hash(patched)}),"Exact emitted hook and contract");
        unpatch(after.get(TARGET));Files.writeString(out.resolve("stock-shutter1994.json"),report(patched)+"\n");
    }
    public static void main(String[] args) throws Exception {
        require(args.length>=1,"write/verify BASE SEED OUT or verify-applied SEED DEX_OR_APK");
        if(args[0].equals("verify-applied")){
            require(args.length==3,"verify-applied SEED DEX_OR_APK");Method patched=patch(seed(Path.of(args[1])));
            Method actual=MergePayloads.methods(MergePayloads.classes(args[2]).values()).get(TARGET);
            require(actual!=null && MergePayloads.hash(actual).equals(MergePayloads.hash(patched)),"Exact applied shutter hook");
            unpatch(actual);System.out.println(report(actual));
        }else{
            require(args.length==4,"write/verify BASE SEED OUT");
            if(args[0].equals("write"))write(Path.of(args[1]),Path.of(args[2]),Path.of(args[3]));
            else {require(args[0].equals("verify"),"Unknown mode");verify(Path.of(args[1]),Path.of(args[2]),Path.of(args[3]));}
            System.out.println(Files.readString(Path.of(args[3]).resolve("stock-shutter1994.json")).trim());
        }
    }
}
