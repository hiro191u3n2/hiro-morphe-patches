import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
/** Capture timing helper update: no native, camera or runtime-callsite hooks. */
public final class Hooks1943 {
 static final Set<String> NATIVE=Set.of(), RUNTIME=Set.of();
 static Method repair(Method m){throw new IllegalStateException("No new hooks in 1.9.43");}
 static void verify(Method before,Method after){throw new IllegalStateException("No new hooks in 1.9.43");}
}
