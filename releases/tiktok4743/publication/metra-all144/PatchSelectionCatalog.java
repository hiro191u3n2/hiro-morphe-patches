import app.morphe.patcher.patch.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Read-only official loader check, to run only when the parent schedules this job. */
public final class PatchSelectionCatalog {
    public static void main(String[] args) throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("PACKAGE VERSION COUNT MPP NAMES_OUT");
        Set<String> names=new TreeSet<>();
        for(Patch<?> patch:PatchKt.loadPatchesFromJar(Collections.singleton(Paths.get(args[3]).toFile()))) {
            String name=patch.getName();if(name==null)continue;
            if(!names.add(name))throw new AssertionError("Duplicate named patch "+name);
            boolean compatible=patch.getCompatibility().isEmpty();
            for(Compatibility value:patch.getCompatibility()) {
                if(!args[0].equals(value.getPackageName()))continue;
                if(value.getTargets()==null||value.getTargets().isEmpty())compatible=true;
                else for(AppTarget target:value.getTargets())if(target.getVersion()==null||args[1].equals(target.getVersion()))compatible=true;
            }
            if(!compatible)throw new AssertionError("Named patch lacks declared target compatibility: "+name);
        }
        if(names.size()!=Integer.parseInt(args[2]))throw new AssertionError("Expected "+args[2]+" named patches, found "+names.size());
        Files.write(Paths.get(args[4]),names,StandardCharsets.UTF_8);
        System.out.println("PASS selection catalog: named="+names.size()+"; package="+args[0]+"; version="+args[1]);
    }
}
