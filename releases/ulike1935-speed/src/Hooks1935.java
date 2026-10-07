import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
/** Exact audited T1 hooks; all other stock and runtime methods retain their contracts. */
public final class Hooks1935 {
 static final Set<String> NATIVE=SaveHooks1935.NATIVE, RUNTIME=SaveHooks1935.RUNTIME;
 static Method repair(Method method){return SaveHooks1935.repair(method);}
 static void verify(Method before,Method after){SaveHooks1935.verify(before,after);}
}
