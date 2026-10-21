package app.hiro.aurora.runtime;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.WeakHashMap;

/** Main-thread startup routing. Authentication and app-lock remain owned by Aurora. */
public final class AuroraFavourites {
    private AuroraFavourites() {}
    private static final WeakHashMap<Activity, State> STATES = new WeakHashMap<Activity, State>();
    private static final class State {
        WeakReference<List<Object>> stack;
        Object favourite;
        boolean pending;
        boolean external;
    }

    static boolean normalLaunch(Intent intent) {
        if (intent == null) return true;
        String action = intent.getAction();
        if (action != null && !Intent.ACTION_MAIN.equals(action)) return false;
        if (intent.getData() != null) return false;
        Bundle extras = intent.getExtras();
        return extras == null || extras.isEmpty();
    }

    static boolean blockedName(String name) {
        return "com.aurora.store.compose.navigation.Screen$z".equals(name)
                || "com.aurora.store.compose.navigation.Screen$s".equals(name)
                || "com.aurora.store.compose.navigation.Screen$l".equals(name);
    }

    static boolean blocked(List<?> stack) {
        if (stack == null || stack.isEmpty()) return true;
        // Only the visible destination is an authentication gate. Aurora may retain
        // a completed Splash behind AppDetails after refreshing a deep-link session.
        Object screen = stack.get(stack.size() - 1);
        return screen == null || blockedName(screen.getClass().getName());
    }

    private static State state(Activity activity) {
        State state = STATES.get(activity);
        if (state == null) {
            state = new State();
            STATES.put(activity, state);
        }
        return state;
    }

    private static Activity activity(Context context) {
        for (int remaining = 16; context != null && remaining > 0; remaining--) {
            if (context instanceof Activity) return (Activity) context;
            if (!(context instanceof ContextWrapper)) break;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) break;
            context = next;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void bind(Context context, List<?> stack, Object favourite) {
        Activity activity = activity(context);
        if (activity == null || stack == null || favourite == null
                || activity.isFinishing() || activity.isDestroyed()) return;
        State state = state(activity);
        List<Object> previous = state.stack == null ? null : state.stack.get();
        if (previous == stack) return; // Recomposition must not undo user navigation.
        state.stack = new WeakReference<List<Object>>((List<Object>) stack);
        state.favourite = favourite;
        state.pending = normalLaunch(activity.getIntent());
        if (state.pending && !blocked(stack)) {
            // Activity recreation after rotation retains its view-model owner. A real
            // cold launch/process restoration has no last non-configuration owner.
            if (activity.getLastNonConfigurationInstance() == null) reset(state);
            state.pending = false;
        }
    }

    public static Object selectMain(List<?> stack, Object original) {
        for (State state : STATES.values()) {
            if (state.stack != null && state.stack.get() == stack && state.pending) {
                state.pending = false;
                return state.favourite == null ? original : state.favourite;
            }
        }
        return original;
    }

    private static void reset(State state) {
        List<Object> stack = state.stack == null ? null : state.stack.get();
        if (stack == null || state.favourite == null || blocked(stack)) return;
        if (stack.size() == 1 && stack.get(0) == state.favourite) return;
        stack.clear();
        stack.add(state.favourite);
    }

    public static void restart(Activity activity) {
        State state = STATES.get(activity);
        if (state == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (state.external) {
            state.external = false;
            return; // Return from Aurora-started installation/settings/account flows.
        }
        // A new foreground after initial delivery is a reopen, even when the original
        // Activity was created from a link or notification. External returns were handled above.
        List<?> stack = state.stack == null ? null : state.stack.get();
        if (blocked(stack)) {
            state.pending = true;
            return;
        }
        reset(state);
        state.pending = false;
    }

    /** Called only after the framework accepted an outbound activity/intent sender. */
    public static void external(Activity activity) {
        state(activity).external = true;
    }

    /** Also clears an outbound call that resumed without ever stopping this activity. */
    public static void resumed(Activity activity) {
        State state = STATES.get(activity);
        if (state != null) state.external = false;
    }
}
