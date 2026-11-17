import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import app.hiro.newxui.patches.ChatRecommendationsHook;
import app.hiro.newxui.patches.PinBarsPatch;
import app.hiro.newxui.patches.UiOptionsPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Full APK preservation and exact transformed-class audit, independent of ZIP/DEX ordering. */
public final class CompletedUiApkAudit {
    static final Opcodes OPS=Opcodes.forApi(26);
    static final Set<String> CHANGED=new TreeSet<>(Arrays.asList(ChatRecommendationsHook.TYPE,PinBarsPatch.TOP,PinBarsPatch.BOTTOM,UiOptionsPatch.BUILTIN,UiOptionsPatch.MAIN));
    static final Set<String> ADDED=new TreeSet<>(Arrays.asList("Lapp/hiro/newxui/UiOptions;","Lapp/hiro/newxui/RefreshRate;","Lapp/hiro/newxui/RefreshRate$Saved;"));
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static byte[] bytes(ClassDef c,boolean normalize)throws Exception{
        ClassDef target=c;
        if(normalize){
            List<Method> methods=new ArrayList<>();
            for(Method m:c.getMethods()){
                MethodImplementation impl=m.getImplementation();
                if(impl!=null){
                    MutableMethodImplementation mut=new MutableMethodImplementation(impl);
                    for(int i=0;i<mut.getInstructions().size();i++){
                        Instruction ins=mut.getInstructions().get(i);
                        if(ins.getOpcode()==Opcode.CONST_STRING)mut.replaceInstruction(i,new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,((OneRegisterInstruction)ins).getRegisterA(),((ReferenceInstruction)ins).getReference()));
                    }
                    impl=mut;
                }
                methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),impl));
            }
            target=new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);
        }
        MemoryDataStore out=new MemoryDataStore();
        try{DexPool.writeTo(out,new ImmutableDexFile(OPS,Collections.singleton(target)));return Arrays.copyOf(out.getBuffer(),out.getSize());}finally{out.close();}
    }
    static Map<String,String> inventory(String path)throws Exception{
        Map<String,String> result=new TreeMap<>();MessageDigest sha=MessageDigest.getInstance("SHA-256");
        MultiDexContainer<? extends DexFile> dex=DexFileFactory.loadDexContainer(new File(path),OPS);
        for(String entry:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(entry).getDexFile().getClasses()){
            require(result.put(c.getType(),HexFormat.of().formatHex(sha.digest(bytes(c,false))))==null,"duplicate class "+c.getType());
        }
        return result;
    }
    static Map<String,ClassDef> selected(String path,Set<String> types)throws Exception{
        Map<String,ClassDef> result=new TreeMap<>();MultiDexContainer<? extends DexFile> dex=DexFileFactory.loadDexContainer(new File(path),OPS);
        for(String entry:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(entry).getDexFile().getClasses())if(types.contains(c.getType()))result.put(c.getType(),c);
        return result;
    }
    static String strings(Collection<String> values){List<String>x=new ArrayList<>();for(String s:values)x.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",x)+"]";}
    public static void main(String[] args)throws Exception{
        if(args.length!=4)throw new IllegalArgumentException("baseline.apk completed.apk expected-runtime.dex report.json");
        Map<String,String> before=inventory(args[0]),after=inventory(args[1]);
        Set<String> removed=new TreeSet<>(before.keySet());removed.removeAll(after.keySet());
        Set<String> added=new TreeSet<>(after.keySet());added.removeAll(before.keySet());
        Set<String> rawChanged=new TreeSet<>();int exactSame=0;
        for(String t:before.keySet())if(after.containsKey(t)){if(before.get(t).equals(after.get(t)))exactSame++;else rawChanged.add(t);}
        Set<String> inspect=new TreeSet<>(rawChanged);inspect.addAll(CHANGED);inspect.addAll(ADDED);
        Map<String,ClassDef> original=selected(args[0],inspect),finished=selected(args[1],inspect);
        Set<String> actualChanged=new TreeSet<>(),normalizedSame=new TreeSet<>();
        for(String t:rawChanged){if(Arrays.equals(bytes(original.get(t),true),bytes(finished.get(t),true)))normalizedSame.add(t);else actualChanged.add(t);}
        List<String> failures=new ArrayList<>();
        if(!removed.isEmpty())failures.add("Removed classes: "+removed);
        if(!ADDED.equals(added))failures.add("Unexpected added class set: "+added);
        if(!CHANGED.equals(actualChanged))failures.add("Changed class set mismatch: "+actualChanged);
        // Reconstruct only five intended edits on the exact baseline class definitions.
        Map<String,MutableClass> expected=new TreeMap<>();for(String t:CHANGED)expected.put(t,new MutableClass(original.get(t)));
        ChatRecommendationsHook.apply(expected.get(ChatRecommendationsHook.TYPE));
        PinBarsPatch.verify(expected.get(PinBarsPatch.TOP),expected.get(PinBarsPatch.BOTTOM));
        PinBarsPatch.apply(expected.get(PinBarsPatch.TOP),expected.get(PinBarsPatch.BOTTOM));
        UiOptionsPatch.verifySettings(expected.get(UiOptionsPatch.BUILTIN));UiOptionsPatch.applySettings(expected.get(UiOptionsPatch.BUILTIN));
        UiOptionsPatch.verifyActivity(expected.get(UiOptionsPatch.MAIN));UiOptionsPatch.applyActivity(expected.get(UiOptionsPatch.MAIN));
        for(String t:CHANGED)if(!Arrays.equals(bytes(expected.get(t),true),bytes(finished.get(t),true)))failures.add("Completed hook differs from verified transformation: "+t);
        Map<String,ClassDef> runtime=selected(args[2],ADDED);
        if(!runtime.keySet().equals(ADDED))failures.add("Expected runtime DEX does not contain exactly required runtime classes");
        for(String t:ADDED)if(runtime.get(t)==null||finished.get(t)==null||!Arrays.equals(bytes(runtime.get(t),true),bytes(finished.get(t),true)))failures.add("Completed runtime class differs: "+t);
        String report="{\"result\":\""+(failures.isEmpty()?"PASS":"FAIL")+"\",\"baseline_classes\":"+before.size()+",\"completed_classes\":"+after.size()+",\"exact_identical_classes\":"+exactSame+",\"jumbo_normalized_identical_classes\":"+normalizedSame.size()+",\"changed_classes\":"+strings(actualChanged)+",\"added_classes\":"+strings(added)+",\"removed_classes\":"+strings(removed)+",\"exact_expected_hooks_verified\":"+failures.isEmpty()+",\"physical_device_tested\":false,\"failures\":"+strings(failures)+"}\n";
        Files.writeString(Path.of(args[3]),report);System.out.print(report);
        if(!failures.isEmpty())throw new AssertionError("APK audit failed");
    }
}
