import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
/** Incremental visual-feedback/black-tap hooks only; previous response/settings hooks remain. */
public final class Hooks1940 {
 static final Set<String> NATIVE=GestureFeedbackHooks1940.NATIVE, RUNTIME=GestureFeedbackHooks1940.RUNTIME;
 static Method repair(Method m){return GestureFeedbackHooks1940.repair(m);}
 static void verify(Method before,Method after){GestureFeedbackHooks1940.verify(before,after);}
}
