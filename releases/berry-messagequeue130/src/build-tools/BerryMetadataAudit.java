import app.morphe.patcher.patch.AppTarget;
import app.morphe.patcher.patch.Compatibility;
import app.morphe.patcher.patch.Patch;
import app.morphe.patcher.patch.PatchKt;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/** Verify all-version metadata from the shipped MPP using the official JVM loader.
 * Runtime classpath: this audit class directory plus one official Morphe host/CLI JAR.
 * Args: candidate.mpp expectedBerryPatchCount
 */
public final class BerryMetadataAudit {
    private static final String PACKAGE = "jp.ejimax.berrybrowser";
    private static final Set<String> EXISTING = new TreeSet<>(Arrays.asList(
        "プレビュー作成を後回しにして表示中タブを優先",
        "共有先ブラウザの表示を個別に切替",
        "ダブルタップから指を離さず片手ズーム"));
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        check(args.length == 2, "Expected candidate MPP and expected Berry patch count");
        Path bundle = Paths.get(args[0]).toRealPath();
        int expected = Integer.parseInt(args[1]);
        check(expected >= 3, "Existing Berry patches must all be retained");
        check(System.getProperty("java.class.path").split(File.pathSeparator).length == 2,
            "Use audit directory and one official host only");
        Set<String> found = new TreeSet<>();
        Collection<? extends Patch<?>> loaded = PatchKt.loadPatchesFromJar(Collections.singleton(bundle.toFile()));
        for (Patch<?> patch : loaded) {
            if (patch.getCompatibility() == null) continue;
            for (Compatibility compatible : patch.getCompatibility()) {
                if (!PACKAGE.equals(compatible.getPackageName())) continue;
                check(found.add(patch.getName()), "Duplicate Berry patch: " + patch.getName());
                boolean unrestricted = compatible.getTargets() == null || compatible.getTargets().isEmpty();
                if (!unrestricted) for (AppTarget target : compatible.getTargets())
                    if (target.getVersion() == null) unrestricted = true;
                check(unrestricted, "Still version-restricted: " + patch.getName());
                Field executeField = Patch.class.getDeclaredField("executeBlock");
                executeField.setAccessible(true);
                Object execute = executeField.get(patch);
                check(execute != null, "Missing executable JVM patch: " + patch.getName());
                Path origin = Paths.get(execute.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
                check(origin.equals(bundle), "Execution class did not come from distributed MPP: " + patch.getName());
                System.out.println("PATCH all_versions=true executable_from_mpp=true name=" + patch.getName());
            }
        }
        check(found.containsAll(EXISTING), "Existing Berry patch missing from JVM discovery: " + found);
        check(found.size() == expected, "Berry patch count differs: " + found.size() + " expected " + expected);
        System.out.println("PASS BerryMetadataAudit: " + found.size() + " Berry patches; all versions metadata; shipped JVM implementation.");
    }
}
