package com.hiro.ulike;

import android.app.Activity;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import com.light.beauty.mainpage.MainActivity;
import com.light.beauty.mc.preview.page.main.UlikeMainPage;

/** Delegate to the application's native page stack before the existing exit controller. */
public final class BackRoute1920 {
    private BackRoute1920() {}

    public static boolean consume(final BackExit185 controller, final Activity activity) {
        if (activity == null) return false;
        if (activity.isFinishing() || activity.isDestroyed()) return true;
        if (SettingsReturn1918.returnToCameraIfSettings(activity)) return true;
        if (!(activity instanceof MainActivity)) return false;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        BackExit185.requestOnMain1920(controller, activity);
                    }
                }
            });
            return true;
        }
        try {
            UlikeMainPage page = ((MainActivity) activity).t;
            if (page == null || !page.isAdded()) return false;
            // MainActivity.onKeyDown routes through G2. Do not call the Activity
            // itself: its unconsumed fallback backgrounds the task instead of exiting.
            long now = SystemClock.uptimeMillis();
            if (page.G2(KeyEvent.KEYCODE_BACK,
                    new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0))) {
                return true;
            }
            // Match the native key-up path only when key-down did not consume Back.
            return page.H2(KeyEvent.KEYCODE_BACK,
                    new KeyEvent(now, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP,
                            KeyEvent.KEYCODE_BACK, 0));
        } catch (RuntimeException error) {
            // Never turn an unsuccessful panel dismissal into an irreversible exit.
            Log.w("ULikeBack1920", "Native page Back failed; application retained", error);
            return true;
        } catch (LinkageError error) {
            Log.w("ULikeBack1920", "Native page Back unavailable; application retained", error);
            return true;
        }
    }
}
