import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
/** Exact preview-lifecycle and accepted camera-switch UI changes only. */
public final class Hooks1936 {
 static Set<String> union(Set<String> a,Set<String> b){var result=new TreeSet<String>(a);result.addAll(b);return Collections.unmodifiableSet(result);}
 static final Set<String> NATIVE=union(FrontHooks1936.NATIVE,LensHooks1936.NATIVE), RUNTIME=union(FrontHooks1936.RUNTIME,LensHooks1936.RUNTIME);
 static Method repair(Method method){String id=MergePayloads.id(method);Method next=method;if(FrontHooks1936.NATIVE.contains(id)||FrontHooks1936.RUNTIME.contains(id))next=FrontHooks1936.repair(next);if(LensHooks1936.NATIVE.contains(id)||LensHooks1936.RUNTIME.contains(id))next=LensHooks1936.repair(next);return next;}
 static void verify(Method before,Method after){String id=MergePayloads.id(before);if((FrontHooks1936.NATIVE.contains(id)||FrontHooks1936.RUNTIME.contains(id))&&(LensHooks1936.NATIVE.contains(id)||LensHooks1936.RUNTIME.contains(id)))throw new IllegalStateException("Overlapping hook requires explicit inverse verification: "+id);if(FrontHooks1936.NATIVE.contains(id)||FrontHooks1936.RUNTIME.contains(id))FrontHooks1936.verify(before,after);else LensHooks1936.verify(before,after);}
}
