import java.util.*;
import java.nio.file.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;

/** Independently validates exact UI/IO rewrites, five preserved alias bodies,
 * compiler equality, helper symbol links and legal exception handler ranges. */
public final class VerifyTiming1947 {
    static int checks;
    static void need(boolean value,String reason){checks++;if(!value)throw new IllegalStateException(reason);}
    static String id(Method method){return MergePayloads.id(method);}
    static String root(String type){int at=type.indexOf('$');return at<0?type:type.substring(0,at)+";";}
    static final Set<String> OWNED=owned();
    static Set<String> owned(){
        var roots=new TreeSet<String>();
        for(String name:System.getProperty("ulike.production","AsyncSave1935,BurstCapture1933,ProcessingTiming1947,QualityPipeline1932,ShotContext1932,TimedIo1947").split(","))roots.add("Lcom/hiro/ulike/"+name+";");
        return Collections.unmodifiableSet(roots);
    }
    static void tryRanges(Collection<ClassDef> classes){
        for(ClassDef c:classes)for(Method method:c.getMethods()){
            MethodImplementation body=method.getImplementation();if(body==null)continue;
            int size=0;Set<Integer> boundaries=new HashSet<Integer>();boundaries.add(0);
            for(Instruction op:body.getInstructions()){size+=op.getCodeUnits();boundaries.add(size);}
            for(TryBlock<? extends ExceptionHandler> block:body.getTryBlocks()){
                int first=block.getStartCodeAddress(),last=first+block.getCodeUnitCount();
                need(first>=0&&block.getCodeUnitCount()>0&&block.getCodeUnitCount()<=65535&&last<=size&&boundaries.contains(first)&&boundaries.contains(last),"DEX try span "+id(method));
                for(ExceptionHandler handler:block.getExceptionHandlers())need(handler.getHandlerCodeAddress()>=0&&handler.getHandlerCodeAddress()<size&&boundaries.contains(handler.getHandlerCodeAddress()),"DEX handler boundary "+id(method));
            }
        }
    }
    public static void main(String[] args)throws Exception{
        need(args.length==2||args.length==3,"BASE_RUNTIME_DEX EMITTED_RUNTIME_DEX [COMPILED_HELPER_DEX]");
        Path base=Path.of(args[0]),emitted=Path.of(args[1]);
        Path baseDex=Files.isDirectory(base)?base.resolve("ulike/runtime.dex"):base;
        Path emittedDex=Files.isDirectory(emitted)?emitted.resolve("runtime.dex"):emitted;
        var before=MergePayloads.classes(baseDex.toString());var after=MergePayloads.classes(emittedDex.toString());
        var bm=MergePayloads.methods(before.values());var am=MergePayloads.methods(after.values());
        for(String key:TimingHooks1947.RUNTIME){
            need(bm.containsKey(key)&&am.containsKey(key),"diagnostic boundary present "+key);
            TimingHooks1947.verify(bm.get(key),am.get(key));checks++;
            Method alias=TimingHooks1947.alias(bm.get(key));
            if(alias!=null){need(am.containsKey(id(alias)),"original alias retained "+id(alias));TimingHooks1947.verifyAlias(bm.get(key),am.get(id(alias)));checks++;}
        }
        for(var entry:before.entrySet()){
            ClassDef next=after.get(entry.getKey());need(next!=null,"baseline runtime class retained");
            if(OWNED.contains(root(entry.getKey())))continue;
            var oldMethods=MergePayloads.methods(List.of(entry.getValue()));var newMethods=MergePayloads.methods(List.of(next));
            var expected=new TreeSet<String>(oldMethods.keySet());
            for(Method old:oldMethods.values()){Method alias=TimingHooks1947.alias(old);if(alias!=null)expected.add(id(alias));}
            need(newMethods.keySet().equals(expected),"only five diagnostic aliases added "+entry.getKey());
            for(Method old:oldMethods.values())if(!TimingHooks1947.RUNTIME.contains(id(old)))need(MergePayloads.hash(old).equals(MergePayloads.hash(newMethods.get(id(old)))),"unrelated runtime method unchanged "+id(old));
            ClassDef restored=new ImmutableClassDef(next.getType(),next.getAccessFlags(),next.getSuperclass(),next.getInterfaces(),next.getSourceFile(),next.getAnnotations(),next.getFields(),oldMethods.values());
            need(MergePayloads.classHash(ImmutableClassDef.of(entry.getValue())).equals(MergePayloads.classHash(ImmutableClassDef.of(restored))),"hook class shell unchanged "+entry.getKey());
        }
        Set<String> fields=new HashSet<String>();for(ClassDef c:after.values())for(Field f:c.getFields())fields.add(f.toString());
        for(ClassDef c:after.values())for(Method method:c.getMethods())if(method.getImplementation()!=null)for(Instruction op:method.getImplementation().getInstructions())if(op instanceof ReferenceInstruction reference){
            if(reference.getReference() instanceof MethodReference target&&OWNED.contains(root(target.getDefiningClass())))need(am.containsKey(target.toString()),"diagnostic helper/alias method resolves "+target);
            if(reference.getReference() instanceof FieldReference target&&OWNED.contains(root(target.getDefiningClass())))need(fields.contains(target.toString()),"diagnostic helper field resolves "+target);
        }
        Path compiledPath=args.length==3?Path.of(args[2]):Files.isDirectory(emitted)?emitted.getParent().resolve("helper-dex/classes.dex"):null;
        if(compiledPath!=null){var compiled=MergePayloads.classes(compiledPath.toString());need(!compiled.isEmpty(),"compiled diagnostic helpers present");for(var entry:compiled.entrySet())need(OWNED.contains(root(entry.getKey()))&&after.containsKey(entry.getKey())&&MergePayloads.classHash(entry.getValue()).equals(MergePayloads.classHash(after.get(entry.getKey()))),"emitted helper bytecode equals compiler "+entry.getKey());}
        if(Files.isDirectory(base)&&Files.isDirectory(emitted)){
            need(Files.mismatch(base.resolve("ulike/methods.dex"),emitted.resolve("methods.dex"))==-1,"native DEX payload byte-identical");
            need(Files.mismatch(base.resolve("ulike/methods.tsv"),emitted.resolve("methods.tsv"))==-1,"native method contracts byte-identical");
        }
        tryRanges(after.values());
        System.out.println("PASS_TIMING1947 checks="+checks+"; five original aliases preserved; settings/IO calls and options retained; no device timing claimed");
    }
}
