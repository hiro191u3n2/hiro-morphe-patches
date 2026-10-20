import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
/** Only the existing accepted-capture display callback changes. */
public final class Hooks1941 {
 static final Set<String> NATIVE=FeedbackHooks1941.NATIVE, RUNTIME=FeedbackHooks1941.RUNTIME;
 static Method repair(Method m){return FeedbackHooks1941.repair(m);}
 static void verify(Method before,Method after){FeedbackHooks1941.verify(before,after);}
}
