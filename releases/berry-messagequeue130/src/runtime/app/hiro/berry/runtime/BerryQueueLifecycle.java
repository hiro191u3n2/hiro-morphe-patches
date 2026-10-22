package app.hiro.berry.runtime;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;

/** One process-local check, followed by at most one persisted permission attempt. */
final class BerryQueueLifecycle implements Application.ActivityLifecycleCallbacks {
    private static final String PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK";
    private static final String PREFERENCES = "hiro_berry_messagequeue";
    private static final String ATTEMPTED = "local_network_permission_attempted_v1";
    private static final int REQUEST_CODE = 0x6B37;

    private final Application application;
    private final Handler main;
    private WeakReference<Activity> resumed = new WeakReference<Activity>(null);
    private boolean pending;
    private boolean diagnosed;
    private boolean finished;

    BerryQueueLifecycle(Application application) {
        this.application = application;
        this.main = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (finished || Build.VERSION.SDK_INT < 37) {
            return;
        }
        try {
            if (!isReady(activity)) {
                return;
            }
            resumed = new WeakReference<Activity>(activity);
            if (!pending) {
                pending = true;
                if (!main.post(new Runnable() {
                    @Override
                    public void run() {
                        runWhenResumed();
                    }
                })) {
                    pending = false;
                }
            }
        } catch (RuntimeException ignored) {
            finish();
        }
    }

    private void runWhenResumed() {
        pending = false;
        if (finished || Build.VERSION.SDK_INT < 37) {
            return;
        }
        try {
            // Use the currently resumed Activity, never a stale Activity captured by a post.
            Activity activity = resumed.get();
            if (!isReady(activity)) {
                return;
            }
            if (!diagnosed) {
                diagnosed = true;
                BerryQueueDiagnostics.logMainQueue();
            }
            if (activity.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED) {
                finish();
                return;
            }
            SharedPreferences preferences = application.getSharedPreferences(
                    PREFERENCES, Context.MODE_PRIVATE);
            if (preferences.getBoolean(ATTEMPTED, false)) {
                finish();
                return;
            }
            // No intervening asynchronous work: record an attempt only at the actual request.
            // apply() updates in-process state immediately without synchronous disk I/O here.
            preferences.edit().putBoolean(ATTEMPTED, true).apply();
            finish();
            activity.requestPermissions(new String[] {PERMISSION}, REQUEST_CODE);
        } catch (RuntimeException ignored) {
            // Includes unavailable preferences and an OEM rejecting a permission request.
            // If an attempt was recorded, preserve it rather than repeatedly prompting.
            finish();
        }
    }

    private boolean isReady(Activity activity) {
        return activity != null && activity.getApplication() == application
                && !activity.isFinishing() && !activity.isDestroyed();
    }

    private void finish() {
        finished = true;
        resumed.clear();
        try {
            application.unregisterActivityLifecycleCallbacks(this);
        } catch (RuntimeException ignored) {
            // finished also prevents duplicate work if unregistering is rejected.
        }
    }

    private void forget(Activity activity) {
        if (resumed.get() == activity) {
            resumed.clear();
        }
    }

    @Override public void onActivityPaused(Activity activity) { forget(activity); }
    @Override public void onActivityStopped(Activity activity) { forget(activity); }
    @Override public void onActivityDestroyed(Activity activity) { forget(activity); }
    @Override public void onActivityCreated(Activity activity, Bundle state) { }
    @Override public void onActivityStarted(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
}
