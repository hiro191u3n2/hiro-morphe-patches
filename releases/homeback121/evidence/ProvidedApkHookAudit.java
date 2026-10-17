import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Read-only examination of the supplied, already patched application APKs.
 * Validates concrete method/field callsites against replacement extension ABI.
 * Does not repatch/sign APKs or claim an Android/device behavioral test.
 */
public final class ProvidedApkHookAudit {
    static final String PREFIX="Lapp/hiro/oneback/runtime/";
    static String methodKey(MethodReference m) {
        StringBuilder r=new StringBuilder(m.getDefiningClass()).append("->").append(m.getName()).append('(');
        for(CharSequence p:m.getParameterTypes())r.append(p);
        return r.append(')').append(m.getReturnType()).toString();
    }
    static String fieldKey(FieldReference f){return f.getDefiningClass()+"->"+f.getName()+":"+f.getType();}
    static String hex(byte[] bytes){StringBuilder r=new StringBuilder();for(byte b:bytes)r.append(String.format("%02x",b&255));return r.toString();}
    static String sha(byte[] bytes)throws Exception{return hex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    static String sha(Path path)throws Exception {MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=Files.newInputStream(path)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}return hex(d.digest());}
    static byte[] canonical(ClassDef c)throws Exception {MemoryDataStore mem=new MemoryDataStore();try{DexPool.writeTo(mem,new ImmutableDexFile(Opcodes.getDefault(),Collections.singleton(c)));return Arrays.copyOf(mem.getBuffer(),mem.getSize());}finally{mem.close();}}
    static String methodHash(Method m)throws Exception{return sha(canonical(new ImmutableClassDef(m.getDefiningClass(),1,"Ljava/lang/Object;",Collections.emptyList(),null,Collections.emptySet(),Collections.emptyList(),Collections.singleton(m))));}
    static Map<String,ClassDef> runtime(Path path)throws Exception {
        Map<String,ClassDef> out=new TreeMap<>();MultiDexContainer<? extends DexFile> c=DexFileFactory.loadDexContainer(path.toFile(),Opcodes.getDefault());
        for(String entry:c.getDexEntryNames())for(ClassDef k:c.getEntry(entry).getDexFile().getClasses())if(k.getType().startsWith(PREFIX)){
            if(out.put(k.getType(),k)!=null)throw new IllegalStateException("Duplicate runtime class "+k.getType());
        }
        return out;
    }
    static String json(Object o) {
        if(o==null)return "null";
        if(o instanceof Number||o instanceof Boolean)return String.valueOf(o);
        if(o instanceof Map){StringBuilder b=new StringBuilder("{");boolean sep=false;for(Object raw:((Map<?,?>)o).entrySet()){Map.Entry<?,?> e=(Map.Entry<?,?>)raw;if(sep)b.append(',');sep=true;b.append(json(e.getKey())).append(':').append(json(e.getValue()));}return b.append('}').toString();}
        if(o instanceof Collection){StringBuilder b=new StringBuilder("[");boolean sep=false;for(Object v:(Collection<?>)o){if(sep)b.append(',');sep=true;b.append(json(v));}return b.append(']').toString();}
        String s=o.toString();StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString();
    }
    static final class Hook {
        String descriptor;boolean field;int calls;Set<String> opcodes=new TreeSet<>(),samples=new TreeSet<>();
        Hook(String d,boolean f){descriptor=d;field=f;}
        Map<String,Object> summary(){Map<String,Object> o=new LinkedHashMap<>();o.put("descriptor",descriptor);o.put("kind",field?"field":"method");o.put("callsites",calls);o.put("opcodes",opcodes);o.put("sample_callers",samples);return o;}
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("APK OLD_RUNTIME NEW_RUNTIME_OR_DASH OUTPUT_JSON");
        Path apk=Paths.get(args[0]),oldPath=Paths.get(args[1]),out=Paths.get(args[3]);
        Map<String,ClassDef> old=runtime(oldPath),current=new TreeMap<>();
        Map<String,Hook> hooks=new TreeMap<>();Map<String,String> hookedMethods=new TreeMap<>();
        List<String> mismatch=new ArrayList<>(),missing=new ArrayList<>(),duplicates=new ArrayList<>();
        Set<String> seen=new HashSet<>();int classes=0,methods=0,callSites=0,wrappedHelpers=0;
        MultiDexContainer<? extends DexFile> container=DexFileFactory.loadDexContainer(apk.toFile(),Opcodes.getDefault());
        List<String> dexNames=container.getDexEntryNames();Collections.sort(dexNames);
        for(String entry:dexNames) {
            for(ClassDef c:container.getEntry(entry).getDexFile().getClasses()) {
                classes++;if(!seen.add(c.getType()))duplicates.add(c.getType());
                if(c.getType().startsWith(PREFIX)){current.put(c.getType(),c);continue;}
                for(Method m:c.getMethods()) {
                    methods++;if(m.getName().startsWith("hiroOneBack$"))wrappedHelpers++;
                    if(m.getImplementation()==null)continue;boolean hooked=false;
                    for(Instruction ins:m.getImplementation().getInstructions()) {
                        if(!(ins instanceof ReferenceInstruction))continue;
                        Reference r=((ReferenceInstruction)ins).getReference();String descriptor=null;boolean field=false;
                        if(r instanceof MethodReference&&((MethodReference)r).getDefiningClass().startsWith(PREFIX))descriptor=methodKey((MethodReference)r);
                        if(r instanceof FieldReference&&((FieldReference)r).getDefiningClass().startsWith(PREFIX)){descriptor=fieldKey((FieldReference)r);field=true;}
                        if(descriptor==null)continue;
                        hooked=true;callSites++;Hook h=hooks.get(descriptor);if(h==null){h=new Hook(descriptor,field);hooks.put(descriptor,h);}
                        h.calls++;h.opcodes.add(ins.getOpcode().name());if(h.samples.size()<4)h.samples.add(methodKey(m));
                    }
                    if(hooked)hookedMethods.put(methodKey(m),methodHash(m));
                }
            }
        }
        for(Map.Entry<String,ClassDef> e:current.entrySet())if(!old.containsKey(e.getKey())||!Arrays.equals(canonical(e.getValue()),canonical(old.get(e.getKey()))))mismatch.add(e.getKey());
        for(String name:old.keySet())if(!current.containsKey(name))missing.add(name);
        Map<String,Object> report=new LinkedHashMap<>();
        report.put("schema","provided-patched-apk-hook-audit-v1");report.put("input_apk",apk.getFileName().toString());report.put("input_sha256",sha(apk));report.put("input_bytes",Files.size(apk));
        try(ZipFile z=new ZipFile(apk.toFile());InputStream manifest=z.getInputStream(z.getEntry("AndroidManifest.xml"))){AndroidManifestBlock m=AndroidManifestBlock.load(manifest);report.put("package",m.getPackageName());report.put("version",m.getVersionName());report.put("version_code",m.getVersionCode());report.put("min_sdk",m.getMinSdkVersion());report.put("target_sdk",m.getTargetSdkVersion());}
        report.put("physical_android_tested",false);report.put("clean_original_apk_tested",false);report.put("input_apk_modified",false);
        report.put("old_extension_sha256",sha(oldPath));report.put("dex_entries",dexNames);report.put("total_classes",classes);report.put("non_runtime_methods",methods);
        report.put("duplicate_class_definitions",duplicates);report.put("existing_runtime_classes",current.size());report.put("known_v116_runtime_classes",old.size());
        report.put("old_runtime_canonical_class_mismatches",mismatch);report.put("old_runtime_classes_missing",missing);report.put("matches_known_v116_runtime",mismatch.isEmpty()&&missing.isEmpty()&&!current.isEmpty());
        report.put("existing_wrapper_helper_methods",wrappedHelpers);report.put("external_runtime_callsites",callSites);report.put("external_hooked_methods",hookedMethods.size());
        report.put("external_hooked_methods_canonical_sha256",sha(json(hookedMethods).getBytes("UTF-8")));
        List<Object> hookReports=new ArrayList<>();for(Hook h:hooks.values())hookReports.add(h.summary());report.put("runtime_entrypoints_referenced_by_apk",hookReports);
        boolean ok=duplicates.isEmpty()&&mismatch.isEmpty()&&missing.isEmpty()&&!hooks.isEmpty();
        if(!args[2].equals("-")) {
            Path nextPath=Paths.get(args[2]);Map<String,ClassDef> next=runtime(nextPath);Map<String,Method> nextMethods=new TreeMap<>();Map<String,Field> nextFields=new TreeMap<>();
            for(ClassDef c:next.values()){for(Method m:c.getMethods())nextMethods.put(methodKey(m),m);for(Field f:c.getFields())nextFields.put(fieldKey(f),f);}
            List<String> errors=new ArrayList<>();int publicApi=0;
            for(ClassDef c:current.values())if((c.getAccessFlags()&1)!=0)for(Method m:c.getMethods())if((m.getAccessFlags()&1)!=0) {
                publicApi++;Method replacement=nextMethods.get(methodKey(m));
                if(replacement==null)errors.add("removed public method "+methodKey(m));
                else if((replacement.getAccessFlags()&9)!=(m.getAccessFlags()&9))errors.add("changed public/static flags "+methodKey(m));
            }
            for(Hook h:hooks.values()) {
                String owner=h.descriptor.substring(0,h.descriptor.indexOf("->"));ClassDef c=next.get(owner);
                if(c==null||(c.getAccessFlags()&1)==0){errors.add("external hook owner absent/nonpublic "+owner);continue;}
                if(h.field){Field f=nextFields.get(h.descriptor);if(f==null||(f.getAccessFlags()&1)==0)errors.add("external field absent/nonpublic "+h.descriptor);}
                else {Method m=nextMethods.get(h.descriptor);if(m==null||(m.getAccessFlags()&1)==0){errors.add("external method absent/nonpublic "+h.descriptor);continue;}
                    for(String op:h.opcodes)if(op.startsWith("INVOKE_")){boolean staticCall=op.startsWith("INVOKE_STATIC"),staticMethod=(m.getAccessFlags()&8)!=0;if(staticCall!=staticMethod)errors.add("invoke static/instance mismatch "+h.descriptor+" "+op);}
                }
            }
            report.put("replacement_runtime_sha256",sha(nextPath));report.put("replacement_runtime_classes",next.size());report.put("old_public_methods_preserved",publicApi);report.put("replacement_abi_errors",errors);report.put("replacement_abi_valid_for_all_existing_hooks",errors.isEmpty());ok&=errors.isEmpty();
            report.put("scope_note","Reads all supplied APK DEX code; verifies the known v1.1.6 extension identity and every existing external method/field hook against replacement descriptors/access/static ABI. Does not execute app UI, repatch, sign, install or test device behavior.");
        } else report.put("scope_note","Read-only baseline input inventory. Replacement ABI check pending compiled v1.1.8 runtime. No UI/device test or APK mutation.");
        report.put("result",ok?"PASS":"FAIL");Files.createDirectories(out.getParent());Files.write(out,(json(report)+"\n").getBytes("UTF-8"));
        System.out.println((ok?"PASS":"FAIL")+" "+apk.getFileName()+" classes="+classes+" hooks="+callSites+" entrypoints="+hooks.size()+" runtime="+current.size()+" old mismatches="+mismatch.size()+" report="+out);
        if(!ok)System.exit(2);
    }
}
