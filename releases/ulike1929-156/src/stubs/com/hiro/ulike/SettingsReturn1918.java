package com.hiro.ulike;

import android.app.Activity;
import android.os.Looper;

/** Settings Back closes only the settings Activity; the live camera task is retained. */
public final class SettingsReturn1918 {
    private static final String SETTINGS = "com.light.beauty.basisplatform.appsetting.AppSettingsActivity";
    private SettingsReturn1918() {}

    public static boolean returnToCameraIfSettings(final Activity activity) {
        if (activity == null) return false;
        boolean settings = false;
        for (Class<?> type = activity.getClass(); type != null; type = type.getSuperclass()) {
            if (SETTINGS.equals(type.getName())) { settings = true; break; }
        }
        if (!settings) return false;
        if (activity.isFinishing() || activity.isDestroyed()) return true;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            activity.finish();
        } else {
            activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (!activity.isFinishing() && !activity.isDestroyed()) activity.finish();
                }
            });
        }
        return true;
    }
}
