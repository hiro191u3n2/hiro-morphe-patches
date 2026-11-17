import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Independent final APK audit. No patch provider implementation is called. */
public final class AuditLocalizedDex {
    static final String LOOKUP = "Lapp/morphe/extension/shared/ResourceUtils;->getStringOrThrow(Ljava/lang/String;)Ljava/lang/String;";
    static final String CONTEXT = "Lapp/morphe/extension/shared/Utils;->setContext(Landroid/content/Context;)V";
    static final String PREFIX = "hiro_piko_newx_";
    static String methodKey(MethodReference m) {
        StringBuilder s = new StringBuilder(m.getDefiningClass()).append("->").append(m.getName()).append('(');
        for (CharSequence p : m.getParameterTypes()) s.append(p);
        return s.append(')').append(m.getReturnType()).toString();
    }
    static String quoted(String s) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if(c=='"'||c=='\\') out.append('\\').append(c);
            else if(c=='\n') out.append("\\n");
            else if(c=='\r') out.append("\\r");
            else if(c=='\t') out.append("\\t");
            else if(c<32) out.append(String.format("\\u%04x",(int)c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
    static String array(Collection<String> values) {
        List<String> q = new ArrayList<>(); for(String s:values)q.add(quoted(s)); return "["+String.join(",",q)+"]";
    }
    static String ref(Instruction i) {
        if(!(i instanceof ReferenceInstruction))return null;
        Reference r=((ReferenceInstruction)i).getReference();
        return r instanceof MethodReference ? methodKey((MethodReference)r) : null;
    }
    static String literal(Instruction i) {
        if(i.getOpcode()!=Opcode.CONST_STRING && i.getOpcode()!=Opcode.CONST_STRING_JUMBO)return null;
        return ((StringReference)((ReferenceInstruction)i).getReference()).getString();
    }
    static List<Instruction> instructions(Method m) {
        List<Instruction> out = new ArrayList<>();
        if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions())out.add(i);
        return out;
    }
    static boolean validLookup(List<Instruction> code,int i) {
        if(i+2>=code.size())return false;
        int r=((OneRegisterInstruction)code.get(i)).getRegisterA();
        Instruction call=code.get(i+1),result=code.get(i+2);
        boolean args=false;
        if(call.getOpcode()==Opcode.INVOKE_STATIC_RANGE) {
            RegisterRangeInstruction x=(RegisterRangeInstruction)call;
            args=x.getRegisterCount()==1&&x.getStartRegister()==r;
        } else if(call.getOpcode()==Opcode.INVOKE_STATIC) {
            FiveRegisterInstruction x=(FiveRegisterInstruction)call;
            args=x.getRegisterCount()==1&&x.getRegisterC()==r;
        }
        return args&&LOOKUP.equals(ref(call))&&result.getOpcode()==Opcode.MOVE_RESULT_OBJECT&&((OneRegisterInstruction)result).getRegisterA()==r;
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("final.apk targets.tsv report.json");
        Map<String,Method> methods = new HashMap<>();
        Set<String> classes = new HashSet<>();
        List<String> failures = new ArrayList<>(),contexts=new ArrayList<>();
        Map<String,Integer> actualSites = new TreeMap<>();
        Map<String,Integer> actualClasses = new TreeMap<>();
        int totalLookupSites=0;
        MultiDexContainer<? extends DexFile> z=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.forApi(26));
        for(String d:z.getDexEntryNames())for(ClassDef c:z.getEntry(d).getDexFile().getClasses()) {
            if(!classes.add(c.getType()))failures.add("Duplicate class: "+c.getType());
            for(Method m:c.getMethods()) {
                String key=methodKey(m);
                if(c.getType().startsWith("Lapp/morphe/extension/"))methods.put(key,m);
                List<Instruction> code=instructions(m);
                for(int i=0;i<code.size();i++) {
                    String value=literal(code.get(i));
                    if(value!=null&&value.startsWith(PREFIX)) {
                        totalLookupSites++;
                        String site=key+"\t"+value;
                        actualSites.put(site,actualSites.getOrDefault(site,0)+1);
                        actualClasses.put(c.getType(),actualClasses.getOrDefault(c.getType(),0)+1);
                        if(!validLookup(code,i))failures.add("Invalid resource lookup/register flow at "+key+" #"+i);
                    }
                    if(CONTEXT.equals(ref(code.get(i)))) {
                        List<String> calls=new ArrayList<>();
                        boolean startupBefore=false;
                        for(int j=0;j<code.size();j++) {
                            String r=ref(code.get(j));
                            if(r!=null&&(r.contains("Lapp/morphe/extension/")||r.contains("Lapp/hiro/newx/"))) {
                                calls.add(j+": "+r);
                                if(j<i&&r.contains("Lapp/morphe/extension/newx/"))startupBefore=true;
                            }
                        }
                        contexts.add("{\"method\":"+quoted(key)+",\"set_context_instruction\":"+i+",\"extension_calls\":"+array(calls)+",\"newx_hook_before_context\":"+startupBefore+"}");
                        if(startupBefore)failures.add("NewX startup hook precedes Utils.setContext: "+key);
                    }
                }
            }
        }
        int checked=0,expectedTotal=0;
        Set<String> expectedClasses=new TreeSet<>();
        Map<String,Integer> expectedSites = new TreeMap<>();
        for(String line:Files.readAllLines(Paths.get(args[1]),StandardCharsets.UTF_8)) {
            if(line.isEmpty())continue;
            String[] a=line.split("\t",-1);
            String key=a[0],resource=a[1],source=new String(Base64.getDecoder().decode(a[3]),StandardCharsets.UTF_8);
            int count=Integer.parseInt(a[2]);
            Method m=methods.get(key);
            expectedClasses.add(key.substring(0,key.indexOf("->")));
            expectedSites.put(key+"\t"+resource,count);
            expectedTotal+=count;
            if(m==null){failures.add("Missing target method: "+key);continue;}
            int resourceCount=0,sourceCount=0;
            for(Instruction i:instructions(m)) {
                String value=literal(i);
                if(resource.equals(value))resourceCount++;
                if(source.equals(value))sourceCount++;
            }
            if(resourceCount!=count)failures.add("Resource count mismatch: "+key+" "+resource+" "+resourceCount+"/"+count);
            if(sourceCount!=0)failures.add("Original target literal remains: "+key+" "+quoted(source));
            checked++;
        }
        if(!expectedSites.equals(actualSites))failures.add("Whole-APK localized lookup-site inventory differs from exact target list");
        Method lookup=methods.get(LOOKUP);
        if(lookup==null||(lookup.getAccessFlags()&9)!=9)failures.add("ResourceUtils.getStringOrThrow is missing or not public static");
        if(contexts.isEmpty())failures.add("No Utils.setContext initialization found");
        StringBuilder counts=new StringBuilder("{");
        for(Map.Entry<String,Integer> e:actualClasses.entrySet()) {
            if(counts.length()>1)counts.append(','); counts.append(quoted(e.getKey())).append(':').append(e.getValue());
        }
        counts.append('}');
        String report="{\"result\":"+quoted(failures.isEmpty()?"PASS":"FAIL")+",\"apk\":"+quoted(new File(args[0]).getAbsolutePath())
            +",\"classes_scanned\":"+classes.size()+",\"exact_target_rows\":"+checked+",\"expected_lookup_sites\":"+expectedTotal
            +",\"actual_lookup_sites\":"+totalLookupSites+",\"target_classes\":"+expectedClasses.size()
            +",\"class_lookup_counts\":"+counts+",\"context_initialization\":["+String.join(",",contexts)+"],\"failures\":"+array(failures)
            +",\"limitations\":[\"Static final DEX instruction and initialization-order checks; no device UI execution.\"]}\n";
        Files.write(Paths.get(args[2]),report.getBytes(StandardCharsets.UTF_8));
        System.out.println((failures.isEmpty()?"PASS":"FAIL")+" classes="+classes.size()+" targetRows="+checked+" lookupSites="+totalLookupSites+" targetClasses="+expectedClasses.size()+" contextInitializations="+contexts.size());
        for(String f:failures)System.out.println(f);
        if(!failures.isEmpty())System.exit(1);
    }
}
