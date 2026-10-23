import app.morphe.patcher.patch.*;
import java.io.File;import java.lang.reflect.Field;import java.nio.file.*;import java.util.*;
/** Exercise the official JVM loader with only the shipped MPP on its patch path. */
public final class MetadataAudit {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        Path path=Paths.get(args[0]).toRealPath();int count=0;
        for(Patch<?> patch:PatchKt.loadPatchesFromJar(Collections.singleton(path.toFile()))) {
            if(!"TikTok設定を日本語化".equals(patch.getName()))continue;count++;
            check(!patch.getDefault(),"companion must be explicitly selected");
            Set<String> packages=new TreeSet<>();
            for(Compatibility c:patch.getCompatibility()){
                packages.add(c.getPackageName());boolean all=c.getTargets()==null||c.getTargets().isEmpty();
                if(!all)for(AppTarget t:c.getTargets())if(t.getVersion()==null)all=true;
                check(all,"unexpected app version restriction");
            }
            check(packages.equals(new TreeSet<>(Arrays.asList("com.zhiliaoapp.musically","com.ss.android.ugc.trill"))),"wrong app scope");
            for(String name:new String[]{"executeBlock","finalizeBlock"}){
                Field field=Patch.class.getDeclaredField(name);field.setAccessible(true);Object block=field.get(patch);
                check(block!=null,"missing action: "+name);
                check(Paths.get(block.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(path),"action not loaded from MPP");
            }
        }
        check(count==1,"patch must be discovered once");
        System.out.println("PASS metadata: 2 TikTok packages; executable and finalizer loaded from shipped MPP");
    }
}
