package app.hiro.mapsstartup;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Fresh-launch-only recentering with native camera acknowledgement and bounded recovery. */
public final class StartupLocation {
    private static final String MAPS_ACTIVITY = "com.google.android.maps.MapsActivity";
    private static final long START_SETTLE_MS = 1000L;
    private static final long RETRY_DELAY_MS = 250L;
    private static final long ACK_WAIT_MS = 1500L;
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
            Intent intent = activity.getIntent();
            session.launcher = normalLaunch(intent);
            // Restoring an existing task (including process/configuration
            // recreation) must preserve its map. Only a fresh launcher creation
            // may start the optional one-shot action.
            if (session.launcher && savedState == null
                    && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
                session.arm();
            }
        } catch (Throwable ignored) { /* An optional startup action must never break Maps. */ }
    }

    public static void onNewIntent(Activity activity, Intent intent) {
        try {
            Session session = SESSIONS.get(activity);
            if (session == null || !supported(activity)) return;
            session.cancel();
            session.launcher = normalLaunch(intent);
            // A new intent delivered to this Activity reuses an existing task;
            // launcher icons and destination links must not restart recentering.
        } catch (Throwable ignored) { }
    }

    public static void onStart(Activity activity) {
        // Keep the verified lifecycle hook, but never treat a retained task's
        // stop/start as a fresh launch. onCreate is the only arming point.
    }

    public static void onResume(Activity activity) {
        try {
            Session session = SESSIONS.get(activity);
            if (session == null) return;
            boolean wasResumed = session.resumed;
            session.resumed = true;
            if (!wasResumed && session.pending
                    && (session.awaitFirstResume || session.resettleAfterPause)) {
                long now = SystemClock.elapsedRealtime();
                session.readyAfter = now + START_SETTLE_MS;
                if (session.requestsIssued > 0) session.retryAfter = session.readyAfter + ACK_WAIT_MS;
                session.awaitFirstResume = false;
                session.resettleAfterPause = false;
            }
            session.schedule(RETRY_DELAY_MS);
        } catch (Throwable ignored) { }
    }

    public static void onPause(Activity activity) {
        try {
            Session session = SESSIONS.get(activity);
            if (session == null) return;
            session.resumed = false;
            if (session.pending && session.requestsIssued > 0) session.resettleAfterPause = true;
            session.confirmations = 0;
            session.unschedule();
        } catch (Throwable ignored) { }
    }

    public static void onStop(Activity activity) {
        try {
            Session session = SESSIONS.get(activity);
            if (session == null) return;
            session.resumed = false;
            // Home/app switching ends even an unfinished startup attempt. A late
            // fix or recovery callback must not move the map after returning.
            session.cancel();
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
            // Touching while the map/fix is still loading must not discard the
            // opening's one-shot request. A dispatched camera command is not an
            // arrival either: check the live camera before handing control back.
            if (session == null || !session.pending || !session.launcher
                    || !session.resumed || session.requestsIssued == 0) return;
            if (activity.isFinishing() || activity.isDestroyed()
                    || SystemClock.elapsedRealtime() >= session.deadline) {
                session.cancel();
                return;
            }
            if (!activity.hasWindowFocus()) return;
            int result = ((LocationHost) activity).hiroTryCurrentLocation(false);
            // A single live acknowledgement is sufficient when the user starts
            // interacting: this hook runs BEFORE Maps handles the gesture, so an
            // already-arrived map is never pulled back by a later recovery poll.
            // This read-only query cannot move the map or renew the retry budget.
            if (result == 1 || result == -1) session.cancel();
        } catch (Throwable ignored) { }
    }

    private static final class Session implements Runnable {
        final WeakReference<Activity> owner;
        boolean launcher;
        boolean resumed;
        boolean pending;
        boolean awaitFirstResume;
        boolean resettleAfterPause;
        int requestsIssued;
        int confirmations;
        long deadline;
        long readyAfter;
        long retryAfter;

        Session(Activity activity) { owner = new WeakReference<Activity>(activity); }

        void arm() {
            unschedule();
            pending = true;
            long now = SystemClock.elapsedRealtime();
            deadline = now + MAX_WAIT_MS;
            readyAfter = now + START_SETTLE_MS;
            retryAfter = 0L;
            requestsIssued = 0;
            confirmations = 0;
            awaitFirstResume = !resumed;
            resettleAfterPause = false;
        }

        void unschedule() {
            if (handler != null) handler.removeCallbacks(this);
        }

        void cancel() { pending = false; confirmations = 0; unschedule(); }

        void schedule(long delay) {
            unschedule();
            if (pending && resumed && launcher) handler().postDelayed(this, delay);
        }

        @Override public void run() {
            Activity activity = owner.get();
            if (activity == null) { cancel(); return; }
            try {
                if (SESSIONS.get(activity) != this || !pending || !launcher || !resumed) return;
                long now = SystemClock.elapsedRealtime();
                if (activity.isFinishing() || activity.isDestroyed()
                        || now >= deadline) { cancel(); return; }
                if (now < readyAfter || !activity.hasWindowFocus()) {
                    confirmations = 0;
                    if (requestsIssued == 1) retryAfter = Math.max(retryAfter, now + ACK_WAIT_MS);
                } else {
                    // The verified native bridge checks the actual map/controller,
                    // copied location fix, renderer and camera target. Button views
                    // are not prerequisites for the native camera operation.
                    boolean issue = confirmations == 0 && (requestsIssued == 0
                        || (requestsIssued == 1 && now >= retryAfter));
                    int result = ((LocationHost) activity).hiroTryCurrentLocation(issue);
                    if (result == -1) { cancel(); return; }
                    if (result == 2) {
                        // Dispatch is not success. Subsequent queries cannot send
                        // another command until the bounded recovery delay expires.
                        if (!issue || requestsIssued >= 2) { cancel(); return; }
                        requestsIssued++;
                        confirmations = 0;
                        retryAfter = now + ACK_WAIT_MS;
                    } else if (result == 1) {
                        confirmations++;
                        retryAfter = now + ACK_WAIT_MS;
                        if (confirmations >= 2) { cancel(); return; }
                    } else if (result == 0) {
                        confirmations = 0;
                    } else {
                        cancel(); return;
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
