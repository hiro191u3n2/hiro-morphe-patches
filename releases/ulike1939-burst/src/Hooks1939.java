import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
/** Native manual-shutter timing plus immutable per-photo save settings. */
public final class Hooks1939 {
 static final Set<String> NATIVE=ResponseHooks1939.NATIVE, RUNTIME=SaveResponseHooks1939.RUNTIME;
 static Method repair(Method m){return NATIVE.contains(MergePayloads.id(m))?ResponseHooks1939.repair(m):SaveResponseHooks1939.repair(m);}
 static void verify(Method before,Method after){if(NATIVE.contains(MergePayloads.id(before)))ResponseHooks1939.verify(before,after);else SaveResponseHooks1939.verify(before,after);}
}
