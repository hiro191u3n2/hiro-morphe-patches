import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
/** Only the reviewed codec preparation and final-save hooks change. */
public final class Hooks1945 {
 static final Set<String> NATIVE=Set.of(), RUNTIME=SaveSpeedHooks1945.RUNTIME;
 static Method repair(Method m){return SaveSpeedHooks1945.repair(m);}
 static void verify(Method before,Method after){SaveSpeedHooks1945.verify(before,after);}
}
