import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;

/** Keeps the immediately previous published extension's public ABI in addition
 * to the concrete hooks found in supplied APKs that still contain v1.1.6.
 */
public final class BaselineRuntimeAbiAudit {
    public static void main(String[] args) throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("BASELINE_117_DEX REPLACEMENT_118_DEX REPORT_JSON");
        Path baseline=Paths.get(args[0]), replacement=Paths.get(args[1]), output=Paths.get(args[2]);
        String baselineSha=ProvidedApkHookAudit.sha(baseline);
        if(!baselineSha.equals("9701fa4c15240cbfc4590fde129eff1c8f34b445139eda699ecb2721b1ea06ea"))
            throw new IllegalStateException("Expected exact published v1.1.7 runtime");
        Map<String,ClassDef> before=ProvidedApkHookAudit.runtime(baseline),after=ProvidedApkHookAudit.runtime(replacement);
        Map<String,Method> nextMethods=new TreeMap<>();Map<String,Field> nextFields=new TreeMap<>();
        for(ClassDef c:after.values()){for(Method m:c.getMethods())nextMethods.put(ProvidedApkHookAudit.methodKey(m),m);for(Field f:c.getFields())nextFields.put(ProvidedApkHookAudit.fieldKey(f),f);}
        List<String> removed=new ArrayList<>(),changed=new ArrayList<>(),removedFields=new ArrayList<>();int preserved=0,preservedFields=0;
        for(ClassDef c:before.values())if((c.getAccessFlags()&1)!=0) {
            ClassDef n=after.get(c.getType());if(n==null||(n.getAccessFlags()&1)==0)changed.add("public class absent/nonpublic "+c.getType());
            for(Method m:c.getMethods())if((m.getAccessFlags()&1)!=0) {
                String key=ProvidedApkHookAudit.methodKey(m);Method target=nextMethods.get(key);
                if(target==null)removed.add(key);
                else if((target.getAccessFlags()&9)!=(m.getAccessFlags()&9))changed.add("public/static method flags "+key);
                else preserved++;
            }
            for(Field f:c.getFields())if((f.getAccessFlags()&1)!=0) {
                String key=ProvidedApkHookAudit.fieldKey(f);Field target=nextFields.get(key);
                if(target==null)removedFields.add(key);
                else if((target.getAccessFlags()&9)!=(f.getAccessFlags()&9))changed.add("public/static field flags "+key);
                else preservedFields++;
            }
        }
        boolean okay=!before.isEmpty()&&preserved>0&&removed.isEmpty()&&changed.isEmpty()&&removedFields.isEmpty();
        Map<String,Object> report=new LinkedHashMap<>();report.put("schema","homeback118-baseline-runtime-abi-v1");
        report.put("baseline_version","1.1.7");report.put("replacement_version","1.1.8");
        report.put("baseline_runtime_sha256",baselineSha);report.put("replacement_runtime_sha256",ProvidedApkHookAudit.sha(replacement));
        report.put("physical_android_tested",false);report.put("baseline_classes",before.size());report.put("replacement_classes",after.size());
        report.put("removed_public_methods",removed);report.put("changed_access_or_static",changed);report.put("preserved_public_methods",preserved);
        report.put("removed_public_fields",removedFields);report.put("preserved_public_fields",preservedFields);
        report.put("result",okay?"PASS":"FAIL");report.put("scope","Static public class/method/field ABI preservation against exact published v1.1.7 runtime. Complements the provided APK hook audit against installed v1.1.6. No Android execution or behavioral validation.");
        Files.createDirectories(output.getParent());Files.write(output,(ProvidedApkHookAudit.json(report)+"\n").getBytes("UTF-8"));
        System.out.println((okay?"PASS":"FAIL")+" baseline1.1.7 public methods="+preserved+" report="+output);
        if(!okay)System.exit(2);
    }
}
