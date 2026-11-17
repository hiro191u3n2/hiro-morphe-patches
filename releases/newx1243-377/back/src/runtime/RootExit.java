package app.hiro.newx.back;

import android.app.Activity;
import java.lang.ref.WeakReference;
import com.x.android.main.ea;
import com.x.main.api.landing.d0;
import com.x.navigation.vd;
import kotlin.jvm.functions.Function0;

/** Uses X's own root predicate and tab selection completion; never intercepts a child pop. */
public final class RootExit {
    private static WeakReference<Activity> activity = new WeakReference<Activity>(null);
    private RootExit() {}
    public static void bind(Activity value) { activity = new WeakReference<Activity>(value); }
    public static boolean enabled(Object value) {
        if (!(value instanceof ea)) return false;
        ea root = (ea)value;
        if (!root.u() || root.X == null) return false;
        d0 landing = root.X.f();
        if (landing == null) return false;
        vd tab = landing.s();
        return tab == vd.HOME || tab == vd.EXPLORE || tab == vd.NOTIFICATIONS || tab == vd.DM;
    }
    public static boolean rootBack(Object value) {
        if (!enabled(value)) return false;
        Activity current = activity.get();
        if (current == null || current.isFinishing() || current.isDestroyed()) return false;
        final Complete complete = new Complete(current);
        try {
            ((ea)value).X.j(vd.HOME, complete);
        } catch (RuntimeException failure) {
            // Preserve the requested terminal back even if native home selection rejects it.
            complete.invoke();
        }
        return true;
    }
    private static final class Complete implements Function0<Object> {
        private Activity current;
        Complete(Activity value) { current = value; }
        @Override public Object invoke() {
            Activity value = current;
            current = null;
            if (value != null && !value.isFinishing() && !value.isDestroyed()) value.finishAndRemoveTask();
            return null;
        }
    }
}
