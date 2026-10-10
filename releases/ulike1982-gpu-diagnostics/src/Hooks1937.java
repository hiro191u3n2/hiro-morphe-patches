import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
/** Exact reviewed stale layout callback and measured-geometry hooks only. */
public final class Hooks1937 {
 static Set<String> union(Set<String> a,Set<String> b){var result=new TreeSet<String>(a);for(String id:b)if(!result.add(id))throw new IllegalStateException("Overlapping hook needs explicit inverse proof: "+id);return Collections.unmodifiableSet(result);}
 static final Set<String> NATIVE=union(LayoutHooks1937.NATIVE,GeometryHooks1937.NATIVE), RUNTIME=union(LayoutHooks1937.RUNTIME,GeometryHooks1937.RUNTIME);
 static boolean lifecycle(Method method){String id=MergePayloads.id(method);return LayoutHooks1937.NATIVE.contains(id)||LayoutHooks1937.RUNTIME.contains(id);}
 static Method repair(Method method){return lifecycle(method)?LayoutHooks1937.repair(method):GeometryHooks1937.repair(method);}
 static void verify(Method before,Method after){if(lifecycle(before))LayoutHooks1937.verify(before,after);else GeometryHooks1937.verify(before,after);}
}
