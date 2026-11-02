package app.hiro.berrybfcache.patches;

import app.morphe.patcher.patch.BytecodePatchContext;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import kotlin.Unit;

/** Do not occupy an AndroidX Page peer slot that surviving app code may own. */
final class NativePagePolicy {
    private static final String PAGE = "Lorg/chromium/support_lib_boundary/WebViewPageBoundaryInterface;";
    private static final String NAV = "Lorg/chromium/support_lib_boundary/WebViewNavigationBoundaryInterface;";
    private static final String NAV_NAME = "org.chromium.support_lib_boundary.WebViewNavigationBoundaryInterface";
    private static final String PAGE_NAME = "org.chromium.support_lib_boundary.WebViewPageBoundaryInterface";
    private static final String GET_PAGE = "Lorg/chromium/support_lib_boundary/WebViewNavigationBoundaryInterface;->getPage(";
    private NativePagePolicy() {}
    static boolean allowOwnPeer(BytecodePatchContext context) {
        final boolean[] allowed = {true};
        context.classDefForEach(c -> {
            if (c.getType().startsWith("Lorg/chromium/support_lib_boundary/")
                    || c.getType().startsWith("Lhiro/berrybfcache/")) return Unit.INSTANCE;
            if (PAGE.equals(c.getSuperclass()) || NAV.equals(c.getSuperclass()) || c.getInterfaces().contains(PAGE) || c.getInterfaces().contains(NAV)) allowed[0] = false;
            for (Field f : c.getFields()) if (f.getType().contains(PAGE) || f.getType().contains(NAV)) allowed[0] = false;
            for (Method m : c.getMethods()) {
                if (m.getReturnType().contains(PAGE) || m.getReturnType().contains(NAV)) allowed[0] = false;
                for (CharSequence p : m.getParameterTypes()) if (p.toString().contains(PAGE) || p.toString().contains(NAV)) allowed[0] = false;
                if (m.getImplementation() == null) continue;
                for (Instruction i : m.getImplementation().getInstructions()) if (i instanceof ReferenceInstruction) {
                    String ref = DexFormatter.INSTANCE.getReference(((ReferenceInstruction)i).getReference());
                    if (ref.contains(PAGE) || ref.contains(PAGE_NAME) || ref.contains(NAV) || ref.contains(NAV_NAME) || ref.startsWith(GET_PAGE)
                            || ref.contains("androidx.webkit.Page") || ref.contains("androidx.webkit.Navigation")) allowed[0] = false;
                }
            }
            return Unit.INSTANCE;
        });
        return allowed[0];
    }
}
