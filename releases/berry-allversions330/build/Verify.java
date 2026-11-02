import java.nio.file.Path;
import java.util.*;
import java.net.*;
import app.morphe.patcher.patch.*;
/** Load the real JVM providers and mirror Manager's target version projection. */
public final class Verify {
 public static void main(String[] a) throws Exception {
  String[] names={"app.hiro.berry.patches.ForegroundTabsPatch","app.hiro.berryshare.patches.BrowserVisibilityPatch","app.hiro.berryzoom.patches.OneHandZoomPatch","app.hiro.berryqueue.patches.BerryMessageQueuePatch","app.hiro.berrybfcache.patches.BfCacheRepairPatch"};
  try(URLClassLoader l=new URLClassLoader(new URL[]{Path.of(a[0]).toUri().toURL()},Verify.class.getClassLoader())) {
   for(String n:names){
    Patch<?> p=(Patch<?>)l.loadClass(n).getField("PATCH").get(null);
    List<Compatibility> compat=p.getCompatibility();
    if(compat.size()!=1)throw new IllegalStateException("Unexpected compatibility count "+n);
    Compatibility c=compat.get(0);
    if(!"jp.ejimax.berrybrowser".equals(c.getPackageName()))throw new IllegalStateException("Package changed "+n);
    for(Object target:c.getTargets())if(target.getClass().getMethod("getVersion").invoke(target)!=null)throw new IllegalStateException("Version restricted "+n);
   }
  }
  System.out.println("{\"berry_patch_count\":5,\"all_versions_null\":true,\"all_package_names_unchanged\":true,\"manager_versions_projection\":null}");
 }
}
