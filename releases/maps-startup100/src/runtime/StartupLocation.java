package app.hiro.mapsstartup;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** One bounded, cancellable native current-location request per normal app opening. */
public final class StartupLocation {
    private static final String MAPS_ACTIVITY = "com.google.android.maps.MapsActivity";
    private static final int MY_LOCATION_BUTTON = 0x7f0b06b0;
    private static final long START_DELAY_MS = 250L;
    private static final long RETRY_DELAY_MS = 250L;
    private static final long MAX_WAIT_MS = 60000L;
    private static final WeakHashMap<Activity, Session> SESSIONS = new WeakHashMap<Activity, Session>();
    private static Handler handler;

    private StartupLocation() {}

    private static Handler handler() {
        if (handler == null) handler = new Handler(Looper.getMainLooper());
        return handler;
    }

    private static boolean supported(Activity activity) {
        return activity instanceof LocationHost && MAPS_ACTIVITY.equals(activity.getClass().getName());
    }

    private static boolean normalLaunch(Intent intent) {
        if (intent == null || !Intent.ACTION_MAIN.equals(intent.getAction())
                || !intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                || intent.getData() != null || intent.getType() != null
                || intent.getSelector() != null) return false;
        ComponentName component = intent.getComponent();
        return component == null || MAPS_ACTIVITY.equals(component.getClassName());
    }

    public static void onCreate(Activity activity, Bundle savedState) {
        try {
            if (!supported(activity)) return;
            Session old = SESSIONS.remove(activity);
            if (old != null) old.cancel();
            Session session = new Session(activity);
            SESSIONS.put(activity, session);
            session.launcher = normalLaunch(activity.getIntent());
            if (session.launcher) session.arm();
        } catch (Throwable ignored) { /* An optional startup action must never break Maps. */ }
    }

    public static void onNewIntent(Activity activity, Intent intent) {
        try {
            Session session = SESSIONS.get(activity);
            if (session == null || !supported(activity)) return;
            session.cancel();
            session.launcher = normalLaunch(intent);
            if (session.launcher) {
                session.arm();
                session.schedule(START_DELAY_MS);
            }
        } catch (Throwable ignored) { }
    }

    public static void onStart(Activity activity) {
        try {
            Session session = SESSIONS.get(activity);
            if (session == null) return;
            // A full stop/start is a fresh foreground opening. A pause/resume alone
            // (permission sheet, transient dialog) must not recenter a second time.
            if (session.stopped && session.launcher) session.arm();
            session.stopped = false;
        } catch (Throwable ignored) { }
    }

    public static void onResume(Activity activity) {
        try {
            Session session = SESSIONS.get(activity);
            if (session == null) return;
            session.resumed = true;
            session.schedule(START_DELAY_MS);
        } catch (Throwable ignored) { }
    }

    public static void onPause(Activity activity) {
        try {
            Session session = SESSIONS.get(activity);
            if (session == null) return;
            session.resumed = false;
            session.unschedule();
        } catch (Throwable ignored) { }
    }

    public static void onStop(Activity activity) {
        try {
            Session session = SESSIONS.get(activity);
            if (session == null) return;
            session.resumed = false;
            session.stopped = true;
            session.unschedule();
        } catch (Throwable ignored) { }
    }

    public static void onDestroy(Activity activity) {
        try {
            Session session = SESSIONS.remove(activity);
            if (session != null) session.cancel();
        } catch (Throwable ignored) { }
    }

    public static void onTouch(Activity activity, MotionEvent event) {
        try {
            if (event == null || event.getActionMasked() != MotionEvent.ACTION_DOWN) return;
            Session session = SESSIONS.get(activity);
            if (session != null) session.cancel();
        } catch (Throwable ignored) { }
    }

    private static final class Session implements Runnable {
        final WeakReference<Activity> owner;
        boolean launcher;
        boolean resumed;
        boolean stopped;
        boolean pending;
        long deadline;

        Session(Activity activity) { owner = new WeakReference<Activity>(activity); }

        void arm() {
            unschedule();
            pending = true;
            deadline = SystemClock.elapsedRealtime() + MAX_WAIT_MS;
        }

        void unschedule() {
            if (handler != null) handler.removeCallbacks(this);
        }

        void cancel() { pending = false; unschedule(); }

        void schedule(long delay) {
            unschedule();
            if (pending && resumed && launcher) handler().postDelayed(this, delay);
        }

        @Override public void run() {
            Activity activity = owner.get();
            if (activity == null) { cancel(); return; }
            try {
                if (SESSIONS.get(activity) != this || !pending || !launcher || !resumed) return;
                if (activity.isFinishing() || activity.isDestroyed()
                        || SystemClock.elapsedRealtime() >= deadline) { cancel(); return; }
                if (activity.hasWindowFocus()) {
                    View button = activity.findViewById(MY_LOCATION_BUTTON);
                    if (button != null && button.isShown() && button.isEnabled()
                            && button.getWindowToken() != null) {
                        // The injected bridge checks native map mode, lifecycle and
                        // actual location-fix readiness before the idempotent f().
                        // It never synthesizes a click or toggles compass mode.
                        int result = ((LocationHost) activity).hiroTryCurrentLocation();
                        if (result != 0) { cancel(); return; }
                    }
                }
                schedule(RETRY_DELAY_MS);
            } catch (Throwable ignored) {
                // An incompatible runtime is terminal for this opening; retrying
                // reflective or malformed native calls could repeatedly disrupt UI.
                cancel();
            }
        }
    }
}
