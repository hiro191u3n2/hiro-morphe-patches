import app.morphe.patcher.patch.Compatibility;
import app.morphe.patcher.patch.Patch;
import app.morphe.patcher.patch.PatchKt;
import java.io.File;
import java.util.Set;
import java.util.TreeSet;

/** Print the patch names and original packages Manager can select from the supplied MPP. */
public class BundleCatalog {
    public static void main(String[] args) {
        TreeSet<String> rows = new TreeSet<>();
        for (Patch patch : PatchKt.loadPatchesFromJar(Set.of(new File(args[0])))) {
            if (patch.getName() == null) continue;
            boolean listed = false;
            if (patch.getCompatibility() != null) {
                for (Compatibility app : patch.getCompatibility()) {
                    rows.add(app.getPackageName() + "\t" + (app.getName() == null ? "" : app.getName()) + "\t" + patch.getName());
                    listed = true;
                }
            }
            if (!listed) rows.add("*\t\t" + patch.getName());
        }
        if (rows.isEmpty()) throw new IllegalArgumentException("Empty patch bundle");
        rows.forEach(System.out::println);
    }
}
