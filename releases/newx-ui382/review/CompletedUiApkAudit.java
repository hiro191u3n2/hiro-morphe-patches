import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
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
    static String strings(Collection<String> values){List<String>x=new ArrayList<>();for(String s:values)x.add("\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"");return "["+String.join(",",x)+"]";}
    static boolean sameDexPayloads(String before,String after)throws Exception{
        try(java.util.zip.ZipFile a=new java.util.zip.ZipFile(before);java.util.zip.ZipFile b=new java.util.zip.ZipFile(after)){
            Set<String> an=new TreeSet<>(),bn=new TreeSet<>();
            a.stream().filter(e->e.getName().matches("classes\\d*\\.dex")).forEach(e->an.add(e.getName()));
            b.stream().filter(e->e.getName().matches("classes\\d*\\.dex")).forEach(e->bn.add(e.getName()));
            if(!an.equals(bn))return false;
            for(String name:an)try(InputStream ai=a.getInputStream(a.getEntry(name));InputStream bi=b.getInputStream(b.getEntry(name))){if(!Arrays.equals(ai.readAllBytes(),bi.readAllBytes()))return false;}
            return true;
        }
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=3)throw new IllegalArgumentException("baseline-v380.apk completed-v382.apk report.json");
        Map<String,String> before=inventory(args[0]),after=inventory(args[1]);
        Set<String> removed=new TreeSet<>(before.keySet());removed.removeAll(after.keySet());
        Set<String> added=new TreeSet<>(after.keySet());added.removeAll(before.keySet());
        Set<String> changed=new TreeSet<>();int exactSame=0;
        for(String t:before.keySet())if(after.containsKey(t)){if(before.get(t).equals(after.get(t)))exactSame++;else changed.add(t);}
        List<String> failures=new ArrayList<>();
        if(!removed.isEmpty())failures.add("Removed classes: "+removed);
        if(!added.isEmpty())failures.add("Added classes: "+added);
        if(!changed.isEmpty())failures.add("Changed classes: "+changed);
        boolean noFormatterState=!after.containsKey("Lapp/hiro/newxui/ViewCountFormat$FormatterState;");
        if(!noFormatterState)failures.add("v381 formatter cache class remains");
        boolean dexIdentical=sameDexPayloads(args[0],args[1]);
        if(!dexIdentical)failures.add("Raw DEX payloads differ from v380");
        MessageDigest sha=MessageDigest.getInstance("SHA-256");
        String oldSha=HexFormat.of().formatHex(sha.digest(Files.readAllBytes(Path.of(args[0]))));
        String newSha=HexFormat.of().formatHex(sha.digest(Files.readAllBytes(Path.of(args[1]))));
        String report="{\"result\":\""+(failures.isEmpty()?"PASS":"FAIL")+"\",\"comparison\":\"v380 to v382 exact rollback\",\"baseline_apk_sha256\":\""+oldSha+"\",\"apk_sha256\":\""+newSha+"\",\"baseline_classes\":"+before.size()+",\"completed_classes\":"+after.size()+",\"exact_identical_classes\":"+exactSame+",\"jumbo_normalized_identical_classes\":0,\"changed_classes\":"+strings(changed)+",\"added_classes\":"+strings(added)+",\"removed_classes\":"+strings(removed)+",\"raw_dex_payloads_exact_identical\":"+dexIdentical+",\"no_v381_formatter_state\":"+noFormatterState+",\"exact_expected_hooks_verified\":"+failures.isEmpty()+",\"earlier_hooks_preserved\":"+failures.isEmpty()+",\"dex_inventory_cache_hits\":"+cacheHits+",\"dex_inventory_cache_misses\":"+cacheMisses+",\"physical_device_tested\":false,\"failures\":"+strings(failures)+"}\n";
        Files.writeString(Path.of(args[2]),report);System.out.print(report);
        if(!failures.isEmpty())throw new AssertionError("Rollback APK audit failed");
    }
}
