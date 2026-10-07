import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
/** Exact hooks are populated only after the stock ABI has been inspected. */
public final class Hooks1934 {
 static final Set<String> NATIVE=Set.of(YuvHooks1934.TARGET), RUNTIME=Set.of();
 static Method repair(Method method){if(MergePayloads.id(method).equals(YuvHooks1934.TARGET))return YuvHooks1934.repair(method);throw new IllegalStateException("No reviewed hook: "+MergePayloads.id(method));}
 static void verify(Method before,Method after){if(MergePayloads.id(after).equals(YuvHooks1934.TARGET)){YuvHooks1934.verify(before,after);return;}throw new IllegalStateException("No reviewed hook: "+MergePayloads.id(after));}
}
