import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/** Real JVM bundle selection regression; do not put provider classes on this classpath. */
public final class MapsSelectionRegression {
    private static final String NAME = "検索欄の下のカテゴリ行を非表示";
    private static final String STOCK = "com.google.android.apps.maps";
    private static final String MICROG = "org.ungoogled.android.apps.maps.microg";
    private static final String VERSION = "26.36.04.973607363";
    private static final String PROVIDER = "app.hiro.maps.patches.MapsCategoryRowPatch";
    private static int assertions;
    private static void check(boolean ok, String message) { assertions++; if (!ok) throw new AssertionError(message); }
    private static Object call(Object object, String name) throws Exception { return object.getClass().getMethod(name).invoke(object); }
    private static Collection<?> load(String path) throws Exception {
        return (Collection<?>)Class.forName("app.morphe.patcher.patch.PatchKt")
            .getMethod("loadPatchesFromJar", Set.class).invoke(null, Collections.singleton(new File(path)));
    }
    private static Map<String,Set<String>> metadata(Object patch) throws Exception {
        Object compatible = call(patch, "getCompatiblePackages");
        check(compatible instanceof Collection, "unexpected universal package metadata");
        Map<String,Set<String>> result = new TreeMap<>();
        for (Object pair : (Collection<?>)compatible) {
            Object pkg = call(pair, "getFirst"), versions = call(pair, "getSecond");
            check(pkg instanceof String && versions instanceof Collection, "invalid package/version metadata");
            Set<String> values = new TreeSet<>(); for (Object value : (Collection<?>)versions) values.add((String)value);
            check(result.put((String)pkg, values) == null, "duplicate package entry");
        }
        return result;
    }
    private static Object selectedPatch(Collection<?> loaded) throws Exception {
        Object result = null;
        for (Object patch : loaded) if (NAME.equals(call(patch, "getName"))) {
            check(result == null, "duplicate Maps patch selection key"); result = patch;
        }
        check(result != null, "Maps named patch missing"); return result;
    }
    private static boolean selectable(Map<String,Set<String>> packages, String pkg, String version) {
        return packages.containsKey(pkg) && packages.get(pkg).contains(version);
    }
    private static void packageGuards(String bundle, boolean stockExpected) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[]{new File(bundle).toURI().toURL()}, MapsSelectionRegression.class.getClassLoader())) {
            Class<?> provider = Class.forName(PROVIDER, true, loader);
            check(provider.getClassLoader() == loader, "provider unexpectedly loaded from parent classpath");
            java.lang.reflect.Method validate = provider.getMethod("validatePackage", String.class, String.class, String.class);
            validate.invoke(null, MICROG, VERSION, "1068763346");
            boolean stockAccepted = true;
            try { validate.invoke(null, STOCK, VERSION, "1068763346"); }
            catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof IllegalStateException)) throw e; stockAccepted = false;
            }
            check(stockAccepted == stockExpected, "package guard did not reproduce expected stock acceptance");
            for (String[] bad : new String[][]{
                {"com.example.maps", VERSION, "1068763346"},
                {MICROG, "26.36.05", "1068763346"},
                {MICROG, VERSION, "1068763347"}}) {
                boolean failed = false;
                try { validate.invoke(null, (Object[])bad); }
                catch (InvocationTargetException e) { if (!(e.getCause() instanceof IllegalStateException)) throw e; failed = true; }
                check(failed, "foreign package/version/code accepted");
            }
        }
    }
    public static void main(String[] args) throws Exception {
        check(args.length == 2, "OLD_MPP NEW_MPP required");
        try { Class.forName(PROVIDER, false, MapsSelectionRegression.class.getClassLoader()); throw new AssertionError("provider must not be on regression classpath"); }
        catch (ClassNotFoundException expected) { assertions++; }
        Object oldPatch = selectedPatch(load(args[0])), newPatch = selectedPatch(load(args[1]));
        Map<String,Set<String>> before = metadata(oldPatch), after = metadata(newPatch);
        check(before.keySet().equals(Collections.singleton(MICROG)), "baseline no longer reproduces microG-only metadata");
        check(after.keySet().equals(new TreeSet<>(Arrays.asList(MICROG, STOCK))), "new bundle does not list exactly two Maps packages");
        check(!selectable(before, STOCK, VERSION) && selectable(after, STOCK, VERSION), "stock selection must change 0 to 1");
        check(selectable(before, MICROG, VERSION) && selectable(after, MICROG, VERSION), "microG selection must stay available");
        for (String pkg : after.keySet()) check(after.get(pkg).equals(Collections.singleton(VERSION)), "version scope changed");
        check(!selectable(after, "com.example.maps", VERSION) && !selectable(after, STOCK, "26.36.05"), "unexpected broad compatibility");
        check(call(oldPatch, "getName").equals(call(newPatch, "getName")), "patch selection name changed");
        check(call(oldPatch, "getDefault").equals(call(newPatch, "getDefault")), "patch default changed");
        check(((Collection<?>)call(newPatch, "getDependencies")).isEmpty(), "unexpected new dependencies");
        check(call(newPatch, "getExtensionInputStream") == null, "unexpected runtime extension");
        packageGuards(args[0], false); packageGuards(args[1], true);
        System.out.println("PASS actual bundle selection: stock Maps 0 -> 1; microG Maps 1 -> 1; old package rejection reproduced; new package acceptance verified; exact version/code guards retained; assertions=" + assertions);
    }
}
