import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
/** Exact reviewed save synchronization hooks; no new camera/native hooks. */
public final class Hooks1944 {
 static final Set<String> NATIVE=Set.of(), RUNTIME=SaveSpeedHooks1944.RUNTIME;
 static Method repair(Method m){return SaveSpeedHooks1944.repair(m);}
 static void verify(Method before,Method after){SaveSpeedHooks1944.verify(before,after);}
}
