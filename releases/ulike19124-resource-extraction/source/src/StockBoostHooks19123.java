import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Exact, invertible replacement of one optional still-photo boost request.
 * The original stock-input contract stays unchanged. No vendor boost manager,
 * preview, readiness, countdown, callback or image-processing body is edited. */
public final class StockBoostHooks19123 {
    static final String TARGET="Li/o/a/b1/a/g/y;->X(IZ)Z";
    static final String ORIGINAL="Li/o/a/p/b;->g()V";
    static final String REPLACEMENT="Lcom/hiro/ulike/CaptureBoost19123;->suppress(Ljava/lang/Object;)V";
    static final String STOCK_HASH="593a400906e70cefffc72bc383dc371afab34763795f33fc0b58e352d3dc2c98";
    static final String BASE_HASH="7915f1efa39a0ef7f69dc54751732002276c53fa6b72699ca5fde655755d2dc8";
    static void req(boolean value,String message){MergePayloads.require(value,message);}
    static String ref(Instruction instruction){return instruction instanceof ReferenceInstruction?((ReferenceInstruction)instruction).getReference().toString():"";}
    static Method wrap(Method old,MethodImplementation body){return new ImmutableMethod(old.getDefiningClass(),old.getName(),old.getParameters(),old.getReturnType(),old.getAccessFlags(),old.getAnnotations(),old.getHiddenApiRestrictions(),body);}
    static Method change(Method old,boolean inverse){
        req(TARGET.equals(MergePayloads.id(old)),"Only the exact still-photo call site may change");
        req(old.getImplementation()!=null&&old.getImplementation().getRegisterCount()==9,"Pinned shutter register ABI");
        MutableMethodImplementation body=new MutableMethodImplementation(old.getImplementation());
        int hits=0;
        for(int index=0;index<body.getInstructions().size();index++){
            Instruction instruction=body.getInstructions().get(index);
            String value=ref(instruction);
            req(!value.equals(inverse?ORIGINAL:REPLACEMENT),"Contradictory boost call site");
            if(!value.equals(inverse?REPLACEMENT:ORIGINAL))continue;
            req(instruction.getOpcode()==(inverse?Opcode.INVOKE_STATIC:Opcode.INVOKE_VIRTUAL),"Pinned boost invocation opcode");
            req(instruction instanceof FiveRegisterInstruction,"Pinned boost invocation format");
            FiveRegisterInstruction registers=(FiveRegisterInstruction)instruction;
            req(registers.getRegisterCount()==1&&registers.getRegisterC()==0,"Pinned boost receiver register");
            req(index>0&&ref(body.getInstructions().get(index-1)).equals("Li/o/a/p/b;->a:Li/o/a/p/b;"),"Original receiver/class initialization must remain");
            body.replaceInstruction(index,new BuilderInstruction35c(inverse?Opcode.INVOKE_VIRTUAL:Opcode.INVOKE_STATIC,1,0,0,0,0,0,
                inverse?new ImmutableMethodReference("Li/o/a/p/b;","g",List.of(),"V"):
                        new ImmutableMethodReference("Lcom/hiro/ulike/CaptureBoost19123;","suppress",List.of("Ljava/lang/Object;"),"V")));
            hits++;
        }
        req(hits==1,"Exactly one optional still-photo boost site is required");
        return wrap(old,body);
    }
    static Method patch(Method old){
        req(MergePayloads.hash(old).equals(BASE_HASH),"Published .122 shutter body changed");
        Method after=change(old,false);
        req(MergePayloads.hash(change(after,true)).equals(BASE_HASH),"Full shutter inverse differs");
        return after;
    }
    static String verify(Path beforeMethods,Path afterMethods,Path beforeTsv,Path afterTsv)throws Exception{
        var beforeClasses=MergePayloads.classes(beforeMethods.toString());
        var afterClasses=MergePayloads.classes(afterMethods.toString());
        req(beforeClasses.keySet().equals(afterClasses.keySet()),"Stock payload class inventory changed");
        var before=MergePayloads.methods(beforeClasses.values());
        var after=MergePayloads.methods(afterClasses.values());
        req(before.keySet().equals(after.keySet()),"Stock payload method inventory changed");
        req(before.containsKey(TARGET)&&MergePayloads.hash(before.get(TARGET)).equals(BASE_HASH),"Pinned input shutter body absent");
        Method restored=change(after.get(TARGET),true);
        req(MergePayloads.hash(restored).equals(BASE_HASH),"Readiness/capture/branch/handler behavior changed outside boost call site");
        int preserved=0;
        for(var row:before.entrySet())if(!row.getKey().equals(TARGET)){
            req(MergePayloads.hash(row.getValue()).equals(MergePayloads.hash(after.get(row.getKey()))),"Unrelated stock method changed: "+row.getKey());preserved++;
        }
        for(var row:beforeClasses.entrySet()){
            ClassDef current=afterClasses.get(row.getKey());
            if(row.getKey().equals(before.get(TARGET).getDefiningClass())){
                var methods=new ArrayList<Method>();
                for(Method method:current.getMethods())methods.add(MergePayloads.id(method).equals(TARGET)?restored:method);
                current=new ImmutableClassDef(current.getType(),current.getAccessFlags(),current.getSuperclass(),current.getInterfaces(),current.getSourceFile(),current.getAnnotations(),current.getFields(),methods);
            }
            req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(current)),"Stock class/header inverse changed: "+row.getKey());
        }
        var oldContracts=MergePayloads.contracts(beforeTsv.toString());
        var newContracts=MergePayloads.contracts(afterTsv.toString());
        req(oldContracts.keySet().equals(before.keySet())&&newContracts.keySet().equals(after.keySet()),"Contract inventories differ from payloads");
        req(oldContracts.get(TARGET)[1].equals(STOCK_HASH),"Original APK shutter contract changed");
        for(var row:oldContracts.entrySet()){
            String[] next=newContracts.get(row.getKey());
            req(row.getValue()[1].equals(next[1]),"Original input contract changed: "+row.getKey());
            req(row.getValue()[2].equals(MergePayloads.hash(before.get(row.getKey()))),"Baseline payload contract mismatch: "+row.getKey());
            req(next[2].equals(MergePayloads.hash(after.get(row.getKey()))),"Emitted payload contract mismatch: "+row.getKey());
            if(!row.getKey().equals(TARGET))req(Arrays.equals(row.getValue(),next),"Unrelated contract row changed: "+row.getKey());
        }
        return "{\"schema\":\"ulike19123-stock-boost-callsite-v1\",\"status\":\"passed\",\"target\":\""+TARGET+"\","
            +"\"original_stock_sha256\":\""+STOCK_HASH+"\",\"baseline_patched_sha256\":\""+BASE_HASH+"\","
            +"\"patched_sha256\":\""+MergePayloads.hash(after.get(TARGET))+"\",\"changed_method_count\":1,"
            +"\"preserved_method_count\":"+preserved+",\"single_callsite_inverse_verified\":true,"
            +"\"original_input_contracts_preserved\":true,\"receiver_initialization_preserved\":true,"
            +"\"camera_readiness_capture_callbacks_preserved\":true,\"vendor_boost_manager_edited\":false,"
            +"\"requested_hint_duration_is_not_a_sleep\":true,\"device_cause_confirmed\":false,\"device_performance_measured\":false}";
    }
    static void write(Path base,Path out)throws Exception{
        var classes=MergePayloads.classes(base.resolve("ulike/methods.dex").toString());
        Method target=MergePayloads.methods(classes.values()).get(TARGET);req(target!=null,"Baseline capture method absent");
        ClassDef old=classes.get(target.getDefiningClass());var methods=new ArrayList<Method>();
        Method updated=patch(target);
        for(Method method:old.getMethods())methods.add(MergePayloads.id(method).equals(TARGET)?updated:method);
        classes.put(old.getType(),new ImmutableClassDef(old.getType(),old.getAccessFlags(),old.getSuperclass(),old.getInterfaces(),old.getSourceFile(),old.getAnnotations(),old.getFields(),methods));
        MergePayloads.writeDex(out.resolve("methods.dex"),classes.values());
        List<String> rows=Files.readAllLines(base.resolve("ulike/methods.tsv"));int hits=0;
        for(int index=0;index<rows.size();index++)if(rows.get(index).startsWith(TARGET+"\t")){
            req(rows.get(index).equals(TARGET+"\t"+STOCK_HASH+"\t"+BASE_HASH),"Pinned contract row changed");
            rows.set(index,TARGET+"\t"+STOCK_HASH+"\t"+MergePayloads.hash(updated));hits++;
        }
        req(hits==1,"Exactly one contract row is required");Files.write(out.resolve("methods.tsv"),rows);
        Files.writeString(out.resolve("stock-boost19123.json"),verify(base.resolve("ulike/methods.dex"),out.resolve("methods.dex"),base.resolve("ulike/methods.tsv"),out.resolve("methods.tsv"))+"\n");
    }
    public static void main(String[] args)throws Exception{
        req(args.length==5,"BASE_METHODS NEW_METHODS BASE_TSV NEW_TSV OUTPUT_JSON");
        String report=verify(Path.of(args[0]),Path.of(args[1]),Path.of(args[2]),Path.of(args[3]));
        Files.writeString(Path.of(args[4]),report+"\n");System.out.println(report);
    }
}
