package android.view.inspector;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
public final class WindowInspector {
    private static final List<View> ROOTS = new ArrayList<View>();
    public static int queries;
    public static List<View> getGlobalWindowViews() { queries++; return new ArrayList<View>(ROOTS); }
    public static void add(View view) { if (!ROOTS.contains(view)) ROOTS.add(view); }
    public static void remove(View view) { ROOTS.remove(view); }
    public static void clear() { ROOTS.clear(); queries = 0; }
}
