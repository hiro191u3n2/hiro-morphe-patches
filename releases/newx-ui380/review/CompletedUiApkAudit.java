import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import app.hiro.newxui.patches.ViewCountHook;
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
    static final String OPTIONS="Lapp/hiro/newxui/UiOptions;";
    static final Set<String> CHANGED=new TreeSet<>(Arrays.asList(ViewCountHook.TYPE,OPTIONS));
    static final Set<String> ADDED=new TreeSet<>(Arrays.asList("Lapp/hiro/newxui/ViewCountFormat;"));
    static final Set<String> RUNTIME=new TreeSet<>(Arrays.asList(OPTIONS,"Lapp/hiro/newxui/RefreshRate;","Lapp/hiro/newxui/RefreshRate$Saved;","Lapp/hiro/newxui/ViewCountFormat;"));
    static final Path CACHE=Path.of(System.getProperty("hiro.audit.cache","/workspace/scratch/440e6cc46323/x-ui380/review/cache"));
    static int cacheHits=0,cacheMisses=0;
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
        Files.createDirectories(CACHE);
        try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(path)) {
          for(String entry:dex.getDexEntryNames()) {
            byte[] input;try(InputStream in=zip.getInputStream(zip.getEntry(entry))){input=in.readAllBytes();}
            String key=HexFormat.of().formatHex(sha.digest(input));
            Path cache=CACHE.resolve("canonical-dex-v1-api26-"+key+".bin");
            Map<String,String> classes=new TreeMap<>();
            if(Files.exists(cache)) {
                try(DataInputStream in=new DataInputStream(new BufferedInputStream(Files.newInputStream(cache)))) {
                    require(in.readUTF().equals(key),"Cache digest mismatch");
                    int count=in.readInt();require(count>=0&&count<300000,"Cache class count");
                    for(int n=0;n<count;n++)require(classes.put(in.readUTF(),in.readUTF())==null,"Cache duplicate class");
                    require(in.read()==-1,"Cache trailing bytes");
                }
                cacheHits++;
            } else {
                for(ClassDef c:dex.getEntry(entry).getDexFile().getClasses())require(classes.put(c.getType(),HexFormat.of().formatHex(sha.digest(bytes(c,false))))==null,"duplicate class "+c.getType());
                Path temp=Files.createTempFile(CACHE,"inventory-",".tmp");
                try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))) {
                    out.writeUTF(key);out.writeInt(classes.size());for(Map.Entry<String,String> item:classes.entrySet()){out.writeUTF(item.getKey());out.writeUTF(item.getValue());}
                }
                Files.move(temp,cache,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);cacheMisses++;
            }
            for(Map.Entry<String,String> item:classes.entrySet())require(result.put(item.getKey(),item.getValue())==null,"duplicate class "+item.getKey());
          }
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
        if(args.length>=2&&args[0].equals("--cache-only")){for(int i=1;i<args.length;i++)System.out.println(args[i]+" classes="+inventory(args[i]).size()+" cacheHits="+cacheHits+" cacheMisses="+cacheMisses);return;}
        if(args.length!=4)throw new IllegalArgumentException("baseline-v379.apk completed-v380.apk expected-runtime.dex report.json");
        Map<String,String> before=inventory(args[0]),after=inventory(args[1]);
        Set<String> removed=new TreeSet<>(before.keySet());removed.removeAll(after.keySet());
        Set<String> added=new TreeSet<>(after.keySet());added.removeAll(before.keySet());
        Set<String> rawChanged=new TreeSet<>();int exactSame=0;
        for(String t:before.keySet())if(after.containsKey(t)){if(before.get(t).equals(after.get(t)))exactSame++;else rawChanged.add(t);}
        Set<String> inspect=new TreeSet<>(rawChanged);inspect.addAll(CHANGED);inspect.addAll(RUNTIME);
        Map<String,ClassDef> original=selected(args[0],inspect),finished=selected(args[1],inspect);
        Set<String> actualChanged=new TreeSet<>(),normalizedSame=new TreeSet<>();
        for(String t:rawChanged){if(Arrays.equals(bytes(original.get(t),true),bytes(finished.get(t),true)))normalizedSame.add(t);else actualChanged.add(t);}
        List<String> failures=new ArrayList<>();
        if(!removed.isEmpty())failures.add("Removed classes: "+removed);
        if(!ADDED.equals(added))failures.add("Unexpected added class set: "+added);
        if(!CHANGED.equals(actualChanged))failures.add("Changed class set mismatch: "+actualChanged);
        // v379 already contains all earlier hooks. Reconstruct only the count-renderer edit.
        MutableClass expected=new MutableClass(original.get(ViewCountHook.TYPE));
        ViewCountHook.verify(expected);ViewCountHook.apply(expected);
        if(!Arrays.equals(bytes(expected,true),bytes(finished.get(ViewCountHook.TYPE),true)))failures.add("Completed count hook differs from verified transformation");
        Map<String,ClassDef> runtime=selected(args[2],RUNTIME);
        if(!runtime.keySet().equals(RUNTIME))failures.add("Expected runtime DEX does not contain all four required runtime classes");
        for(String t:RUNTIME)if(runtime.get(t)==null||finished.get(t)==null||!Arrays.equals(bytes(runtime.get(t),true),bytes(finished.get(t),true)))failures.add("Completed runtime class differs: "+t);
        for(String t:RUNTIME)if(!CHANGED.contains(t)&&!ADDED.contains(t)&&!before.get(t).equals(after.get(t)))failures.add("Existing refresh runtime changed: "+t);
        String report="{\"result\":\""+(failures.isEmpty()?"PASS":"FAIL")+"\",\"comparison\":\"v379 to v380\",\"baseline_classes\":"+before.size()+",\"completed_classes\":"+after.size()+",\"exact_identical_classes\":"+exactSame+",\"jumbo_normalized_identical_classes\":"+normalizedSame.size()+",\"changed_classes\":"+strings(actualChanged)+",\"added_classes\":"+strings(added)+",\"removed_classes\":"+strings(removed)+",\"exact_expected_hooks_verified\":"+failures.isEmpty()+",\"earlier_hooks_preserved\":"+failures.isEmpty()+",\"dex_inventory_cache_hits\":"+cacheHits+",\"dex_inventory_cache_misses\":"+cacheMisses+",\"physical_device_tested\":false,\"failures\":"+strings(failures)+"}\n";
        Files.writeString(Path.of(args[3]),report);System.out.print(report);
        if(!failures.isEmpty())throw new AssertionError("APK audit failed");
    }
}
