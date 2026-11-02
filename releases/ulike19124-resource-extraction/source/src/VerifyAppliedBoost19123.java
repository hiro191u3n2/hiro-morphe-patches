import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;

/** Original private APK and fully rebuilt APK proof, never Android execution. */
public final class VerifyAppliedBoost19123 {
    static void req(boolean value,String message){MergePayloads.require(value,message);}
    static String sha(Path file)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}
    static boolean vendor(String type){return type.startsWith("Lf/b/a/a/")||type.startsWith("Landroid/ss/com/vboost/")||type.equals("Li/o/a/p/b;");}
    static int calls(Collection<ClassDef> classes,String target){
        int hits=0;
        for(ClassDef cls:classes)for(Method method:cls.getMethods())if(method.getImplementation()!=null)
            for(Instruction instruction:method.getImplementation().getInstructions())if(StockBoostHooks19123.ref(instruction).equals(target))hits++;
        return hits;
    }
    public static void main(String[] args)throws Exception{
        req(args.length==3,"ORIGINAL_APKS PATCHED_APK OUTPUT_JSON");
        Path input=Path.of(args[0]),patched=Path.of(args[1]),out=Path.of(args[2]);
        Path base=Files.createTempFile(patched.getParent(),"original-boost-proof-",".apk");
        Map<String,ClassDef> original;
        try{
            try(ZipFile archive=new ZipFile(input.toFile())){
                ZipEntry entry=archive.getEntry("base.apk");req(entry!=null,"Pinned original base split absent");
                try(var stream=archive.getInputStream(entry)){Files.copy(stream,base,StandardCopyOption.REPLACE_EXISTING);}
            }
            original=MergePayloads.classes(base.toString());
        }finally{Files.deleteIfExists(base);}
        var after=MergePayloads.classes(patched.toString());
        var originalMethods=MergePayloads.methods(original.values());
        var afterMethods=MergePayloads.methods(after.values());
        Method old=originalMethods.get(StockBoostHooks19123.TARGET),current=afterMethods.get(StockBoostHooks19123.TARGET);
        req(old!=null&&MergePayloads.hash(old).equals(StockBoostHooks19123.STOCK_HASH),"Original stock shutter differs");
        req(current!=null&&MergePayloads.hash(StockBoostHooks19123.change(current,true)).equals(StockBoostHooks19123.BASE_HASH),"Applied shutter inverse differs from released .122");
        req(calls(original.values(),StockBoostHooks19123.ORIGINAL)==1,"Original boost caller inventory changed");
        req(calls(after.values(),StockBoostHooks19123.ORIGINAL)==0,"Original still-photo boost remains reachable from a direct call");
        req(calls(after.values(),StockBoostHooks19123.REPLACEMENT)==1,"Exactly one applied suppression site required");
        req(afterMethods.containsKey(StockBoostHooks19123.REPLACEMENT),"Suppression helper missing from APK");
        int classes=0,methods=0;
        for(var row:original.entrySet())if(vendor(row.getKey())){
            ClassDef updated=after.get(row.getKey());req(updated!=null,"Vendor boost class removed");
            req(MergePayloads.classHash(row.getValue()).equals(MergePayloads.classHash(updated)),"Vendor boost manager/backend changed: "+row.getKey());
            classes++;for(Method method:row.getValue().getMethods())methods++;
        }
        req(classes>10&&original.containsKey("Li/o/a/p/b;"),"Original vendor boost inventory incomplete");
        String report="{\"schema\":\"ulike19123-applied-stock-boost-v1\",\"status\":\"passed\","
            +"\"apk_sha256\":\""+sha(patched)+"\",\"original_input_sha256\":\""+sha(input)+"\","
            +"\"one_callsite_suppressed\":true,\"actual_shutter_inverse_matches_122\":true,"
            +"\"original_input_contract_preserved\":true,\"vendor_boost_manager_preserved\":true,"
            +"\"vendor_boost_classes_preserved\":"+classes+",\"vendor_boost_methods_preserved\":"+methods+","
            +"\"device_cause_confirmed\":false,\"physical_android_tested\":false}";
        Files.writeString(out,report+"\n");System.out.println(report);
    }
}
