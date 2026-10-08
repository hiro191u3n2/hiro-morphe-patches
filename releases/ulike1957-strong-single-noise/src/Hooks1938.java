import java.util.*;
import com.android.tools.smali.dexlib2.iface.Method;

/** Exact native renderer lifecycle entry points; camera hooks retain their ABI. */
public final class Hooks1938 {
    public static final Set<String> NATIVE = RenderHooks1938.NATIVE;
    public static final Set<String> RUNTIME = Set.of();
    public static Method repair(Method method) { return RenderHooks1938.repair(method); }
    public static void verify(Method before, Method after) { RenderHooks1938.verify(before, after); }
}
