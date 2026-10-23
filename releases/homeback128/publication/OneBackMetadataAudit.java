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

/** Official JVM loader must discover the shipped patch with unrestricted version metadata. */
public final class OneBackMetadataAudit {
  private static final String NAME = "戻る1回で終了・履歴削除";
  private static final Set<String> PACKAGES = new TreeSet<>(Arrays.asList("com.twitter.android", "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "ctrip.english"));
  private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
  public static void main(String[] args) throws Exception {
    check(args.length == 1, "Expected candidate MPP");
    Path bundle = Paths.get(args[0]).toRealPath();
    check(System.getProperty("java.class.path").split(File.pathSeparator).length == 2, "Audit and official host only");
    Collection<? extends Patch<?>> loaded = PatchKt.loadPatchesFromJar(Collections.singleton(bundle.toFile()));
    int matches = 0;
    for (Patch<?> patch : loaded) {
      if (!NAME.equals(patch.getName())) continue;
      matches++;
      check(patch.getCompatibility() != null, "Package restrictions absent");
      Set<String> found = new TreeSet<>();
      for (Compatibility compatible : patch.getCompatibility()) {
        check(found.add(compatible.getPackageName()), "Duplicate package");
        boolean all = compatible.getTargets() == null || compatible.getTargets().isEmpty();
        if (!all) for (AppTarget target : compatible.getTargets()) if (target.getVersion() == null) all = true;
        check(all, "Unexpected version restriction: " + compatible.getPackageName());
      }
      check(found.equals(PACKAGES), "Package scope differs: " + found);
      Field field = Patch.class.getDeclaredField("executeBlock"); field.setAccessible(true);
      Object execute = field.get(patch); check(execute != null, "Executable implementation absent");
      Path origin = Paths.get(execute.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
      check(origin.equals(bundle), "Executable code did not come from shipped MPP");
    }
    check(matches == 1, "Patch must be discovered exactly once");
    System.out.println("PASS OneBackMetadataAudit: 5 packages; all_versions=true; executable_from_mpp=true");
  }
}
