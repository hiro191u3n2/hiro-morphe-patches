import java.nio.file.Path;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import app.morphe.patcher.patch.*;

/** Read all five real distributed JVM providers using the official patcher API. */
public final class Metadata {
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
    public static void main(String[] args) throws Exception {
        // Kotlin hides this baseline constructor from Java source, but existing
        // Manager/patcher runtimes and previously shipped providers use its ABI.
        java.lang.reflect.Constructor<AppTarget> targetConstructor = AppTarget.class.getDeclaredConstructor(
            String.class, boolean.class, Integer.class);
        require(java.lang.reflect.Modifier.isPublic(targetConstructor.getModifiers()), "Baseline AppTarget constructor is not public");
        require(targetConstructor.newInstance(null, false, null).getVersion() == null, "Baseline AppTarget ABI no longer creates an all-version target");
        if (args.length == 1 && "--abi".equals(args[0])) {
            System.out.println("{\"result\":\"PASS\",\"app_target_constructor\":\"(Ljava/lang/String;ZLjava/lang/Integer;)V\",\"compile_only_stub_required\":true}");
            return;
        }
        String[] classes = {"app.hiro.berry.patches.ForegroundTabsPatch",
            "app.hiro.berryshare.patches.BrowserVisibilityPatch",
            "app.hiro.berryzoom.patches.OneHandZoomPatch",
            "app.hiro.berryqueue.patches.BerryMessageQueuePatch",
            "app.hiro.berrybfcache.patches.BfCacheRepairPatch"};
        Set<String> names = new HashSet<>();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{Path.of(args[0]).toUri().toURL()}, Metadata.class.getClassLoader())) {
            for (String name : classes) {
                Patch<?> patch = (Patch<?>) loader.loadClass(name).getField("PATCH").get(null);
                require(patch.getName() != null && names.add(patch.getName()), "Missing or duplicate patch name: " + name);
                List<Compatibility> compat = patch.getCompatibility();
                require(compat.size() == 1, "Unexpected compatibility count: " + name);
                Compatibility c = compat.get(0);
                require("jp.ejimax.berrybrowser".equals(c.getPackageName()), "Package restriction changed: " + name);
                require(!c.getTargets().isEmpty(), "Missing all-version target: " + name);
                for (AppTarget target : c.getTargets()) require(target.getVersion() == null, "Version restriction remains: " + name);
            }
        }
        require(names.size() == 5, "Expected five distinct patch names");
        System.out.println("{\"result\":\"PASS\",\"loaded_berry_patches\":5,\"all_versions_null\":true,\"package_preserved\":true}");
    }
}
